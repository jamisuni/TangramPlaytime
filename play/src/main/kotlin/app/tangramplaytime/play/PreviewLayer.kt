// WO-003 TASK-013 · the landing preview (tasks.md § WO-003 › Seams S-2 `updatePreview`; the tag table's
// `landing-preview` row) · REQ-021: a dashed outline where the piece would lock if released now, nothing when
// no position is valid, updated at most once per displayed frame, the same search as the drop (the controller's
// `findLock`; nothing else calls it) · REQ-021 ASSUMPTION: white, dashed, 2.5 dp (`PlayPalette.previewStroke`).
// WO-003 code review N3 · the per-frame search moved to `PointerRouter`, which gives the controller the frame's
// hand and searches it in one frame callback, so a release drops what the drawn preview showed; this layer
// draws `preview`.
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import app.tangramplaytime.play.PlaySemantics.centre
import app.tangramplaytime.play.PlaySemantics.mirrored
import app.tangramplaytime.play.PlaySemantics.piece
import app.tangramplaytime.play.PlaySemantics.turn

/** REQ-021 ASSUMPTION: the outline's stroke width in dp. */
private const val PREVIEW_STROKE_DP = 2.5f

/** DI-1's dash, 7 dp drawn and 5 dp gap; no REQ names it (decisions row of TASK-013). */
private const val PREVIEW_DASH_ON_DP = 7f
private const val PREVIEW_DASH_OFF_DP = 5f

/**
 * The landing preview: the `landing-preview` node exists exactly while the controller's `preview` is set, its
 * bounds the outline's box, its keys the preview placement. While a drag runs, the pointer router's frame loop
 * calls `updatePreview()` once per displayed frame, inside the frame callback, so the same frame draws the answer.
 */
@Composable
internal fun PreviewLayer(controller: PlayController, mapping: BoardMapping) {
    val p = controller.preview ?: return
    val density = LocalDensity.current.density
    val points = remember(p, mapping) {
        controller.engine.vertices(p.piece, p.turn, p.mirrored, p.centre).map(mapping::toPx)
    }
    val slot = remember(points) { Slot.around(boxOf(points)) }
    val path = remember(points) { polygonPath(points, -slot.leftPx, -slot.topPx) }
    val stroke = remember(density) {
        Stroke(
            width = PREVIEW_STROKE_DP * density,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(PREVIEW_DASH_ON_DP * density, PREVIEW_DASH_OFF_DP * density)),
        )
    }
    Box(
        Modifier
            .slot(slot)
            .testTag(PlayTags.LANDING_PREVIEW)
            .semantics {
                piece = p.piece
                turn = p.turn
                mirrored = p.mirrored
                centre = p.centre
            }
            .drawBehind { drawPath(path, PlayPalette.previewStroke, style = stroke) },
    )
}
