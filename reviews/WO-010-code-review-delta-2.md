# WO-010 delta code review 2 (fresh `code-reviewer`), 2026-10-20

Scope: the F3 kit half, five files in `play/src/androidTest/kotlin/app/tangramplaytime/play/` (`KitScreen.kt`, `SolvedPictureKitTest.kt`, `BoardTrayKitTest.kt`, `SolvedViewKitTest.kt`, `DevAidScreenKitTest.kt`), test tree `cf75b002d650` to `dce62d6e870f`. No product file is in the diff. Read against `decisions.md` 2026-10-20 "F3's cause is the REQ-023.A1 leg's inside mask, not the product or the data", the WO log rows 12:49 and 13:56, and the run-log rows f3-probe-*, f3k-dev, f3k-floor.

## Verdict: forward

0 Blockers · 1 Should · 3 Notes. Every finding is against test code or its documentation.

## Checklist applied

- Pre-change copies are the pre-change files. **Pass** (re-derived, below).
- `kitOutline` is the union's exact boundary for all 106 puzzles. **Pass** (exact-arithmetic replay, below).
- `kitBands`: the 1 px widening decides nothing. **Pass** (argument and replay).
- Self-check oracle independent of `kitOutline` / `kitBands`; able to fail in both directions. **Pass** (mutations, below).
- Each amended leg judges everything it judged before, minus pixels now within 3 px of the outline. **Pass** (replay).
- `PreviewKitTest.kt:220` left unchanged. **Agree**, with S-1 on how it was left.
- No semantics read in a per-pixel or per-sample loop. **Pass.**
- Token set unchanged; every amendment comment names the decisions row. **Pass.**

## Findings

### Blockers
None.

### Shoulds

**S-1 · `PreviewKitTest.kt:220` and `platform-notes/android.md` § Compose test kit (test code and documentation)**
- Observation: `kit.silhouetteMask(p).erode(3)` stays at `PreviewKitTest.kt:220` with no comment. The same file's KDoc for `silhouetteMask` (`KitScreen.kt:498–502`) now says the mask "is not a measure of distance from the outline" and that "3 px in" is `silhouetteBands`. The WO log gives the reasoning ("reasoned and unchanged"). The reasoning is not at the site, and § Compose test kit has no entry for the F3 lesson.
- The reasoning is right. `PlayPalette.previewStroke` is `0xFFFFFFFF` (`PlayPalette.kt:30`), the board colour, and the silhouette is `0xFF4A4E69`. The leg asserts that no near-white pixel sits inside the silhouette with 24/255 tolerance. A pixel whose centre is 0.46 px inside the union is about 96 % silhouette and is far from white. The old inside set is stricter than the exact one near axis edges, so it cannot hollow the leg. It also never leaves the union: in my replay of 12 puzzles (random mappings) the old inside set held no pixel outside the union and none dropped by the new set at 3 px or more. The leg is unpainted by the picture's outline stroke because it runs unsolved.
- Failure scenario: the next kit author sees `erode(3)` described as "3 px inside" in a leg, copies it into a leg whose colour test is not white-versus-dark (for example a piece colour against the board), and rebuilds F3's false red on a different image.
- Resolution: one line at `PreviewKitTest.kt:220` saying why it stays (white stroke colour, unsolved board, no picture outline), and a § Compose test kit lesson: a "N px in" set made by widening a raster and eroding by a square is not an N px distance; measure from the exact outline (`KitScreen.silhouetteBands`). A kit comment plus a notes entry; no product change.

### Notes

**N-1 · `SolvedPictureKitTest.bandFaults` run time (test)**
- The self-check runs 295 s on dev and 42 s on the floor. Dev total for the class went 348 to 642 s. The cost is the per-pixel loop over the whole board (about 1.6 M pixels, 106 puzzles) with `inUnion` calls, `nearEdge` bookkeeping and four mask reads, though most of the board is far from every piece.
- Avoidable without weakening: loop only the union's box grown by `reach + 2` px, and check the rest of the board with two cheap bit tests (the inside set is empty there, the outside set holds every pixel). I estimate 60 to 70 % off. Not needed for the close; the cost is inside reason for a once-per-WO kit.

**N-2 · self-check, old-construction branch (test)**
- `oldFaults.insideClose > 0` and `outsideLeftOut > 0` hold for any puzzle, not for the notch pixel. The old inside set reaches about 2.25 px off an axis edge, and the old outside set leaves out 3 to 3.8 px. So "caught on animals-vulture and letters-z" shows the oracle is sensitive to the old construction, not to the 0.46 px pixel specifically. The F3 pixel itself is guarded by the bundle-wide pass of the new sets. That guard is real: in my mutation, dropping one outline segment from the sets gave 386 inside-too-close and 194 outside-too-close faults on `letters-z`. No action.

**N-3 · REQ-023.A1 and look-alikes, the 3 px margin (test)**
- The 3 px margin works because the picture's outline stroke reaches only its inner half-width, `0.6 dp x density` (1.6 px at 2.625, 1.2 px at the tablet's 2.0), plus anti-aliasing. That dependence is not written at the sites; it would stop holding at about 5x density. All of the bundle's images are far below that. Optional one-line comment in `KitScreen.silhouetteBands`.

**N-4 · tablet image (process)**
- The new sets were run on dev and the floor only (f3k-dev, f3k-floor). The tablet (2560 x 1600 at 320 dpi, a different pixel grid) is pending in TASK-042. Nothing in the geometry depends on the grid, but the self-check's 0-fault count is a per-grid fact, so the tablet run is its first real check there.

## Evidence for each point of the brief

- **Before copies.** I copied the test tree, substituted the five `before/` files, and hashed with the run-log definition (`trees.sh`'s test line): `cf75b002d650`. The live tree hashes `dce62d6e870f`. I made my own `diff -u` of the five files; `KitScreen.kt` only adds (`silhouetteBands`, `kitOutline`, `kitPolygonsPx`, `KitSegPx`, `KitBands`, `kitBands`, comments); nothing is removed.
- **`kitOutline` (point 1).** An independent exact check: coordinates as `a + b*sqrt2` in exact rational arithmetic, every piece edge cut at every vertex that lies on it (exact collinearity), sub-segments counted by multiplicity, boundary = count 1. Compared with a float port of `kitOutline` (the snapping tolerance 1e-6) over all 106 puzzles: the sets of sub-segments are identical, no segment is duplicated, and every outline vertex has even degree (the outline is closed). The bundle exercises the hard cases: 105 puzzles have a piece edge split at another piece's corner (T-junctions and partial collinear overlaps), 7 have an outline vertex of degree 4 (pieces touching at a corner). The cut tolerance is 1e-6 puzzle units against float noise near 1e-15; no corner lies between those. The side probe at 1e-4 units is far below any feature of the bundle. No missed and no spurious segment.
- **`kitBands` (point 2).** The claim holds. A centre outside the union and at least 3 px from the outline cannot be within 1 px of a piece edge: that edge point lies in the closure of the union, and the straight path to it crosses the outline within 1 px. A centre inside the union is inside a piece, or on a shared seam (within the 1 px). Replay on 6 puzzles (random scales and origins, the full window): 0 pixels where the widened membership differs from exact union membership among pixels at least 3 px from the outline. The old-versus-new replay on 15 puzzles: the new inside set drops no old-inside pixel at 3 px or more (max dropped distance 2.65 to 2.998), the new outside set drops no old-outside pixel (the old outside starts at 3.70 to 4.19 px), and the old inside set never holds a pixel outside the union.
- **Self-check (point 3).** The oracle (`bandFaults`) takes the polygons only, with its own point-in-piece and corner logic, and shares only `kitDistToSegment` and `kitPolygonsPx`. My host port of it, on `letters-z`: exact sets 0/0/0/0 faults; margin 2.5 px gives 797 inside-too-close and 1317 outside-too-close; margin 3.6 px gives 588 inside-left-out and 815 outside-left-out; a dropped outline segment gives 386 and 194 too-close; a spurious internal edge in the outline gives 1123 inside-left-out. It fails both ways, and the left-out rule bites at 3.3 px and up (the 0.25 px slack is the only gap). The disc test is sound: the 96 circle points have a sagitta of 3 x (1 - cos 1.875 deg) = 0.0016 px, every outline turn is a piece corner (the interior angles are 45 degrees or more, so the 32-point 0.05 px ring sees the outside wedge), and a piece lying wholly inside the disc has a corner inside it. A set wrong by more than the sliver is therefore caught.
- **Amended legs (point 4).** All eight read the new set and keep every other line: REQ-023.A2 (both its mask loop and the edge-sample rule: the sample and both 3 px sides must be in `inside`, as before), REQ-039.A1, REQ-045.A2, REQ-011.A1 (the seam samples are still taken, and `seamSamples >= 20` stays), REQ-011.A2 (the outside fixture count stays above 1000), and the three helpers in `SolvedViewKitTest` / `DevAidScreenKitTest`. The REQ-023.A2 shared-edge samples that pass the inside filter, replayed at 106 puzzles: 30 728 with the new set against 30 627 with the old; no puzzle has fewer (smallest 68). No assertion is dropped or weakened; the sets grow except for pixels now within 3 px of the outline.
- **`PreviewKitTest.kt:220` (point 5).** Agree; see S-1.
- **Run time and platform rules (point 6).** `silhouetteBands` makes one `bounds` read and one `BoardMappingKey` read, and nothing per pixel. The self-check reads `bounds` and `mapping()` once per puzzle. The cost is in N-1.
- **Tokens (point 7).** `grep -roh --include=*.kt 'REQ-0[0-9]*\.A[0-9]*' .` from the project root: 80 distinct, 210 occurrences. The self-check `@Test` carries no token. Each amended leg has a "Amended WO-010 (F3, recirculation 6 ...)" comment quoting the decisions row title; the row exists (`decisions.md` line 183).

## Re-derived

| Claim | Command or reading | Result |
|---|---|---|
| The five `before/` copies are the pre-change files | scratch copy of the test tree, five files substituted, helper's test-tree hash | **cf75b002d650**; live tree dce62d6e870f |
| Kit compile | `:play:compileDebugAndroidTestKotlin --rerun :app:assembleDebugAndroidTest :play:assembleDebugAndroidTest` | BUILD SUCCESSFUL; only the two old `DecorationKitTest.kt:308/:316` warnings |
| Token set | `grep -roh --include=*.kt 'REQ-0[0-9]*\.A[0-9]*' .` | 80 distinct / 210 occurrences |
| Outline exact | own exact-arithmetic replay, all 106 puzzles | 0 mismatches, 0 open outlines |
| Bands | own replays (scratch, pure Python, no numpy) | 0 membership violations; new sets contain the old except within 3 px |
| Self-check can fail | own host port of the oracle, 4 mutations | all four red (see above) |
| Not re-derived | the device XMLs (f3k-dev, f3k-floor) and the run times | taken from the run log as counted by the orchestrator; no device used |

Scratch: `C:/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/a14970b4-13a8-403c-83f3-0cc7710de28f/scratchpad/wo010/review-delta2/` (`host/outline_check.py`, `bands_check.py`, `old_new.py`, `oracle_mut.py`, `a2_samples.py`; `diff/` holds my five diffs).

## Handoff

- Verdict: **forward**
- Counts: **0 B / 1 S / 3 N** (N-4 is a process note)
- Product vs test:
  - S-1: test code comment and documentation (`PreviewKitTest.kt:220`, `platform-notes/android.md`)
  - N-1, N-2, N-3: test
  - N-4: process (the tablet run is TASK-042's)
- Nothing in the project was edited by this review except this file.
