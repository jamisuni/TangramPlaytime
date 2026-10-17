// Acceptance kit (WO-004, TASK-016) — progress that is kept, on the JVM: PlayController (designs/WO-004-
// design.md §2 S-1Δ, S-2Δ, S-20, S-21, S-3Δ) over Persistence (S-19) on the kit's in-memory store double
// (KitStore.kt), with the real engine and the shipped puzzles.
//
// - Tokens: REQ-003 A2, REQ-025 A1, A2 and A3, REQ-026 A1 and A2, each on the one test whose assertion is
//   that line's meaning (traceability-rules.md §2). The recreation leg and the process-death leg of REQ-003
//   A2 and REQ-025 A1 are the device kit's (TASK-019); here the restart is the seam-level one of design §3
//   (one store instance, one writer): flush, then a new `Persistence.load` and a new controller over the
//   same store (KitStore.kt `KitSession.restart`). The other tests are pins: each cites a seam row, a
//   decisions.md row (date and bold title) or a TYPE rule, and carries no token.
// - What the store holds is read after S-2Δ `flush()` ("delegates to Persistence"); the IProgressStore
//   KDoc: "A save is complete when the call returns".
// - Seeded times and best times are what WO-008 will write (S-20: "Times pass through untouched; only
//   Restart and Retry write 0"), so a lost or invented time can fail.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeKitTest {

    private val session = KitSession()

    @After
    fun closeSession() = session.close()

    // ── REQ-003 A2: leaving and returning never loses a puzzle's state ─────────────────────────────

    /** What a test expects of one puzzle when it is open: its TYPE-006 state and its board (piece → placement). */
    private data class KitKept(val state: PuzzleState, val board: Map<PieceId, Placement>)

    /** Asserts [c] shows [p] as [kept]: state, `stateOf`, board, and (unsolved) the rest of its pieces in the tray (S-1). */
    private fun kitAssertKept(c: PlayController, p: PuzzleDefinition, kept: KitKept, how: String) {
        assertEquals("$how: back on ${p.id}", p.id, c.puzzle.id)
        assertEquals("$how: ${p.id}'s state", kept.state, c.state)
        assertEquals("$how: stateOf(${p.id})", kept.state, c.stateOf(p.id))
        assertEquals("$how: ${p.id}'s board", kept.board, kitBoardOf(c))
        if (kept.state != PuzzleState.SOLVED) {
            assertEquals("$how: ${p.id}'s other pieces in the tray", p.pieces - kept.board.keys, c.trayPieces)
        }
    }

    // REQ-003.A2 — "Leaving and returning to a puzzle never loses its state." Four puzzles made through play
    // (S-2): Solved, In progress with two pieces, In progress with an empty board after a missed first drop
    // (TYPE-006 lists no way back to New; decisions.md 2026-10-15 "REQ-025's load check": an empty board
    // "stays In progress"), and New. Each leaves and returns by every door S-2Δ names — › then ‹, ‹ then ›,
    // the long press then the grid, the grid both ways (with a lock on another puzzle while away), the
    // overview opened and closed over it — and by S-2's `open`. Then the seam-level restart (design §3: one
    // store instance, one writer) and every puzzle again. A Solved puzzle's state is Solved with an empty
    // board (S-21: "NEW or SOLVED: an empty board", on every open). "Never" includes while the one writer
    // (S-19) lags behind the controller: every store write here takes a few milliseconds, and nothing is
    // flushed before the restart.
    @Test
    fun leavingAndReturningByEveryDoorNeverLosesAPuzzlesState() { // REQ-003.A2
        val solved = kitPuzzleOf(PuzzleKind.MINI)
        val placed = kitPuzzleOf(PuzzleKind.WARMUP)
        val emptied = kitPuzzleOf(PuzzleKind.WARMUP, 1)
        val untouched = kitPuzzleOf(PuzzleKind.FULL)
        val elsewhere = kitPuzzleOf(PuzzleKind.FULL, 1)
        val run = session.start(KitProgressStore(writeLatencyMs = { 3L }))
        val c = run.c

        kitChoose(c, solved.id)
        kitPlanOf(solved).forEach { kitPlace(c, it) }
        assertEquals("fixture: ${solved.id} solved", PuzzleState.SOLVED, c.state)
        kitChoose(c, placed.id)
        kitPlanOf(placed).take(2).forEach { kitPlace(c, it) }
        val placedBoard = kitBoardOf(c)
        assertEquals("fixture: two planned locks on ${placed.id}", kitByPiece(kitPlanOf(placed).take(2).map { it.target }), placedBoard)
        kitChoose(c, emptied.id)
        val (miss, turn) = kitMissAt(c, KIT_FAR_POINT)
        kitDragFromTray(c, miss, turn, false)
        c.moveDrag(KIT_FAR_POINT, true)
        assertNull("fixture: the first drop misses", c.drop())
        assertEquals("fixture: In progress with an empty board", PuzzleState.IN_PROGRESS, c.state)

        val expected = linkedMapOf(
            solved to KitKept(PuzzleState.SOLVED, emptyMap()),
            placed to KitKept(PuzzleState.IN_PROGRESS, placedBoard),
            emptied to KitKept(PuzzleState.IN_PROGRESS, emptyMap()),
            untouched to KitKept(PuzzleState.NEW, emptyMap()),
        )

        for ((p, kept) in expected) {
            kitChoose(c, p.id)
            kitNext(c)
            assertNotEquals("fixture: › left ${p.id}", p.id, c.puzzle.id)
            kitPrevious(c)
            kitAssertKept(c, p, kept, "› then ‹")

            kitPrevious(c)
            assertNotEquals("fixture: ‹ left ${p.id}", p.id, c.puzzle.id)
            kitNext(c)
            kitAssertKept(c, p, kept, "‹ then ›")

            kitNextUnsolved(c)
            assertNotEquals("fixture: the long press left ${p.id}", p.id, c.puzzle.id)
            kitChoose(c, p.id)
            kitAssertKept(c, p, kept, "the long press, then the grid")

            kitChoose(c, elsewhere.id)
            assertEquals("fixture: the grid left ${p.id}", elsewhere.id, c.puzzle.id)
            kitPlace(c, kitPlanOf(elsewhere)[c.placed.size])
            kitChoose(c, p.id)
            kitAssertKept(c, p, kept, "the grid, a lock on ${elsewhere.id}, the grid")

            c.openOverview()
            c.closeOverview()
            kitAssertKept(c, p, kept, "the overview opened and closed (REQ-050 rule)")

            assertTrue(c.open(elsewhere.id))
            assertTrue(c.open(p.id))
            c.boardDpPerUnit = KIT_DP_PER_UNIT
            kitAssertKept(c, p, kept, "S-2 open, away and back")
        }

        val again = session.restart(run)
        for ((p, kept) in expected) {
            kitChoose(again.c, p.id)
            kitAssertKept(again.c, p, kept, "after a restart over the same store")
            kitNext(again.c)
            kitPrevious(again.c)
            kitAssertKept(again.c, p, kept, "after a restart, › then ‹")
        }
    }

    // ── REQ-025: continue or restart ───────────────────────────────────────────────────────────────

    // REQ-025.A1 — "Leave a puzzle with three pieces placed, come back: the same three pieces are in the same
    // places." Placed through S-2's player actions (KitPlay.kt `kitPlace`); "the same places" is piece, turn,
    // mirror and centre (REQ-025 rule: "Saved per piece: tray or board, position, turn, mirror"; Placement
    // KDoc). The fixtures discriminate: one has the parallelogram mirrored and a turn off the resting turn
    // (kitThreeWithMirroredPg), the other is a full puzzle's first three locks. Leave by ›, come back by ‹,
    // while the store's writes lag behind (a few milliseconds each, S-19's one writer); then the seam-level
    // restart (it opens there: S-1Δ lastOpenedId), and leave and come back again.
    @Test
    fun threePlacedPiecesAreInTheSamePlacesAfterLeavingAndComingBack() { // REQ-025.A1
        val full = kitPuzzleOf(PuzzleKind.FULL)
        for ((p, three) in listOf(kitThreeWithMirroredPg(), full to kitPlanOf(full).take(3))) {
            val run = session.start(KitProgressStore(writeLatencyMs = { 3L }))
            val c = run.c
            kitChoose(c, p.id)
            three.forEach { kitPlace(c, it) }
            val left = kitBoardOf(c)
            assertEquals("fixture: three pieces on ${p.id}", 3, left.size)
            assertEquals("fixture: ${p.id} In progress, not solved", PuzzleState.IN_PROGRESS, c.state)
            assertEquals("fixture: the board is the planned locks", kitByPiece(three.map { it.target }), left)

            kitNext(c)
            assertNotEquals("fixture: › left ${p.id}", p.id, c.puzzle.id)
            kitPrevious(c)
            assertEquals("back on ${p.id}", p.id, c.puzzle.id)
            assertEquals("${p.id}: the same three pieces in the same places", left, kitBoardOf(c))
            assertEquals("${p.id}: the other pieces in the tray", p.pieces - left.keys, c.trayPieces)

            val again = session.restart(run)
            assertEquals("the restart opens ${p.id}", p.id, again.c.puzzle.id)
            assertEquals("${p.id} after a restart: the same three pieces in the same places", left, kitBoardOf(again.c))
            kitNext(again.c)
            kitPrevious(again.c)
            assertEquals("${p.id} after a restart, › then ‹: the same places", left, kitBoardOf(again.c))
        }
    }

    // REQ-025.A2 — "After Restart, all the puzzle's pieces are in the tray and the puzzle time is zero." S-2Δ
    // `restart()` (In progress) "give[s] New, an empty board, time 0, best kept, the tray at rest"; REQ-025
    // statement: "set the puzzle to New"; the time is read from the store after a flush (S-20: "only Restart
    // and Retry write 0"; IProgressStore `saveProgress` KDoc: the caller "keeps bestSeconds on Restart"). The
    // seeded time and best are what WO-008 will write, so "time is zero" can fail (decisions.md 2026-10-15
    // "When progress is saved"). Two paths: a seeded half-done puzzle with a tray piece turned and the
    // parallelogram mirrored in the tray, and a board placed through play.
    @Test
    fun afterRestartEveryPieceIsInTheTrayAndThePuzzleTimeIsZero() { // REQ-025.A2
        val p = kitPuzzleOf(PuzzleKind.WARMUP)
        val run = session.start(KitProgressStore(mapOf(p.id to kitInProgress(p, 3, time = 95, best = 61)), lastOpened = p.id))
        val c = run.c
        assertEquals("fixture: ${p.id} open", p.id, c.puzzle.id)
        assertEquals("fixture: In progress", PuzzleState.IN_PROGRESS, c.state)
        assertEquals("fixture: three pieces placed", 3, c.placed.size)
        assertEquals("fixture: the seeded best time", 61, c.bestSeconds)
        val turned = c.trayPieces.first { it != PieceId.PG }
        c.tapTray(turned)
        assertTrue("fixture: PG in the tray", PieceId.PG in c.trayPieces)
        kitMirrorInTray(c, true)

        c.restart()
        kitAssertAllInTrayAtRest(c, p, "after Restart")
        assertEquals("New after Restart (REQ-025 statement)", PuzzleState.NEW, c.state)
        assertEquals("the best time is kept", 61, c.bestSeconds)
        session.flush(run)
        assertEquals("saved: New, the tray, time zero, best kept", PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 61), run.store.kitStored(p.id))

        val again = session.restart(run)
        kitAssertAllInTrayAtRest(again.c, p, "after Restart and a restart")
        assertEquals(PuzzleState.NEW, again.c.state)

        val played = kitPuzzleOf(PuzzleKind.FULL)
        kitChoose(again.c, played.id)
        kitPlanOf(played).take(2).forEach { kitPlace(again.c, it) }
        assertEquals("fixture: In progress through play", PuzzleState.IN_PROGRESS, again.c.state)
        again.c.restart()
        kitAssertAllInTrayAtRest(again.c, played, "after Restart of a board placed through play")
        assertEquals(PuzzleState.NEW, again.c.state)
        session.flush(again)
        assertEquals("saved: time zero", 0, again.store.kitStored(played.id).timeSeconds)
        assertEquals("saved: the tray", emptyList<Placement>(), again.store.kitStored(played.id).placed)
    }

    // REQ-025.A3 — "A saved piece that overlaps the current silhouette's edge is in the tray after loading;
    // the others stay." The seeded In-progress board (kitCrossingFixture) holds three planned locks and one
    // piece moved across the outline, second in the list; the engine shows the moved piece alone is not a
    // valid lock while its centre still lies inside its own lock, and that the other three are valid. The
    // load check runs "On every open, the start included" (S-21): the puzzle is loaded at the start (S-1Δ
    // lastOpenedId) and a second one through the grid. The state stays In progress (S-21); "A correction is
    // saved" (S-21), so a restart loads the corrected board.
    @Test
    fun aSavedPieceOverlappingTheSilhouettesEdgeIsInTheTrayAfterLoadingAndTheOthersStay() { // REQ-025.A3
        val atStart = kitCrossingFixture(kitPuzzleOf(PuzzleKind.WARMUP))
        val onOpen = kitCrossingFixture(kitPuzzleOf(PuzzleKind.FULL))
        for (f in listOf(atStart, onOpen)) {
            assertTrue("fixture: ${f.crossing.piece} alone is not a valid lock", kitEngine.stillValid(f.puzzle, listOf(f.crossing)).isEmpty())
            val ownLock = kitPlanOf(f.puzzle).first { it.piece == f.crossing.piece }.target
            assertTrue(
                "fixture: ${f.crossing.piece}'s centre lies inside its own lock, so it covers part of the silhouette",
                kitInsideConvex(f.crossing.centre, kitEngine.vertices(ownLock.piece, ownLock.turn, ownLock.mirrored, ownLock.centre)),
            )
            assertEquals("fixture: the other three are valid locks", f.kept, kitEngine.stillValid(f.puzzle, f.kept))
            assertFalse("fixture: ${f.crossing.piece} is not among the others", f.crossing.piece in f.kept.map { it.piece })
        }
        val seed = mapOf(
            atStart.puzzle.id to PuzzleProgress(PuzzleState.IN_PROGRESS, atStart.saved, 30, null),
            onOpen.puzzle.id to PuzzleProgress(PuzzleState.IN_PROGRESS, onOpen.saved, 45, 40),
        )
        val run = session.start(KitProgressStore(seed, lastOpened = atStart.puzzle.id))
        val c = run.c

        fun kitCheck(c: PlayController, f: KitCrossing, how: String) {
            assertEquals("$how: ${f.puzzle.id} open", f.puzzle.id, c.puzzle.id)
            assertTrue("$how: ${f.crossing.piece} is in the tray", f.crossing.piece in c.trayPieces)
            assertEquals("$how: the others stay, in the same places", kitByPiece(f.kept), kitBoardOf(c))
            assertEquals("$how: still In progress (S-21)", PuzzleState.IN_PROGRESS, c.state)
        }
        kitCheck(c, atStart, "loaded at the start")
        kitChoose(c, onOpen.puzzle.id)
        kitCheck(c, onOpen, "loaded through the grid")

        session.flush(run)
        assertEquals("the correction is saved (S-21)", kitCorrected(atStart, 30, null), run.store.kitStored(atStart.puzzle.id))
        assertEquals("the correction is saved (S-21)", kitCorrected(onOpen, 45, 40), run.store.kitStored(onOpen.puzzle.id))
        val again = session.restart(run)
        kitChoose(again.c, atStart.puzzle.id)
        kitCheck(again.c, atStart, "after a restart")
    }

    /**
     * The corrected save of [x]: In progress, the kept pieces in their saved order (ILockEngine `stillValid`
     * KDoc: "the kept placements, in their original order"), the times as seeded (S-20, S-21).
     */
    private fun kitCorrected(x: KitCrossing, time: Int, best: Int?) = PuzzleProgress(PuzzleState.IN_PROGRESS, x.kept, time, best)

    // ── REQ-026: the solved view ───────────────────────────────────────────────────────────────────

    // REQ-026.A1 — "A solved puzzle never shows where the pieces were." A Solved puzzle opens with an empty
    // board (S-21: "NEW or SOLVED: an empty board", "On every open, the start included"; design §3: "Final
    // placements still in memory are drawn only while their celebration's serial plays", and an open clears
    // the celebration, S-3Δ). Every way back to a solved puzzle: › and ‹, the grid, S-2 `open`, the restart
    // (the start opens it), and a seeded Solved save that still lists placements (an older save); the
    // correction is saved (S-21). The store never keeps a Solved puzzle's placements (S-20: "placed if
    // IN_PROGRESS else []").
    @Test
    fun aSolvedPuzzleNeverShowsWhereItsPiecesWere() { // REQ-026.A1
        val mini = kitPuzzleOf(PuzzleKind.MINI)
        val old = kitPuzzleOf(PuzzleKind.FULL)
        val stale = PuzzleProgress(PuzzleState.SOLVED, kitPlanOf(old).map { it.target }, 120, 110)
        val run = session.start(KitProgressStore(mapOf(old.id to stale)))
        val c = run.c
        assertEquals("fixture: the fresh start opens ${mini.id}", mini.id, c.puzzle.id)
        kitPlanOf(mini).forEach { kitPlace(c, it) }
        assertEquals("fixture: solved", PuzzleState.SOLVED, c.state)

        fun kitCheck(c: PlayController, p: PuzzleDefinition, how: String) {
            assertEquals("$how: ${p.id} open", p.id, c.puzzle.id)
            assertEquals("$how: still Solved", PuzzleState.SOLVED, c.state)
            assertEquals("$how: no piece positions", emptyList<Placement>(), c.placed)
            assertNull("$how: no celebration to draw the final placements with (S-3Δ)", c.celebration)
        }
        kitNext(c)
        kitPrevious(c)
        kitCheck(c, mini, "› then ‹")
        kitPrevious(c)
        kitNext(c)
        kitCheck(c, mini, "‹ then ›")
        kitChoose(c, old.id)
        kitCheck(c, old, "a stale Solved save, through the grid")
        kitChoose(c, mini.id)
        kitCheck(c, mini, "the grid")
        assertTrue(c.open(old.id))
        assertTrue(c.open(mini.id))
        kitCheck(c, mini, "S-2 open")

        session.flush(run)
        assertEquals("the store holds no placements for ${mini.id} (S-20)", emptyList<Placement>(), run.store.kitStored(mini.id).placed)
        assertEquals("the stale save is corrected (S-21)", PuzzleProgress(PuzzleState.SOLVED, emptyList(), 120, 110), run.store.kitStored(old.id))

        val again = session.restart(run)
        kitCheck(again.c, mini, "the start after a restart")
        kitChoose(again.c, old.id)
        kitCheck(again.c, old, "the stale save after a restart")
    }

    // REQ-026.A2 — "Retry leads to an empty silhouette with all pieces in the tray." S-2Δ `retry()` (Solved)
    // "give[s] New, an empty board, time 0, best kept, the tray at rest"; TYPE-006: "Retry returns Solved →
    // New (the best time is kept)"; REQ-026 rule: "Retry keeps the best time"; REQ-026 statement: Retry
    // starts "the puzzle again" — a tray piece can be picked up (S-2 `startDrag` refuses only while Solved).
    // Two paths: Retry right after the solve, while the final placements are still in memory (design §3) and
    // the celebration is set (S-3Δ: cleared by `retry`); and Retry on a seeded Solved puzzle with times.
    @Test
    fun retryLeadsToAnEmptySilhouetteWithEveryPieceInTheTray() { // REQ-026.A2
        val mini = kitPuzzleOf(PuzzleKind.MINI)
        val seeded = kitPuzzleOf(PuzzleKind.WARMUP)
        val run = session.start(KitProgressStore(mapOf(seeded.id to kitSolved(time = 75, best = 61))))
        val c = run.c
        kitPlanOf(mini).forEach { kitPlace(c, it) }
        assertEquals("fixture: solved", PuzzleState.SOLVED, c.state)
        assertEquals("fixture: the final placements are in memory", mini.pieces, c.placed.map { it.piece }.toSet())
        assertNotNull("fixture: the celebration is set", c.celebration)

        c.retry()
        kitAssertAllInTrayAtRest(c, mini, "Retry right after the solve")
        assertEquals("New after Retry (TYPE-006)", PuzzleState.NEW, c.state)
        assertNull("Retry clears the celebration (S-3Δ)", c.celebration)
        val first = mini.pieces.first()
        assertTrue("the puzzle starts again: $first can be picked up", c.startDrag(first, KIT_TRAY_POINT))
        c.goHome()

        kitChoose(c, seeded.id)
        assertEquals("fixture: ${seeded.id} Solved", PuzzleState.SOLVED, c.state)
        assertEquals("fixture: its best time", 61, c.bestSeconds)
        c.retry()
        kitAssertAllInTrayAtRest(c, seeded, "Retry on a seeded Solved puzzle")
        assertEquals("New after Retry (TYPE-006)", PuzzleState.NEW, c.state)
        assertEquals("Retry keeps the best time", 61, c.bestSeconds)
        session.flush(run)
        assertEquals("saved: New, empty, time 0, best kept", PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 61), run.store.kitStored(seeded.id))
        val again = session.restart(run)
        kitAssertAllInTrayAtRest(again.c, seeded, "Retry, then a restart")
    }

    // ── Pins ──────────────────────────────────────────────────────────────────────────────────────

    // seam S-21 — "IN_PROGRESS: stillValid(puzzle, placed.filter { it.piece in puzzle.pieces }.distinctBy {
    // it.piece }); the state stays IN_PROGRESS even if the board comes back empty"; "A correction is saved".
    // decisions.md 2026-10-15 "REQ-025's load check": "the controller drops saved pieces that are not the
    // puzzle's and keeps only the first of any duplicate. A puzzle whose board the check empties stays In
    // progress" (WO-004 checkpoint item 5, default standing). The fixtures discriminate (KitStore.kt): the
    // foreign piece is a lock stillValid alone would keep and it overlaps the mini's own piece, so only the
    // filter applied first keeps the own piece; both duplicates are kept by stillValid alone. Pin, no token.
    @Test
    fun theLoadCheckDropsForeignAndDuplicatePiecesAndAnEmptiedBoardStaysInProgress() {
        val foreign = kitForeignFixture()
        val dup = kitDuplicateFixture()
        val emptied = kitCrossingFixture(kitPuzzleOf(PuzzleKind.WARMUP, 2))
        val bare = kitPuzzleOf(PuzzleKind.FULL, 2)
        assertEquals("fixture: stillValid keeps the foreign piece alone", listOf(foreign.foreign), kitEngine.stillValid(foreign.puzzle, listOf(foreign.foreign)))
        assertEquals("fixture: the foreign piece overlaps the own one", listOf(foreign.foreign), kitEngine.stillValid(foreign.puzzle, listOf(foreign.foreign, foreign.own)))
        assertEquals("fixture: stillValid keeps both duplicates", listOf(dup.first, dup.second), kitEngine.stillValid(dup.puzzle, listOf(dup.first, dup.second)))
        assertNotEquals("fixture: the duplicates are at two places", dup.first.centre, dup.second.centre)
        val seed = mapOf(
            foreign.puzzle.id to PuzzleProgress(PuzzleState.IN_PROGRESS, listOf(foreign.foreign, foreign.own), 5, null),
            dup.puzzle.id to PuzzleProgress(PuzzleState.IN_PROGRESS, listOf(dup.first, dup.second), 6, null),
            emptied.puzzle.id to PuzzleProgress(PuzzleState.IN_PROGRESS, listOf(emptied.crossing), 7, null),
            bare.id to PuzzleProgress(PuzzleState.IN_PROGRESS, emptyList(), 8, null),
        )
        val run = session.start(KitProgressStore(seed))
        val c = run.c

        kitChoose(c, foreign.puzzle.id)
        assertEquals("a piece that is not the puzzle's is dropped", listOf(foreign.own), c.placed)
        assertFalse("the foreign piece is nowhere", foreign.foreign.piece in c.trayPieces)
        kitChoose(c, dup.puzzle.id)
        assertEquals("only the first of a duplicate is kept", listOf(dup.first), c.placed)
        assertTrue("LT2 is in the tray", PieceId.LT2 in c.trayPieces)
        kitChoose(c, emptied.puzzle.id)
        assertTrue("the check empties the board", c.placed.isEmpty())
        assertEquals("an emptied board stays In progress", PuzzleState.IN_PROGRESS, c.state)
        kitChoose(c, bare.id)
        assertEquals("an In-progress save with an empty board stays In progress", PuzzleState.IN_PROGRESS, c.state)

        session.flush(run)
        assertEquals(PuzzleProgress(PuzzleState.IN_PROGRESS, listOf(foreign.own), 5, null), run.store.kitStored(foreign.puzzle.id))
        assertEquals(PuzzleProgress(PuzzleState.IN_PROGRESS, listOf(dup.first), 6, null), run.store.kitStored(dup.puzzle.id))
        assertEquals(PuzzleProgress(PuzzleState.IN_PROGRESS, emptyList(), 7, null), run.store.kitStored(emptied.puzzle.id))
        assertEquals(PuzzleProgress(PuzzleState.IN_PROGRESS, emptyList(), 8, null), run.store.kitProgressOf(bare.id))
        val again = session.restart(run)
        kitChoose(again.c, emptied.puzzle.id)
        assertEquals("still In progress after a restart", PuzzleState.IN_PROGRESS, again.c.state)
    }

    // seam S-21 — "NEW or SOLVED: an empty board"; "A correction is saved"; S-20 — times pass through
    // untouched. A New save that lists placements (not a state TYPE-006 reaches; an older or damaged save)
    // opens with every piece in the tray. (A Solved one is REQ-026 A1's test.) Pin, no token.
    @Test
    fun aNewSaveThatListsPlacementsOpensWithAnEmptyBoardAndIsCorrected() {
        val p = kitPuzzleOf(PuzzleKind.WARMUP, 3)
        val run = session.start(KitProgressStore(mapOf(p.id to PuzzleProgress(PuzzleState.NEW, kitBoard(p, 2), 0, 33)), lastOpened = p.id))
        val c = run.c
        assertEquals("fixture: ${p.id} open", p.id, c.puzzle.id)
        assertEquals("New", PuzzleState.NEW, c.state)
        assertEquals("an empty board", emptyList<Placement>(), c.placed)
        assertEquals("every piece in the tray", p.pieces, c.trayPieces)
        session.flush(run)
        assertEquals("the correction is saved", PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 33), run.store.kitStored(p.id))
    }

    // seam S-20 — "After each action: if PuzzleProgress(state, placed if IN_PROGRESS else [], time, best)
    // differs from the cached one, Persistence.save it." decisions.md 2026-10-15 "When progress is saved": "a
    // tray drag start (New → In progress), a lock, a go-home that changes the board, … the solve, Restart,
    // Retry"; "Tray turns and the mirror are not saved (F36)" (decisions.md line 29). Each step is read from
    // the store after a flush and compared with the controller's own state and board. Pin, no token.
    @Test
    fun everyChangeOfStateOrBoardIsSavedInTheShapeOfTheSaveRule() {
        val p = kitPuzzleOf(PuzzleKind.WARMUP)
        val plan = kitPlanOf(p)
        val run = session.start(KitProgressStore())
        val c = run.c
        kitChoose(c, p.id)
        fun kitSaved(): PuzzleProgress {
            session.flush(run)
            return run.store.kitProgressOf(p.id)
        }
        fun kitExpect(state: PuzzleState, placed: List<Placement>) = PuzzleProgress(state, placed, 0, null)

        kitTurnInTray(c, plan[0].piece, plan[0].target.turn)
        assertEquals("a tray turn saves nothing new", PuzzleProgress.FRESH, kitSaved())
        assertTrue(c.startDrag(plan[0].piece, KIT_TRAY_POINT))
        assertEquals("a tray drag start: In progress", kitExpect(PuzzleState.IN_PROGRESS, emptyList()), kitSaved())
        c.moveDrag(plan[0].dropCentre, true)
        assertNotNull("fixture: the planned drop locks", c.drop())
        assertEquals("a lock", kitExpect(PuzzleState.IN_PROGRESS, c.placed.toList()), kitSaved())
        kitPlace(c, plan[1])
        assertEquals("a second lock", kitExpect(PuzzleState.IN_PROGRESS, c.placed.toList()), kitSaved())
        assertEquals("fixture: two on the board", 2, c.placed.size)

        val top = c.placed.last()
        assertTrue(c.startDrag(top.piece, top.centre))
        c.moveDrag(KIT_TRAY_POINT, false)
        assertNull("fixture: dropped on the tray", c.drop())
        assertEquals("a go-home that changes the board", kitExpect(PuzzleState.IN_PROGRESS, c.placed.toList()), kitSaved())
        assertEquals("fixture: one on the board", 1, c.placed.size)

        for (step in plan.drop(1)) kitPlace(c, step)
        assertEquals("fixture: solved", PuzzleState.SOLVED, c.state)
        assertEquals("the solve: Solved, no placements", kitExpect(PuzzleState.SOLVED, emptyList()), kitSaved())
        c.retry()
        assertEquals("Retry", kitExpect(PuzzleState.NEW, emptyList()), kitSaved())
        kitPlace(c, plan[0])
        assertEquals("a lock after Retry", kitExpect(PuzzleState.IN_PROGRESS, c.placed.toList()), kitSaved())
        c.restart()
        assertEquals("Restart", kitExpect(PuzzleState.NEW, emptyList()), kitSaved())
    }

    // decisions.md 2026-10-15 "O-04 read for this WO" (the TYPE-006 transitions stay in PlayController; time
    // accounting is WO-008's) and seam S-20 — "Times pass through untouched; only Restart and Retry write 0
    // (O-04)"; decisions.md 2026-10-15 "When progress is saved": "`time` and `best` pass through as read".
    // S-1Δ read `bestSeconds`. IProgressStore KDoc: "The store keeps what it is given". Pin, no token.
    @Test
    fun timesPassThroughUntouchedAndOnlyRestartAndRetryWriteZero() {
        val p = kitPuzzleOf(PuzzleKind.WARMUP, 1)
        val plan = kitPlanOf(p)
        val q = kitPuzzleOf(PuzzleKind.FULL, 3)
        val run = session.start(
            KitProgressStore(
                mapOf(p.id to kitInProgress(p, 1, time = 42, best = 17), q.id to kitSolved(time = 50, best = 45)),
                lastOpened = p.id,
            ),
        )
        val c = run.c
        assertEquals("the best time read", 17, c.bestSeconds)
        kitPlace(c, plan[1])
        session.flush(run)
        assertEquals("a lock passes the times through", PuzzleProgress(PuzzleState.IN_PROGRESS, c.placed.toList(), 42, 17), run.store.kitStored(p.id))
        kitNext(c)
        kitPrevious(c)
        plan.drop(2).forEach { kitPlace(c, it) }
        assertEquals("fixture: solved", PuzzleState.SOLVED, c.state)
        session.flush(run)
        assertEquals("the solve passes the times through", PuzzleProgress(PuzzleState.SOLVED, emptyList(), 42, 17), run.store.kitStored(p.id))
        c.retry()
        session.flush(run)
        assertEquals("Retry writes time 0 and keeps the best", PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 17), run.store.kitStored(p.id))

        kitChoose(c, q.id)
        assertEquals("a Solved puzzle's best time", 45, c.bestSeconds)
        session.flush(run)
        assertEquals("opening passes the times through", kitSolved(time = 50, best = 45), run.store.kitProgressOf(q.id))
        val fresh = kitNth(2)
        kitChoose(c, fresh.id)
        assertNull("no best time is invented (design §3: \"Times are never invented\")", c.bestSeconds)
    }

    // seam S-19 — `Persistence(store, scope, saved, lastOpenedId)` "holds the cache and the one FIFO writer:
    // save(id, progress), saveLastOpened(id), suspend fun flush()". Design §3: "one writer (S-19), which
    // rewrites the whole document from memory in FIFO order. Never `launch` once per save: the launches can
    // reorder, and a stale board would win." The double's earlier writes take longer than later ones, so
    // writes run side by side would complete out of step; the store must still end with the latest progress
    // of every puzzle and the latest last-opened id, with one write in flight at a time, each puzzle's writes
    // landing in the order they were saved. Pin, no token.
    @Test
    fun oneFifoWriterEndsWithTheLatestProgressOfEveryPuzzleWhenWritesCompleteOutOfStep() {
        val puzzles = listOf(kitPuzzleOf(PuzzleKind.WARMUP), kitPuzzleOf(PuzzleKind.FULL), kitPuzzleOf(PuzzleKind.WARMUP, 1))
        val rounds = puzzles.associate { p -> p.id to (1..4).map { n -> kitInProgress(p, n, time = n, best = null) } }
        val store = KitProgressStore(writeLatencyMs = { k -> (2L * (30 - k)).coerceAtLeast(0L) })
        val persistence = Persistence(store, session.scope(), emptyMap<String, PuzzleProgress>(), null)
        runBlocking<Unit> {
            for (round in 0 until 4) {
                for (p in puzzles) {
                    persistence.save(p.id, rounds.getValue(p.id)[round])
                    persistence.saveLastOpened(p.id)
                }
            }
            withTimeout<Unit>(KIT_IO_TIMEOUT_MS) { persistence.flush() }
        }
        for (p in puzzles) {
            assertEquals("${p.id}: the latest progress", rounds.getValue(p.id).last(), store.kitStored(p.id))
            val landed = store.kitWrites().filter { it.member == "saveProgress" && it.puzzleId == p.id }.map { rounds.getValue(p.id).indexOf(it.progress) }
            assertEquals("${p.id}: the writes landed in the order saved", landed.sorted(), landed)
        }
        assertEquals("the latest last-opened id", puzzles.last().id, store.kitLastOpened())
        assertEquals("one write in flight at a time", 1, store.kitMaxInFlight())
    }

    // seam S-19 (as above) under the controller — S-20: every action's save goes through Persistence ("Every
    // store write goes through it"). A play script across three puzzles, with the double's earlier writes
    // slower than later ones; after a flush the store holds each puzzle's latest state and board (S-20's
    // shape: placed only In progress) and the last-opened id, and a restart shows them. Pin, no token.
    @Test
    fun theControllersSavesEndWithTheLatestBoardOfEveryPuzzleWhenWritesCompleteOutOfStep() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val b = kitPuzzleOf(PuzzleKind.FULL)
        val mini = kitPuzzleOf(PuzzleKind.MINI, 1)
        val store = KitProgressStore(writeLatencyMs = { k -> (2L * (40 - k)).coerceAtLeast(0L) })
        val run = session.start(store)
        val c = run.c
        kitChoose(c, a.id)
        kitPlanOf(a).take(3).forEach { kitPlace(c, it) }
        kitChoose(c, b.id)
        kitPlanOf(b).take(2).forEach { kitPlace(c, it) }
        kitChoose(c, a.id)
        val top = c.placed.last()
        assertTrue(c.startDrag(top.piece, top.centre))
        c.moveDrag(KIT_TRAY_POINT, false)
        assertNull("fixture: dropped on the tray", c.drop())
        kitChoose(c, mini.id)
        kitPlanOf(mini).forEach { kitPlace(c, it) }
        kitChoose(c, b.id)
        kitPlace(c, kitPlanOf(b)[2])

        val expected = mapOf(
            a.id to PuzzleProgress(PuzzleState.IN_PROGRESS, kitPlanOf(a).take(2).map { it.target }, 0, null),
            b.id to PuzzleProgress(PuzzleState.IN_PROGRESS, kitPlanOf(b).take(3).map { it.target }, 0, null),
            mini.id to PuzzleProgress(PuzzleState.SOLVED, emptyList(), 0, null),
        )
        session.flush(run)
        for ((id, progress) in expected) {
            val got = store.kitStored(id)
            assertEquals("$id: the latest state", progress.state, got.state)
            assertEquals("$id: the latest board", kitByPiece(progress.placed), kitByPiece(got.placed))
        }
        assertEquals("the latest last-opened id", b.id, store.kitLastOpened())
        assertEquals("one write in flight at a time", 1, store.kitMaxInFlight())

        val again = session.restart(run)
        assertEquals(b.id, again.c.puzzle.id)
        for ((id, progress) in expected) {
            kitChoose(again.c, id)
            assertEquals("$id after a restart", progress.state, again.c.state)
            assertEquals("$id's board after a restart", kitByPiece(progress.placed), kitBoardOf(again.c))
        }
    }

    // seam S-3Δ — "Celebration(puzzleId, serial), set by drop() at the solve and cleared by open or retry";
    // "SolveDriver plays a new serial once". decisions.md 2026-10-12 "Carried to WO-004 #Browsing" (2): "solve
    // A, solve B, open A, open B replays B's celebration" is the defect a controller-held cue removes.
    // Design §2 "What survives": "A process death keeps only what the store holds … no cue plays". S-1Δ read
    // `celebration`. Pin, no token.
    @Test
    fun aSolveSetsACelebrationThatAnOpenOrRetryClearsAndARestartNeverReplays() {
        val first = kitPuzzleOf(PuzzleKind.MINI)
        val second = kitPuzzleOf(PuzzleKind.MINI, 1)
        val run = session.start(KitProgressStore())
        val c = run.c
        assertNull("none at the start", c.celebration)
        val plan = kitPlanOf(first)
        plan.dropLast(1).forEach { kitPlace(c, it) }
        assertNull("none before the last lock", c.celebration)
        kitPlace(c, plan.last())
        val one = c.celebration ?: throw AssertionError("the solve sets a celebration")
        assertEquals("for the solved puzzle", first.id, one.puzzleId)

        kitChoose(c, second.id)
        assertNull("an open clears it", c.celebration)
        kitPlanOf(second).forEach { kitPlace(c, it) }
        val two = c.celebration ?: throw AssertionError("the second solve sets a celebration")
        assertEquals(second.id, two.puzzleId)
        assertNotEquals("a new serial", one.serial, two.serial)

        kitChoose(c, first.id)
        assertNull("solve A, solve B, open A: none", c.celebration)
        kitChoose(c, second.id)
        assertNull("… open B: none", c.celebration)

        c.retry()
        kitPlanOf(second).forEach { kitPlace(c, it) }
        val three = c.celebration ?: throw AssertionError("a solve after Retry sets a celebration")
        assertNotEquals("a new serial", two.serial, three.serial)
        assertNotEquals("a new serial", one.serial, three.serial)
        c.retry()
        assertNull("Retry clears it", c.celebration)
        kitPlanOf(second).forEach { kitPlace(c, it) }
        assertNotNull(c.celebration)

        val again = session.restart(run)
        assertEquals("the restart opens ${second.id}", second.id, again.c.puzzle.id)
        assertEquals(PuzzleState.SOLVED, again.c.state)
        assertNull("no cue after a restart", again.c.celebration)
    }
}
