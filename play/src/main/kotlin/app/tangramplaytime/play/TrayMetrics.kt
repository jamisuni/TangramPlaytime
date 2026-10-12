// WO-003 TASK-010 · seam S-6 (tasks.md § WO-003 › Seams) · REQ-013's rules verbatim; the row and cell
// placement follows DI-2 §3 / DI-1 `layout()` (TASK-010's recorded decisions). Screen arithmetic only (O-01).
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import kotlin.math.max
import kotlin.math.min

/**
 * The tray's geometry for one screen size, tray-local dp.
 *
 * @property scaleDpPerUnit the miniatures' common scale (REQ-013), the same for every puzzle.
 * @property heightDp the tray strip's height.
 * @property cells one cell per piece of the puzzle, in the layout's rows.
 */
internal data class TrayGeometry(
    val scaleDpPerUnit: Float,
    val heightDp: Float,
    val cells: Map<PieceId, CellRect>,
)

/** One tray cell, tray-local dp: x from the strip's left edge, y from its top. */
internal data class CellRect(val xDp: Float, val yDp: Float, val widthDp: Float, val heightDp: Float)

/**
 * The REQ-013 tray arithmetic: one common scale, computed for the full set, and square cells sized to
 * each piece's turn diameter so the tray never reflows when a piece is turned in it.
 */
internal object TrayMetrics {
    /** REQ-013 rule: "Cell width = the piece's turn diameter × scale + 12 dp padding". */
    private const val PADDING_DP = 12f

    /** REQ-013 rule: the smallest row height limit, "max(84 dp, 16 % of the screen height)". */
    private const val ROW_LIMIT_MIN_DP = 84f
    private const val ROW_LIMIT_SHARE = 0.16f

    /** REQ-013 rule: "turn diameters in units: large triangle 4.22, parallelogram 3.16, medium triangle 2.98, small triangle 2.11, square 2.00". */
    private fun turnDiameter(type: PieceType): Float = when (type) {
        PieceType.LT -> 4.22f
        PieceType.PG -> 3.16f
        PieceType.MT -> 2.98f
        PieceType.ST -> 2.11f
        PieceType.SQ -> 2.00f
    }

    /**
     * Lays the tray out for a screen composable of [widthDp] × [heightDp].
     *
     * The scale is the largest at which every row of the full set ([PlayLayout.trayRows]) fits the width
     * — side margins, a gap before, between and after the cells, 12 dp padding per cell — and each row is
     * at most max(84 dp, 16 % of [heightDp]) tall; it is computed for the full set whatever [pieces]
     * holds (REQ-013 rules; DI-2 §3 item 4). [pieces] then take their cells in the layout's rows, empty
     * rows dropped (REQ-012 rule); each row is centred in the width, its cells vertically centred in the
     * row, which is as tall as its tallest cell.
     *
     * @param pieces the puzzle's pieces (every cell of the puzzle, placed or not).
     */
    fun layout(widthDp: Float, heightDp: Float, layout: PlayLayout, pieces: Set<PieceId>): TrayGeometry {
        val fullRows = layout.trayRows.filter { it.isNotEmpty() }
        require(fullRows.isNotEmpty()) { "PlayLayout.trayRows has no row" }
        val gap = layout.cellGapDp
        val trayWidth = widthDp - 2 * layout.sideMarginDp
        val rowLimit = max(ROW_LIMIT_MIN_DP, ROW_LIMIT_SHARE * heightDp)

        var scale = Float.POSITIVE_INFINITY
        for (row in fullRows) {
            val diameters = row.map { turnDiameter(it.type) }
            val byWidth = (trayWidth - gap * (row.size + 1) - PADDING_DP * row.size) / diameters.sum()
            val byHeight = (rowLimit - PADDING_DP) / diameters.max()
            scale = min(scale, min(byWidth, byHeight))
        }
        scale = max(scale, 0f)

        fun cellSize(id: PieceId): Float = turnDiameter(id.type) * scale + PADDING_DP

        val rows = fullRows.map { row -> row.filter { it in pieces } }.filter { it.isNotEmpty() }
        val rowHeights = rows.map { row -> row.maxOf(::cellSize) }
        val trayHeight = rowHeights.sum() + gap * (rows.size + 1)

        val cells = LinkedHashMap<PieceId, CellRect>()
        var y = gap
        for ((index, row) in rows.withIndex()) {
            val rowHeight = rowHeights[index]
            val sizes = row.map(::cellSize)
            val rowWidth = sizes.sum() + gap * (row.size - 1)
            var x = layout.sideMarginDp + (trayWidth - rowWidth) / 2
            for ((i, id) in row.withIndex()) {
                val size = sizes[i]
                cells[id] = CellRect(xDp = x, yDp = y + (rowHeight - size) / 2, widthDp = size, heightDp = size)
                x += size + gap
            }
            y += rowHeight + gap
        }
        return TrayGeometry(scaleDpPerUnit = scale, heightDp = trayHeight, cells = cells)
    }
}
