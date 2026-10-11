---
kind: conversation
reliability: authoritative
origin: build
captured: 2026-10-11
by: orchestrator (TangramPlaytime P1, G1 surface)
touches: [REQ-048, REQ-001, REQ-006, REQ-002, REQ-038, REQ-041, REQ-045, REQ-047, REQ-009, REQ-049, REQ-033, REQ-039, REQ-032, TYPE-001, TYPE-003, TYPE-004, TYPE-005, TYPE-006, TYPE-007]
---
# SRC — The owner's answers to the G1 review surface (chat, 2026-10-11)

## Verbatim
> (1) aye we plan for google play (2) play has been ok for 6 years old tester (3) yes (4) yes mirror is need (5) android 8 is more than enough, can go way higher if it simplifies development (like example Android 10) (6) lets go english only, and finnish is there just as reference to make sure language can be change but translations for fin are not important. (7) ok... (x) rest ok also. Plz progress ahead as automatic as possible

## Context
The orchestrator presented the seven review-01 findings it judged most important to the owner, in plain words, each with a recommendation; the numbers above are the orchestrator's items, not finding ids:

| Item | Finding | What the owner answered |
|---|---|---|
| 1 | F3 — distribution channel | Google Play. Account type not stated; the closed-test clause stays conditional. |
| 2 | F2 — the unsigned shared types (snap distance, 60 s idle, 600 dp boundary, colours, turn step, puzzle states) | Confirmed as they are: "play has been ok for 6 years old tester" (the prototype's numbers have been used by a six-year-old). |
| 3 | F1 — puzzle validity rules written into the requirement, script as the tool | Yes. |
| 4 | F4 — "mirror the parallelogram" added to the top-level rule | Yes. |
| 5 | F7 — minimum Android version | Android 8.0 is enough; the build may choose higher if it simplifies development. |
| 6 | F17 (and REQ-047) — Finnish wording | **English only is the Contract.** Finnish stays as a reference locale to prove the language mechanism; Finnish translations are not a requirement. |
| 7 | F24 — licences | Ok to both: a GPL-3.0 / source line in Settings, and only self-made or licence-free assets. |
| x | F5–F38 | "rest ok": the orchestrator's recommendations (the Resolution column of review-01) stand as the verdicts, recorded per finding. |

What the build did with it: verdicts filled in `Requirements/reviews/review-01.md` on the owner's instruction; chg-drafts amended (`chg-draft-req048-store-channel.md`, `chg-draft-req006-platform-floor.md`, `chg-draft-lock-type-dictionary.md`, `chg-draft-req009-req049-finnish-wording.md`) and added (`chg-draft-req047-english-contract.md`, `chg-draft-licence-notice-and-assets.md`, `chg-draft-req032-how-to-play-text.md`); `decisions.md` rows for the minSdk choice and the how-to text; AGENTS.md rule 13 amended with a dated note.
