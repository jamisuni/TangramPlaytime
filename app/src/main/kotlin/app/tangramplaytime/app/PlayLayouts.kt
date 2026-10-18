// WO-003 TASK-015 · seams S-5 and S-15 (tasks.md § WO-003 › Seams) · architecture.md O-05: `:app` decides
// the layout and `:play` receives it. The numbers are DI-2 §2–§3's compact values (decisions.md, the
// TASK-010 row "Numbers the tray and the gestures take from the design inputs", item 1).
// WO-006 TASK-031 · seam S-35 step 3 (tasks.md § WO-006 › Seams): the tablet value — one tray row of all seven pieces in
// TYPE-001 order (REQ-036 statement and A1), DI-1 `layout()`'s and DI-2 §2's non-compact numbers (decisions.md
// 2026-10-17 "The tablet layout, as drawn", planner Q2). `MainActivity` picks the value by the layout class (S-34) and
// adds the window's insets and the keyboard inset (S-31) at run time.
package app.tangramplaytime.app

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.play.PlayLayout

/** The Play screen's layouts, one per layout class (TYPE-007): [phone] and [tablet], both without insets. */
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

    /**
     * The tablet layout (REQ-036: "the tray as a single row across the bottom"): one tray row
     * `[[LT1, LT2, MT, SQ, PG, ST1, ST2]]` (TYPE-001 order), a dragged piece floating 30 dp above the finger,
     * a 14 dp cell gap, a 16 dp side margin and a 74 dp top bar (decisions.md 2026-10-17 "The tablet layout,
     * as drawn"; WO-006 checkpoint item 1).
     */
    val tablet: PlayLayout = PlayLayout(
        trayRows = listOf(
            listOf(PieceId.LT1, PieceId.LT2, PieceId.MT, PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
        ),
        floatOffsetDp = 30f,
        cellGapDp = 14f,
        sideMarginDp = 16f,
        topBarDp = 74f,
    )
}
