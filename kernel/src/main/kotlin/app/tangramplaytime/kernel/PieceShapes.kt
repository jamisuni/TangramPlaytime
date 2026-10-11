// WO-001 TASK-001 · design §2 `PieceShapes` · TYPE-001 shapes, TYPE-003 turns and mirror,
// TYPE-004 "the piece corner's exact offset".
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.Turn
import kotlin.math.sqrt

/**
 * The corner offsets of every piece shape from its centre, for each of the 8 turns and both
 * mirror states. The table is computed once at init, so the drawn corners, the anchors of
 * placed pieces and the locked position all read the very same doubles (TYPE-004: the
 * locked position is the anchor's exact position minus the piece corner's exact offset).
 */
internal object PieceShapes {
    // TYPE-003: one step is 45°. Exact 0 and ±1 at the right angles, never cos(π/2)
    // (design §2 `PieceShapes`, step 3).
    private val H: Double = sqrt(2.0) / 2.0
    private val COS: DoubleArray = doubleArrayOf(1.0, H, 0.0, -H, -1.0, -H, 0.0, H)
    private val SIN: DoubleArray = doubleArrayOf(0.0, H, 1.0, H, 0.0, -H, -1.0, -H)

    /** Indexed [PieceType.ordinal] → mirrored (0 = no, 1 = yes) → [Turn.step]. */
    private val table: List<List<List<List<Point>>>> = PieceType.entries.map { type ->
        listOf(false, true).map { mirrored -> (0..7).map { step -> compute(type, mirrored, step) } }
    }

    /**
     * The corners of a [type] piece at [turn] and [mirrored], relative to its centre (the
     * average of its corners, TYPE-003). Corner i is `Spec/03-puzzle-format.md` §3 corner i,
     * transformed; the winding is not promised. Positive turns are clockwise on screen
     * (y points down). Mirroring changes only [PieceType.PG] (TYPE-003).
     */
    fun offsets(type: PieceType, turn: Turn, mirrored: Boolean): List<Point> =
        table[type.ordinal][if (mirrored) 1 else 0][turn.step]

    /** TYPE-001 shapes at rot 0, in `Spec/03-puzzle-format.md` §3 corner order (y down). */
    private fun local(type: PieceType): List<Point> = when (type) {
        PieceType.LT -> listOf(Point(0.0, 0.0), Point(4.0, 0.0), Point(2.0, 2.0))
        PieceType.MT -> listOf(Point(0.0, 0.0), Point(2.0, 0.0), Point(0.0, 2.0))
        PieceType.ST -> listOf(Point(0.0, 0.0), Point(2.0, 0.0), Point(1.0, 1.0))
        PieceType.SQ -> listOf(Point(0.0, 0.0), Point(1.0, -1.0), Point(2.0, 0.0), Point(1.0, 1.0))
        PieceType.PG -> listOf(Point(0.0, 0.0), Point(2.0, 0.0), Point(3.0, -1.0), Point(1.0, -1.0))
    }

    private fun compute(type: PieceType, mirrored: Boolean, step: Int): List<Point> {
        val corners = local(type)
        // TYPE-003: a turn keeps the centre (the average of the corners) fixed, so the
        // offsets are taken from that average (design §2 `PieceShapes`, step 1).
        val cx = corners.sumOf { it.x } / corners.size
        val cy = corners.sumOf { it.y } / corners.size
        // TYPE-003: only the parallelogram has a mirror image that differs. Mirroring MT
        // would give a 90°-turned pose, a visible effect (design §3 "MT mirror trap").
        val flip = mirrored && type == PieceType.PG
        val c = COS[step]
        val s = SIN[step]
        return corners.map { p ->
            val x = if (flip) -(p.x - cx) else p.x - cx
            val y = p.y - cy
            // Design §2 step 3: x' = x·c − y·s, y' = x·s + y·c. The trailing + 0.0 turns a
            // signed zero (−0.0) into 0.0, so a Point never carries one.
            Point(x * c - y * s + 0.0, x * s + y * c + 0.0)
        }
    }
}
