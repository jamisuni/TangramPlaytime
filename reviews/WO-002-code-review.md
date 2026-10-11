# Code review — WO-002 · #Content

**Reviewer:** code-reviewer (fresh context)  ·  **Date:** 2026-10-11  ·  **Verdict:** forward (0 Blockers, 2 Shoulds, 5 Notes)

## Re-derived evidence

- `:content:testDebugUnitTest :contracts:test --rerun-tasks` (JAVA_HOME = Android Studio JBR): BUILD SUCCESSFUL, 22 tasks executed. Result XML: ContentScaffoldTest 2, PuzzleLibraryKitTest 16, PuzzleLoaderKitTest 36, ParserScaffoldTest 9, ContractsSanityTest 2 = 65 tests, 0 failures, 0 errors, 0 skipped. Matches run r2.
- `python tools/tests/test_tangram_geom.py`: ok, 13 files, warmup-4 gains exactly (2,2). `python tools/validate_puzzles.py Tangrams`: 13/13 valid, builds orders printed.
- `git status`: `contracts/`, `Tangrams/*.json`, `Tangrams/puzzle.schema.json` untouched. `.swdev/contract-baseline.json` changed only for the declared `build-map.md` re-freeze (v1.2).
- grep `content/src/main`: no `import android`, no `java.io.File`, no `File(`, no `app.tangramplaytime.kernel`. `:kernel` is `testImplementation` only (G-12).
- grep `content/src/test` tokens: exactly REQ-007.A1, A2, REQ-038.A1 (x17), REQ-040.A1, A2, REQ-041.A1, A2 (x3 incl. one assertion-level mention), REQ-042.A2, A3, REQ-045.A1. None on REQ-038.A2, REQ-039.*, REQ-042.A1, REQ-045.A2. `KitTexts.kt` has zero `REQ-0` strings. `ContentScaffoldTest` and `ParserScaffoldTest` carry "scaffolding" headers and no token.

## Design review findings F1-F4 in the code

- F1 (`rot/flip/at` is `PLACEMENT`, never `JSON`): `PlacementDto` declares the three keys; `polygonOf` reports `PLACEMENT` naming the form; kit test `aSolutionEntryInTheRotFlipAtFormIsAPlacementViolationNotJson` asserts it as the only rule.
- F2 (geometry in the same task, one `Rule` shape): all 15 `Rule` entries are raised; kit discriminates each geometric rule.
- F3 (release evidence): run a0 lists the release AAR and release APK. See N4 for what that does and does not prove.
- F4 (`:app` wiring is WO-003's): `PuzzleLibraries.bundled` is the only public symbol, not wired into `:app`.

## Findings

### S1 — CONTRACT/DOC · Should · `PuzzleDtos.kt:55-60` (`ShapeDto`), `PuzzleParser.kt:61-66`, `decisions.md` row 49

An unknown art shape `type` is rejected as `Rule.JSON` (sealed-DTO decode), while decisions row 49 lists "known shape types" under the well-formed-picture rule (and validator V10 says `unknown type`, an art error). Settled: not a defect against the design, which already says in §3 "Library facts" that an unknown type throws `SerializationException`, but a doc gap. Row 49 and design §2 (`JSON` means only: not JSON, wrong type, unknown key, missing field) disagree, the choice was made by the implementer, and no test pins it. The observable result is the same (build fails naming the file), only the label differs. Resolution: either amend row 49 to "unknown shape type reads as `JSON`" and add one untokened pin (an edited `things-arrow` with `type: 'star'` expects `Rule.JSON`), or catch it and report `ART`. The first is the cheaper and keeps the decode simple.

### S2 — TEST · Should · `PuzzleLibraryKitTest.kt:50-87` (REQ-007.A1)

The token claims "every shipped puzzle meets REQ-038, REQ-039 and REQ-040", and the WO scope note says the library-level "well-formed picture" leg of A1 is covered here. The test checks pieces, congruence, overlap, connectivity, rating and titles independently, but nothing about the picture. If `checkArt` were deleted from the parser, this test would stay green; only the REQ-038.A1 crafted-rejection tests would notice. Resolution: add to the loop a short independent picture leg (base and every fill/stroke match `#RRGGBB`, opacity in 0..1, width above 0, polygon of 3 or more points), read from `p.art`, with no change to the product. This is not gaming (the test asserts a lot), it is a missing leg of the line's meaning.

### N1 — TEST · Note · `PuzzleLoaderKitTest.kt` (`aSquareWhoseCornersAreNotInBoundaryOrderIsNotCongruent`, token REQ-038.A1)

The test asserts the boundary-order convention of decisions row 46, which the REQ text ("congruent pieces") never names; a bow-tie square has the right vertex set and Python V3 accepts it. The token is still honestly carried by `aPieceOfTheWrongSizeIsNotCongruent`. Resolution: drop the token from the bow-tie test and make it a decision pin, as is already done for the off-turn test.

### N2 — TEST · Note · `KitTexts.kt:~560-590` (`kitSharedLength`) vs `Edges.kt:23-45`

The kit's "independent" shared-length measure is a line-for-line twin of `Edges.sharedLength` (same collinearity test, same projection, same 1e-6). The kit's independence rests on hand-computed fixtures (`theKitMeasureReadsHandComputedValues`, the 0.25 exposure fixture and the exactly-0.5 shipped warm-ups), which are sound and fail on a real defect, so I accept it. If the measure is ever changed, change the kit's by derivation, not by copy.

### N3 — D3 · Note · `PuzzleDtos.kt:1-6`, `PuzzleLoader.kt:12-61` (`Rule`), `BundledPuzzles.kt:1-4`, `content/build.gradle.kts` (`BundlePuzzles`)

The headers of the DTOs, `Rule`, `BundledPuzzles` and `BundlePuzzles` cite the design section and Spec/03 but no REQ ID; `Edges`, `PuzzleLibrary`, `PuzzleLibraries` and the constants in `ValidityCheck` do. Every abstraction maps to an in-scope REQ through its seam-table row (REQ-007 / REQ-038), and there is no speculative generality, so this is not silent scope. Add "REQ-007, REQ-038" to those four headers when the files are next touched. No `Edges` / `Rule` / DTO exists without a requirement.

### N4 — QUALITY · Note · bundle reading path (`BundledPuzzles.kt:20-26`)

The design's highest-risk part is proven on the JVM classpath and by APK/AAR entry listings, never by a read on a device or emulator. `getResourceAsStream` over an APK zip is a standard Android path and I expect it to work, but the first runtime read happens in WO-003. Carry to the WO-003 brief: its first instrumented or on-screen check should call `PuzzleLibraries.bundled(LockEngine())` on the floor AVD (API 26) once and assert 13 puzzles, and the release build's R8 should be checked not to drop `app/tangramplaytime/content/puzzles/*`.

### N5 — TOOLS/DOC · Note · `tools/tests/test_tangram_geom.py:19-33`, `architecture.md:53,99`

(a) The tool test pins the old Python rule's output for 12 files plus the declared (2,2) addition; it does not compare with the kernel's `outlineCorners`. "Unchanged for the other 12" therefore rests on the design probe, not on a check against the oracle the fix claims to match. The rule itself is faithful to `Silhouette.kt:37-76` and design-inputs §2 item 13 (arcs sorted, one gap and a total of pi, tolerance 1e-6), `_angle_at` and its `acos` are gone with no other user, and `build_order` is untouched; the test can fail (it asserts set equality). (b) `architecture.md` §3 and ADR-004 still say "assets" and G-15 names `LoadResult.Unreadable`; decisions row 43 (Java resources) and the internal `LoadResult.Rejected` supersede them. The file is locked and was rightly not edited; log it for the next architecture amendment.

## Points to settle (brief)

1. Unknown shape type to `JSON`: doc gap (S1).
2. Kit geometry: shared length, exposure, SAT overlap (convex pieces only, edge/corner contact is no overlap), congruence by sides in boundary order plus area. Each traces to REQ-038 "congruent / no overlaps / connected through shared edges", the REQ-038 rule on corner joins, and REQ-041's measure. The only asserted convention the spec does not name is boundary order (N1); the 45 degree turn rule sits in an untokened pin. Mean-versus-every-piece is discriminated by the 0.25 fixture whose mean is 0.637.
3. Substance audit per token (all ten assert the line's meaning):
   - REQ-007.A1: assertion-rich, picture leg missing (S2). REQ-007.A2: asserts at least one mini first, all minis in the opening run, at least one warm-up after, all warm-ups contiguous.
   - REQ-038.A1 (x17): the shipped load plus 16 tests each asserting one rule rejected or accepted, with `kitAssertOnlyRule` requiring that rule alone on that file alone. Fixtures break a single rule; crafted geometry checked (corner-only join, 0.25 overlap, off-turn, wrong size). `Loaded` versus `Rejected` and not just "no exception".
   - REQ-040.A1: first listed rating is 1. REQ-040.A2: edited full rating-1 puzzle whose id sorts first is listed right after the last warm-up (relies on the shipped bundle holding no other rating-1 full, noted in the test).
   - REQ-041.A1: four after the minis are warm-ups of rating 1. REQ-041.A2: the below-half warm-up is rejected as `WARMUP` only; shipped warm-ups at exactly 0.5 are accepted; the library-level measure re-derived by the kit.
   - REQ-042.A2: at least four categories. REQ-042.A3: `byId` and `puzzles` both list `shapes-square`.
   - REQ-045.A1: first entry is a mini with three pieces.
   No token on an empty or proxy assertion. Pins and scaffolding are untokened.
4. D3: N3.
5. G-12/G-15: main depends on `:contracts` only; expected outcomes are sealed (`Parsed`, `LoadResult`); exceptions only for the designed fail-fast (`bundled`, missing resource), a programming/build defect.
6. Gradle: `BundlePuzzles` declares `@InputFiles` (NAME_ONLY) and `@OutputDirectory`, clears the output first (no stale puzzle), writes the one copy plus `index.txt` under the same folder as `BundledPuzzles.DIR` (the literal is duplicated in kts and Kotlin, both commented and the scaffold test would catch a mismatch), excludes the schema, README and previews. No runtime path reaches `Tangrams/`.
7. Tools diff: see N5.

## Trajectory

Every station ran its check: design review F1-F4 landed in code; plan review recirculation resolved; kit authored before the implementation and the red window closed at TASK-005 (run r2); tool test and validator run py1; run log hashes match the tree I reviewed. The implementer flagged the unknown-type label honestly, which is S1.

## Verdict

forward. No Blocker. S1 and S2 are cheap and can ride with WO-003's first touch of `:content` or a Test & Verify fix-up.

## Handoff — Code Reviewer · WO-002

- **Scope:** `:content` main (bundle, parser, loader, validity, library), acceptance kit, `tools/tangram_geom.py` corner rule and its test, `build-map.md` 1.2
- **Inputs read:** WO-002, tasks, design + design review, REQ-007/038/040/041/042/045, schema, decisions rows 43-52, directives, architecture, run log, all `:content` main and test sources, the tools diff and test, `Silhouette.kt` header
- **Result:** `reviews/WO-002-code-review.md`
- **Status:** forward (0 B / 2 S / 5 N). Tests: 65 green (2 + 16 + 36 + 9 + 2), tool test and validator 13/13 green
- **Traceability delta:** none
- **Notes:** S1 unknown shape type labelled `JSON` against decisions row 49; S2 REQ-007.A1 has no picture leg; carry N4 (first device read of the bundle) to WO-003.
