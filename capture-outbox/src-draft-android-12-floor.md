---
kind: conversation
reliability: authoritative
origin: build
captured: 2026-10-19
by: orchestrator (TangramPlaytime, WO-010 P4)
touches: [REQ-006]
---
# SRC — The owner raises the oldest supported Android to Android 12 (chat, 2026-10-19)

## Verbatim
> we done here? I cant see nothing spinning? do we need to remove some older phone supports? Maybe we raise olderst supported to Android 12 or 13?

The orchestrator answered that nothing needs removing, priced the change
(the Android 8 emulator is the slowest and least stable test channel; phones
and tablets that never got the new version could not install the game; a new
emulator image and the build setting to change) and asked: "Which oldest
Android should the game support from the next work order on: Android 12
(recommended), Android 13, or keep Android 8?" The owner:

> Lets move to Android 12 as oldest supported.

## Context
At G1 (2026-10-11) the owner answered "android 8 is more than enough, can go
way higher if it simplifies development" (`src-draft-g1-owner-answers.md`,
item 5 / F7). The build chose Android 8.0 (API 26, `minSdk 26`;
`architecture.md` §2 stack line; `decisions.md` 2026-10-11 row 5), and
`chg-draft-req006-platform-floor.md` proposes the floor as a REQ-006 rule.
No locked REQ names a minimum Android version yet.

## What it decides (for the capture session)
- **The platform floor is Android 12 (API 31) and later.** It replaces
  "Android 8.0 (API 26) and later" in `chg-draft-req006-platform-floor.md`
  (amended the same day); devices below Android 12 are out of scope.
- **When:** from the next work order on. WO-010, whose final device run was
  under way on the Android 8 image, closes on Android 8; the move is applied
  before the next WO's device tests (the build side's G2 route for
  `architecture.md`, `minSdk 31`, a new floor emulator image).
