// Acceptance kit (WO-001, TASK-003) — transcribed puzzles and kit helpers, per the design §2
// "Kit fixtures" row (designs/WO-001-design.md). Test-only; no product code.
//
// - The puzzles are TRANSCRIBED from Tangrams/*.json by hand; the kit reads no file at run time
//   (architecture.md §3b O-02: only :content reads Tangrams/*.json).
// - A file coordinate `[a, b]` is a + b·√2 (Spec/03-puzzle-format.md §1) and is written here as
//   kitR2(a, b), so each line can be compared with its source file by eye.
// - This file carries no acceptance token (traceability-rules.md §2: a token is a coverage claim).
// - Helper names carry a `kit` prefix so they can never collide with an `internal` declaration of
//   :kernel's main source set in the same package.
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.contracts.Turn
import kotlin.math.abs
import kotlin.math.sqrt

// ── Coordinates ────────────────────────────────────────────────────────────────────────────────

/** √2 as a Double (IEEE-754 `sqrt` is correctly rounded; design §3 "Library facts"). */
val KIT_SQRT2: Double = sqrt(2.0)

/** A file coordinate pair `[a, b]` = a + b·√2 (Spec/03-puzzle-format.md §1). */
fun kitR2(a: Int, b: Int): Double = a + b * KIT_SQRT2

/** A point in puzzle units (x right, y down; Spec/03 §1). */
fun kitPt(x: Double, y: Double): Point = Point(x, y)

/** A point with integer file coordinates. */
fun kitPt(x: Int, y: Int): Point = Point(x.toDouble(), y.toDouble())

/** One solution entry: the piece and its polygon, in the file's corner order. */
fun kitPiece(id: PieceId, vararg corners: Point): SolvedPiece = SolvedPiece(id, corners.toList())

/** The average of a polygon's corners — the piece centre of TYPE-003 and of `Placement.centre`. */
fun kitAverage(points: List<Point>): Point =
    Point(points.sumOf { it.x } / points.size, points.sumOf { it.y } / points.size)

// ── Puzzles ────────────────────────────────────────────────────────────────────────────────────

/**
 * A [PuzzleDefinition] with only what the lock rule reads: [id], [kind] and the [solution]
 * (design §2 "Kit fixtures": `titles = mapOf("en" to id)`, `ArtLayer("#000000", emptyList())`).
 * Category, rating and the review flag are fixed neutral values; no [ILockEngine] member reads them.
 */
fun puzzleOf(id: String, kind: PuzzleKind, vararg solution: SolvedPiece): PuzzleDefinition =
    PuzzleDefinition(
        id = id,
        titles = mapOf("en" to id),
        category = "shapes",
        rating = 1,
        kind = kind,
        solution = solution.toList(),
        art = ArtLayer("#000000", emptyList()),
        reviewedByHuman = false,
    )

/**
 * Gift box — transcribed from `Tangrams/shapes-square.json` (kind full). The classic 4 × 4 square;
 * outline corners (0,0) (4,0) (4,4) (0,4) (design §2 `outlineCorners` row).
 */
val GIFT_BOX: PuzzleDefinition = puzzleOf(
    "shapes-square", PuzzleKind.FULL,
    kitPiece(PieceId.LT1, kitPt(0, 0), kitPt(4, 0), kitPt(2, 2)),
    kitPiece(PieceId.LT2, kitPt(0, 0), kitPt(2, 2), kitPt(0, 4)),
    kitPiece(PieceId.MT, kitPt(4, 2), kitPt(4, 4), kitPt(2, 4)),
    kitPiece(PieceId.ST1, kitPt(4, 0), kitPt(4, 2), kitPt(3, 1)),
    kitPiece(PieceId.SQ, kitPt(2, 2), kitPt(3, 1), kitPt(4, 2), kitPt(3, 3)),
    kitPiece(PieceId.ST2, kitPt(2, 2), kitPt(3, 3), kitPt(1, 3)),
    kitPiece(PieceId.PG, kitPt(0, 4), kitPt(1, 3), kitPt(3, 3), kitPt(2, 4)),
)

/**
 * Chocolate bar — transcribed from `Tangrams/shapes-rectangle.json` (kind full). A 4√2 × 2√2
 * rectangle; every piece at an odd (45°) turn. `[a, b]` → kitR2(a, b) = a + b·√2 (design §2
 * "Kit fixtures": "where `[a,b]` becomes `a + b*sqrt(2.0)`").
 */
val CHOCOLATE_BAR: PuzzleDefinition = puzzleOf(
    "shapes-rectangle", PuzzleKind.FULL,
    kitPiece(PieceId.LT1, kitPt(0.0, kitR2(0, 2)), kitPt(kitR2(0, 2), 0.0), kitPt(0.0, 0.0)),
    kitPiece(PieceId.LT2, kitPt(0.0, kitR2(0, 2)), kitPt(kitR2(0, 2), 0.0), kitPt(kitR2(0, 2), kitR2(0, 2))),
    kitPiece(PieceId.MT, kitPt(kitR2(0, 3), kitR2(0, 1)), kitPt(kitR2(0, 4), 0.0), kitPt(kitR2(0, 4), kitR2(0, 2))),
    kitPiece(
        PieceId.SQ,
        kitPt(kitR2(0, 2), kitR2(0, 2)), kitPt(kitR2(0, 2), kitR2(0, 1)),
        kitPt(kitR2(0, 3), kitR2(0, 1)), kitPt(kitR2(0, 3), kitR2(0, 2)),
    ),
    kitPiece(
        PieceId.PG,
        kitPt(kitR2(0, 4), 0.0), kitPt(kitR2(0, 3), kitR2(0, 1)),
        kitPt(kitR2(0, 2), kitR2(0, 1)), kitPt(kitR2(0, 3), 0.0),
    ),
    kitPiece(PieceId.ST1, kitPt(kitR2(0, 3), kitR2(0, 1)), kitPt(kitR2(0, 4), kitR2(0, 2)), kitPt(kitR2(0, 3), kitR2(0, 2))),
    kitPiece(PieceId.ST2, kitPt(kitR2(0, 3), 0.0), kitPt(kitR2(0, 2), kitR2(0, 1)), kitPt(kitR2(0, 2), 0.0)),
)

/**
 * Mini pyramid — transcribed from `Tangrams/shapes-mini-1.json` (kind mini, 3 pieces). The
 * silhouette is the triangle (0,2) (4,2) (2,0); outline corners (2,0) (0,2) (4,2) (design §2
 * `outlineCorners` row).
 */
val MINI_PYRAMID: PuzzleDefinition = puzzleOf(
    "shapes-mini-1", PuzzleKind.MINI,
    kitPiece(PieceId.SQ, kitPt(2, 2), kitPt(3, 1), kitPt(2, 0), kitPt(1, 1)),
    kitPiece(PieceId.ST1, kitPt(0, 2), kitPt(2, 2), kitPt(1, 1)),
    kitPiece(PieceId.ST2, kitPt(2, 2), kitPt(4, 2), kitPt(3, 1)),
)

/**
 * Warm-up 4 — transcribed from `Tangrams/shapes-warmup-4.json` (kind warmup). Non-convex, with a
 * notch (2,2) (3,3) (3,5) (2,4) that meets the outside at the pinch point (2,2), an outline corner
 * (design §2 `outlineCorners` row; review F1).
 */
val WARMUP_4: PuzzleDefinition = puzzleOf(
    "shapes-warmup-4", PuzzleKind.WARMUP,
    kitPiece(PieceId.LT1, kitPt(4, 6), kitPt(0, 6), kitPt(2, 4)),
    kitPiece(PieceId.LT2, kitPt(2, 2), kitPt(6, 2), kitPt(4, 4)),
    kitPiece(PieceId.MT, kitPt(2, 2), kitPt(2, 0), kitPt(4, 2)),
    kitPiece(PieceId.SQ, kitPt(0, 4), kitPt(1, 3), kitPt(2, 4), kitPt(1, 5)),
    kitPiece(PieceId.PG, kitPt(4, 6), kitPt(4, 4), kitPt(3, 3), kitPt(3, 5)),
    kitPiece(PieceId.ST1, kitPt(6, 4), kitPt(4, 4), kitPt(5, 3)),
    kitPiece(PieceId.ST2, kitPt(2, 2), kitPt(2, 4), kitPt(1, 3)),
)

/** The solution entry of [piece] in [puzzle]; fails loudly when the puzzle has no such piece. */
fun kitSolved(puzzle: PuzzleDefinition, piece: PieceId): SolvedPiece =
    puzzle.solution.firstOrNull { it.piece == piece }
        ?: throw AssertionError("fixture defect: ${puzzle.id} has no $piece")

// ── Poses ──────────────────────────────────────────────────────────────────────────────────────

/**
 * The [Placement] whose `vertices` reproduce [sp]'s polygon (design §2 "Kit fixtures" row, as
 * corrected by review F3): the centre is the polygon's average, and the result is the **lowest**
 * turn 0..7 — trying `mirrored = true` for PG only — whose corners match the polygon as a point
 * set within 1e-6. Matches are not unique (an SQ matches four turns, a PG two); the lowest wins.
 * Fails loudly when no turn matches.
 */
fun poseOf(engine: ILockEngine, sp: SolvedPiece): Placement {
    val centre = kitAverage(sp.polygon)
    val mirrors = if (sp.piece.type == PieceType.PG) listOf(false, true) else listOf(false)
    for (step in 0..7) {
        for (mirrored in mirrors) {
            val corners = engine.vertices(sp.piece, Turn(step), mirrored, centre)
            if (kitSamePointSet(sp.polygon, corners, 1e-6)) {
                return Placement(sp.piece, Turn(step), mirrored, centre)
            }
        }
    }
    throw AssertionError("poseOf: no turn of ${sp.piece} matches ${sp.polygon} (fixture or vertices defect)")
}

// ── Point assertions ───────────────────────────────────────────────────────────────────────────

/** True when [a] and [b] differ by at most [tol] in each coordinate; false for any NaN. */
fun kitNear(a: Point, b: Point, tol: Double): Boolean =
    abs(a.x - b.x) <= tol && abs(a.y - b.y) <= tol

/**
 * Asserts [actual] is within [tol] of [expected] in each coordinate (design §2 "Kit fixtures":
 * `assertNear(p, q, 1e-9)`; never compare a `Point` with `==`). A NaN coordinate fails.
 */
fun assertNear(expected: Point, actual: Point, tol: Double = 1e-9, message: String = "") {
    if (!kitNear(expected, actual, tol)) {
        throw AssertionError("$message expected $expected but was $actual (tolerance $tol)".trim())
    }
}

/** Asserts two corner lists agree corner by corner, in order, each within [tol]. */
fun assertNearList(expected: List<Point>, actual: List<Point>, tol: Double = 1e-9, message: String = "") {
    if (expected.size != actual.size) {
        throw AssertionError("$message expected ${expected.size} points $expected but was ${actual.size} $actual".trim())
    }
    expected.indices.forEach { i -> assertNear(expected[i], actual[i], tol, "$message [corner $i]") }
}

/**
 * True when [a] and [b] are the same point set within [tol]: equal sizes, and every point of each
 * has a partner in the other.
 */
fun kitSamePointSet(a: List<Point>, b: List<Point>, tol: Double): Boolean =
    a.size == b.size && a.all { p -> b.any { kitNear(p, it, tol) } } && b.all { p -> a.any { kitNear(p, it, tol) } }

/** [points] with near-duplicates (within [tol]) removed, first occurrence kept. */
fun kitDistinct(points: List<Point>, tol: Double): List<Point> {
    val out = mutableListOf<Point>()
    for (p in points) if (out.none { kitNear(it, p, tol) }) out += p
    return out
}

/** Asserts [actual] is [expected] as a point set within [tol] (each point once; order free). */
fun assertSamePointSet(expected: List<Point>, actual: List<Point>, tol: Double = 1e-9, message: String = "") {
    if (!kitSamePointSet(expected, actual, tol)) {
        val missing = expected.filter { p -> actual.none { kitNear(p, it, tol) } }
        val extra = actual.filter { p -> expected.none { kitNear(p, it, tol) } }
        throw AssertionError(
            "$message expected point set $expected (${expected.size}) but was $actual (${actual.size}); " +
                "missing $missing, extra $extra".trim(),
        )
    }
}
