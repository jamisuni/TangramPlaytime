// Acceptance kit (WO-007, TASK-060; kit stage 4) — `Feedback`, the one door for every sound, tick and click, on the JVM
// (designs/WO-007-design.md §2 S-50; decisions.md 2026-10-21 "REQ-033 A1's oracle is one door …" (E8; the title's ID written
// here without its dot) and its amendment "Amends
// … the door carries the click too …" (E8a); "The five sounds and the tick." (E7) and its amendment "Amends … the tap click
// follows the sound switch through `Feedback` …" (E7a)). Pins, no token: REQ-033's one line is `SoundKitTest`'s, on the
// device (design §2 "Tokens"); these moved here from the design's stage 1 (decisions.md 2026-10-21 "The WO-007 kit lands in
// four stages …", PL1). Prose names a line without the dot (traceability-rules.md §2).
//
// - S-50: "`enum Sound { PICK_UP, TURN, LOCK, HOME, SOLVE }`; `interface FeedbackOut { play(Sound); tick(); click() }`;
//   `class Feedback(soundOn: () -> Boolean, device: FeedbackOut) : SoundEffect`, with `@Volatile internal var override:
//   FeedbackOut?` (the kits' door). `on(event)`: returns while `!soundOn()`. Otherwise it maps PickedUp, Turned, Locked
//   (+ `tick()`), WentHome and Solved to the matching `Sound`. `playClickSound()`: returns while `!soundOn()`, else calls
//   `click()`. Both paths go to `override ?: device`. This door is the only playback, haptic or click caller".
// - REQ-033 statement ("a short sound when a piece is picked up, turned, locked, returned to the tray and when a puzzle is
//   solved, and … no sound WHILE sound is switched off"); rule 3 ("A lock also gives a short haptic tick …; the tick follows
//   the sound switch"); E4 ("The readers: … the sound gate read[s] `controller.settings` at each use"); E7a (the click
//   "returns while sound is off").
// - The kit's device double records each call it receives ("sound X", "tick", "click"); the switch is a flag the test sets,
//   read by the door at each call (S-50's `soundOn: () -> Boolean`). Every "nothing" has its positive control in the same test.
// - Same module: `override` (internal) is set directly. JUnit 4; no `assume…`; helper names carry a `kit` / `Kit` prefix.
package app.tangramplaytime.app

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.play.PlayEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackKitTest {

    /** The kit's `FeedbackOut`: every call it receives, in order. */
    private class KitOut : FeedbackOut {
        val calls = ArrayList<String>()

        override fun play(sound: Sound) {
            calls += "sound ${sound.name}"
        }

        override fun tick() {
            calls += "tick"
        }

        override fun click() {
            calls += "click"
        }

        fun take(): List<String> = calls.toList().also { calls.clear() }
    }

    /** One instance of each of S-4's five events (O-07: `:play` emits them), with what S-50 maps each to. */
    private val kitEvents: List<Pair<PlayEvent, List<String>>> = listOf(
        PlayEvent.PickedUp(PieceId.SQ) to listOf("sound PICK_UP"),
        PlayEvent.Turned(PieceId.LT1) to listOf("sound TURN"),
        PlayEvent.Locked(PieceId.ST1) to listOf("sound LOCK", "tick"),
        PlayEvent.WentHome(PieceId.PG) to listOf("sound HOME"),
        PlayEvent.Solved("shapes-mini-1") to listOf("sound SOLVE"),
    )

    // Pin (no token): S-50 `on(event)` ("it maps PickedUp, Turned, Locked (+ `tick()`), WentHome and Solved to the matching
    // `Sound`"); REQ-033 statement (a short sound for each of the five actions) and rule 3 (the tick on a lock). With sound on,
    // each event calls the device exactly with its own sound — the lock also with one tick, and no other event with a tick —
    // and no event makes a click (the click is `playClickSound()`'s, E8a). The lock's two calls are compared as a set: S-50
    // names no order between them.
    @Test
    fun withSoundOnEachEventPlaysItsOwnSoundAndOnlyALockTicks() {
        val out = KitOut()
        val feedback = Feedback({ true }, out)
        for ((event, expected) in kitEvents) {
            feedback.on(event)
            val got = out.take()
            assertEquals("$event: the device receives exactly its sound (S-50; REQ-033 statement)", expected.sorted(), got.sorted())
        }
        for (piece in PieceId.entries) {
            feedback.on(PlayEvent.Locked(piece))
            assertEquals("a lock of $piece: its sound and one tick (REQ-033 rule 3)", listOf("sound LOCK", "tick"), out.take().sorted())
        }
    }

    // Pin (no token): S-50 `on(event)` ("returns while `!soundOn()`"); REQ-033 statement ("SHALL play no sound WHILE sound is
    // switched off") and rule 3 ("the tick follows the sound switch"); E4 (the gate reads the setting at each use). With the
    // switch off, each of the five events reaches the device not at all — no sound, no tick. Positive control in the same test:
    // the switch turned on, the same five events play (so the double and the events can show a call); off again, nothing — the
    // gate is read at each call, not once.
    @Test
    fun withSoundOffNoEventPlaysASoundOrATick() {
        val out = KitOut()
        var on = false
        val feedback = Feedback({ on }, out)
        for ((event, _) in kitEvents) feedback.on(event)
        assertEquals("sound off: the five events reach the device not at all (REQ-033 statement, rule 3)", emptyList<String>(), out.take())

        on = true
        for ((event, _) in kitEvents) feedback.on(event)
        assertEquals("positive control: sound on, the five events play", kitEvents.flatMap { it.second }.sorted(), out.take().sorted())

        on = false
        for ((event, _) in kitEvents) feedback.on(event)
        assertEquals("sound off again: nothing (the gate is read at each call, E4)", emptyList<String>(), out.take())
    }

    // Pin (no token): S-50 `playClickSound()` ("returns while `!soundOn()`, else calls `click()`"); E7a (the tap click follows the
    // sound switch through `Feedback`; "with sound on, today's click stays … with sound off, there is none"); E8a (the click is the
    // door's second entry). With sound on, one call is one click and nothing else; with sound off, none; on again, one again.
    @Test
    fun theClickFollowsTheSwitch() {
        val out = KitOut()
        var on = true
        val feedback = Feedback({ on }, out)
        feedback.playClickSound()
        assertEquals("sound on: a tap's click reaches the device as one click (E7a)", listOf("click"), out.take())
        on = false
        feedback.playClickSound()
        feedback.playClickSound()
        assertEquals("sound off: no click (E7a; REQ-033 statement)", emptyList<String>(), out.take())
        on = true
        feedback.playClickSound()
        assertEquals("positive control: sound on again, one click", listOf("click"), out.take())
    }

    // Pin (no token): S-50 ("Both paths go to `override ?: device`"; "`@Volatile internal var override: FeedbackOut?` (the kits'
    // door)"); E8 ("The kit sets `AppGraph.feedback.override` to a recorder"). With an override set, the five events and a click
    // go to the override and the device receives nothing; with it null again, they go to the device. With sound off and the
    // override set, neither receives anything (the gate comes before the door's choice).
    @Test
    fun anOverrideTakesEveryCallInPlaceOfTheDevice() {
        val device = KitOut()
        val recorder = KitOut()
        var on = true
        val feedback = Feedback({ on }, device)
        val expected = kitEvents.flatMap { it.second } + "click"

        feedback.override = recorder
        for ((event, _) in kitEvents) feedback.on(event)
        feedback.playClickSound()
        assertEquals("with an override, it takes every call (S-50)", expected.sorted(), recorder.take().sorted())
        assertEquals("…and the device receives nothing", emptyList<String>(), device.take())

        on = false
        for ((event, _) in kitEvents) feedback.on(event)
        feedback.playClickSound()
        assertEquals("sound off with the override set: the override receives nothing", emptyList<String>(), recorder.take())
        assertEquals("…nor the device", emptyList<String>(), device.take())

        on = true
        feedback.override = null
        for ((event, _) in kitEvents) feedback.on(event)
        feedback.playClickSound()
        assertEquals("the override null again: the device takes every call", expected.sorted(), device.take().sorted())
        assertEquals("…and the old recorder nothing", emptyList<String>(), recorder.take())
    }
}
