# Tasks — TangramPlaytime

**Status:** Active  ·  **Last updated:** 2026-10-11

> Each task links back to the requirement(s) it implements via REQ IDs and
> the acceptance IDs it covers, so the traceability chain
> `REQ → task → code → test` stays intact. Do not invent tasks for work no
> requirement asked for (no silent scope). Columns and rules:
> `C:/GitHub/AI/SWDev/framework/templates/tasks.md` — **Inputs** (files
> outside the module its tests read) and **Done-check** (the test that must
> be green at that step) are mandatory; a **Status** cell is one word plus
> a link to the WO log, edited with the Edit tool only.

---

## WO-001 — #Locking: the one lock rule in :kernel behind ILockEngine  (Mode: Full (b))

Two lanes. The test lane is TASK-003 [P] (Acceptance Test Author). The implementer lane is TASK-001 → TASK-002, hardest first. Rows are in landing order. The kit lands in two stages: stage 1 beside TASK-001, and stage 2 once TASK-001 is done, because Gradle compiles the whole `kernel/src/test` set at once. Briefs: `workorders/WO-001-briefs.md`. Verification channels (G3): JVM `:kernel:test` and `:contracts:test` only; no device channel.

| Task | [P] | Description | Implements | Covers | Depends on | Inputs | Done-check | Status |
|---|---|---|---|---|---|---|---|---|
| TASK-003 | [P] | Acceptance kit (acceptance-test-author; judgment-heavy, Opus). Stage 1, beside TASK-001: `kernel/src/test/kotlin/app/tangramplaytime/kernel/KitFixtures.kt` and `kernel/src/test/kotlin/app/tangramplaytime/kernel/GeometryKitTest.kt` (hand-computed G-04 pins of the TASK-001 seams, no REQ token). Stage 2, once TASK-001 is done, beside TASK-002: `kernel/src/test/kotlin/app/tangramplaytime/kernel/LockEngineKitTest.kt`. Fixtures are transcribed; the kit reads no file | REQ-019, REQ-020, REQ-022 | REQ-019.A1, REQ-019.A2, REQ-019.A3, REQ-019.A4, REQ-020.A1, REQ-022.A1 (REQ-020.A2 → WO-003, log 12:48) | — | — | `kernel/src/test/kotlin/app/tangramplaytime/kernel/GeometryKitTest.kt` compiles once TASK-001 lands; `kernel/src/test/kotlin/app/tangramplaytime/kernel/LockEngineKitTest.kt` compiles once TASK-002 lands, with the tokens REQ-019.A1–A4, REQ-020.A1 and REQ-022.A1, each on its asserting test | done |
| TASK-001 |  | Geometry and validity helpers (slice-implementer; judgment-heavy geometry, Opus): `internal` objects `PieceShapes`, `Polygons`, `Silhouette` (contiguity corner rule, review F1) and `LockRules`, in new `PieceShapes.kt`, `Polygons.kt`, `Silhouette.kt`, `LockRules.kt` under `kernel/src/main/kotlin/app/tangramplaytime/kernel/` | REQ-019 (+ TYPE-001, TYPE-003, TYPE-004) | REQ-019.A2, REQ-019.A3 (groundwork: the validity predicate and the corner rule; proven at TASK-002) | — | — | `kernel/src/test/kotlin/app/tangramplaytime/kernel/GeometryKitTest.kt` green (G-04 pins; no REQ ID is green at this step) and `kernel/src/test/kotlin/app/tangramplaytime/kernel/KernelScaffoldTest.kt` green | done |
| TASK-002 |  | `LockEngine` (slice-implementer; judgment-heavy search and exact-anchor arithmetic, Opus): new `kernel/src/main/kotlin/app/tangramplaytime/kernel/LockEngine.kt`, which is `class LockEngine : ILockEngine` with all 7 members, `anchors` and the design §1 search. Deletes the `kernel/src/main/kotlin/app/tangramplaytime/kernel/Kernel.kt` placeholder and keeps `KernelScaffoldTest` | REQ-019, REQ-020, REQ-022 (+ TYPE-001, TYPE-003, TYPE-004) | REQ-019.A1, REQ-019.A2, REQ-019.A3, REQ-019.A4, REQ-020.A1, REQ-022.A1 | TASK-001 | — | `kernel/src/test/kotlin/app/tangramplaytime/kernel/LockEngineKitTest.kt` REQ-019.A1–A4, REQ-020.A1, REQ-022.A1 green, plus `kernel/src/test/kotlin/app/tangramplaytime/kernel/GeometryKitTest.kt` and `kernel/src/test/kotlin/app/tangramplaytime/kernel/KernelScaffoldTest.kt` green; `:contracts:test` green | implemented (unverified) → WO-001 log 2026-10-11 (tests 14/14 green; REQ-019.A1–A4 await the TYPE-004 signature, `capture-outbox/chg-draft-lock-type-dictionary.md`) |

**Statuses:** todo → in progress → in review → done · blocked (→ log) · implemented (unverified). A cell other than `todo`/`done` is the word plus `→ WO-001 log YYYY-MM-DD`.

---

## WO-002 — #Content: the puzzle library in :content behind IPuzzleLibrary  (Mode: Full (b))

There are two lanes:
- **Test lane:** TASK-008 (Acceptance Test Author). It drafts the kit in its scratchpad while TASK-004 runs, and lands it once TASK-004 is green.
- **Implementer lane:** TASK-004 → TASK-005 → TASK-007. The hardest task (the reading path) goes first.

Rows are in landing order. Briefs: `workorders/WO-002-briefs.md`.

- **Kit landing.** The kit is drafted beside TASK-004 and lands in **one stage**, once TASK-004's Done-check is green, because Gradle compiles the whole `content/src/test` set at once. The parallel drafting is dispatch, not a plan dependency: TASK-008 depends on TASK-004, and TASK-005 depends on TASK-008.
- **Red window.** From the kit's landing until TASK-005 lands, `:content:testDebugUnitTest` does not compile. The only errors are unresolved references to TASK-005's seams. TASK-008's Done-check is therefore met at landing, and the green verdict on the kit is TASK-005's Done-check.
- **No TASK-006.** The design's suggested geometry task is merged into TASK-005 (design review F2), so that id is not used.
- **No task is [P].** All four tasks declare the read-only input `Tangrams/*.json`, and the v0.18 rule serializes tasks that share a declared input. TASK-008 also depends on TASK-004's reader. TASK-007 (tools) shares no file with the Kotlin lane but reads the same puzzle files, so it runs last in the implementer lane.
- **Inputs.** The bundled puzzles count as an input of every content task. The tests read build-time copies, but the bytes come from `Tangrams/*.json`.
- **Verification channels (G3):**
  - JVM `:content:testDebugUnitTest` and `:contracts:test`;
  - the Gradle artifact listing (TASK-004, review F3);
  - the Python tool channel (TASK-007);
  - no device channel.
- **Not covered here** (no token; WO § Scope notes):
  - REQ-038 A2: the pending deletion CHG, carried `implemented (unverified)`;
  - REQ-039 A1 and A2: WO-003 and WO-009;
  - REQ-042 A1: the content batch WO and WO-009;
  - REQ-045 A2: WO-003;
  - the `:app` wiring of `PuzzleLibraries.bundled`: WO-003 (review F4).

| Task | [P] | Description | Implements | Covers | Depends on | Inputs | Done-check | Status |
|---|---|---|---|---|---|---|---|---|
| TASK-004 |  | Puzzle bundle and reader (slice-implementer; judgment-heavy Gradle/AGP 9.4 wiring, Opus). The `bundlePuzzles` task in `content/build.gradle.kts` copies the top-level `Tangrams/*.json` (never the schema, `README.md` or `previews/`) plus a generated index into `content/build/generated/puzzles`, wired with `androidComponents.onVariants … sources.resources?.addGeneratedSourceDirectory`. New `content/src/main/kotlin/app/tangramplaytime/content/BundledPuzzles.kt` holds `PuzzleSource` and `BundledPuzzles.read`, the one `ClassLoader` path. `content/src/test/kotlin/app/tangramplaytime/content/ContentScaffoldTest.kt` gains an untokened `// scaffolding` bundle check. `Content.kt` stays (TASK-005 deletes it) | REQ-007 (rule: puzzles in their own folder; ADR-004) | REQ-007.A1 (groundwork: the one reading path every shipped puzzle is loaded through; proven at TASK-005) | — | `Tangrams/*.json` (13 files, read through the generated resources; the bundle check also lists the Tangrams folder) | `content/src/test/kotlin/app/tangramplaytime/content/ContentScaffoldTest.kt` green, including the bundle check. No REQ ID is green at this step (REQ-007 A1 groundwork). Artifact listing channel: the debug APK, the unsigned release APK and the release AAR's classes jar each list index.txt plus the 13 puzzle files under the bundle folder, 14 entries each (review F3) | done |
| TASK-008 |  | Acceptance kit (acceptance-test-author; judgment-heavy because the crafted texts must discriminate, Opus). Drafted beside TASK-004 in the author's scratchpad; lands in one stage once TASK-004's Done-check is green. Three files: `content/src/test/kotlin/app/tangramplaytime/content/KitTexts.kt` (kit-prefixed helpers and crafted texts, no token); `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLibraryKitTest.kt` (the shipped bundle through `PuzzleLibraries.bundled(LockEngine())`); `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLoaderKitTest.kt` (shipped, edited and crafted texts through `PuzzleLoader.load`). Never edits `ContentScaffoldTest.kt` or `content/src/main` | REQ-007, REQ-038, REQ-040, REQ-041, REQ-042, REQ-045 (+ TYPE-001) | REQ-007.A1, REQ-007.A2, REQ-038.A1, REQ-040.A1, REQ-040.A2, REQ-041.A1, REQ-041.A2, REQ-042.A2, REQ-042.A3, REQ-045.A1 | TASK-004 | `Tangrams/*.json`, shared with TASK-004, TASK-005 and TASK-007 (read through the bundle reader of TASK-004, plus the Tangrams folder listing for bundle completeness); `:kernel` on the test classpath (`kernel/src/main/kotlin/app/tangramplaytime/kernel/LockEngine.kt`, WO-001, unchanged) | Met at landing; no REQ ID is green at this step. (1) `content/src/test/kotlin/app/tangramplaytime/content/KitTexts.kt`, `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLibraryKitTest.kt` and `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLoaderKitTest.kt` are in the kit folder. (2) A grep for `REQ-0` shows exactly the ten tokens, each on its asserting test: PuzzleLibraryKitTest.kt carries REQ-007.A1, REQ-007.A2, REQ-040.A1, REQ-041.A1, REQ-041.A2, REQ-042.A2, REQ-042.A3, REQ-045.A1; PuzzleLoaderKitTest.kt carries REQ-038.A1, REQ-040.A2, REQ-041.A2; KitTexts.kt carries none. (3) The only compile errors of `:content:testDebugUnitTest` are unresolved references to TASK-005's seams: `PuzzleLoader`, `LoadResult`, `Violation`, `Rule`, `PuzzleLibraries`, and `LockEngine` until the `:kernel` test dependency lands. Red window: the content test set does not compile from this landing until TASK-005 lands; the green verdict is TASK-005's Done-check | done |
| TASK-005 |  | Parser, load-time validity and the library (slice-implementer; judgment-heavy parser, ART grammar and congruence/edge geometry, Opus). New `PuzzleDtos.kt`, `PuzzleParser.kt`, `PuzzleLoader.kt` (`LoadResult`, `Violation`, `Rule`), `ValidityCheck.kt`, `Edges.kt`, `PuzzleLibrary.kt` and `PuzzleLibraries.kt` under `content/src/main/kotlin/app/tangramplaytime/content/`. Adds `testImplementation(project(":kernel"))` to `content/build.gradle.kts`. Deletes `content/src/main/kotlin/app/tangramplaytime/content/Content.kt`. Includes the review F1 narrowing (`rot`/`flip`/`at` → `PLACEMENT`) and the F2 merge (the geometry is here). No `:app` change (F4: WO-003) | REQ-007, REQ-038, REQ-040, REQ-041, REQ-042, REQ-045 (+ TYPE-001) | REQ-007.A1, REQ-007.A2, REQ-038.A1, REQ-040.A1, REQ-040.A2, REQ-041.A1, REQ-041.A2, REQ-042.A2, REQ-042.A3, REQ-045.A1 | TASK-004, TASK-008 | `Tangrams/*.json` (through the generated resources, plus the Tangrams folder listing that the kit's bundle-completeness test reads); `:kernel` on the test classpath (`kernel/src/main/kotlin/app/tangramplaytime/kernel/LockEngine.kt`, unchanged) | Green: `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLibraryKitTest.kt` REQ-007.A1, REQ-007.A2, REQ-040.A1, REQ-041.A1, REQ-041.A2, REQ-042.A2, REQ-042.A3, REQ-045.A1; `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLoaderKitTest.kt` REQ-038.A1, REQ-040.A2, REQ-041.A2 (REQ-038.A1 then stays implemented (unverified) for the pending validity-rules CHG); `content/src/test/kotlin/app/tangramplaytime/content/ContentScaffoldTest.kt`; `:contracts:test` | implemented (unverified) → WO-002 log 2026-10-11 (tests 66/66 green at r3; REQ-038.A1 awaits the capture side's signature of `capture-outbox/chg-draft-req038-validity-rules-in-contract.md`) |
| TASK-007 |  | Corner rule of the authoring tool (slice-implementer; a port of a specified rule with a crisp check, Sonnet). `outline_corners` in `tools/tangram_geom.py` classifies a vertex by the contiguity of the covered directions (`designs/WO-001-design.md` §2 `Silhouette`; design-inputs §2 item 13). Exact `Q2` points are kept and the signature is unchanged. New tool test `tools/tests/test_tangram_geom.py` (plain asserts, run directly). The prototype and the UI sheets are not regenerated (`decisions.md` line 51) | REQ-038 (rule: V11 prints the build order; the validator `tools/validate_puzzles.py` imports the changed module) | REQ-038.A1 (groundwork, tool-side evidence; no token claimed. The fix keeps "passes the validator", as locked, true for every file; the token and its asserting tests are TASK-008's) | — | `Tangrams/*.json` (13 files, read-only, read by the validator and the tool test) | `tools/tests/test_tangram_geom.py` green when run directly (a tool test: no token, outside test_globs): warmup-4's corners include (2,2) and the other twelve files' corners are unchanged. Python channel: python tools/validate_puzzles.py Tangrams reports 13/13 valid with a complete build order each (REQ-038 A1 as locked) | done |

**Statuses:** todo → in progress → in review → done · blocked (→ log) · implemented (unverified). A cell other than `todo`/`done` is the word plus `→ WO-002 log YYYY-MM-DD`.

## Change log

| Version | Date | Change | Reason |
|---|---|---|---|
| 0.1 | YYYY-MM-DD | placeholder | — |
| 0.2 | 2026-10-11 | WO-001 decomposed into TASK-001..003, replacing the placeholder section; Status → Active | P3 (wo-planner) |
| 0.3 | 2026-10-11 | WO-001 closed: TASK-001, TASK-003 done; TASK-002 implemented (unverified) for the pending TYPE-004 dictionary CHG; REQ-020.A2 moved to WO-003 | WO-001 close (orchestrator) |
| 0.4 | 2026-10-11 | WO-002 decomposed into TASK-004, TASK-008 [P], TASK-005, TASK-007. TASK-006 is not used: the design's geometry task is merged into TASK-005 (design review F2) | P3 (wo-planner) |
| 0.6 | 2026-10-11 | WO-002 closed: TASK-004, TASK-007, TASK-008 done; TASK-005 implemented (unverified) for the pending REQ-038 validity-rules CHG; REQ-039.A1 / REQ-045.A2 → WO-003, REQ-039.A2 / REQ-042.A1 → WO-009 (+ the content batch WO) | WO-002 close (orchestrator) |
| 0.5 | 2026-10-11 | WO-002 plan fixes:<br>- TASK-008 is no longer [P] and depends on TASK-004; its Done-check is met at landing and states the red window (B1, B2).<br>- TASK-005 depends on TASK-004 and TASK-008; its Inputs add the Tangrams folder listing (S1).<br>- TASK-007's Covers is worded as groundwork with no token claimed (S2). | plan review recirculation (`reviews/WO-002-plan-review.md`) |
