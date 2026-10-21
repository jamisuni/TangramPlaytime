# WO-008 test and verify (#PlayTime: REQ-005.A1-A2, 029.A1, 030.A1-A3, 031.A1-A2, 006.A1)

Station: test-verify, 2026-10-21, 08:41 to 08:45 (local). Scratch: `C:/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/60f0448e-2054-4150-b342-611b30780569/scratchpad/wo008/tv008/` (`sum.py`, `jvm.py`, `cls.py`, `jvm-run1.txt`, `jvm-run2.txt`). No emulator was used by me.

**Verdict: GREEN, forward** on the test and verify side. Every channel the WO names has a run on the final tree (the cheap ones re-run by me, the device ones from the run log on matching hashes), 9 of 9 in-scope IDs are asserted by tests that can fail, and the only failures anywhere are the two guarded `ProcessDeathKitTest` phases per app set (`AssumptionViolatedException … deathPhase`). **One item is routed to the orchestrator, not to a station that edits tests or product:** `trace_check.py --wo WO-008` is RED (see "Routed item"); it is a defect of the WO text, not of a test or the product.

## Tree hashes (`scratchpad/trees.sh` from the project root)

| | at my start (08:41:39) | at hand-back (08:43:59, before this file) | run log t051 |
|---|---|---|---|
| HEAD / status lines | 5706854 / 57 | 5706854 / 57 | 5706854 / 57 |
| test tree | 23bbfc029580 | 23bbfc029580 | 23bbfc029580 |
| product tree | c7599c4acf30 | c7599c4acf30 | c7599c4acf30 |
| bundle (count) | 02feaef5d941 (106) | 02feaef5d941 (106) | 02feaef5d941 (106) |
| tools tree | 0a7a3a785c3b | 0a7a3a785c3b | 0a7a3a785c3b |

Equal at all three points. My gradle runs rebuilt `build/` outputs only. This file is the only repo change of mine (the status count becomes 58). `adb devices`: empty at the end (I booted none).

## Baseline taken, and why

The hashes match, so t051-floor, t051-dev and t051-tablet are the baseline for the device channels; I re-ran no device class. I opened every XML myself (`tv008/sum.py`: counts `testcase` elements and their `failure` / `error` / `skipped` children; `tv008/cls.py`: per class, per image).

| Channel (row) | Run | XML stamps (local) | My sums (tests / fail / error / skip) | What the failures are |
|---|---|---|---|---|
| Floor `Phone_API_31`, play set (t051-floor) | 05:48 to 06:58 | `play-set.xml` 06:36:50 | **100 / 0 / 0 / 0** | none |
| Floor, app, `DevPromptKeyboardKitTest` alone | same script | `app-DevPromptKeyboard.xml` 06:37:29 | **1 / 0 / 0 / 0** | none |
| Floor, app rest | same | `app-rest.xml` 06:58:03 | **36 / 2 / 0 / 0** (floor app = 37) | `ProcessDeathKitTest` `phase1`, `phase2`: both `AssumptionViolatedException: phaseN runs only with -e deathPhase N (given: null)` |
| Dev `Medium_Phone_API_37.0`, play set (t051-dev) | 06:59 to 08:17 | `play-set.xml` 07:55:13 | **100 / 0 / 0 / 0** | none |
| Dev, app `DevPromptKeyboard` / rest | same | 07:55:53 / 08:16:14 | **1 / 0 / 0 / 0** and **39 / 2 / 0 / 0** (dev app = 40) | the same two guarded phases |
| Tablet `Medium_Tablet_API_37.0`, app (t051-tablet) | 08:17 to 08:38 | 08:19:06 / 08:38:12 | **1 / 0 / 0 / 0** and **39 / 2 / 0 / 0** (tablet app = 40) | the same two guarded phases |

- Every XML is stamped after its script started, and none is stamped after the 08:40 re-count; no skipped element anywhere. Floor `LanguageKitTest` has 1 case (its three `@SdkSuppress(33)` legs are filtered, absent from the XML), dev and tablet have 4: as the channel table says.
- Death pair, read from the logs: floor `[main] PASS - phase 1, a force-stop, then phase 2 in a new process: both passed [APKs … 2026-10-21 06:58:11]` and `[--positive-control] CONTROL PASS … (exit 0)`; dev `PASS` and `CONTROL PASS` with APKs 08:16:24, both exit 0. The control's phase 2 fails with `ComparisonFailure … the puzzle shown expected:<shapes-warmup-1> but was:<shapes-mini-1>`, as it must.
- Hygiene: `check-final.txt` of each image clean, `emu.txt` kill lines, no crash window (as the run log says; I did not re-read all 18 reset files).
- The tablet runs no death pair and no play set: the channel table says floor and dev for the pair, and "the app set only" for the tablet.

## My own runs on the final tree

| Channel | Command (Git Bash, `PYTHONUTF8=1`, JAVA_HOME the Studio jbr) | Result |
|---|---|---|
| Python | `python tools/validate_puzzles.py Tangrams`; `python C:/GitHub/AI/SwReqCollector/tools/validate.py Requirements` | "106/106 puzzle files valid"; "OK - 0 errors, 66 warning(s)" |
| JVM run 1 | `cmd.exe //c ".\gradlew.bat -q :app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun"`, started 08:42:09 | exit 0 |
| JVM run 2 | `… :content:testDebugUnitTest --rerun :play:testDebugUnitTest --rerun :kernel:test --rerun :contracts:test --rerun :store:testDebugUnitTest --rerun :app:testDebugUnitTest --rerun`, started 08:42:22 | exit 0; **295 / 0 failures / 0 skipped**, 40 XMLs, **0 stale** (every XML stamped after 08:42:22). Per module from the XML names: app 4, content 80, contracts 2, kernel 65 (`ActiveTimeKitTest` 9), play 132 (`PlayTimeKitTest` 18, `TimeFormatKitTest` 5), store 12: equals the baseline t051-host |
| V-01 | `python .swdev/verifiers/v01_release_apk.py` | **PASS 3/3** on `app-release-unsigned.apk` (0 okhttp3 classes, 0 `Ljava/net/` references from 405 product classes, the debug canary in none of 2 dex files) |
| V-01 positive control | `… --positive-control` | **PASS 2/2** on `app-debug.apk` (canary seen in classes5.dex; 78 `Ljava/net/` references seen outside product code) |
| Verifiers | `python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-008` | **GREEN**: V-01 PASS, +pc PASS; "WO-008 added 0 verifier(s), 0 lines - budget {count 1, lines 150} - within budget" (the budget line for the Metrics) |
| Trace-check | `python C:/GitHub/AI/SWDev/framework/skills/trace-check/trace_check.py --project . --wo WO-008` | **RED**: 9 of 10 scope IDs covered; `FAIL: REQ-029 (Locked): uncovered acceptance A2`. See "Routed item" |

## Substance audit, per in-scope ID

"Mutation" names the change that turns the test red. Channels: J = JVM (`kernel/`, `play/`), F / D / T = floor / dev / tablet device sets (all from the baseline, all passing). Kits are in the tree at the hashes above.

| ID | Token on (file:line) | What it asserts | Can it fail? | Channels (all pass) |
|---|---|---|---|---|
| REQ-029.A1 | `kernel/src/test/.../ActiveTimeKitTest.kt:97`; `app/src/androidTest/.../ActiveTimeAppKitTest.kt:74` | Kernel: a touch + 30 s counts exactly 30 (control), a touch + 5 untouched minutes counts **exactly 60**, then the same 5 minutes folded every 10 s sums to 60, 5 more minutes add 0; play times `PlayTimes(150, day, 150)`. App: the same through a real window touch on `AppGraph`'s `ActiveTime` over `AppClock`, +30 then exactly +60 | Yes. Mutation: `IDLE_LIMIT_MS` 60 s to 90 s (60 becomes 90), to 30 s, or removed (300); a count that counts a touch-less idle; a touch never reaching the kernel (app leg reads 0, the exact 60 and the 30 control both fail). Exact values, not "at most" | J, F, D, T |
| REQ-005.A1 | `play/src/test/.../PlayTimeKitTest.kt:139` | A mini puzzle: 10 s while New leave time 0, 45 s In progress, solve: controller `timeSeconds` = 45 and the store's `timeSeconds` = 45; 30 s more on the solved view leave 45; 45 again after ›, ‹ and a restart | Yes. Mutation: time not saved on the solving drop (store 0), the fold missing before the solving drop (last seconds lost, 45 to 20), time counted while New (55), time counted on the solved view (75). The companion pin `theSolvingDropFoldsFirst…` (35 s) catches the fold alone | J |
| REQ-005.A2 | `PlayTimeKitTest.kt:182`; `app/src/androidTest/.../ProcessDeathKitTest.kt:137` (extra token on `phase2`) | JVM: all-time play time 0, then 20 / 45 / 75 / 115 on the board (New, In progress), solved view and grid, 115 after a restart and 125 after 10 s more. Death pair: phase 1 counts 60 s through a window touch and the kit clock, phase 2 in a new process reads `timeSeconds` = 60 and all-time = 60 | Yes. Mutation: play time dropped on a screen (solved view, grid, New) or not persisted/restored (0 after restart, 0 after process death). The death-pair legs are only proven by the pair (the ordinary run skips both phases): floor and dev `PASS`. See the observation on the control | J, death pair F, D |
| REQ-030.A1 | `PlayTimeKitTest.kt:226` | A In progress at 7 s, 30 s on A (37), › to B: 2 minutes of play, the ticker folding; B gets 123 (control) and all-time grows 120 (control); back on A, **37** in the controller and the store, then +10 resumes (47) | Yes. Mutation: no fold before the page change (A credited 120 s), a fold crediting the previous puzzle after the change (A 157), time not paused while away. Pin `twoMinutesOnAnotherPuzzle…` (no tick at all across the changes) catches the fold-first mutation separately | J |
| REQ-030.A2 | `PlayTimeKitTest.kt:259` | Solves of 40 s, 70 s, 25 s on one puzzle with Retry between: best 40, then **40** after the slower, then 25; each solve's own time stored beside the best | Yes. Fixture discriminates: "keep the last" gives 70 at the second step, "keep the first" gives 40 at the third, "never set" leaves null | J |
| REQ-030.A3 | `PlayTimeKitTest.kt:292` | A New puzzle with three tray turns and the parallelogram's tray mirror, 10 s of play after each: state NEW, `timeSeconds` 0; the 40 s are play time (control); the same moves In progress add exactly 40 | Yes. Mutation: a tray turn or mirror moves the puzzle to In progress, or time counted while New (40 instead of 0). Controls prove the counting works | J |
| REQ-031.A1 | `play/src/androidTest/.../TimerKitTest.kt:153`; `ActiveTimeAppKitTest.kt:151` | Play: over a fresh store `timerShown` is false and the `timer` node is absent on a New puzzle, In progress at 30 s, at the solve, on the solved view with the picture shown; liveness: 30 s are really counted. App: a cleared app, same absences, with the board shown and `timeSeconds` +60 after a window touch | Yes. Mutation: a timer drawn by default (node present), `timerShown` defaulting true. Positive control is REQ-031.A2's test with the same tag (node found when the setting is on); the app leg adds its own liveness | F, D (play, app); T (app leg) |
| REQ-031.A2 | `TimerKitTest.kt:184` | Setting on: "0:00" on a New puzzle and after 10 s there (rule 4), "0:30" In progress, "1:30" after a touch + 5 untouched minutes with the ticker (exact, so wall-clock 5:30 and stopped 0:30 both fail), "1:30" with the grid open and 20 s, "1:40" after, another New puzzle "0:00", back "1:40", node gone at the solve and on the solved view | Yes. The text is read from the node and tied to the controller's `timeSeconds` each time. The place (14 dp in, inert node) is a separate untokened pin `theTimerSitsInTheBoardsTopRightCornerAndIsNoControl`, which the baseline also passes | F, D |
| REQ-006.A1 | `app/src/androidTest/.../PlayThroughKitTest.kt:59, 69, 75` | A play-through by touch of the first mini and a full puzzle on a 360 x 780 dp phone in portrait, a 1280 x 800 dp landscape tablet and an 800 x 1280 dp portrait tablet (display overrides read back), with the WO-006 no-money / no-network lines; re-covered here on the API 31 floor | Yes (WO-006's tokened play-through, unchanged logic; the mutation is a screen that cannot be solved by touch). Each leg passes on all three images; the 800 x 1280 leg is the one the kit fix of 2026-10-21 00:37 repaired (floor 4/0 in the baseline) | F, D, T |

REQ-029.A2 (settings screen) and REQ-030's "settings list" are WO-007's by `decisions.md` 2026-10-20 row 8; no token is claimed for them. `grep` of every `REQ-005|029|030|031.A<n>` and `REQ-006.A1` token in the test trees found the seven test methods above plus the `phase2` extra token; no token on a scaffolding test or an empty body, no `@Ignore` (the only `assumeTrue` uses are the death-pair guard and one pre-existing `DragKitTest` pin).

Untokened pins I read that matter to the must-dos (all in `PlayTimeKitTest`, passing): the aid solve keeps the best time as it was (`:571`, with a fixture where the naive rule stores 0) and `solveWithoutBest` withholds only while its drive runs (`:615`); no negative time reaches the store across a backwards clock and a date set back (`:701`, exact values); the write cadence of 10 counted seconds and at `setVisible(false)` (`:648`); a resumed In-progress puzzle counts from its first fold (`:327`). Kernel pins: backwards readings, idle stays idle across a jump, whole-second remainders, nothing counts while not visible, midnight and a date set back, "today reads 0 on another date", no count before the first touch (`ActiveTimeKitTest.kt:141-340`).

## Channel gaps, test defects, product defects

- **Channel gaps: none.** Python, JVM two-run line, floor first, dev, tablet, V-01 + control, the death pair on floor and dev: each has a run on the final tree. Human eyes (the timer once WO-007 adds the switch, the best time after a solve, "1 h 5 min") is recorded as not a DoD channel (WO-008 § Verification channels); it stays on the owner's close list.
- **Test defects: none found.** The 17 corrected checks of earlier WOs are not repeated here; the three of this WO (`ResumeKitTest` `kitExpect`, `KitApp` closed world, the "60:00" to "1 h 0 min" pin, plus the `KitApp` 800 x 1280 dp fix) are the orchestrator's, filed at close.
- **Product defects: none found.** No test is red outside the two guarded phases.
- **Skips:** none other than the guarded phases (0 `skipped` elements in all 8 device XMLs and 40 JVM XMLs).

## Routed item

**R1. `trace_check.py --wo WO-008` is RED** (`FAIL: REQ-029 (Locked): uncovered acceptance A2`). Cause, reproduced with the tool's own regexes (`scope_ids`, `SCOPE_BARE`): the `workorders/WO-008.md` Implements bullet (§ Scope, line 20) names the code homes with bare IDs inside parentheses ("`kernel/` (`#ActiveTime` row — REQ-029)", likewise REQ-005, REQ-030, REQ-031, REQ-006). A bare `REQ-NNN` means "all acceptances", so REQ-029.A2 (WO-007's) enters the scope as the tenth ID. The Scope notes already say the bullet is read only by that parser. Route: **orchestrator**, edit the WO's Implements bullet (write the code homes without a bare REQ id, or as `REQ-029.A1`), then re-run trace-check before the Traceability Auditor and the close. It is not a coverage gap in the tests; no test or product change is wanted, and I made none.

## Observations

1. The death-pair positive control (`--positive-control`, a cleared store) fails in phase 2 at "the puzzle shown" (`expected:<shapes-warmup-1> but was:<shapes-mini-1>`), which comes before the new REQ-005.A2 time assertions. So the control proves phase 2 runs and can fail, not that the 60 s read-back specifically can. The read-back is a plain `assertEquals` on `timeSeconds` and all-time (`ProcessDeathKitTest.kt` phase 2), and the JVM kit covers the restart leg of the same line; I judge it adequate, not a defect.
2. REQ-005.A2's death-pair leg and the ordinary connected run: an ordinary run reports `phase2` as skipped by the guard; only the pair's `PASS` line proves the token. This is the pattern WO-004 and WO-006 already use.
3. `TimerKitTest` runs on floor and dev only (the play set hosts `PlayScreen` at the kit's phone size, and the tablet runs the app set only, by the channel table). The tablet has the app leg of REQ-031.A1 and REQ-029.A1.
4. The play-through's `theInstalledAppDeclaresNoPermissions…` (REQ-010.A2) passes on all three images, and V-01 passes with 0 network references in 405 product classes: the owner's no-ads, no-analytics, no-network, no-permissions rule is not contradicted by anything I read.
5. Per-class times in the baseline are within the plan's range (floor play 2848 s, dev 3303 s); the orchestrator's note that each class stayed within +/-5 % of TASK-043's alone-time is the run log's, not re-measured by me.
