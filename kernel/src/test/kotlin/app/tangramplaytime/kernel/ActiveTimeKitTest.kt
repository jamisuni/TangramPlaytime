// Acceptance kit (WO-008, TASK-045) — TYPE-005 as code, on the JVM: the real `ActiveTime` (designs/WO-008-design.md §2
// S-45) seen through `IActiveTime` (S-44), over a kit `Clock` that moves only when the test moves it.
//
// - Token: REQ-029 A1, on the one test whose assertion is that line's meaning (traceability-rules.md §2: "A token is a
//   coverage claim"). Every other test is a pin of S-45 and carries no token: it cites a seam row, a TYPE rule or a
//   decisions.md row by date and bold title (the brief's labels: D3 "TYPE-005 as code.", D5 "Today's play time follows the
//   date `Clock.today()` reports.", D6 "No time the store's reader rejects is ever written …" with D6a, its amendment
//   "… a backwards clock reading shifts the last touch by the jump …").
// - The seams. S-44: `touched()`, `visible(shown)`, `count(): Int` ("whole active seconds since the last count"),
//   `playTimes(): PlayTimes`; "One caller thread"; "Must never: return a negative time; count while not visible or idle
//   more than 60 s". S-45: `ActiveTime(clock: Clock, saved: PlayTimes)`; "It starts not visible, with no touch"; "each
//   member first accrues (last, now], max(0, min(now, lastTouch + 60 000) − last) when visible and touched"; a reading
//   below the last "counts nothing and shifts lastTouch -= last − now (no touch stays none), then sets last = now";
//   `count`: "n = pending / 1000, and pending keeps the rest. If the stored date ≠ clock.today(), it rolls PlayTimes(0,
//   today, all), then adds n to today and to all-time"; `playTimes`: "today reads 0 on another date".
// - The rule. TYPE-005: "A second counts as active only while the game is visible on screen and the player's last touch
//   was at most 60 s ago" (ASSUMPTION: the 60 s idle limit). The kit writes 60 s as the rule states it, never through
//   the seam's constant.
// - Time (ADR-006; G-14 "Tests set the clock"; task-decomposition.md "Time is injected, never slept"): the kit's clock is
//   a plain value the test moves; nothing sleeps. Every call and every clock move runs on the test's one thread (S-44).
//   The kit moves the clock backwards on purpose: the app kit does so when it resets `AppClock.override` inside one
//   process (reviews/WO-008-design-review.md F5).
// - `:kernel` is a pure Kotlin/JVM module (G-12): nothing here touches Android; nothing reads a file. Helper names carry a
//   `kit` / `Kit` / `KIT_` prefix and are members of this class.
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.Clock
import app.tangramplaytime.contracts.IActiveTime
import app.tangramplaytime.contracts.PlayTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ActiveTimeKitTest {

    /**
     * The kit's [Clock]: a millisecond reading and a local date that change only when the test changes them (ADR-006;
     * the Clock KDoc: "Only differences between two readings are meaningful; the zero point is unspecified").
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

        /** A local date (any fixed one), and its neighbours. */
        val KIT_DAY: LocalDate = LocalDate.of(2026, 10, 20)
        val KIT_NEXT_DAY: LocalDate = KIT_DAY.plusDays(1)
        val KIT_DAY_BEFORE: LocalDate = KIT_DAY.minusDays(1)
    }

    private fun kitClock(date: LocalDate = KIT_DAY) = KitClock(KIT_START_MS, date)

    /** A new `ActiveTime` over [clock] and the [saved] play times (S-45), used only through S-44's members. */
    private fun kitTime(clock: KitClock, saved: PlayTimes = PlayTimes.NONE): IActiveTime = ActiveTime(clock, saved)

    /** S-44 "Must never: return a negative time": every field of [times] is ≥ 0. */
    private fun kitAssertNotNegative(times: PlayTimes, what: String) {
        assertTrue("$what: today's play time is never negative (S-44): $times", times.todaySeconds >= 0)
        assertTrue("$what: the all-time play time is never negative (S-44): $times", times.allTimeSeconds >= 0)
    }

    // ── REQ-029 A1 ────────────────────────────────────────────────────────────────────────────────────

    // REQ-029 A1 — "Leaving the phone untouched for 5 minutes adds at most 60 s." TYPE-005: a second counts only while the
    // game is visible "and the player's last touch was at most 60 s ago". S-45 accrual: "min(now, lastTouch + 60 000)";
    // design §3 "Idle adds at most 60 s". "Adds" is read on the play times: REQ-029 statement ("add it to today's and to
    // the all-time play time"), S-45 `count` ("adds n to today and to all-time"); on a fresh install the first count rolls
    // the date in (S-45: "If the stored date ≠ clock.today(), it rolls PlayTimes(0, today, all)").
    // - Positive control first, in the same test (the brief; tasks.md § WO-008 seam note 3): a touch, then 30 s, counts
    //   exactly 30.
    // - Then a touch and 5 untouched minutes count exactly 60: an exact 60 fails a count of 0 as well as one above 60.
    // - The adversarial path of "at most": the same 5 minutes folded by a ticker (S-15Δ3 counts every 250 ms while visible;
    //   here every 10 s). A count is no touch (S-44 names `touched` as the touch report), so the sum is again exactly 60.
    // - Five more untouched minutes add nothing.
    @Test
    fun leavingThePhoneUntouchedForFiveMinutesAddsAtMostSixtySeconds() { // REQ-029.A1
        val clock = kitClock()
        val t = kitTime(clock)
        t.visible(true)

        t.touched()
        clock.forward(30)
        assertEquals("positive control: a touch, then 30 s, counts 30 (TYPE-005)", 30, t.count())
        assertEquals("the 30 s are today's and all-time play time (REQ-029 statement; S-45 count)", PlayTimes(30, KIT_DAY, 30), t.playTimes())

        t.touched()
        clock.forward(5 * 60)
        assertEquals("a touch, then 5 untouched minutes: exactly 60 s count (TYPE-005; design §3)", KIT_IDLE_LIMIT_S, t.count())
        assertEquals("5 untouched minutes add 60 s to the play times", PlayTimes(90, KIT_DAY, 90), t.playTimes())

        t.touched()
        var ticked = 0
        repeat(30) {
            clock.forward(10)
            val n = t.count()
            assertTrue("a count is never negative (S-44): $n", n >= 0)
            ticked += n
        }
        assertEquals("the same 5 untouched minutes counted every 10 s: exactly 60 s (a count is no touch)", KIT_IDLE_LIMIT_S, ticked)
        assertEquals("…added once to the play times", PlayTimes(150, KIT_DAY, 150), t.playTimes())

        clock.forward(5 * 60)
        assertEquals("5 more untouched minutes add nothing (TYPE-005)", 0, t.count())
        assertEquals("…the play times as they were", PlayTimes(150, KIT_DAY, 150), t.playTimes())
    }

    // ── Pins of S-45 ──────────────────────────────────────────────────────────────────────────────────

    // Pin (no token): WO-008 must-do 2; D6 ("ActiveTime counts only forward: a nowMillis() reading below the last one counts
    // nothing …; every count adds a whole number ≥ 0") with D6a ("On now < last, ActiveTime counts nothing, sets lastTouch -=
    // (last − now) (no touch stays no touch) and last = now, so the time since the last touch is kept"); S-45 "Backwards
    // reading (F5)"; S-44 "Must never: return a negative time"; D5 "All-time never decreases".
    // - A touch, 20 s, then the clock 5 minutes back: that count is 0. The time since the touch (20 s) is kept, so 40 s of
    //   activity remain: 30 s later 30 count, 30 s after that 10. Re-basing the touch to the reading would give 30 + 30, not
    //   shifting it would give 30 + 30 too, and waiting for the clock to pass the old reading would give 0 + 0.
    // - A touch, 10 s, then the clock a minute back before any member call: the next count sees a reading below the last and
    //   counts nothing (not −50). 20 s later, 20 count (positive control).
    // - After each count, every field of the play times is ≥ 0 and all-time has not decreased.
    @Test
    fun aBackwardsReadingCountsNothingNeverNegativeAndKeepsTheTimeSinceTheLastTouch() {
        val clock = kitClock()
        val t = kitTime(clock)
        t.visible(true)
        var allTime = 0
        fun kitCount(expected: Int, what: String) {
            val n = t.count()
            assertTrue("$what: a count is never negative (S-44): $n", n >= 0)
            assertEquals(what, expected, n)
            val times = t.playTimes()
            kitAssertNotNegative(times, what)
            assertTrue("$what: all-time never decreases (D5): ${times.allTimeSeconds} after $allTime", times.allTimeSeconds >= allTime)
            allTime = times.allTimeSeconds
        }

        t.touched()
        clock.forward(20)
        kitCount(20, "a touch, then 20 s")
        clock.back(5 * 60)
        kitCount(0, "the clock 5 minutes back: a reading below the last counts nothing (D6)")
        clock.forward(30)
        kitCount(30, "30 s after the jump: the touch is 50 s old, so all 30 count (D6a: the time since the last touch is kept)")
        clock.forward(30)
        kitCount(10, "30 s more: the touch reaches 60 s after 10 of them (D6a; TYPE-005)")
        assertEquals("the play times after the first jump", PlayTimes(60, KIT_DAY, 60), t.playTimes())

        t.touched()
        clock.forward(10)
        clock.back(60)
        kitCount(0, "a touch, 10 s, the clock a minute back, then a count: nothing, never −50 (D6; S-44)")
        clock.forward(20)
        kitCount(20, "positive control: 20 s after the jump count 20")
        assertEquals("the play times after the second jump", PlayTimes(80, KIT_DAY, 80), t.playTimes())
    }

    // Pin (no token): reviews/WO-008-design-review.md F5 ("Re-basing lastTouch to now would count up to 60 s with no
    // touch"; the kernel kit "asserts that an idle player stays idle across the jump"); D6a ("An idle player stays idle
    // across the jump"); S-45 "(no touch stays none)", "Idle stays idle"; TYPE-005.
    // - Never touched: a backwards reading and 30 s count nothing.
    // - A touch, then 90 s: 60 count (TYPE-005). The clock 5 minutes back, then 30 s and 30 s: nothing, the player having
    //   been idle for 90 s before the jump.
    // - Positive control: a touch, then 15 s: 15.
    @Test
    fun anIdlePlayerStaysIdleAcrossABackwardsReading() {
        val clock = kitClock()
        val t = kitTime(clock)
        t.visible(true)

        clock.back(5 * 60)
        assertEquals("never touched, a backwards reading: nothing (no touch stays none)", 0, t.count())
        clock.forward(30)
        assertEquals("never touched, 30 s later: nothing", 0, t.count())

        t.touched()
        clock.forward(90)
        assertEquals("fixture: a touch, then 90 s: 60 count (TYPE-005)", KIT_IDLE_LIMIT_S, t.count())
        clock.back(5 * 60)
        assertEquals("idle, then the clock 5 minutes back: nothing", 0, t.count())
        clock.forward(30)
        assertEquals("idle across the jump, 30 s later: nothing (F5: re-basing the touch would count 30)", 0, t.count())
        clock.forward(30)
        assertEquals("idle across the jump, 60 s later: nothing", 0, t.count())

        t.touched()
        clock.forward(15)
        assertEquals("positive control: a touch, then 15 s, counts 15", 15, t.count())
        assertEquals("the play times: 60 + 15", PlayTimes(75, KIT_DAY, 75), t.playTimes())
    }

    // Pin (no token): D3 ("Whole seconds: active milliseconds pile up in one in-memory remainder, and a second counts when it
    // completes"); S-45 `count` ("n = pending / 1000, and pending keeps the rest"); S-44 `count` ("whole active seconds since
    // the last count"). Within a minute of one touch: 999 ms count 0 (no rounding up), 1 ms more completes the second; 1500
    // ms count 1 and keep 500, which 700 ms make 1200 (1, keeping 200), which 800 ms make 1000 (1); 500 ms count 0, 500 ms
    // more count 1. 5000 ms in all are exactly 5 s of play time: a remainder dropped at each count would give fewer.
    @Test
    fun wholeSecondsCountAndTheRemainderIsKept() {
        val clock = kitClock()
        val t = kitTime(clock)
        t.visible(true)
        t.touched()
        for ((ms, expected) in listOf(999L to 0, 1L to 1, 1500L to 1, 700L to 1, 800L to 1, 500L to 0, 500L to 1)) {
            clock.forwardMs(ms)
            assertEquals("$ms ms more: $expected whole second(s) (D3; S-45 count)", expected, t.count())
        }
        assertEquals("5000 ms of activity are 5 s of play time", PlayTimes(5, KIT_DAY, 5), t.playTimes())
    }

    // Pin (no token): TYPE-005 ("only while the game is visible on screen"); S-44 "Must never: … count while not visible";
    // S-45 "It starts not visible, with no touch" and "each member first accrues"; D3 ("Visible on screen" = between
    // `onStart` and `onStop`, reported through `visible`).
    // - A new ActiveTime, touched, 30 s: nothing (it starts not visible).
    // - Visible, 20 s more: 20 (the touch is 50 s old). A time that started visible would count 50.
    // - A touch, 10 s, hidden, 20 s: 10 (`visible(false)` accrues the visible 10 first; the hidden 20 count nothing).
    // - Hidden for 5 minutes; visible, a touch, 15 s: 15 (positive control).
    @Test
    fun nothingCountsWhileNotVisible() {
        val clock = kitClock()
        val t = kitTime(clock)

        t.touched()
        clock.forward(30)
        assertEquals("a new ActiveTime is not visible: a touch and 30 s count nothing (S-45)", 0, t.count())
        t.visible(true)
        clock.forward(20)
        assertEquals("visible from now: only the 20 visible seconds count", 20, t.count())

        t.touched()
        clock.forward(10)
        t.visible(false)
        clock.forward(20)
        assertEquals("10 visible seconds, then 20 hidden: 10 (the hidden ones never count)", 10, t.count())

        clock.forward(5 * 60)
        assertEquals("hidden for 5 minutes: nothing", 0, t.count())
        t.visible(true)
        t.touched()
        clock.forward(15)
        assertEquals("positive control: visible again, a touch and 15 s: 15", 15, t.count())
        assertEquals("the play times: 20 + 10 + 15", PlayTimes(45, KIT_DAY, 45), t.playTimes())
    }

    // Pin (no token): D5 ("Each count adds its whole seconds to the date the clock reports at that count. When that date
    // differs from the stored one — midnight, … — today restarts at 0 for the new date. All-time never decreases. The seconds
    // of a count that spans midnight go to the new day"); REQ-029 rule "Today's time restarts at local midnight"; S-45
    // `count`. Saved (100 s on the day, 500 s all-time): 20 s on the day go to it; 20 s across midnight, counted once the
    // date is the next day, go to the next day, which starts at 0; all-time grows by both.
    @Test
    fun aCountThatSpansMidnightGoesToTheNewDayAndAllTimeNeverDecreases() {
        val clock = kitClock(KIT_DAY)
        val t = kitTime(clock, PlayTimes(100, KIT_DAY, 500))
        t.visible(true)
        t.touched()
        clock.forward(20)
        assertEquals("20 s on the day", 20, t.count())
        assertEquals("they are the day's (D5)", PlayTimes(120, KIT_DAY, 520), t.playTimes())

        t.touched()
        clock.forward(20)
        clock.date = KIT_NEXT_DAY
        assertEquals("20 s across midnight", 20, t.count())
        val after = t.playTimes()
        assertEquals("they go to the new day, which restarts at 0; all-time grows (D5; REQ-029 rule)", PlayTimes(20, KIT_NEXT_DAY, 540), after)
        assertTrue("all-time never decreases (D5)", after.allTimeSeconds >= 520)
    }

    // Pin (no token): D5 ("When that date differs from the stored one — … a date set back or forward … — today restarts at 0
    // for the new date (`PlayTimes` keeps one date). All-time never decreases"); S-45 `count`. Saved (100 s on the day, 500 s
    // all-time); the device's date set back a day; 15 s counted: today is those 15 s on the earlier date, and all-time is
    // 515, not less.
    @Test
    fun aDateSetBackRestartsToday() {
        val clock = kitClock(KIT_DAY)
        val t = kitTime(clock, PlayTimes(100, KIT_DAY, 500))
        t.visible(true)
        clock.date = KIT_DAY_BEFORE
        t.touched()
        clock.forward(15)
        assertEquals("15 s on the date set back", 15, t.count())
        val after = t.playTimes()
        assertEquals("today restarts at 0 for the date set back; all-time grows (D5)", PlayTimes(15, KIT_DAY_BEFORE, 515), after)
        assertTrue("all-time never decreases (D5)", after.allTimeSeconds >= 500)
    }

    // Pin (no token): S-45 `playTimes` ("today reads 0 on another date"); the PlayTimes KDoc ("when Clock.today differs from
    // this date, today's time is zero"); D5 ("A read on a new day reports today as 0 before any second counts"). Saved (100 s
    // on the day, 500 s all-time). Positive control: read on the same day, today is 100. Read once the clock's date has
    // moved on, before any count, and from an ActiveTime built on the next day: today 0, all-time 500. Only the two counts
    // are asserted; the date a 0 belongs to is not named by any seam.
    @Test
    fun playTimesReadsTodayAsZeroOnAnotherDateBeforeAnyCount() {
        val clock = kitClock(KIT_DAY)
        val t = kitTime(clock, PlayTimes(100, KIT_DAY, 500))
        assertEquals("positive control: on the saved day, today is the saved 100", 100, t.playTimes().todaySeconds)
        clock.date = KIT_NEXT_DAY
        assertEquals("the next day, before any count: today reads 0 (S-45)", 0, t.playTimes().todaySeconds)
        assertEquals("…all-time as saved", 500, t.playTimes().allTimeSeconds)

        val later = kitTime(kitClock(KIT_NEXT_DAY), PlayTimes(100, KIT_DAY, 500))
        assertEquals("built on the next day: today reads 0", 0, later.playTimes().todaySeconds)
        assertEquals("…all-time as saved", 500, later.playTimes().allTimeSeconds)
    }

    // Pin (no token): D3 ("In a new process nothing counts before the first touch on the game (the launcher's tap is another
    // window)"); S-45 ("It starts not visible, with no touch"). A new ActiveTime over saved times (100 s today, 500 s
    // all-time): visible for 5 minutes with no touch, nothing counts and the times are as saved. Positive control: a touch
    // and 10 s count 10 on top of the saved times.
    @Test
    fun aNewActiveTimeOverSavedTimesCountsNothingBeforeItsFirstTouch() {
        val clock = kitClock(KIT_DAY)
        val t = kitTime(clock, PlayTimes(100, KIT_DAY, 500))
        t.visible(true)
        clock.forward(5 * 60)
        assertEquals("visible, no touch yet, 5 minutes: nothing", 0, t.count())
        assertEquals("the saved times, unchanged", PlayTimes(100, KIT_DAY, 500), t.playTimes())

        t.touched()
        clock.forward(10)
        assertEquals("positive control: the first touch, then 10 s: 10", 10, t.count())
        assertEquals("…on top of the saved times", PlayTimes(110, KIT_DAY, 510), t.playTimes())
    }
}
