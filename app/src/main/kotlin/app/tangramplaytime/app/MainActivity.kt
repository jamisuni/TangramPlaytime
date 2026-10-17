// WO-003 TASK-015 · seam S-15 (tasks.md § WO-003 › Seams): the single activity shows the Play screen over
// AppGraph's controller. The texts are `:app` resources (G-10, O-06), the layout is `:app`'s (O-05), reduced
// motion is the system's animator duration scale (REQ-023, REQ-051; read without a permission, G-01).
// The edge-to-edge box stays as P2 left it: insets are WO-006's.
// WO-004 TASK-020 · S-15Δ's strings only (tasks.md § WO-004 seam note 3): the ten S-11Δ texts from `values/` and
// `values-fi/`.
// WO-004 TASK-022 · the rest of S-15Δ (designs/WO-004-design.md §2): `AppGraph.start` loads on IO; `PlayLoading`
// shows until the controller exists, then `PlayScreen`; back closes the overview while it is open (REQ-050 rule);
// `onStop` still sends a running drag home, on the nullable controller.
// WO-005 TASK-026 · S-15Δ2 (tasks.md § WO-005 › Seams): `PlayScreen` is given `DevAidHook.installed` (REQ-046's door;
// null in a release build, G-08).
package app.tangramplaytime.app

import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.tangramplaytime.play.DevAidHook
import app.tangramplaytime.play.PlayLoading
import app.tangramplaytime.play.PlayScreen
import app.tangramplaytime.play.PlayTexts

class MainActivity : ComponentActivity() {
    /** True while the system's animator duration scale is 0; re-read each time the activity starts. */
    private var reducedMotion by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppGraph.start(this)
        val texts = PlayTexts(
            sizeSmall = getString(R.string.size_mark_small),
            sizeMedium = getString(R.string.size_mark_medium),
            sizeLarge = getString(R.string.size_mark_large),
            titleLanguage = titleLanguage(resources.configuration),
            restart = getString(R.string.restart),
            retry = getString(R.string.retry),
            next = getString(R.string.next),
            bestTime = getString(R.string.best_time),
            noTime = getString(R.string.no_time),
            inProgress = getString(R.string.state_in_progress),
            solved = getString(R.string.state_solved),
            counterFormat = getString(R.string.counter_format),
            allPuzzles = getString(R.string.all_puzzles),
            done = getString(R.string.done),
        )
        setContent {
            // After a recreation the controller already exists, so the first frame composes the Play screen
            // (designs/WO-004-design.md §2 "What survives": cues already set count as shown).
            val controller by AppGraph.controller.collectAsState()
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                val current = controller
                if (current == null) {
                    PlayLoading(Modifier.fillMaxSize())
                } else {
                    BackHandler(enabled = current.overviewOpen) { current.closeOverview() }
                    PlayScreen(current, PlayLayouts.phone, texts, reducedMotion, Modifier.fillMaxSize(), devAid = DevAidHook.installed)
                }
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
        // no pulse (decisions.md 2026-10-11, `chg-draft-req020-interrupted-drag.md`). Before the load has built
        // the controller there is no drag.
        val controller = AppGraph.controller.value
        if (controller != null && controller.dragged != null) controller.goHome()
        super.onStop()
    }

    /** `fi` when the device language is Finnish, else `en` (REQ-047; S-11: a missing `fi` title falls back to `en`). */
    private fun titleLanguage(configuration: Configuration): String =
        if (configuration.locales[0].language == "fi") "fi" else "en"
}
