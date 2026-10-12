// Acceptance kit (WO-003, TASK-011): the landing preview (tasks.md § WO-003 › Seams S-2 `updatePreview` /
// `drop`, the tag table's `landing-preview` row). Every drag is by touch on `play-screen`; the preview's
// keys are read against its box through the engine's corners before the drawing is blamed.
package app.tangramplaytime.play

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
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
import kotlin.math.cos
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class PreviewKitTest {

    @get:Rule
    val rule = createComposeRule()

    private val kit by lazy { KitScreen(rule) }

    @Test // REQ-021.A1
    fun releasingThePieceAlwaysLocksItWhereThePreviewWasDrawn() {
        // REQ-021 A1: "Releasing the piece always locks it where the preview was drawn." Rule: "The preview uses
        // the same search as the drop (TYPE-004), so it never disagrees with the lock." Oracle, by touch, for
        // three pieces at two nudged positions each: while the piece is held, `landing-preview` carries a
        // piece, turn, mirror and centre and its box is that pose's corner box; after the release and the
        // 180 ms glide, `board-piece` carries exactly the preview's piece, turn, mirror and centre.
        // The moving release (code review N3; the corrected check of this test, whose drops all lifted a still
        // finger): with the main clock paused and the preview drawn at X, the finger's last move and its lift
        // arrive in one input batch with no frame between them, at a lift point where the engine's own search
        // finds another lock or none. "Always" means the piece still locks at X, the preview that was drawn.
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val dp = kit.onMain { kit.controller.boardDpPerUnit }
        var drops = 0
        for (sp in p.solution.sortedBy { s -> s.polygon.maxOf { it.y } }) {
            if (drops >= 3) break
            val piece = sp.piece
            val pose = kitPoseOf(piece, sp.polygon)
            if (pose.mirrored) continue
            for (nudge in listOf(Point(0.2, 0.15), Point(-0.25, 0.1))) {
                val centre = Point(pose.centre.x + nudge.x, pose.centre.y + nudge.y)
                if (kitEngine.findLock(p, kit.placed(), piece, pose.turn, false, centre, dp) !is LockResult.Locked) continue
                kit.tapTray(piece, turnsNeeded(piece, pose.turn))
                var finger = kit.pickUpFromTray(piece)
                finger = kit.steerDragTo(piece, centre, finger)
                kit.checkOverBoard(finger)
                val preview = PlayTags.LANDING_PREVIEW
                assertTrue("$piece held at $centre: a preview is drawn", kit.exists(preview))
                val previewPiece = kit.key(preview, PlaySemantics.PieceKey)
                val previewTurn = kit.key(preview, PlaySemantics.TurnKey)
                val previewMirrored = kit.key(preview, PlaySemantics.MirroredKey)
                val previewCentre = kit.key(preview, PlaySemantics.CentreKey)
                assertEquals("the preview is for the dragged piece", piece, previewPiece)
                kitAssertBoxNear("$piece: preview box vs its corners", kitBox(kit.boardPolygonPx(preview)), kit.bounds(preview), 2f)
                kit.release()
                kit.advance(250)
                val tag = PlayTags.boardPiece(piece)
                assertEquals("$piece locked after the release", listOf(tag), kit.homesOf(piece))
                assertEquals("$piece: locked turn is the preview's", previewTurn, kit.key(tag, PlaySemantics.TurnKey))
                assertEquals("$piece: locked mirror is the preview's", previewMirrored, kit.key(tag, PlaySemantics.MirroredKey))
                val locked = kit.key(tag, PlaySemantics.CentreKey)
                kitAssertNear("$piece: locked centre x is the preview's", previewCentre.x, locked.x, KIT_TOL)
                kitAssertNear("$piece: locked centre y is the preview's", previewCentre.y, locked.y, KIT_TOL)
                val placement = kit.placed().single { it.piece == piece }
                kitAssertNear("$piece: the controller's placement is where the preview was (x)", previewCentre.x, placement.centre.x, KIT_TOL)
                kitAssertNear("$piece: the controller's placement is where the preview was (y)", previewCentre.y, placement.centre.y, KIT_TOL)
                drops++
                // Back to the tray for the next nudge: a drag from the board released on the tray goes home.
                var f = kit.pickUpFromBoard(piece)
                f = kit.moveFinger(f, kit.bounds(PlayTags.trayCell(piece)).center, 4)
                kit.release()
                kit.advance(250)
                assertEquals("$piece back in the tray", listOf(PlayTags.trayPiece(piece)), kit.homesOf(piece))
            }
        }
        assertTrue("the fixture made at least three previewed drops ($drops)", drops >= 3)

        // The moving release (N3). Every piece is back in the tray; the first piece of the same order that locks
        // from its nudged centre is held there until the preview is drawn at X.
        val (piece, pose) = p.solution.sortedBy { s -> s.polygon.maxOf { it.y } }
            .map { it.piece to kitPoseOf(it.piece, it.polygon) }
            .first { (q, qp) -> !qp.mirrored && kitEngine.findLock(p, kit.placed(), q, qp.turn, false, nudged(qp.centre), dp) is LockResult.Locked }
        kit.tapTray(piece, turnsNeeded(piece, pose.turn))
        var finger = kit.pickUpFromTray(piece)
        finger = kit.steerDragTo(piece, nudged(pose.centre), finger)
        kit.checkOverBoard(finger)
        val preview = PlayTags.LANDING_PREVIEW
        assertTrue("$piece held at ${nudged(pose.centre)}: a preview is drawn", kit.exists(preview))
        val drawnAt = Placement(
            kit.key(preview, PlaySemantics.PieceKey), kit.key(preview, PlaySemantics.TurnKey),
            kit.key(preview, PlaySemantics.MirroredKey), kit.key(preview, PlaySemantics.CentreKey),
        )
        val dragTag = PlayTags.dragPiece(piece)
        val held = kit.key(dragTag, PlaySemantics.CentreKey)
        val lift = liftPointAway(p, piece, kit.key(dragTag, PlaySemantics.TurnKey), kit.key(dragTag, PlaySemantics.MirroredKey), held, finger, drawnAt, dp)
        // One batch: the move to the lift point and the lift at the same event time — the main clock does not
        // advance between them, so no frame (and no preview update) falls between the move and the release.
        kit.touch { moveTo(lift.finger); up() }
        kit.frames()
        kit.advance(250)
        val homes = kit.homesOf(piece)
        val landed = kit.placed().singleOrNull { it.piece == piece }
        val boardTag = PlayTags.boardPiece(piece)
        val keysAtX = homes == listOf(boardTag) &&
            kit.key(boardTag, PlaySemantics.TurnKey) == drawnAt.turn &&
            kit.key(boardTag, PlaySemantics.MirroredKey) == drawnAt.mirrored &&
            kitSamePlacement(Placement(piece, drawnAt.turn, drawnAt.mirrored, kit.key(boardTag, PlaySemantics.CentreKey)), drawnAt)
        assertTrue(
            "N3 — REQ-021 A1 \"Releasing the piece always locks it where the preview was drawn\": the preview was drawn at " +
                "$drawnAt; the finger moved ${"%.1f".format((lift.finger - finger).getDistance() / kit.density)} dp and lifted " +
                "in the same frame (the engine's search from the lift point: ${lift.own}); the piece ended " +
                (if (landed == null) "not locked (homes $homes)" else "locked at $landed") + ", not at the drawn preview",
            keysAtX && kitSamePlacement(landed, drawnAt),
        )
    }

    // ── helpers of the moving release (N3) ─────────────────────────────────────────────────────────────

    private fun nudged(centre: Point): Point = Point(centre.x + 0.2, centre.y + 0.15)

    private class Lift(val centre: Point, val finger: Offset, val own: LockResult)

    /**
     * The nearest drawn-centre point to [held] (rings 0.1 unit apart, every 15°, out to 2 units) whose own lock
     * (the engine's search, TYPE-004) is not [drawnAt] — another lock, or none — with the finger that puts the
     * piece there (the piece follows the finger at a constant offset) still well inside the board, so the
     * release is over the board and only the search position differs.
     */
    private fun liftPointAway(
        p: PuzzleDefinition, piece: PieceId, turn: Turn, mirrored: Boolean, held: Point, finger: Offset, drawnAt: Placement, dp: Double,
    ): Lift {
        val inside = kit.bounds(PlayTags.BOARD).deflate(kit.dp(32f))
        val placed = kit.placed()
        var r = 0.1
        while (r <= 2.0 + 1e-9) {
            for (deg in 0 until 360 step 15) {
                val rad = Math.toRadians(deg.toDouble())
                val c = Point(held.x + r * cos(rad), held.y + r * sin(rad))
                val f = finger + (kit.toPx(c) - kit.toPx(held))
                if (!inside.contains(f)) continue
                val own = kitEngine.findLock(p, placed, piece, turn, mirrored, c, dp)
                if (own is LockResult.None || (own is LockResult.Locked && !kitSamePlacement(own.placement, drawnAt))) return Lift(c, f, own)
            }
            r += 0.1
        }
        throw AssertionError("kit: within 2 units of $held every lift point over the board locks $piece at $drawnAt")
    }

    @Test // REQ-021.A2
    fun withNoValidPositionInReachNoPreviewIsDrawnAndTheDropGoesHome() {
        // REQ-021 A2: "With no valid position in reach, no preview is drawn and the drop goes home (REQ-020)."
        // Oracle, by touch: the piece held in the middle of the silhouette where the engine's search finds
        // nothing (the fixture reads the geometry) — no `landing-preview` node, and no preview-stroke pixel
        // inside the silhouette away from the dragged piece; then the release returns the miniature to its cell.
        // The same from a finger outside the board.
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val piece = PieceId.MT
        val cell = kit.bounds(PlayTags.trayCell(piece))
        val trayTurn = kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey)
        val middle = kitMiddleWithNoLock(p, piece, trayTurn, false, kit.onMain { kit.controller.boardDpPerUnit })
        var finger = kit.pickUpFromTray(piece)
        finger = kit.steerDragTo(piece, middle, finger)
        kit.frames()
        assertFalse("no `landing-preview` with the piece in the middle", kit.exists(PlayTags.LANDING_PREVIEW))
        assertNoPreviewStroke(p, piece)
        kit.release()
        kit.advance(250)
        assertHome("a drop in the middle", piece, cell)

        finger = kit.pickUpFromTray(piece)
        finger = kit.moveFinger(finger, Offset(kit.bounds(PlayTags.SCREEN).center.x, kit.dp(kitPhoneLayout.topBarDp) / 2), 5)
        assertFalse("the finger is outside the board", kit.bounds(PlayTags.BOARD).contains(finger))
        assertFalse("no `landing-preview` with the finger outside the board", kit.exists(PlayTags.LANDING_PREVIEW))
        kit.release()
        kit.advance(250)
        assertHome("a drop outside the board", piece, cell)
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    private fun turnsNeeded(piece: PieceId, target: Turn): Int {
        val now = kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey)
        return ((target.step - now.step) % 8 + 8) % 8
    }

    private fun assertHome(what: String, piece: PieceId, cell: androidx.compose.ui.geometry.Rect) {
        assertEquals("$what: $piece has one home", listOf(PlayTags.trayPiece(piece)), kit.homesOf(piece))
        val box = kit.bounds(PlayTags.trayPiece(piece))
        assertTrue("$what: $piece's miniature $box is inside its cell $cell", cell.contains(box.topLeft) && cell.contains(box.bottomRight))
    }

    /** No pixel of the preview stroke colour inside the silhouette, outside the dragged piece's box grown by 16 px. */
    private fun assertNoPreviewStroke(p: PuzzleDefinition, piece: PieceId) {
        val board = kit.capture(PlayTags.BOARD)
        val origin = kit.bounds(PlayTags.BOARD).topLeft
        val dragBox = kit.bounds(PlayTags.dragPiece(piece)).translate(-origin).inflate(16f)
        val deep = kit.silhouetteMask(p).erode(3)
        var hits = 0
        var first = ""
        deep.forEachSet { x, y ->
            if (dragBox.contains(Offset(x.toFloat(), y.toFloat()))) return@forEachSet
            if (kitNear(board[x, y], PlayPalette.previewStroke, 24f / 255f)) { hits++; if (first.isEmpty()) first = "($x, $y)" }
        }
        assertEquals("preview-stroke pixels inside the silhouette with nothing in reach; first at $first", 0, hits)
    }
}
