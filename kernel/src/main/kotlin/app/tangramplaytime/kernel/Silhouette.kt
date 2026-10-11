// WO-001 TASK-001 · design §2 `Silhouette` · REQ-019 rule (anchors are the outline corners),
// REQ-019.A2 (never sticks out), REQ-019.A3 (no hidden anchors), TYPE-004 tolerance.
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.SolvedPiece
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * The puzzle's silhouette, seen through its stored solution: the "inside" test and the
 * outline corners (the REQ-019 anchors that come from the puzzle itself).
 */
internal object Silhouette {
    private const val TWO_PI: Double = 2.0 * PI

    /**
     * Whether [poly] lies completely inside the silhouette of [solution] (REQ-019.A2,
     * TYPE-004): Σ intersectionArea(poly, s.polygon) ≥ area(poly) − 1e-6.
     *
     * The silhouette is non-convex and may have holes, so it is never used as a clipper;
     * the sum of clips against each solution piece works because those pieces are convex
     * and interior-disjoint (design §3 "Highest-risk part").
     */
    fun contains(poly: List<Point>, solution: List<SolvedPiece>): Boolean {
        var covered = 0.0
        for (s in solution) covered += Polygons.intersectionArea(poly, s.polygon)
        return covered >= Polygons.area(poly) - LockRules.EPS
    }

    /**
     * The silhouette's outline corners: the points of its boundary (outer edge or hole)
     * where the boundary turns (`ILockEngine.outlineCorners`; REQ-019 rule). Each corner is
     * returned once, as the first-seen solution vertex, in first-seen order.
     *
     * A solution vertex p is classified by the **contiguity of the directions** the
     * solution pieces cover around it, not by their angle sum (design §2 `Silhouette`;
     * review F1; `design-inputs.md` §2 item 13 supersedes the angle-sum rule): it is
     * **not** a corner when the covered total Σ ≥ 2π − 1e-6 (interior), or when there is
     * exactly one uncovered gap and |Σ − π| ≤ 1e-6 (a straight side). Every other vertex is
     * a corner — including 180° covered in two separate wedges (`shapes-warmup-4` (2,2)).
     */
    fun outlineCorners(solution: List<SolvedPiece>): List<Point> {
        val polys = solution.map { it.polygon }
        // Design §2 `Silhouette` step 1: the distinct vertices, merged within 1e-6, first-seen order.
        val distinct = ArrayList<Point>()
        for (q in polys) {
            for (p in q) {
                if (distinct.none { near(it, p) }) distinct.add(p)
            }
        }
        return distinct.filter { isCorner(it, polys) }
    }

    /** One arc of directions around a point: from [start] (in [0, 2π)) over [length] radians. */
    private class Arc(val start: Double, val length: Double)

    private fun isCorner(p: Point, polys: List<List<Point>>): Boolean {
        // Design §2 `Silhouette` step 2: one covered arc per polygon that touches p.
        val arcs = polys.mapNotNull { coveredArc(p, it) }
        // Step 3: the total covered angle and the uncovered gaps between the sorted arcs.
        val sum = arcs.sumOf { it.length }
        // Step 4: the full circle is covered, so p is inside the silhouette.
        if (sum >= TWO_PI - LockRules.EPS) return false
        val sorted = arcs.sortedBy { it.start }
        var gaps = 0
        for (k in sorted.indices) {
            val cur = sorted[k]
            val next = sorted[(k + 1) % sorted.size]
            val gap = normalise(next.start - (cur.start + cur.length))
            // A gap of ~0 (or ~2π after the wrap of a tiny negative) is two touching arcs.
            if (gap > LockRules.EPS && gap < TWO_PI - LockRules.EPS) gaps++
        }
        // Step 4: one contiguous half-plane covered is a straight side, not a corner.
        val straightSide = gaps == 1 && abs(sum - PI) <= LockRules.EPS
        return !straightSide
    }

    /**
     * The arc of directions from [p] that polygon [q] covers, or null when [q] does not
     * touch [p]. When p is a vertex of q (within 1e-6), the two edge directions are towards
     * its neighbouring corners; when p lies strictly inside an edge, they are towards that
     * edge's two ends (a half-plane). The covered arc is the one of the two arcs between
     * those directions that holds the direction to q's centre (design §2 `Silhouette` step 2).
     */
    private fun coveredArc(p: Point, q: List<Point>): Arc? {
        val n = q.size
        if (n < 3) return null
        val i = q.indexOfFirst { near(it, p) }
        val u: Point
        val w: Point
        if (i >= 0) {
            u = q[(i - 1 + n) % n]
            w = q[(i + 1) % n]
        } else {
            val k = q.indices.firstOrNull { strictlyInsideEdge(p, q[it], q[(it + 1) % n]) } ?: return null
            u = q[k]
            w = q[(k + 1) % n]
        }
        val centre = Point(q.sumOf { it.x } / n, q.sumOf { it.y } / n)
        val d1 = direction(p, u)
        val d2 = direction(p, w)
        val dc = direction(p, centre)
        val span = normalise(d2 - d1)
        return if (normalise(dc - d1) <= span) Arc(d1, span) else Arc(d2, normalise(d1 - d2))
    }

    /** Port of `tangram_geom._on_segment`: on line ab (|cross| ≤ 1e-9) and strictly between a and b. */
    private fun strictlyInsideEdge(p: Point, a: Point, b: Point): Boolean {
        val ex = b.x - a.x
        val ey = b.y - a.y
        val cross = ex * (p.y - a.y) - ey * (p.x - a.x)
        if (abs(cross) > LockRules.CMP) return false
        val dot = (p.x - a.x) * ex + (p.y - a.y) * ey
        return dot > LockRules.CMP && dot < ex * ex + ey * ey - LockRules.CMP
    }

    /** The direction from [from] to [to] in [0, 2π) (`atan2` returns (−π, π], design §3). */
    private fun direction(from: Point, to: Point): Double = normalise(atan2(to.y - from.y, to.x - from.x))

    /** [a] reduced to [0, 2π). */
    private fun normalise(a: Double): Double {
        var r = a % TWO_PI
        if (r < 0.0) r += TWO_PI
        // A tiny negative r can round up to exactly 2π when 2π is added.
        if (r >= TWO_PI) r -= TWO_PI
        return r
    }

    /** Coincidence of two points, per coordinate within the TYPE-004 tolerance of 1e-6. */
    private fun near(a: Point, b: Point): Boolean =
        abs(a.x - b.x) <= LockRules.EPS && abs(a.y - b.y) <= LockRules.EPS
}
