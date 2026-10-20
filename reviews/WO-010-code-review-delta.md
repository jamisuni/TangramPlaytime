# WO-010 delta code review (fresh `code-reviewer`), 2026-10-20

Scope: the working-tree diff against `a0d3938` of eight files: `FlipBadge.kt` and `DecorationScaffoldTest.kt` (F1), and six kit files (`DecorationKitTest`, `KitScreen`, `OverviewKitTest`, `DevAidScreenKitTest`, `SolvedPictureKitTest`, `BoardTrayKitTest`). The first review (`WO-010-code-review.md`) is not redone.

## Verdict: forward

0 Blockers · 2 Shoulds · 2 Notes. Two of the four findings are about the tests rather than the product. All four are findings against test code or its documentation. The product diff has no finding.

## Checklist applied

- FlipBadge.kt against the REQ-018 rule (60 dp), REQ-037 rule 1, REQ-015 and the four `decisions.md` rows. **Pass.**
- Hit order with the area off the disc's centre (`HitTest.kt`, `PlayScreen.kt:187/209`). **Pass.**
- Disc wholly inside the area; nothing else moved. **Pass.**
- The two scaffolding pins fail without the fix (scratch mutation). **Pass.**
- Kit legs: do they still assert what they asserted before? REQ-050.A2, `scrollTo`, `readDrawnDisc`, the pass-over, the mapping read once. **Pass, with S-1 and N-2.**
- Token set unchanged; no main code in the kit diff; no kit file in the product diff. **Pass.**
- Cited source for each non-REQ assertion (`platform-notes/android.md` § Compose test kit). **Gap, S-2.**

## Findings

### Blockers
None.

### Shoulds

**S-1 · `DecorationKitTest.readDrawnDisc` (kit; test defect)**
- Observation: the "largest enclosed region plus the regions inside its box" rule leaves every enclosed region outside that box out of the reading, with a `Log.i` only (`outsideBox`). Before the change, a second enclosed shape made the box non-square and the reading failed. Now a second full disc, or a duplicated ring, drawn beside the first passes silently. The first disc becomes `main`, and the second is logged and ignored. A missing ring still fails (the glyph pockets are too small, so the width check trips). The seam pocket from the floor (about 94 × 88 px) is a few pixels, not a disc.
- Why it matters: the leg judges "where the badge's disc is drawn", so a double-drawn disc is the one wrong drawing it can no longer see.
- Resolution: fail when `outsideBox` is large (for example, a single outside region that is at least half the main region's size, or `outsideBox` above about 10 % of `inside`). Keep the small-seam tolerance, which was the F1 problem. Also assert the pass-over count, or fail when more than one board is passed over without the reason being on the floor. The floor run had none passed over, so the new path ran only on the happy side.

**S-2 · `platform-notes/android.md` § Compose test kit (documentation of the kit's cited sources)**
- Observation: the file has no entry for the finding that `performScrollTo` (ui-test 1.12.1) repeats its scroll until none is needed, and that the scroll animates on the paused main clock. That finding is the reason for `KitScreen.scrollTo`. It lives in a code comment and in the run log (f2-sB). The new constants have no cited source either: `room = height − 2f`, the 1 px tolerance in `whollyInView`, and "the nearest ancestor with `SemanticsActions.ScrollBy`".
- Why it matters: the rule is that every non-REQ assertion keeps a cited source. The next kit author will copy `performScrollTo` under a paused clock and hang for 35 minutes.
- Resolution: add a row to § Compose test kit (library fact, the `scrollTo` remedy, and the 1 px provenance from `chooseCell`'s existing test). This is a documentation fix; no kit or product change.

### Notes

**N-1 · `FlipBadge.kt` `clampOnto` and the pins (product; no defect)**
- The vertical clamp is not pinned. I mutated a scratch copy to drop `clampOnto` on `top`, and the whole scaffolding class stays green: 312 boards, 0 off. The bundle never puts the PG's top within about 15 dp of the board's top. The branch is correct on reading (the disc's cy is at least `b.top + r`, the area top is `max(b.top, …)`, and the disc stays inside the area), but it is dead weight to the tests.
- If the board is ever shorter than the area, `clampOnto` returns `from` and the area leaves the board without a signal. This cannot happen at 60 dp against any real board.
- Optional: add a synthetic pin for the top edge.

**N-2 · `KitScreen.scrollTo` (kit)**
- `autoAdvance = true` for one call advances the clock until idle, by an amount the code does not measure. The legs that call it are `chooseCell`, REQ-023 A1's "grid scrolled to its last cell", `kitChooseInGrid`, and A2 itself. A time-driven state behind the grid (a shake, a pulse) could therefore advance further than the leg's own `advance(500)`.
- I found no leg that depends on such a state across the call. In the REQ-023 bar leg it could only produce a false red, never a false green. The 263/0 run and the floor counts (OverviewKitTest 6/0, DevAidScreenKitTest 15/0) agree.
- No action beyond what is on record. A `try/finally` already restores the paused clock, and `check(!autoAdvance)` guards misuse.

## Evidence for each point of the brief

- **FlipBadge, hit order.** The disc is read from `BadgePlace.disc` and the area from `BadgePlace.area`. `HitTest.at` checks the visible discs first (step 2), then drawn shapes (3), the Restart pill (4), badge areas (5), and tray cells or piece margins (6). `badgeSlots` feeds both the drawing (`PlayScreen`) and the hit test. Only `area` moved. At the right edge the 11 dp strip moves inward. That strip was previously board margin or empty, and it is now badge area. A drawn shape still beats it, and it beats a piece's margin, as in the decisions row's order.
- **Disc inside the area.** The disc is clamped by its radius (about 50 px) and the area by its 157 px size. After shifting the area to the board's edge, the disc stays inside it, with a margin of about 28 px against rounding of about 0.5 px. The pins assert this per case (`assertBadgeOnBoard`).
- **REQ-050.A2.** The same keys are asserted from the node (`PuzzleIdKey`, `PuzzleStateKey`). The same pixel area is used (4 dp inside the cell bounds). The thresholds, the `n > 1000` check and the messages are the same. The `judged` set, plus the sorted-equals assertion and the `pending.first()` progress check, makes each of the 106 puzzles judged exactly once. The cell is read from one `overview` capture at its rounded `boundsInRoot` instead of from its own `captureToImage`; both copy the window at the node's bounds.
- **`readDrawnDisc`.** A missing ring gives a small box, so the 20 dp width check fails. Two discs are the gap, S-1. A reading in doubt is returned and then thrown by `drawnDisc`; only `findBoardBadgeOverlap` passes over. The search throws when no case is left, so the pass-over cannot make the F1 leg pass with no case.
- **Mapping read once.** `BoardMappingKey` is geometry only (it comes from `ScreenGeometry.of`). Neither leg changes the host or the layout between the first sample and the last. `SolvedPictureKitTest` reads it on the settled solved board, and `BoardTrayKitTest` on the board that was just opened.
- **Tokens.** 80 distinct, 210 occurrences, as recorded. No main code in the kit diff and no kit file in the product diff.

## Re-derived

| Claim | Command or reading | Result |
|---|---|---|
| g3-jvm run 1 (APKs) | `.\gradlew.bat -q :app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun` | exit 0 |
| g3-jvm run 2 (the unit tests) | `-q :content:testDebugUnitTest --rerun :play:testDebugUnitTest --rerun :kernel:test --rerun :contracts:test --rerun :store:testDebugUnitTest --rerun :app:testDebugUnitTest --rerun` | exit 0; the XMLs sum to **263 / 0 skipped / 0 failures / 0 errors** |
| Kit compile | `:app:assembleDebugAndroidTest :play:assembleDebugAndroidTest` | BUILD SUCCESSFUL, exit 0 |
| Token set | `grep -rhoE "REQ-0[0-9]*\.A[0-9]+" --include=*.kt */src` (sorted unique, and a plain count) | 80 distinct, 210 occurrences |
| Pins fail without the fix | scratch copy of the tree with HEAD's `FlipBadge.kt` | `DecorationScaffoldTest` 8 tests, **2 failed** (the two new pins) |
| Top clamp is pinned | scratch copy with `val top` unclamped | all green, sweep 312 boards, 0 off (see N-1) |
| The mutations touched only the scratch folder | `/c/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/a14970b4-13a8-403c-83f3-0cc7710de28f/scratchpad/wo010/review-delta/proj` | project tree untouched |
| Not re-derived | the floor timings and results (f2-sC/sD): no emulator was run | taken as the run log's record; consistent with the code read |

## Handoff

- Verdict: **forward**
- Counts: **0 B / 2 S / 2 N**
- Product vs test:
  - S-1: test (kit, `DecorationKitTest.readDrawnDisc`)
  - S-2: documentation of test sources (`platform-notes/android.md`)
  - N-1: product pin gap, not a product defect (`FlipBadge.kt` vertical clamp unpinned)
  - N-2: test (`KitScreen.scrollTo`), no action
- The product fix needs no change. Nothing in the project was edited by this review except this file.
