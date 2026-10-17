// WO-003 TASK-013 · the one pointer handler of the Play screen (tasks.md § WO-003 › Seams S-10 "One pointer
// handler on the root takes every touch"; the touch routing, steps 1–4; workorders/WO-003.md § Constraints
// "Gestures") · REQ-014 (the drag), REQ-015 (tap or drag on raw pointer events with S-8 — never
// `detectTapGestures`, whose long-press timeout would break "regardless of its duration"; the touch areas
// through `HitTest`), REQ-016 (a tap turns), REQ-017 (the second finger twists through S-7), REQ-020 (a
// release drops; a pointer cancel sends the piece home — decisions.md 2026-10-11, the G1 drafts' readings).
// O-01: the router decides which piece a finger touched and where the piece is held; every lock is the
// controller's, so the engine's.
// WO-003 TASK-014 · the badge branch (step 2, first; REQ-018): a press is `pressBadge()`, and "a touch that
// starts on a badge never drags" — one that moves past the slop presses nothing either.
// WO-003 code review N3 · REQ-021 A1 "Releasing the piece always locks it where the preview was drawn" and its
// rule "the preview uses the same search as the drop": the hand reaches the controller once per displayed frame,
// in the frame callback that searches the preview, and a release drops what that frame searched — a move that
// arrives with the lift, before any frame drew it, is never searched (REQ-014 A1's draft bound: the piece is
// drawn at the finger of the current frame). N4 · the first-down-during-a-drag branch (`twistOnly`) is gone:
// every drag starts and ends inside one `gesture`, which owns every pointer until its primary finger lifts.
// WO-004 TASK-020 · S-22 (designs/WO-004-design.md §2 "Hit order", "Taps and the long press"; §3 library fact 5):
// a touch that lands on a control (‹, ›, the counter, the Restart pill, a third of the solved row) never drags; it
// acts at the lift within the 12 dp slop, whatever its duration, and moving the slop or more cancels it (as on the
// flip badge; decisions.md 2026-10-15 "Browsing readings no REQ fixes"). On › only, a finger held within the slop
// for 500 ms runs `nextUnsolved()` then, and its lift does nothing (REQ-050 rule "at least 500 ms"). The hold is
// timed by `withTimeoutOrNull` on the pointer node's own clock — uptime on a device, the test rule's `mainClock`
// under test — so nothing sleeps and no wall clock is read (G-14). Hit order 1 holds as before: a drag owns every
// touch until its finger lifts, so ‹ and › never act mid-drag.
// WO-004 TASK-021 · S-22 hit order 0 (design §3, library fact 4): while the overview is open, every touch is left to
// the overlay's `clickable`s, unconsumed, the top bar's included.
package app.tangramplaytime.play

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.Turn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.atan2

/** REQ-050 rule "Long press: at least 500 ms": › held this long within the slop runs `nextUnsolved()` (S-22). */
internal const val LONG_PRESS_MS = 500L

/** How a touch on a control ended before any hold fired. */
private enum class ControlEnd {
    /** Lifted within the slop: the control acts. */
    LIFTED,

    /** Moved the slop or more: cancelled; its lift does nothing. */
    SLID,

    /** A pointer cancel: nothing. */
    CANCELLED,
}

/**
 * Routes every touch of `play-screen` (the touch routing; S-22's hit order):
 * 0. While the grid overview is open, a new touch is the overlay's: the router neither consumes nor acts until its
 *    last finger lifts, on the top bar too (the bar stays drawn and inert; the overlay's `clickable`s act).
 * 1. While a drag runs, every further touch is a twist finger for that drag (S-7 → `twistDrag`), whatever it
 *    lands on — ‹, › and the counter included; lifting it keeps the turn reached. Every drag starts and ends
 *    inside the one gesture that owns every pointer until its primary finger lifts, so a new gesture never meets
 *    a running drag.
 * 2. Otherwise a new touch belongs to the first hit of [HitTest] (a top-bar column or a third of the solved row;
 *    then the badges, the drawn pieces, the Restart pill, the rest of the badge areas, the tray cells and the
 *    pieces' margins → nothing).
 * 3. A touch on a control never drags: it acts at the lift within the slop (S-8), whatever its duration; moved
 *    the slop or more it does nothing; on ›, held 500 ms within the slop, it runs `nextUnsolved()` at once and its
 *    lift does nothing ([control]).
 * 4. Any other touch that ends having moved less than the slop is a tap: `pressBadge`, `tapTray` or `tapBoard`.
 *    From the move that reaches the slop it is a drag: `startDrag`, then `moveDrag` with the piece's lowest
 *    point `floatOffsetDp` above the finger and `overBoard` = the finger is over the board. A touch that
 *    starts on a badge never drags: moved past the slop, it does nothing at all.
 * 5. A release calls `drop()`; a pointer cancel calls `goHome()`.
 *
 * The hand reaches the controller once per displayed frame (REQ-021 rules "updated at most once per displayed
 * frame" and "the same search as the drop"): the moves and twist steps that arrive between two frames are held
 * here and given to the controller — `twistDrag`, then `moveDrag` for the latest finger — in the frame callback,
 * just before that frame's `updatePreview()`, so the frame draws the piece and the preview of one state. A
 * release drops that state: input that arrived after the last frame was never drawn and is never searched, so
 * the lock is the drawn preview (REQ-021 A1; code review N3). Before the drag's first frame the drop searches
 * the state the drag started with (S-2 `drop`, "computed now if not yet").
 *
 * The router also keeps [DragVisual] in step: the origin of a drag (where the miniature or the board piece
 * was drawn) and the grow (REQ-014, [GROW_MS]) it drives on the frame clock from the drag's first moment,
 * and the glide every ended drag leaves behind.
 *
 * @param geometry the current layout, read at each touch (null before the first layout: touches do nothing).
 * @param shown the controls drawn now besides the top bar (the Restart pill, the solved row), read at each touch.
 * @param floatOffsetDp how far the dragged piece's lowest point floats above the finger (S-5, REQ-014 ASSUMPTION).
 */
internal class PointerRouter(
    private val controller: PlayController,
    private val visual: DragVisual,
    private val geometry: () -> ScreenGeometry?,
    private val shown: () -> ShownControls,
    private val floatOffsetDp: Float,
) {
    private var growJob: Job? = null

    /** The running drag's frame loop: the held input, then the preview search, once per displayed frame. */
    private var frameJob: Job? = null

    /** The latest finger position not yet given to the controller (null: none since the last frame). */
    private var heldFinger: Offset? = null

    /** The twist steps not yet given to the controller, net, positive clockwise. */
    private var heldSteps = 0

    /** The handler body for `Modifier.pointerInput` on the root: one gesture after another, on raw pointer events. */
    suspend fun PointerInputScope.route() {
        val slopPx = TAP_SLOP_DP * density
        val marginPx = TOUCH_MARGIN_DP * density
        val floatPx = floatOffsetDp * density
        coroutineScope {
            val animations = this
            awaitEachGesture {
                val first = awaitFirstDown(requireUnconsumed = false)
                // Routing 0 (S-22 hit order 0): a touch that starts while the overview is open is the overlay's, from its
                // down to its last lift, on the top bar too — the router neither consumes nor acts. Compose runs the Main
                // pass on descendants first, and a `clickable` cancels when its Final pass sees a change consumed (design
                // §3, library fact 4). Returning here, `awaitEachGesture` waits for every pointer to lift without
                // consuming; a tap on the scrim or a cell that closes the overview at its lift still acts on nothing here.
                if (controller.overviewOpen) return@awaitEachGesture
                first.consume()
                val g = geometry() ?: return@awaitEachGesture
                when (val hit = HitTest.at(first.position, g, controller, marginPx, shown())) {
                    is Hit.OnControl -> control(first, hit.control, slopPx)
                    else -> gesture(first, hit, g, slopPx, floatPx, animations)
                }
            }
        }
    }

    /**
     * Routing 3, one touch on [control] from its down to its up (S-22 "Taps and the long press"): it never drags.
     * A lift within the slop acts ([act]); a move of the slop or more cancels it, and a pointer cancel does nothing.
     * On › the wait for the lift is bounded by [LONG_PRESS_MS] on the pointer node's clock: when it runs out with
     * the finger still down within the slop, `nextUnsolved()` runs then, and the lift does nothing.
     */
    private suspend fun AwaitPointerEventScope.control(first: PointerInputChange, control: Control, slopPx: Float) {
        val classifier = TouchClassifier(slopPx).apply { down(first.position.x, first.position.y) }
        val end = if (control == Control.NEXT) {
            withTimeoutOrNull(LONG_PRESS_MS) { awaitControlEnd(first.id, classifier) }
        } else {
            awaitControlEnd(first.id, classifier)
        }
        when (end) {
            null -> {
                controller.nextUnsolved()
                awaitLift(first.id)
            }
            ControlEnd.LIFTED -> act(control)
            ControlEnd.SLID -> awaitLift(first.id)
            ControlEnd.CANCELLED -> Unit
        }
    }

    /** Waits for the control touch [id] to lift, to move the slop, or to be cancelled; every change is consumed. */
    private suspend fun AwaitPointerEventScope.awaitControlEnd(id: PointerId, classifier: TouchClassifier): ControlEnd {
        while (true) {
            val event = awaitPointerEvent()
            var end: ControlEnd? = null
            for (change in event.changes) {
                if (change.id == id && end == null) {
                    end = when {
                        // A pointer cancel arrives as an up already consumed; a real lift does not (routing 5).
                        change.changedToUpIgnoreConsumed() -> if (change.isConsumed) ControlEnd.CANCELLED else ControlEnd.LIFTED
                        change.pressed && classifier.move(change.position.x, change.position.y) -> ControlEnd.SLID
                        else -> null
                    }
                }
                change.consume()
            }
            if (end != null) return end
        }
    }

    /** Consumes every change until the finger [id] is up (a fired long press, a control slid off): nothing acts. */
    private suspend fun AwaitPointerEventScope.awaitLift(id: PointerId) {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
            val primary = event.changes.firstOrNull { it.id == id }
            if (primary == null || !primary.pressed) return
        }
    }

    /** A control acted on at its lift (S-2Δ): ‹ `previous`, › and Next `next`, the counter `openOverview`, Restart, Retry; the best time is inert. */
    private fun act(control: Control) {
        when (control) {
            Control.PREV -> controller.previous()
            Control.NEXT -> controller.next()
            Control.COUNTER -> controller.openOverview()
            Control.RESTART -> controller.restart()
            Control.RETRY -> controller.retry()
            Control.BEST_TIME -> Unit
            Control.SOLVED_NEXT -> controller.next()
        }
    }

    /** One touch on the board or the tray from its down to its up or cancel, with the twist fingers that join it while it drags. */
    private suspend fun AwaitPointerEventScope.gesture(
        first: PointerInputChange,
        hit: Hit?,
        g: ScreenGeometry,
        slopPx: Float,
        floatPx: Float,
        animations: CoroutineScope,
    ) {
        val classifier = TouchClassifier(slopPx).apply { down(first.position.x, first.position.y) }
        var finger = first.position
        var dragging = false
        var inert = hit == null
        var ended = false
        var twistId: PointerId? = null
        var twist: TwistTracker? = null
        var twistPos = Offset.Zero
        var twistApplied = 0
        try {
            while (!ended) {
                val event = awaitPointerEvent()
                var primary: PointerInputChange? = null
                for (change in event.changes) {
                    if (change.id == first.id) {
                        primary = change
                        continue
                    }
                    // Routing 1: a further touch during a drag is a twist finger and acts on nothing else.
                    if (dragging) {
                        if (twistId == null) {
                            if (change.changedToDownIgnoreConsumed()) {
                                twistId = change.id
                                twistPos = change.position
                                twist = TwistTracker(angleDeg(finger, change.position))
                                twistApplied = 0
                            }
                        } else if (change.id == twistId) {
                            if (change.changedToUpIgnoreConsumed()) {
                                twistId = null // lifting it keeps the turn reached; the drag continues
                                twist = null
                            } else {
                                twistPos = change.position
                            }
                        }
                    }
                    change.consume()
                }
                if (primary != null) {
                    if (primary.changedToUpIgnoreConsumed()) {
                        // Compose reports a pointer cancel (ACTION_CANCEL, the view going away) as an up that
                        // arrives already consumed; a real release does not.
                        val cancelled = primary.isConsumed
                        primary.consume()
                        ended = true
                        when {
                            dragging && cancelled -> cancelDrag(g)
                            dragging -> release(g)
                            !cancelled && !inert && hit != null -> tap(hit)
                        }
                        break
                    }
                    if (primary.pressed) {
                        finger = primary.position
                        primary.consume()
                        if (dragging) {
                            heldFinger = finger // given to the controller in the next frame callback
                        } else if (!inert && hit != null && classifier.move(finger.x, finger.y)) {
                            if (hit is Hit.Badge) {
                                inert = true // a touch that starts on a badge never drags, and slid off it presses nothing
                            } else {
                                dragging = startDrag(hit, finger, g, floatPx, animations)
                                if (!dragging) inert = true
                            }
                        }
                    }
                }
                val tracker = twist
                if (dragging && tracker != null) {
                    val steps = tracker.steps(angleDeg(finger, twistPos))
                    val delta = steps - twistApplied
                    if (delta != 0) {
                        heldSteps += delta // given to the controller in the next frame callback
                        twistApplied = steps
                        heldFinger = finger // the turned shape keeps its lowest point above the finger
                    }
                }
            }
        } catch (e: CancellationException) {
            // The handler itself was cancelled mid-drag (the node reset or left the tree): a missed drop.
            if (dragging && !ended) cancelDrag(g)
            throw e
        }
    }

    // ── the actions ──────────────────────────────────────────────────────────────────────────────────

    /** Routing 3, a tap: `pressBadge` on a badge (REQ-018; a refused board mirror shakes through the controller), `tapTray` for a tray piece, `tapBoard` for a placed one (REQ-016). */
    private fun tap(hit: Hit) {
        when (hit) {
            is Hit.Badge -> controller.pressBadge()
            is Hit.Tray -> controller.tapTray(hit.piece)
            is Hit.Board -> controller.tapBoard(hit.placement.piece)
            is Hit.OnControl -> Unit // never reached: a control touch is routed to `control`
        }
    }

    /**
     * Routing 3, the move that reaches the slop: the piece leaves the tray or the board, is held with its lowest
     * point floating above [finger], and — from the tray — starts growing from where its miniature was drawn.
     *
     * @return false when the controller refused (not the puzzle's piece, a drag running, the puzzle solved).
     */
    private fun startDrag(hit: Hit, finger: Offset, g: ScreenGeometry, floatPx: Float, animations: CoroutineScope): Boolean {
        stopFrames()
        when (hit) {
            is Hit.Badge -> return false // never reached: a badge touch is stopped before a drag can start
            is Hit.OnControl -> return false // never reached: a control touch is routed to `control` and never drags
            is Hit.Tray -> {
                val piece = hit.piece
                val turn = controller.trayTurn(piece)
                val mirrored = controller.trayMirrored(piece)
                val miniature = trayMiniaturePx(controller.engine, piece, turn, mirrored, g) ?: return false
                val held = heldCentre(piece, turn, mirrored, finger, g, floatPx)
                if (!controller.startDrag(piece, held)) return false
                visual.begin(DragOrigin(piece, centroid(miniature), g.trayPxPerUnit, fromTray = true))
                controller.moveDrag(held, overBoard = g.board.contains(finger))
                growJob = animations.launch { onFrames(GROW_MS) { visual.grow = it } }
            }
            is Hit.Board -> {
                val p = hit.placement
                if (!controller.startDrag(p.piece, p.centre)) return false
                visual.begin(DragOrigin(p.piece, g.mapping.toPx(p.centre), g.mapping.pxPerUnit, fromTray = false))
                move(finger, g, floatPx)
            }
        }
        frameJob = animations.launch { everyFrame(g, floatPx) }
        return true
    }

    /**
     * The running drag's frame loop (REQ-021 rules): in each displayed frame's callback, the held input reaches
     * the controller and `updatePreview()` searches the result, before that frame draws the piece and the
     * preview — the only caller of `updatePreview()`, once per frame. It ends with the drag.
     */
    private suspend fun everyFrame(g: ScreenGeometry, floatPx: Float) {
        while (controller.dragged != null) {
            withFrameNanos {
                giveHeldInput(g, floatPx)
                controller.updatePreview()
            }
        }
    }

    /** The held twist steps (`twistDrag`, one `Turned` per net step), then the latest held finger (`moveDrag`). */
    private fun giveHeldInput(g: ScreenGeometry, floatPx: Float) {
        val steps = heldSteps
        val finger = heldFinger
        dropHeldInput()
        if (steps != 0) controller.twistDrag(steps)
        if (finger != null) move(finger, g, floatPx)
    }

    /** Forgets the input no frame has drawn. */
    private fun dropHeldInput() {
        heldSteps = 0
        heldFinger = null
    }

    /** Ends the drag's frame loop and grow, and forgets the input no frame has drawn. */
    private fun stopFrames() {
        frameJob?.cancel()
        frameJob = null
        growJob?.cancel()
        growJob = null
        dropHeldInput()
    }

    /** Routing 3, a move: the piece is held with its lowest point above [finger]; `overBoard` is the finger's. */
    private fun move(finger: Offset, g: ScreenGeometry, floatPx: Float) {
        val d = controller.dragged ?: return
        controller.moveDrag(heldCentre(d.piece, d.turn, d.mirrored, finger, g, floatPx), overBoard = g.board.contains(finger))
    }

    /**
     * Routing 4, a release: `drop()` of the hand as the last displayed frame drew it and searched it — the input
     * that arrived with the lift was never drawn and is dropped first, so the lock is the drawn preview, or home
     * when none was drawn (REQ-021 A1 and A2; code review N3). The piece's node glides from where it was drawn.
     */
    private fun release(g: ScreenGeometry) {
        stopFrames()
        val d = controller.dragged ?: return
        val (centrePx, scale) = drawnDrag(d, visual, g.mapping)
        controller.drop()
        visual.end(d.piece, centrePx, scale)
    }

    /** Routing 4, a pointer cancel: `goHome()` (a missed drop without a pulse); the miniature glides back from where the piece was drawn. */
    private fun cancelDrag(g: ScreenGeometry) {
        stopFrames()
        val d = controller.dragged ?: return
        val (centrePx, scale) = drawnDrag(d, visual, g.mapping)
        controller.goHome()
        visual.end(d.piece, centrePx, scale)
    }

    /**
     * Where the held piece's centre is, in puzzle units, for the finger at [finger]: the shape at the board
     * scale with its lowest point [floatPx] above the finger, horizontally centred on it (REQ-014 rule and
     * ASSUMPTION; touch routing step 3). The shape's extent below its centre is the engine's (O-01).
     */
    private fun heldCentre(piece: PieceId, turn: Turn, mirrored: Boolean, finger: Offset, g: ScreenGeometry, floatPx: Float): Point {
        val below = controller.engine.vertices(piece, turn, mirrored, Point(0.0, 0.0)).maxOf { it.y } * g.mapping.pxPerUnit
        return g.mapping.toUnits(Offset(finger.x, finger.y - floatPx - below.toFloat()))
    }

    /** The screen angle of [to] around [from] in degrees, y down, so that clockwise is positive (S-7's input). */
    private fun angleDeg(from: Offset, to: Offset): Double =
        Math.toDegrees(atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble()))
}
