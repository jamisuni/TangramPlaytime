// Acceptance kit (WO-003, TASK-011): the solved picture (tasks.md § WO-003 › Seams S-2 `drop` → Solved,
// the tag table's `solved-picture`, `confetti` and `board-piece` rows; decisions.md "the solve pop is
// size-neutral"). The picture lines are set up through S-2 (the lines are not about the gesture) over every
// bundled puzzle, sampled after the fade with reduced motion (brief); REQ-045 A2 places the three pieces
// by touch and judges the solve by `solved-picture`, never by the last piece's node.
package app.tangramplaytime.play

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SolvedPictureKitTest {

    @get:Rule
    val rule = createComposeRule()

    private val kit by lazy { KitScreen(rule) }

    // ── REQ-023: the solved picture ────────────────────────────────────────────────────────────────────

    @Test // REQ-023.A1
    fun thePicturesOutlineIsIdenticalToTheSilhouettes() {
        // REQ-023 A1: "The picture's outline is identical to the silhouette's." Statement: the picture is
        // "drawn with exactly the silhouette's outline"; ArtLayer KDoc: "a base colour filling the silhouette,
        // then shapes drawn in order on top". Oracle, over every bundled puzzle, after the fade (reduced
        // motion: no confetti): outside the silhouette (3 px out) the board is pixel for pixel what it was
        // unsolved; inside it (3 px in) no pixel is the silhouette colour any more and none is the bare board
        // (unless the picture itself names white); and the `solved-picture` node's box is the `silhouette`
        // node's box (tag table). How much of the base colour stays visible under the shapes is the
        // picture's own business (a logged number, not an assertion).
        kit.show(reducedMotion = true)
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            val unsolved = kit.capture(PlayTags.BOARD)
            val mask = kit.silhouetteMask(p)
            val inside = mask.erode(3)
            val outside = mask.dilate(3).not()
            val silhouetteBox = kit.bounds(PlayTags.SILHOUETTE)
            kit.solveByApi()
            assertEquals("${p.id}: solved", PuzzleState.SOLVED, kit.key(PlayTags.SCREEN, PlaySemantics.PuzzleStateKey))
            kit.advance(1700)
            assertTrue("${p.id}: `solved-picture` after the fade", kit.exists(PlayTags.SOLVED_PICTURE))
            kitAssertBoxNear("${p.id}: `solved-picture` box vs the silhouette's box", silhouetteBox, kit.bounds(PlayTags.SOLVED_PICTURE), 2f)
            val solved = kit.capture(PlayTags.BOARD)
            assertSameSize(p, unsolved, solved)
            assertNothingOutside(p, unsolved, solved, outside, "after the fade")
            val artHasWhite = kitArtColours(p.art).any { kitNear(it, Color.White, 3f / 255f) }
            val base = kitColour(p.art.base)
            var silhouetteLeft = 0
            var bareBoard = 0
            var baseSeen = 0
            var total = 0
            var first = ""
            inside.forEachSet { x, y ->
                total++
                val c = solved[x, y]
                if (kitNear(c, PlayPalette.silhouette, 6f / 255f)) { silhouetteLeft++; if (first.isEmpty()) first = "($x, $y)" }
                if (!artHasWhite && kitNear(c, PlayPalette.board, 6f / 255f)) { bareBoard++; if (first.isEmpty()) first = "($x, $y)" }
                if (kitNear(c, base, 6f / 255f)) baseSeen++
            }
            assertEquals("${p.id}: silhouette-coloured pixels left inside the picture; first at $first", 0, silhouetteLeft)
            assertEquals("${p.id}: bare board pixels inside the picture; first at $first", 0, bareBoard)
            android.util.Log.i("KitScreen", "${p.id}: base colour ${p.art.base} visible on $baseSeen of $total px inside the outline")
        }
    }

    @Test // REQ-023.A2
    fun noPieceBoundaryIsVisibleInThePicture() {
        // REQ-023 A2: "No piece boundary is visible in the picture." Statement: the picture replaces "the
        // coloured pieces". Oracle, over every bundled puzzle after the fade: no pixel inside the picture is a
        // TYPE-001 piece colour the picture itself does not name; and along every edge two solution pieces
        // share, away from the picture's own ink, a pixel on the edge is the colour of the pixels 3 px to
        // either side whenever those two agree — a drawn piece outline there would differ.
        kit.show(reducedMotion = true)
        val pieceColours = kitPieceColours()
        var samplesInAll = 0
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            val inside = kit.silhouetteMask(p).erode(3)
            kit.solveByApi()
            kit.advance(1700)
            assertTrue("${p.id}: `solved-picture` after the fade", kit.exists(PlayTags.SOLVED_PICTURE))
            val solved = kit.capture(PlayTags.BOARD)
            val artColours = kitArtColours(p.art)
            val foreign = pieceColours.filterValues { pc -> artColours.none { kitNear(it, pc, 6f / 255f) } }
            var pieceColoured = 0
            var first = ""
            inside.forEachSet { x, y ->
                val c = solved[x, y]
                for ((id, pc) in foreign) {
                    if (kitNear(c, pc, 6f / 255f)) { pieceColoured++; if (first.isEmpty()) first = "($x, $y) = ${kitHex(c)} ($id)" }
                }
            }
            assertEquals("${p.id}: piece-coloured pixels inside the picture; first at $first", 0, pieceColoured)

            val ink = kitArtInk(p.art)
            val origin = kit.bounds(PlayTags.BOARD).topLeft
            val pxPerUnit = kit.pxPerUnit()
            val stepUnits = 4.0 / pxPerUnit
            var checked = 0
            var boundaries = 0
            var firstBoundary = ""
            for (seg in kitInternalEdges(p)) {
                val n = kitNormal(seg)
                for (q in kitSamplesAlong(seg, stepUnits)) {
                    if (kitInkDistance(ink, q) <= 3.5 / pxPerUnit) continue
                    val on = kit.toPx(q) - origin
                    val side = 3.0 / pxPerUnit
                    val plus = kit.toPx(Point(q.x + n.x * side, q.y + n.y * side)) - origin
                    val minus = kit.toPx(Point(q.x - n.x * side, q.y - n.y * side)) - origin
                    val xo = on.x.toInt(); val yo = on.y.toInt()
                    val xp = plus.x.toInt(); val yp = plus.y.toInt()
                    val xm = minus.x.toInt(); val ym = minus.y.toInt()
                    if (!inside[xo, yo] || !inside[xp, yp] || !inside[xm, ym]) continue
                    val cp = solved[xp, yp]
                    val cm = solved[xm, ym]
                    if (!kitNear(cp, cm, 6f / 255f)) continue
                    checked++
                    if (!kitNear(solved[xo, yo], cp, 8f / 255f)) {
                        boundaries++
                        if (firstBoundary.isEmpty()) firstBoundary = "($xo, $yo) = ${kitHex(solved[xo, yo])} between ${kitHex(cp)} and ${kitHex(cm)} on $seg"
                    }
                }
            }
            assertTrue("${p.id}: the fixture checks the shared edges ($checked samples)", checked >= 10)
            assertEquals("${p.id}: boundary pixels on shared piece edges; first at $firstBoundary", 0, boundaries)
            samplesInAll += checked
        }
        assertTrue("the shared edges are sampled across the library ($samplesInAll)", samplesInAll >= 200)
    }

    @Test // pin: tag table `board-piece` "until the picture is fully shown"; REQ-023 rule "no confetti" with reduced motion; no token
    fun thePiecesAreGoneOnceThePictureIsFullyShownAndThereIsNoConfettiWithReducedMotion() {
        kit.show(reducedMotion = true)
        val p = kitFirst(PuzzleKind.MINI)
        kit.open(p.id)
        kit.solveByApi()
        for (t in listOf(0L, 300L, 600L, 1000L)) {
            kit.advance(if (t == 0L) 0L else 300L)
            assertFalse("no confetti with reduced motion at $t ms", kit.exists(PlayTags.CONFETTI))
        }
        kit.advance(1000)
        assertTrue("`solved-picture` once fully shown", kit.exists(PlayTags.SOLVED_PICTURE))
        for (piece in p.pieces) assertFalse("$piece's board node gone once the picture is fully shown", kit.exists(PlayTags.boardPiece(piece)))
        for (piece in p.pieces) assertFalse("$piece's cell gone once solved (tag table: the puzzle is unsolved)", kit.exists(PlayTags.trayCell(piece)))
    }

    @Test // pin: REQ-023 rules "The picture fades in over 800 ms, starting 600 ms after the last piece locks" and "confetti lasting 2.4 s"; decisions.md "the solve pop is size-neutral" (REQ-014 A2 holds through the solve); no token
    fun theFadeStartsAtSixHundredMsThePopKeepsTheSizeAndTheConfettiLastsTwoPointFourSeconds() {
        kit.show()
        val p = kitFirst(PuzzleKind.MINI)
        kit.open(p.id)
        val board = kit.pxPerUnit()
        kit.solveByApi()
        for (t in listOf(0L, 100L, 200L, 300L, 400L, 500L)) {
            kit.advance(if (t == 0L) 0L else 100L)
            assertFalse("no `solved-picture` at $t ms, before the fade starts", kit.exists(PlayTags.SOLVED_PICTURE))
            for (piece in p.pieces) {
                val tag = PlayTags.boardPiece(piece)
                if (!kit.exists(tag)) continue
                kitAssertNear("$piece drawn scale at $t ms of the pop", board.toDouble(), kit.key(tag, PlaySemantics.DrawnPxPerUnitKey).toDouble(), 0.01 * board)
            }
        }
        kit.advance(150)
        assertTrue("`solved-picture` from the fade's start (600 ms)", kit.exists(PlayTags.SOLVED_PICTURE))
        assertTrue("confetti during the celebration", kit.exists(PlayTags.CONFETTI))
        kit.advance(1000)
        assertTrue("confetti still at 1.75 s", kit.exists(PlayTags.CONFETTI))
        kit.advance(1000)
        assertFalse("confetti over after 2.4 s", kit.exists(PlayTags.CONFETTI))
    }

    // ── REQ-039: the picture stays inside the outline ──────────────────────────────────────────────────

    @Test // REQ-039.A1
    fun noPicturePaintsOutsideItsPuzzlesOutline() {
        // REQ-039 A1: "No picture paints outside its puzzle's outline." ArtLayer KDoc: "whoever draws it clips
        // to the silhouette". Oracle, over every bundled puzzle, at the fade's start, mid-fade and after it:
        // every board pixel 3 px or more outside the silhouette is what it was before the solve.
        kit.show(reducedMotion = true)
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            val unsolved = kit.capture(PlayTags.BOARD)
            val outside = kit.silhouetteMask(p).dilate(3).not()
            kit.solveByApi()
            kit.advance(650)
            assertTrue("${p.id}: `solved-picture` at the fade's start", kit.exists(PlayTags.SOLVED_PICTURE))
            assertNothingOutside(p, unsolved, kit.capture(PlayTags.BOARD), outside, "at the fade's start")
            kit.advance(350)
            assertNothingOutside(p, unsolved, kit.capture(PlayTags.BOARD), outside, "mid-fade")
            kit.advance(700)
            assertNothingOutside(p, unsolved, kit.capture(PlayTags.BOARD), outside, "after the fade")
        }
    }

    // ── REQ-045: the three pieces by touch ─────────────────────────────────────────────────────────────

    @Test // REQ-045.A2
    fun placingTheThreePiecesShowsTheSolvedPicture() {
        // REQ-045 A2: "Placing those three pieces shows the solved picture." Oracle, by touch on the fresh
        // start's mini puzzle: each of its three pieces is turned by taps to its solution turn, dragged to its
        // solution centre and released; the solve is judged by the `solved-picture` node (displayed, over the
        // silhouette's box) and by the pixels once the celebration is over — no silhouette or piece colour is
        // left inside the outline — never by the last piece's own node.
        kit.show()
        val p = kit.puzzle
        assertEquals("the fresh start is a mini", PuzzleKind.MINI, p.kind)
        assertEquals("three pieces", 3, p.pieces.size)
        val silhouetteBox = kit.bounds(PlayTags.SILHOUETTE)
        val inside = kit.silhouetteMask(p).erode(3)
        val dp = kit.onMain { kit.controller.boardDpPerUnit }
        var remaining = p.pieces.toList()
        while (remaining.isNotEmpty()) {
            val next = remaining.firstOrNull { piece ->
                val pose = kitPoseOf(piece, p.solution.single { it.piece == piece }.polygon)
                kitEngine.findLock(p, kit.placed(), piece, pose.turn, pose.mirrored, pose.centre, dp) is LockResult.Locked
            } ?: throw AssertionError("kit: none of $remaining locks at its solution pose in ${p.id}")
            val pose = kitPoseOf(next, p.solution.single { it.piece == next }.polygon)
            check(!pose.mirrored) { "kit: a mini solution needs a mirrored piece" }
            kit.tapTray(next, turnsNeeded(next, pose.turn))
            var finger = kit.pickUpFromTray(next)
            finger = kit.steerDragTo(next, pose.centre, finger)
            kit.checkOverBoard(finger)
            kit.release()
            kit.advance(250)
            remaining = remaining - next
            assertFalse("no picture before the last piece", remaining.isNotEmpty() && kit.exists(PlayTags.SOLVED_PICTURE))
        }
        kit.advance(3000)
        assertTrue("`solved-picture` is shown", kit.exists(PlayTags.SOLVED_PICTURE))
        rule.onNodeWithTag(PlayTags.SOLVED_PICTURE, useUnmergedTree = true).assertIsDisplayed()
        kitAssertBoxNear("the picture covers the silhouette's box", silhouetteBox, kit.bounds(PlayTags.SOLVED_PICTURE), 2f)
        assertEquals("solved", PuzzleState.SOLVED, kit.key(PlayTags.SCREEN, PlaySemantics.PuzzleStateKey))
        val board = kit.capture(PlayTags.BOARD)
        val pieceColours = kitPieceColours().values
        var leftovers = 0
        var first = ""
        inside.forEachSet { x, y ->
            val c = board[x, y]
            if (kitNear(c, PlayPalette.silhouette, 6f / 255f) || pieceColours.any { kitNear(c, it, 6f / 255f) }) { leftovers++; if (first.isEmpty()) first = "($x, $y) = ${kitHex(c)}" }
        }
        assertEquals("silhouette or piece pixels left inside the outline; first at $first", 0, leftovers)
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    private fun turnsNeeded(piece: PieceId, target: Turn): Int {
        val now = kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey)
        return ((target.step - now.step) % 8 + 8) % 8
    }

    private fun assertSameSize(p: PuzzleDefinition, a: PixelMap, b: PixelMap) {
        assertEquals("${p.id}: board capture width", a.width, b.width)
        assertEquals("${p.id}: board capture height", a.height, b.height)
    }

    /** Every pixel of [outside] is unchanged between [before] and [after]. */
    private fun assertNothingOutside(p: PuzzleDefinition, before: PixelMap, after: PixelMap, outside: KitMask, moment: String) {
        assertSameSize(p, before, after)
        assertTrue("${p.id}: the outside fixture is not empty", outside.count() > 1000)
        var changed = 0
        var first = ""
        outside.forEachSet { x, y ->
            if (!kitNear(before[x, y], after[x, y], 3f / 255f)) { changed++; if (first.isEmpty()) first = "($x, $y): ${kitHex(before[x, y])} → ${kitHex(after[x, y])}" }
        }
        assertEquals("${p.id} $moment: pixels outside the outline changed by the picture; first at $first", 0, changed)
    }
}
