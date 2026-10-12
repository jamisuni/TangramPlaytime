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

## Amended 2026-10-12 (build, WO-003 code review F1) — the board case
Second new bullet in the same place: **"On the board, where the badge's touch area covers another placed piece, a touch inside that piece's drawn shape belongs to the piece; the visible badge always wins, and the rest of the badge's area wins over a piece's 12 dp margin (REQ-015)."**
Reason: REQ-018 puts the badge next to the parallelogram on the board, and its 60 dp area (REQ-018 rule, REQ-037) can cover a neighbouring locked piece whose touch area is its shape grown by 12 dp (REQ-015); a touch there is both. The build follows this reading (`decisions.md` 2026-10-12, WO-003 code review F1). Impact adds REQ-016.A2 / REQ-016 statement (board case) and REQ-015 rule 2 (board case).

## Amended again 2026-10-12 (build, WO-003 fix batch) — the tray sentence, aligned with the board
The first bullet's tray sentence is replaced by: **"In the tray, the visible badge wins; a touch on the parallelogram's drawn miniature belongs to the piece (REQ-015, REQ-016); the rest of the badge's touch area wins over the rest of the cell."** Together with the board bullet this is one rule: a touch on something drawn acts on it, and the badge's area beats a piece's undrawn touch area.
Reason: a 60 dp area inside the parallelogram's cell (so that it never reaches a neighbouring cell, REQ-015) covers 57–82 % of the resting miniature on a phone; with the area first, a tap on the middle of the parallelogram mirrors it and it cannot be turned in the tray as REQ-016 says. `decisions.md` 2026-10-12 (fix batch, reviewer note on F1).
