// measurement (WO-010 question 6): no token; it asserts only liveness
// WO-010 TASK-035 · seam S-43 (tasks.md § WO-010 › Seams; question 6; decisions.md 2026-10-19 "The start-up and grid
// thresholds, and the designs if crossed"): the scale instrument. It measures the start-up and the grid overview over the
// real wiring and the bundle `:content` reads, and prints one `measure:` line per test; it never judges a number. The
// thresholds are the orchestrator's, applied to the medians the channel line collects.
// - The channel runs this class alone, one class per invocation, each a fresh instrumentation process with the app cleared
//   by `tools/device_reset.sh` around it (the scale record row of `workorders/WO-010-briefs.md`). The tests run in the
//   order of their names (the prefix `mN_`): the first claims the fresh start, so `AppGraph.start` (`MainActivity.onCreate`)
//   has not run before its launch. Inside a whole app set (a shared process) it may have: that line is marked warm, not a
//   record, and the test still passes.
// - It stands alone: no helper of `KitApp.kt`. Time is `SystemClock.elapsedRealtime` in this test code only (G-14). Every
//   wait is on an observed state, bounded at 60 s; nothing sleeps.
// - It leaves the controller as it found it: the grid closed (by Done), the open puzzle and its state unchanged.
package app.tangramplaytime.app

import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.runners.model.Statement
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ScaleMeasureTest {

    private val mark = LaunchMark()

    /** Outside the compose rule: it marks the moment just before that rule launches `MainActivity`. */
    @get:Rule(order = 0)
    val launchMark: TestRule = mark

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * `launchToFirstPuzzle`: from the launch of `MainActivity` (the moment before the compose rule starts it; in the channel's
     * fresh process `AppGraph` has no controller yet, and a line from a process where it had one is marked warm) to the first
     * frame in which `play-screen` (S-13) exists, as the first poll after that frame sees it (Compose test idling, about 10 ms
     * steps). The span holds the activity's start, `AppGraph.start`'s
     * load of the bundle and the store on IO (`decisions.md` 2026-10-15, the launch-window row: the loading paper until then)
     * and the first composition of the Play screen. Bounded at 60 s.
     */
    @Test
    fun m1_launchToFirstPuzzle() {
        val seenAt = awaitAt("`play-screen` after the launch") { exists(PlayTags.SCREEN) }
        val span = seenAt - mark.at
        assertTrue("the controller exists once `play-screen` is shown", AppGraph.controller.value != null)
        val puzzles = PuzzleLibraries.bundled(LockEngine()).puzzles.size // after the span: the kit's own parse is not measured
        // Inside a whole app set the process is shared, and another class may have built the controller already: the line
        // says so and the test still passes (liveness only; only the channel's class-alone runs are a record).
        val value = if (mark.controllerBefore) "$span ms, warm process (the controller already existed: not a record)" else "$span ms"
        report("launchToFirstPuzzle", value, puzzles)
    }

    /**
     * `gridOpen`: from a tap on `counter` (the moment before the touch is injected at its centre) to the first frame in which
     * the overview and an `overview-cell-<id>` for every listed puzzle exist (all composed: `decisions.md` 2026-10-15 "The
     * grid overview, as drawn"), as the first poll after that frame sees it. Bounded at 60 s. Then Done closes the grid.
     */
    @Test
    fun m2_gridOpen() {
        awaitAt("`play-screen`") { exists(PlayTags.SCREEN) }
        val before = shown()
        assertFalse("precondition: the grid is closed", exists(PlayTags.OVERVIEW))
        val ids = PuzzleLibraries.bundled(LockEngine()).puzzles.map { it.id }
        composeRule.waitForIdle()
        val counter = composeRule.onNodeWithTag(PlayTags.COUNTER, useUnmergedTree = true)
        val tappedAt = SystemClock.elapsedRealtime()
        counter.performClick()
        val composedAt = awaitGrid(ids)
        report("gridOpen", "${composedAt - tappedAt} ms", ids.size)
        closeGrid(before)
    }

    /**
     * `gridScroll`: with the grid open and settled, `dumpsys gfxinfo app.tangramplaytime reset`; then the fixed swipe series
     * until the last puzzle's cell is wholly in view; then `dumpsys gfxinfo app.tangramplaytime`: the frames rendered, the
     * janky frames and their share, the 90th and the 99th percentile of the frame time. Then Done closes the grid.
     *
     * **The swipe series** (fixed, so the measure compares across images and bundles): each swipe puts one finger down at the
     * horizontal centre of the cell block, [SWIPE_START_ABOVE_BOTTOM_DP] dp above the bottom of the grid's visible cells as the
     * grid shows them at its open; moves it [SWIPE_DISTANCE_DP] dp straight up in [SWIPE_STEPS] equal steps, [SWIPE_STEP_MS] ms
     * apart; holds it still [SWIPE_HOLD_MS] ms, so the lift ends the swipe without a fling (the drag alone scrolls); lifts it.
     * The series repeats that swipe until the last cell is wholly in view (none when it already is at the open), bounded at
     * 60 s; a swipe that moves no cell is a stall. Only the count of swipes depends on the bundle and the screen; it is
     * printed with the frames. With no frame rendered (no swipe needed), the line prints `-` for the share and the percentiles.
     */
    @Test
    fun m3_gridScroll() {
        awaitAt("`play-screen`") { exists(PlayTags.SCREEN) }
        val before = shown()
        assertFalse("precondition: the grid is closed", exists(PlayTags.OVERVIEW))
        val ids = PuzzleLibraries.bundled(LockEngine()).puzzles.map { it.id }
        val lastCell = PlayTags.overviewCell(ids.last())
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(PlayTags.COUNTER, useUnmergedTree = true).performClick()
        awaitGrid(ids)
        composeRule.waitForIdle()

        val density = composeRule.activity.resources.displayMetrics.density
        val cells = cellNodes()
        val visible = cells.map { it.boundsInRoot }.filter { !it.isEmpty }
        assertTrue("the grid shows at least one cell at its open", visible.isNotEmpty())
        val blockLeft = cells.minOf { it.positionInRoot.x }
        val blockRight = cells.maxOf { it.positionInRoot.x + it.size.width }
        val origin = node(PlayTags.SCREEN).boundsInRoot.topLeft
        val start = Offset((blockLeft + blockRight) / 2f, visible.maxOf { it.bottom } - SWIPE_START_ABOVE_BOTTOM_DP * density) - origin
        val distancePx = SWIPE_DISTANCE_DP * density

        shell("dumpsys gfxinfo $APP_PACKAGE reset")
        var swipes = 0
        val seriesStart = SystemClock.elapsedRealtime()
        while (!whollyInView(node(lastCell))) {
            if (SystemClock.elapsedRealtime() - seriesStart > BOUND_MS) {
                throw AssertionError("stalled: `$lastCell` is not wholly in view after $swipes swipes and $BOUND_MS ms")
            }
            val wasAt = node(lastCell).positionInRoot.y
            swipeUp(start, distancePx)
            swipes++
            composeRule.waitForIdle()
            val nowAt = node(lastCell).positionInRoot.y
            if (abs(nowAt - wasAt) < 1f && !whollyInView(node(lastCell))) {
                throw AssertionError("stalled: swipe $swipes moved no cell (`$lastCell` stays at y $nowAt px) and it is not wholly in view")
            }
        }
        composeRule.waitForIdle()
        val out = shell("dumpsys gfxinfo $APP_PACKAGE")
        val stats = frameStats(out) ?: throw AssertionError("`dumpsys gfxinfo $APP_PACKAGE` printed no frame statistics:\n$out")
        // With no frame rendered, gfxinfo's share is NaN and its percentiles are its empty histogram's top bucket, not a time.
        val frames = if (stats.frames == 0) "frames 0, janky -, p90 -, p99 -"
        else "frames ${stats.frames}, janky ${stats.janky} (${stats.jankyShare}%), p90 ${stats.p90} ms, p99 ${stats.p99} ms"
        report("gridScroll", "$frames, swipes $swipes", ids.size)
        closeGrid(before)
    }

    // ── Reads and waits ──────────────────────────────────────────────────────────────────────────────

    /** Waits (bounded) until [condition] holds and returns the time of the poll that first saw it. */
    private fun awaitAt(what: String, condition: () -> Boolean): Long {
        var at = 0L
        try {
            composeRule.waitUntil(timeoutMillis = BOUND_MS) {
                val seen = condition()
                if (seen) at = SystemClock.elapsedRealtime()
                seen
            }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("stalled: $what not seen within $BOUND_MS ms", e)
        }
        return at
    }

    /** The first frame with the overview and a cell for every one of [ids] (all composed). */
    private fun awaitGrid(ids: List<String>): Long {
        val wanted = ids.map { PlayTags.overviewCell(it) }.toSet()
        return awaitAt("the grid overview with a cell for each of the ${ids.size} puzzles") {
            exists(PlayTags.OVERVIEW) && cellNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.toSet().containsAll(wanted)
        }
    }

    /** Done closes the grid; the open puzzle and its state are the ones before it opened. */
    private fun closeGrid(before: Pair<String?, PuzzleState?>) {
        composeRule.onNodeWithTag(PlayTags.OVERVIEW_DONE, useUnmergedTree = true).performClick()
        awaitAt("the grid closed by Done") { !exists(PlayTags.OVERVIEW) }
        assertEquals("the open puzzle and its state are as the class found them", before, shown())
    }

    private fun shown(): Pair<String?, PuzzleState?> {
        val screen = node(PlayTags.SCREEN)
        return screen.config.getOrNull(PlaySemantics.PuzzleIdKey) to screen.config.getOrNull(PlaySemantics.PuzzleStateKey)
    }

    private fun exists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun node(tag: String): SemanticsNode =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode("`$tag`")

    private fun cellNodes(): List<SemanticsNode> =
        composeRule.onAllNodes(isOverviewCell, useUnmergedTree = true).fetchSemanticsNodes()

    /** The node's bounds clipped by the grid equal its whole bounds (within 1 px). */
    private fun whollyInView(n: SemanticsNode): Boolean {
        val shown = n.boundsInRoot
        val whole = Rect(n.positionInRoot, n.size.toSize())
        return abs(shown.left - whole.left) <= 1f && abs(shown.top - whole.top) <= 1f &&
            abs(shown.right - whole.right) <= 1f && abs(shown.bottom - whole.bottom) <= 1f
    }

    /** One swipe of the series, injected at `play-screen` ([start] in its coordinates). */
    private fun swipeUp(start: Offset, distancePx: Float) {
        composeRule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).performTouchInput {
            down(start)
            for (i in 1..SWIPE_STEPS) moveTo(Offset(start.x, start.y - distancePx * i / SWIPE_STEPS), delayMillis = SWIPE_STEP_MS)
            moveTo(Offset(start.x, start.y - distancePx), delayMillis = SWIPE_HOLD_MS)
            up()
        }
    }

    // ── Output ───────────────────────────────────────────────────────────────────────────────────────

    /** One `measure:` line (S-43's form) to logcat (tag [LOG_TAG]) and to the instrumentation status. */
    private fun report(name: String, value: String, puzzles: Int) {
        val line = "measure: $name $value ($puzzles puzzles, ${deviceLabel()})"
        Log.i(LOG_TAG, line)
        val status = Bundle().apply { putString(Instrumentation.REPORT_KEY_STREAMRESULT, "\n$line\n") }
        InstrumentationRegistry.getInstrumentation().sendStatus(STATUS_IN_PROGRESS, status)
    }

    /**
     * The AVD's name (the emulator's property; the model elsewhere), the API level and the display in dp: the `wm` override
     * when one is set (the line then ends in `override`), else the physical display.
     */
    private fun deviceLabel(): String {
        val avd = AVD_NAME_PROPERTIES.map { shell("getprop $it").trim() }.firstOrNull { it.isNotEmpty() } ?: Build.MODEL
        val sizes = shell("wm size")
        val densities = shell("wm density")
        val overridden = sizes.contains("Override size:") || densities.contains("Override density:")
        val size = Regex("""Override size: (\d+)x(\d+)""").find(sizes) ?: Regex("""Physical size: (\d+)x(\d+)""").find(sizes)
        val dpi = Regex("""Override density: (\d+)""").find(densities) ?: Regex("""Physical density: (\d+)""").find(densities)
        val dp = if (size != null && dpi != null) {
            val d = dpi.groupValues[1].toInt()
            " ${size.groupValues[1].toInt() * 160 / d}x${size.groupValues[2].toInt() * 160 / d}dp" + if (overridden) " override" else ""
        } else {
            ""
        }
        return "$avd API ${Build.VERSION.SDK_INT}$dp"
    }

    private fun shell(command: String): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private data class FrameStats(val frames: Int, val janky: Int, val jankyShare: String, val p90: Int, val p99: Int)

    /** The process-wide lines of `dumpsys gfxinfo` (the first of each; the GPU and legacy lines are other lines). */
    private fun frameStats(out: String): FrameStats? {
        val lines = out.lineSequence().map { it.trim() }.toList()
        fun first(re: Regex): MatchResult? = lines.firstNotNullOfOrNull { re.matchEntire(it) }
        val total = first(Regex("""Total frames rendered: (\d+)""")) ?: return null
        val janky = first(Regex("""Janky frames: (\d+) \(([^)]*)%\)""")) ?: return null
        val p90 = first(Regex("""90th percentile: (\d+)ms""")) ?: return null
        val p99 = first(Regex("""99th percentile: (\d+)ms""")) ?: return null
        return FrameStats(
            total.groupValues[1].toInt(), janky.groupValues[1].toInt(), janky.groupValues[2],
            p90.groupValues[1].toInt(), p99.groupValues[1].toInt(),
        )
    }

    /** Records the moment before the inner rules start, and whether `AppGraph` had a controller then. */
    private class LaunchMark : TestRule {
        var at: Long = 0L
            private set
        var controllerBefore: Boolean = false
            private set

        override fun apply(base: Statement, description: Description): Statement = object : Statement() {
            override fun evaluate() {
                controllerBefore = AppGraph.controller.value != null
                at = SystemClock.elapsedRealtime()
                base.evaluate()
            }
        }
    }

    private companion object {
        const val APP_PACKAGE = "app.tangramplaytime"
        const val LOG_TAG = "ScaleMeasure"
        const val BOUND_MS = 60_000L

        /** `sendStatus` code for an in-progress report (the code the connected runner leaves to tools; not a test result). */
        const val STATUS_IN_PROGRESS = 2

        /** The swipe series (see `m3_gridScroll`). */
        const val SWIPE_DISTANCE_DP = 240f
        const val SWIPE_STEPS = 16
        const val SWIPE_STEP_MS = 16L
        const val SWIPE_HOLD_MS = 200L
        const val SWIPE_START_ABOVE_BOTTOM_DP = 32f

        /** The emulator's AVD name: `ro.boot.qemu.avd_name` on newer images, `ro.kernel.qemu.avd_name` on older ones. */
        val AVD_NAME_PROPERTIES = listOf("ro.boot.qemu.avd_name", "ro.kernel.qemu.avd_name")

        val isOverviewCell = SemanticsMatcher("an `${PlayTags.overviewCell("")}…` cell") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(PlayTags.overviewCell("")) == true
        }
    }
}
