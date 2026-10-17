// Acceptance kit (WO-004, TASK-019): the grid overview, reaching every puzzle, and the top bar's "always"
// (designs/WO-004-design.md §2 S-1Δ, S-2Δ, S-13Δ, S-14Δ cell keys, S-22 "Overview" and hit order 0, "What survives";
// decisions.md 2026-10-15 "The grid overview, as drawn" and its amendment). The grid is opened by a tap on the counter zone
// and a cell chosen by a tap on it, after a semantics scroll brings it on screen (the grid scrolls vertically, S-22); the
// controllers save through `Persistence` over the kit's store double, seeded where a line needs puzzles in every state.
// REQ-024 A2 lives here because its adversarial leg — the grid open — needs the overview TASK-021 builds.
package app.tangramplaytime.play

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class OverviewKitTest {

    // The ComponentActivity host, declared so the recreation legs can recreate it (KitScreen.recreate).
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // ── REQ-050 A2: one cell per puzzle, solved ones in colour ─────────────────────────────────────────

    @Test // REQ-050.A2
    fun pressingTheCounterShowsOneCellPerPuzzleSolvedOnesInColour() {
        // REQ-050 A2: "Pressing the counter shows one cell per puzzle, solved ones in colour." Rule: the grid "shows each
        // puzzle's state: New as the flat silhouette, In progress as the silhouette with a dot, Solved as its picture".
        // Oracle: the store double holds three puzzles Solved (each with a picture colour no PlayPalette colour is near, so
        // "in colour" can be told from the silhouette), one In progress (two valid placements) and the rest New; a New
        // puzzle is open. A tap at the centre of `counter` shows `overview`, holding exactly one `overview-cell-<id>` node per
        // listed puzzle (every cell composed, S-22), each carrying its own PuzzleIdKey and the store's state as PuzzleStateKey
        // (S-14Δ). Each cell's pixels, 4 dp inside its bounds, are read against the state the store holds: a Solved cell has
        // at least 2 % of its pixels in its picture's own colours; a New or In-progress cell has at least 2 % in the flat
        // silhouette colour and at most 0.5 % in its picture's own colours; the In-progress cell has a dot (at least 4 px of
        // the accent, decisions.md 2026-10-15 "The grid overview, as drawn": "the silhouette with an accent dot") and the New
        // cells other than the open one none (at most 2 px).
        val list = kitLibrary.puzzles
        val colourful = list.filter { kitDistinctArtColours(it.art).isNotEmpty() }
        assertTrue("the library has three puzzles whose pictures have a colour of their own (${colourful.size})", colourful.size >= 3)
        val solved = listOf(colourful.first(), colourful[colourful.size / 2], colourful.last()).distinctBy { it.id }
        val inProgress = list.first { it.kind == PuzzleKind.WARMUP && it !in solved }
        val open = list.first { it !in solved && it != inProgress }
        val seed = solved.associate { it.id to kitSolvedProgress() } + (inProgress.id to kitInProgress(kitSolutionLocks(inProgress, 2)))
        val ps = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = open.id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        kit.assertShows(open.id, "set-up")
        openGrid(kit)
        val cells = kit.nodesWithTagPrefix(KitTag.OVERVIEW_CELL_PREFIX)
        assertEquals("one cell per listed puzzle: ${cells.map { it.first }}", list.map { KitTag.overviewCell(it.id) }.sorted(), cells.map { it.first }.sorted())
        for (p in list) {
            val tag = KitTag.overviewCell(p.id)
            val expected = (seed[p.id] ?: PuzzleProgress.FRESH).state
            assertEquals("`$tag` PuzzleIdKey", p.id, kit.key(tag, PlaySemantics.PuzzleIdKey))
            assertEquals("`$tag` PuzzleStateKey: the store's state", expected, kit.key(tag, PlaySemantics.PuzzleStateKey))
            val shot = cellShot(kit, tag)
            val art = kitDistinctArtColours(p.art)
            val artPx = shot.count { c -> art.any { kitNear(c, it, 6f / 255f) } }
            val flatPx = shot.count { c -> kitNear(c, PlayPalette.silhouette, 6f / 255f) }
            val accentPx = shot.count { c -> kitNear(c, PlayPalette.accent, 24f / 255f) }
            val n = shot.size
            val report = "${p.id} ($expected): ${pct(artPx, n)} picture colours, ${pct(flatPx, n)} silhouette, $accentPx accent px of $n"
            android.util.Log.i("KitScreen", "overview cell $report")
            when (expected) {
                PuzzleState.SOLVED -> assertTrue("a Solved cell is in colour — its picture's own colours on at least 2 %: $report", artPx * 50 >= n)
                PuzzleState.NEW, PuzzleState.IN_PROGRESS -> {
                    assertTrue("an unsolved cell shows the flat silhouette on at least 2 %: $report", flatPx * 50 >= n)
                    assertTrue("an unsolved cell is not in colour — its picture's own colours on at most 0.5 %: $report", artPx * 200 <= n)
                }
            }
            if (expected == PuzzleState.IN_PROGRESS) assertTrue("the In-progress cell has a dot: $report", accentPx >= 4)
            if (expected == PuzzleState.NEW && p.id != open.id) assertTrue("a New cell has no dot: $report", accentPx <= 2)
        }
    }

    // ── REQ-050 A3: choosing a cell changes no other puzzle ────────────────────────────────────────────

    @Test // REQ-050.A3
    fun choosingACellOpensThatPuzzleWithoutChangingAnyOtherPuzzlesState() {
        // REQ-050 A3: "Choosing a cell opens that puzzle without changing any other puzzle's state." Statement: "one press
        // opens that puzzle". Oracle: the store double holds puzzles in every state, with times and best times (Solved ones,
        // two In progress with valid placements and a time, the rest New); the open puzzle is In progress. A tap on a cell
        // opens its puzzle — an In-progress one (its board as saved), a Solved one, a New one (still New: browsing does not
        // start it, TYPE-006) — and closes the grid. After the three choices: every puzzle's saved progress is exactly what
        // was seeded (state, board, time, best — S-20: times pass through untouched), every cell's PuzzleStateKey is the
        // seeded state, and so is the controller's `stateOf` (S-1Δ).
        val list = kitLibrary.puzzles
        val ipOpen = kitFirst(PuzzleKind.WARMUP)
        val ipOther = kitFirst(PuzzleKind.FULL)
        val solvedA = list[0]
        val solvedB = list.last { it != ipOpen && it != ipOther }
        val newOne = list.first { it.kind == PuzzleKind.WARMUP && it != ipOpen }
        val seed = mapOf(
            ipOpen.id to kitInProgress(kitSolutionLocks(ipOpen, 2), time = 15, best = null),
            ipOther.id to kitInProgress(kitSolutionLocks(ipOther, 3, order = kitBottomFirst(ipOther)), time = 22, best = 80),
            solvedA.id to kitSolvedProgress(time = 33, best = 33),
            solvedB.id to kitSolvedProgress(time = 51, best = 44),
        )
        val ps = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = ipOpen.id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        kit.assertShows(ipOpen.id, "set-up")
        for (target in listOf(ipOther, solvedB, newOne)) {
            val from = kit.shownId()
            openGrid(kit)
            chooseCell(kit, target.id)
            assertFalse("choosing ${target.id} closes the grid", kit.exists(KitTag.OVERVIEW))
            kit.assertShows(target.id, "the cell of ${target.id} chosen from $from")
            val state = (seed[target.id] ?: PuzzleProgress.FRESH).state
            assertEquals("${target.id} opened in its own state", state, kit.shownState())
            if (state == PuzzleState.IN_PROGRESS) {
                val saved = seed.getValue(target.id).placed
                val shown = kit.boardFromNodes()
                assertEquals("${target.id}'s board as saved: the pieces", saved.map { it.piece }.sorted(), shown.map { it.piece }.sorted())
                for (s in saved) assertTrue("${target.id}: ${s.piece} at $s", kitSamePlacement(s, shown.single { it.piece == s.piece }))
            }
        }
        ps.flush()
        for (p in list) {
            assertEquals("${p.id}'s saved progress after three choices", seed[p.id] ?: PuzzleProgress.FRESH, ps.store.progressNow(p.id))
            assertEquals("${p.id}: the controller's stateOf", (seed[p.id] ?: PuzzleProgress.FRESH).state, kit.onMain { kit.controller.stateOf(p.id) })
        }
        openGrid(kit)
        for (p in list) assertEquals("`overview-cell-${p.id}` PuzzleStateKey after three choices", (seed[p.id] ?: PuzzleProgress.FRESH).state, kit.key(KitTag.overviewCell(p.id), PlaySemantics.PuzzleStateKey))
        kit.press(KitTag.OVERVIEW_DONE)
    }

    // ── REQ-003 A1: every puzzle within reach ──────────────────────────────────────────────────────────

    @Test // REQ-003.A1
    fun fromAnyPuzzleEveryOtherPuzzleCanBeReachedWithoutSolvingAnything() {
        // REQ-003 A1: "From any puzzle, every other puzzle can be reached without solving anything." Rule: "No puzzle is
        // locked behind another puzzle." Oracle, with nothing solved — the store double holding two puzzles In progress and
        // the rest New — and nothing solved during the test: from every puzzle in turn (reached by a tap on its grid cell),
        // (1) one grid choice reaches the puzzle farthest from it in ‹ › order, and (2) taps on › from it show every other
        // puzzle once before it comes round again. Every reach is read from the screen (`assertShows`). At the end every
        // puzzle's saved state is as seeded: nothing was solved, nothing restarted.
        val list = kitLibrary.puzzles
        val ipA = kitFirst(PuzzleKind.WARMUP)
        val ipB = kitFirst(PuzzleKind.FULL)
        val seed = mapOf(
            ipA.id to kitInProgress(kitSolutionLocks(ipA, 2)),
            ipB.id to kitInProgress(kitSolutionLocks(ipB, 2, order = kitBottomFirst(ipB))),
        )
        val ps = kitControllerOver(rule, KitDeviceStore(seed))
        val kit = KitScreen(rule, ps.controller)
        kit.show(reducedMotion = true)
        val n = list.size
        for ((i, origin) in list.withIndex()) {
            if (kit.shownId() != origin.id) {
                openGrid(kit)
                chooseCell(kit, origin.id)
            }
            kit.assertShows(origin.id, "origin ${i + 1}")
            val far = list[(i + n / 2) % n]
            openGrid(kit)
            chooseCell(kit, far.id)
            kit.assertShows(far.id, "one grid choice from ${origin.id}")
            openGrid(kit)
            chooseCell(kit, origin.id)
            kit.assertShows(origin.id, "back to ${origin.id} by the grid")
            val seen = ArrayList<String>()
            repeat(n - 1) {
                kit.press(KitTag.NAV_NEXT)
                seen += kit.shownId()
            }
            assertEquals("› from ${origin.id} shows every other puzzle once", list.map { it.id }.filter { it != origin.id }.sorted(), seen.sorted())
            kit.press(KitTag.NAV_NEXT)
            kit.assertShows(origin.id, "› comes round to ${origin.id}")
        }
        ps.flush()
        for (p in list) {
            assertNotEquals("${p.id} was not solved", PuzzleState.SOLVED, ps.store.progressNow(p.id).state)
            assertEquals("${p.id}'s saved progress", seed[p.id] ?: PuzzleProgress.FRESH, ps.store.progressNow(p.id))
        }
    }

    // ── REQ-024 A2: the name and the number, always ────────────────────────────────────────────────────

    @Test // REQ-024.A2
    fun theTopBarAlwaysShowsTheCurrentPuzzlesNameAndNumber() {
        // REQ-024 A2: "The top bar always shows the current puzzle's name and number." Rule: the bar shows "the puzzle's name
        // … its number out of the total". Oracle: at each moment, the `title` node (displayed) reads the current puzzle's
        // title and a text node inside `counter` (displayed) reads its "n / N" (seam note 4; S-11Δ), both lying in the bar
        // (above the board, within the screen) — the current puzzle being `play-screen`'s PuzzleIdKey, read against the
        // silhouette's box (`assertShows`). "Always" (design §3: after a wrap, after a long press, after a recreation, and
        // with the grid open): New, mid-drag, In progress, Solved; after › on the last and ‹ on the first; after a long
        // press on ›; in the first frame of a recreated activity. With the grid open — the adversarial path, where
        // `isDisplayed` cannot see a sheet drawn over the bar — the bar lies wholly outside the `overview` node, the name's
        // and the number's pixels are what they were with the grid closed, and it is inert: taps on ‹, › and the counter and a long
        // press on › change nothing and leave the grid open (decisions.md 2026-10-15 "Amends the 2026-10-15 'grid overview,
        // as drawn' row"); again with the grid scrolled to its end, and in the first frame of an activity recreated with the
        // grid open (it stays open, design "What survives").
        val list = kitLibrary.puzzles
        val seed = listOf(list[1], list[2], list[3]).associate { it.id to kitSolvedProgress() }
        val ps = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = list[0].id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        assertBar(kit, "New, at the start")
        var finger = kit.pickUpFromTray(kit.puzzle.pieces.first())
        assertBar(kit, "mid-drag")
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.trayCell(kit.puzzle.pieces.last())).center, 3)
        kit.release()
        kit.advance(300)
        assertBar(kit, "In progress")
        kit.holdDown(kit.bounds(KitTag.NAV_NEXT).center, 600)
        kit.lift()
        kit.frames()
        assertEquals("set-up: the long press skips the three solved puzzles", list[4].id, kit.shownId())
        assertBar(kit, "after a long press on ›")
        kit.open(list.last().id)
        kit.press(KitTag.NAV_NEXT)
        assertBar(kit, "after › on the last puzzle")
        kit.press(KitTag.NAV_PREV)
        assertBar(kit, "after ‹ on the first puzzle")
        kit.open(list[1].id)
        assertBar(kit, "Solved")
        kit.recreate(rule.activityRule.scenario)
        assertBar(kit, "the first frame of a recreated activity")

        // The grid open.
        kit.open(list[5].id)
        val closed = barShots(kit)
        openGrid(kit)
        assertBarWithGridOpen(kit, list[5].id, closed, "the grid open")
        val lastCell = KitTag.overviewCell(list.last().id)
        rule.onNode(hasTestTag(lastCell), useUnmergedTree = true).performScrollTo()
        kit.advance(500)
        assertBarWithGridOpen(kit, list[5].id, closed, "the grid scrolled to its last cell")
        kit.recreate(rule.activityRule.scenario)
        assertTrue("the grid is still open after a recreation (design \"What survives\")", kit.exists(KitTag.OVERVIEW))
        assertBarWithGridOpen(kit, list[5].id, closed, "the first frame of an activity recreated with the grid open")
        kit.press(KitTag.OVERVIEW_DONE)
        settleClose(kit)
        assertFalse("Done closes the grid", kit.exists(KitTag.OVERVIEW))
        assertBar(kit, "the grid closed")
    }

    // ── Pins ───────────────────────────────────────────────────────────────────────────────────────────

    @Test // pin: REQ-050 rules "The grid lists puzzles in the same order as ‹ › (REQ-040)" and "Each grid cell's touch area is at least 64 dp square"; decisions.md 2026-10-15 "The grid overview, as drawn" ("square cells of at least 84 dp … all composed (not lazy)"); S-22 "a verticalScroll grid (every cell composed)"; no token
    fun theCellsAreSquaresOfAtLeast84DpInListOrderAllComposed() {
        val kit = KitScreen(rule)
        kit.show()
        openGrid(kit)
        val list = kitLibrary.puzzles
        val cells = list.map { p ->
            val node = kit.node(KitTag.overviewCell(p.id))
            p.id to kit.unclippedBounds(node)
        }
        for ((id, r) in cells) {
            assertTrue("`overview-cell-$id` is at least 84 dp wide (${r.width / kit.density} dp)", r.width >= kit.dp(84f) - 1f)
            assertTrue("`overview-cell-$id` is square (${r.width} × ${r.height} px)", kotlin.math.abs(r.width - r.height) <= 1f)
        }
        val reading = cells.sortedWith(compareBy({ (it.second.top / 4f).roundToInt() }, { it.second.left })).map { it.first }
        assertEquals("the cells in reading order are the ‹ › order", list.map { it.id }, reading)
        kit.press(KitTag.OVERVIEW_DONE)
    }

    @Test // pin: REQ-050 rule "The grid is an overlay over the current puzzle; closing it … returns to the same puzzle state"; S-22 "It closes by Done, a tap on the scrim" and hit order 0 "The overview is open: the router neither consumes nor acts"; S-22 "The sheet is inset 16 dp"; decisions.md 2026-10-15 "The grid overview, as drawn" ("Nothing under it reacts while it is open") and "Browsing readings no REQ fixes" ("Choosing the open puzzle's own grid cell only closes the grid"); no token
    fun theGridClosesToTheSamePuzzleStateAndNothingUnderItReacts() {
        // Oracle, on a full-set puzzle In progress (two pieces placed through S-2, a tray piece turned): opened and closed by
        // Done, by a tap on the scrim (4 dp inside the overview's left edge, within the sheet's 16 dp inset, on no cell), and
        // by choosing its own cell — each time the same puzzle, the same board, the same tray turns, still In progress. With
        // the grid open, a tap where the Restart pill lies under it and a tap where a tray piece lies under it (each a point
        // on no cell and not Done) act on neither: no Restart, no turn.
        val kit = KitScreen(rule)
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        var placed = 0
        for (piece in p.pieces) if (placed < 2 && kit.placeByApi(piece)) placed++
        kit.frames()
        kit.advance(250)
        assertEquals("set-up: two pieces placed", 2, placed)
        val trayOne = p.pieces.first { piece -> kit.placed().none { it.piece == piece } }
        kit.tapTray(trayOne, 1)
        val board = kit.boardFromNodes()
        val turn = kit.key(PlayTags.trayPiece(trayOne), PlaySemantics.TurnKey)
        fun assertSame(what: String) {
            kit.assertShows(p.id, what)
            assertEquals("$what: In progress", PuzzleState.IN_PROGRESS, kit.shownState())
            val now = kit.boardFromNodes()
            assertEquals("$what: the board's pieces", board.map { it.piece }, now.map { it.piece })
            for (b in board) assertTrue("$what: ${b.piece} at $b", kitSamePlacement(b, now.single { it.piece == b.piece }))
            assertEquals("$what: $trayOne's tray turn", turn, kit.key(PlayTags.trayPiece(trayOne), PlaySemantics.TurnKey))
        }
        openGrid(kit)
        kit.press(KitTag.OVERVIEW_DONE)
        settleClose(kit)
        assertFalse("Done closes the grid", kit.exists(KitTag.OVERVIEW))
        assertSame("closed by Done")

        openGrid(kit)
        val scrim = scrimPoint(kit)
        kit.tap(scrim)
        settleClose(kit)
        assertFalse("a tap on the scrim at $scrim closes the grid", kit.exists(KitTag.OVERVIEW))
        assertSame("closed by the scrim")

        openGrid(kit)
        chooseCell(kit, p.id)
        assertFalse("choosing the open puzzle's own cell closes the grid", kit.exists(KitTag.OVERVIEW))
        assertSame("closed by its own cell")

        val pill = kit.bounds(KitTag.RESTART)
        val trayCell = kit.bounds(PlayTags.trayCell(trayOne))
        openGrid(kit)
        for ((name, area) in listOf("the Restart pill" to pill, "$trayOne's tray cell" to trayCell)) {
            val at = pointOnNoControl(kit, area) ?: continue
            kit.tap(at)
            settleClose(kit)
            assertEquals("a tap at $at over $name with the grid open: still In progress (no Restart)", PuzzleState.IN_PROGRESS, kit.shownState())
            assertEquals("a tap at $at over $name with the grid open: $trayOne not turned", turn, kit.key(PlayTags.trayPiece(trayOne), PlaySemantics.TurnKey))
            if (!kit.exists(KitTag.OVERVIEW)) openGrid(kit)
        }
        kit.press(KitTag.OVERVIEW_DONE)
        settleClose(kit)
        assertSame("after the taps over the grid")
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** A tap at the centre of the counter zone (S-22: the whole zone opens the grid), then the overlay settled. */
    private fun openGrid(kit: KitScreen) {
        kit.press(KitTag.COUNTER)
        kit.advance(300)
        assertTrue("a tap on the counter shows `overview`", kit.exists(KitTag.OVERVIEW))
    }

    /** Scrolls [id]'s cell on screen (a semantics scroll of the grid) and taps its centre. */
    private fun chooseCell(kit: KitScreen, id: String) {
        val tag = KitTag.overviewCell(id)
        rule.onNode(hasTestTag(tag), useUnmergedTree = true).performScrollTo()
        kit.advance(500)
        val n = kit.node(tag)
        val visible = kit.bounds(tag)
        check(visible.height >= n.size.height - 1f && visible.width >= n.size.width - 1f) { "kit: `$tag` is not wholly on screen after the scroll ($visible, size ${n.size})" }
        kit.tap(visible.center)
        settleClose(kit)
    }

    /** 300 ms on the paused clock after an action that closes the grid: no REQ or row times the overlay's close. */
    private fun settleClose(kit: KitScreen) {
        kit.frames()
        kit.advance(300)
    }

    /** [tag]'s cell scrolled on screen and captured: its pixels 4 dp inside its bounds. */
    private fun cellShot(kit: KitScreen, tag: String): List<androidx.compose.ui.graphics.Color> {
        rule.onNode(hasTestTag(tag), useUnmergedTree = true).performScrollTo()
        kit.advance(500)
        val img: PixelMap = kit.capture(tag)
        val inset = kit.dp(4f).roundToInt()
        val out = ArrayList<androidx.compose.ui.graphics.Color>()
        for (y in inset until img.height - inset) for (x in inset until img.width - inset) out += img[x, y]
        check(out.size > 1000) { "kit: `$tag` capture ${img.width} × ${img.height} px leaves ${out.size} px 4 dp inside" }
        return out
    }

    private fun pct(k: Int, n: Int): String = "%.1f %%".format(100.0 * k / n)

    /** The name and the number in the bar, displayed, in the bar's band (above the board, within the screen). */
    private fun assertBar(kit: KitScreen, what: String) {
        val id = kit.shownId()
        kit.assertShows(id, what)
        rule.onNode(hasTestTag(PlayTags.TITLE), useUnmergedTree = true).assertIsDisplayed()
        val number = rule.onNode(hasText(kitCounterText(id)) and hasAnyAncestor(hasTestTag(KitTag.COUNTER)), useUnmergedTree = true)
        number.assertIsDisplayed()
        val screen = kit.bounds(PlayTags.SCREEN)
        val boardTop = kit.bounds(PlayTags.BOARD).top
        val numberBox = number.fetchSemanticsNode().boundsInRoot.translate(-kit.screenOrigin())
        for ((name, r) in listOf("the title" to kit.bounds(PlayTags.TITLE), "the number" to numberBox)) {
            assertTrue("$what: $name $r has a size", r.width > 0f && r.height > 0f)
            assertTrue("$what: $name $r lies in the bar, above the board (top $boardTop)", r.top >= -1f && r.bottom <= boardTop + 1f)
            assertTrue("$what: $name $r lies within the screen $screen", r.left >= screen.left - 1f && r.right <= screen.right + 1f)
        }
    }

    /** The name's and the number's pixels: the captures of the `title` node and of the "n / N" node inside `counter`. */
    private fun barShots(kit: KitScreen): Map<String, PixelMap> {
        val id = kit.shownId()
        return mapOf(
            "the title" to kit.capture(PlayTags.TITLE),
            "the number" to rule.onNode(hasText(kitCounterText(id)) and hasAnyAncestor(hasTestTag(KitTag.COUNTER)), useUnmergedTree = true).captureToImage().toPixelMap(),
        )
    }

    private fun assertBarWithGridOpen(kit: KitScreen, id: String, closed: Map<String, PixelMap>, what: String) {
        assertTrue("$what: the grid is open", kit.exists(KitTag.OVERVIEW))
        assertBar(kit, what)
        val overview = kit.bounds(KitTag.OVERVIEW)
        for (tag in listOf(KitTag.COUNTER, KitTag.NAV_PREV, KitTag.NAV_NEXT, PlayTags.TITLE)) {
            val r = kit.bounds(tag)
            assertFalse("$what: `$tag` $r overlaps `overview` $overview (the sheet and scrim start below the bar)", r.overlaps(overview))
        }
        val shots = barShots(kit)
        for ((name, before) in closed) {
            val now = shots.getValue(name)
            assertEquals("$what: $name's capture size", before.width to before.height, now.width to now.height)
            var changed = 0
            var first = ""
            for (y in 0 until now.height) for (x in 0 until now.width) {
                if (!kitNear(before[x, y], now[x, y], 3f / 255f)) { changed++; if (first.isEmpty()) first = "($x, $y): ${kitHex(before[x, y])} → ${kitHex(now[x, y])}" }
            }
            assertEquals("$what: $name's pixels that differ from the grid closed (drawn over?); first $first", 0, changed)
        }
        // Inert: nothing acts, and the grid stays open.
        for (tag in listOf(KitTag.NAV_PREV, KitTag.NAV_NEXT, KitTag.COUNTER)) {
            kit.press(tag)
            assertEquals("$what: a tap on `$tag` changes no puzzle", id, kit.shownId())
            assertTrue("$what: a tap on `$tag` leaves the grid open", kit.exists(KitTag.OVERVIEW))
        }
        kit.holdDown(kit.bounds(KitTag.NAV_NEXT).center, 600)
        kit.lift()
        kit.frames()
        assertEquals("$what: a long press on › changes no puzzle", id, kit.shownId())
        assertTrue("$what: a long press on › leaves the grid open", kit.exists(KitTag.OVERVIEW))
    }

    /** A point of `overview` 4 dp inside its left edge (the scrim: S-22's 16 dp inset), at its middle, on no cell and not Done. */
    private fun scrimPoint(kit: KitScreen): Offset {
        val o = kit.bounds(KitTag.OVERVIEW)
        val at = Offset(o.left + kit.dp(4f), o.center.y)
        check(onControl(kit, at) == null) { "kit: the scrim point $at lies on ${onControl(kit, at)}" }
        return at
    }

    /** A point of [area] (2 px in) inside `overview` and on no cell and not Done, or null. */
    private fun pointOnNoControl(kit: KitScreen, area: Rect): Offset? {
        val o = kit.bounds(KitTag.OVERVIEW)
        var y = area.top + 2f
        while (y <= area.bottom - 2f) {
            var x = area.left + 2f
            while (x <= area.right - 2f) {
                val q = Offset(x, y)
                if (o.contains(q) && onControl(kit, q) == null) return q
                x += kit.dp(4f)
            }
            y += kit.dp(4f)
        }
        return null
    }

    /** The cell or Done whose on-screen bounds hold [at], or null. */
    private fun onControl(kit: KitScreen, at: Offset): String? {
        val tags = kit.nodesWithTagPrefix(KitTag.OVERVIEW_CELL_PREFIX).map { it.first } + KitTag.OVERVIEW_DONE
        return tags.firstOrNull { tag -> kit.exists(tag) && kit.bounds(tag).let { it.width > 0f && it.contains(at) } }
    }
}
