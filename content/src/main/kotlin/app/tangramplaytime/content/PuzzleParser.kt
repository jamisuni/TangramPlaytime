// The `tangram-puzzle/1` parser and its structural rules (designs/WO-002-design.md §2
// `PuzzleParser.parse`; §3 "`ART` rejects" and "Format narrowing"). The geometric rules are
// `ValidityCheck`'s, run by the loader once a parse is clean.
package app.tangramplaytime.content

import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.contracts.ArtShape
import app.tangramplaytime.contracts.ArtStyle
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.SolvedPiece
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.sqrt

/** The outcome of [PuzzleParser.parse]. */
internal sealed interface Parsed {
    /** The text breaks no structural rule; [def] is the puzzle it describes. */
    data class Ok(val def: PuzzleDefinition) : Parsed

    /** The text breaks at least one structural rule (or is not a puzzle file at all). */
    data class Bad(val violations: List<Violation>) : Parsed
}

internal object PuzzleParser {
    private const val FORMAT_TAG = "tangram-puzzle/1"

    /** Spec/03 §2 and the schema: `<category>-<name>`, lower-case kebab-case. */
    private val KEBAB = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

    /** Spec/03 §2: the eight categories. */
    private val CATEGORIES = setOf("shapes", "animals", "people", "things", "vehicles", "nature", "letters", "numbers")

    /** Spec/03 §2 `kind`; the list order is the enum's (REQ-040). */
    private val KINDS = mapOf("mini" to PuzzleKind.MINI, "warmup" to PuzzleKind.WARMUP, "full" to PuzzleKind.FULL)

    /** The titles every puzzle carries (REQ-047 as locked; design-inputs §2 item 9: both stay mandatory). */
    private val TITLE_LANGUAGES = listOf("en", "fi")

    private val HEX = Regex("^#[0-9A-Fa-f]{6}$")

    /** `ArtShape.Path` KDoc: absolute M/L/Q/C/Z, with this many numbers per command. */
    private val PATH_ARITY = mapOf('M' to 2, 'L' to 2, 'Q' to 4, 'C' to 6, 'Z' to 0)

    private val PATH_NUMBER = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")

    private val SQRT2 = sqrt(2.0)

    /**
     * Decodes [src] and checks the structural rules: `FORMAT`, `ID`, `TITLE`, `CATEGORY`, `RATING`,
     * `KIND`, `PIECE_SET`, `PLACEMENT` and `ART`. A text that does not decode is one `JSON` violation.
     *
     * @return [Parsed.Ok] with the definition when no rule is broken, otherwise [Parsed.Bad] with every
     *   structural violation of the text.
     */
    fun parse(src: PuzzleSource): Parsed {
        val dto = try {
            Json.decodeFromString(PuzzleDto.serializer(), src.text)
        } catch (e: IllegalArgumentException) { // SerializationException and NumberFormatException included
            val reason = e.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
            return Parsed.Bad(listOf(Violation(src.fileName, Rule.JSON, "not a tangram-puzzle/1 file: $reason")))
        }

        val violations = mutableListOf<Violation>()
        val bad = { rule: Rule, detail: String -> violations += Violation(src.fileName, rule, detail) }

        if (dto.format != FORMAT_TAG) bad(Rule.FORMAT, "the format is \"${dto.format}\", not \"$FORMAT_TAG\"")
        if (!KEBAB.matches(dto.id)) bad(Rule.ID, "the id \"${dto.id}\" is not kebab-case")
        if ("${dto.id}.json" != src.fileName) bad(Rule.ID, "the id \"${dto.id}\" is not the file name ${src.fileName}")
        for (language in TITLE_LANGUAGES) {
            if (dto.title[language].isNullOrBlank()) bad(Rule.TITLE, "no \"$language\" title")
        }
        if (dto.category !in CATEGORIES) bad(Rule.CATEGORY, "the category \"${dto.category}\" is none of $CATEGORIES")
        val kind = KINDS[dto.kind]
        if (kind == null) bad(Rule.KIND, "the kind \"${dto.kind}\" is none of ${KINDS.keys}")
        if (dto.difficulty !in 1..5) {
            bad(Rule.RATING, "the difficulty ${dto.difficulty} is not 1 to 5")
        } else if (kind == PuzzleKind.MINI && dto.difficulty != 1) {
            bad(Rule.RATING, "a mini puzzle has rating 1, not ${dto.difficulty}")
        }
        val pieces = pieceIds(dto.solution, kind, bad)
        val polygons = dto.solution.mapIndexed { i, entry -> polygonOf(i, entry, bad) }
        checkArt(dto.art, bad)

        if (violations.isNotEmpty()) return Parsed.Bad(violations.toList())
        return Parsed.Ok(
            PuzzleDefinition(
                id = dto.id,
                titles = dto.title,
                category = dto.category,
                rating = dto.difficulty,
                kind = kind!!, // a null kind is a KIND violation, returned above
                solution = pieces.indices.map { SolvedPiece(pieces[it]!!, polygons[it]!!) },
                art = artOf(dto.art),
                reviewedByHuman = dto.provenance?.reviewedByHuman ?: false,
            ),
        )
    }

    /** `PIECE_SET`: each id a TYPE-001 piece, each once; all seven, or 1 to 6 distinct for a mini. */
    private fun pieceIds(solution: List<PlacementDto>, kind: PuzzleKind?, bad: (Rule, String) -> Unit): List<PieceId?> {
        val ids = solution.mapIndexed { i, entry ->
            PieceId.entries.firstOrNull { it.name == entry.piece }
                .also { if (it == null) bad(Rule.PIECE_SET, "solution[$i]: \"${entry.piece}\" is not a TYPE-001 piece id") }
        }
        val known = ids.filterNotNull()
        for ((id, count) in known.groupingBy { it }.eachCount()) {
            if (count > 1) bad(Rule.PIECE_SET, "$id is used $count times")
        }
        val distinct = known.toSet()
        when (kind) {
            PuzzleKind.MINI ->
                if (distinct.size !in 1..6) bad(Rule.PIECE_SET, "a mini puzzle uses 1 to 6 distinct pieces, not ${distinct.size}")
            PuzzleKind.WARMUP, PuzzleKind.FULL -> {
                val missing = PieceId.entries.filter { it !in distinct }
                if (missing.isNotEmpty()) bad(Rule.PIECE_SET, "a ${kind.toString().lowercase()} puzzle uses all seven pieces; missing $missing")
            }
            null -> Unit // the kind is a KIND violation; no piece count applies
        }
        return ids
    }

    /**
     * `PLACEMENT`: the entry gives its `polygon` of 3 or 4 corners, each two coordinates. The `rot`/`flip`/`at`
     * form is not read (review F1; design §3 "Format narrowing").
     *
     * @return the corners in the file's order, or null after reporting the violation.
     */
    private fun polygonOf(i: Int, entry: PlacementDto, bad: (Rule, String) -> Unit): List<Point>? {
        val where = "solution[$i] (${entry.piece})"
        val otherForm = listOfNotNull(
            "rot".takeIf { entry.rot != null },
            "flip".takeIf { entry.flip != null },
            "at".takeIf { entry.at != null },
        )
        if (otherForm.isNotEmpty()) {
            bad(Rule.PLACEMENT, "$where: the ${otherForm.joinToString("/")} placement form is not supported; give the piece's polygon")
            return null
        }
        val polygon = entry.polygon
        if (polygon == null) {
            bad(Rule.PLACEMENT, "$where: no polygon")
            return null
        }
        if (polygon.size !in 3..4) {
            bad(Rule.PLACEMENT, "$where: a polygon has 3 or 4 corners, not ${polygon.size}")
            return null
        }
        return polygon.mapIndexed { j, corner ->
            val x = corner.getOrNull(0)?.let(::coordinate)
            val y = corner.getOrNull(1)?.let(::coordinate)
            if (corner.size != 2 || x == null || y == null) {
                bad(Rule.PLACEMENT, "$where: corner $j is not two coordinates, each a number or [a, b]: $corner")
                return null
            }
            Point(x, y)
        }
    }

    /** A coordinate (Spec/03 §1): a number as it is, or `[a, b]` = a + b·√2; anything else is null. */
    private fun coordinate(element: JsonElement): Double? = when (element) {
        is JsonPrimitive -> number(element)
        is JsonArray -> {
            val a = (element.getOrNull(0) as? JsonPrimitive)?.let(::number)
            val b = (element.getOrNull(1) as? JsonPrimitive)?.let(::number)
            if (element.size == 2 && a != null && b != null) a + b * SQRT2 else null
        }
        else -> null
    }

    private fun number(primitive: JsonPrimitive): Double? =
        if (primitive.isString) null else primitive.doubleOrNull?.takeIf { it.isFinite() }

    /**
     * `ART` (decisions.md 2026-10-11, "a well-formed picture"; design §3 "`ART` rejects"): `#RRGGBB`
     * colours, opacity 0..1, width > 0, a polygon of at least 3 points, points of two numbers, a stroke
     * on every `line` and `path`, and a `path.d` of absolute M/L/Q/C/Z commands.
     */
    private fun checkArt(art: ArtDto, bad: (Rule, String) -> Unit) {
        if (!HEX.matches(art.base)) bad(Rule.ART, "art.base \"${art.base}\" is not #RRGGBB")
        art.shapes.forEachIndexed { i, shape ->
            val where = "art.shapes[$i] (${typeOf(shape)})"
            shape.fill?.let { if (!HEX.matches(it)) bad(Rule.ART, "$where: fill \"$it\" is not #RRGGBB") }
            shape.stroke?.let { if (!HEX.matches(it)) bad(Rule.ART, "$where: stroke \"$it\" is not #RRGGBB") }
            shape.width?.let { if (!(it > 0.0)) bad(Rule.ART, "$where: width $it is not above 0") }
            shape.opacity?.let { if (it !in 0.0..1.0) bad(Rule.ART, "$where: opacity $it is not 0 to 1") }
            val points: List<List<Double>> = when (shape) {
                is ShapeDto.Polygon -> {
                    if (shape.points.size < 3) bad(Rule.ART, "$where: ${shape.points.size} points, fewer than 3")
                    shape.points
                }
                is ShapeDto.Rect -> emptyList()
                is ShapeDto.Circle -> listOf(shape.c)
                is ShapeDto.Ellipse -> listOf(shape.c)
                is ShapeDto.Line -> listOf(shape.from, shape.to)
                is ShapeDto.Path -> {
                    pathProblem(shape.d)?.let { bad(Rule.ART, "$where: d \"${shape.d}\" $it") }
                    emptyList()
                }
            }
            for (point in points) {
                if (point.size != 2) bad(Rule.ART, "$where: the point $point is not two numbers")
            }
            if ((shape is ShapeDto.Line || shape is ShapeDto.Path) && shape.stroke == null) {
                bad(Rule.ART, "$where: a ${typeOf(shape)} needs a stroke colour")
            }
        }
    }

    /**
     * Why [d] is not absolute M/L/Q/C/Z with 2/2/4/6/0 numbers per command, starting with M — or null
     * when it is (`ArtShape.Path` KDoc).
     */
    private fun pathProblem(d: String): String? {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < d.length) {
            val ch = d[i]
            when {
                ch.isWhitespace() || ch == ',' -> i++
                ch.isLetter() -> {
                    tokens += ch.toString()
                    i++
                }
                else -> {
                    val match = PATH_NUMBER.matchAt(d, i) ?: return "has '$ch' where a command or a number belongs"
                    tokens += match.value
                    i = match.range.last + 1
                }
            }
        }
        if (tokens.firstOrNull() != "M") return "does not start with M"
        var k = 0
        while (k < tokens.size) {
            val command = tokens[k].single()
            val arity = PATH_ARITY[command] ?: return "has the command '$command'; only absolute M, L, Q, C and Z are read"
            k++
            var count = 0
            while (k < tokens.size && !tokens[k].first().isLetter()) {
                count++
                k++
            }
            if (count != arity) return "gives $command $count numbers, not $arity"
        }
        return null
    }

    private fun typeOf(shape: ShapeDto): String = when (shape) {
        is ShapeDto.Polygon -> "polygon"
        is ShapeDto.Rect -> "rect"
        is ShapeDto.Circle -> "circle"
        is ShapeDto.Ellipse -> "ellipse"
        is ShapeDto.Line -> "line"
        is ShapeDto.Path -> "path"
    }

    /** The picture in contract terms; called only on a picture that passed [checkArt]. */
    private fun artOf(art: ArtDto): ArtLayer = ArtLayer(
        base = art.base,
        shapes = art.shapes.map { shape ->
            val style = ArtStyle(shape.fill, shape.stroke, shape.width, shape.opacity)
            when (shape) {
                is ShapeDto.Polygon -> ArtShape.Polygon(shape.points.map(::pointOf), style)
                is ShapeDto.Rect -> ArtShape.Rect(shape.x, shape.y, shape.w, shape.h, shape.rx, style)
                is ShapeDto.Circle -> ArtShape.Circle(pointOf(shape.c), shape.r, style)
                is ShapeDto.Ellipse -> ArtShape.Ellipse(pointOf(shape.c), shape.rx, shape.ry, style)
                is ShapeDto.Line -> ArtShape.Line(pointOf(shape.from), pointOf(shape.to), style)
                is ShapeDto.Path -> ArtShape.Path(shape.d, style)
            }
        },
    )

    private fun pointOf(xy: List<Double>): Point = Point(xy[0], xy[1])
}
