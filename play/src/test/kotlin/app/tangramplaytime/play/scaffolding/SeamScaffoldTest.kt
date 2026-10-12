// scaffolding (WO-003 TASK-010): the implementer's own checks of S-6, S-7 and S-9 against the words of
// TYPE-001 / REQ-013 / REQ-017 and the recorded decisions — disposable; no REQ token.
package app.tangramplaytime.play.scaffolding

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PieceType
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.PlayLayout
import app.tangramplaytime.play.TrayMetrics
import app.tangramplaytime.play.TrayRest
import app.tangramplaytime.play.TwistTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SeamScaffoldTest {

    private val engine = LockEngine()

    private val phone = PlayLayout(
        trayRows = listOf(
            listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
            listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
        ),
        floatOffsetDp = 30f,
        cellGapDp = 8f,
        sideMarginDp = 8f,
        topBarDp = 66f,
    )

    // S-9 through engine.vertices (y down): TYPE-001 "long side down and pointing up for every triangle,
    // upright for the square, and leaning right for the parallelogram".
    @Test
    fun theRestingTurnsMatchType001ThroughTheEngine() {
        val origin = Point(0.0, 0.0)
        for (id in PieceId.entries) {
            val c = engine.vertices(id, TrayRest.turn(id), false, origin)
            when (id.type) {
                PieceType.LT, PieceType.MT, PieceType.ST -> {
                    val bottom = c.maxOf { it.y }
                    val base = c.filter { abs(it.y - bottom) < 1e-9 }
                    assertEquals("$id: two corners on the bottom line", 2, base.size)
                    val apex = c.first { abs(it.y - bottom) >= 1e-9 }
                    val baseLength = abs(base[0].x - base[1].x)
                    val otherSides = base.map { b -> Math.hypot(b.x - apex.x, b.y - apex.y) }
                    assertTrue("$id: the bottom side is the longest", otherSides.all { it < baseLength - 1e-9 })
                    assertTrue("$id: the apex is above the base, between its ends", apex.y < bottom && apex.x > base.minOf { it.x } && apex.x < base.maxOf { it.x })
                }
                PieceType.SQ -> {
                    assertEquals("$id: two x values", 2, c.map { Math.round(it.x * 1e6) }.toSet().size)
                    assertEquals("$id: two y values", 2, c.map { Math.round(it.y * 1e6) }.toSet().size)
                }
                PieceType.PG -> {
                    val top = c.minOf { it.y }
                    val upper = c.filter { abs(it.y - top) < 1e-9 }
                    val lower = c.filter { abs(it.y - top) >= 1e-9 }
                    assertEquals("$id: a horizontal upper side", 2, upper.size)
                    assertTrue("$id: the upper side is shifted right (leans right)", upper.sumOf { it.x } > lower.sumOf { it.x } + 1e-9)
                }
            }
        }
    }

    // Decision: a twist exactly on a step boundary counts as having passed it, in both directions.
    @Test
    fun aTwistExactlyOnABoundaryCountsAsPassed() {
        for (start in listOf(0.0, 100.0, -170.0)) {
            for (k in 0..3) {
                val boundary = 22.5 + 45.0 * k
                for (dir in listOf(1, -1)) {
                    val tracker = TwistTracker(start)
                    var angle = start
                    var steps = 0
                    val samples = (boundary / 2.5).toInt()
                    repeat(samples) {
                        angle += dir * 2.5
                        steps = tracker.steps(wrap(angle))
                    }
                    assertEquals("from $start°, ${dir * boundary}° exactly", dir * (k + 1), steps)
                }
            }
        }
    }

    private fun wrap(deg: Double): Double {
        var a = deg % 360.0
        if (a <= -180.0) a += 360.0
        if (a > 180.0) a -= 360.0
        return a
    }

    // Decision: a mini's rows are reflowed (empty rows dropped), each row centred in the width; cells sit
    // vertically centred in a row as tall as its tallest cell; the strip is the rows plus a gap above,
    // between and below. DI-2 §3's measured phone value: 390 × 844 → scale ≈ 26.8, smallest cell ≈ 66 dp.
    @Test
    fun theTrayFollowsTheDi2MeasuredValuesAndCentresEachRow() {
        val full = TrayMetrics.layout(390f, 844f, phone, PieceId.entries.toSet())
        assertEquals("DI-2 measured scale", 26.8f, full.scaleDpPerUnit, 0.1f)
        assertEquals("DI-2 smallest cell", 66f, full.cells.values.minOf { it.widthDp }, 1f)
        val row1 = listOf(PieceId.LT1, PieceId.LT2, PieceId.MT).map { full.cells.getValue(it) }
        val row1Width = row1.sumOf { it.widthDp.toDouble() } + 2 * phone.cellGapDp
        assertEquals("row 1 centred", (390.0 - row1Width) / 2, row1.first().xDp.toDouble(), 1e-3)
        val rowHeights = listOf(full.cells.getValue(PieceId.LT1).heightDp, full.cells.getValue(PieceId.PG).heightDp)
        assertEquals("strip height", rowHeights.sum() + 3 * phone.cellGapDp, full.heightDp, 1e-3f)
        assertEquals("MT vertically centred in row 1", (rowHeights[0] - full.cells.getValue(PieceId.MT).heightDp) / 2 + phone.cellGapDp, full.cells.getValue(PieceId.MT).yDp, 1e-3f)

        val mini = TrayMetrics.layout(390f, 844f, phone, setOf(PieceId.SQ, PieceId.ST1, PieceId.ST2))
        assertEquals("one row, dropped the empty one", setOf(PieceId.SQ, PieceId.ST1, PieceId.ST2), mini.cells.keys)
        // ST (2.11 units) is the tallest cell of this row, so it sits at the top gap and the square is centred below it.
        val st = mini.cells.getValue(PieceId.ST1)
        val sq = mini.cells.getValue(PieceId.SQ)
        assertEquals("the one row starts at the top gap", phone.cellGapDp, st.yDp, 1e-3f)
        assertEquals("the square is centred in the row", phone.cellGapDp + (st.heightDp - sq.heightDp) / 2, sq.yDp, 1e-3f)
        assertEquals("the mini strip holds one row", st.heightDp + 2 * phone.cellGapDp, mini.heightDp, 1e-3f)
    }
}
