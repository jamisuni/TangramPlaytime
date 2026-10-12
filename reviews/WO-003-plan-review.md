# Review — plan · WO-003

**Date:** 2026-10-11  ·  **Reviewer:** fresh context (Sonnet)
**Inputs:** workorders/WO-003.md (Scope, Mode, Constraints, Log); tasks.md § WO-003 (intro, S-1..S-16, tag table, touch routing, TASK-009..015, change-log 0.7); workorders/WO-003-briefs.md (dispatch + channel tables, TASK-010 brief in full, others by header and token rules); decisions.md WO-003 rows; build-map.md v1.3 rows #GoHome / #PuzzleArt / #PuzzleMini; REQ-011/012/013/020/045 acceptance; app/build.gradle.kts.

## plan-check output (verbatim)

```
plan-check: WO-003 — 7 task(s) (0 [P]), mode Light, 32 in-scope acceptance ID(s)
  warn: tasks cover 16 acceptance ID(s) outside the WO's scope: REQ-002.A2, REQ-002.A3, REQ-011.A3, REQ-012.A3, REQ-014.A3, REQ-015.A3, REQ-016.A3, REQ-017.A3
plan-check: GREEN (0 blocker(s), 1 warning(s))
```

Warning triaged: it is the known REQ x A-number cross-product (proposals.md, WO-002 entry). By hand: no Covers cell names REQ-002.A2, REQ-039.A2 or a non-existent A3 (Covers cells list explicit IDs only); the 32 explicit IDs of the Implements bullet each sit in some Covers cell (TASK-011 carries 29, TASK-009/010 add REQ-002.A1, REQ-018.A3, REQ-013.A1). The briefs forbid tokens on REQ-002.A2 / REQ-039.A2 (TASK-009 brief line 101, TASK-011 brief line 197, handoff line 576).

## Verdict

**forward** — no Blocker. Coverage both ways holds; every done-check names a file plus IDs that exist at its step (the three red windows are declared and no verdict is asked inside one); Inputs are declared on every task, and the shared `Tangrams/*.json`, `PuzzleLibraries.kt`, `LockEngine.kt` inputs are changed by no task of this WO.
0 Blockers, 3 Shoulds.

## Checklist applied

- [x] **Plan review (three checks):** coverage both ways (script + hand); done-checks green at step (TASK-012 needs only S-10..S-14 for the whole device kit to compile; `app/build.gradle.kts` already carries the androidTest deps; FloorChannelTest keeps passing until TASK-015 because MainActivity is untouched before it; REQ-012.A2 and REQ-013.A2 in DragKitTest need no ghost or badge, so TASK-013 can be green); inputs land together.
- [x] **Task shape:** one implementer context per row, explicit depends-on, hardest first (state holder, rendering, touch, decorations, wiring last), no `[P]` and justified; staffing class marked per row.
- [x] **Briefs point, not paraphrase:** read lists name artifacts and sections; the seams live once in tasks.md.
- [x] **G3 proofs:** JVM, floor (`minSdk 26`) and dev channels proven in the run log; the release-verifier channel's deliverable is TASK-015.
- [x] **Mode:** Light; the WO states none of the four Full reasons (reasons (a) and (b) argued away in § Mode, consistent with WO-002).

## Findings

### F1 — TRACE · Should · target: tasks.md TASK-015, Depends on
- **Observation:** TASK-015 lists `TASK-011, TASK-012`, but its Done-check (3) requires the whole play device set green and the release verifier needs the finished `:play`; the dispatch table gates step 7 on step 6. Row order guarantees it; the Depends cell does not say it.
- **Proposed resolution:** Depends on `TASK-011, TASK-012, TASK-014`.

### F2 — QUALITY · Should · target: tasks.md TASK-015, shape
- **Observation:** One row carries four unlike deliverables in one context: the `:app` wiring + strings + FloorChannelTest adaptation, the release verifier `v01_release_apk.py`, the `DebugCanary.kt` in `:play`, and `tools/device_reset.sh`. Only the wiring has a REQ (REQ-045.A1); the rest is guardrail work on a different channel. Mechanical and Opus, so tolerable, but one failing half hides the other's verdict.
- **Proposed resolution:** Keep one row only if the brief tells the implementer to land the wiring first and report the two Done-check halves separately; otherwise split verifier + canary + reset script into their own row.

### F3 — QUALITY · Should · target: briefs and tasks.md, "decisions.md line N" pointers
- **Observation:** The briefs and tasks.md point at decisions rows by line number ("decisions.md lines 29 and 55-60", "line 59"). The orchestrator appends the implementers' proposed rows and edits the file at close; a line number is a pointer that can drift.
- **Proposed resolution:** Cite rows by date + WO-003 + topic before dispatch, or state once in the briefs header that line numbers are as of HEAD 3fa75ac plus this tree.

## Handoff

- Station: Plan Reviewer (plan-reviewer, Sonnet) · WO-003 · Light
- Report: `C:/GitHub/AI/TangramPlaytime/reviews/WO-003-plan-review.md`
- Counts: 0 Blockers, 3 Shoulds, 0 Notes
- Status: **forward**
