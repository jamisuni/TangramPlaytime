// WO-007 TASK-061 · seam S-51 (designs/WO-007-design.md §2; tasks.md § WO-007 "Files"): the platform playback behind the one
// door (S-50) · REQ-033 rules 1 and 3; REQ-010 rule 2; G-01 (no permission), G-11 (haptics only through the permission-free
// API, never `Vibrator`) · decisions.md 2026-10-21 "The five sounds and the tick." (E7: one `SoundPool`; the tick is
// `View.performHapticFeedback(CLOCK_TICK)`) as amended (E7a: the click is the same `View.playSoundEffect` call
// `AndroidSoundEffect` makes) · design §3 library facts 3 and 4, "Carry 4".
package app.tangramplaytime.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View

/**
 * The device's sounds, tick and click (S-51). Built once per process by `AppGraph.start` (S-15Δ4) over the application
 * context; only [Feedback] calls it (S-50). Every member runs on the main thread.
 *
 * - **Sounds:** one [SoundPool] of [MAX_STREAMS] streams, game sonification, over S-52's five files, loaded at construction.
 *   `SoundPool` needs no permission and loads asynchronously (library fact 3): a sound asked for before its file has loaded is
 *   not played. Past [MAX_STREAMS] the pool stops the lowest priority, then the oldest, so the priorities (the solve 2, the
 *   lock 1, the rest 0) keep the chime and the last lock over a burst of other sounds (carry 4). Each file plays once, at
 *   its own level (decisions.md 2026-10-21 "How `tools/make_sounds.py` reads the sound recipes …": the levels are the files').
 * - **The tick** and **the click** are made on [view], the decor view of the `MainActivity` on screen, with no flag: the
 *   device's own haptic-feedback and touch-sound settings apply, as for any view (library fact 4; E7a). With no view, neither
 *   is made.
 */
internal class DeviceFeedback(context: Context) : FeedbackOut {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    /** Each sound's id in [pool] (`SoundPool.load`'s priority argument has no effect; the platform asks for 1). */
    private val ids: Map<Sound, Int> = Sound.entries.associateWith { pool.load(context, fileOf(it), 1) }

    /**
     * The decor view the tick and the click are made on (S-51): set by a `MainActivity` in `onStart`, and cleared in `onStop`
     * by the activity that set it (`AppGraph.feedbackView` / `feedbackViewGone`). Null while no activity of the game is
     * started.
     */
    var view: View? = null

    override fun play(sound: Sound) {
        pool.play(ids.getValue(sound), FULL_VOLUME, FULL_VOLUME, priorityOf(sound), NO_LOOP, NORMAL_RATE)
    }

    /** G-11: the permission-free haptic API, never `Vibrator` / `VibratorManager` (REQ-010 rule 2). */
    override fun tick() {
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** E7a: the call today's `AndroidSoundEffect` makes, under the device's touch-sound setting. */
    override fun click() {
        view?.playSoundEffect(SoundEffectConstants.CLICK)
    }

    private companion object {
        /** S-51: "maxStreams 4". */
        const val MAX_STREAMS = 4

        const val FULL_VOLUME = 1f
        const val NO_LOOP = 0
        const val NORMAL_RATE = 1f

        /** S-52's five files, one per sound. */
        fun fileOf(sound: Sound): Int = when (sound) {
            Sound.PICK_UP -> R.raw.sfx_pick_up
            Sound.TURN -> R.raw.sfx_turn
            Sound.LOCK -> R.raw.sfx_lock
            Sound.HOME -> R.raw.sfx_home
            Sound.SOLVE -> R.raw.sfx_solve
        }

        /** S-51: "priority SOLVE 2, LOCK 1, the rest 0". */
        fun priorityOf(sound: Sound): Int = when (sound) {
            Sound.SOLVE -> 2
            Sound.LOCK -> 1
            Sound.PICK_UP, Sound.TURN, Sound.HOME -> 0
        }
    }
}
