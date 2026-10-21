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

## 2026-10-17 — WO-004 close: two corrected checks, a stale-pass class seen twice, a pause across two days

- **Corrected check 1 (design review F5(b), reversed on arbitration).**
  The design reviewer asked for a `build-map.md` row, because REQ-040.A1
  and REQ-041.A1 gained screen tests under `play/` and `app/`. trace-check's
  rule 3 fails only when no covering test lives under the code home. The
  `content/` tokens are there, so no row was needed. **Miscalibration:**
  the reviewer read "one code home per tag" as "every test under the home".
  **Proposal:** `traceability-rules.md` §5 rule 3 gets one sentence: extra
  tokens outside the home are allowed; the rule asks for one inside it.
- **Corrected check 2 (a kit pin that could not fail where it ran).** The
  pin `TopBarKitTest.aPieceDrawnOverTheRestartPill…` searched the bundled
  puzzles for a case and skipped by `assumeTrue` when none existed. On the
  dev phone none can exist: the 577 dp board keeps every silhouette at
  least 57.7 dp below the board's top, and the pill ends at 58 dp. The
  connected runner wrote the skip into the XML as a `<failure>` while
  Gradle exited 0. The fix is a kit-constructed case: a kit-owned puzzle,
  shown at a kit-chosen height. **Proposal:** `task-decomposition.md`
  ("Assert only what is mandated" / fixtures) gets one line. A fixture
  search that can come back empty on a WO channel is a check that cannot
  fail: construct the case, or fail loudly. Codified for this project in
  `platform-notes/android.md`.
- **A pass that can be stale, seen twice (WO-003 N2, WO-004 code review
  S2).** WO-003's verifier read an APK nothing tied to the tree. WO-004's
  process-death harness installed whatever APKs sat in `build/outputs`.
  The harness now refuses APKs older than any source or build input. Two
  more facts came out of the fix:
  - Gradle leaves an up-to-date APK unwritten, so the channel line must
    force `packageDebug` and `packageDebugAndroidTest`.
  - Under `-q`, a failed install (`INSTALL_FAILED_ALREADY_EXISTS`) exits 0
    with no XML. Test & Verify caught it only because the XML was missing.

  **Proposal (repeats WO-003's, now with a second case):**
  `run_verifiers.py` checks, generically, that each artifact a manifest
  entry names is newer than the newest input. The test-verify station file
  then gets one rule: judge a device run by its XML (exists, fresh,
  counts), never by an exit code.
- **The bash approver's D4 denies read-only git.** `git check-attr` (from
  the TASK-022 implementer, checking `.gitattributes`) and `git archive`
  (TASK-018, to a scratch folder) were denied as history writes. Both
  read history and write nothing to it. **Proposal:** add `check-attr`,
  `cat-file` and `ls-tree` to the read list in `swdev_bash_approve.py`;
  `archive` stays a judgment call.
- **Mid-WO pause across two days.** The owner paused at 21:32 and the
  WO resumed 33 h later in a fresh context. It worked because of the
  KICKOFF "PAUSED" note, the WO log, the run log and recomputing the kit
  hashes (17/17 unchanged). The paused session's scratchpad (every XML the
  i20 row cites) was gone. **Proposal:** the hand-over recipe (WO-003's
  2026-10-12 entry) adds that XMLs cited across a pause are copied under
  `.swdev/run-log/` or re-run, because scratchpads die with the session.
- **A seam-table gap found by a UI implementer.** S-1Δ named no read for
  the puzzle list the grid must draw, so the TASK-021 implementer added one
  internal read. It was ruled a gap closed in place, and the design was
  written back. **Proposal:** the design-author station file gets one
  checklist line: for each screen, list the data it draws and the seam
  read that gives it.
- **Principle 16 again.** Every turn that ended while a station ran in the
  background ended with "All ready" as the least-wrong signal. Still
  unresolved (2026-10-11 entry).
- **Severity:** low to medium. The stale-pass class is medium: it has now
  occurred twice, and a stale pass on the death channel would have hidden
  a lost-progress defect. The rest are low.

## 2026-10-17 — WO-005 #DevTools: two corrected checks, a build-map cell trace-check misread, a "must be absent" line with no token route

- **Corrected check 1 (a device pin's tap point, contested by the
  implementer and reversed on arbitration).**
  `DevAidScreenKitTest.theDevButtonIsNeitherDrawnNorHitOnASolvedPuzzle…`
  took the DEV button's bounds on one puzzle and tapped them on another,
  solved puzzle. Each puzzle's tray height sets its board's bottom, and the
  solved row replaces that tray strip, so the point fell in Retry's third,
  which the touch order rightly serves first. **Miscalibration:** a
  geometry point carried across two screens whose layout depends on the
  content. **Proposal:** `task-decomposition.md` ("Read geometry from the
  drawing") gets one clause: read a position on the screen it is used on,
  and assert it lies in the region the test means before acting on it.
  Codified for this project in `platform-notes/android.md` at this close.
- **Corrected check 2 (code review S1: the release scan looked for names
  only).** `ReleaseBuildKitTest` checked the aid's package, resource-name
  prefix and service entry. An aid leaking under other names (its text,
  the passcode) would have passed. It now also checks the REQ's own text
  and the passcode, with positive controls. A raw byte search for "0417"
  would have hit library vector paths in the release APK (`…C11.0417,8…`),
  so the passcode check reads the dex string table exactly. **Proposal:**
  the verifiers SKILL / guardrail-authoring "absent from the release
  build" pattern names both kinds of marker: the code's names *and* the
  content the REQ quotes; and "match a table entry, not raw bytes".
- **trace-check read a code-home cell literally.** `build-map.md` had
  ``play/` (`src/debug`)`` for #DevTools. `parse_build_map` kept the
  parenthesis as part of the path, so every REQ-046 token under `play/`
  would have failed the one-code-home rule. The planner found it by reading
  the script; the cell became `` `play/` `` (notify-tier delta).
  **Proposal:** `parse_build_map` takes the first backticked path in the
  cell; or `templates/build-map.md` says "one path per cell, notes
  elsewhere".
- **A "must be absent from the release build" line has no token route but
  a test that reads build outputs.** REQ-046.A4 is honestly proven only by
  scanning the release APK. A verifier earns no trace-check token, and an
  owner-evidence row is for what only a human can prove. The WO used a
  tokened JVM test that reads both APKs, with a stale-APK refusal — which
  in turn needed the JVM channel to build and repackage both APKs first,
  and (measured) in **two** Gradle runs: in one run the test read the
  release APK before `:app:packageRelease` had rewritten it.
  **Proposal:** `traceability-rules.md` §2 lets a manifest verifier carry
  an acceptance token (its positive control is the substance check); or the
  verifiers SKILL documents the "tokened build-output test" pattern with
  the two-run rule.
- **The WO question about `product_globs` was wrong:** trace-check reads no
  `product_globs`; only wo-metrics counts LOC with it. The WO asked whether
  the orphan-code scan saw `play/src/debug`; the answer is that trace-check
  has no orphan-code scan over product globs at all — the code reviewer is
  the orphan check. **Proposal:** the guard.json template's comment on
  `product_globs` says which tools read it.
- **The bash approver's D4 denied `git archive` again** (the TASK-023 kit
  author, wanting a clean copy of HEAD for a dry run). Same proposal as
  WO-004's entry.
- **A subagent killed by the API session limit mid-channel** (TASK-026,
  HTTP 429 during a whole-play-set device run). Resuming the same agent
  with its context intact after the reset lost nothing; the device run it
  had moved to the background had finished on its own. The WO-003 note
  stands: an orchestrator checks the tree and the kit hashes before
  resuming.
- **A launch missing from telemetry.** The WO-005 planner (`wo-planner`,
  dispatched 09:42) has no `agent-launch` row in `.swdev/log/telemetry.jsonl`;
  every other launch of the WO has one. The station ledger and the WO log
  carry it, so the Metrics count 9, telemetry 8. **Proposal:** wo-metrics
  reconciles the ledger against telemetry and prints the difference, so a
  gap in the hook is visible instead of silently lowering the count.
- **Light mode on a UI WO, second time.** The planner's seam table was the
  design (as in WO-003) and carried the touch geometry and "what survives"
  the WO-003 harvest asked for; no design-level defect surfaced later.
- **Principle 16 again.** Same as WO-004.
- **Severity:** low to medium. The build-map parser misread is medium (a
  silent RED at the close gate, found only because the planner read the
  script). The rest are low.

## 2026-10-18 — WO-006 #Layout + #Language + #Promise: thirteen corrected checks, device time, a station re-woken onto another's emulator

- **Corrected checks 1–6 (the device kit at landing, found on the dev
  image by its own author).** (1) A format resource's `$` placeholder was
  counted as a currency symbol on screen. (2) Every untagged clickable was
  a stray, so the grid's scrim inside `overview` failed. (3) The rotation
  restore. (4) The held drag's pose in REQ-036.A2. (5) The pulse pin's
  clock. (6) Held-finger steps under a paused clock. **Miscalibration:** a
  closed-world check written from the REQ's words before the real screen
  was read (1–2), and timing assumptions about the device (3–6).
- **Corrected checks 7–9 (three contests by the implementer, all won by
  the worker).** (7) The floor cannot construct a 2560 × 1600 px display
  (API 26 clamps the width), and an override there is not a recreation.
  (8) Inset snapshots raced the bars settling. (9) The rotation restore's
  unbounded Compose wait hung the class. **Miscalibration:** the kit
  assumed API 37's display behaviour on the floor, and a restore path had
  no bound.
- **Corrected checks 10–11 (found on the floor while proving 7–9).**
  (10) The kit changed the display with the launcher in front; on API 26
  the launcher crashed and its dialog held the focus. (11) A Compose read
  polled across a relaunch parked forever in Espresso's idle wait.
  **Miscalibration:** device-level changes made outside the app's own
  activity, and Compose calls without a bound.
- **Corrected check 12 (a channel line that could not deliver its value).**
  AGP splits instrumentation arguments on commas, so the floor's list leg
  `kitDeviceLocales=sv-SE,fi-FI` arrived as `sv-SE`. It was proven by a
  direct `am instrument` run instead. **Miscalibration:** a channel line
  never shown to deliver its argument whole. **Proposal:** the
  test-author brief template asks for one control run per instrumentation
  argument, with a value that must fail.
- **Corrected check 13 (code review S2: an allow-list with a whole family
  in it).** The G-01 pin allowed bare `androidx/`, which holds ads,
  privacy-sandbox and webkit packages; its self-test only removed
  `kotlinx/`. **Proposal:** the guardrail-authoring "closed allow-list"
  pattern says: no family entry; list the measured members; the self-test
  injects a member of a family.
- **Device time was the WO's cost, not task size.** The fixes were small.
  Proving them took hours: seven classes × three emulators, one at a time,
  about 10 minutes a whole set. A defect found on the last image (the
  floor) forced a re-run of the other two. The first kit recirculation took
  2 h 29 min: about 1 h needed, 55 min a floor hang found last, 40 min
  re-proving. After the owner asked "do we have too big tasks?", TASK-032
  was routed floor-first, iterating on one class and running the whole
  sets once (1 h 35 min). Test & Verify started from the run-log baseline
  (v0.17) and re-ran one floor step, about 11 minutes. **Proposal:**
  `orchestration.md` / the brief template: "riskiest image first; while
  fixing, the failing class alone; the whole sets once, on the final tree".
  The kit template also gets a per-test time limit, so a hang costs
  minutes, not 15 per test.
- **A station re-woken onto another station's emulator.** The TASK-032
  implementer left the dev image running as its own background task. When
  Test & Verify stopped it to boot the floor, the background task's end
  re-woke the finished implementer. It tried to restart the image and ran
  one reset on Test & Verify's floor, and that step was re-run.
  **Proposal:** `adapters/claude/CLAUDE.md` (device stations): "boot an
  emulator detached (`nohup`), never as your own background task; after
  your hand-back, make no device call".
- **A pause mid-station loses the hand-back.** The owner's second pause
  (10:25) closed the session while a kit author ran. The next context
  found a half-fixed kit and no hand-back, and re-dispatched with both hash
  lists and the old evidence folder. KICKOFF's two-case resume prompt
  worked. **Proposal:** none beyond keeping that two-case form.
- **The planner's launch is missing from `telemetry.jsonl` again** (as in
  WO-005). The 13 launches logged from 2026-10-17 16:41 lack the planner's
  (16:51).
- **The review cycle worked.** The code review found what the kit could
  not see: S-36's wrap is unobservable on this APK (a mutation survived),
  and the allow-list family. Test & Verify reused the baseline honestly and
  discarded its own interfered run.
- **Severity:** medium. Device time decides the owner's experience of a WO
  (the owner asked about it mid-WO). The other items are low.

## 2026-10-19 — WO-010 (mid-P4): the turn signal turns background progress into chat spam

- **What happened.** TASK-035 (device-only) ran its emulator steps as many
  short background commands. Each time one ended, the harness woke the
  orchestrator with an interim "the station is still waiting on its own
  work" notice — eleven in about 25 minutes. Principle 16 requires every
  turn to end with one of the two signals, so each notice became a reply
  ("still running … All ready, whats next Jami?"). The owner: "why you
  start spamming?"
- **Two faults.** (1) The signal rule has no state for "a station is
  running, nothing for you": `All ready` claims idle, which is false, and
  the replies carried no information. (2) Device stations were not told to
  batch: a whole image's steps as one background script would wake the
  orchestrator once per image, not once per command.
- **Proposal.** (a) In `00-principles.md` §16 (or the adapter page): an
  interim background notice with no hand-back gets no reply text beyond
  the signal line, and a third signal, e.g. `Working, nothing needed from
  you Jami`, for "stations running, not blocked". (b) In the device
  stations' brief template: one background script per image (boot, steps,
  kill), so a long device task wakes the orchestrator a few times, not
  dozens.
- **Severity:** medium — it costs the owner's attention, which the
  checkpoint rules work hard to protect.

## 2026-10-19 — WO-010 (P4 fan-out): two small frictions

- **The bash approver's D4 denies `git check-ignore`.** It is a read
  (it asks whether a path is ignored), but D4 lists it among history
  writes: "SWDev bash-approve DENY: D4 git writes history (git
  check-ignore)". The orchestrator fell back to reading `.gitignore`.
  **Proposal:** add `check-ignore` (and `ls-tree`, `cat-file`) to the
  read allow-list in `swdev_bash_approve.py` and the adapter page's git
  list. **Severity:** low.
- **A pre-fan-out `git status` must name the orchestrator's own writes.**
  Plan S1 (WO-010) judges each parallel batch against the status recorded
  at dispatch. The orchestrator then edited its ledgers (`tasks.md`,
  the WO log, `progress.md`, `AGENTS.md`, `KICKOFF.md`), and all four
  batches spent effort reporting five "unexplained" paths. **Proposal:**
  in `task-decomposition.md` or the brief template, the fan-out record
  lists the ledgers the orchestrator will write during the fan-out as
  allowed. **Severity:** low.

## 2026-10-20 — WO-010 content batch close: seventeen corrected checks, defects that showed only at 106 puzzles or on the second image

- **Corrected check 1 (TASK-033).** The REQ-040.A2 test's last line
  pinned a 13-bundle fact ("the only full puzzle of rating 1 is the first
  full puzzle") instead of the REQ's ordering rule. **Miscalibration:** a
  fact of today's fixture written as the oracle.
- **Corrected checks 2–3 (F1, the kit half).** The board badge's disc
  reader treated seam pockets as a second region. The bundle-wide search
  stopped at the first doubtful board, although earlier boards had given
  candidates. **Miscalibration:** a pixel reader calibrated on 13 puzzles,
  and a search that throws on the first doubt instead of passing over it.
- **Corrected check 4 (delta review S-1).** The relaxed reader let a
  second disc pass silently, and the pass-over path never ran.
  **Miscalibration:** a check relaxed with no test that it still fails.
  The fix added two kit self-checks.
- **Corrected checks 5–8 (F2 and its look-alikes).**
  - ui-test 1.12.1's `performScrollTo` never returns under the kit's
    paused clock once a scroll is needed (at 13 puzzles none was): a 2-hour
    stall on the floor.
  - A semantics read per edge sample, each waiting for a frame, made
    REQ-023.A2 run over 30 min and REQ-011.A1 18 min.
  **Miscalibration:** per-puzzle and per-cell legs never timed at the
  bundle size they would meet.
- **Corrected checks 9–17 (F3 and eight look-alikes).** The "3 px in /
  3 px out" pixel sets were built by widening each piece 0.75 px and
  eroding or dilating by a 3 px square. At a 315° notch that counted
  pixels 0.46 px from the outline as "3 px in", where the picture's
  designed outline stroke is. It went red on the dev image only, because
  of where the pixel grid fell (API 26 is not edge to edge, so the board
  sits 94.5 px higher). The same construction had been copied into nine
  legs. Now measured from the exact outline, with a self-check against an
  independent disc oracle. **Miscalibration:** a margin set with no
  self-check, copied by imitation.
- **The pattern: the expensive run found what a cheap run could have.**
  F2 needed 106 puzzles, and F3 needed the second image's pixel grid.
  Both surfaced inside the 2–5 h whole-set runs of the final task. One
  whole-set pass was lost to each (about 2 h and 2 h 44 min), plus the
  first floor stall at the 2026-10-19 pause.
  **Proposal:** `task-decomposition.md` / the test-author brief, for any
  WO that grows a bundle or a fixture set:
  1. right after the growth lands, every bundle-iterating class runs
     alone, timed, on the floor **and** on one other image, before the
     final whole sets;
  2. every margin or pixel-band set in a kit gets a self-check against an
     oracle that does not share its construction;
  3. the kit template carries a per-test stall bound. WO-010 added a
     40-minute no-test-finished watchdog at dispatch, and it never fired
     after F2's fix.
- **A station claimed a file it never wrote — again** (WO-003, 2026-10-12).
  The trace auditor's report was a shell heredoc. The bash approver denied
  it (D1, a shell write to markdown), and the hand-back still said
  "written". The receive check (`ls`) caught it; a second message got the
  file written with the Write tool. **Proposal:** the auditor's agent file
  says "write the report with the Write tool and quote `ls -la` of it in
  the hand-back". `handoff-contract.md`: every artifact a hand-back names
  carries its `ls -la` line. **Severity:** low (caught), but it is the
  second time.
- **The orchestrator broke a ledger row with an Edit.** Inserting the
  09:11 triage row, the `old_string` ended after the F1 row's Decision
  cell. The new row went in mid-row, and the F1 row's last five cells sat
  after it until 12:49, when the orchestrator saw it while reading the
  table's format. **Proposal:**
  trace-check (or a hook on ledger edits) lints the governed ledger
  tables: every row has the header's cell count. **Severity:** low-medium
  (`decisions.md` rows are evidence at G4).
- **`wo_metrics.py --write` into an empty Metrics section wrote rows with
  no table header.** It still windows by calendar date (this WO's window
  held WO-004…006), so ten rows were hand-corrected again. **Proposal:**
  emit the `| Metric | Value |` header when absent, and window from the
  WO's open row (the first Log row of its own phase), as proposed at
  WO-006. **Severity:** low.
- **The bash approver's D4 also denies `git archive` (to scratch) and
  `git config`.** These were used to extract HEAD's copy of a file into
  scratch for a diff, and to read a setting; neither writes history.
  **Proposal:** the read allow-list gains `git archive` to stdout or
  scratch, `git config --get`, `git show <rev>:<path>` (already a read),
  with `check-ignore` from 2026-10-19. **Severity:** low.
- **"Is anything happening?"** Mid-run, the owner asked: "is there really
  happening anything anymore or just open sub sessions those dont do
  anything?" Interim notices say only "waiting on its own background
  work", and the signal line says `All ready`. The answer came from
  machine state: `adb devices`, the qemu process, the newest line of the
  station's `script.log` (seconds old), the per-image summaries.
  **Proposal:** extends 2026-10-19. A device station names its live log
  file in its first interim message; the orchestrator's answer to a
  progress question is read from that file and `adb devices`, never
  inferred. The third signal ("Working, nothing needed from you") would
  have answered it before it was asked. **Severity:** medium (the owner's
  attention).
- **What worked.**
  - Facts-first routing. F1 and F3 each went to a `slice-implementer` to
    establish the facts before anyone fixed anything. F1 turned out to be
    a product defect (25–28 of 104 parallelograms' badge areas left the
    board). F3 turned out to be the kit. The orchestrator's wrong premise
    ("the board mapping is the same on both phones") was disproved by the
    station, not acted on.
  - The delta reviews, two of them. They found S-1 (a check that could no
    longer fail) and re-derived the outline maths exactly.
  - Test & Verify reused the t042c baseline on matching hashes in 5
    minutes.
- **Severity:** medium overall. Device time decided the WO's length (about
  9 h of station emulator time on 2026-10-20 for one product fix and 16
  kit corrections).

## 2026-10-21 — WO-008 #PlayTime close: four corrected checks, a scope anchor the two scripts read differently, the first WO on the Android 12 floor

- **Corrected checks 1, 2 and 4 (planned at P3, landed with the feature).**
  Each pinned an earlier WO's interim fact as the oracle:
  1. `ResumeKitTest` `kitExpect` expected "best null after a player's
     solve" (WO-004, before any best time existed).
  2. `KitApp` `closedWorld()` / `languageWorld()` admitted only "–" in the
     solved row.
  4. `ControlsScaffoldTest` expected "60:00" (WO-004's m:ss-only
     rendering; D10 makes it "1 h 0 min").

  **Miscalibration:** a placeholder of a feature not yet built, asserted
  as if final. The design (S-48) and the plan named all three before
  code, so each cost minutes.
  **Proposal:** the test-author brief: a test that pins the visible
  absence of a later WO's feature names that WO in a comment
  (`// interim until WO-008 #PlayTime`). The later WO's planner then
  greps for it instead of finding the pins by reading.
- **Corrected check 3 (the floor move).** `KitApp`'s 800 × 1280 dp tablet
  construction was calibrated on API 26. On API 31, a `wm density` change
  rebuilds the 3-button navigation bar, and the rebuild re-locks a locked
  rotation, so the 90° freeze sent the window the wrong way. Two legs went
  red on `Phone_API_31` only. The kit's author fixed it: start landscape,
  then turn (`platform-notes/android.md`). **Miscalibration:** a device
  construction proven on one API level only.
  **What worked:** a floor move runs every class alone on the new floor
  first, the WO-010 proposal applied, so this showed in TASK-043 hop 1,
  before any product code. It cost 1 h 46 min of kit fix and no
  whole-set pass.
- **trace-check and plan-check read the same Implements bullet
  differently.** The bullet named its code homes with bare ids:
  "`kernel/` (`#ActiveTime` row — REQ-029)" and four like it.
  - trace-check's scope parser (`scope_ids`, `SCOPE_BARE`) reads a bare
    `REQ-NNN` anywhere in the bullet as every acceptance of that REQ. So
    REQ-029.A2, which G3 had moved to WO-007, became a tenth in-scope
    ID, and `--wo WO-008` went RED at P6.
  - plan-check read nine IDs: its warnings at 21:36 and 05:31 listed
    REQ-029.A2 as *outside* the scope.
  - The project already knew the trap: § Scope notes says "kept out of
    the Implements bullet so trace-check's scope parser reads only the
    IDs above". Prose in parentheses still slipped through.

  Test & Verify found it, and the orchestrator dropped the five ids
  (`decisions.md` 2026-10-21). **Proposals:**
  1. One scope parser shared by plan-check and trace-check. Until then,
     plan-check prints the scope trace-check would read and fails when the
     two differ.
  2. trace-check `--wo` runs at G3 (all red then, but its "WO scope: N
     acceptance ID(s)" line is the check), and the G3 checklist compares
     that N with plan-check's.
  3. The WO template's Implements bullet says "IDs only; code homes and
     prose go to Scope notes".

  **Severity:** low-medium. Caught before G4; a scope wider than approved
  reads as a coverage gap, and a narrower one would read GREEN on missing
  coverage.
- **wo-metrics' window, again** (2026-10-18, 2026-10-20). The window's
  end date is now 2026-10-23: the checkpoint default date written in a
  Log row, a day not yet come. Its start, the calendar day, holds WO-010's
  close, so the launch, staffing, approver, test and LOC rows were
  hand-corrected. **Proposal:** as before, window from the WO's
  `Opened` Log row's timestamp to the last row's. Read the Log's Date
  column only, never dates quoted in the notes. **Severity:** low.
- **The Stop hook's "this session appears unlogged" during a station
  wait** (2026-10-21 00:38). The orchestrator was waiting on a 102-minute
  device station; `progress.md` was 74 minutes old by design, since
  interim notices get no reply. **Proposal:** the hook counts a running
  background agent as activity, or its text says "a one-line progress
  bullet is enough while a station runs". **Severity:** low.
- **D4 denied a read again:** a station's CRLF count loop that read
  `git config` (2026-10-20 21:44). Same proposal as 2026-10-20 (the read
  allow-list gains `git config --get`).
- **What worked.**
  - **The final whole sets were green at the first pass** (WO-010 lost
    two whole-set passes). Every class ran alone on the floor and on dev as
    it landed (TASK-046, 047, 049), so TASK-051's 2 h 50 min found
    nothing. Inside the whole set each class ran within ±5 % of its time
    alone, so running a class alone first adds no blind spot.
  - **The design review re-derived the library.** F1 found that Compose's
    `performTouchInput` never reaches `Activity.dispatchTouchEvent`, the
    product's touch door, before any test was written. The kit injects
    through the window.
  - **The code reviewer mutated the fold order and `ActiveTime`.** Seven
    plus four mutants; every one that changes behaviour went red.
  - **Test & Verify reused the t051 baseline** on matching hashes in
    about 4 minutes.
- **Severity:** low overall. 9 IDs, 0 product defects, 4 corrected
  checks. Device time was about 9 h 40 min of station wall-clock; the
  kit fix took 1 h 46 min of it.
