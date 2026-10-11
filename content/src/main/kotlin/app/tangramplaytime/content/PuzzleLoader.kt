// Parse and check every puzzle source, then build the library (designs/WO-002-design.md §2
// `PuzzleLoader.load`, `Violation`, `Rule`). The expected outcome of a bad file is a sealed result, not
// an exception (G-15); only `PuzzleLibraries.bundled` turns a rejection into a build-defect exception.
package app.tangramplaytime.content

import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.PuzzleDefinition

/** The rule a puzzle source breaks (design §2 `Violation`, `Rule`). */
internal enum class Rule {
    /** Not JSON, a wrong JSON type, an unknown key or a missing required field. */
    JSON,

    /** The format tag is not `tangram-puzzle/1`. */
    FORMAT,

    /** The id is not kebab-case, not the file name, or listed more than once. */
    ID,

    /** No `en` or no `fi` title. */
    TITLE,

    /** The category is none of the eight. */
    CATEGORY,

    /** The rating is outside 1..5, or a mini puzzle's is not 1. */
    RATING,

    /** The kind is none of mini, warmup, full. */
    KIND,

    /** A piece id is unknown or repeated; or not all seven (1 to 6 distinct for a mini). */
    PIECE_SET,

    /** A solution entry is not a polygon of 3 or 4 corners of numbers or `[a, b]` pairs. */
    PLACEMENT,

    /** The picture is malformed (decisions.md 2026-10-11, "a well-formed picture"). */
    ART,

    /** A piece's polygon is not its TYPE-001 shape at one of the 8 turns, corners in boundary order. */
    CONGRUENT,

    /** Two solution pieces overlap. */
    OVERLAP,

    /** The pieces are not all joined through shared edges. */
    CONNECTED,

    /** A warm-up has a piece with less than half its outline on the silhouette edge. */
    WARMUP,

    /** There is no puzzle to list. */
    EMPTY,
}

/**
 * One broken rule.
 *
 * @property file the source's file name, exactly [PuzzleSource.fileName]; empty for [Rule.EMPTY], which
 *   belongs to no file.
 * @property rule the rule broken.
 * @property detail what is wrong, for the author.
 */
internal data class Violation(val file: String, val rule: Rule, val detail: String)

/** The outcome of [PuzzleLoader.load]. */
internal sealed interface LoadResult {
    /** Every source passed every rule; [library] lists them all. */
    data class Loaded(val library: IPuzzleLibrary) : LoadResult

    /** At least one rule is broken; [violations] holds every violation of every source, and nothing is listed. */
    data class Rejected(val violations: List<Violation>) : LoadResult
}

internal object PuzzleLoader {
    /**
     * Parses every source and, when its parse is clean, checks its geometry with [engine]; then applies
     * the library rules: ids are unique (`ID`) and there is at least one puzzle (`EMPTY`).
     *
     * @return [LoadResult.Loaded] when no rule is broken, otherwise [LoadResult.Rejected] with every violation.
     */
    fun load(sources: List<PuzzleSource>, engine: ILockEngine): LoadResult {
        val check = ValidityCheck(engine)
        val violations = mutableListOf<Violation>()
        val parsed = mutableListOf<Pair<String, PuzzleDefinition>>()
        for (source in sources) {
            when (val result = PuzzleParser.parse(source)) {
                is Parsed.Bad -> violations += result.violations
                is Parsed.Ok -> {
                    parsed += source.fileName to result.def
                    violations += check.violations(source.fileName, result.def)
                }
            }
        }
        for ((id, group) in parsed.groupBy { it.second.id }) {
            if (group.size < 2) continue
            for (file in group.map { it.first }.distinct()) {
                violations += Violation(file, Rule.ID, "the id \"$id\" is listed ${group.size} times")
            }
        }
        if (sources.isEmpty()) {
            violations += Violation("", Rule.EMPTY, "no puzzle file to list")
        }
        return if (violations.isEmpty()) {
            LoadResult.Loaded(PuzzleLibrary(parsed.map { it.second }))
        } else {
            LoadResult.Rejected(violations.toList())
        }
    }
}
