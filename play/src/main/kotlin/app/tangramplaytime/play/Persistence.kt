// WO-004 TASK-018 · seam S-19 (designs/WO-004-design.md §2) · REQ-003, REQ-025 · architecture.md G-12
// (:contracts only), G-13 (the one road to the store), G-14 (no time), G-15 (results, not exceptions);
// decisions.md 2026-10-15 "Amends the 2026-10-15 ADR-003 row" (one instance, one writer).
// WO-008 TASK-046 · seam S-19Δ (designs/WO-008-design.md §2) · REQ-029, REQ-031 · G-13: the play times and the
// settings are read once at `load`, and `savePlayTimes` goes through the same FIFO writer as every other write.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.IProgressStore
import app.tangramplaytime.contracts.PlayTimes
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * The Play screen's road to [IProgressStore] (S-19): the cache of what is saved, and the one FIFO writer
 * that hands every write to the store in the order it was asked for. **Every store write goes through
 * it** (G-13; WO-003 F3); WO-007's reset and WO-008's times add their methods here.
 *
 * - **The cache** is what the controller reads. [save], [saveLastOpened] and [savePlayTimes] update it at
 *   once, on the caller's thread, and queue the write in the same step, so the cache and the queue have one
 *   order and nothing in the process waits for the store.
 * - **The one writer** is a single coroutine in the injected [scope] that takes the queue in order and
 *   awaits each store call before the next, so at most one write is in flight. One `launch` per save
 *   would let the writes reorder and a stale board win (design §3, "Never `launch` once per save";
 *   library fact 3: `Channel(UNLIMITED).trySend` never fails while open, and one consumer receives in
 *   order).
 * - A save is on the store once [flush] returns ("A save is complete when the call returns",
 *   IProgressStore KDoc).
 *
 * Reads no time (G-14) and names no dispatcher: the writer runs where [scope] runs it, and the store does
 * its own IO.
 *
 * @param store the one store of the process (S-18).
 * @param scope where the writer runs; it lives as long as this object is used.
 * @param saved every puzzle's saved progress, as `IProgressStore.allProgress` read it.
 * @param lastOpenedId the saved last-opened puzzle id, as `IProgressStore.lastOpenedPuzzleId` read it.
 * @param playTimes the saved play times, as `IProgressStore.playTimes` read them (S-19Δ; REQ-029).
 * @param settings the saved settings, as `IProgressStore.settings` read them (S-19Δ; REQ-031: the timer setting).
 */
class Persistence(
    private val store: IProgressStore,
    scope: CoroutineScope,
    saved: Map<String, PuzzleProgress>,
    lastOpenedId: String?,
    playTimes: PlayTimes = PlayTimes.NONE,
    settings: Settings = Settings(),
) {
    /** One queued store call, run by the writer in queue order. */
    private sealed interface Write {
        class Progress(val puzzleId: String, val progress: PuzzleProgress) : Write
        class LastOpened(val puzzleId: String) : Write
        class Times(val times: PlayTimes) : Write
        class Flush(val done: CompletableDeferred<Unit>) : Write
    }

    private val lock = Any()
    private val cache = HashMap(saved)
    private var lastOpened: String? = lastOpenedId
    private var times: PlayTimes = playTimes

    /**
     * The saved settings as [load] read them (S-19Δ): the timer setting the controller reads (REQ-031, S-1Δ3
     * `timerShown`). WO-008 writes no setting; the settings screen is WO-007's.
     */
    val settings: Settings = settings

    private val queue = Channel<Write>(Channel.UNLIMITED) { undelivered ->
        if (undelivered is Write.Flush) undelivered.done.completeExceptionally(stopped(null))
    }

    private val writer: Job = scope.launch {
        for (write in queue) {
            when (write) {
                is Write.Progress -> store.saveProgress(write.puzzleId, write.progress)
                is Write.LastOpened -> store.saveLastOpenedPuzzleId(write.puzzleId)
                is Write.Times -> store.savePlayTimes(write.times)
                is Write.Flush -> write.done.complete(Unit)
            }
        }
    }

    init {
        // The writer ends only when its scope ends or a store call throws (a programming error: the store
        // never throws, IProgressStore KDoc). The queue is then cancelled, so a waiting flush fails instead
        // of hanging, and a later save or flush is refused (G-15).
        writer.invokeOnCompletion { cause -> queue.cancel(CancellationException("Persistence's writer stopped", cause)) }
    }

    /** The saved last-opened puzzle id as of the latest [saveLastOpened], or null when none is saved. */
    internal val lastOpenedId: String?
        get() = synchronized(lock) { lastOpened }

    /**
     * The saved play times as of the latest [savePlayTimes], else as [load] read them (S-19Δ; REQ-029):
     * what `:app` builds the kernel's `ActiveTime` over (S-15Δ3).
     */
    val playTimes: PlayTimes
        get() = synchronized(lock) { times }

    /** The saved progress of [puzzleId] as of the latest [save], or [PuzzleProgress.FRESH] when none is saved. */
    internal fun progress(puzzleId: String): PuzzleProgress = synchronized(lock) { cache[puzzleId] } ?: PuzzleProgress.FRESH

    /**
     * Saves [progress] for [puzzleId]: the cache at once, the store in queue order (S-19, S-20).
     *
     * @throws IllegalStateException when the writer has stopped (its scope ended), a programming error (G-15).
     */
    fun save(puzzleId: String, progress: PuzzleProgress) {
        synchronized(lock) {
            enqueue(Write.Progress(puzzleId, progress))
            cache[puzzleId] = progress
        }
    }

    /**
     * Saves [puzzleId] as the last opened puzzle: the cache at once, the store in queue order (S-19, S-20).
     *
     * @throws IllegalStateException when the writer has stopped (its scope ended), a programming error (G-15).
     */
    fun saveLastOpened(puzzleId: String) {
        synchronized(lock) {
            enqueue(Write.LastOpened(puzzleId))
            lastOpened = puzzleId
        }
    }

    /**
     * Saves the play times (S-19Δ; REQ-029): the cache at once, the store in queue order, in the one FIFO
     * with every other write (G-13).
     *
     * @throws IllegalStateException when the writer has stopped (its scope ended), a programming error (G-15).
     */
    fun savePlayTimes(times: PlayTimes) {
        synchronized(lock) {
            enqueue(Write.Times(times))
            this.times = times
        }
    }

    /**
     * Returns once every write queued before it is on the store (S-19).
     *
     * @throws IllegalStateException when the writer stopped before reaching it, a programming error (G-15).
     */
    suspend fun flush() {
        val done = CompletableDeferred<Unit>()
        synchronized(lock) { enqueue(Write.Flush(done)) }
        done.await()
    }

    private fun enqueue(write: Write) {
        val result = queue.trySend(write)
        if (!result.isSuccess) throw stopped(result.exceptionOrNull())
    }

    private fun stopped(cause: Throwable?) =
        IllegalStateException("Persistence's writer has stopped (its scope ended); the write would be lost", cause)

    /** Persistence's door from a store (S-19). */
    companion object {
        /**
         * Reads [store] once — every puzzle's progress, the last-opened id, the play times and the settings
         * (S-19Δ) — and returns the [Persistence] over it, whose writer runs in [scope]. Called off the main
         * thread by `:app` (S-15Δ).
         */
        suspend fun load(store: IProgressStore, scope: CoroutineScope): Persistence =
            Persistence(store, scope, store.allProgress(), store.lastOpenedPuzzleId(), store.playTimes(), store.settings())
    }
}
