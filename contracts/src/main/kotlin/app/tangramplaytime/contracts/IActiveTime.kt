// ─────────────────────────────────────────────────────────────────────────────
// GOVERNED INTERFACE — SWDev Contract (see architecture.md §4)
// Tier: notify. Changes ONLY by common agreement, recorded like a REQ change.
// Serves: REQ-005 (A2), REQ-029; supplies the seconds REQ-030 credits; TYPE-005
// ─────────────────────────────────────────────────────────────────────────────
package app.tangramplaytime.contracts

/**
 * TYPE-005 accounting and the player's play times, today's and all-time (REQ-029, REQ-005),
 * implemented once in `:kernel` (O-04, G-12). `:app` reports the window's touches and its
 * visibility; a puzzle's own time and its best time are not kept here — the caller folds each
 * [count] into the open puzzle (O-04, REQ-030).
 *
 * The rule (TYPE-005): a second counts as active only while the game is visible on screen and
 * the player's last touch was at most 60 s ago. Time is read only from the injected [Clock]
 * (G-14).
 *
 * One caller thread: every member is called on the same thread; the implementation holds no
 * lock.
 *
 * Must never: return a negative time; count a second while not visible, or while the player
 * has been idle for more than 60 s (TYPE-005; REQ-029.A1: 5 untouched minutes add at most
 * 60 s).
 */
interface IActiveTime {
    /**
     * The player touched the game's window: the "last touch" of the TYPE-005 rule (any touch,
     * on any screen of the game).
     */
    fun touched()

    /**
     * The game became visible on screen ([shown] true) or left the screen ([shown] false): the
     * "visible on screen" of the TYPE-005 rule. It starts not visible.
     */
    fun visible(shown: Boolean)

    /**
     * The whole active seconds (TYPE-005) completed since the last count, ≥ 0, each added to
     * today's and to the all-time play time (REQ-029 statement). These are also the seconds a
     * caller credits to a puzzle in progress (REQ-030). A part of a second is kept for a later
     * count.
     */
    fun count(): Int

    /**
     * Today's and all-time active play time (REQ-029; REQ-005.A2: available at any time). Today's
     * time restarts at local midnight (REQ-029 rule 1): on a date [Clock.today] reports other
     * than the stored one, today reads 0 until a second counts on it.
     */
    fun playTimes(): PlayTimes
}
