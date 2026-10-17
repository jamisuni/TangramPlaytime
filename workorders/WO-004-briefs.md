# WO-004 — dispatch briefs (#Browsing + `:store`)

**Planner:** wo-planner (Opus 5.5)  ·  **Date:** 2026-10-15  ·  **Plan:** `tasks.md` § WO-004 (TASK-016 … TASK-022; TASK-017 ∥ TASK-018 are `[P]`)
**Mode:** Full (c), a new persistence format (`workorders/WO-004.md` § Mode). The seam table is `designs/WO-004-design.md` §2 (Reviewed). Each finding of `reviews/WO-004-design-review.md` is governed by its **Triage:** line; F5(b) is reversed, so there is no build-map row.

> Each brief is dispatched on its own. An Acceptance Test Author gets only its own brief plus the two tables below; it never sees an implementer brief. An implementer never sees another implementer's brief or a kit draft outside the tree.
> Every path is relative to `C:/GitHub/AI/TangramPlaytime/` unless it is absolute. Folder names used below:
> - "the store kit folder": `store/src/test/kotlin/app/tangramplaytime/store/`;
> - "the JVM kit folder": `play/src/test/kotlin/app/tangramplaytime/play/`;
> - "the play device kit folder": `play/src/androidTest/kotlin/app/tangramplaytime/play/`;
> - "the app device kit folder": `app/src/androidTest/kotlin/app/tangramplaytime/app/`.
>
> `decisions.md` rows are cited by date and bold title. Line numbers are given as of 2026-10-15 19:37; rows are only ever appended.

## Dispatch order (two lanes)

| Step | Test lane (acceptance-test-author) | Implementer lane (slice-implementer) | Gate to the next step |
|---|---|---|---|
| 1 | TASK-016 drafts and **lands** the JVM kit (the store kit folder and the JVM kit folder). TASK-019 starts drafting the device kit in its scratchpad. | — | TASK-016's Done-check is met at landing |
| 2 | TASK-019 keeps drafting. | TASK-017 ∥ TASK-018: two contexts in parallel, each reading the landed kit | Both Done-checks are green. This is the JVM kit's green verdict. |
| 3 | TASK-019 **lands** (resume the same author context with "TASK-017 and TASK-018 done"). | — | TASK-019's Done-check is met at landing |
| 4 | — (the play device set does not compile until TASK-020 lands) | TASK-020 | TopBarKitTest, ResumeScreenKitTest and SolvedViewKitTest are green on the dev image, and the six WO-003 classes stay green |
| 5 | — | TASK-021 | OverviewKitTest is green, then the whole play device set on the dev **and** the floor image |
| 6 | — | TASK-022 | The app set is green on both images; the death pair and its positive control pass on both; V-01 passes |
| 7 | Run `plan_check.py --wo WO-004` again now that the kit has compiled (WO DoD) → Code Reviewer → Test & Verify (every set, every channel) | | |

- **Why this order.**
  - The design fixed every name the kits call. The JVM kit can therefore be written before any product code, and it lands first so that TASK-017 and TASK-018 work against the visible acceptance tests for their IDs.
  - TASK-019 lands after TASK-017 and TASK-018, so that any seam they had to recirculate is settled before the device kit lands.
  - Every compile-level seam of the play device kit is TASK-020's (it declares every S-13Δ tag, including the overview's), so the set compiles once TASK-020 lands and TASK-021 only turns OverviewKitTest green.
- **Dependencies versus dispatch.** Drafting the device kit during steps 1–2 touches nothing in the tree, so it is dispatch. TASK-017 and TASK-018 are dispatched only once TASK-016 has landed. Their rows say "—" in Depends-on, because plan-check reads a Depends-on on a `[P]` row as RED (`tasks.md` § WO-004 intro).
- **The `[P]` pair shares one working tree.** Each runs Gradle only for its own module (`:store` or `:play`). A second Gradle build in the same project can wait on, or fail over, a lock held by the other. If that happens, retry once the other build has finished: it is the `[P]` partner, not a defect. The orchestrator may instead dispatch the pair with `isolation: worktree`.
- **Red windows** (`tasks.md` § WO-004 intro, per source set). Nobody runs a set for a verdict inside its window.
  - `store/src/test`: from step 1 until TASK-017 lands.
  - `play/src/test`: from step 1 until TASK-018 lands.
  - `play/src/androidTest`: does not compile from step 3 until TASK-020 lands. OverviewKitTest then fails at run time until TASK-021.
  - `app/src/androidTest`: does not compile from step 3 until TASK-022 lands.
- **Compile errors in the kit.** An unresolved reference to a seam of a task that has not landed yet is expected; each kit row lists which. Any other compile error in a kit file is a test defect. It goes back to that kit's author and counts as a corrected check. An implementer never edits a kit file.
- **In-tree tests this WO changes:** see `tasks.md` § WO-004 intro, "In-tree tests this WO changes, and who changes each". Scaffolding tests belong to the implementers.
- **Decisions the implementers record.** A number or reading that no REQ, seam row or `decisions.md` row fixes goes back as a proposed `decisions.md` row, with its DI source and the alternative. The orchestrator appends it, so each is recorded once.
- **Checkpoint items** (`workorders/WO-004.md` § Checkpoint items, defaults standing 2026-10-18). The build follows the defaults. Kit pins on them cite the rows named there, so an owner answer changes a constant or a branch and the pin that cites it.

## Verification channels (G3)

Each "the line" below is `cd /c/GitHub/AI/TangramPlaytime && export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' && cmd.exe //c ".\gradlew.bat <tasks>"`. `ADB` is `/c/Users/Jami/AppData/Local/Android/Sdk/platform-tools/adb`; `EMU` is `/c/Users/Jami/AppData/Local/Android/Sdk/emulator/emulator`.

| Channel | How (Git Bash) | Result | Needed by |
|---|---|---|---|
| JVM (JUnit 4) | the line with `:store:testDebugUnitTest :play:testDebugUnitTest :content:testDebugUnitTest :contracts:test`. Inside the `[P]` step, each task runs only its own module plus `:contracts:test`. | `<module>/build/test-results/testDebugUnitTest/TEST-*.xml`, `contracts/build/test-results/test/TEST-*.xml` | TASK-016 (compile status), TASK-017, TASK-018; every later task keeps it green |
| Kit compile | the line with `:play:assembleDebugAndroidTest :app:assembleDebugAndroidTest` (plus `:app:assembleDebug` at TASK-020) | the exit code and the compiler messages | TASK-019, TASK-020, TASK-022 |
| Dev image `Medium_Phone_API_37.0`, running as `emulator-5554` | `$ADB devices` first. Then the line with `:play:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.play.TopBarKitTest`, **one class per invocation**: through `cmd.exe //c` a comma list runs only its first class (`platform-notes/android.md` § Compose test kit). Omit the argument to run a whole set. `:app:connectedDebugAndroidTest` works the same way. The XML folder is overwritten on every run, so copy each XML you cite to your scratchpad. | `play/build/outputs/androidTest-results/connected/debug/TEST-*.xml`, `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` | TASK-018 (informational probe), TASK-020, TASK-021, TASK-022 |
| Floor image `Phone_API_26` (`minSdk 26`), one emulator at a time | `$ADB emu kill`, then `$ADB devices` until it is empty. Run `$EMU -avd Phone_API_26 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot` in the background, then `$ADB wait-for-device`, then poll `$ADB shell getprop sys.boot_completed` until it reads `1`, then `$ADB devices`. Run the same lines. Afterwards kill it, boot `Medium_Phone_API_37.0` the same way, and leave it running. | the same XML folders (the file name carries the device) | TASK-021 (the whole play set), TASK-022 (the app set and the death pair), Test & Verify |
| Process death (S-24) | the line with `:app:assembleDebug :app:assembleDebugAndroidTest :app:packageDebug --rerun :app:packageDebugAndroidTest --rerun` *(amended 2026-10-17, code review S2: the script refuses, exit 2, APKs older than any source or build input, and Gradle leaves an up-to-date APK unwritten, so the two package tasks are forced)*, then `bash tools/process_death_check.sh`, `bash tools/process_death_check.sh --positive-control` and `bash tools/process_death_check.sh --negative-run`, on each image | one verdict line (with both APK times) and an exit code each | TASK-022, the close |
| Release verifier V-01 (G-01, G-08) | the line with `:app:assembleRelease :app:assembleDebug`, then `python .swdev/verifiers/v01_release_apk.py`, `python .swdev/verifiers/v01_release_apk.py --positive-control` and `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-004` | one PASS/FAIL line each, then GREEN/RED and the budget line | TASK-022, the close |
| Device hygiene | `bash tools/device_reset.sh`, then `bash tools/device_reset.sh --check` (clean), **before and after** every device step. From this WO on, the app writes its progress file at its first open, so a `--check` straight after a run that leaves the app installed reports NOT clean by design. The after-step therefore resets first. A non-clean check after a reset voids the step (`platform-notes/android.md` § Device hygiene). | `--check` reports clean or not | every device step |

- **Not a DoD channel:** human eyes. The owner plays the build at close from `review-notes.md` (the top bar, Restart, the solved row, the grid, the Finnish strings).
- **Toolchain proof:** the JVM, floor and dev channels are proven in `.swdev/run-log/WO-004.md`, rows g3, f26 and d37.
- **Time:** WO-004 computes no time value, so there is no `Clock` (G-14; `workorders/WO-004.md` § Scope, out of scope). The long press and the solved row run on clocks a device test drives through the compose rule's `mainClock` (design §3, library fact 5). The app kit waits for the IO load with `waitUntil` on a node (design §3). Nothing sleeps.
- **Browser:** none.

---

## Brief — TASK-016 · WO-004 · acceptance-test-author

- **Mission:** Write the JVM part of WO-004's one acceptance kit:
  - the `:store` row-11 test, in the store kit folder;
  - the controller kit over a kit-owned in-memory `IProgressStore` double, in the JVM kit folder.

  Both are written against the design's seam table and ADR-003. Land it before TASK-017 and TASK-018 start. The class is persistence and concurrency oracles, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md` § WO-004:
     - the intro: lanes, `[P]`, red windows, seam notes 1–4, lines covered twice, in-tree tests;
     - the rows TASK-016 (yours), TASK-017 and TASK-018 (their Done-checks are your green verdict).
  2. `designs/WO-004-design.md`:
     - §1, the ADR-003 block (the on-disk shape your store tests write by hand), "Unreadable state", "A failed write", "Pending drafts";
     - §2, the rows S-17, S-18, S-19, S-1Δ (both), S-2Δ, S-20, S-21 and S-3Δ, and "What survives";
     - §3, every bullet. It names the adversarial paths and the oracles of the save path.

     Do not read §4: the cut is in `tasks.md`.
  3. `workorders/WO-004.md`: § Scope with every scope note; § Constraints & notes ("Seams", "Persistence format = ADR-003", "Time is injected"); § Checkpoint items.
  4. Requirements, collection v1.2, read-only:
     - your tokens: `Requirements/reqs/REQ-003.md`, `REQ-024.md`, `REQ-025.md`, `REQ-026.md`, `REQ-050.md`, `REQ-040.md`, `REQ-041.md`, `REQ-045.md`;
     - rules your fixtures cross, no token: `REQ-019.md` (what `stillValid` keeps) and `REQ-022.md` (the solve);
     - `Requirements/req_types.md`: TYPE-001, TYPE-003, TYPE-004, TYPE-006.
  5. Pending drafts, for context. The build follows the locked text (design §1 "Pending drafts"):
     - `capture-outbox/chg-draft-req025-unreadable-state-and-save-timing.md`;
     - `capture-outbox/chg-draft-type006-missing-transitions.md`;
     - `capture-outbox/chg-draft-req025-drop-profile-storage-rule.md`.
  6. Frozen contracts, baseline 2026-10-11, every KDoc line, all under `contracts/src/main/kotlin/app/tangramplaytime/contracts/`: `IProgressStore.kt`, `Types.kt`, `ILockEngine.kt`, `IPuzzleLibrary.kt`.
  7. `decisions.md`:
     - every 2026-10-15 row (lines 86–97);
     - 2026-10-12 "**One-shot cues are shown once per controller**" (line 80) and "**Carried to WO-004 #Browsing**" (line 79);
     - line 29 (a tray turn or flip is not persisted).
  8. In-tree tests:
     - the WO-003 JVM kit, which is yours to keep green: `KitPlay.kt` (reuse its helpers), `PlayControllerKitTest.kt` (amend its "Opening puzzles: memory per puzzle" pin; `tasks.md` intro), `TrayMetricsKitTest.kt` and `TwistKitTest.kt`, all in the JVM kit folder;
     - not yours, never edit and never reuse a name: `play/src/test/kotlin/app/tangramplaytime/play/PlayScaffoldTest.kt`, the `scaffolding/` folder beside it, and `store/src/test/kotlin/app/tangramplaytime/store/StoreScaffoldTest.kt`.
  9. Build files:
     - `store/build.gradle.kts`: JUnit 4 and `kotlinx-coroutines-core` are on its test classpath. The serialization plugin arrives with TASK-017, and your store tests must not need it: write the JSON as text.
     - `play/build.gradle.kts`.
     - `gradle/libs.versions.toml`: there is no coroutines-test artifact, so drive suspend calls with `runBlocking`.
  10. Rules:
      - `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2;
      - `C:/GitHub/AI/SWDev/framework/methodology/task-decomposition.md`: "Assert only what is mandated", and "Seams name their task; adapters fail loudly";
      - `platform-notes/android.md` § Compose test kit, the WO-003 project lessons (a source for every assertion; quantifier lines);
      - `architecture.md` §2 G-12, G-13, G-15; §3b O-03, O-04.
- **Do not read:**
  - the main code of `:store`, `:play`, `:app`, `:kernel` and `:content` (compiler messages are fine);
  - the implementer briefs (§ TASK-017, § TASK-018 and § TASK-020 … § TASK-022 of this file), and TASK-019's drafts;
  - DI-1 … DI-4: never the prototype. They supply no Contract text.
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, read through `PuzzleLibraries.bundled(LockEngine())` on the play test classpath. The store tests read only files they create in a temporary folder.
- **Oracle rule:**
  - Every assertion cites, in a comment, the sentence it checks: a REQ sentence, a TYPE rule, a KDoc line, a seam row (`S-n`), or a `decisions.md` row by date and title. If it has no source, drop it.
  - A REQ token goes only on a test whose assertion is that line's meaning. A pin carries its source and no token.
  - Cite rules without a dotted ID in comments ("REQ-025 rule", not a dotted ID). A dotted ID appears only where it is a token: trace-check counts every dotted ID it finds.
  - **A line with a quantifier** ("every", "never", "any", "always") **names its adversarial path in the same test** (`platform-notes/android.md`). At least these:
    - REQ-003.A2 across every way of leaving, and across a new `Persistence.load` with a new controller over the same double;
    - REQ-050.A3 checking every other puzzle, not a sample.
  - **Fixtures discriminate.** Show test-side that the state you claim to tell apart exists. For example, for REQ-050.A1 a plain › from the third puzzle opens a different puzzle than the seventh. For REQ-025.A3, show with the engine that the moved piece really crosses the outline and the others are valid locks.
  - The double fails loudly on a miss (`task-decomposition.md`), and you call `internal` members directly, with no reflection.
- **Where the tokens go.** If your judgment adds or moves a token, say so in the hand-back; the orchestrator corrects the cells.
  - `BrowsingKitTest.kt`: REQ-003.A1, REQ-024.A1, REQ-024.A3, REQ-050.A1, REQ-050.A3, REQ-040.A1, REQ-041.A1, REQ-045.A1.
  - `ResumeKitTest.kt`: REQ-003.A2, REQ-025.A1, REQ-025.A2, REQ-025.A3, REQ-026.A1, REQ-026.A2.
  - None in `KitStore.kt` or `FileProgressStoreKitTest.kt` (`tasks.md` intro, "Lines covered twice").
  - Never REQ-024.A2 or REQ-050.A2 here: they are screen lines, TASK-019's. Never a dotted ID outside the WO's 16.
- **What each line needs** (pointers, not restatements; the REQ file is the oracle):
  - **REQ-003.A1:** from every listed puzzle, through S-2Δ alone, with no solve. Include starts that are In progress and Solved.
  - **REQ-003.A2:** every door named in S-2Δ, then return; and the seam-level restart (design §3: one store instance, one writer). The recreation leg and the process-death leg are TASK-019's.
  - **REQ-024.A1 and A3:** S-2Δ `next()` and its wrap.
  - **REQ-025.A1:** three pieces placed through S-2, leave, come back. `placed` must be equal in piece, turn, mirror and centre (the REQ-025 rule "saved per piece"). Repeat over a new controller on the same double.
  - **REQ-025.A2:** seed a time and a best time in the double, as WO-008 will write them, so that "time is zero" can fail (S-20; "**When progress is saved**").
  - **REQ-025.A3:** a seeded In-progress board in which one placement crosses the outline. Pins on S-21's filtering and on the empty-board case cite "**REQ-025's load check**" (checkpoint item 5).
  - **REQ-026.A1 and A2:** S-21 (Solved opens with an empty board), S-20 (what is saved for a Solved puzzle), and "**Browsing readings no REQ fixes**" (what Retry leaves).
  - **REQ-050.A1:** the line's own fixture, the third and seventh puzzles of `library.puzzles` counted from 1. Pin S-2Δ's two `nextUnsolved` edges without a token.
  - **REQ-050.A3:** `choose(id)` against every other puzzle's saved progress and `stateOf`.
  - **REQ-040.A1, REQ-041.A1, REQ-045.A1:** a controller over an empty double (S-1Δ construction), then `next()` along the order. These are extra tokens; the `content/` tokens stand.
  - **Pins, no token:**
    - S-19's single FIFO writer: design §3, "Never `launch` once per save". A double whose writes complete out of step must still end with the latest board of every puzzle.
    - S-20's save rule; S-1Δ's last-opened rule.
    - S-3Δ's `Celebration`.
    - S-2Δ's overview actions, and `choose` of the open puzzle.
    - The O-04 pass-through of times ("**O-04 read for this WO**").
  - **`FileProgressStoreKitTest.kt` (governance row 11):**
    - the oracle list of design §3 "Highest-risk part", the first bullet;
    - S-17's read and write boundary;
    - "**Amends the 2026-10-15 ADR-003 row**", including its valid-JSON corrupt cases;
    - the `IProgressStore` KDoc for every member, including `resetAll`, `playTimes` and `settings`.

    For a failed write, provoke the failure through what S-17's constructor controls, and show test-side that the write really fails on this JVM (Windows). S-18's one-instance pin is not here (seam note 2): it is TASK-019's.
- **Run:**
  1. Write the four new files into their kit folders and amend `PlayControllerKitTest.kt`. Landing is your Done-check, met at landing.
  2. Compile with the JVM line of the channel table. Expected, and not your defect: unresolved references to S-17 and S-18 in `:store`, and to S-19, S-1Δ, S-2Δ and S-3Δ's `Celebration` in `:play`. Any other compile error is yours to fix.
  3. Grep both folders for the dotted token pattern `REQ-0[0-9]*\.A[0-9]` before you hand back. It must show exactly the row's list.
- **Done means:** the TASK-016 row's Done-check, plus hygiene:
  - helper names carry a `kit` prefix;
  - JUnit 4 only; no new dependency;
  - never edit main code, a build file or a scaffolding test.
- **Return:** the handoff block (`C:/GitHub/AI/SWDev/framework/agents/handoff-contract.md`), with:
  - the test names for each acceptance ID, and the list of pins with their sources;
  - the files outside each module that the tests read at run time;
  - the compile status, with the command and its exit code;
  - the number of corrected checks so far.

---

## Brief — TASK-019 · WO-004 · acceptance-test-author

- **Mission:** Write the device part of WO-004's one acceptance kit:
  - four new Compose test classes in the play device kit folder;
  - the amendments the new screen forces on the WO-003 device kit;
  - three app tests in the app device kit folder: the fresh start, the `:app` wiring pins, and the two process-death phases.

  Draft it beside TASK-016 … TASK-018. Land it in one stage when you are resumed with "TASK-017 and TASK-018 done". The class is Compose UI with device-only legs, so it goes to the strongest model.
- **Read exactly:**
  1. `tasks.md`:
     - § WO-004: the intro, where seam notes 1 (the death pair's names), 2 (S-18 on the device) and 4 (zones and texts) and the in-tree list are yours to act on; and the rows TASK-019 … TASK-022 (which of your files goes green at which step);
     - § WO-003: the tag table and the touch routing, which still hold where the design does not change them.
  2. `designs/WO-004-design.md`:
     - §1, "How it works" and "Pending drafts";
     - §2, every row, plus "Touch geometry" and "What survives";
     - §3, every bullet, above all the adversarial paths of REQ-026.A1 and REQ-024.A2 and the death pair's oracle.
  3. `workorders/WO-004.md`: § Scope with every scope note; § Mode; § Constraints & notes; § Checkpoint items.
  4. Requirements, collection v1.2, read-only:
     - your tokens: `Requirements/reqs/REQ-003.md`, `REQ-024.md`, `REQ-025.md`, `REQ-026.md`, `REQ-050.md`, `REQ-040.md`, `REQ-041.md`, `REQ-045.md`;
     - rules only, no new token: `REQ-037.md` (the touch sizes; the ‹ › sweep is WO-006's), `REQ-047.md` (the title language), `REQ-023.md` (the celebration), and `REQ-020.md` and `REQ-051.md` (the two WO-003 tests you amend);
     - `Requirements/req_types.md`: TYPE-001, TYPE-003, TYPE-004, TYPE-006.
  5. Pending drafts: the three named in TASK-016's brief, item 5 (`capture-outbox/`), for context.
  6. Frozen contracts, every KDoc line: `IProgressStore.kt`, `Types.kt`, `ILockEngine.kt`, `IPuzzleLibrary.kt` under `contracts/src/main/kotlin/app/tangramplaytime/contracts/`.
  7. `decisions.md`:
     - every 2026-10-15 row (lines 86–97);
     - 2026-10-12: "**Carried to WO-004 #Browsing**" (79), "**One-shot cues are shown once per controller**" (80), "**Touch readings no REQ fixes**" (72), "**Inside the parallelogram's tray cell the visible thing wins**" (83), "**What 'drawn' means in the touch order**" (84), "**The solve, as drawn**" (75);
     - line 29.
  8. `platform-notes/android.md`, all of it. In particular: § Compose test kit (imports; merged, inert containers; `mainClock`; a solve is never judged by what it replaces; a source for every assertion; quantifier lines and their adversarial paths; geometry from the drawing; one class per invocation); § Device hygiene; the channel facts.
  9. In-tree tests:
     - **every file in the play device kit folder.** You own them now. Before you author anything, grep them for exact tag, text, set and pixel assertions that the new screen touches. `tasks.md` § WO-004 intro, "In-tree tests this WO changes", names the known ones:
       - `DragKitTest.kt` REQ-020.A2 and `CornerPulseKitTest.kt` REQ-051.A2: a New-puzzle snapshot against the first tray drag;
       - `SolvedPictureKitTest.kt`: the In-progress board captures that `thePicturesOutlineIsIdenticalToTheSilhouettes` and `noPicturePaintsOutsideItsPuzzlesOutline` compare after the solve, outside the silhouette (the `restart` pill is drawn In progress and gone once Solved);
       - `DecorationKitTest.kt`: its board and tray captures taken In progress;
       - `BoardTrayKitTest.kt`'s title.

       Look for others too, for example `SolvedPictureKitTest.kt` against the solved row and S-3Δ. Each amendment cites its seam row (S-22 "Restart", S-13Δ, S-3Δ) or `decisions.md` row ("**The top bar, Restart and the solved row, as drawn**") in its comment. No token moves, and no oracle is weakened beyond what the cited row changes.
     - `KitScreen.kt`: its header, and `recreate()` (the recreation leg's tool).
     - in the app device kit folder: `AppLaunchKitTest.kt`, which you amend, and `FloorChannelTest.kt`, which is TASK-022's (never edit it or reuse its name).
     - TASK-016's landed JVM kit: `KitStore.kt` in the JVM kit folder, for the double's approach only (`androidTest` cannot import from `test`).
  10. Build files:
      - `play/build.gradle.kts`: `:kernel` and `:content` are androidTest dependencies; `:store` is not, and you need it nowhere in `:play`.
      - `app/build.gradle.kts`: `:store`, `:play`, `:kernel` and `:content` are on its classpath.
      - `gradle/libs.versions.toml`.
  11. Rules: `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §2; `architecture.md` §2 G-10, G-12, G-13, G-17; §3b O-05, O-06.
- **Do not read:**
  - the main code of `:play`, `:app`, `:store`, `:kernel` and `:content` (compiler messages are fine);
  - the implementer briefs; `tools/process_death_check.sh` once TASK-022 writes it (its interface is seam note 1 and the TASK-022 row);
  - DI-1, the prototype: never. DI-2 … DI-4 supply no Contract text.
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, read on the device through `PuzzleLibraries.bundled(LockEngine())`. In `:play` that is the androidTest dependency; in the app it is the app's own classpath.
- **Oracle rule:** as in TASK-016's brief, with these additions:
  - Cite rules without a dotted ID in comments. A dotted ID appears only where it is a token.
  - A semantics value (`CentreKey`, `PuzzleStateKey`, `RatingKey`) is the product's claim. Where it carries a line's assertion, cross-check it at least once against node bounds or pixels.
  - **The quantifier lines and their adversarial paths, each inside its token's test:**
    - REQ-003.A2 and REQ-025.A1 across an activity recreation (`kit.recreate`). The process-death leg is `ProcessDeathKitTest.phase2`, which carries the same tokens; each names the other in its comment.
    - REQ-024.A2 with the grid open. `isDisplayed` cannot see occlusion (design §3; review F4), so check exactly that the bar lies outside the overview and is inert ("**Amends the 2026-10-15 'grid overview, as drawn' row**").
    - REQ-026.A1 with `recreate()` right after a solve, at the first frame, with the clock paused (design §3). During the solve's own celebration, pieces may be drawn (design §3); do not assert otherwise.
    - REQ-050.A3 against every other puzzle's state, in the double and in every cell's `PuzzleStateKey`.
- **How to drive the screen:**
  - The WO-003 way still holds (`KitScreen.kt` header): touches on `play-screen` in its own px, `mainClock` with `autoAdvance` off, the controller only on the main thread.
  - **A controller with persistence:** `Persistence.load` over your double off the main thread, then the controller on the main thread (S-19, S-1Δ).
  - **The long press:** hold on `nav-next` and advance `mainClock` (design §3, library fact 5); never sleep.
  - **The app's controller is one per process**, shared by every test of one instrumentation run, in any order.
    - An ordinary app test leaves it as the fresh start found it.
    - Only `AppLaunchKitTest` claims the fresh start. Its precondition, a cleared store (`tools/device_reset.sh` before the run), is stated in its comment.
    - It waits for `play-screen` with `waitUntil`, because Compose idling cannot see the IO load (design §3; review F2).
  - **The death phases** (seam note 1; design §2 S-24, §3 "the death pair"):
    - Phase 1 may set the board up through S-2 on the main thread, because the line is about surviving the death, not about the gesture. It uses a puzzle on which three placed pieces do not solve it, then calls `flush()` through `AppGraph.controller` (S-15Δ: the kit's one door).
    - Phase 2 derives its expectation the same way phase 1 chose it, from the bundled library and the engine. Nothing passes between the two processes except the app's own file.
    - Phase 2 must fail on a cleared store, because the positive control depends on that.
- **Where the tokens go.** If your judgment moves a token, say so; the orchestrator corrects the cells.
  - `TopBarKitTest.kt`: REQ-024.A1, REQ-024.A3, REQ-050.A1, REQ-041.A1.
  - `ResumeScreenKitTest.kt`: REQ-003.A2, REQ-025.A1, REQ-025.A2, REQ-025.A3.
  - `SolvedViewKitTest.kt`: REQ-026.A1, REQ-026.A2.
  - `OverviewKitTest.kt`: REQ-050.A2, REQ-050.A3, REQ-003.A1, and REQ-024.A2, because its adversarial leg needs the grid that TASK-021 builds.
  - `AppLaunchKitTest.kt`: REQ-045.A1 (kept) and REQ-040.A1.
  - `ProcessDeathKitTest.kt`: REQ-003.A2 and REQ-025.A1, on `phase2`.
  - None in `KitScreen.kt` or `AppWiringKitTest.kt`. The WO-003 tokens stay where they are.
  - Never REQ-037.A1, REQ-002.A2 or REQ-039.A2 with a dot.
- **What some lines need** (pointers; the REQ file is the oracle):
  - **REQ-024.A2** reads the name and the number on the screen (seam note 4 says where each text lives). Its legs are a wrap, a long press, a recreation and the grid open, as design §3 lists them.
  - **REQ-025.A2 and REQ-026.A2** go by touch on `restart` and `retry`. The time half of REQ-025.A2 goes through your double, as in TASK-016's brief.
  - **REQ-050.A2:** one `overview-cell-<id>` per listed puzzle, carrying its keys (S-14Δ). "Solved ones in colour" is read from pixels against the art colours, and the New cells against the flat silhouette (S-22; REQ-050 rule). It also runs on the floor.
  - **REQ-041.A1:** walk › from a controller over an empty double.
  - **REQ-040.A1:** in the fresh start, the opened puzzle's rating.
  - **Pins, no token**, each citing its row:
    - S-22's hit order and columns;
    - Restart only while In progress (REQ-025 rule);
    - the state word, the solved row's timing and the best-time dash (checkpoint items 2–4);
    - the grid cells' size (REQ-050 rule; "**The grid overview, as drawn**");
    - `play-loading` draws no bar ("**REQ-024.A2 and the launch window**");
    - in `AppWiringKitTest.kt`: S-18's one instance (seam note 2), and back closes the overview (S-15Δ).
- **Run:**
  1. **Now:** draft the files in your scratchpad, never under `play/` or `app/`. Hand back the draft paths.
  2. **When you are resumed with "TASK-017 and TASK-018 done":** write them into the two kit folders. Landing is your Done-check, met at landing.
     - Compile with the kit line. Expected, and not your defect: unresolved references to S-11Δ, S-13Δ, S-14Δ and S-23 in `:play`, and to S-15Δ in `:app`.
     - When TASK-020 or TASK-022 lands, the orchestrator may resume you for any other compile error. Each one counts as a corrected check.
  3. Grep both folders for the dotted token pattern `REQ-0[0-9]*\.A[0-9]` before each hand-back. It must show exactly the row's list.
- **Done means:** the TASK-019 row's Done-check, plus hygiene:
  - helper names are `kit`-prefixed; JUnit 4 with `AndroidJUnit4`, and the `junit4.v2` compose rules, as in the in-tree precedent;
  - no new dependency;
  - never edit main code, a build file or `FloorChannelTest.kt`.
- **Return:** the handoff block, with:
  - the test names for each acceptance ID, and the list of pins with their sources;
  - every in-tree test you amended, with why and the row it cites;
  - which of the six WO-003 device classes you audited and found unaffected;
  - the files read at run time outside each module;
  - the compile status, with the command and its exit code;
  - the number of corrected checks, and any token you moved.

---

## Brief — TASK-017 · WO-004 · slice-implementer

- **Mission:** Build `:store`'s `IProgressStore` implementation and its device door, S-17 and S-18, per ADR-003, so that the row-11 kit test (TASK-016) is green.
  - The class is persistence, so it goes to the strongest model. It runs in parallel with TASK-018, which never reads your code.
  - **Riskiest items first:** the atomic replace (design §3, library fact 1), and the failure boundary on both sides (S-17; review F1).
- **Read exactly:**
  1. `tasks.md` § WO-004: the intro (the `[P]` note, red windows, seam note 2) and row TASK-017.
  2. `designs/WO-004-design.md`:
     - §1, the ADR-003 block through "All members are implemented";
     - §2, rows S-17 and S-18;
     - §3, "Never lose another puzzle's progress", library facts 1–3, and "Highest-risk part".
  3. `reviews/WO-004-design-review.md`:
     - F1 and F3, with their Triage lines;
     - the "Evidence re-derived" item 1, including its caution about `Files` members newer than the floor.
  4. `workorders/WO-004.md` § Constraints & notes: "Seams" and "Persistence format = ADR-003" (governance row 11).
  5. Requirements: `Requirements/reqs/REQ-003.md`, `REQ-025.md` (its rules: surviving a close, and the profile rule that design §1 answers with "No profile key"), and `REQ-010.md`.
  6. Contracts, every KDoc line: `contracts/src/main/kotlin/app/tangramplaytime/contracts/IProgressStore.kt` and `Types.kt` (`PuzzleProgress`, `PlayTimes`, `Settings`, `Placement`, `Turn`).
  7. `decisions.md` 2026-10-15: "**ADR-003, the persistence format of `IProgressStore`**" (86) and "**Amends the 2026-10-15 ADR-003 row**" (94).
  8. `architecture.md` §2 G-01, G-12, G-13, G-15; §3b O-03, O-04; §5 (allowed dependencies; ADR-003).
  9. The visible acceptance test, read-only: `store/src/test/kotlin/app/tangramplaytime/store/FileProgressStoreKitTest.kt`.
  10. Existing files:
      - `store/build.gradle.kts`;
      - `store/src/main/kotlin/app/tangramplaytime/store/Store.kt` (to delete);
      - `store/src/test/kotlin/app/tangramplaytime/store/StoreScaffoldTest.kt` (keep it green);
      - `content/build.gradle.kts` (the precedent for the serialization plugin and the JSON library);
      - `gradle/libs.versions.toml`;
      - `app/src/main/AndroidManifest.xml` (read-only).
  - No design input: ADR-003 fixes the format, and DI-1's save cadence is superseded (`design-inputs.md` §2 item 11).
- **Do not read:** the other briefs; kit drafts outside the tree; the `:play` and `:app` bodies; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** none outside `store/`.
- **Run:**
  - The JVM line with `:store:testDebugUnitTest :contracts:test` only. If it waits on a lock held by another Gradle process, that is TASK-018: retry.
  - Grep `store/src/main` for `android.`. The only match is `ProgressStores` (S-18).
  - Grep `store/src/main` for `java.net`. There must be no match (G-01).
  - List every `java.nio.file` and `java.io` member you call, with its API level. Nothing may be newer than `minSdk 26` (the review's caution). The floor runs this code only at TASK-022.
- **Done means:**
  - S-17 and S-18 exactly as the rows say: the file, the format, the read once, the whole-file replace, the boundary on both sides, one memoised instance per file. `ProgressStores` is public, because `:app` calls it. No DTO or serialization type crosses the seam (the KDoc's must-never).
  - All `IProgressStore` members are implemented, and none is a stub (design §1).
  - The placeholder `Store.kt` is deleted. If the guard denies the delete, leave the file and say so.
  - Scaffolding tests are welcome. They go under `store/src/test/kotlin/app/tangramplaytime/store/scaffolding/`, start with `// scaffolding`, and carry no token.
  - Read-only for you: the kit, `:contracts`, every other module. A needed contract change is a `contract-delta`: hand it back.
  - The Done-check is the TASK-017 row.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with:
  - the inputs read with their versions, and the files changed or deleted;
  - the command, its exit code, the result XML paths and the test counts;
  - the API-level list;
  - any proposed `decisions.md` row.

---

## Brief — TASK-018 · WO-004 · slice-implementer

- **Mission:** Make the controller remember through the store, so that the JVM kit (TASK-016) is green. That means:
  - S-19 `Persistence`;
  - the S-1Δ and S-2Δ controller changes;
  - S-20's save rule and S-21's load check;
  - the controller side of S-3Δ.

  The class is persistence, concurrency and state, so it goes to the strongest model. It runs in parallel with TASK-017: you consume `IProgressStore` only, against the kit's double.
  - **Riskiest item first:** the one ordered writer (design §3, "Never `launch` once per save"; library fact 3).
- **Read exactly:**
  1. `tasks.md`:
     - § WO-004: the intro and row TASK-018;
     - § WO-003: S-1 … S-4, the seams you change (design §3 lists the superseded text).
  2. `designs/WO-004-design.md`:
     - §1, "How it works" and "Pending drafts";
     - §2, rows S-19, S-1Δ (both), S-2Δ, S-20, S-21 and S-3Δ, and "What survives";
     - §3, "Never lose another puzzle's progress", "Never show a solved puzzle's piece positions", "WO-003 seam text superseded", "`stillValid` suffices", "O-04", "Residual risk", library fact 3, and "Highest-risk part".
  3. `workorders/WO-004.md`:
     - § Scope notes: the carried WO-003 items F3 and N1, the carried WO-001 N3, and the behaviours to keep;
     - § Constraints & notes: "Seams" and "Time is injected".
  4. Requirements: `Requirements/reqs/REQ-003.md`, `REQ-024.md`, `REQ-025.md`, `REQ-026.md`, `REQ-050.md`, `REQ-040.md`, `REQ-041.md`, `REQ-045.md`, `REQ-019.md`, `REQ-022.md`; `Requirements/req_types.md` TYPE-006.
  5. Pending drafts: `capture-outbox/chg-draft-req025-unreadable-state-and-save-timing.md` and `capture-outbox/chg-draft-type006-missing-transitions.md`.
  6. Contracts, every KDoc line: `IProgressStore.kt`, `ILockEngine.kt` (`stillValid`), `IPuzzleLibrary.kt`, `Types.kt`.
  7. `decisions.md`:
     - 2026-10-15: "**When progress is saved**" (87), "**REQ-025's load check**" (88), "**Browsing readings no REQ fixes**" (89), "**Startup, and what survives**" (92), "**O-04 read for this WO**" (93), "**Amends the 2026-10-15 ADR-003 row**" (94, one instance and one writer);
     - 2026-10-12: "**Carried to WO-004 #Browsing**" (79), "**One-shot cues are shown once per controller**" (80);
     - line 29.
  8. `architecture.md` §2 G-12, G-13, G-14, G-15; §3b O-03, O-04, O-07; §5 (allowed dependencies).
  9. Design inputs (behaviour reference only, never DoD):
     - **DI-1** `tools/prototype_template.html`: `restart()` and `nextUnsolved()`;
     - **DI-3** `Spec/01-gameplay.md` §6 "Browsing and puzzle states";
     - `design-inputs.md` §2 item 11. DI-1's save cadence is superseded.
  10. The visible acceptance tests, read-only, in the JVM kit folder: `KitStore.kt`, `BrowsingKitTest.kt`, `ResumeKitTest.kt`, the amended `PlayControllerKitTest.kt`, and `KitPlay.kt`.
  11. Existing files:
      - `play/src/main/kotlin/app/tangramplaytime/play/PlayController.kt` (yours to change);
      - `play/build.gradle.kts`;
      - the scaffolding tests under `play/src/test/kotlin/app/tangramplaytime/play/scaffolding/`: keep them green, and update one only if it pins superseded S-1/S-2 text; say so if you do;
      - read-only, they must keep compiling: `app/src/main/kotlin/app/tangramplaytime/app/AppGraph.kt` and `MainActivity.kt`, and `play/src/androidTest/kotlin/app/tangramplaytime/play/KitScreen.kt` (it calls `PlayController(library, engine, onEvent)`).
- **Do not read:**
  - the other briefs, and TASK-019's drafts;
  - `store/src/main`, which TASK-017 is writing in parallel;
  - the bodies of `kernel/src/main` and `content/src/main` beyond the two doors (`PuzzleLibraries.kt`, and the `LockEngine` declaration);
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, read through `:content` on the unit-test classpath, and `LockEngine()` from `:kernel`.
- **Run:**
  - The JVM line with `:play:testDebugUnitTest :contracts:test` only. If it waits on a lock held by another Gradle process, that is TASK-017: retry.
  - Grep `play/src/main` for `app.tangramplaytime.kernel`, `app.tangramplaytime.content` and `app.tangramplaytime.store`. There must be no match (G-12).
  - Grep `play/src/main` for `currentTimeMillis`, `SystemClock`, `nanoTime`, `Thread.sleep` and `delay(`. There must be no match (G-14).
  - **Informational probe, not your Done-check.** Once you are green, with the dev image running:
    1. reset and check;
    2. run `:play:connectedDebugAndroidTest` (the WO-003 device kit still compiles at this step);
    3. copy the XML;
    4. reset and check.

    Report each failing test by name. A failure on superseded seam text (design §3) is a kit matter for TASK-019, not yours.
- **Done means:**
  - S-19, S-1Δ, S-2Δ, S-20, S-21 and the controller side of S-3Δ exactly as the rows say.
    - `Persistence` and the new reads and actions are public where `:app` (TASK-022) or the device kit needs them.
    - S-1Δ's `persistence` parameter has no default, so `PlayController(library, engine)` and the trailing-lambda form still resolve to the secondary constructor. `KitPlay.kt`, `KitScreen.kt` and `AppGraph.kt` use both forms.
  - Every store write goes through `Persistence` (S-19; F3).
  - No screen change: the screen side of S-3Δ is TASK-020's.
  - `kotlinx-coroutines-core` comes from the catalog for main. No catalog change: a needed one is `recirculate → planner`, because it would be a file shared with the `[P]` partner.
  - Scaffolding tests are welcome. They go under the scaffolding folder, start with `// scaffolding`, and carry no token.
  - Read-only for you: the kit, `:contracts`, `:store`, `:kernel`, `:content` and `:app`.
  - The Done-check is the TASK-018 row.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with:
  - the inputs read with their versions, and the files changed;
  - the traceability delta;
  - the command, its exit code, the result XML paths and the test counts;
  - the probe's result;
  - any proposed `decisions.md` row.

---

## Brief — TASK-020 · WO-004 · slice-implementer

- **Mission:** Put the top bar, Restart, the solved row and the long press on the Play screen. That covers:
  - S-11Δ, S-13Δ (every tag), S-14Δ's `RatingKey`;
  - S-22 except the overview;
  - the screen side of S-3Δ, and S-23;
  - the strings that keep `:app` compiling (seam note 3).

  When it is done, the device kit compiles; TopBarKitTest, ResumeScreenKitTest and SolvedViewKitTest are green; and the six WO-003 classes stay green. The class is UI, rendering and touch, so it goes to the strongest model.
  - **Riskiest item first:** the hit order against the existing touch routing (design §2 "Hit order"; WO-003's "visible thing wins" rows).
- **Read exactly:**
  1. `tasks.md`:
     - § WO-004: the intro (seam notes 3 and 4, the in-tree list) and row TASK-020;
     - § WO-003: S-10 … S-14, the tag table and the touch routing (still in force where the design does not change them).
  2. `designs/WO-004-design.md`:
     - §2, rows S-3Δ, S-11Δ, S-13Δ, S-14Δ, S-22 and S-23, plus "Touch geometry" (top bar, Restart, solved row, hit order, taps and the long press) and "What survives";
     - §3, "Never show a solved puzzle's piece positions", "Always show the name and number", "Times are never invented", "WO-003 seam text superseded", and library facts 4–5.
  3. `workorders/WO-004.md`: § Mode (touch geometry, recreation); § Checkpoint items 1–4 (the defaults you build); § Constraints & notes.
  4. Requirements:
     - `Requirements/reqs/REQ-024.md`, `REQ-025.md`, `REQ-026.md`, `REQ-050.md`, `REQ-003.md`, `REQ-041.md`;
     - `REQ-037.md` (touch sizes), `REQ-047.md` (the title language), `REQ-023.md` (the celebration you re-trigger);
     - `Requirements/req_types.md` TYPE-006.
  5. Contracts: `Types.kt`, `IPuzzleLibrary.kt` (titles, rating).
  6. `decisions.md`:
     - 2026-10-15: "**The top bar, Restart and the solved row, as drawn**" (90), "**Browsing readings no REQ fixes**" (89), "**Startup, and what survives**" (92), "**REQ-024.A2 and the launch window**" (96), "**Checkpoint items 1–5 asked**" (97);
     - 2026-10-12: "**One-shot cues are shown once per controller**" (80), "**Carried to WO-004 #Browsing**" (79), "**Touch readings no REQ fixes**" (72), "**Inside the parallelogram's tray cell the visible thing wins**" (83), "**What 'drawn' means in the touch order**" (84), "**The solve, as drawn**" (75).
  7. `platform-notes/android.md`: § Verification channels (the floor blurs scaled paths) and § Compose test kit (merged, inert containers).
  8. `architecture.md` §2 G-10, G-12, G-17; §3b O-05, O-06.
  9. Design inputs (behaviour reference only, never DoD):
     - **DI-1** `tools/prototype_template.html`: the top bar, ‹ ›, the solved view, `restart()`;
     - **DI-2** `Spec/02-ui-layout.md` §2 "Zones" and §4 "Sizes", `Spec/ui/01-phone-play.png`, `Spec/ui/04-browse-states.png`;
     - **DI-3** `Spec/01-gameplay.md` §6;
     - `design-inputs.md` §2 item 6. The Contract's sizes win over DI-2's.
  10. The visible acceptance tests, read-only, in the play device kit folder: `TopBarKitTest.kt`, `ResumeScreenKitTest.kt`, `SolvedViewKitTest.kt`, `OverviewKitTest.kt` (it must compile), `KitScreen.kt`, and the six WO-003 classes as TASK-019 left them.
  11. Existing files:
      - in `play/src/main/kotlin/app/tangramplaytime/play/`: `PlayScreen.kt`, `PointerRouter.kt`, `HitTest.kt`, `Celebration.kt`, `PlayTexts.kt`, `PlayTags.kt`, `PlaySemantics.kt`, `PlayDrawing.kt`, `SolvedPicture.kt`, `PlayPalette.kt`;
      - `PlayController.kt` (read: S-1Δ and S-2Δ as TASK-018 built them);
      - `app/src/main/kotlin/app/tangramplaytime/app/MainActivity.kt`, `app/src/main/res/values/strings.xml` and `app/src/main/res/values-fi/strings.xml`: the texts only.
- **Do not read:**
  - the other briefs, and kit drafts outside the tree;
  - the `store/src/main` bodies;
  - the bodies of `kernel/src/main` and `content/src/main`;
  - `progress.md`, chat history, anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, on the device through the play androidTest dependency on `:content`.
- **Run:**
  - The kit-compile line, plus `:app:assembleDebug`.
  - The dev-image line, one class per invocation, with a reset and a check before and after.
  - The JVM line.
  - Grep `play/src/main` for colour literals outside `PlayPalette.kt` and for user-visible string literals. There must be none (G-10, O-06).
- **Done means:**
  - The seams exactly as the rows say. Every S-13Δ tag is declared now, including the overview's, whose nodes TASK-021 adds.
  - `title` keeps its § WO-003 tag-table row, and the zones and texts follow seam note 4.
  - Every path is in px (G-17).
  - The long press runs on the pointer node's own timeout (design §3, fact 5), with no sleep and no wall clock.
  - The ten texts are in `values/` and `values-fi/`. Finnish is a reference translation, listed in your hand-back for `review-notes.md` (G-10). The only `:app` change is the `PlayTexts` construction.
  - The one-shot cue rule holds for the celebration, as "**One-shot cues are shown once per controller**" says.
  - Read-only for you: the kit, `:contracts`, `:store`, `:kernel`, `:content`, `AppGraph.kt`.
  - **Decisions to hand back** as proposed rows: any drawing number that no row fixes (dot size, pill radius, state-word style, and so on), with its DI source.
  - The Done-check is the TASK-020 row.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with:
  - the inputs read, and the files changed;
  - the commands, their exit codes, the result XML paths and the test counts once;
  - the Finnish strings;
  - the proposed decision rows.

---

## Brief — TASK-021 · WO-004 · slice-implementer

- **Mission:** Build the grid overview, which is S-22's overview bullets and S-14Δ's cell keys. When it is done, OverviewKitTest is green, and the whole play device set is green on the dev and the floor image. The class is UI, rendering and touch, so it goes to the strongest model.
  - **Riskiest item first:** the overview's foundation `clickable`s against the root router (design §3, library fact 4: the router must not consume while the overview is open).
- **Read exactly:**
  1. `tasks.md` § WO-004: the intro (seam note 4) and row TASK-021.
  2. `designs/WO-004-design.md`:
     - §1, "How it works";
     - §2, rows S-14Δ and S-22, the "Overview" bullets, hit order 0, and "What survives";
     - §3, "Always show the name and number" (the grid-open leg) and library fact 4.
  3. `workorders/WO-004.md` § Mode (the grid overlay against everything under it).
  4. Requirements:
     - `Requirements/reqs/REQ-050.md`, `REQ-003.md`, `REQ-024.md`;
     - `REQ-026.md` (a Solved cell shows the picture, never piece positions: the REQ-050 ASSUMPTION);
     - `REQ-039.md` (the picture stays inside the outline);
     - `REQ-037.md`;
     - `Requirements/req_types.md` TYPE-006.
  5. `decisions.md` 2026-10-15: "**The grid overview, as drawn**" (91), "**Amends the 2026-10-15 'grid overview, as drawn' row**" (95), "**Startup, and what survives**" (92); and 2026-10-12 "**The solve, as drawn**" (75), the clip precedent.
  6. `platform-notes/android.md`: § Verification channels (the floor) and § Compose test kit.
  7. `architecture.md` §2 G-10, G-17; §3b O-06.
  8. Design inputs (behaviour reference only):
     - **DI-1** `tools/prototype_template.html`: `openGrid()` and `thumb()`;
     - **DI-2** `Spec/02-ui-layout.md` §1 and `Spec/ui/04-browse-states.png`;
     - `design-inputs.md` §2 item 6.
  9. The visible acceptance tests, read-only: `OverviewKitTest.kt` and the whole play device kit folder.
  10. Existing files: in `play/src/main/kotlin/app/tangramplaytime/play/`, `PlayScreen.kt`, `PointerRouter.kt`, `SilhouettePath.kt`, `SolvedPicture.kt`, `PlayPalette.kt`, `PlayTags.kt`, `PlaySemantics.kt` and `PlayController.kt` (its overview reads and actions).
- **Do not read:** the other briefs; the `store/src/main`, `kernel/src/main` and `content/src/main` bodies; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, on the device, as TASK-020.
- **Run:**
  - The dev-image line for `OverviewKitTest`.
  - Then the whole play set on the dev image, then on the floor image (the floor procedure of the channel table), with a reset and a check around each.
  - The JVM line.
- **Done means:**
  - S-22's overview and S-14Δ's keys exactly as the rows say.
  - The bar is drawn and inert while the overview is open.
  - The cells' paths are in px (G-17).
  - The strings already exist (TASK-020); no new string, unless a seam row needs one. If it does, hand it back.
  - Read-only for you: the kit, `:contracts`, `:store`, `:app`.
  - **Decisions to hand back** as proposed rows: any drawing number that no row fixes.
  - The Done-check is the TASK-021 row.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block, with:
  - the files changed;
  - the commands, their exit codes, and the XML paths for the dev and the floor run;
  - the test counts once;
  - the proposed decision rows.

---

## Brief — TASK-022 · WO-004 · slice-implementer

- **Mission:** Wire the persisted Play screen into the app (S-15Δ), and build the process-death channel with its positive control (S-24). Then prove the app set, the death pair and V-01 on both images. The class is an IO start-up (concurrency) plus a device-only channel, so it goes to the strongest model.
  - **Riskiest item first:** the death script's verdict. The runner's exit status never varies (design §3 fact 6; review F2), and a skipped phase still reads `OK` (seam note 1).
- **Read exactly:**
  1. `tasks.md`:
     - § WO-004: the intro (seam notes 1–3, the channels, device hygiene, the harness and the verifier budget) and row TASK-022;
     - § WO-003: S-15 and S-16 (the wiring and the reset script you extend).
  2. `designs/WO-004-design.md`:
     - §1, "How it works";
     - §2, rows S-15Δ, S-23, S-24, and "What survives";
     - §3, the death-pair bullets of "Highest-risk part", and library fact 6.
  3. `reviews/WO-004-design-review.md`: F2 with its Triage line.
  4. `workorders/WO-004.md`: § Scope notes (the carried WO-003 F3, and the behaviours to keep); § Constraints & notes ("Seams", "Verification channels").
  5. Requirements:
     - `Requirements/reqs/REQ-003.md`, `REQ-025.md` (rule: surviving a close), `REQ-040.md`, `REQ-045.md`;
     - `REQ-050.md` (rule: the back button closes the grid);
     - `REQ-010.md`.
  6. Contracts: `IProgressStore.kt`, `IPuzzleLibrary.kt`.
  7. `decisions.md`:
     - 2026-10-15: "**Startup, and what survives**" (92), "**REQ-024.A2 and the launch window**" (96), "**Amends the 2026-10-15 ADR-003 row**" (94);
     - 2026-10-12: "**Carried to WO-004 #Browsing**" (79), "**`:app` readings**" (76), "**Verifier V-01's reading of G-01**" (77).
  8. `platform-notes/android.md`, all of it: the channels, release safety, device hygiene, and the channel facts.
  9. `architecture.md` §2 G-01, G-08, G-12, G-13, G-14; §3b O-05, O-06.
  10. The visible acceptance tests, read-only, in the app device kit folder: `AppLaunchKitTest.kt`, `AppWiringKitTest.kt` and `ProcessDeathKitTest.kt`. Read them for their interface: the tags, `AppGraph.controller`, and the `deathPhase` argument.
  11. Existing files:
      - in `app/src/main/kotlin/app/tangramplaytime/app/`: `AppGraph.kt`, `MainActivity.kt`, `PlayLayouts.kt`;
      - `app/build.gradle.kts`;
      - `app/src/androidTest/kotlin/app/tangramplaytime/app/FloorChannelTest.kt` (yours: scaffolding);
      - `tools/device_reset.sh` (the precedent for finding adb and judging output);
      - `.swdev/verifiers/manifest.json` (no new entry);
      - `PlayLoading.kt` and `Persistence.kt` in `play/src/main/kotlin/app/tangramplaytime/play/`, and `ProgressStores.kt` in `store/src/main/kotlin/app/tangramplaytime/store/`: their public signatures only.
- **Do not read:** the other briefs; the `:play` and `:store` bodies beyond their public seams; the bodies of `kernel/src/main` and `content/src/main`; `progress.md`; chat history; anything under `C:/GitHub/AI/TangramNoAds`.
- **Inputs:** `Tangrams/*.json`, read on the device through the app's own `:content`; `tools/device_reset.sh` (unchanged).
- **Run:**
  1. The kit-compile line, then the JVM line.
  2. Dev image: a reset and a check, then `:app:connectedDebugAndroidTest`, copy the XML, then a reset and a check.
  3. Dev image: `bash tools/process_death_check.sh`, then `bash tools/process_death_check.sh --positive-control`, then the negative run (phase 2 without the `deathPhase` argument; see Done means).
  4. Floor image: step 2, and the main and control runs of step 3. Then return to the dev image and leave it running.
  5. The release-verifier line.
- **Done means:**
  - **S-15Δ exactly.**
    - The bundle and the store load on IO and the controller is built on Main (the WO-003 F3 carry).
    - `PlayLoading` shows until the controller exists.
    - Back closes the overview.
    - `onStop` still sends a running drag home, now on a nullable controller.
    - The door that `ProcessDeathKitTest` calls must be reachable from `app/src/androidTest`. If `internal` is not visible there, widen it; that door is S-15Δ's "the kit's one door".
  - **`FloorChannelTest.kt`** waits for `play-screen`, as `AppLaunchKitTest` does (review F2(c)).
  - **`tools/process_death_check.sh`** follows S-24 and seam note 1:
    - it builds nothing itself (the Gradle line does) and finds adb as `tools/device_reset.sh` does;
    - it judges each phase from the per-test instrumentation output, never from an exit status;
    - **a phase skipped by its guard is a failure, not a pass.** Prove this with a negative run on the dev image: phase 2 without `-e deathPhase` must be judged not passed, and the script must exit non-zero. This is part of the row's Done-check (3);
    - `--positive-control` follows seam note 1;
    - it resets the app as its last step, whatever the verdict;
    - its line count is reported. It is a test-channel harness, outside the verifier budget (`tasks.md` intro); keep it near the size of `tools/device_reset.sh`.
  - **G-01 still holds:** no permission, and the release verifier passes. WO-004 adds no verifier.
  - Read-only for you: the kit, `:contracts`, `:play`, `:store`, `:kernel`, `:content`. A needed `:play` change is `recirculate → planner`.
  - **Decisions to hand back** as proposed rows: any reading that no row fixes (for example, what the loading paper does on a load that throws).
  - The Done-check is the TASK-022 row.
  - **Ambiguity.** If a seam row is ambiguous, stop and hand back `recirculate → planner`.
- **Return:** the handoff block. Report the wiring half and the channel half separately, each with:
  - the files changed;
  - the commands with their exit codes, and the XML paths for the dev and the floor run;
  - the death script's verdict lines (main, control and negative run) on both images;
  - the V-01 and run_verifiers lines with the budget line;
  - the script's line count;
  - the test counts once;
  - the proposed decision rows.
