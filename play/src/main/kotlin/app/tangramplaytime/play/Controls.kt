// WO-004 TASK-020 · the Restart pill and the solved row (designs/WO-004-design.md §2 S-13Δ, S-22 "Restart" and
// "Solved row") · REQ-025 (Restart, shown only In progress), REQ-026 (the tray replaced by Retry, the best time and
// Next), REQ-037 via design-inputs.md §2 item 6 (every player control ≥ 48 dp) · decisions.md 2026-10-15 "The top
// bar, Restart and the solved row, as drawn" (the pill at the board's top-left + 10 dp, above the silhouette and
// below the pieces; the row from the frame the picture is fully shown, in thirds; the best time "–" while none is
// stored, else m:ss) · seam note 4: each control is a tagged display container that merges its label, inert (the
// router acts on it; platform-notes § Compose test kit) · G-17: every icon path in px · G-10 / O-06: every label
// comes in through PlayTexts.
// WO-008 TASK-047 · S-48 (designs/WO-008-design.md §2): the best time is written in the one time format (`timeOf`;
// REQ-029 rule 2; REQ-030 rule 3; decisions.md 2026-10-20 "One format for every time the game shows …").
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.roundToInt

/** S-22 "Restart": a pill 48 dp high (design-inputs.md §2 item 6), at the board's top-left + 10 dp. */
private const val RESTART_HEIGHT_DP = 48f
private const val RESTART_OFFSET_DP = 10f

/** DI-1 `.btn`: 14 dp each side, an 18 dp ↻ icon, 6 dp before the label, the label 16 sp bold. */
private const val PILL_PAD_DP = 14f
private const val PILL_ICON_DP = 18f
private const val PILL_GAP_DP = 6f
private val RESTART_LABEL = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlayPalette.silhouette)

/** DI-1 `.big` (Retry, Next): 52 dp high, 8 dp clear of the third's sides, 12 dp padding, a 20 dp icon 8 dp from the label, the label 20 sp bold autosized down to 12 sp. */
private const val ROW_PILL_HEIGHT_DP = 52f
private const val ROW_PILL_MARGIN_DP = 8f
private const val ROW_PILL_PAD_DP = 12f
private const val ROW_ICON_DP = 20f
private const val ROW_GAP_DP = 8f
private val ROW_LABEL_MAX = 20.sp
private val ROW_LABEL_MIN = 12.sp

/** DI-1 `.best`: the label 14 sp muted over the time 20 sp bold. */
private val BEST_LABEL = 14.sp
private val BEST_VALUE = 20.sp

/** DI-1's ↻ (a 20-unit box): an arc of radius 7 from (7, 2) clockwise round to (6, −3), stroked 2.6 with round ends, and its arrowhead. */
private const val ICON_BOX = 20f
private const val ICON_ARC_RADIUS = 7f
private const val ICON_ARC_STROKE = 2.6f
private val ICON_ARC_START_DEG: Float = Math.toDegrees(atan2(2.0, 7.0)).toFloat()
private val ICON_ARC_SWEEP_DEG: Float = 360f - (ICON_ARC_START_DEG - Math.toDegrees(atan2(-3.0, 6.0)).toFloat())

/**
 * The Restart pill's slot (S-22): at the board's top-left + 10 dp, 48 dp high, as wide as its measured label plus
 * the icon and the padding. The router's hit test and the node use this one slot (S-13Δ: its bounds are its touch area).
 */
@Composable
internal fun rememberRestartSlot(g: ScreenGeometry, label: String): Slot {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(g, label, density) {
        val labelPx = measurer.measure(label, RESTART_LABEL, softWrap = false, maxLines = 1, density = density).size.width
        val d = g.density
        val offset = (RESTART_OFFSET_DP * d).roundToInt()
        val width = ceil((2 * PILL_PAD_DP + PILL_ICON_DP + PILL_GAP_DP) * d).toInt() + labelPx
        Slot(g.board.left + offset, g.board.top + offset, width, (RESTART_HEIGHT_DP * d).roundToInt())
    }
}

/** The Restart pill (REQ-025) at [slot]: white with the control edge, the ↻ icon and [label]; a merged, inert display container. */
@Composable
internal fun RestartPill(slot: Slot, label: String) {
    Box(
        Modifier
            .slot(slot)
            .testTag(PlayTags.RESTART)
            .semantics(mergeDescendants = true) {}
            .drawBehind {
                drawControlShape(Offset.Zero, size, size.height / 2f)
                drawRestartIcon(Offset((PILL_PAD_DP + PILL_ICON_DP / 2f) * density, size.height / 2f), PILL_ICON_DP * density, PlayPalette.silhouette)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = label,
            modifier = Modifier.padding(start = (PILL_PAD_DP + PILL_ICON_DP + PILL_GAP_DP).dp),
            style = RESTART_LABEL,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * The solved row (REQ-026): a `solved-row` node over the tray strip holding its thirds — Retry, the best time
 * (inert) and Next — each third a control's whole touch area (S-22 hit order 2; S-13Δ). Shown from the frame the
 * picture is fully shown, or at once for a puzzle opened already solved (the caller decides).
 */
@Composable
internal fun SolvedRow(g: ScreenGeometry, texts: PlayTexts, bestSeconds: Int?) {
    val strip = g.tray
    val thirds = g.solvedThirds.map { Slot(it.left - strip.left, it.top - strip.top, it.width, it.height) }
    Box(Modifier.slot(strip).testTag(PlayTags.SOLVED_ROW)) {
        SlotLayout(Modifier.fillMaxSize()) {
            RowButton(PlayTags.RETRY, thirds[0], texts.retry, primary = false)
            BestTime(thirds[1], texts.bestTime, bestTimeText(bestSeconds, texts))
            RowButton(PlayTags.SOLVED_NEXT, thirds[2], texts.next, primary = true)
        }
    }
}

/**
 * One button of the solved row over its whole third: a 52 dp pill centred in it — white with the control edge and
 * the ↻ icon before the label (Retry), or filled green with the label before a chevron (Next, the primary action:
 * DI-2 §4 "Next is filled green").
 */
@Composable
private fun RowButton(tag: String, slot: Slot, label: String, primary: Boolean) {
    val ink = if (primary) PlayPalette.control else PlayPalette.silhouette
    Box(
        Modifier.slot(slot).testTag(tag).semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ROW_PILL_MARGIN_DP.dp)
                .height(ROW_PILL_HEIGHT_DP.dp)
                .drawBehind {
                    val corner = size.height / 2f
                    if (primary) drawRoundRect(PlayPalette.go, cornerRadius = CornerRadius(corner)) else drawControlShape(Offset.Zero, size, corner)
                }
                .padding(horizontal = ROW_PILL_PAD_DP.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!primary) {
                Box(Modifier.size(ROW_ICON_DP.dp).drawBehind { drawRestartIcon(center, size.minDimension, ink) })
                Spacer(Modifier.width(ROW_GAP_DP.dp))
            }
            BasicText(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                style = TextStyle(fontSize = ROW_LABEL_MAX, fontWeight = FontWeight.Bold, color = ink),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = ROW_LABEL_MIN, maxFontSize = ROW_LABEL_MAX),
            )
            if (primary) {
                Spacer(Modifier.width(ROW_GAP_DP.dp))
                Box(Modifier.size(ROW_ICON_DP.dp).drawBehind { drawChevron(center, size.minDimension, pointsLeft = false, colour = ink) })
            }
        }
    }
}

/** The best time over its whole third (inert, S-22): its label over the time, a merged display container. */
@Composable
private fun BestTime(slot: Slot, label: String, value: String) {
    Box(
        Modifier.slot(slot).testTag(PlayTags.BEST_TIME).semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = label,
                style = TextStyle(fontSize = BEST_LABEL, color = PlayPalette.muted),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = value,
                style = TextStyle(fontSize = BEST_VALUE, fontWeight = FontWeight.Bold, color = PlayPalette.silhouette),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/**
 * The solved row's best time (S-48; REQ-026, REQ-030 rule 3): [PlayTexts.noTime] while none is stored (decisions.md
 * 2026-10-15 "The top bar, Restart and the solved row, as drawn"; WO-004 checkpoint item 3), else the one time format,
 * [timeOf] — m:ss below one hour, [PlayTexts.hoursMinutes] from one hour (REQ-029 rule 2).
 */
internal fun bestTimeText(seconds: Int?, texts: PlayTexts): String =
    if (seconds == null) texts.noTime else texts.timeOf(seconds)

/** DI-1's ↻ centred on [centre] in a [boxPx] square, in px (G-17): the open arc and its arrowhead. */
internal fun DrawScope.drawRestartIcon(centre: Offset, boxPx: Float, colour: Color) {
    val u = boxPx / ICON_BOX
    val r = ICON_ARC_RADIUS * u
    drawArc(
        colour,
        startAngle = ICON_ARC_START_DEG,
        sweepAngle = ICON_ARC_SWEEP_DEG,
        useCenter = false,
        topLeft = Offset(centre.x - r, centre.y - r),
        size = Size(2 * r, 2 * r),
        style = Stroke(ICON_ARC_STROKE * u, cap = StrokeCap.Round),
    )
    val head = Path().apply {
        moveTo(centre.x + 3f * u, centre.y - 8f * u)
        lineTo(centre.x + 8f * u, centre.y - 3f * u)
        lineTo(centre.x + 2f * u, centre.y - 1f * u)
        close()
    }
    drawPath(head, colour)
}
