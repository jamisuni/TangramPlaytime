// scaffolding (WO-002 TASK-005): parser and edge cases the acceptance kit does not reach — the path
// grammar for L/C/Z (no shipped file uses them), rule attribution of a few malformed texts, and the
// shared-edge measure. Disposable implementer checks; no acceptance token.
package app.tangramplaytime.content.scaffolding

import app.tangramplaytime.content.Edges
import app.tangramplaytime.content.Parsed
import app.tangramplaytime.content.PuzzleParser
import app.tangramplaytime.content.PuzzleSource
import app.tangramplaytime.content.Rule
import app.tangramplaytime.contracts.ArtShape
import app.tangramplaytime.contracts.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserScaffoldTest {
    private val smallTriangle = """{"piece": "ST1", "polygon": [[0, 0], [2, 0], [1, 1]]}"""

    private fun text(solution: String = smallTriangle, shapes: String = ""): PuzzleSource = PuzzleSource(
        "shapes-scaffold.json",
        """{"format": "tangram-puzzle/1", "id": "shapes-scaffold", "title": {"en": "A", "fi": "B"},
           "category": "shapes", "difficulty": 1, "kind": "mini", "solution": [$solution],
           "art": {"base": "#112233", "shapes": [$shapes]}}""",
    )

    private fun path(d: String): String = """{"type": "path", "d": "$d", "stroke": "#000000", "width": 0.1}"""

    private fun rules(src: PuzzleSource): Set<Rule> = when (val parsed = PuzzleParser.parse(src)) {
        is Parsed.Ok -> emptySet()
        is Parsed.Bad -> parsed.violations.map { it.rule }.toSet()
    }

    @Test
    fun theBaseTextParses() {
        val parsed = PuzzleParser.parse(text())
        assertTrue("$parsed", parsed is Parsed.Ok)
    }

    @Test
    fun everyAbsoluteCommandIsReadWithItsNumbers() {
        val d = "M0,0 L1,1 Q1,2 2,2 C2,3 3,3 3,4 Z M-1.5 .5 L1e-1,2E+0 Z"
        val parsed = PuzzleParser.parse(text(shapes = path(d)))
        assertTrue("$parsed", parsed is Parsed.Ok)
        val shape = (parsed as Parsed.Ok).def.art.shapes.single()
        assertEquals(d, (shape as ArtShape.Path).d)
    }

    @Test
    fun aPathOffTheGrammarIsArt() {
        for (d in listOf("", "L0,0 L1,1", "M0,0 L1", "M0,0 L1,1 2,2", "M0,0 H1", "M0,0 l1,1", "M0,0 L1,x")) {
            assertEquals(d, setOf(Rule.ART), rules(text(shapes = path(d))))
        }
    }

    @Test
    fun aLineOrPathWithoutStrokeIsArt() {
        val line = """{"type": "line", "from": [0, 0], "to": [1, 1], "width": 0.1}"""
        val pathNoStroke = """{"type": "path", "d": "M0,0 L1,1", "fill": "#FFFFFF"}"""
        assertEquals(setOf(Rule.ART), rules(text(shapes = line)))
        assertEquals(setOf(Rule.ART), rules(text(shapes = pathNoStroke)))
    }

    @Test
    fun aPointOfThreeNumbersIsArt() {
        val circle = """{"type": "circle", "c": [0, 0, 1], "r": 0.2, "fill": "#FFFFFF"}"""
        assertEquals(setOf(Rule.ART), rules(text(shapes = circle)))
    }

    @Test
    fun anUnknownShapeTypeIsJson() {
        val star = """{"type": "star", "c": [0, 0], "r": 0.2}"""
        assertEquals(setOf(Rule.JSON), rules(text(shapes = star)))
    }

    @Test
    fun anEntryWithoutPolygonOrWithABadCornerIsPlacement() {
        assertEquals(setOf(Rule.PLACEMENT), rules(text(solution = """{"piece": "ST1"}""")))
        assertEquals(setOf(Rule.PLACEMENT), rules(text(solution = """{"piece": "ST1", "polygon": [[0, 0], [2, 0, 1], [1, 1]]}""")))
        assertEquals(setOf(Rule.PLACEMENT), rules(text(solution = """{"piece": "ST1", "polygon": [[0, 0], [2, 0], [[1, 1, 1], 1]]}""")))
        assertEquals(setOf(Rule.PLACEMENT), rules(text(solution = """{"piece": "ST1", "polygon": [[0, 0], [2, 0]]}""")))
        assertEquals(setOf(Rule.PLACEMENT), rules(text(solution = """{"piece": "ST1", "polygon": [[0, 0], [2, 0], [true, 1]]}""")))
    }

    @Test
    fun aBlankTitleIsTitle() {
        val blank = text().let { it.copy(text = it.text.replace("\"fi\": \"B\"", "\"fi\": \" \"")) }
        assertEquals(setOf(Rule.TITLE), rules(blank))
    }

    @Test
    fun sharedLengthCountsOnlyCollinearOverlap() {
        val square = listOf(Point(0.0, 0.0), Point(2.0, 0.0), Point(2.0, 2.0), Point(0.0, 2.0))
        val right = listOf(Point(2.0, 1.0), Point(3.0, 1.0), Point(3.0, 4.0), Point(2.0, 4.0))
        val corner = listOf(Point(2.0, 2.0), Point(3.0, 2.0), Point(3.0, 3.0))
        assertEquals(1.0, Edges.sharedLength(square, right), 1e-12)
        assertEquals(1.0, Edges.sharedLength(right, square), 1e-12)
        assertEquals(0.0, Edges.sharedLength(square, corner), 1e-12)
        assertEquals(8.0, Edges.perimeter(square), 1e-12)
    }
}
