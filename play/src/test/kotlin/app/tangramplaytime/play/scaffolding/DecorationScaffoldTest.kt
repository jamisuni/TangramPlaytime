// scaffolding — TASK-014's own disposable checks of the pure parts of the decorations and the solve: the
// size-mark letters, the badge areas and their precedence in the hit test, the pulse keyframes and the solve
// timeline's arithmetic (no token; the acceptance verdict is the device kit's DecorationKitTest,
// CornerPulseKitTest and SolvedPictureKitTest). WO-004 TASK-020 updated it for S-11Δ (the `PlayTexts` literal),
// S-22 (`HitTest.at` takes the controls shown; none here) and S-3Δ (`SolveVisual` is one frame's value).
// WO-010 F1 added the board badge's clamp: its whole 60 dp area and its disc lie on the board (letters-m, the bundle).
package app.tangramplaytime.play.scaffolding

import androidx.compose.ui.geometry.Offset
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.BADGE_TOUCH_DP
import app.tangramplaytime.play.BadgePlace
import app.tangramplaytime.play.CONFETTI_MS
import app.tangramplaytime.play.FADE_DELAY_MS
import app.tangramplaytime.play.FADE_MS
import app.tangramplaytime.play.Hit
import app.tangramplaytime.play.HitTest
import app.tangramplaytime.play.POP_MS
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlayLayout
import app.tangramplaytime.play.PlayTexts
import app.tangramplaytime.play.ScreenGeometry
import app.tangramplaytime.play.ShownControls
import app.tangramplaytime.play.Slot
import app.tangramplaytime.play.SolveVisual
import app.tangramplaytime.play.artColour
import app.tangramplaytime.play.badgeSlots
import app.tangramplaytime.play.contains
import app.tangramplaytime.play.insidePolygon
import app.tangramplaytime.play.pulseFrame
import app.tangramplaytime.play.sizeMarkOf
import app.tangramplaytime.play.trayMiniaturePx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class DecorationScaffoldTest {

    private val engine = LockEngine()
    private val library = PuzzleLibraries.bundled(engine)
    private val layout = PlayLayout(
        trayRows = listOf(listOf(PieceId.LT1, PieceId.LT2, PieceId.MT), listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)),
        floatOffsetDp = 30f,
        cellGapDp = 8f,
        sideMarginDp = 8f,
        topBarDp = 66f,
    )
    private val texts = PlayTexts(
        sizeSmall = "S", sizeMedium = "M", sizeLarge = "L", titleLanguage = "en",
        restart = "Restart", retry = "Retry", next = "Next", bestTime = "Best time", noTime = "–",
        inProgress = "in progress", solved = "solved", counterFormat = "%1\$d / %2\$d", allPuzzles = "All puzzles", done = "Done",
    )
    private val density = 2.625f

    @Test
    fun theLettersFollowTheTriangleSizesAndTheOthersHaveNone() {
        assertEquals("L", sizeMarkOf(PieceId.LT1, texts))
        assertEquals("L", sizeMarkOf(PieceId.LT2, texts))
        assertEquals("M", sizeMarkOf(PieceId.MT, texts))
        assertEquals("S", sizeMarkOf(PieceId.ST1, texts))
        assertEquals("S", sizeMarkOf(PieceId.ST2, texts))
        assertNull(sizeMarkOf(PieceId.SQ, texts))
        assertNull(sizeMarkOf(PieceId.PG, texts))
    }

    @Test
    fun theTrayBadgeIsSixtyDpInsideThePgCellsTopRightAndWinsTheHitTestOnlyThere() {
        // Code review F1 (decisions.md 2026-10-12): the disc and the whole 60 dp area lie inside the PG cell; every
        // other cell is wholly its piece's. 1080 px at 2.625 (411 dp) and 945 px (360 dp, the narrowest REQ-013 width).
        for (widthPx in listOf(1080, 945)) {
            val controller = PlayController(library, engine)
            controller.open(library.puzzles.first { it.kind == PuzzleKind.FULL }.id)
            val g = ScreenGeometry.of(widthPx, 2280, density, layout, controller.puzzle)
            controller.boardDpPerUnit = (g.mapping.pxPerUnit / density).toDouble()
            val slots = badgeSlots(controller, g)
            val tray = slots.tray ?: throw AssertionError("a tray badge with the PG in the tray")
            assertNull("no board badge with the PG in the tray", slots.board)
            val sixty = Math.round(BADGE_TOUCH_DP * density)
            assertEquals(sixty, tray.area.width)
            assertEquals(sixty, tray.area.height)
            val cell = g.cells.getValue(PieceId.PG)
            val margin = 12f * density
            assertTrue("$widthPx: the area ${tray.area} lies inside the PG cell $cell", tray.area.left >= cell.left && tray.area.top >= cell.top && tray.area.left + tray.area.width <= cell.left + cell.width && tray.area.top + tray.area.height <= cell.top + cell.height)
            assertEquals("the area ends at the cell's right edge", cell.left + cell.width, tray.area.left + tray.area.width)
            assertEquals("the area starts at the cell's top", cell.top, tray.area.top)
            val r = tray.discRadius
            assertTrue("$widthPx: the whole disc ${tray.disc} r $r lies inside the area ${tray.area}", tray.disc.x - r >= tray.area.left && tray.disc.x + r <= tray.area.left + tray.area.width && tray.disc.y - r >= tray.area.top && tray.disc.y + r <= tray.area.top + tray.area.height)
            assertTrue("disc centre ${tray.disc} in the cell's top-right quarter $cell", tray.disc.x > cell.left + cell.width * 0.5f && tray.disc.y < cell.top + cell.height * 0.5f)
            assertEquals("the disc's centre is the badge", Hit.Badge(onBoard = false), HitTest.at(tray.disc, g, controller, margin, ShownControls.NONE))
            assertEquals("the disc's edge is the badge", Hit.Badge(onBoard = false), HitTest.at(Offset(tray.disc.x - r + 1f, tray.disc.y), g, controller, margin, ShownControls.NONE))
            // Decisions.md 2026-10-12 (fix batch): the visible thing wins in the tray too — disc → the PG miniature's
            // drawn shape → the rest of the area → the rest of the cell.
            val mini = trayMiniaturePx(engine, PieceId.PG, controller.trayTurn(PieceId.PG), controller.trayMirrored(PieceId.PG), g)!!
            val middle = Offset(mini.sumOf { it.x.toDouble() }.toFloat() / mini.size, mini.sumOf { it.y.toDouble() }.toFloat() / mini.size)
            assertTrue("$widthPx: the miniature's middle $middle lies in the badge area ${tray.area}", tray.area.contains(middle))
            assertEquals("$widthPx: the miniature's middle is the PG", Hit.Tray(PieceId.PG), HitTest.at(middle, g, controller, margin, ShownControls.NONE))
            var drawnInArea = 0
            var bareInArea = 0
            for (y in tray.area.top until tray.area.top + tray.area.height step 4) for (x in tray.area.left until tray.area.left + tray.area.width step 4) {
                val q = Offset(x + 0.5f, y + 0.5f)
                val expected = when {
                    (q - tray.disc).getDistance() <= r -> Hit.Badge(onBoard = false)
                    insidePolygon(q, mini) -> Hit.Tray(PieceId.PG).also { drawnInArea++ }
                    else -> Hit.Badge(onBoard = false).also { bareInArea++ }
                }
                assertEquals("$widthPx: $q in the badge area", expected, HitTest.at(q, g, controller, margin, ShownControls.NONE))
            }
            assertTrue("$widthPx: the area holds drawn ($drawnInArea) and bare ($bareInArea) samples", drawnInArea > 0 && bareInArea > 0)
            assertEquals("the PG cell's bottom-left corner is the PG", Hit.Tray(PieceId.PG), HitTest.at(Offset(cell.leftPx + 1f, cell.topPx + cell.height - 1f), g, controller, margin, ShownControls.NONE))
            for (id in listOf(PieceId.LT2, PieceId.MT, PieceId.ST1, PieceId.SQ)) {
                val c = g.cells.getValue(id)
                for (q in listOf(Offset(c.leftPx + 1f, c.topPx + c.height - 1f), Offset(c.leftPx + 1f, c.topPx + 1f), Offset(c.leftPx + c.width - 1f, c.topPx + c.height - 1f))) {
                    assertEquals("$widthPx: $q in the $id cell is its piece's", Hit.Tray(id), HitTest.at(q, g, controller, margin, ShownControls.NONE))
                }
            }
            val gapAbove = Offset(tray.disc.x, cell.topPx - 2f)
            assertFalse("the row gap above the PG cell is not the badge", HitTest.at(gapAbove, g, controller, margin, ShownControls.NONE) is Hit.Badge)
        }
    }

    @Test
    fun theBoardBadgesWholeAreaAndItsDiscLieOnTheBoardWithThePgAtTheBoardsRightEdge() {
        // WO-010 F1 (the floor's DecorationKitTest run, 2026-10-19: `flip-badge-board` at x 930–1088 on a 1080 px
        // screen). decisions.md 2026-10-12 "Tray decoration numbers from DI-1/DI-2": the board badge sits 22 dp right
        // of the placed PG's box and 4 dp above it, clamped onto the board; its touch area is 60 dp. letters-m puts
        // the PG at the silhouette's right edge, so on the 411 dp phone the unclamped badge would leave the board.
        // The 1080 px phone at 2.625 px/dp, the kit's literal layout, no insets; the composable's height varies with
        // the host (the whole 914 dp screen, or less the bars), so a few heights are checked.
        val cases = listOf(2400, 2337, 2274, 2211).map { heightPx ->
            val controller = PlayController(library, engine)
            assertTrue(controller.open("letters-m"))
            val g = ScreenGeometry.of(1080, heightPx, density, layout, controller.puzzle)
            controller.boardDpPerUnit = (g.mapping.pxPerUnit / density).toDouble()
            assertTrue("letters-m: the PG locks at its solution pose", setUpBoard(controller, setOf(PieceId.PG)))
            val board = badgeSlots(controller, g).board ?: throw AssertionError("a board badge with the PG placed")
            val pg = controller.placed.single { it.piece == PieceId.PG }
            val box = engine.vertices(pg.piece, pg.turn, pg.mirrored, pg.centre).map(g.mapping::toPx)
            val facts = "1080 × $heightPx px: board ${rectOf(g.board)}; PG box ${rectOf(box)}; area ${rectOf(board.area)}; " +
                "disc ${rectOf(board.disc, board.discRadius)}"
            println("facts — $facts")
            Triple(facts, board, g)
        }
        for ((facts, board, g) in cases) {
            assertBadgeOnBoard(facts, board, g)
            // At the right edge the area ends at the board's edge, and the disc keeps its clamped place there.
            val right = g.board.left + g.board.width
            assertEquals("$facts: the area ends at the board's right edge", right, board.area.left + board.area.width)
            assertEquals("$facts: the disc ends at the board's right edge", right.toFloat(), board.disc.x + board.discRadius, 0.01f)
        }
    }

    @Test
    fun everyBundledPuzzlesBoardBadgeLiesWhollyOnTheBoard() {
        // The same clamp over the bundle: the PG placed at its solution pose (other pieces first where the engine
        // needs them, one piece always left in the tray), on the 411 dp phone and the 360 dp one.
        var checked = 0
        val off = ArrayList<String>()
        for ((widthPx, heightPx) in listOf(1080 to 2211, 1080 to 2400, 945 to 2100)) {
            for (p in library.puzzles.filter { PieceId.PG in it.pieces }) {
                val controller = PlayController(library, engine)
                assertTrue(controller.open(p.id))
                val g = ScreenGeometry.of(widthPx, heightPx, density, layout, controller.puzzle)
                controller.boardDpPerUnit = (g.mapping.pxPerUnit / density).toDouble()
                if (!setUpBoard(controller, setOf(PieceId.PG))) continue
                val board = badgeSlots(controller, g).board ?: throw AssertionError("${p.id}: a board badge with the PG placed")
                checked++
                try {
                    assertBadgeOnBoard("${p.id} at $widthPx × $heightPx px", board, g)
                } catch (e: AssertionError) {
                    off += e.message.orEmpty()
                }
            }
        }
        println("sweep — $checked boards with the PG placed; ${off.size} off the board")
        assertTrue("the PG was placed in some puzzles ($checked)", checked > 0)
        assertTrue("${off.size} of $checked board badges leave the board:\n${off.joinToString("\n")}", off.isEmpty())
    }

    /** The board badge's 60 dp area and its whole 38 dp disc lie inside the board, and the disc inside the area. */
    private fun assertBadgeOnBoard(what: String, place: BadgePlace, g: ScreenGeometry) {
        val b = g.board
        val a = place.area
        val r = place.discRadius
        val sixty = Math.round(BADGE_TOUCH_DP * density)
        assertEquals("$what: the area is 60 dp wide", sixty, a.width)
        assertEquals("$what: the area is 60 dp tall", sixty, a.height)
        assertTrue("$what: the area lies inside the board", a.left >= b.left && a.top >= b.top && a.left + a.width <= b.left + b.width && a.top + a.height <= b.top + b.height)
        assertTrue("$what: the disc lies inside the board", place.disc.x - r >= b.left && place.disc.y - r >= b.top && place.disc.x + r <= b.left + b.width && place.disc.y + r <= b.top + b.height)
        assertTrue("$what: the disc lies inside the area", place.disc.x - r >= a.left && place.disc.y - r >= a.top && place.disc.x + r <= a.left + a.width && place.disc.y + r <= a.top + a.height)
    }

    /** Places [must] at their solution poses through S-2, other pieces first where the engine needs them, always leaving one piece in the tray (as the device kit's set-up). */
    private fun setUpBoard(controller: PlayController, must: Set<PieceId>): Boolean {
        val p = controller.puzzle
        fun placedNow(): Set<PieceId> = controller.placed.map { it.piece }.toSet()
        repeat(p.pieces.size + 1) {
            var progress = false
            for (m in must) {
                if (m !in placedNow() && p.pieces.count { it !in placedNow() } > 1 && placeAtSolution(controller, m)) progress = true
            }
            if (must.all { it in placedNow() }) return true
            val helpers = p.pieces.filter { it !in placedNow() && it !in must }
            if (helpers.size > 1) {
                for (h in helpers) if (placeAtSolution(controller, h)) { progress = true; break }
            }
            if (!progress) return false
        }
        return must.all { it in placedNow() }
    }

    /** Drags [piece] from the tray to its solution pose (the tray mirror by the badge for the PG) and drops it; true when it locked there. */
    private fun placeAtSolution(controller: PlayController, piece: PieceId): Boolean {
        val sp = controller.puzzle.solution.single { it.piece == piece }
        val centre = Point(sp.polygon.sumOf { it.x } / sp.polygon.size, sp.polygon.sumOf { it.y } / sp.polygon.size)
        fun matches(turn: Turn, mirrored: Boolean, at: Point): Boolean =
            engine.vertices(piece, turn, mirrored, at).all { v -> sp.polygon.any { q -> hypot(v.x - q.x, v.y - q.y) < 1e-6 } }
        val trayMirror = controller.trayMirrored(piece)
        val pose = listOf(trayMirror, !trayMirror).flatMap { m -> (0..7).map { Turn(it) to m } }.firstOrNull { (t, m) -> matches(t, m, centre) } ?: return false
        if (pose.second != trayMirror && !(piece == PieceId.PG && controller.pressBadge())) return false
        if (!controller.startDrag(piece, centre)) return false
        val steps = ((pose.first.step - controller.dragged!!.turn.step) % 8 + 8) % 8
        if (steps != 0) controller.twistDrag(if (steps <= 4) steps else steps - 8)
        controller.moveDrag(centre, overBoard = true)
        val lock = controller.drop() ?: return false
        return matches(lock.turn, lock.mirrored, lock.centre)
    }

    private fun rectOf(s: Slot): String = rectOf(s.leftPx, s.topPx, s.leftPx + s.width, s.topPx + s.height)

    private fun rectOf(points: List<Offset>): String = rectOf(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })

    private fun rectOf(c: Offset, r: Float): String = rectOf(c.x - r, c.y - r, c.x + r, c.y + r)

    private fun rectOf(l: Float, t: Float, r: Float, b: Float): String =
        "x %.1f–%.1f y %.1f–%.1f px (x %.1f–%.1f y %.1f–%.1f dp)".format(l, r, t, b, l / density, r / density, t / density, b / density)

    @Test
    fun noBadgeWithoutThePgOrAfterTheSolve() {
        val controller = PlayController(library, engine)
        val mini = library.puzzles.first { it.kind == PuzzleKind.MINI && PieceId.PG !in it.pieces }
        controller.open(mini.id)
        val g = ScreenGeometry.of(1080, 2280, density, layout, controller.puzzle)
        val slots = badgeSlots(controller, g)
        assertNull(slots.tray)
        assertNull(slots.board)
    }

    @Test
    fun thePulseKeyframesPeakAtFortyFivePercentAndSettle() {
        val (s0, a0) = pulseFrame(0f)
        assertEquals(0.3f, s0, 1e-6f)
        assertEquals(0f, a0, 1e-6f)
        val (sPeak, aPeak) = pulseFrame(0.45f)
        assertEquals(1.3f, sPeak, 1e-6f)
        assertEquals(1f, aPeak, 1e-6f)
        val (s1, a1) = pulseFrame(1f)
        assertEquals(1f, s1, 1e-6f)
        assertEquals(0.9f, a1, 1e-6f)
    }

    @Test
    fun theSolveTimelineFollowsTheReqTwentyThreeNumbers() {
        // WO-004 TASK-020: `SolveVisual` is one frame's value now (S-3Δ's driver works it out in the composition).
        var solve = SolveVisual.UNSOLVED
        assertNull(solve.pictureAlpha)
        assertTrue(solve.piecesShown)
        assertFalse(solve.confettiShown(reducedMotion = false))
        assertFalse("no solved row unsolved", solve.rowShown)
        solve = SolveVisual(0f)
        assertEquals(0f, solve.popBump(reducedMotion = false), 1e-6f)
        assertEquals(0f, solve.popBump(reducedMotion = true), 1e-6f)
        solve = SolveVisual(POP_MS / 2f)
        assertEquals(1f, solve.popBump(reducedMotion = false), 1e-6f)
        assertEquals(0f, solve.popBump(reducedMotion = true), 1e-6f)
        solve = SolveVisual((FADE_DELAY_MS - 1).toFloat())
        assertNull("no picture before 600 ms", solve.pictureAlpha)
        assertTrue(solve.confettiShown(reducedMotion = false))
        assertFalse(solve.confettiShown(reducedMotion = true))
        solve = SolveVisual(FADE_DELAY_MS.toFloat())
        assertEquals(0f, solve.pictureAlpha!!, 1e-6f)
        assertTrue(solve.piecesShown)
        solve = SolveVisual((FADE_DELAY_MS + FADE_MS / 2).toFloat())
        assertEquals(0.5f, solve.pictureAlpha!!, 1e-6f)
        assertFalse("no solved row mid-fade", solve.rowShown)
        solve = SolveVisual((FADE_DELAY_MS + FADE_MS).toFloat())
        assertEquals(1f, solve.pictureAlpha!!, 1e-6f)
        assertFalse("the pieces go once the picture is fully shown", solve.piecesShown)
        assertTrue("the solved row from the frame the picture is fully shown", solve.rowShown)
        solve = SolveVisual((CONFETTI_MS - 1).toFloat())
        assertTrue(solve.confettiShown(reducedMotion = false))
        solve = SolveVisual.AT_END
        assertFalse(solve.confettiShown(reducedMotion = false))
        assertTrue("a Solved puzzle at its end shows the row", solve.rowShown)
        assertFalse("and no piece", solve.piecesShown)
    }

    @Test
    fun artColoursReadHexAndOpacity() {
        val c = artColour("#3D8BFD", null)
        assertEquals(0x3D / 255f, c.red, 1e-6f)
        assertEquals(0x8B / 255f, c.green, 1e-6f)
        assertEquals(0xFD / 255f, c.blue, 1e-6f)
        assertEquals(1f, c.alpha, 1e-6f)
        assertEquals(0.4f, artColour("#000000", 0.4).alpha, 1e-6f)
    }
}
