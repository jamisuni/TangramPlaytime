// scaffolding (WO-003 TASK-010): proves that the bundled puzzles load on :play's unit-test classpath through
// `PuzzleLibraries.bundled(LockEngine())` once :content is a testImplementation dependency; no REQ token.
package app.tangramplaytime.play.scaffolding

import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.kernel.LockEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BundleLoadScaffoldTest {

    /** The project's `Tangrams/` folder, found by walking up from the test's working directory (the module). */
    private fun tangramsFolder(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "Tangrams")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile
        }
        throw AssertionError("no Tangrams/ folder above ${File("").absolutePath}")
    }

    @Test
    fun theBundledLibraryListsOnePuzzlePerTopLevelPuzzleFile() {
        val files = tangramsFolder().listFiles { f -> f.isFile && f.name.endsWith(".json") && !f.name.endsWith(".schema.json") }
            ?.map { it.nameWithoutExtension }?.sorted()
            ?: throw AssertionError("Tangrams/ is unreadable")
        assertTrue("fixture: Tangrams/ holds puzzle files", files.isNotEmpty())
        val library = PuzzleLibraries.bundled(LockEngine())
        assertEquals("one listed puzzle per top-level Tangrams/*.json", files, library.puzzles.map { it.id }.sorted())
        assertTrue("every listed puzzle is found by id", library.puzzles.all { library.byId(it.id) === it })
    }
}
