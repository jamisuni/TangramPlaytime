// Acceptance kit (WO-006, TASK-027) — the safe-area geometry of the Play screen, pinned over `ScreenGeometry.of`
// (tasks.md § WO-006 › Seams S-31, S-32, S-33; the WO is Light, the seam table is its design). Pins, no acceptance
// token: this is the groundwork of REQ-035 A2, REQ-036 A1 and REQ-037 A1 (the slots inside the safe area, the
// tablet row), which TASK-030's device kit proves through the real app (the TASK-027 row: "groundwork, no token";
// traceability-rules.md §2: "A token is a coverage claim").
//
// - S-31: `PlayInsets(topDp, bottomDp, leftDp, rightDp)` with `NONE`; `PlayLayout` gains `insets` ("the system bars
//   and the display cutout, dp per side — never the keyboard") and `keyboardDp` ("the soft keyboard's bottom inset
//   in dp, 0 while it is closed"), both defaulted.
// - S-32: "The safe area is the composable minus `layout.insets`", and every slot is placed from it; the new fields
//   `safe`, `overlay`, `overlayContent`, `sheetContent`; "The keyboard moves nothing else"; "With `PlayInsets.NONE`
//   and no keyboard every slot equals WO-005's" (seam note 1).
// - S-33: the overview's and the DEV sheet's `content` is `overlayContent` / `sheetContent`; "With `PlayInsets.NONE`
//   and no keyboard, `content` = `area`".
// - Literals: the S-5 phone value `KIT_PHONE_LAYOUT` (KitPlay.kt, reused) and S-35's tablet value, written here as
//   a literal as the kits write the phone's (S-35: rows `[[LT1, LT2, MT, SQ, PG, ST1, ST2]]`, float offset 30, cell
//   gap 14, side margin 16, top bar 74; decisions.md 2026-10-17 "The tablet layout, as drawn"). Each is given
//   non-zero insets — a portrait phone (status bar and cutout on top, a navigation bar below), a landscape tablet
//   with a cutout on the left and a bar on the right too, a portrait tablet with unequal sides — and a keyboard.
// - Sizes in px with their densities (the brief): the phone at 390 × 844 dp (1170 × 2532 px at 3.0, REQ-035's phone),
//   the tablet at 1280 × 800 dp and 800 × 1280 dp (2560 × 1600 and 1600 × 2560 px at 2.0, REQ-036's tablets).
//   Puzzles from the bundle through KitPlay.kt: the first full puzzle (all seven pieces, the parallelogram among
//   them) and the first mini (fewer pieces, so the phone tray drops a row and the board grows).
// - Fixtures discriminate (the brief's oracle rule): every inset is large enough that the WO-005 geometry — the same
//   size with `PlayInsets.NONE`, which seam note 1 says is WO-005's screen — fails the inside-the-safe-area pins; that
//   is asserted test-side first, so a pin that would pass on the old screen is a fixture defect, reported as one.
// - Tolerance: `Slot` is whole px of `play-screen` (PlayDrawing.kt); no row fixes how a dp edge rounds, so an edge
//   placed from one dp value may sit 1 px off, and one placed from two (a cell: the tray top plus its tray-local y)
//   2 px. Equalities the seams state as equalities (the keyboard moves nothing else; `content` = `area`) are exact.
// - Every assertion cites its S-row sentence; geometry the seams do not fix (the board mapping's fit, the strip's
//   width, the badges, which are not `ScreenGeometry` fields) is not asserted. Internal members are called directly.
//   Nothing is skipped. Helper names carry a `kit` / `Kit` / `KIT_` prefix and are members of this class.
// - Corrected WO-007 (TASK-055; three corrected checks, decisions.md 2026-10-21 "The moved-› pins in `play/` land in the same
//   step as S-53, as declared outputs, each a corrected check."): S-53 (designs/WO-007-design.md §2) adds the gear, "a 52 dp
//   column at the safe right edge, the bar's full height", and "`navNext` now ends at `gear.left`". So `kitControls` lists
//   the gear; the fixture's right-side discriminator reads the NONE layout's rightmost column, now the gear; and the column
//   pins put › at the gear's left and the gear at the safe right edge. The counter still spans ‹ to › (decisions.md
//   2026-10-21 "The gear and the settings screen, as drawn.": "the counter keeps everything between ‹ and ›").
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SafeAreaKitTest {

    private companion object {
        /** S-35's tablet value: "rows `[[LT1, LT2, MT, SQ, PG, ST1, ST2]]`, float offset 30, cell gap 14, side margin 16, top bar 74". */
        val KIT_TABLET_LAYOUT = PlayLayout(
            trayRows = listOf(listOf(PieceId.LT1, PieceId.LT2, PieceId.MT, PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)),
            floatOffsetDp = 30f,
            cellGapDp = 14f,
            sideMarginDp = 16f,
            topBarDp = 74f,
        )

        /** A portrait phone: the status bar with its cutout on top, a three-button navigation bar below (S-31: "the system bars and the display cutout, dp per side"). */
        val KIT_PHONE_INSETS = PlayInsets(topDp = 52f, bottomDp = 48f, leftDp = 0f, rightDp = 0f)

        /** A landscape tablet: the status bar on top, the bar below, a cutout on the left and a bar on the right — unequal sides. */
        val KIT_TABLET_LANDSCAPE_INSETS = PlayInsets(topDp = 24f, bottomDp = 48f, leftDp = 32f, rightDp = 12f)

        /** A portrait tablet with unequal side insets, so the safe width differs from the screen's on a width-bound tray. */
        val KIT_TABLET_PORTRAIT_INSETS = PlayInsets(topDp = 32f, bottomDp = 48f, leftDp = 24f, rightDp = 8f)

        /** An open soft keyboard, taller than every bottom inset above (S-31 `keyboardDp`). */
        const val KIT_KEYBOARD_DP = 300f

        /** A keyboard inset smaller than every bottom inset above: S-32's "raised to at most" leaves the content where it is. */
        const val KIT_SMALL_KEYBOARD_DP = 20f

        /** ‹ and › are columns 60 dp wide (S-32, from S-22). */
        const val KIT_NAV_COLUMN_DP = 60f

        /** The gear is "a 52 dp column at the safe right edge, the bar's full height" (S-53, WO-007). */
        const val KIT_GEAR_COLUMN_DP = 52f

        /** The board starts at "the bar's bottom + 4 dp" and ends at "the tray's top − 8 dp" (S-32). */
        const val KIT_BOARD_TOP_GAP_DP = 4f
        const val KIT_BOARD_BOTTOM_GAP_DP = 8f

        /** An edge placed from one dp value (see the header). */
        const val KIT_EDGE_TOL_PX = 1f

        /** An edge placed from two dp values (see the header). */
        const val KIT_SUM_TOL_PX = 2f
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────────────────────────

    /**
     * One screen: a size in px, its density, the bare literal ([bare], written without the new fields), its insets,
     * and a puzzle. [layout] is the literal with those insets and the keyboard closed.
     */
    private data class KitCase(
        val name: String,
        val widthPx: Int,
        val heightPx: Int,
        val density: Float,
        val bare: PlayLayout,
        val insets: PlayInsets,
        val puzzle: PuzzleDefinition,
    ) {
        val layout: PlayLayout get() = bare.copy(insets = insets, keyboardDp = 0f)
        val tag: String get() = "$name, ${puzzle.id}"

        /** `ScreenGeometry.of` for this screen, the literal given [insets] (default: this screen's) and [keyboardDp] (default: closed). */
        fun geometry(insets: PlayInsets = this.insets, keyboardDp: Float = 0f): ScreenGeometry =
            ScreenGeometry.of(widthPx, heightPx, density, bare.copy(insets = insets, keyboardDp = keyboardDp), puzzle)
    }

    /** The safe area of [c] with [insets], in px as floats: "the composable minus `layout.insets`" (S-32). */
    private class KitSafe(c: KitCase, insets: PlayInsets) {
        val left = insets.leftDp * c.density
        val top = insets.topDp * c.density
        val right = c.widthPx - insets.rightDp * c.density
        val bottom = c.heightPx - insets.bottomDp * c.density
        val widthDp = c.widthPx / c.density - insets.leftDp - insets.rightDp
        val screenHeightDp = c.heightPx / c.density
    }

    /** Collects every failed check of a test, then fails once with all of them. */
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

    private val kitFull: PuzzleDefinition by lazy {
        kitPuzzleOf(PuzzleKind.FULL).also {
            if (it.pieces != PieceId.entries.toSet()) throw AssertionError("fixture defect: the full puzzle ${it.id} holds ${it.pieces}, not all seven pieces")
        }
    }

    private val kitMini: PuzzleDefinition by lazy {
        kitPuzzleOf(PuzzleKind.MINI).also {
            if (it.pieces.size >= PieceId.entries.size) throw AssertionError("fixture defect: the mini ${it.id} holds all seven pieces")
        }
    }

    private fun kitPhone(p: PuzzleDefinition) =
        KitCase("phone 390 × 844 dp", 1170, 2532, 3f, KIT_PHONE_LAYOUT, KIT_PHONE_INSETS, p)

    private fun kitTabletLandscape(p: PuzzleDefinition) =
        KitCase("tablet 1280 × 800 dp", 2560, 1600, 2f, KIT_TABLET_LAYOUT, KIT_TABLET_LANDSCAPE_INSETS, p)

    private fun kitTabletPortrait(p: PuzzleDefinition) =
        KitCase("tablet 800 × 1280 dp", 1600, 2560, 2f, KIT_TABLET_LAYOUT, KIT_TABLET_PORTRAIT_INSETS, p)

    /** Every screen × both puzzles, each with its non-zero insets. */
    private fun kitCases(): List<KitCase> = listOf(kitFull, kitMini).flatMap { p -> listOf(kitPhone(p), kitTabletLandscape(p), kitTabletPortrait(p)) }

    private fun kitRight(s: Slot): Int = s.left + s.width
    private fun kitBottom(s: Slot): Int = s.top + s.height

    /**
     * The player controls and the bar and board slots of [g], by name: ‹, ›, the gear, the counter, each cell, each third
     * (S-32; the gear is S-53's, WO-007: a corrected check).
     */
    private fun kitControls(g: ScreenGeometry): List<Pair<String, Slot>> =
        listOf("topBar" to g.topBar, "navPrev" to g.navPrev, "navNext" to g.navNext, "gear" to g.gear, "counter" to g.counter, "board" to g.board) +
            g.cells.map { (id, s) -> "cell $id" to s } +
            g.solvedThirds.mapIndexed { i, s -> "third $i" to s }

    /** S-32: "lie inside the safe area" — every edge of [s] within [safe], to [KIT_EDGE_TOL_PX]. */
    private fun kitInside(ch: KitChecks, what: String, s: Slot, safe: Slot) {
        ch.check(
            s.left >= safe.left - KIT_EDGE_TOL_PX && s.top >= safe.top - KIT_EDGE_TOL_PX &&
                kitRight(s) <= kitRight(safe) + KIT_EDGE_TOL_PX && kitBottom(s) <= kitBottom(safe) + KIT_EDGE_TOL_PX,
        ) { "$what $s does not lie inside the safe area $safe" }
    }

    /**
     * The brief's "fixtures discriminate": with [c]'s insets, the WO-005 geometry (the same screen with
     * `PlayInsets.NONE` and no keyboard, which is WO-005's by seam note 1) fails the inside-the-safe-area pins —
     * its bar starts above the safe top, its lowest tray cell ends below the safe bottom, and with a side inset its ‹
     * column or its rightmost column reaches past that side. The rightmost column is the gear since WO-007 (S-53: "a 52 dp
     * column at the safe right edge"; › now ends at its left, so › no longer reaches the right edge: a corrected check).
     * Otherwise the fixture cannot tell inside from not: a fixture defect.
     */
    private fun kitAssertFixtureDiscriminates(c: KitCase) {
        val old = c.geometry(insets = PlayInsets.NONE, keyboardDp = 0f)
        val safe = KitSafe(c, c.insets)
        val defects = ArrayList<String>()
        if (old.topBar.top >= safe.top - KIT_EDGE_TOL_PX) defects += "the NONE bar starts at ${old.topBar.top} px, not above the safe top ${safe.top}"
        val lowest = old.cells.values.maxOf { kitBottom(it) }
        if (lowest <= safe.bottom + KIT_EDGE_TOL_PX) defects += "the NONE cells end at $lowest px, not below the safe bottom ${safe.bottom}"
        if (c.insets.leftDp > 0f && old.navPrev.left >= safe.left - KIT_EDGE_TOL_PX) defects += "the NONE ‹ column starts at ${old.navPrev.left} px, not left of the safe left ${safe.left}"
        if (c.insets.rightDp > 0f && kitRight(old.gear) <= safe.right + KIT_EDGE_TOL_PX) defects += "the NONE gear column ends at ${kitRight(old.gear)} px, not right of the safe right ${safe.right}"
        if (defects.isNotEmpty()) throw AssertionError("fixture defect (${c.tag}, insets ${c.insets}): ${defects.joinToString("; ")}")
    }

    // ── Pins ─────────────────────────────────────────────────────────────────────────────────────

    // S-32: "The safe area is the composable minus `layout.insets`" (`safe: Slot`), and "Top bar: `topBarDp` tall from
    // the safe top"; "Every cell, both badges and the solved row's thirds lie inside the safe area"; ‹ is the column
    // "from the safe left edge", the gear "a 52 dp column at the safe right edge" with › ending at its left (S-53, WO-007;
    // a corrected check), and the counter is "everything between" ‹ and ›, so every player control of the bar lies inside
    // it too; the board lies "`sideMarginDp` inside the safe left and right
    // edges" between the bar and the tray. On the phone, both tablet orientations and both puzzles, each with its
    // non-zero insets — insets the WO-005 geometry fails (asserted first). The badges are not `ScreenGeometry` fields
    // (the badge area sits inside the PG cell, decisions.md 2026-10-12 "Inside the parallelogram's tray cell the
    // visible thing wins, as on the board"); the device kit reads them as nodes.
    @Test
    fun theSafeAreaIsTheScreenMinusTheInsetsAndEveryControlLiesInsideIt() {
        val ch = KitChecks("theSafeAreaIsTheScreenMinusTheInsetsAndEveryControlLiesInsideIt")
        for (c in kitCases()) {
            kitAssertFixtureDiscriminates(c)
            val g = c.geometry()
            val e = KitSafe(c, c.insets)
            ch.near("${c.tag}: safe left", g.safe.left, e.left, KIT_EDGE_TOL_PX)
            ch.near("${c.tag}: safe top", g.safe.top, e.top, KIT_EDGE_TOL_PX)
            ch.near("${c.tag}: safe right", kitRight(g.safe), e.right, KIT_EDGE_TOL_PX)
            ch.near("${c.tag}: safe bottom", kitBottom(g.safe), e.bottom, KIT_EDGE_TOL_PX)
            ch.near("${c.tag}: the bar starts at the safe top", g.topBar.top, g.safe.top, KIT_EDGE_TOL_PX)
            for ((what, s) in kitControls(g)) kitInside(ch, "${c.tag}: $what", s, g.safe)
        }
        ch.done()
    }

    // S-32: "The tray's top is the safe bottom − the tray height. The `tray` strip is drawn from that top to the
    // composable's bottom edge, behind the gesture bar. Every cell … lie[s] inside the safe area." The strip reaches the
    // composable's bottom edge while every cell ends at or above the safe bottom (Q3's row, decisions.md 2026-10-17
    // "Edge to edge, the insets and the keyboard, as drawn": "the tray band runs under the gesture bar while every tray
    // cell and control stays inside the safe area"). Bottom insets the WO-005 cells would cross (asserted first).
    @Test
    fun theTrayStripReachesTheBottomEdgeWhileEveryCellEndsAtOrAboveTheSafeBottom() {
        val ch = KitChecks("theTrayStripReachesTheBottomEdgeWhileEveryCellEndsAtOrAboveTheSafeBottom")
        for (c in kitCases()) {
            kitAssertFixtureDiscriminates(c)
            val g = c.geometry()
            ch.near("${c.tag}: the tray strip's bottom is the composable's bottom edge", kitBottom(g.tray), c.heightPx, KIT_EDGE_TOL_PX)
            ch.check(kitBottom(g.tray) > kitBottom(g.safe)) { "${c.tag}: the tray strip ends at ${kitBottom(g.tray)} px, not below the safe bottom ${kitBottom(g.safe)} (behind the bar)" }
            for ((id, s) in g.cells) {
                ch.check(kitBottom(s) <= kitBottom(g.safe) + KIT_EDGE_TOL_PX) { "${c.tag}: cell $id ends at ${kitBottom(s)} px, below the safe bottom ${kitBottom(g.safe)}" }
            }
        }
        ch.done()
    }

    // S-32, sentence by sentence, with each screen's insets and with `PlayInsets.NONE` (seam note 1: with no insets
    // the same sentences give WO-005's screen):
    // - "Top bar: `topBarDp` tall from the safe top, across the safe width."
    // - "‹ is the column from the safe left edge, 60 dp wide; › the column ending at the safe right edge; both the bar's
    //   full height (S-22's columns, moved inside the insets). The counter is everything between."
    //   S-53 (WO-007; a corrected check): "`ScreenGeometry.gear` is a 52 dp column at the safe right edge, the bar's full
    //   height. `navNext` now ends at `gear.left`" — so › is 60 dp ending at the gear's left, the gear ends at the safe right
    //   edge, and the counter still spans ‹ to › (decisions.md 2026-10-21 "The gear and the settings screen, as drawn.").
    // - "Tray: `TrayMetrics.layout(the safe width, the composable's full height, layout, pieces)` (S-6 unchanged;
    //   REQ-013's 'screen height' is the whole screen), cells offset by the safe left. The tray's top is the safe
    //   bottom − the tray height." S-6: cells are tray-local dp and hold exactly `pieces`; `trayPxPerUnit` is "the
    //   miniatures' common scale, px per unit" (ScreenGeometry's declaration), the S-6 scale in px.
    // - "Board: from the bar's bottom + 4 dp to the tray's top − 8 dp, `sideMarginDp` inside the safe left and right
    //   edges."
    // - "the thirds split the band between the tray's top and the safe bottom, across the safe width" (S-22: Retry,
    //   the best time, Next, left to right).
    // Discriminating: TrayMetrics given the safe height instead of the full height changes the scale on the phone and
    // the landscape tablet (their row-height limit, REQ-013 rule 2, is 16 % of the height); given the full width
    // instead of the safe width it changes the scale on the portrait tablet and the cells' x everywhere a side inset is.
    @Test
    fun theBarItsColumnsTheBoardTheTrayAndTheThirdsSitWhereS32PutsThem() {
        val ch = KitChecks("theBarItsColumnsTheBoardTheTrayAndTheThirdsSitWhereS32PutsThem")
        for (c in kitCases()) {
            for (insets in listOf(c.insets, PlayInsets.NONE)) {
                val tag = "${c.tag}, insets $insets"
                val g = c.geometry(insets = insets, keyboardDp = 0f)
                val e = KitSafe(c, insets)
                val d = c.density

                ch.near("$tag: bar top", g.topBar.top, e.top, KIT_EDGE_TOL_PX)
                ch.near("$tag: bar height", g.topBar.height, c.layout.topBarDp * d, KIT_EDGE_TOL_PX)
                ch.near("$tag: bar left", g.topBar.left, e.left, KIT_EDGE_TOL_PX)
                ch.near("$tag: bar right", kitRight(g.topBar), e.right, KIT_EDGE_TOL_PX)

                ch.near("$tag: ‹ left", g.navPrev.left, e.left, KIT_EDGE_TOL_PX)
                ch.near("$tag: ‹ width", g.navPrev.width, KIT_NAV_COLUMN_DP * d, KIT_SUM_TOL_PX)
                ch.same("$tag: ‹ top", g.navPrev.top, g.topBar.top)
                ch.same("$tag: ‹ height", g.navPrev.height, g.topBar.height)
                ch.same("$tag: › ends at the gear's left (S-53)", kitRight(g.navNext), g.gear.left)
                ch.near("$tag: › width", g.navNext.width, KIT_NAV_COLUMN_DP * d, KIT_SUM_TOL_PX)
                ch.same("$tag: › top", g.navNext.top, g.topBar.top)
                ch.same("$tag: › height", g.navNext.height, g.topBar.height)
                ch.near("$tag: the gear's right is the safe right (S-53)", kitRight(g.gear), e.right, KIT_EDGE_TOL_PX)
                ch.near("$tag: the gear's width (S-53: 52 dp)", g.gear.width, KIT_GEAR_COLUMN_DP * d, KIT_SUM_TOL_PX)
                ch.same("$tag: the gear's top (S-53: the bar's full height)", g.gear.top, g.topBar.top)
                ch.same("$tag: the gear's height (S-53: the bar's full height)", g.gear.height, g.topBar.height)
                ch.same("$tag: counter left", g.counter.left, kitRight(g.navPrev))
                ch.same("$tag: counter right", kitRight(g.counter), g.navNext.left)
                ch.same("$tag: counter top", g.counter.top, g.topBar.top)
                ch.same("$tag: counter height", g.counter.height, g.topBar.height)

                val tray = TrayMetrics.layout(e.widthDp, e.screenHeightDp, c.layout, c.puzzle.pieces)
                val trayTop = e.bottom - tray.heightDp * d
                ch.same("$tag: the cells", g.cells.keys, c.puzzle.pieces)
                for ((id, cell) in tray.cells) {
                    val s = g.cells[id] ?: continue
                    ch.near("$tag: cell $id left", s.left, e.left + cell.xDp * d, KIT_SUM_TOL_PX)
                    ch.near("$tag: cell $id top", s.top, trayTop + cell.yDp * d, KIT_SUM_TOL_PX)
                    ch.near("$tag: cell $id width", s.width, cell.widthDp * d, KIT_SUM_TOL_PX)
                    ch.near("$tag: cell $id height", s.height, cell.heightDp * d, KIT_SUM_TOL_PX)
                }
                ch.near("$tag: trayPxPerUnit", g.trayPxPerUnit, tray.scaleDpPerUnit * d, 0.01f)
                ch.near("$tag: the tray strip's top", g.tray.top, trayTop, KIT_SUM_TOL_PX)

                ch.near("$tag: board top", g.board.top, kitBottom(g.topBar) + KIT_BOARD_TOP_GAP_DP * d, KIT_SUM_TOL_PX)
                ch.near("$tag: board bottom", kitBottom(g.board), trayTop - KIT_BOARD_BOTTOM_GAP_DP * d, KIT_SUM_TOL_PX)
                ch.near("$tag: board left", g.board.left, e.left + c.layout.sideMarginDp * d, KIT_SUM_TOL_PX)
                ch.near("$tag: board right", kitRight(g.board), e.right - c.layout.sideMarginDp * d, KIT_SUM_TOL_PX)

                ch.same("$tag: the solved row's thirds", g.solvedThirds.size, 3)
                val third = (e.right - e.left) / 3f
                for ((i, s) in g.solvedThirds.withIndex()) {
                    ch.near("$tag: third $i left", s.left, e.left + i * third, KIT_SUM_TOL_PX)
                    ch.near("$tag: third $i right", kitRight(s), e.left + (i + 1) * third, KIT_SUM_TOL_PX)
                    ch.near("$tag: third $i top", s.top, trayTop, KIT_SUM_TOL_PX)
                    ch.near("$tag: third $i bottom", kitBottom(s), e.bottom, KIT_EDGE_TOL_PX)
                }
            }
        }
        ch.done()
    }

    // REQ-035 rule 1: "Tray row 1 holds the large and medium triangles; row 2 holds the square, parallelogram and small
    // triangles", the rows "from top to bottom" (REQ-035 statement; TYPE-001 "Tray order, left to right and top to
    // bottom: LT1, LT2, MT, SQ, PG, ST1, ST2") — the S-5 phone literal's rows. REQ-036 statement and A1: on a tablet
    // "the tray as a single row" — S-35's tablet literal — in both orientations. A row is cells that one horizontal
    // line crosses (cells in a row are vertically centred and differ in height: decisions.md 2026-10-11 "Numbers the
    // tray and the gestures take from the design inputs" (3), so a shared `top` is not the test); row 1 lies wholly
    // above row 2; within a row the cells run left to right in TYPE-001 order. The full puzzle (all seven pieces),
    // with each screen's insets and with none. The phone's two rows are not one row, and the tablet's one row is.
    @Test
    fun thePhoneTrayHasTwoRowsAndTheTabletTrayOneRowInBothOrientations() {
        val ch = KitChecks("thePhoneTrayHasTwoRowsAndTheTabletTrayOneRowInBothOrientations")
        val row1 = listOf(PieceId.LT1, PieceId.LT2, PieceId.MT)
        val row2 = listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)
        val all = row1 + row2
        val phone = kitPhone(kitFull)
        for (insets in listOf(phone.insets, PlayInsets.NONE)) {
            val tag = "${phone.tag}, insets $insets"
            val cells = phone.geometry(insets = insets, keyboardDp = 0f).cells
            val r1 = row1.map { cells.getValue(it) }
            val r2 = row2.map { cells.getValue(it) }
            ch.check(kitOneLineCrosses(r1)) { "$tag: no horizontal line crosses row 1 $row1: $r1" }
            ch.check(kitOneLineCrosses(r2)) { "$tag: no horizontal line crosses row 2 $row2: $r2" }
            ch.check(r1.all { a -> r2.all { b -> kitBottom(a) <= b.top + KIT_EDGE_TOL_PX } }) { "$tag: row 1 $r1 does not lie above row 2 $r2" }
            ch.check(!kitOneLineCrosses(all.map { cells.getValue(it) })) { "$tag: one line crosses all seven cells — one row, not REQ-035's two" }
            kitLeftToRight(ch, "$tag: row 1", row1, r1)
            kitLeftToRight(ch, "$tag: row 2", row2, r2)
        }
        for (tablet in listOf(kitTabletLandscape(kitFull), kitTabletPortrait(kitFull))) {
            for (insets in listOf(tablet.insets, PlayInsets.NONE)) {
                val tag = "${tablet.tag}, insets $insets"
                val cells = tablet.geometry(insets = insets, keyboardDp = 0f).cells
                val row = all.map { cells.getValue(it) }
                ch.check(kitOneLineCrosses(row)) { "$tag: no horizontal line crosses all seven cells (REQ-036: one row): $row" }
                kitLeftToRight(ch, "$tag: the row", all, row)
            }
        }
        ch.done()
    }

    // S-32's new fields:
    // - "`overlay: Slot` (below the top bar to the composable's edges)";
    // - "`overlayContent: Slot` (`overlay` ∩ `safe`)";
    // - "`sheetContent: Slot` (`overlayContent`, its bottom raised to at most the composable's bottom − `keyboardDp`)".
    // With the keyboard closed (0), with a keyboard smaller than the bottom inset (20 dp: "at most" leaves the content
    // where it is) and with one taller (300 dp: the bottom rises to the composable's bottom − 300 dp). Discriminating:
    // every screen has a bottom inset, so `overlayContent` ends above `overlay`; the 300 dp keyboard raises
    // `sheetContent` above `overlayContent`.
    @Test
    fun theOverlayAndItsTwoContentAreasAreWhatS32Defines() {
        val ch = KitChecks("theOverlayAndItsTwoContentAreasAreWhatS32Defines")
        for (c in kitCases()) {
            for (k in listOf(0f, KIT_SMALL_KEYBOARD_DP, KIT_KEYBOARD_DP)) {
                val tag = "${c.tag}, keyboard $k dp"
                val g = c.geometry(keyboardDp = k)
                ch.near("$tag: overlay left (the composable's edge)", g.overlay.left, 0, KIT_EDGE_TOL_PX)
                ch.near("$tag: overlay top (below the top bar)", g.overlay.top, kitBottom(g.topBar), KIT_EDGE_TOL_PX)
                ch.near("$tag: overlay right (the composable's edge)", kitRight(g.overlay), c.widthPx, KIT_EDGE_TOL_PX)
                ch.near("$tag: overlay bottom (the composable's edge)", kitBottom(g.overlay), c.heightPx, KIT_EDGE_TOL_PX)

                ch.near("$tag: overlayContent left = overlay ∩ safe", g.overlayContent.left, max(g.overlay.left, g.safe.left), KIT_EDGE_TOL_PX)
                ch.near("$tag: overlayContent top = overlay ∩ safe", g.overlayContent.top, max(g.overlay.top, g.safe.top), KIT_EDGE_TOL_PX)
                ch.near("$tag: overlayContent right = overlay ∩ safe", kitRight(g.overlayContent), min(kitRight(g.overlay), kitRight(g.safe)), KIT_EDGE_TOL_PX)
                ch.near("$tag: overlayContent bottom = overlay ∩ safe", kitBottom(g.overlayContent), min(kitBottom(g.overlay), kitBottom(g.safe)), KIT_EDGE_TOL_PX)
                ch.check(kitBottom(g.overlayContent) < kitBottom(g.overlay) - KIT_EDGE_TOL_PX) { "$tag: overlayContent ${g.overlayContent} ends where overlay ${g.overlay} does, despite the bottom inset" }

                ch.same("$tag: sheetContent left", g.sheetContent.left, g.overlayContent.left)
                ch.same("$tag: sheetContent top", g.sheetContent.top, g.overlayContent.top)
                ch.same("$tag: sheetContent right", kitRight(g.sheetContent), kitRight(g.overlayContent))
                val raised = min(kitBottom(g.overlayContent).toFloat(), c.heightPx - k * c.density)
                ch.near("$tag: sheetContent bottom, raised to at most the bottom − keyboardDp", kitBottom(g.sheetContent), raised, KIT_EDGE_TOL_PX)
                if (k == KIT_KEYBOARD_DP) {
                    ch.check(kitBottom(g.sheetContent) < kitBottom(g.overlayContent) - KIT_EDGE_TOL_PX) { "$tag: the keyboard did not raise sheetContent ${g.sheetContent} above overlayContent ${g.overlayContent}" }
                }
            }
        }
        ch.done()
    }

    // S-32: "The keyboard moves nothing else: only `sheetContent` reads `keyboardDp`; the bar, the board, the tray and
    // every control keep their slots while the keyboard is open (the WO-005 carry)"; Q3's row: "while the soft keyboard
    // is open only the DEV sheet's content moves above it, nothing else moves". With a keyboard of 0 and of 300 dp,
    // every other slot, the mapping, the tray scale, `safe`, `overlay` and `overlayContent` are equal — exactly — with
    // each screen's insets and with none. Discriminating: `sheetContent` differs, so the keyboard reached the geometry.
    @Test
    fun theKeyboardMovesNothingButTheSheetContent() {
        val ch = KitChecks("theKeyboardMovesNothingButTheSheetContent")
        for (c in kitCases()) {
            for (insets in listOf(c.insets, PlayInsets.NONE)) {
                val tag = "${c.tag}, insets $insets"
                val closed = c.geometry(insets = insets, keyboardDp = 0f)
                val open = c.geometry(insets = insets, keyboardDp = KIT_KEYBOARD_DP)
                ch.same("$tag: topBar", open.topBar, closed.topBar)
                ch.same("$tag: navPrev", open.navPrev, closed.navPrev)
                ch.same("$tag: navNext", open.navNext, closed.navNext)
                ch.same("$tag: counter", open.counter, closed.counter)
                ch.same("$tag: board", open.board, closed.board)
                ch.same("$tag: mapping", open.mapping, closed.mapping)
                ch.same("$tag: tray", open.tray, closed.tray)
                ch.same("$tag: trayPxPerUnit", open.trayPxPerUnit, closed.trayPxPerUnit)
                ch.same("$tag: cells", open.cells, closed.cells)
                ch.same("$tag: solvedThirds", open.solvedThirds, closed.solvedThirds)
                ch.same("$tag: density", open.density, closed.density)
                ch.same("$tag: safe", open.safe, closed.safe)
                ch.same("$tag: overlay", open.overlay, closed.overlay)
                ch.same("$tag: overlayContent", open.overlayContent, closed.overlayContent)
                ch.check(open.sheetContent != closed.sheetContent) { "$tag: sheetContent ${open.sheetContent} is the same with a 300 dp keyboard — the keyboard never reached the geometry" }
            }
        }
        ch.done()
    }

    // S-31: "`companion object { val NONE = PlayInsets(0f, 0f, 0f, 0f) }`"; `PlayLayout`'s new fields default to
    // `PlayInsets.NONE` and 0 (seam note 1: "every existing literal compiles and means what it meant"), so the phone
    // literal of KitPlay.kt and the tablet literal here, written without them, carry no insets and no keyboard.
    // S-32: with no insets the safe area is the whole composable. S-33: "With `PlayInsets.NONE` and no keyboard,
    // `content` = `area`: WO-004's and WO-005's look" — `overlayContent` and `sheetContent` equal `overlay`, exactly;
    // and `overlay` is the area WO-005's sheet was drawn over, "`Slot(0, bar bottom, W, H − bar bottom)`" (S-10Δ2 of
    // § WO-005, unchanged by seam note 1). The phone and both tablet orientations, both puzzles.
    @Test
    fun withNoInsetsAndNoKeyboardBothContentAreasAreTheOverlay() {
        assertEquals("S-31: PlayInsets.NONE", PlayInsets(topDp = 0f, bottomDp = 0f, leftDp = 0f, rightDp = 0f), PlayInsets.NONE)
        assertEquals("S-31: the phone literal's default insets", PlayInsets.NONE, KIT_PHONE_LAYOUT.insets)
        assertEquals("S-31: the phone literal's default keyboard", 0f, KIT_PHONE_LAYOUT.keyboardDp, 0f)
        assertEquals("S-31: the tablet literal's default insets", PlayInsets.NONE, KIT_TABLET_LAYOUT.insets)
        assertEquals("S-31: the tablet literal's default keyboard", 0f, KIT_TABLET_LAYOUT.keyboardDp, 0f)

        val ch = KitChecks("withNoInsetsAndNoKeyboardBothContentAreasAreTheOverlay")
        for (c in kitCases()) {
            val g = ScreenGeometry.of(c.widthPx, c.heightPx, c.density, c.bare, c.puzzle)
            val tag = "${c.name} with no insets, ${c.puzzle.id}"
            ch.same("$tag: safe is the whole composable", g.safe, Slot(0, 0, c.widthPx, c.heightPx))
            val barBottom = kitBottom(g.topBar)
            ch.same("$tag: overlay is WO-005's sheet area", g.overlay, Slot(0, barBottom, c.widthPx, c.heightPx - barBottom))
            ch.same("$tag: overlayContent = overlay", g.overlayContent, g.overlay)
            ch.same("$tag: sheetContent = overlay", g.sheetContent, g.overlay)
        }
        ch.done()
    }

    // ── Row helpers ──────────────────────────────────────────────────────────────────────────────

    /** True when one horizontal line crosses every slot of [row]: the lowest bottom lies below the highest top. */
    private fun kitOneLineCrosses(row: List<Slot>): Boolean = row.maxOf { it.top } < row.minOf { kitBottom(it) }

    /** Each slot of [slots] ends at or left of where the next begins: [ids] run left to right in that order. */
    private fun kitLeftToRight(ch: KitChecks, tag: String, ids: List<PieceId>, slots: List<Slot>) {
        for (i in 0 until slots.size - 1) {
            ch.check(kitRight(slots[i]) <= slots[i + 1].left + KIT_EDGE_TOL_PX) {
                "$tag: ${ids[i]} ${slots[i]} does not lie left of ${ids[i + 1]} ${slots[i + 1]} (TYPE-001 order ${ids})"
            }
        }
    }
}
