// Acceptance kit (WO-005, TASK-023) — the DEV aid on the JVM: DevAid's state and actions (tasks.md § WO-005
// › Seams S-26), "Solve this puzzle now" (S-27) over every bundled puzzle with the save through Persistence
// (designs/WO-004-design.md §2 S-19, S-20) on the kit's in-memory store double (KitStore.kt), and the door's
// `installed` (S-25). The real engine and the shipped puzzles (KitPlay.kt `kitEngine`, `kitLibrary`).
//
// - Tokens: REQ-046 A1 and A3, each on the one test whose assertion is that line's meaning
//   (traceability-rules.md §2). REQ-046 A2 is a screen line and A4 a build-output line; neither is here. Every
//   other test is a pin: it cites a seam row, a decisions.md row (date and bold title), a REQ-046 rule or a TYPE
//   rule, and carries no token.
// - DevAid's reads are Compose snapshot state, "readable from a JVM test without a composition" (S-26); its
//   members are `internal` and are called directly (play/src/test compiles for the debug unit-test variant
//   only, tasks.md § WO-005 seam note 1).
// - What the store holds is read after S-2Δ `flush()` ("delegates to Persistence"); IProgressStore KDoc: "A
//   save is complete when the call returns".
// - Helper names carry a `kit` / `Kit` prefix (KitPlay.kt) and are members of this class, so no name of
//   another kit file or of the scaffolding tests is reused.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.play.devaid.DevAid
import app.tangramplaytime.play.devaid.DevSheet
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class DevAidKitTest {

    private val session = KitSession()

    @After
    fun closeSession() = session.close()

    // ── Kit helpers ──────────────────────────────────────────────────────────────────────────────

    /** The passcode the REQ-046 statement names ("asks for the passcode 0417"). */
    private val kitPasscode = "0417"

    /** Every read of S-26: `unlocked`, `solutionShown`, `sheet`, `wrongPasscode`, and `sheetOpen` ("`sheet != NONE`"). */
    private data class KitAidSeen(
        val unlocked: Boolean,
        val solutionShown: Boolean,
        val sheet: DevSheet,
        val wrongPasscode: Boolean,
        val sheetOpen: Boolean,
    )

    private fun kitAidSeen(aid: DevAid) = KitAidSeen(aid.unlocked, aid.solutionShown, aid.sheet, aid.wrongPasscode, aid.sheetOpen)

    /**
     * Everything a test can observe of a controller: KitPlay.kt `kitSnapshot` (S-1 reads), plus S-1Δ
     * `celebration`, `bestSeconds` and `stateOf` of every listed puzzle — so "nothing changes" can fail.
     */
    private data class KitControllerSeen(
        val snapshot: KitSnapshot,
        val celebration: Celebration?,
        val best: Int?,
        val states: Map<String, PuzzleState>,
    )

    private fun kitControllerSeen(c: PlayController) = KitControllerSeen(kitSnapshot(c), c.celebration, c.bestSeconds, kitStates(c))

    /** A new DevAid unlocked as a tester unlocks it: the DEV button, then the passcode (S-26 `onButton`, `submit`). */
    private fun kitUnlockedAid(): DevAid {
        val aid = DevAid()
        aid.onButton()
        assertTrue("fixture: 0417 is accepted (S-26 submit)", aid.submit(kitPasscode))
        assertTrue("fixture: the aid is unlocked (S-26 submit)", aid.unlocked)
        return aid
    }

    /** Each of [p]'s pieces at its stored solution pose: S-27 step 1, as KitPlay.kt `kitPoseOf` reads it. */
    private fun kitPoses(p: PuzzleDefinition): Map<PieceId, Placement> = p.solution.associate { it.piece to kitPoseOf(it) }

    /**
     * Asserts [c] shows [p] solved by the aid (S-27 "The last lock is the ordinary solve: Solved, a new
     * Celebration, Solved, and the S-20 save"):
     * - still [p] (S-27 opens nothing), Solved, and `stateOf` Solved (S-1Δ);
     * - a celebration for [p] (S-3Δ: "set by drop() at the solve"; the screen plays the picture from it);
     * - every piece locked, each once (TYPE-006: "In progress → Solved when the last piece locks"), each at its
     *   stored solution pose within 1e-6 (S-27 step 2: Locked "at the pose: the centre within 1e-6, and the
     *   same turn and mirror"; step 3: "drop() returns the planned placement"), a board the engine counts
     *   solved (ILockEngine `isSolved`); nothing in the tray or dragged (S-1 `trayPieces`, `dragged`);
     * - the best time [best] (REQ-046 rule 4: the aid "does not set a best time"; S-27 "The best time is never
     *   written: null stays null, and a saved one is kept").
     */
    private fun kitAssertSolvedByTheAid(c: PlayController, p: PuzzleDefinition, best: Int?, how: String) {
        assertEquals("$how: still on ${p.id}", p.id, c.puzzle.id)
        assertEquals("$how: Solved", PuzzleState.SOLVED, c.state)
        assertEquals("$how: stateOf(${p.id})", PuzzleState.SOLVED, c.stateOf(p.id))
        val celebration = c.celebration
            ?: throw AssertionError("$how: the solve sets a Celebration, from which the screen shows the solved picture (S-3Δ, S-27)")
        assertEquals("$how: the celebration is ${p.id}'s", p.id, celebration.puzzleId)
        assertEquals("$how: every piece of ${p.id} on the board", p.pieces, c.placed.map { it.piece }.toSet())
        assertEquals("$how: each piece once", p.pieces.size, c.placed.size)
        val poses = kitPoses(p)
        for (got in c.placed) kitAssertSamePlacement(poses.getValue(got.piece), got, "$how: ${got.piece} at its solution pose", tol = 1e-6)
        assertTrue("$how: the engine counts the board solved", kitEngine.isSolved(p, c.placed))
        assertTrue("$how: nothing left in the tray", c.trayPieces.isEmpty())
        assertNull("$how: nothing dragged", c.dragged)
        assertEquals("$how: the best time", best, c.bestSeconds)
    }

    /** S-20's shape for a Solved puzzle: "PuzzleProgress(state, placed if IN_PROGRESS else [], time, best)". */
    private fun kitSolvedSave(time: Int, best: Int?) = PuzzleProgress(PuzzleState.SOLVED, emptyList(), time, best)

    /**
     * A puzzle with both large triangles and LT1 locked on its empty board at LT2's solution place — a lock the
     * engine finds (`findLock` returns Locked exactly there) and the load check keeps (`stillValid`), away from
     * LT1's own solution pose (TYPE-001: "The two large triangles are interchangeable"). None is a fixture defect.
     */
    private fun kitLockedAwayFromItsPose(): Pair<PuzzleDefinition, Placement> {
        for (p in kitLibrary.puzzles.filter { PieceId.LT1 in it.pieces && PieceId.LT2 in it.pieces }) {
            val own = kitPoseOf(p.solution.first { it.piece == PieceId.LT1 })
            val there = kitPoseOf(SolvedPiece(PieceId.LT1, p.solution.first { it.piece == PieceId.LT2 }.polygon))
            val r = kitEngine.findLock(p, emptyList(), PieceId.LT1, there.turn, there.mirrored, there.centre, KIT_DP_PER_UNIT)
            if (!kitLocksAt(r, there)) continue
            val away = (r as LockResult.Locked).placement
            if (kitNear(away.centre, own.centre, 1e-6)) continue
            if (kitEngine.stillValid(p, listOf(away)) != listOf(away)) continue
            return p to away
        }
        throw AssertionError("fixture defect: no puzzle locks LT1 at LT2's solution place on an empty board")
    }

    // ── REQ-046 A1: the passcode ─────────────────────────────────────────────────────────────────

    // REQ-046.A1 — "Entering 1234 is refused; entering 0417 unlocks the aid." S-26's actions on a new DevAid over
    // a controller on a fresh store: the DEV button opens the prompt (`onButton`: "PROMPT when locked (with
    // wrongPasscode false)"); 1234 is refused (`submit`: "Anything else … sets wrongPasscode, stays on PROMPT,
    // changes nothing else, and returns false"), and REQ-046 rule 1: a wrong passcode "shows 'Wrong passcode.' and
    // reveals nothing" — still locked, the solution off, the menu not shown, and the game unchanged (every S-1 and
    // S-1Δ read, and nothing saved). While locked, Show the solution and Solve this puzzle now refuse (S-26
    // `toggleSolution`, `solveNow`: "While locked: false and no change"), with the sheet closed and on the prompt.
    // 0417 then unlocks (`submit`: exactly "0417" "gives unlocked, MENU, wrongPasscode false, and returns true"),
    // and the unlocked aid's offer acts (`toggleSolution` "flips solutionShown and closes"). The fixture
    // discriminates: the open puzzle is New and has an order (KitPlay.kt `kitPlanOrNull`), so a solve that went
    // through would change it.
    @Test
    fun entering1234IsRefusedAndEntering0417UnlocksTheAid() { // REQ-046.A1
        val run = session.start(KitProgressStore())
        val c = run.c
        val p = c.puzzle
        assertEquals("fixture: ${p.id} is New", PuzzleState.NEW, c.state)
        assertNotNull("fixture: ${p.id} has an order, so a solve now would change it (S-27)", kitPlanOrNull(p))
        val before = kitControllerSeen(c)
        session.flush(run)
        val writesBefore = run.store.kitWrites()
        val aid = DevAid()
        val closed = kitAidSeen(aid)
        assertEquals("fixture: a new aid is locked with no sheet (S-26)", KitAidSeen(false, false, DevSheet.NONE, closed.wrongPasscode, false), closed)

        assertFalse("locked, Show the solution refuses (S-26 toggleSolution)", aid.toggleSolution())
        assertFalse("locked, Solve this puzzle now refuses (S-26 solveNow)", aid.solveNow(c))
        assertEquals("locked, the refusals change nothing in the aid (S-26: \"no change\")", closed, kitAidSeen(aid))
        assertEquals("locked, the refusals change nothing in the game (S-26: \"no change\")", before, kitControllerSeen(c))

        aid.onButton()
        assertEquals("the DEV button opens the prompt while locked (S-26 onButton)", KitAidSeen(false, false, DevSheet.PROMPT, false, true), kitAidSeen(aid))

        assertFalse("1234 is refused (S-26 submit returns false)", aid.submit("1234"))
        val refused = kitAidSeen(aid)
        assertEquals(
            "1234: the prompt stays, shows the Wrong passcode. line, and reveals nothing — still locked, the solution off (REQ-046 rule 1; S-26 submit)",
            KitAidSeen(false, false, DevSheet.PROMPT, true, true),
            refused,
        )
        assertEquals("1234 reveals nothing and changes nothing in the game (REQ-046 rule 1)", before, kitControllerSeen(c))

        assertFalse("after 1234, Show the solution still refuses (S-26 toggleSolution)", aid.toggleSolution())
        assertFalse("after 1234, Solve this puzzle now still refuses (S-26 solveNow)", aid.solveNow(c))
        assertEquals("after 1234, the refusals change nothing in the aid (S-26: \"no change\")", refused, kitAidSeen(aid))
        assertEquals("after 1234, nothing in the game has changed (REQ-046 rule 1; S-26)", before, kitControllerSeen(c))
        session.flush(run)
        assertEquals("the refused entry and the refused offers wrote nothing", writesBefore, run.store.kitWrites())
        assertEquals("the store still holds nothing for ${p.id}", PuzzleProgress.FRESH, run.store.kitProgressOf(p.id))

        assertTrue("0417 is accepted (S-26 submit returns true)", aid.submit(kitPasscode))
        assertEquals("0417 unlocks the aid and opens its menu, without the message (S-26 submit)", KitAidSeen(true, false, DevSheet.MENU, false, true), kitAidSeen(aid))
        assertEquals("unlocking changes nothing in the game", before, kitControllerSeen(c))
        assertTrue("unlocked, Show the solution acts (S-26 toggleSolution)", aid.toggleSolution())
        assertEquals("the solution is shown and the menu closes (S-26 toggleSolution)", KitAidSeen(true, true, DevSheet.NONE, false, false), kitAidSeen(aid))
    }

    // ── REQ-046 A3: Solve this puzzle now ────────────────────────────────────────────────────────

    // REQ-046.A3 — "'Solve this puzzle now' shows the solved picture and leaves the best time empty." Through
    // `DevAid.solveNow` after the accepted entry (S-26). On the JVM the solved picture is what the screen draws it
    // from: the open puzzle Solved with a new Celebration for it (S-3Δ "set by drop() at the solve"; S-27 "The last
    // lock is the ordinary solve"; decisions.md 2026-10-17 "Solve this puzzle now is an ordinary solve": "the full
    // REQ-023 win"), every piece locked at its stored solution pose (kitAssertSolvedByTheAid). The best time empty:
    // `bestSeconds` null (S-1Δ), and after `flush()` the store holds S-20's shape for a Solved puzzle — Solved, no
    // placements, the time as it passed through, best null (REQ-046 rule 4; S-27 "Times pass through"; the
    // PuzzleProgress KDoc: "a solve by the developer aid sets none").
    // - Every puzzle of the library (REQ-046 rationale: "confirm that every puzzle of the library … really is
    //   solvable"), each from New over its own Persistence on the kit double, the board scale as KitPlay.kt sets
    //   it (KitStore.kt `KitSession.start`). The precondition is shown test-side: `kitPlanOrNull` finds an order
    //   for each (REQ-019's rationale says one exists). A puzzle without one is reported as a CONTENT FINDING and
    //   stays in the loop, where its solve must fail; nothing is skipped.
    // - In progress with LT1 locked away from its own pose (at LT2's place) and a time of 95 s: the aid restarts
    //   first, so the time becomes 0 (S-27 step 3: "First restart() if In progress"; "Restart writes 0, as S-2Δ
    //   says").
    // - New with a tray piece turned and the parallelogram mirrored in the tray, once where PG's pose is
    //   unmirrored (the flip must undo the tray mirror) and once where it is mirrored (the flip must not be
    //   pressed): S-27 step 3, "pressBadge() while the parallelogram's tray mirror differs from its pose" and
    //   "twistDrag, the shorter way to the pose's turn" — from the tray turn the player left (TYPE-006: turning a
    //   tray piece does not start the puzzle, so there is no restart).
    @Test
    fun solveThisPuzzleNowShowsTheSolvedPictureAndLeavesTheBestTimeEmpty() { // REQ-046.A3
        val failures = mutableListOf<Throwable>()
        val findings = mutableListOf<String>()
        fun kitLeg(name: String, body: () -> Unit) {
            try {
                body()
            } catch (t: Throwable) {
                failures += AssertionError("$name: $t", t)
            }
        }

        assertTrue("fixture: the library lists puzzles", kitLibrary.puzzles.isNotEmpty())
        for (p in kitLibrary.puzzles) kitLeg("${p.id} from New") {
            if (kitPlanOrNull(p) == null) {
                findings += "CONTENT FINDING: ${p.id} has no order that locks every piece at its solution pose (REQ-019's rationale says one exists); its solve must fail"
            }
            val run = session.start(KitProgressStore(lastOpened = p.id))
            val c = run.c
            assertEquals("fixture: ${p.id} open", p.id, c.puzzle.id)
            assertEquals("fixture: New", PuzzleState.NEW, c.state)
            assertNull("fixture: no best time", c.bestSeconds)
            val aid = kitUnlockedAid()
            assertTrue("Solve this puzzle now solves ${p.id} (S-27)", aid.solveNow(c))
            kitAssertSolvedByTheAid(c, p, best = null, how = "${p.id} from New")
            session.flush(run)
            assertEquals("${p.id}: saved Solved, best time empty", kitSolvedSave(time = 0, best = null), run.store.kitStored(p.id))
        }

        kitLeg("In progress with LT1 locked away from its pose") {
            val (p, away) = kitLockedAwayFromItsPose()
            val own = kitPoseOf(p.solution.first { it.piece == PieceId.LT1 })
            assertFalse(
                "fixture: LT1 lies away from its own solution polygon",
                kitSamePointSet(kitEngine.vertices(away.piece, away.turn, away.mirrored, away.centre), kitEngine.vertices(own.piece, own.turn, own.mirrored, own.centre), 1e-6),
            )
            val run = session.start(KitProgressStore(mapOf(p.id to PuzzleProgress(PuzzleState.IN_PROGRESS, listOf(away), 95, null)), lastOpened = p.id))
            val c = run.c
            assertEquals("fixture: ${p.id} open", p.id, c.puzzle.id)
            assertEquals("fixture: In progress", PuzzleState.IN_PROGRESS, c.state)
            assertEquals("fixture: LT1 on the board away from its pose (S-21 keeps a valid board)", listOf(away), c.placed)
            val aid = kitUnlockedAid()
            assertTrue("Solve this puzzle now solves ${p.id} from In progress (S-27)", aid.solveNow(c))
            kitAssertSolvedByTheAid(c, p, best = null, how = "${p.id} from In progress")
            session.flush(run)
            assertEquals("${p.id}: saved Solved, the Restart's time 0, best time empty", kitSolvedSave(time = 0, best = null), run.store.kitStored(p.id))
        }

        for (poseMirrored in listOf(false, true)) kitLeg("New, a tray piece turned, PG mirrored in the tray, PG's pose ${if (poseMirrored) "mirrored" else "unmirrored"}") {
            val p = kitLibrary.puzzles.lastOrNull { q ->
                PieceId.PG in q.pieces && q.pieces.size > 1 && kitPoseOf(q.solution.first { it.piece == PieceId.PG }).mirrored == poseMirrored
            } ?: throw AssertionError("fixture defect: no puzzle whose parallelogram's solution pose is ${if (poseMirrored) "" else "un"}mirrored")
            val run = session.start(KitProgressStore(lastOpened = p.id))
            val c = run.c
            assertEquals("fixture: ${p.id} open", p.id, c.puzzle.id)
            val turned = c.trayPieces.first { it != PieceId.PG }
            c.tapTray(turned)
            kitMirrorInTray(c, true)
            assertEquals("fixture: still New (TYPE-006: turning a piece in the tray does not start it)", PuzzleState.NEW, c.state)
            assertNotEquals("fixture: $turned is off its resting turn", TrayRest.turn(turned), c.trayTurn(turned))
            assertTrue("fixture: PG mirrored in the tray", c.trayMirrored(PieceId.PG))
            val aid = kitUnlockedAid()
            assertTrue("Solve this puzzle now solves ${p.id} (S-27)", aid.solveNow(c))
            kitAssertSolvedByTheAid(c, p, best = null, how = "${p.id} from New with $turned turned and PG mirrored")
            session.flush(run)
            assertEquals("${p.id}: saved Solved, best time empty", kitSolvedSave(time = 0, best = null), run.store.kitStored(p.id))
        }

        if (findings.isNotEmpty() || failures.isNotEmpty()) {
            val e = AssertionError(
                (findings + failures.map { it.message ?: it.toString() })
                    .joinToString("\n", prefix = "${findings.size} content finding(s), ${failures.size} failed leg(s):\n"),
            )
            failures.forEach { e.addSuppressed(it) }
            throw e
        }
    }

    // ── Pins ──────────────────────────────────────────────────────────────────────────────────────

    // seam S-26 — "A new DevAid is locked, with solutionShown false and the sheet NONE"; `sheetOpen` is "sheet !=
    // NONE". tasks.md § WO-005 "What survives": "A process death builds a new aid: locked, the overlay off, no
    // sheet" (REQ-046 rule 2 as read: "until the app is restarted"), so the state is the instance's: an aid built
    // after another was unlocked and shows the solution is still locked. seam S-25 — `installed` is
    // "ServiceLoader.load(DevAidHook::class.java, …).firstOrNull(), evaluated once per process (by lazy). It is the
    // debug build's DevAid" (S-30's service file); this classpath is the debug unit-test variant's (seam note 1).
    // Pin, no token.
    @Test
    fun aNewAidIsLockedWithTheSolutionOffAndNoSheetAndTheInstalledAidIsADevAid() {
        fun kitAssertNew(aid: DevAid, what: String) {
            assertFalse("$what: locked", aid.unlocked)
            assertFalse("$what: the solution off", aid.solutionShown)
            assertEquals("$what: no sheet", DevSheet.NONE, aid.sheet)
            assertFalse("$what: sheetOpen is sheet != NONE", aid.sheetOpen)
        }
        val first = DevAid()
        kitAssertNew(first, "a new DevAid")
        first.onButton()
        assertTrue("fixture: 0417 unlocks the first aid", first.submit(kitPasscode))
        assertTrue("fixture: the first aid shows the solution", first.toggleSolution())
        assertTrue("fixture: the first aid is unlocked and shows the solution", first.unlocked && first.solutionShown)
        kitAssertNew(DevAid(), "a DevAid built after another was unlocked (a new process builds a new aid)")

        val installed = DevAidHook.installed
        assertNotNull("S-25: installed is found on the debug unit-test classpath (S-30's service file)", installed)
        assertTrue("S-25: installed is the debug build's DevAid, not $installed", installed is DevAid)
        assertSame("S-25: installed is evaluated once per process (by lazy)", installed, DevAidHook.installed)
    }

    // seam S-26 `submit` — exactly "0417" unlocks; "Anything else, the empty entry included, sets wrongPasscode,
    // stays on PROMPT, changes nothing else, and returns false" (REQ-046 rule 1). `onButton` — "PROMPT when locked
    // (with wrongPasscode false)"; `close` — "NONE and wrongPasscode false (Cancel, Done, a tap on the scrim)". The
    // entries a lazy reading would let through: a number equal to 417, a trimmed one, a prefix or a suffix. A
    // second wrong entry in a row keeps the message; no number of wrong entries stops 0417 from unlocking. Pin, no
    // token.
    @Test
    fun everyEntryButExactly0417IsRefusedTheEmptyOneIncluded() {
        val aid = DevAid()
        val prompt = KitAidSeen(false, false, DevSheet.PROMPT, false, true)
        val wrong = KitAidSeen(false, false, DevSheet.PROMPT, true, true)
        val shut = KitAidSeen(false, false, DevSheet.NONE, false, false)
        for (entry in listOf("", "1234", "0", "417", "00417", "04170", "0416", "0418", "7140", "4017", " 0417", "0417 ", "041", "0417\n")) {
            aid.onButton()
            assertEquals("the prompt opens without the message (S-26 onButton)", prompt, kitAidSeen(aid))
            assertFalse("\"$entry\" is refused", aid.submit(entry))
            assertEquals("\"$entry\": the prompt stays with the message, still locked, the solution off", wrong, kitAidSeen(aid))
            aid.close()
            assertEquals("close() after \"$entry\" (S-26 close)", shut, kitAidSeen(aid))
        }
        aid.onButton()
        assertFalse(aid.submit("1111"))
        assertFalse(aid.submit("2222"))
        assertEquals("two wrong entries in a row: the prompt and the message stay", wrong, kitAidSeen(aid))
        assertTrue("after every wrong entry, exactly 0417 still unlocks", aid.submit(kitPasscode))
        assertEquals("unlocked, the menu, no message (S-26 submit)", KitAidSeen(true, false, DevSheet.MENU, false, true), kitAidSeen(aid))
        aid.close()
        assertEquals("close() on the menu (S-26 close)", KitAidSeen(true, false, DevSheet.NONE, false, false), kitAidSeen(aid))
    }

    // REQ-046 rule 2 — "After the right passcode the aid stays unlocked until the page or app is restarted"; S-26
    // `onButton`: "MENU when unlocked". REQ-046 rule 3 — the overlay "stays on while the player browses puzzles until
    // it is switched off"; S-26 `toggleSolution` "flips solutionShown and closes". After the accepted entry and
    // `close()`, every browsing door of S-2Δ / S-2 (›, ‹, the long press on ›, the grid, `open`) leaves the aid
    // unlocked, so the DEV button opens the menu directly, and leaves `solutionShown` as it was — off, then on, then
    // off again once switched off. The touch geometry (the overlay): "`solutionShown` is kept, so the overlay
    // returns on the next unsolved puzzle" — through a player's solve, Retry and the aid's own solve (S-26 solveNow
    // "closes, then S-27"; S-27 changes the controller only). Every door is shown to leave the open puzzle. Pin, no
    // token.
    @Test
    fun theAidStaysUnlockedAndTheSolutionStaysShownWhileThePlayerBrowsesUntilItIsSwitchedOff() {
        val run = session.start(KitProgressStore())
        val c = run.c
        val puzzles = kitLibrary.puzzles
        fun kitAfter(x: PlayController, steps: Int): String = puzzles[(puzzles.indexOfFirst { it.id == x.puzzle.id } + steps) % puzzles.size].id
        val doors = listOf<Pair<String, (PlayController) -> Unit>>(
            "›" to ::kitNext,
            "‹" to ::kitPrevious,
            "the long press on ›" to ::kitNextUnsolved,
            "the grid" to { x -> kitChoose(x, kitAfter(x, 3)) },
            "S-2 open" to { x ->
                assertTrue("open() of a listed puzzle", x.open(kitAfter(x, 5)))
                x.boardDpPerUnit = KIT_DP_PER_UNIT
            },
        )
        val aid = kitUnlockedAid()
        aid.close()
        fun kitWalk(shown: Boolean, phase: String) {
            for ((door, act) in doors) {
                val from = c.puzzle.id
                act(c)
                assertNotEquals("fixture: $door left $from", from, c.puzzle.id)
                assertTrue("$phase, after $door: still unlocked (REQ-046 rule 2)", aid.unlocked)
                assertEquals("$phase, after $door: the solution ${if (shown) "still shown" else "still off"} (REQ-046 rule 3)", shown, aid.solutionShown)
                aid.onButton()
                assertEquals("$phase, after $door: the DEV button opens the menu directly (S-26 onButton)", DevSheet.MENU, aid.sheet)
                assertFalse("$phase, after $door: no message", aid.wrongPasscode)
                aid.close()
            }
        }

        kitWalk(shown = false, phase = "unlocked, the solution off")
        aid.onButton()
        assertTrue("Show the solution (S-26 toggleSolution)", aid.toggleSolution())
        assertTrue("fixture: the solution is shown", aid.solutionShown)
        kitWalk(shown = true, phase = "the solution shown")

        val mini = kitPuzzleOf(PuzzleKind.MINI)
        kitChoose(c, mini.id)
        kitPlanOf(mini).forEach { kitPlace(c, it) }
        assertEquals("fixture: ${mini.id} solved by the player", PuzzleState.SOLVED, c.state)
        assertTrue("a player's solve keeps the solution shown (touch geometry: \"solutionShown is kept\")", aid.solutionShown)
        c.retry()
        assertEquals("fixture: Retry gives New", PuzzleState.NEW, c.state)
        assertTrue("Retry keeps the solution shown (REQ-046 rule 3: until it is switched off)", aid.solutionShown)
        aid.onButton()
        assertTrue("fixture: the aid solves ${mini.id}", aid.solveNow(c))
        assertEquals("fixture: Solved by the aid", PuzzleState.SOLVED, c.state)
        assertTrue("the aid's solve keeps it unlocked (REQ-046 rule 2)", aid.unlocked)
        assertTrue("the aid's solve keeps the solution shown (touch geometry: \"solutionShown is kept\")", aid.solutionShown)
        kitWalk(shown = true, phase = "the solution shown, after the solves")

        aid.onButton()
        assertTrue("Hide the solution (S-26 toggleSolution)", aid.toggleSolution())
        assertEquals("switched off: the solution off and the menu closed (S-26 toggleSolution)", KitAidSeen(true, false, DevSheet.NONE, false, false), kitAidSeen(aid))
        kitWalk(shown = false, phase = "the solution switched off")
    }

    // REQ-046 rule 4 — "A puzzle solved by the aid is marked solved but does not set a best time." S-27 "Times
    // pass through: Restart writes 0, as S-2Δ says. The best time is never written: null stays null, and a saved
    // one is kept"; decisions.md 2026-10-17 "Solve this puzzle now is an ordinary solve": "a saved one is kept".
    // Two starts with a saved best time, as WO-008 will write them (S-20): In progress with two planned locks and a
    // time (the aid's Restart zeroes the time and keeps the best), and New after a Retry (time 0, best kept). Pin,
    // no token.
    @Test
    fun solveNowKeepsASavedBestTime() {
        val inProgress = kitPuzzleOf(PuzzleKind.WARMUP)
        val retried = kitPuzzleOf(PuzzleKind.FULL)
        val starts = listOf(
            inProgress to kitInProgress(inProgress, 2, time = 95, best = 61),
            retried to PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 61),
        )
        for ((p, seeded) in starts) {
            val run = session.start(KitProgressStore(mapOf(p.id to seeded), lastOpened = p.id))
            val c = run.c
            assertEquals("fixture: ${p.id} open", p.id, c.puzzle.id)
            assertEquals("fixture: ${p.id} ${seeded.state}", seeded.state, c.state)
            assertEquals("fixture: the saved best time", 61, c.bestSeconds)
            assertTrue("Solve this puzzle now solves ${p.id} (S-27)", kitUnlockedAid().solveNow(c))
            kitAssertSolvedByTheAid(c, p, best = 61, how = "${p.id} from ${seeded.state} with a best time")
            session.flush(run)
            assertEquals("${p.id}: saved Solved, time 0, the best time kept", kitSolvedSave(time = 0, best = 61), run.store.kitStored(p.id))
        }
    }

    // tasks.md § WO-005 "What survives" — "A process death builds a new aid … An aid-solved puzzle is in the store
    // as Solved with its best time unchanged. It therefore opens Solved: the picture, the solved row, and the best
    // time '–'." Read with the seam-level restart of designs/WO-004-design.md §3 (KitStore.kt
    // `KitSession.restart`): a new Persistence.load and a new controller over the same store. The new controller
    // opens the last-opened puzzle (S-1Δ) Solved with an empty board (S-21: "NEW or SOLVED: an empty board", on
    // every open), its best time unchanged (null, or a saved one), and no cue (WO-004 design §2 "What survives":
    // "no cue plays"). Two puzzles: one never solved (best null), one In progress with a best time. Pin, no token.
    @Test
    fun aNewControllerOverTheSameStoreOpensAnAidSolvedPuzzleSolvedWithAnEmptyBoardAndItsBestTimeUnchanged() {
        val never = kitPuzzleOf(PuzzleKind.MINI, 1)
        val kept = kitPuzzleOf(PuzzleKind.WARMUP, 1)
        val run = session.start(KitProgressStore(mapOf(kept.id to kitInProgress(kept, 1, time = 40, best = 33))))
        val c = run.c
        val aid = kitUnlockedAid()
        kitChoose(c, never.id)
        assertEquals("fixture: ${never.id} New", PuzzleState.NEW, c.state)
        assertTrue("fixture: the aid solves ${never.id}", aid.solveNow(c))
        kitChoose(c, kept.id)
        assertEquals("fixture: ${kept.id} In progress", PuzzleState.IN_PROGRESS, c.state)
        aid.onButton()
        assertTrue("fixture: the aid solves ${kept.id}", aid.solveNow(c))

        val again = session.restart(run)
        fun kitCheck(x: PlayController, p: PuzzleDefinition, best: Int?, how: String) {
            assertEquals("$how: ${p.id} open", p.id, x.puzzle.id)
            assertEquals("$how: Solved", PuzzleState.SOLVED, x.state)
            assertEquals("$how: stateOf", PuzzleState.SOLVED, x.stateOf(p.id))
            assertEquals("$how: an empty board", emptyList<Placement>(), x.placed)
            assertEquals("$how: the best time unchanged", best, x.bestSeconds)
            assertNull("$how: no cue after a restart", x.celebration)
        }
        kitCheck(again.c, kept, 33, "the new controller's start (the last-opened puzzle)")
        kitChoose(again.c, never.id)
        kitCheck(again.c, never, null, "the new controller, through the grid")
        assertFalse("a new process's aid is locked (What survives)", DevAid().unlocked)
    }

    // seam S-27 — "False, and nothing changes, when controller.state is Solved". Three Solved puzzles: one the
    // player solved (its final placements and its celebration still in memory, WO-004 design §3), one seeded
    // Solved with times, and one the aid itself just solved. Nothing changes: every S-1 / S-1Δ read (the
    // celebration's serial included), no event (S-4: events are emitted by the actions), no store write after a
    // flush, and the aid stays unlocked with the solution as it was (S-26 solveNow changes neither). The aid is
    // unlocked (shown by solving the third puzzle), so the refusal is the Solved state's. Pin, no token.
    @Test
    fun solveNowReturnsFalseAndChangesNothingOnASolvedPuzzle() {
        val played = kitPuzzleOf(PuzzleKind.MINI)
        val seeded = kitPuzzleOf(PuzzleKind.FULL, 2)
        val byTheAid = kitPuzzleOf(PuzzleKind.WARMUP, 2)
        val events = mutableListOf<PlayEvent>()
        val run = session.start(KitProgressStore(mapOf(seeded.id to kitSolved(time = 50, best = 45))), events)
        val c = run.c
        val aid = kitUnlockedAid()
        aid.close()

        fun kitRefused(what: String) {
            session.flush(run)
            val writes = run.store.kitWrites()
            val before = kitControllerSeen(c)
            val eventCount = events.size
            val unlockedBefore: Boolean = aid.unlocked
            val shownBefore: Boolean = aid.solutionShown
            aid.onButton()
            assertFalse("$what: Solve this puzzle now refuses on a Solved puzzle (S-27)", aid.solveNow(c))
            assertEquals("$what: nothing changes in the game (S-27)", before, kitControllerSeen(c))
            assertEquals("$what: no event", eventCount, events.size)
            assertEquals("$what: the aid stays unlocked", unlockedBefore, aid.unlocked)
            assertEquals("$what: the solution as it was", shownBefore, aid.solutionShown)
            session.flush(run)
            assertEquals("$what: nothing written", writes, run.store.kitWrites())
        }

        kitChoose(c, played.id)
        kitPlanOf(played).forEach { kitPlace(c, it) }
        assertEquals("fixture: ${played.id} solved by the player", PuzzleState.SOLVED, c.state)
        assertNotNull("fixture: its celebration is set", c.celebration)
        kitRefused("solved by the player")

        kitChoose(c, seeded.id)
        assertEquals("fixture: ${seeded.id} seeded Solved", PuzzleState.SOLVED, c.state)
        assertEquals("fixture: its best time", 45, c.bestSeconds)
        kitRefused("seeded Solved")

        kitChoose(c, byTheAid.id)
        assertEquals("fixture: ${byTheAid.id} New", PuzzleState.NEW, c.state)
        aid.onButton()
        assertTrue("fixture: the unlocked aid solves ${byTheAid.id}", aid.solveNow(c))
        assertEquals("fixture: Solved by the aid", PuzzleState.SOLVED, c.state)
        kitRefused("solved by the aid")
    }

    // TYPE-006 as locked — "New → In progress when a piece first leaves the tray"; "In progress → Solved when the
    // last piece locks (REQ-022). Restart returns In progress → New". decisions.md 2026-10-17 "Solve this puzzle
    // now is an ordinary solve": "Only TYPE-006's locked transitions are used" — restart() if In progress, then
    // startDrag (New → In progress) … the last lock. S-20: after each action whose PuzzleProgress differs from the
    // cached one, Persistence saves it; S-19 / decisions.md 2026-10-15 "When progress is saved": one FIFO writer
    // drains every queued save, so the double records each write. The writes for the solved puzzle, consecutive
    // equal states collapsed: from New, In progress then Solved, never New → Solved; from In progress, the
    // Restart's New (time 0, best kept), then In progress, then Solved. Every In-progress write's board holds only
    // pieces at their solution poses (S-27 step 3: "drop() returns the planned placement"). Pin, no token.
    @Test
    fun theStoreSeesOnlyTheLockedTransitionsInProgressBeforeSolved() {
        val fromNew = kitPuzzleOf(PuzzleKind.WARMUP, 2)
        val fromInProgress = kitPuzzleOf(PuzzleKind.FULL, 1)
        val run = session.start(
            KitProgressStore(mapOf(fromInProgress.id to kitInProgress(fromInProgress, 2, time = 30, best = null)), lastOpened = fromNew.id),
        )
        val c = run.c
        val aid = kitUnlockedAid()
        fun kitCollapsed(states: List<PuzzleState>): List<PuzzleState> = states.filterIndexed { i, s -> i == 0 || states[i - 1] != s }
        fun kitWritesFor(id: String, from: Int): List<PuzzleProgress> =
            run.store.kitWrites().drop(from).filter { it.member == "saveProgress" && it.puzzleId == id }.map { it.progress!! }

        assertEquals("fixture: ${fromNew.id} open and New", fromNew.id to PuzzleState.NEW, c.puzzle.id to c.state)
        session.flush(run)
        var mark = run.store.kitWrites().size
        assertTrue("the aid solves ${fromNew.id} (S-27)", aid.solveNow(c))
        session.flush(run)
        val newWrites = kitWritesFor(fromNew.id, mark)
        assertEquals(
            "from New: In progress, then Solved (TYPE-006; never New → Solved)",
            listOf(PuzzleState.IN_PROGRESS, PuzzleState.SOLVED),
            kitCollapsed(newWrites.map { it.state }),
        )
        val poses = kitPoses(fromNew)
        for (w in newWrites.filter { it.state == PuzzleState.IN_PROGRESS }) {
            for (q in w.placed) kitAssertSamePlacement(poses.getValue(q.piece), q, "${fromNew.id}: a saved In-progress board piece at its solution pose", tol = 1e-6)
        }

        kitChoose(c, fromInProgress.id)
        assertEquals("fixture: ${fromInProgress.id} In progress", PuzzleState.IN_PROGRESS, c.state)
        session.flush(run)
        mark = run.store.kitWrites().size
        aid.onButton()
        assertTrue("the aid solves ${fromInProgress.id} (S-27)", aid.solveNow(c))
        session.flush(run)
        val ipWrites = kitWritesFor(fromInProgress.id, mark)
        assertEquals(
            "from In progress: the Restart's New, then In progress, then Solved (TYPE-006)",
            listOf(PuzzleState.NEW, PuzzleState.IN_PROGRESS, PuzzleState.SOLVED),
            kitCollapsed(ipWrites.map { it.state }),
        )
        assertEquals("the Restart's save: New, the tray, time 0, best kept (S-2Δ restart; S-27)", PuzzleProgress(PuzzleState.NEW, emptyList(), 0, null), ipWrites.first())
    }

    // seam S-27 — "Events: the ordinary ones (PickedUp, Turned per step, Locked, Solved)"; step 3: per piece
    // "pressBadge() while the parallelogram's tray mirror differs from its pose", "startDrag", "twistDrag, the
    // shorter way to the pose's turn", "moveDrag", "drop() returns the planned placement". S-2 / S-4: `startDrag`
    // emits PickedUp, `twistDrag` one Turned per 45° step, `pressBadge` in the tray one Turned, a lock Locked, the
    // solve Solved. So, from New over every puzzle: one PickedUp and one Locked per piece in lock order, Solved
    // once and last, no WentHome; between a piece's PickedUp and its Locked, exactly the shorter way's number of
    // its Turned (a tie at four steps is four either way); and outside that window only the flip's one Turned of
    // the parallelogram, before it is picked up. Pin, no token.
    @Test
    fun anAidSolveEmitsTheOrdinaryEventsOfItsSteps() {
        for (p in kitLibrary.puzzles) {
            val events = mutableListOf<PlayEvent>()
            val run = session.start(KitProgressStore(lastOpened = p.id), events)
            val c = run.c
            assertEquals("fixture: ${p.id} open", p.id, c.puzzle.id)
            val startTurn = p.pieces.associateWith { c.trayTurn(it) }
            val startMirror = PieceId.PG in p.pieces && c.trayMirrored(PieceId.PG)
            val aid = kitUnlockedAid()
            events.clear()
            assertTrue("the aid solves ${p.id} (S-27)", aid.solveNow(c))
            val order = c.placed.map { it.piece }
            assertEquals("fixture: every piece of ${p.id} locked", p.pieces, order.toSet())
            val texts = events.map(::kitEventText)
            assertEquals(
                "${p.id}: PickedUp and Locked per piece in lock order, then Solved once",
                order.flatMap { listOf("PickedUp $it", "Locked $it") } + "Solved ${p.id}",
                texts.filter { !it.startsWith("Turned ") },
            )
            val poses = kitPoses(p)
            for (x in order) {
                val pose = poses.getValue(x)
                val d = Math.floorMod(pose.turn.step - startTurn.getValue(x).step, 8)
                val twist = min(d, 8 - d)
                val flip = if (x == PieceId.PG && startMirror != pose.mirrored) 1 else 0
                val up = texts.indexOf("PickedUp $x")
                val locked = texts.indexOf("Locked $x")
                assertEquals("${p.id}: $x turns the shorter way from turn ${startTurn.getValue(x).step} to ${pose.turn.step}", twist, texts.subList(up, locked).count { it == "Turned $x" })
                assertEquals("${p.id}: no Turned $x after it locks", 0, texts.drop(locked).count { it == "Turned $x" })
                assertEquals("${p.id}: Turned $x before it is picked up: the flip only", flip, texts.take(up).count { it == "Turned $x" })
            }
        }
    }

    // seam S-27 — "A controller.boardDpPerUnit that is not > 0 is a programming error (S-2): IllegalStateException
    // before anything changes (G-15)"; "The order is planned before anything changes". The values S-2's own pin
    // uses (PlayControllerKitTest: 0, −1, NaN), from In progress (a Restart first would change it) and from New (a
    // first drag would change it): the exception, then every S-1 / S-1Δ read as before and no store write. Pin, no
    // token.
    @Test
    fun aBoardScaleThatIsNotPositiveIsAProgrammingErrorBeforeAnythingChanges() {
        val inProgress = kitPuzzleOf(PuzzleKind.WARMUP, 3)
        val fresh = kitPuzzleOf(PuzzleKind.FULL)
        for (bad in listOf(0.0, -1.0, Double.NaN)) {
            for ((p, seed) in listOf(inProgress to kitInProgress(inProgress, 2, time = 30, best = null), fresh to null)) {
                val run = session.start(KitProgressStore(if (seed == null) emptyMap() else mapOf(p.id to seed), lastOpened = p.id))
                val c = run.c
                assertEquals("fixture: ${p.id} open", p.id, c.puzzle.id)
                assertEquals("fixture: ${p.id} ${seed?.state ?: PuzzleState.NEW}", seed?.state ?: PuzzleState.NEW, c.state)
                val aid = kitUnlockedAid()
                session.flush(run)
                val writes = run.store.kitWrites()
                c.boardDpPerUnit = bad
                val before = kitControllerSeen(c)
                assertThrows("${p.id}, boardDpPerUnit $bad", IllegalStateException::class.java) { aid.solveNow(c) }
                assertEquals("${p.id}, boardDpPerUnit $bad: nothing changed", before, kitControllerSeen(c))
                session.flush(run)
                assertEquals("${p.id}, boardDpPerUnit $bad: nothing written", writes, run.store.kitWrites())
            }
        }
    }
}
