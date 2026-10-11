# WO-001 — dispatch briefs (#Locking)

**Planner:** wo-planner (Opus)  ·  **Date:** 2026-10-11  ·  **Plan:** `tasks.md` § WO-001 (TASK-001..003)
**Mode:** Full (b), because a governed interface is touched: `ILockEngine` is implemented for the first time (`workorders/WO-001.md` § Mode).

> Each brief is dispatched on its own. The Acceptance Test Author gets only § TASK-003 plus the two tables below. It never sees § TASK-001 or § TASK-002.
> Every path is relative to `C:/GitHub/AI/TangramPlaytime/` unless it is absolute. The kit folder is `kernel/src/test/kotlin/app/tangramplaytime/kernel/`.

## Dispatch order (two lanes)

| Step | Test lane (acceptance-test-author) | Implementer lane (slice-implementer) | Gate to the next step |
|---|---|---|---|
| 1 | TASK-003 **stage 1**: `KitFixtures.kt` and `GeometryKitTest.kt` land in the kit folder. `LockEngineKitTest.kt` is drafted in the author's scratchpad, not in the tree. | TASK-001 | TASK-001's Done-check is green |
| 2 | TASK-003 **stage 2** (resume the same author context with "TASK-001 done"): `LockEngineKitTest.kt` lands. | TASK-002 | TASK-002's Done-check is green |
| 3 | Run `plan_check.py --wo WO-001` again, now that the kit has compiled (WO DoD) → Code Reviewer | | |

- **Why the kit is staged.** Gradle compiles all of `kernel/src/test` in one go. `LockEngineKitTest.kt` calls `LockEngine()`, which TASK-002 delivers. If that file were in the tree during TASK-001, `:kernel:test` would not compile, and TASK-001's Done-check could never go green.
- **If TASK-001 hands back before stage 1 has landed:** the orchestrator runs TASK-001's Done-check when stage 1 lands, and only then dispatches TASK-002.
- **Compile errors in the kit.**
  - An unresolved reference to a seam whose task has not landed yet is expected.
  - Any other compile error in a kit file is a test defect. It goes back to the author and counts as a corrected check. An implementer never edits a kit file.

## Verification channels (G3)

| Channel | How (Git Bash, one line) | Result | Needed by |
|---|---|---|---|
| JVM `:kernel:test` (JUnit 4) | `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat :kernel:test"` | `kernel/build/test-results/test/TEST-*.xml` | TASK-001, TASK-002, TASK-003 |
| JVM `:contracts:test` (JUnit 4, must stay unchanged) | the same line with `:contracts:test :kernel:test` | `contracts/build/test-results/test/TEST-*.xml` | TASK-002, Test & Verify |

Not needed in this WO (`workorders/WO-001.md` § Constraints): the dev device image, the floor image `Phone_API_26`, a browser and human eyes. There is no UI and no device test. The toolchain proof is green: WO-001 Log, 2026-10-11 11:14.

---

## Brief — TASK-003 · WO-001 · acceptance-test-author

- **Mission:** Write WO-001's one acceptance kit in the kit folder, land it in two stages, and compile it against the tree. The kit has two parts:
  - the seven in-scope acceptance lines, tested through `LockEngine()`;
  - hand-computed oracle pins of the TASK-001 seams.

  The task is judgment-heavy, because the fixtures must discriminate. It runs on Opus.
- **Read exactly:**
  1. `tasks.md` § WO-001, all three rows. Yours is TASK-003; the other two rows say when your stages land.
  2. `workorders/WO-001.md`:
     - § Scope, including the note on the REQ-020.A1/A2 seam half and the A2 escape clause;
     - § Constraints: the tolerance, G-12 and G-15.
  3. Requirements, collection v1.2:
     - `Requirements/reqs/REQ-019.md`, `REQ-020.md`, `REQ-022.md`;
     - `Requirements/req_types.md` v0.2: TYPE-001, TYPE-003, TYPE-004.
  4. Frozen contracts, baseline 2026-10-11: `contracts/src/main/kotlin/app/tangramplaytime/contracts/ILockEngine.kt` and `Types.kt`.
  5. `designs/WO-001-design.md` (Reviewed):
     - §2, the seam table, every row. Its signatures and value shapes are what your tests call.
     - §3, these bullets only: "Exact-anchor arithmetic", "MT mirror trap", "Turn and mirror stay as dropped", "A3", "A4", "REQ-020, seam half", "REQ-022", "Oracle honesty", "Corner rule: contiguity", "The TYPE-004 window does not change the winner".
  6. `reviews/WO-001-design-review.md`: F1 (its Triage), F2, F3, F4.
  7. Transcription sources, for authoring only. The kit reads no file when it runs (O-02).
     - `Tangrams/shapes-square.json`
     - `Tangrams/shapes-rectangle.json`
     - `Tangrams/shapes-mini-1.json`
     - `Tangrams/shapes-warmup-4.json`
  8. In-tree tests, to check for collisions:
     - `kernel/src/test/kotlin/app/tangramplaytime/kernel/KernelScaffoldTest.kt`
     - `contracts/src/test/kotlin/app/tangramplaytime/contracts/ContractsSanityTest.kt`
     - `kernel/build.gradle.kts`: JUnit 4 is the only test dependency.
  9. Rules:
     - `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2: a token is a coverage claim.
     - `architecture.md` §2 G-04 and G-15, §3b O-02, and §5 "Allowed dependencies".
- **Do not read:**
  - anything under `kernel/src/main/`. Those are implementation bodies; your signatures come from the seam table. Compiler messages are fine to read.
  - design §1;
  - § TASK-001 and § TASK-002 of this file;
  - DI-1 (`tools/prototype_template.html`, `Spec/prototype/`) and DI-4 (`Study/`);
  - `tools/tangram_geom.py`. It is not an oracle: its corner rule misses warmup-4 (2,2) (review F1).
  - `progress.md`, chat history, and anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** none. The fixtures are transcribed, and no test reads a file outside `kernel/`.
- **Run:**
  - **Stage 1, now, beside TASK-001:**
    1. Write `KitFixtures.kt` and `GeometryKitTest.kt` into the kit folder.
    2. Draft `LockEngineKitTest.kt` in your scratchpad, never under `kernel/`.
    3. Hand back.
  - **Stage 2, when you are resumed with "TASK-001 done":** write `LockEngineKitTest.kt` into the kit folder.
  - **Compile against the tree** with `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat :kernel:test"`.
    - Expected, and not your defect: an unresolved reference to a seam whose task has not landed (TASK-001: `PieceShapes`, `Polygons`, `Silhouette`, `LockRules`; TASK-002: `LockEngine`), and red tests before the implementer lands.
    - Any other compile error is yours to fix.
  - **Before each hand-back,** Grep the kit for `REQ-0`. Each of the seven tokens must sit only on a test whose assertion is that line's meaning. `GeometryKitTest.kt` must contain none.
- **Done means:**
  - **Files.**
    - `KitFixtures.kt` follows the design's "Kit fixtures" row: `puzzleOf`, `GIFT_BOX`, `CHOCOLATE_BAR`, `MINI_PYRAMID`, `poseOf`, `assertNear`.
    - It also holds `WARMUP_4`, from `shapes-warmup-4.json`. The `outlineCorners` seam row and review F1 need it.
    - Each fixture names its source file in a comment.
    - The other two files are `GeometryKitTest.kt` and `LockEngineKitTest.kt`.
  - **Coverage.**
    - `LockEngineKitTest.kt` carries the tokens REQ-019.A1, REQ-019.A2, REQ-019.A3, REQ-019.A4, REQ-020.A1, REQ-020.A2 and REQ-022.A1.
    - Each token sits on a test whose assertion is that line's meaning.
    - Each such test cites, in a comment, the REQ, TYPE or seam-row sentence it asserts (assert only what is mandated).
  - **Oracle pins.**
    - `GeometryKitTest.kt` pins the TASK-001 seams (`PieceShapes`, `Polygons`, `Silhouette.contains` and `Silhouette.outlineCorners`, `LockRules`) with hand-computed values. Design §3 "Oracle honesty, A2" requires these pins before any test may use `Polygons` or `Silhouette` as an oracle.
    - It pins that `WARMUP_4`'s outline corners include (2,2). Derive that by hand from the `ILockEngine.outlineCorners` KDoc (review F1).
    - Every pin is tagged `// guardrail G-04` and cites its TYPE or seam row. It carries no REQ token.
  - **Constraint F2 (review F2).**
    - The REQ-019.A3 test includes at least one pose that an engine with hidden anchors would lock: one that treats every solution vertex, or edge points, as anchors.
    - Review F2 names two verified poses. The design's §3 "A3" pose alone does not discriminate.
  - **Constraint F3 (review F3).**
    - `poseOf` returns the lowest matching turn and fails loudly when no turn matches. An SQ matches four turns and a PG two.
    - The REQ-022.A1 arrangement differs from the stored solution and is built through seven `findLock` calls, each asserted `Locked`.
    - `isSolved` must be false before the seventh lock and true after it.
    - Review F3 names a verified arrangement. The Mini pyramid cannot serve.
  - **Constraint F4 (review F4).**
    - Every `findLock` fixture has a unique best score, by more than 1e-9. A tie is a fixture defect, never something to assert.
    - Disregard the line in design §3 about "anchors 1.5 and √2"; it has no referent.
    - No test tries to tell the TYPE-004 window from a score window (design §3).
  - **REQ-020.**
    - A1 and A2 are tested at the seam (WO § Scope).
    - If you judge the seam oracle for REQ-020.A2 dishonest, leave its token out and say so in the hand-back. The orchestrator then moves A2 to WO-003.
  - **Hygiene.**
    - Compare points with `assertNear`, never `==`.
    - Call internal members directly, with no reflection. Use JUnit 4 only and add no dependency.
    - Write no acceptance test for `tryTurnInPlace`, `tryMirrorInPlace` or `stillValid`; those belong to their own REQs' WOs.
    - A test of the `findLock` `IllegalArgumentException` preconditions is optional. If you write one, tag it `// decision` (the `decisions.md` row of 2026-10-11, WO-001 / P3 design), never with a REQ token.
    - You never edit `kernel/src/main/`.
  - **Compilation.** Each stage compiles in the tree once its seams exist.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the test names for each acceptance ID, and the list of geometry pins;
  - the files outside `kernel/` that tests read at run time (expected: none);
  - the compile status of each stage, with the command and its exit code;
  - how many corrected checks there have been so far.

---

## Brief — TASK-001 · WO-001 · slice-implementer

- **Mission:** Build the geometry and validity helpers from the WO-001 seam table: `PieceShapes`, `Polygons`, `Silhouette` and `LockRules`, as `internal` objects in `:kernel`. The task is judgment-heavy geometry and comes first because it is the hardest. It runs on Opus.
- **Read exactly:**
  1. `tasks.md` § WO-001, row TASK-001.
  2. `Requirements/reqs/REQ-019.md`; `Requirements/req_types.md` v0.2: TYPE-001, TYPE-003, TYPE-004.
  3. `designs/WO-001-design.md` (Reviewed):
     - §2, the rows `PieceShapes`, `Polygons`, `Silhouette` and `LockRules`. Also the `outlineCorners` row, whose expected results are the kit's.
     - §3, these bullets: "Highest-risk part", "Exact-anchor arithmetic", "MT mirror trap", "Corner rule: contiguity", "Library facts relied on".
  4. `reviews/WO-001-design-review.md` F1: the corner rule as fixed.
  5. Contracts:
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/ILockEngine.kt`: the KDoc of `outlineCorners` and `vertices`.
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/Types.kt`: `PieceType`, `Turn`, `Point`, `SolvedPiece`.
  6. `architecture.md` §2 G-04, G-12, G-15; §3b O-01; §5 "Allowed dependencies".
  7. `design-inputs.md` §2:
     - Superseded items 1 and 3.
     - The "Also fine" bullet on anchors (the angle-sum rule). The design's `Silhouette` row overrides it (review F1; the Contract wins).
  8. Design inputs. These are a behaviour reference only: never a source of acceptance, never DoD. Where they differ, the design and the Contract win.
     - **DI-5** `Spec/03-puzzle-format.md` §1 (coordinates, y down) and §3 (piece shapes, corner order).
     - **DI-5** `tools/tangram_geom.py`: `PIECE_TYPES`, `transform`, `outline_corners`. This is reference math. Its corner classification is the angle-sum rule, which misses warmup-4 (2,2). Follow the design's contiguity rule instead.
     - **DI-1** `tools/prototype_template.html`, lines ~204–234 and ~501–535: `verts`, `polyArea`, `clip`. It mirrors every piece type; mirror PG only (design §3 "MT mirror trap").
     - **DI-4** `Study/06-snapping-by-anchors.md` §Anchors. The folder is frozen: read only.
  9. The visible acceptance tests, from TASK-003 stage 1. Read them; never edit them.
     - `kernel/src/test/kotlin/app/tangramplaytime/kernel/GeometryKitTest.kt`
     - `kernel/src/test/kotlin/app/tangramplaytime/kernel/KitFixtures.kt`
  10. Existing files:
      - `kernel/build.gradle.kts`;
      - `kernel/src/main/kotlin/app/tangramplaytime/kernel/Kernel.kt`, the placeholder. Leave it; TASK-002 deletes it.
      - `kernel/src/test/kotlin/app/tangramplaytime/kernel/KernelScaffoldTest.kt`.
- **Do not read:** § TASK-002 and § TASK-003 of this file; any draft of `LockEngineKitTest.kt`; `Tangrams/*.json` (O-02); `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** none.
- **Run:**
  - `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat :kernel:test"`. The results are under `kernel/build/test-results/test/`.
  - Grep `kernel/src` for `import android`. There must be no match (G-12).
- **Done means:**
  - **Files.** New `PieceShapes.kt`, `Polygons.kt`, `Silhouette.kt` and `LockRules.kt` in `kernel/src/main/kotlin/app/tangramplaytime/kernel/`.
    - Each signature matches its seam row exactly, because the kit calls them.
    - Visibility is `internal`; dependencies are the stdlib and `:contracts` only.
  - **Done-check.** `GeometryKitTest.kt` and `KernelScaffoldTest.kt` are green in the tree. If stage 1 of the kit is not in the tree when you finish, say so; the orchestrator runs the Done-check when it lands.
  - **No placeholders.** No stub, no `TODO()`, and no hard-coded corner list where a seam row names a rule (task DoD in `C:/GitHub/AI/SWDev/framework/methodology/definitions.md`).
  - **Your own tests.** Any unit tests you write start with `// scaffolding` and carry no `REQ-NNN.A<n>` token.
  - **The kit is read-only for you.** If you think a kit line is wrong, or the kit has a compile error that is not one of your missing seams, put it in your hand-back.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the inputs read, with their versions, and the files changed;
  - the traceability delta;
  - the command run, its exit code, the result XML paths and the test counts.

---

## Brief — TASK-002 · WO-001 · slice-implementer

- **Mission:** Build `class LockEngine : ILockEngine` in `:kernel`: all seven members, `anchors` and the design §1 search. Delete the `Kernel.kt` placeholder. The task is judgment-heavy (the search and the exact-anchor arithmetic) and runs on Opus.
- **Read exactly:**
  1. `tasks.md` § WO-001, row TASK-002.
  2. Requirements, collection v1.2:
     - `Requirements/reqs/REQ-019.md`, `REQ-020.md`, `REQ-022.md`;
     - `Requirements/req_types.md` v0.2: TYPE-001, TYPE-003, TYPE-004.
  3. `designs/WO-001-design.md` (Reviewed):
     - §1, all of it.
     - §2, the rows `LockEngine`, `findLock`, `vertices`, `outlineCorners`, `isSolved`, the `tryTurnInPlace`/`tryMirrorInPlace`/`stillValid` row, and `anchors`. Also the rows `PieceShapes`, `Polygons`, `Silhouette` and `LockRules`, which are the API you call.
     - §3, all of it.
  4. `reviews/WO-001-design-review.md`:
     - **F4, the tie rule.** Implement design §1 steps 3 and 5 exactly as written: a stable sort by d, and a candidate replaces the best only when its score is lower by more than 1e-9. The kit does not pin ties.
     - **F1**, for what `outlineCorners` delegates to.
  5. Contracts, baseline 2026-10-11: `contracts/src/main/kotlin/app/tangramplaytime/contracts/ILockEngine.kt` and `Types.kt`.
  6. `architecture.md` §2 G-04, G-12, G-15; §3b O-01; §5 "Allowed dependencies"; §6 ADR-001.
  7. `decisions.md`: the row of 2026-10-11, WO-001 / P3 design, on the `findLock` `IllegalArgumentException` preconditions.
  8. `design-inputs.md` §2:
     - Superseded items 1 and 3.
     - The "Also fine" bullet on anchors, which the design overrides (review F1).
  9. Design inputs. These are a behaviour reference only: never a source of acceptance, never DoD.
     - **DI-1** `tools/prototype_template.html`, lines ~204–234 and ~501–535: `anchors`, `validAt`, `findLock`.
       - Its score-based candidate window is superseded (`design-inputs.md` §2 item 3).
       - Never port its `cx + tx` (design §3 "Exact-anchor arithmetic").
     - **DI-4** `Study/06-snapping-by-anchors.md`. The folder is frozen. Its per-level parameters, variants and score terms are superseded (`design-inputs.md` §2 item 1).
  10. TASK-001's landed files `PieceShapes.kt`, `Polygons.kt`, `Silhouette.kt` and `LockRules.kt`. They are the API you call; do not change them.
  11. The visible acceptance tests. Read them; never edit them.
      - `kernel/src/test/kotlin/app/tangramplaytime/kernel/LockEngineKitTest.kt`
      - `kernel/src/test/kotlin/app/tangramplaytime/kernel/GeometryKitTest.kt`
      - `kernel/src/test/kotlin/app/tangramplaytime/kernel/KitFixtures.kt`
  12. Existing files: `kernel/src/main/kotlin/app/tangramplaytime/kernel/Kernel.kt` (to delete) and `kernel/src/test/kotlin/app/tangramplaytime/kernel/KernelScaffoldTest.kt` (keep it).
- **Do not read:** § TASK-001 and § TASK-003 of this file; review findings other than F1 and F4; `Tangrams/*.json` (O-02); `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** none.
- **Run:**
  - `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat :contracts:test :kernel:test"`. The results are under `*/build/test-results/test/`.
  - Grep `kernel/src` for `import android`. There must be no match (G-12).
- **Done means:**
  - **The class.** New `kernel/src/main/kotlin/app/tangramplaytime/kernel/LockEngine.kt` holds `class LockEngine : ILockEngine`.
    - Package `app.tangramplaytime.kernel`; a no-argument constructor; no state.
    - Every member is implemented as §1 and its seam row say. No stub and no `TODO()`.
    - `anchors` is `internal fun LockEngine.anchors(...)`.
  - **The placeholder.** `Kernel.kt` is deleted. If the guard denies the delete, leave the file and say so.
  - **Files you do not change.**
    - TASK-001's files. A defect there goes in your hand-back, naming the seam and the failing kit test.
    - `:contracts`. A needed contract change is a `contract-delta`; hand it back, never edit it.
    - The kit.
  - **Done-check.**
    - `LockEngineKitTest.kt` is green: REQ-019.A1–A4, REQ-020.A1–A2, REQ-022.A1.
    - `GeometryKitTest.kt` and `KernelScaffoldTest.kt` are green.
    - `:contracts:test` is green.
  - **The wrappers.** `tryTurnInPlace`, `tryMirrorInPlace` and `stillValid` have no kit coverage in this WO; it arrives with the WOs of REQ-016, REQ-018 and REQ-025. Scaffolding tests for them are welcome if they start with `// scaffolding` and carry no REQ token.
  - **Ambiguity.** If the spec is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the inputs read, with their versions, and the files changed or deleted;
  - the traceability delta;
  - the command run, its exit code, the result XML paths and the test counts for each module.

---

## Handoff — Work-Order Planner · WO-001

- **Scope:** REQ-019 (A1–A4), REQ-020 (A1–A2), REQ-022 (A1); TYPE-001/003/004. Governed interfaces touched: `ILockEngine` (implements), `Types.kt` (consumes).
- **Inputs read:**
  - `workorders/WO-001.md` (log to 11:45);
  - `designs/WO-001-design.md` (Reviewed, 13,770 B);
  - `reviews/WO-001-design-review.md` (second look);
  - REQ-019, REQ-020, REQ-022 (collection v1.2); `req_types.md` v0.2;
  - `ILockEngine.kt` and `Types.kt` (baseline 2026-10-11);
  - `build-map.md` v1.0; `architecture.md` v1.0 §2, §3b, §5; `design-inputs.md` v1.0 §2;
  - `kernel/` (2 source files and `build.gradle.kts`); `ContractsSanityTest.kt`;
  - SWDev `task-decomposition.md` v0.5, `templates/tasks.md`, `traceability-rules.md` §2, `plan-check` SKILL and script, `handoff-contract.md` v0.3.
- **Result:** `tasks.md` § WO-001 (TASK-003 [P], TASK-001, TASK-002) and `workorders/WO-001-briefs.md`.
- **Status:** forward → plan-reviewer.
- **Traceability delta:**
  - REQ-019 → TASK-001 (A2, A3 groundwork), TASK-002, TASK-003.
  - REQ-020 → TASK-002, TASK-003.
  - REQ-022 → TASK-002, TASK-003.
  - Kit files planned: `LockEngineKitTest.kt` (7 tokens) and `GeometryKitTest.kt` (no token).
- **Notes for next station:**
  1. The kit lands in two stages, so `:kernel:test` compiles at every step.
  2. plan-check gives two warnings, both triaged. The out-of-scope IDs are a by-product of the REQ × A product on rows naming several REQs. TASK-001 has no green ID at its step by design: it is groundwork.
  3. The design's seam check was done by hand: all 12 rows are TASK-001, TASK-002 or TASK-003.
