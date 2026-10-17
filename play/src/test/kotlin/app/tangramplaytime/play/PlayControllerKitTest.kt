// Acceptance kit (WO-003, TASK-009) — PlayController (tasks.md § WO-003 › Seams S-1…S-4) over the real
// engine and the shipped puzzles, on the JVM.
//
// - Tokens: REQ-002 A1, REQ-012 A2, REQ-018 A3 and REQ-020 A1, each on the one test whose assertion is
//   that line's meaning (traceability-rules.md §2: a token is a coverage claim; prose writes a line
//   without the dot). Every other test pins a seam row (`// seam S-n`), a decisions.md row
//   (`// decision decisions.md line NN`) or a TYPE rule (`// TYPE-006 rule`) and carries no token; the
//   screen-side tokens of the same REQs are the device kit's (TASK-011).
// - The geometry oracle is the engine (O-01, G-04): each expected lock, preview, turn or mirror in place
//   below is the ILockEngine answer for the controller's own state, computed test-side.
// - decisions.md line numbers are as of 2026-10-11 17:17 (rows are only appended).
package app.tangramplaytime.play

import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlayControllerKitTest {

    // ── The fresh start and the tray ───────────────────────────────────────────────────────────

    // seam S-1 — "Construction opens library.puzzles.first(): state New, nothing placed, every piece in the
    // tray at its S-9 resting turn, not mirrored (the fresh start)"; `trayPieces` lists the pieces "neither
    // placed nor dragged, tray order" (TYPE-001 tray order = PieceId declaration order, Types.kt).
    // S-2 `updatePreview`: null with no drag. Pin, no token.
    @Test
    fun aNewControllerOpensTheFirstListedPuzzleNewWithEveryPieceInTheTrayAtRest() {
        val c = kitController()
        val first = kitLibrary.puzzles.first()
        assertEquals("the opened puzzle", first.id, c.puzzle.id)
        assertEquals(PuzzleState.NEW, c.state)
        assertEquals(emptyList<Placement>(), c.placed)
        assertNull("nothing dragged", c.dragged)
        assertNull("no preview without a drag", c.preview)
        assertEquals("every piece in the tray, in tray order", first.pieces.toList(), c.trayPieces.toList())
        for (id in first.pieces) {
            assertEquals("$id at its S-9 resting turn", TrayRest.turn(id), c.trayTurn(id))
            assertFalse("$id not mirrored", c.trayMirrored(id))
        }
    }

    // seam S-2 `tapTray` — "REQ-016: Turn.next() of a tray piece; no TYPE-006 change; Turned"; `pressBadge` —
    // "PG in the tray → trayMirrored(PG) toggles, Turned, true". REQ-018 statement: the badge mirrors "the
    // parallelogram". (REQ-016's and REQ-018 A1's tokens are the device kit's.) Pin, no token.
    @Test
    fun aTrayTapTurnsThePieceOneStepClockwiseAndTheTrayBadgeTogglesTheMirror() {
        val p = kitPuzzleOf(PuzzleKind.WARMUP)
        val c = kitController()
        kitOpen(c, p.id)
        for (id in p.pieces) {
            val before = c.trayTurn(id)
            c.tapTray(id)
            assertEquals("$id: one tap is one step clockwise (Turn.next)", before.next(), c.trayTurn(id))
        }
        assertFalse(c.trayMirrored(PieceId.PG))
        assertTrue("pressBadge with PG in the tray", c.pressBadge())
        assertTrue("mirrored after one press", c.trayMirrored(PieceId.PG))
        assertTrue("pressBadge again", c.pressBadge())
        assertFalse("back after a second press", c.trayMirrored(PieceId.PG))
        assertTrue(c.pressBadge())
        for (id in p.pieces - PieceId.PG) assertFalse("$id is not mirrored by the badge", c.trayMirrored(id))
        assertEquals("tray actions start nothing (TYPE-006)", PuzzleState.NEW, c.state)
        assertEquals("still all in the tray", p.pieces.toList(), c.trayPieces.toList())
    }

    // ── REQ-002 — every puzzle can be completed ──────────────────────────────────────────────────

    // REQ-002.A1 — "Every puzzle can be completed with pieces of TYPE-001 by drag, turn and drop only",
    // tested as capture-outbox/chg-draft-req002-a1-mirror.md reads it ("by drag, turn, mirror (REQ-018) and
    // drop only"; decisions.md line 58): where a stored solution has the mirrored parallelogram, the solve
    // presses the tray badge. The line stays implemented (unverified) until the draft is decided.
    // Every puzzle the library lists (IPuzzleLibrary `puzzles`) is completed through S-2's player actions
    // alone — tapTray, pressBadge, startDrag, moveDrag, updatePreview, drop — and ends in state Solved
    // (TYPE-006: "In progress → Solved when the last piece locks"). The order of the drops is found
    // test-side with the engine (kitPlan; REQ-019's rationale: an anchorable order exists).
    @Test
    fun everyListedPuzzleIsCompletedByTurningMirroringDraggingAndDroppingAlone() { // REQ-002.A1
        val puzzles = kitLibrary.puzzles
        assertTrue("the library lists puzzles", puzzles.isNotEmpty())
        for (p in puzzles) {
            val plan = kitPlan(p)
            assertEquals("${p.id}: fixture: the plan drops each piece once", p.pieces, plan.map { it.piece }.toSet())
            val c = kitController()
            kitOpen(c, p.id)
            for ((k, step) in plan.withIndex()) {
                assertNotEquals("${p.id}: solved before lock ${k + 1} of ${plan.size}", PuzzleState.SOLVED, c.state)
                kitPlace(c, step)
            }
            assertEquals("${p.id}: completed", PuzzleState.SOLVED, c.state)
            assertEquals("${p.id}: every piece on the board", p.pieces, c.placed.map { it.piece }.toSet())
            assertTrue("${p.id}: nothing left in the tray", c.trayPieces.isEmpty())
            assertNull("${p.id}: nothing in the hand", c.dragged)
        }
    }

    // ── REQ-012 — every piece has exactly one place ──────────────────────────────────────────────

    // REQ-012.A2 — "Every piece is always either in the tray, on the board, or being dragged." After every
    // action of a script — tray drags that lock and that miss, a cancelled drag (goHome), a board drag
    // dropped on the tray, a twist, a badge press, a board drag that locks again, and opening another
    // puzzle in the middle of a drag and coming back — each of the open puzzle's pieces is in exactly one of
    // `trayPieces`, `placed`, `dragged`, and nothing else is there (S-1 reads).
    @Test
    fun everyPieceIsAlwaysInTheTrayOnTheBoardOrBeingDragged() { // REQ-012.A2
        val mini = kitPuzzleOf(PuzzleKind.MINI)
        val full = kitPuzzleOf(PuzzleKind.FULL)
        val miniPlan = kitPlan(mini)
        val fullPlan = kitPlan(full)
        assertTrue("fixture: the mini has three pieces or more", miniPlan.size >= 3)
        val c = kitController()
        var allThreeAtOnce = 0
        fun check(action: String) {
            kitWhereProblem(c)?.let { fail("after $action: $it") }
            if (c.trayPieces.isNotEmpty() && c.placed.isNotEmpty() && c.dragged != null) allThreeAtOnce++
        }
        check("construction")
        kitOpen(c, mini.id)
        check("open ${mini.id}")
        val a = miniPlan[0]
        val b = miniPlan[1]
        val d = miniPlan[2]

        // A tray drag that locks.
        kitTurnInTray(c, a.piece, a.target.turn)
        check("tapTray ${a.piece}")
        assertTrue(c.startDrag(a.piece, KIT_TRAY_POINT))
        check("startDrag ${a.piece} from the tray")
        c.moveDrag(a.dropCentre, true)
        check("moveDrag over the board")
        c.updatePreview()
        check("updatePreview")
        assertNotNull("fixture: the planned drop locks", c.drop())
        check("a drop that locks")

        // A tray drag released over the board where nothing locks, then a cancelled one.
        assertTrue(c.startDrag(b.piece, KIT_TRAY_POINT))
        check("startDrag ${b.piece} from the tray")
        c.moveDrag(KIT_FAR_POINT, true)
        check("moveDrag far off")
        c.drop()
        check("a drop that misses")
        assertTrue(c.startDrag(b.piece, KIT_TRAY_POINT))
        check("startDrag ${b.piece} again")
        c.goHome()
        check("goHome")

        // A board drag released over the tray.
        val onBoard = c.placed.single()
        assertTrue(c.startDrag(onBoard.piece, onBoard.centre))
        check("startDrag ${onBoard.piece} from the board")
        c.moveDrag(KIT_TRAY_POINT, false)
        check("moveDrag over the tray")
        c.drop()
        check("a board drag dropped on the tray")

        // Lock it again; then open the full puzzle in the middle of a drag (S-2 open: "A running drag ends
        // as goHome() first").
        kitPlace(c, a)
        check("the drop of ${a.piece} again")
        kitDragFromTray(c, b.piece, b.target.turn, b.target.mirrored)
        check("startDrag ${b.piece}")
        kitOpen(c, full.id)
        check("open ${full.id} during a drag")

        // The full puzzle: two locks, a twisted drag that misses, a badge press, a board drag that locks again.
        kitPlace(c, fullPlan[0])
        check("lock ${fullPlan[0].piece}")
        kitPlace(c, fullPlan[1])
        check("lock ${fullPlan[1].piece}")
        val e = fullPlan[2]
        assertTrue(c.startDrag(e.piece, KIT_TRAY_POINT))
        check("startDrag ${e.piece}")
        c.twistDrag(3)
        check("twistDrag(3)")
        c.moveDrag(KIT_FAR_POINT, true)
        c.updatePreview()
        check("updatePreview far off")
        c.drop()
        check("a twisted drop that misses")
        if (PieceId.PG in c.trayPieces) {
            c.pressBadge()
            check("pressBadge in the tray")
        }
        val top = c.placed.last()
        assertTrue(c.startDrag(top.piece, top.centre))
        check("startDrag ${top.piece} from the board")
        c.moveDrag(fullPlan[1].dropCentre, true)
        c.updatePreview()
        check("moveDrag back over its spot")
        c.drop()
        check("a board drag that locks again")

        // Back to the mini, and finish it.
        kitOpen(c, mini.id)
        check("open ${mini.id} again")
        kitPlace(c, b)
        check("lock ${b.piece}")
        kitPlace(c, d)
        check("the last lock of ${mini.id}")

        // Fixture: the script had a piece in each of the three places at once, more than once.
        assertTrue("fixture: tray, board and hand all in use at once ($allThreeAtOnce times)", allThreeAtOnce >= 3)
    }

    // ── REQ-018 — the mirror image of a fitting spot ─────────────────────────────────────────────

    // REQ-018.A3 — "A parallelogram dropped in its mirror image of a fitting spot does not lock there."
    // The fitting spot is the parallelogram's own hole: every other piece of the puzzle locked at its
    // solution pose, so the uncovered region is exactly the parallelogram's shape. Shown test-side first:
    // the engine locks the parallelogram there in the fitting image and finds nothing in reach for the
    // mirror image. Then, through the controller, the mirror image (mirrored in the tray with the badge,
    // the same turn) released on the spot does not lock there; and the fitting image released on the same
    // spot of the same board does (the discrimination). REQ-018 rule: "The lock search never tries the
    // other mirror image by itself". Every listed puzzle with the parallelogram.
    @Test
    fun aParallelogramDroppedInTheMirrorImageOfAFittingSpotDoesNotLockThere() { // REQ-018.A3
        val withPg = kitLibrary.puzzles.filter { PieceId.PG in it.pieces }
        assertTrue("fixture: the library lists puzzles with the parallelogram", withPg.isNotEmpty())
        for (p in withPg) {
            val plan = kitPlan(p, last = PieceId.PG)
            val spot = plan.last().target
            val tag = "${p.id}: PG at turn ${spot.turn.step}, fitting image mirrored=${spot.mirrored}"
            val c = kitController()
            kitOpen(c, p.id)
            for (step in plan.dropLast(1)) kitPlace(c, step)

            // Fixture, with the engine: the spot fits the one image and nothing in reach fits the other.
            val fitting = kitEngine.findLock(p, c.placed, PieceId.PG, spot.turn, spot.mirrored, spot.centre, KIT_DP_PER_UNIT)
            assertTrue("$tag: fixture: the fitting image locks on the spot", kitLocksAt(fitting, spot))
            val mirrorImage = kitEngine.findLock(p, c.placed, PieceId.PG, spot.turn, !spot.mirrored, spot.centre, KIT_DP_PER_UNIT)
            assertTrue("$tag: fixture: the mirror image locks nowhere in reach", mirrorImage is LockResult.None)

            // The mirror image, released on the spot.
            kitDragFromTray(c, PieceId.PG, spot.turn, !spot.mirrored)
            assertEquals("$tag: the drag carries the tray mirror (S-2 startDrag)", !spot.mirrored, c.dragged?.mirrored)
            c.moveDrag(spot.centre, true)
            c.updatePreview()
            val got = c.drop()
            assertFalse("$tag: the mirror image locked on the spot: $got", got != null && kitNear(got.centre, spot.centre, 1e-6))
            assertTrue(
                "$tag: a parallelogram lies on the spot: ${c.placed}",
                c.placed.none { it.piece == PieceId.PG && kitNear(it.centre, spot.centre, 1e-6) },
            )
            // With no lock in reach (the fixture above), not locking there means going home (REQ-020).
            assertNull("$tag: the drop of the mirror image", got)
            assertTrue("$tag: PG back in the tray", PieceId.PG in c.trayPieces)

            // Discrimination: the fitting image, released on the same spot of the same board, locks there.
            kitDragFromTray(c, PieceId.PG, spot.turn, spot.mirrored)
            c.moveDrag(spot.centre, true)
            c.updatePreview()
            kitAssertSamePlacement(spot, c.drop(), "$tag: the fitting image on the spot")
        }
    }

    // ── REQ-020 — after any drop: locked or home ─────────────────────────────────────────────────

    // REQ-020.A1 — "After any drop, the piece is either locked on the board or back in its tray cell."
    // A sweep of drops of every remaining piece, at its solution turn and one step off, released at centres
    // on a 0.5-unit grid over the silhouette's box and a unit around it, on an empty board and with up to
    // two pieces locked; every fourth release is over the tray (`overBoard` false; REQ-020 statement: "or is
    // dropped outside the board or on the tray"). Each lock is then picked up from the board and dropped
    // again — on the tray, or far off over the board — which are drops too. After each drop the piece is
    // either in `placed` as the lock the engine finds for that release ("locked": the Placement KDoc, "every
    // placement on the board is a lock found by ILockEngine"; S-2 `drop`) or in `trayPieces`, its cell
    // (S-6: one cell per piece), and nothing is dragged.
    @Test
    fun afterAnyDropThePieceIsLockedOnTheBoardOrBackInItsTrayCell() { // REQ-020.A1
        var locks = 0
        var homes = 0
        var trayReleasesThatWouldLock = 0
        for (p in listOf(kitPuzzleOf(PuzzleKind.MINI), kitPuzzleOf(PuzzleKind.WARMUP), kitPuzzleOf(PuzzleKind.FULL))) {
            val plan = kitPlan(p)
            val corners = kitEngine.outlineCorners(p)
            val xs = kitGrid(corners.minOf { it.x } - 1.0, corners.maxOf { it.x } + 1.0)
            val ys = kitGrid(corners.minOf { it.y } - 1.0, corners.maxOf { it.y } + 1.0)
            // Two pieces stay off the board, so no sweep lock solves the puzzle (S-2: no drag once Solved).
            for (prefix in listOf(0, minOf(2, plan.size - 2)).distinct()) {
                val c = kitController()
                kitOpen(c, p.id)
                plan.take(prefix).forEach { kitPlace(c, it) }
                var n = 0
                for (step in plan.drop(prefix)) {
                    for (turn in listOf(step.target.turn, step.target.turn.next())) {
                        for (x in xs) for (y in ys) {
                            n++
                            val overBoard = n % 4 != 0
                            kitDragFromTray(c, step.piece, turn, step.target.mirrored)
                            c.moveDrag(Point(x, y), overBoard)
                            val held = c.dragged ?: throw AssertionError("${p.id}: ${step.piece} is not dragged after startDrag")
                            val engineSays = kitEngine.findLock(p, c.placed, held.piece, held.turn, held.mirrored, held.centre, KIT_DP_PER_UNIT)
                            if (!overBoard && engineSays is LockResult.Locked) trayReleasesThatWouldLock++
                            val expected = if (overBoard && engineSays is LockResult.Locked) engineSays.placement else null
                            val where = "${p.id} (+$prefix locked) ${held.piece} turn ${held.turn.step} at ($x, $y), overBoard $overBoard"
                            val got = c.drop()
                            kitAssertLockedOrHome(c, held.piece, expected, got, where)
                            if (got == null) {
                                homes++
                                continue
                            }
                            locks++
                            // Pick the lock up from the board and drop it again: on the tray, or far off over the board.
                            assertTrue("$where: startDrag from the board", c.startDrag(got.piece, got.centre))
                            val farOff = locks % 2 == 0
                            c.moveDrag(if (farOff) KIT_FAR_POINT else KIT_TRAY_POINT, farOff)
                            val again = c.dragged ?: throw AssertionError("$where: not dragged after the board startDrag")
                            val engineAgain = kitEngine.findLock(p, c.placed, again.piece, again.turn, again.mirrored, again.centre, KIT_DP_PER_UNIT)
                            assertTrue("$where: fixture: nothing locks far off", !farOff || engineAgain is LockResult.None)
                            kitAssertLockedOrHome(c, again.piece, null, c.drop(), "$where, then the board drag dropped ${if (farOff) "far off" else "on the tray"}")
                            homes++
                        }
                    }
                }
            }
        }
        // Fixture: the sweep held both outcomes, and releases over the tray where the board would have locked.
        assertTrue("fixture: $locks locks, $homes homes", locks >= 50 && homes >= 50)
        assertTrue("fixture: $trayReleasesThatWouldLock tray releases at a spot that locks", trayReleasesThatWouldLock >= 5)
    }

    /** The points from [from] to [to] (inclusive) every 0.5 units. */
    private fun kitGrid(from: Double, to: Double): List<Double> =
        generateSequence(from) { it + 0.5 }.takeWhile { it <= to + 1e-9 }.toList()

    /** After a drop: nothing dragged, and [piece] either locked as [expected] (appended to `placed`) or back in the tray. */
    private fun kitAssertLockedOrHome(c: PlayController, piece: PieceId, expected: Placement?, got: Placement?, where: String) {
        assertNull("$where: nothing is dragged after the drop", c.dragged)
        if (expected != null) {
            kitAssertSamePlacement(expected, got, "$where: locked on the board as the engine's lock")
            kitAssertSamePlacement(expected, c.placed.lastOrNull(), "$where: the lock is the last of `placed`")
            assertFalse("$where: also in the tray", piece in c.trayPieces)
        } else {
            assertNull("$where: the drop locked although nothing locks there", got)
            assertTrue("$where: back in its tray cell", piece in c.trayPieces)
            assertTrue("$where: also on the board", c.placed.none { it.piece == piece })
        }
    }

    // ── TYPE-006 — the puzzle state ──────────────────────────────────────────────────────────────

    // TYPE-006 rule — "New → In progress when a piece first leaves the tray (a drag starts on a tray piece).
    // Turning a piece in the tray … does not start it." "In progress → Solved when the last piece locks
    // (REQ-022)." A missed first drop stays In progress: TYPE-006 as locked lists no way back to New
    // (capture-outbox/chg-draft-type006-missing-transitions.md, the reviewer's reading). Pin, no token.
    @Test
    fun theStateLeavesNewWhenATrayDragStartsAndBecomesSolvedAtTheLastLock() {
        val p = kitPuzzleOf(PuzzleKind.WARMUP)
        val plan = kitPlan(p)
        val c = kitController()
        kitOpen(c, p.id)
        assertEquals(PuzzleState.NEW, c.state)
        c.tapTray(PieceId.SQ)
        assertEquals("after a tray tap", PuzzleState.NEW, c.state)
        assertTrue(c.pressBadge())
        assertEquals("after a tray mirror", PuzzleState.NEW, c.state)

        assertTrue(c.startDrag(plan[0].piece, KIT_TRAY_POINT))
        assertEquals("when a drag starts on a tray piece, before any drop", PuzzleState.IN_PROGRESS, c.state)
        c.moveDrag(KIT_TRAY_POINT, false)
        assertNull(c.drop())
        assertEquals("after a missed first drop", PuzzleState.IN_PROGRESS, c.state)
        assertTrue(c.placed.isEmpty())

        assertTrue(c.startDrag(plan[0].piece, KIT_TRAY_POINT))
        c.moveDrag(KIT_FAR_POINT, true)
        val held = c.dragged ?: throw AssertionError("not dragged")
        assertTrue(
            "fixture: nothing locks far off",
            kitEngine.findLock(p, c.placed, held.piece, held.turn, held.mirrored, held.centre, KIT_DP_PER_UNIT) is LockResult.None,
        )
        assertNull(c.drop())
        assertEquals("after a miss over the board", PuzzleState.IN_PROGRESS, c.state)
        assertTrue(c.startDrag(plan[0].piece, KIT_TRAY_POINT))
        c.goHome()
        assertEquals("after a cancelled drag", PuzzleState.IN_PROGRESS, c.state)

        for ((k, step) in plan.withIndex()) {
            kitPlace(c, step)
            val expected = if (k == plan.lastIndex) PuzzleState.SOLVED else PuzzleState.IN_PROGRESS
            assertEquals("after lock ${k + 1} of ${plan.size}", expected, c.state)
        }
    }

    // ── goHome — a drag ended without a release ──────────────────────────────────────────────────

    // decision decisions.md line 59 — "a drag ended without a release (cancel, the activity stopping) is a
    // missed drop — the piece returns to its cell"; decisions.md line 63 — the corner pulse fires "never on
    // a cancelled drag". seam S-2 `goHome`: "as a missed drop but without pulse; a no-op without a drag";
    // a missed drop: "the piece goes back to its cell (REQ-020), WentHome". Pin, no token.
    @Test
    fun goHomeSendsTheDraggedPieceToItsCellWithoutAPulse() {
        val events = mutableListOf<PlayEvent>()
        val c = kitController(events)
        assertEquals("fixture: the first listed puzzle is a mini", PuzzleKind.MINI, c.puzzle.kind)
        val a = kitPlan(c.puzzle)[0]

        // Held where a release would lock — the preview shows the lock — then cancelled.
        val pulseBefore = c.pulse
        kitDragFromTray(c, a.piece, a.target.turn, a.target.mirrored)
        c.moveDrag(a.dropCentre, true)
        c.updatePreview()
        assertNotNull("fixture: a release here would lock", c.preview)
        events.clear()
        c.goHome()
        assertNull("nothing dragged", c.dragged)
        assertNull("no preview without a drag", c.preview)
        assertTrue("${a.piece} back in its tray cell", a.piece in c.trayPieces)
        assertTrue("nothing on the board", c.placed.isEmpty())
        assertTrue("no pulse: ${c.pulse}", kitNoNewPulse(pulseBefore, c.pulse))
        assertEquals(listOf("WentHome ${a.piece}"), events.map(::kitEventText))

        // A board drag that is cancelled goes to the tray, not back to its board spot.
        kitPlace(c, a)
        val pulseOnBoard = c.pulse
        assertTrue(c.startDrag(a.piece, a.target.centre))
        c.goHome()
        assertTrue("${a.piece} in its tray cell after a cancelled board drag", a.piece in c.trayPieces)
        assertTrue("not back on the board", c.placed.isEmpty())
        assertTrue("no pulse: ${c.pulse}", kitNoNewPulse(pulseOnBoard, c.pulse))

        // Without a drag, goHome changes nothing and emits nothing.
        val before = kitSnapshot(c)
        events.clear()
        c.goHome()
        assertEquals(before, kitSnapshot(c))
        assertEquals(emptyList<String>(), events.map(::kitEventText))
    }

    /** True when [after] is no new pulse: none, or the one that was there ([before], the same serial; S-3 "a new serial per pulse"). */
    private fun kitNoNewPulse(before: Pulse?, after: Pulse?): Boolean =
        after == null || (before != null && after.serial == before.serial)

    // ── The corner pulse, in a mini puzzle only ──────────────────────────────────────────────────

    // REQ-051 rule — "It happens only in mini puzzles; full-set and warm-up puzzles never show anchors";
    // statement: "WHEN a dropped piece goes home (REQ-020)"; decisions.md line 63 — on every release in a
    // mini that goes home (no valid position, outside the board, on the tray). seam S-2 `drop`: "pulse set
    // in a mini puzzle only"; S-3 `Pulse`: "corners = engine.outlineCorners(puzzle), a new serial per
    // pulse". REQ-051's tokens are the device kit's (TASK-011). Pin, no token.
    @Test
    fun aMissedReleaseSetsAPulseOfTheOutlineCornersInAMiniPuzzleOnly() {
        val c = kitController()
        val mini = c.puzzle
        assertEquals("fixture: the first listed puzzle is a mini", PuzzleKind.MINI, mini.kind)
        val outline = kitEngine.outlineCorners(mini)
        val middle = kitAverage(outline)
        val (piece, turn) = kitMissAt(c, middle)

        // A release over the middle of the silhouette where nothing locks.
        kitDragFromTray(c, piece, turn, false)
        c.moveDrag(middle, true)
        assertNull("fixture: nothing locks there", c.drop())
        val first = c.pulse ?: throw AssertionError("a missed release in a mini sets a pulse")
        kitAssertSamePointSet(outline, first.corners, 1e-9, "the pulse shows the outline corners")

        // A release on the tray goes home too: a new pulse.
        assertTrue(c.startDrag(piece, KIT_TRAY_POINT))
        c.moveDrag(KIT_TRAY_POINT, false)
        assertNull(c.drop())
        val second = c.pulse ?: throw AssertionError("a release on the tray in a mini sets a pulse")
        assertNotEquals("a new serial per pulse", first.serial, second.serial)
        kitAssertSamePointSet(outline, second.corners, 1e-9, "the second pulse shows the outline corners")

        // A lock sets none.
        kitPlace(c, kitPlan(mini)[0])
        assertTrue("a lock sets no new pulse: ${c.pulse}", kitNoNewPulse(second, c.pulse))

        // A warm-up and a full puzzle: a missed release never sets one.
        for (p in listOf(kitPuzzleOf(PuzzleKind.WARMUP), kitPuzzleOf(PuzzleKind.FULL))) {
            val d = kitController()
            kitOpen(d, p.id)
            val before = d.pulse
            val mid = kitAverage(kitEngine.outlineCorners(p))
            val (pc, t) = kitMissAt(d, mid)
            kitDragFromTray(d, pc, t, false)
            d.moveDrag(mid, true)
            assertNull("${p.id}: fixture: nothing locks there", d.drop())
            assertTrue("${p.id} (${p.kind}): no pulse after a miss over the board: ${d.pulse}", kitNoNewPulse(before, d.pulse))
            assertTrue(d.startDrag(pc, KIT_TRAY_POINT))
            d.moveDrag(KIT_TRAY_POINT, false)
            assertNull(d.drop())
            assertTrue("${p.id} (${p.kind}): no pulse after a release on the tray: ${d.pulse}", kitNoNewPulse(before, d.pulse))
        }
    }

    // ── Opening puzzles: memory per puzzle ───────────────────────────────────────────────────────

    // decision decisions.md line 29 — "A tray piece's tap-turn or flip is not persisted; only board
    // placements are saved". seam S-2 `open` — "State and placements are kept in memory per puzzle id
    // across opens … Tray turns and mirror reset to S-9 on every open"; "false and no change for an unknown
    // id, G-15". Amended at WO-004 (TASK-016; tasks.md § WO-004 intro, "In-tree tests this WO changes"):
    // S-2's "a Solved puzzle keeps its final placements" is superseded by designs/WO-004-design.md §2 S-21
    // ("NEW or SOLVED: an empty board", on every open) and §3 ("A Solved puzzle opens with an empty board").
    // This controller is the memory-only one (S-1Δ: the secondary constructor passes null). Pin, no token.
    @Test
    fun eachPuzzleKeepsItsStateAndBoardAcrossOpensWhileTrayTurnsAndTheMirrorReset() {
        val a = kitPuzzleOf(PuzzleKind.WARMUP)
        val b = kitPuzzleOf(PuzzleKind.FULL)
        val mini = kitPuzzleOf(PuzzleKind.MINI)
        val planA = kitPlan(a)
        val planB = kitPlan(b)
        val c = kitController()
        kitOpen(c, a.id)
        kitPlace(c, planA[0])
        kitPlace(c, planA[1])
        val keptA = c.placed.toList()
        val turned = c.trayPieces.first { it != PieceId.PG }
        repeat(3) { c.tapTray(turned) }
        assertNotEquals("fixture: three taps moved $turned off its resting turn", TrayRest.turn(turned), c.trayTurn(turned))
        assertTrue("fixture: PG is in the tray", PieceId.PG in c.trayPieces)
        kitMirrorInTray(c, true)

        // Another puzzle opens fresh.
        assertTrue(c.open(b.id))
        c.boardDpPerUnit = KIT_DP_PER_UNIT
        kitAssertFreshTray(c, b, "${b.id} on its first open")
        assertEquals(PuzzleState.NEW, c.state)
        kitPlace(c, planB[0])
        val keptB = c.placed.toList()

        // Back to the first: its state and board as left, its tray turns and mirror at rest.
        assertTrue(c.open(a.id))
        c.boardDpPerUnit = KIT_DP_PER_UNIT
        assertEquals(a.id, c.puzzle.id)
        assertEquals("${a.id}: state kept", PuzzleState.IN_PROGRESS, c.state)
        assertEquals("${a.id}: board kept", keptA, c.placed)
        val keptPieces = keptA.map { it.piece }.toSet()
        assertEquals("${a.id}: the rest in the tray", a.pieces.filter { it !in keptPieces }, c.trayPieces.toList())
        for (id in c.trayPieces) {
            assertEquals("${a.id}: $id back at its resting turn", TrayRest.turn(id), c.trayTurn(id))
            assertFalse("${a.id}: $id not mirrored", c.trayMirrored(id))
        }
        assertTrue(c.open(b.id))
        c.boardDpPerUnit = KIT_DP_PER_UNIT
        assertEquals("${b.id}: state kept", PuzzleState.IN_PROGRESS, c.state)
        assertEquals("${b.id}: board kept", keptB, c.placed)

        // A Solved puzzle stays Solved and opens with an empty board (S-21).
        kitOpen(c, mini.id)
        kitPlan(mini).forEach { kitPlace(c, it) }
        assertEquals(PuzzleState.SOLVED, c.state)
        assertEquals("fixture: the final placements are on the board at the solve", mini.pieces, c.placed.map { it.piece }.toSet())
        assertTrue(c.open(a.id))
        assertTrue(c.open(mini.id))
        c.boardDpPerUnit = KIT_DP_PER_UNIT
        assertEquals("${mini.id}: still solved", PuzzleState.SOLVED, c.state)
        assertEquals("${mini.id}: an empty board (S-21)", emptyList<Placement>(), c.placed)

        // An unknown id: false, and nothing changes.
        val before = kitSnapshot(c)
        assertFalse(c.open("no-such-puzzle"))
        assertEquals(before, kitSnapshot(c))
    }

    /** Asserts [c] shows [p] as never touched: nothing placed or dragged, every piece in the tray at rest, unmirrored (S-1, S-9). */
    private fun kitAssertFreshTray(c: PlayController, p: PuzzleDefinition, what: String) {
        assertEquals(what, p.id, c.puzzle.id)
        assertTrue("$what: nothing placed", c.placed.isEmpty())
        assertNull("$what: nothing dragged", c.dragged)
        assertEquals("$what: every piece in the tray", p.pieces.toList(), c.trayPieces.toList())
        for (id in p.pieces) {
            assertEquals("$what: $id at rest", TrayRest.turn(id), c.trayTurn(id))
            assertFalse("$what: $id not mirrored", c.trayMirrored(id))
        }
    }

    // seam S-2 `open` — "A running drag ends as goHome() first"; `goHome` emits WentHome (a missed drop).
    // TYPE-006: the drag started on a tray piece, so the puzzle is In progress. Pin, no token.
    @Test
    fun openingAnotherPuzzleDuringADragSendsThePieceHomeFirst() {
        val events = mutableListOf<PlayEvent>()
        val c = kitController(events)
        val first = c.puzzle
        val other = kitPuzzleOf(PuzzleKind.WARMUP)
        val piece = first.pieces.first()
        assertTrue(c.startDrag(piece, KIT_TRAY_POINT))
        events.clear()
        assertTrue(c.open(other.id))
        assertEquals(listOf("WentHome $piece"), events.map(::kitEventText))
        assertNull("nothing dragged after the open", c.dragged)
        assertEquals(other.id, c.puzzle.id)
        assertTrue(c.open(first.id))
        c.boardDpPerUnit = KIT_DP_PER_UNIT
        assertTrue("$piece back in its tray cell", piece in c.trayPieces)
        assertTrue("nothing on the board", c.placed.isEmpty())
        assertEquals(PuzzleState.IN_PROGRESS, c.state)
    }

    // ── S-4 — the events and their order ─────────────────────────────────────────────────────────

    // seam S-4 — `PickedUp`, `Turned`, `Locked`, `WentHome`, `Solved(puzzleId)` "emitted synchronously through
    // onEvent by the S-2 actions as listed there" (O-07: `:play` emits, `:app` plays). S-2: tapTray →
    // Turned; pressBadge in the tray → Turned; startDrag → PickedUp; twistDrag → "one Turned per step";
    // a lock → Locked, "then isSolved → state Solved and Solved"; a miss → WentHome; goHome → as a missed
    // drop, "a no-op without a drag". Actions S-2 lists no event for emit none. Pin, no token.
    @Test
    fun eachActionEmitsItsEventsSynchronouslyAndInOrder() {
        val events = mutableListOf<PlayEvent>()
        val c = kitController(events)
        fun taken(): List<String> = events.map(::kitEventText).also { events.clear() }
        val none = emptyList<String>()
        assertEquals("construction", none, taken())
        val p = kitPuzzleOf(PuzzleKind.WARMUP)
        val plan = kitPlan(p)
        kitOpen(c, p.id)
        assertEquals("open with no drag running", none, taken())

        c.tapTray(PieceId.SQ)
        assertEquals("tapTray", listOf("Turned SQ"), taken())
        assertTrue(c.pressBadge())
        assertEquals("pressBadge in the tray", listOf("Turned PG"), taken())

        val s0 = plan[0]
        kitTurnInTray(c, s0.piece, s0.target.turn)
        taken()
        assertTrue(c.startDrag(s0.piece, KIT_TRAY_POINT))
        assertEquals("startDrag", listOf("PickedUp ${s0.piece}"), taken())
        c.twistDrag(2)
        assertEquals("twistDrag(2)", listOf("Turned ${s0.piece}", "Turned ${s0.piece}"), taken())
        c.twistDrag(-2)
        assertEquals("twistDrag(-2)", listOf("Turned ${s0.piece}", "Turned ${s0.piece}"), taken())
        c.moveDrag(s0.dropCentre, true)
        assertEquals("moveDrag", none, taken())
        c.updatePreview()
        assertEquals("updatePreview", none, taken())
        kitAssertSamePlacement(s0.target, c.drop(), "the first lock")
        assertEquals("a drop that locks", listOf("Locked ${s0.piece}"), taken())

        val s1 = plan[1]
        assertTrue(c.startDrag(s1.piece, KIT_TRAY_POINT))
        assertEquals(listOf("PickedUp ${s1.piece}"), taken())
        assertFalse("startDrag while a drag runs", c.startDrag(plan[2].piece, KIT_TRAY_POINT))
        assertEquals("a refused startDrag", none, taken())
        c.moveDrag(KIT_FAR_POINT, true)
        assertNull(c.drop())
        assertEquals("a drop that misses", listOf("WentHome ${s1.piece}"), taken())
        assertTrue(c.startDrag(s1.piece, KIT_TRAY_POINT))
        taken()
        c.goHome()
        assertEquals("goHome", listOf("WentHome ${s1.piece}"), taken())
        c.goHome()
        assertEquals("goHome without a drag", none, taken())

        for (step in plan.subList(1, plan.lastIndex)) kitPlace(c, step)
        taken()
        val last = plan.last()
        kitTurnInTray(c, last.piece, last.target.turn)
        if (last.piece == PieceId.PG) kitMirrorInTray(c, last.target.mirrored)
        taken()
        assertTrue(c.startDrag(last.piece, KIT_TRAY_POINT))
        assertEquals(listOf("PickedUp ${last.piece}"), taken())
        c.moveDrag(last.dropCentre, true)
        c.updatePreview()
        kitAssertSamePlacement(last.target, c.drop(), "the last lock")
        assertEquals("the last lock: Locked, then Solved", listOf("Locked ${last.piece}", "Solved ${p.id}"), taken())

        assertFalse("startDrag once solved", c.startDrag(last.piece, last.target.centre))
        assertFalse("pressBadge once solved", c.pressBadge())
        assertEquals("refused actions after the solve", none, taken())
    }

    // ── S-2 — what a drag carries; the preview; the drop ─────────────────────────────────────────

    // seam S-2 `startDrag` — "the piece leaves the tray (with its tray turn and mirror) or the board (with
    // its placement's turn and mirror; it leaves placed) and becomes dragged at centre"; S-3 `Dragged`;
    // `moveDrag` — "stores the dragged centre and whether a release now would be over the board";
    // `twistDrag` — "positive = clockwise (next / previous)"; `updatePreview` — "preview = the findLock
    // placement for the dragged piece at its centre, turn and mirror, or null (None, overBoard false, no
    // drag)" (REQ-021 rule: "the same search as the drop"); `drop` — "The lock is the updatePreview() result
    // for the current centre (computed now if not yet)"; "Locked → appended". Tag table: `landing-preview`
    // is present while `preview` is not null, and REQ-021 draws it only "WHILE the player drags". Pin.
    @Test
    fun aDragCarriesItsPoseFollowsMoveAndTwistAndDropsWhereTheCurrentPreviewIs() {
        val p = kitPuzzleOf(PuzzleKind.WARMUP)
        val plan = kitPlan(p)
        val c = kitController()
        kitOpen(c, p.id)

        // From the tray: the tray turn and mirror.
        repeat(3) { c.tapTray(PieceId.PG) }
        kitMirrorInTray(c, true)
        val trayTurn = c.trayTurn(PieceId.PG)
        assertTrue(c.startDrag(PieceId.PG, KIT_TRAY_POINT))
        var d = c.dragged ?: throw AssertionError("PG is not dragged")
        assertEquals(PieceId.PG, d.piece)
        assertEquals("the tray turn", trayTurn, d.turn)
        assertTrue("the tray mirror", d.mirrored)
        assertEquals("the centre given", KIT_TRAY_POINT, d.centre)
        assertTrue("from the tray", d.fromTray)
        assertFalse("left the tray", PieceId.PG in c.trayPieces)

        // moveDrag stores the centre and the over-board flag.
        c.moveDrag(Point(1.25, 2.5), true)
        d = c.dragged!!
        assertEquals(Point(1.25, 2.5), d.centre)
        assertTrue(d.overBoard)
        c.moveDrag(Point(1.75, 9.5), false)
        d = c.dragged!!
        assertEquals(Point(1.75, 9.5), d.centre)
        assertFalse(d.overBoard)

        // twistDrag: positive is clockwise.
        c.twistDrag(3)
        assertEquals("three steps clockwise", trayTurn.next().next().next(), c.dragged!!.turn)
        c.twistDrag(-1)
        assertEquals("one step back", trayTurn.next().next(), c.dragged!!.turn)
        c.twistDrag(-2)
        assertEquals("back to the start", trayTurn, c.dragged!!.turn)
        assertTrue("the mirror is untouched by a twist", c.dragged!!.mirrored)
        c.goHome()
        assertNull("no preview without a drag", c.preview)

        // From the board: the placement's turn and mirror; it leaves `placed`.
        val q = kitPlace(c, plan[0])
        assertTrue(c.startDrag(q.piece, q.centre))
        d = c.dragged ?: throw AssertionError("${q.piece} is not dragged")
        assertEquals(q.piece, d.piece)
        assertEquals("the placement's turn", q.turn, d.turn)
        assertEquals("the placement's mirror", q.mirrored, d.mirrored)
        assertEquals(q.centre, d.centre)
        assertFalse("from the board", d.fromTray)
        assertTrue("left the board", c.placed.none { it.piece == q.piece })
        assertFalse("not in the tray", q.piece in c.trayPieces)

        // The preview is findLock's answer for the dragged piece at its centre, turn and mirror.
        c.moveDrag(plan[0].dropCentre, true)
        c.updatePreview()
        val engineSays = kitEngine.findLock(p, c.placed, d.piece, d.turn, d.mirrored, plan[0].dropCentre, KIT_DP_PER_UNIT)
        assertTrue("fixture: it locks there", engineSays is LockResult.Locked)
        kitAssertSamePlacement((engineSays as LockResult.Locked).placement, c.preview, "the preview")
        c.moveDrag(plan[0].dropCentre, false)
        c.updatePreview()
        assertNull("no preview while not over the board", c.preview)
        c.moveDrag(KIT_FAR_POINT, true)
        c.updatePreview()
        assertTrue(
            "fixture: nothing locks far off",
            kitEngine.findLock(p, c.placed, d.piece, d.turn, d.mirrored, KIT_FAR_POINT, KIT_DP_PER_UNIT) is LockResult.None,
        )
        assertNull("no preview where nothing locks", c.preview)

        // The drop uses the current centre, not a preview of an earlier one.
        c.moveDrag(plan[0].dropCentre, true)
        c.updatePreview()
        assertNotNull("fixture: a preview at the spot", c.preview)
        c.moveDrag(KIT_FAR_POINT, true)
        assertNull("released far off after a preview at the spot", c.drop())
        assertTrue(q.piece in c.trayPieces)
        assertNull("no preview after the drop", c.preview)
        kitDragFromTray(c, q.piece, q.turn, q.mirrored)
        c.moveDrag(KIT_FAR_POINT, true)
        c.updatePreview()
        assertNull(c.preview)
        c.moveDrag(plan[0].dropCentre, true)
        kitAssertSamePlacement(plan[0].target, c.drop(), "released on the spot after a preview far off")
        assertNull("no preview after the drop", c.preview)

        // A lock is appended: `placed` is in lock order.
        kitPlace(c, plan[1])
        assertTrue(c.startDrag(plan[0].piece, plan[0].target.centre))
        c.moveDrag(plan[0].dropCentre, true)
        assertNotNull("fixture: it locks again", c.drop())
        assertEquals("lock order", listOf(plan[1].piece, plan[0].piece), c.placed.map { it.piece })
    }

    // ── S-2 — refusals ───────────────────────────────────────────────────────────────────────────

    // seam S-2 — `open`: "false and no change for an unknown id, G-15"; `startDrag`: "False and no change
    // when it is not the puzzle's piece, a drag is running, or the state is Solved"; `pressBadge`: "No PG in
    // the puzzle, PG dragged, or state Solved → false, no change". TYPE-006: a refused start leaves a New
    // puzzle New. Pin, no token.
    @Test
    fun refusedActionsReturnFalseAndChangeNothing() {
        val c = kitController()
        var before = kitSnapshot(c)
        assertFalse("open of an unknown id", c.open("no-such-puzzle"))
        assertEquals(before, kitSnapshot(c))

        val foreign = PieceId.entries.firstOrNull { it !in c.puzzle.pieces }
            ?: throw AssertionError("fixture defect: the first listed puzzle uses every piece")
        before = kitSnapshot(c)
        assertFalse("startDrag of a piece the puzzle does not have", c.startDrag(foreign, KIT_TRAY_POINT))
        assertEquals(before, kitSnapshot(c))
        assertEquals(PuzzleState.NEW, c.state)

        assertFalse("fixture: the first listed puzzle has no parallelogram", PieceId.PG in c.puzzle.pieces)
        assertFalse("pressBadge with no PG in the puzzle", c.pressBadge())
        assertEquals(before, kitSnapshot(c))

        val (x, y) = c.puzzle.pieces.toList()
        assertTrue(c.startDrag(x, KIT_TRAY_POINT))
        before = kitSnapshot(c)
        assertFalse("startDrag of another piece while a drag runs", c.startDrag(y, KIT_TRAY_POINT))
        assertFalse("startDrag of the dragged piece while its drag runs", c.startDrag(x, KIT_TRAY_POINT))
        assertEquals(before, kitSnapshot(c))
        c.goHome()

        val w = kitPuzzleOf(PuzzleKind.WARMUP)
        kitOpen(c, w.id)
        assertTrue(c.startDrag(PieceId.PG, KIT_TRAY_POINT))
        before = kitSnapshot(c)
        assertFalse("pressBadge while PG is dragged", c.pressBadge())
        assertEquals(before, kitSnapshot(c))
        c.goHome()

        kitPlan(w).forEach { kitPlace(c, it) }
        assertEquals(PuzzleState.SOLVED, c.state)
        before = kitSnapshot(c)
        for (placement in c.placed.toList()) {
            assertFalse("startDrag of ${placement.piece} once solved", c.startDrag(placement.piece, placement.centre))
        }
        assertFalse("pressBadge once solved, PG on the board", c.pressBadge())
        assertEquals(before, kitSnapshot(c))
    }

    // ── S-2 — turning and mirroring a board piece ────────────────────────────────────────────────

    // seam S-2 `tapBoard` — "tryTurnInPlace(…, current.turn.next(), …). Locked → the placement is replaced,
    // Turned, true. None → unchanged, shake for it, false"; `pressBadge` — "PG placed → tryMirrorInPlace:
    // Locked → replaced, Turned, true; None → unchanged, shake, false"; S-3 `Shake` — "a new serial per
    // refused turn or mirror". The four fixtures (a kept and a refused turn, a kept and a refused mirror)
    // are found with the engine over the shipped puzzles. REQ-016's and REQ-018 A1's tokens are the device
    // kit's. Pin, no token.
    @Test
    fun aBoardTurnOrMirrorKeepsTheEnginesLockAndARefusalShakesTheUnchangedPiece() {
        for (mirror in listOf(false, true)) {
            for (wantKept in listOf(true, false)) {
                val f = kitFindBoardFixture(mirror, wantKept)
                val what = if (mirror) "mirror" else "turn"
                val tag = "${f.puzzle.id}, ${f.k} locked, ${f.piece}, dpPerUnit ${f.dpPerUnit}: board $what"
                val events = mutableListOf<PlayEvent>()
                val c = kitController(events)
                kitOpen(c, f.puzzle.id)
                f.plan.take(f.k).forEach { kitPlace(c, it) }
                c.boardDpPerUnit = f.dpPerUnit
                val q = c.placed.single { it.piece == f.piece }
                val others = c.placed - q
                val verdict = if (mirror) {
                    kitEngine.tryMirrorInPlace(f.puzzle, others, q, f.dpPerUnit)
                } else {
                    kitEngine.tryTurnInPlace(f.puzzle, others, q, q.turn.next(), f.dpPerUnit)
                }
                assertEquals("$tag: fixture: the engine's verdict", wantKept, verdict is LockResult.Locked)
                val placedBefore = c.placed.toList()
                val shakeBefore = c.shake
                events.clear()
                val kept = if (mirror) c.pressBadge() else c.tapBoard(q.piece)
                if (verdict is LockResult.Locked) {
                    assertTrue("$tag: returns true", kept)
                    assertEquals("$tag: the same pieces on the board", placedBefore.size, c.placed.size)
                    kitAssertSamePlacement(verdict.placement, c.placed.single { it.piece == q.piece }, "$tag: replaced by the engine's lock")
                    for (o in others) assertTrue("$tag: $o stays", o in c.placed)
                    assertEquals("$tag: event", listOf("Turned ${q.piece}"), events.map(::kitEventText))
                    assertEquals("$tag: no shake", shakeBefore, c.shake)
                } else {
                    assertFalse("$tag: returns false", kept)
                    assertEquals("$tag: unchanged", placedBefore, c.placed)
                    assertEquals("$tag: no event", emptyList<String>(), events.map(::kitEventText))
                    val s1 = c.shake ?: throw AssertionError("$tag: no shake")
                    assertEquals("$tag: the shake is for the piece", q.piece, s1.piece)
                    assertNotEquals("$tag: a new shake serial", shakeBefore?.serial, s1.serial)
                    if (mirror) c.pressBadge() else c.tapBoard(q.piece)
                    val s2 = c.shake ?: throw AssertionError("$tag: no shake after the second refusal")
                    assertNotEquals("$tag: a new serial for the second refusal", s1.serial, s2.serial)
                    assertEquals("$tag: still unchanged", placedBefore, c.placed)
                }
            }
        }
    }

    /** A board state for a board turn or mirror: [puzzle] with the first [k] steps of [plan] locked, searched at [dpPerUnit]. */
    private class KitBoardFixture(
        val puzzle: PuzzleDefinition,
        val plan: List<KitStep>,
        val k: Int,
        val dpPerUnit: Double,
        val piece: PieceId,
    )

    /**
     * The first board state, over the shipped puzzles and their drop orders (the parallelogram first, then
     * the tray order), where the engine keeps ([wantKept]) or refuses a board turn of a placed piece (or,
     * with [mirror], a mirror of the placed parallelogram) — at [KIT_DP_PER_UNIT] or [KIT_WIDE_DP_PER_UNIT].
     * None is a fixture defect.
     */
    private fun kitFindBoardFixture(mirror: Boolean, wantKept: Boolean): KitBoardFixture {
        for (p in kitLibrary.puzzles) {
            if (mirror && PieceId.PG !in p.pieces) continue
            val plans = listOfNotNull(kitPlanOrNull(p, first = PieceId.PG.takeIf { it in p.pieces }), kitPlanOrNull(p))
            for (plan in plans) {
                for (k in 1 until plan.size) {
                    val placed = plan.take(k).map { it.target }
                    for (dp in listOf(KIT_DP_PER_UNIT, KIT_WIDE_DP_PER_UNIT)) {
                        for (q in placed) {
                            if (mirror && q.piece != PieceId.PG) continue
                            val others = placed - q
                            val verdict = if (mirror) {
                                kitEngine.tryMirrorInPlace(p, others, q, dp)
                            } else {
                                kitEngine.tryTurnInPlace(p, others, q, q.turn.next(), dp)
                            }
                            if ((verdict is LockResult.Locked) == wantKept) return KitBoardFixture(p, plan, k, dp, q.piece)
                        }
                    }
                }
            }
        }
        throw AssertionError("fixture defect: no shipped board where the engine ${if (wantKept) "keeps" else "refuses"} a board ${if (mirror) "mirror" else "turn"}")
    }

    // ── S-2 — the board scale ────────────────────────────────────────────────────────────────────

    // seam S-2 `boardDpPerUnit` — "passed to every engine search …; A search while it is not > 0 throws
    // IllegalStateException (a programming error, G-15)". The searches: updatePreview, drop over the board,
    // tapBoard. Pin, no token.
    @Test
    fun aSearchWithoutAPositiveBoardScaleIsAProgrammingError() {
        for (bad in listOf(0.0, -1.0, Double.NaN)) {
            for (action in listOf("updatePreview", "drop", "tapBoard")) {
                val c = kitController()
                val plan = kitPlan(c.puzzle)
                if (action == "tapBoard") {
                    val q = kitPlace(c, plan[0])
                    c.boardDpPerUnit = bad
                    assertThrows("tapBoard with boardDpPerUnit $bad", IllegalStateException::class.java) { c.tapBoard(q.piece) }
                    continue
                }
                kitDragFromTray(c, plan[0].piece, plan[0].target.turn, plan[0].target.mirrored)
                c.moveDrag(plan[0].dropCentre, true)
                c.boardDpPerUnit = bad
                assertThrows("$action with boardDpPerUnit $bad", IllegalStateException::class.java) {
                    if (action == "updatePreview") c.updatePreview() else c.drop()
                }
            }
        }
    }
}
