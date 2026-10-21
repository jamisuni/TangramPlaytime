// Acceptance kit (WO-003, TASK-011): kit-prefixed helpers for the Compose device tests of `PlayScreen`
// (tasks.md § WO-003 › Seams S-1…S-14, the tag table and the touch routing; workorders/WO-003-briefs.md
// § TASK-011 "How to drive the screen"). No token lives in this file.
//
// How the kit drives the screen:
// - every touch is injected on the `play-screen` node, in that node's own px; puzzle points convert
//   through `BoardMappingKey` on `board`; cell and piece positions are node bounds relative to `play-screen`;
// - time is the compose rule's `mainClock` with `autoAdvance` off, advanced frame by frame — never a sleep;
// - the controller is driven on the main thread only (`runOnIdle`);
// - pixels come from `captureToImage()` on `board` or `tray`, read against the geometry (the solution
//   polygons through `BoardMapping`, `ILockEngine.vertices` through the node's keys) before a drawing is blamed.
//
// Amended WO-004 (TASK-019; designs/WO-004-design.md §2): the S-11Δ literal; the S-13Δ tag strings (`KitTag`);
// a kit-owned in-memory `IProgressStore` (`KitDeviceStore`, design §4) and a controller over `Persistence` built
// the way S-19 / S-1Δ / S-15Δ say (`Persistence.load` off the main thread, the controller on the main thread);
// the top-bar, Restart and long-press helpers. No token lives in this file.
//
// Amended WO-005 (TASK-025; tasks.md § WO-005 S-10Δ2 and seam note 3): an optional `devAid`, given to `PlayScreen` in
// `show()` and `recreate()`. It defaults to null, so every WO-003/004 test composes the screen exactly as WO-004 did and
// none sees the aid; only the WO-005 tests pass one. No token lives in this file.
//
// Amended WO-010 (F2, recirculation 3): `scrollTo` — a grid scroll that returns under the paused clock. No token lives in this file.
//
// Amended WO-010 (F3, recirculation 6; a corrected check — decisions.md 2026-10-20 "F3's cause is the REQ-023 A1 leg's inside
// mask, not the product or the data"): `silhouetteBands`, the silhouette's "3 px in" and "3 px out" pixel sets measured from
// the exact outline (`kitOutline`, `kitBands`). `silhouetteMask` (each polygon widened 0.75 px) stays for the legs that do not
// measure from the outline and for the self-check's old construction. No token lives in this file.
//
// Amended WO-008 (TASK-048; designs/WO-008-design.md §2 S-11Δ3, S-15Δ3 and "Kit doors", `play/` device): the `PlayTexts`
// literal gains `hoursMinutes` (review F4); `KitDeviceClock` and `kitTimedControllerOver`, a controller with a time source next
// to `kitControllerOver`, which is unchanged. No token lives in this file.
package app.tangramplaytime.play

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.contracts.ArtShape
import app.tangramplaytime.contracts.Clock
import app.tangramplaytime.contracts.IActiveTime
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.IProgressStore
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PlayTimes
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Settings
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.kernel.ActiveTime
import app.tangramplaytime.kernel.LockEngine
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.util.Collections
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ── The shared fixtures: the real engine, the bundled puzzles, the S-5 and S-11 literals ─────────────

/** The one lock rule (WO § Constraints "Seams": the real `LockEngine` as a test dependency). */
internal val kitEngine: ILockEngine by lazy { LockEngine() }

/** The bundled puzzle files (`Tangrams`, `*.json`) read on the device through `:content` (brief § Inputs). Fixed for the process. */
internal val kitLibrary: IPuzzleLibrary by lazy { PuzzleLibraries.bundled(kitEngine) }

/** S-5, the phone value, written as a literal (the kits write it, `:app` has its constant). */
internal val kitPhoneLayout: PlayLayout = PlayLayout(
    trayRows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    ),
    floatOffsetDp = 30f,
    cellGapDp = 8f,
    sideMarginDp = 8f,
    topBarDp = 66f,
)

/**
 * S-11 kit literal, grown by S-11Δ (designs/WO-004-design.md §2: "Kit literal: "Restart", "Retry", "Next", "Best time",
 * "–", "in progress", "solved", "%1$d / %2$d", "All puzzles", "Done""; design §3 "`KitScreen.kt`'s `PlayTexts` literal grows")
 * and by S-11Δ3 (designs/WO-008-design.md §2: `hoursMinutes` from `time_hours_minutes` = "%1$d h %2$d min"; WO-008 TASK-048).
 */
internal val kitTexts: PlayTexts = PlayTexts(
    sizeSmall = "S",
    sizeMedium = "M",
    sizeLarge = "L",
    titleLanguage = "en",
    restart = "Restart",
    retry = "Retry",
    next = "Next",
    bestTime = "Best time",
    noTime = "–",
    inProgress = "in progress",
    solved = "solved",
    counterFormat = "%1\$d / %2\$d",
    allPuzzles = "All puzzles",
    done = "Done",
    hoursMinutes = "%1\$d h %2\$d min",
)

/** S-1: a fresh controller over the real engine and the bundled puzzles — the fresh start. */
internal fun kitController(onEvent: (PlayEvent) -> Unit = {}): PlayController = PlayController(kitLibrary, kitEngine, onEvent)

internal fun kitFirst(kind: PuzzleKind): PuzzleDefinition = kitLibrary.puzzles.first { it.kind == kind }

/** Every puzzle that uses all seven pieces (full and warm-up; the validity rules, the mini rule). */
internal fun kitFullSetPuzzles(): List<PuzzleDefinition> = kitLibrary.puzzles.filter { it.kind != PuzzleKind.MINI }

internal const val KIT_TOL = 1e-6

// ── The screen under test ─────────────────────────────────────────────────────────────────────────────

/**
 * One `PlayScreen` under the compose rule, with the touch, node, time and pixel helpers the kit uses.
 * `show()` composes it with the clock auto-advancing (first layout), then turns `autoAdvance` off: from
 * then on every test step advances frames itself.
 */
internal class KitScreen(
    val rule: ComposeContentTestRule,
    val controller: PlayController = kitController(),
    /**
     * The DEV aid `PlayScreen` is given (S-10Δ2 `devAid`, WO-005), in [show] and again in [recreate]: the same object
     * across a recreation, as `DevAidHook.installed` is built once per process (S-25). Null (the default) composes the
     * screen without an aid, exactly as WO-004 (S-10Δ2: "Without an aid, the screen is exactly WO-004's").
     */
    val devAid: DevAidHook? = null,
) {
    val density: Float get() = rule.density.density

    fun dp(v: Float): Float = v * density

    fun show(reducedMotion: Boolean = false) {
        rule.mainClock.autoAdvance = true
        rule.setContent {
            PlayScreen(controller, kitPhoneLayout, kitTexts, reducedMotion, Modifier.fillMaxSize(), devAid = devAid)
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        frames(2)
        check(exists(PlayTags.SCREEN)) { "kit: no `play-screen` node after setContent" }
        check(onMain { controller.boardDpPerUnit } > 0.0) { "kit: boardDpPerUnit not set by the first layout" }
    }

    /**
     * Recreates the host activity and shows the screen again over the same [controller] (code review F2). The
     * recreation is the real lifecycle — `ActivityScenario.recreate()`: pause, stop, save the instance state,
     * destroy, then a new activity created from the saved state — and the new activity is given `PlayScreen`
     * exactly as [show] gave it to the first one, through a `ComposeView` of the default id (the saved state of
     * the first composition is restored into it). That is what a rotation or any configuration change does to
     * the app: `MainActivity` shows `PlayScreen(AppGraph.controller, …)` in every new activity, and `AppGraph`'s
     * controller lives once per process and outlives both (S-15). The new composition runs on the rule's
     * `mainClock` (the test environment's recomposer serves every window of the test) and the clock stays
     * paused, so the caller sees every frame of the new composition from its first one.
     */
    fun <A : ComponentActivity> recreate(scenario: ActivityScenario<A>, reducedMotion: Boolean = false) {
        check(!rule.mainClock.autoAdvance) { "kit: recreate() expects the paused clock of show()" }
        scenario.recreate()
        scenario.onActivity { activity ->
            activity.setContentView(
                ComposeView(activity).apply {
                    setContent { PlayScreen(controller, kitPhoneLayout, kitTexts, reducedMotion, Modifier.fillMaxSize(), devAid = devAid) }
                },
            )
        }
        rule.waitForIdle()
        check(exists(PlayTags.SCREEN)) { "kit: no `play-screen` node after the recreation" }
        check(count(PlayTags.SCREEN) == 1) { "kit: ${count(PlayTags.SCREEN)} `play-screen` nodes after the recreation (the old activity's screen is still attached)" }
    }

    /** A kit precondition for a drop that must lock: the finger (not the floating piece) is over the board. */
    fun checkOverBoard(finger: Offset) {
        check(bounds(PlayTags.BOARD).contains(finger)) { "kit: the finger $finger is not over the board ${bounds(PlayTags.BOARD)}; pick a higher piece" }
    }

    // ── time ──

    fun frames(n: Int = 1) {
        repeat(n) { rule.mainClock.advanceTimeByFrame() }
        rule.waitForIdle()
    }

    fun advance(ms: Long) {
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    fun <T> onMain(block: () -> T): T = rule.runOnIdle(block)

    /**
     * A semantics scroll that brings [tag]'s node wholly into its scrolling parent's view (`performScrollTo`), from the paused
     * clock of [show]. *Amended WO-010 (F2, recirculation 3; a corrected check):* ui-test 1.12's `performScrollTo` repeats its
     * scroll until the node needs none, and the scroll it sends is animated on the main clock. Under the paused clock that
     * animation never advances, so the call never returned (on the floor at 106 puzzles: 35 min inside `performScrollTo`, the
     * main thread spinning; with 13 puzzles every grid cell fitted the first screen and no call ever had to scroll). The
     * clock auto-advances for this one call only and is paused again after it; the caller settles afterwards as before.
     * Cited: platform-notes/android.md § Compose test kit, WO-010 "Under a paused clock, `performScrollTo` never returns once
     * a scroll is needed" (run log f2-sB; reviews/WO-010-code-review-delta.md S-2).
     */
    fun scrollTo(tag: String) {
        check(!rule.mainClock.autoAdvance) { "kit: scrollTo() expects the paused clock of show()" }
        rule.mainClock.autoAdvance = true
        try {
            interaction(tag).performScrollTo()
        } finally {
            rule.mainClock.autoAdvance = false
        }
    }

    /** S-2 `open` on the main thread, then two frames. */
    fun open(id: String) {
        onMain { check(controller.open(id)) { "kit: open($id) refused" } }
        frames(2)
    }

    val puzzle: PuzzleDefinition get() = onMain { controller.puzzle }

    // ── nodes ──

    fun interaction(tag: String): SemanticsNodeInteraction = rule.onNodeWithTag(tag, useUnmergedTree = true)

    fun node(tag: String): SemanticsNode = interaction(tag).fetchSemanticsNode("kit: node `$tag`")

    fun exists(tag: String): Boolean = rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    fun count(tag: String): Int = rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    fun nodes(tag: String): List<SemanticsNode> = rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()

    fun screenOrigin(): Offset = node(PlayTags.SCREEN).boundsInRoot.topLeft

    /** The node's bounds in `play-screen` px — the space every touch is injected in. */
    fun bounds(tag: String): Rect = node(tag).boundsInRoot.translate(-screenOrigin())

    fun <T> key(tag: String, k: SemanticsPropertyKey<T>): T =
        node(tag).config.getOrNull(k) ?: throw AssertionError("kit: `$tag` carries no ${k.name}")

    fun <T> keyOrNull(tag: String, k: SemanticsPropertyKey<T>): T? = node(tag).config.getOrNull(k)

    /** The merged text of a merged, inert node (the title, a size mark). */
    fun text(tag: String): String {
        val n = rule.onNodeWithTag(tag).fetchSemanticsNode("kit: merged node `$tag`")
        return n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
    }

    fun mapping(): BoardMapping = key(PlayTags.BOARD, PlaySemantics.BoardMappingKey)

    fun toPx(p: Point): Offset = mapping().toPx(p)

    fun toUnits(o: Offset): Point = mapping().toUnits(o)

    fun pxPerUnit(): Float = mapping().pxPerUnit

    /** The tags of every node and the texts / content descriptions of every node, unmerged tree, sorted. */
    fun tagsAndTexts(): Pair<List<String>, List<String>> {
        val tags = ArrayList<String>()
        val texts = ArrayList<String>()
        fun walk(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.TestTag)?.let { tags += it }
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { texts += it.text }
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { texts += it }
            n.children.forEach { walk(it) }
        }
        walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        return tags.sorted() to texts.sorted()
    }

    /** Which of the three homes (tray, board, dragged) hold [piece] right now (tag table). */
    fun homesOf(piece: PieceId): List<String> = listOf(
        PlayTags.trayPiece(piece), PlayTags.boardPiece(piece), PlayTags.dragPiece(piece),
    ).filter { exists(it) }

    // ── geometry read from the nodes ──

    /** A board or drag piece's corners in `play-screen` px, from its keys through the engine (O-01). */
    fun boardPolygonPx(tag: String): List<Offset> {
        val piece = key(tag, PlaySemantics.PieceKey)
        val turn = key(tag, PlaySemantics.TurnKey)
        val mirrored = key(tag, PlaySemantics.MirroredKey)
        val centre = key(tag, PlaySemantics.CentreKey)
        return kitEngine.vertices(piece, turn, mirrored, centre).map { toPx(it) }
    }

    /** A tray piece's corners in `play-screen` px: the shape at its keys, fitted into the node's box. */
    fun trayPolygonPx(piece: PieceId): List<Offset> {
        val tag = PlayTags.trayPiece(piece)
        val turn = key(tag, PlaySemantics.TurnKey)
        val mirrored = key(tag, PlaySemantics.MirroredKey)
        val scale = key(tag, PlaySemantics.DrawnPxPerUnitKey)
        val box = bounds(tag)
        val v = kitEngine.vertices(piece, turn, mirrored, Point(0.0, 0.0))
        val minX = v.minOf { it.x }
        val minY = v.minOf { it.y }
        return v.map { Offset(box.left + ((it.x - minX) * scale).toFloat(), box.top + ((it.y - minY) * scale).toFloat()) }
    }

    /**
     * How far (px) the drawn piece [tag] is from a rest pose: its CentreKey from [rest] and each edge of its box
     * from [restBox] (tag table: a piece's bounds and CentreKey follow the drawing in each frame).
     */
    fun displacementPx(tag: String, rest: Point, restBox: Rect): Float {
        val c = toPx(key(tag, PlaySemantics.CentreKey)) - toPx(rest)
        val b = bounds(tag)
        return maxOf(hypot(c.x, c.y), abs(b.left - restBox.left), abs(b.top - restBox.top), abs(b.right - restBox.right), abs(b.bottom - restBox.bottom))
    }

    /** The unit extent of [piece] at [turn]/[mirrored]: width × height of its corner box. */
    fun unitExtent(piece: PieceId, turn: Turn, mirrored: Boolean): Pair<Double, Double> {
        val v = kitEngine.vertices(piece, turn, mirrored, Point(0.0, 0.0))
        return (v.maxOf { it.x } - v.minOf { it.x }) to (v.maxOf { it.y } - v.minOf { it.y })
    }

    // ── touch ──

    fun touch(block: TouchInjectionScope.() -> Unit) {
        rule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).performTouchInput(block)
    }

    /** A tap: down and up at [at] with no movement (the tap rule: under 12 dp of movement, whatever its length). */
    fun tap(at: Offset) {
        touch { down(at); up() }
        frames()
    }

    /**
     * A point inside [piece]'s tray cell that is not inside the tray badge's 60 dp area (the badge branch of
     * the touch routing wins inside the PG cell; decisions.md "badge touch precedence").
     */
    fun trayTapPoint(piece: PieceId): Offset {
        val cell = bounds(PlayTags.trayCell(piece))
        val avoid = if (piece == PieceId.PG && exists(PlayTags.FLIP_BADGE_TRAY)) bounds(PlayTags.FLIP_BADGE_TRAY) else null
        val candidates = listOf(
            cell.center,
            Offset(cell.left + cell.width * 0.25f, cell.top + cell.height * 0.75f),
            Offset(cell.left + cell.width * 0.75f, cell.top + cell.height * 0.75f),
            Offset(cell.left + cell.width * 0.25f, cell.top + cell.height * 0.25f),
            Offset(cell.left + cell.width * 0.75f, cell.top + cell.height * 0.25f),
        )
        return candidates.firstOrNull { avoid == null || !avoid.contains(it) }
            ?: throw AssertionError("kit: the badge area $avoid covers every tap point of the PG cell $cell")
    }

    /** Taps [piece] in the tray [times] times (the tap-to-turn rule: one step clockwise per tap). */
    fun tapTray(piece: PieceId, times: Int) {
        repeat(times) {
            tap(trayTapPoint(piece))
            advance(48)
        }
    }

    /**
     * Picks [piece] up from its tray cell by touch: down in the cell, a 20 dp move (past the 12 dp slop, so
     * the touch is a drag), then the 120 ms grow (the drag rule) plus one frame. Returns the finger position.
     */
    fun pickUpFromTray(piece: PieceId): Offset {
        val start = trayTapPoint(piece)
        touch { down(start) }
        frames()
        val lifted = Offset(start.x, start.y - dp(20f))
        touch { moveTo(lifted) }
        frames()
        advance(120)
        frames()
        check(exists(PlayTags.dragPiece(piece))) { "kit: no `drag-piece` for $piece after a 20 dp move from its cell" }
        return lifted
    }

    /** Picks a placed [piece] up by touch at its drawn centre, moving 20 dp (past the slop). */
    fun pickUpFromBoard(piece: PieceId): Offset {
        val start = toPx(key(PlayTags.boardPiece(piece), PlaySemantics.CentreKey))
        touch { down(start) }
        frames()
        val lifted = Offset(start.x, start.y - dp(20f))
        touch { moveTo(lifted) }
        frames()
        check(exists(PlayTags.dragPiece(piece))) { "kit: no `drag-piece` for $piece after a 20 dp move from its centre" }
        return lifted
    }

    /** Moves the finger from [from] to [to] in [steps] moves, one frame each. Returns [to]. */
    fun moveFinger(from: Offset, to: Offset, steps: Int = 4): Offset {
        for (i in 1..steps) {
            val f = Offset(from.x + (to.x - from.x) * i / steps, from.y + (to.y - from.y) * i / steps)
            touch { moveTo(f) }
            frames()
        }
        return to
    }

    /**
     * Steers the dragged [piece] so that its drawn centre lands on [target] (puzzle units), moving the finger
     * by the difference between the piece's `CentreKey` and the target (the piece floats above the finger
     * at a constant offset, the floating rule of the drag). Returns the finger position.
     */
    fun steerDragTo(piece: PieceId, target: Point, finger: Offset, steps: Int = 6): Offset {
        var f = finger
        repeat(4) {
            val cur = toPx(key(PlayTags.dragPiece(piece), PlaySemantics.CentreKey))
            val tgt = toPx(target)
            val d = tgt - cur
            if (hypot(d.x, d.y) < 0.75f) return f
            f = moveFinger(f, f + d, steps)
            frames()
        }
        val cur = toPx(key(PlayTags.dragPiece(piece), PlaySemantics.CentreKey))
        val off = cur - toPx(target)
        check(hypot(off.x, off.y) <= 1.5f) { "kit: could not steer $piece onto $target (off by $off px)" }
        return f
    }

    fun release() {
        touch { up() }
        frames()
    }

    // ── the controller through S-2 (main thread) ──

    /**
     * Places [piece] at its stored solution pose through S-2 — a set-up, never a gesture (brief: "Set a board
     * up through S-2 only where the line is not about the gesture"). Returns false, with nothing changed,
     * when the engine says that pose does not lock on the current board (the fixture reads the geometry).
     */
    fun placeByApi(piece: PieceId): Boolean = onMain {
        val p = controller.puzzle
        val sp = p.solution.single { it.piece == piece }
        val pose = kitPoseOf(piece, sp.polygon)
        val lock = kitEngine.findLock(p, controller.placed, piece, pose.turn, pose.mirrored, pose.centre, controller.boardDpPerUnit)
        if (lock !is LockResult.Locked) return@onMain false
        if (piece == PieceId.PG && controller.trayMirrored(PieceId.PG) != pose.mirrored) {
            check(controller.pressBadge()) { "kit: pressBadge() refused with PG in the tray" }
        }
        check(controller.startDrag(piece, pose.centre)) { "kit: startDrag($piece) refused" }
        val from = controller.dragged?.turn?.step ?: error("kit: no drag after startDrag")
        val steps = ((pose.turn.step - from) % 8 + 8) % 8
        if (steps != 0) controller.twistDrag(if (steps <= 4) steps else steps - 8)
        controller.moveDrag(pose.centre, overBoard = true)
        controller.updatePreview()
        controller.drop() != null
    }

    /** Places every piece of the open puzzle at its solution pose (edge-first, as the engine allows). */
    fun solveByApi() {
        val p = puzzle
        var remaining = p.pieces.toList()
        while (remaining.isNotEmpty()) {
            val placedNow = remaining.filter { placeByApi(it) }
            check(placedNow.isNotEmpty()) { "kit: none of $remaining locks at its solution pose in ${p.id}" }
            remaining = remaining - placedNow.toSet()
            frames()
        }
    }

    /** S-2 `placed`, read on the main thread. */
    fun placed(): List<Placement> = onMain { controller.placed.toList() }

    // ── pixels ──

    fun capture(tag: String): PixelMap = interaction(tag).captureToImage().toPixelMap()

    /** A `play-screen` px point in the coordinates of [tag]'s capture. */
    fun toCapture(tag: String, p: Offset): Offset = p - bounds(tag).topLeft

    /** The silhouette's solution polygons in the `board` capture's px. */
    fun solutionPolygonsInBoardPx(p: PuzzleDefinition = puzzle): List<List<Offset>> {
        val origin = bounds(PlayTags.BOARD).topLeft
        return p.solution.map { sp -> sp.polygon.map { toPx(it) - origin } }
    }

    /**
     * The silhouette mask in the `board` capture's px (each solution polygon widened by 0.75 px). Not a measure of distance
     * from the outline: eroded or dilated by a square it comes as close as 0.46 px to the outline at a 315° notch (WO-010 F3);
     * a "3 px in" or "3 px out" set is [silhouetteBands].
     */
    fun silhouetteMask(p: PuzzleDefinition = puzzle): KitMask {
        val b = bounds(PlayTags.BOARD)
        return kitRasterise(solutionPolygonsInBoardPx(p), b.width.roundToInt(), b.height.roundToInt())
    }

    /**
     * The silhouette's "[marginPx] in" and "[marginPx] out" sets in the `board` capture's px, measured from the exact outline
     * (decisions.md 2026-10-20 "F3's cause is the REQ-023 A1 leg's inside mask, not the product or the data": "3 px in" and
     * "3 px out" are measured from the exact outline): the pixels whose centres lie in the union of the solution polygons at
     * least [marginPx] from its outline ([KitBands.inside]), and those whose centres lie outside it at least [marginPx] from it
     * ([KitBands.outside]). One `board` bounds read and one `BoardMappingKey` read; nothing is read per pixel.
     * The 3 px margin holds because the picture's outline stroke reaches inside only its inner half-width, 0.6 dp × density (1.6 px at 2.625, 1.2 px at 2.0), plus anti-aliasing.
     */
    fun silhouetteBands(p: PuzzleDefinition = puzzle, marginPx: Double = 3.0): KitBands {
        val b = bounds(PlayTags.BOARD)
        val m = mapping()
        val outline = kitOutline(p).map { s -> KitSegPx(m.toPx(s.a) - b.topLeft, m.toPx(s.b) - b.topLeft) }
        return kitBands(kitPolygonsPx(p, m, b.topLeft), outline, b.width.roundToInt(), b.height.roundToInt(), marginPx)
    }

    // ── WO-004 (TASK-019): the top bar, the controls, the long press ──

    /** The puzzle `play-screen` shows (S-14 `PuzzleIdKey`). */
    fun shownId(): String = key(PlayTags.SCREEN, PlaySemantics.PuzzleIdKey)

    /** The shown puzzle's TYPE-006 state (S-14 `PuzzleStateKey`), the product's claim: cross-checked where a line rests on it. */
    fun shownState(): PuzzleState = key(PlayTags.SCREEN, PlaySemantics.PuzzleStateKey)

    /**
     * A still tap at the centre of [tag]'s bounds — S-13Δ: "A control's bounds are its touch area" — then one more frame
     * (a control acts at the lift, decisions.md 2026-10-15 "Browsing readings no REQ fixes"; the next frame composes it).
     */
    fun press(tag: String) {
        tap(bounds(tag).center)
        frames()
    }

    /**
     * A finger put down at [at] and held still for [ms] on the paused clock; it stays down. The hold's timeout runs on the
     * pointer node's clock, which is `mainClock` under the test rule (design §3, library fact 5), so nothing sleeps.
     */
    fun holdDown(at: Offset, ms: Long) {
        touch { down(at) }
        frames()
        advance(ms)
    }

    /** Lifts the one finger that is down, then one frame. */
    fun lift() {
        touch { up() }
        frames()
    }

    /** Every node with a text in [tag]'s unmerged subtree, with that text (seam note 4: texts are merged, inert nodes). */
    fun textNodesUnder(tag: String): List<Pair<SemanticsNode, String>> {
        val out = ArrayList<Pair<SemanticsNode, String>>()
        fun walk(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.Text)?.let { t -> out += n to t.joinToString("") { it.text } }
            n.children.forEach { walk(it) }
        }
        walk(node(tag))
        return out
    }

    /** Every node of the unmerged tree whose test tag starts with [prefix], with its tag. */
    fun nodesWithTagPrefix(prefix: String): List<Pair<String, SemanticsNode>> {
        val out = ArrayList<Pair<String, SemanticsNode>>()
        fun walk(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.TestTag)?.let { if (it.startsWith(prefix)) out += it to n }
            n.children.forEach { walk(it) }
        }
        walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        return out
    }

    /** The node's unclipped bounds in `play-screen` px (its position and size, whatever a scroll container clips). */
    fun unclippedBounds(n: SemanticsNode): Rect {
        val o = n.positionInRoot - screenOrigin()
        return Rect(o.x, o.y, o.x + n.size.width, o.y + n.size.height)
    }

    /**
     * The state word the top bar shows, or "" when it shows none (decisions.md 2026-10-15 "The top bar, Restart and the
     * solved row, as drawn": "a state word for In progress and Solved, with none for New"; S-13Δ `state-word`).
     */
    fun stateWord(): String = if (exists(KitTag.STATE_WORD)) text(KitTag.STATE_WORD) else ""

    /**
     * Asserts that the screen shows the puzzle [id]: `play-screen`'s PuzzleIdKey (the product's claim), cross-checked
     * against the drawing — the title's text (S-11: `titles[titleLanguage]`), the `silhouette` node's box against [id]'s
     * solution outline through BoardMapping (tag table), and the "n / N" text inside `counter` (seam note 4; S-11Δ
     * `counterFormat`; n is [id]'s position in ‹ › order, REQ-040).
     */
    fun assertShows(id: String, what: String) {
        val shown = shownId()
        if (shown != id) throw AssertionError("$what: `play-screen` shows $shown (${kitNumberOf(shown)} / ${kitLibrary.puzzles.size}), expected $id (${kitNumberOf(id)} / ${kitLibrary.puzzles.size})")
        val p = kitLibrary.byId(id) ?: throw AssertionError("$what: $id is not a bundled puzzle")
        val title = text(PlayTags.TITLE)
        if (title != p.titles.getValue(kitTexts.titleLanguage)) throw AssertionError("$what: the title reads '$title', expected '${p.titles.getValue(kitTexts.titleLanguage)}' ($id)")
        val all = p.solution.flatMap { it.polygon }
        val expected = Rect(toPx(Point(all.minOf { it.x }, all.minOf { it.y })), toPx(Point(all.maxOf { it.x }, all.maxOf { it.y })))
        kitAssertBoxNear("$what: `silhouette` box vs $id's solution outline", expected, bounds(PlayTags.SILHOUETTE), 2f)
        val number = kitCounterText(id)
        val texts = textNodesUnder(KitTag.COUNTER).map { it.second }
        if (number !in texts) throw AssertionError("$what: no '$number' text inside `counter` (its texts: $texts)")
    }

    /**
     * A point of the `restart` pill (S-13Δ: its bounds are its touch area) clear of everything S-22's hit order puts
     * before it or level with it: outside every board piece's drawn shape grown by 12 dp (REQ-015's margin) and outside
     * the board badge's area. The point of the pill farthest from the pieces.
     */
    fun restartTapPoint(): Offset {
        val pill = bounds(KitTag.RESTART)
        val shapes = PieceId.entries.filter { exists(PlayTags.boardPiece(it)) }
            .map { boardPolygonPx(PlayTags.boardPiece(it)).map { o -> Point(o.x.toDouble(), o.y.toDouble()) } }
        val badge = if (exists(PlayTags.FLIP_BADGE_BOARD)) bounds(PlayTags.FLIP_BADGE_BOARD) else null
        var best: Offset? = null
        var bestClear = -1f
        val step = dp(2f)
        var y = pill.top + 2f
        while (y <= pill.bottom - 2f) {
            var x = pill.left + 2f
            while (x <= pill.right - 2f) {
                val q = Offset(x, y)
                x += step
                if (badge != null && badge.contains(q)) continue
                val pq = Point(q.x.toDouble(), q.y.toDouble())
                val clear = shapes.minOfOrNull { if (kitInside(pq, it)) -1.0 else kitDistToBoundary(pq, it) }?.toFloat() ?: Float.MAX_VALUE
                if (clear > bestClear) { best = q; bestClear = clear }
            }
            y += step
        }
        val found = best
        check(found != null && bestClear > dp(12f)) { "kit: no point of the `restart` pill $pill is clear of the pieces' 12 dp margins (best ${bestClear / density} dp)" }
        return found
    }

    /** Every piece of [p] is in the tray, its miniature inside its own cell, nothing of it on the board (tag table). */
    fun assertAllInTray(p: PuzzleDefinition, what: String) {
        for (piece in p.pieces) {
            val homes = homesOf(piece)
            if (homes != listOf(PlayTags.trayPiece(piece))) throw AssertionError("$what: $piece is at $homes, not in the tray")
            val cell = bounds(PlayTags.trayCell(piece))
            val box = bounds(PlayTags.trayPiece(piece))
            if (!(cell.contains(box.topLeft) && cell.contains(box.bottomRight))) throw AssertionError("$what: $piece's miniature $box is not inside its cell $cell")
        }
    }

    /** The board as `board-piece` nodes show it: each one's piece, turn, mirror and centre (S-14 keys), in piece order. */
    fun boardFromNodes(): List<Placement> = PieceId.entries.filter { exists(PlayTags.boardPiece(it)) }.map {
        val tag = PlayTags.boardPiece(it)
        Placement(key(tag, PlaySemantics.PieceKey), key(tag, PlaySemantics.TurnKey), key(tag, PlaySemantics.MirroredKey), key(tag, PlaySemantics.CentreKey))
    }

    /**
     * The keys of every `board-piece` node read against the drawing: the node's box is the box of the engine's corners at
     * its keys (tag table: bounds follow the drawing), and the pixels 3 px inside that polygon are the piece's colour
     * (S-12 `PlayPalette.piece`), away from the board badge (drawn over pieces).
     */
    fun assertBoardDrawnAtItsKeys(what: String) {
        val board = capture(PlayTags.BOARD)
        val boardBounds = bounds(PlayTags.BOARD)
        val avoid = listOfNotNull(if (exists(PlayTags.FLIP_BADGE_BOARD)) bounds(PlayTags.FLIP_BADGE_BOARD) else null)
        for (piece in PieceId.entries) {
            val tag = PlayTags.boardPiece(piece)
            if (!exists(tag)) continue
            val poly = boardPolygonPx(tag)
            kitAssertBoxNear("$what: `$tag` box vs the engine's corners at its keys", kitBox(poly), bounds(tag), 2f)
            val samples = kitInteriorSamples(poly, 3f, avoid, within = boardBounds)
            if (samples.size < 8) throw AssertionError("$what: only ${samples.size} interior samples of $piece (kit fixture)")
            val colour = PlayPalette.piece(piece)
            val wrong = samples.filter { s -> !kitNear(board[(s.x - boardBounds.left).toInt(), (s.y - boardBounds.top).toInt()], colour, 16f / 255f) }
            if (wrong.isNotEmpty()) {
                val s = wrong.first()
                throw AssertionError("$what: ${wrong.size} of ${samples.size} pixels 3 px inside $piece's polygon are not ${kitHex(colour)}; first $s = ${kitHex(board[(s.x - boardBounds.left).toInt(), (s.y - boardBounds.top).toInt()])}")
            }
        }
    }
}

// ── WO-004 (TASK-019): tags, numbers, the store double, a controller over Persistence ─────────────────

/**
 * The S-13Δ tag strings. The design's seam row fixes the strings (designs/WO-004-design.md §2 S-13Δ), not the names of
 * `PlayTags`' members, so the kit spells them itself. S-13Δ: "A control's bounds are its touch area."
 */
internal object KitTag {
    const val NAV_PREV = "nav-prev"
    const val NAV_NEXT = "nav-next"
    const val COUNTER = "counter"
    const val RATING = "rating"
    const val STATE_WORD = "state-word"
    const val RESTART = "restart"
    const val SOLVED_ROW = "solved-row"
    const val RETRY = "retry"
    const val BEST_TIME = "best-time"
    const val SOLVED_NEXT = "solved-next"
    const val OVERVIEW = "overview"
    const val OVERVIEW_DONE = "overview-done"
    const val OVERVIEW_CELL_PREFIX = "overview-cell-"
    const val PLAY_LOADING = "play-loading"

    fun overviewCell(id: String): String = OVERVIEW_CELL_PREFIX + id
}

/** The 1-based position of [id] in ‹ › order (REQ-040; `IPuzzleLibrary.puzzles`), 0 when it is not listed. */
internal fun kitNumberOf(id: String): Int = kitLibrary.puzzles.indexOfFirst { it.id == id } + 1

/** The puzzle after [id] in ‹ › order, wrapping (REQ-024 rule: "The list wraps"). */
internal fun kitAfter(id: String): PuzzleDefinition = kitLibrary.puzzles[kitNumberOf(id) % kitLibrary.puzzles.size]

/** The puzzle before [id] in ‹ › order, wrapping (REQ-024 rule: "‹ on the first puzzle shows the last"). */
internal fun kitBefore(id: String): PuzzleDefinition = kitLibrary.puzzles[(kitNumberOf(id) - 2 + kitLibrary.puzzles.size) % kitLibrary.puzzles.size]

/** The "n / N" text for [id]: S-11Δ `counterFormat` ("%1$d / %2$d") with its position and the library's size. */
internal fun kitCounterText(id: String): String = kitTexts.counterFormat.format(kitNumberOf(id), kitLibrary.puzzles.size)

/**
 * The kit's in-memory `IProgressStore` (designs/WO-004-design.md §4: the controller kits run over a kit-owned double; the
 * file store is the oracle of governance row 11 and of the death pair only). It keeps what it is given (IProgressStore
 * KDoc: "The store keeps what it is given"), reads a puzzle without an entry as `PuzzleProgress.FRESH` (KDoc of
 * `progress`), and logs every `saveProgress` in order. Thread-safe: `Persistence`'s writer calls it off the main thread
 * while the test thread reads it.
 */
internal class KitDeviceStore(
    seed: Map<String, PuzzleProgress> = emptyMap(),
    lastOpened: String? = null,
) : IProgressStore {
    private val lock = Any()
    private val puzzles = LinkedHashMap(seed)
    private var last: String? = lastOpened
    private var times: PlayTimes = PlayTimes.NONE
    private var prefs: Settings = Settings()
    private val saveLog = ArrayList<Pair<String, PuzzleProgress>>()

    /** Every saved puzzle's progress now (a copy). */
    fun snapshot(): Map<String, PuzzleProgress> = synchronized(lock) { LinkedHashMap(puzzles) }

    /** [id]'s saved progress now, `PuzzleProgress.FRESH` when none is saved. */
    fun progressNow(id: String): PuzzleProgress = synchronized(lock) { puzzles[id] ?: PuzzleProgress.FRESH }

    /** The last-opened id saved now. */
    fun lastOpenedNow(): String? = synchronized(lock) { last }

    /** Every `saveProgress` call so far, in order. */
    fun saves(): List<Pair<String, PuzzleProgress>> = synchronized(lock) { ArrayList(saveLog) }

    override suspend fun progress(puzzleId: String): PuzzleProgress = progressNow(puzzleId)

    override suspend fun saveProgress(puzzleId: String, progress: PuzzleProgress) {
        synchronized(lock) {
            puzzles[puzzleId] = progress
            saveLog += puzzleId to progress
        }
    }

    override suspend fun allProgress(): Map<String, PuzzleProgress> = snapshot()

    override suspend fun playTimes(): PlayTimes = synchronized(lock) { times }

    override suspend fun savePlayTimes(times: PlayTimes) {
        synchronized(lock) { this.times = times }
    }

    override suspend fun settings(): Settings = synchronized(lock) { prefs }

    override suspend fun saveSettings(settings: Settings) {
        synchronized(lock) { prefs = settings }
    }

    override suspend fun lastOpenedPuzzleId(): String? = lastOpenedNow()

    override suspend fun saveLastOpenedPuzzleId(id: String) {
        synchronized(lock) { last = id }
    }

    /** KDoc: every puzzle FRESH and the play times NONE afterwards; settings and the last-opened id kept. */
    override suspend fun resetAll() {
        synchronized(lock) {
            puzzles.clear()
            times = PlayTimes.NONE
        }
    }
}

/** How long a kit load or flush may take before the test fails instead of hanging (nothing here sleeps). */
internal const val KIT_DEVICE_IO_TIMEOUT_MS: Long = 20_000L

/** A controller over `Persistence` over the kit's [store] (S-19, S-1Δ), with the `Persistence` it saves through. */
internal class KitPersisted(
    val store: KitDeviceStore,
    val persistence: Persistence,
    val controller: PlayController,
    private val failures: MutableList<Throwable>,
) {
    /**
     * S-19 `flush()`: returns once every queued write has reached [store]. Called from the test thread, never the main
     * one. A failure on the writer's scope (recorded by its handler) fails the test here rather than vanishing.
     */
    fun flush() {
        runBlocking { withTimeout(KIT_DEVICE_IO_TIMEOUT_MS) { persistence.flush() } }
        val seen = synchronized(failures) { failures.toList() }
        if (seen.isNotEmpty()) throw AssertionError("Persistence's writer failed before the flush: ${seen.first()}", seen.first())
    }
}

/**
 * Builds a controller the way the app does (S-15Δ: the load on IO, the controller on Main; S-19 `Persistence.load`; S-1Δ
 * `PlayController(library, engine, persistence, onEvent)`): `Persistence.load` over [store] off the main thread, on a
 * supervisor scope on a background dispatcher whose failures are recorded, then the controller on the main thread, over
 * the real engine and the bundled puzzles.
 */
internal fun kitControllerOver(rule: ComposeContentTestRule, store: KitDeviceStore, onEvent: (PlayEvent) -> Unit = {}): KitPersisted {
    val failures = Collections.synchronizedList(ArrayList<Throwable>())
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, t -> failures += t })
    val persistence = runBlocking(Dispatchers.IO) { withTimeout(KIT_DEVICE_IO_TIMEOUT_MS) { Persistence.load(store, scope) } }
    val controller = rule.runOnUiThread { PlayController(kitLibrary, kitEngine, persistence, onEvent) }
    return KitPersisted(store, persistence, controller, failures)
}

// ── WO-008 (TASK-048): a controller with a time source ──────────────────────────────────────────────────

/** Where the device kit's clock starts: any reading far from zero (Clock KDoc: "the zero point is unspecified"). */
internal const val KIT_DEVICE_CLOCK_START_MS: Long = 50_000_000L

/**
 * The play device kit's `Clock` (ADR-006; G-14 "Tests set the clock"; designs/WO-008-design.md §2 "Kit doors", `play/` device:
 * "The same doors; the kit calls `time.touched()` in place of `:app`'s hook"). Its reading moves only when a test moves it, and a
 * test moves it on the main thread, where the controller and the kernel read it (platform-notes/android.md § Compose test kit:
 * the clock is advanced "on the main thread, never `Thread.sleep`"; S-44 "One caller thread"). It is not the compose rule's
 * `mainClock`, which paces the frames only.
 */
internal class KitDeviceClock(start: Long = KIT_DEVICE_CLOCK_START_MS, var date: LocalDate = LocalDate.of(2026, 10, 20)) : Clock {
    @Volatile
    var millis: Long = start
        private set

    override fun nowMillis(): Long = millis

    override fun today(): LocalDate = date

    /** Moves the reading [seconds] forward (call it on the main thread). */
    fun forward(seconds: Int) {
        millis += seconds * 1000L
    }
}

/** A controller over `Persistence` with a time source ([kitTimedControllerOver]): the boot, the kit's [clock], the kernel's [time]. */
internal class KitTimedPersisted(val persisted: KitPersisted, val clock: KitDeviceClock, val time: IActiveTime) {
    val controller: PlayController get() = persisted.controller
    val store: KitDeviceStore get() = persisted.store
}

/**
 * [kitControllerOver] with a time source, in `AppGraph.start`'s order (designs/WO-008-design.md §2 S-15Δ3: it "builds
 * ActiveTime(AppClock, persistence.playTimes) and the controller with it"): `Persistence.load` over [store] off the main thread
 * (S-19Δ: "`load` also reads both" the play times and the settings, so a setting saved on [store] before this call is the one the
 * controller reads, S-1Δ3 `timerShown`), then, on the main thread, the real `ActiveTime` (S-45) over [clock] and the play times
 * `Persistence` read, and `PlayController(library, engine, persistence, time, onEvent)` (S-1Δ3). It calls no `setVisible`: the
 * test makes the game visible itself, as `AppGraph.start` does last ("then calls setVisible(visibleNow)").
 */
internal fun kitTimedControllerOver(
    rule: ComposeContentTestRule,
    store: KitDeviceStore,
    clock: KitDeviceClock = KitDeviceClock(),
    onEvent: (PlayEvent) -> Unit = {},
): KitTimedPersisted {
    val failures = Collections.synchronizedList(ArrayList<Throwable>())
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, t -> failures += t })
    val persistence = runBlocking(Dispatchers.IO) { withTimeout(KIT_DEVICE_IO_TIMEOUT_MS) { Persistence.load(store, scope) } }
    val built = rule.runOnUiThread {
        val time: IActiveTime = ActiveTime(clock, persistence.playTimes)
        time to PlayController(kitLibrary, kitEngine, persistence, time, onEvent)
    }
    return KitTimedPersisted(KitPersisted(store, persistence, built.second, failures), clock, built.first)
}

/**
 * A full-set puzzle and three of its solution locks, in an order the engine places them in, for a "come back to the
 * same three" fixture that can show a lost mirror (REQ-025 rule: "Saved per piece: tray or board, position, turn,
 * mirror"): the first non-mini puzzle whose parallelogram is mirrored in its solution and comes among the first three
 * locks when it is tried first; else the first full-set puzzle's first three solution locks.
 */
internal fun kitThreeToLeave(): Pair<PuzzleDefinition, List<Placement>> {
    for (p in kitLibrary.puzzles.filter { it.kind != PuzzleKind.MINI && PieceId.PG in it.pieces }) {
        if (!kitPoseOf(PieceId.PG, p.solution.single { it.piece == PieceId.PG }.polygon).mirrored) continue
        val order = listOf(PieceId.PG) + p.solution.map { it.piece }.filter { it != PieceId.PG }
        val all = kitSolutionLocks(p, p.pieces.size, order = order)
        if (all.indexOfFirst { it.piece == PieceId.PG } in 0..2) return p to all.take(3)
    }
    android.util.Log.i("KitScreen", "no puzzle locks a mirrored parallelogram among three; the three-piece fixture shows no mirror")
    val p = kitFirst(PuzzleKind.FULL)
    return p to kitSolutionLocks(p, 3)
}

/**
 * A `dpPerUnit` for engine searches made before a screen exists. A search from a piece's exact solution pose does not
 * depend on it: the lock distance is never below 0.65 units (TYPE-004), wider than the 0.16-unit band in which the
 * winning candidate is chosen, so every candidate that can win is in reach at any scale.
 */
internal const val KIT_ANY_DP = 100.0

/**
 * Up to [count] of [p]'s pieces at their stored solution poses, placed in [order] as the engine allows (each one the
 * `Locked` result of `findLock` from its exact pose against the ones before it, and equal to that pose): saved placements
 * that are valid locks, so `stillValid` keeps every one (REQ-025 rule; ILockEngine KDoc). [skip] pieces are never used.
 */
internal fun kitSolutionLocks(
    p: PuzzleDefinition,
    count: Int,
    order: List<PieceId> = p.solution.map { it.piece },
    skip: Set<PieceId> = emptySet(),
): List<Placement> {
    val out = ArrayList<Placement>()
    var progress = true
    while (out.size < count && progress) {
        progress = false
        for (piece in order) {
            if (out.size >= count) break
            if (piece in skip || out.any { it.piece == piece }) continue
            val pose = kitPoseOf(piece, p.solution.single { it.piece == piece }.polygon)
            val exact = Placement(piece, pose.turn, pose.mirrored, pose.centre)
            val lock = kitEngine.findLock(p, out, piece, pose.turn, pose.mirrored, pose.centre, KIT_ANY_DP)
            if (lock is LockResult.Locked && kitSamePlacement(lock.placement, exact)) {
                out += lock.placement
                progress = true
            }
        }
    }
    return out
}

/** [p]'s pieces ordered bottom first (by the lowest point of their solution polygon, y down): placements far from the board's top-left. */
internal fun kitBottomFirst(p: PuzzleDefinition): List<PieceId> = p.solution.sortedByDescending { s -> s.polygon.maxOf { it.y } }.map { it.piece }

/** A Solved puzzle's saved progress (IProgressStore KDoc of `placed`: empty when Solved), with a time and a best time. */
internal fun kitSolvedProgress(time: Int = 61, best: Int? = 61): PuzzleProgress = PuzzleProgress(PuzzleState.SOLVED, emptyList(), time, best)

/** An In-progress puzzle's saved progress over [placed]. */
internal fun kitInProgress(placed: List<Placement>, time: Int = 0, best: Int? = null): PuzzleProgress = PuzzleProgress(PuzzleState.IN_PROGRESS, placed, time, best)

/**
 * The picture's colours that no `PlayPalette` colour is within [tol] of (S-12: `:play` has no other colour literal; art
 * colours come from `ArtStyle`): what only a drawn picture paints — the REQ-050 rule's "Solved as its picture".
 */
internal fun kitDistinctArtColours(art: ArtLayer, tol: Float = 24f / 255f): List<Color> {
    val palette = listOf(PlayPalette.silhouette, PlayPalette.board, PlayPalette.tray, PlayPalette.paper, PlayPalette.accent, PlayPalette.previewStroke)
    return kitArtColours(art).filter { c -> palette.none { kitNear(it, c, tol) } }.distinctBy { kitHex(it) }
}

// ── Puzzle geometry (the kit's own reading of the solution; px only through BoardMapping) ─────────────

internal data class KitPose(val turn: Turn, val mirrored: Boolean, val centre: Point)

/** The centre of a polygon: the average of its corners (TYPE-003; `Placement.centre`). */
internal fun kitCentroid(poly: List<Point>): Point = Point(poly.sumOf { it.x } / poly.size, poly.sumOf { it.y } / poly.size)

/** The same piece, turn and mirror, and centres within [tol] units (a placement compared as the engine returns it). */
internal fun kitSamePlacement(a: Placement?, b: Placement?, tol: Double = KIT_TOL): Boolean =
    a != null && b != null && a.piece == b.piece && a.turn == b.turn && a.mirrored == b.mirrored &&
        abs(a.centre.x - b.centre.x) <= tol && abs(a.centre.y - b.centre.y) <= tol

internal fun kitSamePointSet(a: List<Point>, b: List<Point>, tol: Double = KIT_TOL): Boolean =
    a.size == b.size &&
        a.all { p -> b.any { q -> abs(p.x - q.x) <= tol && abs(p.y - q.y) <= tol } } &&
        b.all { q -> a.any { p -> abs(p.x - q.x) <= tol && abs(p.y - q.y) <= tol } }

/** The (turn, mirror) at which `ILockEngine.vertices` reproduces a stored solution polygon. */
internal fun kitPoseOf(piece: PieceId, polygon: List<Point>): KitPose {
    val centre = kitCentroid(polygon)
    for (mirrored in listOf(false, true)) {
        for (step in 0..7) {
            val v = kitEngine.vertices(piece, Turn(step), mirrored, centre)
            if (kitSamePointSet(v, polygon)) return KitPose(Turn(step), mirrored, centre)
        }
    }
    throw AssertionError("kit: no turn and mirror of $piece reproduces its solution polygon $polygon")
}

internal fun kitDistToSegment(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
    val dx = bx - ax
    val dy = by - ay
    val len2 = dx * dx + dy * dy
    val t = if (len2 == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
    return hypot(px - (ax + t * dx), py - (ay + t * dy))
}

internal fun kitDistToBoundary(p: Point, poly: List<Point>): Double {
    var best = Double.MAX_VALUE
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        best = min(best, kitDistToSegment(p.x, p.y, a.x, a.y, b.x, b.y))
    }
    return best
}

/** Inside the polygon (even-odd ray test), or within [tol] of its boundary. */
internal fun kitInside(p: Point, poly: List<Point>, tol: Double = 0.0): Boolean {
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[j]
        if ((a.y > p.y) != (b.y > p.y)) {
            val x = a.x + (p.y - a.y) * (b.x - a.x) / (b.y - a.y)
            if (p.x < x) inside = !inside
        }
        j = i
    }
    if (inside) return true
    return tol > 0.0 && kitDistToBoundary(p, poly) <= tol
}

internal fun kitInsideAny(p: Point, polys: List<List<Point>>, tol: Double = 0.0): Boolean = polys.any { kitInside(p, it, tol) }

/** The area-weighted centre of the silhouette (the union of the solution polygons). */
internal fun kitSilhouetteCentre(p: PuzzleDefinition): Point {
    var area = 0.0
    var cx = 0.0
    var cy = 0.0
    for (sp in p.solution) {
        val poly = sp.polygon
        var a = 0.0
        var x = 0.0
        var y = 0.0
        for (i in poly.indices) {
            val p0 = poly[i]
            val p1 = poly[(i + 1) % poly.size]
            val cross = p0.x * p1.y - p1.x * p0.y
            a += cross
            x += (p0.x + p1.x) * cross
            y += (p0.y + p1.y) * cross
        }
        a /= 2.0
        if (abs(a) < 1e-12) continue
        x /= 6.0 * a
        y /= 6.0 * a
        val w = abs(a)
        area += w
        cx += x * w
        cy += y * w
    }
    return Point(cx / area, cy / area)
}

/**
 * A point in the middle of the silhouette where [piece] at [turn]/[mirrored] finds no lock (the engine
 * decides, TYPE-004): the area centre, or the nearest probe on a 0.1-unit spiral around it that is still
 * inside the silhouette. Fails when every probe within one unit locks.
 */
internal fun kitMiddleWithNoLock(p: PuzzleDefinition, piece: PieceId, turn: Turn, mirrored: Boolean, dpPerUnit: Double): Point {
    val centre = kitSilhouetteCentre(p)
    val polys = p.solution.map { it.polygon }
    val probes = ArrayList<Point>()
    probes += centre
    var r = 0.1
    while (r <= 1.0 + 1e-9) {
        var deg = 0.0
        while (deg < 360.0) {
            val rad = Math.toRadians(deg)
            probes += Point(centre.x + r * cos(rad), centre.y + r * sin(rad))
            deg += 30.0
        }
        r += 0.1
    }
    for (q in probes) {
        if (!kitInsideAny(q, polys)) continue
        val lock = kitEngine.findLock(p, emptyList(), piece, turn, mirrored, q, dpPerUnit)
        if (lock is LockResult.None) return q
    }
    throw AssertionError("kit: every probe around the middle of ${p.id} locks $piece; the middle-drop fixture needs another piece")
}

internal data class KitSeg(val a: Point, val b: Point)

/** The edges two different solution pieces share (collinear overlaps): where a piece boundary would show. */
internal fun kitInternalEdges(p: PuzzleDefinition): List<KitSeg> {
    val out = ArrayList<KitSeg>()
    val polys = p.solution.map { it.polygon }
    for (i in polys.indices) for (j in i + 1 until polys.size) {
        val pi = polys[i]
        val pj = polys[j]
        for (ei in pi.indices) {
            val a = pi[ei]
            val b = pi[(ei + 1) % pi.size]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = hypot(dx, dy)
            if (len < KIT_TOL) continue
            for (ej in pj.indices) {
                val c = pj[ej]
                val d = pj[(ej + 1) % pj.size]
                val distC = abs((c.x - a.x) * dy - (c.y - a.y) * dx) / len
                val distD = abs((d.x - a.x) * dy - (d.y - a.y) * dx) / len
                if (distC > KIT_TOL || distD > KIT_TOL) continue
                val tc = ((c.x - a.x) * dx + (c.y - a.y) * dy) / (len * len)
                val td = ((d.x - a.x) * dx + (d.y - a.y) * dy) / (len * len)
                val t0 = max(0.0, min(tc, td))
                val t1 = min(1.0, max(tc, td))
                if (t1 - t0 <= KIT_TOL) continue
                out += KitSeg(Point(a.x + t0 * dx, a.y + t0 * dy), Point(a.x + t1 * dx, a.y + t1 * dy))
            }
        }
    }
    return out
}

/**
 * The silhouette's exact outline in puzzle units (WO-010 F3): every sub-segment of a solution-piece edge, cut at the corners of
 * the pieces that lie on it, with the union of the pieces on one side only. A sub-segment two pieces share has pieces on both
 * sides and is left out ([kitInternalEdges] are those); the outline's corners are piece corners. Worked out by the F3 probe
 * (run log f3-probe-*).
 */
internal fun kitOutline(p: PuzzleDefinition): List<KitSeg> {
    val polys = p.solution.map { it.polygon }
    val corners = polys.flatten()
    val out = ArrayList<KitSeg>()
    for (poly in polys) for (i in poly.indices) {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len2 = dx * dx + dy * dy
        if (len2 < KIT_TOL * KIT_TOL) continue
        val cuts = sortedSetOf(0.0, 1.0)
        for (v in corners) {
            val t = ((v.x - a.x) * dx + (v.y - a.y) * dy) / len2
            if (t > 1e-9 && t < 1 - 1e-9 && hypot(v.x - (a.x + t * dx), v.y - (a.y + t * dy)) < KIT_TOL) cuts += t
        }
        val ts = cuts.toList()
        val len = sqrt(len2)
        val nx = -dy / len * 1e-4
        val ny = dx / len * 1e-4
        for (k in 0 until ts.size - 1) {
            val tm = (ts[k] + ts[k + 1]) / 2
            val mx = a.x + tm * dx
            val my = a.y + tm * dy
            val left = kitInsideAny(Point(mx + nx, my + ny), polys)
            val right = kitInsideAny(Point(mx - nx, my - ny), polys)
            if (left != right) out += KitSeg(Point(a.x + ts[k] * dx, a.y + ts[k] * dy), Point(a.x + ts[k + 1] * dx, a.y + ts[k + 1] * dy))
        }
    }
    return out
}

/** Points along [seg] every [stepUnits], excluding its ends. */
internal fun kitSamplesAlong(seg: KitSeg, stepUnits: Double): List<Point> {
    val len = hypot(seg.b.x - seg.a.x, seg.b.y - seg.a.y)
    val n = max(2, ceil(len / stepUnits).toInt())
    return (1 until n).map { k ->
        val t = k.toDouble() / n
        Point(seg.a.x + (seg.b.x - seg.a.x) * t, seg.a.y + (seg.b.y - seg.a.y) * t)
    }
}

/** The unit normal of [seg] (either side). */
internal fun kitNormal(seg: KitSeg): Point {
    val dx = seg.b.x - seg.a.x
    val dy = seg.b.y - seg.a.y
    val len = hypot(dx, dy)
    return Point(-dy / len, dx / len)
}

// ── The picture's own ink (so an art line on a piece edge is not read as a piece boundary) ────────────

internal data class KitInk(val a: Point, val b: Point, val halfWidth: Double)

private fun kitPolyline(points: List<Point>, closed: Boolean, hw: Double): List<KitInk> {
    val out = ArrayList<KitInk>()
    for (i in 0 until points.size - 1) out += KitInk(points[i], points[i + 1], hw)
    if (closed && points.size > 2) out += KitInk(points.last(), points.first(), hw)
    return out
}

private fun kitEllipsePoints(c: Point, rx: Double, ry: Double): List<Point> =
    (0 until 48).map { k -> val t = 2 * Math.PI * k / 48; Point(c.x + rx * cos(t), c.y + ry * sin(t)) }

private fun kitFlattenPath(d: String): List<List<Point>> {
    val tokens = Regex("[MLQCZmlqcz]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?").findAll(d).map { it.value }.toList()
    val polylines = ArrayList<List<Point>>()
    var current = ArrayList<Point>()
    var i = 0
    fun num(): Double = tokens[i++].toDouble()
    var cmd = 'M'
    while (i < tokens.size) {
        val t = tokens[i]
        if (t.length == 1 && t[0].isLetter()) { cmd = t[0].uppercaseChar(); i++; if (cmd == 'Z') { if (current.size > 1) { current.add(current.first()); polylines += current }; current = ArrayList(); continue } }
        when (cmd) {
            'M' -> { if (current.size > 1) polylines += current; current = ArrayList(); current += Point(num(), num()); cmd = 'L' }
            'L' -> current += Point(num(), num())
            'Q' -> {
                val p0 = current.lastOrNull() ?: Point(0.0, 0.0)
                val c = Point(num(), num()); val p1 = Point(num(), num())
                for (k in 1..16) { val s = k / 16.0; val u = 1 - s; current += Point(u * u * p0.x + 2 * u * s * c.x + s * s * p1.x, u * u * p0.y + 2 * u * s * c.y + s * s * p1.y) }
            }
            'C' -> {
                val p0 = current.lastOrNull() ?: Point(0.0, 0.0)
                val c1 = Point(num(), num()); val c2 = Point(num(), num()); val p1 = Point(num(), num())
                for (k in 1..16) { val s = k / 16.0; val u = 1 - s; current += Point(u * u * u * p0.x + 3 * u * u * s * c1.x + 3 * u * s * s * c2.x + s * s * s * p1.x, u * u * u * p0.y + 3 * u * u * s * c1.y + 3 * u * s * s * c2.y + s * s * s * p1.y) }
            }
            else -> i++
        }
    }
    if (current.size > 1) polylines += current
    return polylines
}

/** Every boundary and stroke of the picture as segments in puzzle units with a half-width. */
internal fun kitArtInk(art: ArtLayer): List<KitInk> {
    val out = ArrayList<KitInk>()
    for (s in art.shapes) {
        val hw = if (s.style.stroke != null) (s.style.width ?: 0.0) / 2 else 0.0
        when (s) {
            is ArtShape.Polygon -> out += kitPolyline(s.points, closed = true, hw = hw)
            is ArtShape.Rect -> {
                val pts = listOf(Point(s.x, s.y), Point(s.x + s.w, s.y), Point(s.x + s.w, s.y + s.h), Point(s.x, s.y + s.h))
                out += kitPolyline(pts, closed = true, hw = hw)
                val rx = s.rx ?: 0.0
                if (rx > 0) { // the rounded corners cut inside the sharp corner by up to rx
                    for (corner in pts) out += kitPolyline(kitEllipsePoints(corner, rx, rx), closed = true, hw = hw)
                }
            }
            is ArtShape.Circle -> out += kitPolyline(kitEllipsePoints(s.c, s.r, s.r), closed = true, hw = hw)
            is ArtShape.Ellipse -> out += kitPolyline(kitEllipsePoints(s.c, s.rx, s.ry), closed = true, hw = hw)
            is ArtShape.Line -> out += KitInk(s.from, s.to, hw)
            is ArtShape.Path -> for (pl in kitFlattenPath(s.d)) out += kitPolyline(pl, closed = false, hw = hw)
        }
    }
    return out
}

/** Distance in units from [p] to the nearest ink edge (negative inside a stroke's width). */
internal fun kitInkDistance(ink: List<KitInk>, p: Point): Double {
    var best = Double.MAX_VALUE
    for (s in ink) best = min(best, kitDistToSegment(p.x, p.y, s.a.x, s.a.y, s.b.x, s.b.y) - s.halfWidth)
    return best
}

// ── Pixel masks and colours ───────────────────────────────────────────────────────────────────────────

internal class KitMask(val w: Int, val h: Int, val bits: BooleanArray = BooleanArray(w * h)) {
    operator fun get(x: Int, y: Int): Boolean = x in 0 until w && y in 0 until h && bits[y * w + x]
    operator fun set(x: Int, y: Int, v: Boolean) { bits[y * w + x] = v }
    fun count(): Int = bits.count { it }
    fun not(): KitMask = KitMask(w, h, BooleanArray(w * h) { !bits[it] })

    /** Keeps a pixel only when every pixel of the (2r+1)² square around it is set (the border counts as unset). */
    fun erode(r: Int): KitMask {
        val tmp = KitMask(w, h)
        for (y in 0 until h) for (x in 0 until w) {
            var all = true
            var i = x - r
            while (all && i <= x + r) { if (!this[i, y]) all = false; i++ }
            tmp[x, y] = all
        }
        val out = KitMask(w, h)
        for (y in 0 until h) for (x in 0 until w) {
            var all = true
            var j = y - r
            while (all && j <= y + r) { if (!tmp[x, j]) all = false; j++ }
            out[x, y] = all
        }
        return out
    }

    fun dilate(r: Int): KitMask = not().erode(r).not()

    fun forEachSet(block: (Int, Int) -> Unit) { for (y in 0 until h) for (x in 0 until w) if (bits[y * w + x]) block(x, y) }
}

/** The pixels whose centres lie inside any polygon or within [expandPx] of its boundary. */
internal fun kitRasterise(polys: List<List<Offset>>, w: Int, h: Int, expandPx: Float = 0.75f): KitMask {
    val m = KitMask(w, h)
    for (poly in polys) {
        val pts = poly.map { Point(it.x.toDouble(), it.y.toDouble()) }
        val x0 = max(0, floor(poly.minOf { it.x } - expandPx - 1).toInt())
        val x1 = min(w - 1, ceil(poly.maxOf { it.x } + expandPx + 1).toInt())
        val y0 = max(0, floor(poly.minOf { it.y } - expandPx - 1).toInt())
        val y1 = min(h - 1, ceil(poly.maxOf { it.y } + expandPx + 1).toInt())
        for (y in y0..y1) for (x in x0..x1) {
            if (!m[x, y] && kitInside(Point(x + 0.5, y + 0.5), pts, expandPx.toDouble())) m[x, y] = true
        }
    }
    return m
}

/** [p]'s solution polygons in the `board` capture's px through [m], the `board` node's top-left at [boardTopLeft]. */
internal fun kitPolygonsPx(p: PuzzleDefinition, m: BoardMapping, boardTopLeft: Offset): List<List<Offset>> =
    p.solution.map { sp -> sp.polygon.map { m.toPx(it) - boardTopLeft } }

/** A segment in a capture's px. */
internal data class KitSegPx(val a: Offset, val b: Offset)

/** A silhouette's inside and outside pixel sets at a margin from its exact outline ([KitScreen.silhouetteBands]). */
internal class KitBands(val inside: KitMask, val outside: KitMask)

/**
 * The pixels whose centres lie at least [marginPx] from [outline]: in [KitBands.inside] when the centre is in the union of
 * [polys], in [KitBands.outside] when it is not (WO-010 F3; the distance is the exact one, to the nearest point of a segment).
 * A centre counts as in the union when it lies in a polygon or within 1 px of one, so a centre on an edge two pieces share is
 * in it; every centre either set keeps lies [marginPx] (more than 1 px) from the outline, so the 1 px decides nothing else.
 */
internal fun kitBands(polys: List<List<Offset>>, outline: List<KitSegPx>, w: Int, h: Int, marginPx: Double = 3.0): KitBands {
    require(marginPx > 1.0) { "kit: a band margin of $marginPx px; more than 1 px is needed" }
    val near = KitMask(w, h)
    for (s in outline) {
        val ax = s.a.x.toDouble()
        val ay = s.a.y.toDouble()
        val bx = s.b.x.toDouble()
        val by = s.b.y.toDouble()
        val x0 = max(0, floor(min(ax, bx) - marginPx - 1).toInt())
        val x1 = min(w - 1, ceil(max(ax, bx) + marginPx + 1).toInt())
        val y0 = max(0, floor(min(ay, by) - marginPx - 1).toInt())
        val y1 = min(h - 1, ceil(max(ay, by) + marginPx + 1).toInt())
        for (y in y0..y1) for (x in x0..x1) {
            if (!near[x, y] && kitDistToSegment(x + 0.5, y + 0.5, ax, ay, bx, by) < marginPx) near[x, y] = true
        }
    }
    val member = kitRasterise(polys, w, h, expandPx = 1f)
    val inside = KitMask(w, h)
    val outside = KitMask(w, h)
    for (i in 0 until w * h) {
        if (near.bits[i]) continue
        if (member.bits[i]) inside.bits[i] = true else outside.bits[i] = true
    }
    return KitBands(inside, outside)
}

internal fun kitNear(a: Color, b: Color, tol: Float = 10f / 255f): Boolean =
    abs(a.red - b.red) <= tol && abs(a.green - b.green) <= tol && abs(a.blue - b.blue) <= tol

internal fun kitHex(c: Color): String = "#%02X%02X%02X".format((c.red * 255).roundToInt(), (c.green * 255).roundToInt(), (c.blue * 255).roundToInt())

internal fun kitColour(hex: String): Color {
    val v = hex.removePrefix("#").toLong(16)
    return Color(((v shr 16) and 0xFF).toInt(), ((v shr 8) and 0xFF).toInt(), (v and 0xFF).toInt())
}

/** Every colour the picture names: its base, fills and strokes. */
internal fun kitArtColours(art: ArtLayer): List<Color> {
    val out = ArrayList<Color>()
    out += kitColour(art.base)
    for (s in art.shapes) {
        s.style.fill?.let { out += kitColour(it) }
        s.style.stroke?.let { out += kitColour(it) }
    }
    return out
}

internal fun kitPieceColours(): Map<PieceId, Color> = PieceId.entries.associateWith { PlayPalette.piece(it) }

/**
 * A pixel inside [poly] (capture px), at least [marginPx] from its boundary, outside [avoid] and — when
 * [within] is given — inside it (the captured area: a sample outside the kit's own bitmap is never read).
 */
internal fun kitInteriorSamples(poly: List<Offset>, marginPx: Float, avoid: List<Rect> = emptyList(), stepPx: Float = 3f, within: Rect? = null): List<Offset> {
    val pts = poly.map { Point(it.x.toDouble(), it.y.toDouble()) }
    val out = ArrayList<Offset>()
    var y = poly.minOf { it.y }
    while (y <= poly.maxOf { it.y }) {
        var x = poly.minOf { it.x }
        while (x <= poly.maxOf { it.x }) {
            val p = Point(x.toDouble(), y.toDouble())
            val o = Offset(x, y)
            val inCapture = within == null || (o.x >= within.left && o.x < within.right - 1f && o.y >= within.top && o.y < within.bottom - 1f)
            if (inCapture && kitInside(p, pts) && kitDistToBoundary(p, pts) >= marginPx && avoid.none { it.contains(o) }) out += o
            x += stepPx
        }
        y += stepPx
    }
    return out
}

/** Pixels inside [inPoly] and at least [marginPx] outside [notPoly] (and inside [within]): where the two poses differ. */
internal fun kitDifferenceSamples(inPoly: List<Offset>, notPoly: List<Offset>, marginPx: Float, avoid: List<Rect> = emptyList(), within: Rect? = null): List<Offset> {
    val notPts = notPoly.map { Point(it.x.toDouble(), it.y.toDouble()) }
    return kitInteriorSamples(inPoly, marginPx, avoid, within = within).filter { o ->
        val p = Point(o.x.toDouble(), o.y.toDouble())
        !kitInside(p, notPts) && kitDistToBoundary(p, notPts) >= marginPx
    }
}

internal fun kitBox(poly: List<Offset>): Rect = Rect(poly.minOf { it.x }, poly.minOf { it.y }, poly.maxOf { it.x }, poly.maxOf { it.y })

internal fun kitAssertBoxNear(what: String, expected: Rect, actual: Rect, tolPx: Float) {
    val ok = abs(expected.left - actual.left) <= tolPx && abs(expected.top - actual.top) <= tolPx &&
        abs(expected.right - actual.right) <= tolPx && abs(expected.bottom - actual.bottom) <= tolPx
    if (!ok) throw AssertionError("$what: expected box $expected, node bounds $actual (tolerance $tolPx px)")
}

internal fun kitAssertNear(what: String, expected: Double, actual: Double, tol: Double) {
    if (abs(expected - actual) > tol) throw AssertionError("$what: expected $expected, was $actual (tolerance $tol)")
}

internal fun kitAngleDeg(from: Offset, to: Offset): Double = Math.toDegrees(atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble()))

internal fun kitOnCircle(centre: Offset, radiusPx: Float, deg: Double): Offset {
    val rad = Math.toRadians(deg)
    return Offset(centre.x + (radiusPx * cos(rad)).toFloat(), centre.y + (radiusPx * sin(rad)).toFloat())
}

internal val KIT_SQRT2: Double = sqrt(2.0)
