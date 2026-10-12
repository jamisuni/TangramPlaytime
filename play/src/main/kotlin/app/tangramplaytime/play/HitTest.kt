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
package app.tangramplaytime.play

import androidx.compose.ui.geometry.Offset
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import kotlin.math.hypot
import kotlin.math.min

/** REQ-015 rule: a board piece's touch area is its drawn shape grown by this many dp (DI-2 §4 "its shape grown by 12 dp"). */
internal const val TOUCH_MARGIN_DP = 12f

/** What a new touch landed on (touch routing step 2). */
internal sealed interface Hit {
    /** A flip badge (REQ-018 rule; TASK-014): the tray badge, or the one next to the placed parallelogram. A touch here never drags. */
    data class Badge(val onBoard: Boolean) : Hit

    /** A piece that is in the tray, hit anywhere in its cell (REQ-015 rule: "its whole tray cell"). */
    data class Tray(val piece: PieceId) : Hit

    /** A placed piece, hit inside its drawn shape or within the margin around it; [placement] is its lock now. */
    data class Board(val placement: Placement) : Hit
}

/** The touch routing's hit order, in one function. */
internal object HitTest {
    /**
     * The first hit at [at] (`play-screen` px): the visible thing wins, then the badge's undrawn area, then a
     * piece's undrawn area (decisions.md 2026-10-12, both rows on the badge's touch area). In order:
     * 1. a badge's visible 38 dp disc ([badgeSlots]: the tray badge, or the one next to the placed parallelogram);
     * 2. a piece's drawn shape: the parallelogram's miniature at rest in its cell, where the tray badge's area lies
     *    over it; a placed piece whose drawn shape contains the point — inside beats another piece's margin —
     *    the last-locked piece on top;
     * 3. the rest of a badge's 60 dp area (the tray one lies wholly inside the parallelogram's cell);
     * 4. a tray cell, the whole cell, for a piece that is in the tray (REQ-015); a placed piece whose shape grown
     *    by [marginPx] contains the point, again the topmost first (the tray and the board never overlap);
     * 5. nothing.
     */
    fun at(at: Offset, geometry: ScreenGeometry, controller: PlayController, marginPx: Float): Hit? {
        val badges = badgeSlots(controller, geometry)
        val trayBadge = badges.tray
        val boardBadge = badges.board
        // 1. the visible discs
        if (trayBadge != null && trayBadge.onDisc(at)) return Hit.Badge(onBoard = false)
        if (boardBadge != null && boardBadge.onDisc(at)) return Hit.Badge(onBoard = true)
        // 2. a drawn shape: the parallelogram's miniature (only the tray badge's area lies over a miniature) …
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
        // 3. the rest of a badge's area
        if (trayBadge != null && trayBadge.area.contains(at)) return Hit.Badge(onBoard = false)
        if (boardBadge != null && boardBadge.area.contains(at)) return Hit.Badge(onBoard = true)
        // 4. the rest of a tray cell, a placed piece's margin
        for (id in controller.trayPieces) {
            val cell = geometry.cells[id] ?: continue
            if (cell.contains(at)) return Hit.Tray(id)
        }
        shapes.firstOrNull { (_, polygon) -> distanceToEdges(at, polygon) <= marginPx }?.let { return Hit.Board(it.first) }
        return null
    }
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
