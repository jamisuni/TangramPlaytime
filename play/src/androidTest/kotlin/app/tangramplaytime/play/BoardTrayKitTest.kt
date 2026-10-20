// Acceptance kit (WO-003, TASK-011): the static screen — the flat silhouette board and the fixed tray
// (tasks.md § WO-003 › Seams S-10…S-14 and the tag table). Each tokened test asserts the meaning of its
// acceptance line on the drawn screen; pins of a seam row, a decision or a rule carry no token.
// REQ-011 A1 is a pure pixel test over `BoardMapping`, `PlayPalette` and the solution polygons: on the
// floor image it is the proof that no anti-aliasing seam shows between neighbouring solution pieces.
package app.tangramplaytime.play

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class BoardTrayKitTest {

    @get:Rule
    val rule = createComposeRule()

    private val kit by lazy { KitScreen(rule) }

    // ── REQ-011: the empty silhouette board ────────────────────────────────────────────────────────────

    @Test // REQ-011.A1
    fun noInnerLineOrSlotOutlineIsVisibleOnAnUnsolvedPuzzle() {
        // REQ-011 A1: "No inner line or slot outline is visible on an unsolved puzzle." Rule: "One colour for
        // the whole silhouette." Oracle: every pixel more than 3 px inside the silhouette (the union of the
        // solution polygons through BoardMapping) is the silhouette colour — including the pixels on the
        // edges the solution pieces share, where an inner line or an anti-aliasing seam would show.
        // Amended WO-010 (F3, recirculation 6, a look-alike; a corrected check — decisions.md 2026-10-20 "F3's cause is the
        // REQ-023 A1 leg's inside mask, not the product or the data"): "more than 3 px inside" is measured from the exact outline
        // (`kit.silhouetteBands`), no longer as the polygons widened 0.75 px and eroded by a 7 × 7 square. At a 315° notch that
        // mask held pixels 0.46 px from the outline, whose anti-aliased edge blend with the board may differ from the silhouette
        // colour by more than the tolerance: a false red where grid and notch fall badly. The rule is unchanged.
        kit.show()
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            val board = kit.capture(PlayTags.BOARD)
            val deep = kit.silhouetteBands(p).inside
            assertTrue("${p.id}: the silhouette fixture is not empty", deep.count() > 1000)
            var wrong = 0
            var first = ""
            deep.forEachSet { x, y ->
                val c = board[x, y]
                if (!kitNear(c, PlayPalette.silhouette)) {
                    wrong++
                    if (first.isEmpty()) first = "($x, $y) = ${kitHex(c)}"
                }
            }
            assertEquals("${p.id}: pixels inside the silhouette that are not ${kitHex(PlayPalette.silhouette)}; first at $first", 0, wrong)

            // The shared edges themselves: the fixture reads them against the geometry (they lie in the inside
            // set: a centre on an edge two pieces share is in the union, `kitBands`), then every one is the one colour.
            val origin = kit.bounds(PlayTags.BOARD).topLeft
            // Amended WO-010 (recirculation 3, a look-alike of F2; a corrected check — run time only): `BoardMappingKey` is read
            // once per puzzle, on this board under the paused clock, instead of one semantics read per sample and per edge
            // (`kit.toPx`, `kit.pxPerUnit`, each waiting for a frame). Nothing moves between the samples: every point maps as before.
            val mapping = kit.mapping()
            var seamSamples = 0
            for (seg in kitInternalEdges(p)) {
                for (q in kitSamplesAlong(seg, 4.0 / mapping.pxPerUnit)) {
                    val px = mapping.toPx(q) - origin
                    val x = px.x.toInt()
                    val y = px.y.toInt()
                    if (!deep[x, y]) continue
                    seamSamples++
                    assertTrue("${p.id}: a seam pixel at ($x, $y) on the edge $seg is ${kitHex(board[x, y])}", kitNear(board[x, y], PlayPalette.silhouette))
                }
            }
            assertTrue("${p.id}: the fixture samples the shared edges ($seamSamples)", seamSamples >= 20)
        }
    }

    @Test // REQ-011.A2
    fun theSilhouetteOutlineMatchesThePuzzlesSolutionOutlineExactly() {
        // REQ-011 A2: "The silhouette outline matches the puzzle's solution outline exactly." Oracle, over every
        // puzzle: inside the union of the solution polygons (3 px in) every pixel is the silhouette colour;
        // outside it (3 px out) no pixel is; and the `silhouette` node's box is the union's box (tag table).
        // Amended WO-010 (F3, recirculation 6, a look-alike; a corrected check — decisions.md 2026-10-20 "F3's cause is the
        // REQ-023 A1 leg's inside mask, not the product or the data"): "3 px in" and "3 px out" are measured from the exact
        // outline (`kit.silhouetteBands`), no longer as the polygons widened 0.75 px, then eroded or dilated by a 7 × 7 square.
        // That inside set held pixels 0.46 px from the outline at a 315° notch, whose anti-aliased blend may miss the tolerance
        // (a false red); that outside set never judged the pixels 3–3.8 px off an edge (to 5 px off a 45° one), nor a notch's tip
        // out to 11–13 px, where a silhouette bulging past the solution outline would show (decisions.md 2026-10-12 "The solve,
        // as drawn": "so a concave 45° notch is not bulged"): a hollow check. The rule is unchanged.
        kit.show()
        for (p in kitLibrary.puzzles) {
            kit.open(p.id)
            val board = kit.capture(PlayTags.BOARD)
            val bands = kit.silhouetteBands(p)
            val inside = bands.inside
            val outside = bands.outside
            assertTrue("${p.id}: the inside fixture is not empty", inside.count() > 1000)
            assertTrue("${p.id}: the outside fixture is not empty", outside.count() > 1000)
            var missing = 0
            var firstMissing = ""
            inside.forEachSet { x, y ->
                if (!kitNear(board[x, y], PlayPalette.silhouette)) { missing++; if (firstMissing.isEmpty()) firstMissing = "($x, $y) = ${kitHex(board[x, y])}" }
            }
            var spill = 0
            var firstSpill = ""
            outside.forEachSet { x, y ->
                if (kitNear(board[x, y], PlayPalette.silhouette)) { spill++; if (firstSpill.isEmpty()) firstSpill = "($x, $y)" }
            }
            assertEquals("${p.id}: pixels inside the solution outline not painted as silhouette; first $firstMissing", 0, missing)
            assertEquals("${p.id}: silhouette pixels outside the solution outline; first $firstSpill", 0, spill)

            val all = p.solution.flatMap { it.polygon }
            val expected = Rect(
                kit.toPx(Point(all.minOf { it.x }, all.minOf { it.y })),
                kit.toPx(Point(all.maxOf { it.x }, all.maxOf { it.y })),
            )
            kitAssertBoxNear("${p.id}: `silhouette` node box vs the solution outline's box", expected, kit.bounds(PlayTags.SILHOUETTE), 2f)
        }
    }

    // ── REQ-012: the fixed tray ────────────────────────────────────────────────────────────────────────

    @Test // REQ-012.A1
    fun twoDifferentFullSetPuzzlesShowIdenticalTraysAtTheirStart() {
        // REQ-012 A1: "Two different full-set puzzles show identical trays at their start." Statement: the
        // pieces wait "in the order, colours and resting turns of TYPE-001". Oracle: for every full-set puzzle
        // against the first one, every tray node (cells, miniatures with their turn, mirror and scale, size
        // marks, the badge) has the same bounds and keys, and the tray strip's pixels are the same.
        kit.show()
        val fullSet = kitFullSetPuzzles()
        assertTrue("the library lists at least two full-set puzzles", fullSet.size >= 2)
        val reference = fullSet.first()
        kit.open(reference.id)
        val refNodes = trayFingerprint()
        val refPixels = kit.capture(PlayTags.TRAY)
        for (p in fullSet.drop(1)) {
            kit.open(p.id)
            assertEquals("${p.id} vs ${reference.id}: the tray's nodes", refNodes, trayFingerprint())
            val pixels = kit.capture(PlayTags.TRAY)
            assertEquals("${p.id}: tray capture width", refPixels.width, pixels.width)
            assertEquals("${p.id}: tray capture height", refPixels.height, pixels.height)
            var differing = 0
            var first = ""
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                if (!kitNear(refPixels[x, y], pixels[x, y], 3f / 255f)) { differing++; if (first.isEmpty()) first = "($x, $y): ${kitHex(refPixels[x, y])} vs ${kitHex(pixels[x, y])}" }
            }
            assertEquals("${p.id} vs ${reference.id}: differing tray pixels; first $first", 0, differing)
        }
    }

    @Test // pin: REQ-012 statement "in the order … of TYPE-001"; TYPE-001 "Tray order, left to right and top to bottom: LT1, LT2, MT, SQ, PG, ST1, ST2"; S-9 resting turns; no token
    fun theTrayOrderAndTheRestingTurnsAreTypeOnes() {
        kit.show()
        kit.open(kitFirst(PuzzleKind.FULL).id)
        val cells = PieceId.entries.associateWith { kit.bounds(PlayTags.trayCell(it)) }
        // Reading order: the S-5 rows top to bottom (every cell of a row sits above every cell of the next;
        // cells are square and may differ in height, so no shared edge is assumed), left to right within a row.
        val rows = kitPhoneLayout.trayRows
        for (i in 0 until rows.size - 1) {
            val lowest = rows[i].maxOf { cells.getValue(it).center.y }
            val highest = rows[i + 1].minOf { cells.getValue(it).center.y }
            assertTrue("row ${i + 1} (${rows[i]}) is above row ${i + 2} (${rows[i + 1]})", lowest < highest)
        }
        val inReadingOrder = rows.flatMap { row -> row.sortedBy { cells.getValue(it).left } }
        assertEquals(PieceId.entries.toList(), inReadingOrder)
        for (piece in PieceId.entries) {
            val tag = PlayTags.trayPiece(piece)
            assertEquals("$piece resting turn (S-9 TrayRest)", TrayRest.turn(piece), kit.key(tag, PlaySemantics.TurnKey))
            assertFalse("$piece not mirrored at rest", kit.key(tag, PlaySemantics.MirroredKey))
            assertEquals("$piece PieceKey", piece, kit.key(tag, PlaySemantics.PieceKey))
            assertTrue("$piece miniature inside its cell", cells.getValue(piece).contains(kit.bounds(tag).center))
        }
    }

    @Test // pin: S-11 "The title is puzzle.titles[titleLanguage]"; REQ-012 rule "The puzzle's title is shown"; no token
    fun theTitleShowsThePuzzlesTitleInTheKitsLanguage() {
        kit.show()
        for (p in listOf(kitFirst(PuzzleKind.MINI), kitFirst(PuzzleKind.FULL))) {
            kit.open(p.id)
            rule.onNodeWithTag(PlayTags.TITLE).assertIsDisplayed()
            assertEquals(p.titles.getValue("en"), kit.text(PlayTags.TITLE))
        }
    }

    // ── REQ-013: one tray scale ────────────────────────────────────────────────────────────────────────

    @Test // REQ-013.A3
    fun theSamePieceHasTheSameMiniatureSizeInAMiniPuzzleAndInAFullSetPuzzle() {
        // REQ-013 A3: "The same piece has the same miniature size in a mini puzzle and in a full-set puzzle."
        // Rule: the scale "is computed for the full set even when a puzzle uses fewer pieces". Oracle: for
        // every mini puzzle and each of its pieces, the miniature's drawn scale and its box size equal the
        // full-set puzzle's; the scale (the product's claim) is read against the box through the engine's corners.
        kit.show()
        val full = kitFirst(PuzzleKind.FULL)
        kit.open(full.id)
        val fullSizes = PieceId.entries.associateWith { miniatureSize(it) }
        val minis = kitLibrary.puzzles.filter { it.kind == PuzzleKind.MINI }
        assertTrue("the library lists a mini puzzle", minis.isNotEmpty())
        for (mini in minis) {
            kit.open(mini.id)
            for (piece in mini.pieces) {
                val (scale, box) = miniatureSize(piece)
                val (fullScale, fullBox) = fullSizes.getValue(piece)
                kitAssertNear("${mini.id} vs ${full.id}: $piece drawn px per unit", fullScale.toDouble(), scale.toDouble(), 0.01)
                kitAssertNear("${mini.id} vs ${full.id}: $piece miniature width", fullBox.width.toDouble(), box.width.toDouble(), 1.0)
                kitAssertNear("${mini.id} vs ${full.id}: $piece miniature height", fullBox.height.toDouble(), box.height.toDouble(), 1.0)
            }
        }
    }

    // ── REQ-045: the fresh start (the copy under play/, decisions.md "#PuzzleMini → play/") ───────────

    @Test // REQ-045.A1
    fun aFreshControllerOpensOnAMiniPuzzleWhoseTrayHoldsThreePieces() {
        // REQ-045 A1: "A fresh install opens on a mini puzzle whose tray holds three pieces." On the screen, a
        // fresh `PlayController` is the fresh start (S-1: construction opens the first listed puzzle). Statement:
        // "WHILE a mini puzzle is open, the tray SHALL show only that puzzle's pieces."
        kit.show()
        val id = kit.key(PlayTags.SCREEN, PlaySemantics.PuzzleIdKey)
        val opened = kitLibrary.byId(id) ?: throw AssertionError("`play-screen` names a puzzle the library does not list: $id")
        assertEquals("$id is a mini puzzle", PuzzleKind.MINI, opened.kind)
        assertEquals("$id uses three pieces", 3, opened.pieces.size)
        val inTray = PieceId.entries.filter { kit.exists(PlayTags.trayPiece(it)) }.toSet()
        assertEquals("the tray holds exactly the puzzle's three pieces", opened.pieces, inTray)
        assertEquals("one cell per piece of the puzzle", opened.pieces, PieceId.entries.filter { kit.exists(PlayTags.trayCell(it)) }.toSet())
        for (piece in inTray) rule.onNodeWithTag(PlayTags.trayPiece(piece), useUnmergedTree = true).assertIsDisplayed()
        assertTrue("nothing on the board on a fresh start", PieceId.entries.none { kit.exists(PlayTags.boardPiece(it)) })
        assertEquals("a fresh puzzle is New (TYPE-006)", PuzzleState.NEW, kit.key(PlayTags.SCREEN, PlaySemantics.PuzzleStateKey))
    }

    // ── helpers of this file ───────────────────────────────────────────────────────────────────────────

    /** The miniature's drawn scale and its box, with the scale read against the box through `vertices`. */
    private fun miniatureSize(piece: PieceId): Pair<Float, Rect> {
        val tag = PlayTags.trayPiece(piece)
        val scale = kit.key(tag, PlaySemantics.DrawnPxPerUnitKey)
        val box = kit.bounds(tag)
        val (w, h) = kit.unitExtent(piece, kit.key(tag, PlaySemantics.TurnKey), kit.key(tag, PlaySemantics.MirroredKey))
        kitAssertNear("$piece: box width vs DrawnPxPerUnit × unit width", w * scale, box.width.toDouble(), 2.0)
        kitAssertNear("$piece: box height vs DrawnPxPerUnit × unit height", h * scale, box.height.toDouble(), 2.0)
        return scale to box
    }

    /** Everything the tray shows, as comparable strings: bounds rounded to the px, keys and texts. */
    private fun trayFingerprint(): List<String> {
        fun r(b: Rect) = "[${b.left.roundToInt()},${b.top.roundToInt()},${b.right.roundToInt()},${b.bottom.roundToInt()}]"
        val out = ArrayList<String>()
        out += "tray ${r(kit.bounds(PlayTags.TRAY))}"
        for (piece in PieceId.entries) {
            out += "cell $piece ${r(kit.bounds(PlayTags.trayCell(piece)))}"
            val tag = PlayTags.trayPiece(piece)
            out += "piece $piece ${r(kit.bounds(tag))} turn=${kit.key(tag, PlaySemantics.TurnKey).step} mirrored=${kit.key(tag, PlaySemantics.MirroredKey)} scale=${"%.2f".format(kit.key(tag, PlaySemantics.DrawnPxPerUnitKey))}"
            val mark = PlayTags.sizeMark(piece)
            if (kit.exists(mark)) out += "mark $piece ${r(kit.bounds(mark))} '${kit.text(mark)}'"
            if (kit.exists(PlayTags.trayGhost(piece))) out += "ghost $piece"
        }
        if (kit.exists(PlayTags.FLIP_BADGE_TRAY)) out += "badge ${r(kit.bounds(PlayTags.FLIP_BADGE_TRAY))}"
        return out
    }
}
