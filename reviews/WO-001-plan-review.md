# Review — plan · WO-001

**Date:** 2026-10-11  ·  **Reviewer:** fresh context (plan-reviewer)
**Inputs:** `tasks.md` § WO-001 · `workorders/WO-001-briefs.md` · `workorders/WO-001.md` · `designs/WO-001-design.md` (§2, §4) · `reviews/WO-001-design-review.md` (F2–F4) · REQ-019/020/022 as counted by the script · kit folder `kernel/src/test/kotlin/app/tangramplaytime/kernel/` (only `KernelScaffoldTest.kt` exists) · SWDev `task-decomposition.md`, `plan-check` SKILL, `review-report.md`, `handoff-contract.md`

## Verdict

**forward.** 0 Blockers, 2 Shoulds.

## plan_check.py output (verbatim)

```
plan-check: WO-001 — 3 task(s) (1 [P]), mode Full (b), 7 in-scope acceptance ID(s)
  warn: tasks cover 5 acceptance ID(s) outside the WO's scope: REQ-020.A3, REQ-020.A4, REQ-022.A2, REQ-022.A3, REQ-022.A4
  warn: TASK-001: Done-check names no acceptance IDs
plan-check: GREEN (0 blocker(s), 2 warning(s))
```

Warning triage:
- Out-of-scope IDs: the script's REQ × A cross-product on multi-REQ rows (filed in `proposals.md`). The explicit Covers tokens name exactly the seven in-scope IDs. Accepted.
- TASK-001 names no acceptance ID: the reasoning holds. The WO scope is acceptance lines, and none can honestly be green before `LockEngine` exists. The Done-check is the G-04 geometry pins in `GeometryKitTest.kt`, which are real and runnable. The A2/A3 tokens on TASK-001's Covers cell are labelled groundwork and the same IDs land green on TASK-002, so there is no double claim. Accepted.

## Checklist

- [x] **Coverage both ways.**
  - All seven in-scope IDs (REQ-019 A1–A4, REQ-020 A1–A2, REQ-022 A1) are on TASK-002 and TASK-003.
  - Every task names its REQs.
  - The kit tokens are written only on `LockEngineKitTest.kt`.
- [~] **Done-checks green at their step.**
  - Each named file is first named by TASK-003, and nothing in the tree exists today except `KernelScaffoldTest.kt`.
  - The two-stage kit is sound. Gradle compiles all of `kernel/src/test`, so a `LockEngineKitTest.kt` calling `LockEngine()` in the tree during TASK-001 would stop TASK-001 from ever going green. Staging it avoids that.
  - The kit's own Done-checks (compiles once TASK-001 / TASK-002 lands) and the implementers' Done-checks depend on each other's landing. The table does not say so. See F1 and F2.
- [x] **Declared inputs land together.** Every Inputs cell is "—". The design says fixtures are transcribed and the kit reads no file (O-02). The TASK-003 brief lists `Tangrams/*.json` for authoring only and requires the hand-back to report files read at run time, expected none. The TASK-001 and TASK-002 briefs forbid `Tangrams/*.json`. No brief asks a test to read a file. Honest.
- [~] **Task shape.**
  - One implementer and one context each. TASK-003 resumes the same author context for stage 2.
  - One seam group per task. The design's §2 seam table has 12 rows, and each "Delivered by" is TASK-001, TASK-002 or TASK-003.
  - The order is hardest first: TASK-001, the validity predicate that the design names as the highest risk.
  - The deviation from design §4 is the two-stage kit. It is sound for the reason above.
  - `[P]` on TASK-003: there are no shared files, since the kit writes only under `src/test` and TASK-001 only under `src/main`. The Depends-on cell is incomplete. See F1.
- [x] **Briefs point, not paraphrase.**
  - They point by path and ID.
  - The restated constraints are the three design-review Shoulds. F2 is carried (the A3 fixture must include a pose that hidden anchors would lock). F3 is carried (`poseOf` returns the lowest turn, and REQ-022.A1 is reached through seven `findLock` calls with `isSolved` false before the seventh). F4 is carried (unique best score by more than 1e-9, and the "1.5 and √2" line is disregarded).
  - DI-1, DI-4 and DI-5 all appear by path in the TASK-001 brief's "Read exactly". DI-1 and DI-4 appear in the TASK-002 brief's "Read exactly" too. DI-5 is read only by TASK-001, which is correct because TASK-002 calls its seams.
  - The test-author brief excludes `kernel/src/main`, the TASK-001 and TASK-002 briefs, DI-1, DI-4 and `tools/tangram_geom.py`.
- [x] **Toolchain and verification channels (G3).** The proof is green and logged (WO-001 Log, 11:14). The JVM-only declaration is honest, since there is no UI and no device test. The floor channel (`Phone_API_26`) was proven at P2 and is deferred to WO-003 as the first WO with device tests, and the WO states this.
- [x] **Mode.** Full (b), "a governed interface is touched". Honest enough: `ILockEngine` is a registered notify-tier interface, this is its first implementation, and the WO says its shape may change. No `I*` is changed by the design, but a reviewable delta is expected to be possible. Not an inflated reason.

## Findings

### F1 — PLAN · Should · Depends-on cells for TASK-001, TASK-002, TASK-003
- **Observation:** The kit and the implementers depend on each other in both directions, and the table shows only `—` / `TASK-001`.
  - TASK-001's Done-check needs `GeometryKitTest.kt` (TASK-003 stage 1) in the tree.
  - TASK-002's Done-check needs `LockEngineKitTest.kt` (TASK-003 stage 2).
  - Stage 2 needs TASK-001 done.
  - `[P]` is therefore true only of stage 1's authoring, and the Depends-on column does not say that.
  - The dispatch table in the briefs does handle the order; `tasks.md` does not.
- **Proposed resolution:** In `tasks.md`, make TASK-003's Depends-on cell "stage 1: —; stage 2: TASK-001 done". Put a pointer on the TASK-001 and TASK-002 Depends-on cells ("Done-check also needs TASK-003 stage 1 / stage 2 in the tree"). Keep `[P]` for stage 1 only.

### F2 — PLAN · Should · TASK-002 brief and dispatch table (step 2)
- **Observation:** The dispatch table runs TASK-003 stage 2 and TASK-002 together at step 2. The TASK-002 brief reads `LockEngineKitTest.kt` as its visible acceptance test (item 11) and its Done-check is that file.
  - The TASK-001 brief has the fallback "if stage 1 is not in the tree when you finish, say so" and the orchestrator gate. The TASK-002 brief has no such line.
  - TASK-002 can therefore start before its Done-check file exists.
- **Proposed resolution:** Add the same line to the TASK-002 brief: if stage 2 is not in the tree, the implementer says so and the orchestrator runs the Done-check when it lands. Or, simpler, change step 2 so TASK-002 is dispatched only after stage 2 lands. In either case, say which one in the dispatch table.

## Handoff — Plan Reviewer · WO-001

- **Scope:** WO-001 plan: TASK-001..003; REQ-019 (A1–A4), REQ-020 (A1–A2), REQ-022 (A1).
- **Inputs read:** `plan_check.py` output; `tasks.md` § WO-001; `workorders/WO-001-briefs.md`; `workorders/WO-001.md`; `designs/WO-001-design.md` §2 and §4; `reviews/WO-001-design-review.md` F2–F4; kit folder listing; SWDev rules listed above.
- **Result:** `C:/GitHub/AI/TangramPlaytime/reviews/WO-001-plan-review.md`
- **Status:** forward
- **Traceability delta:** none
- **Notes:** The script is GREEN and both warnings are accepted. F1 and F2 both concern the table hiding the kit/implementer ordering, which the briefs already handle for TASK-001 only. The planner can fix both in a few lines.
