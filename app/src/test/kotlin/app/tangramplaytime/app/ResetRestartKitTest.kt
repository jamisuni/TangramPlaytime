// Acceptance kit (WO-007, TASK-052; kit stage 1) — the restart leg of Reset all progress, on the JVM: the real
// `FileProgressStore` (`:store`, ADR-003) on a temporary file, the real `Persistence` (`:play`, S-19Δ2), the real
// `ActiveTime` (`:kernel`, S-45Δ) over a kit clock, and the controller (S-1Δ4, S-2Δ4); then a fresh store, `Persistence.load`
// and a controller over the same file — the read that shows a stale write landing after the reset.
//
// - Why it exists: designs/WO-007-design.md §3 "Highest-risk part" (`resetAll`'s order, silent until a restart) and its
//   oracles; §2 "Kit doors (Q11)", JVM: "The restart leg (F4) runs in `app/src/test`, over the real `FileProgressStore` on a
//   temp file, `Persistence`, `ActiveTime` and the controller. Trap state: a Solved puzzle with a best; the open puzzle In
//   progress with pieces and time; play time > 0; timer on. Steps: `resetAll`, `flush`, then a new store, `Persistence.load`
//   and a controller over the same file. Assertion: every puzzle FRESH, `PlayTimes.NONE` (plus any second counted since the
//   reset), and the settings and last-opened kept"; decisions.md 2026-10-21 "Adds the oracles to … Reset all progress …"
//   (E6a: "This catches any stale write that lands after the reset, whatever its order"); reviews/WO-007-design-review.md
//   F4 and Re-derived rows 7 and 12.
// - No token (design §2 "Tokens": REQ-034's lines are `ResetKitTest`'s, on the device). Every assertion cites its source;
//   prose names a line without the dot (traceability-rules.md §2). Sources: REQ-034 statement ("set every puzzle to New and
//   erase all puzzle, best and play times") and rule 2 ("Settings (timer, sound) are kept"); the IProgressStore `resetAll`
//   KDoc ("every puzzle reads FRESH … the play times read NONE. Settings are kept … and the last opened puzzle id is
//   unchanged"); S-19Δ2 ("the settings and last-opened are kept"); E6 ("Afterwards: the player stays on the open puzzle").
// - The trap is E6a's, made so each kept or erased item can be told apart: S Solved with a best (seeded through the store,
//   as an earlier run left it); A, the last-opened puzzle, made In progress by two drops in this run, with time; play time
//   above 0; the timer on, and the sound off too (both fields of REQ-034 rule 2 away from their defaults). The settings
//   screen is open when the reset is made, as on the device (E6), with seconds still waiting in the kernel.
// - The positive control is in the same test: the same trap on another file, restarted without a reset, reads back every
//   item the reset must erase or keep.
// - Boots follow `AppGraph.start`'s order (designs/WO-008-design.md §2 S-15Δ3: `Persistence.load`, then
//   `ActiveTime(clock, persistence.playTimes)` and the controller with it). The kit reports touches to the kernel itself and
//   ticks with `countTime()` (the WO-008 JVM pattern, play/src/test/.../PlayTimeKitTest.kt, read, not called: another module).
// - Time (ADR-006; G-14; task-decomposition.md "Time is injected, never slept"): the kit owns its clock and moves it; nothing
//   sleeps. Every action, touch and clock move runs on the test's one thread (S-44 "One caller thread").
// - `app/src/test` is another module than `:play`, `:store` and `:kernel`: public members only (the brief). The engine is
//   the geometry oracle (G-04): every drop is a lock the engine finds at a solution pose, shown before it is used. The
//   bundle is read through `:content` (`PuzzleLibraries.bundled`); the test reads no repository file itself, and writes only
//   in its temporary folder. Helper names carry a `kit` / `Kit` / `KIT_` prefix and are members of this class.
package app.tangramplaytime.app

import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.Clock
import app.tangramplaytime.contracts.IActiveTime
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.IProgressStore
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceType
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
import app.tangramplaytime.kernel.ActiveTime
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.Persistence
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.store.FileProgressStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.util.Collections
import kotlin.math.abs
import kotlin.math.min

class ResetRestartKitTest {

    @get:Rule
    val kitTmp = TemporaryFolder()

    private val kitScopes = Collections.synchronizedList(ArrayList<CoroutineScope>())
    private val kitFailures = Collections.synchronizedList(ArrayList<Throwable>())

    @After
    fun kitClose() {
        kitScopes.forEach { it.cancel() }
        kitAssertNoFailures("the end of the test")
    }

    private companion object {
        /** How long a load or flush may take before the test fails instead of hanging (nothing here sleeps). */
        const val KIT_IO_TIMEOUT_MS = 20_000L

        /** Where the kit's clock starts: far from zero (the Clock KDoc: "the zero point is unspecified"). */
        const val KIT_CLOCK_START_MS = 50_000_000L

        /** The kit clock's local date (any fixed date). */
        val KIT_DAY: LocalDate = LocalDate.of(2026, 10, 21)

        /**
         * The board scale the kit's controller uses, dp per puzzle unit: lock distance R = max(0.65, 30 / 60) = 0.65 units
         * (TYPE-004; the ILockEngine KDoc "Lock distance"), as the `:play` JVM kit uses it (KitPlay.kt `KIT_DP_PER_UNIT`).
         */
        const val KIT_DP_PER_UNIT = 60.0

        /** Where a tray drag starts, in puzzle units: below every shipped board (KitPlay.kt `KIT_TRAY_POINT`; S-2 `startDrag`). */
        val KIT_TRAY_POINT: Point = Point(2.0, 12.0)

        /** The real lock engine (WO-001), one for the library and the controller, as `:app` wires it (S-15). */
        val kitEngine: ILockEngine by lazy { LockEngine() }

        /** The shipped puzzles (WO-002), built once ("the library is fixed for the process's lifetime", IPuzzleLibrary KDoc). */
        val kitLibrary: IPuzzleLibrary by lazy { PuzzleLibraries.bundled(kitEngine) }
    }

    /** The kit's [Clock] (ADR-006; G-14 "Tests set the clock"): it moves only when the test moves it, on the test's thread. */
    private class KitClock(var millis: Long = KIT_CLOCK_START_MS, var date: LocalDate = KIT_DAY) : Clock {
        override fun nowMillis(): Long = millis

        override fun today(): LocalDate = date

        fun forward(seconds: Int) {
            millis += seconds * 1000L
        }
    }

    /** One boot of the game's state: the store, the scope Persistence writes on, Persistence, the kernel's time, the controller. */
    private class KitBoot(
        val store: IProgressStore,
        val scope: CoroutineScope,
        val persistence: Persistence,
        val time: IActiveTime,
        val clock: KitClock,
        val c: PlayController,
    )

    /** One drop the engine locks: released at [dropCentre], the piece locks at [target] (a solution pose, as the engine places it). */
    private data class KitLock(val target: Placement, val dropCentre: Point)

    /** The trap of E6a on one file: the boot it was built in, the open puzzle [a] with its two [locks], and the Solved [s]. */
    private class KitTrap(val file: File, val boot: KitBoot, val a: PuzzleDefinition, val s: PuzzleDefinition, val locks: List<KitLock>)

    // ── Boots ────────────────────────────────────────────────────────────────────────────────────────

    /** A scope like the one `:app` gives Persistence: a supervisor, a background dispatcher, failures recorded. */
    private fun kitScope(): CoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> kitFailures += e })
        kitScopes += scope
        return scope
    }

    private fun kitAssertNoFailures(where: String) {
        val seen = synchronized(kitFailures) { kitFailures.toList() }
        if (seen.isNotEmpty()) throw AssertionError("Persistence's scope failed before $where: ${seen.first()}", seen.first())
    }

    /** `AppGraph.start`'s order (S-15Δ3): `Persistence.load(store, scope)`, then `ActiveTime(clock, persistence.playTimes)`, then the controller. */
    private fun kitBoot(store: IProgressStore, clock: KitClock): KitBoot {
        val scope = kitScope()
        val persistence = runBlocking { withTimeout(KIT_IO_TIMEOUT_MS) { Persistence.load(store, scope) } }
        val time: IActiveTime = ActiveTime(clock, persistence.playTimes)
        val c = PlayController(kitLibrary, kitEngine, persistence, time)
        c.boardDpPerUnit = KIT_DP_PER_UNIT
        kitAssertNoFailures("the boot")
        return KitBoot(store, scope, persistence, time, clock, c)
    }

    /** S-2Δ `flush()` ("delegates to Persistence"), awaited: every queued write has landed in the file. */
    private fun kitFlush(boot: KitBoot) {
        runBlocking { withTimeout(KIT_IO_TIMEOUT_MS) { boot.c.flush() } }
        kitAssertNoFailures("the flush")
    }

    /** The restart: flush, end the old writer's scope, then a new `FileProgressStore` over the same [file] and a new boot over it. */
    private fun kitRestart(boot: KitBoot, file: File): KitBoot {
        kitFlush(boot)
        boot.scope.cancel()
        return kitBoot(FileProgressStore(file), boot.clock)
    }

    // ── Play ─────────────────────────────────────────────────────────────────────────────────────────

    /** A touch on the game, reported to the kernel (S-44 `touched()`; S-15Δ3 `AppGraph.touched()` forwards it there). */
    private fun kitTouch(b: KitBoot) = b.time.touched()

    /** [seconds] of active play: a touch, the clock moved by at most 30 s (inside TYPE-005's 60 s), then a tick (`countTime()`). */
    private fun kitPlay(b: KitBoot, seconds: Int) {
        var left = seconds
        while (left > 0) {
            val step = min(30, left)
            kitTouch(b)
            b.clock.forward(step)
            b.c.countTime()
            left -= step
        }
    }

    /** A touch, then [seconds] on the clock, with no tick: the seconds wait in the kernel for the next fold. */
    private fun kitTouchAndWait(b: KitBoot, seconds: Int) {
        kitTouch(b)
        b.clock.forward(seconds)
    }

    // ── Geometry: every drop is a lock the engine finds ──────────────────────────────────────────────

    private fun kitNear(a: Point, b: Point, tol: Double): Boolean = abs(a.x - b.x) <= tol && abs(a.y - b.y) <= tol

    private fun kitSamePointSet(a: List<Point>, b: List<Point>, tol: Double): Boolean =
        a.size == b.size && a.all { p -> b.any { kitNear(p, it, tol) } } && b.all { p -> a.any { kitNear(p, it, tol) } }

    private fun kitSamePose(a: Placement, b: Placement, tol: Double = 1e-6): Boolean =
        a.piece == b.piece && a.turn == b.turn && a.mirrored == b.mirrored && kitNear(a.centre, b.centre, tol)

    /**
     * The pose of one stored solution polygon: the centre is the polygon's average (the `Placement.centre` KDoc), and the
     * turn is the lowest 0..7, unmirrored (the kit uses no parallelogram), whose `engine.vertices` match the polygon as a
     * point set within 1e-6 (KitPlay.kt `kitPoseOf`, the same rule). Null when none matches.
     */
    private fun kitPoseOrNull(sp: SolvedPiece): Placement? {
        val centre = Point(sp.polygon.sumOf { it.x } / sp.polygon.size, sp.polygon.sumOf { it.y } / sp.polygon.size)
        for (step in 0..7) {
            if (kitSamePointSet(sp.polygon, kitEngine.vertices(sp.piece, Turn(step), false, centre), 1e-6)) {
                return Placement(sp.piece, Turn(step), false, centre)
            }
        }
        return null
    }

    /**
     * Two drops on [p] that the engine locks, one after the other, each exactly at a solution pose (ILockEngine `findLock`
     * with the pieces locked before it on the board); the parallelogram is left out (no badge in the fixture). Empty when
     * no two such drops are found.
     */
    private fun kitTwoLocks(p: PuzzleDefinition): List<KitLock> {
        val poses = p.solution.filter { it.piece.type != PieceType.PG }.mapNotNull { kitPoseOrNull(it) }.sortedBy { it.piece }
        val locks = ArrayList<KitLock>()
        while (locks.size < 2) {
            val next = poses.filter { pose -> locks.none { it.target.piece == pose.piece } }.firstNotNullOfOrNull { pose ->
                val r = kitEngine.findLock(p, locks.map { it.target }, pose.piece, pose.turn, false, pose.centre, KIT_DP_PER_UNIT)
                if (r is LockResult.Locked && kitSamePose(r.placement, pose)) KitLock(r.placement, pose.centre) else null
            } ?: return emptyList()
            locks += next
        }
        return locks
    }

    /** The first listed warm-up or full puzzle that takes two such drops, and the drops; none is a fixture defect. */
    private fun kitOpenPuzzle(): Pair<PuzzleDefinition, List<KitLock>> {
        for (p in kitLibrary.puzzles.filter { it.kind != PuzzleKind.MINI }) {
            val locks = kitTwoLocks(p)
            if (locks.size == 2) return p to locks
        }
        throw AssertionError("fixture defect: no listed puzzle takes two drops that lock at solution poses")
    }

    /**
     * Plays [lock] with the player's actions only — taps in the tray to its turn, `startDrag`, `moveDrag` over the board,
     * `updatePreview`, `drop` — and asserts the drop locks at the planned target (S-2 `drop`: "The lock is the
     * updatePreview() result for the current centre").
     */
    private fun kitDrop(c: PlayController, lock: KitLock) {
        val piece = lock.target.piece
        repeat(8) { if (c.trayTurn(piece) != lock.target.turn) c.tapTray(piece) }
        assertEquals("fixture: $piece's tray turn", lock.target.turn, c.trayTurn(piece))
        assertTrue("fixture: startDrag($piece) from the tray", c.startDrag(piece, KIT_TRAY_POINT))
        c.moveDrag(lock.dropCentre, true)
        c.updatePreview()
        val got = c.drop() ?: throw AssertionError("fixture: the planned drop of $piece went home; the engine locks it at ${lock.target}")
        assertTrue("fixture: the planned drop of $piece locks at ${lock.target}, not $got", kitSamePose(lock.target, got))
    }

    // ── Reads ────────────────────────────────────────────────────────────────────────────────────────

    /** S-1Δ4 `BestTime.puzzle` as a puzzle id: a listed puzzle's definition or its id; anything else fails loudly. */
    private fun kitIdOf(puzzle: Any?): String = when (puzzle) {
        is PuzzleDefinition -> puzzle.id
        is String -> puzzle
        else -> throw AssertionError("S-1Δ4: BestTime.puzzle names a listed puzzle; it was $puzzle")
    }

    /** S-1Δ4 `BestTime.seconds` as whole seconds; anything but a number fails loudly. */
    private fun kitSecondsOf(seconds: Any?): Int =
        (seconds as? Number)?.toInt() ?: throw AssertionError("S-1Δ4: BestTime.seconds is a time in seconds; it was $seconds")

    /** S-1Δ4 `bestTimes` after `openSettings` ("Refreshed by `openSettings`"), as (puzzle id, seconds) rows. */
    private fun kitBestRows(c: PlayController): List<Pair<String, Int>> {
        c.openSettings()
        return c.bestTimes.map { kitIdOf(it.puzzle) to kitSecondsOf(it.seconds) }
    }

    /** What the restarted store holds, read through IProgressStore only. */
    private fun kitStored(store: IProgressStore, id: String): PuzzleProgress = runBlocking { store.progress(id) }

    // ── The trap ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * E6a's trap on a new file in the temporary folder:
     * - as an earlier run left it, through the store: S Solved (50 s, best 42 s); A last opened, New;
     * - this run, booted over the same store: the game shown; the timer on and the sound off, through the switches; A's
     *   first drop (In progress); 25 s of play; A's second drop; 15 s waiting; the settings opened (the opening folds the
     *   15 s into A first: S-2Δ4); 3 s waiting with the settings open (play time only: REQ-032 rule 2).
     * Fixture checks: A open, In progress with the two locks and 40 s; the play time 40 s, with 3 s waiting.
     */
    private fun kitTrap(name: String): KitTrap {
        val (a, locks) = kitOpenPuzzle()
        val s = kitLibrary.puzzles.firstOrNull { it.id != a.id }
            ?: throw AssertionError("fixture defect: the library lists one puzzle")
        val file = File(kitTmp.newFolder(name), "progress.json")
        val store = FileProgressStore(file)
        runBlocking {
            store.saveProgress(s.id, PuzzleProgress(PuzzleState.SOLVED, emptyList(), 50, 42))
            store.saveLastOpenedPuzzleId(a.id)
        }
        val boot = kitBoot(store, KitClock())
        val c = boot.c
        assertEquals("fixture: A, the last opened, is open", a.id, c.puzzle.id)
        assertEquals("fixture: A New", PuzzleState.NEW, c.state)
        c.setVisible(true)
        c.setTimerShown(true)
        c.setSoundOn(false)
        kitDrop(c, locks[0])
        assertEquals("fixture: A In progress after its first drop (TYPE-006)", PuzzleState.IN_PROGRESS, c.state)
        kitPlay(boot, 25)
        kitDrop(c, locks[1])
        kitTouchAndWait(boot, 15)
        c.openSettings()
        kitTouchAndWait(boot, 3)
        assertTrue("fixture: the settings are open", c.settingsOpen)
        assertEquals("fixture: A's two locks on the board", locks.map { it.target.piece }.toSet(), c.placed.map { it.piece }.toSet())
        assertEquals("fixture: A's time, 25 s and the 15 s folded when the settings opened", 40, c.timeSeconds)
        assertEquals("fixture: the play time so far, 3 s still waiting", 40, c.playTimes.allTimeSeconds)
        return KitTrap(file, boot, a, s, locks)
    }

    // ── The restart leg ──────────────────────────────────────────────────────────────────────────────

    // Pin (no token): REQ-034 statement and rule 2; the IProgressStore `resetAll` KDoc; S-19Δ2; S-2Δ4; E6 and E6a; design
    // §3 "Reset order"; S-44Δ ("NONE until a second counts"). Two files, the same trap on each:
    // - Positive control (no reset): the game leaves the screen (S-20Δ: the times are written at `setVisible(false)`), the
    //   writes are flushed, and a fresh store, Persistence and controller over the file read back every trap item: S Solved
    //   with its best; A In progress with its two locks and 40 s; the play time 43 s (the 3 s folded at the leaving); the
    //   timer on and the sound off; A last opened; S's best time listed.
    // - The reset: Reset all progress with the settings open and 3 s waiting; then 5 s of play (counted after the reset);
    //   the game leaves the screen; flush; a fresh store, Persistence and controller over the file. Every listed puzzle
    //   reads FRESH in the store and New in the controller; A is open, New, nothing placed, no time, no best; the play times
    //   are NONE plus the 5 s counted after the reset (today and all-time 5 s on the kit's day); the timer is on and the sound
    //   off; A is still the last opened; no best time is listed.
    @Test
    fun afterResetAllAFreshStoreReadsEveryPuzzleFreshThePlayTimesErasedAndTheSettingsKept() {
        // Positive control: the trap survives a restart, item by item.
        val control = kitTrap("control")
        control.boot.c.setVisible(false)
        val seen = kitRestart(control.boot, control.file)
        assertEquals("control: S Solved with its best", PuzzleProgress(PuzzleState.SOLVED, emptyList(), 50, 42), kitStored(seen.store, control.s.id))
        val aSaved = kitStored(seen.store, control.a.id)
        assertEquals("control: A In progress", PuzzleState.IN_PROGRESS, aSaved.state)
        assertEquals(
            "control: A's two locks",
            control.locks.associate { it.target.piece to it.target },
            aSaved.placed.associateBy { it.piece },
        )
        assertEquals("control: A's time", 40, aSaved.timeSeconds)
        assertEquals("control: the play times", PlayTimes(43, KIT_DAY, 43), runBlocking { seen.store.playTimes() })
        assertEquals("control: the switches", Settings(timerShown = true, soundOn = false), runBlocking { seen.store.settings() })
        assertEquals("control: A last opened", control.a.id, runBlocking { seen.store.lastOpenedPuzzleId() })
        assertEquals("control: the restarted controller opens A, In progress", PuzzleState.IN_PROGRESS, seen.c.state)
        assertEquals("control: …with the timer on", true, seen.c.timerShown)
        assertEquals("control: S's best time listed", listOf(control.s.id to 42), kitBestRows(seen.c))

        // The reset, then the restart.
        val trap = kitTrap("reset")
        val c = trap.boot.c
        c.resetAll()
        assertEquals("fixture: A New at once (S-2Δ4 step 4)", PuzzleState.NEW, c.state)
        assertEquals("fixture: the play times NONE at once (S-2Δ4 step 5)", PlayTimes.NONE, c.playTimes)
        kitPlay(trap.boot, 5)
        c.setVisible(false)
        val after = kitRestart(trap.boot, trap.file)

        assertEquals(
            "a fresh store reads every listed puzzle FRESH (REQ-034 statement; IProgressStore resetAll)",
            emptyList<String>(),
            kitLibrary.puzzles.map { it.id }.filter { kitStored(after.store, it) != PuzzleProgress.FRESH },
        )
        assertEquals(
            "…and holds no entry other than FRESH (no stale write landed after the reset: E6a)",
            emptyMap<String, PuzzleProgress>(),
            runBlocking { after.store.allProgress() }.filterValues { it != PuzzleProgress.FRESH },
        )
        assertEquals(
            "the play times: NONE plus the 5 s counted after the reset (E6a; S-44Δ)",
            PlayTimes(5, KIT_DAY, 5),
            runBlocking { after.store.playTimes() },
        )
        assertEquals("the settings kept: timer on, sound off (REQ-034 rule 2)", Settings(timerShown = true, soundOn = false), runBlocking { after.store.settings() })
        assertEquals("the last-opened id kept (IProgressStore resetAll; S-19Δ2)", trap.a.id, runBlocking { after.store.lastOpenedPuzzleId() })

        val c2 = after.c
        assertEquals("the restarted controller opens A (E6: the player stays on the open puzzle)", trap.a.id, c2.puzzle.id)
        assertEquals("…New", PuzzleState.NEW, c2.state)
        assertEquals("…with nothing placed", emptyList<Placement>(), c2.placed)
        assertEquals("…no puzzle time", 0, c2.timeSeconds)
        assertNull("…no best time", c2.bestSeconds)
        assertEquals(
            "every listed puzzle New in the restarted controller (REQ-034 statement)",
            emptyList<String>(),
            kitLibrary.puzzles.map { it.id }.filter { c2.stateOf(it) != PuzzleState.NEW },
        )
        assertEquals("the restarted controller's settings kept (REQ-034 rule 2)", Settings(timerShown = true, soundOn = false), c2.settings)
        assertEquals("…the timer shown", true, c2.timerShown)
        assertEquals("the restarted play times: the 5 s after the reset only", PlayTimes(5, KIT_DAY, 5), c2.playTimes)
        assertEquals("no best time listed (REQ-034 statement: best times erased)", emptyList<Pair<String, Int>>(), kitBestRows(c2))
    }
}
