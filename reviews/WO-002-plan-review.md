# Review - plan - WO-002

**Date:** 2026-10-11  ·  **Reviewer:** fresh context (plan-reviewer)
**Inputs:** `tasks.md` § WO-002 · `workorders/WO-002-briefs.md` · `workorders/WO-002.md` · `designs/WO-002-design.md` · `reviews/WO-002-design-review.md` · REQ-007/038/040/041/042/045 (via the Covers cells) · `content/src/test/**`, `kernel/src/test/**` · SWDev `task-decomposition.md` v0.5.

## Script output

```
plan-check: WO-002 - 4 task(s) (1 [P]), mode Full (b), 10 in-scope acceptance ID(s)
  warn: tasks cover 8 acceptance ID(s) outside the WO's scope: REQ-007.A3, REQ-038.A2, REQ-038.A3, REQ-040.A3, REQ-041.A3, REQ-042.A1, REQ-045.A2, REQ-045.A3
plan-check: GREEN (0 blocker(s), 1 warning(s))
```

The warning is the known cross-product. Explicit Covers tokens, read by hand: TASK-004 REQ-007.A1; TASK-005 and TASK-008 exactly the ten in-scope IDs; TASK-007 REQ-038.A1. REQ-038.A2, REQ-039.*, REQ-042.A1 and REQ-045.A2 appear only in the "Not covered" list, never as a token. Accepted.

## Verdict

**recirculate -> wo-planner.** 2 Blockers, 2 Shoulds, 0 Notes. The script is green; both Blockers are on what it cannot see.

## Checks that passed

- Coverage both ways: every in-scope ID lands on TASK-005 and TASK-008; every task names its REQs.
- Seam to task: every `Delivered by task` entry in design §2 and §4 is TASK-004, 005, 007 or 008, all present in `tasks.md`. TASK-006 appears only in the design review text.
- Design review F1 (Attribution line, TASK-005 brief, TASK-008 decision pin), F2 (geometry merged into TASK-005), F3 (three listings in TASK-004's Done-check; channel proven, WO Log 14:46, run log a0), F4 (WO-003; TASK-005 brief bars `:app`): all handled.
- TASK-004's Done-check can be green with no kit in the tree (the kit enters only after it). TASK-005's can be green with the kit present and `:kernel` added to the test classpath in the same task.
- Kit brief points at REQs, contracts, design §2/§3/§4, the CHG draft and decisions; it forbids main bodies and the other briefs. No paraphrase of implementation.
- Mode Full (b) with the reason stated honestly (first implementation of a governed interface; (a) disclaimed). No device tests, so no minSdk image is needed, and the brief says so.

## Findings

### B1 - PLAN-SHAPE · Blocker · TASK-008 `[P]` and `Depends on`
- **Observation:** TASK-008 is `[P]` with Depends on `—`. The rule needs no dependency, no shared files and no shared declared inputs. (1) Its Inputs cell is `Tangrams/*.json`, the same input as TASK-004; the plan serializes TASK-007 for exactly that shared input (intro, "Why TASK-007 is not [P]"), so the same rule applied to TASK-008 gives the opposite answer. (2) It is not independent of TASK-004: it may enter the tree only after TASK-004's Done-check is green (briefs, "Gate to the next step"), and its tests call TASK-004's `BundledPuzzles.read()`. It also shares the `content/src/test` source set with TASK-004's edit of `ContentScaffoldTest.kt`.
- **Proposed resolution:** drop `[P]` and set Depends on TASK-004, or split the row: the scratchpad draft (touches nothing in the tree) is parallel, the landing is a step that depends on TASK-004. Name the shared input as TASK-007's note does.

### B2 - DONE-CHECK · Blocker · TASK-008 Done-check
- **Observation:** TASK-008 sits before TASK-005 in landing order, but its Done-check is "compile in the tree once TASK-005 lands". At its own step the kit cannot compile (unresolved `PuzzleLoader`, `Rule`, `PuzzleLibraries`, and `LockEngine` until TASK-005 adds the `:kernel` test dependency), and the whole `content` test source set is red until TASK-005. The check is a pointer to a later task's green.
- **Proposed resolution:** give TASK-008 a check that can be met at its step: the three files exist in the kit folder; `grep REQ-0` shows exactly the ten tokens on the stated test files and none in `KitTexts.kt`; the only compile errors are unresolved references to TASK-005 seams. State the red window in the row (`:content:testDebugUnitTest` is not runnable between the kit landing and TASK-005) and leave the green verdict to TASK-005. Alternatively make TASK-008 depend on TASK-005 and land the kit as that task's first step.

### S1 - INPUTS · Should · TASK-005 Inputs
- **Observation:** TASK-004 and TASK-008 name the Tangrams folder listing as read by tests (bundle completeness; REQ-007.A1 "one per puzzle file"). TASK-005's Inputs cell omits it, though its Done-check runs those tests.
- **Proposed resolution:** add "the Tangrams folder listing" to TASK-005 Inputs.

### S2 - COVERAGE · Should · TASK-007 Covers
- **Observation:** TASK-007 lists `REQ-038.A1` in Covers, but its Done-check has no tokened test (the tool test is untokened; "the token lives in the kit"). A token in Covers with no asserting test of its own can mislead the trace gate.
- **Proposed resolution:** word the cell as groundwork, as TASK-004 does for REQ-007.A1, or drop the token and keep the prose.

## Handoff - Plan Reviewer · WO-002

- **Scope:** REQ-007 A1-A2, REQ-038 A1, REQ-040 A1-A2, REQ-041 A1-A2, REQ-042 A2-A3, REQ-045 A1; `tools/` corner-rule fix · governed touched: none (review only)
- **Inputs read:** as listed above
- **Result:** `reviews/WO-002-plan-review.md`
- **Status:** recirculate -> wo-planner (B1: TASK-008 `[P]` shares `Tangrams/*.json` with TASK-004 and depends on its landing; B2: TASK-008's Done-check cannot be green before TASK-005)
- **Traceability delta:** none
- **Notes for next station:** plan-check GREEN (0 blockers, 1 accepted warning). Both Blockers are edits to the TASK-008 row only.

## Second look (after change-log row 0.5)

Re-run: `plan-check: GREEN (0 blocker(s), 1 warning(s))` with 4 tasks, 0 `[P]`, mode Full (b), 10 in-scope IDs. The warning is the same accepted cross-product.

| Finding | Status | Evidence |
|---|---|---|
| B1 | Fixed | TASK-008 has no `[P]`, Depends on TASK-004; Inputs name `Tangrams/*.json` as shared with TASK-004, 005, 007. Briefs say "No task is `[P]`" and separate drafting (dispatch) from the plan dependency. Design §4 agrees. |
| B2 | Fixed | TASK-008 Done-check is met at landing: three files present, exactly ten tokens on the stated tests, none in `KitTexts.kt`, only unresolved TASK-005 seams as compile errors; the red window is stated in the row, the dispatch table (step 3) and the briefs. Green verdict is TASK-005's. |
| S1 | Fixed | TASK-005 Depends on TASK-004, TASK-008; Inputs add the Tangrams folder listing. |
| S2 | Fixed | TASK-007 Covers reads "groundwork, tool-side evidence; no token claimed", the same form as TASK-004. |

Nothing new found. TASK-005's Done-check can be green with the kit landed; TASK-004's needs no kit.

**Verdict: forward.** 0 Blockers, 0 Shoulds, 0 Notes open.
