# WO-007 plan review (mechanical, one round)

**Reviewer:** plan-reviewer (fresh context)  ·  **Date:** 2026-10-21  ·  **Plan:** `tasks.md` § WO-007 (TASK-052 … TASK-062), `workorders/WO-007-briefs.md`  ·  **Mode:** Full (b)

## plan-check output (verbatim)

```
plan-check: WO-007 — 11 task(s) (0 [P]), mode Full (b), 16 in-scope acceptance ID(s)
  warn: tasks cover 11 acceptance ID(s) outside the WO's scope: REQ-001.A2, REQ-009.A2, REQ-024.A1, REQ-029.A1, REQ-030.A1, REQ-030.A2, REQ-031.A1, REQ-033.A2
  warn: TASK-052: Input Tangrams/*.json not in the tree and no task names it
  warn: TASK-054 / 055 / 056 / 058 / 059 / 060 / 061 / 062: Input Tangrams/*.json not in the tree and no task names it
plan-check: GREEN (0 blocker(s), 10 warning(s))
```

(The script's own output lists nine `Tangrams/*.json` lines, one per task. I re-ran it and give them as two lines here.)

Warning triage:
- `Tangrams/*.json` is a false positive. The folder holds 106 puzzle files, and no task in this WO changes it. The glob is a declared Input and is satisfied.
- The out-of-scope IDs are the REQ x A-number product. I grepped the WO-007 section.
  - REQ-024.A1, REQ-029.A1, REQ-030.A1 and A2, and REQ-031.A1 appear only as "kept green" lines in Done-checks, for tests already in the tree.
  - REQ-001.A2 appears once, in "Not covered here" (WO-009).
  - REQ-009.A2 and REQ-033.A2 appear nowhere in the section. They do not exist as IDs.
  - No Covers cell claims any of them.

## Hand checks (what the script cannot see)

1. **Coverage both ways: holds.**
   - All 16 IDs land on a task, and each has a tokened test in its code home (`build-map.md` v1.9):
     - `SettingsKitTest` (`app/`): REQ-032 A1..A2, REQ-009.A1, REQ-049.A1, REQ-029.A2 and REQ-031.A2.
     - `ResetKitTest` (`app/`): REQ-034 A1..A2.
     - `SoundKitTest` (`app/`): REQ-033.A1.
     - `PlayThroughKitTest`: REQ-001.A1, REQ-008 A1..A2, REQ-010 A1..A2.
     - `TouchTargetKitTest`: REQ-037.A1.
     - `LanguageKitTest`: REQ-047.A1, which stays unverified by design.
     - REQ-010.A2 is also held by `ReleasePromiseKitTest` and V-01.
   - Every task row names its REQs and IDs. TASK-052, 055 and 060 state their groundwork or token plainly.
   - Every design seam (S-44Δ … S-53, S-22Δ3, S-15Δ4) names a delivering task, and the cut matches the design's §4 pointer.
   - The `#ActiveTime` move to `app/` is in build-map v1.9 and in the WO's Contract-deltas row. It is not a directory-theatre token.
2. **Every done-check can be green at its step: holds.**
   - Each red window is named: `kernel/src/test` 052→053; `play/src/test` and `app/src/test` 052→054; `app/src/test` and `app/src/androidTest` 060→061. Run 2 of the JVM line leaves the red module out.
   - The kit stages land before the seams they pin.
   - The Done-checks of 053, 054, 056, 057 and 059 name only tests that are in the tree at that step.
     - TASK-054 names `SettingsControllerKitTest`, `ResetRestartKitTest` and the three device classes from the existing tree.
     - TASK-059 does not name `SoundKitTest` or `FeedbackKitTest`; both land at 060.
     - TASK-056 runs `TopBarFitKitTest` and `GearKitTest`, which land with 055 in the same step.
   - The one-step landings (055+056, 058+057) are written out in the briefs, step by step, so their Done-checks can be green.
3. **Declared inputs land together: holds**, with S3 below.
   - I grepped the moved-› assertions independently:
     - Play JVM: `SafeAreaKitTest` :176, :202 and :285; `ControlsScaffoldTest` :55 and :66; `SafeAreaScaffoldTest` :40 and :101. All are in the plan, and every cited line number matches the tree.
     - Play device: `TopBarKitTest` :192, :264, :377 and :408; `DevAidScreenKitTest` :1059 and :1074. All are in the plan. `OverviewKitTest` :532 and :548 are a named non-edit.
     - `TouchScaffoldTest`, `DevButtonHitScaffoldTest` and `DecorationScaffoldTest` use `ScreenGeometry` but pin no right-edge coordinate. The DEV-button test checks the counter and cell hits only. Green without an edit.
   - `AssetLicencesKitTest` pins `## Pictures, fonts, sounds`. TASK-057 keeps that heading and adds `## Sounds`, so there is no window.
   - The closed-world texts read `R.string` as a whole (`KitApp.appStrings`, :1913). Strings and their admission therefore land together in TASK-059.
   - `app/build.gradle.kts` already puts `:store`, `:kernel` and `:play` on the app test classpath. `ResetRestartKitTest` needs no build-file change.
4. **Who edits tests: holds.** Seam note 6 and every implementer brief end "never edit a test file". The three scaffolds are TASK-056's, and `ScreenSeamScaffoldTest` is edited only if red. The grep finds no assertion that S-53 moves in it, so this is consistent.
5. **Briefs point, never paraphrase: holds.** Briefs cite REQ files, seam rows and `decisions.md` rows by the labels E1–E13, C1–C6 and PL1–PL3a. The test-author briefs exclude implementer briefs and DI-1…DI-10, and put a positive control on each "nothing" line. The implementer briefs exclude the kit briefs.
6. **Channels and floor: named.**
   - The WO's channels are all in the channel table: Python, the JVM two-run line, V-01 with its positive control, the floor `Phone_API_31` first, then dev, then the tablet, and the death pair on floor and dev.
   - The minSdk and floor channel is named, and every device task runs the floor first.
   - The toolchain proof exists (run log g3-*).
   - The device-time rules are carried: one background script per image, one emulator at a time, judged by the XML, a 40-minute watchdog.
7. **REQ-033.A1's oracle** honours the oracle-cost rule. There is one door, driven with the switch off and a positive control with it on. There is no source scan, no caller inventory and no allow-list. `FeedbackKitTest` pins the map and the gate on the JVM, and each "nothing" has a control.
8. **Mode.** Full (b) is stated with its reason, a governed interface that changes (`IActiveTime` gains `resetPlayTimes`). The design adds no progress-file field, so reason (c) and hard-stop 2 are not raised. The reason is honest.

Task shape: no `[P]`, and the dropped design `[P]` pair is justified by the shared APK inputs. The hardest part, `resetAll`'s order, is TASK-054 and comes right after its one dependency. The Implements bullet holds IDs only, and plan-check reads 16.

## Findings

| ID | Severity | Target | Observation | Proposed resolution |
|---|---|---|---|---|
| S1 | Should | TASK-061 (Done-check 4) vs PL3a | The 0.21 amendment (PL3a) says a `KitApp` change re-opens every `KitApp` class's verdict, so TASK-059 runs `PhoneLayoutKitTest`, `TabletLayoutKitTest`, `DevAidAppKitTest` and `DevPromptKeyboardKitTest` alone. TASK-060 edits `KitApp.kt` again (the feedback door, null again through `onRestore`). TASK-061 also wraps all of `setContent` in the `LocalSoundEffect` provider. Its Done-check (4) lists five classes and none of the four. They first run again at TASK-062, so a regression there costs the whole-set run, the very run the plan tries to make a single pass. | Add the four classes (floor and dev, alone) to TASK-061's Done-check and to the channel table's TASK-061 row. About 40 min of device time. Or state the reason for leaving them to TASK-062. The orchestrator can edit this at G3. |
| S2 | Should | TASK-055 / TASK-056, TASK-057 / TASK-058 (Depends on) | Both pairs carry a Depends cell that points at its partner (056 on 055; 055 on 056's main code; 058 on 057's files; 057 on 058). Read as a graph the order is a cycle. Only the prose "(one step)" and the briefs' landing sequence resolve it. A tool or a reader that sorts by Depends cannot. | Give each pair one direction in the Depends cell: the main code first, then the kit "lands with" it. For example 056 → 054, 055 → 054 + "lands with TASK-056", 057 → 056, 058 → 056 + "lands with TASK-057". The briefs' sequence stays as written. |
| S3 | Should | TASK-058, TASK-057 (Inputs) | `SoundAssetsKitTest` reads five files outside its test folder: `app/src/main/res/raw/sfx_*.wav`, as well as `ASSET-LICENCES.md`, which is listed. The wavs appear only as "TASK-057's files" in the Depends cell and not in either Inputs cell. The same-step landing makes this safe in fact, but the Inputs rule asks for the files in the cell. | Add `app/src/main/res/raw/sfx_{pick_up,turn,lock,home,solve}.wav` to the Inputs cells of TASK-058 and TASK-057 (057 as a file it changes). |
| S4 | Should | TASK-059 (task shape) | TASK-059 is the plan's largest row. One context writes the screen, about 17 strings with `fi` references and the activity layer. It then runs seven classes alone on three images, four more `KitApp` classes alone on two, and the death pair. The plan puts this at about 2 h 20 min, and a failing class re-runs on top of that. There is no declared split point. | Carry a split in dispatch with no plan change: (a) the code, the JVM line and the floor in one context; (b) dev, tablet and the four `KitApp` classes in a resumed one. |

Counts: 0 Blockers · 4 Should · 0 Notes.

## Handoff

- **Verdict:** `forward` (plan-check GREEN; no Blocker stands).
- **Result line:** `plan-check: GREEN (0 blocker(s), 10 warning(s))`; the warnings are triaged above as the A-number product and the `Tangrams/*.json` glob, both false positives.
- **Findings:** 0 B / 4 S / 0 N.
- **Artifact:** `reviews/WO-007-plan-review.md`.
- **Next:** G3. S1 (TASK-061's Done-check), S2 (the Depends cells) and S3 (TASK-057 / 058 Inputs) are orchestrator edits. S4 is a dispatch note. I made no edit to the plan.
