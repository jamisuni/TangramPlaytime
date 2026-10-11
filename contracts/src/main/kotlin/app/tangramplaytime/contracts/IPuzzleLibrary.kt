// ─────────────────────────────────────────────────────────────────────────────
// GOVERNED INTERFACE — SWDev Contract (see architecture.md §4)
// Tier: locked. Changes ONLY by common agreement, recorded like a REQ change.
// Serves: REQ-007, REQ-011, REQ-019, REQ-022, REQ-023, REQ-026, REQ-038,
//         REQ-039, REQ-040, REQ-041, REQ-042, REQ-045, REQ-046, REQ-047, REQ-050
// ─────────────────────────────────────────────────────────────────────────────
package app.tangramplaytime.contracts

/**
 * The game's puzzle library (REQ-007), read from the `tangram-puzzle/1` files that ship
 * with the app (the JSON files of `Tangrams/`, DI-5; parsed and checked in `:content` only, O-02).
 *
 * Every listed puzzle is valid (REQ-038, REQ-007.A1): all seven pieces exactly once (1–6
 * distinct pieces for a mini puzzle, REQ-045), no overlaps, pieces connected through
 * shared edges, a rating 1..5 (REQ-040), a picture (REQ-039) and an `en` and `fi` title
 * (REQ-047). Each puzzle's solution is one valid arrangement, never the only accepted
 * answer (REQ-022); it supplies the silhouette and its outline corners (REQ-019).
 *
 * Must never: list a puzzle that fails the validity rules; expose file paths, JSON or
 * any parsing type across this seam; change while the app runs (the library is fixed for
 * the process's lifetime).
 */
interface IPuzzleLibrary {
    /**
     * Every puzzle, in ‹ › order (REQ-040): by [PuzzleKind] — mini first (REQ-045), then
     * warm-up (REQ-041), then full — then by [PuzzleDefinition.rating] ascending, then by
     * [PuzzleDefinition.id] within a rating. A full puzzle of rating 1 comes after every
     * warm-up (REQ-040.A2); the first entry on a fresh install is a mini puzzle of rating 1
     * whose tray holds three pieces (REQ-040.A1, REQ-045.A1). The grid overview lists the
     * same order (REQ-050 rule).
     *
     * Edge: ids are unique; the list is never empty in a shipped build (REQ-007,
     * REQ-045: at least one mini puzzle).
     */
    val puzzles: List<PuzzleDefinition>

    /**
     * The puzzle with [id], or null when no listed puzzle has that id — for example a
     * saved last-opened id or saved progress for a puzzle removed in an update (REQ-025,
     * REQ-003). A null is an expected outcome, never an exception (G-15).
     *
     * @param id the puzzle's file id.
     * @return the puzzle, or null.
     */
    fun byId(id: String): PuzzleDefinition?
}
