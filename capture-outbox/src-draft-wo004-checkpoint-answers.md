---
kind: conversation
reliability: authoritative
origin: build
captured: 2026-10-17
by: orchestrator (TangramPlaytime WO-004, G4 surface)
touches: [REQ-025, REQ-026, REQ-024, REQ-050, TYPE-006]
---
# SRC — The owner's answers to the WO-004 checkpoint items (chat, 2026-10-17)

## Verbatim
> (Q1) yes just one press (Q2) k (Q3) k (Q4) k (Q5) k.

## Context
The orchestrator put five product-visible checkpoint items to the owner in
plain words (`workorders/WO-004.md` § Checkpoint items, asked 2026-10-15,
asked again on the WO-004 G4 surface 2026-10-17 with the defaults that would
stand on 2026-10-18). The build already followed each default. The numbers are
the orchestrator's items, not REQ acceptance lines.

| Item | What was asked | What the owner answered |
|---|---|---|
| 1 | Restart takes one press, with no "are you sure?" | Yes — one press. |
| 2 | The Retry · best time · Next row appears once the solved picture has faded in (about 1.4 s) | Ok. |
| 3 | The best time shows "–" until play time is counted (WO-008) | Ok. |
| 4 | A tap anywhere in the middle of the top bar (name, rating dots, number) opens the puzzle grid; the state word shows only for "in progress" and "solved" | Ok. |
| 5 | A puzzle whose saved pieces no longer fit after an update (the board comes back empty) stays "in progress" | Ok. |

## What it decides (for the capture session)
- Items 1–4 confirm build-side readings no locked REQ fixed (`decisions.md`
  2026-10-15 "The top bar, Restart and the solved row, as drawn"). They need
  no Contract change; the capture side may record them as owner-confirmed.
- **Item 5 is the owner's support for the proposed text of
  `chg-draft-type006-missing-transitions.md`** (its bullet "A puzzle stays
  In progress while no piece is on the board … after the load check of
  REQ-025 has returned every saved piece to the tray"): no transition back
  to New. `decisions.md` 2026-10-15 "REQ-025's load check".
