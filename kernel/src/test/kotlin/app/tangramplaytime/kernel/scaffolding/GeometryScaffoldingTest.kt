// scaffolding (WO-001 TASK-001, slice-implementer): disposable self-checks of the TASK-001
// seams. Not acceptance, no verdict; the fixtures are built by hand, not transcribed.
package app.tangramplaytime.kernel.scaffolding

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.kernel.LockRules
import app.tangramplaytime.kernel.PieceShapes
import app.tangramplaytime.kernel.Polygons
import app.tangramplaytime.kernel.Silhouette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GeometryScaffoldingTest {
    private fun sp(id: PieceId, vararg xy: Double) =
        SolvedPiece(id, xy.toList().chunked(2) { Point(it[0], it[1]) })

    private fun near(a: Point, b: Point, tol: Double = 1e-9) = abs(a.x - b.x) <= tol && abs(a.y - b.y) <= tol

    private fun assertSamePointSet(expected: List<Point>, actual: List<Point>) {
        assertEquals("size of $actual", expected.size, actual.size)
        for (e in expected) assertTrue("$e missing from $actual", actual.any { near(it, e) })
    }

    private fun square(x: Double, y: Double, side: Double = 1.0) =
        listOf(Point(x, y), Point(x + side, y), Point(x + side, y + side), Point(x, y + side))

    // A hand-built 4 × 4 classic square cover (not a puzzle file).
    private val squareCover = listOf(
        sp(PieceId.LT1, 0.0, 0.0, 4.0, 0.0, 2.0, 2.0),
        sp(PieceId.LT2, 0.0, 0.0, 2.0, 2.0, 0.0, 4.0),
        sp(PieceId.MT, 4.0, 2.0, 4.0, 4.0, 2.0, 4.0),
        sp(PieceId.ST1, 4.0, 0.0, 4.0, 2.0, 3.0, 1.0),
        sp(PieceId.ST2, 2.0, 2.0, 3.0, 3.0, 1.0, 3.0),
        sp(PieceId.SQ, 2.0, 2.0, 3.0, 1.0, 4.0, 2.0, 3.0, 3.0),
        sp(PieceId.PG, 0.0, 4.0, 1.0, 3.0, 3.0, 3.0, 2.0, 4.0),
    )

    @Test
    fun offsetsKeepTheShapeAreaAndCentreAtEveryTurnAndMirror() {
        for (type in PieceType.entries) for (step in 0..7) for (m in listOf(false, true)) {
            val o = PieceShapes.offsets(type, Turn(step), m)
            assertEquals("$type $step $m area", type.area.toDouble(), Polygons.area(o), 1e-12)
            assertEquals(0.0, o.sumOf { it.x } / o.size, 1e-12)
            assertEquals(0.0, o.sumOf { it.y } / o.size, 1e-12)
        }
    }

    @Test
    fun turnZeroIsTheSpecCornersMinusTheirAverageInOrder() {
        val lt = PieceShapes.offsets(PieceType.LT, Turn(0), false)
        assertTrue(near(lt[0], Point(-2.0, -2.0 / 3)))
        assertTrue(near(lt[1], Point(2.0, -2.0 / 3)))
        assertTrue(near(lt[2], Point(0.0, 4.0 / 3)))
    }

    @Test
    fun twoStepsAreAnExactQuarterTurnClockwiseOnScreen() {
        val t0 = PieceShapes.offsets(PieceType.LT, Turn(0), false)
        val t2 = PieceShapes.offsets(PieceType.LT, Turn(2), false)
        for (i in t0.indices) {
            // y down: (x, y) → (−y, x), with exact 0 and 1 factors.
            assertEquals(-t0[i].y, t2[i].x, 0.0)
            assertEquals(t0[i].x, t2[i].y, 0.0)
        }
    }

    @Test
    fun onlyTheParallelogramMirrors() {
        for (type in PieceType.entries) for (step in 0..7) {
            val plain = PieceShapes.offsets(type, Turn(step), false)
            val flipped = PieceShapes.offsets(type, Turn(step), true)
            if (type == PieceType.PG) assertNotEquals(plain, flipped) else assertEquals(plain, flipped)
        }
        val pg = PieceShapes.offsets(PieceType.PG, Turn(0), false)
        val pgM = PieceShapes.offsets(PieceType.PG, Turn(0), true)
        for (i in pg.indices) assertTrue(near(pgM[i], Point(-pg[i].x, pg[i].y)))
    }

    @Test
    fun areasAndClipsOfHandComputedSquares() {
        val a = square(0.0, 0.0)
        assertEquals(1.0, Polygons.signedArea(a), 0.0)
        assertEquals(-1.0, Polygons.signedArea(a.reversed()), 0.0)
        assertEquals(0.25, Polygons.intersectionArea(a, square(0.5, 0.5)), 1e-12)
        assertEquals(0.25, Polygons.intersectionArea(a.reversed(), square(0.5, 0.5)), 1e-12)
        assertEquals(0.25, Polygons.intersectionArea(a, square(0.5, 0.5).reversed()), 1e-12)
        assertTrue(Polygons.overlaps(a, square(0.5, 0.5)))
        // Sharing an edge or a corner is not an overlap.
        assertFalse(Polygons.overlaps(a, square(1.0, 0.0)))
        assertFalse(Polygons.overlaps(a, square(1.0, 1.0)))
        assertEquals(0.0, Polygons.intersectionArea(a, square(3.0, 3.0)), 0.0)
        assertTrue(Polygons.clipConvex(a, emptyList()).isEmpty())
    }

    @Test
    fun containsAcceptsTheCoverAndRejectsAPieceSticksOut() {
        for (s in squareCover) assertTrue(Silhouette.contains(s.polygon, squareCover))
        assertTrue(Silhouette.contains(square(0.0, 0.0, 4.0), squareCover))
        val st1Shifted = listOf(Point(4.5, 0.0), Point(4.5, 2.0), Point(3.5, 1.0))
        assertFalse(Silhouette.contains(st1Shifted, squareCover))
    }

    @Test
    fun squareCoverHasExactlyItsFourCorners() {
        assertSamePointSet(
            listOf(Point(0.0, 0.0), Point(4.0, 0.0), Point(4.0, 4.0), Point(0.0, 4.0)),
            Silhouette.outlineCorners(squareCover),
        )
    }

    @Test
    fun twoSeparateWedgesSummingTo180DegreesAreACorner() {
        // The three pieces around (2,2) named in the WO-001 design review F1.
        val pinch = listOf(
            sp(PieceId.LT2, 2.0, 2.0, 6.0, 2.0, 4.0, 4.0),
            sp(PieceId.MT, 2.0, 2.0, 2.0, 0.0, 4.0, 2.0),
            sp(PieceId.ST2, 2.0, 2.0, 2.0, 4.0, 1.0, 3.0),
        )
        val corners = Silhouette.outlineCorners(pinch)
        assertTrue("(2,2) missing from $corners", corners.any { near(it, Point(2.0, 2.0)) })
        // (4,2) lies inside LT2's top edge (a half-plane, 180°) and is MT's 45° corner: 225°, a reflex corner.
        assertTrue(corners.any { near(it, Point(4.0, 2.0)) })
        // (6,2) and (4,4) are LT2's own corners; (2,0) is MT's; (2,4) and (1,3) are ST2's.
        assertEquals(7, corners.size)
    }

    @Test
    fun reflexCornerOfAnLIsACornerAndStraightJoinsAreNot() {
        val ell = listOf(
            SolvedPiece(PieceId.SQ, square(0.0, 0.0)),
            SolvedPiece(PieceId.PG, square(1.0, 0.0)),
            SolvedPiece(PieceId.MT, square(0.0, 1.0)),
        )
        assertSamePointSet(
            listOf(Point(0.0, 0.0), Point(2.0, 0.0), Point(2.0, 1.0), Point(1.0, 1.0), Point(1.0, 2.0), Point(0.0, 2.0)),
            Silhouette.outlineCorners(ell),
        )
    }

    @Test
    fun lockRadiusHasTheThirtyDpFloor() {
        assertEquals(0.65, LockRules.lockRadius(100.0), 0.0)
        assertEquals(1.5, LockRules.lockRadius(20.0), 1e-15)
        assertThrows(IllegalArgumentException::class.java) { LockRules.lockRadius(0.0) }
        assertThrows(IllegalArgumentException::class.java) { LockRules.lockRadius(Double.NaN) }
    }
}
