// WO-003 TASK-012 · seam S-11 (tasks.md § WO-003 › Seams) · REQ-012 rule "The puzzle's title is shown in the
// game's language (REQ-047)", REQ-043 (the S/M/L letters) · architecture.md G-10 / O-06: every user-visible
// string is an `:app` resource; `:play` receives the texts and never holds a literal.
// WO-004 TASK-020 · seam S-11Δ (designs/WO-004-design.md §2): the texts of the top bar (REQ-024 rule: the number
// out of the total and the state), Restart (REQ-025), the solved row (REQ-026: Retry, the best time, Next) and the
// grid overview (REQ-050: its title and Done, drawn by TASK-021). No field has a default: a default would be a
// literal user-visible string in Kotlin (G-10).
// WO-008 TASK-047 · seams S-11Δ3 and S-48 (designs/WO-008-design.md §2) · REQ-029 rule 2 (m:ss below one hour, h and
// min from one hour) · decisions.md 2026-10-20 "One format for every time the game shows …" and 2026-10-19 "The counter
// keeps ASCII digits under every device language" · G-10: "h" and "min" come in as `hoursMinutes`, with no default.
// WO-007 TASK-056 · seam S-11Δ4 (designs/WO-007-design.md §2) · REQ-029 rule 2, REQ-047: `timeOf` and `titleOf` become
// public, so `:app`'s settings screen shows its play times and best-time titles through the one format and the one title
// rule. Nothing else changes.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PuzzleDefinition
import java.util.Locale

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
 *   and the solved row, as drawn": "–").
 * @property inProgress the top bar's state word for In progress (REQ-024 rule; TYPE-006).
 * @property solved the top bar's state word for Solved (REQ-024 rule; TYPE-006); New has none.
 * @property counterFormat the top bar's "n / N": a format with the 1-based number (`%1$d`) and the total
 *   (`%2$d`) (REQ-024 rule: "its number out of the total").
 * @property allPuzzles the grid overview's title (REQ-050).
 * @property done the grid overview's Done (REQ-050).
 * @property hoursMinutes a time of one hour or more: a format with the whole hours (`%1$d`) and the minutes left over
 *   (`%2$d`), "%1$d h %2$d min" (S-11Δ3; REQ-029 rule 2: "as h and min from one hour up").
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
    val hoursMinutes: String,
)

/**
 * The title the screen shows for [puzzle]: `titles[titleLanguage]`, else `titles["en"]` (REQ-012 and
 * REQ-047 rules; S-11). Every library puzzle has an `en` title (the `PuzzleDefinition` KDoc); the last
 * resorts never show for a listed puzzle. Public since WO-007 (S-11Δ4): the settings screen's best times name their
 * puzzles with it.
 */
fun PlayTexts.titleOf(puzzle: PuzzleDefinition): String =
    puzzle.titles[titleLanguage] ?: puzzle.titles["en"] ?: puzzle.titles.values.firstOrNull() ?: puzzle.id

/** The top bar's "n / N" for puzzle [number] of [count] (S-11Δ `counterFormat`; REQ-024 rule). */
internal fun PlayTexts.counterOf(number: Int, count: Int): String = counterFormat.format(java.util.Locale.ROOT, number, count)

/** REQ-029 rule 2's boundary: one hour, in seconds. */
private const val SECONDS_PER_HOUR = 3600

/**
 * The one format for every time the game shows (S-48; REQ-029 rule 2; decisions.md 2026-10-20 "One format for every
 * time the game shows …"): the board's timer, the solved row's best time, and WO-007's settings rows.
 * - Below one hour: minutes and two-digit seconds, "m:ss", built by hand from ASCII digits and a colon.
 * - From one hour: [PlayTexts.hoursMinutes] with the whole hours and the minutes left over (the seconds are dropped),
 *   its digits formatted under `Locale.ROOT`, so they stay ASCII under every device language (decisions.md 2026-10-19
 *   "The counter keeps ASCII digits under every device language").
 *
 * Public since WO-007 (S-11Δ4; it was internal until then, WO-008 design review F4): the settings screen's play times and
 * best times use it.
 */
fun PlayTexts.timeOf(seconds: Int): String {
    if (seconds < SECONDS_PER_HOUR) {
        return buildString {
            append(seconds / 60)
            append(':')
            append((seconds % 60).toString().padStart(2, '0'))
        }
    }
    return hoursMinutes.format(Locale.ROOT, seconds / SECONDS_PER_HOUR, seconds % SECONDS_PER_HOUR / 60)
}
