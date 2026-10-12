// scaffolding — TASK-014's own disposable checks of the pure parts of the decorations and the solve: the
// size-mark letters, the badge areas and their precedence in the hit test, the pulse keyframes and the solve
// timeline's arithmetic (no token; the acceptance verdict is the device kit's DecorationKitTest,
// CornerPulseKitTest and SolvedPictureKitTest).
package app.tangramplaytime.play.scaffolding

import androidx.compose.ui.geometry.Offset
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.BADGE_TOUCH_DP
import app.tangramplaytime.play.CONFETTI_MS
import app.tangramplaytime.play.FADE_DELAY_MS
import app.tangramplaytime.play.FADE_MS
import app.tangramplaytime.play.Hit
import app.tangramplaytime.play.HitTest
import app.tangramplaytime.play.POP_MS
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlayLayout
import app.tangramplaytime.play.PlayTexts
import app.tangramplaytime.play.ScreenGeometry
import app.tangramplaytime.play.SolveVisual
import app.tangramplaytime.play.artColour
import app.tangramplaytime.play.badgeSlots
import app.tangramplaytime.play.contains
import app.tangramplaytime.play.insidePolygon
import app.tangramplaytime.play.pulseFrame
import app.tangramplaytime.play.sizeMarkOf
import app.tangramplaytime.play.trayMiniaturePx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecorationScaffoldTest {

    private val engine = LockEngine()
    private val library = PuzzleLibraries.bundled(engine)
    private val layout = PlayLayout(
        trayRows = listOf(listOf(PieceId.LT1, PieceId.LT2, PieceId.MT), listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)),
        floatOffsetDp = 30f,
        cellGapDp = 8f,
        sideMarginDp = 8f,
        topBarDp = 66f,
    )
    private val texts = PlayTexts("S", "M", "L", "en")
    private val density = 2.625f

    @Test
    fun theLettersFollowTheTriangleSizesAndTheOthersHaveNone() {
        assertEquals("L", sizeMarkOf(PieceId.LT1, texts))
        assertEquals("L", sizeMarkOf(PieceId.LT2, texts))
        assertEquals("M", sizeMarkOf(PieceId.MT, texts))
        assertEquals("S", sizeMarkOf(PieceId.ST1, texts))
        assertEquals("S", sizeMarkOf(PieceId.ST2, texts))
        assertNull(sizeMarkOf(PieceId.SQ, texts))
        assertNull(sizeMarkOf(PieceId.PG, texts))
    }

    @Test
    fun theTrayBadgeIsSixtyDpInsideThePgCellsTopRightAndWinsTheHitTestOnlyThere() {
        // Code review F1 (decisions.md 2026-10-12): the disc and the whole 60 dp area lie inside the PG cell; every
        // other cell is wholly its piece's. 1080 px at 2.625 (411 dp) and 945 px (360 dp, the narrowest REQ-013 width).
        for (widthPx in listOf(1080, 945)) {
            val controller = PlayController(library, engine)
            controller.open(library.puzzles.first { it.kind == PuzzleKind.FULL }.id)
            val g = ScreenGeometry.of(widthPx, 2280, density, layout, controller.puzzle)
            controller.boardDpPerUnit = (g.mapping.pxPerUnit / density).toDouble()
            val slots = badgeSlots(controller, g)
            val tray = slots.tray ?: throw AssertionError("a tray badge with the PG in the tray")
            assertNull("no board badge with the PG in the tray", slots.board)
            val sixty = Math.round(BADGE_TOUCH_DP * density)
            assertEquals(sixty, tray.area.width)
            assertEquals(sixty, tray.area.height)
            val cell = g.cells.getValue(PieceId.PG)
            val margin = 12f * density
            assertTrue("$widthPx: the area ${tray.area} lies inside the PG cell $cell", tray.area.left >= cell.left && tray.area.top >= cell.top && tray.area.left + tray.area.width <= cell.left + cell.width && tray.area.top + tray.area.height <= cell.top + cell.height)
            assertEquals("the area ends at the cell's right edge", cell.left + cell.width, tray.area.left + tray.area.width)
            assertEquals("the area starts at the cell's top", cell.top, tray.area.top)
            val r = tray.discRadius
            assertTrue("$widthPx: the whole disc ${tray.disc} r $r lies inside the area ${tray.area}", tray.disc.x - r >= tray.area.left && tray.disc.x + r <= tray.area.left + tray.area.width && tray.disc.y - r >= tray.area.top && tray.disc.y + r <= tray.area.top + tray.area.height)
            assertTrue("disc centre ${tray.disc} in the cell's top-right quarter $cell", tray.disc.x > cell.left + cell.width * 0.5f && tray.disc.y < cell.top + cell.height * 0.5f)
            assertEquals("the disc's centre is the badge", Hit.Badge(onBoard = false), HitTest.at(tray.disc, g, controller, margin))
            assertEquals("the disc's edge is the badge", Hit.Badge(onBoard = false), HitTest.at(Offset(tray.disc.x - r + 1f, tray.disc.y), g, controller, margin))
            // Decisions.md 2026-10-12 (fix batch): the visible thing wins in the tray too — disc → the PG miniature's
            // drawn shape → the rest of the area → the rest of the cell.
            val mini = trayMiniaturePx(engine, PieceId.PG, controller.trayTurn(PieceId.PG), controller.trayMirrored(PieceId.PG), g)!!
            val middle = Offset(mini.sumOf { it.x.toDouble() }.toFloat() / mini.size, mini.sumOf { it.y.toDouble() }.toFloat() / mini.size)
            assertTrue("$widthPx: the miniature's middle $middle lies in the badge area ${tray.area}", tray.area.contains(middle))
            assertEquals("$widthPx: the miniature's middle is the PG", Hit.Tray(PieceId.PG), HitTest.at(middle, g, controller, margin))
            var drawnInArea = 0
            var bareInArea = 0
            for (y in tray.area.top until tray.area.top + tray.area.height step 4) for (x in tray.area.left until tray.area.left + tray.area.width step 4) {
                val q = Offset(x + 0.5f, y + 0.5f)
                val expected = when {
                    (q - tray.disc).getDistance() <= r -> Hit.Badge(onBoard = false)
                    insidePolygon(q, mini) -> Hit.Tray(PieceId.PG).also { drawnInArea++ }
                    else -> Hit.Badge(onBoard = false).also { bareInArea++ }
                }
                assertEquals("$widthPx: $q in the badge area", expected, HitTest.at(q, g, controller, margin))
            }
            assertTrue("$widthPx: the area holds drawn ($drawnInArea) and bare ($bareInArea) samples", drawnInArea > 0 && bareInArea > 0)
            assertEquals("the PG cell's bottom-left corner is the PG", Hit.Tray(PieceId.PG), HitTest.at(Offset(cell.leftPx + 1f, cell.topPx + cell.height - 1f), g, controller, margin))
            for (id in listOf(PieceId.LT2, PieceId.MT, PieceId.ST1, PieceId.SQ)) {
                val c = g.cells.getValue(id)
                for (q in listOf(Offset(c.leftPx + 1f, c.topPx + c.height - 1f), Offset(c.leftPx + 1f, c.topPx + 1f), Offset(c.leftPx + c.width - 1f, c.topPx + c.height - 1f))) {
                    assertEquals("$widthPx: $q in the $id cell is its piece's", Hit.Tray(id), HitTest.at(q, g, controller, margin))
                }
            }
            val gapAbove = Offset(tray.disc.x, cell.topPx - 2f)
            assertFalse("the row gap above the PG cell is not the badge", HitTest.at(gapAbove, g, controller, margin) is Hit.Badge)
        }
    }

    @Test
    fun noBadgeWithoutThePgOrAfterTheSolve() {
        val controller = PlayController(library, engine)
        val mini = library.puzzles.first { it.kind == PuzzleKind.MINI && PieceId.PG !in it.pieces }
        controller.open(mini.id)
        val g = ScreenGeometry.of(1080, 2280, density, layout, controller.puzzle)
        val slots = badgeSlots(controller, g)
        assertNull(slots.tray)
        assertNull(slots.board)
    }

    @Test
    fun thePulseKeyframesPeakAtFortyFivePercentAndSettle() {
        val (s0, a0) = pulseFrame(0f)
        assertEquals(0.3f, s0, 1e-6f)
        assertEquals(0f, a0, 1e-6f)
        val (sPeak, aPeak) = pulseFrame(0.45f)
        assertEquals(1.3f, sPeak, 1e-6f)
        assertEquals(1f, aPeak, 1e-6f)
        val (s1, a1) = pulseFrame(1f)
        assertEquals(1f, s1, 1e-6f)
        assertEquals(0.9f, a1, 1e-6f)
    }

    @Test
    fun theSolveTimelineFollowsTheReqTwentyThreeNumbers() {
        val solve = SolveVisual()
        assertNull(solve.pictureAlpha)
        assertTrue(solve.piecesShown)
        assertFalse(solve.confettiShown(reducedMotion = false))
        solve.elapsedMs = 0f
        assertEquals(0f, solve.popBump(reducedMotion = false), 1e-6f)
        assertEquals(0f, solve.popBump(reducedMotion = true), 1e-6f)
        solve.elapsedMs = POP_MS / 2f
        assertEquals(1f, solve.popBump(reducedMotion = false), 1e-6f)
        assertEquals(0f, solve.popBump(reducedMotion = true), 1e-6f)
        solve.elapsedMs = (FADE_DELAY_MS - 1).toFloat()
        assertNull("no picture before 600 ms", solve.pictureAlpha)
        assertTrue(solve.confettiShown(reducedMotion = false))
        assertFalse(solve.confettiShown(reducedMotion = true))
        solve.elapsedMs = FADE_DELAY_MS.toFloat()
        assertEquals(0f, solve.pictureAlpha!!, 1e-6f)
        assertTrue(solve.piecesShown)
        solve.elapsedMs = (FADE_DELAY_MS + FADE_MS / 2).toFloat()
        assertEquals(0.5f, solve.pictureAlpha!!, 1e-6f)
        solve.elapsedMs = (FADE_DELAY_MS + FADE_MS).toFloat()
        assertEquals(1f, solve.pictureAlpha!!, 1e-6f)
        assertFalse("the pieces go once the picture is fully shown", solve.piecesShown)
        solve.elapsedMs = (CONFETTI_MS - 1).toFloat()
        assertTrue(solve.confettiShown(reducedMotion = false))
        solve.elapsedMs = CONFETTI_MS.toFloat()
        assertFalse(solve.confettiShown(reducedMotion = false))
    }

    @Test
    fun artColoursReadHexAndOpacity() {
        val c = artColour("#3D8BFD", null)
        assertEquals(0x3D / 255f, c.red, 1e-6f)
        assertEquals(0x8B / 255f, c.green, 1e-6f)
        assertEquals(0xFD / 255f, c.blue, 1e-6f)
        assertEquals(1f, c.alpha, 1e-6f)
        assertEquals(0.4f, artColour("#000000", 0.4).alpha, 1e-6f)
    }
}
