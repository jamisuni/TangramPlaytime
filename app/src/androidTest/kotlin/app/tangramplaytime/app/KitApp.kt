// Acceptance kit (WO-006, TASK-030): kit-prefixed helpers for the device kit of the real app (workorders/WO-006-briefs.md
// § TASK-030 "KitApp.kt"; tasks.md § WO-006 seam notes 2–5, the touch geometry per layout class, the player controls per
// screen and "What survives a rotation"). No token lives in this file.
//
// - A black box (seam note 2). The kit reads nodes (`PlayTags` S-13 and the S-13Δ strings, `PlaySemantics` S-14,
//   `DevAidTags` S-28), the controller through `AppGraph.controller` (designs/WO-004-design.md §2 S-15Δ: the kits' one door),
//   and the window insets from the launched activity's decor view. It changes device state only through `UiAutomation`
//   (`executeShellCommand`, `setRotation`). It calls no new `:app` seam, and nothing it does needs a permission in the app.
// - Every class leaves the controller and the device as it found them (seam note 3). Each change below registers its own
//   restore; `KitApp.restoreRule()` runs them after the test body, newest first, in two phases and then closes the
//   activity: the app's state (a held finger, the paused clock, the DEV sheet, the controller), then the device's state (the
//   display, the rotation, the locales, airplane mode, `show_ime_with_hard_keyboard`) through the shell alone, bounded and
//   failing loudly, waiting on nothing of the app (decisions.md 2026-10-18 "Worker wins three contests", part 3: "no
//   unbounded wait in a restore path … bounded waits that fail loudly"), then every activity of the app is closed.
//   `tools/device_reset.sh` (S-37) is the net.
// - No device setting changes while the home screen is in front: an activity of the app is always on top when the display,
//   the rotation or a locale changes. On the floor the home app crashes when the display changes under it, and its crash
//   dialog then holds the focus and keeps a finishing MainActivity from being destroyed (measured 2026-10-18 on
//   `Phone_API_26`, 14:12:03, 14:17:06 and 14:22:08: `am_crash` com.google.android.apps.nexuslauncher, a
//   NullPointerException in its relaunch after the kit's `wm size` / `wm density` with it resumed; "Showing crash dialog";
//   `ActivityScenario.close` then timed out "never becomes … DESTROYED (last … PAUSED)"). So a device restore runs before the
//   activity is closed, and a constructed tablet is set up under the running activity.
// - A size the device lacks is constructed, never skipped (seam note 4): a `wm size` + `wm density` override, read back from
//   `wm` and from the launched activity. A device that already has the size is not overridden. Portrait on a tablet is a
//   frozen rotation. A constructed tablet is a tablet the game starts on: the override and the rotation are set under the
//   running activity, and then a new activity is started, which takes the old one's place (the ruling's part 1; see
//   `useTablet`).
// - After a display change the system bars settle in steps for a while (the ruling's part 2): every value the kit reads from
//   the window insets, and every layout read that depends on them, is read once they have settled (`settledInsets`), never
//   from a snapshot taken before.
// - Positions. Every touch is injected on `play-screen`, in that node's own px (the play kit's pattern). Every comparison
//   with an inset is in window px (`SemanticsNode.boundsInWindow` against the decor view's insets). Dp are px over the
//   density read from the launched activity (platform-notes/android.md § Compose test kit), never from `targetContext`.
//   A point is read on the screen it is used on and asserted inside the region meant before it is touched (WO-005 lesson).
// - Nothing sleeps. Every wait is `composeRule.waitUntil` over an observed state, and it fails loudly at its timeout. That
//   binds the restore paths too (the ruling's part 3): a restore's Compose step runs only after a bounded frame probe
//   (`composeReady`), because Compose's own waits wait for a Choreographer frame without a bound.
// - Compose's own waits are bounded, and none of them runs while an activity relaunch may be running. Measured on the floor
//   (2026-10-18 14:48:14, `TabletLayoutKitTest`, the turn to portrait of a running tablet activity): a node fetch polled
//   while the activity relaunched never returned. The test thread was parked in `Espresso.onIdle` (`FutureTask.get`,
//   Espresso 3.7.0 line 349: its main-thread idle task never ran), under Compose's `waitForIdle` →
//   `waitForNextChoreographerFrame`. The app's main thread was idle (its queue held one ordinary delayed message, no sync
//   barrier) and the app answered input: one `input tap` turned the square in the tray and drew a frame. That is the same
//   stall the ruling's part 3 names, in a test body. So:
//   - a wait across a display change, a turn or a recreation reads the activity alone (`awaitSteadyActivity`: the same
//     MainActivity, resumed, unchanged for [KIT_APP_SETTLE_MS], no Compose call) before its first Compose read;
//   - every Compose call of the kit runs under [bounded]: if it does not return within [KIT_APP_COMPOSE_BOUND_MS], the kit
//     interrupts it and fails with a [KitComposeStall], which no wait polls again (a stall is reported, never retried).
package app.tangramplaytime.app

import android.app.Activity
import android.app.UiAutomation
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import app.tangramplaytime.content.PuzzleLibraries
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
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.BoardMapping
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.play.devaid.DevAidTags
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import java.lang.reflect.Modifier
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** The compose rule every WO-006 app class uses: the real `MainActivity` (S-15Δ), launched by the rule. */
internal typealias KitAppRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

/** The installed app's package (`app/build.gradle.kts` `applicationId`): the only package whose windows the player may see. */
internal const val KIT_APP_PACKAGE = "app.tangramplaytime"

/** TYPE-004's geometric tolerance, for placements compared as the engine returns them. */
internal const val KIT_APP_TOL = 1e-6

/** How long a wait on the app may take before the test fails instead of hanging. */
internal const val KIT_APP_WAIT_MS = 30_000L

/** How long a device-level change (a display override, a rotation, a locale) may take to reach the app; the floor is slow. */
internal const val KIT_APP_DEVICE_WAIT_MS = 60_000L

/**
 * How long the window insets (and the activity showing them) must read the same before they count as settled
 * (decisions.md 2026-10-18 "Worker wins three contests", part 2: "unchanged over consecutive reads"). After a display
 * change the bars move in steps (dev 63 → 72 px; tablet 64 → 48 → 72 px); the longest gap between two steps in TASK-031's
 * device logs is 0.75 s, so a reading unchanged for 2 s is past every measured step.
 */
internal const val KIT_APP_SETTLE_MS = 2_000L

/** How long a restore waits for the app to draw one frame before its Compose steps (the ruling's part 3); a frame is ~16 ms. */
internal const val KIT_APP_FRAME_PROBE_MS = 5_000L

/**
 * How long one of Compose's own waits (a node fetch, `waitForIdle`, a touch injection) may take before the kit stops it and
 * fails (the header). A healthy one takes milliseconds, or the length of the animation it lets finish (the longest, the
 * celebration, about 1.4 s).
 */
internal const val KIT_APP_COMPOSE_BOUND_MS = 60_000L

/**
 * One of Compose's own waits did not return within [KIT_APP_COMPOSE_BOUND_MS] and was stopped (the header). It is fatal: a
 * wait that meets it rethrows it instead of polling again, so a stall is reported, never retried away (decisions.md
 * 2026-10-18 "Worker wins three contests", part 3).
 */
internal class KitComposeStall(message: String, cause: Throwable?) : AssertionError(message, cause)

/** The one thread that stops a stalled Compose wait of the kit ([KitApp.bounded]). */
private val kitComposeWatch: ScheduledExecutorService by lazy {
    Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "kit-compose-bound").apply { isDaemon = true } }
}

/**
 * A board scale for engine searches from exact solution poses. The result does not depend on it: the lock distance is never
 * below 0.65 units (TYPE-004), wider than the 0.16-unit band in which the winning candidate is chosen (the play kit's
 * reading, `KitScreen.kt` `KIT_ANY_DP`).
 */
internal const val KIT_APP_ANY_DP = 100.0

/** The prefix of every #DevTools node's tag (the aid's tag table, S-28). REQ-037 rule 3 exempts them. */
internal const val KIT_APP_DEV_PREFIX = "dev-"

/** The S-13Δ tag strings (designs/WO-004-design.md §2: the seam fixes the strings; "A control's bounds are its touch area"). */
internal object KitAppTag {
    const val NAV_PREV = "nav-prev"
    const val NAV_NEXT = "nav-next"
    const val COUNTER = "counter"
    const val RESTART = "restart"
    const val SOLVED_ROW = "solved-row"
    const val RETRY = "retry"
    const val BEST_TIME = "best-time"
    const val SOLVED_NEXT = "solved-next"
    const val OVERVIEW = "overview"
    const val OVERVIEW_DONE = "overview-done"
    const val OVERVIEW_CELL_PREFIX = "overview-cell-"

    fun overviewCell(id: String): String = OVERVIEW_CELL_PREFIX + id
}

/** The one lock rule: the real `LockEngine` on the app's classpath, as `ProcessDeathKitTest` uses it. */
internal val kitAppEngine: ILockEngine by lazy { LockEngine() }

/** The bundled puzzles (the JSON files of `Tangrams`), read through `:content` on the app's classpath (TASK-030 Inputs). */
internal val kitAppLibrary: IPuzzleLibrary by lazy { PuzzleLibraries.bundled(kitAppEngine) }

// ── Displays and insets ───────────────────────────────────────────────────────────────────────────────

/** A display as `wm size` and `wm density` set it: px in the display's natural orientation, and its dpi. */
internal data class KitDisplay(val widthPx: Int, val heightPx: Int, val dpi: Int) {
    val widthDp: Float get() = widthPx * 160f / dpi
    val heightDp: Float get() = heightPx * 160f / dpi

    /** The smallest width in dp: the upright width TYPE-007 reads (planner Q1, `Configuration.smallestScreenWidthDp`). */
    val smallestDp: Float get() = min(widthDp, heightDp)

    /** REQ-036 A1's tablet: 1280 × 800 dp, in either natural orientation (within half a dp). */
    val isTablet1280By800: Boolean
        get() = abs(min(widthDp, heightDp) - 800f) <= 0.5f && abs(max(widthDp, heightDp) - 1280f) <= 0.5f

    override fun toString(): String =
        "${widthPx}x$heightPx px at $dpi dpi (${"%.1f".format(Locale.ROOT, widthDp)} × ${"%.1f".format(Locale.ROOT, heightDp)} dp)"
}

/** REQ-035 A1's 390 × 844 dp phone: 1170 × 2532 px at 480 dpi (workorders/WO-006-briefs.md, the per-image table). */
internal val KIT_PHONE_390 = KitDisplay(1170, 2532, 480)

/** The phone legs' 360 × 780 dp (decisions.md 2026-10-17 Q5 and Q9 rows): 1080 × 2340 px at 480 dpi (the per-image table). */
internal val KIT_PHONE_360 = KitDisplay(1080, 2340, 480)

/**
 * REQ-036 A1's tablet, 1280 × 800 dp, as the kit constructs it on a phone image: 2160 × 1350 px at 270 dpi, landscape at
 * rotation 0. REQ-036 A1 names the dp, not a px pair; this is a pair every image accepts (decisions.md 2026-10-18 "Worker wins
 * three contests", part 1: API 26 clamps a forced width to twice the initial width, so `wm size 2560x1600` reads back
 * 2160x1600 on the floor; 2160 × 1350 at 270 dpi was measured there in both orientations). The tablet image runs at its own
 * 2560 × 1600 px at 320 dpi = 1280 × 800 dp, overriding nothing (seam note 4).
 */
internal val KIT_TABLET_OVERRIDE = KitDisplay(2160, 1350, 270)

/** What `wm size` and `wm density` report: the physical display and any override. */
internal data class KitWm(val physical: KitDisplay, val overrideSize: String?, val overrideDensity: String?) {
    /** The display in force: the override where there is one, else the physical display. */
    val inForce: KitDisplay
        get() {
            val wh = overrideSize?.split('x')?.map { it.toInt() } ?: listOf(physical.widthPx, physical.heightPx)
            return KitDisplay(wh[0], wh[1], overrideDensity?.toInt() ?: physical.dpi)
        }
}

/** The launched activity's window and its insets, in window px (`ViewCompat.getRootWindowInsets` of the decor view). */
internal data class KitInsets(
    val windowWidthPx: Int,
    val windowHeightPx: Int,
    val navigationBars: Insets,
    val systemBars: Insets,
    val displayCutout: Insets,
    val ime: Insets,
) {
    /** S-35 step 3: the layout's insets are the system bars ∪ the display cutout, side by side (S-31: never the keyboard). */
    val safeLeft: Int get() = max(systemBars.left, displayCutout.left)
    val safeTop: Int get() = max(systemBars.top, displayCutout.top)
    val safeRight: Int get() = max(systemBars.right, displayCutout.right)
    val safeBottom: Int get() = max(systemBars.bottom, displayCutout.bottom)

    /** S-32: the safe area is the window minus those insets (window px). */
    val safe: Rect
        get() = Rect(safeLeft.toFloat(), safeTop.toFloat(), (windowWidthPx - safeRight).toFloat(), (windowHeightPx - safeBottom).toFloat())

    val window: Rect get() = Rect(0f, 0f, windowWidthPx.toFloat(), windowHeightPx.toFloat())

    override fun toString(): String =
        "window ${windowWidthPx}x$windowHeightPx px; navigationBars $navigationBars; systemBars $systemBars; displayCutout $displayCutout; ime $ime"
}

// ── Puzzle geometry (the kit's own reading of the solution; px only through BoardMapping) ─────────────

/** The same piece, turn and mirror, and centres within [tol] units (a placement compared as the engine returns it). */
internal fun kitAppSame(a: Placement?, b: Placement?, tol: Double = KIT_APP_TOL): Boolean =
    a != null && b != null && a.piece == b.piece && a.turn == b.turn && a.mirrored == b.mirrored &&
        abs(a.centre.x - b.centre.x) <= tol && abs(a.centre.y - b.centre.y) <= tol

/** The same placements, whatever their order. */
internal fun kitAppSameBoard(a: List<Placement>, b: List<Placement>): Boolean =
    a.size == b.size && b.all { x -> a.any { kitAppSame(it, x) } }

/**
 * A piece's solution pose, read from the puzzle (decisions.md 2026-10-17 "A solution pose is read from the puzzle, not
 * computed"). The centre is the polygon's average (`Placement.centre` KDoc). The turn and mirror are the first of turns 0..7,
 * unmirrored first and mirrored for the parallelogram only, whose `ILockEngine.vertices` equal the polygon within 1e-6.
 */
internal fun kitAppPose(p: PuzzleDefinition, piece: PieceId): Placement {
    val polygon = p.solution.single { it.piece == piece }.polygon
    val centre = Point(polygon.sumOf { it.x } / polygon.size, polygon.sumOf { it.y } / polygon.size)
    val mirrors = if (piece == PieceId.PG) listOf(false, true) else listOf(false)
    for (mirrored in mirrors) {
        for (step in 0..7) {
            val v = kitAppEngine.vertices(piece, Turn(step), mirrored, centre)
            val same = v.size == polygon.size &&
                v.all { a -> polygon.any { b -> abs(a.x - b.x) <= KIT_APP_TOL && abs(a.y - b.y) <= KIT_APP_TOL } } &&
                polygon.all { b -> v.any { a -> abs(a.x - b.x) <= KIT_APP_TOL && abs(a.y - b.y) <= KIT_APP_TOL } }
            if (same) return Placement(piece, Turn(step), mirrored, centre)
        }
    }
    throw AssertionError("kit: no turn and mirror of $piece reproduces its solution polygon in ${p.id}")
}

/**
 * An order in which every piece of [p] not yet in [placed] locks exactly at its solution pose against the ones before it, or
 * null. It is S-27's depth-first search over the remaining pieces, [prefer] first; REQ-019's rationale says such an order
 * exists from an empty board. A set of placed pieces with no way on is not tried again.
 */
internal fun kitAppLockOrder(
    p: PuzzleDefinition,
    placed: List<Placement> = emptyList(),
    prefer: List<PieceId> = p.pieces.toList(),
    dpPerUnit: Double = KIT_APP_ANY_DP,
): List<Placement>? {
    val dead = HashSet<Set<PieceId>>()
    fun search(on: List<Placement>, remaining: List<PieceId>): List<Placement>? {
        if (remaining.isEmpty()) return emptyList()
        val key = on.map { it.piece }.toSet()
        if (key in dead) return null
        for (piece in remaining) {
            val pose = kitAppPose(p, piece)
            val lock = kitAppEngine.findLock(p, on, piece, pose.turn, pose.mirrored, pose.centre, dpPerUnit)
            if (lock is LockResult.Locked && kitAppSame(lock.placement, pose)) {
                val rest = search(on + lock.placement, remaining - piece)
                if (rest != null) return listOf(lock.placement) + rest
            }
        }
        dead += key
        return null
    }
    val free = p.pieces.filter { piece -> placed.none { it.piece == piece } }
    val remaining = prefer.filter { it in free } + free.filter { it !in prefer }
    return search(placed, remaining)
}

/**
 * The title S-36 and the 2026-10-17 Q7 row give for [language]: "Finnish → `title.fi` (falling back to `title.en`); anything
 * else → `title.en`" (S-11: `puzzle.titles[titleLanguage] ?: puzzle.titles["en"]`).
 */
internal fun kitAppTitle(p: PuzzleDefinition, language: String): String =
    (if (language == "fi") p.titles["fi"] else null) ?: p.titles.getValue("en")

/** Inside the polygon (even-odd ray test). "What 'drawn' means" is the drawn shape exactly, edges only, no margin (decisions.md 2026-10-12). */
internal fun kitAppInside(o: Offset, polygon: List<Offset>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[j]
        if ((a.y > o.y) != (b.y > o.y)) {
            val x = a.x + (o.y - a.y) * (b.x - a.x) / (b.y - a.y)
            if (o.x < x) inside = !inside
        }
        j = i
    }
    return inside
}

/** The distance from [o] to the polygon's boundary, in the polygon's units. */
internal fun kitAppDistToBoundary(o: Offset, polygon: List<Offset>): Float {
    var best = Float.MAX_VALUE
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0f) 0f else (((o.x - a.x) * dx + (o.y - a.y) * dy) / len2).coerceIn(0f, 1f)
        best = min(best, hypot(o.x - (a.x + t * dx), o.y - (a.y + t * dy)))
    }
    return best
}

internal fun kitAppOnCircle(centre: Offset, radius: Float, deg: Double): Offset {
    val rad = Math.toRadians(deg)
    return Offset(centre.x + (radius * cos(rad)).toFloat(), centre.y + (radius * sin(rad)).toFloat())
}

/** [inner] lies inside [outer], each edge within [tolPx]. */
internal fun kitAppWithin(inner: Rect, outer: Rect, tolPx: Float = 0.5f): Boolean =
    inner.left >= outer.left - tolPx && inner.top >= outer.top - tolPx && inner.right <= outer.right + tolPx && inner.bottom <= outer.bottom + tolPx

/**
 * Q8 part (3)'s "no foreign view", as decisions.md 2026-10-17 "The device kit's open readings" (ruling 4) reads it: a
 * platform view (loaded by the boot class loader, as `View` itself is: the `android.*` widgets and the platform's own decor,
 * `com.android.internal.policy.DecorView`), an AndroidX view (`androidx.*`, Compose's host views) or the app's own
 * (`app.tangramplaytime.*`). An ad SDK's views load from the app's class loader under their own packages; a `WebView` is
 * caught on its own.
 */
internal fun kitAppIsPlatformAndroidXOrOwn(c: Class<*>): Boolean =
    c.classLoader == View::class.java.classLoader || c.name.startsWith("androidx.") || c.name.startsWith("$KIT_APP_PACKAGE.")

/** The `int` fields of a resource class: its resources by name. */
internal fun kitAppResourceIds(rClass: Class<*>): Map<String, Int> =
    rClass.fields.filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }.associate { it.name to it.getInt(null) }

// ── The kit ───────────────────────────────────────────────────────────────────────────────────────────

/**
 * The device kit's helpers over one compose rule. Each test class builds one and installs [restoreRule] as the rule inside
 * the compose rule, so every restore runs before the rule closes the activity.
 */
internal class KitApp(private val rule: KitAppRule) {

    val automation: UiAutomation get() = InstrumentationRegistry.getInstrumentation().uiAutomation
    val target: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    val library: IPuzzleLibrary get() = kitAppLibrary
    val sdk: Int get() = Build.VERSION.SDK_INT

    // ── Restores (seam note 3) ──

    /** The app's restores: they act on the open activity, first. */
    private val appRestores = ArrayList<Pair<String, () -> Unit>>()

    /**
     * The device's restores: they run after the app's and before the activity is closed (so the home screen is never in
     * front of a device change), through the shell alone; none of them waits on the app.
     */
    private val deviceRestores = ArrayList<Pair<String, () -> Unit>>()

    /**
     * Registers [block] as the restore of [key] in the app's phase (it may act on the open activity), once per key and test;
     * it runs after the test body, newest first.
     */
    fun onRestore(key: String, block: () -> Unit) {
        if (appRestores.none { it.first == key }) appRestores += key to block
    }

    /**
     * Registers [block] as the restore of the device-level setting [key], once per key and test; it runs after the app's
     * restores and before the activity is closed, newest first. It must not touch the app: shell commands and their
     * read-back only, each wait bounded and failing loudly (decisions.md 2026-10-18 "Worker wins three contests", part 3:
     * "no unbounded wait in a restore path … bounded waits that fail loudly").
     */
    fun onDeviceRestore(key: String, block: () -> Unit) {
        if (deviceRestores.none { it.first == key }) deviceRestores += key to block
    }

    /**
     * The rule that runs every registered restore after the test body, whatever its outcome (seam note 3): the app's phase,
     * then the device's phase with the app's activity still in front (the header: never a device change under the home
     * screen), then every activity of the app closed (the rule's and any the kit started).
     */
    fun restoreRule(): TestRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                var primary: Throwable? = null
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    primary = t
                }
                val failures = ArrayList<Throwable>()
                failures += runRestores(appRestores)
                failures += runRestores(deviceRestores)
                failures += closeActivities("after the restores")
                val first = primary
                if (first != null) {
                    failures.forEach { first.addSuppressed(it) }
                    throw first
                }
                if (failures.isNotEmpty()) {
                    val e = AssertionError(
                        "kit: the test body passed but ${failures.size} restore(s) failed (seam note 3: every class leaves the controller and the device as it found them); first: ${failures.first().message}",
                        failures.first(),
                    )
                    failures.drop(1).forEach { e.addSuppressed(it) }
                    throw e
                }
            }
        }
    }

    private fun runRestores(list: ArrayList<Pair<String, () -> Unit>>): List<Throwable> {
        val failures = ArrayList<Throwable>()
        for ((key, block) in list.asReversed().toList()) {
            try {
                block()
            } catch (t: Throwable) {
                failures += AssertionError("kit: restoring $key failed: $t", t)
            }
        }
        list.clear()
        return failures
    }

    /**
     * Closes every activity the kit started ([coldStart], [useRelaunched]), newest first, and the rule's own
     * (`ActivityScenario.close`, which fails at androidx.test's own timeout; closing a closed scenario does nothing, so the
     * rule's close after the test stays harmless). From here no activity of the app is on screen.
     */
    private fun closeActivities(what: String): List<Throwable> {
        val failures = ArrayList<Throwable>()
        for (s in started.asReversed().toList() + rule.activityRule.scenario) {
            try {
                s.close()
            } catch (t: Throwable) {
                failures += AssertionError("kit: closing MainActivity $what failed: $t", t)
            }
        }
        started.clear()
        return failures
    }

    /**
     * Whether a restore's Compose step can run: false when no MainActivity is resumed (there is nothing to act on), true when
     * one is and the app draws — a Choreographer frame callback, posted from the main looper, runs within
     * [KIT_APP_FRAME_PROBE_MS]. Otherwise it fails loudly. Compose's own waits (`waitForIdle`, every node fetch) wait for such
     * a frame without a bound; that is the hang of TASK-031's runs (decisions.md 2026-10-18 "Worker wins three contests",
     * part 3: "no unbounded wait in a restore path"). The frame wait is bounded and is posted, never run on the main thread.
     */
    private fun composeReady(what: String): Boolean {
        if (!live()) return false
        val frame = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post { Choreographer.getInstance().postFrameCallback { frame.countDown() } }
        if (!frame.await(KIT_APP_FRAME_PROBE_MS, TimeUnit.MILLISECONDS)) {
            throw AssertionError("kit: $what: MainActivity is resumed but the app drew no frame within $KIT_APP_FRAME_PROBE_MS ms, so this restore's Compose steps are not run (they would wait without a bound)")
        }
        return true
    }

    // ── The shell (UiAutomation's, seam note 2) ──

    fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText() }.trim()

    fun argument(name: String): String? = InstrumentationRegistry.getArguments().getString(name)

    // ── The activity ──

    /** The scenarios the kit started in this test ([coldStart], [useRelaunched]), oldest first; the last one is in use. */
    private val started = ArrayList<ActivityScenario<MainActivity>>()

    /** The scenario in use: the rule's, or the last one the kit started ([coldStart]) or the Back pin relaunched ([useRelaunched]). */
    val scenario: ActivityScenario<MainActivity> get() = started.lastOrNull() ?: rule.activityRule.scenario

    /** Uses [s] from here on; the restore's closing phase closes it (as well as its owner, if that closes it first). */
    fun useRelaunched(s: ActivityScenario<MainActivity>) {
        started += s
    }

    /**
     * Starts MainActivity anew on a device [prepare] changes (decisions.md 2026-10-18 "Worker wins three contests", part 1:
     * "start the activity after the override"): [prepare] runs under the activity in use (never under the home screen, the
     * header), then a new MainActivity is launched and used from here on. androidx.test launches it with
     * `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` (the event log's flags 0x10008000), so it takes the old activity's
     * place and the system finishes the old one; that old instance must be destroyed within [KIT_APP_DEVICE_WAIT_MS], so
     * one `play-screen` shows, or this fails loudly. S-35 classifies the layout "at each activity creation", so the new
     * activity is created on the device as [prepare] left it — a device the game starts on.
     */
    private fun coldStart(what: String, prepare: () -> Unit) {
        val old = activityOrNull()
        prepare()
        started += ActivityScenario.launch(MainActivity::class.java)
        if (old != null) {
            await("$what: the MainActivity in use before the start destroyed (its place taken by the new one)", KIT_APP_DEVICE_WAIT_MS) {
                onMain { old.isDestroyed }
            }
        }
    }

    fun scenarioState(): Lifecycle.State? = try {
        scenario.state
    } catch (t: Throwable) {
        null
    }

    fun activityOrNull(): MainActivity? = try {
        var found: MainActivity? = null
        scenario.onActivity { found = it }
        found
    } catch (t: Throwable) {
        null
    }

    fun activity(what: String): MainActivity =
        activityOrNull() ?: throw AssertionError("kit: no live MainActivity for $what (scenario state ${scenarioState()})")

    private fun live(): Boolean = activityOrNull() != null && scenarioState() == Lifecycle.State.RESUMED

    fun <T> onMain(block: () -> T): T = rule.runOnUiThread(block)

    /**
     * Runs one of Compose's own waits ([block]: a node fetch, `waitForIdle`, a touch injection) under a bound (the header):
     * when it has not returned after [KIT_APP_COMPOSE_BOUND_MS], the kit interrupts the waiting thread and fails with a
     * [KitComposeStall] naming [what]. The interrupt is armed only while [block] runs, and a flag it set is cleared after.
     */
    fun <T> bounded(what: String, block: () -> T): T {
        val caller = Thread.currentThread()
        val gate = Any()
        var running = true
        var fired = false
        val alarm = kitComposeWatch.schedule({
            synchronized(gate) {
                if (running) {
                    fired = true
                    caller.interrupt()
                }
            }
        }, KIT_APP_COMPOSE_BOUND_MS, TimeUnit.MILLISECONDS)
        try {
            return block()
        } catch (t: Throwable) {
            if (t !is KitComposeStall && synchronized(gate) { fired }) {
                throw KitComposeStall(
                    "kit: $what: one of Compose's own waits did not return within $KIT_APP_COMPOSE_BOUND_MS ms and was stopped " +
                        "(the KitApp.kt header: the floor's stall of 2026-10-18 14:48:14 — the test thread parked in Espresso.onIdle while " +
                        "the app's main thread was idle); this fails here, it is not polled again (decisions.md 2026-10-18 part 3). " +
                        "MainActivity: ${scenarioState()}",
                    t,
                )
            }
            throw t
        } finally {
            val wasFired = synchronized(gate) {
                running = false
                fired
            }
            alarm.cancel(false)
            if (wasFired) Thread.interrupted()
        }
    }

    /** `waitForIdle` under [bounded]. */
    private fun waitIdle(what: String = "an idle wait") = bounded(what) { rule.waitForIdle() }

    fun idle() = waitIdle()

    /** Lets Compose catch up where an activity is live and drawing (it is also a restore step: [composeReady] bounds it). */
    private fun idleIfLive() {
        if (composeReady("an idle after a controller action")) waitIdle("an idle after a controller action")
    }

    /**
     * Waits until [condition] holds, failing loudly with [what] at the timeout; an exception in a poll counts as "not yet",
     * except a [KitComposeStall], which fails at once (the header).
     */
    fun await(what: String, timeoutMs: Long = KIT_APP_WAIT_MS, condition: () -> Boolean) {
        var last: Throwable? = null
        try {
            rule.waitUntil(timeoutMs) {
                try {
                    condition()
                } catch (s: KitComposeStall) {
                    throw s
                } catch (t: Throwable) {
                    last = t
                    false
                }
            }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("kit: timed out after $timeoutMs ms waiting for $what" + (last?.let { " (last error: $it)" } ?: ""), e)
        }
    }

    /** Waits until [problems] reports none, failing loudly with the last report at the timeout (a [KitComposeStall] at once). */
    fun awaitNoProblems(what: String, timeoutMs: Long, problems: () -> String) {
        var last = "not polled"
        try {
            rule.waitUntil(timeoutMs) {
                last = try {
                    problems()
                } catch (s: KitComposeStall) {
                    throw s
                } catch (t: Throwable) {
                    "error: $t"
                }
                last.isEmpty()
            }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("$what: still after $timeoutMs ms: $last", e)
        }
    }

    /**
     * Waits, reading the activity alone (no Compose call: the header), until one MainActivity is resumed, meets [holds] and
     * stays the same instance for [KIT_APP_SETTLE_MS] — an observed state, never a sleep: no relaunch is running. It fails
     * loudly after [KIT_APP_DEVICE_WAIT_MS].
     */
    fun awaitSteadyActivity(what: String, holds: (MainActivity) -> Boolean = { true }) {
        var last: MainActivity? = null
        var since = 0L
        await("$what (one MainActivity, resumed, the same instance for $KIT_APP_SETTLE_MS ms: no relaunch running)", KIT_APP_DEVICE_WAIT_MS) {
            val a = activityOrNull()
            val t = SystemClock.uptimeMillis()
            if (a == null || scenarioState() != Lifecycle.State.RESUMED || !holds(a)) {
                last = null
                false
            } else if (a !== last) {
                last = a
                since = t
                false
            } else {
                t - since >= KIT_APP_SETTLE_MS
            }
        }
    }

    // ── Nodes ──

    fun nodes(tag: String): List<SemanticsNode> = bounded("fetching the nodes tagged `$tag`") {
        rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)
    }

    fun exists(tag: String): Boolean = nodes(tag).isNotEmpty()

    fun node(tag: String): SemanticsNode {
        val all = nodes(tag)
        if (all.size != 1) throw AssertionError("kit: ${all.size} nodes tagged `$tag`, expected exactly one")
        return all.single()
    }

    /** Every node of every compose root, unmerged. */
    fun allNodes(): List<SemanticsNode> = bounded("fetching every node") {
        rule.onAllNodes(SemanticsMatcher("any node") { true }, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)
    }

    /**
     * Waits for exactly one `play-screen` in a resumed activity: the IO load is invisible to Compose idling (WO-004 design §3).
     * The activity is first seen steady without Compose ([awaitSteadyActivity]; the header).
     */
    fun awaitScreen(what: String, timeoutMs: Long = KIT_APP_WAIT_MS) {
        awaitSteadyActivity("$what: before reading `play-screen`")
        await("`play-screen` in a resumed MainActivity ($what)", timeoutMs) {
            scenarioState() == Lifecycle.State.RESUMED && nodes(PlayTags.SCREEN).size == 1
        }
        waitIdle("$what: an idle after `play-screen`")
    }

    fun awaitExists(tag: String, what: String, timeoutMs: Long = KIT_APP_WAIT_MS) = await("`$tag` ($what)", timeoutMs) { exists(tag) }

    fun awaitGone(tag: String, what: String, timeoutMs: Long = KIT_APP_WAIT_MS) = await("no `$tag` ($what)", timeoutMs) { !exists(tag) }

    private fun screenOrigin(): Offset = node(PlayTags.SCREEN).boundsInRoot.topLeft

    /** [tag]'s bounds in `play-screen` px: the space every touch is injected in. */
    fun bounds(tag: String): Rect = node(tag).boundsInRoot.translate(-screenOrigin())

    fun boundsOf(n: SemanticsNode): Rect = n.boundsInRoot.translate(-screenOrigin())

    /** A node's whole layout box in `play-screen` px, whatever a scroll container clips of it. */
    fun unclippedBoundsOf(n: SemanticsNode): Rect {
        val o = n.positionInRoot - screenOrigin()
        return Rect(o.x, o.y, o.x + n.size.width, o.y + n.size.height)
    }

    /** `play-screen` itself, in its own px. */
    fun screenBounds(): Rect = boundsOf(node(PlayTags.SCREEN))

    /** [tag]'s bounds in window px: the space the insets are given in. */
    fun windowBounds(tag: String): Rect = node(tag).boundsInWindow

    fun <T> key(tag: String, k: SemanticsPropertyKey<T>): T =
        node(tag).config.getOrNull(k) ?: throw AssertionError("kit: `$tag` carries no ${k.name}")

    fun shownId(): String = key(PlayTags.SCREEN, PlaySemantics.PuzzleIdKey)

    fun shownState(): PuzzleState = key(PlayTags.SCREEN, PlaySemantics.PuzzleStateKey)

    fun mapping(): BoardMapping = key(PlayTags.BOARD, PlaySemantics.BoardMappingKey)

    fun toPx(p: Point): Offset = mapping().toPx(p)

    /** The merged text of a merged, inert node (the title: S-13 tag table). */
    fun text(tag: String): String = bounded("reading the text of `$tag`") {
        rule.onNodeWithTag(tag).fetchSemanticsNode("kit: merged node `$tag`").config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
    }

    // ── Density, window, display ──

    fun density(what: String = "the launched activity"): Float {
        val a = activity(what)
        return onMain { a.resources.displayMetrics.density }
    }

    fun dp(v: Float): Float = v * density()

    /** The decor view's size in px: the activity's window. */
    fun windowPx(what: String): Pair<Int, Int> {
        val a = activity(what)
        return onMain { a.window.decorView.width to a.window.decorView.height }
    }

    @Suppress("DEPRECATION")
    private fun displayPxOf(a: Activity): Pair<Int, Int> {
        if (sdk >= 30) {
            val b = a.windowManager.maximumWindowMetrics.bounds
            return b.width() to b.height()
        }
        val p = android.graphics.Point()
        a.windowManager.defaultDisplay.getRealSize(p)
        return p.x to p.y
    }

    @Suppress("DEPRECATION")
    private fun rotationOf(a: Activity): Int = if (sdk >= 30) (a.display?.rotation ?: -1) else a.windowManager.defaultDisplay.rotation

    /** The display's current rotation as the launched activity reads it. */
    fun displayRotation(what: String): Int {
        val a = activity(what)
        return onMain { rotationOf(a) }
    }

    /**
     * The launched activity's window and insets, in window px, as they read now. An expected value derived from them is read
     * through [settledInsets] instead (decisions.md 2026-10-18 "Worker wins three contests", part 2).
     */
    fun insets(what: String): KitInsets = insetsOf(activity(what), what)

    private fun insetsOf(a: Activity, what: String): KitInsets = onMain {
        val decor = a.window.decorView
        val w = ViewCompat.getRootWindowInsets(decor) ?: throw AssertionError("kit: $what: the decor view has no root window insets")
        KitInsets(
            decor.width,
            decor.height,
            w.getInsets(WindowInsetsCompat.Type.navigationBars()),
            w.getInsets(WindowInsetsCompat.Type.systemBars()),
            w.getInsets(WindowInsetsCompat.Type.displayCutout()),
            w.getInsets(WindowInsetsCompat.Type.ime()),
        )
    }

    /**
     * The window insets once they have settled (decisions.md 2026-10-18 "Worker wins three contests", part 2: REQ-035 rule 3
     * binds the live insets, and after a display change the bars settle up to seconds later): the same MainActivity, resumed,
     * with the same window size and insets in every read across [KIT_APP_SETTLE_MS] — an observed state, never a sleep, read
     * from the decor view alone (no Compose call while a relaunch may run: the header). It fails loudly after
     * [KIT_APP_DEVICE_WAIT_MS]; then one `play-screen` is required and Compose is idled, so the layout read next is the one for
     * these insets. A caller that reads nodes against the result checks with [assertInsetsStill] afterwards.
     */
    fun settledInsets(what: String): KitInsets {
        var last: Pair<Activity, KitInsets>? = null
        var since = 0L
        await("$what: the window insets to settle (one MainActivity, resumed, the same instance and insets for $KIT_APP_SETTLE_MS ms)", KIT_APP_DEVICE_WAIT_MS) {
            val a = activityOrNull()
            if (a == null || scenarioState() != Lifecycle.State.RESUMED) {
                last = null
                false
            } else {
                val now = a to insetsOf(a, what)
                val t = SystemClock.uptimeMillis()
                val before = last
                if (before == null || before.first !== now.first || before.second != now.second) {
                    last = now
                    since = t
                    false
                } else {
                    t - since >= KIT_APP_SETTLE_MS
                }
            }
        }
        val settled = last?.second ?: throw AssertionError("kit: $what: no settled insets were read")
        await("$what: one `play-screen` once the insets settled") { nodes(PlayTags.SCREEN).size == 1 }
        waitIdle("$what: an idle once the insets settled")
        return settled
    }

    /**
     * After reading nodes against [settled]: the insets still read the same, so the reads were of one state. A change here is
     * the kit's (a settle window too short), named as such, never laid on the product.
     */
    fun assertInsetsStill(settled: KitInsets, what: String) {
        val now = insets(what)
        if (now != settled) {
            throw AssertionError("kit: $what: the window insets moved after they had settled ($settled → $now); the reads against them are not of one state (decisions.md 2026-10-18 part 2)")
        }
    }

    fun wm(): KitWm {
        val size = shell("wm size")
        val density = shell("wm density")
        val physical = Regex("Physical size: (\\d+)x(\\d+)").find(size) ?: throw AssertionError("kit: `wm size` printed '$size'")
        val physicalDpi = Regex("Physical density: (\\d+)").find(density) ?: throw AssertionError("kit: `wm density` printed '$density'")
        return KitWm(
            KitDisplay(physical.groupValues[1].toInt(), physical.groupValues[2].toInt(), physicalDpi.groupValues[1].toInt()),
            Regex("Override size: (\\d+x\\d+)").find(size)?.groupValues?.get(1),
            Regex("Override density: (\\d+)").find(density)?.groupValues?.get(1),
        )
    }

    /**
     * Puts the display at [d] (seam note 4) over the running activity: [applyDisplay], then waits until the launched activity
     * reports [d] (its density and its real display size) and shows `play-screen`.
     */
    fun useDisplay(d: KitDisplay, what: String) {
        applyDisplay(d, what)
        awaitDisplay(d, what, needScreen = true)
    }

    /**
     * A `wm size` + `wm density` override for [d], or none on a device whose own display is [d], read back from `wm` (a size
     * the device clamps fails here, by name: decisions.md 2026-10-18 "Worker wins three contests", part 1). Its restore, in
     * the device phase, puts back the override found before (normally none) and reads `wm` back.
     */
    private fun applyDisplay(d: KitDisplay, what: String) {
        val before = wm()
        onDeviceRestore("the display") { restoreDisplay(before) }
        if (before.physical == d) {
            if (before.overrideSize != null) shell("wm size reset")
            if (before.overrideDensity != null) shell("wm density reset")
        } else {
            shell("wm size ${d.widthPx}x${d.heightPx}")
            shell("wm density ${d.dpi}")
        }
        val now = wm()
        if (now.inForce != d) {
            throw AssertionError("kit: $what: after setting the display $d, `wm` reads $now (API $sdk; a forced size this device clamps or refuses)")
        }
    }

    /**
     * Waits until the launched activity reports [d] (its density and its real display size), read from the activity alone and
     * steady ([awaitSteadyActivity]: no relaunch running, the header); then, with [needScreen], one `play-screen`.
     */
    private fun awaitDisplay(d: KitDisplay, what: String, needScreen: Boolean) {
        awaitSteadyActivity("$what: the launched activity to report the display $d") { a ->
            onMain {
                val (w, h) = displayPxOf(a)
                a.resources.configuration.densityDpi == d.dpi &&
                    min(w, h) == min(d.widthPx, d.heightPx) && max(w, h) == max(d.widthPx, d.heightPx)
            }
        }
        if (needScreen) {
            await("$what: one `play-screen` on the display $d") { nodes(PlayTags.SCREEN).size == 1 }
            waitIdle("$what: an idle on the display $d")
        }
    }

    /**
     * The device phase's display restore, under the app's activity (never under the home screen, the header): it reads `wm`
     * back and waits on nothing of the app (the ruling's part 3); the activity is closed after it, and the next one is
     * created on the restored display.
     */
    private fun restoreDisplay(before: KitWm) {
        shell(before.overrideSize?.let { "wm size $it" } ?: "wm size reset")
        shell(before.overrideDensity?.let { "wm density $it" } ?: "wm density reset")
        val now = wm()
        if (now.overrideSize != before.overrideSize || now.overrideDensity != before.overrideDensity) {
            throw AssertionError("kit: after the restore `wm` reads $now, expected $before")
        }
    }

    /** The device's own display, as `wm` reports it physically. */
    fun ownDisplay(): KitDisplay = wm().physical

    /**
     * The phone at [d], upright (REQ-035 statement: portrait only; S-35 step 2: a phone requests portrait), read back from the
     * launched activity: the display is [d] in dp, upright; then the activity and its insets settle ([settledInsets]).
     * The override is set over the running activity: on the API 37 images a display override recreates it ("What survives a
     * rotation"), and on the floor a phone stays a phone, its configuration change reaching the screen (decisions.md
     * 2026-10-18 part 1 narrows only the tablet).
     */
    fun usePhone(d: KitDisplay, what: String) {
        useDisplay(d, what)
        awaitDisplayDp(min(d.widthDp, d.heightDp), max(d.widthDp, d.heightDp), what, needScreen = true)
        settledInsets("$what, after the display change")
    }

    /** REQ-036 A1's tablet on this device: its own display where that is 1280 × 800 dp (seam note 4), else [KIT_TABLET_OVERRIDE]. */
    fun tabletDisplay(): KitDisplay = ownDisplay().takeIf { it.isTablet1280By800 } ?: KIT_TABLET_OVERRIDE

    /**
     * The tablet at 1280 × 800 dp (landscape) or 800 × 1280 dp (portrait), read back from the launched activity (seam note 4:
     * "Portrait on a tablet is a frozen rotation"; landscape is the rotation at which [tabletDisplay] is wider than tall).
     * - When that display is already in force (the tablet image's own; or a constructed tablet earlier in the test), only the
     *   rotation is frozen: a turn of the running activity, the turn REQ-036 A2 and "What survives a rotation" mean.
     * - Otherwise the tablet is constructed, and it is a tablet the game starts on (decisions.md 2026-10-18 "Worker wins three
     *   contests", part 1): the override and the rotation are set under the running activity, then a new activity is started
     *   in its place ([coldStart]). On API 26 an override over a running activity is no recreation (the instance keeps its
     *   phone layout, measured), and on the API 37 images it is one; the kit takes the cold start on both phone images, so the
     *   dev run takes the floor's route and every constructed tablet is the device REQ-036 A1 names, started on, never a phone
     *   grown into one (no REQ names that).
     * With [needScreen] the activity and its insets then settle ([settledInsets]); without it (a paused clock) the caller
     * reads the first frames itself.
     */
    fun useTablet(portrait: Boolean, what: String, needScreen: Boolean = true) {
        val d = tabletDisplay()
        val landscapeAtZero = d.widthPx >= d.heightPx
        val rotation = if (portrait == landscapeAtZero) UiAutomation.ROTATION_FREEZE_90 else UiAutomation.ROTATION_FREEZE_0
        if (wm().inForce == d) {
            freezeRotation(rotation, what)
        } else {
            coldStart(what) {
                applyDisplay(d, what)
                freezeRotation(rotation, what)
            }
            awaitDisplay(d, "$what (started on the tablet)", needScreen = true)
        }
        val wide = max(d.widthDp, d.heightDp)
        val narrow = min(d.widthDp, d.heightDp)
        if (portrait) awaitDisplayDp(narrow, wide, what, needScreen) else awaitDisplayDp(wide, narrow, what, needScreen)
        if (needScreen) settledInsets("$what, after the display change")
    }

    /**
     * Waits until the launched activity's display is [widthDp] × [heightDp] dp as it stands now (within 1 dp), read from the
     * activity alone and steady ([awaitSteadyActivity]: a turn's relaunch is over, the header); then, with [needScreen], one
     * `play-screen`. Without it (a paused clock) no Compose call is made: the caller reads the first frames itself.
     */
    fun awaitDisplayDp(widthDp: Float, heightDp: Float, what: String, needScreen: Boolean) {
        awaitSteadyActivity("$what: the launched activity's display $widthDp × $heightDp dp") { a ->
            onMain {
                val density = a.resources.displayMetrics.density
                val (w, h) = displayPxOf(a)
                abs(w / density - widthDp) <= 1f && abs(h / density - heightDp) <= 1f
            }
        }
        if (needScreen) {
            await("$what: one `play-screen` at $widthDp × $heightDp dp") { nodes(PlayTags.SCREEN).size == 1 }
            waitIdle("$what: an idle at $widthDp × $heightDp dp")
        }
    }

    // ── Rotation (UiAutomation.setRotation, seam note 4) ──

    /**
     * Freezes the rotation at [rotation] (a `UiAutomation.ROTATION_FREEZE_*` constant) and waits until the settings read it
     * back. The restore, in the device phase, puts back `user_rotation` and `accelerometer_rotation` as read before.
     */
    fun freezeRotation(rotation: Int, what: String) {
        val accelerometer = shell("settings get system accelerometer_rotation").toIntOrNull()
        val user = shell("settings get system user_rotation").toIntOrNull()
        onDeviceRestore("the rotation") { restoreRotation(accelerometer, user) }
        if (!automation.setRotation(rotation)) throw AssertionError("kit: UiAutomation.setRotation($rotation) refused ($what)")
        await("$what: user_rotation $rotation and accelerometer_rotation 0", KIT_APP_DEVICE_WAIT_MS) {
            shell("settings get system user_rotation").toIntOrNull() == rotation &&
                shell("settings get system accelerometer_rotation").toIntOrNull() == 0
        }
    }

    /**
     * The device phase's rotation restore, under the app's activity (never under the home screen, the header) and through the
     * settings alone: no Compose wait follows the thaw (decisions.md 2026-10-18 "Worker wins three contests", part 3: TASK-031's
     * runs hung in an unbounded Compose wait after the thaw), and the activity is closed after it.
     * - Where the rotation was free (`accelerometer_rotation` not 0, every image's default): it thaws first, from wherever the
     *   test froze it (`UiAutomation.setRotation(ROTATION_UNFREEZE)`), and reads that back; then it writes `user_rotation`
     *   back as read before, the S-37 reset's own route (`tools/device_reset.sh`: `settings put system …`), which is not in
     *   force while the rotation is free, so nothing turns.
     * - Where it was locked: it freezes at the `user_rotation` read before.
     * - Then both settings must read the values as read before, unchanged across [KIT_APP_SETTLE_MS], or it fails loudly with
     *   the values read. One pass: nothing is tried again, nothing waits on the app.
     * Why the thaw comes first: on API 37 SystemUI locks the rotation again whenever the display turns back to its natural
     * rotation while the rotation is locked, asynchronously (`dumpsys window displays`, RotationLockHistory, dev image
     * 2026-10-18 10:20:30: `mode=USER_ROTATION_LOCKED … caller=RotationButtonController#onRotationWatcherChanged`), so a
     * freeze at 0 followed by a thaw, or settings written while it is locked, can be overwritten after their read-back (two
     * restores of the dev run timed out reading `accelerometer_rotation 0`). The old three-attempt loop hid that race.
     */
    private fun restoreRotation(accelerometer: Int?, user: Int?) {
        if (accelerometer != 0) {
            if (!automation.setRotation(UiAutomation.ROTATION_UNFREEZE)) throw AssertionError("kit: UiAutomation.setRotation(ROTATION_UNFREEZE) refused (restoring the rotation)")
            await("the rotation thawed (accelerometer_rotation ${accelerometer ?: 1})", KIT_APP_DEVICE_WAIT_MS) {
                shell("settings get system accelerometer_rotation").toIntOrNull() == (accelerometer ?: 1)
            }
            if (user != null && shell("settings get system user_rotation").toIntOrNull() != user) shell("settings put system user_rotation $user")
        } else {
            val u = user ?: 0
            if (!automation.setRotation(u)) throw AssertionError("kit: UiAutomation.setRotation($u) refused (restoring the rotation)")
        }
        val wanted = "user_rotation ${user ?: "(as found)"}, accelerometer_rotation ${accelerometer ?: "(as found)"}"
        var read = ""
        var holding = false
        var since = 0L
        try {
            await("the rotation settings to read $wanted, unchanged for $KIT_APP_SETTLE_MS ms", KIT_APP_DEVICE_WAIT_MS) {
                val usr = shell("settings get system user_rotation")
                val acc = shell("settings get system accelerometer_rotation")
                read = "user_rotation $usr, accelerometer_rotation $acc"
                val ok = (user == null || usr.toIntOrNull() == user) && (accelerometer == null || acc.toIntOrNull() == accelerometer)
                val t = SystemClock.uptimeMillis()
                when {
                    !ok -> { holding = false; false }
                    !holding -> { holding = true; since = t; false }
                    else -> t - since >= KIT_APP_SETTLE_MS
                }
            }
        } catch (e: AssertionError) {
            throw AssertionError("${e.message}; last read: $read", e)
        }
    }

    /** Waits until the accessibility event stream is quiet: the system has acted on a device request it may refuse. */
    fun awaitQuiet(what: String) {
        try {
            automation.waitForIdle(1_000, KIT_APP_DEVICE_WAIT_MS)
        } catch (e: TimeoutException) {
            throw AssertionError("kit: $what: the device did not settle within $KIT_APP_DEVICE_WAIT_MS ms", e)
        }
        awaitSteadyActivity("$what: after the device settled")
        waitIdle("$what: an idle after the device settled")
    }

    // ── Locales (seam note 5; S-36) ──

    /** The device's own locale list (REQ-047 "the device language"), never the activity's: that one is the app's rule, S-36. */
    fun deviceLocales(): LocaleList = Resources.getSystem().configuration.locales

    /**
     * Seam note 5 and decisions.md 2026-10-17 "The floor routes, as measured": the floor channel line names the device locale
     * list it set by the root route (`kitDeviceLocales`: one locale or a comma-separated list such as `sv-SE,fi-FI`, in order);
     * null in an ordinary run.
     */
    fun channelDeviceLocales(): List<Locale>? =
        argument("kitDeviceLocales")?.split(',')?.map { Locale.forLanguageTag(it.trim()) }

    /** Seam note 5: the floor channel line switched airplane mode on by the measured root route (floor step (e), `kitAirplaneByChannel=true`). */
    fun channelAirplane(): Boolean = argument("kitAirplaneByChannel") == "true"

    /** [list] starts with [wanted]: the same languages, and the same regions where [wanted] names one. */
    fun localesStartWith(list: LocaleList, wanted: List<Locale>): Boolean =
        list.size() >= wanted.size && wanted.indices.all { i ->
            list[i].language == wanted[i].language && (wanted[i].country.isEmpty() || list[i].country == wanted[i].country)
        }

    /**
     * Sets the device's locale list to [tags] with `cmd locale` (API 33+, seam note 5) and waits until the device reads it and a
     * new activity shows `play-screen` (S-36: "A device language change recreates the activity"). The restore puts back the
     * list read before.
     */
    fun setDeviceLocales(tags: String, what: String) {
        check(sdk >= 33) { "kit: the `cmd locale` legs run on API 33+ only (seam note 5)" }
        val before = deviceLocales().toLanguageTags()
        val setting = shell("settings get system system_locales")
        onDeviceRestore("the device locales") {
            applyDeviceLocales(before, "the device locales restored to $before", awaitApp = false)
            // decisions.md 2026-10-17 "The floor routes, as measured" (6): the API 33+ restore also puts `system_locales` back.
            if (setting == "null" || setting.isEmpty()) shell("settings delete system system_locales") else shell("settings put system system_locales $setting")
            val now = shell("settings get system system_locales")
            if (now != setting && !(setting.isEmpty() && now == "null")) throw AssertionError("kit: system_locales reads '$now' after the restore, expected '$setting'")
        }
        applyDeviceLocales(tags, what)
    }

    /**
     * Sets the device's locale list and reads it back. With [awaitApp] (the test body) it then waits for the new activity S-36
     * names; a restore passes false and waits on nothing of the app (decisions.md 2026-10-18 part 3).
     */
    private fun applyDeviceLocales(tags: String, what: String, awaitApp: Boolean = true) {
        val wanted = tags.split(',').map { Locale.forLanguageTag(it.trim()) }
        if (deviceLocales().size() == wanted.size && localesStartWith(deviceLocales(), wanted)) return
        val old = if (awaitApp) activityOrNull() else null
        val out = shell("cmd locale set-device-locale $tags")
        await("$what: the device's locale list to read $tags (`cmd locale set-device-locale $tags` printed '$out')", KIT_APP_DEVICE_WAIT_MS) {
            localesStartWith(deviceLocales(), wanted)
        }
        if (old != null) awaitRecreated(old, what)
    }

    /** The app's own locale list, as `cmd locale get-app-locales` prints it between its brackets ("" when none is set). */
    fun appLocaleTags(): String {
        val out = shell("cmd locale get-app-locales $KIT_APP_PACKAGE")
        val open = out.lastIndexOf('[')
        val close = out.lastIndexOf(']')
        if (open < 0 || close < open) throw AssertionError("kit: `cmd locale get-app-locales $KIT_APP_PACKAGE` printed '$out'")
        return out.substring(open + 1, close).trim()
    }

    /**
     * Sets the app's locale list to [tags] with `cmd locale set-app-locales` (API 33+, seam note 5) and waits for the read-back
     * and a new activity. The restore puts back the list read before (normally none: the app locales cleared).
     */
    fun setAppLocales(tags: String, what: String) {
        check(sdk >= 33) { "kit: the `cmd locale` legs run on API 33+ only (seam note 5)" }
        val before = appLocaleTags()
        onDeviceRestore("the app locales") { applyAppLocales(before, "the app locales restored to [$before]", awaitApp = false) }
        applyAppLocales(tags, what)
    }

    /** As [applyDeviceLocales], for the app's own list: a restore passes [awaitApp] false and waits on nothing of the app. */
    private fun applyAppLocales(tags: String, what: String, awaitApp: Boolean = true) {
        if (sameTags(appLocaleTags(), tags)) return
        val old = if (awaitApp) activityOrNull() else null
        val out = shell(if (tags.isEmpty()) "cmd locale set-app-locales $KIT_APP_PACKAGE" else "cmd locale set-app-locales $KIT_APP_PACKAGE --locales $tags")
        await("$what: the app's locale list to read [$tags] (`cmd locale set-app-locales` printed '$out')", KIT_APP_DEVICE_WAIT_MS) {
            sameTags(appLocaleTags(), tags)
        }
        if (old != null) awaitRecreated(old, what)
    }

    private fun sameTags(a: String, b: String): Boolean =
        a.split(',').map { it.trim() }.filter { it.isNotEmpty() } == b.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * S-36: "A device language change recreates the activity, so strings and titles follow it together." The new instance is
     * seen steady without Compose first ([awaitSteadyActivity]; the header), then showing `play-screen`.
     */
    private fun awaitRecreated(old: Activity, what: String) {
        awaitSteadyActivity("$what: a new MainActivity") { it !== old }
        await("$what: the new MainActivity showing `play-screen`", KIT_APP_DEVICE_WAIT_MS) {
            val a = activityOrNull()
            a != null && a !== old && scenarioState() == Lifecycle.State.RESUMED && nodes(PlayTags.SCREEN).size == 1
        }
        waitIdle("$what: an idle in the new MainActivity")
    }

    // ── Airplane mode, the keyboard setting, the navigation mode ──

    /** `settings get global airplane_mode_on`, readable on every API. */
    fun airplaneModeOn(): Boolean = shell("settings get global airplane_mode_on") == "1"

    /**
     * Airplane mode on by `cmd connectivity airplane-mode enable` (API 30+, seam note 5), read back. Where the kit switched it
     * on, the restore switches it off and reads that back.
     */
    fun switchAirplaneModeOn(what: String) {
        check(sdk >= 30) { "kit: `cmd connectivity airplane-mode` needs API 30 (seam note 5): below it the channel line sets airplane mode" }
        val before = airplaneModeOn()
        if (!before) {
            onDeviceRestore("airplane mode") {
                shell("cmd connectivity airplane-mode disable")
                await("airplane mode off again (`settings get global airplane_mode_on` 0)", KIT_APP_DEVICE_WAIT_MS) { !airplaneModeOn() }
            }
        }
        shell("cmd connectivity airplane-mode enable")
        await("$what: airplane mode on (`settings get global airplane_mode_on` 1)", KIT_APP_DEVICE_WAIT_MS) { airplaneModeOn() }
    }

    /** `show_ime_with_hard_keyboard` 1, so the emulator's soft keyboard shows beside its hardware keyboard; restored as read. */
    fun showImeWithHardKeyboard(what: String) {
        val before = shell("settings get secure show_ime_with_hard_keyboard")
        onDeviceRestore("show_ime_with_hard_keyboard") {
            if (before == "null" || before.isEmpty()) shell("settings delete secure show_ime_with_hard_keyboard")
            else shell("settings put secure show_ime_with_hard_keyboard $before")
            val now = shell("settings get secure show_ime_with_hard_keyboard")
            if (now != before && !(before.isEmpty() && now == "null")) {
                throw AssertionError("kit: show_ime_with_hard_keyboard reads '$now' after the restore, expected '$before'")
            }
        }
        shell("settings put secure show_ime_with_hard_keyboard 1")
        await("$what: show_ime_with_hard_keyboard 1") { shell("settings get secure show_ime_with_hard_keyboard") == "1" }
    }

    /** `settings get secure navigation_mode` on API 29+ (2 is gesture navigation; REQ-035 A2's evidence, the channel table). */
    fun navigationMode(): String = if (sdk >= 29) shell("settings get secure navigation_mode") else "(no navigation_mode below API 29)"

    // ── The controller (S-15Δ: `AppGraph.controller`, the kits' one door; main thread only, S-2) ──

    fun controller(): PlayController =
        AppGraph.controller.value ?: throw AssertionError("kit: AppGraph.controller is null once `play-screen` is shown (S-15Δ)")

    fun <T> onController(block: PlayController.() -> T): T {
        val c = controller()
        return onMain { c.block() }
    }

    private class KitSnapshot(
        val openId: String,
        val states: Map<String, PuzzleState>,
        val placed: List<Placement>,
        val trayTurns: Map<PieceId, Turn>,
        val trayMirrored: Map<PieceId, Boolean>,
        val overviewOpen: Boolean,
    )

    /**
     * The start of every test: `play-screen` shown, nothing open, and the controller's state recorded so that the restore
     * leaves it as found (seam note 3: "The open puzzle and every state it changed come back; the overview and the DEV sheet
     * are closed").
     */
    fun start(what: String) {
        awaitScreen(what)
        if (exists(DevAidTags.SHEET)) throw AssertionError("kit: precondition ($what): the DEV sheet is open at the start; a class before this one left it open")
        val ids = library.puzzles.map { it.id }
        val snap = onController {
            KitSnapshot(
                puzzle.id,
                ids.associateWith { stateOf(it) },
                placed.toList(),
                trayPieces.associateWith { trayTurn(it) },
                trayPieces.associateWith { trayMirrored(it) },
                overviewOpen,
            )
        }
        onRestore("the controller") { restoreController(snap) }
    }

    private fun restoreController(snap: KitSnapshot) {
        val c = AppGraph.controller.value ?: return
        if (composeReady("the controller's restore") && exists(PlayTags.SCREEN)) closeDevSheet()
        onMain {
            if (c.dragged != null) c.goHome()
            if (c.overviewOpen) c.closeOverview()
        }
        val problems = ArrayList<String>()
        for ((id, then) in snap.states) {
            if (id == snap.openId) continue
            val now = onMain { c.stateOf(id) }
            if (now == then) continue
            if (then != PuzzleState.NEW) {
                problems += "$id is $now and was $then: the kit cannot rebuild a board it did not see"
                continue
            }
            onMain {
                check(c.open(id)) { "kit: open($id) refused (restore)" }
                when (c.state) {
                    PuzzleState.IN_PROGRESS -> c.restart()
                    PuzzleState.SOLVED -> c.retry()
                    PuzzleState.NEW -> Unit
                }
            }
        }
        onMain { check(c.open(snap.openId)) { "kit: open(${snap.openId}) refused (restore)" } }
        idleIfLive()
        rebuildBoard(c, snap.states.getValue(snap.openId), snap.placed)
        onMain {
            for ((piece, turn) in snap.trayTurns) {
                if (piece !in c.trayPieces) continue
                var guard = 0
                while (c.trayTurn(piece) != turn && guard++ < 8) c.tapTray(piece)
            }
            val mirrored = snap.trayMirrored[PieceId.PG]
            if (mirrored != null && PieceId.PG in c.trayPieces && c.trayMirrored(PieceId.PG) != mirrored) c.pressBadge()
            if (snap.overviewOpen && !c.overviewOpen) c.openOverview()
        }
        idleIfLive()
        flush(c)
        val back = onMain { Triple(c.puzzle.id, snap.states.keys.associateWith { c.stateOf(it) }, c.placed.toList()) }
        if (back.first != snap.openId) problems += "the open puzzle is ${back.first}, was ${snap.openId}"
        for ((id, then) in snap.states) if (back.second[id] != then) problems += "$id is ${back.second[id]}, was $then"
        if (!kitAppSameBoard(back.third, snap.placed)) problems += "the open puzzle's board is ${back.third}, was ${snap.placed}"
        if (problems.isNotEmpty()) throw AssertionError("kit: the controller was not left as found (seam note 3): $problems")
    }

    private fun rebuildBoard(c: PlayController, state: PuzzleState, placed: List<Placement>) {
        val now = onMain { c.state to c.placed.toList() }
        if (now.first == state && kitAppSameBoard(now.second, placed)) return
        onMain {
            when (c.state) {
                PuzzleState.IN_PROGRESS -> c.restart()
                PuzzleState.SOLVED -> c.retry()
                PuzzleState.NEW -> Unit
            }
        }
        when (state) {
            PuzzleState.NEW -> Unit
            PuzzleState.IN_PROGRESS ->
                if (placed.isEmpty()) {
                    // TYPE-006: a drag that starts on a tray piece moves New → In progress; going home keeps that state.
                    onMain {
                        val piece = c.trayPieces.first()
                        check(c.startDrag(piece, kitAppPose(c.puzzle, piece).centre)) { "kit: startDrag($piece) refused (restore)" }
                        c.goHome()
                    }
                } else {
                    placed.forEach { placeByController(it, "rebuilding the open puzzle's board") }
                }
            PuzzleState.SOLVED -> solveByController("rebuilding the open puzzle's solve")
        }
        idleIfLive()
    }

    /** S-2Δ `flush()` (S-19 `suspend fun flush()`): every queued write reaches the store; a Job it might return is joined. */
    fun flush(c: PlayController = controller()) {
        runBlocking {
            withTimeout(KIT_APP_WAIT_MS) {
                val result: Any? = c.flush()
                if (result is Job) result.join()
            }
        }
    }

    /** Closes the DEV sheet by Cancel or Done (the aid's touch geometry: "A tap on the scrim, Cancel or Done closes it"). */
    fun closeDevSheet() {
        if (!exists(DevAidTags.SHEET)) return
        when {
            exists(DevAidTags.CANCEL) -> pressCentre(DevAidTags.CANCEL)
            exists(DevAidTags.DONE) -> pressCentre(DevAidTags.DONE)
            else -> throw AssertionError("kit: the DEV sheet is open with neither Cancel nor Done")
        }
        awaitGone(DevAidTags.SHEET, "the DEV sheet closed by Cancel or Done")
    }

    /**
     * Registers the DEV sheet's restore (seam note 3: "the overview and the DEV sheet are closed"), in the app's phase: it
     * closes the sheet by Cancel or Done where an activity is open and drawing ([composeReady]; the aid's sheet is
     * process-wide, so it outlives the activity and must be closed while one shows it).
     */
    fun closeDevSheetAfterTheTest() {
        onRestore("the DEV sheet") { if (composeReady("closing the DEV sheet") && exists(PlayTags.SCREEN)) closeDevSheet() }
    }

    /** The first puzzle of [kind] that is New and meets [extra]; the fixtures start from a cleared store (the channel's reset). */
    fun firstNew(kind: PuzzleKind, what: String, extra: (PuzzleDefinition) -> Boolean = { true }): PuzzleDefinition {
        val all = library.puzzles
        val states = onController { all.associate { it.id to stateOf(it.id) } }
        return all.firstOrNull { it.kind == kind && states[it.id] == PuzzleState.NEW && extra(it) }
            ?: throw AssertionError("kit: precondition ($what): no New $kind puzzle meets the fixture; the device is reset (tools/device_reset.sh) before every device step")
    }

    /** S-2 `open(id)` on the main thread, then `play-screen` shows [id]. */
    fun openByController(id: String, what: String) {
        onController { check(this.open(id)) { "kit: open($id) refused ($what)" } }
        await("$what: `play-screen` shows $id") { nodes(PlayTags.SCREEN).size == 1 && shownId() == id }
        waitIdle()
    }

    /**
     * Places [target] through S-2's actions (a set-up, never the gesture under test): the flip while the parallelogram's tray
     * mirror differs, `startDrag` at the pose's centre, the shorter twist, `moveDrag` over the board, `drop`. The drop must
     * return [target] (the engine's own lock, decisions.md 2026-10-17 "A solution pose is read from the puzzle").
     */
    fun placeByController(target: Placement, what: String) {
        val c = controller()
        val result = onMain {
            check(c.boardDpPerUnit > 0.0) { "kit: no board scale yet ($what)" }
            if (target.piece == PieceId.PG && c.trayMirrored(PieceId.PG) != target.mirrored) check(c.pressBadge()) { "kit: pressBadge() refused ($what)" }
            check(c.startDrag(target.piece, target.centre)) { "kit: startDrag(${target.piece}) refused ($what)" }
            val from = c.dragged?.turn?.step ?: error("kit: no drag after startDrag(${target.piece}) ($what)")
            val steps = ((target.turn.step - from) % 8 + 8) % 8
            if (steps != 0) c.twistDrag(if (steps <= 4) steps else steps - 8)
            c.moveDrag(target.centre, overBoard = true)
            c.updatePreview()
            c.drop()
        }
        if (!kitAppSame(result, target)) throw AssertionError("kit: the set-up drop of ${target.piece} ($what) locked at $result, planned $target")
        idleIfLive()
    }

    /** Solves the open puzzle through S-2 from its board, in an order the engine allows (S-27's search). */
    fun solveByController(what: String) {
        val c = controller()
        val (p, on) = onMain { c.puzzle to c.placed.toList() }
        val order = kitAppLockOrder(p, on) ?: throw AssertionError("kit: no order solves ${p.id} from its board $on ($what)")
        order.forEach { placeByController(it, what) }
        val state = onMain { c.state }
        if (state != PuzzleState.SOLVED) throw AssertionError("kit: ${p.id} is $state after every piece was placed ($what)")
    }

    /**
     * A New full puzzle and three of its pieces at their solution poses, in an order the engine locks them in: the first one
     * whose parallelogram is mirrored in its solution and comes among the first three when tried first (so a lost mirror
     * shows), else the first New full puzzle's first three (ProcessDeathKitTest's fixture, rebuilt here).
     */
    fun threeToLeave(what: String): Pair<PuzzleDefinition, List<Placement>> {
        val all = library.puzzles
        val states = onController { all.associate { it.id to stateOf(it.id) } }
        val fulls = all.filter { it.kind == PuzzleKind.FULL && states[it.id] == PuzzleState.NEW }
        for (p in fulls) {
            if (PieceId.PG !in p.pieces || !kitAppPose(p, PieceId.PG).mirrored) continue
            val order = kitAppLockOrder(p, prefer = listOf(PieceId.PG) + p.solution.map { it.piece }.filter { it != PieceId.PG }) ?: continue
            if (order.indexOfFirst { it.piece == PieceId.PG } in 0..2) return p to order.take(3)
        }
        val p = fulls.firstOrNull() ?: throw AssertionError("kit: precondition ($what): no New full puzzle; the device is reset before every device step")
        val order = kitAppLockOrder(p) ?: throw AssertionError("kit: no lock order for ${p.id} ($what)")
        return p to order.take(3)
    }

    /** The solved row is shown (S-22: from the frame the picture is fully shown; at once for a puzzle opened solved). */
    fun awaitSolvedRow(what: String) {
        await("$what: the solved row (`solved-row`, `retry`, `solved-next`)") {
            exists(KitAppTag.SOLVED_ROW) && exists(KitAppTag.RETRY) && exists(KitAppTag.SOLVED_NEXT)
        }
        waitIdle()
    }

    // ── Touch on `play-screen` (the play kit's pattern, copied: another module) ──

    fun touch(block: TouchInjectionScope.() -> Unit) {
        bounded("a touch on `play-screen`") { rule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).performTouchInput(block) }
    }

    /** A still tap at [at] (`play-screen` px): down and up in one injection, so no hold timer runs (S-22: a tap acts at the lift). */
    fun tap(at: Offset, what: String) {
        val screen = screenBounds()
        if (!screen.contains(at)) throw AssertionError("kit: the tap for $what at $at lies outside `play-screen` $screen")
        touch {
            down(at)
            up()
        }
        waitIdle()
    }

    /** A still tap at the centre of [tag]'s bounds (S-13Δ: "A control's bounds are its touch area"). */
    fun pressCentre(tag: String) = tap(bounds(tag).center, "the centre of `$tag`")

    /** A board or drag piece's drawn corners in `play-screen` px, from its keys through the engine (O-01). */
    fun boardPolygonPx(tag: String): List<Offset> {
        val n = node(tag)
        val piece = n.config.getOrNull(PlaySemantics.PieceKey) ?: throw AssertionError("kit: `$tag` carries no PieceKey")
        val turn = n.config.getOrNull(PlaySemantics.TurnKey) ?: throw AssertionError("kit: `$tag` carries no TurnKey")
        val mirrored = n.config.getOrNull(PlaySemantics.MirroredKey) ?: throw AssertionError("kit: `$tag` carries no MirroredKey")
        val centre = n.config.getOrNull(PlaySemantics.CentreKey) ?: throw AssertionError("kit: `$tag` carries no CentreKey")
        val m = mapping()
        return kitAppEngine.vertices(piece, turn, mirrored, centre).map { m.toPx(it) }
    }

    /** A tray miniature's drawn shape in `play-screen` px: the shape at its keys, fitted into its node's box (tag table: the drawn shape's box). */
    fun trayPolygonPx(piece: PieceId): List<Offset> {
        val tag = PlayTags.trayPiece(piece)
        val turn = key(tag, PlaySemantics.TurnKey)
        val mirrored = key(tag, PlaySemantics.MirroredKey)
        val scale = key(tag, PlaySemantics.DrawnPxPerUnitKey)
        val box = bounds(tag)
        val v = kitAppEngine.vertices(piece, turn, mirrored, Point(0.0, 0.0))
        val minX = v.minOf { it.x }
        val minY = v.minOf { it.y }
        return v.map { Offset(box.left + ((it.x - minX) * scale).toFloat(), box.top + ((it.y - minY) * scale).toFloat()) }
    }

    /**
     * A point of [piece]'s tray cell outside the tray badge's area (REQ-015: the whole cell is the piece's touch area; inside
     * the parallelogram's cell the badge's area takes what no drawn piece covers, decisions.md 2026-10-12).
     */
    fun trayPoint(piece: PieceId): Offset {
        val cell = bounds(PlayTags.trayCell(piece))
        val avoid = if (piece == PieceId.PG && exists(PlayTags.FLIP_BADGE_TRAY)) bounds(PlayTags.FLIP_BADGE_TRAY) else null
        val candidates = listOf(
            cell.center,
            Offset(cell.left + cell.width * 0.25f, cell.top + cell.height * 0.75f),
            Offset(cell.left + cell.width * 0.75f, cell.top + cell.height * 0.75f),
            Offset(cell.left + cell.width * 0.25f, cell.top + cell.height * 0.25f),
        )
        return candidates.firstOrNull { avoid == null || !avoid.contains(it) }
            ?: throw AssertionError("kit: the badge area $avoid covers every tap point of the PG cell $cell")
    }

    /** [times] taps on [piece] in the tray (REQ-016: one tap turns one 45° step clockwise). */
    fun tapTrayPiece(piece: PieceId, times: Int) {
        repeat(times) { tap(trayPoint(piece), "a tap on $piece in the tray") }
    }

    /**
     * The point of the tray badge's area farthest from the parallelogram's drawn shape, off that shape: there the badge acts
     * (decisions.md 2026-10-12: the disc → the piece's drawn shape → the rest of the badge's area).
     */
    fun trayBadgePoint(): Offset {
        val area = bounds(PlayTags.FLIP_BADGE_TRAY)
        val shape = if (exists(PlayTags.trayPiece(PieceId.PG))) trayPolygonPx(PieceId.PG) else emptyList()
        var best: Offset? = null
        var bestClear = -1f
        for (i in 1..11) {
            for (j in 1..11) {
                val q = Offset(area.left + area.width * i / 12f, area.top + area.height * j / 12f)
                if (shape.isNotEmpty() && kitAppInside(q, shape)) continue
                val clear = if (shape.isEmpty()) Float.MAX_VALUE else kitAppDistToBoundary(q, shape)
                if (clear > bestClear) {
                    best = q
                    bestClear = clear
                }
            }
        }
        return best ?: throw AssertionError("kit: the parallelogram's drawn shape covers the whole tray badge area $area")
    }

    /**
     * Picks [piece] up from its tray cell by touch: down in the cell, then a 24 dp move (REQ-015: a touch that moves 12 dp or
     * more is a drag). The finger stays down; returns its position.
     */
    fun pickUpFromTray(piece: PieceId): Offset {
        val start = trayPoint(piece)
        val lifted = Offset(start.x, start.y - dp(24f))
        beginHeldFinger()
        touch { down(start) }
        heldStep()
        touch { moveTo(lifted) }
        heldStep()
        advance(150) // REQ-014: the tray piece grows to board size within 120 ms of the drag starting
        awaitFrames("`${PlayTags.dragPiece(piece)}` after a 24 dp move from its cell") { exists(PlayTags.dragPiece(piece)) }
        return lifted
    }

    private var fingerHeld = false

    /**
     * A held finger keeps the screen working every frame (the drag follows the finger, the preview updates per frame), so
     * Compose never idles under an auto-advancing clock (measured at landing, 2026-10-17 18:56). While a finger is held the
     * clock is paused and every step advances one frame by hand (the play kit's `KitScreen` pattern). The restore lifts a
     * finger a failed test left down and turns `autoAdvance` back on.
     */
    private fun beginHeldFinger() {
        onRestore("a held finger") {
            if (fingerHeld && composeReady("lifting a held finger")) runCatching { touch { up() } }
            fingerHeld = false
            rule.mainClock.autoAdvance = true
        }
        rule.mainClock.autoAdvance = false
        fingerHeld = true
    }

    /** One frame while a finger is held; a plain idle otherwise. */
    private fun heldStep() {
        if (fingerHeld) rule.mainClock.advanceTimeByFrame()
        waitIdle()
    }

    /** Advances one frame at a time (at most [maxFrames]) until [condition] holds, failing loudly otherwise: a wait under the paused clock. */
    fun awaitFrames(what: String, maxFrames: Int = 60, condition: () -> Boolean) {
        var last: Throwable? = null
        for (i in 0..maxFrames) {
            val ok = try {
                condition()
            } catch (s: KitComposeStall) {
                throw s
            } catch (t: Throwable) {
                last = t
                false
            }
            if (ok) return
            rule.mainClock.advanceTimeByFrame()
            waitIdle()
        }
        throw AssertionError("kit: $what did not hold within $maxFrames frames" + (last?.let { " (last error: $it)" } ?: ""))
    }

    /** Moves the held finger from [from] to [to] in [steps] moves, the frames composed after each. Returns [to]. */
    fun moveFinger(from: Offset, to: Offset, steps: Int = 6): Offset {
        val screen = screenBounds()
        if (!screen.contains(to)) throw AssertionError("kit: a finger move to $to leaves `play-screen` $screen")
        for (i in 1..steps) {
            val f = Offset(from.x + (to.x - from.x) * i / steps, from.y + (to.y - from.y) * i / steps)
            touch { moveTo(f) }
            heldStep()
        }
        return to
    }

    /**
     * A second finger going round the first (held at [first]) from 0° to [toDeg] in 5° moves, 80 dp out, then lifted (REQ-017:
     * clockwise on screen is positive; the play kit's twist).
     */
    fun twist(first: Offset, toDeg: Double) {
        val r = dp(80f)
        val screen = screenBounds()
        val start = kitAppOnCircle(first, r, 0.0)
        if (!screen.contains(start)) throw AssertionError("kit: the twist's second finger at $start leaves `play-screen` $screen")
        touch { down(1, start) }
        heldStep()
        val steps = max(1, (abs(toDeg) / 5.0).toInt())
        for (i in 1..steps) {
            val q = kitAppOnCircle(first, r, toDeg * i / steps)
            if (!screen.contains(q)) throw AssertionError("kit: the twist's second finger at $q leaves `play-screen` $screen")
            touch { moveTo(1, q) }
            heldStep()
        }
        touch { up(1) }
        heldStep()
    }

    /**
     * Steers the dragged [piece] so that its drawn centre lands on [target] (puzzle units): the finger moves by the gap between
     * the piece's `CentreKey` and the target (REQ-014: the piece follows the finger at a constant float). Returns the finger.
     */
    fun steerDrag(piece: PieceId, target: Point, finger: Offset): Offset {
        val tag = PlayTags.dragPiece(piece)
        var f = finger
        repeat(5) {
            val gap = toPx(target) - toPx(key(tag, PlaySemantics.CentreKey))
            if (hypot(gap.x, gap.y) < 0.75f) return f
            f = moveFinger(f, f + gap, 4)
        }
        val off = toPx(key(tag, PlaySemantics.CentreKey)) - toPx(target)
        if (hypot(off.x, off.y) > 1.5f) throw AssertionError("kit: could not steer $piece onto $target (off by $off px)")
        return f
    }

    /** Lifts the first finger (a release: S-2 `drop()`), then composes. */
    fun release() {
        touch { up() }
        heldStep()
        fingerHeld = false
        rule.mainClock.autoAdvance = true
        waitIdle()
    }

    // ── Texts (the closed world; the language legs) ──

    /** Every text of every compose root, unmerged: each `Text` entry of each node. */
    fun shownTexts(): List<String> =
        allNodes().flatMap { n -> n.config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList() }

    /** The target context resolved for [language] only (`createConfigurationContext`, the TASK-030 brief). */
    fun contextFor(language: String): Context {
        val conf = Configuration(target.resources.configuration)
        conf.setLocales(LocaleList(Locale.forLanguageTag(language)))
        return target.createConfigurationContext(conf)
    }

    /** `:app`'s own string resources, by name: its R class lists exactly them (TASK-030 Inputs, the two `:app` string files). */
    fun appStrings(): Map<String, Int> = kitAppResourceIds(R.string::class.java)

    /** The #DevTools strings, by name: `:play`'s debug R, every name `dev_aid_` (S-29; TASK-030 Inputs, the two aid string files). */
    fun aidStrings(): Map<String, Int> {
        val ids = kitAppResourceIds(Class.forName("app.tangramplaytime.play.R\$string")).filterKeys { it.startsWith("dev_aid_") }
        if (ids.isEmpty()) throw AssertionError("kit: the debug app holds no dev_aid_ string (S-29)")
        return ids
    }

    /**
     * A resource text that is a format pattern (S-11Δ `counterFormat`): never shown raw, only through its outputs
     * ([counterTexts]), so its raw text is no text of the closed world.
     */
    private fun isFormatPattern(text: String): Boolean = Regex("%(\\d+\\$)?[sd]").containsMatchIn(text)

    /** The counter's "n / N" for every puzzle of this library, as [ctx] formats it (S-11Δ `counterFormat`). */
    fun counterTexts(ctx: Context): Set<String> {
        val n = library.puzzles.size
        return (1..n).map { ctx.getString(R.string.counter_format, it, n) }.toSet()
    }

    /**
     * The play-through's closed world of texts (the TASK-030 brief: "the `values/` strings of `:app`, the `dev_aid_*` strings,
     * the bundle's titles, the counter format's output for this library, the best time's '–'", the last one being `values/`'s
     * own; a format pattern counts by its outputs only), plus the DEV overlay's labels, `PieceId` names (S-29: "data, as a
     * puzzle title is").
     */
    fun closedWorld(): Set<String> {
        val en = contextFor("en")
        val out = LinkedHashSet<String>()
        (appStrings().values + aidStrings().values).map { en.getString(it) }.filterNot { isFormatPattern(it) }.forEach { out += it }
        out += counterTexts(en)
        library.puzzles.forEach { out += it.titles.values }
        PieceId.entries.forEach { out += it.name }
        return out
    }

    /** Every text the screens may show under S-36's rule for [language] ("fi" or "en"): its resources, its counter, its titles. */
    fun languageWorld(language: String): Set<String> {
        val ctx = contextFor(language)
        val out = LinkedHashSet<String>()
        (appStrings().values + aidStrings().values).map { ctx.getString(it) }.filterNot { isFormatPattern(it) }.forEach { out += it }
        out += counterTexts(ctx)
        library.puzzles.forEach { out += kitAppTitle(it, language) }
        PieceId.entries.forEach { out += it.name }
        return out
    }

    /** The resource texts of [language] whose resolution in the other language differs: the comparisons that discriminate. */
    fun languageOnly(language: String): Set<String> {
        val mine = contextFor(language)
        val theirs = contextFor(if (language == "fi") "en" else "fi")
        return (appStrings().values + aidStrings().values).map { mine.getString(it) to theirs.getString(it) }
            .filter { it.first != it.second }.map { it.first }.toSet()
    }

    // ── The promise's window checks (decisions.md 2026-10-17 Q8 row, part (3)) ──

    /**
     * At one step of the play-through: every text shown is in [allowed] (none of which holds a currency symbol, checked once by
     * the caller); the decor view holds no `WebView` and only platform, AndroidX or app views (ruling 4); `MainActivity` is the
     * only resumed activity, its window has focus, and the active window is the app's.
     */
    fun assertPromise(what: String, allowed: Set<String>) {
        val stray = shownTexts().filter { it !in allowed }.distinct()
        if (stray.isNotEmpty()) throw AssertionError("$what: texts shown that are none of the app's own texts (REQ-001 A1, REQ-008 A1): $stray")
        val a = activity(what)
        val foreign = onMain {
            val bad = ArrayList<String>()
            fun walk(v: View) {
                if (v is WebView) bad += "a WebView (${v.javaClass.name})" else if (!kitAppIsPlatformAndroidXOrOwn(v.javaClass)) bad += v.javaClass.name
                if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
            walk(a.window.decorView)
            bad
        }
        if (foreign.isNotEmpty()) throw AssertionError("$what: the decor view holds a view that is no platform, AndroidX or app view, a possible ad or web surface (REQ-008 A1): $foreign")
        awaitNoProblems("$what: the windows (REQ-008 A2: no rating prompt; REQ-001 A1: no ad)", 10_000) {
            val (resumed, focus) = onMain {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).toList() to a.hasWindowFocus()
            }
            val problems = ArrayList<String>()
            if (resumed.size != 1 || resumed.single() !== a) problems += "resumed activities ${resumed.map { it.javaClass.name }}"
            if (!focus) problems += "MainActivity's window has no focus"
            val pkg = automation.rootInActiveWindow?.packageName?.toString()
            if (pkg != KIT_APP_PACKAGE) problems += "the active window belongs to $pkg"
            problems.joinToString("; ")
        }
    }

    // ── Paused time (the cue pins) ──

    /** Pauses the compose clock (the frames are then advanced by hand); the restore turns `autoAdvance` back on. */
    fun pauseClock() {
        rule.mainClock.autoAdvance = false
        onRestore("the paused clock") { rule.mainClock.autoAdvance = true }
    }

    fun frames(n: Int) {
        repeat(n) { rule.mainClock.advanceTimeByFrame() }
        waitIdle()
    }

    /** Advances the paused compose clock by [ms]: time is injected, never slept (G-14). */
    fun advance(ms: Long) {
        rule.mainClock.advanceTimeBy(ms)
        waitIdle()
    }
}
