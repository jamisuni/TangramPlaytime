# WO-004 plan review

**Reviewer:** plan-reviewer (fresh, one round) · **Date:** 2026-10-15 · **Plan:** `tasks.md` § WO-004 (TASK-016 … TASK-022), `workorders/WO-004-briefs.md`
**Verdict:** 0 Blockers, 3 Shoulds, 0 Notes. **forward**

## plan-check output (verbatim)

```
plan-check: WO-004 — 7 task(s) (2 [P]), mode Full (c), 16 in-scope acceptance ID(s)
  warn: tasks cover 8 acceptance ID(s) outside the WO's scope: REQ-003.A3, REQ-026.A3, REQ-040.A2, REQ-040.A3, REQ-041.A2, REQ-041.A3, REQ-045.A2, REQ-045.A3
plan-check: GREEN (0 blocker(s), 1 warning(s))
```

**Warning triage.** The known REQ x A-number cross-product artefact (`proposals.md` 2026-10-11). I grepped `tasks.md` lines 159-210 for all eight IDs: none occurs in any cell. Dismissed.

## What I checked by hand

- **Coverage, both ways.** All 16 in-scope IDs land on tasks, and every task names its REQs. REQ-024.A2 and REQ-050.A2 are covered by TASK-019 (kit) and TASK-021 (green), as the briefs say.
- **Done-checks.** Each names files that exist at its step: TASK-017 and TASK-018 name TASK-016's files; TASK-020 names TASK-019's. No Done-check needs a window closed later. TASK-020 explicitly does not run `OverviewKitTest`, and TASK-020/021 do not run the app set. All four red windows (store/test, play/test, play/androidTest, app/androidTest) are declared with their closing task, and `StoreScaffoldTest` does not reference the deleted `Store.kt` placeholder.
- **Inputs.** `Tangrams/*.json` and the two `:content`/`:kernel` doors are listed wherever a test reads them. The `[P]` pair shares no declared input (TASK-017 reads only its own temp files).
- **`[P]` pair TASK-017 / TASK-018: honest.**
  - Neither depends on the other: they sit on opposite sides of the frozen `IProgressStore`, and the controller kit runs over a kit-owned double (design review F5(a)).
  - They share no file. The catalog already has `kotlinx-serialization-json` and `kotlinx-coroutines-core`, and `store/build.gradle.kts` already has coroutines on `implementation`.
  - The real predecessor is TASK-016 (their Done-checks name its files). The "—" Depends-on cell is a plan-check workaround; the order is carried by the rows, and the intro and briefs § Dispatch say so. Acceptable.
- **Mode.** Full (c), new persistence format, is one of the four listed reasons and is stated in the WO. Honest.
- **Channels.** JVM, dev image, floor image (minSdk 26 named), process death with positive control, and V-01 with positive control each have a running task. Toolchain and both device channels are proven in the run log (g3, f26, d37). Device hygiene is named.
- **Quantifier lines.** All of the 16 IDs that have a quantifier (REQ-003.A1/A2, 024.A2, 025.A1, 026.A1, 050.A3) name their adversarial path in a kit brief: every start state, every door, recreation, grid open, first frame after a solve, every other puzzle.
- **Kit briefs.** They point at artifacts and the REQ files as oracle, and say "pointers, not restatements". They forbid main-code bodies and implementer briefs.
- **Collisions the planner routed:** `DragKitTest` REQ-020.A2, `CornerPulseKitTest` REQ-051.A2, `PlayControllerKitTest`'s solved-placements pin, `BoardTrayKitTest`'s title node, `AppLaunchKitTest`, `FloorChannelTest`, `KitScreen` literal. All are correctly owned.

## Findings

### Blockers
None.

### Should 1 — In-tree collision missed: `SolvedPictureKitTest` (and `DecorationKitTest`) board captures
- **Target:** `tasks.md` § WO-004 intro, "In-tree tests this WO changes" (the TASK-019 bullet); TASK-019 Description; TASK-020 Done-check (2).
- **Observation:**
  - The plan names only "any board pixel sampler that runs In progress" and the two text/tag snapshot tests.
  - `SolvedPictureKitTest.kt` takes `unsolved = kit.capture(PlayTags.BOARD)` at lines 51 and 200, with all but the last piece placed. That is In progress, so the new Restart pill is drawn at the board's top-left + 10 dp.
  - It then calls `assertNothingOutside(p, unsolved, ...)` at lines 205-209, and compares the solved capture at line 61. Once Solved, the pill is gone.
  - Whether that pixel diff fails is for the test author to determine, but it is the likeliest red among the six WO-003 classes.
  - `DecorationKitTest.kt` also captures the board and tray while In progress (lines 267, 457, 497, 687).
  - TASK-020's Done-check requires those classes to stay green "as TASK-019 left them", but an implementer may not edit a kit file. A miss here costs a TASK-020 recirculation to TASK-019's author.
  - Brief § TASK-019 does tell the author to grep for others and gives `SolvedPictureKitTest` as an example, so this is not a Blocker.
- **Proposed resolution:** Name `SolvedPictureKitTest.kt` (lines 51/61/200-209) and `DecorationKitTest.kt` in the intro bullet and in the TASK-019 Description's "at least" list. Add to TASK-019's brief a request to state, in the hand-back, which of the six WO-003 classes were audited and found unaffected.

### Should 2 — TASK-022's Done-check does not require the skipped-phase negative run
- **Target:** TASK-022 Done-check (3); seam note 1.
- **Observation:**
  - Seam note 1 and the design call the skipped-phase misjudgement (`OK (1 test)` for an `assumeTrue`-skipped phase) the death channel's key risk.
  - Brief § TASK-022 "Done means" requires a negative run: phase 2 without `-e deathPhase` must not pass.
  - The row's Done-check, the DoD surface the orchestrator and Test & Verify read, lists only the passing run and the `--positive-control`. The positive control proves "phase 2 on a cleared store is judged failed". It does not prove "a skipped phase is judged not-passed".
  - The row is therefore weaker than the brief on exactly the failure the design review (F2) was raised about.
- **Proposed resolution:** Add to TASK-022 Done-check (3): "and a run of phase 2 without `deathPhase` (skipped by its guard) is judged not passed (script exit non-zero), on the dev image".

### Should 3 — TASK-019 Done-check (2) "a grep for `REQ-0` shows exactly" cannot be literally true
- **Target:** TASK-016 Done-check (2), TASK-019 Done-check (2).
- **Observation:**
  - The briefs' oracle rule requires every assertion to cite its source in a comment, including undotted rule citations such as "REQ-025 rule", REQ-019 and REQ-037.
  - Those contain `REQ-0`. A literal grep for `REQ-0` will therefore show more than the listed dotted IDs. Dotted IDs in a comment also count as tokens for trace-check.
  - The intended check is clearly the dotted `REQ-\d+\.A\d+` set.
- **Proposed resolution:** Reword both cells to "a grep for `REQ-0[0-9]*\.A[0-9]` shows exactly:", and add to both kit briefs: "cite rules without a dotted ID in comments; a dotted ID appears only where it is a token".

## Remaining judgments (no finding)

- **TASK-020 and TASK-022 are large.**
  - TASK-020 spans the bar, Restart, solved row, long press, S-3Δ screen side, S-23 and the `:app` strings.
  - TASK-022 combines the `:app` wiring with a shell harness.
  - Each is one context and one layer, with the riskiest item named first in its brief, and the cut follows the reviewed design. I do not recommend a split.
- **Order.** Hardest first is honoured: the save path (017/018) leads, the touch order follows, then the overview.
- **Kit briefs.** TASK-019's brief reads the TASK-020..022 rows, which describe approach at file-list level. This stops short of implementation bodies or implementer briefs, so I left it.
