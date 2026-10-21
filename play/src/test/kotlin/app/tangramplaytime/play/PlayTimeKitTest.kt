// Acceptance kit (WO-008, TASK-045) — the play time, the puzzle time and the best time, on the JVM: PlayController
// (designs/WO-008-design.md §2 S-1Δ3, S-2Δ3, S-20Δ) over Persistence (S-19Δ) on the kit's in-memory store double
// (KitStore.kt), with the real ActiveTime (S-45) behind IActiveTime (S-44), the real engine and the shipped puzzles.
//
// - Tokens: REQ-005 A1 and A2, REQ-030 A1, A2 and A3, each on the one test whose assertion is that line's meaning
//   (traceability-rules.md §2). REQ-005's "available" is read as decisions.md 2026-10-20 "REQ-005's two lines are verified
//   as locked, at the seam …": "the game holds the value and returns it when asked". Every other test is a pin: it cites a
//   seam row, a REQ rule, a TYPE rule or a decisions.md row by date and bold title (the brief's labels: D4 "What counts on
//   each screen.", D5 "Today's play time follows the date `Clock.today()` reports.", D6 "No time the store's reader rejects is
//   ever written …" with D6a, D7 "An aid solve keeps the best time through one generic controller member.", D8 "When the
//   times are written, and what a process death may lose."), and carries no token.
// - Time (design §2 "Kit doors", JVM: "a fake Clock, the real ActiveTime, and countTime() after each jump"; tasks.md § WO-008
//   seam note 3): the kit owns its clock (KitStore.kt `KitClock`) and moves it; nothing sleeps. The kit reports touches to
//   the kernel itself (`time.touched()`, in place of `:app`'s window hook; S-15Δ3 "AppGraph.touched() forwards to the
//   kernel") and visibility through the controller (`setVisible`, as S-15Δ3's `AppGraph.visible` "forwards it to the
//   controller"). A tick of the ticker is `countTime()` (S-2Δ3 "the fold"; S-15Δ3 "countTime(), then delay(250)"). Every
//   action, touch and clock move runs on the test's one thread (S-44 "One caller thread").
// - Boots: KitStore.kt `KitSession.startTimed` follows `AppGraph.start` (S-15Δ3): Persistence.load, then
//   ActiveTime(clock, persistence.playTimes) and the controller with it. `restartTimed` is the seam-level restart of
//   designs/WO-004-design.md §3 over the same store and the same clock.
// - What the store holds is read after S-2Δ `flush()` (IProgressStore KDoc: "A save is complete when the call returns").
// - Every "nothing" or "at most" line has its positive control in the same test (seam note 3; reviews/WO-008-design-review.md
//   F1). Fixtures discriminate: each expected time is exact, so a lost, a doubled or a misattributed second fails.
// - The puzzles are the shipped ones (KitPlay.kt `kitLibrary`, through `:content`); the kit reads no file itself. Helper
//   names carry a `kit` / `Kit` prefix.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class PlayTimeKitTest {

    private val session = KitSession()

    @After
    fun closeSession() = session.close()

    // ── Kit helpers ──────────────────────────────────────────────────────────────────────────────────

    /** A touch on the game, reported to the kernel (S-44 `touched()`; S-15Δ3 `AppGraph.touched()` forwards it there). */
    private fun kitTouch(t: KitTimed) = t.time.touched()

    /** One tick of the ticker: the fold (S-2Δ3 `countTime()`). */
    private fun kitTick(t: KitTimed) {
        t.c.countTime()
    }

    /**
     * [seconds] of active play as the app makes them: a touch, the clock moved by at most 30 s (inside TYPE-005's "at most
     * 60 s ago"), then a tick; repeated until [seconds] have passed.
     */
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

    /** The first listed mini puzzle with at least two pieces, so its first drag and its solving drop are two actions. */
    private fun kitMini(): PuzzleDefinition =
        kitLibrary.puzzles.firstOrNull { it.kind == PuzzleKind.MINI && it.pieces.size >= 2 }
            ?: throw AssertionError("fixture defect: the library lists no mini puzzle with two pieces")

    /** The puzzle after [p] in the ‹ › order (IPuzzleLibrary `puzzles`; S-2Δ `next()`). */
    private fun kitAfter(p: PuzzleDefinition): PuzzleDefinition {
        val all = kitLibrary.puzzles
        val i = all.indexOfFirst { it.id == p.id }
        if (i < 0) throw AssertionError("fixture defect: ${p.id} is not listed")
        return all[(i + 1) % all.size]
    }

    /**
     * Solves the open New puzzle [p] by the player's actions (KitPlay.kt `kitPlace`): its first drop (the drag from the tray
     * makes it In progress, TYPE-006), then [seconds] of play ([kitPlay]), then its other drops, the last of which solves it.
     */
    private fun kitSolveTaking(t: KitTimed, p: PuzzleDefinition, seconds: Int) {
        assertEquals("fixture: ${p.id} open", p.id, t.c.puzzle.id)
        assertEquals("fixture: ${p.id} New", PuzzleState.NEW, t.c.state)
        val plan = kitPlanOf(p)
        if (plan.size < 2) throw AssertionError("fixture defect: ${p.id} has a single piece")
        kitPlace(t.c, plan.first())
        assertEquals("fixture: In progress from the first drag (TYPE-006)", PuzzleState.IN_PROGRESS, t.c.state)
        kitPlay(t, seconds)
        plan.drop(1).forEach { kitPlace(t.c, it) }
        assertEquals("fixture: ${p.id} solved by the player", PuzzleState.SOLVED, t.c.state)
    }

    /** What the store holds for [id] once every queued write has landed (S-2Δ `flush()`). */
    private fun kitSaved(t: KitTimed, id: String): PuzzleProgress {
        session.flush(t.run)
        return t.store.kitStored(id)
    }

    /** The play times the store holds once every queued write has landed. */
    private fun kitSavedTimes(t: KitTimed): PlayTimes {
        session.flush(t.run)
        return runBlocking { t.store.playTimes() }
    }

    // ── REQ-005: play-time tracking ──────────────────────────────────────────────────────────────────

    // REQ-005 A1 — "After solving a puzzle, its solve time is available." Read as decisions.md 2026-10-20 "REQ-005's two lines
    // are verified as locked, at the seam …": "after a solve, the controller's time of the solve and the store's timeSeconds
    // for that Solved puzzle equal the active seconds counted". S-1Δ3: `timeSeconds` (now public); S-20Δ: "timeSeconds now
    // rides every S-20 save"; the PuzzleProgress KDoc: "when Solved, the time of the solve".
    // - A mini puzzle: 10 s on it while New (TYPE-006 rule 4, "Time is counted only In progress"), then 45 s between its first
    //   drag and its solve: the time of the solve is 45, in the controller and in the store.
    // - Still available afterwards: on the solved view, where 30 s more of play change nothing (D4: "Solved view and its
    //   celebration: play only"); after › and ‹ (the puzzle opens Solved with its time: decisions.md 2026-10-15 "When progress
    //   is saved", "`time` and `best` pass through as read"); after leaving the screen and the seam-level restart.
    @Test
    fun afterSolvingAPuzzleItsSolveTimeIsAvailable() { // REQ-005.A1
        val m = kitMini()
        val t = session.startTimed(KitProgressStore(lastOpened = m.id), KitClock())
        assertEquals("fixture: ${m.id} open", m.id, t.c.puzzle.id)
        t.c.setVisible(true)
        kitPlay(t, 10)
        assertEquals("fixture: no puzzle time while New (TYPE-006 rule 4)", 0, t.c.timeSeconds)

        kitSolveTaking(t, m, 45)
        assertEquals("the controller's time of the solve", 45, t.c.timeSeconds)
        val saved = kitSaved(t, m.id)
        assertEquals("the store: Solved", PuzzleState.SOLVED, saved.state)
        assertEquals("the store: the time of the solve", 45, saved.timeSeconds)

        kitPlay(t, 30)
        assertEquals("on the solved view, 30 s more of play leave the time of the solve (D4)", 45, t.c.timeSeconds)
        assertEquals("…in the store too", 45, kitSaved(t, m.id).timeSeconds)

        kitNext(t.c)
        assertNotEquals("fixture: › left ${m.id}", m.id, t.c.puzzle.id)
        kitPrevious(t.c)
        assertEquals("fixture: ‹ back on ${m.id}", m.id, t.c.puzzle.id)
        assertEquals("fixture: ${m.id} Solved", PuzzleState.SOLVED, t.c.state)
        assertEquals("back on the solved puzzle: the time of its solve", 45, t.c.timeSeconds)

        t.c.setVisible(false)
        val again = session.restartTimed(t)
        assertEquals("fixture: the restart opens ${m.id}", m.id, again.c.puzzle.id)
        assertEquals("fixture: ${m.id} Solved after the restart", PuzzleState.SOLVED, again.c.state)
        assertEquals("after a restart: the time of the solve", 45, again.c.timeSeconds)
        assertEquals("…and the store's", 45, again.store.kitStored(m.id).timeSeconds)
    }

    // REQ-005 A2 — "The total active play time is available at any time." Read as decisions.md 2026-10-20 "REQ-005's two lines
    // are verified as locked, at the seam …": "the all-time play time the controller reports (from IActiveTime.playTimes())
    // equals every second counted so far. It is read on the board, the solved view and the grid, after a restart" (the
    // process death is the app kit's). S-1Δ3: `playTimes` "(refreshed per fold)"; D4: play time "counts every active second
    // whatever is shown". REQ-005 rule: "Time is measured in active seconds (TYPE-005)".
    // - A fresh install: none, once the game is shown (the fold of `setVisible`).
    // - 20 s on the board while New, 25 s In progress, 30 s on the solved view, 40 s on the grid: 20, 45, 75, 115.
    // - The game leaves the screen; the seam-level restart (a new Persistence, a new ActiveTime over the play times it read,
    //   a new controller); the game shown again: 115. Then 10 s more: 125.
    @Test
    fun theTotalActivePlayTimeIsAvailableAtAnyTime() { // REQ-005.A2
        val m = kitMini()
        val t = session.startTimed(KitProgressStore(lastOpened = m.id), KitClock())
        assertEquals("fixture: ${m.id} open", m.id, t.c.puzzle.id)
        t.c.setVisible(true)
        assertEquals("a fresh install: no play time yet", 0, t.c.playTimes.allTimeSeconds)

        kitPlay(t, 20)
        assertEquals("on the board, New", 20, t.c.playTimes.allTimeSeconds)
        val plan = kitPlanOf(m)
        kitPlace(t.c, plan.first())
        assertEquals("fixture: In progress", PuzzleState.IN_PROGRESS, t.c.state)
        kitPlay(t, 25)
        assertEquals("on the board, In progress", 45, t.c.playTimes.allTimeSeconds)
        plan.drop(1).forEach { kitPlace(t.c, it) }
        assertEquals("fixture: Solved", PuzzleState.SOLVED, t.c.state)
        kitPlay(t, 30)
        assertEquals("on the solved view", 75, t.c.playTimes.allTimeSeconds)
        t.c.openOverview()
        assertTrue("fixture: the grid is open", t.c.overviewOpen)
        kitPlay(t, 40)
        assertEquals("on the grid", 115, t.c.playTimes.allTimeSeconds)
        t.c.closeOverview()

        t.c.setVisible(false)
        val again = session.restartTimed(t)
        again.c.setVisible(true)
        assertEquals("after a restart", 115, again.c.playTimes.allTimeSeconds)
        kitPlay(again, 10)
        assertEquals("…and it goes on from there", 125, again.c.playTimes.allTimeSeconds)
    }

    // ── REQ-030: puzzle time and best time ───────────────────────────────────────────────────────────

    // REQ-030 A1 — "Browsing away for 2 minutes adds nothing to the puzzle's time." Statement: "WHILE a puzzle in progress
    // (TYPE-006) is shown, the game SHALL count active seconds as that puzzle's time"; rule 1: "The time pauses while another
    // puzzle … is shown, and resumes on return."
    // - Puzzle A and the next puzzle B, both In progress in the store (A at 7 s, B at 3 s), A open (the start; S-2Δ3: "a
    //   resumed In-progress puzzle counts from the first fold"). 30 s on A: 37. › to B; 2 minutes of play on B, the ticker
    //   folding throughout; ‹ back to A: A's time is 37, in the controller and in the store.
    // - Positive controls in the same test (seam note 3): the 2 minutes are play time (D4: "Play time … counts every active
    //   second whatever is shown") and B's time (B In progress is the puzzle shown, so the seconds had somewhere else to go);
    //   back on A, 10 s of play count again (rule 1: "resumes on return").
    @Test
    fun browsingAwayForTwoMinutesAddsNothingToThePuzzlesTime() { // REQ-030.A1
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val b = kitAfter(a)
        val store = KitProgressStore(mapOf(a.id to kitInProgress(a, 1, time = 7), b.id to kitInProgress(b, 1, time = 3)), lastOpened = a.id)
        val t = session.startTimed(store, KitClock())
        assertEquals("fixture: A open", a.id, t.c.puzzle.id)
        assertEquals("fixture: A In progress", PuzzleState.IN_PROGRESS, t.c.state)
        t.c.setVisible(true)
        kitPlay(t, 30)
        assertEquals("fixture: 30 s on A", 37, t.c.timeSeconds)
        val playBefore = t.c.playTimes.allTimeSeconds

        kitNext(t.c)
        assertEquals("fixture: › shows B", b.id, t.c.puzzle.id)
        assertEquals("fixture: B In progress", PuzzleState.IN_PROGRESS, t.c.state)
        kitPlay(t, 120)
        assertEquals("positive control: the 2 minutes away are play time (D4)", playBefore + 120, t.c.playTimes.allTimeSeconds)
        assertEquals("positive control: …and B's time, B being the puzzle shown", 123, t.c.timeSeconds)

        kitPrevious(t.c)
        assertEquals("fixture: ‹ back on A", a.id, t.c.puzzle.id)
        assertEquals("2 minutes away added nothing to A's time", 37, t.c.timeSeconds)
        assertEquals("…nor to A's time in the store", 37, kitSaved(t, a.id).timeSeconds)
        kitPlay(t, 10)
        assertEquals("positive control: back on A, its time resumes (REQ-030 rule 1)", 47, t.c.timeSeconds)
    }

    // REQ-030 A2 — "Solving again slower keeps the earlier, faster best time." Statement: "WHEN the puzzle is solved, the game
    // SHALL store the time and keep the shortest as the best time"; S-2Δ3: after the solving drop "bestSeconds = … minOf(
    // bestSeconds ?: timeSeconds, timeSeconds)"; rule 2: Retry resets "the running time, not the best time". The fixture
    // discriminates (the brief): a first solve of 40 s, a slower second of 70 s (keeping "the last" fails here), a faster third
    // of 25 s (keeping "the first" fails here). Each solve's own time is stored as its time, and the best beside it.
    @Test
    fun solvingAgainSlowerKeepsTheEarlierFasterBestTime() { // REQ-030.A2
        val m = kitMini()
        val t = session.startTimed(KitProgressStore(lastOpened = m.id), KitClock())
        assertEquals("fixture: ${m.id} open", m.id, t.c.puzzle.id)
        t.c.setVisible(true)
        assertNull("fixture: never solved, no best time (PuzzleProgress KDoc)", t.c.bestSeconds)

        kitSolveTaking(t, m, 40)
        assertEquals("the first solve's time", 40, t.c.timeSeconds)
        assertEquals("the first solve sets the best time", 40, t.c.bestSeconds)
        assertEquals("stored: Solved, its time, the best", kitSolved(time = 40, best = 40), kitSaved(t, m.id))

        assertTrue("fixture: Retry", t.c.retry())
        kitSolveTaking(t, m, 70)
        assertEquals("the slower solve's time is stored as its time", 70, t.c.timeSeconds)
        assertEquals("solving again slower keeps the earlier, faster best time", 40, t.c.bestSeconds)
        assertEquals("stored: the slower time, the faster best", kitSolved(time = 70, best = 40), kitSaved(t, m.id))

        assertTrue("fixture: Retry", t.c.retry())
        kitSolveTaking(t, m, 25)
        assertEquals("the faster solve's time", 25, t.c.timeSeconds)
        assertEquals("a faster solve becomes the best (the statement: \"keep the shortest\")", 25, t.c.bestSeconds)
        assertEquals("stored: the faster time and best", kitSolved(time = 25, best = 25), kitSaved(t, m.id))
    }

    // REQ-030 A3 — "Turning a tray piece on a New puzzle starts no time." TYPE-006: "New → In progress when a piece first leaves
    // the tray (a drag starts on a tray piece). Turning a piece in the tray … does not start it"; rule 4: "Time is counted only
    // In progress". REQ-030 rationale: "turning a piece in the tray costs nothing".
    // - A New puzzle with the parallelogram: three tray turns (S-2 `tapTray`) and the parallelogram's tray mirror (S-2
    //   `pressBadge`), each followed by 10 s of play with the ticker folding: still New, time 0.
    // - Positive controls in the same test (seam note 3): those 40 s are play time (D4: "Board, New: play only"); and the same
    //   touches, clock and tray moves on the same puzzle once In progress (a piece dragged out and locked) add their 40 s.
    @Test
    fun turningATrayPieceOnANewPuzzleStartsNoTime() { // REQ-030.A3
        val p = kitLibrary.puzzles.firstOrNull { it.kind != PuzzleKind.MINI && PieceId.PG in it.pieces && kitPlanOf(it).first().piece != PieceId.PG }
            ?: throw AssertionError("fixture defect: no non-mini puzzle with the parallelogram whose first planned piece is another")
        val step = kitPlanOf(p).first()
        val t = session.startTimed(KitProgressStore(lastOpened = p.id), KitClock())
        assertEquals("fixture: ${p.id} open", p.id, t.c.puzzle.id)
        t.c.setVisible(true)
        fun kitTrayMovesWithPlay(what: String) {
            val turned = t.c.trayPieces.firstOrNull { it != PieceId.PG && it != step.piece }
                ?: throw AssertionError("fixture defect: $what: no tray piece to turn")
            repeat(3) {
                t.c.tapTray(turned)
                kitPlay(t, 10)
            }
            kitMirrorInTray(t.c, !t.c.trayMirrored(PieceId.PG))
            kitPlay(t, 10)
        }

        kitTrayMovesWithPlay("New")
        assertEquals("tray turns and the tray mirror leave the puzzle New (TYPE-006)", PuzzleState.NEW, t.c.state)
        assertEquals("turning tray pieces on a New puzzle starts no time", 0, t.c.timeSeconds)
        assertEquals("positive control: the 40 s are play time (D4)", 40, t.c.playTimes.allTimeSeconds)

        kitPlace(t.c, step)
        assertEquals("fixture: In progress once a piece left the tray", PuzzleState.IN_PROGRESS, t.c.state)
        kitTrayMovesWithPlay("In progress")
        assertEquals("positive control: the same moves and clock In progress add their 40 s", 40, t.c.timeSeconds)
    }

    // ── Pins: the fold and the resumed puzzle ────────────────────────────────────────────────────────

    // Pin (no token): reviews/WO-008-design-review.md F2 and decisions.md 2026-10-20 "Amends three 2026-10-20 rows … F2 option
    // (a) …": "Pinned in the JVM controller kit: an In-progress puzzle in the store → construct → visible, touch, +30 s →
    // countTime() → its time +30, with no other action"; S-2Δ3: "a resumed In-progress puzzle counts from the first fold".
    @Test
    fun aResumedInProgressPuzzleCountsFromTheFirstFold() {
        val p = kitPuzzleOf(PuzzleKind.WARMUP)
        val t = session.startTimed(KitProgressStore(mapOf(p.id to kitInProgress(p, 2, time = 12)), lastOpened = p.id), KitClock())
        assertEquals("fixture: ${p.id} open", p.id, t.c.puzzle.id)
        assertEquals("fixture: In progress", PuzzleState.IN_PROGRESS, t.c.state)
        assertEquals("fixture: its stored time, read at the start (decisions.md 2026-10-15 \"When progress is saved\")", 12, t.c.timeSeconds)
        t.c.setVisible(true)
        kitTouch(t)
        t.clock.forward(30)
        t.c.countTime()
        assertEquals("+30 s at the first fold, with no other action", 42, t.c.timeSeconds)
    }

    // Pin (no token): design §3 "Fold before every change": "Without a fold, '2 min on B, back to A' credits A"; S-2Δ3 "Fold
    // first, before: open …"; D4 ("Board, New: play only"). No tick at all across the changes, so only the doors' folds
    // attribute the seconds: A In progress, 30 s, › to B (New), 2 minutes of touches on B, ‹ back to A, then a tick. A has its
    // 30 s (the › fold) and none of the 2 minutes (the ‹ fold gives them to B, New: nobody's puzzle time), and B, opened again,
    // has none either. Positive control: all 150 s are play time.
    @Test
    fun twoMinutesOnAnotherPuzzleAreNeverCreditedToThePuzzleReturnedTo() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val b = kitAfter(a)
        val t = session.startTimed(KitProgressStore(mapOf(a.id to kitInProgress(a, 1, time = 0)), lastOpened = a.id), KitClock())
        assertEquals("fixture: A open", a.id, t.c.puzzle.id)
        t.c.setVisible(true)
        val playBefore = t.c.playTimes.allTimeSeconds

        kitTouchAndWait(t, 30)
        kitNext(t.c)
        assertEquals("fixture: › shows B", b.id, t.c.puzzle.id)
        assertEquals("fixture: B New", PuzzleState.NEW, t.c.state)
        repeat(4) { kitTouchAndWait(t, 30) }
        kitPrevious(t.c)
        assertEquals("fixture: ‹ back on A", a.id, t.c.puzzle.id)
        kitTick(t)
        assertEquals("A has its 30 s and none of the 2 minutes on B", 30, t.c.timeSeconds)
        assertEquals("positive control: all 150 s are play time", playBefore + 150, t.c.playTimes.allTimeSeconds)
        kitNext(t.c)
        assertEquals("B, New, has no time either", 0, t.c.timeSeconds)
    }

    // Pin (no token): design §3 "Fold before every change": "Without a fold, … a solve loses its last seconds"; S-2Δ3 "Fold
    // first, before: … the solving drop, after which bestSeconds = …". A mini puzzle In progress with 10 s; every piece but the
    // last placed; the last one dragged from the tray (its start folds), then 25 s with no tick, then the solving drop: the
    // time of the solve is 35 and so is the best, in the controller and the store, and a tick after it changes nothing.
    @Test
    fun theSolvingDropFoldsFirstSoASolveKeepsItsLastSeconds() {
        val m = kitMini()
        val plan = kitPlanOf(m)
        val t = session.startTimed(KitProgressStore(lastOpened = m.id), KitClock())
        assertEquals("fixture: ${m.id} open", m.id, t.c.puzzle.id)
        t.c.setVisible(true)
        kitPlace(t.c, plan.first())
        kitPlay(t, 10)
        plan.drop(1).dropLast(1).forEach { kitPlace(t.c, it) }
        val last = plan.last()
        kitDragFromTray(t.c, last.piece, last.target.turn, last.target.mirrored)
        kitTouchAndWait(t, 25)
        t.c.moveDrag(last.dropCentre, true)
        t.c.updatePreview()
        assertNotNull("fixture: the planned last drop locks", t.c.drop())
        assertEquals("fixture: the drop solves ${m.id}", PuzzleState.SOLVED, t.c.state)
        assertEquals("the solve keeps its last 25 s", 35, t.c.timeSeconds)
        assertEquals("…and so does the best time", 35, t.c.bestSeconds)
        kitTick(t)
        assertEquals("a tick after the solve changes nothing", 35, t.c.timeSeconds)
        assertEquals("stored: the time and the best of the solve", kitSolved(time = 35, best = 35), kitSaved(t, m.id))
    }

    // Pin (no token): S-2Δ3 "Fold first, before: open, then the leaving puzzle's S-20 save, then load" and "The construction's
    // first load neither folds nor saves"; design §3 "The leaving save belongs in open, never in the first load (it would save
    // defaults over the puzzle)"; S-20Δ "timeSeconds now rides every S-20 save". The store holds A In progress at 12 s and B In
    // progress at 5 s, A last opened.
    // - The construction writes no progress, and the store still holds A as seeded.
    // - 30 s on A with no tick, then ›: the store holds A with 42 s (after a flush), though no action changed A after the fold;
    //   B, opened, is in the store as it was.
    @Test
    fun theLeavingPuzzleIsSavedWithItsTimeAndTheConstructionSavesNothing() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val b = kitAfter(a)
        val seedA = kitInProgress(a, 2, time = 12)
        val seedB = kitInProgress(b, 1, time = 5)
        val store = KitProgressStore(mapOf(a.id to seedA, b.id to seedB), lastOpened = a.id)
        val t = session.startTimed(store, KitClock())
        assertEquals("fixture: A open", a.id, t.c.puzzle.id)
        assertEquals("fixture: A's board as seeded (the load check keeps a planned board)", kitByPiece(seedA.placed), kitBoardOf(t.c))
        session.flush(t.run)
        assertEquals("the construction writes no progress (S-2Δ3)", emptyList<KitWrite>(), store.kitWrites().filter { it.member == "saveProgress" })
        assertEquals("…and the store still holds A as seeded", seedA, store.kitStored(a.id))

        t.c.setVisible(true)
        val boardA = t.c.placed.toList()
        kitTouchAndWait(t, 30)
        kitNext(t.c)
        assertEquals("fixture: › shows B", b.id, t.c.puzzle.id)
        assertEquals(
            "the leaving puzzle A is saved with its 30 s (S-20's shape: state, the board, time, best)",
            PuzzleProgress(PuzzleState.IN_PROGRESS, boardA, 42, null),
            kitSaved(t, a.id),
        )
        val savedB = kitSaved(t, b.id)
        assertEquals("B, opened, is in the store In progress as it was", PuzzleState.IN_PROGRESS, savedB.state)
        assertEquals("…with its own 5 s: the 30 s were A's", 5, savedB.timeSeconds)
    }

    // Pin (no token): D4 ("Grid overview open: play only — it shows the other puzzles over the board (REQ-030 rule 1 'while
    // another puzzle … is shown')"); S-2Δ3: the fold credits the puzzle "if (state == IN_PROGRESS && !overviewOpen)", and
    // "Fold first, before: … openOverview, closeOverview". A In progress:
    // - 10 s with ticks: 10 (positive control);
    // - 20 s with no tick, then the grid opens: 30 (the opening folds first, so those 20 s are A's);
    // - 30 s with ticks on the grid: A's time pauses at 30, and play time counts (positive control);
    // - 25 s with no tick, then the grid closes, then a tick: still 30 (the closing folds first, so those 25 s are not A's);
    // - 10 s with ticks: 40 (the time resumes).
    @Test
    fun theGridPausesThePuzzleTimeAndItsOpeningAndClosingFoldFirst() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val t = session.startTimed(KitProgressStore(mapOf(a.id to kitInProgress(a, 1, time = 0)), lastOpened = a.id), KitClock())
        assertEquals("fixture: A open", a.id, t.c.puzzle.id)
        t.c.setVisible(true)
        kitPlay(t, 10)
        assertEquals("positive control: 10 s on A", 10, t.c.timeSeconds)

        kitTouchAndWait(t, 20)
        t.c.openOverview()
        assertTrue("fixture: the grid is open", t.c.overviewOpen)
        assertEquals("the 20 s before the grid opened are A's (the opening folds first)", 30, t.c.timeSeconds)
        val playBefore = t.c.playTimes.allTimeSeconds
        kitPlay(t, 30)
        assertEquals("the grid pauses A's time (D4)", 30, t.c.timeSeconds)
        assertEquals("positive control: play time counts on the grid (D4)", playBefore + 30, t.c.playTimes.allTimeSeconds)

        kitTouchAndWait(t, 25)
        t.c.closeOverview()
        assertFalse("fixture: the grid is closed", t.c.overviewOpen)
        kitTick(t)
        assertEquals("the 25 s under the grid are not A's (the closing folds first)", 30, t.c.timeSeconds)
        kitPlay(t, 10)
        assertEquals("positive control: the time resumes once the grid is closed", 40, t.c.timeSeconds)
    }

    // Pin (no token): S-2Δ3 "Fold first, before: … a tray startDrag"; TYPE-006 rule 4 "Time is counted only In progress"; D4
    // ("Board, New: play only"). 20 s on a New puzzle with no tick, then the first drag from the tray (New → In progress), then
    // a tick: the 20 s were spent while New, so they are play time and not the puzzle's. Positive control: 10 s of play after it
    // are the puzzle's.
    @Test
    fun theFirstDragFromTheTrayFoldsFirstSoSecondsSpentWhileNewAreNotThePuzzles() {
        val m = kitMini()
        val step = kitPlanOf(m).first()
        val t = session.startTimed(KitProgressStore(lastOpened = m.id), KitClock())
        assertEquals("fixture: ${m.id} open", m.id, t.c.puzzle.id)
        t.c.setVisible(true)
        kitTouchAndWait(t, 20)
        kitDragFromTray(t.c, step.piece, step.target.turn, step.target.mirrored)
        assertEquals("fixture: In progress from the drag", PuzzleState.IN_PROGRESS, t.c.state)
        kitTick(t)
        assertEquals("the 20 s spent while New are not the puzzle's", 0, t.c.timeSeconds)
        assertEquals("…they are play time (D4)", 20, t.c.playTimes.allTimeSeconds)
        kitPlay(t, 10)
        assertEquals("positive control: 10 s In progress are the puzzle's", 10, t.c.timeSeconds)
    }

    // Pin (no token): REQ-030 rule 2: "Restart and Retry reset the running time, not the best time"; REQ-026
    // rule "Retry keeps the best time"; REQ-025 statement (Restart sets the puzzle to New) and its A2 line ("the puzzle time is
    // zero"); the IProgressStore `saveProgress` KDoc: the caller "keeps bestSeconds on Restart and Retry"; S-2Δ3 "Fold first,
    // before: … restart, retry". A puzzle In progress at 50 s with a best time of 33 s:
    // - 10 s: 60. 15 s with no tick, then Restart: time 0, best 33, New, in the controller and the store; a tick adds nothing
    //   to the New puzzle.
    // - Solved again, taking 20 s: its time counts from 0 (20, not 75), and the best becomes 20.
    // - 15 s with no tick, then Retry: time 0, best 20, in the controller and the store; a tick adds nothing.
    // - Positive control: once a piece leaves the tray, 5 s count 5.
    @Test
    fun restartAndRetryResetTheRunningTimeAndKeepTheBestTime() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val t = session.startTimed(KitProgressStore(mapOf(a.id to kitInProgress(a, 2, time = 50, best = 33)), lastOpened = a.id), KitClock())
        assertEquals("fixture: A open", a.id, t.c.puzzle.id)
        assertEquals("fixture: A's best time", 33, t.c.bestSeconds)
        t.c.setVisible(true)
        kitPlay(t, 10)
        assertEquals("fixture: 10 s more on A", 60, t.c.timeSeconds)

        kitTouchAndWait(t, 15)
        assertTrue("fixture: Restart acts In progress", t.c.restart())
        assertEquals("Restart: New", PuzzleState.NEW, t.c.state)
        assertEquals("Restart resets the running time", 0, t.c.timeSeconds)
        assertEquals("Restart keeps the best time", 33, t.c.bestSeconds)
        kitTick(t)
        assertEquals("a tick after Restart adds nothing to the New puzzle", 0, t.c.timeSeconds)
        assertEquals("stored after Restart: New, the tray, time 0, best kept", PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 33), kitSaved(t, a.id))

        kitSolveTaking(t, a, 20)
        assertEquals("after Restart the time counts from 0", 20, t.c.timeSeconds)
        assertEquals("the faster solve becomes the best", 20, t.c.bestSeconds)

        kitTouchAndWait(t, 15)
        assertTrue("fixture: Retry acts Solved", t.c.retry())
        assertEquals("Retry: New", PuzzleState.NEW, t.c.state)
        assertEquals("Retry resets the running time", 0, t.c.timeSeconds)
        assertEquals("Retry keeps the best time", 20, t.c.bestSeconds)
        kitTick(t)
        assertEquals("a tick after Retry adds nothing to the New puzzle", 0, t.c.timeSeconds)
        assertEquals("stored after Retry: New, the tray, time 0, best kept", PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 20), kitSaved(t, a.id))

        kitPlace(t.c, kitPlanOf(a).first())
        kitPlay(t, 5)
        assertEquals("positive control: In progress again, 5 s count 5", 5, t.c.timeSeconds)
    }

    // Pin (no token): REQ-030 statement "the game SHALL count active seconds as that puzzle's time"; TYPE-005 (a second counts
    // only while "the player's last touch was at most 60 s ago"); design §3 "Idle adds at most 60 s". A puzzle In progress: a
    // touch, then 5 minutes with the ticker folding every 10 s: exactly 60 s of puzzle time (an exact 60 fails 0 as well);
    // 5 more untouched minutes: nothing; then a touch and 10 s: 10 more (positive control). Play time agrees: 70.
    @Test
    fun thePuzzleTimeCountsActiveSecondsOnly() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val t = session.startTimed(KitProgressStore(mapOf(a.id to kitInProgress(a, 1, time = 0)), lastOpened = a.id), KitClock())
        assertEquals("fixture: A open", a.id, t.c.puzzle.id)
        t.c.setVisible(true)
        kitTouch(t)
        repeat(30) {
            t.clock.forward(10)
            kitTick(t)
        }
        assertEquals("a touch, then 5 untouched minutes: exactly 60 s of puzzle time", 60, t.c.timeSeconds)
        repeat(30) {
            t.clock.forward(10)
            kitTick(t)
        }
        assertEquals("5 more untouched minutes: nothing", 60, t.c.timeSeconds)
        kitPlay(t, 10)
        assertEquals("positive control: a touch and 10 s: 10 more", 70, t.c.timeSeconds)
        assertEquals("play time agrees", 70, t.c.playTimes.allTimeSeconds)
    }

    // ── Pins: the best time and the aid ──────────────────────────────────────────────────────────────

    // Pin (no token): REQ-030 rule 4 and REQ-046 rule 4 ("A puzzle solved by the aid is marked solved but does not set a best
    // time"); D7 with its F2 amendment ("none stays '–', a saved one is kept"); S-27Δ "drive runs inside c.solveWithoutBest
    // { }"; WO-008 must-do 1. Through `DevAid.solveNow` after the accepted entry, as DevAidKitTest reaches it (S-26 `onButton`,
    // `submit` "0417"), on a timed controller with play counted before each aid solve. The fixtures discriminate (D7): one
    // puzzle with a saved best (In progress at 40 s, best 61 s; the aid restarts it first, so its time is 0: decisions.md
    // 2026-10-17 "Solve this puzzle now is an ordinary solve") and one never solved (New, no best). An aid solve takes 0 s, so a
    // rule that took the solve's time as the best would store 0 for both. Positive control in the same test: after Retry, the
    // player's own solve of the second puzzle sets its best (S-2Δ3: "a withheld flag in try/finally").
    @Test
    fun anAidSolveKeepsTheBestTimeAsItWas() {
        val kept = kitPuzzleOf(PuzzleKind.WARMUP)
        val never = kitMini()
        val store = KitProgressStore(mapOf(kept.id to kitInProgress(kept, 2, time = 40, best = 61)), lastOpened = kept.id)
        val t = session.startTimed(store, KitClock())
        assertEquals("fixture: ${kept.id} open", kept.id, t.c.puzzle.id)
        t.c.setVisible(true)
        kitPlay(t, 20)
        assertEquals("fixture: 20 s counted on ${kept.id}", 60, t.c.timeSeconds)
        assertEquals("fixture: its saved best time", 61, t.c.bestSeconds)

        val aid = DevAid()
        aid.onButton()
        assertTrue("fixture: 0417 is accepted (S-26 submit)", aid.submit("0417"))
        assertTrue("the aid solves ${kept.id} (S-27)", aid.solveNow(t.c))
        assertEquals("fixture: Solved by the aid", PuzzleState.SOLVED, t.c.state)
        assertEquals("fixture: the aid's solve takes 0 s, so a naive rule would store 0", 0, t.c.timeSeconds)
        assertEquals("an aid solve keeps a saved best time", 61, t.c.bestSeconds)
        assertEquals("stored: Solved, time 0, the best kept", kitSolved(time = 0, best = 61), kitSaved(t, kept.id))

        kitChoose(t.c, never.id)
        assertEquals("fixture: ${never.id} New", PuzzleState.NEW, t.c.state)
        assertNull("fixture: ${never.id} has no best time", t.c.bestSeconds)
        kitPlay(t, 20)
        aid.onButton()
        assertTrue("the aid solves ${never.id} (S-27)", aid.solveNow(t.c))
        assertEquals("fixture: Solved by the aid", PuzzleState.SOLVED, t.c.state)
        assertNull("an aid solve leaves no best time none", t.c.bestSeconds)
        assertEquals("stored: Solved, no best time", kitSolved(time = 0, best = null), kitSaved(t, never.id))

        assertTrue("fixture: Retry", t.c.retry())
        kitSolveTaking(t, never, 30)
        assertEquals("positive control: the player's own solve sets the best time", 30, t.c.bestSeconds)
        assertEquals("stored: the player's time and best", kitSolved(time = 30, best = 30), kitSaved(t, never.id))
    }

    // Pin (no token): S-2Δ3 "internal fun solveWithoutBest(drive: () -> Unit): a withheld flag in try/finally" and the solving
    // drop's "bestSeconds = if (withheld) bestSeconds else minOf(bestSeconds ?: timeSeconds, timeSeconds)"; D7. Called
    // directly (the brief: internal members, no reflection).
    // - A solve shaped as the player's, taking 25 s, inside the member: Solved with its time 25 and still no best time — the
    //   withheld best does not depend on the time being 0.
    // - A drive that throws: its exception passes on, and the rule is back (try/finally).
    // - Positive control: the player's own solve outside it, taking 35 s, sets the best.
    @Test
    fun solveWithoutBestWithholdsTheBestOnlyWhileItsDriveRuns() {
        val m = kitMini()
        val t = session.startTimed(KitProgressStore(lastOpened = m.id), KitClock())
        assertEquals("fixture: ${m.id} open", m.id, t.c.puzzle.id)
        t.c.setVisible(true)

        t.c.solveWithoutBest { kitSolveTaking(t, m, 25) }
        assertEquals("fixture: Solved inside solveWithoutBest", PuzzleState.SOLVED, t.c.state)
        assertEquals("the solve's time is kept as its time", 25, t.c.timeSeconds)
        assertNull("inside solveWithoutBest a solve sets no best time, whatever its time", t.c.bestSeconds)

        assertTrue("fixture: Retry", t.c.retry())
        assertThrows("a drive that throws passes its exception on", IllegalStateException::class.java) {
            t.c.solveWithoutBest { throw IllegalStateException("kit: a drive that fails") }
        }
        kitSolveTaking(t, m, 35)
        assertEquals("positive control: after the throw, the player's own solve sets the best time", 35, t.c.bestSeconds)
    }

    // ── Pins: when the times are written ─────────────────────────────────────────────────────────────

    // Pin (no token): S-20Δ "Every 10 counted play seconds and at setVisible(false): Persistence.savePlayTimes(...), then the
    // open puzzle's S-20 save"; D8 ("every 10 counted play seconds and when the game leaves the screen (onStop)"; "Why 10: …
    // at most 360 rewrites per hour"); S-2Δ3 "setVisible(shown): fold, then time.visible(shown); false writes the times at
    // once"; S-19Δ `savePlayTimes` "updates the cache and queues the write, in one FIFO". A fresh install's play times and one
    // puzzle In progress at 5 s:
    // - ticks after 4 s and 5 s: nothing written (9 counted);
    // - 1 s more (10 counted): the play times (10 s), then the puzzle (15 s);
    // - 3 s more (13): nothing more;
    // - 4 s with no tick, then the game leaves the screen: the fold first, then the play times (17 s) and the puzzle (22 s);
    // - hidden: a touch and 30 s, then the game shown again and a tick: nothing counted (`setVisible(false)` reached the
    //   kernel; TYPE-005 "visible on screen"); positive control: 10 s of play count 10.
    @Test
    fun theTimesAreWrittenEveryTenCountedPlaySecondsAndWhenTheGameLeavesTheScreen() {
        val p = kitPuzzleOf(PuzzleKind.WARMUP)
        val store = KitProgressStore(mapOf(p.id to kitInProgress(p, 2, time = 5)), lastOpened = p.id)
        val t = session.startTimed(store, KitClock())
        assertEquals("fixture: ${p.id} open", p.id, t.c.puzzle.id)
        t.c.setVisible(true)
        session.flush(t.run)
        fun kitTimeWritesSince(mark: Int): List<String> =
            store.kitWrites().drop(mark)
                .filter { it.member == "savePlayTimes" || it.member == "saveProgress" }
                .map { if (it.puzzleId == null) it.member else "${it.member} ${it.puzzleId}" }
        var mark = store.kitWrites().size

        kitPlay(t, 4)
        kitPlay(t, 5)
        session.flush(t.run)
        assertEquals("9 counted play seconds: nothing written yet", emptyList<String>(), kitTimeWritesSince(mark))

        kitPlay(t, 1)
        session.flush(t.run)
        assertEquals("the 10th counted second: the play times, then the open puzzle", listOf("savePlayTimes", "saveProgress ${p.id}"), kitTimeWritesSince(mark))
        assertEquals("the play times written", PlayTimes(10, KIT_DAY, 10), kitSavedTimes(t))
        assertEquals("the open puzzle written with its time", 15, store.kitStored(p.id).timeSeconds)
        mark = store.kitWrites().size

        kitPlay(t, 3)
        session.flush(t.run)
        assertEquals("13 counted: nothing more", emptyList<String>(), kitTimeWritesSince(mark))

        kitTouchAndWait(t, 4)
        t.c.setVisible(false)
        session.flush(t.run)
        assertEquals("leaving the screen: the play times, then the open puzzle", listOf("savePlayTimes", "saveProgress ${p.id}"), kitTimeWritesSince(mark))
        assertEquals("…with the last 4 s folded first", PlayTimes(17, KIT_DAY, 17), kitSavedTimes(t))
        assertEquals("…on the puzzle too", 22, store.kitStored(p.id).timeSeconds)

        kitTouchAndWait(t, 30)
        t.c.setVisible(true)
        kitTick(t)
        assertEquals("hidden, nothing counted", 22, t.c.timeSeconds)
        assertEquals("…no play time either", 17, t.c.playTimes.allTimeSeconds)
        kitPlay(t, 10)
        assertEquals("positive control: shown again, 10 s count", 32, t.c.timeSeconds)
    }

    // Pin (no token): WO-008 must-do 2; D6 ("No time the store's reader rejects is ever written — the kernel's time math
    // guarantees it"; "The controller only adds counted seconds to a loaded time (≥ 0, the reader checked it) or writes 0")
    // with D6a; ADR-003 (a file the reader rejects reads as a fresh install); D5 (a date set back restarts today; all-time never
    // decreases). A puzzle In progress at 5 s: 30 s; the clock sent 10 minutes back (as the app kit's reset of
    // AppClock.override does, review F5) and a tick; 20 s; the date set back a day; 10 s; then the solve, and the game leaving
    // the screen. Every progress the double recorded, and the play times stored, are never negative, nor the best time.
    // Positive control: the seconds after each jump still count (exact values: 35, 35, 55, 65; today 10 on the date set back).
    @Test
    fun noNegativeTimeReachesTheStoreAcrossABackwardsClock() {
        val p = kitPuzzleOf(PuzzleKind.WARMUP)
        val store = KitProgressStore(mapOf(p.id to kitInProgress(p, 2, time = 5)), lastOpened = p.id)
        val t = session.startTimed(store, KitClock())
        assertEquals("fixture: ${p.id} open", p.id, t.c.puzzle.id)
        t.c.setVisible(true)
        kitPlay(t, 30)
        assertEquals("fixture: 30 s", 35, t.c.timeSeconds)

        t.clock.back(10 * 60)
        kitTouch(t)
        kitTick(t)
        assertEquals("the clock 10 minutes back: nothing counted, nothing negative", 35, t.c.timeSeconds)
        kitPlay(t, 20)
        assertEquals("positive control: 20 s after the jump count", 55, t.c.timeSeconds)

        t.clock.date = KIT_DAY.minusDays(1)
        kitPlay(t, 10)
        assertEquals("positive control: 10 s on the date set back", 65, t.c.timeSeconds)
        assertEquals("today restarts on the date set back; all-time keeps growing (D5)", PlayTimes(10, KIT_DAY.minusDays(1), 60), t.c.playTimes)

        kitPlanOf(p).drop(2).forEach { kitPlace(t.c, it) }
        assertEquals("fixture: Solved", PuzzleState.SOLVED, t.c.state)
        assertEquals("the solve's best time", 65, t.c.bestSeconds)
        t.c.setVisible(false)
        session.flush(t.run)

        val written = store.kitWrites().mapNotNull { it.progress }
        assertTrue("fixture: progress was written", written.isNotEmpty())
        for (w in written) {
            assertTrue("a written puzzle time is never negative: $w", w.timeSeconds >= 0)
            val best = w.bestSeconds
            assertTrue("a written best time is never negative: $w", best == null || best >= 0)
        }
        val times = kitSavedTimes(t)
        assertTrue("the stored play times are never negative: $times", times.todaySeconds >= 0 && times.allTimeSeconds >= 0)
        assertEquals("the stored play times", PlayTimes(10, KIT_DAY.minusDays(1), 60), times)
        assertEquals("the stored puzzle", kitSolved(time = 65, best = 65), store.kitStored(p.id))
    }

    // ── Pin: the timer setting ───────────────────────────────────────────────────────────────────────

    // Pin (no token): S-1Δ3 "timerShown (from Persistence.settings, else false)"; S-19Δ "`load` also reads both"; REQ-031 rule
    // "Setting values: off (default), on"; the Settings KDoc ("off by default"). A fresh store: false. The store holding the
    // setting on: true. The store holding it off with sound off: false (the timer's own field is read). No Persistence: false.
    @Test
    fun timerShownIsFalseOnAFreshStoreAndTrueWhenTheStoreHoldsTheSettingOn() {
        assertFalse("a fresh store: the timer setting is off", session.startTimed(KitProgressStore(), KitClock()).c.timerShown)

        val on = KitProgressStore()
        runBlocking { on.saveSettings(Settings(timerShown = true)) }
        assertTrue("the store holds the setting on: shown", session.startTimed(on, KitClock()).c.timerShown)

        val off = KitProgressStore()
        runBlocking { off.saveSettings(Settings(timerShown = false, soundOn = false)) }
        assertFalse("the store holds it off (and sound off): not shown", session.startTimed(off, KitClock()).c.timerShown)

        assertFalse("no Persistence: false (S-1Δ3 \"else false\")", kitController().timerShown)
    }
}
