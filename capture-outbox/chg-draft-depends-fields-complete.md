---
status: proposed
targets: [REQ-014, REQ-018, REQ-021, REQ-024, REQ-026, REQ-030, REQ-031, REQ-032, REQ-033, REQ-035, REQ-037, REQ-041, REQ-042, REQ-043, REQ-045, REQ-047, REQ-048, REQ-049, REQ-050, REQ-051]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F21
---
# CHG — `depends:` reflects every REQ and TYPE a Statement or Rule relies on

## Change
Frontmatter `depends:` gains the ids the prose already uses (current value → added ids):
- REQ-014 `[]` → REQ-019, REQ-020
- REQ-018 `[TYPE-003]` → REQ-016, REQ-037
- REQ-021 `[TYPE-004]` → REQ-020
- REQ-024 `[]` → TYPE-006, REQ-025, REQ-050
- REQ-026 `[]` → REQ-030
- REQ-030 `[TYPE-005, TYPE-006]` → REQ-026, REQ-046
- REQ-031 `[]` → TYPE-005, REQ-030
- REQ-032 `[]` → REQ-009, REQ-029, REQ-030, REQ-031, REQ-033, REQ-034, REQ-049
- REQ-033 `[]` → REQ-010
- REQ-035 `[TYPE-007]` → REQ-025
- REQ-037 `[]` → REQ-013, REQ-015, REQ-018, REQ-046
- REQ-041 `[]` → REQ-045
- REQ-042 `[]` → REQ-039
- REQ-043 `[TYPE-001, REQ-013]` → REQ-014, REQ-047
- REQ-045 `[TYPE-001, REQ-012]` → REQ-038, REQ-040
- REQ-047 `[]` → REQ-009, REQ-039, REQ-043, REQ-049
- REQ-048 `[]` → REQ-001, REQ-009, REQ-010, REQ-049
- REQ-049 `[]` → REQ-009, REQ-047
- REQ-050 `[TYPE-006]` → REQ-037, REQ-040
- REQ-051 `[]` → REQ-045
No cycle is introduced (checked: every added edge points to a REQ that does not reach the source). No text of any Statement, Rule or Acceptance changes.

## Reason
Review-01 F21 (Should). `depends:` lists TYPEs almost exclusively; the REQ-to-REQ graph the build orders its work orders by, and the change-impact view in `views/trace.md`, read the field, not the prose.

## Impact
No acceptance line changes. Build side: WO ordering and impact analysis become mechanical.
