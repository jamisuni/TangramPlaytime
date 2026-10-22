// scaffolding — WO-006 TASK-029's own disposable check of seam note 1 (tasks.md § WO-006): "With `PlayInsets.NONE` and
// no keyboard every slot of `ScreenGeometry` equals WO-005's". The JVM kit's `SafeAreaKitTest` pins S-32's sentences
// within ±1–2 px; this file holds WO-005's `ScreenGeometry.of` frozen (commit a797553, `PlayScreen.kt`) and asserts the
// new one equal to it **exactly**, slot by slot, for every bundled puzzle on a spread of screen sizes and densities, with
// the phone literal and the tablet literal — so the eleven play device classes keep their meaning. No token.
// WO-007 TASK-056 (S-53; review F3, decisions.md 2026-10-21 "The moved-› pins in `play/` land in the same step as S-53 …"):
// the frozen reference gains S-53's one change to the bar — the gear's 52 dp column at the right edge, › ending at its
// left, the counter narrower by the gear — and the gear's slot is compared too. A corrected check: its source was
// WO-005's three-column bar. Every other slot is still WO-005's exactly.
package app.tangramplaytime.play.scaffolding

import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.BoardMapping
import app.tangramplaytime.play.PlayInsets
import app.tangramplaytime.play.PlayLayout
import app.tangramplaytime.play.ScreenGeometry
import app.tangramplaytime.play.Slot
import app.tangramplaytime.play.TrayMetrics
import app.tangramplaytime.play.within
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SafeAreaScaffoldTest {

    /** WO-005's slots, as its `ScreenGeometry.of` placed them (frozen below), with S-53's gear (WO-007). */
    private class Wo005(
        val topBar: Slot, val navPrev: Slot, val navNext: Slot, val counter: Slot, val gear: Slot, val board: Slot, val mapping: BoardMapping,
        val tray: Slot, val trayPxPerUnit: Float, val cells: Map<PieceId, Slot>, val solvedThirds: List<Slot>, val sheetArea: Slot,
    )

    /**
     * WO-005's `ScreenGeometry.of`, verbatim but for the private constants written as their values (4, 8, 0.82, 0.80, 60,
     * and S-53's 52), and for S-53's bar: the gear's column at the right edge, › ending at its left, the counter between.
     */
    private fun wo005(widthPx: Int, heightPx: Int, density: Float, layout: PlayLayout, puzzle: PuzzleDefinition): Wo005 {
        val topBarPx = (layout.topBarDp * density).roundToInt()
        val topBar = Slot(0, 0, widthPx, topBarPx)
        val column = (60f * density).roundToInt().coerceAtMost(widthPx / 2)
        val gearWidth = (52f * density).roundToInt().coerceIn(0, widthPx - 2 * column)
        val gear = Slot(widthPx - gearWidth, 0, gearWidth, topBarPx)
        val navPrev = Slot(0, 0, column, topBarPx)
        val navNext = Slot(widthPx - gearWidth - column, 0, column, topBarPx)
        val counter = Slot(column, 0, (widthPx - 2 * column - gearWidth).coerceAtLeast(0), topBarPx)
        val trayGeometry = TrayMetrics.layout(widthPx / density, heightPx / density, layout, puzzle.pieces)
        val trayTop = (heightPx - trayGeometry.heightDp * density).roundToInt().coerceIn(0, heightPx)
        val tray = Slot(0, trayTop, widthPx, heightPx - trayTop)
        val cells = trayGeometry.cells.mapValues { (_, c) ->
            Slot((c.xDp * density).roundToInt(), trayTop + (c.yDp * density).roundToInt(), (c.widthDp * density).roundToInt(), (c.heightDp * density).roundToInt())
        }
        val side = (layout.sideMarginDp * density).roundToInt()
        val boardTop = topBarPx + (4f * density).roundToInt()
        val boardBottom = trayTop - (8f * density).roundToInt()
        val board = Slot(side, boardTop, (widthPx - 2 * side).coerceAtLeast(0), (boardBottom - boardTop).coerceAtLeast(0))
        val points = puzzle.solution.flatMap { it.polygon }
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val unitWidth = max(points.maxOf { it.x } - minX, 1e-6)
        val unitHeight = max(points.maxOf { it.y } - minY, 1e-6)
        val scale = min(board.width * 0.82f / unitWidth, board.height * 0.80f / unitHeight).toFloat()
        val mapping = BoardMapping(
            originXPx = (board.leftPx + (board.width - unitWidth * scale) / 2 - minX * scale).toFloat(),
            originYPx = (board.topPx + (board.height - unitHeight * scale) / 2 - minY * scale).toFloat(),
            pxPerUnit = scale,
        )
        val cuts = listOf(0, (widthPx / 3f).roundToInt(), (widthPx * 2 / 3f).roundToInt(), widthPx)
        val thirds = (0 until 3).map { i -> Slot(cuts[i], tray.top, cuts[i + 1] - cuts[i], tray.height) }
        // WO-005's PlayScreen: the overview and the sheet over Slot(0, bar bottom, W, H − bar bottom).
        val barBottom = topBar.top + topBar.height
        val sheetArea = Slot(0, barBottom, widthPx, (heightPx - barBottom).coerceAtLeast(0))
        return Wo005(topBar, navPrev, navNext, counter, gear, board, mapping, tray, trayGeometry.scaleDpPerUnit * density, cells, thirds, sheetArea)
    }

    private val phone = PlayLayout(
        trayRows = listOf(listOf(PieceId.LT1, PieceId.LT2, PieceId.MT), listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)),
        floatOffsetDp = 30f, cellGapDp = 8f, sideMarginDp = 8f, topBarDp = 66f,
    )

    private val tablet = PlayLayout(
        trayRows = listOf(listOf(PieceId.LT1, PieceId.LT2, PieceId.MT, PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)),
        floatOffsetDp = 30f, cellGapDp = 14f, sideMarginDp = 16f, topBarDp = 74f,
    )

    /** Screen sizes in px with their density: the dev image and its composable heights, the kit phones, the tablets, and odd sizes for the rounding. */
    private val sizes = listOf(
        Triple(1080, 2400, 2.625f), Triple(1080, 2211, 2.625f), Triple(1080, 2280, 2.625f), Triple(1080, 2337, 2.625f),
        Triple(1170, 2532, 3f), Triple(1080, 2340, 3f), Triple(2560, 1600, 2f), Triple(1600, 2560, 2f),
        Triple(1081, 2399, 2.75f), Triple(721, 1517, 1.5f), Triple(1439, 3119, 3.5f),
    )

    @Test
    fun withNoInsetsAndNoKeyboardEverySlotIsWo005sExactly() {
        val puzzles = PuzzleLibraries.bundled(LockEngine()).puzzles
        assertTrue("fixture: the bundle has puzzles", puzzles.isNotEmpty())
        var compared = 0
        for (layout in listOf(phone, tablet)) {
            for ((w, h, d) in sizes) {
                for (p in puzzles) {
                    val tag = "${w}x$h @$d, top bar ${layout.topBarDp}, ${p.id}"
                    val old = wo005(w, h, d, layout, p)
                    for (g in listOf(ScreenGeometry.of(w, h, d, layout, p), ScreenGeometry.of(w, h, d, layout.copy(insets = PlayInsets.NONE, keyboardDp = 0f), p))) {
                        assertEquals("$tag: topBar", old.topBar, g.topBar)
                        assertEquals("$tag: navPrev", old.navPrev, g.navPrev)
                        assertEquals("$tag: navNext", old.navNext, g.navNext)
                        assertEquals("$tag: counter", old.counter, g.counter)
                        assertEquals("$tag: gear", old.gear, g.gear)
                        assertEquals("$tag: board", old.board, g.board)
                        assertEquals("$tag: mapping", old.mapping, g.mapping)
                        assertEquals("$tag: tray", old.tray, g.tray)
                        assertEquals("$tag: trayPxPerUnit", old.trayPxPerUnit, g.trayPxPerUnit, 0f)
                        assertEquals("$tag: cells", old.cells, g.cells)
                        assertEquals("$tag: solvedThirds", old.solvedThirds, g.solvedThirds)
                        assertEquals("$tag: overlay = WO-005's sheet area", old.sheetArea, g.overlay)
                        assertEquals("$tag: overlayContent = overlay", g.overlay, g.overlayContent)
                        assertEquals("$tag: sheetContent = overlay", g.overlay, g.sheetContent)
                        assertEquals("$tag: safe = the composable", Slot(0, 0, w, h), g.safe)
                        compared++
                    }
                }
            }
        }
        assertEquals("every case compared", 2 * sizes.size * puzzles.size * 2, compared)
    }

    @Test
    fun aContentAreaEqualToItsScrimFillsTheScrimsNode() {
        val area = Slot(0, 173, 1080, 2107)
        assertEquals(Slot(0, 0, 1080, 2107), area.within(area))
        assertEquals(Slot(84, 0, 1000, 1990), Slot(84, 173, 1000, 1990).within(area))
    }
}
