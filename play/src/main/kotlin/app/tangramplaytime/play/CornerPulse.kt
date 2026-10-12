// WO-003 TASK-014 · the corner pulse of a mini puzzle (tasks.md § WO-003 › Seams S-3 `Pulse`; the tag
// table's `corner-pulse` and `corner-marker` rows) · REQ-051: when a dropped piece goes home in a mini
// puzzle the silhouette's outline corners pulse once, about 600 ms, only the corners (never a piece
// position); with reduced motion the corners are shown for 600 ms without animation · REQ-051 ASSUMPTION
// and decisions.md checkpoint item 1: a small circle in the accent colour · O-01: the corners are the
// controller's `Pulse.corners` = `ILockEngine.outlineCorners` · G-17: drawn in px · the timing runs on
// Compose's frame clock (`onFrames`), never a wall clock (platform-notes).
// WO-003 code review F2 · "once" across an activity recreation: a pulse that `ShownCues` counts as shown is
// never played again.
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.play.PlaySemantics.centre
import kotlin.math.ceil

/** REQ-051 rule: the pulse lasts about 600 ms. */
internal const val PULSE_MS = 600

/** The marker: a disc of this radius in the accent with a 2 dp ring in `PlayPalette.board`, dp (DI-1 `pulseCorners`; decisions row of TASK-014). */
private const val MARKER_RADIUS_DP = 8f
private const val MARKER_RING_DP = 2f

/** DI-1's pulse keyframes: the disc grows from 0.3 to 1.3 of its size at 45 % and settles to 1; its alpha rises from 0 to 1 and ends at 0.9. */
private const val PULSE_PEAK_AT = 0.45f
private const val PULSE_SCALE_START = 0.3f
private const val PULSE_SCALE_PEAK = 1.3f
private const val PULSE_ALPHA_END = 0.9f

/**
 * The pulse layer: while the controller's [Pulse] is being shown — [PULSE_MS] on the frame clock from the
 * frame after it appeared — a `corner-pulse` node over the board holding one `corner-marker` per outline
 * corner, each centred on its corner and carrying it as `CentreKey`. A new serial restarts it. Once it has
 * run, or when it was set before this screen composed, [cues] counts it as shown and nothing is drawn (REQ-051
 * "once", code review F2). With [reducedMotion] the markers are drawn at rest for the whole time (REQ-051 rule).
 */
@Composable
internal fun CornerPulse(controller: PlayController, g: ScreenGeometry, reducedMotion: Boolean, cues: ShownCues) {
    val pulse = controller.pulse?.takeIf { it.serial != cues.pulseSerial } ?: return
    var progress by remember(pulse.serial) { mutableFloatStateOf(0f) }
    LaunchedEffect(pulse.serial) {
        onFrames(PULSE_MS) { progress = it }
        cues.pulseSerial = pulse.serial
    }
    val density = g.density
    val board = g.board
    val radius = MARKER_RADIUS_DP * density
    val ring = MARKER_RING_DP * density
    val (scale, alpha) = if (reducedMotion) 1f to 1f else pulseFrame(progress)
    Box(Modifier.slot(board).testTag(PlayTags.CORNER_PULSE)) {
        SlotLayout {
            for (corner in pulse.corners) {
                CornerMarker(corner, g, board, radius, ring, scale, alpha)
            }
        }
    }
}

/** The disc's scale and alpha at [t] of the pulse (0..1), piecewise linear through DI-1's keyframes. */
internal fun pulseFrame(t: Float): Pair<Float, Float> {
    val k = t.coerceIn(0f, 1f)
    return if (k < PULSE_PEAK_AT) {
        val f = k / PULSE_PEAK_AT
        lerpF(PULSE_SCALE_START, PULSE_SCALE_PEAK, f) to f
    } else {
        val f = (k - PULSE_PEAK_AT) / (1f - PULSE_PEAK_AT)
        lerpF(PULSE_SCALE_PEAK, 1f, f) to lerpF(1f, PULSE_ALPHA_END, f)
    }
}

/** One marker: its node's slot is the square around the largest disc, centred on the corner (board-local px inside the pulse layer). */
@Composable
private fun CornerMarker(corner: Point, g: ScreenGeometry, board: Slot, radius: Float, ring: Float, scale: Float, alpha: Float) {
    val px = g.mapping.toPx(corner)
    val reach = ceil(radius * PULSE_SCALE_PEAK + ring).toInt() + 1
    val slot = remember(px, reach, board) {
        Slot((px.x - board.leftPx).toInt() - reach, (px.y - board.topPx).toInt() - reach, 2 * reach, 2 * reach)
    }
    // The disc is drawn at the corner's exact px inside the whole-px slot.
    val local = Offset(px.x - board.leftPx - slot.leftPx, px.y - board.topPx - slot.topPx)
    Box(
        Modifier
            .slot(slot)
            .testTag(PlayTags.CORNER_MARKER)
            .semantics { centre = corner }
            .drawBehind {
                val r = radius * scale
                drawCircle(PlayPalette.accent.copy(alpha = alpha), r, local)
                drawCircle(PlayPalette.board.copy(alpha = alpha), r - ring / 2f, local, style = Stroke(ring))
            },
    )
}
