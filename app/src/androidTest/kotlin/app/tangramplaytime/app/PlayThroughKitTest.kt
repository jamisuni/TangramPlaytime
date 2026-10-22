// Acceptance kit (WO-006, TASK-030): the play-through and the free-and-offline promise in the real app (REQ-006, REQ-001,
// REQ-008, REQ-010; decisions.md 2026-10-17 "What 'fully playable' means, and the exact sizes" (Q9) and "The promise lines'
// oracles" (Q8, as amended); capture-outbox/chg-draft-req006-a1-playable-oracle.md; tasks.md § WO-006 seam notes 2–5).
// - The play-through, once per leg (Q9: "a 360 × 780 dp phone in portrait and … a 1280 × 800 dp tablet in both orientations,
//   on all three images"): the library's first mini puzzle, opened New through the controller, is solved by touch; a full
//   puzzle is reached by touch (the solved row's Next, the counter, the grid's cell) and solved by touch; the leg ends with
//   Retry by touch. Every lock is made by a drop (REQ-019, REQ-021); across the leg every move of REQ-014 to REQ-018 is used:
//   a drag from the tray, a tap turn in the tray, a two-finger twist, the flip badge. The Solved state and its picture are
//   reached (REQ-022, REQ-023). Positions are read from the nodes on the screen in use, never from a number in a brief.
// - At every step (Q8 part (3)): the closed world of texts, no web or foreign view, `MainActivity` the only resumed activity
//   with window focus, the active window the app's (`KitApp.assertPromise`).
// - Airplane mode (REQ-010 A1; seam note 5; decisions.md 2026-10-17 "The floor routes, as measured"): on and read back for the
//   whole play-through. API 30+: the kit switches it on and off. Below API 30 with `kitAirplaneByChannel=true` (floor step
//   (e), this class run alone): the channel line switched it on by the measured root route; the kit requires it on at the
//   start and after the last step and never switches it. Below API 30 without the argument (the floor's ordinary run): the
//   play-through still runs and asserts, without airplane mode, and its log points at floor step (e). Nothing here skips.
// - The installed app (REQ-010 A2) on every image; its release-APK leg is TASK-027's `ReleasePromiseKitTest`. Amended WO-009
//   (TASK-064; decisions.md 2026-10-22 R6): REQ-048 A3's token joins REQ-010 A2's on that leg — a token added, no assertion
//   changed.
// - The app's controller is one per process and shared by every class of the run: each test leaves it, and the device, as
//   it found them (seam note 3; `KitApp.restoreRule()`).
// - Amended WO-007 (TASK-058; designs/WO-007-design.md §2 "Tokens (Q10)", re-covered: "`PlayThroughKitTest` (REQ-001 A1,
//   REQ-008, REQ-010 A1): the gear, the whole sheet scrolled under the closed world, both switches flipped and back, Reset then
//   Cancel, then Done" — the design's words, its IDs written here without the dot; decisions.md 2026-10-21 "Scope of WO-007 …" (E1): a new
//   screen changes those lines' truth): each leg walks the settings screen after its last Retry (`walkTheSettings`). The
//   closed world admits the screen's times (E10, KitApp `closedWorld`). This file's tokens are unchanged.
package app.tangramplaytime.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.PlaySemantics
import app.tangramplaytime.play.PlayTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class PlayThroughKitTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val kit = KitApp(composeRule)

    @get:Rule(order = 1)
    val kitRestore: TestRule = kit.restoreRule()

    @Test // REQ-006.A1 REQ-001.A1 REQ-008.A1 REQ-008.A2 REQ-010.A1
    fun onA360By780DpPhoneInPortraitTheFirstMiniAndAFullPuzzleAreSolvedByTouchWithNoMoneyAskAndNoNetwork() {
        // REQ-006 A1: "The game is fully playable on a phone in portrait and on a tablet." (Q9: verified as locked by this
        // play-through, which also meets the pending draft word for word.) REQ-001 A1: "A full play-through of every screen shows
        // no ad, no price and no request for money." REQ-008 A1: "No ad, price, purchase, tip or donation prompt appears
        // anywhere." REQ-008 A2: "No rating prompt appears." REQ-010 A1: "With the device in airplane mode, every feature works."
        // This leg: the phone at 360 × 780 dp in portrait (a display override read back from the launched activity).
        playThrough("the 360 × 780 dp phone in portrait") { kit.usePhone(KIT_PHONE_360, it) }
    }

    @Test // REQ-006.A1 REQ-001.A1 REQ-008.A1 REQ-008.A2 REQ-010.A1
    fun onA1280By800DpTabletTheFirstMiniAndAFullPuzzleAreSolvedByTouchWithNoMoneyAskAndNoNetwork() {
        // The same lines as the phone leg (see its comment); this leg: the tablet at 1280 × 800 dp, landscape.
        playThrough("the 1280 × 800 dp tablet") { kit.useTablet(portrait = false, what = it) }
    }

    @Test // REQ-006.A1 REQ-001.A1 REQ-008.A1 REQ-008.A2 REQ-010.A1
    fun onAn800By1280DpTabletTheFirstMiniAndAFullPuzzleAreSolvedByTouchWithNoMoneyAskAndNoNetwork() {
        // The same lines as the phone leg (see its comment); this leg: the tablet at 800 × 1280 dp, portrait by a frozen rotation.
        playThrough("the 800 × 1280 dp tablet") { kit.useTablet(portrait = true, what = it) }
    }

    @Test // REQ-010.A2 REQ-048.A3
    fun theInstalledAppDeclaresNoPermissionsNotEvenNetworkAccess() {
        // REQ-010 A2: "The installed app declares no permissions, not even network access." Rule: "The game requests no Android
        // permissions." Q8 part (2): the installed app on every image, no requested or declared permission. Oracle: the
        // `PackageManager.GET_PERMISSIONS` view of `app.tangramplaytime`: `requestedPermissions` and `permissions` null or empty.
        // Fixture: the same call on the test APK's own package returns its requested permissions (androidx.test's core library
        // asks for one), so an empty answer for the app is a read that could have seen a permission.
        // REQ-048 A3: "The installed app still declares no permissions (REQ-010)." The same assertions carry it (WO-009,
        // TASK-064): decisions.md 2026-10-22 R6 ("REQ-048 A3 is the release manifest and the installed app; …", its IDs quoted
        // here without the dots): "The token joins REQ-010 A2's … on `PlayThroughKitTest` (the installed app, three images)".
        // No assertion changed.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = packageInfo(kit.target.packageManager, KIT_APP_PACKAGE)
        val requested = app.requestedPermissions?.toList().orEmpty()
        val declared = app.permissions?.map { it.name }.orEmpty()
        assertTrue("the installed $KIT_APP_PACKAGE requests no permission: $requested", requested.isEmpty())
        assertTrue("the installed $KIT_APP_PACKAGE declares no permission: $declared", declared.isEmpty())
        val testPackage = instrumentation.context.packageName
        val control = packageInfo(instrumentation.context.packageManager, testPackage).requestedPermissions?.toList().orEmpty()
        assertTrue("fixture: the same read on the test APK $testPackage sees its requested permissions (it is not blind)", control.isNotEmpty())
    }

    // ── The play-through ──────────────────────────────────────────────────────────────────────────────

    /** Which moves of REQ-014 to REQ-018 the leg has used. */
    private class KitMoves {
        var drag = false
        var tapTurn = false
        var twist = false
        var badge = false
    }

    /** One piece's moves: tray taps (REQ-016), badge presses (REQ-018), twist steps and a ±45° twist pair (REQ-017). */
    private data class KitRoute(val taps: Int, val badgePresses: Int, val twistSteps: Int, val twistPair: Boolean)

    private fun playThrough(leg: String, layout: (String) -> Unit) {
        kit.start("the play-through, $leg")
        assertEquals(
            "the play-through reads the app's English texts (G-10: English is the Contract wording); the device's list is ${kit.deviceLocales().toLanguageTags()}",
            "en", kit.deviceLocales()[0].language,
        )
        val allowed = kit.closedWorld()
        val priced = allowed.filter { t -> t.any { Character.getType(it) == Character.CURRENCY_SYMBOL.toInt() } }
        assertTrue("none of the app's own texts holds a currency symbol (REQ-008 A1: no price): $priced", priced.isEmpty())
        val airplane = airplaneOnForTheWholePlayThrough(leg)
        layout(leg)

        val mini = kit.library.puzzles.first()
        assertEquals("the library's first puzzle is a mini (IPuzzleLibrary KDoc of `puzzles`)", PuzzleKind.MINI, mini.kind)
        assertEquals("precondition: ${mini.id} is New (a cleared store)", PuzzleState.NEW, kit.onController { stateOf(mini.id) })
        val full = kit.firstNew(PuzzleKind.FULL, "$leg: a full puzzle to reach by touch") { it.pieces.size == 7 }
        val moves = KitMoves()

        kit.openByController(mini.id, "$leg: the first mini, opened New through the controller")
        kit.assertPromise("$leg: ${mini.id} New", allowed)
        solveByTouch(leg, mini, moves, allowed, last = false)
        assertSolved(leg, mini, allowed)

        // Reach the full puzzle by touch: the solved row's Next (decisions.md 2026-10-15: Next = ›), then the counter (REQ-050:
        // the counter opens the grid), then the full puzzle's cell ("one press opens that puzzle").
        val next = kit.library.puzzles[(kit.library.puzzles.indexOfFirst { it.id == mini.id } + 1) % kit.library.puzzles.size].id
        kit.pressCentre(KitAppTag.SOLVED_NEXT)
        kit.await("$leg: Next shows $next") { kit.shownId() == next }
        kit.assertPromise("$leg: Next to $next", allowed)
        kit.pressCentre(KitAppTag.COUNTER)
        kit.awaitExists(KitAppTag.OVERVIEW, "$leg: the counter opens the grid")
        kit.assertPromise("$leg: the grid overview", allowed)
        bringGridCellIntoViewByTouch(full.id, leg, allowed)
        val cell = kit.bounds(KitAppTag.overviewCell(full.id))
        kit.tap(cell.center, "$leg: ${full.id}'s cell in the grid")
        kit.await("$leg: the grid's cell opens ${full.id}") { !kit.exists(KitAppTag.OVERVIEW) && kit.shownId() == full.id }
        assertEquals("$leg: ${full.id} opens New", PuzzleState.NEW, kit.shownState())
        kit.assertPromise("$leg: ${full.id} New", allowed)

        solveByTouch(leg, full, moves, allowed, last = true)
        assertSolved(leg, full, allowed)

        // End with Retry by touch (REQ-026: an empty board, every piece in the tray).
        kit.pressCentre(KitAppTag.RETRY)
        kit.await("$leg: Retry gives ${full.id} New") { kit.shownState() == PuzzleState.NEW }
        for (piece in full.pieces) assertTrue("$leg: after Retry $piece is in the tray", kit.exists(PlayTags.trayPiece(piece)))
        assertTrue("$leg: after Retry nothing is on the board", PieceId.entries.none { kit.exists(PlayTags.boardPiece(it)) })
        kit.assertPromise("$leg: ${full.id} after Retry", allowed)

        walkTheSettings(leg, allowed)

        assertTrue("$leg: a drag from the tray was used (REQ-014)", moves.drag)
        assertTrue("$leg: a tap turn in the tray was used (REQ-016)", moves.tapTurn)
        assertTrue("$leg: a two-finger twist was used (REQ-017)", moves.twist)
        assertTrue("$leg: the flip badge was used (REQ-018)", moves.badge)
        if (airplane) assertTrue("$leg: airplane mode is still on after the last step (`settings get global airplane_mode_on`)", kit.airplaneModeOn())
    }

    /**
     * The settings screen, walked by touch under the closed world (design §2 "Tokens (Q10)", re-covered; REQ-001 A1: "A full
     * play-through of every screen shows no ad, no price and no request for money"; REQ-008 A1 and A2; REQ-010 A1, airplane mode
     * still on): the gear opens it; the whole body is scrolled, item by item in REQ-032 rule 1's order, the promise checked at
     * each; the timer switch is turned on and straight back off, and the sound switch off, checked, and back on, each by touch
     * and read back from the controller; Reset asks its question, which is checked, and Cancel answers it; Done closes the
     * screen. The promise is not checked while the timer is on: the board's timer then shows the puzzle's time, a text the
     * closed world does not admit (E10 admits the settings screen's times; C3 the solved row's best time).
     */
    private fun walkTheSettings(leg: String, allowed: Set<String>) {
        kit.openSettingsByTouch("$leg: the gear")
        kit.assertPromise("$leg: the settings screen", allowed)
        for (tag in KitAppTag.SETTINGS_BODY_ORDER) {
            kit.scrollIntoView(tag, "$leg: the settings screen")
            kit.assertPromise("$leg: the settings screen, `$tag` in view", allowed)
        }

        kit.scrollIntoView(KitAppTag.SETTINGS_TIMER, "$leg: the timer switch")
        val timer = kit.onController { timerShown }
        kit.pressCentre(KitAppTag.SETTINGS_TIMER)
        kit.await("$leg: the timer switch flipped (E4: at once)") { kit.onController { timerShown } != timer }
        kit.pressCentre(KitAppTag.SETTINGS_TIMER)
        kit.await("$leg: the timer switch back") { kit.onController { timerShown } == timer }
        kit.assertPromise("$leg: the timer switch flipped and back", allowed)

        kit.scrollIntoView(KitAppTag.SETTINGS_SOUND, "$leg: the sound switch")
        val sound = kit.onController { settings.soundOn }
        kit.pressCentre(KitAppTag.SETTINGS_SOUND)
        kit.await("$leg: the sound switch flipped (E4: at once)") { kit.onController { settings.soundOn } != sound }
        kit.assertPromise("$leg: the sound switch flipped", allowed)
        kit.pressCentre(KitAppTag.SETTINGS_SOUND)
        kit.await("$leg: the sound switch back") { kit.onController { settings.soundOn } == sound }
        kit.assertPromise("$leg: the sound switch back", allowed)

        kit.scrollIntoView(KitAppTag.SETTINGS_RESET, "$leg: Reset")
        kit.pressCentre(KitAppTag.SETTINGS_RESET)
        kit.awaitExists(KitAppTag.RESET_QUESTION, "$leg: Reset asks its question")
        kit.assertPromise("$leg: the reset question", allowed)
        kit.pressCentre(KitAppTag.RESET_CANCEL)
        kit.awaitGone(KitAppTag.RESET_QUESTION, "$leg: Cancel answers it")
        kit.assertPromise("$leg: after Cancel", allowed)

        kit.closeSettingsByDone("$leg: Done")
        kit.assertPromise("$leg: the settings closed", allowed)
    }

    /** Seam note 5's three cases (as measured on the floor); true when airplane mode is on and under test for this play-through. */
    private fun airplaneOnForTheWholePlayThrough(leg: String): Boolean {
        if (kit.sdk >= 30) {
            kit.switchAirplaneModeOn("$leg: airplane mode for the whole play-through")
            return true
        }
        if (kit.channelAirplane()) {
            assertTrue("$leg: airplane mode is on at the start, switched on as root by the floor channel line (`kitAirplaneByChannel`)", kit.airplaneModeOn())
            return true
        }
        Log.w(
            "PlayThroughKitTest",
            "$leg: API ${kit.sdk} without kitAirplaneByChannel: this ordinary run plays through without airplane mode (not a skip); " +
                "REQ-010 A1's floor evidence is floor step (e), this class alone with kitAirplaneByChannel=true " +
                "(decisions.md 2026-10-17 \"The floor routes, as measured\")",
        )
        return false
    }

    /** The Solved state (REQ-022), its picture (REQ-023) and the solved row, read from the nodes after the last drop. */
    private fun assertSolved(leg: String, p: PuzzleDefinition, allowed: Set<String>) {
        kit.await("$leg: ${p.id} Solved after its last lock (REQ-022)") { kit.shownState() == PuzzleState.SOLVED }
        kit.awaitExists(PlayTags.SOLVED_PICTURE, "$leg: ${p.id}'s picture (REQ-023)")
        kit.awaitSolvedRow("$leg: ${p.id}'s solved view")
        kit.assertPromise("$leg: ${p.id} Solved", allowed)
    }

    /**
     * Solves [p] by touch, piece by piece, in an order the engine locks at the solution poses (S-27's search). The moves are
     * planned before the first touch from the tray turns the nodes show: a piece whose turn must change is turned by tray taps
     * or by a twist, alternately; the parallelogram is mirrored by the tray badge when its pose is mirrored. On the leg's last
     * puzzle, a move not yet used is forced: eight tray taps (a full turn, REQ-016 A1's rule), a ±45° twist pair, or two badge
     * presses.
     */
    private fun solveByTouch(leg: String, p: PuzzleDefinition, moves: KitMoves, allowed: Set<String>, last: Boolean) {
        val order = kitAppLockOrder(p) ?: throw AssertionError("kit: no lock order for ${p.id}")
        val restTurn = order.associate { it.piece to kit.key(PlayTags.trayPiece(it.piece), PlaySemantics.TurnKey) }
        val restMirror = if (PieceId.PG in p.pieces) kit.key(PlayTags.trayPiece(PieceId.PG), PlaySemantics.MirroredKey) else false
        val steps = order.map { ((it.turn.step - restTurn.getValue(it.piece).step) % 8 + 8) % 8 }
        var tapPlanned = moves.tapTurn
        var twistPlanned = moves.twist
        var useTapNext = !tapPlanned || twistPlanned
        val routes = order.mapIndexed { i, t ->
            val k = steps[i]
            var taps = 0
            var twistSteps = 0
            if (k != 0) {
                if (useTapNext) {
                    taps = k
                    tapPlanned = true
                } else {
                    twistSteps = if (k <= 4) k else k - 8
                    twistPlanned = true
                }
                useTapNext = !useTapNext
            }
            val badge = if (t.piece == PieceId.PG && t.mirrored != restMirror) 1 else 0
            KitRoute(taps, badge, twistSteps, twistPair = false)
        }.toMutableList()
        if (last) {
            if (!tapPlanned) routes[0] = routes[0].copy(taps = routes[0].taps + 8)
            if (!twistPlanned) routes[0] = routes[0].copy(twistPair = true)
            val pg = order.indexOfFirst { it.piece == PieceId.PG }
            if (!moves.badge && routes.none { it.badgePresses > 0 }) {
                if (pg < 0) throw AssertionError("kit: $leg: the leg's last puzzle ${p.id} has no parallelogram; the flip badge cannot be used")
                routes[pg] = routes[pg].copy(badgePresses = 2)
            }
        }
        for ((i, target) in order.withIndex()) {
            placeByTouch(leg, p, target, routes[i], moves, allowed, isLast = i == order.lastIndex)
        }
    }

    /**
     * One piece by touch: the tray taps; the badge presses; the drag from its cell (REQ-014, REQ-015); the twist (REQ-017); a drop
     * point from which the engine locks it at [target] with the finger over the board (TYPE-004: the search snaps it; the finger
     * read from the drag); the landing preview at [target] (REQ-021); the release, which locks it there (REQ-019, REQ-021 A1's
     * rule). On the puzzle's last piece the drop solves it, and that is read from the state, not from a piece the picture replaces.
     */
    private fun placeByTouch(leg: String, p: PuzzleDefinition, target: Placement, route: KitRoute, moves: KitMoves, allowed: Set<String>, isLast: Boolean) {
        val piece = target.piece
        val where = "$leg, ${p.id}, $piece"
        val miniature = PlayTags.trayPiece(piece)
        if (route.taps > 0) {
            val was = kit.key(miniature, PlaySemantics.TurnKey)
            kit.tapTrayPiece(piece, route.taps)
            var expected = was
            repeat(route.taps) { expected = expected.next() }
            assertEquals("$where: $piece's tray turn after ${route.taps} taps (REQ-016: one 45° step each)", expected, kit.key(miniature, PlaySemantics.TurnKey))
            moves.tapTurn = true
            kit.assertPromise("$where: after ${route.taps} tray taps", allowed)
        }
        repeat(route.badgePresses) {
            val was = kit.key(miniature, PlaySemantics.MirroredKey)
            kit.tap(kit.trayBadgePoint(), "$where: the tray badge")
            assertEquals("$where: the tray badge mirrors the parallelogram (REQ-018)", !was, kit.key(miniature, PlaySemantics.MirroredKey))
            moves.badge = true
            kit.assertPromise("$where: after the badge", allowed)
        }

        var finger = kit.pickUpFromTray(piece)
        moves.drag = true
        val dragTag = PlayTags.dragPiece(piece)
        kit.assertPromise("$where: dragged from the tray", allowed)
        if (route.twistSteps != 0 || route.twistPair) {
            finger = kit.moveFinger(finger, kit.bounds(PlayTags.BOARD).center, 6)
            if (route.twistPair) {
                val was = kit.key(dragTag, PlaySemantics.TurnKey)
                kit.twist(finger, 45.0)
                assertEquals("$where: a 45° twist turns the dragged piece one step (REQ-017)", was.next(), kit.key(dragTag, PlaySemantics.TurnKey))
                kit.twist(finger, -45.0)
                assertEquals("$where: a −45° twist turns it back (REQ-017)", was, kit.key(dragTag, PlaySemantics.TurnKey))
            }
            if (route.twistSteps != 0) kit.twist(finger, route.twistSteps * 45.0)
            moves.twist = true
            kit.assertPromise("$where: after the twist", allowed)
        }
        assertEquals("$where: the dragged piece's turn is its pose's", target.turn, kit.key(dragTag, PlaySemantics.TurnKey))
        assertEquals("$where: the dragged piece's mirror is its pose's", target.mirrored, kit.key(dragTag, PlaySemantics.MirroredKey))

        val offset = finger - kit.toPx(kit.key(dragTag, PlaySemantics.CentreKey))
        val drop = planDrop(p, target, offset)
            ?: throw AssertionError("kit: $where: no drop point within the lock distance locks $piece at $target with the finger over the board (finger − drawn centre $offset px)")
        finger = kit.steerDrag(piece, drop, finger)
        val board = kit.bounds(PlayTags.BOARD)
        assertTrue("kit: $where: the finger $finger stays over the board $board, so the release is a drop on it (touch routing 3)", board.deflate(2f).contains(finger))
        kit.awaitFrames("$where: the landing preview at $target before the release (REQ-021)") {
            kit.exists(PlayTags.LANDING_PREVIEW) && kitAppSame(
                Placement(
                    kit.key(PlayTags.LANDING_PREVIEW, PlaySemantics.PieceKey), kit.key(PlayTags.LANDING_PREVIEW, PlaySemantics.TurnKey),
                    kit.key(PlayTags.LANDING_PREVIEW, PlaySemantics.MirroredKey), kit.key(PlayTags.LANDING_PREVIEW, PlaySemantics.CentreKey),
                ),
                target,
            )
        }
        kit.assertPromise("$where: over its place, the preview shown", allowed)
        kit.release()
        if (isLast) {
            kit.await("$where: the last lock solves ${p.id} (REQ-022)") { kit.shownState() == PuzzleState.SOLVED }
        } else {
            val tag = PlayTags.boardPiece(piece)
            kit.awaitExists(tag, "$where: locked on the board")
            val shown = Placement(kit.key(tag, PlaySemantics.PieceKey), kit.key(tag, PlaySemantics.TurnKey), kit.key(tag, PlaySemantics.MirroredKey), kit.key(tag, PlaySemantics.CentreKey))
            assertTrue("$where: the drop locked it where the preview was, at $target; shown at $shown (REQ-021 A1's rule, REQ-019)", kitAppSame(shown, target))
            assertEquals("$where: In progress", PuzzleState.IN_PROGRESS, kit.shownState())
            kit.assertPromise("$where: locked", allowed)
        }
    }

    /**
     * A drop centre from which the engine's search (the one the drop uses, TYPE-004) locks [target], also from 0.03 units around
     * it, and whose finger (the drop centre plus the measured finger − drawn-centre gap) lies over the board 4 dp in from its
     * edge: the solution centre first, then points up to 0.6 units higher and 0.2 units aside (within the lock distance).
     */
    private fun planDrop(p: PuzzleDefinition, target: Placement, offset: Offset): Point? {
        val placedNow = kit.onController { placed.toList() }
        val dpPerUnit = kit.onController { boardDpPerUnit }
        val board = kit.bounds(PlayTags.BOARD)
        val inset = kit.dp(4f)
        val inner = Rect(board.left + inset, board.top + inset, board.right - inset, board.bottom - inset)
        val mapping = kit.mapping()
        val around = listOf(0.0 to 0.0, 0.03 to 0.0, -0.03 to 0.0, 0.0 to 0.03, 0.0 to -0.03)
        for (dy in (0..6).map { -0.1 * it }) {
            for (dx in listOf(0.0, 0.1, -0.1, 0.2, -0.2)) {
                val c = Point(target.centre.x + dx, target.centre.y + dy)
                if (!inner.contains(mapping.toPx(c) + offset)) continue
                val locks = around.all { (ex, ey) ->
                    val lock = kitAppEngine.findLock(p, placedNow, target.piece, target.turn, target.mirrored, Point(c.x + ex, c.y + ey), dpPerUnit)
                    lock is LockResult.Locked && kitAppSame(lock.placement, target)
                }
                if (locks) return c
            }
        }
        return null
    }

    /**
     * Scrolls the grid by touch until [id]'s cell is wholly in view: a vertical swipe from the middle of a wholly visible cell,
     * by one and a half cells, towards the target (the grid alone scrolls, decisions.md 2026-10-17 "The grid overview, drawn in
     * detail"). The promise is checked after each swipe.
     */
    private fun bringGridCellIntoViewByTouch(id: String, leg: String, allowed: Set<String>) {
        val tag = KitAppTag.overviewCell(id)
        repeat(12) {
            val n = kit.node(tag)
            val shown = kit.boundsOf(n)
            val whole = kit.unclippedBoundsOf(n)
            if (sameBox(shown, whole)) return
            val anchor = kit.library.puzzles.map { KitAppTag.overviewCell(it.id) }
                .map { kit.node(it) }
                .firstOrNull { sameBox(kit.boundsOf(it), kit.unclippedBoundsOf(it)) }
                ?: throw AssertionError("kit: $leg: no grid cell is wholly in view to swipe from")
            val from = kit.boundsOf(anchor).center
            val distance = kit.boundsOf(anchor).height * 1.5f
            val up = whole.center.y > shown.center.y || whole.bottom > shown.bottom
            val to = Offset(from.x, if (up) from.y - distance else from.y + distance)
            kit.touch {
                down(from)
                for (i in 1..16) moveTo(Offset(from.x, from.y + (to.y - from.y) * i / 16))
                moveTo(to, delayMillis = 200) // held still before the lift, so the swipe ends without a fling
                up()
            }
            kit.idle()
            kit.assertPromise("$leg: the grid after a swipe", allowed)
        }
        throw AssertionError("kit: $leg: `$tag` is still not wholly in view after twelve swipes")
    }

    private fun sameBox(a: Rect, b: Rect): Boolean =
        abs(a.left - b.left) <= 1f && abs(a.top - b.top) <= 1f && abs(a.right - b.right) <= 1f && abs(a.bottom - b.bottom) <= 1f

    @Suppress("DEPRECATION")
    private fun packageInfo(pm: PackageManager, pkg: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        else pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
}
