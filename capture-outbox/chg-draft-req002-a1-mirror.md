---
status: proposed
targets: [REQ-002]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F4
---
# CHG — Mirroring the parallelogram is one of the moves a puzzle may need

## Change
REQ-002 Statement: **"… rebuild a shown silhouette from the classic seven tangram pieces by dragging, turning and dropping them."** becomes **"… rebuild a shown silhouette from the classic seven tangram pieces by dragging, turning, mirroring (the parallelogram only, REQ-018) and dropping them."**
REQ-002 Acceptance A1: **"Every puzzle can be completed with pieces of TYPE-001 by drag, turn and drop only."** becomes **"Every puzzle can be completed with pieces of TYPE-001 by drag, turn, mirror (REQ-018) and drop only."**

## Reason
Review-01 F4 (Blocker). REQ-018 and TYPE-003 lock a mirror operation that only the parallelogram needs; a puzzle whose solution uses the mirrored parallelogram cannot be completed "by drag, turn and drop only", so the BIZ acceptance contradicts a locked FUN REQ.

## Impact
REQ-002.A1. Build side: the solvability test of every puzzle may use the mirror; no code change. Carries `implemented (unverified)` on REQ-002.A1 until decided.
