// Acceptance kit (WO-003, TASK-011): kit-prefixed helpers for the Compose device tests of `PlayScreen`
// (tasks.md § WO-003 › Seams S-1…S-14, the tag table and the touch routing; workorders/WO-003-briefs.md
// § TASK-011 "How to drive the screen"). No token lives in this file.
//
// How the kit drives the screen:
// - every touch is injected on the `play-screen` node, in that node's own px; puzzle points convert
//   through `BoardMappingKey` on `board`; cell and piece positions are node bounds relative to `play-screen`;
// - time is the compose rule's `mainClock` with `autoAdvance` off, advanced frame by frame — never a sleep;
// - the controller is driven on the main thread only (`runOnIdle`);
// - pixels come from `captureToImage()` on `board` or `tray`, read against the geometry (the solution
//   polygons through `BoardMapping`, `ILockEngine.vertices` through the node's keys) before a drawing is blamed.
package app.tangramplaytime.play

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.contracts.ArtShape
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.Turn
import app.tangramplaytime.kernel.LockEngine
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ── The shared fixtures: the real engine, the bundled puzzles, the S-5 and S-11 literals ─────────────

/** The one lock rule (WO § Constraints "Seams": the real `LockEngine` as a test dependency). */
internal val kitEngine: ILockEngine by lazy { LockEngine() }

/** The bundled puzzle files (`Tangrams`, `*.json`) read on the device through `:content` (brief § Inputs). Fixed for the process. */
internal val kitLibrary: IPuzzleLibrary by lazy { PuzzleLibraries.bundled(kitEngine) }

/** S-5, the phone value, written as a literal (the kits write it, `:app` has its constant). */
internal val kitPhoneLayout: PlayLayout = PlayLayout(
    trayRows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    ),
    floatOffsetDp = 30f,
    cellGapDp = 8f,
    sideMarginDp = 8f,
    topBarDp = 66f,
)

/** S-11 kit literal. */
internal val kitTexts: PlayTexts = PlayTexts(sizeSmall = "S", sizeMedium = "M", sizeLarge = "L", titleLanguage = "en")

/** S-1: a fresh controller over the real engine and the bundled puzzles — the fresh start. */
internal fun kitController(onEvent: (PlayEvent) -> Unit = {}): PlayController = PlayController(kitLibrary, kitEngine, onEvent)

internal fun kitFirst(kind: PuzzleKind): PuzzleDefinition = kitLibrary.puzzles.first { it.kind == kind }

/** Every puzzle that uses all seven pieces (full and warm-up; the validity rules, the mini rule). */
internal fun kitFullSetPuzzles(): List<PuzzleDefinition> = kitLibrary.puzzles.filter { it.kind != PuzzleKind.MINI }

internal const val KIT_TOL = 1e-6

// ── The screen under test ─────────────────────────────────────────────────────────────────────────────

/**
 * One `PlayScreen` under the compose rule, with the touch, node, time and pixel helpers the kit uses.
 * `show()` composes it with the clock auto-advancing (first layout), then turns `autoAdvance` off: from
 * then on every test step advances frames itself.
 */
internal class KitScreen(
    val rule: ComposeContentTestRule,
    val controller: PlayController = kitController(),
) {
    val density: Float get() = rule.density.density

    fun dp(v: Float): Float = v * density

    fun show(reducedMotion: Boolean = false) {
        rule.mainClock.autoAdvance = true
        rule.setContent {
            PlayScreen(controller, kitPhoneLayout, kitTexts, reducedMotion, Modifier.fillMaxSize())
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        frames(2)
        check(exists(PlayTags.SCREEN)) { "kit: no `play-screen` node after setContent" }
        check(onMain { controller.boardDpPerUnit } > 0.0) { "kit: boardDpPerUnit not set by the first layout" }
    }

    /**
     * Recreates the host activity and shows the screen again over the same [controller] (code review F2). The
     * recreation is the real lifecycle — `ActivityScenario.recreate()`: pause, stop, save the instance state,
     * destroy, then a new activity created from the saved state — and the new activity is given `PlayScreen`
     * exactly as [show] gave it to the first one, through a `ComposeView` of the default id (the saved state of
     * the first composition is restored into it). That is what a rotation or any configuration change does to
     * the app: `MainActivity` shows `PlayScreen(AppGraph.controller, …)` in every new activity, and `AppGraph`'s
     * controller lives once per process and outlives both (S-15). The new composition runs on the rule's
     * `mainClock` (the test environment's recomposer serves every window of the test) and the clock stays
     * paused, so the caller sees every frame of the new composition from its first one.
     */
    fun <A : ComponentActivity> recreate(scenario: ActivityScenario<A>, reducedMotion: Boolean = false) {
        check(!rule.mainClock.autoAdvance) { "kit: recreate() expects the paused clock of show()" }
        scenario.recreate()
        scenario.onActivity { activity ->
            activity.setContentView(
                ComposeView(activity).apply {
                    setContent { PlayScreen(controller, kitPhoneLayout, kitTexts, reducedMotion, Modifier.fillMaxSize()) }
                },
            )
        }
        rule.waitForIdle()
        check(exists(PlayTags.SCREEN)) { "kit: no `play-screen` node after the recreation" }
        check(count(PlayTags.SCREEN) == 1) { "kit: ${count(PlayTags.SCREEN)} `play-screen` nodes after the recreation (the old activity's screen is still attached)" }
    }

    /** A kit precondition for a drop that must lock: the finger (not the floating piece) is over the board. */
    fun checkOverBoard(finger: Offset) {
        check(bounds(PlayTags.BOARD).contains(finger)) { "kit: the finger $finger is not over the board ${bounds(PlayTags.BOARD)}; pick a higher piece" }
    }

    // ── time ──

    fun frames(n: Int = 1) {
        repeat(n) { rule.mainClock.advanceTimeByFrame() }
        rule.waitForIdle()
    }

    fun advance(ms: Long) {
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    fun <T> onMain(block: () -> T): T = rule.runOnIdle(block)

    /** S-2 `open` on the main thread, then two frames. */
    fun open(id: String) {
        onMain { check(controller.open(id)) { "kit: open($id) refused" } }
        frames(2)
    }

    val puzzle: PuzzleDefinition get() = onMain { controller.puzzle }

    // ── nodes ──

    fun interaction(tag: String): SemanticsNodeInteraction = rule.onNodeWithTag(tag, useUnmergedTree = true)

    fun node(tag: String): SemanticsNode = interaction(tag).fetchSemanticsNode("kit: node `$tag`")

    fun exists(tag: String): Boolean = rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    fun count(tag: String): Int = rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    fun nodes(tag: String): List<SemanticsNode> = rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()

    fun screenOrigin(): Offset = node(PlayTags.SCREEN).boundsInRoot.topLeft

    /** The node's bounds in `play-screen` px — the space every touch is injected in. */
    fun bounds(tag: String): Rect = node(tag).boundsInRoot.translate(-screenOrigin())

    fun <T> key(tag: String, k: SemanticsPropertyKey<T>): T =
        node(tag).config.getOrNull(k) ?: throw AssertionError("kit: `$tag` carries no ${k.name}")

    fun <T> keyOrNull(tag: String, k: SemanticsPropertyKey<T>): T? = node(tag).config.getOrNull(k)

    /** The merged text of a merged, inert node (the title, a size mark). */
    fun text(tag: String): String {
        val n = rule.onNodeWithTag(tag).fetchSemanticsNode("kit: merged node `$tag`")
        return n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
    }

    fun mapping(): BoardMapping = key(PlayTags.BOARD, PlaySemantics.BoardMappingKey)

    fun toPx(p: Point): Offset = mapping().toPx(p)

    fun toUnits(o: Offset): Point = mapping().toUnits(o)

    fun pxPerUnit(): Float = mapping().pxPerUnit

    /** The tags of every node and the texts / content descriptions of every node, unmerged tree, sorted. */
    fun tagsAndTexts(): Pair<List<String>, List<String>> {
        val tags = ArrayList<String>()
        val texts = ArrayList<String>()
        fun walk(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.TestTag)?.let { tags += it }
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { texts += it.text }
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { texts += it }
            n.children.forEach { walk(it) }
        }
        walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        return tags.sorted() to texts.sorted()
    }

    /** Which of the three homes (tray, board, dragged) hold [piece] right now (tag table). */
    fun homesOf(piece: PieceId): List<String> = listOf(
        PlayTags.trayPiece(piece), PlayTags.boardPiece(piece), PlayTags.dragPiece(piece),
    ).filter { exists(it) }

    // ── geometry read from the nodes ──

    /** A board or drag piece's corners in `play-screen` px, from its keys through the engine (O-01). */
    fun boardPolygonPx(tag: String): List<Offset> {
        val piece = key(tag, PlaySemantics.PieceKey)
        val turn = key(tag, PlaySemantics.TurnKey)
        val mirrored = key(tag, PlaySemantics.MirroredKey)
        val centre = key(tag, PlaySemantics.CentreKey)
        return kitEngine.vertices(piece, turn, mirrored, centre).map { toPx(it) }
    }

    /** A tray piece's corners in `play-screen` px: the shape at its keys, fitted into the node's box. */
    fun trayPolygonPx(piece: PieceId): List<Offset> {
        val tag = PlayTags.trayPiece(piece)
        val turn = key(tag, PlaySemantics.TurnKey)
        val mirrored = key(tag, PlaySemantics.MirroredKey)
        val scale = key(tag, PlaySemantics.DrawnPxPerUnitKey)
        val box = bounds(tag)
        val v = kitEngine.vertices(piece, turn, mirrored, Point(0.0, 0.0))
        val minX = v.minOf { it.x }
        val minY = v.minOf { it.y }
        return v.map { Offset(box.left + ((it.x - minX) * scale).toFloat(), box.top + ((it.y - minY) * scale).toFloat()) }
    }

    /**
     * How far (px) the drawn piece [tag] is from a rest pose: its CentreKey from [rest] and each edge of its box
     * from [restBox] (tag table: a piece's bounds and CentreKey follow the drawing in each frame).
     */
    fun displacementPx(tag: String, rest: Point, restBox: Rect): Float {
        val c = toPx(key(tag, PlaySemantics.CentreKey)) - toPx(rest)
        val b = bounds(tag)
        return maxOf(hypot(c.x, c.y), abs(b.left - restBox.left), abs(b.top - restBox.top), abs(b.right - restBox.right), abs(b.bottom - restBox.bottom))
    }

    /** The unit extent of [piece] at [turn]/[mirrored]: width × height of its corner box. */
    fun unitExtent(piece: PieceId, turn: Turn, mirrored: Boolean): Pair<Double, Double> {
        val v = kitEngine.vertices(piece, turn, mirrored, Point(0.0, 0.0))
        return (v.maxOf { it.x } - v.minOf { it.x }) to (v.maxOf { it.y } - v.minOf { it.y })
    }

    // ── touch ──

    fun touch(block: TouchInjectionScope.() -> Unit) {
        rule.onNodeWithTag(PlayTags.SCREEN, useUnmergedTree = true).performTouchInput(block)
    }

    /** A tap: down and up at [at] with no movement (the tap rule: under 12 dp of movement, whatever its length). */
    fun tap(at: Offset) {
        touch { down(at); up() }
        frames()
    }

    /**
     * A point inside [piece]'s tray cell that is not inside the tray badge's 60 dp area (the badge branch of
     * the touch routing wins inside the PG cell; decisions.md "badge touch precedence").
     */
    fun trayTapPoint(piece: PieceId): Offset {
        val cell = bounds(PlayTags.trayCell(piece))
        val avoid = if (piece == PieceId.PG && exists(PlayTags.FLIP_BADGE_TRAY)) bounds(PlayTags.FLIP_BADGE_TRAY) else null
        val candidates = listOf(
            cell.center,
            Offset(cell.left + cell.width * 0.25f, cell.top + cell.height * 0.75f),
            Offset(cell.left + cell.width * 0.75f, cell.top + cell.height * 0.75f),
            Offset(cell.left + cell.width * 0.25f, cell.top + cell.height * 0.25f),
            Offset(cell.left + cell.width * 0.75f, cell.top + cell.height * 0.25f),
        )
        return candidates.firstOrNull { avoid == null || !avoid.contains(it) }
            ?: throw AssertionError("kit: the badge area $avoid covers every tap point of the PG cell $cell")
    }

    /** Taps [piece] in the tray [times] times (the tap-to-turn rule: one step clockwise per tap). */
    fun tapTray(piece: PieceId, times: Int) {
        repeat(times) {
            tap(trayTapPoint(piece))
            advance(48)
        }
    }

    /**
     * Picks [piece] up from its tray cell by touch: down in the cell, a 20 dp move (past the 12 dp slop, so
     * the touch is a drag), then the 120 ms grow (the drag rule) plus one frame. Returns the finger position.
     */
    fun pickUpFromTray(piece: PieceId): Offset {
        val start = trayTapPoint(piece)
        touch { down(start) }
        frames()
        val lifted = Offset(start.x, start.y - dp(20f))
        touch { moveTo(lifted) }
        frames()
        advance(120)
        frames()
        check(exists(PlayTags.dragPiece(piece))) { "kit: no `drag-piece` for $piece after a 20 dp move from its cell" }
        return lifted
    }

    /** Picks a placed [piece] up by touch at its drawn centre, moving 20 dp (past the slop). */
    fun pickUpFromBoard(piece: PieceId): Offset {
        val start = toPx(key(PlayTags.boardPiece(piece), PlaySemantics.CentreKey))
        touch { down(start) }
        frames()
        val lifted = Offset(start.x, start.y - dp(20f))
        touch { moveTo(lifted) }
        frames()
        check(exists(PlayTags.dragPiece(piece))) { "kit: no `drag-piece` for $piece after a 20 dp move from its centre" }
        return lifted
    }

    /** Moves the finger from [from] to [to] in [steps] moves, one frame each. Returns [to]. */
    fun moveFinger(from: Offset, to: Offset, steps: Int = 4): Offset {
        for (i in 1..steps) {
            val f = Offset(from.x + (to.x - from.x) * i / steps, from.y + (to.y - from.y) * i / steps)
            touch { moveTo(f) }
            frames()
        }
        return to
    }

    /**
     * Steers the dragged [piece] so that its drawn centre lands on [target] (puzzle units), moving the finger
     * by the difference between the piece's `CentreKey` and the target (the piece floats above the finger
     * at a constant offset, the floating rule of the drag). Returns the finger position.
     */
    fun steerDragTo(piece: PieceId, target: Point, finger: Offset, steps: Int = 6): Offset {
        var f = finger
        repeat(4) {
            val cur = toPx(key(PlayTags.dragPiece(piece), PlaySemantics.CentreKey))
            val tgt = toPx(target)
            val d = tgt - cur
            if (hypot(d.x, d.y) < 0.75f) return f
            f = moveFinger(f, f + d, steps)
            frames()
        }
        val cur = toPx(key(PlayTags.dragPiece(piece), PlaySemantics.CentreKey))
        val off = cur - toPx(target)
        check(hypot(off.x, off.y) <= 1.5f) { "kit: could not steer $piece onto $target (off by $off px)" }
        return f
    }

    fun release() {
        touch { up() }
        frames()
    }

    // ── the controller through S-2 (main thread) ──

    /**
     * Places [piece] at its stored solution pose through S-2 — a set-up, never a gesture (brief: "Set a board
     * up through S-2 only where the line is not about the gesture"). Returns false, with nothing changed,
     * when the engine says that pose does not lock on the current board (the fixture reads the geometry).
     */
    fun placeByApi(piece: PieceId): Boolean = onMain {
        val p = controller.puzzle
        val sp = p.solution.single { it.piece == piece }
        val pose = kitPoseOf(piece, sp.polygon)
        val lock = kitEngine.findLock(p, controller.placed, piece, pose.turn, pose.mirrored, pose.centre, controller.boardDpPerUnit)
        if (lock !is LockResult.Locked) return@onMain false
        if (piece == PieceId.PG && controller.trayMirrored(PieceId.PG) != pose.mirrored) {
            check(controller.pressBadge()) { "kit: pressBadge() refused with PG in the tray" }
        }
        check(controller.startDrag(piece, pose.centre)) { "kit: startDrag($piece) refused" }
        val from = controller.dragged?.turn?.step ?: error("kit: no drag after startDrag")
        val steps = ((pose.turn.step - from) % 8 + 8) % 8
        if (steps != 0) controller.twistDrag(if (steps <= 4) steps else steps - 8)
        controller.moveDrag(pose.centre, overBoard = true)
        controller.updatePreview()
        controller.drop() != null
    }

    /** Places every piece of the open puzzle at its solution pose (edge-first, as the engine allows). */
    fun solveByApi() {
        val p = puzzle
        var remaining = p.pieces.toList()
        while (remaining.isNotEmpty()) {
            val placedNow = remaining.filter { placeByApi(it) }
            check(placedNow.isNotEmpty()) { "kit: none of $remaining locks at its solution pose in ${p.id}" }
            remaining = remaining - placedNow.toSet()
            frames()
        }
    }

    /** S-2 `placed`, read on the main thread. */
    fun placed(): List<Placement> = onMain { controller.placed.toList() }

    // ── pixels ──

    fun capture(tag: String): PixelMap = interaction(tag).captureToImage().toPixelMap()

    /** A `play-screen` px point in the coordinates of [tag]'s capture. */
    fun toCapture(tag: String, p: Offset): Offset = p - bounds(tag).topLeft

    /** The silhouette's solution polygons in the `board` capture's px. */
    fun solutionPolygonsInBoardPx(p: PuzzleDefinition = puzzle): List<List<Offset>> {
        val origin = bounds(PlayTags.BOARD).topLeft
        return p.solution.map { sp -> sp.polygon.map { toPx(it) - origin } }
    }

    /** The silhouette mask in the `board` capture's px (each solution polygon widened by 0.75 px). */
    fun silhouetteMask(p: PuzzleDefinition = puzzle): KitMask {
        val b = bounds(PlayTags.BOARD)
        return kitRasterise(solutionPolygonsInBoardPx(p), b.width.roundToInt(), b.height.roundToInt())
    }
}

// ── Puzzle geometry (the kit's own reading of the solution; px only through BoardMapping) ─────────────

internal data class KitPose(val turn: Turn, val mirrored: Boolean, val centre: Point)

/** The centre of a polygon: the average of its corners (TYPE-003; `Placement.centre`). */
internal fun kitCentroid(poly: List<Point>): Point = Point(poly.sumOf { it.x } / poly.size, poly.sumOf { it.y } / poly.size)

/** The same piece, turn and mirror, and centres within [tol] units (a placement compared as the engine returns it). */
internal fun kitSamePlacement(a: Placement?, b: Placement?, tol: Double = KIT_TOL): Boolean =
    a != null && b != null && a.piece == b.piece && a.turn == b.turn && a.mirrored == b.mirrored &&
        abs(a.centre.x - b.centre.x) <= tol && abs(a.centre.y - b.centre.y) <= tol

internal fun kitSamePointSet(a: List<Point>, b: List<Point>, tol: Double = KIT_TOL): Boolean =
    a.size == b.size &&
        a.all { p -> b.any { q -> abs(p.x - q.x) <= tol && abs(p.y - q.y) <= tol } } &&
        b.all { q -> a.any { p -> abs(p.x - q.x) <= tol && abs(p.y - q.y) <= tol } }

/** The (turn, mirror) at which `ILockEngine.vertices` reproduces a stored solution polygon. */
internal fun kitPoseOf(piece: PieceId, polygon: List<Point>): KitPose {
    val centre = kitCentroid(polygon)
    for (mirrored in listOf(false, true)) {
        for (step in 0..7) {
            val v = kitEngine.vertices(piece, Turn(step), mirrored, centre)
            if (kitSamePointSet(v, polygon)) return KitPose(Turn(step), mirrored, centre)
        }
    }
    throw AssertionError("kit: no turn and mirror of $piece reproduces its solution polygon $polygon")
}

internal fun kitDistToSegment(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
    val dx = bx - ax
    val dy = by - ay
    val len2 = dx * dx + dy * dy
    val t = if (len2 == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
    return hypot(px - (ax + t * dx), py - (ay + t * dy))
}

internal fun kitDistToBoundary(p: Point, poly: List<Point>): Double {
    var best = Double.MAX_VALUE
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        best = min(best, kitDistToSegment(p.x, p.y, a.x, a.y, b.x, b.y))
    }
    return best
}

/** Inside the polygon (even-odd ray test), or within [tol] of its boundary. */
internal fun kitInside(p: Point, poly: List<Point>, tol: Double = 0.0): Boolean {
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[j]
        if ((a.y > p.y) != (b.y > p.y)) {
            val x = a.x + (p.y - a.y) * (b.x - a.x) / (b.y - a.y)
            if (p.x < x) inside = !inside
        }
        j = i
    }
    if (inside) return true
    return tol > 0.0 && kitDistToBoundary(p, poly) <= tol
}

internal fun kitInsideAny(p: Point, polys: List<List<Point>>, tol: Double = 0.0): Boolean = polys.any { kitInside(p, it, tol) }

/** The area-weighted centre of the silhouette (the union of the solution polygons). */
internal fun kitSilhouetteCentre(p: PuzzleDefinition): Point {
    var area = 0.0
    var cx = 0.0
    var cy = 0.0
    for (sp in p.solution) {
        val poly = sp.polygon
        var a = 0.0
        var x = 0.0
        var y = 0.0
        for (i in poly.indices) {
            val p0 = poly[i]
            val p1 = poly[(i + 1) % poly.size]
            val cross = p0.x * p1.y - p1.x * p0.y
            a += cross
            x += (p0.x + p1.x) * cross
            y += (p0.y + p1.y) * cross
        }
        a /= 2.0
        if (abs(a) < 1e-12) continue
        x /= 6.0 * a
        y /= 6.0 * a
        val w = abs(a)
        area += w
        cx += x * w
        cy += y * w
    }
    return Point(cx / area, cy / area)
}

/**
 * A point in the middle of the silhouette where [piece] at [turn]/[mirrored] finds no lock (the engine
 * decides, TYPE-004): the area centre, or the nearest probe on a 0.1-unit spiral around it that is still
 * inside the silhouette. Fails when every probe within one unit locks.
 */
internal fun kitMiddleWithNoLock(p: PuzzleDefinition, piece: PieceId, turn: Turn, mirrored: Boolean, dpPerUnit: Double): Point {
    val centre = kitSilhouetteCentre(p)
    val polys = p.solution.map { it.polygon }
    val probes = ArrayList<Point>()
    probes += centre
    var r = 0.1
    while (r <= 1.0 + 1e-9) {
        var deg = 0.0
        while (deg < 360.0) {
            val rad = Math.toRadians(deg)
            probes += Point(centre.x + r * cos(rad), centre.y + r * sin(rad))
            deg += 30.0
        }
        r += 0.1
    }
    for (q in probes) {
        if (!kitInsideAny(q, polys)) continue
        val lock = kitEngine.findLock(p, emptyList(), piece, turn, mirrored, q, dpPerUnit)
        if (lock is LockResult.None) return q
    }
    throw AssertionError("kit: every probe around the middle of ${p.id} locks $piece; the middle-drop fixture needs another piece")
}

internal data class KitSeg(val a: Point, val b: Point)

/** The edges two different solution pieces share (collinear overlaps): where a piece boundary would show. */
internal fun kitInternalEdges(p: PuzzleDefinition): List<KitSeg> {
    val out = ArrayList<KitSeg>()
    val polys = p.solution.map { it.polygon }
    for (i in polys.indices) for (j in i + 1 until polys.size) {
        val pi = polys[i]
        val pj = polys[j]
        for (ei in pi.indices) {
            val a = pi[ei]
            val b = pi[(ei + 1) % pi.size]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = hypot(dx, dy)
            if (len < KIT_TOL) continue
            for (ej in pj.indices) {
                val c = pj[ej]
                val d = pj[(ej + 1) % pj.size]
                val distC = abs((c.x - a.x) * dy - (c.y - a.y) * dx) / len
                val distD = abs((d.x - a.x) * dy - (d.y - a.y) * dx) / len
                if (distC > KIT_TOL || distD > KIT_TOL) continue
                val tc = ((c.x - a.x) * dx + (c.y - a.y) * dy) / (len * len)
                val td = ((d.x - a.x) * dx + (d.y - a.y) * dy) / (len * len)
                val t0 = max(0.0, min(tc, td))
                val t1 = min(1.0, max(tc, td))
                if (t1 - t0 <= KIT_TOL) continue
                out += KitSeg(Point(a.x + t0 * dx, a.y + t0 * dy), Point(a.x + t1 * dx, a.y + t1 * dy))
            }
        }
    }
    return out
}

/** Points along [seg] every [stepUnits], excluding its ends. */
internal fun kitSamplesAlong(seg: KitSeg, stepUnits: Double): List<Point> {
    val len = hypot(seg.b.x - seg.a.x, seg.b.y - seg.a.y)
    val n = max(2, ceil(len / stepUnits).toInt())
    return (1 until n).map { k ->
        val t = k.toDouble() / n
        Point(seg.a.x + (seg.b.x - seg.a.x) * t, seg.a.y + (seg.b.y - seg.a.y) * t)
    }
}

/** The unit normal of [seg] (either side). */
internal fun kitNormal(seg: KitSeg): Point {
    val dx = seg.b.x - seg.a.x
    val dy = seg.b.y - seg.a.y
    val len = hypot(dx, dy)
    return Point(-dy / len, dx / len)
}

// ── The picture's own ink (so an art line on a piece edge is not read as a piece boundary) ────────────

internal data class KitInk(val a: Point, val b: Point, val halfWidth: Double)

private fun kitPolyline(points: List<Point>, closed: Boolean, hw: Double): List<KitInk> {
    val out = ArrayList<KitInk>()
    for (i in 0 until points.size - 1) out += KitInk(points[i], points[i + 1], hw)
    if (closed && points.size > 2) out += KitInk(points.last(), points.first(), hw)
    return out
}

private fun kitEllipsePoints(c: Point, rx: Double, ry: Double): List<Point> =
    (0 until 48).map { k -> val t = 2 * Math.PI * k / 48; Point(c.x + rx * cos(t), c.y + ry * sin(t)) }

private fun kitFlattenPath(d: String): List<List<Point>> {
    val tokens = Regex("[MLQCZmlqcz]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?").findAll(d).map { it.value }.toList()
    val polylines = ArrayList<List<Point>>()
    var current = ArrayList<Point>()
    var i = 0
    fun num(): Double = tokens[i++].toDouble()
    var cmd = 'M'
    while (i < tokens.size) {
        val t = tokens[i]
        if (t.length == 1 && t[0].isLetter()) { cmd = t[0].uppercaseChar(); i++; if (cmd == 'Z') { if (current.size > 1) { current.add(current.first()); polylines += current }; current = ArrayList(); continue } }
        when (cmd) {
            'M' -> { if (current.size > 1) polylines += current; current = ArrayList(); current += Point(num(), num()); cmd = 'L' }
            'L' -> current += Point(num(), num())
            'Q' -> {
                val p0 = current.lastOrNull() ?: Point(0.0, 0.0)
                val c = Point(num(), num()); val p1 = Point(num(), num())
                for (k in 1..16) { val s = k / 16.0; val u = 1 - s; current += Point(u * u * p0.x + 2 * u * s * c.x + s * s * p1.x, u * u * p0.y + 2 * u * s * c.y + s * s * p1.y) }
            }
            'C' -> {
                val p0 = current.lastOrNull() ?: Point(0.0, 0.0)
                val c1 = Point(num(), num()); val c2 = Point(num(), num()); val p1 = Point(num(), num())
                for (k in 1..16) { val s = k / 16.0; val u = 1 - s; current += Point(u * u * u * p0.x + 3 * u * u * s * c1.x + 3 * u * s * s * c2.x + s * s * s * p1.x, u * u * u * p0.y + 3 * u * u * s * c1.y + 3 * u * s * s * c2.y + s * s * s * p1.y) }
            }
            else -> i++
        }
    }
    if (current.size > 1) polylines += current
    return polylines
}

/** Every boundary and stroke of the picture as segments in puzzle units with a half-width. */
internal fun kitArtInk(art: ArtLayer): List<KitInk> {
    val out = ArrayList<KitInk>()
    for (s in art.shapes) {
        val hw = if (s.style.stroke != null) (s.style.width ?: 0.0) / 2 else 0.0
        when (s) {
            is ArtShape.Polygon -> out += kitPolyline(s.points, closed = true, hw = hw)
            is ArtShape.Rect -> {
                val pts = listOf(Point(s.x, s.y), Point(s.x + s.w, s.y), Point(s.x + s.w, s.y + s.h), Point(s.x, s.y + s.h))
                out += kitPolyline(pts, closed = true, hw = hw)
                val rx = s.rx ?: 0.0
                if (rx > 0) { // the rounded corners cut inside the sharp corner by up to rx
                    for (corner in pts) out += kitPolyline(kitEllipsePoints(corner, rx, rx), closed = true, hw = hw)
                }
            }
            is ArtShape.Circle -> out += kitPolyline(kitEllipsePoints(s.c, s.r, s.r), closed = true, hw = hw)
            is ArtShape.Ellipse -> out += kitPolyline(kitEllipsePoints(s.c, s.rx, s.ry), closed = true, hw = hw)
            is ArtShape.Line -> out += KitInk(s.from, s.to, hw)
            is ArtShape.Path -> for (pl in kitFlattenPath(s.d)) out += kitPolyline(pl, closed = false, hw = hw)
        }
    }
    return out
}

/** Distance in units from [p] to the nearest ink edge (negative inside a stroke's width). */
internal fun kitInkDistance(ink: List<KitInk>, p: Point): Double {
    var best = Double.MAX_VALUE
    for (s in ink) best = min(best, kitDistToSegment(p.x, p.y, s.a.x, s.a.y, s.b.x, s.b.y) - s.halfWidth)
    return best
}

// ── Pixel masks and colours ───────────────────────────────────────────────────────────────────────────

internal class KitMask(val w: Int, val h: Int, val bits: BooleanArray = BooleanArray(w * h)) {
    operator fun get(x: Int, y: Int): Boolean = x in 0 until w && y in 0 until h && bits[y * w + x]
    operator fun set(x: Int, y: Int, v: Boolean) { bits[y * w + x] = v }
    fun count(): Int = bits.count { it }
    fun not(): KitMask = KitMask(w, h, BooleanArray(w * h) { !bits[it] })

    /** Keeps a pixel only when every pixel of the (2r+1)² square around it is set (the border counts as unset). */
    fun erode(r: Int): KitMask {
        val tmp = KitMask(w, h)
        for (y in 0 until h) for (x in 0 until w) {
            var all = true
            var i = x - r
            while (all && i <= x + r) { if (!this[i, y]) all = false; i++ }
            tmp[x, y] = all
        }
        val out = KitMask(w, h)
        for (y in 0 until h) for (x in 0 until w) {
            var all = true
            var j = y - r
            while (all && j <= y + r) { if (!tmp[x, j]) all = false; j++ }
            out[x, y] = all
        }
        return out
    }

    fun dilate(r: Int): KitMask = not().erode(r).not()

    fun forEachSet(block: (Int, Int) -> Unit) { for (y in 0 until h) for (x in 0 until w) if (bits[y * w + x]) block(x, y) }
}

/** The pixels whose centres lie inside any polygon or within [expandPx] of its boundary. */
internal fun kitRasterise(polys: List<List<Offset>>, w: Int, h: Int, expandPx: Float = 0.75f): KitMask {
    val m = KitMask(w, h)
    for (poly in polys) {
        val pts = poly.map { Point(it.x.toDouble(), it.y.toDouble()) }
        val x0 = max(0, floor(poly.minOf { it.x } - expandPx - 1).toInt())
        val x1 = min(w - 1, ceil(poly.maxOf { it.x } + expandPx + 1).toInt())
        val y0 = max(0, floor(poly.minOf { it.y } - expandPx - 1).toInt())
        val y1 = min(h - 1, ceil(poly.maxOf { it.y } + expandPx + 1).toInt())
        for (y in y0..y1) for (x in x0..x1) {
            if (!m[x, y] && kitInside(Point(x + 0.5, y + 0.5), pts, expandPx.toDouble())) m[x, y] = true
        }
    }
    return m
}

internal fun kitNear(a: Color, b: Color, tol: Float = 10f / 255f): Boolean =
    abs(a.red - b.red) <= tol && abs(a.green - b.green) <= tol && abs(a.blue - b.blue) <= tol

internal fun kitHex(c: Color): String = "#%02X%02X%02X".format((c.red * 255).roundToInt(), (c.green * 255).roundToInt(), (c.blue * 255).roundToInt())

internal fun kitColour(hex: String): Color {
    val v = hex.removePrefix("#").toLong(16)
    return Color(((v shr 16) and 0xFF).toInt(), ((v shr 8) and 0xFF).toInt(), (v and 0xFF).toInt())
}

/** Every colour the picture names: its base, fills and strokes. */
internal fun kitArtColours(art: ArtLayer): List<Color> {
    val out = ArrayList<Color>()
    out += kitColour(art.base)
    for (s in art.shapes) {
        s.style.fill?.let { out += kitColour(it) }
        s.style.stroke?.let { out += kitColour(it) }
    }
    return out
}

internal fun kitPieceColours(): Map<PieceId, Color> = PieceId.entries.associateWith { PlayPalette.piece(it) }

/**
 * A pixel inside [poly] (capture px), at least [marginPx] from its boundary, outside [avoid] and — when
 * [within] is given — inside it (the captured area: a sample outside the kit's own bitmap is never read).
 */
internal fun kitInteriorSamples(poly: List<Offset>, marginPx: Float, avoid: List<Rect> = emptyList(), stepPx: Float = 3f, within: Rect? = null): List<Offset> {
    val pts = poly.map { Point(it.x.toDouble(), it.y.toDouble()) }
    val out = ArrayList<Offset>()
    var y = poly.minOf { it.y }
    while (y <= poly.maxOf { it.y }) {
        var x = poly.minOf { it.x }
        while (x <= poly.maxOf { it.x }) {
            val p = Point(x.toDouble(), y.toDouble())
            val o = Offset(x, y)
            val inCapture = within == null || (o.x >= within.left && o.x < within.right - 1f && o.y >= within.top && o.y < within.bottom - 1f)
            if (inCapture && kitInside(p, pts) && kitDistToBoundary(p, pts) >= marginPx && avoid.none { it.contains(o) }) out += o
            x += stepPx
        }
        y += stepPx
    }
    return out
}

/** Pixels inside [inPoly] and at least [marginPx] outside [notPoly] (and inside [within]): where the two poses differ. */
internal fun kitDifferenceSamples(inPoly: List<Offset>, notPoly: List<Offset>, marginPx: Float, avoid: List<Rect> = emptyList(), within: Rect? = null): List<Offset> {
    val notPts = notPoly.map { Point(it.x.toDouble(), it.y.toDouble()) }
    return kitInteriorSamples(inPoly, marginPx, avoid, within = within).filter { o ->
        val p = Point(o.x.toDouble(), o.y.toDouble())
        !kitInside(p, notPts) && kitDistToBoundary(p, notPts) >= marginPx
    }
}

internal fun kitBox(poly: List<Offset>): Rect = Rect(poly.minOf { it.x }, poly.minOf { it.y }, poly.maxOf { it.x }, poly.maxOf { it.y })

internal fun kitAssertBoxNear(what: String, expected: Rect, actual: Rect, tolPx: Float) {
    val ok = abs(expected.left - actual.left) <= tolPx && abs(expected.top - actual.top) <= tolPx &&
        abs(expected.right - actual.right) <= tolPx && abs(expected.bottom - actual.bottom) <= tolPx
    if (!ok) throw AssertionError("$what: expected box $expected, node bounds $actual (tolerance $tolPx px)")
}

internal fun kitAssertNear(what: String, expected: Double, actual: Double, tol: Double) {
    if (abs(expected - actual) > tol) throw AssertionError("$what: expected $expected, was $actual (tolerance $tol)")
}

internal fun kitAngleDeg(from: Offset, to: Offset): Double = Math.toDegrees(atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble()))

internal fun kitOnCircle(centre: Offset, radiusPx: Float, deg: Double): Offset {
    val rad = Math.toRadians(deg)
    return Offset(centre.x + (radiusPx * cos(rad)).toFloat(), centre.y + (radiusPx * sin(rad)).toFloat())
}

internal val KIT_SQRT2: Double = sqrt(2.0)
