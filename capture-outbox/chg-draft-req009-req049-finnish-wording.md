---
status: proposed
targets: [REQ-009, REQ-049]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F17
---
# CHG — The English wording of the free note and the privacy text is the Contract; Finnish is a reference translation

## Change
REQ-009 Rules, new bullet: **"The English text above is the Contract wording. A Finnish version exists so that the language mechanism (REQ-047) can be shown to work; its wording is not a requirement and is proof-read on review, never tested (owner, 2026-10-11)."**
REQ-049 Rules, new bullet: the same sentence for the privacy text.
REQ-049 Assumptions: **"the wording above, and its Finnish translation in the prototype."** becomes **"the English wording above."**
REQ-009 A1 / REQ-049 A1 read: **"The settings screen shows the note / the privacy text in full"** → **"… in full, in English (the device language set to English)."**

## Reason
Review-01 F17 (Should) found that the Finnish wording was only in the prototype, a design input, so A1 "in full" compared against nothing signed in Finnish. The reviewer proposed signing Finnish copy; the owner decided the other way on the G1 surface: "lets go english only, and finnish is there just as reference to make sure language can be change but translations for fin are not important" (`src-draft-g1-owner-answers.md`). This draft follows the owner, not the reviewer. See also `chg-draft-req047-english-contract.md`.

## Impact
REQ-009.A1, REQ-049.A1 (oracle: the English screen). Build side: `fi` string resources ship as a reference translation listed in `review-notes.md`; no acceptance test reads Finnish copy.
