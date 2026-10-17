// WO-003 TASK-014 · the solving moment and the solved picture (tasks.md § WO-003 › the tag table's
// `solved-picture` and `board-piece` rows; workorders/WO-003.md § Constraints "Timings", "Draw in pixels") ·
// REQ-023: the picture replaces the coloured pieces with exactly the silhouette's outline; it fades in over
// 800 ms starting 600 ms after the last piece locks; with reduced motion the pieces do not pop and the
// picture still appears · REQ-039 A1: no picture paints outside its outline — the picture is clipped to
// `SilhouettePath`, the same path the silhouette fills · REQ-045 A2 · REQ-014 A2 and decisions.md "The solve
// pop is size-neutral": the pop is a brightness bounce, never a scale · `ArtLayer` KDoc: the base colour
// filling the silhouette, then the shapes in order; `ArtShape.Path`: absolute M/L/Q/C/Z in puzzle units
// (decisions.md 2026-10-11 "what a well-formed picture means") · G-17: every coordinate and width is mapped
// unit → px through `BoardMapping`; nothing is drawn under a canvas scale · the timeline runs on Compose's
// frame clock (`onFrames`), never a wall clock (platform-notes).
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.contracts.ArtShape
import app.tangramplaytime.contracts.ArtStyle
import app.tangramplaytime.contracts.Point
import kotlin.math.PI
import kotlin.math.sin

/** REQ-023 rule: the picture starts fading in this long after the last piece locks, and fades over [FADE_MS]. */
internal const val FADE_DELAY_MS = 600
internal const val FADE_MS = 800

/** REQ-023 rule: the confetti lasts 2.4 s. The celebration's timeline ends with it. */
internal const val CONFETTI_MS = 2400

/** The pop (decisions row of TASK-014): a 360 ms brightness bounce — the pieces blend up to this share toward `PlayPalette.board` at the peak — finished before the fade starts. */
internal const val POP_MS = 360
private const val POP_LIGHTEN = 0.45f

/** A stroke the file gives without a width is drawn this wide, in puzzle units (no REQ names it; decisions row of TASK-014). */
private const val DEFAULT_STROKE_UNITS = 0.05

/**
 * The thin outline drawn with the picture (DI-2 §5 "the silhouette edge stays visible as a thin outline"):
 * this wide, centred on the edge, so half of it survives the clip; bevel joins, so that at a corner the
 * stroke reaches no further inside than half its width (decisions row of TASK-014).
 */
private const val OUTLINE_STROKE_DP = 1.2f

/**
 * Where the open puzzle is on the celebration's timeline in this frame: [elapsedMs] since the last lock, on the
 * frame clock, or null while the puzzle is unsolved. The screen reads it for the pop, the picture's fade, when the
 * board pieces stop being drawn, how long the confetti lasts and when the solved row appears. A Solved puzzle whose
 * celebration is not playing stands at the end ([AT_END]): the picture is shown, nothing celebrates (REQ-023 is
 * about the moment of solving). WO-004 TASK-020: one value per frame, worked out in the composition by
 * `solveVisual` (`Celebration.kt`; S-3Δ), so a Solved puzzle is at its end in the very first frame it is drawn.
 */
internal class SolveVisual(val elapsedMs: Float?) {
    /** The pop's brightness bump, 0..1, for this frame; 0 with reduced motion (REQ-023 rule). */
    fun popBump(reducedMotion: Boolean): Float {
        val e = elapsedMs ?: return 0f
        if (reducedMotion || e >= POP_MS) return 0f
        return sin(PI * (e / POP_MS)).toFloat()
    }

    /** The picture's alpha, 0..1, or null before the fade starts (the node does not exist yet). */
    val pictureAlpha: Float?
        get() {
            val e = elapsedMs ?: return null
            if (e < FADE_DELAY_MS) return null
            return ((e - FADE_DELAY_MS) / FADE_MS).coerceIn(0f, 1f)
        }

    /** The board pieces are drawn until the picture is fully shown (the tag table; REQ-023 "replace"). */
    val piecesShown: Boolean get() = (pictureAlpha ?: 0f) < 1f

    /** The confetti exists during the celebration, never with reduced motion (REQ-023 rule). */
    fun confettiShown(reducedMotion: Boolean): Boolean {
        val e = elapsedMs ?: return false
        return !reducedMotion && e < CONFETTI_MS
    }

    /** The share of the piece colour replaced by `PlayPalette.board` in this frame of the pop. */
    fun popTint(reducedMotion: Boolean): Float = popBump(reducedMotion) * POP_LIGHTEN

    /**
     * The solved row replaces the tray from the frame the picture is fully shown (S-22 "Solved row"; decisions.md
     * 2026-10-15 "The top bar, Restart and the solved row, as drawn"; WO-004 checkpoint item 2), so at once for a
     * Solved puzzle standing at its end.
     */
    val rowShown: Boolean get() = (pictureAlpha ?: 0f) >= 1f

    companion object {
        /** An unsolved puzzle: no timeline. */
        val UNSOLVED: SolveVisual = SolveVisual(null)

        /** A Solved puzzle whose celebration is not playing: the end of the timeline. */
        val AT_END: SolveVisual = SolveVisual(CONFETTI_MS.toFloat())
    }
}

/**
 * The solved picture (REQ-023, REQ-039): a `solved-picture` node at the silhouette's box, drawing
 * [art] — the base colour over the whole box, then every shape in order, then the thin outline — clipped
 * to [silhouette] (the merged `SilhouettePath`, the path the silhouette itself is filled with) and, while
 * it fades, composited through one layer at [alpha]. Every unit coordinate and width goes through [mapping]
 * (G-17). The outline is stroked in the silhouette colour on the silhouette's own anti-aliased edge, so the
 * clip's edge — aliased on a hardware canvas of the floor image — never shows: inside it the stroke, outside
 * it the silhouette, both one colour.
 */
@Composable
internal fun SolvedPicture(art: ArtLayer, silhouette: Path, slot: Slot, mapping: BoardMapping, density: Float, alpha: Float) {
    val base = remember(art.base) { artColour(art.base, null) }
    val shapes = remember(art, mapping) { art.shapes.map { ArtDrawing.of(it, mapping, density) } }
    val outline = remember(density) { Stroke(OUTLINE_STROKE_DP * density, join = StrokeJoin.Bevel) }
    Box(
        Modifier
            .slot(slot)
            .testTag(PlayTags.SOLVED_PICTURE)
            .drawBehind {
                val bounds = Rect(0f, 0f, size.width, size.height)
                val fading = alpha < 1f
                if (fading) drawContext.canvas.saveLayer(bounds, Paint().apply { this.alpha = alpha })
                translate(-slot.leftPx, -slot.topPx) {
                    clipPath(silhouette) {
                        drawRect(base, topLeft = Offset(slot.leftPx, slot.topPx), size = Size(bounds.width, bounds.height))
                        for (shape in shapes) shape.draw(this)
                        drawPath(silhouette, PlayPalette.silhouette, style = outline)
                    }
                }
                if (fading) drawContext.canvas.restore()
            },
    )
}

/** `#RRGGBB` (the `ArtStyle` KDoc; the load check guarantees the form) with [opacity] applied. */
internal fun artColour(hex: String, opacity: Double?): Color {
    val v = hex.removePrefix("#").toLongOrNull(16) ?: 0L
    val c = Color(((v shr 16) and 0xFF).toInt(), ((v shr 8) and 0xFF).toInt(), (v and 0xFF).toInt())
    return if (opacity == null) c else c.copy(alpha = opacity.coerceIn(0.0, 1.0).toFloat())
}

/**
 * One art shape ready to draw in `play-screen` px: its fill and stroke colours, its stroke in px, and the
 * draw call itself. Built once per shape and mapping.
 */
internal class ArtDrawing(
    private val fill: Color?,
    private val stroke: Color?,
    private val strokeStyle: Stroke?,
    private val body: DrawScope.(Color, DrawStyle) -> Unit,
) {
    fun draw(scope: DrawScope) {
        if (fill != null) scope.body(fill, Fill)
        if (stroke != null && strokeStyle != null) scope.body(stroke, strokeStyle)
    }

    companion object {
        fun of(shape: ArtShape, mapping: BoardMapping, density: Float): ArtDrawing {
            val style = shape.style
            val px = mapping.pxPerUnit
            val fill = style.fill?.let { artColour(it, style.opacity) }
            val stroke = style.stroke?.let { artColour(it, style.opacity) }
            val strokeStyle = stroke?.let { Stroke(strokeWidthPx(style, px), cap = StrokeCap.Round, join = StrokeJoin.Round) }
            return when (shape) {
                is ArtShape.Polygon -> {
                    val path = polygonPath(shape.points.map(mapping::toPx))
                    ArtDrawing(fill, stroke, strokeStyle) { c, s -> drawPath(path, c, style = s) }
                }
                is ArtShape.Rect -> {
                    val topLeft = mapping.toPx(Point(shape.x, shape.y))
                    val size = Size((shape.w * px).toFloat(), (shape.h * px).toFloat())
                    val rx = shape.rx
                    if (rx != null && rx > 0.0) {
                        val radius = CornerRadius((rx * px).toFloat())
                        ArtDrawing(fill, stroke, strokeStyle) { c, s -> drawRoundRect(c, topLeft, size, radius, style = s) }
                    } else {
                        ArtDrawing(fill, stroke, strokeStyle) { c, s -> drawRect(c, topLeft, size, style = s) }
                    }
                }
                is ArtShape.Circle -> {
                    val centre = mapping.toPx(shape.c)
                    val r = (shape.r * px).toFloat()
                    ArtDrawing(fill, stroke, strokeStyle) { c, s -> drawCircle(c, r, centre, style = s) }
                }
                is ArtShape.Ellipse -> {
                    val centre = mapping.toPx(shape.c)
                    val rx = (shape.rx * px).toFloat()
                    val ry = (shape.ry * px).toFloat()
                    ArtDrawing(fill, stroke, strokeStyle) { c, s -> drawOval(c, Offset(centre.x - rx, centre.y - ry), Size(2 * rx, 2 * ry), style = s) }
                }
                is ArtShape.Line -> {
                    val from = mapping.toPx(shape.from)
                    val to = mapping.toPx(shape.to)
                    val width = strokeWidthPx(style, px)
                    // A line has no interior: only its stroke draws (the `ArtShape.Line` KDoc).
                    ArtDrawing(null, stroke, strokeStyle) { c, _ -> drawLine(c, from, to, width, cap = StrokeCap.Round) }
                }
                is ArtShape.Path -> {
                    val path = artPath(shape.d, mapping)
                    ArtDrawing(fill, stroke, strokeStyle) { c, s -> drawPath(path, c, style = s) }
                }
            }
        }

        private fun strokeWidthPx(style: ArtStyle, px: Float): Float = ((style.width ?: DEFAULT_STROKE_UNITS) * px).toFloat()
    }
}

private val ART_TOKEN = Regex("[MLQCZmlqcz]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?")

/**
 * The `ArtShape.Path` grammar — absolute M/L/Q/C/Z with 2/2/4/6/0 numbers (decisions.md 2026-10-11) — as a
 * px path through [mapping]. A token the grammar does not allow ends the path there (the library never
 * lists such a file, so this is belt and braces, not an outcome).
 */
internal fun artPath(d: String, mapping: BoardMapping): Path {
    val tokens = ART_TOKEN.findAll(d).map { it.value }.toList()
    val path = Path()
    var i = 0
    var cmd = 'M'
    fun number(): Double? = tokens.getOrNull(i)?.toDoubleOrNull()?.also { i++ }
    fun point(): Offset? {
        val x = number() ?: return null
        val y = number() ?: return null
        return mapping.toPx(Point(x, y))
    }
    while (i < tokens.size) {
        val t = tokens[i]
        if (t.length == 1 && t[0].isLetter()) {
            cmd = t[0].uppercaseChar()
            i++
            if (cmd == 'Z') {
                path.close()
                continue
            }
        }
        when (cmd) {
            'M' -> { val p = point() ?: return path; path.moveTo(p.x, p.y); cmd = 'L' }
            'L' -> { val p = point() ?: return path; path.lineTo(p.x, p.y) }
            'Q' -> {
                val c = point() ?: return path
                val p = point() ?: return path
                path.quadraticTo(c.x, c.y, p.x, p.y)
            }
            'C' -> {
                val c1 = point() ?: return path
                val c2 = point() ?: return path
                val p = point() ?: return path
                path.cubicTo(c1.x, c1.y, c2.x, c2.y, p.x, p.y)
            }
            else -> return path
        }
    }
    return path
}
