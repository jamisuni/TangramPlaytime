// Acceptance kit (WO-007, TASK-058; kit stage 3): the settings screen in the real app (designs/WO-007-design.md §2 S-49 with
// its tags and strings, S-1Δ4, S-2Δ4, S-15Δ4, "Tokens (Q10)", "Kit doors (Q11)"; §3 the must-nevers, "The layer must take every
// touch", "N1"; decisions.md 2026-10-21 rows E2 "The gear and the settings screen, as drawn.", E3 "The settings screen sits in
// the activity's window …", E4 / E4a (the switches and the kit's doors), E5 "Play time and best times on the settings screen.",
// E9, E10; WO-007 § Scope notes).
//
// - Tokens (design §2 "Tokens"): REQ-032 A1 and A2, REQ-009 A1, REQ-049 A1, REQ-029 A2 (its code home moved to `app/`: E9), and
//   REQ-031 A2 as an extra token through the real switch (E4; its `play/` token stands). Each token sits on the one test whose
//   assertion is that line's meaning (traceability-rules.md §2). Every other test is a pin and carries no token: it names its
//   line without the dot.
// - The texts (G-10; the brief): the free note and the privacy text are checked against the REQ sentences themselves (REQ-009
//   statement; REQ-049 rule 1), in English, on an English device; the licence line against S-49's strings row (E1: plain text,
//   not a link). No `R.string` that S-49 adds is named here (seam note 4), and no Finnish word.
// - The tags are S-49's tag row, as `KitAppTag` literals (seam note 4). Positions are read on the screen in use, from the nodes;
//   each "shows" is read with the node scrolled into view (auto-advance on: design §2 "Sheet scrolls") and asserted wholly in
//   view inside `settings-sheet`.
// - Time (G-14; ADR-006): the kit clock (`AppClock.override`, KitApp `kitUseClock`) moves on the main thread with a fold after
//   each move; touches that must count go through the window (KitApp `kitTouchThroughWindow`: `settings-title` while the screen
//   is open, the board's inert point otherwise; E4a). Nothing sleeps; every wait is bounded and fails loudly.
// - Board set-ups go through S-2 (KitApp `placeByController`, `solveByController`): they are fixtures, never the gesture under
//   test. The gear, the switches, Reset, Done and the scrim are pressed by touch.
// - The app's controller is one per process: each test leaves it, the settings and the device as it found them (seam note 5;
//   KitApp `start`'s restores, E4a). No Erase is pressed here.
package app.tangramplaytime.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsKitTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val kit = KitApp(composeRule)

    @get:Rule(order = 1)
    val kitRestore: TestRule = kit.restoreRule()

    private companion object {
        /** REQ-009 statement, the note in full (the Contract wording, G-10). */
        const val KIT_FREE_NOTE = "Enjoy, it's absolutely free. No ads, no purchases, no network. Your play time stays on this device."

        /** REQ-049 rule 1, the privacy text in full. */
        const val KIT_PRIVACY_TEXT = "Privacy: this game collects no data. It has no network access, no accounts and no analytics. " +
            "Your puzzle progress and play times are stored only on this device and are deleted when the game is uninstalled."

        /** S-49 strings row (E1; workorders/WO-007.md Q8: "plain text, never a link"; the address from the project's remote). */
        const val KIT_LICENCE_LINE = "Open source, GPL-3.0 — source code: github.com/jamisuni/TangramPlaytime"

        /** The pending `chg-draft-req032-how-to-play-text.md` (WO-007 Q8): "at most 60 words". */
        const val KIT_HOW_TO_MAX_WORDS = 60

        /** E4a's REQ-029 A2 oracle: M s on one date, N s on the next, K s with the screen open (each inside TYPE-005's 60 s). */
        const val KIT_M = 45
        const val KIT_N = 25
        const val KIT_K = 15
    }

    // ── REQ-032: the screen from the top bar ─────────────────────────────────────────────────────────────

    @Test // REQ-032.A1
    fun openingAndClosingTheSettingsChangesNothingOnTheBoard() {
        // REQ-032 A1: "Opening and closing the settings changes nothing on the board." Statement: "WHEN the player presses the gear
        // in the top bar, the game SHALL open the settings screen over the current puzzle, and WHEN it is closed, the game SHALL
        // return to the same puzzle state." Rule 2: "Puzzle time is paused while the settings screen is open." The must-never of
        // design §3 ("opening or closing the screen changes no placement, state, tray turn, open puzzle or puzzle time").
        // Oracle, over a board that can show a change (the brief: "over a board with placed pieces, a turned tray piece and
        // counted time"): a New full puzzle with two pieces placed (S-2), a tray piece turned by a tap, and 20 s of its time
        // counted through the window. The gear pressed by touch opens the screen over the same puzzle; with it open, a window
        // touch on `settings-title` and 15 s leave the puzzle's time where it was while the play time grows by them (the positive
        // control: E3, C1); Done closes it; the board read from the nodes — the open puzzle, its state, every placed piece's keys,
        // every tray piece's turn and mirror — and the puzzle's time are as before. Positive control after the closing: a window
        // touch and 10 s add 10 s to the puzzle's time.
        kit.start("REQ-032 A1")
        val clock = kit.kitUseClock("REQ-032 A1")
        val p = kitBoardWithPiecesATurnAndTime("REQ-032 A1", clock)
        val before = kitBoardView()
        val counts = kit.kitFold()

        kit.openSettingsByTouch("REQ-032 A1")
        assertEquals("the settings open over the current puzzle: `play-screen` still shows ${p.id}", p.id, kit.shownId())
        kit.kitTouchThroughWindow("REQ-032 A1: a touch on the open settings screen")
        val during = kit.kitMoveClock(clock, KIT_K)
        assertEquals("while the settings are open the puzzle's time stays put (REQ-032 rule 2)", before.seconds, during.puzzle)
        assertEquals("positive control: the same seconds are play time (E3; C1)", counts.allTime + KIT_K, during.allTime)

        kit.closeSettingsByDone("REQ-032 A1")
        assertEquals("opening and closing the settings changes nothing on the board", before, kitBoardView())

        kit.kitTouchThroughWindow("REQ-032 A1: a touch after the closing")
        val after = kit.kitMoveClock(clock, 10)
        assertEquals("positive control: after the closing the puzzle's time counts again (REQ-030 rule 1: it resumes on return)", before.seconds + 10, after.puzzle)
    }

    @Test // REQ-032.A2
    fun noDifficultyControlIsPresent() {
        // REQ-032 A2: "No difficulty control is present." G-09: no difficulty level "appears in code, settings or UI". Oracle: the
        // controls on the open screen are exactly S-49's tag row ("Controls: `settings` (the scrim), `settings-done`,
        // `settings-timer` and `settings-sound` …, `settings-reset`, `reset-erase`, `reset-cancel`"): every node with a click
        // action, a long-click action or a toggle state, at every scroll position of the body, is one of them (an untagged one
        // counts as its nearest tagged ancestor only when it spans that control's bounds, decisions.md 2026-10-17 "Two readings
        // of the device kit's closed-world checks" (2)); `dev-*` nodes are the debug aid's (REQ-046), outside the player's
        // screen. A difficulty control, a picker or a third switch would be one more control, so the scan can see it; positive
        // control: it sees S-49's five controls of the closed question, and the confirmation's two once Reset is pressed. The Play
        // screen under the layer is a New puzzle, which has no foundation control of its own (S-22: Restart only In progress,
        // the solved row only Solved).
        kit.start("REQ-032 A2")
        val p = kit.firstNew(PuzzleKind.FULL, "REQ-032 A2: a New full puzzle")
        kit.openByController(p.id, "REQ-032 A2: ${p.id} New")
        kit.openSettingsByTouch("REQ-032 A2")

        val closedQuestion = setOf(KitAppTag.SETTINGS, KitAppTag.SETTINGS_DONE, KitAppTag.SETTINGS_TIMER, KitAppTag.SETTINGS_SOUND, KitAppTag.SETTINGS_RESET)
        val seen = LinkedHashSet<String>()
        seen += kitControlsShown()
        for (tag in KitAppTag.SETTINGS_BODY_ORDER) {
            kit.scrollIntoView(tag, "REQ-032 A2")
            seen += kitControlsShown()
        }
        assertEquals("every control on the settings screen, at every scroll position, is one of S-49's (no difficulty control: REQ-032 A2)", closedQuestion, seen)

        kit.scrollIntoView(KitAppTag.SETTINGS_RESET, "REQ-032 A2: the question")
        kit.pressCentre(KitAppTag.SETTINGS_RESET)
        kit.awaitExists(KitAppTag.RESET_QUESTION, "REQ-032 A2: Reset asks its question")
        val withQuestion = kitControlsShown()
        val expected = closedQuestion - KitAppTag.SETTINGS_RESET + KitAppTag.RESET_ERASE + KitAppTag.RESET_CANCEL
        assertTrue(
            "with the question shown, the controls are S-49's: $withQuestion (Reset itself may stay or go: E2 \"becomes the question\")",
            withQuestion.containsAll(expected) && (withQuestion - expected - KitAppTag.SETTINGS_RESET).isEmpty(),
        )
        kit.pressCentre(KitAppTag.RESET_CANCEL)
        kit.awaitGone(KitAppTag.RESET_QUESTION, "REQ-032 A2: Cancel")
        kit.closeSettingsByDone("REQ-032 A2")
    }

    // ── REQ-009 and REQ-049: the free note and the privacy text ───────────────────────────────────────────

    @Test // REQ-009.A1
    fun theSettingsScreenShowsTheFreeNoteInFull() {
        // REQ-009 A1: "The settings screen shows the note in full." Statement: "WHEN the settings screen is open, the game SHALL
        // show the note: "Enjoy, it's absolutely free. No ads, no purchases, no network. Your play time stays on this device."".
        // Oracle, on an English device (G-10): with the screen opened by the gear, `free-note` scrolled into view holds exactly
        // REQ-009's sentence as its one text, lies wholly in view inside `settings-sheet`, and its text is drawn whole: every
        // character on its lines, none ellipsized, every line inside its box (the 2026-10-21 reading of "drawn whole", the
        // `TopBarFitKitTest` fix).
        kit.start("REQ-009 A1")
        kitAssertEnglish("REQ-009 A1")
        kit.openSettingsByTouch("REQ-009 A1")
        kitAssertShownInFull(KitAppTag.FREE_NOTE, KIT_FREE_NOTE, "REQ-009 A1: the free note")
        kit.closeSettingsByDone("REQ-009 A1")
    }

    @Test // REQ-049.A1
    fun theSettingsScreenShowsThePrivacyTextInFullBelowTheFreeNote() {
        // REQ-049 A1: "The settings screen shows the privacy text in full, below the free note." Rule 1: "The text says, in full:
        // "Privacy: this game collects no data. …"". Oracle, on an English device: `privacy-text` holds exactly rule 1's text, is
        // shown in full as the free note is (REQ-009 A1's oracle above), and its box starts at or below the free note's bottom
        // (both read in one layout of the scrolling body).
        kit.start("REQ-049 A1")
        kitAssertEnglish("REQ-049 A1")
        kit.openSettingsByTouch("REQ-049 A1")
        kitAssertShownInFull(KitAppTag.PRIVACY_TEXT, KIT_PRIVACY_TEXT, "REQ-049 A1: the privacy text")
        val note = kit.unclippedBoundsOf(kit.node(KitAppTag.FREE_NOTE))
        val privacy = kit.unclippedBoundsOf(kit.node(KitAppTag.PRIVACY_TEXT))
        assertTrue("the privacy text $privacy lies below the free note $note (REQ-049 statement and A1)", privacy.top >= note.bottom - 1f)
        kit.closeSettingsByDone("REQ-049 A1")
    }

    // ── REQ-029 A2: the play times on the screen ─────────────────────────────────────────────────────────

    @Test // REQ-029.A2
    fun theSettingsScreenShowsTodaysAndAllTimePlayTime() {
        // REQ-029 A2: "The settings screen shows today's and all-time play time." Statement: each active second is added "to
        // today's and to the all-time play time, and the settings screen SHALL show both"; rule 1: "Today's time restarts at local
        // midnight"; rule 2: m:ss below one hour (C2: from one hour "h h m min"; E10). E4a's oracle, which can fail: the kit clock
        // counts M = 45 s on one date and N = 25 s on the next, each after a touch through the window; the screen, opened by the
        // gear, shows today = N and all-time = the earlier all-time + M + N — both above 0 and different, so rows that showed
        // one value twice, or no date roll, fail. With the screen open, a touch on `settings-title` and K = 15 s move both rows
        // by K s (E5: "shown live"), while the In-progress puzzle's time stays put (REQ-032 rule 2).
        kit.start("REQ-029 A2")
        val p = kit.firstNew(PuzzleKind.FULL, "REQ-029 A2: a New full puzzle")
        kit.openByController(p.id, "REQ-029 A2: ${p.id} New")
        val order = kitAppLockOrder(p) ?: throw AssertionError("kit: no lock order for ${p.id}")
        kit.placeByController(order.first(), "REQ-029 A2: ${p.id} In progress")
        val clock = kit.kitUseClock("REQ-029 A2")
        val start = kit.kitFold()

        kit.kitTouchThroughWindow("REQ-029 A2: a touch on the first date")
        kit.kitMoveClock(clock, KIT_M)
        kit.onMain { clock.date = clock.date.plusDays(1) }
        kit.kitTouchThroughWindow("REQ-029 A2: a touch on the next date")
        val counted = kit.kitMoveClock(clock, KIT_N)
        assertEquals("fixture: today's play time restarted on the next date and holds its N s (REQ-029 rule 1)", KIT_N, counted.today)
        assertEquals("fixture: the all-time play time grew by M + N (REQ-029 statement)", start.allTime + KIT_M + KIT_N, counted.allTime)

        kit.openSettingsByTouch("REQ-029 A2")
        val en = kit.contextFor("en")
        kitAwaitRowValue(KitAppTag.PLAY_TIME_TODAY, kit.kitTimeText(KIT_N, en), "REQ-029 A2: today's row")
        kitAwaitRowValue(KitAppTag.PLAY_TIME_ALL, kit.kitTimeText(counted.allTime, en), "REQ-029 A2: the all-time row")
        val today = kit.mergedTexts(KitAppTag.PLAY_TIME_TODAY).last()
        val all = kit.mergedTexts(KitAppTag.PLAY_TIME_ALL).last()
        assertNotEquals("today's and the all-time play time are shown as two different values", today, all)
        assertTrue("both rows lie in `settings-sheet`", listOf(KitAppTag.PLAY_TIME_TODAY, KitAppTag.PLAY_TIME_ALL).all { kitAppWithin(kit.bounds(it), kit.bounds(KitAppTag.SETTINGS_SHEET), 1f) })

        val puzzleBefore = kit.onController { timeSeconds }
        kit.kitTouchThroughWindow("REQ-029 A2: a touch on `settings-title`")
        val moved = kit.kitMoveClock(clock, KIT_K)
        kitAwaitRowValue(KitAppTag.PLAY_TIME_TODAY, kit.kitTimeText(KIT_N + KIT_K, en), "REQ-029 A2: today's row, K s later")
        kitAwaitRowValue(KitAppTag.PLAY_TIME_ALL, kit.kitTimeText(counted.allTime + KIT_K, en), "REQ-029 A2: the all-time row, K s later")
        assertEquals("with the screen open, the In-progress puzzle's time stays put (REQ-032 rule 2)", puzzleBefore, moved.puzzle)
        kit.closeSettingsByDone("REQ-029 A2")
    }

    // ── REQ-031 A2 through the real switch ───────────────────────────────────────────────────────────────

    @Test // REQ-031.A2
    fun withTheSettingOnTheTimerIsShownAndCountsActiveSecondsOnly() {
        // REQ-031 A2: "With the setting on, the timer is shown and counts active seconds only." Rule 1: "Setting values: off
        // (default), on"; rule 2: "The timer shows active seconds of the current puzzle only (TYPE-005, REQ-030)". E4: the switch
        // writes through the controller and takes effect at once (N1). Oracle: an In-progress puzzle with no timer (the cleared
        // store's default); the timer switch turned on by touch on the open screen (its toggle state reads On); after Done, the
        // `timer` node is on the board; a window touch and 30 s show the puzzle's time 30 s later; a touch and 5 untouched minutes
        // (moved 10 s at a time, each folded) add exactly 60 s (TYPE-005: at most 60 s after the last touch). Positive control:
        // the switch turned off again by touch, the timer is gone. The class's restore puts the setting back as found (E4a).
        kit.start("REQ-031 A2")
        val p = kit.firstNew(PuzzleKind.FULL, "REQ-031 A2: a New full puzzle")
        kit.openByController(p.id, "REQ-031 A2: ${p.id} New")
        val order = kitAppLockOrder(p) ?: throw AssertionError("kit: no lock order for ${p.id}")
        kit.placeByController(order.first(), "REQ-031 A2: ${p.id} In progress")
        val clock = kit.kitUseClock("REQ-031 A2")
        assertFalse("fixture: the timer is off at the start (REQ-031 rule 1, the default)", kit.onController { timerShown })
        assertFalse("fixture: no `${PlayTags.TIMER}` node with the setting off", kit.exists(PlayTags.TIMER))

        kit.openSettingsByTouch("REQ-031 A2")
        kit.scrollIntoView(KitAppTag.SETTINGS_TIMER, "REQ-031 A2")
        assertEquals("fixture: the timer switch reads Off", ToggleableState.Off, kitToggle(KitAppTag.SETTINGS_TIMER))
        kit.pressCentre(KitAppTag.SETTINGS_TIMER)
        kit.await("REQ-031 A2: the switch reads On") { kitToggle(KitAppTag.SETTINGS_TIMER) == ToggleableState.On }
        assertTrue("the switch turned the setting on, at once (E4; N1)", kit.onController { timerShown })
        kit.closeSettingsByDone("REQ-031 A2")

        kit.awaitExists(PlayTags.TIMER, "REQ-031 A2: with the setting on, the timer is shown")
        val en = kit.contextFor("en")
        val base = kit.kitFold()
        kit.kitTouchThroughWindow("REQ-031 A2: a touch")
        val played = kit.kitMoveClock(clock, 30)
        assertEquals("fixture: 30 s of active play on the puzzle", base.puzzle + 30, played.puzzle)
        kit.await("REQ-031 A2: the timer shows the puzzle's time, 30 s later") { kit.text(PlayTags.TIMER) == kit.kitTimeText(played.puzzle, en) }

        kit.kitTouchThroughWindow("REQ-031 A2: the touch before 5 untouched minutes")
        var last = played
        repeat(30) { last = kit.kitMoveClock(clock, 10) }
        assertEquals("a touch, then 5 untouched minutes: exactly 60 active seconds (TYPE-005; REQ-031 rule 2)", played.puzzle + 60, last.puzzle)
        kit.await("REQ-031 A2: the timer shows only the active seconds") { kit.text(PlayTags.TIMER) == kit.kitTimeText(last.puzzle, en) }

        kit.openSettingsByTouch("REQ-031 A2: the positive control")
        kit.scrollIntoView(KitAppTag.SETTINGS_TIMER, "REQ-031 A2")
        kit.pressCentre(KitAppTag.SETTINGS_TIMER)
        kit.await("REQ-031 A2: the switch reads Off again") { kitToggle(KitAppTag.SETTINGS_TIMER) == ToggleableState.Off }
        kit.closeSettingsByDone("REQ-031 A2: the positive control")
        kit.awaitGone(PlayTags.TIMER, "REQ-031 A2: positive control: with the setting off, no timer")
    }

    // ── Pins ─────────────────────────────────────────────────────────────────────────────────────────────

    // Pin (no token): REQ-032 rule 1 ("Contents, in this order: timer (REQ-031), sound (REQ-033), play time and best times
    // (REQ-029, REQ-030), reset (REQ-034), a short how-to-play text, the free note (REQ-009) and the privacy text (REQ-049)");
    // E2 (the body "follows REQ-032's order … the privacy text; the licence line"); WO-007 scope note ("The licence line … built
    // below the privacy text"). Oracle: the body's nodes, every one of them composed at once (S-49 "Body (scrolls)"), read in one
    // layout: their tops strictly increase in that order.
    @Test
    fun theContentsFollowTheOrderOfTheRule() {
        kit.start("REQ-032 rule 1")
        kit.openSettingsByTouch("REQ-032 rule 1")
        kit.scrollIntoView(KitAppTag.SETTINGS_TIMER, "REQ-032 rule 1: the top of the body")
        val missing = KitAppTag.SETTINGS_BODY_ORDER.filter { !kit.exists(it) }
        assertTrue("every item of the body is composed at once (S-49: one scrolling body); missing: $missing", missing.isEmpty())
        val tops = KitAppTag.SETTINGS_BODY_ORDER.map { it to kit.unclippedBoundsOf(kit.node(it)).top }
        for (i in 0 until tops.size - 1) {
            assertTrue("`${tops[i].first}` (top ${tops[i].second}) comes before `${tops[i + 1].first}` (top ${tops[i + 1].second}) (REQ-032 rule 1)", tops[i].second < tops[i + 1].second)
        }
        kit.closeSettingsByDone("REQ-032 rule 1")
    }

    // Pin (no token): REQ-032 statement ("WHEN it is closed, the game SHALL return to the same puzzle state") and rule 3 ("The
    // Android back button closes the settings screen"); E2 ("Closing: Done, Back, or a tap on the scrim"); S-49 (the scrim "covers
    // the whole window; its `clickable` calls `closeSettings()`"; "Back: `BackHandler(settingsOpen) { closeSettings() }`"). Three
    // legs, each from the screen opened by the gear: Done, the system back, and a tap on the scrim beside the sheet (a point
    // inside `settings`, outside `settings-sheet`, read on the screen in use). Each closes the screen — `settings-sheet` gone,
    // `settingsOpen` false — with the activity still resumed and the same puzzle in the same state.
    @Test
    fun doneBackAndATapOnTheScrimEachCloseTheSettings() {
        kit.start("REQ-032 rule 3")
        val id = kit.shownId()
        val state = kit.shownState()

        kit.openSettingsByTouch("Done")
        kit.closeSettingsByDone("Done")
        kitAssertSamePuzzle(id, state, "Done")

        kit.openSettingsByTouch("Back")
        kit.bounded("the system back") { Espresso.pressBack() }
        kit.awaitGone(KitAppTag.SETTINGS_SHEET, "Back closes the settings screen (REQ-032 rule 3)")
        assertFalse("Back: settingsOpen is false", kit.onController { settingsOpen })
        assertEquals("Back: the activity is still resumed", Lifecycle.State.RESUMED, kit.scenarioState())
        kitAssertSamePuzzle(id, state, "Back")

        kit.openSettingsByTouch("the scrim")
        val sheet = kit.bounds(KitAppTag.SETTINGS_SHEET)
        val scrim = kit.bounds(KitAppTag.SETTINGS)
        val at = Offset(sheet.left - kit.dp(8f), sheet.center.y)
        assertTrue("fixture: the point $at lies on the scrim $scrim, beside the sheet $sheet (16 dp margins, E2)", scrim.contains(at) && !sheet.contains(at) && at.x > 0f)
        kit.tap(at, "a tap on the scrim beside the sheet")
        kit.awaitGone(KitAppTag.SETTINGS_SHEET, "a tap on the scrim closes the settings screen (E2)")
        assertFalse("the scrim: settingsOpen is false", kit.onController { settingsOpen })
        kitAssertSamePuzzle(id, state, "the scrim")
    }

    // Pin (no token): E1 and S-49's strings row (the licence line "Open source, GPL-3.0 — source code:
    // github.com/jamisuni/TangramPlaytime"; WO-007 Q8: "plain text, never a link"); REQ-049 rule 2 ("The text is plain text, not a
    // link"). On an English device: `licence-line` holds exactly that line and is shown in full; neither it nor `privacy-text`
    // carries a link annotation in its text or a click action on itself or inside it.
    @Test
    fun theLicenceLineIsShownInFullAndItAndThePrivacyTextArePlainText() {
        kit.start("the licence line")
        kitAssertEnglish("the licence line")
        kit.openSettingsByTouch("the licence line")
        kitAssertShownInFull(KitAppTag.LICENCE_LINE, KIT_LICENCE_LINE, "the licence line")
        for (tag in listOf(KitAppTag.LICENCE_LINE, KitAppTag.PRIVACY_TEXT)) kitAssertPlainText(tag)
        kit.closeSettingsByDone("the licence line")
    }

    // Pin (no token): REQ-032 rule 1 ("a short how-to-play text"); the pending `chg-draft-req032-how-to-play-text.md` (WO-007 Q8:
    // "the how-to-play text written by the build, at most 60 words"); S-49 (the body holds "how to play"). Oracle: `how-to-play`,
    // scrolled into view, is wholly in view inside the sheet and shows at least one text, each of at most 60 words. Its wording
    // is a proof-reading item (`review-notes.md`), not asserted.
    @Test
    fun theHowToPlayTextIsShown() {
        kit.start("the how-to-play text")
        kit.openSettingsByTouch("the how-to-play text")
        kit.scrollIntoView(KitAppTag.HOW_TO_PLAY, "the how-to-play text")
        assertTrue("`how-to-play` is wholly in view", kit.wholeInView(KitAppTag.HOW_TO_PLAY))
        assertTrue("`how-to-play` lies inside the sheet", kitAppWithin(kit.bounds(KitAppTag.HOW_TO_PLAY), kit.bounds(KitAppTag.SETTINGS_SHEET), 1f))
        val texts = kit.mergedTexts(KitAppTag.HOW_TO_PLAY).filter { it.isNotBlank() }
        assertTrue("`how-to-play` shows a text", texts.isNotEmpty())
        for (t in texts) {
            val words = t.trim().split(Regex("\\s+")).size
            assertTrue("a how-to-play text of $words words, at most $KIT_HOW_TO_MAX_WORDS (WO-007 Q8): \"$t\"", words <= KIT_HOW_TO_MAX_WORDS)
        }
        kit.closeSettingsByDone("the how-to-play text")
    }

    // Pin (no token): REQ-009 rule 1 ("The note appears in the settings screen only, never over a puzzle"); the must-never of design
    // §3 ("the free note never shows over a puzzle"). On an English device: with the settings closed, on the Play screen New, In
    // progress and solved, and on the grid overview, no `free-note` node exists and no text shown is the note. Positive control:
    // with the screen open the note is there, so the read can see it.
    @Test
    fun theFreeNoteNeverShowsOverAPuzzle() {
        kit.start("REQ-009 rule 1")
        kitAssertEnglish("REQ-009 rule 1")
        fun noNote(where: String) {
            assertFalse("$where: no `free-note` node with the settings closed", kit.exists(KitAppTag.FREE_NOTE))
            assertFalse("$where: no text shown is the free note", KIT_FREE_NOTE in kit.shownTexts())
        }
        val p = kit.firstNew(PuzzleKind.FULL, "REQ-009 rule 1: a New full puzzle")
        kit.openByController(p.id, "REQ-009 rule 1: ${p.id} New")
        noNote("the Play screen, New")
        val order = kitAppLockOrder(p) ?: throw AssertionError("kit: no lock order for ${p.id}")
        kit.placeByController(order.first(), "REQ-009 rule 1: In progress")
        noNote("the Play screen, In progress")
        kit.solveByController("REQ-009 rule 1: solved")
        kit.awaitSolvedRow("REQ-009 rule 1: the solved view")
        noNote("the solved view")
        kit.onController { openOverview() }
        kit.awaitExists(KitAppTag.OVERVIEW, "REQ-009 rule 1: the grid")
        noNote("the grid overview")
        kit.onController { closeOverview() }
        kit.awaitGone(KitAppTag.OVERVIEW, "REQ-009 rule 1: the grid closed")

        kit.openSettingsByTouch("REQ-009 rule 1: the positive control")
        kit.scrollIntoView(KitAppTag.FREE_NOTE, "REQ-009 rule 1: the positive control")
        assertTrue("positive control: with the settings open the note is shown", KIT_FREE_NOTE in kit.shownTexts())
        kit.closeSettingsByDone("REQ-009 rule 1")
        noNote("the Play screen after the settings closed")
    }

    // Pin (no token): E3 ("`settingsOpen` survives a recreation, because the controller holds it. The reset confirmation does
    // not; it reads as cancelled (REQ-034 A2)" — the row's ID written here without its dot). Oracle: the screen
    // opened by the gear and Reset pressed (the question shown), then the activity recreated (`ActivityScenario.recreate()`): the
    // new activity shows the settings screen (`settingsOpen` still true), without the question and with Reset back, and every
    // puzzle's state is as before (nothing was erased).
    @Test
    fun theSettingsStayOpenAcrossARecreationAndTheConfirmationReadsAsCancelled() {
        kit.start("E3: a recreation")
        val ids = kit.library.puzzles.map { it.id }
        val states = kit.onController { ids.associateWith { stateOf(it) } }
        kit.openSettingsByTouch("E3")
        kit.scrollIntoView(KitAppTag.SETTINGS_RESET, "E3")
        kit.pressCentre(KitAppTag.SETTINGS_RESET)
        kit.awaitExists(KitAppTag.RESET_QUESTION, "E3: Reset asks its question")

        kit.recreateActivity("E3: the recreation with the question shown")
        kit.awaitExists(KitAppTag.SETTINGS_SHEET, "E3: the settings screen in the new activity")
        assertTrue("settingsOpen survives the recreation (E3)", kit.onController { settingsOpen })
        assertFalse("the question does not survive: it reads as cancelled (E3)", kit.exists(KitAppTag.RESET_QUESTION))
        kit.scrollIntoView(KitAppTag.SETTINGS_RESET, "E3: after the recreation")
        assertTrue("…and Reset is back", kit.exists(KitAppTag.SETTINGS_RESET))
        assertEquals("nothing was erased: every puzzle's state is as before", states, kit.onController { ids.associateWith { stateOf(it) } })
        kit.closeSettingsByDone("E3")
    }

    // Pin (no token): E5 ("One row per puzzle with a stored best, in ‹ › order: the puzzle's title in the game's language, then its
    // time"; "A puzzle the player never solved has no row"; checkpoint candidate 2, its default); REQ-030 rule 3 (the best time
    // "in the settings list"); C2 (the time's format). On an English device, two full puzzles solved through S-2, the later one in
    // ‹ › order first, each after a touch and a known number of seconds on the kit clock (so each best is the seconds counted on
    // it, S-2Δ3); then the screen opened by the gear: each has one `best-time-<id>` row showing [its English title, its best
    // time], in ‹ › order, and a third full puzzle, never solved, has none.
    @Test
    fun theBestTimesListShowsEachSolvedPuzzleInListOrder() {
        kit.start("E5: the best times")
        kitAssertEnglish("E5: the best times")
        val clock = kit.kitUseClock("E5: the best times")
        val ids = kit.library.puzzles.map { it.id }
        val states = kit.onController { ids.associateWith { stateOf(it) } }
        val fulls = kit.library.puzzles.filter { it.kind == PuzzleKind.FULL && states[it.id] == PuzzleState.NEW }
        if (fulls.size < 3) throw AssertionError("kit: precondition: three New full puzzles (a cleared store)")
        val (early, late, never) = Triple(fulls[0], fulls[1], fulls[2])
        val bests = LinkedHashMap<String, Int>()
        for ((p, seconds) in listOf(late to 40, early to 25)) {
            kit.openByController(p.id, "E5: ${p.id}")
            val order = kitAppLockOrder(p) ?: throw AssertionError("kit: no lock order for ${p.id}")
            kit.placeByController(order.first(), "E5: ${p.id} In progress")
            kit.kitTouchThroughWindow("E5: a touch on ${p.id}")
            kit.kitMoveClock(clock, seconds)
            kit.solveByController("E5: ${p.id} solved")
            bests[p.id] = kit.onController { bestSeconds } ?: throw AssertionError("kit: ${p.id} solved by S-2 has no best time")
        }
        kit.openSettingsByTouch("E5")
        val en = kit.contextFor("en")
        for (p in listOf(early, late)) {
            val tag = KitAppTag.bestTimeRow(p.id)
            kit.scrollIntoView(tag, "E5: ${p.id}'s row")
            assertEquals("${p.id}'s row shows its English title and its best time (E5; C2)", listOf(kitAppTitle(p, "en"), kit.kitTimeText(bests.getValue(p.id), en)), kit.mergedTexts(tag))
        }
        assertTrue(
            "the rows are in ‹ › order (E5): ${early.id} above ${late.id}",
            kit.unclippedBoundsOf(kit.node(KitAppTag.bestTimeRow(early.id))).top < kit.unclippedBoundsOf(kit.node(KitAppTag.bestTimeRow(late.id))).top,
        )
        assertFalse("${never.id}, never solved, has no row (E5)", kit.exists(KitAppTag.bestTimeRow(never.id)))
        kit.closeSettingsByDone("E5")
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────────────────

    /** The board as the nodes show it, and the puzzle's time (S-1Δ3): what REQ-032 A1 and design §3's must-never name. */
    private data class KitBoardView(
        val puzzleId: String,
        val state: PuzzleState,
        val placed: Set<Placement>,
        val trayTurns: Map<PieceId, Pair<Turn, Boolean>>,
        val seconds: Int,
    )

    private fun kitBoardView(): KitBoardView {
        val placed = PieceId.entries.filter { kit.exists(PlayTags.boardPiece(it)) }.map {
            val tag = PlayTags.boardPiece(it)
            Placement(kit.key(tag, PlaySemantics.PieceKey), kit.key(tag, PlaySemantics.TurnKey), kit.key(tag, PlaySemantics.MirroredKey), kit.key(tag, PlaySemantics.CentreKey))
        }.toSet()
        val tray = PieceId.entries.filter { kit.exists(PlayTags.trayPiece(it)) }.associateWith {
            kit.key(PlayTags.trayPiece(it), PlaySemantics.TurnKey) to kit.key(PlayTags.trayPiece(it), PlaySemantics.MirroredKey)
        }
        return KitBoardView(kit.shownId(), kit.shownState(), placed, tray, kit.onController { timeSeconds })
    }

    /**
     * REQ-032 A1's board: a New full puzzle, two pieces placed through S-2, a tray piece turned by one tap (REQ-016), then a window
     * touch and 20 s on the kit clock (its time counted, In progress).
     */
    private fun kitBoardWithPiecesATurnAndTime(what: String, clock: KitAppClock): PuzzleDefinition {
        val p = kit.firstNew(PuzzleKind.FULL, "$what: a New full puzzle") { it.pieces.size == 7 }
        kit.openByController(p.id, "$what: ${p.id} New")
        val order = kitAppLockOrder(p) ?: throw AssertionError("kit: no lock order for ${p.id}")
        order.take(2).forEach { kit.placeByController(it, "$what: two pieces placed") }
        val turned = kit.onController { trayPieces.first() }
        val was = kit.key(PlayTags.trayPiece(turned), PlaySemantics.TurnKey)
        kit.tapTrayPiece(turned, 1)
        assertEquals("fixture: $turned turned one step in the tray (REQ-016)", was.next(), kit.key(PlayTags.trayPiece(turned), PlaySemantics.TurnKey))
        kit.kitTouchThroughWindow("$what: a touch on the board")
        val counted = kit.kitMoveClock(clock, 20)
        assertTrue("fixture: the puzzle's time counted (${counted.puzzle} s)", counted.puzzle >= 20)
        assertEquals("fixture: ${p.id} In progress", PuzzleState.IN_PROGRESS, kit.shownState())
        return p
    }

    /** G-10: the REQ texts are English; the device must be English for a text oracle (the play-through's precondition). */
    private fun kitAssertEnglish(what: String) {
        assertEquals("$what reads the app's English texts (G-10: English is the Contract wording); the device's list is ${kit.deviceLocales().toLanguageTags()}", "en", kit.deviceLocales()[0].language)
    }

    /** The same puzzle in the same state after a closing (REQ-032 statement). */
    private fun kitAssertSamePuzzle(id: String, state: PuzzleState, what: String) {
        assertEquals("$what: the same puzzle", id, kit.shownId())
        assertEquals("$what: the same state", state, kit.shownState())
    }

    /** The toggle state of a switch row (S-49: "whole-row `toggleable`, Role.Switch"); none fails loudly. */
    private fun kitToggle(tag: String): ToggleableState =
        kit.node(tag).config.getOrNull(SemanticsProperties.ToggleableState) ?: throw AssertionError("kit: `$tag` carries no toggle state (S-49: a toggleable row)")

    /** Waits until the merged row [tag] ("[label, value]", S-49) shows [value] as its value, failing loudly at the timeout. */
    private fun kitAwaitRowValue(tag: String, value: String, what: String) {
        kit.await("$what shows $value (now ${runCatching { kit.mergedTexts(tag) }.getOrNull()})") { kit.mergedTexts(tag).lastOrNull() == value }
    }

    /**
     * The controls shown now: every node with a click or long-click action or a toggle state, by its tag or, untagged, by its
     * nearest tagged ancestor when it spans that ancestor's bounds (±1 px); anything else is named as a stray. `dev-*` nodes are
     * left out (REQ-046's aid; REQ-037 rule 3).
     */
    private fun kitControlsShown(): Set<String> {
        val out = LinkedHashSet<String>()
        for (n in kit.allNodes()) {
            val acts = SemanticsActions.OnClick in n.config || SemanticsActions.OnLongClick in n.config || SemanticsProperties.ToggleableState in n.config
            if (!acts) continue
            val tag = n.config.getOrNull(SemanticsProperties.TestTag)
            val name = if (tag != null) tag else {
                var a = n.parent
                while (a != null && a.config.getOrNull(SemanticsProperties.TestTag) == null) a = a.parent
                val owner = a?.config?.getOrNull(SemanticsProperties.TestTag)
                if (owner != null && kitSameBox(kit.boundsOf(a), kit.boundsOf(n))) owner else "(untagged) at ${kit.boundsOf(n)}, nearest tagged ancestor `$owner`"
            }
            if (!name.startsWith(KIT_APP_DEV_PREFIX)) out += name
        }
        return out
    }

    private fun kitSameBox(a: Rect, b: Rect): Boolean =
        kotlin.math.abs(a.left - b.left) <= 1f && kotlin.math.abs(a.top - b.top) <= 1f && kotlin.math.abs(a.right - b.right) <= 1f && kotlin.math.abs(a.bottom - b.bottom) <= 1f

    /**
     * "Shows … in full", read on the screen: [tag] scrolled into view holds exactly [text] as its one text, lies wholly in view
     * inside `settings-sheet`, and its text is drawn whole (2026-10-21 "The `TopBarFitKitTest` fix is accepted …": not
     * ellipsized, every character on its lines, every drawn line inside its box).
     */
    private fun kitAssertShownInFull(tag: String, text: String, what: String) {
        kit.scrollIntoView(tag, what)
        assertEquals("$what: `$tag` holds exactly the text", listOf(text), kit.mergedTexts(tag))
        assertTrue("$what: `$tag` is wholly in view", kit.wholeInView(tag))
        assertTrue("$what: `$tag` ${kit.bounds(tag)} lies inside `settings-sheet` ${kit.bounds(KitAppTag.SETTINGS_SHEET)}", kitAppWithin(kit.bounds(tag), kit.bounds(KitAppTag.SETTINGS_SHEET), 1f))
        val node = kitTextNodeUnder(kit.node(tag), text, what)
        val layout = kitLayoutOf(node, what)
        val last = layout.lineCount - 1
        assertTrue("$what: its text is laid out (${layout.lineCount} lines)", layout.lineCount >= 1)
        assertTrue("$what: no line is ellipsized", (0..last).none { layout.isLineEllipsized(it) })
        assertEquals("$what: every character is on its lines", text.length, layout.getLineEnd(last, visibleEnd = true))
        for (i in 0..last) {
            assertTrue("$what: line $i lies inside its box (${layout.getLineLeft(i)}..${layout.getLineRight(i)} of ${layout.size.width})", layout.getLineLeft(i) >= -0.5f && layout.getLineRight(i) <= layout.size.width + 0.5f)
        }
        assertTrue("$what: the last line's bottom ${layout.getLineBottom(last)} lies inside the box's ${layout.size.height}", layout.getLineBottom(last) <= layout.size.height + 0.5f)
    }

    /** Plain text (REQ-049 rule 2; WO-007 Q8 for the licence line): no link annotation, no click action on the node or inside it. */
    private fun kitAssertPlainText(tag: String) {
        kit.scrollIntoView(tag, "plain text")
        val n = kit.node(tag)
        val links = n.config.getOrNull(SemanticsProperties.Text).orEmpty().flatMap { it.getLinkAnnotations(0, it.length) }
        assertTrue("`$tag` carries no link annotation: $links", links.isEmpty())
        fun walk(x: SemanticsNode): Boolean =
            SemanticsActions.OnClick in x.config || SemanticsActions.OnLongClick in x.config || x.children.any { walk(it) }
        assertFalse("`$tag` has no click action on itself or inside it (plain text, not a link)", walk(n))
        val merged = kit.bounded("reading `$tag` merged") { composeRule.onNode(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNode() }
        val mergedLinks = merged.config.getOrNull(SemanticsProperties.Text).orEmpty().flatMap { it.getLinkAnnotations(0, it.length) }
        assertTrue("`$tag` (merged) carries no link annotation: $mergedLinks", mergedLinks.isEmpty())
    }

    /** The one node at or under [n] (unmerged tree) that shows [text] and carries a text layout (S-14Δ merged, inert text nodes). */
    private fun kitTextNodeUnder(n: SemanticsNode, text: String, what: String): SemanticsNode {
        val found = ArrayList<SemanticsNode>()
        fun walk(x: SemanticsNode) {
            val shown = x.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            if (shown == text && x.config.getOrNull(SemanticsActions.GetTextLayoutResult) != null) found += x
            x.children.forEach { walk(it) }
        }
        walk(n)
        if (found.size != 1) throw AssertionError("kit: $what: ${found.size} text nodes at or under it show the text with a text layout, expected one")
        return found.single()
    }

    /** The node's text layout through its `GetTextLayoutResult` action; none fails loudly. */
    private fun kitLayoutOf(n: SemanticsNode, what: String): TextLayoutResult {
        val action = n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: throw AssertionError("kit: $what has no text layout to read")
        val out = ArrayList<TextLayoutResult>()
        kit.onMain { action(out) }
        return out.firstOrNull() ?: throw AssertionError("kit: $what returned no text layout")
    }
}
