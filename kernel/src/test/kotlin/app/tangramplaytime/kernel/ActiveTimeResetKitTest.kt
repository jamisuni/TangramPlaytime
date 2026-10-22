// Acceptance kit (WO-007, TASK-052; kit stage 1) — the erasure of the play times by Reset all progress, on the JVM: the
// real `ActiveTime` (designs/WO-007-design.md §2 S-45Δ) seen through `IActiveTime` (S-44Δ, the WO's notify delta), over a
// kit `Clock` that moves only when the test moves it.
//
// - No token (design §2 "Tokens": REQ-034's lines are screen lines, `app/`'s device kit's). Every test is a pin of S-44Δ /
//   S-45Δ; prose names a line without the dot (traceability-rules.md §2: "A token is a coverage claim").
// - The seams.
//   - S-44Δ: `fun resetPlayTimes()`, with the design's §1 KDoc: "Erases today's and the all-time play time (REQ-034
//     statement). Afterwards `playTimes` reads `PlayTimes.NONE` until a second counts; an uncounted part of a second is
//     dropped. Visibility and the last touch (TYPE-005) are kept. The confirmation is the caller's (REQ-034 rule 1, A2)."
//   - S-45Δ: "`accrue()`; `pendingMs = 0`; `times = PlayTimes.NONE`. `shown`, `lastTouch`, `last` kept".
//   - S-44 (unchanged): "Must never: return a negative time; count a second while not visible, or while the player has been
//     idle for more than 60 s"; "One caller thread".
//   - S-45 (WO-008, unchanged): `ActiveTime(clock: Clock, saved: PlayTimes)`; "It starts not visible, with no touch";
//     `count`: "n = pending / 1000, and pending keeps the rest. If the stored date ≠ clock.today(), it rolls PlayTimes(0,
//     today, all), then adds n to today and to all-time". So the first second counted after the erasure (NONE's date is
//     null) gives PlayTimes(n, today, n).
//   - decisions.md 2026-10-21 "Reset all progress, in order, and where the player is afterwards." (E6), step (2): "The
//     kernel's play times are erased by `IActiveTime.resetPlayTimes()` … The uncounted part of a second is dropped;
//     visibility and the last touch are kept."
// - The rule. TYPE-005: "A second counts as active only while the game is visible on screen and the player's last touch was
//   at most 60 s ago" (ASSUMPTION: the 60 s idle limit). The kit writes 60 s as the rule states it.
// - Time (ADR-006; G-14 "Tests set the clock"; task-decomposition.md "Time is injected, never slept"): the kit's clock is a
//   plain value the test moves; nothing sleeps. Every call and every clock move runs on the test's one thread (S-44).
// - Every "nothing" line has its positive control in the same test. Fixtures discriminate: each expected count is exact,
//   so a kept remainder, a cleared or a re-based touch, or a reset visibility each give another number.
// - `:kernel` is a pure Kotlin/JVM module (G-12): nothing here touches Android or reads a file. Helper names carry a `kit` /
//   `Kit` / `KIT_` prefix and are members of this class (ActiveTimeKitTest.kt's clock pattern, read, not shared: it is
//   private there).
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.Clock
import app.tangramplaytime.contracts.IActiveTime
import app.tangramplaytime.contracts.PlayTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ActiveTimeResetKitTest {

    /**
     * The kit's [Clock]: a millisecond reading and a local date that change only when the test changes them (ADR-006; the
     * Clock KDoc: "Only differences between two readings are meaningful; the zero point is unspecified").
     */
    private class KitClock(var millis: Long, var date: LocalDate) : Clock {
        override fun nowMillis(): Long = millis

        override fun today(): LocalDate = date

        fun forward(seconds: Int) {
            millis += seconds * 1000L
        }

        fun forwardMs(ms: Long) {
            millis += ms
        }

        fun back(seconds: Int) {
            millis -= seconds * 1000L
        }
    }

    private companion object {
        /** Where the kit's clock starts: far from zero, so it can be sent back minutes without a negative reading. */
        const val KIT_START_MS = 50_000_000L

        /** TYPE-005's idle limit as the rule states it ("at most 60 s ago"). */
        const val KIT_IDLE_LIMIT_S = 60

        /** A local date (any fixed one). */
        val KIT_DAY: LocalDate = LocalDate.of(2026, 10, 21)

        /** Saved play times that differ from NONE in every field (S-45 `ActiveTime(clock, saved)`). */
        val KIT_SAVED: PlayTimes = PlayTimes(100, KIT_DAY, 500)
    }

    private fun kitClock() = KitClock(KIT_START_MS, KIT_DAY)

    /** A new `ActiveTime` over [clock] and the [saved] play times (S-45), used only through S-44Δ's members. */
    private fun kitTime(clock: KitClock, saved: PlayTimes = KIT_SAVED): IActiveTime = ActiveTime(clock, saved)

    /** S-44 "Must never: return a negative time": every field of [times] is ≥ 0. */
    private fun kitAssertNotNegative(times: PlayTimes, what: String) {
        assertTrue("$what: today's play time is never negative (S-44): $times", times.todaySeconds >= 0)
        assertTrue("$what: the all-time play time is never negative (S-44): $times", times.allTimeSeconds >= 0)
    }

    /** One count that must be [expected] and never negative (S-44 `count`: "≥ 0"). */
    private fun kitCount(t: IActiveTime, expected: Int, what: String) {
        val n = t.count()
        assertTrue("$what: a count is never negative (S-44): $n", n >= 0)
        assertEquals(what, expected, n)
    }

    // Pin (no token): S-44Δ KDoc "Erases today's and the all-time play time (REQ-034 statement). Afterwards `playTimes` reads
    // `PlayTimes.NONE` until a second counts"; S-45Δ "`times = PlayTimes.NONE`"; REQ-034 statement ("erase all … play
    // times"); the PlayTimes KDoc ("NONE: No play time yet: a fresh install, or after a reset (REQ-034)").
    // - Positive control first: over the saved times (100 s today, 500 s all-time), a touch and 20 s count 20, and the play
    //   times read 120 / 520 — not NONE.
    // - The erasure: `playTimes()` is exactly NONE (today 0, no date, all-time 0).
    // - "until a second counts": a count with no clock move counts 0, and the times are still NONE.
    // - The first second counted afterwards: 10 s (the touch is 20 s old, inside the 60 s) give PlayTimes(10, today, 10)
    //   (S-45 `count` rolls the null date to today, then adds n): the saved 120 / 520 are gone, not resumed.
    @Test
    fun afterTheErasureThePlayTimesReadNoneUntilASecondCounts() {
        val clock = kitClock()
        val t = kitTime(clock)
        t.visible(true)
        t.touched()
        clock.forward(20)
        kitCount(t, 20, "positive control: a touch, then 20 s, counts 20 (TYPE-005)")
        assertEquals("positive control: the play times before the erasure are not NONE", PlayTimes(120, KIT_DAY, 520), t.playTimes())

        t.resetPlayTimes()
        assertEquals("after the erasure the play times read NONE (S-44Δ)", PlayTimes.NONE, t.playTimes())
        kitCount(t, 0, "a count right after the erasure, with no clock move: nothing")
        assertEquals("…the play times are still NONE until a second counts (S-44Δ)", PlayTimes.NONE, t.playTimes())

        clock.forward(10)
        kitCount(t, 10, "10 s after the erasure, the touch 30 s old: 10 count")
        assertEquals(
            "the first seconds after the erasure start from NONE: today and all-time are those 10 s (S-45 count)",
            PlayTimes(10, KIT_DAY, 10),
            t.playTimes(),
        )
    }

    // Pin (no token): S-44Δ KDoc "an uncounted part of a second is dropped"; S-45Δ "`accrue()`; `pendingMs = 0`"; E6 step
    // (2) "The uncounted part of a second is dropped"; S-45 `count` ("pending keeps the rest").
    // - Part of a second: a touch, 1400 ms: 1 counts and 400 ms wait. The erasure. 700 ms more count 0 (a kept 400 ms would
    //   make 1100 ms and count 1); 300 ms more complete the first second after the erasure: 1 (positive control). The times
    //   read PlayTimes(1, today, 1).
    // - Whole seconds not yet counted at the erasure (S-45Δ accrues first, then sets the pending time to 0): a touch, 5 s with
    //   no count, the erasure, then a count: 0, and NONE. Positive control: 2 s more count 2.
    @Test
    fun theErasureDropsWhatWasNotYetCounted() {
        val clock = kitClock()
        val t = kitTime(clock)
        t.visible(true)
        t.touched()
        clock.forwardMs(1400)
        kitCount(t, 1, "fixture: 1400 ms count 1 and keep 400 ms (S-45 count)")

        t.resetPlayTimes()
        clock.forwardMs(700)
        kitCount(t, 0, "700 ms after the erasure: 0 — the 400 ms waiting before it were dropped (S-44Δ)")
        clock.forwardMs(300)
        kitCount(t, 1, "positive control: 300 ms more complete the first second after the erasure")
        assertEquals("the play times: that one second", PlayTimes(1, KIT_DAY, 1), t.playTimes())

        val clock2 = kitClock()
        val u = kitTime(clock2)
        u.visible(true)
        u.touched()
        clock2.forward(5)
        u.resetPlayTimes()
        kitCount(u, 0, "5 s not yet counted at the erasure count nothing after it (S-45Δ: accrue, then the pending time is 0)")
        assertEquals("…and the play times read NONE", PlayTimes.NONE, u.playTimes())
        clock2.forward(2)
        kitCount(u, 2, "positive control: 2 s after the erasure count 2")
    }

    // Pin (no token): S-44Δ KDoc "Visibility and the last touch (TYPE-005) are kept"; S-45Δ "`shown` … kept"; TYPE-005 ("only
    // while the game is visible on screen"); S-45 "It starts not visible".
    // - Visible, then the erasure: a touch and 10 s count 10 (an erasure that set the game not visible would count 0).
    // - Never made visible, then the erasure: a touch and 10 s count 0 (an erasure that set it visible would count 10).
    //   Positive control: made visible, 10 s more count 10.
    // - Visible, then hidden, then the erasure: a touch and 10 s count 0. Positive control: visible again, 10 s count 10.
    @Test
    fun theErasureKeepsVisibility() {
        val c1 = kitClock()
        val shown = kitTime(c1)
        shown.visible(true)
        shown.resetPlayTimes()
        shown.touched()
        c1.forward(10)
        kitCount(shown, 10, "visible before the erasure, still visible after it: a touch and 10 s count 10")

        val c2 = kitClock()
        val never = kitTime(c2)
        never.resetPlayTimes()
        never.touched()
        c2.forward(10)
        kitCount(never, 0, "never visible before the erasure, still not visible after it: nothing counts")
        never.visible(true)
        c2.forward(10)
        kitCount(never, 10, "positive control: made visible, 10 s more count 10 (the touch is 20 s old)")

        val c3 = kitClock()
        val hidden = kitTime(c3)
        hidden.visible(true)
        hidden.visible(false)
        hidden.resetPlayTimes()
        hidden.touched()
        c3.forward(10)
        kitCount(hidden, 0, "hidden before the erasure, still hidden after it: nothing counts")
        hidden.visible(true)
        c3.forward(10)
        kitCount(hidden, 10, "positive control: visible again, 10 s more count 10")
    }

    // Pin (no token): S-44Δ KDoc "the last touch (TYPE-005) [is] kept"; S-45Δ "`lastTouch` … kept"; the brief ("the 60 s idle
    // limit still runs from the touch before the erasure"); TYPE-005 ("the player's last touch was at most 60 s ago").
    // - A touch, 20 s (20 count), the erasure, then 60 s with no touch: exactly 40 count — the limit runs from the touch 20 s
    //   before the erasure. A cleared touch would count 0; a touch re-based to the erasure would count 60.
    // - 30 s more with no touch: nothing (idle). Positive control: a touch and 10 s count 10.
    // - Never touched, then the erasure: 30 s count nothing ("no touch" stays none). Positive control: a touch and 5 s: 5.
    @Test
    fun theErasureKeepsTheLastTouch() {
        val clock = kitClock()
        val t = kitTime(clock)
        t.visible(true)
        t.touched()
        clock.forward(20)
        kitCount(t, 20, "fixture: a touch, then 20 s")

        t.resetPlayTimes()
        clock.forward(60)
        kitCount(t, KIT_IDLE_LIMIT_S - 20, "60 s after the erasure, no touch: the 60 s limit runs from the touch 20 s before it")
        clock.forward(30)
        kitCount(t, 0, "30 s more with no touch: idle, nothing (TYPE-005)")
        t.touched()
        clock.forward(10)
        kitCount(t, 10, "positive control: a touch and 10 s count 10")
        assertEquals("the play times after the erasure: 40 + 10", PlayTimes(50, KIT_DAY, 50), t.playTimes())

        val clock2 = kitClock()
        val u = kitTime(clock2)
        u.visible(true)
        u.resetPlayTimes()
        clock2.forward(30)
        kitCount(u, 0, "never touched, then the erasure: 30 s count nothing (S-45: no touch)")
        u.touched()
        clock2.forward(5)
        kitCount(u, 5, "positive control: a touch and 5 s count 5")
    }

    // Pin (no token): S-44 "Must never: return a negative time" (unchanged by S-44Δ); S-45Δ (the erasure accrues first);
    // decisions.md 2026-10-20 "No time the store's reader rejects is ever written …" with its amendment "… a backwards
    // clock reading shifts the last touch by the jump …" (D6, D6a: "On now < last, ActiveTime counts nothing, sets
    // lastTouch -= (last − now) … and last = now, so the time since the last touch is kept").
    // - Over the saved times: a touch, 10 s, then the clock 5 minutes back before any member call, then the erasure (its
    //   accrual sees the backwards reading): NONE, every field ≥ 0.
    // - A count right after: 0, never negative. 20 s later: 20 (positive control: the touch is kept and shifted with the jump,
    //   D6a, so these 20 s lie inside its 60 s), and the times are PlayTimes(20, today, 20).
    // - The clock a minute back again, a second erasure, a count: 0 and NONE, never negative.
    @Test
    fun theErasureNeverMakesATimeNegative() {
        val clock = kitClock()
        val t = kitTime(clock)
        t.visible(true)
        t.touched()
        clock.forward(10)
        clock.back(5 * 60)
        t.resetPlayTimes()
        val erased = t.playTimes()
        kitAssertNotNegative(erased, "the erasure after a backwards reading")
        assertEquals("the erasure after a backwards reading: NONE", PlayTimes.NONE, erased)

        kitCount(t, 0, "a count right after: nothing, never negative")
        clock.forward(20)
        kitCount(t, 20, "positive control: 20 s later 20 count (S-45Δ keeps the touch; D6a shifts it with the jump)")
        val after = t.playTimes()
        kitAssertNotNegative(after, "20 s after the erasure")
        assertEquals("the play times after the erasure", PlayTimes(20, KIT_DAY, 20), after)

        clock.back(60)
        t.resetPlayTimes()
        kitCount(t, 0, "the clock a minute back and a second erasure: a count is 0, never negative")
        val again = t.playTimes()
        kitAssertNotNegative(again, "the second erasure")
        assertEquals("the second erasure: NONE", PlayTimes.NONE, again)
    }
}
