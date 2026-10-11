// WO-001 TASK-002 · design §1 (the `findLock` search) and §2 rows `LockEngine`, `findLock`, `vertices`,
// `outlineCorners`, `isSolved`, `tryTurnInPlace`/`tryMirrorInPlace`/`stillValid`, `anchors` ·
// REQ-019, REQ-020 (seam half), REQ-022; TYPE-001, TYPE-003, TYPE-004; guardrails G-04, G-12, G-15.
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.Turn
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The one lock rule (guardrail G-04, O-01): piece geometry, the TYPE-004 lock search, the turn and
 * mirror in place, the reload check and the solved check, behind [ILockEngine].
 *
 * Stateless, with a no-argument constructor (design §2 `LockEngine`). It ports the prototype's
 * `verts`/`anchors`/`validAt`/`findLock` (ADR-001) and follows the TYPE-004 text where the prototype
 * differs: the candidate window is on |t|, not on the score (`design-inputs.md` §2 item 3), and the
 * locked centre is the anchor minus the corner's exact offset, never the prototype's `cx + tx`
 * (design §3 "Exact-anchor arithmetic").
 */
class LockEngine : ILockEngine {

    /**
     * REQ-019 anchors: the silhouette's outline corners, by the contiguity rule of [Silhouette]
     * (design §2 `outlineCorners`; review F1 — `shapes-warmup-4` includes (2,2)).
     */
    override fun outlineCorners(puzzle: PuzzleDefinition): List<Point> =
        Silhouette.outlineCorners(puzzle.solution)

    /**
     * `centre + PieceShapes.offsets(...)` (design §2 `vertices`): the TYPE-001 shape at a TYPE-003
     * turn about its centre, mirrored only for the parallelogram. Corner i is `Spec/03` §3 corner i,
     * transformed; the centre is the average of the corners, as in [Placement.centre].
     */
    override fun vertices(piece: PieceId, turn: Turn, mirrored: Boolean, centre: Point): List<Point> =
        PieceShapes.offsets(piece.type, turn, mirrored).map { o -> Point(centre.x + o.x, centre.y + o.y) }

    /**
     * The design §1 search (TYPE-004). Keeps the input [turn] and [mirrored] (REQ-019 rule; REQ-018
     * rule: the other mirror image is never tried). Returns [LockResult.None] when no candidate is
     * valid, a drop off the board included (REQ-020; G-15: a miss is a value, not an exception).
     *
     * @throws IllegalArgumentException when [dpPerUnit] is not finite or ≤ 0, or when [piece] is in
     *   [placed] — programming errors (G-15; decisions.md 2026-10-11, WO-001 / P3 design).
     */
    override fun findLock(
        puzzle: PuzzleDefinition,
        placed: List<Placement>,
        piece: PieceId,
        turn: Turn,
        mirrored: Boolean,
        dropCentre: Point,
        dpPerUnit: Double,
    ): LockResult {
        // Design §1 step 1: R = max(0.65, 30 / dpPerUnit) (TYPE-004). lockRadius carries the
        // dpPerUnit `require` (G-15).
        val r = LockRules.lockRadius(dpPerUnit)
        // G-15 (decisions.md 2026-10-11): `placed` holds the pieces *other than* the dropped one
        // (ILockEngine "Common terms"), so the dropped piece in `placed` is a caller bug.
        require(placed.none { it.piece == piece }) { "the dropped piece $piece must not be in placed" }

        val anchors = anchors(puzzle, placed)
        val offs = PieceShapes.offsets(piece.type, turn, mirrored)
        val placedPolys = placed.map { polygonOf(it) }

        // Step 2, TYPE-004: for every corner v (corner i) and anchor a (anchor j), t = a − v; keep the
        // pair when |t| ≤ R (+1e-9 slack). The candidate centre is the anchor's exact position minus
        // the corner's exact offset (TYPE-004; G-04), never dropCentre + t (design §3).
        val candidates = ArrayList<Candidate>()
        for (o in offs) {
            val vx = dropCentre.x + o.x
            val vy = dropCentre.y + o.y
            for (a in anchors) {
                val d = hypot(a.x - vx, a.y - vy)
                if (d <= r + LockRules.CMP) candidates.add(Candidate(d, Point(a.x - o.x, a.y - o.y)))
            }
        }

        // Step 3: a stable sort by d; equal d keeps generation order (corner i, then anchor j)
        // (`sortedBy` is stable; review F4).
        val sorted = candidates.sortedBy { it.d }

        var dMin: Double? = null
        var best: Candidate? = null
        var bestScore = 0.0
        for (c in sorted) {
            // Step 4, TYPE-004 window: only candidates with |t| ≤ (best |t|) + 0.16 are considered,
            // where best |t| (dMin) is the d of the first valid candidate.
            val first = dMin
            if (first != null && c.d > first + LockRules.WINDOW + LockRules.CMP) break
            val v = vertices(piece, turn, mirrored, c.centre)
            // REQ-019 statement and A2; TYPE-004 validity: inside the silhouette, over no placed piece.
            if (!isValid(v, puzzle, placedPolys)) continue
            if (first == null) dMin = c.d
            // Step 5, TYPE-004: score = |t| − 0.04 × (corners exactly on anchors); "exactly" is
            // within 1e-6 per coordinate (design §3 "Exact-anchor arithmetic": never `==`).
            val snug = v.count { corner -> anchors.any { near(it, corner) } }
            val score = c.d - LockRules.SNUG_BONUS * snug
            // The lowest score wins; a later candidate replaces the best only when lower by more
            // than 1e-9 (design §1 step 5; review F4 tie rule).
            if (best == null || score < bestScore - LockRules.CMP) {
                best = c
                bestScore = score
            }
        }

        // Step 6: the input turn and mirror are kept (REQ-019 rule).
        val winner = best ?: return LockResult.None
        return LockResult.Locked(Placement(piece, turn, mirrored, winner.centre))
    }

    /**
     * REQ-016 turn in place (design §1 wrapper): [findLock] for [current]'s piece at [newTurn], its
     * mirror unchanged, from [current]'s centre.
     */
    override fun tryTurnInPlace(
        puzzle: PuzzleDefinition,
        placed: List<Placement>,
        current: Placement,
        newTurn: Turn,
        dpPerUnit: Double,
    ): LockResult = findLock(puzzle, placed, current.piece, newTurn, current.mirrored, current.centre, dpPerUnit)

    /**
     * REQ-018 mirror in place (design §1 wrapper): [findLock] for [current]'s piece with the
     * opposite mirror state, its turn unchanged, from [current]'s centre.
     */
    override fun tryMirrorInPlace(
        puzzle: PuzzleDefinition,
        placed: List<Placement>,
        current: Placement,
        dpPerUnit: Double,
    ): LockResult = findLock(puzzle, placed, current.piece, current.turn, !current.mirrored, current.centre, dpPerUnit)

    /**
     * REQ-025 reload check (design §1 wrapper): the design §1 step-4 predicate in list order, each
     * placement tested against the placements kept so far (`ILockEngine.stillValid` KDoc). The kept
     * placements are the [saved] instances themselves, in their order.
     */
    override fun stillValid(puzzle: PuzzleDefinition, saved: List<Placement>): List<Placement> {
        val kept = ArrayList<Placement>()
        val keptPolys = ArrayList<List<Point>>()
        for (p in saved) {
            val poly = polygonOf(p)
            if (isValid(poly, puzzle, keptPolys)) {
                kept.add(p)
                keptPolys.add(poly)
            }
        }
        return kept
    }

    /**
     * REQ-022: solved exactly when the placed pieces are the puzzle's pieces (design §2 `isSolved`).
     * No geometry and no comparison with the stored solution (REQ-022 rule): every placement is
     * already a lock, so any exact cover counts (REQ-022.A1).
     */
    override fun isSolved(puzzle: PuzzleDefinition, placed: List<Placement>): Boolean =
        placed.size == puzzle.pieces.size && placed.map { it.piece }.toSet() == puzzle.pieces

    /** One lock candidate (design §1 step 2): its distance d = |t| and its exact centre. */
    private class Candidate(val d: Double, val centre: Point)

    private fun polygonOf(p: Placement): List<Point> = vertices(p.piece, p.turn, p.mirrored, p.centre)

    /**
     * Design §1 step 4, TYPE-004 validity: [poly] lies completely inside the silhouette and overlaps
     * none of [others] (REQ-019 statement, A2), with the 1e-6 tolerance of [Silhouette] and [Polygons].
     */
    private fun isValid(poly: List<Point>, puzzle: PuzzleDefinition, others: List<List<Point>>): Boolean =
        Silhouette.contains(poly, puzzle.solution) && others.none { Polygons.overlaps(poly, it) }

    /** Coincidence of two points, per coordinate within the TYPE-004 tolerance of 1e-6 (design §1 step 5). */
    private fun near(a: Point, b: Point): Boolean =
        abs(a.x - b.x) <= LockRules.EPS && abs(a.y - b.y) <= LockRules.EPS
}

/**
 * The REQ-019 anchor points (design §2 `anchors`): the outline corners of [puzzle], then each
 * [placed] piece's [LockEngine.vertices], in list order. Nothing else is an anchor — there are no
 * hidden lock points (REQ-019 rule, A3; G-04).
 */
internal fun LockEngine.anchors(puzzle: PuzzleDefinition, placed: List<Placement>): List<Point> {
    val out = ArrayList<Point>(outlineCorners(puzzle))
    for (p in placed) out.addAll(vertices(p.piece, p.turn, p.mirrored, p.centre))
    return out
}
