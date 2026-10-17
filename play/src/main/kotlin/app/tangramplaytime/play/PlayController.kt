// WO-003 TASK-010 · seams S-1, S-2, S-3 (tasks.md § WO-003 › Seams) · REQ-002, REQ-012, REQ-016, REQ-017,
// REQ-018, REQ-020, REQ-021, REQ-022, REQ-051; TYPE-003, TYPE-006 · architecture.md O-01 / G-04 (every
// position comes from ILockEngine), G-12 (:contracts only), G-14 (no time), G-15 (results, not exceptions).
// WO-004 TASK-018 · seams S-1Δ, S-2Δ, S-20, S-21, the controller side of S-3Δ (designs/WO-004-design.md §2) ·
// REQ-003, REQ-024, REQ-025, REQ-026, REQ-050; TYPE-006 · O-04 as read in decisions.md 2026-10-15 (the
// TYPE-006 transitions stay here).
// WO-004 TASK-021 · one internal read for the grid overview: the listed puzzles (`puzzles`).
package app.tangramplaytime.play

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Turn
import kotlin.math.abs

/**
 * The piece in the player's hand (S-3).
 *
 * @property piece the dragged piece.
 * @property turn its turn now (the tray turn or the placement's turn it left with, then every twist step).
 * @property mirrored its mirror state (the parallelogram only has a visible one, TYPE-003).
 * @property centre where its centre is held, in puzzle units.
 * @property fromTray true when it left the tray, false when it left the board.
 * @property overBoard whether a release now would be over the board (REQ-020: a drop outside the board or on the tray goes home).
 */
data class Dragged(
    val piece: PieceId,
    val turn: Turn,
    val mirrored: Boolean,
    val centre: Point,
    val fromTray: Boolean,
    val overBoard: Boolean,
)

/**
 * One corner pulse of a mini puzzle (REQ-051): the screen animates it once per [serial].
 *
 * @property corners the silhouette's outline corners, `engine.outlineCorners(puzzle)`.
 * @property serial a new value per pulse.
 */
data class Pulse(val corners: List<Point>, val serial: Int)

/**
 * One refused board turn or mirror (REQ-016, REQ-018): the screen shakes [piece] once per [serial].
 *
 * @property piece the piece that kept its place and turn.
 * @property serial a new value per refusal.
 */
data class Shake(val piece: PieceId, val serial: Int)

/**
 * The solve the screen celebrates (S-3Δ; REQ-023, WO-003 N1): set by [PlayController.drop] at the last
 * lock, cleared by every open and by [PlayController.retry]. The screen plays a new [serial] once; while
 * it plays, the final placements still in memory may be drawn (REQ-026.A1, design §3).
 *
 * @property puzzleId the puzzle just solved.
 * @property serial a new value per solve.
 */
data class Celebration(val puzzleId: String, val serial: Int)

/**
 * The Play screen's state holder (S-1, S-2; S-1Δ, S-2Δ): one puzzle open, its TYPE-006 state, the
 * locked pieces, the tray pieces with their turns and mirror, the dragged piece, the landing preview,
 * the pulse, shake and celebration the screen animates, and the moves between puzzles. Every lock,
 * preview, turn or mirror in place, load check and the solved check is [engine]'s answer (O-01, G-04);
 * the controller never derives a position.
 *
 * Reads are Compose snapshot state, readable from a JVM test without a composition. Actions run on the
 * main thread only; there is no time and no animation inside (the screen animates).
 *
 * **What is kept per puzzle** (S-20): after each action, the open puzzle's
 * `PuzzleProgress(state, placed if In progress else [], time, best)` is saved when it differs from the
 * saved one — through [persistence] when there is one, else in this controller's memory. Tray turns and
 * the mirror are not kept (decisions.md 2026-10-11, F36). Every open, the start included, loads the saved
 * progress through the load check (S-21) and saves the last-opened id.
 *
 * @param library the puzzles; construction opens the saved last-opened puzzle when the library lists it,
 *   else `library.puzzles.first()` (the fresh start).
 * @param engine the one lock rule.
 * @param persistence the road to the store (S-19), or null to keep progress in memory only.
 * @param onEvent receives every [PlayEvent] synchronously, after the state changed (O-07).
 */
class PlayController(
    private val library: IPuzzleLibrary,
    internal val engine: ILockEngine,
    private val persistence: Persistence?,
    private val onEvent: (PlayEvent) -> Unit = {},
) {
    /**
     * A controller that keeps progress in memory only (S-1Δ: the WO-003 construction, which passes no
     * [Persistence]); it opens `library.puzzles.first()`.
     */
    constructor(
        library: IPuzzleLibrary,
        engine: ILockEngine,
        onEvent: (PlayEvent) -> Unit = {},
    ) : this(library, engine, null, onEvent)

    /** Each puzzle's progress when there is no [persistence] (S-1Δ "memory only"). */
    private val memory = HashMap<String, PuzzleProgress>()
    private var pulseSerial = 0
    private var shakeSerial = 0
    private var celebrationSerial = 0

    /** The open puzzle's active seconds as read at its open; only Restart and Retry write 0 (S-20, O-04). */
    private var timeSeconds = 0

    /** The drag the current [preview] was searched for, so a drop reuses that search (REQ-021 A1). */
    private var previewFor: Dragged? = null

    /** The open puzzle. */
    var puzzle: PuzzleDefinition by mutableStateOf(startPuzzle())
        private set

    /** The open puzzle's TYPE-006 state. */
    var state: PuzzleState by mutableStateOf(PuzzleState.NEW)
        private set

    /** The pieces on the board in lock order; every entry is a lock the engine found. */
    var placed: List<Placement> by mutableStateOf(emptyList())
        private set

    /** The piece in the hand, or null. */
    var dragged: Dragged? by mutableStateOf(null)
        private set

    /** Where the dragged piece would lock if released now (REQ-021), or null: no drag, not over the board, or nothing in reach. */
    var preview: Placement? by mutableStateOf(null)
        private set

    /** The corner pulse to show (REQ-051), or null; a new serial per pulse. */
    var pulse: Pulse? by mutableStateOf(null)
        private set

    /** The refused turn or mirror to shake (REQ-016, REQ-018), or null; a new serial per refusal. */
    var shake: Shake? by mutableStateOf(null)
        private set

    /** The solve to celebrate (S-3Δ), or null: set at the last lock, cleared by every open and by [retry]. */
    var celebration: Celebration? by mutableStateOf(null)
        private set

    /** The open puzzle's best time in seconds as saved, or null when none is saved (S-1Δ; kept by Restart and Retry). */
    var bestSeconds: Int? by mutableStateOf(null)
        private set

    /** Whether the grid overview is open (S-1Δ, S-2Δ). It lives here, so it survives a recreation but not a process death. */
    var overviewOpen: Boolean by mutableStateOf(false)
        private set

    private var trayTurns: Map<PieceId, Turn> by mutableStateOf(emptyMap())
    private var trayMirrors: Map<PieceId, Boolean> by mutableStateOf(emptyMap())

    /** The open puzzle's place in the ‹ › order, 1-based (S-1Δ; REQ-024 rule: "its number out of the total"). */
    val puzzleNumber: Int
        get() = indexOfOpen() + 1

    /** How many puzzles the library lists (S-1Δ). */
    val puzzleCount: Int
        get() = library.puzzles.size

    /**
     * Every listed puzzle in ‹ › order (REQ-040): what the grid overview draws, one cell each (S-22 "Overview"; REQ-050
     * A2). WO-004 TASK-021: the screen receives only the controller, and S-1Δ's reads name no list (hand-back note).
     */
    internal val puzzles: List<PuzzleDefinition>
        get() = library.puzzles

    /**
     * The TYPE-006 state of puzzle [id] (S-1Δ): the open puzzle's [state], else its saved state (New when
     * none is saved). Reading it reads [puzzle], so a reader sees a change of the open puzzle; another
     * puzzle's saved state changes only while it is open.
     */
    fun stateOf(id: String): PuzzleState = if (id == puzzle.id) state else savedOf(id).state

    /**
     * The board's scale, dp per puzzle unit, passed to every engine search (the 30 dp floor of the lock
     * distance, TYPE-004). The screen sets it at each layout; a JVM test sets it. A search while it is
     * not > 0 throws [IllegalStateException] (a programming error, G-15).
     */
    var boardDpPerUnit: Double = 0.0

    /** The puzzle's pieces that are neither placed nor dragged, in tray order (TYPE-001). */
    val trayPieces: Set<PieceId>
        get() {
            val onBoard = placed.mapTo(HashSet()) { it.piece }
            val inHand = dragged?.piece
            return puzzle.pieces.filterTo(LinkedHashSet()) { it !in onBoard && it != inHand }
        }

    init {
        // The start is an open (S-21 "On every open, the start included"; S-20 "Each open calls saveLastOpened").
        load(puzzle)
    }

    /** The turn [piece] has in the tray (its S-9 resting turn until tapped, twisted or brought home turned). */
    fun trayTurn(piece: PieceId): Turn = trayTurns[piece] ?: TrayRest.turn(piece)

    /** Whether [piece] is mirrored in the tray (false until the badge is pressed or it comes home mirrored). */
    fun trayMirrored(piece: PieceId): Boolean = trayMirrors[piece] ?: false

    // ── Opening ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Opens the listed puzzle [id]. A running drag ends as [goHome] first. The puzzle comes back as saved,
     * through the load check (S-21): In progress with the board pieces that are still valid locks, New or
     * Solved with an empty board (REQ-026.A1). Its tray turns and mirror reset to rest (decisions.md
     * 2026-10-11, F36); the celebration is cleared (S-3Δ); the last-opened id is saved (S-20).
     *
     * @return false, and nothing changes, for an id the library does not list (G-15).
     */
    fun open(id: String): Boolean {
        val next = library.byId(id) ?: return false
        goHome()
        load(next)
        return true
    }

    /** › (S-2Δ, REQ-024): opens the next puzzle of the ‹ › order; the last one's next is the first. */
    fun next() {
        openAt(indexOfOpen() + 1)
    }

    /** ‹ (S-2Δ, REQ-024): opens the previous puzzle of the ‹ › order; the first one's previous is the last. */
    fun previous() {
        openAt(indexOfOpen() - 1)
    }

    /**
     * The long press on › (S-2Δ, REQ-050): opens the first puzzle after the open one, wrapping, that is not
     * Solved. When that is the open puzzle itself, nothing changes; when every puzzle is Solved, it acts as
     * [next] (REQ-050 rule). A retried puzzle is New, so it counts as unsolved (decisions.md 2026-10-15
     * "Browsing readings no REQ fixes").
     */
    fun nextUnsolved() {
        val puzzles = library.puzzles
        val from = indexOfOpen()
        for (step in 1..puzzles.size) {
            val at = (from + step) % puzzles.size
            if (stateOf(puzzles[at].id) == PuzzleState.SOLVED) continue
            if (at != from) openAt(at)
            return
        }
        next()
    }

    /** Opens the grid overview (S-2Δ, REQ-050); no puzzle changes. */
    fun openOverview() {
        overviewOpen = true
    }

    /** Closes the grid overview (S-2Δ); the open puzzle stays as it is (REQ-050 rule). */
    fun closeOverview() {
        overviewOpen = false
    }

    /**
     * One press on [id]'s grid cell (S-2Δ, REQ-050): closes the overview, then opens [id] unless it is the
     * open puzzle, which then stays as it is (decisions.md 2026-10-15 "Browsing readings no REQ fixes").
     *
     * @return false for an id the library does not list (the overview is closed; nothing else changes).
     */
    fun choose(id: String): Boolean {
        closeOverview()
        return id == puzzle.id || open(id)
    }

    /**
     * Restart (S-2Δ, REQ-025): an In-progress puzzle becomes New with an empty board, time 0 and its best
     * time kept, every piece in the tray at its resting turn, unmirrored. A running drag ends as [goHome]
     * first.
     *
     * @return false, and nothing changes, when the puzzle is not In progress (Restart is shown only then).
     */
    fun restart(): Boolean {
        if (state != PuzzleState.IN_PROGRESS) return false
        goHome()
        startOver()
        return true
    }

    /**
     * Retry (S-2Δ, REQ-026): a Solved puzzle becomes New with an empty board, time 0 and its best time kept,
     * every piece in the tray at its resting turn, unmirrored; the celebration is cleared (S-3Δ).
     *
     * @return false, and nothing changes, when the puzzle is not Solved (Retry is shown only then).
     */
    fun retry(): Boolean {
        if (state != PuzzleState.SOLVED) return false
        startOver()
        return true
    }

    /**
     * Returns once every save queued so far is on the store (S-2Δ: delegates to [Persistence.flush]); at
     * once for a controller that keeps progress in memory only.
     */
    suspend fun flush() {
        persistence?.flush()
    }

    /** The puzzle a construction opens (S-1Δ): the saved last-opened one when listed, else the first. */
    private fun startPuzzle(): PuzzleDefinition =
        persistence?.lastOpenedId?.let { library.byId(it) } ?: library.puzzles.first()

    private fun indexOfOpen(): Int {
        val id = puzzle.id
        return library.puzzles.indexOfFirst { it.id == id }
    }

    /** Opens the puzzle at [index] of the ‹ › order, wrapping at both ends. */
    private fun openAt(index: Int) {
        val puzzles = library.puzzles
        open(puzzles[Math.floorMod(index, puzzles.size)].id)
    }

    /**
     * Shows [next] as saved, through the load check (S-21): In progress → the saved placements that are
     * the puzzle's, the first of any duplicate, then `stillValid` (the state stays In progress even when
     * the board comes back empty; decisions.md 2026-10-15 "REQ-025's load check"); New or Solved → an empty
     * board. Every tray piece at rest, nothing dragged, no cue. A correction is saved (S-20, S-21), and the
     * last-opened id is saved (S-20).
     */
    private fun load(next: PuzzleDefinition) {
        val saved = savedOf(next.id)
        puzzle = next
        state = saved.state
        placed = when (saved.state) {
            PuzzleState.IN_PROGRESS ->
                engine.stillValid(next, saved.placed.filter { it.piece in next.pieces }.distinctBy { it.piece })
            PuzzleState.NEW, PuzzleState.SOLVED -> emptyList()
        }
        timeSeconds = saved.timeSeconds
        bestSeconds = saved.bestSeconds
        trayTurns = next.pieces.associateWith { TrayRest.turn(it) }
        trayMirrors = emptyMap()
        dragged = null
        clearPreview()
        pulse = null
        shake = null
        celebration = null
        keep()
        persistence?.saveLastOpened(next.id)
    }

    /** Restart and Retry: New, an empty board, time 0, the best time kept, every tray piece at rest, no cue. */
    private fun startOver() {
        state = PuzzleState.NEW
        placed = emptyList()
        timeSeconds = 0
        trayTurns = puzzle.pieces.associateWith { TrayRest.turn(it) }
        trayMirrors = emptyMap()
        dragged = null
        clearPreview()
        pulse = null
        shake = null
        celebration = null
        keep()
    }

    // ── What is kept per puzzle (S-20) ───────────────────────────────────────────────────────────

    /** Puzzle [id]'s saved progress: [persistence]'s cache, or this controller's memory; FRESH when none. */
    private fun savedOf(id: String): PuzzleProgress =
        persistence?.progress(id) ?: memory[id] ?: PuzzleProgress.FRESH

    /**
     * S-20, after each action: the open puzzle's progress in the save shape — its board only In progress
     * (a Solved puzzle's placements are never kept, REQ-026.A1) — is saved when it differs from the saved
     * one. Times pass through as read (O-04; WO-008 counts them).
     */
    private fun keep() {
        val now = PuzzleProgress(
            state = state,
            placed = if (state == PuzzleState.IN_PROGRESS) placed else emptyList(),
            timeSeconds = timeSeconds,
            bestSeconds = bestSeconds,
        )
        if (now == savedOf(puzzle.id)) return
        if (persistence != null) persistence.save(puzzle.id, now) else memory[puzzle.id] = now
    }

    // ── Turning and mirroring in place ───────────────────────────────────────────────────────────

    /** REQ-016: turns a tray piece one step clockwise (`Turn.next`); no TYPE-006 change. Nothing happens for a piece not in the tray. */
    fun tapTray(piece: PieceId) {
        if (piece !in trayPieces) return
        trayTurns = trayTurns + (piece to trayTurn(piece).next())
        onEvent(PlayEvent.Turned(piece))
    }

    /**
     * REQ-016: turns a board piece one step clockwise about its centre, kept only if it locks there
     * (`tryTurnInPlace`).
     *
     * @return true when the turn is kept (the placement is replaced; `Turned`); false when the engine
     *   finds no lock (nothing changes; a new [shake] for the piece), when [piece] is not on the board, or
     *   once the puzzle is solved.
     */
    fun tapBoard(piece: PieceId): Boolean {
        if (state == PuzzleState.SOLVED) return false
        val current = placed.firstOrNull { it.piece == piece } ?: return false
        requireBoardScale()
        val result = engine.tryTurnInPlace(puzzle, placed - current, current, current.turn.next(), boardDpPerUnit)
        return keepOrShake(current, result)
    }

    /**
     * REQ-018: the flip badge mirrors the parallelogram — in the tray by toggling its tray mirror, on the
     * board only if the mirrored piece still locks there (`tryMirrorInPlace`).
     *
     * @return true when the mirror is kept (`Turned`); false when the board mirror finds no lock (nothing
     *   changes; a new [shake]), when the puzzle has no parallelogram, while it is dragged, or once the
     *   puzzle is solved.
     */
    fun pressBadge(): Boolean {
        val pg = PieceId.PG
        if (pg !in puzzle.pieces || state == PuzzleState.SOLVED || dragged?.piece == pg) return false
        val current = placed.firstOrNull { it.piece == pg }
        if (current == null) {
            trayMirrors = trayMirrors + (pg to !trayMirrored(pg))
            onEvent(PlayEvent.Turned(pg))
            return true
        }
        requireBoardScale()
        val result = engine.tryMirrorInPlace(puzzle, placed - current, current, boardDpPerUnit)
        return keepOrShake(current, result)
    }

    /** Replaces [current] by the engine's lock and reports `Turned`, or shakes the unchanged piece. */
    private fun keepOrShake(current: Placement, result: LockResult): Boolean = when (result) {
        is LockResult.Locked -> {
            placed = placed.map { if (it === current) result.placement else it }
            onEvent(PlayEvent.Turned(current.piece))
            keep()
            true
        }
        LockResult.None -> {
            shake = Shake(current.piece, ++shakeSerial)
            false
        }
    }

    // ── The drag ─────────────────────────────────────────────────────────────────────────────────

    /**
     * The player picks [piece] up at [centre] (puzzle units): it leaves the tray with its tray turn and
     * mirror, or the board with its placement's turn and mirror. A start from the tray moves a New
     * puzzle to In progress (TYPE-006). `PickedUp`.
     *
     * @return false, and nothing changes, when [piece] is not the puzzle's, a drag is running, or the
     *   puzzle is solved.
     */
    fun startDrag(piece: PieceId, centre: Point): Boolean {
        if (piece !in puzzle.pieces || dragged != null || state == PuzzleState.SOLVED) return false
        val current = placed.firstOrNull { it.piece == piece }
        if (current != null) {
            placed = placed - current
            dragged = Dragged(piece, current.turn, current.mirrored, centre, fromTray = false, overBoard = true)
        } else {
            dragged = Dragged(piece, trayTurn(piece), trayMirrored(piece), centre, fromTray = true, overBoard = false)
            if (state == PuzzleState.NEW) state = PuzzleState.IN_PROGRESS
        }
        onEvent(PlayEvent.PickedUp(piece))
        keep()
        return true
    }

    /** The finger moved: the dragged centre is now [centre] and a release now would be over the board when [overBoard]. No search. */
    fun moveDrag(centre: Point, overBoard: Boolean) {
        val d = dragged ?: return
        dragged = d.copy(centre = centre, overBoard = overBoard)
    }

    /** REQ-017: turns the dragged piece [steps] 45° steps, positive clockwise (`next`), negative counter-clockwise (`previous`); one `Turned` per step. */
    fun twistDrag(steps: Int) {
        val d = dragged ?: return
        var turn = d.turn
        repeat(abs(steps)) {
            turn = if (steps > 0) turn.next() else turn.previous()
            dragged = dragged?.copy(turn = turn)
            onEvent(PlayEvent.Turned(d.piece))
        }
    }

    /**
     * REQ-021: [preview] becomes where the dragged piece would lock if released now — the `findLock`
     * answer for its centre, turn and mirror — or null when nothing is in reach, the finger is not over
     * the board, or nothing is dragged. The screen calls it at most once per displayed frame.
     */
    fun updatePreview() {
        val d = dragged
        if (d == null) {
            clearPreview()
            return
        }
        preview = searchLock(d)
        previewFor = d
    }

    /**
     * The player releases the piece. The lock is the [updatePreview] result for the current centre,
     * computed now if not yet, so a release locks where the preview was (REQ-021 A1).
     *
     * @return the lock, appended to [placed] (`Locked`; then, when every piece is on the board, state
     *   Solved, a new [celebration] and `Solved`), or null when the piece went back to its tray cell
     *   (REQ-020; `WentHome`; a [pulse] in a mini puzzle, REQ-051). Null, and nothing happens, without a
     *   drag.
     */
    fun drop(): Placement? {
        val d = dragged ?: return null
        val lock = if (previewFor == d) preview else searchLock(d)
        dragged = null
        clearPreview()
        if (lock == null) {
            sendHome(d)
            if (puzzle.kind == PuzzleKind.MINI) pulse = Pulse(engine.outlineCorners(puzzle), ++pulseSerial)
            keep()
            return null
        }
        placed = placed + lock
        onEvent(PlayEvent.Locked(d.piece))
        if (engine.isSolved(puzzle, placed)) {
            state = PuzzleState.SOLVED
            celebration = Celebration(puzzle.id, ++celebrationSerial)
            onEvent(PlayEvent.Solved(puzzle.id))
        }
        keep()
        return lock
    }

    /**
     * A drag ended without a release (a pointer cancel, the activity stopping): the piece goes back to
     * its tray cell as after a missed drop, `WentHome`, but with no pulse (decisions.md 2026-10-11). A
     * no-op without a drag.
     */
    fun goHome() {
        val d = dragged ?: return
        dragged = null
        clearPreview()
        sendHome(d)
        keep()
    }

    /** The dragged piece becomes a tray piece again, keeping the turn and mirror it was held at (DI-1), and `WentHome`. */
    private fun sendHome(d: Dragged) {
        trayTurns = trayTurns + (d.piece to d.turn)
        trayMirrors = trayMirrors + (d.piece to d.mirrored)
        onEvent(PlayEvent.WentHome(d.piece))
    }

    /** The lock for [d] released now: null when not over the board, else the engine's `findLock` placement or null for `None`. */
    private fun searchLock(d: Dragged): Placement? {
        if (!d.overBoard) return null
        requireBoardScale()
        return when (val result = engine.findLock(puzzle, placed, d.piece, d.turn, d.mirrored, d.centre, boardDpPerUnit)) {
            is LockResult.Locked -> result.placement
            LockResult.None -> null
        }
    }

    private fun clearPreview() {
        preview = null
        previewFor = null
    }

    /** G-15: searching without a board scale is a programming error of the screen, not an outcome. */
    private fun requireBoardScale() {
        if (!(boardDpPerUnit > 0)) throw IllegalStateException("boardDpPerUnit must be > 0 before a lock search, was $boardDpPerUnit")
    }
}
