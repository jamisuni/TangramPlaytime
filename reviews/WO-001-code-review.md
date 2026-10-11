# Code Review — WO-001 · #Locking (TYPE-004 lock rule in `:kernel` behind `ILockEngine`)

**Reviewer:** code-reviewer (fresh context) · **Date:** 2026-10-11
**Inputs:** product diff (`PieceShapes.kt`, `Polygons.kt`, `Silhouette.kt`, `LockRules.kt`, `LockEngine.kt`; `Kernel.kt` deleted) · REQ-019/020/022 · TYPE-001/003/004 · `ILockEngine.kt`, `Types.kt` · `designs/WO-001-design.md` · `reviews/WO-001-design-review.md` · `architecture.md` G-04/G-12/G-15 · `decisions.md` WO-001 rows · kit (`LockEngineKitTest`, `GeometryKitTest`, `KitFixtures`) read for assertion substance.

## Verdict

**forward** — 0 Blockers, 0 Shoulds, 3 Notes.

## Evidence (re-derived)

- `gradlew :kernel:test :contracts:test --rerun-tasks`: BUILD SUCCESSFUL. XML under `kernel/build/test-results/test/`: GeometryKitTest 20, KernelScaffoldTest 1, LockEngineKitTest 14, GeometryScaffoldingTest 10, LockEngineScaffoldingTest 7, ContractsSanityTest 2; 0 failures, 0 errors, 0 skipped. Matches run r2.

## Checklist

- [x] **(a) TYPE-004 literally** — `LockEngine.findLock`: R = `max(0.65, 30/dp)`; candidates = every piece corner x every anchor with d <= R+1e-9; centre = `a - o` (never `dropCentre + t`); stable sort by d; window tested on `c.d` against the first *valid* d, not on score; score = d - 0.04 x snug (snug at 1e-6 per coordinate); replace only when lower by > 1e-9; input turn/mirror returned. Matches design §1 steps 1-6 and the F4 tie rule (recorded in decisions.md).
- [x] **(b) REQ-019 must-never** — validity = `Silhouette.contains && none overlaps` for every candidate before it can win; `anchors` = `outlineCorners` + placed `vertices` only (LockEngine.kt tail); no midpoints, no non-corner solution vertices. Kit A3 poses (design-review F2 fixes) sit on 360-degree vertices and an edge midpoint and return `None`.
- [x] **(c) Contiguity rule** — `Silhouette.isCorner`: sum >= 2pi-1e-6 is interior; exactly one gap and |sum-pi| <= 1e-6 is a straight side; everything else is a corner. Traced by hand: lone 90-degree arc (gap 1, sum != pi) corner; half-plane from a vertex strictly inside an edge (`strictlyInsideEdge`, arc = side holding the piece centre) straight; warmup-4 (2,2) arcs 0-45/90-135/270-360 sum pi, three gaps, corner; touching arcs give gap ~0 or ~2pi after wrap and are not counted. `GeometryScaffoldingTest.twoSeparateWedges...` pins the (2,2) case.
- [x] **(d) `contains`** — sum of convex clips, both polygons normalised to positive area in `clipConvex`; straddling two solution pieces handled by the sum; shared-edge slivers ~1e-16 << 1e-6. See F1 for the tolerance's meaning.
- [x] **(e) `PieceShapes`** — Spec §3 corners and TYPE-001 areas correct (LT 4, MT 2, ST 1, SQ 2, PG 2); offsets from the corner average; exact 0/+-1 cos/sin table, `+ 0.0` removes -0.0; clockwise on screen (y down); flip applied before the turn and for PG only (equals Spec `R·F·local` since the centre is taken after the transform); `mirrored` kept on the `Placement`.
- [x] **(f) G-15** — a miss is `LockResult.None` on every path; `IllegalArgumentException` only for bad `dpPerUnit` and dropped piece in `placed` (both recorded in decisions.md).
- [x] **(g) G-12** — `kernel/build.gradle.kts` is plain `kotlin.jvm`; main sources import only `app.tangramplaytime.contracts.*` and `kotlin.math.*`; no new dependency.
- [x] **(h) D3 / scope** — only the seven `ILockEngine` members plus `anchors`, `PieceShapes`, `Polygons`, `Silhouette`, `LockRules`, two private holder classes; each names its REQ/TYPE in a header or KDoc; no silent product decision beyond the four recorded rows.
- [x] **(i) `isSolved`** — `placed.size == pieces.size && placed.map{piece}.toSet() == puzzle.pieces`; no geometry, no stored-solution comparison; a duplicated piece id fails the set test.
- [x] **(j) wrappers** — `tryTurnInPlace`/`tryMirrorInPlace` delegate to `findLock` from `current.centre` with the new turn / opposite mirror; `stillValid` keeps in list order each placement inside and not overlapping those kept before, returns the `saved` instances; none is a stub, each has a scaffolding test.
- [x] **Kit tokens** — every `REQ-NNN.A<n>` token sits on a test asserting that line's meaning (A1: exact lock at hand-computed centres; A2: traps (a)-(d) plus sampled-point check; A3: four None poses incl. both hidden-anchor bugs; A4: five arrangements x seven holes; REQ-020.A1: Locked-or-None sweep plus off-board; REQ-022.A1: lock-by-lock cover, `isSolved` false before / true after). No gaming found; REQ-020.A2 untokened as recorded.
- [x] **Implementer notes** — `stillValid` checking only inside + no-overlap matches its KDoc; snug test <= 1e-6 vs the prototype's strict < is the design's "compare at 1e-6, never ==" and matches TYPE-004's "exactly". Both accepted.

## Findings

### F1 — QUALITY · Note · `Polygons.overlaps` / `Silhouette.contains`
- **Observation:** The 1e-6 tolerance is applied to area (intersection area > 1e-6; covered >= area - 1e-6), whereas TYPE-004 words it as a tolerance "of at most 1e-6 units". For a thin sliver the length is tiny (2.5e-7 on a 4-long edge), but a corner poke can be up to ~1.4e-3 units deep before it exceeds 1e-6 area. All real candidates are aligned to the √2 lattice (gaps >= ~1e-3 per design §3), so this cannot trigger today, and it is the prototype's method.
- **Proposed resolution:** None required. Revisit if a later WO adds non-lattice geometry.

### F2 — QUALITY · Note · `LockEngine.anchors` / `Silhouette.outlineCorners`
- **Observation:** `outlineCorners` is recomputed on every `findLock` call, and REQ-021 calls `findLock` each drag frame. About 20 distinct vertices x 7 polygons, a few hundred small allocations: well under a millisecond, consistent with the design's "no cache" cost note.
- **Proposed resolution:** Leave; cache per `PuzzleDefinition` only if a device profile ever shows it.

### F3 — CONTRACT · Note · `stillValid`
- **Observation:** Per its KDoc it does not require a corner on an anchor and does not reject two saved placements of the same `PieceId` that do not overlap. Both are outside the KDoc and this WO's acceptance; REQ-025's WO owns the corrupt-save question.
- **Proposed resolution:** The REQ-025 WO decides whether a duplicate piece id in a save is a case to drop.

## Handoff — Code Reviewer · WO-001

- **Scope:** product code of WO-001 TASK-001/002 (`:kernel`: PieceShapes, Polygons, Silhouette, LockRules, LockEngine) against REQ-019/020/022, TYPE-001/003/004, `ILockEngine`.
- **Inputs read:** the files listed at the top; kit read for assertion substance only.
- **Result:** `C:/GitHub/AI/TangramPlaytime/reviews/WO-001-code-review.md`
- **Status:** forward
- **Traceability delta:** none
- **Notes for next station:** 0 Blockers, 0 Shoulds, 3 Notes; 54 tests re-run green (14 + 20 + 1 + 10 + 7 + 2). No product decision found outside decisions.md.
