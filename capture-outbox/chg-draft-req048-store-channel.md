---
status: proposed
targets: [REQ-048]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F3
---
# CHG — Google Play is the release channel; the index's open question closes

## Change
REQ-048 Statement: **"The released game SHALL meet the obligations of the store it is published in for an audience that includes children …"** becomes **"The released game SHALL meet the obligations of Google Play for an audience that includes children …"**.
REQ-048 Out of scope: **"Which store or channel (Play, F-Droid, a signed file): an open question in the index. Technology."** becomes **"Other channels (F-Droid, a signed file from the repository) are not release channels of the first version. Technology."**
REQ-048 Rules, new bullet: **"Publishing channel: Google Play (owner, 2026-10-11, `src-draft-g1-owner-answers.md`; first stated 2026-10-05, SRC-016). IF the publishing account is a personal account created after 13 Nov 2023, THEN a closed test with at least 12 testers for 14 days precedes production (SRC-013); the owner's device playtest may be that test."**
`requirements.md` §5: the OPEN QUESTION (distribution, SRC-013) is replaced by the owner's answer, dated 2026-10-11 (and 2026-10-05, SRC-016); the account type is recorded when the owner names it.

## Reason
Review-01 F3 (Blocker). A project-wide open question while the index Status is Locked binds nobody (lock gate v2), yet every REQ-048 rule and REQ-001 A2 can only be proven on Google Play. The owner answered on the G1 surface: "aye we plan for google play" (`src-draft-g1-owner-answers.md`), consistent with SRC-016 ("Google Play (Recommended)", 2026-10-05).

## Impact
REQ-048.A1, REQ-048.A2, REQ-001.A2 (now have an oracle: the Play listing). REQ-048 Rules (new bullet). Build side: the #Release WO's listing draft and testing-track plan; the lines carry `implemented (unverified)` until the capture side applies this delta.
