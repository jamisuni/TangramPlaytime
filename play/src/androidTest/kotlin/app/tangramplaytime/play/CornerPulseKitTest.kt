// Acceptance kit (WO-003, TASK-011): the corner pulse of a mini puzzle (tasks.md § WO-003 › Seams S-2
// `drop` / `goHome`, S-3 `Pulse`, the tag table's `corner-pulse` and `corner-marker` rows; decisions.md
// "When the REQ-051 pulse fires"). Every drop is by touch on `play-screen`; the pulse's timing is driven
// through `mainClock`; the markers' CentreKey is read against their bounds through BoardMapping.
package app.tangramplaytime.play

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.hypot

@RunWith(AndroidJUnit4::class)
class CornerPulseKitTest {

    // The ComponentActivity host that `createComposeRule()` also uses (it is this very call), declared so the
    // REQ-051 A1 test can recreate the activity (code review F2).
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val kit by lazy { KitScreen(rule) }

    @Test // REQ-051.A1
    fun inAMiniPuzzleADropInTheMiddleOfTheSilhouetteSendsThePieceHomeAndTheCornersPulseOnce() {
        // REQ-051 A1: "In a mini puzzle, a drop in the middle of the silhouette sends the piece home and the
        // corners pulse once." Rule: "The pulse lasts about 600 ms and shows only the outline corners, never a
        // piece position." Oracle, by touch on the first (mini) puzzle: a piece released in the middle of the
        // silhouette, where the engine finds no lock, goes back to its cell; `corner-pulse` appears with one
        // `corner-marker` per outline corner of the engine (each centred on its corner, read against its
        // bounds), something is drawn there, and the pulse is gone by one second and does not come back.
        // "Once" also holds across an activity recreation (code review F2; the corrected check of this test, which
        // watched one composition only): the activity is recreated with the same controller — what a rotation or
        // any configuration change does — and the new composition shows no `corner-pulse` and no `corner-marker`
        // from its first frame through the next second (longer than the pulse's about 600 ms).
        kit.show()
        val p = kit.puzzle
        assertEquals("the fresh start is a mini puzzle", PuzzleKind.MINI, p.kind)
        val piece = p.pieces.first()
        val cell = kit.bounds(PlayTags.trayCell(piece))
        val middle = middleWithNoLock(p, piece)
        var finger = kit.pickUpFromTray(piece)
        finger = kit.steerDragTo(piece, middle, finger)
        assertFalse("no pulse before the release", kit.exists(PlayTags.CORNER_PULSE))
        kit.release()
        // The pulse.
        assertTrue("`corner-pulse` on the release", kit.exists(PlayTags.CORNER_PULSE))
        val corners = kitEngine.outlineCorners(p)
        assertMarkersOnTheCorners(p, corners)
        kit.advance(100)
        assertTrue("pulse present at 100 ms", kit.exists(PlayTags.CORNER_PULSE))
        kit.advance(200)
        assertTrue("pulse present at 300 ms", kit.exists(PlayTags.CORNER_PULSE))
        assertMarkersOnTheCorners(p, corners)
        assertSomethingDrawnAtTheMarkers()
        // Home meanwhile (REQ-020: the 180 ms return).
        assertEquals("the piece is home", listOf(PlayTags.trayPiece(piece)), kit.homesOf(piece))
        val box = kit.bounds(PlayTags.trayPiece(piece))
        assertTrue("the miniature $box is inside its cell $cell", cell.contains(box.topLeft) && cell.contains(box.bottomRight))
        // Once: gone by a second, and no second pulse.
        kit.advance(700)
        assertFalse("pulse gone at 1000 ms", kit.exists(PlayTags.CORNER_PULSE))
        assertFalse("markers gone at 1000 ms", kit.exists(PlayTags.CORNER_MARKER))
        for (t in 1..6) {
            kit.advance(250)
            assertFalse("no second pulse at ${1000 + t * 250} ms", kit.exists(PlayTags.CORNER_PULSE))
        }
        // Once, across a recreation (F2). The recreated screen is live — the same puzzle, the piece still home —
        // so a missing node is not a missing screen.
        kit.recreate(rule.activityRule.scenario)
        assertEquals("the recreated screen shows the same puzzle", p.id, kit.key(PlayTags.SCREEN, PlaySemantics.PuzzleIdKey))
        assertEquals("the piece is still home after the recreation", listOf(PlayTags.trayPiece(piece)), kit.homesOf(piece))
        for (f in 0..63) {
            val pulse = kit.exists(PlayTags.CORNER_PULSE)
            val markers = kit.count(PlayTags.CORNER_MARKER)
            assertFalse(
                "F2 pulse — REQ-051 A1 \"the corners pulse once\": the pulse of the one drop is shown again after the activity " +
                    "was recreated, frame $f of the new composition (${f * 16} ms; `corner-pulse` $pulse, $markers `corner-marker`)",
                pulse || markers > 0,
            )
            kit.frames()
        }
    }

    @Test // REQ-051.A2
    fun inAWarmUpTheSameDropSendsThePieceHomeAndNothingElseIsShown() {
        // REQ-051 A2: "In a warm-up, the same drop sends the piece home and nothing else is shown." Rule: "It
        // happens only in mini puzzles; full-set and warm-up puzzles never show anchors." Oracle, by touch on
        // the first warm-up: the same middle drop; from the release through the next second, no `corner-pulse`
        // or `corner-marker` node, the texts and the tagged nodes (apart from the dropped piece's own) are the
        // same as before the drag, and the piece is back in its cell.
        kit.show()
        val p = kitFirst(PuzzleKind.WARMUP)
        kit.open(p.id)
        val piece = PieceId.ST1
        val cell = kit.bounds(PlayTags.trayCell(piece))
        val own = setOf(PlayTags.trayPiece(piece), PlayTags.trayGhost(piece), PlayTags.dragPiece(piece))
        val (tagsBefore, textsBefore) = kit.tagsAndTexts()
        val middle = middleWithNoLock(p, piece)
        var finger = kit.pickUpFromTray(piece)
        finger = kit.steerDragTo(piece, middle, finger)
        kit.release()
        var t = 0L
        while (t <= 1000L) {
            assertFalse("no `corner-pulse` at $t ms", kit.exists(PlayTags.CORNER_PULSE))
            assertFalse("no `corner-marker` at $t ms", kit.exists(PlayTags.CORNER_MARKER))
            val (tags, texts) = kit.tagsAndTexts()
            assertEquals("texts at $t ms", textsBefore, texts)
            assertEquals("tagged nodes at $t ms", tagsBefore.filter { it !in own }, tags.filter { it !in own })
            kit.advance(100)
            t += 100
        }
        assertEquals("the piece is home", listOf(PlayTags.trayPiece(piece)), kit.homesOf(piece))
        val box = kit.bounds(PlayTags.trayPiece(piece))
        assertTrue("the miniature $box is inside its cell $cell", cell.contains(box.topLeft) && cell.contains(box.bottomRight))
        assertEquals("tagged nodes once the piece is home", tagsBefore, kit.tagsAndTexts().first)
    }

    @Test // pin: REQ-051 rule "With reduced motion enabled, the corners are shown for 600 ms without animation"; decisions.md checkpoint item 1 "pulse markers small circles in the accent"; no token
    fun withReducedMotionTheCornersAreShownWithoutAnimation() {
        kit.show(reducedMotion = true)
        val p = kit.puzzle
        val piece = p.pieces.first()
        val middle = middleWithNoLock(p, piece)
        var finger = kit.pickUpFromTray(piece)
        finger = kit.steerDragTo(piece, middle, finger)
        kit.release()
        val corners = kitEngine.outlineCorners(p)
        for (t in listOf(50L, 200L, 200L)) {
            kit.advance(t)
            assertTrue("markers shown", kit.exists(PlayTags.CORNER_MARKER))
            assertMarkersOnTheCorners(p, corners)
            val board = kit.capture(PlayTags.BOARD)
            val origin = kit.bounds(PlayTags.BOARD).topLeft
            for (c in corners) {
                val px = kit.toPx(c) - origin
                var accent = 0
                for (dy in -2..2) for (dx in -2..2) {
                    val x = (px.x.toInt() + dx).coerceIn(0, board.width - 1)
                    val y = (px.y.toInt() + dy).coerceIn(0, board.height - 1)
                    if (kitNear(board[x, y], PlayPalette.accent, 24f / 255f)) accent++
                }
                assertTrue("an accent-coloured marker at the corner $c ($accent px)", accent >= 5)
            }
        }
        kit.advance(600)
        assertFalse("markers gone after about 600 ms", kit.exists(PlayTags.CORNER_MARKER))
    }

    @Test // pin: decisions.md "the pulse fires on every go-home release in a mini (all three REQ-020 cases) and never on a cancel"; no token
    fun aReleaseOnTheTrayPulsesInAMiniAndACancelDoesNot() {
        kit.show()
        val p = kit.puzzle
        val piece = p.pieces.first()
        val other = p.pieces.last()
        var finger = kit.pickUpFromTray(piece)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.trayCell(other)).center, 4)
        kit.release()
        assertTrue("a release on the tray pulses in a mini", kit.exists(PlayTags.CORNER_PULSE))
        kit.advance(1500)
        assertFalse("the pulse is over", kit.exists(PlayTags.CORNER_PULSE))
        finger = kit.pickUpFromTray(piece)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 4)
        kit.touch { cancel() }
        kit.frames()
        assertFalse("a cancelled drag does not pulse", kit.exists(PlayTags.CORNER_PULSE))
        kit.advance(300)
        assertFalse("still no pulse after the return", kit.exists(PlayTags.CORNER_PULSE))
        assertEquals("the piece is home", listOf(PlayTags.trayPiece(piece)), kit.homesOf(piece))
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    private fun middleWithNoLock(p: PuzzleDefinition, piece: PieceId): Point {
        val turn = kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey)
        val mirrored = kit.key(PlayTags.trayPiece(piece), PlaySemantics.MirroredKey)
        return kitMiddleWithNoLock(p, piece, turn, mirrored, kit.onMain { kit.controller.boardDpPerUnit })
    }

    /** One marker per outline corner, each carrying its corner and centred on it (bounds through BoardMapping). */
    private fun assertMarkersOnTheCorners(p: PuzzleDefinition, corners: List<Point>) {
        val markers = kit.nodes(PlayTags.CORNER_MARKER)
        assertEquals("${p.id}: one marker per outline corner", corners.size, markers.size)
        val origin = kit.screenOrigin()
        val seen = ArrayList<Point>()
        for (m in markers) {
            val centre = m.config.getOrNull(PlaySemantics.CentreKey) ?: throw AssertionError("a marker without CentreKey")
            val corner = corners.firstOrNull { abs(it.x - centre.x) <= KIT_TOL && abs(it.y - centre.y) <= KIT_TOL }
                ?: throw AssertionError("${p.id}: marker centre $centre is not an outline corner of the engine: $corners")
            assertTrue("${p.id}: each corner marked once ($corner)", seen.none { abs(it.x - corner.x) <= KIT_TOL && abs(it.y - corner.y) <= KIT_TOL })
            seen += corner
            val expected = kit.toPx(corner)
            val actual = m.boundsInRoot.translate(-origin).center
            assertTrue("${p.id}: marker bounds centred on the corner $corner: expected $expected, was $actual", hypot(expected.x - actual.x, expected.y - actual.y) <= 2f)
        }
    }

    /** Pixels inside each marker's bounds that are neither the silhouette nor the board colour. */
    private fun assertSomethingDrawnAtTheMarkers() {
        val board = kit.capture(PlayTags.BOARD)
        val origin = kit.bounds(PlayTags.BOARD).topLeft
        for (m in kit.nodes(PlayTags.CORNER_MARKER)) {
            val b = m.boundsInRoot.translate(-kit.screenOrigin()).translate(-origin)
            var ink = 0
            for (y in b.top.toInt()..b.bottom.toInt()) for (x in b.left.toInt()..b.right.toInt()) {
                if (x !in 0 until board.width || y !in 0 until board.height) continue
                val c = board[x, y]
                if (!kitNear(c, PlayPalette.silhouette, 12f / 255f) && !kitNear(c, PlayPalette.board, 12f / 255f) && !kitNear(c, PlayPalette.paper, 12f / 255f)) ink++
            }
            assertTrue("something is drawn at the marker $b ($ink px)", ink >= 4)
        }
    }
}
