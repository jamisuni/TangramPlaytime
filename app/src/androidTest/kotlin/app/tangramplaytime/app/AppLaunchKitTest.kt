// Acceptance kit (WO-003, TASK-011): the real wiring on a fresh start (tasks.md § WO-003 › Seams S-15:
// `MainActivity` shows `PlayScreen` over `AppGraph`'s controller, library and engine; the bundle read on
// the device). The app's controller lives once per process, so this is the one test of the instrumentation
// run that claims the fresh start; the device is reset (`tools/device_reset.sh`) before it runs.
// Amended WO-004 (TASK-019; designs/WO-004-design.md §3 "WO-003 seam text superseded"):
// - Precondition: a cleared store. `tools/device_reset.sh` runs before the instrumentation run, so the app starts as a
//   fresh install (design §3: "`AppLaunchKitTest`'s fresh start needs a cleared store"); only this class claims the fresh
//   start, and every other test of the app set leaves the one controller as it found it.
// - The app now loads the bundle and the store on IO and composes `PlayScreen` once the controller exists (S-15Δ), which
//   Compose idling cannot see (design §3; reviews/WO-004-design-review.md F2): each test waits for `play-screen` with
//   `waitUntil` before it reads the screen.
// - REQ-040 A1 is re-covered here beside REQ-045 A1 (workorders/WO-004.md scope note "Re-covered lines").
package app.tangramplaytime.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchKitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test // REQ-045.A1
    fun aFreshStartOpensOnAMiniPuzzleWhoseTrayHoldsThreePieces() {
        // REQ-045 A1: "A fresh install opens on a mini puzzle whose tray holds three pieces." Statement:
        // "WHILE a mini puzzle is open, the tray SHALL show only that puzzle's pieces." The real launch: the
        // opened puzzle is read from `play-screen`'s PuzzleIdKey and looked up in a library the kit builds
        // itself from the same bundle; the tray's nodes are counted.
        kitWaitForScreen()
        val screen = composeRule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).fetchSemanticsNode("the Play screen")
        val id = screen.config.getOrNull(PlaySemantics.PuzzleIdKey) ?: throw AssertionError("`play-screen` carries no PuzzleIdKey")
        val library = PuzzleLibraries.bundled(LockEngine())
        val opened = library.byId(id) ?: throw AssertionError("the opened puzzle $id is not in the bundle")
        assertEquals("$id is a mini puzzle", PuzzleKind.MINI, opened.kind)
        assertEquals("$id uses three pieces", 3, opened.pieces.size)
        val inTray = PieceId.entries.filter { kitExists(PlayTags.trayPiece(it)) }.toSet()
        assertEquals("the tray holds exactly the puzzle's three pieces", opened.pieces, inTray)
        assertEquals("three miniatures", 3, inTray.size)
        for (piece in inTray) composeRule.onNodeWithTag(PlayTags.trayPiece(piece), useUnmergedTree = true).assertIsDisplayed()
        assertTrue("nothing on the board on a fresh start", PieceId.entries.none { kitExists(PlayTags.boardPiece(it)) })
        assertEquals("a fresh puzzle is New (TYPE-006)", PuzzleState.NEW, screen.config.getOrNull(PlaySemantics.PuzzleStateKey))
    }

    @Test // REQ-040.A1
    fun theFirstPuzzleShownOnAFreshInstallHasRatingOne() {
        // REQ-040 A1: "The first puzzle shown on a fresh install has rating 1." Rule: "Rating: an integer from 1 to 5, shown
        // as dots in the top bar." The real launch over a cleared store (this class's precondition): the opened puzzle —
        // `play-screen`'s PuzzleIdKey, read against the title's text (one of that puzzle's titles: the app picks the device
        // language, REQ-047) — is the bundle's first in ‹ › order (S-1Δ: with no last-opened id the controller opens
        // `puzzles.first()`; IPuzzleLibrary KDoc: "the first entry on a fresh install is a mini puzzle of rating 1"), and
        // its puzzle file's rating is 1. (The rule's dots in the top bar are a pin of the play kit's TopBarKitTest.)
        kitWaitForScreen()
        val screen = composeRule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).fetchSemanticsNode("the Play screen")
        val id = screen.config.getOrNull(PlaySemantics.PuzzleIdKey) ?: throw AssertionError("`play-screen` carries no PuzzleIdKey")
        val library = PuzzleLibraries.bundled(LockEngine())
        val opened = library.byId(id) ?: throw AssertionError("the opened puzzle $id is not in the bundle")
        val title = composeRule.onNodeWithTag(PlayTags.TITLE).fetchSemanticsNode("the title")
            .config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
        assertTrue("the title '$title' is one of $id's titles ${opened.titles.values}", title in opened.titles.values)
        assertEquals("the first puzzle shown is the first in ‹ › order", library.puzzles.first().id, id)
        assertEquals("$id's rating (its puzzle file)", 1, opened.rating)
    }

    /** Waits for `play-screen`: the IO load that builds the controller is invisible to Compose idling (design §3). */
    private fun kitWaitForScreen() {
        composeRule.waitUntil(timeoutMillis = 30_000) { kitExists(PlayTags.SCREEN) }
    }

    private fun kitExists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
}
