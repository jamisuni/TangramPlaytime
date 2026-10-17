// WO-004 TASK-020 · the top bar (designs/WO-004-design.md §2 S-13Δ, S-14Δ, S-22 "Top bar"; seam note 4 of
// tasks.md § WO-004) · REQ-024 (‹ and ›; rule: the bar shows the name, the difficulty dots, the number out of the
// total and the state, TYPE-006), REQ-050 (the counter opens the grid; › held opens the next unsolved), REQ-037 (‹ ›
// drawn 52 dp, touch columns 60 dp), REQ-047 (the title in the game's language, S-11) · decisions.md 2026-10-15
// "The top bar, Restart and the solved row, as drawn" (three full-height columns; the title autosized 22 → 14 sp,
// then an ellipsis, over one line of rating dots, "n / N" and a state word for In progress and Solved, none for
// New) · architecture.md G-17 (every path in px), G-10 / O-06 (every text comes in through PlayTexts).
// The touches are the router's (`PointerRouter`, `HitTest`): these nodes only draw and carry the tags and keys.
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tangramplaytime.play.PlaySemantics.rating

/** S-22: ‹ covers x from 0 to 60 dp and › the last 60 dp, the bar's full height (REQ-037: more than the 52 dp drawn). */
internal const val NAV_COLUMN_DP = 60f

/** S-22 / REQ-037 rule "‹ and › are 52 dp": the square drawn in each column. */
private const val NAV_SQUARE_DP = 52f

/** S-22 "a 52 dp square drawn at 8 dp": from the screen's edge, and from the bar's top (DI-1 `.topbar` padding 8 px). */
private const val NAV_INSET_DP = 8f

/** DI-1 `.nav`: a 16 dp corner radius and a 2 dp edge. */
private const val NAV_CORNER_DP = 16f
private const val CONTROL_EDGE_DP = 2f

/** DI-1's chevron: the path M14 3 L6 11 L14 19 in a 22-unit box drawn 22 dp wide, stroked 3.2 with round ends. */
private const val CHEVRON_BOX = 22f
private const val CHEVRON_STROKE = 3.2f

/** The title: 22 sp bold, autosized down to 14 sp before it ellipsizes (design §3 "Always show the name and number"). */
private val TITLE_MAX = 22.sp
private val TITLE_MIN = 14.sp

/** The second line ("n / N", the state word): DI-1's 12 px muted line, at 13 sp. */
private val SUB_LINE = 13.sp

/** The counter's content keeps this far from the ‹ and › columns. */
private const val COUNTER_PAD_DP = 4f

/** The rating dots (decisions.md 2026-10-15: "●×rating, ○ up to five"): five 8 dp dots 3 dp apart, the hollow ones a 1.5 dp ring, on a 12 dp line. */
private const val RATING_DOTS = 5
private const val DOT_DP = 8f
private const val DOT_GAP_DP = 3f
private const val DOT_RING_DP = 1.5f
private const val RATING_HEIGHT_DP = 12f

/** The drawn separator between the dots, the number and the state word (DI-1's " · "): a 3 dp muted dot with 5 dp each side. */
private const val SEPARATOR_DP = 3f
private const val SEPARATOR_SPACE_DP = 5f

/**
 * The top bar (S-22): the `nav-prev` and `nav-next` columns, each drawing its 52 dp square with a chevron, and the
 * `counter` zone between them. The counter does not merge its descendants, so `title`, `rating` and `state-word`
 * keep their tags, and "n / N" is its own merged, inert text node (seam note 4).
 *
 * @param stateWord the state word, or null for New (no word).
 */
@Composable
internal fun TopBar(g: ScreenGeometry, title: String, stars: Int, number: String, stateWord: String?) {
    NavButton(PlayTags.NAV_PREV, g.navPrev, previous = true)
    NavButton(PlayTags.NAV_NEXT, g.navNext, previous = false)
    Box(Modifier.slot(g.counter).testTag(PlayTags.COUNTER), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = COUNTER_PAD_DP.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.testTag(PlayTags.TITLE).semantics(mergeDescendants = true) {}) {
                BasicText(
                    text = title,
                    style = TextStyle(fontSize = TITLE_MAX, fontWeight = FontWeight.Bold, color = PlayPalette.silhouette),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(minFontSize = TITLE_MIN, maxFontSize = TITLE_MAX),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingDots(stars)
                Separator()
                Box(Modifier.semantics(mergeDescendants = true) {}) { SubLine(number) }
                if (stateWord != null) {
                    Separator()
                    Box(Modifier.testTag(PlayTags.STATE_WORD).semantics(mergeDescendants = true) {}) { SubLine(stateWord) }
                }
            }
        }
    }
}

/** One line of the bar's second line, muted. */
@Composable
private fun SubLine(text: String) {
    BasicText(text = text, style = TextStyle(fontSize = SUB_LINE, color = PlayPalette.muted), maxLines = 1, softWrap = false)
}

/** A drawn " · " between two parts of the second line (no text: a 3 dp dot). */
@Composable
private fun Separator() {
    Box(
        Modifier
            .padding(horizontal = SEPARATOR_SPACE_DP.dp)
            .size(SEPARATOR_DP.dp)
            .drawBehind { drawCircle(PlayPalette.muted) },
    )
}

/**
 * The `rating` node (S-14Δ `RatingKey`): [stars] filled dots, then hollow ones up to five, drawn in px. Its box holds
 * nothing else, so the dots are its only ink.
 */
@Composable
private fun RatingDots(stars: Int) {
    val width = RATING_DOTS * DOT_DP + (RATING_DOTS - 1) * DOT_GAP_DP
    Box(
        Modifier
            .size(width.dp, RATING_HEIGHT_DP.dp)
            .testTag(PlayTags.RATING)
            .semantics { rating = stars }
            .drawBehind {
                val r = DOT_DP / 2f * density
                val step = (DOT_DP + DOT_GAP_DP) * density
                val ring = DOT_RING_DP * density
                val y = size.height / 2f
                for (i in 0 until RATING_DOTS) {
                    val centre = Offset(r + i * step, y)
                    if (i < stars) {
                        drawCircle(PlayPalette.rating, r, centre)
                    } else {
                        drawCircle(PlayPalette.rating, r - ring / 2f, centre, style = Stroke(ring))
                    }
                }
            },
    )
}

/**
 * One arrow column (S-22): the node is the whole column (its touch area, S-13Δ); it draws the 52 dp square 8 dp in
 * from the screen's edge and the bar's top, with DI-1's chevron, every coordinate in px (G-17).
 */
@Composable
private fun NavButton(tag: String, slot: Slot, previous: Boolean) {
    Box(
        Modifier
            .slot(slot)
            .testTag(tag)
            .drawBehind {
                val side = NAV_SQUARE_DP * density
                val inset = NAV_INSET_DP * density
                val left = if (previous) inset else size.width - inset - side
                val top = inset
                drawControlSquare(Offset(left, top), side, NAV_CORNER_DP * density)
                drawChevron(Offset(left + side / 2f, top + side / 2f), CHEVRON_BOX * density, previous, PlayPalette.silhouette)
            },
    )
}

/** A white rounded square or pill with the 2 dp control edge drawn inside it (DI-1 `.nav`, `.btn`). */
internal fun DrawScope.drawControlSquare(topLeft: Offset, side: Float, corner: Float) {
    drawControlShape(topLeft, Size(side, side), corner)
}

/** A white rounded rectangle of [size] at [topLeft] with the 2 dp control edge drawn inside it. */
internal fun DrawScope.drawControlShape(topLeft: Offset, size: Size, corner: Float) {
    val edge = CONTROL_EDGE_DP * density
    drawRoundRect(PlayPalette.control, topLeft, size, CornerRadius(corner))
    drawRoundRect(
        PlayPalette.controlEdge,
        Offset(topLeft.x + edge / 2f, topLeft.y + edge / 2f),
        Size(size.width - edge, size.height - edge),
        CornerRadius((corner - edge / 2f).coerceAtLeast(0f)),
        style = Stroke(edge),
    )
}

/**
 * DI-1's chevron centred on [centre] in a [boxPx] square: ‹ (the path M14 3 L6 11 L14 19 of a 22-unit box) when
 * [pointsLeft], else its mirror ›; stroked 3.2 units with round ends and joins. The path is built in px.
 */
internal fun DrawScope.drawChevron(centre: Offset, boxPx: Float, pointsLeft: Boolean, colour: Color) {
    val u = boxPx / CHEVRON_BOX
    val half = CHEVRON_BOX / 2f
    fun x(v: Float): Float = centre.x + (if (pointsLeft) v - half else half - v) * u
    fun y(v: Float): Float = centre.y + (v - half) * u
    val path = Path().apply {
        moveTo(x(14f), y(3f))
        lineTo(x(6f), y(11f))
        lineTo(x(14f), y(19f))
    }
    drawPath(path, colour, style = Stroke(CHEVRON_STROKE * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
