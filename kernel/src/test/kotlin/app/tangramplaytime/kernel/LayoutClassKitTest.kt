// Acceptance kit (WO-006, TASK-027) — TYPE-007's boundary, pinned on S-34 `LayoutClass` (tasks.md § WO-006 › Seams
// S-34; the WO is Light, the seam table is its design). Pins, no acceptance token: TYPE-007 is a TYPE rule, and the
// REQ lines that consume it (REQ-006, REQ-035 and REQ-036, A-lines included) are proven on the device by TASK-030's
// kit through the real app (tasks.md § WO-006, the TASK-030 row). traceability-rules.md §2: "A token is a coverage
// claim", so this file carries none.
//
// - The rule (Requirements/req_types.md TYPE-007): "Phone layout when the window is less than 600 dp wide; Tablet
//   layout when it is 600 dp or wider." It lives once in `:kernel` (architecture.md §1 D1 note: "TYPE-001/003/004/
//   005/006/007 rules are implemented once, in `:kernel`"; §3 component map: `:kernel` owns "TYPE-007 layout class").
// - S-34: public `enum class LayoutClass { PHONE, TABLET }` with `companion object { const val TABLET_MIN_WIDTH_DP:
//   Int = 600; fun of(uprightWidthDp: Int): LayoutClass }`: "PHONE below 600, TABLET at 600 or more". Pure JVM, no
//   `android.*` (G-12): this file compiles in `kernel/src/test`, a Kotlin/JVM source set with no Android on its
//   classpath (kernel/build.gradle.kts).
// - The argument is the window's width held upright (decisions.md 2026-10-17 "TYPE-007 reads the window's upright
//   width, once in `:kernel`"): `:app` passes `Configuration.smallestScreenWidthDp`, which a rotation does not
//   change. The widths this WO's channels present are that value: phones 360 × 780 and 390 × 844 dp (the display
//   overrides), 411 × 914 dp (the dev and floor images), 320 dp (the pending floor draft's narrowest phone,
//   tasks.md § WO-006 "Informative arithmetic"), and the tablet 1280 × 800 dp, whose upright width is 800.
// - Nothing is skipped; nothing reads a file. Helper names carry a `kit` prefix and are members of this class.
package app.tangramplaytime.kernel

import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutClassKitTest {

    private companion object {
        /** TYPE-007's boundary, written as the rule states it ("600 dp"), independent of the seam's constant. */
        const val KIT_BOUNDARY_DP = 600

        /** The widest width the sweep reaches; any width at or above the boundary is Tablet (TYPE-007). */
        const val KIT_SWEEP_TOP_DP = 4000
    }

    // TYPE-007 ("less than 600 dp wide" → Phone; "600 dp or wider" → Tablet) and S-34 ("const val TABLET_MIN_WIDTH_DP:
    // Int = 600"; "PHONE below 600, TABLET at 600 or more"): the boundary pair 599 / 600, and the constant itself.
    @Test
    fun theBoundaryIs600DpWithPhoneBelowAndTabletAtIt() {
        assertEquals("S-34: TABLET_MIN_WIDTH_DP", KIT_BOUNDARY_DP, LayoutClass.TABLET_MIN_WIDTH_DP)
        assertEquals("TYPE-007: 599 dp is less than 600 dp wide", LayoutClass.PHONE, LayoutClass.of(599))
        assertEquals("TYPE-007: 600 dp is 600 dp or wider", LayoutClass.TABLET, LayoutClass.of(600))
        assertEquals(
            "S-34: one below TABLET_MIN_WIDTH_DP is PHONE",
            LayoutClass.PHONE, LayoutClass.of(LayoutClass.TABLET_MIN_WIDTH_DP - 1),
        )
        assertEquals(
            "S-34: TABLET_MIN_WIDTH_DP itself is TABLET",
            LayoutClass.TABLET, LayoutClass.of(LayoutClass.TABLET_MIN_WIDTH_DP),
        )
    }

    // TYPE-007 names two layouts, Phone and Tablet; S-34: "enum class LayoutClass { PHONE, TABLET }".
    @Test
    fun thereAreExactlyTheTwoClassesTheTypeNames() {
        assertEquals("S-34: the classes", listOf(LayoutClass.PHONE, LayoutClass.TABLET), LayoutClass.entries.toList())
    }

    // TYPE-007 holds for every width, not only at the boundary: each whole width from 1 dp to 599 dp is "less than
    // 600 dp wide" → Phone, and each from 600 dp to 4000 dp is "600 dp or wider" → Tablet. A rule written with the
    // wrong comparison, or with a second threshold, fails here at the first width it gets wrong.
    @Test
    fun everyWidthBelow600IsPhoneAndEveryWidthFrom600IsTablet() {
        val wrong = ArrayList<String>()
        for (w in 1 until KIT_BOUNDARY_DP) kitExpect(w, LayoutClass.PHONE, wrong)
        for (w in KIT_BOUNDARY_DP..KIT_SWEEP_TOP_DP) kitExpect(w, LayoutClass.TABLET, wrong)
        if (wrong.isNotEmpty()) {
            throw AssertionError("TYPE-007: ${wrong.size} width(s) in the wrong class, first ${wrong.take(10)}")
        }
    }

    // decisions.md 2026-10-17 "TYPE-007 reads the window's upright width, once in `:kernel`": "Below 600 dp → Phone
    // … 600 dp or more → Tablet", read from the upright width. The upright widths of this WO's channels: the phones
    // 320 (the pending floor draft), 360 (the 360 × 780 dp override), 390 (REQ-035's 390 × 844 dp phone) and 411 dp
    // (the dev and floor images, 411 × 914 dp) are Phone; the tablet image and override at 1280 × 800 dp, upright
    // 800 dp, is Tablet. (Its landscape width, 1280, would be Tablet too; a phone's landscape width — 844 or 914 —
    // would not be Phone, which is why S-34's argument is the upright width; S-35 passes it, proven on the device.)
    @Test
    fun theUprightWidthsOfThisWorkOrdersChannelsLandInTheirClass() {
        val wrong = ArrayList<String>()
        for (w in listOf(320, 360, 390, 411)) kitExpect(w, LayoutClass.PHONE, wrong)
        kitExpect(800, LayoutClass.TABLET, wrong)
        if (wrong.isNotEmpty()) throw AssertionError("Q1 row: ${wrong.joinToString("; ")}")
    }

    /** Records in [wrong] when `LayoutClass.of(widthDp)` is not [expected]. */
    private fun kitExpect(widthDp: Int, expected: LayoutClass, wrong: MutableList<String>) {
        val got = LayoutClass.of(widthDp)
        if (got != expected) wrong += "$widthDp dp → $got, expected $expected"
    }
}
