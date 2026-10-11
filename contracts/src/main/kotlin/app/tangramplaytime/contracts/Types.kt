// ─────────────────────────────────────────────────────────────────────────────
// GOVERNED INTERFACE — SWDev Contract (see architecture.md §4)
// Tier: notify. Changes ONLY by common agreement, recorded like a REQ change.
// Serves: TYPE-001, TYPE-003, TYPE-005, TYPE-006; REQ-012, REQ-016, REQ-017,
//         REQ-018, REQ-022, REQ-023, REQ-025, REQ-029, REQ-030, REQ-031,
//         REQ-033, REQ-038, REQ-039, REQ-040, REQ-041, REQ-045, REQ-047
// ─────────────────────────────────────────────────────────────────────────────
package app.tangramplaytime.contracts

import java.time.LocalDate

/**
 * The five piece shapes of the classic set (TYPE-001), with each shape's area in
 * TYPE-001 size units (the full set assembles into a 4 × 4 square, area 16).
 *
 * Must never: gain a shape outside TYPE-001 (other piece sets are the `idea`
 * feature #PieceSets, not a build unit).
 *
 * @property area the shape's area in square puzzle units (TYPE-001).
 */
enum class PieceType(val area: Int) {
    /** Large triangle, area 4 (TYPE-001). */
    LT(4),

    /** Medium triangle, area 2 (TYPE-001). */
    MT(2),

    /** Small triangle, area 1 (TYPE-001). */
    ST(1),

    /** Square, area 2 (TYPE-001). */
    SQ(2),

    /** Parallelogram, area 2 — the only shape whose mirror image differs (TYPE-003, REQ-018). */
    PG(2),
}

/**
 * The seven pieces of TYPE-001, each with its shape.
 *
 * **Declaration order is the tray order** (TYPE-001: LT1, LT2, MT, SQ, PG, ST1, ST2,
 * left to right and top to bottom; REQ-012). `PieceId.entries` and the natural
 * `compareTo` of this enum are therefore the tray order; a mini puzzle (REQ-045)
 * shows its own pieces in this same relative order (REQ-012 rule).
 *
 * LT1/LT2 are interchangeable, and so are ST1/ST2 (TYPE-001): solving never
 * requires a specific one of a congruent pair (REQ-022 — any exact cover counts).
 *
 * Must never: be reordered (it would change every tray, REQ-012.A1).
 *
 * @property type the piece's shape (TYPE-001).
 */
enum class PieceId(val type: PieceType) {
    /** First large triangle (TYPE-001). */
    LT1(PieceType.LT),

    /** Second large triangle (TYPE-001). */
    LT2(PieceType.LT),

    /** Medium triangle (TYPE-001). */
    MT(PieceType.MT),

    /** Square (TYPE-001). */
    SQ(PieceType.SQ),

    /** Parallelogram (TYPE-001); the only piece with a flip badge (REQ-018.A2). */
    PG(PieceType.PG),

    /** First small triangle (TYPE-001). */
    ST1(PieceType.ST),

    /** Second small triangle (TYPE-001). */
    ST2(PieceType.ST),
}

/**
 * A piece's orientation as a number of 45° clockwise steps (TYPE-003): one step is
 * 45°, so there are exactly 8 turns, `step` 0..7 (0°, 45° … 315°). Step 0 is the
 * piece's reference shape of the puzzle format (`Spec/03-puzzle-format.md` §3,
 * `rot` 0); positive is clockwise on screen (y points down).
 *
 * A turn keeps the piece's centre (the average of its corners) fixed (TYPE-003);
 * that geometry is [ILockEngine.vertices]'s, never the caller's (G-04).
 *
 * Must never: hold a value outside 0..7 — constructing one is a programming error
 * and throws [IllegalArgumentException] (G-15: exceptions mean programming errors).
 *
 * @property step the number of 45° clockwise steps, 0..7.
 */
@JvmInline
value class Turn(val step: Int) {
    init {
        require(step in 0..7) { "Turn step must be 0..7, was $step" }
    }

    /**
     * One 45° step clockwise (REQ-016: one tap turns one step; REQ-017: a twist
     * passing a further step boundary clockwise). `Turn(7).next()` is `Turn(0)`, so
     * eight steps return a piece to its starting turn (REQ-016.A1).
     */
    fun next(): Turn = Turn((step + 1) % 8)

    /**
     * One 45° step counter-clockwise: a two-finger twist passing a further step
     * boundary in the other direction (REQ-017 boundaries at ±22.5°, ±67.5° …).
     * `Turn(0).previous()` is `Turn(7)`.
     */
    fun previous(): Turn = Turn((step + 7) % 8)
}

/**
 * A point in puzzle units (TYPE-001: the full set is a 4 × 4 square), x to the right
 * and **y down** (`Spec/03-puzzle-format.md` §1). Runtime geometry uses doubles with
 * the TYPE-004 tolerance (at most 1e-6 units; ADR-001); exact a + b√2 arithmetic
 * stays in the authoring tools.
 *
 * Must never: carry screen units (dp/px) — conversion to the screen is the UI's,
 * through the `dpPerUnit` scale it passes to [ILockEngine.findLock].
 *
 * @property x horizontal position in puzzle units, increasing to the right.
 * @property y vertical position in puzzle units, increasing downwards.
 */
data class Point(val x: Double, val y: Double)

/**
 * One piece locked on the board: which piece, its turn, its mirror state and the
 * position of its centre (the average of its corners, TYPE-003). This is what is
 * saved per board piece (REQ-025 rule: "position, turn, mirror"); a piece that has
 * no placement is in the tray.
 *
 * Must never: describe a loose piece — every placement on the board is a lock found
 * by [ILockEngine] (REQ-020: no loose piece on the board at any time).
 *
 * @property piece the piece (TYPE-001).
 * @property turn its orientation (TYPE-003).
 * @property mirrored whether it is mirrored; only visible for [PieceId.PG] (TYPE-003, REQ-018).
 * @property centre the piece's centre in puzzle units.
 */
data class Placement(
    val piece: PieceId,
    val turn: Turn,
    val mirrored: Boolean,
    val centre: Point,
)

/**
 * The puzzle file's `kind` (REQ-040 rule; `Spec/03-puzzle-format.md` §2).
 * **Declaration order is the list order**: mini, then warm-up, then full (REQ-040,
 * REQ-045, REQ-041).
 */
enum class PuzzleKind {
    /** A small first-success puzzle using 1–6 distinct pieces, rating 1, listed first (REQ-045). */
    MINI,

    /** All seven pieces, mostly along the silhouette edge (validator V12); listed after the minis (REQ-041). */
    WARMUP,

    /** All seven pieces (REQ-038); listed after every warm-up (REQ-040.A2). */
    FULL,
}

/**
 * One piece of a puzzle's stored solution, as the corners of its polygon in puzzle
 * units. The solution draws the silhouette and supplies the outline corners (the lock
 * anchors, REQ-019); it is **one** valid arrangement, never the only accepted answer
 * (REQ-022: no comparison with the stored solution is made).
 *
 * @property piece the piece (TYPE-001).
 * @property polygon the piece's corners in puzzle units, in the file's order.
 */
data class SolvedPiece(val piece: PieceId, val polygon: List<Point>)

/**
 * Drawing style shared by every art shape (`Spec/03-puzzle-format.md` §2 "Art shapes":
 * every shape may have `fill`, `stroke`, `width` and `opacity`). Colours are
 * `#RRGGBB` strings; `null` means the attribute is absent in the file.
 *
 * @property fill fill colour, `#RRGGBB`, or null for no fill.
 * @property stroke stroke colour, `#RRGGBB`, or null for no stroke.
 * @property width stroke width in puzzle units, or null when absent.
 * @property opacity 0.0..1.0, or null for fully opaque.
 */
data class ArtStyle(
    val fill: String? = null,
    val stroke: String? = null,
    val width: Double? = null,
    val opacity: Double? = null,
)

/**
 * One flat shape of a puzzle's solved picture (REQ-023, REQ-039;
 * `Spec/03-puzzle-format.md` §2 "Art shapes"). Coordinates are puzzle units.
 *
 * Must never: carry text (REQ-039 rule: no text in pictures, REQ-047) — there is no
 * text shape.
 */
sealed interface ArtShape {
    /** The shape's fill, stroke, stroke width and opacity. */
    val style: ArtStyle

    /** A closed polygon through [points]. */
    data class Polygon(val points: List<Point>, override val style: ArtStyle) : ArtShape

    /** An axis-aligned rectangle at ([x], [y]) of size [w] × [h], corner radius [rx] when given. */
    data class Rect(
        val x: Double,
        val y: Double,
        val w: Double,
        val h: Double,
        val rx: Double?,
        override val style: ArtStyle,
    ) : ArtShape

    /** A circle with centre [c] and radius [r]. */
    data class Circle(val c: Point, val r: Double, override val style: ArtStyle) : ArtShape

    /** An axis-aligned ellipse with centre [c] and radii [rx], [ry]. */
    data class Ellipse(val c: Point, val rx: Double, val ry: Double, override val style: ArtStyle) : ArtShape

    /** A straight line from [from] to [to]; drawn with the style's stroke and width. */
    data class Line(val from: Point, val to: Point, override val style: ArtStyle) : ArtShape

    /** A path [d] of absolute M/L/Q/C/Z commands with numbers in puzzle units. */
    data class Path(val d: String, override val style: ArtStyle) : ArtShape
}

/**
 * A puzzle's solved picture (REQ-023, REQ-039): a [base] colour filling the
 * silhouette, then [shapes] drawn in order on top.
 *
 * Must never: paint outside the puzzle's outline — whoever draws it clips to the
 * silhouette (REQ-039.A1).
 *
 * @property base the base colour, `#RRGGBB`.
 * @property shapes the shapes, in drawing order.
 */
data class ArtLayer(val base: String, val shapes: List<ArtShape>)

/**
 * One puzzle of the library (REQ-007), as read from its `tangram-puzzle/1` file.
 * Every definition the library lists is valid (REQ-038) — see [IPuzzleLibrary].
 *
 * @property id unique kebab-case id, equal to the file name; never reused, because
 *   saved progress is keyed on it (REQ-025, `Spec/03-puzzle-format.md` §2).
 * @property titles the title per language code; always has `"en"` (the Contract
 *   wording) and `"fi"` (REQ-047; English Contract pending
 *   `chg-draft-req047-english-contract.md`). The caller picks the device language and
 *   falls back to `"en"` (REQ-047 rule).
 * @property category the file's category (`shapes`, `animals`, …).
 * @property rating the difficulty rating, an integer 1..5 (REQ-040; the file's
 *   `difficulty` field); mini puzzles have rating 1 (REQ-045).
 * @property kind mini, warm-up or full (REQ-040, REQ-041, REQ-045).
 * @property solution one valid arrangement of the puzzle's pieces, each piece exactly
 *   once — all seven, or 1–6 distinct pieces for a mini puzzle (REQ-038, REQ-045).
 *   It is never the only accepted answer (REQ-022).
 * @property art the solved picture (REQ-023, REQ-039).
 * @property reviewedByHuman the file's `provenance.reviewedByHuman`; a released
 *   puzzle has `true` (REQ-039 rule and A2). The AI never sets it (owner rule 7).
 */
data class PuzzleDefinition(
    val id: String,
    val titles: Map<String, String>,
    val category: String,
    val rating: Int,
    val kind: PuzzleKind,
    val solution: List<SolvedPiece>,
    val art: ArtLayer,
    val reviewedByHuman: Boolean,
) {
    /**
     * The puzzle's pieces — the ones its tray shows at the start (REQ-012, REQ-045),
     * iterated in tray order (TYPE-001). Seven for every non-mini puzzle (REQ-038).
     */
    val pieces: Set<PieceId>
        get() = PieceId.entries.filterTo(LinkedHashSet()) { id -> solution.any { it.piece == id } }
}

/**
 * The progress state every puzzle keeps (TYPE-006): exactly three values.
 *
 * Transitions (TYPE-006): New → In progress when a piece first leaves the tray (a drag
 * starts on a tray piece — turning a tray piece, browsing past or opening the settings
 * does not start it, REQ-030.A3); In progress → Solved when the last piece locks
 * (REQ-022); Restart: In progress → New (REQ-025); Retry: Solved → New, best time kept
 * (REQ-030); a confirmed reset sets every puzzle to New (REQ-034).
 */
enum class PuzzleState {
    /** Not started, or restarted / retried / reset (TYPE-006). */
    NEW,

    /** At least one piece has left the tray; time counts only in this state (TYPE-006, REQ-030). */
    IN_PROGRESS,

    /** Every piece locked (REQ-022). */
    SOLVED,
}

/**
 * One puzzle's saved progress (REQ-003: each puzzle keeps its own state; REQ-025,
 * REQ-030).
 *
 * @property state the TYPE-006 state.
 * @property placed the pieces on the board, each a lock (REQ-025: saved per piece);
 *   pieces not listed are in the tray. Empty when [state] is New (Restart returns all
 *   pieces to the tray, REQ-025.A2) and when it is Solved (the solved view shows the
 *   picture, not the piece layout).
 * @property timeSeconds active seconds (TYPE-005) of the current attempt (REQ-030);
 *   zero when New (REQ-025.A2: after Restart the puzzle time is zero); when Solved, the
 *   time of the solve.
 * @property bestSeconds the shortest solve time, or null when the puzzle was never
 *   solved by the player (REQ-030; a solve by the developer aid sets none, REQ-046).
 *   Kept on Restart and Retry (REQ-030 rule); erased only by a reset (REQ-034).
 */
data class PuzzleProgress(
    val state: PuzzleState,
    val placed: List<Placement>,
    val timeSeconds: Int,
    val bestSeconds: Int?,
) {
    /** Contract constants. */
    companion object {
        /**
         * The progress of a puzzle never touched: New, nothing placed, no time, no best
         * time. What a fresh install (and an unreadable store) reports for every puzzle
         * (REQ-034.A1, [IProgressStore]).
         */
        val FRESH: PuzzleProgress = PuzzleProgress(PuzzleState.NEW, emptyList(), 0, null)
    }
}

/**
 * The player's active play time (REQ-029), counted in active seconds (TYPE-005).
 *
 * @property todaySeconds active seconds counted on [todayDate].
 * @property todayDate the local calendar date [todaySeconds] belongs to, or null before
 *   the first counted second. Today's time restarts at local midnight (REQ-029 rule):
 *   when [Clock.today] differs from this date, today's time is zero.
 * @property allTimeSeconds all active seconds ever counted, until a reset (REQ-034).
 */
data class PlayTimes(
    val todaySeconds: Int,
    val todayDate: LocalDate?,
    val allTimeSeconds: Int,
) {
    /** Contract constants. */
    companion object {
        /** No play time yet: a fresh install, or after a reset (REQ-034). */
        val NONE: PlayTimes = PlayTimes(0, null, 0)
    }
}

/**
 * The player's settings. A reset of progress keeps them (REQ-034 rule).
 *
 * Must never: hold a difficulty level or a language choice (REQ-004/REQ-044 withdrawn,
 * G-09; REQ-047: the device language decides).
 *
 * @property timerShown whether the running puzzle time is shown on the board; off by
 *   default (REQ-031, A1).
 * @property soundOn whether sounds and the haptic tick play; on by default (REQ-033).
 */
data class Settings(
    val timerShown: Boolean = false,
    val soundOn: Boolean = true,
)

/**
 * The one source of time for product code (TYPE-005, REQ-029, REQ-030; ADR-006,
 * guardrail G-14). `:app` provides the real clock; tests inject their own.
 *
 * Must never: be bypassed — product code outside `:app`'s implementation never reads
 * the system clock directly (G-14).
 */
interface Clock {
    /**
     * A millisecond reading that only moves forward, for measuring elapsed time
     * (active seconds and the 60 s idle limit, TYPE-005). Only differences between two
     * readings are meaningful; the zero point is unspecified.
     */
    fun nowMillis(): Long

    /**
     * The device's current local calendar date, in its current time zone; today's play
     * time restarts when it changes (REQ-029 rule: local midnight).
     */
    fun today(): LocalDate
}

/**
 * The outcome of a lock search (TYPE-004) — an expected result, never an exception
 * (G-15).
 */
sealed interface LockResult {
    /**
     * The piece locks at [placement]: inside the silhouette, overlapping no placed piece,
     * one of its corners exactly on an anchor (REQ-019, TYPE-004). The placement keeps
     * the turn and mirror that were tried (REQ-019 rule, REQ-018 rule).
     */
    data class Locked(val placement: Placement) : LockResult

    /**
     * No valid position within the lock distance: a drop goes home to its tray cell
     * (REQ-020; the "went home" signal of REQ-051), no landing preview is drawn
     * (REQ-021.A2), and a turn or mirror in place is undone with a shake (REQ-016,
     * REQ-018).
     */
    data object None : LockResult
}
