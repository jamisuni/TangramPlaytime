// Acceptance kit (WO-007, TASK-052; kit stage 1) — the settings state, the pause and Reset all progress, on the JVM:
// PlayController (designs/WO-007-design.md §2 S-1Δ4 reads, S-2Δ4 actions) over Persistence (S-19Δ2) on the kit's recording
// in-memory store double (KitStore.kt `KitProgressStore`), with the real ActiveTime (S-45, S-45Δ) behind IActiveTime (S-44,
// S-44Δ), the real engine and the shipped puzzles.
//
// - No token (design §2 "Tokens": REQ-032, REQ-034 and the switches' lines are screen lines, `app/`'s device kit's). Every
//   test is a pin of a seam row, a REQ rule, a TYPE rule or a decisions.md row; prose names a line without the dot
//   (traceability-rules.md §2: "A token is a coverage claim"). The brief's labels for the decisions.md rows of 2026-10-21:
//   - E3 "The settings screen sits in the activity's window, pauses the puzzle time and is the only open overlay.";
//   - E4 "The two switches write through `PlayController` and `Persistence` and take effect at once. …";
//   - E5 "Play time and best times on the settings screen." (checkpoint candidate 2; its default is pinned);
//   - E6 "Reset all progress, in order, and where the player is afterwards." (checkpoint candidate 3; its default is
//     pinned: "the player stays on the open puzzle. The settings screen stays open");
//   - and C1, 2026-10-20 "What counts on each screen." ("WO-007's settings screen: play only").
// - The seams (design §2):
//   - S-1Δ4: "`settingsOpen`: false at start"; "`settings`: the controller's own `mutableStateOf`, seeded from
//     `persistence?.settings ?: Settings()`"; "`timerShown`: `settings.timerShown`"; "`bestTimes: List<BestTime(puzzle,
//     seconds)>`: listed puzzles with a stored best, in ‹ › order; the open puzzle's comes from `bestSeconds`. Refreshed by
//     `openSettings` and `resetAll`".
//   - S-2Δ4: "`openSettings` / `closeSettings`: fold first, then set the flag. The fold predicate gains `&& !settingsOpen`";
//     "`setTimerShown` / `setSoundOn`: `settings = copy(…)`, then `persistence?.saveSettings`"; "`resetAll()`: (1) fold;
//     (2) `time?.resetPlayTimes()`; (3) `persistence?.resetAll()`, else `memory.clear()`; (4) `load(puzzle)`: New, and
//     nothing saved but last-opened; (5) re-read `playTimes`; `unsavedPlaySeconds = 0`; `bestTimes` empty. It never calls
//     `open` and emits no event".
//   - S-19Δ2: "`var settings`: read under the lock; private set"; "`saveSettings(s)`: updates the cache, then queues
//     `Write.Settings`"; "`resetAll()`: queues `Write.Reset` (`store.resetAll()`), empties the cache, sets `times = NONE`;
//     the settings and last-opened are kept. All in one FIFO".
//   - design §3 "Reset order": "a pre-reset save queued after `Write.Reset` would restore that puzzle in the store while the
//     cache says New. `open()`'s leaving `keep` would make such a save, so `resetAll` never calls `open`".
// - `BestTime`'s field types are not named by S-1Δ4 ("BestTime(puzzle, seconds)"): the kit reads `puzzle` as a listed
//   puzzle (its definition or its id) and `seconds` as a number, and fails loudly on anything else (`kitIdOf`, `kitSecondsOf`).
// - Time (design §2 "Kit doors", JVM: "`KitStore`, the real `ActiveTime` and a kit clock"; the WO-008 pattern of
//   PlayTimeKitTest.kt, read, not edited): the kit owns its clock (KitStore.kt `KitClock`) and moves it; nothing sleeps. A
//   touch is reported to the kernel (`time.touched()`), a tick of the ticker is `countTime()`. Every action, touch and clock
//   move runs on the test's one thread (S-44 "One caller thread"). The store double's write latency (KitStore.kt
//   `writeLatencyMs`) is the writer's own delay, never the test thread's.
// - What the store holds is read after S-2Δ `flush()` (IProgressStore KDoc: "A save is complete when the call returns").
// - Every "nothing" line has its positive control in the same test. Fixtures discriminate: the reset's open puzzle is In
//   progress with placed pieces and time, other puzzles are Solved and In progress with best times, and the switches are both
//   away from their defaults; the best-time puzzles are seeded and solved in orders other than ‹ › order.
// - Same module: `internal` members (`Persistence.progress`, `Persistence.lastOpenedId`) are called directly. Helper names
//   carry a `kit` / `Kit` / `KIT_` prefix and are members of this class.
package app.tangramplaytime.play

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PlayTimes
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Settings
import app.tangramplaytime.play.devaid.DevAid
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class SettingsControllerKitTest {

    private val session = KitSession()

    @After
    fun closeSession() = session.close()

    private companion object {
        /** How long the store double holds a write the test slows (KitStore.kt `writeLatencyMs`). */
        const val KIT_SLOW_WRITE_MS = 100L
    }

    // ── Kit helpers ──────────────────────────────────────────────────────────────────────────────────

    /** A touch on the game, reported to the kernel (S-44 `touched()`; S-15Δ3 `AppGraph.touched()` forwards it there). */
    private fun kitTouch(t: KitTimed) = t.time.touched()

    /** One tick of the ticker: the fold (S-2Δ3 `countTime()`). */
    private fun kitTick(t: KitTimed) {
        t.c.countTime()
    }

    /** [seconds] of active play: a touch, the clock moved by at most 30 s (inside TYPE-005's 60 s), then a tick; repeated. */
    private fun kitPlay(t: KitTimed, seconds: Int) {
        var left = seconds
        while (left > 0) {
            val step = min(30, left)
            kitTouch(t)
            t.clock.forward(step)
            kitTick(t)
            left -= step
        }
    }

    /** A touch, then [seconds] on the clock, with no tick: the seconds wait in the kernel for the next fold (S-45 accrual). */
    private fun kitTouchAndWait(t: KitTimed, seconds: Int) {
        kitTouch(t)
        t.clock.forward(seconds)
    }

    /** The listed mini and warm-up puzzles with at least two pieces, in ‹ › order: quick to solve by play. */
    private fun kitQuick(): List<PuzzleDefinition> =
        kitLibrary.puzzles.filter { (it.kind == PuzzleKind.MINI || it.kind == PuzzleKind.WARMUP) && it.pieces.size >= 2 }

    /** The 0-based place of [id] in the ‹ › order (IPuzzleLibrary `puzzles`); an unlisted id is a fixture defect. */
    private fun kitIndexOf(id: String): Int = kitLibrary.puzzles.indexOfFirst { it.id == id }.also {
        if (it < 0) throw AssertionError("fixture defect: $id is not listed")
    }

    /**
     * Solves the open New puzzle [p] by the player's actions (KitPlay.kt `kitPlace`): its first drop (the drag from the tray
     * makes it In progress, TYPE-006), then [seconds] of play, then its other drops, the last of which solves it.
     */
    private fun kitSolveTaking(t: KitTimed, p: PuzzleDefinition, seconds: Int) {
        assertEquals("fixture: ${p.id} open", p.id, t.c.puzzle.id)
        assertEquals("fixture: ${p.id} New", PuzzleState.NEW, t.c.state)
        val plan = kitPlanOf(p)
        if (plan.size < 2) throw AssertionError("fixture defect: ${p.id} has a single piece")
        kitPlace(t.c, plan.first())
        kitPlay(t, seconds)
        plan.drop(1).forEach { kitPlace(t.c, it) }
        assertEquals("fixture: ${p.id} solved by the player", PuzzleState.SOLVED, t.c.state)
    }

    /** S-1Δ4 `BestTime.puzzle` as a puzzle id: a listed puzzle's definition or its id; anything else fails loudly. */
    private fun kitIdOf(puzzle: Any?): String = when (puzzle) {
        is PuzzleDefinition -> puzzle.id
        is String -> puzzle
        else -> throw AssertionError("S-1Δ4: BestTime.puzzle names a listed puzzle; it was $puzzle")
    }

    /** S-1Δ4 `BestTime.seconds` as whole seconds; anything but a number fails loudly. */
    private fun kitSecondsOf(seconds: Any?): Int =
        (seconds as? Number)?.toInt() ?: throw AssertionError("S-1Δ4: BestTime.seconds is a time in seconds; it was $seconds")

    /** S-1Δ4 `bestTimes` as (puzzle id, seconds) rows, in the controller's order. */
    private fun kitRows(c: PlayController): List<Pair<String, Int>> = c.bestTimes.map { kitIdOf(it.puzzle) to kitSecondsOf(it.seconds) }

    /** The rows S-1Δ4 names for [rows]: one per puzzle, in ‹ › order ("listed puzzles with a stored best, in ‹ › order"). */
    private fun kitExpected(vararg rows: Pair<PuzzleDefinition, Int>): List<Pair<String, Int>> =
        rows.sortedBy { kitIndexOf(it.first.id) }.map { it.first.id to it.second }

    /** Every listed puzzle New (TYPE-006), as KitStore.kt `kitStates` reports them. */
    private fun kitAllNew(): Map<String, PuzzleState> = kitLibrary.puzzles.associate { it.id to PuzzleState.NEW }

    /** The settings the store holds once every queued write has landed. */
    private fun kitStoredSettings(t: KitTimed): Settings {
        session.flush(t.run)
        return runBlocking { t.store.settings() }
    }

    /** A completed store write as text ("saveProgress shapes-warmup-1 IN_PROGRESS"), so a failing order reads plainly. */
    private fun kitWriteText(w: KitWrite): String =
        listOfNotNull(w.member, w.puzzleId, w.progress?.let { "${it.state} placed ${it.placed.size}" }).joinToString(" ")

    /** Everything on the board REQ-032 A1 and design §3's must-never name: KitPlay.kt's snapshot, the puzzle time and best. */
    private data class KitBoardView(
        val board: KitSnapshot,
        val timeSeconds: Int,
        val bestSeconds: Int?,
        val overviewOpen: Boolean,
        val celebration: Celebration?,
    )

    private fun kitView(c: PlayController) = KitBoardView(kitSnapshot(c), c.timeSeconds, c.bestSeconds, c.overviewOpen, c.celebration)

    /**
     * True when a snapshot-state object that [read] reads is among the state objects [write] writes, as Compose's snapshot
     * read and write observers report them (the objects a composition that ran [read] is recomposed for). The kit's own
     * harness check, that this can be true and can be false, is in the test.
     */
    private fun kitIsToldOf(read: () -> Unit, write: () -> Unit): Boolean {
        val reads = HashSet<Any>()
        Snapshot.observe(readObserver = { reads += it }) { read() }
        val writes = HashSet<Any>()
        Snapshot.observe(writeObserver = { writes += it }) { write() }
        return reads.any { it in writes }
    }

    // ── The screen's flag and the pause ──────────────────────────────────────────────────────────────

    // Pin (no token): S-1Δ4 "`settingsOpen`: false at start"; S-2Δ4 `openSettings` / `closeSettings` ("then set the flag");
    // E3 "A new process starts with the screen closed". A controller without Persistence and a timed boot both start closed;
    // openSettings opens (positive control), closeSettings closes; opened, then the seam-level restart (KitStore.kt
    // `restartTimed`: a new Persistence and a new controller over the same store) starts closed.
    @Test
    fun theSettingsAreClosedAtTheStartAndAfterARestart() {
        assertFalse("a controller without Persistence starts with the settings closed (S-1Δ4)", kitController().settingsOpen)
        val t = session.startTimed(KitProgressStore(), KitClock())
        assertFalse("a timed boot starts with the settings closed (S-1Δ4)", t.c.settingsOpen)
        t.c.openSettings()
        assertTrue("positive control: openSettings opens them (S-2Δ4)", t.c.settingsOpen)
        t.c.closeSettings()
        assertFalse("closeSettings closes them (S-2Δ4)", t.c.settingsOpen)
        t.c.openSettings()
        assertTrue("fixture: open again before the restart", t.c.settingsOpen)
        val again = session.restartTimed(t)
        assertFalse("a new controller over the same store starts with the settings closed (E3)", again.c.settingsOpen)
    }

    // Pin (no token): REQ-032 rule 2 "Puzzle time is paused while the settings screen is open"; REQ-030 rule 1 ("The time
    // pauses while … the settings screen is shown, and resumes on return"); S-2Δ4 "fold first, then set the flag. The fold
    // predicate gains `&& !settingsOpen`"; C1 ("WO-007's settings screen: play only"); E3 ("Play time keeps counting while
    // the screen is open, and touches on it count"). A In progress at 0 s:
    // - 10 s with ticks: 10 (positive control);
    // - 20 s with no tick, then the settings open: 30 (the opening folds first, so those 20 s are A's; flag-then-fold: 10);
    // - 30 s with ticks while open: A stays at 30, and the play time grows by the same 30 s (the positive control in the
    //   same test);
    // - 25 s with no tick, then the settings close, then a tick: still 30 (the closing folds first, so those 25 s are not
    //   A's; flag-then-fold: 55), and they are play time;
    // - 10 s with ticks: 40 (the time resumes).
    @Test
    fun whileTheSettingsAreOpenThePuzzleTimeStaysPutAndThePlayTimeCounts() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val t = session.startTimed(KitProgressStore(mapOf(a.id to kitInProgress(a, 1, time = 0)), lastOpened = a.id), KitClock())
        assertEquals("fixture: A open", a.id, t.c.puzzle.id)
        assertEquals("fixture: A In progress", PuzzleState.IN_PROGRESS, t.c.state)
        t.c.setVisible(true)
        kitPlay(t, 10)
        assertEquals("positive control: 10 s on A count", 10, t.c.timeSeconds)

        kitTouchAndWait(t, 20)
        t.c.openSettings()
        assertTrue("fixture: the settings are open", t.c.settingsOpen)
        assertEquals("the 20 s before the settings opened are A's (openSettings folds first)", 30, t.c.timeSeconds)
        val playOpen = t.c.playTimes.allTimeSeconds
        kitPlay(t, 30)
        assertEquals("while the settings are open, A's time stays put (REQ-032 rule 2)", 30, t.c.timeSeconds)
        assertEquals("positive control: the same 30 s are play time (C1; E3)", playOpen + 30, t.c.playTimes.allTimeSeconds)

        kitTouchAndWait(t, 25)
        t.c.closeSettings()
        assertFalse("fixture: the settings are closed", t.c.settingsOpen)
        kitTick(t)
        assertEquals("the 25 s under the settings are not A's (closeSettings folds first)", 30, t.c.timeSeconds)
        assertEquals("…they are play time", playOpen + 55, t.c.playTimes.allTimeSeconds)
        kitPlay(t, 10)
        assertEquals("positive control: A's time resumes once the settings are closed (REQ-030 rule 1)", 40, t.c.timeSeconds)
    }

    // Pin (no token): REQ-032 statement ("WHEN it is closed, the game SHALL return to the same puzzle state") and its A1 line
    // ("Opening and closing the settings changes nothing on the board"); design §3 must-never ("opening or closing the screen
    // changes no placement, state, tray turn, open puzzle or puzzle time"); TYPE-006 rule 2 ("… opening the settings does not
    // start it").
    // - A In progress with two pieces placed, 12 s and a best of 30 s; a tray piece turned off its rest; the parallelogram
    //   mirrored when it is in the tray; 10 s of play. The board view (KitPlay.kt `kitSnapshot`: open puzzle, state, placed,
    //   tray, tray turns and mirrors, drag, preview, cues; plus the puzzle time, the best, the grid, the celebration) is the
    //   same after the opening, after 30 s of play with the settings open, and after the closing.
    // - Positive control: a tray tap after the closing changes the view, so the view can tell states apart.
    // - B New: the settings opened, 20 s of play, closed: B is still New, nothing placed, no puzzle time; the 20 s are play
    //   time (positive control).
    @Test
    fun openingAndClosingTheSettingsChangesNothingOnTheBoard() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val b = kitPuzzleOf(PuzzleKind.WARMUP, 1)
        val store = KitProgressStore(mapOf(a.id to kitInProgress(a, 2, time = 12, best = 30)), lastOpened = a.id)
        val t = session.startTimed(store, KitClock())
        assertEquals("fixture: A open", a.id, t.c.puzzle.id)
        assertEquals("fixture: A In progress with two pieces placed", 2, t.c.placed.size)
        t.c.setVisible(true)
        val turned = t.c.trayPieces.firstOrNull { it != PieceId.PG }
            ?: throw AssertionError("fixture defect: no tray piece but the parallelogram on ${a.id}")
        t.c.tapTray(turned)
        assertNotEquals("fixture: a tray piece turned off its resting turn", TrayRest.turn(turned), t.c.trayTurn(turned))
        if (PieceId.PG in t.c.trayPieces) kitMirrorInTray(t.c, true)
        kitPlay(t, 10)
        assertEquals("fixture: 10 s on A", 22, t.c.timeSeconds)
        val before = kitView(t.c)

        t.c.openSettings()
        assertTrue("fixture: the settings are open", t.c.settingsOpen)
        assertEquals("opening the settings changes nothing on the board", before, kitView(t.c))
        kitPlay(t, 30)
        assertEquals("30 s of play with the settings open change nothing on the board, its time included", before, kitView(t.c))
        t.c.closeSettings()
        assertEquals("closing the settings returns to the same puzzle state (REQ-032 statement)", before, kitView(t.c))

        t.c.tapTray(turned)
        assertNotEquals("positive control: a tray tap after the closing changes the view", before, kitView(t.c))

        kitChoose(t.c, b.id)
        assertEquals("fixture: B open", b.id, t.c.puzzle.id)
        assertEquals("fixture: B New", PuzzleState.NEW, t.c.state)
        val playB = t.c.playTimes.allTimeSeconds
        t.c.openSettings()
        kitPlay(t, 20)
        t.c.closeSettings()
        assertEquals("opening the settings does not start a New puzzle (TYPE-006 rule 2)", PuzzleState.NEW, t.c.state)
        assertEquals("…nothing placed", emptyList<Placement>(), t.c.placed)
        assertEquals("…no puzzle time", 0, t.c.timeSeconds)
        assertEquals("positive control: the 20 s were play time", playB + 20, t.c.playTimes.allTimeSeconds)
    }

    // ── The two switches ─────────────────────────────────────────────────────────────────────────────

    // Pin (no token): REQ-031 rule "Setting values: off (default), on"; REQ-033 ASSUMPTION "sound is on by default"; the
    // Settings KDoc; S-1Δ4 (`settings` "seeded from `persistence?.settings ?: Settings()`"; `timerShown` = "`settings.timerShown`");
    // S-2Δ4 (`setTimerShown` / `setSoundOn`: "`settings = copy(…)`, then `persistence?.saveSettings`"); S-19Δ2 (`saveSettings`
    // "updates the cache, then queues `Write.Settings`"); E4 ("take effect at once"; "The readers: the board's `timerShown`
    // … read[s] `controller.settings`"); WO-007 § Scope carry 1 (code review N1: "the board's timer changes only after a
    // restart" otherwise); G-13 (the settings persist through IProgressStore).
    // - An empty store: timer off, sound on, so no timer. A store holding both flipped: the controller is seeded from it.
    // - On one controller, each switch in turn: the controller's settings and `timerShown` change at once, the other field
    //   is untouched, Persistence's cache holds the new value at once, and the store holds it after the flush.
    // - After the seam-level restart the new controller reads what the switches wrote.
    // - No Persistence: the defaults, and each switch still changes the controller at once.
    @Test
    fun theTwoSwitchesChangeTheControllersSettingsAtOnceAndReachTheStore() {
        val fresh = session.startTimed(KitProgressStore(), KitClock())
        assertEquals(
            "an empty store: timer off (REQ-031 rule), sound on (REQ-033 ASSUMPTION)",
            Settings(timerShown = false, soundOn = true),
            fresh.c.settings,
        )
        assertFalse("…so the timer is not shown", fresh.c.timerShown)

        val seeded = KitProgressStore()
        runBlocking { seeded.saveSettings(Settings(timerShown = true, soundOn = false)) }
        assertEquals(
            "a store holding both switches flipped: the controller's settings are seeded from it (S-1Δ4)",
            Settings(timerShown = true, soundOn = false),
            session.startTimed(seeded, KitClock()).c.settings,
        )

        val t = session.startTimed(KitProgressStore(), KitClock())
        t.c.setTimerShown(true)
        assertEquals("setTimerShown(true): the controller's settings at once, sound untouched", Settings(timerShown = true, soundOn = true), t.c.settings)
        assertTrue("…timerShown with it, on the same controller (N1)", t.c.timerShown)
        assertEquals("…Persistence's cache at once (S-19Δ2)", Settings(timerShown = true, soundOn = true), t.run.persistence.settings)
        assertEquals("…and the store after the flush", Settings(timerShown = true, soundOn = true), kitStoredSettings(t))

        t.c.setSoundOn(false)
        assertEquals("setSoundOn(false): the controller's settings at once, the timer untouched", Settings(timerShown = true, soundOn = false), t.c.settings)
        assertTrue("…the timer still shown", t.c.timerShown)
        assertEquals("…Persistence's cache at once", Settings(timerShown = true, soundOn = false), t.run.persistence.settings)
        assertEquals("…and the store after the flush", Settings(timerShown = true, soundOn = false), kitStoredSettings(t))

        t.c.setTimerShown(false)
        assertEquals("setTimerShown(false): at once", Settings(timerShown = false, soundOn = false), t.c.settings)
        assertFalse("…the timer no longer shown, on the same controller (N1)", t.c.timerShown)
        assertEquals("…and the store after the flush", Settings(timerShown = false, soundOn = false), kitStoredSettings(t))

        val again = session.restartTimed(t)
        assertEquals("after the restart the controller reads what the switches wrote", Settings(timerShown = false, soundOn = false), again.c.settings)

        val bare = kitController()
        assertEquals("no Persistence: the defaults (S-1Δ4 `?: Settings()`)", Settings(timerShown = false, soundOn = true), bare.settings)
        bare.setTimerShown(true)
        assertTrue("no Persistence: setTimerShown still changes the controller at once (S-2Δ4 `persistence?.`)", bare.timerShown)
        bare.setSoundOn(false)
        assertEquals("no Persistence: setSoundOn too", Settings(timerShown = true, soundOn = false), bare.settings)
    }

    // Pin (no token): S-1Δ4 "`settings`: the controller's own `mutableStateOf`"; E4 ("`PlayController.settings` is snapshot
    // state"); reviews/WO-007-design-review.md Re-derived row 6 ("Holds, provided `settings` is the controller's own
    // `mutableStateOf` … and not a getter over `Persistence.settings`"); WO-007 § Scope carry 1 (N1: "The switch must make the
    // read snapshot state"). A composition that read `timerShown` (PlayScreen reads it during composition, row 6) or the
    // sound field (the sound gate reads `settings`, E4) is told when the switch changes it: the state object it read is
    // among those the switch writes, as Compose's snapshot observers report them. A getter over a plain field (N1's
    // defect) reads no state object and fails here, though it would pass a plain read after the switch.
    // - The kit's harness first, on its own state object: told of a write to what was read (positive control), not told of
    //   a write to something else (it can fail).
    @Test
    fun aSwitchChangesSnapshotStateSoAReaderIsToldAtOnce() {
        val kitRead = mutableStateOf(0)
        val kitOther = mutableStateOf(0)
        assertTrue("harness: a reader is told of a write to the state it read", kitIsToldOf({ kitRead.value }, { kitRead.value = 1 }))
        assertFalse("harness: a reader is not told of a write to other state", kitIsToldOf({ kitRead.value }, { kitOther.value = 1 }))

        val t = session.startTimed(KitProgressStore(), KitClock())
        assertTrue(
            "a reader of timerShown is told when setTimerShown changes it (S-1Δ4: snapshot state)",
            kitIsToldOf({ t.c.timerShown }, { t.c.setTimerShown(true) }),
        )
        assertTrue(
            "a reader of the sound setting is told when setSoundOn changes it (S-1Δ4: snapshot state)",
            kitIsToldOf({ t.c.settings.soundOn }, { t.c.setSoundOn(false) }),
        )
    }

    // ── Best times ───────────────────────────────────────────────────────────────────────────────────

    // Pin (no token): S-1Δ4 `bestTimes` ("listed puzzles with a stored best, in ‹ › order; the open puzzle's comes from
    // `bestSeconds`. Refreshed by `openSettings` and `resetAll`"); S-2Δ4 step (5) ("`bestTimes` empty"); E5 ("One row per
    // puzzle with a stored best, in ‹ › order"; "A puzzle the player never solved has no row; nor does one solved only by the
    // DEV aid, which sets no best"); REQ-030 rule 3 (the best time "in the settings list") and rule 4 ("A puzzle solved by the
    // developer aid sets no best time"); REQ-030 rule 2 (Retry keeps the best, so a New puzzle may hold one).
    // - The seed, in an order other than ‹ › order: F3 Solved (best 70), B Solved (best 90), F0 New after a Retry (best 17),
    //   F1 Solved with no best (an aid solve), F2 In progress, never solved. C (New) is open.
    // - The first opening: F0 17, F3 70, B 90 in ‹ › order; F1 and F2 have no row.
    // - Then, in an order other than ‹ › order: C solved by play in 40 s; A solved by the DEV aid (no best); B retried and
    //   solved in 25 s, a better best, B left open. The next opening refreshes the rows: F0 17, F3 70, B 25, C 40 in ‹ ›
    //   order, A absent; B's row is its `bestSeconds`.
    // - Reset all progress empties them.
    @Test
    fun bestTimesListsEveryPuzzleWithAStoredBestInListOrder() {
        val quick = kitQuick()
        if (quick.size < 3) throw AssertionError("fixture defect: the library lists ${quick.size} quick puzzles, not 3")
        val qa = quick[0]
        val qb = quick[1]
        val qc = quick[2]
        val full = kitLibrary.puzzles.filter { it.kind == PuzzleKind.FULL }
        if (full.size < 4) throw AssertionError("fixture defect: the library lists ${full.size} full puzzles, not 4")
        val seed = LinkedHashMap<String, PuzzleProgress>()
        seed[full[3].id] = kitSolved(time = 75, best = 70)
        seed[qb.id] = kitSolved(time = 95, best = 90)
        seed[full[0].id] = PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 17)
        seed[full[1].id] = kitSolved(time = 0, best = null)
        seed[full[2].id] = kitInProgress(full[2], 1, time = 12)
        val t = session.startTimed(KitProgressStore(seed, lastOpened = qc.id), KitClock())
        assertEquals("fixture: C open", qc.id, t.c.puzzle.id)
        assertEquals("fixture: C New", PuzzleState.NEW, t.c.state)
        t.c.setVisible(true)

        t.c.openSettings()
        assertEquals(
            "the first opening: one row per puzzle with a stored best, in ‹ › order; none for the aid's or the unsolved",
            kitExpected(full[0] to 17, full[3] to 70, qb to 90),
            kitRows(t.c),
        )
        t.c.closeSettings()

        kitSolveTaking(t, qc, 40)
        assertEquals("fixture: C's best time", 40, t.c.bestSeconds)
        kitChoose(t.c, qa.id)
        assertEquals("fixture: A open", qa.id, t.c.puzzle.id)
        val aid = DevAid()
        aid.onButton()
        assertTrue("fixture: 0417 is accepted (S-26 submit)", aid.submit("0417"))
        assertTrue("fixture: the aid solves A (S-27)", aid.solveNow(t.c))
        assertEquals("fixture: A Solved by the aid", PuzzleState.SOLVED, t.c.state)
        assertNull("fixture: the aid sets no best time (REQ-030 rule 4)", t.c.bestSeconds)
        kitChoose(t.c, qb.id)
        assertEquals("fixture: B open, Solved", PuzzleState.SOLVED, t.c.state)
        assertTrue("fixture: Retry", t.c.retry())
        kitSolveTaking(t, qb, 25)
        assertEquals("fixture: B's best time improved", 25, t.c.bestSeconds)

        t.c.openSettings()
        val rows = kitRows(t.c)
        assertEquals(
            "refreshed by openSettings: the new rows, in ‹ › order (not the order solved); the aid's puzzle has none",
            kitExpected(full[0] to 17, full[3] to 70, qb to 25, qc to 40),
            rows,
        )
        assertEquals("the open puzzle's row is its bestSeconds (S-1Δ4)", t.c.bestSeconds, rows.firstOrNull { it.first == qb.id }?.second)

        t.c.resetAll()
        assertEquals("Reset all progress empties the best times (S-2Δ4 step 5)", emptyList<Pair<String, Int>>(), kitRows(t.c))
    }

    // ── Reset all progress ───────────────────────────────────────────────────────────────────────────

    // Pin (no token): REQ-034 statement ("set every puzzle to New and erase all puzzle, best and play times") and rule 2
    // ("Settings (timer, sound) are kept"); S-2Δ4 `resetAll()` (its five steps; "It never calls `open` and emits no event");
    // S-19Δ2 ("All in one FIFO"; "the settings and last-opened are kept"); design §3 "Reset order"; E6 ("Nothing pre-reset is
    // written after (3): `open`'s leaving save is never called"; "Afterwards: the player stays on the open puzzle. The
    // settings screen stays open, showing 0:00, 0:00 and no best times"); S-44Δ ("NONE until a second counts"); decisions.md
    // 2026-10-15 "When progress is saved." (a lock queues the open puzzle's progress) and 2026-10-17 "Readings the JVM kit
    // pins where S-26 / S-27 are silent:" ("every queued save lands as its own store write, no merging").
    // - The trap: A open, In progress (seeded with one lock, 5 s and a best of 44 s); S Solved (best 33); I In progress
    //   (best 20); both switches flipped (timer on, sound off); 25 s of play. Positive controls after a flush: the store holds
    //   A's progress, play times and the switches — none of them at their fresh values.
    // - Then every store write is slowed, so the pre-reset writes are still in flight when the reset is queued: A's second
    //   piece locks (its save queued), 20 s wait, the settings open (fold first), 7 s wait, then Reset all progress.
    // - At once: the settings stay open; A is still open, New, with an empty board, every piece in the tray, time 0, no best;
    //   every listed puzzle New; the play times NONE, and a tick with no time passed leaves them NONE; the switches kept;
    //   the best times empty; no PlayEvent.
    // - After the flush: the store reads every puzzle FRESH, the play times NONE, the switches and the last-opened id kept.
    //   Its writes since the trap: exactly one reset; a save of A with its two pieces before it (positive control: a stale
    //   write existed, and the FIFO landed it first); after it nothing but the last-opened id of A — no pre-reset save, no
    //   leaving save.
    // - Positive control: 5 s of play after the reset count from NONE.
    @Test
    fun resetAllErasesEveryPuzzleAndThePlayTimesKeepsTheSettingsAndStaysOnTheOpenPuzzle() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val s = kitPuzzleOf(PuzzleKind.WARMUP, 1)
        val i = kitPuzzleOf(PuzzleKind.WARMUP, 2)
        val plan = kitPlanOf(a)
        val seed = linkedMapOf(
            a.id to kitInProgress(a, 1, time = 5, best = 44),
            s.id to kitSolved(time = 50, best = 33),
            i.id to kitInProgress(i, 1, time = 12, best = 20),
        )
        val slow = AtomicBoolean(false)
        val store = KitProgressStore(seed, lastOpened = a.id) { if (slow.get()) KIT_SLOW_WRITE_MS else 0L }
        val events = mutableListOf<PlayEvent>()
        val t = session.startTimed(store, KitClock(), events)
        assertEquals("fixture: A open", a.id, t.c.puzzle.id)
        assertEquals("fixture: A In progress", PuzzleState.IN_PROGRESS, t.c.state)
        t.c.setVisible(true)
        t.c.setTimerShown(true)
        t.c.setSoundOn(false)
        kitPlay(t, 25)
        session.flush(t.run)
        assertNotEquals("positive control: the store holds A's progress, not FRESH", PuzzleProgress.FRESH, store.kitProgressOf(a.id))
        assertNotEquals("positive control: the store holds play times, not NONE", PlayTimes.NONE, runBlocking { store.playTimes() })
        assertEquals("positive control: the store holds both switches flipped", Settings(timerShown = true, soundOn = false), runBlocking { store.settings() })

        val mark = store.kitWrites().size
        slow.set(true)
        kitPlace(t.c, plan[1])
        assertEquals("fixture: two pieces on A", 2, t.c.placed.size)
        kitTouchAndWait(t, 20)
        t.c.openSettings()
        assertEquals(
            "positive control: the open settings list the stored best times (S-1Δ4), so 'empty' below can fail",
            kitExpected(a to 44, s to 33, i to 20),
            kitRows(t.c),
        )
        kitTouchAndWait(t, 7)
        val eventsBefore = events.size

        t.c.resetAll()
        slow.set(false)

        assertTrue("the settings screen stays open (E6)", t.c.settingsOpen)
        assertEquals("the player stays on the open puzzle (E6; S-2Δ4 step 4)", a.id, t.c.puzzle.id)
        assertEquals("…now New", PuzzleState.NEW, t.c.state)
        assertEquals("…with an empty board", emptyList<Placement>(), t.c.placed)
        assertEquals("…every piece in the tray", a.pieces, t.c.trayPieces.toSet())
        assertEquals("…time 0", 0, t.c.timeSeconds)
        assertNull("…no best time", t.c.bestSeconds)
        assertEquals("every listed puzzle New (REQ-034 statement)", kitAllNew(), kitStates(t.c))
        assertEquals("the play times erased (S-2Δ4 step 5)", PlayTimes.NONE, t.c.playTimes)
        assertEquals("the settings kept (REQ-034 rule 2)", Settings(timerShown = true, soundOn = false), t.c.settings)
        assertTrue("…the timer still shown", t.c.timerShown)
        assertEquals("the best times empty (S-2Δ4 step 5)", emptyList<Pair<String, Int>>(), kitRows(t.c))
        assertEquals("Reset all progress emits no PlayEvent (S-2Δ4)", emptyList<String>(), events.drop(eventsBefore).map { kitEventText(it) })
        kitTick(t)
        assertEquals("a tick with no time passed: still NONE until a second counts (S-44Δ)", PlayTimes.NONE, t.c.playTimes)

        session.flush(t.run)
        assertEquals(
            "the store reads every listed puzzle FRESH (REQ-034 statement; IProgressStore resetAll)",
            emptyList<String>(),
            kitLibrary.puzzles.map { it.id }.filter { store.kitProgressOf(it) != PuzzleProgress.FRESH },
        )
        assertEquals("the store's play times NONE", PlayTimes.NONE, runBlocking { store.playTimes() })
        assertEquals("the store's settings kept (REQ-034 rule 2)", Settings(timerShown = true, soundOn = false), runBlocking { store.settings() })
        assertEquals("the store's last-opened id kept (S-19Δ2)", a.id, store.kitLastOpened())

        val writes = store.kitWrites().drop(mark)
        val resets = writes.indices.filter { writes[it].member == "resetAll" }
        assertEquals("exactly one store reset (S-19Δ2): ${writes.map { kitWriteText(it) }}", 1, resets.size)
        val r = resets.single()
        assertTrue(
            "positive control: a save of A with its two pieces was queued before the reset and landed before it (the one FIFO): " +
                writes.map { kitWriteText(it) },
            writes.take(r).any { it.member == "saveProgress" && it.puzzleId == a.id && (it.progress?.placed?.size ?: 0) == 2 },
        )
        assertEquals(
            "after the store's reset nothing but A's last-opened id is written: no pre-reset save, no leaving save (design §3)",
            emptyList<String>(),
            writes.drop(r + 1).filterNot { it.member == "saveLastOpenedPuzzleId" && it.puzzleId == a.id }.map { kitWriteText(it) },
        )

        kitPlay(t, 5)
        assertEquals("positive control: the first seconds after the reset count from NONE", PlayTimes(5, KIT_DAY, 5), t.c.playTimes)
    }

    // Pin (no token): S-2Δ4 step (3) ("`persistence?.resetAll()`, else `memory.clear()`") and step (2) (`time?.`); REQ-034
    // statement; S-2Δ4 ("emits no event"). A controller with neither Persistence nor a time source: M solved by play, then N
    // open with a piece placed (In progress). Positive control: M reads Solved. Reset all progress: every listed puzzle New,
    // still on N with an empty board, the play times NONE, no event; M opened again is New with nothing placed.
    @Test
    fun resetAllWithoutPersistenceSetsEveryPuzzleNew() {
        val quick = kitQuick()
        if (quick.size < 2) throw AssertionError("fixture defect: the library lists ${quick.size} quick puzzles, not 2")
        val m = quick[0]
        val n = quick[1]
        val events = mutableListOf<PlayEvent>()
        val c = kitController(events)
        kitOpen(c, m.id)
        kitPlanOf(m).forEach { kitPlace(c, it) }
        assertEquals("fixture: M solved", PuzzleState.SOLVED, c.state)
        kitOpen(c, n.id)
        kitPlace(c, kitPlanOf(n).first())
        assertEquals("fixture: N In progress", PuzzleState.IN_PROGRESS, c.state)
        assertEquals("positive control: M reads Solved from the controller's memory", PuzzleState.SOLVED, c.stateOf(m.id))
        val mark = events.size

        c.resetAll()
        assertEquals("every listed puzzle New (S-2Δ4 step 3: memory cleared)", kitAllNew(), kitStates(c))
        assertEquals("still on N (S-2Δ4 step 4)", n.id, c.puzzle.id)
        assertEquals("…with an empty board", emptyList<Placement>(), c.placed)
        assertEquals("no time source: the play times NONE", PlayTimes.NONE, c.playTimes)
        assertEquals("no PlayEvent (S-2Δ4)", emptyList<String>(), events.drop(mark).map { kitEventText(it) })
        kitOpen(c, m.id)
        assertEquals("M opened again is New", PuzzleState.NEW, c.state)
        assertEquals("…with nothing placed", emptyList<Placement>(), c.placed)
    }

    // ── Persistence (S-19Δ2) ─────────────────────────────────────────────────────────────────────────

    // Pin (no token): S-19Δ2 ("`var settings`: read under the lock"; "`saveSettings(s)`: updates the cache, then queues
    // `Write.Settings`"; "All in one FIFO"); ADR-003 ("one FIFO writer (`:play`'s `Persistence`)"); S-19Δ "`load` also reads
    // both" (the play times and the settings). On a plain boot over a store with A In progress: the defaults read at load
    // (fixture); a save of A that the store holds slowly, then saveSettings: the cache holds the new settings at once, and the
    // store completes the two writes in the order they were queued (a write launched on its own would overtake the slow one).
    // The store then holds both; a new Persistence over it reads the settings at load.
    @Test
    fun persistenceSaveSettingsUpdatesItsCacheAtOnceAndWritesInTheOneFifo() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val slowNext = AtomicBoolean(false)
        val store = KitProgressStore(mapOf(a.id to kitInProgress(a, 1, time = 3)), lastOpened = a.id) {
            if (slowNext.getAndSet(false)) KIT_SLOW_WRITE_MS * 3 else 0L
        }
        val run = session.start(store)
        session.flush(run)
        val p = run.persistence
        assertEquals("fixture: the defaults read at load", Settings(timerShown = false, soundOn = true), p.settings)
        val mark = store.kitWrites().size

        slowNext.set(true)
        val board = kitInProgress(a, 2, time = 9)
        p.save(a.id, board)
        p.saveSettings(Settings(timerShown = true, soundOn = false))
        assertEquals("saveSettings updates Persistence's cache at once", Settings(timerShown = true, soundOn = false), p.settings)
        session.flush(run)
        assertEquals(
            "the settings write is queued behind the slow save: one FIFO",
            listOf("saveProgress ${a.id} IN_PROGRESS placed 2", "saveSettings"),
            store.kitWrites().drop(mark).map { kitWriteText(it) },
        )
        assertEquals("the store holds the settings", Settings(timerShown = true, soundOn = false), runBlocking { store.settings() })
        assertEquals("…and the slow save", board, store.kitStored(a.id))
        val again = session.restart(run)
        assertEquals("a new Persistence over the store reads the settings at load", Settings(timerShown = true, soundOn = false), again.persistence.settings)
    }

    // Pin (no token): S-19Δ2 ("`resetAll()`: queues `Write.Reset` (`store.resetAll()`), empties the cache, sets `times =
    // NONE`; the settings and last-opened are kept. All in one FIFO"); the IProgressStore `resetAll` KDoc; design §3 "Reset
    // order" ("the FIFO lands every earlier write first").
    // - The store holds A In progress (best 20), B Solved (best 33), the switches flipped, play times 30 / 90; A last opened.
    // - A save of B (In progress) that the store holds slowly, then resetAll. At once: the cache reads every listed puzzle
    //   FRESH, the play times NONE, the settings and the last-opened id as they were.
    // - A save of C after the reset: the cache holds it (positive control: the cache is not simply empty from now on).
    // - After the flush: the store completed exactly the slow save of B, the reset, then the save of C, in that order; it
    //   reads A and B FRESH (B's pre-reset save landed before the reset), C as saved after it, the play times NONE, the
    //   settings and the last-opened id kept.
    @Test
    fun persistenceResetAllEmptiesItsCacheKeepsTheSettingsAndLastOpenedAndWritesInTheOneFifo() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val b = kitPuzzleOf(PuzzleKind.WARMUP, 1)
        val c = kitPuzzleOf(PuzzleKind.WARMUP, 2)
        val slowNext = AtomicBoolean(false)
        val store = KitProgressStore(
            mapOf(a.id to kitInProgress(a, 1, time = 12, best = 20), b.id to kitSolved(time = 40, best = 33)),
            lastOpened = a.id,
        ) { if (slowNext.getAndSet(false)) KIT_SLOW_WRITE_MS * 3 else 0L }
        runBlocking {
            store.saveSettings(Settings(timerShown = true, soundOn = false))
            store.savePlayTimes(PlayTimes(30, KIT_DAY, 90))
        }
        val run = session.start(store)
        session.flush(run)
        val p = run.persistence
        assertEquals("fixture: the play times read at load", PlayTimes(30, KIT_DAY, 90), p.playTimes)
        assertEquals("fixture: A's progress in the cache", PuzzleState.IN_PROGRESS, p.progress(a.id).state)
        val mark = store.kitWrites().size

        slowNext.set(true)
        p.save(b.id, kitInProgress(b, 1, time = 7))
        p.resetAll()
        assertEquals(
            "at once, the cache reads every listed puzzle FRESH",
            emptyList<String>(),
            kitLibrary.puzzles.map { it.id }.filter { p.progress(it) != PuzzleProgress.FRESH },
        )
        assertEquals("…the play times NONE", PlayTimes.NONE, p.playTimes)
        assertEquals("…the settings kept", Settings(timerShown = true, soundOn = false), p.settings)
        assertEquals("…the last-opened id kept", a.id, p.lastOpenedId)
        val later = kitInProgress(c, 1, time = 4)
        p.save(c.id, later)
        assertEquals("positive control: a save after the reset is in the cache", later, p.progress(c.id))

        session.flush(run)
        assertEquals(
            "the store completed the slow save, the reset and the later save in the order they were queued: one FIFO",
            listOf("saveProgress ${b.id} IN_PROGRESS placed 1", "resetAll", "saveProgress ${c.id} IN_PROGRESS placed 1"),
            store.kitWrites().drop(mark).map { kitWriteText(it) },
        )
        assertEquals("the store reads A FRESH", PuzzleProgress.FRESH, store.kitProgressOf(a.id))
        assertEquals("…B FRESH: its pre-reset save landed before the reset", PuzzleProgress.FRESH, store.kitProgressOf(b.id))
        assertEquals("…C as saved after the reset", later, store.kitStored(c.id))
        assertEquals("…the play times NONE", PlayTimes.NONE, runBlocking { store.playTimes() })
        assertEquals("…the settings kept", Settings(timerShown = true, soundOn = false), runBlocking { store.settings() })
        assertEquals("…the last-opened id kept", a.id, store.kitLastOpened())
    }
}
