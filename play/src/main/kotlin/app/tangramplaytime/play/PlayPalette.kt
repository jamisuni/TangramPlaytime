// WO-003 TASK-012 · seam S-12 (tasks.md § WO-003 › Seams) · TYPE-001 piece colours; the board colours of
// decisions.md 2026-10-11 "Checkpoint item 1 — the board's colours" (DI-2 §5 default). The only colour
// literals in `:play`; art colours come from each puzzle's `ArtLayer`.
package app.tangramplaytime.play

import androidx.compose.ui.graphics.Color
import app.tangramplaytime.contracts.PieceId

/** Every colour the Play screen paints (S-12). */
internal object PlayPalette {
    /** The flat silhouette (REQ-011: one colour), and the ink of the title. */
    val silhouette: Color = Color(0xFF4A4E69)

    /** The board behind the silhouette. */
    val board: Color = Color(0xFFFFFFFF)

    /** The tray strip. */
    val tray: Color = Color(0xFFEFE7D6)

    /** The paper around the board and the tray (the top bar sits on it). */
    val paper: Color = Color(0xFFFBF7EE)

    /** The accent: the corner-pulse markers (REQ-051) and the landing preview's contrast. */
    val accent: Color = Color(0xFF3D8BFD)

    /** The landing preview's dashed stroke (REQ-021 ASSUMPTION: white, dashed, 2.5 dp). */
    val previewStroke: Color = Color(0xFFFFFFFF)

    /** The fixed colour of [id] (TYPE-001 ASSUMPTION: LT1 red, LT2 blue, MT orange, SQ yellow, PG pink, ST1 green, ST2 purple). */
    fun piece(id: PieceId): Color = when (id) {
        PieceId.LT1 -> Color(0xFFE8505B)
        PieceId.LT2 -> Color(0xFF3D8BFD)
        PieceId.MT -> Color(0xFFF9A826)
        PieceId.SQ -> Color(0xFFFFD23F)
        PieceId.PG -> Color(0xFFFF7AB8)
        PieceId.ST1 -> Color(0xFF2DBE7E)
        PieceId.ST2 -> Color(0xFF9B5DE5)
    }
}
