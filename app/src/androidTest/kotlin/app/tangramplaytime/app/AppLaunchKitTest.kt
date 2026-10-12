// Acceptance kit (WO-003, TASK-011): the real wiring on a fresh start (tasks.md § WO-003 › Seams S-15:
// `MainActivity` shows `PlayScreen` over `AppGraph`'s controller, library and engine; the bundle read on
// the device). The app's controller lives once per process, so this is the one test of the instrumentation
// run that claims the fresh start; the device is reset (`tools/device_reset.sh`) before it runs.
package app.tangramplaytime.app

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
        composeRule.waitForIdle()
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

    private fun kitExists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
}
