# WO-009 — dispatch briefs (#Release: the marked release set, the mark command, the gate on the store build, the launcher and store icons, the store kit and the owner's checklist)

**Planner:** wo-planner (Opus 5.5)  ·  **Date:** 2026-10-22  ·  **Plan:** `tasks.md` § WO-009 (TASK-063 … TASK-071, in landing order; no task is `[P]`)
**Mode:** Light (`workorders/WO-009.md` § Mode). There is no design. `tasks.md` § WO-009 › Seams is the design: S-54 … S-63 and the Kit files row, the shared-file table, the eleven questions settled, the in-tree tests the WO touches and the coverage table.

> **Who sees what.** Each brief is dispatched on its own.
> - The Acceptance Test Author gets only its three briefs (TASK-064, TASK-066, TASK-069) and the two tables below. It never sees an implementer brief.
> - An implementer never sees another implementer's brief, nor a kit draft outside the tree.
>
> **Paths and folders.** Every path is relative to `C:/GitHub/AI/TangramPlaytime/` unless it is absolute. Folder names used below:
> - "the content kit folder": `content/src/test/kotlin/app/tangramplaytime/content/`;
> - "the JVM kit folder": `play/src/test/kotlin/app/tangramplaytime/play/`;
> - "the play device kit folder": `play/src/androidTest/kotlin/app/tangramplaytime/play/`;
> - "the app JVM folder": `app/src/test/kotlin/app/tangramplaytime/app/`;
> - "the app device kit folder": `app/src/androidTest/kotlin/app/tangramplaytime/app/`;
> - "the REQ folder": `Requirements/reqs/` (collection v1.2, read-only, G-02);
> - "the store kit": `release/` (new; S-59), with `PRIVACY.md` at the repository root;
> - "your scratch": `<scratch>/wo009/task-0NN/`, the folder the orchestrator names at dispatch. Copied XMLs, PNG copies, the signed throwaway APK, the repository copy and logs live there, never in the repo.
>
> **`decisions.md` rows** are cited by date and bold title; rows are only ever appended.
> - **In force:** 2026-10-22 "**Scope of WO-009: 10 IDs; three are the owner's evidence and three wait for his marks, so the close waits for him.**"
> - **The planner's readings R1–R13**, appended by the orchestrator before dispatch, cited by the titles the planner proposed. If the orchestrator changes a title or rules a reading the other way, it corrects the citations and the affected brief lines in the same edit.
>   - **R1** "The release ships only the puzzles the owner marked; the debug build keeps every puzzle."
>   - **R2** "The release-readiness bar, and the store build refuses below it."
>   - **R3** "The mark command: status, mark and unmark, one field changed byte for byte."
>   - **R4** "REQ-039 A2 and REQ-042 A1–A3 are read from the release APK; the REQ-042 legs stay red until the owner's marks."
>   - **R5** "REQ-002 A2's oracle: every player screen offers only the controls its REQs name."
>   - **R6** "REQ-048 A3 is the release manifest and the installed app; the throwaway-signed release install is evidence, not a token."
>   - **R7** "The launcher icon and the store icon are made from DI-9 by one generator."
>   - **R8** "The first upload's identity: app.tangramplaytime, version 1 (1.0.0), portrait not required, every language in the bundle's base."
>   - **R9** "The store kit lives in `release/`, the privacy page is `PRIVACY.md`, and the drafts carry a mechanical self-check."
>   - **R10** "The console answers: no data, no ads, rated for all ages, audience from 6 with the Families policy, Teacher Approved where offered."
>   - **R11** "The store pictures: a banner from the icon and the title; screenshots from the release build after the owner's marks."
>   - **R12** "The owner's marks do not reopen the device sets."
>   - **R13** "WO-009's carries: the convex-13 credit line is added; the licence line, the new-window guard, the D-pad sound and the library locales stay as they are."
> - **Earlier rows the briefs cite:**
>   - 2026-10-11 "**A missing `provenance` or `reviewedByHuman` reads as `false`.**" and "**A bundled puzzle that fails a rule fails fast.**"
>   - 2026-10-17 "**The promise lines' oracles.**" and "**Two readings of the device kit's closed-world checks.**"
>   - 2026-10-18 "**S1 recorded and carried, S2 fixed now, N1 carried, N2 to the owner's play list, N3 on the G4 surface.**" (the release dex allow-list by name)
>   - 2026-10-18 "**A phone is portrait from the manifest; a tablet is unlocked at run time**" (the `uses-feature` carry)
>   - 2026-10-18, the row whose title begins "The device's first locale" (S-36, Q7) (the first locale Android puts first for the app)
>   - 2026-10-21 "**The gear and the settings screen, as drawn.**" and "**The closed world of texts admits the settings screen's times …**"
>
> **Checkpoint items** (the orchestrator asks them on the G3 surface; the build follows the defaults):
> 1. which puzzles the release ships — only the marked, no cap; the debug build keeps all (R1, R2: a three-piece mini and the four warm-ups must be among the marks, or the store build refuses);
> 2. the app id `app.tangramplaytime` kept (R8);
> 3. the privacy page in this repository, the repository public (R9);
> 4. the declared age groups 6–8, 9–12 and 13+ (Play's 13–15, 16–17, 18 and over), Teacher Approved where offered (R10);
> 5. the store pictures — the banner from the icon and the title, phone and tablet screenshots (R7, R11).
>
> The listing texts are proof-reading items in `review-notes.md`, not checkpoint items.

## Dispatch order (two lanes)

| Step | Test lane (acceptance-test-author) | Implementer lane (slice-implementer) | Gate to the next step |
|---|---|---|---|
| 0 | — | — (orchestrator) Append R1–R13. Ask the five checkpoint items. Extend the run log's tree definitions (`tasks.md` § WO-009 seam note 5). `plan_check.py --wo WO-009` GREEN; G3 | G3 passed |
| 1 | TASK-064 may start drafting | **TASK-063** (the mark command) | TASK-063's Done-check green. The orchestrator tells the owner he can mark from now (checkpoint item 1) |
| 2 | **TASK-064 lands** (stage 1) | — | TASK-064 met at landing; REQ-039 A2 red in situ |
| 3 | TASK-066 may start drafting | **TASK-065** (the release set, the gate, the identity lines) | TASK-065's Done-check green, the positive control on a copy included |
| 4 | **TASK-066 lands** (stage 2) | — | TASK-066 met at landing (both device test APKs compile) |
| 5 | TASK-069 may start drafting from S-57 and S-59 | **TASK-067** (the icons, the manifest, `ASSET-LICENCES.md`, the release install leg; floor, dev, tablet) | TASK-067's Done-check green; the orchestrator has opened `release/pictures/icon-512.png` |
| 6 | — | **TASK-068** (the store kit) | TASK-068's Done-check green; the orchestrator has opened the banner |
| 7 | **TASK-069 lands** (stage 3) | — | TASK-069 met at landing; a red pin goes back to TASK-067 or TASK-068 |
| 8 | `plan_check.py --wo WO-009` again (the kit has compiled) → **Code Reviewer** (fresh) on the tree after TASK-069. Each fix lands, and its class re-runs alone | | The review is closed |
| 9 | — | **TASK-070**: every channel once, on the reviewed tree, before the owner's marks | TASK-070's Done-check green |
| 10 | Test & Verify (from the run log; it re-runs only what changed; REQ-042 A1–A3 are the expected red) | | |
| 11 | *The owner marks* (any time from step 1) until `python tools/mark_puzzles.py status` says `release-ready: yes` | | the owner's step |
| 12 | — | **TASK-071**: after the marks | TASK-071's Done-check green; the orchestrator has opened every screenshot |
| 13 | Test & Verify (the delta) → *the owner's console steps and his three signatures* → trace-check `--wo WO-009` GREEN → close | | |

- **Why this order.**
  - **The mark command first** (WO question 2): the owner marks while the rest is built.
  - **The kit before the change it pins**, where the kit reads only build outputs or the installed app (stages 1 and 2). Each such stage shows its oracle red on today's tree: the unfiltered release APK (REQ-039 A2), the default launcher icon (REQ-048 A4).
  - **Stage 3 after the files it reads** (TASK-067, TASK-068), as `task-decomposition.md` asks for a test that reads files outside its module (the § WO-010 TASK-041 precedent).
  - **TASK-065 is the product risk**, right after stage 1.
  - **Code review before TASK-070**, so the whole sets run once, on the reviewed tree.
  - **TASK-071 after the owner's marks:** the release APK can only start, and the screenshots can only show what ships, once there is a release set.
- **The expected red.** `ReleaseSetKitTest`'s release legs (REQ-042 A1–A3 and the release-set pins) are red from TASK-065 until the owner's marks. **Run 2 of the JVM line runs with `--continue` during WO-009** and is judged by the XMLs: "green" in a Done-check means every test but those legs, unless the row says otherwise.
- **Red windows** (`tasks.md` § WO-009 intro). Nobody runs a set for a verdict inside one:
  - between TASK-064 and TASK-065: `ReleaseMarksKitTest`'s REQ-039 A2 leg (in situ);
  - between TASK-066 and TASK-067: `LauncherIconKitTest` at run time (no app device class runs for a verdict).
- **Compile errors in the kit** are the kit author's defect and count as a corrected check. No stage names a symbol or resource id an implementer adds (seam note 3), so every stage compiles at its landing.
- **Who edits tests** (seam note 6): kit files are the author's; `tools/tests/test_mark_puzzles.py` and `tools/tests/test_make_icon.py` are the implementers'. An implementer whose change breaks any other test hands back `recirculate → acceptance-test-author`, naming the file and the line.
- **Decisions the implementers record.** A number or reading that no REQ, seam row or `decisions.md` row fixes goes back as a proposed row, with its source and the alternative. The orchestrator appends it, so each is recorded once.
- **Hard limits for every station** (seam note 7): never set `reviewedByHuman: true`, never run `mark` or `unmark` on `Tangrams/`, never edit a puzzle file; no signing config, key, key store, secret, account or console entry (the throwaway debug-key copy in your scratch is the only signature); never edit `IPuzzleLibrary.kt`, `Tangrams/puzzle.schema.json`, `workorders/WO-009.md`, `decisions.md` or `owner-evidence.md`; no permission, network, ad or purchase code; no new window.
- **Device-time rules** (`platform-notes/android.md`; KICKOFF "State after WO-007"):
  - the floor image first, one emulator at a time, booted detached;
  - one background script per image, judged by the XML;
  - the 40-minute no-test-finished watchdog;
  - while fixing, the failing class alone; the whole sets once, on the reviewed tree (TASK-070);
  - after a stopped or killed run, reboot the image before its next step;
  - a station never owns an emulator as its own background task.

## Verification channels (G3)

**Conventions for every command below.**
- "The line" is `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat <tasks>"`.
- `ADB` is `/c/Users/Jami/AppData/Local/Android/Sdk/platform-tools/adb`; `EMU` is `/c/Users/Jami/AppData/Local/Android/Sdk/emulator/emulator`; `BT` is `/c/Users/Jami/AppData/Local/Android/Sdk/build-tools/36.0.0` (`zipalign.exe`, `apksigner.bat`, `aapt2.exe`); `CHROME` is `"/c/Program Files/Google/Chrome/Application/chrome.exe"`.
- Python runs with `PYTHONUTF8=1` (the cp1252 console).
- **Every adb, emulator, Chrome and Gradle device call is wrapped in `timeout`.** The bounds:
  - a boot 300 s;
  - a single class 3600 s (`TouchTargetKitTest` takes about 13 min on each image);
  - the whole play set 7200 s (about 48 min on the floor and 59 min on dev, run log WO-007 t062);
  - the whole app set's rest 3600 s (about 23 min per image);
  - `tools/release_install.sh` 600 s; `tools/store_screenshots.sh` 900 s; Chrome 60 s.

| Channel | How (Git Bash) | Result | Needed by |
|---|---|---|---|
| Python tool channel | `PYTHONUTF8=1 python tools/validate_puzzles.py Tangrams`; `PYTHONUTF8=1 python C:/GitHub/AI/SwReqCollector/tools/validate.py Requirements`; the tool tests run directly: `python tools/tests/test_mark_puzzles.py`, `python tools/tests/test_make_icon.py`, `python tools/tests/test_tangram_geom.py`, `python tools/tests/test_import_figures.py`; `PYTHONUTF8=1 python tools/mark_puzzles.py status` (read-only on `Tangrams/`) | "106/106 puzzle files valid"; "0 errors" (warnings allowed); each test file's exit 0; the status lines | TASK-063, TASK-067, TASK-070, TASK-071; Test & Verify |
| Picture tools | `PYTHONUTF8=1 python tools/make_icon.py` twice, then `cmp` of each of its four outputs against a copy taken after the first run; `PYTHONUTF8=1 python tools/make_feature_graphic.py` twice, `cmp` of the SVG | identical bytes (the banner's PNG is not byte-compared: S-60) | TASK-067, TASK-068 |
| Eyes (the AI's look) | Open the PNG with the Read tool: `release/pictures/icon-512.png`, `release/pictures/feature-graphic.png`, each screenshot, a device screen capture. The orchestrator opens each before accepting the hand-back. `tools/svg2png.py` needs Playwright, which is not installed: do not install it | one line per picture in the hand-back | TASK-067, TASK-068, TASK-071 |
| JVM (JUnit 4) | **Two runs of the line, in this order** (run log g3-jvm): **(1)** `:app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun`, **then (2)** `--continue :content:testDebugUnitTest --rerun :play:testDebugUnitTest --rerun :kernel:test --rerun :contracts:test --rerun :store:testDebugUnitTest --rerun :app:testDebugUnitTest --rerun`. Run (2) without run (1) first is not a verdict (the WO-005 lesson). `--continue` stays until TASK-071 (the expected red); Gradle's exit code is then not the verdict | `*/build/test-results/**/TEST-*.xml`, each stamped after run (2) started. Baseline g3-jvm: app 10, content 80, contracts 2, kernel 70, play 146, store 12 = 320/0 | every task |
| Kit compile | The line with `:play:assembleDebugAndroidTest :app:assembleDebugAndroidTest` | the exit code and the compiler messages | TASK-064, TASK-066, TASK-067, TASK-070 |
| The store artifact's gate (S-56) | The line with `:app:bundleRelease`. Before the owner's marks it must fail at `:content:checkReleasePuzzles` with the five items; after them it builds `app/build/outputs/bundle/release/app-release.aab`. List the AAB's puzzle folder with `unzip -l` (or a scratch script) | the gate's message, or the AAB and its listing | TASK-065, TASK-070, TASK-071 |
| **The device script — one per image** | 1. `$ADB emu kill`; `$ADB devices` until it is empty.<br>2. Boot detached: `nohup $EMU -avd <image> -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot > <your scratch>/emu-<image>.log 2>&1 &`.<br>3. Wait for `sys.boot_completed` = 1. `$ADB devices` must show `emulator-5554` alone.<br>4. Record `getprop ro.build.version.sdk`, `wm size`, `wm density` and `settings get secure navigation_mode`.<br>5. `$ADB uninstall app.tangramplaytime` and `app.tangramplaytime.test` ("not installed" is fine).<br>6. `bash tools/device_reset.sh`, then `bash tools/device_reset.sh --check` (clean), then `$ADB shell dumpsys window \| grep mCurrentFocus`.<br>7. The classes, **one class per invocation**: the line with `:play:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.play.<Class>` (or `:app:…` with `app.tangramplaytime.app.<Class>`). A comma list through `cmd.exe //c` runs only its first class. **Whole sets** (TASK-070 only): the play set `:play:connectedDebugAndroidTest`; the app set in two invocations, `…class=app.tangramplaytime.app.DevPromptKeyboardKitTest` alone, then the rest with `-Pandroid.testInstrumentationRunnerArguments.notClass=app.tangramplaytime.app.DevPromptKeyboardKitTest`.<br>8. After each invocation: copy its XML to your scratch (the folder is overwritten), then reset + `--check` + focus.<br>9. The release install leg or the screenshots, where the table below says so.<br>10. A final reset + `--check`; `$ADB emu kill`; `$ADB devices` empty.<br>**The watchdog:** if no test has finished for 40 min (no new `TestRunner: finished` line in `$ADB logcat -s TestRunner`), stop the step: end the Gradle invocation, `gradlew --stop`, `$ADB emu kill`. Record it; the image is rebooted before its next step | `play/build/outputs/androidTest-results/connected/debug/TEST-*.xml`, `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`. Judge a run by its XML, never by Gradle's exit code: the XML exists, is stamped after the command started and carries the expected counts. Under `-q`, a failed install exits 0 and writes no XML | every device step |
| Floor image **`Phone_API_31`** (Android 12; 1080 × 2400 px at 420 dpi = 411 × 914 dp) — **always first** | The script with `-avd Phone_API_31`. Proven: run log WO-007 g3-floor and WO-009's g3 rows | the same folders (the file name carries the device) | TASK-067, TASK-070, TASK-071 |
| Dev image `Medium_Phone_API_37.0` (411 × 914 dp; gesture navigation, `navigation_mode` 2) | The script with `-avd Medium_Phone_API_37.0` | the same | TASK-067, TASK-070, TASK-071 (screenshots) |
| Tablet image `Medium_Tablet_API_37.0` (2560 × 1600 px at 320 dpi = 1280 × 800 dp, landscape) | The script with `-avd Medium_Tablet_API_37.0`. App classes only: the play set hosts `PlayScreen` at the kit's phone size (WO-006 practice) | the app folder | TASK-067, TASK-070, TASK-071 (screenshots) |
| The release install leg (S-62) | Inside an image's script, after run (1) of the JVM line: `bash tools/release_install.sh <your scratch>/release` (TASK-067, TASK-070); `… --launch` only after the owner's marks (TASK-071). Reset + `--check` before and after | its PASS/FAIL lines and exit code; the signed copy stays in your scratch | TASK-067 (floor, dev), TASK-070 (floor), TASK-071 (floor) |
| The store screenshots (S-61) | After the owner's marks, inside the dev and the tablet scripts: `bash tools/store_screenshots.sh` as TASK-071 defines it. Reset + `--check` after | the PNGs in `release/pictures/screenshots/`, each looked at | TASK-071 |
| Release verifier V-01 (G-01, G-08) | After run (1) of the JVM line: `python .swdev/verifiers/v01_release_apk.py`, `python .swdev/verifiers/v01_release_apk.py --positive-control`, then `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-009` | the PASS lines, GREEN, and the budget line (expected: 0 verifiers, 0 lines for WO-009) | TASK-065, TASK-067, TASK-070, TASK-071 |
| Device hygiene | Reset, `--check` and `mCurrentFocus` before and after every device step; `--check` names a focused crash or not-responding window (S-37Δ). A non-clean check after a reset voids the step: reset, re-run, record the leftover. A check that names a crash window means a reboot of the image | `--check` clean or not; the focus line | every device step |

**What runs where.** "Alone" means one class per invocation, each with a reset around it.

| Task | Floor `Phone_API_31` | Dev `Medium_Phone_API_37.0` | Tablet `Medium_Tablet_API_37.0` |
|---|---|---|---|
| TASK-067 | before the change: `LauncherIconKitTest` alone (red expected: the default icon). After it: `LauncherIconKitTest`, `SettingsKitTest`, `AppLaunchKitTest` (app) and `NoToddlerModeKitTest` (play), alone; S-62 | the same, without the before-run | `LauncherIconKitTest`, `SettingsKitTest`, `AppLaunchKitTest`, alone |
| TASK-070 | the whole play set; the whole app set (two invocations); S-62 | the whole play set; the whole app set | the whole app set |
| TASK-071 | S-62 with `--launch`, and one screen capture looked at | S-61 (phone) | S-61 (tablet) |

**Device time** (informative; per-class times from `.swdev/run-log/WO-007.md` t059–t062): about 55 min at TASK-067 (floor about 25 with the before-run, dev about 20, tablet about 10), about 3 h 20 min at TASK-070 (WO-007's three-image run of 3 h 07 min plus the new classes and S-62), about 30 min at TASK-071; **about 4 h 45 min in all**, fix loops excluded. `NoToddlerModeKitTest` is unmeasured: TASK-067 times it on the floor before running it elsewhere. Each hand-back reports its per-class times.

**Expected skips.**
- The only legitimate skips are the two guarded `ProcessDeathKitTest` phases in an ordinary connected run.
- On the floor (API 31 < 33), `LanguageKitTest`'s three `@SdkSuppress(minSdkVersion = 33)` legs are filtered: they are absent from the XML, which is not a skip.
- Any other skip in an XML is a failure.

**Other channels.**
- **Human eyes, the owner's:** his marks (REQ-039 rule 2); the live listing (the three `owner-evidence.md` rows); the `review-notes.md` rows (the listing texts en and fi, the privacy page, the icon, the banner, the screenshots).
- **Browser:** headless Chrome as the banner's renderer only (S-60).
- **Not a WO-009 channel:** the process-death pair (no persistence or lifecycle change).
- **Toolchain proof:** `.swdev/run-log/WO-009.md` rows g3-py, g3-jvm, g3-v01 (2026-10-22 05:19) and the device g3 rows.
- **Time (G-14):** WO-009 adds no time value to product code. Wait for an observed state, always with a bound; never sleep.

---

## Brief — TASK-064 · WO-009 · acceptance-test-author

- **Mission:** Stage 1 of WO-009's one acceptance kit, landed before TASK-065 (kit first). The class is JVM oracles over a build output.
  1. New `play/src/test/kotlin/app/tangramplaytime/play/ReleaseMarksKitTest.kt`: REQ-039 A2's token over the puzzle files the release APK carries.
  2. New `content/src/test/kotlin/app/tangramplaytime/content/ReleaseSetKitTest.kt`: REQ-042 A1, A2 and A3's tokens over the same files, loaded through `:content`'s loader, and the untokened release-set pins.
  3. REQ-048 A3's token added beside REQ-010 A2's on two existing tests.
- **Read exactly:**
  1. `tasks.md` § WO-009: the intro (lanes; the expected red and the windows; seam notes 1–7; the in-tree tests the WO touches; pending drafts; not covered); § Seams: S-54 (the bar's words), S-55, S-56, the Kit files row; the eleven questions 1, 2, 5 and 11; the coverage table; the rows TASK-064 (yours), TASK-065 (what turns REQ-039 A2 green) and TASK-071 (when REQ-042 turns green).
  2. `workorders/WO-009.md`: § Scope and its notes; § Constraints & notes, questions 1, 5 and 11; the standing constraints.
  3. The REQ folder, read-only: `REQ-039.md` (rule 2, A2), `REQ-042.md`, `REQ-045.md`, `REQ-041.md`, `REQ-040.md`, `REQ-007.md` (the pins' statements), `REQ-048.md` (A3, rule 8), `REQ-010.md` (A2). `Requirements/req_types.md`: TYPE-001.
  4. Frozen contracts: `contracts/src/main/kotlin/app/tangramplaytime/contracts/IPuzzleLibrary.kt` (the `puzzles` KDoc: the order; "never empty in a shipped build") and `contracts/src/main/kotlin/app/tangramplaytime/contracts/Types.kt` (`PuzzleDefinition` with `reviewedByHuman`, `PuzzleKind`).
  5. `Spec/03-puzzle-format.md` §2 (the fields: `category`'s list, `kind`, `provenance`) and `Tangrams/puzzle.schema.json` (read only; locked).
  6. `decisions.md`: the scope row; R1, R2, R4, R6; 2026-10-11 "**A missing `provenance` or `reviewedByHuman` reads as `false`.**"; 2026-10-17 "**The promise lines' oracles.**"; 2026-10-18 "**S1 recorded and carried, S2 fixed now, …**".
  7. In-tree, the patterns you follow (read, not called): `play/src/test/kotlin/app/tangramplaytime/play/ReleaseBuildKitTest.kt` and `app/src/test/kotlin/app/tangramplaytime/app/ReleasePromiseKitTest.kt` (reading an APK as a zip; the project root; the staleness refusal; the build line in the failure; every check printed before failing). In the content kit folder: `KitTexts.kt` (`kitEngine()`, `kitShipped()`, `kitHandMade`, `kitEdited`, `kitReviewed()`, `kitNoProvenance()`, `kitTangramsDir()`, `kitTangramsPuzzleIds()`: reuse, never change), `PuzzleLibraryKitTest.kt` (its REQ-042 A2 and A3 tokens over the whole library stay as they are) and `PuzzleLoaderKitTest.kt` (:225, the missing-provenance reading). **Declarations only, never bodies:** `content/src/main/kotlin/app/tangramplaytime/content/PuzzleLoader.kt` (`internal object PuzzleLoader`, `fun load(sources, engine): LoadResult`, `Rule`), `BundledPuzzles.kt` (`PuzzleSource`, `DIR`).
  8. The two tests you add a token to: `app/src/test/kotlin/app/tangramplaytime/app/ReleasePromiseKitTest.kt` (`theReleaseApkDeclaresNoPermission`) and `app/src/androidTest/kotlin/app/tangramplaytime/app/PlayThroughKitTest.kt` (`theInstalledAppDeclaresNoPermissionsNotEvenNetworkAccess`).
  9. Rules: `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2; `C:/GitHub/AI/SWDev/framework/methodology/task-decomposition.md` ("Assert only what is mandated", "Declared inputs land together"); `platform-notes/android.md` § Verification channels (the staleness and two-run lessons).
- **Do not read:** the bodies of every module's main code; `content/build.gradle.kts`'s task bodies beyond `BundlePuzzles`' output layout; the implementer briefs (every brief here but TASK-064, TASK-066 and TASK-069); `tools/mark_puzzles.py`; DI-1 … DI-4, DI-10; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`; the release APK (`app/build/outputs/apk/release/app-release-unsigned.apk`) from run (1) of the JVM line, with the staleness sources of your row.
- **What the oracles read** (S-55; R4):
  - The release APK is a zip. Its Java resources lie at its root: `app/tangramplaytime/content/puzzles/index.txt` (the sorted file names, one per line) and each file it names. Read the files `index.txt` names; a named file missing from the zip fails. A missing or stale APK is refused with the build line, as `ReleasePromiseKitTest` does. Your staleness sources are the ones your row names, the top-level `Tangrams/*.json` among them, so a mark after a build refuses the old APK.
  - **REQ-039 A2** ("No released puzzle has reviewedByHuman: false"): every file the release APK carries has `provenance.reviewedByHuman` `true`; a missing `provenance` or field counts as `false` (the 2026-10-11 row). `:play`'s test classpath has no JSON library of `:content`'s, so read the field with your own small, strict reader. Liveness: `index.txt` was read (it may name zero files: an empty release ships no unreviewed puzzle; REQ-042 A1 is what catches emptiness). Discrimination in the same class: your check over crafted texts (one with `false`, one with no `provenance`, one with `true`) flags exactly the first two.
  - **REQ-042 A1–A3** (`content/`, the code home): load the release files as `PuzzleSource`s through `PuzzleLoader.load(sources, kitEngine())`. A1: at least 20 listed puzzles with `reviewedByHuman` true. A2: at least four distinct `category` values among the release's puzzles ("At least four categories are present"). A3: `shapes-square` among them (as locked; the `chg-draft-acceptance-content-ids.md` rewording is pending). A load that is `Rejected` fails each leg with the violations.
  - **The release-set pins** (no token): the release files are exactly the top-level `Tangrams/*.json` files (the schema excluded) whose provenance says `true` (S-55); the release set loads with no violation (REQ-007 statement); its list starts with a mini of exactly three pieces and rating 1 (REQ-045 statement and rule 3) and then at least four warm-ups of rating 1 (REQ-041 statement).
  - **Fixtures that discriminate**, crafted through the kit's doors (`kitHandMade`, `kitEdited`, `kitReviewed`), each failing its own check: 19 marked; 20 marked over three categories; 20 marked without `shapes-square`; 20 marked plus one unmarked (A1 counts only the true ones); no three-piece mini; three warm-ups.
  - **REQ-048 A3** (R6): add the token beside REQ-010 A2's on the two named tests, with a comment citing REQ-048 A3's sentence and R6. Change no assertion.
- **Oracle rule:**
  - Every assertion cites, in a comment, the sentence it checks: a REQ sentence, a TYPE rule, a seam row (`S-n`) or a `decisions.md` row by date and title. No source, no assertion.
  - A REQ token goes only on a test whose assertion is that line's meaning; a pin carries its source and no token. Cite rules without a dotted ID in prose ("REQ-045 statement"). **Never write REQ-001.A2, REQ-048.A1 or REQ-048.A2 in a test file** (seam note 4): write them with a space.
  - No `assume…`; nothing is skipped; every check is printed before the test fails with every failed check. Call `internal` members directly; no reflection. Helper names carry a `kit` / `Kit` / `KIT_` prefix.
- **Run:**
  1. Write the files and the edits; compile (`:content:compileDebugUnitTestKotlin :play:compileDebugUnitTestKotlin :app:compileDebugUnitTestKotlin :app:assembleDebugAndroidTest`, or the kit compile line).
  2. Run (1), then run (2) with `--continue`. Expected: REQ-039 A2 red on today's release APK (it carries the 106 unmarked files: the in-situ fixture), naming the count; the crafted-fixture legs green; `ReleaseSetKitTest`'s release legs over the unfiltered APK (all 106 files, none marked): REQ-042 A1 and the set-equality pin red, each naming its count, and REQ-042 A2, A3 and the valid-load, mini and warm-up pins green. Those three reds are the only ones; everything else is green.
  3. Grep for `REQ-0[0-9]*\.A[0-9]` as the row's Done-check (3) says.
- **Done means:** the TASK-064 row's Done-check, met at landing; JUnit 4; no new dependency; never edit main code, a build file, `tools/`, a scaffolding test or a data file.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with: each test and its source; each fixture and the check it fails; the JVM counts per module and the reds with their messages; the grep's result. Stay available: TASK-066 and TASK-069 are your stages 2 and 3, in this context.

---

## Brief — TASK-066 · WO-009 · acceptance-test-author (stage 2; TASK-064's author, context resumed)

- **Mission:** Stage 2 of the kit, landed before TASK-067 (kit first): the device oracles.
  1. New `app/src/androidTest/kotlin/app/tangramplaytime/app/LauncherIconKitTest.kt`: REQ-048 A4, the installed app's leg.
  2. New `play/src/androidTest/kotlin/app/tangramplaytime/play/NoToddlerModeKitTest.kt`: REQ-002 A2 over every player screen of `:play` (the code home).
  3. `app/src/androidTest/kotlin/app/tangramplaytime/app/SettingsKitTest.kt`: REQ-002 A2's settings half.
  The class is Compose device legs and pixel sampling, so it goes to the strongest model.
- **Read exactly** (in addition to what you read for TASK-064):
  1. `tasks.md` § WO-009 › Seams: S-57 (what the icon is, and what it is not), S-58 (the manifest's icon line), S-63 (REQ-002 A2's oracle), the Kit files row; seam note 3; the rows TASK-066 (yours) and TASK-067 (where your classes run).
  2. The REQ folder: `REQ-048.md` (rule 2, A4), `REQ-002.md` (rule 2, A2), `REQ-032.md` (rule 1: the settings screen's items), `REQ-046.md` (the debug aid). `Requirements/req_types.md`: TYPE-001 (the piece colours DI-9 uses).
  3. **DI-9**, the reference for "matches": `Spec/icon/option-A-foreground.svg`, `Spec/icon/option-A-background.txt`, `Spec/icon/README.txt`. Read the paths as data and derive your sample points from them (a point well inside each of the seven pieces; background points inside the adaptive icon's visible window and outside every piece). Cite the file and the path in the test.
  4. `decisions.md`: R5, R7; 2026-10-17 "**Two readings of the device kit's closed-world checks.**"; 2026-10-21 "**The gear and the settings screen, as drawn.**" and "**The closed world of texts admits the settings screen's times …**".
  5. The tag tables: `tasks.md` § WO-003 › Seams S-13 (the tag table) and its Δ rows in § WO-004, § WO-005 (the `dev-*` tags of the debug aid) and § WO-007 (S-53, the gear; S-49's tags). **Declarations only:** `play/src/main/kotlin/app/tangramplaytime/play/PlayTags.kt` and the `Control` enum of `play/src/main/kotlin/app/tangramplaytime/play/HitTest.kt`.
  6. In-tree kit, the helpers you reuse (never change; add `kit`-prefixed ones): the play device kit folder's `KitScreen.kt` (the host; `open`, `placeByApi`, `solveByApi`, `tagsAndTexts()`, `allNodes`-style reads) and `OverviewKitTest.kt` (how the grid is opened and read); the app device kit folder's `KitApp.kt` (`start`, `firstNew`, `openByController`, `openSettingsByTouch`, `allNodes`, `closedWorld`, `appStrings`) and `SettingsKitTest.kt` (`noDifficultyControlIsPresent`, `kitControlsShown`).
  7. `platform-notes/android.md` § Compose test kit (bound every Compose call; read positions on the screen in use; node reads stay out of per-sample loops — the grid holds 106 cells; a pin that skips cannot fail).
- **Do not read:** as for TASK-064, plus `tools/make_icon.py` and every implementer hand-back.
- **Inputs:** `Spec/icon/option-A-foreground.svg`, `Spec/icon/option-A-background.txt`; `Tangrams/*.json`; `app/src/main/res/values/strings.xml`, `app/src/main/res/values-fi/strings.xml`, `play/src/debug/res/values/strings.xml`, `play/src/debug/res/values-fi/strings.xml`.
- **What each class checks:**
  - **`LauncherIconKitTest`** (REQ-048 A4: "The installed app shows a launcher icon that matches the store icon; the icon contains no text."):
    - the launcher activity of the app under test, as `PackageManager.queryIntentActivities` resolves `ACTION_MAIN` / `CATEGORY_LAUNCHER` for its package, is exactly one, `MainActivity`;
    - its icon, loaded as a launcher loads it, is an `AdaptiveIconDrawable`, and its icon resource is not `android.R.drawable.sym_def_app_icon`;
    - drawn at a fixed square size (draw the background and foreground layers over the 108-unit canvas, or the masked icon over its 72-unit window: say which, and keep every sample point inside what you draw), its pixels at your DI-9 sample points equal DI-9's piece colours and background colour, within a tolerance you state and justify (the device's colour conversion);
    - the positive control in the same test: the platform default icon drawn the same way fails that comparison;
    - the store-icon side of "matches" and "no text" are stage 3's (`StoreIconKitTest`, TASK-069): both sides match DI-9 (S-57).
    - **Never name `R.mipmap.ic_launcher` or any resource TASK-067 adds** (seam note 3): the class must compile today, and fails at run time until TASK-067 (the default icon: the in-situ fixture).
  - **`NoToddlerModeKitTest`** (REQ-002 A2: "No screen offers a toddler or picture-matching mode."; S-63): host `PlayScreen` with the real library and engine at the kit's phone size, and walk the Play screen New, the Play screen In progress, the solved view and the grid overview. On each:
    - every node with a click, long-click or toggle action, and every router control the screen draws (the `Control` set), is one of the controls the tag tables name for that screen; `dev-*` nodes are the debug aid's (REQ-046), outside the player's screens, and their absence from the release is `ReleaseBuildKitTest`'s REQ-046 A4;
    - every visible text is one the screen's REQs give it (the bundle's titles, the counter's "n / N", the state words, the times);
    - the planted controls, in the same class: a kit host that composes `PlayScreen` with one extra clickable text "Toddler mode" beside the board, and a second with an untagged clickable icon box; each must fail the checks, which name the planted node.
    - Time the class on the floor at 106 puzzles before relying on it elsewhere (read the grid's cells once, never per sample).
  - **`SettingsKitTest`**, the settings half: `noDifficultyControlIsPresent` already asserts that the screen's controls, at every scroll position, are exactly S-49's. REQ-002 A2's token goes there with a comment citing REQ-002 rule 2 and S-63, or on a new leg doing the same; your call, with the reason in the hand-back. Its texts are `PlayThroughKitTest`'s closed-world walk (unchanged).
- **Oracle rule:** as for TASK-064. Every planted fixture and every positive control lives in the same class as the check it proves.
- **Run:**
  1. Kit compile: `:play:assembleDebugAndroidTest :app:assembleDebugAndroidTest` with no error.
  2. The JVM line stays as TASK-065 left it: run 2 with `--continue`, and the only red is the expected one, `ReleaseSetKitTest`'s release legs (REQ-042 A1–A3 and the release-set pins), each naming its count. Your landing opens one window: `LauncherIconKitTest` fails at run time until TASK-067 lands, and no app device class runs for a verdict in between.
  3. Grep for `REQ-0[0-9]*\.A[0-9]` as the row's Done-check (3) says.
  You run no device class: the device verdicts are TASK-067's.
- **Done means:** the TASK-066 row's Done-check, met at landing, with the TASK-064 hygiene.
- **Return:** the handoff block, with: each class's legs and their sources; the DI-9 sample points and colours, with the path each came from; the tolerance and its reason; the planted fixtures; the token placement in `SettingsKitTest` and why.

---

## Brief — TASK-069 · WO-009 · acceptance-test-author (stage 3; the same author, context resumed)

- **Mission:** Stage 3 of the kit, landed after TASK-067 and TASK-068 (it reads their files) and met at landing. Pins over files, on the JVM.
  1. New `app/src/test/kotlin/app/tangramplaytime/app/StoreIconKitTest.kt`: REQ-048 A4, the store leg.
  2. New `app/src/test/kotlin/app/tangramplaytime/app/StoreListingKitTest.kt`: S-59's mechanical self-check and S-60's size rule, untokened.
  You may draft from TASK-066's landing, against S-57, S-59 and S-60; you land when the orchestrator says TASK-068 is done.
- **Read exactly** (in addition to stages 1 and 2):
  1. `tasks.md` § WO-009 › Seams: S-57, S-59 (its last block, "The mechanical self-check", is your list), S-60; seam note 4; the rows TASK-067 and TASK-068 (the files), TASK-069 (yours).
  2. The REQ folder: `REQ-048.md` (rules 1–4, the ASSUMPTIONs, A1, A2, A4), `REQ-049.md` (rule 1: the text in full), `REQ-001.md`, `REQ-010.md`.
  3. `capture-outbox/chg-draft-req048-store-channel.md`, `capture-outbox/chg-draft-req048-console-declarations-acceptance.md` (pending: what is not yet Contract).
  4. `decisions.md`: R7, R9, R10, R11.
  5. Data, read at run time and by you: the store icon and the three icon resource files (TASK-067's); the store kit's listing files, `release/console/app-content.md`, `PRIVACY.md`, `release/pictures/feature-graphic.png` (TASK-068's); DI-9 (`Spec/icon/`). They are data, not implementation.
  6. `app/src/test/kotlin/app/tangramplaytime/app/ReleasePromiseKitTest.kt`: its binary-XML manifest reader (read, not called).
- **Do not read:** as before, plus `tools/make_icon.py`, `tools/make_feature_graphic.py` and the implementers' hand-backs.
- **Inputs:** the row's Inputs cell (TASK-067's and TASK-068's files; DI-9; the release APK of run 1).
- **What each class checks:**
  - **`StoreIconKitTest`** (REQ-048 A4; S-57):
    - `release/pictures/icon-512.png` is 512 × 512, a 32-bit PNG (RGBA, colour type 6) with every alpha 255 (`decisions.md` 2026-10-22 "Amends … (R7): the 512 × 512 store icon is a 32-bit PNG (RGBA, colour type 6) with every alpha 255 …"); a fixture with one alpha below 255, and one 24-bit RGB file, each fail; at your DI-9 sample points, mapped through S-57's window (x and y 18 to 90 of 108, scaled by 512 / 72), its pixels equal DI-9's piece and background colours (anti-aliasing touches edge pixels only, so choose interior points);
    - `ic_launcher_foreground.xml` is a vector with a 108 × 108 viewport whose paths are DI-9's, in order, `pathData` equal to the SVG's `d` and `fillColor` to its `fill`; `ic_launcher.xml` is an adaptive icon of exactly a background (the colour resource, equal to DI-9's background) and a foreground (that vector), and no other layer;
    - no text: DI-9's SVG holds only `path` elements with a `fill` and a `d` (no `text`, `tspan`, `image` or font); the vector holds only `path` elements;
    - the release APK's manifest: `application`'s `icon` is a resource reference into the app's own package, not the framework's;
    - crafted fixtures, each failing its check: a PNG with a stripe of another colour across a piece; a vector with an eighth path; an SVG with a `text` element.
  - **`StoreListingKitTest`** (no token; seam note 4): S-59's self-check list, item by item, each with its source (REQ-048 rule 1 and its ASSUMPTIONs, CHG-001, REQ-049 rule 1, REQ-048 rules 3 and 4, Play's limits: title 30, short description 80, full description 4000, the feature graphic 1024 × 500 as JPEG or 24-bit PNG). Count characters as Unicode code points. The promise words are REQ-048 rule 1's, with its em dash. The Finnish wording is never asserted. Crafted fixtures fail: a title with "Free"; an 81-character short description; a short description without the promise words; a privacy page missing one sentence; a data-safety answer "Yes"; a 1024 × 512 banner.
- **Oracle rule:** as for TASK-064. A red pin is a finding against TASK-067's or TASK-068's file: report the file, the line and the sentence; never edit the data.
- **Run:** run (1), then run (2) with `--continue`; then the grep of the row's Done-check (2).
- **Done means:** the TASK-069 row's Done-check, met at landing, with the TASK-064 hygiene.
- **Return:** the handoff block, with each pin and its source, the fixtures, the JVM counts, and any finding against a file.

---

## Brief — TASK-063 · WO-009 · slice-implementer

- **Mission:** S-54, the mark command, and the marking paragraph of `Tangrams/README.md`. It lands first, so the owner can mark while the rest is built. The class is a mechanical stdlib tool (Opus by the project's staffing).
- **Read exactly:**
  1. `tasks.md` § WO-009: the intro (seam notes 2 and 7); § Seams S-54 and S-56 (the bar's five items: `status` prints them in the same words); question 2; the TASK-063 row.
  2. `workorders/WO-009.md`: § Scope notes ("The owner's marks gate three lines"); question 2; the standing constraints.
  3. The REQ folder: `REQ-039.md` (rule 2), `REQ-042.md`, `REQ-045.md` (statement, rule 3), `REQ-041.md` (statement).
  4. Design inputs: **DI-8** (`Requirements/evidence/src-016-first-build-owner-decisions.md`: "A mark command (Recommended)"); **DI-5** (`Spec/03-puzzle-format.md` §2 and §5: the fields, the categories, "A human sets `reviewedByHuman: true`"; `Tangrams/puzzle.schema.json`, locked). `design-inputs.md` §2 item 10: the bar is REQ-042's 20; DI-8's "25" is neither a bar nor a cap (R1).
  5. `decisions.md`: R1, R2, R3; 2026-10-11 "**A missing `provenance` or `reviewedByHuman` reads as `false`.**"
  6. `Tangrams/README.md` (where the paragraph goes) and two shipped files of each `provenance` layout: `Tangrams/shapes-square.json` (multi-line) and `Tangrams/shapes-mini-1.json` (one line). `tools/tests/test_import_figures.py` (the style of a tool test run directly; its `test_layout_is_the_shipped_layout` must stay green after any mark).
  7. `C:/GitHub/AI/SWDev/adapters/claude/CLAUDE.md` § Windows (LF, `newline="\n"`, forward slashes).
- **Do not read:** the kit author's briefs; other implementers' briefs; DI-1 … DI-4, DI-10; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json` (read by `status`; the test copies files into a temporary folder).
- **Constraints:**
  - **Never run `mark` or `unmark` on `Tangrams/`**, and never write a file under it but `Tangrams/README.md` (owner rule 7, G-05). Your test works only in temporary folders; `status` on `Tangrams/` is read-only.
  - Python 3, stdlib only. Read and write bytes, so LF and the file's own final newline survive on Windows. No BOM.
  - The tool never re-formats a file; it never touches any key but the one boolean.
- **Run:** `python tools/tests/test_mark_puzzles.py`; `PYTHONUTF8=1 python tools/mark_puzzles.py status`; `git status --short Tangrams` before and after; `PYTHONUTF8=1 python tools/validate_puzzles.py Tangrams`.
- **Done means:** the TASK-063 row's Done-check.
- **Return:** the handoff block, with the `status` output on `Tangrams/`, the test's cases, and the README paragraph as written.

---

## Brief — TASK-065 · WO-009 · slice-implementer

- **Mission:** S-55 (the release set), S-56 (the gate on the store artifact) and S-58's build lines. The class is build logic under AGP 9.4's variant API, and this is the WO's product risk: the debug bundle every JVM and device kit reads must stay exactly as it is.
- **Read exactly:**
  1. `tasks.md` § WO-009: the intro (the expected red; seam notes 1, 2 and 7); § Seams S-55, S-56, S-58, S-54 (the copy you mark for the positive control); question 1; the TASK-065 row.
  2. `workorders/WO-009.md`: § Scope and its notes; questions 1 and 8; the standing constraints.
  3. The REQ folder: `REQ-039.md`, `REQ-042.md`, `REQ-045.md`, `REQ-041.md`, `REQ-007.md`, `REQ-047.md` (the statement: the language split), `REQ-048.md` (the statement; rule 8).
  4. `architecture.md` v1.5: §2 G-01, G-05, G-08, G-12; §3b O-02; §5 (the stack, the application id, the allowed dependencies).
  5. Design inputs: **DI-5** (`Spec/03-puzzle-format.md` §2), **DI-8** (Google Play).
  6. `decisions.md`: R1, R2, R4, R8; 2026-10-11 "**A missing `provenance` or `reviewedByHuman` reads as `false`.**" and "**A bundled puzzle that fails a rule fails fast.**"; 2026-10-18, the row whose title begins "The device's first locale" (S-36, Q7).
  7. Code: `content/build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`; `content/src/main/kotlin/app/tangramplaytime/content/BundledPuzzles.kt` (the resource folder, the one reader).
  8. The visible acceptance tests for your IDs (never edit them): `play/src/test/kotlin/app/tangramplaytime/play/ReleaseMarksKitTest.kt` (REQ-039 A2), `content/src/test/kotlin/app/tangramplaytime/content/ReleaseSetKitTest.kt` (REQ-042 A1–A3, the release-set pins), `app/src/test/kotlin/app/tangramplaytime/app/ReleasePromiseKitTest.kt`, `play/src/test/kotlin/app/tangramplaytime/play/ReleaseBuildKitTest.kt`; and, as the guard on the debug bundle, `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLibraryKitTest.kt` and `content/src/test/kotlin/app/tangramplaytime/content/ContentScaffoldTest.kt`.
  9. `platform-notes/android.md` § Verification channels and § Gradle from Git Bash on Windows.
- **Do not read:** the kit author's briefs; other implementers' briefs; DI-1 … DI-4, DI-9, DI-10; chat history; `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the row's Inputs cell.
- **Constraints:**
  - No new dependency (JSON through Gradle's own parser); no Kotlin source; no manifest (TASK-067's); no `Tangrams/` file; no kit or scaffolding test.
  - **No signing configuration of any kind** (hard-stop 1). If `bundleRelease` needs one, stop and `recirculate → planner`.
  - `assembleRelease` never waits on the gate (V-01 and three kits read its APK at every step).
  - The positive control runs on a copy of the repository in your scratch: copy the tree without its `build/` and `.gradle/` folders (no git command that writes; `local.properties` comes along), mark a release-ready set there with `tools/mark_puzzles.py --folder <copy>/Tangrams mark …`, and build there. Never `mark` on `Tangrams/`.
- **Run:**
  1. Run (1), then run (2) with `--continue`.
  2. List both APKs' puzzle folders (`app/tangramplaytime/content/puzzles/`).
  3. The gate on this tree (expected to refuse), then `:app:assembleRelease`.
  4. The positive control on the copy: the gate passes and `bundleRelease` builds; the AAB's listing; one `unmark` below the bar, and the gate refuses again.
  5. V-01 and its positive control.
- **Done means:** the TASK-065 row's Done-check.
- **Return:** the handoff block, with: the diff of both build files; the JVM counts per module and the expected reds with their messages; both APKs' listings; the gate's message on this tree; the copy's gate output and the AAB listing; the V-01 lines.

---

## Brief — TASK-067 · WO-009 · slice-implementer

- **Mission:** S-57 (the launcher icon and the store icon from DI-9, by one deterministic stdlib generator), S-58's manifest lines, the `ASSET-LICENCES.md` edits (the icon section; the convex-13 credit line, carry 1) and S-62 (the release install leg). Then the stage-2 classes and the release install leg on the images. The class is rendering, resources and device legs, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-009: the intro (windows; seam notes 3, 5 and 7; the in-tree tests the WO touches); § Seams S-57, S-58, S-62; questions 4, 5, 8 and 9; the TASK-067 row; the briefs' channel table and "What runs where" above.
  2. `workorders/WO-009.md`: § Scope notes ("Carried into this WO" 1, 5, 6; "Hard-stop 1 is not reached by the build"); questions 4, 5, 8 and 9.
  3. The REQ folder: `REQ-048.md` (rule 2, A3, A4), `REQ-010.md` (A2), `REQ-035.md` and `REQ-036.md` (the statements: why the portrait feature is not required), `REQ-002.md` (A2: the class you run).
  4. Design inputs: **DI-9** (`Spec/icon/option-A-foreground.svg`, `Spec/icon/option-A-background.txt`, `Spec/icon/README.txt`: used as is, never redrawn); **DI-8** (the owner picked option A). `design-inputs.md` §2 item 7 and "Also fine" (DI-9).
  5. `architecture.md` v1.5 §2: G-01, G-06, G-16; §5.
  6. `decisions.md`: R6, R7, R8, R13; 2026-10-18 "**A phone is portrait from the manifest; a tablet is unlocked at run time**".
  7. Files you change: `app/src/main/AndroidManifest.xml`; `ASSET-LICENCES.md` (keep the headings `## Pictures, fonts, sounds` and `## Sounds`; your section comes after `## Sounds`). The convex-13 credit sentence: `C:/GitHub/AI/TangramCollections/collections/convex-13/NOTICE.md` (outside the project, read-only), and `reviews/WO-010-code-review.md` N2.
  8. The visible acceptance tests for your IDs (never edit them): `app/src/androidTest/kotlin/app/tangramplaytime/app/LauncherIconKitTest.kt` (REQ-048 A4), `play/src/androidTest/kotlin/app/tangramplaytime/play/NoToddlerModeKitTest.kt` and `app/src/androidTest/kotlin/app/tangramplaytime/app/SettingsKitTest.kt` (REQ-002 A2), `app/src/test/kotlin/app/tangramplaytime/app/ReleasePromiseKitTest.kt` and `app/src/androidTest/kotlin/app/tangramplaytime/app/PlayThroughKitTest.kt` (REQ-048 A3), `content/src/test/kotlin/app/tangramplaytime/content/AssetLicencesKitTest.kt` and `app/src/test/kotlin/app/tangramplaytime/app/SoundAssetsKitTest.kt` (the record).
  9. `tools/make_sounds.py` (the precedent: a deterministic stdlib generator writing project files); `platform-notes/android.md` (all of it; § Device hygiene: the throwaway-signed copy); `C:/GitHub/AI/SWDev/adapters/claude/CLAUDE.md` § Windows.
- **Do not read:** the kit author's briefs; other implementers' briefs; DI-1 … DI-4, DI-10; chat history; `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the row's Inputs cell.
- **Constraints:**
  - The tool converts DI-9 path for path; it never redraws or "improves" it. Two runs write the same bytes (G-06). Stdlib only; XML and PNG written with explicit bytes (LF, no BOM).
  - No Kotlin source, no string resource, no kit file, no dependency, no `roundIcon`, no monochrome layer, no PNG mipmap.
  - S-62 signs only a copy in your scratch with the local debug key store; it refuses a scratch dir inside the project. Nothing signed lands in the project. No other key, ever (hard-stop 1).
  - The release APK is not launched in this task: its bundle is empty until the owner's marks (S-55).
- **Run:**
  1. **Before you change anything:** on the floor, `LauncherIconKitTest` alone (red expected: the default icon). Record the XML.
  2. The tool twice and `cmp`; the tool test.
  3. Run (1), then run (2) with `--continue`; kit compile; V-01 and its positive control.
  4. The device steps of "What runs where" (TASK-067), floor first, `NoToddlerModeKitTest` timed on the floor before dev.
  5. Open `release/pictures/icon-512.png` with the Read tool and write one line about it.
- **Done means:** the TASK-067 row's Done-check.
- **Return:** the handoff block, with: the files written; the `cmp` result; the before and after XMLs per image with per-class times; S-62's lines per image; the V-01 lines; the icon look; the `ASSET-LICENCES.md` diff.

---

## Brief — TASK-068 · WO-009 · slice-implementer

- **Mission:** S-59 (the store kit and the root `PRIVACY.md`, with the owner's release checklist) and S-60 (the banner tool and its two files), plus one `review-notes.md` row. The class is mechanical text and one picture tool (Opus by the project's staffing). You perform none of the checklist's steps.
- **Read exactly:**
  1. `tasks.md` § WO-009: seam notes 4 and 7; § Seams S-59, S-60 and S-57 (the icon you reuse); questions 3, 7 and 10; the TASK-068 row.
  2. `workorders/WO-009.md`: § Goal; § Scope notes ("Owner evidence", "Pending drafts", "Hard-stop 1 is not reached by the build"); questions 3, 7 and 10.
  3. The REQ folder: `REQ-048.md` (all of it, the ASSUMPTIONs included), `REQ-049.md`, `REQ-001.md`, `REQ-009.md` (the free note's words), `REQ-010.md`, `REQ-002.md` (the statement and rules: who the game is for), `REQ-006.md` (phone and tablet), `REQ-047.md` (en and fi).
  4. Pending drafts (not Contract yet; follow the locked text): `capture-outbox/chg-draft-req048-store-channel.md` (the closed-test step), `capture-outbox/chg-draft-req048-console-declarations-acceptance.md`, `capture-outbox/chg-draft-req047-english-contract.md`, `capture-outbox/chg-draft-req009-req049-finnish-wording.md`, `capture-outbox/chg-draft-licence-notice-and-assets.md`.
  5. Design inputs: **DI-8** (`Requirements/evidence/src-016-first-build-owner-decisions.md`: Google Play; the title "Tangram Playtime"); **DI-9** (`Spec/icon/`: the banner's pieces). `design-inputs.md` §2 items 7 and 9.
  6. `decisions.md`: the scope row; R9, R10, R11; the WO-009 checkpoint items (above).
  7. `owner-evidence.md` (read only: the three rows the checklist's last step names); `app/src/main/res/values/strings.xml` and `app/src/main/res/values-fi/strings.xml` (`app_name`, `free_note`, `privacy_text`, `licence_line`: read only); `review-notes.md` (its row format); `release/pictures/icon-512.png` (TASK-067's; look at it).
  8. The pins that will judge your files at TASK-069's landing are S-59's "mechanical self-check" list: meet each item.
- **Do not read:** the kit author's briefs; other implementers' briefs; DI-1 … DI-4, DI-10; chat history; `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the row's Inputs cell.
- **Constraints:**
  - English is the Contract wording; Finnish is a reference translation, listed in `review-notes.md` (G-10). No promise, price or "free" word in a title or in the banner (CHG-001).
  - The full description makes no claim the build does not make, and names no puzzle count the release might not ship.
  - No personal e-mail, no key, no key store, no signing config and no account detail in the tree (hard-stop 1). The privacy page's contact is the Play listing's developer e-mail and the repository's issues (S-59).
  - The checklist says what the owner does, where, and from which file; it never says the build did a step. Name console pages as the console names them, and where unsure, say "as the console names it".
  - The banner: Chrome headless only through `CHROME`, bounded by `timeout 60`; `tools/svg2png.py` and Playwright are not used.
- **Run:** the banner tool twice (`cmp` the SVG); open the PNG and the icon with the Read tool; run (1), then run (2) with `--continue`; the search for signing material.
- **Done means:** the TASK-068 row's Done-check.
- **Return:** the handoff block, with the list of files written, the character counts of each listing field (en and fi), the banner look, and the `review-notes.md` row.

---

## Brief — TASK-070 · WO-009 · slice-implementer

- **Mission:** Every channel once, on the final, reviewed tree, before the owner's marks. Device-only; you make no product, kit, data or tool change of your own. A red test is routed, not fixed.
- **Read exactly:**
  1. `tasks.md` § WO-009: the intro (the expected red; seam note 2; the verification channels); the coverage table; the TASK-070 row; the briefs' channel table, "What runs where", "Expected skips" and the device-time rules above.
  2. `workorders/WO-009.md` § Constraints & notes (the verification channels; device time).
  3. `platform-notes/android.md` (all of it); `.swdev/run-log/WO-009.md` (Env, the tree definitions as extended, the g3 rows).
  4. The tests named in your row's Done-check: read their headers for what each proves and its expected counts.
- **Do not read:** the kit author's briefs; other implementers' briefs; chat history; `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the row's Inputs cell.
- **Run:** the row's (a) to (f), in that order: Python; JVM (run (2) with `--continue`); the gate; then the floor script, the dev script, the tablet script, one after another; S-62 inside the floor script; V-01 and `run_verifiers.py --wo WO-009`. A failing class re-runs alone; then the whole sets run once more on the final tree.
- **Done means:** the TASK-070 row's Done-check. If the owner's marks are already release-ready, the REQ-042 legs are green and the gate builds instead; say so.
- **Return:** the handoff block, with each channel's counts, the expected reds and their messages, per-image XMLs copied to your scratch with each run's tree line, per-class times, and the routing of any red.

---

## Brief — TASK-071 · WO-009 · slice-implementer

- **Mission:** After the owner's marks: prove the release set, build the store artifact, launch the release APK, and make the store screenshots (S-61). Device-only; no product, kit or puzzle change. Dispatched when `python tools/mark_puzzles.py status` prints `release-ready: yes`.
- **Read exactly:**
  1. `tasks.md` § WO-009: seam notes 1, 2 and 7; § Seams S-54 (`status --list`), S-55, S-56, S-61, S-62; questions 1 and 7; the TASK-071 row; the briefs' channel table and "What runs where" above.
  2. The REQ folder: `REQ-042.md`, `REQ-039.md`, `REQ-045.md`, `REQ-041.md`, `REQ-040.md`, `REQ-048.md` (the statement, A3).
  3. `decisions.md`: R1, R2, R11, R12.
  4. `platform-notes/android.md` (all of it); `.swdev/run-log/WO-009.md` (TASK-070's recorded tree).
  5. The visible tests for your IDs: `content/src/test/kotlin/app/tangramplaytime/content/ReleaseSetKitTest.kt`, `play/src/test/kotlin/app/tangramplaytime/play/ReleaseMarksKitTest.kt`.
  6. `tools/release_install.sh` (S-62; TASK-067's); `tools/device_reset.sh`; `review-notes.md` (its row format).
- **Do not read:** the kit author's briefs; other implementers' briefs; chat history; `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the row's Inputs cell.
- **Constraints:**
  - **First, the marks' diff** (R12): against TASK-070's recorded tree, the only changed lines in the puzzle files are `"reviewedByHuman": false` → `true`. Anything else: stop and report; the device sets then re-run.
  - Never run `mark` or `unmark`. Never edit a puzzle file.
  - S-61 drives the **throwaway-signed release copy**, never the debug APK. Tap positions are read on the screen in use, never typed from a brief. The phone screenshots are taken at `wm size 1080x1920`, and the display is restored after.
  - No signing beyond S-62's debug-key copy in your scratch.
- **Run:**
  1. The marks' diff.
  2. Python channel; run (1), then run (2) **without** `--continue` (the expected red is gone); V-01 and its control.
  3. The gate: `:app:bundleRelease` builds; list the AAB's puzzle folder against `status --list`.
  4. The floor script: S-62 with `--launch`; one screen capture, opened with the Read tool.
  5. Write `tools/store_screenshots.sh` (S-61). The dev script: the phone screenshots. The tablet script: the tablet screenshots. Open each PNG with the Read tool and write one line about it.
  6. One `review-notes.md` row: the screenshots and the release smoke, to look at.
- **Done means:** the TASK-071 row's Done-check.
- **Return:** the handoff block, with the diff summary, the JVM counts, the gate's lines and the AAB listing, S-62's lines, a line per screenshot, and the `review-notes.md` row.
