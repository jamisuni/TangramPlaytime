# Review — code · WO-003

**Date:** 2026-10-12  ·  **Reviewer:** fresh context (Sonnet 5.5)
**Inputs:** the brief's read list (WO-003 § Scope/Constraints/Log, `tasks.md` § WO-003, plan review, the 23 WO-003 `decisions.md` rows, architecture G-xx/O-xx, directives, REQ-002/011–018/020/021/023/039/043/045/051), the whole `play/`, `app/`, `.swdev/verifiers/`, `tools/device_reset.sh` diff. Product files read in full: PlayController, PointerRouter, HitTest, PlayScreen, DragLayer, PreviewLayer, SolvedPicture, CornerPulse, FlipBadge, TrayDecorations, Celebration, TrayMetrics, TwistTracker, TouchClassifier, TrayRest, SilhouettePath, PlayLayout, PlayEvent, MainActivity, AppGraph, PlayLayouts, DebugCanary, build file. Not read line by line: PlayTexts, PlaySemantics, PlayTags, PlayPalette, PlayDrawing bodies.

## Verdict

**forward** — no Blocker. Every token I read asserts its line's meaning. The three kit corrections are sound. Four Shoulds are product-side defects a user can reach today; they go to the orchestrator for triage (fix now or log for WO-004/006). 0 Blockers, 4 Shoulds, 5 Notes.

## Checklist applied

- [x] **Directives** — D3: `PlayDrawing.kt` is the one file with no REQ named (N4). D5: the espresso pin is logged (decisions row 71). D4: see N4 (dead `twistOnly` path).
- [x] **Guardrails** — greps below all clean (G-12, G-14, G-10/O-06, G-17, G-08/O-08). O-01 is met under decisions row 61 (the px hit test picks a piece and derives no position). O-01's text does list "inside tests" under `:kernel`; the row's reading stands, and the owner sees it on the G4 surface.
- [x] **Contract** — `git status` shows no change to `ILockEngine.kt`, `IPuzzleLibrary.kt`, `Types.kt`, `contracts/`, `kernel/`, `content/` or `Tangrams/`.
- [x] **Scope** — every file traces to an in-scope REQ. Two recorded readings go beyond the tag table: the ghosts are gone once solved (row 74), and the pop is a brightness bounce (row 62). Both are logged.
- [x] **Hard-stops** — none touched. The manifest declares no `uses-permission`.
- [x] **Evidence re-derived** — see the record below.
- [x] **The conceptual 20 %** — F1 to F4 and N1 come from reading the touch routing, the lifecycle and the arithmetic against REQ-015/016/018/051, not from the diff's boilerplate.

## Re-derivation record

| Command / check | Result |
|---|---|
| `gradlew :play:testDebugUnitTest :contracts:test --rerun-tasks -q` (exit 0) | XML counts: PlayControllerKitTest 16, PlayScaffoldTest 1, TrayMetricsKitTest 6, TwistKitTest 6, BundleLoad 1, Decoration 6, ScreenSeam 3, Seam 3, Touch 3 = **45**, plus ContractsSanityTest **2**. Failures 0, errors 0, skipped 0. |
| `gradlew :play:assembleDebugAndroidTest :app:assembleDebugAndroidTest -q` | exit 0, both device kits compile. |
| `python .swdev/verifiers/v01_release_apk.py` | PASS 3/3 (no `uses-permission`; canary in 0 of 2 dex; 0 okhttp3 classes, 0 `Ljava/net/` from 263 product classes, 72 library references not counted). |
| `… --positive-control` | PASS 2/2 (canary seen in `classes9.dex`; 78 library `Ljava/net/` references seen). |
| Tokens | `play/src/test`: 7 distinct (REQ-002.A1, 012.A2, 013.A1, 017.A1, 017.A2, 018.A3, 020.A1). Device sets: 29 distinct, REQ-045.A1 twice (BoardTray and AppLaunch). None on REQ-002.A2 or REQ-039.A2. |
| `import android.` in `play/src/main` | 0 hits. |
| `tangramplaytime.kernel` / `.content` in `play/src/main` | 0 hits. |
| Wall clock (`currentTimeMillis`, `nanoTime`, `SystemClock`, `Instant`, `delay(`, `sleep`, `uptimeMillis`) | 0 hits. Animation uses `withFrameNanos` only. |
| Colour literals outside `PlayPalette.kt` | 0 hits. |
| Canvas `scale(` / `graphicsLayer` | 0 hits (the one `scale` hit is `DragOrigin.scale`, a Float). |
| `DebugCanary` referenced from `src/main` or `app/` | 0 hits. |
| User-visible literals in `play/src/main` | none. `BasicText` takes `title` and `letter` parameters; the ⇋ glyph is a path. |
| `.gitattributes` | absent. `device_reset.sh` is LF on disk, and git warns that the working copy is CRLF-converted (`autocrlf`). |
| Cold launch 1.9 s | **Not re-derived.** The app is not installed on the dev emulator after the kits, and I left device state alone. The cause is visible in the code: `AppGraph.controller` is first read inside `setContent`, so the bundle parse runs in the first composition on the main thread. |

## Substance audit (checklist 1)

I read the body of every token below and found no proxy and no empty test.

- **JVM kit.**
  - REQ-002.A1: every listed puzzle is completed through S-2 actions alone, including the badge press where a solution needs the mirrored PG, and ends Solved. That is the pending draft's reading. The line stays `implemented (unverified)` (decisions row 58).
  - REQ-012.A2: a script of locks, misses, cancel, twist, badge, board drag and open-mid-drag, with a where-is-every-piece check after each action.
  - REQ-018.A3: the fixture shows the engine locks the fitting image and finds nothing for the mirror image. The controller drop of the mirror image then does not lock, and the discrimination case locks.
  - REQ-020.A1: a 0.5-unit sweep with board and tray releases. Each outcome is compared with the engine's own `findLock`, and the sweep asserts it held both outcomes.
  - REQ-013.A1: 360 × 780 dp, every shipped puzzle's tray, both directions.
  - REQ-017.A1 and A2: every start angle, both directions, tracker and dragged turn, and no `Turned` event for 20°.
- **Device kit.**
  - REQ-014.A1 is the one-frame bound of the draft (current or previous frame, 2 px). It stays `implemented (unverified)`.
  - REQ-014.A2: drawn scale is checked at 120 ms, during moves, at release, mid-glide, on the board and on a board pick-up.
  - REQ-015.A1: the 3 s press on the tray turns the piece. The board half only asserts it is not dragged and stays, which is weaker but not a proxy.
  - REQ-015.A2 has a 14 dp negative pin.
  - REQ-016.A1: the turn key and the pixels are checked for four pieces over eight taps.
  - REQ-016.A2: the fixture is engine-refused, and position and turn are unchanged after the shake.
  - REQ-011.A1 and A2: every pixel more than 3 px inside is the silhouette colour, none outside. A2 also checks the node box.
  - REQ-012.A1 compares nodes and pixels between full-set puzzles. The order is pinned untokened.
  - REQ-013.A3: the drawn scale and box are the same in the mini and the full puzzle.
  - REQ-021.A1 reads the preview keys and compares them with the locked placement. It is the still-finger case only (N3).
  - REQ-021.A2: no node, no stroke pixels, and the drop goes home.
  - REQ-051.A1: one marker per engine corner, gone by 1 s, no second pulse.
  - REQ-051.A2: no pulse nodes and the same texts and tags.
  - REQ-018.A1: tray and board mirror, with the engine's mirrored placement. REQ-018.A2: one badge per puzzle with the PG, none without.
  - REQ-023.A1, A2, REQ-039.A1 and REQ-045.A2 are sound (see the checklist 3 note). REQ-045.A1 is covered twice on purpose.
  - I skimmed rather than read the bodies of REQ-012.A2 (device), REQ-013.A2, REQ-017 (device), REQ-020.A1/A2 (device) and REQ-043.A1/A2. Their headers and oracles are stated consistently with the lines.
- **Skippable tests.** The only `assumeTrue` is on an untokened pin (`aBoardPieceWithRoomToTurnTurnsOneStepClockwise`). No token-carrying test can be skipped. The two `android.util.Log` lines are diagnostics and gate nothing.
- **Checklist 2.** The REQ-002.A1 and REQ-014.A1 tests match their drafts' readings. Both are recorded for `implemented (unverified)` (decisions row 58, WO log). The task status cells are set at close.
- **Checklist 3.** All three corrections are sound.
  - The tray-row pin now reads rows by S-5 membership and centre y. It still fails if the order or rows are wrong.
  - The capture-clipped sampler keeps ≥ 4 discriminating samples per side, an explicit same-point-set skip for the square, and a centre-fitted alternative pose. A counter-clockwise draw still fails it.
  - With the ≥ 1 % base-colour assertion dropped, REQ-023.A1 still discriminates. A picture that is too small leaves silhouette-colour pixels inside. One that is too large changes pixels outside. A bare-board hole is also checked. Nothing in REQ-023 or the `ArtLayer` KDoc mandates a visible base.
- **Checklist 5.** Both TASK-014 fixes are sound. `clipPath` with the silhouette's own `Path` removes the `DstIn` problem. A 1.2 dp bevel-joined outline keeps the clip edge hidden and does not bulge into concave notches. The 1.9 s cold launch is F3.
- **Checklist 6.** `onStop` → `goHome()` is correct for the interrupted-drag draft. On rotation the activity is destroyed and the finger is cancelled anyway, so the piece home is the only sound result. The cost is F2, a separate defect.
- **Checklist 7.** The verifier's class filter (`Lapp/tangramplaytime/`) matches every module, because all modules use that package. Its liveness rules hold: it fails if it finds no product class, if `aapt2` does not name the package, or if the positive control does not see the canary and a library `java.net` reference. See N2 for the freshness gap.

## Findings

### F1 — QUALITY · Should · `FlipBadge.kt:badgeSlots`, `HitTest.kt:at`
- **Observation:** the badge's 60 dp touch area is checked first, whatever it overlaps, and it is not kept inside the PG's cell. In the tray the area top is `cell.top − 20 dp` (disc 10 dp below the top, area ±30 dp), and it ends at the PG cell's right edge.
  - By the arithmetic for 411 dp wide (the 1080 px / 2.625 emulator): scale ≈ 28.6, PG cell x ≈ 113–215. The area then covers x 155–215, 12 dp into row 1 (the 8 dp gap plus 12 dp).
  - LT2's cell is the full row height, x ≈ 157–290. So its bottom 12 dp × about 58 dp, including the left half of the base of the resting triangle, belongs to the badge, not to LT2.
  - A tap there mirrors the PG. A drag starting there does nothing at all ("a touch that starts on a badge never drags").
  - REQ-015 says a tray piece's touch area is its whole cell. Decisions row 74 says the area "never reaches the neighbouring cell", but that holds only horizontally.
  - On the board, the 60 × 60 dp area at `box.right + 22 dp`, `box.top − 4 dp` takes precedence over any neighbouring piece's shape inside it. No kit test taps outside the PG cell near the badge, so nothing catches either case.
- **Proposed resolution:** clip the tray area to the PG cell, or move the disc so the whole 60 dp area sits inside the 88 dp-plus cell (disc 30 dp below the top, still 60 dp across, REQ-018 rule). On the board, let a hit inside another piece's drawn shape win outside the 38 dp disc. Add a kit pin that taps LT2's bottom-left. Product-side. If deferred, log it on the G4 surface.
- **Triage:**

### F2 — QUALITY · Should · `PlayController.pulse` / `shake`, `CornerPulse.kt`, `PlayScreen.kt:BoardPiece`
- **Observation:** `pulse` and `shake` are one-shot cues but they live in the process-lifetime controller (`AppGraph`). They are cleared only by `open`/`load`, never after the screen has played them.
  - Activity recreation (rotation, a locale change, a theme or config change) builds a new composition. `CornerPulse` finds `controller.pulse != null` and plays the 600 ms corners again, however long after the miss. `BoardPiece`'s `LaunchedEffect(shake)` re-shakes the last refused piece.
  - REQ-051 says "pulse once" and REQ-016 says "shake it once". The kit tests one composition only.
  - The same recreation also opens a solved puzzle with `elapsedMs == null` for one frame, so the coloured pieces show before the picture (N1).
- **Proposed resolution:** the screen reports a played cue back (`pulse` and `shake` become null after their last frame, or a serial the controller marks consumed), or the controller clears the cue on the next action. Add a recreation case to the kit (`StateRestorationTester` or `ActivityScenario.recreate()`). Product-side.
- **Triage:**

### F3 — QUALITY · Should · `AppGraph.kt`, `MainActivity.kt`
- **Observation:** `AppGraph.library = PuzzleLibraries.bundled(engine)` parses and checks 13 puzzles lazily on first read. That read is inside `setContent`, so it runs on the main thread in the first composition. The implementer measured a 1.9 s cold launch on the dev emulator (I did not re-measure), with nothing on screen but the window background.
  - The floor is API 26, and the audience is children on possibly slow phones. A slower device approaches the 5 s ANR threshold for a launch and shows a blank window for the whole parse.
  - WO-004's store read will add to this.
- **Proposed resolution:** read the bundle off the main thread (a `produceState`/`LaunchedEffect` on `Dispatchers.Default`, with the screen composed once the controller exists), or at least measure on the floor image and log the number against REQ-006. This is a design call for the orchestrator; it can defer to WO-004, which has to do the same job for the store.
- **Triage:**

### F4 — QUALITY · Should · `tools/device_reset.sh` (repo root: no `.gitattributes`)
- **Observation:** the machine has `core.autocrlf` on (git warns "LF will be replaced by CRLF" for the working copy). The `.sh` is LF on disk now, but the next clone or checkout converts it to CRLF. Then `set -uo pipefail\r` aborts with "invalid option name" and the device-hygiene duty breaks silently, on the machine this project runs on.
- **Proposed resolution:** add `.gitattributes` with `*.sh text eol=lf` (and `*.py text eol=lf`). A one-line repo-root change. Product-side, infrastructure.
- **Triage:**

### N1 — QUALITY · Note · `SolvedPicture.kt:SolveDriver`
`unsolvedSeen` holds a single id. Solve A, solve B, open A (jump), open B (`unsolvedSeen == B` again) replays the whole celebration on an already solved puzzle. It is latent: no UI path calls `open()` in this WO. It becomes live when WO-004 navigates, so fix it before `open()` gets a caller (a controller-held "just solved" event or the `PlayEvent.Solved` is the clean source). The one-frame flicker on opening a solved puzzle shares the fix.

### N2 — TEST · Note · `.swdev/verifiers/v01_release_apk.py`
The verifier never ties the APK to the tree. A stale `app-release-unsigned.apk` (built before a dependency or permission change) passes. Test & Verify must rebuild first, as the WO says. A cheap guard would be to refuse an APK older than the newest `*.kt`, `*.kts` or manifest file. The canary and liveness rules themselves are sound, and I have no kit-side objection.

### N3 — TEST · Note · `PreviewKitTest.kt` (REQ-021.A1, kit-side)
The token releases a still finger, so preview and drop agree trivially. The product's `drop` re-searches whenever the centre changed since the last frame's preview. A move batched with the up in one input frame could therefore lock one frame away from the drawn preview. The window is tiny and the lock distance is generous. The corrected check would add one move-then-release-in-the-same-frame case.

### N4 — DIRECTIVE · Note · `PlayDrawing.kt`, `PointerRouter.kt:twistOnly`
- D3: `PlayDrawing.kt` (Slot, SlotLayout, PolygonNode) names a task and G-17 but no REQ.
- D4: `PointerRouter.twistOnly` handles a first-down while `controller.dragged != null`. Every drag starts inside `gesture`, which owns all pointers until the primary lifts. The branch is unreachable from real input and no test covers it. Delete it or cite the case it serves.

### N5 — PROCESS · Note · WO log
- All three corrected checks are kit-fixture defects of one class: a check that asserted or read beyond what the REQ mandates. They were an invented shared-top convention, a read outside the kit's own capture, and an unmandated base-colour share. This is for `proposals.md` as a recurring class (the "assert only what is mandated" line exists in `task-decomposition`).
- Light mode skipped the design review. F1 and F2 (touch-area overlap, one-shot cues across activity recreation) are the lifecycle and geometry class a design review would have asked about. Feed this into the harvest note the WO already plans.
- Staffing: no escalation is warranted, since the UI tasks already ran on the top model.

## Handoff

**Status:** forward (0 Blockers · 4 Shoulds · 5 Notes).
**Blockers:** none.
**Product-side:** F1 (badge area overlap), F2 (one-shot cues replay on recreation), F3 (main-thread bundle parse), F4 (`.gitattributes` for the `.sh`), N1 (solve replay), N4 (no REQ in `PlayDrawing.kt`, dead `twistOnly` branch).
**Kit-side:** N3 (REQ-021.A1 case; plus F1's missing tap pin and F2's missing recreation case if those are fixed).
**Verifier-side:** N2.
**Process:** N5.
**Contract:** frozen files unchanged. **Evidence:** JVM 45 + 2 green, both device kits compile, verifier 3/3 and positive control 2/2, tokens 7 + 29 as expected, G-12/G-14/G-17/G-10/G-08 greps clean. The device suites and the 1.9 s launch figure were not re-run by me; they are Test & Verify's.
