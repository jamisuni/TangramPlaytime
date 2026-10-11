// The one public door of `:content` (designs/WO-002-design.md §2 `PuzzleLibraries.bundled`). Every
// parsing type stays internal (`IPuzzleLibrary` KDoc: never expose a path, JSON or a parsing type).
package app.tangramplaytime.content

import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.IPuzzleLibrary

/** Builds the game's puzzle library (REQ-007) from the puzzle files that ship with the app. */
object PuzzleLibraries {
    /**
     * The library of the bundled `tangram-puzzle/1` files, read through the one reading path
     * (`BundledPuzzles.read`) and checked against every validity rule, the geometry through [engine].
     * Build it once per process; the library is fixed for the process's lifetime.
     *
     * A bundled file that breaks a rule is a build defect, not an expected outcome (G-15; decisions.md
     * 2026-10-11, "fails fast"): nothing is listed, and this throws [IllegalStateException] naming every
     * violation (file, rule, detail).
     *
     * @param engine the lock engine whose piece geometry and overlap test the check uses (G-12: injected).
     * @return the library, every listed puzzle valid.
     */
    fun bundled(engine: ILockEngine): IPuzzleLibrary =
        when (val result = PuzzleLoader.load(BundledPuzzles.read(), engine)) {
            is LoadResult.Loaded -> result.library
            is LoadResult.Rejected -> throw IllegalStateException(
                "bundled puzzles break the validity rules (a build defect):\n" +
                    result.violations.joinToString("\n") { "${it.file.ifEmpty { "(library)" }}: ${it.rule}: ${it.detail}" },
            )
        }
}
