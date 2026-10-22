// WO-003 TASK-012 · seam S-13 (tasks.md § WO-003 › Seams; the tag table) · the test tags both device kits
// find on the unmerged semantics tree. Every tag is declared here, including the ones whose nodes
// TASK-013 (drag, preview) and TASK-014 (decorations, pulse, celebration, picture) add.
// WO-004 TASK-020 · seam S-13Δ (designs/WO-004-design.md §2): the top bar, Restart, the solved row, the grid
// overview and the loading paper. Every S-13Δ tag is declared here now, including the overview's, whose nodes
// TASK-021 adds. A control's bounds are its touch area (S-13Δ).
// WO-008 TASK-047 · seam S-13Δ3 (designs/WO-008-design.md §2): the timer on the board (REQ-031).
// WO-007 TASK-056 · seam S-53 (designs/WO-007-design.md §2): the gear, the top bar's fourth column (REQ-032, REQ-037);
// › now ends at the gear's left.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId

/** The Play screen's test tags (S-13, S-13Δ). The per-piece functions append the piece's name. */
object PlayTags {
    /** The screen root; every test touch is injected here. Carries `PuzzleIdKey` and `PuzzleStateKey`. */
    const val SCREEN: String = "play-screen"

    /** The puzzle title: a merged, inert text node inside `counter` (seam note 4). */
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

    /** ‹ (REQ-024): the top bar's left column, from the screen edge to 60 dp, full height (S-22). */
    const val NAV_PREV: String = "nav-prev"

    /** › (REQ-024, REQ-050 long press): the top bar's 60 dp column ending at the gear's left, full height (S-22, S-53). */
    const val NAV_NEXT: String = "nav-next"

    /**
     * The gear (REQ-032; S-53): the top bar's 52 dp column at the safe right edge, full height; bounds = that column, its
     * touch area. A router control: no `clickable` and no semantics click action; a press opens the settings at the lift.
     */
    const val GEAR: String = "gear"

    /** The counter zone between ‹ and ›, full height; a press opens the grid (REQ-050). It does not merge its descendants (seam note 4). */
    const val COUNTER: String = "counter"

    /** The rating dots inside `counter` (REQ-024 rule); carries `RatingKey`. */
    const val RATING: String = "rating"

    /** The state word inside `counter`, In progress and Solved only (REQ-024 rule): a merged, inert text node. */
    const val STATE_WORD: String = "state-word"

    /** The Restart pill, shown only In progress (REQ-025); bounds = the pill. A merged display container. */
    const val RESTART: String = "restart"

    /**
     * The running puzzle time at the board's top-right (REQ-031; S-13Δ3, S-47), shown while the timer setting is on and
     * the puzzle is not Solved; bounds = its measured slot. An inert merged node whose text is the time; never a control.
     */
    const val TIMER: String = "timer"

    /** The solved row that replaces the tray (REQ-026); bounds = the tray strip. */
    const val SOLVED_ROW: String = "solved-row"

    /** Retry, the solved row's first third (REQ-026); bounds = that third. A merged display container. */
    const val RETRY: String = "retry"

    /** The best time, the solved row's middle third, inert (REQ-026); bounds = that third. A merged display container. */
    const val BEST_TIME: String = "best-time"

    /** Next, the solved row's last third (REQ-026); bounds = that third. A merged display container. */
    const val SOLVED_NEXT: String = "solved-next"

    /** The grid overview (REQ-050; its nodes are TASK-021's). */
    const val OVERVIEW: String = "overview"

    /** The grid overview's Done (REQ-050; TASK-021). A merged display container. */
    const val OVERVIEW_DONE: String = "overview-done"

    /** The loading paper shown before a puzzle is current (S-23). */
    const val PLAY_LOADING: String = "play-loading"

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

    /** One grid cell per puzzle (REQ-050; TASK-021); carries `PuzzleIdKey` and `PuzzleStateKey` (S-14Δ). */
    fun overviewCell(id: String): String = "overview-cell-$id"
}
