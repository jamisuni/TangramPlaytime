// Acceptance kit (WO-005, TASK-025): the DEV aid on the Play screen — the passcode prompt, the solution overlay and "Solve
// this puzzle now" through to the picture and the best time — with the pins of its touch geometry and of "What survives"
// (tasks.md § WO-005 › Seams S-25, S-26, S-27, S-28, S-10Δ2, S-22Δ2, the aid's tag table, the touch geometry, "What
// survives"; decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" and "Solve this puzzle now is an
// ordinary solve"). How the kit drives the aid:
// - `PlayScreen` is given a new `DevAid` through `KitScreen(…, devAid)` (S-26: "A new DevAid is locked, with solutionShown
//   false and the sheet NONE"). A recreation gives the new activity the same object, as `DevAidHook.installed` is built once
//   per process (S-25; "What survives").
// - Every touch is injected on `play-screen` (KitScreen). A pill of the sheet is tapped at its bounds' centre (the aid's tag
//   table: "the pill, which is its touch area"). The passcode is typed into `dev-aid-passcode` through its semantics text
//   input and read back as the field's editable text (tag table: "its editable text").
// - Every line is judged on the screen: nodes, keys, pixels. The aid's reads (S-26) are read on the main thread only as
//   cross-checks.
// - Texts are read only once the device language is English (G-10: English is the Contract wording; Finnish is never
//   asserted). The texts asserted are REQ-046's own quotes: "DEV", "Wrong passcode.", "Show the solution" and "Solve this
//   puzzle now" (S-29: "From REQ-046 (the Contract wording)"). The wording DI-1 gave the other strings is not asserted.
package app.tangramplaytime.play

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.play.devaid.DevAid
import app.tangramplaytime.play.devaid.DevAidTags
import app.tangramplaytime.play.devaid.DevSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/** The prefix every overlay node's tag starts with (the aid's tag table: `dev-solution-<ID>`, `dev-solution-label-<ID>`). */
private const val KIT_DEV_SOLUTION_PREFIX = "dev-solution-"

/** 300 ms on the paused clock before a capture or a geometry read: no REQ or seam row times the sheet or the overlay. */
private const val KIT_AID_SETTLE_MS = 300L

/** The host heights the hit-order-3 pin scans (dp, at the full width), in 8 dp steps; see that pin's comment. */
private const val KIT_CORNER_SCAN_FROM_DP = 360f
private const val KIT_CORNER_SCAN_TO_DP = 720f

/**
 * The hit-order-3 pin's kit-owned puzzle: a full set (all seven pieces once, TYPE-001) shaped as a 2 : 1 wall, 4√2 × 2√2
 * units, in steps of √2. LT1 and LT2 fill the left 2 × 2 square, LT1's right angle at the wall's bottom-left corner with its
 * legs along the bottom and left edges; SQ, ST2, PG, ST1 and MT fill the right one (the tiling `TopBarKitTest`'s staircase
 * uses, moved one square right). LT2 shares an edge with SQ, so the puzzle is connected, with no hole. It meets the
 * IPuzzleLibrary KDoc's validity rules as a test fixture: no overlap (the pin checks that the engine locks every piece at its
 * solution pose), rating 3, a picture (its base colour), an `en` and a `fi` title; `reviewedByHuman` false.
 */
private val kitDevCornerPuzzle: PuzzleDefinition by lazy {
    val s = sqrt(2.0)
    fun at(vararg steps: Pair<Int, Int>): List<Point> = steps.map { (x, y) -> Point(x * s, y * s) }
    PuzzleDefinition(
        id = "kit-dev-corner-wall",
        titles = mapOf("en" to "Wall", "fi" to "Muuri"),
        category = "shapes",
        rating = 3,
        kind = PuzzleKind.FULL,
        solution = listOf(
            SolvedPiece(PieceId.LT1, at(0 to 0, 0 to 2, 2 to 2)),
            SolvedPiece(PieceId.LT2, at(0 to 0, 2 to 0, 2 to 2)),
            SolvedPiece(PieceId.MT, at(2 to 2, 3 to 1, 4 to 2)),
            SolvedPiece(PieceId.SQ, at(2 to 0, 3 to 0, 3 to 1, 2 to 1)),
            SolvedPiece(PieceId.PG, at(3 to 0, 3 to 1, 4 to 2, 4 to 1)),
            SolvedPiece(PieceId.ST1, at(2 to 1, 3 to 1, 2 to 2)),
            SolvedPiece(PieceId.ST2, at(3 to 0, 4 to 0, 4 to 1)),
        ),
        art = ArtLayer("#5C3620", emptyList()),
        reviewedByHuman = false,
    )
}

/** A library listing only [kitDevCornerPuzzle] (IPuzzleLibrary: fixed for the process; `byId` null for any other id). */
private val kitDevCornerLibrary: IPuzzleLibrary by lazy {
    object : IPuzzleLibrary {
        override val puzzles: List<PuzzleDefinition> = listOf(kitDevCornerPuzzle)
        override fun byId(id: String): PuzzleDefinition? = puzzles.firstOrNull { it.id == id }
    }
}

@RunWith(AndroidJUnit4::class)
class DevAidScreenKitTest {

    // The ComponentActivity host, declared so the recreation legs can recreate it (KitScreen.recreate).
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // ── REQ-046 A1: the passcode ───────────────────────────────────────────────────────────────────────

    @Test // REQ-046.A1
    fun entering1234IsRefusedAndEntering0417UnlocksTheAid() {
        // REQ-046 A1: "Entering 1234 is refused; entering 0417 unlocks the aid." Statement: the DEV button "asks for the
        // passcode 0417, and after the right passcode SHALL offer to show every piece's solution position on the silhouette
        // and to solve the current puzzle at once". Rule 1: "A wrong passcode shows "Wrong passcode." and reveals nothing."
        // Rule 2: "After the right passcode the aid stays unlocked until the page or app is restarted" (read: until the
        // process restarts; tasks.md § WO-005 "What survives").
        // Oracle, on the fresh start's New puzzle with a new (locked) aid:
        //  - a tap on `dev-button` (its text "DEV") opens the prompt: `dev-aid-prompt` with its field, OK and Cancel, and
        //    no menu and no message (tag table);
        //  - 1234, then OK: `dev-aid-message` reads "Wrong passcode."; no `dev-aid-menu`, no menu pill and no overlay node
        //    exists, so nothing is revealed; the prompt stays; the field is empty (touch geometry: "A wrong passcode empties
        //    it"). The same for the empty entry (decisions.md 2026-10-17 "The passcode prompt's open readings" (2): "OK on an
        //    empty field acts: it submits "" and S-26 refuses it with "Wrong passcode."") and for 417 (S-26: exactly "0417";
        //    417 is what a numeric comparison would let through). Before each OK the field holds the typed digits, so "empty"
        //    is read from the right node;
        //  - 0417, then OK: `dev-aid-menu` offers "Show the solution" (the REQ-046 A2 quote) and "Solve this puzzle now" (the
        //    REQ-046 A3 quote); the prompt and the message are gone;
        //  - rule 2's adversarial path (the brief's oracle rule): Done, DEV again (the menu at once), then a recreated
        //    activity over the same controller and aid (KitScreen.recreate), where DEV opens the menu at once, with no prompt.
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show()
        kitAssertEnglish()
        assertEquals("set-up: the fresh start's puzzle is New", PuzzleState.NEW, kit.shownState())
        assertTrue("set-up: `dev-button` on the open puzzle (tag table: present while the open puzzle is not Solved)", kit.exists(DevAidTags.DEV_BUTTON))
        assertEquals("the DEV button's text (REQ-046 statement \"a DEV button\"; tag table: its text, \"DEV\")", "DEV", kit.text(DevAidTags.DEV_BUTTON))
        assertFalse("set-up: no sheet before DEV (S-26: a new aid's sheet is NONE)", kit.exists(DevAidTags.SHEET))

        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertPrompt(kit, message = false, "a tap on DEV with the aid locked")
        assertEquals("the field is empty when the prompt opens", "", kitFieldText(kit))

        for (entry in listOf("1234", "", "417")) {
            val what = if (entry.isEmpty()) "the empty entry" else "the entry $entry"
            if (entry.isNotEmpty()) {
                kitType(kit, entry)
                assertEquals("$what typed: the field holds ${entry.length} characters (the fixture reads the right node)", entry.length, kitFieldText(kit).length)
            }
            kit.press(DevAidTags.OK)
            kitAssertPrompt(kit, message = true, "$what, then OK")
            assertEquals("$what refused: the message (REQ-046 rule 1)", "Wrong passcode.", kit.text(DevAidTags.MESSAGE))
            kitAssertNothingRevealed(kit, "$what refused")
            assertEquals("$what refused: the field is empty (touch geometry: \"A wrong passcode empties it\")", "", kitFieldText(kit))
            assertFalse("$what refused: the aid is still locked (S-26 `unlocked`)", kit.onMain { aid.unlocked })
        }

        kitType(kit, "0417")
        assertEquals("0417 typed: the field holds 4 characters", 4, kitFieldText(kit).length)
        kit.press(DevAidTags.OK)
        kitAssertMenu(kit, showOffered = true, "0417, then OK")
        assertFalse("0417 accepted: no message", kit.exists(DevAidTags.MESSAGE))
        assertTrue("0417 accepted: the aid is unlocked (S-26 `unlocked`)", kit.onMain { aid.unlocked })

        // REQ-046 rule 2: the aid stays unlocked — in this activity, then in a recreated one.
        kit.press(DevAidTags.DONE)
        assertFalse("Done closes the menu (touch geometry: \"A tap on the scrim, Cancel or Done closes it\")", kit.exists(DevAidTags.SHEET))
        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertMenu(kit, showOffered = true, "DEV again in the same activity (S-26 onButton: MENU when unlocked)")
        kit.press(DevAidTags.DONE)
        kit.recreate(rule.activityRule.scenario)
        assertFalse("the first frame of a recreated activity: no sheet (it was closed)", kit.exists(DevAidTags.SHEET))
        assertTrue("the first frame of a recreated activity: `dev-button`", kit.exists(DevAidTags.DEV_BUTTON))
        kit.frames()
        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertMenu(kit, showOffered = true, "DEV in a recreated activity (REQ-046 rule 2: the aid stays unlocked)")
        kit.press(DevAidTags.DONE)
    }

    // ── REQ-046 A2: the solution overlay ───────────────────────────────────────────────────────────────

    @Test // REQ-046.A2
    fun showTheSolutionDrawsOneShapePerPieceOfThePuzzleOnTheSilhouette() {
        // REQ-046 A2: ""Show the solution" draws one shape per piece of the puzzle on the silhouette." Rule 3: "The solution
        // overlay draws each piece's stored solution position in its colour with its id". Oracle, after a tap on "Show the
        // solution" in the unlocked menu, on a New puzzle (nothing placed covers the overlay: the touch geometry's z-order is
        // board → silhouette → the overlay → … board pieces), once on a mini and once on a full puzzle (their piece counts
        // differ, so the fixture tells "one per piece of the puzzle" from a fixed seven):
        //  - the menu closes (S-26: toggleSolution "flips solutionShown and closes");
        //  - the `dev-solution-` nodes are exactly one shape and one label per piece of the puzzle (tag table:
        //    `dev-solution-<ID>` "one overlay shape per piece of the open puzzle"; `dev-solution-label-<ID>` "with its shape");
        //  - each shape carries its PieceKey; its bounds are the px box of the piece's stored solution polygon through
        //    `board`'s BoardMappingKey, within 1 px (tag table: "the px box of the piece's solution polygon"; G-17), and lie in
        //    the `silhouette` node's box (the statement: "on the silhouette");
        //  - each label reads its piece's id (rule 3 "with its id"; tag table: "its text, PieceId.name");
        //  - the colour (rule 3 "in its colour"; touch geometry: "The fill is PlayPalette.piece(id) at 55 % alpha"): every
        //    board pixel 4 dp or more inside the polygon, clear of its label and of the DEV button (drawn above the overlay),
        //    is the piece's colour at 55 % over the silhouette colour (S-12 `PlayPalette`), within 10/255.
        // Between the two puzzles the same pill hides the overlay (tag table: the shapes are present while `solutionShown`
        // holds), so the full puzzle's overlay comes from its own Show, not from rule 3's carry-over.
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show()
        kitAssertEnglish()
        val mini = kitFirst(PuzzleKind.MINI)
        val full = kitFirst(PuzzleKind.FULL)
        assertNotEquals("the fixture: the mini and the full puzzle have different piece counts", mini.pieces.size, full.pieces.size)

        kit.open(mini.id)
        kitUnlock(kit)
        kit.press(DevAidTags.SHOW)
        assertFalse("Show closes the menu (S-26 toggleSolution)", kit.exists(DevAidTags.SHEET))
        assertTrue("Show: the overlay is on (S-26 `solutionShown`)", kit.onMain { aid.solutionShown })
        kit.advance(KIT_AID_SETTLE_MS)
        kitAssertOverlay(kit, mini, "Show on ${mini.id}")
        kitAssertOverlayColour(kit, mini, "Show on ${mini.id}")

        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertMenu(kit, showOffered = false, "DEV with the overlay on")
        kit.press(DevAidTags.SHOW)
        assertFalse("Hide closes the menu (S-26 toggleSolution)", kit.exists(DevAidTags.SHEET))
        kitAssertNoOverlay(kit, "the overlay hidden on ${mini.id}")

        kit.open(full.id)
        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertMenu(kit, showOffered = true, "DEV on ${full.id} with the overlay off")
        kit.press(DevAidTags.SHOW)
        kit.advance(KIT_AID_SETTLE_MS)
        kitAssertOverlay(kit, full, "Show on ${full.id}")
        kitAssertOverlayColour(kit, full, "Show on ${full.id}")
    }

    // ── REQ-046 A3: "Solve this puzzle now" ────────────────────────────────────────────────────────────

    @Test // REQ-046.A3
    fun solveThisPuzzleNowShowsTheSolvedPictureAndLeavesTheBestTimeEmpty() {
        // REQ-046 A3: ""Solve this puzzle now" shows the solved picture and leaves the best time empty." Statement: "solve
        // the current puzzle at once"; rule 4: "A puzzle solved by the aid is marked solved but does not set a best time."
        // Oracle, through a tap on "Solve this puzzle now" in the unlocked menu, once from New and once from In progress (S-27:
        // "First restart() if In progress"), over the kit's store double. The In-progress puzzle is seeded with a player's
        // board: one piece at its solution lock and one locked where no solution piece lies (a valid lock the engine found,
        // so the load check keeps it, S-21), so the solve must clear the board first; and with a puzzle time of 15 s and no
        // best time, so a rule that took the time as the best would show it.
        //  - at once: the menu is closed (S-26 solveNow: "closes, then S-27"), `play-screen`'s PuzzleStateKey reads Solved
        //    (the controller's state agrees) and `dev-button` does not exist (tag table: present while the open puzzle is
        //    not Solved; REQ-046 rule 5);
        //  - once REQ-023's timings have passed (the picture "fades in over 800 ms, starting 600 ms after the last piece
        //    locks"): `solved-picture` is displayed over the silhouette's box (tag table); 3 s after the solve, the
        //    confetti over, 3 px inside the silhouette no pixel is a TYPE-001 piece colour the picture does not name and none
        //    is the flat silhouette colour (REQ-023 statement: the picture replaces the coloured pieces; decisions.md
        //    2026-10-12 "The solve, as drawn": "the board pieces stop being composed once the picture is opaque");
        //  - the solved row's `best-time` holds the kit's no-time literal "–" (S-11Δ `noTime`; decisions.md 2026-10-15 "The
        //    top bar, Restart and the solved row, as drawn": "The best time reads "–" while none is stored"), and the
        //    controller's `bestSeconds` is null (S-1Δ);
        //  - once the writer has flushed, the store holds both puzzles Solved with no best time (rule 4; S-27 "the S-20 save
        //    through Persistence"; "The best time is never written: null stays null").
        val fromNew = kitFirst(PuzzleKind.FULL)
        val fromProgress = kitFirst(PuzzleKind.WARMUP)
        val right = kitSolutionLocks(fromProgress, 1)
        assertEquals("set-up: one solution lock on ${fromProgress.id}", 1, right.size)
        val board = right + kitLockOffTheSolution(fromProgress, right, KIT_ANY_DP)
        val seed = mapOf(fromProgress.id to kitInProgress(board, time = 15, best = null))
        val ps = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = fromNew.id))
        val aid = kitNewAid()
        val kit = KitScreen(rule, ps.controller, aid)
        kit.show()
        kitAssertEnglish()

        kit.assertShows(fromNew.id, "set-up: from New")
        assertEquals("set-up: ${fromNew.id} is New", PuzzleState.NEW, kit.shownState())
        kitUnlock(kit)
        kit.press(DevAidTags.SOLVE)
        kitAssertAidSolved(kit, fromNew, "an aid solve from New")

        kit.open(fromProgress.id)
        kit.assertShows(fromProgress.id, "set-up: from In progress")
        assertEquals("set-up: ${fromProgress.id} is In progress", PuzzleState.IN_PROGRESS, kit.shownState())
        val shown = kit.boardFromNodes()
        assertEquals("set-up: ${fromProgress.id}'s board as seeded: the pieces", board.map { it.piece }.sorted(), shown.map { it.piece }.sorted())
        for (b in board) assertTrue("set-up: ${fromProgress.id}: ${b.piece} at $b", kitSamePlacement(b, shown.single { it.piece == b.piece }))
        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertMenu(kit, showOffered = true, "DEV on ${fromProgress.id}, unlocked")
        kit.press(DevAidTags.SOLVE)
        kitAssertAidSolved(kit, fromProgress, "an aid solve from In progress")

        ps.flush()
        for (p in listOf(fromNew, fromProgress)) {
            val saved = ps.store.progressNow(p.id)
            assertEquals("${p.id}'s saved state after the aid solve (REQ-046 rule 4: marked solved)", PuzzleState.SOLVED, saved.state)
            assertNull("${p.id}'s saved best time after the aid solve (REQ-046 rule 4: no best time)", saved.bestSeconds)
        }
    }

    // ── Pins: the DEV button ───────────────────────────────────────────────────────────────────────────

    @Test // pin: REQ-046 ASSUMPTION "the DEV button sits in the bottom-left corner of the board, small and dashed"; touch geometry "Place: the board's bottom-left. Its left edge is 10 dp right of the board's left edge, and its bottom edge 10 dp above the board's bottom edge" and "Size: 28 dp high"; decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" (WO-005 checkpoint item 1); REQ-037 rule "The #DevTools controls (REQ-046) are testing aids, not player controls, and are exempt"; the brief's tolerances (within 16 dp of each edge, shorter than 48 dp); no token
    fun theDevButtonSitsInTheBoardsBottomLeftCornerAndIsSmall() {
        // Oracle, on a mini, a warm-up and a full puzzle (New): `dev-button` (its bounds are the drawn pill, tag table) lies
        // inside `board`, its left edge within 16 dp right of the board's left edge, its bottom edge within 16 dp above the
        // board's bottom edge, and it is less than 48 dp high.
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show()
        for (p in listOf(kitFirst(PuzzleKind.MINI), kitFirst(PuzzleKind.WARMUP), kitFirst(PuzzleKind.FULL))) {
            kit.open(p.id)
            val board = kit.bounds(PlayTags.BOARD)
            val button = kit.bounds(DevAidTags.DEV_BUTTON)
            val what = "${p.id}: `dev-button` $button on `board` $board"
            assertTrue("$what: inside the board", button.left >= board.left - 1f && button.right <= board.right + 1f && button.top >= board.top - 1f && button.bottom <= board.bottom + 1f)
            assertTrue("$what: its left edge ${(button.left - board.left) / kit.density} dp from the board's left (at most 16)", button.left - board.left <= kit.dp(16f))
            assertTrue("$what: its bottom edge ${(board.bottom - button.bottom) / kit.density} dp above the board's bottom (at most 16)", board.bottom - button.bottom <= kit.dp(16f))
            assertTrue("$what: ${button.height / kit.density} dp high (less than 48)", button.height < kit.dp(48f))
            assertTrue("$what: it has a size", button.width > 0f && button.height > 0f)
        }
    }

    @Test // pin: REQ-046 rule 5 "The DEV button is hidden on a solved puzzle"; tag table `dev-button` "Present while: the open puzzle is not Solved" ("A node's presence follows the aid's state and the controller at once"); touch geometry "It is neither drawn nor hit while the open puzzle is Solved (REQ-046 rule 5)" and "Place: … Its left edge is 10 dp right of the board's left edge, and its bottom edge 10 dp above the board's bottom edge"; S-22Δ2 hit order (2. "a third of the solved row" comes before 3. the DEV button); REQ-026 statement (Retry starts the puzzle again); no token
    fun theDevButtonIsNeitherDrawnNorHitOnASolvedPuzzleAndReturnsAfterRetry() {
        // Oracle: over the kit's store double holding the second puzzle Solved, the first (New) shows `dev-button`. › onto the
        // solved one: no `dev-button` in its first frame nor over the next 60 frames; a tap where the button would be drawn on
        // the solved puzzle opens no sheet (S-26 `sheet` stays NONE) and the puzzle stays Solved. Retry (New again) brings the
        // button back.
        // Where the button would be (kit finding, WO-005 TASK-026 arbitration): the solved puzzle's own `board` decides it, not
        // the first puzzle's — each puzzle's tray height sets its board's bottom, and the solved row replaces that tray strip
        // (decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn"), so the first puzzle's button box can
        // lie in the solved puzzle's Retry third, which hit order 2 rightly puts first. The box is the drawn pill's size, read
        // from `dev-button` on the first puzzle (tag table: its bounds are the drawn pill), placed by the touch geometry's
        // offsets on the solved puzzle's board; its centre must lie inside `board` and outside `solved-row`, or the fixture
        // fails loudly.
        val list = kitLibrary.puzzles
        val solved = list[1]
        val ps = kitControllerOver(rule, KitDeviceStore(mapOf(solved.id to kitSolvedProgress()), lastOpened = list[0].id))
        val aid = kitNewAid()
        val kit = KitScreen(rule, ps.controller, aid)
        kit.show()
        kit.assertShows(list[0].id, "set-up")
        assertTrue("set-up: `dev-button` on the New puzzle ${list[0].id}", kit.exists(DevAidTags.DEV_BUTTON))
        val pill = kit.bounds(DevAidTags.DEV_BUTTON).size
        kit.tap(kit.bounds(KitTag.NAV_NEXT).center)
        kit.assertShows(solved.id, "› onto the solved puzzle")
        assertEquals("set-up: ${solved.id} is Solved", PuzzleState.SOLVED, kit.shownState())
        assertFalse("the first frame of the solved puzzle: no `dev-button`", kit.exists(DevAidTags.DEV_BUTTON))
        for (f in 1..60) {
            kit.frames()
            assertFalse("the solved puzzle, frame $f: no `dev-button`", kit.exists(DevAidTags.DEV_BUTTON))
        }
        val board = kit.bounds(PlayTags.BOARD)
        val where = Rect(Offset(board.left + kit.dp(10f), board.bottom - kit.dp(10f) - pill.height), pill)
        val at = where.center
        if (!board.contains(at)) throw AssertionError("kit: the DEV button's place $where on ${solved.id} has its centre outside `board` $board")
        val row = if (kit.exists(KitTag.SOLVED_ROW)) kit.bounds(KitTag.SOLVED_ROW) else null
        if (row != null && row.contains(at)) throw AssertionError("kit: the DEV button's place $where on ${solved.id} has its centre in `solved-row` $row (hit order 2 comes first)")
        kit.tap(at)
        kit.frames()
        assertFalse("a tap at $at, where the DEV button would be drawn, on a solved puzzle opens no sheet", kit.exists(DevAidTags.SHEET))
        assertEquals("…and the aid's sheet stays NONE (S-26 `sheet`)", DevSheet.NONE, kit.onMain { aid.sheet })
        assertEquals("…and the puzzle stays Solved", PuzzleState.SOLVED, kit.shownState())
        kit.press(KitTag.RETRY)
        assertEquals("Retry: New", PuzzleState.NEW, kit.shownState())
        assertTrue("after Retry the puzzle is not Solved, and `dev-button` is back", kit.exists(DevAidTags.DEV_BUTTON))
    }

    @Test // pin: touch geometry "Taps. The DEV button is a control: it never drags. It acts at the lift within the 12 dp slop, whatever the duration. A slide of the slop or more cancels it, and it has no long press (S-22 "Taps and the long press")"; S-8 (a touch is a drag from the move at which it has moved ≥ the slop, "true from then on"); decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" ("acts at the lift, never drags, no long press"); no token
    fun theDevButtonActsAtTheLiftWhateverTheDurationAndASlideFromItDoesNothing() {
        // Oracle, on a full puzzle (New), a finger put down at the centre of `dev-button`:
        //  (1) slid 20 dp towards the board's centre and lifted there: no sheet; nothing dragged; still New;
        //  (2) slid 20 dp out and back onto the button, lifted there: no sheet (the slide cancelled it for good);
        //  (3) held still for 1500 ms: no sheet while it is down (no long press); at the lift, the prompt;
        //  (4) moved 6 dp (within the slop) and lifted: the prompt.
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val at = kit.bounds(DevAidTags.DEV_BUTTON).center
        val towards = kit.bounds(PlayTags.BOARD).center - at
        val len = hypot(towards.x, towards.y)
        val away = at + Offset(towards.x / len * kit.dp(20f), towards.y / len * kit.dp(20f))

        kit.touch { down(at) }
        kit.frames()
        kit.moveFinger(at, away, 4)
        kit.lift()
        kitAssertNoSheet(kit, aid, "(1) a 20 dp slide from the DEV button, lifted off it")
        assertEquals("(1) still New", PuzzleState.NEW, kit.shownState())
        assertTrue("(1) nothing dragged", PieceId.entries.none { kit.exists(PlayTags.dragPiece(it)) })

        kit.touch { down(at) }
        kit.frames()
        kit.moveFinger(at, away, 4)
        kit.moveFinger(away, at, 4)
        kit.lift()
        kitAssertNoSheet(kit, aid, "(2) a 20 dp slide out and back onto the DEV button")

        kit.holdDown(at, 1500)
        kitAssertNoSheet(kit, aid, "(3) the DEV button held still for 1500 ms, the finger still down")
        kit.lift()
        assertTrue("(3) the lift after a 1500 ms hold opens the prompt", kit.exists(DevAidTags.PROMPT))
        kit.press(DevAidTags.CANCEL)
        kitAssertNoSheet(kit, aid, "(3) Cancel")

        kit.touch { down(at) }
        kit.frames()
        kit.moveFinger(at, at + Offset(kit.dp(6f), 0f), 2)
        kit.lift()
        assertTrue("(4) a 6 dp move within the slop, lifted: the prompt", kit.exists(DevAidTags.PROMPT))
        kit.press(DevAidTags.CANCEL)
    }

    // ── Pins: the sheet ────────────────────────────────────────────────────────────────────────────────

    @Test // pin: touch geometry "The field shows masked digits, uses the system's number keyboard, and takes focus when the prompt opens"; S-26 "The passcode is a const in the aid folder and is never shown"; decisions.md 2026-10-17 "The passcode prompt's open readings" (3: "never drawn as visible text — no hint, label or placeholder shows 0417 … the semantics tree is not constrained … either masked-field kind is allowed"); no token
    fun thePromptTakesFocusAndThePasscodeIsNeverShown() {
        // Oracle: the prompt's field is focused once the prompt has opened (SemanticsProperties.Focused); no node of the
        // screen shows the passcode as a text or a content description — with the prompt open, with 0417 typed, in the menu
        // and with the overlay on. The field's own editable text is not read for this: the row leaves the semantics tree
        // unconstrained, and `BasicSecureTextField` reports the raw digits there while `BasicTextField` with a password
        // transformation reports bullets (both allowed; measured on the dev image, 2026-10-17).
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show()
        kit.press(DevAidTags.DEV_BUTTON)
        kit.frames(3)
        assertEquals("the passcode field takes focus when the prompt opens", true, kit.key(DevAidTags.PASSCODE, SemanticsProperties.Focused))
        kitAssertPasscodeNotShown(kit, "the prompt open")
        kitType(kit, "0417")
        kitAssertPasscodeNotShown(kit, "0417 typed")
        kit.press(DevAidTags.OK)
        assertTrue("set-up: 0417 opens the menu", kit.exists(DevAidTags.MENU))
        kitAssertPasscodeNotShown(kit, "the menu open")
        kit.press(DevAidTags.SHOW)
        kit.advance(KIT_AID_SETTLE_MS)
        kitAssertPasscodeNotShown(kit, "the overlay on")
    }

    @Test // pin: S-22Δ2 hit order 0 "The overview or the aid's sheet is open: the router neither consumes nor acts, on the top bar too"; touch geometry "The sheet. … The scrim (PlayPalette.scrim) and the sheet start below the top bar. The bar stays drawn and inert"; S-10Δ2 "SheetLayer comes last, over Slot(0, bar bottom, W, H − bar bottom)"; tag table `dev-aid-sheet` bounds "the screen below the top bar"; REQ-024 A2 (the name and the number always shown; `isDisplayed` cannot see occlusion — designs/WO-004-design.md §3); decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" ("below the top bar like the overview — the bar drawn and inert, the router standing aside"); WO-007 (TASK-055): S-53 "It is inert under hit orders 0 and 1" and decisions.md 2026-10-21 "The gear and the settings screen, as drawn." ("It is inert while a drag runs or the grid or the DEV sheet is open"), so the gear joins the bar's nodes; no token
    fun withTheSheetOpenTheBarIsDrawnAboveItAndNothingUnderItActs() {
        // Oracle, on a full puzzle (New), once with the prompt open and once with the menu open:
        //  - `dev-aid-sheet` is the screen below the top bar within 2 px, and no bar node (title, counter, ‹, ›, the gear)
        //    overlaps it;
        //  - with the menu open, the title's and the "n / N" text's pixels are what they were with the sheet closed. With the
        //    prompt open the field has focus and the soft keyboard may show, whose handling is WO-006's (decisions.md
        //    2026-10-17 "Solve this puzzle now is an ordinary solve", Carries: WO-006 "handles the soft keyboard under the
        //    passcode prompt"; "The passcode prompt's open readings" (4)), so the pixels are not compared there;
        //  - taps on ‹, ›, the counter and the gear change no puzzle, open no grid and no settings, and leave the sheet open
        //    (the bar is not the scrim);
        //  - a tap on a tray piece under the sheet does not turn it, and a 20 dp drag from it drags nothing and leaves the
        //    puzzle New (a tap there may land on the scrim and close the sheet, which then is opened again).
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        kit.advance(KIT_AID_SETTLE_MS)
        val closed = kitBarShots(kit)
        kit.press(DevAidTags.DEV_BUTTON)
        kit.advance(KIT_AID_SETTLE_MS)
        kitAssertSheetOwnsEveryTouch(kit, p, DevAidTags.PROMPT, null, "the prompt open")
        kit.press(DevAidTags.CANCEL)
        kitUnlock(kit)
        kit.advance(KIT_AID_SETTLE_MS)
        kitAssertSheetOwnsEveryTouch(kit, p, DevAidTags.MENU, closed, "the menu open")
        kit.press(DevAidTags.DONE)
    }

    @Test // pin: touch geometry "A tap on the scrim, Cancel or Done closes it (close())" and "The sheet is centred, inset 16 dp"; S-26 close() "NONE and wrongPasscode false (Cancel, Done, a tap on the scrim)" and onButton() "PROMPT when locked (with wrongPasscode false)"; decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" ("a tap on the scrim closes"); no token
    fun aTapOnTheScrimCancelOrDoneClosesTheSheet() {
        // Oracle, on the fresh start's New puzzle: the prompt closes by a tap on the scrim (4 dp inside the sheet's left edge,
        // level with the prompt's content and outside it) and by Cancel; after a refused entry and Cancel, DEV opens the prompt
        // with no message (the reset of wrongPasscode); once unlocked, the menu closes by the scrim and by Done. After each
        // close: no `dev-aid-sheet` and S-26 `sheet` NONE; the puzzle is the same, still New, nothing placed.
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show()
        val id = kit.shownId()
        fun kitAssertClosed(what: String) {
            kitAssertNoSheet(kit, aid, what)
            assertEquals("$what: the same puzzle", id, kit.shownId())
            assertEquals("$what: still New", PuzzleState.NEW, kit.shownState())
            assertTrue("$what: nothing placed", kit.placed().isEmpty())
        }
        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertPrompt(kit, message = false, "the prompt")
        kit.tap(kitScrimPoint(kit))
        kit.frames()
        kitAssertClosed("a tap on the scrim with the prompt open")

        kit.press(DevAidTags.DEV_BUTTON)
        kitType(kit, "1234")
        kit.press(DevAidTags.OK)
        kitAssertPrompt(kit, message = true, "1234 refused")
        kit.press(DevAidTags.CANCEL)
        kitAssertClosed("Cancel after a refused entry")
        assertFalse("Cancel resets the refusal (S-26 close(): wrongPasscode false)", kit.onMain { aid.wrongPasscode })
        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertPrompt(kit, message = false, "DEV after Cancel: the prompt with no message")

        kitType(kit, "0417")
        kit.press(DevAidTags.OK)
        kitAssertMenu(kit, showOffered = null, "0417 accepted")
        kit.tap(kitScrimPoint(kit))
        kit.frames()
        kitAssertClosed("a tap on the scrim with the menu open")
        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertMenu(kit, showOffered = null, "DEV, unlocked")
        kit.press(DevAidTags.DONE)
        kitAssertClosed("Done")
    }

    @Test // pin: S-22Δ2 hit order 1 "A drag is running: every further touch is a twist finger, a finger on the DEV button included"; decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed"; no token
    fun aSecondFingerOnTheDevButtonDuringADragOpensNothing() {
        // Oracle, on a full puzzle: ST1 picked up from the tray and held over the board's centre; a second finger tapping the
        // DEV button, then held on it for 700 ms, opens no sheet, and the drag goes on; the drag's own lift opens none either.
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        var finger = kit.pickUpFromTray(PieceId.ST1)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 4)
        val button = kit.bounds(DevAidTags.DEV_BUTTON)
        check(!button.contains(finger)) { "kit: the dragging finger $finger lies on the DEV button $button" }
        kit.touch { down(1, button.center) }
        kit.frames()
        kit.touch { up(1) }
        kit.frames(2)
        kitAssertNoSheet(kit, aid, "a second finger tapping the DEV button during a drag")
        assertTrue("the drag goes on after the second finger's tap", kit.exists(PlayTags.dragPiece(PieceId.ST1)))
        kit.touch { down(1, button.center) }
        kit.frames()
        kit.advance(700)
        kit.touch { up(1) }
        kit.frames(2)
        kitAssertNoSheet(kit, aid, "a second finger held 700 ms on the DEV button during a drag")
        assertTrue("the drag goes on after the second finger's hold", kit.exists(PlayTags.dragPiece(PieceId.ST1)))
        kit.release()
        kit.advance(300)
        kitAssertNoSheet(kit, aid, "the drag released")
    }

    // ── Pins: the DEV button over a piece, the overlay under one (constructed cases) ────────────────────

    @Test // pin: S-22Δ2 hit order 3 "The DEV button. It is drawn above every resting piece and badge, and the visible thing wins"; touch geometry z-order (… board pieces … → the DEV button) and "Taps. The DEV button is a control: it never drags"; decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" ("first in hit order 3 (drawn above every resting piece and badge — the visible thing wins)"; WO-005 checkpoint item 1: "where it sits over a piece's edge, a tap there opens the aid instead of picking up the piece") and "The passcode prompt's open readings" (5: constructed by the kit, not skipped); no token
    fun theDevButtonWinsOverARestingPieceDrawnUnderIt() {
        // Why the bundled puzzles on the full screen have no case: the silhouette's unit box is scaled to at most 80 % of the
        // board height and 82 % of its width, and centred (decisions.md 2026-10-11 "Board and tray drawing numbers from
        // DI-1/DI-2"), so its bottom lies at least 10 % of the board height above the board's bottom, while the button's top
        // lies 38 dp above it (touch geometry: 10 dp up, 28 dp high). A placed piece lies inside the silhouette (ILockEngine
        // KDoc, "valid"). The full-screen board is 577 dp high (dev image, 2026-10-17), so 57.7 dp ≥ 38 dp: no piece reaches
        // the button.
        // The case is constructed, not searched for in the bundle (platform-notes/android.md, the WO-004 lesson):
        // - the puzzle is kit-owned (`kitDevCornerPuzzle`, through S-1's `PlayController(library, engine)`): a 2 : 1 wall whose
        //   bottom-left corner is LT1's right angle, so on a board whose 82 % × 80 % box is 2 : 1 too the silhouette touches
        //   both of that box's margins at the corner the button sits in;
        // - the screen is hosted at the full width and a kit-chosen height (S-10: PlayScreen "fills `modifier`'s size"). The
        //   kit scans the heights from 360 to 720 dp in 8 dp steps, reads at each the `dev-button` node and LT1's solution
        //   polygon through BoardMapping, and keeps the height with the point deepest inside both (2 px inside the button,
        //   at least 2 dp inside LT1). Measured on the dev image (2026-10-17, the button's nominal 48 × 28 dp box, before the
        //   aid existed): the case exists for host heights 432–488 dp (boards 180–236 dp), deepest 16.4 dp at 456 dp. A scan
        //   that finds no such height fails loudly with its table; there is no skip.
        // Oracle, with LT1 placed at its solution pose through S-2 (In progress) and the point read again from its drawn shape
        // (its node keys through the engine):
        //  - a still tap at the point opens the prompt (the DEV button acts), and LT1 is neither turned nor shaken: its
        //    placement and the controller's `shake` are unchanged (a tap on a board piece would turn it or shake it, REQ-016);
        //  - Cancel; a 20 dp slide from the point towards the board's centre drags nothing (no `drag-piece` during the slide,
        //    LT1 still placed where it was) and opens no sheet.
        val p = kitDevCornerPuzzle
        check(kitSolutionLocks(p, p.pieces.size).size == p.pieces.size) { "kit: ${p.id} does not lock all its pieces at their solution poses (the engine's validity check of the kit-owned puzzle)" }
        val piece = PieceId.LT1
        val aid = kitNewAid()
        val kit = KitScreen(rule, PlayController(kitDevCornerLibrary, kitEngine), aid)
        val height = kitShowAtTheCornerHeight(kit, piece)
        check(kit.placeByApi(piece)) { "kit: $piece does not lock at its solution pose in ${p.id}" }
        kit.frames()
        kit.advance(KIT_AID_SETTLE_MS)
        assertEquals("set-up: $piece placed, In progress", PuzzleState.IN_PROGRESS, kit.shownState())
        assertTrue("set-up: `dev-button` on the open puzzle (not Solved)", kit.exists(DevAidTags.DEV_BUTTON))
        val button = kit.bounds(DevAidTags.DEV_BUTTON)
        val (at, depth) = kitDeepestUnderTheButton(kit, kit.boardPolygonPx(PlayTags.boardPiece(piece)), button)
        if (at == null || depth < kit.dp(2f)) throw AssertionError("kit: at $height dp $piece's drawn shape reaches no point 2 dp deep under `dev-button` $button (depth ${depth / kit.density} dp); `board` ${kit.bounds(PlayTags.BOARD)}, `silhouette` ${kit.bounds(PlayTags.SILHOUETTE)}")
        android.util.Log.i("KitScreen", "DEV over $piece: host $height dp, point $at, ${depth / kit.density} dp inside $piece; `dev-button` $button, `board` ${kit.bounds(PlayTags.BOARD)} (px, density ${kit.density})")
        val before = kit.placed()
        val shakeBefore = kit.onMain { kit.controller.shake }

        kit.tap(at)
        kit.frames()
        assertTrue("a still tap at $at, on the DEV button over $piece, opens the prompt (hit order 3: the DEV button first)", kit.exists(DevAidTags.PROMPT))
        assertEquals("…and $piece is not turned (its placement is unchanged)", before, kit.placed())
        assertEquals("…and $piece is not shaken (the controller's shake is unchanged)", shakeBefore, kit.onMain { kit.controller.shake })
        kit.press(DevAidTags.CANCEL)
        kitAssertNoSheet(kit, aid, "Cancel")

        val towards = kit.bounds(PlayTags.BOARD).center - at
        val len = hypot(towards.x, towards.y)
        val away = at + Offset(towards.x / len * kit.dp(20f), towards.y / len * kit.dp(20f))
        kit.touch { down(at) }
        kit.frames()
        for (i in 1..4) {
            kit.touch { moveTo(at + (away - at) * (i / 4f)) }
            kit.frames()
            assertTrue("a slide from the DEV button over $piece, move $i of 4: nothing is dragged (the DEV button never drags)", PieceId.entries.none { kit.exists(PlayTags.dragPiece(it)) })
        }
        kit.lift()
        kit.advance(KIT_AID_SETTLE_MS)
        assertEquals("after the slide $piece is where it was", before, kit.placed())
        kitAssertNoSheet(kit, aid, "a 20 dp slide from the DEV button over $piece")
    }

    @Test // pin: touch geometry z-order "board → silhouette → the overlay → Restart pill → board pieces → …"; decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" ("Overlay: … under the placed pieces, above the silhouette"; WO-005 checkpoint item 3: "pieces already placed cover their part") and "The passcode prompt's open readings" (5: "a piece locked at its solution place keeps its solid colour with the overlay on"); no token
    fun aPlacedPieceIsDrawnOverTheOverlay() {
        // Oracle, on the first full puzzle with the overlay on (S-12 colours; the board-piece reading of
        // `KitScreen.assertBoardDrawnAtItsKeys`: 3 px inside the drawn shape, within 16/255):
        //  (1) the first piece that locks at its solution pose on the empty board, placed through S-2: every board pixel 3 px
        //      inside its drawn shape (its node keys through the engine) is its solid colour, its overlay label's box included.
        //      At its own solution place the overlay's fill is its own colour, so what tells "under" from "over" there is the
        //      overlay's white label and dashed edge: the fixture checks that at least 4 samples fall in the label's box;
        //  (2) a second piece locked where no solution piece lies (the engine's lock, dropped at its own centre through S-2):
        //      every pixel 3 px inside its drawn shape and 4 dp inside another piece's solution polygon of a clearly different
        //      colour is its own solid colour, not that piece's overlay over it; at least 8 such samples.
        // The board badge's area (drawn over a placed PG) and the DEV button's box are left out of every sample.
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        kitUnlock(kit)
        kit.press(DevAidTags.SHOW)
        kit.advance(KIT_AID_SETTLE_MS)

        val first = p.pieces.firstOrNull { kit.placeByApi(it) } ?: throw AssertionError("kit: no piece of ${p.id} locks at its solution pose on an empty board")
        kit.frames()
        kit.advance(KIT_AID_SETTLE_MS)
        kitAssertOverlay(kit, p, "set-up: the overlay on with $first placed")
        val label = kit.bounds(DevAidTags.solutionLabel(first))
        val samples1 = kitAssertSolidOver(kit, first, null, "(1) $first at its solution place, the overlay on")
        val inLabel = samples1.count { label.contains(it) }
        assertTrue("(1) the fixture: $inLabel samples inside $first's overlay label box $label (at least 4)", inLabel >= 4)

        val dp = kit.onMain { kit.controller.boardDpPerUnit }
        val off = kitLockOffTheSolution(p, kit.placed(), dp)
        check(kitPlaceAt(kit, off)) { "kit: the drop of ${off.piece} at $off through S-2 did not lock there" }
        kit.frames()
        kit.advance(KIT_AID_SETTLE_MS)
        val others = p.solution.filter { it.piece != off.piece && kitDiffers(PlayPalette.piece(it.piece), PlayPalette.piece(off.piece)) }
        kitAssertSolidOver(kit, off.piece, others.map { sp -> sp.polygon.map { kit.toPx(it) } }, "(2) ${off.piece} placed off the solution over other pieces' overlay")
    }

    // ── Pins: the solve, browsing, a recreation ────────────────────────────────────────────────────────

    @Test // pin: decisions.md 2026-10-17 "Solve this puzzle now is an ordinary solve" ("The last lock is the ordinary solve: Solved, the full REQ-023 win (pop, confetti, the picture, the solved row)"; WO-005 checkpoint item 2); S-27 "The last lock is the ordinary solve (S-2 drop): Solved, a new Celebration"; REQ-023 rules (the picture fades in "starting 600 ms after the last piece locks"; "A short celebration (a 4-note chime and confetti lasting 2.4 s) plays with it"); tag table `confetti` "during the celebration; never with reduced motion"; no token
    fun anAidSolvePlaysTheCelebrationWithReducedMotionOff() {
        // Oracle, with reduced motion off, on the first mini: an aid solve; every 100 ms up to 1 s, no `solved-picture` up to
        // 500 ms and one at 700 ms; the `confetti` node appears at least once on that timeline; 2 s after the solve the
        // solved row is shown.
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show(reducedMotion = false)
        val p = kitFirst(PuzzleKind.MINI)
        kit.open(p.id)
        kitUnlock(kit)
        kit.press(DevAidTags.SOLVE)
        assertEquals("the aid solve: Solved", PuzzleState.SOLVED, kit.shownState())
        var confetti = false
        for (t in 0..10) {
            if (t > 0) kit.advance(100)
            if (kit.exists(PlayTags.CONFETTI)) confetti = true
            val picture = kit.exists(PlayTags.SOLVED_PICTURE)
            if (t <= 5) assertFalse("no `solved-picture` ${t * 100} ms after the aid solve (REQ-023: the fade starts at 600 ms)", picture)
            if (t >= 7) assertTrue("`solved-picture` ${t * 100} ms after the aid solve", picture)
        }
        assertTrue("the confetti appears within 1 s of the aid solve (the full REQ-023 win)", confetti)
        kit.advance(1000)
        kit.frames()
        assertTrue("the solved row 2 s after the aid solve", kit.exists(KitTag.SOLVED_ROW))
    }

    @Test // pin: REQ-046 rule 3 "The solution overlay … stays on while the player browses puzzles until it is switched off" (its adversarial path, the brief's oracle rule); touch geometry "It is not drawn while the open puzzle is Solved. solutionShown is kept, so the overlay returns on the next unsolved puzzle"; tag table `dev-solution-<ID>` "Present while: solutionShown holds and the puzzle is not Solved"; REQ-024 statement (‹ ›), REQ-050 statement (the long press, the grid); decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" ("off only through "Hide the solution""; WO-005 checkpoint item 3); no token
    fun theOverlayStaysOnWhileBrowsingUntilItIsSwitchedOff() {
        // Oracle, over the kit's store double holding puzzles 3 and 4 Solved (positions in ‹ › order), the overlay switched
        // on at puzzle 1: after ›, after a long press on › (to puzzle 5, the next unsolved), after a grid choice (puzzle 7),
        // the overlay is drawn for the shown puzzle's own pieces (the node set, PieceKeys and boxes). On a Solved puzzle
        // chosen in the grid: no overlay node and no `dev-button`, while S-26 `solutionShown` still holds; ‹ back to an
        // unsolved puzzle: the overlay again. Then "Hide the solution" (tag table; "The passcode prompt's open readings" (1)) switches it
        // off, and it stays off on the next unsolved puzzle reached by ›.
        val list = kitLibrary.puzzles
        assertTrue("the library lists at least seven puzzles (${list.size})", list.size >= 7)
        val seed = mapOf(list[2].id to kitSolvedProgress(), list[3].id to kitSolvedProgress())
        val ps = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = list[0].id))
        val aid = kitNewAid()
        val kit = KitScreen(rule, ps.controller, aid)
        kit.show()
        kitAssertEnglish()
        kit.assertShows(list[0].id, "set-up")
        kitUnlock(kit)
        kit.press(DevAidTags.SHOW)
        kit.advance(KIT_AID_SETTLE_MS)
        kitAssertOverlay(kit, list[0], "Show on puzzle 1")

        kit.press(KitTag.NAV_NEXT)
        kit.advance(KIT_AID_SETTLE_MS)
        kit.assertShows(list[1].id, "›")
        kitAssertOverlay(kit, list[1], "after › to puzzle 2")

        kit.holdDown(kit.bounds(KitTag.NAV_NEXT).center, 600)
        kit.lift()
        kit.frames()
        kit.advance(KIT_AID_SETTLE_MS)
        kit.assertShows(list[4].id, "a long press on › from puzzle 2 (puzzles 3 and 4 Solved)")
        kitAssertOverlay(kit, list[4], "after the long press on › to puzzle 5")

        kitChooseInGrid(kit, list[6].id)
        kit.assertShows(list[6].id, "the grid's cell of puzzle 7")
        kitAssertOverlay(kit, list[6], "after a grid choice of puzzle 7")

        kitChooseInGrid(kit, list[2].id)
        kit.assertShows(list[2].id, "the grid's cell of puzzle 3 (Solved)")
        assertEquals("set-up: puzzle 3 is Solved", PuzzleState.SOLVED, kit.shownState())
        kitAssertNoOverlay(kit, "on the Solved puzzle 3")
        assertFalse("on the Solved puzzle 3: no `dev-button` (REQ-046 rule 5)", kit.exists(DevAidTags.DEV_BUTTON))
        assertTrue("on the Solved puzzle 3 the overlay is still switched on (S-26 `solutionShown` is kept)", kit.onMain { aid.solutionShown })

        kit.press(KitTag.NAV_PREV)
        kit.advance(KIT_AID_SETTLE_MS)
        kit.assertShows(list[1].id, "‹ from the Solved puzzle 3")
        kitAssertOverlay(kit, list[1], "back on puzzle 2 after the Solved puzzle 3")

        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertMenu(kit, showOffered = false, "DEV with the overlay on")
        kit.press(DevAidTags.SHOW)
        kitAssertNoOverlay(kit, "after Hide on puzzle 2")
        assertFalse("after Hide: S-26 `solutionShown` is false", kit.onMain { aid.solutionShown })
        repeat(3) { kit.press(KitTag.NAV_NEXT) }
        kit.advance(KIT_AID_SETTLE_MS)
        kit.assertShows(list[4].id, "› three times from puzzle 2")
        kitAssertNoOverlay(kit, "on puzzle 5 after Hide (it stays off until it is switched on)")
    }

    @Test // pin: tasks.md § WO-005 "What survives" ("An activity recreation keeps the aid object. DevAidHook.installed is built once per process, so unlocked, solutionShown, the open sheet and its message survive; what was typed in the field may be lost"); REQ-046 rule 2 (stays unlocked) and rule 3 (the overlay stays on until switched off); decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" ("State: one aid object per process — survives a rotation or leaving the app"; WO-005 checkpoint item 4); no token
    fun aRecreationKeepsTheOverlayTheUnlockedStateAndTheOpenSheetWithItsMessage() {
        // Oracle:
        //  (1) on a full puzzle, unlocked, the overlay on, one piece placed through S-2, the menu open: in the first frame of
        //      a recreated activity over the same controller and aid the menu is still open and the overlay is drawn for
        //      every piece of the puzzle (the tag table: one shape per piece of the open puzzle, placed or not); Done, then DEV
        //      opens the menu at once;
        //  (2) a second, new aid composed into a recreated activity over the same controller: locked, no overlay; 1234
        //      refused shows "Wrong passcode."; in the first frame of another recreation the prompt and its message are still
        //      there, and no menu; 0417 then opens the menu.
        val p = kitFirst(PuzzleKind.FULL)
        val aid = kitNewAid()
        val kit = KitScreen(rule, devAid = aid)
        kit.show()
        kitAssertEnglish()
        kit.open(p.id)
        kitUnlock(kit)
        kit.press(DevAidTags.SHOW)
        kit.advance(KIT_AID_SETTLE_MS)
        val placed = p.pieces.firstOrNull { kit.placeByApi(it) } ?: throw AssertionError("kit: no piece of ${p.id} locks at its solution pose on an empty board")
        kit.frames()
        kit.advance(KIT_AID_SETTLE_MS)
        assertEquals("set-up: $placed placed", listOf(placed), kit.placed().map { it.piece })
        kitAssertOverlay(kit, p, "(1) set-up: the overlay on with $placed placed")
        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertMenu(kit, showOffered = false, "(1) set-up: the menu open")

        kit.recreate(rule.activityRule.scenario)
        kitAssertMenu(kit, showOffered = false, "(1) the first frame of a recreated activity: the open menu survives")
        kitAssertOverlay(kit, p, "(1) the first frame of a recreated activity: the overlay survives")
        assertTrue("(1) after the recreation: still unlocked (S-26 `unlocked`)", kit.onMain { aid.unlocked })
        kit.frames()
        kit.press(DevAidTags.DONE)
        kit.press(DevAidTags.DEV_BUTTON)
        kitAssertMenu(kit, showOffered = false, "(1) DEV after the recreation: the menu at once (REQ-046 rule 2)")
        kit.press(DevAidTags.DONE)

        val aid2 = kitNewAid()
        val kit2 = KitScreen(rule, kit.controller, aid2)
        kit2.recreate(rule.activityRule.scenario)
        kitAssertNoOverlay(kit2, "(2) a new aid: no overlay")
        kit2.frames()
        kit2.press(DevAidTags.DEV_BUTTON)
        kitAssertPrompt(kit2, message = false, "(2) DEV with a new aid: the prompt (S-26: a new aid is locked)")
        kitType(kit2, "1234")
        kit2.press(DevAidTags.OK)
        kitAssertPrompt(kit2, message = true, "(2) 1234 refused")
        kit2.recreate(rule.activityRule.scenario)
        kitAssertPrompt(kit2, message = true, "(2) the first frame of a recreated activity: the prompt and its message survive")
        assertEquals("(2) the message after the recreation (REQ-046 rule 1)", "Wrong passcode.", kit2.text(DevAidTags.MESSAGE))
        kit2.frames()
        kitType(kit2, "0417")
        kit2.press(DevAidTags.OK)
        kitAssertMenu(kit2, showOffered = true, "(2) 0417 after the recreation")
        kit2.press(DevAidTags.DONE)
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** A new aid, built on the main thread (S-26: "A new DevAid is locked, with solutionShown false and the sheet NONE"). */
    private fun kitNewAid(): DevAid = rule.runOnUiThread { DevAid() }

    /**
     * Fails loudly unless the device language is English: the kit reads the aid's texts in the language the screen resolves
     * them in, and only English is the Contract wording (G-10; brief "Oracle rule").
     */
    private fun kitAssertEnglish() {
        val locale = rule.activity.resources.configuration.locales[0]
        if (locale.language != "en") {
            throw AssertionError("the device language is ${locale.toLanguageTag()}: this kit reads the aid's English Contract wording (G-10) and never asserts another language; run it with the device language set to English")
        }
    }

    /** Types [text] into the passcode field through its semantics text input (tag table: "the masked numeric field"). */
    private fun kitType(kit: KitScreen, text: String) {
        kit.interaction(DevAidTags.PASSCODE).performTextInput(text)
        kit.frames()
    }

    /** The passcode field's editable text (tag table: `dev-aid-passcode` "its editable text"); fails when the node has none. */
    private fun kitFieldText(kit: KitScreen): String = kit.key(DevAidTags.PASSCODE, SemanticsProperties.EditableText).text

    /** The prompt is open, per the aid's tag table: the sheet, the prompt, its field and pills; the message as [message]; no menu. */
    private fun kitAssertPrompt(kit: KitScreen, message: Boolean, what: String) {
        for (tag in listOf(DevAidTags.SHEET, DevAidTags.PROMPT, DevAidTags.PASSCODE, DevAidTags.OK, DevAidTags.CANCEL)) {
            assertTrue("$what: `$tag` (tag table: present while PROMPT)", kit.exists(tag))
        }
        for (tag in listOf(DevAidTags.MENU, DevAidTags.SHOW, DevAidTags.SOLVE, DevAidTags.DONE)) {
            assertFalse("$what: no `$tag` (tag table: present while MENU)", kit.exists(tag))
        }
        assertEquals("$what: `${DevAidTags.MESSAGE}` (tag table: present while PROMPT and wrongPasscode)", message, kit.exists(DevAidTags.MESSAGE))
    }

    /**
     * The menu is open, per the aid's tag table: the sheet, the menu and its three pills; no prompt. Its labels: "Solve this
     * puzzle now" (REQ-046 A3's quote); "Show the solution" (REQ-046 A2's quote) when [showOffered] is true; "Hide the
     * solution" when it is false (tag table: "Show the solution", or "Hide the solution" while solutionShown; decisions.md
     * 2026-10-17 "The passcode prompt's open readings" (1): the tag table's quote is binding); not read when it is null.
     */
    private fun kitAssertMenu(kit: KitScreen, showOffered: Boolean?, what: String) {
        for (tag in listOf(DevAidTags.SHEET, DevAidTags.MENU, DevAidTags.SHOW, DevAidTags.SOLVE, DevAidTags.DONE)) {
            assertTrue("$what: `$tag` (tag table: present while MENU)", kit.exists(tag))
        }
        for (tag in listOf(DevAidTags.PROMPT, DevAidTags.PASSCODE, DevAidTags.MESSAGE, DevAidTags.OK, DevAidTags.CANCEL)) {
            assertFalse("$what: no `$tag` (tag table: present while PROMPT)", kit.exists(tag))
        }
        if (showOffered == null) return
        assertEquals("$what: the solve pill's label (REQ-046 A3)", "Solve this puzzle now", kit.text(DevAidTags.SOLVE))
        val show = kit.text(DevAidTags.SHOW)
        if (showOffered) {
            assertEquals("$what: the show pill's label with the overlay off (REQ-046 A2)", "Show the solution", show)
        } else {
            assertEquals("$what: the show pill's label with the overlay on (tag table: \"Hide the solution\" while solutionShown)", "Hide the solution", show)
        }
    }

    /** Rule 1 "reveals nothing": no menu, no menu pill, no overlay node. */
    private fun kitAssertNothingRevealed(kit: KitScreen, what: String) {
        for (tag in listOf(DevAidTags.MENU, DevAidTags.SHOW, DevAidTags.SOLVE)) assertFalse("$what: no `$tag` (REQ-046 rule 1: reveals nothing)", kit.exists(tag))
        val overlay = kit.nodesWithTagPrefix(KIT_DEV_SOLUTION_PREFIX).map { it.first }
        assertTrue("$what: no overlay node (REQ-046 rule 1: reveals nothing): $overlay", overlay.isEmpty())
    }

    /** No sheet node, and the aid's sheet NONE (S-26). */
    private fun kitAssertNoSheet(kit: KitScreen, aid: DevAid, what: String) {
        assertFalse("$what: no `${DevAidTags.SHEET}`", kit.exists(DevAidTags.SHEET))
        assertEquals("$what: the aid's sheet (S-26 `sheet`)", DevSheet.NONE, kit.onMain { aid.sheet })
    }

    /** DEV on the locked aid, 0417 and OK: the menu (REQ-046 statement; S-26 submit). A set-up for the lines after A1. */
    private fun kitUnlock(kit: KitScreen) {
        kit.press(DevAidTags.DEV_BUTTON)
        check(kit.exists(DevAidTags.PROMPT)) { "kit: DEV on the locked aid opened no prompt" }
        kitType(kit, "0417")
        kit.press(DevAidTags.OK)
        check(kit.exists(DevAidTags.MENU)) { "kit: 0417 and OK opened no menu" }
    }

    /**
     * The overlay is drawn for exactly [p]'s pieces (the aid's tag table): one `dev-solution-<ID>` and one
     * `dev-solution-label-<ID>` per piece and nothing else under that prefix; each shape's PieceKey; each shape's bounds the
     * px box of its solution polygon through BoardMapping within 1 px, inside the `silhouette` box; each label's text its id.
     */
    private fun kitAssertOverlay(kit: KitScreen, p: PuzzleDefinition, what: String) {
        val tags = kit.nodesWithTagPrefix(KIT_DEV_SOLUTION_PREFIX).map { it.first }.sorted()
        val expected = p.pieces.flatMap { listOf(DevAidTags.solution(it), DevAidTags.solutionLabel(it)) }.sorted()
        assertEquals("$what: the overlay's nodes are one shape and one label per piece of ${p.id} ${p.pieces}", expected, tags)
        val silhouette = kit.bounds(PlayTags.SILHOUETTE)
        for (sp in p.solution) {
            val tag = DevAidTags.solution(sp.piece)
            assertEquals("$what: `$tag` PieceKey", sp.piece, kit.key(tag, PlaySemantics.PieceKey))
            val box = kitBox(sp.polygon.map { kit.toPx(it) })
            val shown = kit.bounds(tag)
            kitAssertBoxNear("$what: `$tag` bounds vs ${sp.piece}'s stored solution polygon through BoardMapping", box, shown, 1f)
            assertTrue(
                "$what: `$tag` $shown lies on the silhouette's box $silhouette",
                shown.left >= silhouette.left - 1f && shown.top >= silhouette.top - 1f && shown.right <= silhouette.right + 1f && shown.bottom <= silhouette.bottom + 1f,
            )
            assertEquals("$what: `${DevAidTags.solutionLabel(sp.piece)}` reads the piece's id", sp.piece.name, kit.text(DevAidTags.solutionLabel(sp.piece)))
        }
    }

    /** No overlay node at all (tag table: the shapes and labels are present while solutionShown holds and the puzzle is not Solved). */
    private fun kitAssertNoOverlay(kit: KitScreen, what: String) {
        val overlay = kit.nodesWithTagPrefix(KIT_DEV_SOLUTION_PREFIX).map { it.first }
        assertTrue("$what: no overlay node: $overlay", overlay.isEmpty())
    }

    /** [id]'s colour at 55 % over the flat silhouette (touch geometry: "PlayPalette.piece(id) at 55 % alpha"; S-12). */
    private fun kitOverlayOverSilhouette(id: PieceId): Color {
        val piece = PlayPalette.piece(id)
        val under = PlayPalette.silhouette
        val a = 0.55f
        return Color(piece.red * a + under.red * (1 - a), piece.green * a + under.green * (1 - a), piece.blue * a + under.blue * (1 - a))
    }

    /**
     * On a New puzzle, every `board` pixel 4 dp or more inside each solution polygon, 2 dp clear of its label's box and of the
     * DEV button's box, is the piece's colour at 55 % over the silhouette, within 10/255. At least 8 samples per piece.
     */
    private fun kitAssertOverlayColour(kit: KitScreen, p: PuzzleDefinition, what: String) {
        assertEquals("$what: the colour fixture needs a New puzzle (nothing placed over the overlay)", PuzzleState.NEW, kit.shownState())
        val board = kit.capture(PlayTags.BOARD)
        val bb = kit.bounds(PlayTags.BOARD)
        val pad = kit.dp(2f)
        val button = if (kit.exists(DevAidTags.DEV_BUTTON)) listOf(kit.bounds(DevAidTags.DEV_BUTTON).inflate(pad)) else emptyList()
        for (sp in p.solution) {
            val poly = sp.polygon.map { kit.toPx(it) }
            val label = kit.bounds(DevAidTags.solutionLabel(sp.piece)).inflate(pad)
            val samples = kitInteriorSamples(poly, kit.dp(4f), listOf(label) + button, within = bb)
            if (samples.size < 8) throw AssertionError("$what: only ${samples.size} samples 4 dp inside ${sp.piece}'s polygon and clear of its label (kit fixture)")
            val expected = kitOverlayOverSilhouette(sp.piece)
            val wrong = samples.filter { s -> !kitNear(board[(s.x - bb.left).toInt(), (s.y - bb.top).toInt()], expected, 10f / 255f) }
            if (wrong.isNotEmpty()) {
                val s = wrong.first()
                throw AssertionError("$what: ${wrong.size} of ${samples.size} pixels 4 dp inside ${sp.piece}'s polygon are not ${kitHex(expected)} (${kitHex(PlayPalette.piece(sp.piece))} at 55 % over ${kitHex(PlayPalette.silhouette)}); first $s = ${kitHex(board[(s.x - bb.left).toInt(), (s.y - bb.top).toInt()])}")
            }
        }
    }

    /**
     * A lock of one of [p]'s pieces that is not in [placed], found by the engine against [placed] at [dpPerUnit] (so a valid
     * lock, which the load check keeps, S-21; ILockEngine `stillValid`), whose corners match no stored solution polygon: a
     * piece a player put where the stored solution has none. Dropped, unmirrored, at each turn, on each solution polygon's
     * centre and then each outline corner (the anchors, REQ-019). Only a lock the engine gives again when the piece is dropped
     * at its own centre counts, so a drop there through S-2 lands exactly on it.
     */
    private fun kitLockOffTheSolution(p: PuzzleDefinition, placed: List<Placement>, dpPerUnit: Double): Placement {
        val drops = p.solution.map { kitCentroid(it.polygon) } + kitEngine.outlineCorners(p)
        for (piece in p.pieces.filter { id -> placed.none { it.piece == id } }) {
            for (drop in drops) for (step in 0..7) {
                val lock = kitEngine.findLock(p, placed, piece, Turn(step), false, drop, dpPerUnit)
                if (lock !is LockResult.Locked) continue
                val at = lock.placement
                val corners = kitEngine.vertices(at.piece, at.turn, at.mirrored, at.centre)
                if (p.solution.any { kitSamePointSet(it.polygon, corners) }) continue
                val again = kitEngine.findLock(p, placed, piece, at.turn, at.mirrored, at.centre, dpPerUnit)
                if (again is LockResult.Locked && kitSamePlacement(again.placement, at)) return at
            }
        }
        throw AssertionError("kit: no piece of ${p.id} locks off the stored solution next to $placed")
    }

    /** What REQ-046 A3 means on the screen after "Solve this puzzle now" on [p] (see the A3 test's oracle). */
    private fun kitAssertAidSolved(kit: KitScreen, p: PuzzleDefinition, what: String) {
        assertFalse("$what: Solve closes the menu (S-26 solveNow: \"closes, then S-27\")", kit.exists(DevAidTags.SHEET))
        assertEquals("$what: `play-screen` PuzzleStateKey", PuzzleState.SOLVED, kit.shownState())
        assertEquals("$what: the controller's state (S-1)", PuzzleState.SOLVED, kit.onMain { kit.controller.state })
        assertFalse("$what: no `dev-button` on the solved puzzle, at once (tag table; REQ-046 rule 5)", kit.exists(DevAidTags.DEV_BUTTON))
        kit.advance(1500)
        kit.frames()
        kit.interaction(PlayTags.SOLVED_PICTURE).assertIsDisplayed()
        kitAssertBoxNear("$what: `solved-picture` box vs the silhouette's box (tag table)", kit.bounds(PlayTags.SILHOUETTE), kit.bounds(PlayTags.SOLVED_PICTURE), 2f)
        assertFalse("$what: still no `dev-button` 1.5 s after the solve", kit.exists(DevAidTags.DEV_BUTTON))
        kit.advance(1500)
        kit.frames()
        assertTrue("$what: the solved row's `best-time`", kit.exists(KitTag.BEST_TIME))
        val best = kit.text(KitTag.BEST_TIME)
        assertTrue("$what: `best-time` reads '$best', which holds the no-time literal '${kitTexts.noTime}' (the best time is empty)", best.contains(kitTexts.noTime))
        assertNull("$what: the controller's bestSeconds (S-1Δ)", kit.onMain { kit.controller.bestSeconds })
        kitAssertThePictureReplacedThePieces(kit, p, what)
    }

    /** 3 px inside the silhouette: no TYPE-001 piece colour the picture does not name, and no flat silhouette colour unless the picture names it. */
    private fun kitAssertThePictureReplacedThePieces(kit: KitScreen, p: PuzzleDefinition, what: String) {
        val board = kit.capture(PlayTags.BOARD)
        // Amended WO-010 (F3, recirculation 6, a look-alike; a corrected check — decisions.md 2026-10-20 "F3's cause is the
        // REQ-023 A1 leg's inside mask, not the product or the data"): "3 px inside" is measured from the exact outline
        // (`kit.silhouetteBands`), no longer the polygons widened 0.75 px and eroded by a 7 × 7 square, which at a 315° notch held
        // pixels 0.46 px from the outline, where the picture's own outline stroke rightly paints the flat silhouette colour
        // (decisions.md 2026-10-12 "The solve, as drawn"): F3's false red, and kitFirst(WARMUP), shapes-warmup-1, has such a notch.
        val inside = kit.silhouetteBands(p).inside
        val art = kitArtColours(p.art)
        val foreign = kitPieceColours().filterValues { pc -> art.none { kitNear(it, pc, 6f / 255f) } }
        val silhouetteInArt = art.any { kitNear(it, PlayPalette.silhouette, 6f / 255f) }
        var pieceColoured = 0
        var flat = 0
        var first = ""
        inside.forEachSet { x, y ->
            val c = board[x, y]
            for ((id, pc) in foreign) if (kitNear(c, pc, 6f / 255f)) { pieceColoured++; if (first.isEmpty()) first = "($x, $y) = ${kitHex(c)} ($id)" }
            if (!silhouetteInArt && kitNear(c, PlayPalette.silhouette, 6f / 255f)) { flat++; if (first.isEmpty()) first = "($x, $y) = ${kitHex(c)} (silhouette)" }
        }
        assertTrue("$what: the fixture reads pixels inside the silhouette (${inside.count()})", inside.count() > 1000)
        assertEquals("$what: piece-coloured pixels 3 px inside the silhouette; first $first", 0, pieceColoured)
        assertEquals("$what: flat silhouette pixels 3 px inside the silhouette (the picture is not shown); first $first", 0, flat)
    }

    /** Every Text and ContentDescription of the unmerged tree (the field's editable text is not one of them). */
    private fun kitShownTexts(): List<String> {
        val out = ArrayList<String>()
        fun walk(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { out += it.text }
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { out += it }
            n.children.forEach { walk(it) }
        }
        walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        return out
    }

    private fun kitAssertPasscodeNotShown(kit: KitScreen, what: String) {
        kit.frames()
        val shown = kitShownTexts().filter { it.contains("0417") }
        assertTrue("$what: a text or content description shows the passcode (S-26: never shown): $shown", shown.isEmpty())
    }

    /** The name's and the number's pixels: the captures of `title` and of the "n / N" text inside `counter` (seam note 4 of WO-004). */
    private fun kitBarShots(kit: KitScreen): Map<String, PixelMap> {
        val id = kit.shownId()
        return mapOf(
            "the title" to kit.capture(PlayTags.TITLE),
            "the number" to rule.onNode(hasText(kitCounterText(id)) and hasAnyAncestor(hasTestTag(KitTag.COUNTER)), useUnmergedTree = true).captureToImage().toPixelMap(),
        )
    }

    /** The hit-order-0 oracle with [open] (the prompt's or the menu's content tag) open, the bar's pixels against [closed] when given; see the pin's comment. */
    private fun kitAssertSheetOwnsEveryTouch(kit: KitScreen, p: PuzzleDefinition, open: String, closed: Map<String, PixelMap>?, what: String) {
        fun kitReopen() {
            if (kit.exists(open)) return
            kit.press(DevAidTags.DEV_BUTTON)
            kit.advance(KIT_AID_SETTLE_MS)
            check(kit.exists(open)) { "kit: DEV did not open `$open` again" }
        }
        assertTrue("$what: `$open`", kit.exists(open))
        val screen = kit.bounds(PlayTags.SCREEN)
        val barBottom = kit.bounds(KitTag.COUNTER).bottom
        val sheet = kit.bounds(DevAidTags.SHEET)
        kitAssertBoxNear("$what: `dev-aid-sheet` is the screen below the top bar (S-10Δ2 Slot(0, bar bottom, W, H − bar bottom))", Rect(0f, barBottom, screen.width, screen.height), sheet, 2f)
        for (tag in listOf(PlayTags.TITLE, KitTag.COUNTER, KitTag.NAV_PREV, KitTag.NAV_NEXT, PlayTags.GEAR)) {
            val r = kit.bounds(tag)
            assertFalse("$what: `$tag` $r overlaps `dev-aid-sheet` $sheet (the scrim and the sheet start below the top bar)", r.overlaps(sheet))
        }
        val shots = if (closed != null) kitBarShots(kit) else emptyMap()
        for ((name, before) in closed.orEmpty()) {
            val now = shots.getValue(name)
            assertEquals("$what: $name's capture size", before.width to before.height, now.width to now.height)
            var changed = 0
            var first = ""
            for (y in 0 until now.height) for (x in 0 until now.width) {
                if (!kitNear(before[x, y], now[x, y], 3f / 255f)) { changed++; if (first.isEmpty()) first = "($x, $y): ${kitHex(before[x, y])} → ${kitHex(now[x, y])}" }
            }
            assertEquals("$what: $name's pixels that differ from the sheet closed (drawn over?); first $first", 0, changed)
        }
        for (tag in listOf(KitTag.NAV_PREV, KitTag.NAV_NEXT, KitTag.COUNTER, PlayTags.GEAR)) {
            kit.press(tag)
            assertEquals("$what: a tap on `$tag` changes no puzzle", p.id, kit.shownId())
            assertFalse("$what: a tap on `$tag` opens no grid", kit.onMain { kit.controller.overviewOpen })
            assertFalse("$what: a tap on `$tag` opens no settings (S-53: inert under hit order 0)", kit.onMain { kit.controller.settingsOpen })
            assertTrue("$what: a tap on `$tag` leaves the sheet open (the bar is inert and is not the scrim)", kit.exists(open))
        }
        val piece = PieceId.ST1
        val turn = kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey)
        val at = kit.trayTapPoint(piece)
        kit.tap(at)
        kit.frames()
        assertEquals("$what: a tap at $at on $piece in the tray under the sheet does not turn it", turn, kit.key(PlayTags.trayPiece(piece), PlaySemantics.TurnKey))
        assertEquals("$what: …and the puzzle stays New", PuzzleState.NEW, kit.shownState())
        kitReopen()
        kit.touch { down(at) }
        kit.frames()
        kit.moveFinger(at, Offset(at.x, at.y - kit.dp(20f)), 4)
        assertTrue("$what: a 20 dp drag from $piece in the tray under the sheet drags nothing", PieceId.entries.none { kit.exists(PlayTags.dragPiece(it)) })
        kit.lift()
        kit.advance(KIT_AID_SETTLE_MS)
        assertEquals("$what: after that drag $piece is in the tray", listOf(PlayTags.trayPiece(piece)), kit.homesOf(piece))
        assertEquals("$what: after that drag the puzzle is still New", PuzzleState.NEW, kit.shownState())
        kitReopen()
    }

    /** A point of `dev-aid-sheet` 4 dp inside its left edge, level with the open content's centre and outside the content: the scrim. */
    private fun kitScrimPoint(kit: KitScreen): Offset {
        val sheet = kit.bounds(DevAidTags.SHEET)
        val content = listOf(DevAidTags.PROMPT, DevAidTags.MENU).filter { kit.exists(it) }.map { kit.bounds(it) }
        check(content.size == 1) { "kit: ${content.size} sheet contents are open" }
        val at = Offset(sheet.left + kit.dp(4f), content[0].center.y)
        check(sheet.contains(at)) { "kit: the scrim point $at is outside `dev-aid-sheet` $sheet" }
        check(!content[0].contains(at)) { "kit: the scrim point $at lies on the sheet's content ${content[0]} (the touch geometry insets the sheet 16 dp)" }
        return at
    }

    /**
     * Shows [kit]'s screen at the full width and a kit-chosen height (S-10: PlayScreen "fills `modifier`'s size"), with its aid:
     * the heights from [KIT_CORNER_SCAN_FROM_DP] to [KIT_CORNER_SCAN_TO_DP] in 8 dp steps are tried, and the one where a point
     * lies deepest inside both `dev-button` (2 px in) and [piece]'s solution polygon through BoardMapping is kept. A height the
     * window cannot hold (the `play-screen` node shorter than asked) is left out. Fails loudly, with what it read, when no
     * height gives a point 2 dp deep. Returns the height; the clock is paused afterwards, as after `KitScreen.show()`.
     */
    private fun kitShowAtTheCornerHeight(kit: KitScreen, piece: PieceId): Float {
        val height = mutableFloatStateOf(KIT_CORNER_SCAN_FROM_DP)
        rule.mainClock.autoAdvance = true
        rule.setContent {
            PlayScreen(kit.controller, kitPhoneLayout, kitTexts, false, Modifier.fillMaxWidth().height(height.floatValue.dp), devAid = kit.devAid)
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        kit.frames(2)
        check(kit.exists(PlayTags.SCREEN)) { "kit: no `play-screen` node after setContent" }
        val table = StringBuilder()
        var best = -1f
        var bestDepth = 0f
        var h = KIT_CORNER_SCAN_FROM_DP
        while (h <= KIT_CORNER_SCAN_TO_DP) {
            height.floatValue = h
            kit.frames(2)
            val screen = kit.node(PlayTags.SCREEN).boundsInRoot
            if (abs(screen.height - kit.dp(h)) <= 1.5f && kit.exists(DevAidTags.DEV_BUTTON) && kit.bounds(PlayTags.BOARD).height > 0f) {
                val polygon = kit.puzzle.solution.single { it.piece == piece }.polygon.map { kit.toPx(it) }
                val depth = kitDeepestUnderTheButton(kit, polygon, kit.bounds(DevAidTags.DEV_BUTTON)).second
                table.append("$h dp: board ${kit.bounds(PlayTags.BOARD)}, button ${kit.bounds(DevAidTags.DEV_BUTTON)}, depth ${depth / kit.density} dp; ")
                if (depth > bestDepth) { bestDepth = depth; best = h }
            } else {
                table.append("$h dp: not held (screen ${screen.height} px); ")
            }
            h += 8f
        }
        if (best < 0f || bestDepth < kit.dp(2f)) throw AssertionError("kit: no host height puts $piece 2 dp deep under the DEV button (px, density ${kit.density}): $table")
        height.floatValue = best
        kit.frames(2)
        kit.advance(KIT_AID_SETTLE_MS)
        check(kit.onMain { kit.controller.boardDpPerUnit } > 0.0) { "kit: boardDpPerUnit not set at $best dp" }
        return best
    }

    /** The point 2 px or more inside [button] that lies deepest inside [polygon] (px), and its depth; null and 0 when none is inside. */
    private fun kitDeepestUnderTheButton(kit: KitScreen, polygon: List<Offset>, button: Rect): Pair<Offset?, Float> {
        val poly = polygon.map { Point(it.x.toDouble(), it.y.toDouble()) }
        val area = button.deflate(2f)
        var best: Offset? = null
        var depth = 0f
        var y = area.top
        while (y <= area.bottom) {
            var x = area.left
            while (x <= area.right) {
                val q = Point(x.toDouble(), y.toDouble())
                if (kitInside(q, poly)) {
                    val d = kitDistToBoundary(q, poly).toFloat()
                    if (d > depth) { best = Offset(x, y); depth = d }
                }
                x += kit.dp(1f)
            }
            y += kit.dp(1f)
        }
        return best to depth
    }

    /** [target] placed through S-2's public actions, as `KitScreen.placeByApi` places a solution pose; true when the drop locks exactly there. */
    private fun kitPlaceAt(kit: KitScreen, target: Placement): Boolean = kit.onMain {
        val c = kit.controller
        if (target.piece == PieceId.PG && c.trayMirrored(PieceId.PG) != target.mirrored) check(c.pressBadge()) { "kit: pressBadge() refused with PG in the tray" }
        check(c.startDrag(target.piece, target.centre)) { "kit: startDrag(${target.piece}) refused" }
        val from = c.dragged?.turn?.step ?: error("kit: no drag after startDrag")
        val steps = ((target.turn.step - from) % 8 + 8) % 8
        if (steps != 0) c.twistDrag(if (steps <= 4) steps else steps - 8)
        c.moveDrag(target.centre, overBoard = true)
        c.updatePreview()
        kitSamePlacement(c.drop(), target)
    }

    /** Two colours more than 40/255 apart in some channel: a fixture that can tell one from the other at 55 % over it. */
    private fun kitDiffers(a: Color, b: Color): Boolean = !kitNear(a, b, 40f / 255f)

    /**
     * Every `board` pixel 3 px inside [piece]'s drawn shape (its node keys through the engine) — and, when [within] is given,
     * 4 dp inside one of those polygons — clear of the board badge's area and the DEV button's box, is [piece]'s solid colour
     * (S-12) within 16/255. Needs at least 8 samples. Returns the samples (`play-screen` px).
     */
    private fun kitAssertSolidOver(kit: KitScreen, piece: PieceId, within: List<List<Offset>>?, what: String): List<Offset> {
        val board = kit.capture(PlayTags.BOARD)
        val bb = kit.bounds(PlayTags.BOARD)
        val avoid = listOfNotNull(
            if (kit.exists(PlayTags.FLIP_BADGE_BOARD)) kit.bounds(PlayTags.FLIP_BADGE_BOARD) else null,
            if (kit.exists(DevAidTags.DEV_BUTTON)) kit.bounds(DevAidTags.DEV_BUTTON).inflate(kit.dp(2f)) else null,
        )
        val shape = kit.boardPolygonPx(PlayTags.boardPiece(piece))
        var samples = kitInteriorSamples(shape, 3f, avoid, within = bb)
        if (within != null) {
            val margin = kit.dp(4f).toDouble()
            samples = samples.filter { o ->
                val q = Point(o.x.toDouble(), o.y.toDouble())
                within.any { poly -> val pts = poly.map { Point(it.x.toDouble(), it.y.toDouble()) }; kitInside(q, pts) && kitDistToBoundary(q, pts) >= margin }
            }
        }
        if (samples.size < 8) throw AssertionError("$what: only ${samples.size} samples (kit fixture)")
        val colour = PlayPalette.piece(piece)
        val wrong = samples.filter { s -> !kitNear(board[(s.x - bb.left).toInt(), (s.y - bb.top).toInt()], colour, 16f / 255f) }
        if (wrong.isNotEmpty()) {
            val s = wrong.first()
            throw AssertionError("$what: ${wrong.size} of ${samples.size} pixels inside $piece's drawn shape are not its colour ${kitHex(colour)} (the overlay drawn over it?); first $s = ${kitHex(board[(s.x - bb.left).toInt(), (s.y - bb.top).toInt()])}")
        }
        return samples
    }

    /** Opens the grid by a tap on the counter zone (S-22), scrolls [id]'s cell on screen and taps it (REQ-050: "one press opens that puzzle"). */
    private fun kitChooseInGrid(kit: KitScreen, id: String) {
        kit.press(KitTag.COUNTER)
        kit.advance(KIT_AID_SETTLE_MS)
        check(kit.exists(KitTag.OVERVIEW)) { "kit: a tap on the counter opened no grid" }
        val tag = KitTag.overviewCell(id)
        kit.scrollTo(tag) // WO-010 F2: a scroll that returns under the paused clock (KitScreen.scrollTo)
        kit.advance(500)
        val n = kit.node(tag)
        val visible = kit.bounds(tag)
        check(visible.height >= n.size.height - 1f && visible.width >= n.size.width - 1f) { "kit: `$tag` is not wholly on screen after the scroll ($visible, size ${n.size})" }
        kit.tap(visible.center)
        kit.frames()
        kit.advance(KIT_AID_SETTLE_MS)
        check(!kit.exists(KitTag.OVERVIEW)) { "kit: choosing `$tag` left the grid open" }
    }
}
