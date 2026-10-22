// WO-003 TASK-010 · seams S-1, S-2, S-3 (tasks.md § WO-003 › Seams) · REQ-002, REQ-012, REQ-016, REQ-017,
// REQ-018, REQ-020, REQ-021, REQ-022, REQ-051; TYPE-003, TYPE-006 · architecture.md O-01 / G-04 (every
// position comes from ILockEngine), G-12 (:contracts only), G-14 (no time), G-15 (results, not exceptions).
// WO-004 TASK-018 · seams S-1Δ, S-2Δ, S-20, S-21, the controller side of S-3Δ (designs/WO-004-design.md §2) ·
// REQ-003, REQ-024, REQ-025, REQ-026, REQ-050; TYPE-006 · O-04 as read in decisions.md 2026-10-15 (the
// TYPE-006 transitions stay here).
// WO-004 TASK-021 · one internal read for the grid overview: the listed puzzles (`puzzles`).
// WO-008 TASK-046 · seams S-1Δ3, S-2Δ3, S-20Δ (designs/WO-008-design.md §2) · REQ-005, REQ-030 (statement, rules 1, 2
// and 4), TYPE-006 rule 4 · O-04 as read in decisions.md 2026-10-20 "O-04 read for WO-008 …" with its F2 amendment (the
// puzzle time and the best time are kept here; TYPE-005 and the play times are `:kernel`'s, behind IActiveTime) ·
// G-13 (every write through Persistence), G-14 (time only through IActiveTime's counts) · decisions.md 2026-10-20 "What
// counts on each screen.", "An aid solve keeps the best time through one generic controller member." (as amended), "When
// the times are written, and what a process death may lose.".
// WO-007 TASK-054 · seams S-1Δ4, S-2Δ4 (designs/WO-007-design.md §2) · REQ-032 (statement, rule 2, A1), REQ-031, REQ-033,
// REQ-030 (rules 1 and 3), REQ-034; TYPE-005, TYPE-006 · G-13 (every write through Persistence's one FIFO) · decisions.md
// 2026-10-21 "The settings screen sits in the activity's window, pauses the puzzle time …", "The two switches write
// through `PlayController` and `Persistence` …", "Play time and best times on the settings screen.", "Reset all
// progress, in order, and where the player is afterwards.".
package app.tangramplaytime.play

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.tangramplaytime.contracts.IActiveTime
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PlayTimes
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Settings
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
 * One row of the settings screen's best times (S-1Δ4; REQ-030 rule 3; decisions.md 2026-10-21 "Play time and best times
 * on the settings screen."): a listed puzzle with a stored best time.
 *
 * @property puzzle the puzzle; the screen shows its title in the game's language.
 * @property seconds its best time, in active seconds (TYPE-005).
 */
data class BestTime(val puzzle: PuzzleDefinition, val seconds: Int)

/**
 * S-20Δ's cadence: the play times, then the open puzzle, are written once this many play seconds have been counted since
 * the last such write (decisions.md 2026-10-20 "When the times are written, and what a process death may lose.": "every 10
 * counted play seconds"; at most 360 rewrites per hour of play, ADR-003). Not DI-1's 5 s (`design-inputs.md` §2 item 11).
 */
private const val SAVE_EVERY_PLAY_SECONDS = 10

/**
 * The Play screen's state holder (S-1, S-2; S-1Δ, S-2Δ; S-1Δ3, S-2Δ3; S-1Δ4, S-2Δ4): one puzzle open, its TYPE-006
 * state, the locked pieces, the tray pieces with their turns and mirror, the dragged piece, the landing preview,
 * the pulse, shake and celebration the screen animates, the moves between puzzles, the open puzzle's time
 * and best time (REQ-030; O-04), and the settings screen's flag, the settings, the best times and Reset all progress
 * (REQ-031, REQ-032, REQ-033, REQ-034). Every lock, preview, turn or mirror in place, load check and the solved check
 * is [engine]'s answer (O-01, G-04); the controller never derives a position.
 *
 * Reads are Compose snapshot state, readable from a JVM test without a composition. Actions run on the
 * main thread only, and so do [countTime] and [setVisible] (S-44 "One caller thread"). There is no clock and no
 * animation inside: time arrives only as [time]'s whole counted seconds (G-14), and the screen animates.
 *
 * **The fold** (S-2Δ3; decisions.md 2026-10-20 "Amends three 2026-10-20 rows … F2 option (a) …"): `n = time.count()`
 * is added to [timeSeconds] when the open puzzle is In progress, the grid overview is closed and the settings screen is
 * closed (S-2Δ4) — the predicate as it stands at the fold — and [playTimes] is read again. Every change of that
 * predicate folds first ([open], [restart], [retry], a tray [startDrag], the solving [drop], [openOverview],
 * [closeOverview], [openSettings], [closeSettings], [resetAll]), and so does [setVisible] before it tells the kernel; the
 * predicate therefore held for the whole interval since the previous fold, and the seconds go to the puzzle they were
 * spent on. [countTime] folds, then writes on S-20Δ's cadence. The construction does not fold.
 *
 * **What is kept per puzzle** (S-20, S-20Δ): after each action, the open puzzle's
 * `PuzzleProgress(state, placed if In progress else [], time, best)` is saved when it differs from the
 * saved one — through [persistence] when there is one, else in this controller's memory. Tray turns and
 * the mirror are not kept (decisions.md 2026-10-11, F36). Every open, the start included, loads the saved
 * progress through the load check (S-21) and saves the last-opened id; every open but the start first saves the
 * leaving puzzle with its folded time.
 *
 * @param library the puzzles; construction opens the saved last-opened puzzle when the library lists it,
 *   else `library.puzzles.first()` (the fresh start).
 * @param engine the one lock rule.
 * @param persistence the road to the store (S-19), or null to keep progress in memory only.
 * @param time the kernel's TYPE-005 accounting (S-44), or null: a controller without it counts nothing, so its
 *   puzzle times only pass through as read (S-1Δ3; tasks.md § WO-008 seam note 1).
 * @param onEvent receives every [PlayEvent] synchronously, after the state changed (O-07).
 */
class PlayController(
    private val library: IPuzzleLibrary,
    internal val engine: ILockEngine,
    private val persistence: Persistence?,
    private val time: IActiveTime?,
    private val onEvent: (PlayEvent) -> Unit = {},
) {
    /**
     * A controller over [persistence] that counts no time (S-1Δ3: the WO-004 construction, which passes no
     * [IActiveTime]; every existing construction compiles unchanged).
     */
    constructor(
        library: IPuzzleLibrary,
        engine: ILockEngine,
        persistence: Persistence?,
        onEvent: (PlayEvent) -> Unit = {},
    ) : this(library, engine, persistence, null, onEvent)

    /**
     * A controller that keeps progress in memory only and counts no time (S-1Δ: the WO-003 construction, which
     * passes no [Persistence]); it opens `library.puzzles.first()`.
     */
    constructor(
        library: IPuzzleLibrary,
        engine: ILockEngine,
        onEvent: (PlayEvent) -> Unit = {},
    ) : this(library, engine, null, null, onEvent)

    /** Each puzzle's progress when there is no [persistence] (S-1Δ "memory only"). */
    private val memory = HashMap<String, PuzzleProgress>()
    private var pulseSerial = 0
    private var shakeSerial = 0
    private var celebrationSerial = 0

    /** True while [solveWithoutBest]'s drive runs: a solve then keeps the best time as it was (S-2Δ3; REQ-030 rule 4). */
    private var withheld = false

    /** Play seconds counted since the play times were last written (S-20Δ's cadence). */
    private var unsavedPlaySeconds = 0

    /**
     * The open puzzle's active seconds (S-1Δ3; REQ-030; TYPE-005): read at its open, then each fold adds the seconds
     * counted while it is In progress and the grid overview is closed; Restart and Retry write 0 (REQ-030 rule 2). For
     * a Solved puzzle it is the time of its solve (REQ-005.A1; the PuzzleProgress KDoc).
     */
    var timeSeconds: Int by mutableStateOf(0)
        private set

    /**
     * Today's and all-time play time (S-1Δ3; REQ-005.A2, REQ-029) as the kernel reports them, read again at every
     * fold; [PlayTimes.NONE] for a controller that counts no time.
     */
    var playTimes: PlayTimes by mutableStateOf(time?.playTimes() ?: PlayTimes.NONE)
        private set

    /**
     * Whether the settings screen is open (S-1Δ4; REQ-032): false at the start, set by [openSettings] and [closeSettings].
     * It lives here, so it survives a recreation but not a process death (decisions.md 2026-10-21 "The settings screen
     * sits in the activity's window …").
     */
    var settingsOpen: Boolean by mutableStateOf(false)
        private set

    /**
     * The player's settings (S-1Δ4; REQ-031, REQ-033): this controller's own snapshot state, seeded once from
     * [persistence]'s settings, else the defaults (timer off, sound on). [setTimerShown] and [setSoundOn] replace it, so
     * every reader sees a switch at once (N1; decisions.md 2026-10-21 "The two switches write through `PlayController` …").
     */
    var settings: Settings by mutableStateOf(persistence?.settings ?: Settings())
        private set

    /** Whether the running puzzle time is shown on the board (S-1Δ4; REQ-031): [settings]' timer setting. */
    val timerShown: Boolean
        get() = settings.timerShown

    /**
     * The settings screen's best times (S-1Δ4; REQ-030 rule 3): one row per listed puzzle with a stored best time, in ‹ ›
     * order; the open puzzle's is its [bestSeconds]. A puzzle never solved, or solved only by the developer aid (REQ-030
     * rule 4), has none. Refreshed by [openSettings], and empty after [resetAll]; empty until the first [openSettings].
     */
    var bestTimes: List<BestTime> by mutableStateOf(emptyList())
        private set

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
        // The start is an open (S-21 "On every open, the start included"; S-20 "Each open calls saveLastOpened"). It
        // neither folds nor saves a leaving puzzle (S-2Δ3): there is none, and a leaving save here would write the
        // property defaults over the saved puzzle (design §3). A resumed In-progress puzzle counts from the first fold.
        load(puzzle)
    }

    /** The turn [piece] has in the tray (its S-9 resting turn until tapped, twisted or brought home turned). */
    fun trayTurn(piece: PieceId): Turn = trayTurns[piece] ?: TrayRest.turn(piece)

    /** Whether [piece] is mirrored in the tray (false until the badge is pressed or it comes home mirrored). */
    fun trayMirrored(piece: PieceId): Boolean = trayMirrors[piece] ?: false

    // ── Opening ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Opens the listed puzzle [id]. The counted seconds are folded into the leaving puzzle first (S-2Δ3), a
     * running drag ends as [goHome], and the leaving puzzle is saved with its time (S-20Δ). The puzzle comes back
     * as saved, through the load check (S-21): In progress with the board pieces that are still valid locks, New or
     * Solved with an empty board (REQ-026.A1). Its tray turns and mirror reset to rest (decisions.md
     * 2026-10-11, F36); the celebration is cleared (S-3Δ); the last-opened id is saved (S-20).
     *
     * @return false, and nothing changes, for an id the library does not list (G-15).
     */
    fun open(id: String): Boolean {
        val next = library.byId(id) ?: return false
        fold()
        goHome()
        keep()
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

    /**
     * Opens the grid overview (S-2Δ, REQ-050); no puzzle changes. It folds first (S-2Δ3): the puzzle time pauses while
     * the grid shows the other puzzles (REQ-030 rule 1; decisions.md 2026-10-20 "What counts on each screen.").
     */
    fun openOverview() {
        fold()
        overviewOpen = true
    }

    /** Closes the grid overview (S-2Δ); the open puzzle stays as it is (REQ-050 rule). It folds first (S-2Δ3). */
    fun closeOverview() {
        fold()
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

    // ── The settings screen (S-2Δ4) ──────────────────────────────────────────────────────────────

    /**
     * The gear opens the settings screen over the open puzzle (S-2Δ4; REQ-032 statement): the fold first, then the flag,
     * so the seconds before the opening are the puzzle's and those after it are play time only (REQ-032 rule 2; REQ-030
     * rule 1; decisions.md 2026-10-20 "What counts on each screen."); then [bestTimes] is refreshed. Nothing on the board
     * changes (REQ-032.A1): no placement, state, tray turn, open puzzle or puzzle time.
     */
    fun openSettings() {
        fold()
        settingsOpen = true
        bestTimes = listedBestTimes()
    }

    /**
     * Closes the settings screen (S-2Δ4; REQ-032 statement: back to the same puzzle state): the fold first, so the
     * seconds spent on the screen stay play time only, then the flag. Nothing on the board changes (REQ-032.A1).
     */
    fun closeSettings() {
        fold()
        settingsOpen = false
    }

    /**
     * The timer switch (S-2Δ4; REQ-031): [settings] is replaced at once, so the board's timer follows on this controller
     * (N1), then the setting is saved through [persistence]'s one FIFO (G-13).
     */
    fun setTimerShown(shown: Boolean) {
        settings = settings.copy(timerShown = shown)
        persistence?.saveSettings(settings)
    }

    /**
     * The sound switch (S-2Δ4; REQ-033): [settings] is replaced at once, so every reader of the sound setting follows
     * (N1), then the setting is saved through [persistence]'s one FIFO (G-13).
     */
    fun setSoundOn(on: Boolean) {
        settings = settings.copy(soundOn = on)
        persistence?.saveSettings(settings)
    }

    /**
     * The confirmed Reset all progress (S-2Δ4; REQ-034 statement and rule 2; decisions.md 2026-10-21 "Reset all progress,
     * in order, and where the player is afterwards."), in this order, every step on the caller's (main) thread:
     * 1. the fold, so the seconds before the reset are counted under the predicate that held;
     * 2. the kernel's play times are erased ([IActiveTime.resetPlayTimes]);
     * 3. [persistence] queues the store's reset behind every earlier write and empties its cache, keeping the settings and
     *    the last-opened id ([Persistence.resetAll]); without one, this controller's memory is cleared;
     * 4. the open puzzle is shown again as now saved: New, an empty board, every piece in the tray at rest, time 0, no
     *    best time. Nothing is saved for it but the last-opened id;
     * 5. [playTimes] is read again, the play seconds of S-20Δ's cadence start from 0, and [bestTimes] is empty.
     *
     * It never calls [open], whose leaving save would write the pre-reset board after the store's reset (design §3
     * "Reset order"), and it emits no [PlayEvent]. The player stays on the open puzzle and [settingsOpen] is unchanged.
     * The confirmation is the caller's (REQ-034 rule 1, A2).
     */
    fun resetAll() {
        fold()
        time?.resetPlayTimes()
        if (persistence != null) persistence.resetAll() else memory.clear()
        load(puzzle)
        playTimes = time?.playTimes() ?: PlayTimes.NONE
        unsavedPlaySeconds = 0
        bestTimes = emptyList()
    }

    /** [bestTimes]' rows as now saved (S-1Δ4): the listed puzzles with a best time, in ‹ › order; the open one's [bestSeconds]. */
    private fun listedBestTimes(): List<BestTime> = library.puzzles.mapNotNull { p ->
        val best = if (p.id == puzzle.id) bestSeconds else savedOf(p.id).bestSeconds
        best?.let { BestTime(p, it) }
    }

    /**
     * Restart (S-2Δ, REQ-025): an In-progress puzzle becomes New with an empty board, time 0 and its best
     * time kept (REQ-030 rule 2), every piece in the tray at its resting turn, unmirrored. It folds first (S-2Δ3),
     * and a running drag ends as [goHome].
     *
     * @return false, and nothing changes, when the puzzle is not In progress (Restart is shown only then).
     */
    fun restart(): Boolean {
        if (state != PuzzleState.IN_PROGRESS) return false
        fold()
        goHome()
        startOver()
        return true
    }

    /**
     * Retry (S-2Δ, REQ-026): a Solved puzzle becomes New with an empty board, time 0 and its best time kept
     * (REQ-030 rule 2), every piece in the tray at its resting turn, unmirrored; the celebration is cleared (S-3Δ).
     * It folds first (S-2Δ3).
     *
     * @return false, and nothing changes, when the puzzle is not Solved (Retry is shown only then).
     */
    fun retry(): Boolean {
        if (state != PuzzleState.SOLVED) return false
        fold()
        startOver()
        return true
    }

    // ── Time (S-2Δ3, S-20Δ) ──────────────────────────────────────────────────────────────────────

    /**
     * One tick of `:app`'s ticker (S-2Δ3, S-15Δ3): the fold, then S-20Δ's cadence — once [SAVE_EVERY_PLAY_SECONDS]
     * play seconds have been counted since the last such write, the play times are written through [persistence],
     * then the open puzzle's S-20 save (decisions.md 2026-10-20 "When the times are written, and what a process death
     * may lose."). Nothing happens for a controller that counts no time.
     */
    fun countTime() {
        fold()
        if (unsavedPlaySeconds >= SAVE_EVERY_PLAY_SECONDS) writeTimes()
    }

    /**
     * The game became visible on screen ([shown] true) or left it (false) (S-2Δ3; TYPE-005 "visible on screen"): the
     * fold, then the kernel is told. Leaving the screen writes the times at once: the play times, then the open
     * puzzle's S-20 save (S-20Δ), so a death after leaving the screen loses none of them (decisions.md 2026-10-20 "When
     * the times are written, and what a process death may lose.").
     */
    fun setVisible(shown: Boolean) {
        fold()
        time?.visible(shown)
        if (!shown) writeTimes()
    }

    /**
     * Runs [drive] with the best time withheld (S-2Δ3; REQ-030 rule 4, REQ-046 rule 4; decisions.md 2026-10-20 "An aid
     * solve keeps the best time through one generic controller member." as amended): a solve inside it is the ordinary
     * solve — Solved, the celebration, the save — but the best time stays as it was (none stays none, a saved one is
     * kept). The flag is restored when [drive] returns or throws; its exception passes on.
     */
    internal fun solveWithoutBest(drive: () -> Unit) {
        val before = withheld
        withheld = true
        try {
            drive()
        } finally {
            withheld = before
        }
    }

    /**
     * The fold (S-2Δ3, S-2Δ4): the whole active seconds the kernel counted since the last count are added to
     * [timeSeconds] when the open puzzle is In progress, the grid overview is closed and the settings screen is closed
     * (REQ-030 statement and rule 1; REQ-032 rule 2; TYPE-006 rule 4; decisions.md 2026-10-20 "What counts on each
     * screen."), and [playTimes] is read again. No write: the writes are [countTime]'s cadence, [setVisible]'s and each
     * action's own S-20 save.
     */
    private fun fold() {
        val t = time ?: return
        val n = t.count()
        if (state == PuzzleState.IN_PROGRESS && !overviewOpen && !settingsOpen) timeSeconds += n
        playTimes = t.playTimes()
        unsavedPlaySeconds += n
    }

    /**
     * S-20Δ's write: the play times through [persistence] (when this controller counts time; one without a clock has
     * none of its own to write), then the open puzzle's S-20 save.
     */
    private fun writeTimes() {
        if (time != null) persistence?.savePlayTimes(playTimes)
        unsavedPlaySeconds = 0
        keep()
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
     * one. The time rides every save as folded so far, and the best time as kept (S-20Δ; O-04).
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
     * mirror, or the board with its placement's turn and mirror. A start from the tray folds first (S-2Δ3), then
     * moves a New puzzle to In progress (TYPE-006), so the seconds spent while New are not the puzzle's (REQ-030.A3).
     * `PickedUp`.
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
            fold()
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
     * The solving drop folds first, while the puzzle is still In progress, so the solve keeps its last seconds
     * (S-2Δ3); its time is then the time of the solve, and the best time becomes the shorter of the two
     * (REQ-030 statement, A2) — unless [solveWithoutBest] withholds it (REQ-030 rule 4).
     *
     * @return the lock, appended to [placed] (`Locked`; then, when every piece is on the board, state
     *   Solved, the best time kept, a new [celebration] and `Solved`), or null when the piece went back to its
     *   tray cell (REQ-020; `WentHome`; a [pulse] in a mini puzzle, REQ-051). Null, and nothing happens, without a
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
        val board = placed + lock
        val solves = engine.isSolved(puzzle, board)
        if (solves) fold()
        placed = board
        onEvent(PlayEvent.Locked(d.piece))
        if (solves) {
            state = PuzzleState.SOLVED
            if (!withheld) bestSeconds = minOf(bestSeconds ?: timeSeconds, timeSeconds)
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
