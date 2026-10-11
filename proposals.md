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
