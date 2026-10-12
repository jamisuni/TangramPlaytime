// WO-003 TASK-012 · seam S-13 (tasks.md § WO-003 › Seams; the tag table) · the test tags both device kits
// find on the unmerged semantics tree. Every tag is declared here, including the ones whose nodes
// TASK-013 (drag, preview) and TASK-014 (decorations, pulse, celebration, picture) add.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId

/** The Play screen's test tags (S-13). The per-piece functions append the piece's name. */
object PlayTags {
    /** The screen root; every test touch is injected here. Carries `PuzzleIdKey` and `PuzzleStateKey`. */
    const val SCREEN: String = "play-screen"

    /** The puzzle title: a merged, inert text node. */
    const val TITLE: String = "title"

    /** The board area; carries `BoardMappingKey`. */
    const val BOARD: String = "board"

    /** The flat silhouette; its bounds are the silhouette's bounding box. */
    const val SILHOUETTE: String = "silhouette"

    /** The tray strip. */
    const val TRAY: String = "tray"

    /** The flip badge at the parallelogram's tray cell (REQ-018); bounds = the 60 dp touch area. */
    const val FLIP_BADGE_TRAY: String = "flip-badge-tray"

    /** The flip badge next to the placed parallelogram (REQ-018); bounds = the 60 dp touch area. */
    const val FLIP_BADGE_BOARD: String = "flip-badge-board"

    /** The dashed landing preview (REQ-021). */
    const val LANDING_PREVIEW: String = "landing-preview"

    /** The corner-pulse layer of a mini puzzle (REQ-051). */
    const val CORNER_PULSE: String = "corner-pulse"

    /** One marker per outline corner inside the pulse (REQ-051); carries `CentreKey`. */
    const val CORNER_MARKER: String = "corner-marker"

    /** The confetti of the celebration (REQ-023); never with reduced motion. */
    const val CONFETTI: String = "confetti"

    /** The solved picture (REQ-023); bounds = the silhouette's box. */
    const val SOLVED_PICTURE: String = "solved-picture"

    /** One cell per piece of the puzzle while it is unsolved; bounds = the S-6 cell. */
    fun trayCell(id: PieceId): String = "tray-cell-${id.name}"

    /** The miniature while the piece is in the tray; bounds = the drawn shape's box. */
    fun trayPiece(id: PieceId): String = "tray-piece-${id.name}"

    /** The dashed outline in the cell while the piece is placed or dragged (REQ-012 rule). */
    fun trayGhost(id: PieceId): String = "tray-ghost-${id.name}"

    /** The S / M / L mark of a triangle in the tray (REQ-043): a merged, inert text node. */
    fun sizeMark(id: PieceId): String = "size-mark-${id.name}"

    /** A locked piece on the board; bounds = the drawn shape's box. */
    fun boardPiece(id: PieceId): String = "board-piece-${id.name}"

    /** The dragged piece; bounds = the drawn shape's box. */
    fun dragPiece(id: PieceId): String = "drag-piece-${id.name}"
}
