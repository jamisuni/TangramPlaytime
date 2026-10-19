# Plan review — WO-010 (Light) — plan-reviewer, 2026-10-19

## plan-check output (verbatim result lines)

```
plan-check: WO-010 — 10 task(s) (4 [P]), mode Light, 11 in-scope acceptance ID(s)
  warn: tasks cover 14 acceptance ID(s) outside the WO's scope: REQ-007.A3, REQ-024.A2, REQ-038.A2, REQ-038.A3, REQ-039.A2, REQ-039.A3, REQ-040.A3, REQ-041.A3
plan-check: GREEN (0 blocker(s), 1 warning(s))
```

Blockers by the script: 0. Verdict: **forward**.

## Triage of the known warning — harmless
- The 14 "outside the scope" IDs come from the script's Implements x Covers product (a row that Implements REQ-007 and covers REQ-007.A1 is also read as covering REQ-007.A2/A3, and so on). No Covers cell claims REQ-007.A3, REQ-038.A2/A3, REQ-039.A2/A3, REQ-040.A3 or REQ-041.A3. REQ-024.A2 is named by TASK-034 as "groundwork, no token", which is correct. REQ-038.A2, REQ-039.A2 and REQ-042.A1 are declared out of scope in the WO. No coverage gap either way.

## Own reading (what the script cannot see)
- **Rule 1, coverage both ways.** All 11 Scope IDs land on tasks (Q9 table and the Covers cells agree). Every task names its REQs.
- **Rule 2, done-checks green at their step.**
  - I confirmed the REQ-040.A2 test exists at `PuzzleLoaderKitTest.kt` line 320 and `ScreenSeamScaffoldTest` has "3 / 13".
  - `CounterDigitsKitTest.kt` is absent, as it should be: TASK-033 creates it.
  - TASK-033's declared red (the pin fails until TASK-034) is stated in its own done-check and in the intro, and nothing else is red. TASK-034 turns it green. The run-time red is honest.
  - TASK-036's done-checks (4) and (5) run the JVM line and two floor classes at its own landing, on stub art. That is before its own flux window opens, so it is consistent, and none of them is a REQ-039 verdict.
  - TASK-037..040 run only the Python tools, which exist in the tree (the colour report and `show` are TASK-036's).
  - TASK-041's pin is green only on the tree after the four batches, and its Depends-on says so. Its pin reads `ASSET-LICENCES.md` (TASK-036) and `Tangrams/*.json` `art` (the batches), both earlier.
  - The code review between TASK-041 and TASK-042 breaks no done-check. TASK-042 makes no change of its own, and a review fix before it only ahead of the whole-set runs.
- **Rule 3, inputs land together.** The two changed tests land before or with the files that break them: the REQ-040.A2 correction in TASK-033, before the first import in TASK-036; `test_tangram_geom.py` narrowed inside TASK-036 itself. The shared-file table gives one writer per file. The batches write distinct `art` objects and distinct sheets. I confirmed `render_puzzle.py` writes only `<out>/<stem>.svg` per file (no index file), so single-file invocations do not collide.
- **`[P]` TASK-037..040.** No dependency (Depends-on "—"; the lane order carries it), no shared file, and no shared declared input (four distinct batch lists, no Gradle, per-batch scratch). The three-part rule holds. See S1 for one done-check line.
- **Shape and order.** Hardest first is argued honestly: TASK-036 carries the risk and goes before any picture, while 034/035 are cheap preconditions. TASK-035 must precede TASK-036 for the 13-puzzle baseline, and TASK-036 depends on it.
- **Briefs point, not paraphrase.** The briefs cite S-n rows, Q rows and decisions rows by title. All 25 cited bold titles exist in `decisions.md`, including R1..R10 (dated 2026-10-19) and the three 2026-10-18 rows. The channel table is a command list, not a restatement of numbers.
- **Channels and proof.** Python, JVM (two-run line), eyes, V-01, and the floor `Phone_API_26` (minSdk 26), dev and tablet images are all named, and proofs g3-py, g3-jvm, g3-v01, g3-dev, g3-floor, g3-tablet and g3-eyes are in the run log. The floor channel (the minSdk channel) is named first for every device step.
- **Mode.** Light, with all four reasons answered "none": no new module (a tool in `tools/`), the governed interface and format consumed as locked, no persistence change, no hard-stop domain. This is honest, and the size is rightly not given as a reason.

## Findings

| # | Category | Severity | Target | Observation | Proposed resolution |
|---|---|---|---|---|---|
| 1 | Done-check can be green | Should | TASK-037..040 (4) | "`git status` shows only the listed files and their new sheets changed" is run by four implementers at once in one working tree, so each sees the other three batches' changes plus TASK-036's. Literally it cannot be green during the fan-out. | Reword to "`git diff --name-only` for this batch's own files shows only `art` changes", or "no file outside this batch's list and its sheets was written by this task", or give each batch its own worktree. |
| 2 | Declared inputs | Should | TASK-036 Inputs | The Q7 table assigns to TASK-036's implementer any scaffolding test the first imported full puzzle breaks (for example `DecorationScaffoldTest` and `DevButtonHitScaffoldTest` use `first { FULL }`, `DevAidScaffoldTest` uses `puzzles.first()`), but the Inputs cell and Description list only `test_tangram_geom.py`. Done-check (4) says "the scaffolding tests" green without naming them. | Name the play and content scaffolding tests in the Inputs cell (read, and changed if broken), so the tests the task may edit are declared with it. |
| 3 | Task shape | Should | TASK-036 | One context carries the converter and its test, about 93 data files, three prose files, a test narrowing, and a device step with three measurement runs. It is defensible as the hardest-first task (every later task depends on its output), but it is the one task where an overrun is most likely. | Keep the order. Note in the dispatch that the floor step (5) may be split off as its own implementer if the context runs short, with the same Inputs and done-check (5). |

Counts: Blockers 0, Shoulds 3, Notes 0.

Report path: `C:/GitHub/AI/TangramPlaytime/reviews/WO-010-plan-review.md`
