// Acceptance kit (WO-001, TASK-003, stage 1) — hand-computed oracle pins of the TASK-001 seams
// PieceShapes, Polygons, Silhouette and LockRules, exactly as the design §2 seam table specifies
// them (designs/WO-001-design.md). Design §3 "Oracle honesty, A2": a later test may use Polygons or
// Silhouette as its oracle only because this file pins them with hand-computed values.
//
// Every expected number below was computed by hand from TYPE-001 (Spec/03-puzzle-format.md §3
// local corners), TYPE-003, TYPE-004, the ILockEngine KDoc and the seam rows; none comes from code,
// the prototype or tools/tangram_geom.py (which misses the warmup-4 corner (2,2), review F1).
// Every test is a guardrail pin (G-04: one lock rule, in the kernel) and carries no acceptance token.
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class GeometryKitTest {

    private val h = KIT_SQRT2 / 2

    /** Independent shoelace (kit-side), so area pins of PieceShapes do not lean on Polygons. */
    private fun kitShoelace(p: List<Point>): Double {
        var s = 0.0
        for (i in p.indices) {
            val a = p[i]
            val b = p[(i + 1) % p.size]
            s += a.x * b.y - b.x * a.y
        }
        return s / 2
    }

    private fun square(x0: Double, y0: Double, side: Double): List<Point> =
        listOf(kitPt(x0, y0), kitPt(x0 + side, y0), kitPt(x0 + side, y0 + side), kitPt(x0, y0 + side))

    private val unitSquare = square(0.0, 0.0, 1.0)

    // ── PieceShapes ────────────────────────────────────────────────────────────────────────────

    // guardrail G-04 — PieceShapes row step 1: "Take the Spec §3 corners minus their average";
    // `vertices` row: "Corner i is Spec §3 corner i". Spec/03 §3 local corners (TYPE-001 shapes):
    // LT (0,0)(4,0)(2,2) avg (2,2/3); MT (0,0)(2,0)(0,2) avg (2/3,2/3); ST (0,0)(2,0)(1,1) avg (1,1/3);
    // SQ (0,0)(1,-1)(2,0)(1,1) avg (1,0); PG (0,0)(2,0)(3,-1)(1,-1) avg (3/2,-1/2).
    @Test
    fun pieceShapes_turn0_isSpecCornersMinusTheirAverage_inSpecOrder() {
        val expected = mapOf(
            PieceType.LT to listOf(kitPt(-2.0, -2.0 / 3), kitPt(2.0, -2.0 / 3), kitPt(0.0, 4.0 / 3)),
            PieceType.MT to listOf(kitPt(-2.0 / 3, -2.0 / 3), kitPt(4.0 / 3, -2.0 / 3), kitPt(-2.0 / 3, 4.0 / 3)),
            PieceType.ST to listOf(kitPt(-1.0, -1.0 / 3), kitPt(1.0, -1.0 / 3), kitPt(0.0, 2.0 / 3)),
            PieceType.SQ to listOf(kitPt(-1, 0), kitPt(0, -1), kitPt(1, 0), kitPt(0, 1)),
            PieceType.PG to listOf(kitPt(-1.5, 0.5), kitPt(0.5, 0.5), kitPt(1.5, -0.5), kitPt(-0.5, -0.5)),
        )
        for ((type, corners) in expected) {
            assertNearList(corners, PieceShapes.offsets(type, Turn(0), false), 1e-9, "$type turn 0")
        }
    }

    // guardrail G-04 — PieceShapes row step 3: x' = x·c − y·s, y' = x·s + y·c with c[2] = 0, s[2] = 1,
    // i.e. (x, y) → (−y, x); Types.kt `Turn`: "positive is clockwise on screen (y points down)".
    // Hand: LT (0,4/3) apex-down → (−4/3, 0) apex-left = a quarter turn clockwise on screen.
    @Test
    fun pieceShapes_turn2_isAQuarterTurnClockwiseOnScreen() {
        assertNearList(
            listOf(kitPt(2.0 / 3, -2.0), kitPt(2.0 / 3, 2.0), kitPt(-4.0 / 3, 0.0)),
            PieceShapes.offsets(PieceType.LT, Turn(2), false), 1e-9, "LT turn 2",
        )
        assertNearList(
            listOf(kitPt(-0.5, -1.5), kitPt(-0.5, 0.5), kitPt(0.5, 1.5), kitPt(0.5, -0.5)),
            PieceShapes.offsets(PieceType.PG, Turn(2), false), 1e-9, "PG turn 2",
        )
    }

    // guardrail G-04 — PieceShapes row step 3 with c, s = ±h at odd steps, h = √2/2 (TYPE-003: one
    // step is 45°). Hand: ST turn 1 (c = s = h): (x, y) → (h(x − y), h(x + y));
    // MT turn 3 (c = −h, s = h): (x, y) → (−h(x + y), h(x − y)).
    @Test
    fun pieceShapes_oddTurns_rotateByMultiplesOf45Degrees() {
        val r = KIT_SQRT2
        assertNearList(
            listOf(kitPt(-r / 3, -2 * r / 3), kitPt(2 * r / 3, r / 3), kitPt(-r / 3, r / 3)),
            PieceShapes.offsets(PieceType.ST, Turn(1), false), 1e-9, "ST turn 1",
        )
        assertNearList(
            listOf(kitPt(-h, -h), kitPt(h, -h), kitPt(h, h), kitPt(-h, h)),
            PieceShapes.offsets(PieceType.SQ, Turn(1), false), 1e-9, "SQ turn 1",
        )
        assertNearList(
            listOf(kitPt(2 * r / 3, 0.0), kitPt(-r / 3, r), kitPt(-r / 3, -r)),
            PieceShapes.offsets(PieceType.MT, Turn(3), false), 1e-9, "MT turn 3",
        )
    }

    // guardrail G-04 — PieceShapes row: "Exact 0 and ±1, never cos(π/2)"; TYPE-004: "the locked
    // position is the anchor's exact position minus the piece corner's exact offset". At the axis
    // turns 0, 2, 4, 6 the SQ offsets (exact integers at turn 0) must come out bit-exact (delta 0.0);
    // a cos(π/2) ≈ 6e-17 table entry would leave a non-zero residue here.
    @Test
    fun pieceShapes_axisTurns_areExact() {
        val expected = mapOf(
            0 to listOf(kitPt(-1, 0), kitPt(0, -1), kitPt(1, 0), kitPt(0, 1)),
            2 to listOf(kitPt(0, -1), kitPt(1, 0), kitPt(0, 1), kitPt(-1, 0)),
            4 to listOf(kitPt(1, 0), kitPt(0, 1), kitPt(-1, 0), kitPt(0, -1)),
            6 to listOf(kitPt(0, 1), kitPt(-1, 0), kitPt(0, -1), kitPt(1, 0)),
        )
        for ((step, corners) in expected) {
            val actual = PieceShapes.offsets(PieceType.SQ, Turn(step), false)
            assertEquals("SQ turn $step corner count", 4, actual.size)
            for (i in 0 until 4) {
                assertEquals("SQ turn $step corner $i x", corners[i].x, actual[i].x, 0.0)
                assertEquals("SQ turn $step corner $i y", corners[i].y, actual[i].y, 0.0)
            }
        }
        // LT corner 2 is (0, 4/3) at turn 0; its x offset 2 − 2 is exactly 0, so at turns 2 and 6
        // its y must be exactly 0 (y' = x·s + y·c with c = 0).
        assertEquals("LT turn 2 corner 2 y", 0.0, PieceShapes.offsets(PieceType.LT, Turn(2), false)[2].y, 0.0)
        assertEquals("LT turn 6 corner 2 y", 0.0, PieceShapes.offsets(PieceType.LT, Turn(6), false)[2].y, 0.0)
    }

    // guardrail G-04 — TYPE-003: "A turn keeps the piece's centre (the average of its corners)
    // fixed" → the offsets average to (0,0) at every turn and mirror; TYPE-001 areas LT 4, MT 2,
    // ST 1, SQ 2, PG 2 (PieceType.area); a turn or mirror is rigid, so every side keeps its turn-0
    // length (LT legs 2√2 / hypotenuse 4, MT legs 2 / hypotenuse 2√2, ST legs √2 / hypotenuse 2,
    // SQ sides √2, PG sides 2 and √2).
    @Test
    fun pieceShapes_everyTurnAndMirror_keepsCentreAreaAndSides() {
        val r = KIT_SQRT2
        val sides = mapOf(
            PieceType.LT to listOf(4.0, 2 * r, 2 * r),
            PieceType.MT to listOf(2.0, 2 * r, 2.0),
            PieceType.ST to listOf(2.0, r, r),
            PieceType.SQ to listOf(r, r, r, r),
            PieceType.PG to listOf(2.0, r, 2.0, r),
        )
        for (type in PieceType.entries) {
            for (step in 0..7) {
                for (mirrored in listOf(false, true)) {
                    val o = PieceShapes.offsets(type, Turn(step), mirrored)
                    val tag = "$type turn $step mirrored $mirrored"
                    assertEquals("$tag corner count", sides.getValue(type).size, o.size)
                    assertNear(kitPt(0.0, 0.0), kitAverage(o), 1e-9, "$tag centre")
                    assertEquals("$tag area", type.area.toDouble(), abs(kitShoelace(o)), 1e-9)
                    for (i in o.indices) {
                        val a = o[i]
                        val b = o[(i + 1) % o.size]
                        assertEquals("$tag side $i", sides.getValue(type)[i], hypot(b.x - a.x, b.y - a.y), 1e-9)
                    }
                }
            }
        }
    }

    // guardrail G-04 — TYPE-003: "Only the parallelogram has a mirror image that differs from
    // itself; mirroring the other pieces has no visible effect"; PieceShapes row step 2: "Mirror
    // x → −x for PG only"; design §3 "MT mirror trap" (a mirrored MT would be a 90°-turned pose).
    // Hand: PG mirrored turn 0 = (1.5,0.5) (−0.5,0.5) (−1.5,−0.5) (0.5,−0.5); mirrored turn 3
    // (c = −h, s = h) = (−√2, √2/2) (0, −√2/2) (√2, −√2/2) (0, √2/2).
    @Test
    fun pieceShapes_mirrorChangesOnlyTheParallelogram() {
        for (type in PieceType.entries.filter { it != PieceType.PG }) {
            for (step in 0..7) {
                assertNearList(
                    PieceShapes.offsets(type, Turn(step), false),
                    PieceShapes.offsets(type, Turn(step), true),
                    1e-12, "$type turn $step mirrored must equal unmirrored",
                )
            }
        }
        val pgMirrored0 = PieceShapes.offsets(PieceType.PG, Turn(0), true)
        assertNearList(
            listOf(kitPt(1.5, 0.5), kitPt(-0.5, 0.5), kitPt(-1.5, -0.5), kitPt(0.5, -0.5)),
            pgMirrored0, 1e-9, "PG mirrored turn 0",
        )
        val r = KIT_SQRT2
        assertNearList(
            listOf(kitPt(-r, r / 2), kitPt(0.0, -r / 2), kitPt(r, -r / 2), kitPt(0.0, r / 2)),
            PieceShapes.offsets(PieceType.PG, Turn(3), true), 1e-9, "PG mirrored turn 3",
        )
        for (step in 0..7) {
            assertFalse(
                "PG mirrored turn 0 must differ from every unmirrored turn (here $step)",
                kitSamePointSet(pgMirrored0, PieceShapes.offsets(PieceType.PG, Turn(step), false), 1e-6),
            )
        }
    }

    /** Every (turn, mirrored) whose PieceShapes corners, centred on the polygon's average, match it. */
    private fun matchingPoses(puzzle: PuzzleDefinition): Map<String, Set<Pair<Int, Boolean>>> =
        puzzle.solution.associate { sp ->
            val c = kitAverage(sp.polygon)
            val mirrors = if (sp.piece.type == PieceType.PG) listOf(false, true) else listOf(false)
            val matches = (0..7).flatMap { step -> mirrors.map { m -> step to m } }.filter { (step, m) ->
                val world = PieceShapes.offsets(sp.piece.type, Turn(step), m).map { kitPt(c.x + it.x, c.y + it.y) }
                kitSamePointSet(sp.polygon, world, 1e-6)
            }.toSet()
            sp.piece.name to matches
        }

    // guardrail G-04 — TYPE-001 (shapes, Spec/03 §3) and TYPE-003 (8 turns of 45°; only PG mirrors):
    // every transcribed solution polygon is a PieceShapes pose. Hand-derived match sets (review F3:
    // "every SQ polygon matches four turns, every PG two"). Also guards the transcription itself.
    @Test
    fun pieceShapes_everyFixturePolygon_isOneOfTheseTurns() {
        val f = false
        val expected = mapOf(
            GIFT_BOX to mapOf(
                "LT1" to setOf(0 to f), "LT2" to setOf(6 to f), "MT" to setOf(4 to f), "ST1" to setOf(2 to f),
                "SQ" to setOf(0 to f, 2 to f, 4 to f, 6 to f), "ST2" to setOf(4 to f), "PG" to setOf(0 to f, 4 to f),
            ),
            CHOCOLATE_BAR to mapOf(
                "LT1" to setOf(3 to f), "LT2" to setOf(7 to f), "MT" to setOf(7 to f),
                "SQ" to setOf(1 to f, 3 to f, 5 to f, 7 to f), "PG" to setOf(3 to true, 7 to true),
                "ST1" to setOf(1 to f), "ST2" to setOf(3 to f),
            ),
            MINI_PYRAMID to mapOf(
                "SQ" to setOf(0 to f, 2 to f, 4 to f, 6 to f), "ST1" to setOf(4 to f), "ST2" to setOf(4 to f),
            ),
            WARMUP_4 to mapOf(
                "LT1" to setOf(4 to f), "LT2" to setOf(0 to f), "MT" to setOf(6 to f),
                "SQ" to setOf(0 to f, 2 to f, 4 to f, 6 to f), "PG" to setOf(2 to f, 6 to f),
                "ST1" to setOf(4 to f), "ST2" to setOf(2 to f),
            ),
        )
        for ((puzzle, table) in expected) {
            assertEquals("${puzzle.id} (turn, mirrored) match sets", table, matchingPoses(puzzle))
        }
    }

    // ── Polygons ───────────────────────────────────────────────────────────────────────────────

    // guardrail G-04 — Polygons row: "signedArea(p) is the shoelace Σ(xᵢyᵢ₊₁ − xᵢ₊₁yᵢ)/2; area(p)
    // is its absolute value". Hand: unit square (0,0)(1,0)(1,1)(0,1): Σ = 0+1+1+0 = 2 → +1;
    // Gift box LT1 (0,0)(4,0)(2,2): Σ = 0+8+0 → +4; Gift box PG (0,4)(1,3)(3,3)(2,4):
    // Σ = −4−6+6+8 → +2; Mini pyramid SQ (2,2)(3,1)(2,0)(1,1): Σ = −4−2+2+0 → −2.
    @Test
    fun polygons_signedAreaAndArea_areTheShoelace() {
        assertEquals(1.0, Polygons.signedArea(unitSquare), 1e-12)
        assertEquals(-1.0, Polygons.signedArea(unitSquare.reversed()), 1e-12)
        assertEquals(1.0, Polygons.area(unitSquare.reversed()), 1e-12)
        assertEquals(4.0, Polygons.signedArea(kitSolved(GIFT_BOX, PieceId.LT1).polygon), 1e-12)
        assertEquals(2.0, Polygons.signedArea(kitSolved(GIFT_BOX, PieceId.PG).polygon), 1e-12)
        val miniSq = kitSolved(MINI_PYRAMID, PieceId.SQ).polygon
        assertEquals(-2.0, Polygons.signedArea(miniSq), 1e-12)
        assertEquals(2.0, Polygons.area(miniSq), 1e-12)
    }

    // guardrail G-04 — Polygons row `area` + TYPE-001 areas (LT 4, MT 2, SQ 2, PG 2, ST 1), over every
    // transcribed polygon of the four fixtures, both windings and √2 coordinates included.
    @Test
    fun polygons_area_ofEveryFixturePieceIsItsTypeArea() {
        for (puzzle in listOf(GIFT_BOX, CHOCOLATE_BAR, MINI_PYRAMID, WARMUP_4)) {
            for (sp in puzzle.solution) {
                assertEquals("${puzzle.id} ${sp.piece}", sp.piece.type.area.toDouble(), Polygons.area(sp.polygon), 1e-9)
            }
        }
    }

    // guardrail G-04 — Polygons row: "intersectionArea(a, b) is the area of the clip, or 0 when the
    // clip has fewer than 3 points"; clipConvex "first normalises both polygons to positive signed
    // area" (so winding never matters; design §3 "Both polygons must have the same winding").
    // Hand: unit squares offset by (0.5,0.5) overlap in [0.5,1]² = 0.25 (design §3 "Oracle honesty,
    // A2"); LT (0,0)(4,0)(2,2) vs the same moved by (2,0) overlap in (2,0)(4,0)(3,1) = 1; the ST
    // (1,0)(3,0)(2,1) lies inside that LT, so the overlap is the ST's area 1.
    @Test
    fun polygons_intersectionArea_handComputed() {
        val offset = square(0.5, 0.5, 1.0)
        assertEquals(0.25, Polygons.intersectionArea(unitSquare, offset), 1e-12)
        assertEquals(0.25, Polygons.intersectionArea(unitSquare.reversed(), offset), 1e-12)
        assertEquals(0.25, Polygons.intersectionArea(unitSquare, offset.reversed()), 1e-12)
        assertEquals(0.25, Polygons.intersectionArea(unitSquare.reversed(), offset.reversed()), 1e-12)
        assertEquals(0.25, Polygons.intersectionArea(offset, unitSquare), 1e-12)

        val lt = listOf(kitPt(0, 0), kitPt(4, 0), kitPt(2, 2))
        val ltMoved = listOf(kitPt(2, 0), kitPt(6, 0), kitPt(4, 2))
        assertEquals(1.0, Polygons.intersectionArea(lt, ltMoved), 1e-12)
        assertEquals(1.0, Polygons.intersectionArea(ltMoved.reversed(), lt), 1e-12)

        val st = listOf(kitPt(1, 0), kitPt(3, 0), kitPt(2, 1))
        assertEquals(1.0, Polygons.intersectionArea(st, lt), 1e-12)
        assertEquals(1.0, Polygons.intersectionArea(lt, st), 1e-12)

        // Disjoint: the clip is empty (fewer than 3 points) → exactly 0.
        assertEquals(0.0, Polygons.intersectionArea(unitSquare, square(3.0, 3.0, 1.0)), 0.0)
        // Sharing only an edge: "clip to about 1e-16, below the overlap tolerance" (design §3).
        assertEquals(0.0, Polygons.intersectionArea(unitSquare, square(1.0, 0.0, 1.0)), 1e-6)
    }

    // guardrail G-04 — Polygons row: "clipConvex(subject, clipper) is Sutherland–Hodgman"; the clip
    // is the common region. Hand: unit square ∩ offset square = (0.5,0.5)(1,0.5)(1,1)(0.5,1); the ST
    // inside the LT clips to itself. Compared as point sets (duplicates merged at 1e-9), since the
    // row promises neither start corner nor winding of the result.
    @Test
    fun polygons_clipConvex_isTheCommonRegion() {
        val clip = Polygons.clipConvex(unitSquare, square(0.5, 0.5, 1.0))
        assertSamePointSet(
            listOf(kitPt(0.5, 0.5), kitPt(1.0, 0.5), kitPt(1.0, 1.0), kitPt(0.5, 1.0)),
            kitDistinct(clip, 1e-9), 1e-9, "unit square ∩ offset square",
        )
        assertEquals(0.25, Polygons.area(clip), 1e-12)

        val st = listOf(kitPt(1, 0), kitPt(3, 0), kitPt(2, 1))
        val inside = Polygons.clipConvex(st, listOf(kitPt(0, 0), kitPt(4, 0), kitPt(2, 2)))
        assertSamePointSet(st, kitDistinct(inside, 1e-9), 1e-9, "ST inside LT")
    }

    // guardrail G-04 — Polygons row: "overlaps(a, b) is intersectionArea > 1e-6"; TYPE-004:
    // "overlaps" is decided with a tolerance of at most 1e-6. Hand: an overlap strip 1e-3 × 1 has
    // area 1e-3 (a genuine overlap, design §3: real gaps are ≥ about 1e-3); a strip 1e-9 × 1 has
    // area 1e-9 (float noise, not an overlap); an edge-only contact is no overlap. The stored
    // solutions are interior-disjoint (Spec/03 V4), so no pair of their pieces overlaps — including
    // the Chocolate bar's √2 coordinates.
    @Test
    fun polygons_overlaps_usesTheMicroUnitTolerance() {
        assertTrue(Polygons.overlaps(unitSquare, square(0.5, 0.5, 1.0)))
        assertFalse(Polygons.overlaps(unitSquare, square(1.0, 0.0, 1.0)))
        assertFalse(Polygons.overlaps(unitSquare, square(3.0, 3.0, 1.0)))
        assertTrue(Polygons.overlaps(unitSquare, square(1.0 - 1e-3, 0.0, 1.0)))
        assertFalse(Polygons.overlaps(unitSquare, square(1.0 - 1e-9, 0.0, 1.0)))
        for (puzzle in listOf(GIFT_BOX, CHOCOLATE_BAR, MINI_PYRAMID, WARMUP_4)) {
            for (i in puzzle.solution.indices) {
                for (j in i + 1 until puzzle.solution.size) {
                    val a = puzzle.solution[i]
                    val b = puzzle.solution[j]
                    assertFalse("${puzzle.id} ${a.piece} vs ${b.piece}", Polygons.overlaps(a.polygon, b.polygon))
                }
            }
        }
    }

    // ── Silhouette.contains ────────────────────────────────────────────────────────────────────

    // guardrail G-04 — Silhouette row: "contains(poly, solution) is Σ intersectionArea(poly,
    // s.polygon) ≥ area(poly) − 1e-6" (TYPE-004 "inside", tolerance at most 1e-6). Every solution
    // piece lies inside its own silhouette (√2 coordinates and both windings included).
    @Test
    fun silhouette_contains_everySolutionPieceOfEveryFixture() {
        for (puzzle in listOf(GIFT_BOX, CHOCOLATE_BAR, MINI_PYRAMID, WARMUP_4)) {
            for (sp in puzzle.solution) {
                assertTrue("${puzzle.id} ${sp.piece}", Silhouette.contains(sp.polygon, puzzle.solution))
                assertTrue("${puzzle.id} ${sp.piece} reversed", Silhouette.contains(sp.polygon.reversed(), puzzle.solution))
            }
        }
    }

    // guardrail G-04 — Silhouette row (sum of clips against each solution piece; design §3 "inside
    // means the sum of clips against each solution piece"): a polygon spanning several solution
    // pieces is inside. Hand: the Gift box's whole 4 × 4 square (all 7 pieces, area 16); its middle
    // square (1,1)(3,1)(3,3)(1,3); the Mini pyramid's own outline triangle (0,2)(4,2)(2,0) (3 pieces,
    // area 4); in Warm-up 4 the SQ diamond (2,2)(3,1)(4,2)(3,3) whose upper half lies in MT and lower
    // half in LT2.
    @Test
    fun silhouette_contains_polygonsSpanningSeveralPieces() {
        assertTrue("Gift box whole square", Silhouette.contains(square(0.0, 0.0, 4.0), GIFT_BOX.solution))
        assertTrue("Gift box middle square", Silhouette.contains(square(1.0, 1.0, 2.0), GIFT_BOX.solution))
        assertTrue(
            "Mini pyramid outline triangle",
            Silhouette.contains(listOf(kitPt(0, 2), kitPt(4, 2), kitPt(2, 0)), MINI_PYRAMID.solution),
        )
        assertTrue(
            "Warm-up 4 diamond across MT and LT2",
            Silhouette.contains(listOf(kitPt(2, 2), kitPt(3, 1), kitPt(4, 2), kitPt(3, 3)), WARMUP_4.solution),
        )
    }

    // guardrail G-04 — Silhouette row + TYPE-004 "lies completely inside the silhouette": a polygon
    // that sticks out is not inside. Hand: Gift box ST (3,0)(5,0)(4,1) has 0.5 of its area 1 beyond
    // x = 4; the ST (1,0)(3,0)(2,1) moved up 0.01 has about 0.02 above y = 0; the Mini pyramid's
    // bounding box (0,0)–(4,2) has only 4 of 8 inside; in Warm-up 4 the notch (2,2)(3,3)(3,5)(2,4)
    // is covered by no piece (non-convex silhouette), and the ST (1,3)(3,3)(2,4) has its right half
    // (2,3)(3,3)(2,4) in that notch.
    @Test
    fun silhouette_contains_rejectsPolygonsThatStickOut() {
        assertFalse("Gift box ST half out", Silhouette.contains(listOf(kitPt(3, 0), kitPt(5, 0), kitPt(4, 1)), GIFT_BOX.solution))
        assertFalse(
            "Gift box ST 0.01 above the top edge",
            Silhouette.contains(listOf(kitPt(1.0, -0.01), kitPt(3.0, -0.01), kitPt(2.0, 0.99)), GIFT_BOX.solution),
        )
        assertFalse(
            "Mini pyramid bounding box",
            Silhouette.contains(listOf(kitPt(0, 0), kitPt(4, 0), kitPt(4, 2), kitPt(0, 2)), MINI_PYRAMID.solution),
        )
        assertFalse(
            "Warm-up 4 notch",
            Silhouette.contains(listOf(kitPt(2, 2), kitPt(3, 3), kitPt(3, 5), kitPt(2, 4)), WARMUP_4.solution),
        )
        assertFalse(
            "Warm-up 4 ST half over the notch",
            Silhouette.contains(listOf(kitPt(1, 3), kitPt(3, 3), kitPt(2, 4)), WARMUP_4.solution),
        )
    }

    // ── Silhouette.outlineCorners ──────────────────────────────────────────────────────────────

    // guardrail G-04 — ILockEngine.outlineCorners KDoc: "A corner is a point of the silhouette's
    // boundary (outer edge or hole) where the boundary turns; a point where solution pieces meet
    // with a straight (180°) or full (360°) angle is not a corner"; "each outline corner once; order
    // unspecified"; design §2 `outlineCorners` row: "Gift box gives the 4 square corners; mini-1
    // gives (2,0) (0,2) (4,2)". Hand: Gift box (4,2) and (2,4) are straight (180°), (2,2) (3,1)
    // (3,3) (1,3) are full (360°); Chocolate bar (2√2,0) (3√2,0) (2√2,2√2) (3√2,2√2) are straight,
    // (2√2,√2) (3√2,√2) full; Mini pyramid (1,1) (3,1) (2,2) straight.
    @Test
    fun silhouette_outlineCorners_ofTheConvexFixtures() {
        assertSamePointSet(
            listOf(kitPt(0, 0), kitPt(4, 0), kitPt(4, 4), kitPt(0, 4)),
            Silhouette.outlineCorners(GIFT_BOX.solution), 1e-9, "Gift box",
        )
        assertSamePointSet(
            listOf(kitPt(0.0, 0.0), kitPt(kitR2(0, 4), 0.0), kitPt(kitR2(0, 4), kitR2(0, 2)), kitPt(0.0, kitR2(0, 2))),
            Silhouette.outlineCorners(CHOCOLATE_BAR.solution), 1e-9, "Chocolate bar",
        )
        assertSamePointSet(
            listOf(kitPt(2, 0), kitPt(0, 2), kitPt(4, 2)),
            Silhouette.outlineCorners(MINI_PYRAMID.solution), 1e-9, "Mini pyramid",
        )
    }

    // guardrail G-04 — review F1 and the design §2 `outlineCorners` row: "`shapes-warmup-4` includes
    // (2,2)". Derived by hand from the KDoc ("where the boundary turns"): around (2,2), LT2
    // (2,2)(6,2)(4,4) covers directions 0°–45°, MT (2,2)(2,0)(4,2) covers 270°–360°, ST2
    // (2,2)(2,4)(1,3) covers 90°–135° (y down). The covered directions total 180° but in two
    // separate wedges — 45°–90° (the notch) and 135°–270° (outside) are uncovered — so the boundary
    // turns at (2,2): it is a corner, though an angle-sum-only rule would call it straight.
    @Test
    fun silhouette_outlineCorners_warmup4_includesThePinchPoint2_2() {
        val corners = Silhouette.outlineCorners(WARMUP_4.solution)
        assertTrue("(2,2) must be an outline corner of warmup-4; got $corners", corners.any { kitNear(it, kitPt(2, 2), 1e-9) })
    }

    // guardrail G-04 — the same KDoc and the design §2 `Silhouette` row (contiguity rule), applied by
    // hand to all 15 distinct vertices of Warm-up 4. Only (1,3) is not a corner: SQ covers 45°–135°
    // and ST2 315°–45° there, one unbroken 180° arc (it lies on the straight edge (0,4)–(2,2)). The
    // other 14 turn: convex (2,0) (6,2) (6,4) (0,6) (0,4) (4,6); reflex (4,2) 225°, (5,3) 270°,
    // (4,4) 270°, (2,4) 225°, (1,5) 270°; notch corners (3,3) 225°, (3,5) 315°; pinch point (2,2).
    @Test
    fun silhouette_outlineCorners_warmup4_isExactlyTheseFourteen() {
        assertSamePointSet(
            listOf(
                kitPt(2, 0), kitPt(4, 2), kitPt(6, 2), kitPt(2, 2), kitPt(5, 3), kitPt(6, 4), kitPt(4, 4),
                kitPt(4, 6), kitPt(0, 6), kitPt(2, 4), kitPt(0, 4), kitPt(1, 5), kitPt(3, 3), kitPt(3, 5),
            ),
            Silhouette.outlineCorners(WARMUP_4.solution), 1e-9, "Warm-up 4",
        )
    }

    // ── LockRules ──────────────────────────────────────────────────────────────────────────────

    // guardrail G-04 — LockRules row: "LOCK_DISTANCE = 0.65, FLOOR_DP = 30.0, SNUG_BONUS = 0.04,
    // WINDOW = 0.16, EPS = 1e-6, CMP = 1e-9" — the TYPE-004 numbers (R = 0.65 units / 30 dp floor,
    // score −0.04 per exact corner, window 0.16, tolerance 1e-6), owner-confirmed 2026-10-11.
    @Test
    fun lockRules_constants_areTheType004Values() {
        assertEquals(0.65, LockRules.LOCK_DISTANCE, 0.0)
        assertEquals(30.0, LockRules.FLOOR_DP, 0.0)
        assertEquals(0.04, LockRules.SNUG_BONUS, 0.0)
        assertEquals(0.16, LockRules.WINDOW, 0.0)
        assertEquals(1e-6, LockRules.EPS, 0.0)
        assertEquals(1e-9, LockRules.CMP, 0.0)
    }

    // guardrail G-04 — LockRules row: "lockRadius(dpPerUnit) = max(0.65, 30.0 / dpPerUnit)";
    // ILockEngine KDoc: "R = max(0.65 units, 30 dp expressed in units)"; `findLock` row: "dpPerUnit =
    // 100.0 gives R = 0.65; 20.0 gives R = 1.5". Hand: 30/60 = 0.5 < 0.65; 30/40 = 0.75;
    // 30/(30/0.65) = 0.65 (the switch-over scale); 30/1 = 30.
    @Test
    fun lockRules_lockRadius_isTheLargerOfDistanceAndFloor() {
        assertEquals(0.65, LockRules.lockRadius(100.0), 1e-12)
        assertEquals(1.5, LockRules.lockRadius(20.0), 1e-12)
        assertEquals(0.65, LockRules.lockRadius(60.0), 1e-12)
        assertEquals(0.75, LockRules.lockRadius(40.0), 1e-12)
        assertEquals(0.65, LockRules.lockRadius(30.0 / 0.65), 1e-12)
        assertEquals(30.0, LockRules.lockRadius(1.0), 1e-12)
    }
}
