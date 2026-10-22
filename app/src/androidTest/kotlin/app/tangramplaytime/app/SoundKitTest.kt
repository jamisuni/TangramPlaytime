// Acceptance kit (WO-007, TASK-060; kit stage 4): REQ-033 A1 in the real app — one door for every sound, tick and click,
// driven by touch with the switch off and then on (designs/WO-007-design.md §2 S-50, S-15Δ4, "Tokens (Q10)" and "Kit doors
// (Q11)"; decisions.md 2026-10-21 "REQ-033 A1's oracle is one door …" (E8; the title's ID written here without its dot) and
// its amendment "Amends … the door carries the click too, and the sound-off leg presses the click path." (E8a, which wins
// where they differ); "The five sounds and the tick." (E7) and its amendment (E7a); reviews/WO-007-design-review.md F1 and
// § F1 re-check).
//
// - Token: REQ-033 A1 on the one test (traceability-rules.md §2). `chg-draft-req033-a1-device-oracle.md` is pending; the build
//   follows the locked A1 (E8: "verified as locked").
// - The door (E8, E8a): every sound and tick goes through `Feedback.on(PlayEvent)`, and every Compose tap click through
//   `Feedback.playClickSound()` (`LocalSoundEffect`, S-15Δ4); both go to `override ?: device` (S-50). The kit's recorder sits
//   at `AppGraph.feedback.override` (KitApp `kitUseFeedbackRecorder`; null again through `onRestore`). No source scan, no caller
//   inventory, no allow-list (E8: that no other code plays a sound is the code reviewer's read).
// - Why it can fail (the F1 re-check, item 3): a gate that ignored the switch shows calls in the sound-off leg; a missing or
//   misplaced provider leaves the clicks on the platform's own sound, and then the positive control — Done on the settings
//   screen and a grid cell inside `PlayScreen`, one control in each subtree the provider must cover — records no click.
// - Everything is pressed or driven by touch on `play-screen` (KitApp `pressCentre`, `tap`, `pickUpFromTray`, `steerDrag`,
//   `release`): the gear, the sound switch, Done, the counter, a grid cell, tray taps, drags and drops. The routes and drop
//   points are planned from the engine's solution poses, as `PlayThroughKitTest` plans them (its `planDrop`, copied here: it is
//   private there). The puzzles are New minis or warm-ups whose parallelogram, if any, is not mirrored (no badge press, whose
//   event S-4 does not name).
// - The settings are put back by KitApp `start`'s `onRestore("the settings")` (E4a), the controller by its own restore. Nothing
//   sleeps; every wait is bounded and fails loudly.
package app.tangramplaytime.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SoundKitTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val kit = KitApp(composeRule)

    @get:Rule(order = 1)
    val kitRestore: TestRule = kit.restoreRule()

    private companion object {
        /** What S-50 maps each action to on the recorder (E7: each action its own sound; REQ-033 rule 3: a lock also ticks). */
        val KIT_PICK_UP = listOf("sound PICK_UP")
        val KIT_TURN = listOf("sound TURN")
        val KIT_LOCK = listOf("sound LOCK", "tick")
        val KIT_SOLVE = listOf("sound LOCK", "sound SOLVE", "tick")
        val KIT_HOME = listOf("sound HOME")
        val KIT_CLICK = listOf("click")
    }

    /** What one leg's five actions recorded, for the positive control's totals. */
    private class KitTally {
        val calls = ArrayList<String>()
        var locks = 0
    }

    @Test // REQ-033.A1
    fun withSoundOffNoActionMakesASoundOrAHapticTick() {
        // REQ-033 A1: "With sound off, no action makes a sound or a haptic tick." Statement: "SHALL play no sound WHILE sound is
        // switched off in the settings"; rule 3: the tick "follows the sound switch". E8a's oracle, in one test:
        // - Sound turned off through the real switch, pressed by touch on the open screen. That press records one click, because
        //   sound was still on when it fired (E8a; `performClick` plays before `onClick`, design §3 fact 2); the leg clears it.
        // - With sound off: Done; the five actions by touch on a New puzzle — a pick-up and a return to the tray, tray taps
        //   (turns), drags and drops that lock, the last of which solves it; the counter opens the grid and a cell is pressed;
        //   the settings opened again and the sound switch pressed to turn sound back on. The recorder stays empty after every
        //   one of these.
        // - The positive control, sound on (E8a): Done records one click; the five actions on a second New puzzle record each
        //   action's own sound — the pick-up PICK_UP, each tray tap TURN, each lock LOCK and one tick, the last lock LOCK, a tick
        //   and SOLVE, the return HOME — and the grid cell records one click.
        // - The router's raw-pointer controls (the gear, the counter, the pieces) make no click in either state (E7a: they "play
        //   nothing"), read at each of their presses.
        kit.start("REQ-033 A1")
        val (offPuzzle, onPuzzle) = kitTwoPuzzles("REQ-033 A1")
        assertTrue("fixture: sound is on at the start (REQ-033 ASSUMPTION: on by default; a cleared store)", kit.onController { settings.soundOn })
        kit.openByController(offPuzzle.id, "REQ-033 A1: ${offPuzzle.id} New")
        val rec = kit.kitUseFeedbackRecorder("REQ-033 A1")

        // Sound off through the real switch.
        kit.openSettingsByTouch("REQ-033 A1: the gear")
        assertEquals("the gear (a router control) makes no click (E7a)", emptyList<String>(), rec.take())
        kit.scrollIntoView(KitAppTag.SETTINGS_SOUND, "REQ-033 A1: the sound switch")
        kit.pressCentre(KitAppTag.SETTINGS_SOUND)
        kit.await("REQ-033 A1: the sound switch reads Off") { kitToggle(KitAppTag.SETTINGS_SOUND) == ToggleableState.Off }
        assertFalse("the switch turned sound off, at once (E4)", kit.onController { settings.soundOn })
        assertEquals("the switch's own press records one click: sound was still on when it fired (E8a)", KIT_CLICK, rec.take())

        // With sound off, nothing reaches the door.
        kit.closeSettingsByDone("REQ-033 A1: Done with sound off")
        kitAssertNothing(rec, "Done with sound off")
        kitFiveActionsByTouch("sound off", offPuzzle, rec, soundOn = false)
        kit.pressCentre(KitAppTag.COUNTER)
        kit.awaitExists(KitAppTag.OVERVIEW, "REQ-033 A1: the counter opens the grid, sound off")
        kitAssertNothing(rec, "the counter (a router control) with sound off")
        kitPressCell(onPuzzle.id, "sound off")
        kitAssertNothing(rec, "a grid cell with sound off")
        kit.openSettingsByTouch("REQ-033 A1: the settings again")
        kitAssertNothing(rec, "the gear with sound off")
        kit.scrollIntoView(KitAppTag.SETTINGS_SOUND, "REQ-033 A1: the sound switch again")
        kit.pressCentre(KitAppTag.SETTINGS_SOUND)
        kit.await("REQ-033 A1: the sound switch reads On") { kitToggle(KitAppTag.SETTINGS_SOUND) == ToggleableState.On }
        assertTrue("the switch turned sound back on", kit.onController { settings.soundOn })
        kitAssertNothing(rec, "the switch turning sound back on, pressed while sound was off (E8a)")

        // The positive control, sound on: the same paths record.
        kit.closeSettingsByDone("REQ-033 A1: Done with sound on")
        assertEquals("positive control: Done records one click with sound on (E8a)", KIT_CLICK, rec.take())
        assertEquals("fixture: the grid cell opened ${onPuzzle.id}", onPuzzle.id, kit.shownId())
        val tally = kitFiveActionsByTouch("sound on", onPuzzle, rec, soundOn = true)
        for (sound in listOf("sound PICK_UP", "sound TURN", "sound LOCK", "sound HOME", "sound SOLVE")) {
            assertTrue("positive control: the five actions recorded $sound (REQ-033 statement): ${tally.calls}", sound in tally.calls)
        }
        assertEquals("positive control: one tick per lock (REQ-033 rule 3): ${tally.calls}", tally.locks, tally.calls.count { it == "tick" })
        kit.pressCentre(KitAppTag.COUNTER)
        kit.awaitExists(KitAppTag.OVERVIEW, "REQ-033 A1: the counter opens the grid, sound on")
        assertEquals("the counter (a router control) makes no click with sound on (E7a)", emptyList<String>(), rec.take())
        kitPressCell(onPuzzle.id, "sound on")
        assertEquals("positive control: a grid cell records one click with sound on (E8a)", KIT_CLICK, rec.take())
    }

    // ── The five actions by touch ────────────────────────────────────────────────────────────────────────

    /**
     * The five actions of REQ-033's statement on [p] (open, New), by touch, each read at the door at once: a return — a piece
     * picked up from the tray and released over its own cell, so it goes home; then every piece of an order the engine locks at
     * the solution poses: its tray taps (each a turn; a full turn of eight taps on the first piece when no piece needs one, so the
     * leg always turns), its pick-up, and its drop, which locks it (the last one solves [p]). With [soundOn] false each action
     * must record nothing; with it true, exactly its own calls (S-50's map; the lock's calls compared as a set, S-50 names no
     * order between them).
     */
    private fun kitFiveActionsByTouch(leg: String, p: PuzzleDefinition, rec: KitFeedbackRecorder, soundOn: Boolean): KitTally {
        val tally = KitTally()
        fun expect(expected: List<String>, what: String) {
            val got = rec.take()
            tally.calls += got
            if (soundOn) {
                assertEquals("$leg, ${p.id}: $what records its own calls (S-50; REQ-033 statement)", expected.sorted(), got.sorted())
            } else {
                assertEquals("$leg, ${p.id}: $what makes no sound and no tick (REQ-033 A1)", emptyList<String>(), got)
            }
        }
        assertEquals("fixture: $leg: ${p.id} open", p.id, kit.shownId())
        assertEquals("fixture: $leg: ${p.id} New", PuzzleState.NEW, kit.shownState())
        val order = kitAppLockOrder(p) ?: throw AssertionError("kit: no lock order for ${p.id}")

        // A return to the tray.
        val back = order.first().piece
        val start = kit.trayPoint(back)
        val finger = kit.pickUpFromTray(back)
        expect(KIT_PICK_UP, "a pick-up from the tray")
        kit.moveFinger(finger, start, 4)
        kit.release()
        kit.awaitExists(PlayTags.trayPiece(back), "$leg, ${p.id}: $back back in the tray")
        assertFalse("fixture: $leg, ${p.id}: $back is not on the board after its return", kit.exists(PlayTags.boardPiece(back)))
        expect(KIT_HOME, "a return to the tray")

        // The turns, the locks and the solve.
        val rest = order.associate { it.piece to kit.key(PlayTags.trayPiece(it.piece), PlaySemantics.TurnKey) }
        val taps = order.map { ((it.turn.step - rest.getValue(it.piece).step) % 8 + 8) % 8 }.toMutableList()
        if (taps.all { it == 0 }) taps[0] = 8
        for ((i, target) in order.withIndex()) {
            repeat(taps[i]) { n ->
                kit.tapTrayPiece(target.piece, 1)
                expect(KIT_TURN, "tray tap ${n + 1} on ${target.piece}")
            }
            assertEquals("fixture: $leg, ${p.id}: ${target.piece}'s tray turn is its pose's", target.turn, kit.key(PlayTags.trayPiece(target.piece), PlaySemantics.TurnKey))
            var f = kit.pickUpFromTray(target.piece)
            expect(KIT_PICK_UP, "the pick-up of ${target.piece}")
            val dragTag = PlayTags.dragPiece(target.piece)
            val offset = f - kit.toPx(kit.key(dragTag, PlaySemantics.CentreKey))
            val drop = kitPlanDrop(p, target, offset)
                ?: throw AssertionError("kit: $leg, ${p.id}: no drop point locks ${target.piece} at $target with the finger over the board")
            f = kit.steerDrag(target.piece, drop, f)
            assertTrue("kit: $leg, ${p.id}: the finger $f is over the board, so the release is a drop on it", kit.bounds(PlayTags.BOARD).deflate(2f).contains(f))
            kit.release()
            val last = i == order.lastIndex
            if (last) {
                kit.await("$leg, ${p.id}: the last lock solves it (REQ-022)") { kit.shownState() == PuzzleState.SOLVED }
            } else {
                kit.awaitExists(PlayTags.boardPiece(target.piece), "$leg, ${p.id}: ${target.piece} locked")
            }
            tally.locks++
            expect(if (last) KIT_SOLVE else KIT_LOCK, if (last) "the last lock, which solves ${p.id}" else "the lock of ${target.piece}")
        }
        return tally
    }

    /**
     * A drop centre from which the engine's search locks [target], also from 0.03 units around it, with the finger (the drop centre
     * plus the measured finger − drawn-centre gap) over the board 4 dp in from its edge: the solution centre first, then points up
     * to 0.6 units higher and 0.2 units aside, within the lock distance (`PlayThroughKitTest.planDrop`, copied: TYPE-004).
     */
    private fun kitPlanDrop(p: PuzzleDefinition, target: Placement, offset: Offset): Point? {
        val placedNow = kit.onController { placed.toList() }
        val dpPerUnit = kit.onController { boardDpPerUnit }
        val board = kit.bounds(PlayTags.BOARD)
        val inset = kit.dp(4f)
        val inner = Rect(board.left + inset, board.top + inset, board.right - inset, board.bottom - inset)
        val mapping = kit.mapping()
        val around = listOf(0.0 to 0.0, 0.03 to 0.0, -0.03 to 0.0, 0.0 to 0.03, 0.0 to -0.03)
        for (dy in (0..6).map { -0.1 * it }) {
            for (dx in listOf(0.0, 0.1, -0.1, 0.2, -0.2)) {
                val c = Point(target.centre.x + dx, target.centre.y + dy)
                if (!inner.contains(mapping.toPx(c) + offset)) continue
                val locks = around.all { (ex, ey) ->
                    val lock = kitAppEngine.findLock(p, placedNow, target.piece, target.turn, target.mirrored, Point(c.x + ex, c.y + ey), dpPerUnit)
                    lock is LockResult.Locked && kitAppSame(lock.placement, target)
                }
                if (locks) return c
            }
        }
        return null
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Two New puzzles to drive by touch: minis or warm-ups (few pieces, quick), in ‹ › order, each with at least two pieces and no
     * mirrored parallelogram in its solution (no badge press, whose event S-4 does not name). None is a precondition failure.
     */
    private fun kitTwoPuzzles(what: String): Pair<PuzzleDefinition, PuzzleDefinition> {
        val ids = kit.library.puzzles.map { it.id }
        val states = kit.onController { ids.associateWith { stateOf(it) } }
        val fit = kit.library.puzzles.filter {
            (it.kind == PuzzleKind.MINI || it.kind == PuzzleKind.WARMUP) && it.pieces.size >= 2 && states[it.id] == PuzzleState.NEW &&
                (PieceId.PG !in it.pieces || !kitAppPose(it, PieceId.PG).mirrored)
        }
        if (fit.size < 2) throw AssertionError("kit: precondition ($what): two New mini or warm-up puzzles without a mirrored parallelogram; found ${fit.map { it.id }}")
        return fit[0] to fit[1]
    }

    /** A still tap at the centre of [id]'s grid cell, scrolled wholly into view first; the grid then closes on [id] (REQ-050). */
    private fun kitPressCell(id: String, leg: String) {
        val tag = KitAppTag.overviewCell(id)
        kit.scrollIntoView(tag, "$leg: $id's cell")
        assertTrue("$leg: `$tag` wholly in view before the press", kit.wholeInView(tag))
        kit.pressCentre(tag)
        kit.await("$leg: the cell closes the grid and opens $id") { !kit.exists(KitAppTag.OVERVIEW) && kit.shownId() == id }
    }

    private fun kitAssertNothing(rec: KitFeedbackRecorder, what: String) {
        assertEquals("$what: no sound, tick or click reaches the door (REQ-033 A1; E8a)", emptyList<String>(), rec.take())
    }

    private fun kitToggle(tag: String): ToggleableState =
        kit.node(tag).config.getOrNull(SemanticsProperties.ToggleableState) ?: throw AssertionError("kit: `$tag` carries no toggle state (S-49)")
}
