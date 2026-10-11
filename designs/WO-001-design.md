# Design — WO-001 · #Locking: the one lock rule in `:kernel` behind `ILockEngine`

**Author:** Design Author (design-author · Opus)  ·  **Date:** 2026-10-11  ·  **Status:** Draft
**Full because:** (b) governed interface — `ILockEngine` implemented for the first time

## 1. Scope

- **Implements:** REQ-019 (A1–A4), REQ-020 (A1–A2, seam half), REQ-022 (A1); TYPE-001/003/004 once in `:kernel` (G-04, O-01).
- **Governed interfaces:** `ILockEngine` (implements, all 7 members); `Types.kt` (consumes). **Proposed contract delta:** none.
- **Hard-stop domains touched:** none.
- **How it works.** `LockEngine` is stateless and uses only stdlib plus `:contracts`. It ports the prototype's `verts`/`clip`/`anchors`/`validAt`/`findLock` (ADR-001), following the TYPE-004 text where the prototype differs. **`findLock`:**
  1. Set R = `LockRules.lockRadius(dpPerUnit)`. Build `A = anchors(puzzle, placed)` and `offs = PieceShapes.offsets(piece.type, turn, mirrored)`. Compute the placed pieces' polygons once.
  2. Make the candidates. For each corner i and anchor j, set t = `A[j] − (dropCentre + offs[i])`, d = `hypot(t)`. Keep the pair when d ≤ R + 1e-9, with centre = `A[j] − offs[i]`. This is the anchor's exact position minus the corner's exact offset; it is never `dropCentre + t`.
  3. Stable-sort the candidates by d. Equal d keeps generation order (i, then j).
  4. Walk the list. V = `vertices(piece, turn, mirrored, centre)` is **valid** when `Silhouette.contains(V, puzzle.solution)` holds and no placed polygon `overlaps` V. The first valid candidate sets dMin. Stop at the first d > dMin + 0.16 + 1e-9.
  5. Score each valid candidate as d − 0.04 × snug, where snug is the number of V's corners within 1e-6 (per coordinate) of some anchor. A candidate replaces the best only if its score is lower by more than 1e-9.
  6. Return `Locked(Placement(piece, turn, mirrored, best.centre))`, or `None` when no candidate was valid.

  The other members are thin wrappers that later WOs test through the seam:
  - `tryTurnInPlace` calls `findLock` with `newTurn`, `current.mirrored` and `current.centre`.
  - `tryMirrorInPlace` calls `findLock` with `current.turn`, `!current.mirrored` and `current.centre`.
  - `stillValid` applies the step-4 predicate in list order, against the placements it has kept so far.

## 2. Seam table

| Seam | Kind | Signature & value shapes | Required by | Delivered by task |
|---|---|---|---|---|
| `LockEngine` | class (`I*` impl) | `class LockEngine : ILockEngine`, package `app.tangramplaytime.kernel`, no-argument constructor, no state. Tests call `LockEngine()`. Replaces `Kernel.kt`. | G-04 | TASK-002 |
| `findLock` | `I*` | The frozen signature.<br>**Returns:** `Locked(Placement(piece, turn, mirrored, centre))`, keeping the *input* turn and mirror, or `None`.<br>**Never throws** for finite input; a drop off the board returns `None`.<br>**`IllegalArgumentException`** (G-15) when `dpPerUnit` is not finite or ≤ 0, or when `piece` is in `placed`.<br>`dpPerUnit` = 100.0 gives R = 0.65; 20.0 gives R = 1.5 (the 30 dp floor). | REQ-019.A1–A4, REQ-020.A1–A2, TYPE-004 | TASK-002 |
| `vertices` | `I*` | Returns `centre + PieceShapes.offsets(...)`. Corner i is Spec §3 corner i, transformed; winding is not promised. The centre is the average of the corners, the same as `Placement.centre`. | TYPE-001, TYPE-003 | TASK-002 |
| `outlineCorners` | `I*` | Returns `Silhouette.outlineCorners(puzzle.solution)`. Gift box gives the 4 square corners; mini-1 gives (2,0) (0,2) (4,2); `shapes-warmup-4` includes (2,2). | REQ-019 anchors | TASK-002 |
| `isSolved` | `I*` | `placed.size == puzzle.pieces.size && placed.map { it.piece }.toSet() == puzzle.pieces`. No geometry, no comparison with the solution. | REQ-022.A1 | TASK-002 |
| `tryTurnInPlace`, `tryMirrorInPlace`, `stillValid` | `I*` | Wrappers (§1); acceptance comes with REQ-016/018/025. | completeness | TASK-002 |
| `PieceShapes` | i* | `internal object PieceShapes { fun offsets(type: PieceType, turn: Turn, mirrored: Boolean): List<Point> }`, computed once at init:<br>1. Take the Spec §3 corners minus their average.<br>2. Mirror x → −x **for PG only**.<br>3. Rotate: x' = x·c − y·s, y' = x·s + y·c, with `c = [1,h,0,−h,−1,−h,0,h][step]`, `s = [0,h,1,h,0,−h,−1,−h][step]`, h = `sqrt(2.0)/2`. Exact 0 and ±1, never `cos(π/2)`. | TYPE-001, TYPE-003, TYPE-004 "exact offset" | TASK-001 |
| `Polygons` | i* | `internal object Polygons`:<br>`signedArea(p)` is the shoelace Σ(xᵢyᵢ₊₁ − xᵢ₊₁yᵢ)/2; `area(p)` is its absolute value.<br>`clipConvex(subject, clipper)` is Sutherland–Hodgman. It first normalises both polygons to positive signed area; P is inside edge AB when cross(B−A, P−A) ≥ −1e-9.<br>`intersectionArea(a, b)` is the area of the clip, or 0 when the clip has fewer than 3 points.<br>`overlaps(a, b)` is `intersectionArea > 1e-6`. | REQ-019.A2, TYPE-004 tolerance | TASK-001 |
| `Silhouette` | i* | `internal object Silhouette`:<br>`contains(poly, solution: List<SolvedPiece>)` is Σ `intersectionArea(poly, s.polygon)` ≥ `area(poly)` − 1e-6.<br>`outlineCorners(solution): List<Point>` ports `tangram_geom.outline_corners`, but classifies each point by the **contiguity of the covered directions**, not the angle sum (Contract: "where the boundary turns"):<br>1. Take the distinct vertices p (merged within 1e-6), in first-seen order.<br>2. Each polygon q with p as a vertex (within 1e-6), or strictly inside an edge (abs(cross) ≤ 1e-9), covers one arc of directions from p: the arc between q's two edge directions at p that holds the direction to q's centre.<br>3. Σ is the total arc length. Sort the arcs by start; each gap is (next.start − cur.end) mod 2π, counted when it lies in (1e-6, 2π − 1e-6).<br>4. p is **not** a corner when Σ ≥ 2π − 1e-6 (interior), or when there is exactly one gap and abs(Σ − π) ≤ 1e-6 (straight side). Every other p is a corner. | REQ-019 rule, A2, A3 | TASK-001 |
| `LockRules` | i* | `internal object LockRules`. Constants: `LOCK_DISTANCE = 0.65`, `FLOOR_DP = 30.0`, `SNUG_BONUS = 0.04`, `WINDOW = 0.16`, `EPS = 1e-6`, `CMP = 1e-9`. Function: `lockRadius(dpPerUnit) = max(0.65, 30.0 / dpPerUnit)`. | TYPE-004 | TASK-001 |
| `anchors` | i* | `internal fun LockEngine.anchors(puzzle, placed): List<Point>` returns the outline corners, then each placed piece's `vertices`, in list order. | REQ-019 rule, A3 | TASK-002 |
| Kit fixtures | test-only | Puzzles are **transcribed** into the kit, which reads no file (O-02: only `:content` reads `Tangrams/*.json`).<br>`puzzleOf(id, kind, vararg SolvedPiece)` uses `titles = mapOf("en" to id)` and `ArtLayer("#000000", emptyList())`.<br>The puzzles are `GIFT_BOX` (`shapes-square.json`), `CHOCOLATE_BAR` (`shapes-rectangle.json`, where `[a,b]` becomes `a + b*sqrt(2.0)`, all 45° turns) and `MINI_PYRAMID` (`shapes-mini-1.json`).<br>`poseOf(engine, sp): Placement` takes the polygon's average as the centre and returns the **lowest** turn 0..7 (with `mirrored = true` tried for PG only) whose `vertices` match the polygon as a point set within 1e-6. Matches are not unique: an SQ matches four turns and a PG two. It fails loudly when none matches.<br>`assertNear(p, q, 1e-9)` compares points; never compare a `Point` with `==`. | REQ-019.A4, REQ-022.A1 | TASK-003 |

## 3. Risks & edge notes

- **Highest-risk part: the validity predicate.**
  - The silhouette is non-convex and may have holes, and Sutherland–Hodgman is correct only for a *convex* clipper. So "inside" means the **sum of clips against each solution piece**, never a clip against the outline. This works because the solution pieces are convex and interior-disjoint (V4).
  - Both polygons must have the **same winding**: file polygons and a mirrored PG come either way, and mixed winding gives an empty clip, so nothing would ever lock.
  - Pieces that only share an edge clip to about 1e-16, below the overlap tolerance.
- **Exact-anchor arithmetic.** Taking centre = `a − offs[i]` from the one table reproduces a to ≤ 1e-12, but not bit-exactly (`(a − o) + o` may differ from a by one ulp). Snug and coincidence checks therefore compare at 1e-6, never with `==`. Do not port the prototype's `cx + tx`.
- **The TYPE-004 window does not change the winner.** The smallest-|t| valid candidate has at least 1 snug corner, so its score is ≤ dMin − 0.04. Any candidate with d > dMin + 0.16 scores ≥ d − 0.16 > dMin, since a piece has at most 4 corners. The window, and the prototype's score-based variant, only prune the search. No test should try to tell them apart; `design-inputs.md` §2 item 3 has no behavioural effect.
- **MT mirror trap.** The prototype mirrors x for every piece type. LT, ST and SQ are symmetric about the vertical axis at rot 0, but MT is not: mirroring it would give a 90°-turned pose, which is a visible effect TYPE-003 forbids. Only PG is mirrored, and `Placement` keeps the flag unchanged (REQ-019 rule).
- **Turn and mirror stay as dropped.** The search tries one (turn, mirror) pair, with no variant loop (REQ-019 rule; REQ-018; TYPE-004). A drop one 45° step off its hole never locks into it.
- **A3:** an ST at turn 0 centred at (2, 1/3) on the Gift box, with R = 0.65, lies exactly along the top edge but more than R from every corner. It returns `None`: there are no hidden anchors.
- **A4:** with six pieces placed, the hole is the only valid pose, and its convex corners are anchors (REQ-019 rationale).
- **REQ-020, seam half:** `LockResult` is sealed, so every drop gets exactly one of the two values. A miss is the text-free data object `None`, not an exception.
- **REQ-022:** `isSolved` is a set check, which is enough because every `Placement` is already a lock.
- **Oracle honesty.**
  - **A1:** the fixtures must make the intended pose the *unique* best. A snugger valid candidate up to 0.12 away wins legitimately, and anchors 1.5 and √2 are only 0.086 apart.
  - **A2:** a sweep may use `Polygons`/`Silhouette` as its oracle only if the same kit pins them with hand-computed areas (for example, unit squares offset by (0.5, 0.5) overlap by 0.25). Otherwise it needs an independent sampled-point check.
- **Corner rule: contiguity, not angle sum.** In `shapes-warmup-4`, LT2, MT and ST2 cover 0–45°, 270–360° and 90–135° around (2,2). That sums to 180°, but in two separate wedges, so the boundary turns there and the point is an outline corner (REQ-019; `outlineCorners` KDoc; V5 does not exclude it). A sum-only rule drops this anchor, so a valid drop anchored only there would return `None`. The contiguity rule matches the reference on all 13 shipped puzzles except that one point, which it adds. `tools/tangram_geom.outline_corners` (DI-5) keeps the blind spot, which is WO-002's content-tool matter. The kit must **not** use it as the oracle for warmup-4.
- **Library facts relied on:**
  - Kotlin `Double` is IEEE-754 and `sqrt` is correctly rounded, so the a + b√2 values from the files carry about 1e-15 error.
  - Genuine gaps on the ℚ(√2) lattice are at least about 1e-3 for real-puzzle coefficients (99 − 70√2 ≈ 0.005), so 1e-6 cleanly separates touching from overlapping.
  - Sutherland–Hodgman needs a convex clipper with consistent winding.
  - `sortedBy` is stable.
  - `atan2` returns values in (−π, π]; normalise each direction to [0, 2π) before comparing arcs.
- **Cost:** the REQ-021 preview calls `findLock` every drag frame. That is at most about 180 candidates with at most 13 small clips each: under 1 ms, so no cache (D2/D6).

## 4. Suggested cut

| Task | [P] | Delivers seams | Covers | Depends on |
|---|---|---|---|---|
| TASK-001 | | `PieceShapes`, `Polygons`, `Silhouette`, `LockRules` (geometry and validity; hardest first) | TYPE-001/003/004; REQ-019.A2/A3 groundwork | — |
| TASK-002 | | `LockEngine` (all 7 members), `anchors`, the §1 search. Deletes the `Kernel.kt` placeholder; keeps `KernelScaffoldTest`. | REQ-019.A1–A4, REQ-020.A1–A2, REQ-022.A1 | TASK-001 |
| TASK-003 | [P] | Acceptance kit (Acceptance Test Author): fixtures and tests in `kernel/src/test/kotlin/app/tangramplaytime/kernel/`. **Inputs:** none. | every in-scope ID | the seam table; compiles after TASK-002 |

*Alternatives considered:*
- A union polygon with non-convex clipping: more code and fragile; the sum of convex clips is the prototype's method (ADR-001).
- SAT overlap depth: it would add a second overlap rule with its own tolerance.
- Runtime ℚ(√2) arithmetic: ruled out by ADR-001.

## Handoff — Design Author · WO-001

- **Scope:** REQ-019 (A1–A4), REQ-020 (A1–A2), REQ-022 (A1); TYPE-001/003/004 · governed touched: `ILockEngine` (implements), `Types.kt` (consumes)
- **Inputs read:**
  - WO-001 (2026-10-11)
  - REQ-019/020/022 (locked, collection v1.2); `req_types.md` v0.2
  - `architecture.md` v1.0; `ILockEngine.kt` and `Types.kt` (baseline frozen 2026-10-11); `design-inputs.md` v1.0 §2; `Spec/03` draft 0.4; `Study/06` (frozen)
  - `tools/tangram_geom.py`; `prototype_template.html` L204–234 and L501–535; `kernel/*`; four `Tangrams/*.json` files and the schema (all at bddffef)
  - SWDev `design-doc.md`, `directives.md` v0.3, `handoff-contract.md` v0.3
  - Round 2: `reviews/WO-001-design-review.md`; `Tangrams/shapes-warmup-4.json`
- **Result:** `designs/WO-001-design.md`
- **Status:** forward
- **Traceability delta:** REQ-019.A1–A4, REQ-020.A1–A2 and REQ-022.A1 now map to `findLock`/`isSolved` and TASK-001…003 (design level only).
- **Notes for next station:** The highest risk is the validity predicate, then the contiguity corner rule; `tangram_geom` is not the oracle for warmup-4 (2,2). The window does not change the outcome. F2, F3(b) and F4 are carried by the planner.
