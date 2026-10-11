// scaffolding (WO-001 TASK-002, slice-implementer): disposable self-checks of the LockEngine
// wrappers and the anchors seam. Not acceptance, no verdict; the fixtures are built by hand, not
// transcribed.
package app.tangramplaytime.kernel.scaffolding

import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.kernel.anchors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LockEngineScaffoldingTest {
    private val engine = LockEngine()

    private fun pt(x: Double, y: Double) = Point(x, y)

    private fun sp(id: PieceId, vararg xy: Double) =
        SolvedPiece(id, xy.toList().chunked(2) { Point(it[0], it[1]) })

    private fun puzzle(vararg solution: SolvedPiece) = PuzzleDefinition(
        "scaffold", mapOf("en" to "scaffold"), "shapes", 1, PuzzleKind.MINI,
        solution.toList(), ArtLayer("#000000", emptyList()), false,
    )

    private fun near(a: Point, b: Point, tol: Double = 1e-9) = abs(a.x - b.x) <= tol && abs(a.y - b.y) <= tol

    private fun assertLocked(expected: Placement, result: LockResult) {
        assertTrue("expected Locked, was $result", result is LockResult.Locked)
        val got = (result as LockResult.Locked).placement
        assertEquals(expected.piece, got.piece)
        assertEquals(expected.turn, got.turn)
        assertEquals(expected.mirrored, got.mirrored)
        assertTrue("centre ${got.centre} vs ${expected.centre}", near(expected.centre, got.centre))
    }

    // A diamond (0,0) (1,-1) (2,0) (1,1) of two small triangles; its four corners are outline corners.
    private val diamond = puzzle(
        sp(PieceId.ST1, 0.0, 0.0, 2.0, 0.0, 1.0, 1.0),
        sp(PieceId.ST2, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0),
    )

    // Small triangle at turn 0 centred (1, 1/3): corners (0,0) (2,0) (1,1).
    private val st1Up = Placement(PieceId.ST1, Turn(0), false, pt(1.0, 1.0 / 3))

    // Small triangle at turn 4 centred (1, -1/3): corners (2,0) (0,0) (1,-1).
    private val st2Down = Placement(PieceId.ST2, Turn(4), false, pt(1.0, -1.0 / 3))

    @Test
    fun anchors_areOutlineCornersThenPlacedCornersInListOrder() {
        val a = engine.anchors(diamond, listOf(st1Up))
        val outline = engine.outlineCorners(diamond)
        assertEquals(4, outline.size)
        assertEquals(7, a.size)
        assertEquals(outline, a.subList(0, 4))
        assertEquals(engine.vertices(st1Up.piece, st1Up.turn, st1Up.mirrored, st1Up.centre), a.subList(4, 7))
        assertEquals(outline, engine.anchors(diamond, emptyList()))
    }

    @Test
    fun findLock_locksIntoTheHoleAtTheExactCentre() {
        val r = engine.findLock(diamond, listOf(st1Up), PieceId.ST2, Turn(4), false, pt(1.1, -1.0 / 3 + 0.1), 100.0)
        assertLocked(st2Down, r)
    }

    @Test
    fun tryTurnInPlace_turnsAboutTheCentreAndSearchesFromThere() {
        // Turned to 4 at (1, 1/3) its corners are (2,2/3) (0,2/3) (1,-1/3): each 2/3 from an anchor.
        assertSame(LockResult.None, engine.tryTurnInPlace(diamond, emptyList(), st1Up, Turn(4), 100.0))
        // With R = 1.5 the nearest valid candidate (d = 2/3, three snug corners) is the lower half.
        assertLocked(st2Down.copy(piece = PieceId.ST1), engine.tryTurnInPlace(diamond, emptyList(), st1Up, Turn(4), 20.0))
    }

    @Test
    fun tryTurnInPlace_passesTheFindLockPreconditionThrough() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.tryTurnInPlace(diamond, listOf(st1Up), st1Up, Turn(1), 100.0)
        }
    }

    @Test
    fun tryMirrorInPlace_flipsTheParallelogramAndKeepsTheTurn() {
        val pg = Placement(PieceId.PG, Turn(0), false, pt(1.5, -0.5))
        // Its mirror image at (1.5, -0.5) is (3,0) (1,0) (0,-1) (2,-1): exactly this silhouette.
        val mirroredHole = puzzle(sp(PieceId.PG, 3.0, 0.0, 1.0, 0.0, 0.0, -1.0, 2.0, -1.0))
        assertLocked(pg.copy(mirrored = true), engine.tryMirrorInPlace(mirroredHole, emptyList(), pg, 100.0))
        // On the unmirrored silhouette every mirrored corner is 1.0 from every anchor: no lock.
        val plainHole = puzzle(sp(PieceId.PG, 0.0, 0.0, 2.0, 0.0, 3.0, -1.0, 1.0, -1.0))
        assertSame(LockResult.None, engine.tryMirrorInPlace(plainHole, emptyList(), pg, 100.0))
    }

    @Test
    fun stillValid_keepsInsideNonOverlappingPlacementsInListOrder() {
        val both = listOf(st1Up, st2Down)
        assertEquals(both, engine.stillValid(diamond, both))
        val clash = st2Down.copy(turn = Turn(0), centre = st1Up.centre)
        assertEquals(listOf(st1Up), engine.stillValid(diamond, listOf(st1Up, clash)))
        // A placement outside the silhouette is dropped and does not block the next one.
        val outside = st1Up.copy(centre = pt(5.0, 5.0))
        assertEquals(listOf(clash), engine.stillValid(diamond, listOf(outside, clash)))
    }

    @Test
    fun isSolved_isTheSetOfPuzzlePieces() {
        assertFalse(engine.isSolved(diamond, emptyList()))
        assertFalse(engine.isSolved(diamond, listOf(st1Up)))
        assertTrue(engine.isSolved(diamond, listOf(st1Up, st2Down)))
        assertFalse(engine.isSolved(diamond, listOf(st1Up, st1Up)))
    }
}
