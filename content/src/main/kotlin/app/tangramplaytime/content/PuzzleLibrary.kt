// The `IPuzzleLibrary` implementation (designs/WO-002-design.md §2 `PuzzleLibrary`): a fixed, ordered
// list built once from puzzles that passed every rule (`PuzzleLoader.load`).
package app.tangramplaytime.content

import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.PuzzleDefinition
import java.util.Collections

/**
 * The library of [defs], each valid and each id once (the loader's rules).
 *
 * [puzzles] is in ‹ › order (REQ-040): by kind (the enum order MINI, WARMUP, FULL), then rating, then id,
 * and cannot be modified, so the library never changes while the app runs (`IPuzzleLibrary` KDoc).
 */
internal class PuzzleLibrary(defs: List<PuzzleDefinition>) : IPuzzleLibrary {
    override val puzzles: List<PuzzleDefinition> =
        Collections.unmodifiableList(defs.sortedWith(compareBy<PuzzleDefinition>({ it.kind }, { it.rating }, { it.id })))

    private val index: Map<String, PuzzleDefinition> = puzzles.associateBy { it.id }

    /** The listed puzzle with [id], or null when none has it (an expected outcome, G-15). */
    override fun byId(id: String): PuzzleDefinition? = index[id]
}
