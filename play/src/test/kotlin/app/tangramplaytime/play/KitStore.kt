// Acceptance kit (WO-004, TASK-016) — the JVM kit's persistence helpers: a kit-owned in-memory
// IProgressStore double, the drivers that boot PlayController over Persistence on it, the browsing doors,
// and the fixtures the WO-004 tests seed. Test-only; no product code.
//
// - Written against designs/WO-004-design.md §2 (S-19 `Persistence`, S-1Δ construction and reads, S-2Δ
//   actions, S-20, S-21, S-3Δ) and the frozen IProgressStore KDoc (contracts, baseline 2026-10-11). The
//   puzzles are the shipped ones through KitPlay.kt's `kitLibrary`; the double reads and writes no file.
// - The double implements every member as the IProgressStore KDoc says, so no product reading is
//   invented here. It fails loudly where a test expects something that is not there (`kitStored`), it
//   records every write it completes, and it counts writes in flight (S-19: "the one FIFO writer").
// - The scope Persistence writes on has an exception handler; a failure there fails the test at the next
//   flush or at the end (task-decomposition.md: "A test adapter fails with an error on a miss").
// - This file carries no acceptance token (tasks.md § WO-004 intro, "Lines covered twice").
// - Helper names carry a `kit` / `Kit` / `KIT_` prefix (KitPlay.kt). Declarations that name `Persistence`
//   are `internal`, so the kit compiles whether S-19 is public or internal.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.IProgressStore
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
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.contracts.Turn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.util.Collections
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** How long a kit load or flush may take before the test fails instead of hanging (nothing here sleeps). */
const val KIT_IO_TIMEOUT_MS: Long = 20_000L

// ── The store double ───────────────────────────────────────────────────────────────────────────

/** One write the double completed, in completion order: the member, the puzzle it was for, what it wrote. */
internal data class KitWrite(val member: String, val puzzleId: String?, val progress: PuzzleProgress?)

/**
 * The kit's in-memory [IProgressStore]. Every member is the KDoc's: a puzzle with no entry reads
 * [PuzzleProgress.FRESH]; `saveProgress` replaces that puzzle's entry "and nothing else"; `allProgress`
 * is "every saved puzzle's progress, keyed by puzzle id"; `resetAll` makes every puzzle FRESH and the
 * play times [PlayTimes.NONE], keeping the settings and the last opened id. Thread-safe: Persistence's
 * writer calls it from its own scope (S-19).
 *
 * [seed] and [lastOpened] are what an earlier run saved. [writeLatencyMs] holds the k-th write (0-based,
 * counted when the write starts) before it lands, so a test can make writes complete out of step
 * (designs/WO-004-design.md §3: "Never `launch` once per save: the launches can reorder, and a stale
 * board would win").
 */
internal class KitProgressStore(
    seed: Map<String, PuzzleProgress> = emptyMap(),
    lastOpened: String? = null,
    private val writeLatencyMs: (Int) -> Long = { 0L },
) : IProgressStore {
    private val lock = Any()
    private val saved = LinkedHashMap(seed)
    private var lastOpenedId: String? = lastOpened
    private var times: PlayTimes = PlayTimes.NONE
    private var prefs: Settings = Settings()
    private val log = ArrayList<KitWrite>()
    private var started = 0
    private var inFlight = 0
    private var maxInFlight = 0

    private suspend fun write(member: String, puzzleId: String?, progress: PuzzleProgress?, apply: () -> Unit) {
        val k = synchronized(lock) {
            inFlight++
            if (inFlight > maxInFlight) maxInFlight = inFlight
            started++
        }
        try {
            val wait = writeLatencyMs(k)
            if (wait > 0) delay(wait)
            synchronized(lock) {
                apply()
                log += KitWrite(member, puzzleId, progress)
            }
        } finally {
            synchronized(lock) { inFlight-- }
        }
    }

    override suspend fun progress(puzzleId: String): PuzzleProgress =
        synchronized(lock) { saved[puzzleId] ?: PuzzleProgress.FRESH }

    override suspend fun saveProgress(puzzleId: String, progress: PuzzleProgress) =
        write("saveProgress", puzzleId, progress) { saved[puzzleId] = progress }

    override suspend fun allProgress(): Map<String, PuzzleProgress> = synchronized(lock) { LinkedHashMap(saved) }

    override suspend fun playTimes(): PlayTimes = synchronized(lock) { times }

    override suspend fun savePlayTimes(times: PlayTimes) = write("savePlayTimes", null, null) { this.times = times }

    override suspend fun settings(): Settings = synchronized(lock) { prefs }

    override suspend fun saveSettings(settings: Settings) = write("saveSettings", null, null) { prefs = settings }

    override suspend fun lastOpenedPuzzleId(): String? = synchronized(lock) { lastOpenedId }

    override suspend fun saveLastOpenedPuzzleId(id: String) =
        write("saveLastOpenedPuzzleId", id, null) { lastOpenedId = id }

    override suspend fun resetAll() = write("resetAll", null, null) {
        saved.clear()
        times = PlayTimes.NONE
    }

    // Kit-side reads, for assertions (no suspension; the writer may run concurrently).

    /** What the store holds for [puzzleId], read as the KDoc's `progress` reads it: FRESH when nothing is saved. */
    fun kitProgressOf(puzzleId: String): PuzzleProgress = synchronized(lock) { saved[puzzleId] ?: PuzzleProgress.FRESH }

    /** The saved entry for [puzzleId]; a test that calls this expects a save, so no entry fails loudly. */
    fun kitStored(puzzleId: String): PuzzleProgress = synchronized(lock) { saved[puzzleId] }
        ?: throw AssertionError("the store holds no progress for $puzzleId, where the test expects a save (S-20)")

    /** The saved last-opened id, or null. */
    fun kitLastOpened(): String? = synchronized(lock) { lastOpenedId }

    /** Every write completed so far, in completion order. */
    fun kitWrites(): List<KitWrite> = synchronized(lock) { log.toList() }

    /** The most writes that were ever in flight at once. */
    fun kitMaxInFlight(): Int = synchronized(lock) { maxInFlight }
}

// ── Booting the controller over Persistence ──────────────────────────────────────────────────────

/** One boot over [store]: the scope Persistence writes on, Persistence (S-19) and the controller over it (S-1Δ). */
internal class KitRun(
    val store: KitProgressStore,
    val scope: CoroutineScope,
    val persistence: Persistence,
    val c: PlayController,
)

/**
 * The boots of one test. [start] is what `:app` does (S-15Δ): `Persistence.load(store, scope)`, then
 * `PlayController(library, engine, persistence)` (S-19, S-1Δ). [restart] is the seam-level restart of
 * designs/WO-004-design.md §3: flush, end the old writer's scope, then a new `Persistence.load` and a new
 * controller over the same store instance. [close] ends every scope (call it from `@After`).
 */
internal class KitSession {
    private val scopes = Collections.synchronizedList(ArrayList<CoroutineScope>())
    private val failures = Collections.synchronizedList(ArrayList<Throwable>())

    /** A scope like the one `:app` gives Persistence: a supervisor, a background dispatcher, failures recorded. */
    fun scope(): CoroutineScope {
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, t -> failures += t },
        )
        scopes += scope
        return scope
    }

    fun start(store: KitProgressStore, events: MutableList<PlayEvent>? = null): KitRun {
        val scope = scope()
        lateinit var persistence: Persistence
        runBlocking { withTimeout(KIT_IO_TIMEOUT_MS) { persistence = Persistence.load(store, scope) } }
        val c = if (events == null) {
            PlayController(kitLibrary, kitEngine, persistence)
        } else {
            PlayController(kitLibrary, kitEngine, persistence) { event: PlayEvent -> events += event }
        }
        c.boardDpPerUnit = KIT_DP_PER_UNIT
        assertNoFailures("the start")
        return KitRun(store, scope, persistence, c)
    }

    /** S-2Δ `flush()` ("delegates to Persistence"), awaited; a Job it might return is joined too. */
    fun flush(run: KitRun) {
        runBlocking {
            withTimeout(KIT_IO_TIMEOUT_MS) {
                val result: Any? = run.c.flush()
                if (result is Job) result.join()
            }
        }
        assertNoFailures("the flush")
    }

    fun restart(run: KitRun, events: MutableList<PlayEvent>? = null): KitRun {
        flush(run)
        run.scope.cancel()
        return start(run.store, events)
    }

    fun close() {
        scopes.forEach { it.cancel() }
        assertNoFailures("the end of the test")
    }

    private fun assertNoFailures(where: String) {
        val seen = synchronized(failures) { failures.toList() }
        if (seen.isNotEmpty()) throw AssertionError("Persistence's scope failed before $where: ${seen.first()}", seen.first())
    }
}

// ── The browsing doors (S-2Δ), each followed by the board scale (KitPlay.kt `kitOpen`) ──────────

/** › (S-2Δ `next()`). */
internal fun kitNext(c: PlayController) {
    c.next()
    c.boardDpPerUnit = KIT_DP_PER_UNIT
}

/** ‹ (S-2Δ `previous()`). */
internal fun kitPrevious(c: PlayController) {
    c.previous()
    c.boardDpPerUnit = KIT_DP_PER_UNIT
}

/** The long press on › (S-2Δ `nextUnsolved()`). */
internal fun kitNextUnsolved(c: PlayController) {
    c.nextUnsolved()
    c.boardDpPerUnit = KIT_DP_PER_UNIT
}

/** The grid: open the overview, then one press on [id]'s cell (S-2Δ `openOverview()`, `choose(id)`). */
internal fun kitChoose(c: PlayController, id: String) {
    c.openOverview()
    c.choose(id)
    c.boardDpPerUnit = KIT_DP_PER_UNIT
}

// ── Observations ─────────────────────────────────────────────────────────────────────────────────

/** The board as a map from piece to its placement (piece, turn, mirror, centre) — order-free. */
internal fun kitBoardOf(c: PlayController): Map<PieceId, Placement> = c.placed.associateBy { it.piece }

/** [placements] as a map from piece to placement; a piece twice is a fixture defect. */
internal fun kitByPiece(placements: List<Placement>): Map<PieceId, Placement> {
    val map = placements.associateBy { it.piece }
    if (map.size != placements.size) throw AssertionError("fixture defect: a piece appears twice in $placements")
    return map
}

/** `stateOf(id)` (S-1Δ) for every listed puzzle. */
internal fun kitStates(c: PlayController): Map<String, PuzzleState> = kitLibrary.puzzles.associate { it.id to c.stateOf(it.id) }

/**
 * Asserts [c] shows [p] with nothing placed or dragged and every piece in the tray, in tray order, at its
 * S-9 resting turn and unmirrored (S-1 reads; S-2Δ `restart()` / `retry()`: "the tray at rest";
 * decisions.md 2026-10-15 "Browsing readings no REQ fixes": "Restart and Retry put every piece in the tray
 * at its resting turn, unmirrored").
 */
internal fun kitAssertAllInTrayAtRest(c: PlayController, p: PuzzleDefinition, what: String) {
    assertEquals("$what: the open puzzle", p.id, c.puzzle.id)
    assertEquals("$what: nothing on the board", emptyList<Placement>(), c.placed)
    assertNull("$what: nothing dragged", c.dragged)
    assertEquals("$what: every piece in the tray, in tray order", p.pieces.toList(), c.trayPieces.toList())
    for (id in p.pieces) {
        assertEquals("$what: $id at its resting turn", TrayRest.turn(id), c.trayTurn(id))
        assertFalse("$what: $id not mirrored", c.trayMirrored(id))
    }
}

// ── Fixtures ─────────────────────────────────────────────────────────────────────────────────────

/** The puzzle at 1-based place [n] of the ‹ › order (IPuzzleLibrary `puzzles`). */
internal fun kitNth(n: Int): PuzzleDefinition = kitLibrary.puzzles.getOrNull(n - 1)
    ?: throw AssertionError("fixture defect: the library lists ${kitLibrary.puzzles.size} puzzles, not $n")

private val kitPlanCache = HashMap<String, List<KitStep>>()

/** [kitPlan] of [p], computed once per puzzle. */
internal fun kitPlanOf(p: PuzzleDefinition): List<KitStep> = synchronized(kitPlanCache) { kitPlanCache.getOrPut(p.id) { kitPlan(p) } }

/**
 * The first [n] locks of [p]'s plan, as a saved board: each a lock the engine found (KitPlay.kt
 * `kitPlan`), and shown here to be a board `stillValid` keeps whole (ILockEngine `stillValid` KDoc:
 * "Equal to saved when nothing changed").
 */
internal fun kitBoard(p: PuzzleDefinition, n: Int): List<Placement> {
    val plan = kitPlanOf(p)
    if (n >= plan.size) throw AssertionError("fixture defect: ${p.id} has ${plan.size} pieces; a board of $n would solve it")
    val board = plan.take(n).map { it.target }
    assertEquals("fixture: stillValid keeps ${p.id}'s first $n planned locks", board, kitEngine.stillValid(p, board))
    return board
}

/** An In-progress save of [p] with its first [n] planned locks on the board. */
internal fun kitInProgress(p: PuzzleDefinition, n: Int, time: Int = 0, best: Int? = null): PuzzleProgress =
    PuzzleProgress(PuzzleState.IN_PROGRESS, kitBoard(p, n), time, best)

/** A Solved save: nothing placed (the PuzzleProgress KDoc: `placed` is "Empty … when it is Solved"). */
internal fun kitSolved(time: Int, best: Int?): PuzzleProgress = PuzzleProgress(PuzzleState.SOLVED, emptyList(), time, best)

/**
 * A save for every listed puzzle in turn — Solved, In progress (one or two planned locks), New (no
 * entry) — with times and best times as WO-008 will write them (S-20: they pass through untouched).
 */
internal fun kitMixedSeed(): Map<String, PuzzleProgress> {
    val seed = LinkedHashMap<String, PuzzleProgress>()
    for ((i, p) in kitLibrary.puzzles.withIndex()) {
        when (i % 3) {
            0 -> seed[p.id] = kitSolved(time = 60 + i, best = 50 + i)
            1 -> seed[p.id] = kitInProgress(p, 1 + i % 2, time = 10 + i, best = if (i % 2 == 0) null else 40 + i)
            else -> Unit // New: never saved
        }
    }
    return seed
}

/** The state a seed gives every listed puzzle (no entry = New, the IProgressStore KDoc's FRESH). */
internal fun kitSeededStates(seed: Map<String, PuzzleProgress>): Map<String, PuzzleState> =
    kitLibrary.puzzles.associate { it.id to (seed[it.id]?.state ?: PuzzleState.NEW) }

/** True when [pt] lies strictly inside the convex [polygon] (every tangram piece is convex; TYPE-001). */
internal fun kitInsideConvex(pt: Point, polygon: List<Point>): Boolean {
    var sign = 0
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]
        val cross = (b.x - a.x) * (pt.y - a.y) - (b.y - a.y) * (pt.x - a.x)
        if (abs(cross) < 1e-9) return false
        val s = if (cross > 0) 1 else -1
        if (sign == 0) sign = s else if (s != sign) return false
    }
    return true
}

/**
 * REQ-025 A3's board for [p]: [kept] = its first three planned locks, and [crossing] = a further planned
 * piece moved off its lock so that it overlaps the silhouette's edge. [saved] puts the crossing piece
 * second, so pieces before and after it are both "the others".
 */
internal data class KitCrossing(val puzzle: PuzzleDefinition, val kept: List<Placement>, val crossing: Placement) {
    val saved: List<Placement> get() = listOf(kept[0], crossing) + kept.drop(1)
}

/**
 * Finds [KitCrossing] for [p] with the engine: the moved piece alone is not kept by `stillValid` (alone
 * it overlaps nothing, so part of it lies outside the silhouette), its centre still lies inside its own
 * planned lock (so part of it covers the silhouette), and `stillValid` of [KitCrossing.saved] is exactly
 * [KitCrossing.kept]. None is a fixture defect.
 */
internal fun kitCrossingFixture(p: PuzzleDefinition): KitCrossing {
    val kept = kitBoard(p, 3)
    for (step in kitPlanOf(p).drop(3)) {
        val pose = step.target
        val shape = kitEngine.vertices(pose.piece, pose.turn, pose.mirrored, pose.centre)
        for (d in listOf(0.25, 0.4)) {
            for (k in 0 until 8) {
                val a = k * PI / 4
                val moved = pose.copy(centre = Point(pose.centre.x + d * cos(a), pose.centre.y + d * sin(a)))
                if (kitEngine.stillValid(p, listOf(moved)).isNotEmpty()) continue
                if (!kitInsideConvex(moved.centre, shape)) continue
                val fixture = KitCrossing(p, kept, moved)
                if (kitEngine.stillValid(p, fixture.saved) != kept) continue
                return fixture
            }
        }
    }
    throw AssertionError("fixture defect: no planned piece of ${p.id} moved by 0.25 or 0.4 units overlaps the outline")
}

/**
 * A mini puzzle, a placement of a piece it does not have ([foreign]) that `stillValid` alone would keep
 * (inside the silhouette, a lock the engine found), and one of its own planned locks ([own]) that the
 * foreign piece overlaps — so only S-21's filter, applied before the check, keeps [own].
 */
internal data class KitForeign(val puzzle: PuzzleDefinition, val foreign: Placement, val own: Placement)

internal fun kitForeignFixture(): KitForeign {
    for (mini in kitLibrary.puzzles.filter { it.kind == PuzzleKind.MINI }) {
        val centres = mini.solution.map { kitAverage(it.polygon) } + kitEngine.outlineCorners(mini)
        for (f in PieceId.entries.filter { it !in mini.pieces }) {
            for (step in 0..7) {
                for (at in centres) {
                    val r = kitEngine.findLock(mini, emptyList(), f, Turn(step), false, at, KIT_WIDE_DP_PER_UNIT)
                    if (r !is LockResult.Locked) continue
                    val foreign = r.placement
                    if (kitEngine.stillValid(mini, listOf(foreign)) != listOf(foreign)) continue
                    val own = kitPlanOf(mini).map { it.target }
                        .firstOrNull { kitEngine.stillValid(mini, listOf(foreign, it)) == listOf(foreign) } ?: continue
                    return KitForeign(mini, foreign, own)
                }
            }
        }
    }
    throw AssertionError("fixture defect: no mini puzzle takes a foreign piece that overlaps one of its own locks")
}

/**
 * A puzzle with both large triangles, LT1 at its own solution place ([first]) and LT1 again at LT2's
 * solution place ([second]); `stillValid` keeps both (no overlap, both inside), so only S-21's
 * `distinctBy { it.piece }` removes [second].
 */
internal data class KitDuplicate(val puzzle: PuzzleDefinition, val first: Placement, val second: Placement)

internal fun kitDuplicateFixture(): KitDuplicate {
    for (p in kitLibrary.puzzles.filter { PieceId.LT1 in it.pieces && PieceId.LT2 in it.pieces }) {
        val first = kitPoseOf(p.solution.first { it.piece == PieceId.LT1 })
        val second = kitPoseOf(SolvedPiece(PieceId.LT1, p.solution.first { it.piece == PieceId.LT2 }.polygon))
        if (kitEngine.stillValid(p, listOf(first, second)) == listOf(first, second)) return KitDuplicate(p, first, second)
    }
    throw AssertionError("fixture defect: no puzzle keeps LT1 at both large-triangle places")
}

/**
 * Three planned drops of a non-mini puzzle whose first is the parallelogram mirrored (KitPlay.kt
 * `kitPlanOrNull` with `first = PG`), so a lost mirror shows; the three include a turn away from the
 * piece's resting turn, so a lost turn shows. None is a fixture defect.
 */
internal fun kitThreeWithMirroredPg(): Pair<PuzzleDefinition, List<KitStep>> {
    for (p in kitLibrary.puzzles.filter { it.kind != PuzzleKind.MINI && PieceId.PG in it.pieces }) {
        if (!kitPoseOf(p.solution.first { it.piece == PieceId.PG }).mirrored) continue
        val three = kitPlanOrNull(p, first = PieceId.PG)?.take(3) ?: continue
        if (three.none { it.target.turn != TrayRest.turn(it.piece) }) continue
        return p to three
    }
    throw AssertionError("fixture defect: no puzzle places a mirrored parallelogram first")
}
