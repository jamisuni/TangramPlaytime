# WO-002 — dispatch briefs (#Content)

**Planner:** wo-planner (Opus)  ·  **Date:** 2026-10-11  ·  **Plan:** `tasks.md` § WO-002 (TASK-004, TASK-008, TASK-005, TASK-007; TASK-006 not used; no task is `[P]`)
**Mode:** Full (b), because a governed interface is touched: `IPuzzleLibrary` and the `tangram-puzzle/1` format (locked tier) are implemented for the first time (`workorders/WO-002.md` § Mode). Reason (a) is not claimed, because the `:content` skeleton exists since P2.

> Each brief is dispatched on its own. The Acceptance Test Author gets only § TASK-008 plus the two tables below. It never sees § TASK-004, § TASK-005 or § TASK-007.
> Every path is relative to `C:/GitHub/AI/TangramPlaytime/` unless it is absolute. The kit folder is `content/src/test/kotlin/app/tangramplaytime/content/`.

## Dispatch order (two lanes)

| Step | Test lane (acceptance-test-author) | Implementer lane (slice-implementer) | Gate to the next step |
|---|---|---|---|
| 1 | TASK-008 **drafts** all three kit files in the author's scratchpad. Nothing of the kit goes under `content/src/test` yet. | TASK-004 (Opus) | TASK-004's Done-check is green: `ContentScaffoldTest` plus the three artifact listings |
| 2 | TASK-008 **lands** (resume the same author context with "TASK-004 done"): the three files enter the kit folder. | — | TASK-008's Done-check is met at landing: the three files are present; the ten tokens sit on their stated tests, and there are none in `KitTexts.kt`; the only compile errors are unresolved references to TASK-005's seams |
| 3 | — (red window: `:content:testDebugUnitTest` does not compile until TASK-005 lands) | TASK-005 (Opus), reading the landed kit | TASK-005's Done-check is green. This is the kit's green verdict. |
| 4 | — | TASK-007 (Sonnet) | TASK-007's Done-check is green |
| 5 | Run `plan_check.py --wo WO-002` again, now that the kit has compiled (WO DoD) → Code Reviewer | | |

- **Why the kit lands in one stage.** Gradle compiles all of `content/src/test` in one go.
  - Before step 2, a kit file in the tree would stop `ContentScaffoldTest` from compiling, so TASK-004's Done-check could never go green.
  - The design's geometry task is merged into TASK-005 (design review F2). So once the kit lands, every test in it can be turned green by the one task in flight. No geometric test waits on a later task, and the loader is written only once.
- **Dependencies versus dispatch.**
  - Drafting the kit while TASK-004 runs is dispatch, not a plan dependency: it touches nothing in the tree.
  - In the plan, TASK-008 depends on TASK-004, whose reader its tests call, and TASK-005 depends on TASK-008, whose files its Done-check runs.
- **Red window.** From the kit's landing until TASK-005 lands, the `content` test set does not compile. Nobody runs `:content:testDebugUnitTest` for a verdict in that window.
- **No task is `[P]`.** All four tasks declare the read-only input `Tangrams/*.json`, and shared declared inputs serialize (`task-decomposition.md` v0.5). TASK-007 shares no file with the Kotlin lane, so it runs last in the implementer lane.
- **Compile errors in the kit.**
  - An unresolved reference to a TASK-005 seam before TASK-005 lands is expected. That includes `LockEngine`, which reaches the test classpath with TASK-005's `testImplementation(project(":kernel"))`.
  - Any other compile error in a kit file is a test defect. It goes back to the author and counts as a corrected check. An implementer never edits a kit file.
- **`ContentScaffoldTest.kt`** is scaffolding. It belongs to TASK-004, which adds the bundle check; TASK-005 keeps it green. The kit never edits it.

## Verification channels (G3)

| Channel | How (Git Bash, one line) | Result | Needed by |
|---|---|---|---|
| JVM `:content:testDebugUnitTest` (JUnit 4) | `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat :content:testDebugUnitTest"` | `content/build/test-results/testDebugUnitTest/TEST-*.xml` | TASK-004, TASK-005, TASK-008 |
| JVM `:contracts:test` (must stay unchanged) | the same line with `:contracts:test :content:testDebugUnitTest` | `contracts/build/test-results/test/TEST-*.xml` | TASK-005, Test & Verify |
| Artifact listing (Gradle build only, no device; review F3) | the same line with `:app:assembleDebug :app:assembleRelease :content:bundleReleaseAar`, then the three `unzip -l … \| grep app/tangramplaytime/content/puzzles/` listings in § TASK-004 "Run" | 14 entries in each: `index.txt` and the 13 puzzle files | TASK-004, Test & Verify |
| Python tool | `cd /c/GitHub/AI/TangramPlaytime && python tools/validate_puzzles.py Tangrams && python tools/tests/test_tangram_geom.py` | stdout and the exit code (run log row `py0` is the baseline) | TASK-007 |

- **Not needed in this WO:** the dev device image `Medium_Phone_API_37.0`, the floor image `Phone_API_26`, a browser and human eyes.
  - There is no UI, and nothing in `:app` calls the library yet (review F4).
  - The review proved from the Gradle build alone that the debug APK, the release AAR and the release APK carry the resources (R1). The listing channel re-proves that in the real tree.
  - The first on-device read of the bundle is WO-003's, when `:app` wires `PuzzleLibraries.bundled`.
- **Toolchain proof:** the JVM and Python channels are green (WO-002 Log 13:56, run log rows `g3` and `py0`). The artifact-listing channel has not yet run in this tree; see the planner's handoff.

---

## Brief — TASK-008 · WO-002 · acceptance-test-author

- **Mission:** Write WO-002's one acceptance kit, three files in the kit folder, and compile it against the tree.
  - It covers the ten in-scope acceptance lines at the `IPuzzleLibrary` seam and at the loader's door for crafted texts.
  - You draft it now and land it in one stage once TASK-004 is green.
  - The task is judgment-heavy, because every crafted text must discriminate. It runs on Opus.
- **Read exactly:**
  1. `tasks.md` § WO-002: the intro and all four rows. Yours is TASK-008; the other rows say when you land and what each task delivers.
  2. `workorders/WO-002.md`:
     - § Scope, including every scope note. They say which lines of these REQs are not this WO's and why, and how REQ-038 A1's "passes the validator" is read.
     - § Constraints & notes: the "must never" bullet, G-15 and the "Pending deltas" bullet.
  3. Requirements, collection v1.2 (read-only):
     - `Requirements/reqs/REQ-007.md`, `REQ-038.md`, `REQ-040.md`, `REQ-041.md`, `REQ-042.md`, `REQ-045.md`;
     - `Requirements/reqs/REQ-039.md`, only because REQ-007 A1 says "meets REQ-039"; its own lines carry no token in this WO;
     - `Requirements/req_types.md`: TYPE-001.
  4. `capture-outbox/chg-draft-req038-validity-rules-in-contract.md`, the pending CHG. Its written rules are what REQ-038.A1 is tested as (WO § Scope, last note).
  5. Frozen contracts, baseline 2026-10-11:
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/IPuzzleLibrary.kt`, every KDoc line, including "Must never";
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/Types.kt`: `PuzzleDefinition` (with `pieces`), `PuzzleKind`, `PieceId`, `SolvedPiece`, `ArtLayer`, `ArtShape`, `Point`;
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/ILockEngine.kt`, which `bundled` and `load` take.
  6. `designs/WO-002-design.md` (Reviewed, planned):
     - §2, the seam table, every row. Its signatures, the `Rule` names you assert, and the "Attribution" and `PLACEMENT` lines of `PuzzleParser.parse` are what your tests call and expect.
     - §3, every bullet except "Tools fix".
     - §4 "Kit staging".
  7. `reviews/WO-002-design-review.md`: "What was re-derived" rows R2, R3, R6, R7 and R8, and findings F1 and F2.
  8. `decisions.md` lines 43–50, the design's rows: fail fast (45), congruence (46), the `rot`/`flip`/`at` form (47), `reviewedByHuman` (48), "well-formed picture" (49) and tolerances (50).
  9. The format, used to craft and edit texts (DI-5):
     - `Spec/03-puzzle-format.md` §1–§3.
     - §4 only as the legend of the V-numbers REQ-038 cites. Where it differs from the CHG draft, the draft's written rules are what you test.
     - `Tangrams/puzzle.schema.json` (locked).
     - The 13 `Tangrams/*.json`, as edit bases and transcription sources.
  10. In-tree tests and build files, to check for collisions:
      - `content/src/test/kotlin/app/tangramplaytime/content/ContentScaffoldTest.kt`. After TASK-004 it also holds an untokened bundle check. It is not yours: never edit it, and do not reuse its class name.
      - `contracts/src/test/kotlin/app/tangramplaytime/contracts/ContractsSanityTest.kt`.
      - `content/build.gradle.kts`: JUnit 4 and `kotlinx-serialization-json` are on the test classpath; `testImplementation(project(":kernel"))` arrives with TASK-005.
      - `kernel/src/test/kotlin/app/tangramplaytime/kernel/KitFixtures.kt`. It is kernel-only and not on `:content`'s test classpath. Read it only for the pattern (`kit`-prefixed names, `kitR2`); never import it.
  11. Rules:
      - `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2: a token is a coverage claim, and prose writes a line without the dot (`REQ-038 A2`).
      - `architecture.md` §2 G-12 and G-15, §3b O-02, and §5 "Allowed dependencies".
- **Do not read:**
  - anything under `content/src/main/`. Those are implementation bodies; your signatures come from the seam table. Compiler messages are fine to read.
  - `kernel/src/main/` bodies;
  - § TASK-004, § TASK-005 and § TASK-007 of this file;
  - `tools/*.py` and `tools/tests/`. The Python validator is evidence of a check, never the rule (WO § Constraints, DI-5).
  - DI-1 (`tools/prototype_template.html`, `Spec/prototype/`);
  - `progress.md`, chat history, and anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:**
  - `Tangrams/*.json`, read through `BundledPuzzles.read()`, plus the Tangrams folder listing if you check that the bundle is complete;
  - `LockEngine()` from `:kernel`, on the test classpath.
  - No other file outside `content/` is read at run time.
- **Oracle rule:**
  - Every assertion cites, in a comment, the sentence it checks: a REQ sentence, a KDoc line, a seam row or a `decisions.md` line. Assert only what is mandated.
  - A REQ token goes only on a test whose assertion is that line's meaning.
  - A pin of a decision carries `// decision decisions.md line NN` and no token. A pin of a seam cites the seam row and carries no token.
  - You never read implementation bodies, the implementer briefs or the prototype.
- **Run:**
  - **Now, beside TASK-004:**
    1. Draft `KitTexts.kt`, `PuzzleLibraryKitTest.kt` and `PuzzleLoaderKitTest.kt` in your scratchpad, never under `content/`.
    2. Hand back the draft paths.
  - **When you are resumed with "TASK-004 done":** write the three files into the kit folder. That landing is your Done-check, met at landing:
    1. the three files are in the kit folder;
    2. a grep for `REQ-0` shows exactly the ten tokens on the tests the row names, and none in `KitTexts.kt`;
    3. the only compile errors of `:content:testDebugUnitTest` are unresolved references to TASK-005's seams.
    - From then until TASK-005 lands, the content test set does not compile (the red window). The green verdict on the kit is TASK-005's Done-check, not yours.
  - **Compile against the tree** with `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat :content:testDebugUnitTest"`.
    - Expected, and not your defect: unresolved references to TASK-005's seams (`PuzzleLoader`, `LoadResult`, `Violation`, `Rule`, `PuzzleLibraries`, and `LockEngine` until the `:kernel` test dependency lands), and red tests before TASK-005 lands.
    - Any other compile error is yours to fix.
  - **Before each hand-back,** Grep the kit for `REQ-0`.
    - Exactly the ten in-scope tokens may appear, each on its asserting test.
    - `KitTexts.kt` carries none.
    - None of REQ-038 A2, REQ-039 A1/A2, REQ-042 A1 or REQ-045 A2 appears with a dot.
- **Done means:**
  - **Files.**
    - `KitTexts.kt` holds the crafted texts and the helpers. Each helper name carries a `kit` prefix, so it cannot collide with an `internal` declaration of the main source set in the same package.
    - Each crafted text names, in a comment, the shipped file it was edited from (or "hand-made").
  - **Where the tokens go.** If your judgment moves a token between the files, say so in the hand-back; the orchestrator then corrects the Done-check cells.
    - `PuzzleLibraryKitTest.kt`, through `PuzzleLibraries.bundled(LockEngine())`: REQ-007.A1, REQ-007.A2, REQ-040.A1, REQ-041.A1, REQ-041.A2, REQ-042.A2, REQ-042.A3, REQ-045.A1.
    - `PuzzleLoaderKitTest.kt`, through `PuzzleLoader.load(sources, LockEngine())`: REQ-038.A1, REQ-040.A2, REQ-041.A2.
  - **REQ-038.A1** (WO § Scope, last note). Test it as the written rules of the pending CHG:
    - the shipped texts load as `Loaded` with no violation;
    - for each rule the draft lists, one crafted or edited text is `Rejected` with that `Rule` for that file.
    - The tests go green, and the line still stays `implemented (unverified)` until the capture side signs the draft. The token still goes on the asserting tests.
  - **Fixtures must discriminate.**
    - Each crafted text breaks exactly one rule. The test asserts that this rule is reported for that file and that no other rule is: a text that trips two rules cannot show which one the check enforces.
    - A crafted text's `id` equals its `PuzzleSource.fileName` stem, unless `ID` is the rule under test.
    - At least these discriminators are needed:
      - **Corner-only join** (REQ-038 rule "a join at a single corner point does not count as connected"): pieces that touch only at a corner → `CONNECTED`.
      - **REQ-040.A2:** a full puzzle of rating 1 whose id sorts before "shapes-warmup" (and before "shapes-mini"), loaded with the shipped texts. An order by rating and id that ignores the kind must fail this test; the shipped bundle alone cannot tell the two orders apart.
      - **REQ-041.A2, both sides of the edge** (design §3 "Warm-ups sit exactly on the line"; review R6):
        - one warm-up with a piece below the half-outline measure → `WARMUP`. Derive its exposure by hand and show the arithmetic in a comment.
        - the shipped warm-ups, each with a piece at exactly 0.5, are accepted.
      - **Congruence** (design §3 "Congruence is stricter than Python V3"; `decisions.md` line 46):
        - a piece's own corners in a non-boundary order (a bow-tie) → `CONGRUENT`;
        - a wrong-size piece → `CONGRUENT`.
      - **Overlap:** two pieces that overlap by area → `OVERLAP`.
      - **Piece set:**
        - a mini with 1–6 distinct pieces is accepted;
        - a mini that repeats a piece → `PIECE_SET`;
        - a non-mini with six pieces → `PIECE_SET`.
  - **Review F1** (`decisions.md` line 47; design §2 `PuzzleParser.parse` and §3 "Format narrowing").
    - A solution entry in the `piece` + `rot`/`flip`/`at` form → `Rejected` with `PLACEMENT`, never `JSON`.
    - Tag the test `// decision decisions.md line 47`, with no token.
  - **Fail fast** (`decisions.md` line 45).
    - The shipped texts plus one bad text through `load` → `Rejected`, naming that file, with no library.
    - Do not fake the bundle to drive `bundled`'s throw.
  - **The "Must never" of `IPuzzleLibrary`** (cite the KDoc; no REQ token):
    - `puzzles` cannot be modified, and two reads are equal;
    - `byId` of an absent id is `null`, never an exception (G-15).
  - **REQ-007.A1.**
    - `bundled(LockEngine())` returns. Because loading fails fast, that means every bundled file passed every rule.
    - It lists one puzzle per puzzle file in the Tangrams folder (the schema excluded).
    - The test's comment says where the rules' discrimination is proven: in `PuzzleLoaderKitTest.kt`.
    - If you also measure something independently, and use `Edges` or `ValidityCheck` as the oracle, first pin that seam with hand-computed values. Those pins carry no token and cite the seam row.
  - **As locked** (WO § Constraints "Pending deltas"):
    - REQ-042.A3 is asserted as `byId("shapes-square")` being listed.
    - REQ-042.A2 is asserted as at least four distinct categories among the listed puzzles.
  - **Hygiene.**
    - Call `internal` members directly, with no reflection. Use JUnit 4 only and add no dependency.
    - Never edit `content/src/main/` or `ContentScaffoldTest.kt`.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the test names for each acceptance ID, and the list of decision and KDoc pins;
  - the files outside `content/` that tests read at run time;
  - the compile status, with the command and its exit code;
  - how many corrected checks there have been so far.

---

## Brief — TASK-004 · WO-002 · slice-implementer

- **Mission:** Ship the puzzle files inside `:content` as Java resources, and read them through one `ClassLoader` path that serves both the JVM unit test and the APK. This is the design's highest-risk part, so it goes first. The Gradle/AGP 9.4 wiring is judgment-heavy, so the task runs on Opus.
- **Read exactly:**
  1. `tasks.md` § WO-002, row TASK-004.
  2. `Requirements/reqs/REQ-007.md`, the rule that puzzles are held in their own folder.
  3. `designs/WO-002-design.md` (Reviewed, planned):
     - §2, the rows `bundlePuzzles` and `BundledPuzzles.read`. Implement the signatures exactly as written (`PuzzleSource`, `BundledPuzzles.DIR`, `read()`), because TASK-005 and the kit call them.
     - §3, these bullets: "Highest-risk part: the reading path" with all its sub-bullets (AGP facts, Proof including the release evidence, Fallback, On device), and "Files and coordinates".
  4. `reviews/WO-002-design-review.md`: R1 (the wiring, proven in a scratch build) and F3.
  5. `decisions.md` line 43 (Java resources).
  6. `architecture.md` §2 G-05, G-12 and G-15; §3b O-02; §5 "Allowed dependencies"; §6 ADR-004.
  7. `platform-notes/android.md`, the section "Gradle from Git Bash on Windows".
  8. Existing files:
     - `content/build.gradle.kts`, the root `build.gradle.kts`, `settings.gradle.kts` and `gradle/libs.versions.toml`;
     - `app/build.gradle.kts`, read-only (`:app` already depends on `:content`);
     - `content/src/main/kotlin/app/tangramplaytime/content/Content.kt`. Leave it; TASK-005 deletes it.
     - `content/src/test/kotlin/app/tangramplaytime/content/ContentScaffoldTest.kt`.
  9. Design input DI-5: the `Tangrams/` folder (the top-level files, `README.md`, `previews/`). Read it only to know what ships and what must not. Never edit it.
- **Do not read:** § TASK-005, § TASK-007 and § TASK-008 of this file; any kit draft; the design reviewer's scratchpad probe; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json` (13 files), read through the generated resources. The bundle check also lists the Tangrams folder.
- **Run:**
  - `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat :content:testDebugUnitTest :app:assembleDebug :app:assembleRelease :content:bundleReleaseAar"`. The test results are under `content/build/test-results/testDebugUnitTest/`.
  - The three listings. Each must show `index.txt` and the 13 puzzle files under `app/tangramplaytime/content/puzzles/`, 14 entries:
    - `unzip -l app/build/outputs/apk/debug/app-debug.apk | grep app/tangramplaytime/content/puzzles/`
    - `unzip -l app/build/outputs/apk/release/app-release-unsigned.apk | grep app/tangramplaytime/content/puzzles/`
    - `unzip -o -q content/build/outputs/aar/content-release.aar classes.jar -d <your scratchpad>/aar && unzip -l <your scratchpad>/aar/classes.jar | grep app/tangramplaytime/content/puzzles/`
  - Grep `content/src/main` for `File(` and `Tangrams`. Main code never reads `../Tangrams` from the file system (design §3 "Fallback").
- **Done means:**
  - **The Gradle task.** `bundlePuzzles` follows its seam row:
    - its inputs are the top-level `*.json` of `../Tangrams` without `*.schema.json`, and its output is `build/generated/puzzles`, both declared to Gradle;
    - it writes the copies and `index.txt` (sorted names) under `DIR`;
    - it is wired through `androidComponents.onVariants { it.sources.resources?.addGeneratedSourceDirectory(…) }`.
    - Use the §3 fallback (the unit-test variant's resources) only if the test classpath misses the directory, and say whether you used it.
  - **The reader.** New `content/src/main/kotlin/app/tangramplaytime/content/BundledPuzzles.kt` holds `internal data class PuzzleSource` and `internal object BundledPuzzles`, exactly as the seam row gives them.
    - Text is decoded as UTF-8.
    - A missing resource throws `IllegalStateException`, because it is a build defect (G-15).
  - **The scaffolding check.** `ContentScaffoldTest.kt` gains one test, commented `// scaffolding`, with no REQ token. It checks that:
    - `BundledPuzzles.read()` returns one source per top-level `Tangrams/*.json` except the schema. Compare with the folder listing, not a literal 13, so the later content batch WO does not break it.
    - every text is non-empty;
    - "Lämmittely" decodes correctly.
    - Keep the existing test.
  - **Files you do not change.** Add no dependency (D5). Do not edit `Tangrams/`, the schema, `:app` or `:contracts`.
  - **Done-check:** as the TASK-004 row says. No REQ ID is green at this step; the task is REQ-007 A1 groundwork.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the inputs read, with their versions, and the files changed;
  - the command run, its exit code, the result XML path and the test counts;
  - the three listings, with the 14 lines of each pasted, for the run log.

---

## Brief — TASK-005 · WO-002 · slice-implementer

- **Mission:** Build the `tangram-puzzle/1` parser, the load-time validity check and the library behind `IPuzzleLibrary` in `:content`. That is every seam row the design gives TASK-005, including the geometry (`ValidityCheck`, `Edges`), which review F2 merged in. The task is judgment-heavy (the parser, the ART grammar, congruence and edge geometry) and runs on Opus.
- **Read exactly:**
  1. `tasks.md` § WO-002, row TASK-005.
  2. Requirements, collection v1.2:
     - `Requirements/reqs/REQ-007.md`, `REQ-038.md`, `REQ-040.md`, `REQ-041.md`, `REQ-042.md`, `REQ-045.md`;
     - `Requirements/reqs/REQ-039.md`, for its rules on the picture (its acceptance lines are not this WO's);
     - `Requirements/req_types.md`: TYPE-001.
  3. `capture-outbox/chg-draft-req038-validity-rules-in-contract.md`. These are the written rules the kit tests REQ-038.A1 as.
  4. Contracts, baseline 2026-10-11:
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/IPuzzleLibrary.kt`, every KDoc line;
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/Types.kt`;
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/ILockEngine.kt`: the KDoc of `vertices` and `stillValid`.
  5. `designs/WO-002-design.md` (Reviewed, planned):
     - §1.
     - §2, every row except `outline_corners`. `bundlePuzzles` and `BundledPuzzles.read` are TASK-004's: the reader is the API you call. Read the "Attribution" and `PLACEMENT` lines of `PuzzleParser.parse` closely, because the kit asserts which rule rejected a text.
     - §3, every bullet except "Tools fix".
     - §4.
  6. `reviews/WO-002-design-review.md`:
     - R2, R3, R4, R6 and R7;
     - F1, now written into design §2 and §3;
     - F2, the reason the geometry is yours;
     - F4: no `:app` change.
  7. `decisions.md` lines 44–50.
  8. `architecture.md` §2 G-12 and G-15; §3b O-01 and O-02; §5 "Allowed dependencies"; §6 ADR-004.
  9. `design-inputs.md` §2 item 9: both titles stay mandatory.
  10. Design input DI-5. It is a behaviour reference only: never DoD. Where it differs, the design and the Contract win; the `rot`/`flip`/`at` form and V3's vertex-set equality are the two known differences.
      - `Spec/03-puzzle-format.md`;
      - `Tangrams/puzzle.schema.json` (locked);
      - `Tangrams/*.json`;
      - `tools/validate_puzzles.py` and `tools/tangram_geom.py`.
  11. TASK-004's landed files:
      - `content/src/main/kotlin/app/tangramplaytime/content/BundledPuzzles.kt`. Call it; do not change the reader.
      - `content/build.gradle.kts`. Extend it.
  12. The visible acceptance tests, from TASK-008. Read them; never edit them.
      - `content/src/test/kotlin/app/tangramplaytime/content/KitTexts.kt`
      - `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLibraryKitTest.kt`
      - `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLoaderKitTest.kt`
  13. Existing files: `content/src/main/kotlin/app/tangramplaytime/content/Content.kt` (to delete) and `content/src/test/kotlin/app/tangramplaytime/content/ContentScaffoldTest.kt` (keep it green).
- **Do not read:**
  - § TASK-004, § TASK-007 and § TASK-008 of this file; kit drafts outside the tree;
  - `kernel/src/main/` bodies. You consume `ILockEngine` through its KDoc and review R2/R3 (G-12).
  - DI-1, the prototype;
  - `progress.md`, chat history, and anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json` through the generated resources, and `LockEngine()` from `:kernel` on the test classpath.
- **Run:**
  - `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat :contracts:test :content:testDebugUnitTest"`. The results are under `*/build/test-results/`.
  - Grep `content/src/main` for `app.tangramplaytime.kernel`. There must be no match: main code depends on `:contracts` only, and the engine is injected (G-12).
  - Grep `content/src/main` for declarations that are neither `internal` nor `private`. Only `PuzzleLibraries` may be public (the KDoc "must never" bars any parsing type across the seam).
- **Done means:**
  - **Files.** New files under `content/src/main/kotlin/app/tangramplaytime/content/`:
    - `PuzzleDtos.kt`, `PuzzleParser.kt`;
    - `PuzzleLoader.kt`, which also holds `LoadResult`, `Violation` and `Rule`;
    - `ValidityCheck.kt`, `Edges.kt`;
    - `PuzzleLibrary.kt`, `PuzzleLibraries.kt`.
    - Each signature matches its seam row exactly, because the kit calls them.
  - **Review F1.** The DTO declares `rot`, `flip` and `at` as optional fields.
    - An entry that carries any of them, or has no `polygon`, is a `PLACEMENT` violation whose detail names the unsupported form.
    - Rule attribution follows the "Attribution" line of `PuzzleParser.parse`. `JSON` is only for what that line lists.
  - **Tolerances** follow `decisions.md` line 50 and design §3: 1e-6, and an exposure of at least 0.5 − 1e-6 for the warm-up measure.
  - **Build file.** Add `testImplementation(project(":kernel"))` to `content/build.gradle.kts`. Main dependencies stay unchanged (D5).
  - **The placeholder.** `Content.kt` is deleted. If the guard denies the delete, leave the file and say so.
  - **Files you do not change:**
    - `:app`. Its wiring of `bundled` is WO-003's (F4).
    - `Tangrams/`, the schema, `:contracts`, `:kernel` and the TASK-004 reader;
    - the kit.
    - A needed contract change is a `contract-delta`: hand it back, never edit it.
  - **Done-check.** As the TASK-005 row says: both kit test files green with their IDs, `ContentScaffoldTest.kt` green and `:contracts:test` green.
  - **Your own tests.** Scaffolding tests are welcome. They start with `// scaffolding`, carry no `REQ-NNN.A<n>` token, and live outside the kit's file names, for example under `content/src/test/kotlin/app/tangramplaytime/content/scaffolding/`.
  - **The kit is read-only for you.** If a kit line looks wrong, name the test in your hand-back.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the inputs read, with their versions, and the files changed or deleted;
  - the traceability delta;
  - the command run, its exit code, the result XML paths and the test counts for each module.

---

## Brief — TASK-007 · WO-002 · slice-implementer

- **Mission:** Re-judge the authoring tool's corner rule. `tools/tangram_geom.outline_corners` should classify a vertex by whether the covered directions are contiguous, as the kernel does, so that warmup-4 (2,2) becomes a corner and the V11 build-order line agrees with the kernel. The task is a port of a specified rule with a crisp check, so it runs on Sonnet.
- **Read exactly:**
  1. `tasks.md` § WO-002, row TASK-007.
  2. `workorders/WO-002.md` § Constraints & notes: the "The `tools/` fix" bullet.
  3. `designs/WO-002-design.md`: §2 row `outline_corners`, and §3 "Tools fix".
  4. `reviews/WO-002-design-review.md`: R5.
  5. `design-inputs.md` §2 item 13.
  6. The rule:
     - `designs/WO-001-design.md` §2, row `Silhouette`;
     - `reviews/WO-001-design-review.md` F1.
  7. The rule as built, read-only: `kernel/src/main/kotlin/app/tangramplaytime/kernel/Silhouette.kt`, its corner classification.
  8. `decisions.md` line 51: the prototype and the UI sheets are not regenerated.
  9. The tool and its data:
     - `tools/tangram_geom.py`, all of it;
     - `tools/validate_puzzles.py`, for how V11 uses `outline_corners`;
     - `Tangrams/*.json`, read-only.
  10. The header of `tools/tests/test_prototype.py`, for the folder's conventions only. Do not run it; it needs Playwright.
- **Do not read:** § TASK-004, § TASK-005 and § TASK-008 of this file; anything under `content/`; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json` (13 files, read-only).
- **Run:** `cd /c/GitHub/AI/TangramPlaytime && python tools/validate_puzzles.py Tangrams && python tools/tests/test_tangram_geom.py`.
- **Done means:**
  - **The rule.** `outline_corners` keeps its signature and classifies by contiguity, as the seam row says. Points stay exact `Q2`, as before.
  - **Pre-fix sets.** Before you edit, capture the pre-fix corner set of every file. Keep them either as literals in the test or as a test-local copy of the angle-sum rule, and say which.
  - **The tool test.** New `tools/tests/test_tangram_geom.py`:
    - plain asserts, stdlib only;
    - it runs directly (`if __name__ == "__main__":`) and exits 0 on a pass.
    - It asserts that warmup-4's corners are the pre-fix set plus exactly (2,2), and that every other file's corners equal its pre-fix set.
  - **The validator.** It still reports 13/13 valid, with a complete build order for each puzzle. Compare against run log row `py0`.
  - **Files you do not change.**
    - `validate_puzzles.py`: no change is expected. If one is needed, stop and hand back `recirculate → planner`.
    - `Tangrams/`.
    - Do not run `build_prototype.py` or `make_ui_sheets.py`, and leave `Spec/prototype/` and `Spec/ui/` untouched (`decisions.md` line 51).
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the files changed;
  - the validator's summary line and the test's exit code;
  - the warmup-4 corner difference, before and after.

---

## Handoff — Work-Order Planner · WO-002

- **Scope:** REQ-007 (A1–A2), REQ-038 (A1), REQ-040 (A1–A2), REQ-041 (A1–A2), REQ-042 (A2–A3), REQ-045 (A1), plus the `tools/` corner-rule fix. Governed interfaces: `IPuzzleLibrary` and the format (implements, unchanged); `ILockEngine` and `Types.kt` (consumes).
- **Inputs read:**
  - `workorders/WO-002.md` (log to 14:28);
  - `designs/WO-002-design.md` (Reviewed);
  - `reviews/WO-002-design-review.md`;
  - REQ-007, REQ-038, REQ-040, REQ-041, REQ-042 and REQ-045 (collection v1.2);
  - `IPuzzleLibrary.kt`, `Types.kt` and `ILockEngine.kt` (baseline 2026-10-11);
  - `build-map.md` (change log 1.2); `design-inputs.md` §2 items 9, 10 and 13;
  - the test tree of `content/`, `kernel/` and `contracts/`; the schema's top level;
  - SWDev `task-decomposition.md` v0.5, `templates/tasks.md`, `traceability-rules.md` §2, and the `plan-check` SKILL and script.
- **Result:** `tasks.md` § WO-002 (change log 0.4; plan-review fixes 0.5); this file; write-backs in `designs/WO-002-design.md`.
- **What happened to each design-review finding:**
  - **F1:** written into design §2 `PuzzleParser.parse` and §3 "Format narrowing". TASK-005 implements it; TASK-008 pins it as a decision test.
  - **F2:** merged. The geometry is in TASK-005 and the kit lands in one stage.
  - **F3:** the release AAR and the release APK listings are part of TASK-004's Done-check and the channel table.
  - **F4:** design §1 step 2 and §4 now name WO-003; TASK-005's brief bars any `:app` change.
