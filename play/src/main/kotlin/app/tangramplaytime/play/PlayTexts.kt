// WO-003 TASK-012 · seam S-11 (tasks.md § WO-003 › Seams) · REQ-012 rule "The puzzle's title is shown in the
// game's language (REQ-047)", REQ-043 (the S/M/L letters) · architecture.md G-10 / O-06: every user-visible
// string is an `:app` resource; `:play` receives the texts and never holds a literal.
// WO-004 TASK-020 · seam S-11Δ (designs/WO-004-design.md §2): the texts of the top bar (REQ-024 rule: the number
// out of the total and the state), Restart (REQ-025), the solved row (REQ-026: Retry, the best time, Next) and the
// grid overview (REQ-050: its title and Done, drawn by TASK-021). No field has a default: a default would be a
// literal user-visible string in Kotlin (G-10).
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
 * @property restart the label of the Restart pill, shown only In progress (REQ-025).
 * @property retry the label of the solved row's Retry (REQ-026).
 * @property next the label of the solved row's Next (REQ-026).
 * @property bestTime the label over the solved row's best time (REQ-026).
 * @property noTime what the best time reads while none is stored (decisions.md 2026-10-15 "The top bar, Restart
 *   and the solved row, as drawn": "–" until WO-008 computes times).
 * @property inProgress the top bar's state word for In progress (REQ-024 rule; TYPE-006).
 * @property solved the top bar's state word for Solved (REQ-024 rule; TYPE-006); New has none.
 * @property counterFormat the top bar's "n / N": a format with the 1-based number (`%1$d`) and the total
 *   (`%2$d`) (REQ-024 rule: "its number out of the total").
 * @property allPuzzles the grid overview's title (REQ-050).
 * @property done the grid overview's Done (REQ-050).
 */
data class PlayTexts(
    val sizeSmall: String,
    val sizeMedium: String,
    val sizeLarge: String,
    val titleLanguage: String,
    val restart: String,
    val retry: String,
    val next: String,
    val bestTime: String,
    val noTime: String,
    val inProgress: String,
    val solved: String,
    val counterFormat: String,
    val allPuzzles: String,
    val done: String,
)

/**
 * The title the screen shows for [puzzle]: `titles[titleLanguage]`, else `titles["en"]` (REQ-012 and
 * REQ-047 rules; S-11). Every library puzzle has an `en` title (the `PuzzleDefinition` KDoc); the last
 * resorts never show for a listed puzzle.
 */
internal fun PlayTexts.titleOf(puzzle: PuzzleDefinition): String =
    puzzle.titles[titleLanguage] ?: puzzle.titles["en"] ?: puzzle.titles.values.firstOrNull() ?: puzzle.id

/** The top bar's "n / N" for puzzle [number] of [count] (S-11Δ `counterFormat`; REQ-024 rule). */
internal fun PlayTexts.counterOf(number: Int, count: Int): String = counterFormat.format(java.util.Locale.ROOT, number, count)
