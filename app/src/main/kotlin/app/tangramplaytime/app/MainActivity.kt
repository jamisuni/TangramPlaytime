// WO-003 TASK-015 · seam S-15 (tasks.md § WO-003 › Seams): the single activity shows the Play screen over
// AppGraph's controller. The texts are `:app` resources (G-10, O-06), the layout is `:app`'s (O-05), reduced
// motion is the system's animator duration scale (REQ-023, REQ-051; read without a permission, G-01).
// WO-004 TASK-020 · S-15Δ's strings only (tasks.md § WO-004 seam note 3): the ten S-11Δ texts from `values/` and
// `values-fi/`.
// WO-004 TASK-022 · the rest of S-15Δ (designs/WO-004-design.md §2): `AppGraph.start` loads on IO; `PlayLoading`
// shows until the controller exists, then `PlayScreen`; back closes the overview while it is open (REQ-050 rule);
// `onStop` still sends a running drag home, on the nullable controller.
// WO-005 TASK-026 · S-15Δ2 (tasks.md § WO-005 › Seams): `PlayScreen` is given `DevAidHook.installed` (REQ-046's door;
// null in a release build, G-08).
// WO-006 TASK-031 · S-35, the window shell (tasks.md § WO-006 › Seams): at each activity creation the layout class
// (S-34, TYPE-007), the orientation policy, the phone or tablet `PlayLayout`, edge to edge with the system bars' and
// the cutout's insets and the keyboard inset in `PlayLayout` (S-31) · REQ-006; REQ-035 statement, rules 3–4; REQ-036
// statement, rules 1–2; O-05 · decisions.md 2026-10-17 "TYPE-007 reads the window's upright width, once in `:kernel`"
// (Q1), "The tablet layout, as drawn" (Q2), "Edge to edge, the insets and the keyboard, as drawn" (Q3), "Back closes
// the grid; no other surface is an open overlay" (Q4). P2's `safeDrawingPadding` box is gone.
// WO-006 TASK-032 · S-36, one language rule (tasks.md § WO-006 › Seams): the base context is wrapped by `AppLanguage` in
// `attachBaseContext`, and `PlayTexts.titleLanguage` is the same rule's value · REQ-047; G-10, O-06 · decisions.md 2026-10-17
// "One language rule for strings and titles" (Q7), replacing the 2026-10-12 TASK-015 row's split.
// WO-008 TASK-047 · S-11Δ3 (designs/WO-008-design.md §2): `PlayTexts.hoursMinutes` from `time_hours_minutes` (G-10).
// WO-008 TASK-049 · S-15Δ3 (designs/WO-008-design.md §2): the window's touches (`dispatchTouchEvent`, before `super`) and the
// activity's start and stop (`onStart`; `onStop`, after `goHome`) go to `AppGraph` · TYPE-005; REQ-029 · decisions.md
// 2026-10-20 "TYPE-005 as code." · design §3 library facts 1 and 2.
package app.tangramplaytime.app

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import app.tangramplaytime.kernel.LayoutClass
import app.tangramplaytime.play.DevAidHook
import app.tangramplaytime.play.PlayInsets
import app.tangramplaytime.play.PlayLayout
import app.tangramplaytime.play.PlayLoading
import app.tangramplaytime.play.PlayScreen
import app.tangramplaytime.play.PlayTexts

class MainActivity : ComponentActivity() {
    /** True while the system's animator duration scale is 0; re-read each time the activity starts. */
    private var reducedMotion by mutableStateOf(false)

    /**
     * S-36: the activity's context speaks one language, the one the device's first locale names (`fi` or `en`), so every
     * string resolved through it — the activity's, `:play`'s and the DEV aid's — follows that locale alone (REQ-047; Q7). A
     * device language change recreates the activity, and this runs again.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // S-35 step 1: the class from the window's upright width, which a rotation does not change (S-34; Q1).
        val layoutClass = LayoutClass.of(resources.configuration.smallestScreenWidthDp)
        // S-35 step 2, before the first frame: a phone runs in portrait only (REQ-035 statement), a tablet in both
        // orientations (REQ-036 statement). The manifest declares portrait, so a phone is never created in landscape;
        // this request unlocks a tablet, and locks a phone again after a tablet instance of this activity unlocked it
        // (a display that became phone-sized). Android 16 and later ignore both on windows 600 dp and wider.
        requestedOrientation = when (layoutClass) {
            LayoutClass.PHONE -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            LayoutClass.TABLET -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        // S-35 step 4: drawn behind the system bars, with dark icons on the always-light paper, also when the system is
        // dark (Q3; WO-006 checkpoint item 2). Both bars are transparent; `light` never uses its dark scrim on API 26+.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        AppGraph.start(this)
        val texts = PlayTexts(
            sizeSmall = getString(R.string.size_mark_small),
            sizeMedium = getString(R.string.size_mark_medium),
            sizeLarge = getString(R.string.size_mark_large),
            // S-36: the same rule as the strings, read from the configuration they are resolved under (S-11: a missing
            // `fi` title falls back to `en`).
            titleLanguage = AppLanguage.of(resources.configuration.locales[0]),
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
            // WO-008 S-11Δ3: the one time format's "h" and "min" (REQ-029 rule 2; G-10).
            hoursMinutes = getString(R.string.time_hours_minutes),
        )
        // S-35 step 3: the layout of the class (S-5's phone value unchanged, or the tablet value of Q2).
        val layout = when (layoutClass) {
            LayoutClass.PHONE -> PlayLayouts.phone
            LayoutClass.TABLET -> PlayLayouts.tablet
        }
        setContent {
            // After a recreation the controller already exists, so the first frame composes the Play screen
            // (designs/WO-004-design.md §2 "What survives": cues already set count as shown).
            val controller by AppGraph.controller.collectAsState()
            val current = controller
            // S-35 step 4: both fill the window; the Play screen keeps its controls inside the insets itself (S-32).
            if (current == null) {
                PlayLoading(Modifier.fillMaxSize())
            } else {
                // S-35 step 5: Back closes the grid while it is open; nothing else handles Back (Q4).
                BackHandler(enabled = current.overviewOpen) { current.closeOverview() }
                PlayScreen(current, layout.inWindow(), texts, reducedMotion, Modifier.fillMaxSize(), devAid = DevAidHook.installed)
            }
        }
    }

    /**
     * S-15Δ3: every touch of the window is the player's "last touch" (TYPE-005; decisions.md 2026-10-20 "TYPE-005 as code.":
     * down, move or up), reported before the window's views handle it. Every real touch of the window passes here
     * (`DecorView` → `Window.Callback`, design §3 library fact 2).
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        AppGraph.touched()
        return super.dispatchTouchEvent(ev)
    }

    override fun onStart() {
        super.onStart()
        // The setting can change while the app is in the background; onStart precedes every first frame.
        reducedMotion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        // S-15Δ3: the game is on screen from here to onStop (TYPE-005 "visible on screen"; design §3 library fact 1).
        AppGraph.visible(true)
    }

    override fun onStop() {
        // A drag ended by the activity stopping is a missed drop: the piece goes back to its tray cell, with
        // no pulse (decisions.md 2026-10-11, `chg-draft-req020-interrupted-drag.md`). Before the load has built
        // the controller there is no drag.
        val controller = AppGraph.controller.value
        if (controller != null && controller.dragged != null) controller.goHome()
        // S-15Δ3, after goHome: the game leaves the screen, and the times are written with the piece back home.
        AppGraph.visible(false)
        super.onStop()
    }
}

/**
 * S-35 step 3: this layout with the window's insets in dp (S-31) — the system bars ∪ the display cutout per side, left and
 * right as laid out left to right — and the soft keyboard's bottom inset as `keyboardDp` (REQ-035 rule 3, REQ-036 rule 2;
 * Q3). The keyboard's inset reaches Compose on API 26–29 because the activity is `adjustResize` (the manifest).
 */
@Composable
private fun PlayLayout.inWindow(): PlayLayout {
    val density = LocalDensity.current
    val bars = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val pxPerDp = density.density
    return copy(
        insets = PlayInsets(
            topDp = bars.getTop(density) / pxPerDp,
            bottomDp = bars.getBottom(density) / pxPerDp,
            leftDp = bars.getLeft(density, LayoutDirection.Ltr) / pxPerDp,
            rightDp = bars.getRight(density, LayoutDirection.Ltr) / pxPerDp,
        ),
        keyboardDp = WindowInsets.ime.getBottom(density) / pxPerDp,
    )
}
