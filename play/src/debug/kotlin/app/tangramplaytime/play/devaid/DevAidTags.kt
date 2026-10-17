// WO-005 TASK-024 · seam S-28 (tasks.md § WO-005 › Seams, the aid's tag table) · REQ-046 · architecture.md G-08 /
// O-08: the tags live with the aid in the debug source set. Public, because app/src/androidTest reads them.
package app.tangramplaytime.play.devaid

import app.tangramplaytime.contracts.PieceId

/**
 * The test tags of the aid's nodes, every one of the aid's tag table (S-28). A node's presence follows the aid's
 * state and the controller at once; TASK-026 draws the nodes.
 */
object DevAidTags {
    /** The DEV pill, shown while the open puzzle is not Solved; its bounds are its touch area. */
    const val DEV_BUTTON = "dev-button"

    /** The scrim and the sheet, below the top bar, while a sheet is open. */
    const val SHEET = "dev-aid-sheet"

    /** The prompt's content. */
    const val PROMPT = "dev-aid-prompt"

    /** The prompt's masked numeric field. */
    const val PASSCODE = "dev-aid-passcode"

    /** The "Wrong passcode." line, on the prompt after a wrong entry. */
    const val MESSAGE = "dev-aid-message"

    /** The prompt's OK pill. */
    const val OK = "dev-aid-ok"

    /** The prompt's Cancel pill. */
    const val CANCEL = "dev-aid-cancel"

    /** The unlocked aid's menu content. */
    const val MENU = "dev-aid-menu"

    /** The menu's "Show the solution" pill, "Hide the solution" while the solution is shown. */
    const val SHOW = "dev-aid-show"

    /** The menu's "Solve this puzzle now" pill. */
    const val SOLVE = "dev-aid-solve"

    /** The menu's "Done" pill. */
    const val DONE = "dev-aid-done"

    /** The overlay shape of [id]'s solution polygon. */
    fun solution(id: PieceId): String = "dev-solution-${id.name}"

    /** The label of [id]'s overlay shape: its id. */
    fun solutionLabel(id: PieceId): String = "dev-solution-label-${id.name}"
}
