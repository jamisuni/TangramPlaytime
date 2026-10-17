// WO-005 TASK-026 · S-25's `rememberButtonSlot`, `SolutionLayer` and `ButtonLayer`, drawn by the aid (tasks.md § WO-005
// › Seams S-25, S-10Δ2, S-22Δ2; the aid's tag table; the touch geometry "The DEV button" and "The overlay") · REQ-046
// statement (a DEV button on the board), rule 3 and A2 (each piece's stored solution position in its colour with its
// id), rule 5 (hidden on a solved puzzle), ASSUMPTION (bottom-left, small, dashed); REQ-037 rule (the #DevTools
// controls are exempt from 48 dp) · decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" ·
// architecture.md G-08 / O-08 (debug build type only), G-17 (every path built in px, no canvas scale), G-10 (the
// label comes from the aid's resources; the overlay's labels are `PieceId.name`, data), S-12 (colours only from
// `PlayPalette`) · DI-1 `.devbtn`, `.devbtn.on`, `drawHint` (numbers no row fixes: proposed decisions row of TASK-026).
package app.tangramplaytime.play.devaid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlayPalette
import app.tangramplaytime.play.PlaySemantics.piece
import app.tangramplaytime.play.R
import app.tangramplaytime.play.ScreenGeometry
import app.tangramplaytime.play.Slot
import app.tangramplaytime.play.boxOf
import app.tangramplaytime.play.polygonPath
import app.tangramplaytime.play.slot
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Touch geometry "Place": the pill's left edge 10 dp right of the board's left edge, its bottom 10 dp above the board's bottom (DI-1 `left: b.x + 10`, `top: b.y + b.h − 38`). */
private const val BUTTON_INSET_DP = 10f

/** Touch geometry "Size": 28 dp high. */
private const val BUTTON_HEIGHT_DP = 28f

/** Touch geometry "Size": the label plus 10 dp of padding and the 1.5 dp outline on each side (DI-1 `.devbtn` padding 7 × 10 px, border 1.5 px). */
private const val BUTTON_PAD_DP = 10f
private const val BUTTON_OUTLINE_DP = 1.5f

/** Touch geometry "Size": the label "DEV", 11 sp bold, letter spacing 0.8 sp (DI-1 `.devbtn` `font: 700 11px/1`, `letter-spacing: .8px`). */
private val BUTTON_LABEL = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)

/** Touch geometry "Look, off": the outline and the label at 75 % opacity (DI-1 `.devbtn` `opacity: .75`). */
private const val BUTTON_OFF_ALPHA = 0.75f

/** The off outline's dash: DI-1's `1.5px dashed` leaves it to the browser; 4.5 dp drawn (three outline widths) and 3 dp gap (proposed decisions row of TASK-026). */
private const val BUTTON_DASH_ON_DP = 4.5f
private const val BUTTON_DASH_OFF_DP = 3f

/** Touch geometry "The overlay": the fill is the piece's colour at 55 % alpha (DI-1 `drawHint` `fill-opacity: .55`). */
private const val OVERLAY_ALPHA = 0.55f

/** Touch geometry "The overlay": the edge white, 2 dp, dashed 6 dp on and 4 dp off, with round joins (DI-1 `drawHint`). */
private const val OVERLAY_EDGE_DP = 2f
private const val OVERLAY_DASH_ON_DP = 6f
private const val OVERLAY_DASH_OFF_DP = 4f

/** Touch geometry "The overlay": the label is the piece's id, white, 14 sp bold (DI-1 `drawHint` text). */
private val OVERLAY_LABEL = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PlayPalette.control)

/**
 * The DEV pill's slot in `play-screen` px (touch geometry "The DEV button"): at the board's bottom-left, 10 dp in from
 * its left and bottom edges, 28 dp high, as wide as its measured label plus the padding and the outline. The router's
 * hit test and the node use this one slot (tag table: "the drawn pill, which is its touch area"). Null while the open
 * puzzle is Solved: then it is neither drawn nor hit (REQ-046 rule 5).
 */
@Composable
internal fun rememberDevButtonSlot(controller: PlayController, g: ScreenGeometry): Slot? {
    val label = stringResource(R.string.dev_aid_button)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val slot = remember(g, label, density) {
        val labelPx = measurer.measure(label, BUTTON_LABEL, softWrap = false, maxLines = 1, density = density).size.width
        val d = g.density
        val inset = (BUTTON_INSET_DP * d).roundToInt()
        val height = (BUTTON_HEIGHT_DP * d).roundToInt()
        val width = ceil(2 * (BUTTON_PAD_DP + BUTTON_OUTLINE_DP) * d).toInt() + labelPx
        Slot(g.board.left + inset, g.board.top + g.board.height - inset - height, width, height)
    }
    return if (controller.state == PuzzleState.SOLVED) null else slot
}

/**
 * The DEV pill at [slot] (the tag table's `dev-button`: a merged, inert display container whose text is "DEV"; the
 * router acts on it). Off: a dashed 1.5 dp `controlEdge` outline, no fill, the label in `muted`, all at 75 %. While the
 * solution is shown ([on]): a solid `accent` outline and label, opaque. The outline is built in px, inside the pill.
 */
@Composable
internal fun DevButton(slot: Slot, on: Boolean) {
    val label = stringResource(R.string.dev_aid_button)
    val edge = if (on) PlayPalette.accent else PlayPalette.controlEdge.copy(alpha = BUTTON_OFF_ALPHA)
    val ink = if (on) PlayPalette.accent else PlayPalette.muted.copy(alpha = BUTTON_OFF_ALPHA)
    Box(
        Modifier
            .slot(slot)
            .testTag(DevAidTags.DEV_BUTTON)
            .semantics(mergeDescendants = true) {}
            .drawWithCache {
                val w = BUTTON_OUTLINE_DP * density
                val outline = Path().apply {
                    addRoundRect(RoundRect(w / 2f, w / 2f, size.width - w / 2f, size.height - w / 2f, CornerRadius((size.height - w) / 2f)))
                }
                val stroke = if (on) {
                    Stroke(w)
                } else {
                    Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(BUTTON_DASH_ON_DP * density, BUTTON_DASH_OFF_DP * density)))
                }
                onDrawBehind { drawPath(outline, edge, style = stroke) }
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = BUTTON_LABEL.copy(color = ink), maxLines = 1, softWrap = false)
    }
}

/**
 * The solution overlay of the open puzzle (REQ-046 A2, rule 3): for each `SolvedPiece`, its stored polygon in px
 * through the board's mapping (G-17), filled with the piece's colour at 55 % and edged white, dashed; then each piece's
 * id, centred on the polygon's average. The shapes come first so that no shape covers another piece's label. Nothing
 * here takes a touch.
 */
@Composable
internal fun DevSolutionOverlay(controller: PlayController, g: ScreenGeometry) {
    val puzzle = controller.puzzle
    val mapping = g.mapping
    val polygons = remember(puzzle, mapping) { puzzle.solution.map { sp -> sp.piece to sp.polygon.map(mapping::toPx) } }
    for ((id, points) in polygons) {
        key(id) { OverlayShape(id, points) }
    }
    val measurer = rememberTextMeasurer()
    for ((id, points) in polygons) {
        key(id) { OverlayLabel(id, points, measurer) }
    }
}

/** One overlay shape (the tag table's `dev-solution-<ID>`): its bounds the px box of the polygon, its key `PieceKey`. */
@Composable
private fun OverlayShape(id: PieceId, points: List<Offset>) {
    val slot = remember(points) { Slot.around(boxOf(points)) }
    val path = remember(points) { polygonPath(points, -slot.leftPx, -slot.topPx) }
    Box(
        Modifier
            .slot(slot)
            .testTag(DevAidTags.solution(id))
            .semantics { piece = id }
            .drawWithCache {
                val fill = PlayPalette.piece(id).copy(alpha = OVERLAY_ALPHA)
                val edge = Stroke(
                    width = OVERLAY_EDGE_DP * density,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(OVERLAY_DASH_ON_DP * density, OVERLAY_DASH_OFF_DP * density)),
                )
                onDrawBehind {
                    drawPath(path, fill)
                    drawPath(path, PlayPalette.previewStroke, style = edge)
                }
            },
    )
}

/** One overlay label (the tag table's `dev-solution-label-<ID>`): the piece's id, a merged, inert text centred on the polygon's average. */
@Composable
private fun OverlayLabel(id: PieceId, points: List<Offset>, measurer: TextMeasurer) {
    val density = LocalDensity.current
    val slot = remember(points, density) {
        val size = measurer.measure(id.name, OVERLAY_LABEL, softWrap = false, maxLines = 1, density = density).size
        val cx = points.sumOf { it.x.toDouble() } / points.size
        val cy = points.sumOf { it.y.toDouble() } / points.size
        Slot((cx - size.width / 2.0).roundToInt(), (cy - size.height / 2.0).roundToInt(), size.width, size.height)
    }
    Box(
        Modifier.slot(slot).testTag(DevAidTags.solutionLabel(id)).semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        BasicText(id.name, style = OVERLAY_LABEL, maxLines = 1, softWrap = false)
    }
}
