// scaffolding — TASK-012's own disposable checks of the pure parts of S-11 / S-13 / S-14 (no token; the
// acceptance verdict is the device kit's BoardTrayKitTest).
package app.tangramplaytime.play.scaffolding

import androidx.compose.ui.geometry.Offset
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.play.BoardMapping
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.play.PlayTexts
import app.tangramplaytime.play.titleOf
import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.kernel.LockEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenSeamScaffoldTest {

    @Test
    fun boardMappingRoundTripsBetweenUnitsAndPx() {
        val m = BoardMapping(originXPx = 120.5f, originYPx = 300.25f, pxPerUnit = 57.75f)
        val p = Point(1.25, 2.5)
        val px = m.toPx(p)
        assertEquals(120.5f + 1.25f * 57.75f, px.x, 1e-4f)
        assertEquals(300.25f + 2.5f * 57.75f, px.y, 1e-4f)
        val back = m.toUnits(px)
        assertEquals(p.x, back.x, 1e-5)
        assertEquals(p.y, back.y, 1e-5)
        val origin = m.toUnits(Offset(120.5f, 300.25f))
        assertEquals(0.0, origin.x, 1e-9)
        assertEquals(0.0, origin.y, 1e-9)
    }

    @Test
    fun perPieceTagsAppendThePieceName() {
        assertEquals("tray-cell-LT1", PlayTags.trayCell(PieceId.LT1))
        assertEquals("tray-piece-PG", PlayTags.trayPiece(PieceId.PG))
        assertEquals("tray-ghost-ST2", PlayTags.trayGhost(PieceId.ST2))
        assertEquals("size-mark-MT", PlayTags.sizeMark(PieceId.MT))
        assertEquals("board-piece-SQ", PlayTags.boardPiece(PieceId.SQ))
        assertEquals("drag-piece-LT2", PlayTags.dragPiece(PieceId.LT2))
        assertEquals("play-screen", PlayTags.SCREEN)
    }

    @Test
    fun theTitleIsTheRequestedLanguageWithAnEnglishFallback() {
        val puzzle = PuzzleLibraries.bundled(LockEngine()).puzzles.first()
        assertEquals(puzzle.titles.getValue("en"), PlayTexts("S", "M", "L", "en").titleOf(puzzle))
        assertEquals(puzzle.titles.getValue("fi"), PlayTexts("S", "M", "L", "fi").titleOf(puzzle))
        assertEquals(puzzle.titles.getValue("en"), PlayTexts("S", "M", "L", "sv").titleOf(puzzle))
    }
}
