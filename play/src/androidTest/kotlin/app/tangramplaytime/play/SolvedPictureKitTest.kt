// Acceptance kit (WO-003, TASK-011): the solved picture (tasks.md § WO-003 › Seams S-2 `drop` → Solved,
// the tag table's `solved-picture`, `confetti` and `board-piece` rows; decisions.md "the solve pop is
// size-neutral"). The picture lines are set up through S-2 (the lines are not about the gesture) over every
// bundled puzzle, sampled after the fade with reduced motion (brief); REQ-045 A2 places the three pieces
// by touch and judges the solve by `solved-picture`, never by the last piece's node.
package app.tangramplaytime.play

import androidx.compose.ui.geometry.Offset
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
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

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
        // Amended WO-010 (F3, recirculation 6; a corrected check — decisions.md 2026-10-20 "F3's cause is the REQ-023 A1 leg's
        // inside mask, not the product or the data"): "3 px in" and "3 px out" are measured from the exact outline
        // (`kit.silhouetteBands`: pixel centres in the union at least 3 px from its outline, and centres outside it at least 3 px
        // from it), no longer as the solution polygons widened 0.75 px, then eroded or dilated by a 7 × 7 square. At a 315° notch
        // (a piece corner on the middle of another piece's edge) that mask filled the notch's tip and counted pixels 0.46 px from
        // the outline as "3 px in", where the picture's thin outline, stroked in the silhouette colour inside the clip, rightly
        // paints that colour (decisions.md 2026-10-12 "The solve, as drawn"); dev went red on animals-vulture (run log
        // t042b-dev, f3-repro). The outside set now starts at the 3 px it names (the dilated mask kept 3.8 px or more off an edge,
        // 5 px off a 45° one, and 11–13 px off a notch's tip). The rule and everything else the leg judges are unchanged; the
        // self-check below proves the sets.
        kit.show(reducedMotion = true)
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            val unsolved = kit.capture(PlayTags.BOARD)
            val bands = kit.silhouetteBands(p)
            val inside = bands.inside
            val outside = bands.outside
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
        // Amended WO-010 (F3, recirculation 6, a look-alike; a corrected check — decisions.md 2026-10-20 "F3's cause is the
        // REQ-023 A1 leg's inside mask, not the product or the data"): "inside the picture" is the 3 px-in set measured from the
        // exact outline (`kit.silhouetteBands`), no longer the widened mask eroded by a 7 × 7 square, which at a 315° notch held
        // pixels 0.46 px from the outline, on the picture's own silhouette-coloured outline stroke (decisions.md 2026-10-12 "The
        // solve, as drawn"): there an edge sample and its two side samples could read the stroke against the picture and count a
        // boundary that is none. Everything the leg judges is unchanged.
        kit.show(reducedMotion = true)
        val pieceColours = kitPieceColours()
        var samplesInAll = 0
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            val inside = kit.silhouetteBands(p).inside
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
            // Amended WO-010 (recirculation 3, a look-alike of F2; a corrected check — run time only): `BoardMappingKey` is read
            // once per puzzle, here on the solved board under the paused clock, instead of three semantics reads per sample
            // (`kit.toPx`, each waiting for a frame: at 106 puzzles the leg ran 30 min without ending). Nothing moves between the
            // samples, so every point maps as before.
            val mapping = kit.mapping()
            val pxPerUnit = mapping.pxPerUnit
            val stepUnits = 4.0 / pxPerUnit
            var checked = 0
            var boundaries = 0
            var firstBoundary = ""
            for (seg in kitInternalEdges(p)) {
                val n = kitNormal(seg)
                for (q in kitSamplesAlong(seg, stepUnits)) {
                    if (kitInkDistance(ink, q) <= 3.5 / pxPerUnit) continue
                    val on = mapping.toPx(q) - origin
                    val side = 3.0 / pxPerUnit
                    val plus = mapping.toPx(Point(q.x + n.x * side, q.y + n.y * side)) - origin
                    val minus = mapping.toPx(Point(q.x - n.x * side, q.y - n.y * side)) - origin
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
        // Amended WO-010 (F3, recirculation 6, a look-alike; a corrected check — decisions.md 2026-10-20 "F3's cause is the
        // REQ-023 A1 leg's inside mask, not the product or the data"): "3 px or more outside" is measured from the exact outline
        // (`kit.silhouetteBands`). The widened mask dilated by a 7 × 7 square never judged the pixels 3–3.8 px off an edge (to
        // 5 px off a 45° one), nor a 315° notch's tip out to 11–13 px (the exact set reaches in to 7.9 px), where a picture that
        // bulged past the outline would show (decisions.md 2026-10-12 "The solve, as drawn": bevel joins "so a concave 45° notch
        // is not bulged"): a hollow check there.
        kit.show(reducedMotion = true)
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            val unsolved = kit.capture(PlayTags.BOARD)
            val outside = kit.silhouetteBands(p).outside
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
        // Amended WO-010 (F3, recirculation 6, a look-alike; a corrected check — decisions.md 2026-10-20 "F3's cause is the
        // REQ-023 A1 leg's inside mask, not the product or the data"): "inside the outline" is the 3 px-in set measured from the
        // exact outline (`kit.silhouetteBands`), no longer the widened mask eroded by a 7 × 7 square, which at a 315° notch held
        // pixels 0.46 px from the outline, where the picture's own outline stroke rightly paints the silhouette colour.
        val inside = kit.silhouetteBands(p).inside
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

    // ── Kit self-check (no token): the 3 px sets the pixel legs read (WO-010 F3) ───────────────────────────────────────

    @Test // kit self-check, no token: decisions.md 2026-10-20 "F3's cause is the REQ-023 A1 leg's inside mask, not the product or the data" ("3 px in" and "3 px out" are measured from the exact outline) — the sets keep 3 px from the outline over the bundle, and the old construction is caught on animals-vulture and letters-z
    fun kitSelfCheckTheThreePxSetsKeepThreePxFromTheExactOutline() {
        // The legs' sets (`kit.silhouetteBands`) are read against an oracle of this check's own that never builds an outline
        // (`bandFaults`): from the solution polygons alone, a pixel is 3 px in when the open disc of radius 3 px round its centre
        // lies in the union of the pieces, and 3 px out when that disc meets no piece. Over every bundled puzzle, on the device's
        // own board mapping: no pixel of either set is closer than 3 px to the outline (where a false red would sit), and no
        // pixel 3.25 px or more from it is left out of its set (a hollow check). Shown to discriminate, in the same test: the old
        // construction (the solution polygons widened 0.75 px, eroded and dilated by a 7 × 7 square) fails it on
        // animals-vulture and letters-z, the two puzzles it turned red on dev (run log t042b-dev, f3-probe-*): its inside set
        // comes too close and its outside set leaves pixels out.
        kit.show(reducedMotion = true)
        val caught = linkedMapOf("animals-vulture" to "", "letters-z" to "")
        val faulty = ArrayList<String>()
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            val board = kit.bounds(PlayTags.BOARD)
            val polys = kitPolygonsPx(p, kit.mapping(), board.topLeft)
            val w = board.width.roundToInt()
            val h = board.height.roundToInt()
            val t0 = android.os.SystemClock.uptimeMillis()
            val bands = kit.silhouetteBands(p)
            val faults = bandFaults(polys, w, h, bands.inside, bands.outside)
            android.util.Log.i("KitScreen", "self-check ${p.id}: inside ${bands.inside.count()} px, outside ${bands.outside.count()} px; $faults (${android.os.SystemClock.uptimeMillis() - t0} ms)")
            if (!faults.none) faulty += "${p.id}: $faults"
            if (p.id in caught) {
                val old = kit.silhouetteMask(p)
                val oldFaults = bandFaults(polys, w, h, old.erode(3), old.dilate(3).not())
                android.util.Log.i("KitScreen", "self-check ${p.id}, the old construction: $oldFaults")
                assertTrue("kit self-check: ${p.id}: the old construction's inside set comes closer than 3 px to the outline; the oracle found $oldFaults", oldFaults.insideClose > 0)
                assertTrue("kit self-check: ${p.id}: the old construction's outside set leaves out pixels 3.25 px or more out; the oracle found $oldFaults", oldFaults.outsideLeftOut > 0)
                caught[p.id] = oldFaults.toString()
            }
        }
        assertTrue("kit self-check: the bundle holds both puzzles the old construction turned red: $caught", caught.values.none { it.isEmpty() })
        assertTrue("kit self-check: the 3 px sets against the disc oracle — ${faulty.size} of ${kitLibrary.puzzles.size} puzzles at fault: $faulty", faulty.isEmpty())
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** What the disc oracle finds wrong with a pair of 3 px sets: pixels closer than the margin, pixels the rule judges left out. */
    private class BandFaults {
        var insideClose = 0
        var outsideClose = 0
        var insideLeftOut = 0
        var outsideLeftOut = 0
        var first = ""
        val none: Boolean get() = insideClose + outsideClose + insideLeftOut + outsideLeftOut == 0

        fun note(what: String, x: Int, y: Int) { if (first.isEmpty()) first = "$what at ($x, $y)" }

        override fun toString(): String =
            "inside too close $insideClose, outside too close $outsideClose, inside left out $insideLeftOut, outside left out $outsideLeftOut" +
                if (first.isEmpty()) "" else "; first: $first"
    }

    /**
     * The self-check's oracle, which never builds an outline. With [polys] (the solution pieces in the `board` capture's px), a
     * pixel is [marginPx] in when the open disc of that radius round its centre lies in the union of the pieces, and
     * [marginPx] out when the disc meets no piece. Where no piece edge comes within [marginPx] + [slackPx] of a centre, the
     * centre decides (the disc lies in one piece or in none: every piece is far larger). Elsewhere the disc is read on its
     * circle, 96 points, and at the piece corners inside it: for "in", a corner on the union's outline (one of 32 points
     * 0.05 px round it lies in no piece) puts the outside within the disc; for "out", any piece corner inside the disc meets
     * a piece. Every turn of the outline is a piece corner, so what the circle misses is a sliver under a chord, at most
     * 0.002 px deep. A point within 0.001 px of a piece counts as in it, so a point on an edge two pieces share is in the
     * union. Faults: a pixel of [inside] or [outside] whose disc of radius [marginPx] fails (too close), and a pixel of neither
     * whose disc of radius [marginPx] + [slackPx] passes (left out).
     */
    private fun bandFaults(polys: List<List<Offset>>, w: Int, h: Int, inside: KitMask, outside: KitMask, marginPx: Double = 3.0, slackPx: Double = 0.25): BandFaults {
        val tol = 1e-3
        val xs = polys.map { poly -> DoubleArray(poly.size) { poly[it].x.toDouble() } }
        val ys = polys.map { poly -> DoubleArray(poly.size) { poly[it].y.toDouble() } }
        val boxes = xs.indices.map { k -> doubleArrayOf(xs[k].min() - tol, ys[k].min() - tol, xs[k].max() + tol, ys[k].max() + tol) }
        fun inPiece(k: Int, x: Double, y: Double): Boolean {
            val px = xs[k]
            val py = ys[k]
            var odd = false
            var j = px.size - 1
            for (i in px.indices) {
                if ((py[i] > y) != (py[j] > y) && x < px[i] + (y - py[i]) * (px[j] - px[i]) / (py[j] - py[i])) odd = !odd
                j = i
            }
            if (odd) return true
            for (i in px.indices) {
                val n = (i + 1) % px.size
                if (kitDistToSegment(x, y, px[i], py[i], px[n], py[n]) <= tol) return true
            }
            return false
        }
        fun inUnion(x: Double, y: Double): Boolean {
            for (k in xs.indices) {
                val b = boxes[k]
                if (x < b[0] || x > b[2] || y < b[1] || y > b[3]) continue
                if (inPiece(k, x, y)) return true
            }
            return false
        }
        val cornersX = xs.flatMap { it.asList() }.toDoubleArray()
        val cornersY = ys.flatMap { it.asList() }.toDoubleArray()
        val onOutline = BooleanArray(cornersX.size) { c ->
            (0 until 32).any { k -> val t = (k + 0.5) * 2 * PI / 32; !inUnion(cornersX[c] + 0.05 * cos(t), cornersY[c] + 0.05 * sin(t)) }
        }
        val ringCos = DoubleArray(96) { cos((it + 0.5) * 2 * PI / 96) }
        val ringSin = DoubleArray(96) { sin((it + 0.5) * 2 * PI / 96) }
        fun discIn(cx: Double, cy: Double, r: Double): Boolean {
            for (k in 0 until 96) if (!inUnion(cx + r * ringCos[k], cy + r * ringSin[k])) return false
            for (c in cornersX.indices) if (onOutline[c] && hypot(cornersX[c] - cx, cornersY[c] - cy) < r) return false
            return true
        }
        fun discOut(cx: Double, cy: Double, r: Double): Boolean {
            for (k in 0 until 96) if (inUnion(cx + r * ringCos[k], cy + r * ringSin[k])) return false
            for (c in cornersX.indices) if (hypot(cornersX[c] - cx, cornersY[c] - cy) < r) return false
            return true
        }
        // The pixels with some piece edge (shared or not) within reach of the centre.
        val reach = marginPx + slackPx
        val nearEdge = KitMask(w, h)
        for (k in xs.indices) for (i in xs[k].indices) {
            val n = (i + 1) % xs[k].size
            val ax = xs[k][i]
            val ay = ys[k][i]
            val bx = xs[k][n]
            val by = ys[k][n]
            val x0 = max(0, floor(min(ax, bx) - reach - 1).toInt())
            val x1 = min(w - 1, ceil(max(ax, bx) + reach + 1).toInt())
            val y0 = max(0, floor(min(ay, by) - reach - 1).toInt())
            val y1 = min(h - 1, ceil(max(ay, by) + reach + 1).toInt())
            for (y in y0..y1) for (x in x0..x1) {
                if (!nearEdge[x, y] && kitDistToSegment(x + 0.5, y + 0.5, ax, ay, bx, by) < reach) nearEdge[x, y] = true
            }
        }
        val f = BandFaults()
        for (y in 0 until h) for (x in 0 until w) {
            val cx = x + 0.5
            val cy = y + 0.5
            val centreIn = inUnion(cx, cy)
            val inSet = inside[x, y]
            val outSet = outside[x, y]
            if (!nearEdge[x, y]) {
                if (inSet && !centreIn) { f.insideClose++; f.note("an inside pixel outside every piece", x, y) }
                if (!inSet && centreIn) { f.insideLeftOut++; f.note("a pixel deep in a piece left out of the inside set", x, y) }
                if (outSet && centreIn) { f.outsideClose++; f.note("an outside pixel deep in a piece", x, y) }
                if (!outSet && !centreIn) { f.outsideLeftOut++; f.note("a pixel far from every piece left out of the outside set", x, y) }
                continue
            }
            if (inSet) {
                if (!centreIn || !discIn(cx, cy, marginPx - tol)) { f.insideClose++; f.note("an inside pixel closer than $marginPx px to the outline", x, y) }
            } else if (centreIn && discIn(cx, cy, reach)) {
                f.insideLeftOut++; f.note("a pixel $reach px or more inside left out of the inside set", x, y)
            }
            if (outSet) {
                if (centreIn || !discOut(cx, cy, marginPx - tol)) { f.outsideClose++; f.note("an outside pixel closer than $marginPx px to the outline", x, y) }
            } else if (!centreIn && discOut(cx, cy, reach)) {
                f.outsideLeftOut++; f.note("a pixel $reach px or more outside left out of the outside set", x, y)
            }
        }
        return f
    }

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
