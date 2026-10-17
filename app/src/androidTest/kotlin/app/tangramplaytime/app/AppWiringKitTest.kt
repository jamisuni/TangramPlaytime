// Acceptance kit (WO-004, TASK-019): untokened pins of the `:app` wiring (designs/WO-004-design.md §2 S-15Δ, S-18;
// tasks.md § WO-004 seam note 2). No token lives in this file. The app's controller is one per process and shared by
// every test of the instrumentation run, in any order: these tests leave it as they found it (the grid closed, no
// puzzle changed). The screen appears once `AppGraph`'s IO load has built the controller, which Compose idling cannot
// see (design §3): each test waits for `play-screen` with `waitUntil`.
package app.tangramplaytime.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.tangramplaytime.contracts.IProgressStore
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.store.ProgressStores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AppWiringKitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test // pin: S-18 "onDevice(context): IProgressStore returns one memoised instance per file. Two calls give the same object"; tasks.md § WO-004 seam note 2 (the pin runs on the device); decisions.md 2026-10-15 "Amends the 2026-10-15 ADR-003 row" ("ProgressStores.onDevice returns one process-wide instance per file (memoised, pinned by a test)"); design §3 "This rests on one store instance per file (S-18)"; no token
    fun onDeviceGivesOneStoreInstanceForTheFileWhateverTheContextOrTheThread() {
        // Oracle: the app's own process (the launched activity's): two calls with the target context, a call with the
        // application context, one with the activity, and calls from eight other threads released together all return the
        // very same object.
        kitWaitForScreen()
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val first = ProgressStores.onDevice(target)
        assertSame("a second call with the same context", first, ProgressStores.onDevice(target))
        assertSame("a call with the application context", first, ProgressStores.onDevice(target.applicationContext))
        var fromActivity: IProgressStore? = null
        composeRule.activityRule.scenario.onActivity { fromActivity = ProgressStores.onDevice(it) }
        assertSame("a call with the activity", first, fromActivity)
        val threads = 8
        val go = CountDownLatch(1)
        val done = CountDownLatch(threads)
        val results = arrayOfNulls<IProgressStore>(threads)
        repeat(threads) { i ->
            Thread {
                go.await()
                results[i] = ProgressStores.onDevice(target)
                done.countDown()
            }.start()
        }
        go.countDown()
        assertTrue("the eight threads returned within 10 s", done.await(10, TimeUnit.SECONDS))
        for ((i, r) in results.withIndex()) assertSame("a call from thread $i", first, r)
    }

    @Test // pin: S-15Δ "BackHandler(enabled = overviewOpen)"; REQ-050 rule "The grid is an overlay over the current puzzle; closing it (or the back button) returns to the same puzzle state"; decisions.md 2026-10-15 "The grid overview, as drawn" ("It closes by Done, a tap on the scrim outside the sheet, or back"); no token
    fun backClosesTheGridAndKeepsThePuzzleAndItsState() {
        // Oracle: in the real app, a tap at the centre of the counter zone opens `overview` (S-22); the system back closes
        // it — the activity stays resumed, the same puzzle is shown in the same TYPE-006 state. The grid is closed through
        // `AppGraph.controller` (S-15Δ: the kit's one door) if the test fails half-way, so the next test finds it closed.
        kitWaitForScreen()
        val id = kitScreenKey(PlaySemantics.PuzzleIdKey)
        val state = kitScreenKey(PlaySemantics.PuzzleStateKey)
        try {
            val screen = composeRule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).fetchSemanticsNode("the Play screen").boundsInRoot
            val counter = composeRule.onNodeWithTag(KIT_COUNTER, useUnmergedTree = true).fetchSemanticsNode("the counter zone").boundsInRoot
            val at = Offset(counter.center.x - screen.left, counter.center.y - screen.top)
            composeRule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).performTouchInput { down(at); up() }
            composeRule.waitForIdle()
            assertTrue("a tap on the counter opens the grid", kitExists(KIT_OVERVIEW))
            Espresso.pressBack()
            composeRule.waitForIdle()
            assertFalse("back closes the grid", kitExists(KIT_OVERVIEW))
            assertEquals("the activity is still resumed after back", Lifecycle.State.RESUMED, composeRule.activityRule.scenario.state)
            assertEquals("the same puzzle after back", id, kitScreenKey(PlaySemantics.PuzzleIdKey))
            assertEquals("the same state after back", state, kitScreenKey(PlaySemantics.PuzzleStateKey))
        } finally {
            composeRule.runOnUiThread { AppGraph.controller.value?.let { if (it.overviewOpen) it.closeOverview() } }
        }
    }

    private fun kitWaitForScreen() {
        composeRule.waitUntil(timeoutMillis = 30_000) { kitExists(PlayTags.SCREEN) }
    }

    private fun kitExists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun <T> kitScreenKey(key: androidx.compose.ui.semantics.SemanticsPropertyKey<T>): T =
        composeRule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).fetchSemanticsNode("the Play screen").config.getOrNull(key)
            ?: throw AssertionError("`play-screen` carries no ${key.name}")

    private companion object {
        /** S-13Δ's tag strings (designs/WO-004-design.md §2; the seam fixes the strings). */
        const val KIT_COUNTER = "counter"
        const val KIT_OVERVIEW = "overview"
    }
}
