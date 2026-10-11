// The one reading path for the shipped puzzles (designs/WO-002-design.md §2 `BundledPuzzles.read`;
// decisions.md 2026-10-11, "Java resources"). The `bundlePuzzles` Gradle task writes the files as
// Java resources; the same ClassLoader call reads them in the JVM unit test and on the device.
package app.tangramplaytime.content

/** One shipped puzzle file: its file name (for example `animals-cat.json`) and its UTF-8 text. */
internal data class PuzzleSource(val fileName: String, val text: String)

/**
 * Reads the bundled puzzle files in the order of the generated `index.txt` (sorted file names).
 *
 * A missing resource is a build defect, not an expected outcome, so it throws
 * [IllegalStateException] (G-15).
 */
internal object BundledPuzzles {
    const val DIR = "app/tangramplaytime/content/puzzles"

    fun read(): List<PuzzleSource> =
        resourceText("$DIR/index.txt")
            .lines()
            .filter { it.isNotBlank() }
            .map { name -> PuzzleSource(name, resourceText("$DIR/$name")) }

    private fun resourceText(path: String): String {
        val stream = BundledPuzzles::class.java.classLoader?.getResourceAsStream(path)
            ?: throw IllegalStateException("bundled puzzle resource missing: $path")
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
