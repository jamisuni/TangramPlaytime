// WO-003 TASK-010 · seams S-1, S-2, S-3 (tasks.md § WO-003 › Seams) · REQ-002, REQ-012, REQ-016, REQ-017,
// REQ-018, REQ-020, REQ-021, REQ-022, REQ-051; TYPE-003, TYPE-006 · architecture.md O-01 / G-04 (every
// position comes from ILockEngine), G-12 (:contracts only), G-14 (no time), G-15 (results, not exceptions).
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
 * The Play screen's state holder (S-1, S-2): one puzzle open, its TYPE-006 state, the locked pieces,
 * the tray pieces with their turns and mirror, the dragged piece, the landing preview, and the pulse
 * and shake the screen animates. Every lock, preview, turn or mirror in place and the solved check is
 * [engine]'s answer (O-01, G-04); the controller never derives a position.
 *
 * Reads are Compose snapshot state, readable from a JVM test without a composition. Actions run on the
 * main thread only; there is no time and no animation inside (the screen animates). State and board
 * placements are kept in memory per puzzle id across opens (WO-004 replaces the memory with
 * `IProgressStore`); tray turns and the mirror are not part of that state (decisions.md 2026-10-11, F36).
 *
 * @param library the puzzles; construction opens `library.puzzles.first()` (the fresh start; WO-004
 *   later opens the last-opened puzzle).
 * @param engine the one lock rule.
 * @param onEvent receives every [PlayEvent] synchronously, after the state changed (O-07).
 */
class PlayController(
    private val library: IPuzzleLibrary,
    internal val engine: ILockEngine,
    private val onEvent: (PlayEvent) -> Unit = {},
) {
    /** What is remembered per puzzle id while another puzzle is open: its TYPE-006 state and its board. */
    private class Kept(val state: PuzzleState, val placed: List<Placement>)

    private val memory = HashMap<String, Kept>()
    private var pulseSerial = 0
    private var shakeSerial = 0

    /** The drag the current [preview] was searched for, so a drop reuses that search (REQ-021 A1). */
    private var previewFor: Dragged? = null

    /** The open puzzle. */
    var puzzle: PuzzleDefinition by mutableStateOf(library.puzzles.first())
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

    private var trayTurns: Map<PieceId, Turn> by mutableStateOf(emptyMap())
    private var trayMirrors: Map<PieceId, Boolean> by mutableStateOf(emptyMap())

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
        load(puzzle)
    }

    /** The turn [piece] has in the tray (its S-9 resting turn until tapped, twisted or brought home turned). */
    fun trayTurn(piece: PieceId): Turn = trayTurns[piece] ?: TrayRest.turn(piece)

    /** Whether [piece] is mirrored in the tray (false until the badge is pressed or it comes home mirrored). */
    fun trayMirrored(piece: PieceId): Boolean = trayMirrors[piece] ?: false

    // ── Opening ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Opens the listed puzzle [id]. A running drag ends as [goHome] first. The puzzle's state and board
     * come back as left (a Solved puzzle keeps its final placements); its tray turns and mirror reset to
     * rest (decisions.md 2026-10-11, F36).
     *
     * @return false, and nothing changes, for an id the library does not list (G-15).
     */
    fun open(id: String): Boolean {
        val next = library.byId(id) ?: return false
        goHome()
        memory[puzzle.id] = Kept(state, placed)
        load(next)
        return true
    }

    /** Shows [next] with its remembered state and board (or New and empty), every tray piece at rest. */
    private fun load(next: PuzzleDefinition) {
        val kept = memory[next.id]
        puzzle = next
        state = kept?.state ?: PuzzleState.NEW
        placed = kept?.placed ?: emptyList()
        trayTurns = next.pieces.associateWith { TrayRest.turn(it) }
        trayMirrors = emptyMap()
        dragged = null
        clearPreview()
        pulse = null
        shake = null
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
     *   Solved and `Solved`), or null when the piece went back to its tray cell (REQ-020; `WentHome`;
     *   a [pulse] in a mini puzzle, REQ-051). Null, and nothing happens, without a drag.
     */
    fun drop(): Placement? {
        val d = dragged ?: return null
        val lock = if (previewFor == d) preview else searchLock(d)
        dragged = null
        clearPreview()
        if (lock == null) {
            sendHome(d)
            if (puzzle.kind == PuzzleKind.MINI) pulse = Pulse(engine.outlineCorners(puzzle), ++pulseSerial)
            return null
        }
        placed = placed + lock
        onEvent(PlayEvent.Locked(d.piece))
        if (engine.isSolved(puzzle, placed)) {
            state = PuzzleState.SOLVED
            onEvent(PlayEvent.Solved(puzzle.id))
        }
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
