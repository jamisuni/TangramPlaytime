// Acceptance kit (WO-007, TASK-058; kit stage 3): Reset all progress in the real app (designs/WO-007-design.md §2 S-49 "Reset:
// inline `remember` state; only Erase calls `resetAll()`", S-2Δ4 `resetAll()`, "Kit doors (Q11)": "A resetting class: needs every
// puzzle New at its start and leaves them New. Before both Cancel and Erase it builds the JVM leg's trap state, by touch and
// through the real switch"; decisions.md 2026-10-21 "Reset all progress, in order, and where the player is afterwards." (E6) and
// "Adds the oracles to … Reset all progress …" (E6a); reviews/WO-007-design-review.md F4).
//
// - Tokens (design §2 "Tokens"): REQ-034 A1 and A2, each on the one test whose assertion is that line's meaning
//   (traceability-rules.md §2). The restart after a reset is the JVM leg's (`ResetRestartKitTest`, E6a); this class reads the
//   screen.
// - The trap (E6a: "a Solved puzzle with a best; the open puzzle In progress with pieces and time; play time above 0; the timer
//   on"), built before Erase and before Cancel: the seconds by touches through the window and the kit clock (KitApp
//   `kitTouchThroughWindow`, `kitMoveClock`), the timer through the real switch, pressed by touch on the open screen. The boards
//   are set up through S-2 (KitApp `placeByController`, `solveByController`), the kit's standing practice for a fixture that is
//   not the gesture under test; the reset itself — Reset, Erase or Cancel — is pressed by touch.
// - "Shows" (REQ-034 A1: "every puzzle shows as New"): read on the screen, from the nodes — the settings screen's rows, the Play
//   screen, and every cell of the grid overview, each cell's PuzzleStateKey (S-14Δ cell keys).
// - A resetting class (the brief; seam note 5): each test starts with every puzzle New, failing loudly otherwise
//   (KitApp `requireEveryPuzzleNew`), and leaves every puzzle New and every time erased: its restore calls `resetAll()` through
//   the controller before KitApp's own restores put the settings and the open puzzle back (E4a). Nothing sleeps.
package app.tangramplaytime.app

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PlayTimes
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Settings
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.store.ProgressStores
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ResetKitTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val kit = KitApp(composeRule)

    @get:Rule(order = 1)
    val kitRestore: TestRule = kit.restoreRule()

    private companion object {
        /** The seconds the trap counts: on the puzzle that is then solved (its best), and on the open puzzle (TYPE-005: each under 60 s). */
        const val KIT_SOLVED_SECONDS = 20
        const val KIT_OPEN_SECONDS = 15
    }

    /** The trap of E6a, as the kit built it. */
    private data class KitTrap(val solved: PuzzleDefinition, val best: Int, val open: PuzzleDefinition, val placed: List<Placement>, val openSeconds: Int)

    /** What the controller holds that a reset changes or keeps (S-1Δ4 reads), for "Cancelling the confirmation changes nothing". */
    private data class KitHeld(
        val openId: String,
        val states: Map<String, PuzzleState>,
        val placed: List<Placement>,
        val seconds: Int,
        val best: Int?,
        val playTimes: PlayTimes,
        val settings: Settings,
        val bestTimes: List<Pair<String, Int>>,
    )

    private fun kitHeld(): KitHeld {
        val ids = kit.library.puzzles.map { it.id }
        return kit.onController {
            KitHeld(puzzle.id, ids.associateWith { stateOf(it) }, placed.toList(), timeSeconds, bestSeconds, playTimes, settings, bestTimes.map { it.puzzle.id to it.seconds })
        }
    }

    /**
     * The start of a resetting class's test: KitApp `start`, every puzzle New (failing loudly otherwise), and the class's own
     * restore — `resetAll()` through the controller (S-2Δ4), the settings screen closed first — registered after `start`'s, so it
     * runs before them (restores run newest first): every puzzle is New and every time erased before the settings and the open
     * puzzle are put back.
     */
    private fun kitStartResetting(what: String) {
        kit.start(what)
        kit.requireEveryPuzzleNew(what)
        kit.onRestore("every puzzle New and every time erased (a resetting class)") {
            val c = AppGraph.controller.value
            if (c != null) {
                kit.onMain {
                    if (c.settingsOpen) c.closeSettings()
                    c.resetAll()
                }
                kit.flush(c)
            }
        }
    }

    /**
     * E6a's trap: a full puzzle solved after a touch and [KIT_SOLVED_SECONDS] s (its best, S-2Δ3); then another full puzzle
     * opened, two pieces placed and [KIT_OPEN_SECONDS] s counted on it after a touch; then the settings opened by the gear and the
     * timer switch turned on by touch. The screen is left open.
     */
    private fun kitBuildTrap(what: String): KitTrap {
        val clock = kit.kitUseClock(what)
        val fulls = kit.library.puzzles.filter { it.kind == PuzzleKind.FULL }
        if (fulls.size < 2) throw AssertionError("kit: precondition ($what): two full puzzles")
        val s = fulls[0]
        val a = fulls[1]

        kit.openByController(s.id, "$what: ${s.id}, to be solved")
        val orderS = kitAppLockOrder(s) ?: throw AssertionError("kit: no lock order for ${s.id}")
        kit.placeByController(orderS.first(), "$what: ${s.id} In progress")
        kit.kitTouchThroughWindow("$what: a touch on ${s.id}")
        kit.kitMoveClock(clock, KIT_SOLVED_SECONDS)
        kit.solveByController("$what: ${s.id} solved")
        val best = kit.onController { bestSeconds } ?: throw AssertionError("kit: $what: ${s.id} solved by S-2 has no best time")
        assertEquals("fixture: ${s.id} Solved", PuzzleState.SOLVED, kit.onController { stateOf(s.id) })

        kit.openByController(a.id, "$what: ${a.id}, the open puzzle")
        val orderA = kitAppLockOrder(a) ?: throw AssertionError("kit: no lock order for ${a.id}")
        orderA.take(2).forEach { kit.placeByController(it, "$what: ${a.id}'s pieces") }
        kit.kitTouchThroughWindow("$what: a touch on ${a.id}")
        val counted = kit.kitMoveClock(clock, KIT_OPEN_SECONDS)
        assertEquals("fixture: ${a.id} In progress", PuzzleState.IN_PROGRESS, kit.shownState())
        assertTrue("fixture: the play time is above 0 (E6a)", counted.allTime > 0)

        kit.openSettingsByTouch(what)
        kit.scrollIntoView(KitAppTag.SETTINGS_TIMER, what)
        if (kitToggle(KitAppTag.SETTINGS_TIMER) != ToggleableState.On) {
            kit.pressCentre(KitAppTag.SETTINGS_TIMER)
            kit.await("$what: the timer switch reads On") { kitToggle(KitAppTag.SETTINGS_TIMER) == ToggleableState.On }
        }
        assertTrue("fixture: the timer on through the real switch (E6a)", kit.onController { settings.timerShown })
        return KitTrap(s, best, a, kit.onController { placed.toList() }, counted.puzzle)
    }

    /** Reset pressed by touch on the open screen: the inline question with Erase and Cancel (S-49; REQ-034 rule 1). */
    private fun kitAskTheQuestion(what: String) {
        kit.scrollIntoView(KitAppTag.SETTINGS_RESET, what)
        kit.pressCentre(KitAppTag.SETTINGS_RESET)
        kit.awaitExists(KitAppTag.RESET_QUESTION, "$what: Reset asks its question (REQ-034 rule 1: a second, explicit confirmation)")
        kit.awaitExists(KitAppTag.RESET_ERASE, "$what: Erase")
        kit.awaitExists(KitAppTag.RESET_CANCEL, "$what: Cancel")
    }

    private fun kitToggle(tag: String): ToggleableState =
        kit.node(tag).config.getOrNull(SemanticsProperties.ToggleableState) ?: throw AssertionError("kit: `$tag` carries no toggle state (S-49)")

    // ── REQ-034 ──────────────────────────────────────────────────────────────────────────────────────────

    @Test // REQ-034.A1
    fun afterAConfirmedResetEveryPuzzleShowsAsNew() {
        // REQ-034 A1: "After a confirmed reset, every puzzle shows as New." Statement: "WHEN the player confirms Reset all progress
        // in the settings, the game SHALL set every puzzle to New and erase all puzzle, best and play times." Rule 1: a second,
        // explicit confirmation; rule 2: "Settings (timer, sound) are kept". E6: "the player stays on the open puzzle. The settings
        // screen stays open, showing 0:00, 0:00 and no best times" (checkpoint candidate 3, its default).
        // Oracle, over E6a's trap (which can fail: before the reset the screen shows the Solved puzzle's best-time row and play
        // times above 0, and the open puzzle is In progress): Reset, then Erase, by touch. Then, on the screen: the question gone,
        // the screen still open, today's and all-time rows "0:00", no best-time row, the timer switch still On; after Done, the
        // Play screen shows the same puzzle New with nothing on the board and every piece in the tray; and on the grid every
        // puzzle's cell shows New.
        kitStartResetting("REQ-034 A1")
        val t = kitBuildTrap("REQ-034 A1")
        val en = kit.contextFor("en")
        kit.scrollIntoView(KitAppTag.bestTimeRow(t.solved.id), "REQ-034 A1: the positive control")
        assertTrue("positive control: before the reset the screen shows ${t.solved.id}'s best-time row", kit.exists(KitAppTag.bestTimeRow(t.solved.id)))
        assertNotEquals("positive control: before the reset the all-time row is not 0:00", kit.kitTimeText(0, en), kit.mergedTexts(KitAppTag.PLAY_TIME_ALL).last())

        kitAskTheQuestion("REQ-034 A1")
        kit.pressCentre(KitAppTag.RESET_ERASE)
        kit.awaitGone(KitAppTag.RESET_QUESTION, "REQ-034 A1: Erase answers the question")

        assertTrue("the settings screen stays open after the reset (E6)", kit.exists(KitAppTag.SETTINGS_SHEET) && kit.onController { settingsOpen })
        kit.scrollIntoView(KitAppTag.PLAY_TIME_TODAY, "REQ-034 A1: the play times")
        kit.await("REQ-034 A1: today's row shows 0:00 (E6)") { kit.mergedTexts(KitAppTag.PLAY_TIME_TODAY).lastOrNull() == kit.kitTimeText(0, en) }
        kit.await("REQ-034 A1: the all-time row shows 0:00 (E6)") { kit.mergedTexts(KitAppTag.PLAY_TIME_ALL).lastOrNull() == kit.kitTimeText(0, en) }
        assertFalse("no best-time row after the reset (REQ-034 statement: best times erased; E6)", kit.exists(KitAppTag.bestTimeRow(t.solved.id)))
        kit.scrollIntoView(KitAppTag.SETTINGS_TIMER, "REQ-034 A1: the settings kept")
        assertEquals("the timer setting is kept (REQ-034 rule 2)", ToggleableState.On, kitToggle(KitAppTag.SETTINGS_TIMER))

        kit.closeSettingsByDone("REQ-034 A1")
        assertEquals("the player stays on the open puzzle (E6)", t.open.id, kit.shownId())
        assertEquals("…which shows as New", PuzzleState.NEW, kit.shownState())
        assertTrue("…with nothing on the board", PieceId.entries.none { kit.exists(PlayTags.boardPiece(it)) })
        for (piece in t.open.pieces) assertTrue("…and $piece in the tray", kit.exists(PlayTags.trayPiece(piece)))

        kit.pressCentre(KitAppTag.COUNTER)
        kit.awaitExists(KitAppTag.OVERVIEW, "REQ-034 A1: the grid")
        val notNew = kit.library.puzzles.map { it.id to kit.key(KitAppTag.overviewCell(it.id), PlaySemantics.PuzzleStateKey) }.filter { it.second != PuzzleState.NEW }
        assertTrue("every puzzle's cell in the grid shows New after the reset (REQ-034 A1): not New $notNew", notNew.isEmpty())
        assertEquals("…including ${t.solved.id}, Solved before", PuzzleState.NEW, kit.key(KitAppTag.overviewCell(t.solved.id), PlaySemantics.PuzzleStateKey))
        kit.onController { closeOverview() }
        kit.awaitGone(KitAppTag.OVERVIEW, "REQ-034 A1: the grid closed")
    }

    @Test // REQ-034.A2
    fun cancellingTheConfirmationChangesNothing() {
        // REQ-034 A2: "Cancelling the confirmation changes nothing." Rule 1: the reset "needs a second, explicit confirmation inside
        // the settings screen"; the must-never of design §3 ("Cancel changes nothing"); S-49 ("only Erase calls `resetAll()`").
        // Oracle, over E6a's trap, so that a reset would show (F4: "it needs non-trivial state before Cancel"): Reset, then Cancel,
        // by touch. Then: the question gone and Reset back; the controller holds what it held — every puzzle's state, the open
        // puzzle, its pieces, its time and best, the play times, the settings and the best-time list (S-1Δ4) — and the screen
        // shows what it showed: the two play-time rows and the Solved puzzle's best-time row. After Done, the Play screen shows
        // the open puzzle In progress with the same pieces.
        kitStartResetting("REQ-034 A2")
        val t = kitBuildTrap("REQ-034 A2")
        val before = kitHeld()
        kit.scrollIntoView(KitAppTag.PLAY_TIME_TODAY, "REQ-034 A2")
        val todayBefore = kit.mergedTexts(KitAppTag.PLAY_TIME_TODAY)
        val allBefore = kit.mergedTexts(KitAppTag.PLAY_TIME_ALL)
        kit.scrollIntoView(KitAppTag.bestTimeRow(t.solved.id), "REQ-034 A2")
        val rowBefore = kit.mergedTexts(KitAppTag.bestTimeRow(t.solved.id))
        assertEquals("fixture: ${t.solved.id} Solved with its best", PuzzleState.SOLVED, before.states[t.solved.id])
        assertEquals("fixture: ${t.open.id} In progress", PuzzleState.IN_PROGRESS, before.states[t.open.id])

        kitAskTheQuestion("REQ-034 A2")
        kit.pressCentre(KitAppTag.RESET_CANCEL)
        kit.awaitGone(KitAppTag.RESET_QUESTION, "REQ-034 A2: Cancel answers the question")
        kit.scrollIntoView(KitAppTag.SETTINGS_RESET, "REQ-034 A2: Reset back")
        assertTrue("Reset is back after Cancel", kit.exists(KitAppTag.SETTINGS_RESET))

        assertEquals("cancelling changes nothing the controller holds (REQ-034 A2)", before, kitHeld())
        kit.scrollIntoView(KitAppTag.PLAY_TIME_TODAY, "REQ-034 A2: the rows after Cancel")
        assertEquals("…today's row as it was", todayBefore, kit.mergedTexts(KitAppTag.PLAY_TIME_TODAY))
        assertEquals("…the all-time row as it was", allBefore, kit.mergedTexts(KitAppTag.PLAY_TIME_ALL))
        kit.scrollIntoView(KitAppTag.bestTimeRow(t.solved.id), "REQ-034 A2: the best-time row after Cancel")
        assertEquals("…${t.solved.id}'s best-time row as it was", rowBefore, kit.mergedTexts(KitAppTag.bestTimeRow(t.solved.id)))

        kit.closeSettingsByDone("REQ-034 A2")
        assertEquals("after Done the same puzzle (REQ-032 statement)", t.open.id, kit.shownId())
        assertEquals("…still In progress", PuzzleState.IN_PROGRESS, kit.shownState())
        val shown = PieceId.entries.filter { kit.exists(PlayTags.boardPiece(it)) }.toSet()
        assertEquals("…with the same pieces on the board", t.placed.map { it.piece }.toSet(), shown)
    }

    // ── REQ-034 rule 1: a repeated press never answers with Erase (pin) ─────────────────────────────────

    // Pin (no token: REQ-034 rule 1 has no acceptance ID; the licence line's practice): REQ-034 rule 1 ("The reset needs a second,
    // explicit confirmation inside the settings screen"); reviews/WO-007-code-review.md N4 and decisions.md 2026-10-21 "N4 is
    // raised to a Should and fixed before TASK-062, kit first …" (a double tap on Reset must not erase). A press repeated where
    // Reset was, once the question shows, never answers it with Erase. Over E6a's trap (so an erase would show), on two windows —
    // the device's own, and a short phone of 360 × 533 dp (a display override, read back) where the body scrolls (asserted: a
    // scrolling node with a vertical range) — each read after the scroll and the layout settle:
    // - geometric: Reset's bounds read just before a press at its centre; once the question shows and Erase's bounds read the
    //   same in two reads an idle apart, Erase's bounds hold no point of Reset's;
    // - behavioural: for each of five points of Reset's bounds (its centre and 1 dp inside each corner), a fresh press on Reset,
    //   then a second still tap at that point: what the controller holds (every puzzle's state, the open board, its time and
    //   best, the play times, the settings, the best-time list) and what the store holds after a flush (every puzzle's
    //   progress and the play times, `ProgressStores.onDevice`, the app's own instance, S-18) are as before.
    // The fix's layout is not prescribed: Cancel under the old place, Erase moved away, or anything else that keeps Erase off
    // Reset's bounds passes. Every failure is collected so one run names each window and form; after an erase the trap is
    // built again for the next window.
    @Test
    fun aSecondPressWhereResetWasNeverAnswersTheQuestionWithErase() {
        kitStartResetting("REQ-034 rule 1")
        val failures = ArrayList<String>()
        val t = kitBuildTrap("REQ-034 rule 1")
        kitSecondPressLeg("the device's own window", t, failures, needsScroll = false)

        kit.onController { if (settingsOpen) closeSettings() }
        kit.awaitGone(KitAppTag.SETTINGS_SHEET, "REQ-034 rule 1: the settings closed before the display change")
        kit.usePhone(KIT_SHORT_PHONE, "REQ-034 rule 1: a short phone window")
        val again = if (kit.onController { stateOf(t.solved.id) } == PuzzleState.SOLVED) t else kitBuildTrap("REQ-034 rule 1, rebuilt after an erase")
        kitSecondPressLeg("a short phone window (360 × 533 dp)", again, failures, needsScroll = true)

        if (failures.isNotEmpty()) {
            throw AssertionError("REQ-034 rule 1: a press repeated where Reset was answered, or could answer, the question with Erase (${failures.size}):\n" + failures.joinToString("\n"))
        }
    }

    /** One window's two forms (see the pin's comment); failures go to [failures]. The settings screen is left open, the question closed. */
    private fun kitSecondPressLeg(leg: String, t: KitTrap, failures: MutableList<String>, needsScroll: Boolean) {
        if (!kit.exists(KitAppTag.SETTINGS_SHEET)) kit.openSettingsByTouch("$leg: the settings")
        assertEquals("fixture: $leg: the trap stands (${t.solved.id} Solved)", PuzzleState.SOLVED, kit.onController { stateOf(t.solved.id) })
        if (needsScroll) {
            val scrolls = kit.allNodes().any { n -> (n.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke() ?: 0f) > 0f }
            assertTrue("fixture: $leg: the settings body scrolls in this window (a scrolling node with a vertical range above 0)", scrolls)
        }

        // Geometric.
        kitCloseTheQuestion(leg)
        kit.scrollIntoView(KitAppTag.SETTINGS_RESET, leg)
        assertTrue("fixture: $leg: Reset wholly in view before its press", kit.wholeInView(KitAppTag.SETTINGS_RESET))
        val reset = kit.bounds(KitAppTag.SETTINGS_RESET)
        kit.tap(reset.center, "$leg: the first press on Reset")
        kit.awaitExists(KitAppTag.RESET_QUESTION, "$leg: Reset asks its question")
        val erase = kitSettledBounds(KitAppTag.RESET_ERASE, leg)
        Log.i("ResetKitTest", "$leg: Reset $reset; Erase settled at $erase; Cancel ${if (kit.exists(KitAppTag.RESET_CANCEL)) kit.bounds(KitAppTag.RESET_CANCEL).toString() else "absent"}")
        if (reset.overlaps(erase)) {
            failures += "$leg, geometric: Erase's settled bounds $erase hold points of Reset's bounds $reset, so a press repeated there erases"
        }

        // Behavioural.
        for (point in listOf("centre", "top-left corner", "top-right corner", "bottom-left corner", "bottom-right corner")) {
            kitCloseTheQuestion(leg)
            kit.scrollIntoView(KitAppTag.SETTINGS_RESET, leg)
            val r = kit.bounds(KitAppTag.SETTINGS_RESET)
            val inset = kit.dp(1f)
            val at = when (point) {
                "centre" -> r.center
                "top-left corner" -> Offset(r.left + inset, r.top + inset)
                "top-right corner" -> Offset(r.right - inset, r.top + inset)
                "bottom-left corner" -> Offset(r.left + inset, r.bottom - inset)
                else -> Offset(r.right - inset, r.bottom - inset)
            }
            val held = kitHeld()
            val stored = kitStoredNow()
            kit.tap(r.center, "$leg: a first press on Reset")
            kit.awaitExists(KitAppTag.RESET_QUESTION, "$leg: Reset asks its question")
            kitSettledBounds(KitAppTag.RESET_ERASE, leg)
            kit.tap(at, "$leg: a second press at Reset's $point $at")
            kit.idle()
            val heldAfter = kitHeld()
            val storedAfter = kitStoredNow()
            if (heldAfter != held || storedAfter != stored) {
                failures += "$leg, behavioural: a second press at Reset's $point $at (Reset was $r) changed the game: " +
                    "${t.solved.id} ${held.states[t.solved.id]} → ${heldAfter.states[t.solved.id]}, play times ${held.playTimes} → ${heldAfter.playTimes}, " +
                    "store ${if (storedAfter == stored) "unchanged" else "changed"}"
                break
            }
        }
        kitCloseTheQuestion(leg)
    }

    /** Closes the question by Cancel when it is shown (S-49: "only Erase calls `resetAll()`"). */
    private fun kitCloseTheQuestion(what: String) {
        if (!kit.exists(KitAppTag.RESET_QUESTION)) return
        kit.scrollIntoView(KitAppTag.RESET_CANCEL, what)
        kit.pressCentre(KitAppTag.RESET_CANCEL)
        kit.awaitGone(KitAppTag.RESET_QUESTION, "$what: Cancel closes the question")
    }

    /** [tag]'s bounds once they read the same in two reads an idle apart (the scroll and the sheet's layout settled). */
    private fun kitSettledBounds(tag: String, what: String): Rect {
        var last: Rect? = null
        kit.await("$what: `$tag` settled") {
            kit.idle()
            val now = kit.bounds(tag)
            val before = last
            last = now
            before != null && abs(before.left - now.left) <= 0.5f && abs(before.top - now.top) <= 0.5f &&
                abs(before.right - now.right) <= 0.5f && abs(before.bottom - now.bottom) <= 0.5f
        }
        return last ?: throw AssertionError("kit: $what: `$tag` was never read")
    }

    /** What the store holds now, after a flush: every puzzle's progress and the play times (G-13: the one store; S-18: one instance). */
    private fun kitStoredNow(): Pair<Map<String, PuzzleProgress>, PlayTimes> {
        kit.flush()
        val store = ProgressStores.onDevice(kit.target)
        return runBlocking { store.allProgress() to store.playTimes() }
    }
}

/** A short phone for REQ-034 rule 1's second window: 1080 × 1600 px at 480 dpi = 360 × 533 dp, portrait (TYPE-007: a phone). */
private val KIT_SHORT_PHONE = KitDisplay(1080, 1600, 480)
