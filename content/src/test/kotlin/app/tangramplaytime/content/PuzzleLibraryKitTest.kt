// Acceptance kit (WO-002, TASK-008): the shipped library through `PuzzleLibraries.bundled(LockEngine())`
// (designs/WO-002-design.md §2, "PuzzleLibraries.bundled" and "Acceptance kit" rows).
//
// Each tokened test asserts the meaning of its acceptance line at the `IPuzzleLibrary` seam. The
// "fresh install" halves of the order lines are #Browsing's screen (WO-004); the seam proves the
// order (workorders/WO-002.md § Scope notes). Pins of a KDoc line, a seam row or a decision carry
// no token. Crafted and edited texts, and every rule's discrimination, are in PuzzleLoaderKitTest.kt.
package app.tangramplaytime.content

import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.contracts.ArtShape
import app.tangramplaytime.contracts.ArtStyle
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.kernel.LockEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleLibraryKitTest {

    private companion object {
        /** Built once for the class: the library is fixed for the process's lifetime (IPuzzleLibrary KDoc). */
        val library: IPuzzleLibrary by lazy { PuzzleLibraries.bundled(LockEngine()) }

        /** REQ-040 statement: by kind (mini, then warm-up, then full). */
        val KIND_RANK = mapOf(PuzzleKind.MINI to 0, PuzzleKind.WARMUP to 1, PuzzleKind.FULL to 2)

        /** How many minis open the list. */
        fun leadingMinis(puzzles: List<PuzzleDefinition>): Int = puzzles.takeWhile { it.kind == PuzzleKind.MINI }.size
    }

    // ── REQ-007: the library ───────────────────────────────────────────────────────────────────

    @Test // REQ-007.A1
    fun everyShippedPuzzleIsListedAndMeetsTheShapesTheArtAndTheOrderRules() {
        // REQ-007 A1: "Every shipped puzzle meets REQ-038, REQ-039 and REQ-040."
        // `bundled` returning is the main claim: a bundled file that fails any rule fails the build
        // (decisions.md line 45, fail fast; design §2 `PuzzleLibraries.bundled` throws), so a returned
        // library means every bundled file passed every rule. That each rule rejects what it must is
        // proven, rule by rule, with crafted texts in PuzzleLoaderKitTest.kt.
        val lib = PuzzleLibraries.bundled(LockEngine())

        // One listed puzzle per puzzle file of the Tangrams folder, the schema excluded (REQ-007 rule:
        // "Puzzles are held in their own folder"; Spec/03 §2: the id equals the file name).
        val folder = kitTangramsPuzzleIds()
        assertTrue("the Tangrams folder lists puzzle files", folder.isNotEmpty())
        assertEquals("one listed puzzle per puzzle file", folder, lib.puzzles.map { it.id }.sorted())

        // An independent re-measure of every listed puzzle with the kit's own geometry, so the claim
        // does not rest on :content's checks alone.
        for (p in lib.puzzles) {
            val ids = p.solution.map { it.piece }
            if (p.kind == PuzzleKind.MINI) {
                // REQ-045 rule: "a mini puzzle ... 1 to 6 distinct pieces instead of all seven".
                assertTrue("${p.id}: a mini uses 1 to 6 pieces", ids.size in 1..6)
                assertEquals("${p.id}: a mini's pieces are distinct", ids.size, ids.toSet().size)
            } else {
                // REQ-038 rule: "Every puzzle uses all seven pieces, except a puzzle of kind mini";
                // statement: "every piece of TYPE-001 exactly once".
                assertEquals("${p.id}: all seven pieces exactly once", PieceId.entries.toList(), ids.sorted())
            }
            // REQ-038 statement: "congruent pieces".
            for (s in p.solution) {
                assertTrue("${p.id}: ${s.piece} is congruent to its TYPE-001 shape", kitCongruent(s.piece, s.polygon))
            }
            // REQ-038 statement: "no overlaps".
            for (i in p.solution.indices) for (j in i + 1 until p.solution.size) {
                val a = p.solution[i]
                val b = p.solution[j]
                assertFalse("${p.id}: ${a.piece} and ${b.piece} overlap", kitOverlaps(a.polygon, b.polygon))
            }
            // REQ-038 statement: "connected through shared edges"; rule: "a join at a single corner
            // point does not count as connected".
            assertTrue("${p.id}: edge-connected", kitEdgeConnected(kitPiecesOf(p)))
            // REQ-039 statement: "a stylised picture that is drawn inside the puzzle's outline only";
            // rule: "The picture uses flat shapes" (the colour count is not a load rule, decisions.md
            // line 49). The library-level "every puzzle has a well-formed picture" (WO § Scope notes):
            // Spec/03 §2 "Art shapes" ("Colours are `#RRGGBB`"; fill, stroke, width, opacity) and the
            // locked schema's ranges (opacity 0..1, width above 0, a polygon of 3 or more points).
            assertEquals("${p.id}: a well-formed picture", emptyList<String>(), kitArtProblems(p.art))
            // REQ-040 rule: "Rating: an integer from 1 to 5"; Kind: "mini, warmup, full" (typed).
            assertTrue("${p.id}: rating ${p.rating} in 1..5", p.rating in 1..5)
            // IPuzzleLibrary KDoc: every listed puzzle has "an `en` and `fi` title (REQ-047)".
            assertTrue("${p.id}: en title", p.titles["en"].orEmpty().isNotBlank())
            assertTrue("${p.id}: fi title", p.titles["fi"].orEmpty().isNotBlank())
        }
    }

    @Test // REQ-007.A2
    fun theListOpensWithTheMiniPuzzlesThenTheWarmUps() {
        // REQ-007 A2: "The first puzzles in the list are the mini puzzles (REQ-045), then the warm-ups (REQ-041)."
        val kinds = library.puzzles.map { it.kind }
        val minis = leadingMinis(library.puzzles)
        assertTrue("the list opens with a mini puzzle", minis >= 1)
        assertEquals("every mini is in the opening run", kinds.count { it == PuzzleKind.MINI }, minis)
        val warmUps = kinds.drop(minis).takeWhile { it == PuzzleKind.WARMUP }.size
        assertTrue("warm-ups follow the minis", warmUps >= 1)
        assertEquals("every warm-up is in the run after the minis", kinds.count { it == PuzzleKind.WARMUP }, warmUps)
    }

    // ── REQ-040: the order ─────────────────────────────────────────────────────────────────────

    @Test // REQ-040.A1
    fun theFirstListedPuzzleHasRatingOne() {
        // REQ-040 A1: "The first puzzle shown on a fresh install has rating 1." At the seam: the first
        // entry of `puzzles` (KDoc: "the first entry on a fresh install is a mini puzzle of rating 1").
        assertTrue("the shipped library is not empty", library.puzzles.isNotEmpty())
        assertEquals(1, library.puzzles.first().rating)
    }

    @Test // pin: REQ-040 statement and the `puzzles` KDoc; no token
    fun theListIsInKindThenRatingThenIdOrder() {
        // REQ-040 statement: "list puzzles by kind (mini, then warm-up, then full), then by their
        // difficulty rating, lowest first, and by id within a rating". KDoc: "Every puzzle, in ‹ › order".
        val listed = library.puzzles
        val expected = listed.sortedWith(
            compareBy<PuzzleDefinition>({ KIND_RANK.getValue(it.kind) }, { it.rating }, { it.id }),
        )
        assertEquals(expected.map { it.id }, listed.map { it.id })
    }

    // ── REQ-041: the warm-ups ──────────────────────────────────────────────────────────────────

    @Test // REQ-041.A1
    fun theFirstFourPuzzlesAfterTheMinisAreWarmUpsOfRatingOne() {
        // REQ-041 A1: "On a fresh install, the first four puzzles after the mini puzzles are warm-ups."
        val listed = library.puzzles
        val minis = leadingMinis(listed)
        assertTrue("at least four puzzles follow the minis", listed.size >= minis + 4)
        for (p in listed.subList(minis, minis + 4)) {
            assertEquals("${p.id} is a warm-up", PuzzleKind.WARMUP, p.kind)
            // REQ-041 statement: "at least four warm-up puzzles of rating 1".
            assertEquals("${p.id} has rating 1", 1, p.rating)
        }
    }

    @Test // REQ-041.A2
    fun everyListedWarmUpMeetsTheHalfOutlineMeasure() {
        // REQ-041 A2: "Each warm-up meets the half-outline measure." Rule: "At least half of every
        // piece's outline on the silhouette edge". Measured with the kit's own geometry; the 1e-6
        // tolerance is decisions.md line 50 (each shipped warm-up has a piece at exactly 0.5).
        val warmUps = library.puzzles.filter { it.kind == PuzzleKind.WARMUP }
        assertTrue("the library lists warm-ups", warmUps.isNotEmpty())
        for (p in warmUps) {
            for ((piece, exposure) in kitExposures(kitPiecesOf(p))) {
                assertTrue("${p.id}: $piece exposes $exposure of its outline", exposure >= 0.5 - KIT_TOL)
            }
        }
    }

    // ── REQ-042: categories and the classic square ─────────────────────────────────────────────

    @Test // REQ-042.A2
    fun atLeastFourCategoriesArePresent() {
        // REQ-042 A2: "At least four categories are present." (As locked; the theme-set CHG is pending.)
        val categories = library.puzzles.map { it.category }.toSet()
        assertTrue("categories: $categories", categories.size >= 4)
    }

    @Test // REQ-042.A3
    fun shapesSquareIsAmongTheListedPuzzles() {
        // REQ-042 A3, as locked: "shapes-square is among them." (WO § Constraints "Pending deltas":
        // the build follows the locked letter; the content-ids CHG is pending.)
        val square = library.byId("shapes-square")
        assertNotNull("byId(\"shapes-square\")", square)
        assertTrue("`puzzles` lists shapes-square", library.puzzles.any { it.id == "shapes-square" })
    }

    // ── REQ-045: the mini puzzle first ─────────────────────────────────────────────────────────

    @Test // REQ-045.A1
    fun theFirstListedPuzzleIsAMiniWhoseTrayHoldsThreePieces() {
        // REQ-045 A1: "A fresh install opens on a mini puzzle whose tray holds three pieces." At the
        // seam (WO § Scope notes): the first entry is a mini whose piece set has three pieces;
        // `PuzzleDefinition.pieces` KDoc: "the ones its tray shows at the start".
        val first = library.puzzles.first()
        assertEquals(PuzzleKind.MINI, first.kind)
        assertEquals("${first.id}: ${first.pieces}", 3, first.pieces.size)
    }

    // ── IPuzzleLibrary KDoc pins (no token) ────────────────────────────────────────────────────

    @Test // pin: IPuzzleLibrary KDoc "Must never: ... change while the app runs"; seam `PuzzleLibrary` row
    fun theListedPuzzlesCannotBeModified() {
        // Design §2 `PuzzleLibrary`: `puzzles` is "in an unmodifiable list" (java.util.List: an
        // unsupported mutation throws UnsupportedOperationException). A library of its own, so a
        // mutation that wrongly succeeds cannot leak into the other tests.
        val own = PuzzleLibraries.bundled(LockEngine())
        val before = own.puzzles.toList()
        @Suppress("UNCHECKED_CAST")
        val asMutable = own.puzzles as MutableList<PuzzleDefinition>
        assertThrows(UnsupportedOperationException::class.java) { asMutable.add(before.first()) }
        assertThrows(UnsupportedOperationException::class.java) { asMutable.removeAt(0) }
        assertThrows(UnsupportedOperationException::class.java) { asMutable[0] = before.last() }
        assertEquals("unchanged after the attempts", before, own.puzzles)
    }

    @Test // pin: IPuzzleLibrary KDoc "the library is fixed for the process's lifetime"
    fun twoReadsAreEqual() {
        assertEquals(library.puzzles, library.puzzles)
        for (p in library.puzzles) assertEquals(library.byId(p.id), library.byId(p.id))
    }

    @Test // pin: `byId` KDoc "or null when no listed puzzle has that id ... never an exception (G-15)"
    fun byIdOfAnIdNoPuzzleHasIsNull() {
        assertNull(library.byId("no-such-puzzle"))
        assertNull(library.byId(""))
        // Near misses of a listed id are other ids (`byId` KDoc: "The puzzle with [id]").
        assertNull(library.byId("shapes-square.json"))
        assertNull(library.byId("Shapes-Square"))
    }

    @Test // pin: `byId` KDoc "The puzzle with [id]"; `puzzles` KDoc "Edge: ids are unique"
    fun byIdFindsEveryListedPuzzleAndIdsAreUnique() {
        val ids = library.puzzles.map { it.id }
        assertEquals("ids are unique", ids.size, ids.toSet().size)
        for (p in library.puzzles) assertEquals(p.id, p, library.byId(p.id))
    }

    // ── Seam pins of the reading path and the parser (no token) ────────────────────────────────

    @Test // pin: seam `BundledPuzzles.read` "Read each named file as UTF-8"; design §3 "the ä in Lämmittely"
    fun titlesAreDecodedAsUtf8() {
        val warmUp1 = library.byId("shapes-warmup-1") ?: throw AssertionError("shapes-warmup-1 is not listed")
        assertEquals("Lämmittely 1", warmUp1.titles["fi"])
    }

    @Test // pin: seam `PuzzleParser.parse` "Coordinates": `[a, b]` becomes a + b·√2; `SolvedPiece` KDoc "in the file's order"
    fun pairCoordinatesAreReadAsAPlusBRootTwoInTheFilesCornerOrder() {
        // shapes-rectangle.json, LT1: [[0, [0, 2]], [[0, 2], 0], [0, 0]] = (0, 2√2), (2√2, 0), (0, 0).
        val rectangle = library.byId("shapes-rectangle") ?: throw AssertionError("shapes-rectangle is not listed")
        val lt1 = rectangle.solution.single { it.piece == PieceId.LT1 }.polygon
        val expected = listOf(Point(0.0, 2 * KIT_SQRT2), Point(2 * KIT_SQRT2, 0.0), Point(0.0, 0.0))
        assertEquals(expected.size, lt1.size)
        for (i in expected.indices) {
            assertEquals("corner $i x", expected[i].x, lt1[i].x, 1e-9)
            assertEquals("corner $i y", expected[i].y, lt1[i].y, 1e-9)
        }
    }

    // ── The kit's own measure, read against hand values (no token) ─────────────────────────────

    @Test // pin: kit geometry self-check, so a red measure points at the kit, not at :content
    fun theKitMeasureReadsHandComputedValues() {
        val st = listOf(Point(0.0, 0.0), Point(2.0, 0.0), Point(1.0, 1.0))
        // Small triangle: sides 2, √2, √2 → perimeter 2 + 2√2; area 1.
        assertEquals(2 + 2 * KIT_SQRT2, kitPerimeter(st), 1e-12)
        assertTrue(kitCongruent(PieceId.ST1, st))
        // Corner-only touch at (1,1): shared length 0, no overlap.
        val below = listOf(Point(1.0, 1.0), Point(2.0, 2.0), Point(0.0, 2.0))
        assertEquals(0.0, kitSharedLength(st, below), 1e-12)
        assertFalse(kitOverlaps(st, below))
        assertFalse(kitEdgeConnected(listOf(KitPiece("ST1", st), KitPiece("ST2", below))))
        // Shifted by 1: the bottoms share (1,0)–(2,0), length 1; the two overlap by area 0.25.
        val shifted = listOf(Point(1.0, 0.0), Point(3.0, 0.0), Point(2.0, 1.0))
        assertEquals(1.0, kitSharedLength(st, shifted), 1e-12)
        assertTrue(kitOverlaps(st, shifted))
        // shapes-warmup-1: SQ (1,5)(0,4)(1,3)(2,4) lies along MT (0,6)(0,4)(2,6) for √2 and along
        // LT2 (5,5)(1,5)(3,3) for √2 — 2√2 of its 4√2 outline: exposure exactly 0.5.
        val sq = listOf(Point(1.0, 5.0), Point(0.0, 4.0), Point(1.0, 3.0), Point(2.0, 4.0))
        val mt = listOf(Point(0.0, 6.0), Point(0.0, 4.0), Point(2.0, 6.0))
        val lt2 = listOf(Point(5.0, 5.0), Point(1.0, 5.0), Point(3.0, 3.0))
        assertEquals(KIT_SQRT2, kitSharedLength(sq, mt), 1e-12)
        assertEquals(KIT_SQRT2, kitSharedLength(sq, lt2), 1e-12)
        val exposures = kitExposures(listOf(KitPiece("SQ", sq), KitPiece("MT", mt), KitPiece("LT2", lt2)))
        assertEquals(0.5, exposures.getValue("SQ"), 1e-12)
        // A bow-tie of the square's corners is not congruent (area 0); the wrong size is not either.
        val bowTie = listOf(Point(1.0, 0.0), Point(1.0, 2.0), Point(2.0, 1.0), Point(0.0, 1.0))
        assertFalse(kitCongruent(PieceId.SQ, bowTie))
        assertFalse(kitCongruent(PieceId.ST1, listOf(Point(0.0, 0.0), Point(4.0, 0.0), Point(2.0, 2.0))))
        // The picture check: a well-formed picture has no problem; each malformed part is one problem.
        val dot = ArtShape.Circle(Point(1.0, 1.0), 0.2, ArtStyle(fill = "#FFFFFF", width = 0.1, opacity = 0.5))
        assertEquals(emptyList<String>(), kitArtProblems(ArtLayer("#E9C46A", listOf(dot))))
        assertEquals(1, kitArtProblems(ArtLayer("#E9C46A", listOf(dot.copy(style = ArtStyle(fill = "#FFF"))))).size)
        assertEquals(1, kitArtProblems(ArtLayer("#E9C46A", listOf(dot.copy(style = ArtStyle(opacity = 1.5))))).size)
        assertEquals(1, kitArtProblems(ArtLayer("#E9C46A", listOf(dot.copy(style = ArtStyle(width = 0.0))))).size)
        assertEquals(1, kitArtProblems(ArtLayer("#E9C4", listOf(dot))).size)
        assertEquals(1, kitArtProblems(ArtLayer("#E9C46A", listOf(ArtShape.Polygon(listOf(Point(0.0, 0.0), Point(1.0, 0.0)), ArtStyle())))).size)
    }
}
