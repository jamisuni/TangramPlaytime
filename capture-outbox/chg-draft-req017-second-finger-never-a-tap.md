---
status: proposed
targets: [REQ-017]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F12
---
# CHG — During a drag every further touch is a twist finger, never a tap or a drag of another piece

## Change
REQ-017 Rules, new bullet: **"WHILE a piece is being dragged, any further touch anywhere on the screen is a twist finger for that drag; it never taps (REQ-016), mirrors (REQ-018), drags or otherwise acts on another piece or control, whatever it lands on."**

## Reason
Review-01 F12 (Should). REQ-017 says a second finger that "touches the screen" twists; REQ-015 says a touch on a piece that moves under 12 dp is a tap. A second finger landing on a tray piece and lifting satisfies both, so REQ-017 A2 ("A 20° twist does not turn the piece") can pass while a different piece turned.

## Impact
REQ-017.A1, REQ-017.A2, REQ-016.A1, REQ-012.A2. Build side: pointer routing while a drag is active.
