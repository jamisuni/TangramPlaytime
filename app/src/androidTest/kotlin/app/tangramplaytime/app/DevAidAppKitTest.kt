// Acceptance kit (WO-005, TASK-025): the DEV aid in the real app — the DEV button and the passcode through the real wiring
// (tasks.md § WO-005 › Seams S-15Δ2 "`PlayScreen(…, devAid = DevAidHook.installed)`", S-25 `installed`, S-28 `DevAidTags`,
// S-29 (the aid's strings, the app's own resources in the debug build), S-30 (the service file); the aid's tag table).
// - Precondition: a cleared store. `tools/device_reset.sh` runs before the instrumentation run (the channel's reset before
//   every device step), so the app opens on a New puzzle (S-1Δ; the precondition AppLaunchKitTest states).
// - The app loads the bundle and the store on IO and composes `PlayScreen` once the controller exists (S-15Δ of WO-004),
//   which Compose idling cannot see (designs/WO-004-design.md §3): each test waits for `play-screen` with `waitUntil`.
// - The app's controller and its aid are each one per process (S-15, S-25), shared by every test of the run in any order.
//   This class leaves the controller as it found it: no puzzle changed, the sheet closed (by Cancel or Done in a `finally`
//   when a test fails half-way). The aid stays unlocked for the rest of the process, as REQ-046 rule 2 says; no other class
//   of the app set reads it.
// - Texts are read only once the device language is English (G-10: English is the Contract wording); the texts asserted
//   are REQ-046's own quotes (S-29: "From REQ-046 (the Contract wording)").
package app.tangramplaytime.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.DevAidHook
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.play.devaid.DevAidTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevAidAppKitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test // REQ-046.A1
    fun inTheRealAppEntering1234IsRefusedAndEntering0417UnlocksTheAid() {
        // REQ-046 A1: "Entering 1234 is refused; entering 0417 unlocks the aid." Statement: "WHERE the build is a prototype or
        // test build, the game SHALL show a DEV button on the board that asks for the passcode 0417"; rule 1: "A wrong passcode
        // shows "Wrong passcode." and reveals nothing"; rule 2: the aid "stays unlocked until the page or app is restarted".
        // The real wiring (the debug app is a test build; G-08): `MainActivity` gives `PlayScreen` the aid `ServiceLoader`
        // finds (S-15Δ2, S-25, S-30). Oracle, on the open puzzle of the real launch (not Solved, by the cleared-store
        // precondition):
        //  - `dev-button` is there and reads "DEV" (the statement; the aid's tag table);
        //  - a tap on it opens the prompt (`dev-aid-prompt`, its field), not the menu;
        //  - 1234, then OK: `dev-aid-message` reads "Wrong passcode."; no menu, no menu pill, no overlay node; the prompt
        //    stays and its field is empty (touch geometry: "A wrong passcode empties it"); the field held the four digits
        //    before OK, so "empty" is read from the right node;
        //  - 0417, then OK: `dev-aid-menu` offering "Show the solution" and "Solve this puzzle now" (the REQ-046 A2 and A3
        //    quotes); no prompt;
        //  - rule 2 through the real wiring: Done; the activity recreated (`ActivityScenario.recreate()`, as a rotation does):
        //    the new `MainActivity` is given the same process-wide aid, so DEV opens the menu at once. The open puzzle and its
        //    state are the same at the end.
        kitWaitForScreen()
        kitAssertEnglish()
        val id = kitScreenKey(PlaySemantics.PuzzleIdKey)
        val state = kitScreenKey(PlaySemantics.PuzzleStateKey)
        assertNotEquals("precondition (a cleared store, tools/device_reset.sh before the run): the open puzzle $id is not Solved", PuzzleState.SOLVED, state)
        assertTrue("`dev-button` on the open puzzle $id of the debug app", kitExists(DevAidTags.DEV_BUTTON))
        assertEquals("the DEV button's text (REQ-046 statement; tag table: its text, \"DEV\")", "DEV", kitText(DevAidTags.DEV_BUTTON))
        try {
            kitPress(DevAidTags.DEV_BUTTON)
            if (kitExists(DevAidTags.MENU)) throw AssertionError("precondition: DEV opened the menu, so the process-wide aid was already unlocked when this test began; this class must run in a process where nothing else unlocks it")
            assertTrue("a tap on DEV opens the prompt (tag table: `dev-aid-prompt` while PROMPT)", kitExists(DevAidTags.PROMPT))
            assertTrue("the prompt's field", kitExists(DevAidTags.PASSCODE))
            assertFalse("no message when the prompt opens", kitExists(DevAidTags.MESSAGE))

            kitType("1234")
            assertEquals("1234 typed: the field holds 4 characters (the fixture reads the right node)", 4, kitFieldText().length)
            kitPress(DevAidTags.OK)
            assertTrue("1234 refused: `dev-aid-message`", kitExists(DevAidTags.MESSAGE))
            assertEquals("1234 refused: the message (REQ-046 rule 1)", "Wrong passcode.", kitText(DevAidTags.MESSAGE))
            for (tag in listOf(DevAidTags.MENU, DevAidTags.SHOW, DevAidTags.SOLVE)) assertFalse("1234 refused: no `$tag` (REQ-046 rule 1: reveals nothing)", kitExists(tag))
            assertTrue("1234 refused: no overlay node (REQ-046 rule 1: reveals nothing)", kitTagsStartingWith(KIT_DEV_SOLUTION_PREFIX).isEmpty())
            assertTrue("1234 refused: the prompt stays", kitExists(DevAidTags.PROMPT))
            assertEquals("1234 refused: the field is empty (touch geometry: \"A wrong passcode empties it\")", "", kitFieldText())

            kitType("0417")
            kitPress(DevAidTags.OK)
            assertTrue("0417 accepted: `dev-aid-menu`", kitExists(DevAidTags.MENU))
            assertFalse("0417 accepted: no prompt", kitExists(DevAidTags.PROMPT))
            assertEquals("0417 accepted: the show pill's label (REQ-046 A2)", "Show the solution", kitText(DevAidTags.SHOW))
            assertEquals("0417 accepted: the solve pill's label (REQ-046 A3)", "Solve this puzzle now", kitText(DevAidTags.SOLVE))
            kitPress(DevAidTags.DONE)
            assertFalse("Done closes the menu", kitExists(DevAidTags.SHEET))

            composeRule.activityRule.scenario.recreate()
            kitWaitForScreen()
            assertFalse("a recreated activity: no sheet (it was closed)", kitExists(DevAidTags.SHEET))
            kitPress(DevAidTags.DEV_BUTTON)
            assertTrue("DEV in a recreated activity opens the menu at once (REQ-046 rule 2: the aid stays unlocked)", kitExists(DevAidTags.MENU))
            assertFalse("DEV in a recreated activity: no prompt", kitExists(DevAidTags.PROMPT))
            kitPress(DevAidTags.DONE)
            assertFalse("Done closes the menu", kitExists(DevAidTags.SHEET))
            assertEquals("the same puzzle at the end", id, kitScreenKey(PlaySemantics.PuzzleIdKey))
            assertEquals("the same state at the end", state, kitScreenKey(PlaySemantics.PuzzleStateKey))
        } finally {
            kitCloseTheSheet()
        }
    }

    @Test // pin: S-25 "Public companion object { val installed: DevAidHook? }: java.util.ServiceLoader.load(…).firstOrNull(), evaluated once per process (by lazy). It is the debug build's DevAid"; S-30 "The service file holds one line, app.tangramplaytime.play.devaid.DevAid"; decisions.md 2026-10-17 "The DEV aid reaches the screen through one door"; no token
    fun theDebugAppFindsTheAidThroughTheDoorOncePerProcess() {
        // Oracle, in the debug app's own process: `DevAidHook.installed` is not null, it is an instance of the class the
        // service file names, and a second read gives the very same object.
        val first = DevAidHook.installed
        assertNotNull("DevAidHook.installed in the debug app (S-25: the debug build's DevAid; S-30: its service file)", first)
        assertEquals("the aid's class (S-30: the service file's one line)", "app.tangramplaytime.play.devaid.DevAid", first!!.javaClass.name)
        assertSame("a second read of DevAidHook.installed (S-25: evaluated once per process)", first, DevAidHook.installed)
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** Waits for `play-screen`: the IO load that builds the controller is invisible to Compose idling (WO-004 design §3). */
    private fun kitWaitForScreen() {
        composeRule.waitUntil(timeoutMillis = 30_000) { kitExists(PlayTags.SCREEN) }
    }

    /** Fails loudly unless the device language is English (G-10: the kit asserts the English Contract wording only). */
    private fun kitAssertEnglish() {
        val locale = composeRule.activity.resources.configuration.locales[0]
        if (locale.language != "en") {
            throw AssertionError("the device language is ${locale.toLanguageTag()}: this kit reads the aid's English Contract wording (G-10) and never asserts another language; run it with the device language set to English")
        }
    }

    private fun kitExists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun <T> kitScreenKey(key: SemanticsPropertyKey<T>): T =
        composeRule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).fetchSemanticsNode("the Play screen").config.getOrNull(key)
            ?: throw AssertionError("`play-screen` carries no ${key.name}")

    /** [tag]'s bounds in `play-screen` px, the space every touch is injected in. */
    private fun kitBounds(tag: String): Rect {
        val screen = composeRule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).fetchSemanticsNode("the Play screen").boundsInRoot
        return composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode("`$tag`").boundsInRoot.translate(-screen.topLeft)
    }

    /** A still tap at the centre of [tag]'s bounds (tag table: a pill is its touch area), injected on `play-screen`. */
    private fun kitPress(tag: String) {
        val at: Offset = kitBounds(tag).center
        composeRule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).performTouchInput { down(at); up() }
        composeRule.waitForIdle()
    }

    /** The merged text of a merged node (the DEV pill, the message, a pill's label). */
    private fun kitText(tag: String): String =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode("`$tag`").config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""

    /** Types [text] into the passcode field through its semantics text input. */
    private fun kitType(text: String) {
        composeRule.onNodeWithTag(DevAidTags.PASSCODE, useUnmergedTree = true).performTextInput(text)
        composeRule.waitForIdle()
    }

    /** The passcode field's editable text (tag table: "its editable text"); fails when the node has none. */
    private fun kitFieldText(): String =
        composeRule.onNodeWithTag(DevAidTags.PASSCODE, useUnmergedTree = true).fetchSemanticsNode("the passcode field")
            .config.getOrNull(SemanticsProperties.EditableText)?.text
            ?: throw AssertionError("`${DevAidTags.PASSCODE}` carries no editable text")

    /** Every test tag of the unmerged tree, in every compose root, that starts with [prefix]. */
    private fun kitTagsStartingWith(prefix: String): List<String> =
        composeRule.onAllNodes(SemanticsMatcher("a test tag starting with '$prefix'") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true }, useUnmergedTree = true)
            .fetchSemanticsNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }

    /** Leaves the sheet closed for the next test of the run: Cancel or Done (touch geometry: either closes it). */
    private fun kitCloseTheSheet() {
        if (!kitExists(PlayTags.SCREEN) || !kitExists(DevAidTags.SHEET)) return
        when {
            kitExists(DevAidTags.CANCEL) -> kitPress(DevAidTags.CANCEL)
            kitExists(DevAidTags.DONE) -> kitPress(DevAidTags.DONE)
        }
    }

    private companion object {
        /** The prefix of every overlay node's tag (the aid's tag table: `dev-solution-<ID>`, `dev-solution-label-<ID>`). */
        const val KIT_DEV_SOLUTION_PREFIX = "dev-solution-"
    }
}
