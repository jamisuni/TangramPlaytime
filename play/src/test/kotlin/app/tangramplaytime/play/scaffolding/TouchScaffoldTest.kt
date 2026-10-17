// scaffolding — TASK-013's own disposable checks of the pure parts of the touch routing: the px hit test
// (REQ-015's touch areas) and the dragged piece's drawn centre and scale (no token; the acceptance verdict is
// the device kit's DragKitTest and PreviewKitTest). WO-004 TASK-020: `HitTest.at` takes the controls shown (S-22);
// these points lie on the board and the tray with no Restart pill and no solved row (`ShownControls.NONE`).
package app.tangramplaytime.play.scaffolding

import androidx.compose.ui.geometry.Offset
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.BoardMapping
import app.tangramplaytime.play.DragOrigin
import app.tangramplaytime.play.DragVisual
import app.tangramplaytime.play.Dragged
import app.tangramplaytime.play.Hit
import app.tangramplaytime.play.HitTest
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlayLayout
import app.tangramplaytime.play.ScreenGeometry
import app.tangramplaytime.play.ShownControls
import app.tangramplaytime.play.distanceToEdges
import app.tangramplaytime.play.drawnDrag
import app.tangramplaytime.play.easeOutCubic
import app.tangramplaytime.play.insidePolygon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class TouchScaffoldTest {

    private val engine = LockEngine()
    private val library = PuzzleLibraries.bundled(engine)
    private val layout = PlayLayout(
        trayRows = listOf(listOf(PieceId.LT1, PieceId.LT2, PieceId.MT), listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)),
        floatOffsetDp = 30f,
        cellGapDp = 8f,
        sideMarginDp = 8f,
        topBarDp = 66f,
    )
    private val density = 2.625f // the dev image's 420 dpi

    @Test
    fun polygonHelpersTellInsideFromTheMarginFromOutside() {
        val tri = listOf(Offset(0f, 0f), Offset(100f, 0f), Offset(0f, 100f))
        assertTrue(insidePolygon(Offset(20f, 20f), tri))
        assertFalse(insidePolygon(Offset(80f, 80f), tri))
        // 10 px below the bottom edge's midpoint (the edge from (0,0) to (100,0) is the top edge; use the left one).
        assertEquals(10f, distanceToEdges(Offset(-10f, 50f), tri), 1e-4f)
        assertEquals(14f, distanceToEdges(Offset(-14f, 50f), tri), 1e-4f)
        // The hypotenuse: a point 10 px out along its normal.
        val n = Offset(1f / hypot(1f, 1f), 1f / hypot(1f, 1f))
        assertEquals(10f, distanceToEdges(Offset(50f + n.x * 10f, 50f + n.y * 10f), tri), 1e-3f)
    }

    @Test
    fun hitTestFindsATrayPieceInItsWholeCellAndAPlacedPieceWithinTwelveDp() {
        val controller = PlayController(library, engine)
        val puzzle = library.puzzles.first { it.kind == PuzzleKind.FULL }
        assertTrue(controller.open(puzzle.id))
        val g = ScreenGeometry.of((411 * density).toInt(), (914 * density).toInt(), density, layout, puzzle)
        controller.boardDpPerUnit = (g.mapping.pxPerUnit / density).toDouble()
        val marginPx = 12f * density

        // A tray piece: anywhere in its cell, including a corner of the cell far from the miniature.
        val cell = g.cells.getValue(PieceId.LT1)
        assertEquals(Hit.Tray(PieceId.LT1), HitTest.at(Offset(cell.leftPx + 1f, cell.topPx + 1f), g, controller, marginPx, ShownControls.NONE))
        // The board with nothing placed: nothing.
        assertNull(HitTest.at(g.mapping.toPx(Point(2.0, 2.0)), g, controller, marginPx, ShownControls.NONE))

        // Place ST1 at its solution pose through S-2 (where the engine locks it alone).
        val sp = puzzle.solution.single { it.piece == PieceId.ST1 }
        val centre = Point(sp.polygon.sumOf { it.x } / sp.polygon.size, sp.polygon.sumOf { it.y } / sp.polygon.size)
        var pose: Pair<Turn, Boolean>? = null
        for (m in listOf(false, true)) for (s in 0..7) {
            val v = engine.vertices(PieceId.ST1, Turn(s), m, centre)
            if (v.all { p -> sp.polygon.any { q -> hypot(p.x - q.x, p.y - q.y) < 1e-6 } }) pose = Turn(s) to m
        }
        val (turn, mirrored) = pose ?: error("no pose reproduces ST1's solution polygon")
        val lock = engine.findLock(puzzle, emptyList(), PieceId.ST1, turn, mirrored, centre, controller.boardDpPerUnit)
        if (lock !is LockResult.Locked) return // this puzzle's ST1 does not lock alone; the device kit searches the library
        assertTrue(controller.startDrag(PieceId.ST1, centre))
        val from = controller.dragged!!.turn.step
        val steps = ((turn.step - from) % 8 + 8) % 8
        if (steps != 0) controller.twistDrag(if (steps <= 4) steps else steps - 8)
        controller.moveDrag(centre, overBoard = true)
        val placement = controller.drop() ?: error("ST1 did not lock")

        // Its empty cell is nothing now; the piece's shape and its 12 dp margin are the piece.
        val st1Cell = g.cells.getValue(PieceId.ST1)
        assertNull(HitTest.at(Offset(st1Cell.leftPx + 1f, st1Cell.topPx + 1f), g, controller, marginPx, ShownControls.NONE))
        val poly = engine.vertices(placement.piece, placement.turn, placement.mirrored, placement.centre).map(g.mapping::toPx)
        assertEquals(Hit.Board(placement), HitTest.at(g.mapping.toPx(placement.centre), g, controller, marginPx, ShownControls.NONE))
        val a = poly[0]
        val b = poly[1]
        val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
        val cx = poly.map { it.x }.average().toFloat()
        val cy = poly.map { it.y }.average().toFloat()
        var nx = -(b.y - a.y)
        var ny = b.x - a.x
        val len = hypot(nx, ny)
        nx /= len
        ny /= len
        if ((mid.x - cx) * nx + (mid.y - cy) * ny < 0) { nx = -nx; ny = -ny }
        assertEquals(Hit.Board(placement), HitTest.at(Offset(mid.x + nx * 10f * density, mid.y + ny * 10f * density), g, controller, marginPx, ShownControls.NONE))
        assertNull(HitTest.at(Offset(mid.x + nx * 14f * density, mid.y + ny * 14f * density), g, controller, marginPx, ShownControls.NONE))
    }

    @Test
    fun theDrawnDragGrowsFromItsOriginToTheControllersCentreAtBoardScale() {
        val mapping = BoardMapping(originXPx = 100f, originYPx = 200f, pxPerUnit = 80f)
        val d = Dragged(PieceId.MT, Turn(1), false, Point(2.0, 1.5), fromTray = true, overBoard = true)
        val visual = DragVisual()
        // No origin (a drag the router did not start): grown.
        assertEquals(mapping.toPx(d.centre) to 80f, drawnDrag(d, visual, mapping))
        visual.begin(DragOrigin(PieceId.MT, Offset(50f, 900f), 20f, fromTray = true))
        assertEquals(0f, visual.grow, 0f)
        assertEquals(Offset(50f, 900f) to 20f, drawnDrag(d, visual, mapping))
        visual.grow = 0.5f
        val k = easeOutCubic(0.5f)
        val (c, s) = drawnDrag(d, visual, mapping)
        assertEquals(20f + 60f * k, s, 1e-4f)
        assertEquals(50f + (mapping.toPx(d.centre).x - 50f) * k, c.x, 1e-3f)
        visual.grow = 1f
        assertEquals(mapping.toPx(d.centre) to 80f, drawnDrag(d, visual, mapping))
        assertEquals(0f, easeOutCubic(0f), 0f)
        assertEquals(1f, easeOutCubic(1f), 0f)
        // Ended: the glide remembers where the piece was; the origin is gone.
        visual.end(PieceId.MT, Offset(1f, 2f), 80f)
        assertNull(visual.origin)
        assertEquals(PieceId.MT, visual.glide!!.piece)
        visual.glideDone(visual.glide!!)
        assertNull(visual.glide)
    }
}
