# WO-008 — dispatch briefs (#PlayTime: active play time, puzzle time and best time, the timer on the board)

**Planner:** wo-planner (Opus 5.5)  ·  **Date:** 2026-10-20  ·  **Plan:** `tasks.md` § WO-008 (TASK-043 … TASK-051, in landing order; no task is `[P]`)
**Mode:** Full (b). The design is `designs/WO-008-design.md` (Reviewed; write-back 2026-10-20 for F1, F2 (a), F4 and F5). Its §2 seam table is the oracle every brief points at. `tasks.md` § WO-008 adds seam notes 1–6, the planner seam S-37Δ, the file names (the "Files" row) and the coverage table.

> **Who sees what.** Each brief is dispatched on its own.
> - The Acceptance Test Author gets only its three briefs (TASK-045, TASK-048, TASK-050) and the two tables below. It never sees an implementer brief.
> - An implementer never sees another implementer's brief, nor a kit draft outside the tree.
>
> **Paths and folders.** Every path is relative to `C:/GitHub/AI/TangramPlaytime/` unless it is absolute. Folder names used below:
> - "the kernel kit folder": `kernel/src/test/kotlin/app/tangramplaytime/kernel/`;
> - "the JVM kit folder": `play/src/test/kotlin/app/tangramplaytime/play/` (with its `scaffolding/` subfolder where named);
> - "the play device kit folder": `play/src/androidTest/kotlin/app/tangramplaytime/play/`;
> - "the app device kit folder": `app/src/androidTest/kotlin/app/tangramplaytime/app/`;
> - "your scratch": `<scratch>/wo008/task-0NN/`, the folder the orchestrator names at dispatch. Copied XMLs, logs and probes live there, never in the repo.
>
> **`decisions.md` rows** are cited by date and bold title; rows are only ever appended. The briefs use these short labels:
> - The WO-008 rows (2026-10-20):
>   - **D1** "WO-008 #PlayTime runs before WO-007 #Settings; REQ-029.A2 moves to WO-007."
>   - **D2** "O-04 read for WO-008: the time rules move to `:kernel` …", and **D2a**, its amendment "Amends three 2026-10-20 rows … F2 option (a) …". D2a wins wherever the two differ: there is no `puzzleRuns`, no `Counted` and no `bestAfterSolve`.
>   - **D3** "TYPE-005 as code." (as amended by D2a)
>   - **D4** "What counts on each screen."
>   - **D5** "Today's play time follows the date `Clock.today()` reports."
>   - **D6** "No time the store's reader rejects is ever written …", and **D6a**, its amendment "… a backwards clock reading shifts the last touch by the jump …"
>   - **D7** "An aid solve keeps the best time through one generic controller member." (as amended by D2a)
>   - **D8** "When the times are written, and what a process death may lose."
>   - **D9** "The timer on the board, as drawn." (checkpoint candidate 1)
>   - **D10** "One format for every time the game shows …" (checkpoint candidate 2)
>   - **D11** "All five Shoulds accepted …" (the design-review triage)
>   - **D12** "`architecture.md` v1.4 by the G2 route …"
> - The planner's rows (2026-10-20):
>   - **P1** "REQ-005's two lines are verified as locked, at the seam …"
>   - **P2** "`minSdk 31` leaves the always-true API branches in place …"
>   - **P3** "The reset's crash-dialog blind spot (S-37) is closed in TASK-043 …"
>   - **P4** "Every test edit in WO-008 is the acceptance-test author's …"
>   - **P5** "The play-through's closed world of texts admits the best time …"
>   - **P6** "REQ-031.A2 is proven in `play/` only …"
>
> **Checkpoint candidates.** The design has two: the timer's look (D9) and one time format (D10). The orchestrator asks them on the G3 surface, and the build follows their defaults meanwhile. A different answer changes constants in `BoardTimer.kt` or in `timeOf`, and the kit pins that cite D9 or D10.

## Dispatch order (two lanes)

| Step | Test lane (acceptance-test-author) | Implementer lane (slice-implementer) | Gate to the next step |
|---|---|---|---|
| 0 | — | — (orchestrator) `plan_check.py --wo WO-008` GREEN; G3 (the plan, the channels, the checkpoint candidates asked); the tree at dispatch recorded in the run log | G3 passed |
| 1 | TASK-045 drafts in its scratch | **TASK-043**: the floor; S-37Δ first; then the three images, floor first | TASK-043's Done-check green. The re-judgments and the per-class times are in the hand-back |
| 2 | TASK-045 **lands** | — | TASK-045 met at landing. The red windows open on `kernel/src/test` and `play/src/test` |
| 3 | TASK-048 may start drafting once TASK-046 lands | **TASK-044** | The kernel kit is green. The orchestrator fills the two Contract-deltas rows and re-freezes the baseline |
| 4 | — | **TASK-046** | The play JVM kit is green; so are the five floor classes |
| 5 | TASK-048 **lands in one step with** TASK-047 | **TASK-047** | The JVM line; `TimerKitTest` and `SolvedViewKitTest` on the floor and on dev |
| 6 | TASK-050 **lands** (TASK-048's author, resumed) | — | TASK-050 met at landing. The `app/src/androidTest` window opens |
| 7 | — | **TASK-049** | The `app/` lines on three images; the death pair on the floor and dev |
| 8 | `plan_check.py --wo WO-008` again (the kit has compiled; WO DoD) → **Code Reviewer** (fresh) on the tree after TASK-049. Each fix lands, and its class re-runs alone | | The review is closed |
| 9 | — | **TASK-051**: every channel once, on the final tree | TASK-051's Done-check green |
| 10 | Test & Verify (from the run log; it re-runs only what changed) → trace-check → close | | |

- **Why this order.**
  - **TASK-043 first:** the WO says so. The floor image is new, so every class runs alone there first. That is the riskiest image first, and the cheap per-class run before the whole sets (`platform-notes/android.md`, the WO-006 lesson; `proposals.md` 2026-10-20).
  - **The kit before the seams it pins** (P4). TASK-044 and TASK-046 start with the visible oracles of the design's §3 "Highest-risk part" in the tree.
  - **TASK-048 with TASK-047.** F4's `hoursMinutes` has no default, so neither side compiles alone.
  - **TASK-050 before TASK-049.** The window-route lines exist before the hook they test.
  - **Code review before TASK-051**, so the whole sets run once, on the reviewed tree (the WO-010 dispatch ruling).
- **How step 5 runs.**
  1. TASK-047's implementer writes the main code and compiles the main sources (`:app:assembleDebug`). It then tells the orchestrator; it does not run tests.
  2. The orchestrator resumes TASK-048's author: "TASK-047's main code is in the tree: land TASK-048". The author applies its edits and compiles.
  3. The orchestrator resumes TASK-047's implementer to run its Done-check.
- **Red windows** (`tasks.md` § WO-008 intro). Nobody runs a set for a verdict inside its window. Inside one, run 2 of the JVM line leaves the red module out:
  - between TASK-045 and TASK-044: without `:kernel:test` and `:play:testDebugUnitTest`;
  - between TASK-044 and TASK-046: without `:play:testDebugUnitTest`.
- **Compile errors in the kit.**
  - An unresolved reference to a seam whose task has not landed is expected; each kit row lists which.
  - Any other compile error in a kit file is the kit author's defect, and counts as a corrected check.
  - **Every test edit is the author's** (P4; seam note 5). An implementer whose change breaks a test hands back `recirculate → acceptance-test-author`, naming the file and the line.
- **Decisions the implementers record.** A number or reading that no REQ, seam row or `decisions.md` row fixes goes back as a proposed row, with its source and the alternative. The orchestrator appends it, so each is recorded once.
- **Device-time rules** (`platform-notes/android.md`; KICKOFF "State after WO-010"):
  - the floor image first, one emulator at a time, booted detached;
  - one background script per image, judged by the XML;
  - the 40-minute no-test-finished watchdog;
  - while fixing, the failing class alone; the whole sets once, on the final tree (TASK-051);
  - after a stopped or killed run, reboot the image before its next step;
  - a station never owns an emulator as its own background task.

## Verification channels (G3)

**Conventions for every command below.**
- "The line" is `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat <tasks>"`.
- `ADB` is `/c/Users/Jami/AppData/Local/Android/Sdk/platform-tools/adb`; `EMU` is `/c/Users/Jami/AppData/Local/Android/Sdk/emulator/emulator`.
- Python runs with `PYTHONUTF8=1` (the cp1252 console).
- **Every adb, emulator and Gradle device call is wrapped in `timeout`.** The bounds:
  - a boot 300 s;
  - a single class 3600 s (`OverviewKitTest` takes about 25 min on dev);
  - the whole play set 7200 s (about 45 min on the floor and 54 min on dev, run log t042c);
  - the whole app set's rest 3600 s;
  - the death pair 900 s.

| Channel | How (Git Bash) | Result | Needed by |
|---|---|---|---|
| Python tool channel | `PYTHONUTF8=1 python tools/validate_puzzles.py Tangrams`; `PYTHONUTF8=1 python C:/GitHub/AI/SwReqCollector/tools/validate.py Requirements` | "106/106 puzzle files valid"; "0 errors" (warnings allowed) | TASK-051; Test & Verify |
| JVM (JUnit 4) | **Two runs of the line, in this order** (run log g3-jvm): **(1)** `:app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun`, **then (2)** `:content:testDebugUnitTest --rerun :play:testDebugUnitTest --rerun :kernel:test --rerun :contracts:test --rerun :store:testDebugUnitTest --rerun :app:testDebugUnitTest --rerun`. Run (2) without run (1) first is not a verdict (the WO-005 lesson). In a red window, run (2) leaves the red module out (§ Dispatch order) | `*/build/test-results/**/TEST-*.xml`, each stamped after run (2) started. Baseline g3-jvm: content 80, play 109, kernel 56, contracts 2, store 12, app 4 = 263/0 | every implementer task; TASK-045; Test & Verify |
| Kit compile | The line with `:play:assembleDebugAndroidTest :app:assembleDebugAndroidTest` | the exit code and the compiler messages | every task |
| Lint (once) | The line with `:app:lintDebug :play:lintDebug :content:lintDebug :store:lintDebug`, then search each module's `build/reports/lint-results-debug.*` for `ObsoleteSdkInt` | the findings in product code (expected: none) | TASK-043 |
| **The device script — one per image** | 1. `$ADB emu kill`; `$ADB devices` until it is empty.<br>2. Boot detached: `nohup $EMU -avd <image> -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot > <your scratch>/emu-<image>.log 2>&1 &`.<br>3. Wait for `sys.boot_completed` = 1. `$ADB devices` must show `emulator-5554` alone.<br>4. Record `getprop ro.build.version.sdk`, `wm size`, `wm density` and `settings get secure navigation_mode`.<br>5. `$ADB uninstall app.tangramplaytime` and `app.tangramplaytime.test` ("not installed" is fine).<br>6. `bash tools/device_reset.sh`, then `bash tools/device_reset.sh --check` (clean), then `$ADB shell dumpsys window \| grep mCurrentFocus`.<br>7. The classes, **one class per invocation**: the line with `:play:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.play.<Class>` (or `:app:…` with `app.tangramplaytime.app.<Class>`). A comma list through `cmd.exe //c` runs only its first class. **Whole sets** (TASK-051 only):<br>- the play set: `:play:connectedDebugAndroidTest`;<br>- the app set: two invocations, `…class=app.tangramplaytime.app.DevPromptKeyboardKitTest` alone, then the rest with `-Pandroid.testInstrumentationRunnerArguments.notClass=app.tangramplaytime.app.DevPromptKeyboardKitTest`.<br>8. After each invocation: copy its XML to your scratch (the folder is overwritten), then reset + `--check` + focus.<br>9. The death pair, where the table below says so.<br>10. A final reset + `--check`; `$ADB emu kill`; `$ADB devices` empty.<br>**The watchdog:** if no test has finished for 40 min (no new `TestRunner: finished` line in `$ADB logcat -s TestRunner`), stop the step: end the Gradle invocation, `gradlew --stop`, `$ADB emu kill`. Record it; the image is rebooted before its next step. | `play/build/outputs/androidTest-results/connected/debug/TEST-*.xml`, `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`. Judge a run by its XML, never by Gradle's exit code: the XML exists, is stamped after the command started and carries the expected counts. Under `-q`, a failed install exits 0 and writes no XML | every device step |
| Floor image **`Phone_API_31`** (Android 12; 1080 × 2400 px at 420 dpi = 411 × 914 dp) — **always first** | The script with `-avd Phone_API_31`. Proven: run log g3-floor and `.swdev/run-log/floor-API31.md` (boot 41 s; `--check` clean, with app-locales skipped below API 33) | the same folders (the file name carries the device) | TASK-043, TASK-046, TASK-047, TASK-049, TASK-051 |
| Dev image `Medium_Phone_API_37.0` (411 × 914 dp; gesture navigation, `navigation_mode` 2) | The script with `-avd Medium_Phone_API_37.0` | the same | TASK-043, TASK-047, TASK-049, TASK-051 |
| Tablet image `Medium_Tablet_API_37.0` (2560 × 1600 px at 320 dpi = 1280 × 800 dp, landscape) | The script with `-avd Medium_Tablet_API_37.0`. The app set only: the play set hosts `PlayScreen` at the kit's phone size (WO-006 practice) | the app folder | TASK-043, TASK-049, TASK-051 |
| The process-death pair (ADR-003's row-11 harness; the death pair's times from TASK-050 on) | Inside the floor and dev scripts:<br>1. Build with the line `:app:assembleDebug :app:assembleDebugAndroidTest :app:packageDebug --rerun :app:packageDebugAndroidTest --rerun` (the harness refuses stale APKs).<br>2. `bash tools/process_death_check.sh`, then `bash tools/process_death_check.sh --positive-control`.<br>3. Reset + `--check` | the script's verdict line: `PASS …` / `CONTROL PASS …` and exit 0 | TASK-043, TASK-049, TASK-051 (floor, dev) |
| Release verifier V-01 (G-01, G-08) | After run (1) of the JVM line: `python .swdev/verifiers/v01_release_apk.py`, `python .swdev/verifiers/v01_release_apk.py --positive-control`, then `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-008` | the PASS lines, GREEN, and the budget line (expected: 0 verifiers, 0 lines for WO-008) | TASK-043, TASK-046, TASK-047, TASK-049, TASK-051 |
| Device hygiene | Reset, `--check` and `mCurrentFocus` before and after every device step. From TASK-043's step (a) on, `--check` also names a focused crash or not-responding window (S-37Δ). A non-clean check after a reset voids the step: reset, re-run, record the leftover. A check that names a crash window means a reboot of the image | `--check` clean or not; the focus line | every device step |

**What runs where.** "Alone" means one class per invocation, each with a reset around it.

| Task | Floor `Phone_API_31` | Dev `Medium_Phone_API_37.0` | Tablet `Medium_Tablet_API_37.0` |
|---|---|---|---|
| TASK-043 | S-37Δ's control; every play class alone; every app class alone; the death pair | every play class alone; every app class alone; the death pair | every app class alone |
| TASK-046 | `SolvedViewKitTest`, `ResumeScreenKitTest`, `DevAidScreenKitTest`, `PlayThroughKitTest`, `LanguageKitTest`, alone | — | — |
| TASK-047 | `TimerKitTest`, `SolvedViewKitTest`, alone | the same | — |
| TASK-049 | `ActiveTimeAppKitTest`, `PlayThroughKitTest`, `AppLaunchKitTest`, `AppWiringKitTest`, alone; the death pair | the same | the four app classes, alone |
| TASK-051 | the whole play set; the whole app set (two invocations); the death pair | the same | the whole app set |

**Expected skips.**
- The only legitimate skips are the two guarded `ProcessDeathKitTest` phases in an ordinary connected run.
- On the floor (API 31 < 33), `LanguageKitTest`'s three `@SdkSuppress(minSdkVersion = 33)` legs are filtered: they are absent from the XML, which is not a skip.
- Any other skip in an XML is a failure.

**Other channels.**
- **Not a DoD channel:** human eyes. The owner plays at close, from `review-notes.md`: the timer once switched on (WO-007 adds the switch), the best time after a solve, and the "1 h 5 min" form.
- **Browser:** none.
- **Toolchain proof:** `.swdev/run-log/WO-008.md` rows g3-floor, g3-py, g3-jvm, g3-v01, g3-dev and g3-tablet, on the tree at G3. The process-death harness runs on those proven images; it is unchanged since WO-004.
- **Time (G-14):** product code reads time only through `Clock`, and `AppClock` is the one implementation. Kits move a kit clock and never sleep. Wait for an observed state, always with a bound.

---

## Brief — TASK-045 · WO-008 · acceptance-test-author

- **Mission:** Stage 1 of WO-008's one acceptance kit. It lands after TASK-043, before any time code exists. Three parts:
  1. New `kernel/src/test/kotlin/app/tangramplaytime/kernel/ActiveTimeKitTest.kt`: REQ-029.A1 and S-45's pins.
  2. New `play/src/test/kotlin/app/tangramplaytime/play/PlayTimeKitTest.kt`: REQ-005.A1, REQ-005.A2, REQ-030.A1, REQ-030.A2, REQ-030.A3 and the controller's pins.
  3. The in-tree corrections seam note 2 names:
     - `KitApp.kt`'s closed world and language world (P5);
     - every kit assertion that a player's solve leaves no best time.

  The class is JVM oracles of the WO's highest-risk part, so it goes to the strongest model. Draft while TASK-043 runs, and land when the orchestrator says TASK-043 is done.
- **Read exactly:**
  1. `tasks.md` § WO-008:
     - the intro, all of it: the cut, the lanes, the red windows, seam notes 1–6, the in-tree tests, the lines covered twice, the must-dos, the Contract deltas;
     - § Seams (the "Files" row) and the coverage table;
     - the rows TASK-045 (yours), and TASK-044 and TASK-046, whose Done-checks are your green verdicts.
  2. `designs/WO-008-design.md`:
     - §1;
     - §2: the rows S-44, S-45, S-1Δ3, S-2Δ3, S-20Δ, S-19Δ and S-27Δ, and the paragraphs "Per screen", "Kit doors" and "Tokens";
     - §3, all of it;
     - not §4: the plan's cut replaces it.
  3. `reviews/WO-008-design-review.md` F1, F2 and F5 (why the oracles are shaped as they are).
  4. `workorders/WO-008.md`: § Scope with its notes (must-dos 1–3); § Constraints & notes questions 2–6 and 9; the standing constraints.
  5. Requirements, collection v1.2, read-only:
     - your tokens: `Requirements/reqs/REQ-005.md`, `REQ-029.md`, `REQ-030.md`;
     - the sources of your pins: `REQ-046.md` (rule 4, A3), `REQ-026.md`, `REQ-025.md`, `REQ-031.md` (the setting);
     - `Requirements/req_types.md`: TYPE-005, TYPE-006.
  6. Frozen contracts:
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/Types.kt`: `Clock`, `PlayTimes`, `PuzzleProgress`, `PuzzleState`, `Settings`;
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/IProgressStore.kt`. TASK-044 rewords its lines 19–20 as the design's "`IProgressStore` KDoc Δ" row says.
     - `IActiveTime.kt` does not exist until TASK-044: its members are S-44's row.
  7. `architecture.md` v1.4: §2 G-12, G-13, G-14; §3 O-04; §4 the `IActiveTime` row; §6 ADR-003, ADR-006.
  8. `decisions.md`:
     - D2 with D2a, D3, D4, D5, D6 with D6a, D7, D8, D11; P1, P4, P5;
     - 2026-10-15 "The top bar, Restart and the solved row, as drawn" (the interim "–");
     - 2026-10-15 "When progress is saved" (S-20's save shape);
     - 2026-10-17 "The promise lines' oracles" and "Two readings of the device kit's closed-world checks" (the closed world you amend).
  9. **In-tree kit.** Reuse it. You may add a `kit`-prefixed helper; you change an existing helper or assertion only where this brief says so.
     - The kernel kit folder: `KitFixtures.kt`.
     - The JVM kit folder:
       - `KitPlay.kt`;
       - `KitStore.kt` (`KitProgressStore`, `KitSession`): add a session door that builds the controller with a time source;
       - `DevAidKitTest.kt`, read-only: the aid's door and the REQ-046.A3 tripwires;
       - `ResumeKitTest.kt`: the S-20 save shapes. Read-only unless your grep finds an interim pin there.
     - The app device kit folder: `KitApp.kt` (`closedWorld()`, `languageWorld()`, `assertPromise`; yours to correct); `PlayThroughKitTest.kt` and `LanguageKitTest.kt`, read-only.
  10. **Your grep**, read-only, over the JVM kit folder, the play device kit folder and the app device kit folder. Find every assertion that, after a player's solve (by drag, `solveByApi`, `solveByController` or `drop`):
      - `bestSeconds` is null, the solved row reads "–", or a saved `PuzzleProgress` has no best;
      - an exact text set or saved shape holds that a counted time or a best time would change.

      The aid's assertions keep their meaning (seam note 2).
  11. **Declarations only, never bodies:**
      - `PlayController.kt`: the constructors and the public members;
      - `Persistence.kt`: the constructor, `load`, `save`, `flush`;
      - `play/src/debug/kotlin/app/tangramplaytime/play/devaid/DevAid.kt`: `solveNow`;
      - `Controls.kt`: `bestTimeText`;
      - `app/src/main/kotlin/app/tangramplaytime/app/AppGraph.kt`: `controller`.
  12. Rules:
      - `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2;
      - `C:/GitHub/AI/SWDev/framework/methodology/task-decomposition.md`: "Assert only what is mandated", "Seams name their task; adapters fail loudly", "Declared inputs land together", "Time is injected, never slept";
      - `platform-notes/android.md` § Compose test kit.
- **Do not read:**
  - the main-code bodies of every module and of `play/src/debug`;
  - the implementer briefs (every brief in this file but TASK-045, TASK-048 and TASK-050);
  - DI-1 … DI-10 (the prototype, never);
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-045 row's Inputs cell.
- **Oracle rule:**
  - **Sources.** Every assertion cites, in a comment, the sentence it checks: a REQ sentence, a TYPE rule, a seam row (`S-n`), an architecture line, or a `decisions.md` row by date and title. No source, no assertion.
  - **Tokens.** A REQ token goes only on a test whose assertion is that line's meaning. A pin carries its source and no token. Cite rules in prose, without a dotted ID: trace-check counts every dotted ID it finds.
  - **Time.** The kit owns its `Clock`: it moves the clock and never sleeps. Every action and every clock move runs on the test's one thread (S-44: "One caller thread"), with `countTime()` after each jump (seam note 3).
  - **Every "at most" or "nothing" line has its positive control in the same test** (seam note 3; review F1):
    - REQ-029.A1: after a touch, +30 s counts exactly 30, and after a touch, +5 min counts exactly 60. An exact 60 makes a count of 0 fail.
    - REQ-030.A1: the 2 minutes away count as play time in the same test (D4), so "the puzzle time pauses" is not "nothing counts at all".
    - REQ-030.A3: the same touch and clock on an In-progress puzzle add the seconds.
  - **Fixtures discriminate:**
    - REQ-030.A2: a slower second solve keeps the first best, and a faster third solve replaces it, so both "keep the first" and "keep the last" fail.
    - The aid pin: one puzzle with a saved best and one without (D7).
  - **Mechanics.** No `assume…`. Call `internal` members directly, no reflection. Helper names carry a `kit` prefix.
- **Where the tokens go:**
  - `ActiveTimeKitTest.kt`: REQ-029.A1, on the untouched-five-minutes test.
  - `PlayTimeKitTest.kt`: REQ-005.A1, REQ-005.A2, REQ-030.A1, REQ-030.A2 and REQ-030.A3, each on its asserting test. P1 says what "available" means.
  - No token in `KitApp.kt` or in any correction.
- **What each file needs** (pointers; the REQ and the seam row are the oracle):
  - **`ActiveTimeKitTest.kt`** (S-45; TYPE-005; D3, D5, D6 with D6a):
    - REQ-029.A1 with its control;
    - never negative, over a backwards reading;
    - an idle player stays idle across a backwards jump (F5);
    - whole seconds, with the remainder kept (D3);
    - nothing counts while not visible;
    - a count that spans midnight goes to the new day, and all-time never decreases (D5);
    - a date set back restarts today (D5);
    - `playTimes()` reads today as 0 on another date, before any count;
    - a new `ActiveTime` over saved times counts nothing before its first touch (D3).
  - **`PlayTimeKitTest.kt`** (S-1Δ3, S-2Δ3, S-19Δ, S-20Δ and S-27Δ, over `KitStore.kt`'s double and a real `ActiveTime`):
    - the five tokens;
    - **the F2 pin** (design §3 "Oracles"): an In-progress puzzle in the store → construct → visible, touch, +30 s → `countTime()` → +30, with no other action;
    - **a fold before every change** (design §3 "Fold before every change"): "2 min on B, back to A" credits A nothing; a solve keeps its last seconds; the leaving puzzle is saved in `open`, and the construction saves nothing;
    - **Restart and Retry** reset the running time and keep the best (REQ-030 rule 2);
    - **the aid keeps the best** (REQ-030 rule 4; D7), through `DevAid.solveNow` as `DevAidKitTest` reaches it;
    - **S-20Δ's cadence:** every 10 counted play seconds, the play times and then the open puzzle's save; and the write at `setVisible(false)` (D8);
    - **no negative time reaches the store** across a backwards clock (must-do 2);
    - **`timerShown`** is false on a fresh store and true when seeded (S-1Δ3).
  - **The `KitApp.kt` correction** (P5):
    - `closedWorld()` and `languageWorld()` admit the solved row's best-time text as S-48 forms the controller's `bestSeconds`, and "–" while none is stored;
    - 3600 s or more fails loudly;
    - the kit forms the m:ss itself, because S-48's `timeOf` is `internal` to `:play`.
- **Run:**
  1. Write the files. Landing is your Done-check, met at landing.
  2. Run (1) of the JVM line, then run (2) without `:kernel:test` and `:play:testDebugUnitTest` (the red windows). Content, store, contracts and app are green.
  3. Compile `:kernel:test` and `:play:testDebugUnitTest`, and list every compile error.
     - Expected, and not your defect: unresolved references to S-44's and S-45's names (kernel), and to S-44's names and the new names of S-1Δ3, S-2Δ3, S-19Δ and S-27Δ (play).
     - Any other compile error is yours.
  4. `:app:assembleDebugAndroidTest` compiles.
  5. Grep the kit folders for `REQ-0[0-9]*\.A[0-9]`: exactly the row's list, with the earlier tokens unchanged.
- **Done means:** the TASK-045 row's Done-check, plus hygiene: JUnit 4 only; no new dependency; never edit main code, a build file or `tools/`.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the test names per acceptance ID, and each pin with its source;
  - each correction: the file, the line, before and after, why, and whether it is a corrected check;
  - your grep's findings in the three folders, including the ones you left alone, each with why;
  - the compile status: the commands, and the expected unresolved names;
  - the JVM counts.

  Stay available: TASK-048 and TASK-050 are your later stages, in this context.

---

## Brief — TASK-048 · WO-008 · acceptance-test-author (stage 2; TASK-045's author, context resumed)

- **Mission:** Stage 2 of the kit. Three parts:
  1. F4's five test files, moved to S-11Δ3 and S-48: `hoursMinutes`; `bestTimeText(seconds, texts)`; "60:00" becomes "1 h 0 min".
  2. New `play/src/test/kotlin/app/tangramplaytime/play/TimeFormatKitTest.kt`: S-48's pin, no token.
  3. New `play/src/androidTest/kotlin/app/tangramplaytime/play/TimerKitTest.kt`: REQ-031.A1 and REQ-031.A2.

  **Depends on** (the row's cell): TASK-046, and TASK-047's main code. You draft from TASK-046's landing. You land in one step with TASK-047 (§ Dispatch order, "How step 5 runs"), because your compile check (2) needs TASK-047's field, `timeOf`, `bestTimeText` signature and timer tag in the tree. The class is Compose UI with device-only legs, so it goes to the strongest model.
- **Read exactly** (in addition to your TASK-045 reading):
  1. `tasks.md` § WO-008: the rows TASK-048 (yours) and TASK-047 (its Done-check is your green verdict); seam notes 3, 4 and 5.
  2. `designs/WO-008-design.md`:
     - §2: the rows S-11Δ3 / S-48 and S-13Δ3 / S-47;
     - §3: "No timer on a fresh install or on the solved view", and library facts 5 and 6.
  3. `reviews/WO-008-design-review.md` F4.
  4. Requirements: `Requirements/reqs/REQ-031.md` (your tokens), `REQ-029.md` (rule 2: the format), `REQ-026.md` (the solved view), `REQ-047.md` (the statement: the digits).
  5. `decisions.md`: D4, D9, D10 (the defaults the build follows); 2026-10-19 "The counter keeps ASCII digits under every device language"; P4, P6.
  6. `architecture.md`: G-10 (never assert a Finnish word), G-17.
  7. In-tree:
     - the five F4 sites (the TASK-048 row gives the files and lines);
     - `CounterDigitsKitTest.kt`: its non-ASCII-locale fixture pattern, which you reuse for the format pin;
     - in the play device kit folder:
       - `KitScreen.kt` (`kitTexts`, `KitDeviceStore`, `kitControllerOver`, `KitTag`, the paused-clock helpers). Add a `kit`-prefixed builder that takes a time source; never change an existing one.
       - `SolvedViewKitTest.kt` (how `best-time` is read) and `TopBarKitTest.kt` (how a node's text is read).
  8. **Declarations only:**
     - `PlayTexts.kt`, `Controls.kt` (`bestTimeText`), `PlayTags.kt`;
     - `PlayController.kt`: S-1Δ3's reads `timeSeconds`, `timerShown`, `playTimes`; `countTime`; `setVisible`;
     - `Persistence.kt`: `load`, `settings`.
  9. `platform-notes/android.md` § Compose test kit, all of it: time-driven tests set the injected clock; a tagged display container merges its descendants; `performScrollTo` under a paused clock.
- **Do not read:** as for TASK-045, plus TASK-047's diff beyond its declarations.
- **Inputs:** the TASK-048 row's Inputs cell.
- **Oracle rule:** as for TASK-045, plus:
  - **The timer's text** is read from the `timer` node's merged text (S-47: "An inert merged node whose text is the time"). The expected value is S-48's form of the controller's `timeSeconds`. Each timer leg also reads the counted seconds through the controller, so a timer that shows a stale or wrong value fails.
  - **REQ-031.A1:** over an empty kit store (default `Settings`; the `IProgressStore` KDoc: missing stored state "behaves as a fresh install"), there is no `timer` node on a New puzzle, nor In progress with seconds counted, nor on the solved view. The positive control is in the same class: with the setting seeded on, the node exists.
  - **REQ-031.A2:** with the setting seeded on (`saveSettings` on the kit store before the load):
    - a touch (`time.touched()`), +N s and `countTime()` → the timer shows N's form;
    - idle beyond 60 s → it shows at most 60 more ("active seconds only");
    - it shows "0:00" on a New puzzle and is gone on a Solved one (D9);
    - the shown time does not move while the grid is open (D4).
  - **Pins from D9** (read on the screen in use, from the nodes' bounds; the WO-005 lesson): the timer's place against the `board` node. It takes no touch: it is never a control (S-47).
  - **The format pin** (`TimeFormatKitTest.kt`; S-48, D10):
    - below 3600 s: m:ss with two-digit seconds;
    - from 3600 s: the `hoursMinutes` format with hours and minutes;
    - boundary values on both sides of a minute and of an hour;
    - ASCII digits under a non-ASCII default locale, restored in `finally`, with `CounterDigitsKitTest`'s fixture check that the locale really gives non-ASCII digits;
    - `bestTimeText(null, texts)` is `noTime`.
  - **"60:00" becomes "1 h 0 min"** (D10). It is a corrected check: its source was WO-004's m:ss-only rendering.
- **Where the tokens go:** `TimerKitTest.kt`: REQ-031.A1 and REQ-031.A2. None in `TimeFormatKitTest.kt` or in the F4 edits.
- **Run:**
  1. Draft in your scratch.
  2. When the orchestrator says TASK-047's main code is in the tree, apply your edits and new files.
  3. Kit compile, then run (1) and run (2) of the JVM line: every module green, `TimeFormatKitTest` and `ControlsScaffoldTest` included. A red play JVM test is reported with your reading of whose it is.
  4. Grep for the tokens.

  The device verdict is TASK-047's (floor, then dev).
- **Done means:** the TASK-048 row's Done-check, with the TASK-045 hygiene.
- **Return:** the handoff block, with:
  - each F4 edit: before, after, and whether it is a corrected check;
  - the format pin's cases and its locale evidence;
  - `TimerKitTest`'s legs and their controls;
  - the compile and JVM results;
  - the grep.

  Stay available for TASK-050.

---

## Brief — TASK-050 · WO-008 · acceptance-test-author (stage 3; the same author, context resumed)

- **Mission:** Stage 3 of the kit: the `app/` lines that only the real app can prove. Three parts:
  1. New `app/src/androidTest/kotlin/app/tangramplaytime/app/ActiveTimeAppKitTest.kt`: the review's F1 lines through the window, each with its control. REQ-029.A1 and REQ-031.A1 go there as extra tokens; the stopped legs are TYPE-005 pins.
  2. `ProcessDeathKitTest.kt`'s two phases carry the times, with REQ-005.A2 as an extra token on `phase2`.
  3. `KitApp.kt` gets the helpers.

  It lands before TASK-049, which opens a red window on `app/src/androidTest` until TASK-049 lands. The class is device-only, so it goes to the strongest model.
- **Read exactly** (in addition to your earlier reading):
  1. `tasks.md` § WO-008: the rows TASK-050 (yours) and TASK-049 (its Done-check is your green verdict); seam notes 3, 4 and 5; the coverage table.
  2. `designs/WO-008-design.md`:
     - §2: the rows S-46 and S-15Δ3, and "Kit doors" for the `app/` device (as amended by P6);
     - §3: "Backwards readings (F5)", "Recreation and death", and library facts 1, 2, 3 and 7.
  3. `reviews/WO-008-design-review.md` F1 and F5.
  4. Requirements: `Requirements/reqs/REQ-029.md` (A1), `REQ-031.md` (A1), `REQ-005.md` (A2); `REQ-003.md` and `REQ-025.md` (`phase2`'s existing tokens); TYPE-005.
  5. `decisions.md`: D3, D5, D6a, D8; P1, P4, P5, P6.
  6. The death pair's origin: `designs/WO-004-design.md` §2 S-24 and §3 "Highest-risk part"; `tasks.md` § WO-004 seam note 1; § WO-006 seam note 3 (every app-kit class leaves the controller and the device as it found them).
  7. `architecture.md`: G-14; §6 ADR-003 (the row-11 test) and ADR-006.
  8. In-tree:
     - in the app device kit folder:
       - `KitApp.kt`, all of it (the bounded waits, `onMain`, the steady-activity wait, `threeToLeave`, `awaitSolvedRow`). You add `kit`-prefixed helpers.
       - `ProcessDeathKitTest.kt`: yours to extend. Each phase keeps one finished test, and the guard stays.
       - `AppWiringKitTest.kt` and `AppLaunchKitTest.kt` as patterns; `PlayThroughKitTest.kt` read-only.
     - `tools/process_death_check.sh` (how a phase passes) and `tools/device_reset.sh`, read-only.
  9. **Declarations only:**
     - `AppGraph.kt` (`controller`; S-15Δ3 adds `touched` and `visible`);
     - `MainActivity.kt`;
     - S-46's `AppClock`, which is not in the tree until TASK-049: its row is the declaration;
     - `PlayController.kt`'s reads.
  10. `platform-notes/android.md`, all of it.
- **Do not read:** as for TASK-045.
- **Inputs:** the TASK-050 row's Inputs cell.
- **Oracle rule:** as for TASK-045, plus:
  - **Touches go through the window only** (seam note 3):
    - `Instrumentation.sendPointerSync`, DOWN then UP, at an inert point: the board's empty margin;
    - read the point on the screen in use, and assert that it lies inside `board` and outside every piece and control;
    - display coordinates are the node's window bounds plus the window's location on the screen;
    - never `performTouchInput` or `performClick` for a time line.
  - **The kit clock:**
    - `AppClock.override` is a kit clock that starts at `SystemClock.elapsedRealtime()` and `LocalDate.now()`;
    - it moves on the main thread, and `countTime()` runs on the main thread after each move (or the next tick of the ticker is awaited, with a bound);
    - it is reset to null in `finally`. That backwards jump is S-45's F5 case.
  - **The seconds are read through `AppGraph.controller`** (`playTimes`, `timeSeconds`), never from the timer. The setting stays off (P6).
  - **Untouched:** a touch, +30 s → +30 s (the control); a touch, +5 min → exactly +60 s. That is REQ-029.A1, on the play time: today's and all-time.
  - **Stopped**, with the lifecycle read back before each clock move:
    - a touch, +20 s → +20 s (the control);
    - a touch, Home at once, +5 min, return → +0 s;
    - the same with the screen off: `input keyevent KEYCODE_SLEEP`, then `KEYCODE_WAKEUP` and `wm dismiss-keyguard`. Design library fact 1 asks for this check on the floor; your class runs it on every image.
  - **REQ-031.A1, the app leg:** over the app's own store (cleared by the reset before the run), there is no `timer` node on a New puzzle, In progress or Solved. Liveness: the board exists, `timerShown` reads false, and `timeSeconds` is above 0 after a counted leg, so a timer, if drawn, would have something to show. The positive control with the setting on is `play/`'s (P6).
  - **The death pair:**
    - `phase1` (deathPhase 1): after its three pieces are placed (In progress), a touch through the window, the kit clock +N s and `countTime()`, then the flush. N is a multiple of S-20Δ's 10-second cadence, so every counted second is on disk before the force-stop (D8).
    - `phase2`: a new process, with no override and no touch. The fixture puzzle's `timeSeconds` is N, and the all-time play time is N (REQ-005.A2), besides the three pieces (REQ-003.A2 and REQ-025.A1, unchanged).
    - Each phase stays one finished test (the script's rule), and the guard stays.
  - **Every class leaves the controller and the device as it found them:** the open puzzle and its state, `AppClock.override` null, the screen on and unlocked, the app in front.
- **Where the tokens go:** `ActiveTimeAppKitTest.kt`: REQ-029.A1 on the untouched leg and REQ-031.A1 on the cleared-app leg. `ProcessDeathKitTest.kt` `phase2`: REQ-005.A2 added to its token comment. None in `KitApp.kt`.
- **Run:**
  1. Write.
  2. Kit compile. The only errors in `:app:assembleDebugAndroidTest` are unresolved references to S-46's names (`AppClock`, its override); `:play:assembleDebugAndroidTest` is unaffected.
  3. Grep for the tokens.

  The device verdicts are TASK-049's.
- **Done means:** the TASK-050 row's Done-check, with the TASK-045 hygiene.
- **Return:** the handoff block, with:
  - each leg, its control and its source;
  - the window-touch helper's route and how it finds an inert point;
  - the death pair's N and why;
  - the compile result (the expected unresolved names);
  - the grep.

---

## Brief — TASK-043 · WO-008 · slice-implementer

- **Mission:** Move the code and the channels onto the floor, in this order:
  - **(a)** S-37Δ in `tools/device_reset.sh`, with its positive control on the floor.
  - **(b)** `minSdk = 31` in `app/build.gradle.kts`, `content/build.gradle.kts`, `play/build.gradle.kts` and `store/build.gradle.kts`.
  - **(c)** Lint once, for `ObsoleteSdkInt`.
  - **(d)** Every class of the play and app sets alone, floor first, then dev, with the death pair on both.
  - **(e)** Every app class alone on the tablet.
  - **(f)** Each API 26 history rule re-judged on `Phone_API_31`.

  REQ-006.A1 is re-covered on the new floor. The class is device-only, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-008: the intro (seam note 6, the channels, the device time); § Seams S-37Δ; the TASK-043 row.
  2. `workorders/WO-008.md`: § Scope's notes ("REQ-006.A1 is re-covered on the new floor"); § Constraints & notes, "First task"; the standing constraints and the verification channels.
  3. `Requirements/reqs/REQ-006.md`; `Requirements/req_types.md` TYPE-007. Also `capture-outbox/chg-draft-req006-platform-floor.md` and `chg-draft-req006-a1-playable-oracle.md`: both pending, and the build follows the Contract as locked.
  4. `architecture.md` v1.4: §5 (the stack line, the channels); G-17.
  5. `decisions.md`:
     - 2026-10-19 "The platform floor moves to Android 12 (API 31) …";
     - 2026-10-20 "The floor move, steps 1–2 and 4 done …";
     - P2, P3;
     - 2026-10-18 "The kit fix is accepted; two more kit defects …" (4: the blind spot and the interim rule);
     - 2026-10-17 "The floor routes, as measured — amends the Q7 and Q8 rows" (API 26 routes you re-judge);
     - 2026-10-18 "A comma never reaches an instrumentation argument …".
  6. `platform-notes/android.md`, all of it; KICKOFF.md "State after WO-010" (the device-time line); `.swdev/run-log/WO-008.md` (Env, the tree-hash definitions, the g3 rows); `.swdev/run-log/floor-API31.md`.
  7. The files you change: the four build files and `tools/device_reset.sh`.
  8. The visible tests, which you run and never edit:
     - every class of the play device kit folder and of the app device kit folder;
     - `app/src/androidTest/kotlin/app/tangramplaytime/app/PlayThroughKitTest.kt` (REQ-006.A1's token);
     - `tools/process_death_check.sh`;
     - the JVM kits.
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-043 row's Inputs cell.
- **Constraints:**
  - **(a) S-37Δ** (its seam row is the spec).
    - The match is on `dumpsys window`'s `mCurrentFocus` line, for a title starting "Application Error" or "Application Not Responding". The reset never dismisses such a window.
    - **The positive control:** a crash or not-responding window provoked over a started app on the floor.
      - Record the route you used. Any setting it changes is restored and read back.
      - Never hang or crash `system_server`.
      - The row's Done-check (3) has two outcomes, and either one meets it. **Provoked:** `--check` exits 1 naming the window, and is clean after a reboot. **Not provoked:** no route raises such a window within about 15 minutes of trying. Then keep the item as built, and record every route tried with its command and the `mCurrentFocus` line it left; `--check` is clean afterwards. The orchestrator rules at receive.
    - The exit codes and the old verdicts stay: after a reset, `--check` is clean on each image.
  - **(b) Only the `minSdk` line** of each of the four files changes. `:contracts` and `:kernel` are pure JVM and do not change. `compileSdk` and `targetSdk` stay.
  - **(c) Lint findings:** a product-code finding is handed back (P2: a `recirculate → planner`). Lint does not check test sources by default, so they are out.
  - **(d)–(e) Device runs:**
    - Follow the channel table and "What runs where".
    - One background script per image, floor first, under the 40-minute watchdog.
    - One class per invocation, with reset + `--check` + focus around each, and both packages uninstalled after each boot.
    - Copy each XML; record each run's tree hash per the run log's definitions.
    - **A red class:** finish the image's script, then hand back with the XML line. Never edit a kit or a product file.
  - **(f) The re-judgments.** For each rule of `platform-notes/android.md` marked as API 26 history, give a verdict on `Phone_API_31` (holds, does not hold, or not exercised by WO-008) with the command and its read-back, and propose the platform-notes line. The orchestrator edits platform-notes; you do not. The rules:
    - the launcher crashes when the display changes while the home screen is in front, and so the floor is rebooted after a stopped run;
    - a forced width is clamped to twice the initial width;
    - an override over a running activity is a configuration change, not a recreation;
    - the floor's limits: no gesture bar (record `navigation_mode`), `svc wifi`, the launcher relaunching after a display change;
    - "API 26 is not edge to edge": compare the board's top on the floor and on dev, read from a kit run;
    - the Compose read that parked across a relaunch;
    - the locale and airplane-mode routes: on API 31 the kit sets airplane mode itself (`sdk >= 30`), and `cmd locale` is absent;
    - `show_ime_with_hard_keyboard`'s default.
  - **The always-true API branches stay** (P2). Change nothing in a kit or the script but S-37Δ.
- **Run:**
  1. S-37Δ, and its control on the floor (boot, the control, reset, check).
  2. `minSdk`; the JVM line (1) + (2); V-01 and its positive control; the badging of both APKs; lint; kit compile.
  3. The floor script.
  4. The dev script.
  5. The tablet script.
- **Done means:** the TASK-043 row's Done-check (1)–(7).
- **Return:** the handoff block, with:
  - the diff: four lines, plus the script item;
  - the JVM counts per module; V-01's lines; the minSdkVersion from the badging;
  - S-37Δ's control: the route, the `mCurrentFocus` line, the `--check` output;
  - per image and per class: the counts, the time, the XML stamp and the tree hash;
  - the death pair's lines;
  - the lint result;
  - the re-judgment table (rule · verdict · command · read-back · proposed line);
  - anything that voided a step.

---

## Brief — TASK-044 · WO-008 · slice-implementer

- **Mission:** Three changes:
  - **S-44:** new `contracts/src/main/kotlin/app/tangramplaytime/contracts/IActiveTime.kt`, a governed interface: the banner, tier notify, each member's REQ (D3).
  - **S-45:** new `kernel/src/main/kotlin/app/tangramplaytime/kernel/ActiveTime.kt`.
  - **The `IProgressStore` KDoc Δ.**

  The class is pure-JVM time arithmetic with must-nevers (never negative; idle stays idle), so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-008: the intro (red windows, Contract deltas, must-dos), the "Files" row, the TASK-044 row.
  2. `designs/WO-008-design.md`:
     - §1, "G2 route": the registry row and O-04;
     - §2: the rows S-44, S-45 and "`IProgressStore` KDoc Δ";
     - §3: "Must-nevers", "Backwards readings (F5)", "One thread", and library fact 3.
  3. `reviews/WO-008-design-review.md` F3 (the KDoc) and F5.
  4. Requirements: `Requirements/reqs/REQ-029.md`, `REQ-005.md`, and `REQ-030.md` (the statement: the seconds it credits); `Requirements/req_types.md` TYPE-005.
  5. Contracts:
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/Types.kt` (`Clock`, `PlayTimes`);
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/IProgressStore.kt` (the banner's pattern, and lines 19–20);
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/ILockEngine.kt` (another banner).
  6. `architecture.md` v1.4: §2 G-12, G-14; §3 O-04; §4, the `IActiveTime` row and the marker convention; §6 ADR-002, ADR-006.
  7. `decisions.md`: D2 with D2a, D3, D5, D6 with D6a, D12; P4.
  8. Design inputs: `design-inputs.md` §2, "Also fine", the "Time counting" line (DI-1, DI-3 §8, DI-8: the owner's "about a minute").
  9. The visible tests, never edited:
     - `kernel/src/test/kotlin/app/tangramplaytime/kernel/ActiveTimeKitTest.kt` (TASK-045's);
     - the kernel kit folder's `LockEngineKitTest.kt`, `GeometryKitTest.kt`, `LayoutClassKitTest.kt`.
  10. Code: `kernel/src/main/kotlin/app/tangramplaytime/kernel/LockEngine.kt`, the pattern of a `:kernel` implementation of a governed interface.
  11. `.swdev/guard.json`: `contract_paths.notify` lists both contract files, so each write logs a `contract-delta` event. That is expected; the orchestrator records it.
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10 beyond the line named above.
- **Inputs:** none; the kernel kit reads no file outside its module.
- **Constraints:**
  - The names and signatures are exactly S-44's row.
  - The banner is `IProgressStore.kt`'s, with tier notify and the registry row's "Serves".
  - `ActiveTime` has no locks (one caller thread). It reads time only from the injected `Clock` (G-14) and imports no `android.*` (G-12).
  - The KDoc Δ wording is the design's row, with no signature change.
  - No `:play` or `:app` change, and no test edit (seam note 5).
- **Run:** run (1) of the JVM line, then run (2) without `:play:testDebugUnitTest` (its red window lasts until TASK-046); `:app:assembleDebug`.
- **Done means:** the TASK-044 row's Done-check.
- **Return:** the handoff block, with:
  - the files and member list;
  - the counts per module;
  - the two `contract-delta` guard-log lines;
  - anything in S-45 that the row leaves open, as a proposed `decisions.md` row.

---

## Brief — TASK-046 · WO-008 · slice-implementer

- **Mission:** S-19Δ, S-1Δ3, S-2Δ3, S-20Δ and S-27Δ: the controller folds each count into the open puzzle, keeps the best time, and writes through `Persistence`. The fold-and-save order is the design's highest-risk part, and it is silent on screen. The class is concurrency on the main thread over the one FIFO writer, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-008: the intro (seam notes 1, 2 and 5; the must-dos), the "Files" row, the TASK-046 row.
  2. `designs/WO-008-design.md`:
     - §1: "How it works", and must-do 3;
     - §2: the rows S-1Δ3, S-2Δ3, S-20Δ, S-19Δ and S-27Δ, and "Per screen";
     - §3, all of it: fold before every change, the highest-risk part, recreation and death, one thread.
  3. `reviews/WO-008-design-review.md`: F2 (option a); "Fold order on the real code"; "Constructor overloads".
  4. Requirements: `Requirements/reqs/REQ-005.md`, `REQ-030.md`, `REQ-046.md` (rule 4, A3), `REQ-025.md`, `REQ-026.md`; `Requirements/req_types.md` TYPE-006.
  5. Contracts: `contracts/src/main/kotlin/app/tangramplaytime/contracts/IActiveTime.kt` (TASK-044's), `IProgressStore.kt`, `Types.kt`.
  6. `architecture.md`: G-08, G-12, G-13, G-14, G-15; O-04, O-08 (`solveWithoutBest` names no aid); ADR-003.
  7. `decisions.md`: D2 with D2a, D4, D6, D7 (as amended), D8; 2026-10-15 "O-04 read for this WO"; 2026-10-17 "Solve this puzzle now is an ordinary solve" (amended by D7).
  8. Design inputs: `design-inputs.md` §2 item 11 (DI-1's 5-second cadence is not a rule) and the "Time counting" line.
  9. The visible tests, never edited:
     - the JVM kit folder's `PlayTimeKitTest.kt` (your IDs), `DevAidKitTest.kt` (the REQ-046.A3 tripwire), `ResumeKitTest.kt`, `PlayControllerKitTest.kt`, `BrowsingKitTest.kt`, and the scaffolding tests;
     - `kernel/src/test/kotlin/app/tangramplaytime/kernel/ActiveTimeKitTest.kt`;
     - for the floor step: `SolvedViewKitTest.kt`, `ResumeScreenKitTest.kt`, `DevAidScreenKitTest.kt` (play device kit folder) and `PlayThroughKitTest.kt`, `LanguageKitTest.kt` (app device kit folder).
  10. Code: `play/src/main/kotlin/app/tangramplaytime/play/PlayController.kt`, `play/src/main/kotlin/app/tangramplaytime/play/Persistence.kt`, `play/src/debug/kotlin/app/tangramplaytime/play/devaid/DevAid.kt`.
  11. `platform-notes/android.md`: § Verification channels and § Device hygiene.
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10 beyond the lines named above.
- **Inputs:** the TASK-046 row's Inputs cell.
- **Constraints:**
  - **Construction:** every existing construction compiles unchanged (seam note 1).
  - **The fold:** the fold-first list is exactly S-2Δ3's. The construction's first `load` neither folds nor saves.
  - **The aid:** `solveWithoutBest` is `internal`, with its flag in `try` / `finally`.
  - **The writes:** the cadence is S-20Δ's (D8). `savePlayTimes` goes through the one FIFO writer (G-13).
  - **What `:app` reads:** `Persistence`'s play times and settings are readable by `:app`, because S-15Δ3 reads `persistence.playTimes`.
  - **Not yours:** `timeOf`, the timer and the strings (TASK-047); `:app` (TASK-049); any test file (seam note 5: a broken test → `recirculate → acceptance-test-author`).
- **Run:**
  1. The JVM line (1) + (2), every module.
  2. Kit compile.
  3. V-01 and its positive control.
  4. The floor script, with the five classes of the TASK-046 row's Done-check (4).
- **Done means:** the TASK-046 row's Done-check.
- **Return:** the handoff block, with:
  - the fold points as built (each action, then where it folds and saves);
  - the counts per module; V-01's lines;
  - the floor XMLs, with their stamps and tree hashes;
  - any red test, with its file and line and your reading of whose it is.

---

## Brief — TASK-047 · WO-008 · slice-implementer

- **Mission:** The one time format and the timer on the board:
  - S-11Δ3: the string, the `PlayTexts` field, the `MainActivity` line;
  - S-48: `timeOf` and `bestTimeText`;
  - S-13Δ3 and S-47: the tag and the timer composable;
  - one `review-notes.md` row.

  You land in one step with TASK-048 (§ Dispatch order, "How step 5 runs"). The class is UI and rendering, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-008: the intro (the one-step landing), the "Files" row, the TASK-047 row.
  2. `designs/WO-008-design.md`:
     - §2: the rows S-11Δ3 / S-48 and S-13Δ3 / S-47;
     - §3: "No timer on a fresh install or on the solved view", and library facts 5 and 6.
  3. `reviews/WO-008-design-review.md` F4.
  4. Requirements: `Requirements/reqs/REQ-031.md`, `REQ-029.md` (rule 2), `REQ-026.md`, `REQ-030.md` (rule 3), `REQ-047.md`.
  5. `architecture.md`: G-10, G-17, O-05, O-06.
  6. `decisions.md`: D4, D9, D10; 2026-10-19 "The counter keeps ASCII digits under every device language"; 2026-10-15 "The top bar, Restart and the solved row, as drawn" (the Restart pill, which the timer mirrors).
  7. Design inputs (behaviour references only, never an oracle):
     - DI-2: `Spec/02-ui-layout.md` §2 (the sketch's "⏱ 1:23");
     - DI-1: `Spec/prototype/tangram-prototype.html`, the `.timer` style and `fmt`;
     - DI-3: `Spec/01-gameplay.md` §8.
  8. The visible tests, never edited. They land with TASK-048: `TimerKitTest.kt`, `TimeFormatKitTest.kt`, `scaffolding/ControlsScaffoldTest.kt`; and the play device kit folder's `SolvedViewKitTest.kt`.
  9. Code and files:
     - `PlayTexts.kt`;
     - `Controls.kt` (`rememberRestartSlot`, `RestartPill`, `bestTimeText`);
     - `PlayTags.kt`, `PlayScreen.kt`, `MainActivity.kt`;
     - `app/src/main/res/values/strings.xml`, `app/src/main/res/values-fi/strings.xml`;
     - `review-notes.md` (the row format).
- **Do not read:** the kit author's briefs; the other implementer briefs.
- **Inputs:** the TASK-047 row's Inputs cell.
- **Constraints:**
  - **`timeOf`** stays `internal` until WO-007. Digits are formatted with `Locale.ROOT`.
  - **`hoursMinutes`** comes from the string resource, in `en` and `fi`. It has no default (G-10).
  - **The timer:**
    - it is never in `ShownControls`, and it takes no touch;
    - it reads `timeSeconds` in its own composable (library fact 5);
    - its paths are in px (G-17), with tabular digits;
    - it is hidden while the setting is off and on a Solved puzzle (D9);
    - it is layered after the Restart pill and before the pieces.
  - **Never edit a test file** (seam note 5).
- **Run:**
  1. Write the main code; compile it (`:app:assembleDebug`); tell the orchestrator.
  2. After TASK-048 lands: the JVM line (1) + (2); kit compile; V-01 and its positive control.
  3. The floor script, then the dev script, each with `TimerKitTest` and `SolvedViewKitTest` alone.
- **Done means:** the TASK-047 row's Done-check.
- **Return:** the handoff block, with:
  - the timer's slot as drawn (from which nodes, in dp);
  - the format's cases;
  - the counts per module; V-01's lines;
  - the XMLs, with their stamps and tree hashes;
  - the review-notes row.

---

## Brief — TASK-049 · WO-008 · slice-implementer

- **Mission:** S-46 `AppClock`, the one `Clock` (G-14), and S-15Δ3, the wiring:
  - `AppGraph` builds `ActiveTime` and hands it to the controller;
  - `AppGraph` forwards touches and visibility, and runs the 250 ms ticker;
  - `MainActivity` reports the window's touches and its start and stop.

  The class is the app shell's lifecycle, input and coroutines on three images, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-008: the intro (seam notes 3 and 4; the `app/src/androidTest` red window), the "Files" row, the TASK-049 row.
  2. `designs/WO-008-design.md`:
     - §2: the rows S-46 and S-15Δ3;
     - §3: "Recreation and death"; "Carried to WO-007" (informative: the settings screen stays in the activity's window); library facts 1, 2, 3, 4 and 7.
  3. `reviews/WO-008-design-review.md` F1.
  4. Requirements: `Requirements/reqs/REQ-029.md`, `REQ-005.md`, `REQ-031.md`, `REQ-006.md`; `Requirements/req_types.md` TYPE-005.
  5. `architecture.md`: G-12, G-14, O-05; §6 ADR-006.
  6. `decisions.md`: D3, D8, P6.
  7. The visible tests, never edited:
     - the app device kit folder's `ActiveTimeAppKitTest.kt`, `ProcessDeathKitTest.kt` (with `tools/process_death_check.sh`), `PlayThroughKitTest.kt`, `AppLaunchKitTest.kt`, `AppWiringKitTest.kt`.
  8. Code: `app/src/main/kotlin/app/tangramplaytime/app/AppGraph.kt`, `app/src/main/kotlin/app/tangramplaytime/app/MainActivity.kt`.
  9. **Declarations only:** `PlayController.kt` (`countTime`, `setVisible`); `Persistence.kt` (`playTimes`); `kernel/src/main/kotlin/app/tangramplaytime/kernel/ActiveTime.kt`; `contracts/src/main/kotlin/app/tangramplaytime/contracts/IActiveTime.kt`.
  10. `platform-notes/android.md`, all of it; `.swdev/run-log/WO-008.md`.
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10.
- **Inputs:** the TASK-049 row's Inputs cell.
- **Constraints:**
  - **`AppClock`** is the only `Clock`: `SystemClock.elapsedRealtime()` and `LocalDate.now()`, behind `@Volatile internal var override` (S-46).
  - **`MainActivity`:**
    - `dispatchTouchEvent` calls `AppGraph.touched()` before `super`;
    - `onStart` and `onStop` call `visible`, and `onStop` does so after `goHome`.
  - **`AppGraph`:**
    - the ticker runs `countTime()`, then `delay(250)`, on `mainScope`, and only while the app is visible;
    - before the controller exists, `visible` is remembered and given to it at construction.
  - **Idling:** the ticker must not keep the device kits from idling (library fact 4). If a class stalls under it, hand back with the stack; never edit a kit.
  - No string, no permission (G-01), no new dependency. Never edit a test file (seam note 5).
- **Run:**
  1. The JVM line (1) + (2); kit compile; V-01 and its positive control.
  2. The floor script, then the dev script, then the tablet script, per "What runs where", with the death pair on the floor and dev.
- **Done means:** the TASK-049 row's Done-check.
- **Return:** the handoff block, with:
  - how the ticker starts and stops (which callback, on which thread);
  - the counts per module; V-01's lines;
  - per image: each class's XML counts, stamp and tree hash;
  - the death pair's verdict lines.

---

## Brief — TASK-051 · WO-008 · slice-implementer

- **Mission:** Run every channel once, on the final, reviewed tree:
  - **(a)** the Python channel;
  - **(b)** the JVM line;
  - **(c)** the floor first, then dev: the whole play set, the whole app set (two invocations) and the death pair; then the tablet: the whole app set;
  - **(d)** V-01 and its positive control.

  You make no product, kit, data or tool change of your own. The class is device-only, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-008: the intro (the channels, the device time, the lines covered twice), the coverage table, the TASK-051 row.
  2. `workorders/WO-008.md`: § Constraints & notes (the verification channels, the device time); § Definition of Done.
  3. Requirements, read-only: `Requirements/reqs/REQ-005.md`, `REQ-006.md`, `REQ-029.md`, `REQ-030.md`, `REQ-031.md`.
  4. `decisions.md`: P1–P6; 2026-10-18 "A comma never reaches an instrumentation argument …" (why the app set takes two invocations).
  5. Every kit folder, read-only: you run them.
  6. `platform-notes/android.md`, all of it; `.swdev/run-log/WO-008.md`, every row so far; the per-class times of TASK-043's hand-back (the orchestrator gives them to you).
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10.
- **Inputs:** the TASK-051 row's Inputs cell.
- **Constraints:**
  - **Routing.** A red test is routed, not fixed. It goes to the kit stage that wrote it (TASK-045, TASK-048 or TASK-050) or to the task that delivered its seam. Report the test, the XML line and a short reading of why; the orchestrator dispatches.
  - **Re-runs.** After a fix, the failing class runs alone on that image, then the whole sets run once more, on the final tree.
  - **Skips** follow "Expected skips" in the channel section.
  - **The tablet** runs the app set only.
- **Run:** the Python channel → the JVM line → V-01 → kit compile → the floor script → the dev script → the tablet script. Use the channel table's timeouts.
- **Done means:** the TASK-051 row's Done-check (1)–(4).
- **Return:** the handoff block, with:
  - per channel: the counts, and each XML's stamp and tree hash;
  - per image: each class's time in the whole set, against TASK-043's per-class times;
  - every red test and where it was routed;
  - V-01's lines.
