# Design — WO-002 · #Content: the puzzle library in `:content` behind `IPuzzleLibrary`

**Author:** Design Author (design-author · Opus)  ·  **Date:** 2026-10-11  ·  **Status:** Reviewed (design review 2026-10-11 14:28: 0 B / 4 S — `reviews/WO-002-design-review.md`; F1–F4 carried to the planner) · planned 2026-10-11 (`tasks.md` § WO-002; F1, F2, F4 and the F3 evidence written back by the planner in §1 step 2, §2 `PuzzleParser.parse` / `Rule` / `ValidityCheck` / `Edges`, §3 and §4)
**Full because:** (b) governed interface — `IPuzzleLibrary` + `tangram-puzzle/1` (locked tier), first implementation

## 1. Scope

- **Implements:** REQ-007 (A1–A2), REQ-038 (A1), REQ-040 (A1–A2), REQ-041 (A1–A2), REQ-042 (A2–A3), REQ-045 (A1), at the seam as WO-002 scopes them. Also the `tools/tangram_geom.outline_corners` fix (design-inputs §2 item 13).
- **Governed interfaces:** `IPuzzleLibrary` + format (implements, unchanged) · `Types.kt` (consumes) · `ILockEngine` (consumes `vertices` and `stillValid`, unchanged). No `I*` delta. One notify-tier delta: in `build-map.md`, the #Content cross-feature deps change from `—` to `(kernel) (via ILockEngine)`.
- **Hard-stop domains touched:** none.
- **How it works:**
  1. **Build.** A Gradle task bundles `Tangrams/*.json` plus an `index.txt` as **Java resources**.
  2. **Load.** `:app` calls `PuzzleLibraries.bundled(LockEngine())` once — wired in WO-003, not in this WO (review F4). It reads the bundle through one `ClassLoader` path.
  3. **Check.** Each file is parsed and checked against the structural rules, then the geometric rules: congruence and overlap through `ILockEngine`, edge-sharing in `:content`.
  4. **Order.** The library is sorted kind → rating → id.
  5. **Failure.** Any violation is a build defect, so `bundled` throws.

## 2. Seam table

| Seam / abstraction | Kind | Signature & value shapes | Required by (REQ, one line) | Delivered by task |
|---|---|---|---|---|
| `PuzzleLibraries.bundled` | public fn | `object PuzzleLibraries { fun bundled(engine: ILockEngine): IPuzzleLibrary }`. On any violation it throws `IllegalStateException` listing them all (a build defect, G-15). | REQ-007; KDoc "must never list a puzzle that fails" | TASK-005 |
| `PuzzleLibrary` | `I*` impl | `internal class PuzzleLibrary(defs: List<PuzzleDefinition>) : IPuzzleLibrary`.<br>**`puzzles`:** `sortedWith(compareBy({ it.kind }, { it.rating }, { it.id }))` in an unmodifiable list. The enum order is MINI, WARMUP, FULL.<br>**`byId`:** a map lookup that returns null when the id is absent. | REQ-040, REQ-007.A2 | TASK-005 |
| `PuzzleLoader.load` | i* | `internal object PuzzleLoader { fun load(sources: List<PuzzleSource>, engine: ILockEngine): LoadResult }`.<br>**Per file:** parse, then `ValidityCheck` if the parse is clean.<br>**Library rules:** `ID` (ids unique) and `EMPTY` (at least one puzzle).<br>**Result:** `LoadResult` = `Loaded(library: IPuzzleLibrary)` or `Rejected(violations: List<Violation>)`, with every violation of every file. This is the kit's door for crafted texts. | REQ-038.A1, REQ-041.A2 | TASK-005 (geometry included, review F2) |
| `Violation`, `Rule` | i* values | `internal data class Violation(val file: String, val rule: Rule, val detail: String)`.<br>`internal enum class Rule { JSON, FORMAT, ID, TITLE, CATEGORY, RATING, KIND, PIECE_SET, PLACEMENT, ART, CONGRUENT, OVERLAP, CONNECTED, WARMUP, EMPTY }`. All entries exist, and are raised, from TASK-005 on (review F2: the geometry is in the same task, so the loader has one shape). | REQ-038.A1 (the kit asserts which rule rejected) | TASK-005 |
| `BundledPuzzles.read` | i* | `internal data class PuzzleSource(val fileName: String, val text: String)`.<br>`internal object BundledPuzzles { const val DIR = "app/tangramplaytime/content/puzzles"; fun read(): List<PuzzleSource> }`:<br>1. Read `"$DIR/index.txt"` through `BundledPuzzles::class.java.classLoader.getResourceAsStream`.<br>2. Read each named file as UTF-8.<br>A missing resource throws `IllegalStateException`. This is the **one** path for the app and the JVM test. | REQ-007 rule (own folder), G-05, O-02 | TASK-004 |
| `bundlePuzzles` | Gradle task | Lives in `content/build.gradle.kts`:<br>- `@InputFiles`: the top-level `*.json` of `../Tangrams`, excluding `*.schema.json`.<br>- `@OutputDirectory`: `build/generated/puzzles`.<br>It writes the copies plus `index.txt` (sorted names) under `DIR`.<br>Wired by `androidComponents.onVariants { it.sources.resources?.addGeneratedSourceDirectory(bundlePuzzles, BundlePuzzles::outputDir) }`.<br>**Inputs:** `Tangrams/*.json` (13 files). | REQ-007 rule, ADR-004 | TASK-004 |
| `PuzzleParser.parse` | i* | `internal object PuzzleParser { fun parse(src: PuzzleSource): Parsed }`, where `Parsed` = `Ok(def)` or `Bad(violations)`.<br>**Decoding:** `internal @Serializable` DTOs with the default `Json`. Any `IllegalArgumentException` → `JSON`.<br>**Attribution (planner):** every rule-checked field keeps its raw JSON type in the DTO (`kind`, `category` and `piece` as strings, `title` as a string map, `difficulty` as an integer). So a value of the right JSON type that breaks a rule raises that rule, for example `kind: "expert"` → `KIND` or `piece: "XX"` → `PIECE_SET`. `JSON` means only: not JSON, a wrong JSON type, an unknown key, or a missing required field.<br>**Coordinates:** each `solution` coordinate is a `JsonElement`. A number stays as it is; `[a, b]` becomes `a + b*sqrt(2.0)`; anything else → `PLACEMENT`.<br>**Rules:**<br>- `FORMAT`: the tag.<br>- `ID`: kebab-case, equal to the file name.<br>- `TITLE`: `en` and `fi`.<br>- `CATEGORY`: one of 8.<br>- `RATING`: 1..5 (a mini: 1).<br>- `KIND`: default full.<br>- `PIECE_SET`: each id once; 7, or 1–6 for a mini.<br>- `PLACEMENT`: `polygon`, 3–4 points. The DTO declares `rot`, `flip` and `at` as optional fields. An entry that carries any of them, or has no `polygon`, is a `PLACEMENT` violation whose detail names the unsupported form — never `JSON` (review F1, `decisions.md` line 47; §3 "Format narrowing").<br>- `ART`: §3.<br>**Mapping:**<br>- `rating` = `difficulty`.<br>- `reviewedByHuman` = `provenance?.reviewedByHuman ?: false`. | REQ-038/040/045 rules; REQ-007.A1 (meets REQ-039) | TASK-005 |
| `ValidityCheck` | i* | `internal class ValidityCheck(engine: ILockEngine) { fun violations(file: String, def: PuzzleDefinition): List<Violation> }`:<br>1. **`CONGRUENT`:** c is the vertex average. The pose is the lowest turn k with `engine.vertices(piece, Turn(k), m, c)` equal to the polygon, up to cyclic shift and reversal, within 1e-6. m is `false`; PG also tries `true`.<br>2. **`OVERLAP`:** `stillValid(def, poses).size == poses.size`.<br>3. **`CONNECTED`:** pieces are joined by `sharedLength > 1e-6`, and every piece must be reachable.<br>4. **`WARMUP`:** each piece has `1 − Σshared/perimeter ≥ 0.5 − 1e-6`. | REQ-038 (TYPE-001 congruence, no overlap, edge-connected), REQ-041.A2 | TASK-005 |
| `Edges` | i* | `internal object Edges { fun sharedLength(a: List<Point>, b: List<Point>): Double; fun perimeter(p: List<Point>): Double }`.<br>Shared length sums the projection overlap of collinear edge pairs (≤ 1e-6 off the line). A corner-only touch gives 0. | REQ-038 rule (no corner joins), REQ-041 measure | TASK-005 |
| `:content` test → `:kernel` | build | `testImplementation(project(":kernel"))`, so tests can use `LockEngine()` (WO-001, public, no arguments). Main code depends on `:contracts` only (G-12). | REQ-038 (the real engine on the JVM) | TASK-005 |
| `outline_corners` (tools) | Python fn | The signature is unchanged.<br>**New rule (WO-001 `Silhouette`):** classify a point by whether the covered directions around it are contiguous. A point is not a corner when the cover totals 2π, or when it totals π with exactly one gap.<br>Points stay exact `Q2`.<br>**Test:** `tools/tests/test_tangram_geom.py`, plain asserts, run directly. | design-inputs §2 item 13 (REQ-019 corners; V11 line) | TASK-007 |
| Acceptance kit | test | Lives in `content/src/test/kotlin/app/tangramplaytime/content/`.<br>- Calls `bundled(LockEngine())`, plus `load(...)` on edited `BundledPuzzles.read()` texts.<br>- **Inputs:** the 13 puzzles via the classpath.<br>- Optionally, the `../Tangrams` names, to check the bundle is complete. | every in-scope ID | TASK-008 |

## 3. Risks & edge notes

- **Highest-risk part: the reading path (TASK-004).** One `ClassLoader` call serves the JVM test and the device only if the generated Java-resources directory reaches both `testDebugUnitTest`'s classpath and the APK.
  - **AGP 9.4 facts relied on:**
    - `Variant.sources.resources` holds Java resources, not `res` and not `assets`.
    - A library's Java resources go into the AAR `classes.jar` and from there into the APK root.
    - Library unit tests run with the tested variant's processed Java resources on the classpath.
  - **Proof:**
    - `:content:testDebugUnitTest` reads 13 sources.
    - `unzip -l app/build/outputs/apk/debug/app-debug.apk` lists `index.txt` and the 13 files (`:app` already depends on `:content`).
    - Release evidence (review F3, added at planning): the `classes.jar` inside `content/build/outputs/aar/content-release.aar` (`:content:bundleReleaseAar`) and `app/build/outputs/apk/release/app-release-unsigned.apk` (`:app:assembleRelease`) list the same 14 entries.
  - **Fallback:** add the same directory to `variant.unitTest?.sources?.resources`. The reader stays as it is; main code never reads `File("../Tangrams")`.
  - **On device:** `BaseDexClassLoader` reads resources from the APK zip. The first call opens the APK, a one-time cost of milliseconds, so build the library once (WO-003).
- **Second risk: `stillValid` as the overlap oracle.** By its KDoc, `stillValid` keeps a placement when the piece is inside the silhouette and overlaps none kept before it. A solution piece is always inside its own silhouette, because the kernel's sum of clips includes the piece itself. So a dropped piece means an overlap. Call it only when the piece set is clean.
- **Congruence is stricter than Python V3,** which compares vertex sets. SQ and PG corners must trace the boundary, or the silhouette draws as a bow-tie. All 13 files pass (design probe). Reversal is accepted because `vertices` promises corner order but not winding.
- **Warm-ups sit exactly on the line.** Each of the four shipped warm-ups has a piece at exactly 0.5 exposure, so a bare `>= 0.5` rejects them on rounding.
- **Corner-only joins** appear in every full puzzle (2–5 each). Only shared length connects pieces.
- **Files and coordinates:**
  - The schema, `README.md` and `previews/` never ship.
  - Negative coordinates and fractional `[a, b]` values (the sailboat) are valid.
  - Decode as UTF-8 (the "ä" in "Lämmittely").
- **`ART` rejects:**
  - colours that are not `#RRGGBB`;
  - opacity outside 0..1, or width ≤ 0;
  - a polygon with fewer than 3 points, or a point that is not two numbers;
  - `line` or `path` without `stroke` (V10);
  - `path.d` that is not absolute M/L/Q/C/Z with 2/2/4/6/0 numbers per command and starting with M (the `ArtShape.Path` KDoc; WO-003 draws it).

  REQ-039's "3–8 colours" is not a load rule: `shapes-mini-2` uses two hex colours, and the owner's review judges pictures.
- **Library facts relied on (kotlinx.serialization 1.11, default `Json`):**
  - `art.shapes` is decoded as a `@Serializable sealed class ShapeDto` whose subclasses carry `@SerialName("polygon")` and so on.
  - The discriminator is `"type"`, read from any position in the object. No subclass may declare a `type` property.
  - An unknown type, an unknown key or a missing field without a default throws `SerializationException`.
  - The `coord` union cannot be declared, so the parser hand-maps a `JsonElement`.
  - `SerializationException` and `NumberFormatException` both extend `IllegalArgumentException`.
  - Tests see `internal` declarations.
- **Tools fix:** `build_prototype.py` and `make_ui_sheets.py` also call `outline_corners`. Their generated outputs (DI-1, DI-2) are not rebuilt here (G-06). Design probe: only warmup-4 changes, gaining (2,2), and every build order stays complete.
- **Format narrowing (review F1, `decisions.md` line 47).** The locked format admits a solution entry either as `polygon` or as `piece` + `rot`/`flip`/`at` (schema `placement.oneOf`, `Spec/03` §2). The loader reads only the `polygon` form, which all 13 files use. A format-valid file in the other form therefore fails the build as `PLACEMENT`; the DTO declares the three keys so that the rejection is `PLACEMENT`, not `JSON` (§2 `PuzzleParser.parse`). The authoring validator still accepts the other form and compares polygons as vertex sets (V3). That divergence is a tools follow-up for the content batch WO, not part of this WO.
- **Must never:**
  - list an invalid puzzle (fail fast);
  - expose a path, JSON or a DTO (everything is internal);
  - change at runtime (the list is immutable and built once).

## 4. Suggested cut

*As planned 2026-10-11 (`tasks.md` § WO-002), in landing order. The design's suggested geometry task (`ValidityCheck`, `Edges`) is merged into TASK-005 (review F2), and the id TASK-006 is not used.*

| Task | [P] | Delivers seams | Covers | Depends on |
|---|---|---|---|---|
| TASK-004 | | `bundlePuzzles` and `BundledPuzzles.read`. `ContentScaffoldTest` gains an untokened bundle check. **Done:** the JVM read plus the artifact listings: the debug APK, the release AAR's `classes.jar` and the release APK (§3, review F3). Hardest first. | REQ-007.A1 (groundwork) | — |
| TASK-008 | | Acceptance kit (acceptance-test-author). Drafted in the author's scratchpad beside TASK-004; lands in one stage once TASK-004 is green. Its Done-check is met at landing; the kit's green verdict is TASK-005's. | every in-scope ID | TASK-004 |
| TASK-005 | | DTOs, `PuzzleParser`, `PuzzleLibrary`, `PuzzleLoader`, `LoadResult`/`Violation`/`Rule`, `ValidityCheck`, `Edges`, `PuzzleLibraries.bundled` and the `:kernel` test dependency. Deletes `Content.kt`. | every in-scope ID | TASK-004, TASK-008 |
| TASK-007 | | `outline_corners` fix and tool test. **Done:** the validator passes 13/13 with complete build orders; warmup-4 gains (2,2); the other 12 are unchanged. No task of this WO is `[P]`: all four share the read-only input `Tangrams/*.json` (task-decomposition v0.5). | REQ-038.A1, groundwork on the tool side (no token claimed) | — |

**Kit staging (review F2).** Gradle compiles all of `content/src/test` at once. The kit is therefore drafted in parallel and enters the tree in **one stage**, once TASK-004's done-check is green. The content test set then stays red, not compiling, until TASK-005 lands; TASK-005's done-check is the kit's green verdict. Because the geometry is part of TASK-005, there is no step at which a kit test exists that its task cannot turn green.

**Not in this WO (review F4).** The `:app` wiring of `PuzzleLibraries.bundled(LockEngine())` belongs to WO-003, the first WO that reads `IPuzzleLibrary` on screen. That WO builds the library once per process.

**Orchestrator at G3:** the `build-map.md` #Content deps cell (notify-tier delta), done as build-map 1.2.

*Alternatives considered:*
- **Android `assets/`:** needs an `AssetManager`, so the JVM would need Robolectric (D5) or a second path.
- **A content-side TYPE-001 table:** a second copy of the piece shapes (D1, O-01).
- **A new `ILockEngine` member:** a change to the lock seam for a content rule.
- **Dropping invalid files:** a broken file would vanish silently on the device.

## Handoff — Design Author · WO-002

- **Scope:** REQ-007 A1–A2, REQ-038 A1, REQ-040 A1–A2, REQ-041 A1–A2, REQ-042 A2–A3, REQ-045 A1; tools `outline_corners` fix · governed touched: `IPuzzleLibrary` + format (implements, unchanged), `ILockEngine` (consumes), `Types.kt` (consumes)
- **Inputs read:** WO-002 (log 13:58) · REQ-007/038/039/040/041/042/045, TYPE-001 (v1.2) · contracts (baseline 2026-10-11) · architecture v1.0 · build-map v1.1 · design-inputs v1.1 · Spec/03 0.4, schema, `Tangrams/` · tools · WO-001 design · Gradle (AGP 9.4.1, Kotlin 2.4.20, serialization 1.11.0)
- **Result:** `designs/WO-002-design.md`; 9 `decisions.md` rows (G4 surface WO-002)
- **Status:** forward
- **Traceability delta:** none yet (TASK-004..008 proposed)
- **Notes for next station:** Verify the §3 AGP facts by building, and check `stillValid`-as-overlap against WO-001 `Silhouette.contains`. Architecture §4 "Between" for `ILockEngine` omits `:content`; the file is locked and was not edited.
