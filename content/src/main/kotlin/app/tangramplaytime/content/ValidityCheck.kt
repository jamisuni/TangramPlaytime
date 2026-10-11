// The geometric rules of the load-time check (designs/WO-002-design.md §2 `ValidityCheck`; review F2:
// part of TASK-005). Piece shapes and the overlap test come through `ILockEngine` (D1, O-01; decisions.md
// 2026-10-11, "through `ILockEngine`"); only the shared-edge measure is `:content`'s (`Edges`).
package app.tangramplaytime.content

import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.contracts.Turn
import kotlin.math.abs

internal class ValidityCheck(private val engine: ILockEngine) {
    /**
     * The geometric violations of [def], a puzzle whose structural rules hold (the piece set included):
     * 1. `CONGRUENT` — each polygon equals `engine.vertices` of its piece at one of the 8 turns (the PG at
     *    either mirror state), centred on the polygon's corner average, up to cyclic shift and reversal,
     *    each coordinate within 1e-6 (decisions.md 2026-10-11, "congruent to its piece").
     * 2. `OVERLAP` — once every piece has that pose, `engine.stillValid` keeps them all.
     * 3. `CONNECTED` — every piece is reached from the first through shared outline longer than 1e-6.
     * 4. `WARMUP` — in a warm-up, every piece has 1 − shared / perimeter ≥ 0.5 − 1e-6.
     *
     * @param file the source's file name, for the violations.
     * @return every violation found; empty when the puzzle is valid.
     */
    fun violations(file: String, def: PuzzleDefinition): List<Violation> {
        val violations = mutableListOf<Violation>()

        val poses = def.solution.map { piece ->
            pose(piece).also {
                if (it == null) {
                    violations += Violation(
                        file,
                        Rule.CONGRUENT,
                        "${piece.piece} is not a ${piece.piece.type} at any 45° turn with its corners in boundary order: ${piece.polygon}",
                    )
                }
            }
        }
        if (poses.all { it != null }) {
            val placed = poses.filterNotNull()
            val kept = engine.stillValid(def, placed).map { it.piece }.toSet()
            val dropped = placed.map { it.piece }.filter { it !in kept }
            if (dropped.isNotEmpty()) {
                violations += Violation(file, Rule.OVERLAP, "$dropped overlap a piece listed before them")
            }
        }

        val polygons = def.solution.map { it.polygon }
        val reached = reachedFromFirst(polygons)
        if (reached.size != polygons.size) {
            val apart = def.solution.indices.filter { it !in reached }.map { def.solution[it].piece }
            violations += Violation(
                file,
                Rule.CONNECTED,
                "$apart share no edge with the rest (a single shared corner point does not connect)",
            )
        }

        if (def.kind == PuzzleKind.WARMUP) {
            for ((i, piece) in def.solution.withIndex()) {
                val shared = polygons.indices.filter { it != i }.sumOf { Edges.sharedLength(piece.polygon, polygons[it]) }
                val exposure = 1.0 - shared / Edges.perimeter(piece.polygon)
                if (exposure < WARMUP_EXPOSURE - TOL) {
                    violations += Violation(
                        file,
                        Rule.WARMUP,
                        "${piece.piece} shows $exposure of its outline on the silhouette edge, less than half",
                    )
                }
            }
        }
        return violations
    }

    /** The lowest turn (mirror state false first; the PG also tries true) whose corners are [piece]'s polygon. */
    private fun pose(piece: SolvedPiece): Placement? {
        val polygon = piece.polygon
        val centre = Point(polygon.sumOf { it.x } / polygon.size, polygon.sumOf { it.y } / polygon.size)
        val mirrors = if (piece.piece == PieceId.PG) listOf(false, true) else listOf(false)
        for (mirrored in mirrors) {
            for (step in 0..7) {
                val turn = Turn(step)
                if (sameOutline(engine.vertices(piece.piece, turn, mirrored, centre), polygon)) {
                    return Placement(piece.piece, turn, mirrored, centre)
                }
            }
        }
        return null
    }

    /** Whether [b] lists [a]'s corners from some corner in either direction, each coordinate within 1e-6. */
    private fun sameOutline(a: List<Point>, b: List<Point>): Boolean {
        if (a.size != b.size) return false
        val n = a.size
        for (shift in 0 until n) {
            if ((0 until n).all { i -> near(a[i], b[(shift + i) % n]) }) return true
            if ((0 until n).all { i -> near(a[i], b[(shift - i + n) % n]) }) return true
        }
        return false
    }

    private fun near(p: Point, q: Point): Boolean = abs(p.x - q.x) <= TOL && abs(p.y - q.y) <= TOL

    /** The indices reached from the first polygon through shared outline longer than 1e-6. */
    private fun reachedFromFirst(polygons: List<List<Point>>): Set<Int> {
        if (polygons.isEmpty()) return emptySet()
        val reached = mutableSetOf(0)
        val stack = ArrayDeque(listOf(0))
        while (stack.isNotEmpty()) {
            val k = stack.removeLast()
            for (m in polygons.indices) {
                if (m !in reached && Edges.sharedLength(polygons[k], polygons[m]) > TOL) {
                    reached += m
                    stack.addLast(m)
                }
            }
        }
        return reached
    }

    private companion object {
        /** TYPE-004 tolerance for congruence, collinearity and shared length (decisions.md line 50). */
        const val TOL = 1e-6

        /** REQ-041: at least half of every piece's outline; passes at ≥ 0.5 − 1e-6 (decisions.md line 50). */
        const val WARMUP_EXPOSURE = 0.5
    }
}
