---
status: proposed
targets: [REQ-040, REQ-042, REQ-047]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F20
---
# CHG — Acceptance lines test properties, not puzzle ids or titles from the design-input library

## Change
REQ-040 Acceptance A2: **"A full puzzle of rating 1 whose id sorts before 'shapes-warmup' still comes after every warm-up."** becomes **"A full puzzle of rating 1 comes after every warm-up, whatever its id."**
REQ-042 Acceptance A3: **"shapes-square is among them."** becomes **"The classic square puzzle is among them."**
REQ-047 Acceptance A1: **"… the Cat puzzle is titled 'Kissa'."** becomes **"… every puzzle's title is its Finnish title."**

## Reason
Review-01 F20 (Should). No REQ requires a Cat puzzle or the ids "shapes-warmup" and "shapes-square"; the library under `../Tangrams/` is, in the index's own words, "not requirements". Three lines go RED or vacuous if the content is re-authored in this run.

## Impact
REQ-040.A2, REQ-042.A3, REQ-047.A1. Build side: the content and language tests assert properties.
