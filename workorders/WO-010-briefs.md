# WO-010 — dispatch briefs (content batch: the TangramCollections figures)

**Planner:** wo-planner (Opus 5.5)  ·  **Date:** 2026-10-18  ·  **Plan:** `tasks.md` § WO-010 (TASK-033 … TASK-042; TASK-037 ∥ TASK-038 ∥ TASK-039 ∥ TASK-040 are `[P]`)
**Mode:** Light (`workorders/WO-010.md` § Mode). There is no design. § WO-010 › Seams in `tasks.md` is the design: S-38 … S-43 and S-11Δ2, the shared-file table, the ten questions settled, the Q7 test table and the Q9 coverage table.

> Each brief is dispatched on its own. The Acceptance Test Author gets only its own briefs (TASK-033, TASK-041) and the two tables below. It never sees an implementer brief. An implementer never sees another implementer's brief or a kit draft outside the tree.
>
> Every path is relative to `C:/GitHub/AI/TangramPlaytime/` unless it is absolute. Folder names used below:
> - "the content kit folder": `content/src/test/kotlin/app/tangramplaytime/content/`;
> - "the JVM kit folder": `play/src/test/kotlin/app/tangramplaytime/play/`;
> - "the play device kit folder": `play/src/androidTest/kotlin/app/tangramplaytime/play/`;
> - "the app device kit folder": `app/src/androidTest/kotlin/app/tangramplaytime/app/`;
> - "your scratch": the folder the orchestrator names at dispatch, `<scratch>/wo010/task-0NN/`. PNG copies, reports and copied XMLs live there, never in the repo.
>
> `decisions.md` rows are cited by date and bold title, and rows are only ever appended. Already in force (2026-10-18): "**Scope of WO-010 …**", "**The figures' source and licence are credited in `ASSET-LICENCES.md`, not in the puzzle file**" and "**`architecture.md` v1.2 signed: G-16 and O-06 amended**". **The WO-010 rows** are the ones the orchestrator appends before dispatch for the planner's readings. These briefs cite them by the titles the planner proposed:
> - R1 "**How a TangramCollections figure becomes a puzzle file**";
> - R2 "**What passes, and what counts as the same figure**";
> - R3 "**The imported files' fields: kind, category, id, titles, rating**";
> - R4 "**Sheets, PNG copies and the prototype in WO-010**";
> - R5 "**`ASSET-LICENCES.md` lists every puzzle, kept complete by a JVM pin**";
> - R6 "**The start-up and grid thresholds, and the designs if crossed**";
> - R7 "**The tests that pinned the 13-puzzle bundle**";
> - R8 "**REQ-039.A1's oracle over the imported pictures**";
> - R9 "**The counter keeps ASCII digits under every device language**";
> - R10 "**WO-010's device channels**".
>
> **Checkpoint items C1** (turned copies ship as separate puzzles — the default) **and C2** (the measured rating rule — the default) are asked on the G3 surface. The build follows the defaults. An answer that arrives before TASK-036 runs costs nothing. After the batches, C1's other answer costs at most the six turned copies' pictures, and C2's other answer costs only `difficulty` fields. If the orchestrator changes a title or rules a reading the other way, it corrects the citations and the affected brief lines in the same edit.

## Dispatch order (two lanes, one fan-out)

| Step | Test lane (acceptance-test-author) | Implementer lane (slice-implementer) | Gate to the next step |
|---|---|---|---|
| 0 | — | — (orchestrator) Append R1–R10. Ask C1 and C2. Extend the run log's tree definitions (`tasks.md` § WO-010 seam note 3). Prove the eyes channel: one committed sheet to a PNG by headless Chrome, read back (the channel table) | The rows exist; the eyes channel is proved |
| 1 | TASK-033 **lands** stage 1. Then it may draft stage 2 (TASK-041) in its scratch | — | TASK-033 met at landing (`CounterDigitsKitTest` is the only red) |
| 2 | — | TASK-034 | `CounterDigitsKitTest` green; the JVM line green |
| 3 | — | TASK-035 (device; floor, then dev, then tablet) | The baseline numbers are in the hand-back |
| 4 | — | TASK-036 (tools, data, shared files; a floor step) | TASK-036's Done-check green; no Q6 threshold crossed (a crossing → `recirculate → planner`) |
| 5 | Stage 2 keeps drafting | **TASK-037 ∥ TASK-038 ∥ TASK-039 ∥ TASK-040**, four implementers at once. Python tools and headless Chrome only, no Gradle | Each Done-check green, and the orchestrator has opened every PNG of the batch before accepting it |
| 6 | TASK-041 **lands** (resume the TASK-033 author with "the four batches are done") | — | TASK-041 met at landing |
| 7 | — | TASK-042 (every channel on the final tree: floor first) | TASK-042's Done-check green |
| 8 | Run `plan_check.py --wo WO-010` again → Code Reviewer → Test & Verify (from the run log; it re-runs only what changed) | | |

- **Why this order.**
  - **The kit first.** The REQ-040.A2 correction must be in the tree before TASK-036 adds full puzzles of rating 1, and the counter pin before its fix.
  - **TASK-034 is a one-line fix.** It goes early so that the counter pin is not red across the WO.
  - **TASK-035 measures the 13-puzzle baseline** before the bundle grows.
  - **TASK-036 is the risk:** the conversion, both validity checks, the duplicates and an early floor run on the real geometry, before any picture is drawn.
  - **The batches fan out.** No shared file, no shared declared input, no Gradle.
  - **TASK-041** lands after the files it reads. **TASK-042** runs the whole sets once, on the final tree (`platform-notes/android.md`, the WO-006 lesson).
- **The flux window** (steps 4–6). The bundle's pictures are half drawn, so nobody runs the JVM line or a device set for a verdict until TASK-041.
- **Compile errors in the kit** are the kit author's defect and count as a corrected check. An implementer never edits a kit file.
- **Scaffolding tests** belong to the implementers. Whoever's change breaks one updates it and says so.
- **Decisions the implementers record.** A number or reading that no REQ, seam row or `decisions.md` row fixes goes back as a proposed `decisions.md` row, with its source and the alternative. The orchestrator appends it, so each is recorded once.
- **A red test on an imported puzzle** (TASK-042, or the review) goes back to that puzzle's author. A picture goes to its batch; every other field goes to TASK-036.

## Verification channels (G3)

"The line" below is `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat <tasks>"`. `ADB` is `/c/Users/Jami/AppData/Local/Android/Sdk/platform-tools/adb`; `EMU` is `/c/Users/Jami/AppData/Local/Android/Sdk/emulator/emulator`; `CHROME` is `"/c/Program Files/Google/Chrome/Application/chrome.exe"`. Python runs with `PYTHONUTF8=1` (the cp1252 console). **Every adb, emulator and Gradle device call is wrapped in `timeout`** (the WO log's interim duty). The bounds are: a single class 3600 s; `ScaleMeasureTest` 900 s; the whole app set 2 h; **the whole play set 5 h** (about 2–2.5 h is expected at about 106 puzzles: `tasks.md` § WO-010, Q7).

| Channel | How (Git Bash) | Result | Needed by |
|---|---|---|---|
| Python tool channel | `PYTHONUTF8=1 python tools/validate_puzzles.py Tangrams` (or the listed files). The whole-folder render goes to your scratch: `PYTHONUTF8=1 python tools/render_puzzle.py Tangrams <your scratch>/previews-all`. An imported file's sheet: `PYTHONUTF8=1 python tools/render_puzzle.py Tangrams/<id>.json Tangrams/previews`, one file per invocation; the 13 committed sheets are never re-rendered (R4). The tool tests: `python tools/tests/test_tangram_geom.py`, `python tools/tests/test_import_figures.py`. The colour report: `python tools/import_figures.py colours <file> …` | the exit code and the printed lines | TASK-036 … TASK-040, TASK-042 |
| Eyes (the AI's look at a sheet; owner rule 7) | `timeout 60 $CHROME --headless=new --disable-gpu --hide-scrollbars --window-size=665,290 --screenshot="<your scratch, as C:/…>/png/<id>.png" "file:///C:/GitHub/AI/TangramPlaytime/Tangrams/previews/<id>.svg"`, then open the PNG with the Read tool. `tools/svg2png.py` needs Playwright, which is not installed: do not install it. PNGs are never committed (R4) | the PNG, opened | TASK-036 (silhouettes, from the scratch render), TASK-037 … TASK-040, the orchestrator |
| JVM (JUnit 4) | **Two runs of the line, in this order** (run log g3-jvm): **(1)** `:app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun`, **then (2)** `:content:testDebugUnitTest --rerun :play:testDebugUnitTest --rerun :kernel:test --rerun :contracts:test --rerun :store:testDebugUnitTest --rerun :app:testDebugUnitTest --rerun`. A run (2) without run (1) first is not a verdict (the WO-005 lesson) | `<module>/build/test-results/…/TEST-*.xml`, stamped after run (2) started | TASK-033, TASK-034, TASK-036, TASK-041, TASK-042; Test & Verify |
| Kit compile | The line with `:app:assembleDebugAndroidTest :play:assembleDebugAndroidTest` | the exit code and the compiler messages | TASK-035, TASK-042 |
| Floor image `Phone_API_26` (`minSdk 26`; 411 × 914 dp) — **always first** | One emulator at a time: `$ADB emu kill`, then `$ADB devices` until it is empty. Boot detached: `nohup $EMU -avd Phone_API_26 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot > <your scratch>/emu.log 2>&1 &`. Judge the boot by "Boot completed" in that log and by `$ADB shell getprop sys.boot_completed`, then run `$ADB devices` and confirm the serial is `emulator-5554` (`platform-notes/android.md`: a second instance can come up as 5556). A single class: the line with `:play:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<fqcn>` (or `:app:…`), **one class per invocation** (through `cmd.exe //c`, a comma list runs only its first class). **The whole play set:** `:play:connectedDebugAndroidTest`. **The whole app set** is two invocations, as since WO-006: `…class=app.tangramplaytime.app.DevPromptKeyboardKitTest` alone, then the rest with `-Pandroid.testInstrumentationRunnerArguments.notClass=app.tangramplaytime.app.DevPromptKeyboardKitTest`. `ScaleMeasureTest` runs inside the rest too: its numbers there are not a record. Copy every XML you cite to your scratch (the folder is overwritten). Judge a run by its XML: it exists, is stamped after the command started and carries the expected counts. Never judge by Gradle's exit code. WO-006's floor root-route steps (a)–(e) are not WO-010 channels (R10) | `play/build/outputs/androidTest-results/connected/debug/TEST-*.xml`, `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` | TASK-035, TASK-036, TASK-042; Test & Verify |
| Dev image `Medium_Phone_API_37.0` (411 × 914 dp; gesture navigation) | As the floor, with `-avd Medium_Phone_API_37.0` | the same folders | TASK-035, TASK-042; Test & Verify |
| Tablet image `Medium_Tablet_API_37.0` (1280 × 800 dp, landscape) | As the floor, with `-avd Medium_Tablet_API_37.0`. The app set and `ScaleMeasureTest` only: the play set hosts `PlayScreen` with the kit's phone literal (WO-006 practice) | the app folder | TASK-035, TASK-042; Test & Verify |
| The scale record (S-43; Q6) | On each image, after a reset: build the APKs once (JVM run (1)). **(a) Three runs** of the line with `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.app.ScaleMeasureTest`; each invocation is a fresh process, with a reset and a clean `--check` between. Take the `measure:` lines from the XML's system-out or from `$ADB logcat -d -s ScaleMeasure` (whichever tag the class uses). **(b) Three cold starts:** `$ADB install -r app/build/outputs/apk/debug/app-debug.apk`; then, three times, `bash tools/device_reset.sh` (clears the app), `$ADB shell am force-stop app.tangramplaytime`, `$ADB shell am start -W -n app.tangramplaytime/app.tangramplaytime.app.MainActivity` (record TotalTime). Then `$ADB uninstall app.tangramplaytime` and a clean `--check` | a table per image: launch-to-first-puzzle, grid open, scroll frames (janky share, p90, p99), TotalTime — each a median of three, with the three values | TASK-035 (baseline), TASK-036 (floor, early), TASK-042 (record) |
| Release verifier V-01 (G-01, G-08) | After run (1) of the JVM line: `python .swdev/verifiers/v01_release_apk.py`, `python .swdev/verifiers/v01_release_apk.py --positive-control`, then `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-010` | PASS/FAIL lines, GREEN/RED and the budget line (expected: 0 verifiers, 0 lines for WO-010) | TASK-042, the close |
| Device hygiene | `bash tools/device_reset.sh`, then `bash tools/device_reset.sh --check` (it must be clean), **before and after** every device step. On the floor, also record `$ADB shell dumpsys window \| grep mCurrentFocus`. A non-clean check after a reset voids the step. After a stopped or killed floor run, reboot the floor instead of trusting the reset (`platform-notes/android.md` § Device hygiene) | `--check` clean or not | every device step |

- **Not a DoD channel:** human eyes. The owner's review of the pictures is REQ-039 rule 2's (WO-009); the owner's play-test is a `review-notes.md` row the orchestrator writes at close.
- **Browser:** headless Chrome as the PNG renderer only. `tools/tests/test_prototype.py` is not run, and the prototype and the UI sheets are not regenerated (R4).
- **Toolchain proof:** g3-py, g3-jvm, g3-v01, g3-dev, g3-floor and g3-tablet in `.swdev/run-log/WO-010.md`; the eyes channel at step 0.
- **Time:** WO-010 computes no time value in product code (G-14). The measurement reads `SystemClock.elapsedRealtime` in test code only. Nothing sleeps to wait for a state: wait on an observed state with a bound.

---

## Brief — TASK-033 · WO-010 · acceptance-test-author

- **Mission:** Stage 1 of WO-010's one acceptance kit, landed before any implementer starts:
  1. Correct the content kit's assertions that pin a fact of the 13-puzzle bundle rather than a REQ sentence, starting with the last assertion of the REQ-040.A2 test in `PuzzleLoaderKitTest.kt`, so that the kit holds when about 93 imported puzzles arrive.
  2. Write the new `play/src/test/kotlin/app/tangramplaytime/play/CounterDigitsKitTest.kt`: S-11Δ2's pin, no token.

  The class is JVM oracles, so it goes to Opus.
- **Read exactly:**
  1. `tasks.md` § WO-010: the intro (lanes, windows, seam notes, "In-tree tests this WO changes", "Not covered here"); § Seams: S-39 (what an imported file carries: `kind` full, ratings 1–5, the id rule's `kit` exclusion), S-11Δ2, the ten questions (3, 7, 9, 10) and the Q7 and Q9 tables; the rows TASK-033 (yours), TASK-034 (its Done-check is your counter pin's green verdict) and TASK-036 (what it adds to the bundle).
  2. `workorders/WO-010.md`: § Scope with its notes; § Constraints & notes, questions 3, 7, 9 and 10; the standing constraints.
  3. Requirements, collection v1.2, read-only: `Requirements/reqs/REQ-040.md` (your token's line); `REQ-041.md`, `REQ-045.md`, `REQ-007.md`, `REQ-038.md`, `REQ-042.md` (the other content lines your grep checks); `REQ-047.md` (the statement your pin cites); `REQ-024.md` (A2: the number in the top bar). `Requirements/req_types.md`: TYPE-001.
  4. Frozen contracts: `contracts/src/main/kotlin/app/tangramplaytime/contracts/IPuzzleLibrary.kt` (the `puzzles` KDoc: the order) and `contracts/src/main/kotlin/app/tangramplaytime/contracts/Types.kt` (`PuzzleDefinition`, `PuzzleKind`).
  5. `decisions.md`: 2026-10-18 "**Scope of WO-010 …**"; the WO-010 rows R3, R7 and R9; 2026-10-18 "**S1 recorded and carried, S2 fixed now, N1 carried …**" (the WO-006 code-review triage row, N1: the counter's digits); 2026-10-11 "**What 'congruent to its piece' means**" and "**The load check's tolerances**" (the kit's measures).
  6. In-tree, the kit you correct and extend:
     - every file in the content kit folder (`PuzzleLibraryKitTest.kt`, `PuzzleLoaderKitTest.kt`, `KitTexts.kt`; `ContentScaffoldTest.kt` and `scaffolding/` read-only: not yours);
     - in the JVM kit folder: `KitPlay.kt` (its `PlayTexts` literal and helpers; you may add a kit-prefixed helper, never change one) and `BrowsingKitTest.kt` (the counter's existing pins);
     - grep, read-only, every kit file of the play and app device kit folders and of the JVM kit folder for assertions on a count, an order, an id list, the last puzzle, the ‹ › wrap, a grid size or position, or a puzzle chosen by index (the Q7 table is the planner's sweep; yours is the one that counts). A device-kit pin you find goes into your hand-back as a finding: stage 1 edits only JVM files, and a device correction is dispatched at TASK-042 if one is needed.
  7. **Declarations only, never bodies:** `play/src/main/kotlin/app/tangramplaytime/play/PlayTexts.kt` (`data class PlayTexts`, its `counterFormat`, and `internal fun PlayTexts.counterOf(number: Int, count: Int): String`).
  8. The `counter_format` strings, read as data: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-fi/strings.xml`.
  9. Rules: `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2; `C:/GitHub/AI/SWDev/framework/methodology/task-decomposition.md` ("Assert only what is mandated", "Declared inputs land together"); `platform-notes/android.md` § Compose test kit (the oracle-citation lessons).
- **Do not read:** the bodies of the main code of every module; the implementer briefs (every brief in this file but yours and TASK-041's); `tools/import_figures.py` and its test; DI-1 … DI-4 and DI-10; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json` (the row's Inputs cell). Your kits read the bundle through `:content`. Today it holds the 13 shipped files; TASK-036 adds about 93 after you land.
- **Oracle rule:**
  - Every assertion cites, in a comment, the sentence it checks: a REQ sentence, a TYPE rule, a seam row (`S-n`) or a `decisions.md` row by date and title. No source, no assertion.
  - An assertion that holds only because of what the 13-puzzle bundle happens to contain is a defect. Correct it to the REQ sentence, or drop it. Each one you change is a corrected check: list it with the sentence it had no source in.
  - A REQ token goes only on a test whose assertion is that line's meaning; a pin carries its source and no token. Cite rules without a dotted ID in prose ("REQ-047 statement").
  - Fixtures discriminate:
    - **REQ-040.A2:** the corrected assertion must still fail for an order that puts a full rating-1 puzzle before a warm-up. Show that test-side, with a crafted list or text through the kit's own doors.
    - **The counter pin:** it needs a default locale whose digits are not ASCII on this JVM (for example `ar-EG-u-nu-arab` or `fa-IR`). Its fixture check proves `String.format` under that locale gives non-ASCII digits, so the pin can fail.
  - The pin sets the JVM default locale and restores it in `finally`: the JVM is shared by every test class of the run.
  - No `assume…`. Call `internal` members directly; no reflection.
- **Where the tokens go:** no new token. REQ-040.A2 stays on its test; the WO-001…WO-006 tokens stay unchanged.
- **What the counter pin covers** (S-11Δ2; the REQ-047 statement): with the default locale switched as above, `counterOf` on the `en` and on the `fi` `PlayTexts` gives the format's text with ASCII digits for a one-digit, a two-digit and a three-digit total (the bundle's count reaches three digits).
- **Run:**
  1. Make the corrections and write the new file. Landing is your Done-check, met at landing.
  2. Run (1), then (2), of the JVM line. Expected: the whole content kit green; `CounterDigitsKitTest` compiles, its fixture check passes and its pin fails, the only red. Any other red is yours to explain.
  3. Grep the JVM folders for `REQ-0[0-9]*\.A[0-9]` before you hand back: no new dotted ID, and the earlier tokens unchanged.
- **Done means:** the TASK-033 row's Done-check, plus hygiene: helper names carry a `kit` prefix; JUnit 4 only; no new dependency; never edit main code, a build file, a scaffolding test, `tools/` or a data file.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - each changed assertion: before, after, its source, and whether it is a corrected check;
  - your grep's findings over every kit folder, the device kits included, each with the file, the line and why it does or does not pin the 13-puzzle bundle;
  - the counter pin's locale and the fixture check's evidence;
  - the JVM run's counts.

  Stay available: TASK-041 is your stage 2, in this context.

---

## Brief — TASK-041 · WO-010 · acceptance-test-author (stage 2; TASK-033's author, context resumed)

- **Mission:** Stage 2 of the kit: new `content/src/test/kotlin/app/tangramplaytime/content/AssetLicencesKitTest.kt` (no token), with two pins:
  - **(a)** G-16 v1.2's record: `ASSET-LICENCES.md` as S-41 shapes it, complete against the Tangrams folder.
  - **(b)** REQ-039 rule 1's colour rule over the imported pictures, counted as S-42 counts.

  You may draft from TASK-033 on. You land once the orchestrator says the four picture batches (TASK-037…TASK-040) are done.
- **Read exactly** (in addition to what you read for TASK-033):
  1. `tasks.md` § WO-010 › Seams: S-41, S-42, S-39 (the stub picture), S-40; the shared-file table; question 5; the rows TASK-041 (yours) and TASK-042.
  2. `architecture.md` §2: G-05, G-16 (v1.2); ADR-005 (the system font: the record's last section).
  3. `Requirements/reqs/REQ-039.md` (the rule your colour pin cites), `REQ-007.md` (A1: why the rule binds every shipped puzzle), `REQ-010.md`.
  4. `capture-outbox/chg-draft-licence-notice-and-assets.md` (the pending asset rule G-16 follows; not Contract text yet).
  5. `decisions.md`: 2026-10-11 "**What 'a well-formed picture' means**" (why the 13 are not in the colour pin: `shapes-mini-2`); 2026-10-18 "**The figures' source and licence are credited in `ASSET-LICENCES.md`, not in the puzzle file**"; the WO-010 rows R5 and R8.
  6. Data, read at run time and read by you: `ASSET-LICENCES.md` and the Tangrams folder (`Tangrams/*.json`). They are data files, not implementation. Write your parser from S-41, then run it on the file.
  7. In-tree: `KitTexts.kt` (`kitTangramsDir()`, `kitTangramsPuzzleIds()` and the picture-check helpers; reuse them, never change them); `ContentScaffoldTest.kt` (how a content test finds `../Tangrams` from the module folder; read-only).
- **Do not read:** the same list as TASK-033, plus `tools/import_figures.py`, its test, its report, and the batch implementers' hand-backs.
- **Inputs:** `ASSET-LICENCES.md`; `Tangrams/*.json` (the row's Inputs cell).
- **What each pin checks** (pointers; S-41 and S-42 are the oracle):
  - **(a) the record.** In the `## Puzzle figures` table:
    - every top-level `Tangrams/*.json` (the schema excluded) has exactly one row, and every row's puzzle id names such a file;
    - Figure is one of `this project`, `convex-13`, `android-tangram-131`, and Licence matches it as S-41 pairs them;
    - a collection row has a source figure id and a source name, and a `this project` row has `—` in both;
    - Picture reads `made for this project` on every row.

    The `## Sources` table names both collections, each with a licence and a credit. A file missing from the table, or a table row without a file, fails with the id.
  - **(b) the colours.** For every row whose Figure is a collection (WO-010's imports), the puzzle's `art` uses 3 to 8 distinct `#RRGGBB` values, case-insensitive, among `base` and every shape's `fill` and `stroke` (S-42). The stub (S-39) has one colour, so a picture not drawn fails, naming its id. The 13 `this project` rows are outside this pin (decisions row above).
  - **Discrimination, shown test-side:** run the same parsing and counting functions on crafted texts (a record missing a row, one with an extra row, one with a wrong licence pair, a one-colour `art` and a nine-colour `art`); each must fail its check.
  - Liveness: the record has at least 13 rows, and the colour pin judges at least one imported file, or the test fails.
- **Run:** run (1), then (2), of the JVM line. `AssetLicencesKitTest` is green on the tree the batches left. If it is red, that is a finding against the record or a picture. Report the id and the cell, never edit the data; the orchestrator routes it. Then grep for `REQ-0[0-9]*\.A[0-9]`: no dotted ID in the new file.
- **Done means:** the TASK-041 row's Done-check, met at landing, with the TASK-033 hygiene.
- **Return:** the handoff block, with the two pins and their sources, the discrimination cases, the JVM counts, and any finding against the record or a picture (id, cell, sentence).

---

## Brief — TASK-034 · WO-010 · slice-implementer

- **Mission:** S-11Δ2. `counterOf` in `play/src/main/kotlin/app/tangramplaytime/play/PlayTexts.kt` formats with `java.util.Locale.ROOT`, so the counter's digits are ASCII whatever the device language. One line; the class is a mechanical product fix (Opus by the project's staffing).
- **Read exactly:**
  1. `tasks.md` § WO-010: the intro (windows) and S-11Δ2; the TASK-034 row.
  2. `Requirements/reqs/REQ-047.md` (the statement: "in English otherwise") and `REQ-024.md` (A2: the number in the top bar).
  3. `decisions.md`: 2026-10-18 "**S1 recorded and carried, S2 fixed now, N1 carried …**" (N1); the WO-010 row R9.
  4. The visible tests for your step: `play/src/test/kotlin/app/tangramplaytime/play/CounterDigitsKitTest.kt` (TASK-033's pin; never edit it) and `play/src/test/kotlin/app/tangramplaytime/play/scaffolding/ScreenSeamScaffoldTest.kt` (its "3 / 13" must stay green).
  5. `play/src/main/kotlin/app/tangramplaytime/play/PlayTexts.kt`.
- **Do not read:** the kit author's briefs (§ TASK-033, § TASK-041); other implementers' briefs; DI-1 … DI-10.
- **Inputs:** `Tangrams/*.json` (the play JVM kit reads the bundle through `:content`; nothing of yours touches it).
- **Constraints:** no signature, no string and no other file change; G-10 and O-06 unchanged (the format stays an `:app` resource).
- **Run:** run (1), then (2), of the JVM line.
- **Done means:** the TASK-034 row's Done-check.
- **Return:** the handoff block, with the diff (one line) and the JVM counts per module.

---

## Brief — TASK-035 · WO-010 · slice-implementer

- **Mission:** S-43, then the 13-puzzle baseline:
  1. Write `app/src/androidTest/kotlin/app/tangramplaytime/app/ScaleMeasureTest.kt`, a measurement class with no token that asserts only liveness.
  2. Run the scale record on the floor, dev and tablet images (in that order) on today's 13-puzzle bundle: three fresh-process runs of the class and three cold starts on each image.

  These numbers are the baseline that TASK-036's and TASK-042's measurements of the grown bundle are compared with (`tasks.md` § WO-010, question 6). The class is device-only.
- **Read exactly:**
  1. `tasks.md` § WO-010: the intro (channels), seam note 3, S-43, question 6; the TASK-035 row; § WO-004's and § WO-006's tag tables for `play-screen`, `counter`, `overview`, `overview-cell-<id>` (S-13, S-13Δ; the names are in `PlayTags.kt`).
  2. `workorders/WO-010.md` § Risks and question 6.
  3. `Requirements/reqs/REQ-045.md` (A1: the fresh start), `REQ-050.md` (the grid of every puzzle).
  4. `decisions.md`: 2026-10-15 "**The grid overview, as drawn**" (all composed); 2026-10-15 "**REQ-024.A2 and the launch window**" (the loading paper); 2026-10-17 "**A start-up load that throws ends the process**"; the WO-010 row R6.
  5. Code: `app/src/main/kotlin/app/tangramplaytime/app/MainActivity.kt` and `app/src/main/kotlin/app/tangramplaytime/app/AppGraph.kt` (where the load starts); `play/src/main/kotlin/app/tangramplaytime/play/PlayTags.kt`.
  6. The visible device tests, as patterns only: `app/src/androidTest/kotlin/app/tangramplaytime/app/AppLaunchKitTest.kt` (a launch over the real wiring); `app/src/androidTest/kotlin/app/tangramplaytime/app/PlayThroughKitTest.kt` (opening the grid, scrolling to a cell). Do not call the kit's helpers in `KitApp.kt`: the measurement stands alone.
  7. `platform-notes/android.md` (all of it); `.swdev/run-log/WO-010.md` (Env, the device rows g3-dev, g3-floor (void), g3-floor, g3-tablet).
- **Do not read:** the kit author's briefs; other implementers' briefs; DI-1 … DI-10.
- **Inputs:** `Tangrams/*.json` (the app bundles it); `tools/device_reset.sh`.
- **Constraints:**
  - No product file, no kit file, no build file. If a build file needs a new androidTest dependency, recirculate.
  - Every wait is on an observed state with a bound (60 s), never a sleep (G-14 in spirit). The class leaves the controller and the device as it found them: the grid closed, the open puzzle unchanged; the reset script clears the app around every run.
  - Each test prints one `measure:` line in S-43's form, with the bundle's count and the device. The `gridScroll` swipe series is fixed, so the measure is comparable across images and bundles; record the series in the class.
- **Run:**
  1. Kit compile.
  2. Floor image first: three fresh-process runs of the class, then three cold starts, as the channel table's "scale record" row says. A reset and a clean `--check` (plus `mCurrentFocus`) go around every step.
  3. Then the dev image, then the tablet image. One emulator at a time, booted detached.
  4. On the floor: `AppLaunchKitTest` alone, to show the app set still runs with your class in it.
- **Done means:** the TASK-035 row's Done-check.
- **Return:** the handoff block, with:
  - the class (its three tests and what each span covers, from which call to which frame);
  - per image: the three values and the median of each measure (launch-to-first-puzzle, grid open, scroll janky share / p90 / p99, TotalTime);
  - the XMLs copied to your scratch, with each run's tree hash per the run log;
  - anything that made a run void.

---

## Brief — TASK-036 · WO-010 · slice-implementer

- **Mission:** The import. It is the riskiest task of the WO.
  1. **The tool:** write `tools/import_figures.py` (S-38, with `colours` per S-42 and `show`) and its tool test `tools/tests/test_import_figures.py`.
  2. **The files:** run the import over DI-10's two allowed collections. Every accepted figure becomes `Tangrams/<id>.json`, with S-39's fields and the stub picture, and goes into one of S-40's four batch lists.
  3. **The shared files:** write them once and complete (the shared-file table): `ASSET-LICENCES.md` (S-41), the import section of `Tangrams/README.md`, and the titles row of `review-notes.md`.
  4. **The tool test pin:** narrow the folder-equality line of `tools/tests/test_tangram_geom.py` (Q7).
  5. **The floor step:** prove the new geometry on the floor image: two play classes and the scale record.

  The class is mechanical data and tool work with exact geometry, plus a floor step.
- **Read exactly:**
  1. `tasks.md` § WO-010: the intro (lanes, windows, seam notes 1–3, "In-tree tests this WO changes"); § Seams S-38, S-39, S-40, S-41, S-42; the shared-file table; questions 1–5, 7 and 8; the Q7 table's row for `tools/tests/test_tangram_geom.py`; the rows TASK-036 (yours) and TASK-037 (what a batch does next with your files).
  2. `workorders/WO-010.md`: § Goal, § Scope with every note, § Constraints & notes (questions 1–8, the standing constraints, the risks), § Checkpoint items.
  3. Requirements, collection v1.2, read-only: `Requirements/reqs/REQ-007.md`, `REQ-038.md`, `REQ-039.md`, `REQ-040.md`, `REQ-041.md`, `REQ-042.md`, `REQ-045.md`, `REQ-047.md` (rule 2: both titles); `Requirements/req_types.md`: TYPE-001, TYPE-003.
  4. `architecture.md` §2: G-05, G-06, G-10, G-16 (v1.2); §3b O-02.
  5. `design-inputs.md`: DI-5 and DI-10 (register rows); §2 items 9, 14, 15, 16.
  6. **DI-10, read-only, outside the project — never write there:** `C:/GitHub/AI/TangramCollections/README.md`. In `collections/convex-13/` and in `collections/android-tangram-131/`: `figures.json`, `NOTICE.md`, and `LICENSE-GPL-3.0.txt` (android only). Not the other collections, not `sources/twpride-tangram/`, not `previews/` or `atlas/`. The `original/` folders are not read: `figures.json` is the registered data, and the snap is the route (R1).
  7. **DI-5:** `Spec/03-puzzle-format.md`; `Tangrams/README.md`; `Tangrams/puzzle.schema.json` (locked: never edited); the 13 `Tangrams/*.json`; `tools/tangram_geom.py`; `tools/validate_puzzles.py`; `tools/render_puzzle.py`; `tools/tests/test_tangram_geom.py`.
  8. `decisions.md`:
     - 2026-10-11 "**What 'congruent to its piece' means**", "**The `rot`/`flip`/`at` placement form is not read**", "**A bundled puzzle that fails a rule fails fast**", "**What 'a well-formed picture' means**", "**The load check's tolerances**", "**A missing `provenance` or `reviewedByHuman` reads as `false`**", "**The validity check's piece geometry comes through `ILockEngine`**";
     - 2026-10-17 "**WO-010 content batch: which figures may ship, and how many**";
     - 2026-10-18 "**WO-010 checkpoint items 1–2 answered by the owner**", "**The figures' source and licence are credited in `ASSET-LICENCES.md`, not in the puzzle file**";
     - the WO-010 rows R1–R5 and R7.
  9. `capture-outbox/src-draft-wo010-checkpoint-answers.md`, `capture-outbox/chg-draft-licence-notice-and-assets.md` (the asset rule as amended), `capture-outbox/chg-draft-req038-validity-rules-in-contract.md` (why REQ-038.A1 stays unverified).
  10. The visible acceptance tests for your IDs (read-only; never edit a kit file): the content kit folder (`PuzzleLibraryKitTest.kt`, `PuzzleLoaderKitTest.kt`, `KitTexts.kt`); the JVM kit folder's `PlayControllerKitTest.kt` (REQ-002.A1: every puzzle completed through the engine — a regression line your geometry must keep green); the play device kit folder's `BoardTrayKitTest.kt` and `DragKitTest.kt`; `app/src/androidTest/kotlin/app/tangramplaytime/app/ScaleMeasureTest.kt` (TASK-035's).
  11. `review-notes.md` (its table: you add one row); `LICENSE`.
  12. `platform-notes/android.md` (all of it); `.swdev/run-log/WO-010.md` (Env; g3-py's preview finding).
- **Do not read:** the kit author's briefs; other implementers' briefs; `C:/GitHub/AI/TangramNoAds`; the three other collections and `sources/twpride-tangram/` in `C:/GitHub/AI/TangramCollections`.
- **Inputs:** the row's Inputs cell: DI-10's two `figures.json`; `Tangrams/*.json`; `tools/tests/test_tangram_geom.py`; the play scaffolding tests that open the first full puzzle (`DecorationScaffoldTest.kt`, `DevButtonHitScaffoldTest.kt`, `TouchScaffoldTest.kt` in `play/src/test/kotlin/app/tangramplaytime/play/scaffolding/`; yours to update if your imports break one — say so); `tools/device_reset.sh`. *(Plan review S2, orchestrator edit 2026-10-19.)*
- **Constraints:**
  - **S-38 … S-42 are binding.** A rule that does not work on the data (a snap bound that must widen, a category a figure does not fit, an id the rule cannot make) is a `recirculate → planner` with the figure and the numbers, never a quiet change.
  - **Files:** never overwrite or edit a shipped file (seam note 1); never edit the schema or `IPuzzleLibrary.kt` (locked); never set `reviewedByHuman: true` (owner rule 7); never write under `C:/GitHub/AI/TangramCollections`.
  - **The collection's numbers are never the oracle.** That includes its `overlap`, `maxPieceAreaError` and `mirrored`. The validator and the Kotlin load check are (R1).
  - **Markdown by hand.** `ASSET-LICENCES.md`, `Tangrams/README.md` and `review-notes.md` are written with the Edit/Write tools from your report, never by the script (the ledger rule, `AGENTS.md`).
  - **Titles.** Each title edit follows S-39's three cases, and every `title.en` is unique. The fi titles are reference translations (G-10); proof-reading belongs in `review-notes.md`, not in your judgement calls.
  - **No sheet is committed by you.** The batches render the imports' sheets with their pictures. Your render goes to your scratch, so you can look at the silhouettes (Q8).
  - **Scaffolding tests** that a new first full puzzle breaks are yours to update; say which and why. A kit test that breaks is a finding for the orchestrator (the kit author corrects it), never your edit.
- **Run:**
  1. Write the tool and its test; run the test.
  2. Run `import … --dry-run` and read the report.
  3. Render the dry-run drafts' silhouettes in your scratch and look at them. This is where the convex names and every title edit are chosen.
  4. Run the import for real.
  5. Write the shared files.
  6. Narrow `test_tangram_geom.py` and run both tool tests.
  7. Run the Python channel.
  8. Run the JVM line (1) + (2).
  9. Floor image: `BoardTrayKitTest`, then `DragKitTest` (one class per invocation, with a reset around each), then the scale record (the channel table).
- **Done means:** the TASK-036 row's Done-check (1)–(5). REQ-038.A1 stays `implemented (unverified)` for the validity-rules CHG.
- **Return:** the handoff block, with:
  - the counts: imported, and skipped by reason (they add up to 144);
  - the turned copies; the rating distribution;
  - the batch sizes;
  - every title edit (from → to, the case), every convex name;
  - the widest and tallest imports (units);
  - the validator line, the tool tests' lines, and the JVM counts per module;
  - the floor XMLs and the scale record set against TASK-035's baseline and question 6's thresholds;
  - any scaffolding test you updated;
  - your proposed `decisions.md` rows, if any reading was left open.

---

## Brief — TASK-037 · WO-010 · slice-implementer

- **Mission:** Picture batch 1: the `shapes` and `numbers` puzzles listed in `tools/import/batch-1.txt`. For each listed file:
  1. Replace the stub `art` (S-39) with a picture drawn for this project.
  2. Render its sheet into `Tangrams/previews/`.
  3. Make a PNG copy in your scratch and look at it.

  The pictures are the visual work of this WO. Three batches run beside you on other files.
- **Read exactly:**
  1. `tasks.md` § WO-010: seam note 1; S-39 (`art`), S-40, S-42 (the colour report and **the art grammar every new picture keeps**); the shared-file table; question 4 ("Looked at"); the TASK-037 row.
  2. `Requirements/reqs/REQ-039.md` (the statement, rule 1: flat shapes, 3–8 colours, no text) and `REQ-007.md` (A1); `REQ-023.md` (A2: no piece boundary visible in the picture, whose device oracle reads your picture's pixels).
  3. **DI-5:** `Spec/03-puzzle-format.md` §1 (units, y down), §2 (`art`, Art shapes, **Art guidelines**); `Tangrams/puzzle.schema.json`'s `art` part (locked); `tools/render_puzzle.py` (what the sheet shows); `tools/validate_puzzles.py` (V10).
  4. **Style references:** the 13 shipped files' `art` and their committed sheets `Tangrams/previews/<id>.png` (open them with the Read tool). They show the house style; they are not reviewed, so they are no rule.
  5. `decisions.md`: 2026-10-11 "**What 'a well-formed picture' means**"; the WO-010 rows R4 and R8.
  6. `architecture.md` §2: G-05, G-16 (v1.2: every picture made for this project).
  7. The visible device test that judges your pictures at TASK-042, read-only: `play/src/androidTest/kotlin/app/tangramplaytime/play/SolvedPictureKitTest.kt`. REQ-039.A1 is "no picture paints outside its puzzle's outline", which the screen's clip guarantees. REQ-023.A2's oracle fails a picture in which a TYPE-001 piece colour that the picture does not itself use shows up inside it.
  8. Your list `tools/import/batch-1.txt` and the files it names. For coordinates, use `python tools/import_figures.py show <file>` (the bounding box and each piece's corners as floats).
- **Do not read:** the other batches' lists and files (they are not yours to change); the kit author's briefs; other implementers' briefs; DI-10's folders (its figures carry no pictures, and every picture here is made for this project); `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `tools/import/batch-1.txt` and the files it lists (their `art` only).
- **Constraints:**
  - **Change only `art`.** In each listed file, change only the `art` object. Never touch the solution, the titles, `difficulty`, `kind`, `category`, `id` or `provenance` (`reviewedByHuman` stays `false`, owner rule 7). A field you think wrong is a finding in your hand-back for TASK-036's author.
  - **The picture follows its title** (an abstract or a number figure gets a pattern or a scene that fits its shape). It uses flat shapes, 3–8 colours and no text: a number's picture adds no digit, since the silhouette is the number. It stays inside S-42's grammar, with its important details away from the very edge (`Spec/03` Art guidelines).
  - **Sheets:** one per listed file, rendered by the renderer (`render_puzzle.py Tangrams/<id>.json Tangrams/previews`). Never re-render another file's sheet: the 13 committed sheets are untouched (R4).
  - **No Gradle and no device:** the bundle is in its flux window. Write a file only when it passes the validator, so a reader never meets a half-written file.
- **Run, per file:**
  1. `show`.
  2. Write the `art`.
  3. Run the validator on the file.
  4. Run the colour report on the file.
  5. Render the sheet.
  6. Make the PNG copy (the channel table's Eyes row) and open it.

  Repeat until the line of question 4 holds. At the end, run the validator and the colour report over the whole list, and `git status`.
- **Done means:** the TASK-037 row's Done-check (1)–(4).
- **Return:** the handoff block, with:
  - one line per file (id · title · what the picture shows · colours · the "looked at" line of question 4);
  - the validator and colour-report output over the list;
  - the PNG folder's path, which the orchestrator opens before accepting;
  - every field you found wrong, for TASK-036's author.

---

## Brief — TASK-038 · WO-010 · slice-implementer

- **Mission:** Picture batch 2: the `letters` puzzles listed in `tools/import/batch-2.txt`. For each listed file: replace the stub `art` with a picture drawn for this project, render its sheet into `Tangrams/previews/`, make a PNG copy in your scratch and look at it. Three batches run beside you on other files.
- **Read exactly:** as TASK-037's brief items 1–8, with your own row (TASK-038) and your list `tools/import/batch-2.txt`.
- **Do not read:** as TASK-037's brief.
- **Inputs:** `tools/import/batch-2.txt` and the files it lists (their `art` only).
- **Constraints:** as TASK-037's brief. Also, a letter's picture adds no letter, glyph or other text (REQ-039 rule 1): the silhouette is the letter, and the picture decorates it.
- **Run, per file:** as TASK-037's brief; at the end, the validator and the colour report over your list, and `git status`.
- **Done means:** the TASK-038 row's Done-check (1)–(4).
- **Return:** as TASK-037's brief, for your list.

---

## Brief — TASK-039 · WO-010 · slice-implementer

- **Mission:** Picture batch 3: the `things` and `vehicles` puzzles listed in `tools/import/batch-3.txt`. For each listed file: replace the stub `art` with a picture drawn for this project, render its sheet into `Tangrams/previews/`, make a PNG copy in your scratch and look at it. Three batches run beside you on other files.
- **Read exactly:** as TASK-037's brief items 1–8, with your own row (TASK-039) and your list `tools/import/batch-3.txt`.
- **Do not read:** as TASK-037's brief.
- **Inputs:** `tools/import/batch-3.txt` and the files it lists (their `art` only).
- **Constraints:** as TASK-037's brief. Your list holds the widest imports (Boat, Bridge, Cargo Ship, about 9.9 units wide): keep their details large enough to read at a phone's small scale.
- **Run, per file:** as TASK-037's brief; at the end, the validator and the colour report over your list, and `git status`.
- **Done means:** the TASK-039 row's Done-check (1)–(4).
- **Return:** as TASK-037's brief, for your list.

---

## Brief — TASK-040 · WO-010 · slice-implementer

- **Mission:** Picture batch 4: the `animals`, `nature` and `people` puzzles listed in `tools/import/batch-4.txt`. For each listed file: replace the stub `art` with a picture drawn for this project, render its sheet into `Tangrams/previews/`, make a PNG copy in your scratch and look at it. Three batches run beside you on other files.
- **Read exactly:** as TASK-037's brief items 1–8, with your own row (TASK-040) and your list `tools/import/batch-4.txt`.
- **Do not read:** as TASK-037's brief.
- **Inputs:** `tools/import/batch-4.txt` and the files it lists (their `art` only).
- **Constraints:** as TASK-037's brief.
- **Run, per file:** as TASK-037's brief; at the end, the validator and the colour report over your list, and `git status`.
- **Done means:** the TASK-040 row's Done-check (1)–(4).
- **Return:** as TASK-037's brief, for your list.

---

## Brief — TASK-042 · WO-010 · slice-implementer

- **Mission:** The scale record and every channel, once, on the final tree. You make no product, kit or data change of your own.
  - **(a)** The Python channel over the whole folder.
  - **(b)** The JVM line.
  - **(c)** The device sets, floor first: the play set on the floor and dev images, the app set on all three.
  - **(d)** The scale record on each image: question 6's record, set against TASK-035's baseline.
  - **(e)** V-01 and its positive control.

  A red test is routed, not fixed by you. The class is device-only.
- **Read exactly:**
  1. `tasks.md` § WO-010: the intro (channels, windows, seam notes, "Unverified by design"); S-43; questions 6, 7 and 9 with the Q7 and Q9 tables; the TASK-042 row; the TASK-035 and TASK-036 rows (the baseline and the early floor numbers come from their hand-backs, which the orchestrator gives you).
  2. `workorders/WO-010.md`: § Constraints & notes (the verification channels, the device-time rules, the risks), § Definition of Done.
  3. Requirements, read-only: `Requirements/reqs/REQ-007.md`, `REQ-038.md`, `REQ-039.md`, `REQ-040.md`, `REQ-041.md`, `REQ-042.md`, `REQ-045.md`.
  4. `decisions.md`: the WO-010 rows R4, R6, R7, R8 and R10; 2026-10-18 "**A comma never reaches an instrumentation argument …**" (why the app set is two invocations and no comma list is passed).
  5. The visible tests: every kit folder (read-only; you run them). The named ones are `SolvedPictureKitTest.kt` and `BoardTrayKitTest.kt` (the play device kit folder), `AppLaunchKitTest.kt` and `ScaleMeasureTest.kt` (the app device kit folder), and the content kit folder.
  6. `platform-notes/android.md` (all of it); `.swdev/run-log/WO-010.md` (Env, the tree definitions, every row so far).
- **Do not read:** the kit author's briefs; other implementers' briefs; DI-1 … DI-10.
- **Inputs:** the row's Inputs cell.
- **Constraints:**
  - **Routing.** A red test on an imported puzzle goes back to that puzzle's author: its batch for a picture, TASK-036's author for every other field. A red kit test whose fixture assumed the 13-puzzle bundle goes to the kit author. Report the id, the test, the XML line and a short reading of why; the orchestrator dispatches.
  - **Re-runs.** After a fix, run the failing class alone on that image, then the whole sets once more on the final tree (the WO-006 lesson).
  - **Skips.** The only legitimate skips are `ProcessDeathKitTest`'s two guarded phases; any other skip in an XML is a failure.
  - **The tablet** runs the app set only.
  - **A threshold crossed** (question 6) is a `recirculate → planner` with the numbers, never a design change of your own.
- **Run:**
  1. The Python channel (the colour report over the four lists).
  2. The JVM line (1) + (2).
  3. V-01 and its positive control.
  4. Kit compile.
  5. Floor: the play set, the app set (two invocations), the scale record.
  6. Dev: the same.
  7. Tablet: the app set and the scale record.

  Use the timeouts of the channel table, and put a reset with a clean `--check` (plus `mCurrentFocus` on the floor) around every step. Copy every XML to your scratch and record each run's tree hash per the run log's definitions.
- **Done means:** the TASK-042 row's Done-check (1)–(5).
- **Return:** the handoff block, with:
  - per channel: the counts, and each XML's stamp and tree hash;
  - per image: the scale record (three values and the median of each measure, against TASK-035's baseline and question 6's thresholds);
  - the per-class run times of the play set on the floor (they are the evidence for the Q7 estimates);
  - every red test and where it was routed;
  - V-01's lines.
