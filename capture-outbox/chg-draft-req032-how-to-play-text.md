---
status: proposed
targets: [REQ-032]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F22
---
# CHG — The how-to-play text is written by the build, bounded, and proof-read on review

## Change
REQ-032 Rules: **"… a short how-to-play text, …"** becomes **"… a how-to-play text of at most 60 words in English, written by the build and proof-read by the owner on review (it never names a solution or a hint), …"**.

## Reason
Review-01 F22 (Should) asked whether the owner supplies the text or leaves it to the build with a length bound. The owner's blanket "rest ok" on the G1 surface (2026-10-11, `src-draft-g1-owner-answers.md`) delegated the answer to the orchestrator's recommendation: the build writes it, bounded, and the owner proof-reads it from `review-notes.md` (`decisions.md` 2026-10-11, row 12).

## Impact
REQ-032 (rule gains a bound; A1/A2 unchanged). Build side: the #Settings WO writes the text and lists it in `review-notes.md`.
