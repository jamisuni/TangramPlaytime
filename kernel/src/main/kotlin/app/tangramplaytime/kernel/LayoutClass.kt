// WO-006 TASK-029 · seam S-34 (tasks.md § WO-006 › Seams) · TYPE-007 (Requirements/req_types.md: "Phone layout when
// the window is less than 600 dp wide; Tablet layout when it is 600 dp or wider"), implemented once, in `:kernel`
// (architecture.md §1 D1 note, §3 component map) · decisions.md 2026-10-17 "TYPE-007 reads the window's upright width,
// once in `:kernel`" (planner Q1) · REQ-006 rule, REQ-035 and REQ-036 statements consume it · G-12: pure Kotlin/JVM.
package app.tangramplaytime.kernel

/**
 * TYPE-007's layout class: which of the two layouts the game uses on a given window (REQ-035: Phone, portrait only;
 * REQ-036: Tablet, both orientations).
 *
 * `:app` decides the class (O-05) and calls [of] directly as the composition root (the Q1 row's G-12 reading).
 */
enum class LayoutClass {
    /** The window is less than [TABLET_MIN_WIDTH_DP] wide held upright (REQ-035). */
    PHONE,

    /** The window is [TABLET_MIN_WIDTH_DP] or wider held upright (REQ-036). */
    TABLET,
    ;

    companion object {
        /** TYPE-007's boundary: "600 dp or wider" is Tablet. */
        const val TABLET_MIN_WIDTH_DP: Int = 600

        /**
         * The layout class of a window whose width held upright is [uprightWidthDp] (TYPE-007): [PHONE] below
         * [TABLET_MIN_WIDTH_DP], [TABLET] at it or above.
         *
         * The argument is the upright width (`Configuration.smallestScreenWidthDp`, passed by `:app`), which a rotation
         * does not change, so the class never depends on the orientation it decides (decisions.md 2026-10-17, Q1).
         */
        fun of(uprightWidthDp: Int): LayoutClass = if (uprightWidthDp < TABLET_MIN_WIDTH_DP) PHONE else TABLET
    }
}
