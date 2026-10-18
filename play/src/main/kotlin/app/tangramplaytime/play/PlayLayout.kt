// WO-003 TASK-010 · seam S-5 (tasks.md § WO-003 › Seams) · architecture.md O-05: `:app` decides the layout,
// `:play` receives it and never the window size.
// WO-006 TASK-029 · seam S-31 (tasks.md § WO-006 › Seams): `PlayInsets` and `PlayLayout`'s `insets` and `keyboardDp`,
// both defaulted, so every existing literal compiles and means what it meant (seam note 1) · REQ-035 rule 3, REQ-036
// rule 2 (edge to edge; every control respects the insets) · decisions.md 2026-10-17 "Edge to edge, the insets and the
// keyboard, as drawn" · O-05: the insets are part of the layout `:app` decides; `:play` still never receives the window size.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId

/**
 * The system bars and the display cutout around the Play screen, dp per side (S-31; REQ-035 rule 3, REQ-036 rule 2).
 * Never the soft keyboard: that is [PlayLayout.keyboardDp].
 */
data class PlayInsets(val topDp: Float, val bottomDp: Float, val leftDp: Float, val rightDp: Float) {
    companion object {
        /** No insets: the safe area is the whole screen, and the screen is WO-005's (seam note 1). */
        val NONE = PlayInsets(0f, 0f, 0f, 0f)
    }
}

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
 * @property insets the system bars and the display cutout, dp per side — never the keyboard (S-31). Every control lies
 *   inside the screen minus these (S-32's safe area).
 * @property keyboardDp the soft keyboard's bottom inset in dp, 0 while it is closed (S-31). Only the DEV sheet's content
 *   reads it (S-32 "The keyboard moves nothing else").
 */
data class PlayLayout(
    val trayRows: List<List<PieceId>>,
    val floatOffsetDp: Float,
    val cellGapDp: Float,
    val sideMarginDp: Float,
    val topBarDp: Float,
    val insets: PlayInsets = PlayInsets.NONE,
    val keyboardDp: Float = 0f,
)
