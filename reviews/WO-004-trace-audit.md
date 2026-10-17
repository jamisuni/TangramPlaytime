# WO-004 trace audit

Command: `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/trace-check/trace_check.py --project . --wo WO-004` (no --freeze)
Exit code: 0 · Stamp: Sat Oct 17 09:19:38 2026 (clock `date`)

**Verdict line:** `trace-check: GREEN (WO WO-004 scope)`

- In-scope coverage: WO WO-004: 16 of 16 in-scope acceptance ID(s) covered; 42 gap(s) outside the WO's scope not counted (other WOs' business).
- FAIL lines: none.
- Project-wide: the tool did not print a project-wide RED; the 42 out-of-scope gaps belong to other WOs and are not this gate.
- Tests: 45 test files scanned, 20 REQ-token, 2 guardrail, 24 untagged.
- Views: fresh (17 files byte-identical to a regeneration).
- Contract notes: the tool printed no re-freeze, no drift and no contract-delta note (63 contract files). The baseline was re-frozen by the orchestrator at 09:19 under governance row 4 (decisions.md 2026-10-17, architecture.md v1.1 signed). WO-004 § Contract deltas table is still empty (the close fills it).
- req-lint (validate.py): OK, 0 errors, 66 warnings.
- Governance profile in force: ai-mastered (20 control points).

## Lock-behind-build
25 drafts await a capture session in capture-outbox/ (24 chg-draft-*, 1 src-draft-g1-owner-answers).
Drafts mentioning this WO's REQs (file-name grep):
- REQ-003: chg-draft-lock-type-dictionary, chg-draft-req025-drop-profile-storage-rule, chg-draft-req025-unreadable-state-and-save-timing
- REQ-024: chg-draft-depends-fields-complete, chg-draft-req025-unreadable-state-and-save-timing
- REQ-025: chg-draft-depends-fields-complete, chg-draft-lock-type-dictionary, chg-draft-req025-drop-profile-storage-rule, chg-draft-req025-unreadable-state-and-save-timing, chg-draft-type006-missing-transitions
- REQ-026: chg-draft-depends-fields-complete, chg-draft-req005-available
- REQ-050: chg-draft-depends-fields-complete, chg-draft-lock-type-dictionary, chg-draft-type006-missing-transitions
- REQ-040: chg-draft-acceptance-content-ids, chg-draft-depends-fields-complete, chg-draft-req038-validity-rules-in-contract
- REQ-041: chg-draft-depends-fields-complete, chg-draft-req038-validity-rules-in-contract, src-draft-g1-owner-answers
- REQ-045: chg-draft-depends-fields-complete, chg-draft-req038-validity-rules-in-contract, src-draft-g1-owner-answers
