# WO-006 — dispatch briefs (#Layout + #Language + #Promise)

**Planner:** wo-planner (Opus 5.5)  ·  **Date:** 2026-10-17  ·  **Plan:** `tasks.md` § WO-006 (TASK-027 … TASK-032; TASK-027 ∥ TASK-028 are `[P]`)
**Mode:** Light (`workorders/WO-006.md` § Mode). There is no design. § WO-006 › Seams in `tasks.md` is the design: the seam table S-31 … S-37, the touch geometry per layout class, the player controls per screen and "What survives a rotation".

> Each brief is dispatched on its own. An Acceptance Test Author gets only its own brief plus the three tables below; it never sees an implementer brief. An implementer never sees another implementer's brief or a kit draft outside the tree.
> Every path is relative to `C:/GitHub/AI/TangramPlaytime/` unless it is absolute. Folder names used below:
> - "the JVM kit folder": `play/src/test/kotlin/app/tangramplaytime/play/`;
> - "the kernel kit folder": `kernel/src/test/kotlin/app/tangramplaytime/kernel/`;
> - "the app JVM folder": `app/src/test/kotlin/app/tangramplaytime/app/` (new in this WO);
> - "the play device kit folder": `play/src/androidTest/kotlin/app/tangramplaytime/play/`;
> - "the app device kit folder": `app/src/androidTest/kotlin/app/tangramplaytime/app/`;
> - "the aid folder": `play/src/debug/kotlin/app/tangramplaytime/play/devaid/`.
>
> `decisions.md` rows are cited by date and bold title; rows are only ever appended. Already in force: 2026-10-17 "**Scope of WO-006, and where the free note and the store-labels line land**" and "**REQ-047.A1 carries `implemented (unverified)`**". **The WO-006 rows** are the ones the orchestrator appends before dispatch for the planner's questions; these briefs cite them by the titles the planner proposed:
> - Q1 "**TYPE-007 reads the window's upright width, once in `:kernel`**";
> - Q2 "**The tablet layout, as drawn**";
> - Q3 "**Edge to edge, the insets and the keyboard, as drawn**";
> - Q4 "**Back closes the grid; no other surface is an open overlay**";
> - Q5 "**REQ-037's oracle: the router's own areas on the device**";
> - Q6 "**The screen-reader-label carry is dropped**";
> - Q7 "**One language rule for strings and titles**";
> - Q8 "**The promise lines' oracles**";
> - Q9 "**What 'fully playable' means, and the exact sizes**".
>
> If the orchestrator changes a title or rules a question the other way, it corrects the citations and the affected brief lines in the same edit.

## Dispatch order (three lanes)

| Step | Test lane (acceptance-test-author) | Hygiene and implementer lane (slice-implementer) | Gate to the next step |
|---|---|---|---|
| 0 | — | — (orchestrator) Append the WO-006 rows Q1–Q9. Extend the run log's tree definitions (`tasks.md` § WO-006 seam note 6). Ask the checkpoint items. | The rows exist |
| 1 | TASK-027 drafts and **lands** the JVM kit. TASK-030 starts drafting the device kit in its scratchpad. | TASK-028 (the reset script), in parallel: adb and the existing debug APK only, no Gradle | TASK-027 met at landing (`ReleasePromiseKitTest` green); TASK-028's Done-check green |
| 2 | TASK-030 keeps drafting. | TASK-029, reading the landed JVM kit | TASK-029's Done-check green. This is the JVM kit's green verdict, and the play device set is unchanged on the dev image |
| 3 | TASK-030 **lands** (resume the same author context with "TASK-029 done"). | — | TASK-030's Done-check met at landing |
| 4 | — | TASK-031 | The layout, touch and play-through classes green on the dev, tablet and floor images; the old sets stay green |
| 5 | — | TASK-032 | `LanguageKitTest` green; every set on its images |
| 6 | Run `plan_check.py --wo WO-006` again now that the kit has compiled (WO DoD) → Code Reviewer → Test & Verify (every set, every channel) | | |

- **Why this order.**
  - TASK-029 goes first among the implementers because it changes the geometry under every screen and must leave an inset-free screen exactly as WO-005 left it (seam note 1). The JVM kit pins that geometry before anyone writes it.
  - TASK-030 calls no new `:app` seam (seam note 2), so it compiles whenever it lands. It lands after TASK-029 only so that two Gradle runs never overlap in one working tree, and so that a seam TASK-029 had to recirculate is settled first.
  - TASK-031 then has the visible device tests for its IDs before it starts; TASK-032 has `LanguageKitTest`'s `[sv, fi]` leg red until it lands.
- **Red windows** (`tasks.md` § WO-006 intro). Nobody runs a set for a verdict inside its window: `kernel/src/test` and `play/src/test` from step 1 until TASK-029 lands. During that window the JVM line's run 2 is `:app:testDebugUnitTest --rerun` alone.
- **Compile errors in the kit.** An unresolved reference to a seam of a task that has not landed yet is expected; each kit row lists which. Any other compile error in a kit file is a test defect. It goes back to that kit's author and counts as a corrected check. An implementer never edits a kit file.
- **In-tree tests this WO changes:** none. The scaffolding tests belong to the implementers.
- **Decisions the implementers record.** A number or reading that no REQ, seam row or `decisions.md` row fixes goes back as a proposed `decisions.md` row, with its source and the alternative. The orchestrator appends it, so each is recorded once.
- **Checkpoint items** (`workorders/WO-006.md` § Checkpoint items, once asked). The build follows the defaults. A kit pin on one cites the WO-006 row, so an owner answer changes a constant and the pin that cites it.

## Verification channels (G3)

Each "the line" below is `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat <tasks>"`. `ADB` is `/c/Users/Jami/AppData/Local/Android/Sdk/platform-tools/adb`; `EMU` is `/c/Users/Jami/AppData/Local/Android/Sdk/emulator/emulator`.

| Channel | How (Git Bash) | Result | Needed by |
|---|---|---|---|
| JVM (JUnit 4) | **Two runs of the line, in this order:** **(1)** `:app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun`, **then (2)** `:play:testDebugUnitTest --rerun :contracts:test --rerun :kernel:test --rerun :app:testDebugUnitTest --rerun`. Run (2) reads the APKs run (1) wrote (`ReleaseBuildKitTest`, `ReleasePromiseKitTest`); a bare run (2) without run (1) first is not a verdict (`platform-notes/android.md`, the WO-005 lesson). Between TASK-027's landing and TASK-029, run (2) is `:app:testDebugUnitTest --rerun` alone. At close add `:store:testDebugUnitTest :content:testDebugUnitTest`. | `play/build/test-results/testDebugUnitTest/TEST-*.xml`, `kernel/build/test-results/test/TEST-*.xml`, `app/build/test-results/testDebugUnitTest/TEST-*.xml`, `contracts/build/test-results/test/TEST-*.xml` | TASK-027, TASK-029, TASK-031, TASK-032; Test & Verify |
| Kit compile | The line with `:app:assembleDebugAndroidTest :play:assembleDebugAndroidTest` | the exit code and the compiler messages | TASK-030, TASK-031 |
| Dev image `Medium_Phone_API_37.0` (1080 × 2400 px at 420 dpi = 411 × 914 dp; gesture navigation), running as `emulator-5554` | `$ADB devices`, then record `$ADB shell settings get secure navigation_mode` (must read `2`; REQ-035.A2's evidence). The line with `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.app.PhoneLayoutKitTest`, **one class per invocation** (through `cmd.exe //c`, a comma list runs only its first class); omit the argument for a whole set; `:play:connectedDebugAndroidTest` the same way. **The whole app set is two invocations on every image** (orchestrator, `decisions.md` 2026-10-17 "The device kit's open readings …", ruling 2): first `…class=app.tangramplaytime.app.DevPromptKeyboardKitTest` alone (the DEV aid still locked in a fresh process), then the rest with `-Pandroid.testInstrumentationRunnerArguments.notClass=app.tangramplaytime.app.DevPromptKeyboardKitTest` (the kit author proves `notClass` passes through `cmd.exe //c` at landing; if not, one class per invocation). Copy each XML you cite to your scratchpad (the folder is overwritten). Judge a run by its XML — it exists, is stamped after the command started and carries the expected counts — never by Gradle's exit code. | `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`, `play/build/outputs/androidTest-results/connected/debug/TEST-*.xml` | TASK-029 (play set), TASK-031, TASK-032; Test & Verify |
| Floor image `Phone_API_26` (`minSdk 26`; 411 × 914 dp; three-button navigation), one emulator at a time | `$ADB emu kill`, then `$ADB devices` until it is empty. `$EMU -avd Phone_API_26 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot` in the background, `$ADB wait-for-device`, poll `$ADB shell getprop sys.boot_completed` until `1`, `$ADB devices`. The same lines. **Then the floor's extra steps** (`tasks.md` § WO-006 seam note 5; the routes as measured, `decisions.md` 2026-10-17 "The floor routes, as measured — amends the Q7 and Q8 rows"; the reset script prints the locale route). Each route runs **before** its Gradle invocation (the locale route's framework restart kills running apps) and is read back (`am get-config` for a locale; the setting and the radios for airplane mode), with `bash tools/device_reset.sh` and its `--check` around each step: (a) the root locale route with `fi-FI`, then `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.app.LanguageKitTest -Pandroid.testInstrumentationRunnerArguments.kitDeviceLocales=fi-FI`; (b) the same with `sv-SE`; (c) the same with the list `sv-SE,fi-FI` — amended by the orchestrator, `decisions.md` 2026-10-18 "**A comma never reaches an instrumentation argument …**": plus the direct `am instrument` run that row names; (d) the root locale route with `en-US` (the restore); (e) the root airplane route on, then `PlayThroughKitTest` alone with `-Pandroid.testInstrumentationRunnerArguments.kitAirplaneByChannel=true`, then the route off and `$ADB unroot`. | the same XML folders (the file name carries the device); copy each extra step's XML under a name that says its locale or airplane mode | TASK-028 (the probes), TASK-031, TASK-032; Test & Verify |
| Tablet image `Medium_Tablet_API_37.0` (2560 × 1600 px at 320 dpi = 1280 × 800 dp, landscape) | As the floor, with `-avd Medium_Tablet_API_37.0` (its first boot with `-wipe-data` is done, run log t37). Record `navigation_mode`. The app set only (the play set hosts `PlayScreen` with the kit's phone literal; nothing the tablet changes is under test there). Afterwards boot `Medium_Phone_API_37.0` again and leave it running. | the app XML folder | TASK-028, TASK-031, TASK-032; Test & Verify |
| Release verifier V-01 (G-01, G-08) | After run (1) of the JVM line: `python .swdev/verifiers/v01_release_apk.py`, `python .swdev/verifiers/v01_release_apk.py --positive-control`, `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-006` | one PASS/FAIL line each, then GREEN/RED and the budget line (expected: 0 verifiers, 0 lines for WO-006) | TASK-031, TASK-032, the close |
| Device hygiene | `bash tools/device_reset.sh`, then `bash tools/device_reset.sh --check` (clean), **before and after** every device step; from TASK-028 on, `--check` also names any device-level leftover (S-37). A non-clean check after a reset voids the step (`platform-notes/android.md` § Device hygiene). | `--check` reports clean or not | every device step |

**What the app set does on each image** (`tasks.md` § WO-006 seam notes 4–5). "Override" is the kit's `wm size` + `wm density`, read back from the launched activity and restored in `finally`.

| Class | Dev image (API 37, phone, gesture bar) | Floor image (API 26, phone, button bar) | Tablet image (API 37, 1280 × 800 dp) |
|---|---|---|---|
| `PhoneLayoutKitTest` | its own size and 390 × 844 dp (override 1170 × 2532 px at 480 dpi); REQ-035.A2 against the gesture bar | the same; REQ-035.A2's assertion against the button bar | 390 × 844 dp only (its own size is a tablet) |
| `TabletLayoutKitTest` | override 2560 × 1600 px at 320 dpi; landscape, then portrait by a frozen rotation | the same override; landscape and portrait | its own size; landscape and portrait |
| `TouchTargetKitTest` | phone 360 × 780 dp (override 1080 × 2340 px at 480 dpi); tablet 1280 × 800 and 800 × 1280 dp (override) | the same | phone by override; tablet at its own size, both orientations |
| `LanguageKitTest` | every leg (the kit sets the locales with `cmd locale`) | the own-locale leg in the ordinary run (`en-US`) and in the floor steps (a)–(c) (`kitDeviceLocales` = `fi-FI`, `sv-SE`, `sv-SE,fi-FI`); the `cmd locale` legs filtered (`@SdkSuppress(minSdkVersion = 33)`), the list leg's case covered by step (c) — no skip | every leg |
| `PlayThroughKitTest` | phone 360 × 780 dp; tablet 1280 × 800 and 800 × 1280 dp; airplane mode on (the kit switches it) | the same; in the ordinary run without airplane mode (not a skip: the play-through still runs and asserts), and in floor step (e) with airplane mode on through the channel's root route and `kitAirplaneByChannel=true` — REQ-010.A1's floor evidence | the same as the dev image |
| WO-003…005 classes | as before | as before | as before |

The named guarded skips are listed once, in `workorders/WO-006.md` § Checkpoint items, last paragraph — now only the two `ProcessDeathKitTest` phases. A filtered `@SdkSuppress` leg is absent from the XML, not a skip. Any other skip in an XML is a failure.

- **Not a DoD channel:** human eyes. The owner plays at close from `review-notes.md` (the edge-to-edge look, a tablet in both orientations, turning it mid-puzzle, the device in Finnish).
- **Not a channel of this WO:** the process-death pair (no store path changes). The app set's two guarded `ProcessDeathKitTest` skips run as before.
- **Toolchain proof:** JVM (g3), tablet (t37), floor (f26) and dev (d37) in `.swdev/run-log/WO-006.md`.
- **Time:** WO-006 computes no time value (G-14). Nothing sleeps: wait with `composeRule.waitUntil` or on an observed state.
- **Browser:** none.

---

## Brief — TASK-027 · WO-006 · acceptance-test-author

- **Mission:** Write the JVM part of WO-006's one acceptance kit, against `tasks.md` § WO-006 › Seams, and land it before TASK-029 starts:
  - `kernel/src/test/kotlin/app/tangramplaytime/kernel/LayoutClassKitTest.kt`: S-34's pins of TYPE-007 (no token);
  - `play/src/test/kotlin/app/tangramplaytime/play/SafeAreaKitTest.kt`: S-31 … S-33's pins over `ScreenGeometry.of` (no token);
  - `app/src/test/kotlin/app/tangramplaytime/app/ReleasePromiseKitTest.kt`: REQ-010.A2 on the release APK, and the G-01 allow-list pin of Q8.

  The class is JVM oracles over a geometry seam and a scan of the release APK, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-006: the intro (lanes, red windows, seam notes 1, 5 and 6, "Verification channels", "Not covered here"); § Seams S-31, S-32, S-33, S-34; the touch geometry per layout class; the rows TASK-027 (yours) and TASK-029 (its Done-check is your green verdict).
  2. The geometry your pins drive: `tasks.md` § WO-003 › Seams S-5, S-6 and S-10; `designs/WO-004-design.md` §2, S-22's touch geometry and the overview; § WO-005 › Seams S-25 and the touch geometry's "The sheet".
  3. `workorders/WO-006.md`: § Scope with every scope note; § Mode; § Constraints & notes, questions 1, 2, 3, 6 and 8, and the standing constraints.
  4. Requirements, collection v1.2, read-only:
     - your token: `Requirements/reqs/REQ-010.md`;
     - the sources of your pins, no token: `REQ-035.md`, `REQ-036.md`, `REQ-037.md`, `REQ-013.md` (consumed by S-32), `REQ-008.md` and `REQ-001.md` (the rule the allow-list pins);
     - `Requirements/req_types.md`: TYPE-001 (tray order), TYPE-007.
  5. `architecture.md` §1 (the D1 note on TYPE rules), §2 G-01, G-12, G-17, §3 (component map, O-05), §5 (the allowed dependencies the allow-list comes from).
  6. `decisions.md`: the WO-006 rows Q1, Q2, Q3 and Q8; 2026-10-11 "**Numbers the tray and the gestures take from the design inputs**" (the phone literal's numbers); 2026-10-17 "**REQ-046.A4's oracle is the release APK**" (the pattern of a test that reads build outputs).
  7. In-tree, read-only:
     - `play/src/test/kotlin/app/tangramplaytime/play/KitPlay.kt`: reuse `KIT_PHONE_LAYOUT` and the bundled-library helpers; you may add a kit-prefixed helper, never change one;
     - `play/src/test/kotlin/app/tangramplaytime/play/ReleaseBuildKitTest.kt`: the pattern for reading `classes*.dex`, the staleness refusal and the project-root lookup. Not yours to edit;
     - `.swdev/verifiers/v01_release_apk.py`: the precedent for the manifest check (`aapt2 dump permissions`, the SDK path from `ANDROID_HOME` or `local.properties`) and its liveness rule;
     - **declarations only, never bodies:** `play/src/main/kotlin/app/tangramplaytime/play/PlayScreen.kt` (`ScreenGeometry.of(widthPx, heightPx, density, layout, puzzle)` and its fields `topBar`, `navPrev`, `navNext`, `counter`, `board`, `mapping`, `tray`, `trayPxPerUnit`, `cells`, `solvedThirds`, `density`) and `play/src/main/kotlin/app/tangramplaytime/play/PlayDrawing.kt` (`Slot(left, top, width, height)`, px). S-32 adds `safe`, `overlay`, `overlayContent` and `sheetContent`.
  8. Build files: `app/build.gradle.kts` (`testImplementation(libs.junit)` exists; `app/src/test` is new), `kernel/build.gradle.kts`, `play/build.gradle.kts`.
  9. Rules: `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2; `C:/GitHub/AI/SWDev/framework/methodology/task-decomposition.md` ("Assert only what is mandated", "Seams name their task; adapters fail loudly", "Declared inputs land together"); `platform-notes/android.md`, all of it.
- **Do not read:**
  - the bodies of the main code of `:play`, `:app`, `:kernel`, `:content`, `:store` and of `play/src/debug` (compiler messages are fine);
  - the implementer briefs (§ TASK-028, § TASK-029, § TASK-031, § TASK-032 of this file) and TASK-030's drafts;
  - DI-1 … DI-4 (they supply no Contract text);
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-027 row's Inputs cell (the release APK and the sources its staleness check reads).
- **Oracle rule:**
  - Every assertion cites, in a comment, the sentence it checks: a REQ sentence, a TYPE rule, a seam row (`S-n`), an architecture line or a `decisions.md` row by date and title. No source, no assertion.
  - A REQ token goes only on a test whose assertion is that line's meaning; a pin carries its source and no token. Cite rules without a dotted ID in prose ("REQ-008 rule 1"): trace-check counts every dotted ID it finds.
  - Fixtures discriminate: a pin that tells "inside the safe area" from "not" uses insets large enough that the WO-005 geometry would fail it (show that test-side).
  - No `assume…`. A leg that cannot run fails with a message saying why. Call `internal` members directly, no reflection.
- **Where the tokens go:** `ReleasePromiseKitTest.kt`: REQ-010.A2, on the manifest assertion. Nothing else in the three files carries a dotted ID.
- **What each file needs** (pointers, not restatements; the REQ and the seam row are the oracle):
  - **`LayoutClassKitTest.kt`** (S-34; TYPE-007): the boundary (599 → PHONE, 600 → TABLET, from TYPE-007's two sentences); `TABLET_MIN_WIDTH_DP` is 600; the upright widths of this WO's channels (phones 320, 360, 390, 411; the tablet 800) land in the class Q1's row says.
  - **`SafeAreaKitTest.kt`** (S-31 … S-33):
    - Literals: the S-5 phone value (`KIT_PHONE_LAYOUT`) and S-35's tablet value written as a literal (as the kits write the phone's), each with non-zero `insets` (a portrait phone; a landscape tablet with a left or right inset too) and a `keyboardDp`.
    - Sizes in px with densities: the phone at 390 × 844 dp (1170 × 2532 px, 3.0); the tablet at 1280 × 800 dp (2560 × 1600 px, 2.0) and 800 × 1280 dp. Puzzles from the bundle through `KitPlay.kt`: a full puzzle (seven pieces, the parallelogram) and a mini.
    - Pins, each citing its S-row sentence: every control slot (`navPrev`, `navNext`, `counter`, each cell, each third) lies inside `safe`, and the bar starts at the safe top; the `tray` strip reaches the composable's bottom edge while every cell ends at or above the safe bottom; the phone literal gives REQ-035 rule 1's two rows and the tablet literal one row (one horizontal line crosses all seven cells, left to right in TYPE-001 order) in both orientations; `overlay`, `overlayContent` and `sheetContent` as S-32 defines them; with a keyboard of 0 and of 300 dp every other slot and the mapping are equal (S-32 "the keyboard moves nothing else"); with `PlayInsets.NONE` and no keyboard, `overlayContent` and `sheetContent` equal `overlay` (S-33).
  - **`ReleasePromiseKitTest.kt`** (Q8):
    - **REQ-010.A2:** the release APK's merged manifest holds no `uses-permission` and no `uses-permission-sdk-23` element — read as binary XML (the start-element chunks resolved through the string pool) or through `aapt2 dump permissions` (V-01's route). Liveness: the scan reads the package `app.tangramplaytime` and at least one `activity` element, or it fails.
    - **Pin, no token** (G-01; REQ-008 rule 1; architecture §5; Q8's row): every class definition of the release `classes*.dex` lies under one of Q8's allowed top-level packages; at least one class under `app/tangramplaytime/` is read. **Self-test:** the same check with `kotlinx/` removed from the list must fail on the same APK, in the same test, so a check that cannot fail cannot pass. The release APK only: the debug APK carries debug-only classes outside the list and is not shipped.
    - The test builds nothing. It refuses an APK older than the newest file of its staleness sources (the row's Inputs) and fails on a missing APK with the command that builds it. The project root is the nearest ancestor of the working directory that holds `settings.gradle.kts`. Every check is printed before the test fails, so one run shows every leg.
- **Run:**
  1. Write the three files. Landing is your Done-check, met at landing.
  2. Run (1) of the JVM line, then run (2) as `:app:testDebugUnitTest --rerun` alone: `ReleasePromiseKitTest` must be green (REQ-010.A2 has held since P2). Then compile the other two sets with `:kernel:test :play:testDebugUnitTest`: expected, and not your defect, are unresolved references to S-34 (`LayoutClass`) and to S-31 / S-32's new names (`PlayInsets`, `insets`, `keyboardDp`, `safe`, `overlay`, `overlayContent`, `sheetContent`). Any other compile error is yours.
  3. Grep the three folders for `REQ-0[0-9]*\.A[0-9]` before you hand back: exactly the row's list, the earlier tokens unchanged.
- **Done means:** the TASK-027 row's Done-check, plus hygiene: helper names carry a `kit` prefix; JUnit 4 only; no new dependency; never edit main code, a build file or a scaffolding test.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with: the test names per acceptance ID and the pins with their sources; the files outside each module the tests read at run time; the compile status (command, exit code, the expected unresolved names); the measured allow-list (any top-level package of the release dex Q8's row does not list is a finding, not an edit); the corrected checks so far.

---

## Brief — TASK-030 · WO-006 · acceptance-test-author

- **Mission:** Write the device part of WO-006's one acceptance kit, all in the app device kit folder, as a black box over the real app (seam note 2):
  - `KitApp.kt`: kit helpers, no token;
  - `PhoneLayoutKitTest.kt`: REQ-035.A1, REQ-035.A2 and the REQ-035 pins;
  - `TabletLayoutKitTest.kt`: REQ-036.A1, REQ-036.A2 and "What survives a rotation";
  - `TouchTargetKitTest.kt`: REQ-037.A1 on each layout class;
  - `LanguageKitTest.kt`: REQ-047.A1 (the mechanism, unverified) and REQ-047.A2;
  - `PlayThroughKitTest.kt`: REQ-006.A1, REQ-001.A1, REQ-008.A1, REQ-008.A2, REQ-010.A1 and the installed app's REQ-010.A2.

  Draft beside TASK-027 … TASK-029; land in one stage once TASK-029 is green. The class is a Compose UI kit with device-only legs (display overrides, rotation, locales, airplane mode, insets, the keyboard), so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-006, all of it: the intro with seam notes 1–6; § Seams S-31 … S-37; the touch geometry per layout class; the player controls per screen; "What survives a rotation"; the rows TASK-030 (yours), TASK-031 and TASK-032 (their Done-checks are your green verdicts).
  2. The tags, keys and doors you read: § WO-003's tag table and S-13, S-14; `designs/WO-004-design.md` §2, rows S-13Δ, S-14Δ, S-15Δ (`AppGraph.controller`, the kits' one door), S-22 (touch geometry, hit order, the overview) and "What survives"; § WO-005's S-28 and the aid's tag table, the touch geometry and "What survives".
  3. `workorders/WO-006.md`: § Scope with every scope note; § Mode; § Constraints & notes (the eight questions, the standing constraints, the G3 channels); § Checkpoint items, its last paragraph (the named guarded skips).
  4. Requirements, collection v1.2, read-only:
     - your tokens: `REQ-006.md`, `REQ-035.md`, `REQ-036.md`, `REQ-037.md`, `REQ-047.md`, `REQ-001.md`, `REQ-008.md`, `REQ-010.md`;
     - rules your legs cross, no token: `REQ-012.md`, `REQ-013.md`, `REQ-015.md`, `REQ-018.md`, `REQ-024.md`, `REQ-025.md`, `REQ-026.md`, `REQ-050.md`, `REQ-046.md`; the moves of the play-through `REQ-014.md`, `REQ-016.md`, `REQ-017.md`, `REQ-019.md`, `REQ-020.md`, `REQ-021.md`, `REQ-022.md`; `REQ-023.md` (what a solve shows);
     - `Requirements/req_types.md`: TYPE-001, TYPE-006, TYPE-007.
  5. Pending drafts: `capture-outbox/chg-draft-req047-english-contract.md` (why A1 is compared with the `fi` resource and the file's `title.fi`, never a typed word); `capture-outbox/chg-draft-req006-a1-playable-oracle.md` (the play-through's sentence, Q9); `capture-outbox/chg-draft-req006-platform-floor.md` (context only: 320 dp is not a line here).
  6. Frozen contracts, every KDoc line, under `contracts/src/main/kotlin/app/tangramplaytime/contracts/`: `IPuzzleLibrary.kt`, `ILockEngine.kt`, `Types.kt`.
  7. `decisions.md`:
     - the two WO-006 rows of 2026-10-17 in force, and the WO-006 rows Q1–Q9;
     - 2026-10-12: the TASK-015 row (`onStop` sends a drag home, also on a rotation; the title language), "**Inside the parallelogram's tray cell the visible thing wins, as on the board**", "**What 'drawn' means in the touch order**";
     - 2026-10-15: "**The top bar, Restart and the solved row, as drawn**", "**Browsing readings no REQ fixes**";
     - 2026-10-17: "**The grid overview, drawn in detail**", "**The DEV button, the overlay and the sheet, as placed**", "**The DEV aid, drawn in detail**" (the keyboard carry), "**A solution pose is read from the puzzle, not computed**" (how to find a piece's solution pose with the engine).
  8. In-tree tests, read-only — patterns and the assertions you must not break:
     - the app device kit folder: `AppLaunchKitTest.kt` (it claims the fresh start: leave the controller as found, seam note 3), `AppWiringKitTest.kt` (back closes the grid — do not duplicate it), `DevAidAppKitTest.kt` (`kitBounds` / `kitPress` injected on `play-screen`; `kitAssertEnglish` fails loudly on a non-English activity, so restore every locale), `ProcessDeathKitTest.kt` (`kitAssertShowsLeft`: board pieces against the mapping; its phases stay guarded skips), `FloorChannelTest.kt`;
     - the play device kit folder, as patterns for touch only (another module: copy the approach into `KitApp.kt`, never import): `KitScreen.kt`, `DragKitTest.kt` (drags with the 30 dp float, the two-finger twist), `DecorationKitTest.kt` (the badge).
  9. Rules: `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2; `C:/GitHub/AI/SWDev/framework/methodology/task-decomposition.md` (the same three sections as above); `platform-notes/android.md`, all of it; `architecture.md` §2 G-01, G-10, G-12, G-14, G-17 and §3b O-05, O-06, O-08.
- **Do not read:**
  - the bodies of the main code of `:app`, `:play`, `:kernel`, `:content`, `:store` and of `play/src/debug` — the tags, keys and `AppGraph`'s API come from the seam rows; compiler messages are fine;
  - the implementer briefs; DI-1 … DI-4;
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-030 row's Inputs cell: the bundled `Tangrams/*.json` through `:content` on the app's classpath; the four string files, read as resources (`R.string.*` of `:app`, and `:play`'s debug `R` for `dev_aid_*`); `tools/device_reset.sh` around every device run.
- **Oracle rule:**
  - As in TASK-027's brief: every assertion cites its source; a token only on its meaning; no dotted IDs in prose; fixtures discriminate (a Finnish comparison holds only for texts whose `fi` and `en` resources differ — show it test-side); positions and density are read from the launched activity and the screen in use, and each point is asserted inside the region meant before it is touched (the WO-005 lesson).
  - **A size the device lacks is constructed, never skipped** (seam note 4). Locales and airplane mode follow seam note 5: on API 33+ (locales) and API 30+ (airplane) the kit sets them itself; on the floor the channel line sets them through root routes before Gradle and tells the kit through an instrumentation argument (`kitDeviceLocales`, `kitAirplaneByChannel`). The WO's named skips (`workorders/WO-006.md` § Checkpoint items, last paragraph) are now only the two `ProcessDeathKitTest` phases: nothing of yours may skip. The `cmd locale` legs carry `@SdkSuppress(minSdkVersion = 33)`, which filters them on the floor — check in the floor XML that they are absent rather than written as a `<failure>` skip, and report it.
  - **Every class leaves the controller and the device as it found them** (seam note 3), in `finally`: the open puzzle and every state it changed (Restart and Retry through the controller), the overview and the DEV sheet closed; `wm size reset` / `wm density reset`; the rotation as read before; airplane mode off where the kit switched it on (never on the floor: the channel line switches it back); the device locale as read before and the app locales cleared; `show_ime_with_hard_keyboard` as read before.
  - A quantifier gets its adversarial path in the same test (`platform-notes/android.md`): "every placed piece" across a rotation *with a drag running*; "everything is in English" with the device list `[sv-SE, fi-FI]`; "no player control in any screen" over every screen that exists, on each layout class.
  - Nothing sleeps: `composeRule.waitUntil`, or poll an observed state with a timeout that fails loudly.
- **Where the tokens go** (if your judgment moves one, say so; the orchestrator corrects the cells):
  - `PhoneLayoutKitTest.kt`: REQ-035.A1, REQ-035.A2;
  - `TabletLayoutKitTest.kt`: REQ-036.A1, REQ-036.A2;
  - `TouchTargetKitTest.kt`: REQ-037.A1;
  - `LanguageKitTest.kt`: REQ-047.A1 on the Finnish `cmd locale` leg and on the own-locale leg; REQ-047.A2 on the Swedish `cmd locale` legs and on the own-locale leg (its comment names the runs that make it A1 or A2 evidence: the floor's root-route runs with `fi-FI`, `sv-SE` and `sv-SE,fi-FI`);
  - `PlayThroughKitTest.kt`: REQ-006.A1, REQ-001.A1, REQ-008.A1, REQ-008.A2, REQ-010.A1 on the play-through methods; REQ-010.A2 on the installed-app method;
  - none in `KitApp.kt`.
- **`KitApp.kt`** (helpers, `kit` prefix): the display override (set through `UiAutomation.executeShellCommand`, then wait until the launched activity reports the target size and density; restore); rotation (`UiAutomation.setRotation`); the device locale (`cmd locale set-device-locale`) and the app's locale list (`cmd locale set-app-locales app.tangramplaytime --locales …`), API 33+; the device's own locale list, read from `Resources.getSystem().configuration` (the activity's own configuration is the app's rule, S-36); airplane mode (`cmd connectivity airplane-mode enable|disable` on API 30+, read back from `settings get global airplane_mode_on` on every API); the instrumentation arguments `kitDeviceLocales` and `kitAirplaneByChannel` (seam note 5); the window insets from the launched activity's decor view (`ViewCompat.getRootWindowInsets`: `navigationBars`, `systemBars`, `displayCutout`, `ime`); touch on `play-screen` (tap; drag from a tray cell to a target with the 30 dp float; the two-finger twist; the badge press); the solution pose of a piece (the 2026-10-17 solution-pose row); the closed world of texts (below); the window checks (below); the controller restore through `AppGraph.controller` on the main thread.
- **What each class needs** (pointers; the REQ file and the seam row are the oracle):
  - **`PhoneLayoutKitTest.kt`:**
    - **REQ-035.A1:** under the 390 × 844 dp override (and at the device's own size where it is a phone), a full puzzle open: every `tray-piece-<ID>` of its seven pieces is displayed, inside its `tray-cell-<ID>`, inside the window and above the navigation-bar inset; `play-screen` is 390 × 844 dp (the display is that phone, drawn edge to edge); no node of the tree offers a scroll action or a scroll range while the Play screen shows ("without scrolling").
    - **REQ-035.A2:** every tray cell ends at or above the window's bottom minus the navigation-bar inset, and that inset is greater than 0 (fail loudly otherwise). On API 29 and later, put the `navigation_mode` you read through the shell into the message. The REQ-035.A2 evidence is the dev image with gesture navigation (the channel line records `navigation_mode` 2); the same assertion runs on every image.
    - **Pins, no token:** REQ-035 rule 1's two rows (every row-2 cell at or below every row-1 cell, each row in TYPE-001 order); portrait only — under `setRotation(ROTATION_FREEZE_90)` the window stays taller than wide (REQ-035 statement); rule 3 — `play-screen` covers the whole window, the top bar's zones lie below the status-bar inset and inside the side insets, the cells and the solved row above the bottom inset; rule 4 — Back with nothing open leaves the app (the activity is no longer resumed; `Espresso.pressBackUnconditionally`) while the store already holds the board placed before it (S-20; read after relaunch); the keyboard carry (debug app) — open the DEV prompt, set `show_ime_with_hard_keyboard` to 1, wait for the keyboard inset to exceed 0 (fail loudly after a timeout), then `dev-aid-passcode`, `dev-aid-ok` and `dev-aid-cancel` lie above the keyboard's top and `board`'s `BoardMappingKey` equals its value before the prompt opened (S-32, S-33; `decisions.md` 2026-10-17 "The DEV aid, drawn in detail").
  - **`TabletLayoutKitTest.kt`:**
    - **REQ-036.A1:** at 1280 × 800 dp and at 800 × 1280 dp (seam note 4), a full puzzle open: one horizontal line crosses all seven cells, left to right in TYPE-001 order, each inside the window and the safe area. Pin, no token: a mini puzzle's cells in one row too (REQ-012 rule).
    - **REQ-036.A2:** In progress with at least three pieces placed (through the controller at engine-confirmed poses, or by touch), rotate to portrait and back. After each rotation — the activity recreated, `play-screen` laid out again — the same placements (piece, turn, mirror, centre within 1e-6), each drawn through the new mapping (node box = the box of the engine's corners ±2 px, `ProcessDeathKitTest`'s pattern), the same puzzle and state. **Adversarial path:** a rotation while a drag runs (started by a held touch, or through the controller's `startDrag`): the dragged piece is back in its cell, every placed piece kept ("What survives a rotation": `onStop` sends a drag home).
    - **Pins, no token** ("What survives a rotation"): a tray turn kept; the overview's open state kept; a pulse or celebration set before the rotation is not played again.
  - **`TouchTargetKitTest.kt`** (REQ-037.A1; Q5): on each layout class (the phone at 360 × 780 dp; the tablet at 1280 × 800 and 800 × 1280 dp), every screen of § WO-006's "player controls per screen": the size of each listed node in dp, ≥ its minimum (‹ › 52 dp, badges 60 dp, everything else 48 dp; grid cells also ≥ 64 dp as a REQ-050 pin); the corner probe; the completeness check; `dev-*` nodes exempt; board pieces not swept.
  - **`LanguageKitTest.kt`** (Q7):
    - The screens that exist: the Play screen New and In progress, the solved view, the grid overview, and the DEV pill and prompt (the debug app shows them; their strings come from `play/src/debug/res`).
    - **The `cmd locale` legs (API 33+, `@SdkSuppress(minSdkVersion = 33)`):**
      - **REQ-047.A1 (mechanism):** with the device locale `fi-FI`, each text on those screens equals its resource as resolved for `fi` (`createConfigurationContext` on the target context), and the title equals the open puzzle's `title.fi` from the bundle. At least the compared texts whose `fi` and `en` resources differ discriminate. Never a typed Finnish word (G-10).
      - **REQ-047.A2:** with the device locale `sv-SE`, and with the app's locale list `[sv-SE, fi-FI]` (the adversarial path: Android alone would mix Finnish strings with English titles), every text equals its `values/` resource and the title the puzzle's `title.en`. On the floor these legs are filtered; the list case is covered there by the own-locale leg's `sv-SE,fi-FI` run.
    - **The own-locale leg (every image, every run; REQ-047.A1 and REQ-047.A2):** it reads the device's own locale list. When the channel passes `kitDeviceLocales` — one locale or a comma-separated list such as `sv-SE,fi-FI` — it first asserts that the device's list starts with the locales named, in that order (fail loudly otherwise: a root route that did not take must not pass as English). Then it asserts S-36's rule for that list on the same screens — Finnish first: the `fi` resources and `title.fi`, compared as in the A1 leg; anything else: the `values/` resources and `title.en`. In the ordinary runs (`en-US`) it is the "in English otherwise" check of REQ-047's statement; in the floor's root-route runs it is that image's A1 evidence (`fi-FI`) and A2 evidence (`sv-SE`, and the adversarial `sv-SE,fi-FI`) (seam note 5). Only the run with the matching argument may run it under a non-English device: the other app classes assume English (`DevAidAppKitTest.kitAssertEnglish`).
  - **`PlayThroughKitTest.kt`** (Q8, Q9):
    - **The play-through** (REQ-006.A1, REQ-001.A1, REQ-008.A1, REQ-008.A2, REQ-010.A1), once per leg — the phone at 360 × 780 dp in portrait; the tablet at 1280 × 800 dp and at 800 × 1280 dp: from the library's first mini puzzle (opened New through the controller), solve it by touch; then reach a full puzzle by touch (the solved row's Next, ‹ ›, or the counter → the grid → its cell) and solve it by touch. Across the legs use every move of REQ-014 to REQ-018 at least once — a drag from the tray, a tap turn in the tray, a two-finger twist, the flip badge — with every lock made by a drop (REQ-019, REQ-021) and the Solved state and its picture reached (REQ-022, REQ-023). End with Retry by touch.
    - **At every step** (REQ-001.A1, REQ-008.A1, REQ-008.A2): the closed world of texts — every text node shown is one of the app's own texts (the `values/` strings of `:app`, the `dev_aid_*` strings, the bundle's titles, the counter format's output for this library, the best time's "–"), and none of those allowed texts holds a currency symbol (a price, REQ-008.A1); the decor view holds no `WebView` and no view class outside `android.*` / `androidx.*` (no ad surface); `MainActivity` is the only resumed activity and the active window's package is the app's (no rating prompt, no ad activity).
    - **Airplane mode** (REQ-010.A1), on and read back on for the whole play-through:
      - on API 30+ the kit switches it on itself and off in `finally`;
      - below API 30, `UiAutomation`'s shell has no route (the broadcast is protected): with `kitAirplaneByChannel=true` the channel line has switched it on through the measured root route (floor step (e)), and the kit requires `airplane_mode_on` = 1 at the start and after the last step, failing loudly otherwise — it never switches it itself there;
      - below API 30 without the argument (the floor's ordinary whole-set run), the play-through runs and asserts without airplane mode, and its message points at floor step (e) for REQ-010.A1. It is not a skip.
    - **The installed app** (REQ-010.A2): the package's `requestedPermissions` and `permissions` are null or empty (`PackageManager.GET_PERMISSIONS`), on every image.
- **Run:**
  1. Write the six files into the app device kit folder. Landing is your Done-check, met at landing.
  2. Compile with the kit compile line: `:app:assembleDebugAndroidTest` must compile with no error (no red window: seam note 2).
  3. Grep for `REQ-0[0-9]*\.A[0-9]`: exactly the row's list, the WO-003…005 tokens unchanged.
  4. Optional, informational, never a verdict: one class on the dev image with a reset and a clean check around it.
- **Done means:** the TASK-030 row's Done-check, plus hygiene: helper names carry a `kit` prefix; no new dependency (the catalog's AndroidX test, Compose UI test and Espresso suffice; `UiAutomation` comes from the instrumentation); never edit an existing test, main code, a build file or `tools/`.
- **Return:** the handoff block, with: the test names per acceptance ID and every pin with its source; which legs run on which image and why; the files outside `app/` read at run time; the compile status; every device setting the kit changes and where it is restored; the corrected checks so far; any seam you found ambiguous (a `recirculate → planner`, never a guess).

---

## Brief — TASK-028 · WO-006 · slice-implementer

- **Mission:** Two parts, in this order.
  1. **Prove the floor routes** (`decisions.md` 2026-10-17, the Q7 and Q8 rows as amended; `tasks.md` § WO-006 seam note 5) on `Phone_API_26`, and record each as works or does not, with the command and its read-back:
     - **Locale:** one boot with `-change-locale fi-FI`; read back `getprop persist.sys.locale` and `settings get system system_locales`; then a boot with `-change-locale en-US` and the same read-back. If `-change-locale` does not take, try a root route (`adb root`, `setprop persist.sys.locale`, a framework restart) and record which one works.
     - **Airplane mode:** `adb root`; as root `settings put global airplane_mode_on 1` and `am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true`; read back the setting and that the radio is off (`dumpsys connectivity` or the telephony registry); off the same way; `adb unroot`. The route runs from the channel line's `adb shell` (seam note 5), so that is where you prove it.
     - **Optional — a list:** a root route that gives the floor the locale list `[sv-SE, fi-FI]` (for example `settings put system system_locales` with `persist.sys.locale` and a framework restart), read back. If none works, the list leg stays the WO's named floor skip.
  2. **Build S-37** in `tools/device_reset.sh`: the reset also restores, and `--check` also reports by name, the device-level state the WO-006 kit and the floor channel change. The script is the safety net under the kit's own restores (seam note 3).

  The class is device-only work on three images, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-006: the intro seam notes 3–5; S-37; the row TASK-028; the channel table and "What the app set does on each image" in this file.
  2. `decisions.md` 2026-10-17, the WO-006 rows Q7 and Q8 (their amended sentences); `workorders/WO-006.md` § Checkpoint items, its last paragraph (the named guarded skips your probes decide).
  3. `tools/device_reset.sh` as WO-003 and WO-004 left it; `decisions.md` 2026-10-12 "**Verifier V-01's reading of G-01 'product code' and its liveness rules**" (the existing `--check` semantics: an unreadable install is never "clean").
  4. `platform-notes/android.md` § Device hygiene and § Gradle from Git Bash; `.swdev/run-log/WO-006.md` (the env, the emulator lines, the three AVDs).
- **Do not read:** the kits, the main code, the other briefs, `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** none (the script reads device state only).
- **Run:**
  - First the floor probes (part 1), one emulator at a time: kill the dev image, boot the floor image as the channel table says with the extra flag, probe, and leave the floor at `en-US` with adbd not root.
  - On the dev image (`emulator-5554`): the row's positive control. The app-locale item needs the package installed: `$ADB install -r app/build/outputs/apk/debug/app-debug.apk` (built at the g3 line), and the reset leaves it as the old `pm clear` path does.
  - The same on the floor image (with its own items: the device locale through `persist.sys.locale`, airplane mode through the root route if it works, adbd left as root) and on the tablet image, then boot the dev image again and leave it running. Measure each image's `accelerometer_rotation` and `user_rotation` defaults before you change anything.
- **Done means:**
  - The row's Done-check, on the three images.
  - The old behaviour unchanged: `pm clear`, the `run-as` listing, exit codes 0 / 1 / 2, no device attached → 2.
  - Every item is skipped by API level where the command does not exist (`ro.build.version.sdk`), never reported clean by a failing command (an unknown state is never "clean").
  - **Decisions to hand back** as proposed rows: the floor routes that work and the ones that do not (the orchestrator writes them into the floor channel line and the WO's named skips); a floor or tablet default that differs from the dev image's; anything else the row does not fix.
  - **Ambiguity:** stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with: each floor probe's commands, outputs and read-back, and its verdict (works / does not); the commands, outputs and exit codes per image (once); the measured defaults; the proposed rows.

---

## Brief — TASK-029 · WO-006 · slice-implementer

- **Mission:** Build the safe-area geometry and TYPE-007:
  - S-31 in `play/src/main/kotlin/app/tangramplaytime/play/PlayLayout.kt`;
  - S-32 in `play/src/main/kotlin/app/tangramplaytime/play/PlayScreen.kt` (`ScreenGeometry`);
  - S-33 in `PlayScreen.kt`, `Overview.kt`, `DevAidHook.kt` and the aid's `DevAid.kt` and `DevAidSheet.kt`;
  - S-34 in new `kernel/src/main/kotlin/app/tangramplaytime/kernel/LayoutClass.kt`.

  When it is done, `SafeAreaKitTest.kt` and `LayoutClassKitTest.kt` are green, the whole JVM kit is green again, and the eleven play device classes still pass on the dev image. The class is UI geometry and rendering under every screen, so it goes to the strongest model.
  - **Riskiest item first:** seam note 1 — with `PlayInsets.NONE` and no keyboard every slot equals WO-005's. The eleven play device classes and the scaffolding pins depend on it.
- **Read exactly:**
  1. `tasks.md` § WO-006: the intro (seam notes 1 and 6, the red windows); § Seams S-31, S-32, S-33, S-34 (S-35 as context: TASK-031 calls you); the touch geometry per layout class; the row TASK-029.
  2. The screen you change: `tasks.md` § WO-003 › Seams S-5, S-6, S-10, S-12; `designs/WO-004-design.md` §2, S-22 (touch geometry, hit order, the overview); § WO-005 › Seams S-25, S-10Δ2, S-22Δ2 and the touch geometry's "The sheet".
  3. `workorders/WO-006.md` § Constraints & notes, questions 1, 2 and 3, and the standing constraints.
  4. Requirements: `REQ-035.md`, `REQ-036.md`, `REQ-037.md`, `REQ-013.md`, `REQ-050.md`, `REQ-046.md`; `Requirements/req_types.md` TYPE-001, TYPE-007.
  5. `architecture.md` §1 (the D1 note), §2 G-12, G-17, §3b O-05, O-08.
  6. `decisions.md`: the WO-006 rows Q1, Q2 and Q3; 2026-10-11 "**Board and tray drawing numbers from DI-1/DI-2**"; 2026-10-17 "**The grid overview, drawn in detail**", "**The DEV aid reaches the screen through one door**", "**The DEV aid, drawn in detail**" (the keyboard carry).
  7. Design inputs (behaviour reference only, never DoD): **DI-2** `Spec/02-ui-layout.md` §2, §3 and §6; **DI-1** `tools/prototype_template.html` `layout()` (the non-compact values). `design-inputs.md` §2 "Also fine" (board fit, the tray rules).
  8. The visible acceptance tests, read-only: `play/src/test/kotlin/app/tangramplaytime/play/SafeAreaKitTest.kt`, `kernel/src/test/kotlin/app/tangramplaytime/kernel/LayoutClassKitTest.kt`, and the play device kit (the eleven classes keep their meaning).
  9. Existing files: in `play/src/main/kotlin/app/tangramplaytime/play/` — `PlayLayout.kt`, `PlayScreen.kt`, `PlayDrawing.kt` (`Slot`), `TrayMetrics.kt`, `Controls.kt`, `TopBar.kt`, `FlipBadge.kt`, `HitTest.kt`, `PointerRouter.kt`, `Overview.kt`, `DevAidHook.kt`; the aid folder's `DevAid.kt` and `DevAidSheet.kt`; `kernel/build.gradle.kts`; the scaffolding tests that build `ScreenGeometry` (`play/src/test/kotlin/app/tangramplaytime/play/scaffolding/`).
- **Do not read:** the other briefs; kit drafts outside the tree; `app/src/main` beyond a compile check; `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-029 row's Inputs cell.
- **Run:**
  - The JVM line (run 1, then run 2).
  - The kit compile line, then the play device set on the dev image (whole set), with a reset and a clean check around it.
  - `:app:assembleDebug :app:assembleRelease` compile.
  - `grep -rnE "devaid|DevAid[^H]|0417|dev_aid_|Wrong passcode" play/src/main app/src/main` prints nothing (O-08, G-08).
- **Done means:**
  - The seams exactly as the rows say; `ScreenGeometry`'s new fields with the names S-32 gives (the JVM kit calls them).
  - No `:app` change (TASK-031 passes the insets), no string, no new dependency; every path in px (G-17); `LayoutClass` is pure JVM (G-12).
  - Scaffolding tests you break, you update, and you say which.
  - **Decisions to hand back** as proposed rows: any number or reading no row fixes (for example, where the overview's sheet sits when the safe area is narrower than its 640 dp maximum), with its source.
  - The Done-check is the TASK-029 row. **Ambiguity:** stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with: the inputs read and the files changed; the commands, exit codes, result XML paths and test counts, once; the scaffolding tests changed; the proposed rows.

---

## Brief — TASK-031 · WO-006 · slice-implementer

- **Mission:** Build S-35, the window shell, in `app/src/main/kotlin/app/tangramplaytime/app/MainActivity.kt`, `app/src/main/kotlin/app/tangramplaytime/app/PlayLayouts.kt` and `app/src/main/AndroidManifest.xml`: the layout class (S-34), the orientation policy, the tablet `PlayLayout`, edge to edge with the insets and the keyboard inset in `PlayLayout`. When it is done, `PhoneLayoutKitTest`, `TabletLayoutKitTest`, `TouchTargetKitTest` and `PlayThroughKitTest` are green on the dev, tablet and floor images, and every older set stays green. The class is device-only work on three images, so it goes to the strongest model.
  - **Riskiest items first:** the orientation request and the class on API 26 against API 37 and the tablet (Android 16 and later ignore orientation requests on screens 600 dp and wider, which matches TYPE-007's boundary; on API 26 an orientation request from a translucent activity throws — the theme is opaque, keep it so); the keyboard inset reaching Compose on API 26 (`adjustResize`).
- **Read exactly:**
  1. `tasks.md` § WO-006: the intro (seam notes 2–5); § Seams S-31, S-34, S-35 (S-32, S-33 as context: what `:play` does with the insets); the touch geometry per layout class; "What survives a rotation"; the row TASK-031.
  2. `tasks.md` § WO-003 › Seams S-15; `designs/WO-004-design.md` §2 S-15Δ; § WO-005 › Seams S-15Δ2.
  3. `workorders/WO-006.md` § Constraints & notes, questions 1, 2, 6 and 7, and the standing constraints.
  4. Requirements: `REQ-006.md`, `REQ-035.md`, `REQ-036.md`, `REQ-037.md`, `REQ-001.md`, `REQ-008.md`, `REQ-010.md`; `REQ-050.md` (Back closes the grid) and `REQ-025.md` (the state already saved); `Requirements/req_types.md` TYPE-007.
  5. `architecture.md` §2 G-01, G-12, G-17, §3b O-05.
  6. `decisions.md`: the WO-006 rows Q1, Q2, Q3, Q4, Q8, Q9; 2026-10-12, the TASK-015 row (`onStop`, reduced motion); 2026-10-11 "**The signature permission … is removed**" (keep those two manifest lines).
  7. Design inputs (behaviour reference only): **DI-2** `Spec/02-ui-layout.md` §2 and §6; **DI-1** `tools/prototype_template.html` `layout()` (the tablet numbers).
  8. The visible acceptance tests, read-only: the app device kit folder's `PhoneLayoutKitTest.kt`, `TabletLayoutKitTest.kt`, `TouchTargetKitTest.kt`, `PlayThroughKitTest.kt` and `KitApp.kt`.
  9. Existing files: `MainActivity.kt`, `PlayLayouts.kt`, `AppGraph.kt`, `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`; `play/src/main/kotlin/app/tangramplaytime/play/PlayLayout.kt` and `kernel/src/main/kotlin/app/tangramplaytime/kernel/LayoutClass.kt` as TASK-029 left them.
- **Do not read:** the other briefs; `play/src/main` bodies beyond S-31 and S-34; `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-031 row's Inputs cell.
- **Run:**
  - The JVM line; the kit compile line.
  - On the dev image (record `navigation_mode`), the tablet image and the floor image, one class per invocation with a reset and a clean check around each: the four new classes and the WO-003…005 app classes; `LanguageKitTest` is not run for a verdict. On the floor, add the channel table's step (e): `PlayThroughKitTest` alone with airplane mode on through the root route.
  - The play device set on the dev and floor images.
  - V-01 and its positive control.
- **Done means:**
  - S-35 exactly; the tablet value's numbers as S-35 lists them.
  - No string, no permission (G-01: the merged manifest still declares none), no new dependency; Back handled only while the overview is open (Q4); `onStop` unchanged.
  - **Decisions to hand back** as proposed rows: the orientation mechanism you chose (manifest portrait with a run-time tablet unlock, or a run-time request only) and why; any reading no row fixes.
  - The Done-check is the TASK-031 row. A kit leg you believe is wrong is reported with the evidence (the orchestrator arbitrates), never worked around. **Ambiguity:** stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with: the files changed; per image the commands, exit codes, result XML paths and counts, once; the measured window sizes and insets per image; the proposed rows.

---

## Brief — TASK-032 · WO-006 · slice-implementer

- **Mission:** Build S-36: new `app/src/main/kotlin/app/tangramplaytime/app/AppLanguage.kt`; `MainActivity.kt` wraps its base context in `attachBaseContext` and takes `PlayTexts.titleLanguage` from the same rule. When it is done, `LanguageKitTest` is green — the `[sv-SE, fi-FI]` leg included — and every set stays green. The class is device-only, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-006: seam note 5; S-36; the row TASK-032.
  2. `tasks.md` § WO-003 › Seams S-11 (`PlayTexts`, the title rule).
  3. Requirements: `REQ-047.md`; `REQ-012.md` (the title rule), `REQ-043.md` (the marks are the same letters).
  4. `architecture.md` §2 G-10, §3b O-06.
  5. `decisions.md`: the WO-006 row Q7; the two WO-006 rows of 2026-10-17 in force (scope, A1 unverified); 2026-10-12, the TASK-015 row (the flagged mix); 2026-10-17 "**The DEV aid's strings live with the aid**".
  6. `capture-outbox/chg-draft-req047-english-contract.md` (why A1 stays unverified).
  7. The visible acceptance test, read-only: `app/src/androidTest/kotlin/app/tangramplaytime/app/LanguageKitTest.kt` and `KitApp.kt`.
  8. Existing files: `MainActivity.kt` as TASK-031 left it; `play/src/main/kotlin/app/tangramplaytime/play/PlayTexts.kt`; the four string files, read-only.
- **Do not read:** the other briefs; `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** the TASK-032 row's Inputs cell.
- **Run:** the JVM line; the kit compile line; `LanguageKitTest` on the dev, tablet and floor images, on the floor also in the root-route runs of the channel table's steps (a)–(c), then the restore (d); then the whole app set on the three images and the play set on the dev and floor images, a reset and a clean check around each step; V-01 and its positive control.
- **Done means:**
  - S-36 exactly: one rule, read from the device's first locale; no string added or changed; no `values-sv`; no new dependency. The DEV aid's strings follow the same rule because they resolve through the activity.
  - The Done-check is the TASK-032 row; REQ-047.A1 then stays `implemented (unverified)` for the draft.
  - **Decisions to hand back** as proposed rows: any reading no row fixes. **Ambiguity:** stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with the files changed, per image the commands, exit codes, result XML paths and counts (once), and the proposed rows.
