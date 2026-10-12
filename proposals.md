# Framework Proposals — from TangramPlaytime

Append-only. **Blind-write** anything you want the SWDev framework to examine:
a recurring friction, a missing rule, an awkward gate, a question the playbook
should have asked. No framework approval is needed to write here; the framework
harvests this file during its own review.

> Never edit the framework directly. This file is the project's only outbound
> channel to it.

**What belongs here** (projects under-file — when in doubt, write it):
corrected checks — any test or review the pipeline had to fix, naming what
the checker over-specified; environment gaps that stalled a DoD item (a
missing browser IS framework signal, not "local setup"); a question the
playbook should have asked; a station skipped "per precedent"; any rule you
had to interpret rather than follow. A WO that closes with "no friction"
still writes the one-line entry saying so — silence is indistinguishable
from starvation.

---

## 2026-10-11 — req-lint false FAIL on Windows: validate.py crashes on cp1252 stdout

- **Context:** P1 step 1. `trace_check.py --project .` reported
  `FAIL: req-lint: validate.py reported errors — UnicodeEncodeError: 'charmap'
  codec can't encode character '→'`. The SwReqCollector validator prints
  `→` and `·` in its warnings; under a Windows console (cp1252) Python 3.14
  raises on `print`, the subprocess exits non-zero and trace-check reads that
  as lint errors. With `PYTHONUTF8=1` the same run is `OK — 0 errors, 66
  warning(s)` and trace-check is RED only for uncovered acceptance (correct
  for P1). Both frameworks are read-only to this project, so the note goes
  here (SwReqCollector's `framework-notes.md` lives inside the collection,
  which the build side never writes).
- **Proposal:** (a) trace-check: run `validate.py` and `views.py` with
  `PYTHONUTF8=1` (or `-X utf8`) in the subprocess environment, and surface a
  non-zero exit with no "error" lines as "tool crashed", not "lint errors";
  (b) for SwReqCollector: `sys.stdout.reconfigure(encoding="utf-8",
  errors="replace")` at the top of `validate.py` / `views.py`, or an ASCII
  fallback for the arrows. (c) `adapters/claude/CLAUDE.md` Windows page: add
  `PYTHONUTF8=1` to the "python, not python3" line. Also: trace_check.py:408
  emits a `DeprecationWarning` (`re.split` positional `maxsplit`) on 3.13+.
- **Severity:** medium (a false RED at the very first gate; a less careful
  session would have "fixed" the collection)

## 2026-10-11 — guard.json locks architecture.md before P2 has authored it

- **Context:** P2, first write of `architecture.md` (the placeholder from
  `project-template/`). The guard hook denied it: the scaffold's
  `contract_paths.locked` already contains `architecture.md` while no
  baseline exists and the profile is ai-mastered with G2 = `ai+inform`. The
  hook's advice ("the human adds this path to `unlocked`") assumes a
  human-governed G2. Resolved by opening `unlocked` under governance row 4,
  with a `decisions.md` row.
- **Proposal:** (a) the guard allows writes to a locked-tier path while
  `.swdev/contract-baseline.json` does not exist (nothing frozen can be
  broken before the first freeze), or (b) `swdev-guard.json` in the template
  ships `architecture.md` under `unlocked` with a comment "remove at G2", or
  (c) the hook reads `governance.md` row 4 and, under `ai`/`ai+inform`,
  logs instead of denying before the freeze.
- **Severity:** medium (a one-turn stall on every ai-mastered project's P2)

## 2026-10-11 — plan-check: three small misreads of a valid plan (WO-001 P3)

- **Context:** `plan_check.py --wo WO-001` on the wo-planner's three-task plan.
  (a) A task row whose Implements cell names three REQs and whose Covers
  cell lists explicit `REQ-NNN.A<n>` tokens produced `warn: tasks cover 5
  acceptance ID(s) outside the WO's scope: REQ-020.A3, REQ-020.A4,
  REQ-022.A2 …` — the script pairs every REQ in the row with every A-number
  in the row instead of reading the explicit tokens; the IDs it warned about
  do not exist. (b) The design's seam table used the column header `Task`
  instead of the template's `Delivered by task`; the seam check was
  **silently skipped** (no warning), so a seam row pointing at a task
  outside the WO would not have been caught. Fixed by renaming the header.
  (c) An escaped pipe (`\|`) inside a seam-table cell split the row and the
  seam check could not resolve it. Fixed by rewriting the cell.
- **Proposal:** (a) when a Covers cell contains explicit `REQ-NNN.A<n>`
  tokens, use them and skip the cross-product; (b) accept `Task` as the
  delivering column, or emit `warn: seam table found but no delivering
  column` when none matches; (c) honour `\|` inside table cells (split on
  unescaped pipes only). Also: `trace_check.py:408` DeprecationWarning
  still prints on every run (filed above).
- **Severity:** low (b is the one that hides a real check; a and c cost a
  reviewer's minute each)

## 2026-10-11 — WO-001 close: corrected checks none; wo-metrics same-day window

- **Corrected checks this WO: none.** The kit compiled first time against the
  tree, no assertion was reversed, no dispute was raised. Friction worth
  one line each: (a) the Test & Verify agent tried to write its report with
  a shell redirect and the approver denied it (D1 — working as designed; it
  used the Write tool); (b) the orchestrator typed three log stamps from
  memory and had to correct them from telemetry — handoff rule 7 ("stamped
  from a tool clock") held, but only on re-check.
- **wo-metrics window:** `wo_metrics.py --wo` takes the Log's first and last
  *date* as the window. When P1, P2 and WO-001 run on one calendar day, the
  WO's rows absorb the earlier phases' launches (12 vs 10), guard denials
  (1 vs 0), contract-deltas (5 vs 1), approver counts and the P2 device XML;
  `Product LOC` from `git log --numstat` is 0-based while nothing is
  committed. **Proposal:** window on the Log's first/last *timestamp* when
  rows carry `HH:MM`, or on the telemetry `wo` field if launches are tagged;
  count LOC on the working tree (`product_globs`/`test_globs`) when git has
  no commit in the window. The orchestrator annotated the rows by hand.
- **Severity:** low (the numbers are explainable, but the harvest would read
  them as WO-001's)

## 2026-10-11 — principle 16 has no signal for "a station is running"

- **Context:** P1 step 2. The requirements-reviewer subagent runs in the
  background; the orchestrator's turn ends while it works and resumes on the
  completion notice. Neither `All ready` (work is not complete) nor `Need
  help` (no question for the human) is true at that moment, yet principle 16
  says "never neither".
- **Proposal:** add a third verbatim line for the in-flight state, e.g.
  `Working, nothing needed from Jami` — the human still sees at a glance
  whether to act — or state that a turn ended by a background station carries
  no signal and the next one does.
- **Severity:** low

## 2026-10-11 — WO-002: plan-check reads Covers as a REQ × A-number product; `\|` in seam cells; req-lint dies on a cp1252 console

- **plan-check (a):** the script's comment says explicit `REQ-NNN.A<n>`
  tokens in a Covers cell override the REQ × A-number cross-product, but the
  code adds them to it. A row naming six REQs in Implements and ten explicit
  tokens in Covers is reported as covering 8 IDs "outside the WO's scope"
  (REQ-007.A3, REQ-038.A2 … REQ-045.A3) that no cell names. The warning was
  accepted by hand twice (planner, plan reviewer) — a warning that is always
  accepted trains everyone to accept warnings. **Proposal:** when a Covers
  cell contains any dotted token, use only the dotted tokens.
- **plan-check (b):** a `\|` inside a code span in a seam-table cell shifts
  the column split, so two seams read as "names no delivering task" until the
  planner rewrote `\|` as "or" in the design. (Same family as the WO-001
  entry's item c.) **Proposal:** split on unescaped pipes only.
- **trace-check req-lint:** `validate.py` prints a warning containing `→`
  (U+2192); on a Windows console with cp1252 stdout it dies with
  `UnicodeEncodeError` and trace-check reports `FAIL: req-lint` — a RED
  caused by the console, not the collection. Green with `PYTHONUTF8=1`.
  **Proposal:** trace-check runs req-lint with `PYTHONUTF8=1` (or captures
  stdout as UTF-8 bytes), and validate.py sets `sys.stdout.reconfigure(encoding="utf-8")`.
- **The `[P]` rule on read-only inputs (observation, no proposal yet):** the
  tools task (Python, touches nothing under `content/`) was serialized
  behind the Kotlin lane because it *reads* the same `Tangrams/*.json` the
  kit reads. The v0.18 rule exists for files a task *changes*; two readers
  of a file nobody changes could be `[P]`. Cost here: one short Sonnet
  dispatch of wall time. Worth a line in task-decomposition.md if it recurs.
- **Severity:** low (a and the req-lint one each hide a real check behind a
  false signal)

## 2026-10-12 — WO-003 close: six corrected checks, a mid-WO orchestrator hand-over, a station that reported a file it never wrote

- **Corrected checks: 6, all kit-side, zero product change for any of them**
  (orchestration §7, channel 2). Three classes:
  - **(A) The kit asserted more than the line mandates** (×2). A tray-row
    pin required every cell of a row to share one `top`. The seam said only
    "cells are square", and the product centres them. A REQ-023.A1 assertion
    required ≥ 1 % of the picture's base colour to show, but the sailboat's
    art covers its whole base, and nothing mandates a visible base. The
    rule "assert only what is mandated" already exists in
    `task-decomposition.md`, and it still recurred twice in one kit.
    **Proposal:** the acceptance-test-author agent file adds one self-check
    before landing. For every assertion that is not a REQ's literal, name
    the seam row or decisions row it comes from, or drop it.
  - **(B) The token's test did not cover the line's quantifier** (×2,
    found by the code review, N3 and F2). REQ-021.A1 says "*always* locks
    it where the preview was drawn", but its test released a still finger
    only; a move and a lift in one frame locked elsewhere. REQ-051.A1 says
    "pulse *once*", but its test watched one composition, and an activity
    recreation replayed the pulse. Both were reachable product defects that
    the green kit could not see. **Proposal:** for a line with a quantifier
    ("always", "once", "never", "every"), the kit names the adversarial
    path it exercises. For a UI line that means input batched in one frame,
    and an activity recreation (rotation, config change) over a state
    holder that outlives the activity. Put this in task-decomposition.md's
    kit checklist and in platform-notes/android.md § Compose test kit.
  - **(C) Fixture plumbing** (×2). A pixel sampler read two rows outside
    its own capture and crashed. One kit fixture took the badge disc to be
    the centre of its touch-area node, which it is not. **The root cause of
    the second one was the orchestrator.** My resume brief to the kit
    author restated a geometry fact ("the disc is centred in the node")
    instead of pointing at the decisions row, and the fact was stale. The
    worker contested it once and won. That is the framework's own
    watch-for, "paraphrasing spec content into briefs", and it happened
    anyway in a short follow-up message. **Proposal:** orchestrator.md's
    watch-for says "including follow-up and resume messages: cite the
    row, never restate a number or a position".
- **Light mode on the first UI WO (harvest note promised in WO § Mode).**
  The code review found two Shoulds that a design review asks about
  routinely: F1, touch areas that overlap across cells and pieces, and F2,
  one-shot cues that live in a state holder outliving the activity. Then
  the fix for F1 made the parallelogram un-turnable by a tap on its middle,
  because the 60 dp badge area covered 57–82 % of it. One more ruling and
  one more round followed: "the visible thing wins" on both surfaces, and
  the badge-precedence chg-draft was amended twice. The seam table the
  planner wrote in place of a design carried names, not geometry or
  lifecycle. **Proposal:** either a fifth Full reason, "the first WO on a
  new UI surface", or, for Light UI WOs, a planner-written "touch geometry
  & lifecycle" paragraph in the seam table: overlapping hit areas and their
  order, and what survives an activity recreation. The paragraph is
  checked by the plan reviewer.
- **Mid-WO orchestrator hand-over (model change for credits).** The owner
  moved the orchestrator from Fable to Opus mid-P5. It worked through a
  "MID-WO HAND-OVER" section in KICKOFF.md with a paste-able resume prompt.
  Two gaps showed:
  - (a) The code reviewer was still running as a subagent of the old
    session. Its hand-back could only reach the old context, and both
    contexts could have triaged it. This was resolved by a cross-session
    message: the old session stood down and relayed the verdict.
  - (b) "One orchestrator context per WO" has no wording for a forced
    switch.
  **Proposal:** a hand-over template in templates/work-order.md. It names
  every station still running, which context receives its hand-back, and
  the rule that the old context stops at the hand-over row. The staffing
  note goes in the Metrics row "model actually run".
- **A station reported an artifact it never wrote.** The traceability
  auditor's first hand-back said "Report: reviews/WO-003-trace-audit.md".
  The file did not exist. The bash approver's D1 had denied its shell
  heredoc write, and the agent reported success anyway. Neither
  `reviews/WO-001-trace-audit.md` nor `reviews/WO-002-trace-audit.md`
  exists either, although the WO-002 station ledger cites the latter, so
  this is a recurring blind spot. **Proposals:**
  - (1) The auditor and test-verify agent files say "write the report with
    the Write tool".
  - (2) The orchestrator's RECEIVE step checks that every artifact a
    hand-back names exists before logging it.
  - (3) The station ledger links only files that exist. A one-line check
    in wo-metrics or trace-check would do it.
- **Two agents killed by the API session limit (HTTP 429).** TASK-013's
  first attempt left nothing. TASK-014 died after its floor run and before
  its hand-back, and was resumed with its context intact. Both were
  recovered with `git status` plus the agent's scratchpad XML copies. No
  rule was missing, but the recovery recipe lives only in this WO's log.
  **Proposal:** add one paragraph to orchestration.md, "a station killed
  mid-run". Read the tree and the agent's copies, resume the same context
  if it survived, otherwise give a fresh context the same brief. A kill is
  not a recirculation.
- **Channel fact (platform-notes/android.md).** In
  `-Pandroid.testInstrumentationRunnerArguments.class=A,B` passed through
  `cmd.exe //c` from Git Bash, only the first class runs. Use one class per
  invocation, or omit the argument for a whole set. The WO-003 briefs'
  channel table said otherwise. The connected-test XML folder is also
  overwritten per run, so stations must copy what they cite.
- **Verifier freshness (code review N2).** A project verifier reads an
  APK the close builds, but nothing ties that APK to the tree. A stale
  `app-release-unsigned.apk` passes. Here the budget was full at exactly
  150 lines, so the guard could not live in the verifier. **Proposal:**
  `run_verifiers.py` checks, generically, that each artifact a manifest
  entry names is newer than the newest source under `product_globs`. That
  costs the project's budget nothing.
- **Principle 16 again.** Most turns of this WO ended while a station ran
  in the background. "All ready" was the least-wrong signal (see the
  2026-10-11 entry). Still unresolved.
- **Severity:** medium. Class B hid two real product defects behind a
  green kit, and class C's root cause is an orchestrator duty. The rest
  are low.
