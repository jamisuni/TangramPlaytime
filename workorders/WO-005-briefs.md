# WO-005 — dispatch briefs (#DevTools)

**Planner:** wo-planner (Opus 5.5)  ·  **Date:** 2026-10-17  ·  **Plan:** `tasks.md` § WO-005 (TASK-023 … TASK-026; no task is `[P]`)
**Mode:** Light (`workorders/WO-005.md` § Mode). There is no design. The seam table, the aid's tag table, the touch geometry and "What survives" in `tasks.md` § WO-005 › Seams are the design.

> Each brief is dispatched on its own. An Acceptance Test Author gets only its own brief plus the two tables below; it never sees an implementer brief. An implementer never sees another implementer's brief or a kit draft outside the tree.
> Every path is relative to `C:/GitHub/AI/TangramPlaytime/` unless it is absolute. Folder names used below:
> - "the JVM kit folder": `play/src/test/kotlin/app/tangramplaytime/play/`;
> - "the play device kit folder": `play/src/androidTest/kotlin/app/tangramplaytime/play/`;
> - "the app device kit folder": `app/src/androidTest/kotlin/app/tangramplaytime/app/`;
> - "the aid folder": `play/src/debug/kotlin/app/tangramplaytime/play/devaid/`.
>
> `decisions.md` rows are cited by date and bold title; rows are only ever appended. **The WO-005 rows** are the ones the orchestrator appends before dispatch for the planner's questions. These briefs cite them by the titles the planner proposed:
> - Q1 "**The #DevTools code home reads `play/`**" (`build-map.md`, a notify-tier delta);
> - Q2 "**The DEV aid reaches the screen through one door**";
> - Q3 "**The DEV aid's strings live with the aid**";
> - Q4 "**Solve this puzzle now is an ordinary solve**";
> - Q5 "**A solution pose is read from the puzzle, not computed**";
> - Q6 "**The DEV button, the overlay and the sheet, as placed**";
> - Q7 "**REQ-046.A4's oracle is the release APK**";
> - Q8 "**Product LOC counts the debug source set**".
>
> If the orchestrator changes a title, it corrects the citations here in the same edit.

## Dispatch order (two lanes)

| Step | Test lane (acceptance-test-author) | Implementer lane (slice-implementer) | Gate to the next step |
|---|---|---|---|
| 0 | — | — (orchestrator) Make the `build-map.md` `#DevTools` code-home cell read `play/` (Q1). Append the WO-005 rows for Q2–Q8. | The rows exist, and the build-map delta is logged |
| 1 | TASK-023 drafts and **lands** the JVM kit in the JVM kit folder. TASK-025 starts drafting the device kit in its scratchpad. | — | TASK-023's Done-check is met at landing |
| 2 | TASK-025 keeps drafting. | TASK-024, reading the landed kit | TASK-024's Done-check is green. This is the JVM kit's green verdict. |
| 3 | TASK-025 **lands** (resume the same author context with "TASK-024 done"). | — | TASK-025's Done-check is met at landing |
| 4 | — (the play device set does not compile until TASK-026 lands) | TASK-026 | Both device sets are green on the dev **and** the floor image; the JVM line and V-01 pass |
| 5 | Run `plan_check.py --wo WO-005` again now that the kit has compiled (WO DoD) → Code Reviewer → Test & Verify (every set, every channel) | | |

- **Why this order.**
  - The seams fix every name the kits call. The JVM kit can therefore be written before any product code, and it lands first so that TASK-024 works against the visible acceptance tests for its IDs.
  - TASK-025 lands after TASK-024, so that any seam TASK-024 had to recirculate is settled before the device kit lands. It needs S-28's tags and S-26's class to compile.
  - Every compile-level seam of the play device kit that TASK-024 does not deliver is TASK-026's (S-10Δ2). The set therefore compiles once TASK-026 lands.
- **Dependencies versus dispatch.** Drafting the device kit during steps 1–2 touches nothing in the tree, so it is dispatch, not a plan dependency.
- **Red windows** (`tasks.md` § WO-005 intro, per source set). Nobody runs a set for a verdict inside its window.
  - `play/src/test`: from step 1 until TASK-024 lands. From then on, the JVM line rebuilds both APKs first, because `ReleaseBuildKitTest` reads them.
  - `play/src/androidTest`: from step 3 until TASK-026 lands.
  - `app/src/androidTest`: compiles from step 3. `DevAidAppKitTest` fails at run time until TASK-026.
- **Compile errors in the kit.** An unresolved reference to a seam of a task that has not landed yet is expected; each kit row lists which. Any other compile error in a kit file is a test defect. It goes back to that kit's author and counts as a corrected check. An implementer never edits a kit file.
- **In-tree tests this WO changes:** only `KitScreen.kt`, by TASK-025 (`tasks.md` § WO-005 intro, seam note 3). The scaffolding tests belong to the implementers.
- **Decisions the implementers record.** A number or reading that no REQ, seam row or `decisions.md` row fixes goes back as a proposed `decisions.md` row, with its DI source and the alternative. The orchestrator appends it, so each is recorded once.
- **Checkpoint items** (`workorders/WO-005.md` § Checkpoint items, once the orchestrator has asked them). The build follows the defaults. Kit pins on them cite the WO-005 rows, so an owner answer changes a constant or a branch and the pin that cites it.

## Verification channels (G3)

Each "the line" below is `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat <tasks>"`. `ADB` is `/c/Users/Jami/AppData/Local/Android/Sdk/platform-tools/adb`; `EMU` is `/c/Users/Jami/AppData/Local/Android/Sdk/emulator/emulator`.

| Channel | How (Git Bash) | Result | Needed by |
|---|---|---|---|
| JVM (JUnit 4) | **Two runs of the line, in this order** (amended 2026-10-17 10:51 by the orchestrator on the TASK-023 kit author's measurement: in one Gradle run `:play:testDebugUnitTest` started while `:app:lintVitalAnalyzeRelease`, which `:app:packageRelease` waits on, was still running, so the test read the release APK before it was repackaged — a false RED by stale refusal): **(1)** `:app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun`, **then (2)** `:play:testDebugUnitTest --rerun :contracts:test --rerun`. A bare `:play:testDebugUnitTest` without run (1) first is not a verdict (plan review F2). The APK tasks come first because `ReleaseBuildKitTest` reads both APKs and refuses one older than the sources (seam note 4). Gradle leaves an up-to-date APK unwritten, so the two package tasks are forced (the WO-004 lesson, `platform-notes/android.md`). At close, add `:store:testDebugUnitTest :content:testDebugUnitTest`. | `play/build/test-results/testDebugUnitTest/TEST-*.xml`, `contracts/build/test-results/test/TEST-*.xml` | TASK-023 (compile status), TASK-024, TASK-026; Test & Verify |
| Kit compile | The line with `:play:assembleDebugAndroidTest :app:assembleDebugAndroidTest`, plus `:app:assembleDebug :app:assembleRelease` at TASK-026 | the exit code and the compiler messages | TASK-025, TASK-026 |
| Dev image `Medium_Phone_API_37.0`, running as `emulator-5554` | `$ADB devices` first. Then the line with `:play:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.play.DevAidScreenKitTest`, **one class per invocation**: through `cmd.exe //c`, a comma list runs only its first class (`platform-notes/android.md` § Compose test kit). Omit the argument to run a whole set. `:app:connectedDebugAndroidTest` works the same way. The XML folder is overwritten on every run, so copy each XML you cite to your scratchpad. Judge a run by its XML (it exists, is stamped after the command started, and carries the expected counts), never by Gradle's exit code. | `play/build/outputs/androidTest-results/connected/debug/TEST-*.xml`, `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` | TASK-026; Test & Verify |
| Floor image `Phone_API_26` (`minSdk 26`), one emulator at a time | `$ADB emu kill`, then `$ADB devices` until it is empty. Run `$EMU -avd Phone_API_26 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot` in the background, then `$ADB wait-for-device`, then poll `$ADB shell getprop sys.boot_completed` until it reads `1`, then `$ADB devices`. Run the same lines. Afterwards kill it, boot `Medium_Phone_API_37.0` the same way, and leave it running. | the same XML folders (the file name carries the device) | TASK-026 (both sets); Test & Verify |
| Release verifier V-01 (G-01, G-08) | The line with `:app:assembleRelease :app:assembleDebug`, then `python .swdev/verifiers/v01_release_apk.py`, `python .swdev/verifiers/v01_release_apk.py --positive-control`, and `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-005` | one PASS/FAIL line each, then GREEN/RED and the budget line (expected: 0 verifiers, 0 lines for WO-005) | TASK-024, TASK-026, the close |
| Device hygiene | `bash tools/device_reset.sh`, then `bash tools/device_reset.sh --check` (clean), **before and after** every device step. The app writes its progress file at its first open, so a `--check` straight after a run that leaves the app installed reports NOT clean by design. The after-step therefore resets first. A non-clean check after a reset voids the step (`platform-notes/android.md` § Device hygiene). | `--check` reports clean or not | every device step |

- **Not a DoD channel:** human eyes. The owner plays the debug build at close from `review-notes.md` (the DEV button, the prompt, the overlay, "solve now", the Finnish strings).
- **Not a channel of this WO:** the process-death pair. No store path changes, and the aid saves through `Persistence` (S-19). The app set's two guarded `ProcessDeathKitTest` skips run as before.
- **Toolchain proof:** the JVM, floor and dev channels are proven in `.swdev/run-log/WO-005.md`, rows g3, f26 and d37. The JVM line above adds the two APK builds, which V-01 has built at every close since WO-003.
- **Time:** WO-005 computes no time value (G-14). The celebration after an aid solve runs on the compose rule's `mainClock` in a device test. Nothing sleeps.
- **Browser:** none.

---

## Brief — TASK-023 · WO-005 · acceptance-test-author

- **Mission:** Write the JVM part of WO-005's one acceptance kit, in the JVM kit folder:
  - `DevAidKitTest.kt`: the aid's state (S-26), and "solve now" (S-27) over every bundled puzzle, with the save;
  - `ReleaseBuildKitTest.kt`: REQ-046.A4 on the release and debug APKs (S-30's markers).

  Both are written against `tasks.md` § WO-005 › Seams. Land them before TASK-024 starts. The class is JVM oracles over every bundled puzzle plus a scan of the release APK, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-005:
     - the intro: lanes, red windows, seam notes 1–4, lines covered twice, "Unverified by design", "Not covered here";
     - § Seams: S-25 … S-30, S-10Δ2, S-22Δ2, S-15Δ2, the aid's tag table, the touch geometry, "What survives";
     - the rows TASK-023 (yours) and TASK-024 (its Done-check is your green verdict).
  2. The controller your fixtures drive:
     - `tasks.md` § WO-003 › Seams S-1, S-2, S-3;
     - `designs/WO-004-design.md` §2, rows S-19, S-1Δ (both), S-2Δ, S-20, S-21 and S-3Δ, and "What survives".
  3. `workorders/WO-005.md`: § Scope with every scope note; § Mode; § Constraints & notes (the five questions and the standing constraints); § Checkpoint items.
  4. Requirements, collection v1.2, read-only:
     - your tokens: `Requirements/reqs/REQ-046.md`;
     - rules your fixtures cross, no token: `REQ-022.md` (the solve), `REQ-023.md` (what a solve shows), `REQ-019.md` (its rationale on orders a player can build), `REQ-007.md` (REQ-046's rationale);
     - `Requirements/req_types.md`: TYPE-001, TYPE-003, TYPE-004, TYPE-006.
  5. Pending drafts, for context: `capture-outbox/chg-draft-req046-android-build-terms.md` and `capture-outbox/chg-draft-type006-missing-transitions.md`. The build meets both and the locked text (§ WO-005 intro, "Unverified by design").
  6. Frozen contracts, baseline 2026-10-11, every KDoc line, all under `contracts/src/main/kotlin/app/tangramplaytime/contracts/`: `IPuzzleLibrary.kt`, `ILockEngine.kt`, `IProgressStore.kt`, `Types.kt`.
  7. `decisions.md`:
     - 2026-10-15: "**When progress is saved**", "**Restart and Retry act only in their own state**", "**O-04 read for this WO**", "**`Persistence`'s cache and its stop path**", "**The store does not check the values it is given**";
     - 2026-10-12: "**One-shot cues are shown once per controller**";
     - the WO-005 rows Q2, Q4, Q5 and Q7.
  8. In-tree tests in the JVM kit folder:
     - **reuse:** `KitPlay.kt` (`kitPoseOf`, `kitPlanOrNull`, `kitController`, the kit's dp-per-unit literal) and `KitStore.kt` (the in-memory `IProgressStore` double). You may add a kit-prefixed helper to either, but change no existing helper.
     - **patterns:** `ResumeKitTest.kt` (a new controller over the same double) and `BrowsingKitTest.kt`.
     - **not yours:** never edit, and never reuse a name from, `PlayScaffoldTest.kt` or the `scaffolding/` folder beside it.
  9. Build files: `play/build.gradle.kts` and `app/build.gradle.kts`. `gradle/libs.versions.toml` has no coroutines-test artifact, so drive suspend calls with `runBlocking`. `play/src/test` compiles only for the debug unit-test variant (seam note 1).
  10. `.swdev/verifiers/v01_release_apk.py`: the precedent for an APK scan with a positive control and a "the scan read nothing" guard. It is not yours to change.
  11. Rules:
      - `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2;
      - `C:/GitHub/AI/SWDev/framework/methodology/task-decomposition.md`: "Assert only what is mandated", "Seams name their task; adapters fail loudly", "Declared inputs land together";
      - `platform-notes/android.md`, all of it, including the WO-004 lessons on build outputs and on pins that skip;
      - `architecture.md` §2 G-08, G-10, G-12, G-14, G-15; §3b O-01, O-06, O-08.
- **Do not read:**
  - the main code of `:play`, `:app`, `:kernel`, `:content` and `:store` (compiler messages are fine), and anything under `play/src/debug` (TASK-024's);
  - the implementer briefs (§ TASK-024 and § TASK-026 of this file), and TASK-025's drafts;
  - DI-1 … DI-4: never the prototype. They supply no Contract text.
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-023 row's Inputs cell: the bundled `Tangrams/*.json` through `PuzzleLibraries.bundled(LockEngine())` on the play test classpath, and, for `ReleaseBuildKitTest`, the two APKs of `:app` plus the source trees its staleness check reads.
- **Oracle rule:**
  - Every assertion cites, in a comment, the sentence it checks: a REQ sentence, a TYPE rule, a KDoc line, a seam row (`S-n`), or a `decisions.md` row by date and title. If it has no source, drop it.
  - A REQ token goes only on a test whose assertion is that line's meaning. A pin carries its source and no token.
  - Cite rules without a dotted ID in comments ("REQ-046 rule 4", not a dotted ID). A dotted ID appears only where it is a token: trace-check counts every dotted ID it finds.
  - **A line with a quantifier names its adversarial path in the same test** (`platform-notes/android.md`). "Every puzzle of the library" (REQ-046's rationale) means every bundled puzzle, not a sample.
  - **Fixtures discriminate.** Show test-side that the state you claim to tell apart exists. For example, an In-progress start must have a piece locked away from its solution pose; show that with the engine.
  - The double fails loudly on a miss. Call `internal` members directly, with no reflection. No `assume…`: a leg that cannot run fails with a message saying why.
- **Where the tokens go.** If your judgment adds or moves a token, say so in the hand-back; the orchestrator corrects the cells.
  - `DevAidKitTest.kt`: REQ-046.A1, REQ-046.A3.
  - `ReleaseBuildKitTest.kt`: REQ-046.A4.
  - Never REQ-046.A2 here: it is a screen line, TASK-025's. Never a dotted ID outside REQ-046.
- **What each line needs** (pointers, not restatements; the REQ file is the oracle):
  - **REQ-046.A1:** S-26's actions on a new `DevAid`: `onButton()`, then `submit` with the entry A1 refuses, then with the one it accepts. Rule 1 says a refused entry "reveals nothing". After it, `unlocked`, `solutionShown` and the controller are unchanged, and S-26's `toggleSolution` and `solveNow` refuse while locked.
  - **REQ-046.A3:** S-27, through `DevAid.solveNow` after the accepted entry.
    - **Loop:** every bundled puzzle from New, each over its own `Persistence` on the kit double, with `boardDpPerUnit` set as `KitPlay.kt` sets it.
    - **Assert:** the end state, `bestSeconds`, the `celebration` (S-3Δ), and, after `flush()`, what the double holds for the puzzle (S-20's shape for a Solved puzzle).
    - **Starts that discriminate:** In progress with a piece locked away from its pose; and New with a tray piece turned and the parallelogram mirrored.
    - **Content precondition:** confirm test-side with `kitPlanOrNull` that every bundled puzzle has an order. A puzzle without one is a content finding for your hand-back. Keep it in the loop; it must fail, never be skipped.
  - **REQ-046.A4:** S-30's three markers.
    - The release APK holds none of them. The debug APK holds all three: that is the positive control, in the same test.
    - Each APK must yield at least one `Lapp/tangramplaytime/play/` class, so a scan that read nothing cannot pass.
    - Refuse a stale APK as the TASK-023 row says. A missing APK fails loudly.
    - Resolve the project root as the nearest ancestor of the working directory that holds `settings.gradle.kts`.
    - Look for the resource prefix in `resources.arsc` in both UTF-8 and UTF-16LE. The positive control shows which one the scan sees.
  - **Pins, no token:**
    - A new `DevAid` is locked, with the solution off and the sheet NONE (S-26; REQ-046 rule 2 as read: a new process builds a new aid; "What survives").
    - S-25's `installed` is a `DevAid` on this classpath (S-25, S-30).
    - After the accepted entry, then `close()` and browsing (`next()`, `nextUnsolved()`, `choose`), `onButton()` opens the menu directly (rule 2). `solutionShown` survives browsing (rule 3).
    - A best time seeded in the double is kept by S-27 (rule 4; S-27 "Times pass through").
    - A new controller over the same double opens an aid-solved puzzle Solved, with an empty board and its best time unchanged ("What survives").
    - S-27 returns false and changes nothing on a Solved puzzle.
    - The double's writes for a puzzle solved from New show it In progress before Solved: the locked TYPE-006 transitions, per Q4's row.
- **Run:**
  1. Write the two files into the JVM kit folder. Landing is your Done-check, met at landing.
  2. Compile with the JVM line of the channel table. Expected, and not your defect: unresolved references to S-25 and S-26 (`DevAidHook`, `DevAid`, `DevSheet`). Any other compile error is yours to fix.
  3. Grep the folder for the dotted token pattern `REQ-0[0-9]*\.A[0-9]` before you hand back. It must show exactly the row's list, and the WO-003/004 tokens unchanged.
- **Done means:** the TASK-023 row's Done-check, plus hygiene:
  - helper names carry a `kit` prefix;
  - JUnit 4 only; no new dependency;
  - never edit main code, a build file or a scaffolding test.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the test names for each acceptance ID, and the list of pins with their sources;
  - the files outside `play/` that the tests read at run time;
  - the compile status, with the command and its exit code;
  - any bundled puzzle without an order (a content finding);
  - the number of corrected checks so far.

---

## Brief — TASK-025 · WO-005 · acceptance-test-author

- **Mission:** Write the device part of WO-005's one acceptance kit:
  - `DevAidScreenKitTest.kt` in the play device kit folder: `PlayScreen` with a `DevAid`, covering the prompt, the overlay and "solve now", and the pins;
  - `DevAidAppKitTest.kt` in the app device kit folder: the real app, with the DEV button and the passcode through the real wiring;
  - an amendment to `KitScreen.kt`: an optional `devAid`.

  Draft beside TASK-023 and TASK-024. Land in one stage once TASK-024 is green. The class is a Compose UI kit with device-only legs and a pixel-colour oracle, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-005: the intro, all of § Seams (the seam table, the aid's tag table, the touch geometry, "What survives"), and the rows TASK-025 (yours) and TASK-026 (its Done-check is your green verdict).
  2. `tasks.md` § WO-003: S-10, S-13 and S-14 (`BoardMapping`), the tag table, and the touch routing as superseded on 2026-10-12.
  3. `designs/WO-004-design.md`:
     - §2: S-22 (the touch geometry, the hit order, taps and the long press, the overview), S-3Δ, S-13Δ, "What survives";
     - §3: "Always show the name and number", and library facts 4–5.
  4. `workorders/WO-005.md`: § Scope, § Mode, § Constraints & notes, § Checkpoint items.
  5. Requirements, read-only:
     - your tokens: `Requirements/reqs/REQ-046.md`;
     - lines your pins cross, no token: `REQ-023.md` (the celebration), `REQ-024.md` (A2: the bar), `REQ-026.md` (the solved row), `REQ-037.md` (its rule on the #DevTools controls), `REQ-050.md` (the long press, the grid);
     - `Requirements/req_types.md` TYPE-006.
  6. Contracts: `Types.kt`, `IPuzzleLibrary.kt`, `ILockEngine.kt`.
  7. `decisions.md`:
     - 2026-10-15: "**The grid overview, as drawn**" and its amendment; "**The top bar, Restart and the solved row, as drawn**"; "**What a Solved puzzle draws, and fingers on a control**"; "**The corner-pulse markers draw above the Restart pill**";
     - 2026-10-12: "**One-shot cues are shown once per controller**", "**Where the flip badge's touch area ends**", "**Inside the parallelogram's tray cell the visible thing wins, as on the board**", "**The solve, as drawn**";
     - 2026-10-17: "**The grid overview, drawn in detail**";
     - the WO-005 rows Q2, Q3, Q4 and Q6.
  8. The in-tree kit:
     - **amend:** `KitScreen.kt`. Add an optional `devAid` to `KitScreen` and give it to `PlayScreen` in `show()` and `recreate()`. No existing caller changes.
     - **patterns** (read-only):
       - `OverviewKitTest.kt`: a sheet below the bar; the bar drawn above it; the router standing aside;
       - `SolvedViewKitTest.kt` and `SolvedPictureKitTest.kt`: the solve timeline;
       - `TopBarKitTest.kt`: the long press;
       - `ResumeScreenKitTest.kt`: the recreation leg;
       - in the app device kit folder, `AppLaunchKitTest.kt` (the wait for `play-screen`, the cleared-store precondition) and `AppWiringKitTest.kt`.
     - Never edit any of these except `KitScreen.kt`, and never reuse their names.
  9. `platform-notes/android.md`, all of it, and `tools/device_reset.sh`.
  10. Rules: `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2; `C:/GitHub/AI/SWDev/framework/methodology/task-decomposition.md` ("Assert only what is mandated", "Seams name their task; adapters fail loudly", "Declared inputs land together"); `architecture.md` §2 G-08, G-10, G-17; §3b O-08.
- **Do not read:**
  - the main code (`play/src/main`, `play/src/debug`, `app/src/main`); compiler messages are fine;
  - the implementer briefs (§ TASK-024 and § TASK-026 of this file);
  - DI-1 … DI-4: never the prototype.
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-025 row's Inputs cell: the bundled puzzles on the device, and the aid's English strings, which the app kit reads in the real app.
- **Oracle rule:**
  - Every assertion cites its source: a REQ sentence, a seam row, a tag-table row, or a `decisions.md` row by date and title. If it has no source, drop it. Dotted IDs appear only as tokens.
  - **The Contract wording is English.** The kit checks that the device language is English before it reads a text, and fails loudly otherwise. It never asserts Finnish (G-10).
  - Read geometry from the nodes (`board`'s `BoardMappingKey`, node bounds) and the pixels, never from a number in prose. Clip every pixel sampler to its own capture.
  - **Quantifier lines get their adversarial path in the same test:**
    - REQ-046 rule 3 ("stays on while the player browses puzzles until it is switched off"): ›, the long press on ›, a grid choice, a Solved puzzle and back, then Hide;
    - rule 2 ("stays unlocked"): a recreation through `KitScreen.recreate()`, after which DEV opens the menu directly;
    - A2's "one shape per piece of the puzzle": a mini and a full puzzle (the counts differ, so the fixture discriminates).
  - No `assume…`. The only legitimate guarded skips in this project are the death phases.
- **Where the tokens go.** If your judgment adds or moves a token, say so in the hand-back.
  - `DevAidScreenKitTest.kt`: REQ-046.A1, REQ-046.A2, REQ-046.A3.
  - `DevAidAppKitTest.kt`: REQ-046.A1 (the real app: extra coverage).
  - None in `KitScreen.kt`. Never REQ-046.A4 here (the APKs are TASK-023's). Never a dotted ID outside REQ-046.
- **What each line needs** (pointers; the REQ file is the oracle):
  - **REQ-046.A1 (screen):**
    - Open: DEV, then the prompt.
    - The entry A1 refuses, then OK: the message node's text is rule 1's sentence; no menu node and no overlay node exist; the field is empty (touch geometry, "The sheet").
    - The entry A1 accepts, then OK: the menu, with the labels A2 and A3 quote.
  - **REQ-046.A2:** Show (S-26). Then, per the aid's tag table:
    - the set of shape tags equals the puzzle's pieces;
    - each shape's bounds are the px box of its solution polygon through `board`'s `BoardMappingKey`, within 1 px, and lie inside `silhouette`;
    - each label's text is its piece's name;
    - the colour follows the touch geometry's overlay line: on a New puzzle, sample at least 4 dp inside each polygon and outside its label.
  - **REQ-046.A3:**
    - Solve, from the menu.
    - Then: the screen's `PuzzleStateKey` reads Solved; once REQ-023's rule timings have passed, `solved-picture` is displayed; the solved row's `best-time` shows the kit's no-time literal; `dev-button` does not exist.
    - Start once from New and once from In progress.
  - **Pins, no token:**
    - The DEV button's place and size (S-22Δ2; the REQ-046 ASSUMPTION): inside `board`, its left within 16 dp of the board's left, its bottom within 16 dp of the board's bottom, shorter than 48 dp.
    - A slide from the DEV button does nothing.
    - Hit order 0 with the sheet open: ‹, the counter and a tray piece do nothing, and the bar is drawn above the sheet (the sheet's top is at or below the bar's bottom, as `OverviewKitTest.kt` checks it).
    - A tap on the scrim closes the sheet.
    - Hit order 1: a second finger on the DEV button during a drag opens nothing.
    - With reduced motion off, the celebration plays after an aid solve: the `confetti` node appears on the timeline (Q4's row).
    - "What survives": a recreation keeps the overlay and the unlocked state.
  - **`DevAidAppKitTest.kt`:**
    - Run `tools/device_reset.sh` first (a cleared store), then wait for `play-screen`.
    - `dev-button` exists on the open puzzle.
    - Then A1, through the real app.
    - Pin: `DevAidHook.installed` is not null in the debug app (S-25, S-30).
- **Run:**
  1. Write the two new files into their kit folders and amend `KitScreen.kt`. Landing is your Done-check, met at landing.
  2. Compile with the kit-compile line. Expected, and not your defect: in `:play:assembleDebugAndroidTest`, unresolved references to S-10Δ2 (`PlayScreen`'s `devAid`). `:app:assembleDebugAndroidTest` must compile. Any other compile error is yours to fix.
  3. Grep both folders for `REQ-0[0-9]*\.A[0-9]`. It must show exactly the row's list, and the WO-003/004 tokens unchanged.
- **Done means:** the TASK-025 row's Done-check; helper names carry a `kit` prefix; no new dependency; never edit main code, a build file or another WO-003/004 test.
- **Return:** the handoff block, with:
  - the test names for each acceptance ID, and the pins with their sources;
  - the files outside each module that the tests read at run time;
  - the compile status, with the command and its exit code;
  - the number of corrected checks so far.

---

## Brief — TASK-024 · WO-005 · slice-implementer

- **Mission:** Build the aid's core:
  - S-25, the door in `src/main`: the class, `installed`, `sheetOpen` and `onButton`;
  - in the debug source set: S-26 (the state and the actions), S-27 ("solve now"), S-28 (the tags), S-29 (the strings) and S-30 (the service file).

  When it is done, `DevAidKitTest.kt` (REQ-046.A1, REQ-046.A3) and `ReleaseBuildKitTest.kt` (REQ-046.A4) are green, and so is the rest of the JVM kit. The class is novel logic behind a build-variant seam, so it goes to the strongest model.
  - **Riskiest item first:** S-27's order and drive, over every bundled puzzle. Second: `ServiceLoader` finding the aid on the debug unit-test classpath (S-25, S-30).
- **Read exactly:**
  1. `tasks.md` § WO-005:
     - the intro (seam notes 1, 3 and 4);
     - § Seams S-25 … S-30 and "What survives";
     - S-10Δ2, S-22Δ2 and S-15Δ2 as context only (they are TASK-026's);
     - row TASK-024.
  2. The controller you drive: `tasks.md` § WO-003 › Seams S-1, S-2, S-3; `designs/WO-004-design.md` §2, rows S-2Δ, S-3Δ, S-19, S-20, S-21.
  3. `workorders/WO-005.md`: § Scope with its notes; § Constraints & notes.
  4. Requirements: `Requirements/reqs/REQ-046.md`, `REQ-022.md`, `REQ-023.md`, `REQ-019.md` (its rationale); `Requirements/req_types.md` TYPE-001, TYPE-003, TYPE-004, TYPE-006.
  5. Pending drafts: `capture-outbox/chg-draft-req046-android-build-terms.md`, `capture-outbox/chg-draft-type006-missing-transitions.md`.
  6. Contracts: `ILockEngine.kt`, `IPuzzleLibrary.kt`, `Types.kt`.
  7. `decisions.md`:
     - the WO-005 rows Q2, Q3, Q4, Q5, Q7 and Q8;
     - 2026-10-15: "**Restart and Retry act only in their own state**", "**When progress is saved**", "**The save runs after every action, a board drag start included**", "**O-04 read for this WO**".
  8. `architecture.md` §2 G-08, G-10, G-12, G-14, G-15; §3b O-01, O-06, O-08.
  9. `platform-notes/android.md`.
  10. Design inputs (behaviour reference only, never DoD): **DI-1** `tools/prototype_template.html`, the `devSolveNow`, `openDev` and `devSubmit` functions and the `#devDlg` texts; `design-inputs.md` §2 item 12.
  11. The visible acceptance tests, read-only, in the JVM kit folder: `DevAidKitTest.kt`, `ReleaseBuildKitTest.kt`, and the helpers they use in `KitPlay.kt` and `KitStore.kt`.
  12. Existing files:
      - in `play/src/main/kotlin/app/tangramplaytime/play/`: `PlayController.kt`, `Persistence.kt`, `PlayDrawing.kt` (`Slot`); `PlayScreen.kt` (`ScreenGeometry`), read-only;
      - `play/src/debug/kotlin/app/tangramplaytime/play/DebugCanary.kt`, `play/build.gradle.kts`, `.swdev/verifiers/v01_release_apk.py` (what V-01 reads).
- **Do not read:**
  - the other briefs, and kit drafts outside the tree;
  - the bodies of `store/src/main`, `kernel/src/main` and `content/src/main` (the contracts suffice);
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-024 row's Inputs cell.
- **Run:**
  - The JVM line (it rebuilds both APKs first).
  - The V-01 line.
  - `grep -rnE "devaid|DevAid[^H]|0417|dev_aid_|Wrong passcode" play/src/main app/src/main` must print nothing (O-08, G-08). `DevAidHook`'s KDoc names REQ-046, G-08 and O-08, never an aid class.
  - Grep the aid folder for colour literals and for user-visible string literals. There must be none (S-12, G-10).
- **Done means:**
  - The seams exactly as the rows say. S-25's four composable members are TASK-026's: do not add them now.
  - No `PlayController`, `Persistence` or contract change, no new dependency, no time (G-14). `DebugCanary.kt` stays where it is.
  - The strings are in `values/` and `values-fi/`. Finnish is a reference translation, listed in your hand-back for `review-notes.md` (G-10).
  - Read-only for you: the kit, `:contracts`, `:store`, `:kernel`, `:content`, `:app`.
  - **Decisions to hand back** as proposed rows: any reading that no row fixes (for example, the twist direction at a tie of four steps), with its source.
  - The Done-check is the TASK-024 row.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with:
  - the inputs read, and the files changed;
  - the commands, their exit codes, the result XML paths and the test counts, once;
  - the Finnish strings;
  - the proposed decision rows;
  - any bundled puzzle S-27 cannot plan (a content finding, never worked around).

---

## Brief — TASK-026 · WO-005 · slice-implementer

- **Mission:** Put the aid on the Play screen and wire it into the app:
  - S-25's four composable members, and their `DevAid` overrides: the overlay, the DEV button, and the sheet with the passcode field;
  - S-10Δ2 (`PlayScreen`), S-22Δ2 (the hit order, the router standing aside), S-15Δ2 (`MainActivity`).

  When it is done:
  - the device kit compiles;
  - `DevAidScreenKitTest.kt` and `DevAidAppKitTest.kt` are green on the dev and the floor image;
  - the ten WO-003/004 play classes and the app set stay green;
  - the JVM line and V-01 pass.

  The class is UI, rendering and touch, device-only, so it goes to the strongest model.
  - **Riskiest item first:** the sheet against the router: hit order 0, and `designs/WO-004-design.md` §3 library fact 4 (the router must not consume while the sheet is open). Second: the text field's focus and the soft keyboard, which may resize the window under `MainActivity`'s `safeDrawingPadding`. Find nodes by tag, never by a position measured before the keyboard opened.
- **Read exactly:**
  1. `tasks.md` § WO-005: all of it (the intro, § Seams, the aid's tag table, the touch geometry, "What survives"), and row TASK-026.
  2. `tasks.md` § WO-003: S-10 … S-14, the tag table, and the touch routing (still in force where WO-004 and WO-005 do not change it).
  3. `designs/WO-004-design.md`:
     - §2: S-22 (all), S-13Δ, S-3Δ, "What survives";
     - §3: "Always show the name and number", and library facts 4–5.
  4. `workorders/WO-005.md`: § Mode, § Constraints & notes, § Checkpoint items (the defaults you build).
  5. Requirements: `Requirements/reqs/REQ-046.md`, `REQ-023.md`, `REQ-024.md`, `REQ-026.md`, `REQ-037.md`, `REQ-050.md`; `Requirements/req_types.md` TYPE-006.
  6. Contracts: `Types.kt`, `IPuzzleLibrary.kt`.
  7. `decisions.md`:
     - the WO-005 rows Q2, Q3, Q4 and Q6;
     - 2026-10-12: "**Where the flip badge's touch area ends**", "**Inside the parallelogram's tray cell the visible thing wins, as on the board**", "**What "drawn" means in the touch order**";
     - 2026-10-15: "**The grid overview, as drawn**" and its amendment; "**The top bar, Restart and the solved row, as drawn**"; "**What a Solved puzzle draws, and fingers on a control**"; "**The corner-pulse markers draw above the Restart pill**";
     - 2026-10-17: "**The grid overview, drawn in detail**".
  8. `platform-notes/android.md`: § Verification channels (the floor blurs scaled paths) and § Compose test kit (merged, inert containers).
  9. `architecture.md` §2 G-08, G-10, G-12, G-17; §3b O-06, O-08.
  10. Design inputs (behaviour reference only, never DoD):
      - **DI-1** `tools/prototype_template.html`: `.devbtn`, `.devbtn.on`, `#devDlg`, `.pin`, `drawHint`, `openDev`;
      - **DI-2** `Spec/02-ui-layout.md` §5 (the visual language);
      - `design-inputs.md` §2 items 6 and 12.
  11. The visible acceptance tests, read-only:
      - in the play device kit folder: `DevAidScreenKitTest.kt`, `KitScreen.kt`, and the ten WO-003/004 classes;
      - in the app device kit folder: `DevAidAppKitTest.kt` and the app set;
      - in the JVM kit folder: `ReleaseBuildKitTest.kt`.
  12. Existing files:
      - in `play/src/main/kotlin/app/tangramplaytime/play/`: `PlayScreen.kt`, `HitTest.kt`, `PointerRouter.kt`, `Overview.kt` (the pattern for a sheet below the bar), `Controls.kt` (`rememberRestartSlot`, the pills), `TopBar.kt` (`drawControlShape`), `PlayDrawing.kt`, `PlayPalette.kt`, `PlaySemantics.kt`, `DevAidHook.kt`;
      - the aid folder (TASK-024's);
      - `app/src/main/kotlin/app/tangramplaytime/app/MainActivity.kt`;
      - the scaffolding tests `ControlsScaffoldTest.kt`, `DecorationScaffoldTest.kt` and `TouchScaffoldTest.kt` in `play/src/test/kotlin/app/tangramplaytime/play/scaffolding/`, yours to keep green.
- **Do not read:**
  - the other briefs, and kit drafts outside the tree;
  - the bodies of `store/src/main`, `kernel/src/main` and `content/src/main`;
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-026 row's Inputs cell.
- **Run:**
  - The kit-compile line, plus `:app:assembleDebug :app:assembleRelease`.
  - The dev-image line, one class per invocation, with a reset and a check before and after. Then the floor image.
  - The JVM line.
  - The V-01 line, with `run_verifiers --wo WO-005`.
  - Greps:
    - `play/src/main` and the aid folder hold no colour literal outside `PlayPalette.kt` and no user-visible string literal (S-12, G-10);
    - `grep -rnE "devaid|DevAid[^H]|0417|dev_aid_|Wrong passcode" play/src/main app/src/main` prints nothing (O-08, G-08).
- **Done means:**
  - The seams exactly as the rows say: the z-order and the hit order exactly as the touch geometry; every path in px (G-17).
  - Without an aid, `PlayScreen` is exactly WO-004's.
  - The only `:app` change is the `devAid` argument.
  - Read-only for you: the kit, `:contracts`, `:store`, `:kernel`, `:content`, `AppGraph.kt`, and S-26/S-27's state and solve as TASK-024 built them. A change there is a recirculation.
  - **Decisions to hand back** as proposed rows: any drawing number that no row fixes (the dash of the button's outline, the field's size, the sheet's spacing, and so on), with its DI source.
  - The Done-check is the TASK-026 row.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with:
  - the inputs read, and the files changed;
  - the commands, their exit codes, the result XML paths and the test counts, once;
  - the proposed decision rows.
