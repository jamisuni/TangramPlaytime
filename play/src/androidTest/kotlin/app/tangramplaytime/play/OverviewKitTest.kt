// Acceptance kit (WO-004, TASK-019): the grid overview, reaching every puzzle, and the top bar's "always"
// (designs/WO-004-design.md §2 S-1Δ, S-2Δ, S-13Δ, S-14Δ cell keys, S-22 "Overview" and hit order 0, "What survives";
// decisions.md 2026-10-15 "The grid overview, as drawn" and its amendment). The grid is opened by a tap on the counter zone
// and a cell chosen by a tap on it, after a semantics scroll brings it on screen (the grid scrolls vertically, S-22); the
// controllers save through `Persistence` over the kit's store double, seeded where a line needs puzzles in every state.
// REQ-024 A2 lives here because its adversarial leg — the grid open — needs the overview TASK-021 builds.
package app.tangramplaytime.play

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.IntRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PuzzleDefinition
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
import kotlin.math.max
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
        // Amended WO-010 (F2, recirculation 3 — a corrected check: the leg did not finish at 106 puzzles; every assertion,
        // threshold and message above unchanged). The first scroll to a cell below the first screenful never returned under
        // the paused clock (`KitScreen.scrollTo` says why and is the remedy). And the cells are judged a screenful at a time,
        // not by one scroll, settle and capture per cell: the grid is scrolled (the semantics scroll of S-22's vertically
        // scrolling grid) so that the first cell not yet judged, and the cells after it that fit, lie wholly in view; then
        // 500 ms on the paused clock (what each cell had) and one capture of `overview`. Each cell wholly in view and not yet judged is read from that capture at its
        // own bounds, 4 dp inside — the pixels its own capture holds (`captureToImage` copies the window at a node's bounds) —
        // and its PuzzleIdKey and PuzzleStateKey from its node of the same read. Every listed puzzle's cell is judged exactly
        // once: "every" stays every (decisions.md 2026-10-19 "The tests that pinned the 13-puzzle bundle").
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
        val judged = LinkedHashSet<String>()
        val started = SystemClock.elapsedRealtime()
        var screenfuls = 0
        while (judged.size < list.size) {
            val pending = list.filter { it.id !in judged }
            val view = screenfulOf(kit, pending.map { it.id })
            screenfuls++
            val judging = SystemClock.elapsedRealtime()
            var here = 0
            for (p in pending) {
                val (node, at) = view.cells[p.id] ?: continue
                judgeCell(kit, p, (seed[p.id] ?: PuzzleProgress.FRESH).state, open.id, node, at, view.image)
                judged += p.id
                here++
            }
            check(pending.first().id in judged) { "kit: `${KitTag.overviewCell(pending.first().id)}` is not wholly in view after the scroll" }
            android.util.Log.i(
                "KitScreen",
                "overview A2: screenful $screenfuls judged $here cells (${judged.size} / ${list.size}): scroll and settle ${view.settleMs} ms, " +
                    "capture ${view.captureMs} ms, judging ${SystemClock.elapsedRealtime() - judging} ms",
            )
        }
        assertEquals("every listed puzzle's cell judged once", list.map { it.id }.sorted(), judged.sorted())
        android.util.Log.i("KitScreen", "overview A2: ${list.size} cells in $screenfuls screenfuls, ${SystemClock.elapsedRealtime() - started} ms")
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
            val gridFrom = SystemClock.elapsedRealtime() // run-time record only (WO-010 F2): the grid choices, then the › walk
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
            val walkFrom = SystemClock.elapsedRealtime()
            val seen = ArrayList<String>()
            repeat(n - 1) {
                kit.press(KitTag.NAV_NEXT)
                seen += kit.shownId()
            }
            assertEquals("› from ${origin.id} shows every other puzzle once", list.map { it.id }.filter { it != origin.id }.sorted(), seen.sorted())
            kit.press(KitTag.NAV_NEXT)
            kit.assertShows(origin.id, "› comes round to ${origin.id}")
            android.util.Log.i("KitScreen", "REQ-003 A1 origin ${i + 1} / $n ${origin.id}: grid choices ${walkFrom - gridFrom} ms, › walk ${SystemClock.elapsedRealtime() - walkFrom} ms")
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
        kit.scrollTo(lastCell) // WO-010 F2: a scroll that returns under the paused clock (KitScreen.scrollTo)
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

    /** Scrolls [id]'s cell on screen (a semantics scroll of the grid, `KitScreen.scrollTo` since WO-010 F2) and taps its centre. */
    private fun chooseCell(kit: KitScreen, id: String) {
        val tag = KitTag.overviewCell(id)
        kit.scrollTo(tag)
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

    // ── REQ-050 A2 a screenful at a time (WO-010 F2) ─────────────────────────────────────────────────────

    /** One screenful of the grid: the capture of `overview`, and each wholly visible pending cell with its node and its box in that capture. */
    private class Screenful(val image: PixelMap, val cells: Map<String, Pair<SemanticsNode, IntRect>>, val settleMs: Long, val captureMs: Long)

    /**
     * Brings the first of [pending] (ids in ‹ › order) and the cells after it that fit wholly into the grid's view: when the
     * first is not wholly in view, a semantics scroll (`KitScreen.scrollTo`, S-22's scrolling grid) to the last pending cell
     * whose bottom lies within the grid's viewport height of the first one's top; then 500 ms on the paused clock (what each
     * cell had before WO-010 F2); if the first is still not wholly in view, a scroll to it and 500 ms more. Then one capture of
     * `overview`, and each pending cell wholly in view with its node and its box in that capture (the rounded node bounds,
     * as `captureToImage` takes them).
     */
    private fun screenfulOf(kit: KitScreen, pending: List<String>): Screenful {
        val t0 = SystemClock.elapsedRealtime()
        val firstTag = KitTag.overviewCell(pending.first())
        var nodes = cellNodes(kit)
        val first = nodes[firstTag] ?: throw AssertionError("kit: no `$firstTag` node")
        if (!whollyInView(first)) {
            // − 2 px: the 1 px `whollyInView` allows at each end; the scroll puts the last cell's bottom at the view's bottom
            // (ui-test 1.12.1 `performScrollTo`; platform-notes/android.md § Compose test kit, WO-010 "Under a paused clock …")
            val room = scrollParent(first, firstTag).boundsInRoot.height - 2f
            val top = first.positionInRoot.y
            val last = pending.map { KitTag.overviewCell(it) }
                .lastOrNull { t -> nodes[t]?.let { it.positionInRoot.y + it.size.height - top <= room } == true } ?: firstTag
            kit.scrollTo(last)
        }
        kit.advance(500)
        nodes = cellNodes(kit)
        if (!whollyInView(nodes[firstTag] ?: throw AssertionError("kit: no `$firstTag` node after the scroll"))) {
            android.util.Log.i("KitScreen", "overview A2: `$firstTag` not wholly in view after the screenful scroll; scrolled to it")
            kit.scrollTo(firstTag)
            kit.advance(500)
            nodes = cellNodes(kit)
        }
        val t1 = SystemClock.elapsedRealtime()
        val overview = kit.node(KitTag.OVERVIEW).boundsInRoot
        val image = kit.capture(KitTag.OVERVIEW)
        val t2 = SystemClock.elapsedRealtime()
        val ox = overview.left.roundToInt()
        val oy = overview.top.roundToInt()
        val out = LinkedHashMap<String, Pair<SemanticsNode, IntRect>>()
        for (id in pending) {
            val n = nodes[KitTag.overviewCell(id)] ?: continue
            if (!whollyInView(n)) continue
            val b = n.boundsInRoot
            out[id] = n to IntRect(b.left.roundToInt() - ox, b.top.roundToInt() - oy, b.right.roundToInt() - ox, b.bottom.roundToInt() - oy)
        }
        return Screenful(image, out, t1 - t0, t2 - t1)
    }

    /** Every `overview-cell-<id>` node of the unmerged tree, by tag (one walk). */
    private fun cellNodes(kit: KitScreen): Map<String, SemanticsNode> = kit.nodesWithTagPrefix(KitTag.OVERVIEW_CELL_PREFIX).toMap()

    /** The node's bounds as the grid clips them hold its whole size, within 1 px (the test `chooseCell` makes after a scroll). */
    private fun whollyInView(n: SemanticsNode): Boolean {
        // 1 px: the tolerance of `chooseCell`'s existing check after a scroll (WO-004 TASK-019: `visible.height >= n.size.height - 1f`)
        val shown = n.boundsInRoot
        return shown.width >= n.size.width - 1f && shown.height >= n.size.height - 1f
    }

    /** The nearest ancestor of [n] that scrolls: the node `performScrollTo` scrolls (S-22's vertically scrolling grid). */
    private fun scrollParent(n: SemanticsNode, tag: String): SemanticsNode {
        var p = n.parent
        // The closest ancestor with `ScrollBy`: the node `performScrollTo` itself scrolls (ui-test 1.12.1 `scrollToNode`,
        // `findClosestParentNode { hasScrollAction() }`); S-22's `verticalScroll` grid (designs/WO-004-design.md §2).
        while (p != null && SemanticsActions.ScrollBy !in p.config) p = p.parent
        return p ?: throw AssertionError("kit: `$tag` has no scrolling ancestor (S-22: the grid scrolls vertically)")
    }

    private fun <T> keyOf(n: SemanticsNode, tag: String, k: SemanticsPropertyKey<T>): T =
        n.config.getOrNull(k) ?: throw AssertionError("kit: `$tag` carries no ${k.name}")

    /**
     * REQ-050 A2's reading of one cell, the oracle of the test's comment unchanged: its PuzzleIdKey and PuzzleStateKey from
     * [node]; its pixels from [image] inside [at], 4 dp in, against the state the store holds ([expected]).
     */
    private fun judgeCell(kit: KitScreen, p: PuzzleDefinition, expected: PuzzleState, openId: String, node: SemanticsNode, at: IntRect, image: PixelMap) {
        val tag = KitTag.overviewCell(p.id)
        assertEquals("`$tag` PuzzleIdKey", p.id, keyOf(node, tag, PlaySemantics.PuzzleIdKey))
        assertEquals("`$tag` PuzzleStateKey: the store's state", expected, keyOf(node, tag, PlaySemantics.PuzzleStateKey))
        val inset = kit.dp(4f).roundToInt()
        val x0 = at.left + inset
        val y0 = at.top + inset
        val x1 = at.right - inset
        val y1 = at.bottom - inset
        check(x0 >= 0 && y0 >= 0 && x1 <= image.width && y1 <= image.height) { "kit: `$tag` at $at lies outside the `overview` capture (${image.width} × ${image.height} px)" }
        val n = max(0, x1 - x0) * max(0, y1 - y0)
        check(n > 1000) { "kit: `$tag` capture ${at.width} × ${at.height} px leaves $n px 4 dp inside" }
        val art = kitDistinctArtColours(p.art)
        var artPx = 0
        var flatPx = 0
        var accentPx = 0
        for (y in y0 until y1) for (x in x0 until x1) {
            val c = image[x, y]
            if (art.any { kitNear(c, it, 6f / 255f) }) artPx++
            if (kitNear(c, PlayPalette.silhouette, 6f / 255f)) flatPx++
            if (kitNear(c, PlayPalette.accent, 24f / 255f)) accentPx++
        }
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
        if (expected == PuzzleState.NEW && p.id != openId) assertTrue("a New cell has no dot: $report", accentPx <= 2)
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
