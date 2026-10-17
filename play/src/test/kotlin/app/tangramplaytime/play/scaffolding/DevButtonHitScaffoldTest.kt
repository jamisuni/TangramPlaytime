// scaffolding — TASK-026's own disposable checks of the pure part of S-22Δ2 (no token; the acceptance verdict is the
// device kit's DevAidScreenKitTest): the DEV button's slot comes first in hit order 3, after the top bar's columns
// (order 2), and without a slot the hit test is WO-004's.
package app.tangramplaytime.play.scaffolding

import androidx.compose.ui.geometry.Offset
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.Control
import app.tangramplaytime.play.Hit
import app.tangramplaytime.play.HitTest
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlayLayout
import app.tangramplaytime.play.ScreenGeometry
import app.tangramplaytime.play.ShownControls
import app.tangramplaytime.play.Slot
import app.tangramplaytime.play.badgeSlots
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class DevButtonHitScaffoldTest {

    private val engine = LockEngine()
    private val library = PuzzleLibraries.bundled(engine)
    private val layout = PlayLayout(
        trayRows = listOf(listOf(PieceId.LT1, PieceId.LT2, PieceId.MT), listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2)),
        floatOffsetDp = 30f,
        cellGapDp = 8f,
        sideMarginDp = 8f,
        topBarDp = 66f,
    )
    private val density = 2.625f

    private fun screen(): Pair<PlayController, ScreenGeometry> {
        val controller = PlayController(library, engine)
        controller.open(library.puzzles.first { it.kind == PuzzleKind.FULL }.id)
        val g = ScreenGeometry.of(1080, 2280, density, layout, controller.puzzle)
        controller.boardDpPerUnit = (g.mapping.pxPerUnit / density).toDouble()
        return controller to g
    }

    /** A 20 dp square slot centred on [at]. */
    private fun around(at: Offset): Slot {
        val half = (10f * density).roundToInt()
        return Slot(at.x.roundToInt() - half, at.y.roundToInt() - half, 2 * half, 2 * half)
    }

    @Test
    fun theDevButtonBeatsATrayCellAndTheBadgeDiscButNotTheTopBar() {
        val (controller, g) = screen()
        val margin = 12f * density
        val cell = g.cells.getValue(PieceId.LT1)
        val inCell = Offset(cell.leftPx + cell.width / 2f, cell.topPx + cell.height / 2f)
        assertEquals("without the button: the tray cell", Hit.Tray(PieceId.LT1), HitTest.at(inCell, g, controller, margin, ShownControls.NONE))
        assertEquals(
            "a button slot over the cell: the button",
            Hit.OnControl(Control.DEV_BUTTON),
            HitTest.at(inCell, g, controller, margin, ShownControls(null, false, around(inCell))),
        )
        val disc = badgeSlots(controller, g).tray?.disc ?: throw AssertionError("a tray badge with the PG in the tray")
        assertEquals("without the button: the badge disc", Hit.Badge(onBoard = false), HitTest.at(disc, g, controller, margin, ShownControls.NONE))
        assertEquals(
            "a button slot over the disc: the button",
            Hit.OnControl(Control.DEV_BUTTON),
            HitTest.at(disc, g, controller, margin, ShownControls(null, false, around(disc))),
        )
        val counter = Offset(g.counter.leftPx + g.counter.width / 2f, g.counter.topPx + g.counter.height / 2f)
        assertEquals(
            "a button slot over the counter: the counter (order 2 before order 3)",
            Hit.OnControl(Control.COUNTER),
            HitTest.at(counter, g, controller, margin, ShownControls(null, false, around(counter))),
        )
    }

    @Test
    fun aPointOutsideTheButtonsSlotIsWhatItWasWithoutIt() {
        val (controller, g) = screen()
        val margin = 12f * density
        val cell = g.cells.getValue(PieceId.LT1)
        val slot = Slot(cell.left, cell.top, 4, 4)
        val outside = Offset(cell.leftPx + cell.width - 2f, cell.topPx + cell.height - 2f)
        assertEquals(Hit.Tray(PieceId.LT1), HitTest.at(outside, g, controller, margin, ShownControls(null, false, slot)))
        assertEquals(Hit.OnControl(Control.DEV_BUTTON), HitTest.at(Offset(cell.leftPx + 1f, cell.topPx + 1f), g, controller, margin, ShownControls(null, false, slot)))
    }
}
