// Acceptance kit (WO-006, TASK-030): the Phone layout in the real app (REQ-035; TYPE-007; tasks.md § WO-006 S-31, S-32, S-33,
// S-35, seam notes 2–4, "What survives a rotation"; decisions.md 2026-10-17 Q1, Q3, Q4 rows).
// - Where it runs: every image. REQ-035 A1's 390 × 844 dp phone is constructed by a display override (1170 × 2532 px at
//   480 dpi, read back from the launched activity) on the dev, floor and tablet images; the device's own size runs too where
//   the device is a phone (the dev and floor images). REQ-035 A2's evidence is the dev image with gesture navigation (the
//   channel line records `navigation_mode` 2); the same assertion runs on every image (the per-image table).
// - The app's controller is one per process and shared by every class of the run: each test leaves it, and the device, as
//   it found them (seam note 3; `KitApp.restoreRule()`).
// - Insets: REQ-035 rule 3 binds the live insets, and after a display change (the override, its restore before the own-size
//   leg) the bars settle seconds later, so every value derived from them is read once they have settled
//   (`KitApp.settledInsets`) and checked unchanged after the node reads (decisions.md 2026-10-18 "Worker wins three
//   contests", part 2).
// - The keyboard carry of the TASK-030 brief lives in its own class, `DevPromptKeyboardKitTest` (decisions.md 2026-10-17
//   "The device kit's open readings", ruling 2: it needs the process-wide aid still locked).
package app.tangramplaytime.app

import android.app.UiAutomation
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.play.devaid.DevAidTags
import app.tangramplaytime.store.ProgressStores
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class PhoneLayoutKitTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val kit = KitApp(composeRule)

    @get:Rule(order = 1)
    val kitRestore: TestRule = kit.restoreRule()

    @Test // REQ-035.A1
    fun onA390By844DpPhoneAllSevenPiecesAreVisibleInTheTrayWithoutScrolling() {
        // REQ-035 A1: "On a 390 × 844 dp phone, all seven pieces are visible in the tray without scrolling." Statement: WHERE the
        // layout class is Phone, the tray is shown "in two rows" below the board. The phone is constructed (seam note 4), and the
        // device's own size runs too where it is a phone. On a full puzzle opened New (all seven in the tray), per size:
        //  - the display is that phone, drawn edge to edge (REQ-035 rule 3; S-35 step 4: `PlayScreen` fills the window):
        //    `play-screen` covers the window, and for the override it is 390 × 844 dp at the launched activity's density;
        //  - every `tray-piece-<ID>` of the seven is displayed, inside its own `tray-cell-<ID>` (tag table), inside the window and
        //    above the navigation-bar inset ("visible");
        //  - no node of the tree offers a scroll action or a scroll range ("without scrolling"); the grid, the one scrolling
        //    surface (designs/WO-004-design.md §2 S-22), is closed.
        kit.start("REQ-035 A1")
        val p = kit.firstNew(PuzzleKind.FULL, "a full puzzle opened New") { it.pieces.size == 7 }
        for ((leg, display) in phoneLegs()) {
            kit.usePhone(display, leg)
            kit.openByController(p.id, "$leg: ${p.id} New")
            assertEquals("$leg: ${p.id} is New, so all seven pieces are in the tray", PuzzleState.NEW, kit.shownState())
            assertTrue("$leg: the grid is closed (it is the one scrolling surface)", !kit.exists(KitAppTag.OVERVIEW))
            val density = kit.density(leg)
            val ins = kit.settledInsets(leg)
            val screen = kit.windowBounds(PlayTags.SCREEN)
            assertNear("$leg: `play-screen` covers the window (drawn edge to edge)", ins.window, screen, 1f)
            if (display == KIT_PHONE_390) {
                assertEquals("$leg: `play-screen` width in dp (density $density)", 390f, screen.width / density, 1f / density + 0.01f)
                assertEquals("$leg: `play-screen` height in dp (density $density)", 844f, screen.height / density, 1f / density + 0.01f)
            }
            val navTop = ins.windowHeightPx - ins.navigationBars.bottom
            for (piece in PieceId.entries) {
                val tag = PlayTags.trayPiece(piece)
                assertTrue("$leg: `$tag` exists (a piece of ${p.id} in the tray)", kit.exists(tag))
                composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
                val box = kit.windowBounds(tag)
                val cell = kit.windowBounds(PlayTags.trayCell(piece))
                assertTrue("$leg: $piece's miniature $box lies inside its cell $cell", kitAppWithin(box, cell))
                assertTrue("$leg: $piece's miniature $box lies inside the window ${ins.window}", kitAppWithin(box, ins.window))
                assertTrue("$leg: $piece's miniature $box ends above the navigation-bar inset (top $navTop; $ins)", box.bottom <= navTop + 0.5f)
            }
            val scrolling = kit.allNodes().flatMap { n ->
                n.config.map { it.key.name }.filter { it.contains("Scroll") }.map { "${n.config.tagOrUntagged()}: $it" }
            }
            assertTrue("$leg: no node offers a scroll action or a scroll range while the Play screen shows: $scrolling", scrolling.isEmpty())
            kit.assertInsetsStill(ins, "$leg, after the tray reads")
        }
    }

    @Test // REQ-035.A2
    fun withGestureNavigationNoTrayCellLiesUnderTheGestureBar() {
        // REQ-035 A2: "With gesture navigation, no tray cell lies under the gesture bar." Rule 3: "the tray stays clear of the
        // gesture bar". Oracle (tasks.md § WO-006 S-32: "Every cell … lie[s] inside the safe area"; the TASK-030 brief): with a
        // full puzzle open, every `tray-cell-<ID>` ends at or above the window's bottom minus the navigation-bar inset, and that
        // inset is greater than 0 (a zero inset would make the comparison vacuous). The message carries `navigation_mode` (2 is
        // gesture navigation): the dev image's run is this line's evidence; every image runs the same assertion.
        kit.start("REQ-035 A2")
        val p = kit.firstNew(PuzzleKind.FULL, "a full puzzle") { it.pieces.size == 7 }
        val mode = kit.navigationMode()
        for ((leg, display) in phoneLegs()) {
            kit.usePhone(display, leg)
            kit.openByController(p.id, "$leg: ${p.id}")
            val ins = kit.settledInsets(leg)
            assertTrue("$leg: the navigation-bar inset is greater than 0 (navigation_mode $mode; $ins)", ins.navigationBars.bottom > 0)
            val limit = ins.windowHeightPx - ins.navigationBars.bottom
            for (piece in p.pieces) {
                val cell = kit.windowBounds(PlayTags.trayCell(piece))
                assertTrue(
                    "$leg: `${PlayTags.trayCell(piece)}` $cell ends at or above the gesture bar's top $limit (navigation_mode $mode; $ins)",
                    cell.bottom <= limit + 0.5f,
                )
            }
            kit.assertInsetsStill(ins, "$leg, after the cell reads")
        }
    }

    @Test // pin: REQ-035 statement ("from top to bottom: the top bar, the board, and the tray in two rows") and rule 1 ("Tray row 1 holds the large and medium triangles; row 2 holds the square, parallelogram and small triangles"); TYPE-001 tray order ("left to right and top to bottom"); no token
    fun theTrayHasTwoRowsTrianglesFirstEachInTrayOrderBelowTheBoardBelowTheBar() {
        // Oracle, on a full puzzle at each phone size: every row-2 cell lies at or below every row-1 cell (their boxes do not
        // overlap vertically); within each row the cells' centres run left to right in TYPE-001 order; the top bar's zones end
        // above the board and the board ends above every cell. No shared row top is asserted (platform-notes: WO-003's
        // invented convention).
        kit.start("REQ-035 rule 1")
        val p = kit.firstNew(PuzzleKind.FULL, "a full puzzle") { it.pieces.size == 7 }
        val row1 = listOf(PieceId.LT1, PieceId.LT2, PieceId.MT)
        val row2 = listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)
        for ((leg, display) in phoneLegs()) {
            kit.usePhone(display, leg)
            kit.openByController(p.id, "$leg: ${p.id}")
            val cells = PieceId.entries.associateWith { kit.windowBounds(PlayTags.trayCell(it)) }
            for (a in row1) for (b in row2) {
                assertTrue("$leg: row-2 cell $b ${cells[b]} lies at or below row-1 cell $a ${cells[a]}", cells.getValue(b).top >= cells.getValue(a).bottom - 0.5f)
            }
            for (row in listOf(row1, row2)) {
                for ((a, b) in row.zipWithNext()) {
                    assertTrue("$leg: $a's cell lies left of $b's (TYPE-001 order): ${cells[a]} / ${cells[b]}", cells.getValue(a).center.x < cells.getValue(b).center.x)
                }
            }
            val board = kit.windowBounds(PlayTags.BOARD)
            for (zone in listOf(KitAppTag.NAV_PREV, KitAppTag.COUNTER, KitAppTag.NAV_NEXT)) {
                val z = kit.windowBounds(zone)
                assertTrue("$leg: the top bar's `$zone` $z ends above the board $board", z.bottom <= board.top + 0.5f)
            }
            for ((piece, cell) in cells) assertTrue("$leg: the board $board ends above $piece's cell $cell", board.bottom <= cell.top + 0.5f)
        }
    }

    @Test // pin: REQ-035 statement ("SHALL run in portrait only") and § Out of scope ("Phone landscape"); tasks.md § WO-006 "What survives a rotation" ("A phone does not turn: under a landscape rotation request its window stays taller than wide"); S-35 step 2; no token
    fun aPhoneStaysPortraitUnderALandscapeRotationRequest() {
        // Oracle, on the 390 × 844 dp phone: after `UiAutomation.setRotation(ROTATION_FREEZE_90)` and then `ROTATION_FREEZE_270`,
        // each read back from the settings and the device left to settle (the accessibility stream quiet), the activity's
        // window and `play-screen` stay taller than wide, the same puzzle shown.
        kit.start("REQ-035 portrait only")
        kit.usePhone(KIT_PHONE_390, "the 390 × 844 dp phone")
        val id = kit.shownId()
        for (rotation in listOf(UiAutomation.ROTATION_FREEZE_90, UiAutomation.ROTATION_FREEZE_270)) {
            val leg = "under a landscape rotation request ($rotation)"
            kit.freezeRotation(rotation, leg)
            kit.awaitQuiet(leg)
            kit.awaitScreen(leg)
            val (w, h) = kit.windowPx(leg)
            assertTrue("$leg: the window ${w}x$h px stays taller than wide (display rotation ${kit.displayRotation(leg)})", h > w)
            val s = kit.windowBounds(PlayTags.SCREEN)
            assertTrue("$leg: `play-screen` $s stays taller than wide", s.height > s.width)
            assertEquals("$leg: the same puzzle", id, kit.shownId())
        }
    }

    @Test // pin: REQ-035 rule 3 ("drawn edge to edge behind the system bars; … every control respects the system insets"); S-32 (the safe area: "Every cell, both badges and the solved row's thirds lie inside the safe area"; the top bar from the safe top, its columns inside the safe edges); S-35 steps 3–4 (insets = system bars ∪ display cutout; `PlayScreen` fills the window); no token
    fun theScreenIsDrawnEdgeToEdgeAndEveryControlLiesInsideTheSafeArea() {
        // Oracle, at each phone size: `play-screen` covers the window and the status-bar inset is greater than 0 (so "behind the
        // system bars" is not vacuous); ‹, the counter and › lie below the safe top and inside the safe sides; every tray cell,
        // Restart (In progress) and the solved row's Retry, best time and Next (Solved) lie inside the safe area. The safe area
        // is the settled insets' (decisions.md 2026-10-18 part 2), and each state's reads are checked against insets that
        // still read the same.
        kit.start("REQ-035 rule 3")
        val p = kit.firstNew(PuzzleKind.FULL, "a full puzzle") { it.pieces.size == 7 }
        for ((leg, display) in phoneLegs()) {
            kit.usePhone(display, leg)
            kit.openByController(p.id, "$leg: ${p.id} New")
            val ins = kit.settledInsets(leg)
            assertNear("$leg: `play-screen` covers the window", ins.window, kit.windowBounds(PlayTags.SCREEN), 1f)
            assertTrue("$leg: the system bars' top inset is greater than 0 ($ins)", ins.systemBars.top > 0)
            val safe = ins.safe
            for (zone in listOf(KitAppTag.NAV_PREV, KitAppTag.COUNTER, KitAppTag.NAV_NEXT)) {
                val z = kit.windowBounds(zone)
                assertTrue("$leg: the top bar's `$zone` $z lies below the safe top and inside the safe sides $safe", z.top >= safe.top - 0.5f && z.left >= safe.left - 0.5f && z.right <= safe.right + 0.5f)
            }
            for (piece in p.pieces) assertInsideSafe("$leg, New", PlayTags.trayCell(piece), safe)
            kit.assertInsetsStill(ins, "$leg, New")
            val first = kitAppLockOrder(p)?.first() ?: throw AssertionError("kit: no lock order for ${p.id}")
            kit.placeByController(first, "$leg: one piece placed")
            kit.awaitExists(KitAppTag.RESTART, "$leg: Restart while In progress")
            assertInsideSafe("$leg, In progress", KitAppTag.RESTART, safe)
            kit.assertInsetsStill(ins, "$leg, In progress")
            kit.solveByController("$leg: solved")
            kit.awaitSolvedRow("$leg: Solved")
            for (tag in listOf(KitAppTag.RETRY, KitAppTag.BEST_TIME, KitAppTag.SOLVED_NEXT)) assertInsideSafe("$leg, Solved", tag, safe)
            kit.assertInsetsStill(ins, "$leg, Solved")
            kit.onController { retry() }
            kit.idle()
        }
    }

    @Test // pin: REQ-035 rule 4 ("The Android back button closes an open overlay; otherwise it leaves the app normally, the state being already saved (REQ-025)"); decisions.md 2026-10-17 "Back closes the grid; no other surface is an open overlay"; designs/WO-004-design.md §2 S-20 (the save after each action); the grid's own Back is AppWiringKitTest's; no token
    fun backWithNothingOpenLeavesTheAppAndTheBoardIsAlreadySaved() {
        // Oracle: one piece placed on a New full puzzle through S-2 (a set-up), nothing open (no grid, no DEV sheet); Back
        // (`Espresso.pressBackUnconditionally`) leaves the app: the activity is no longer resumed. The store already holds the
        // puzzle In progress with that piece at its placement (read through `ProgressStores.onDevice`, S-18, polled until it
        // does, failing at the timeout), and after a relaunch the app shows the same puzzle In progress with that piece, and
        // the store still holds it.
        kit.start("REQ-035 rule 4")
        val p = kit.firstNew(PuzzleKind.FULL, "a full puzzle") { it.pieces.size == 7 }
        kit.openByController(p.id, "${p.id} New")
        val one = kitAppLockOrder(p)?.first() ?: throw AssertionError("kit: no lock order for ${p.id}")
        kit.placeByController(one, "one piece placed before Back")
        assertTrue("nothing open: no grid", !kit.exists(KitAppTag.OVERVIEW))
        assertTrue("nothing open: no DEV sheet", !kit.exists(DevAidTags.SHEET))
        val original = composeRule.activityRule.scenario
        Espresso.pressBackUnconditionally()
        kit.await("Back to leave the app: the activity no longer resumed") {
            original.state != Lifecycle.State.RESUMED && original.state != Lifecycle.State.STARTED
        }
        val store = ProgressStores.onDevice(kit.target)
        kit.await("the store to hold ${p.id} In progress with ${one.piece} at $one (S-20: saved after each action)") {
            val saved = runBlocking { store.progress(p.id) }
            saved.state == PuzzleState.IN_PROGRESS && saved.placed.size == 1 && kitAppSame(saved.placed.single(), one)
        }
        // A stopped activity keeps its window attached, so it is closed before the relaunch: one `play-screen` at a time.
        if (original.state != Lifecycle.State.DESTROYED) original.close()
        val again = ActivityScenario.launch(MainActivity::class.java)
        kit.useRelaunched(again)
        try {
            kit.awaitScreen("the app relaunched after Back")
            assertEquals("after the relaunch: the same puzzle", p.id, kit.shownId())
            assertEquals("after the relaunch: In progress", PuzzleState.IN_PROGRESS, kit.shownState())
            val tag = PlayTags.boardPiece(one.piece)
            assertTrue("after the relaunch: `$tag`", kit.exists(tag))
            val shown = Placement(
                kit.key(tag, PlaySemantics.PieceKey), kit.key(tag, PlaySemantics.TurnKey),
                kit.key(tag, PlaySemantics.MirroredKey), kit.key(tag, PlaySemantics.CentreKey),
            )
            assertTrue("after the relaunch: ${one.piece} shown at $one, is $shown", kitAppSame(shown, one))
            val saved = runBlocking { store.progress(p.id) }
            assertTrue(
                "after the relaunch: the store still holds ${one.piece} In progress ($saved)",
                saved.state == PuzzleState.IN_PROGRESS && saved.placed.size == 1 && kitAppSame(saved.placed.single(), one),
            )
        } finally {
            // The controller's restore (seam note 3) then runs through `AppGraph.controller` alone: it needs no activity.
            again.close()
        }
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** REQ-035 A1's phone (constructed, seam note 4), then the device's own size where it is a phone (TYPE-007: below 600 dp). */
    private fun phoneLegs(): List<Pair<String, KitDisplay>> {
        val own = kit.ownDisplay()
        val legs = mutableListOf("the 390 × 844 dp phone (override)" to KIT_PHONE_390)
        if (own.smallestDp < 600f) legs += "the device's own size $own" to own
        return legs
    }

    private fun assertInsideSafe(what: String, tag: String, safe: Rect) {
        val b = kit.windowBounds(tag)
        assertTrue("$what: `$tag` $b lies inside the safe area $safe", kitAppWithin(b, safe))
    }

    private fun assertNear(what: String, expected: Rect, actual: Rect, tolPx: Float) {
        val ok = abs(expected.left - actual.left) <= tolPx && abs(expected.top - actual.top) <= tolPx &&
            abs(expected.right - actual.right) <= tolPx && abs(expected.bottom - actual.bottom) <= tolPx
        assertTrue("$what: expected $expected, was $actual (tolerance $tolPx px)", ok)
    }

    private fun SemanticsConfiguration.tagOrUntagged(): String = getOrNull(SemanticsProperties.TestTag) ?: "(untagged)"
}
