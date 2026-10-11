---
status: proposed
targets: [REQ-042]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F5
---
# CHG — The theme set is named in the REQ, not in a Spec file

## Change
REQ-042 Rules, second bullet: **"Themes: at least four of the categories in ../Spec/03-puzzle-format.md."** becomes **"Themes: every puzzle declares one theme; the first release has puzzles of at least four distinct themes from this set: <the owner-confirmed theme names, transcribed by the capture session from the current puzzle format and confirmed by the owner>."**
REQ-042 Acceptance A2: **"At least four categories are present."** becomes **"At least four distinct themes from the set above are present."**

## Reason
Review-01 F5 (Should). `../Spec/` is a design input; a locked rule that points at it delegates the Contract to an unsigned file (lock gate v2). A2 can count distinct values but cannot judge whether a theme is a legitimate one.

## Impact
REQ-042.A2. Build side: the content WO's count test reads the named set.
