// The decoding shapes of a `tangram-puzzle/1` file (designs/WO-002-design.md §2 `PuzzleParser.parse`,
// "Decoding" and "Attribution"). Decoded with the default `Json`: an unknown key, a wrong JSON type or a
// missing required field throws, which the parser reports as `Rule.JSON`. Every rule-checked field keeps
// its raw JSON type here, so a value of the right type that breaks a rule is reported by that rule.
// Internal only: no DTO crosses the `IPuzzleLibrary` seam (its KDoc "must never").
package app.tangramplaytime.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** One puzzle file (Spec/03-puzzle-format.md §2; schema `tangram-puzzle/1`). `kind` defaults to full. */
@Serializable
internal data class PuzzleDto(
    val format: String,
    val id: String,
    val title: Map<String, String>,
    val category: String,
    val difficulty: Int,
    val kind: String = "full",
    val solution: List<PlacementDto>,
    val art: ArtDto,
    val provenance: ProvenanceDto? = null,
)

/**
 * One solution entry. Each coordinate stays a [JsonElement] (a number or `[a, b]`; the union cannot be
 * declared). `rot`, `flip` and `at` are declared so that the format's other placement form is reported
 * as `Rule.PLACEMENT`, never `Rule.JSON` (review F1; decisions.md 2026-10-11, "not read").
 */
@Serializable
internal data class PlacementDto(
    val piece: String,
    val polygon: List<List<JsonElement>>? = null,
    val rot: Int? = null,
    val flip: Boolean? = null,
    val at: JsonElement? = null,
)

/** `provenance`; a missing flag reads as not reviewed (decisions.md 2026-10-11). */
@Serializable
internal data class ProvenanceDto(
    val author: String? = null,
    val reviewedByHuman: Boolean? = null,
)

/** The solved picture: a base colour and the shapes in drawing order. */
@Serializable
internal data class ArtDto(
    val base: String,
    val shapes: List<ShapeDto>,
)

/**
 * One art shape, chosen by its `type` (the default class discriminator). An unknown type throws while
 * decoding. `stroke` is optional on every shape, so a `line` or `path` without one is `Rule.ART` (V10).
 * A point is a list of numbers, so a point that is not two numbers is `Rule.ART`.
 */
@Serializable
internal sealed class ShapeDto {
    abstract val fill: String?
    abstract val stroke: String?
    abstract val width: Double?
    abstract val opacity: Double?

    @Serializable
    @SerialName("polygon")
    data class Polygon(
        val points: List<List<Double>>,
        override val fill: String? = null,
        override val stroke: String? = null,
        override val width: Double? = null,
        override val opacity: Double? = null,
    ) : ShapeDto()

    @Serializable
    @SerialName("rect")
    data class Rect(
        val x: Double,
        val y: Double,
        val w: Double,
        val h: Double,
        val rx: Double? = null,
        override val fill: String? = null,
        override val stroke: String? = null,
        override val width: Double? = null,
        override val opacity: Double? = null,
    ) : ShapeDto()

    @Serializable
    @SerialName("circle")
    data class Circle(
        val c: List<Double>,
        val r: Double,
        override val fill: String? = null,
        override val stroke: String? = null,
        override val width: Double? = null,
        override val opacity: Double? = null,
    ) : ShapeDto()

    @Serializable
    @SerialName("ellipse")
    data class Ellipse(
        val c: List<Double>,
        val rx: Double,
        val ry: Double,
        override val fill: String? = null,
        override val stroke: String? = null,
        override val width: Double? = null,
        override val opacity: Double? = null,
    ) : ShapeDto()

    @Serializable
    @SerialName("line")
    data class Line(
        val from: List<Double>,
        val to: List<Double>,
        override val fill: String? = null,
        override val stroke: String? = null,
        override val width: Double? = null,
        override val opacity: Double? = null,
    ) : ShapeDto()

    @Serializable
    @SerialName("path")
    data class Path(
        val d: String,
        override val fill: String? = null,
        override val stroke: String? = null,
        override val width: Double? = null,
        override val opacity: Double? = null,
    ) : ShapeDto()
}
