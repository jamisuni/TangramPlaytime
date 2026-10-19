// Acceptance kit (WO-002, TASK-008): shipped, edited and crafted texts through `PuzzleLoader.load`
// (designs/WO-002-design.md §2 `PuzzleLoader.load`, "the kit's door for crafted texts").
//
// REQ-038 A1 is tested as the written rules of the pending CHG
// (capture-outbox/chg-draft-req038-validity-rules-in-contract.md; workorders/WO-002.md § Scope,
// last note): the shipped texts load with no violation, and for each rule the draft lists, one
// crafted or edited text is Rejected with that rule for that file. The line stays
// `implemented (unverified)` until the capture side signs the draft.
// Two clauses of the draft have no crafted text, because no text in this format can break them:
// "every piece lies inside the silhouette" and "the silhouette outline equals the outline of the
// union of the solution pieces". The format carries no outline apart from the solution, which
// "is used to draw the silhouette" (Spec/03-puzzle-format.md §2).
//
// Every crafted text breaks exactly one rule (KitTexts.kt), and every rejection test asserts that
// this rule, and no other, is reported, for that file only. Crafted texts are loaded together with
// the shipped texts, so a library-level rule (`EMPTY`) can never fire beside the rule under test.
// Pins of a decision carry `// decision decisions.md line NN`, pins of a seam row cite the row;
// neither carries a token.
package app.tangramplaytime.content

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleLoaderKitTest {

    // ── The shipped texts ──────────────────────────────────────────────────────────────────────

    @Test // REQ-038.A1
    fun everyShippedTextLoadsWithNoViolation() {
        // CHG A1: "Every shipped puzzle satisfies every rule above." Loaded (not Rejected) means no
        // violation of any rule; every shipped file is listed under its file-name stem.
        val shipped = kitShipped()
        assertTrue("the bundle reader returns the shipped texts", shipped.isNotEmpty())
        val library = kitLoaded(PuzzleLoader.load(shipped, kitEngine()))
        assertEquals(shipped.map { kitStem(it) }.sorted(), library.puzzles.map { it.id }.sorted())
    }

    // ── Geometry: a valid tangram ──────────────────────────────────────────────────────────────

    @Test // REQ-038.A1
    fun piecesThatTouchOnlyAtACornerAreNotConnected() {
        // REQ-038 rule: "a join at a single corner point does not count as connected"; CHG: "The
        // solution pieces are connected through shared edges (a single shared corner point does not connect)."
        val text = kitCornerOnlyJoin()
        // Fixture check: the two pieces meet at (1,1) only.
        assertFalse(kitEdgeConnected(kitSolutionOf(text.text)))
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.CONNECTED)
    }

    @Test // REQ-038.A1
    fun piecesThatOverlapByAreaAreRejected() {
        // REQ-038 statement: "no overlaps"; CHG: "No two solution pieces overlap".
        val text = kitOverlap()
        // Fixture check: they overlap, and they share an edge, so only the overlap rule breaks.
        val pieces = kitSolutionOf(text.text)
        assertTrue(kitOverlaps(pieces[0].polygon, pieces[1].polygon))
        assertTrue(kitEdgeConnected(pieces))
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.OVERLAP)
    }

    @Test // decision decisions.md line 46
    fun aSquareWhoseCornersAreNotInBoundaryOrderIsNotCongruent() {
        // decisions.md line 46: "with the corners in boundary order (any starting corner, either
        // direction)"; design §3 "SQ and PG corners must trace the boundary, or the silhouette draws as
        // a bow-tie". No REQ sentence names corner order (WO-002 code review N1), so this is a pin; the
        // congruence rule itself is carried by `aPieceOfTheWrongSizeIsNotCongruent`.
        val text = kitBowTieSquare()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.CONGRUENT)
    }

    @Test // REQ-038.A1
    fun aPieceOfTheWrongSizeIsNotCongruent() {
        // CHG: "Every piece of a puzzle's solution is congruent to one piece of TYPE-001" — its own
        // piece: an "ST1" drawn as large as a large triangle is not a small triangle.
        val text = kitWrongSizePiece()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.CONGRUENT)
    }

    @Test // decision decisions.md line 46
    fun aPieceAtNoFortyFiveDegreeTurnIsNotCongruent() {
        // decisions.md line 46: "The file polygon equals `ILockEngine.vertices` at one of the 8 turns".
        val text = kitOffTurnPiece()
        // Fixture check: the right size and shape (sides 2, √2, √2; area 1), so only the turn is wrong.
        val piece = kitSolutionOf(text.text).single()
        assertTrue(kitCongruent(PieceId.ST1, piece.polygon))
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.CONGRUENT)
    }

    // ── The piece set ──────────────────────────────────────────────────────────────────────────

    @Test // REQ-038.A1
    fun aMiniThatRepeatsAPieceIsRejected() {
        // CHG: "a mini puzzle (REQ-045) uses 1 to 6 distinct pieces".
        val text = kitMiniRepeatingAPiece()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.PIECE_SET)
    }

    @Test // REQ-038.A1
    fun aMiniWithSevenPiecesIsRejected() {
        // CHG: "a mini puzzle (REQ-045) uses 1 to 6 distinct pieces" — seven is not 1 to 6
        // (REQ-045 rule: "1 to 6 distinct pieces instead of all seven").
        val text = kitSevenPieceMini()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.PIECE_SET)
    }

    @Test // REQ-038.A1
    fun aFullPuzzleWithSixPiecesIsRejected() {
        // CHG: "a full puzzle uses each of the seven exactly once"; REQ-038 rule: "Every puzzle uses
        // all seven pieces, except a puzzle of kind mini". The text differs from the accepted
        // six-piece mini only in its kind.
        val text = kitSixPieceFull()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.PIECE_SET)
    }

    @Test // REQ-038.A1
    fun aFullPuzzleThatRepeatsAPieceIsRejected() {
        // REQ-038 statement: "use every piece of TYPE-001 exactly once"; CHG: "each of the seven exactly once".
        val text = kitFullRepeatingAPiece()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.PIECE_SET)
    }

    @Test // REQ-038.A1
    fun aMiniWithOneOrSixDistinctPiecesIsAccepted() {
        // CHG: "a mini puzzle (REQ-045) uses 1 to 6 distinct pieces" — both ends of the range load.
        val one = kitOnePieceMini()
        val six = kitSixPieceMini()
        val result = kitLoadWithShipped(one, six)
        val listedOne = kitAssertListed(result, one)
        val listedSix = kitAssertListed(result, six)
        assertEquals(PuzzleKind.MINI, listedOne.kind)
        assertEquals(1, listedOne.pieces.size)
        assertEquals(PuzzleKind.MINI, listedSix.kind)
        assertEquals(6, listedSix.pieces.size)
    }

    @Test // pin: seam `PuzzleParser.parse` Attribution "`piece: "XX"` → `PIECE_SET`"
    fun anUnknownPieceIdIsAPieceSetViolation() {
        val text = kitUnknownPiece()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.PIECE_SET)
    }

    // ── Titles, rating, kind, picture ──────────────────────────────────────────────────────────

    @Test // REQ-038.A1
    fun aPuzzleWithoutAFinnishTitleIsRejected() {
        // CHG: "Each puzzle carries an English and a Finnish title (REQ-047)" (as locked; the
        // English-only CHG would make `fi` optional).
        val text = kitNoFinnishTitle()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.TITLE)
    }

    @Test // REQ-038.A1
    fun aPuzzleWithoutAnEnglishTitleIsRejected() {
        // CHG: "Each puzzle carries an English and a Finnish title (REQ-047)".
        val text = kitNoEnglishTitle()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.TITLE)
    }

    @Test // REQ-038.A1
    fun aRatingBelowOneIsRejected() {
        // CHG: "a difficulty rating 1–5"; REQ-040 rule: "Rating: an integer from 1 to 5".
        val text = kitArrowRated(0)
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.RATING)
    }

    @Test // REQ-038.A1
    fun aRatingAboveFiveIsRejected() {
        // CHG: "a difficulty rating 1–5"; REQ-040 rule: "Rating: an integer from 1 to 5".
        val text = kitArrowRated(6)
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.RATING)
    }

    @Test // REQ-038.A1
    fun aRatingOfFiveIsAccepted() {
        // CHG: "a difficulty rating 1–5" — the top of the range loads, with rating = difficulty
        // (design §2 `PuzzleParser.parse` Mapping).
        val text = kitArrowRated(5)
        assertEquals(5, kitAssertListed(kitLoadWithShipped(text), text).rating)
    }

    @Test // pin: seam `PuzzleParser.parse` "`RATING`: 1..5 (a mini: 1)"; REQ-045 rule "Mini puzzles have rating 1"
    fun aMiniOfRatingTwoIsRejected() {
        val text = kitMiniRatedTwo()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.RATING)
    }

    @Test // REQ-038.A1
    fun anUnknownKindIsRejected() {
        // CHG: "Each puzzle carries ... a kind (REQ-040)"; REQ-040 rule: "Kind: the puzzle file's
        // `kind` field (mini, warmup, full)".
        val text = kitUnknownKind()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.KIND)
    }

    @Test // pin: seam `PuzzleParser.parse` "`KIND`: default full"; schema `kind` "default": "full"
    fun aPuzzleWithoutAKindIsAFullPuzzle() {
        val text = kitNoKind()
        assertEquals(PuzzleKind.FULL, kitAssertListed(kitLoadWithShipped(text), text).kind)
    }

    @Test // REQ-038.A1
    fun aPictureWhoseBaseColourIsNotRrggbbIsRejected() {
        // CHG: "Each puzzle carries ... a picture drawn inside its outline (REQ-039)". What a
        // well-formed picture is at load time: decisions.md line 49 ("a `#RRGGBB` base and colours").
        val text = kitBadBaseColour()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.ART)
    }

    @Test // decision decisions.md line 49
    fun malformedArtShapesAreRejected() {
        // decisions.md line 49 and design §3 "`ART` rejects": opacity outside 0..1, width ≤ 0, a
        // polygon with fewer than 3 points, a `path.d` that is not absolute M/L/Q/C/Z.
        for (text in listOf(kitOpacityAboveOne(), kitZeroStrokeWidth(), kitTwoPointArtPolygon(), kitRelativePath())) {
            kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.ART)
        }
    }

    @Test // decision decisions.md line 48
    fun theReviewFlagIsReadAndAMissingProvenanceReadsAsNotReviewed() {
        // decisions.md line 48: "A missing `provenance` or `reviewedByHuman` reads as `false`."
        // Seam `PuzzleParser.parse` Mapping: "`reviewedByHuman` = `provenance?.reviewedByHuman ?: false`".
        val reviewed = kitReviewed()
        val unmarked = kitNoProvenance()
        val result = kitLoadWithShipped(reviewed, unmarked)
        assertTrue(kitAssertListed(result, reviewed).reviewedByHuman)
        assertFalse(kitAssertListed(result, unmarked).reviewedByHuman)
    }

    // ── Format, id, category, JSON, placement form (seam pins) ─────────────────────────────────

    @Test // pin: seam `PuzzleParser.parse` "`FORMAT`: the tag"
    fun aWrongFormatTagIsRejected() {
        val text = kitWrongFormatTag()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.FORMAT)
    }

    @Test // pin: seam `PuzzleParser.parse` "`ID`: kebab-case, equal to the file name"
    fun anIdThatIsNotTheFileNameOrNotKebabCaseIsRejected() {
        for (text in listOf(kitIdNotTheFileName(), kitIdNotKebabCase())) {
            kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.ID)
        }
    }

    @Test // pin: seam `PuzzleLoader.load` "Library rules: `ID` (ids unique)"; `puzzles` KDoc "ids are unique"
    fun anIdListedTwiceIsRejected() {
        val violations = kitViolations(kitLoadWithShipped(kitDuplicateOfShapesSquare()))
        assertEquals("only ID: $violations", setOf(Rule.ID), violations.map { it.rule }.toSet())
        assertEquals("only shapes-square.json is named: $violations", setOf("shapes-square.json"), violations.map { it.file }.toSet())
    }

    @Test // pin: seam `PuzzleParser.parse` "`CATEGORY`: one of 8"
    fun anUnknownCategoryIsRejected() {
        val text = kitUnknownCategory()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.CATEGORY)
    }

    @Test // pin: seam `PuzzleParser.parse` "`JSON` means only: not JSON, a wrong JSON type, an unknown key, or a missing required field"
    fun notJsonAnUnknownKeyAndAMissingFieldAreJsonViolations() {
        for (text in listOf(kitNotJson(), kitUnknownTopLevelKey(), kitMissingArt())) {
            kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.JSON)
        }
    }

    @Test // decision decisions.md (WO-002 code review S1: unknown shape type = JSON)
    fun anUnknownArtShapeTypeIsOneJsonViolation() {
        // Design §3 "Library facts": "An unknown type ... throws `SerializationException`"; design §2
        // `PuzzleParser.parse`: "Any `IllegalArgumentException` → `JSON`". One violation, naming the file.
        val text = kitUnknownShapeType()
        val result = kitLoadWithShipped(text)
        kitAssertOnlyRule(result, text, Rule.JSON)
        assertEquals("exactly one violation: ${kitViolations(result)}", 1, kitViolations(result).size)
    }

    @Test // decision decisions.md line 47
    fun aSolutionEntryInTheRotFlipAtFormIsAPlacementViolationNotJson() {
        // decisions.md line 47: "A solution entry must give its `polygon`; any other form is a
        // `PLACEMENT` violation." Design §2 (review F1): "never `JSON`".
        val text = kitRotFlipAtForm()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.PLACEMENT)
    }

    @Test // pin: seam `PuzzleParser.parse` "Coordinates: ... anything else → `PLACEMENT`"
    fun aCoordinateThatIsNeitherANumberNorAPairIsAPlacementViolation() {
        val text = kitStringCoordinate()
        kitAssertOnlyRule(kitLoadWithShipped(text), text, Rule.PLACEMENT)
    }

    // ── The loader as a whole ──────────────────────────────────────────────────────────────────

    @Test // decision decisions.md line 45
    fun oneBadFileAmongTheShippedTextsRejectsTheWholeLoad() {
        // decisions.md line 45: a bundled puzzle that fails a rule fails fast, "and nothing is listed";
        // `load` returns `Rejected(violations)`, which carries no library.
        val bad = kitNoFinnishTitle()
        val violations = kitViolations(kitLoadWithShipped(bad))
        assertEquals("only the bad file is named: $violations", setOf(bad.fileName), violations.map { it.file }.toSet())
    }

    @Test // pin: seam `PuzzleLoader.load` "`Rejected(violations)`, with every violation of every file"
    fun everyBadFileIsNamed() {
        val noFi = kitNoFinnishTitle()
        val expert = kitUnknownKind()
        val violations = kitViolations(kitLoadWithShipped(noFi, expert))
        assertTrue("$violations", violations.any { it.file == noFi.fileName && it.rule == Rule.TITLE })
        assertTrue("$violations", violations.any { it.file == expert.fileName && it.rule == Rule.KIND })
    }

    @Test // pin: seam `PuzzleLoader.load` "Library rules: ... `EMPTY` (at least one puzzle)"; `puzzles` KDoc "never empty"
    fun loadingNothingIsRejectedAsEmpty() {
        val violations = kitViolations(PuzzleLoader.load(emptyList(), kitEngine()))
        assertEquals("$violations", setOf(Rule.EMPTY), violations.map { it.rule }.toSet())
    }

    // ── REQ-040: the order ─────────────────────────────────────────────────────────────────────

    @Test // REQ-040.A2
    fun aFullPuzzleOfRatingOneWhoseIdSortsFirstStillComesAfterEveryWarmUp() {
        // REQ-040 A2: "A full puzzle of rating 1 whose id sorts before "shapes-warmup" still comes
        // after every warm-up." The text is edited so that the case exists whatever the bundle holds
        // (the 13 shipped files have no full puzzle of rating 1; WO-010's imports add some, S-39).
        val early = kitEarlyFullOfRatingOne()
        val id = kitStem(early)
        // Fixture check: the id sorts before "shapes-warmup" and before "shapes-mini", so an order by
        // rating and id that ignored the kind would list it before every mini and every warm-up.
        assertTrue(id < "shapes-warmup")
        assertTrue(id < "shapes-mini")
        val listed = kitLoaded(kitLoadWithShipped(early)).puzzles
        val at = listed.indexOfFirst { it.id == id }
        assertTrue("$id is listed", at >= 0)
        assertEquals(PuzzleKind.FULL, listed[at].kind)
        assertEquals(1, listed[at].rating)
        val lastWarmUp = listed.indexOfLast { it.kind == PuzzleKind.WARMUP }
        assertTrue("the list has warm-ups", lastWarmUp >= 0)
        assertTrue("$id at $at comes after every warm-up (last at $lastWarmUp)", at > lastWarmUp)
        // REQ-040 statement: "list puzzles by kind (mini, then warm-up, then full), then by their
        // difficulty rating, lowest first, and by id within a rating" — so its place is the one that
        // order gives it: every puzzle listed before it sorts before it, every one after it sorts after.
        // Corrected check (WO-010 TASK-033; decisions.md 2026-10-19 "The tests that pinned the
        // 13-puzzle bundle"): the line was `assertEquals(lastWarmUp + 1, at)` ("the only full puzzle of
        // rating 1 is the first full puzzle"), a fact of the 13 shipped files that no REQ sentence states.
        assertEquals("$id's place in the REQ-040 order", emptyList<String>(), kitOrderPlaceProblems(listed, id))

        // A second full puzzle of rating 1 whose id sorts before it: the case WO-010's imports bring
        // (S-39: every import full, rated 1 to 5). Both come after every warm-up, each where the
        // statement's order puts it (the earlier id first: "by id within a rating").
        val ahead = kitAheadFullOfRatingOne()
        val aheadId = kitStem(ahead)
        // Fixture check: it sorts before animals-kit-early, so that one is not right after the warm-ups
        // here — the order the removed line rejected.
        assertTrue(aheadId < id)
        val both = kitLoaded(kitLoadWithShipped(early, ahead)).puzzles
        val lastWarmUpOfBoth = both.indexOfLast { it.kind == PuzzleKind.WARMUP }
        assertTrue("the list has warm-ups", lastWarmUpOfBoth >= 0)
        for (each in listOf(aheadId, id)) {
            val i = both.indexOfFirst { it.id == each }
            assertTrue("$each is listed", i >= 0)
            assertTrue("$each at $i comes after every warm-up (last at $lastWarmUpOfBoth)", i > lastWarmUpOfBoth)
            assertEquals("$each's place in the REQ-040 order", emptyList<String>(), kitOrderPlaceProblems(both, each))
        }
        assertTrue("fixture: $id is not right after the warm-ups", both.indexOfFirst { it.id == id } != lastWarmUpOfBoth + 1)

        // Discrimination, test-side: the same check, on crafted lists of the same puzzles, rejects
        // (1) the full puzzle of rating 1 put right before the last warm-up, (2) the order by rating
        // and id alone, (3) it put after a full puzzle of a higher rating, (4) the two rating-1 full
        // puzzles in the wrong id order.
        val beforeAWarmUp = listed.toMutableList().apply { add(lastWarmUp, removeAt(at)) }
        assertTrue("fixture (1): a warm-up comes after $id", beforeAWarmUp.indexOfFirst { it.id == id } < beforeAWarmUp.indexOfLast { it.kind == PuzzleKind.WARMUP })
        assertTrue("(1) before a warm-up is rejected", kitOrderPlaceProblems(beforeAWarmUp, id).isNotEmpty())
        val byRatingAndIdOnly = listed.sortedWith(compareBy<PuzzleDefinition>({ it.rating }, { it.id }))
        assertTrue("fixture (2): a warm-up comes after $id", byRatingAndIdOnly.indexOfFirst { it.id == id } < byRatingAndIdOnly.indexOfLast { it.kind == PuzzleKind.WARMUP })
        assertTrue("(2) the order by rating and id alone is rejected", kitOrderPlaceProblems(byRatingAndIdOnly, id).isNotEmpty())
        val higher = listed.indexOfFirst { it.kind == PuzzleKind.FULL && it.rating > 1 }
        assertTrue("fixture (3): the list has a full puzzle of a rating above 1 after $id", higher > at)
        val afterAHigherRating = listed.toMutableList().apply { add(higher, removeAt(at)) }
        assertTrue("(3) after a full puzzle of a higher rating is rejected", kitOrderPlaceProblems(afterAHigherRating, id).isNotEmpty())
        val swapped = both.toMutableList().apply {
            val i = indexOfFirst { it.id == aheadId }
            val j = indexOfFirst { it.id == id }
            this[i] = both[j]
            this[j] = both[i]
        }
        assertTrue("(4) the wrong id order within rating 1 is rejected", kitOrderPlaceProblems(swapped, id).isNotEmpty())
    }

    // ── REQ-041: the warm-up measure ───────────────────────────────────────────────────────────

    @Test // REQ-041.A2
    fun aWarmUpWithAPieceBelowHalfOnTheEdgeIsRejected() {
        // REQ-041 A2: "Each warm-up meets the half-outline measure." Statement: "in each of which at
        // least half of every piece's outline lies on the silhouette's edge".
        val warmUp = kitWarmUpWithAPieceBelowHalf()
        // Fixture check (arithmetic in KitTexts.kt): exactly one piece, SQ, is below 0.5, at 0.25;
        // the mean exposure is above 0.5, so only an every-piece check rejects it.
        val exposures = kitExposures(kitSolutionOf(warmUp.text))
        assertEquals(listOf("SQ"), exposures.filterValues { it < 0.5 - KIT_TOL }.keys.toList())
        assertEquals(0.25, exposures.getValue("SQ"), 1e-9)
        assertTrue(exposures.values.average() > 0.5)
        kitAssertOnlyRule(kitLoadWithShipped(warmUp), warmUp, Rule.WARMUP)
        // The same arrangement as a full puzzle loads: the measure is the warm-up's (REQ-041 rule
        // "A warm-up is marked `kind: warmup`"; REQ-038 sets no exposure rule).
        val full = kitFullWithAPieceBelowHalf()
        assertEquals(PuzzleKind.FULL, kitAssertListed(kitLoadWithShipped(full), full).kind)
    }

    @Test // REQ-041.A2
    fun theShippedWarmUpsWithAPieceExactlyOnTheLineAreAccepted() {
        // REQ-041 A2 at its edge: "at least half" admits exactly half (design §3 "Warm-ups sit exactly
        // on the line"; decisions.md line 50: passes at an exposure ≥ 0.5 − 1e-6).
        val shipped = kitShipped()
        val warmUps = shipped.filter { kitKindOf(it.text) == "warmup" }
        assertTrue("the shipped texts include warm-ups", warmUps.isNotEmpty())
        // Fixture check: each shipped warm-up has a piece at exactly 0.5 (within float rounding).
        for (w in warmUps) {
            val lowest = kitExposures(kitSolutionOf(w.text)).values.min()
            assertEquals("${w.fileName}: lowest exposure", 0.5, lowest, 1e-9)
        }
        val library = kitLoaded(PuzzleLoader.load(shipped, kitEngine()))
        for (w in warmUps) {
            val listed = library.byId(kitStem(w)) ?: throw AssertionError("${w.fileName} is not listed")
            assertEquals(PuzzleKind.WARMUP, listed.kind)
        }
    }
}

// ── The REQ-040 order, as the kit reads the statement (WO-010 TASK-033) ─────────────────────────────

/** REQ-040 statement: "by kind (mini, then warm-up, then full)". */
private val KIT_KIND_RANK: Map<PuzzleKind, Int> = mapOf(PuzzleKind.MINI to 0, PuzzleKind.WARMUP to 1, PuzzleKind.FULL to 2)

/** REQ-040 statement: by kind, "then by their difficulty rating, lowest first, and by id within a rating". */
private val KIT_REQ040_ORDER: Comparator<PuzzleDefinition> =
    compareBy<PuzzleDefinition>({ KIT_KIND_RANK.getValue(it.kind) }, { it.rating }, { it.id })

/**
 * What is wrong with [id]'s place in [listed] under the REQ-040 statement's order, as text; empty
 * when it sits where that order puts it: every puzzle listed before it sorts before it, and every
 * puzzle listed after it sorts after it (ids are unique: `puzzles` KDoc "Edge: ids are unique").
 * It compares [id] with every other listed puzzle and names no other puzzle, so it holds for any
 * bundle the statement's order lists, and judges [id]'s place only.
 */
private fun kitOrderPlaceProblems(listed: List<PuzzleDefinition>, id: String): List<String> {
    val at = listed.indexOfFirst { it.id == id }
    if (at < 0) return listOf("$id is not listed")
    val p = listed[at]
    val problems = mutableListOf<String>()
    listed.forEachIndexed { i, q ->
        val c = KIT_REQ040_ORDER.compare(q, p)
        if (i < at && c > 0) problems += "${q.id} (${q.kind}, rating ${q.rating}) at $i is listed before $id (${p.kind}, rating ${p.rating}) at $at but sorts after it"
        if (i > at && c < 0) problems += "${q.id} (${q.kind}, rating ${q.rating}) at $i is listed after $id (${p.kind}, rating ${p.rating}) at $at but sorts before it"
    }
    return problems
}
