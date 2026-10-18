# Plan review — WO-006 (Light) — plan-reviewer, 2026-10-17

## plan-check output (verbatim result lines)

```
plan-check: WO-006 — 6 task(s) (2 [P]), mode Light, 13 in-scope acceptance ID(s)
  warn: tasks cover 3 acceptance ID(s) outside the WO's scope: REQ-001.A2, REQ-006.A2, REQ-037.A2
  warn: TASK-027: Input app/build/outputs/apk/release/app-release-unsigned.apk not in the tree and no task names it
  warn: TASK-027: Input app/src/main | play/src/main | play/src/debug | kernel/src/main | content/src/main | store/src/main | contracts/src/main not in the tree and no task names it  (7 lines)
  warn: TASK-029 / 030 / 031 / 032: Input Tangrams/*.json not in the tree and no task names it  (4 lines)
plan-check: GREEN (0 blocker(s), 13 warning(s))
```

Blockers by the script: 0. Verdict: **forward**.

## Triage of the 13 known warnings — all harmless
- REQ-001.A2 / REQ-006.A2 / REQ-037.A2: no task's coverage cell claims them (each row names only .A1 or the in-scope A-lines); the script counts them through Covers x Implements. No coverage gap either way.
- Release APK path: a build output, written by run 1 of the JVM line before the test reads it; TASK-027's done-check (2) is ordered that way, and the stale-APK refusal is in the test.
- `*/src/main`, `play/src/debug`: directories that exist; the check looks for files. I confirmed the paths are real.
- `Tangrams/*.json`: a glob; the folder holds the bundle (`animals-cat.json`, `nature-mountain.json`, ...). Real.
- I also `ls`-confirmed every named existing file (device_reset.sh, the four strings.xml, ReleaseBuildKitTest, PuzzleLibraries, LockEngine, PlayLayout, Overview, DevAidHook, DevAidSheet, PlayLayouts, FloorChannelTest). `app/src/test` is absent, correctly: TASK-027 creates it.

## Own reading (what the script cannot see)
- Rule 1: every in-scope ID lands on a task; every task names its REQs. TASK-027/029 correctly claim "groundwork, no token" for the lines proven on the device at TASK-031. OK.
- Rule 2: done-checks exist in the tree at their step. TASK-027's red window (`:kernel:test`, `:play:testDebugUnitTest` unresolved until TASK-029) is declared, and its done-check uses `:app:testDebugUnitTest --rerun` alone. TASK-029 turns that window green. TASK-030 is a compile-only check. TASK-031 names TASK-030's classes. TASK-032 names `LanguageKitTest`. OK.
- Rule 3: strings, tools script and bundle are changed by no task except TASK-028 (script); see S1.
- Shape: hardest first (029 under every screen); depends-on explicit; the two `[P]` tasks (027 kit files, 028 `tools/device_reset.sh`) share no file or input. Mode Light with a four-reasons answer of "none", honest: no new module (O-05/O-06 existing `:app`), governed interfaces consumed only, no persistence change, no hard-stop. G3 proofs exist in the run log (JVM, dev, floor, tablet channels; floor/minSdk 26 channel named).

## Findings

| # | Category | Severity | Target | Observation | Proposed resolution |
|---|---|---|---|---|---|
| 1 | Inputs / dependency | Should | TASK-030 | Inputs lists `tools/device_reset.sh` (TASK-028's output, "around every device run"), but Depends-on is TASK-027 only. Order and the brief's "lands once TASK-029 is green" make it safe in practice (028 runs in parallel with 027 and is done before 029 lands), but the plan does not state it. | Add TASK-028 to TASK-030's Depends-on, or add one line that landing after TASK-029 already implies it. |
| 2 | Task shape | Should | TASK-029 | One task spans four seams (S-31..S-34) over `:kernel` and `:play`, including the debug-aid files. Defensible as one context (the geometry under every screen), and the dependency order is clean. If the implementer's context overruns, the natural split is S-34 (`LayoutClass.kt`, kernel) first, since TASK-031 consumes it. | Keep; note S-34 as the cut line in the brief if the implementer reports pressure. |

Counts: Blockers 0, Shoulds 2, Notes 0.

Report path: `C:/GitHub/AI/TangramPlaytime/reviews/WO-006-plan-review.md`
