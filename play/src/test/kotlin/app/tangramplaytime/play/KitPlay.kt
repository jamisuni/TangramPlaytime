// Acceptance kit (WO-003, TASK-009) — the JVM kit's shared helpers: the shipped puzzles over the real
// engine, the S-5 phone layout literal, the solution-pose finder, the drop planner and small drivers
// for PlayController. Test-only; no product code.
//
// - Written against tasks.md § WO-003 › Seams S-1…S-9 (the planner's seam table; this WO is Light and
//   has no design). The puzzles are the shipped ones, read through
//   `PuzzleLibraries.bundled(LockEngine())` with `:content` and `:kernel` on the unit-test classpath
//   (TASK-010 adds them); the kit reads no file itself.
// - The engine is the geometry oracle (architecture.md O-01, G-04: "`:play` asks `ILockEngine`; it never
//   re-derives a position"). Every fixture a test relies on is shown test-side with the engine first
//   (WO-003 brief, oracle rule: "Fixtures must discriminate"); PlayController is what is under test.
// - This file carries no acceptance token (traceability-rules.md §2: a token is a coverage claim).
// - Helper names carry a `kit` / `Kit` / `KIT_` prefix so they cannot collide with an `internal`
//   declaration of :play's main source set in the same package.
package app.tangramplaytime.play

import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.kernel.LockEngine
// S-4 names the five events of `sealed interface PlayEvent`. This import reaches them when they are
// nested in PlayEvent; when they are top-level in this package they resolve without it.
import app.tangramplaytime.play.PlayEvent.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.math.abs

// ── The library and the engine ─────────────────────────────────────────────────────────────────

/** The real lock engine (WO-001). One engine serves the library and every kit controller, as `:app` wires it (S-15). */
val kitEngine: ILockEngine by lazy { LockEngine() }

/** The shipped puzzles (WO-002), built once: "the library is fixed for the process's lifetime" (IPuzzleLibrary KDoc). */
val kitLibrary: IPuzzleLibrary by lazy { PuzzleLibraries.bundled(kitEngine) }

/** The [n]th listed puzzle of [kind], in list order; a missing one is a fixture defect. */
fun kitPuzzleOf(kind: PuzzleKind, n: Int = 0): PuzzleDefinition =
    kitLibrary.puzzles.filter { it.kind == kind }.getOrNull(n)
        ?: throw AssertionError("fixture defect: the library lists no ${kind.name} puzzle number $n")

// ── Literals ───────────────────────────────────────────────────────────────────────────────────

/**
 * The S-5 phone value, which "the kits write as a literal": rows [[LT1, LT2, MT], [SQ, PG, ST1, ST2]],
 * float offset 30 (REQ-014 ASSUMPTION), gap 8, side margin 8, top bar 66 (tasks.md § WO-003 S-5).
 */
val KIT_PHONE_LAYOUT: PlayLayout = PlayLayout(
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
 * The board scale the kit's controllers use, dp per puzzle unit. Lock distance R = max(0.65, 30 / 60) =
 * 0.65 units (ILockEngine KDoc "Lock distance"; TYPE-004). The planner uses the same value.
 */
const val KIT_DP_PER_UNIT: Double = 60.0

/** A small board scale: R = max(0.65, 30 / 10) = 3 units, the 30 dp floor of TYPE-004. Used only where a fixture needs a wide reach. */
const val KIT_WIDE_DP_PER_UNIT: Double = 10.0

/** Where a tray drag starts, in puzzle units: below every shipped board. S-2 `startDrag` stores the centre as given. */
val KIT_TRAY_POINT: Point = Point(2.0, 12.0)

/** A centre far from every shipped silhouette: no anchor lies within R of any piece corner there (shown with the engine where used). */
val KIT_FAR_POINT: Point = Point(-40.0, -40.0)

// ── Geometry helpers (points only; every shape comes from the engine) ──────────────────────────

/** The average of [points] — the piece centre of TYPE-003 and of the `Placement.centre` KDoc. */
fun kitAverage(points: List<Point>): Point =
    Point(points.sumOf { it.x } / points.size, points.sumOf { it.y } / points.size)

/** True when [a] and [b] differ by at most [tol] in each coordinate. */
fun kitNear(a: Point, b: Point, tol: Double): Boolean = abs(a.x - b.x) <= tol && abs(a.y - b.y) <= tol

/** True when [a] and [b] are the same point set within [tol] (equal sizes, every point has a partner). */
fun kitSamePointSet(a: List<Point>, b: List<Point>, tol: Double): Boolean =
    a.size == b.size && a.all { p -> b.any { kitNear(p, it, tol) } } && b.all { p -> a.any { kitNear(p, it, tol) } }

/** Asserts [actual] is [expected] as a point set within [tol]. */
fun kitAssertSamePointSet(expected: List<Point>, actual: List<Point>, tol: Double, message: String) {
    assertTrue("$message: expected the point set $expected but was $actual", kitSamePointSet(expected, actual, tol))
}

/** Asserts [actual] is [expected]: the same piece, turn and mirror, and the centre within [tol]. */
fun kitAssertSamePlacement(expected: Placement, actual: Placement?, message: String, tol: Double = 1e-9) {
    if (actual == null) throw AssertionError("$message: expected $expected but was null")
    assertEquals("$message: piece", expected.piece, actual.piece)
    assertEquals("$message: turn", expected.turn, actual.turn)
    assertEquals("$message: mirror", expected.mirrored, actual.mirrored)
    assertTrue("$message: centre ${actual.centre}, expected ${expected.centre}", kitNear(expected.centre, actual.centre, tol))
}

/** True when [result] is a lock of [pose]'s piece at its turn, mirror and centre (within [tol]). */
fun kitLocksAt(result: LockResult, pose: Placement, tol: Double = 1e-6): Boolean =
    result is LockResult.Locked &&
        result.placement.piece == pose.piece &&
        result.placement.turn == pose.turn &&
        result.placement.mirrored == pose.mirrored &&
        kitNear(result.placement.centre, pose.centre, tol)

// ── Solution poses and the drop planner ────────────────────────────────────────────────────────

/**
 * The pose of one stored solution polygon: its centre is the polygon's average (`Placement.centre`
 * KDoc), and its turn and mirror are the lowest turn 0..7 — unmirrored first, mirrored tried for the
 * parallelogram only (TYPE-003: "Only the parallelogram has a mirror image that differs") — whose
 * `engine.vertices` match the polygon as a point set within 1e-6. No match is a fixture defect.
 */
fun kitPoseOf(sp: SolvedPiece): Placement {
    val centre = kitAverage(sp.polygon)
    val mirrors = if (sp.piece.type == PieceType.PG) listOf(false, true) else listOf(false)
    for (mirrored in mirrors) {
        for (step in 0..7) {
            val corners = kitEngine.vertices(sp.piece, Turn(step), mirrored, centre)
            if (kitSamePointSet(sp.polygon, corners, 1e-6)) return Placement(sp.piece, Turn(step), mirrored, centre)
        }
    }
    throw AssertionError("fixture defect: no turn of ${sp.piece} matches ${sp.polygon}")
}

/**
 * One planned drop (S-2 `drop`): released at [dropCentre], the engine locks the piece at [target] — the
 * piece's solution pose, as the engine places it ("the anchor's exact position minus the corner's exact
 * offset", ILockEngine `findLock` KDoc).
 */
data class KitStep(val target: Placement, val dropCentre: Point) {
    /** The planned piece. */
    val piece: PieceId get() = target.piece
}

/**
 * An order of drops that completes [puzzle] with every piece at its stored solution pose, found with the
 * engine at [dpPerUnit]: each drop is released at the pose's centre with the pose's turn and mirror, and
 * `findLock` (with the pieces planned before it on the board, in lock order) must lock it exactly at
 * that pose. Depth-first over the remaining pieces in tray order; [first] / [last] force a piece to the
 * front / back. REQ-019's rationale says an anchorable order exists ("Every arrangement a player builds
 * is anchorable at every step"). Null when no such order exists.
 */
fun kitPlanOrNull(
    puzzle: PuzzleDefinition,
    dpPerUnit: Double = KIT_DP_PER_UNIT,
    first: PieceId? = null,
    last: PieceId? = null,
): List<KitStep>? {
    val poses = puzzle.solution.map { kitPoseOf(it) }.sortedBy { it.piece }
    val dead = HashSet<Set<PieceId>>()
    fun search(placed: List<Placement>, steps: List<KitStep>): List<KitStep>? {
        val done = placed.map { it.piece }.toSet()
        val left = poses.filter { it.piece !in done }
        if (left.isEmpty()) return steps
        if (done in dead) return null
        var candidates = left
        if (last != null && left.size > 1) candidates = candidates.filter { it.piece != last }
        if (first != null && steps.isEmpty()) candidates = candidates.filter { it.piece == first }
        for (pose in candidates) {
            val result = kitEngine.findLock(puzzle, placed, pose.piece, pose.turn, pose.mirrored, pose.centre, dpPerUnit)
            if (kitLocksAt(result, pose)) {
                val locked = (result as LockResult.Locked).placement
                search(placed + locked, steps + KitStep(locked, pose.centre))?.let { return it }
            }
        }
        dead += done
        return null
    }
    return search(emptyList(), emptyList())
}

/** [kitPlanOrNull], with no order being a fixture defect. */
fun kitPlan(
    puzzle: PuzzleDefinition,
    dpPerUnit: Double = KIT_DP_PER_UNIT,
    first: PieceId? = null,
    last: PieceId? = null,
): List<KitStep> = kitPlanOrNull(puzzle, dpPerUnit, first, last)
    ?: throw AssertionError("fixture defect: no drop order of ${puzzle.id} locks every piece at its solution pose (first $first, last $last)")

// ── Controller drivers (S-1, S-2) ──────────────────────────────────────────────────────────────

/**
 * A PlayController over [kitLibrary] and [kitEngine] (S-1), its events appended to [events] when given
 * (S-4: "emitted synchronously through onEvent"), with `boardDpPerUnit` set to [KIT_DP_PER_UNIT] (S-2:
 * "the screen sets it at each layout, a JVM test sets it").
 */
fun kitController(events: MutableList<PlayEvent>? = null): PlayController {
    val c = if (events == null) {
        PlayController(kitLibrary, kitEngine)
    } else {
        PlayController(kitLibrary, kitEngine) { event -> events += event }
    }
    c.boardDpPerUnit = KIT_DP_PER_UNIT
    return c
}

/** Opens the listed puzzle [id] unless it is already open (S-2 `open`), then sets the board scale again. */
fun kitOpen(c: PlayController, id: String) {
    if (c.puzzle.id != id) assertTrue("open($id) of a listed puzzle", c.open(id))
    c.boardDpPerUnit = KIT_DP_PER_UNIT
}

/** Taps [piece] in the tray until its tray turn is [turn] (S-2 `tapTray`: `Turn.next()`); eight taps reach every turn. */
fun kitTurnInTray(c: PlayController, piece: PieceId, turn: Turn) {
    repeat(8) {
        if (c.trayTurn(piece) == turn) return
        c.tapTray(piece)
    }
    if (c.trayTurn(piece) != turn) throw AssertionError("tapTray($piece) never reached turn ${turn.step}; at ${c.trayTurn(piece).step}")
}

/** Presses the badge while the parallelogram is in the tray until its tray mirror is [mirrored] (S-2 `pressBadge`). */
fun kitMirrorInTray(c: PlayController, mirrored: Boolean) {
    if (c.trayMirrored(PieceId.PG) != mirrored) assertTrue("pressBadge() with PG in the tray", c.pressBadge())
    assertEquals("PG's tray mirror", mirrored, c.trayMirrored(PieceId.PG))
}

/** Turns (and, for the parallelogram, mirrors) [piece] in the tray, then starts its drag at [KIT_TRAY_POINT]. */
fun kitDragFromTray(c: PlayController, piece: PieceId, turn: Turn, mirrored: Boolean) {
    kitTurnInTray(c, piece, turn)
    if (piece == PieceId.PG) kitMirrorInTray(c, mirrored)
    assertTrue("startDrag($piece) from the tray", c.startDrag(piece, KIT_TRAY_POINT))
}

/**
 * Plays [step] with the player's actions only — tapTray / pressBadge in the tray, startDrag, moveDrag
 * over the board, updatePreview, drop — and asserts the drop locks at the planned target (S-2 `drop`:
 * "The lock is the updatePreview() result for the current centre"). Returns the lock.
 */
fun kitPlace(c: PlayController, step: KitStep): Placement {
    kitDragFromTray(c, step.piece, step.target.turn, step.target.mirrored)
    c.moveDrag(step.dropCentre, true)
    c.updatePreview()
    val got = c.drop()
        ?: throw AssertionError("${c.puzzle.id}: the planned drop of ${step.piece} at ${step.dropCentre} went home; the engine locks it at ${step.target}")
    kitAssertSamePlacement(step.target, got, "${c.puzzle.id}: the planned drop of ${step.piece}")
    return got
}

/**
 * A tray piece and turn, unmirrored, that the engine locks nowhere when released at [centre] on [c]'s
 * current board (`findLock` returns `None`). No such piece is a fixture defect.
 */
fun kitMissAt(c: PlayController, centre: Point): Pair<PieceId, Turn> {
    for (piece in c.trayPieces) {
        for (step in 0..7) {
            val result = kitEngine.findLock(c.puzzle, c.placed, piece, Turn(step), false, centre, c.boardDpPerUnit)
            if (result is LockResult.None) return piece to Turn(step)
        }
    }
    throw AssertionError("fixture defect: every tray piece of ${c.puzzle.id} locks somewhere from $centre")
}

// ── Observations ───────────────────────────────────────────────────────────────────────────────

/**
 * Null when every piece of the open puzzle is in exactly one of the three places — `trayPieces`, `placed`,
 * `dragged` — and nothing else is in them; otherwise what is wrong.
 */
fun kitWhereProblem(c: PlayController): String? {
    val tray = c.trayPieces.toList()
    val board = c.placed.map { it.piece }
    val hand = listOfNotNull(c.dragged?.piece)
    val all = tray + board + hand
    val twice = all.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    val nowhere = c.puzzle.pieces - all.toSet()
    val foreign = all.toSet() - c.puzzle.pieces
    if (twice.isEmpty() && nowhere.isEmpty() && foreign.isEmpty()) return null
    return "${c.puzzle.id}: tray $tray, board $board, dragged $hand; in two places $twice, nowhere $nowhere, not the puzzle's $foreign"
}

/** Everything a test can observe on a controller (S-1 reads), to show that a refused action changed nothing. */
data class KitSnapshot(
    val puzzleId: String,
    val state: PuzzleState,
    val placed: List<Placement>,
    val tray: List<PieceId>,
    val trayTurns: Map<PieceId, Turn>,
    val trayMirrors: Map<PieceId, Boolean>,
    val dragged: Dragged?,
    val preview: Placement?,
    val pulse: Pulse?,
    val shake: Shake?,
)

/** The observable state of [c]; tray turns and mirrors are read for the pieces in the tray only. */
fun kitSnapshot(c: PlayController): KitSnapshot {
    val tray = c.trayPieces.toList()
    return KitSnapshot(
        puzzleId = c.puzzle.id,
        state = c.state,
        placed = c.placed.toList(),
        tray = tray,
        trayTurns = tray.associateWith { c.trayTurn(it) },
        trayMirrors = tray.associateWith { c.trayMirrored(it) },
        dragged = c.dragged,
        preview = c.preview,
        pulse = c.pulse,
        shake = c.shake,
    )
}

/** An S-4 event as text ("Turned SQ", "Solved shapes-square"), so a failing order reads plainly. */
fun kitEventText(event: PlayEvent): String = when (event) {
    is PickedUp -> "PickedUp ${event.piece}"
    is Turned -> "Turned ${event.piece}"
    is Locked -> "Locked ${event.piece}"
    is WentHome -> "WentHome ${event.piece}"
    is Solved -> "Solved ${event.puzzleId}"
    else -> "an event S-4 does not name: $event"
}
