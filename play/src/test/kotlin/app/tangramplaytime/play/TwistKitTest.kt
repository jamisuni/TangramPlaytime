// Acceptance kit (WO-003, TASK-009) — the gesture arithmetic on the JVM: the two-finger twist (tasks.md
// § WO-003 › Seams S-7 `TwistTracker`, fed into S-2 `twistDrag` of a dragging PlayController) and the
// tap/drag split (S-8 `TouchClassifier`).
//
// - Tokens: REQ-017 A1 and A2, each on the one test whose assertion is that line's meaning
//   (traceability-rules.md §2). The other tests pin REQ-017's rules and the S-7 / S-8 rows and carry no
//   token; the two-finger gesture on the screen is the device kit's (TASK-011).
// - A twist is fed as the screen feeds it: the second finger's angle around the first, as atan2 reports
//   it (degrees in (−180, 180], y down so positive = clockwise; S-7), in samples at most 3° apart, and
//   each change of the net step count goes to `twistDrag` as the difference (S-2: "steps 45° steps of
//   the dragged piece, positive = clockwise").
// - A twist ending exactly on a step boundary is TASK-010's recorded decision (S-7); no test ends there.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

class TwistKitTest {

    /** Second-finger starting angles, including ones whose twist crosses ±180°. */
    private val kitStarts: List<Double> = listOf(0.0, 30.0, 135.0, 170.0, -100.0, -179.0)

    /** [deg] wrapped into (−180, 180], as atan2 reports a finger's angle. */
    private fun kitWrap(deg: Double): Double {
        var a = deg % 360.0
        if (a <= -180.0) a += 360.0
        if (a > 180.0) a -= 360.0
        return a
    }

    /** The second finger's angles for a twist from [startDeg] by [byDeg], at most 3° apart, wrapped. */
    private fun kitSamples(startDeg: Double, byDeg: Double): List<Double> {
        val n = max(1, ceil(abs(byDeg) / 3.0).toInt())
        return (1..n).map { k -> kitWrap(startDeg + byDeg * k / n) }
    }

    /**
     * Feeds [angles] to [tracker] and, when [c] is given, each change of the net steps to `c.twistDrag` as the
     * difference from the last net. Returns the net after each sample.
     */
    private fun kitFeed(tracker: TwistTracker, angles: List<Double>, c: PlayController?): List<Int> {
        var last = 0
        return angles.map { a ->
            val net = tracker.steps(a)
            if (c != null && net != last) c.twistDrag(net - last)
            last = net
            net
        }
    }

    /** A twist of [byDeg] from [startDeg] with a fresh tracker (a second finger that has just touched). */
    private fun kitTwist(startDeg: Double, byDeg: Double, c: PlayController?): List<Int> =
        kitFeed(TwistTracker(kitWrap(startDeg)), kitSamples(startDeg, byDeg), c)

    /** [t] moved by [steps] 45° steps, positive = `Turn.next` (Turn KDoc). */
    private fun kitTurned(t: Turn, steps: Int): Turn {
        var r = t
        repeat(abs(steps)) { r = if (steps > 0) r.next() else r.previous() }
        return r
    }

    /** A controller on the first listed puzzle with its first piece dragged from the tray. */
    private fun kitDragging(events: MutableList<PlayEvent>? = null): PlayController {
        val c = kitController(events)
        assertTrue("startDrag from the tray", c.startDrag(c.puzzle.pieces.first(), KIT_TRAY_POINT))
        events?.clear()
        return c
    }

    // ── REQ-017 A1 and A2 ──────────────────────────────────────────────────────────────────────

    // REQ-017.A1 — "A 90° twist turns the piece exactly two steps." For each starting angle and both
    // directions: the tracker's net after a 90° twist is exactly two steps, the dragged piece's turn is
    // exactly two 45° steps from where it was (clockwise for a clockwise twist), and S-7's own reading
    // ("90° → 2, −90° → −2") holds for the twist reported at once.
    @Test
    fun aNinetyDegreeTwistTurnsTheDraggedPieceExactlyTwoSteps() { // REQ-017.A1
        for (start in kitStarts) {
            for (dir in listOf(1, -1)) {
                val tag = "a ${90 * dir}° twist from $start°"
                val c = kitDragging()
                val before = c.dragged?.turn ?: throw AssertionError("$tag: nothing dragged")
                val nets = kitTwist(start, 90.0 * dir, c)
                assertEquals("$tag: net steps", 2 * dir, nets.last())
                assertEquals("$tag: the dragged piece's turn", kitTurned(before, 2 * dir), c.dragged?.turn)
                val atOnce = TwistTracker(kitWrap(start)).steps(kitWrap(start + 90.0 * dir))
                assertEquals("$tag, reported at once", 2 * dir, atOnce)
            }
        }
    }

    // REQ-017.A2 — "A 20° twist does not turn the piece." For each starting angle and both directions: the
    // net is zero at every sample of the twist, the dragged piece's turn never changes, and no Turned event
    // is emitted (S-2 `twistDrag`: "one Turned per step").
    @Test
    fun aTwentyDegreeTwistDoesNotTurnThePiece() { // REQ-017.A2
        for (start in kitStarts) {
            for (dir in listOf(1, -1)) {
                val tag = "a ${20 * dir}° twist from $start°"
                val events = mutableListOf<PlayEvent>()
                val c = kitDragging(events)
                val before = c.dragged?.turn ?: throw AssertionError("$tag: nothing dragged")
                val nets = kitTwist(start, 20.0 * dir, c)
                assertTrue("$tag: net steps $nets", nets.all { it == 0 })
                assertEquals("$tag: the dragged piece's turn", before, c.dragged?.turn)
                assertEquals("$tag: events", emptyList<String>(), events.map(::kitEventText))
                assertEquals("$tag, reported at once", 0, TwistTracker(kitWrap(start)).steps(kitWrap(start + 20.0 * dir)))
            }
        }
    }

    // ── REQ-017's rules and the S-7 row (pins) ─────────────────────────────────────────────────

    // REQ-017 rule — "Step boundaries are at ±22.5°, ±67.5° and so on of twist from where the second finger
    // started"; statement — "one 45° step each time the twist angle passes a further 45° step boundary";
    // S-7 — "the net 45° steps …, unwrapped across ±180°". Twists 1° short of and 1° past each boundary, in
    // both directions, up to past a full turn. Pin, no token.
    @Test
    fun theStepCountChangesOnlyAtTheBoundaries() {
        for (start in kitStarts) {
            for (k in 0..8) {
                val boundary = 22.5 + 45.0 * k
                for (dir in listOf(1, -1)) {
                    val short = kitTwist(start, dir * (boundary - 1.0), null).last()
                    val past = kitTwist(start, dir * (boundary + 1.0), null).last()
                    assertEquals("from $start°, ${dir * (boundary - 1.0)}°", dir * k, short)
                    assertEquals("from $start°, ${dir * (boundary + 1.0)}°", dir * (k + 1), past)
                }
            }
        }
    }

    // S-7 — "the net 45° steps": a twist that goes 100° one way and comes back to 10° has net zero; one that
    // goes on round through a full turn (360°) has eight. Pin, no token.
    @Test
    fun theStepCountIsNetAndUnwrapped() {
        for (start in kitStarts) {
            val tracker = TwistTracker(kitWrap(start))
            val there = kitFeed(tracker, kitSamples(start, 100.0), null)
            assertEquals("from $start°, out to 100°", 2, there.last())
            val back = kitFeed(tracker, kitSamples(start + 100.0, -90.0), null)
            assertEquals("from $start°, back to 10°", 0, back.last())
            assertEquals("from $start°, a full turn", 8, kitTwist(start, 360.0, null).last())
            assertEquals("from $start°, a full turn the other way", -8, kitTwist(start, -360.0, null).last())
        }
    }

    // REQ-017 rule — "Lifting the second finger keeps the turn reached; the drag continues." S-2 `twistDrag`:
    // "one Turned per step", for the dragged piece. A second twist is a new second finger (a new tracker,
    // counting from where it touched). Pin, no token.
    @Test
    fun liftingTheSecondFingerKeepsTheTurnAndEachStepEmitsOneTurned() {
        val events = mutableListOf<PlayEvent>()
        val c = kitDragging(events)
        val piece = c.dragged?.piece ?: throw AssertionError("nothing dragged")
        val before = c.dragged!!.turn
        kitTwist(40.0, 135.0, c)
        assertEquals("three steps clockwise", kitTurned(before, 3), c.dragged?.turn)
        assertEquals(List(3) { "Turned $piece" }, events.map(::kitEventText))
        events.clear()
        // The finger lifts; the drag continues, with the turn reached. A new finger twists 20°: nothing.
        assertNotNull("the drag continues", c.dragged)
        kitTwist(-60.0, 20.0, c)
        assertEquals("the turn reached is kept", kitTurned(before, 3), c.dragged?.turn)
        assertEquals(emptyList<String>(), events.map(::kitEventText))
        // Another finger twists 90° the other way: two steps back.
        kitTwist(10.0, -90.0, c)
        assertEquals("one step clockwise net", kitTurned(before, 1), c.dragged?.turn)
        assertEquals(List(2) { "Turned $piece" }, events.map(::kitEventText))
    }

    // ── S-8 — tap or drag (pins) ───────────────────────────────────────────────────────────────

    // seam S-8 — "The touch is a drag from the move at which it has moved ≥ the slop from its down point
    // (true from then on). There is no time input, so a touch that ends without becoming a drag is a tap
    // whatever its duration (REQ-015)"; `TAP_SLOP_DP = 12f` (REQ-015: a tap has "moved < 12 dp"). The
    // classifier gets the slop in px: 12 dp at densities 1 and 2.625. Pin, no token.
    @Test
    fun aTouchBecomesADragAtTwelveDpFromItsDownPointAndStaysOne() {
        assertEquals(12f, TAP_SLOP_DP)
        for (density in listOf(1f, 2.625f)) {
            val slop = TAP_SLOP_DP * density
            val x0 = 40f
            val y0 = 60f
            val t = TouchClassifier(slop)
            t.down(x0, y0)
            assertFalse("density $density: a touch that has not moved", t.isDrag)
            // Many moves, all less than the slop from the down point (0.99 × slop at most): still a tap.
            for (k in 0 until 200) {
                val f = 0.7f * (k % 7) / 6f
                val moved = t.move(x0 + f * slop * (if (k % 2 == 0) 1f else -1f), y0 + f * slop)
                assertFalse("density $density: move $k within the slop", moved)
                assertFalse("density $density: move $k within the slop", t.isDrag)
            }
            assertFalse("density $density: back on the down point", t.move(x0, y0))
            // Exactly the slop away: a drag (≥).
            assertTrue("density $density: a move of exactly the slop", t.move(x0 + slop, y0))
            assertTrue(t.isDrag)
            // From then on it stays a drag, also back on the down point.
            t.move(x0, y0)
            assertTrue("density $density: still a drag back on the down point", t.isDrag)

            // A new touch starts as a tap.
            t.down(x0 + 100f, y0 + 100f)
            assertFalse("density $density: a new down", t.isDrag)
            assertFalse("density $density: 11.88 dp diagonally", t.move(x0 + 100f + 0.7f * slop, y0 + 100f + 0.7f * slop))
            assertFalse(t.isDrag)
            assertTrue("density $density: past the slop", t.move(x0 + 100f, y0 + 100f - 1.01f * slop))
            assertTrue(t.isDrag)
        }
    }
}
