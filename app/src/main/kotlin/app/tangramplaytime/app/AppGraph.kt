// WO-003 TASK-015 · seam S-15 (tasks.md § WO-003 › Seams): `:app` is the one
// place that builds the lock engine, the puzzle library and the Play screen's state holder, and hands them
// on (WO-002 design review F4: the engine and the library exist once per process).
// WO-004 TASK-022 · seam S-15Δ (designs/WO-004-design.md §2): the bundle and the store load on IO and the
// controller is built on Main (the WO-003 F3 carry, decisions.md 2026-10-12 "Carried to WO-004 #Browsing";
// 2026-10-15 "Startup, and what survives"); `:app` builds the one store of the process (S-18, G-13).
// WO-008 TASK-049 · seam S-15Δ3 (designs/WO-008-design.md §2): the kernel's `ActiveTime` over `AppClock` (S-46, G-14) is
// built with the controller; the window's touches and its visibility are forwarded; a 250 ms ticker folds the counted
// seconds while the game is visible · TYPE-005; REQ-029, REQ-005, REQ-031 · decisions.md 2026-10-20 "TYPE-005 as code."
// and "When the times are written, and what a process death may lose."
// WO-007 TASK-061 · seam S-15Δ4, its feedback half (designs/WO-007-design.md §2): the one door (`Feedback`, S-50) over the
// device's playback (`DeviceFeedback`, S-51) is built synchronously, before the load, and is the controller's `onEvent`;
// `MainActivity` gives the playback its decor view · REQ-033; O-07 · decisions.md 2026-10-21 "REQ-033 A1's oracle is one
// door …" (E8) as amended (E8a).
package app.tangramplaytime.app

import android.content.Context
import android.view.View
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.IActiveTime
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.kernel.ActiveTime
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.Persistence
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.store.ProgressStores
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The app's object graph, once per process (a Kotlin `object` is initialised once per class loader,
 * thread-safely), kept across activity recreations.
 *
 * [start] loads the bundled puzzles and the progress store on [Dispatchers.IO], then builds the kernel's
 * TYPE-005 accounting over [AppClock] and the one [PlayController] with it on the main thread, and publishes
 * the controller in [controller]; until then [controller] holds null and no puzzle is current (decisions.md
 * 2026-10-15 "REQ-024.A2 and the launch window").
 *
 * Time (S-15Δ3): `MainActivity` reports every touch of its window ([touched]) and its start and stop
 * ([visible]). While the game is visible and the controller exists, a ticker on the main thread runs
 * `countTime()`, then waits [TICK_MS], over and over; it stops when the game leaves the screen. Every member
 * runs on the main thread, as the kernel requires (`IActiveTime`: one caller thread).
 *
 * Sounds, the tick and the click (S-15Δ4; O-07): [start] builds the one door, [feedback], before the load; the controller
 * emits its events into it, and `MainActivity` provides it as `LocalSoundEffect` and gives the device's playback its decor
 * view ([feedbackView], [feedbackViewGone]).
 *
 * The kits' doors into the app (S-15Δ; designs/WO-004-design.md §3; S-46; S-50): `ProcessDeathKitTest`,
 * `AppWiringKitTest` and `ActiveTimeAppKitTest` read [controller]; the `app/` time kits set
 * [AppClock.override]; the `app/` sound kit sets [feedback]'s `override`.
 */
internal object AppGraph {
    /** The one lock rule (WO-001), stateless. */
    val engine: ILockEngine = LockEngine()

    /**
     * Where the start-up runs, where `Persistence`'s one writer runs (S-15Δ "`mainScope`"; S-19: the
     * store does its own IO) and where the ticker runs (S-15Δ3). It lives as long as the process.
     */
    private val mainScope: CoroutineScope = MainScope()

    private val started = AtomicBoolean(false)

    private val built = MutableStateFlow<PlayController?>(null)

    /**
     * The Play screen's state holder, or null until [start]'s load has finished. Its events go to
     * [feedback] (S-15Δ4; O-07).
     */
    val controller: StateFlow<PlayController?> = built.asStateFlow()

    /**
     * The one door for every sound, tick and click (S-50; S-15Δ4: "`AppGraph.feedback` is internal"), built by the first
     * [start], synchronously, before the load. Sound is on exactly while the controller exists and its settings say so: every
     * use reads `controller.settings` (design §3 "N1"), and on the loading paper there is nothing to sound.
     */
    internal lateinit var feedback: Feedback
        private set

    /** The device's playback behind [feedback] (S-51), built with it. Main thread. */
    private lateinit var device: DeviceFeedback

    /**
     * The kernel's TYPE-005 accounting the controller was built with (S-15Δ3), or null until [start]'s
     * load has finished: a touch before that is not reported (design §2 "Per screen": play time does not
     * count on the loading paper). Main thread only.
     */
    private var time: IActiveTime? = null

    /**
     * How many `MainActivity` instances are between `onStart` and `onStop` (main thread only). The game is
     * visible on screen while at least one is (decisions.md 2026-10-20 "TYPE-005 as code.": "Visible on
     * screen" = `MainActivity` between `onStart` and `onStop`). A count, not the last value reported: a new
     * instance started over a running one (a launch with `FLAG_ACTIVITY_CLEAR_TASK`) starts before the old
     * one stops, and the game stays on screen across that overlap.
     */
    private var startedActivities = 0

    /** The game is visible on screen (TYPE-005): remembered before the controller exists, given to it when built. */
    private val visibleNow: Boolean
        get() = startedActivities > 0

    /** The running ticker, or null while the game is not visible or the controller does not exist yet. */
    private var ticker: Job? = null

    /**
     * Starts the load once per process; a later call does nothing (S-15Δ: idempotent). The library throws
     * on a build defect (G-15); that exception is not caught here, so it ends the process as WO-003's
     * synchronous load did. The store never throws for unreadable state (`IProgressStore` KDoc).
     *
     * S-15Δ3: the kernel's `ActiveTime` starts from the saved play times and reads [AppClock]; the
     * controller is told the visibility remembered so far before it is published, and the ticker starts if
     * the game is visible.
     *
     * S-15Δ4: [feedback] and its device playback are built here, on the caller's (main) thread, before the
     * load starts, so they exist before `MainActivity` composes or starts; the controller's `onEvent` is the
     * door's [Feedback.on].
     */
    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val app = context.applicationContext
        val playback = DeviceFeedback(app)
        val door = Feedback({ built.value?.settings?.soundOn == true }, playback)
        device = playback
        feedback = door
        mainScope.launch {
            val (library, persistence) = withContext(Dispatchers.IO) {
                PuzzleLibraries.bundled(engine) to Persistence.load(ProgressStores.onDevice(app), mainScope)
            }
            val activeTime = ActiveTime(AppClock, persistence.playTimes)
            val playController = PlayController(library, engine, persistence, activeTime, door::on)
            time = activeTime
            playController.setVisible(visibleNow)
            built.value = playController
            syncTicker()
        }
    }

    /**
     * The player touched the game's window (S-15Δ3; TYPE-005 "the player's last touch"): any `MotionEvent` the
     * window dispatches (decisions.md 2026-10-20 "TYPE-005 as code."), forwarded to the kernel. Main thread.
     */
    fun touched() {
        time?.touched()
    }

    /**
     * A `MainActivity` started ([shown] true) or stopped ([shown] false) (S-15Δ3; TYPE-005 "visible on screen").
     * When that changes whether the game is visible, the controller is told (`setVisible`: the fold, the kernel,
     * and on leaving the screen the times written at once, decisions.md 2026-10-20 "When the times are written …"),
     * and the ticker starts or stops. Main thread.
     */
    fun visible(shown: Boolean) {
        val was = visibleNow
        if (shown) startedActivities++ else startedActivities--
        if (visibleNow != was) built.value?.setVisible(visibleNow)
        syncTicker()
    }

    /**
     * A `MainActivity` started (S-51; S-15Δ4): from now on the tick and the click are made on its decor [view]. A new instance
     * started over a running one takes the view over before the old one stops. Main thread.
     */
    fun feedbackView(view: View) {
        device.view = view
    }

    /**
     * A `MainActivity` stopped (S-51): its decor [view] makes no tick or click any more. Only the activity that set the view
     * clears it, so an old instance stopping after a new one started leaves the new one's view in place. Main thread.
     */
    fun feedbackViewGone(view: View) {
        if (device.view === view) device.view = null
    }

    /**
     * The ticker runs exactly while the game is visible and the controller exists (S-15Δ3): `countTime()`, then
     * [TICK_MS] of `delay` on the main thread (design §3 library fact 4: the delay only paces it; between ticks the
     * main thread is idle). Cancelling it ends the wait; no further tick runs.
     */
    private fun syncTicker() {
        val c = built.value
        if (visibleNow && c != null) {
            if (ticker == null) {
                ticker = mainScope.launch {
                    while (isActive) {
                        c.countTime()
                        delay(TICK_MS)
                    }
                }
            }
        } else {
            ticker?.cancel()
            ticker = null
        }
    }

    /** The ticker's period (S-15Δ3: "`countTime()`, then `delay(250)`"). */
    private const val TICK_MS = 250L
}
