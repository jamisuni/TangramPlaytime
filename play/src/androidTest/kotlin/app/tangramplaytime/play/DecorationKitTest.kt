// Acceptance kit (WO-003, TASK-011): the tray decorations — the flip badge and the size marks (tasks.md
// § WO-003 › Seams S-11, S-12, the tag table's `flip-badge-*`, `size-mark-*` and `tray-ghost-*` rows;
// decisions.md "Where the flip badge is"). The badge is pressed by touch; a placed piece is set up through
// S-2 where the line is not about the gesture. Pixels are read against the geometry of the node's keys.
// Amended WO-004 (TASK-019), no token moved: the board samplers that run In progress (the badge's ink, the disc finder)
// keep the Restart pill's pixels out (cited where made). The tray captures are taken while the tray is shown and are
// unchanged: the solved row replaces the tray only once a puzzle is Solved.
package app.tangramplaytime.play

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class DecorationKitTest {

    @get:Rule
    val rule = createComposeRule()

    private val kit by lazy { KitScreen(rule) }

    // ── REQ-018: the flip badge ────────────────────────────────────────────────────────────────────────

    @Test // REQ-018.A1
    fun theParallelogramCanBeMirroredInTheTrayAndOnTheBoard() {
        // REQ-018 A1: "The parallelogram can be mirrored in the tray and on the board." Statement: "WHEN the
        // player presses the badge, the game SHALL mirror the parallelogram." Oracle, by touch on the badge:
        // in the tray, MirroredKey toggles and the miniature is drawn in the mirrored pose (the pixels the two
        // poses do not share tell them apart); on the board, with the PG at a lock whose mirror also locks
        // (the engine decides, rule: "a mirror is kept only if the mirrored piece still locks in place"),
        // the placed piece carries the mirrored state and the engine's mirrored placement. The badge is pressed
        // where it is drawn: the centre of its disc, read from the drawing (`drawnDisc`).
        kit.show()
        val case = findBoardMirrorCase() ?: throw AssertionError("kit: no board position in any bundled puzzle where the mirrored parallelogram locks")
        kit.open(case.puzzle.id)
        // In the tray.
        val trayTag = PlayTags.trayPiece(PieceId.PG)
        assertFalse("PG starts unmirrored", kit.key(trayTag, PlaySemantics.MirroredKey))
        val unmirroredPose = kit.trayPolygonPx(PieceId.PG)
        val trayDisc = drawnDisc(PlayTags.TRAY, PlayTags.FLIP_BADGE_TRAY)
        kit.tap(trayDisc.centre)
        kit.advance(100)
        assertTrue("PG mirrored after the badge press", kit.key(trayTag, PlaySemantics.MirroredKey))
        assertEquals("PG still in the tray", listOf(trayTag), kit.homesOf(PieceId.PG))
        val mirroredPose = kit.trayPolygonPx(PieceId.PG)
        assertDrawnPose("tray PG mirrored", mirroredPose, unmirroredPose, PlayTags.TRAY, listOf(kit.bounds(PlayTags.FLIP_BADGE_TRAY)))
        kit.tap(trayDisc.centre)
        kit.advance(100)
        assertFalse("PG mirrored back by a second press", kit.key(trayTag, PlaySemantics.MirroredKey))
        assertDrawnPose("tray PG mirrored back", kit.trayPolygonPx(PieceId.PG), mirroredPose, PlayTags.TRAY, listOf(kit.bounds(PlayTags.FLIP_BADGE_TRAY)))
        // On the board.
        case.neighbour?.let { assertTrue("set-up: neighbour $it placed through S-2", kit.placeByApi(it)) }
        placePgByApi(case.before)
        kit.frames()
        kit.advance(250)
        val boardTag = PlayTags.boardPiece(PieceId.PG)
        assertEquals("set-up: PG locked where the fixture planned", case.before, kit.placed().single { it.piece == PieceId.PG })
        assertTrue("a board badge next to the placed PG", kit.exists(PlayTags.FLIP_BADGE_BOARD))
        val beforePolygon = kit.boardPolygonPx(boardTag)
        kit.tap(drawnDisc(PlayTags.BOARD, PlayTags.FLIP_BADGE_BOARD).centre)
        kit.advance(400)
        assertEquals("PG still placed", listOf(boardTag), kit.homesOf(PieceId.PG))
        assertTrue("PG mirrored on the board", kit.key(boardTag, PlaySemantics.MirroredKey))
        assertEquals("the placement is the engine's mirrored lock", case.after, kit.placed().single { it.piece == PieceId.PG })
        assertEquals("turn unchanged by the mirror", case.before.turn, kit.key(boardTag, PlaySemantics.TurnKey))
        assertDrawnPose("board PG mirrored", kit.boardPolygonPx(boardTag), beforePolygon, PlayTags.BOARD, listOf(kit.bounds(PlayTags.FLIP_BADGE_BOARD)))
    }

    @Test // REQ-018.A2
    fun theBadgeIsVisibleInEveryPuzzleWithTheParallelogramAndOnlyNextToIt() {
        // REQ-018 A2: "The badge is visible in every puzzle that contains the parallelogram, and only next to
        // the parallelogram." Oracle, over every bundled puzzle: with the PG in the puzzle, exactly one badge
        // node, displayed, drawn (pixels in its area that are neither the tray colour nor the PG colour),
        // nearer the PG cell than any other cell; with the PG placed, one badge next to the placed piece and
        // none in the tray; in a puzzle without the PG (the minis), no badge node at all.
        kit.show()
        var withPg = 0
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            if (PieceId.PG !in p.pieces) {
                assertFalse("${p.id}: no tray badge without the PG", kit.exists(PlayTags.FLIP_BADGE_TRAY))
                assertFalse("${p.id}: no board badge without the PG", kit.exists(PlayTags.FLIP_BADGE_BOARD))
                continue
            }
            withPg++
            assertEquals("${p.id}: one tray badge", 1, kit.count(PlayTags.FLIP_BADGE_TRAY))
            assertFalse("${p.id}: no board badge while the PG is in the tray", kit.exists(PlayTags.FLIP_BADGE_BOARD))
            rule.onNodeWithTag(PlayTags.FLIP_BADGE_TRAY, useUnmergedTree = true).assertIsDisplayed()
            val badge = kit.bounds(PlayTags.FLIP_BADGE_TRAY)
            val pgCell = kit.bounds(PlayTags.trayCell(PieceId.PG))
            val dPg = distance(badge.center, pgCell.center)
            for (other in p.pieces) {
                if (other == PieceId.PG) continue
                val d = distance(badge.center, kit.bounds(PlayTags.trayCell(other)).center)
                assertTrue("${p.id}: the badge (${badge.center}) is nearer the PG cell ($dPg px) than the $other cell ($d px)", dPg < d)
            }
            val glyphArea = badge.intersect(pgCell).takeIf { it.width > 0 && it.height > 0 } ?: badge
            assertDrawnSomething("${p.id}: tray badge", PlayTags.TRAY, glyphArea, listOf(PlayPalette.tray, PlayPalette.paper, PlayPalette.piece(PieceId.PG)))
        }
        assertTrue("the library has puzzles with the PG", withPg >= 1)
        // Placed: the badge is next to the board piece, and only there.
        var placedCase: PuzzleDefinition? = null
        for (p in kitLibrary.puzzles) {
            if (PieceId.PG !in p.pieces) continue
            kit.open(p.id)
            if (kit.placeByApi(PieceId.PG)) { placedCase = p; break }
        }
        placedCase ?: throw AssertionError("kit: the PG locks alone at its solution pose in no bundled puzzle")
        kit.frames()
        kit.advance(250)
        assertEquals("${placedCase.id}: one board badge with the PG placed", 1, kit.count(PlayTags.FLIP_BADGE_BOARD))
        assertFalse("${placedCase.id}: no tray badge with the PG placed", kit.exists(PlayTags.FLIP_BADGE_TRAY))
        rule.onNodeWithTag(PlayTags.FLIP_BADGE_BOARD, useUnmergedTree = true).assertIsDisplayed()
        val badge = kit.bounds(PlayTags.FLIP_BADGE_BOARD)
        val pg = kit.bounds(PlayTags.boardPiece(PieceId.PG))
        val gap = max(0f, max(pg.left - badge.center.x, badge.center.x - pg.right)).let { dx ->
            val dy = max(0f, max(pg.top - badge.center.y, badge.center.y - pg.bottom)); hypot(dx, dy)
        }
        assertTrue("${placedCase.id}: the board badge's centre is next to the placed PG (gap $gap px, badge ${badge.width} × ${badge.height})", gap <= max(badge.width, badge.height))
        // Amended WO-004 (TASK-019): with the PG placed the puzzle is In progress, and the Restart pill is drawn at the
        // board's top-left (S-22 "Restart"; decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn");
        // its pixels are not the badge's, so the pill's node is left out of the badge's area here.
        val pill = if (kit.exists(KitTag.RESTART)) listOf(kit.bounds(KitTag.RESTART)) else emptyList()
        assertDrawnSomething("${placedCase.id}: board badge", PlayTags.BOARD, badge, listOf(PlayPalette.board, PlayPalette.paper, PlayPalette.silhouette, PlayPalette.piece(PieceId.PG)), exclude = pill)
    }

    @Test // pin: decisions.md "no badge while the piece is being dragged (it is in neither place)"; REQ-018 rule "touch area is at least 60 dp across"; no token
    fun noBadgeWhileTheParallelogramIsDraggedAndTheBadgeAreaIsSixtyDp() {
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val badge = kit.bounds(PlayTags.FLIP_BADGE_TRAY)
        assertTrue("badge area ${badge.width} × ${badge.height} px is at least 60 dp across", badge.width >= kit.dp(60f) - 1f && badge.height >= kit.dp(60f) - 1f)
        var finger = kit.pickUpFromTray(PieceId.PG)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 4)
        assertFalse("no tray badge while dragged", kit.exists(PlayTags.FLIP_BADGE_TRAY))
        assertFalse("no board badge while dragged", kit.exists(PlayTags.FLIP_BADGE_BOARD))
        kit.release()
        kit.advance(300)
        assertTrue("the badge is back once the PG is home", kit.exists(PlayTags.FLIP_BADGE_TRAY) || kit.exists(PlayTags.FLIP_BADGE_BOARD))
    }

    @Test // pin: touch routing 3 "A touch that starts on a badge never drags"; decisions.md "badge touch precedence"; no token
    fun aTouchThatStartsOnTheTrayBadgeNeverDragsTheParallelogram() {
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val at = drawnDisc(PlayTags.TRAY, PlayTags.FLIP_BADGE_TRAY).centre // the badge as drawn (`drawnDisc`)
        kit.touch { down(at) }
        kit.frames()
        kit.touch { moveTo(Offset(at.x, at.y - kit.dp(60f))) }
        kit.frames()
        kit.advance(200)
        assertFalse("no drag from the badge", kit.exists(PlayTags.dragPiece(PieceId.PG)))
        kit.release()
        kit.advance(100)
        assertEquals("PG stays in the tray", listOf(PlayTags.trayPiece(PieceId.PG)), kit.homesOf(PieceId.PG))
    }

    // ── Where the badge's touch area ends (code review F1) ────────────────────────────────────────────────
    // decisions.md 2026-10-12 "Where the flip badge's touch area ends": in the tray the whole 60 dp area lies
    // inside the parallelogram's own cell, because REQ-015's rule gives every other cell wholly to its piece ("the
    // touch area of a tray piece is its whole tray cell"; the pinned precedence covers the PG cell only); on the
    // board the order is the visible 38 dp disc → a placed piece's drawn shape → the rest of the 60 dp area → a
    // piece's 12 dp margin (capture-outbox/chg-draft-req018-badge-touch-precedence.md, amended 2026-10-12).
    // The cells are read from the `tray-cell-<ID>` nodes, the badge areas from the `flip-badge-*` nodes.

    @Test // pin: REQ-015 rule "the touch area of a tray piece is its whole tray cell"; decisions.md 2026-10-12 "Where the flip badge's touch area ends" (code review F1); no token
    fun aStillTapInTheSecondLargeTrianglesCellNextToThePgCellTurnsItAndLeavesThePgAlone() {
        // Oracle: in the first full puzzle, every sampled point of LT2's cell within 30 dp of the PG cell (its
        // bottom-left corner first) is tapped still; each tap turns LT2 one step clockwise (REQ-016, what a tap on
        // a tray piece does) and leaves the PG's mirror and turn as they were, LT2 in the tray.
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val points = cellPointsNear(PieceId.LT2, PieceId.PG, kit.dp(30f))
        for ((at, where) in points) {
            val lt2 = kit.key(PlayTags.trayPiece(PieceId.LT2), PlaySemantics.TurnKey)
            val pgMirrored = kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.MirroredKey)
            val pgTurn = kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.TurnKey)
            kit.tap(at)
            kit.advance(48)
            val lt2After = kit.key(PlayTags.trayPiece(PieceId.LT2), PlaySemantics.TurnKey)
            val pgMirroredAfter = kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.MirroredKey)
            val pgTurnAfter = kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.TurnKey)
            val homes = kit.homesOf(PieceId.LT2)
            assertTrue(
                "F1 tray — REQ-015 rule \"the touch area of a tray piece is its whole tray cell\": a still tap at $where must turn " +
                    "LT2 one step and leave the PG alone; LT2 turn ${lt2.step} → ${lt2After.step}, PG mirrored $pgMirrored → " +
                    "$pgMirroredAfter, PG turn ${pgTurn.step} → ${pgTurnAfter.step}, LT2 homes $homes",
                lt2After == lt2.next() && pgMirroredAfter == pgMirrored && pgTurnAfter == pgTurn && homes == listOf(PlayTags.trayPiece(PieceId.LT2)),
            )
        }
    }

    @Test // pin: REQ-015 rule "the touch area of a tray piece is its whole tray cell"; decisions.md 2026-10-12 "Where the flip badge's touch area ends" (code review F1); no token
    fun aDragStartedInTheSecondLargeTrianglesCellNextToThePgCellPicksItUp() {
        // Oracle: from points of LT2's cell within 30 dp of the PG cell (its bottom-left corner first, then the
        // sampled point nearest the tray badge's drawn disc, then points spread over the sample), a touch that moves
        // 20 dp (past the 12 dp tap threshold, REQ-015: a drag) picks LT2 up — `drag-piece-LT2` after the 120 ms
        // grow — and the PG stays in the tray unmirrored; the release on the tray sends LT2 home (REQ-020).
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val all = cellPointsNear(PieceId.LT2, PieceId.PG, kit.dp(30f))
        val badgeCentre = drawnDisc(PlayTags.TRAY, PlayTags.FLIP_BADGE_TRAY).centre
        val nearestBadge = all.minByOrNull { distance(it.first, badgeCentre) }!!
        val spread = all.filterIndexed { i, _ -> i % max(1, all.size / 4) == 0 }
        val points = (listOf(all.first(), nearestBadge) + spread).distinctBy { it.first }.take(7)
        val pgMirrored = kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.MirroredKey)
        for ((at, where) in points) {
            kit.touch { down(at) }
            kit.frames()
            kit.touch { moveTo(Offset(at.x, at.y - kit.dp(20f))) }
            kit.frames()
            kit.advance(120)
            kit.frames()
            val dragged = kit.exists(PlayTags.dragPiece(PieceId.LT2))
            val homes = kit.homesOf(PieceId.LT2)
            val pgHomes = kit.homesOf(PieceId.PG)
            val pgMirroredNow = if (kit.exists(PlayTags.trayPiece(PieceId.PG))) kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.MirroredKey) else null
            kit.release()
            kit.advance(250)
            assertTrue(
                "F1 tray — REQ-015 rule \"the touch area of a tray piece is its whole tray cell\": a drag started at $where must " +
                    "pick LT2 up; LT2 homes $homes, PG homes $pgHomes, PG mirrored $pgMirrored → $pgMirroredNow",
                dragged && pgHomes == listOf(PlayTags.trayPiece(PieceId.PG)) && pgMirroredNow == pgMirrored,
            )
            check(kit.homesOf(PieceId.LT2) == listOf(PlayTags.trayPiece(PieceId.LT2))) { "kit: LT2 not home after the release on the tray" }
        }
    }

    @Test // pin: decisions.md 2026-10-12 "Where the flip badge's touch area ends" — on the board a touch inside another placed piece's drawn shape, outside the visible disc, belongs to that piece (chg-draft-req018-badge-touch-precedence.md, amended 2026-10-12; REQ-015 rule, REQ-016 statement); code review F1; no token
    fun aTapInsideANeighboursDrawnShapeUnderTheBoardBadgesAreaActsOnThatPieceNotOnThePg() {
        // Oracle: over the bundled puzzles, a board with the PG and a neighbour placed at their solution poses
        // (other pieces placed first where the engine needs them, one piece always left in the tray so the badge
        // shows) where the neighbour's drawn shape reaches into the `flip-badge-board` node's 60 dp area outside
        // the drawn disc (read from the drawing, `drawnDisc`: at least 2 dp outside it and more than 21 dp from its
        // centre; within 28 dp of the area's centre; at least 2 dp inside the shape; the pixel the neighbour's
        // colour). A still tap there acts on the neighbour as REQ-016 says a tap on a board piece
        // does — turned one step when the engine's `tryTurnInPlace` keeps it, else shaken (a `Shake` for it, its
        // drawing moved at least 2 dp) and kept — and the PG keeps its placement and mirror, is not shaken and is
        // drawn where it was (within 1 px) over the next 40 frames. Preferred: a neighbour the engine lets turn (a
        // visible turn), then a PG whose mirror locks (the wrong outcome would be a visible mirror); the bundled
        // puzzles may offer neither, and then the shake tells the two pieces apart.
        // Amended WO-010 (F1 kit half, a corrected check): a candidate board whose drawn disc cannot be read without doubt
        // is passed over by the search (named in the failure below), instead of ending the search; the oracle is unchanged.
        kit.show()
        val passedOver = ArrayList<String>()
        val case = badgeOverlapOrFail(findBoardBadgeOverlap(passedOver), passedOver)
        val p = case.puzzle
        val n = case.neighbour
        kit.open(p.id)
        settle()
        val area = kit.bounds(PlayTags.FLIP_BADGE_BOARD)
        val disc = drawnDisc(PlayTags.BOARD, PlayTags.FLIP_BADGE_BOARD)
        val (at, margin) = badgeTapPoint(area, disc, kit.boardPolygonPx(PlayTags.boardPiece(n)))
            ?: throw AssertionError("kit: ${p.id}: the $n shape no longer reaches the badge area $area outside the drawn disc $disc")
        val board = kit.capture(PlayTags.BOARD)
        val c = kit.toCapture(PlayTags.BOARD, at)
        val pixel = board[c.x.toInt(), c.y.toInt()]
        assertTrue("set-up: the tap point $at is inside $n's drawn shape — pixel ${kitHex(pixel)}, $n's colour ${kitHex(PlayPalette.piece(n))}", kitNear(pixel, PlayPalette.piece(n), 16f / 255f))
        val dp = kit.onMain { kit.controller.boardDpPerUnit }
        val before = kit.placed()
        val q = before.single { it.piece == n }
        val pg = before.single { it.piece == PieceId.PG }
        val turned = kitEngine.tryTurnInPlace(p, before - q, q, q.turn.next(), dp)
        val shakeBefore = kit.onMain { kit.controller.shake }
        val pgMirroredKey = kit.key(PlayTags.boardPiece(PieceId.PG), PlaySemantics.MirroredKey)
        val nTag = PlayTags.boardPiece(n)
        val pgTag = PlayTags.boardPiece(PieceId.PG)
        val nRestBox = kit.bounds(nTag)
        val pgRestBox = kit.bounds(pgTag)
        // The drawing over the next 40 frames (about 640 ms, past a shake): how far each piece is drawn from its
        // placement (tag table: bounds and CentreKey follow the drawing in each frame).
        var nMoved = 0f
        var pgMoved = 0f
        kit.tap(at)
        repeat(40) {
            if (kit.exists(nTag)) nMoved = max(nMoved, kit.displacementPx(nTag, q.centre, nRestBox))
            if (kit.exists(pgTag)) pgMoved = max(pgMoved, kit.displacementPx(pgTag, pg.centre, pgRestBox))
            kit.frames()
        }
        val after = kit.placed()
        val qAfter = after.singleOrNull { it.piece == n }
        val pgAfter = after.singleOrNull { it.piece == PieceId.PG }
        val shakeAfter = kit.onMain { kit.controller.shake }
        val newShake = shakeAfter != null && shakeAfter != shakeBefore
        val pgMirroredKeyAfter = if (kit.exists(PlayTags.boardPiece(PieceId.PG))) kit.key(PlayTags.boardPiece(PieceId.PG), PlaySemantics.MirroredKey) else null
        val pgAlone = kitSamePlacement(pgAfter, pg) && !(newShake && shakeAfter?.piece == PieceId.PG) &&
            pgMirroredKeyAfter == pgMirroredKey && pgMoved <= 1f
        val expected: String
        val neighbourActed = if (turned is LockResult.Locked) {
            expected = "$n turned one step to ${turned.placement}"
            kitSamePlacement(qAfter, turned.placement)
        } else {
            expected = "$n shaken (a Shake for $n, its drawing moved) and kept at $q"
            kitSamePlacement(qAfter, q) && newShake && shakeAfter?.piece == n && nMoved >= kit.dp(2f)
        }
        val fromDisc = distance(at, disc.centre) / kit.density
        assertTrue(
            "F1 board — decisions.md 2026-10-12 (\"a touch inside that piece's drawn shape belongs to the piece\"): ${p.id}, a still tap " +
                "at $at inside $n's drawn shape (${"%.1f".format(margin / kit.density)} dp inside it), inside `flip-badge-board` $area " +
                "and ${"%.1f".format(fromDisc)} dp from the drawn disc's centre (its radius ${"%.1f".format(disc.radiusPx / kit.density)} dp); " +
                "expected $expected and the PG left " +
                "alone; was: $n $q → $qAfter (drawn up to ${"%.1f".format(nMoved)} px off), PG $pg → $pgAfter (drawn up to " +
                "${"%.1f".format(pgMoved)} px off), shake $shakeBefore → $shakeAfter",
            neighbourActed && pgAlone,
        )
    }

    // ── Inside the PG's tray cell: the visible thing wins (fix batch, reviewer note on F1) ─────────────────
    // decisions.md 2026-10-12 (fix batch, reviewer note on F1): "Inside the parallelogram's tray cell the visible
    // thing wins, as on the board: the 38 dp disc → the PG miniature's drawn shape → the rest of the badge's 60 dp
    // area → the rest of the cell"; the draft's tray sentence, amended again 2026-10-12: "In the tray, the visible
    // badge wins; a touch on the parallelogram's drawn miniature belongs to the piece (REQ-015, REQ-016); the rest
    // of the badge's touch area wins over the rest of the cell." The disc is read from the drawing (`drawnDisc`):
    // decisions.md 2026-10-12 "What 'drawn' means in the touch order" — "the disc is the drawn disc (its drawn
    // centre, 19 dp radius) — not centred in its 60 dp area". The miniature's drawn shape is read from its keys
    // and confirmed by its colour in the tray capture. Every leg starts from the resting pose (S-2 `open` resets
    // the tray turns and mirror).

    @Test // pin: decisions.md 2026-10-12 "Inside the parallelogram's tray cell the visible thing wins" (chg-draft-req018-badge-touch-precedence.md, amended again 2026-10-12; REQ-016 statement, REQ-018 statement); no token
    fun aStillTapOnThePgMiniatureOutsideTheDiscTurnsItAndTheDiscAndTheBareBadgeAreaMirrorIt() {
        // Oracle, in the first full puzzle at the resting pose, with the disc read from the drawing: (1) a tap at the
        // drawn disc's centre, and at a point of the miniature's shape at least 2 dp inside the drawn disc, mirrors
        // the PG (REQ-018: the badge press) and leaves its turn; (2) a tap in the badge area where nothing is drawn
        // (at least 2 dp outside the disc and outside the shape) mirrors it; (3) a tap on the miniature's drawn
        // pixels more than 21 dp from the drawn disc's centre and 2 dp outside it — its middle first, then points
        // inside the badge's 60 dp area, then one outside it — turns the PG one step clockwise (REQ-016: a tap on a
        // tray piece) and does not mirror it.
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val g = pgTrayGeometry()
        android.util.Log.i("KitScreen", "PG tray: drawn disc ${g.disc} (radius ${"%.1f".format(g.discRadius / kit.density)} dp; ${"%.1f".format(distance(g.disc, g.area.center) / kit.density)} dp from the area's centre), area ${g.area}, cell ${g.cell}, middle ${g.middle} (${"%.1f".format(distance(g.middle, g.disc) / kit.density)} dp from the disc centre), ${g.drawn.size} drawn samples, ${g.drawnInArea.size} of them in the area outside the disc, under-disc ${g.underDisc}, bare area ${g.bareArea}")
        // (1) the disc
        val discPoints = listOfNotNull(g.disc to "the drawn disc's centre", g.underDisc?.let { it to "a point of the miniature's shape at least 2 dp inside the drawn disc" })
        for ((at, where) in discPoints) {
            kit.open(p.id)
            val (turn, mirrored) = pgTrayKeys()
            kit.tap(at)
            kit.advance(100)
            val (turnAfter, mirroredAfter) = pgTrayKeys()
            assertTrue(
                "PG tray disc — the visible disc wins (decisions.md 2026-10-12): a still tap at $at, $where " +
                    "(${"%.1f".format(distance(at, g.disc) / kit.density)} dp from the drawn disc's centre, radius " +
                    "${"%.1f".format(g.discRadius / kit.density)} dp), must mirror the PG and keep its turn; " +
                    "mirrored $mirrored → $mirroredAfter, turn ${turn.step} → ${turnAfter.step}",
                mirroredAfter == !mirrored && turnAfter == turn,
            )
        }
        // (2) the bare badge area
        if (g.bareArea != null) {
            kit.open(p.id)
            val (turn, mirrored) = pgTrayKeys()
            kit.tap(g.bareArea)
            kit.advance(100)
            val (turnAfter, mirroredAfter) = pgTrayKeys()
            assertTrue(
                "PG tray area — the rest of the badge's 60 dp area wins over the rest of the cell (REQ-018 rule): a still tap at " +
                    "${g.bareArea}, ${"%.1f".format(distance(g.bareArea, g.disc) / kit.density)} dp from the drawn disc's centre where no miniature " +
                    "is drawn, must mirror the PG and keep its turn; mirrored $mirrored → $mirroredAfter, turn ${turn.step} → ${turnAfter.step}",
                mirroredAfter == !mirrored && turnAfter == turn,
            )
        }
        // (3) the miniature's drawn shape outside the disc
        for ((at, where) in g.miniaturePoints()) {
            kit.open(p.id)
            val (turn, mirrored) = pgTrayKeys()
            kit.tap(at)
            kit.advance(100)
            val (turnAfter, mirroredAfter) = pgTrayKeys()
            val homes = kit.homesOf(PieceId.PG)
            assertTrue(
                "PG tray tap — a touch on the parallelogram's drawn miniature belongs to the piece (decisions.md 2026-10-12; REQ-016 " +
                    "\"WHEN the player taps a piece in the tray, the game SHALL turn it 45° clockwise\"): a still tap at $at, $where, " +
                    "${"%.1f".format(distance(at, g.disc) / kit.density)} dp from the disc's centre, must turn the PG one step and not mirror " +
                    "it; turn ${turn.step} → ${turnAfter.step}, mirrored $mirrored → $mirroredAfter, PG homes $homes",
                turnAfter == turn.next() && mirroredAfter == mirrored && homes == listOf(PlayTags.trayPiece(PieceId.PG)),
            )
        }
    }

    @Test // pin: decisions.md 2026-10-12 "Inside the parallelogram's tray cell the visible thing wins" (chg-draft-req018-badge-touch-precedence.md, amended again 2026-10-12; REQ-015 statement: a touch that moves 12 dp or more is a drag); no token
    fun aDragStartedOnThePgMiniatureOutsideTheDiscPicksItUp() {
        // Oracle, in the first full puzzle at the resting pose: from the miniature's middle and from drawn points
        // inside the badge's 60 dp area more than 21 dp from the drawn disc's centre (and 2 dp outside it), a touch that moves 20 dp (past the
        // 12 dp tap threshold) picks the PG up — `drag-piece-PG` after the 120 ms grow, unmirrored; the release on the
        // tray sends it home (REQ-020).
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val g = pgTrayGeometry()
        val points = g.miniaturePoints().take(4)
        for ((at, where) in points) {
            kit.open(p.id)
            kit.touch { down(at) }
            kit.frames()
            kit.touch { moveTo(Offset(at.x, at.y - kit.dp(20f))) }
            kit.frames()
            kit.advance(120)
            kit.frames()
            val dragged = kit.exists(PlayTags.dragPiece(PieceId.PG))
            val mirrored = if (dragged) kit.key(PlayTags.dragPiece(PieceId.PG), PlaySemantics.MirroredKey) else null
            val homes = kit.homesOf(PieceId.PG)
            kit.release()
            kit.advance(250)
            assertTrue(
                "PG tray drag — a touch on the parallelogram's drawn miniature belongs to the piece (decisions.md 2026-10-12; REQ-015: " +
                    "a touch that moves 12 dp or more is a drag): a drag started at $at, $where, " +
                    "${"%.1f".format(distance(at, g.disc) / kit.density)} dp from the disc's centre, must pick the PG up unmirrored; " +
                    "PG homes $homes, mirrored $mirrored",
                dragged && mirrored == false,
            )
            check(kit.homesOf(PieceId.PG) == listOf(PlayTags.trayPiece(PieceId.PG))) { "kit: PG not home after the release on the tray" }
        }
    }

    // ── REQ-043: the size marks ────────────────────────────────────────────────────────────────────────

    @Test // REQ-043.A1
    fun aFullSetTrayShowsTheMarksLLMSSOnTheFiveTrianglesAndNoneOnTheSquareOrTheParallelogram() {
        // REQ-043 A1: "A full-set tray shows the marks L, L, M, S, S on the five triangles, and none on the
        // square or the parallelogram." The letters are the S-11 literal ("S", "M", "L"). Oracle: in every
        // full-set puzzle, `size-mark-<ID>` of LT1, LT2 reads L, of MT reads M, of ST1, ST2 reads S, each
        // displayed inside its piece's cell; no size-mark node for SQ or PG.
        kit.show()
        val expected = mapOf(
            PieceId.LT1 to kitTexts.sizeLarge, PieceId.LT2 to kitTexts.sizeLarge, PieceId.MT to kitTexts.sizeMedium,
            PieceId.ST1 to kitTexts.sizeSmall, PieceId.ST2 to kitTexts.sizeSmall,
        )
        for (p in kitFullSetPuzzles()) {
            kit.open(p.id)
            for ((piece, letter) in expected) {
                val tag = PlayTags.sizeMark(piece)
                rule.onNodeWithTag(tag).assertIsDisplayed().assertTextEquals(letter)
                assertTrue("${p.id}: the mark of $piece is inside its cell", kit.bounds(PlayTags.trayCell(piece)).contains(kit.bounds(tag).center))
            }
            assertFalse("${p.id}: no mark on the square", kit.exists(PlayTags.sizeMark(PieceId.SQ)))
            assertFalse("${p.id}: no mark on the parallelogram", kit.exists(PlayTags.sizeMark(PieceId.PG)))
        }
    }

    @Test // pin: REQ-043 rule "The mark sits in the top-left corner of the cell, tinted with the piece's colour"; no token
    fun theMarkSitsTopLeftInItsCellTintedWithThePiecesColour() {
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val tray = kit.capture(PlayTags.TRAY)
        val origin = kit.bounds(PlayTags.TRAY).topLeft
        for (piece in PieceId.entries.filter { it.type == PieceType.LT || it.type == PieceType.MT || it.type == PieceType.ST }) {
            val cell = kit.bounds(PlayTags.trayCell(piece))
            val mark = kit.bounds(PlayTags.sizeMark(piece))
            assertTrue("$piece: mark centre ${mark.center} in the top-left quadrant of the cell $cell", mark.center.x < cell.center.x && mark.center.y < cell.center.y)
            val colour = PlayPalette.piece(piece)
            var tinted = 0
            for (y in mark.top.toInt()..mark.bottom.toInt()) for (x in mark.left.toInt()..mark.right.toInt()) {
                val c = tray[(x - origin.x).toInt().coerceIn(0, tray.width - 1), (y - origin.y).toInt().coerceIn(0, tray.height - 1)]
                if (kitNear(c, colour, 40f / 255f)) tinted++
            }
            assertTrue("$piece: pixels of the piece's colour in the mark's area ($tinted)", tinted >= 4)
        }
    }

    @Test // REQ-043.A2
    fun aPlacedTrianglesCellShowsOnlyItsDashedOutlineWithoutAMark() {
        // REQ-043 A2: "A placed triangle's cell shows only its dashed outline, without a mark." Rule: the mark
        // "is hidden while the piece is on the board"; REQ-012 rule: "A piece on the board leaves a dashed
        // outline in its tray cell." Oracle: with a triangle placed through S-2, its cell has a ghost node
        // and no mark node and no miniature; the pixels where the mark was (away from the outline) are the
        // tray colour; the cell has outline pixels (not the tray colour) and no filled miniature.
        kit.show()
        val (p, piece) = firstPlaceableTriangle()
        kit.open(p.id)
        val cell = kit.bounds(PlayTags.trayCell(piece))
        val markBox = kit.bounds(PlayTags.sizeMark(piece))
        val miniature = kit.trayPolygonPx(piece)
        assertTrue("set-up: $piece placed through S-2", kit.placeByApi(piece))
        kit.frames()
        kit.advance(250)
        assertFalse("${p.id}: no mark for the placed $piece", kit.exists(PlayTags.sizeMark(piece)))
        assertFalse("${p.id}: no miniature for the placed $piece", kit.exists(PlayTags.trayPiece(piece)))
        assertTrue("${p.id}: a ghost outline for the placed $piece", kit.exists(PlayTags.trayGhost(piece)))
        val ghost = kit.bounds(PlayTags.trayGhost(piece))
        assertTrue("${p.id}: the ghost $ghost is inside the cell $cell", cell.contains(ghost.topLeft) && cell.contains(ghost.bottomRight))
        assertEquals("${p.id}: the cell is unchanged", cell, kit.bounds(PlayTags.trayCell(piece)))
        // Pixels: the mark's former area, away from the outline, is bare tray (a dash of the outline that
        // strays into it is a few pixels; a mark is many).
        val tray = kit.capture(PlayTags.TRAY)
        val origin = kit.bounds(PlayTags.TRAY).topLeft
        val miniaturePts = miniature.map { Point(it.x.toDouble(), it.y.toDouble()) }
        var checked = 0
        var inked = 0
        for (y in markBox.top.toInt()..markBox.bottom.toInt()) for (x in markBox.left.toInt()..markBox.right.toInt()) {
            val q = Point(x + 0.5, y + 0.5)
            if (kitDistToBoundary(q, miniaturePts) < 6.0) continue
            checked++
            val c = tray[(x - origin.x).toInt().coerceIn(0, tray.width - 1), (y - origin.y).toInt().coerceIn(0, tray.height - 1)]
            if (!kitNear(c, PlayPalette.tray, 12f / 255f)) inked++
        }
        assertTrue("${p.id}: the mark's area has pixels away from the outline ($checked)", checked >= 8)
        assertTrue("${p.id}: $inked of $checked pixels where the mark was are not the tray colour", inked * 100 <= checked * 8)
        // The interior of the former miniature, away from its outline, is not a filled piece.
        var filled = 0
        var interior = 0
        for (s in kitInteriorSamples(miniature, 6f)) {
            interior++
            if (kitNear(tray[(s.x - origin.x).toInt(), (s.y - origin.y).toInt()], PlayPalette.piece(piece), 16f / 255f)) filled++
        }
        assertTrue("${p.id}: the fixture samples the miniature's interior ($interior)", interior >= 8)
        assertTrue("${p.id}: $filled of $interior interior pixels are the piece colour: a filled miniature in the cell of the placed $piece", filled * 10 < interior)
        // Something is drawn along the outline.
        var outlineInk = 0
        for (i in miniature.indices) {
            val a = miniature[i]
            val b = miniature[(i + 1) % miniature.size]
            for (k in 1..9) {
                val x = (a.x + (b.x - a.x) * k / 10 - origin.x).toInt()
                val y = (a.y + (b.y - a.y) * k / 10 - origin.y).toInt()
                for (dy in -2..2) for (dx in -2..2) {
                    val xx = (x + dx).coerceIn(0, tray.width - 1)
                    val yy = (y + dy).coerceIn(0, tray.height - 1)
                    if (!kitNear(tray[xx, yy], PlayPalette.tray, 12f / 255f)) outlineInk++
                }
            }
        }
        assertTrue("${p.id}: outline pixels in the cell of the placed $piece", outlineInk >= 10)
    }

    // ── Kit self-checks (no token): the disc reading and the badge search (reviews/WO-010-code-review-delta.md S-1) ────
    // The two readings the board-badge pin rests on are shown to discriminate, as WO-010 TASK-041 showed its pins. The disc
    // reading gets crafted images of the TASK-014 row's badge (a 38 dp white disc, a 2.5 dp accent ring, an accent ⇋
    // glyph) at the floor image's 2.625 px/dp. The search runs on the bundled boards with a reading made to doubt.

    @Test // kit self-check, no token: reviews/WO-010-code-review-delta.md S-1 — the disc reading keeps the floor's seam pockets and doubts a second disc or ring
    fun kitSelfCheckTheDiscReadingKeepsSeamPocketsAndDoubtsASecondDiscOrRing() {
        val d = 2.625f // px per dp of the floor image (1080 × 2400 px at 420 dpi)
        val cx = 110f
        val cy = 106f
        fun read(name: String, image: KitDiscImage) = readDiscIn(image.w, image.h, image::at, null, d, "in the self-check image `$name`")
        fun badge(i: KitDiscImage) { i.ring(cx, cy, d); i.glyph(cx, cy, d) }
        // A solid accent triangle (LT2's colour) whose corner lies inside the ring band, as LT2's corner did on the floor's letters-m.
        fun lt2(i: KitDiscImage) = i.triangle(Offset(cx - 17f * d, cy + 4f * d), Offset(cx - 17f * d - 70f, cy + 4f * d), Offset(cx - 17f * d, cy + 4f * d + 70f))
        val pocketsRight = (cx - 19f * d).toInt() - 2 // left of the ring's outer edge: outside the inside's box, as the floor's pockets were
        val reads = linkedMapOf(
            "a plain disc" to KitDiscImage(212, 212).also { badge(it) },
            "a disc with a solid accent triangle on its ring" to KitDiscImage(212, 212).also { badge(it); lt2(it) },
            // run log f2-s1: letters-m (PG + LT2) left 24 enclosed px in 6 regions outside the inside's box, the largest 12 px
            "letters-m's case: the triangle on the ring and seam pockets of 24 px in 6 regions" to
                KitDiscImage(212, 212).also { badge(it); lt2(it); it.pockets(pocketsRight, 4, listOf(4 to 3, 2 to 2, 2 to 2, 2 to 1, 1 to 1, 1 to 1)) },
            // run log f2-s1: the floor's largest, 137 px in 50 regions, the largest 15 px (shapes-warmup-4, shapes-trapezoid)
            "the floor's largest seam: pockets of 137 px in 50 regions" to
                KitDiscImage(212, 212).also { badge(it); it.pockets(pocketsRight, 4, listOf(5 to 3) + List(24) { 3 to 1 } + List(25) { 2 to 1 }) },
        )
        for ((name, image) in reads) {
            val r = read(name, image)
            val disc = r.disc ?: throw AssertionError("kit self-check: $name must read; it is in doubt: ${r.doubt}")
            assertTrue("kit self-check: $name — the centre ${disc.centre}, expected ($cx, $cy) within 1 px", abs(disc.centre.x - cx) <= 1f && abs(disc.centre.y - cy) <= 1f)
            assertTrue("kit self-check: $name — the drawn radius ${disc.radiusPx} px, expected 19 dp (${19f * d} px) within 2 px", abs(disc.radiusPx - 19f * d) <= 2f)
        }
        val doubts = linkedMapOf(
            "a second disc beside the first" to
                KitDiscImage(212, 212).also { it.ring(55f, cy, d); it.glyph(55f, cy, d); it.ring(158f, cy, d); it.glyph(158f, cy, d) },
            "a doubled ring 10 px off the first" to KitDiscImage(212, 212).also { badge(it); it.ring(cx + 10f, cy, d) },
            "a doubled ring 20 px off the first" to KitDiscImage(212, 212).also { badge(it); it.ring(cx + 20f, cy, d) },
            "seam pockets of 600 px, more than a tenth of the inside" to KitDiscImage(212, 212).also { badge(it); it.pockets(pocketsRight, 4, List(300) { 2 to 1 }) },
            "no ring, the glyph alone" to KitDiscImage(212, 212).also { it.glyph(cx, cy, d) },
        )
        for ((name, image) in doubts) {
            val r = read(name, image)
            assertTrue("kit self-check: $name must be a reading in doubt; it read ${r.disc}", r.disc == null && r.doubt != null)
            android.util.Log.i("KitScreen", "self-check: $name — in doubt: ${r.doubt}")
        }
    }

    @Test // kit self-check, no token: reviews/WO-010-code-review-delta.md S-1 — the badge search passes over a board in doubt and goes on, and the leg fails loudly when no case is left
    fun kitSelfCheckTheBadgeSearchPassesOverABoardInDoubtAndFailsLoudlyWithNoneLeft() {
        kit.show()
        val doubt = "kit: a reading the self-check made to doubt"
        // (1) The first board that gives the pin's case, read as the leg reads it.
        val a = findBoardBadgeOverlap(ArrayList(), firstCaseOnly = true) ?: throw AssertionError("kit self-check: no bundled board gives the pin's case at all")
        // (2) That board's reading in doubt: it is passed over and named, and the search goes on to another case.
        val passed = ArrayList<String>()
        val b = findBoardBadgeOverlap(passed, firstCaseOnly = true, read = { c, n -> if (kit.puzzle.id == a.puzzle.id) DiscReading(null, doubt) else readDrawnDisc(c, n) })
        assertTrue("kit self-check: ${a.puzzle.id}'s board, in doubt, is named among the boards passed over: $passed", passed.any { it.startsWith("${a.puzzle.id} (") && it.endsWith(doubt) })
        assertTrue("kit self-check: the search goes on past ${a.puzzle.id} to another case (found ${b?.puzzle?.id})", b != null && b.puzzle.id != a.puzzle.id)
        // (3) Every reading in doubt, over the bundled puzzles up to and including that board's: no case is left, and the leg's
        // failure is loud and names each board passed over.
        val upToA = kitLibrary.puzzles.subList(0, kitLibrary.puzzles.indexOf(a.puzzle) + 1)
        val all = ArrayList<String>()
        val c = findBoardBadgeOverlap(all, puzzles = upToA, read = { _, _ -> DiscReading(null, doubt) })
        assertTrue("kit self-check: with every reading in doubt no case is left (found ${c?.puzzle?.id})", c == null)
        assertTrue("kit self-check: ${a.puzzle.id}'s board is among the boards passed over: $all", all.any { it.startsWith("${a.puzzle.id} (") })
        val failure = try {
            badgeOverlapOrFail(c, all)
            null
        } catch (e: AssertionError) {
            e
        }
        assertTrue("kit self-check: the leg fails loudly when no case is left", failure != null)
        for (entry in all) assertTrue("kit self-check: the failure names the board passed over `$entry`: ${failure?.message}", failure?.message?.contains(entry) == true)
    }

    /** A crafted accent image for the disc reading's self-check: `true` where the accent colour is drawn, read at pixel centres. */
    private class KitDiscImage(val w: Int, val h: Int) {
        private val accent = BooleanArray(w * h)

        fun at(x: Float, y: Float): Boolean = x >= 0f && y >= 0f && x.toInt() < w && y.toInt() < h && accent[y.toInt() * w + x.toInt()]

        private fun paint(inside: (Float, Float) -> Boolean) {
            for (y in 0 until h) for (x in 0 until w) if (inside(x + 0.5f, y + 0.5f)) accent[y * w + x] = true
        }

        /** The TASK-014 row's ring: a 2.5 dp accent band whose outer edge is the 38 dp disc's. */
        fun ring(cx: Float, cy: Float, d: Float) = paint { x, y -> hypot(x - cx, y - cy).let { it >= 16.5f * d && it <= 19f * d } }

        /** A ⇋-like glyph: two accent bars 18 dp long and 2 dp thick, 4 dp above and below the centre, clear of the ring. */
        fun glyph(cx: Float, cy: Float, d: Float) = paint { x, y -> abs(x - cx) <= 9f * d && (abs(y - cy - 4f * d) <= d || abs(y - cy + 4f * d) <= d) }

        /** A solid accent triangle. */
        fun triangle(a: Offset, b: Offset, c: Offset) = paint { x, y ->
            fun side(p: Offset, q: Offset) = (x - q.x) * (p.y - q.y) - (p.x - q.x) * (y - q.y)
            val s1 = side(a, b)
            val s2 = side(b, c)
            val s3 = side(c, a)
            !((s1 < 0f || s2 < 0f || s3 < 0f) && (s1 > 0f || s2 > 0f || s3 > 0f))
        }

        /** Seam pockets: each hole (`width` × `height` px) inside a 1 px accent frame, the frames stacked 1 px apart in columns ending at [xRight]. */
        fun pockets(xRight: Int, yTop: Int, holes: List<Pair<Int, Int>>) {
            var x = xRight
            var y = yTop
            for ((hw, hh) in holes) {
                val fw = hw + 2
                val fh = hh + 2
                if (y + fh > h - 12) { y = yTop; x -= 6 }
                for (j in 0 until fh) for (i in 0 until fw) {
                    if (i == 0 || j == 0 || i == fw - 1 || j == fh - 1) accent[(y + j) * w + (x - fw + 1 + i)] = true
                }
                y += fh + 1
            }
        }
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    private class MirrorCase(val puzzle: PuzzleDefinition, val neighbour: PieceId?, val before: Placement, val after: Placement)

    /**
     * A board where the PG locks unmirrored and `tryMirrorInPlace` locks its mirror (TYPE-004 decides): the
     * PG alone, or with one other solution piece placed at its pose, at grid centres over the silhouette.
     */
    private fun findBoardMirrorCase(): MirrorCase? {
        val dp = kit.onMain { kit.controller.boardDpPerUnit }
        for (p in kitLibrary.puzzles) {
            if (PieceId.PG !in p.pieces) continue
            val all = p.solution.flatMap { it.polygon }
            val boards = ArrayList<Pair<PieceId?, List<Placement>>>()
            boards += null to emptyList()
            for (sp in p.solution) {
                if (sp.piece == PieceId.PG) continue
                val pose = kitPoseOf(sp.piece, sp.polygon)
                if (kitEngine.findLock(p, emptyList(), sp.piece, pose.turn, pose.mirrored, pose.centre, dp) is LockResult.Locked) {
                    boards += sp.piece to listOf(Placement(sp.piece, pose.turn, pose.mirrored, pose.centre))
                }
            }
            for ((neighbour, placed) in boards) {
                var y = all.minOf { it.y }
                while (y <= all.maxOf { it.y }) {
                    var x = all.minOf { it.x }
                    while (x <= all.maxOf { it.x }) {
                        for (step in 0..7) {
                            val lock = kitEngine.findLock(p, placed, PieceId.PG, Turn(step), false, Point(x, y), dp)
                            if (lock is LockResult.Locked) {
                                val mirrored = kitEngine.tryMirrorInPlace(p, placed, lock.placement, dp)
                                if (mirrored is LockResult.Locked) return MirrorCase(p, neighbour, lock.placement, mirrored.placement)
                            }
                        }
                        x += 0.5
                    }
                    y += 0.5
                }
            }
        }
        return null
    }

    /** The PG from the tray to [target] through S-2 (its turn by `twistDrag`, unmirrored). */
    private fun placePgByApi(target: Placement) {
        kit.onMain {
            val c = kit.controller
            check(!c.trayMirrored(PieceId.PG)) { "kit: PG unexpectedly mirrored in the tray" }
            check(c.startDrag(PieceId.PG, target.centre)) { "kit: startDrag(PG) refused" }
            val from = c.dragged?.turn?.step ?: error("kit: no drag")
            val steps = ((target.turn.step - from) % 8 + 8) % 8
            if (steps != 0) c.twistDrag(if (steps <= 4) steps else steps - 8)
            c.moveDrag(target.centre, overBoard = true)
            c.updatePreview()
            checkNotNull(c.drop()) { "kit: the planned PG drop did not lock" }
        }
    }

    private fun firstPlaceableTriangle(): Pair<PuzzleDefinition, PieceId> {
        val dp = kit.onMain { kit.controller.boardDpPerUnit }
        for (p in kitFullSetPuzzles()) for (sp in p.solution) {
            if (sp.piece.type != PieceType.LT && sp.piece.type != PieceType.MT && sp.piece.type != PieceType.ST) continue
            val pose = kitPoseOf(sp.piece, sp.polygon)
            if (kitEngine.findLock(p, emptyList(), sp.piece, pose.turn, pose.mirrored, pose.centre, dp) is LockResult.Locked) return p to sp.piece
        }
        throw AssertionError("kit: no triangle locks alone at its solution pose in any full-set puzzle")
    }

    private fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

    /**
     * Points of [piece]'s tray cell (2 px inside its node bounds) within [withinPx] of [other]'s cell — its
     * bottom-left corner first, then a 10 dp × 7 dp grid — each with a description for the assertion message.
     */
    private fun cellPointsNear(piece: PieceId, other: PieceId, withinPx: Float): List<Pair<Offset, String>> {
        val cell = kit.bounds(PlayTags.trayCell(piece))
        val o = kit.bounds(PlayTags.trayCell(other))
        val inset = 2f
        fun gap(q: Offset): Float = hypot(max(0f, max(o.left - q.x, q.x - o.right)), max(0f, max(o.top - q.y, q.y - o.bottom)))
        fun describe(q: Offset, name: String): String =
            "(${"%.1f".format(q.x)}, ${"%.1f".format(q.y)}) px in $piece's cell $cell" + (if (name.isEmpty()) "" else ", $name") +
                ", ${"%.1f".format((cell.bottom - q.y) / kit.density)} dp above its bottom, ${"%.1f".format(gap(q) / kit.density)} dp from the $other cell $o"
        val out = ArrayList<Pair<Offset, String>>()
        val corner = Offset(cell.left + inset, cell.bottom - inset)
        out += corner to describe(corner, "its bottom-left corner")
        var y = cell.bottom - inset
        while (y >= cell.top + inset) {
            var x = cell.left + inset
            while (x <= cell.right - inset) {
                val q = Offset(x, y)
                if (gap(q) <= withinPx && out.none { it.first == q }) out += q to describe(q, "")
                x += kit.dp(10f)
            }
            y -= kit.dp(7f)
        }
        check(out.size >= 6) { "kit: only ${out.size} points of the $piece cell $cell lie within ${withinPx / kit.density} dp of the $other cell $o" }
        return out
    }

    /** The PG tray miniature's turn and mirror (S-14 keys). */
    private fun pgTrayKeys(): Pair<Turn, Boolean> =
        kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.TurnKey) to kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.MirroredKey)

    private inner class PgTrayGeometry(
        val disc: Offset,
        val discRadius: Float,
        val area: Rect,
        val cell: Rect,
        val middle: Offset,
        val drawn: List<Offset>,
        val drawnInArea: List<Offset>,
        val underDisc: Offset?,
        val bareArea: Offset?,
    ) {
        /**
         * Points on the miniature's drawn pixels outside the drawn disc (more than 21 dp from its centre and at
         * least 2 dp outside its drawn radius): the middle first, then up to four inside the badge area (spread
         * over the sample), then one outside it.
         */
        fun miniaturePoints(): List<Pair<Offset, String>> {
            val out = ArrayList<Pair<Offset, String>>()
            check(distance(middle, disc) > outsideDisc(discRadius)) { "kit: the miniature's middle $middle is not outside the drawn disc $disc (radius $discRadius px)" }
            out += middle to "the miniature's middle" + (if (area.contains(middle)) " (inside the badge area)" else " (outside the badge area)")
            val step = kotlin.math.max(1, drawnInArea.size / 4)
            drawnInArea.filterIndexed { i, _ -> i % step == 0 }.take(4).forEach { out += it to "a drawn point of the miniature inside the badge's 60 dp area" }
            drawn.firstOrNull { !area.contains(it) && distance(it, middle) > kit.dp(6f) }?.let { out += it to "a drawn point of the miniature outside the badge area" }
            return out.distinctBy { it.first }
        }
    }

    /**
     * The PG tray cell at the resting pose, read from the nodes and the tray capture: the disc as drawn
     * (`drawnDisc`: its centre and drawn radius — decisions.md 2026-10-12 "What 'drawn' means in the touch order",
     * the disc is not centred in its 60 dp area), the badge area (the `flip-badge-tray` bounds), the cell, the
     * miniature's middle (the mean of its drawn corners, as `trayPolygonPx` reads them from the keys), and the
     * drawn samples: inside the shape, 3 px from its edges, painted the PG colour, outside the drawn disc (more than
     * 21 dp from its centre and 2 dp outside its radius). `underDisc`: the point of the shape (3 px inside) nearest
     * the disc's centre, at least 2 dp inside the drawn disc. `bareArea`: a point of the badge area (2 px inside its
     * bounds, within 28 dp of its centre) inside the PG cell, at least 2 dp outside both the drawn disc and the
     * shape and not painted the PG colour — the one farthest from both.
     */
    private fun pgTrayGeometry(): PgTrayGeometry {
        val area = kit.bounds(PlayTags.FLIP_BADGE_TRAY)
        val drawnDisc = drawnDisc(PlayTags.TRAY, PlayTags.FLIP_BADGE_TRAY)
        val disc = drawnDisc.centre
        val outside = outsideDisc(drawnDisc.radiusPx)
        val cell = kit.bounds(PlayTags.trayCell(PieceId.PG))
        val poly = kit.trayPolygonPx(PieceId.PG)
        val pts = poly.map { Point(it.x.toDouble(), it.y.toDouble()) }
        val trayBounds = kit.bounds(PlayTags.TRAY)
        val tray = kit.capture(PlayTags.TRAY)
        val origin = trayBounds.topLeft
        val colour = PlayPalette.piece(PieceId.PG)
        fun painted(o: Offset): Boolean {
            val x = (o.x - origin.x).toInt()
            val y = (o.y - origin.y).toInt()
            return x in 0 until tray.width && y in 0 until tray.height && kitNear(tray[x, y], colour, 16f / 255f)
        }
        val inside = kitInteriorSamples(poly, 3f, within = trayBounds)
        val drawn = inside.filter { painted(it) && distance(it, disc) > outside }
        val drawnInArea = drawn.filter { area.deflate(2f).contains(it) }
        val c = kitCentroid(pts)
        val middle = Offset(c.x.toFloat(), c.y.toFloat())
        check(kitInside(c, pts) && kitDistToBoundary(c, pts) >= 3.0) { "kit: the miniature's middle $middle is not inside its shape" }
        check(painted(middle)) { "kit: the miniature's middle $middle is not painted the PG colour" }
        val underDisc = inside.filter { distance(it, disc) <= drawnDisc.radiusPx - kit.dp(2f) }.minByOrNull { distance(it, disc) }
        var bare: Offset? = null
        var bareClearance = 0f
        val step = kit.dp(1f)
        var y = area.top + 2f
        while (y <= area.bottom - 2f) {
            var x = area.left + 2f
            while (x <= area.right - 2f) {
                val q = Offset(x, y)
                val pq = Point(q.x.toDouble(), q.y.toDouble())
                x += step
                if (!cell.deflate(2f).contains(q) || distance(q, area.center) > kit.dp(28f)) continue
                val fromDisc = distance(q, disc) - drawnDisc.radiusPx
                if (fromDisc < kit.dp(2f) || kitInside(pq, pts) || painted(q)) continue
                val fromShape = kitDistToBoundary(pq, pts).toFloat()
                if (fromShape < kit.dp(2f)) continue
                val clearance = kotlin.math.min(fromDisc, fromShape)
                if (clearance > bareClearance) { bare = q; bareClearance = clearance }
            }
            y += step
        }
        check(drawn.size >= 8) { "kit: only ${drawn.size} drawn PG samples outside the drawn disc" }
        return PgTrayGeometry(disc, drawnDisc.radiusPx, area, cell, middle, drawn, drawnInArea, underDisc, bare)
    }

    private fun settle() {
        kit.frames()
        kit.advance(250)
    }

    private class BadgeOverlap(val puzzle: PuzzleDefinition, val neighbour: PieceId, val rank: Int)

    /**
     * The tap point for the board badge case: inside [poly] (at least 2 dp from its edges), inside the badge
     * area [area] (2 px inside the node bounds and within 28 dp of its centre, so inside the 60 dp area however it
     * is shaped), outside the drawn [disc] (more than 21 dp from its centre and 2 dp outside its drawn radius),
     * and on the board. The point farthest inside [poly], with that distance (px).
     */
    private fun badgeTapPoint(area: Rect, disc: DrawnDisc, poly: List<Offset>): Pair<Offset, Float>? {
        val pts = poly.map { Point(it.x.toDouble(), it.y.toDouble()) }
        val board = kit.bounds(PlayTags.BOARD)
        val outside = outsideDisc(disc.radiusPx)
        val step = kit.dp(1f)
        var best: Pair<Offset, Float>? = null
        var y = area.top + 2f
        while (y <= area.bottom - 2f) {
            var x = area.left + 2f
            while (x <= area.right - 2f) {
                val q = Offset(x, y)
                x += step
                if (!board.contains(q) || distance(q, area.center) > kit.dp(28f) || distance(q, disc.centre) <= outside) continue
                val pq = Point(q.x.toDouble(), q.y.toDouble())
                if (!kitInside(pq, pts)) continue
                val m = kitDistToBoundary(pq, pts).toFloat()
                if (m < kit.dp(2f)) continue
                if (best == null || m > best.second) best = q to m
            }
            y += step
        }
        return best
    }

    /** "Outside the drawn disc": more than 21 dp from its centre and at least 2 dp outside its drawn radius (px). */
    private fun outsideDisc(radiusPx: Float): Float = max(kit.dp(21f), radiusPx + kit.dp(2f))

    /** The flip badge's disc as drawn: its centre and its radius to the ring's outer edge, in `play-screen` px. */
    private data class DrawnDisc(val centre: Offset, val radiusPx: Float)

    /** A reading of the drawn disc: the disc, or (`disc` null) the `kit:` reason it cannot be read without doubt. */
    private class DiscReading(val disc: DrawnDisc?, val doubt: String?)

    /** The badge's disc read from the drawing ([readDrawnDisc]); a reading in doubt fails the test with its `kit:` reason. */
    private fun drawnDisc(captureTag: String, nodeTag: String): DrawnDisc {
        val reading = readDrawnDisc(captureTag, nodeTag)
        return reading.disc ?: throw IllegalStateException(reading.doubt)
    }

    /**
     * The badge's disc located from the drawing, never from its node or a constant (decisions.md 2026-10-12 "What
     * 'drawn' means in the touch order": the disc is not centred in its 60 dp area). The disc is a white disc with
     * an accent ring and an accent ⇋ glyph inside (decisions.md TASK-014 row). In the [captureTag] capture, within
     * the [nodeTag] node's bounds grown by 8 dp: the pixels not of the accent colour that cannot be reached from the
     * region's border without crossing an accent pixel are enclosed. Colour alone is not enough: LT2 is drawn in the
     * accent colour and can lie under the board badge; a solid piece only thickens the walls. The disc's inside is
     * the largest enclosed region (4-connected), with the regions lying wholly inside its bounding box (pockets of
     * the glyph's strokes). *Amended WO-010 (F1 kit half, a corrected check):* where an accent piece touches the ring
     * from outside, the anti-aliased seam between the two can enclose a few pixels beyond the ring (on the floor,
     * `letters-m` with PG + LT2: the box read 94 × 88 px); such a pocket is cut off from the inside by the ring, is
     * not the disc's, and no longer widens the box. The bounding box of the inside gives the centre; the drawn radius
     * is the ring's outer edge, measured on 16 rays from the centre (a ray that runs on into an accent-coloured piece,
     * more than 5 dp past the inside, is skipped; the median of the rest). Checked: the inside is square within 3 px,
     * 20–46 dp across and fills at least half its circle; at least 8 rays measure the ring. A check that fails is a
     * reading in doubt: the `kit:` reason is returned, never a guess. *Amended WO-010 (recirculation 4, delta code review
     * S-1; a corrected check):* the pixels enclosed outside the inside's box may be a seam pocket, never a second disc or
     * ring: more than a tenth of the inside there is a reading in doubt ([readDiscIn]). The reading of the region is
     * [readDiscIn], which the kit's self-checks feed with crafted images.
     */
    private fun readDrawnDisc(captureTag: String, nodeTag: String): DiscReading {
        val captureBounds = kit.bounds(captureTag)
        val image = kit.capture(captureTag)
        val region = kit.bounds(nodeTag).inflate(kit.dp(8f)).intersect(captureBounds)
        val x0 = (region.left - captureBounds.left).toInt().coerceIn(0, image.width - 1)
        val y0 = (region.top - captureBounds.top).toInt().coerceIn(0, image.height - 1)
        val w = ((region.right - captureBounds.left).toInt().coerceIn(0, image.width)) - x0
        val h = ((region.bottom - captureBounds.top).toInt().coerceIn(0, image.height)) - y0
        if (!(w > 2 && h > 2)) return DiscReading(null, "kit: no capture area around `$nodeTag`")
        fun accentAt(cx: Int, cy: Int): Boolean =
            cx in 0 until image.width && cy in 0 until image.height && kitNear(image[cx, cy], PlayPalette.accent, 24f / 255f)
        // Amended WO-004 (TASK-019): In progress the Restart pill is drawn at the board's top-left, above the silhouette
        // (S-22 "Restart"; decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn"); see [readDiscIn].
        val pill = if (captureTag == PlayTags.BOARD && kit.exists(KitTag.RESTART)) kit.bounds(KitTag.RESTART) else null
        val inPill: ((Int, Int) -> Boolean)? = pill?.let { r -> { x, y -> r.contains(Offset(captureBounds.left + x0 + x + 0.5f, captureBounds.top + y0 + y + 0.5f)) } }
        val context = "near `$nodeTag` ${kit.bounds(nodeTag)} in ${kit.puzzle.id} (placed ${kit.placed().map { it.piece }})"
        val reading = readDiscIn(w, h, { x, y -> accentAt((x0 + x).toInt(), (y0 + y).toInt()) }, inPill, kit.density, context)
        val inRegion = reading.disc ?: return reading
        return DiscReading(DrawnDisc(Offset(captureBounds.left + x0 + inRegion.centre.x, captureBounds.top + y0 + inRegion.centre.y), inRegion.radiusPx), null)
    }

    /**
     * [readDrawnDisc]'s reading of one region, `w` × `h` px, in the region's own px: [accentAt] says whether a point is the
     * accent colour (pixel `(x, y)` is asked at `(x.toFloat(), y.toFloat())`; a ray may ask outside the region), [inPill]
     * whether a region pixel lies in the Restart pill, [pxPerDp] the density. The disc it returns has its centre in region px.
     */
    private fun readDiscIn(w: Int, h: Int, accentAt: (Float, Float) -> Boolean, inPill: ((Int, Int) -> Boolean)?, pxPerDp: Float, context: String): DiscReading {
        fun doubt(reason: String) = DiscReading(null, reason)
        val accent = BooleanArray(w * h) { i -> accentAt((i % w).toFloat(), (i / w).toFloat()) }
        // Flood the non-accent pixels from the region's border; what stays unreached is enclosed by accent pixels.
        val reached = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var head = 0
        var tail = 0
        fun seed(i: Int) {
            if (!accent[i] && !reached[i]) { reached[i] = true; queue[tail++] = i }
        }
        for (x in 0 until w) { seed(x); seed((h - 1) * w + x) }
        for (y in 0 until h) { seed(y * w); seed(y * w + w - 1) }
        while (head < tail) {
            val i = queue[head++]
            val x = i % w
            val y = i / w
            if (x > 0) seed(i - 1)
            if (x < w - 1) seed(i + 1)
            if (y > 0) seed(i - w)
            if (y < h - 1) seed(i + w)
        }
        val enclosed = BooleanArray(w * h) { i -> !accent[i] && !reached[i] }
        // Amended WO-004 (TASK-019): In progress the Restart pill is drawn at the board's top-left, above the silhouette
        // (S-22 "Restart"; decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn"). Where it lies in
        // the region, its own outline or label can enclose pixels too (the pill's inside, a letter's counter). An enclosed
        // component lying wholly inside the `restart` node is the pill's, never the disc's, and is not counted.
        if (inPill != null) {
            val component = IntArray(w * h)
            for (start in 0 until w * h) {
                if (!enclosed[start]) continue
                var n = 0
                var allInPill = true
                component[n++] = start
                enclosed[start] = false
                var k = 0
                while (k < n) {
                    val i = component[k++]
                    val x = i % w
                    val y = i / w
                    if (!inPill(x, y)) allInPill = false
                    for (j in listOf(if (x > 0) i - 1 else -1, if (x < w - 1) i + 1 else -1, if (y > 0) i - w else -1, if (y < h - 1) i + w else -1)) {
                        if (j >= 0 && !accent[j] && !reached[j] && enclosed[j]) { enclosed[j] = false; component[n++] = j }
                    }
                }
                // Restore the component unless it is the pill's (marked by `reached`, so the scan below skips it).
                for (m in 0 until n) if (allInPill) reached[component[m]] = true
            }
            for (i in 0 until w * h) enclosed[i] = !accent[i] && !reached[i]
        }
        // The enclosed regions (4-connected, as the flood), each with its size and bounding box.
        val label = IntArray(w * h) { -1 }
        val stack = IntArray(w * h)
        val sizes = ArrayList<Int>()
        val boxes = ArrayList<IntArray>() // minX, minY, maxX, maxY
        for (start in 0 until w * h) {
            if (!enclosed[start] || label[start] >= 0) continue
            val id = sizes.size
            var top = 0
            var n = 0
            var bx0 = Int.MAX_VALUE
            var by0 = Int.MAX_VALUE
            var bx1 = -1
            var by1 = -1
            label[start] = id
            stack[top++] = start
            while (top > 0) {
                val i = stack[--top]
                n++
                val x = i % w
                val y = i / w
                bx0 = min(bx0, x); bx1 = max(bx1, x)
                by0 = min(by0, y); by1 = max(by1, y)
                for (j in intArrayOf(if (x > 0) i - 1 else -1, if (x < w - 1) i + 1 else -1, if (y > 0) i - w else -1, if (y < h - 1) i + w else -1)) {
                    if (j >= 0 && enclosed[j] && label[j] < 0) { label[j] = id; stack[top++] = j }
                }
            }
            sizes += n
            boxes += intArrayOf(bx0, by0, bx1, by1)
        }
        if (sizes.isEmpty()) return doubt("kit: no pixel enclosed by the badge's accent ring $context")
        val main = sizes.indices.maxByOrNull { sizes[it] }!!
        val (minX, minY, maxX, maxY) = boxes[main]
        var inside = 0
        var outsideBox = 0
        var outsideRegions = 0
        var largestOutside = 0
        for (k in sizes.indices) {
            val b = boxes[k]
            if (b[0] >= minX && b[1] >= minY && b[2] <= maxX && b[3] <= maxY) {
                inside += sizes[k]
            } else {
                outsideBox += sizes[k]
                outsideRegions++
                largestOutside = max(largestOutside, sizes[k])
            }
        }
        if (outsideBox > 0) {
            android.util.Log.i(
                "KitScreen",
                "drawn disc $context: $outsideBox enclosed px in $outsideRegions regions outside the inside's box (largest $largestOutside px), " +
                    "inside $inside px (${"%.1f".format(100.0 * outsideBox / max(1, inside))} %)",
            )
        }
        // Amended WO-010 (recirculation 4, delta code review S-1; a corrected check): outside the inside's box, an enclosed
        // region may only be a seam pocket where an accent piece meets the ring. On the floor, over every PG board, the largest
        // total was 137 px in 50 regions, none above 15 px: 2.6 % of a 5,370 px inside (shapes-warmup-4 and shapes-trapezoid,
        // with LT2 on the ring; run log f2-s1, as in f2-sA and f2-sC). More than a tenth of the inside outside its box is a
        // second disc or ring drawn beside the first, never a seam (the kit self-check below shows both sides).
        if (outsideBox * 10 > inside) {
            return doubt("kit: $outsideBox enclosed px in $outsideRegions regions lie outside the disc's inside ($inside px), more than a tenth of it — a second disc or ring? $context")
        }
        val bw = (maxX - minX + 1).toFloat()
        val bh = (maxY - minY + 1).toFloat()
        if (kotlin.math.abs(bw - bh) > 3f) return doubt("kit: the enclosed pixels span $bw × $bh px — not one disc's inside $context")
        if (!(bw >= 20f * pxPerDp && bw <= 46f * pxPerDp)) return doubt("kit: the disc's inside is ${bw / pxPerDp} dp across $context")
        val rIn = bw / 2f
        if (inside < 0.5 * Math.PI * rIn * rIn) return doubt("kit: only $inside enclosed pixels in a ${bw / pxPerDp} dp box $context")
        val cx = (minX + maxX + 1) / 2f
        val cy = (minY + maxY + 1) / 2f
        val radii = ArrayList<Float>()
        for (k in 0 until 16) {
            val rad = Math.toRadians(k * 22.5)
            var r = rIn - 2f
            var sawRing = false
            while (r <= rIn + 5f * pxPerDp) {
                val a = accentAt(cx + r * kotlin.math.cos(rad).toFloat(), cy + r * kotlin.math.sin(rad).toFloat())
                if (a) sawRing = true
                if (sawRing && !a) break
                r += 0.5f
            }
            if (sawRing && r <= rIn + 5f * pxPerDp) radii += r
        }
        if (radii.size < 8) return doubt("kit: the ring's outer edge was measured on ${radii.size} of 16 rays $context")
        return DiscReading(DrawnDisc(Offset(cx, cy), radii.sorted()[radii.size / 2]), null)
    }

    /**
     * Places [must] at their solution poses through S-2 (a set-up, not the gesture), placing other solution
     * pieces first where the engine needs them (edge-first, as `solveByApi`), and always leaving at least one
     * piece in the tray so the puzzle stays unsolved (the board badge shows only then). Placements are kept per
     * puzzle across opens (S-2), so pieces placed by an earlier call stay.
     */
    private fun setUpBoard(p: PuzzleDefinition, must: Set<PieceId>): Boolean {
        fun placedNow(): Set<PieceId> = kit.placed().map { it.piece }.toSet()
        repeat(p.pieces.size + 1) {
            var progress = false
            for (m in must) {
                val placed = placedNow()
                if (m !in placed && p.pieces.count { it !in placed } > 1 && kit.placeByApi(m)) progress = true
            }
            if (must.all { it in placedNow() }) return true
            val placed = placedNow()
            val helpers = p.pieces.filter { it !in placed && it !in must }
            if (helpers.size > 1) {
                for (h in helpers) if (kit.placeByApi(h)) { progress = true; break }
            }
            if (!progress) return false
        }
        return must.all { it in placedNow() }
    }

    /**
     * Over every bundled puzzle with the PG: the PG placed (S-2), the board badge read from its node, and the
     * solution piece whose shape reaches deepest into the badge area outside the disc placed beside it. Ranked:
     * 0 when the engine lets that piece turn in place (the right outcome is a visible turn), 1 when the PG's
     * mirror locks in place (the wrong outcome is a visible mirror), 2 otherwise (told apart by the shake's piece).
     * *Amended WO-010 (F1 kit half, a corrected check):* a board whose drawn disc cannot be read without doubt
     * ([readDrawnDisc]) is passed over — the puzzle with the PG alone, or that neighbour with the PG — and named in
     * [passedOver]; it no longer stops the search over the other puzzles. The caller fails loudly when no case is left
     * ([badgeOverlapOrFail]).
     * *Amended WO-010 (recirculation 4, delta code review S-1):* the leg calls it with the defaults (every bundled puzzle,
     * [readDrawnDisc], the best rank over all). The kit's self-check gives it a [read] that doubts chosen boards, a shorter
     * [puzzles] list, or [firstCaseOnly] (the first case found, not the best), so that the pass-over can be shown to run.
     */
    private fun findBoardBadgeOverlap(
        passedOver: MutableList<String>,
        puzzles: List<PuzzleDefinition> = kitLibrary.puzzles,
        read: (String, String) -> DiscReading = { captureTag, nodeTag -> readDrawnDisc(captureTag, nodeTag) },
        firstCaseOnly: Boolean = false,
    ): BadgeOverlap? {
        val found = ArrayList<BadgeOverlap>()
        fun passOver(p: PuzzleDefinition, doubt: String?) {
            passedOver += "${p.id} (placed ${kit.placed().map { it.piece }}): $doubt"
            android.util.Log.i("KitScreen", "board badge overlap: passed over ${passedOver.last()}")
        }
        for (p in puzzles) {
            if (firstCaseOnly && found.isNotEmpty()) break
            if (PieceId.PG !in p.pieces) continue
            kit.open(p.id)
            if (!setUpBoard(p, setOf(PieceId.PG))) continue
            settle()
            if (!kit.exists(PlayTags.FLIP_BADGE_BOARD)) continue
            val area = kit.bounds(PlayTags.FLIP_BADGE_BOARD)
            val first = read(PlayTags.BOARD, PlayTags.FLIP_BADGE_BOARD)
            val disc = first.disc
            if (disc == null) { passOver(p, first.doubt); continue }
            val reaching = p.solution.filter { it.piece != PieceId.PG }
                .mapNotNull { sp -> badgeTapPoint(area, disc, sp.polygon.map { kit.toPx(it) })?.let { sp.piece to it.second } }
                .sortedByDescending { it.second }
            for ((n, _) in reaching) {
                if (!setUpBoard(p, setOf(PieceId.PG, n))) continue
                settle()
                if (!kit.exists(PlayTags.FLIP_BADGE_BOARD)) continue
                val again = read(PlayTags.BOARD, PlayTags.FLIP_BADGE_BOARD)
                val discNow = again.disc
                if (discNow == null) { passOver(p, again.doubt); continue }
                badgeTapPoint(kit.bounds(PlayTags.FLIP_BADGE_BOARD), discNow, kit.boardPolygonPx(PlayTags.boardPiece(n))) ?: continue
                val dp = kit.onMain { kit.controller.boardDpPerUnit }
                val placed = kit.placed()
                val q = placed.single { it.piece == n }
                val pg = placed.single { it.piece == PieceId.PG }
                val rank = when {
                    kitEngine.tryTurnInPlace(p, placed - q, q, q.turn.next(), dp) is LockResult.Locked -> 0
                    kitEngine.tryMirrorInPlace(p, placed - pg, pg, dp) is LockResult.Locked -> 1
                    else -> 2
                }
                android.util.Log.i("KitScreen", "board badge overlap: ${p.id} PG + $n, rank $rank, placed ${placed.map { it.piece }}")
                found += BadgeOverlap(p, n, rank)
                break
            }
        }
        return found.minByOrNull { it.rank }
    }

    /** The leg's case, or its loud failure when the search left none, naming the boards it passed over (S-1). */
    private fun badgeOverlapOrFail(case: BadgeOverlap?, passedOver: List<String>): BadgeOverlap =
        case ?: throw AssertionError(
            "kit: no bundled puzzle places another piece's drawn shape inside the board badge's 60 dp area outside its 38 dp disc" +
                if (passedOver.isEmpty()) "" else "; ${passedOver.size} boards passed over, their drawn disc not read without doubt: $passedOver",
        )

    /** The drawn shape is [drawn] and not [other]: the pixels only [drawn] covers are the PG colour, the pixels only [other] covers are not. */
    private fun assertDrawnPose(what: String, drawn: List<Offset>, other: List<Offset>, captureTag: String, avoid: List<Rect>) {
        val capture = kit.capture(captureTag)
        val origin = kit.bounds(captureTag).topLeft
        val colour = PlayPalette.piece(PieceId.PG)
        val onlyDrawn = kitDifferenceSamples(drawn, other, 3f, avoid, within = kit.bounds(captureTag))
        val onlyOther = kitDifferenceSamples(other, drawn, 3f, avoid, within = kit.bounds(captureTag))
        assertTrue("$what: the two poses differ somewhere (${onlyDrawn.size} / ${onlyOther.size} samples)", onlyDrawn.isNotEmpty() && onlyOther.isNotEmpty())
        for (s in onlyDrawn) {
            val c = capture[(s.x - origin.x).toInt(), (s.y - origin.y).toInt()]
            assertTrue("$what: pixel $s inside the expected pose is ${kitHex(c)}, not ${kitHex(colour)}", kitNear(c, colour, 16f / 255f))
        }
        for (s in onlyOther) {
            val c = capture[(s.x - origin.x).toInt(), (s.y - origin.y).toInt()]
            assertFalse("$what: pixel $s of the other pose is painted in the PG colour", kitNear(c, colour, 16f / 255f))
        }
    }

    /** Something is drawn in [area]: pixels that are none of [backgrounds], outside every [exclude] rectangle. */
    private fun assertDrawnSomething(what: String, captureTag: String, area: Rect, backgrounds: List<androidx.compose.ui.graphics.Color>, exclude: List<Rect> = emptyList()) {
        val capture = kit.capture(captureTag)
        val origin = kit.bounds(captureTag).topLeft
        var ink = 0
        for (y in area.top.toInt()..area.bottom.toInt()) for (x in area.left.toInt()..area.right.toInt()) {
            if (exclude.any { it.contains(Offset(x + 0.5f, y + 0.5f)) }) continue
            val cx = (x - origin.x).toInt()
            val cy = (y - origin.y).toInt()
            if (cx !in 0 until capture.width || cy !in 0 until capture.height) continue
            val c = capture[cx, cy]
            if (backgrounds.none { kitNear(c, it, 12f / 255f) }) ink++
        }
        assertTrue("$what: pixels drawn in the badge area ($ink)", ink >= 20)
    }
}
