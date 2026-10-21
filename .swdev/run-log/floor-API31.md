# RUN LOG — the floor channel `Phone_API_31` (orchestrator, 2026-10-20)

The floor move to Android 12 (`decisions.md` 2026-10-19 "The platform floor moves to Android 12 (API 31) …", step 2): the image installed, the AVD created with `Phone_API_26`'s screen, booted once, the trivial test run — the floor-channel proof of G3 (`gates-and-autonomy.md` §1). Copied from the session scratchpad (`floor31/`, wiped at session end); the script ran as one background job (boot detached, reset, the proof classes one per invocation, reset, kill).

Env: JAVA_HOME='C:\Program Files\Android\Android Studio\jbr', ANDROID_HOME='C:\Users\Jami\AppData\Local\Android\Sdk', Gradle wrapper 9.8.0, emulator 37.2.12; shell `cmd.exe //c ".\gradlew.bat <tasks>"` from Git Bash. The APKs under test are the committed tree's (`minSdk 26`); `minSdk 31` is WO-008's first task.

Tree hash (definitions as in `.swdev/run-log/WO-010.md`, the 2026-10-19 06:15 extension; product excludes `puzzle.schema.json`): `5706854 · 3 · test 598b6af2b8bf · product 683e7ab5cc58 · bundle 02feaef5d941 (106) · tools 0a7a3a785c3b` — the same test, product, bundle and tools hashes as WO-010's final run; the 3 status lines are the hook logs.

| # | Time | Command | Result | Result XML |
|---|---|---|---|---|
| f31-tools | 20:10:24–20:11:00 | SDK command-line tools 23.0 (`commandlinetools-win-16111833_latest.zip`, 154 957 218 bytes, sha1 `57d04f2d75eb8e8fffc5000a987e5de4b5a63e9d` = Google's `repository2-3.xml`) unpacked to `<sdk>/cmdline-tools/latest` | `sdkmanager --list` lists `system-images/android-31/google_apis/x86_64` (it now wraps the Android CLI) | — |
| f31-image | 20:11:38–20:14:04 | `cmdline-tools/latest/bin/android.exe --no-metrics --sdk=<sdk> sdk install system-images/android-31/google_apis/x86_64` | exit 0; `<sdk>/system-images/android-31/google_apis/x86_64` (`AndroidVersion.ApiLevel=31`, `SystemImage.TagId=google_apis`) | — |
| f31-avd | 20:14:30 | `~/.android/avd/Phone_API_31.ini` + `Phone_API_31.avd/config.ini`: `Phone_API_26`'s config with `image.sysdir.1`, `target`, `AvdId` and the display name changed (1080 × 2400, 420 dpi, 2 GB RAM, 4 cores, google_apis, no Play Store) | `emulator -list-avds` lists `Phone_API_31` | — |
| f31-boot | 20:14:46–20:15:29 | `adb devices` empty; `nohup emulator -avd Phone_API_31 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot &`; wait for `sys.boot_completed` | booted in 41 s ("Boot completed in 43797 ms"), `emulator-5554`; sdk 31, release 12, physical 1080x2400, density 420 | — |
| f31-reset1 | 20:15:52 | `bash tools/device_reset.sh`; `--check` | exit 0 / exit 0, **clean** (app not installed; no display override; rotation 1/0; show-ime 1 = this image's default; app-locales skipped, API 31 < 33; system_locales null, persist.sys.locale empty; airplane 0; adbd uid 2000); focus = launcher | — |
| f31-proof | 20:15:52–20:16:23 | `cmd.exe //c ".\gradlew.bat -q :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tangramplaytime.app.FloorChannelTest"` | Gradle exit 0 — **1/0/0** (`launchesAndShowsThePlayScreen` 4.358 s) | `TEST-Phone_API_31(AVD) - 12.xml` 17:16:17 UTC (20:16:18 local) |
| f31-launch | 20:16:23–20:16:41 | the same with `class=app.tangramplaytime.app.AppLaunchKitTest` | Gradle exit 0 — **2/0/0** (`aFreshStartOpensOnAMiniPuzzleWhoseTrayHoldsThreePieces` 4.451 s, `theFirstPuzzleShownOnAFreshInstallHasRatingOne` 1.053 s) | same file, 17:16:36 UTC (20:16:37 local) |
| f31-reset2 | 20:16:44 | `bash tools/device_reset.sh`; `--check` | exit 0 / exit 0, **clean**; focus = launcher | — |
| f31-kill | 20:16:46 | `adb emu kill` | `adb devices` empty | — |

**Verdict:** the floor channel `Phone_API_31` (Android 12) is reachable — image installed, booted once, the trivial test run green, the device reset clean on it. Re-counted by the orchestrator from the copied XMLs (no `<failure>` element in either).
