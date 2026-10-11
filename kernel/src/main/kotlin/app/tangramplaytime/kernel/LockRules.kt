// WO-001 TASK-001 · design §2 `LockRules` · TYPE-004 lock parameters, defined once in :kernel (G-04).
package app.tangramplaytime.kernel

import kotlin.math.max

/**
 * The TYPE-004 lock parameters, in one place for the whole kernel (guardrail G-04: lock
 * distance, scoring and tolerance are exactly TYPE-004 and live only in `:kernel`).
 */
internal object LockRules {
    /** TYPE-004: lock distance R = 0.65 units (TYPE-001 unit). */
    const val LOCK_DISTANCE: Double = 0.65

    /** TYPE-004: R is never below 30 dp on screen. */
    const val FLOOR_DP: Double = 30.0

    /** TYPE-004: score = |t| − 0.04 × (number of piece corners that land exactly on anchors). */
    const val SNUG_BONUS: Double = 0.04

    /** TYPE-004: only candidates with |t| ≤ (best |t|) + 0.16 are considered. */
    const val WINDOW: Double = 0.16

    /**
     * TYPE-004: "inside" and "overlaps" are decided with a tolerance of at most 1e-6 units.
     * Also the coincidence tolerance for points (design §3 "Exact-anchor arithmetic":
     * compare at 1e-6, never with `==`).
     */
    const val EPS: Double = 1e-6

    /**
     * Floating-point comparison slack for values that are equal in exact arithmetic
     * (design §1 and §2: the distance test d ≤ R + 1e-9, score ties, the clip's inside
     * test, the on-edge test). Far below [EPS], so it never changes a TYPE-004 decision.
     */
    const val CMP: Double = 1e-9

    /**
     * The lock distance R in puzzle units for a board drawn at [dpPerUnit] dp per unit:
     * `max(0.65, 30.0 / dpPerUnit)` (TYPE-004; `ILockEngine` "Lock distance").
     * `dpPerUnit` = 100.0 gives 0.65; 20.0 gives 1.5 (the 30 dp floor).
     *
     * @throws IllegalArgumentException when [dpPerUnit] is not finite or ≤ 0 — a
     *   programming error (G-15; design §2 `findLock`).
     */
    fun lockRadius(dpPerUnit: Double): Double {
        // G-15: a non-positive or non-finite scale is a caller bug, not an expected outcome.
        require(dpPerUnit.isFinite() && dpPerUnit > 0.0) {
            "dpPerUnit must be finite and > 0, was $dpPerUnit"
        }
        return max(LOCK_DISTANCE, FLOOR_DP / dpPerUnit)
    }
}
