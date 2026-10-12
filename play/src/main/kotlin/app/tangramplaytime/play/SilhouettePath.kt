// WO-003 TASK-012 · the merged silhouette path (tasks.md § WO-003 TASK-012; workorders/WO-003.md
// § Constraints "Draw in pixels (G-17)") · REQ-011: one flat colour, no inner line · REQ-023 A1 / REQ-039 A1:
// TASK-014 clips the solved picture to this same path.
package app.tangramplaytime.play

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import app.tangramplaytime.contracts.SolvedPiece
import kotlin.math.abs

/**
 * The silhouette as one path in `play-screen` px, built from the puzzle's stored solution polygons
 * (`SolvedPiece.polygon`, puzzle units) through a [BoardMapping] — never under a canvas scale (G-17).
 *
 * The polygons are merged so that no edge two solution pieces share is ever rasterised: a seam of
 * anti-aliasing between two separately filled neighbours would read as an inner line (REQ-011 A1, proven
 * on the floor image). The merge is a path union (`Path.op`, Skia path ops) folded over the polygons; if
 * Skia refuses an operation, the fallback is one path holding every polygon as a sub-path, all wound the
 * same way, filled with the non-zero rule — a single fill, so coincident edges cancel instead of seaming.
 */
internal object SilhouettePath {
    /** The merged silhouette of [solution] in `play-screen` px. */
    fun build(solution: List<SolvedPiece>, mapping: BoardMapping): Path {
        val polygons = solution.map { piece -> sameWinding(piece.polygon.map(mapping::toPx)) }
        val combined = Path().apply {
            fillType = PathFillType.NonZero
            polygons.forEach { addPolygon(it) }
        }
        var union: Path? = null
        for (polygon in polygons) {
            val next = Path().apply { addPolygon(polygon) }
            union = if (union == null) {
                next
            } else {
                val out = Path()
                if (!out.op(union, next, PathOperation.Union)) return combined
                out
            }
        }
        val merged = union ?: return combined
        // A path op that "succeeded" but lost area is no silhouette: fall back to the single-fill path.
        val a = merged.getBounds()
        val b = combined.getBounds()
        val sane = abs(a.left - b.left) <= 1f && abs(a.top - b.top) <= 1f && abs(a.right - b.right) <= 1f && abs(a.bottom - b.bottom) <= 1f
        return if (sane) merged else combined
    }

    /** The bounding box of [solution] in `play-screen` px (the `silhouette` node's bounds). */
    fun boundsPx(solution: List<SolvedPiece>, mapping: BoardMapping): Rect {
        val points = solution.flatMap { piece -> piece.polygon.map(mapping::toPx) }
        return Rect(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })
    }

    /** [points] wound clockwise on screen (y down): a positive signed area; the file's order may be either way. */
    private fun sameWinding(points: List<Offset>): List<Offset> {
        var twiceArea = 0f
        for (i in points.indices) {
            val p = points[i]
            val q = points[(i + 1) % points.size]
            twiceArea += p.x * q.y - q.x * p.y
        }
        return if (twiceArea < 0f) points.asReversed() else points
    }

    private fun Path.addPolygon(points: List<Offset>) {
        if (points.isEmpty()) return
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        close()
    }
}
