// Acceptance kit (WO-003, TASK-011): the gestures — drag and drop, tap or drag, tap-to-turn, the
// two-finger twist and the go-home return (tasks.md § WO-003 › Seams, the tag table and the touch routing).
// Every line that is about a gesture is driven by touch on `play-screen`; a board is set up through S-2
// only where the line is not about the gesture. Each tokened test asserts the meaning of its acceptance
// line; pins of a rule, a seam row or a decision carry no token.
// REQ-014 A1 is tested as the one-frame bound of capture-outbox/chg-draft-req014-a1-without-delay.md
// (decisions.md "REQ-002 A1 and REQ-014 A1 carry implemented (unverified)").
// Amended WO-004 (TASK-019), no token moved: the REQ-020 A2 snapshot is taken In progress, and the margin fixture of
// REQ-015 A2 keeps off the Restart pill (each amendment cites its row where it is made).
package app.tangramplaytime.play

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

@RunWith(AndroidJUnit4::class)
class DragKitTest {

    // The ComponentActivity host that `createComposeRule()` also uses (it is this very call), declared so a pin
    // can recreate the activity (code review F2).
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val kit by lazy { KitScreen(rule) }

    // ── REQ-012: every piece has one home ──────────────────────────────────────────────────────────────

    @Test // REQ-012.A2
    fun everyPieceIsAlwaysEitherInTheTrayOnTheBoardOrBeingDragged() {
        // REQ-012 A2: "Every piece is always either in the tray, on the board, or being dragged." Oracle: at
        // every moment of a lock, a missed drop and a drag taken from the board — at rest, on pick-up, during
        // the grow and the moves, at the release and through the 180 ms glides — each of the puzzle's pieces
        // has exactly one of `tray-piece`, `board-piece`, `drag-piece` (tag table).
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        fun check(moment: String) {
            for (piece in p.pieces) {
                val homes = kit.homesOf(piece)
                assertEquals("$moment: $piece has exactly one home, found $homes", 1, homes.size)
            }
        }
        check("at rest")
        // A drag that locks.
        val (piece, pose) = lockablePose(p)
        kit.tapTray(piece, turnsNeeded(piece, pose.turn))
        check("after the taps")
        val start = kit.trayTapPoint(piece)
        kit.touch { down(start) }
        kit.frames()
        check("finger down")
        var finger = Offset(start.x, start.y - kit.dp(20f))
        kit.touch { moveTo(finger) }
        kit.frames()
        check("the drag started")
        kit.advance(48)
        check("mid-grow")
        kit.advance(80)
        kit.frames()
        finger = kit.steerDragTo(piece, offsetFrom(pose.centre), finger)
        kit.checkOverBoard(finger)
        check("over the lock position")
        kit.release()
        check("released")
        kit.advance(60)
        check("mid-glide")
        kit.advance(200)
        check("after the glide")
        assertTrue("the drop locked", kit.exists(PlayTags.boardPiece(piece)))
        // A drag from the board to the middle, where nothing locks: it goes home.
        val placedPose = kit.key(PlayTags.boardPiece(piece), PlaySemantics.TurnKey)
        val middle = kitMiddleWithNoLock(p, piece, placedPose, kit.key(PlayTags.boardPiece(piece), PlaySemantics.MirroredKey), kit.onMain { kit.controller.boardDpPerUnit })
        finger = kit.pickUpFromBoard(piece)
        check("picked up from the board")
        finger = kit.steerDragTo(piece, middle, finger)
        check("over the middle")
        kit.release()
        check("released in the middle")
        kit.advance(60)
        check("mid go-home glide")
        kit.advance(200)
        check("home")
        assertTrue("the piece is back in the tray", kit.exists(PlayTags.trayPiece(piece)))
    }

    // ── REQ-013: cells never change on a tray turn ─────────────────────────────────────────────────────

    @Test // REQ-013.A2
    fun turningAPieceInTheTrayNeverChangesAnyCellSize() {
        // REQ-013 A2: "Turning a piece in the tray never changes any cell size." Rule: each cell is sized "so
        // that its piece fits in any turn". Oracle: after every tap on every tray piece (REQ-016: a tap turns
        // it one step), every cell's bounds and the tray strip's bounds are what they were.
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val cellsBefore = PieceId.entries.associateWith { kit.bounds(PlayTags.trayCell(it)) }
        val trayBefore = kit.bounds(PlayTags.TRAY)
        for (piece in PieceId.entries) {
            val turns = if (piece == PieceId.LT1) 8 else 2
            repeat(turns) { k ->
                val before = kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey)
                kit.tap(kit.trayTapPoint(piece))
                kit.advance(48)
                assertEquals("$piece tap ${k + 1} turned it (REQ-016)", before.next(), kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey))
                for (other in PieceId.entries) {
                    assertEquals("$other cell after tap ${k + 1} on $piece", cellsBefore.getValue(other), kit.bounds(PlayTags.trayCell(other)))
                }
                assertEquals("tray strip after tap ${k + 1} on $piece", trayBefore, kit.bounds(PlayTags.TRAY))
            }
        }
    }

    // ── REQ-014: drag and drop ─────────────────────────────────────────────────────────────────────────

    @Test // REQ-014.A1
    fun aDraggedPieceIsDrawnAtTheFingersPositionOfTheCurrentOrThePreviousFrame() {
        // REQ-014 A1: "A dragged piece follows the finger without delay" — tested as the one-frame bound of
        // chg-draft-req014-a1-without-delay.md: "While dragged, the piece is drawn at the finger's position
        // of the current or the previous displayed frame, offset as the Assumptions state." Oracle: after
        // each finger move and one frame, the piece's drawn centre (CentreKey, read against its box) has
        // moved by the finger's movement of this frame or of the previous one, within 2 px.
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val piece = PieceId.LT1
        val f0 = kit.pickUpFromTray(piece)
        val p0 = dragCentrePx(piece)
        val boardCentre = kit.bounds(PlayTags.BOARD).center
        val path = ArrayList<Offset>()
        for (i in 1..10) path += Offset(f0.x + (boardCentre.x - f0.x) * i / 10, f0.y + (boardCentre.y - f0.y) * i / 10)
        for (i in 1..6) path += Offset(boardCentre.x + kit.dp(12f) * i, boardCentre.y + kit.dp(5f) * i)
        for (i in 1..6) path += Offset(boardCentre.x + kit.dp(72f) - kit.dp(15f) * i, boardCentre.y + kit.dp(30f) - kit.dp(9f) * i)
        var previous = f0
        for ((i, f) in path.withIndex()) {
            kit.touch { moveTo(f) }
            kit.frames()
            val moved = dragCentrePx(piece) - p0
            val now = f - f0
            val before = previous - f0
            val okNow = hypot(moved.x - now.x, moved.y - now.y) <= 2f
            val okBefore = hypot(moved.x - before.x, moved.y - before.y) <= 2f
            assertTrue("move ${i + 1}: the piece moved by $moved, the finger by $now this frame and $before the frame before", okNow || okBefore)
            previous = f
        }
        kit.frames()
        val settled = dragCentrePx(piece) - p0
        val last = path.last() - f0
        assertTrue("one frame later the piece is at the finger: moved $settled, finger $last", hypot(settled.x - last.x, settled.y - last.y) <= 2f)
        kit.release()
    }

    @Test // pin: REQ-014 rule "the piece floats above the touch point"; touch routing 3 "the piece's lowest point 30 dp above the finger" (S-5 floatOffsetDp); no token
    fun theDraggedPieceFloatsThirtyDpAboveTheFinger() {
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val piece = PieceId.MT
        var finger = kit.pickUpFromTray(piece)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 4)
        val box = kit.bounds(PlayTags.dragPiece(piece))
        kitAssertNear("lowest point of $piece above the finger (px)", (finger.y - kit.dp(kitPhoneLayout.floatOffsetDp)).toDouble(), box.bottom.toDouble(), 2.0)
        kitAssertBoxNear("drag-piece box vs its corners", kitBox(kit.boardPolygonPx(PlayTags.dragPiece(piece))), box, 2f)
        kit.release()
    }

    @Test // REQ-014.A2
    fun thePieceIsNeverDrawnLargerOrSmallerThanItsBoardSizeOnceOnTheBoard() {
        // REQ-014 A2: "The piece is never drawn larger or smaller than its board size once on the board."
        // Rules: "A tray piece grows from miniature to board size within 120 ms of the drag starting"; "The
        // piece's size never changes on the board". Oracle: the drawn scale (DrawnPxPerUnitKey, read against
        // the node's box through the engine's corners) equals the board's px per unit from the end of the
        // grow, through every move, at the release, through the lock glide, on the board, and from the very
        // first frame of a drag taken from the board.
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val board = kit.pxPerUnit()
        val (piece, pose) = lockablePose(p)
        kit.tapTray(piece, turnsNeeded(piece, pose.turn))
        val start = kit.trayTapPoint(piece)
        kit.touch { down(start) }
        kit.frames()
        var finger = Offset(start.x, start.y - kit.dp(20f))
        kit.touch { moveTo(finger) }
        kit.frames()
        kit.advance(120)
        kit.frames()
        assertBoardSize("at 120 ms of the drag", PlayTags.dragPiece(piece), board)
        val target = offsetFrom(pose.centre)
        repeat(3) { step ->
            finger = kit.moveFinger(finger, Offset(finger.x + (kit.toPx(target).x - dragCentrePx(piece).x) / 3, finger.y + (kit.toPx(target).y - dragCentrePx(piece).y) / 3), 2)
            assertBoardSize("during move ${step + 1}", PlayTags.dragPiece(piece), board)
        }
        finger = kit.steerDragTo(piece, target, finger)
        kit.checkOverBoard(finger)
        assertBoardSize("over the lock position", PlayTags.dragPiece(piece), board)
        kit.release()
        assertTrue("the drop locked", kit.exists(PlayTags.boardPiece(piece)))
        assertBoardSize("at the release", PlayTags.boardPiece(piece), board)
        kit.advance(48)
        assertBoardSize("mid lock-glide", PlayTags.boardPiece(piece), board)
        kit.advance(200)
        assertBoardSize("locked", PlayTags.boardPiece(piece), board)
        // Taken from the board: no grow, board size from the first frame.
        val centre = kit.toPx(kit.key(PlayTags.boardPiece(piece), PlaySemantics.CentreKey))
        kit.touch { down(centre) }
        kit.frames()
        finger = Offset(centre.x, centre.y - kit.dp(20f))
        kit.touch { moveTo(finger) }
        kit.frames()
        assertTrue("a 20 dp move from a board piece drags it", kit.exists(PlayTags.dragPiece(piece)))
        assertBoardSize("first frame of a drag from the board", PlayTags.dragPiece(piece), board)
        finger = kit.moveFinger(finger, Offset(finger.x + kit.dp(30f), finger.y - kit.dp(30f)), 3)
        assertBoardSize("moving a piece taken from the board", PlayTags.dragPiece(piece), board)
        kit.release()
        kit.advance(300)
    }

    // ── REQ-015: tap or drag, touch areas ──────────────────────────────────────────────────────────────

    @Test // REQ-015.A1
    fun aThreeSecondStillPressOnAPieceActsAsATap() {
        // REQ-015 A1: "A 3 s still press on a piece acts as a tap." Statement: a touch that ends "having moved
        // less than 12 dp" is a tap "regardless of its duration". Oracle: a 3 s still press on a tray piece
        // turns it one step (REQ-016, what a tap does) and never starts a drag; a 3 s still press on a board
        // piece never drags it and leaves it on the board.
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val tray = PieceId.LT2
        val before = kit.key(PlayTags.trayPiece(tray), PlaySemantics.TurnKey)
        val at = kit.trayTapPoint(tray)
        kit.touch { down(at) }
        kit.advance(1500)
        assertTrue("1.5 s into the press $tray is still in the tray", kit.exists(PlayTags.trayPiece(tray)))
        assertFalse("1.5 s into the press no drag has started", kit.exists(PlayTags.dragPiece(tray)))
        kit.advance(1500)
        kit.touch { advanceEventTime(3000); up() }
        kit.frames()
        assertEquals("the 3 s press turned $tray one step", before.next(), kit.key(PlayTags.trayPiece(tray), PlaySemantics.TurnKey))
        assertEquals("$tray has one home", listOf(PlayTags.trayPiece(tray)), kit.homesOf(tray))

        val (piece, _) = lockablePose(p)
        assertTrue("set-up: $piece placed through S-2", kit.placeByApi(piece))
        kit.frames()
        kit.advance(200)
        val centre = kit.toPx(kit.key(PlayTags.boardPiece(piece), PlaySemantics.CentreKey))
        kit.touch { down(centre) }
        kit.advance(2900)
        assertFalse("2.9 s into the press $piece is not dragged", kit.exists(PlayTags.dragPiece(piece)))
        assertTrue("2.9 s into the press $piece is on the board", kit.exists(PlayTags.boardPiece(piece)))
        kit.advance(100)
        kit.touch { advanceEventTime(3000); up() }
        kit.frames()
        kit.advance(500)
        assertEquals("$piece stays on the board after the press", listOf(PlayTags.boardPiece(piece)), kit.homesOf(piece))
    }

    @Test // REQ-015.A2
    fun aTouchTenDpOutsideASmallTrianglesEdgePicksItUp() {
        // REQ-015 A2: "A touch 10 dp outside a small triangle's edge picks it up." Rule: "The touch area of a
        // board piece is its shape grown by 12 dp." Oracle: with ST1 alone on the board (set up through S-2),
        // a touch 10 dp outside the middle of one of its edges, moved past the tap threshold, drags ST1.
        kit.show()
        val placed = placeSmallTriangleAlone()
        val outside = pointOutside(placed, kit.dp(10f))
        kit.touch { down(outside) }
        kit.frames()
        kit.touch { moveTo(Offset(outside.x, outside.y - kit.dp(40f))) }
        kit.frames()
        assertTrue("ST1 is dragged from a touch 10 dp outside its edge at $outside", kit.exists(PlayTags.dragPiece(PieceId.ST1)))
        assertFalse("ST1 left the board", kit.exists(PlayTags.boardPiece(PieceId.ST1)))
        kit.release()
        kit.advance(300)
    }

    @Test // pin: REQ-015 rule "its shape grown by 12 dp" — 14 dp outside is nothing; no token
    fun aTouchFourteenDpOutsideASmallTriangleDoesNotPickItUp() {
        kit.show()
        val placed = placeSmallTriangleAlone()
        val outside = pointOutside(placed, kit.dp(14f))
        kit.touch { down(outside) }
        kit.frames()
        kit.touch { moveTo(Offset(outside.x, outside.y - kit.dp(40f))) }
        kit.frames()
        assertFalse("no drag from 14 dp outside ST1 at $outside", kit.exists(PlayTags.dragPiece(PieceId.ST1)))
        assertTrue("ST1 stays on the board", kit.exists(PlayTags.boardPiece(PieceId.ST1)))
        kit.release()
    }

    // ── REQ-016: tap to turn ───────────────────────────────────────────────────────────────────────────

    @Test // REQ-016.A1
    fun eightTapsReturnATrayPieceToItsStartingTurn() {
        // REQ-016 A1: "Eight taps return a tray piece to its starting turn." Statement: a tap on a tray piece
        // turns it "45° clockwise". Oracle, on four tray pieces: each tap sets TurnKey to the next step and
        // draws the piece in that pose (the pixels inside the clockwise pose but outside the counter-clockwise
        // one are the piece's colour; the converse pixels are not), and after eight taps the turn and the
        // miniature's box are the starting ones.
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        for (piece in listOf(PieceId.LT1, PieceId.MT, PieceId.SQ, PieceId.PG)) {
            val tag = PlayTags.trayPiece(piece)
            val startTurn = kit.key(tag, PlaySemantics.TurnKey)
            val startBox = kit.bounds(tag)
            var turn = startTurn
            for (k in 1..8) {
                kit.tap(kit.trayTapPoint(piece))
                kit.advance(48)
                turn = turn.next()
                assertEquals("$piece after tap $k", turn, kit.key(tag, PlaySemantics.TurnKey))
                assertDrawnClockwise(piece, turn)
            }
            assertEquals("$piece after eight taps", startTurn, kit.key(tag, PlaySemantics.TurnKey))
            kitAssertBoxNear("$piece miniature box after eight taps", startBox, kit.bounds(tag), 1f)
        }
    }

    @Test // REQ-016.A2
    fun aBoardPieceThatHasNoRoomToTurnKeepsItsPositionAndTurn() {
        // REQ-016 A2: "A board piece that has no room to turn keeps its position and turn." Rule: "IF a turned
        // board piece cannot lock near its place, THEN the game SHALL restore its previous turn and shake it
        // once." Oracle: a placed piece for which the engine's `tryTurnInPlace` says None (the fixture reads
        // the geometry) is tapped by touch; after the shake its CentreKey, TurnKey and MirroredKey and the
        // controller's placement are unchanged.
        kit.show()
        val found = findTurnCase(wantLocked = false) ?: throw AssertionError("kit: no solution pose whose next turn is refused by the engine")
        kit.open(found.first.id)
        assertTrue("set-up: ${found.second} placed through S-2", kit.placeByApi(found.second))
        kit.frames()
        kit.advance(200)
        val tag = PlayTags.boardPiece(found.second)
        val turn = kit.key(tag, PlaySemantics.TurnKey)
        val mirrored = kit.key(tag, PlaySemantics.MirroredKey)
        val centre = kit.key(tag, PlaySemantics.CentreKey)
        val placedBefore = kit.placed()
        kit.tap(kit.toPx(centre))
        kit.advance(600)
        assertEquals("turn kept", turn, kit.key(tag, PlaySemantics.TurnKey))
        assertEquals("mirror kept", mirrored, kit.key(tag, PlaySemantics.MirroredKey))
        val after = kit.key(tag, PlaySemantics.CentreKey)
        kitAssertNear("centre x kept", centre.x, after.x, KIT_TOL)
        kitAssertNear("centre y kept", centre.y, after.y, KIT_TOL)
        assertEquals("the controller's placements are unchanged", placedBefore, kit.placed())
        assertEquals("the piece has one home", listOf(tag), kit.homesOf(found.second))
    }

    @Test // pin: REQ-016 statement "keep the turn only if the turned piece locks" — the positive case through the screen; no token
    fun aBoardPieceWithRoomToTurnTurnsOneStepClockwise() {
        kit.show()
        val found = findTurnCase(wantLocked = true)
        assumeTrue("no solution pose whose next turn locks: the pin does not apply to this library", found != null)
        kit.open(found!!.first.id)
        assertTrue("set-up: ${found.second} placed through S-2", kit.placeByApi(found.second))
        kit.frames()
        kit.advance(200)
        val tag = PlayTags.boardPiece(found.second)
        val turn = kit.key(tag, PlaySemantics.TurnKey)
        val expected = found.third
        kit.tap(kit.toPx(kit.key(tag, PlaySemantics.CentreKey)))
        kit.advance(300)
        assertEquals("turned one step", turn.next(), kit.key(tag, PlaySemantics.TurnKey))
        kitAssertNear("centre x is the engine's", expected.centre.x, kit.key(tag, PlaySemantics.CentreKey).x, KIT_TOL)
        kitAssertNear("centre y is the engine's", expected.centre.y, kit.key(tag, PlaySemantics.CentreKey).y, KIT_TOL)
    }

    @Test // pin: REQ-016 rule "restore its previous turn and shake it once" — the shake is not played again when the activity is recreated (code review F2); no token
    fun aRefusedTurnsShakeIsNotPlayedAgainWhenTheActivityIsRecreated() {
        // The rule: a refused board turn shakes the piece once. The tag table: a board piece's bounds and CentreKey
        // follow the drawing in each frame. Oracle: a placed piece whose next turn the engine refuses (as in the
        // REQ-016 A2 test) is tapped by touch; the shake moves the drawn piece (so the fixture can tell a shake from
        // a still piece) and ends at the placement; then the activity is recreated with the same controller — what a
        // rotation or any configuration change does — and over the new composition's first 48 frames (about 770 ms,
        // twice the shake) the piece's CentreKey and box stay at the placement.
        kit.show()
        val found = findTurnCase(wantLocked = false) ?: throw AssertionError("kit: no solution pose whose next turn is refused by the engine")
        kit.open(found.first.id)
        assertTrue("set-up: ${found.second} placed through S-2", kit.placeByApi(found.second))
        kit.frames()
        kit.advance(200)
        val tag = PlayTags.boardPiece(found.second)
        val rest = kit.key(tag, PlaySemantics.CentreKey)
        val restBox = kit.bounds(tag)
        val placedBefore = kit.placed()
        kit.tap(kit.toPx(rest))
        var shook = 0f
        repeat(40) {
            shook = max(shook, displacementPx(tag, rest, restBox))
            kit.frames()
        }
        assertTrue(
            "REQ-016 rule \"shake it once\": the refused turn moved the drawn ${found.second} by at most $shook px over 40 frames " +
                "(the fixture needs the shake to show in CentreKey or the box)",
            shook >= kit.dp(2f),
        )
        kit.advance(500)
        assertTrue("the shake ended at the placement (${displacementPx(tag, rest, restBox)} px off)", displacementPx(tag, rest, restBox) <= 1f)
        assertEquals("set-up: the refused turn changed no placement", placedBefore, kit.placed())
        val boardBefore = kit.bounds(PlayTags.BOARD)
        kit.recreate(rule.activityRule.scenario)
        assertEquals("the recreated screen shows the same puzzle", found.first.id, kit.key(PlayTags.SCREEN, PlaySemantics.PuzzleIdKey))
        assertEquals("the recreated screen has the same board", boardBefore, kit.bounds(PlayTags.BOARD))
        assertEquals("the piece is still placed after the recreation", listOf(tag), kit.homesOf(found.second))
        for (f in 0..47) {
            val d = displacementPx(tag, rest, restBox)
            assertTrue(
                "F2 shake — REQ-016 rule \"shake it once\": the refused turn's shake is played again after the activity was " +
                    "recreated: ${found.second} is drawn $d px from its placement in frame $f of the new composition " +
                    "(${f * 16} ms; CentreKey ${kit.key(tag, PlaySemantics.CentreKey)} vs $rest, box ${kit.bounds(tag)} vs $restBox)",
                d <= 1f,
            )
            kit.frames()
        }
        assertEquals("the placements are unchanged after the recreation", placedBefore, kit.placed())
    }

    @Test // pin: REQ-016 A2 "A board piece that has no room to turn keeps its position and turn" and rule "shake it once", for two refused pieces whose shakes overlap (fix batch, reviewer note 2); no token
    fun twoRefusedTurnsWhoseShakesOverlapLeaveBothPiecesDrawnAtTheirPlacements() {
        // Oracle: two board pieces of one bundled puzzle, placed at their solution poses through S-2, each of whose
        // next turn the engine refuses with the other placed (the fixture reads the geometry; neither is the PG, so
        // no badge is near). Piece A is tapped by touch; while its shake runs (the drawing has moved, the clock less
        // than 200 ms on — the shake is 360 ms), piece B is tapped. Once both shakes are over, both pieces are drawn
        // at their placements — CentreKey and the node box within 1 px — over 48 frames (about 770 ms), and the
        // placements are unchanged.
        kit.show()
        val case = findTwoRefusedTurns()
            ?: throw AssertionError("kit: no bundled puzzle has two solution pieces (not the PG) that lock together and are each refused a turn")
        kit.open(case.puzzle.id)
        assertTrue("set-up: ${case.a} placed through S-2", kit.placeByApi(case.a))
        assertTrue("set-up: ${case.b} placed through S-2", kit.placeByApi(case.b))
        kit.frames()
        kit.advance(250)
        val tagA = PlayTags.boardPiece(case.a)
        val tagB = PlayTags.boardPiece(case.b)
        val restA = kit.key(tagA, PlaySemantics.CentreKey)
        val restB = kit.key(tagB, PlaySemantics.CentreKey)
        val boxA = kit.bounds(tagA)
        val boxB = kit.bounds(tagB)
        val placedBefore = kit.placed()
        val t0 = rule.mainClock.currentTime
        kit.tap(kit.toPx(restA))
        var aMoved = 0f
        repeat(5) {
            kit.frames()
            aMoved = max(aMoved, displacementPx(tagA, restA, boxA))
        }
        val gap = rule.mainClock.currentTime - t0
        check(gap < 200) { "kit: the second tap would come $gap ms after the first" }
        assertTrue("set-up: ${case.a}'s shake is running before the second tap (drawn up to $aMoved px off)", aMoved >= kit.dp(1f))
        kit.tap(kit.toPx(restB))
        var bMoved = 0f
        repeat(30) {
            kit.frames()
            bMoved = max(bMoved, displacementPx(tagB, restB, boxB))
        }
        assertTrue("set-up: ${case.b} shook after its refused turn (drawn up to $bMoved px off)", bMoved >= kit.dp(2f))
        kit.advance(500)
        for (f in 0..47) {
            val dA = displacementPx(tagA, restA, boxA)
            val dB = displacementPx(tagB, restB, boxB)
            assertTrue(
                "two shakes — REQ-016 A2 \"keeps its position and turn\" (rule: \"shake it once\"): ${case.puzzle.id}, ${case.b}'s refused turn " +
                    "came $gap ms into ${case.a}'s shake; ${rule.mainClock.currentTime - t0} ms after the first tap (frame $f of 48) ${case.a} is drawn ${"%.1f".format(dA)} px " +
                    "off its placement (CentreKey ${kit.key(tagA, PlaySemantics.CentreKey)} vs $restA, box ${kit.bounds(tagA)} vs $boxA) and " +
                    "${case.b} ${"%.1f".format(dB)} px (CentreKey ${kit.key(tagB, PlaySemantics.CentreKey)} vs $restB)",
                dA <= 1f && dB <= 1f,
            )
            kit.frames()
        }
        assertEquals("the placements are unchanged by the two refused turns", placedBefore, kit.placed())
    }

    // ── REQ-017: the two-finger twist ──────────────────────────────────────────────────────────────────

    @Test // REQ-017.A1
    fun aNinetyDegreeTwistTurnsThePieceExactlyTwoSteps() {
        // REQ-017 A1: "A 90° twist turns the piece exactly two steps." Statement: the piece turns "one 45° step
        // each time the twist angle passes a further 45° step boundary". Oracle: with LT1 dragged over the
        // board, a second finger that goes round the first by 90° clockwise in 5° moves leaves TurnKey two
        // steps on (never more than two on the way), drawn in that pose; 90° counter-clockwise brings it back.
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val piece = PieceId.LT1
        val tag = PlayTags.dragPiece(piece)
        var finger = kit.pickUpFromTray(piece)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 5)
        val start = kit.key(tag, PlaySemantics.TurnKey)
        twist(finger, 0.0, 90.0)
        assertEquals("after a 90° clockwise twist", start.next().next(), kit.key(tag, PlaySemantics.TurnKey))
        kitAssertBoxNear("drag-piece box vs its corners at the new turn", kitBox(kit.boardPolygonPx(tag)), kit.bounds(tag), 2f)
        twist(finger, 0.0, -90.0)
        assertEquals("after a 90° counter-clockwise twist", start, kit.key(tag, PlaySemantics.TurnKey))
        kit.release()
        kit.advance(300)
    }

    @Test // REQ-017.A2
    fun aTwentyDegreeTwistDoesNotTurnThePiece() {
        // REQ-017 A2: "A 20° twist does not turn the piece." Rule: "Step boundaries are at ±22.5°, ±67.5° and
        // so on". Oracle: 20° clockwise, back, and 20° counter-clockwise leave TurnKey as it was.
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val piece = PieceId.LT2
        val tag = PlayTags.dragPiece(piece)
        var finger = kit.pickUpFromTray(piece)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 5)
        val start = kit.key(tag, PlaySemantics.TurnKey)
        twist(finger, 0.0, 20.0)
        assertEquals("after a 20° clockwise twist", start, kit.key(tag, PlaySemantics.TurnKey))
        twist(finger, 0.0, -20.0)
        assertEquals("after a 20° counter-clockwise twist", start, kit.key(tag, PlaySemantics.TurnKey))
        kit.release()
        kit.advance(300)
    }

    @Test // pin: REQ-017 rule "Lifting the second finger keeps the turn reached; the drag continues"; rule: 30° passes the 22.5° boundary; no token
    fun liftingTheSecondFingerKeepsTheTurnAndTheDragContinues() {
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val piece = PieceId.MT
        val tag = PlayTags.dragPiece(piece)
        var finger = kit.pickUpFromTray(piece)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 5)
        val start = kit.key(tag, PlaySemantics.TurnKey)
        twist(finger, 0.0, 30.0)
        assertEquals("30° passes the 22.5° boundary: one step", start.next(), kit.key(tag, PlaySemantics.TurnKey))
        assertTrue("still dragged after the lift", kit.exists(tag))
        val before = dragCentrePx(piece)
        finger = kit.moveFinger(finger, Offset(finger.x + kit.dp(40f), finger.y), 3)
        val moved = dragCentrePx(piece) - before
        kitAssertNear("the drag continues: moved x", kit.dp(40f).toDouble(), moved.x.toDouble(), 2.0)
        assertEquals("the turn is kept while the drag continues", start.next(), kit.key(tag, PlaySemantics.TurnKey))
        kit.release()
        kit.advance(300)
    }

    @Test // pin: touch routing 1 / decisions.md "while a piece is dragged every further touch is a twist finger … acts on nothing else" (chg-draft-req017-second-finger-never-a-tap.md); no token
    fun aSecondFingerOnATrayCellDuringADragDoesNotTurnThatTrayPiece() {
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val dragged = PieceId.LT1
        val other = PieceId.ST2
        val otherTurn = kit.key(PlayTags.trayPiece(other), PlaySemantics.TurnKey)
        var finger = kit.pickUpFromTray(dragged)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 5)
        val cell = kit.trayTapPoint(other)
        kit.touch { down(1, cell) }
        kit.frames()
        kit.touch { up(1) }
        kit.frames()
        assertEquals("$other was not tapped", otherTurn, kit.key(PlayTags.trayPiece(other), PlaySemantics.TurnKey))
        assertEquals("$other stayed in the tray", listOf(PlayTags.trayPiece(other)), kit.homesOf(other))
        assertTrue("$dragged is still dragged", kit.exists(PlayTags.dragPiece(dragged)))
        kit.release()
        kit.advance(300)
    }

    // ── REQ-020: the missed drop ───────────────────────────────────────────────────────────────────────

    @Test // REQ-020.A1
    fun afterAnyDropThePieceIsEitherLockedOnTheBoardOrBackInItsTrayCell() {
        // REQ-020 A1: "After any drop, the piece is either locked on the board or back in its tray cell."
        // Statement: a drop with "no valid position by REQ-019, or … outside the board or on the tray" returns
        // the piece to its cell. Oracle, by touch: a drop near a valid position ends locked (the placement is a
        // lock by the engine's own search); a drop in the middle, a drop on the top bar and a drop on the tray
        // each end with the miniature back inside its own cell, after the 180 ms return.
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val dp = kit.onMain { kit.controller.boardDpPerUnit }
        // (a) locks
        val (piece, pose) = lockablePose(p)
        kit.tapTray(piece, turnsNeeded(piece, pose.turn))
        var finger = kit.pickUpFromTray(piece)
        finger = kit.steerDragTo(piece, offsetFrom(pose.centre), finger)
        kit.checkOverBoard(finger)
        kit.release()
        kit.advance(250)
        assertEquals("(a) $piece after the drop", listOf(PlayTags.boardPiece(piece)), kit.homesOf(piece))
        val placement = kit.placed().single { it.piece == piece }
        val others = kit.placed().filter { it.piece != piece }
        val lock = kitEngine.findLock(p, others, piece, placement.turn, placement.mirrored, placement.centre, dp)
        assertEquals("(a) the placement is a lock of the engine's search", LockResult.Locked(placement), lock)
        // (b) no valid position: the middle of the silhouette
        val second = p.pieces.first { it != piece }
        val cell = kit.bounds(PlayTags.trayCell(second))
        val trayTurn = kit.key(PlayTags.trayPiece(second), PlaySemantics.TurnKey)
        val middle = kitMiddleWithNoLock(p, second, trayTurn, false, dp)
        finger = kit.pickUpFromTray(second)
        finger = kit.steerDragTo(second, middle, finger)
        kit.release()
        kit.advance(250)
        assertHome("(b) a drop in the middle", second, cell)
        // (c) outside the board: on the top bar
        finger = kit.pickUpFromTray(second)
        finger = kit.moveFinger(finger, Offset(kit.bounds(PlayTags.SCREEN).center.x, kit.dp(kitPhoneLayout.topBarDp) / 2), 5)
        assertFalse("(c) the finger is outside the board", kit.bounds(PlayTags.BOARD).contains(finger))
        kit.release()
        kit.advance(250)
        assertHome("(c) a drop on the top bar", second, cell)
        // (d) on the tray: over another piece's cell
        val third = p.pieces.first { it != piece && it != second }
        finger = kit.pickUpFromTray(second)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.trayCell(third)).center, 5)
        kit.release()
        kit.advance(250)
        assertHome("(d) a drop on the tray", second, cell)
        assertEquals("(d) the other piece is untouched", listOf(PlayTags.trayPiece(third)), kit.homesOf(third))
    }

    @Test // REQ-020.A2
    fun noErrorMessageIsShownForAMissedDrop() {
        // REQ-020 A2: "No error message is shown for a missed drop." Rule: "there is no failure sound or
        // message." Oracle: the texts and content descriptions of every node, and the set of tagged nodes
        // apart from the dropped piece's own, are the same before the drag, at the release, during the
        // return and after it (brief: compared before and after the drop).
        // Amended WO-004 (TASK-019): the first tray drag of a New puzzle moves it to In progress (TYPE-006), which now adds
        // the `restart` pill and the `state-word` with their texts to the screen (S-22 "Restart"; S-13Δ; decisions.md
        // 2026-10-15 "The top bar, Restart and the solved row, as drawn": Restart shown only In progress, a state word for
        // In progress). Those belong to the drag start, not to the missed drop, so the puzzle is put In progress first
        // through S-2 — a drag of another piece started and sent home on the main thread, nothing placed — and the
        // snapshot is taken then. The comparison itself is unchanged.
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val piece = PieceId.SQ
        kit.onMain {
            check(kit.controller.startDrag(PieceId.LT1, Point(0.0, 0.0))) { "kit: startDrag(LT1) refused" }
            kit.controller.goHome()
        }
        kit.frames()
        kit.advance(300)
        check(kit.key(PlayTags.SCREEN, PlaySemantics.PuzzleStateKey) == PuzzleState.IN_PROGRESS) { "kit: ${p.id} is not In progress after the set-up drag" }
        check(kit.exists(KitTag.RESTART)) { "kit: no `restart` pill In progress, so the snapshot would not hold it" }
        val own = setOf(PlayTags.trayPiece(piece), PlayTags.trayGhost(piece), PlayTags.dragPiece(piece))
        val (tagsBefore, textsBefore) = kit.tagsAndTexts()
        val middle = kitMiddleWithNoLock(p, piece, kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey), false, kit.onMain { kit.controller.boardDpPerUnit })
        var finger = kit.pickUpFromTray(piece)
        finger = kit.steerDragTo(piece, middle, finger)
        kit.release()
        for (t in listOf(0L, 48L, 100L, 200L, 500L)) {
            kit.advance(t)
            val (tags, texts) = kit.tagsAndTexts()
            assertEquals("texts $t ms after the missed drop", textsBefore, texts)
            assertEquals("tagged nodes $t ms after the missed drop", tagsBefore.filter { it !in own }, tags.filter { it !in own })
        }
        assertEquals("tagged nodes once the piece is home", tagsBefore, kit.tagsAndTexts().first)
    }

    @Test // pin: touch routing 4 "a pointer cancel calls goHome()"; decisions.md "a drag ended without a release is a missed drop" (chg-draft-req020-interrupted-drag.md); no token
    fun aCancelledDragReturnsThePieceToItsCell() {
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val piece = PieceId.ST1
        val cell = kit.bounds(PlayTags.trayCell(piece))
        var finger = kit.pickUpFromTray(piece)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 4)
        kit.touch { cancel() }
        kit.frames()
        kit.advance(250)
        assertHome("after a pointer cancel", piece, cell)
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    private fun dragCentrePx(piece: PieceId): Offset = kit.toPx(kit.key(PlayTags.dragPiece(piece), PlaySemantics.CentreKey))

    private fun displacementPx(tag: String, rest: Point, restBox: Rect): Float = kit.displacementPx(tag, rest, restBox)

    private fun turnsNeeded(piece: PieceId, target: Turn): Int {
        val now = kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey)
        return ((target.step - now.step) % 8 + 8) % 8
    }

    /** The solution centre nudged by a quarter unit, so the drop snaps (TYPE-004: R ≥ 0.65 units). */
    private fun offsetFrom(centre: Point): Point = Point(centre.x + 0.2, centre.y + 0.15)

    /**
     * A piece of [p] whose solution pose locks alone on an empty board, from the nudged centre too (the
     * fixture reads the geometry through the engine); the topmost such piece, so the finger — 30 dp below the
     * piece — stays over the board.
     */
    private fun lockablePose(p: PuzzleDefinition): Pair<PieceId, KitPose> {
        val dp = kit.onMain { kit.controller.boardDpPerUnit }
        val candidates = p.solution.map { sp -> sp.piece to kitPoseOf(sp.piece, sp.polygon) to sp.polygon.maxOf { it.y } }
            .sortedBy { it.second }
            .map { it.first }
            .filter { (piece, pose) ->
                !pose.mirrored &&
                    kitEngine.findLock(p, emptyList(), piece, pose.turn, pose.mirrored, pose.centre, dp) is LockResult.Locked &&
                    kitEngine.findLock(p, emptyList(), piece, pose.turn, pose.mirrored, offsetFrom(pose.centre), dp) is LockResult.Locked
            }
        return candidates.firstOrNull() ?: throw AssertionError("kit: no piece of ${p.id} locks alone at its solution pose")
    }

    private fun assertHome(what: String, piece: PieceId, cell: Rect) {
        assertEquals("$what: $piece has one home", listOf(PlayTags.trayPiece(piece)), kit.homesOf(piece))
        val box = kit.bounds(PlayTags.trayPiece(piece))
        assertTrue("$what: $piece's miniature $box is inside its cell $cell", cell.contains(box.topLeft) && cell.contains(box.bottomRight))
        assertFalse("$what: no ghost in the cell", kit.exists(PlayTags.trayGhost(piece)))
    }

    private fun assertBoardSize(moment: String, tag: String, boardPxPerUnit: Float) {
        val scale = kit.key(tag, PlaySemantics.DrawnPxPerUnitKey)
        kitAssertNear("$moment: DrawnPxPerUnit of `$tag` vs the board's px per unit", boardPxPerUnit.toDouble(), scale.toDouble(), 0.01 * boardPxPerUnit)
        kitAssertBoxNear("$moment: `$tag` box vs its corners at the board scale", kitBox(kit.boardPolygonPx(tag)), kit.bounds(tag), 2f)
    }

    /** ST1 alone at its solution pose, in the first puzzle where that pose locks on an empty board. */
    private fun placeSmallTriangleAlone(): List<Offset> {
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            if (!kit.placeByApi(PieceId.ST1)) continue
            kit.frames()
            kit.advance(200)
            return kit.boardPolygonPx(PlayTags.boardPiece(PieceId.ST1))
        }
        throw AssertionError("kit: ST1 locks alone at its solution pose in no bundled puzzle")
    }

    /**
     * The first edge midpoint of [poly] moved [distPx] outward (away from the centre) that lies on the board.
     * Amended WO-004 (TASK-019): and outside the `restart` pill, which is drawn In progress at the board's top-left and
     * comes before a piece's margin in the touch order (S-22 hit order 3 "a piece's drawn shape → the Restart pill → … → a
     * cell or a margin"; decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn").
     */
    private fun pointOutside(poly: List<Offset>, distPx: Float): Offset {
        val board = kit.bounds(PlayTags.BOARD)
        val pill = if (kit.exists(KitTag.RESTART)) kit.bounds(KitTag.RESTART) else null
        val cx = poly.sumOf { it.x.toDouble() } / poly.size
        val cy = poly.sumOf { it.y.toDouble() } / poly.size
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
            var nx = -(b.y - a.y)
            var ny = b.x - a.x
            val len = hypot(nx, ny)
            nx /= len
            ny /= len
            if ((mid.x - cx) * nx + (mid.y - cy) * ny < 0) { nx = -nx; ny = -ny }
            val q = Offset(mid.x + nx * distPx, mid.y + ny * distPx)
            if (board.contains(q) && (pill == null || !pill.contains(q))) return q
        }
        throw AssertionError("kit: no edge of the placed ST1 has an outward point on the board")
    }

    /** A puzzle, a piece and the engine's answer: a solution pose alone on the board whose next turn is refused (or kept). */
    private fun findTurnCase(wantLocked: Boolean): Triple<PuzzleDefinition, PieceId, Placement>? {
        val dp = kit.onMain { kit.controller.boardDpPerUnit }
        for (p in kitLibrary.puzzles) for (sp in p.solution) {
            val pose = kitPoseOf(sp.piece, sp.polygon)
            if (kitEngine.findLock(p, emptyList(), sp.piece, pose.turn, pose.mirrored, pose.centre, dp) !is LockResult.Locked) continue
            val current = Placement(sp.piece, pose.turn, pose.mirrored, pose.centre)
            val r = kitEngine.tryTurnInPlace(p, emptyList(), current, pose.turn.next(), dp)
            if (!wantLocked && r is LockResult.None) return Triple(p, sp.piece, current)
            if (wantLocked && r is LockResult.Locked) return Triple(p, sp.piece, r.placement)
        }
        return null
    }

    private class TwoRefused(val puzzle: PuzzleDefinition, val a: PieceId, val b: PieceId)

    /**
     * Over the bundled puzzles (each opened, so the engine searches at its own board scale): two solution pieces,
     * neither the PG, that lock at their solution poses in the order A then B, whose next turns the engine refuses
     * with the other placed (`tryTurnInPlace` → None), and whose centres lie at least 20 dp outside the other's
     * shape (a shaking piece moves 6 dp at most, decisions.md TASK-013 row; its 12 dp margin never reaches the
     * other's tap point).
     */
    private fun findTwoRefusedTurns(): TwoRefused? {
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            val dp = kit.onMain { kit.controller.boardDpPerUnit }
            val poses = p.solution.filter { it.piece != PieceId.PG }.map { it.piece to kitPoseOf(it.piece, it.polygon) }
            for ((a, pa) in poses) for ((b, pb) in poses) {
                if (a == b) continue
                val la = kitEngine.findLock(p, emptyList(), a, pa.turn, pa.mirrored, pa.centre, dp) as? LockResult.Locked ?: continue
                val lb = kitEngine.findLock(p, listOf(la.placement), b, pb.turn, pb.mirrored, pb.centre, dp) as? LockResult.Locked ?: continue
                if (kitEngine.tryTurnInPlace(p, listOf(lb.placement), la.placement, la.placement.turn.next(), dp) !is LockResult.None) continue
                if (kitEngine.tryTurnInPlace(p, listOf(la.placement), lb.placement, lb.placement.turn.next(), dp) !is LockResult.None) continue
                val polyA = kitEngine.vertices(a, la.placement.turn, la.placement.mirrored, la.placement.centre).map { kit.toPx(it) }
                val polyB = kitEngine.vertices(b, lb.placement.turn, lb.placement.mirrored, lb.placement.centre).map { kit.toPx(it) }
                if (!clearOf(kit.toPx(la.placement.centre), polyB) || !clearOf(kit.toPx(lb.placement.centre), polyA)) continue
                return TwoRefused(p, a, b)
            }
        }
        return null
    }

    /** [o] lies outside [poly] by at least 20 dp. */
    private fun clearOf(o: Offset, poly: List<Offset>): Boolean {
        val pts = poly.map { Point(it.x.toDouble(), it.y.toDouble()) }
        val q = Point(o.x.toDouble(), o.y.toDouble())
        return !kitInside(q, pts) && kitDistToBoundary(q, pts) >= kit.dp(20f)
    }

    /** A second finger going round the first (at [first]) from [fromDeg] to [toDeg] in 5° moves, 80 dp out; then lifted. */
    private fun twist(first: Offset, fromDeg: Double, toDeg: Double) {
        val radius = kit.dp(80f)
        kit.touch { down(1, kitOnCircle(first, radius, fromDeg)) }
        kit.frames()
        val steps = (abs(toDeg - fromDeg) / 5.0).toInt().coerceAtLeast(1)
        for (i in 1..steps) {
            val deg = fromDeg + (toDeg - fromDeg) * i / steps
            kit.touch { moveTo(1, kitOnCircle(first, radius, deg)) }
            kit.frames()
        }
        kit.touch { up(1) }
        kit.frames()
    }

    /**
     * The miniature is drawn at [turn], not at the counter-clockwise alternative: pixels inside the drawn
     * pose and outside the pose two steps back are the piece's colour; the converse pixels are not. The
     * drawn pose fills the node's box; the alternative shares its centre (a turn keeps the centre, TYPE-003).
     * Only samples inside the tray capture are read.
     */
    private fun assertDrawnClockwise(piece: PieceId, turn: Turn) {
        val tag = PlayTags.trayPiece(piece)
        val mirrored = kit.key(tag, PlaySemantics.MirroredKey)
        val scale = kit.key(tag, PlaySemantics.DrawnPxPerUnitKey)
        val box = kit.bounds(tag)
        fun centred(t: Turn): List<Offset> {
            val v = kitEngine.vertices(piece, t, mirrored, Point(0.0, 0.0))
            val cx = (v.minOf { it.x } + v.maxOf { it.x }) / 2
            val cy = (v.minOf { it.y } + v.maxOf { it.y }) / 2
            return v.map { Offset(box.center.x + ((it.x - cx) * scale).toFloat(), box.center.y + ((it.y - cy) * scale).toFloat()) }
        }
        val drawn = centred(turn)
        val alternative = centred(turn.previous().previous())
        val trayBounds = kit.bounds(PlayTags.TRAY)
        val trayOrigin = trayBounds.topLeft
        val avoid = listOfNotNull(
            if (kit.exists(PlayTags.sizeMark(piece))) kit.bounds(PlayTags.sizeMark(piece)) else null,
            if (piece == PieceId.PG && kit.exists(PlayTags.FLIP_BADGE_TRAY)) kit.bounds(PlayTags.FLIP_BADGE_TRAY) else null,
        )
        val colour = PlayPalette.piece(piece)
        val tray = kit.capture(PlayTags.TRAY)
        val inDrawn = kitDifferenceSamples(drawn, alternative, 3f, avoid, within = trayBounds)
        val inAlternative = kitDifferenceSamples(alternative, drawn, 3f, avoid, within = trayBounds)
        android.util.Log.i("KitScreen", "assertDrawnClockwise $piece turn ${turn.step}: ${inDrawn.size} drawn-only, ${inAlternative.size} alternative-only samples in the tray capture")
        val samePose = kitSamePointSet(drawn.map { Point(it.x.toDouble(), it.y.toDouble()) }, alternative.map { Point(it.x.toDouble(), it.y.toDouble()) }, 0.5)
        if (samePose) return // the two poses coincide (the square): nothing to tell apart
        assertTrue("$piece at turn ${turn.step}: the fixture keeps samples that tell the poses apart (${inDrawn.size} / ${inAlternative.size})", inDrawn.size >= 4 && inAlternative.size >= 4)
        for (s in inDrawn) {
            val c = tray[(s.x - trayOrigin.x).toInt(), (s.y - trayOrigin.y).toInt()]
            assertTrue("$piece at turn ${turn.step}: pixel $s inside the drawn pose is ${kitHex(c)}, not ${kitHex(colour)}", kitNear(c, colour, 16f / 255f))
        }
        for (s in inAlternative) {
            val c = tray[(s.x - trayOrigin.x).toInt(), (s.y - trayOrigin.y).toInt()]
            assertFalse("$piece at turn ${turn.step}: pixel $s of the counter-clockwise pose is painted ${kitHex(c)}", kitNear(c, colour, 16f / 255f))
        }
    }
}
