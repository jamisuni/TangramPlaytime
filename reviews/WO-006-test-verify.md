# WO-006 test and verify (#Layout + #Language + #Promise: REQ-006.A1, 035.A1-A2, 036.A1-A2, 037.A1, 047.A1-A2, 001.A1, 008.A1-A2, 010.A1-A2)

Station: test-verify, 2026-10-18, 18:42 to 18:53 (local). Scratch and copied XMLs: `C:/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/b74880cd-17d0-4f1f-b364-f13531532087/scratchpad/tv006/`.

**Verdict: GREEN, forward.** Every named channel has a run on the final trees. 0 failures other than the 2 allowed `ProcessDeathKitTest` skips per app set. Substance sound. No product defect, no test defect found.

## Tree hashes (recipe of the run log header; recomputed by me at start 18:42 and at hand-back 18:53)

| | start | hand-back |
|---|---|---|
| HEAD / status lines | a797553 / 42 | a797553 / 42 |
| test tree | bf79f0e02074 | bf79f0e02074 |
| product tree | 83abecd0d723 | 83abecd0d723 |
| device-kit subtree (`app/src/androidTest` + `play/src/androidTest` + `tools/device_reset.sh`) | 4ce17d58658a | 4ce17d58658a |

These equal row k27b (JVM line: test bf79f0e02074, product 83abecd0d723) and row i32 (device: product 83abecd0d723; test tree 55d463a6b587 differs from k27b only by the one JVM file, the device-kit subtree is the 4ce17d58658a the orchestrator named). Device kit and product are unchanged since i32.

## Baseline taken, and why

- **k27b** for the JVM line (play 105/0, kernel 56/0, app 4/0, contracts 2/0): trees match. I opened the XMLs (`tv006/jvm-base/`, stamped 18:38:57 to 18:39:04, after run 2 started 18:36:3x) and summed the per-class counts myself. The APKs from run 1 (18:36:33) are the ones V-01 read.
- **i32** for the device channels on the dev, tablet and floor images: product and device-kit subtree match. I read the XMLs in `task032/xml/` (stamped after each command; all product files last written 16:15, every whole set after):
  - Dev app set: Keyboard 1/0; rest 33/2 (AppLaunch 2, AppWiring 2, DevAidApp 2, FloorChannel 1, Language 4, PhoneLayout 6, PlayThrough 4, ProcessDeath 2 skips, TabletLayout 7, TouchTarget 3). Dev play set 94/0 (11 classes).
  - Tablet app set: Keyboard 1/0; rest 33/2, the same counts; Language ordinary 4/0.
  - Floor app set: Keyboard 1/0; rest 30/2 (Language 1: the three `@SdkSuppress(33)` legs are absent, not skipped). Floor play set 94/0. Floor steps (a) `fi-FI` 1/0, (b) `sv-SE` 1/0, (c) `sv-SE,fi-FI` 1/0 plus the direct `am instrument` run (`task032/logs/f26-c-direct.log`, `INSTRUMENTATION_CODE: -1`, 16:29:12), (d) restore read back `en-US`.
  - The only `<failure>` elements are the two `AssumptionViolatedException` phases of `ProcessDeathKitTest` (grepped on the floor XML).

## What I ran

| Item | Command / start | Result | XML or log |
|---|---|---|---|
| JVM close addition | `cmd.exe //c ".\gradlew.bat -q :store:testDebugUnitTest --rerun :content:testDebugUnitTest --rerun"`, start 18:43:25, exit 0 | store 12/0 (FileProgressStoreKit 10, StoreScaffold 1, WrittenShapeScaffold 1); content 64/0 (PuzzleLibraryKit 16, PuzzleLoaderKit 37, ContentScaffold 2, ParserScaffold 9); 0 skipped. XMLs stamped 18:43:27 to 18:43:28 | `tv006/jvm/` |
| V-01 | `python .swdev/verifiers/v01_release_apk.py` | PASS 3/3 on the 18:36:33 release APK (no uses-permission; canary in none of 2 dex; 0 okhttp3, 0 `Ljava/net/` from 393 product classes) | console |
| V-01 positive control | `--positive-control` | PASS 2/2 (canary in `classes5.dex`; 78 `Ljava/net/` outside product code) | console |
| Runner | `PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project . --wo WO-006` | **GREEN**: 1 verifier, 150 lines, 0 failures. **Budget line: WO-006 added 0 verifier(s), 0 lines, budget {count 1, lines 150}, within budget.** | console |
| trace-check | `PYTHONUTF8=1 trace_check.py --project . --wo WO-006` | GREEN, 13 of 13 in-scope IDs; req-lint 0 errors; 28 outbox drafts awaiting capture | console |
| Floor step (e), run 2 (counts) | floor booted 18:49:31; reset + `--check` clean; root airplane route on at 18:49:57 (read back `airplane_mode_on=1`, wifi disabled, "Active default network: none"); `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.app.PlayThroughKitTest -Pandroid.testInstrumentationRunnerArguments.kitAirplaneByChannel=true`, Gradle start 18:50:05, exit 0 | PlayThroughKitTest **4/0/0/0 skipped**, suite timestamp 15:51:49Z (18:51:49 local, after the start), mtime 18:51:53. Read back after the run, still on: `airplane_mode_on=1`, "Active default network: none". Route off at 18:51:53 (read back 0, network up), `adb unroot` (uid 2000), reset, `--check` clean, `mCurrentFocus` = the launcher | `tv006/xml/f26-e-airplane/`, `tv006/f26-e.sequence-run2.txt` |

### Step (e), the interference, and my ruling

Run 1 of step (e) (Gradle start 18:45:22, XML 4/0, suite 15:47:05Z) was disturbed. The orchestrator told me the TASK-032 implementer, re-woken, ran `bash tools/device_reset.sh` once on my floor at about 18:47:05, and that my after-run read-back failed with "no devices/emulators found" (it did). Order of events cannot be fixed to the second, and a root reset on API 26 can switch airplane mode off. The kit asserts airplane mode still on after the last step (PlayThroughKitTest line 165) and passed, which argues the run was sound, but I did not rely on that. **I discarded run 1 and re-ran step (e) in full (run 2 above), undisturbed, with the after-run read-back succeeding.** Run 2 is the evidence. Run 1's files are kept for the record under `tv006/xml/f26-e-airplane-run1-interfered/` and `tv006/f26-e.sequence-run1.txt`. Cost: about 4 minutes.

Device rules kept: one emulator at a time (each swap checked with `adb devices` empty first); reset and clean `--check` before and after the floor step; `mCurrentFocus` recorded (launcher) at the floor checks. Dev image `Medium_Phone_API_37.0` rebooted at 18:52:34 as `emulator-5554`, sdk 37, `navigation_mode` 2, reset, `--check` clean at 18:53, left running.

## Per acceptance ID, per channel

All pass on the baseline rows above, plus floor (e) re-run now. "JVM" means groundwork pins (no token) or the release test.

| ID | Dev (API 37) | Tablet (API 37) | Floor (API 26) | Other |
|---|---|---|---|---|
| REQ-006.A1 | PASS (PlayThrough x3 legs) | PASS | PASS | |
| REQ-035.A1 | PASS (PhoneLayout) | PASS (390 x 844 override) | PASS | |
| REQ-035.A2 | PASS against the gesture bar (`navigation_mode` 2) | PASS | PASS against the button bar | |
| REQ-036.A1 | PASS | PASS (own size, both orientations) | PASS (override, both orientations) | |
| REQ-036.A2 | PASS (placed pieces kept, running drag goes home) | PASS | PASS | |
| REQ-037.A1 | PASS (phone, tablet x2) | PASS | PASS | |
| REQ-047.A1 | PASS mechanism, **stays `implemented (unverified)`** on `chg-draft-req047-english-contract.md` | PASS | own-locale leg under `fi-FI` PASS | |
| REQ-047.A2 | PASS (`sv-SE`, list `[sv-SE, fi-FI]`) | PASS | steps (b) `sv-SE` and (c) `sv-SE,fi-FI` PASS (c: read-back plus direct run) | |
| REQ-001.A1, REQ-008.A1, REQ-008.A2 | PASS (play-through, closed world of texts, no currency symbol) | PASS | PASS | |
| REQ-010.A1 | PASS (airplane on by `cmd connectivity`) | PASS | PASS in step (e), run now on the final product | |
| REQ-010.A2 | PASS (installed app) + JVM release manifest PASS | PASS | PASS | V-01 PASS 3/3 |

WO-003 to WO-005 classes and the play sets (94/0) stay green on all images they run on.

## Substance audit (source read by me)

No `assume`, `@Ignore` or skip in any new kit file (the only `assumeTrue` is ProcessDeathKitTest, by design; one `skipped` variable in TouchTargetKitTest is a list of unprobed edge points, guarded by `probed == 0` failing). Token set: exactly the 13 IDs sit on tests that assert them.

- REQ-035.A1/A2: visible, inside own cell, above the nav inset, no scroll action or range; A2 requires the inset > 0 (not vacuous) and compares every cell bottom to window height minus the platform inset. Sound.
- REQ-036.A1/A2: one horizontal line across seven cells in TYPE-001 order, below the board, inside the safe area, both orientations; A2 places three pieces, turns twice, checks pose and boxes and also a drag running at the turn (fixture proves a drop would have locked). Sound.
- REQ-037.A1: sweeps every screen state, per-tag minimum (52 / 60 / 48 dp), probes real corners and edge middles, asserts every clickable is listed. Sound.
- REQ-047: compares shown texts with the resolved `fi` or `en` resources and the title with the file's `title.fi` or `title.en`; never a typed Finnish word; fixture asserts a discriminating text exists. Sound.
- REQ-006/001/008/010.A1: a touch play-through of a mini and a full puzzle with a closed world of texts, currency check, focus and package checks. Sound (see finding N3).
- REQ-010.A2: manifest parse with liveness checks (JVM) and installed-app package info with a non-blind control (device). Sound.
- G-01 pin (`ReleasePromiseKitTest`, corrected check 13): allow-list with 21 measured `androidx/` subpackages, self-tests that `kotlinx/` removal and a made-up `androidx/ads/` class are caught. Sound.

## Findings (classified)

| # | Finding | Class |
|---|---|---|
| 1 | None blocking. No product defect and no test defect found by this station. | n/a |
| 2 | Floor step (e) run 1 was disturbed by another station's reset; discarded and re-run (above). | process, not a defect |
| 3 | Carried from the code review, unchanged: REQ-010.A1's airplane leg cannot fail for an app with no network permission or code (N3); do not cite it as independent evidence at G4, the line rests on REQ-010.A2 and the allow-list. | note |
| 4 | Carried, as ruled: S1 (the S-36 wrap is not observable on this APK; WO-009), S-37's crash-dialog blind spot (interim floor rule followed). | note |

Corrected checks stay at the orchestrator's count of 13; this station added none.

## Coverage gaps

None for the 13 IDs on any named channel. REQ-047.A1 is `implemented (unverified)` by ruling. Not channels: human eyes (the owner's play), the process-death pair (two guarded skips, expected).

## Handoff

Forward. Verifier budget line for the Metrics: 0 verifiers added, 0 lines, within budget {count 1, lines 150}. Trees at hand-back: test bf79f0e02074, product 83abecd0d723, HEAD a797553, 42 status lines.
