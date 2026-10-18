// Acceptance kit (WO-006, TASK-030): the Tablet layout in the real app (REQ-036; TYPE-007; tasks.md § WO-006 S-32, S-34,
// S-35, seam notes 2–4, "What survives a rotation"; decisions.md 2026-10-17 Q1, Q2 rows; the 2026-10-12 TASK-015 row:
// `onStop` sends a running drag home, also on a rotation).
// - Where it runs: every image. On the tablet image the display is its own 1280 × 800 dp (nothing overridden, seam note 4);
//   on the dev and floor phone images it is constructed by a display override of 2160 × 1350 px at 270 dpi (a pair every
//   image accepts), read back from `wm` and the launched activity, and the activity is started after the override: a tablet
//   the game starts on (decisions.md 2026-10-18 "Worker wins three contests", part 1; `KitApp.useTablet`). Landscape is the
//   frozen rotation 0 and portrait the frozen rotation 90 (seam note 4); every turn inside a test is a turn of the running
//   activity. The insets the one-row checks use are read once settled (the ruling's part 2), and the rotation and display
//   are restored through the shell alone, bounded, before the activity is closed (its part 3; `KitApp.restoreRule`: never
//   a device change under the home screen).
// - The app's controller is one per process and shared by every class of the run: each test leaves it, and the device, as
//   it found them (seam note 3; `KitApp.restoreRule()`).
package app.tangramplaytime.app

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class TabletLayoutKitTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val kit = KitApp(composeRule)

    @get:Rule(order = 1)
    val kitRestore: TestRule = kit.restoreRule()

    @Test // REQ-036.A1
    fun onTabletsOf1280By800And800By1280DpTheTrayIsOneRow() {
        // REQ-036 A1: "On 1280 × 800 dp and 800 × 1280 dp tablets, the tray is one row." Statement: WHERE the layout class is
        // Tablet, the game "SHALL show the tray as a single row across the bottom"; rule 2: insets "behave as on the phone".
        // Oracle, on a full puzzle (seven cells) at each size (seam note 4), read back from the launched activity:
        //  - one horizontal line crosses all seven `tray-cell-<ID>` boxes (the lowest top is above the highest bottom);
        //  - their centres run left to right in TYPE-001 order ("Tray order, left to right and top to bottom");
        //  - each lies inside the window and the safe area (S-32: "Every cell … lie[s] inside the safe area"; REQ-036 rule 2);
        //  - "across the bottom": each lies below `board` (S-32: the board ends 8 dp above the tray's top).
        kit.start("REQ-036 A1")
        val p = kit.firstNew(PuzzleKind.FULL, "a full puzzle") { it.pieces.size == 7 }
        for (portrait in listOf(false, true)) {
            val leg = if (portrait) "the 800 × 1280 dp tablet (portrait)" else "the 1280 × 800 dp tablet (landscape)"
            kit.useTablet(portrait, leg)
            kit.openByController(p.id, "$leg: ${p.id}")
            assertOneRow(leg, p)
        }
    }

    @Test // pin: REQ-012 rule ("A mini puzzle (REQ-045) shows only its own pieces, in the same relative order") on the tablet's one row (REQ-036 statement); no token
    fun aMiniPuzzlesCellsFormOneRowInTrayOrderToo() {
        // Oracle: the first New mini puzzle at both tablet sizes: exactly its own pieces have cells, and those cells form one row
        // in TYPE-001 relative order, inside the safe area, below the board.
        kit.start("the tablet row, mini")
        // The fixture discriminates where the bundle allows: a mini with pieces from both of the phone's rows (REQ-035 rule 1).
        val phoneRow1 = setOf(PieceId.LT1, PieceId.LT2, PieceId.MT)
        val mini = runCatching {
            kit.firstNew(PuzzleKind.MINI, "a mini spanning both phone rows") { m -> m.pieces.any { it in phoneRow1 } && m.pieces.any { it !in phoneRow1 } }
        }.getOrNull() ?: kit.firstNew(PuzzleKind.MINI, "a mini puzzle")
        for (portrait in listOf(false, true)) {
            val leg = if (portrait) "the 800 × 1280 dp tablet (portrait)" else "the 1280 × 800 dp tablet (landscape)"
            kit.useTablet(portrait, leg)
            kit.openByController(mini.id, "$leg: ${mini.id}")
            val withCells = PieceId.entries.filter { kit.exists(PlayTags.trayCell(it)) }.toSet()
            assertEquals("$leg: cells for exactly ${mini.id}'s pieces", mini.pieces, withCells)
            assertOneRow(leg, mini)
        }
    }

    @Test // REQ-036.A2
    fun rotatingMidPuzzleKeepsEveryPlacedPieceAndADragRunningAtTheTurnGoesHome() {
        // REQ-036 A2: "Rotating mid-puzzle keeps every placed piece." Rule 1: "Turning the tablet re-lays the screen and keeps
        // every piece's state." Oracle ("What survives a rotation": kept — the open puzzle and its TYPE-006 state, every placed
        // piece at its placement in puzzle units, redrawn through the new `BoardMapping`):
        //  - three pieces placed through S-2 at engine-confirmed solution poses (the parallelogram mirrored among them where the
        //    bundle allows, so a lost mirror shows), on the 1280 × 800 dp tablet;
        //  - after the turn to portrait, and after the turn back: the same puzzle, In progress, the same three `board-piece`
        //    nodes at the same piece, turn, mirror and centre (within 1e-6), each node's box the box of the engine's corners
        //    through the new mapping (±2 px), the controller's board the same three, the other pieces in the tray.
        // The quantifier's adversarial path, in this test: a turn while a drag runs. A fourth piece is dragged through S-2's
        // `startDrag` and held over its solution pose with `overBoard` true (a drop there would lock it: the fixture tells a
        // drop from a return home); after the turn it is back in its tray cell, not on the board, no drag runs, and the
        // three placed pieces are kept ("What survives a rotation": ended — a running drag, `onStop` sends it home).
        kit.start("REQ-036 A2")
        kit.useTablet(portrait = false, what = "the 1280 × 800 dp tablet")
        val (p, three) = kit.threeToLeave("three pieces to keep across the turn")
        kit.openByController(p.id, "${p.id} New")
        three.forEach { kit.placeByController(it, "a piece to keep") }
        assertKept(p, three, "landscape, before the turn")
        kit.useTablet(portrait = true, what = "after the turn to portrait")
        assertKept(p, three, "after the turn to portrait")
        kit.useTablet(portrait = false, what = "after the turn back to landscape")
        assertKept(p, three, "after the turn back to landscape")

        val held = kitAppLockOrder(p, three)?.first() ?: throw AssertionError("kit: no fourth piece of ${p.id} locks after the three")
        kit.onController {
            // S-2: a drag starts with the tray turn and mirror, so it is brought to the pose's (the flip first, then the twist).
            if (held.piece == PieceId.PG && trayMirrored(PieceId.PG) != held.mirrored) check(pressBadge()) { "kit: pressBadge() refused" }
            check(startDrag(held.piece, held.centre)) { "kit: startDrag(${held.piece}) refused" }
            val from = dragged?.turn?.step ?: error("kit: no drag after startDrag(${held.piece})")
            val steps = ((held.turn.step - from) % 8 + 8) % 8
            if (steps != 0) twistDrag(if (steps <= 4) steps else steps - 8)
            moveDrag(held.centre, overBoard = true)
            updatePreview()
        }
        kit.awaitExists(PlayTags.dragPiece(held.piece), "a drag running before the turn")
        assertTrue(
            "fixture: a drop now would lock ${held.piece} (the preview is its pose), so a return home differs from a drop",
            kitAppSame(kit.onController { preview }, held),
        )
        kit.useTablet(portrait = true, what = "a turn to portrait with a drag running")
        assertTrue("after the turn: no drag runs", kit.onController { dragged } == null)
        assertTrue("after the turn: no `drag-piece`", !kit.exists(PlayTags.dragPiece(held.piece)))
        assertTrue("after the turn: ${held.piece} is not on the board", !kit.exists(PlayTags.boardPiece(held.piece)))
        assertTrue("after the turn: ${held.piece}'s miniature is in the tray", kit.exists(PlayTags.trayPiece(held.piece)))
        val box = kit.windowBounds(PlayTags.trayPiece(held.piece))
        val cell = kit.windowBounds(PlayTags.trayCell(held.piece))
        assertTrue("after the turn: ${held.piece}'s miniature $box lies inside its cell $cell", kitAppWithin(box, cell))
        assertKept(p, three, "after a turn with a drag running")
        kit.useTablet(portrait = false, what = "back to landscape")
        assertKept(p, three, "back in landscape after the drag went home")
    }

    @Test // pin: tasks.md § WO-006 "What survives a rotation" ("Kept … the tray turns and the parallelogram's tray mirror (held in the controller; never saved, decisions.md 2026-10-11 F36)"); no token
    fun aTrayTurnAndTheParallelogramsTrayMirrorSurviveARotation() {
        // Oracle: on a New full puzzle in landscape, LT1 turned two steps in the tray and the parallelogram mirrored in the tray
        // through S-2 (`tapTray`, `pressBadge`); after the turn to portrait, `tray-piece-LT1`'s TurnKey and `tray-piece-PG`'s
        // MirroredKey still show them, as does the controller.
        kit.start("tray turns across a rotation")
        kit.useTablet(portrait = false, what = "the 1280 × 800 dp tablet")
        val p = kit.firstNew(PuzzleKind.FULL, "a full puzzle with the parallelogram") { PieceId.PG in it.pieces && it.pieces.size == 7 }
        kit.openByController(p.id, "${p.id} New")
        val restTurn = kit.key(PlayTags.trayPiece(PieceId.LT1), PlaySemantics.TurnKey)
        val restMirror = kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.MirroredKey)
        kit.onController {
            tapTray(PieceId.LT1)
            tapTray(PieceId.LT1)
            check(pressBadge()) { "kit: pressBadge() refused with PG in the tray" }
        }
        kit.idle()
        val turned = restTurn.next().next()
        assertEquals("before the turn: LT1 two steps on", turned, kit.key(PlayTags.trayPiece(PieceId.LT1), PlaySemantics.TurnKey))
        assertEquals("before the turn: PG mirrored", !restMirror, kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.MirroredKey))
        kit.useTablet(portrait = true, what = "after the turn to portrait")
        assertEquals("after the turn: `tray-piece-LT1` TurnKey", turned, kit.key(PlayTags.trayPiece(PieceId.LT1), PlaySemantics.TurnKey))
        assertEquals("after the turn: `tray-piece-PG` MirroredKey", !restMirror, kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.MirroredKey))
        assertEquals("after the turn: the controller's LT1 tray turn", turned, kit.onController { trayTurn(PieceId.LT1) })
        assertEquals("after the turn: the controller's PG tray mirror", !restMirror, kit.onController { trayMirrored(PieceId.PG) })
    }

    @Test // pin: tasks.md § WO-006 "What survives a rotation" ("Kept … the overview's open state"); REQ-050 rule ("The grid is an overlay over the current puzzle; closing it … returns to the same puzzle state"); no token
    fun anOpenGridStaysOpenAcrossARotation() {
        // Oracle: the grid opened through S-2Δ `openOverview` in landscape; after the turn to portrait `overview` is shown, the
        // controller's `overviewOpen` holds, and the same puzzle is open behind it.
        kit.start("the grid across a rotation")
        kit.useTablet(portrait = false, what = "the 1280 × 800 dp tablet")
        val id = kit.shownId()
        kit.onController { openOverview() }
        kit.awaitExists(KitAppTag.OVERVIEW, "the grid open before the turn")
        kit.useTablet(portrait = true, what = "after the turn to portrait")
        assertTrue("after the turn: `overview` is shown", kit.exists(KitAppTag.OVERVIEW))
        assertTrue("after the turn: the controller's overviewOpen", kit.onController { overviewOpen })
        assertEquals("after the turn: the same puzzle behind the grid", id, kit.shownId())
    }

    @Test // pin: tasks.md § WO-006 "What survives a rotation" ("the cue serials: a pulse, shake or celebration set before the new composition counts as shown, so a just-solved puzzle stands at the end of its timeline with its solved row", S-3Δ); no token
    fun aCelebrationAlreadyShownIsNotPlayedAgainAfterARotation() {
        // Oracle: a full puzzle solved through S-2 in landscape and its timeline played out (the solved row shown). With the
        // compose clock paused, the tablet turns to portrait; from the first frame of the new composition on, `solved-row`
        // and `solved-picture` are there, no `confetti` and no `board-piece`; ten more frames: still no `confetti`.
        // (A replayed celebration starts with the pieces and confetti and shows the row about 1.4 s later.)
        kit.start("a celebration across a rotation")
        kit.useTablet(portrait = false, what = "the 1280 × 800 dp tablet")
        val p = kit.firstNew(PuzzleKind.FULL, "a full puzzle") { it.pieces.size == 7 }
        kit.openByController(p.id, "${p.id} New")
        kit.solveByController("solved before the turn")
        kit.awaitSolvedRow("the celebration played out before the turn")
        assertTrue("fixture: no confetti once the row shows", !kit.exists(PlayTags.CONFETTI))
        kit.pauseClock()
        kit.useTablet(portrait = true, what = "after the turn to portrait, clock paused", needScreen = false)
        firstFramesOfTheNewComposition("after the turn") {
            assertTrue("$it: `solved-row` from the first frame", kit.exists(KitAppTag.SOLVED_ROW))
            assertTrue("$it: `solved-picture` from the first frame", kit.exists(PlayTags.SOLVED_PICTURE))
            assertTrue("$it: no `confetti`", !kit.exists(PlayTags.CONFETTI))
            assertTrue("$it: no `board-piece`", PieceId.entries.none { piece -> kit.exists(PlayTags.boardPiece(piece)) })
        }
        repeat(10) {
            kit.frames(1)
            assertTrue("frame ${it + 1} after the turn: no `confetti`", !kit.exists(PlayTags.CONFETTI))
        }
    }

    @Test // pin: tasks.md § WO-006 "What survives a rotation" (the cue serials: "a pulse … set before the new composition counts as shown"); S-2 `drop()` (`pulse` set in a mini puzzle on a missed drop, REQ-051); no token
    fun aCornerPulseAlreadyShownIsNotPlayedAgainAfterARotation() {
        // Oracle: on the first New mini puzzle in landscape, with the compose clock paused, a piece dragged through S-2 and
        // dropped off the board (`moveDrag(…, overBoard = false)`, `drop()`) goes home and sets `pulse`; within two frames
        // `corner-pulse` shows (the fixture sees a pulse play), and after 1.5 s of clock it is gone. The tablet then turns to
        // portrait: from the first frame of the new composition on, and for ten more frames, no `corner-pulse`.
        kit.start("a corner pulse across a rotation")
        kit.useTablet(portrait = false, what = "the 1280 × 800 dp tablet")
        val mini = kit.firstNew(PuzzleKind.MINI, "a mini puzzle")
        kit.openByController(mini.id, "${mini.id} New")
        val piece = mini.pieces.first()
        kit.pauseClock()
        val missed = kit.onController {
            check(startDrag(piece, kitAppPose(mini, piece).centre)) { "kit: startDrag($piece) refused" }
            moveDrag(kitAppPose(mini, piece).centre, overBoard = false)
            drop()
        }
        assertTrue("fixture: the drop off the board did not lock", missed == null)
        assertTrue("fixture: the missed drop in a mini set a pulse (S-2 `drop()`)", kit.onController { pulse } != null)
        kit.frames(2)
        assertTrue("fixture: the pulse plays before the turn (`corner-pulse` within two frames)", kit.exists(PlayTags.CORNER_PULSE))
        kit.advance(1_500)
        assertTrue("fixture: the pulse has played out before the turn (tag table: about 600 ms)", !kit.exists(PlayTags.CORNER_PULSE))
        kit.useTablet(portrait = true, what = "after the turn to portrait, clock paused", needScreen = false)
        firstFramesOfTheNewComposition("after the turn") {
            assertTrue("$it: no `corner-pulse`", !kit.exists(PlayTags.CORNER_PULSE))
        }
        repeat(10) {
            kit.frames(1)
            assertTrue("frame ${it + 1} after the turn: no `corner-pulse`", !kit.exists(PlayTags.CORNER_PULSE))
        }
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** The one-row checks of REQ-036 A1 on [p]'s cells (see that test's comment for the sources). */
    private fun assertOneRow(leg: String, p: PuzzleDefinition) {
        val ins = kit.settledInsets(leg)
        val ordered = PieceId.entries.filter { it in p.pieces }
        val cells = ordered.map { it to kit.windowBounds(PlayTags.trayCell(it)) }
        val lineTop = cells.maxOf { it.second.top }
        val lineBottom = cells.minOf { it.second.bottom }
        assertTrue("$leg: one horizontal line crosses every cell of ${p.id} (lowest top $lineTop < highest bottom $lineBottom): $cells", lineTop < lineBottom)
        for ((a, b) in cells.zipWithNext()) {
            assertTrue("$leg: ${a.first}'s cell ${a.second} lies left of ${b.first}'s ${b.second} (TYPE-001 order)", a.second.center.x < b.second.center.x)
        }
        val board = kit.windowBounds(PlayTags.BOARD)
        for ((piece, cell) in cells) {
            assertTrue("$leg: $piece's cell $cell lies inside the window ${ins.window}", kitAppWithin(cell, ins.window))
            assertTrue("$leg: $piece's cell $cell lies inside the safe area ${ins.safe} ($ins)", kitAppWithin(cell, ins.safe))
            assertTrue("$leg: $piece's cell $cell lies below the board $board (across the bottom)", cell.top >= board.bottom - 0.5f)
        }
        kit.assertInsetsStill(ins, "$leg, after the cell reads")
    }

    /** The REQ-036 A2 checks after a turn (see that test's comment for the sources). */
    private fun assertKept(p: PuzzleDefinition, kept: List<Placement>, what: String) {
        kit.awaitScreen(what)
        assertEquals("$what: the same puzzle", p.id, kit.shownId())
        assertEquals("$what: still In progress", PuzzleState.IN_PROGRESS, kit.shownState())
        val onBoard = PieceId.entries.filter { kit.exists(PlayTags.boardPiece(it)) }
        assertEquals("$what: the pieces on the board", kept.map { it.piece }.sorted(), onBoard.sorted())
        val mapping = kit.mapping()
        for (pl in kept) {
            val tag = PlayTags.boardPiece(pl.piece)
            val shown = Placement(kit.key(tag, PlaySemantics.PieceKey), kit.key(tag, PlaySemantics.TurnKey), kit.key(tag, PlaySemantics.MirroredKey), kit.key(tag, PlaySemantics.CentreKey))
            assertTrue("$what: ${pl.piece} kept at $pl, shown at $shown", kitAppSame(shown, pl))
            val corners = kitAppEngine.vertices(pl.piece, shown.turn, shown.mirrored, shown.centre).map { mapping.toPx(it) }
            val expected = Rect(corners.minOf { it.x }, corners.minOf { it.y }, corners.maxOf { it.x }, corners.maxOf { it.y })
            val box = kit.bounds(tag)
            val ok = abs(box.left - expected.left) <= 2f && abs(box.top - expected.top) <= 2f && abs(box.right - expected.right) <= 2f && abs(box.bottom - expected.bottom) <= 2f
            assertTrue("$what: `$tag` box $box is the box of its corners through the new mapping $expected (±2 px)", ok)
        }
        val board = kit.onController { placed.toList() }
        assertTrue("$what: the controller's board $board is the kept three $kept", kitAppSameBoard(board, kept))
        val inTray = p.pieces.filter { kit.exists(PlayTags.trayPiece(it)) }.toSet()
        assertEquals("$what: the other pieces in the tray", p.pieces - kept.map { it.piece }.toSet(), inTray)
    }

    /**
     * Under the paused clock: advances one frame at a time (at most five) until the new composition shows `play-screen`,
     * then runs [check] on that first frame. The activity re-creation runs on Android's own frames; only the composition's
     * animations wait for the paused clock (`KitScreen.recreate`'s reading in the play kit).
     */
    private fun firstFramesOfTheNewComposition(what: String, check: (String) -> Unit) {
        var frames = 0
        while (kit.nodes(PlayTags.SCREEN).size != 1) {
            if (frames >= 5) throw AssertionError("kit: $what: no `play-screen` after $frames paused frames")
            kit.frames(1)
            frames++
        }
        check("$what (frame $frames of the new composition)")
    }
}
