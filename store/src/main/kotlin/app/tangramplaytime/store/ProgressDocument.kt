// ADR-003 (designs/WO-004-design.md §1; decisions.md 2026-10-15, the ADR-003 row and its amendment):
// the format-1 document of `progress.json`, its private DTOs, and the mapping to and from contract types.
// No DTO or serialization type leaves this file (IProgressStore KDoc: must never expose a serialisation type).
package app.tangramplaytime.store

import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PlayTimes
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Settings
import app.tangramplaytime.contracts.Turn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** ADR-003: the only format this store reads; absent or 0 is older, above 1 is unknown. */
private const val FORMAT = 1

/**
 * Strict JSON (ADR-003 "Decoding is strict"): the library defaults — an unknown key, an unknown enum
 * value, a non-finite number, a missing field or a type mismatch fails the decode; `null` is written
 * explicitly for `best` and `date`; `lastOpened` is left out while it has no value.
 */
private val json: Json = Json

/**
 * The in-memory document of ADR-003 in contract types: what the file holds, read once and rewritten whole
 * on every save.
 */
internal data class ProgressDocument(
    val lastOpened: String?,
    val puzzles: Map<String, PuzzleProgress>,
    val playTimes: PlayTimes,
    val settings: Settings,
) {
    /** The whole document as format-1 JSON text. Throws on a value JSON cannot hold (a non-finite number). */
    fun encode(): String = json.encodeToString(DocumentDto.serializer(), toDto())

    companion object {
        /** A fresh install (IProgressStore KDoc): no puzzle saved, no play time, default settings, no last-opened id. */
        val FRESH: ProgressDocument = ProgressDocument(null, emptyMap(), PlayTimes.NONE, Settings())

        /**
         * Decodes, validates and maps [text]. Throws on anything ADR-003 reads as unreadable: `format` ≠ 1,
         * an unknown key or enum, `turn` outside 0..7, a non-finite number, a negative time or an invalid date.
         */
        fun decode(text: String): ProgressDocument {
            val dto = json.decodeFromString(DocumentDto.serializer(), text)
            require(dto.format == FORMAT) { "format ${dto.format} is not $FORMAT" }
            return dto.toDocument()
        }
    }
}

// ── The format-1 DTOs (ADR-003). Property names are the file's keys; declaration order is the written order. ──

@Serializable
private data class DocumentDto(
    val format: Int,
    val lastOpened: String? = null,
    val puzzles: Map<String, PuzzleDto>,
    val playTimes: PlayTimesDto,
    val settings: SettingsDto,
)

@Serializable
private data class PuzzleDto(
    val state: StateDto,
    val placed: List<PlacementDto>,
    val time: Int,
    val best: Int?,
)

@Serializable
private data class PlacementDto(
    val piece: PieceDto,
    val turn: Int,
    val mirrored: Boolean,
    val x: Double,
    val y: Double,
)

@Serializable
private data class PlayTimesDto(
    val today: Int,
    val date: String?,
    val allTime: Int,
)

@Serializable
private data class SettingsDto(
    val timerShown: Boolean,
    val soundOn: Boolean,
)

/** The file's state names (ADR-003 `"NEW|IN_PROGRESS|SOLVED"`), kept apart from the contract enum. */
@Serializable
private enum class StateDto { NEW, IN_PROGRESS, SOLVED }

/** The file's piece names (TYPE-001), kept apart from the contract enum. */
@Serializable
private enum class PieceDto { LT1, LT2, MT, SQ, PG, ST1, ST2 }

// ── Contract → DTO (writing) ──

private fun ProgressDocument.toDto(): DocumentDto = DocumentDto(
    format = FORMAT,
    lastOpened = lastOpened,
    puzzles = puzzles.mapValues { (_, p) -> p.toDto() },
    playTimes = PlayTimesDto(playTimes.todaySeconds, playTimes.todayDate?.toString(), playTimes.allTimeSeconds),
    settings = SettingsDto(settings.timerShown, settings.soundOn),
)

private fun PuzzleProgress.toDto(): PuzzleDto = PuzzleDto(
    state = when (state) {
        PuzzleState.NEW -> StateDto.NEW
        PuzzleState.IN_PROGRESS -> StateDto.IN_PROGRESS
        PuzzleState.SOLVED -> StateDto.SOLVED
    },
    placed = placed.map { it.toDto() },
    time = timeSeconds,
    best = bestSeconds,
)

private fun Placement.toDto(): PlacementDto = PlacementDto(
    piece = when (piece) {
        PieceId.LT1 -> PieceDto.LT1
        PieceId.LT2 -> PieceDto.LT2
        PieceId.MT -> PieceDto.MT
        PieceId.SQ -> PieceDto.SQ
        PieceId.PG -> PieceDto.PG
        PieceId.ST1 -> PieceDto.ST1
        PieceId.ST2 -> PieceDto.ST2
    },
    turn = turn.step,
    mirrored = mirrored,
    x = centre.x,
    y = centre.y,
)

// ── DTO → contract (reading), with the ADR-003 validation ──

private fun DocumentDto.toDocument(): ProgressDocument = ProgressDocument(
    lastOpened = lastOpened,
    puzzles = puzzles.mapValues { (id, p) -> p.toProgress(id) },
    playTimes = playTimes.toPlayTimes(),
    settings = Settings(timerShown = settings.timerShown, soundOn = settings.soundOn),
)

private fun PuzzleDto.toProgress(id: String): PuzzleProgress {
    require(time >= 0) { "$id: negative time $time" }
    require(best == null || best >= 0) { "$id: negative best time $best" }
    return PuzzleProgress(
        state = when (state) {
            StateDto.NEW -> PuzzleState.NEW
            StateDto.IN_PROGRESS -> PuzzleState.IN_PROGRESS
            StateDto.SOLVED -> PuzzleState.SOLVED
        },
        placed = placed.map { it.toPlacement(id) },
        timeSeconds = time,
        bestSeconds = best,
    )
}

private fun PlacementDto.toPlacement(id: String): Placement {
    require(turn in 0..7) { "$id: turn $turn outside 0..7" }
    require(x.isFinite() && y.isFinite()) { "$id: non-finite centre ($x, $y)" }
    return Placement(
        piece = when (piece) {
            PieceDto.LT1 -> PieceId.LT1
            PieceDto.LT2 -> PieceId.LT2
            PieceDto.MT -> PieceId.MT
            PieceDto.SQ -> PieceId.SQ
            PieceDto.PG -> PieceId.PG
            PieceDto.ST1 -> PieceId.ST1
            PieceDto.ST2 -> PieceId.ST2
        },
        turn = Turn(turn),
        mirrored = mirrored,
        centre = Point(x, y),
    )
}

private fun PlayTimesDto.toPlayTimes(): PlayTimes {
    require(today >= 0) { "negative today time $today" }
    require(allTime >= 0) { "negative all-time time $allTime" }
    // ISO yyyy-MM-dd, strict: "2026-02-30" throws DateTimeParseException (design review F1).
    return PlayTimes(todaySeconds = today, todayDate = date?.let { LocalDate.parse(it) }, allTimeSeconds = allTime)
}
