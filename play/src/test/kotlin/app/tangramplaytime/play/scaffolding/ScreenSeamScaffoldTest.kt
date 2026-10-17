// scaffolding — TASK-012's own disposable checks of the pure parts of S-11 / S-13 / S-14 (no token; the
// acceptance verdict is the device kit's BoardTrayKitTest). WO-004 TASK-020 grew it for S-11Δ and S-13Δ (the
// verdict there is the device kit's TopBarKitTest, ResumeScreenKitTest and SolvedViewKitTest).
package app.tangramplaytime.play.scaffolding

import androidx.compose.ui.geometry.Offset
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.play.BoardMapping
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.play.PlayTexts
import app.tangramplaytime.play.counterOf
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
        assertEquals(puzzle.titles.getValue("en"), texts("en").titleOf(puzzle))
        assertEquals(puzzle.titles.getValue("fi"), texts("fi").titleOf(puzzle))
        assertEquals(puzzle.titles.getValue("en"), texts("sv").titleOf(puzzle))
    }

    // WO-004 TASK-020 (S-11Δ, S-13Δ): the counter's text and the new tag strings.

    @Test
    fun theCounterReadsTheNumberOutOfTheTotal() {
        assertEquals("3 / 13", texts("en").counterOf(3, 13))
    }

    @Test
    fun theWoFourTagsAreTheDesignsStrings() {
        assertEquals(
            listOf("nav-prev", "nav-next", "counter", "rating", "state-word", "restart", "solved-row", "retry", "best-time", "solved-next", "overview", "overview-done", "overview-cell-cat", "play-loading"),
            listOf(
                PlayTags.NAV_PREV, PlayTags.NAV_NEXT, PlayTags.COUNTER, PlayTags.RATING, PlayTags.STATE_WORD, PlayTags.RESTART,
                PlayTags.SOLVED_ROW, PlayTags.RETRY, PlayTags.BEST_TIME, PlayTags.SOLVED_NEXT, PlayTags.OVERVIEW,
                PlayTags.OVERVIEW_DONE, PlayTags.overviewCell("cat"), PlayTags.PLAY_LOADING,
            ),
        )
    }

    private fun texts(language: String) = PlayTexts(
        sizeSmall = "S", sizeMedium = "M", sizeLarge = "L", titleLanguage = language,
        restart = "Restart", retry = "Retry", next = "Next", bestTime = "Best time", noTime = "–",
        inProgress = "in progress", solved = "solved", counterFormat = "%1\$d / %2\$d", allPuzzles = "All puzzles", done = "Done",
    )
}
