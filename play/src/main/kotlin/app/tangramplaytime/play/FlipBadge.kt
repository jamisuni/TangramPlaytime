// WO-003 TASK-014 · the flip badge (tasks.md § WO-003 › the tag table's `flip-badge-tray` / `flip-badge-board`
// rows; the touch routing, step 2 "a badge's 60 dp area" first) · REQ-018: a flip badge (⇋) next to the
// parallelogram while it is in the tray or on the board; its touch area is at least 60 dp across (REQ-037) ·
// decisions.md 2026-10-11 "Where the flip badge is" (the tray cell's top-right and next to the placed piece;
// none while dragged, none after the solve) and "badge touch precedence" (the area wins inside the cell) ·
// DI-2 §4: 38 dp visible, 60 dp touch area · G-17: the disc and the glyph are drawn in px, no canvas scale ·
// G-10: the glyph is a path, not a text.
// WO-003 code review F1 · decisions.md 2026-10-12 "Where the flip badge's touch area ends": the tray badge's
// disc and its whole 60 dp area lie inside the parallelogram's own cell (REQ-015 gives every other cell wholly
// to its piece); on both surfaces the visible disc, a piece's drawn shape (the PG miniature; a placed piece), the
// rest of the area and the rest of the cell or a piece's 12 dp margin are hit in that order (`HitTest`;
// decisions.md 2026-10-12 "Inside the parallelogram's tray cell the visible thing wins";
// capture-outbox/chg-draft-req018-badge-touch-precedence.md, amended twice on 2026-10-12).
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleState
import kotlin.math.roundToInt

/** REQ-018 rule (REQ-037): the badge's touch area, dp across. */
internal const val BADGE_TOUCH_DP = 60f

/** DI-2 §4: the visible disc, dp across (no REQ names it; decisions row of TASK-014). */
internal const val BADGE_DISC_DP = 38f

/** The disc's ring, dp (DI-1's 2.5; decisions row of TASK-014). */
private const val BADGE_RING_DP = 2.5f

/** The ⇋ glyph's stroke, dp, and its half-width and the arrows' spacing from the centre, dp (decisions row of TASK-014). */
private const val GLYPH_STROKE_DP = 2.4f
private const val GLYPH_HALF_WIDTH_DP = 9f
private const val GLYPH_ROW_DP = 4.5f
private const val GLYPH_HEAD_DP = 5f

/**
 * The tray badge (DI-2 §3 4b "at its top-right corner"; decisions row of TASK-014, corrected by decisions.md
 * 2026-10-12 "Where the flip badge's touch area ends"): the disc's centre sits 21 dp inside the cell's right
 * edge and 21 dp below its top, so the whole disc is inside the cell with a 2 dp margin, as near its corner
 * as it fits. Its 60 dp area is the cell's top-right 60 × 60 dp: it ends at the cell's right edge and starts
 * at its top, so it never reaches the row gap or a neighbouring cell (REQ-015: a tray piece's touch area is
 * its whole cell). With the phone layout (S-5) the parallelogram's cell (3.16 × the tray scale + 12 dp,
 * REQ-013) is at least 60 dp on every screen at least 258 dp wide, so the area fits it.
 */
private const val TRAY_BADGE_DISC_INSET_DP = 21f

/** The board badge: the disc's centre is this far right of the placed PG's box and this far above its top, dp (DI-1 `updateFlip`; decisions row of TASK-014); the 60 dp area is centred on it. */
private const val BOARD_BADGE_DX_DP = 22f
private const val BOARD_BADGE_DY_DP = 4f

/**
 * One badge's place in this frame: its 60 dp touch [area] (whole px of `play-screen`, the node's bounds),
 * the centre of its visible [disc] inside that area and the disc's radius [discRadius] (px).
 */
internal class BadgePlace(val area: Slot, val disc: Offset, val discRadius: Float) {
    /** Whether [p] lies on the visible 38 dp disc (the board's first hit, decisions.md 2026-10-12). */
    fun onDisc(p: Offset): Boolean = (p - disc).getDistance() <= discRadius
}

/**
 * Where the two badges are in this frame, or null where a badge is not shown: [tray] while the
 * parallelogram is in the tray, [board] while it is placed and the puzzle is unsolved — none while it is
 * dragged (it is in neither place) and none after the solve (decisions.md "Where the flip badge is"). The
 * drawing and the hit test (`HitTest`, which orders the badge against the pieces) read the same answer.
 */
internal class BadgeSlots(val tray: BadgePlace?, val board: BadgePlace?)

/** The badge places for the controller's state and this layout (see [BadgeSlots]). */
internal fun badgeSlots(controller: PlayController, g: ScreenGeometry): BadgeSlots {
    val pg = PieceId.PG
    if (pg !in controller.puzzle.pieces || controller.state == PuzzleState.SOLVED) return BadgeSlots(null, null)
    val density = g.density
    val size = (BADGE_TOUCH_DP * density).roundToInt()
    val radius = BADGE_DISC_DP * density / 2f
    val tray = if (pg in controller.trayPieces) {
        g.cells[pg]?.let { cell ->
            val inset = TRAY_BADGE_DISC_INSET_DP * density
            val disc = Offset(cell.leftPx + cell.width - inset, cell.topPx + inset)
            // The cell's top-right 60 × 60 dp: inside the parallelogram's own cell (decisions.md 2026-10-12).
            val area = Slot(cell.left + cell.width - size, cell.top, size, size)
            BadgePlace(area, disc, radius)
        }
    } else {
        null
    }
    val placement = controller.placed.firstOrNull { it.piece == pg }
    val board = if (placement != null) {
        val box = boxOf(controller.engine.vertices(pg, placement.turn, placement.mirrored, placement.centre).map(g.mapping::toPx))
        val b = g.board
        // Next to the piece's top-right corner, kept on the board so that the disc never leaves it.
        val cx = (box.right + BOARD_BADGE_DX_DP * density).coerceIn(b.leftPx + radius, b.leftPx + b.width - radius)
        val cy = (box.top - BOARD_BADGE_DY_DP * density).coerceIn(b.topPx + radius, b.topPx + b.height - radius)
        val disc = Offset(cx, cy)
        BadgePlace(Slot((cx - size / 2f).roundToInt(), (cy - size / 2f).roundToInt(), size, size), disc, radius)
    } else {
        null
    }
    return BadgeSlots(tray, board)
}

/**
 * One flip badge: a node at its 60 dp touch area carrying [tag] (bounds = the touch area, as the tag table
 * says), drawing the 38 dp disc — `PlayPalette.board` with an accent ring — and the ⇋ glyph as two opposed
 * arrows in `PlayPalette.accent`, all in px, at the place's disc centre. Pressing it is the root pointer
 * handler's job (the badge branch of the touch routing), so the node itself is inert.
 */
@Composable
internal fun FlipBadge(tag: String, place: BadgePlace, density: Float) {
    val slot = place.area
    val local = Offset(place.disc.x - slot.leftPx, place.disc.y - slot.topPx)
    Box(
        Modifier
            .slot(slot)
            .testTag(tag)
            .drawBehind { drawBadge(local, density) },
    )
}

/** The disc and the glyph around [c] (local px). */
private fun DrawScope.drawBadge(c: Offset, density: Float) {
    val radius = BADGE_DISC_DP * density / 2f
    val ring = BADGE_RING_DP * density
    drawCircle(PlayPalette.board, radius, c)
    drawCircle(PlayPalette.accent, radius - ring / 2f, c, style = Stroke(ring))
    val half = GLYPH_HALF_WIDTH_DP * density
    val row = GLYPH_ROW_DP * density
    val head = GLYPH_HEAD_DP * density
    val stroke = Stroke(GLYPH_STROKE_DP * density, cap = StrokeCap.Round)
    // The upper arrow points left, the lower one right: ⇋.
    arrow(Offset(c.x + half, c.y - row), Offset(c.x - half, c.y - row), head, stroke)
    arrow(Offset(c.x - half, c.y + row), Offset(c.x + half, c.y + row), head, stroke)
}

/** A straight arrow from [from] to [to] with a filled head of size [head] at [to]. */
private fun DrawScope.arrow(from: Offset, to: Offset, head: Float, stroke: Stroke) {
    val dir = if (to.x >= from.x) 1f else -1f
    drawLine(PlayPalette.accent, from, Offset(to.x - dir * head * 0.6f, to.y), stroke.width, cap = StrokeCap.Round)
    val tip = Path().apply {
        moveTo(to.x, to.y)
        lineTo(to.x - dir * head, to.y - head * 0.75f)
        lineTo(to.x - dir * head, to.y + head * 0.75f)
        close()
    }
    drawPath(tip, PlayPalette.accent)
}
