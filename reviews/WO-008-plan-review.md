# WO-008 plan review (mechanical, one round)

**Reviewer:** plan-reviewer (fresh context)  ·  **Date:** 2026-10-20  ·  **Plan:** `tasks.md` § WO-008 (TASK-043 … TASK-051), `workorders/WO-008-briefs.md`  ·  **Mode:** Full (b)

## plan-check output (verbatim)

```
plan-check: WO-008 — 9 task(s) (0 [P]), mode Full (b), 9 in-scope acceptance ID(s)
  warn: tasks cover 6 acceptance ID(s) outside the WO's scope: REQ-005.A3, REQ-006.A2, REQ-006.A3, REQ-029.A2, REQ-029.A3, REQ-031.A3
  warn: TASK-043 / 045 / 046 / 047 / 048 / 049 / 050 / 051: Input Tangrams/*.json not in the tree and no task names it
plan-check: GREEN (0 blocker(s), 9 warning(s))
```

Warning triage:
- `Tangrams/*.json`: a false positive. The folder holds 106 puzzle files plus `README.md`, and no task changes it. The glob is a declared Input and is satisfied.
- The six out-of-scope IDs come from the REQ x A-number product. No Covers cell claims them. REQ-029.A2 and REQ-030 rule 3's list appear only as "→ WO-007".

## Hand checks (what the script cannot see)

1. **Coverage both ways: holds.**
   - Every in-scope ID has a tokened test in the code home of `build-map.md` v1.7:
     - REQ-005 A1..A2, REQ-030 A1..A3: `play/`, in `PlayTimeKitTest`.
     - REQ-029.A1: `kernel/`, in `ActiveTimeKitTest`.
     - REQ-031 A1..A2: `play/`, in `TimerKitTest`.
     - REQ-006.A1: `app/`, WO-006's `PlayThroughKitTest`.
   - Every task row names its REQs and IDs.
   - Every design seam (S-44 … S-48, the Δ rows, the `IProgressStore` KDoc Δ) names a delivering task that exists, and S-37Δ is the planner's own.
   - The design's write-back is done: §4 is superseded with a pointer, and the kit door of P6 is in §2.
2. **Every done-check can be green at its step: holds**, with S1 and S2 below.
   - The kit-first order is consistent. Each red window is named, and the briefs' JVM line leaves the red module out.
   - TASK-046's check names `DevAidKitTest` REQ-046.A3 and `PlayTimeKitTest`, both in the tree by then.
3. **Declared inputs land together: holds.**
   - The `PlayTexts` breakage is exactly the five F4 files. I confirmed them by grep: `KitScreen.kt`, `CounterDigitsKitTest.kt`, `DecorationScaffoldTest.kt`, `ScreenSeamScaffoldTest.kt` and `ControlsScaffoldTest.kt`.
   - They are edited in TASK-048, in one step with TASK-047, which adds the field and the `bestTimeText` signature.
   - The "60:00" pin (`ControlsScaffoldTest.kt:119`) is a corrected check, in the same step as its cause.
   - TASK-045's `closedWorld()` correction forms m:ss itself, so it does not need TASK-047's new string resource.
   - TASK-047 lists `strings.xml` (en and fi) as an Input, and it is the task that changes them.
   - TASK-043 changes `device_reset.sh` and the build files before the device steps that read them.
4. **Test edits belong to the acceptance-test author: holds.**
   - Seam note 5 and P4 say so. Every implementer brief ends "never edit a test file" and routes a break to `recirculate → acceptance-test-author`.
   - `tools/device_reset.sh` is not a test, and TASK-043 owns it.
5. **Briefs point, never paraphrase: holds.**
   - Briefs cite REQ files, seam rows and `decisions.md` row labels.
   - The test-author briefs read declarations only, exclude implementer briefs and DI-1..DI-10, and carry per-line positive controls.
   - The implementer briefs exclude the kit author's briefs.
6. **Channels: match the WO.**
   - The WO's Verification channels are all in the channel table: Python, the JVM two-run line, the floor `Phone_API_31` first, then dev, then tablet, V-01 plus its positive control, and the death pair on the floor and dev.
   - Device-time rules are carried: one background script per image, one emulator at a time, judged by the XML, a 40-minute watchdog.
   - The floor/minSdk channel is named, and the device tests have it.
7. **Contract deltas: named.** TASK-044 writes `IActiveTime.kt` and rewords the `IProgressStore` KDoc lines 19–20. Both paths are in `guard.json`'s notify list. The orchestrator re-freezes the baseline after TASK-044 (WO § Contract deltas and the tasks.md intro).
8. **Mode:** Full (b) is stated with its reason, a new governed interface. The design adds no progress-file field, so reason (c) is not raised.

Task shape: no `[P]`, and the depends-on cells are explicit. The hardest part, the fold-and-save order in TASK-046, comes early.

## Findings

| ID | Severity | Target | Observation | Proposed resolution |
|---|---|---|---|---|
| S1 | Should | TASK-048 (Depends on, Done-check) | The Depends cell says TASK-046 only. The Done-check's compile leg (2) is green only with TASK-047's main code in the tree. Dispatch step 5 orders this (047 writes main code, then 048 lands), but the row's own dependency does not show it. The planner's note says it is "one step". | Put "TASK-046; TASK-047's main code (one step)" in the Depends cell. The orchestrator can do this at G3. |
| S2 | Should | TASK-043 (Done-check 3, brief (a)) | Check (3) requires a crash or not-responding window to be provoked on the floor. The brief adds an escape: "if no route provokes one within about 15 minutes, record the attempts and keep the item". A check that can end "attempted" is not binary. | State the fallback outcome in the Done-check itself, for example "provoked, or unprovoked after the logged attempts, ruled at receive". Alternatively, make a non-provoked control an explicit receive decision. |
| S3 | Should | TASK-043 (task shape) | This is the largest row: a script change, four build lines, lint, three images with every class alone (≈3–3.5 h of device time), the death pairs, and eight API 26 re-judgments, all in one context. There is no declared split point. WO-010's plan review (S3) carried the same risk in dispatch. | Carry a split in dispatch: (a)–(c) plus the floor in one context, dev and tablet plus the re-judgments in a resumed one. No plan change is needed. |
| S4 | Should | `build-map.md` row `#TimerDisplay` | Its status is `Planned`, while `#PlayTime` and `#ActiveTime` read `In progress (WO-008)`. WO-008 builds `#TimerDisplay` (REQ-031). | The orchestrator sets `In progress (WO-008)` on that row at the next build-map edit. It is a notify-tier note, not a gate. |

Counts: 0 Blockers · 4 Should · 0 Notes.

## Handoff

- **Verdict:** `forward` (plan-check GREEN; no Blocker stands).
- **Result line:** `plan-check: GREEN (0 blocker(s), 9 warning(s))`, with all warnings triaged above as false positives.
- **Findings:** 0 B / 4 S / 0 N.
- **Artifact:** `reviews/WO-008-plan-review.md`.
- **Next:** G3. S1 (the TASK-048 Depends cell), S2 (TASK-043's Done-check wording) and S4 (the `#TimerDisplay` status) are orchestrator edits. S3 is a dispatch note. I made no edit to the plan.
