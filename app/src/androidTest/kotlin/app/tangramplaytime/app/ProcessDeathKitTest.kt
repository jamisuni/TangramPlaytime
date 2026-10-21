// Acceptance kit (WO-004, TASK-019): the process-death pair (designs/WO-004-design.md §2 S-24, §3 "Highest-risk part":
// "Phase 1 places three pieces through the real app and calls `flush()` via `AppGraph`, the kit's one door into it. Phase 2
// runs in a new process and finds them in place. A same-process test would be fooled by the controller, which outlives the
// activity"; tasks.md § WO-004 seam note 1). `tools/process_death_check.sh` runs `phase1` with the instrumentation argument
// `deathPhase` 1, force-stops the app, then runs `phase2` with `deathPhase` 2 in a new process; each phase is judged from
// the instrumentation output. Without the argument (the ordinary connected run) both phases are skipped by `assumeTrue`
// before the activity is launched. Nothing passes between the two processes but the app's own progress file (ADR-003):
// phase 2 derives what it looks for the same way phase 1 chose it, from the bundled library and the engine.
// The play kit's same-process legs of REQ-003 A2 and REQ-025 A1 are ResumeScreenKitTest's; this file carries the
// process-death leg, tokened on `phase2`.
// Amended WO-008 (TASK-050; tasks.md § WO-008, the TASK-050 row: "the death pair's times"): `phase1` also counts N s through
// the window and the kit clock (KitApp `kitInstallClock`, `kitTouchThroughWindow`; S-46 `AppClock.override`, reset to null in
// `finally`) before its flush, and `phase2` reads them back: REQ-005 A2 is an extra token there, beside the JVM kit's in
// `play/` (PlayTimeKitTest). Each phase is still one finished test, and the guard is unchanged.
package app.tangramplaytime.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ProcessDeathKitTest {

    /**
     * Seam note 1: each phase is guarded by `assumeTrue` on the instrumentation argument `deathPhase` (`1` for `phase1`,
     * `2` for `phase2`). The guard is the outer rule, so a skipped phase never launches the activity.
     */
    @get:Rule(order = 0)
    val kitPhaseGuard: TestRule = TestRule { base: Statement, description: Description ->
        object : Statement() {
            override fun evaluate() {
                val wanted = when (description.methodName) {
                    "phase1" -> "1"
                    "phase2" -> "2"
                    else -> null
                }
                val given = InstrumentationRegistry.getArguments().getString(KIT_PHASE_ARGUMENT)
                assumeTrue("${description.methodName} runs only with -e $KIT_PHASE_ARGUMENT $wanted (given: $given)", wanted != null && given == wanted)
                base.evaluate()
            }
        }
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test // no token: the set-up half of the death pair, judged by phase2
    fun phase1() {
        // Over a cleared store (the script's `pm clear`): the death fixture's puzzle is opened and its three pieces are
        // placed through S-2 on the main thread — the line is about surviving the death, not about the gesture — then the
        // screen is checked to show them and `flush()` is called through `AppGraph.controller` (S-15Δ; S-2Δ `flush`), so
        // the progress file holds them before the script force-stops the process.
        kitWaitForScreen()
        val (p, three) = kitDeathFixture()
        val controller = AppGraph.controller.value ?: throw AssertionError("AppGraph.controller is null once `play-screen` is shown")
        composeRule.runOnUiThread { check(controller.open(p.id)) { "kit: open(${p.id}) refused" } }
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            check(controller.boardDpPerUnit > 0.0) { "kit: the screen set no board scale for ${p.id}" }
            for (pl in three) {
                if (pl.piece == PieceId.PG && controller.trayMirrored(PieceId.PG) != pl.mirrored) check(controller.pressBadge()) { "kit: pressBadge() refused" }
                check(controller.startDrag(pl.piece, pl.centre)) { "kit: startDrag(${pl.piece}) refused" }
                val from = controller.dragged?.turn?.step ?: error("kit: no drag after startDrag(${pl.piece})")
                val steps = ((pl.turn.step - from) % 8 + 8) % 8
                if (steps != 0) controller.twistDrag(if (steps <= 4) steps else steps - 8)
                controller.moveDrag(pl.centre, overBoard = true)
                controller.updatePreview()
                checkNotNull(controller.drop()) { "kit: the planned drop of ${pl.piece} did not lock" }
            }
        }
        composeRule.waitForIdle()
        kitAssertShowsLeft(p, three, "phase 1, before the flush")
        // WO-008 TASK-050, the death pair's times (the brief; design §3 "Recreation and death"; decisions.md 2026-10-20 "When the
        // times are written, and what a process death may lose." (D8)): with the kit clock installed (S-46 `AppClock.override`,
        // KitApp `kitInstallClock`: a fold on the main thread, so nothing real counts after it), the first touch of this process
        // goes through the window at an inert point of the board (KitApp `kitTouchThroughWindow`; review F1), then the kit clock
        // moves N s and the controller folds, on the main thread. Fixture first: nothing was counted before that touch (D3: "In a
        // new process nothing counts before the first touch"), so the N s are the only seconds of this process. N = 60 is a
        // multiple of S-20Δ's 10-second cadence (decisions.md 2026-10-21 "The save cadence counts play seconds since the last
        // play-times write …": the tick writes the play times, then the open puzzle, once 10 are unsaved), so every counted
        // second is queued before the flush below; and it is TYPE-005's idle limit, so after its one touch the player is idle
        // when the override is reset to null in `finally` (S-45's backwards reading keeps the time since the last touch,
        // decisions.md 2026-10-20, the F5 amendment), and no real second is counted between the reset and the force-stop.
        val kit = KitApp(composeRule)
        val clock = kit.kitInstallClock("phase 1")
        try {
            val before = composeRule.runOnUiThread { controller.countTime(); controller.timeSeconds to controller.playTimes.allTimeSeconds }
            assertEquals("fixture: phase 1 counted nothing before the first touch (the puzzle's seconds, the all-time play time; D3)", 0 to 0, before)
            kit.kitTouchThroughWindow("phase 1: the first touch of the process")
            val after = composeRule.runOnUiThread {
                clock.forward(KIT_DEATH_SECONDS)
                controller.countTime()
                controller.timeSeconds to controller.playTimes.allTimeSeconds
            }
            assertEquals("phase 1: a touch, then $KIT_DEATH_SECONDS s: the fixture puzzle's seconds and the all-time play time", KIT_DEATH_SECONDS to KIT_DEATH_SECONDS, after)
        } finally {
            composeRule.runOnUiThread { AppClock.override = null }
        }
        // S-2Δ: "`flush()` delegates to Persistence" (S-19 `suspend fun flush()`); a Job it might return is joined too.
        runBlocking {
            withTimeout(30_000) {
                val result: Any? = controller.flush()
                if (result is Job) result.join()
            }
        }
    }

    @Test // REQ-003.A2 REQ-025.A1 REQ-005.A2
    fun phase2() {
        // REQ-025 A1: "Leave a puzzle with three pieces placed, come back: the same three pieces are in the same places."
        // REQ-003 A2: "Leaving and returning to a puzzle never loses its state." Rule (REQ-025): "The state survives closing
        // the app." The leg across a real process death (the play kit's ResumeScreenKitTest carries the same-process legs):
        // phase 1 left the death fixture's puzzle with three pieces placed and the process was killed; in this new process
        // the app comes back to that puzzle (IProgressStore KDoc of `lastOpenedPuzzleId`: "so the player returns to the same
        // puzzle"), In progress, with the same three `board-piece` nodes at the same piece, turn, mirror and centre (S-14
        // keys, read against each node's box through BoardMappingKey and the engine's corners), the controller's board the
        // same three (through `AppGraph.controller`), and the other pieces in the tray. On a cleared store (the script's
        // positive control) the app opens the first puzzle New instead, and this phase fails.
        kitWaitForScreen()
        val (p, three) = kitDeathFixture()
        kitAssertShowsLeft(p, three, "phase 2, in a new process")
        val controller = AppGraph.controller.value ?: throw AssertionError("AppGraph.controller is null once `play-screen` is shown")
        val placed = composeRule.runOnUiThread { controller.placed.toList() }
        assertEquals("the controller's board: the pieces", three.map { it.piece }.sorted(), placed.map { it.piece }.sorted())
        for (pl in three) assertTrue("the controller's ${pl.piece}: expected $pl, is ${placed.single { it.piece == pl.piece }}", kitSame(pl, placed.single { it.piece == pl.piece }))
        // WO-008 TASK-050 — REQ-005 A2: "The total active play time is available at any time", read as decisions.md 2026-10-20
        // "REQ-005's two lines are verified as locked, at the seam …" (P1): "the all-time play time the controller reports (from
        // IActiveTime.playTimes()) equals every second counted so far. It is read … after a process death." In this new process
        // there is no override and no touch (D3: nothing counts before the first touch), so every second counted is phase 1's
        // N: the all-time play time is N, and so is the fixture puzzle's own time (S-20Δ: `timeSeconds` rides the open puzzle's
        // save; the PuzzleProgress KDoc: the active seconds of the current attempt). A process death that lost the times, or a
        // write that never reached the file, reads 0 here.
        val times = composeRule.runOnUiThread { controller.timeSeconds to controller.playTimes.allTimeSeconds }
        assertEquals("the fixture puzzle's seconds across the death (S-1Δ3 timeSeconds)", KIT_DEATH_SECONDS, times.first)
        assertEquals("the all-time play time across the death (S-1Δ3 playTimes)", KIT_DEATH_SECONDS, times.second)
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** The screen shows [p], In progress, with exactly [three] on the board (keys and boxes) and the rest in the tray. */
    private fun kitAssertShowsLeft(p: PuzzleDefinition, three: List<Placement>, what: String) {
        val screen = kitNode(PlayTags.SCREEN)
        val id = screen.config.getOrNull(PlaySemantics.PuzzleIdKey)
        assertEquals("$what: the puzzle shown", p.id, id)
        val title = composeRule.onNodeWithTag(PlayTags.TITLE).fetchSemanticsNode("the title").config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
        assertTrue("$what: the title '$title' is one of ${p.id}'s titles ${p.titles.values}", title in p.titles.values)
        assertEquals("$what: In progress", PuzzleState.IN_PROGRESS, screen.config.getOrNull(PlaySemantics.PuzzleStateKey))
        val onBoard = PieceId.entries.filter { kitExists(PlayTags.boardPiece(it)) }
        assertEquals("$what: the pieces on the board", three.map { it.piece }.sorted(), onBoard.sorted())
        val mapping = kitNode(PlayTags.BOARD).config.getOrNull(PlaySemantics.BoardMappingKey) ?: throw AssertionError("$what: `board` carries no BoardMappingKey")
        val origin = screen.boundsInRoot.topLeft
        for (pl in three) {
            val n = kitNode(PlayTags.boardPiece(pl.piece))
            val shown = Placement(
                n.config.getOrNull(PlaySemantics.PieceKey) ?: error("no PieceKey"),
                n.config.getOrNull(PlaySemantics.TurnKey) ?: error("no TurnKey"),
                n.config.getOrNull(PlaySemantics.MirroredKey) ?: error("no MirroredKey"),
                n.config.getOrNull(PlaySemantics.CentreKey) ?: error("no CentreKey"),
            )
            assertTrue("$what: ${pl.piece} expected at $pl, shown at $shown", kitSame(pl, shown))
            val corners = kitEngine.vertices(pl.piece, shown.turn, shown.mirrored, shown.centre).map { mapping.toPx(it) }
            val box = n.boundsInRoot.translate(-origin)
            val tol = 2f
            assertTrue(
                "$what: ${pl.piece}'s node box $box is the box of its corners (${corners.minOf { it.x }}, ${corners.minOf { it.y }}, ${corners.maxOf { it.x }}, ${corners.maxOf { it.y }})",
                abs(box.left - corners.minOf { it.x }) <= tol && abs(box.top - corners.minOf { it.y }) <= tol &&
                    abs(box.right - corners.maxOf { it.x }) <= tol && abs(box.bottom - corners.maxOf { it.y }) <= tol,
            )
        }
        val inTray = p.pieces.filter { kitExists(PlayTags.trayPiece(it)) }.toSet()
        assertEquals("$what: the other pieces in the tray", p.pieces - three.map { it.piece }.toSet(), inTray)
    }

    private fun kitWaitForScreen() {
        composeRule.waitUntil(timeoutMillis = 30_000) { kitExists(PlayTags.SCREEN) }
    }

    private fun kitExists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun kitNode(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode("node `$tag`")

    private companion object {
        /** Seam note 1: the instrumentation argument that selects a phase. */
        const val KIT_PHASE_ARGUMENT = "deathPhase"

        /**
         * WO-008 TASK-050: the seconds phase 1 counts after its one touch (N): a multiple of S-20Δ's 10-second cadence, and
         * TYPE-005's 60 s idle limit, so the player is idle when phase 1 resets the kit clock (see `phase1`).
         */
        const val KIT_DEATH_SECONDS = 60

        const val KIT_TOL = 1e-6

        /** A board scale for searches from exact solution poses: the outcome does not depend on it (TYPE-004: R ≥ 0.65 units). */
        const val KIT_ANY_DP = 100.0

        val kitEngine: ILockEngine by lazy { LockEngine() }

        /**
         * The death fixture, chosen identically in both phases from the bundled library and the engine: a puzzle of at
         * least four pieces (so three placed do not solve it, REQ-022) and three of its pieces at their stored solution
         * poses, each the engine's exact lock at its pose against the ones before it. Preferred: the first such puzzle in
         * ‹ › order whose parallelogram is mirrored in its solution and comes among the first three locks when tried first,
         * so a mirror lost across the death shows (REQ-025 rule: "Saved per piece: … turn, mirror"); else the first such
         * puzzle, in solution order. It is never the fresh start's puzzle (a mini of three pieces, REQ-045), so a cleared
         * store fails phase 2.
         */
        fun kitDeathFixture(): Pair<PuzzleDefinition, List<Placement>> {
            val library = PuzzleLibraries.bundled(kitEngine)
            for (p in library.puzzles) {
                if (p.pieces.size < 4 || PieceId.PG !in p.pieces) continue
                if (!kitPoseOf(PieceId.PG, p.solution.single { it.piece == PieceId.PG }.polygon).mirrored) continue
                val order = listOf(PieceId.PG) + p.solution.map { it.piece }.filter { it != PieceId.PG }
                val all = kitLocks(p, order, p.pieces.size)
                if (all.indexOfFirst { it.piece == PieceId.PG } in 0..2) return p to all.take(3)
            }
            for (p in library.puzzles) {
                if (p.pieces.size < 4) continue
                val three = kitLocks(p, p.solution.map { it.piece }, 3)
                if (three.size == 3) return p to three
            }
            throw AssertionError("kit: no bundled puzzle of four or more pieces locks three pieces at their solution poses")
        }

        /** Up to [count] solution poses of [p], tried in [order] pass after pass, each kept when the engine locks it exactly there. */
        fun kitLocks(p: PuzzleDefinition, order: List<PieceId>, count: Int): List<Placement> {
            val out = ArrayList<Placement>()
            var progress = true
            while (out.size < count && progress) {
                progress = false
                for (piece in order) {
                    if (out.size >= count) break
                    if (out.any { it.piece == piece }) continue
                    val pose = kitPoseOf(piece, p.solution.single { it.piece == piece }.polygon)
                    val lock = kitEngine.findLock(p, out, piece, pose.turn, pose.mirrored, pose.centre, KIT_ANY_DP)
                    if (lock is LockResult.Locked && kitSame(lock.placement, pose)) {
                        out += lock.placement
                        progress = true
                    }
                }
            }
            return out
        }

        /** The (turn, mirror, centre) at which `vertices` reproduces a stored solution polygon (centre: the corners' mean, TYPE-003). */
        fun kitPoseOf(piece: PieceId, polygon: List<Point>): Placement {
            val centre = Point(polygon.sumOf { it.x } / polygon.size, polygon.sumOf { it.y } / polygon.size)
            for (mirrored in listOf(false, true)) for (step in 0..7) {
                val v = kitEngine.vertices(piece, Turn(step), mirrored, centre)
                val same = v.size == polygon.size &&
                    v.all { a -> polygon.any { b -> abs(a.x - b.x) <= KIT_TOL && abs(a.y - b.y) <= KIT_TOL } } &&
                    polygon.all { b -> v.any { a -> abs(a.x - b.x) <= KIT_TOL && abs(a.y - b.y) <= KIT_TOL } }
                if (same) return Placement(piece, Turn(step), mirrored, centre)
            }
            throw AssertionError("kit: no turn and mirror of $piece reproduces its solution polygon $polygon")
        }

        fun kitSame(a: Placement, b: Placement): Boolean =
            a.piece == b.piece && a.turn == b.turn && a.mirrored == b.mirrored &&
                abs(a.centre.x - b.centre.x) <= KIT_TOL && abs(a.centre.y - b.centre.y) <= KIT_TOL
    }
}
