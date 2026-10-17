// WO-003 TASK-013 · the touch areas of REQ-015 as a px hit test (tasks.md § WO-003 › touch routing, step 2;
// workorders/WO-003.md § Constraints "Gestures") · decisions.md 2026-10-11 "A px hit test in :play is UI, not
// geometry" (O-01, G-04): which piece a finger touched is decided here, on the px polygon the screen draws from
// `ILockEngine.vertices`; every position — lock, preview, turn in place — still comes from the engine.
// WO-003 TASK-014 · the badge branch (REQ-018 rule: the 60 dp area; decisions.md "badge touch precedence").
// WO-003 code review F1 · decisions.md 2026-10-12 "Where the flip badge's touch area ends" and "Inside the
// parallelogram's tray cell the visible thing wins, as on the board": the tray badge's area lies inside the
// parallelogram's cell; on both surfaces the order is the visible disc → a piece's drawn shape (the PG miniature
// in its cell, a placed piece on the board) → the rest of the badge's 60 dp area → the rest of a tray cell or a
// placed piece's 12 dp margin (capture-outbox/chg-draft-req018-badge-touch-precedence.md, amended twice on
// 2026-10-12; REQ-015, REQ-016 and REQ-018 rules).
// WO-004 TASK-020 · S-22's hit order (designs/WO-004-design.md §2 "Touch geometry"): a top-bar column or a third
// of the solved row comes first (order 2), then the WO-003 order above with the Restart pill between a piece's
// drawn shape and the rest of the badge's area (order 3; decisions.md 2026-10-15 "The top bar, Restart and the
// solved row, as drawn": "a piece's drawn shape beats it and it beats a piece's margin"). Order 1 (a running drag
// owns every touch) is the router's; order 0 (the overview open) is TASK-021's.
package app.tangramplaytime.play

import androidx.compose.ui.geometry.Offset
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PuzzleState
import kotlin.math.hypot
import kotlin.math.min

/** REQ-015 rule: a board piece's touch area is its drawn shape grown by this many dp (DI-2 §4 "its shape grown by 12 dp"). */
internal const val TOUCH_MARGIN_DP = 12f

/** The controls of S-22: the top bar's three columns, the Restart pill and the solved row's thirds. */
internal enum class Control {
    /** ‹: the previous puzzle (REQ-024). */
    PREV,

    /** ›: the next puzzle; held 500 ms, the next unsolved one (REQ-024, REQ-050). */
    NEXT,

    /** The counter zone: the grid overview (REQ-050). */
    COUNTER,

    /** The Restart pill (REQ-025). */
    RESTART,

    /** The solved row's Retry (REQ-026). */
    RETRY,

    /** The solved row's best time: inert (S-22 "best time (inert)"). */
    BEST_TIME,

    /** The solved row's Next: the next puzzle in order (decisions.md 2026-10-15 "Browsing readings no REQ fixes"). */
    SOLVED_NEXT,
}

/**
 * The controls the screen shows in this frame besides the top bar's columns, which are there while a puzzle is
 * current (S-22): the Restart pill's slot (shown only In progress, REQ-025 rule) and whether the solved row is shown
 * (from the frame the picture is fully shown, REQ-026).
 */
internal data class ShownControls(val restart: Slot?, val solvedRow: Boolean) {
    companion object {
        /** Neither the pill nor the solved row. */
        val NONE: ShownControls = ShownControls(restart = null, solvedRow = false)
    }
}

/** What a new touch landed on (touch routing step 2; S-22 hit order). */
internal sealed interface Hit {
    /** A flip badge (REQ-018 rule; TASK-014): the tray badge, or the one next to the placed parallelogram. A touch here never drags. */
    data class Badge(val onBoard: Boolean) : Hit

    /** A piece that is in the tray, hit anywhere in its cell (REQ-015 rule: "its whole tray cell"). */
    data class Tray(val piece: PieceId) : Hit

    /** A placed piece, hit inside its drawn shape or within the margin around it; [placement] is its lock now. */
    data class Board(val placement: Placement) : Hit

    /** A control of S-22: acted on at the lift (› also by a 500 ms hold); a touch here never drags. */
    data class OnControl(val control: Control) : Hit
}

/** The touch routing's hit order, in one function. */
internal object HitTest {
    /**
     * The first hit at [at] (`play-screen` px). In order (S-22 hit order 2 and 3):
     * 1. a top-bar column (‹, the counter, ›; full height, no gaps), or — while [shown] has the solved row — a third
     *    of the solved row (Retry, the inert best time, Next);
     * 2. a badge's visible 38 dp disc ([badgeSlots]: the tray badge, or the one next to the placed parallelogram);
     * 3. a piece's drawn shape: the parallelogram's miniature at rest in its cell, where the tray badge's area lies
     *    over it; a placed piece whose drawn shape contains the point — inside beats another piece's margin —
     *    the last-locked piece on top;
     * 4. the Restart pill, while [shown] has it;
     * 5. the rest of a badge's 60 dp area (the tray one lies wholly inside the parallelogram's cell);
     * 6. a tray cell, the whole cell, for a piece that is in the tray while the puzzle is unsolved (REQ-015; the tag
     *    table: the cells exist only then); a placed piece whose shape grown by [marginPx] contains the point, again
     *    the topmost first (the tray and the board never overlap);
     * 7. nothing — the 4 dp gap between the bar and the board among it.
     */
    fun at(at: Offset, geometry: ScreenGeometry, controller: PlayController, marginPx: Float, shown: ShownControls): Hit? {
        // 1. the top bar's columns, the solved row's thirds
        if (geometry.navPrev.contains(at)) return Hit.OnControl(Control.PREV)
        if (geometry.navNext.contains(at)) return Hit.OnControl(Control.NEXT)
        if (geometry.counter.contains(at)) return Hit.OnControl(Control.COUNTER)
        if (shown.solvedRow) {
            val third = geometry.solvedThirds.indexOfFirst { it.contains(at) }
            if (third >= 0) return Hit.OnControl(SOLVED_ROW[third])
        }
        val badges = badgeSlots(controller, geometry)
        val trayBadge = badges.tray
        val boardBadge = badges.board
        // 2. the visible discs
        if (trayBadge != null && trayBadge.onDisc(at)) return Hit.Badge(onBoard = false)
        if (boardBadge != null && boardBadge.onDisc(at)) return Hit.Badge(onBoard = true)
        // 3. a drawn shape: the parallelogram's miniature (only the tray badge's area lies over a miniature) …
        if (trayBadge != null && trayBadge.area.contains(at)) {
            val pg = PieceId.PG
            val miniature = trayMiniaturePx(controller.engine, pg, controller.trayTurn(pg), controller.trayMirrored(pg), geometry)
            if (miniature != null && insidePolygon(at, miniature)) return Hit.Tray(pg)
        }
        // … or a placed piece
        val mapping = geometry.mapping
        val shapes = controller.placed.asReversed().map { p ->
            p to controller.engine.vertices(p.piece, p.turn, p.mirrored, p.centre).map(mapping::toPx)
        }
        shapes.firstOrNull { (_, polygon) -> insidePolygon(at, polygon) }?.let { return Hit.Board(it.first) }
        // 4. the Restart pill
        if (shown.restart?.contains(at) == true) return Hit.OnControl(Control.RESTART)
        // 5. the rest of a badge's area
        if (trayBadge != null && trayBadge.area.contains(at)) return Hit.Badge(onBoard = false)
        if (boardBadge != null && boardBadge.area.contains(at)) return Hit.Badge(onBoard = true)
        // 6. the rest of a tray cell, a placed piece's margin
        if (controller.state != PuzzleState.SOLVED) {
            for (id in controller.trayPieces) {
                val cell = geometry.cells[id] ?: continue
                if (cell.contains(at)) return Hit.Tray(id)
            }
        }
        shapes.firstOrNull { (_, polygon) -> distanceToEdges(at, polygon) <= marginPx }?.let { return Hit.Board(it.first) }
        return null
    }

    /** The solved row's thirds, left to right (S-22: "Retry, best time (inert), Next"). */
    private val SOLVED_ROW = listOf(Control.RETRY, Control.BEST_TIME, Control.SOLVED_NEXT)
}

/** Whether [p] lies in this slot: left and top edges in, right and bottom edges out (whole px). */
internal fun Slot.contains(p: Offset): Boolean =
    p.x >= left && p.x < left + width && p.y >= top && p.y < top + height

/** Whether [p] lies inside the polygon [polygon] (even-odd ray test, px). A point exactly on an edge may fall either way; the margin test covers it. */
internal fun insidePolygon(p: Offset, polygon: List<Offset>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[j]
        if ((a.y > p.y) != (b.y > p.y)) {
            val x = a.x + (p.y - a.y) * (b.x - a.x) / (b.y - a.y)
            if (p.x < x) inside = !inside
        }
        j = i
    }
    return inside
}

/** The distance in px from [p] to the nearest edge of [polygon]. */
internal fun distanceToEdges(p: Offset, polygon: List<Offset>): Float {
    var best = Float.MAX_VALUE
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]
        best = min(best, distanceToSegment(p, a, b))
    }
    return best
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len2 = dx * dx + dy * dy
    val t = if (len2 == 0f) 0f else (((p.x - a.x) * dx + (p.y - a.y) * dy) / len2).coerceIn(0f, 1f)
    return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
}
