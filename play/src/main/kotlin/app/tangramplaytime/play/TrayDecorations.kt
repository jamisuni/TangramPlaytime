// WO-003 TASK-014 · the tray decorations (tasks.md § WO-003 › the tag table's `tray-ghost-*` and
// `size-mark-*` rows; seam S-11 for the letters) · REQ-012 rule: "A piece on the board leaves a dashed
// outline in its tray cell" · REQ-043: S / M / L on the triangles in the tray, top-left of the cell, tinted
// with the piece's colour, hidden while the piece is on the board, never changing a cell's size or the tray
// scale · REQ-047 rule: the same letters in both languages (they come in through `PlayTexts`, O-06, G-10) ·
// G-17: the ghost's path is the miniature's px corners, dashed in px.
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.play.PlaySemantics.piece
import kotlin.math.ceil
import kotlin.math.roundToInt

/** The ghost's stroke, dp, and its dash — DI-1's 2 dp, 5 dp drawn / 4 dp gap (no REQ names them; decisions row of TASK-014). */
private const val GHOST_STROKE_DP = 2f
private const val GHOST_DASH_ON_DP = 5f
private const val GHOST_DASH_OFF_DP = 4f

/** The ghost's ink: the silhouette colour at this alpha over the tray (decisions row of TASK-014). */
private const val GHOST_ALPHA = 0.35f

/** The size mark: a 22 × 20 dp pill 4 dp inside the cell's top-left corner, the letter 13 sp bold (DI-1 `buildTrayUi`; decisions row of TASK-014). */
private const val MARK_INSET_DP = 4f
private const val MARK_WIDTH_DP = 22f
private const val MARK_HEIGHT_DP = 20f
private const val MARK_CORNER_DP = 6f
private const val MARK_PILL_ALPHA = 0.18f
private val MARK_TEXT_SIZE = 13.sp

/** REQ-043: the letter for [id], or null for the square and the parallelogram (one size each). */
internal fun sizeMarkOf(id: PieceId, texts: PlayTexts): String? = when (id.type) {
    PieceType.LT -> texts.sizeLarge
    PieceType.MT -> texts.sizeMedium
    PieceType.ST -> texts.sizeSmall
    PieceType.SQ, PieceType.PG -> null
}

/**
 * The dashed ghost of [id]'s miniature in its cell while the piece is placed or dragged (REQ-012 rule): the
 * outline of the shape at the turn and mirror the piece has in the tray (`trayTurn`, `trayMirrored` — where
 * its miniature was drawn when it left), at the tray scale. Its node's bounds are the outline's box.
 */
@Composable
internal fun TrayGhost(controller: PlayController, id: PieceId, g: ScreenGeometry) {
    val turnNow = controller.trayTurn(id)
    val mirroredNow = controller.trayMirrored(id)
    val density = g.density
    val points = remember(id, turnNow, mirroredNow, g) {
        trayMiniaturePx(controller.engine, id, turnNow, mirroredNow, g) ?: emptyList()
    }
    if (points.isEmpty()) return
    val pad = ceil(GHOST_STROKE_DP * density / 2f).toInt() + 1
    val slot = remember(points) {
        val box = boxOf(points)
        Slot.around(box).let { Slot(it.left - pad, it.top - pad, it.width + 2 * pad, it.height + 2 * pad) }
    }
    val path = remember(points, slot) { polygonPath(points, -slot.leftPx, -slot.topPx) }
    val stroke = remember(density) {
        Stroke(
            width = GHOST_STROKE_DP * density,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(GHOST_DASH_ON_DP * density, GHOST_DASH_OFF_DP * density)),
        )
    }
    Box(
        Modifier
            .slot(slot)
            .testTag(PlayTags.trayGhost(id))
            .semantics { piece = id }
            .drawBehind { drawPath(path, PlayPalette.silhouette.copy(alpha = GHOST_ALPHA), style = stroke) },
    )
}

/**
 * The size mark of a triangle in the tray (REQ-043): [letter] in the piece's colour on a faint pill of the
 * same colour, in the top-left corner of [cell]. A merged, inert text node (platform-notes § Compose test
 * kit); it touches neither the cell nor the miniature.
 */
@Composable
internal fun SizeMark(id: PieceId, letter: String, cell: Slot, density: Float) {
    val slot = remember(cell, density) {
        val inset = (MARK_INSET_DP * density).roundToInt()
        Slot(cell.left + inset, cell.top + inset, (MARK_WIDTH_DP * density).roundToInt(), (MARK_HEIGHT_DP * density).roundToInt())
    }
    val colour = PlayPalette.piece(id)
    Box(
        Modifier
            .slot(slot)
            .testTag(PlayTags.sizeMark(id))
            .semantics(mergeDescendants = true) {}
            .drawBehind {
                drawRoundRect(colour.copy(alpha = MARK_PILL_ALPHA), cornerRadius = CornerRadius(MARK_CORNER_DP * density))
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = letter,
            style = TextStyle(fontSize = MARK_TEXT_SIZE, fontWeight = FontWeight.Bold, color = colour),
            maxLines = 1,
            softWrap = false,
        )
    }
}
