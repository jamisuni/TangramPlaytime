// Acceptance kit (WO-004, TASK-019): the solved view — the picture without piece positions, Retry, the best time and
// Next (designs/WO-004-design.md §2 S-1Δ, S-2Δ, S-3Δ `Celebration`, S-21, S-22 "Solved row", "What survives"; §3 "Never
// show a solved puzzle's piece positions"; decisions.md 2026-10-15 rows). Controllers save through `Persistence` over the
// kit's store double where a line needs a seeded or a read-back store. The quantifier line REQ-026 A1 takes the design's
// adversarial path in its own test: `recreate()` right after a solve, at the first frame, with the clock paused. During a
// solve's own celebration pieces may be drawn (design §3); nothing here asserts otherwise.
package app.tangramplaytime.play

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SolvedViewKitTest {

    // The ComponentActivity host, declared so the recreation legs can recreate it (KitScreen.recreate).
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // ── REQ-026 A1: never the piece positions ──────────────────────────────────────────────────────────

    @Test // REQ-026.A1
    fun aSolvedPuzzleNeverShowsWhereThePiecesWere() {
        // REQ-026 A1: "A solved puzzle never shows where the pieces were." Statement: "WHILE a solved puzzle is shown, the
        // game SHALL display only its picture, without piece positions"; rule: "The tray is replaced by the Retry, best time
        // and Next row." Oracle, at the first frame of each way a solved puzzle comes on screen and over the next 60 frames
        // (about 1 s, past the 600 ms in which a replayed celebration would still draw its pieces): no `board-piece`, `drag-piece`, `tray-cell` or `tray-ghost` node, no landing preview, no confetti (no
        // celebration replays: design "What survives"; decisions.md 2026-10-12 "Carried to WO-004 #Browsing" (2)); at the
        // first frame and at the end, the `solved-picture` node and, 3 px inside the silhouette, no pixel of a TYPE-001 piece
        // colour the picture does not name and none of the flat silhouette colour (decisions.md 2026-10-12 "The solve, as
        // drawn": "a puzzle opened already solved shows the picture at once").
        //  (a) opened by › — the store holding it Solved with stale placements (S-21: "NEW or SOLVED: an empty board");
        //  (b) a cold start onto it: a new controller over the same kind of store, composed in a recreated activity;
        //  (c) the design's adversarial path: a solve through S-2, three frames into its celebration, then `recreate()` over
        //      the same controller (S-3Δ: a celebration already set counts as shown and starts at its end);
        //  (d) the celebration cue across opens (WO-003 N1): a second puzzle solved, then ‹ back to the first and › to the
        //      second — neither replays.
        val list = kitLibrary.puzzles
        val first = list[0]
        val stale = list[1]
        val third = list[2]
        val staleBoard = kitSolutionLocks(stale, stale.pieces.size)
        assertEquals("set-up: the stale board covers ${stale.id}", stale.pieces.size, staleBoard.size)
        val seed = mapOf(stale.id to PuzzleProgress(PuzzleState.SOLVED, staleBoard, 40, 40))
        val ps = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = first.id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        kit.assertShows(first.id, "set-up")

        // (a) opened by ›
        kit.tap(kit.bounds(KitTag.NAV_NEXT).center)
        assertNoPiecePositions(kit, stale, "(a) the first frame after › onto ${stale.id}", pixels = true)
        watch(kit, stale, 60, "(a) opened by ›")

        // (b) a cold start onto it
        val cold = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = stale.id))
        val kitB = KitScreen(rule, cold.controller)
        kitB.recreate(rule.activityRule.scenario)
        assertNoPiecePositions(kitB, stale, "(b) the first frame of a cold start onto ${stale.id}", pixels = true)
        watch(kitB, stale, 60, "(b) a cold start")

        // (c) recreate() right after a solve
        kitB.press(KitTag.NAV_PREV)
        kitB.assertShows(first.id, "(c) set-up: ‹ to ${first.id}")
        assertEquals("(c) set-up: ${first.id} is New", PuzzleState.NEW, kitB.shownState())
        kitB.solveByApi()
        assertEquals("(c) set-up: ${first.id} solved", PuzzleState.SOLVED, kitB.shownState())
        kitB.frames(3)
        kitB.recreate(rule.activityRule.scenario)
        assertNoPiecePositions(kitB, first, "(c) the first frame of an activity recreated right after the solve", pixels = true)
        watch(kitB, first, 60, "(c) after the recreation")

        // (d) the celebration cue across opens
        kitB.press(KitTag.NAV_NEXT)
        kitB.press(KitTag.NAV_NEXT)
        kitB.assertShows(third.id, "(d) set-up: › › to ${third.id}")
        kitB.solveByApi()
        kitB.advance(3000)
        assertEquals("(d) set-up: ${third.id} solved", PuzzleState.SOLVED, kitB.shownState())
        kitB.press(KitTag.NAV_PREV)
        kitB.tap(kitB.bounds(KitTag.NAV_PREV).center)
        assertNoPiecePositions(kitB, first, "(d) the first frame after ‹ back to ${first.id}", pixels = true)
        watch(kitB, first, 60, "(d) back to ${first.id}")
        kitB.press(KitTag.NAV_NEXT)
        kitB.tap(kitB.bounds(KitTag.NAV_NEXT).center)
        assertNoPiecePositions(kitB, third, "(d) the first frame after › back to ${third.id}", pixels = true)
        watch(kitB, third, 60, "(d) back to ${third.id}")
    }

    // ── REQ-026 A2: Retry ──────────────────────────────────────────────────────────────────────────────

    @Test // REQ-026.A2
    fun retryLeadsToAnEmptySilhouetteWithAllPiecesInTheTray() {
        // REQ-026 A2: "Retry leads to an empty silhouette with all pieces in the tray." Statement: "WHEN the player presses
        // Retry, the game SHALL start the puzzle again from an empty board." Oracle, by a tap on `retry`, for a puzzle opened
        // already Solved from the store (best time 45 s) and for a puzzle just solved through S-2 (after its row appeared):
        // every piece of the puzzle in the tray inside its own cell, at its resting turn and unmirrored (decisions.md
        // 2026-10-15 "Browsing readings no REQ fixes"; S-9), no board piece, no picture, the state New, and 3 px inside the
        // silhouette every pixel the one silhouette colour (an empty silhouette: REQ-011's "One colour for the whole
        // silhouette", S-12). Once the writer has flushed, the store holds the opened puzzle New, nothing placed, time 0 and
        // its best time 45 kept (REQ-026 rule: "Retry keeps the best time"; IProgressStore KDoc).
        val p = kitFirst(PuzzleKind.FULL)
        val ps = kitControllerOver(rule, KitDeviceStore(mapOf(p.id to PuzzleProgress(PuzzleState.SOLVED, emptyList(), 45, 45)), lastOpened = p.id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        kit.assertShows(p.id, "set-up: the solved puzzle opened")
        assertEquals("set-up: Solved", PuzzleState.SOLVED, kit.shownState())
        assertTrue("set-up: Retry shown at once for a puzzle opened already solved", kit.exists(KitTag.RETRY))
        kit.press(KitTag.RETRY)
        assertRetried(kit, p, "after Retry on ${p.id} opened solved")
        ps.flush()
        assertEquals("the saved progress after Retry: New, nothing placed, time 0, best time kept", PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 45), ps.store.progressNow(p.id))

        val mini = kitLibrary.puzzles.first()
        kit.open(mini.id)
        kit.solveByApi()
        kit.advance(2000)
        assertTrue("set-up: Retry shown once ${mini.id}'s picture is fully shown", kit.exists(KitTag.RETRY))
        kit.press(KitTag.RETRY)
        assertRetried(kit, mini, "after Retry on ${mini.id} just solved")
        ps.flush()
        val saved = ps.store.progressNow(mini.id)
        assertEquals("${mini.id}'s saved state after Retry", PuzzleState.NEW, saved.state)
        assertTrue("${mini.id}: nothing saved on its board after Retry", saved.placed.isEmpty())
        assertEquals("${mini.id}: the puzzle time 0 after Retry (S-2Δ)", 0, saved.timeSeconds)
    }

    // ── Pins: the solved row ───────────────────────────────────────────────────────────────────────────

    @Test // pin: decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn" ("Solved row: replaces the tray from the frame the picture is fully shown …, or at once when a solved puzzle is opened. Retry, best time and Next fill thirds of the tray strip, with buttons ≥ 48 dp high"; WO-004 checkpoint item 2); S-22 "Solved row … It splits the tray strip into thirds: Retry, best time (inert), Next"; REQ-023 rule (the picture fades in over 800 ms, starting 600 ms after the last lock); REQ-026 rule "The tray is replaced by the Retry, best time and Next row"; no token
    fun theSolvedRowReplacesTheTrayOnceThePictureIsFullyShownInThirds() {
        val kit = KitScreen(rule)
        kit.show()
        val p = kitLibrary.puzzles.first()
        kit.open(p.id)
        val tray = kit.bounds(PlayTags.TRAY)
        kit.solveByApi()
        kit.advance(1300)
        assertFalse("no solved row 1.3 s after the last lock (the picture is fully shown at 1.4 s)", kit.exists(KitTag.SOLVED_ROW))
        kit.advance(200)
        kit.frames()
        assertTrue("the solved row once the picture is fully shown", kit.exists(KitTag.SOLVED_ROW))
        for (piece in p.pieces) assertFalse("no tray cell for $piece with the row shown", kit.exists(PlayTags.trayCell(piece)))
        val row = kit.bounds(KitTag.SOLVED_ROW)
        assertTrue("the row $row lies in the tray strip $tray", row.left >= tray.left - 2f && row.right <= tray.right + 2f && row.top >= tray.top - 2f && row.bottom <= tray.bottom + 2f)
        val third = tray.width / 3f
        for ((i, tag) in listOf(KitTag.RETRY, KitTag.BEST_TIME, KitTag.SOLVED_NEXT).withIndex()) {
            val r = kit.bounds(tag)
            val lo = tray.left + i * third
            assertTrue("`$tag` ${r.center} is in third ${i + 1} of the tray strip [$lo, ${lo + third}]", r.center.x >= lo && r.center.x <= lo + third)
            assertTrue("`$tag` lies in the tray strip", r.center.y >= tray.top && r.center.y <= tray.bottom)
            if (tag != KitTag.BEST_TIME) assertTrue("`$tag` is at least 48 dp high (${r.height / kit.density} dp)", r.height >= kit.dp(48f) - 1f)
        }
        assertEquals("Retry's label", kitTexts.retry, kit.text(KitTag.RETRY))
        assertEquals("Next's label", kitTexts.next, kit.text(KitTag.SOLVED_NEXT))
        // At once for a puzzle opened already solved.
        kit.press(KitTag.NAV_NEXT)
        kit.tap(kit.bounds(KitTag.NAV_PREV).center)
        assertTrue("the solved row in the first frame of a solved puzzle opened by ‹", kit.exists(KitTag.SOLVED_ROW))
    }

    @Test // pin: decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn" ("The best time reads "–" while none is stored, which is always until WO-008 computes times; otherwise m:ss"; WO-004 checkpoint item 3); S-11Δ `noTime` "–"; S-1Δ `bestSeconds`; REQ-026 statement ("the best time"); no token
    fun theBestTimeReadsADashWhileNoneIsStoredAndMinutesAndSecondsOtherwise() {
        val list = kitLibrary.puzzles
        val none = list[0]
        val some = list[1]
        val seed = mapOf(none.id to kitSolvedProgress(time = 0, best = null), some.id to kitSolvedProgress(time = 75, best = 75))
        val ps = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = none.id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        val dash = kit.text(KitTag.BEST_TIME)
        assertTrue("no best time stored: `best-time` reads '$dash', which holds '${kitTexts.noTime}'", dash.contains(kitTexts.noTime))
        kit.press(KitTag.NAV_NEXT)
        kit.assertShows(some.id, "set-up")
        val time = kit.text(KitTag.BEST_TIME)
        assertTrue("best time 75 s: `best-time` reads '$time', which holds 1:15", time.contains("1:15"))
        assertFalse("best time 75 s: `best-time` reads '$time', with no dash", time.contains(kitTexts.noTime))
    }

    @Test // pin: decisions.md 2026-10-15 "Browsing readings no REQ fixes" ("The solved row's Next = › (the next puzzle in order, not the next unsolved)"); S-22 "best time (inert)"; no token
    fun nextInTheSolvedRowShowsTheNextPuzzleInOrderAndTheBestTimeIsInert() {
        val list = kitLibrary.puzzles
        val p = list[3]
        val seed = mapOf(p.id to kitSolvedProgress(), list[4].id to kitSolvedProgress())
        val ps = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = p.id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        kit.press(KitTag.BEST_TIME)
        kit.assertShows(p.id, "a tap on the best time")
        assertEquals("a tap on the best time keeps the puzzle Solved", PuzzleState.SOLVED, kit.shownState())
        kit.press(KitTag.SOLVED_NEXT)
        kit.assertShows(list[4].id, "Next in the solved row (the next puzzle, Solved too)")
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** No piece position anywhere on screen (the line's meaning, read as nodes and, when [pixels], as board pixels). */
    private fun assertNoPiecePositions(kit: KitScreen, p: PuzzleDefinition, what: String, pixels: Boolean) {
        if (pixels) kit.assertShows(p.id, what) else assertEquals("$what: the puzzle shown", p.id, kit.shownId())
        assertEquals("$what: Solved", PuzzleState.SOLVED, kit.shownState())
        for (piece in PieceId.entries) {
            assertFalse("$what: a `board-piece-$piece` node", kit.exists(PlayTags.boardPiece(piece)))
            assertFalse("$what: a `drag-piece-$piece` node", kit.exists(PlayTags.dragPiece(piece)))
            assertFalse("$what: a `tray-cell-$piece` node (the tray is replaced)", kit.exists(PlayTags.trayCell(piece)))
            assertFalse("$what: a `tray-ghost-$piece` node", kit.exists(PlayTags.trayGhost(piece)))
        }
        assertFalse("$what: a landing preview", kit.exists(PlayTags.LANDING_PREVIEW))
        assertFalse("$what: confetti (no celebration replays)", kit.exists(PlayTags.CONFETTI))
        if (!pixels) return
        assertTrue("$what: the `solved-picture` node", kit.exists(PlayTags.SOLVED_PICTURE))
        val board = kit.capture(PlayTags.BOARD)
        // Amended WO-010 (F3, recirculation 6, a look-alike; a corrected check — decisions.md 2026-10-20 "F3's cause is the
        // REQ-023 A1 leg's inside mask, not the product or the data"): "3 px inside" is measured from the exact outline
        // (`kit.silhouetteBands`), no longer the polygons widened 0.75 px and eroded by a 7 × 7 square, which at a 315° notch held
        // pixels 0.46 px from the outline, where the picture's own outline stroke rightly paints the flat silhouette colour
        // (decisions.md 2026-10-12 "The solve, as drawn"): F3's false red, and shapes-warmup-1 (list[2]) has such a notch.
        val inside = kit.silhouetteBands(p).inside
        val artColours = kitArtColours(p.art)
        val foreign = kitPieceColours().filterValues { pc -> artColours.none { kitNear(it, pc, 6f / 255f) } }
        val silhouetteInArt = artColours.any { kitNear(it, PlayPalette.silhouette, 6f / 255f) }
        var pieceColoured = 0
        var flat = 0
        var first = ""
        inside.forEachSet { x, y ->
            val c = board[x, y]
            for ((id, pc) in foreign) if (kitNear(c, pc, 6f / 255f)) { pieceColoured++; if (first.isEmpty()) first = "($x, $y) = ${kitHex(c)} ($id)" }
            if (!silhouetteInArt && kitNear(c, PlayPalette.silhouette, 6f / 255f)) { flat++; if (first.isEmpty()) first = "($x, $y) = ${kitHex(c)} (silhouette)" }
        }
        assertEquals("$what: piece-coloured pixels 3 px inside the silhouette; first $first", 0, pieceColoured)
        assertEquals("$what: flat silhouette pixels 3 px inside the silhouette (the picture is not fully shown); first $first", 0, flat)
    }

    /** Over [frames] frames, no piece position by node; the pixels again at the end. */
    private fun watch(kit: KitScreen, p: PuzzleDefinition, frames: Int, what: String) {
        for (f in 1..frames) {
            kit.frames()
            assertNoPiecePositions(kit, p, "$what, frame $f (${f * 16} ms)", pixels = false)
        }
        assertNoPiecePositions(kit, p, "$what, after $frames frames", pixels = true)
    }

    private fun assertRetried(kit: KitScreen, p: PuzzleDefinition, what: String) {
        kit.assertShows(p.id, what)
        assertEquals("$what: New", PuzzleState.NEW, kit.shownState())
        kit.assertAllInTray(p, what)
        for (piece in p.pieces) {
            assertEquals("$what: $piece at its resting turn", TrayRest.turn(piece), kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey))
            assertFalse("$what: $piece unmirrored", kit.key(PlayTags.trayPiece(piece), PlaySemantics.MirroredKey))
        }
        assertFalse("$what: no solved picture (tag table: a node's presence follows the controller at once)", kit.exists(PlayTags.SOLVED_PICTURE))
        assertFalse("$what: no solved row", kit.exists(KitTag.SOLVED_ROW))
        kit.advance(500) // the pixels once any fade the drawing may play is over (no REQ or row times one)
        val board = kit.capture(PlayTags.BOARD)
        // Amended WO-010 (F3, recirculation 6, a look-alike; a corrected check — decisions.md 2026-10-20 "F3's cause is the
        // REQ-023 A1 leg's inside mask, not the product or the data"): "3 px inside" is measured from the exact outline
        // (`kit.silhouetteBands`), no longer the polygons widened 0.75 px and eroded by a 7 × 7 square, which at a 315° notch held
        // pixels 0.46 px from the outline, whose anti-aliased blend with the board may miss the tolerance (a false red).
        val inside = kit.silhouetteBands(p).inside
        var wrong = 0
        var first = ""
        inside.forEachSet { x, y ->
            if (!kitNear(board[x, y], PlayPalette.silhouette)) { wrong++; if (first.isEmpty()) first = "($x, $y) = ${kitHex(board[x, y])}" }
        }
        assertEquals("$what: pixels 3 px inside the silhouette that are not ${kitHex(PlayPalette.silhouette)} (an empty silhouette); first $first", 0, wrong)
    }
}
