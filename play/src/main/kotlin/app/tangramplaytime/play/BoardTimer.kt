// WO-008 TASK-047 · seam S-47 (designs/WO-008-design.md §2 "S-13Δ3, S-47 timer") · REQ-031 statement ("WHERE the
// timer setting is on, the game SHALL show the running puzzle time in the top-right corner of the board"), rule 2 (the
// active seconds of the current puzzle only), A1, A2; REQ-026 statement (the solved view shows only its picture with
// Retry, the best time and Next, so no timer there) · decisions.md 2026-10-20 "The timer on the board, as drawn" and "One
// format for every time the game shows …" · design §3 library facts 5 (a snapshot read invalidates only its own scope:
// the time is read here, never by `PlayScreen`) and 6 (`tnum` gives tabular digits) · G-17: the stopwatch is drawn in px
// · G-10 / O-06: the time's words come in through PlayTexts.
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** S-47 / DI-1 `.timer`: the slot's right edge 14 dp in from the board's right, its top 14 dp below the board's top. */
private const val TIMER_INSET_DP = 14f

/** S-47: the stopwatch outline before the time, a 12 dp square. */
private const val TIMER_ICON_DP = 12f

/** Between the stopwatch and the time (DI-1's "⏱ " — the space after the glyph at 14 px; no REQ or seam fixes it). */
private const val TIMER_GAP_DP = 4f

/** S-47 / DI-1 `.timer`: the time 14 sp, muted, with tabular digits (`tnum`), so its width holds while it counts. */
private val TIMER_TEXT = TextStyle(fontSize = 14.sp, color = PlayPalette.muted, fontFeatureSettings = "tnum")

/** The stopwatch's drawing box, in units: a 12-unit square, as the icon's dp. */
private const val WATCH_BOX = 12f
private const val WATCH_STROKE = 1.2f

/** The body: a circle of radius 4.4 whose centre sits 1 unit below the box's centre. */
private const val WATCH_BODY_DROP = 1f
private const val WATCH_BODY_RADIUS = 4.4f

/** The crown: a stem from the body's top up to y = −4.9, and a bar 3.2 wide at y = −5.3 (from the box's centre). */
private const val WATCH_STEM_TOP = -4.9f
private const val WATCH_CROWN_Y = -5.3f
private const val WATCH_CROWN_HALF = 1.6f

/** The hand: from the body's centre up and to the right. */
private const val WATCH_HAND_DX = 1.8f
private const val WATCH_HAND_DY = -1.8f

/** The side button: a short stroke outside the body at 45°, from 0.4 to 1.6 units past its edge. */
private const val WATCH_BUTTON_FROM = 0.4f
private const val WATCH_BUTTON_TO = 1.6f

/**
 * The timer's slot (S-47): its right edge at the board's right − 14 dp, its top at the board's top + 14 dp, as wide as
 * the stopwatch, the gap and the measured [time] (as `rememberRestartSlot` measures its label), as high as the taller
 * of the measured line and the stopwatch. Whole px of the `play-screen` node (G-17).
 */
@Composable
internal fun rememberTimerSlot(g: ScreenGeometry, time: String): Slot {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(g, time, density) {
        val line = measurer.measure(time, TIMER_TEXT, softWrap = false, maxLines = 1, density = density).size
        val d = g.density
        val inset = (TIMER_INSET_DP * d).roundToInt()
        val width = ((TIMER_ICON_DP + TIMER_GAP_DP) * d).roundToInt() + line.width
        val height = max(line.height, (TIMER_ICON_DP * d).roundToInt())
        Slot(g.board.left + g.board.width - inset - width, g.board.top + inset, width, height)
    }
}

/**
 * The running puzzle time on the board (REQ-031; S-47): a stopwatch outline and the controller's `timeSeconds` in the
 * one time format ([timeOf]). The caller composes it only while the timer setting is on and the puzzle is not Solved
 * (decisions.md 2026-10-20 "The timer on the board, as drawn"), after the Restart pill and before the pieces.
 *
 * It reads `timeSeconds` in its own scope, so a counted second recomposes this node alone (library fact 5). It is an
 * inert merged node whose text is the time: it is never in `ShownControls`, so the router's hits pass under it.
 */
@Composable
internal fun BoardTimer(controller: PlayController, g: ScreenGeometry, texts: PlayTexts) {
    val time = texts.timeOf(controller.timeSeconds)
    val slot = rememberTimerSlot(g, time)
    Box(
        Modifier
            .slot(slot)
            .testTag(PlayTags.TIMER)
            .semantics(mergeDescendants = true) {}
            .drawBehind {
                drawStopwatch(Offset(TIMER_ICON_DP / 2f * density, size.height / 2f), TIMER_ICON_DP * density, PlayPalette.muted)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = time,
            modifier = Modifier.padding(start = (TIMER_ICON_DP + TIMER_GAP_DP).dp),
            style = TIMER_TEXT,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * A stopwatch outline centred on [centre] in a [boxPx] square, in px (G-17): the round body, the crown's stem and bar,
 * the side button and one hand, each a stroke with round ends.
 */
private fun DrawScope.drawStopwatch(centre: Offset, boxPx: Float, colour: Color) {
    val u = boxPx / WATCH_BOX
    val stroke = WATCH_STROKE * u
    val body = Offset(centre.x, centre.y + WATCH_BODY_DROP * u)
    val r = WATCH_BODY_RADIUS * u
    drawCircle(colour, radius = r, center = body, style = Stroke(stroke))
    drawLine(colour, Offset(centre.x, body.y - r), Offset(centre.x, centre.y + WATCH_STEM_TOP * u), stroke, StrokeCap.Round)
    drawLine(
        colour,
        Offset(centre.x - WATCH_CROWN_HALF * u, centre.y + WATCH_CROWN_Y * u),
        Offset(centre.x + WATCH_CROWN_HALF * u, centre.y + WATCH_CROWN_Y * u),
        stroke,
        StrokeCap.Round,
    )
    val diagonal = sqrt(0.5f)
    drawLine(
        colour,
        Offset(body.x + (WATCH_BODY_RADIUS + WATCH_BUTTON_FROM) * diagonal * u, body.y - (WATCH_BODY_RADIUS + WATCH_BUTTON_FROM) * diagonal * u),
        Offset(body.x + (WATCH_BODY_RADIUS + WATCH_BUTTON_TO) * diagonal * u, body.y - (WATCH_BODY_RADIUS + WATCH_BUTTON_TO) * diagonal * u),
        stroke,
        StrokeCap.Round,
    )
    drawLine(colour, body, Offset(body.x + WATCH_HAND_DX * u, body.y + WATCH_HAND_DY * u), stroke, StrokeCap.Round)
}
