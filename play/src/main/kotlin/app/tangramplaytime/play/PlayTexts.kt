// WO-003 TASK-012 · seam S-11 (tasks.md § WO-003 › Seams) · REQ-012 rule "The puzzle's title is shown in the
// game's language (REQ-047)", REQ-043 (the S/M/L letters) · architecture.md G-10 / O-06: every user-visible
// string is an `:app` resource; `:play` receives the texts and never holds a literal.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PuzzleDefinition

/**
 * The texts the Play screen shows, handed in by `:app` (O-06, G-10).
 *
 * @property sizeSmall the size mark of a small triangle (REQ-043: "S" in both languages).
 * @property sizeMedium the size mark of the medium triangle ("M").
 * @property sizeLarge the size mark of a large triangle ("L").
 * @property titleLanguage the language code the puzzle title is picked in (`fi` or `en`, REQ-047); a
 *   title missing in that language falls back to `en` (REQ-047 rule).
 */
data class PlayTexts(
    val sizeSmall: String,
    val sizeMedium: String,
    val sizeLarge: String,
    val titleLanguage: String,
)

/**
 * The title the screen shows for [puzzle]: `titles[titleLanguage]`, else `titles["en"]` (REQ-012 and
 * REQ-047 rules; S-11). Every library puzzle has an `en` title (the `PuzzleDefinition` KDoc); the last
 * resorts never show for a listed puzzle.
 */
internal fun PlayTexts.titleOf(puzzle: PuzzleDefinition): String =
    puzzle.titles[titleLanguage] ?: puzzle.titles["en"] ?: puzzle.titles.values.firstOrNull() ?: puzzle.id
