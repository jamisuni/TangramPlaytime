// Acceptance kit (WO-002, TASK-008): crafted puzzle texts and kit helpers. Test-only; no product code.
//
// - This file carries no acceptance token (traceability-rules.md §2: a token is a coverage claim).
//   The tests that use these texts carry the tokens and cite the sentences they check.
// - Every helper name carries a `kit` prefix (`KIT_` for constants), so it can never collide with
//   an `internal` declaration of :content's main source set in the same package.
// - Edited texts start from a shipped file read through `BundledPuzzles.read()` (designs/WO-002-design.md
//   §2, "Acceptance kit" row); each names its base file and its edits. Hand-made texts say "hand-made".
// - Fixtures discriminate (task-decomposition.md, "assert only what is mandated"): every crafted text
//   breaks exactly one rule, and its `id` equals its file-name stem unless `ID` is the rule under test.
//   The geometry of every crafted solution was checked by hand and with the author's own scratch
//   geometry; the arithmetic is in the comments.
// - JSON in this file is written with single quotes for legibility; `kitQ` turns them into double quotes.
//   No edited text contains an apostrophe.
package app.tangramplaytime.content

import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.contracts.ArtShape
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.SolvedPiece
import app.tangramplaytime.kernel.LockEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// ── Constants ──────────────────────────────────────────────────────────────────────────────────

/** The TYPE-004 geometry tolerance, 1e-6 units (decisions.md line 50; `Point` KDoc). */
internal const val KIT_TOL: Double = 1e-6

/** √2 as a Double: a file coordinate `[a, b]` is a + b·√2 (Spec/03-puzzle-format.md §1). */
internal val KIT_SQRT2: Double = sqrt(2.0)

/** The format tag every file carries (Spec/03 §2, schema `format`). */
internal const val KIT_FORMAT: String = "tangram-puzzle/1"

// ── The door: engine, shipped sources and the loader ──────────────────────────────────────────

/** The real lock engine of WO-001 (`:kernel`, on the test classpath since TASK-005; design §2). */
internal fun kitEngine(): ILockEngine = LockEngine()

/** The shipped puzzle texts, through the one reading path of the app (design §2 `BundledPuzzles.read`). */
internal fun kitShipped(): List<PuzzleSource> = BundledPuzzles.read()

/** The file-name stem of a source: `shapes-square.json` → `shapes-square` (Spec/03 §2: id = file name). */
internal fun kitStem(source: PuzzleSource): String = source.fileName.removeSuffix(".json")

/** The text of one shipped file, read through the bundle reader. Fails loudly when it is not bundled. */
internal fun kitShippedText(fileName: String): String {
    val all = kitShipped()
    val hit = all.filter { it.fileName == fileName }
    if (hit.size != 1) {
        throw AssertionError("kit edit base '$fileName' is not bundled exactly once; bundled: ${all.map { it.fileName }}")
    }
    return hit.single().text
}

/** `PuzzleLoader.load` over the shipped texts plus [extra] (design §2 `PuzzleLoader.load`, the kit's door). */
internal fun kitLoadWithShipped(vararg extra: PuzzleSource): LoadResult =
    PuzzleLoader.load(kitShipped() + extra.toList(), kitEngine())

/**
 * The library of a `Loaded` result. Fails loudly, listing the violations, on `Rejected`.
 * `LoadResult` is the sealed `Loaded | Rejected(violations)` of decisions.md line 45 (design §2).
 */
internal fun kitLoaded(result: LoadResult): IPuzzleLibrary = when (result) {
    is LoadResult.Loaded -> result.library
    is LoadResult.Rejected -> throw AssertionError("expected Loaded, got Rejected: ${result.violations}")
}

/** The violations of a `Rejected` result. Fails loudly on `Loaded`. */
internal fun kitViolations(result: LoadResult): List<Violation> = when (result) {
    is LoadResult.Rejected -> result.violations
    is LoadResult.Loaded -> throw AssertionError("expected Rejected, got Loaded: ${result.library.puzzles.map { it.id }}")
}

/**
 * Asserts that [result] is `Rejected`, that [rule] is reported for [source]'s file, and that no
 * other rule and no other file is reported: the fixture breaks exactly one rule, so a second rule
 * would hide which one the check enforces (task-decomposition.md, fixtures must discriminate).
 */
internal fun kitAssertOnlyRule(result: LoadResult, source: PuzzleSource, rule: Rule) {
    val violations = kitViolations(result)
    if (violations.none { it.file == source.fileName && it.rule == rule }) {
        throw AssertionError("expected $rule for ${source.fileName}; got $violations")
    }
    val rules = violations.map { it.rule }.toSet()
    if (rules != setOf(rule)) {
        throw AssertionError("only $rule may be reported for ${source.fileName}; got $violations")
    }
    val files = violations.map { it.file }.toSet()
    if (files != setOf(source.fileName)) {
        throw AssertionError("only ${source.fileName} may be named; got $violations")
    }
}

/** Asserts that [result] is `Loaded` and lists [source]'s puzzle under its stem; returns that puzzle. */
internal fun kitAssertListed(result: LoadResult, source: PuzzleSource): PuzzleDefinition {
    val library = kitLoaded(result)
    val id = kitStem(source)
    val definition = library.byId(id) ?: throw AssertionError("$id is not listed; listed: ${library.puzzles.map { it.id }}")
    if (library.puzzles.none { it.id == id }) {
        throw AssertionError("byId($id) answered but `puzzles` does not list it")
    }
    return definition
}

// ── Text building ──────────────────────────────────────────────────────────────────────────────

/** Single-quoted JSON → JSON. */
internal fun kitQ(singleQuoted: String): String = singleQuoted.replace('\'', '"')

/** Replaces the one occurrence of [old] in [text]. Fails loudly when the edit base changed. */
internal fun kitEdit(text: String, old: String, new: String): String {
    val first = text.indexOf(old)
    if (first < 0 || text.indexOf(old, first + 1) >= 0) {
        throw AssertionError("kit edit base changed: '$old' must occur exactly once")
    }
    return text.replaceRange(first, first + old.length, new)
}

/**
 * A shipped file with its id changed to [id] and [edits] applied (each pair is single-quoted JSON:
 * old to new). The new text is named "[fileName]", by default "$id.json", so id = file-name stem.
 */
internal fun kitEdited(
    base: String,
    id: String,
    vararg edits: Pair<String, String>,
    fileName: String = "$id.json",
): PuzzleSource {
    val baseId = base.removeSuffix(".json")
    var text = kitEdit(kitShippedText(base), kitQ("'id': '$baseId'"), kitQ("'id': '$id'"))
    for ((old, new) in edits) text = kitEdit(text, kitQ(old), kitQ(new))
    return PuzzleSource(fileName, text)
}

/** A flat picture every hand-made text carries: a `#RRGGBB` base and one filled circle (Spec/03 §2). */
internal const val KIT_ART: String = "{'base': '#E9C46A', 'shapes': [{'type': 'circle', 'c': [1, 1], 'r': 0.2, 'fill': '#FFFFFF'}]}"

/** The provenance every hand-made text carries unless a test leaves it out. */
internal const val KIT_PROVENANCE: String = "{'author': 'human:kit', 'reviewedByHuman': false}"

/**
 * A hand-made text that keeps every rule its caller does not break: the format tag, id = file-name
 * stem, an `en` and `fi` title, a known category, the given kind and difficulty, a picture and a
 * provenance. [solution] is the body of the `solution` array; [art] or [provenance] null leaves
 * that field out.
 */
internal fun kitHandMade(
    id: String,
    kind: String,
    solution: String,
    difficulty: Int = 1,
    art: String? = KIT_ART,
    provenance: String? = KIT_PROVENANCE,
): PuzzleSource {
    val fields = mutableListOf(
        "'format': '$KIT_FORMAT'",
        "'id': '$id'",
        "'title': {'en': 'Kit puzzle', 'fi': 'Testipalapeli'}",
        "'category': 'shapes'",
        "'difficulty': $difficulty",
        "'kind': '$kind'",
        "'solution': [$solution]",
    )
    if (art != null) fields += "'art': $art"
    if (provenance != null) fields += "'provenance': $provenance"
    return PuzzleSource("$id.json", kitQ(fields.joinToString(prefix = "{", separator = ", ", postfix = "}")))
}

// ── Crafted texts: geometry (the shapes REQ's "valid tangram") ─────────────────────────────────

/**
 * Hand-made mini. ST1 (0,0)(2,0)(1,1) and ST2 (1,1)(2,2)(0,2) touch only at the corner (1,1):
 * no edge of one lies on an edge of the other, so their shared length is 0. Both are small
 * triangles (sides 2, √2, √2; turns 0 and 4), distinct, apart, rating 1.
 * Breaks only "a single shared corner point does not connect" → CONNECTED.
 */
internal fun kitCornerOnlyJoin(): PuzzleSource = kitHandMade(
    id = "shapes-kit-corner-join",
    kind = "mini",
    solution = "{'piece': 'ST1', 'polygon': [[0, 0], [2, 0], [1, 1]]}, {'piece': 'ST2', 'polygon': [[1, 1], [2, 2], [0, 2]]}",
)

/**
 * Hand-made mini. ST1 (0,0)(2,0)(1,1) and ST2 (1,0)(3,0)(2,1) overlap in the triangle
 * (1,0)(2,0)(1.5,0.5), area 0.25. Their bottom edges share the segment (1,0)–(2,0), length 1, so
 * they are edge-connected. Both are small triangles at turn 0, distinct, rating 1.
 * Breaks only "no two solution pieces overlap" → OVERLAP.
 */
internal fun kitOverlap(): PuzzleSource = kitHandMade(
    id = "shapes-kit-overlap",
    kind = "mini",
    solution = "{'piece': 'ST1', 'polygon': [[0, 0], [2, 0], [1, 1]]}, {'piece': 'ST2', 'polygon': [[1, 0], [3, 0], [2, 1]]}",
)

/**
 * Hand-made one-piece mini. The square (1,0)(2,1)(1,2)(0,1) listed as (1,0)(1,2)(2,1)(0,1): the same
 * four corners, but not in boundary order, so the outline crosses itself (a bow-tie, signed area 0).
 * One piece: nothing to overlap, nothing to connect. Breaks only congruence → CONGRUENT
 * (decisions.md line 46: corners in boundary order; design §3 "Congruence is stricter than Python V3").
 */
internal fun kitBowTieSquare(): PuzzleSource = kitHandMade(
    id = "shapes-kit-bow-tie",
    kind = "mini",
    solution = "{'piece': 'SQ', 'polygon': [[1, 0], [1, 2], [2, 1], [0, 1]]}",
)

/**
 * Hand-made one-piece mini. "ST1" drawn at twice its size: (0,0)(4,0)(2,2) has sides 4, 2√2, 2√2 and
 * area 4, the large triangle's, not the small one's (sides 2, √2, √2, area 1; TYPE-001).
 * Breaks only "congruent to one piece of TYPE-001" for its own piece → CONGRUENT.
 */
internal fun kitWrongSizePiece(): PuzzleSource = kitHandMade(
    id = "shapes-kit-wrong-size",
    kind = "mini",
    solution = "{'piece': 'ST1', 'polygon': [[0, 0], [4, 0], [2, 2]]}",
)

/**
 * Hand-made one-piece mini. The small triangle (0,0)(2,0)(1,1) turned by the 3-4-5 angle
 * (cos 0.8, sin 0.6, about 36.87°): (0,0)(1.6,1.2)(0.2,1.4). Sides |(1.6,1.2)| = 2,
 * |(0.2,1.4)| = √2, |(1.4,−0.2)| = √2, area 1: the right size and shape, but at no 45° turn.
 * Breaks only the 45° reading of congruence → CONGRUENT (decisions.md line 46; TYPE-003 turns).
 */
internal fun kitOffTurnPiece(): PuzzleSource = kitHandMade(
    id = "shapes-kit-off-turn",
    kind = "mini",
    solution = "{'piece': 'ST1', 'polygon': [[0, 0], [1.6, 1.2], [0.2, 1.4]]}",
)

/** Hand-made one-piece mini: the small triangle (0,0)(2,0)(1,1) at turn 0. Breaks nothing (1 piece). */
internal fun kitOnePieceMini(): PuzzleSource = kitHandMade(
    id = "shapes-kit-one-piece",
    kind = "mini",
    solution = "{'piece': 'ST1', 'polygon': [[0, 0], [2, 0], [1, 1]]}",
)

// ── Crafted texts: the piece set ───────────────────────────────────────────────────────────────

/**
 * Edited from shapes-mini-1.json: its ST2 (2,2)(4,2)(3,1) is labelled ST1, so ST1 appears twice and
 * ST2 not at all. The geometry is unchanged (a valid arrangement of three pieces).
 * Breaks only "a mini puzzle uses 1 to 6 distinct pieces" → PIECE_SET.
 */
internal fun kitMiniRepeatingAPiece(): PuzzleSource = kitEdited(
    "shapes-mini-1.json", "shapes-kit-mini-repeat",
    "{'piece': 'ST2', 'polygon': [[2, 2], [4, 2], [3, 1]]}" to "{'piece': 'ST1', 'polygon': [[2, 2], [4, 2], [3, 1]]}",
)

/**
 * Edited from shapes-square.json: ST1 (4,0)(4,2)(3,1) removed, rating 4 → 1, kind full → mini.
 * Six distinct pieces, still edge-connected (LT1–LT2, LT1–SQ, SQ–MT, SQ–ST2, LT2–ST2, LT2–PG share
 * edges), no overlap, all congruent. Breaks nothing: a mini may use 6 distinct pieces.
 */
internal fun kitSixPieceMini(): PuzzleSource = kitEdited(
    "shapes-square.json", "shapes-kit-mini-six",
    "{'piece': 'ST1', 'polygon': [[4, 0], [4, 2], [3, 1]]}," to "",
    "'difficulty': 4," to "'difficulty': 1,",
    "'kind': 'full'," to "'kind': 'mini',",
)

/**
 * Edited from shapes-square.json: the same six pieces as [kitSixPieceMini] and rating 1, kind full
 * kept. The two texts differ only in `kind`.
 * Breaks only "a full puzzle uses each of the seven exactly once" → PIECE_SET.
 */
internal fun kitSixPieceFull(): PuzzleSource = kitEdited(
    "shapes-square.json", "shapes-kit-full-six",
    "{'piece': 'ST1', 'polygon': [[4, 0], [4, 2], [3, 1]]}," to "",
    "'difficulty': 4," to "'difficulty': 1,",
)

/**
 * Edited from shapes-square.json: LT2 (0,0)(2,2)(0,4) is labelled LT1, so LT1 appears twice and LT2
 * not at all; seven entries, geometry unchanged.
 * Breaks only "each of the seven exactly once" → PIECE_SET.
 */
internal fun kitFullRepeatingAPiece(): PuzzleSource = kitEdited(
    "shapes-square.json", "shapes-kit-full-repeat",
    "{'piece': 'LT2', 'polygon': [[0, 0], [2, 2], [0, 4]]}" to "{'piece': 'LT1', 'polygon': [[0, 0], [2, 2], [0, 4]]}",
)

/**
 * Edited from shapes-square.json: kind full → mini, rating 4 → 1. All seven pieces, geometry unchanged.
 * Breaks only "a mini puzzle uses 1 to 6 distinct pieces" (seven is not 1 to 6) → PIECE_SET.
 */
internal fun kitSevenPieceMini(): PuzzleSource = kitEdited(
    "shapes-square.json", "shapes-kit-mini-seven",
    "'difficulty': 4," to "'difficulty': 1,",
    "'kind': 'full'," to "'kind': 'mini',",
)

/**
 * Hand-made one-piece mini whose piece id "XX" is not a TYPE-001 piece; its polygon is a small
 * triangle. Breaks only the piece set → PIECE_SET (design §2 `PuzzleParser.parse` Attribution:
 * `piece: "XX"` → `PIECE_SET`).
 */
internal fun kitUnknownPiece(): PuzzleSource = kitHandMade(
    id = "shapes-kit-unknown-piece",
    kind = "mini",
    solution = "{'piece': 'XX', 'polygon': [[0, 0], [2, 0], [1, 1]]}",
)

// ── Crafted texts: the warm-up measure ─────────────────────────────────────────────────────────

/**
 * Edited from shapes-warmup-1.json: ST1 moves from (4,2)(4,4)(3,3) to (0,4)(0,2)(1,3), where it fits
 * against the square's lower-left edge. Still seven congruent pieces, no overlap, edge-connected
 * (ST1 shares (0,4)–(1,3) with SQ). Kind warmup, rating 1.
 *
 * The square SQ (1,5)(0,4)(1,3)(2,4) has four sides of √2, perimeter 4√2:
 * - (1,5)–(0,4) lies on MT's side (0,4)–(2,6): shared √2;
 * - (2,4)–(1,5) lies on LT2's side (1,5)–(3,3): shared √2;
 * - (0,4)–(1,3) is ST1's side (1,3)–(0,4): shared √2;
 * - (1,3)–(2,4) borders no piece: on the silhouette edge, √2.
 * Exposure = 1 − 3√2 / 4√2 = 0.25 < 0.5.
 *
 * Every other piece stays at or above 0.5 (LT1 0.707, LT2 0.707, MT 0.793, PG 0.586, ST1 0.707,
 * ST2 0.707), so the mean exposure is 0.637: a check of the mean, or of "some piece", would accept it.
 * Breaks only "at least half of every piece's outline lies on the silhouette's edge" → WARMUP.
 */
internal fun kitWarmUpWithAPieceBelowHalf(): PuzzleSource = kitEdited(
    "shapes-warmup-1.json", "shapes-kit-warmup-below",
    "{'piece': 'ST1', 'polygon': [[4, 2], [4, 4], [3, 3]]}" to "{'piece': 'ST1', 'polygon': [[0, 4], [0, 2], [1, 3]]}",
)

/** The arrangement of [kitWarmUpWithAPieceBelowHalf] with kind warmup → full. Breaks nothing. */
internal fun kitFullWithAPieceBelowHalf(): PuzzleSource = kitEdited(
    "shapes-warmup-1.json", "shapes-kit-full-below",
    "{'piece': 'ST1', 'polygon': [[4, 2], [4, 4], [3, 3]]}" to "{'piece': 'ST1', 'polygon': [[0, 4], [0, 2], [1, 3]]}",
    "'kind': 'warmup'," to "'kind': 'full',",
)

// ── Crafted texts: the order ───────────────────────────────────────────────────────────────────

/**
 * Edited from animals-cat.json: id animals-cat → animals-kit-early, rating 3 → 1; kind full kept.
 * A valid full puzzle of rating 1 whose id sorts before "shapes-mini" and "shapes-warmup", so an
 * order by rating and id that ignores the kind would list it first. Breaks nothing.
 */
internal fun kitEarlyFullOfRatingOne(): PuzzleSource = kitEdited(
    "animals-cat.json", "animals-kit-early",
    "'difficulty': 3," to "'difficulty': 1,",
)

/**
 * Edited from animals-cat.json: id animals-cat → animals-kit-ahead, rating 3 → 1; kind full kept.
 * (WO-010 TASK-033.) A second valid full puzzle of rating 1, whose id sorts before
 * animals-kit-early ("a" < "e" after "animals-kit-"): loaded beside [kitEarlyFullOfRatingOne], it is
 * a full puzzle of rating 1 listed between the warm-ups and animals-kit-early — the case WO-010's
 * imports bring (tasks.md § WO-010 › Seams S-39: every import is `full`, rated 1 to 5). No import
 * id has a `kit` segment (S-39), so the id cannot collide. Breaks nothing.
 */
internal fun kitAheadFullOfRatingOne(): PuzzleSource = kitEdited(
    "animals-cat.json", "animals-kit-ahead",
    "'difficulty': 3," to "'difficulty': 1,",
)

// ── Crafted texts: titles, rating, kind, picture ───────────────────────────────────────────────

/** Edited from things-arrow.json: the `fi` title removed. Breaks only "an English and a Finnish title" → TITLE. */
internal fun kitNoFinnishTitle(): PuzzleSource = kitEdited(
    "things-arrow.json", "things-kit-no-fi",
    "'en': 'Arrow'," to "'en': 'Arrow'",
    "'fi': 'Nuoli'" to "",
)

/** Edited from things-arrow.json: the `en` title removed. Breaks only "an English and a Finnish title" → TITLE. */
internal fun kitNoEnglishTitle(): PuzzleSource = kitEdited(
    "things-arrow.json", "things-kit-no-en",
    "'en': 'Arrow'," to "",
)

/** Edited from things-arrow.json: difficulty 2 → [rating]. */
internal fun kitArrowRated(rating: Int): PuzzleSource = kitEdited(
    "things-arrow.json", "things-kit-rating-$rating",
    "'difficulty': 2," to "'difficulty': $rating,",
)

/**
 * Edited from shapes-mini-1.json: difficulty 1 → 2. Breaks only "mini puzzles have rating 1"
 * → RATING (design §2 `PuzzleParser.parse`: "`RATING`: 1..5 (a mini: 1)").
 */
internal fun kitMiniRatedTwo(): PuzzleSource = kitEdited(
    "shapes-mini-1.json", "shapes-kit-mini-rating-2",
    "'difficulty': 1," to "'difficulty': 2,",
)

/** Edited from things-arrow.json: kind full → "expert". Breaks only "a kind" (mini, warmup, full) → KIND. */
internal fun kitUnknownKind(): PuzzleSource = kitEdited(
    "things-arrow.json", "things-kit-kind-expert",
    "'kind': 'full'," to "'kind': 'expert',",
)

/** Edited from things-arrow.json: the `kind` field removed (the schema default is full). Breaks nothing. */
internal fun kitNoKind(): PuzzleSource = kitEdited(
    "things-arrow.json", "things-kit-no-kind",
    "'kind': 'full'," to "",
)

/**
 * Edited from things-arrow.json: art base #B07A4F → #B07A4 (five hex digits, not `#RRGGBB`).
 * Breaks only the picture rule → ART (decisions.md line 49: a `#RRGGBB` base).
 */
internal fun kitBadBaseColour(): PuzzleSource = kitEdited(
    "things-arrow.json", "things-kit-bad-base",
    "'base': '#B07A4F'," to "'base': '#B07A4',",
)

/** Edited from shapes-mini-1.json: the first polygon's opacity 0.45 → 1.5 (outside 0..1) → ART. */
internal fun kitOpacityAboveOne(): PuzzleSource = kitEdited(
    "shapes-mini-1.json", "shapes-kit-opacity",
    "'opacity': 0.45" to "'opacity': 1.5",
)

/** Edited from shapes-mini-2.json: the frame polygon's stroke width 0.28 → 0 (not > 0) → ART. */
internal fun kitZeroStrokeWidth(): PuzzleSource = kitEdited(
    "shapes-mini-2.json", "shapes-kit-width",
    "'width': 0.28" to "'width': 0",
)

/** Edited from shapes-mini-2.json: the first art polygon keeps two of its three points → ART. */
internal fun kitTwoPointArtPolygon(): PuzzleSource = kitEdited(
    "shapes-mini-2.json", "shapes-kit-two-points",
    "'points': [[0.1, 0.1], [0.75, 0.1], [0.1, 0.75]]" to "'points': [[0.1, 0.1], [0.75, 0.1]]",
)

/**
 * Edited from animals-cat.json: the first path "M3.3,3.35 Q3.7,3.55 4,3.35" becomes the same curve
 * with a relative command, "M3.3,3.35 q0.4,0.2 0.7,0" (`q` is not absolute M/L/Q/C/Z) → ART.
 */
internal fun kitRelativePath(): PuzzleSource = kitEdited(
    "animals-cat.json", "animals-kit-relative-path",
    "'d': 'M3.3,3.35 Q3.7,3.55 4,3.35'" to "'d': 'M3.3,3.35 q0.4,0.2 0.7,0'",
)

// ── Crafted texts: format, id, category, JSON, placement form ──────────────────────────────────

/** Edited from things-arrow.json: format tag tangram-puzzle/1 → tangram-puzzle/2 → FORMAT. */
internal fun kitWrongFormatTag(): PuzzleSource = kitEdited(
    "things-arrow.json", "things-kit-format",
    "'format': 'tangram-puzzle/1'," to "'format': 'tangram-puzzle/2',",
)

/** Edited from things-arrow.json: id things-kit-other in the file things-kit-id.json (id ≠ file name) → ID. */
internal fun kitIdNotTheFileName(): PuzzleSource = kitEdited(
    "things-arrow.json", "things-kit-other",
    fileName = "things-kit-id.json",
)

/** Edited from things-arrow.json: id Things_Kit in the file Things_Kit.json (equal, but not kebab-case) → ID. */
internal fun kitIdNotKebabCase(): PuzzleSource = kitEdited(
    "things-arrow.json", "Things_Kit",
)

/** A second, unchanged copy of shapes-square.json under the same name: its id is listed twice → ID. */
internal fun kitDuplicateOfShapesSquare(): PuzzleSource =
    PuzzleSource("shapes-square.json", kitShippedText("shapes-square.json"))

/**
 * Edited from things-arrow.json: category things → robots, id robots-kit-arrow (so the id still
 * starts with its category). "robots" is none of the eight categories (Spec/03 §2) → CATEGORY.
 */
internal fun kitUnknownCategory(): PuzzleSource = kitEdited(
    "things-arrow.json", "robots-kit-arrow",
    "'category': 'things'," to "'category': 'robots',",
)

/** Hand-made: a text that is not JSON at all → JSON. */
internal fun kitNotJson(): PuzzleSource = PuzzleSource("shapes-kit-not-json.json", "this is not a puzzle file")

/** Edited from things-arrow.json: an extra top-level key `author` (the schema allows none) → JSON. */
internal fun kitUnknownTopLevelKey(): PuzzleSource = kitEdited(
    "things-arrow.json", "things-kit-extra-key",
    "'category': 'things'," to "'category': 'things', 'author': 'kit',",
)

/** Hand-made one-piece mini without the required `art` field → JSON. */
internal fun kitMissingArt(): PuzzleSource = kitHandMade(
    id = "shapes-kit-no-art",
    kind = "mini",
    solution = "{'piece': 'ST1', 'polygon': [[0, 0], [2, 0], [1, 1]]}",
    art = null,
)

/**
 * Edited from shapes-square.json: LT1 `polygon` (0,0)(4,0)(2,2) rewritten in the format's other
 * form, `rot` 0 + `flip` false + `at` (0,0). By Spec/03 §3 (LT local (0,0)(4,0)(2,2), anchor = first
 * vertex) that is the same polygon, so the text is schema-valid and geometrically unchanged; only
 * the form is one the loader does not read → PLACEMENT, never JSON (decisions.md line 47).
 */
internal fun kitRotFlipAtForm(): PuzzleSource = kitEdited(
    "shapes-square.json", "shapes-kit-rot-at",
    "{'piece': 'LT1', 'polygon': [[0, 0], [4, 0], [2, 2]]}" to "{'piece': 'LT1', 'rot': 0, 'flip': false, 'at': [0, 0]}",
)

/**
 * Edited from shapes-square.json: in LT1's third corner the x coordinate 2 is written as the string
 * "2" — neither a number nor `[a, b]` → PLACEMENT (design §2 `PuzzleParser.parse` "Coordinates").
 */
internal fun kitStringCoordinate(): PuzzleSource = kitEdited(
    "shapes-square.json", "shapes-kit-string-coordinate",
    "{'piece': 'LT1', 'polygon': [[0, 0], [4, 0], [2, 2]]}" to "{'piece': 'LT1', 'polygon': [[0, 0], [4, 0], ['2', 2]]}",
)

/** Hand-made one-piece mini whose provenance says reviewedByHuman true. */
internal fun kitReviewed(): PuzzleSource = kitHandMade(
    id = "shapes-kit-reviewed",
    kind = "mini",
    solution = "{'piece': 'ST1', 'polygon': [[0, 0], [2, 0], [1, 1]]}",
    provenance = "{'author': 'human:kit', 'reviewedByHuman': true}",
)

/** Hand-made one-piece mini with no provenance at all (the field is optional in the schema). */
internal fun kitNoProvenance(): PuzzleSource = kitHandMade(
    id = "shapes-kit-no-provenance",
    kind = "mini",
    solution = "{'piece': 'ST1', 'polygon': [[0, 0], [2, 0], [1, 1]]}",
    provenance = null,
)

/**
 * Edited from things-arrow.json: its one art `rect` gets the type "star", which is not one of the six
 * art shape types (Spec/03 §2 "Art shapes"). Its fields are unchanged → JSON (WO-002 code review
 * S1: the sealed shape type fails to decode; design §3 "Library facts").
 */
internal fun kitUnknownShapeType(): PuzzleSource = kitEdited(
    "things-arrow.json", "things-kit-star",
    "'type': 'rect'," to "'type': 'star',",
)

// ── The Tangrams folder listing (bundle completeness) ──────────────────────────────────────────

/** The project's Tangrams folder, found upward from the test's working directory. Fails loudly. */
internal fun kitTangramsDir(): File {
    val start = File("").absoluteFile
    var dir: File? = start
    while (dir != null) {
        val candidate = File(dir, "Tangrams")
        if (File(candidate, "puzzle.schema.json").isFile) return candidate
        dir = dir.parentFile
    }
    throw AssertionError("no Tangrams folder (with puzzle.schema.json) above $start")
}

/** The puzzle files of the Tangrams folder: top-level `*.json` except `*.schema.json`, as sorted stems. */
internal fun kitTangramsPuzzleIds(): List<String> {
    val dir = kitTangramsDir()
    val files = dir.listFiles() ?: throw AssertionError("cannot list $dir")
    return files
        .filter { it.isFile && it.name.endsWith(".json") && !it.name.endsWith(".schema.json") }
        .map { it.name.removeSuffix(".json") }
        .sorted()
}

// ── Kit picture check: the author's own reading of a listed puzzle's art ───────────────────────

private val KIT_HEX = Regex("^#[0-9A-Fa-f]{6}$")

/**
 * What is wrong with a listed picture, as text (empty when it is well formed). Spec/03 §2 "Art
 * shapes": a `base` colour, then shapes; "Every shape may have `fill`, `stroke`, `width` ... and
 * `opacity`. Colours are `#RRGGBB`." Ranges from the locked schema (Tangrams/puzzle.schema.json):
 * `opacity` 0..1, `width` above 0, a `polygon` of at least 3 points. The 3–8 colour count is not
 * checked (decisions.md line 49: not a load rule).
 */
internal fun kitArtProblems(art: ArtLayer): List<String> {
    val problems = mutableListOf<String>()
    if (!KIT_HEX.matches(art.base)) problems += "base ${art.base} is not #RRGGBB"
    art.shapes.forEachIndexed { i, shape ->
        val s = shape.style
        val fill = s.fill
        val stroke = s.stroke
        val width = s.width
        val opacity = s.opacity
        if (fill != null && !KIT_HEX.matches(fill)) problems += "shape $i fill $fill is not #RRGGBB"
        if (stroke != null && !KIT_HEX.matches(stroke)) problems += "shape $i stroke $stroke is not #RRGGBB"
        if (opacity != null && opacity !in 0.0..1.0) problems += "shape $i opacity $opacity is outside 0..1"
        if (width != null && width <= 0.0) problems += "shape $i width $width is not above 0"
        if (shape is ArtShape.Polygon && shape.points.size < 3) problems += "shape $i polygon has ${shape.points.size} points"
    }
    return problems
}

// ── Kit geometry: the author's own measure, independent of :content's code ─────────────────────

/** One piece of a solution as the kit reads it: the piece id as written and its corners. */
internal data class KitPiece(val piece: String, val polygon: List<Point>)

/** A file coordinate: a number, or `[a, b]` = a + b·√2 (Spec/03 §1). */
internal fun kitCoord(element: JsonElement): Double =
    if (element is JsonArray) element[0].jsonPrimitive.double + element[1].jsonPrimitive.double * KIT_SQRT2
    else element.jsonPrimitive.double

/** The kit's own reading of a text's `solution` (polygon form only), for fixture checks. */
internal fun kitSolutionOf(text: String): List<KitPiece> =
    kitJson(text).getValue("solution").jsonArray.map { entry ->
        val o = entry.jsonObject
        KitPiece(
            o.getValue("piece").jsonPrimitive.content,
            o.getValue("polygon").jsonArray.map { pt -> Point(kitCoord(pt.jsonArray[0]), kitCoord(pt.jsonArray[1])) },
        )
    }

/** The kit's own reading of a text's `kind` (the schema default is full). */
internal fun kitKindOf(text: String): String {
    val o = kitJson(text)
    return if ("kind" in o) o.getValue("kind").jsonPrimitive.content else "full"
}

private fun kitJson(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

/** A listed puzzle's solution in the kit's terms. */
internal fun kitPiecesOf(definition: PuzzleDefinition): List<KitPiece> =
    definition.solution.map { s: SolvedPiece -> KitPiece(s.piece.name, s.polygon) }

private fun kitDist(a: Point, b: Point): Double = hypot(b.x - a.x, b.y - a.y)

/** The length of a polygon's outline. */
internal fun kitPerimeter(p: List<Point>): Double = p.indices.sumOf { kitDist(p[it], p[(it + 1) % p.size]) }

/** Twice-signed shoelace area / 2 (sign follows the corner order). */
internal fun kitSignedArea(p: List<Point>): Double =
    p.indices.sumOf { val a = p[it]; val b = p[(it + 1) % p.size]; a.x * b.y - b.x * a.y } / 2.0

/**
 * The length of outline two polygons share: for every pair of collinear sides (both ends of one
 * within [KIT_TOL] of the other's line), the overlap of their projections. A touch at a single
 * corner gives 0 — the rule "a single shared corner point does not connect".
 */
internal fun kitSharedLength(a: List<Point>, b: List<Point>): Double {
    var total = 0.0
    for (i in a.indices) {
        val a1 = a[i]
        val a2 = a[(i + 1) % a.size]
        val len = kitDist(a1, a2)
        val ux = (a2.x - a1.x) / len
        val uy = (a2.y - a1.y) / len
        for (j in b.indices) {
            val b1 = b[j]
            val b2 = b[(j + 1) % b.size]
            val off1 = abs(ux * (b1.y - a1.y) - uy * (b1.x - a1.x))
            val off2 = abs(ux * (b2.y - a1.y) - uy * (b2.x - a1.x))
            if (off1 > KIT_TOL || off2 > KIT_TOL) continue
            val t1 = (b1.x - a1.x) * ux + (b1.y - a1.y) * uy
            val t2 = (b2.x - a1.x) * ux + (b2.y - a1.y) * uy
            val lo = max(0.0, min(t1, t2))
            val hi = min(len, max(t1, t2))
            if (hi > lo) total += hi - lo
        }
    }
    return total
}

/**
 * Each piece's exposure: the share of its outline on the silhouette's edge, 1 − (outline shared
 * with the other pieces) / perimeter. Pieces that do not overlap cover the silhouette exactly, so
 * an unshared stretch of outline is silhouette edge (the warm-up measure).
 */
internal fun kitExposures(pieces: List<KitPiece>): Map<String, Double> =
    pieces.indices.associate { i ->
        val p = pieces[i].polygon
        val shared = pieces.indices.filter { it != i }.sumOf { kitSharedLength(p, pieces[it].polygon) }
        pieces[i].piece to 1.0 - shared / kitPerimeter(p)
    }

/** Whether every piece is reachable from the first through shared outline longer than [KIT_TOL]. */
internal fun kitEdgeConnected(pieces: List<KitPiece>): Boolean {
    if (pieces.isEmpty()) return false
    val seen = mutableSetOf(0)
    val stack = ArrayDeque(listOf(0))
    while (stack.isNotEmpty()) {
        val k = stack.removeLast()
        for (m in pieces.indices) {
            if (m !in seen && kitSharedLength(pieces[k].polygon, pieces[m].polygon) > KIT_TOL) {
                seen += m
                stack.addLast(m)
            }
        }
    }
    return seen.size == pieces.size
}

/**
 * Whether two convex polygons overlap by area (every TYPE-001 piece is convex): no side normal of
 * either separates them by more than −[KIT_TOL] (separating-axis test). Pieces that share a side or
 * a corner do not overlap.
 */
internal fun kitOverlaps(a: List<Point>, b: List<Point>): Boolean {
    for (poly in listOf(a, b)) {
        for (i in poly.indices) {
            val p1 = poly[i]
            val p2 = poly[(i + 1) % poly.size]
            val n = kitDist(p1, p2)
            val nx = -(p2.y - p1.y) / n
            val ny = (p2.x - p1.x) / n
            val pa = a.map { it.x * nx + it.y * ny }
            val pb = b.map { it.x * nx + it.y * ny }
            if (min(pa.max(), pb.max()) - max(pa.min(), pb.min()) <= KIT_TOL) return false
        }
    }
    return true
}

/**
 * The side lengths of each TYPE-001 shape in boundary order (TYPE-001 sizes: the set makes a 4 × 4
 * square, large-triangle long side 4, small-triangle long side 2, square side √2; Spec/03 §1, §3).
 */
private val KIT_SIDES: Map<String, List<Double>> = mapOf(
    "LT" to listOf(4.0, 2 * KIT_SQRT2, 2 * KIT_SQRT2),
    "MT" to listOf(2.0, 2 * KIT_SQRT2, 2.0),
    "ST" to listOf(2.0, KIT_SQRT2, KIT_SQRT2),
    "SQ" to listOf(KIT_SQRT2, KIT_SQRT2, KIT_SQRT2, KIT_SQRT2),
    "PG" to listOf(2.0, KIT_SQRT2, 2.0, KIT_SQRT2),
)

/**
 * Whether [polygon] is congruent to [piece]'s TYPE-001 shape: its sides, read in boundary order from
 * some corner in either direction, match the shape's within [KIT_TOL], and its area matches
 * [PieceId.type]'s area. A crossed (bow-tie) outline fails on its area. Mirror images pass.
 */
internal fun kitCongruent(piece: PieceId, polygon: List<Point>): Boolean {
    val want = KIT_SIDES.getValue(piece.type.name)
    if (polygon.size != want.size) return false
    val sides = polygon.indices.map { kitDist(polygon[it], polygon[(it + 1) % polygon.size]) }
    val n = sides.size
    val sidesMatch = listOf(sides, sides.reversed()).any { seq ->
        (0 until n).any { k -> (0 until n).all { i -> abs(seq[(i + k) % n] - want[i]) <= KIT_TOL } }
    }
    return sidesMatch && abs(abs(kitSignedArea(polygon)) - piece.type.area) <= KIT_TOL
}
