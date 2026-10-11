---
status: proposed
targets: [TYPE-001, TYPE-003, TYPE-004, TYPE-005, TYPE-006, TYPE-007]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F2
---
# CHG — The owner confirms and signs the shared types that 47 locked REQs depend on

## Change
`req_types.md` header: **"Status: Draft · Version: 0.2 … Approved by: — · Approved on: —"** becomes **"Status: Locked · Version: 1.0 … Approved by: Jami · Approved on: <date>"**.
TYPE-001, TYPE-003, TYPE-004, TYPE-005, TYPE-007: **"Confidence: Inferred (…)"** becomes **"Confidence: Stated (owner confirmation 2026-10-11, `src-draft-g1-owner-answers.md`: "play has been ok for 6 years old tester")"**. The owner confirmed every value **as it stands** on the G1 surface (item 2); the capture session registers the statement and signs. The values confirmed:
- TYPE-001: the seven colours; the resting turns in the tray.
- TYPE-003: 45° step; centre-preserving turn; parallelogram-only mirror.
- TYPE-004: R = 0.65 units and never below 30 dp; exact turn and mirror; score = |t| − 0.04 × exact corners within a 0.16 window; 1e-6 tolerance and exact anchor placement.
- TYPE-005: visible on screen and last touch ≤ 60 s.
- TYPE-006: the three states and their transitions (see also `chg-draft-type006-missing-transitions.md`).
- TYPE-007: Phone below 600 dp width, Tablet at 600 dp or wider.
The ASSUMPTION lines stay as the record of who chose the number; each gains "confirmed by the owner 2026-10-11". The owner's words also carry a firsthand observation (a six-year-old tester has played with these numbers); the capture session may register it as the first `firsthand` source of the UI values (lock gate v2).

## Reason
Review-01 F2 (Blocker). Every locked REQ's TYPEs are Contract with it; the lock receipt lists 45 "depends on TYPE-00n, which is not locked" lines and five Inferred TYPEs. The build tests REQ-019, 021, 016, 029, 030, 035, 036, 013, 043 against these numbers, and nobody has signed them. The v2 lock gate makes this an error at the next re-lock.

## Impact
REQ-019.A1–A4, REQ-021.A1–A2, REQ-016.A1, REQ-029.A1, REQ-030.A1, REQ-031.A2, REQ-035.A1, REQ-036.A1, REQ-013.A1–A3, REQ-043.A1, REQ-003.A2, REQ-025.A1–A3, REQ-050.A1. Build side: all play and layout WOs carry `implemented (unverified)` on TYPE-bound lines until the dictionary is signed; a changed R or boundary is a value change, not a redesign.
