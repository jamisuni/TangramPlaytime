// Acceptance kit (WO-004, TASK-016) — browsing on the JVM: PlayController (designs/WO-004-design.md §2
// S-1Δ, S-2Δ) over Persistence (S-19) on the kit's in-memory store double (KitStore.kt), with the real
// engine and the shipped puzzles.
//
// - Tokens: REQ-003 A1, REQ-024 A1 and A3, REQ-050 A1 and A3, and the screen half of REQ-040 A1, REQ-041
//   A1 and REQ-045 A1 (tasks.md § WO-004 intro, "Lines covered twice"; their `content/` tokens stand). Each
//   sits on the one test whose assertion is that line's meaning (traceability-rules.md §2). REQ-024 A2 and
//   REQ-050 A2 are screen lines (TASK-019). The other tests are pins: each cites a seam row of the design's
//   §2, a decisions.md row (date and bold title) or a TYPE rule, and carries no token.
// - Puzzles are named by their 1-based place in `kitLibrary.puzzles`, the ‹ › order (IPuzzleLibrary
//   `puzzles` KDoc: "Every puzzle, in ‹ › order").
// - A fresh install is a controller over an empty store: the IProgressStore KDoc's "Missing or unreadable
//   stored state behaves as a fresh install … no last-opened puzzle".
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowsingKitTest {

    private val session = KitSession()

    @After
    fun closeSession() = session.close()

    private val puzzles get() = kitLibrary.puzzles

    // ── A fresh install (S-1Δ construction over an empty store) ──────────────────────────────────

    // REQ-045.A1 — "A fresh install opens on a mini puzzle whose tray holds three pieces." S-1Δ: the
    // controller "Opens lastOpenedId if listed, else puzzles.first()"; an empty store has none. The tray is
    // S-1's `trayPieces` ("the puzzle's pieces neither placed nor dragged"); TYPE-006: a fresh puzzle is New.
    @Test
    fun aFreshInstallOpensOnAMiniPuzzleWhoseTrayHoldsThreePieces() { // REQ-045.A1
        val c = session.start(KitProgressStore()).c
        assertEquals("a mini puzzle (REQ-045)", PuzzleKind.MINI, c.puzzle.kind)
        assertEquals("its tray holds three pieces", 3, c.trayPieces.size)
        assertEquals("the tray holds that puzzle's pieces only (REQ-045 statement)", c.puzzle.pieces, c.trayPieces)
        assertTrue("nothing on the board", c.placed.isEmpty())
        assertEquals("New (TYPE-006)", PuzzleState.NEW, c.state)
    }

    // REQ-040.A1 — "The first puzzle shown on a fresh install has rating 1." Shown = the controller's open
    // puzzle at construction (S-1Δ); the fixture's store is empty. The open puzzle is the first of the ‹ ›
    // order (S-1Δ: "else puzzles.first()").
    @Test
    fun theFirstPuzzleShownOnAFreshInstallHasRatingOne() { // REQ-040.A1
        val c = session.start(KitProgressStore()).c
        assertEquals("the first shown has rating 1", 1, c.puzzle.rating)
        assertEquals("it is the first of the ‹ › order (S-1Δ)", puzzles.first().id, c.puzzle.id)
    }

    // REQ-041.A1 — "On a fresh install, the first four puzzles after the mini puzzles are warm-ups." Walked as
    // the player walks them: › (S-2Δ `next()`) from the puzzle a fresh install opens. The walk is the ‹ ›
    // order itself (IPuzzleLibrary `puzzles`; REQ-024 statement: › shows "the next puzzle"). REQ-041
    // statement: the warm-ups are "of rating 1".
    @Test
    fun onAFreshInstallTheFirstFourPuzzlesAfterTheMinisAreWarmUps() { // REQ-041.A1
        val c = session.start(KitProgressStore()).c
        val shown = mutableListOf(c.puzzle)
        repeat(puzzles.size - 1) {
            kitNext(c)
            shown += c.puzzle
        }
        assertEquals("› walks the ‹ › order", puzzles.map { it.id }, shown.map { it.id })
        val minis = shown.takeWhile { it.kind == PuzzleKind.MINI }
        assertTrue("fixture: the walk starts with the mini puzzles (REQ-045)", minis.isNotEmpty())
        val next4 = shown.drop(minis.size).take(4)
        assertEquals("four puzzles follow the minis", 4, next4.size)
        for (p in next4) {
            assertEquals("${p.id} is a warm-up", PuzzleKind.WARMUP, p.kind)
            assertEquals("${p.id} has rating 1 (REQ-041 statement)", 1, p.rating)
        }
    }

    // ── › and ‹ ──────────────────────────────────────────────────────────────────────────────────

    // REQ-024.A1 — "› works on an unsolved puzzle." REQ-024 statement: › shows "the next puzzle, whether or
    // not the current one is solved". Unsolved = New or In progress (TYPE-006). Two paths: a fresh install's
    // first puzzle New and then In progress through play; and every listed puzzle but the last seeded
    // unsolved, New or In progress in turn (the last one's › is A3's wrap).
    @Test
    fun nextWorksOnAnUnsolvedPuzzle() { // REQ-024.A1
        val c = session.start(KitProgressStore()).c
        val first = puzzles[0]
        assertEquals("fixture: New", PuzzleState.NEW, c.state)
        kitNext(c)
        assertEquals("› on a New puzzle", puzzles[1].id, c.puzzle.id)
        kitPrevious(c)
        assertEquals(first.id, c.puzzle.id)
        kitPlace(c, kitPlanOf(first)[0])
        assertEquals("fixture: In progress", PuzzleState.IN_PROGRESS, c.state)
        kitNext(c)
        assertEquals("› on an In-progress puzzle", puzzles[1].id, c.puzzle.id)

        val seed = puzzles.withIndex().filter { it.index % 2 == 1 }.associate { it.value.id to kitInProgress(it.value, 1) }
        val d = session.start(KitProgressStore(seed)).c
        for (i in 0 until puzzles.size - 1) {
            val p = puzzles[i]
            kitChoose(d, p.id)
            assertEquals("fixture: ${p.id} open", p.id, d.puzzle.id)
            assertNotEquals("fixture: ${p.id} unsolved", PuzzleState.SOLVED, d.state)
            val state = d.state
            kitNext(d)
            assertEquals("› on ${p.id} ($state)", puzzles[i + 1].id, d.puzzle.id)
        }
    }

    // REQ-024.A3 — "› on the last puzzle shows the first." The last puzzle in each TYPE-006 state (REQ-024
    // statement: "whether or not the current one is solved"), opened as the start (S-1Δ: lastOpenedId).
    // The rule's other half, "‹ on the first puzzle shows the last" (REQ-024 rule; S-2Δ "next() and
    // previous() wrap"), is asserted beside it.
    @Test
    fun nextOnTheLastPuzzleShowsTheFirst() { // REQ-024.A3
        val first = puzzles.first()
        val last = puzzles.last()
        assertNotEquals("fixture: more than one puzzle", first.id, last.id)
        for (seeded in listOf(null, kitInProgress(last, 2), kitSolved(time = 70, best = 65))) {
            val store = KitProgressStore(if (seeded == null) emptyMap() else mapOf(last.id to seeded), lastOpened = last.id)
            val c = session.start(store).c
            val state = seeded?.state ?: PuzzleState.NEW
            assertEquals("fixture: the last puzzle is open", last.id, c.puzzle.id)
            assertEquals("fixture: it is $state", state, c.state)
            kitNext(c)
            assertEquals("› on the last puzzle ($state) shows the first", first.id, c.puzzle.id)
            kitPrevious(c)
            assertEquals("‹ on the first puzzle shows the last (REQ-024 rule)", last.id, c.puzzle.id)
        }
    }

    // ── REQ-003 A1: nothing is locked ──────────────────────────────────────────────────────────────

    // REQ-003.A1 — "From any puzzle, every other puzzle can be reached without solving anything." From every
    // listed puzzle (starts that are New, In progress and Solved: kitMixedSeed), every other one is reached
    // through S-2Δ alone — one press on its grid cell (REQ-050 statement: "one press opens that puzzle"),
    // and the › and ‹ walks — and nothing is ever placed, dropped or solved: every puzzle keeps its seeded
    // state throughout (`stateOf`, S-1Δ).
    @Test
    fun fromAnyPuzzleEveryOtherPuzzleCanBeReachedWithoutSolvingAnything() { // REQ-003.A1
        val seed = kitMixedSeed()
        val states = kitSeededStates(seed)
        for (s in PuzzleState.entries) assertTrue("fixture: some start is $s", s in states.values)
        val c = session.start(KitProgressStore(seed)).c
        val ids = puzzles.map { it.id }
        for (start in ids) {
            for (target in ids) {
                if (target == start) continue
                kitChoose(c, start)
                assertEquals("fixture: $start open", start, c.puzzle.id)
                kitChoose(c, target)
                assertEquals("$target reached from $start (${states[start]}) through the grid", target, c.puzzle.id)
                assertEquals("nothing solved or changed on the way from $start to $target", states, kitStates(c))
            }
            kitChoose(c, start)
            val byNext = List(ids.size - 1) { kitNext(c); c.puzzle.id }
            assertEquals("› from $start reaches every other puzzle once", (ids - start).toSet(), byNext.toSet())
            assertEquals("› from $start: no puzzle twice", ids.size - 1, byNext.distinct().size)
            kitChoose(c, start)
            val byPrevious = List(ids.size - 1) { kitPrevious(c); c.puzzle.id }
            assertEquals("‹ from $start reaches every other puzzle once", (ids - start).toSet(), byPrevious.toSet())
            assertEquals("‹ from $start: no puzzle twice", ids.size - 1, byPrevious.distinct().size)
            assertEquals("nothing solved or changed by the walks from $start", states, kitStates(c))
        }
    }

    // ── REQ-050: the long press and the grid ────────────────────────────────────────────────────────

    // REQ-050.A1 — "With puzzles 3 and 7 unsolved and puzzle 3 open, a long press on › opens puzzle 7." The
    // long press is S-2Δ `nextUnsolved()`. Fixture: every other puzzle Solved, 3 In progress and open (S-1Δ
    // lastOpenedId), 7 New and then In progress (both unsolved, TYPE-006). It discriminates: a plain › from 3
    // opens 4, not 7 (shown on a second controller over the same seed).
    @Test
    fun withPuzzlesThreeAndSevenUnsolvedAndThreeOpenALongPressOnNextOpensSeven() { // REQ-050.A1
        val three = kitNth(3)
        val four = kitNth(4)
        val seven = kitNth(7)
        for (sevenSave in listOf<PuzzleProgress?>(null, kitInProgress(seven, 2))) {
            val seed = LinkedHashMap<String, PuzzleProgress>()
            for (p in puzzles) if (p.id != three.id && p.id != seven.id) seed[p.id] = kitSolved(time = 90, best = 80)
            seed[three.id] = kitInProgress(three, 1)
            if (sevenSave != null) seed[seven.id] = sevenSave
            val sevenState = sevenSave?.state ?: PuzzleState.NEW

            val plain = session.start(KitProgressStore(seed, lastOpened = three.id)).c
            assertEquals("fixture: 3 open", three.id, plain.puzzle.id)
            kitNext(plain)
            assertEquals("fixture: a plain › from 3 opens 4", four.id, plain.puzzle.id)
            assertNotEquals("fixture: 4 is not 7", seven.id, plain.puzzle.id)

            val c = session.start(KitProgressStore(seed, lastOpened = three.id)).c
            assertEquals("fixture: 3 open", three.id, c.puzzle.id)
            for (p in puzzles) {
                val unsolved = p.id == three.id || p.id == seven.id
                assertEquals("fixture: ${p.id} unsolved = $unsolved", unsolved, c.stateOf(p.id) != PuzzleState.SOLVED)
            }
            kitNextUnsolved(c)
            assertEquals("the long press on › opens 7 (7 $sevenState)", seven.id, c.puzzle.id)
            assertEquals("7 opens as it was", sevenState, c.state)
        }
    }

    // REQ-050.A3 — "Choosing a cell opens that puzzle without changing any other puzzle's state." Every cell
    // is chosen in turn (S-2Δ `openOverview()`, `choose(id)`), each from the puzzle the choice before opened,
    // over a seed of New, In-progress and Solved puzzles with times. After each choice, for every other
    // puzzle — not a sample — `stateOf` (S-1Δ) and the saved progress (flushed, S-2Δ `flush()`) are as
    // seeded. The puzzle just left is among them.
    @Test
    fun choosingACellOpensThatPuzzleWithoutChangingAnyOtherPuzzlesState() { // REQ-050.A3
        val seed = kitMixedSeed()
        val states = kitSeededStates(seed)
        val run = session.start(KitProgressStore(seed))
        val c = run.c
        for (target in puzzles + puzzles.reversed()) {
            val from = c.puzzle.id
            kitChoose(c, target.id)
            assertEquals("the cell opens ${target.id} (from $from)", target.id, c.puzzle.id)
            session.flush(run)
            for (other in puzzles) {
                if (other.id == target.id) continue
                assertEquals("${other.id}'s state after choosing ${target.id} from $from", states[other.id], c.stateOf(other.id))
                assertEquals(
                    "${other.id}'s saved progress after choosing ${target.id} from $from",
                    seed[other.id] ?: PuzzleProgress.FRESH,
                    run.store.kitProgressOf(other.id),
                )
            }
        }
    }

    // ── Pins ──────────────────────────────────────────────────────────────────────────────────────

    // seam S-2Δ `nextUnsolved()` — "takes the first non-Solved puzzle after the current one, wrapping: if that
    // is the current one, nothing changes; if every puzzle is solved, it acts as next()". decisions.md
    // 2026-10-15 "Browsing readings no REQ fixes": "When the open puzzle is the only unsolved one, a long press
    // stays on it"; "A retried puzzle counts as unsolved (F23)". REQ-050 rule: "IF every puzzle is solved,
    // THEN the long press shows the next puzzle as a normal press would". "Nothing changes" is read on S-1's
    // snapshot, with a tray piece turned first: an open resets tray turns (S-2 `open`), so a re-open would
    // show. Pin, no token.
    @Test
    fun nextUnsolvedWrapsStaysOnTheOnlyUnsolvedPuzzleAndActsAsNextWhenAllAreSolved() {
        val three = kitNth(3)
        val seven = kitNth(7)
        fun kitSolvedBut(vararg open: String): LinkedHashMap<String, PuzzleProgress> {
            val seed = LinkedHashMap<String, PuzzleProgress>()
            for (p in puzzles) if (p.id !in open) seed[p.id] = kitSolved(time = 90, best = 80)
            return seed
        }

        // Wrapping: 3 and 7 unsolved, 7 open → 3.
        val wrap = session.start(KitProgressStore(kitSolvedBut(three.id, seven.id), lastOpened = seven.id)).c
        assertEquals("fixture: 7 open", seven.id, wrap.puzzle.id)
        kitNextUnsolved(wrap)
        assertEquals("from 7 the first unsolved after it, wrapping, is 3", three.id, wrap.puzzle.id)

        // Only the open puzzle unsolved: nothing changes.
        val only = kitSolvedBut(three.id).also { it[three.id] = kitInProgress(three, 2) }
        val alone = session.start(KitProgressStore(only, lastOpened = three.id)).c
        val turned = alone.trayPieces.first { it != PieceId.PG }
        alone.tapTray(turned)
        assertNotEquals("fixture: $turned is off its resting turn", TrayRest.turn(turned), alone.trayTurn(turned))
        val before = kitSnapshot(alone)
        kitNextUnsolved(alone)
        assertEquals("the only unsolved puzzle is open: nothing changes", before, kitSnapshot(alone))

        // Every puzzle solved: as ›, wrap included.
        val all = kitSolvedBut()
        val solvedRun = session.start(KitProgressStore(all, lastOpened = kitNth(5).id))
        val done = solvedRun.c
        kitNextUnsolved(done)
        assertEquals("all solved: from 5 as ›", kitNth(6).id, done.puzzle.id)
        kitChoose(done, puzzles.last().id)
        kitNextUnsolved(done)
        assertEquals("all solved: from the last as ›", puzzles.first().id, done.puzzle.id)

        // A retried puzzle counts as unsolved.
        val nine = kitNth(9)
        kitChoose(done, nine.id)
        done.retry()
        assertEquals("fixture: Retry gives New (TYPE-006)", PuzzleState.NEW, done.state)
        kitChoose(done, three.id)
        kitNextUnsolved(done)
        assertEquals("from 3 the retried 9 is the next unsolved", nine.id, done.puzzle.id)
    }

    // seam S-1Δ construction — "Opens lastOpenedId if listed, else puzzles.first()"; IProgressStore
    // `lastOpenedPuzzleId` KDoc: "The id may name a puzzle no longer in the library; the caller then falls
    // back to the first puzzle". seam S-20 — "Each open calls saveLastOpened" (decisions.md 2026-10-15 "When
    // progress is saved": "Every open queues the last-opened id"); read after S-2Δ `flush()`. A restart over
    // the same store opens it (S-1Δ). Pin, no token.
    @Test
    fun theStartOpensTheLastOpenedPuzzleIfListedElseTheFirstAndEveryOpenSavesIt() {
        val five = kitNth(5)
        assertEquals("a listed last-opened id", five.id, session.start(KitProgressStore(lastOpened = five.id)).c.puzzle.id)
        assertEquals(
            "an id no longer in the library falls back to the first",
            puzzles.first().id,
            session.start(KitProgressStore(lastOpened = "kit-no-such-puzzle")).c.puzzle.id,
        )
        assertEquals("no saved id opens the first", puzzles.first().id, session.start(KitProgressStore()).c.puzzle.id)

        val seed = kitMixedSeed()
        val run = session.start(KitProgressStore(seed))
        val c = run.c
        val doors = listOf<Pair<String, (PlayController) -> Unit>>(
            "›" to ::kitNext,
            "› again" to ::kitNext,
            "‹" to ::kitPrevious,
            "the long press" to ::kitNextUnsolved,
            "the grid" to { x -> kitChoose(x, kitNth(11).id) },
            "‹ from the first" to { x -> kitChoose(x, puzzles.first().id); kitPrevious(x) },
        )
        for ((door, act) in doors) {
            act(c)
            session.flush(run)
            assertEquals("the last-opened id after $door", c.puzzle.id, run.store.kitLastOpened())
        }
        val left = c.puzzle.id
        val again = session.restart(run)
        assertEquals("a restart opens the last-opened puzzle", left, again.c.puzzle.id)
    }

    // seam S-2Δ — `openOverview()`, `closeOverview()`; "choose(id) closes, then opens id unless it is
    // already open"; S-1Δ read `overviewOpen`. REQ-050 rule: "closing it … returns to the same puzzle state".
    // decisions.md 2026-10-15 "Browsing readings no REQ fixes": "Choosing the open puzzle's own grid cell only
    // closes the grid" — read with a tray piece turned, which an open would reset (S-2 `open`). decisions.md
    // 2026-10-15 "The grid overview, as drawn": "its open state lives in the controller, so it survives a
    // recreation but not a process death" (design §2 "What survives": "The overview is closed"). Pin, no token.
    @Test
    fun theOverviewOpensClosesAndChoosesAndChoosingTheOpenPuzzleOnlyClosesIt() {
        val three = kitNth(3)
        val run = session.start(KitProgressStore(mapOf(three.id to kitInProgress(three, 2)), lastOpened = three.id))
        val c = run.c
        assertFalse("closed at the start", c.overviewOpen)
        val turned = c.trayPieces.first { it != PieceId.PG }
        c.tapTray(turned)
        val before = kitSnapshot(c)

        c.openOverview()
        assertTrue("open after openOverview()", c.overviewOpen)
        assertEquals("opening the overview changes no puzzle state", before, kitSnapshot(c))
        c.closeOverview()
        assertFalse("closed after closeOverview()", c.overviewOpen)
        assertEquals("closing returns to the same puzzle state", before, kitSnapshot(c))

        c.openOverview()
        c.choose(three.id)
        assertFalse("choose closes the overview", c.overviewOpen)
        assertEquals("choosing the open puzzle only closes the grid", before, kitSnapshot(c))

        val other = kitNth(8)
        c.openOverview()
        c.choose(other.id)
        assertFalse("choose closes the overview", c.overviewOpen)
        assertEquals("choose opens the cell's puzzle", other.id, c.puzzle.id)

        c.openOverview()
        assertTrue(c.overviewOpen)
        val again = session.restart(run)
        assertFalse("a restart opens with the overview closed", again.c.overviewOpen)
    }

    // seam S-1Δ reads — "puzzleNumber (1-based) · puzzleCount · stateOf(id)"; REQ-024 rule: the top bar shows
    // "its number out of the total, and its state (TYPE-006)". Along the › walk over a mixed seed, the number
    // is the open puzzle's place in the ‹ › order and `stateOf` of the open puzzle is its `state`; `stateOf`
    // reads every puzzle's saved state. Pin, no token (REQ-024 A2 is the screen's, TASK-019).
    @Test
    fun theCounterReadsTheOpenPuzzlesPlaceAndTheTotalAndStateOfReadsEveryPuzzle() {
        val seed = kitMixedSeed()
        val c = session.start(KitProgressStore(seed)).c
        assertEquals("stateOf every puzzle, as saved", kitSeededStates(seed), kitStates(c))
        for ((i, p) in puzzles.withIndex()) {
            assertEquals("fixture: the walk is at ${p.id}", p.id, c.puzzle.id)
            assertEquals("${p.id}'s number", i + 1, c.puzzleNumber)
            assertEquals("the total", puzzles.size, c.puzzleCount)
            assertEquals("${p.id}: stateOf the open puzzle is its state", c.state, c.stateOf(p.id))
            kitNext(c)
        }
        assertEquals("the walk wrapped to 1", 1, c.puzzleNumber)
    }
}
