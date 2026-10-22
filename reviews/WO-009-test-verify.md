# WO-009 Test & Verify (P6, the pre-marks verdict) - test-verify, fresh

Tree at the start and at the end: `f80ed88 · 10 · test 598e72459933 · product 6550e479342c · 02feaef5d941 (106) · tools f1421752fb16`. This equals the t070 rows' tree, so the t070 rows are the baseline and no device set was re-run. This is the first of two verdicts. TASK-071 gets the delta verdict after the owner's marks.

**Verdict: forward** (to the Traceability Auditor). 0 Blockers, 0 Shoulds, 6 Notes. Nothing fails for a wrong reason.

The only reds are the six `ReleaseSetKitTest` release legs, which are the expected red until the marks. The three owner-evidence IDs are awaiting the owner's signature. Both are listed below, not hidden.

## Per acceptance ID, per channel

Legend: **P** pass · **XR** expected red (until the owner's marks) · **OE** owner evidence awaiting signature · **n/a** not applicable to that channel.
Channels: PY = Python tool channel; JVM = two-run line; FLOOR = `Phone_API_31` (the minSdk 31 floor); DEV = `Medium_Phone_API_37.0`; TAB = `Medium_Tablet_API_37.0`; REL = V-01 + control, `bundleRelease` gate, release install leg.

| ID | Test (home) | PY | JVM | FLOOR | DEV | TAB | REL | Status |
|---|---|---|---|---|---|---|---|---|
| REQ-001.A2 | none (no token, by design) | n/a | n/a | n/a | n/a | n/a | n/a | **OE** (live listing; `owner-evidence.md` row unsigned) |
| REQ-002.A2 | `NoToddlerModeKitTest.noPlayerScreenOffersAControlOrATextItsReqsDoNotName` (`play/` androidTest) + `SettingsKitTest.noDifficultyControlIsPresent` (settings half) | n/a | n/a | P 3/0 · Settings 13/0 | P 3/0 · Settings 13/0 | Settings 13/0 P; NoToddler not run (see N4) | n/a | **P** |
| REQ-039.A2 | `ReleaseMarksKitTest.noPuzzleFileTheReleaseApkCarriesHasReviewedByHumanFalse` (`play/`) | n/a | P 2/0 | n/a | n/a | n/a | n/a | **P, vacuous** (empty release set, N1). The real verdict is TASK-071's. |
| REQ-042.A1 | `ReleaseSetKitTest.theReleaseContainsAtLeast20PuzzlesWithReviewedByHumanTrue` (`content/`) | `status` 0 of 20 | **XR** | n/a | n/a | n/a | gate refuses, 0 of 20 | **XR** |
| REQ-042.A2 | `ReleaseSetKitTest.atLeastFourCategoriesArePresentInTheRelease` | `status` 0 of 4 | **XR** | n/a | n/a | n/a | gate refuses | **XR** (the WO-002 whole-library token in `PuzzleLibraryKitTest` stays green, N3) |
| REQ-042.A3 | `ReleaseSetKitTest.shapesSquareIsAmongTheReleasedPuzzles` | `status` not marked | **XR** | n/a | n/a | n/a | gate refuses | **XR** (same remark) |
| REQ-048.A1 | none (by design) | n/a | n/a | n/a | n/a | n/a | n/a | **OE** |
| REQ-048.A2 | none (by design) | n/a | n/a | n/a | n/a | n/a | n/a | **OE** |
| REQ-048.A3 | `ReleasePromiseKitTest.theReleaseApkDeclaresNoPermission` (JVM) + `PlayThroughKitTest.theInstalledAppDeclaresNoPermissionsNotEvenNetworkAccess` (device) | n/a | P 2/0 | P 4/0; S-62: 0 entries in all four permission sections, control 680 | P 4/0 | P 4/0 | V-01 PASS 3/3, control 2/2 | **P** |
| REQ-048.A4 | `LauncherIconKitTest` (device); `StoreIconKitTest` x3 tokened tests (JVM) | `test_make_icon` 6/6 | P StoreIcon 5/0 | P LauncherIcon 1/0 | P 1/0 | P 1/0 | n/a | **P** |

Pins without a token also ran green: `StoreListingKitTest` 7/0, `AssetLicencesKitTest` 16/0, `SoundAssetsKitTest` 1/0, and `ReleaseSetKitTest`'s 11 crafted-fixture legs (17 tests, 6 red).

In-scope tally: 10 IDs. 4 pass (REQ-002.A2, REQ-039.A2 vacuous, REQ-048.A3, REQ-048.A4). 3 are expected red (REQ-042 A1..A3). 3 are owner evidence awaiting (REQ-001.A2, REQ-048.A1, REQ-048.A2). That is 4 + 3 + 3 = 10.

## Channels and their baseline

I re-counted every XML myself with my own script. I did not reuse the orchestrator's counts.

| Channel | Baseline row or my re-run | Result (my count) |
|---|---|---|
| Python | my re-run 17:41 (`tv/py.txt`) plus `t070-py` | validator 106/106 valid; `validate.py` 0 errors, 66 warnings; mark tool `ok: 14`; icon tool 6/6; geom ok; import 9 ok; `status` 0 of 106 marked, `release-ready: no`, five unmet items; `git status Tangrams` empty |
| JVM | my re-run, run 1 exit 0, run 2 `--continue` exit 1 (fresh XMLs, none stale), and the `t070-jvm` XMLs in `task-070/jvm/xml/` | both: **351 tests, 6 failures, 0 errors, 0 skipped** (app 22/0, content 97/6, contracts 2/0, kernel 70/0, play 148/0, store 12/0). The six are exactly `ReleaseSetKitTest`'s release legs: A1, A2, A3, loads, mini, warm-ups. Each reads "the release set (0 file(s)) does not load ... rule=EMPTY". |
| `bundleRelease` gate | my re-run | refused at `:content:checkReleasePuzzles` with the five unmet items and the `status` pointer; `app/build/outputs/bundle` absent. `t070-gate` also shows `assembleRelease` builds (exit 0) |
| FLOOR (`t070-floor` XMLs) | baseline, not re-run (hashes match) | play 105/0; DevPromptKeyboard 1/0; app rest 56 with 2 failures; total 162 tests / 2 failures / 0 skipped. S-62 release install leg PASS. |
| DEV (`t070-dev` XMLs) | baseline | play 105/0; DevPromptKeyboard 1/0; app rest 59 with 2 failures; total 165 / 2 / 0 |
| TAB (`t070-tablet` XMLs) | baseline | DevPromptKeyboard 1/0; app rest 59 with 2 failures; total 60 / 2 / 0 (app set only, as planned) |
| V-01 | my re-run via the runner | V-01 PASS 3/3 on `app-release-unsigned.apk`; positive control 2/2 |
| Owner's eyes | n/a | live listing (three rows) and the marks: not yet |

On the device "2 failures": all six (two per image) are `ProcessDeathKitTest.phase1` and `phase2`. In the XML they are `AssumptionViolatedException: phaseN runs only with -e deathPhase N (given: null)`. They are the guarded death-pair phases, a separate channel that is not a WO-009 channel (the tasks.md channel note: "no persistence or lifecycle change"). I read the messages myself. No `<skipped>` element exists in any device XML.

Per-class device results for the WO's classes, from the XMLs:

| Class | Floor | Dev | Tablet |
|---|---|---|---|
| `LauncherIconKitTest` | 1/0 | 1/0 | 1/0 |
| `NoToddlerModeKitTest` | 3/0 | 3/0 | not run (a `:play` class; N4) |
| `SettingsKitTest` | 13/0 | 13/0 | 13/0 |
| `PlayThroughKitTest` | 4/0 | 4/0 | 4/0 |

Verifiers: `run_verifiers.py --project . --wo WO-009` is **GREEN**. 1 verifier, 150 lines. WO-009 added 0 verifiers and 0 lines, within budget.

trace-check (`PYTHONUTF8=1 ... --wo WO-009`): **RED**, expected. Scope is 10 IDs, 7 of 10 covered. The only FAILs are REQ-001.A2 and REQ-048.A1/A2, the three owner-evidence IDs ("owner evidence: 3 row(s), 0 signed"). req-lint: 0 errors. No governed-file FAIL. N3 says what "7 covered" means.

## Substance audit

For each in-scope token I asked whether it asserts the acceptance line's meaning, can fail, and is green or red for the right reason.

**REQ-039.A2** (`ReleaseMarksKitTest`). It reads the release APK's `app/tangramplaytime/content/puzzles/` folder: `index.txt` plus every other file there. A strict RFC 8259 reader takes `provenance.reviewedByHuman` and demands JSON `true`. A missing `provenance` or field reads false. A stale or missing APK fails. It can fail: the second test runs the same check over crafted texts (false, no provenance, no field, a decoy `reviewedByHuman` in `art`, a truncated file, two marked layouts) and over crafted folders (a named file missing from the zip, an unnamed unmarked file, no `index.txt`). It asserts the line's meaning. **Plainly: on this tree it is green over an empty release set.** `index.txt` names zero files, so no puzzle is released and none is unmarked. That is N3 of the code review, and the real verdict is TASK-071's, after the marks, with the marked count stated. It is not a finding against the test.

**REQ-042 A1..A3** (`ReleaseSetKitTest`). It loads the release APK's files through `PuzzleLoader.load`, then counts `reviewedByHuman` true (A1), distinct `category` values (A2) and the id `shapes-square` (A3). It can fail: 11 crafted-fixture legs each depart from a passing 20-marked bar by exactly one item. They cover 19 marked, 20 files with one unmarked (it counts true only), three categories, no square, a one-piece mini, no mini, three warm-ups and a rating-2 warm-up. The bar-passing control stays green. The red is honest: the shipped release is empty, so the three lines are red now. They go green honestly only with marks. The code review saw the release legs green with 20 marks in a scratch copy, and the fixtures show both directions. One precision for the record (N2): the red text names "0 file(s)" and the loader's `rule=EMPTY`, not each line's own count. It reads the same on all six legs. The count-bearing text (for example "A1: 5 of 5 ... at least 20") appears only once at least one puzzle is marked.

**REQ-002.A2** (`NoToddlerModeKitTest`, plus the settings half). The oracle is that every control on a player screen is one its REQs name, and every visible text is in the closed world. It walks the Play screen New, In progress, the solved view and the grid. It combines semantics actions, the router's `Control` set (exactly nine) and a text walk. It can fail. Two planted hosts are shown caught in the same class: an extra clickable "Toddler mode" text, and an untagged clickable icon box. In each, the checks report the planted node and nothing else. The settings half is `SettingsKitTest.noDifficultyControlIsPresent`. It asserts an exact control set at every scroll position, so a mode toggle would be one more control. The token is added to an existing test with no assertion change. That reading is sound for "no mode is offered by a control", which the REQ's rule 2 supports. It is a control-set oracle, not a text search for the word "toddler".

**REQ-048.A3.** Two tokens are added to existing tests, with no assertion change. `ReleasePromiseKitTest.theReleaseApkDeclaresNoPermission` reads the release manifest (binary XML) for any `uses-permission`, `uses-permission-sdk-23`, `permission`, `permission-group` or `permission-tree` element. It has liveness (the package and an activity) and an APK staleness check. `PlayThroughKitTest` reads the installed app's requested and declared permissions on the device (debug build, on three images). The release APK's installed view is the S-62 leg, which is evidence, not a token. On the floor it printed the emulator guard's PASS line and 0 entries in each of the four sections, and the control `com.android.shell` found 680 entries, so the parse can see a permission. V-01 and its control agree. The line is carried honestly.

**REQ-048.A4.** `LauncherIconKitTest` resolves the one launcher activity, takes its icon as a launcher does, requires an adaptive icon from the app's own package, draws both layers and compares 11 DI-9 points within 8/255. Its positive control (the platform default icon) must fail the same comparison. It was seen red before TASK-067 and green after, on three images. `StoreIconKitTest` holds the store PNG (512 x 512, 32-bit RGBA, every alpha 255, exact DI-9 colours at the mapped points) and the launcher resources (path for path and colour for colour, no monochrome layer), and checks "no text" over DI-9's SVG and the vector. Its crafted fixtures each fail their own check only: a stripe, an alpha of 254, a 24-bit file, an eighth path and an added `<text>`. N5 notes how "matches" and "no text" are carried.

**Pins with no token.** `StoreListingKitTest` has one test per self-check item plus crafted drafts. It checks the title, the short description (78 characters, with the promise words), the full-description limits, `PRIVACY.md` holding REQ-049 rule 1 in full, the console answers, and the banner at 1024 x 500 RGB. It carries no token for the three owner-evidence IDs (I grepped all `.kt` and `.py` files: no dotted REQ-001.A2, REQ-048.A1 or REQ-048.A2). It is not counted as a REQ-048 A1/A2 proof. No scaffolding test carries an in-scope token, and none is gaming. Every in-scope token sits on a test that asserts something.

**Corrected kit check.** One test defect is on record: recirculation 1, TASK-064's fixtures assumed the shipped mark, found by TASK-065's positive control. It was fixed at the TASK-066 landing and shown green in a 26-marked copy. No test defect arose in this pass, and no product defect.

## Findings

| ID | Sev | Observation | Class / proposed handling |
|---|---|---|---|
| N1 | Note | REQ-039.A2 is green over an empty release set (index lists 0 files). The check is non-vacuous in its crafted-text twin, but the tree gives it nothing to judge. | Not a defect. TASK-071 re-runs it after the marks and states the marked count it saw. |
| N2 | Note | The REQ-042 A1/A2/A3 reds name "the release set (0 file(s)) does not load ... rule=EMPTY", the same message on all six legs, not A1's marked count or A2's category count. | Accepted by the plan (S-55: an empty bundle fails at load). Count-bearing messages exist in the code and appear once any puzzle is marked. TASK-071 should record the marked count, the categories and the square from the green legs' INFO lines. |
| N3 | Note | trace-check's "7 of 10 covered" counts token presence, not green. REQ-042 A1..A3 and REQ-039.A2 are counted covered while red or vacuous. REQ-042.A2 and A3 also carry the older whole-library tokens in `PuzzleLibraryKitTest` (green over all 106), which would hide the red from a token-only reading. | A reading note for the auditor. The line is open until the marks. Not a trace defect. |
| N4 | Note | `NoToddlerModeKitTest` hosts the phone layout only and was not run on the tablet image. The plan has the tablet running app classes only (the run log's `t070-tablet` "the app set only"). The settings half did run on the tablet (13/0). No waiver is recorded, and none is needed under the plan's channel list. | Informational. The control sets are the same on the tablet layout by REQ-036, but the oracle did not walk the tablet's Play screen. |
| N5 | Note | "The icon contains no text" and "matches the store icon" are carried through the common source. The kit checks DI-9's SVG and the vector (paths only), the store PNG at DI-9's colour points, and the installed icon at 11 points. It does not scan the PNG's remaining pixels. The orchestrator's eyes opened `icon-512.png` (07:15) and the banner (07:30). | Sound under S-57's "one generator", with a positive control and crafted fixtures. Not a defect. |
| N6 | Note | The `adb uninstall` of an absent package prints `DELETE_FAILED_INTERNAL_ERROR` on these images. In `floor/s62.txt` it is an INFO line, not a check, and the leg still passes. It is already a proposed `decisions.md` reading. | None. |

No Blocker and no Should. Four items remain open for the owner, none of which a station can close:

- his marks (at least 20, four themes, the square, a three-piece mini, four warm-ups);
- the three signatures in `owner-evidence.md`;
- the TASK-071 re-run;
- the final `trace-check --wo WO-009` GREEN.

## What I ran

| Command | Result |
|---|---|
| `bash .../wo009/trees9.sh` (start, and again at the end) | `f80ed88 · 10 · test 598e72459933 · product 6550e479342c · 02feaef5d941 (106) · tools f1421752fb16`, equal both times, so t070 is the baseline |
| `python tools/validate_puzzles.py Tangrams`; `validate.py Requirements`; the four `tools/tests/` scripts; `tools/mark_puzzles.py status` | 106/106; 0 errors, 66 warnings; 14, 6/6, geom ok, 9; 0 of 106 marked, not ready |
| JVM run 1 then run 2 `--continue` (the two-run line, from `gradlew.bat`) | exit 0, then exit 1 (the expected red). XMLs fresh: 351 tests, 6 failures, 0 errors, 0 skipped; the six are `ReleaseSetKitTest` |
| `gradlew.bat -q :app:bundleRelease` | refused at `:content:checkReleasePuzzles`; no AAB on disk |
| `run_verifiers.py --project . --wo WO-009` | V-01 PASS 3/3; control 2/2; GREEN, within budget |
| `PYTHONUTF8=1 python .../trace_check.py --project . --wo WO-009` | RED; only the three owner-evidence IDs; 7 of 10 covered |
| my own XML counter (`tv/cnt.py`) over the t070 copies: `jvm/xml`, `floor/xml`, `dev/xml`, `tablet/xml`; reading `floor/s62.txt`, `v01/v01.txt` and the `ProcessDeathKitTest` messages | the counts above |
| Read-only: WO, tasks.md § WO-009, run log, REQ files, `owner-evidence.md`, both code reviews, and the kit files named in the brief | substance audit |

I did not run any emulator, device set, `mark` or `unmark`, or signing. I edited no code, test, ledger, decision, WO, `owner-evidence.md` or run-log file. I only built, which changes untracked `build/` output. The tree hashes matched again afterwards. Scratch: `C:/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/b3148999-a4ac-4936-a274-abc67af518f4/scratchpad/wo009/tv/`.

## Handoff

- **Verdict: forward** (to the Traceability Auditor). B 0 / S 0 / N 6.
- **Counts:** JVM 351 tests, 6 red, exactly `ReleaseSetKitTest`'s release legs (the expected red). Device: floor 162 / dev 165 / tablet 60, with only the guarded `ProcessDeathKitTest` phases (2 per image) not passing, no skips. V-01 3/3 and control 2/2. Python green.
- **Per ID:** pass: REQ-002.A2, REQ-048.A3, REQ-048.A4, REQ-039.A2 (vacuous, N1). Expected red: REQ-042 A1..A3. Owner evidence awaiting: REQ-001.A2, REQ-048.A1, REQ-048.A2.
- **Coverage gaps:** none in tokens or channels beyond the above. The tablet does not walk the Play-screen oracle (N4).
- **trace-check:** RED (WO-009 scope), only the three owner-evidence IDs awaiting signature; 7 of 10 covered; req-lint 0 errors.
- **Verifiers:** GREEN; WO-009 added 0 verifiers and 0 lines, within budget (1 verifier, 150 lines).
- **Tree:** `f80ed88 · 10 · test 598e72459933 · product 6550e479342c · 02feaef5d941 (106) · tools f1421752fb16`.
