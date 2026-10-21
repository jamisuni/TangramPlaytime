// Acceptance kit (WO-008, TASK-050): active time in the real app — the touches through the window, the lifecycle and the
// stored setting (designs/WO-008-design.md §2 S-46 `AppClock`, S-15Δ3 the `:app` wiring, "Kit doors", `app/` device, as
// amended by the 2026-10-20 planner row P6 (REQ-031 A2 proven in `play/` only; no `app/` class writes the timer setting);
// reviews/WO-008-design-review.md F1, F5).
//
// - Tokens: REQ-029 A1 on the untouched test and REQ-031 A1 on the cleared-app test, extra tokens beside the code homes'
//   (`kernel/` ActiveTimeKitTest; `play/` TimerKitTest; tasks.md § WO-008 "Lines covered twice": the window's touches and the
//   real lifecycle are only here). The stopped test is a pin of TYPE-005 and carries no token.
// - Why the real app: `:app` reports the window's touches (`MainActivity.dispatchTouchEvent` → `AppGraph.touched()`) and its
//   visibility (`onStart` / `onStop` → `AppGraph.visible`), and ticks `countTime()` every 250 ms while visible (S-15Δ3). The
//   JVM and `play/` kits call the kernel themselves, so a missing or misplaced hook stays green there (review F1).
// - Touches go through the window only (seam note 3; review F1; design §3 library fact 7): KitApp `kitTouchThroughWindow`,
//   `Instrumentation.sendPointerSync` DOWN then UP at an inert point of the board's empty margin, read on the screen in use.
//   Never `performTouchInput` or `performClick` for a time line: they reach the `AndroidComposeView`, not the activity.
// - The clock (G-14; ADR-006; design §2 S-46): `AppClock.override` is a kit clock that starts at `elapsedRealtime()` and
//   `LocalDate.now()` (KitApp `kitUseClock`); it moves on the main thread with a fold right after each move (KitApp
//   `kitMoveClock`), and its reset to null is a registered restore. That jump back is S-45's backwards reading (review F5), so
//   each leg ends with the player idle and no real second is counted after it.
// - The seconds are read through `AppGraph.controller` (S-1Δ3 `playTimes`, `timeSeconds`), never from the timer. No class here
//   writes the timer setting (seam note 4; P6).
// - Every class leaves the controller and the device as it found them (WO-006 seam note 3; the brief): the controller (KitApp
//   `start`), `AppClock.override` null, the screen on and unlocked with the app in front. Helper names carry a `kit` prefix.
package app.tangramplaytime.app

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.lifecycle.Stage
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.PlayTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveTimeAppKitTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val kit = KitApp(composeRule)

    @get:Rule(order = 1)
    val kitRestore: TestRule = kit.restoreRule()

    /** A New full puzzle opened through the controller, so the board's margin is empty of pieces (S-2 `open`). */
    private fun kitOpenANewFullPuzzle(what: String) {
        val p = kit.firstNew(PuzzleKind.FULL, "$what: a New full puzzle")
        kit.openByController(p.id, "$what: ${p.id} New")
        assertTrue("fixture: $what: the board is shown", kit.exists(PlayTags.BOARD))
    }

    /** Asserts today's and the all-time play time grew by [seconds] from [from] to [to] (REQ-029 statement: each active second is added to both). */
    private fun kitAssertPlayTimeGrew(from: KitAppCounts, to: KitAppCounts, seconds: Int, what: String) {
        assertEquals("$what: the all-time play time (S-1Δ3 playTimes)", from.allTime + seconds, to.allTime)
        assertEquals("$what: today's play time (S-1Δ3 playTimes)", from.today + seconds, to.today)
    }

    // ── REQ-029 A1, through the real window ──────────────────────────────────────────────────────────────

    // REQ-029 A1 — "Leaving the phone untouched for 5 minutes adds at most 60 s." TYPE-005: a second counts only while the game is
    // visible "and the player's last touch was at most 60 s ago"; REQ-029 statement: each active second goes "to today's and to
    // the all-time play time". On the app's own controller (`AppGraph`'s `ActiveTime` over `AppClock`, S-15Δ3), with touches
    // through the window, MainActivity read back RESUMED before each clock move:
    // - the control: a touch, then 30 s → exactly +30 s of play time, today and all-time (so a touch reached the kernel);
    // - a touch, then 5 minutes untouched, moved 10 s at a time with a fold after each (as the app's ticker folds) → exactly
    //   +60 s: at most 60, and an exact 60 also fails a hook that never reported the touch (review F1: "After 5 min untouched
    //   the test sees exactly 60 s counted, not 0").
    @Test
    fun leavingThePhoneUntouchedForFiveMinutesAddsAtMostSixtySeconds() { // REQ-029.A1
        kit.start("REQ-029 A1 through the window")
        kitOpenANewFullPuzzle("REQ-029 A1")
        val clock = kit.kitUseClock("REQ-029 A1")
        val start = kit.kitFold()

        kit.kitAssertResumed("the control, before its clock move")
        kit.kitTouchThroughWindow("the control's touch")
        val control = kit.kitMoveClock(clock, 30)
        kitAssertPlayTimeGrew(start, control, 30, "the control: a touch through the window, then 30 s")

        kit.kitAssertResumed("the untouched leg, before its clock moves")
        kit.kitTouchThroughWindow("the touch before the 5 untouched minutes")
        var last = control
        repeat(30) { last = kit.kitMoveClock(clock, 10) }
        kitAssertPlayTimeGrew(control, last, 60, "a touch, then 5 untouched minutes: exactly 60 s")
        kit.kitAssertResumed("the untouched leg, after its clock moves")
    }

    // ── A stopped app counts nothing (pin) ───────────────────────────────────────────────────────────────

    // Pin (no token): TYPE-005 ("only while the game is visible on screen"); decisions.md 2026-10-20 "TYPE-005 as code." ("Visible
    // on screen" = MainActivity between `onStart` and `onStop`; "screen off stops it"); S-15Δ3 (`onStart` and `onStop` call
    // `visible`); design §3 library fact 1 ("Screen-off stopping it on API 31+ is checked on the floor image"; this class checks
    // it on every image). The lifecycle is read back before each clock move:
    // - the control: RESUMED, a touch, then 20 s → +20 s;
    // - a touch, Home at once, MainActivity read back STOPPED, then 5 minutes, then the app brought back and read back RESUMED →
    //   +0 s (an app still counted visible would add 60 s: the touch's window);
    // - the same with the screen off: a touch, the power key (`KEYCODE_SLEEP`), STOPPED read back, 5 minutes, the screen on and
    //   the keyguard dismissed, RESUMED → +0 s.
    @Test
    fun aStoppedAppCountsNothingAfterHomeOrWithTheScreenOff() {
        kit.start("a stopped app")
        kitOpenANewFullPuzzle("a stopped app")
        kit.kitRestoreScreenAndAppAfterTheTest()
        val clock = kit.kitUseClock("a stopped app")
        val start = kit.kitFold()

        kit.kitAssertResumed("the control, before its clock move")
        kit.kitTouchThroughWindow("the control's touch")
        val control = kit.kitMoveClock(clock, 20)
        kitAssertPlayTimeGrew(start, control, 20, "the control: a touch through the window, then 20 s")

        kit.kitAssertResumed("before Home")
        kit.kitTouchThroughWindow("the touch before Home")
        val beforeHome = kit.activity("before Home")
        kit.kitPressHome()
        kit.kitAwaitStage(beforeHome, Stage.STOPPED, "after Home")
        kit.kitMoveClock(clock, 300)
        kit.kitBringAppToFront("after 5 minutes at Home")
        kit.kitAssertResumed("back from Home")
        val afterHome = kit.kitFold()
        kitAssertPlayTimeGrew(control, afterHome, 0, "a touch, Home at once, 5 minutes, back: nothing counted while stopped")

        kit.kitAssertResumed("before the screen goes off")
        kit.kitTouchThroughWindow("the touch before the screen goes off")
        val beforeSleep = kit.activity("before the screen goes off")
        kit.kitScreenOff()
        kit.kitAwaitStage(beforeSleep, Stage.STOPPED, "the screen off")
        kit.kitMoveClock(clock, 300)
        kit.kitScreenOn("after 5 minutes with the screen off")
        kit.awaitSteadyActivity("the screen on again: MainActivity resumed")
        kit.kitAssertResumed("the screen on again")
        val afterSleep = kit.kitFold()
        kitAssertPlayTimeGrew(control, afterSleep, 0, "a touch, the screen off, 5 minutes, the screen on: nothing counted while stopped")
    }

    // ── REQ-031 A1, the app leg ──────────────────────────────────────────────────────────────────────────

    // REQ-031 A1 — "On a fresh install, no timer is shown." REQ-031 rule: "Setting values: off (default), on". The app's own store,
    // cleared by the reset before the run (tools/device_reset.sh, `pm clear`); no app-kit class writes the setting (seam note 4;
    // P6), so the controller reads the default (S-1Δ3 `timerShown`: "from Persistence.settings, else false"). No `timer` node
    // on a New puzzle, In progress, or Solved (D9: hidden on a Solved puzzle; design §3 "No timer on a fresh install or on the
    // solved view"). Liveness, so a drawn timer would have something to show: the board is shown, `timerShown` reads false,
    // and after a touch through the window and 60 s the open puzzle's `timeSeconds` is 60. The positive control, with the
    // setting on, is `play/`'s TimerKitTest (P6).
    @Test
    fun aClearedAppShowsNoTimer() { // REQ-031.A1
        kit.start("REQ-031 A1, the app leg")
        assertFalse("fixture: the app's store holds the default setting, off (S-1Δ3 timerShown; seam note 4)", kit.onController { timerShown })
        val p = kit.firstNew(PuzzleKind.FULL, "REQ-031 A1: a New full puzzle")
        kit.openByController(p.id, "REQ-031 A1: ${p.id} New")
        assertTrue("liveness: the board is shown", kit.exists(PlayTags.BOARD))
        assertFalse("a New puzzle: no `${PlayTags.TIMER}` node", kit.exists(PlayTags.TIMER))

        val order = kitAppLockOrder(p) ?: throw AssertionError("kit: no lock order for ${p.id}")
        kit.placeByController(order.first(), "REQ-031 A1: one piece placed")
        assertEquals("fixture: In progress", PuzzleState.IN_PROGRESS, kit.onController { state })
        val clock = kit.kitUseClock("REQ-031 A1")
        val base = kit.kitFold()
        kit.kitAssertResumed("before the clock move")
        kit.kitTouchThroughWindow("REQ-031 A1: a touch")
        val counted = kit.kitMoveClock(clock, 60)
        assertEquals("liveness: the open puzzle's timeSeconds grew by 60 after a touch and 60 s (S-1Δ3)", base.puzzle + 60, counted.puzzle)
        assertTrue("liveness: the open puzzle's timeSeconds is above 0", counted.puzzle > 0)
        assertFalse("In progress with 60 s counted: no `${PlayTags.TIMER}` node", kit.exists(PlayTags.TIMER))

        kit.solveByController("REQ-031 A1: solved")
        kit.awaitSolvedRow("REQ-031 A1: the solved view")
        assertFalse("the solved view: no `${PlayTags.TIMER}` node", kit.exists(PlayTags.TIMER))
    }
}
