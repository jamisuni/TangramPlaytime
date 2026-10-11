// Acceptance kit (WO-001, TASK-003, stage 2) — the in-scope acceptance lines of REQ-019, REQ-020 and
// REQ-022, tested through `LockEngine()` (design §2 `LockEngine` row: "Tests call LockEngine()").
//
// - Fixtures come from KitFixtures.kt (transcribed puzzles; the kit reads no file, O-02).
// - Every `findLock` fixture that expects a lock has exactly one valid position in reach, so its best
//   score is unique (review F4: a tie is a fixture defect, never an assertion target). No test tries
//   to tell the TYPE-004 |t| window from a score window (design §3).
// - Polygons / Silhouette serve as oracles only because GeometryKitTest pins them by hand (design §3
//   "Oracle honesty, A2"); the validity checks add an independent sampled-point check besides.
// - An acceptance token sits only on a test whose assertion is that line's meaning
//   (traceability-rules.md §2). Guardrail and decision pins carry `// guardrail` / `// decision`.
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.LockResult
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.contracts.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LockEngineKitTest {

    private val engine = LockEngine()

    /** dpPerUnit 100 gives R = 0.65 (design §2 `findLock` row). */
    private val dp100 = 100.0

    /** dpPerUnit 20 gives R = 1.5, the 30 dp floor (design §2 `findLock` row). */
    private val dp20 = 20.0

    private val r2 = KIT_SQRT2

    // ── Helpers ────────────────────────────────────────────────────────────────────────────────

    private fun posesOf(solution: List<SolvedPiece>): List<Placement> = solution.map { poseOf(engine, it) }

    private fun corners(p: Placement): List<Point> = engine.vertices(p.piece, p.turn, p.mirrored, p.centre)

    /** Drops [pose]'s piece at its own turn and mirror, its centre moved by (dx, dy). */
    private fun dropNear(
        puzzle: PuzzleDefinition,
        placed: List<Placement>,
        pose: Placement,
        dx: Double,
        dy: Double,
        dpPerUnit: Double = dp100,
    ): LockResult = engine.findLock(
        puzzle, placed, pose.piece, pose.turn, pose.mirrored,
        kitPt(pose.centre.x + dx, pose.centre.y + dy), dpPerUnit,
    )

    /** Asserts [result] locks [expected]'s piece, at its turn and mirror, with its centre at [expected]'s. */
    private fun assertLockedAt(expected: Placement, result: LockResult, message: String): Placement {
        assertTrue("$message: expected Locked, was $result", result is LockResult.Locked)
        val got = (result as LockResult.Locked).placement
        assertEquals("$message: piece", expected.piece, got.piece)
        assertEquals("$message: turn kept as dropped", expected.turn, got.turn)
        assertEquals("$message: mirror kept as dropped", expected.mirrored, got.mirrored)
        assertNear(expected.centre, got.centre, 1e-9, "$message: centre")
        return got
    }

    /** Point in a convex polygon (either winding), boundary included within [tol]. */
    private fun kitInsideConvex(p: Point, poly: List<Point>, tol: Double): Boolean {
        var allPos = true
        var allNeg = true
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val cr = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
            if (cr < -tol) allPos = false
            if (cr > tol) allNeg = false
        }
        return allPos || allNeg
    }

    /** Point strictly inside a convex polygon (either winding), at least [margin] off every edge line. */
    private fun kitStrictlyInsideConvex(p: Point, poly: List<Point>, margin: Double): Boolean {
        var allPos = true
        var allNeg = true
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val cr = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
            if (cr <= margin) allPos = false
            if (cr >= -margin) allNeg = false
        }
        return allPos || allNeg
    }

    /** Points strictly inside a convex polygon: its average, and points 90 % / 50 % of the way to each corner and edge midpoint. */
    private fun kitSamples(poly: List<Point>): List<Point> {
        val c = kitAverage(poly)
        val targets = poly + poly.indices.map { i ->
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            kitPt((a.x + b.x) / 2, (a.y + b.y) / 2)
        }
        return listOf(c) + targets.flatMap { t ->
            listOf(0.9, 0.5).map { f -> kitPt(c.x + f * (t.x - c.x), c.y + f * (t.y - c.y)) }
        }
    }

    /**
     * Asserts [p] is a lock REQ-019 allows on [puzzle] with [placed] on the board: completely inside the
     * silhouette and overlapping no placed piece (REQ-019 statement; TYPE-004 validity), checked with
     * the pinned Polygons / Silhouette and with an independent sampled-point check (design §3).
     */
    private fun assertInsideAndNoOverlap(puzzle: PuzzleDefinition, placed: List<Placement>, p: Placement, message: String) {
        val poly = corners(p)
        assertTrue("$message: sticks out of the silhouette", Silhouette.contains(poly, puzzle.solution))
        for (q in placed) {
            assertFalse("$message: overlaps ${q.piece}", Polygons.overlaps(poly, corners(q)))
        }
        for (s in kitSamples(poly)) {
            assertTrue(
                "$message: sample $s lies in no solution piece",
                puzzle.solution.any { kitInsideConvex(s, it.polygon, 1e-9) },
            )
            for (q in placed) {
                assertFalse("$message: sample $s lies inside ${q.piece}", kitStrictlyInsideConvex(s, corners(q), 1e-9))
            }
        }
    }

    /**
     * Asserts one of [p]'s corners is exactly on an anchor: an outline corner of [puzzle] or a corner of a
     * placed piece (REQ-019 statement and first rule; `LockResult.Locked` KDoc: "one of its corners
     * exactly on an anchor").
     */
    private fun assertCornerOnAnAnchor(puzzle: PuzzleDefinition, placed: List<Placement>, p: Placement, message: String) {
        val anchors = engine.outlineCorners(puzzle) + placed.flatMap { corners(it) }
        assertTrue(
            "$message: no corner of ${corners(p)} is on an anchor",
            corners(p).any { c -> anchors.any { kitNear(it, c, 1e-9) } },
        )
    }

    /** The Gift box solution turned 90° clockwise about (2,2): (x, y) → (4 − y, x) (review F3). */
    private val giftBoxTurned: List<SolvedPiece> =
        GIFT_BOX.solution.map { sp -> SolvedPiece(sp.piece, sp.polygon.map { kitPt(4 - it.y, it.x) }) }

    /** The Gift box solution mirrored across its diagonal: (x, y) → (y, x); its PG is mirrored. */
    private val giftBoxMirrored: List<SolvedPiece> =
        GIFT_BOX.solution.map { sp -> SolvedPiece(sp.piece, sp.polygon.map { kitPt(it.y, it.x) }) }

    // ── REQ-019 — lock to corners and neighbours ───────────────────────────────────────────────

    // REQ-019.A1 — "A piece released within the lock distance of a valid position locks there
    // exactly." Each drop is a valid position moved by less than R = 0.65 (TYPE-004), on an empty
    // board, so its corners are within R of outline corners; "exactly": the placement is the anchor
    // minus the corner's exact offset (TYPE-004), so the locked corners sit on the anchors within 1e-9.
    // Each fixture has one valid position in reach (F4). Hand: Gift box LT1 turn 0 → centre (2, 2/3),
    // corners (0,0) (4,0) (2,2); Chocolate bar LT1 turn 3 → (2√2/3, 2√2/3), corners (0,2√2) (2√2,0)
    // (0,0); Warm-up 4 ST2 turn 2 → (5/3, 3), corners (2,2) (2,4) (1,3).
    @Test
    fun req019A1_aDropNearAnOutlineCornerPositionLocksThereExactly() { // REQ-019.A1
        val cases = listOf(
            Triple(GIFT_BOX, PieceId.LT1, kitPt(0.21, -0.13)),
            Triple(CHOCOLATE_BAR, PieceId.LT1, kitPt(0.15, 0.10)),
            Triple(WARMUP_4, PieceId.ST2, kitPt(0.20, -0.15)),
        )
        for ((puzzle, piece, d) in cases) {
            val target = poseOf(engine, kitSolved(puzzle, piece))
            val got = assertLockedAt(target, dropNear(puzzle, emptyList(), target, d.x, d.y), "${puzzle.id} $piece")
            assertSamePointSet(kitSolved(puzzle, piece).polygon, corners(got), 1e-9, "${puzzle.id} $piece corners")
        }
        assertNear(kitPt(2.0, 2.0 / 3), poseOf(engine, kitSolved(GIFT_BOX, PieceId.LT1)).centre, 1e-9, "hand centre")
        assertNear(kitPt(2 * r2 / 3, 2 * r2 / 3), poseOf(engine, kitSolved(CHOCOLATE_BAR, PieceId.LT1)).centre, 1e-9, "hand centre")
    }

    // REQ-019.A1 — the valid position is anchored on a corner of a piece already on the board (REQ-019
    // statement: "or of a piece already on the board"). Hand: Gift box with LT1 placed, SQ turn 0 locks
    // at centre (3,2) with its corner (2,2) on LT1's corner — none of its corners is an outline corner;
    // Chocolate bar with LT1 placed, LT2 turn 7 locks at (4√2/3, 4√2/3) on LT1's corners (0,2√2) and
    // (2√2,0) — a √2 anchor reached exactly.
    @Test
    fun req019A1_aDropNearANeighbourCornerPositionLocksThereExactly() { // REQ-019.A1
        val giftLt1 = poseOf(engine, kitSolved(GIFT_BOX, PieceId.LT1))
        val sq = poseOf(engine, kitSolved(GIFT_BOX, PieceId.SQ))
        assertNear(kitPt(3, 2), sq.centre, 1e-9, "hand centre")
        val gotSq = assertLockedAt(sq, dropNear(GIFT_BOX, listOf(giftLt1), sq, 0.18, 0.20), "Gift box SQ by LT1")
        assertTrue("SQ corner on LT1's (2,2)", corners(gotSq).any { kitNear(it, kitPt(2, 2), 1e-9) })

        val chocLt1 = poseOf(engine, kitSolved(CHOCOLATE_BAR, PieceId.LT1))
        val lt2 = poseOf(engine, kitSolved(CHOCOLATE_BAR, PieceId.LT2))
        assertNear(kitPt(4 * r2 / 3, 4 * r2 / 3), lt2.centre, 1e-9, "hand centre")
        val gotLt2 = assertLockedAt(lt2, dropNear(CHOCOLATE_BAR, listOf(chocLt1), lt2, -0.20, 0.25), "Chocolate bar LT2 by LT1")
        assertSamePointSet(kitSolved(CHOCOLATE_BAR, PieceId.LT2).polygon, corners(gotLt2), 1e-9, "LT2 corners")
    }

    // REQ-019.A1 — a valid position anchored only at the Warm-up 4 pinch corner (2,2) (review F1;
    // design §3 "Corner rule": "a valid drop anchored only there would return None" under a sum-only
    // rule). Hand: MT turn 7 offsets (−2√2/3, 0) (√2/3, −√2) (√2/3, √2); with corner 1 on (2,2) the
    // centre is (2 − √2/3, 2 + √2) and the corners are (2 − √2, 2 + √2) (2,2) (2, 2 + 2√2): one leg on
    // the outline edge (2,2)–(0,4), inside ST2 ∪ SQ ∪ LT1, and no corner but (2,2) on an outline corner.
    @Test
    fun req019A1_aPositionAnchoredOnlyAtTheWarmup4PinchCornerLocksThere() { // REQ-019.A1
        val target = Placement(PieceId.MT, Turn(7), false, kitPt(2 - r2 / 3, 2 + r2))
        assertSamePointSet(
            listOf(kitPt(2 - r2, 2 + r2), kitPt(2, 2), kitPt(2.0, 2 + 2 * r2)), corners(target), 1e-9, "hand corners",
        )
        val got = assertLockedAt(target, dropNear(WARMUP_4, emptyList(), target, 0.10, 0.12), "Warm-up 4 MT at (2,2)")
        assertTrue("MT corner on (2,2)", corners(got).any { kitNear(it, kitPt(2, 2), 1e-9) })
    }

    // REQ-019.A1 — "within the lock distance": R = max(0.65, 30 dp in units) (TYPE-004; ILockEngine
    // KDoc). At dpPerUnit 20, R = 1.5, so a drop 1.0 from the valid position still locks there exactly.
    // Hand: Gift box LT1 turn 0 moved by (0.8, 0.6), |t| = 1.0 ≤ 1.5.
    @Test
    fun req019A1_theLockDistanceWidensToThe30dpFloor() { // REQ-019.A1
        val lt1 = poseOf(engine, kitSolved(GIFT_BOX, PieceId.LT1))
        assertLockedAt(lt1, dropNear(GIFT_BOX, emptyList(), lt1, 0.8, 0.6, dp20), "Gift box LT1, R = 1.5")
    }

    // REQ-019.A2 — "A locked piece never overlaps another piece and never sticks out of the silhouette."
    // Traps where the snuggest candidate in reach is invalid (hand-checked):
    // (a) LT2 turn 0 dropped exactly onto the placed LT1: t = 0 puts all three corners on anchors but
    //     overlaps LT1 fully; no other candidate is in reach (R = 0.65) → never a lock there; None.
    // (b) ST turn 0 with its right corner 0.1 from (0,0), from outside the box: the only candidate puts
    //     the ST at (−2,0)(0,0)(−1,1), outside → never a lock there; None.
    // (c) Gift box with LT1 and SQ placed, ST2 turn 7 dropped at (3.03, 3.86): the snuggest candidate
    //     pokes out below y = 4; the one valid lock is the corner (4,4), centre (4 − √2/3, 4 − √2/3).
    // (d) Warm-up 4 with LT2 placed, MT turn 7 dropped at (0.99, 3.28): the nearest candidate pokes out
    //     past the edge (2,2)–(0,4); the one valid lock is centre (2 − √2/3, 2 + √2).
    // "None" in (a)/(b) is TYPE-004 (no valid candidate in reach → REQ-020).
    @Test
    fun req019A2_aLockNeverOverlapsOrSticksOut_traps() { // REQ-019.A2
        val lt1 = poseOf(engine, kitSolved(GIFT_BOX, PieceId.LT1))
        val a = engine.findLock(GIFT_BOX, listOf(lt1), PieceId.LT2, Turn(0), false, lt1.centre, dp100)
        if (a is LockResult.Locked) assertInsideAndNoOverlap(GIFT_BOX, listOf(lt1), a.placement, "(a)")
        assertSame("(a) no valid candidate in reach", LockResult.None, a)

        val b = engine.findLock(GIFT_BOX, emptyList(), PieceId.ST1, Turn(0), false, kitPt(-0.9, 0.1 + 1.0 / 3), dp100)
        if (b is LockResult.Locked) assertInsideAndNoOverlap(GIFT_BOX, emptyList(), b.placement, "(b)")
        assertSame("(b) no valid candidate in reach", LockResult.None, b)

        val sq = poseOf(engine, kitSolved(GIFT_BOX, PieceId.SQ))
        val c = engine.findLock(GIFT_BOX, listOf(lt1, sq), PieceId.ST2, Turn(7), false, kitPt(3.03, 3.86), dp100)
        val gotC = assertLockedAt(Placement(PieceId.ST2, Turn(7), false, kitPt(4 - r2 / 3, 4 - r2 / 3)), c, "(c)")
        assertInsideAndNoOverlap(GIFT_BOX, listOf(lt1, sq), gotC, "(c)")

        val lt2 = poseOf(engine, kitSolved(WARMUP_4, PieceId.LT2))
        val d = engine.findLock(WARMUP_4, listOf(lt2), PieceId.MT, Turn(7), false, kitPt(0.99, 3.28), dp100)
        val gotD = assertLockedAt(Placement(PieceId.MT, Turn(7), false, kitPt(2 - r2 / 3, 2 + r2)), d, "(d)")
        assertInsideAndNoOverlap(WARMUP_4, listOf(lt2), gotD, "(d)")
    }

    /** Every free piece × turn × (PG: mirror) × drop centre on [xs] × [ys]; calls [check] per result. */
    private fun sweep(
        puzzle: PuzzleDefinition,
        placedIds: List<PieceId>,
        xs: List<Double>,
        ys: List<Double>,
        check: (PieceId, Turn, Boolean, Point, List<Placement>, LockResult) -> Unit,
    ) {
        val placed = placedIds.map { poseOf(engine, kitSolved(puzzle, it)) }
        for (piece in puzzle.pieces.filter { it !in placedIds }) {
            val mirrors = if (piece.type == PieceType.PG) listOf(false, true) else listOf(false)
            for (step in 0..7) for (mirrored in mirrors) for (x in xs) for (y in ys) {
                val at = kitPt(x, y)
                check(piece, Turn(step), mirrored, at, placed, engine.findLock(puzzle, placed, piece, Turn(step), mirrored, at, dp100))
            }
        }
    }

    private fun grid(from: Double, to: Double, n: Int): List<Double> = (0 until n).map { from + (to - from) * it / (n - 1) }

    private val sweepBoards = listOf(
        Triple(GIFT_BOX, listOf(PieceId.LT1, PieceId.SQ), grid(-0.3, 4.3, 9) to grid(-0.3, 4.3, 9)),
        Triple(WARMUP_4, listOf(PieceId.LT2, PieceId.MT), grid(-0.3, 6.3, 11) to grid(-0.3, 6.3, 11)),
        Triple(CHOCOLATE_BAR, listOf(PieceId.LT1, PieceId.PG), grid(-0.3, 4 * KIT_SQRT2 + 0.3, 11) to grid(-0.3, 2 * KIT_SQRT2 + 0.3, 7)),
    )

    // REQ-019.A2 — the same meaning over a sweep: every free piece at every turn (PG both mirrors)
    // dropped on a grid over three boards (convex, non-convex with a notch, √2 coordinates), with two
    // pieces already placed. Every lock lies inside the silhouette and overlaps no placed piece.
    // (The sweep must contain locks to mean anything; the scratch model expects several hundred.)
    @Test
    fun req019A2_overASweep_everyLockIsInsideAndOverlapsNothing() { // REQ-019.A2
        var locks = 0
        for ((puzzle, placedIds, xy) in sweepBoards) {
            sweep(puzzle, placedIds, xy.first, xy.second) { piece, turn, mirrored, at, placed, result ->
                if (result is LockResult.Locked) {
                    locks++
                    assertInsideAndNoOverlap(puzzle, placed, result.placement, "${puzzle.id} $piece ${turn.step} $mirrored at $at")
                }
            }
        }
        assertTrue("the sweep must exercise locks (fixture check), got $locks", locks >= 100)
    }

    // REQ-019.A3 — "A piece that touches no outline corner and has no neighbour to tie to never
    // locks." Gift box, R = 0.65. Each pose lies inside the silhouette (so only the anchor rule can
    // reject it) and no corner is within R of an outline corner (nearest is √2 or more away):
    // - design §3 pose: ST turn 0 at (2, 1/3), corners (1,0) (3,0) (2,1) — along the top edge;
    // - review F2: ST turn 0 at (2, 4/3), corners (1,1) (3,1) (2,2) — two corners on 360° solution
    //   vertices: an engine taking every solution vertex as an anchor would lock it in place;
    // - review F2: ST turn 6 at (10/3, 2), corners (3,3) (3,1) (4,2) — on 360° vertices and on the
    //   180° edge point (4,2), the right edge's midpoint: both hidden-anchor bugs would lock it;
    // - the last pose again with LT1 on the board: a neighbour, but none of its corners in reach.
    @Test
    fun req019A3_aPieceWithNoOutlineCornerOrNeighbourInReachNeverLocks() { // REQ-019.A3
        val lt1 = poseOf(engine, kitSolved(GIFT_BOX, PieceId.LT1))
        val cases = listOf(
            Triple(Turn(0), kitPt(2.0, 1.0 / 3), emptyList()),
            Triple(Turn(0), kitPt(2.0, 4.0 / 3), emptyList()),
            Triple(Turn(6), kitPt(10.0 / 3, 2.0), emptyList()),
            Triple(Turn(6), kitPt(10.0 / 3, 2.0), listOf(lt1)),
        )
        for ((turn, centre, placed) in cases) {
            val poly = engine.vertices(PieceId.ST1, turn, false, centre)
            assertTrue("fixture: ST at $centre lies inside the Gift box", Silhouette.contains(poly, GIFT_BOX.solution))
            assertSame(
                "ST turn ${turn.step} at $centre with ${placed.map { it.piece }} must not lock",
                LockResult.None,
                engine.findLock(GIFT_BOX, placed, PieceId.ST1, turn, false, centre, dp100),
            )
        }
    }

    // REQ-019.A4 — "Six pieces locked in any arrangement that leaves a hole of the seventh piece's shape:
    // the seventh piece locks into it." Five arrangements (Gift box as stored, turned 90° and mirrored
    // across its diagonal — review F3; Chocolate bar at 45° turns with a mirrored PG; Warm-up 4,
    // non-convex), each piece in turn the seventh: dropped at the hole's turn and mirror, moved by up
    // to 0.42 (< R = 0.65), it locks exactly into the hole. The hole is the only valid position (design
    // §3 "A4"), so the best is unique (F4).
    @Test
    fun req019A4_theSeventhPieceLocksIntoTheHoleOfItsShape() { // REQ-019.A4
        val moves = listOf(
            kitPt(0.30, -0.20), kitPt(-0.25, 0.30), kitPt(0.35, 0.15), kitPt(-0.20, -0.35),
            kitPt(0.10, 0.40), kitPt(-0.40, 0.05), kitPt(0.25, 0.25),
        )
        val arrangements = listOf(
            "Gift box" to (GIFT_BOX to GIFT_BOX.solution),
            "Gift box turned" to (GIFT_BOX to giftBoxTurned),
            "Gift box mirrored" to (GIFT_BOX to giftBoxMirrored),
            "Chocolate bar" to (CHOCOLATE_BAR to CHOCOLATE_BAR.solution),
            "Warm-up 4" to (WARMUP_4 to WARMUP_4.solution),
        )
        for ((name, pair) in arrangements) {
            val (puzzle, arrangement) = pair
            val poses = posesOf(arrangement)
            for ((k, hole) in poses.withIndex()) {
                val six = poses.filter { it.piece != hole.piece }
                val m = moves[k % moves.size]
                val got = assertLockedAt(hole, dropNear(puzzle, six, hole, m.x, m.y), "$name, ${hole.piece} last")
                assertSamePointSet(arrangement[k].polygon, corners(got), 1e-9, "$name, ${hole.piece} fills the hole")
            }
        }
    }

    // guardrail G-04 — TYPE-004: "The turn must match: a piece one 45° step off does not lock. The
    // mirror must match: the search never tries the other mirror image"; REQ-019 rule: "The turn and
    // mirror the piece has when dropped are the ones tried; the lock never changes them"; design §3
    // "A drop one 45° step off its hole never locks into it". The A4 holes, with the seventh dropped one
    // step either way (and the PG at the other mirror): no lock fills the hole.
    @Test
    fun theTurnAndMirrorAreNeverSearched() {
        for ((puzzle, arrangement) in listOf(GIFT_BOX to GIFT_BOX.solution, CHOCOLATE_BAR to CHOCOLATE_BAR.solution, WARMUP_4 to WARMUP_4.solution)) {
            val poses = posesOf(arrangement)
            for (hole in poses) {
                val six = poses.filter { it.piece != hole.piece }
                val variants = mutableListOf(hole.copy(turn = hole.turn.next()), hole.copy(turn = hole.turn.previous()))
                if (hole.piece.type == PieceType.PG) variants += hole.copy(mirrored = !hole.mirrored)
                for (v in variants) {
                    val result = dropNear(puzzle, six, v, 0.1, 0.1)
                    if (result is LockResult.Locked) {
                        assertEquals("${puzzle.id} ${v.piece}: turn kept", v.turn, result.placement.turn)
                        assertEquals("${puzzle.id} ${v.piece}: mirror kept", v.mirrored, result.placement.mirrored)
                    }
                    assertSame("${puzzle.id} ${v.piece} turn ${v.turn.step} mirrored ${v.mirrored}", LockResult.None, result)
                }
            }
        }
    }

    // ── REQ-020 — missed drop returns to the tray (seam half; WO-001 § Scope) ──────────────────

    // REQ-020.A1 — "After any drop, the piece is either locked on the board or back in its tray cell."
    // At the seam (WO § Scope; design §3 "REQ-020, seam half"): every drop gets exactly one of
    // `Locked` — a real lock of the dropped piece, at its turn and mirror, inside the silhouette, over
    // no placed piece, a corner on an anchor (REQ-019) — or `None`, the go-home value; never an
    // exception (G-15) and never a third, loose outcome (REQ-020 rule: "no loose (unlocked) piece on
    // the board at any time"). REQ-020 statement: a drop with no valid position within R, or outside
    // the board or on the tray, goes home → `None` (design §2 `findLock`: "a drop off the board returns
    // None"). Hand: Gift box LT1 moved 1.0 from its spot at R = 0.65 has no candidate in reach.
    @Test
    fun req020A1_everyDropEndsLockedOnTheBoardOrGoesHome() { // REQ-020.A1
        var locks = 0
        var misses = 0
        for ((puzzle, placedIds, xy) in sweepBoards) {
            sweep(puzzle, placedIds, xy.first, xy.second) { piece, turn, mirrored, at, placed, result ->
                when (result) {
                    is LockResult.Locked -> {
                        locks++
                        val p = result.placement
                        val tag = "${puzzle.id} $piece ${turn.step} $mirrored at $at"
                        assertEquals("$tag: the dropped piece", piece, p.piece)
                        assertEquals("$tag: turn", turn, p.turn)
                        assertEquals("$tag: mirror", mirrored, p.mirrored)
                        assertInsideAndNoOverlap(puzzle, placed, p, tag)
                        assertCornerOnAnAnchor(puzzle, placed, p, tag)
                    }
                    LockResult.None -> misses++
                }
            }
        }
        assertTrue("the sweep must contain both outcomes (fixture check): $locks locks, $misses misses", locks >= 100 && misses >= 100)

        // Off the board and on the tray (the kernel knows no tray: anywhere well away from the silhouette).
        val lt1 = poseOf(engine, kitSolved(GIFT_BOX, PieceId.LT1))
        val placed = listOf(poseOf(engine, kitSolved(GIFT_BOX, PieceId.MT)))
        for (at in listOf(kitPt(-3.0, 2.0), kitPt(2.0, -3.0), kitPt(7.5, 2.0), kitPt(2.0, 9.0), kitPt(1e6, -1e6))) {
            for (step in 0..7) {
                assertSame("drop off the board at $at, turn $step", LockResult.None,
                    engine.findLock(GIFT_BOX, placed, PieceId.LT1, Turn(step), false, at, dp100))
            }
        }
        // No valid position within R = 0.65: the drop 1.0 away (locks at R = 1.5, see the A1 floor test).
        assertSame("no valid position in reach", LockResult.None, dropNear(GIFT_BOX, emptyList(), lt1, 0.8, 0.6, dp100))
    }

    // REQ-020 A2 ("No error message is shown for a missed drop") carries no token in this kit: the seam
    // can only show that a miss is the text-free value None and throws nothing — G-15 conformance, not
    // the player-visible outcome. Judged a dishonest seam oracle (WO-001 § Scope escape clause); A2 moved
    // to WO-003 #Solving (decisions.md, 2026-10-11). This test is a guardrail pin only.
    // guardrail G-15 — "Expected outcomes cross governed seams as sealed results (LockResult.None …),
    // never as exceptions": misses return the value None and throw nothing.
    @Test
    fun aMissIsTheValueNoneNeverAnException() {
        val lt1 = poseOf(engine, kitSolved(GIFT_BOX, PieceId.LT1))
        val misses = listOf(
            engine.findLock(GIFT_BOX, listOf(lt1), PieceId.LT2, Turn(0), false, lt1.centre, dp100),
            engine.findLock(GIFT_BOX, emptyList(), PieceId.ST1, Turn(0), false, kitPt(2.0, 4.0 / 3), dp100),
            engine.findLock(GIFT_BOX, emptyList(), PieceId.PG, Turn(3), true, kitPt(-50.0, 50.0), dp100),
        )
        for (m in misses) assertSame(LockResult.None, m)
    }

    // ── REQ-022 — any exact cover solves the puzzle ────────────────────────────────────────────

    // REQ-022.A1 — "An arrangement different from the stored solution that fills the silhouette counts
    // as solved." Review F3: the arrangement is reached through seven findLock calls in tray order, each
    // asserted Locked at its intended pose, and isSolved is false before the seventh lock and true after
    // it. Two covers of the Gift box that differ from the stored one: turned 90° about (2,2) (all seven
    // polygons differ) and mirrored across the diagonal (its PG mirrored). REQ-022 rule: "No comparison
    // with the stored solution is made."
    @Test
    fun req022A1_aDifferentCoverBuiltLockByLockCountsAsSolved() { // REQ-022.A1
        val moves = listOf(
            kitPt(0.22, 0.0), kitPt(0.0, 0.22), kitPt(-0.22, 0.0), kitPt(0.0, -0.22),
            kitPt(0.15, 0.15), kitPt(-0.15, 0.15), kitPt(0.15, -0.15),
        )
        for ((name, cover) in listOf("turned" to giftBoxTurned, "mirrored" to giftBoxMirrored)) {
            assertTrue(
                "fixture: the $name cover differs from the stored solution",
                cover.any { c -> GIFT_BOX.solution.none { s -> kitSamePointSet(c.polygon, s.polygon, 1e-9) } },
            )
            val targets = PieceId.entries.map { id -> poseOf(engine, cover.first { it.piece == id }) }
            val placed = mutableListOf<Placement>()
            for ((k, target) in targets.withIndex()) {
                assertFalse("$name: solved before lock ${k + 1}", engine.isSolved(GIFT_BOX, placed))
                val m = moves[k]
                placed += assertLockedAt(target, dropNear(GIFT_BOX, placed.toList(), target, m.x, m.y), "$name, lock ${k + 1} ${target.piece}")
            }
            assertTrue("$name: solved after the seventh lock", engine.isSolved(GIFT_BOX, placed))
        }
    }

    // ── Engine seams used above ────────────────────────────────────────────────────────────────

    // guardrail G-04 — design §2 `outlineCorners` row: "Returns Silhouette.outlineCorners(puzzle.solution).
    // Gift box gives the 4 square corners; mini-1 gives (2,0) (0,2) (4,2); shapes-warmup-4 includes
    // (2,2)"; `vertices` row: "Returns centre + PieceShapes.offsets(...)". These are the anchors the
    // tests above lean on.
    @Test
    fun engineOutlineCornersAndVerticesAreTheSeams() {
        for (puzzle in listOf(GIFT_BOX, CHOCOLATE_BAR, MINI_PYRAMID, WARMUP_4)) {
            assertSamePointSet(Silhouette.outlineCorners(puzzle.solution), engine.outlineCorners(puzzle), 1e-12, puzzle.id)
        }
        assertSamePointSet(listOf(kitPt(0, 0), kitPt(4, 0), kitPt(4, 4), kitPt(0, 4)), engine.outlineCorners(GIFT_BOX), 1e-9, "Gift box")
        assertTrue(engine.outlineCorners(WARMUP_4).any { kitNear(it, kitPt(2, 2), 1e-9) })
        for (type in PieceType.entries) {
            val id = PieceId.entries.first { it.type == type }
            for (step in 0..7) for (mirrored in listOf(false, true)) {
                val c = kitPt(1.25, -0.5)
                assertNearList(
                    PieceShapes.offsets(type, Turn(step), mirrored).map { kitPt(c.x + it.x, c.y + it.y) },
                    engine.vertices(id, Turn(step), mirrored, c), 1e-12, "$id turn $step mirrored $mirrored",
                )
            }
        }
    }

    // decision — decisions.md 2026-10-11 (WO-001 / P3 design): "findLock throws IllegalArgumentException
    // when dpPerUnit is not finite and > 0, or when the dropped piece is also in placed" (G-15: a
    // programming error, not an expected outcome).
    @Test
    fun findLockPreconditionViolationsThrowIllegalArgument() {
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows("dpPerUnit $bad", IllegalArgumentException::class.java) {
                engine.findLock(GIFT_BOX, emptyList(), PieceId.SQ, Turn(0), false, kitPt(3, 2), bad)
            }
        }
        val lt1 = poseOf(engine, kitSolved(GIFT_BOX, PieceId.LT1))
        assertThrows("piece in placed", IllegalArgumentException::class.java) {
            engine.findLock(GIFT_BOX, listOf(lt1), PieceId.LT1, Turn(0), false, lt1.centre, dp100)
        }
    }
}
