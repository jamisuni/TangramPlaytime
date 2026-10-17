// Acceptance kit (WO-004, TASK-019): coming back to a puzzle, Restart and the load check on the screen
// (designs/WO-004-design.md §2 S-19, S-1Δ, S-2Δ, S-20, S-21, S-22 "Restart", "What survives"; §3 "Never lose another
// puzzle's progress"). Every controller here saves through `Persistence` over the kit's store double (KitScreen:
// `kitControllerOver`); leaving and coming back is by touch on ‹ and ›; a board is set up through S-2 or seeded in the
// store where the line is not about the gesture. The quantifier lines take their adversarial paths in the same test: an
// activity recreation over the same controller, and a new controller over a new `Persistence` over the same store; the
// process-death leg of REQ-003 A2 and REQ-025 A1 is `app.tangramplaytime.app.ProcessDeathKitTest.phase2`.
package app.tangramplaytime.play

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.hypot

@RunWith(AndroidJUnit4::class)
class ResumeScreenKitTest {

    // The ComponentActivity host, declared so the recreation legs can recreate it (KitScreen.recreate).
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // ── REQ-025 A1: the same three pieces in the same places ───────────────────────────────────────────

    @Test // REQ-025.A1
    fun aPuzzleLeftWithThreePiecesPlacedShowsTheSameThreeInTheSamePlacesWhenThePlayerComesBack() {
        // REQ-025 A1: "Leave a puzzle with three pieces placed, come back: the same three pieces are in the same places."
        // Statement: "WHEN the player returns to a puzzle in progress, the game SHALL show it exactly as it was left." Oracle:
        // a puzzle of seven pieces (three placed do not solve it) with three pieces placed through S-2 — a mirrored
        // parallelogram among them where the library has one (`kitThreeToLeave`), so a lost mirror shows; left by a tap on
        // › and on ‹ and come back by the other arrow; then come back after an activity recreation over the same
        // controller, and after a new controller over a new `Persistence` over the same store (what the next launch builds,
        // S-15Δ). Each time: the same three `board-piece` nodes, each with the same piece, turn, mirror and centre (S-14
        // keys, read against each node's box through the engine's corners and against the board's pixels), the other four
        // in the tray, the puzzle In progress. The process-death leg is ProcessDeathKitTest.phase2 (same token).
        val ps = kitControllerOver(rule, KitDeviceStore())
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        val (p, three) = kitThreeToLeave()
        kit.open(p.id)
        for (pl in three) assertTrue("set-up: ${pl.piece} of ${p.id} placed through S-2", kit.placeByApi(pl.piece))
        kit.frames()
        kit.advance(250)
        val placed = kit.placed()
        assertSameBoard("set-up: the three planned locks of ${p.id}", three, placed)
        val left = kit.boardFromNodes()
        assertSameBoard("set-up: the board as left", placed, left)
        assertCameBack(kit, p, left, "set-up, before leaving")

        kit.press(KitTag.NAV_NEXT)
        assertNotEquals("› left ${p.id}", p.id, kit.shownId())
        kit.press(KitTag.NAV_PREV)
        assertCameBack(kit, p, left, "left by ›, back by ‹")

        kit.press(KitTag.NAV_PREV)
        assertNotEquals("‹ left ${p.id}", p.id, kit.shownId())
        kit.press(KitTag.NAV_NEXT)
        assertCameBack(kit, p, left, "left by ‹, back by ›")

        kit.recreate(rule.activityRule.scenario)
        assertCameBack(kit, p, left, "the first frame of a recreated activity")
        kit.advance(500)
        assertCameBack(kit, p, left, "500 ms into the recreated activity")

        ps.flush()
        val saved = ps.store.progressNow(p.id)
        assertEquals("S-20: the store holds ${p.id} In progress", PuzzleState.IN_PROGRESS, saved.state)
        assertSameBoard("S-20: the store holds the three placements", left, saved.placed)
        val next = kitControllerOver(rule, ps.store)
        val kit2 = KitScreen(rule, next.controller)
        kit2.recreate(rule.activityRule.scenario)
        assertCameBack(kit2, p, left, "a new controller over a new Persistence over the same store, its first frame")
        kit2.advance(500)
        assertCameBack(kit2, p, left, "a new controller over the same store, 500 ms on")
    }

    // ── REQ-003 A2: no state is ever lost ──────────────────────────────────────────────────────────────

    @Test // REQ-003.A2
    fun leavingAndReturningToAPuzzleNeverLosesItsState() {
        // REQ-003 A2: "Leaving and returning to a puzzle never loses its state." Rule: "Each puzzle keeps its own state
        // (TYPE-006)." Oracle: four puzzles in each state — S Solved (solved through S-2), A In progress with two pieces, B
        // In progress with one piece (placed after A was left: saving B must not cost A anything, design §3 "Never lose
        // another puzzle's progress"), N New — and every puzzle between them untouched. Puzzles are left and returned to by
        // taps on ‹ and ›, walking through every puzzle between S and N; each one shown is checked against what it was left
        // with: its TYPE-006 state (PuzzleStateKey, read against the `restart` pill, shown only In progress — REQ-025 rule —
        // and the `solved-picture`, tag table) and its board (the `board-piece` keys, read against the drawing). "Never":
        // again after an activity recreation over the same controller, and again — walking the whole range — over a new
        // controller over a new `Persistence` over the same store. The process-death leg is ProcessDeathKitTest.phase2.
        val ps = kitControllerOver(rule, KitDeviceStore())
        val kit = KitScreen(rule, ps.controller)
        kit.show(reducedMotion = true)
        val list = kitLibrary.puzzles
        val s = list.first()
        val a = kitFirst(PuzzleKind.WARMUP)
        val b = kitAfter(a.id)
        val n = kitAfter(b.id)
        check(kitNumberOf(s.id) < kitNumberOf(a.id) && kitNumberOf(a.id) < kitNumberOf(n.id)) { "kit: S, A, B, N are not in ‹ › order" }
        kit.open(s.id)
        kit.solveByApi()
        kit.advance(1700)
        assertEquals("set-up: ${s.id} solved", PuzzleState.SOLVED, kit.shownState())
        kit.open(a.id)
        val aBoard = placeUpTo(kit, a, 2)
        assertEquals("set-up: two pieces of ${a.id}", 2, aBoard.size)
        kit.press(KitTag.NAV_NEXT)
        kit.assertShows(b.id, "set-up: › from A")
        val bBoard = placeUpTo(kit, b, 1)
        assertEquals("set-up: one piece of ${b.id}", 1, bBoard.size)
        kit.press(KitTag.NAV_NEXT)
        kit.assertShows(n.id, "set-up: › from B")
        val expected = mapOf(
            s.id to Left(PuzzleState.SOLVED, emptyList()),
            a.id to Left(PuzzleState.IN_PROGRESS, sortedByPiece(aBoard)),
            b.id to Left(PuzzleState.IN_PROGRESS, sortedByPiece(bBoard)),
        )
        fun expect(id: String) = expected[id] ?: Left(PuzzleState.NEW, emptyList())

        // Back from N to S by ‹, then forward to N again by ›, checking every puzzle on the way.
        assertLeft(kit, n.id, expect(n.id), "N before leaving")
        walk(kit, from = n.id, to = s.id, tag = KitTag.NAV_PREV, expect = ::expect, what = "returning by ‹")
        walk(kit, from = s.id, to = n.id, tag = KitTag.NAV_NEXT, expect = ::expect, what = "returning by ›")

        // An activity recreation over the same controller, on A.
        walk(kit, from = n.id, to = a.id, tag = KitTag.NAV_PREV, expect = ::expect, what = "back to A")
        kit.recreate(rule.activityRule.scenario)
        assertLeft(kit, a.id, expect(a.id), "A in the first frame of a recreated activity")
        walk(kit, from = a.id, to = s.id, tag = KitTag.NAV_PREV, expect = ::expect, what = "after the recreation, by ‹")

        // A new controller over a new Persistence over the same store: it opens the last opened (S), then every puzzle
        // of the range is walked again.
        ps.flush()
        val next = kitControllerOver(rule, ps.store)
        val kit2 = KitScreen(rule, next.controller)
        kit2.recreate(rule.activityRule.scenario, reducedMotion = true)
        assertLeft(kit2, s.id, expect(s.id), "S, opened by a new controller over the same store")
        walk(kit2, from = s.id, to = n.id, tag = KitTag.NAV_NEXT, expect = ::expect, what = "over a new controller, by ›")
        next.flush()
        for ((id, left) in expected) {
            val saved = next.store.progressNow(id)
            assertEquals("$id's saved state", left.state, saved.state)
            assertSameBoard("$id's saved board", left.board, saved.placed)
        }
    }

    // ── REQ-025 A2: Restart ────────────────────────────────────────────────────────────────────────────

    @Test // REQ-025.A2
    fun afterRestartAllThePuzzlesPiecesAreInTheTrayAndThePuzzleTimeIsZero() {
        // REQ-025 A2: "After Restart, all the puzzle's pieces are in the tray and the puzzle time is zero." Statement: "WHEN
        // the player presses Restart, the game SHALL return all its pieces to the tray and set the puzzle to New." Oracle: the
        // store double holds a full-set puzzle In progress — three valid placements (kitSolutionLocks), puzzle time 37 s and
        // best time 90 s, opened last — so the time half is visible (WO-004 computes no time, O-04: the store carries what it
        // is given); another puzzle is In progress too; one tray piece is turned two steps by taps first. One tap on the
        // `restart` pill (a point clear of the pieces' 12 dp margins): every piece of the puzzle in the tray, inside its own
        // cell, at its resting turn and unmirrored (decisions.md 2026-10-15 "Browsing readings no REQ fixes": "Restart and
        // Retry put every piece in the tray at its resting turn, unmirrored"; S-9), the state New and no pill (REQ-025 rule:
        // Restart only In progress). Once the writer has flushed, the store holds the puzzle New, nothing placed, time 0 and
        // the best time 90 kept (IProgressStore KDoc: the caller "keeps bestSeconds on Restart"; S-2Δ "best kept"), and the
        // other puzzle as seeded. Left by › and come back by ‹, it is still New with every piece in the tray.
        val p = kitFirst(PuzzleKind.FULL)
        val board = kitSolutionLocks(p, 3, order = kitBottomFirst(p))
        assertEquals("set-up: three valid placements of ${p.id}", 3, board.size)
        val other = kitFirst(PuzzleKind.WARMUP)
        val otherSeed = kitInProgress(kitSolutionLocks(other, 2), time = 12, best = 30)
        val seed = mapOf(p.id to kitInProgress(board, time = 37, best = 90), other.id to otherSeed)
        val ps = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = p.id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        kit.assertShows(p.id, "set-up: the seeded puzzle opened")
        assertEquals("set-up: In progress", PuzzleState.IN_PROGRESS, kit.shownState())
        assertSameBoard("set-up: the seeded board shown", board, kit.boardFromNodes())
        val turned = p.pieces.first { piece -> board.none { it.piece == piece } }
        kit.tapTray(turned, 2)
        assertEquals("set-up: $turned turned two steps in the tray", TrayRest.turn(turned).next().next(), kit.key(PlayTags.trayPiece(turned), PlaySemantics.TurnKey))

        kit.tap(kit.restartTapPoint())
        kit.frames(2)
        assertRestarted(kit, p, "after one tap on Restart")
        ps.flush()
        assertEquals("the saved progress after Restart: New, nothing placed, time 0, best time kept", PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 90), ps.store.progressNow(p.id))
        assertEquals("the other puzzle's saved progress after Restart", otherSeed, ps.store.progressNow(other.id))

        kit.press(KitTag.NAV_NEXT)
        kit.press(KitTag.NAV_PREV)
        assertRestarted(kit, p, "left by › and back by ‹ after Restart")
    }

    // ── REQ-025 A3: the load check ─────────────────────────────────────────────────────────────────────

    @Test // REQ-025.A3
    fun aSavedPieceThatOverlapsTheSilhouettesEdgeIsInTheTrayAfterLoadingAndTheOthersStay() {
        // REQ-025 A3: "A saved piece that overlaps the current silhouette's edge is in the tray after loading; the others
        // stay." Rule: "IF a saved piece is no longer a valid lock against the current silhouette (a puzzle changed in an
        // update), THEN the game SHALL return that piece to the tray when the puzzle is loaded." Oracle: the store holds a
        // full-set puzzle In progress with three saved placements — two at solution poses the engine locks, and between them
        // a third piece moved half a unit away from the silhouette's centre so that it overlaps the edge (the kit's own
        // geometry: its centre inside the silhouette, a corner more than 0.05 units outside every solution polygon; and the
        // engine's `stillValid` drops exactly it). Loaded at the start (S-21: "On every open, the start included") and loaded
        // by an open (a tap on › from the puzzle before it): the two stay on the board at their saved placements, the third
        // is in the tray in its own cell, and the puzzle stays In progress. Once the writer has flushed, the store holds the
        // corrected board (S-21: "A correction is saved").
        val p = kitFirst(PuzzleKind.FULL)
        val kept = kitSolutionLocks(p, 2)
        assertEquals("set-up: two valid placements of ${p.id}", 2, kept.size)
        val bad = edgeOverlap(p, kept) ?: throw AssertionError("kit: no piece of ${p.id} moved half a unit outward overlaps the edge and is dropped by stillValid")
        val saved = listOf(kept[0], bad, kept[1])

        val ps = kitControllerOver(rule, KitDeviceStore(mapOf(p.id to kitInProgress(saved)), lastOpened = p.id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        assertLoaded(kit, p, kept, bad.piece, "loaded at the start")
        ps.flush()
        assertEquals("S-21: the corrected board is saved", kitInProgress(kept), ps.store.progressNow(p.id))

        val before = kitBefore(p.id)
        val ps2 = kitControllerOver(rule, KitDeviceStore(mapOf(p.id to kitInProgress(saved)), lastOpened = before.id))
        val kit2 = KitScreen(rule, ps2.controller)
        kit2.recreate(rule.activityRule.scenario)
        kit2.assertShows(before.id, "set-up: the puzzle before ${p.id} opened")
        kit2.press(KitTag.NAV_NEXT)
        assertLoaded(kit2, p, kept, bad.piece, "loaded by › from ${before.id}")
        ps2.flush()
        assertEquals("S-21: the corrected board is saved after an open", kitInProgress(kept), ps2.store.progressNow(p.id))
    }

    // ── Pin: Restart is shown only In progress ─────────────────────────────────────────────────────────

    @Test // pin: REQ-025 rule "Restart is shown only while the puzzle is in progress"; S-22 "Restart. A pill shown only In progress: 48 dp high (design-inputs §2 item 6), at the board's top-left + 10 dp"; decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn"; TYPE-006 (New → In progress when a drag starts on a tray piece; In progress → Solved at the last lock; back to New only by Restart or Retry); tag table "A node's presence follows the controller at once"; no token
    fun restartIsShownOnlyWhileThePuzzleIsInProgress() {
        val kit = KitScreen(rule)
        kit.show(reducedMotion = true)
        val p = kit.puzzle
        assertEquals("set-up: the fresh start is New", PuzzleState.NEW, kit.shownState())
        assertFalse("no Restart while New", kit.exists(KitTag.RESTART))
        var finger = kit.pickUpFromTray(p.pieces.first())
        assertEquals("In progress from the drag start", PuzzleState.IN_PROGRESS, kit.shownState())
        assertTrue("Restart while In progress (a piece dragged)", kit.exists(KitTag.RESTART))
        rule.onNode(hasTestTag(KitTag.RESTART), useUnmergedTree = true).assertIsDisplayed()
        val pill = kit.bounds(KitTag.RESTART)
        val board = kit.bounds(PlayTags.BOARD)
        kitAssertNear("the pill's left: the board's left + 10 dp", (board.left + kit.dp(10f)).toDouble(), pill.left.toDouble(), 2.0)
        kitAssertNear("the pill's top: the board's top + 10 dp", (board.top + kit.dp(10f)).toDouble(), pill.top.toDouble(), 2.0)
        assertTrue("the pill is at least 48 dp high (${pill.height / kit.density} dp)", pill.height >= kit.dp(48f) - 1f)
        assertEquals("the pill's label", kitTexts.restart, kit.text(KitTag.RESTART))
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.trayCell(p.pieces.last())).center, 3)
        kit.release()
        kit.advance(300)
        assertTrue("Restart while In progress with the board empty", kit.exists(KitTag.RESTART))
        kit.solveByApi()
        assertEquals("set-up: solved", PuzzleState.SOLVED, kit.shownState())
        assertFalse("no Restart from the frame the puzzle is Solved", kit.exists(KitTag.RESTART))
        kit.advance(1700)
        assertFalse("no Restart once the picture is shown", kit.exists(KitTag.RESTART))
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** What a puzzle was left with: its TYPE-006 state and its board (S-14 keys), in piece order. */
    private data class Left(val state: PuzzleState, val board: List<Placement>)

    private fun sortedByPiece(board: List<Placement>): List<Placement> = board.sortedBy { it.piece }

    /** Places up to [n] pieces of [p] (open) through S-2, edge-first as the engine allows; returns the controller's placements. */
    private fun placeUpTo(kit: KitScreen, p: PuzzleDefinition, n: Int): List<Placement> {
        var count = 0
        var progress = true
        while (count < n && progress) {
            progress = false
            for (piece in p.pieces) {
                if (count >= n) break
                if (kit.placed().any { it.piece == piece }) continue
                if (kit.placeByApi(piece)) { count++; progress = true }
            }
        }
        kit.frames()
        kit.advance(250)
        return kit.placed()
    }

    /** The same pieces with the same turn, mirror and centre (within the kit tolerance), whatever the order. */
    private fun assertSameBoard(what: String, expected: List<Placement>, actual: List<Placement>) {
        assertEquals("$what: the pieces", expected.map { it.piece }.sorted(), actual.map { it.piece }.sorted())
        for (e in expected) {
            val a = actual.single { it.piece == e.piece }
            assertTrue("$what: ${e.piece} expected at $e, is at $a", kitSamePlacement(e, a))
        }
    }

    private fun assertCameBack(kit: KitScreen, p: PuzzleDefinition, left: List<Placement>, what: String) {
        kit.assertShows(p.id, what)
        assertEquals("$what: In progress", PuzzleState.IN_PROGRESS, kit.shownState())
        assertTrue("$what: Restart is shown (In progress, REQ-025 rule)", kit.exists(KitTag.RESTART))
        assertSameBoard("$what: the board", left, kit.boardFromNodes())
        val inTray = p.pieces.filter { kit.exists(PlayTags.trayPiece(it)) }.toSet()
        assertEquals("$what: the other pieces in the tray", p.pieces - left.map { it.piece }.toSet(), inTray)
        kit.assertBoardDrawnAtItsKeys(what)
    }

    /** The shown puzzle is [id] with [left]'s state and board, the state read against what the screen draws. */
    private fun assertLeft(kit: KitScreen, id: String, left: Left, what: String) {
        kit.assertShows(id, what)
        assertEquals("$what: $id's state", left.state, kit.shownState())
        assertEquals("$what: Restart shown exactly while In progress (REQ-025 rule)", left.state == PuzzleState.IN_PROGRESS, kit.exists(KitTag.RESTART))
        if (left.state == PuzzleState.SOLVED) {
            assertTrue("$what: the solved picture (tag table)", kit.exists(PlayTags.SOLVED_PICTURE))
            assertTrue("$what: no piece drawn on a Solved puzzle", PieceId.entries.none { kit.exists(PlayTags.boardPiece(it)) })
        } else {
            assertFalse("$what: no solved picture on an unsolved puzzle", kit.exists(PlayTags.SOLVED_PICTURE))
            assertSameBoard("$what: $id's board", left.board, kit.boardFromNodes())
            kit.assertBoardDrawnAtItsKeys(what)
        }
    }

    /** Taps [tag] from [from] until [to] is shown, checking every puzzle on the way against [expect]. */
    private fun walk(kit: KitScreen, from: String, to: String, tag: String, expect: (String) -> Left, what: String) {
        assertEquals("$what: the walk starts at $from", from, kit.shownId())
        var guard = kitLibrary.puzzles.size
        while (kit.shownId() != to) {
            check(guard-- > 0) { "kit: $what never reached $to" }
            kit.press(tag)
            val id = kit.shownId()
            assertLeft(kit, id, expect(id), "$what: $id")
        }
    }

    private fun assertRestarted(kit: KitScreen, p: PuzzleDefinition, what: String) {
        kit.assertShows(p.id, what)
        assertEquals("$what: New", PuzzleState.NEW, kit.shownState())
        kit.assertAllInTray(p, what)
        for (piece in p.pieces) {
            assertEquals("$what: $piece at its resting turn", TrayRest.turn(piece), kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey))
            assertFalse("$what: $piece unmirrored", kit.key(PlayTags.trayPiece(piece), PlaySemantics.MirroredKey))
        }
        assertFalse("$what: no Restart pill once New", kit.exists(KitTag.RESTART))
    }

    private fun assertLoaded(kit: KitScreen, p: PuzzleDefinition, kept: List<Placement>, dropped: PieceId, what: String) {
        kit.assertShows(p.id, what)
        assertEquals("$what: still In progress", PuzzleState.IN_PROGRESS, kit.shownState())
        assertSameBoard("$what: the valid pieces stay", kept, kit.boardFromNodes())
        kit.assertBoardDrawnAtItsKeys(what)
        assertEquals("$what: $dropped is in the tray", listOf(PlayTags.trayPiece(dropped)), kit.homesOf(dropped))
        val cell = kit.bounds(PlayTags.trayCell(dropped))
        val box = kit.bounds(PlayTags.trayPiece(dropped))
        assertTrue("$what: $dropped's miniature $box is inside its cell $cell", cell.contains(box.topLeft) && cell.contains(box.bottomRight))
        val inTray = p.pieces.filter { kit.exists(PlayTags.trayPiece(it)) }.toSet()
        assertEquals("$what: every piece not kept is in the tray", p.pieces - kept.map { it.piece }.toSet(), inTray)
    }

    /**
     * A piece of [p] not in [kept], at its solution pose moved half a unit away from the silhouette's centre: its centre
     * still inside the silhouette, a corner more than 0.05 units outside every solution polygon (it overlaps the edge), and
     * `stillValid` over [kept]'s first, it, [kept]'s second keeps exactly the two.
     */
    private fun edgeOverlap(p: PuzzleDefinition, kept: List<Placement>): Placement? {
        val polys = p.solution.map { it.polygon }
        val c = kitSilhouetteCentre(p)
        for (sp in p.solution) {
            if (kept.any { it.piece == sp.piece }) continue
            val pose = kitPoseOf(sp.piece, sp.polygon)
            var dx = pose.centre.x - c.x
            var dy = pose.centre.y - c.y
            val len = hypot(dx, dy)
            if (len < 1e-9) { dx = 1.0; dy = 0.0 } else { dx /= len; dy /= len }
            val moved = Placement(sp.piece, pose.turn, pose.mirrored, Point(pose.centre.x + 0.5 * dx, pose.centre.y + 0.5 * dy))
            if (!kitInsideAny(moved.centre, polys)) continue
            val corners = kitEngine.vertices(moved.piece, moved.turn, moved.mirrored, moved.centre)
            val outside = corners.any { v -> !kitInsideAny(v, polys) && polys.minOf { kitDistToBoundary(v, it) } > 0.05 }
            if (!outside) continue
            if (kitEngine.stillValid(p, listOf(kept[0], moved, kept[1])) == kept) return moved
        }
        return null
    }
}
