// ─────────────────────────────────────────────────────────────────────────────
// GOVERNED INTERFACE — SWDev Contract (see architecture.md §4)
// Tier: notify. Changes ONLY by common agreement, recorded like a REQ change.
// Serves: REQ-003, REQ-025, REQ-026, REQ-029, REQ-030, REQ-031, REQ-033, REQ-034,
//         REQ-050; TYPE-006
// ─────────────────────────────────────────────────────────────────────────────
package app.tangramplaytime.contracts

/**
 * Everything that survives closing the app (REQ-025 rule; guardrail G-13: the only road
 * to persisted state), implemented in `:store` (O-03). Device-local only (REQ-010, G-01).
 *
 * Contract for every member:
 * - **Missing or unreadable stored state behaves as a fresh install**: every puzzle
 *   [PuzzleProgress.FRESH], [PlayTimes.NONE], default [Settings], no last-opened puzzle.
 *   It never fails the launch and never throws for it (G-15).
 * - A save is complete when the call returns: a later read — in this process or after a
 *   restart — returns what was saved (REQ-003.A2, REQ-025.A1).
 * - The store keeps what it is given; the rules that compute the values (TYPE-005
 *   accounting, TYPE-006 transitions, best-time keeping) are the kernel's (O-04).
 *
 * Must never: expose a file, preference, database or serialisation type across this
 * seam; lose one puzzle's progress when another is saved (REQ-003); be bypassed by any
 * other module touching storage (G-13).
 */
interface IProgressStore {
    /**
     * The saved progress of the puzzle [puzzleId] (REQ-003: each puzzle keeps its own
     * state; REQ-025: shown exactly as it was left).
     *
     * @return the saved progress, or [PuzzleProgress.FRESH] when none is saved or it is
     *   unreadable.
     */
    suspend fun progress(puzzleId: String): PuzzleProgress

    /**
     * Saves [progress] for [puzzleId], replacing what was saved for it and nothing else
     * (REQ-003, REQ-025, REQ-030). The caller passes an empty `placed` list for a New or
     * Solved puzzle (REQ-025: Restart returns all pieces to the tray) and keeps
     * `bestSeconds` on Restart and Retry (REQ-030 rule).
     */
    suspend fun saveProgress(puzzleId: String, progress: PuzzleProgress)

    /**
     * Every saved puzzle's progress, keyed by puzzle id — for the grid overview's states
     * (REQ-050), the next-unsolved search (REQ-050) and the best times in the settings list
     * (REQ-030 rule). A puzzle without an entry is [PuzzleProgress.FRESH]. Entries for ids
     * no longer in the library may be present; callers ignore them.
     */
    suspend fun allProgress(): Map<String, PuzzleProgress>

    /** Today's and all-time active play time (REQ-029). [PlayTimes.NONE] when none is saved. */
    suspend fun playTimes(): PlayTimes

    /** Saves the play times (REQ-029), replacing the saved ones. */
    suspend fun savePlayTimes(times: PlayTimes)

    /**
     * The player's settings: timer shown (REQ-031, default off) and sound on (REQ-033,
     * default on). The defaults of [Settings] when none are saved (REQ-031.A1).
     */
    suspend fun settings(): Settings

    /** Saves the settings (REQ-031, REQ-033), replacing the saved ones. */
    suspend fun saveSettings(settings: Settings)

    /**
     * The id of the puzzle that was open when the app was last left, or null when none is
     * saved — so the player returns to the same puzzle (REQ-025: shown exactly as it was
     * left; REQ-003). The id may name a puzzle no longer in the library; the caller then
     * falls back to the first puzzle.
     */
    suspend fun lastOpenedPuzzleId(): String?

    /** Saves [id] as the last opened puzzle (REQ-025, REQ-003). */
    suspend fun saveLastOpenedPuzzleId(id: String)

    /**
     * The confirmed Reset all progress (REQ-034): afterwards every puzzle reads
     * [PuzzleProgress.FRESH] — New, nothing placed, puzzle and best times erased
     * (REQ-034.A1) — and the play times read [PlayTimes.NONE]. Settings are kept (REQ-034
     * rule) and the last opened puzzle id is unchanged. The confirmation itself is the
     * caller's; this is called only after it (REQ-034.A2: cancelling changes nothing).
     */
    suspend fun resetAll()
}
