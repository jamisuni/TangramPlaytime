// WO-004 TASK-020 · the top bar (designs/WO-004-design.md §2 S-13Δ, S-14Δ, S-22 "Top bar"; seam note 4 of
// tasks.md § WO-004) · REQ-024 (‹ and ›; rule: the bar shows the name, the difficulty dots, the number out of the
// total and the state, TYPE-006), REQ-050 (the counter opens the grid; › held opens the next unsolved), REQ-037 (‹ ›
// drawn 52 dp, touch columns 60 dp), REQ-047 (the title in the game's language, S-11) · decisions.md 2026-10-15
// "The top bar, Restart and the solved row, as drawn" (three full-height columns; the title autosized 22 → 14 sp,
// then an ellipsis, over one line of rating dots, "n / N" and a state word for In progress and Solved, none for
// New) · architecture.md G-17 (every path in px), G-10 / O-06 (every text comes in through PlayTexts).
// The touches are the router's (`PointerRouter`, `HitTest`): these nodes only draw and carry the tags and keys.
// WO-007 TASK-056 · S-53 (designs/WO-007-design.md §2) · REQ-032 statement, REQ-037 · decisions.md 2026-10-21 "The gear
// and the settings screen, as drawn.": the gear's column (`GEAR_COLUMN_DP`, 52 dp at the safe right edge) draws DI-1's
// gear, 24 dp and muted, with no square, in px. S-22Δ3 · REQ-024 rule (consumed), REQ-006.A1's 360 dp phone · decisions.md
// 2026-10-21 "Amends … the top bar's second line shrinks to fit the narrower counter.": the dots, the separators, "n / N"
// and the state word scale by one factor f = min(1, available ÷ natural width at 13 sp), floored at 0.75, measured with
// `rememberTextMeasurer`; every size is computed from f and drawn in px, with no layer scale (G-17). At the floor only the
// state word ellipsizes, never the number. Tags and texts are unchanged.
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tangramplaytime.play.PlaySemantics.rating

/** S-22: ‹ covers x from 0 to 60 dp and › the 60 dp before the gear, the bar's full height (REQ-037: more than the 52 dp drawn). */
internal const val NAV_COLUMN_DP = 60f

/** S-53: the gear's column, 52 dp at the safe right edge, the bar's full height (REQ-037: at least 48 dp; `design-inputs.md` §2 item 6). */
internal const val GEAR_COLUMN_DP = 52f

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

/** S-53 "DI-1's gear, 24 dp, muted": DI-1's icon box of 24 units (its viewBox −12 … 12), drawn 24 dp wide. */
private const val GEAR_ICON_DP = 24f
private const val GEAR_BOX = 24f

/** DI-1's gear: eight spokes stroked 3 units with round ends, around a ring of radius 5 stroked 2.8, not filled. */
private const val GEAR_SPOKE_STROKE = 3f
private const val GEAR_RING_RADIUS = 5f
private const val GEAR_RING_STROKE = 2.8f

/** DI-1's eight spokes, each (x1, y1, x2, y2) in the icon box's units around its centre. */
private val GEAR_SPOKES = floatArrayOf(
    0f, -10f, 0f, -7f,
    0f, 10f, 0f, 7f,
    -10f, 0f, -7f, 0f,
    10f, 0f, 7f, 0f,
    -7f, -7f, -5f, -5f,
    7f, 7f, 5f, 5f,
    -7f, 7f, -5f, 5f,
    7f, -7f, 5f, -5f,
)

/** The title: 22 sp bold, autosized down to 14 sp before it ellipsizes (design §3 "Always show the name and number"). */
private val TITLE_MAX = 22.sp
private val TITLE_MIN = 14.sp

/** The second line ("n / N", the state word): DI-1's 12 px muted line, at 13 sp — its natural size, f = 1 (S-22Δ3). */
private val SUB_LINE = 13.sp

/** S-22Δ3: the second line's factor never goes below this; a line that still does not fit ellipsizes its state word. */
private const val SUB_LINE_MIN_FACTOR = 0.75f

/** S-22Δ3: the halvings that find the largest factor that fits when whole-px rounding leaves the line at f too wide. */
private const val SUB_LINE_FIT_STEPS = 10

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
 * The top bar (S-22, S-53): the `nav-prev` and `nav-next` columns, each drawing its 52 dp square with a chevron, the
 * `gear` column at the safe right edge, and the `counter` zone between ‹ and ›. The counter does not merge its
 * descendants, so `title`, `rating` and `state-word` keep their tags, and "n / N" is its own merged, inert text node
 * (seam note 4). Its second line scales to fit the counter (S-22Δ3, [SecondLine]).
 *
 * @param stateWord the state word, or null for New (no word).
 */
@Composable
internal fun TopBar(g: ScreenGeometry, title: String, stars: Int, number: String, stateWord: String?) {
    NavButton(PlayTags.NAV_PREV, g.navPrev, previous = true)
    NavButton(PlayTags.NAV_NEXT, g.navNext, previous = false)
    GearButton(g.gear)
    // S-22Δ3 "available": the counter's width less the content's padding on each side, as the padding below rounds it.
    val padPx = with(LocalDensity.current) { COUNTER_PAD_DP.dp.roundToPx() }
    val availablePx = (g.counter.width - 2 * padPx).coerceAtLeast(0)
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
            SecondLine(stars, number, stateWord, availablePx)
        }
    }
}

/**
 * The bar's second line (S-22Δ3): the rating dots, a separator, "n / N", and — In progress and Solved — a separator and
 * the state word, all scaled by one factor ([secondLineFactor]) so that the line fits in [availablePx]. The factor is
 * measured with `rememberTextMeasurer` from the line as it is laid out here, and every size is computed from it — the
 * text sizes in sp, the dots and the separators in dp, drawn in px — with no layer scale (G-17). At the factor's floor
 * the state word ellipsizes if the line still does not fit; the number never does.
 */
@Composable
private fun SecondLine(stars: Int, number: String, stateWord: String?, availablePx: Int) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val factor = remember(measurer, density, number, stateWord, availablePx) {
        secondLineFactor(availablePx) { f -> secondLineWidthPx(measurer, density, number, stateWord, f) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RatingDots(stars, factor)
        Separator(factor)
        Box(Modifier.semantics(mergeDescendants = true) {}) { SubLine(number, factor, ellipsize = false) }
        if (stateWord != null) {
            Separator(factor)
            Box(Modifier.testTag(PlayTags.STATE_WORD).semantics(mergeDescendants = true) {}) { SubLine(stateWord, factor, ellipsize = true) }
        }
    }
}

/**
 * S-22Δ3's one factor for the second line: f = min(1, [availablePx] ÷ the natural width at 13 sp), floored at 0.75.
 * [widthAt] gives the line's width in whole px at a factor, laid out as [SecondLine] lays it out (f = 1 is the natural
 * width). Each part of the line rounds to whole px on its own, so the line at f can be a few px wider than
 * [availablePx]; then the factor is the largest one below f at which the line fits, found by halving between the floor
 * and f. When even the floor does not fit, the floor is returned, and the state word ellipsizes.
 */
internal fun secondLineFactor(availablePx: Int, widthAt: (Float) -> Int): Float {
    val natural = widthAt(1f)
    if (natural <= availablePx) return 1f
    val f = (availablePx.toFloat() / natural).coerceAtLeast(SUB_LINE_MIN_FACTOR)
    if (f <= SUB_LINE_MIN_FACTOR || widthAt(f) <= availablePx) return f
    var fits = SUB_LINE_MIN_FACTOR
    var tooWide = f
    repeat(SUB_LINE_FIT_STEPS) {
        val mid = (fits + tooWide) / 2f
        if (widthAt(mid) <= availablePx) fits = mid else tooWide = mid
    }
    return fits
}

/**
 * The second line's width in whole px at factor [f], part by part as the `Row` measures it: the dots' box, each
 * separator's padding and dot, and each text's single-line width (S-22Δ3).
 */
private fun secondLineWidthPx(measurer: TextMeasurer, density: Density, number: String, stateWord: String?, f: Float): Int =
    with(density) {
        val separator = 2 * (SEPARATOR_SPACE_DP * f).dp.roundToPx() + (SEPARATOR_DP * f).dp.roundToPx()
        var width = ratingWidthDp(f).dp.roundToPx() + separator + measurer.lineWidthPx(number, f)
        if (stateWord != null) width += separator + measurer.lineWidthPx(stateWord, f)
        width
    }

/** One text of the second line measured on one line, unwrapped, at factor [f]: its width in whole px. */
private fun TextMeasurer.lineWidthPx(text: String, f: Float): Int =
    measure(text, subLineStyle(f), maxLines = 1, softWrap = false).size.width

/** The second line's text style at factor [f]: 13 sp × f, muted. */
private fun subLineStyle(f: Float): TextStyle = TextStyle(fontSize = SUB_LINE * f, color = PlayPalette.muted)

/** One text of the bar's second line, muted, at factor [f]; only the state word may ellipsize ([ellipsize]). */
@Composable
private fun SubLine(text: String, f: Float, ellipsize: Boolean) {
    BasicText(
        text = text,
        style = subLineStyle(f),
        maxLines = 1,
        softWrap = false,
        overflow = if (ellipsize) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

/** A drawn " · " between two parts of the second line (no text: a 3 dp dot with 5 dp each side, × [f]). */
@Composable
private fun Separator(f: Float) {
    Box(
        Modifier
            .padding(horizontal = (SEPARATOR_SPACE_DP * f).dp)
            .size((SEPARATOR_DP * f).dp)
            .drawBehind { drawCircle(PlayPalette.muted) },
    )
}

/** The rating dots' box width in dp at factor [f]: five dots and four gaps (52 dp at f = 1). */
private fun ratingWidthDp(f: Float): Float = (RATING_DOTS * DOT_DP + (RATING_DOTS - 1) * DOT_GAP_DP) * f

/**
 * The `rating` node (S-14Δ `RatingKey`): [stars] filled dots, then hollow ones up to five, drawn in px at factor [f]
 * (S-22Δ3). Its box holds nothing else, so the dots are its only ink.
 */
@Composable
private fun RatingDots(stars: Int, f: Float) {
    Box(
        Modifier
            .size(ratingWidthDp(f).dp, (RATING_HEIGHT_DP * f).dp)
            .testTag(PlayTags.RATING)
            .semantics { rating = stars }
            .drawBehind {
                val r = DOT_DP * f / 2f * density
                val step = (DOT_DP + DOT_GAP_DP) * f * density
                val ring = DOT_RING_DP * f * density
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
 * from the column's outer edge and the bar's top, with DI-1's chevron, every coordinate in px (G-17).
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

/**
 * The gear's column (S-53): the node is the whole column, its touch area. The router hits it (seam note 2: no
 * `clickable` and no semantics click action). It draws DI-1's gear, 24 dp and muted, with no square, centred across the
 * column and level with the centre of the ‹ › squares (DI-1's `.topbar` centres its items on one line), in px (G-17).
 */
@Composable
private fun GearButton(slot: Slot) {
    Box(
        Modifier
            .slot(slot)
            .testTag(PlayTags.GEAR)
            .drawBehind {
                val centre = Offset(size.width / 2f, (NAV_INSET_DP + NAV_SQUARE_DP / 2f) * density)
                drawGear(centre, GEAR_ICON_DP * density, PlayPalette.muted)
            },
    )
}

/**
 * DI-1's gear centred on [centre] in a [boxPx] square (its 24-unit box): eight spokes stroked 3 units with round ends and
 * a ring of radius 5 stroked 2.8, every coordinate built in px.
 */
private fun DrawScope.drawGear(centre: Offset, boxPx: Float, colour: Color) {
    val u = boxPx / GEAR_BOX
    for (i in GEAR_SPOKES.indices step 4) {
        drawLine(
            colour,
            Offset(centre.x + GEAR_SPOKES[i] * u, centre.y + GEAR_SPOKES[i + 1] * u),
            Offset(centre.x + GEAR_SPOKES[i + 2] * u, centre.y + GEAR_SPOKES[i + 3] * u),
            strokeWidth = GEAR_SPOKE_STROKE * u,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(colour, GEAR_RING_RADIUS * u, centre, style = Stroke(GEAR_RING_STROKE * u))
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
