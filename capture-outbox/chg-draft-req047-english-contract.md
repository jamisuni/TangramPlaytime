---
status: proposed
targets: [REQ-047, REQ-043]
origin: build
sources: []
created: 2026-10-11
raised_in: G1 surface, owner answer item 6 (follows review-01 F17 / F20)
---
# CHG — English is the Contract language; Finnish is a reference locale that proves the language mechanism

## Change
REQ-047 title/abstract: **"The game follows the device language, ships Finnish and English, and falls back to English."** becomes **"The game is English; it follows the device language with a Finnish reference locale and falls back to English."**
REQ-047 Statement: **"The game SHALL show every text, including puzzle titles, in Finnish WHEN the device language is Finnish, and in English otherwise."** becomes **"The game SHALL show every text, including puzzle titles, in English, and WHEN the device language is Finnish SHALL show the Finnish text where one exists and English otherwise."**
REQ-047 Rules: **"Languages shipped: `fi` and `en`; any other device language gets `en`."** becomes **"Languages shipped: `en` (the Contract wording) and `fi` (a reference translation whose wording is not a requirement — owner, 2026-10-11); any other device language gets `en`."** The bullet **"Puzzle titles come from the puzzle file's `title[lang]`; validator rule V8 requires both `en` and `fi`."** becomes **"Puzzle titles come from the puzzle file's `title[lang]`; `en` is mandatory, `fi` optional (falls back to `en`)."** The bullet **"The free note (REQ-009) and the privacy text (REQ-049) exist in both languages."** becomes **"The free note (REQ-009) and the privacy text (REQ-049) are Contract in English; their Finnish versions are reference translations."**
REQ-047 Acceptance: **A1 "With the device in Finnish, the top bar, the settings screen and the buttons show Finnish; the Cat puzzle is titled "Kissa"."** becomes **"With the device in Finnish, at least one visible text of the settings screen is shown in Finnish (the mechanism works); no Finnish wording is asserted."** **A2 "With the device in Swedish, everything is in English."** stays.
REQ-047 Rationale: the owner's words replace "The players are Finnish children aged 8 and up".
REQ-043 Assumptions: the second ASSUMPTION ("Finnish size labels use the same letters") becomes moot; S, M, L are the only marks.

## Reason
The owner decided on the G1 surface, 2026-10-11: "lets go english only, and finnish is there just as reference to make sure language can be change but translations for fin are not important" (`src-draft-g1-owner-answers.md`). The locked REQ-047 makes every Finnish string Contract and A1 binds a specific Finnish title (review-01 F20). Two locked items (REQ-047 as written, the owner's decision) would otherwise conflict; the build follows the owner's decision and files this delta the same day (AGENTS.md: two locked items in conflict → a CHG, never an interim rule).

## Impact
REQ-047.A1 (rewritten), REQ-009.A1, REQ-049.A1 (see `chg-draft-req009-req049-finnish-wording.md`), REQ-012 rule "The puzzle's title is shown in the game's language" (unchanged in meaning). Build side: the #Language WO ships `fi` resources as reference strings listed in `review-notes.md`; the puzzle validator's V8 becomes "en mandatory"; AGENTS.md owner rule 13 ("every text in two languages") is amended by the owner's statement. Lines carry `implemented (unverified)` until the capture side applies this delta.
