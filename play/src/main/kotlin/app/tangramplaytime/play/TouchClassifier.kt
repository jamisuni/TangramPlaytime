// WO-003 TASK-010 · seam S-8 (tasks.md § WO-003 › Seams) · REQ-015: a tap moves < 12 dp, whatever its duration.
package app.tangramplaytime.play

import kotlin.math.hypot

/** The tap slop of REQ-015 in dp: a touch that moves less than this from its down point is a tap. */
internal const val TAP_SLOP_DP = 12f

/**
 * Tells a tap from a drag for one touch, on raw pointer positions in px (REQ-015). There is no time
 * input: a touch that ends without becoming a drag is a tap whatever its duration.
 *
 * @param slopPx [TAP_SLOP_DP] at the screen's density.
 */
internal class TouchClassifier(private val slopPx: Float) {
    private var downX = 0f
    private var downY = 0f

    /** True from the move at which the touch has moved at least the slop from its down point. */
    var isDrag: Boolean = false
        private set

    /** A new touch at ([x], [y]); it starts as a tap. */
    fun down(x: Float, y: Float) {
        downX = x
        downY = y
        isDrag = false
    }

    /**
     * The touch moved to ([x], [y]).
     *
     * @return [isDrag] after this move: true from the move that reaches the slop on.
     */
    fun move(x: Float, y: Float): Boolean {
        if (!isDrag && hypot(x - downX, y - downY) >= slopPx) isDrag = true
        return isDrag
    }
}
