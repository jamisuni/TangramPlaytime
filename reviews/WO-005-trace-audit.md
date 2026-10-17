# WO-005 trace audit (2026-10-17, 13:11)

Verdict: **GREEN (WO WO-005 scope)**

## Commands (root C:/GitHub/AI/TangramPlaytime)
1. `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/trace-check/trace_check.py --project . --wo WO-005`
2. Same without `--wo` (project-wide, a note only).

## Verbatim summary lines
- `trace-check: GREEN (WO WO-005 scope)`
- `note: WO WO-005: 4 of 4 in-scope acceptance ID(s) covered; 38 gap(s) outside the WO's scope not counted (other WOs' business)`
- `note: req-lint (validate.py): OK — 0 errors, 66 warning(s)`
- `note: views/ fresh (17 file(s) byte-identical to a regeneration)`
- Project-wide: `trace-check: RED` (22 Locked REQs with uncovered acceptance IDs: REQ-001, 002, 005, 006, 008, 009, 010, 029-039, 042, 047, 048, 049; none is REQ-046; other WOs' scope).

## Per-ID coverage (all under play/)
- REQ-046.A1: play/src/test/.../DevAidKitTest.kt:154; play/src/androidTest/.../DevAidScreenKitTest.kt:127. Extra: app/src/androidTest/.../DevAidAppKitTest.kt:49.
- REQ-046.A2: play/src/androidTest/.../DevAidScreenKitTest.kt:198 (device test only; no JVM token).
- REQ-046.A3: play/src/test/.../DevAidKitTest.kt:223; play/src/androidTest/.../DevAidScreenKitTest.kt:251.
- REQ-046.A4: play/src/test/.../ReleaseBuildKitTest.kt:104.
- No dangling depends-on reported.

## Contract baseline
- No drift finding in either run. `.swdev/contract-baseline.json` mtime 10:21:37 (before this audit); `build-map.md` entry stamped 2026-10-17T10:19:25 (the recorded v1.5 delta). trace-check did not re-freeze anything in this run. File shows `M` in git, as it did before the run.

## Lock behind build
- tool printed 26 drafts awaiting a capture session in capture-outbox/ (no pending-CHG-on-REQ-046 line beyond that).
- Drafts mentioning REQ-046 or TYPE-006:
  - chg-draft-req046-android-build-terms.md (REQ-046 rules 2 and 5, new "release build" definition; touches A4)
  - chg-draft-type006-missing-transitions.md (TYPE-006; names REQ-046 "Solve this puzzle now" transition; A3)
  - chg-draft-depends-fields-complete.md (REQ-030 and REQ-037 gain depends-on REQ-046; TYPE-006)
  - chg-draft-lock-type-dictionary.md (TYPE-006)
  - src-draft-g1-owner-answers.md (REQ-046 mention, touches list truncated in my grep; TYPE-006 not confirmed)
  - src-draft-wo004-checkpoint-answers.md (TYPE-006)
- Build was made against the locked wording; the drafts are unratified capture-side changes, not blockers for this WO.

## Gaps
None for WO-005 scope. Note: A2 has device-suite coverage only.
