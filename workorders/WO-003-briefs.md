# WO-003 — dispatch briefs (#Solving)

**Planner:** wo-planner (Opus)  ·  **Date:** 2026-10-11  ·  **Plan:** `tasks.md` § WO-003 (TASK-009 … TASK-015; no task is `[P]`)
**Mode:** Light (`workorders/WO-003.md` § Mode; decisions.md line 55). Reason (a) is not claimed, because `:play` has compiled as a skeleton since P2. Reason (b) is not claimed, because `ILockEngine` and `IPuzzleLibrary` are consumed, not implemented or changed. There is no design: **`tasks.md` § WO-003 › Seams** (S-1…S-16, the tag table and the touch routing) is the seam table both lanes build against.

> Each brief is dispatched on its own. An Acceptance Test Author gets only its own brief plus the two tables below; it never sees an implementer brief. An implementer never sees another implementer's brief or a kit draft outside the tree.
> Every path is relative to `C:/GitHub/AI/TangramPlaytime/` unless it is absolute. "The JVM kit folder" is `play/src/test/kotlin/app/tangramplaytime/play/`; "the play device kit folder" is `play/src/androidTest/kotlin/app/tangramplaytime/play/`; "the app device kit folder" is `app/src/androidTest/kotlin/app/tangramplaytime/app/`.

## Dispatch order (two lanes)

| Step | Test lane (acceptance-test-author) | Implementer lane (slice-implementer) | Gate to the next step |
|---|---|---|---|
| 1 | TASK-009 (Opus) drafts and **lands** the JVM kit. TASK-011 (Fable) starts drafting the device kit in its scratchpad. | — | TASK-009's Done-check is met at landing |
| 2 | TASK-011 keeps drafting. | TASK-010 (Fable), reading the landed JVM kit | TASK-010's Done-check is green. This is the JVM kit's green verdict. |
| 3 | TASK-011 **lands** (resume the same author context with "TASK-010 done"). | — | TASK-011's Done-check is met at landing |
| 4 | — (both device sets fail to compile until TASK-012 lands) | TASK-012 (Fable) | TASK-012's Done-check: both device sets compile; BoardTrayKitTest green on the dev **and** the floor image |
| 5 | — | TASK-013 (Fable) | DragKitTest and PreviewKitTest green on the dev image |
| 6 | — | TASK-014 (Fable) | Its three kit files green, then the whole play device set green on the dev and the floor image |
| 7 | — | TASK-015 (Opus) | AppLaunchKitTest and FloorChannelTest green on the dev image; the verifier passes and its positive control sees the canary |
| 8 | Run `plan_check.py --wo WO-003` again, now that the kit has compiled (WO DoD) → Code Reviewer → Test & Verify (all three sets, every channel) | | |

- **Why this order.**
  - The planner fixed every name the kits call (§ Seams), so the JVM kit can be written before any product code. It lands first, so TASK-010 works against the visible acceptance tests for its IDs.
  - Every compile-level seam of the device kit (S-10…S-14, including the tags and keys whose nodes TASK-013 and TASK-014 add) is TASK-012's. So the device kit compiles once TASK-012 lands, and the later tasks only turn its classes green.
  - TASK-011 lands after TASK-010 so that a seam TASK-010 had to recirculate is settled before the device kit lands.
- **Dependencies versus dispatch.** Drafting the device kit during steps 1–2 is dispatch: it touches nothing in the tree. In the plan, TASK-011 depends on TASK-010, and TASK-012 depends on TASK-011, whose files its Done-check compiles and runs.
- **Red windows** (`tasks.md` § WO-003 intro, per source set). Nobody runs a set for a verdict inside its window.
  - `play/src/test`: from step 1 until TASK-010 lands.
  - `play/src/androidTest`: does not compile from step 3 until TASK-012 lands. From then on, the classes of TASK-013 and TASK-014 fail at run time until each lands.
  - `app/src/androidTest`: does not compile from step 3 until TASK-012 lands; AppLaunchKitTest fails at run time until TASK-015 lands.
- **No task is `[P]`.** Every task's tests read `Tangrams/*.json` through `:content`, and shared declared inputs serialize (`task-decomposition.md` v0.5).
- **Compile errors in the kit.**
  - An unresolved reference to a seam of a task that has not landed yet is expected (each kit row lists which).
  - Any other compile error in a kit file is a test defect. It goes back to that kit's author and counts as a corrected check. If TASK-012's Done-check fails on a device-kit compile error that is not an unresolved seam, the orchestrator resumes TASK-011's author. An implementer never edits a kit file.
- **Scaffolding files in the tree.**
  - `play/src/test/kotlin/app/tangramplaytime/play/PlayScaffoldTest.kt` stays as it is; TASK-010 keeps it green.
  - `app/src/androidTest/kotlin/app/tangramplaytime/app/FloorChannelTest.kt` asserts that the text "Tangram Playtime" is displayed. That is the one in-tree text assertion this WO's screen touches. **TASK-015 owns it** and adapts it in the same task that rewires `MainActivity`, so there is no window between the new screen and its check. The kits never edit it.
- **Decisions the implementers record.** Numbers that come only from DI-2 (or DI-1) are decisions, not Contract. Each brief lists the ones its task must hand back as proposed `decisions.md` rows; the orchestrator appends them, so each is recorded once.

## Verification channels (G3)

Each "the line" below is `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat <tasks>"`. `ADB` is `/c/Users/Jami/AppData/Local/Android/Sdk/platform-tools/adb` and `EMU` is `/c/Users/Jami/AppData/Local/Android/Sdk/emulator/emulator`.

| Channel | How (Git Bash) | Result | Needed by |
|---|---|---|---|
| JVM `:play:testDebugUnitTest` + `:contracts:test` (JUnit 4) | the line with `:play:testDebugUnitTest :contracts:test` | `play/build/test-results/testDebugUnitTest/TEST-*.xml`, `contracts/build/test-results/test/TEST-*.xml` | TASK-009 (compile status), TASK-010; every later task keeps it green |
| Device kit compile | the line with `:play:assembleDebugAndroidTest :app:assembleDebugAndroidTest` | the exit code and compiler messages | TASK-011, TASK-012 |
| Dev image `Medium_Phone_API_37.0` (Android 17), running as `emulator-5554` since G3 | `$ADB devices` first. Then the line with `:play:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.play.BoardTrayKitTest` (classes comma-separated; omit the argument for the whole set), or `:app:connectedDebugAndroidTest` | `play/build/outputs/androidTest-results/connected/debug/TEST-*.xml`, `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` | TASK-012 … TASK-015 |
| Floor image `Phone_API_26` (Android 8.0, `minSdk 26`). One emulator at a time. | `$ADB emu kill`, then `$ADB devices` until it is empty. `$EMU -avd Phone_API_26 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot` in the background; `$ADB wait-for-device`, then poll `$ADB shell getprop sys.boot_completed` until it is `1`, then `$ADB devices`. Run the same Gradle line. Afterwards kill it and boot `Medium_Phone_API_37.0` the same way, and leave it running. | the same XML folders (the file name carries the device) | TASK-012 (BoardTrayKitTest), TASK-014 (the whole play set), Test & Verify (every set) |
| Release verifier (G-01, G-08) | the line with `:app:assembleRelease :app:assembleDebug`, then `python .swdev/verifiers/v01_release_apk.py`, `python .swdev/verifiers/v01_release_apk.py --positive-control` and `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-003` | one PASS/FAIL line each, then GREEN/RED and the budget line | TASK-015, the close |
| Device hygiene | `bash tools/device_reset.sh` before and `bash tools/device_reset.sh --check` after each device step, once TASK-015 has landed it. Before that, `:play`'s tests keep no state (the controller is in memory). | `--check` reports clean or not | TASK-015, Test & Verify |

- **Not a DoD channel:** human eyes. The owner plays the build at close from `review-notes.md` (colours, size marks, pulse).
- **Toolchain proof:** JVM, floor and dev channels are proven (run log `.swdev/run-log/WO-003.md` rows g3, f26, d37). `:app:assembleRelease` is proven in this tree (WO-002 run log a0). The verifier itself is TASK-015's deliverable.
- **Time in device tests:** this WO has no `Clock`. Every animation runs on Compose's frame clock; a device test drives the compose rule's `mainClock` and never sleeps (`platform-notes/android.md` § Compose test kit).

---

## Brief — TASK-009 · WO-003 · acceptance-test-author

- **Mission:** Write the JVM part of WO-003's one acceptance kit: four files in the JVM kit folder, against § Seams S-1…S-9. Land it before TASK-010 starts. It is a pure-JVM kit, so it runs on Opus.
- **Read exactly:**
  1. `tasks.md` § WO-003:
     - the intro;
     - § Seams S-1…S-9 (what your tests call), and S-10…S-16 for orientation only;
     - the rows TASK-009 (yours) and TASK-010 (whose Done-check is your green verdict).
  2. `workorders/WO-003.md`: § Scope with every scope note; § Constraints & notes: "Seams", "In-memory state behind the seam", "Gestures", "Pending deltas".
  3. Requirements, collection v1.2 (read-only):
     - `Requirements/reqs/REQ-002.md`, `REQ-012.md`, `REQ-013.md`, `REQ-017.md`, `REQ-018.md`, `REQ-020.md` (your tokens);
     - `REQ-016.md`, `REQ-019.md`, `REQ-021.md`, `REQ-022.md`, `REQ-045.md`, `REQ-051.md`: rules your fixtures cross; their tokens are TASK-011's or not this WO's;
     - `Requirements/req_types.md`: TYPE-001, TYPE-003, TYPE-004, TYPE-006.
  4. Pending drafts:
     - `capture-outbox/chg-draft-req002-a1-mirror.md`. REQ-002.A1 is tested as the draft reads it: the solve may use the badge where a stored solution has the mirrored parallelogram (decisions.md line 58).
     - `capture-outbox/chg-draft-req020-interrupted-drag.md` and `capture-outbox/chg-draft-type006-missing-transitions.md`, for the untokened pins (`goHome`; a missed first drop stays In progress, as TYPE-006 is locked).
  5. Frozen contracts, baseline 2026-10-11, every KDoc line: `contracts/src/main/kotlin/app/tangramplaytime/contracts/ILockEngine.kt`, `IPuzzleLibrary.kt`, `Types.kt`.
  6. `decisions.md` lines 29 (tray turns not saved), 37 and 40 (lines moved into this WO), and 55–60 (the WO-003 rows).
  7. In-tree tests, to avoid collisions:
     - `play/src/test/kotlin/app/tangramplaytime/play/PlayScaffoldTest.kt`: not yours; never edit it or reuse its name.
     - `kernel/src/test/kotlin/app/tangramplaytime/kernel/KitFixtures.kt` and `LockEngineKitTest.kt` (its REQ-020.A1 test is the seam-level precedent): read for the pattern (`kit`-prefixed names) only. They are not on `:play`'s classpath.
     - `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLibraryKitTest.kt`, the REQ-045.A1 seam precedent.
  8. `play/build.gradle.kts`: JUnit 4 is on the test classpath; `:kernel` and `:content` arrive as test dependencies with TASK-010.
  9. Rules:
     - `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2: a token is a coverage claim; prose writes a line without the dot (`REQ-002 A2`).
     - `architecture.md` §2 G-04, G-12, G-15; §3b O-01, O-04, O-07.
- **Do not read:**
  - `play/src/main/` bodies, once TASK-010 writes them (compiler messages are fine); `kernel/src/main/` and `content/src/main/` bodies;
  - the implementer briefs (§ TASK-010 … § TASK-015 of this file); TASK-011's drafts;
  - DI-1…DI-4 (`tools/prototype_template.html`, `Spec/`, `Study/`). They are generated inputs and supply no Contract text, and § Seams carries every value your tests need.
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, read through `PuzzleLibraries.bundled(LockEngine())` on the test classpath. No other file outside `play/` is read at run time.
- **Oracle rule:**
  - Every assertion cites, in a comment, the sentence it checks: a REQ sentence, a TYPE rule, a KDoc line, a seam row (`S-n`) or a `decisions.md` line. Assert only what is mandated.
  - A REQ token goes only on a test whose assertion is that line's meaning.
  - A pin carries `// decision decisions.md line NN`, `// seam S-n` or `// TYPE-006 rule` and no token.
  - Fixtures must discriminate: show test-side, with the engine, that the state you claim to distinguish exists (for example, that a spot really fits before you test that the mirror image does not lock there).
- **Where the tokens go.** If your judgment adds or moves a token, say so in the hand-back; the orchestrator corrects the cells.
  - `PlayControllerKitTest.kt`: REQ-002.A1, REQ-012.A2, REQ-018.A3, REQ-020.A1.
  - `TrayMetricsKitTest.kt`: REQ-013.A1. You may add REQ-013.A3 at S-6 (the same scale and cell size for a mini's pieces as for the full set); if you do, say so.
  - `TwistKitTest.kt`: REQ-017.A1, REQ-017.A2.
  - `KitPlay.kt`: none.
  - Never REQ-002.A2, REQ-039.A2 or any ID outside the WO's 32 with a dot.
- **What each line needs** (pointers, not restatements; the REQ file is the oracle):
  - **REQ-002.A1:** every puzzle `library.puzzles` lists is completed through S-2 alone (`tapTray`, `pressBadge`, `startDrag`, `moveDrag`, `updatePreview`, `drop`), ending in state Solved.
    - Find each stored solution polygon's turn and mirror test-side, by matching `engine.vertices` at the polygon's centre (the `Placement.centre` KDoc: the average of the corners).
    - Order the drops so that each one locks; REQ-019's rationale says an anchorable order exists.
    - The line stays `implemented (unverified)`, and the token still goes on.
  - **REQ-012.A2:** after every action of a scripted sequence, the puzzle's pieces are exactly `trayPieces` plus the placed pieces plus the dragged piece, with no overlap. Use tray drags, board drags, misses, `goHome`, and opening another puzzle and coming back.
  - **REQ-013.A1:** S-6 at 360 × 780 dp with the S-5 phone literal.
  - **REQ-017.A1 and A2:** S-7 for a 90° and a 20° twist, and the dragged piece's turn after `twistDrag` with the resulting steps.
  - **REQ-018.A3:** first show that the unmirrored PG locks at the spot; then the mirrored PG, dropped at that spot, does not lock there.
  - **REQ-020.A1:** a sweep of drops (many centres over the board, and with `overBoard` false). Each ends with the piece in `placed` (a lock) or in `trayPieces`.
  - **Pins, no token:**
    - TYPE-006: New → In progress on a tray drag start, not on a tray tap; Solved at the last lock; a missed first drop stays In progress.
    - decisions.md line 59: `goHome` returns the piece without a pulse.
    - decisions.md line 29 and S-2 `open`: per-puzzle memory, and tray turns reset on open.
    - REQ-051 rule: a pulse only in a mini puzzle. Its tokens are TASK-011's.
    - S-4: the events and their order.
    - S-8: the 12 dp boundary.
    - S-9: the resting turns, checked through `engine.vertices` against TYPE-001's words.
- **Run:**
  1. Draft `KitPlay.kt`, `PlayControllerKitTest.kt`, `TrayMetricsKitTest.kt` and `TwistKitTest.kt`, then write them into the JVM kit folder. Landing is your Done-check, met at landing.
  2. Compile against the tree with the JVM line of the channel table. Expected, and not your defect: unresolved references to S-1…S-9 and to `LockEngine` / `PuzzleLibraries` (TASK-010 adds the test dependencies). Any other compile error is yours to fix.
  3. Grep the kit for `REQ-0` before you hand back.
- **Done means:** the TASK-009 row's Done-check, plus hygiene:
  - every helper name carries a `kit` prefix, so it cannot collide with an `internal` declaration of the main source set;
  - call `internal` members directly, with no reflection; JUnit 4 only; no dependency;
  - never edit `play/src/main`, `PlayScaffoldTest.kt` or a build file.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the test names for each acceptance ID, and the list of pins;
  - the files outside `play/` the tests read at run time;
  - the compile status, with the command and its exit code;
  - how many corrected checks there have been so far.

---

## Brief — TASK-011 · WO-003 · acceptance-test-author

- **Mission:** Write the device part of WO-003's one acceptance kit, against § Seams and the tag table:
  - seven files in the play device kit folder: Compose UI tests of `PlayScreen`, with a `PlayController` over the real engine and the bundled puzzles;
  - one launch test in the app device kit folder: the real wiring on a fresh start.
  Draft it beside TASK-009 and TASK-010; land it in one stage when resumed with "TASK-010 done". It is the Compose kit, so it runs on Fable.
- **Read exactly:**
  1. `tasks.md` § WO-003: the intro, § Seams (every row), the tag table, the touch routing, and the rows TASK-011 … TASK-015 (which of your files goes green at which step).
  2. `workorders/WO-003.md`: § Scope with every scope note; § Constraints & notes: "Seams", "In-memory state behind the seam", "Draw in pixels", "Gestures", "Timings", "Device tests", "Pending deltas".
  3. Requirements, collection v1.2 (read-only):
     - your tokens: `Requirements/reqs/REQ-011.md`, `REQ-012.md`, `REQ-013.md`, `REQ-014.md`, `REQ-015.md`, `REQ-016.md`, `REQ-017.md`, `REQ-018.md`, `REQ-020.md`, `REQ-021.md`, `REQ-023.md`, `REQ-039.md`, `REQ-043.md`, `REQ-045.md`, `REQ-051.md`;
     - rules only, no token: `REQ-019.md` and `REQ-022.md` (what a drop and a solve follow), `REQ-037.md` (the 60 dp badge area), `REQ-047.md` (the title language);
     - `Requirements/req_types.md`: TYPE-001, TYPE-003, TYPE-004, TYPE-006, TYPE-007.
  4. Pending drafts:
     - `capture-outbox/chg-draft-req014-a1-without-delay.md`. REQ-014.A1 is tested as its one-frame bound (decisions.md line 58).
     - `capture-outbox/chg-draft-req017-second-finger-never-a-tap.md`, `capture-outbox/chg-draft-req018-badge-touch-precedence.md` and `capture-outbox/chg-draft-req020-interrupted-drag.md`: pinned untokened (decisions.md line 59).
  5. Frozen contracts, baseline 2026-10-11, every KDoc line: `ILockEngine.kt`, `IPuzzleLibrary.kt`, `Types.kt` under `contracts/src/main/kotlin/app/tangramplaytime/contracts/`.
  6. `decisions.md` lines 29, 37, 40 and 55–60.
  7. `platform-notes/android.md`, § Compose test kit and § Device hygiene:
     - the import rules;
     - merged, inert containers;
     - `mainClock`, never a sleep;
     - a solve by touch is not judged by a visual that the completion replaces.
  8. In-tree tests:
     - `app/src/androidTest/kotlin/app/tangramplaytime/app/FloorChannelTest.kt`. It is the import precedent (`androidx.compose.ui.test.junit4.v2`). Its "Tangram Playtime" assertion is TASK-015's to adapt: never edit it or reuse its name.
     - TASK-009's landed JVM kit in `play/src/test/kotlin/app/tangramplaytime/play/`, for its helpers' approach only. `androidTest` cannot import from `test`.
     - `content/src/test/kotlin/app/tangramplaytime/content/PuzzleLibraryKitTest.kt`, the REQ-045.A1 seam precedent.
  9. Build files:
     - `play/build.gradle.kts`: the androidTest dependencies and the runner arrive with TASK-012;
     - `app/build.gradle.kts`: its androidTest dependencies exist, and `:content` and `:kernel` are on its classpath;
     - `gradle/libs.versions.toml`.
  10. Rules:
      - `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2;
      - `architecture.md` §2 G-10, G-12, G-17; §3b O-01, O-05, O-06, O-07.
- **Do not read:**
  - `play/src/main/` and `app/src/main/` bodies (compiler messages are fine); `kernel/src/main/` and `content/src/main/` bodies;
  - the implementer briefs;
  - DI-1, the prototype (`tools/prototype_template.html`, `Spec/prototype/`) — never. Do not read DI-2…DI-4 (`Spec/`, `Study/`) either: they supply no Contract text, and § Seams carries the values you need.
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, read on the device through `PuzzleLibraries.bundled(LockEngine())`. In `:play` that is an androidTest dependency from TASK-012; in the app it is the app's own classpath.
- **Oracle rule:** as in TASK-009, with these additions:
  - A semantics value (`CentreKey`, `DrawnPxPerUnitKey`, `TurnKey`) is the product's claim. Where it carries a line's assertion, cross-check it at least once against the node bounds or the pixels.
  - A rendering fixture is read against the geometry before the drawing is blamed.
- **How to drive the screen:**
  - **Touches.** Inject every touch on the `play-screen` node, in its own px. Puzzle points convert through `BoardMappingKey` on `board`; cell and piece positions come from node bounds taken relative to `play-screen`.
  - **Steering a drag.** The dragged piece floats above the finger, and a tray piece grows for up to 120 ms (REQ-014 rules). So read `drag-piece-<ID>`'s `CentreKey` after the grow, then move the finger by the difference.
  - **Main thread.** Drive the controller only on the main thread (`runOnIdle`).
  - **Setting up by touch or by API.**
    - Set a board up through S-2 only where the line is not about the gesture: for example, a solved state for each of the 13 puzzles (REQ-023, REQ-039), or a placed ST1 for REQ-015.A2.
    - Use touch wherever the line is about the gesture: REQ-012.A2, REQ-013.A2, REQ-014 … REQ-017, REQ-018.A1, REQ-020, REQ-021, REQ-045.A2 and REQ-051.
  - **Pixels.** Use `captureToImage()` on `board` or `tray` (available from API 26). Compare against `PlayPalette` and against the solution polygons mapped through `BoardMapping`. Leave out a band of about 2 px around every boundary.
  - **Time.** Use `mainClock` with `autoAdvance` off whenever a timing matters. The confetti, the pulse and the fade have the windows given in the tag table.
- **Where the tokens go.** If your judgment moves a token, say so; the orchestrator corrects the cells.
  - `BoardTrayKitTest.kt`: REQ-011.A1, REQ-011.A2, REQ-012.A1, REQ-013.A3, REQ-045.A1.
  - `DragKitTest.kt`: REQ-012.A2, REQ-013.A2, REQ-014.A1, REQ-014.A2, REQ-015.A1, REQ-015.A2, REQ-016.A1, REQ-016.A2, REQ-017.A1, REQ-017.A2, REQ-020.A1, REQ-020.A2.
  - `PreviewKitTest.kt`: REQ-021.A1, REQ-021.A2.
  - `DecorationKitTest.kt`: REQ-018.A1, REQ-018.A2, REQ-043.A1, REQ-043.A2.
  - `CornerPulseKitTest.kt`: REQ-051.A1, REQ-051.A2.
  - `SolvedPictureKitTest.kt`: REQ-023.A1, REQ-023.A2, REQ-039.A1, REQ-045.A2.
  - `AppLaunchKitTest.kt`: REQ-045.A1.
  - `KitScreen.kt`: none.
  - Never REQ-002.A2, REQ-039.A2, REQ-037.A1, REQ-047.A1 or REQ-047.A2 with a dot.
- **What some lines need** (pointers; the REQ file is the oracle):
  - **REQ-011.A1** also runs on the floor image, where it is the proof that no anti-aliasing seam shows between solution pieces. Keep it a pure pixel test that depends on nothing but `BoardMapping`, `PlayPalette` and the solution polygons.
  - **REQ-045.A1** is in two places, on purpose (`tasks.md` intro).
    - In BoardTrayKitTest, a fresh `PlayController` is the fresh start. This copy is the one under `play/` that trace-check's code-home rule needs (decisions.md line 57).
    - In AppLaunchKitTest, the real launch: `play-screen`'s `PuzzleIdKey` names a mini in a library you build yourself, and the tray holds three `tray-piece-*` nodes.
    - The app's controller lives once per process, so every test in one instrumentation run shares it. Only one test may claim the fresh start.
  - **REQ-020.A2 and REQ-051.A2:** "no error message" and "nothing else is shown" are checked by comparing the text-bearing and tagged nodes before and after the drop.
  - **REQ-023.A1, REQ-023.A2 and REQ-039.A1** run over every listed puzzle.
    - Compare the unsolved silhouette's mask with the board after the fade. Sample after the confetti has ended, or with `reducedMotion = true`.
    - The REQ-023.A2 oracle must fail on a build that draws piece outlines or piece colours over the picture.
  - **REQ-018.A2:** for every listed puzzle with a PG, exactly one badge node, and it is next to the PG. For the minis, which have no PG, none.
  - **REQ-043.A1:** a full-set tray, using the letters of the S-11 literal.
  - **REQ-045.A2:** placing the three pieces by touch. Judge the solve by `solved-picture`, not by the last piece's node.
- **Run:**
  1. **Now, beside TASK-009 and TASK-010:** draft the eight files in your scratchpad, never under `play/` or `app/`. Hand back the draft paths.
  2. **When you are resumed with "TASK-010 done":** write them into the two kit folders. Landing is your Done-check, met at landing.
     - Compile with the device-kit line of the channel table. Expected, and not your defect: unresolved references to S-10…S-14, and to the androidTest dependencies TASK-012 adds to `:play` (Compose UI test, AndroidX test, `:kernel`, `:content`).
     - When TASK-012 lands, the orchestrator may resume you for any other compile error. Each one counts as a corrected check.
  3. Grep both folders for `REQ-0` before each hand-back.
- **Done means:** the TASK-011 row's Done-check, plus hygiene:
  - helper names are `kit`-prefixed; JUnit 4 with the `AndroidJUnit4` runner; the `junit4.v2` compose rules, as in the precedent;
  - no new dependency;
  - never edit main code, a build file or `FloorChannelTest.kt`.
- **Return:** the handoff block, with:
  - the test names for each acceptance ID, and the list of pins;
  - the files read at run time outside each module;
  - the compile status, with the command and its exit code;
  - how many corrected checks there have been, and any token you moved.

---

## Brief — TASK-010 · WO-003 · slice-implementer

- **Mission:** Build the Play screen's state holder and the pure logic of `:play`, S-1…S-9, so that the JVM kit (TASK-009) is green.
  - The controller asks `ILockEngine` for every lock, preview, turn or mirror in place, and the solved check. It keeps TYPE-006 and the board in memory per puzzle.
  - The other seams: the REQ-013 tray arithmetic, the REQ-017 twist steps, the REQ-015 tap/drag classification and the TYPE-001 resting turns.
  - It is gesture logic and state, so it runs on Fable (WO § Constraints staffing).
  - **Riskiest item first:** prove that the bundled puzzles load in `play/src/test`.
- **Read exactly:**
  1. `tasks.md` § WO-003:
     - the intro and row TASK-010;
     - S-1…S-9: implement them exactly, because the kits call these names;
     - S-10, S-15 and the touch routing: who calls you.
  2. `workorders/WO-003.md`: § Scope notes (REQ-002 A1 unverified); § Constraints & notes: "Seams", "In-memory state behind the seam", "Gestures". "Timings" belong to the screen, not to you.
  3. Requirements, collection v1.2:
     - `Requirements/reqs/REQ-002.md`, `REQ-012.md`, `REQ-013.md`, `REQ-016.md`, `REQ-017.md`, `REQ-018.md`, `REQ-019.md`, `REQ-020.md`, `REQ-021.md`, `REQ-022.md`, `REQ-045.md`, `REQ-051.md`;
     - `Requirements/req_types.md`: TYPE-001, TYPE-003, TYPE-004, TYPE-006.
  4. Pending drafts: `capture-outbox/chg-draft-req002-a1-mirror.md`, `chg-draft-req017-second-finger-never-a-tap.md`, `chg-draft-req020-interrupted-drag.md`, `chg-draft-type006-missing-transitions.md`.
  5. Contracts, baseline 2026-10-11, every KDoc line: `ILockEngine.kt`, `IPuzzleLibrary.kt`, `Types.kt`.
  6. `decisions.md` lines 29 and 55–60.
  7. `architecture.md` §2 G-04, G-12, G-14, G-15; §3b O-01, O-04, O-05, O-07; §5 "Allowed dependencies".
  8. Design inputs:
     - **DI-2** `Spec/02-ui-layout.md` §2 and §3: the zones, the tray rows, padding, gap, margin and top bar that S-5 fixes. The REQ-013 rules are quoted there verbatim.
     - **DI-3** `Spec/01-gameplay.md` §2–§4: the interaction in prose.
     - **DI-5** `Spec/03-puzzle-format.md` §3: the reference shapes behind S-9.
     - **DI-1** `tools/prototype_template.html`: behaviour reference only, never DoD. Use it, for example, for the turn and mirror a piece keeps when it goes home.
     - `design-inputs.md` §2 items 3, 11 and 13.
  9. The visible acceptance tests (TASK-009), read-only: `play/src/test/kotlin/app/tangramplaytime/play/KitPlay.kt`, `PlayControllerKitTest.kt`, `TrayMetricsKitTest.kt`, `TwistKitTest.kt`.
  10. Existing files:
      - `play/build.gradle.kts`;
      - `play/src/main/kotlin/app/tangramplaytime/play/Play.kt` (to delete);
      - `play/src/test/kotlin/app/tangramplaytime/play/PlayScaffoldTest.kt` (keep it green);
      - `content/src/main/kotlin/app/tangramplaytime/content/PuzzleLibraries.kt` (the door);
      - `kernel/src/main/kotlin/app/tangramplaytime/kernel/LockEngine.kt` lines 1–40 (`class LockEngine : ILockEngine`, no-arg).
- **Do not read:** the other briefs; kit drafts outside the tree; `kernel/src/main/` and `content/src/main/` beyond the two doors; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, read through `:content` on the unit-test classpath; `LockEngine()` from `:kernel`.
- **Run:**
  - The JVM line of the channel table: `:play:testDebugUnitTest :contracts:test`.
  - Grep `play/src/main` for `app.tangramplaytime.kernel` and `app.tangramplaytime.content`. There must be no match (G-12): main code depends on `:contracts` only.
  - Grep `play/src/main` for `currentTimeMillis`, `SystemClock`, `nanoTime` and `Thread.sleep`. There must be no match (G-14): there is no time in the controller.
- **Done means:**
  - **First, the classpath.**
    - Add `testImplementation(project(":kernel"))` and `testImplementation(project(":content"))` to `play/build.gradle.kts`.
    - In a scaffolding test, prove that `PuzzleLibraries.bundled(LockEngine())` lists one puzzle per top-level `Tangrams/*.json` (not a literal 13).
    - WO-002 proved the bundle only for `:content`'s own tests and for the APKs. If `:content`'s generated Java resources are not on `:play`'s unit-test classpath, stop and hand back `recirculate → planner` with what you found. Never copy the puzzles.
  - **The seams.**
    - S-1…S-9 exactly, in the files the row names.
    - Public: `PlayController`, `Dragged`, `Pulse`, `Shake`, `PlayEvent` and `PlayLayout` (`:app` uses them). Everything else is `internal`.
    - The reads are Compose snapshot state (the Compose runtime comes with `compose-ui`) and are readable from a JVM test.
    - No `android.*` call; no geometry in puzzle units (O-01). Every position, preview, turn or mirror in place and solved check comes from the engine; S-7's screen angle is input math, not geometry.
  - **REQ-021 A1 by construction.** `drop()` uses the same search result that `updatePreview()` gives for the current centre, and `preview` is null when `overBoard` is false.
  - **REQ-051.** Only `drop()` sets `pulse`, and only in a mini puzzle (`PuzzleKind.MINI`); `goHome()` never does.
  - **TYPE-006 as locked.** A missed first drop stays In progress; the type006 draft names this reading.
  - **S-9.** A scaffolding test checks the resting turns through `engine.vertices` against TYPE-001's words: triangles long side down and pointing up, the square upright, the parallelogram leaning right. If a value in S-9 is wrong, hand back the correct one; do not quietly differ from the row.
  - **Decisions to hand back as proposed `decisions.md` rows** (control point 12; each with its DI source and the alternative):
    - the phone `PlayLayout` values of S-5: rows, 8 dp gap, 8 dp side margin, 66 dp top bar (DI-2 §2–§3);
    - square cells (REQ-013 "fits in any turn"; DI-2 §3 item 3);
    - the mini tray layout (DI-2 §3 item 2): the rows reflowed, and where the cells sit;
    - a twist exactly on a step boundary;
    - a touch that has crossed the slop stays a drag;
    - the turn and mirror a piece keeps when it goes home (follow DI-1);
    - the S-9 steps.
  - **The placeholder.** Delete `play/src/main/kotlin/app/tangramplaytime/play/Play.kt`. If the guard denies the delete, leave the file and say so.
  - **Your own tests.** Scaffolding tests are welcome. They live under `play/src/test/kotlin/app/tangramplaytime/play/scaffolding/`, start with `// scaffolding` and carry no `REQ-NNN.A<n>` token.
  - **Read-only for you:** the kit (if a kit line looks wrong, name the test in your hand-back); `:contracts`, `:kernel`, `:content` and `:app`. A needed contract change is a `contract-delta`: hand it back, never edit it.
  - **Done-check:** as the TASK-010 row says. REQ-002.A1 goes green and then stays `implemented (unverified)`.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with:
  - the inputs read, with their versions, and the files changed or deleted;
  - the traceability delta;
  - the command run, its exit code, the result XML paths and the test counts for each module;
  - the proposed decision rows.

---

## Brief — TASK-012 · WO-003 · slice-implementer

- **Mission:** Build the static core of the Play screen (S-10) and every compile-level seam the device kit reads (S-11…S-14), so that both device test sets compile and BoardTrayKitTest is green on the dev and the floor image.
  - The screen shows the top bar title, the board, the silhouette as one flat merged path, the locked board pieces, and the tray cells and miniatures. Everything is drawn in px (G-17).
  - The silhouette must show no anti-aliasing seam on the floor image; that is the risk this task carries.
  - It is rendering on a device, so it runs on Fable.
- **Read exactly:**
  1. `tasks.md` § WO-003:
     - the intro and row TASK-012;
     - S-10…S-14: implement them exactly, including every tag and key whose nodes TASK-013 and TASK-014 add later, because the kit compiles against them now;
     - S-1…S-9: the API you call;
     - the tag table, and the touch routing (TASK-013's, but your layout must let it work).
  2. `workorders/WO-003.md` § Constraints & notes: "Seams", "Draw in pixels (G-17)", "Device tests", "Verification channels", and § Checkpoint items (item 1).
  3. Requirements, collection v1.2:
     - your IDs: `Requirements/reqs/REQ-011.md`, `REQ-012.md`, `REQ-013.md`, `REQ-045.md`;
     - rules your layout must make room for: `REQ-014.md` (size on the board), `REQ-018.md` (the badge in the PG cell), `REQ-043.md` (the mark in a cell), `REQ-047.md` (titles);
     - `Requirements/req_types.md`: TYPE-001, TYPE-007 (the phone only this WO).
  4. Contracts: `ILockEngine.kt` (`vertices`, `outlineCorners`) and `Types.kt` (`PuzzleDefinition`, `SolvedPiece`, `PieceId` order, `Turn`, `Point`).
  5. `decisions.md` lines 55–60; line 60 is the palette.
  6. `architecture.md` §2 G-10, G-12, G-17; §3b O-01, O-05, O-06; §5; §6 ADR-005 (system font).
  7. `platform-notes/android.md`, all of it.
  8. Design inputs:
     - **DI-2** `Spec/02-ui-layout.md` §2, §3 and §5: the zones; the algorithm (REQ-013's rules); the visual language (colours = S-12; the 2 dp white piece edge). `Spec/ui/01-phone-play.png` for the look.
     - **DI-3** `Spec/01-gameplay.md` §1–§2.
     - **DI-1** `tools/prototype_template.html`: behaviour reference only. Its `layout()` board fit is 0.82 / 0.80 (`design-inputs.md` §2 "Also fine").
     - `design-inputs.md` §2 items 5, 6 and 9, and "Also fine".
  9. The visible acceptance tests (TASK-011), read-only:
     - `play/src/androidTest/kotlin/app/tangramplaytime/play/BoardTrayKitTest.kt` and `KitScreen.kt` (your IDs);
     - the other five play kit files and `app/src/androidTest/kotlin/app/tangramplaytime/app/AppLaunchKitTest.kt`, to see every tag and key they compile against.
  10. Existing files: `play/build.gradle.kts`, `gradle/libs.versions.toml`, and TASK-010's landed files (call them; do not change their seams).
- **Do not read:** the other briefs; `kernel/src/main/` and `content/src/main/` bodies; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, read on the device through the androidTest dependency on `:content`; `LockEngine()` from `:kernel`.
- **Run:**
  - The device-kit compile line.
  - The dev image: `:play:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.play.BoardTrayKitTest`.
  - Switch to the floor image (channel table), run the same line, then switch back to the dev image and leave it running.
  - The JVM line.
- **Done means:**
  - **Build file** (`play/build.gradle.kts`, all from the catalog, so no D5 record):
    - `implementation(libs.androidx.compose.foundation)`;
    - `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` in `defaultConfig`;
    - `androidTestImplementation` of the Compose BOM platform, `compose-ui-test-junit4`, the AndroidX test runner, rules and ext-junit;
    - `debugImplementation(libs.androidx.compose.ui.test.manifest)`;
    - `androidTestImplementation(project(":kernel"))` and `androidTestImplementation(project(":content"))`. Main code still depends on `:contracts` only (G-12).
    - No Material library (D2/D5).
  - **The silhouette.**
    - The solution polygons are merged into one path in px (a path union, or one path with non-zero winding) and filled once with `PlayPalette.silhouette`. There is no inner line and no slot outline (REQ-011).
    - Put it in `SilhouettePath.kt`, because TASK-014 clips the picture to the same path.
    - Prove it with REQ-011.A1 on the floor image.
  - **Px only.** Every board and tray path is built from `engine.vertices` × the current scale, in px. Nothing is drawn under a canvas `scale()` or a `graphicsLayer` scale (G-17).
  - **Layout.**
    - The top bar is `layout.topBarDp` high and shows the title from S-11.
    - The board lies between the top bar and the tray, and the silhouette fits it with about 10 % padding (REQ-035 belongs to WO-006; follow DI-1's fit and record it).
    - The tray follows S-6 with the screen's own size.
    - Set `controller.boardDpPerUnit` at every layout.
  - **Nodes.** Every node you draw carries the tag table's tag, bounds and keys. Tagged display containers merge their descendants and stay inert (platform-notes). `PlayTags` and `PlaySemantics` declare every row, including those TASK-013 and TASK-014 will draw.
  - **Strings and colours.** No user-visible string literal (G-10, O-06): the title comes from the puzzle, the letters from S-11. No colour literal outside `PlayPalette` (S-12).
  - **Decisions to hand back as proposed `decisions.md` rows:**
    - the board fit (padding);
    - the 2 dp white piece edge and the tray miniature edge (DI-2 §5);
    - how cells sit in the tray strip;
    - the merged-path technique you used, and the floor evidence.
  - **Done-check:** as the TASK-012 row says. The JVM set stays green.
  - **Read-only for you:** the kit, TASK-010's seams, `:contracts` and `:app`. If a seam row is ambiguous, stop and hand back `recirculate → planner`.
  - **Your own tests.** Scaffolding device or JVM tests go under a `scaffolding/` subfolder, start with `// scaffolding` and carry no token.
- **Return:** the handoff block, with:
  - the inputs read and the files changed;
  - the commands, their exit codes and the result XML paths, for the dev and the floor run (test counts once);
  - the proposed decision rows.

---

## Brief — TASK-013 · WO-003 · slice-implementer

- **Mission:** Make every touch work, as § WO-003's touch routing says:
  - the one pointer handler on `play-screen`, on raw pointer events;
  - tap or drag (S-8), the touch areas, and the second-finger twist (S-7);
  - the dragged piece: it grows within 120 ms and floats 30 dp above the finger;
  - the landing preview: `updatePreview` at most once per displayed frame, a white dashed 2.5 dp outline;
  - the lock glide and the go-home glide (180 ms each), the shake of a refused board turn, and a pointer cancel → `goHome()`.
  It is gesture work on a device, so it runs on Fable.
- **Read exactly:**
  1. `tasks.md` § WO-003: the intro, row TASK-013, § Seams (S-1…S-3, S-7, S-8, S-10, S-13, S-14), the tag table, and the touch routing (yours; TASK-014 adds the badge branch).
  2. `workorders/WO-003.md` § Constraints & notes: "Gestures", "Timings", "Draw in pixels (G-17)", "Device tests".
  3. Requirements, collection v1.2:
     - `Requirements/reqs/REQ-012.md`, `REQ-013.md`, `REQ-014.md`, `REQ-015.md`, `REQ-016.md`, `REQ-017.md`, `REQ-019.md` (the rules a drop follows), `REQ-020.md`, `REQ-021.md`;
     - `Requirements/req_types.md`: TYPE-003, TYPE-004.
  4. Pending drafts:
     - `capture-outbox/chg-draft-req014-a1-without-delay.md` (the one-frame bound the kit tests);
     - `chg-draft-req017-second-finger-never-a-tap.md`, `chg-draft-req018-badge-touch-precedence.md` (keep the hit order in one function, so that TASK-014 can put the badge first) and `chg-draft-req020-interrupted-drag.md`.
  5. Contracts: `ILockEngine.kt` (`vertices`, `findLock`, `tryTurnInPlace`) and `Types.kt`.
  6. `decisions.md` lines 58 and 59.
  7. `architecture.md` §2 G-04, G-11 (no haptics in `:play`), G-12, G-17; §3b O-01, O-07.
  8. `platform-notes/android.md`, all of it.
  9. Design inputs:
     - **DI-3** `Spec/01-gameplay.md` §3–§4: the interaction table;
     - **DI-2** `Spec/02-ui-layout.md` §4: the piece touch area +12 dp;
     - **DI-1** `tools/prototype_template.html`: behaviour reference only (how the drag, the twist, the preview and go-home feel), never DoD.
  10. The visible acceptance tests (TASK-011), read-only: `play/src/androidTest/kotlin/app/tangramplaytime/play/DragKitTest.kt`, `PreviewKitTest.kt` and `KitScreen.kt`.
  11. The landed files of TASK-010 and TASK-012 that you call or extend: `PlayController.kt`, `TouchClassifier.kt`, `TwistTracker.kt`, `PlayScreen.kt`, `PlayTags.kt`, `PlaySemantics.kt`, `PlayPalette.kt`, and the drawing helpers TASK-012 left.
- **Do not read:** the other briefs; `kernel/src/main/` and `content/src/main/` bodies; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json` on the device, as TASK-012.
- **Run:**
  - The dev image: `:play:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.play.DragKitTest,app.tangramplaytime.play.PreviewKitTest,app.tangramplaytime.play.BoardTrayKitTest`.
  - The JVM line.
- **Done means:**
  - **Raw events.** Classify tap or drag on raw pointer events (`awaitPointerEventScope` / `awaitEachGesture`) with S-8. Do not use `detectTapGestures` or `detectDragGestures`: their long-press timeout breaks REQ-015.
  - **Touch areas.**
    - A tray piece's touch area is its whole cell.
    - A placed piece's touch area is its drawn shape grown by 12 dp, hit-tested in px on the drawn polygon (`HitTest.kt`). It is a screen hit test, never a lock decision: every position still comes from the engine (O-01). The orchestrator records the O-01 reading.
  - **During a drag** every further touch is a twist finger (S-7 → `twistDrag`) and acts on nothing else. Lifting it keeps the turn.
  - **Release and cancel.** A release calls `moveDrag` with `overBoard` = the finger is over the board, then `drop()`. A pointer cancel calls `goHome()`; tell a cancel apart from a release.
  - **The preview.** Call `updatePreview()` from one frame-driven loop (`withFrameNanos`) while a drag runs. Draw `controller.preview` only, in white, dashed, 2.5 dp (REQ-021 ASSUMPTION). Nothing else calls `findLock`.
  - **The dragged piece.**
    - It grows from the miniature to board size within 120 ms, with its path rebuilt in px at each frame's scale (G-17).
    - Its lowest point floats 30 dp above the finger (`layout.floatOffsetDp`).
    - Its node keys follow what is drawn in each frame (the tag table).
  - **Glides and the shake.**
    - The lock glide and the go-home glide take 180 ms each (REQ-019, REQ-020).
    - A refused board turn (`shake`) shakes the piece once (REQ-016).
    - Nothing appears on a missed drop: no message, no toast, no text (REQ-020).
  - **Decisions to hand back as proposed `decisions.md` rows:**
    - the shake (amplitude and duration);
    - the over-the-board rule for `overBoard` (by the finger);
    - the hit order in detail (inside beats the margin; the topmost piece);
    - the frame-loop mechanism for the preview;
    - the O-01 reading of the px hit test, for the orchestrator to record.
  - **Done-check:** as the TASK-013 row says. REQ-014.A1 goes green and then stays `implemented (unverified)`.
  - **Read-only for you:** the kit, the S-1…S-9 seams and `:contracts`. If a seam is ambiguous, stop and hand back `recirculate → planner`.
  - **Your own tests:** scaffolding only, as in TASK-012.
- **Return:** the handoff block, with the inputs read, the files changed, the commands with their exit codes, the result XML paths, the test counts and the proposed decision rows.

---

## Brief — TASK-014 · WO-003 · slice-implementer

- **Mission:** Add the decorations and the solving moment.
  - In the tray: the ghost outlines and the S/M/L size marks.
  - The flip badge, in the tray and next to the placed parallelogram, including its branch in the touch routing.
  - The corner pulse of a mini puzzle.
  - The solve: a size-neutral pop; confetti for 2.4 s; the picture fading in over 800 ms from 600 ms after the last lock, drawn from `ArtLayer` and clipped to the silhouette path; and reduced motion.
  It is rendering and animation on a device, so it runs on Fable.
- **Read exactly:**
  1. `tasks.md` § WO-003: the intro, row TASK-014, § Seams (S-1…S-3, S-10…S-14), the tag table, and the touch routing (the badge branch is yours).
  2. `workorders/WO-003.md`: § Constraints & notes "Seams", "Draw in pixels (G-17)", "Timings"; § Checkpoint items (item 1).
  3. Requirements, collection v1.2:
     - your IDs: `Requirements/reqs/REQ-018.md`, `REQ-023.md`, `REQ-039.md`, `REQ-043.md`, `REQ-045.md`, `REQ-051.md`;
     - rules: `REQ-012.md` (the dashed outline in the cell), `REQ-013.md` (what a cell holds), `REQ-014.md` (A2: a piece is never drawn larger or smaller than its board size, so **the pop must not change a piece's size**), `REQ-037.md` (60 dp), `REQ-047.md` (the same letters in both languages);
     - `Requirements/req_types.md`: TYPE-001, TYPE-003.
  4. Pending draft: `capture-outbox/chg-draft-req018-badge-touch-precedence.md`.
  5. Contracts: `Types.kt`, every KDoc line of `ArtLayer`, `ArtShape`, `ArtStyle` and `PuzzleDefinition.art`; `ILockEngine.kt` (`tryMirrorInPlace`, `outlineCorners`).
  6. `decisions.md` lines 40, 49 (the `path.d` grammar you draw), 57, 59 and 60.
  7. `architecture.md` §2 G-10, G-16 (no asset; system font), G-17; §3b O-06, O-07.
  8. `platform-notes/android.md`, all of it.
  9. Design inputs:
     - **DI-2** `Spec/02-ui-layout.md`: §3 item 4b (marks top-left, badge top-right); §4 (badge 38 dp visible, 60 dp touch); §5 (the picture style; the thin silhouette outline must stay inside the clip); §6 (reduced motion).
     - **DI-3** `Spec/01-gameplay.md` §7: the solving moment.
     - **DI-1** `tools/prototype_template.html`: behaviour reference only (the solve, the pulse), never DoD.
  10. The visible acceptance tests (TASK-011), read-only: `play/src/androidTest/kotlin/app/tangramplaytime/play/DecorationKitTest.kt`, `CornerPulseKitTest.kt`, `SolvedPictureKitTest.kt` and `KitScreen.kt`.
  11. The landed files of TASK-010, TASK-012 and TASK-013 that you call or extend, among them `PlayController.kt`, `PlayScreen.kt`, `SilhouettePath.kt` and `PointerRouter.kt`.
- **Do not read:** the other briefs; `kernel/src/main/` and `content/src/main/` bodies; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json` on the device, as TASK-012.
- **Run:**
  - The dev image: first your three classes, then the whole play set (`:play:connectedDebugAndroidTest`).
  - Switch to the floor image (channel table), run the whole play set, then switch back to the dev image and leave it running.
  - The JVM line.
- **Done means:**
  - **The badge.**
    - `flip-badge-tray` sits at the top-right of the PG cell; `flip-badge-board` sits next to the placed PG. Each has a 60 dp touch area (REQ-018 rule) and a 38 dp visible disc.
    - The ⇋ glyph is drawn as a path: no text and no icon library.
    - In the router, the badge area is the first hit. A touch that starts on a badge never drags. A press calls `pressBadge()`, and a refused mirror shakes.
    - There is no badge while the PG is dragged, and none after the solve.
  - **The marks and the ghost.**
    - Size marks only on triangles in the tray, using the S-11 letters (no string literal, G-10), top-left of the cell, tinted with the piece's colour.
    - A dashed ghost outline in the cell while the piece is placed or dragged.
    - Neither changes a cell's size or the tray scale (REQ-043 rule).
  - **The pulse.** It follows `controller.pulse`: small circles in `PlayPalette.accent` at exactly `Pulse.corners`, about 600 ms per serial. With reduced motion, it is shown for 600 ms without animation (REQ-051 rule).
  - **The solve.**
    - The picture is drawn in px from `puzzle.art`: the base fill, then the shapes in order, with every coordinate and width converted through the board mapping, no canvas scale (G-17).
    - It is clipped to `SilhouettePath`, the same path TASK-012 fills, so nothing paints outside the outline (REQ-039, REQ-023). Anything else you draw with the picture (an outline, say) stays inside the clip.
    - The fade runs over 800 ms, starting 600 ms after the last lock. Once it is complete, the board pieces are no longer drawn (REQ-023 "replace").
    - Confetti lasts 2.4 s and carries the `confetti` tag.
    - Reduced motion (the `reducedMotion` parameter): no pop and no confetti, and the picture still appears.
  - **Decisions to hand back as proposed `decisions.md` rows:**
    - the badge's 38 dp visible size (DI-2 §4), the board badge's position, and the rule that a badge touch never drags;
    - the ghost's dash;
    - the mark's size, position and tint;
    - the marker radius ("a small circle");
    - the form of the pop (size-neutral, because of REQ-014.A2);
    - the confetti's look;
    - whether the picture fades or appears at once under reduced motion;
    - the outline drawn with the picture, if any.
  - **Done-check:** as the TASK-014 row says, including the whole play device set on the floor image.
  - **Read-only for you:** the kit, the S-1…S-14 seams and `:contracts`. If a seam is ambiguous, stop and hand back `recirculate → planner`.
  - **Your own tests:** scaffolding only, as in TASK-012.
- **Return:** the handoff block, with the inputs read, the files changed, the commands with their exit codes and result XML paths for the dev and the floor run, the test counts once, and the proposed decision rows.

---

## Brief — TASK-015 · WO-003 · slice-implementer

- **Mission:** Wire the Play screen into the app (S-15), and deliver the release-safety channel and device hygiene (S-16). This is mechanical work with one known trap in the verifier, so it runs on Opus.
- **Read exactly:**
  1. `tasks.md` § WO-003: the intro, row TASK-015, § Seams S-5, S-10, S-11, S-15 and S-16.
  2. `workorders/WO-003.md` § Constraints & notes: "Seams", "Timings" (reduced motion), "Device tests", "The release verifier", "Verification channels".
  3. Requirements, collection v1.2: `Requirements/reqs/REQ-045.md` (A1); `REQ-043.md` (the letters, the same in both languages); `REQ-047.md` (the title-language rule).
  4. Pending drafts:
     - `capture-outbox/chg-draft-req046-android-build-terms.md`, which gives the reading of "release build";
     - `chg-draft-req020-interrupted-drag.md` (`onStop`);
     - `chg-draft-req047-english-contract.md` (English is the Contract; `fi` is a reference translation).
  5. `decisions.md` lines 21 (the API 26 floor), 57 and 59.
  6. `architecture.md` §2 G-01, G-08, G-10, G-11, G-12, G-14; §3b O-05, O-06, O-07, O-08; §5 (application id `app.tangramplaytime`; the shell).
  7. `platform-notes/android.md`, all of it: the release-safety duty, the positive control and device hygiene.
  8. Framework: `C:/GitHub/AI/SWDev/framework/skills/verifiers/SKILL.md` (the manifest format and the budget) and `C:/GitHub/AI/SWDev/framework/guardrails/guardrail-authoring.md` § Verifiers.
  9. The visible acceptance test (TASK-011), read-only: `app/src/androidTest/kotlin/app/tangramplaytime/app/AppLaunchKitTest.kt`.
  10. Existing files:
      - `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/app/tangramplaytime/app/MainActivity.kt`;
      - `app/src/main/res/values/strings.xml` and `app/src/main/res/values-fi/strings.xml`;
      - `app/src/androidTest/kotlin/app/tangramplaytime/app/FloorChannelTest.kt` (yours to adapt);
      - `content/src/main/kotlin/app/tangramplaytime/content/PuzzleLibraries.kt`, `kernel/src/main/kotlin/app/tangramplaytime/kernel/LockEngine.kt` lines 1–40;
      - the public `:play` files of S-1, S-4, S-5, S-10 and S-11.
- **Do not read:** the other briefs; `:play` bodies beyond the public seams; `kernel/src/main/` and `content/src/main/` bodies beyond the doors; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, read on the device through the app's own `:content`; `play/src/debug/kotlin/app/tangramplaytime/play/DebugCanary.kt`, which you produce.
- **Run:**
  - `bash tools/device_reset.sh`. Then the dev image: `:app:connectedDebugAndroidTest`. Then `bash tools/device_reset.sh --check`.
  - The release-verifier line of the channel table: the builds, the verifier, its positive control and the runner with `--wo WO-003`.
  - The JVM line.
- **Done means:**
  - **The wiring (S-15).**
    - `AppGraph.kt`: `LockEngine()`, `PuzzleLibraries.bundled(engine)` and the `PlayController`, each built once per process.
    - `PlayLayouts.kt`: the S-5 phone value.
    - `MainActivity` shows `PlayScreen` inside the existing `enableEdgeToEdge` / `safeDrawingPadding` box (insets stay WO-006's).
    - Texts: new `size_mark_small`, `size_mark_medium` and `size_mark_large` strings in `values/` and `values-fi/` ("S", "M", "L"). The title language is `fi` when the device language is Finnish, else `en`.
    - `reducedMotion` is true when `Settings.Global.ANIMATOR_DURATION_SCALE` is 0. Reading it needs no permission.
    - `onEvent` is a no-op this WO (WO-007 plays sounds and the haptic).
    - `onStop` calls `AppGraph.controller.goHome()` while a drag is running.
  - **`FloorChannelTest`.** Adapt it to assert that the `play-screen` node is displayed. It stays scaffolding, with no token.
  - **The canary.** `play/src/debug/kotlin/app/tangramplaytime/play/DebugCanary.kt` holds one distinctive constant string, beside which WO-005's DEV aid will live (O-08). Nothing in `src/main` references it.
  - **The verifier.** `.swdev/verifiers/v01_release_apk.py`: at most 150 lines, stdlib only. It builds nothing; it finds the SDK's `build-tools/36.0.0` through `ANDROID_HOME` or `local.properties`.
    - Default mode, on `app/build/outputs/apk/release/app-release-unsigned.apk`:
      1. `aapt2 dump permissions` (or `badging`) shows no `uses-permission`.
      2. No `classes*.dex` contains the canary string.
      3. No `Lokhttp3/` class exists, and no class under `Lapp/tangramplaytime/` references `Ljava/net/` (use `dexdump -d` and attribute each reference to its class).
      - **The trap:** kotlin-stdlib's `kotlin.io` references `java.net.URL`, so a whole-dex string scan is a false RED. G-01 says "in product code".
    - `--positive-control`, on `app/build/outputs/apk/debug/app-debug.apk`: the canary must be found. Exit 0 only when it is seen.
    - One PASS/FAIL line per check.
  - **The manifest.** `.swdev/verifiers/manifest.json`: `budget_per_wo` {count 1, lines 150} and one entry `V-01` (`wo` WO-003, guardrail G-01 and G-08, channel toolchain, `cmd`, `positive_control`, a timeout), in the SKILL's format.
  - **The reset script.** `tools/device_reset.sh` runs `adb shell pm clear app.tangramplaytime`; with `--check`, it reports the app's data clean or not and exits non-zero when not. It finds `adb` through `ANDROID_HOME` or the PATH, and treats "not installed" as clean.
  - **No new dependency.** If the guard denies a write under `.swdev/` or `tools/`, stop and say so.
  - **Hand back for `review-notes.md`** (the orchestrator writes it): the new strings with their `en` and `fi` values.
  - **Done-check:** as the TASK-015 row says.
  - **Read-only for you:** the kit and the `:play` seams. If something is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with:
  - the inputs read and the files changed;
  - the commands, their exit codes, the result XML paths and the test counts;
  - the verifier's PASS lines, the positive control's line, and the runner's GREEN line with the budget;
  - the reset check's output; the strings list.

---

## Handoff — Work-Order Planner · WO-003

- **Scope:** the 32 acceptance IDs of `workorders/WO-003.md` § Scope (REQ-002 A1; REQ-011 A1–A2; REQ-012 A1–A2; REQ-013 A1–A3; REQ-043 A1–A2; REQ-014 A1–A2; REQ-015 A1–A2; REQ-016 A1–A2; REQ-017 A1–A2; REQ-018 A1–A3; REQ-021 A1–A2; REQ-051 A1–A2; REQ-023 A1–A2; REQ-020 A1–A2; REQ-039 A1; REQ-045 A1–A2). Governed interfaces: `ILockEngine`, `IPuzzleLibrary`, `Types.kt` (all consumed, unchanged).
- **Inputs read:**
  - `workorders/WO-003.md` (log to 16:40); `tasks.md` v0.6; `workorders/WO-002-briefs.md`;
  - REQ-002, 011–023, 037, 039, 043, 045, 047, 051 and TYPE-001/003/004/006/007 (collection v1.2);
  - the three contract files (baseline 2026-10-11);
  - `architecture.md` v1.0; `build-map.md` v1.3; `design-inputs.md` v1.1; DI-2 and DI-3 §1–§4, §7; `Spec/03-puzzle-format.md` §3;
  - `platform-notes/android.md`; the run log `.swdev/run-log/WO-003.md`; `decisions.md` lines 21–60; the five pending drafts on this WO's routing and lines;
  - the `play/` and `app/` tree and the in-tree kits;
  - SWDev `task-decomposition.md` v0.5, `templates/tasks.md`, the plan-check SKILL and script, `traceability-rules.md` §2 and §5, the verifiers SKILL.
- **Result:** `tasks.md` § WO-003 (change log 0.7) and this file.
- **Status:** forward → Plan Reviewer.
- **Traceability delta:** 32 IDs → 7 tasks. The tokens are planned in 6 kit test files under `play/` and 1 under `app/`; no token on REQ-002.A2 or REQ-039.A2.
