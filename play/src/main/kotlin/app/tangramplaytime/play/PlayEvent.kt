// WO-003 TASK-010 · seam S-4 (tasks.md § WO-003 › Seams) · architecture.md O-07: `:play` emits, `:app` plays.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId

/**
 * What the Play screen's state holder reports to whoever plays sounds and haptics (O-07; WO-007 wires
 * them). Emitted synchronously by the [PlayController] actions, after the state has changed, so a
 * listener that reads the controller sees the new state. `:play` itself plays nothing.
 */
sealed interface PlayEvent {
    /** A drag started on [piece], from the tray or the board (`startDrag`). */
    data class PickedUp(val piece: PieceId) : PlayEvent

    /** [piece] turned one step or mirrored: a tray tap, a kept board turn, a twist step, a badge press (REQ-016, REQ-017, REQ-018). */
    data class Turned(val piece: PieceId) : PlayEvent

    /** A dropped [piece] locked on the board (REQ-019). */
    data class Locked(val piece: PieceId) : PlayEvent

    /** A dropped or cancelled [piece] went back to its tray cell (REQ-020). */
    data class WentHome(val piece: PieceId) : PlayEvent

    /** The last piece of the puzzle [puzzleId] locked (REQ-022); always right after that piece's [Locked]. */
    data class Solved(val puzzleId: String) : PlayEvent
}
