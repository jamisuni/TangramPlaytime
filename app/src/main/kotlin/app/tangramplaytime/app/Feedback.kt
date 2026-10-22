// WO-007 TASK-061 · seam S-50 (designs/WO-007-design.md §2; tasks.md § WO-007 "Files"): the one door for every sound, haptic
// tick and tap click · REQ-033 statement, rule 3, A1; O-07 (`:play` emits, `:app` plays) · decisions.md 2026-10-21 "The five
// sounds and the tick." (E7) as amended ("… the tap click follows the sound switch through `Feedback` …", E7a), and "REQ-033
// A1's oracle is one door …" (E8) as amended ("… the door carries the click too …", E8a); reviews/WO-007-design-review.md F1.
package app.tangramplaytime.app

import androidx.compose.ui.platform.SoundEffect
import app.tangramplaytime.play.PlayEvent

/** The game's five sounds (REQ-033 statement: pick-up, turn, lock, return to the tray, solve; S-52's five files). */
internal enum class Sound { PICK_UP, TURN, LOCK, HOME, SOLVE }

/** Where the door's calls are made (S-50): the device's playback ([DeviceFeedback], S-51), or a kit's recorder. */
internal interface FeedbackOut {
    /** Plays [sound] once. */
    fun play(sound: Sound)

    /** The short haptic tick of a lock (REQ-033 rule 3). */
    fun tick()

    /** The tap click of a Compose `clickable` / `toggleable` (design §3 library fact 2, corrected). */
    fun click()
}

/**
 * The one door (S-50; E8a): the only caller of a sound, a haptic tick or a click in the app.
 *
 * It has two entries. [on] takes every [PlayEvent] the controller emits (S-15Δ4: the controller's `onEvent`). [playClickSound]
 * takes every Compose tap click: `MainActivity` provides this door as `LocalSoundEffect` over everything it composes, and
 * foundation's `performClick()` calls `playClickSound()` before `onClick` (design §3 library facts 2 and 6).
 *
 * Both return at once while [soundOn] reads false (REQ-033 statement, rule 3; A1), read at each call, so a switch takes effect
 * at the next action (decisions.md 2026-10-21 "The two switches write through `PlayController` …", E4). Otherwise both go to
 * [override] when it is set, else to [device].
 */
internal class Feedback(
    private val soundOn: () -> Boolean,
    private val device: FeedbackOut,
) : SoundEffect {
    /**
     * The kits' door (S-50; E8: "the `AppClock.override` pattern, ADR-006"): while non-null, it takes every call in place of
     * [device]. Product code never sets it. Volatile because the instrumentation thread may write it while the main thread reads.
     */
    @Volatile
    internal var override: FeedbackOut? = null

    /** The five actions (REQ-033 statement): each its own sound, and a lock also the tick (rule 3). */
    fun on(event: PlayEvent) {
        if (!soundOn()) return
        val out = override ?: device
        when (event) {
            is PlayEvent.PickedUp -> out.play(Sound.PICK_UP)
            is PlayEvent.Turned -> out.play(Sound.TURN)
            is PlayEvent.Locked -> {
                out.play(Sound.LOCK)
                out.tick()
            }
            is PlayEvent.WentHome -> out.play(Sound.HOME)
            is PlayEvent.Solved -> out.play(Sound.SOLVE)
        }
    }

    /** A Compose tap's click (E7a): with sound on, the click; with sound off, none. */
    override fun playClickSound() {
        if (!soundOn()) return
        (override ?: device).click()
    }
}
