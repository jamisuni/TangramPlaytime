---
status: proposed
targets: [REQ-014]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F10
---
# CHG — "Without delay" is dropped or bounded to a frame

## Change
REQ-014 Acceptance A1: **"A dragged piece follows the finger without delay."** becomes **"While dragged, the piece is drawn at the finger's position of the current or the previous displayed frame, offset as the Assumptions state."** — or A1 is deleted, since the 120 ms grow rule and A2 already carry the testable content.

## Reason
Review-01 F10 (Should). "Without delay" is a lock-gate-v2 vague term; no implementation satisfies it literally and a lenient reading proves nothing.

## Impact
REQ-014.A1. Build side: the drag test asserts a one-frame bound or the line disappears.
