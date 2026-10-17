# Test & Verify — WO-004 #Browsing + `:store`

**Date:** 2026-10-17 08:40-09:15 (clock `date`)  ·  **Station:** test-verify (Sonnet 5.5)  ·  **Mode:** Full (c)
**Owner instruction for this WO:** every set on both images from the final tree; no earlier device run used as baseline. Followed: nothing below is reused from the run log.

## 1. Hashes
| | Start (08:40) | End (09:15) | Brief |
|---|---|---|---|
| HEAD | 19806a8 | 19806a8 | 19806a8 |
| status lines | 67 | 67 | (r2: 66) |
| test tree | 9147ec60ee21 | 9147ec60ee21 | 9147ec60ee21 |
| product tree | 3ff3d2463ae6 | 3ff3d2463ae6 | 3ff3d2463ae6 |
| `tools/process_death_check.sh` | ec454985c025 | ec454985c025 | ec454985c025 |

Recipe: the run-log header's (`find <dirs> -type f | sort | xargs sha1sum | sha1sum`, first 12). The status count is one above r2's 66; neither tree hash moved, so the extra line is outside both trees (not investigated further). Nothing was edited by me.

## 2. Run rows
Scratchpad `S` = `C:/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/cec4023e-06ef-466b-bf4c-142aad5e0760/scratchpad`. Every device step had reset + `--check` clean before and after (my scripts `S/dev.sh`, logs `S/dev.log`, `S/floor.log`).

| # | Time | Command | Result | XML |
|---|---|---|---|---|
| t1 | 08:40:14-26 | `:store:testDebugUnitTest :play:testDebugUnitTest :content:testDebugUnitTest :contracts:test --rerun-tasks -q` | exit 0 | `S/jvm/` (21 files) |
| t2 | 08:40:43-08:56:37 | dev `Medium_Phone_API_37.0` (emulator-5554, sdk 37): `:play:connectedDebugAndroidTest --rerun -q`, whole set | exit 0, 79 / 0 / 0 / 0 (05:56:34 UTC) | `S/dev/play.xml` |
| t3 | 08:56:38-54 | dev `:app:connectedDebugAndroidTest --rerun -q`, whole set | exit 0, 7 testcases, 2 `<failure>` = the guarded skips (section 3) | `S/dev/app.xml` |
| t4 | 08:56:55-08:57:20 | dev: forced Gradle line (`:app:assembleDebug :app:assembleDebugAndroidTest :app:packageDebug --rerun :app:packageDebugAndroidTest --rerun -q`, exit 0), then `process_death_check.sh`, `--positive-control`, `--negative-run` | main PASS (exit 0), control PASS (exit 0), negative FAIL (exit 1); APKs 08:56:56 | `S/dev.log` |
| t5 | 08:57:5x | `adb emu kill`, `adb devices` empty, boot `Phone_API_26` headless; `adb devices` emulator-5554; sdk 26 | booted | `S/emu26.log` |
| t6 | 08:58:05-09:12:31 | floor `:play:connectedDebugAndroidTest --rerun -q`, whole set | exit 0, 79 / 0 / 0 / 0 (06:12:29 UTC), file `TEST-Phone_API_26(AVD) - 8.0.0.xml` | `S/floor/play.xml` |
| t7 | 09:12:32-41 | floor `:app:connectedDebugAndroidTest --rerun -q` | **voided**: `INSTALL_FAILED_ALREADY_EXISTS` at the APK install, Gradle still exit 0, no XML written (section 6) | none |
| t8 | 09:12:41-09:13:07 | floor: forced Gradle line (exit 0), death main PASS (0), control PASS (0), negative FAIL (1); APKs 09:12:42-43 | as the dev row | `S/floor.log` |
| t9 | 09:13:36-50 | floor, after `adb uninstall` of both packages, reset + `--check` clean: `:app:connectedDebugAndroidTest --rerun -q` again | exit 0, 7 testcases, same 2 guarded skips (06:13:47 UTC) | `S/floor/app.xml` |
| t10 | 09:14:28 | floor killed, `adb devices` empty; `Medium_Phone_API_37.0` booted headless, sdk 37, **left running** | emulator-5554 device | `S/emu37.log` |
| t11 | 09:14:32-57 | `:app:assembleRelease :app:assembleDebug --rerun-tasks -q` (APKs 09:14); `PYTHONUTF8=1 run_verifiers.py --project . --wo WO-004` | exit 0; see section 5 | stdout |

Order note: on the floor image the death trio (t8) ran before the app-set retry (t9); the two do not depend on each other (each is preceded by reset, and the death script resets last).

## 3. Test counts per channel
- **JVM (t1):** store 12 (FileProgressStoreKit 10, StoreScaffold 1, WrittenShapeScaffold 1) · play 79 (BrowsingKit 12, ResumeKit 13, PlayControllerKit 16, TrayMetricsKit 6, TwistKit 6, PlayScaffold 1, scaffolding 25) · content 64 · contracts 2. 0 failures, 0 errors, 0 skipped.
- **Dev play set (t2):** 79 / 0 / 0 / 0 — BoardTray 7, CornerPulse 4, Decoration 12, Drag 20, Overview 6, Preview 2, ResumeScreen 5, SolvedPicture 6, SolvedView 5, TopBar 12.
- **Floor play set (t6):** 79 / 0 / 0 / 0, same ten classes, same counts.
- **Dev app set (t3) and floor app set (t9):** 7 testcases each — AppLaunch 2, AppWiring 2, FloorChannel 1, ProcessDeath 2. Five pass. The two `<failure>` entries are the expected guarded skips, not verdicts: `app.xml` **line 28** `ProcessDeathKitTest#phase1` ("phase1 runs only with -e deathPhase 1 (given: null)") and **line 62** `#phase2` ("phase2 runs only with -e deathPhase 2 (given: null)"), both `AssumptionViolatedException`, identical on both images. No other `<failure>` or `<error>` in any XML.
- **Death channel (t4, t8):** both images — main PASS (phase 1, force-stop, phase 2 in a new process, both status 0); control PASS (phase 2 over a cleared store ran and failed: `the puzzle shown expected:<shapes-warmup-1> but was:<shapes-mini-1>`); negative-run FAIL exit 1 (phase 2 not run, `-4`, phase 1 passed). Every verdict line names both APK times (dev 08:56:56; floor 09:12:42-43), and the stale-APK guard accepted the forced build.

## 4. Per acceptance ID by channel
P = pass. "death" = the process-death pair (main PASS on both images). `-` = not applicable per `tasks.md`. JVM tests are in `play/src/test`, device tests in `play/src/androidTest` / `app/src/androidTest`.

| ID | JVM | dev | floor | death | Tests (token lines) |
|---|---|---|---|---|---|
| REQ-003.A1 | P | P | P | - | BrowsingKit; OverviewKit |
| REQ-003.A2 | P | P | P | P | ResumeKit; ResumeScreenKit; ProcessDeathKit#phase2 |
| REQ-024.A1 | P | P | P | - | BrowsingKit; TopBarKit |
| REQ-024.A2 | - | P | P | - | OverviewKit |
| REQ-024.A3 | P | P | P | - | BrowsingKit; TopBarKit |
| REQ-025.A1 | P | P | P | P | ResumeKit; ResumeScreenKit; ProcessDeathKit#phase2 |
| REQ-025.A2 | P | P | P | - | ResumeKit; ResumeScreenKit |
| REQ-025.A3 | P | P | P | - | ResumeKit; ResumeScreenKit |
| REQ-026.A1 | P | P | P | - | ResumeKit; SolvedViewKit |
| REQ-026.A2 | P | P | P | - | ResumeKit; SolvedViewKit |
| REQ-050.A1 | P | P | P | - | BrowsingKit; TopBarKit |
| REQ-050.A2 | - | P | P | - | OverviewKit |
| REQ-050.A3 | P | P | P | - | BrowsingKit; OverviewKit |
| REQ-040.A1 | P | P | P (AppLaunch) | - | BrowsingKit (+ `content/` PuzzleLibraryKit, kept from WO-002) |
| REQ-041.A1 | P | P | P | - | BrowsingKit; TopBarKit (+ `content/` token) |
| REQ-045.A1 | P | P | P | - | BrowsingKit; AppLaunchKit; BoardTrayKit (WO-003) (+ `content/` token) |

All 16 in-scope IDs carry a token on every channel `tasks.md` assigns them, and every test class that carries a token passed on every channel that runs it. REQ-003.A2 and REQ-025.A1's process-death leg is judged by the death script's verdict lines alone (main PASS, control PASS, negative FAIL on both images). Token grep by line: `play/src/test` BrowsingKit and ResumeKit; `play/src/androidTest` TopBar, ResumeScreen, SolvedView, Overview; `app/src/androidTest` AppLaunch (REQ-045.A1 line 41, REQ-040.A1 line 62) and ProcessDeathKit line 107 (`phase2`). Nothing in `KitStore.kt`, `KitScreen.kt`, `AppWiringKitTest.kt` or the store kit carries a token, as planned.

## 5. Verifiers
- V-01 PASS 3/3 on `app/build/outputs/apk/release/app-release-unsigned.apk` (4.0 s), rebuilt at 09:14 before the runner.
- Positive control PASS 2/2 on `app/build/outputs/apk/debug/app-debug.apk` (4.7 s).
- Budget line for WO Metrics: `verifiers: WO-004 added 0 verifier(s), 0 lines — budget {"count": 1, "lines": 150} — within budget`; `verifiers: GREEN — 1 verifier(s), 150 lines total, 0 failure(s) (manifest mode)`.
- Harness (not a verifier): `tools/process_death_check.sh` 107 lines; its three modes ran as above.

## 6. Substance re-check
The code review's per-token audit (all 16 substantive; two mutations caught) was re-checked, not redone. I re-read: `BrowsingKitTest` REQ-003.A1 (every start x every target, with seeded New / In progress / Solved states, states compared after each move, plus › and ‹ walks that must reach every other puzzle once), REQ-050.A1 (the discriminating plain-› control opens 4, the long press opens 7, both a New and an In-progress 7), REQ-050.A3 (every cell in turn, every other puzzle's state and saved progress compared after a flush); `ProcessDeathKitTest` phase 1 (places three pieces through S-2 and flushes) and phase 2 (the same three pieces in the shown nodes by key and box, and in the controller's board). All assert the meaning of their ID; the control's failure message proves phase 2 can fail for the stated reason. Not independently re-mutated by me (the review did, with sha1 restore). No assertion-free token test found. Code review N4 (device REQ-041.A1 asserts "warm-up", not rating 1) stands as the review triaged it; `BrowsingKitTest` and the `content/` token cover the rating.

## 7. Failures and gaps
- **No test failed on any channel.** No product defect, no test defect, no corrected check from this run.
- **One voided step, environment (t7):** the first floor app-set run died at the APK install with `INSTALL_FAILED_ALREADY_EXISTS` and no XML. Gradle printed the error but exited 0 (`-q`), so an exit code alone would have read this as a pass; I caught it from the missing XML. The `--check` before it read clean (data directory empty), so the package was installed with empty data. After `adb uninstall` of `app.tangramplaytime` and `.test` and a clean `--check`, the retry (t9) installed and ran. This is not a tree defect and not a kit result; it is recorded as run-environment noise for the orchestrator (the floor play run immediately before it ended with the app installed, which the dev image did not show).
- **Coverage gaps:** none for a named channel. Human eyes is not a DoD channel (owner plays at close). The unverified lines: none by design (`tasks.md`).
- Carried, not mine: code review S1 (accepted as a G4 risk, carried to WO-008).

## Verdict
**GREEN, forward.** Every channel the WO names ran from the final tree on both images: JVM 12 + 79 + 64 + 2 all green; dev play 79/0 and app 5 pass + 2 expected guarded skips; floor play 79/0 and app 5 pass + 2 expected guarded skips (after one voided install retry); death pair main/control/negative as required on both images; V-01 3/3 with positive control 2/2; run_verifiers GREEN. 0 Blockers, 0 failures. Both tree hashes and the death script hash equal the start.

## Handoff
- **Scope:** WO-004 verification, 16 acceptance IDs, all channels, both images.
- **Inputs read:** run log, briefs lines 1-61, WO-004 Scope / Constraints / Checkpoint items / Log of 2026-10-17, `tasks.md` § WO-004, the two `decisions.md` death-check rows, the code review, the eight REQ acceptance lines, the kit tests named in section 6, `reviews/WO-003-test-verify.md` for the shape. (`platform-notes/android.md` was not opened; the channel facts came from the briefs table and the run log header.)
- **Result:** `C:\GitHub\AI\TangramPlaytime\reviews\WO-004-test-verify.md`; XMLs under `S/jvm`, `S/dev`, `S/floor`.
- **Status:** forward.
- **Device state:** `Medium_Phone_API_37.0` running (emulator-5554, sdk 37), app reset, `--check` clean. Floor stopped.
- **Metrics inputs:** corrected checks 0 new; disputes 0; verifier budget 0 added (1 / 150 total); harness 107 lines; dev play run 15 m 54 s, floor play run 14 m 26 s.
