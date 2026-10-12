// WO-003 TASK-010 · seam S-7 (tasks.md § WO-003 › Seams) · REQ-017: 45° steps with boundaries at ±22.5°, ±67.5° …
// from where the second finger started. Screen-angle arithmetic (input), not puzzle geometry (O-01).
package app.tangramplaytime.play

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Counts the 45° steps of a two-finger twist (REQ-017) from the second finger's angle around the first,
 * in degrees of the screen (y down, so positive is clockwise), as `atan2` reports it in (−180°, 180°].
 *
 * The twist is unwrapped: each new angle is read as the shortest turn from the last one, so a jump
 * across ±180° counts the way the finger went (170° → −100° is +90°). The net steps are
 * `sign × floor((|twist| + 22.5) / 45)`: 20° → 0, 90° → 2, −90° → −2, and a twist exactly on a
 * boundary (±22.5°, ±67.5° …) counts as having passed it (TASK-010's recorded decision).
 *
 * @param startAngleDeg the second finger's angle where it touched.
 */
internal class TwistTracker(startAngleDeg: Double) {
    private var lastAngleDeg = startAngleDeg
    private var twistDeg = 0.0

    /**
     * The second finger is now at [angleDeg].
     *
     * @return the net 45° steps since the finger touched, positive = clockwise.
     */
    fun steps(angleDeg: Double): Int {
        twistDeg += shortestTurn(angleDeg - lastAngleDeg)
        lastAngleDeg = angleDeg
        val magnitude = floor((abs(twistDeg) + HALF_STEP_DEG) / STEP_DEG).toInt()
        return if (twistDeg < 0) -magnitude else magnitude
    }

    /** [deltaDeg] wrapped into (−180°, 180°]: the shortest turn that explains the change. */
    private fun shortestTurn(deltaDeg: Double): Double = deltaDeg - 360.0 * ceil((deltaDeg - 180.0) / 360.0)

    private companion object {
        const val STEP_DEG = 45.0
        const val HALF_STEP_DEG = 22.5
    }
}
