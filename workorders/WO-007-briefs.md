# WO-007 — dispatch briefs (#Settings: the settings screen, the timer and sound switches, sounds and the haptic tick, the play times, reset)

**Planner:** wo-planner (Opus 5.5)  ·  **Date:** 2026-10-21  ·  **Plan:** `tasks.md` § WO-007 (TASK-052 … TASK-062, in landing order; TASK-058 before TASK-057; no task is `[P]`)
**Mode:** Full (b). The design is `designs/WO-007-design.md` (Reviewed; write-back 2026-10-21 for F1–F5; F1 re-checked and resolved). Its §2 seam table is the oracle every brief points at. `tasks.md` § WO-007 adds seam notes 1–7, the "Files" and "Kit files" rows, the in-tree tests the WO moves, and the coverage table. No seam changed at planning.

> **Who sees what.** Each brief is dispatched on its own.
> - The Acceptance Test Author gets only its four briefs (TASK-052, TASK-055, TASK-058, TASK-060) and the two tables below. It never sees an implementer brief.
> - An implementer never sees another implementer's brief, nor a kit draft outside the tree.
>
> **Paths and folders.** Every path is relative to `C:/GitHub/AI/TangramPlaytime/` unless it is absolute. Folder names used below:
> - "the kernel kit folder": `kernel/src/test/kotlin/app/tangramplaytime/kernel/`;
> - "the JVM kit folder": `play/src/test/kotlin/app/tangramplaytime/play/` (with its `scaffolding/` subfolder where named);
> - "the play device kit folder": `play/src/androidTest/kotlin/app/tangramplaytime/play/`;
> - "the app JVM folder": `app/src/test/kotlin/app/tangramplaytime/app/`;
> - "the app device kit folder": `app/src/androidTest/kotlin/app/tangramplaytime/app/`;
> - "the REQ folder": `Requirements/reqs/` (collection v1.2, read-only, G-02);
> - "your scratch": `<scratch>/wo007/task-0NN/`, the folder the orchestrator names at dispatch. Copied XMLs, logs and probes live there, never in the repo.
>
> **`decisions.md` rows** are cited by date and bold title; rows are only ever appended. The briefs use these short labels:
> - The WO-007 rows (2026-10-21):
>   - **E1** "Scope of WO-007: 16 IDs, the privacy text included, the "every screen" lines re-covered, the licence line built ahead of its draft."
>   - **E2** "The gear and the settings screen, as drawn.", and **E2a**, its amendment "Amends … the top bar's second line shrinks to fit the narrower counter." (checkpoint candidate 1)
>   - **E3** "The settings screen sits in the activity's window, pauses the puzzle time and is the only open overlay."
>   - **E4** "The two switches write through `PlayController` and `Persistence` and take effect at once. …", and **E4a**, its amendment "Amends … (its kit rule): the `app/` kit's doors know the settings layer." E4a wins where the two differ: the switches are restored through `onRestore`, not "in `finally` (timer off, sound on)".
>   - **E5** "Play time and best times on the settings screen." (checkpoint candidate 2)
>   - **E6** "Reset all progress, in order, and where the player is afterwards." (checkpoint candidate 3), and **E6a** "Adds the oracles to … a JVM restart leg over the real file store carries the restart …"
>   - **E7** "The five sounds and the tick." (checkpoint candidate 4), and **E7a**, its amendment "Amends … the tap click follows the sound switch through `Feedback`; "No system click" was false." E7a wins where they differ.
>   - **E8** "REQ-033.A1's oracle is one door, driven on the device with the switch off and then on …", and **E8a**, its amendment "Amends … the door carries the click too, and the sound-off leg presses the click path." E8a wins where they differ.
>   - **E9** "REQ-029.A2 meets trace-check's one-code-home rule because `#ActiveTime`'s code home moves from `kernel/` to `app/` …"
>   - **E10** "The closed world of texts admits the settings screen's times …"
>   - **E11** "F1 (Blocker) and F2–F5 all go back to the same design author; …" (the design-review triage)
>   - **E12** "The moved-› pins in `play/` land in the same step as S-53, as declared outputs, each a corrected check."
>   - **E13** "`architecture.md` v1.5 by the G2 route: `IActiveTime` gains `resetPlayTimes()` …"
> - The planner's rows (2026-10-21):
>   - **PL1** "The WO-007 kit lands in four stages, each with or before what it pins; …"
>   - **PL2** "F5's second-line pin is an app-kit class, `TopBarFitKitTest`, …"
>   - **PL3** "The death pair carries no new WO-007 line. …", and **PL3a**, its amendment "Amends … TASK-059 runs every `KitApp` class alone …". PL3a wins where they differ: every class stage 3's `KitApp` change reaches runs alone at TASK-059 on the floor and dev. **PL3b**, the second amendment "Amends … TASK-061 runs the four `KitApp` classes alone too, on the floor and dev." (plan review S1), extends that to TASK-061.
> - Earlier rows the briefs cite:
>   - **C1** 2026-10-20 "What counts on each screen."
>   - **C2** 2026-10-20 "One format for every time the game shows …"
>   - **C3** 2026-10-20 "The play-through's closed world of texts admits the best time the solved row shows …" (amended by E10)
>   - **C4** 2026-10-20 "REQ-031.A2 is proven in `play/` only; …" (amended by E4)
>   - **C5** 2026-10-17 "The promise lines' oracles" and "Two readings of the device kit's closed-world checks"
>   - **C6** 2026-10-15 "The top bar's drawing numbers" and "The top bar, Restart and the solved row, as drawn"
>
> **Checkpoint candidates.** The design has four: the gear and the screen's look, with F5 (E2, E2a); which best times (E5); where the player is after a reset (E6); the sounds and the click (E7, E7a). The how-to-play text is a proof-reading item in `review-notes.md`, not a checkpoint. The orchestrator asks them on the G3 surface, and the build follows their defaults meanwhile. A different answer changes constants in `SettingsScreen.kt`, `TopBar.kt` or `tools/make_sounds.py`, and the kit pins that cite those rows.

## Dispatch order (two lanes)

| Step | Test lane (acceptance-test-author) | Implementer lane (slice-implementer) | Gate to the next step |
|---|---|---|---|
| 0 | — | — (orchestrator) `plan_check.py --wo WO-007` GREEN; G3 (the plan, the channels, the checkpoint candidates asked); the tree at dispatch recorded in the run log | G3 passed |
| 1 | **TASK-052** drafts and **lands** | — | TASK-052 met at landing. The windows open on `kernel/src/test`, `play/src/test` and `app/src/test` |
| 2 | — | **TASK-053** | The kernel kit is green. The orchestrator fills the Contract-deltas row and re-freezes the baseline |
| 3 | TASK-055 may draft once TASK-054 lands | **TASK-054** | The JVM line on every module; three classes on the floor and dev; the death pair on both |
| 4 | TASK-055 **lands in one step with** TASK-056 | **TASK-056** | The JVM line; four classes on the floor and dev |
| 5 | TASK-058 (drafted from TASK-056's landing) **lands in one step with** TASK-057 | **TASK-057** | TASK-057's Done-check (the tool's bytes, the JVM line, V-01); TASK-058 met at landing |
| 6 | TASK-060 may draft once TASK-059 lands | **TASK-059**, in two hops (its brief's dispatch line) | Seven app classes on three images, the four `KitApp` classes on the floor and dev; the death pair on the floor and dev |
| 7 | TASK-060 **lands** | — | TASK-060 met at landing. The windows open on `app/src/test` and `app/src/androidTest` |
| 8 | — | **TASK-061** | Five app classes on three images, the four `KitApp` classes on the floor and dev; the death pair on the floor and dev |
| 9 | `plan_check.py --wo WO-007` again (the kit has compiled; WO DoD) → **Code Reviewer** (fresh) on the tree after TASK-061. Each fix lands, and its class re-runs alone | | The review is closed |
| 10 | — | **TASK-062**: every channel once, on the final tree | TASK-062's Done-check green |
| 11 | Test & Verify (from the run log; it re-runs only what changed) → trace-check → close | | |

- **Why this order.**
  - **The kit before the seams it pins** (PL1). TASK-053 and TASK-054 start with the visible oracles of the design's §3 "Highest-risk part" in the tree.
  - **TASK-054 right after TASK-053:** it carries the highest-risk part (`resetAll`'s order, silent until a restart) and needs only S-44Δ.
  - **TASK-055 with TASK-056** (E12): the moved › breaks pins in `play/src/test` and `play/src/androidTest`; landing their corrections in the same step keeps both source sets green.
  - **TASK-058 with TASK-057** (PL1): the sounds' file pin can be neither before nor after the files it reads.
  - **S-50 and S-51 after the screen:** REQ-033.A1's oracle presses the real sound switch (E8a), which is S-49's.
  - **Code review before TASK-062,** so the whole sets run once, on the reviewed tree.
- **How the one-step landings run.**
  - **Step 4.**
    1. TASK-056's implementer writes the main code and its three scaffold corrections, and compiles the main sources (`:app:assembleDebug`). It then tells the orchestrator; it runs no test.
    2. The orchestrator resumes TASK-055's author: "TASK-056's main code is in the tree: land TASK-055". The author applies its edits and new files, and compiles.
    3. The orchestrator resumes TASK-056's implementer to run its Done-check.
  - **Step 5.**
    1. TASK-057's implementer writes `tools/make_sounds.py`, runs it, and writes `ASSET-LICENCES.md` § Sounds. It then tells the orchestrator; it runs no test.
    2. The orchestrator resumes TASK-058's author: "TASK-057's files are in the tree: land TASK-058". The author applies its stage and compiles.
    3. The orchestrator resumes TASK-057's implementer to run its Done-check. TASK-058's Done-check (3) is read from that run.
- **Red windows** (`tasks.md` § WO-007 intro). Nobody runs a set for a verdict inside its window. Inside one, run 2 of the JVM line leaves the red module out:
  - between TASK-052 and TASK-053: without `:kernel:test`, `:play:testDebugUnitTest` and `:app:testDebugUnitTest`;
  - between TASK-053 and TASK-054: without `:play:testDebugUnitTest` and `:app:testDebugUnitTest`;
  - between TASK-058 and TASK-059: the JVM line runs whole, but the new app classes and the extended legs fail at run time (nothing composes the screen yet); no app device class runs for a verdict;
  - between TASK-060 and TASK-061: without `:app:testDebugUnitTest`, and no app device class runs (`app/src/androidTest` does not compile).
- **Compile errors in the kit.**
  - An unresolved reference to a seam whose task has not landed is expected; each kit row lists which.
  - Any other compile error in a kit file is the kit author's defect, and counts as a corrected check.
  - **Who edits tests** (seam note 6): kit files are the author's; the three scaffolds named in TASK-056's row are TASK-056's. An implementer whose change breaks any other test hands back `recirculate → acceptance-test-author`, naming the file and the line.
- **Decisions the implementers record.** A number or reading that no REQ, seam row or `decisions.md` row fixes goes back as a proposed row, with its source and the alternative. The orchestrator appends it, so each is recorded once.
- **Device-time rules** (`platform-notes/android.md`; KICKOFF "State after WO-008"):
  - the floor image first, one emulator at a time, booted detached;
  - one background script per image, judged by the XML;
  - the 40-minute no-test-finished watchdog;
  - while fixing, the failing class alone; the whole sets once, on the final tree (TASK-062);
  - after a stopped or killed run, reboot the image before its next step;
  - a station never owns an emulator as its own background task.

## Verification channels (G3)

**Conventions for every command below.**
- "The line" is `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat <tasks>"`.
- `ADB` is `/c/Users/Jami/AppData/Local/Android/Sdk/platform-tools/adb`; `EMU` is `/c/Users/Jami/AppData/Local/Android/Sdk/emulator/emulator`.
- Python runs with `PYTHONUTF8=1` (the cp1252 console).
- **Every adb, emulator and Gradle device call is wrapped in `timeout`.** The bounds:
  - a boot 300 s;
  - a single class 3600 s (`TouchTargetKitTest` takes about 12.5 min on each image before this WO's legs; `OverviewKitTest` about 25 min);
  - the whole play set 7200 s (about 47 min on the floor and 55 min on dev, run log WO-008 t051);
  - the whole app set's rest 3600 s;
  - the death pair 900 s.

| Channel | How (Git Bash) | Result | Needed by |
|---|---|---|---|
| Python tool channel | `PYTHONUTF8=1 python tools/validate_puzzles.py Tangrams`; `PYTHONUTF8=1 python C:/GitHub/AI/SwReqCollector/tools/validate.py Requirements` | "106/106 puzzle files valid"; "0 errors" (warnings allowed) | TASK-062; Test & Verify |
| JVM (JUnit 4) | **Two runs of the line, in this order** (run log g3-jvm): **(1)** `:app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun`, **then (2)** `:content:testDebugUnitTest --rerun :play:testDebugUnitTest --rerun :kernel:test --rerun :contracts:test --rerun :store:testDebugUnitTest --rerun :app:testDebugUnitTest --rerun`. Run (2) without run (1) first is not a verdict (the WO-005 lesson). In a red window, run (2) leaves the red module out (§ Dispatch order) | `*/build/test-results/**/TEST-*.xml`, each stamped after run (2) started. Baseline g3-jvm: app 4, content 80, contracts 2, kernel 65, play 132, store 12 = 295/0 | every task |
| Kit compile | The line with `:play:assembleDebugAndroidTest :app:assembleDebugAndroidTest` | the exit code and the compiler messages | every task |
| Sound tool | `PYTHONUTF8=1 python tools/make_sounds.py` twice, then `cmp` of each of the five `app/src/main/res/raw/sfx_*.wav` against a copy taken after the first run | identical bytes | TASK-057 |
| **The device script — one per image** | 1. `$ADB emu kill`; `$ADB devices` until it is empty.<br>2. Boot detached: `nohup $EMU -avd <image> -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot > <your scratch>/emu-<image>.log 2>&1 &`.<br>3. Wait for `sys.boot_completed` = 1. `$ADB devices` must show `emulator-5554` alone.<br>4. Record `getprop ro.build.version.sdk`, `wm size`, `wm density` and `settings get secure navigation_mode`.<br>5. `$ADB uninstall app.tangramplaytime` and `app.tangramplaytime.test` ("not installed" is fine).<br>6. `bash tools/device_reset.sh`, then `bash tools/device_reset.sh --check` (clean), then `$ADB shell dumpsys window \| grep mCurrentFocus`.<br>7. The classes, **one class per invocation**: the line with `:play:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.play.<Class>` (or `:app:…` with `app.tangramplaytime.app.<Class>`). A comma list through `cmd.exe //c` runs only its first class. **Whole sets** (TASK-062 only):<br>- the play set: `:play:connectedDebugAndroidTest`;<br>- the app set: two invocations, `…class=app.tangramplaytime.app.DevPromptKeyboardKitTest` alone, then the rest with `-Pandroid.testInstrumentationRunnerArguments.notClass=app.tangramplaytime.app.DevPromptKeyboardKitTest`.<br>8. After each invocation: copy its XML to your scratch (the folder is overwritten), then reset + `--check` + focus.<br>9. The death pair, where the table below says so.<br>10. A final reset + `--check`; `$ADB emu kill`; `$ADB devices` empty.<br>**The watchdog:** if no test has finished for 40 min (no new `TestRunner: finished` line in `$ADB logcat -s TestRunner`), stop the step: end the Gradle invocation, `gradlew --stop`, `$ADB emu kill`. Record it; the image is rebooted before its next step. | `play/build/outputs/androidTest-results/connected/debug/TEST-*.xml`, `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`. Judge a run by its XML, never by Gradle's exit code: the XML exists, is stamped after the command started and carries the expected counts. Under `-q`, a failed install exits 0 and writes no XML | every device step |
| Floor image **`Phone_API_31`** (Android 12; 1080 × 2400 px at 420 dpi = 411 × 914 dp) — **always first** | The script with `-avd Phone_API_31`. Proven: run log g3-floor (boot 23 s; `--check` clean, with app-locales skipped below API 33) | the same folders (the file name carries the device) | TASK-054, TASK-056, TASK-059, TASK-061, TASK-062 |
| Dev image `Medium_Phone_API_37.0` (411 × 914 dp; gesture navigation, `navigation_mode` 2) | The script with `-avd Medium_Phone_API_37.0` | the same | TASK-054, TASK-056, TASK-059, TASK-061, TASK-062 |
| Tablet image `Medium_Tablet_API_37.0` (2560 × 1600 px at 320 dpi = 1280 × 800 dp, landscape) | The script with `-avd Medium_Tablet_API_37.0`. App classes only: the play set hosts `PlayScreen` at the kit's phone size (WO-006 practice) | the app folder | TASK-059, TASK-061, TASK-062 |
| The process-death pair (ADR-003's row-11 harness; PL3) | Inside the floor and dev scripts:<br>1. Build with the line `:app:assembleDebug :app:assembleDebugAndroidTest :app:packageDebug --rerun :app:packageDebugAndroidTest --rerun` (the harness refuses stale APKs).<br>2. `bash tools/process_death_check.sh`, then `bash tools/process_death_check.sh --positive-control`.<br>3. Reset + `--check` | the script's verdict line: `PASS …` / `CONTROL PASS …` and exit 0 | TASK-054, TASK-059, TASK-061, TASK-062 (floor, dev) |
| Release verifier V-01 (G-01, G-08) | After run (1) of the JVM line: `python .swdev/verifiers/v01_release_apk.py`, `python .swdev/verifiers/v01_release_apk.py --positive-control`, then `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-007` | the PASS lines, GREEN, and the budget line (expected: 0 verifiers, 0 lines for WO-007) | TASK-054, TASK-056, TASK-057, TASK-059, TASK-061, TASK-062 |
| Device hygiene | Reset, `--check` and `mCurrentFocus` before and after every device step; `--check` names a focused crash or not-responding window (S-37Δ). A non-clean check after a reset voids the step: reset, re-run, record the leftover. A check that names a crash window means a reboot of the image | `--check` clean or not; the focus line | every device step |

**What runs where.** "Alone" means one class per invocation, each with a reset around it.

| Task | Floor `Phone_API_31` | Dev `Medium_Phone_API_37.0` | Tablet `Medium_Tablet_API_37.0` |
|---|---|---|---|
| TASK-054 | `TimerKitTest`, `ResumeScreenKitTest` (play); `ActiveTimeAppKitTest` (app), alone; the death pair | the same | — |
| TASK-056 | `TopBarKitTest`, `DevAidScreenKitTest` (play); `TopBarFitKitTest`, `PhoneLayoutKitTest` (app), alone | the same | — |
| TASK-059 | `SettingsKitTest`, `ResetKitTest`, `PlayThroughKitTest`, `TouchTargetKitTest`, `LanguageKitTest`, `AppWiringKitTest`, `ActiveTimeAppKitTest`, alone; then `PhoneLayoutKitTest`, `TabletLayoutKitTest`, `DevAidAppKitTest`, `DevPromptKeyboardKitTest`, alone (stage 3's `KitApp` doors reach them); the death pair | the same | the seven app classes, alone |
| TASK-061 | `SoundKitTest`, `SettingsKitTest`, `PlayThroughKitTest`, `AppLaunchKitTest`, `AppWiringKitTest`, alone; then `PhoneLayoutKitTest`, `TabletLayoutKitTest`, `DevAidAppKitTest`, `DevPromptKeyboardKitTest`, alone (PL3b); the death pair | the same | the five app classes, alone |
| TASK-062 | the whole play set; the whole app set (two invocations); the death pair | the same | the whole app set |

**Device time** (informative; per-class times from `.swdev/run-log/WO-008.md`): about 20 min at TASK-054, 30 min at TASK-056, 2 h 20 min at TASK-059, 1 h 25 min at TASK-061 and 3.5 h at TASK-062; about 8 h in all. Each hand-back reports its per-class times so the next step's bounds can be checked.

**Expected skips.**
- The only legitimate skips are the two guarded `ProcessDeathKitTest` phases in an ordinary connected run.
- On the floor (API 31 < 33), `LanguageKitTest`'s three `@SdkSuppress(minSdkVersion = 33)` legs are filtered: they are absent from the XML, which is not a skip.
- Any other skip in an XML is a failure.

**Other channels.**
- **Not a DoD channel:** human eyes. The owner plays at close, from `review-notes.md`: the gear and the screen on a phone and a tablet; the five sounds and the tick on a real device (an emulator gives no haptic); sound off silencing the taps; Reset. The how-to-play text and the `fi` strings are proof-reading items.
- **Browser:** none.
- **Toolchain proof:** `.swdev/run-log/WO-007.md` rows g3-py, g3-jvm, g3-v01, g3-floor, g3-dev and g3-tablet, on the tree at G3. The process-death harness is unchanged since WO-004 and ran on this tree at WO-008 t051.
- **Time (G-14):** product code reads time only through `Clock`, and `AppClock` is the one implementation. Kits move a kit clock and never sleep. Wait for an observed state, always with a bound.

---

## Brief — TASK-052 · WO-007 · acceptance-test-author

- **Mission:** Stage 1 of WO-007's one acceptance kit, before any WO-007 product code exists. Three new JVM files, pins only, no token:
  1. `kernel/src/test/kotlin/app/tangramplaytime/kernel/ActiveTimeResetKitTest.kt`: S-44Δ / S-45Δ's pins.
  2. `play/src/test/kotlin/app/tangramplaytime/play/SettingsControllerKitTest.kt`: S-1Δ4, S-2Δ4 and S-19Δ2's pins.
  3. `app/src/test/kotlin/app/tangramplaytime/app/ResetRestartKitTest.kt`: the restart leg (E6a).

  The class is JVM oracles of the WO's highest-risk part (the reset order), so it goes to the strongest model. You land first; the orchestrator says when.
- **Read exactly:**
  1. `tasks.md` § WO-007:
     - the intro, all of it: where the cut differs, the lanes, the red windows, seam notes 1–7, the in-tree tests, the lines covered twice, the must-nevers, the Contract deltas;
     - § Seams (the "Files" and "Kit files" rows) and the coverage table;
     - the rows TASK-052 (yours), and TASK-053 and TASK-054, whose Done-checks are your green verdicts.
  2. `designs/WO-007-design.md`:
     - §1 (the delta's KDoc);
     - §2: the rows S-44Δ, S-45Δ, S-19Δ2, S-1Δ4 and S-2Δ4, and the paragraphs "Tokens (Q10)" and "Kit doors (Q11)";
     - §3, all of it;
     - not §4: the plan's cut replaces it.
  3. `reviews/WO-007-design-review.md`: F2, F4, and § Re-derived rows 6, 7 and 8 (why the oracles are shaped as they are).
  4. `workorders/WO-007.md`: § Scope with its notes (carries 1 and 2); § Constraints & notes questions 2, 3, 5 and 11; the standing constraints.
  5. Requirements, read-only, in the REQ folder: `REQ-034.md`, `REQ-032.md`, `REQ-031.md`, `REQ-033.md`, `REQ-029.md`, `REQ-030.md`; `Requirements/req_types.md`: TYPE-005, TYPE-006.
  6. Frozen contracts:
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/IActiveTime.kt`. Its new member does not exist until TASK-053: it is S-44Δ's row, with the KDoc in the design's §1.
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/IProgressStore.kt` (`saveSettings`, `resetAll`);
     - `contracts/src/main/kotlin/app/tangramplaytime/contracts/Types.kt`: `Settings`, `PlayTimes`, `PuzzleProgress`, `PuzzleState`, `Clock`.
  7. `architecture.md` v1.5: §2 G-13, G-14; §3 O-04; §4 the `IActiveTime` and `IProgressStore` rows; §6 ADR-003, ADR-006.
  8. `decisions.md`: E3, E4 with E4a, E5, E6 with E6a; C1, C4; PL1.
  9. **In-tree kit.** Reuse it. You may add a `kit`-prefixed helper; you change an existing helper only where this brief says so.
     - The kernel kit folder: `KitFixtures.kt`; `ActiveTimeKitTest.kt`, read-only (its kit clock).
     - The JVM kit folder: `KitPlay.kt`; `KitStore.kt` (`KitProgressStore` records every write it completes, `kitWrites()`; `KitSession.startTimed` / `restartTimed`); add a door there only if your pins need one. `PlayTimeKitTest.kt`, read-only (the timed-session pattern).
     - `store/src/test/kotlin/app/tangramplaytime/store/FileProgressStoreKitTest.kt`, read-only: how the file store is built on a temporary file.
  10. **Declarations only, never bodies:**
      - `play/src/main/kotlin/app/tangramplaytime/play/PlayController.kt`: the constructors and the public members;
      - `play/src/main/kotlin/app/tangramplaytime/play/Persistence.kt`: the constructor, `load`, `save`, `flush`, `settings`, `playTimes`;
      - `kernel/src/main/kotlin/app/tangramplaytime/kernel/ActiveTime.kt`: the constructor;
      - `store/src/main/kotlin/app/tangramplaytime/store/FileProgressStore.kt`: the constructor.
  11. Rules:
      - `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2;
      - `C:/GitHub/AI/SWDev/framework/methodology/task-decomposition.md`: "Assert only what is mandated", "Seams name their task; adapters fail loudly", "Declared inputs land together", "Time is injected, never slept";
      - `platform-notes/android.md` § Compose test kit (the time lines).
- **Do not read:**
  - the main-code bodies of every module and of `play/src/debug`;
  - the implementer briefs (every brief in this file but TASK-052, TASK-055, TASK-058 and TASK-060);
  - DI-1 … DI-10 (the prototype, never);
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-052 row's Inputs cell.
- **Oracle rule:**
  - **Sources.** Every assertion cites, in a comment, the sentence it checks: a REQ sentence, a TYPE rule, a seam row (`S-n`), an architecture line, or a `decisions.md` row by date and title. No source, no assertion.
  - **No token in this stage** (design §2 "Tokens": REQ-032, REQ-034 and REQ-029.A2 are screen lines, `app/`'s device kit's). Cite rules in prose, without a dotted ID: trace-check counts every dotted ID it finds.
  - **Time.** The kit owns its `Clock`: it moves the clock and never sleeps. Every action and every clock move runs on the test's one thread (`IActiveTime`: one caller thread), with `countTime()` after each jump.
  - **Every "nothing" line has its positive control in the same test:**
    - the pause (S-2Δ4; REQ-032 rule 2; C1): while the screen is open the In-progress puzzle's time stays put **and** the play time grows by the same clock move;
    - "no pre-reset write lands after the reset" (design §3 "Reset order"): the open puzzle is In progress with placed pieces and time before the reset, so a stale write would differ from FRESH in the recorded writes;
    - the switches (S-1Δ4, N1): a change shows in the controller's snapshot at once, on the same controller, and reaches the store after `flush`.
  - **Fixtures discriminate:**
    - `bestTimes` (S-1Δ4; E5): at least two puzzles solved in an order other than ‹ › order; one solved only through the DEV aid (no row: REQ-030 rule 4); the open puzzle's best read from `bestSeconds`;
    - the restart leg: exactly E6a's trap state, so each kept or erased item can be told apart.
  - **Mechanics.** No `assume…`. In the same module, call `internal` members directly, never by reflection. `app/src/test` is another module than `:play` and `:store`: it uses their public members only. Helper names carry a `kit` prefix.
- **What each file needs** (pointers; the REQ and the seam row are the oracle):
  - **`ActiveTimeResetKitTest.kt`** (S-44Δ's KDoc; S-45Δ): `PlayTimes.NONE` after the erasure until a second counts; an uncounted part of a second dropped; visibility kept; the last touch kept (the 60 s idle limit still runs from the touch before the erasure); never negative.
  - **`SettingsControllerKitTest.kt`** (S-1Δ4, S-2Δ4, S-19Δ2; E3, E4, E5, E6):
    - `settingsOpen` false at start; the fold before open and before close; the pause with its control;
    - open and close change no placement, state, tray turn, open puzzle or puzzle time;
    - `setTimerShown` / `setSoundOn`: the snapshot at once, `timerShown` with it, the store after `flush`; the defaults on an empty store (REQ-031 rule; REQ-033 ASSUMPTION);
    - `bestTimes`: rows, order, the aid's puzzle absent, refreshed by `openSettings` and `resetAll`;
    - `resetAll`: the fold first; every puzzle FRESH; the play times `NONE`; the settings and the last-opened id kept; the recorded writes show no pre-reset progress after the store's reset; no `PlayEvent`; no leaving save (`open` is never called).
  - **`ResetRestartKitTest.kt`** (E6a; design §2 "Kit doors", the restart leg): the real `FileProgressStore` on a temporary file, the real `Persistence`, `ActiveTime` over a kit clock, and the controller. Build the trap state; `resetAll`; `flush`; then a new store, `Persistence.load` and controller over the same file. Every puzzle FRESH; `PlayTimes.NONE` plus any second counted after the reset; the settings (timer on) and the last-opened id kept.
- **Run:**
  1. Write the files. Landing is your Done-check, met at landing.
  2. Run (1) of the JVM line, then run (2) without `:kernel:test`, `:play:testDebugUnitTest` and `:app:testDebugUnitTest`. Content, store and contracts are green.
  3. Compile the three red modules and list every compile error.
     - Expected, and not your defect: unresolved references to S-44Δ's member (kernel, app), and to the names of S-1Δ4, S-2Δ4 and S-19Δ2 (play, app).
     - Any other compile error is yours.
  4. Kit compile: `:play:assembleDebugAndroidTest :app:assembleDebugAndroidTest`.
  5. Grep the kit folders for `REQ-0[0-9]*\.A[0-9]`: no dotted ID in your three files, the earlier tokens unchanged.
- **Done means:** the TASK-052 row's Done-check, plus hygiene: JUnit 4 only; no new dependency; never edit main code, a build file or `tools/`.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the test names, each pin with its source;
  - the compile status: the commands, and the expected unresolved names;
  - the JVM counts per module.

  Stay available: TASK-055, TASK-058 and TASK-060 are your later stages, in this context.

---

## Brief — TASK-055 · WO-007 · acceptance-test-author (stage 2; TASK-052's author, context resumed)

- **Mission:** Stage 2 of the kit. It lands in one step with TASK-056 (§ Dispatch order, "How the one-step landings run"). Three parts:
  1. **The corrections** that S-53 forces (E12), each a corrected check: `SafeAreaKitTest.kt` :176, :202, :285–295; `TopBarKitTest.kt` :192–234.
  2. **The gear's pins** (S-53): new `play/src/test/kotlin/app/tangramplaytime/play/GearKitTest.kt`; the gear's device legs in `TopBarKitTest.kt`; one tag in `DevAidScreenKitTest.kt` :1059 / :1074.
  3. **F5's pin** (PL2): new `app/src/androidTest/kotlin/app/tangramplaytime/app/TopBarFitKitTest.kt`.

  No token. The class is a kit stage with Compose device legs, so it goes to the strongest model. Draft from TASK-054's landing.
- **Read exactly:**
  1. `tasks.md` § WO-007: the intro (the one-step landings; seam notes 2 and 6; "In-tree tests this WO changes", the TASK-055 and TASK-056 items); the rows TASK-055 and TASK-056.
  2. `designs/WO-007-design.md`: §2 the rows S-53 and S-22Δ3; §3 "The moved ›", "The second line (F5)", and library facts 1 and 2.
  3. `reviews/WO-007-design-review.md`: F3, F5, and § Re-derived rows 9 and 14.
  4. Requirements, read-only: `REQ-032.md` (statement), `REQ-037.md`, `REQ-024.md` (its rule, consumed), `REQ-006.md` (A1: the 360 × 780 dp phone), `REQ-047.md`.
  5. `architecture.md`: G-10, G-17, O-05, O-06.
  6. `decisions.md`: E2 with E2a, E12, PL2; C6.
  7. **In-tree kit:**
     - the JVM kit folder: `SafeAreaKitTest.kt` (yours to correct), `KitPlay.kt`;
     - the play device kit folder: `TopBarKitTest.kt` (yours to correct and extend), `DevAidScreenKitTest.kt` (one list each at :1059 and :1074), `KitScreen.kt` (helpers; `kitTexts`);
     - the app device kit folder: `KitApp.kt` (`contextFor`, `usePhone`, the display helpers), read-only in this stage;
     - read-only, the scaffolds TASK-056 corrects: `scaffolding/ControlsScaffoldTest.kt`, `scaffolding/SafeAreaScaffoldTest.kt`, `scaffolding/ScreenSeamScaffoldTest.kt`.
  8. **Declarations only:** `ScreenGeometry`'s fields and `of` (`play/src/main/kotlin/app/tangramplaytime/play/PlayScreen.kt`); `HitTest.at`, `Control`, `ShownControls`, `Hit` (`HitTest.kt`); `PlayTags.kt`; `PlayTexts.kt`; the `PlayScreen(…)` signature; `PlayController`'s public members.
  9. `platform-notes/android.md` § Compose test kit, all of it.
- **Do not read:** as TASK-052's brief.
- **Inputs:** the TASK-055 row's Inputs cell.
- **What each part needs** (pointers):
  - **The corrections.** › now ends at the gear's left, and the gear's 52 dp column ends at the safe right edge (S-53, S-32Δ). The NONE layout's rightmost column, the one :202's fixture tests against a right inset, is now the gear. The counter still spans ‹ to › (E2).
  - **`GearKitTest.kt`** (JVM): the slot over the cases `SafeAreaKitTest` uses (insets and none); `HitTest.at` gives the gear inside its slot, and nothing while an overlay is open (hit order 0) or a drag runs (hit order 1).
  - **`TopBarKitTest.kt`**, the gear's device legs: a tap 2 px inside each of its edges sets `settingsOpen` at the lift; a long press does not act early; a second finger on it during a drag does nothing; nothing while the grid is open. Close the settings after each leg (`closeSettings`).
  - **`DevAidScreenKitTest.kt`:** the gear joins the top-bar tags held inert under the DEV sheet.
  - **`TopBarFitKitTest.kt`** (E2a; PL2):
    - at 360 × 780 dp, on a three-digit In-progress puzzle (the bundle holds 106);
    - in `en` and in `fi`, with every text taken from `:app`'s resources (`KitApp.contextFor`), never a copied Finnish word (G-10);
    - the number's and the state word's text layouts show no visual overflow, and their nodes lie inside `counter`.
    - It must run on the floor too, where no device-locale route exists (`platform-notes/android.md`), so its language cannot come from the device locale. Whether it composes `PlayScreen` in a kit-sized host or constructs the phone with a display override is your choice; read every size from the nodes.
  - Every assertion cites its source (S-53, S-22Δ3, E2, E2a, C6). A pin with no token cites rules in prose, without a dotted ID.
- **Run:**
  1. When the orchestrator says TASK-056's main code is in the tree, apply your edits and files.
  2. Compile `:play:testDebugUnitTest`, `:play:assembleDebugAndroidTest` and `:app:assembleDebugAndroidTest`. If you compile before TASK-056's diff is in, the only errors are unresolved references to S-53's names.
  3. Grep the kit folders for `REQ-0[0-9]*\.A[0-9]`.
  4. Do not run a device class: TASK-056's Done-check does.
- **Done means:** the TASK-055 row's Done-check, plus hygiene (JUnit 4; no new dependency; never edit main code, a build file, `tools/` or a scaffolding test).
- **Return:** the handoff block, with:
  - each correction: the file, the line, before and after, why, and that it is a corrected check;
  - the new tests, each with its source;
  - the compile status.

---

## Brief — TASK-058 · WO-007 · acceptance-test-author (stage 3; the same author, context resumed)

- **Mission:** Stage 3 of the kit: the settings screen and the reset in the real app, the "every screen" lines over the new screen, the kit's doors, and one JVM file pin for the sounds. It lands in one step with TASK-057 and before TASK-059 (§ Dispatch order, step 5). Parts:
  1. New `app/src/androidTest/kotlin/app/tangramplaytime/app/SettingsKitTest.kt`: REQ-032.A1, REQ-032.A2, REQ-009.A1, REQ-049.A1, REQ-029.A2, and REQ-031.A2 as an extra token; the untokened pins of the row (the licence line among them).
  2. New `app/src/androidTest/kotlin/app/tangramplaytime/app/ResetKitTest.kt`: REQ-034.A1, REQ-034.A2.
  3. New `app/src/test/kotlin/app/tangramplaytime/app/SoundAssetsKitTest.kt`: the five WAVs and their listing, no token.
  4. Edits: `KitApp.kt` (the F2 doors, the worlds, the tag literals), `PlayThroughKitTest.kt`, `TouchTargetKitTest.kt`, `LanguageKitTest.kt`.

  The class is a Compose kit in the real app with device-only legs, so it goes to the strongest model. Draft from TASK-056's landing.
- **Read exactly:**
  1. `tasks.md` § WO-007: the intro (seam notes 3–7; the red windows; "In-tree tests this WO changes", the TASK-058 item; the lines covered twice; the must-nevers; "Unverified by design"); the coverage table; the rows TASK-058, TASK-057 and TASK-059.
  2. `designs/WO-007-design.md`:
     - §1 "How it works";
     - §2: the rows S-49 (placement, tags, strings), S-1Δ4, S-2Δ4, S-15Δ4, S-52, and the paragraphs "Tokens (Q10)" and "Kit doors (Q11)";
     - §3: the must-nevers, "The layer must take every touch", "N1", library fact 1.
  3. `reviews/WO-007-design-review.md`: F2, F4, § Re-derived rows 8 and 13.
  4. `workorders/WO-007.md`: § Scope notes (the licence line; the re-covered lines); § Constraints & notes questions 1, 4, 5, 8, 9 and 11.
  5. Requirements, read-only: `REQ-032.md`, `REQ-009.md`, `REQ-049.md`, `REQ-029.md`, `REQ-031.md`, `REQ-034.md`, `REQ-030.md` (rule 3), `REQ-001.md`, `REQ-008.md`, `REQ-010.md`, `REQ-037.md`, `REQ-047.md`, `REQ-033.md` (rules 1–2); `Requirements/req_types.md` TYPE-005, TYPE-006.
  6. Contracts: `Types.kt` (`Settings`, `PlayTimes`, `PuzzleState`); `IProgressStore.kt`.
  7. `architecture.md`: G-01, G-09, G-10, G-16; O-06; §5 the application id.
  8. `decisions.md`: E1, E2 with E2a, E3, E4 with E4a, E5, E6 with E6a, E9, E10; C1, C3, C5; PL1, PL3 with PL3a.
  9. **In-tree kit:**
     - the app device kit folder: `KitApp.kt` (yours to edit: `start` :1311, `restoreController` :1328, `onRestore` / `restoreRule` :465 / :484, `kitInertBoardPoint` :1559, `kitTouchThroughWindow` :1606, `closedWorld` :1979, `languageWorld` :1994, `kitBestTimeText`, `KitAppTag`); `PlayThroughKitTest.kt`, `TouchTargetKitTest.kt`, `LanguageKitTest.kt` (yours to extend);
     - read-only: `ActiveTimeAppKitTest.kt` (the window touch and the kit clock), `AppWiringKitTest.kt` (Back), `PhoneLayoutKitTest.kt` (`usePhone`), `ProcessDeathKitTest.kt` with `tools/process_death_check.sh` (its `phase1` uses `KitApp`'s window touch: your change must keep it green);
     - read-only: `content/src/test/kotlin/app/tangramplaytime/content/AssetLicencesKitTest.kt` (how `ASSET-LICENCES.md` is read; its `## Pictures, fonts, sounds` check).
  10. **Declarations only:** `PlayController`'s public members (S-1Δ4, S-2Δ4 are in the tree since TASK-054); `AppGraph.controller`; `AppClock.override`. `SettingsTags` and S-49's strings do not exist until TASK-059: take the tags from S-49's tag row and the texts from the REQs (seam note 4).
  11. `platform-notes/android.md`, all of it (the window touches, the bounded Compose calls, `KitScreen.scrollTo` and the paused clock, the display overrides on API 31).
- **Do not read:** as TASK-052's brief.
- **Inputs:** the TASK-058 row's Inputs cell.
- **Oracle rule** (as TASK-052's, plus):
  - **Tokens** exactly as design §2 "Tokens" and the row say. The extended classes keep exactly their tokens.
  - **The REQ texts are the oracle** for the free note (REQ-009 statement) and the privacy text (REQ-049 rule 1), verbatim, in English (G-10). The licence line's text is S-49's strings row (E1; plain text, not a link). The Finnish walk compares each text with its `fi` resource and never asserts a Finnish word.
  - **Each oracle can fail:**
    - REQ-029.A2 as E4a says (M s on one date and N s on the next; today = N and all-time ≥ M + N, both above 0 and different; with the screen open, a window touch on `settings-title` plus K s moves both rows by K s while an In-progress puzzle's time stays put);
    - REQ-034.A1 and A2 over E6a's trap state, built by touch and through the real switch;
    - REQ-032.A2 must be able to see a stray control (cite S-49's tag row);
    - REQ-032.A1 over a board with placed pieces, a turned tray piece and counted time.
  - **"Shows"** (REQ-034.A1, REQ-009.A1, REQ-049.A1, REQ-029.A2): read on the screen, from the nodes, each placement asserted inside the region meant (below the note, inside the sheet).
- **Constraints:**
  - **The doors** (seam note 5; E4a): window touches on the open screen land on `settings-title`, read on the screen in use; the helper's before/after check adds `settingsOpen`; `start` fails loudly while `settingsOpen`, reads the settings and registers `onRestore("the settings")`; `restoreController` calls `closeSettings()`.
  - **A resetting class** needs every puzzle New at its start, fails loudly otherwise, and leaves every puzzle New. A probe that presses Erase resets: restore as the class requires.
  - **The worlds** admit the screen's times as E10 says, read at each check.
  - **Sheet scrolls** run with auto-advance on (`KitScreen.scrollTo`'s lesson).
  - **Compile at landing** (seam note 4): no reference to `SettingsTags`, to an `R.string` S-49 adds, or to S-50's names.
  - **`SoundAssetsKitTest.kt`:** REQ-033 rules 1 and 2, S-52, G-16. It reads `ASSET-LICENCES.md` from the project root and the five files under `app/src/main/res/raw/`. The "soft low" pin cites REQ-033 rule 2 and E7's recipe, and asserts only what they say.
- **Run:**
  1. Draft from TASK-056's landing. When the orchestrator says TASK-057's files are in the tree, apply your files and edits.
  2. Compile `:app:assembleDebugAndroidTest` (no error) and `:app:compileDebugUnitTestKotlin`.
  3. Grep the kit folders for `REQ-0[0-9]*\.A[0-9]`.
  4. Do not run a device class: they fail at run time until TASK-059, whose Done-check runs them. TASK-057's Done-check runs the JVM line with your file pin.
- **Done means:** the TASK-058 row's Done-check, plus hygiene.
- **Return:** the handoff block, with:
  - the test names per acceptance ID, and each pin with its source;
  - each edit to an existing class: what changed and why, and any corrected check;
  - the compile status and the grep's result.

---

## Brief — TASK-060 · WO-007 · acceptance-test-author (stage 4; the same author, context resumed)

- **Mission:** Stage 4 of the kit. It lands before TASK-061 (kit first). Three parts:
  1. New `app/src/androidTest/kotlin/app/tangramplaytime/app/SoundKitTest.kt`: REQ-033.A1, as E8a says.
  2. New `app/src/test/kotlin/app/tangramplaytime/app/FeedbackKitTest.kt`: S-50's pins, no token (moved here from the design's stage 1, PL1).
  3. `KitApp.kt`: the feedback door.

  The class is a device-only kit, so it goes to the strongest model. Draft from TASK-059's landing.
- **Read exactly:**
  1. `tasks.md` § WO-007: the intro (the red windows; the must-nevers); the rows TASK-060 and TASK-061.
  2. `designs/WO-007-design.md`: §2 the rows S-50, S-51, S-15Δ4, and "Tokens (Q10)", "Kit doors (Q11)"; §3 the must-nevers, "Carry 4", "Outside the tap path", library facts 2, 3, 4 and 6.
  3. `reviews/WO-007-design-review.md`: F1, and § F1 re-check, all of it.
  4. `workorders/WO-007.md`: § Constraints & notes questions 6 and 7; carry 4.
  5. Requirements, read-only: `REQ-033.md`, all of it; `REQ-010.md` (rule 2).
  6. `architecture.md`: G-01, G-11, O-07.
  7. `decisions.md`: E7 with E7a, E8 with E8a, E11; PL1.
  8. `capture-outbox/chg-draft-req033-a1-device-oracle.md`: read only to know it is pending; the build follows the locked A1.
  9. **In-tree kit:** `KitApp.kt` (yours to edit); `SettingsKitTest.kt` (yours: the real switch); `PlayThroughKitTest.kt` (driving the actions by touch), read-only.
  10. **Declarations only:** `play/src/main/kotlin/app/tangramplaytime/play/PlayEvent.kt` (the five events); `PlayController`'s public members. `Feedback`, `FeedbackOut`, `Sound` and `AppGraph.feedback` do not exist until TASK-061: their shapes are S-50's and S-15Δ4's rows.
- **Do not read:** as TASK-052's brief.
- **Inputs:** the TASK-060 row's Inputs cell.
- **Constraints:**
  - **The door** (S-50; E8): the recorder sits at `AppGraph.feedback.override`; `onRestore` sets it back to null. No source scan, no caller inventory, no allow-list.
  - **The sound-off leg** (E8a): sound is turned off through the real switch; that press records one click, which the leg clears. Then Done; pick-up, turn, lock, a return and a solve, by touch; the grid and a cell; the settings again and the sound switch back on. The recorder stays empty throughout.
  - **The positive control, in the same test** (E8a): with sound on, Done and a grid cell record one click each; the five actions record each sound once, and one tick per lock.
  - **`FeedbackKitTest.kt`:** S-50's map (each event to its sound; the tick on a lock only), the gate per event and per click, `override` before the device. Every "nothing" has its positive control.
  - The switches are restored through `KitApp`'s `onRestore("the settings")` (E4a).
- **Run:**
  1. Write the files; land when the orchestrator says.
  2. Compile `:app:testDebugUnitTest` and `:app:assembleDebugAndroidTest`, and list every error. Expected, and not your defect: unresolved references to the names of S-50 and of S-15Δ4's feedback door.
  3. Grep the kit folders for `REQ-0[0-9]*\.A[0-9]`.
- **Done means:** the TASK-060 row's Done-check, plus hygiene.
- **Return:** the handoff block, with the test names, each leg and its source, and the expected unresolved names.

---

## Brief — TASK-053 · WO-007 · slice-implementer

- **Mission:** S-44Δ and S-45Δ: `IActiveTime` gains `resetPlayTimes()` and `ActiveTime` implements it. The class is a governed interface and the kernel's time arithmetic with must-nevers, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-007: the intro (seam note 1; the Contract deltas), the "Files" row, the TASK-053 row.
  2. `designs/WO-007-design.md`: §1 (the Contract delta and its KDoc); §2 the rows S-44Δ and S-45Δ.
  3. Requirements: the REQ folder's `REQ-034.md`, `REQ-029.md`; `Requirements/req_types.md` TYPE-005.
  4. Contracts: `contracts/src/main/kotlin/app/tangramplaytime/contracts/IActiveTime.kt`, `Types.kt` (`PlayTimes.NONE`).
  5. `architecture.md` v1.5: §4 the `IActiveTime` row; G-12, G-14; O-04.
  6. `decisions.md`: E6, E13.
  7. The visible tests, never edited: the kernel kit folder's `ActiveTimeResetKitTest.kt` and `ActiveTimeKitTest.kt`.
  8. Code: `kernel/src/main/kotlin/app/tangramplaytime/kernel/ActiveTime.kt`.
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10.
- **Inputs:** the TASK-053 row's Inputs cell.
- **Constraints:**
  - The KDoc is the design's §1 text; the banner's tier stays notify, and its `Serves:` gains REQ-034.
  - The new member is the interface's only change. No `:play` or `:app` change; never edit a test.
- **Run:**
  1. The JVM line: run (1), then run (2) without `:play:testDebugUnitTest` and `:app:testDebugUnitTest` (their window).
  2. `:app:assembleDebug`.
  3. Read `.swdev/log/guard-log.jsonl` for the `contract-delta` event of `IActiveTime.kt`.
- **Done means:** the TASK-053 row's Done-check.
- **Return:** the handoff block, with the member as built, the counts per module, and the guard event's line.

---

## Brief — TASK-054 · WO-007 · slice-implementer

- **Mission:** S-19Δ2, S-1Δ4 and S-2Δ4: the settings become the controller's snapshot state and are written through `Persistence`; the settings screen's open flag pauses the puzzle time; Reset all progress runs in the design's order. The reset order is the design's highest-risk part and is silent until a restart. The class is concurrency and persistence order on the main thread over the one FIFO writer, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-007: the intro (seam notes 1, 5, 6 and 7; the must-nevers), the "Files" row, the TASK-054 row.
  2. `designs/WO-007-design.md`: §2 the rows S-19Δ2, S-1Δ4 and S-2Δ4; §3 "Reset order", "N1", "Highest-risk part".
  3. `reviews/WO-007-design-review.md`: § Re-derived rows 6, 7 and 8.
  4. Requirements: the REQ folder's `REQ-034.md`, `REQ-032.md`, `REQ-031.md`, `REQ-033.md`, `REQ-030.md`, `REQ-029.md`; `Requirements/req_types.md` TYPE-005, TYPE-006; `capture-outbox/chg-draft-type006-missing-transitions.md` (pending: a reset returns every puzzle to New).
  5. Contracts: `contracts/src/main/kotlin/app/tangramplaytime/contracts/IActiveTime.kt` (TASK-053's), `IProgressStore.kt`, `Types.kt`.
  6. `architecture.md`: G-13, G-14, G-15; O-04; ADR-003.
  7. `decisions.md`: E3, E4, E5, E6, E6a; C1, C4; PL3.
  8. The visible tests, never edited:
     - the JVM kit folder's `SettingsControllerKitTest.kt`, `PlayTimeKitTest.kt`, `ResumeKitTest.kt`, `PlayControllerKitTest.kt`, `BrowsingKitTest.kt`, `DevAidKitTest.kt` and the scaffolding tests;
     - the app JVM folder's `ResetRestartKitTest.kt`;
     - for the device step: the play device kit folder's `TimerKitTest.kt`, `ResumeScreenKitTest.kt`; the app device kit folder's `ActiveTimeAppKitTest.kt`, `ProcessDeathKitTest.kt` with `tools/process_death_check.sh`.
  9. Code: `play/src/main/kotlin/app/tangramplaytime/play/Persistence.kt`, `play/src/main/kotlin/app/tangramplaytime/play/PlayController.kt`.
  10. `platform-notes/android.md`: § Verification channels and § Device hygiene.
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10.
- **Inputs:** the TASK-054 row's Inputs cell.
- **Constraints:**
  - **Construction:** every existing construction compiles unchanged (seam note 1).
  - **`settings`** is the controller's own snapshot state, seeded from `Persistence.settings`, never a getter over it (review row 6); every reader reads it (N1).
  - **The order of `resetAll`** is S-2Δ4's five steps; it never calls `open` and emits no event (E6; design §3 "Reset order"). `Persistence.resetAll` keeps the settings and the last-opened id (S-19Δ2).
  - **One FIFO** for every write (G-13; ADR-003); every step on the main thread.
  - **Not yours:** the gear, the screen, the strings (TASK-056, TASK-059); `:app`; any test file (seam note 6).
- **Run:**
  1. The JVM line (1) + (2), every module.
  2. Kit compile.
  3. V-01 and its positive control.
  4. The floor script, then the dev script, per "What runs where", with the death pair on both.
- **Done means:** the TASK-054 row's Done-check.
- **Return:** the handoff block, with:
  - `resetAll` as built, step by step, and each write it queues;
  - the counts per module; V-01's lines;
  - the XMLs, with their stamps and tree hashes; the death pair's verdict lines;
  - any red test, with its file and line and your reading of whose it is.

---

## Brief — TASK-056 · WO-007 · slice-implementer

- **Mission:** S-53, the gear: a fourth top-bar column at the safe right edge that opens the settings; S-22Δ3, the bar's second line scaled to fit the narrower counter; S-11Δ4, `timeOf` and `titleOf` made public; and the three scaffold corrections (E12). You land in one step with TASK-055 (§ Dispatch order, "How the one-step landings run"). The class is UI geometry, hit order and rendering, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-007: the intro (seam notes 2 and 6; "In-tree tests this WO changes", the TASK-056 item), the "Files" row, the TASK-056 row.
  2. `designs/WO-007-design.md`: §2 the rows S-53, S-22Δ3 and S-11Δ4; §3 "The moved ›", "The second line (F5)", library facts 1 and 2.
  3. `reviews/WO-007-design-review.md`: F3, F5, § Re-derived rows 9 and 14.
  4. Requirements: the REQ folder's `REQ-032.md`, `REQ-037.md`, `REQ-024.md` (rule), `REQ-029.md` (rule 2), `REQ-047.md`.
  5. `architecture.md`: G-17, O-05, O-06.
  6. `decisions.md`: E2 with E2a, E12; C6.
  7. Design inputs (behaviour references only, never an oracle): DI-1 `Spec/prototype/tangram-prototype.html`, the `.topbar` gear; DI-2 `Spec/02-ui-layout.md` §1, §2 and §4; `design-inputs.md` §2 item 6 (the gear is 48 dp or more).
  8. The visible tests, never edited; they land with TASK-055: the JVM kit folder's `SafeAreaKitTest.kt` and `GearKitTest.kt`; the play device kit folder's `TopBarKitTest.kt` and `DevAidScreenKitTest.kt`; the app device kit folder's `TopBarFitKitTest.kt` and `PhoneLayoutKitTest.kt`.
  9. Code: `PlayTags.kt`, `PlayScreen.kt` (`ScreenGeometry`), `HitTest.kt`, `PointerRouter.kt`, `TopBar.kt`, `PlayTexts.kt` (all in `play/src/main/kotlin/app/tangramplaytime/play/`); the scaffolds `scaffolding/ControlsScaffoldTest.kt`, `scaffolding/SafeAreaScaffoldTest.kt`, `scaffolding/ScreenSeamScaffoldTest.kt` (yours to correct).
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10 beyond the lines named above.
- **Inputs:** the TASK-056 row's Inputs cell.
- **Constraints:**
  - **The gear** is a router control: no `clickable` and no semantics click action (seam note 2). It acts at the lift within the slop, with no long press, and is inert under hit orders 0 and 1 (S-53).
  - **Drawing** in px (G-17): the gear and the second line's sizes are computed from the factor, never drawn under a layer scale. The factor is floored at 0.75; beyond it only the state word ellipsizes, never the number (S-22Δ3). Tags and texts are unchanged.
  - **The scaffolds:** correct `ControlsScaffoldTest.kt` :55–66 and `SafeAreaScaffoldTest.kt` :40 and :101 to the new geometry. Edit `ScreenSeamScaffoldTest.kt` only if it is red. Report each as a corrected check.
  - No `:app` change, no string, no new dependency. Never edit a kit file (seam note 6).
- **Run:**
  1. Write the main code and the scaffold corrections; compile the main sources (`:app:assembleDebug`); tell the orchestrator.
  2. After TASK-055 lands: the JVM line (1) + (2); kit compile; V-01 and its positive control.
  3. The floor script, then the dev script, per "What runs where".
- **Done means:** the TASK-056 row's Done-check.
- **Return:** the handoff block, with:
  - the gear's slot and the narrowed › as built (from which nodes, in dp), on the 360 dp and 411 dp phones;
  - the second line's factor on the 360 dp phone, in `en` and `fi`;
  - the scaffold corrections, before and after;
  - the counts per module; V-01's lines; the XMLs with their stamps and tree hashes.

---

## Brief — TASK-057 · WO-007 · slice-implementer

- **Mission:** S-52: the five sounds, made for this project by a deterministic Python stdlib tool, and their rows in `ASSET-LICENCES.md`. You land in one step with TASK-058 (§ Dispatch order, step 5). The class is mechanical; it runs on the station's Opus.
- **Read exactly:**
  1. `tasks.md` § WO-007: the intro (seam note 3), the "Files" row, the rows TASK-057 and TASK-058.
  2. `designs/WO-007-design.md`: §2 the row S-52; §3 "Carry 4".
  3. Requirements: the REQ folder's `REQ-033.md` (rules 1–2), `REQ-010.md` (rule 2).
  4. `architecture.md`: G-06, G-16.
  5. `decisions.md`: E7 with E7a (the recipes).
  6. `capture-outbox/chg-draft-licence-notice-and-assets.md` (the sound rule; pending).
  7. Design input (a behaviour reference only, never an oracle): DI-1 `Spec/prototype/tangram-prototype.html`, `sfx` and `tone()`.
  8. `ASSET-LICENCES.md`, all of it (its shape).
  9. The visible tests, never edited: the app JVM folder's `SoundAssetsKitTest.kt` (it lands with TASK-058); `content/src/test/kotlin/app/tangramplaytime/content/AssetLicencesKitTest.kt` (its `## Pictures, fonts, sounds` check).
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10 beyond the lines named above.
- **Inputs:** the TASK-057 row's Inputs cell.
- **Constraints:**
  - **The tool:** Python stdlib only; deterministic (no unseeded randomness, no timestamps in the files); it writes exactly the five files of the row, 16-bit mono at 22 050 Hz, each at most 1 s; the return soft and low (REQ-033 rule 2). No clipping.
  - **`ASSET-LICENCES.md`:** a new `## Sounds` section, one row per file; the `## Pictures, fonts, sounds` heading stays, and its "Sounds:" bullet points to the new section (seam note 3).
  - No Kotlin, no string, no test file, no dependency.
- **Run:**
  1. Write the tool, run it, write the § Sounds section; tell the orchestrator.
  2. After TASK-058 lands: the tool twice and `cmp` (the channel table's "Sound tool" row); the JVM line (1) + (2); V-01 and its positive control.
- **Done means:** the TASK-057 row's Done-check.
- **Return:** the handoff block, with each file's duration, size and peak level, the `cmp` result, the counts per module and V-01's lines.

---

## Brief — TASK-059 · WO-007 · slice-implementer

- **Mission:** S-49, the settings screen in the activity's window over the Play screen, with its strings in `en` and `fi`, and S-15Δ4's half that composes it and handles Back. The class is UI, so it goes to the strongest model.
- **Dispatch:** two hops (plan review S4, the orchestrator's split).
  - **Hop 1:** the code; the JVM line; kit compile; V-01 and its positive control; the floor script (the seven classes, the four `KitApp` classes, the death pair). Hand back.
  - **Hop 2,** the same context resumed: the dev script (the seven classes, the four `KitApp` classes on dev, the death pair), then the tablet script (the seven classes).
- **Read exactly:**
  1. `tasks.md` § WO-007: the intro (seam notes 4 and 5; the must-nevers), the "Files" row, the TASK-059 row, the coverage table.
  2. `designs/WO-007-design.md`: §1 "How it works"; §2 the rows S-49 (placement, tags, strings), S-15Δ4, and what it calls in S-1Δ4, S-2Δ4 and S-11Δ4; the how-to-play draft; §3 the must-nevers, "The layer must take every touch", library facts 1 and 5.
  3. `reviews/WO-007-design-review.md`: § Re-derived rows 1 and 8; § F1 re-check "Watch point for the code review".
  4. Requirements: the REQ folder's `REQ-032.md`, `REQ-009.md`, `REQ-049.md`, `REQ-029.md`, `REQ-030.md` (rule 3), `REQ-034.md`, `REQ-031.md`, `REQ-033.md`, `REQ-037.md`, `REQ-047.md`, `REQ-010.md`.
  5. `architecture.md`: G-09, G-10, G-12; O-05, O-06.
  6. `decisions.md`: E2, E3, E4, E5, E6, E10.
  7. `capture-outbox/`: `chg-draft-req032-how-to-play-text.md`, `chg-draft-req009-req049-finnish-wording.md` (the `fi` references), `chg-draft-licence-notice-and-assets.md` (the line). All pending; the build follows the locked REQs.
  8. Design inputs (behaviour references only, never an oracle): DI-1 `Spec/prototype/tangram-prototype.html` `#settings`; DI-2 `Spec/02-ui-layout.md` §1, §2 and §4; `design-inputs.md` §2 item 6.
  9. The visible tests, never edited: the app device kit folder's `SettingsKitTest.kt`, `ResetKitTest.kt`, `PlayThroughKitTest.kt`, `TouchTargetKitTest.kt`, `LanguageKitTest.kt`, `AppWiringKitTest.kt`, `ActiveTimeAppKitTest.kt`, `KitApp.kt` (the doors), `ProcessDeathKitTest.kt`; and the four classes stage 3's doors reach, `PhoneLayoutKitTest.kt`, `TabletLayoutKitTest.kt`, `DevAidAppKitTest.kt`, `DevPromptKeyboardKitTest.kt` (PL3a).
  10. Code and files: `app/src/main/kotlin/app/tangramplaytime/app/MainActivity.kt`; `app/src/main/res/values/strings.xml`, `app/src/main/res/values-fi/strings.xml`; `review-notes.md` (the row format). **Declarations only:** `PlayController`'s public members; `PlayTexts`' `timeOf` and `titleOf`; the `PlayScreen(…)` signature.
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10 beyond the lines named above.
- **Inputs:** the TASK-059 row's Inputs cell.
- **Constraints:**
  - **The window:** a sibling of `PlayScreen` in `setContent`, never a `Dialog` or a `Popup` (E3; the review's watch point: a `Popup` would also get its own click sound and miss the switch).
  - **The layer:** the scrim and the sheet are siblings, never nested (fact 1; design §3); the sheet's hit target takes every touch on it.
  - **The body** follows REQ-032 rule 1's order, as S-49 lists it; the reset confirmation is inline `remember` state, and only Erase calls `resetAll()`.
  - **Tags** exactly S-49's tag row. Each text entry is one resource, title or time, never concatenated.
  - **Strings** (G-10): every new string in `values/` and `values-fi/`; the free note and the privacy text as REQ-009 and REQ-049 word them; one `review-notes.md` row per new string, and the how-to-play text as a proof-reading item.
  - No difficulty control (G-09; REQ-032.A2). No permission, no new dependency, no `:play` change. Never edit a test file.
- **Run:**
  1. The JVM line (1) + (2); kit compile; V-01 and its positive control.
  2. The floor script, then the dev script, then the tablet script, per "What runs where": on the floor and dev the seven classes, then the four `KitApp` classes, each alone (PL3a), then the death pair; on the tablet the seven.
- **Done means:** the TASK-059 row's Done-check.
- **Return:** the handoff block, with:
  - the screen as drawn (the sheet's width on each image, where the body scrolls);
  - the new strings and their `review-notes.md` rows;
  - the counts per module; V-01's lines; per image, each class's XML counts, time, stamp and tree hash; the death pair's lines.

---

## Brief — TASK-061 · WO-007 · slice-implementer

- **Mission:** S-50 `Feedback`, the one door for every sound, haptic tick and tap click; S-51 `DeviceFeedback`, the platform playback behind it; and S-15Δ4's feedback half: `AppGraph` builds the door and gives the controller its events, and `MainActivity` provides `LocalSoundEffect` over everything it composes and gives the door its view. The class is device-only playback and the composition-local wiring on three images, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-007: the intro (the red windows; the must-nevers), the "Files" row, the TASK-061 row.
  2. `designs/WO-007-design.md`: §2 the rows S-50, S-51 and S-15Δ4; §3 the must-nevers, "Carry 4", "`onStop`'s `goHome`", "Outside the tap path", library facts 2, 3, 4 and 6.
  3. `reviews/WO-007-design-review.md`: F1, and § F1 re-check, all of it.
  4. Requirements: the REQ folder's `REQ-033.md`, `REQ-010.md` (rule 2).
  5. `architecture.md`: G-01, G-11, G-12; O-07.
  6. `decisions.md`: E7 with E7a, E8 with E8a, E11.
  7. Design inputs (behaviour references only): DI-1 `Spec/prototype/tangram-prototype.html` `sfx`, `tone()` and `haptic()`; `design-inputs.md` §2 item 4 (the prototype's `navigator.vibrate` is superseded by G-11).
  8. The visible tests, never edited: the app device kit folder's `SoundKitTest.kt`, `SettingsKitTest.kt`, `PlayThroughKitTest.kt`, `AppLaunchKitTest.kt`, `AppWiringKitTest.kt`, `KitApp.kt` (the feedback door), and the four classes `KitApp` and your provider reach, `PhoneLayoutKitTest.kt`, `TabletLayoutKitTest.kt`, `DevAidAppKitTest.kt`, `DevPromptKeyboardKitTest.kt` (PL3b); the app JVM folder's `FeedbackKitTest.kt` and `ReleasePromiseKitTest.kt`.
  9. Code: `app/src/main/kotlin/app/tangramplaytime/app/AppGraph.kt`, `app/src/main/kotlin/app/tangramplaytime/app/MainActivity.kt`; the five `app/src/main/res/raw/sfx_*.wav` (TASK-057's). **Declarations only:** `play/src/main/kotlin/app/tangramplaytime/play/PlayEvent.kt`; `PlayController.settings`.
  10. `platform-notes/android.md`, all of it.
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10 beyond the lines named above.
- **Inputs:** the TASK-061 row's Inputs cell.
- **Constraints:**
  - **One door:** `Feedback` is the only caller of a sound, a haptic tick or a click; both of its paths return first while sound is off (S-50; E8a).
  - **Playback:** one `SoundPool` with S-51's streams, usages and priorities; the tick through `View.performHapticFeedback` (G-11: never `Vibrator`); the click through `View.playSoundEffect`, as today's default makes it.
  - **The view:** set in `onStart`, cleared in `onStop` by the activity that set it (S-51).
  - **`AppGraph`:** the door is built synchronously, before the load, and is `internal`; the controller's `onEvent` is the door's (S-15Δ4).
  - **`MainActivity`:** the `LocalSoundEffect` provider wraps the whole of `setContent`'s content (S-15Δ4; the review's re-check item 1).
  - No permission (G-01), no new dependency, no string. Never edit a test file.
- **Run:**
  1. The JVM line (1) + (2); kit compile; V-01 and its positive control.
  2. The floor script, then the dev script, then the tablet script, per "What runs where": on the floor and dev the five classes, then the four `KitApp` classes, each alone (PL3b), then the death pair; on the tablet the five.
- **Done means:** the TASK-061 row's Done-check.
- **Return:** the handoff block, with:
  - where the provider sits and when the view is set and cleared;
  - the counts per module; V-01's lines; per image, each class's XML counts, time, stamp and tree hash; the death pair's lines.

---

## Brief — TASK-062 · WO-007 · slice-implementer

- **Mission:** Run every channel once, on the final, reviewed tree:
  - **(a)** the Python channel;
  - **(b)** the JVM line;
  - **(c)** the floor first, then dev: the whole play set, the whole app set (two invocations) and the death pair; then the tablet: the whole app set;
  - **(d)** V-01 and its positive control, and `run_verifiers.py --wo WO-007`.

  You make no product, kit, data or tool change of your own. The class is device-only, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-007: the intro (the channels, the device time, the lines covered twice, "Unverified by design"), the coverage table, the TASK-062 row.
  2. `workorders/WO-007.md`: § Constraints & notes (the verification channels, the device time); § Definition of Done.
  3. Requirements, read-only: the REQ folder's `REQ-032.md`, `REQ-033.md`, `REQ-034.md`, `REQ-009.md`, `REQ-008.md`, `REQ-029.md`, `REQ-031.md`, `REQ-049.md`, `REQ-001.md`, `REQ-010.md`, `REQ-037.md`, `REQ-047.md`.
  4. `decisions.md`: PL1–PL3, PL3a; 2026-10-18 "A comma never reaches an instrumentation argument …" (why the app set takes two invocations).
  5. Every kit folder, read-only: you run them.
  6. `platform-notes/android.md`, all of it; `.swdev/run-log/WO-007.md`, every row so far; the per-class times of the TASK-054 … TASK-061 hand-backs (the orchestrator gives them to you).
- **Do not read:** the kit author's briefs; the other implementer briefs; DI-1 … DI-10.
- **Inputs:** the TASK-062 row's Inputs cell.
- **Constraints:**
  - **Routing.** A red test is routed, not fixed: to the kit stage that wrote it (TASK-052, TASK-055, TASK-058 or TASK-060) or to the task that delivered its seam. A red test in a class that stage 3's `KitApp` doors reach goes to TASK-058 first. Report the test, the XML line and a short reading of why; the orchestrator dispatches.
  - **Re-runs.** After a fix, the failing class runs alone on that image, then the whole sets run once more, on the final tree.
  - **Skips** follow "Expected skips" in the channel section.
  - **The tablet** runs the app set only.
- **Run:** the Python channel → the JVM line → V-01 → kit compile → the floor script → the dev script → the tablet script. Use the channel table's timeouts.
- **Done means:** the TASK-062 row's Done-check (1)–(4).
- **Return:** the handoff block, with:
  - per channel: the counts, and each XML's stamp and tree hash;
  - per image: each class's time in the whole set, against the earlier per-class times;
  - every red test and where it was routed;
  - V-01's lines and the verifiers' budget line.
