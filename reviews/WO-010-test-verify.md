# WO-010 test and verify (#Content batch: REQ-007.A1-A2, 038.A1, 039.A1, 040.A1-A2, 041.A1-A2, 042.A2-A3, 045.A1)

Station: test-verify, 2026-10-20, 17:08 to 17:12 (local). Scratch and copied XMLs: `C:/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/a14970b4-13a8-403c-83f3-0cc7710de28f/scratchpad/wo010/tv010/` (`jvm/`, `jvm-mine/`, `floor/`, `dev/`, `tablet/`, `sum.py`, `validate.txt`, `colours.txt`, `previews/`).

**Verdict: GREEN, forward.** Every channel the WO names has a run on the final tree, 11 of 11 in-scope IDs are asserted by tests that can fail, and the only failures anywhere are the two guarded `ProcessDeathKitTest` phases per app set. I found no product defect, no test defect and no channel gap. Three observations (none blocking) are at the end.

## Tree hashes (recipe of the run log header; `wo010/trees.sh` run from the project root)

| | at my start (17:08) | at hand-back (17:11, before this file was written) | run log t042c-a |
|---|---|---|---|
| HEAD / status lines | a0d3938 / 217 | a0d3938 / 217 | a0d3938 / 217 |
| test tree | 598b6af2b8bf | 598b6af2b8bf | 598b6af2b8bf |
| product tree | 683e7ab5cc58 | 683e7ab5cc58 | 683e7ab5cc58 |
| bundle (count) | 02feaef5d941 (106) | 02feaef5d941 (106) | 02feaef5d941 (106) |
| tools tree | 0a7a3a785c3b | 0a7a3a785c3b | 0a7a3a785c3b |

Equal at all three points. The status count becomes 218 with this file, the only repo change of mine. No test, product, bundle or tool file has an mtime later than 14:10 on 2026-10-20, and the first device run of the baseline started 14:14:36, after the kit compile (14:13:49-57). The device kits were therefore built from the final test tree.

## Baseline taken, and why

The hashes match, so the orchestrator's t042c rows are the baseline for the device channels (I did not re-run any device class). I opened every XML myself with my own summing script (`tv010/sum.py`; it counts `testcase` elements and their `failure`/`error`/`skipped` children, not the suite attributes). Device XML stamps are UTC; local = UTC + 3.

| Channel (row) | Command started | XML stamps (local) | Sum (tests / failures) | What the failures are |
|---|---|---|---|---|
| JVM, run 2 (t042c-a) | 14:13:01 | 14:13:03 to 14:13:12 | content 80, play 109, kernel 56, contracts 2, store 12, app 4 = **263 / 0**, 0 errors, 0 skipped | none |
| Floor play set (t042c-floor) | 14:14:36 start of the floor script | 15:00:03 | **97 / 0** (BoardTray 7, CornerPulse 4, Decoration 14, DevAidScreen 15, Drag 20, Overview 6, Preview 2, ResumeScreen 5, SolvedPicture 7, SolvedView 5, TopBar 12) | none |
| Floor `DevPromptKeyboardKitTest` | same script | 15:00:52 | **1 / 0** | none |
| Floor app rest | same script | 15:20:27 | **33 tests, 2 failures** (AppLaunch 2, AppWiring 2, DevAidApp 2, FloorChannel 1, Language 1, PhoneLayout 6, PlayThrough 4, ProcessDeath 2, ScaleMeasure 3, TabletLayout 7, TouchTarget 3) | exactly `ProcessDeathKitTest#phase1` and `#phase2`, both `AssumptionViolatedException` (the guard). Language shows 1 because the three `@SdkSuppress(33)` legs are filtered on API 26 (absent, not skipped) |
| Floor `ScaleMeasureTest` x3 | same script | 15:21:07, 15:21:49, 15:22:27 | 3 / 0 each | none |
| Dev play set (t042c-dev) | 15:24:13 start | 16:19:04 | **97 / 0**, same 11 classes and counts as the floor | none |
| Dev `DevPromptKeyboardKitTest` | same | 16:19:57 | 1 / 0 | none |
| Dev app rest | same | 16:39:37 | **36 tests, 2 failures** (Language 4 here) | exactly the two `ProcessDeathKitTest` phases |
| Dev `ScaleMeasureTest` x3 | same | 16:40:06, 16:40:45, 16:41:24 | 3 / 0 each | none |
| Tablet `DevPromptKeyboardKitTest` (t042c-tablet) | 16:43:13 start | 16:43:47 | 1 / 0 | none |
| Tablet app rest | same | 17:02:49 | **36 tests, 2 failures**, same class counts as dev | exactly the two `ProcessDeathKitTest` phases |
| Tablet `ScaleMeasureTest` x3 | same | 17:03:23, 17:04:03, 17:04:42 | 3 / 0 each | none |

Every device XML is stamped after its script's start and after the kit's last change. The two guarded phases are the only `<failure>` elements in any device XML, and no XML carries a `<skipped>` or `<error>` element. The play set was run on the floor and dev images only, the tablet ran the app set and `ScaleMeasureTest` only, and R10 excluded WO-006's floor root-route steps; all three match the channel table. The `assumeTrue` in `DragKitTest` (line 370) did not fire: Drag shows 20 of 20 on both images, none skipped.

The scale record (t042c-scale) is a record, not a verdict: R6's 5 s and 1 s thresholds are not crossed on any image (floor launch 1512 ms, grid open 331 ms; dev 2137 / 235 ms; tablet 2115 / 235 ms). I did not re-measure it.

## What I ran

All on the final tree, started 17:09 to 17:11 local, Python with `PYTHONUTF8=1`, no device used (`adb devices` empty, no qemu, before and after).

| Item | Command | Result | XML or log |
|---|---|---|---|
| Validator | `python tools/validate_puzzles.py Tangrams` (17:09:21) | exit 0, "106/106 puzzle files valid"; 106 lines each with `build order …` (REQ-038.A2's tool evidence, carried outside `--wo`) | `tv010/validate.txt` |
| Tool test, geometry | `python tools/tests/test_tangram_geom.py` | "ok: corner sets match for the 13 pinned files (106 puzzle files in Tangrams); warmup-4 gains exactly (2,2)" | console |
| Tool test, import | `python tools/tests/test_import_figures.py` | "ok: 9 import tool tests" (its two "0/1 files with 3 to 8 colours" lines are the test's crafted-fixture output, not a result) | console |
| Whole-folder render | `python tools/render_puzzle.py Tangrams <scratch>/previews` | 106 SVGs rendered into the scratch; `Tangrams/previews` untouched (`git diff --name-only -- Tangrams/previews` empty) | `tv010/previews/` |
| Colour report | `python tools/import_figures.py colours` over the 93 ids of `tools/import/batch-1..4.txt` (as `Tangrams/<id>.json`) | "93/93 files with 3 to 8 colours and a drawn picture", exit 0 | `tv010/colours.txt` |
| JVM run 1 | `cmd.exe //c ".\gradlew.bat -q :app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun"`, 17:09:45 | exit 0; release APK written 17:09:47 | console |
| JVM run 2 | `cmd.exe //c ".\gradlew.bat -q :content:testDebugUnitTest --rerun :play:testDebugUnitTest --rerun :kernel:test --rerun :contracts:test --rerun :store:testDebugUnitTest --rerun :app:testDebugUnitTest --rerun"`, 17:09:49 | exit 0; **263 / 0 / 0 errors / 0 skipped**: content 80, play 109, kernel 56, contracts 2, store 12, app 4. XMLs stamped 14:09:51 to 14:10:00 UTC (17:09:51 to 17:10:00 local), after the start | `tv010/jvm-mine/<module>/` |
| V-01 | `python .swdev/verifiers/v01_release_apk.py` | **PASS 3/3** on the 17:09:47 release APK: no `uses-permission`; canary in none of 2 dex; 0 okhttp3, 0 `Ljava/net/` from 393 product classes | console |
| V-01 positive control | `… --positive-control` | **PASS 2/2** (canary in `classes5.dex`; 78 `Ljava/net/` outside product code) | console |
| Runner | `python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-010` | **GREEN**: 1 verifier, 150 lines, 0 failures. Budget line: "WO-010 added 0 verifier(s), 0 lines — budget {count 1, lines 150} — within budget" | console |
| trace-check | `python C:/GitHub/AI/SWDev/framework/skills/trace-check/trace_check.py --project . --wo WO-010` | **GREEN**, 11 of 11 in-scope IDs covered; req-lint 0 errors; 29 outbox drafts awaiting capture | console |
| Baseline XML sum | `tv010/sum.py` over the copied `task-042c` XMLs | the table above | `tv010/jvm/`, `floor/`, `dev/`, `tablet/` |

The JVM run reproduced the baseline's counts exactly (263/0, same per-module split), so the baseline and my re-run agree.

## Per ID, per channel

"Py" is the Python tool channel; JVM is the two-run line; F, D, T are the floor (API 26), dev (API 37) and tablet images. A dash means the channel table does not name that channel for the ID.

| ID | Test (file) | Py | JVM | F | D | T | Verdict |
|---|---|---|---|---|---|---|---|
| REQ-007.A1 | `PuzzleLibraryKitTest.everyShippedPuzzleIsListedAndMeetsTheShapesTheArtAndTheOrderRules` | validator 106/106 | PASS (also `AssetLicencesKitTest` colour pin on the 93) | - | - | - | PASS |
| REQ-007.A2 | `PuzzleLibraryKitTest.theListOpensWithTheMiniPuzzlesThenTheWarmUps` | - | PASS | - | - | - | PASS |
| REQ-038.A1 | `PuzzleLoaderKitTest` (16 tokened tests: shipped texts load with no violation, one crafted text per rule) | validator 106/106 | PASS 37/37 in the class | - | - | - | PASS; **stays `implemented (unverified)`** on `chg-draft-req038-validity-rules-in-contract.md` (scope note) |
| REQ-039.A1 | `SolvedPictureKitTest.noPicturePaintsOutsideItsPuzzlesOutline` (all 106; fade start, mid-fade, after) | colour report 93/93 | - | PASS (7/7 class) | PASS (7/7 class) | - | PASS |
| REQ-040.A1 | `PuzzleLibraryKitTest.theFirstListedPuzzleHasRatingOne`; `BrowsingKitTest.theFirstPuzzleShownOnAFreshInstallHasRatingOne`; `AppLaunchKitTest.theFirstPuzzleShownOnAFreshInstallHasRatingOne` | - | PASS | PASS (AppLaunch 2/2) | PASS | PASS | PASS |
| REQ-040.A2 | `PuzzleLoaderKitTest.aFullPuzzleOfRatingOneWhoseIdSortsFirstStillComesAfterEveryWarmUp` (crafted lists) | - | PASS | - | - | - | PASS |
| REQ-041.A1 | `PuzzleLibraryKitTest.theFirstFourPuzzlesAfterTheMinisAreWarmUpsOfRatingOne`; `BrowsingKitTest.onAFreshInstall…` (walks › over all 106); `TopBarKitTest.onAFreshInstall…` (walks › on the screen) | - | PASS | PASS (TopBar 12/12) | PASS | - | PASS |
| REQ-041.A2 | `PuzzleLibraryKitTest.everyListedWarmUpMeetsTheHalfOutlineMeasure`; `PuzzleLoaderKitTest` (below-half rejected; exactly-half accepted) | V12 inside the validator | PASS | - | - | - | PASS |
| REQ-042.A2 | `PuzzleLibraryKitTest.atLeastFourCategoriesArePresent` (bundle holds 8: animals 16, letters 22, nature 5, numbers 9, people 1, shapes 25, things 23, vehicles 5) | - | PASS | - | - | - | PASS |
| REQ-042.A3 | `PuzzleLibraryKitTest.shapesSquareIsAmongTheListedPuzzles` | - | PASS | - | - | - | PASS |
| REQ-045.A1 | `PuzzleLibraryKitTest.theFirstListedPuzzleIsAMini…`; `BrowsingKitTest.aFreshInstallOpensOnAMini…`; `BoardTrayKitTest.aFreshControllerOpensOnAMini…`; `AppLaunchKitTest.aFreshStartOpensOnAMini…` | - | PASS | PASS (BoardTray 7/7, AppLaunch 2/2) | PASS | PASS (AppLaunch in the app rest) | PASS |

Regression lines that iterate the bundle (not WO-010 IDs) are green on their own channels in the same XMLs: REQ-002.A1 (`PlayControllerKitTest`, JVM), REQ-011.A1 (`BoardTrayKitTest`, 7/7 floor and dev), REQ-046.A3 (`DevAidScreenKitTest`, 15/15 floor and dev), REQ-050.A1 to A3 (`OverviewKitTest` 6/6, `TopBarKitTest` 12/12, `BrowsingKitTest`, floor and dev), REQ-023.A1 and A2 (`SolvedPictureKitTest` 7/7 on both). The REQ-003.A1 leg ran 1465 s on the floor and 1485 s on dev.

## Substance audit (source read by me)

Token set: `grep` of `REQ-NNN.A<n>` over the kit folders gives 210 occurrences of 80 distinct IDs, the figure the orchestrator logged (no ID added or lost). No `assume` or `@Ignore` in any kit file other than `ProcessDeathKitTest` (the guard, by design) and `DragKitTest:370` (did not fire). Each WO ID, with what it asserts and why it can fail:

- **REQ-007.A1.** Loads the bundle through `PuzzleLibraries.bundled`, which throws if any file breaks a rule, then re-measures every listed puzzle with the kit's own geometry: seven pieces exactly once (mini: 1 to 6 distinct), each congruent, no overlap, edge-connected, a well-formed picture, rating 1 to 5, both titles; one listed puzzle per file in the folder (`assertEquals(folder, listed ids)`). Over all 106. Each rule's rejection is proved separately by the crafted texts of `PuzzleLoaderKitTest`. Sound. The 3 to 8 colour half of "meets REQ-039" is not in this test; it is the untokened pin in `AssetLicencesKitTest` (93 imports, not the 13 shipped files, by `decisions.md` "What 'a well-formed picture' means"). See observation 2.
- **REQ-007.A2.** All minis form the opening run, then all warm-ups form the next run, with a liveness check (at least one of each). Fails for any interleaving. Sound.
- **REQ-038.A1.** The shipped-texts test asserts load without violation, listed ids equal file stems; every rule of the pending CHG has a crafted text that breaks exactly that rule and `kitAssertOnlyRule` demands that rule and no other, for that file only. Fixtures are checked (the corner-only join really touches at a point; the overlap really overlaps and still shares an edge). The two clauses with no possible crafted text are stated in the file header. Sound; unverified status is for the capture side, as scoped.
- **REQ-039.A1 (device).** Over every bundled puzzle, the board pixels at least 3 px outside the exact outline (`kit.silhouetteBands`) equal what they were before the solve, at 650 ms, 1000 ms and after the fade, with a fixture (outside set above 1000 px, same capture size). A picture painting past the clip would change them, so it can fail. The 3 px band was corrected under F3 (corrected check, `decisions.md` 2026-10-20), and its construction is proved by `kitSelfCheckTheThreePxSetsKeepThreePxFromTheExactOutline` against an independent disc oracle over all 106 (0 faults on the new sets; the old construction is caught on `animals-vulture` and `letters-z`). Green on floor and dev. Sound.
- **REQ-040.A1.** The seam test, the JVM controller test and the real-launch device test each read the first puzzle shown on an empty store and assert rating 1 (the device test also ties it to the title text and to `puzzles.first()`). Sound.
- **REQ-040.A2.** The corrected check (TASK-033): it no longer pins "the only full rating-1 puzzle is the first full puzzle" (a fact of the 13 files) but the REQ-040 order itself via `kitOrderPlaceProblems` (everything listed before it sorts before it, everything after sorts after). Crafted early-id full puzzles make the case exist whatever the bundle holds; fixtures check the ids sort before `shapes-warmup` and `shapes-mini`; four crafted wrong lists (before a warm-up, rating-and-id order only, after a higher rating, wrong id order) are each shown rejected. Sound.
- **REQ-041.A1.** Three tests walk the real order and assert the four puzzles after the minis are warm-ups of rating 1; the controller test also asserts the walk equals `puzzles` (so the whole of the 106 is stepped). Sound.
- **REQ-041.A2.** Measures every listed warm-up with the kit's own exposure function (at least 0.5 within 1e-6) and, with crafted texts, a warm-up with one piece at 0.25 (mean above 0.5, so only an every-piece check rejects it) is refused, the same arrangement as a full puzzle loads, and exactly 0.5 is accepted. Sound; the validator's V12 gives the independent tool evidence.
- **REQ-042.A2 / A3.** Straight assertions on the library (at least four categories; `byId("shapes-square")` and a listing). Can fail by removing the file. Sound; the "hand-checked/reviewed" half of REQ-042.A1 is WO-009's, out of scope.
- **REQ-045.A1.** Four tests at three layers assert a mini puzzle with exactly three pieces, a tray holding exactly those pieces, nothing on the board, state New; the device ones judge displayed nodes. Sound.

Pins without tokens that this WO added, read for substance: `CounterDigitsKitTest` (default-locale switched to non-ASCII digits, fixture proves `String.format` yields them, restored in `finally`), `AssetLicencesKitTest` (16 tests: the record's shape against the folder, the colour count 3 to 8, and crafted records or `art` each shown to fail its check, with liveness checks), the F1 scaffolding pins (`DecorationScaffoldTest`, 8/0 in the JVM XML) and `DecorationKitTest`'s two self-checks. None carries a REQ token; none is counted as coverage.

Gaming check: no token sits on a test that asserts nothing. Every tokened test of the 11 IDs has at least one assertion that fails when the REQ's meaning is violated.

## Defects

None found: no product defect, no test defect (no corrected check from me), no channel gap. The 16 corrected checks logged by the orchestrator were not re-judged beyond reading the ones that touch my IDs (REQ-040.A2 and the three `SolvedPictureKitTest` legs); the reading is above.

## Observations (not defects, no recirculation)

1. **REQ-039.A1 has no mutation proof of its own.** The 3 px bands are self-checked, but nothing shows the leg turning red when a picture does paint outside the clip, since the clip is in the product and the data cannot break it. Honest oracle, hollow only if the bands were empty, which the `outside.count() > 1000` fixture rules out. Recorded for the record only.
2. **The 3 to 8 colour rule is kept by an untokened pin over the 93 imports only.** The 13 shipped files are excluded by an owner-visible decision (`shapes-mini-2`), so REQ-007.A1's "meets REQ-039" is proved for rule 1's colour count on 93 of 106 puzzles, plus the well-formed-picture check on all 106. This is the planned shape (`decisions.md` 2026-10-11 and R8), not a gap.
3. **Human eyes.** The pictures were looked at by the orchestrator through contact pages (WO Log, 2026-10-19 18:57 to 19:11); I did not re-open them, and owner rule 7's review of the pictures is WO-009's. REQ-038.A1 stays unverified and REQ-038.A2 is carried outside `--wo`, both as scoped.

## Hand-back

`adb devices` empty, no qemu, no device used by me. No version-control write, no ledger edit, `.swdev/log/` untouched. The only file I created in the repo is this one.
