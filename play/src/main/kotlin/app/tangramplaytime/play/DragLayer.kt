// WO-003 TASK-013 · the dragged piece (tasks.md § WO-003 › touch routing step 3; the tag table's `drag-piece`
// row) · REQ-014: a tray piece grows from the miniature to board size within 120 ms of the drag starting, floats
// above the finger and is never drawn at another size once on the board; REQ-019 / REQ-020: the lock glide and
// the go-home glide take 180 ms · G-17: the path is rebuilt in px at each frame's scale, never drawn under a
// canvas scale · O-01: every corner comes from `ILockEngine.vertices`. Every animation runs on Compose's frame
// clock (`withFrameNanos`), so a device test drives it through the compose rule's main clock (platform-notes).
package app.tangramplaytime.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.play.PlaySemantics.centre
import app.tangramplaytime.play.PlaySemantics.drawnPxPerUnit
import app.tangramplaytime.play.PlaySemantics.mirrored
import app.tangramplaytime.play.PlaySemantics.piece
import app.tangramplaytime.play.PlaySemantics.turn

/** REQ-014 rule: a tray piece grows from the miniature to board size within this many ms of the drag starting. */
internal const val GROW_MS = 120

/** REQ-019 rule (the lock animation) and REQ-020 rule (the return animation): 180 ms each. */
internal const val GLIDE_MS = 180

/**
 * Where the dragged piece was drawn when it was picked up: the miniature in its cell (then it grows, REQ-014)
 * or its place on the board (already at board size).
 *
 * @property centrePx the shape's centre then, `play-screen` px.
 * @property scale the scale it was drawn at then, px per unit.
 * @property fromTray true for a pick-up from the tray: the grow runs.
 */
internal class DragOrigin(val piece: PieceId, val centrePx: Offset, val scale: Float, val fromTray: Boolean)

/**
 * One glide after a drag ended (REQ-019 lock glide, REQ-020 go-home glide): the piece's node — `board-piece`
 * or `tray-piece`, which the controller shows at once — draws from where the dragged piece was at the release
 * to its lock or its cell over [GLIDE_MS].
 *
 * @property fromCentrePx the drawn centre at the release, `play-screen` px.
 * @property fromScale the drawn scale at the release, px per unit.
 * @property serial a new value per glide.
 */
internal class Glide(val piece: PieceId, val fromCentrePx: Offset, val fromScale: Float, val serial: Int)

/**
 * What the screen draws of a drag that the controller does not hold (S-2: "no time and no animation inside"):
 * where the piece came from, how far it has grown, and the glide of the piece that just left the hand. Compose
 * snapshot state, written by the pointer router and read by the drawing.
 */
internal class DragVisual {
    /** The running drag's origin, or null when nothing is dragged by touch. */
    var origin: DragOrigin? by mutableStateOf(null)
        private set

    /** The grow's progress, 0 at the pick-up to 1 at board size (REQ-014); 1 for a drag from the board. */
    var grow: Float by mutableFloatStateOf(1f)

    /** The glide of the piece that last left the hand, until it has arrived. */
    var glide: Glide? by mutableStateOf(null)
        private set

    private var glideSerial = 0

    /** A drag by touch starts at [origin]: the grow starts at 0 for a tray piece; a pending glide is dropped. */
    fun begin(origin: DragOrigin) {
        this.origin = origin
        grow = if (origin.fromTray) 0f else 1f
        glide = null
    }

    /** The drag ended (release or cancel) with the piece drawn at [centrePx] and [scale]: its node glides from there. */
    fun end(piece: PieceId, centrePx: Offset, scale: Float) {
        origin = null
        grow = 1f
        glide = Glide(piece, centrePx, scale, ++glideSerial)
    }

    /** [glide] has arrived; forget it (a later one may already have replaced it). */
    fun glideDone(glide: Glide) {
        if (this.glide === glide) this.glide = null
    }
}

/**
 * The dragged piece's drawn centre (`play-screen` px) and scale (px per unit) in this frame: the controller's
 * centre at the board scale once grown; on the way there, both eased from the origin (DI-1's cubic ease-out;
 * decisions row of TASK-013). A drag the router did not start (no origin) is drawn grown.
 */
internal fun drawnDrag(d: Dragged, visual: DragVisual, mapping: BoardMapping): Pair<Offset, Float> {
    val target = mapping.toPx(d.centre)
    val board = mapping.pxPerUnit
    val origin = visual.origin
    if (origin == null || origin.piece != d.piece) return target to board
    val k = easeOutCubic(visual.grow)
    if (k >= 1f) return target to board
    return lerp(origin.centrePx, target, k) to lerpF(origin.scale, board, k)
}

/** DI-1's grow curve `1 − (1 − k)³` (decisions row of TASK-013). */
internal fun easeOutCubic(t: Float): Float {
    val u = 1f - t.coerceIn(0f, 1f)
    return 1f - u * u * u
}

internal fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** The average of [points]: a drawn shape's centre, since the engine's corners average to the centre (TYPE-003). */
internal fun centroid(points: List<Offset>): Offset {
    var x = 0f
    var y = 0f
    for (p in points) {
        x += p.x
        y += p.y
    }
    return Offset(x / points.size, y / points.size)
}

/**
 * The px corners of [piece] at [turn] / [mirrored], drawn with its centre at [centrePx] and [scale] px per
 * unit — the engine's corners (O-01) scaled in px (G-17).
 */
internal fun piecePointsPx(engine: ILockEngine, piece: PieceId, turn: Turn, mirrored: Boolean, centrePx: Offset, scale: Float): List<Offset> =
    engine.vertices(piece, turn, mirrored, Point(0.0, 0.0)).map {
        Offset(centrePx.x + (it.x * scale).toFloat(), centrePx.y + (it.y * scale).toFloat())
    }

/**
 * Drives [onProgress] from 0 to 1 over [durationMs] on the composition's frame clock. Each value is written
 * inside the frame callback, so the frame that produced it draws it; the last call is exactly 1.
 */
internal suspend fun onFrames(durationMs: Int, onProgress: (Float) -> Unit) {
    val durationNanos = durationMs * 1_000_000L
    var start = -1L
    var done = false
    while (!done) {
        withFrameNanos { now ->
            if (start < 0L) start = now
            val elapsed = now - start
            if (elapsed >= durationNanos) {
                done = true
                onProgress(1f)
            } else {
                onProgress(elapsed.toFloat() / durationNanos.toFloat())
            }
        }
    }
}

/**
 * The dragged piece: one `drag-piece` node while the controller's `dragged` is set, drawn at this frame's
 * centre and scale ([drawnDrag]) with its path rebuilt in px, on top of everything. Its keys say where and at
 * what scale it is drawn now (the tag table); once grown, `CentreKey` is the controller's centre exactly.
 */
@Composable
internal fun DragLayer(controller: PlayController, geometry: ScreenGeometry, visual: DragVisual) {
    val d = controller.dragged ?: return
    val mapping = geometry.mapping
    val (centrePx, scale) = drawnDrag(d, visual, mapping)
    val grown = scale == mapping.pxPerUnit && centrePx == mapping.toPx(d.centre)
    val points = piecePointsPx(controller.engine, d.piece, d.turn, d.mirrored, centrePx, scale)
    PolygonNode(PlayTags.dragPiece(d.piece), points, PlayPalette.piece(d.piece)) {
        piece = d.piece
        turn = d.turn
        mirrored = d.mirrored
        centre = if (grown) d.centre else mapping.toUnits(centrePx)
        drawnPxPerUnit = scale
    }
}
