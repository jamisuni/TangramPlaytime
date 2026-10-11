// ─────────────────────────────────────────────────────────────────────────────
// GOVERNED INTERFACE — SWDev Contract (see architecture.md §4)
// Tier: notify. Changes ONLY by common agreement, recorded like a REQ change.
// Serves: REQ-012, REQ-016, REQ-017, REQ-018, REQ-019, REQ-020, REQ-021, REQ-022,
//         REQ-025, REQ-051; TYPE-001, TYPE-003, TYPE-004
// ─────────────────────────────────────────────────────────────────────────────
package app.tangramplaytime.contracts

/**
 * The one lock rule (TYPE-004, guardrail G-04), implemented once in `:kernel`: piece
 * geometry, where a dropped or turned piece locks, the landing preview, the go-home
 * decision and the solved check. The UI asks; it never derives a position itself (O-01).
 *
 * Common terms for every member:
 * - **Anchors** are only the silhouette's outline corners ([outlineCorners]) and the
 *   corners of the pieces currently on the board (REQ-019 rule). There are no hidden
 *   lock points (REQ-019.A3).
 * - **Lock distance** R = max(0.65 units, 30 dp expressed in units), i.e.
 *   `max(0.65, 30 / dpPerUnit)` (TYPE-004).
 * - **Valid** means the moved piece lies completely inside the silhouette and overlaps no
 *   placed piece, decided with a tolerance of at most 1e-6 units (TYPE-004).
 * - **`placed`** is the list of pieces on the board *other than* the piece being
 *   dropped, turned or mirrored; it is never modified.
 *
 * Must never: change a piece's turn or mirror during a search (REQ-019 rule; REQ-018
 * rule: the search never tries the other mirror image); return a position that overlaps a
 * placed piece or sticks out of the silhouette (REQ-019.A2); report an expected outcome
 * by exception (G-15).
 */
interface ILockEngine {
    /**
     * The silhouette's outline corners for [puzzle], from its solution (REQ-019 anchors;
     * the corners REQ-051 pulses in a mini puzzle). A corner is a point of the
     * silhouette's boundary (outer edge or hole) where the boundary turns; a point where
     * solution pieces meet with a straight (180°) or full (360°) angle is not a corner.
     *
     * @param puzzle the puzzle.
     * @return each outline corner once, in puzzle units; order unspecified.
     */
    fun outlineCorners(puzzle: PuzzleDefinition): List<Point>

    /**
     * The corners of [piece] at [turn] and [mirrored], with its centre at [centre] — the
     * geometry every board and tray drawing uses (REQ-012, REQ-016, REQ-017, REQ-018;
     * TYPE-001 shapes, TYPE-003 turns about the centre). Mirroring has a visible effect
     * only on [PieceId.PG] (TYPE-003).
     *
     * @return the piece's corners in puzzle units, in a fixed order per piece.
     */
    fun vertices(piece: PieceId, turn: Turn, mirrored: Boolean, centre: Point): List<Point>

    /**
     * Where [piece], dropped (or held) with its centre at [dropCentre], locks — the drop of
     * REQ-019 and the landing preview of REQ-021, which use this same search so the two
     * never disagree (REQ-021.A1).
     *
     * Candidates are the translations t = a − v for every corner v of the piece (at
     * [turn], [mirrored], centred on [dropCentre]) and every anchor a, with |t| ≤ R; only
     * valid candidates count. Among them, score = |t| − 0.04 × (number of piece corners
     * that land exactly on anchors); only candidates with |t| ≤ (best |t|) + 0.16 are
     * considered and the lowest score wins (TYPE-004). The locked position is the anchor's
     * exact position minus the corner's exact offset, so later locks against it are exact.
     *
     * @param puzzle the open puzzle.
     * @param placed the other pieces on the board.
     * @param piece the piece being dropped; not in [placed].
     * @param turn the piece's turn as dropped; kept in the result.
     * @param mirrored the piece's mirror state as dropped; kept in the result.
     * @param dropCentre the piece's centre where it is released, in puzzle units.
     * @param dpPerUnit the board's current scale, dp per puzzle unit (for the 30 dp floor of R).
     * @return [LockResult.Locked] at the winning position (REQ-019.A1, A4), or
     *   [LockResult.None] when no valid candidate exists — the piece goes home (REQ-020;
     *   the "went home" signal of REQ-051) and no preview is drawn (REQ-021.A2).
     */
    fun findLock(
        puzzle: PuzzleDefinition,
        placed: List<Placement>,
        piece: PieceId,
        turn: Turn,
        mirrored: Boolean,
        dropCentre: Point,
        dpPerUnit: Double,
    ): LockResult

    /**
     * A tap turns a board piece 45° clockwise about its centre (REQ-016): the lock search
     * of [findLock] for [current]'s piece at [newTurn], its mirror state unchanged,
     * centred on [current]'s centre.
     *
     * @param current the piece's placement before the turn; not in [placed].
     * @param newTurn the turn to try (normally `current.turn.next()`).
     * @return [LockResult.Locked] with the new placement, or [LockResult.None] — the
     *   caller then keeps [current] unchanged (position and turn) and shakes the piece
     *   once (REQ-016 rule, A2).
     */
    fun tryTurnInPlace(
        puzzle: PuzzleDefinition,
        placed: List<Placement>,
        current: Placement,
        newTurn: Turn,
        dpPerUnit: Double,
    ): LockResult

    /**
     * The flip badge mirrors the parallelogram on the board (REQ-018): the lock search of
     * [findLock] for [current]'s piece with the opposite mirror state, its turn unchanged,
     * centred on [current]'s centre.
     *
     * @param current the piece's placement before the mirror; not in [placed].
     * @return [LockResult.Locked] with the mirrored placement, or [LockResult.None] — the
     *   caller then keeps [current] and mirrors back with a short shake (REQ-018 rule).
     */
    fun tryMirrorInPlace(
        puzzle: PuzzleDefinition,
        placed: List<Placement>,
        current: Placement,
        dpPerUnit: Double,
    ): LockResult

    /**
     * The saved board pieces that are still valid locks against [puzzle]'s current
     * silhouette, for loading a puzzle in progress (REQ-025 rule: a saved piece that is no
     * longer a valid lock — a puzzle changed in an update — returns to the tray; A3). Each
     * placement, taken in list order, is kept when it lies completely inside the
     * silhouette and overlaps no placement kept before it (TYPE-004 tolerance).
     *
     * @param saved the saved placements of one puzzle.
     * @return the kept placements, in their original order; the omitted pieces go to the
     *   tray. Equal to [saved] when nothing changed (REQ-025.A1).
     */
    fun stillValid(puzzle: PuzzleDefinition, saved: List<Placement>): List<Placement>

    /**
     * Whether [puzzle] is solved: every one of its pieces is locked on the board (REQ-022).
     * Because every lock is inside the silhouette without overlap and the pieces' area
     * equals the silhouette's, any exact cover counts — no comparison with the stored
     * solution is made (REQ-022 rule, A1).
     *
     * @param placed every piece on the board.
     * @return true exactly when the placed pieces are the puzzle's pieces ([PuzzleDefinition.pieces]).
     */
    fun isSolved(puzzle: PuzzleDefinition, placed: List<Placement>): Boolean
}
