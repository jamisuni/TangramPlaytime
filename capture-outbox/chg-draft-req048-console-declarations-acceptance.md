---
status: proposed
targets: [REQ-048]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F16
---
# CHG — Every store declaration in REQ-048 has an acceptance line the owner can prove

## Change
REQ-048 Rules: **"The game is offered to the store's teacher-approved programme where one exists."** becomes **"The listing opts in to Google Play's Teacher Approved programme; acceptance by the programme is not required."** (or the bullet is deleted — the owner decides)
REQ-048 Acceptance, new lines:
- **A5** — "The data-safety section of the listing reads 'No data collected'."
- **A6** — "The listing shows a content rating obtained from the completed questionnaire."
- **A7** — "The target audience declared in the console includes an age group under 13 and the Families policy checklist is complete."
- **A8** — "The Teacher Approved opt-in is set." (only if the rule is kept)
Each of A1, A5–A8 is proven by an owner-signed evidence row (a console screenshot), not by a test.

## Reason
Review-01 F16 (Should). Four locked rules have no acceptance line to go GREEN on; "where one exists" is a lock-gate-v2-style vagueness for a programme Google runs by opt-in and review.

## Impact
REQ-048 (new A5–A8), REQ-048 rule 7. Build side: the #Release WO's done-check gains owner evidence rows; no code.
