# WO-009 plan review (mechanical, one round)

**Reviewer:** plan-reviewer (fresh context)  ·  **Date:** 2026-10-22  ·  **Plan:** `tasks.md` § WO-009 (TASK-063 … TASK-071, change-log row 0.23), `workorders/WO-009-briefs.md`  ·  **Mode:** Light

## Script output (verbatim result lines)

```
plan-check: WO-009 — 9 task(s) (0 [P]), mode Light, 10 in-scope acceptance ID(s)
plan-check: GREEN (0 blocker(s), 8 warning(s))
trace-check: note: WO scope from workorders/WO-009.md § Scope: 10 acceptance ID(s)
trace-check: RED (WO WO-009 scope)   (expected: 2 of 10 covered; REQ-001.A2, REQ-048.A1, REQ-048.A2 await the owner's signature; REQ-039.A2, REQ-042.A1, REQ-048.A3, REQ-048.A4, REQ-002.A2 have no test token until the kit lands)
```

The WO scope count (10) equals plan-check's in-scope count (10).

Warning triage:
- Seven `Tangrams/*.json` "not in the tree" lines are the known false positive. The glob is satisfied by the 106 files, and no task changes them (TASK-063 only reads).
- The "40 acceptance IDs outside scope" line is the REQ x A-number product. The out-of-scope IDs (REQ-010.A2, REQ-040.A1, REQ-045.A1 and the like) appear only as "kept green" or "among the green" in Done-checks. No Covers cell claims them.

## The five specific checks

1. **Owner-evidence IDs: yes.**
   - The briefs say it outright: "Never write REQ-001.A2, REQ-048.A1 or REQ-048.A2 in a test file" (briefs line 176), and seam note 4 says the same.
   - TASK-064 Done-check (3) and TASK-069 Done-check (2) are greps that must find none of the three.
   - Each lands on TASK-068 (the drafts) and TASK-069 (untokened pins) in the Covers cell and the coverage table.
   - The `owner-evidence.md` rows exist (3 rows, 0 signed, per trace-check).
2. **JVM line green and the expected red: mostly.**
   - Named by test: TASK-065 (1) and TASK-070 (2) name `ReleaseSetKitTest`'s release legs (REQ-042.A1, A2, A3).
   - TASK-071 (2) says "run 2 without `--continue` again". TASK-070 and TASK-065 use `--continue`.
   - TASK-064, TASK-066, TASK-067, TASK-068 and TASK-069 say "only the expected red" and do not repeat the test. See S2.
   - No other red is excused. The only other red leg, `ReleaseMarksKitTest` REQ-039.A2 at TASK-064, is named in that row.
3. **Hard limits: yes.**
   - Briefs lines 86, 273, 301 and 400 forbid `mark` or `unmark` on `Tangrams/`, and a puzzle-file edit. The only `Tangrams/` write is `README.md`.
   - TASK-065's positive control runs `mark` on a scratch copy of the repository, which seam note 7 allows.
   - TASK-068 Done-check (3) searches the tree for `signingConfig`, `storeFile` and key-store extensions.
   - S-62 signs only a scratch copy with the local debug key.
4. **New paths and the hashed trees: yes, with one edge.**
   - Seam note 5 names `release/`, `PRIVACY.md`, `Spec/icon/`, `tools/release_install.sh` and `tools/store_screenshots.sh`.
   - The run log header already extended the trees on 06:00 (`trees9.sh`).
   - The new `*.py` tools, `tools/tests/` and the icon resources under `app/src/main` are inside the existing definitions.
   - The edge is S4.
5. **Red windows: yes.**
   - TASK-064: REQ-039.A2 red on today's tree, named in (2) and in the intro.
   - TASK-066: `LauncherIconKitTest` red until TASK-067, named in the intro.
   - No task runs a set for a verdict inside either window.
   - TASK-066 compiles only (no device run).
   - TASK-067 (4) runs `LauncherIconKitTest` before the change as the in-situ fixture, which is a deliberate red proof and not a verdict.

Other hand checks:
- **Coverage both ways:** holds. All 10 IDs land on a task and in the coverage table, each with its code home (`play/` for REQ-002.A2 and REQ-039.A2, `content/` for REQ-042) or an owner-evidence row. Every task row names its REQs.
- **Order and inputs:** the kits land before the seams they read (064 before 065; 066 before 067; 069 after 067 and 068). Every file a test reads (`ASSET-LICENCES.md`, `release/`, `PRIVACY.md`, the icon resources, the strings files) is in the Inputs cell, and the changing task is the same or an earlier one. `tools/device_reset.sh` exists in the tree.
- **Mode:** Light with the four reasons denied (no new module; `IPuzzleLibrary` and the format consumed, not changed; no persistence change; signing and accounts stay out). The reason is honest.
- **Shape:** no `[P]`, and each pair shares an input. The hardest part (TASK-065) comes right after the mark command and its kit. The Implements bullet holds IDs only. The briefs point at S-rows and R-titles.

## Findings

| ID | Severity | Target | Observation | Proposed resolution |
|---|---|---|---|---|
| S1 | Should | TASK-065 (Depends on) | Depends is `TASK-064` only. The Inputs cell lists `tools/mark_puzzles.py` (the positive control marks a copy with it), and the tool lands in TASK-063. The order is linear, so it holds in fact. The Depends cell does not say it. | Add `TASK-063` to TASK-065's Depends cell. |
| S2 | Should | TASK-064, 066, 067, 068, 069 (Done-checks) | Five rows say "only the expected red" or "the expected red" without naming the test. TASK-064 says "as today's unfiltered APK makes them" with no leg list. TASK-066's row also does not state the `LauncherIconKitTest` window that the intro gives. A reader of one row alone cannot tell which red is excused. | In each, write "the expected red: `ReleaseSetKitTest`'s release legs (REQ-042.A1, A2, A3 and the release-set pins)". In TASK-066 add "`LauncherIconKitTest` fails at run time until TASK-067". |
| S3 | Should | TASK-065 (Covers / Done-check 1) | REQ-039.A2 is "green" at TASK-065 because the release APK carries no puzzle file. That green is vacuous: the empty set trivially has no unmarked file. The real verdict is TASK-071. The crafted-fixture legs do prove the check can fail. | State in the Covers cell that the REQ-039.A2 verdict is taken at TASK-071 on the marked set, and that the green here is a no-fixture-fail result. This is a wording change; the Done-check holds. |
| S4 | Should | TASK-063 (`Tangrams/README.md`), `review-notes.md` | The README edit sits outside every hashed tree: the bundle tree counts only the 106 json files. The same holds for `review-notes.md` (TASK-068, TASK-071). Neither is named in seam note 5, so a doc-only change leaves no tree-hash trace. | Either name both as deliberately unhashed in seam note 5, or add `Tangrams/README.md` to the product tree definition. The orchestrator's call at G3. |

Counts: 0 Blockers · 4 Should · 0 Notes.

## Handoff

- **Verdict:** `forward` (plan-check GREEN; no Blocker stands).
- **Result lines:** `plan-check: GREEN (0 blocker(s), 8 warning(s))`; trace-check "WO scope … 10 acceptance ID(s)", equal to plan-check's 10, RED as expected until the kit, the build and the owner's signatures land.
- **Findings:** 0 B / 4 S / 0 N.
- **Artifact:** `reviews/WO-009-plan-review.md`.
- **Next:** G3. S1 to S4 are wording or Depends edits for the orchestrator; none changes a task, an ID or the order. I made no edit to the plan.
