// WO-001 TASK-001 · design §2 `Polygons` · REQ-019.A2 (no overlap), TYPE-004 tolerance.
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.Point
import kotlin.math.abs

/**
 * Area and convex clipping of polygons given as their corners in puzzle units (y down).
 * The ported prototype method (ADR-001): Sutherland–Hodgman against a convex clipper.
 */
internal object Polygons {
    /** The shoelace sum Σ(xᵢyᵢ₊₁ − xᵢ₊₁yᵢ)/2 over the closed polygon [p]; 0.0 for an empty list. */
    fun signedArea(p: List<Point>): Double {
        var sum = 0.0
        for (i in p.indices) {
            val a = p[i]
            val b = p[(i + 1) % p.size]
            sum += a.x * b.y - b.x * a.y
        }
        return sum / 2.0
    }

    /** The absolute value of [signedArea]. */
    fun area(p: List<Point>): Double = abs(signedArea(p))

    /**
     * Sutherland–Hodgman: [subject] clipped by the **convex** [clipper]. Both are first
     * normalised to positive signed area, because mixed winding would clip to nothing
     * (design §3 "Highest-risk part"). A point P is inside edge AB when
     * cross(B − A, P − A) ≥ −1e-9.
     *
     * @return the clipped polygon's corners; empty when either polygon has fewer than 3
     *   corners (it encloses no area) or the clip leaves nothing.
     */
    fun clipConvex(subject: List<Point>, clipper: List<Point>): List<Point> {
        if (subject.size < 3 || clipper.size < 3) return emptyList()
        val clip = positive(clipper)
        var out = positive(subject)
        for (i in clip.indices) {
            if (out.isEmpty()) break
            val a = clip[i]
            val b = clip[(i + 1) % clip.size]
            val input = out
            val next = ArrayList<Point>(input.size + 2)
            for (j in input.indices) {
                val p = input[j]
                val q = input[(j + 1) % input.size]
                val sp = side(a, b, p)
                val sq = side(a, b, q)
                val pIn = sp >= -LockRules.CMP
                val qIn = sq >= -LockRules.CMP
                if (pIn) next.add(p)
                if (pIn != qIn) {
                    val t = sp / (sp - sq)
                    next.add(Point(p.x + t * (q.x - p.x), p.y + t * (q.y - p.y)))
                }
            }
            out = next
        }
        return out
    }

    /** The area of [clipConvex] (a, b), or 0.0 when the clip has fewer than 3 corners. */
    fun intersectionArea(a: List<Point>, b: List<Point>): Double {
        val clipped = clipConvex(a, b)
        return if (clipped.size < 3) 0.0 else area(clipped)
    }

    /**
     * Whether [a] and [b] overlap: their intersection area exceeds the TYPE-004 tolerance
     * of 1e-6. Pieces that only share an edge clip to about 1e-16 and do not overlap.
     */
    fun overlaps(a: List<Point>, b: List<Point>): Boolean = intersectionArea(a, b) > LockRules.EPS

    private fun positive(p: List<Point>): List<Point> = if (signedArea(p) < 0.0) p.asReversed() else p

    /** cross(B − A, P − A): ≥ 0 when P is on the inner side of edge AB of a positive polygon. */
    private fun side(a: Point, b: Point, p: Point): Double =
        (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
}
