// Acceptance kit (WO-006, TASK-030): touch-area sizes in the real app on each layout class (REQ-037; tasks.md § WO-006 "Touch
// geometry per layout class" and "The player controls per screen"; decisions.md 2026-10-17 "REQ-037's oracle: the router's own
// areas on the device"; designs/WO-004-design.md §2 S-13Δ "A control's bounds are its touch area", S-22 hit order).
// - Where it runs: every image. The phone is constructed at 360 × 780 dp (1080 × 2340 px at 480 dpi); the tablet is 1280 × 800
//   dp (the tablet image's own display; elsewhere an override of 2160 × 1350 px at 270 dpi with the activity started after it,
//   decisions.md 2026-10-18 "Worker wins three contests", part 1) and 800 × 1280 dp by a frozen rotation (seam note 4). Each
//   leg starts once the activity and its insets have settled (`KitApp.usePhone` / `useTablet`; the ruling's part 2).
// - Per layout class, every screen that exists (decisions.md 2026-10-17 "Scope of WO-006 …": the Play screen New and In
//   progress, the solved view, the grid overview): each listed control's size in dp (density read from the launched
//   activity), the probe, and the completeness check. The probe taps 1 dp inside each corner of the router's rectangular
//   areas and of the scrim, and 1 dp inside the middle of each edge of the rounded foundation clickables, the grid's cells and
//   the Done pill (decisions.md 2026-10-17 "The device kit's open readings — amends the Q5 row's probe", ruling 3). `dev-*` nodes are exempt (REQ-037 rule 3); board pieces are not
//   swept (REQ-015 fixes their area; Q5 row).
// - Amended WO-007 (TASK-058; design §2 "Tokens (Q10)", re-covered: "`TouchTargetKitTest`: the gear and S-49's controls"): the
//   gear (S-53, a router control like ‹ and ›: no click action, so the completeness check does not see it; tasks.md § WO-007
//   seam note 2) is listed and probed on the Play screen New, In progress and on the solved view; the settings screen is a
//   new screen of the sweep, with and without its reset question (S-49's tag row: the scrim, Done, the two switches, Reset,
//   Erase, Cancel). Erase resets, so this class needs every puzzle New at its start and leaves them New (the brief; seam note
//   5). This file's tokens are unchanged.
// - The app's controller is one per process and shared by every class of the run: each probe undoes its act, and the test
//   leaves the controller and the device as it found them (seam note 3; `KitApp.restoreRule()`).
package app.tangramplaytime.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.play.devaid.DevAidTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.max

@RunWith(AndroidJUnit4::class)
class TouchTargetKitTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val kit = KitApp(composeRule)

    @get:Rule(order = 1)
    val kitRestore: TestRule = kit.restoreRule()

    @Test // REQ-037.A1
    fun onA360By780DpPhoneNoPlayerControlInAnyScreenHasATouchAreaBelowItsMinimum() {
        // REQ-037 A1: "No player control in any screen has a touch area below 48 dp." Statement: every interactive control a
        // player uses has a touch area of at least 48 dp in both directions; rule 1: "‹ and › are 52 dp; the flip badge's touch
        // area is 60 dp"; rule 2: a tray piece's touch area is its whole cell; rule 3: the #DevTools controls are exempt.
        // The phone layout class at 360 × 780 dp; every screen swept as `sweepEveryScreen` says.
        kit.start("REQ-037 A1, phone")
        kit.requireEveryPuzzleNew("REQ-037 A1, phone: Erase is probed (a resetting class)")
        kit.usePhone(KIT_PHONE_360, "the 360 × 780 dp phone")
        sweepEveryScreen("the 360 × 780 dp phone")
    }

    @Test // REQ-037.A1
    fun onA1280By800DpTabletNoPlayerControlInAnyScreenHasATouchAreaBelowItsMinimum() {
        // REQ-037 A1 (see the phone test): the tablet layout class at 1280 × 800 dp, landscape.
        kit.start("REQ-037 A1, tablet landscape")
        kit.requireEveryPuzzleNew("REQ-037 A1, tablet landscape: Erase is probed (a resetting class)")
        kit.useTablet(portrait = false, what = "the 1280 × 800 dp tablet")
        sweepEveryScreen("the 1280 × 800 dp tablet")
    }

    @Test // REQ-037.A1
    fun onAn800By1280DpTabletNoPlayerControlInAnyScreenHasATouchAreaBelowItsMinimum() {
        // REQ-037 A1 (see the phone test): the tablet layout class at 800 × 1280 dp, portrait by a frozen rotation.
        kit.start("REQ-037 A1, tablet portrait")
        kit.requireEveryPuzzleNew("REQ-037 A1, tablet portrait: Erase is probed (a resetting class)")
        kit.useTablet(portrait = true, what = "the 800 × 1280 dp tablet")
        sweepEveryScreen("the 800 × 1280 dp tablet")
    }

    // ── The sweep ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * Every screen of "The player controls per screen", on a New full puzzle with the parallelogram (so both badges exist):
     * the Play screen New; In progress with the parallelogram placed and a piece left in the tray; the solved view; the grid.
     */
    private fun sweepEveryScreen(leg: String) {
        val p = kit.firstNew(PuzzleKind.FULL, "$leg: a full puzzle with the parallelogram") { PieceId.PG in it.pieces && it.pieces.size == 7 }
        kit.openByController(p.id, "$leg: ${p.id} New")
        sweepPlayScreen("$leg, the Play screen New", p, emptyList())

        val placed = inProgressFixture(p, leg)
        placed.forEach { kit.placeByController(it, "$leg: the In-progress fixture") }
        sweepPlayScreen("$leg, the Play screen In progress", p, placed)

        kit.solveByController("$leg: the solved view")
        kit.awaitSolvedRow("$leg: the solved view")
        sweepSolvedView("$leg, the solved view", p)
        kit.onController { retry() }
        kit.idle()

        kit.onController { openOverview() }
        kit.awaitExists(KitAppTag.OVERVIEW, "$leg: the grid open")
        sweepOverview("$leg, the grid overview", p)
        kit.onController { if (overviewOpen) closeOverview() }
        kit.idle()

        if (kit.shownId() != p.id) kit.openByController(p.id, "$leg: back to ${p.id}")
        sweepSettings("$leg, the settings screen", p)
    }

    /**
     * "Play screen, New: `nav-prev`, `nav-next`, `counter`; every `tray-cell-<ID>`; `flip-badge-tray` when the puzzle has the
     * parallelogram. Play screen, In progress: the same, plus `restart`, and `flip-badge-board` while the parallelogram is
     * placed." A cell whose piece is on the board holds only its ghost: its size is checked, and it is probed only while its
     * piece is in the tray (a tap there turns that piece, REQ-016).
     */
    private fun sweepPlayScreen(where: String, p: PuzzleDefinition, placed: List<Placement>) {
        val inTray = kit.onController { trayPieces.toSet() }
        val listed = LinkedHashMap<String, Float>()
        listed[KitAppTag.NAV_PREV] = NAV_MIN_DP
        listed[KitAppTag.NAV_NEXT] = NAV_MIN_DP
        listed[KitAppTag.COUNTER] = CONTROL_MIN_DP
        listed[KitAppTag.GEAR] = CONTROL_MIN_DP
        for (piece in p.pieces) listed[PlayTags.trayCell(piece)] = CONTROL_MIN_DP
        if (PieceId.PG in inTray) listed[PlayTags.FLIP_BADGE_TRAY] = BADGE_MIN_DP
        if (placed.isNotEmpty()) listed[KitAppTag.RESTART] = CONTROL_MIN_DP
        if (placed.any { it.piece == PieceId.PG }) listed[PlayTags.FLIP_BADGE_BOARD] = BADGE_MIN_DP
        for ((tag, min) in listed) assertMinimum(where, tag, min)
        assertEveryClickableListed(where, listed.keys)

        val id = p.id
        probeBrowse(where, id, KitAppTag.NAV_PREV, before(id), prepare = {})
        probeBrowse(where, id, KitAppTag.NAV_NEXT, after(id), prepare = {})
        probeCounter(where, prepare = {})
        probeGear(where, prepare = {})
        for (piece in p.pieces.filter { it in inTray }) probeTrayCell(where, piece)
        if (PieceId.PG in inTray) probeTrayBadge(where)
        if (placed.isNotEmpty()) probeRestart(where, placed)
        if (placed.any { it.piece == PieceId.PG }) probeBoardBadge(where, placed)
    }

    /** "Solved view: `nav-prev`, `nav-next`, `counter`, `retry`, `solved-next`. `best-time` is inert, not a control." */
    private fun sweepSolvedView(where: String, p: PuzzleDefinition) {
        val listed = linkedMapOf(
            KitAppTag.NAV_PREV to NAV_MIN_DP,
            KitAppTag.NAV_NEXT to NAV_MIN_DP,
            KitAppTag.COUNTER to CONTROL_MIN_DP,
            KitAppTag.GEAR to CONTROL_MIN_DP,
            KitAppTag.RETRY to CONTROL_MIN_DP,
            KitAppTag.SOLVED_NEXT to CONTROL_MIN_DP,
        )
        for ((tag, min) in listed) assertMinimum(where, tag, min)
        assertEveryClickableListed(where, listed.keys)
        val id = p.id
        val solvedRow = { kit.awaitSolvedRow("$where: the solved row before the probe") }
        probeBrowse(where, id, KitAppTag.NAV_PREV, before(id), prepare = solvedRow)
        probeBrowse(where, id, KitAppTag.NAV_NEXT, after(id), prepare = solvedRow)
        probeCounter(where, prepare = solvedRow)
        probeGear(where, prepare = solvedRow)
        // decisions.md 2026-10-15 "Browsing readings no REQ fixes": "The solved row's Next = › (the next puzzle in order)".
        probeBrowse(where, id, KitAppTag.SOLVED_NEXT, after(id), prepare = solvedRow)
        probe(
            where, KitAppTag.RETRY, prepare = solvedRow,
            expect = { { if (kit.shownState() == PuzzleState.NEW) null else "the state is ${kit.shownState()}, expected New (REQ-026: Retry starts the puzzle again)" } },
            restore = {
                kit.solveByController("$where: solved again after the Retry probe")
                kit.awaitSolvedRow("$where: solved again")
            },
        )
    }

    /** "Grid overview: every `overview-cell-<id>`, `overview-done`, and the scrim (the `overview` node outside the sheet)." */
    private fun sweepOverview(where: String, p: PuzzleDefinition) {
        val listed = LinkedHashMap<String, Float>()
        for (q in kit.library.puzzles) listed[KitAppTag.overviewCell(q.id)] = max(CONTROL_MIN_DP, GRID_CELL_MIN_DP)
        listed[KitAppTag.OVERVIEW_DONE] = CONTROL_MIN_DP
        listed[KitAppTag.OVERVIEW] = CONTROL_MIN_DP
        for ((tag, min) in listed) assertMinimum(where, tag, min)
        // S-22 hit order 0: while the grid is open the top bar is inert, so it is not a control of this screen.
        assertEveryClickableListed(where, listed.keys)

        val reopen = {
            if (kit.shownId() != p.id) kit.openByController(p.id, "$where: back to ${p.id}")
            kit.onController { if (!overviewOpen) openOverview() }
            kit.awaitExists(KitAppTag.OVERVIEW, "$where: the grid open again")
        }
        val closed = { if (!kit.exists(KitAppTag.OVERVIEW)) null else "`overview` is still shown" }
        for (q in kit.library.puzzles) {
            val tag = KitAppTag.overviewCell(q.id)
            probe(
                where, tag, points = KitProbePoints.EDGE_MIDDLES,
                prepare = {
                    reopen()
                    composeRule.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo()
                    kit.idle()
                    val n = kit.node(tag)
                    val shown = kit.boundsOf(n)
                    val whole = kit.unclippedBoundsOf(n)
                    val visible = abs(shown.left - whole.left) <= 1f && abs(shown.top - whole.top) <= 1f && abs(shown.right - whole.right) <= 1f && abs(shown.bottom - whole.bottom) <= 1f
                    assertTrue("$where: `$tag` scrolled fully into view before its probe ($shown of $whole)", visible)
                },
                // REQ-050 statement: "one press opens that puzzle"; S-2Δ `choose(id)`: it closes the grid, then opens id.
                expect = { { if (kit.exists(KitAppTag.OVERVIEW)) "`overview` is still shown" else if (kit.shownId() != q.id) "the shown puzzle is ${kit.shownId()}, expected ${q.id}" else null } },
                restore = reopen,
            )
        }
        probe(where, KitAppTag.OVERVIEW_DONE, points = KitProbePoints.EDGE_MIDDLES, prepare = reopen, expect = { closed }, restore = reopen)
        probe(
            where, KitAppTag.OVERVIEW, prepare = reopen,
            owner = { at ->
                val sheet = kit.library.puzzles.map { KitAppTag.overviewCell(it.id) } + KitAppTag.OVERVIEW_DONE
                sheet.firstOrNull { kit.exists(it) && kit.bounds(it).contains(at) }?.let { "inside `$it`, on the sheet" }
            },
            expect = { closed },
            restore = reopen,
        )
    }

    // ── The probes (tasks.md § WO-006 "The player controls per screen", Probe) ─────────────────────────────

    /**
     * The probe: a still tap at each of four points of [tag]'s bounds, each read on the screen in use and asserted inside the
     * control's bounds before it is tapped, must make the control act. [points]: 1 dp inside each corner (the router's
     * rectangular areas and the scrim, the Q5 row), or 1 dp inside the middle of each edge (a clipped rounded shape, whose
     * corners Compose's hit test leaves out: its extent in both directions, REQ-037's "48 dp in both directions"; ruling 3 of
     * decisions.md 2026-10-17 "The device kit's open readings"). [prepare] brings the screen to the state the probe needs;
     * [owner] names what owns a point by the visible-thing-wins rows ("A corner that a drawn shape owns … is not a probe point
     * of that control"); [expect] reads the state before the tap and returns a check naming what failed, or null when the
     * control acted; [restore] undoes the act. At least one point of every control is probed (ruling 7).
     */
    private fun probe(
        where: String,
        tag: String,
        points: KitProbePoints = KitProbePoints.CORNERS,
        prepare: () -> Unit = {},
        owner: (Offset) -> String? = { null },
        expect: () -> (() -> String?),
        restore: () -> Unit,
    ) {
        var probed = 0
        val skipped = ArrayList<String>()
        for (index in 0 until 4) {
            prepare()
            val b = kit.bounds(tag)
            val inset = kit.dp(1f)
            val (name, at) = when (points) {
                KitProbePoints.CORNERS -> when (index) {
                    0 -> "the top-left corner" to Offset(b.left + inset, b.top + inset)
                    1 -> "the top-right corner" to Offset(b.right - inset, b.top + inset)
                    2 -> "the bottom-left corner" to Offset(b.left + inset, b.bottom - inset)
                    else -> "the bottom-right corner" to Offset(b.right - inset, b.bottom - inset)
                }
                KitProbePoints.EDGE_MIDDLES -> when (index) {
                    0 -> "the top edge's middle" to Offset(b.center.x, b.top + inset)
                    1 -> "the right edge's middle" to Offset(b.right - inset, b.center.y)
                    2 -> "the bottom edge's middle" to Offset(b.center.x, b.bottom - inset)
                    else -> "the left edge's middle" to Offset(b.left + inset, b.center.y)
                }
            }
            assertTrue("$where: the probe point at $name, $at, lies inside `$tag` $b", b.contains(at))
            val reason = owner(at)
            if (reason != null) {
                skipped += "$name: $reason"
                continue
            }
            val check = expect()
            kit.tap(at, "$where: $name of `$tag`")
            val failure = check()
            restore()
            if (failure != null) throw AssertionError("$where: a tap 1 dp inside $name of `$tag` at $at did not make it act: $failure")
            probed++
        }
        if (probed == 0) throw AssertionError("$where: no probe point of `$tag` is left ($skipped); the fixture must leave one")
    }

    /** ‹, › and the solved row's Next: the shown puzzle becomes [expected] (REQ-024: the previous or next puzzle; the list wraps). */
    private fun probeBrowse(where: String, id: String, tag: String, expected: String, prepare: () -> Unit) {
        probe(
            where, tag, prepare = prepare,
            expect = { { if (kit.shownId() == expected) null else "the shown puzzle is ${kit.shownId()}, expected $expected" } },
            restore = { kit.openByController(id, "$where: back to $id") },
        )
    }

    /** The counter: the grid opens (REQ-050 statement: "presses the puzzle counter … open a grid"). */
    private fun probeCounter(where: String, prepare: () -> Unit) {
        probe(
            where, KitAppTag.COUNTER, prepare = prepare,
            expect = { { if (kit.exists(KitAppTag.OVERVIEW)) null else "no `overview` after the tap" } },
            restore = {
                kit.onController { if (overviewOpen) closeOverview() }
                kit.awaitGone(KitAppTag.OVERVIEW, "$where: the grid closed again")
            },
        )
    }

    /**
     * The gear: the settings open (REQ-032 statement: "WHEN the player presses the gear in the top bar, the game SHALL open the
     * settings screen"; S-53: it "acts at lift within the slop … and calls `openSettings()`"), read back from the controller
     * (S-1Δ4 `settingsOpen`). Its area is the router's 52 dp column (S-53), probed at its corners as ‹ and › are.
     */
    private fun probeGear(where: String, prepare: () -> Unit) {
        probe(
            where, KitAppTag.GEAR, prepare = prepare,
            expect = { { if (kit.onController { settingsOpen }) null else "settingsOpen is false after the tap" } },
            restore = {
                kit.onController { if (settingsOpen) closeSettings() }
                kit.awaitGone(KitAppTag.SETTINGS_SHEET, "$where: the settings closed again")
            },
        )
    }

    /**
     * The settings screen (S-49's tag row, "Controls: `settings` (the scrim), `settings-done`, `settings-timer` and
     * `settings-sound` (whole-row `toggleable`, Role.Switch, ≥ 56 dp), `settings-reset`, `reset-erase`, `reset-cancel`"), over
     * [p] New (so the Play screen under the scrim has no foundation control of its own): each control's size (REQ-037: 48 dp),
     * the completeness check with the question closed and open, and the probe of each control, the control scrolled wholly into
     * view first (auto-advance on, KitApp `scrollIntoView`). Done and the scrim close the screen; each switch flips its setting
     * (E4: at once, read from the controller) and is put back; Reset asks its question and Cancel answers it; Erase resets —
     * its probe first puts [p] In progress, so the reset can show (S-2Δ4: every puzzle New).
     */
    private fun sweepSettings(where: String, p: PuzzleDefinition) {
        val open = {
            if (!kit.onController { settingsOpen }) kit.onController { openSettings() }
            kit.awaitExists(KitAppTag.SETTINGS_SHEET, "$where: the settings open")
        }
        val inView = { tag: String ->
            kit.scrollIntoView(tag, where)
            assertTrue("$where: `$tag` scrolled wholly into view before its probe", kit.wholeInView(tag))
        }
        val closed = { if (kit.exists(KitAppTag.SETTINGS_SHEET) || kit.onController { settingsOpen }) "the settings screen is still shown" else null }
        val question = {
            open()
            inView(KitAppTag.SETTINGS_RESET)
            kit.pressCentre(KitAppTag.SETTINGS_RESET)
            kit.awaitExists(KitAppTag.RESET_QUESTION, "$where: Reset asks its question")
        }
        open()
        val listed = linkedMapOf(
            KitAppTag.SETTINGS to CONTROL_MIN_DP,
            KitAppTag.SETTINGS_DONE to CONTROL_MIN_DP,
            KitAppTag.SETTINGS_TIMER to CONTROL_MIN_DP,
            KitAppTag.SETTINGS_SOUND to CONTROL_MIN_DP,
            KitAppTag.SETTINGS_RESET to CONTROL_MIN_DP,
        )
        for ((tag, min) in listed) {
            // S-49: "Header (fixed): the title and Done. Body (scrolls): timer, sound, …, reset, …" — only the body's controls
            // have a scrolling parent; the scrim covers the window and Done stays in the fixed header (corrected check, TASK-059
            // hop 1 on the floor: scrolling Done failed "no parent layout with a Scroll SemanticsAction").
            if (tag in KitAppTag.SETTINGS_BODY_ORDER) kit.scrollIntoView(tag, where)
            assertMinimum(where, tag, min)
        }
        assertEveryClickableListed(where, listed.keys)

        probe(where, KitAppTag.SETTINGS_DONE, points = KitProbePoints.EDGE_MIDDLES, prepare = open, expect = { closed }, restore = open)
        for ((tag, flipped) in listOf<Pair<String, () -> Boolean>>(
            KitAppTag.SETTINGS_TIMER to { kit.onController { settings.timerShown } },
            KitAppTag.SETTINGS_SOUND to { kit.onController { settings.soundOn } },
        )) {
            var was = false
            probe(
                where, tag, points = KitProbePoints.EDGE_MIDDLES,
                prepare = { open(); inView(tag) },
                expect = {
                    was = flipped()
                    ({ if (flipped() != was) null else "its setting stayed $was" })
                },
                restore = {
                    kit.onController { if (tag == KitAppTag.SETTINGS_TIMER) setTimerShown(was) else setSoundOn(was) }
                    kit.idle()
                },
            )
        }
        probe(
            where, KitAppTag.SETTINGS_RESET, points = KitProbePoints.EDGE_MIDDLES,
            prepare = { open(); inView(KitAppTag.SETTINGS_RESET) },
            expect = { { if (kit.exists(KitAppTag.RESET_QUESTION)) null else "no `reset-question` after the tap" } },
            restore = {
                kit.pressCentre(KitAppTag.RESET_CANCEL)
                kit.awaitGone(KitAppTag.RESET_QUESTION, "$where: Cancel after the Reset probe")
            },
        )
        probe(
            where, KitAppTag.SETTINGS, prepare = open,
            owner = { at -> if (kit.exists(KitAppTag.SETTINGS_SHEET) && kit.bounds(KitAppTag.SETTINGS_SHEET).contains(at)) "inside `settings-sheet`, on the sheet" else null },
            expect = { closed },
            restore = open,
        )

        question()
        val withQuestion = linkedMapOf(KitAppTag.RESET_ERASE to CONTROL_MIN_DP, KitAppTag.RESET_CANCEL to CONTROL_MIN_DP)
        for ((tag, min) in withQuestion) {
            kit.scrollIntoView(tag, where)
            assertMinimum("$where, the reset question", tag, min)
        }
        assertEveryClickableListed("$where, the reset question", listed.keys + withQuestion.keys)
        probe(
            where, KitAppTag.RESET_CANCEL, points = KitProbePoints.EDGE_MIDDLES,
            prepare = { if (!kit.exists(KitAppTag.RESET_QUESTION)) question(); inView(KitAppTag.RESET_CANCEL) },
            expect = { { if (!kit.exists(KitAppTag.RESET_QUESTION)) null else "`reset-question` is still shown" } },
            restore = {},
        )
        val firstLock = kitAppLockOrder(p)?.first() ?: throw AssertionError("kit: $where: no lock order for ${p.id}")
        probe(
            where, KitAppTag.RESET_ERASE, points = KitProbePoints.EDGE_MIDDLES,
            prepare = {
                if (kit.onController { stateOf(p.id) } == PuzzleState.NEW) {
                    kit.onController { if (settingsOpen) closeSettings() }
                    kit.awaitGone(KitAppTag.SETTINGS_SHEET, "$where: the settings closed to set up the Erase probe")
                    kit.placeByController(firstLock, "$where: ${p.id} In progress before the Erase probe")
                }
                if (!kit.exists(KitAppTag.RESET_QUESTION)) question()
                inView(KitAppTag.RESET_ERASE)
            },
            expect = { { if (kit.onController { stateOf(p.id) } == PuzzleState.NEW && !kit.exists(KitAppTag.RESET_QUESTION)) null else "${p.id} is ${kit.onController { stateOf(p.id) }} after the tap (S-2Δ4: every puzzle New)" } },
            restore = {},
        )
        kit.onController { if (settingsOpen) closeSettings() }
        kit.awaitGone(KitAppTag.SETTINGS_SHEET, "$where: the settings closed at the end of the sweep")
    }

    /**
     * A tray cell holding its piece: the piece turns one step (REQ-016: a tap turns a tray piece 45° clockwise; REQ-037 rule 2:
     * its touch area is its whole cell). Inside the parallelogram's cell, the tray badge's area takes a corner no drawn piece
     * covers (decisions.md 2026-10-12 "Inside the parallelogram's tray cell the visible thing wins"); where the badge's disc is
     * drawn inside its area is not a node, so every corner inside that area is left out.
     */
    private fun probeTrayCell(where: String, piece: PieceId) {
        val miniature = PlayTags.trayPiece(piece)
        probe(
            where, PlayTags.trayCell(piece),
            owner = { at ->
                if (piece == PieceId.PG && kit.exists(PlayTags.FLIP_BADGE_TRAY) && kit.bounds(PlayTags.FLIP_BADGE_TRAY).contains(at)) "the tray badge's area" else null
            },
            expect = {
                val was = kit.key(miniature, PlaySemantics.TurnKey)
                ({ val now = kit.key(miniature, PlaySemantics.TurnKey); if (now == was.next()) null else "$piece's tray turn is $now, expected ${was.next()}" })
            },
            restore = {
                kit.onController { repeat(7) { tapTray(piece) } }
                kit.idle()
            },
        )
    }

    /**
     * The tray badge: the parallelogram mirrors in the tray (REQ-018; REQ-037 rule 1: 60 dp). A corner on the parallelogram's
     * drawn shape belongs to the piece unless the badge's disc is drawn there (decisions.md 2026-10-12 "What 'drawn' means in
     * the touch order"); the disc is not a node, so such a corner is left out.
     */
    private fun probeTrayBadge(where: String) {
        val miniature = PlayTags.trayPiece(PieceId.PG)
        probe(
            where, PlayTags.FLIP_BADGE_TRAY,
            owner = { at -> if (kitAppInside(at, kit.trayPolygonPx(PieceId.PG))) "the parallelogram's drawn shape" else null },
            expect = {
                val was = kit.key(miniature, PlaySemantics.MirroredKey)
                ({ val now = kit.key(miniature, PlaySemantics.MirroredKey); if (now != was) null else "the parallelogram's tray mirror stayed $now" })
            },
            restore = {
                kit.onController { check(pressBadge()) { "kit: pressBadge() refused (undoing the tray badge probe)" } }
                kit.idle()
            },
        )
    }

    /**
     * Restart: the puzzle returns to New with every piece in the tray (REQ-025 statement). Owners in S-22's hit order 3 (with the
     * aid, § WO-005): the DEV button, a board badge's disc (not a node: the whole board badge area is left out), then a piece's
     * drawn shape beat the Restart pill.
     */
    private fun probeRestart(where: String, placed: List<Placement>) {
        probe(
            where, KitAppTag.RESTART,
            owner = { at -> boardOwner(at, includeRestart = false) },
            expect = {
                {
                    val onBoard = PieceId.entries.filter { kit.exists(PlayTags.boardPiece(it)) }
                    if (kit.shownState() == PuzzleState.NEW && onBoard.isEmpty()) null else "the state is ${kit.shownState()} with $onBoard on the board, expected New and an empty board"
                }
            },
            restore = { rebuild(placed, "$where: the board again after the Restart probe") },
        )
    }

    /**
     * The board badge: the placed parallelogram mirrors in place, or — where the mirrored piece does not lock — it mirrors back
     * with a short shake (REQ-018 rule; S-2 `pressBadge`: `None` → unchanged, `shake`). Owners: the DEV button, a piece's drawn
     * shape (unless the disc is drawn there; the disc is not a node), and the Restart pill beat the rest of the badge's area.
     */
    private fun probeBoardBadge(where: String, placed: List<Placement>) {
        probe(
            where, PlayTags.FLIP_BADGE_BOARD,
            owner = { at -> boardOwner(at, includeRestart = true) },
            expect = {
                val wasMirror = kit.onController { this.placed.single { it.piece == PieceId.PG }.mirrored }
                val wasShake = kit.onController { shake }
                ({
                    val nowMirror = kit.onController { this.placed.firstOrNull { it.piece == PieceId.PG }?.mirrored }
                    val nowShake = kit.onController { shake }
                    val mirrored = nowMirror != null && nowMirror != wasMirror
                    val shook = nowShake != null && nowShake.piece == PieceId.PG && nowShake.serial != wasShake?.serial
                    if (mirrored || shook) null else "the parallelogram neither mirrored nor shook (mirror $wasMirror → $nowMirror, shake $wasShake → $nowShake)"
                })
            },
            restore = { rebuild(placed, "$where: the board again after the board badge probe") },
        )
    }

    /** What owns a board point before Restart or the rest of a badge's area in S-22's hit order 3, or null. */
    private fun boardOwner(at: Offset, includeRestart: Boolean): String? {
        if (kit.exists(DevAidTags.DEV_BUTTON) && kit.bounds(DevAidTags.DEV_BUTTON).contains(at)) return "the DEV button (first in hit order 3)"
        if (!includeRestart && kit.exists(PlayTags.FLIP_BADGE_BOARD) && kit.bounds(PlayTags.FLIP_BADGE_BOARD).contains(at)) return "the board badge's area, whose disc beats Restart"
        for (piece in PieceId.entries) {
            val tag = PlayTags.boardPiece(piece)
            if (kit.exists(tag) && kitAppInside(at, kit.boardPolygonPx(tag))) return "$piece's drawn shape"
        }
        if (includeRestart && kit.exists(KitAppTag.RESTART) && kit.bounds(KitAppTag.RESTART).contains(at)) return "the Restart pill"
        return null
    }

    /** Puts the open puzzle back to [placed] through S-2 when a probe changed its board. */
    private fun rebuild(placed: List<Placement>, what: String) {
        val now = kit.onController { state to this.placed.toList() }
        if (now.first == PuzzleState.IN_PROGRESS && kitAppSameBoard(now.second, placed)) return
        kit.onController { if (state == PuzzleState.IN_PROGRESS) restart() }
        kit.idle()
        placed.forEach { kit.placeByController(it, what) }
    }

    // ── Sizes and completeness ─────────────────────────────────────────────────────────────────────────

    /**
     * [tag]'s node is there and its bounds (its touch area, S-13Δ and the tag tables) are at least [minDp] in both directions, in
     * dp at the launched activity's density, within the 1 px a dp edge rounds to.
     */
    private fun assertMinimum(where: String, tag: String, minDp: Float) {
        assertTrue("$where: `$tag` is shown", kit.exists(tag))
        val n = kit.node(tag)
        val density = kit.density(where)
        val w = n.size.width / density
        val h = n.size.height / density
        val slack = 1f / density
        assertTrue("$where: `$tag`'s touch area is $w × $h dp, below its $minDp dp minimum (REQ-037)", w >= minDp - slack && h >= minDp - slack)
    }

    /**
     * "Completeness: every node with a click action in the tree is in this list or is a `dev-*` node." A clickable node without
     * a tag counts as the control of its nearest tagged ancestor only when it spans exactly that control's bounds (±1 px): S-13Δ,
     * "A control's bounds are its touch area" — the scrim's clickable under `overview`, for one. Any other untagged clickable
     * (a button inside the sheet, say) is a stray.
     */
    private fun assertEveryClickableListed(where: String, listed: Set<String>) {
        fun allowed(tag: String) = tag in listed || tag.startsWith(KIT_APP_DEV_PREFIX)
        val stray = kit.allNodes()
            .filter { n -> SemanticsActions.OnClick in n.config || SemanticsActions.OnLongClick in n.config }
            .mapNotNull { n ->
                val tag = n.config.getOrNull(SemanticsProperties.TestTag)
                if (tag != null) {
                    if (allowed(tag)) null else "$tag at ${kit.boundsOf(n)}"
                } else {
                    var a = n.parent
                    while (a != null && a.config.getOrNull(SemanticsProperties.TestTag) == null) a = a.parent
                    val owner = a?.config?.getOrNull(SemanticsProperties.TestTag)
                    val spans = a != null && sameBox(kit.boundsOf(a), kit.boundsOf(n))
                    if (owner != null && spans && allowed(owner)) null
                    else "(untagged) at ${kit.boundsOf(n)}, nearest tagged ancestor `$owner`" + (if (a != null) " at ${kit.boundsOf(a)}" else "")
                }
            }
        assertTrue("$where: nodes with a click action that are no listed player control of this screen and no `dev-*` node: $stray", stray.isEmpty())
    }

    private fun sameBox(a: androidx.compose.ui.geometry.Rect, b: androidx.compose.ui.geometry.Rect): Boolean =
        abs(a.left - b.left) <= 1f && abs(a.top - b.top) <= 1f && abs(a.right - b.right) <= 1f && abs(a.bottom - b.bottom) <= 1f

    /**
     * The In-progress fixture: the parallelogram placed (so `flip-badge-board` shows) with the pieces the engine needs before it,
     * bottom pieces first so Restart's corner stays clear where the bundle allows, and at least one piece left in the tray.
     */
    private fun inProgressFixture(p: PuzzleDefinition, leg: String): List<Placement> {
        val bottomFirst = p.solution.sortedByDescending { s -> s.polygon.maxOf { it.y } }.map { it.piece }
        val order = kitAppLockOrder(p, prefer = listOf(PieceId.PG) + bottomFirst.filter { it != PieceId.PG })
            ?: throw AssertionError("kit: $leg: no lock order for ${p.id}")
        val upToPg = order.take(order.indexOfFirst { it.piece == PieceId.PG } + 1)
        if (upToPg.size >= p.pieces.size) throw AssertionError("kit: $leg: placing the parallelogram of ${p.id} needs every piece; no tray cell would be left to sweep")
        return upToPg
    }

    private fun before(id: String): String {
        val all = kit.library.puzzles
        val i = all.indexOfFirst { it.id == id }
        return all[(i - 1 + all.size) % all.size].id
    }

    private fun after(id: String): String {
        val all = kit.library.puzzles
        val i = all.indexOfFirst { it.id == id }
        return all[(i + 1) % all.size].id
    }

    /** Where the probe taps (the Probe line of tasks.md § WO-006, as narrowed by ruling 3 of the open-readings row). */
    private enum class KitProbePoints { CORNERS, EDGE_MIDDLES }

    private companion object {
        /** REQ-037 statement: "a touch area of at least 48 dp in both directions". */
        const val CONTROL_MIN_DP = 48f

        /** REQ-037 rule 1: "‹ and › are 52 dp". */
        const val NAV_MIN_DP = 52f

        /** REQ-037 rule 1 and REQ-018 rule: "the flip badge's touch area is 60 dp". */
        const val BADGE_MIN_DP = 60f

        /** REQ-050 rule: "Each grid cell's touch area is at least 64 dp square (REQ-037)" (a pin of the sweep, no token of REQ-050). */
        const val GRID_CELL_MIN_DP = 64f
    }
}
