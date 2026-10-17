// WO-003 TASK-015 · seam S-15 (tasks.md § WO-003 › Seams) · architecture.md G-12 / §3: `:app` is the one
// place that builds the lock engine, the puzzle library and the Play screen's state holder, and hands them
// on (WO-002 design review F4: the engine and the library exist once per process).
// WO-004 TASK-022 · seam S-15Δ (designs/WO-004-design.md §2): the bundle and the store load on IO and the
// controller is built on Main (the WO-003 F3 carry, decisions.md 2026-10-12 "Carried to WO-004 #Browsing";
// 2026-10-15 "Startup, and what survives"); `:app` builds the one store of the process (S-18, G-13).
package app.tangramplaytime.app

import android.content.Context
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.Persistence
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.store.ProgressStores
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The app's object graph, once per process (a Kotlin `object` is initialised once per class loader,
 * thread-safely), kept across activity recreations.
 *
 * [start] loads the bundled puzzles and the progress store on [Dispatchers.IO], then builds the one
 * [PlayController] on the main thread and publishes it in [controller]; until then [controller] holds null
 * and no puzzle is current (decisions.md 2026-10-15 "REQ-024.A2 and the launch window").
 *
 * The kits' one door into the app (S-15Δ; designs/WO-004-design.md §3): `ProcessDeathKitTest` and
 * `AppWiringKitTest` read [controller].
 */
internal object AppGraph {
    /** The one lock rule (WO-001), stateless. */
    val engine: ILockEngine = LockEngine()

    /**
     * Where the start-up runs and where `Persistence`'s one writer runs (S-15Δ "`mainScope`"; S-19: the
     * store does its own IO). It lives as long as the process.
     */
    private val mainScope: CoroutineScope = MainScope()

    private val started = AtomicBoolean(false)

    private val built = MutableStateFlow<PlayController?>(null)

    /**
     * The Play screen's state holder, or null until [start]'s load has finished. Its events go nowhere in
     * this WO: sounds and the haptic are WO-007's (O-07), which passes a listener here.
     */
    val controller: StateFlow<PlayController?> = built.asStateFlow()

    /**
     * Starts the load once per process; a later call does nothing (S-15Δ: idempotent). The library throws
     * on a build defect (G-15); that exception is not caught here, so it ends the process as WO-003's
     * synchronous load did. The store never throws for unreadable state (`IProgressStore` KDoc).
     */
    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val app = context.applicationContext
        mainScope.launch {
            val (library, persistence) = withContext(Dispatchers.IO) {
                PuzzleLibraries.bundled(engine) to Persistence.load(ProgressStores.onDevice(app), mainScope)
            }
            built.value = PlayController(library, engine, persistence)
        }
    }
}
