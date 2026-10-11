# WO-002 Test & Verify (2026-10-11 15:42)

Verdict: **GREEN**. 0 failures, 0 Blockers.

## Reuse / re-run decision

Tree hashes recomputed (recipe of the run log) and compared with run-log row r3:

| Tree | r3 | Now | Match |
|---|---|---|---|
| test (`content/src/test`) | 9be1ef7aa1bb | 9be1ef7aa1bb | yes |
| product (`content/src/main` + `content/build.gradle.kts`) | 9775feb3e84f | 9775feb3e84f | yes |
| tools (`tools/tangram_geom.py` + `tools/tests`) | 5b621365f638 | 5b621365f638 | yes |

All three match, so r3 is the baseline and the Gradle suites were **not re-run**. Result XML read from disk (all stamped 15:41, the r3 run):

- ContentScaffoldTest 2/0/0 failures/errors/skipped
- PuzzleLibraryKitTest 16/0/0
- PuzzleLoaderKitTest 37/0/0
- scaffolding.ParserScaffoldTest 9/0/0
- ContractsSanityTest 2/0/0
- Total 66 tests, 0 failures, 0 errors, 0 skipped.

## Channels

| Channel | Command | Result |
|---|---|---|
| JVM `:content:testDebugUnitTest` | run-log r3 (reused) | 64 tests green |
| JVM `:contracts:test` | run-log r3 (reused) | 2/0/0 |
| Python tool test | `python tools/tests/test_tangram_geom.py` (run now) | exit 0: "corner sets match for 13 files; warmup-4 gains exactly (2,2)" |
| Python validator | `python tools/validate_puzzles.py Tangrams` (run now) | exit 0: 13/13 puzzle files valid, a build order printed for each |
| Artifact listing | `unzip -l` on existing artifacts (reused a0 and TASK-004) | 14 entries under `app/tangramplaytime/content/puzzles/` in the debug APK, the unsigned release APK, and the AAR `classes.jar` |
| Device | not named by the WO (amended nowhere) | none required; no waiver needed |

Artifact reuse basis: `Tangrams/*.json` are unchanged (git clean on `Tangrams/`, mtimes 06:14). The only edit to `content/build.gradle.kts` after the a0/r1 artifacts is the `testImplementation(project(":kernel"))` line, which does not touch the bundle task. The debug APK was rebuilt at 15:31, after that edit, and still lists 14. The release APK and AAR date from 14:55, before the edit. Their listings are reused on the argument that the bundle task is untouched. The bundle depends only on the JSON files and the task block.

## Per acceptance ID (JVM channel)

| ID | Result | Asserting test | Substance |
|---|---|---|---|
| REQ-007.A1 | PASS | `PuzzleLibraryKitTest.everyShippedPuzzleIsListedAndMeetsTheShapesTheArtAndTheOrderRules` | One listed puzzle per Tangrams file, schema excluded. Independent re-measure per puzzle: piece set, congruence, no overlap, edge-connected, picture (`kitArtProblems`, S2 fix present), rating 1..5, en+fi titles. |
| REQ-007.A2 | PASS | `theListOpensWithTheMiniPuzzlesThenTheWarmUps` | Leading run of minis, then a run of warm-ups. Both runs must include every item of their kind, so a mis-sorted list fails. |
| REQ-038.A1 | PASS | 16 tests in `PuzzleLoaderKitTest` (shipped texts load, plus one crafted rejection per rule) | Each rejection asserts that rule is the only violation (`kitAssertOnlyRule`), and fixtures are self-checked. Rules covered: corner-only join, overlap, wrong size, mini repeat, mini of 7, full of 6, full repeat, missing en and fi title, rating 0 and 6, unknown kind, bad art colour. Boundary accepts: mini of 1 and 6 pieces, rating 5. Status stays `implemented (unverified)`, pending the validity-rules CHG. |
| REQ-040.A1 | PASS | `theFirstListedPuzzleHasRatingOne` | The first entry of `puzzles` has rating 1. |
| REQ-040.A2 | PASS | `aFullPuzzleOfRatingOneWhoseIdSortsFirstStillComesAfterEveryWarmUp` | Edited fixture whose id sorts before "shapes-warmup" and "shapes-mini". A kind-blind sort would list it first, so the test discriminates. |
| REQ-041.A1 | PASS | `theFirstFourPuzzlesAfterTheMinisAreWarmUpsOfRatingOne` | Checks kind and rating 1 on the four entries after the minis. |
| REQ-041.A2 | PASS | `everyListedWarmUpMeetsTheHalfOutlineMeasure`; `aWarmUpWithAPieceBelowHalfOnTheEdgeIsRejected`; `theShippedWarmUpsWithAPieceExactlyOnTheLineAreAccepted` | The fixture has one SQ at 0.25 while the mean is above 0.5, so a mean-based check would wrongly pass. The same arrangement as a full puzzle loads. The exact-0.5 edge is accepted. |
| REQ-042.A2 | PASS | `atLeastFourCategoriesArePresent` | Counts distinct categories over the shipped library, at least 4. |
| REQ-042.A3 | PASS | `shapesSquareIsAmongTheListedPuzzles` | Checks both `byId` and `puzzles`. |
| REQ-045.A1 | PASS | `theFirstListedPuzzleIsAMiniWhoseTrayHoldsThreePieces` | Kind is mini and `pieces.size == 3`. This is the seam half only; the screen half is WO-004's. |

**Token audit.** `grep REQ-0` over `content/src/test` shows exactly the ten IDs above. REQ-038.A1 is on 16 tests (N1 fix: the bow-tie square test is demoted to a decision pin). REQ-041.A2 is on 3 tests. There is no token on REQ-038.A2, REQ-039.*, REQ-042.A1 or REQ-045.A2. Tokens appear only in the two kit test files. `KitTexts.kt` and the pins carry none. No empty bodies and no proxy assertions found.

**Review fixes verified in the tree.**
- S1: the pin `anUnknownArtShapeTypeIsOneJsonViolation` exists (`PuzzleLoaderKitTest.kt:269`).
- S2: `kitArtProblems` is called in the REQ-007.A1 test (line 88).
- N1: the bow-tie test is untokened (`// decision decisions.md line 46`).

**Coverage gaps, as scoped by the WO.**
- The shapes REQ's printed-build-order line is carried `implemented (unverified)`. Its tool-side evidence is the validator run.
- The picture REQ's lines go to WO-003 and WO-009.
- The count line of REQ-042 goes to the content batch WO and WO-009.
- The mini REQ's solved-picture line goes to WO-003.
- The "fresh install opens on" screen halves go to WO-004.
- The device read of the bundle (code-review N4) is carried to WO-003.

## Verifier runner

`python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project C:/GitHub/AI/TangramPlaytime --wo WO-002` last line:
`verifiers: none found under .swdev/verifiers (discovery mode) — nothing to run`
Budget line for Metrics: nothing to run.

## Failures and classification

None. No product defect and no test defect found by this station (corrected checks stay at the 2 counted at P5: S2 and N1).
