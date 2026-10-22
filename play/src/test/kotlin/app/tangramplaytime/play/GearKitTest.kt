// Acceptance kit (WO-007, TASK-055; kit stage 2) — the gear's slot and its hit, pinned over `ScreenGeometry.of` and
// `HitTest.at` on the JVM (designs/WO-007-design.md §2 S-53). Pins, no token: this is the groundwork of REQ-032 A1 (the
// gear opens the settings screen: REQ-032 statement "WHEN the player presses the gear in the top bar") and of REQ-037 A1
// (the gear's 52 dp column); their tokens are the `app/` device kit's (design §2 "Tokens"). Prose names a line without
// the dot (traceability-rules.md §2: "A token is a coverage claim").
//
// - S-53: "Tag: `PlayTags.GEAR = "gear"`. Slot: `ScreenGeometry.gear` is a 52 dp column at the safe right edge, the
//   bar's full height. `navNext` now ends at `gear.left` (S-32Δ). Hit: `Control.GEAR` acts at lift within the slop, with
//   no long press, and calls `openSettings()`. It is inert under hit orders 0 and 1."
// - decisions.md 2026-10-21 "The gear and the settings screen, as drawn." (E2): "a fourth top-bar column at the safe right
//   edge, after ›"; "It is 52 dp wide and the bar's full height"; "› moves left by 52 dp; the counter keeps everything
//   between ‹ and ›"; "It is inert while a drag runs or the grid or the DEV sheet is open".
// - S-32 (tasks.md § WO-006 › Seams, unchanged): "The safe area is the composable minus `layout.insets`"; "The keyboard
//   moves nothing else: only `sheetContent` reads `keyboardDp`; the bar, the board, the tray and every control keep their
//   slots while the keyboard is open". S-22 (designs/WO-004-design.md §2 "Touch geometry"): the bar's columns are "full
//   height, with no gaps"; its hit order 2 is "a top-bar column".
// - What this file does not pin: the act at the lift, the long press, and hit orders 0 (the grid or the DEV sheet open) and 1
//   (a drag running). S-22 puts them in the router ("0. The overview is open: the router neither consumes nor acts, on the
//   top bar too. 1. A drag is running: the touch is a twist finger"), and `HitTest.at` is S-22's hit orders 2 and 3 (its
//   KDoc). They are pinned through touch on the device: play/src/androidTest/.../TopBarKitTest.kt (the lift, the hold, a
//   second finger during a drag, the grid open) and DevAidScreenKitTest.kt (the DEV sheet open).
// - The screens are SafeAreaKitTest.kt's, written here as literals (that file's cases are private to it): the phone at
//   390 × 844 dp (1170 × 2532 px at 3.0) with a status bar, a cutout and a navigation bar; the tablet at 1280 × 800 dp and
//   800 × 1280 dp (2560 × 1600 and 1600 × 2560 px at 2.0) with unequal side insets; each also with `PlayInsets.NONE`. The
//   layouts are the S-5 phone literal (KitPlay.kt `KIT_PHONE_LAYOUT`) and S-35's tablet literal. The puzzles are the
//   bundle's first full puzzle and first mini (KitPlay.kt `kitPuzzleOf`).
// - Tolerance (SafeAreaKitTest's header): `Slot` is whole px; an edge placed from one dp value may sit 1 px off, a width
//   from two 2 px. What S-53 states as an equality ("`navNext` now ends at `gear.left`") is exact.
// - Every assertion cites its source. Internal members are called directly. Nothing is skipped. Helper names carry a
//   `kit` / `Kit` / `KIT_` prefix and are members of this class.
package app.tangramplaytime.play

import androidx.compose.ui.geometry.Offset
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class GearKitTest {

    private companion object {
        /** S-35's tablet value (SafeAreaKitTest.kt's literal): rows `[[LT1, LT2, MT, SQ, PG, ST1, ST2]]`, float 30, gap 14, margin 16, bar 74. */
        val KIT_TABLET_LAYOUT = PlayLayout(
            trayRows = listOf(listOf(PieceId.LT1, PieceId.LT2, PieceId.MT, PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)),
            floatOffsetDp = 30f,
            cellGapDp = 14f,
            sideMarginDp = 16f,
            topBarDp = 74f,
        )

        /** SafeAreaKitTest.kt's insets: a portrait phone, a landscape tablet, a portrait tablet with unequal sides. */
        val KIT_PHONE_INSETS = PlayInsets(topDp = 52f, bottomDp = 48f, leftDp = 0f, rightDp = 0f)
        val KIT_TABLET_LANDSCAPE_INSETS = PlayInsets(topDp = 24f, bottomDp = 48f, leftDp = 32f, rightDp = 12f)
        val KIT_TABLET_PORTRAIT_INSETS = PlayInsets(topDp = 32f, bottomDp = 48f, leftDp = 24f, rightDp = 8f)

        /** An open soft keyboard (S-31 `keyboardDp`), as SafeAreaKitTest.kt opens it. */
        const val KIT_KEYBOARD_DP = 300f

        /** The gear: "a 52 dp column" (S-53); › stays the 60 dp column of S-22 / S-32, now ending at the gear's left (E2). */
        const val KIT_GEAR_COLUMN_DP = 52f
        const val KIT_NAV_COLUMN_DP = 60f

        /** REQ-015 rule's board touch margin, 12 dp (`HitTest.at`'s `marginPx`, as the screen passes it). */
        const val KIT_MARGIN_DP = 12f

        /** How far inside an edge a probe lies, as TopBarKitTest's taps do ("2 px inside each column's edges"). */
        const val KIT_INSIDE_PX = 2f

        const val KIT_EDGE_TOL_PX = 1f
        const val KIT_SUM_TOL_PX = 2f
    }

    /** One screen: a size in px, its density, the bare literal, its insets and a puzzle (SafeAreaKitTest.kt's `KitCase`). */
    private data class KitCase(
        val name: String,
        val widthPx: Int,
        val heightPx: Int,
        val density: Float,
        val bare: PlayLayout,
        val insets: PlayInsets,
        val puzzle: PuzzleDefinition,
    ) {
        fun geometry(insets: PlayInsets = this.insets, keyboardDp: Float = 0f): ScreenGeometry =
            ScreenGeometry.of(widthPx, heightPx, density, bare.copy(insets = insets, keyboardDp = keyboardDp), puzzle)
    }

    /** Collects every failed check of a test, then fails once with all of them (SafeAreaKitTest.kt's pattern). */
    private class KitChecks(private val test: String) {
        private val failures = ArrayList<String>()
        private var count = 0

        fun check(ok: Boolean, what: () -> String) {
            count++
            if (!ok) failures += what()
        }

        fun near(what: String, got: Number, expected: Number, tol: Float) {
            count++
            val g = got.toFloat()
            val e = expected.toFloat()
            if (abs(g - e) > tol) failures += "$what: $g, expected $e (± $tol)"
        }

        fun same(what: String, a: Any?, b: Any?) {
            count++
            if (a != b) failures += "$what: $a, expected $b"
        }

        fun done() {
            if (failures.isNotEmpty()) {
                throw AssertionError("$test: ${failures.size} of $count checks failed:\n" + failures.take(80).joinToString("\n"))
            }
        }
    }

    private val kitFull: PuzzleDefinition by lazy { kitPuzzleOf(PuzzleKind.FULL) }
    private val kitMini: PuzzleDefinition by lazy { kitPuzzleOf(PuzzleKind.MINI) }

    /** Every screen × both puzzles, each with its insets. */
    private fun kitCases(): List<KitCase> = listOf(kitFull, kitMini).flatMap { p ->
        listOf(
            KitCase("phone 390 × 844 dp", 1170, 2532, 3f, KIT_PHONE_LAYOUT, KIT_PHONE_INSETS, p),
            KitCase("tablet 1280 × 800 dp", 2560, 1600, 2f, KIT_TABLET_LAYOUT, KIT_TABLET_LANDSCAPE_INSETS, p),
            KitCase("tablet 800 × 1280 dp", 1600, 2560, 2f, KIT_TABLET_LAYOUT, KIT_TABLET_PORTRAIT_INSETS, p),
        )
    }

    private fun kitRight(s: Slot): Int = s.left + s.width
    private fun kitBottom(s: Slot): Int = s.top + s.height
    private fun kitMidX(s: Slot): Float = s.left + s.width / 2f
    private fun kitMidY(s: Slot): Float = s.top + s.height / 2f

    // Pin (no token): S-53 "Tag: `PlayTags.GEAR = "gear"`"; S-13Δ (designs/WO-004-design.md §2: the seam fixes the tag
    // strings, which the device kits write as literals). The gear's tag is "gear" and no other top-bar control has it.
    @Test
    fun theGearsTagIsGear() {
        assertEquals("S-53: the gear's tag", "gear", PlayTags.GEAR)
        val others = listOf(PlayTags.NAV_PREV, PlayTags.NAV_NEXT, PlayTags.COUNTER, PlayTags.TITLE, PlayTags.RATING, PlayTags.STATE_WORD)
        assertEquals("the gear's tag is no other top-bar tag", emptyList<String>(), others.filter { it == PlayTags.GEAR })
    }

    // Pin (no token): S-53 "Slot: `ScreenGeometry.gear` is a 52 dp column at the safe right edge, the bar's full height.
    // `navNext` now ends at `gear.left`"; E2 ("a fourth top-bar column at the safe right edge, after ›"; "› moves left by
    // 52 dp; the counter keeps everything between ‹ and ›"); S-32 ("The safe area is the composable minus `layout.insets`";
    // every player control lies inside it). On the phone and both tablet orientations, both puzzles, each with its insets and
    // with none:
    // - the gear ends at the safe right edge, is 52 dp wide, and spans the bar's full height;
    // - › ends exactly at the gear's left and is still 60 dp wide, so it starts 112 dp from the safe right edge;
    // - the counter still ends where › starts;
    // - the gear lies inside the safe area.
    // Discriminating: the right insets of both tablets put the screen's right edge 8 and 12 dp past the safe right, so a gear
    // placed from the composable's edge fails; a › left where it was (ending at the safe right) overlaps the gear and fails.
    @Test
    fun theGearIsA52DpFullHeightColumnAtTheSafeRightEdgeAndNextEndsAtItsLeft() {
        val ch = KitChecks("theGearIsA52DpFullHeightColumnAtTheSafeRightEdgeAndNextEndsAtItsLeft")
        for (c in kitCases()) {
            for (insets in listOf(c.insets, PlayInsets.NONE)) {
                val tag = "${c.name}, ${c.puzzle.id}, insets $insets"
                val g = c.geometry(insets = insets)
                val d = c.density
                val safeRight = c.widthPx - insets.rightDp * d
                ch.near("$tag: the gear's right is the safe right", kitRight(g.gear), safeRight, KIT_EDGE_TOL_PX)
                ch.near("$tag: the gear is 52 dp wide", g.gear.width, KIT_GEAR_COLUMN_DP * d, KIT_SUM_TOL_PX)
                ch.same("$tag: the gear's top is the bar's", g.gear.top, g.topBar.top)
                ch.same("$tag: the gear's height is the bar's", g.gear.height, g.topBar.height)
                ch.same("$tag: › ends at the gear's left", kitRight(g.navNext), g.gear.left)
                ch.near("$tag: › is 60 dp wide", g.navNext.width, KIT_NAV_COLUMN_DP * d, KIT_SUM_TOL_PX)
                ch.near("$tag: › starts 112 dp from the safe right", g.navNext.left, safeRight - (KIT_GEAR_COLUMN_DP + KIT_NAV_COLUMN_DP) * d, KIT_SUM_TOL_PX + KIT_EDGE_TOL_PX)
                ch.same("$tag: the counter ends where › starts", kitRight(g.counter), g.navNext.left)
                ch.check(
                    g.gear.left >= g.safe.left - KIT_EDGE_TOL_PX && g.gear.top >= g.safe.top - KIT_EDGE_TOL_PX &&
                        kitRight(g.gear) <= kitRight(g.safe) + KIT_EDGE_TOL_PX && kitBottom(g.gear) <= kitBottom(g.safe) + KIT_EDGE_TOL_PX,
                ) { "$tag: the gear ${g.gear} does not lie inside the safe area ${g.safe}" }
            }
        }
        ch.done()
    }

    // Pin (no token): S-32 "The keyboard moves nothing else: … the bar, the board, the tray and every control keep their slots
    // while the keyboard is open"; the gear is a control of the bar (S-53). With a keyboard of 0 and of 300 dp, each screen's
    // insets and none: the gear's slot is the same, exactly. Discriminating: `sheetContent` differs, so the keyboard reached
    // the geometry (as SafeAreaKitTest.kt asserts).
    @Test
    fun theKeyboardDoesNotMoveTheGear() {
        val ch = KitChecks("theKeyboardDoesNotMoveTheGear")
        for (c in kitCases()) {
            for (insets in listOf(c.insets, PlayInsets.NONE)) {
                val tag = "${c.name}, ${c.puzzle.id}, insets $insets"
                val closed = c.geometry(insets = insets, keyboardDp = 0f)
                val open = c.geometry(insets = insets, keyboardDp = KIT_KEYBOARD_DP)
                ch.same("$tag: the gear with the keyboard open", open.gear, closed.gear)
                ch.check(open.sheetContent != closed.sheetContent) { "$tag: the 300 dp keyboard never reached the geometry (sheetContent ${open.sheetContent})" }
            }
        }
        ch.done()
    }

    // Pin (no token): S-53 "Hit: `Control.GEAR`"; S-22 "Touch geometry" (the bar's columns are "full height, with no gaps";
    // hit order 2, "a top-bar column", comes first) and its hit order as `HitTest.at` runs it (orders 2 and 3, its KDoc);
    // S-13Δ "A control's bounds are its touch area". On every screen, with its insets and with none, over a controller showing
    // the case's puzzle:
    // - a point 2 px inside each edge of the gear's slot, at its middle and at two opposite corners, hits `Control.GEAR`;
    // - the neighbour column: 2 px left of the gear's left is › (`Control.NEXT`), and the counter's middle is the counter;
    // - with a right inset, a point 2 px right of the gear's right edge (in the inset, outside the slot) is not the gear.
    @Test
    fun aTouchInsideTheGearsColumnHitsTheGearAndItsNeighboursDoNot() {
        val ch = KitChecks("aTouchInsideTheGearsColumnHitsTheGearAndItsNeighboursDoNot")
        for (c in kitCases()) {
            for (insets in listOf(c.insets, PlayInsets.NONE)) {
                val tag = "${c.name}, ${c.puzzle.id}, insets $insets"
                val g = c.geometry(insets = insets)
                val controller = kitController()
                kitOpen(controller, c.puzzle.id)
                val margin = KIT_MARGIN_DP * c.density
                fun at(o: Offset): Hit? = HitTest.at(o, g, controller, margin, ShownControls.NONE)
                val gear = g.gear
                val inside = listOf(
                    "2 px inside the left edge" to Offset(gear.left + KIT_INSIDE_PX, kitMidY(gear)),
                    "2 px inside the right edge" to Offset(kitRight(gear) - KIT_INSIDE_PX, kitMidY(gear)),
                    "2 px inside the top edge" to Offset(kitMidX(gear), gear.top + KIT_INSIDE_PX),
                    "2 px inside the bottom edge" to Offset(kitMidX(gear), kitBottom(gear) - KIT_INSIDE_PX),
                    "2 px inside the top-left corner" to Offset(gear.left + KIT_INSIDE_PX, gear.top + KIT_INSIDE_PX),
                    "2 px inside the bottom-right corner" to Offset(kitRight(gear) - KIT_INSIDE_PX, kitBottom(gear) - KIT_INSIDE_PX),
                    "the middle" to Offset(kitMidX(gear), kitMidY(gear)),
                )
                for ((what, o) in inside) ch.same("$tag: $what of the gear $gear ($o)", at(o), Hit.OnControl(Control.GEAR))
                val leftOfGear = Offset(gear.left - KIT_INSIDE_PX, kitMidY(gear))
                ch.same("$tag: 2 px left of the gear ($leftOfGear) is ›", at(leftOfGear), Hit.OnControl(Control.NEXT))
                ch.same("$tag: the counter's middle is the counter", at(Offset(kitMidX(g.counter), kitMidY(g.counter))), Hit.OnControl(Control.COUNTER))
                if (insets.rightDp * c.density >= 2 * KIT_INSIDE_PX) {
                    val inInset = Offset(kitRight(gear) + KIT_INSIDE_PX, kitMidY(gear))
                    ch.check(at(inInset) != Hit.OnControl(Control.GEAR)) { "$tag: $inInset, in the right inset outside the gear's slot $gear, hits the gear" }
                }
            }
        }
        ch.done()
    }
}
