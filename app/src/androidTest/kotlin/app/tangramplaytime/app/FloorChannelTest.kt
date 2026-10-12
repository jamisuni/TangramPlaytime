// scaffolding: floor channel proof (P2), adapted at WO-003 TASK-015 to the Play screen the app now opens on
// (tasks.md § WO-003 › Seams S-15; the tag is S-13's). No acceptance token.
package app.tangramplaytime.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.play.PlayTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloorChannelTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesAndShowsThePlayScreen() {
        composeRule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).assertIsDisplayed()
    }
}
