# WO-005 test and verify (#DevTools, REQ-046 A1..A4)

Station: test-verify, 2026-10-17, 12:33 to 13:09 (local, UTC+3). Nothing reused from earlier runs: every set ran from the final tree, on both images.

**Verdict: GREEN.** Every channel executed; 0 failures other than the 2 allowed guarded skips per image; substance sound for all four tokens.

## Tree hashes

| | start 12:33 | end 13:09 |
|---|---|---|
| HEAD | 057492c | 057492c |
| test tree | 1b4196afafc7 | 1b4196afafc7 |
| product tree | dfee9dcb22b2 | dfee9dcb22b2 |
| DevAidKitTest / ReleaseBuildKitTest | a10fce3b1c3e / 846cd90bdb06 | same |
| DevAidScreenKitTest / KitScreen / DevAidAppKitTest | 4ad3f04c9047 / 386c012f2be4 / 09904a94cee6 | same |

Status lines read 35 at start, 37 at end. The extra two are `platform-notes/android.md` and `proposals.md` (documents outside both hash trees, changed by someone else during the run). No kit or product file changed.

## Channels (XML copies under the scratchpad `xml/`: `C:/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/b3855b08-7ae8-4116-9ea4-789d5407fecd/scratchpad/xml/<dir>/`)

### 1. JVM (two Gradle runs, from the shell)
- Run 1 (12:33:26, exit 0): `:app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun`. Both APKs stamped 12:33:28.
- Run 2 (09:33:33 UTC, exit 0): `:play:testDebugUnitTest --rerun :contracts:test --rerun :store:testDebugUnitTest :content:testDebugUnitTest`. The store and content tasks were not forced, so I re-ran them with `--rerun` (09:33:46 and 09:33:47 UTC, exit 0). XMLs in `xml/jvm/` (play, contracts) and the build folders.
- `:play` 96 / 0 / 0 / 0 skipped, XML 09:33:35 to 09:33:38 UTC: DevAidKit 11, ReleaseBuildKit 1, Browsing 12, PlayController 16, Resume 13, TrayMetrics 6, Twist 6, PlayScaffold 1, scaffolding (BundleLoad 1, Controls 4, Decoration 6, DevAid 3, DevButtonHit 2, Persistence 3, ScreenSeam 5, Seam 3, Touch 3).
- `:contracts` 2/0. `:store` 12/0 (FileProgressStoreKit 10, StoreScaffold 1, WrittenShapeScaffold 1). `:content` 64/0 (PuzzleLibraryKit 16, PuzzleLoaderKit 37, ContentScaffold 2, ParserScaffold 9).
- `ReleaseBuildKitTest` system-out: 16 checks, all PASS. Both APKs fresh against the newest input. Debug: 276 product classes scanned, 51 `devaid/` classes, `dev_aid_` names (UTF-8), the service entry, "Wrong passcode." (UTF-8), "0417" in `classes2.dex`. Release: 223 product classes, 2 dex files; no aid class, no `dev_aid_` name (UTF-8 or UTF-16LE), no service entry, no "Wrong passcode.", no "0417" dex string. This also closes the code review's S1 (kit already carries the two literal markers).

### 2. Dev image `Medium_Phone_API_37.0` (emulator-5554, sdk 37, `adb emu avd name` confirmed)
Reset and `--check` clean before and after each step (app not installed: clean).
- Play set `:play:connectedDebugAndroidTest --rerun`, started 09:34:02 UTC, XML stamped 09:51:09 UTC, exit 0. `xml/dev-play/`. **94 / 0 / 0 / 0 skipped:** BoardTray 7, CornerPulse 4, Decoration 12, DevAidScreen 15, Drag 20, Overview 6, Preview 2, ResumeScreen 5, SolvedPicture 6, SolvedView 5, TopBar 12. Zero `<failure>` elements. The previously contested pin passes.
- App set `:app:connectedDebugAndroidTest --rerun`, started 09:51:32 UTC, XML 09:51:52 UTC (12:51:52 local), exit 0. `xml/dev-app/`. AppLaunch 2, AppWiring 2, DevAidApp 2, FloorChannel 1, ProcessDeath 2. The only `<failure>` elements are the two guarded skips (`AssumptionViolatedException: phase1/phase2 runs only with -e deathPhase N (given: null)`).

### 3. Floor image `Phone_API_26` (emulator-5554, sdk 26, headless; the dev emulator was killed first, `adb devices` empty, floor booted 12:52:22)
Reset and `--check` clean before and after each step.
- Play set, started 09:52:26 UTC, XML 10:07:47 UTC, exit 0. `xml/floor-play/`. **94 / 0 / 0 / 0 skipped**, same eleven classes and counts as dev. Zero `<failure>` elements.
- App set, started 10:08:05 UTC, XML 10:08:18 UTC, exit 0. `xml/floor-app/`. Counts as dev (2+2+2+1+2), the two guarded skips only.
- Floor emulator killed; `Medium_Phone_API_37.0` rebooted (13:08:56, sdk 37) and left running as emulator-5554.

### 4. Release channel
- `python .swdev/verifiers/v01_release_apk.py`: V-01 PASS 3/3 (no uses-permission; canary in none of 2 dex files; 0 okhttp3, 0 `Ljava/net/` from 385 product classes).
- `--positive-control`: PASS 2/2 (canary seen in `classes5.dex`; 78 `Ljava/net/` outside product code).
- `PYTHONUTF8=1 run_verifiers.py --project . --wo WO-005`: GREEN, 1 verifier, 150 lines, 0 failures, WO-005 added 0 verifiers and 0 lines, within budget `{count 1, lines 150}`.

## Failures

None. Product defects 0; test or fixture defects 0 in this run. (The earlier contested pin, `…stays Solved`, passes on both images in this run; its ruling as a fixture defect is the code review's item 3 and the implementer's run row i26, not re-judged here beyond seeing it green.)

## Substance audit (source read independently; no `assume`, `@Ignore` or skip in any of the five kit files, grep empty)

| Token | Test | Verdict |
|---|---|---|
| REQ-046.A1 | `DevAidKitTest.entering1234IsRefusedAndEntering0417UnlocksTheAid` (JVM); `DevAidScreenKitTest.entering1234…` (screen); `DevAidAppKitTest.inTheRealAppEntering1234…` (real app) | Sound. JVM: 1234 returns false, stays locked, sheet PROMPT with the wrong-passcode flag, controller state and the store's writes unchanged; locked `toggleSolution` and `solveNow` refuse; 0417 returns true, unlocked, MENU. Screen and app: the message text equals "Wrong passcode.", no menu, no overlay node, the field emptied; the empty entry and 417 are also refused; 0417 shows the menu with both labels; unlocked across a recreation. English is checked before any text is read. The fixture discriminates (a New puzzle with an order, so a leaked solve would change it). |
| REQ-046.A2 | `DevAidScreenKitTest.showTheSolutionDrawsOneShapePerPieceOfThePuzzleOnTheSilhouette` | Sound. Shape tags equal the puzzle's pieces on a mini and a full puzzle (different counts), bounds equal the stored polygon through `board`'s mapping within 1 px and lie in `silhouette`, labels are the piece names, colour sampled inside each polygon at the 55 % overlay over the silhouette. Quantifier met by the two sizes and by Hide then Show. |
| REQ-046.A3 | `DevAidKitTest.solveThisPuzzleNowShowsTheSolvedPictureAndLeavesTheBestTimeEmpty` (JVM, loops every bundled puzzle from New, plus In progress with LT1 locked away at 95 s, plus New with a turned tray piece and a mirrored parallelogram, both pose mirrorings); `DevAidScreenKitTest.solveThisPuzzleNowShows…` | Sound. JVM asserts Solved, celebration, every piece at its stored pose, `bestSeconds` null, and after `flush()` the store holds Solved with no placements and best null; a puzzle without an order fails (content finding), it is never skipped. Screen: Solved state key, `solved-picture` displayed after the REQ-023 timings, pixels inside the silhouette carry no piece colour, the solved row's best time reads the no-time literal, `dev-button` gone, store holds Solved with null best for both starts. |
| REQ-046.A4 | `ReleaseBuildKitTest.aReleaseBuildContainsNoDevButton` | Sound. Release APK holds none of five markers (class prefix, resource-name prefix in both encodings, service entry, "Wrong passcode.", "0417" dex string); the same debug APK holds all five (positive control in the same test); each APK must yield product classes and a parsing resource table; stale or missing APK fails. The verdict is on the APK, not the source. |

No tokened test skips where it runs; no token sits on a test that asserts nothing. Tokens appear only on the listed tests (grep of `REQ-046.A[0-9]` over `play/src` and `app/src`: A1 x3, A2 x1, A3 x2, A4 x1; none in `KitScreen.kt` or scaffolding).

## Coverage gaps
None for REQ-046.A1 to A4 on any named channel. Not a channel of this WO: human eyes (the owner's play of the debug build, per the brief), the process-death pair (two guarded skips, expected).

## Handoff
GREEN. Corrected checks this run: 0. Hand-over note for the orchestrator: add the verifier budget line (0 verifiers added, 0 lines, within budget) to the WO Metrics.
