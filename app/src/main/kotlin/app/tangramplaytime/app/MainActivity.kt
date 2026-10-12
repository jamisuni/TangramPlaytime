// WO-003 TASK-015 · seam S-15 (tasks.md § WO-003 › Seams): the single activity shows the Play screen over
// AppGraph's controller. The texts are `:app` resources (G-10, O-06), the layout is `:app`'s (O-05), reduced
// motion is the system's animator duration scale (REQ-023, REQ-051; read without a permission, G-01).
// The edge-to-edge box stays as P2 left it: insets are WO-006's.
package app.tangramplaytime.app

import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.tangramplaytime.play.PlayScreen
import app.tangramplaytime.play.PlayTexts

class MainActivity : ComponentActivity() {
    /** True while the system's animator duration scale is 0; re-read each time the activity starts. */
    private var reducedMotion by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val texts = PlayTexts(
            sizeSmall = getString(R.string.size_mark_small),
            sizeMedium = getString(R.string.size_mark_medium),
            sizeLarge = getString(R.string.size_mark_large),
            titleLanguage = titleLanguage(resources.configuration),
        )
        setContent {
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                PlayScreen(AppGraph.controller, PlayLayouts.phone, texts, reducedMotion, Modifier.fillMaxSize())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // The setting can change while the app is in the background; onStart precedes every first frame.
        reducedMotion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    override fun onStop() {
        // A drag ended by the activity stopping is a missed drop: the piece goes back to its tray cell, with
        // no pulse (decisions.md 2026-10-11, `chg-draft-req020-interrupted-drag.md`).
        val controller = AppGraph.controller
        if (controller.dragged != null) controller.goHome()
        super.onStop()
    }

    /** `fi` when the device language is Finnish, else `en` (REQ-047; S-11: a missing `fi` title falls back to `en`). */
    private fun titleLanguage(configuration: Configuration): String =
        if (configuration.locales[0].language == "fi") "fi" else "en"
}
