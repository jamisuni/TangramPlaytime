// WO-003 TASK-014 · the celebration's confetti (tasks.md § WO-003 › the tag table's `confetti` row) ·
// REQ-023 rule: confetti lasting 2.4 s plays with the picture; none with reduced motion (the chime is
// `:app`'s, O-07, WO-007) · the confetti is drawn, not a dependency (D5): rectangles in the TYPE-001 piece
// colours thrown up from the board's middle and falling under gravity, each corner computed in px (G-17: no
// canvas scale or rotate) · its clock is the solve timeline of `SolveVisual` (Compose's frame clock).
// WO-004 TASK-020 · the screen side of S-3Δ (designs/WO-004-design.md §2 `Celebration`; §3 "Never show a solved
// puzzle's piece positions"; "What survives") · decisions.md 2026-10-12 "One-shot cues are shown once per
// controller" and "Carried to WO-004 #Browsing" (2): the celebration is the controller's `Celebration` cue, played
// once per serial (`ShownCues.celebrationSerial`); otherwise a Solved puzzle stands at the end of its timeline from
// the first frame it is drawn (REQ-026 A1, REQ-023).
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The confetti's look (DI-1 `confetti`; no REQ names it; decisions row of TASK-014): this many pieces, 7–12 × 10–16 dp, in the piece colours. */
private const val CONFETTI_COUNT = 96
private const val CONFETTI_SEED = 20261012
private const val CONFETTI_MIN_W_DP = 7f
private const val CONFETTI_MAX_W_DP = 12f
private const val CONFETTI_MIN_H_DP = 10f
private const val CONFETTI_MAX_H_DP = 16f

/** DI-1's throw, converted from px per 60 Hz frame to dp per second: sideways ±330, upwards 240–960, gravity 1150 dp/s². */
private const val CONFETTI_SPREAD_DP = 80f
private const val CONFETTI_VX_DP_S = 330f
private const val CONFETTI_VY_MIN_DP_S = 240f
private const val CONFETTI_VY_MAX_DP_S = 960f
private const val CONFETTI_GRAVITY_DP_S2 = 1150f
private const val CONFETTI_SPIN_RAD_S = 9f

/** The last part of the celebration over which the confetti fades out, ms (decisions row of TASK-014). */
private const val CONFETTI_FADE_MS = 400f

/** One piece of confetti, in dp around the board's centre, with its throw. */
private class Flake(
    val x0: Float,
    val y0: Float,
    val vx: Float,
    val vy: Float,
    val spin0: Float,
    val spin: Float,
    val w: Float,
    val h: Float,
    val colour: Color,
)

/** The clock of the celebration playing now (S-3Δ): the serial it plays and how far it has run on the frame clock. */
internal class CelebrationClock {
    /** The serial whose timeline [elapsedMs] measures, or null before the first one played. */
    var serial: Int? by mutableStateOf(null)

    /** Milliseconds since the first frame after that solve. */
    var elapsedMs: Float by mutableFloatStateOf(0f)
}

/**
 * The open puzzle's place on the celebration's timeline in this frame (S-3Δ), worked out in the composition:
 * - unsolved: no timeline;
 * - Solved with a [Celebration] of this puzzle whose serial [cues] does not count as shown: it plays, once, from 0
 *   in the frame after the solve to [CONFETTI_MS] on the frame clock, and then counts as shown;
 * - any other Solved puzzle — opened already solved, its celebration over, or one already set when this screen
 *   first composed (an activity recreation, [ShownCues]) — stands at the end at once, in the first frame: the
 *   picture fully shown, no piece drawn, no confetti, the solved row shown.
 */
@Composable
internal fun solveVisual(controller: PlayController, cues: ShownCues): SolveVisual {
    val clock = remember(controller) { CelebrationClock() }
    if (controller.state != PuzzleState.SOLVED) return SolveVisual.UNSOLVED
    val celebration = controller.celebration
    if (celebration == null || celebration.puzzleId != controller.puzzle.id || celebration.serial == cues.celebrationSerial) {
        return SolveVisual.AT_END
    }
    val serial = celebration.serial
    LaunchedEffect(serial) {
        clock.elapsedMs = 0f
        clock.serial = serial
        onFrames(CONFETTI_MS) { clock.elapsedMs = it * CONFETTI_MS }
        cues.celebrationSerial = serial
    }
    return SolveVisual(if (clock.serial == serial) clock.elapsedMs else 0f)
}

/** The same shower every time: a fixed seed, so a run is reproducible (no wall clock, no system random). */
private fun flakes(): List<Flake> {
    val random = Random(CONFETTI_SEED)
    val colours = PieceId.entries.map(PlayPalette::piece)
    return List(CONFETTI_COUNT) {
        Flake(
            x0 = (random.nextFloat() - 0.5f) * CONFETTI_SPREAD_DP,
            y0 = 0f,
            vx = (random.nextFloat() - 0.5f) * 2f * CONFETTI_VX_DP_S,
            vy = -(CONFETTI_VY_MIN_DP_S + random.nextFloat() * (CONFETTI_VY_MAX_DP_S - CONFETTI_VY_MIN_DP_S)),
            spin0 = random.nextFloat() * 2f * Math.PI.toFloat(),
            spin = (random.nextFloat() - 0.5f) * 2f * CONFETTI_SPIN_RAD_S,
            w = CONFETTI_MIN_W_DP + random.nextFloat() * (CONFETTI_MAX_W_DP - CONFETTI_MIN_W_DP),
            h = CONFETTI_MIN_H_DP + random.nextFloat() * (CONFETTI_MAX_H_DP - CONFETTI_MIN_H_DP),
            colour = colours[random.nextInt(colours.size)],
        )
    }
}

/**
 * The confetti: a `confetti` node over the board (its bounds the board, its drawing clipped to it) while
 * the celebration runs, drawn for the timeline's [elapsedMs]: every flake at its thrown position at that
 * moment, fading out over the last [CONFETTI_FADE_MS].
 */
@Composable
internal fun Confetti(g: ScreenGeometry, elapsedMs: Float) {
    val density = g.density
    val shower = remember { flakes() }
    val board = g.board
    Box(
        Modifier
            .slot(board)
            .testTag(PlayTags.CONFETTI)
            .drawBehind {
                val t = elapsedMs / 1000f
                val alpha = ((CONFETTI_MS - elapsedMs) / CONFETTI_FADE_MS).coerceIn(0f, 1f)
                val origin = Offset(size.width / 2f, size.height / 2f)
                val quad = Path()
                clipRect {
                    for (f in shower) {
                        val x = origin.x + (f.x0 + f.vx * t) * density
                        val y = origin.y + (f.y0 + f.vy * t + 0.5f * CONFETTI_GRAVITY_DP_S2 * t * t) * density
                        if (y - f.h * density > size.height) continue
                        val a = f.spin0 + f.spin * t
                        val c = cos(a)
                        val s = sin(a)
                        val hw = f.w * density / 2f
                        val hh = f.h * density / 2f
                        quad.reset()
                        quad.moveTo(x + (-hw * c - -hh * s), y + (-hw * s + -hh * c))
                        quad.lineTo(x + (hw * c - -hh * s), y + (hw * s + -hh * c))
                        quad.lineTo(x + (hw * c - hh * s), y + (hw * s + hh * c))
                        quad.lineTo(x + (-hw * c - hh * s), y + (-hw * s + hh * c))
                        quad.close()
                        drawPath(quad, f.colour.copy(alpha = alpha))
                    }
                }
            },
    )
}
