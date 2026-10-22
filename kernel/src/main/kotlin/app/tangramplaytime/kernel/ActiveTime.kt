// WO-008 TASK-044 · designs/WO-008-design.md §2 rows S-44 `IActiveTime`, S-45 `ActiveTime`; §3 "Must-nevers",
// "Backwards readings (F5)", "One thread" · REQ-029 (A1), REQ-005 (A2: the play times' source), the seconds REQ-030
// credits; TYPE-005; guardrails G-12, G-14; decisions.md 2026-10-20 "TYPE-005 as code.", "Today's play time follows
// the date `Clock.today()` reports.", "No time the store's reader rejects is ever written …" and its F5 amendment.
// WO-007 TASK-053 · designs/WO-007-design.md §2 row S-45Δ (`resetPlayTimes`) · REQ-034 (the play times' erasure);
// decisions.md 2026-10-21 "Reset all progress, in order, and where the player is afterwards." step (2).
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.Clock
import app.tangramplaytime.contracts.IActiveTime
import app.tangramplaytime.contracts.PlayTimes
import java.time.LocalDate

/**
 * TYPE-005 accounting and the play times (REQ-029, REQ-005.A2), and their erasure by Reset all
 * progress (REQ-034), behind [IActiveTime] (O-04).
 *
 * A moment is active when the game is visible and the last touch was at most [IDLE_LIMIT_MS] ago
 * (TYPE-005). Every member first accrues the interval since the previous reading of [clock]
 * (S-45 "Accrual"); active milliseconds pile up in one remainder, and [count] takes the whole
 * seconds out of it (decisions.md 2026-10-20 "TYPE-005 as code.").
 *
 * It starts not visible, with no touch (S-45): in a new process nothing counts before the first
 * touch. Time comes only from the injected [clock] (G-14); no `android.*` (G-12). One caller
 * thread, so no locks (design §3 "One thread").
 *
 * Must never (S-44; WO-008 must-do 2): produce a negative time — a reading below the previous one
 * counts nothing, and every count adds a whole number ≥ 0; count a second while not visible or
 * more than 60 s after the last touch, an idle player staying idle across a backwards reading
 * (design §3 "Backwards readings (F5)").
 *
 * @param clock the one source of time (ADR-006).
 * @param saved the play times read from the store at start-up (REQ-029; [PlayTimes.NONE] on a
 *   fresh install).
 */
class ActiveTime(private val clock: Clock, saved: PlayTimes) : IActiveTime {

    /** The play times as of the last count that added a second (S-45 `count`). */
    private var times: PlayTimes = saved

    /** The previous [Clock.nowMillis] reading: the start of the next accrued interval (last, now]. */
    private var last: Long = clock.nowMillis()

    /** The reading of the last touch, or null when there has been none (S-45: "no touch"). */
    private var lastTouch: Long? = null

    /** Whether the game is visible on screen (TYPE-005); false at the start (S-45). */
    private var shown: Boolean = false

    /** Active milliseconds not yet counted as whole seconds (S-45: "pending keeps the rest"). */
    private var pendingMs: Long = 0L

    override fun touched() {
        accrue()
        lastTouch = last
    }

    override fun visible(shown: Boolean) {
        accrue()
        this.shown = shown
    }

    /**
     * S-45 `count`: n = pending / 1000, and pending keeps the rest. When n > 0, the play times roll
     * to `PlayTimes(0, today, all)` if their date is not [Clock.today], then n is added to today and
     * to all-time (decisions.md 2026-10-20 "Today's play time follows the date `Clock.today()`
     * reports."). A count of 0 leaves the play times as they are.
     */
    override fun count(): Int {
        accrue()
        val n = (pendingMs / MS_PER_SECOND).toInt()
        pendingMs -= n * MS_PER_SECOND
        if (n > 0) {
            val today = clock.today()
            val base = if (times.todayDate == today) times else PlayTimes(0, today, times.allTimeSeconds)
            times = PlayTimes(base.todaySeconds + n, today, base.allTimeSeconds + n)
        }
        return n
    }

    /**
     * S-45 `playTimes`: today reads 0 on another date. On a date other than the stored one, the
     * result is `PlayTimes(0, today, all)` — what the next count's roll starts from; with no
     * stored date (no second counted yet), the date stays null (the [PlayTimes] KDoc).
     */
    override fun playTimes(): PlayTimes {
        accrue()
        val today: LocalDate = clock.today()
        val stored = times
        return when (stored.todayDate) {
            today -> stored
            null -> stored.copy(todaySeconds = 0)
            else -> PlayTimes(0, today, stored.allTimeSeconds)
        }
    }

    /**
     * S-45Δ `resetPlayTimes` (REQ-034 statement; decisions.md 2026-10-21 "Reset all progress, in
     * order, …" step (2)): first accrues, so the interval up to now is settled the usual way (a
     * backwards reading shifts the last touch, F5) and [last] becomes the reading; then the
     * remainder not yet counted is dropped and the play times are [PlayTimes.NONE]. [shown] and
     * [lastTouch] are kept, so the TYPE-005 rule runs on as before the erasure.
     */
    override fun resetPlayTimes() {
        accrue()
        pendingMs = 0L
        times = PlayTimes.NONE
    }

    /**
     * S-45 "Accrual": adds `max(0, min(now, lastTouch + 60 000) − last)` to the remainder when
     * visible and touched. A reading below [last] (F5) counts nothing and shifts the last touch back
     * by the jump, keeping the time since the touch (no touch stays none), so an idle player stays
     * idle. Either way [last] becomes the reading.
     */
    private fun accrue() {
        val now = clock.nowMillis()
        val touch = lastTouch
        if (now < last) {
            if (touch != null) lastTouch = touch - (last - now)
        } else if (shown && touch != null) {
            pendingMs += maxOf(0L, minOf(now, touch + IDLE_LIMIT_MS) - last)
        }
        last = now
    }

    /** S-45 constants. */
    companion object {
        /** TYPE-005's idle limit: a touch keeps the player active for at most 60 s (ASSUMPTION in TYPE-005). */
        const val IDLE_LIMIT_MS: Long = 60_000L

        private const val MS_PER_SECOND: Long = 1_000L
    }
}
