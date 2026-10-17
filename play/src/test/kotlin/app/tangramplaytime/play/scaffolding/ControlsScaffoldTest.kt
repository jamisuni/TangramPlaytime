// scaffolding — TASK-020's own disposable checks of the pure parts of S-22 (no token; the acceptance verdict is the
// device kit's TopBarKitTest, ResumeScreenKitTest and SolvedViewKitTest): the top bar's three full-height columns and
// the gap below them, the hit order around the Restart pill (a piece's drawn shape → the pill → a piece's margin),
// the solved row's thirds, and the best time's m:ss.
package app.tangramplaytime.play.scaffolding

import androidx.compose.ui.geometry.Offset
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.Control
import app.tangramplaytime.play.Hit
import app.tangramplaytime.play.HitTest
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlayLayout
import app.tangramplaytime.play.ScreenGeometry
import app.tangramplaytime.play.ShownControls
import app.tangramplaytime.play.Slot
import app.tangramplaytime.play.bestTimeText
import app.tangramplaytime.play.distanceToEdges
import app.tangramplaytime.play.insidePolygon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.roundToInt

class ControlsScaffoldTest {

    private val engine = LockEngine()
    private val library = PuzzleLibraries.bundled(engine)
    private val layout = PlayLayout(
        trayRows = listOf(listOf(PieceId.LT1, PieceId.LT2, PieceId.MT), listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)),
        floatOffsetDp = 30f,
        cellGapDp = 8f,
        sideMarginDp = 8f,
        topBarDp = 66f,
    )
    private val density = 2.625f

    @Test
    fun theTopBarIsThreeFullHeightColumnsAndTheGapBelowItIsNothing() {
        for (widthPx in listOf(1080, 945)) {
            val controller = PlayController(library, engine)
            val g = ScreenGeometry.of(widthPx, 2280, density, layout, controller.puzzle)
            controller.boardDpPerUnit = (g.mapping.pxPerUnit / density).toDouble()
            val column = (60f * density).roundToInt()
            val bar = (66f * density).roundToInt()
            assertEquals(Slot(0, 0, column, bar), g.navPrev)
            assertEquals(Slot(widthPx - column, 0, column, bar), g.navNext)
            assertEquals(Slot(column, 0, widthPx - 2 * column, bar), g.counter)
            fun at(x: Float, y: Float) = HitTest.at(Offset(x, y), g, controller, 12f * density, ShownControls.NONE)
            assertEquals(Hit.OnControl(Control.PREV), at(0f, 0f))
            assertEquals(Hit.OnControl(Control.PREV), at(column - 1f, bar - 1f))
            assertEquals(Hit.OnControl(Control.COUNTER), at(column.toFloat(), 0f))
            assertEquals(Hit.OnControl(Control.COUNTER), at(widthPx - column - 1f, bar - 1f))
            assertEquals(Hit.OnControl(Control.NEXT), at(widthPx - column.toFloat(), 0f))
            assertEquals(Hit.OnControl(Control.NEXT), at(widthPx - 1f, bar - 1f))
            assertNull("the 4 dp gap between the bar and the board", at(widthPx / 2f, bar + 2f))
        }
    }

    @Test
    fun aDrawnShapeBeatsTheRestartPillAndThePillBeatsAMargin() {
        val (controller, g, placement) = placedAlone()
        val margin = 12f * density
        val poly = engine.vertices(placement.piece, placement.turn, placement.mirrored, placement.centre).map(g.mapping::toPx)
        // A pill laid over the piece's box, grown by the margin on every side.
        val left = poly.minOf { it.x } - margin
        val top = poly.minOf { it.y } - margin
        val pill = Slot(left.toInt(), top.toInt(), (poly.maxOf { it.x } + margin - left).toInt() + 1, (poly.maxOf { it.y } + margin - top).toInt() + 1)
        val shown = ShownControls(pill, solvedRow = false)
        fun at(o: Offset) = HitTest.at(o, g, controller, margin, shown)
        val centre = g.mapping.toPx(placement.centre)
        assertEquals("inside the drawn shape: the piece", Hit.Board(placement), at(centre))
        val marginPoints = ArrayList<Offset>()
        for (y in pill.top until pill.top + pill.height step 3) for (x in pill.left until pill.left + pill.width step 3) {
            val q = Offset(x + 0.5f, y + 0.5f)
            if (insidePolygon(q, poly)) continue
            if (distanceToEdges(q, poly) <= margin) marginPoints += q
            assertEquals("$q in the pill, outside the shape: the pill", Hit.OnControl(Control.RESTART), at(q))
        }
        assertTrue("the fixture reaches the margin (${marginPoints.size} points)", marginPoints.size > 10)
        // Without the pill the same margin points are the piece's again.
        for (q in marginPoints) assertEquals("$q without the pill", Hit.Board(placement), HitTest.at(q, g, controller, margin, ShownControls.NONE))
    }

    @Test
    fun theSolvedRowsThirdsAreRetryTheBestTimeAndNextOnlyWhileShown() {
        val controller = PlayController(library, engine)
        val g = ScreenGeometry.of(1080, 2280, density, layout, controller.puzzle)
        controller.boardDpPerUnit = (g.mapping.pxPerUnit / density).toDouble()
        assertEquals(3, g.solvedThirds.size)
        assertEquals(g.tray.left, g.solvedThirds.first().left)
        assertEquals(g.tray.left + g.tray.width, g.solvedThirds.last().left + g.solvedThirds.last().width)
        val y = g.tray.top + g.tray.height / 2f
        val row = ShownControls(restart = null, solvedRow = true)
        val expected = listOf(Control.RETRY, Control.BEST_TIME, Control.SOLVED_NEXT)
        for ((i, third) in g.solvedThirds.withIndex()) {
            assertEquals(third.height, g.tray.height)
            val q = Offset(third.left + third.width / 2f, y)
            assertEquals(Hit.OnControl(expected[i]), HitTest.at(q, g, controller, 12f * density, row))
            assertTrue("without the row the strip is the tray's", HitTest.at(q, g, controller, 12f * density, ShownControls.NONE) !is Hit.OnControl)
        }
    }

    @Test
    fun theBestTimeIsMinutesAndTwoDigitSecondsOrTheDash() {
        assertEquals("–", bestTimeText(null, "–"))
        assertEquals("1:15", bestTimeText(75, "–"))
        assertEquals("0:05", bestTimeText(5, "–"))
        assertEquals("60:00", bestTimeText(3600, "–"))
    }

    /** A full-set puzzle open with one piece (not the parallelogram) placed alone at its solution pose through S-2. */
    private fun placedAlone(): Triple<PlayController, ScreenGeometry, Placement> {
        for (puzzle in library.puzzles.filter { it.kind == PuzzleKind.FULL }) {
            for (sp in puzzle.solution) {
                if (sp.piece == PieceId.PG) continue // its board badge's disc would come before the pill
                val controller = PlayController(library, engine)
                controller.open(puzzle.id)
                val g = ScreenGeometry.of(1080, 2280, density, layout, puzzle)
                controller.boardDpPerUnit = (g.mapping.pxPerUnit / density).toDouble()
                val centre = Point(sp.polygon.sumOf { it.x } / sp.polygon.size, sp.polygon.sumOf { it.y } / sp.polygon.size)
                var pose: Pair<Turn, Boolean>? = null
                for (m in listOf(false, true)) for (s in 0..7) {
                    val v = engine.vertices(sp.piece, Turn(s), m, centre)
                    if (pose == null && v.all { p -> sp.polygon.any { q -> hypot(p.x - q.x, p.y - q.y) < 1e-6 } }) pose = Turn(s) to m
                }
                val (turn, mirrored) = pose ?: continue
                if (mirrored) continue
                if (engine.findLock(puzzle, emptyList(), sp.piece, turn, mirrored, centre, controller.boardDpPerUnit) !is LockResult.Locked) continue
                assertTrue(controller.startDrag(sp.piece, centre))
                val from = controller.dragged!!.turn.step
                val steps = ((turn.step - from) % 8 + 8) % 8
                if (steps != 0) controller.twistDrag(if (steps <= 4) steps else steps - 8)
                controller.moveDrag(centre, overBoard = true)
                val placement = controller.drop() ?: continue
                assertNotNull(placement)
                return Triple(controller, g, placement)
            }
        }
        throw AssertionError("no full-set piece locks alone at its solution pose")
    }
}
