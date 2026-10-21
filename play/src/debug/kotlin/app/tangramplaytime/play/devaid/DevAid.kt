// WO-005 TASK-024 · seams S-26 (the aid's state and actions) and S-27 ("Solve this puzzle now") (tasks.md
// § WO-005 › Seams) · REQ-046 statement, rules 1-4, A1, A3; TYPE-003, TYPE-006 as locked · architecture.md
// G-08 / O-08 (debug build type only), O-01 / G-04 (every position is the engine's), G-14 (no time), G-15
// (a programming error throws) · decisions.md 2026-10-17 "The DEV aid reaches the screen through one door",
// "Solve this puzzle now is an ordinary solve", "A solution pose is read from the puzzle, not computed".
// WO-005 TASK-026 · the overrides of S-25's composable members (plan review F3): the DEV button's slot and drawing
// and the solution overlay (`DevAidLayers.kt`), the sheet (`DevAidSheet.kt`). The state and the solve above are
// TASK-024's and unchanged.
// WO-006 TASK-029 · S-33: `SheetLayer` passes the sheet's content area on to `DevSheetLayer`.
// WO-008 TASK-046 · S-27Δ (designs/WO-008-design.md §2): `drive` runs inside `PlayController.solveWithoutBest`, so the
// aid's solve keeps the best time as it was (REQ-030 rule 4, REQ-046 rule 4 and A3; decisions.md 2026-10-20 "An aid
// solve keeps the best time through one generic controller member." as amended).
package app.tangramplaytime.play.devaid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.play.DevAidHook
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.ScreenGeometry
import app.tangramplaytime.play.Slot
import kotlin.math.abs

/** The passcode of REQ-046's statement. It is compared, never shown (S-26). */
private const val PASSCODE = "0417"

/** S-27's tolerance for a pose's point set and for a lock at the pose, in puzzle units (TYPE-004: at most 1e-6). */
private const val POSE_TOLERANCE = 1e-6

/** Which of the aid's sheets is open (S-26). */
internal enum class DevSheet {
    /** No sheet: the screen is the game's. */
    NONE,

    /** The passcode prompt. */
    PROMPT,

    /** The unlocked aid's menu. */
    MENU,
}

/**
 * REQ-046's testing aid, in the debug build type only (G-08, O-08): the passcode prompt, the solution
 * overlay's on/off and "Solve this puzzle now" (S-26, S-27). `ServiceLoader` builds it through the debug
 * source set's service registration (S-25, S-30), once per process, so its state lives until the app is
 * restarted (REQ-046 rule 2 as read; "What survives").
 *
 * Reads are Compose snapshot state, readable from a JVM test without a composition. Actions run on the main
 * thread. A new aid is locked, with the solution off and no sheet.
 */
internal class DevAid : DevAidHook() {

    /** The right passcode was entered in this process (REQ-046 A1, rule 2). */
    var unlocked: Boolean by mutableStateOf(false)
        private set

    /** The solution overlay is on (REQ-046 A2, rule 3); only [toggleSolution] changes it. */
    var solutionShown: Boolean by mutableStateOf(false)
        private set

    /** The open sheet: the prompt, the menu, or none. */
    var sheet: DevSheet by mutableStateOf(DevSheet.NONE)
        private set

    /** The prompt shows the "Wrong passcode." line (REQ-046 rule 1). */
    var wrongPasscode: Boolean by mutableStateOf(false)
        private set

    override val sheetOpen: Boolean
        get() = sheet != DevSheet.NONE

    /** The DEV button: the prompt while locked, without the message; the menu once unlocked (S-26). */
    override fun onButton() {
        sheet = if (unlocked) DevSheet.MENU else DevSheet.PROMPT
        wrongPasscode = false
    }

    /** The DEV pill's slot (S-25, touch geometry "The DEV button"); null while the open puzzle is Solved (REQ-046 rule 5). */
    @Composable
    override fun rememberButtonSlot(controller: PlayController, geometry: ScreenGeometry): Slot? =
        rememberDevButtonSlot(controller, geometry)

    /**
     * The solution overlay (S-25; REQ-046 A2, rule 3): one shape and one label per piece of the open puzzle while
     * [solutionShown] holds and the puzzle is not Solved (the aid's tag table); [solutionShown] itself is kept.
     */
    @Composable
    override fun SolutionLayer(controller: PlayController, geometry: ScreenGeometry) {
        if (solutionShown && controller.state != PuzzleState.SOLVED) DevSolutionOverlay(controller, geometry)
    }

    /** The DEV pill at [slot] (S-25), solid accent while [solutionShown] holds, dashed and faded otherwise. */
    @Composable
    override fun ButtonLayer(slot: Slot) {
        DevButton(slot, solutionShown)
    }

    /** The open sheet (S-25): the scrim over [area], below the top bar; the prompt or the menu inside [content] (S-33). */
    @Composable
    override fun SheetLayer(controller: PlayController, area: Slot, content: Slot) {
        DevSheetLayer(this, controller, area, content)
    }

    /**
     * The prompt's OK (S-26; REQ-046 A1, rule 1).
     *
     * @return true for exactly the passcode: unlocked, the menu, no message. Otherwise false: the message is
     *   shown and nothing else changes, the empty entry included (a wrong passcode reveals nothing).
     */
    fun submit(passcode: String): Boolean {
        if (passcode != PASSCODE) {
            wrongPasscode = true
            return false
        }
        unlocked = true
        sheet = DevSheet.MENU
        wrongPasscode = false
        return true
    }

    /** Cancel, Done or a tap on the scrim: no sheet and no message (S-26). */
    fun close() {
        sheet = DevSheet.NONE
        wrongPasscode = false
    }

    /**
     * Show or Hide the solution (S-26; REQ-046 A2, rule 3): flips [solutionShown] and closes the sheet.
     *
     * @return false, and nothing changes, while locked; true when it acted.
     */
    fun toggleSolution(): Boolean {
        if (!unlocked) return false
        solutionShown = !solutionShown
        close()
        return true
    }

    /**
     * Solve this puzzle now (S-26, S-27; REQ-046 A3, rule 4): closes the sheet, then solves the open puzzle
     * through [controller]'s public actions only, in an order the engine confirms before anything changes. The
     * last lock is the ordinary solve (Solved, a new celebration, `Solved`, the save through `Persistence`); the
     * actions run inside `solveWithoutBest` (S-27Δ), so the best time stays as it was, and only TYPE-006's locked
     * transitions are used.
     *
     * @return false, and nothing changes, while locked; false, with the sheet closed and the game unchanged,
     *   when the open puzzle is Solved or no order exists; true when it solved the puzzle.
     * @throws IllegalStateException when `boardDpPerUnit` is not > 0 (S-2; a programming error, G-15), before
     *   anything changes, the sheet included; or when an action does not do what the plan found (G-15).
     */
    fun solveNow(controller: PlayController): Boolean {
        if (!unlocked) return false
        val plan = planSolve(controller)
        close()
        if (plan == null) return false
        controller.solveWithoutBest { drive(controller, plan) }
        return true
    }

    /** One planned drop: the piece's solution [pose] (where it is released) and the engine's [lock] there. */
    private class PlannedDrop(val pose: Placement, val lock: Placement)

    /**
     * S-27 steps 1 and 2, before anything changes. Each piece's pose is read from the puzzle's own solution
     * ([poseOf]); the order is a depth-first search over the remaining pieces in tray order from an empty board,
     * where `findLock` must lock each piece at its pose given the pieces planned before it. A set of placed
     * pieces with no way on is not tried again.
     *
     * @return the drops in lock order, or null when the puzzle is Solved, a piece has no pose, or no order exists.
     */
    private fun planSolve(c: PlayController): List<PlannedDrop>? {
        if (c.state == PuzzleState.SOLVED) return null
        val dpPerUnit = c.boardDpPerUnit
        check(dpPerUnit > 0) { "boardDpPerUnit must be > 0 before Solve this puzzle now, was $dpPerUnit" }
        val puzzle = c.puzzle
        val poses = puzzle.solution.map { poseOf(c, it) ?: return null }.sortedBy { it.piece }
        val dead = HashSet<Set<PieceId>>()

        fun search(planned: List<PlannedDrop>): List<PlannedDrop>? {
            if (planned.size == poses.size) return planned
            val done = planned.mapTo(HashSet()) { it.pose.piece }
            if (done in dead) return null
            val board = planned.map { it.lock }
            for (pose in poses) {
                if (pose.piece in done) continue
                val result = c.engine.findLock(puzzle, board, pose.piece, pose.turn, pose.mirrored, pose.centre, dpPerUnit)
                if (result is LockResult.Locked && isAt(result.placement, pose)) {
                    search(planned + PlannedDrop(pose, result.placement))?.let { return it }
                }
            }
            dead += done
            return null
        }
        return search(emptyList())
    }

    /**
     * S-27 step 1: the pose of one stored solution polygon. The centre is the polygon's average (the
     * `Placement.centre` KDoc); the turn and mirror are the first of turns 0..7, unmirrored first and mirrored
     * for the parallelogram only (TYPE-003), whose `engine.vertices` equal the polygon as a point set.
     *
     * @return the pose, or null when no turn matches.
     */
    private fun poseOf(c: PlayController, sp: SolvedPiece): Placement? {
        val n = sp.polygon.size
        val centre = Point(sp.polygon.sumOf { it.x } / n, sp.polygon.sumOf { it.y } / n)
        val mirrors = if (sp.piece == PieceId.PG) listOf(false, true) else listOf(false)
        for (mirrored in mirrors) {
            for (step in 0..7) {
                val turn = Turn(step)
                if (samePointSet(c.engine.vertices(sp.piece, turn, mirrored, centre), sp.polygon)) {
                    return Placement(sp.piece, turn, mirrored, centre)
                }
            }
        }
        return null
    }

    /**
     * S-27 step 3, through S-2's public actions only: `restart()` first if In progress (In progress → New);
     * then, per planned drop, the flip while the parallelogram's tray mirror differs from its pose, `startDrag`
     * at the pose's centre (the first moves New → In progress), the twist the shorter way to the pose's turn,
     * the move over the board, and `drop()`, which must return the planned lock.
     */
    private fun drive(c: PlayController, plan: List<PlannedDrop>) {
        if (c.state == PuzzleState.IN_PROGRESS) check(c.restart()) { "restart() refused on an In-progress puzzle" }
        for (drop in plan) {
            val pose = drop.pose
            val piece = pose.piece
            if (piece == PieceId.PG && c.trayMirrored(piece) != pose.mirrored) {
                check(c.pressBadge() && c.trayMirrored(piece) == pose.mirrored) { "the flip did not mirror $piece in the tray" }
            }
            check(c.startDrag(piece, pose.centre)) { "startDrag($piece) refused" }
            val held = c.dragged?.turn ?: throw IllegalStateException("startDrag($piece) left nothing dragged")
            c.twistDrag(shorterWay(held, pose.turn))
            c.moveDrag(pose.centre, overBoard = true)
            val got = c.drop()
            check(got != null && isAt(got, drop.lock)) { "drop() of $piece returned $got, not the planned ${drop.lock}" }
        }
    }

    /**
     * The twist steps from [from] to [to] the shorter way (S-27), positive clockwise (S-2 `twistDrag`). S-27
     * leaves a tie of four steps open; it turns clockwise, the way a tap turns (REQ-016, `Turn.next`).
     */
    private fun shorterWay(from: Turn, to: Turn): Int {
        val d = Math.floorMod(to.step - from.step, 8)
        return if (d <= 4) d else d - 8
    }

    /** [got] is [pose]'s lock: the same piece, turn and mirror, the centre within [POSE_TOLERANCE] (S-27 step 2). */
    private fun isAt(got: Placement, pose: Placement): Boolean =
        got.piece == pose.piece && got.turn == pose.turn && got.mirrored == pose.mirrored && near(got.centre, pose.centre)

    /** [a] and [b] are the same point set within [POSE_TOLERANCE]: equal sizes, and every point has a partner both ways. */
    private fun samePointSet(a: List<Point>, b: List<Point>): Boolean =
        a.size == b.size && a.all { p -> b.any { near(p, it) } } && b.all { p -> a.any { near(p, it) } }

    private fun near(a: Point, b: Point): Boolean = abs(a.x - b.x) <= POSE_TOLERANCE && abs(a.y - b.y) <= POSE_TOLERANCE
}
