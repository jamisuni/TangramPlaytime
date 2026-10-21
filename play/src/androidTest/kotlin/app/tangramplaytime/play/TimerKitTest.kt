// Acceptance kit (WO-008, TASK-048): the timer on the board, in `PlayScreen` on the device (designs/WO-008-design.md §2 S-13Δ3 /
// S-47 timer, S-1Δ3 `timerShown` and `timeSeconds`, S-2Δ3 the fold, S-48 the format; §3 "No timer on a fresh install or on the
// solved view"; decisions.md 2026-10-20 "The timer on the board, as drawn." (D9, checkpoint candidate 1, on its default) and
// "What counts on each screen." (D4)).
//
// - Tokens: REQ-031 A1 and A2, each on the one test whose assertion is that line's meaning (traceability-rules.md §2). The pin
//   of the timer's place and inertness carries none.
// - The controller is built the way `:app` builds it (KitScreen.kt `kitTimedControllerOver`: S-15Δ3's order, over the kit's store
//   double) with the real `ActiveTime` (S-45) on the kit's clock (`KitDeviceClock`). The kit reports touches to the kernel
//   itself (`time.touched()`, in place of `:app`'s window hook: design §2 "Kit doors", `play/` device), makes the game visible
//   through the controller (`setVisible(true)`, as `AppGraph.start` does), and ticks with `countTime()` (S-2Δ3). Every touch,
//   clock move and tick runs on the main thread (S-44 "One caller thread"; platform-notes/android.md § Compose test kit: the
//   clock is advanced on the main thread, never slept). The compose rule's `mainClock` stays paused (KitScreen `show()`) and
//   only paces frames.
// - The setting: REQ-031 A1's store is empty (default `Settings`; the IProgressStore KDoc: missing stored state "behaves as a
//   fresh install"); A2's has `Settings(timerShown = true)` saved before the load (S-19Δ: `load` reads it; S-1Δ3: `timerShown`
//   "from Persistence.settings, else false"). WO-007's switch does not exist yet (D9: "Until WO-007's switch exists, only the
//   stored setting turns it on").
// - The timer's text is the `timer` node's merged text (S-47: "An inert merged node whose text is the time"). The expected value
//   is S-48's form of the controller's `timeSeconds`, formed here by the kit (m:ss below one hour, hand-built ASCII), and each
//   read also checks the controller's counted seconds, so a stale or a wrong timer fails.
// - Puzzles: the first listed mini puzzle with two pieces or more, so a first lock (In progress) and the solve are two steps,
//   and the puzzle after it in ‹ › order. Boards are set up through S-2 (`placeByApi`; `kitSolveRest`): the lines are about the
//   timer, not the gestures. Helper names carry a `kit` prefix.
package app.tangramplaytime.play

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Settings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class TimerKitTest {

    @get:Rule
    val rule = createComposeRule()

    // ── Kit helpers ──────────────────────────────────────────────────────────────────────────────────────

    /** The first listed mini puzzle with at least two pieces (REQ-045), so its first lock and its solve are two steps. */
    private val kitPuzzle: PuzzleDefinition by lazy {
        kitLibrary.puzzles.firstOrNull { it.kind == PuzzleKind.MINI && it.pieces.size >= 2 }
            ?: throw AssertionError("fixture defect: the library lists no mini puzzle with two pieces")
    }

    /** S-48's form of [seconds] below one hour, as the kit forms it: whole minutes, then two ASCII digits of seconds. */
    private fun kitMss(seconds: Int): String {
        check(seconds in 0 until 3600) { "kit: the timer legs stay below one hour (S-48's m:ss), not $seconds s" }
        return "${seconds / 60}:" + (seconds % 60).toString().padStart(2, '0')
    }

    /**
     * The controller with a time source over a kit store whose timer setting is [timerShown] (none saved when null: a fresh
     * install), composed in `PlayScreen` (KitScreen `show()`), made visible (S-2Δ3 `setVisible(true)`, as `AppGraph.start`
     * does), and on [kitPuzzle] (S-2 `open`).
     */
    private fun kitBoot(timerShown: Boolean?): Pair<KitTimedPersisted, KitScreen> {
        val store = KitDeviceStore()
        if (timerShown != null) runBlocking { store.saveSettings(Settings(timerShown = timerShown)) }
        val t = kitTimedControllerOver(rule, store)
        val kit = KitScreen(rule, t.controller)
        kit.show()
        kit.onMain { t.controller.setVisible(true) }
        if (kit.puzzle.id != kitPuzzle.id) kit.open(kitPuzzle.id)
        kit.frames(2)
        assertEquals("fixture: ${kitPuzzle.id} open", kitPuzzle.id, kit.puzzle.id)
        assertEquals("fixture: ${kitPuzzle.id} New", PuzzleState.NEW, kit.onMain { t.controller.state })
        return t to kit
    }

    /**
     * [seconds] of active play as the app makes them, on the main thread: a touch, the kit clock moved by at most 30 s (inside
     * TYPE-005's "at most 60 s ago"), then a tick; repeated; then two frames so the timer recomposes (S-47: it "reads
     * `timeSeconds` in its own composable").
     */
    private fun kitPlay(t: KitTimedPersisted, kit: KitScreen, seconds: Int) {
        var left = seconds
        while (left > 0) {
            val step = min(30, left)
            kit.onMain {
                t.time.touched()
                t.clock.forward(step)
                t.controller.countTime()
            }
            left -= step
        }
        kit.frames(2)
    }

    /** Locks one piece of the open New puzzle through S-2 (TYPE-006: New → In progress when a piece first leaves the tray). */
    private fun kitPlaceOne(t: KitTimedPersisted, kit: KitScreen) {
        val p = kit.puzzle
        p.pieces.firstOrNull { kit.placeByApi(it) } ?: throw AssertionError("fixture defect: no piece of ${p.id} locks on its empty board")
        kit.frames(2)
        assertEquals("fixture: ${p.id} In progress after its first lock", PuzzleState.IN_PROGRESS, kit.onMain { t.controller.state })
    }

    /**
     * Solves the open puzzle from its board through S-2: every piece not yet on the board, each at its solution pose, in an
     * order the engine locks (KitScreen `placeByApi`; `solveByApi` starts from every piece and so cannot continue a board).
     */
    private fun kitSolveRest(t: KitTimedPersisted, kit: KitScreen) {
        repeat(8) {
            if (kit.onMain { t.controller.state } == PuzzleState.SOLVED) return
            val onBoard = kit.placed().map { it.piece }.toSet()
            val left = kit.puzzle.pieces.filter { it !in onBoard }
            val lockedNow = left.filter { kit.placeByApi(it) }
            if (lockedNow.isEmpty()) throw AssertionError("fixture defect: none of $left locks at its solution pose in ${kit.puzzle.id}")
            kit.frames()
        }
        if (kit.onMain { t.controller.state } != PuzzleState.SOLVED) throw AssertionError("fixture defect: ${kit.puzzle.id} is not solved after every piece was placed")
    }

    /**
     * The controller's counted seconds are [seconds] (S-1Δ3 `timeSeconds`), and the `timer` node shows them: it exists and its
     * merged text is S-48's form of them, which is [shown] (S-47, S-48).
     */
    private fun kitAssertTimer(t: KitTimedPersisted, kit: KitScreen, seconds: Int, shown: String, what: String) {
        assertEquals("fixture: the kit's m:ss of $seconds s", shown, kitMss(seconds))
        assertEquals("$what: the controller's counted seconds (S-1Δ3 timeSeconds)", seconds, kit.onMain { t.controller.timeSeconds })
        assertTrue("$what: the `${PlayTags.TIMER}` node is shown (S-13Δ3, S-47)", kit.exists(PlayTags.TIMER))
        assertEquals("$what: the timer's text is S-48's form of the controller's timeSeconds", shown, kit.text(PlayTags.TIMER))
    }

    private fun kitAssertNoTimer(kit: KitScreen, what: String) {
        assertFalse("$what: no `${PlayTags.TIMER}` node", kit.exists(PlayTags.TIMER))
    }

    // ── REQ-031 A1 ───────────────────────────────────────────────────────────────────────────────────────

    // REQ-031 A1 — "On a fresh install, no timer is shown." REQ-031 rule: "Setting values: off (default), on"; statement: "WHERE
    // the timer setting is on, the game SHALL show the running puzzle time". The store is empty, so the setting reads its
    // default (S-1Δ3 `timerShown` false). No `timer` node on a New puzzle, nor In progress with 30 s counted (the seconds are
    // read through the controller, so there is a time a timer would show), nor on the solved view, at the solve and once the
    // picture is fully shown (design §3 "No timer on a fresh install or on the solved view"). The positive control is this
    // class's REQ-031 A2 test and the pin below: the same tag, with the setting on, finds the node.
    @Test
    fun onAFreshInstallNoTimerIsShown() { // REQ-031.A1
        val (t, kit) = kitBoot(timerShown = null)
        assertFalse("fixture: a fresh store's timer setting is off (S-1Δ3 timerShown; Settings default)", kit.onMain { t.controller.timerShown })
        kitAssertNoTimer(kit, "a fresh install, a New puzzle")

        kitPlaceOne(t, kit)
        kitPlay(t, kit, 30)
        assertEquals("fixture: 30 s counted In progress (S-1Δ3 timeSeconds)", 30, kit.onMain { t.controller.timeSeconds })
        kitAssertNoTimer(kit, "a fresh install, In progress with 30 s counted")

        kitSolveRest(t, kit)
        assertEquals("fixture: Solved", PuzzleState.SOLVED, kit.onMain { t.controller.state })
        kitAssertNoTimer(kit, "a fresh install, the frame of the solve")
        kit.advance(2000)
        kitAssertNoTimer(kit, "a fresh install, the solved view with its picture shown")
    }

    // ── REQ-031 A2 ───────────────────────────────────────────────────────────────────────────────────────

    // REQ-031 A2 — "With the setting on, the timer is shown and counts active seconds only." REQ-031 rule: "The timer shows
    // active seconds of the current puzzle only (TYPE-005, REQ-030)". The setting seeded on before the load:
    // - a New puzzle shows "0:00" (D9: "It shows on a New puzzle ("0:00") and In progress"), and 10 s of play on it leave
    //   "0:00" (TYPE-006 rule 4: "Time is counted only In progress");
    // - In progress, a touch and 30 s: "0:30";
    // - active seconds only: a touch, then 5 minutes untouched while the ticker folds every 10 s: 60 s more, "1:30" — exactly,
    //   so a timer of the wall clock (5:30) and one that stopped (0:30) both fail (TYPE-005: the last touch "at most 60 s ago");
    // - the grid open: 20 s of play leave "1:30" (D4: "Grid overview open: play only"); closed again, 10 s: "1:40";
    // - another puzzle (S-2 `open` of the next, New): its own "0:00", and 10 s of play leave it there; back: "1:40" (the current
    //   puzzle's seconds only);
    // - solved: the node is gone at the solve and on the solved view (D9: "hidden on a Solved puzzle").
    @Test
    fun withTheSettingOnTheTimerIsShownAndCountsActiveSecondsOnly() { // REQ-031.A2
        val (t, kit) = kitBoot(timerShown = true)
        assertTrue("fixture: the setting saved on is the one the controller reads (S-1Δ3 timerShown; S-19Δ load)", kit.onMain { t.controller.timerShown })
        kitAssertTimer(t, kit, 0, "0:00", "a New puzzle (D9)")
        kitPlay(t, kit, 10)
        kitAssertTimer(t, kit, 0, "0:00", "a New puzzle after 10 s of play (TYPE-006 rule 4)")

        kitPlaceOne(t, kit)
        kitPlay(t, kit, 30)
        kitAssertTimer(t, kit, 30, "0:30", "In progress, a touch and 30 s")

        kit.onMain { t.time.touched() }
        repeat(30) {
            kit.onMain {
                t.clock.forward(10)
                t.controller.countTime()
            }
        }
        kit.frames(2)
        kitAssertTimer(t, kit, 90, "1:30", "a touch, then 5 untouched minutes with the ticker: 60 s more, not 300 (active seconds only)")

        kit.onMain { t.controller.openOverview() }
        kit.frames(2)
        assertTrue("fixture: the grid is open", kit.onMain { t.controller.overviewOpen })
        kitPlay(t, kit, 20)
        kitAssertTimer(t, kit, 90, "1:30", "the grid open, 20 s of play: the time does not move (D4)")
        kit.onMain { t.controller.closeOverview() }
        kit.frames(2)
        kitPlay(t, kit, 10)
        kitAssertTimer(t, kit, 100, "1:40", "the grid closed, 10 s of play: the time resumes")

        val next = kitAfter(kitPuzzle.id)
        kit.open(next.id)
        assertEquals("fixture: ${next.id} New", PuzzleState.NEW, kit.onMain { t.controller.state })
        kitAssertTimer(t, kit, 0, "0:00", "another puzzle, New: its own time (the current puzzle's seconds only)")
        kitPlay(t, kit, 10)
        kitAssertTimer(t, kit, 0, "0:00", "another puzzle, New, after 10 s of play")
        kit.open(kitPuzzle.id)
        kit.frames(2)
        kitAssertTimer(t, kit, 100, "1:40", "back on ${kitPuzzle.id}: its own time")

        kitSolveRest(t, kit)
        assertEquals("fixture: Solved", PuzzleState.SOLVED, kit.onMain { t.controller.state })
        kitAssertNoTimer(kit, "the setting on, the frame of the solve (D9: hidden on a Solved puzzle)")
        kit.advance(2000)
        kitAssertNoTimer(kit, "the setting on, the solved view with its picture shown")
    }

    // ── Pin: the timer's place and its inertness ──────────────────────────────────────────────────────────

    @Test // pin: D9 "drawn at the board's top-right corner, 14 dp in from its right and top edges" and "It takes no touch"; S-47 "Slot: right edge at the board's right − 14 dp, top at the board's top + 14 dp, width measured" and "An inert merged node whose text is the time, never in ShownControls"; S-13Δ3 `PlayTags.TIMER = "timer"`; platform-notes/android.md § Compose test kit (a tagged display container merges its descendants, inert: no click action, no role); no token
    fun theTimerSitsInTheBoardsTopRightCornerAndIsNoControl() {
        assertEquals("S-13Δ3: the timer's tag", "timer", PlayTags.TIMER)
        val (t, kit) = kitBoot(timerShown = true)

        fun kitCheckPlace(what: String) {
            // Read on the screen in use, from the nodes' bounds (WO-005 lesson), within 2 px of the dp values.
            val board = kit.bounds(PlayTags.BOARD)
            val timer = kit.bounds(PlayTags.TIMER)
            kitAssertNear("$what: the timer's right edge is the board's right − 14 dp (D9, S-47)", (board.right - kit.dp(14f)).toDouble(), timer.right.toDouble(), 2.0)
            kitAssertNear("$what: the timer's top is the board's top + 14 dp (D9, S-47)", (board.top + kit.dp(14f)).toDouble(), timer.top.toDouble(), 2.0)
            assertTrue(
                "$what: the timer $timer lies inside the board $board (D9: at the board's corner)",
                timer.left >= board.left && timer.right <= board.right && timer.top >= board.top && timer.bottom <= board.bottom,
            )
            val merged = rule.onNodeWithTag(PlayTags.TIMER).fetchSemanticsNode("kit: merged node `${PlayTags.TIMER}`")
            assertFalse("$what: the timer has no click action (S-47 inert; D9 \"It takes no touch\")", SemanticsActions.OnClick in merged.config)
            assertNull("$what: the timer has no role (S-47 inert)", merged.config.getOrNull(SemanticsProperties.Role))
        }

        kitAssertTimer(t, kit, 0, "0:00", "a New puzzle")
        kitCheckPlace("a New puzzle, \"0:00\"")
        kitPlaceOne(t, kit)
        kitPlay(t, kit, 75)
        kitAssertTimer(t, kit, 75, "1:15", "In progress, 75 s")
        kitCheckPlace("In progress, \"1:15\" (a wider text: the width is measured, the right edge stays)")
    }
}
