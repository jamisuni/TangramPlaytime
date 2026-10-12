// WO-003 TASK-010 · seam S-5 (tasks.md § WO-003 › Seams) · architecture.md O-05: `:app` decides the layout,
// `:play` receives it and never the window size.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId

/**
 * The layout values `:app` hands the Play screen (O-05; the phone value lives in `:app`, S-15; a tablet
 * value is WO-006's).
 *
 * @property trayRows the tray's rows in TYPE-001 order, for the full set; a puzzle with fewer pieces
 *   keeps only its own pieces in these rows and drops an empty row (REQ-012 rule).
 * @property floatOffsetDp how far a dragged piece's lowest point floats above the finger (REQ-014 ASSUMPTION).
 * @property cellGapDp the gap between tray cells and between tray rows (DI-2 §3).
 * @property sideMarginDp the margin at the tray's left and right edges (DI-2 §3).
 * @property topBarDp the top bar's height (DI-2 §2).
 */
data class PlayLayout(
    val trayRows: List<List<PieceId>>,
    val floatOffsetDp: Float,
    val cellGapDp: Float,
    val sideMarginDp: Float,
    val topBarDp: Float,
)
