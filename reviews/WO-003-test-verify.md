# Test & Verify — WO-003 #Solving

**Date:** 2026-10-12 20:23-20:40 (clock `date`)  ·  **Station:** test-verify (Sonnet 5.5)  ·  **Mode:** Light
**Tree:** HEAD 3fa75ac, 59 status lines (run log k17: 58; the extra line is not under `play/src`, `app/src` or `contracts/src`; both source hashes below equal k17), test tree `29306d193615` (= k17), product tree `84a18f2f4e81` (= k17; recipe: `play/src/main` + `play/src/debug` + `app/src/main` + `play/build.gradle.kts` + `app/build.gradle.kts`, sorted, sha1sum of sha1sums).

## 1. Hashes
Both equal k17, so the dev-image baseline stands (play set 51/0 by composition, app set 2/0, JVM 45 + 2). I checked its XMLs in the session scratchpad: `xml/r2-dev-play-all-1` (i17, 51 tests / 1 failure, the contested Decoration leg), `xml-r3deco2` (k17, DecorationKitTest 12/0, 17:22:05 UTC), `xml/r2-dev-app-all-1` (2/0). I did not re-run the dev image (nothing disputed). On the floor image all 51 passed, including the leg that failed on dev in i17, so the ruling "fixture defect, corrected check 6" is confirmed on a second image.

## 2. Run rows
| # | Time | Command | Result | XML (my scratchpad `.../scratchpad/tv/`) |
|---|---|---|---|---|
| t1 | 20:24:55 | `:play:testDebugUnitTest :contracts:test --rerun-tasks -q` (JVM, exit 0, 6 s) | play 45 (Controller 16, TrayMetrics 6, Twist 6, PlayScaffold 1, BundleLoad 1, DecorationScaffold 6, ScreenSeam 3, Seam 3, Touch 3) + contracts 2; 0 failures/errors/skipped | `jvm/` |
| t2 | 20:25 | `adb emu kill` (dev), `adb devices` empty; emulator -avd Phone_API_26 headless; `adb devices` emulator-5554, sdk 26 | booted | `emu26.log` |
| t3 | 20:25:35-20:35:22 | `device_reset.sh` (nothing installed), `:play:connectedDebugAndroidTest --rerun -q` (whole set, no class arg), exit 0, 9 m 47 s | **51 / 0 / 0 / 0** on Phone_API_26(AVD) - 8.0.0 (17:35:20 UTC): BoardTray 7, CornerPulse 4, Decoration 12, Drag 20, Preview 2, SolvedPicture 6 | `floor-play/` |
| t4 | 20:35 | `device_reset.sh --check` | clean (not installed) | - |
| t5 | 20:35:37-48 | reset, `:app:connectedDebugAndroidTest --rerun -q`, exit 0 | **2 / 0 / 0**: AppLaunchKitTest 1, FloorChannelTest 1 (17:35:45 UTC) | `floor-app/` |
| t6 | 20:35 | `--check` | clean | - |
| t7 | ~20:36 | floor stopped; emulator -avd Medium_Phone_API_37.0 headless; `adb devices` emulator-5554, sdk 37 | booted, **left running** | `emu37.log` |
| t8 | 20:37:35 | `:app:assembleRelease :app:assembleDebug --rerun-tasks -q`, exit 0 | both APKs rebuilt 20:37 (code review N2) | - |
| t9 | 20:38 | `run_verifiers.py --project . --wo WO-003` | see section 4 | - |

## 3. Cold launch (information only, code review F3; carried to WO-004)
Debug APK (same tree), launcher `app.tangramplaytime.app.MainActivity` (merged manifest), `force-stop` then `am start -W`, three runs each; reset and uninstall after, `--check` clean.

| Image | TotalTime (ms) |
|---|---|
| Phone_API_26 (floor) | 809, 690, 786 |
| Medium_Phone_API_37.0 (dev, measured about 1 min after boot) | 2316, 1725, 1890 |

The implementer's 1.9 s on the dev image is reproduced (1.7-2.3 s). The floor emulator is faster (about 0.8 s). No REQ names a launch time; no verdict effect.

## 4. Verifier lines
- V-01 PASS 3/3 checks on `app/build/outputs/apk/release/app-release-unsigned.apk` (3.8 s), rebuilt in t8.
- Positive control PASS 2/2 on `app/build/outputs/apk/debug/app-debug.apk` (4.6 s).
- Budget line for WO Metrics: `verifiers: WO-003 added 1 verifier(s), 150 lines — budget {"count": 1, "lines": 150} — within budget`; `verifiers: GREEN — 1 verifier(s), 150 lines total, 0 failure(s) (manifest mode)`.

## 5. Per-ID by channel
P = pass. JVM = unit tests; dev = baseline k17/i17 by composition; floor = t3/t5. Device classes: BT BoardTray, DR Drag, PV Preview, DC Decoration, CP CornerPulse, SP SolvedPicture, AL AppLaunch.

| ID | JVM | dev | floor | Test |
|---|---|---|---|---|
| REQ-002.A1 | P | - | - | PlayControllerKit (unverified, section 7) |
| REQ-011.A1, A2 | - | P | P | BT |
| REQ-012.A1 | - | P | P | BT |
| REQ-012.A2 | P | P | P | PlayControllerKit; DR |
| REQ-013.A1 | P | - | - | TrayMetricsKit |
| REQ-013.A2 | - | P | P | DR |
| REQ-013.A3 | - | P | P | BT |
| REQ-014.A1 | - | P | P | DR (unverified, section 7) |
| REQ-014.A2, 015.A1, 015.A2, 016.A1, 016.A2 | - | P | P | DR |
| REQ-017.A1, A2 | P | P | P | TwistKit; DR |
| REQ-018.A1, A2 | - | P | P | DC |
| REQ-018.A3 | P | - | - | PlayControllerKit |
| REQ-020.A1 | P | P | P | PlayControllerKit; DR |
| REQ-020.A2 | - | P | P | DR |
| REQ-021.A1, A2 | - | P | P | PV (A1 includes the moving-release leg) |
| REQ-023.A1, A2, REQ-039.A1, REQ-045.A2 | - | P | P | SP |
| REQ-043.A1, A2 | - | P | P | DC |
| REQ-045.A1 | - | P | P | BT; AL (twice, by design) |
| REQ-051.A1, A2 | - | P | P | CP (A1 includes the recreation leg) |

All 32 in-scope IDs carry a token and are green on every channel that applies to them (JVM-only IDs have no device test by design). The untokened pins (F1, F2, PG tray, two shakes and the rest) are green on dev and floor too.

## 6. Substance audit
Read in full: DragKitTest REQ-012.A2, REQ-013.A2, REQ-017.A1/A2, REQ-020.A1/A2; DecorationKitTest REQ-043.A1/A2 and the F1 / PG-tray pins; PreviewKitTest REQ-021.A1 (both legs); CornerPulseKitTest REQ-051.A1; the F2 shake pin and the two-shakes pin.
- REQ-012.A2 (device): exactly one home per piece at ten moments across a lock and a go-home. Sound. REQ-013.A2: every cell and the strip compared after each tap on every piece, with a check that the tap really turned it. Sound.
- REQ-017 (device): 90 degrees in 5-degree moves gives exactly two steps and back, 20 degrees none, plus box vs drawn corners. Sound. REQ-020.A1: the lock verified against the engine's own `findLock`; three miss kinds end in the cell. REQ-020.A2: texts and tagged nodes unchanged at five times. Sound.
- REQ-043: A1 asserts letters and cell containment for every full-set puzzle and no marks on SQ/PG; A2 asserts ghost present, mark and miniature absent, with pixel checks. Sound.
- REQ-021.A1 moving-release leg: finger moves and lifts in one input batch with the clock paused, at a point where the engine's search differs from the drawn preview; the placement must equal the drawn preview. Discriminates (red before the fix, run log k15). REQ-051.A1 recreation leg: a real `ActivityScenario.recreate()`, then 64 frames with no pulse or marker, after proving the new screen is the same puzzle with the piece home. Sound.
- F1/F2 pins and the two-shakes pin assert the stated REQ-015/016 behaviours and were red before the fixes. The PG-tray tap pin reads the disc from the drawing (corrected check 6), consistent with the decisions rows.
- Minor, no action: the REQ-015.A1 board half only asserts "not dragged and stays" (weaker than the tray half, not a proxy). A tap on the PG miniature's drawn pixels away from the disc turns the piece: this follows the 2026-10-12 ruling and the amended pending draft, not Contract text.
- Scaffolding tests (`scaffolding/*`, PlayScaffoldTest, FloorChannelTest) carry no token and are not counted. Token grep: `play/src/test` 7 IDs, device sets 29 distinct, REQ-045.A1 twice, placement unchanged. No assertion-free token test found.

## 7. Unverified lines (report at G4)
- **REQ-002.A1**: green (JVM, every bundled puzzle completed through S-2 actions, mirror badge included); stays `implemented (unverified)` for `capture-outbox/chg-draft-req002-a1-mirror.md`.
- **REQ-014.A1**: green on dev and floor (drawn at the finger's position of the current or previous frame, 2 px); stays `implemented (unverified)` for `capture-outbox/chg-draft-req014-a1-without-delay.md`.
- Not in scope and not tested: REQ-002.A2, REQ-039.A2 (WO-009).

## 8. Failures and gaps
No test failed on any channel in my runs; nothing to reclassify; no class re-run was needed. Corrected checks in the WO (from the log, not new): 6. Disputes worker won / checker won: 1 / 0. Coverage gaps: none for a named channel. Human eyes were not a DoD channel. The dev suites were not re-run by me (hashes equal k17, baseline XMLs verified). Verifier freshness (N2) handled by rebuilding before the runner.

## Verdict
**GREEN, forward to the Traceability Auditor.** Every channel the WO names ran: JVM 45 + 2 (re-run), floor image 51/0 play and 2/0 app (re-run, new on this tree), dev image 51/0 and 2/0 (baseline, hashes equal), release verifier 3/3 with positive control 2/2. 0 Blockers.

## Handoff
**Status:** forward (0 Blockers; 0 failures; 0 coverage gaps).
**Evidence:** run rows t1-t9; XMLs in `C:/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/98b81014-f591-4ac3-89d7-0135c8d9e787/scratchpad/tv/`.
**Unverified:** REQ-002.A1, REQ-014.A1 (pending drafts).
**Metrics inputs:** corrected checks 6; disputes 1/0; verifier budget 1 verifier / 150 lines; floor play run 9 m 47 s; floor app run 11 s.
**Device state:** dev image `Medium_Phone_API_37.0` running (emulator-5554, sdk 37), app uninstalled, reset `--check` clean. Floor stopped.
**Carried:** F3 cold launch (0.7-0.8 s floor, 1.7-2.3 s dev) and N1 to WO-004; N5 to `proposals.md`.
**Next:** Traceability Auditor (`trace-check --wo WO-003`, with PYTHONUTF8=1).
