// scaffolding (WO-004, TASK-017): the exact text FileProgressStore writes, against the ADR-003 shape
// (designs/WO-004-design.md §1): key names and order, `best` and `date` written as null, `lastOpened` left
// out until one is saved. Disposable; no REQ token.
package app.tangramplaytime.store.scaffolding

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.store.FileProgressStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WrittenShapeScaffoldTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun theWrittenTextIsTheAdr003Shape() {
        val file = File(tmp.newFolder(), "progress.json")
        val store = FileProgressStore(file)
        val progress = PuzzleProgress(
            PuzzleState.IN_PROGRESS,
            listOf(Placement(PieceId.PG, Turn(7), true, Point(1.5, 2.0))),
            12,
            null,
        )
        runBlocking { store.saveProgress("cat", progress) }
        assertEquals(
            """{"format":1,"puzzles":{"cat":{"state":"IN_PROGRESS","placed":[{"piece":"PG","turn":7,"mirrored":true,"x":1.5,"y":2.0}],"time":12,"best":null}},""" +
                """"playTimes":{"today":0,"date":null,"allTime":0},"settings":{"timerShown":false,"soundOn":true}}""",
            file.readText(Charsets.UTF_8),
        )

        runBlocking { store.saveLastOpenedPuzzleId("cat") }
        assertTrue("lastOpened follows format", file.readText(Charsets.UTF_8).startsWith("""{"format":1,"lastOpened":"cat","puzzles":"""))
        assertFalse("no temporary file is left after a save", File(file.path + ".tmp").exists())
    }
}
