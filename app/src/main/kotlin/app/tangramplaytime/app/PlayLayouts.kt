// WO-003 TASK-015 · seams S-5 and S-15 (tasks.md § WO-003 › Seams) · architecture.md O-05: `:app` decides
// the layout and `:play` receives it. The numbers are DI-2 §2–§3's compact values (decisions.md, the
// TASK-010 row "Numbers the tray and the gestures take from the design inputs", item 1).
package app.tangramplaytime.app

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.play.PlayLayout

/** The Play screen's layouts. Only the phone value exists in this WO; the tablet value is WO-006's. */
internal object PlayLayouts {
    /**
     * The phone layout: tray rows `[[LT1, LT2, MT], [SQ, PG, ST1, ST2]]` (TYPE-001 order), a dragged
     * piece floating 30 dp above the finger (REQ-014 ASSUMPTION), an 8 dp cell gap, an 8 dp side margin
     * and a 66 dp top bar.
     */
    val phone: PlayLayout = PlayLayout(
        trayRows = listOf(
            listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
            listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
        ),
        floatOffsetDp = 30f,
        cellGapDp = 8f,
        sideMarginDp = 8f,
        topBarDp = 66f,
    )
}
