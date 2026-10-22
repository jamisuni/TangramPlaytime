// Acceptance kit (WO-004, TASK-019): the top bar — ‹ and ›, the counter zone, the long press on ›, the state word, the
// rating dots, the Restart pill's place in the touch order and the loading paper (designs/WO-004-design.md §2 S-1Δ, S-2Δ,
// S-11Δ, S-13Δ, S-14Δ, S-22 with its touch geometry, S-23; decisions.md 2026-10-15 rows). Every line about a control is
// driven by touch on `play-screen` (KitScreen header); a board is set up through S-2, and a store through the kit's
// store double, where the line is not about the gesture. Tags are S-13Δ's strings (`KitTag`). Each tokened test asserts
// its acceptance line's meaning; pins of a seam row, a rule or a decisions.md row carry no token. The hit-order-3 pin alone
// hosts the screen at a 731 dp phone height over a kit-owned puzzle (its comment says why; kit finding, 2026-10-17).
//
// Amended WO-007 (TASK-055; designs/WO-007-design.md §2 S-53; decisions.md 2026-10-21 "The moved-› pins in `play/` land in
// the same step as S-53, as declared outputs, each a corrected check."): the bar's columns are four (› ends at the gear's
// left, a corrected check); the gear's legs — taps 2 px inside its edges open the settings, at the lift, with no long press;
// a second finger on it during a drag and a tap on it with the grid open do nothing — and the gear on the loading paper's
// list of absent tags. The settings are closed after each leg (S-2Δ4 `closeSettings`). No token is added.
package app.tangramplaytime.play

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.SolvedPiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class TopBarKitTest {

    @get:Rule
    val rule = createComposeRule()

    // ── REQ-024: ‹ and › ───────────────────────────────────────────────────────────────────────────────

    @Test // REQ-024.A1
    fun nextWorksOnAnUnsolvedPuzzle() {
        // REQ-024 A1: "› works on an unsolved puzzle." Statement: "WHEN the player presses ‹ or › in the top bar, the game
        // SHALL show the previous or next puzzle, whether or not the current one is solved." Oracle, by a tap on `nav-next`
        // from each unsolved state of TYPE-006: from the fresh start (New) and from a full-set puzzle In progress with two
        // pieces on its board, the screen shows the next puzzle of ‹ › order — `play-screen`'s PuzzleIdKey, read against the
        // title's text, the `silhouette` node's box (the next puzzle's outline through BoardMapping) and "n / N" (`assertShows`).
        val kit = KitScreen(rule)
        kit.show()
        val start = kit.shownId()
        assertEquals("set-up: the fresh start is New", PuzzleState.NEW, kit.shownState())
        kit.press(KitTag.NAV_NEXT)
        kit.assertShows(kitAfter(start).id, "› from the New puzzle $start")

        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val placed = placeUpTo(kit, p, 2)
        assertEquals("set-up: two pieces of ${p.id} placed through S-2", 2, placed)
        assertEquals("set-up: ${p.id} is In progress", PuzzleState.IN_PROGRESS, kit.shownState())
        kit.press(KitTag.NAV_NEXT)
        kit.assertShows(kitAfter(p.id).id, "› from ${p.id} In progress with two pieces placed")
    }

    @Test // REQ-024.A3
    fun nextOnTheLastPuzzleShowsTheFirst() {
        // REQ-024 A3: "› on the last puzzle shows the first." Rule: "The list wraps: ‹ on the first puzzle shows the last, ›
        // on the last shows the first." Oracle, by taps on `nav-next` with the last puzzle of ‹ › order open, New and then
        // In progress (a piece placed through S-2): the first puzzle is shown ("1 / N", `assertShows`), and the wrap is no
        // dead end — a second › shows the second puzzle. The rule's other half: ‹ on the first shows the last.
        val kit = KitScreen(rule)
        kit.show()
        val list = kitLibrary.puzzles
        val last = list.last()
        kit.open(last.id)
        kit.assertShows(last.id, "set-up: the last puzzle open")
        kit.press(KitTag.NAV_NEXT)
        kit.assertShows(list.first().id, "› on the last puzzle (New)")
        kit.press(KitTag.NAV_NEXT)
        kit.assertShows(list[1].id, "› once more after the wrap")

        kit.open(last.id)
        assertEquals("set-up: a piece of ${last.id} placed through S-2", 1, placeUpTo(kit, last, 1))
        kit.press(KitTag.NAV_NEXT)
        kit.assertShows(list.first().id, "› on the last puzzle (In progress)")
        kit.press(KitTag.NAV_PREV)
        kit.assertShows(last.id, "‹ on the first puzzle")
    }

    // ── REQ-050: the long press on › ───────────────────────────────────────────────────────────────────

    @Test // REQ-050.A1
    fun withPuzzlesThreeAndSevenUnsolvedAndThreeOpenALongPressOnNextOpensSeven() {
        // REQ-050 A1: "With puzzles 3 and 7 unsolved and puzzle 3 open, a long press on › opens puzzle 7." Rule: "Long press:
        // at least 500 ms." The numbers are positions in ‹ › order (the counter's n, REQ-024 rule). Oracle: a controller over
        // the kit's store double in which every other puzzle is Solved and puzzle 3 was opened last (S-1Δ: it opens there); a
        // finger held still on `nav-next`: puzzle 3 still shown at 400 ms (the rule's 500 ms); puzzle 7 shown at 600 ms with
        // the finger still down; still puzzle 7 after the lift (decisions.md 2026-10-15 "Browsing readings no REQ fixes": the
        // long press fires at 500 ms while the finger is down, and the lift then does nothing). A long press that acts as ›
        // (puzzle 4) or at the lift fails. Nothing else changes: every other puzzle's saved state is as seeded.
        val list = kitLibrary.puzzles
        assertTrue("the library lists at least seven puzzles (${list.size})", list.size >= 7)
        val three = list[2]
        val seven = list[6]
        val seed = list.filter { it.id != three.id && it.id != seven.id }.associate { it.id to kitSolvedProgress() }
        val ps = kitControllerOver(rule, KitDeviceStore(seed, lastOpened = three.id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        kit.assertShows(three.id, "set-up: puzzle 3 open")
        kit.holdDown(kit.bounds(KitTag.NAV_NEXT).center, 400)
        assertEquals("400 ms into a still hold on › nothing has changed (REQ-050 rule: at least 500 ms)", three.id, kit.shownId())
        kit.advance(200)
        kit.frames()
        kit.assertShows(seven.id, "600 ms into a still hold on ›")
        kit.lift()
        kit.frames()
        kit.assertShows(seven.id, "after the lift")
        ps.flush()
        for ((id, progress) in seed) assertEquals("$id's saved state after the long press", progress, ps.store.progressNow(id))
        assertEquals("puzzle 3 is still New", PuzzleState.NEW, ps.store.progressNow(three.id).state)
    }

    @Test // pin: REQ-050 rule "IF every puzzle is solved, THEN the long press shows the next puzzle as a normal press would"; no token
    fun withEveryPuzzleSolvedALongPressOnNextShowsTheNextPuzzle() {
        val list = kitLibrary.puzzles
        val ps = kitControllerOver(rule, KitDeviceStore(list.associate { it.id to kitSolvedProgress() }, lastOpened = list[2].id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        kit.assertShows(list[2].id, "set-up")
        kit.holdDown(kit.bounds(KitTag.NAV_NEXT).center, 600)
        kit.lift()
        kit.frames()
        kit.assertShows(list[3].id, "a long press on › with every puzzle solved")
    }

    @Test // pin: S-2Δ nextUnsolved "if that is the current one, nothing changes"; decisions.md 2026-10-15 "Browsing readings no REQ fixes" ("When the open puzzle is the only unsolved one, a long press stays on it"; "Moving ≥ 12 dp on any top-bar … control cancels it"); no token
    fun aLongPressStaysOnTheOnlyUnsolvedPuzzleAndAHoldThatMovesIsCancelled() {
        val list = kitLibrary.puzzles
        val open = list[2]
        val ps = kitControllerOver(rule, KitDeviceStore(list.filter { it.id != open.id }.associate { it.id to kitSolvedProgress() }, lastOpened = open.id))
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        kit.holdDown(kit.bounds(KitTag.NAV_NEXT).center, 600)
        kit.lift()
        kit.frames()
        kit.assertShows(open.id, "a long press on › with the open puzzle the only unsolved one")
        // A hold that moves 20 dp (past the 12 dp slop) before 500 ms is no tap and no long press.
        val at = kit.bounds(KitTag.NAV_NEXT).center
        kit.touch { down(at) }
        kit.frames()
        kit.touch { moveTo(Offset(at.x - kit.dp(20f), at.y + kit.dp(4f))) }
        kit.frames()
        kit.advance(700)
        kit.lift()
        kit.frames()
        kit.assertShows(open.id, "a hold on › that moved 20 dp")
    }

    // ── REQ-041: the warm-ups after the minis ──────────────────────────────────────────────────────────

    @Test // REQ-041.A1
    fun onAFreshInstallTheFirstFourPuzzlesAfterTheMinisAreWarmUps() {
        // REQ-041 A1: "On a fresh install, the first four puzzles after the mini puzzles are warm-ups." The screen half
        // (workorders/WO-004.md scope note "Re-covered lines": "› walks the order"). Oracle: a controller over an empty
        // store double — a fresh install (IProgressStore KDoc: missing state behaves as a fresh install) — opens its first
        // puzzle; › is tapped until every puzzle has been shown, each one read from the screen (`assertShows`); the run of
        // mini puzzles at the start is skipped and the next four shown are warm-ups (the puzzle file's kind, REQ-041 rule).
        val ps = kitControllerOver(rule, KitDeviceStore())
        val kit = KitScreen(rule, ps.controller)
        kit.show()
        val shown = ArrayList<String>()
        shown += kit.shownId()
        kit.assertShows(shown.last(), "the fresh start")
        repeat(kitLibrary.puzzles.size - 1) { i ->
            kit.press(KitTag.NAV_NEXT)
            shown += kit.shownId()
            kit.assertShows(shown.last(), "› number ${i + 1} from the fresh start")
        }
        val kinds = shown.map { id -> kitLibrary.byId(id)!!.kind }
        val minis = kinds.takeWhile { it == PuzzleKind.MINI }.size
        assertTrue("four puzzles are shown after the $minis minis (${shown.size} shown)", kinds.size >= minis + 4)
        assertEquals("the four puzzles shown after the $minis minis: ${shown.drop(minis).take(4)}", List(4) { PuzzleKind.WARMUP }, kinds.drop(minis).take(4))
    }

    // ── Pins: the bar's columns and the hit order (S-22) ───────────────────────────────────────────────

    @Test // pin: S-22 touch geometry "Top bar (66 dp). … full-height columns, with no gaps: ‹ covers x from 0 to 60 dp …; the counter is everything between … The 4 dp gap to the board hits nothing", as S-53 (designs/WO-007-design.md §2) moves it: "`ScreenGeometry.gear` is a 52 dp column at the safe right edge, the bar's full height. `navNext` now ends at `gear.left`"; decisions.md 2026-10-21 "The gear and the settings screen, as drawn." ("a fourth top-bar column at the safe right edge, after ›"; "the counter keeps everything between ‹ and ›"); S-13Δ "A control's bounds are its touch area"; decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn" ("the whole counter zone opens the grid"); corrected WO-007 (TASK-055): three columns became four, a corrected check; no token
    fun theTopBarIsFourFullHeightColumnsWithNoGapsAndTheGapToTheBoardHitsNothing() {
        // Oracle: the four nodes' bounds are the columns over the whole bar height (S-5 `topBarDp`), with `PlayInsets.NONE`
        // (the kit's phone literal), so the safe right edge is W: ‹ from 0 to 60 dp, › from W − 112 to W − 52 dp, the gear
        // from W − 52 dp to W, the counter between ‹ and ›. Then by taps 2 px inside each column's edges: ‹ shows the previous
        // puzzle, › the next, the counter opens the grid (S-1Δ `overviewOpen`; the overlay's own drawing is OverviewKitTest's),
        // the gear opens the settings (S-53 "calls `openSettings()`"; S-1Δ4 `settingsOpen`; the screen itself is `:app`'s and
        // the app kit's) and changes neither the puzzle nor the grid, and a tap between the bar and the board changes nothing.
        // The settings are closed after each gear tap (S-2Δ4 `closeSettings`).
        val kit = KitScreen(rule)
        kit.show()
        val list = kitLibrary.puzzles
        val home = list[1]
        kit.open(home.id)
        val w = kit.bounds(PlayTags.SCREEN).width
        val bar = kit.dp(kitPhoneLayout.topBarDp)
        val prev = kit.bounds(KitTag.NAV_PREV)
        val next = kit.bounds(KitTag.NAV_NEXT)
        val counter = kit.bounds(KitTag.COUNTER)
        val gear = kit.bounds(PlayTags.GEAR)
        val tol = 2.0
        kitAssertNear("‹ column left", 0.0, prev.left.toDouble(), tol)
        kitAssertNear("‹ column right (60 dp)", kit.dp(60f).toDouble(), prev.right.toDouble(), tol)
        kitAssertNear("› column left (W − 112 dp: 60 dp ending at the gear's left)", (w - kit.dp(112f)).toDouble(), next.left.toDouble(), tol)
        kitAssertNear("› column right (W − 52 dp)", (w - kit.dp(52f)).toDouble(), next.right.toDouble(), tol)
        kitAssertNear("› ends where the gear starts (S-53)", gear.left.toDouble(), next.right.toDouble(), tol)
        kitAssertNear("gear column left (W − 52 dp)", (w - kit.dp(52f)).toDouble(), gear.left.toDouble(), tol)
        kitAssertNear("gear column right (W, the safe right edge)", w.toDouble(), gear.right.toDouble(), tol)
        kitAssertNear("the counter starts where ‹ ends", prev.right.toDouble(), counter.left.toDouble(), tol)
        kitAssertNear("the counter ends where › starts", next.left.toDouble(), counter.right.toDouble(), tol)
        for ((name, r) in listOf("‹" to prev, "›" to next, "counter" to counter, "gear" to gear)) {
            kitAssertNear("$name column top", 0.0, r.top.toDouble(), tol)
            kitAssertNear("$name column bottom (the bar's ${kitPhoneLayout.topBarDp} dp)", bar.toDouble(), r.bottom.toDouble(), tol)
        }
        // By touch at the columns' edges.
        for (at in listOf(Offset(prev.left + 2f, prev.bottom - 2f), Offset(prev.right - 2f, prev.top + 2f))) {
            kit.open(home.id)
            kit.tap(at)
            kit.frames()
            kit.assertShows(kitBefore(home.id).id, "a tap at $at in the ‹ column")
        }
        for (at in listOf(Offset(next.left + 2f, next.top + 2f), Offset(next.right - 2f, next.bottom - 2f))) {
            kit.open(home.id)
            kit.tap(at)
            kit.frames()
            kit.assertShows(kitAfter(home.id).id, "a tap at $at in the › column")
        }
        for (at in listOf(Offset(counter.left + 2f, counter.bottom - 2f), Offset(counter.right - 2f, counter.top + 2f), counter.center)) {
            kit.open(home.id)
            kit.tap(at)
            kit.frames()
            assertTrue("a tap at $at in the counter zone opens the grid (S-1Δ overviewOpen)", kit.onMain { kit.controller.overviewOpen })
            assertEquals("a tap in the counter zone keeps the puzzle", home.id, kit.shownId())
            kit.onMain { kit.controller.closeOverview() }
            kit.frames(2)
        }
        val gearMid = gear.center
        for (at in listOf(
            Offset(gear.left + 2f, gearMid.y),
            Offset(gear.right - 2f, gearMid.y),
            Offset(gearMid.x, gear.top + 2f),
            Offset(gearMid.x, gear.bottom - 2f),
        )) {
            kit.open(home.id)
            assertFalse("set-up: the settings are closed before the tap at $at", kit.onMain { kit.controller.settingsOpen })
            kit.tap(at)
            kit.frames()
            assertTrue("a tap at $at, 2 px inside an edge of the gear column, opens the settings (S-53; S-1Δ4 settingsOpen)", kit.onMain { kit.controller.settingsOpen })
            assertEquals("a tap in the gear column keeps the puzzle", home.id, kit.shownId())
            assertFalse("a tap in the gear column opens no grid", kit.onMain { kit.controller.overviewOpen })
            kit.onMain { kit.controller.closeSettings() }
            kit.frames(2)
        }
        // The gap between the bar and the board hits nothing.
        val board = kit.bounds(PlayTags.BOARD)
        kit.open(home.id)
        if (board.top - counter.bottom >= 2f) {
            kit.tap(Offset(w / 2f, (counter.bottom + board.top) / 2f))
            kit.frames()
            assertEquals("a tap in the gap between the bar and the board keeps the puzzle", home.id, kit.shownId())
            assertFalse("a tap in the gap opens no grid", kit.onMain { kit.controller.overviewOpen })
            assertFalse("a tap in the gap opens no settings", kit.onMain { kit.controller.settingsOpen })
        } else {
            android.util.Log.i("KitScreen", "the bar (bottom ${counter.bottom}) and the board (top ${board.top}) leave no 2 px gap to tap")
        }
    }

    @Test // pin: S-53 "Hit: `Control.GEAR` acts at lift within the slop, with no long press, and calls `openSettings()`"; S-22 "Taps and the long press" ("A tap is a lift within the 12 dp slop, acted on at the lift whatever its duration; moving the slop distance or more cancels it. On › only, a finger held within the slop at 500 ms runs `nextUnsolved()`"); no token
    fun theGearOpensTheSettingsAtTheLiftWithNoLongPressAndAHoldThatMovesIsCancelled() {
        // Oracle, by touch on the gear's centre, with the settings closed before each leg (S-2Δ4 `closeSettings`):
        //  - a finger down opens nothing before the lift; held still for 700 ms (past ›'s 500 ms long press) it still opens
        //    nothing and the puzzle is unchanged; the lift then opens the settings (a tap whatever its duration);
        //  - a finger that moves 20 dp (past the 12 dp slop) before the lift opens nothing, and nothing else acts;
        //  - positive control: a still tap opens the settings.
        val kit = KitScreen(rule)
        kit.show()
        val home = kitLibrary.puzzles[1]
        kit.open(home.id)
        val at = kit.bounds(PlayTags.GEAR).center
        assertFalse("set-up: the settings are closed", kit.onMain { kit.controller.settingsOpen })

        kit.touch { down(at) }
        kit.frames()
        assertFalse("a finger down on the gear opens nothing before the lift", kit.onMain { kit.controller.settingsOpen })
        kit.advance(700)
        assertFalse("held still on the gear for 700 ms, it still opens nothing: no long press", kit.onMain { kit.controller.settingsOpen })
        assertEquals("…and the puzzle is unchanged (no long press runs nextUnsolved)", home.id, kit.shownId())
        kit.lift()
        kit.frames()
        assertTrue("the lift within the slop after the hold opens the settings (acted on at the lift whatever its duration)", kit.onMain { kit.controller.settingsOpen })
        assertEquals("…on the same puzzle", home.id, kit.shownId())
        kit.onMain { kit.controller.closeSettings() }
        kit.frames(2)

        kit.touch { down(at) }
        kit.frames()
        kit.touch { moveTo(Offset(at.x - kit.dp(20f), at.y + kit.dp(4f))) }
        kit.frames()
        kit.advance(300)
        kit.lift()
        kit.frames()
        assertFalse("a touch on the gear that moved 20 dp (past the 12 dp slop) opens no settings", kit.onMain { kit.controller.settingsOpen })
        assertEquals("…changes no puzzle", home.id, kit.shownId())
        assertFalse("…and opens no grid", kit.onMain { kit.controller.overviewOpen })

        kit.press(PlayTags.GEAR)
        assertTrue("positive control: a still tap on the gear opens the settings", kit.onMain { kit.controller.settingsOpen })
        kit.onMain { kit.controller.closeSettings() }
        kit.frames(2)
    }

    @Test // pin: S-22 hit order 1 "A drag is running: the touch is a twist finger, so ‹ and › never act mid-drag"; S-53 (WO-007) "[The gear] is inert under hit orders 0 and 1"; decisions.md 2026-10-21 "The gear and the settings screen, as drawn." ("It is inert while a drag runs …"); reviews/WO-007-design-review.md Re-derived row 9 ("The gear never acts while another finger is down"); the gear joins the list (TASK-055); no token
    fun noTopBarColumnActsWhileADragRuns() {
        // Oracle: with a piece dragged over the board, a second finger tapping ‹, ›, the counter and the gear, and held on ›
        // for 700 ms, changes neither the puzzle, the grid nor the settings, and the drag goes on.
        val kit = KitScreen(rule)
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        var finger = kit.pickUpFromTray(PieceId.ST1)
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 4)
        for (tag in listOf(KitTag.NAV_NEXT, KitTag.NAV_PREV, KitTag.COUNTER, PlayTags.GEAR)) {
            val at = kit.bounds(tag).center
            kit.touch { down(1, at) }
            kit.frames()
            kit.touch { up(1) }
            kit.frames(2)
            assertEquals("a second finger on `$tag` during a drag keeps the puzzle", p.id, kit.shownId())
            assertFalse("a second finger on `$tag` during a drag opens no grid", kit.onMain { kit.controller.overviewOpen })
            assertFalse("a second finger on `$tag` during a drag opens no settings", kit.onMain { kit.controller.settingsOpen })
            assertTrue("the drag goes on after a second finger on `$tag`", kit.exists(PlayTags.dragPiece(PieceId.ST1)))
        }
        kit.touch { down(1, kit.bounds(KitTag.NAV_NEXT).center) }
        kit.frames()
        kit.advance(700)
        kit.touch { up(1) }
        kit.frames(2)
        assertEquals("a second finger held on › for 700 ms during a drag keeps the puzzle", p.id, kit.shownId())
        kit.release()
        kit.advance(300)
    }

    @Test // pin: S-53 "[The gear] is inert under hit orders 0 and 1"; S-22 hit order 0 ("The overview is open: the router neither consumes nor acts, on the top bar too"; "The bar stays drawn and inert: a touch on ‹, › or the counter does nothing while the overview is open"); decisions.md 2026-10-21 "The gear and the settings screen, as drawn." ("It is inert while … the grid … is open"); no token
    fun theGearDoesNothingWhileTheGridIsOpen() {
        // Oracle: the grid opened by a tap on the counter; a tap on the gear opens no settings, keeps the puzzle and leaves the
        // grid open (the bar is inert and is not the scrim). Positive control: the grid closed, the same tap opens the settings.
        val kit = KitScreen(rule)
        kit.show()
        val home = kitLibrary.puzzles[1]
        kit.open(home.id)
        kit.press(KitTag.COUNTER)
        assertTrue("set-up: a tap on the counter opened the grid", kit.onMain { kit.controller.overviewOpen })
        kit.press(PlayTags.GEAR)
        assertFalse("a tap on the gear with the grid open opens no settings", kit.onMain { kit.controller.settingsOpen })
        assertTrue("…and leaves the grid open", kit.onMain { kit.controller.overviewOpen })
        assertEquals("…on the same puzzle", home.id, kit.shownId())
        kit.onMain { kit.controller.closeOverview() }
        kit.frames(2)
        kit.press(PlayTags.GEAR)
        assertTrue("positive control: with the grid closed, a tap on the gear opens the settings", kit.onMain { kit.controller.settingsOpen })
        kit.onMain { kit.controller.closeSettings() }
        kit.frames(2)
    }

    @Test // pin: S-22 hit order 3 "The badge disc → a piece's drawn shape → the Restart pill → the rest of the badge area → a cell or a margin"; decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn" ("In the touch order a piece's drawn shape beats it and it beats a piece's margin"); no token
    fun aPieceDrawnOverTheRestartPillWinsTheTouchAndThePillBeatsAPiecesMargin() {
        // Fixture (kit finding of the WO-004 log, 2026-10-15 21:32: the bundled search found no case on the dev image and skipped).
        // The case is constructed, not searched for. Why the plain full screen has none: the silhouette's unit box is scaled to
        // at most 80 % of the board height and centred (decisions.md 2026-10-11 "Board and tray drawing numbers from DI-1/DI-2"),
        // so a silhouette's top is at least 10 % of the board height below the board's top. The pill's bottom is the board's top
        // + 58 dp (S-22 "Restart": 48 dp high at the board's top-left + 10 dp). A placed piece lies inside the silhouette (ILockEngine
        // KDoc, "valid"). On the 914 dp dev screen the board is 577 dp tall, its 10 % is 57.7 dp, and no puzzle's piece reaches
        // 2 dp into the pill. So:
        // - the screen is hosted at the full width and 731 dp high (`kitShowAtHeight`; S-10: PlayScreen "fills `modifier`'s size"),
        //   a 16:9 phone (1080 × 1920 px at 420 dpi) that the window of both channel images (1080 × 2400 px at 420 dpi; at least
        //   842 dp high with API 26's bars) holds. The `play-screen` node is then the same px size on the dev and the floor image,
        //   and nothing else the layout reads differs, so the board, the silhouette and the pill are the same on both; only the
        //   pill's label width can differ with the fonts, which moves its right edge and nothing of the case's height. Read on
        //   the dev image (2026-10-17): the board is 421 dp high, its 10 % is 42.1 dp, so the silhouette's top lies 15.9 dp above
        //   the pill's bottom;
        // - the puzzle is kit-owned (`kitPillPuzzle`, through S-1's `PlayController(library, engine)` over `kitPillLibrary`): a
        //   full set whose bounding box is 0.75 as wide as tall, narrower than the board's 82 % × 80 % box (0.96 here), so it
        //   spans that box's height and its top lies exactly at the 10 %, whatever the bundled puzzles are. LT1's right angle sits
        //   at the box's top-left corner, its legs along the silhouette's top and left edges; the silhouette starts 71 dp into
        //   the board, 44 dp short of the pill's right edge (115 dp), so the case is a band about 44 × 16 dp of LT1 under the pill.
        // With LT1 placed through S-2 (one piece, so the puzzle is In progress and the pill is shown, S-22; REQ-025 rule), the
        // kit reads from the drawing a point at least 2 dp inside LT1's drawn shape and 2 px inside the `restart` node, and a point
        // of the node outside every drawn shape within 3–9 dp of LT1's edge (its 12 dp touch margin, REQ-015 rule). A missing
        // case fails the test loudly (`kitPillCaseMissing`); there is no skip.
        // Oracle, margin leg (run first, from the set-up board): a still tap at the margin point runs Restart (REQ-025: every
        // piece back in the tray, New), read from the controller and from the drawing (no `board-piece` node; the pill gone,
        // because Restart is shown only In progress). Piece leg (LT1 placed again through S-2): a still tap at the shape point
        // acts on LT1 as a tap on a board piece does (REQ-016: turned one step when the engine's `tryTurnInPlace` keeps it, else
        // a `Shake` for it), and Restart does not run: still In progress with the pill shown, every placed piece still placed.
        // A product that puts the margin before the pill fails the first leg; one that puts the pill before the drawn shape
        // fails the second.
        val p = kitPillPuzzle
        check(kitSolutionLocks(p, p.pieces.size).size == p.pieces.size) { "kit: ${p.id} does not lock all its pieces at their solution poses (the engine's validity check of the kit-owned puzzle)" }
        val kit = KitScreen(rule, PlayController(kitPillLibrary, kitEngine))
        kitShowAtHeight(kit, KIT_PILL_SCREEN_HEIGHT_DP)
        val piece = PieceId.LT1

        // The pill beats a piece's margin.
        kitPlaceForPill(kit, piece)
        val margin = kitMarginPointInPill(kit) ?: kitPillCaseMissing(kit, piece, "a point of the pill outside every drawn shape within 3–9 dp of $piece")
        android.util.Log.i("KitScreen", "pill margin case: ${p.id} $piece at $margin; px at density ${kit.density}: `play-screen` ${kit.bounds(PlayTags.SCREEN)}, `board` ${kit.bounds(PlayTags.BOARD)}, `silhouette` ${kit.bounds(PlayTags.SILHOUETTE)}, `restart` ${kit.bounds(KitTag.RESTART)}")
        kit.tap(margin)
        kit.frames(2)
        assertEquals("a tap in the pill on $piece's 12 dp margin runs Restart (New)", PuzzleState.NEW, kit.shownState())
        assertTrue("after Restart nothing is placed (controller)", kit.placed().isEmpty())
        assertTrue("after Restart no `board-piece` is drawn (tag table), found ${kit.homesOf(piece)}", PieceId.entries.none { kit.exists(PlayTags.boardPiece(it)) })
        assertFalse("after Restart (New) the pill is not shown (S-22: only In progress)", kit.exists(KitTag.RESTART))

        // A piece's drawn shape beats the pill.
        kitPlaceForPill(kit, piece)
        val at = kitPointInShapeAndPill(kit, piece) ?: kitPillCaseMissing(kit, piece, "a point 2 dp inside $piece's drawn shape and 2 px inside the pill")
        val dp = kit.onMain { kit.controller.boardDpPerUnit }
        val before = kit.placed()
        val q = before.single { it.piece == piece }
        val turned = kitEngine.tryTurnInPlace(p, before - q, q, q.turn.next(), dp)
        android.util.Log.i("KitScreen", "piece over the pill: ${p.id} $piece at $at; the engine's turn in place gives $turned")
        val shakeBefore = kit.onMain { kit.controller.shake }
        kit.tap(at)
        kit.advance(600)
        assertEquals("a tap on $piece's drawn shape over the pill keeps the puzzle In progress (no Restart)", PuzzleState.IN_PROGRESS, kit.shownState())
        assertTrue("a tap on $piece's drawn shape over the pill leaves the pill shown (S-22: shown In progress)", kit.exists(KitTag.RESTART))
        val after = kit.placed()
        assertEquals("a tap on $piece's drawn shape over the pill keeps every placed piece placed", before.map { it.piece }.toSet(), after.map { it.piece }.toSet())
        if (turned is LockResult.Locked) {
            assertTrue("$piece turned one step to ${turned.placement} (REQ-016), was ${after.single { it.piece == piece }}", kitSamePlacement(after.single { it.piece == piece }, turned.placement))
        } else {
            val shake = kit.onMain { kit.controller.shake }
            assertTrue("$piece shaken (a new Shake for it), was $shakeBefore → $shake", shake != null && shake != shakeBefore && shake.piece == piece)
        }
    }

    // ── Pins: the state word and the rating dots ───────────────────────────────────────────────────────

    @Test // pin: decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn" ("a state word for In progress and Solved, with none for New"; WO-004 checkpoint item 4); REQ-024 rule "The top bar shows … its state (TYPE-006)"; S-11Δ literals; seam note 4 (`state-word` inside `counter`); no token
    fun theStateWordReadsInProgressAndSolvedAndThereIsNoneForNew() {
        val kit = KitScreen(rule)
        kit.show(reducedMotion = true)
        val p = kit.puzzle
        assertEquals("set-up: the fresh start is New", PuzzleState.NEW, kit.shownState())
        assertEquals("no state word for New", "", kit.stateWord())
        var finger = kit.pickUpFromTray(p.pieces.first())
        assertEquals("In progress from the drag start (TYPE-006)", PuzzleState.IN_PROGRESS, kit.shownState())
        assertEquals("the state word In progress", kitTexts.inProgress, kit.stateWord())
        finger = kit.moveFinger(finger, kit.bounds(PlayTags.trayCell(p.pieces.last())).center, 3)
        kit.release()
        kit.advance(300)
        assertEquals("still In progress with the piece home (TYPE-006 has no way back to New but Restart)", kitTexts.inProgress, kit.stateWord())
        kit.solveByApi()
        kit.advance(1700)
        assertEquals("set-up: solved", PuzzleState.SOLVED, kit.shownState())
        assertEquals("the state word Solved", kitTexts.solved, kit.stateWord())
        val counter = kit.bounds(KitTag.COUNTER)
        val word = kit.bounds(KitTag.STATE_WORD)
        assertTrue("the state word $word lies in the counter zone $counter", counter.contains(word.center))
    }

    @Test // pin: REQ-024 rule "The top bar shows the puzzle's … difficulty dots"; REQ-040 rule "Rating: an integer from 1 to 5, shown as dots in the top bar"; decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn" ("one line of rating dots (●×rating, ○ up to five)"); S-14Δ RatingKey; no token
    fun theRatingDotsShowThePuzzlesRating() {
        // Oracle, for one puzzle of each rating the library holds: the `rating` node's RatingKey is the puzzle file's rating,
        // and the key (the product's claim) is read against the pixels: the node's capture holds five dots, the first `rating`
        // of them filled (ink at the dot's centre) and the rest hollow (the capture's background at the centre).
        val kit = KitScreen(rule)
        kit.show()
        val byRating = kitLibrary.puzzles.groupBy { it.rating }.toSortedMap()
        for ((rating, puzzles) in byRating) {
            val p = puzzles.first()
            kit.open(p.id)
            assertEquals("${p.id}: RatingKey", rating, kit.key(KitTag.RATING, PlaySemantics.RatingKey))
            val (total, filled) = ratingDots(kit)
            assertEquals("${p.id}: dots drawn in `rating`", 5, total)
            assertEquals("${p.id}: filled dots (rating $rating), left to right ${filled.joinToString("") { if (it) "●" else "○" }}", List(5) { it < rating }, filled)
        }
    }

    // ── Pin: the loading paper ─────────────────────────────────────────────────────────────────────────

    @Test // pin: S-23 "PlayLoading(modifier): plain paper, no top bar (no puzzle is current yet), tag play-loading" (the gear, S-53, is a top-bar column, so it is absent too: TASK-055); decisions.md 2026-10-15 "REQ-024 A2 and the launch window" (its title's dotted ID written without the dot: it is not this test's token) and "Startup, and what survives" ("blank paper (play-loading: no spinner, no text)"); no token
    fun theLoadingPaperDrawsNoTopBarAndNoText() {
        rule.setContent { PlayLoading(Modifier.fillMaxSize()) }
        rule.waitForIdle()
        val kit = KitScreen(rule)
        assertTrue("a `play-loading` node", kit.exists(KitTag.PLAY_LOADING))
        rule.onNodeWithTagUnmerged(KitTag.PLAY_LOADING).assertIsDisplayed()
        for (tag in listOf(PlayTags.SCREEN, PlayTags.TITLE, KitTag.NAV_PREV, KitTag.NAV_NEXT, PlayTags.GEAR, KitTag.COUNTER, KitTag.RATING, KitTag.STATE_WORD)) {
            assertFalse("no `$tag` on the loading paper", kit.exists(tag))
        }
        assertEquals("no text on the loading paper", emptyList<String>(), kit.tagsAndTexts().second)
        val paper = kit.capture(KitTag.PLAY_LOADING)
        var other = 0
        var first = ""
        for (y in 0 until paper.height) for (x in 0 until paper.width) {
            if (!kitNear(paper[x, y], PlayPalette.paper, 3f / 255f)) { other++; if (first.isEmpty()) first = "($x, $y) = ${kitHex(paper[x, y])}" }
        }
        assertEquals("pixels of the loading paper that are not ${kitHex(PlayPalette.paper)}; first $first", 0, other)
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** Places up to [n] pieces of [p] (open) through S-2, edge-first as the engine allows; returns how many. */
    private fun placeUpTo(kit: KitScreen, p: PuzzleDefinition, n: Int): Int {
        var count = 0
        var progress = true
        while (count < n && progress) {
            progress = false
            for (piece in p.pieces) {
                if (count >= n) break
                if (kit.placed().any { it.piece == piece }) continue
                if (kit.placeByApi(piece)) { count++; progress = true }
            }
        }
        kit.frames()
        kit.advance(250)
        return count
    }

    /**
     * Shows the screen as `KitScreen.show()` does (the clock auto-advancing for the first layout, then paused), but at the full
     * width and [heightDp] high instead of the whole window (S-10: PlayScreen "fills `modifier`'s size"). Fails loudly when the
     * window cannot hold that height, so the pin never runs at a size other than the one its case is built for.
     */
    private fun kitShowAtHeight(kit: KitScreen, heightDp: Float) {
        rule.mainClock.autoAdvance = true
        rule.setContent {
            PlayScreen(kit.controller, kitPhoneLayout, kitTexts, false, Modifier.fillMaxWidth().height(heightDp.dp))
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        kit.frames(2)
        check(kit.exists(PlayTags.SCREEN)) { "kit: no `play-screen` node after setContent" }
        val screen = kit.node(PlayTags.SCREEN).boundsInRoot
        check(abs(screen.height - kit.dp(heightDp)) <= 1.5f) { "kit: `play-screen` is ${screen.height} px high, not the $heightDp dp (${kit.dp(heightDp)} px) the pin hosts it at; the window is too short" }
        check(kit.onMain { kit.controller.boardDpPerUnit } > 0.0) { "kit: boardDpPerUnit not set by the first layout" }
    }

    /**
     * [piece] alone placed at its solution pose through S-2 (a set-up, not the gesture under test), with the open puzzle then
     * In progress and the `restart` pill shown (S-22: "A pill shown only In progress"; REQ-025 rule).
     */
    private fun kitPlaceForPill(kit: KitScreen, piece: PieceId) {
        check(kit.placed().isEmpty()) { "kit: the board is not empty before placing $piece: ${kit.placed()}" }
        check(kit.placeByApi(piece)) { "kit: $piece does not lock at its solution pose in ${kit.puzzle.id}" }
        kit.frames()
        kit.advance(250)
        check(kit.placed().map { it.piece } == listOf(piece)) { "kit: after placing $piece the board holds ${kit.placed()}" }
        check(kit.shownState() == PuzzleState.IN_PROGRESS) { "kit: ${kit.puzzle.id} is ${kit.shownState()} with $piece placed, not In progress" }
        check(kit.exists(KitTag.RESTART)) { "kit: no `restart` pill with ${kit.puzzle.id} In progress" }
    }

    /**
     * The point deepest inside [piece]'s drawn shape (its polygon at its keys, decisions.md 2026-10-12 "What 'drawn' means in
     * the touch order": edges only, no margin) that is also 2 px inside the `restart` node, at least 2 dp from the shape's
     * edge, and outside the board badge's area (the badge comes before both in the hit order); null when there is none.
     */
    private fun kitPointInShapeAndPill(kit: KitScreen, piece: PieceId): Offset? {
        val poly = kit.boardPolygonPx(PlayTags.boardPiece(piece)).map { Point(it.x.toDouble(), it.y.toDouble()) }
        val area = kit.bounds(KitTag.RESTART).deflate(2f)
        val badge = if (kit.exists(PlayTags.FLIP_BADGE_BOARD)) kit.bounds(PlayTags.FLIP_BADGE_BOARD) else null
        var best: Offset? = null
        var depth = 0f
        var y = area.top
        while (y <= area.bottom) {
            var x = area.left
            while (x <= area.right) {
                val q = Point(x.toDouble(), y.toDouble())
                if (kitInside(q, poly) && (badge == null || !badge.contains(Offset(x, y)))) {
                    val d = kitDistToBoundary(q, poly).toFloat()
                    if (d >= kit.dp(2f) && d > depth) { best = Offset(x, y); depth = d }
                }
                x += kit.dp(1f)
            }
            y += kit.dp(1f)
        }
        return best
    }

    /** Fails the pin loudly when its constructed case is missing from the drawing, with the geometry it read. */
    private fun kitPillCaseMissing(kit: KitScreen, piece: PieceId, what: String): Nothing {
        val shape = if (kit.exists(PlayTags.boardPiece(piece))) kit.boardPolygonPx(PlayTags.boardPiece(piece)).toString() else "not drawn"
        throw AssertionError(
            "kit: the hit-order-3 case is missing: no $what. `play-screen` ${kit.bounds(PlayTags.SCREEN)}, `board` ${kit.bounds(PlayTags.BOARD)}, " +
                "`silhouette` ${kit.bounds(PlayTags.SILHOUETTE)}, `restart` ${if (kit.exists(KitTag.RESTART)) kit.bounds(KitTag.RESTART).toString() else "absent"}, $piece $shape (px, density ${kit.density})",
        )
    }

    /** A point inside the `restart` node (2 px in) outside every drawn piece shape and within 12 dp of one (its margin), or null. */
    private fun kitMarginPointInPill(kit: KitScreen): Offset? {
        val area = kit.bounds(KitTag.RESTART).deflate(2f)
        val shapes = PieceId.entries.filter { kit.exists(PlayTags.boardPiece(it)) }
            .map { kit.boardPolygonPx(PlayTags.boardPiece(it)).map { o -> Point(o.x.toDouble(), o.y.toDouble()) } }
        val badge = if (kit.exists(PlayTags.FLIP_BADGE_BOARD)) kit.bounds(PlayTags.FLIP_BADGE_BOARD) else null
        var y = area.top
        while (y <= area.bottom) {
            var x = area.left
            while (x <= area.right) {
                val o = Offset(x, y)
                val q = Point(x.toDouble(), y.toDouble())
                x += kit.dp(1f)
                if (badge != null && badge.contains(o)) continue
                if (shapes.any { kitInside(q, it) }) continue
                val d = shapes.minOfOrNull { kitDistToBoundary(q, it) } ?: continue
                if (d >= kit.dp(3f) && d <= kit.dp(9f)) return o
            }
            y += kit.dp(1f)
        }
        return null
    }

    /**
     * The dots of the `rating` capture: the background is its most frequent colour; ink is any pixel more than 48/255 from
     * it in a channel; dots are the 8-connected ink components of at least 6 px, left to right; a dot is filled when the
     * pixel at the centre of its box is ink. Returns the number of dots and which are filled.
     */
    private fun ratingDots(kit: KitScreen): Pair<Int, List<Boolean>> {
        val img = kit.capture(KitTag.RATING)
        val w = img.width
        val h = img.height
        val counts = HashMap<Int, Int>()
        for (y in 0 until h) for (x in 0 until w) counts.merge(img[x, y].toArgb(), 1, Int::plus)
        val bgArgb = counts.maxByOrNull { it.value }!!.key
        val bg = androidx.compose.ui.graphics.Color(bgArgb)
        fun ink(x: Int, y: Int): Boolean {
            val c = img[x, y]
            return abs(c.red - bg.red) > 48f / 255f || abs(c.green - bg.green) > 48f / 255f || abs(c.blue - bg.blue) > 48f / 255f
        }
        val seen = BooleanArray(w * h)
        val dots = ArrayList<Pair<Float, Boolean>>()
        for (y0 in 0 until h) for (x0 in 0 until w) {
            if (seen[y0 * w + x0] || !ink(x0, y0)) continue
            var minX = x0; var maxX = x0; var minY = y0; var maxY = y0; var size = 0
            val stack = ArrayDeque<Int>()
            stack.addLast(y0 * w + x0)
            seen[y0 * w + x0] = true
            while (stack.isNotEmpty()) {
                val i = stack.removeLast()
                val x = i % w
                val y = i / w
                size++
                minX = minOf(minX, x); maxX = maxOf(maxX, x); minY = minOf(minY, y); maxY = maxOf(maxY, y)
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until w || ny !in 0 until h) continue
                    val j = ny * w + nx
                    if (!seen[j] && ink(nx, ny)) { seen[j] = true; stack.addLast(j) }
                }
            }
            if (size < 6) continue
            dots += ((minX + maxX) / 2f) to ink((minX + maxX) / 2, (minY + maxY) / 2)
        }
        val sorted = dots.sortedBy { it.first }
        return sorted.size to sorted.map { it.second }
    }
}

/** The unmerged-tree interaction of [tag]. */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithTagUnmerged(tag: String) =
    onNode(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true)

/**
 * The height the hit-order-3 pin hosts `PlayScreen` at: a 16:9 phone (1080 × 1920 px at 420 dpi), below the window of both
 * channel images (1080 × 2400 px at 420 dpi; see the pin's comment for why the full window has no case).
 */
private const val KIT_PILL_SCREEN_HEIGHT_DP = 731f

/**
 * The hit-order-3 pin's kit-owned puzzle: a full set (all seven pieces once, TYPE-001) shaped as a staircase, a 2 × 2 square
 * on top of a 2 × 2 square shifted one step right, in steps of √2 units, so its bounding box is 3√2 × 4√2 (0.75 as wide as
 * tall). LT1's right angle is the box's top-left corner (legs along the silhouette's top and left edges); LT2 completes the
 * top square; SQ, ST1, MT, PG and ST2 tile the bottom one. The pieces share edges (LT2 with LT1 and SQ; SQ with ST1 and
 * PG; MT with ST1 and PG; PG with ST2), so the puzzle is connected, with no hole. It meets the IPuzzleLibrary KDoc's validity
 * rules as a test fixture: no overlap (the pin checks that the engine locks every piece at its solution pose), rating 3, a
 * picture (its base colour), an `en` and a `fi` title; `reviewedByHuman` false, as in every bundled file (the AI never sets it).
 */
private val kitPillPuzzle: PuzzleDefinition by lazy {
    val s = sqrt(2.0)
    fun at(vararg steps: Pair<Int, Int>): List<Point> = steps.map { (x, y) -> Point(x * s, y * s) }
    PuzzleDefinition(
        id = "kit-pill-staircase",
        titles = mapOf("en" to "Staircase", "fi" to "Portaat"),
        category = "shapes",
        rating = 3,
        kind = PuzzleKind.FULL,
        solution = listOf(
            SolvedPiece(PieceId.LT1, at(0 to 0, 2 to 0, 0 to 2)),
            SolvedPiece(PieceId.LT2, at(2 to 0, 2 to 2, 0 to 2)),
            SolvedPiece(PieceId.MT, at(1 to 4, 2 to 3, 3 to 4)),
            SolvedPiece(PieceId.SQ, at(1 to 2, 2 to 2, 2 to 3, 1 to 3)),
            SolvedPiece(PieceId.PG, at(2 to 2, 2 to 3, 3 to 4, 3 to 3)),
            SolvedPiece(PieceId.ST1, at(1 to 3, 2 to 3, 1 to 4)),
            SolvedPiece(PieceId.ST2, at(2 to 2, 3 to 2, 3 to 3)),
        ),
        art = ArtLayer("#5C3620", emptyList()),
        reviewedByHuman = false,
    )
}

/** A library listing only [kitPillPuzzle] (IPuzzleLibrary: fixed for the process; `byId` null for any other id). */
private val kitPillLibrary: IPuzzleLibrary by lazy {
    object : IPuzzleLibrary {
        override val puzzles: List<PuzzleDefinition> = listOf(kitPillPuzzle)
        override fun byId(id: String): PuzzleDefinition? = puzzles.firstOrNull { it.id == id }
    }
}
