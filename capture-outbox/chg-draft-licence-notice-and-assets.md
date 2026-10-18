---
status: proposed
targets: [REQ-049, REQ-033, REQ-039]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F24
---
# CHG — A licence line in Settings; only self-made or licence-free assets

## Change
REQ-049 Rules, new bullet: **"Below the privacy text the settings screen shows one line: "Open source, GPL-3.0 — source code: <repository address>". It is plain text (a readable address, not a link that opens a browser: REQ-010 forbids network use)."**
REQ-049 Acceptance, new **A2**: **"The settings screen shows the licence line with the repository address."**
REQ-033 Rules, new bullet: **"Every sound effect is made for this project or taken from a source whose licence requires no attribution and no payment; the licence of each sound is recorded in the repository."**
REQ-039 Rules, new bullet: **"Every picture, font and other shipped asset is made for this project or taken from a source whose licence requires no attribution and no payment; the licence of each asset is recorded in the repository."**
REQ-039 Rules, new bullet *(amended 2026-10-18, the owner's WO-010 answer)*: **"A puzzle figure (the arrangement of the seven pieces) may also come from a source under the app's own licence, GPL-3.0; its source and licence are recorded in the repository and credited in `ASSET-LICENCES.md`. Its picture is still made for this project."**

## Reason
Review-01 F24 (Should): the GPL-3.0 licence appeared only inside an open question; no REQ covered a licence notice or the licence of sounds, chime, confetti, fonts and pictures. The owner answered "ok" to both proposals on the G1 surface, 2026-10-11 (`src-draft-g1-owner-answers.md`, item 7). On 2026-10-18 the owner allowed the 131 GPL-3.0 figures of `C:/GitHub/AI/TangramCollections` (`src-draft-wo010-checkpoint-answers.md`: "lets do that 13+131 now"), hence the puzzle-figure bullet.

## Impact
REQ-049.A2 (new), REQ-033 and REQ-039 (new rules, checkable by a repository licence file and a dependency scan). Build side: the #Settings WO adds the line; the asset licence file is kept from the first WO that ships a sound or a font. Lines carry `implemented (unverified)` until the capture side applies this delta.
