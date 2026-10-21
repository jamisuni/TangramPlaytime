# Review — design · WO-008

**Date:** 2026-10-20  ·  **Reviewer:** fresh context (`design-reviewer`, Opus 5.5)
**Inputs:** `designs/WO-008-design.md` (Draft, 2026-10-20) · `workorders/WO-008.md` (Open, P3: Scope, Scope notes, must-dos 1–3, Q1–Q10, Standing constraints) · REQ-005, REQ-006, REQ-029, REQ-030, REQ-031 (locked), plus REQ-026, REQ-032, REQ-034 and REQ-046 for context · `Requirements/req_types.md` TYPE-005, TYPE-006 · `architecture.md` v1.3 · `build-map.md` v1.6 · `design-inputs.md` §2 · `contracts/src/main/kotlin/app/tangramplaytime/contracts/*.kt` (baseline 2026-10-20) · `decisions.md` 2026-10-20 rows "WO-008 / P3 design" (file rows 188–196) · product code at `5706854`: `PlayController`, `Persistence`, `PlayScreen`, `Controls`, `PlayTexts`, `DevAid`, `AppGraph`, `MainActivity`, `FileProgressStore`, `ProgressDocument`, and every kit that builds a controller, a `Persistence` or a `PlayTexts` · `.swdev/guard.json`, `.swdev/contract-baseline.json`, SWDev `framework/skills/trace-check/trace_check.py`, `adapters/claude/hooks/swdev_guard.py` · libraries: `androidx.compose.ui:ui-test-android` 1.12.1 (bytecode), `androidx.activity:activity` 1.13.0 (bytecode), Android SDK sources `android-37.0`.

## Verdict

**forward**: no Blocker stands. Checked against the real `PlayController`, `Persistence` and store reader, the time maths, the three must-dos and the fold order hold. F1 and F2 should be settled first, because both are silent on screen and in the kit as written.

## Checklist applied

- [x] **Directives.** D3: each `IActiveTime` member names its REQ. D5: no new dependency. D2: the puzzle-time switch duplicates controller state (F2). D4: `timeOf` is public for WO-007 (F4).
- [x] **Guardrails.**
  - G-12 holds: `play/build.gradle.kts` main depends only on `:contracts`, and `:app` builds `ActiveTime` the way it already builds `LockEngine`.
  - G-13 holds: every write goes through `Persistence`.
  - G-14 holds: `AppClock` is the one `Clock`, and `delay` is not a clock read.
  - G-10: `time_hours_minutes` exists in both `en` and `fi`.
  - G-08 / O-08 hold: `solveWithoutBest` names no aid.
- [x] **Contract.** No existing contract file changes. The new interface goes through the G2 route, but the route as written leaves the new file unguarded and leaves one governed KDoc contradicting O-04 (F3).
- [x] **Scope.** REQ-029.A2 and the settings screen stay with WO-007. Nothing is pre-built except `timeOf`'s visibility (F4).
- [x] **Hard-stops.** No progress-file field is added: `ProgressDocument` already holds `playTimes` and `settings` (ADR-003 format 1). So there is no reason (c) and no hard-stop 2.
- [x] **Evidence re-derived.**
  - **Fact 2 (touches).** Holds for real input: `Activity.dispatchTouchEvent` (Activity.java:4670) is reached from `DecorView.dispatchTouchEvent` → `Window.Callback` (DecorView.java:422), and `ComponentActivity` 1.13.0 has no override. It is false for the kits' injected touches (F1).
  - **Fact 3 (clock).** Holds:
    - `SystemClock.elapsedRealtime` is "since boot, including time spent in sleep".
    - `ActivityThread.updateTimeZone` calls `TimeZone.setDefault(null)` (ActivityThread.java:1600).
    - `ZoneId.systemDefault` reads `TimeZone.getDefaultRef` per call.
  - **Fact 1 (visibility).** Not readable in the SDK sources (system_server). The design's device check plus the 60 s cap is sufficient.
  - **Must-do 2.** `ProgressDocument` rejects only negative `time`, `best`, `today` and `allTime`, a bad `turn` or centre, or a non-ISO date. `LocalDate.toString` → `parse` round-trips, and nothing else that WO-008 writes is rejected.
  - **Fold order on the real code.**
    - `open` = `goHome` + `load`, with S-20 `keep()` only at actions, so the leaving save is needed.
    - A leaving save in `init` would write the property defaults over the saved puzzle, and the design rightly excludes it.
    - In the solving `drop`, `keep()` follows the best-time update.
    - `DevAid.drive`'s `restart()` runs inside the withheld block.
  - **Constructor overloads.** Every existing kit construction still resolves: a lambda never matches `IActiveTime?`, and `persistence` has no default.
- [x] **The conceptual 20 %.**
  - **Per-screen table vs REQ-030 rule 1 and `design-inputs.md` §2.** The DEV sheet not pausing deviates from §2's "no overlay". That deviation is recorded with a D2 reason, applies to debug builds only, and "Solve now" restarts the time anyway.
  - **Quantifiers.**
    - REQ-029.A1: ⌊(r + 60 000)/1000⌋ = 60.
    - REQ-030.A1: the fold in `open` precedes the switch. Sub-second remainders shift by under 1 s per run interval, never by the 2 minutes.
    - REQ-030.A2: min-of-two, the aid keeps the best, and Retry and Restart keep it.

## Findings

### F1 — TEST · Should · S-15Δ3, "Kit doors" `app/` device, TASK-049 (REQ-029.A1, TYPE-005; library fact 2)
- **Observation:** Compose's injected touches never reach `MainActivity.dispatchTouchEvent`.
  - **The bytecode.** In ui-test-android 1.12.1, `AndroidInputDispatcher_androidKt.createInputDispatcher$lambda$1` delivers every `performTouchInput` / `performClick` MotionEvent with `View.dispatchTouchEvent` on `ViewRootForTest.getView()`. That is the `AndroidComposeView` itself, not the DecorView, so `AppGraph.touched()` never runs for injected touches.
  - **Every `app/` kit touches this way:** `KitApp.kt:1456`, `AppWiringKitTest:79`, `DevAidAppKitTest:156` and `ScaleMeasureTest`. The death pair drives `PlayController` directly.
  - **TASK-049's lines pass vacuously.** If "Touches are real" is read as the house helper:
    - "5 min untouched adds ≤ 60 s" passes with 0 counted;
    - "a stopped app counts nothing" passes with nothing to stop;
    - "the death pair carries the times" carries 0.
  - **A broken hook stays green.** A missing or misplaced `AppGraph.touched()` call stays green on every channel, because the JVM and `play/` doors call `time.touched()` themselves.
- **Proposed resolution:**
  - Say in "Kit doors" that the `app/` time lines inject through the window with `Instrumentation.sendPointerSync` or `UiAutomation.injectInputEvent`. Both go through `InputManagerGlobal.injectInputEvent` → the window → `Activity.dispatchTouchEvent` (Instrumentation.java:1291, 1312). They must never use `performTouchInput`.
  - Give every "at most" or "nothing" line a positive control in the same test: a window touch, then N s on the kit clock, adds N s. After 5 min untouched the test sees exactly 60 s counted, not 0.
  - File the fact under `platform-notes/android.md` § Compose test kit at the close.

### F2 — QUALITY (D2) · Should · S-2Δ3 / S-45: the flagged highest-risk part (REQ-030 statement; REQ-025)
- **Observation:** On every cold start, a resumed In-progress puzzle may count no puzzle time.
  - **The gap.** S-45 starts `ActiveTime` "with the puzzle not running", and only the listed actions report `puzzleRuns`. "The construction's first `load` neither folds nor saves" does not say whether it reports the switch.
  - **When it bites.** `AppGraph.start` builds the controller on every cold start and after every process death. `init` → `load` opens the last puzzle as saved, usually In progress (REQ-025). Unless the construction reports the switch, that puzzle counts nothing until the first tray `startDrag`. Board drags, `tapBoard`, `pressBadge` and non-solving drops are not on the list.
  - **The effect.** REQ-030's "WHILE a puzzle in progress is shown … SHALL count" fails silently, and with the setting on, the timer (REQ-031) stands still.
  - **The cause is structural.** The switch is a second copy of `state == IN_PROGRESS && !overviewOpen` that must be re-sent after every change. WO-007's reset and settings pause can miss it the same way. Because every change folds first, the controller's own predicate at fold time already equals the switch for the whole interval since the previous fold.
- **Proposed resolution:** One of these.
  - **(a) The simpler design (D2).**
    - Drop `puzzleRuns` and `Counted`. `count(): Int` returns whole play seconds, and the fold adds them to `timeSeconds` when the predicate holds. The result is the same, with two fewer governed members and nothing to forget.
    - REQ-030's tokens then belong in `play/`, which needs a `#PuzzleTime` build-map row (the Q10 delta).
  - **(b) Keep the switch.**
    - S-2Δ3 says that the construction reports `puzzleRuns` after its first `load`.
    - The JVM controller kit pins it: a store with an In-progress puzzle → construct → visible, touch, +30 s → `countTime()` → its time +30, with no other action.

### F3 — CONTRACT · Should · §1 "G2 route" (registry row, guard, O-04)
- **Observation:** The G2 route as written leaves the new file unguarded and the Contract contradicting itself.
  - **The guard.** "`guard.json`'s notify glob already covers the file" is false.
    - `.swdev/guard.json` `contract_paths.notify` lists four explicit paths (`build-map.md`, `ILockEngine.kt`, `IProgressStore.kt`, `Types.kt`), with no `contracts/**` glob.
    - `swdev_guard.py:85` matches them with `fnmatch`.
    - `trace_check.py --freeze` builds `.swdev/contract-baseline.json` from the same list.
  - **The effect.** `IActiveTime.kt` would be governed in §4 but neither guarded nor hashed, so a later edit would never surface as a contract delta.
  - **The KDoc.** The reworded O-04 puts the TYPE-006 transitions in `PlayController`. The governed `IProgressStore` KDoc still says "the rules that compute the values (TYPE-005 accounting, TYPE-006 transitions, best-time keeping) are the kernel's (O-04)", so "No delta to an existing contract file" leaves the two contradicting each other.
- **Proposed resolution:**
  - The G2 route also adds `contracts/src/main/kotlin/app/tangramplaytime/contracts/IActiveTime.kt` to `contract_paths.notify`.
  - Re-freeze the baseline once TASK-044 lands the file.
  - Give the proposed registry row its Sign-off cell.
  - Either reword that `IProgressStore` KDoc line as a logged notify-tier delta, or record in the WO why it stays.

### F4 — QUALITY · Should · S-11Δ3 / S-48 and the cut (TASK-045 [P], TASK-047)
- **Observation:** S-11Δ3 breaks the compile of five existing test files, and no task in the cut lists them.
  - **The new field.** `PlayTexts` gains `hoursMinutes`, and by its own KDoc no field has a default (G-10). Four files construct it and stop compiling:
    - `play/src/androidTest/kotlin/app/tangramplaytime/play/KitScreen.kt:114`;
    - `play/src/test/kotlin/app/tangramplaytime/play/CounterDigitsKitTest.kt:38,46` (acceptance kit);
    - `play/src/test/kotlin/app/tangramplaytime/play/scaffolding/DecorationScaffoldTest.kt:56`;
    - `play/src/test/kotlin/app/tangramplaytime/play/scaffolding/ScreenSeamScaffoldTest.kt:74`.
  - **The fifth file.** "`bestTimeText` uses it" changes `bestTimeText(seconds, noTime)`. `ControlsScaffoldTest.kt:116–119` calls it and pins `"60:00"` for 3600 s, which becomes "1 h 0 min".
  - **The timing risk.** The design's "compiles unchanged" covers only S-1Δ3. TASK-045 (the test author, in `play/src/test`) can still be running when TASK-047 lands, and one broken constructor stops the whole `:play` JVM test compile.
  - **D4.** `timeOf` is made public "for WO-007's rows", but nothing in this WO needs more than `internal`.
- **Proposed resolution:**
  - TASK-047 declares the five files as outputs. The two kit files are coordinated with the test author's task, or TASK-045 owns the `PlayTexts` literal edits, so that `play/src/test` compiles at every step.
  - The scaffold pin moves to the one-format value.
  - `timeOf` stays `internal` until WO-007.

### F5 — QUALITY · Should · S-45 accrual, "a reading below `last`" (must-do 2; S-44 must-never "idle more than 60 s")
- **Observation:** "Re-bases both `last` and `lastTouch`" does not say to what value.
  - **The likely reading breaks a must-never.** The natural reading is `lastTouch = now`. A player idle for minutes then counts as touched at the jump, and up to 60 s count with no touch. That breaks S-44's own must-never.
  - **Where it happens.** On a device, `elapsedRealtime` never goes back, but the design's `app/` kit makes it go back:
    - `AppClock.override` starts at `elapsedRealtime()` and jumps 5 min forward;
    - resetting it to null reads the real clock again, minutes lower;
    - this happens inside one instrumentation process, where `AppGraph` and its `ActiveTime` live across tests.
- **Proposed resolution:**
  - On a reading `now < last`, shift instead of resetting: `lastTouch -= (last − now)` (no touch stays no touch), then `last = now`. This keeps the time since the last touch.
  - The kernel kit's backwards-clock pin asserts that an idle player stays idle across the jump, as well as "never negative".

## Handoff — Design Reviewer · WO-008
- **Scope:** REQ-005, REQ-006, REQ-029, REQ-030, REQ-031; TYPE-005, TYPE-006 · governed touched: `IActiveTime` (new, proposed); consumes `IProgressStore`, `Types.kt`, `ILockEngine`, `IPuzzleLibrary`
- **Inputs read:** `designs/WO-008-design.md` Draft 2026-10-20 · `workorders/WO-008.md` P3 · REQs as above (locked) · `architecture.md` v1.3 · `build-map.md` v1.6 · `design-inputs.md` §2 · contracts (baseline 2026-10-20) · `decisions.md` 2026-10-20 rows 188–196 · code at `5706854` · ui-test-android 1.12.1, activity 1.13.0, SDK sources android-37.0
- **Result:** `reviews/WO-008-design-review.md`. Findings B/S/N: 0/5/0
- **Status:** forward
- **Traceability delta:** none (review only)
- **Notes for next station:** The planner carries F1–F5. F2 is the author's choice between (a) and (b); (a) also moves REQ-030's tokens to `play/`. The orchestrator applies F3 in the G2 route.
