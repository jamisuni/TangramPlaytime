// WO-003 TASK-010 · seam S-9 (tasks.md § WO-003 › Seams) · TYPE-001 resting turns · decisions.md 2026-10-11
// ("The TYPE-001 resting turns live once, in `:play`").
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.contracts.Turn

/**
 * How every piece rests in the tray (TYPE-001: "long side down and pointing up for every triangle,
 * upright for the square, and leaning right for the parallelogram"), as TYPE-003 steps of the
 * `Spec/03-puzzle-format.md` §3 reference shapes — the one copy of these values. A resting piece is
 * never mirrored.
 */
internal object TrayRest {
    /** The resting turn of [piece]: triangles of the LT and ST shapes `Turn(4)`, MT and SQ `Turn(1)`, PG `Turn(0)`. */
    fun turn(piece: PieceId): Turn = when (piece.type) {
        PieceType.LT, PieceType.ST -> Turn(4)
        PieceType.MT, PieceType.SQ -> Turn(1)
        PieceType.PG -> Turn(0)
    }
}
