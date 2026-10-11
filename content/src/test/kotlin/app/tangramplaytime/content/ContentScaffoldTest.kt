// scaffolding (P2): proves :content compiles and tests against :contracts; no REQ token
package app.tangramplaytime.content

import app.tangramplaytime.contracts.PuzzleKind
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentScaffoldTest {
    @Test
    fun seesTheContractsModule() {
        assertEquals(3, PuzzleKind.entries.size)
    }

    // scaffolding (WO-002 TASK-004): the bundle read through the one ClassLoader path holds one
    // UTF-8 source per top-level Tangrams/*.json except the schema — compared with the folder
    // listing, not a fixed count. The test runs with the module folder (content/) as working dir.
    @Test
    fun bundleCarriesEveryTopLevelPuzzleFile() {
        val folder = File("../Tangrams")
        assertTrue("Tangrams folder not found from ${File("").absolutePath}", folder.isDirectory)
        val expected = folder.listFiles()!!
            .filter { it.isFile && it.name.endsWith(".json") && !it.name.endsWith(".schema.json") }
            .map { it.name }
            .sorted()
        assertTrue("no puzzle files listed in ${folder.absolutePath}", expected.isNotEmpty())

        val sources = BundledPuzzles.read()

        assertEquals(expected, sources.map { it.fileName })
        for (source in sources) {
            assertTrue("${source.fileName} is empty", source.text.isNotBlank())
            assertEquals(
                "${source.fileName} differs from its Tangrams/ file read as UTF-8",
                File(folder, source.fileName).readText(Charsets.UTF_8),
                source.text,
            )
        }
        assertTrue(
            "\"Lämmittely\" not decoded in any bundled text",
            sources.any { it.text.contains("Lämmittely") },
        )
    }
}
