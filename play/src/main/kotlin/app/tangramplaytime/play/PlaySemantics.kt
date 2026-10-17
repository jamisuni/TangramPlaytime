// WO-003 TASK-012 · seam S-14 (tasks.md § WO-003 › Seams; the tag table's Keys column) · the semantics
// keys both device kits read with `config.getOrNull`, and the board's px ↔ puzzle-unit mapping.
// WO-004 TASK-020 · seam S-14Δ (designs/WO-004-design.md §2): `RatingKey`. The overview cells reuse
// `PuzzleIdKey` and `PuzzleStateKey` (TASK-021).
package app.tangramplaytime.play

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Turn

/**
 * The board's placement on the screen (S-14): puzzle units ↔ px of the `play-screen` node's own
 * coordinates. The unit point (0, 0) lies at ([originXPx], [originYPx]); one unit is [pxPerUnit] px.
 *
 * @property originXPx the x of unit 0 in `play-screen` px.
 * @property originYPx the y of unit 0 in `play-screen` px.
 * @property pxPerUnit the board scale, px per puzzle unit (the controller's `boardDpPerUnit` × density).
 */
data class BoardMapping(val originXPx: Float, val originYPx: Float, val pxPerUnit: Float) {
    /** [p] in `play-screen` px. */
    fun toPx(p: Point): Offset = Offset(originXPx + (p.x * pxPerUnit).toFloat(), originYPx + (p.y * pxPerUnit).toFloat())

    /** The `play-screen` px point [o] in puzzle units. */
    fun toUnits(o: Offset): Point = Point((o.x - originXPx).toDouble() / pxPerUnit, (o.y - originYPx).toDouble() / pxPerUnit)
}

/**
 * The semantics keys of the Play screen's nodes (S-14; the tag table). A node's keys follow the drawing
 * in each frame: `CentreKey` and `DrawnPxPerUnitKey` say where and at what scale the shape is drawn now.
 */
object PlaySemantics {
    /** The piece a tray, board, drag, ghost or preview node is for. */
    val PieceKey: SemanticsPropertyKey<PieceId> = SemanticsPropertyKey("PlayPiece")

    /** The turn the shape is drawn at (TYPE-003). */
    val TurnKey: SemanticsPropertyKey<Turn> = SemanticsPropertyKey("PlayTurn")

    /** Whether the shape is drawn mirrored (TYPE-003). */
    val MirroredKey: SemanticsPropertyKey<Boolean> = SemanticsPropertyKey("PlayMirrored")

    /** The centre the shape is drawn at in this frame, in puzzle units (board, drag, preview, marker nodes). */
    val CentreKey: SemanticsPropertyKey<Point> = SemanticsPropertyKey("PlayCentre")

    /** The scale the shape is drawn at in this frame, px per puzzle unit (tray, board and drag nodes). */
    val DrawnPxPerUnitKey: SemanticsPropertyKey<Float> = SemanticsPropertyKey("PlayDrawnPxPerUnit")

    /** The board's mapping, on the `board` node, in `play-screen` px. */
    val BoardMappingKey: SemanticsPropertyKey<BoardMapping> = SemanticsPropertyKey("PlayBoardMapping")

    /** The open puzzle's id, on `play-screen`. */
    val PuzzleIdKey: SemanticsPropertyKey<String> = SemanticsPropertyKey("PlayPuzzleId")

    /** The open puzzle's TYPE-006 state, on `play-screen`. */
    val PuzzleStateKey: SemanticsPropertyKey<PuzzleState> = SemanticsPropertyKey("PlayPuzzleState")

    /** The open puzzle's rating, 1..5 (S-14Δ; REQ-024 rule "its difficulty dots"), on `rating`. */
    val RatingKey: SemanticsPropertyKey<Int> = SemanticsPropertyKey("PlayRating")

    /** Receiver extension for [PieceKey]. */
    var SemanticsPropertyReceiver.piece: PieceId by PieceKey

    /** Receiver extension for [TurnKey]. */
    var SemanticsPropertyReceiver.turn: Turn by TurnKey

    /** Receiver extension for [MirroredKey]. */
    var SemanticsPropertyReceiver.mirrored: Boolean by MirroredKey

    /** Receiver extension for [CentreKey]. */
    var SemanticsPropertyReceiver.centre: Point by CentreKey

    /** Receiver extension for [DrawnPxPerUnitKey]. */
    var SemanticsPropertyReceiver.drawnPxPerUnit: Float by DrawnPxPerUnitKey

    /** Receiver extension for [BoardMappingKey]. */
    var SemanticsPropertyReceiver.boardMapping: BoardMapping by BoardMappingKey

    /** Receiver extension for [PuzzleIdKey]. */
    var SemanticsPropertyReceiver.puzzleId: String by PuzzleIdKey

    /** Receiver extension for [PuzzleStateKey]. */
    var SemanticsPropertyReceiver.puzzleState: PuzzleState by PuzzleStateKey

    /** Receiver extension for [RatingKey]. */
    var SemanticsPropertyReceiver.rating: Int by RatingKey
}
