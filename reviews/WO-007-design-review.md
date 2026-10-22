# Review — design · WO-007

**Date:** 2026-10-21  ·  **Reviewer:** fresh context (`design-reviewer`, Opus 5.5)
**Inputs:** `designs/WO-007-design.md` (Draft, 2026-10-21) · `workorders/WO-007.md` (In progress, P3: Scope, Scope notes, carries 1–5, Q1–Q11, Standing constraints) · `decisions.md` 2026-10-21 rows "Scope of WO-007 …" and the nine "WO-007 / P3 design" rows (file rows 219–228), and the 2026-10-20 rows they amend (file rows 190, 205, 206) · REQ-032, 033, 034, 009, 029, 049, 031, 030, 001, 008, 010, 037, 047 (locked), REQ-024 for the top bar; `Requirements/req_types.md` TYPE-005, TYPE-006 · `architecture.md` v1.4 · `build-map.md` v1.8 · `design-inputs.md` §2 · `platform-notes/android.md` · `capture-outbox/` `chg-draft-req033-a1-device-oracle.md`, `chg-draft-licence-notice-and-assets.md`, `chg-draft-req032-how-to-play-text.md`, `chg-draft-type006-missing-transitions.md` · code at `aeba492`: `IActiveTime.kt`, `IProgressStore.kt`, `Types.kt`, `ActiveTime.kt`, `PlayController.kt`, `Persistence.kt`, `PlayScreen.kt` (`ScreenGeometry`), `TopBar.kt`, `HitTest.kt`, `PointerRouter.kt`, `Overview.kt`, `PlayTexts.kt`, `PlayEvent.kt`, `PlayTags.kt`, `AppGraph.kt`, `AppClock.kt`, `MainActivity.kt`, `AndroidManifest.xml`, `res/values{,-fi}/strings.xml`, `FileProgressStore.kt`, `DevAid.kt` (drive) · kits: `PlayThroughKitTest`, `KitApp` (start / restore, `closedWorld`, `kitTouchThroughWindow`), `ProcessDeathKitTest` with `tools/process_death_check.sh`, `ReleasePromiseKitTest` (allow-list), `ActiveTimeAppKitTest`, `TouchTargetKitTest` (header), `TopBarKitTest`, `SafeAreaKitTest` and the `play/` scaffolds · `.swdev/guard.json` · SWDev `framework/skills/trace-check/trace_check.py` `check_features_v2` · libraries: `androidx.compose.foundation:foundation-android` 1.12.1 and `androidx.compose.ui:ui-android` 1.12.1 (bytecode, `javap`), Android SDK sources `android-37.0` (`View`, `ViewRootImpl`, `AudioManager`, `SoundPool`, `Vibrator`, `SystemVibrator`, `SystemVibratorManager`).

## Verdict

**recirculate → design-author**: one Blocker stands. Library fact 2 is false. Compose 1.12.1's `clickable` and `toggleable` play the platform click, so with the game's sound off, the settings controls and the grid's cells still click on a device whose touch sounds are on. The design's "no system click" row and REQ-033.A1's oracle both rest on that fact. Everything else held when checked against the real code: the reset order (the flagged risk), N1, the pause, facts 1 and 3–5, and the `#ActiveTime` move. The second look covers F1's fix only. The planner carries F2–F5.

## Checklist applied

- [x] **Directives.**
  - D3: every new abstraction names its REQ (`FeedbackOut` is REQ-033.A1's door, `BestTime` is REQ-030 rule 3, `SettingsTags` is REQ-037).
  - D4: `timeOf` and `titleOf` go public now that `:app` needs them (WO-008 F4).
  - D5: no new dependency; `SoundPool` and the haptic API are the platform's.
  - D2: one `IActiveTime` member is the smallest way to erase the kernel's in-memory times.
- [x] **Guardrails.**
  - G-01 / G-11: the sound and haptic APIs need no permission (rows 3–4).
  - G-10: every new string has an `fi` reference in `review-notes.md`.
  - G-12: `Feedback` and `DeviceFeedback` live in `:app`; `:play` only emits (O-07).
  - G-13: both new writes go through `Persistence`'s one FIFO.
  - G-14: no clock read in the new code.
  - G-16: the sounds are made for the project and listed.
  - G-09: no difficulty control.
- [x] **Contract.**
  - `IActiveTime.resetPlayTimes()` is declared as a notify delta, with its KDoc and the banner's Serves line.
  - `IProgressStore` (`saveSettings`, `resetAll`) and `Types.kt` (`Settings(timerShown = false, soundOn = true)`) are consumed unchanged.
  - The registry row goes through the G2 route; `architecture.md` is a locked path in `guard.json`.
- [x] **Scope.** Everything traces to the 16 IDs, plus the licence line, which is pinned without a token as the scope row says.
- [x] **Hard-stops.** No progress-file field is added: format 1 already holds `settings` and `playTimes`, and `FileProgressStore.resetAll` (line 73) keeps the settings and last-opened. Hard-stop 1 is not reached.
- [x] **Seam table.** Every row names the task that delivers it.
- [x] **Evidence re-derived:** the table below.
- [x] **The conceptual 20 %:** the reset order, the readers of the settings, the pause and the window, the oracle's cost, the kit doors, and the top bar with a fourth column.

## Re-derived

| # | What the design relies on | Where it was checked | Result |
|---|---|---|---|
| 1 | Fact 1: the topmost hit sibling takes the pointer; lower siblings are not hit; a parent in the path still sees the event | ui-android 1.12.1 `InnerNodeCoordinator.hitTestChild`: z-sorted children are tried from the top, and after a hit the next sibling is tried only when `shouldSharePointerInputWithSiblings()` | **Holds.** The sheet must therefore be the scrim's *sibling*, as in `Overview.kt:126–142`. A sheet nested inside the scrim's `clickable` would close the screen at any tap on its text. |
| 2 | Fact 2: foundation 1.12.x `clickable` and `toggleable` never call `View.playSoundEffect` | foundation-android 1.12.1: `AbstractClickableNode.performClick()` calls `playClickSound()` before `onClick`; the gate `ComposeFoundationFlags.isInteractionSoundEffectOnClickEnabled` is set `true` in its static initializer, and the sound comes from `LocalSoundEffect`. ui-android 1.12.1: `ComposeViewContext.getSoundEffect()` provides `AndroidSoundEffect(view)` while `AndroidComposeUiFlags.isInteractionSoundEffectsEnabled` (true), and that calls `view.playSoundEffect(0 = CLICK)`. `ToggleableNode extends ClickableNode`. android-37: `View.playSoundEffect` → `ViewRootImpl.playSoundEffect` → `AudioManager.playSoundEffect(FX_KEY_CLICK)`, which follows the device's touch-sounds setting | **False** (F1) |
| 3 | Fact 3: `SoundPool` needs no permission, loads asynchronously, and steals streams by priority, then age | android-37 `SoundPool.java` class doc, lines 57–87 | **Holds.** A new stream whose priority is below every active one does not play, and `play` returns 0. |
| 4 | Fact 4: `performHapticFeedback(CLOCK_TICK)` needs no permission and obeys the system setting | android-37: `View.performHapticFeedback` (29345) does nothing when the view is detached (`isPerformHapticFeedbackSuppressed`), else calls `ViewRootImpl.performHapticFeedback` (10644), then `SystemVibratorManager.performHapticFeedback`, then the service. No `@RequiresPermission` is on that path; the VIBRATE-annotated member at `Vibrator.java:634` is a different one | **Holds.** The view must be attached, which S-51's `onStart` → `onStop` window gives. |
| 5 | Fact 5: `stringResource` uses the S-36-wrapped resources | `MainActivity.attachBaseContext(AppLanguage.wrap(…))`; the context of `setContent` is the activity, whose resources `getString` already reads | **Holds** |
| 6 | N1: every reader sees a switch at once | `PlayScreen.kt:207` reads `controller.timerShown` during composition; S-2Δ4 assigns the controller's own `settings`; `Feedback`'s lambda reads `built.value?.settings` on the main thread at each event | **Holds**, provided `settings` is the controller's own `mutableStateOf`, as S-2Δ4's assignment implies, and not a getter over `Persistence.settings` |
| 7 | The reset order (§3, the flagged risk) | `PlayController` (fold 427, load 475, keep 523); `Persistence` (writes enqueued under the lock, one `Channel` consumer); `ActiveTime.accrue`; `FileProgressStore.resetAll:73`; the `AppGraph` ticker on `MainScope` | **Holds.**<br>• Every step runs on the main thread, so neither the ticker nor `setVisible(false)` can run inside `resetAll`.<br>• Every write queued before `Write.Reset` lands before it.<br>• After it, `load`'s `keep()` writes nothing (FRESH equals FRESH), so only `LastOpened` follows.<br>• The next `writeTimes` writes the kernel's post-reset times.<br>• The board in memory after the reset: tray turns at rest, no drag, no cue, no celebration, best null, time 0. |
| 8 | The pause, and play time counting while the screen is open | The fold predicate (line 430). `Activity.dispatchTouchEvent` sees every real touch of its window (WO-008 design review, fact 2), and the layer is in that window | **Holds** |
| 9 | The gear never acts while another finger is down, and no drag starts under the open layer | `PointerRouter.route`: `awaitEachGesture` gives one gesture every pointer until its primary finger lifts, and the other pointers' changes are consumed in `gesture` and `awaitControlEnd` | **Holds** |
| 10 | Trace-check rule 3, and moving `#ActiveTime`'s code home to `app/` | `trace_check.py:556–574` requires, per token, at least one covering test under the code home; `ActiveTimeAppKitTest.kt:74` already carries `REQ-029.A1` in `app/` | **The move is right.** `app/` is the one module where both lines have honest oracles. Precedents: `#PlayTime` → `play/` (v1.7), `#GoHome`. A `—` home would skip rule 3, and a `kernel/` A2 token would be theater. |
| 11 | `ReleasePromiseKitTest`'s allow-list | `KIT_ALLOWED_ANDROIDX` already holds `androidx/compose/`, and `android.media` is the platform's | No new package is expected from `toggleable` or `SoundPool` |
| 12 | The death pair can carry a reset | `tools/process_death_check.sh:97–105` runs exactly `phase1` → force-stop → `phase2`; `ProcessDeathKitTest.phase2` asserts the placed pieces and the times (REQ-025.A1, REQ-003.A2, REQ-005.A2) | **Not as written** (F4) |
| 13 | `kitTouchThroughWindow` "where time counts", on the new screen | `KitApp.kitInertBoardPoint` / `kitTouchThroughWindow` (lines 1559–1632): the point is on the board, and the before/after check compares (puzzle, state, placed, overviewOpen) | **Not on the settings screen** (F2) |
| 14 | "The counter keeps everything between ‹ and ›" | `TopBar.kt`: the second line is a `Row` of 52 dp of dots, a 13 dp separator, "n / N", a separator and the state word, all 13 sp and `softWrap = false` with no ellipsis; the columns are 60 dp (`NAV_COLUMN_DP`) | **Not at 360 dp** (F5) |

## Findings

### F1 — QUALITY (library fact) · Blocker · §3 fact 2; S-49, S-50, S-15Δ4; decisions.md 2026-10-21 "The five sounds and the tick" ("No system click") and "REQ-033.A1's oracle is one door …" (REQ-033 statement and A1; WO-007 Q6, last clause)
- **Observation:** Every Compose `clickable` and `toggleable` tap in this app plays the platform click (row 2).
  - **The chain.** `performClick()` plays the click before it runs `onClick`. Both library flags are on, and `LocalSoundEffect` is `AndroidSoundEffect(view)`, which calls `View.playSoundEffect(CLICK)`. The system plays it whenever the device's touch sounds are on.
  - **Where it sounds with the game's sound off.**
    - The new screen: Done, the scrim, both switch rows, Reset, Erase and Cancel. The sound switch clicks *before* its `onClick`, so turning sound back on clicks while sound is still off.
    - Already shipped since WO-004: the grid's cells, its Done and its scrim (`Overview.kt:130, 218, 283`), and the DEV sheet.
    - The router's raw-pointer controls play nothing: ‹, ›, the counter, the gear and the pieces.
  - **What rests on the false fact.**
    - The decisions row states "No system click: Compose's foundation clickables play none".
    - §3's must-never says "sound off → no sound or tick".
    - REQ-033.A1 is to be "verified as locked" on the `Feedback` recorder, which cannot see this path. Its sound-off leg ("browses and opens the grid") would pass on a device that clicks.
    - Question 6's last clause ("whether any platform sound … plays on a tap anywhere, and what REQ-033 asks of it") is answered on this fact.
- **Proposed resolution:** the author chooses one of the options below and writes it into §3, S-15Δ4 / S-49 and the two rows.
  - **(a) Keep the click out of the window.** Wrap the content of `setContent` in `SoundEffectOnInteraction(enabled = false)`. It is public in ui-android 1.12.1, carries no opt-in marker, and provides a `DelegatingSoundEffect(…, isEnabled)`. It covers the grid, the DEV sheet and S-49 together. With `enabled = controller.settings.soundOn`, the click follows the switch instead.
  - **(b) Route the click through the one door.** Provide `LocalSoundEffect` with an `:app` `SoundEffect` whose `playClickSound()` goes to `Feedback`, behind the switch. SoundKitTest's recorder then sees it, and the oracle covers the click.
  - **(c) Read REQ-033 as the game's own five sounds,** with the device's touch sounds the system's feature under the system's own setting. The locked A1 says "no action", so this needs a decisions row and a capture-side note beside the pending A1 draft.
  - **Whichever is chosen:**
    - Correct fact 2.
    - SoundKitTest's sound-off leg presses at least the switch (turning sound back on), Done and a grid cell, and the design says what observes those presses.
    - The A1 status waits until its oracle can see every path that can make a sound.

### F2 — TEST · Should · §2 "Kit doors (Q11)", `app/` device (`kitTouchThroughWindow`, the restore rules); TASK-055 (REQ-029 statement and A2, REQ-030 rule 1, REQ-032 rule 2; decisions.md 2026-10-21 "The settings screen sits in the activity's window …")
- **Observation:** The kit's doors do not know the new layer (row 13).
  - **The window touch lands on the layer.** `kitTouchThroughWindow` taps the board's inert point. With the screen open:
    - under the scrim, that DOWN/UP is a click, and the click calls `closeSettings()`;
    - under the sheet, it can hit a switch row or Reset.
    - The helper's own before/after check leaves out `settingsOpen`, so a screen closed by the touch passes unseen.
    - "Play time counts while the screen is open, and touches on it count" and "the puzzle time stays paused" would then be measured on a closed screen. The first passes vacuously; the second is wrong whenever the puzzle is In progress.
  - **A layer left open cascades.**
    - `KitApp.start` fails only on a DEV sheet left open.
    - `restoreController` closes the overview and the DEV sheet, but not the settings screen.
    - A class that fails with the screen open leaves `play-screen` present, so the next class starts, and every touch it makes lands on the scrim, across a device set of about 2 h 50 min.
  - **The switches' restore.** "Restores in `finally` (timer off, sound on)" puts back the defaults, not what the class found. It also runs outside `restoreRule()`'s ordered, bounded restores (seam note 3).
- **Proposed resolution:**
  - Name an inert point on the settings screen for window touches: the fixed header's title, a merged inert node, read on the screen in use. Add `settingsOpen` to the helper's before/after check.
  - Make `start` fail loudly while `settingsOpen` holds, and have `restoreController` call `closeSettings()`.
  - Register the switches' restore with `onRestore`, putting back the values read at `start`.
  - Make REQ-029.A2's oracle able to fail:
    - today ≠ all-time, both above 0;
    - with the screen open, a window touch on the header plus N s on the kit clock moves both shown rows by N s;
    - the puzzle's time stays unchanged.

### F3 — QUALITY · Should · §3 "The moved ›"; §4 TASK-055 and TASK-056 (S-53, S-32Δ; REQ-037)
- **Observation:** The cut puts "the gear pins with the moved-› corrections" in TASK-055, "Kit stage 2 (`app/` device)". The pins that put › at the safe right edge live in `play/`, and half of them run on the JVM line:
  - `play/src/test/.../SafeAreaKitTest.kt:288–293`: › ends at the safe right, and the counter ends at `navNext.left`.
  - The fixture check in the same file at `:202`: with a right inset, the NONE layout's › column must end right of the safe right. Once › stops 52 dp short of the edge, that check fails unless the inset is wider than 52 dp.
  - `play/src/androidTest/.../TopBarKitTest.kt:192–234`: › spans W − 60 dp to W, with taps 2 px inside its right edge.
  - The implementer-owned scaffolds `ControlsScaffoldTest.kt:55–59`, `SafeAreaScaffoldTest.kt:40, 101` and `ScreenSeamScaffoldTest`.

  When S-53 lands in TASK-056, the JVM two-run line goes red until each of these moves in the same step. This is WO-008 F4's pattern.
- **Proposed resolution:**
  - List the files as declared outputs: the kit files go to the test author's task, the scaffolds to TASK-056.
  - Land all of them with S-53, so that `play/src/test` and `play/src/androidTest` are green at every step.
  - File each one in `proposals.md` as a corrected check.

### F4 — TEST · Should · §3 "Highest-risk part" (its oracles); TASK-055, TASK-061 (REQ-034 statement, rule 2, A1, A2; ADR-003)
- **Observation:** The flagged risk holds on the code (row 7), but its two named oracles do not yet test it.
  - **The death pair cannot carry a reset as written** (row 12). `phase2` asserts what a reset in the same pair erases. "The death pair's reset phases" therefore means new test methods plus a script change (its modes and its positive control), run on two images, and no seam row or task names that change.
  - **"Silent until a restart" can be caught more cheaply on the JVM.** The risk is a stale write that lands after `Write.Reset`; only a fresh read of the store shows it. A JVM leg:
    1. Use the real `FileProgressStore` on a temporary file, the real `Persistence`, `ActiveTime` over a kit clock, and the controller.
    2. Build a trap state: a Solved puzzle with a best, the open puzzle In progress with placed pieces and time, play time above 0, the timer setting on.
    3. Run `resetAll`, then `flush`.
    4. Build a new store, `Persistence.load` and controller over the same file.

    The fresh read must show every puzzle FRESH, `PlayTimes.NONE` plus any second counted after the reset, and the settings and last-opened kept. This catches any late stale write, whatever order it came in.
  - **The device fixture cannot fail.** "A resetting class requires all puzzles New" means ResetKitTest's A1 passes on a store that was already New. A2 (Cancel changes nothing) has the same gap: it needs non-trivial state before Cancel.
- **Proposed resolution:**
  - Say which oracle carries the restart: the JVM restart leg above, or the death pair with its new phases and a task for the script.
  - Have ResetKitTest build the trap state of step 2 before Erase and before Cancel. The class still leaves every puzzle New for the next class.

### F5 — QUALITY · Should · S-53 and S-32Δ; decisions.md 2026-10-21 "The gear and the settings screen, as drawn" ("the counter keeps everything between ‹ and ›") (REQ-024 rule 1, consumed; REQ-006.A1's 360 × 780 dp phone; WO-007 Q1)
- **Observation:** With the gear, the counter's second line no longer fits on the play-through's 360 dp phone (row 14).
  - **The space.** The counter is 360 − 2 × 60 − 52 = 188 dp, and its content 180 dp after the 4 dp padding on each side.
  - **The need.** Roboto's advance widths at 13 sp put "50 / 106 · in progress" at about 191 dp: 52 dp of dots, two 13 dp separators, about 48 dp for the number and about 65 dp for the word. "100 / 106" needs about 198 dp.
  - **The effect.** The `Row` hands the state word what is left, and `SubLine` clips it with no ellipsis, so the bar shows "in progre". REQ-024's rule says the bar shows the puzzle's state.
  - **Why no pin sees it.** `TopBarKitTest:377–378` checks only that the word's centre lies in the counter zone.
  - The width is 52 dp narrower than today, which still fits with about 40 dp to spare.
- **Proposed resolution:**
  - Measure it on the 360 dp phone at font scale 1, with the worst cases: a three-digit number, "in progress", and the Finnish words.
  - Then either change a constant (the nav columns' 60 dp, the gear's 52 dp, the separators) or let the second line shrink.
  - Add a pin that the number and the state word are drawn whole, with no visual overflow.

## Handoff — Design Reviewer · WO-007
- **Scope:** REQ-032 A1..A2, REQ-033.A1, REQ-034 A1..A2, REQ-009.A1, REQ-008 A1..A2, REQ-029.A2, REQ-031.A2, REQ-049.A1, REQ-001.A1, REQ-010 A1..A2, REQ-037.A1, REQ-047.A1; TYPE-005, TYPE-006 · governed touched: `IActiveTime` (changes, notify; proposed); consumes `IProgressStore`, `Types.kt`, `ILockEngine`, `IPuzzleLibrary`
- **Inputs read:** `designs/WO-007-design.md` Draft 2026-10-21 · `workorders/WO-007.md` P3 · REQs as above (locked), `req_types.md` · `architecture.md` v1.4 · `build-map.md` v1.8 · `design-inputs.md` §2 · `platform-notes/android.md` · `decisions.md` file rows 190, 205, 206, 219–228 · four `capture-outbox/` drafts · code and kits at `aeba492` · foundation-android and ui-android 1.12.1 (bytecode), SDK sources android-37.0 · SWDev `trace_check.py`
- **Result:** `reviews/WO-007-design-review.md`. Findings B/S/N: 1/4/0
- **Status:** recirculate → design-author (F1: library fact 2 is false; every `clickable` / `toggleable` tap plays the platform click, with the game's sound off too)
- **Traceability delta:** none (review only)
- **Notes for next station:** The second look covers F1's fix only. The planner carries F2–F5. The `#ActiveTime` move to `app/` (build-map v1.9) is right; the orchestrator applies it together with the `IActiveTime` delta through the G2 route.

## F1 re-check (2026-10-21)

**Scope:** F1's fix only. The design is now Reviewed, with its write-back: S-50, S-51, S-15Δ4, the S-49 tags, §2 "Tokens", §3 facts 2 and 6, and the residual on focus sounds. Also read: the orchestrator's ruling row ("F1 (Blocker) and F2–F5 all go back …"; option (c) is out) and the two amending rows ("Amends … "The five sounds and the tick." …" and "Amends … "REQ-033.A1's oracle is one door …" …"). F2–F5 are not re-opened.

**Verdict: resolved.** Option (b) puts every Compose tap click behind `Feedback`'s switch gate, and the recorder sees it. Checked against the same foundation-android and ui-android 1.12.1 bytecode as row 2.

1. **The provider reaches every `clickable` / `toggleable` in the window.**
   - **The read.** `AbstractClickableNode.playClickSound()` reads `currentValueOf(LocalSoundEffect)` at the node, at click time. `ToggleableNode extends ClickableNode`, so the switches take the same path.
   - **The default sits above the app's provider.** It comes from `ComposeViewContext.ProvideCompositionLocals$ui` (called from `WrappedComposition`), which wraps the whole composition of each `ComposeView`. A `CompositionLocalProvider(LocalSoundEffect provides AppGraph.feedback)` around `setContent`'s content therefore wins for everything composed in it:
     - `PlayScreen` with the grid's cells, its Done and its scrim, and the DEV sheet layer, all composed inside `PlayScreen`;
     - the sibling `SettingsScreen`.
   - **Nothing in between re-provides it.** The only re-providers are:
     - `ComposeViewContext`, at the root of each `ComposeView`. That is a new window's root: a `Popup` or a `Dialog`, which S-49 forbids, and none exists in `play/src/main`, `play/src/debug` or `app/src/main`.
     - `SoundEffectOnInteraction`, which is not used.

     Nothing in foundation, foundation-layout, animation, runtime or activity-compose 1.13.0 provides it.
   - **The only readers** are `AbstractClickableNode` and `CombinedClickableNode`, and the app has no `combinedClickable`.
2. **No opt-in.**
   - `CompositionLocalsKt.getLocalSoundEffect()` is public static; its only annotation is `@NotNull`.
   - `SoundEffect` is a public interface whose single abstract member is `playClickSound()`, so `Feedback : SoundEffect` compiles.
   - Neither carries an opt-in marker, and no `$annotations` holder exists for the property.
   - Fact 6 matches the bytecode.
3. **The sound-off leg can fail, together with its positive control in the same test.**
   - **A broken gate.** If `Feedback.playClickSound()` ignored the switch, the recorder would see clicks with sound off, and the negative leg fails.
   - **A missing or misplaced provider.** The clicks would fall back to `AndroidSoundEffect`, the recorder would stay empty, and the negative leg alone would pass. The positive control then fails: it presses one control in each subtree the provider must cover, the settings screen's Done and a grid cell inside `PlayScreen`.
   - **The switch.** `performClick()` plays the click before `onClick`. Turning sound off therefore records one click, which the leg clears; turning it back on with sound off records none. The amending row states exactly this.
   - **No other touch path to a platform sound.** `View.playSoundEffect` is called in ui only by `AndroidSoundEffect` and by the focus-navigation lambda, and in foundation not at all. No app view has an `OnClickListener`.
   - **The device call.** `DeviceFeedback.click()` makes the same `View.playSoundEffect(CLICK)` call on the attached decor view, so the click with sound on stays as today, as the ruling requires.
4. **The residual is honestly outside REQ-033.A1 for this game.**
   - **The path.** `AndroidComposeViewNavigationSoundEffect` plays a navigation sound when focus moves by hardware keys or a D-pad. It calls `view.playSoundEffect` on the `AndroidComposeView` itself, behind `AndroidComposeUiFlags.isInteractionSoundEffectsEnabled`, not through `LocalSoundEffect`, so the design's claim is right.
   - **Why it is not a gap.** The game is played by touch: REQ-037, and no key handling is designed. Enter or D-pad centre on a focused control goes through `onClickKeyUpEvent` → `performClick()`, so even that click is gated. Only the focus *move* escapes. A G4 note is the right weight.
   - **If the owner wants it closed:** `View.playSoundEffect` returns when `!isSoundEffectsEnabled()`, so setting the `AndroidComposeView`'s `isSoundEffectsEnabled` (`LocalView.current`) from the switch would silence it with public API. This is not required.
   - **The same class of residual:** a long-press menu on the DEV prompt's text field, which is debug only.

**Watch point for the code review (not a finding):** a `clickable` composed later inside a `Popup` or `Dialog` would get that window's own `AndroidSoundEffect` and miss the switch. S-49's "never a `Dialog` or `Popup`" is what keeps the door whole.

### Handoff — Design Reviewer · WO-007 (F1 re-check)
- **Scope:** REQ-033 statement and A1 (F1 only) · governed touched: none by this fix
- **Inputs read:** `designs/WO-007-design.md` (Reviewed, write-back 2026-10-21) · `decisions.md` 2026-10-21 rows "F1 (Blocker) and F2–F5 all go back …", "Amends … "The five sounds and the tick." …", "Amends … "REQ-033.A1's oracle is one door …" …" · foundation-android and ui-android 1.12.1, animation, foundation-layout and runtime 1.12.1, activity-compose 1.13.0 (bytecode) · product code at `aeba492` (Popup / Dialog / re-provider grep)
- **Result:** this section. F1 resolved; findings B/S/N after the re-check: 0/4/0
- **Status:** forward
- **Traceability delta:** none (review only)
- **Notes for next station:** The planner carries F2–F5 as written back. The focus-sound residual goes on the G4 surface as a note.
