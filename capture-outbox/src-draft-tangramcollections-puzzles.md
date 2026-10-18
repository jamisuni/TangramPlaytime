---
kind: conversation
reliability: authoritative
origin: build
captured: 2026-10-17
by: orchestrator (TangramPlaytime, after the WO-005 close)
touches: [REQ-042, REQ-038, REQ-039, REQ-007]
---
# SRC — The owner asks for the puzzles in `C:\GitHub\AI\TangramCollections` to be added to the game (chat, 2026-10-17)

## Verbatim
> plz look this folder [C:\GitHub\AI\TangramCollections] it has plenty of new puzzles. Plz add task to workorders to add those out game when possible

## Context
`C:/GitHub/AI/TangramCollections` (README dated 17 Oct 2026, written "for the
TangramNoAds game") holds five collections of ready-made tangram figures
converted to one JSON format, with the README's own licence reading per
collection: `convex-13` (13, CC0), `android-tangram-131` (131, GPL-3.0),
`nevit-dilmen-203` (203, a Wikimedia Commons licence needing attribution,
unconfirmed), `kilogram-1013` (1002, education and research use only) and
`richter-anchor-179` (179 silhouettes, no solutions; out of copyright, the
trace itself unlicensed). The game has 13 puzzles; the first-release count
stands at 25 (WO-002 checkpoint item 1, default 25, stood 2026-10-14; REQ-042
"at least 20").

## What it decides (for the capture session)
- The content batch already planned on the build side (`decisions.md`
  2026-10-11, "A content batch WO (Light) authors new puzzles…") takes its
  figures from this folder: it is now **WO-010** (`workorders/WO-010.md`,
  Proposed), sequenced before WO-009 #Release.
- It does **not** decide which collections may ship. The owner's G1 answer
  (`src-draft-g1-owner-answers.md` item 7: "only self-made or licence-free
  assets") and `architecture.md` G-16 allow only licence-free material, so by
  default only `convex-13` (CC0) ships as figures; the other collections are
  idea sources. Whether the GPL-3.0 figures may ship under the app's own
  GPL-3.0 licence, and how many puzzles the first release gets, are WO-010's
  checkpoint items 1 and 2 (asked 2026-10-17, defaults stand 2026-10-20).
- If the owner allows GPL-3.0 or attribution-licensed figures, the pending
  asset rule in `chg-draft-licence-notice-and-assets.md` ("…a source whose
  licence requires no attribution and no payment") needs amending in the same
  capture session.
