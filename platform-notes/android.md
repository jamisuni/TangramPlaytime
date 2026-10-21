# Platform notes — Android  *(project-template, v0.17)*

Project-level rules the first Android run (TangramNoAds, 2026-10) learned
the hard way and wrote into its own `AGENTS.md`, where no later project
would find them. Copy this file into an Android project's root (or append
it to `AGENTS.md`) at P2; delete it for other platforms. Each line is a
duty of the station it names; none of it is methodology.

## This project's channels (P2, 2026-10-11)

- **Dev image:** AVD `Medium_Phone_API_37.0` (system image android-37.0, google_apis_playstore).
- **Floor image (from 2026-10-20):** AVD `Phone_API_31` (system image android-31, google_apis, x86_64; `Phone_API_26`'s screen, 1080 × 2400, 420 dpi) — `minSdk 31`, Android 12 (the owner's decision 2026-10-19; `decisions.md` 2026-10-19 "The platform floor moves to Android 12 (API 31) …"); floor channel proven 2026-10-20 (`.swdev/run-log/floor-API31.md`): booted headless with the same flags as below as `emulator-5554` in 41 s; `tools/device_reset.sh --check` is clean on it (app-locales skipped: API 33+). Its home app is `com.google.android.apps.nexuslauncher`. A crash dialog, titled `Application Error: <process>`, appears on the second crash of the same app within 60 s (`am crash <pkg>` twice), with no setting changed (WO-008 TASK-043, S-37Δ's control). The death harness leaves both APKs installed, so the next boot's uninstall reads "Success".
- **Floor image through WO-010 (history):** AVD `Phone_API_26` (system image android-26, google_apis) — `minSdk 26`; booted headless 2026-10-11 as `emulator-5554` in 13.5 s (`emulator -avd Phone_API_26 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot`). Kept on the machine; no longer a channel.
- **Toolchain:** JDK `C:/Program Files/Android/Android Studio/jbr` (25), Gradle 9.8 wrapper, AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00; SDK `C:/Users/Jami/AppData/Local/Android/Sdk` (platform 37, build-tools 36.0.0). SDK command-line tools 23.0 in `cmdline-tools/latest` (added 2026-10-20 for the android-31 image): `sdkmanager` now wraps the Android CLI, so install with `cmdline-tools/latest/bin/android.exe --no-metrics sdk install <path>` (`--no-metrics` turns its usage reporting off).
- **Shell:** `export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'` then `cmd.exe //c ".\gradlew.bat <tasks>"`; one emulator at a time, `adb devices` after every boot.

## Verification channels (name them at G3)

- **Two device channels at every WO close:** the full device suites run on
  the development image **and** on the **minSdk floor image** (e.g. API 26
  when `minSdk 26`), one emulator at a time. The floor renders differently
  (a path drawn under a canvas scale blurs there; draw paths in px). The
  floor channel must exist before the first WO with device tests
  (`gates-and-autonomy.md` G3; a floor waiver caps the WO at
  `implemented (unverified)`, v0.18).
- **Release safety at every WO close:** build the release APK and run the
  project's release verifier (no debug tooling, canary present), then a
  fresh debug APK with the verifier's `--positive-control` (the scanner
  must see every marker). One build at a time; record both result lines in
  the WO. Re-run the positive control after any toolchain, AGP or
  dex-affecting change. A verifier that pins an allow-list (callers of a
  sound or haptic API, say) fails on any new dependency or BOM bump —
  re-judge the inventory and log a decision row before extending it.
- **The run log is the baseline:** the orchestrator records each suite run
  as command · hash of the test tree · hash of the product tree · result
  XML; Test & Verify re-runs only what changed (`test-verify.md`).
- *(Project lessons, WO-004 close.)* **Judge a device run by its XML,
  never by Gradle's exit code.** The XML must exist, be stamped after the
  command started and carry the expected counts. Under `-q`, an
  `INSTALL_FAILED_ALREADY_EXISTS` exits 0 and writes no XML. The fix is
  `adb uninstall` of both packages, a clean `--check`, then a re-run.
- **A harness that installs build outputs refuses stale ones.** Gradle
  leaves an up-to-date APK unwritten, so an mtime check needs the channel
  line to force the package tasks (`:app:packageDebug --rerun
  :app:packageDebugAndroidTest --rerun`). `tools/process_death_check.sh`
  does this (WO-004 code review S2). A pass on an old APK is a pass for
  code nobody reviewed.
- *(Project lessons, WO-005 close.)* **A test that reads build outputs
  runs after them, in a separate Gradle run.** In one run with
  `:app:packageRelease --rerun`, `:play:testDebugUnitTest` started while
  `:app:lintVitalAnalyzeRelease` (which packaging waits on) still ran, and
  read the old release APK. The JVM line is two runs: build and repackage
  the APKs, then the tests.
- **"Absent from the release build" scans for names and for content.**
  Check the code's own markers (package, resource-name prefix, service
  entry) **and** the content the REQ quotes (its text, its passcode), each
  with a debug-APK positive control. Match a table entry (the dex string
  table, the resource string pool), never raw bytes: "0417" occurs in
  library vector paths of this release APK (`…C11.0417,8…`).
- *(Project lesson, WO-006.)* **An allow-list names each member of a
  family.** A bare `androidx/` in the release dex's package allow-list
  admits `androidx.ads`, `androidx.privacysandbox`, `androidx.webkit` and
  `androidx.browser`. List the measured subpackages by name, and give the
  self-test a made-up class inside the family (`androidx/ads/…`) that must
  land outside. The staleness inputs include the version catalog and
  `settings.gradle.kts`.
- *(Project lesson, WO-006.)* **Device time: riskiest image first, the
  whole set once.** Run the floor image first: every hard device failure in
  WO-006 came from API 26, and finding one last cost a re-run of the other
  two images. While fixing, run only the failing class. Run the whole sets
  once, on the final tree. A kit change re-opens every image's verdict.

## Device hygiene

- *(Project lesson, WO-008.)* **On API 31 a `wm density` change resets a
  locked rotation.** It rebuilds the 3-button navigation bar, and the new
  bar re-locks a locked rotation at the display's rotation of that moment
  (a lock to 90° sent within about 0.3 s of `wm density 270` fell back to
  0; `wm size` alone rebuilds nothing). Construct at the display's own
  rotation and turn afterwards, or freeze only once the density change
  has settled (`decisions.md` 2026-10-21 "The kit fix is accepted …").

*(2026-10-20: the floor is now `Phone_API_31`. The lines below that name API 26 are kept as history: they explain code and kit paths written for API 26, which stay until a task removes them with a reason. Re-measure such a rule on the new floor before relying on it.)*

- **The API 26 rules, re-judged on `Phone_API_31`** (WO-008 TASK-043 (f),
  2026-10-21; run log t043-f):
  - *The launcher crashes on a display change with the home screen in
    front:* **does not hold.** No `am_crash`, the launcher keeps its window.
    After a stopped run the image is still rebooted (the device-time rule);
    S-37Δ's `crash-window` item names a crash dialog if one appears.
  - *A forced width is clamped to twice the initial width:* **holds**
    (`wm size 2560x1600` reads back 2160x1600).
  - *An override over a running activity is a configuration change:* **does
    not hold.** An override over the running `MainActivity` recreates it
    (`wm_relaunch_resume_activity`, destroy and create), and so does the
    restore, as on the API 37 images. The kit still starts the activity
    after the override.
  - *The floor's limits:* no gesture bar **holds** (`navigation_mode` 0, the
    gesture overlays installed but off); `svc wifi` **exists** on API 31
    (no test uses it); the launcher is **not** relaunched by a display
    change.
  - *API 26 is not edge to edge:* **holds** for the play kit's full-window
    host. It is 189 px (72 dp, both bars) shorter on the floor than on API
    37, so a pixel-exact leg in a full-window host says nothing across
    images. In a kit-sized host (1080 × 1919 px) the board is the same px
    rect on both.
  - *The Compose read that parked across a relaunch:* **holds as a guard.**
    The 60 s bound fired once (TASK-043 hop 1, during the rotation re-lock
    above). Keep the bound and the steady-activity wait.
  - *The locale and airplane-mode routes:* the kit switches airplane mode
    itself (`cmd connectivity airplane-mode`, `sdk >= 30`); `cmd locale` is
    absent (API 33+). The root locale route (`vdc cryptfs`) is unmeasured:
    the image is file-encrypted and `vdc` is not on the shell's PATH.
    Re-measure before a WO needs device-locale lists on the floor.
  - *`show_ime_with_hard_keyboard`'s default:* **holds:** 1 on the floor, 0
    on the API 37 images. The reset restores each image's own default.

- Before and after every device step: reset the device state with the
  project's reset script (`--check` after; a non-clean check voids the step:
  reset, re-run, record the leftover in the WO).
- Confirm serials with `adb devices` after every boot; a second instance can
  come up as `emulator-5556` while 5554 is still held.
- A throwaway-signed copy of the release APK is launched on the floor image;
  the signed copy lives in the session scratchpad only, with the local
  debug key.
- Floor-image limits are recorded once as decisions, not rediscovered: no
  gesture bar (a gesture-bar rule is evidenced on the dev image), `svc wifi`
  unavailable, the launcher relaunches after a display change (the kit
  waits for it to settle).
- *(Project lesson, WO-006.)* **On API 26 never change the display while
  the home screen is in front.** The launcher crashes (`am_crash` NPE), and
  its crash dialog holds the focus: finishing activities never reach
  DESTROYED, and the keyboard inset never rises. Change `wm size` /
  `wm density` / the rotation under the app's own running activity. Then
  launch the next one, and close every activity only after the device
  restores. `--check` does not see the dialog, so record
  `dumpsys window | grep mCurrentFocus` with each floor check. After a
  stopped or killed floor run, reboot the floor instead of trusting the
  reset.
- *(Project lesson, WO-006.)* **API 26 display overrides.** A forced width
  is clamped to twice the initial width: `wm size 2560x1600` reads back
  2160x1600, so construct 1280 × 800 dp as 2160 × 1350 px at 270 dpi. And
  an override over a running activity is a configuration change, not a
  recreation (no `am_relaunch`), so start the activity after the override.
- *(Project lesson, WO-006.)* **A station never owns an emulator as its
  background task.** When another station stops that emulator, the
  background task's end re-wakes the first station, and it acts on a
  device it no longer holds. In WO-006 a reset landed on the next station's
  floor run, and that step was re-run. Boot detached (`nohup … &`), and let
  one station hold the device at a time.

## Compose test kit

- *(Project lesson, WO-008.)* **`performTouchInput` never reaches
  `Activity.dispatchTouchEvent`.** It dispatches on the
  `AndroidComposeView`, so a kit line that needs the activity's touch hook
  (the play-time touch) injects through the window:
  `Instrumentation.sendPointerSync`, DOWN then UP, at an inert point read
  on the screen in use (display coordinates = the node's window bounds +
  the window's location on screen). `KitApp.kitTouchThroughWindow` is the
  helper (design library fact 7; design review F1).
- Import rules *(recurred three times)*: `assertExists` /
  `assertDoesNotExist` are **member** functions of
  `SemanticsNodeInteraction` (never imported); `assertIsDisplayed`,
  `assertIsOn` / `assertIsOff` and the other `assert…` helpers are
  **extension** functions and need `import androidx.compose.ui.test.<name>`;
  so do `click()`, `swipe…()`, `longClick()` inside `performTouchInput`.
  The test author compiles device tests as soon as the API exists
  (`:<module>:assembleDebugAndroidTest`).
- A tagged display container (a pill, a label + value row) merges its
  descendants' semantics (`semantics(mergeDescendants = true)`), inert — no
  click action, no role — or tests and TalkBack read an empty node.
- Time-driven tests set the injected clock (`TestConfig.timeSource`) before
  launch and reset it after; a test that changes screen and then jumps the
  clock advances it on the main thread, never `Thread.sleep`. A device test
  reads density from the launched app under the display override, never
  from `targetContext`.
- A test that solves a puzzle (or completes a flow) by touch does not judge
  the final step by a visual that the completion itself replaces — the
  fixture must distinguish the states it claims to test
  (`task-decomposition.md`).
- *(Project lessons, WO-003 close — six corrected checks.)* Every assertion
  that is not a REQ's literal names its source in a comment: a seam row or a
  `decisions.md` row. If it has no source, drop it. WO-003 had two that
  invented a convention: tray rows sharing one `top`, and a visible share of
  the picture's base colour.
- A line with a quantifier ("always", "once", "never", "every") gets its
  adversarial path in the same test. For a UI line that means:
  - a move and a lift batched in one input frame;
  - an activity recreation over a state holder that outlives the activity
    (`ActivityScenario.recreate()`). `StateRestorationTester` does not
    remove content under a paused clock.
  WO-003 N3 and F2 were reachable defects behind a green kit.
- Read geometry from the drawing (pixels) or from the nodes, never from a
  number in a brief or a decision row's prose. Clip every pixel sampler to
  its own capture. WO-003: a sampler read past its capture, and the badge
  disc was assumed to sit at its node's centre.
- *(Project lesson, WO-004.)* **A pin that skips where it runs cannot
  fail.** The connected runner writes an `assumeTrue` skip into the XML as
  a `<failure>` while Gradle exits 0. When the bundled content cannot reach
  a case at a channel's screen size, the kit constructs it: a kit-owned
  puzzle through the seams, or a kit-chosen host size. Otherwise the leg
  fails loudly. The only legitimate guarded skips are the ones the WO names
  (WO-004: `ProcessDeathKitTest#phase1` / `#phase2` without `deathPhase`).
- *(Project lesson, WO-005.)* **Read a position on the screen you use it
  on.** A point read on one puzzle and tapped on another moves with the
  content: each puzzle's tray height sets the board's bottom, and a solved
  puzzle's row replaces the tray strip. Read it after the navigation, and
  assert it lies in the region the test means (inside `board`, outside
  `solved-row`) before acting, failing loudly otherwise.
- Channel: `-Pandroid.testInstrumentationRunnerArguments.class=A,B` through
  `cmd.exe //c` runs only `A`. Run one class per invocation, or omit the
  argument for a whole set. The connected-test XML folder is overwritten on
  every run, so copy each XML you cite to the scratchpad.
- *(Project lesson, WO-006.)* **A comma never reaches an instrumentation
  argument.** AGP joins every `android.testInstrumentationRunnerArguments.*`
  into one comma list and splits it on commas, so `kitDeviceLocales=sv-SE,fi-FI`
  arrives as `-e kitDeviceLocales sv-SE`. Use a separator without a comma,
  or run that one leg with `am instrument -w -e <key> <a,b> …` and the APKs
  installed by adb. A control run with a value that must fail shows whether
  the whole value arrived.
- *(Project lesson, WO-006.)* **Bound every Compose call a device kit
  makes, and wait for a steady activity first.** On API 26 a Compose read
  polled while a turn relaunched the activity parked forever in Espresso's
  idle wait, with the main thread idle. Wait without Compose until one
  resumed activity has been steady for a settle time, then read. Bound each
  Compose call (60 s → a named stall failure), and never retry it. No
  restore path has an unbounded wait. Read values derived from the insets
  only after the bars settle: after a display change they move for up to
  about 5 s.

- *(Project lessons, WO-010.)* **Under a paused clock, `performScrollTo`
  never returns once a scroll is needed.** ui-test 1.12.1's
  `performScrollTo` repeats its scroll until the node needs none, and the
  scroll animates on the main clock; with `mainClock.autoAdvance = false`
  the animation never ends and the call spins (WO-010: 35 min on the floor,
  then 2 h in the whole set — at 13 puzzles no grid cell needed a scroll,
  so the hang only appeared at 106). Scroll through the kit's
  `KitScreen.scrollTo` (auto-advance on for that one call, paused again
  after), and settle on the paused clock afterwards as before (run log
  f2-sB, `reviews/WO-010-code-review-delta.md` S-2).
- *(Project lessons, WO-010.)* **Every semantics read waits for a frame,
  so node reads stay out of per-sample loops.** A `BoardMappingKey` read
  for every edge sample made REQ-023.A2 run over 30 min at 106 puzzles;
  reading the mapping once per puzzle (the board does not move inside a
  leg) gave the same pixels in about 6 min. A per-puzzle or per-cell leg
  is timed on the floor at the bundle size it will meet, before the whole
  set runs (WO-010 F2 and its look-alikes).
- *(Project lessons, WO-010.)* **A "3 px in / 3 px out" pixel set is
  measured from the exact outline, never made by widening a raster mask
  and eroding or dilating it by a square.** The kit's silhouette mask
  (each piece widened 0.75 px) fills the tip of every 315° notch, where a
  piece corner meets the middle of another piece's edge; `erode(3)` then
  counted pixels 0.46 px from the outline as "3 px in", and the solved
  picture's designed outline stroke there went red on one image and not
  another (where the pixel grid fell). The same construction left 3–5 px
  off every edge unjudged on the outside. Use `KitScreen.silhouetteBands`.
  A margin set gets a self-check against an oracle that does not share
  its construction (WO-010 F3, `decisions.md` 2026-10-20 "F3's cause is
  the REQ-023.A1 leg's inside mask …", `reviews/WO-010-code-review-delta-2.md`
  S-1).
- *(Project lessons, WO-010.)* **API 26 is not edge to edge, so the
  floor's board sits at a different pixel offset from an API 35+ phone
  with the same dp size.** On the 411 × 914 dp phone the floor's board is
  1325 px tall against 1514 px on API 37, 94.5 px higher: the same puzzle
  falls on a different pixel grid. A pixel-exact leg green on one image
  says nothing about the other (WO-010 F3, run log f3-probe-*).

## Gradle from Git Bash on Windows

`cmd.exe //c ".\gradlew.bat assembleDebug test"`; set `JAVA_HOME` and
`ANDROID_HOME` in single quotes in the command if the shell predates them;
forward slashes in every path. The rest of the Windows page is
`adapters/claude/CLAUDE.md`.

---

*v0.1 — 2026-10-07 (SWDev v0.17, T-18). Source: TangramNoAds `AGENTS.md`
lessons WO-003…WO-008.*
