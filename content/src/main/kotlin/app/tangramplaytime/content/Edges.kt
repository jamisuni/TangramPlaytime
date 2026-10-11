// The shared-edge measure of the load-time check (designs/WO-002-design.md §2 `Edges`): REQ-038's
// "connected through shared edges" (a single shared corner point does not connect) and REQ-041's
// half-outline measure. No `ILockEngine` member measures shared boundary (decisions.md 2026-10-11).
package app.tangramplaytime.content

import app.tangramplaytime.contracts.Point
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal object Edges {
    /** A point within this distance of a side's line lies on it (TYPE-004 tolerance; decisions.md line 50). */
    private const val TOL = 1e-6

    /**
     * The length of outline [a] and [b] share: for every side of [a] and every side of [b] whose two ends
     * lie within 1e-6 of that side's line, the overlap of their projections onto it. Two polygons that
     * touch only at a corner share 0.
     */
    fun sharedLength(a: List<Point>, b: List<Point>): Double {
        var total = 0.0
        for (i in a.indices) {
            val a1 = a[i]
            val a2 = a[(i + 1) % a.size]
            val length = hypot(a2.x - a1.x, a2.y - a1.y)
            if (length == 0.0) continue // a repeated corner has no side to share
            val ux = (a2.x - a1.x) / length
            val uy = (a2.y - a1.y) / length
            for (j in b.indices) {
                val b1 = b[j]
                val b2 = b[(j + 1) % b.size]
                val off1 = abs(ux * (b1.y - a1.y) - uy * (b1.x - a1.x))
                val off2 = abs(ux * (b2.y - a1.y) - uy * (b2.x - a1.x))
                if (off1 > TOL || off2 > TOL) continue
                val t1 = (b1.x - a1.x) * ux + (b1.y - a1.y) * uy
                val t2 = (b2.x - a1.x) * ux + (b2.y - a1.y) * uy
                val lo = max(0.0, min(t1, t2))
                val hi = min(length, max(t1, t2))
                if (hi > lo) total += hi - lo
            }
        }
        return total
    }

    /** The length of [p]'s outline. */
    fun perimeter(p: List<Point>): Double =
        p.indices.sumOf { i ->
            val next = p[(i + 1) % p.size]
            hypot(next.x - p[i].x, next.y - p[i].y)
        }
}
