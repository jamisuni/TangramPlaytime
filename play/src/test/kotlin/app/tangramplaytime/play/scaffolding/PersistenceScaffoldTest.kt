// scaffolding — TASK-018's own checks of Persistence's stop path (S-19; G-15): once the writer's scope has
// ended, a save or a flush is refused with an exception instead of being lost silently or hanging. Not an
// acceptance test: it carries no token, and the kit (KitStore.kt, ResumeKitTest.kt) is the S-19 verdict.
package app.tangramplaytime.play.scaffolding

import app.tangramplaytime.contracts.IProgressStore
import app.tangramplaytime.contracts.PlayTimes
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Settings
import app.tangramplaytime.play.Persistence
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistenceScaffoldTest {

    /** A store whose progress writes wait for [gate]; everything else is a no-op. */
    private class GatedStore(private val gate: CompletableDeferred<Unit>) : IProgressStore {
        val written = ArrayList<String>()
        override suspend fun progress(puzzleId: String) = PuzzleProgress.FRESH
        override suspend fun saveProgress(puzzleId: String, progress: PuzzleProgress) {
            gate.await()
            synchronized(written) { written += puzzleId }
        }
        override suspend fun allProgress(): Map<String, PuzzleProgress> = emptyMap()
        override suspend fun playTimes() = PlayTimes.NONE
        override suspend fun savePlayTimes(times: PlayTimes) = Unit
        override suspend fun settings() = Settings()
        override suspend fun saveSettings(settings: Settings) = Unit
        override suspend fun lastOpenedPuzzleId(): String? = null
        override suspend fun saveLastOpenedPuzzleId(id: String) = Unit
        override suspend fun resetAll() = Unit
    }

    private val progress = PuzzleProgress(PuzzleState.IN_PROGRESS, emptyList(), 0, null)

    @Test
    fun aFlushReturnsOnceTheWritesBeforeItAreOnTheStore() {
        val gate = CompletableDeferred<Unit>()
        val store = GatedStore(gate)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val persistence = Persistence(store, scope, emptyMap(), null)
        persistence.save("a", progress)
        persistence.save("b", progress)
        assertEquals("the cache has the save at once, before the store", progress, persistence.progress("b"))
        assertTrue("nothing is on the store yet", synchronized(store.written) { store.written.isEmpty() })
        gate.complete(Unit)
        runBlocking { withTimeout(5_000) { persistence.flush() } }
        assertEquals(listOf("a", "b"), synchronized(store.written) { store.written.toList() })
        scope.cancel()
    }

    @Test
    fun aFlushWaitingWhenTheScopeEndsFailsInsteadOfHanging() {
        val store = GatedStore(CompletableDeferred())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val persistence = Persistence(store, scope, emptyMap(), null)
        persistence.save("a", progress)
        val failure = runBlocking {
            withTimeout(5_000) {
                val waiting = async(Dispatchers.Default) { runCatching { persistence.flush() } }
                scope.cancel()
                waiting.await().exceptionOrNull()
            }
        }
        assertTrue("the flush failed: $failure", failure is IllegalStateException)
    }

    @Test
    fun aSaveOrFlushAfterTheScopeEndedIsRefused() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val persistence = Persistence(GatedStore(CompletableDeferred(Unit)), scope, emptyMap(), null)
        scope.cancel()
        // The scope's job completes after its child, the writer, whose completion stops the queue.
        runBlocking { withTimeout(5_000) { scope.coroutineContext[Job]!!.join() } }
        assertThrows(IllegalStateException::class.java) { persistence.save("a", progress) }
        assertThrows(IllegalStateException::class.java) { persistence.saveLastOpened("a") }
        assertThrows(IllegalStateException::class.java) { runBlocking { withTimeout(5_000) { persistence.flush() } } }
    }
}
