---
status: proposed
targets: [REQ-018]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F11
---
# CHG — Inside the parallelogram's tray cell, the flip badge's area wins over the piece's tap

## Change
REQ-018 Rules, new bullet after **"The badge's touch area is at least 60 dp across (REQ-037)."**: **"In the tray, the badge's touch area takes precedence over the cell's piece touch area (REQ-015): a touch that starts inside the badge area is a badge press, never a tap-to-turn (REQ-016) or a drag start; the rest of the cell is the piece."**

## Reason
Review-01 F11 (Should). REQ-015 makes the whole tray cell the piece's touch area and REQ-013 puts the badge inside that cell; a still touch on the badge is both a tap (turn) and a badge press (mirror). REQ-016 A1 and REQ-018 A1 cannot both be derived for the same touch.

## Impact
REQ-018.A1, REQ-016.A1 (tray case), REQ-015 rule 2 (reads with this exception). Build side: hit-testing order in the tray.
