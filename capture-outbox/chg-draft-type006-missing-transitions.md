---
status: proposed
targets: [TYPE-006]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F13
---
# CHG — TYPE-006 names every transition the locked REQs perform

## Change
TYPE-006 Rules, after **"In progress → Solved when the last piece locks (REQ-022). Restart returns In progress → New; Retry returns Solved → New (the best time is kept)."**, new bullets:
- **"Reset all progress (REQ-034) returns every puzzle, from any state, to New and erases its times."**
- **"The developer aid 'Solve this puzzle now' (REQ-046) moves New or In progress → Solved without setting a best time."**
- **"A puzzle stays In progress while no piece is on the board (after a missed first drop, REQ-020, or after the load check of REQ-025 has returned every saved piece to the tray); its time keeps counting and Restart is shown."** (the reviewer's reading of the current text; the owner may prefer "returns to New when the board is empty")

## Reason
Review-01 F13 (Should). Every entity transition must be governed; REQ-034, REQ-046 and REQ-025 perform transitions the lifecycle does not list, and the empty-board In progress case is reachable on the very first touch of a child's first puzzle.

## Impact
REQ-034.A1, REQ-046.A3, REQ-025.A3, REQ-030.A3, REQ-050.A1–A2 (grid state). Build side: the puzzle-state model in the browsing WO.
