# Platform notes — Android  *(project-template, v0.17)*

Project-level rules the first Android run (TangramNoAds, 2026-10) learned
the hard way and wrote into its own `AGENTS.md`, where no later project
would find them. Copy this file into an Android project's root (or append
it to `AGENTS.md`) at P2; delete it for other platforms. Each line is a
duty of the station it names; none of it is methodology.

## This project's channels (P2, 2026-10-11)

- **Dev image:** AVD `Medium_Phone_API_37.0` (system image android-37.0, google_apis_playstore).
- **Floor image:** AVD `Phone_API_26` (system image android-26, google_apis) — `minSdk 26`; booted headless 2026-10-11 as `emulator-5554` in 13.5 s (`emulator -avd Phone_API_26 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot`).
- **Toolchain:** JDK `C:/Program Files/Android/Android Studio/jbr` (25), Gradle 9.8 wrapper, AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00; SDK `C:/Users/Jami/AppData/Local/Android/Sdk` (platform 37, build-tools 36.0.0).
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

## Device hygiene

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

## Compose test kit

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

## Gradle from Git Bash on Windows

`cmd.exe //c ".\gradlew.bat assembleDebug test"`; set `JAVA_HOME` and
`ANDROID_HOME` in single quotes in the command if the shell predates them;
forward slashes in every path. The rest of the Windows page is
`adapters/claude/CLAUDE.md`.

---

*v0.1 — 2026-10-07 (SWDev v0.17, T-18). Source: TangramNoAds `AGENTS.md`
lessons WO-003…WO-008.*
