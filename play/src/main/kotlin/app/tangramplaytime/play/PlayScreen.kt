// WO-003 TASK-012 · seam S-10, the static core (tasks.md § WO-003 › Seams; the tag table) · REQ-011 (the flat
// silhouette), REQ-012 (the fixed tray, the title), REQ-013 (one tray scale, S-6), REQ-045 (a mini's tray) ·
// architecture.md G-17 (px only), G-12 (:contracts + Compose), O-01 (every corner from ILockEngine.vertices),
// O-05 (`:app` hands the layout in), O-06 (texts come in through PlayTexts).
// WO-003 TASK-013 · the touch routing (`PointerRouter` on the root), the dragged piece (`DragLayer`), the
// landing preview (`PreviewLayer`), the lock glide and the go-home glide of REQ-019 / REQ-020 (180 ms, on the
// board piece and the miniature), and the shake of a refused board turn (REQ-016).
// WO-003 TASK-014 · the decorations (`TrayDecorations`: the ghosts of REQ-012 and the size marks of REQ-043;
// `FlipBadge`: REQ-018 in the tray and next to the placed parallelogram), the corner pulse of a mini
// (`CornerPulse`, REQ-051) and the solve (`SolvedPicture`, `Celebration`: REQ-023, REQ-039 A1, REQ-045 A2 —
// the size-neutral pop, the picture masked by the silhouette's own path, the confetti, reduced motion).
// WO-003 code review F2 · the one-shot cues (`ShownCues`): the corner pulse (REQ-051 "pulse … once") and the
// shake of a refused turn or mirror (REQ-016 "shake it once", REQ-018 rule) are played once per serial, never
// again by a screen composed later over the same controller (an activity recreation).
// WO-004 TASK-020 · S-22 (designs/WO-004-design.md §2 "Touch geometry"): the top bar's three full-height columns
// (`TopBar`), the Restart pill above the silhouette and below the pieces, and the solved row over the tray strip
// (`Controls`), with the controls the router may hit published each frame (`ShownControls`); the screen side of S-3Δ
// (`ShownCues.celebrationSerial`, the timeline worked out by `solveVisual`). A Solved puzzle draws neither tray
// pieces nor size marks: its tray is replaced (REQ-026 rule), and its board is empty (S-21).
// WO-004 TASK-021 · S-22's overview (`Overview`): drawn last, over the screen below the top bar, while the
// controller's `overviewOpen` holds.
// WO-005 TASK-026 · S-10Δ2 (tasks.md § WO-005 › Seams; the touch geometry's z-order) · REQ-046 through its one door
// (`DevAidHook`, decisions.md 2026-10-17 "The DEV aid reaches the screen through one door"): with an aid, the button's
// slot is published with the other controls each composition, the solution layer follows the silhouette, the button
// follows the solved row, and the sheet comes last, below the top bar, while it is open. Without one (the default),
// nothing of it is composed and the screen is exactly WO-004's.
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.play.PlaySemantics.boardMapping
import app.tangramplaytime.play.PlaySemantics.centre
import app.tangramplaytime.play.PlaySemantics.drawnPxPerUnit
import app.tangramplaytime.play.PlaySemantics.mirrored
import app.tangramplaytime.play.PlaySemantics.piece
import app.tangramplaytime.play.PlaySemantics.puzzleId
import app.tangramplaytime.play.PlaySemantics.puzzleState
import app.tangramplaytime.play.PlaySemantics.turn
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** DI-1 `layout()`: the board starts 4 dp below the top bar (not a REQ number; decisions.md row of TASK-012). */
private const val BOARD_GAP_TOP_DP = 4f

/** DI-1 `layout()`: the board ends 8 dp above the tray strip (decisions.md row of TASK-012). */
private const val BOARD_GAP_BOTTOM_DP = 8f

/** DI-1: the board's corner radius (decisions.md row of TASK-012). */
private const val BOARD_CORNER_DP = 26f

/** DI-1 `layout()`: the silhouette uses 82 % of the board's width and 80 % of its height — "about 10 % padding" (design-inputs.md §2 "Also fine"; REQ-035 is WO-006's). */
private const val BOARD_FIT_WIDTH = 0.82f
private const val BOARD_FIT_HEIGHT = 0.80f

/** REQ-016 rule "shake it once": DI-1's shake, 360 ms, ±6 dp sideways (no REQ names the numbers; decisions row of TASK-013). */
private const val SHAKE_MS = 360
private const val SHAKE_AMPLITUDE_DP = 6f

/**
 * Which one-shot cues this screen counts as shown (code review F2): the serial of the last corner pulse
 * (REQ-051 "pulse … once") and of the last shake (REQ-016 "shake it once", REQ-018 rule "mirrors back with a
 * short shake"). The controller keeps its last `pulse` and `shake` (S-1) for as long as it lives — the process,
 * in the app (S-15) — and a screen composed over it later (an activity recreation: a rotation, a locale or a
 * theme change) must not play them again. So a cue already set when this screen first composes counts as
 * shown (an earlier screen played it, or its moment has passed), and a cue counts as shown once its animation
 * has ended; only a serial that differs is played. Snapshot state: a cue's node goes when it counts as shown.
 * Remembered with the controller, never saved, so a serial from another process's controller cannot match.
 * WO-004 (S-3Δ): the celebration of a solve is such a cue too — one set before this screen composed (an activity
 * recreation right after a solve) is never replayed: the puzzle stands at the end of its timeline from the first
 * frame (REQ-026 A1; design "What survives").
 */
internal class ShownCues(pulse: Pulse?, shake: Shake?, celebration: Celebration?) {
    /** The serial of the last pulse shown (or set before this screen composed), or null. */
    var pulseSerial: Int? by mutableStateOf(pulse?.serial)

    /** The serial of the last shake shown (or set before this screen composed), or null. */
    var shakeSerial: Int? by mutableStateOf(shake?.serial)

    /** The serial of the last celebration shown (or set before this screen composed), or null (S-3Δ). */
    var celebrationSerial: Int? by mutableStateOf(celebration?.serial)
}

/**
 * The Play screen (S-10): the top bar (‹, the counter with the title, the rating dots, "n / N" and the state word,
 * ›), the board with the flat silhouette, the Restart pill while In progress and the locked pieces, and the tray
 * with the puzzle's cells and miniatures — replaced by the solved row once a Solved puzzle's picture is fully
 * shown; the dragged piece and the landing preview while a drag runs; the grid overview below the bar while it is
 * open (S-22). It fills [modifier]'s size, lays every zone
 * out in px, sets [PlayController.boardDpPerUnit] at every layout, routes every touch through one pointer handler
 * on the root (`PointerRouter`), and gives every node the tag, bounds and keys of the tag table and S-13Δ.
 *
 * @param controller the state holder (S-1, S-2); its `engine` supplies every corner (O-01).
 * @param layout the zone values `:app` decided (S-5, O-05).
 * @param texts the title language, the size-mark letters and the texts of the bar and the controls (S-11, S-11Δ, O-06).
 * @param reducedMotion the system's "remove animations" setting: no pop, no confetti, an unanimated pulse;
 *   the picture and the corner markers still appear (REQ-023 and REQ-051 rules).
 * @param devAid the testing aid a debug build installs (REQ-046; S-10Δ2, `DevAidHook.installed` in the app), or null:
 *   then the screen composes, hits and stands aside for nothing of it.
 */
@Composable
fun PlayScreen(
    controller: PlayController,
    layout: PlayLayout,
    texts: PlayTexts,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    devAid: DevAidHook? = null,
) {
    val puzzle = controller.puzzle
    val state = controller.state
    val density = LocalDensity.current.density
    val visual = remember(controller) { DragVisual() }
    val cues = remember(controller) { ShownCues(controller.pulse, controller.shake, controller.celebration) }
    val solve = solveVisual(controller, cues)
    val geometryNow = remember(controller) { mutableStateOf<ScreenGeometry?>(null) }
    val shownNow = remember(controller) { mutableStateOf(ShownControls.NONE) }
    val router = remember(controller, visual, layout.floatOffsetDp, devAid) {
        PointerRouter(controller, visual, { geometryNow.value }, { shownNow.value }, layout.floatOffsetDp, devAid)
    }
    BoxWithConstraints(
        modifier
            .testTag(PlayTags.SCREEN)
            .semantics {
                puzzleId = puzzle.id
                puzzleState = state
            }
            .drawBehind { drawRect(PlayPalette.paper) }
            .pointerInput(router) { with(router) { route() } },
    ) {
        val widthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val heightPx = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
        if (widthPx > 0 && heightPx > 0) {
            val geometry = remember(widthPx, heightPx, density, layout, puzzle) {
                ScreenGeometry.of(widthPx, heightPx, density, layout, puzzle)
            }
            // S-22: the Restart pill only In progress (REQ-025 rule); the solved row from the frame the picture is
            // fully shown (at once for a puzzle opened already solved). The router hits exactly what is drawn.
            val restart = if (state == PuzzleState.IN_PROGRESS) rememberRestartSlot(geometry, texts.restart) else null
            val solvedRow = state == PuzzleState.SOLVED && solve.rowShown
            // S-10Δ2: the DEV button's slot, at each composition, published with the other controls (S-22Δ2).
            val devButton = devAid?.rememberButtonSlot(controller, geometry)
            val shown = ShownControls(restart, solvedRow, devButton)
            SideEffect {
                controller.boardDpPerUnit = (geometry.mapping.pxPerUnit / density).toDouble()
                geometryNow.value = geometry
                shownNow.value = shown
            }
            val silhouetteSlot = remember(puzzle, geometry.mapping) { Slot.around(SilhouettePath.boundsPx(puzzle.solution, geometry.mapping)) }
            val silhouettePath = remember(puzzle, geometry.mapping) { SilhouettePath.build(puzzle.solution, geometry.mapping) }
            val trayPieces = if (state == PuzzleState.SOLVED) emptySet() else controller.trayPieces
            val badges = badgeSlots(controller, geometry)
            val stateWord = when (state) {
                PuzzleState.NEW -> null
                PuzzleState.IN_PROGRESS -> texts.inProgress
                PuzzleState.SOLVED -> texts.solved
            }
            SlotLayout(Modifier.fillMaxSize()) {
                TopBar(geometry, texts.titleOf(puzzle), puzzle.rating, texts.counterOf(controller.puzzleNumber, controller.puzzleCount), stateWord)
                BoardArea(geometry)
                Silhouette(silhouetteSlot, silhouettePath)
                // S-10Δ2: the aid's solution layer, right after the silhouette (under the pill and the pieces).
                devAid?.SolutionLayer(controller, geometry)
                restart?.let { RestartPill(it, texts.restart) }
                if (solve.piecesShown) {
                    val tint = solve.popTint(reducedMotion)
                    for (placement in controller.placed) {
                        key(placement.piece) { BoardPiece(controller, placement, geometry.mapping, visual, cues, tint) }
                    }
                }
                solve.pictureAlpha?.let { alpha ->
                    SolvedPicture(puzzle.art, silhouettePath, silhouetteSlot, geometry.mapping, density, alpha)
                }
                badges.board?.let { FlipBadge(PlayTags.FLIP_BADGE_BOARD, it, density) }
                TrayStrip(geometry.tray)
                if (state != PuzzleState.SOLVED) {
                    for (id in puzzle.pieces) {
                        key(id) {
                            geometry.cells[id]?.let { TrayCell(id, it) }
                            if (id !in trayPieces) TrayGhost(controller, id, geometry)
                        }
                    }
                }
                for (id in trayPieces) {
                    key(id) {
                        TrayPiece(controller, id, controller.trayTurn(id), controller.trayMirrored(id), geometry, visual)
                    }
                }
                for (id in trayPieces) {
                    val letter = sizeMarkOf(id, texts) ?: continue
                    val cell = geometry.cells[id] ?: continue
                    key(id) { SizeMark(id, letter, cell, density) }
                }
                badges.tray?.let { FlipBadge(PlayTags.FLIP_BADGE_TRAY, it, density) }
                if (solvedRow) SolvedRow(geometry, texts, controller.bestSeconds)
                // S-10Δ2: the DEV button, after the solved row and before the landing preview, while it is shown.
                if (devAid != null && devButton != null) devAid.ButtonLayer(devButton)
                PreviewLayer(controller, geometry.mapping)
                DragLayer(controller, geometry, visual)
                CornerPulse(controller, geometry, reducedMotion, cues)
                solve.elapsedMs?.let { if (solve.confettiShown(reducedMotion)) Confetti(geometry, it) }
                // S-22 "Overview": over everything below the top bar, which stays drawn (and inert: hit order 0). Its
                // open state is the controller's, so a recreated screen shows it from the first frame ("What survives").
                if (controller.overviewOpen) {
                    val barBottom = geometry.topBar.top + geometry.topBar.height
                    Overview(controller, Slot(0, barBottom, widthPx, (heightPx - barBottom).coerceAtLeast(0)), texts)
                }
                // S-10Δ2: the aid's sheet last, over the screen below the top bar, while it is open (hit order 0).
                if (devAid != null && devAid.sheetOpen) {
                    val barBottom = geometry.topBar.top + geometry.topBar.height
                    devAid.SheetLayer(controller, Slot(0, barBottom, widthPx, (heightPx - barBottom).coerceAtLeast(0)))
                }
            }
        }
    }
}

/**
 * Where everything sits for one screen size, whole px of the `play-screen` node: the top bar and its three
 * columns (S-22), the board and its mapping, the tray strip and its cells (S-6 with the screen's own size), and
 * the thirds the solved row splits the strip into (S-22).
 */
internal class ScreenGeometry(
    val topBar: Slot,
    /** ‹: x from 0 to 60 dp, the bar's full height (S-22). */
    val navPrev: Slot,
    /** ›: x from W − 60 dp to W, the bar's full height (S-22). */
    val navNext: Slot,
    /** The counter zone: everything between ‹ and ›, the bar's full height (S-22). */
    val counter: Slot,
    val board: Slot,
    val mapping: BoardMapping,
    val tray: Slot,
    /** The miniatures' common scale, px per unit (REQ-013). */
    val trayPxPerUnit: Float,
    val cells: Map<PieceId, Slot>,
    /** The tray strip in thirds, left to right: Retry, the best time, Next (S-22 "Solved row"). */
    val solvedThirds: List<Slot>,
    /** The screen's density, px per dp, for the dp sizes of the decorations (the badge's 60 dp area, the marks, the markers). */
    val density: Float,
) {
    companion object {
        fun of(widthPx: Int, heightPx: Int, density: Float, layout: PlayLayout, puzzle: PuzzleDefinition): ScreenGeometry {
            val topBarPx = (layout.topBarDp * density).roundToInt()
            val topBar = Slot(0, 0, widthPx, topBarPx)
            val column = (NAV_COLUMN_DP * density).roundToInt().coerceAtMost(widthPx / 2)
            val navPrev = Slot(0, 0, column, topBarPx)
            val navNext = Slot(widthPx - column, 0, column, topBarPx)
            val counter = Slot(column, 0, (widthPx - 2 * column).coerceAtLeast(0), topBarPx)

            val trayGeometry = TrayMetrics.layout(widthPx / density, heightPx / density, layout, puzzle.pieces)
            val trayTop = (heightPx - trayGeometry.heightDp * density).roundToInt().coerceIn(0, heightPx)
            val tray = Slot(0, trayTop, widthPx, heightPx - trayTop)
            val cells = trayGeometry.cells.mapValues { (_, c) ->
                Slot(
                    left = (c.xDp * density).roundToInt(),
                    top = trayTop + (c.yDp * density).roundToInt(),
                    width = (c.widthDp * density).roundToInt(),
                    height = (c.heightDp * density).roundToInt(),
                )
            }

            val side = (layout.sideMarginDp * density).roundToInt()
            val boardTop = topBarPx + (BOARD_GAP_TOP_DP * density).roundToInt()
            val boardBottom = trayTop - (BOARD_GAP_BOTTOM_DP * density).roundToInt()
            val board = Slot(side, boardTop, (widthPx - 2 * side).coerceAtLeast(0), (boardBottom - boardTop).coerceAtLeast(0))

            val points = puzzle.solution.flatMap { it.polygon }
            val minX = points.minOf { it.x }
            val minY = points.minOf { it.y }
            val unitWidth = max(points.maxOf { it.x } - minX, 1e-6)
            val unitHeight = max(points.maxOf { it.y } - minY, 1e-6)
            val scale = min(board.width * BOARD_FIT_WIDTH / unitWidth, board.height * BOARD_FIT_HEIGHT / unitHeight).toFloat()
            val mapping = BoardMapping(
                originXPx = (board.leftPx + (board.width - unitWidth * scale) / 2 - minX * scale).toFloat(),
                originYPx = (board.topPx + (board.height - unitHeight * scale) / 2 - minY * scale).toFloat(),
                pxPerUnit = scale,
            )
            val cuts = listOf(0, (widthPx / 3f).roundToInt(), (widthPx * 2 / 3f).roundToInt(), widthPx)
            val thirds = (0 until 3).map { i -> Slot(cuts[i], tray.top, cuts[i + 1] - cuts[i], tray.height) }
            return ScreenGeometry(topBar, navPrev, navNext, counter, board, mapping, tray, trayGeometry.scaleDpPerUnit * density, cells, thirds, density)
        }
    }
}

/** The board: a rounded white area between the top bar and the tray, carrying the mapping. */
@Composable
private fun BoardArea(g: ScreenGeometry) {
    val density = LocalDensity.current.density
    val mapping = g.mapping
    Box(
        Modifier
            .slot(g.board)
            .testTag(PlayTags.BOARD)
            .semantics { boardMapping = mapping }
            .drawBehind { drawRoundRect(PlayPalette.board, cornerRadius = CornerRadius(BOARD_CORNER_DP * density)) },
    )
}

/** The silhouette: one merged path in px ([SilhouettePath], shared with the solved picture's mask), filled once with one colour (REQ-011). */
@Composable
private fun Silhouette(slot: Slot, path: Path) {
    Box(
        Modifier
            .slot(slot)
            .testTag(PlayTags.SILHOUETTE)
            .drawBehind { translate(-slot.leftPx, -slot.topPx) { drawPath(path, PlayPalette.silhouette) } },
    )
}

/**
 * A locked piece, in its own colour on top of the silhouette (REQ-011 rule), at the board scale. After a drop
 * by touch it glides from where the dragged piece was to its lock over [GLIDE_MS] (REQ-019 rule); a refused
 * turn or mirror in place shakes it once (REQ-016 rule) — a shake that [cues] counts as shown is never played
 * (code review F2), and a shake cut short by another piece's refusal ends at the placement. Its keys follow the
 * drawing in each frame; at rest `CentreKey` is the placement's centre
 * exactly. [popTint] is the solve's size-neutral pop: the share of the colour blended toward
 * `PlayPalette.board` in this frame (REQ-014 A2 holds: the shape and its scale never change).
 */
@Composable
private fun BoardPiece(controller: PlayController, placement: Placement, mapping: BoardMapping, visual: DragVisual, cues: ShownCues, popTint: Float) {
    val density = LocalDensity.current.density
    val engine = controller.engine
    val glide = visual.glide?.takeIf { it.piece == placement.piece }
    val shake = controller.shake?.takeIf { it.piece == placement.piece && it.serial != cues.shakeSerial }
    var glideProgress by remember(glide) { mutableFloatStateOf(if (glide == null) 1f else 0f) }
    var shakeDx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(glide) {
        if (glide != null) {
            onFrames(GLIDE_MS) { glideProgress = it }
            visual.glideDone(glide)
        }
    }
    LaunchedEffect(shake) {
        if (shake != null) {
            try {
                onFrames(SHAKE_MS) { shakeDx = shakeOffset(it) * SHAKE_AMPLITUDE_DP * density }
                cues.shakeSerial = shake.serial
            } finally {
                // A shake cut short — another piece's refusal replaced it — leaves the piece at its placement
                // (REQ-016 A2 "keeps its position"; fix batch, reviewer note 2).
                shakeDx = 0f
            }
        }
    }
    val restPoints = remember(placement, mapping) {
        engine.vertices(placement.piece, placement.turn, placement.mirrored, placement.centre).map(mapping::toPx)
    }
    val k = easeOutCubic(glideProgress)
    val gliding = glide != null && k < 1f
    val moving = gliding || shakeDx != 0f
    val targetPx = mapping.toPx(placement.centre)
    val centrePx = (if (glide != null && k < 1f) lerp(glide.fromCentrePx, targetPx, k) else targetPx) + Offset(shakeDx, 0f)
    val scale = if (glide != null && k < 1f) lerpF(glide.fromScale, mapping.pxPerUnit, k) else mapping.pxPerUnit
    val points = if (moving) piecePointsPx(engine, placement.piece, placement.turn, placement.mirrored, centrePx, scale) else restPoints
    val colour = PlayPalette.piece(placement.piece).let { if (popTint > 0f) androidx.compose.ui.graphics.lerp(it, PlayPalette.board, popTint) else it }
    PolygonNode(PlayTags.boardPiece(placement.piece), points, colour) {
        piece = placement.piece
        turn = placement.turn
        mirrored = placement.mirrored
        centre = if (moving) mapping.toUnits(centrePx) else placement.centre
        drawnPxPerUnit = scale
    }
}

/** DI-1's shake keyframes (0 → −1 → +1 → −½ → 0 of the amplitude at 0, 25, 50, 75, 100 %), linear between them. */
private fun shakeOffset(t: Float): Float {
    val keys = floatArrayOf(0f, -1f, 1f, -0.5f, 0f)
    val x = (t.coerceIn(0f, 1f) * 4f)
    val i = x.toInt().coerceAtMost(3)
    val f = x - i
    return keys[i] + (keys[i + 1] - keys[i]) * f
}

/** The tray strip: a flat band at the bottom of the screen. */
@Composable
private fun TrayStrip(slot: Slot) {
    Box(Modifier.slot(slot).testTag(PlayTags.TRAY).drawBehind { drawRect(PlayPalette.tray) })
}

/** One tray cell (S-6): a region of the flat strip, no drawing of its own. */
@Composable
private fun TrayCell(id: PieceId, slot: Slot) {
    Box(Modifier.slot(slot).testTag(PlayTags.trayCell(id)).semantics { piece = id })
}

/**
 * The px corners of [id]'s miniature at the tray scale, its shape's box centred in its cell with whole-px
 * rounding (REQ-012, REQ-013; decisions.md row of TASK-012), or null when the puzzle has no cell for it.
 */
internal fun trayMiniaturePx(engine: ILockEngine, id: PieceId, turnNow: Turn, mirroredNow: Boolean, g: ScreenGeometry): List<Offset>? {
    val cell = g.cells[id] ?: return null
    val scale = g.trayPxPerUnit
    val corners = engine.vertices(id, turnNow, mirroredNow, Point(0.0, 0.0))
    val minX = corners.minOf { it.x }
    val minY = corners.minOf { it.y }
    val width = ((corners.maxOf { it.x } - minX) * scale).toFloat()
    val height = ((corners.maxOf { it.y } - minY) * scale).toFloat()
    val left = (cell.leftPx + (cell.width - width) / 2).roundToInt().toFloat()
    val top = (cell.topPx + (cell.height - height) / 2).roundToInt().toFloat()
    return corners.map { Offset(left + ((it.x - minX) * scale).toFloat(), top + ((it.y - minY) * scale).toFloat()) }
}

/**
 * A miniature at the tray scale, its shape's box centred in its cell (REQ-012, REQ-013). A piece that just
 * went home glides back from where the dragged piece was, shrinking to the tray scale, over [GLIDE_MS]
 * (REQ-020 rule); its keys follow the drawing in each frame.
 */
@Composable
private fun TrayPiece(controller: PlayController, id: PieceId, turnNow: Turn, mirroredNow: Boolean, g: ScreenGeometry, visual: DragVisual) {
    val cell = g.cells[id] ?: return
    val engine = controller.engine
    val scale = g.trayPxPerUnit
    val restPoints = remember(id, turnNow, mirroredNow, scale, cell) {
        trayMiniaturePx(engine, id, turnNow, mirroredNow, g) ?: emptyList()
    }
    if (restPoints.isEmpty()) return
    val glide = visual.glide?.takeIf { it.piece == id }
    var glideProgress by remember(glide) { mutableFloatStateOf(if (glide == null) 1f else 0f) }
    LaunchedEffect(glide) {
        if (glide != null) {
            onFrames(GLIDE_MS) { glideProgress = it }
            visual.glideDone(glide)
        }
    }
    val k = easeOutCubic(glideProgress)
    val points: List<Offset>
    val drawnScale: Float
    if (glide != null && k < 1f) {
        val centrePx = lerp(glide.fromCentrePx, centroid(restPoints), k)
        drawnScale = lerpF(glide.fromScale, scale, k)
        points = piecePointsPx(engine, id, turnNow, mirroredNow, centrePx, drawnScale)
    } else {
        points = restPoints
        drawnScale = scale
    }
    PolygonNode(PlayTags.trayPiece(id), points, PlayPalette.piece(id)) {
        piece = id
        turn = turnNow
        mirrored = mirroredNow
        drawnPxPerUnit = drawnScale
    }
}
