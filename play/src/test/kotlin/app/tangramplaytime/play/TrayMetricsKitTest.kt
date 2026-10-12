// Acceptance kit (WO-003, TASK-009) — the tray's arithmetic (tasks.md § WO-003 › Seams S-6 `TrayMetrics`
// with the S-5 phone literal) and the tray's resting turns (S-9 `TrayRest`), on the JVM.
//
// - Token: REQ-013 A1, on the one test whose assertion is that line's meaning (traceability-rules.md §2).
//   The other tests pin REQ-013's rules, the S-6 row and the S-9 row and carry no token. REQ-013 A2 and
//   A3 are the device kit's (TASK-011): the miniature's drawn size is the screen's, not this seam's.
// - REQ-013's rules give the turn diameters to two decimals (4.22, 3.16, 2.98, 2.11, 2.00 units); the
//   exact diameters differ from them by less than 0.005 units, so a width is compared within
//   0.005 units × the scale + 0.01 dp.
package app.tangramplaytime.play

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.contracts.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TrayMetricsKitTest {

    /** The full set, in tray order (TYPE-001; PieceId declaration order). */
    private val kitFullSet: Set<PieceId> = PieceId.entries.toCollection(LinkedHashSet())

    /** REQ-013 rule: "turn diameters in units: large triangle 4.22, parallelogram 3.16, medium triangle 2.98, small triangle 2.11, square 2.00". */
    private val kitTurnDiameter: Map<PieceType, Double> = mapOf(
        PieceType.LT to 4.22,
        PieceType.PG to 3.16,
        PieceType.MT to 2.98,
        PieceType.ST to 2.11,
        PieceType.SQ to 2.00,
    )

    /** Phone screens (TYPE-007: phone below 600 dp wide), width × height in dp; the first is REQ-013 A1's. */
    private val kitPhones: List<Pair<Float, Float>> = listOf(
        360f to 780f,
        320f to 568f,
        393f to 851f,
        411f to 914f,
        480f to 854f,
    )

    /** Piece sets: the full set, every shipped puzzle's own set, and every single piece. */
    private fun kitPieceSets(): List<Set<PieceId>> =
        listOf(kitFullSet) + kitLibrary.puzzles.map { it.pieces } + PieceId.entries.map { setOf(it) }

    private fun kitLayout(width: Float, height: Float, pieces: Set<PieceId>): TrayGeometry =
        TrayMetrics.layout(width, height, KIT_PHONE_LAYOUT, pieces)

    // ── REQ-013 A1 ─────────────────────────────────────────────────────────────────────────────

    // REQ-013.A1 — "On a 360 × 780 dp phone the smallest tray cell is at least 56 dp in both directions."
    // S-6 at 360 × 780 dp with the S-5 phone literal ("widthDp / heightDp are the screen composable's
    // size"): the smallest cell of the full set's tray, and of every shipped puzzle's own tray, is at least
    // 56 dp wide and 56 dp tall.
    @Test
    fun onA360By780DpPhoneTheSmallestTrayCellIsAtLeast56DpInBothDirections() { // REQ-013.A1
        val full = kitLayout(360f, 780f, kitFullSet)
        assertEquals("fixture: a cell for each of the seven pieces", kitFullSet, full.cells.keys)
        for (pieces in listOf(kitFullSet) + kitLibrary.puzzles.map { it.pieces }) {
            val g = kitLayout(360f, 780f, pieces)
            assertEquals("fixture: a cell for each piece of $pieces", pieces, g.cells.keys)
            for ((id, cell) in g.cells) {
                assertTrue("$pieces: $id cell ${cell.widthDp} dp wide", cell.widthDp >= 56f)
                assertTrue("$pieces: $id cell ${cell.heightDp} dp tall", cell.heightDp >= 56f)
            }
        }
    }

    // ── REQ-013's rules and the S-6 row (pins) ─────────────────────────────────────────────────

    // REQ-013 rule — "Cell width = the piece's turn diameter × scale + 12 dp padding"; seam S-6 — "cells are
    // square". Pin, no token.
    @Test
    fun eachCellIsSquareAndAsWideAsItsTurnDiameterAtTheScalePlusTwelveDp() {
        for ((w, h) in kitPhones) {
            val g = kitLayout(w, h, kitFullSet)
            assertTrue("$w × $h: a positive scale, was ${g.scaleDpPerUnit}", g.scaleDpPerUnit > 0f)
            for ((id, cell) in g.cells) {
                assertEquals("$w × $h: $id cell is square", cell.widthDp, cell.heightDp, 1e-3f)
                val expected = kitTurnDiameter.getValue(id.type) * g.scaleDpPerUnit + 12.0
                val tol = 0.005 * g.scaleDpPerUnit + 0.01
                assertEquals("$w × $h: $id cell width at scale ${g.scaleDpPerUnit}", expected, cell.widthDp.toDouble(), tol)
            }
        }
    }

    // REQ-013 rule — "The common scale is the largest one at which every tray row of the full set fits the
    // screen width and each row is at most max(84 dp, 16 % of the screen height) tall." A row is as tall as
    // its tallest cell at least, so every cell is within the height limit; every cell lies within the
    // screen width (tray-local x from 0, S-6). Pin, no token.
    @Test
    fun theFullSetsRowsFitTheScreenWidthAndTheRowHeightLimit() {
        for ((w, h) in kitPhones) {
            val g = kitLayout(w, h, kitFullSet)
            val limit = maxOf(84f, 0.16f * h)
            for ((id, cell) in g.cells) {
                assertTrue("$w × $h: $id cell starts at x ${cell.xDp}", cell.xDp >= -1e-3f)
                assertTrue("$w × $h: $id cell ends at x ${cell.xDp + cell.widthDp}, past $w", cell.xDp + cell.widthDp <= w + 1e-3f)
                assertTrue("$w × $h: $id cell ${cell.heightDp} dp tall, over the row limit $limit", cell.heightDp <= limit + 1e-3f)
            }
        }
    }

    // REQ-013 rule — "It is computed for the full set even when a puzzle uses fewer pieces, so a piece has the
    // same miniature size in every puzzle"; seam S-6 — "the scale comes from the full set's rows whatever
    // pieces holds". The scale, and each piece's cell size, are the same for every piece set. (REQ-013 A3's
    // token is the device kit's, on the drawn miniature.) Pin, no token.
    @Test
    fun theScaleAndEachPiecesCellSizeAreTheSameForEveryPieceSet() {
        for ((w, h) in kitPhones) {
            val full = kitLayout(w, h, kitFullSet)
            for (pieces in kitPieceSets()) {
                val g = kitLayout(w, h, pieces)
                assertEquals("$w × $h, $pieces: the scale", full.scaleDpPerUnit, g.scaleDpPerUnit, 1e-4f)
                for (id in pieces) {
                    val cell = g.cells[id] ?: throw AssertionError("$w × $h, $pieces: no cell for $id")
                    val ref = full.cells.getValue(id)
                    assertEquals("$w × $h, $pieces: $id cell width", ref.widthDp, cell.widthDp, 1e-3f)
                    assertEquals("$w × $h, $pieces: $id cell height", ref.heightDp, cell.heightDp, 1e-3f)
                }
            }
        }
    }

    // seam S-6 — "cells holds exactly pieces, in the layout's rows with empty rows dropped (REQ-012 rule)";
    // REQ-045 statement — "the tray SHALL show only that puzzle's pieces"; TYPE-001 — "Tray order, left to
    // right and top to bottom"; REQ-012 rule — a mini "shows only its own pieces, in the same relative
    // order". In each S-5 row a piece's cell lies left of the next one's; every cell of an earlier
    // (non-empty) row lies above every cell of a later one; no two cells overlap. Pin, no token.
    @Test
    fun cellsHoldExactlyThePiecesLeftToRightAndTopToBottomWithoutOverlap() {
        val tol = 1e-3f
        for ((w, h) in kitPhones) {
            for (pieces in kitPieceSets()) {
                val g = kitLayout(w, h, pieces)
                val tag = "$w × $h, $pieces"
                assertEquals("$tag: exactly the pieces", pieces, g.cells.keys)
                val rows = KIT_PHONE_LAYOUT.trayRows.map { row -> row.filter { it in pieces } }.filter { it.isNotEmpty() }
                for (row in rows) {
                    for (i in 0 until row.size - 1) {
                        val left = g.cells.getValue(row[i])
                        val right = g.cells.getValue(row[i + 1])
                        assertTrue("$tag: ${row[i]} left of ${row[i + 1]}", left.xDp + left.widthDp <= right.xDp + tol)
                    }
                }
                for (i in rows.indices) for (j in i + 1 until rows.size) {
                    for (upper in rows[i]) for (lower in rows[j]) {
                        val u = g.cells.getValue(upper)
                        val l = g.cells.getValue(lower)
                        assertTrue("$tag: $upper above $lower", u.yDp + u.heightDp <= l.yDp + tol)
                    }
                }
                val all = g.cells.entries.toList()
                for (i in all.indices) for (j in i + 1 until all.size) {
                    val a = all[i].value
                    val b = all[j].value
                    val apart = a.xDp + a.widthDp <= b.xDp + tol || b.xDp + b.widthDp <= a.xDp + tol ||
                        a.yDp + a.heightDp <= b.yDp + tol || b.yDp + b.heightDp <= a.yDp + tol
                    assertTrue("$tag: ${all[i].key} and ${all[j].key} overlap", apart)
                }
            }
        }
    }

    // ── S-9 — the resting turns ────────────────────────────────────────────────────────────────

    // seam S-9 + TYPE-001 rule — "the resting turn in the tray is long side down and pointing up for every
    // triangle, upright for the square, and leaning right for the parallelogram"; S-9: "never mirrored";
    // decision decisions.md line 65 (the resting turns live once, in `TrayRest`). Checked through
    // `engine.vertices` (y down, Point KDoc), not against S-9's step numbers (S-9: "a pin that disagrees
    // corrects this row"). Discrimination: the next turn, and the mirrored parallelogram,
    // fail the same reading. Pin, no token.
    @Test
    fun theRestingTurnsAreLongSideDownPointingUpUprightAndLeaningRight() {
        val origin = Point(0.0, 0.0)
        for (id in PieceId.entries) {
            val rest = TrayRest.turn(id)
            val corners = kitEngine.vertices(id, rest, false, origin)
            assertTrue("$id at turn ${rest.step}: $corners", kitRestsAsType001Says(id, corners))
            val off = kitEngine.vertices(id, rest.next(), false, origin)
            assertFalse("fixture: $id one step off is not at rest: $off", kitRestsAsType001Says(id, off))
        }
        val mirrored = kitEngine.vertices(PieceId.PG, TrayRest.turn(PieceId.PG), true, origin)
        assertFalse("fixture: the mirrored parallelogram does not lean right: $mirrored", kitRestsAsType001Says(PieceId.PG, mirrored))
    }

    /** TYPE-001's resting-turn words, read on the corners (y down): see the test above. */
    private fun kitRestsAsType001Says(id: PieceId, corners: List<Point>): Boolean = when (id.type) {
        PieceType.LT, PieceType.MT, PieceType.ST -> kitLongSideDownPointingUp(corners)
        PieceType.SQ -> kitUpright(corners)
        PieceType.PG -> kitLeaningRight(corners)
    }

    private fun kitSame(a: Double, b: Double): Boolean = abs(a - b) <= 1e-9

    /** A triangle whose longest side is horizontal at the bottom (largest y) and whose third corner is above it, between its ends. */
    private fun kitLongSideDownPointingUp(c: List<Point>): Boolean {
        if (c.size != 3) return false
        val sides = listOf(0 to 1, 1 to 2, 0 to 2)
        val (i, j) = sides.maxByOrNull { (a, b) -> kitDist2(c[a], c[b]) } ?: return false
        val apex = c[(0..2).first { it != i && it != j }]
        val baseY = c[i].y
        return kitSame(c[i].y, c[j].y) &&
            apex.y < baseY - 1e-6 &&
            apex.x > minOf(c[i].x, c[j].x) + 1e-6 && apex.x < maxOf(c[i].x, c[j].x) - 1e-6
    }

    /** A square with horizontal and vertical sides: its corners take exactly two x values and two y values. */
    private fun kitUpright(c: List<Point>): Boolean =
        c.size == 4 && kitDistinct(c.map { it.x }).size == 2 && kitDistinct(c.map { it.y }).size == 2

    /** A parallelogram with a horizontal pair of sides whose upper side is shifted right of the lower one. */
    private fun kitLeaningRight(c: List<Point>): Boolean {
        if (c.size != 4) return false
        val ys = kitDistinct(c.map { it.y })
        if (ys.size != 2) return false
        val upperY = ys.minOrNull() ?: return false
        val upper = c.filter { kitSame(it.y, upperY) }
        val lower = c.filter { !kitSame(it.y, upperY) }
        if (upper.size != 2 || lower.size != 2) return false
        return upper.sumOf { it.x } / 2 > lower.sumOf { it.x } / 2 + 1e-6
    }

    private fun kitDist2(a: Point, b: Point): Double = (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)

    private fun kitDistinct(values: List<Double>): List<Double> {
        val out = mutableListOf<Double>()
        for (v in values) if (out.none { kitSame(it, v) }) out += v
        return out
    }
}
