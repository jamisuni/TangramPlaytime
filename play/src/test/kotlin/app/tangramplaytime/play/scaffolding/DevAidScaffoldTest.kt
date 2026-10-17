// scaffolding — TASK-024's own disposable checks of three readings of S-26 / S-27 that the acceptance kit leaves
// open, each proposed as a decisions.md row in the TASK-024 hand-back: a twist tie of four steps turns clockwise;
// a refusal for a Solved puzzle or for a puzzle with no order closes the sheet and changes nothing in the game; a
// board scale that is not > 0 throws before the sheet closes. Not an acceptance test: it carries no token, and
// DevAidKitTest.kt is the S-26 / S-27 verdict.
package app.tangramplaytime.play.scaffolding

import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlayEvent
import app.tangramplaytime.play.devaid.DevAid
import app.tangramplaytime.play.devaid.DevSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DevAidScaffoldTest {
    private val engine = LockEngine()
    private val library = PuzzleLibraries.bundled(engine)
    private val scale = 60.0

    private fun unlockedAid(): DevAid = DevAid().apply {
        onButton()
        assertTrue(submit("0417"))
    }

    /** The lowest unmirrored turn whose engine shape is [sp]'s polygon (a test-side copy of S-27 step 1, non-PG only). */
    private fun poseStep(sp: SolvedPiece): Int {
        val n = sp.polygon.size
        val centre = Point(sp.polygon.sumOf { it.x } / n, sp.polygon.sumOf { it.y } / n)
        fun near(a: Point, b: Point) = abs(a.x - b.x) <= 1e-6 && abs(a.y - b.y) <= 1e-6
        return (0..7).first { s ->
            val v = engine.vertices(sp.piece, Turn(s), false, centre)
            v.size == n && v.all { p -> sp.polygon.any { near(p, it) } } && sp.polygon.all { p -> v.any { near(p, it) } }
        }
    }

    @Test
    fun aTwistTieOfFourStepsTurnsClockwise() {
        val p = library.puzzles.first()
        val sp = p.solution.first { it.piece != PieceId.PG }
        val start = (poseStep(sp) + 4) % 8
        val seen = mutableListOf<Int>()
        lateinit var c: PlayController
        c = PlayController(library, engine) { e ->
            val held = c.dragged
            if (e is PlayEvent.Turned && e.piece == sp.piece && held != null && held.piece == sp.piece) seen += held.turn.step
        }
        c.boardDpPerUnit = scale
        repeat(8) { if (c.trayTurn(sp.piece).step != start) c.tapTray(sp.piece) }
        assertEquals("the tray turn is four steps from the pose", start, c.trayTurn(sp.piece).step)
        assertTrue(unlockedAid().solveNow(c))
        assertEquals("four clockwise steps (Turn.next)", (1..4).map { (start + it) % 8 }, seen)
    }

    @Test
    fun aRefusalForASolvedPuzzleClosesTheSheetAndABadScaleThrowsBeforeTheSheetCloses() {
        val c = PlayController(library, engine).apply { boardDpPerUnit = scale }
        val aid = unlockedAid()
        assertTrue(aid.solveNow(c))
        assertEquals(PuzzleState.SOLVED, c.state)
        aid.onButton()
        val placed = c.placed
        val celebration = c.celebration
        assertFalse(aid.solveNow(c))
        assertEquals("a refusal on a Solved puzzle closes the sheet (S-26 \"closes, then S-27\")", DevSheet.NONE, aid.sheet)
        assertEquals(placed, c.placed)
        assertEquals(celebration, c.celebration)

        c.next()
        c.boardDpPerUnit = 0.0
        aid.onButton()
        assertThrows(IllegalStateException::class.java) { aid.solveNow(c) }
        assertEquals("a programming error changes nothing, the sheet included", DevSheet.MENU, aid.sheet)
        assertEquals(PuzzleState.NEW, c.state)
    }

    @Test
    fun aPuzzleWithNoOrderIsRefusedWithTheSheetClosedAndTheGameUnchanged() {
        val real = library.puzzles.first { q -> q.solution.map { it.piece.type }.distinct().size > 1 }
        val a = real.solution[0]
        val b = real.solution.first { it.piece.type != a.piece.type }
        // Two pieces of different shapes swap polygons, so neither has a pose: no order exists (S-27).
        val tampered = real.copy(
            solution = real.solution.map {
                when (it) {
                    a -> it.copy(polygon = b.polygon)
                    b -> it.copy(polygon = a.polygon)
                    else -> it
                }
            },
        )
        val lib = object : IPuzzleLibrary {
            override val puzzles: List<PuzzleDefinition> = listOf(tampered)
            override fun byId(id: String): PuzzleDefinition? = puzzles.firstOrNull { it.id == id }
        }
        val events = mutableListOf<PlayEvent>()
        val c = PlayController(lib, engine) { events += it }.apply { boardDpPerUnit = scale }
        val aid = unlockedAid()
        val before = listOf(c.state, c.placed, c.trayPieces.toList(), c.celebration)
        assertFalse(aid.solveNow(c))
        assertEquals(DevSheet.NONE, aid.sheet)
        assertEquals(before, listOf(c.state, c.placed, c.trayPieces.toList(), c.celebration))
        assertTrue("no event", events.isEmpty())
    }
}
