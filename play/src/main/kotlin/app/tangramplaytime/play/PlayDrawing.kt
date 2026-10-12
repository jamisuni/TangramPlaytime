// WO-003 TASK-012 · internal drawing helpers of the Play screen (tasks.md § WO-003 TASK-012 "plus internal
// drawing helpers") · REQ-011 (a locked piece drawn flat in its own colour over the silhouette), REQ-012 and
// REQ-013 (the tray miniatures, each in its cell at the one tray scale), REQ-014 (the dragged piece at the board
// scale), REQ-021 (the preview's outline path), REQ-023 (the picture's shapes), all through `PolygonNode`,
// `polygonPath` and the whole-px `Slot`s that give every node the tag table's bounds (code review N4) ·
// architecture.md G-17: every node is placed and every path is built in px; nothing is drawn under a canvas
// scale.
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A node's place in the screen, whole px of the `play-screen` node's own coordinates. A node's semantics
 * bounds are exactly this rectangle, so the tag table's "the drawn shape's box" is met to the pixel.
 */
internal data class Slot(val left: Int, val top: Int, val width: Int, val height: Int) {
    val leftPx: Float get() = left.toFloat()
    val topPx: Float get() = top.toFloat()

    companion object {
        /** The whole-px slot that covers [r]: the floor of its left/top, the ceiling of its right/bottom. */
        fun around(r: Rect): Slot {
            val l = floor(r.left).toInt()
            val t = floor(r.top).toInt()
            return Slot(l, t, (ceil(r.right).toInt() - l).coerceAtLeast(0), (ceil(r.bottom).toInt() - t).coerceAtLeast(0))
        }
    }
}

/** Places this node at [slot] inside a [SlotLayout]. */
internal fun Modifier.slot(slot: Slot): Modifier = layoutId(slot)

/**
 * A layout whose children sit where their [Slot] says, in px, in content order (a later child draws on
 * top of an earlier one). A child without a slot fills the whole layout at (0, 0) — a full-screen layer.
 */
@Composable
internal fun SlotLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
        val placed = measurables.map { measurable ->
            val slot = measurable.layoutId as? Slot
            if (slot == null) {
                measurable.measure(Constraints.fixed(width, height)) to IntOffset.Zero
            } else {
                measurable.measure(Constraints.fixed(slot.width, slot.height)) to IntOffset(slot.left, slot.top)
            }
        }
        layout(width, height) {
            for ((placeable, offset) in placed) placeable.place(offset)
        }
    }
}

/** The bounding box of [points]. */
internal fun boxOf(points: List<Offset>): Rect =
    Rect(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })

/** A closed polygon through [points], shifted by ([dx], [dy]) — e.g. into a slot's local coordinates. */
internal fun polygonPath(points: List<Offset>, dx: Float = 0f, dy: Float = 0f): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points[0].x + dx, points[0].y + dy)
    for (i in 1 until points.size) lineTo(points[i].x + dx, points[i].y + dy)
    close()
}

/**
 * One flat polygon as a tagged node: placed at the whole-px slot around [pointsPx] (`play-screen` px),
 * filled with [colour], carrying [tag] and the [semantics] the tag table names for it. The path is built
 * in px and only shifted into the slot (G-17).
 */
@Composable
internal fun PolygonNode(tag: String, pointsPx: List<Offset>, colour: Color, semantics: SemanticsPropertyReceiver.() -> Unit) {
    val slot = remember(pointsPx) { Slot.around(boxOf(pointsPx)) }
    val path = remember(pointsPx) { polygonPath(pointsPx, -slot.leftPx, -slot.topPx) }
    Box(
        Modifier
            .slot(slot)
            .testTag(tag)
            .semantics(properties = semantics)
            .drawBehind { drawPath(path, colour) },
    )
}
