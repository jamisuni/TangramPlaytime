// WO-004 TASK-021 · S-22's overview (designs/WO-004-design.md §2 "Overview", hit order 0, "What survives") and S-14Δ's
// cell keys · REQ-050 (rules: the grid lists every puzzle in ‹ › order and shows its state — New as the flat
// silhouette, In progress as the silhouette with a dot, Solved as its picture; an overlay over the current puzzle that
// closing returns to as it was; cells at least 64 dp square; A2, A3), REQ-003 A1 (every puzzle one choice away),
// REQ-024 A2 (the bar stays drawn above the sheet), REQ-026 and the REQ-050 ASSUMPTION (a Solved cell shows the picture,
// never piece positions), REQ-039 (the picture inside the outline: clipped with the silhouette's own path, the
// precedent of decisions.md 2026-10-12 "The solve, as drawn"), REQ-037 (Done at least 48 dp; design-inputs.md §2
// item 6) · decisions.md 2026-10-15 "The grid overview, as drawn" and its amendment (the scrim and the sheet start below
// the top bar; the bar stays drawn and inert; closed by Done, a tap on the scrim or back; nothing under it reacts) ·
// DI-1 `openGrid()` and `thumb()` with the CSS of their markup (`.overlay`, `.sheet`, `.grid`, `.gcell`, `.big.next`) ·
// design §1 "How it works" and §3 library fact 4: the overlay works with foundation `clickable`s while the root router
// stands aside (`PointerRouter`, hit order 0) · G-17: every cell path is built in px for the cell's own size and drawn
// without a canvas scale · G-10 / O-06: the title and Done come in through PlayTexts.
package app.tangramplaytime.play

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.PlaySemantics.puzzleId
import app.tangramplaytime.play.PlaySemantics.puzzleState
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** S-22 "The sheet is inset 16 dp" (DI-1 `.overlay` padding 16 px) from the overview's edges. */
private const val SHEET_INSET_DP = 16f

/** DI-1 `#gridDlg .sheet` `width: min(640px, 100%)`: the sheet's widest. */
private const val SHEET_MAX_WIDTH_DP = 640f

/** DI-1 `.sheet`: a 26 dp corner, padding 18 dp at the top and the sides and 16 dp at the bottom, 14 dp between its title, grid and Done row. */
private const val SHEET_CORNER_DP = 26f
private const val SHEET_PAD_DP = 18f
private const val SHEET_PAD_BOTTOM_DP = 16f
private const val SHEET_GAP_DP = 14f

/** DI-1 `.sheet h2`: the title 26 sp, weight 800 — drawn bold, as TASK-020 draws DI-1's other weight-800 labels. */
private val SHEET_TITLE = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PlayPalette.silhouette)

/** decisions.md 2026-10-15 "The grid overview, as drawn": square cells of at least 84 dp (DI-1 `.grid` `minmax(84px, 1fr)`; REQ-050 asks for 64). */
private const val CELL_MIN_DP = 84f

/** DI-1 `.grid`: 10 dp between cells, across and down. */
private const val GRID_GAP_DP = 10f

/** DI-1 `.gcell`: a 2 dp edge (the accent for the open puzzle, `.gcell.cur`), a 16 dp corner, 8 dp padding inside the edge. */
private const val CELL_EDGE_DP = 2f
private const val CELL_CORNER_DP = 16f
private const val CELL_PAD_DP = 8f

/** DI-1 `thumb()`: the silhouette's box plus 0.35 puzzle units on each side is fitted into the cell's padded area, centred. */
private const val THUMB_PAD_UNITS = 0.35

/** DI-1 `.gcell.prog::after`: the In-progress dot, 10 dp across, 8 dp in from the bottom-right inside the edge. */
private const val DOT_DP = 10f
private const val DOT_INSET_DP = 8f

/** A Solved cell's thin outline, stroked on the clip's edge: the solved picture's 1.2 dp with bevel joins (decisions.md 2026-10-12 "The solve, as drawn"). */
private const val THUMB_OUTLINE_DP = 1.2f

/** DI-1 `.big.next` (Done): a green pill with a white label, 22 dp each side, the label 20 sp bold; 52 dp high, as TASK-020 draws `.big`. */
private const val DONE_HEIGHT_DP = 52f
private const val DONE_PAD_DP = 22f
private val DONE_LABEL = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PlayPalette.control)

/** REQ-037: every player control's touch area is at least 48 dp in both directions. */
private const val MIN_TOUCH_DP = 48f

/**
 * The grid overview (S-22), shown while [PlayController.overviewOpen]: the `overview` node over [area] — the whole
 * Play screen below the top bar — holding the scrim and, inset 16 dp, the sheet with the title, a vertically scrolling
 * grid of every listed puzzle (every cell composed, not lazy) and Done.
 *
 * Touches go to foundation `clickable`s: a cell is [PlayController.choose], Done and the scrim are
 * [PlayController.closeOverview]. The sheet is a hit target of its own, so a touch on it outside a cell and Done reaches
 * neither the scrim nor anything under the overlay. The root router stands aside for every touch that starts while the
 * overview is open (hit order 0), so nothing under it — the top bar included — reacts.
 */
@Composable
internal fun Overview(controller: PlayController, area: Slot, texts: PlayTexts) {
    Box(Modifier.slot(area).testTag(PlayTags.OVERVIEW)) {
        // The scrim: every point of the overview the sheet does not cover; a tap on it closes the grid (S-22).
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(PlayPalette.scrim) }
                .clickable(interactionSource = null, indication = null) { controller.closeOverview() },
        )
        Box(Modifier.fillMaxSize().padding(SHEET_INSET_DP.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = SHEET_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    // A hit target that consumes nothing: a touch on the sheet stops here instead of at the scrim
                    // beneath it, and the cells' and Done's `clickable`s still see every change unconsumed.
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                    .drawBehind { drawRoundRect(PlayPalette.paper, cornerRadius = CornerRadius(SHEET_CORNER_DP * density)) }
                    .padding(start = SHEET_PAD_DP.dp, top = SHEET_PAD_DP.dp, end = SHEET_PAD_DP.dp, bottom = SHEET_PAD_BOTTOM_DP.dp),
                verticalArrangement = Arrangement.spacedBy(SHEET_GAP_DP.dp),
            ) {
                // The title: a merged, inert text node (S-14Δ).
                Box(Modifier.semantics(mergeDescendants = true) {}) {
                    BasicText(texts.allPuzzles, style = SHEET_TITLE, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    OverviewGrid(controller)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    DoneButton(texts.done) { controller.closeOverview() }
                }
            }
        }
    }
}

/**
 * Every listed puzzle in ‹ › order (REQ-050 rule; REQ-040), row by row: as many square columns of at least 84 dp as
 * the width holds, 10 dp apart, each cell the same whole px, the row centred in the width that is left over.
 */
@Composable
private fun OverviewGrid(controller: PlayController) {
    val openId = controller.puzzle.id
    Layout(
        content = {
            for (p in controller.puzzles) {
                key(p.id) {
                    OverviewCell(p, controller.stateOf(p.id), p.id == openId) { controller.choose(p.id) }
                }
            }
        },
    ) { measurables, constraints ->
        val minCell = ceil(CELL_MIN_DP * density).toInt()
        val gap = (GRID_GAP_DP * density).roundToInt()
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else minCell
        val columns = ((width + gap) / (minCell + gap)).coerceAtLeast(1)
        val cell = ((width - (columns - 1) * gap) / columns).coerceAtLeast(1)
        val rows = (measurables.size + columns - 1) / columns
        val height = if (rows == 0) 0 else rows * cell + (rows - 1) * gap
        val left = ((width - (columns * cell + (columns - 1) * gap)) / 2).coerceAtLeast(0)
        val placeables = measurables.map { it.measure(Constraints.fixed(cell, cell)) }
        layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            placeables.forEachIndexed { i, placeable ->
                placeable.place(left + (i % columns) * (cell + gap), (i / columns) * (cell + gap))
            }
        }
    }
}

/**
 * One grid cell (S-22; S-14Δ: `PuzzleIdKey` and `PuzzleStateKey`): a white rounded square with a 2 dp edge — the
 * accent when [p] is the open puzzle — showing [p]'s flat silhouette (New), the silhouette with an accent dot (In
 * progress), or its picture clipped to the silhouette with the thin outline (Solved; never a piece position: REQ-026,
 * the REQ-050 ASSUMPTION). There are no numbers (decisions.md 2026-10-15). The silhouette is the merged path of
 * [SilhouettePath] and every art shape is built in px through a mapping of the cell's own size (G-17).
 */
@Composable
private fun OverviewCell(p: PuzzleDefinition, state: PuzzleState, isOpen: Boolean, onChoose: () -> Unit) {
    Box(
        Modifier
            .testTag(PlayTags.overviewCell(p.id))
            .semantics {
                puzzleId = p.id
                puzzleState = state
            }
            .clickable(interactionSource = null, indication = null, onClick = onChoose)
            .drawWithCache {
                val d = density
                val w = size.width
                val h = size.height
                val edge = CELL_EDGE_DP * d
                val corner = CELL_CORNER_DP * d
                val inner = (CELL_EDGE_DP + CELL_PAD_DP) * d
                val mapping = thumbMapping(p, inner, inner, w - 2 * inner, h - 2 * inner)
                val silhouette = SilhouettePath.build(p.solution, mapping)
                val solved = state == PuzzleState.SOLVED
                val base = if (solved) artColour(p.art.base, null) else null
                val shapes = if (solved) p.art.shapes.map { ArtDrawing.of(it, mapping, d) } else emptyList()
                val outline = Stroke(THUMB_OUTLINE_DP * d, join = StrokeJoin.Bevel)
                val edgeStroke = Stroke(edge)
                val edgeColour = if (isOpen) PlayPalette.accent else PlayPalette.controlEdge
                val dotCentre = Offset(w - (CELL_EDGE_DP + DOT_INSET_DP + DOT_DP / 2f) * d, h - (CELL_EDGE_DP + DOT_INSET_DP + DOT_DP / 2f) * d)
                onDrawBehind {
                    drawRoundRect(PlayPalette.control, cornerRadius = CornerRadius(corner))
                    drawRoundRect(
                        edgeColour,
                        Offset(edge / 2f, edge / 2f),
                        Size(w - edge, h - edge),
                        CornerRadius((corner - edge / 2f).coerceAtLeast(0f)),
                        style = edgeStroke,
                    )
                    if (base == null) {
                        drawPath(silhouette, PlayPalette.silhouette)
                    } else {
                        clipPath(silhouette) {
                            drawRect(base)
                            for (shape in shapes) shape.draw(this)
                            drawPath(silhouette, PlayPalette.silhouette, style = outline)
                        }
                    }
                    if (state == PuzzleState.IN_PROGRESS) drawCircle(PlayPalette.accent, DOT_DP / 2f * d, dotCentre)
                }
            },
    )
}

/**
 * Puzzle units → the cell's own px for [p] (DI-1 `thumb()`): the solution's box, [THUMB_PAD_UNITS] wider on each side,
 * fitted into the area at ([left], [top]) of [width] × [height] px without distortion and centred in it.
 */
private fun thumbMapping(p: PuzzleDefinition, left: Float, top: Float, width: Float, height: Float): BoardMapping {
    val points = p.solution.flatMap { it.polygon }
    val minX = points.minOf { it.x }
    val minY = points.minOf { it.y }
    val unitWidth = max(points.maxOf { it.x } - minX, 1e-6)
    val unitHeight = max(points.maxOf { it.y } - minY, 1e-6)
    val scale = min(width / (unitWidth + 2 * THUMB_PAD_UNITS), height / (unitHeight + 2 * THUMB_PAD_UNITS))
    return BoardMapping(
        originXPx = (left + (width - unitWidth * scale) / 2 - minX * scale).toFloat(),
        originYPx = (top + (height - unitHeight * scale) / 2 - minY * scale).toFloat(),
        pxPerUnit = scale.toFloat(),
    )
}

/** Done (S-22, REQ-050): a green pill 52 dp high with [label], a merged node; a tap closes the grid. */
@Composable
private fun DoneButton(label: String, onDone: () -> Unit) {
    Box(
        Modifier
            .testTag(PlayTags.OVERVIEW_DONE)
            .clickable(interactionSource = null, indication = null, onClick = onDone)
            .defaultMinSize(minWidth = MIN_TOUCH_DP.dp)
            .height(DONE_HEIGHT_DP.dp)
            .drawBehind { drawRoundRect(PlayPalette.go, cornerRadius = CornerRadius(size.height / 2f)) }
            .padding(horizontal = DONE_PAD_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = DONE_LABEL, maxLines = 1, softWrap = false)
    }
}
