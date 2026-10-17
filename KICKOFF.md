# KICKOFF — TangramPlaytime (the TangramNoAds re-run under SWDev v0.20)

**Why this project exists.** The first run (`C:/GitHub/AI/TangramNoAds`,
SWDev v0.15, 2026-10-02…05) built the game from this exact collection and
was harvested into SWDev v0.16–v0.20 and SwReqCollector v0.5
(`SWDev/project-review-tangramnoads-2026-10.md`). This project is the
verification: the **same locked requirements and the same design inputs**,
built again under the improved frameworks. Do not read the first run's
code, designs, decisions or reviews — comparing comes after, not during.

**What is already in place (scaffold 2026-10-08):**
- `Requirements/` — the locked collection, now v1.2 (47 locked REQs, 4
  withdrawn, 7 TYPEs): the first run's v1.1 unchanged except REQ-048, which
  carries the owner's 2026-10-05 decisions (CHG-001/002, SRC-016); views
  regenerated with SwReqCollector v0.5 (`views/lock-receipt.md` is new).
- `Spec/`, `Study/` (frozen), `Tangrams/` (13 puzzles + schema), `tools/`
  (prototype + puzzle pipeline) — the v1 intake material as of 2026-10-01.
- `AGENTS.md` (the owner's rules 1–13 + the v0.20 build section),
  `.claude/settings.json`, `.claude/agents/` (10 stations incl.
  `wo-planner`), `.swdev/guard.json` (path C, P1 values),
  `governance.md` (ai-mastered, **proposed** — the owner sets it in force),
  `design-inputs.md` (register pre-filled; Contract-delta is P2 work),
  `platform-notes/android.md`, `capture-outbox/`, `owner-evidence.md`,
  `review-notes.md`, and empty `architecture.md`, `build-map.md`,
  `decisions.md`, `tasks.md`, `progress.md`, `proposals.md`.

**Decided already (the owner's, from the first run — SRC-016 / DI-8):**
- **Technology:** Kotlin + Jetpack Compose, native; the prototype's locking
  logic is ported, not re-invented (first run's ADR-001). Row 5 is answered.
- **Work-order order** (your kickoff order; `priority: none` is accepted):
  WO-001 #Locking → WO-002 #Content → WO-003 #Solving → WO-004 #Browsing
  (+ the progress store) → WO-005 #DevTools → WO-006 #Layout + #Language +
  #Promise → WO-007 #Settings → WO-008 #PlayTime → WO-009 #Release. The
  same order as the first run, so the two runs compare WO by WO.
- **Standing answers** to the forks the first run met (channel, puzzle
  count, review marking, time counting, icon) are in DI-8 — the defaults of
  the matching checkpoint items.
- **Two change deltas are applied in the collection** (accepted by the
  owner 2026-10-11): `CHG-001` store title "Tangram Playtime" (the promise
  moves to the short description, REQ-048 A2 rewritten) and `CHG-002` the
  launcher icon in scope (REQ-048 A4, asset DI-9). REQ-048 is re-signed.

**Owner steps before the first session** (each is yours, not the AI's):
1. `governance.md` is **in force** (done 2026-10-08).
2. Commit the scaffold (`git add -A && git commit -m "Scaffold under SWDev v0.20 (TangramNoAds re-run)"`).
3. Make sure the API 26 (minSdk floor) emulator image still exists — it is
   a **G3 precondition** for the first WO with device tests (v0.18).

---

## State after the first session (2026-10-11)

- **G1 accepted** (Jami, 2026-10-11): collection v1.2 binds this project.
  `Requirements/reviews/review-01.md` carries all 38 verdicts; 24 chg-drafts
  + 1 src-draft wait in `capture-outbox/` for a capture session (lock behind
  build). Owner decisions taken on the G1 surface are in
  `capture-outbox/src-draft-g1-owner-answers.md` — read them: **English is
  the Contract language (Finnish = reference locale)**, Google Play, Android
  8.0 floor, licence line in Settings, the shared-type numbers confirmed.
- **P2 artefacts:** `architecture.md` (G-01..G-17, O-01..O-08, registry:
  `IPuzzleLibrary` locked; `ILockEngine`, `IProgressStore`, contract types
  notify), `build-map.md` (six Gradle modules), `design-inputs.md` §2 (12
  superseded statements, no open forks), `decisions.md` rows of 2026-10-11.
  G2 status: see `architecture.md` §7 and the last `progress.md` entry.

## State after WO-005 (2026-10-17, closed 13:13)

- **WO-005 #DevTools is done** (`workorders/WO-005.md`, Light).
  - **The DEV aid, debug builds only:**
    - a small dashed "DEV" pill at the board's bottom-left, hidden on a solved puzzle;
    - the passcode box over the board (the top bar visible, inert): "Wrong passcode." for anything but 0417, the empty entry too; unlocked until the app restarts;
    - "Show the solution": each piece's place, see-through in its colour with its name, under the placed pieces, on while you browse until "Hide the solution";
    - "Solve this puzzle now": the pieces placed through the controller's own actions, then the normal win; the best time stays "–"; a half-done puzzle is cleared first.
  - **How it stays out of release:** all of it lives in `play/src/debug`; `src/main` holds one empty door, `DevAidHook`, which finds the aid with `ServiceLoader` (null in release). The aid's strings live in `play/src/debug/res`.
  - **REQ-046.A4** is proven by `ReleaseBuildKitTest`, a JVM test reading both APKs (five markers: the aid's package, resource names, service entry, "Wrong passcode.", "0417"; the debug APK is its control). **The JVM line is two Gradle runs**: build and repackage the APKs, then the tests (a measured race otherwise).
  - **Results:** 4/4 IDs green, every set on both images from the final tree: JVM 174; play device set 94/0 on the dev and floor images; app set 7 + 2 guarded skips on each; V-01. trace-check `--wo` GREEN; verifiers GREEN within budget. No line is `implemented (unverified)`.
  - **Code review:** 0 B / 1 S / 3 N; three deliberate breaks all caught. Corrected checks 2 (a device pin's tap point — the implementer won the contest; the release scan widened to content markers). Product defects found 0.
- **Carry into WO-006 #Layout + #Language + #Promise:**
  - REQ-037's touch-size sweep skips the `dev-*` nodes (REQ-037's own rule exempts the #DevTools controls).
  - The number keyboard pushes the passcode box up while it is open (seen in the debug app; `safeDrawingPadding`).
  - From WO-004: the grid cells and the scrim have no screen-reader labels; the ‹ › 52 dp touch sweep.
  - From WO-003: the title language follows the primary locale, while strings follow the whole locale list (`decisions.md` 2026-10-12, TASK-015 row).
- **Carry into later WOs:**
  - **WO-007:** an aid solve fires the normal `PickedUp` / `Turned` / `Locked` events in one burst — decide how sounds handle it.
  - **WO-008 must-dos:** an aid solve never sets a best time (the REQ-046.A3 tests are the tripwire: the aid leaves time 0, so a naive rule would show "0:00"); and, from WO-004, which side guarantees a time the store's reader rejects is never written.
  - **Next G2-route touch of `architecture.md`:** a one-line O-06 exception for the aid's strings (code review N2; recorded in `decisions.md` 2026-10-17 "The DEV aid's strings live with the aid").
- **Checkpoint items 1–4** (the DEV pill · "solve now" plays the normal win · the overlay under the pieces · the passcode box and how long it stays unlocked): asked 2026-10-17, **defaults stand 2026-10-20** (`workorders/WO-005.md` § Checkpoint items).
- **Lock behind build:** 26 outbox drafts await a capture session; this WO wrote and amended none. REQ-038.A1, REQ-019.A1–A4, REQ-002.A1 and REQ-014.A1 stay unverified as before.
- **Owner steps:**
  - Commit the WO-005 snapshot (prompted on the G4 surface).
  - **This is the 1-in-5 spot-check WO:** play a debug build (`review-notes.md` 2026-10-17, "Try the DEV aid") and, if you like, look at the diff of `play/src/debug/.../devaid/` and `DevAidHook.kt`.
  - Answer checkpoint items 1–4 if a default is wrong ("Q2 no" is enough).
  - Run a capture session on `capture-outbox/` when convenient.

## Paste this to start the WO-006 session (one orchestrator context per WO)

```text
WE ARE IN PHASE P3 — WORK ORDER WO-006 #Layout + #Language + #Promise
(TangramPlaytime, SWDev v0.20, intake path C). Read AGENTS.md first (incl. the
"from P3 on" framework files), then governance.md, architecture.md (v1.1),
build-map.md (v1.5), design-inputs.md §2, decisions.md (incl. the WO-005 rows
of 2026-10-17), the last progress.md entry, KICKOFF.md "State after WO-005",
capture-outbox/ (the REQ-006, REQ-047, REQ-048 and licence drafts),
platform-notes/android.md (incl. the WO-005 lesson lines).
WO-006 scope: #Layout (REQ-006) — #PhoneLayout (REQ-035), #TabletLayout
(REQ-036), #TouchTargets (REQ-037); #Language (REQ-047); #Promise (REQ-001)
with #NoMoney (REQ-008, REQ-009) and #DeviceOnly (REQ-010) — the TYPE-007
window class and the phone/tablet layouts in :app (O-05), insets, back
handling, the touch-target sweep (the dev-* nodes exempt by REQ-037's rule),
the en/fi resources (English Contract, Finnish reference), the promise lines.
REQ-009's free note sits on the settings screen (WO-007's) — say where it
lands. Carried: the grid's screen-reader labels; the ‹ › 52 dp sweep; the
keyboard under the DEV passcode box; the locale-list mix (KICKOFF).
Mode: decide from the four reasons — Light unless one is written in the WO.
Verification channels at G3: JVM where the logic is pure; the dev image
Medium_Phone_API_37.0 AND the floor image Phone_API_26 (one emulator at a
time) — and a tablet image if a REQ needs one (prove it reachable at G3 or
record a waiver); the release verifier V-01 + positive control; the JVM line
is two Gradle runs (APKs, then tests). Device runs are judged by their XML.
Checkpoint candidates (≤5, product-visible). Strings to review-notes.md.
Stations as subagents; the four scripts are duties. End every turn with the
one signal.
```

## State after WO-004 (2026-10-17, closed 09:25)

- **WO-004 #Browsing + `:store` is done** (`workorders/WO-004.md`, Full (c)).
  - **Browsing on the Play screen (`:play`):**
    - the top bar: ‹ › (wrapping) and the counter (name, rating dots, "n / N", state word);
    - a long press on › jumps to the next unsolved puzzle;
    - Restart, shown only while a puzzle is In progress;
    - the Retry · best time · Next row, shown once the picture has faded in;
    - the grid overview below the bar, with the bar inert while it is open;
    - the loading paper.
  - **The celebration is a consumed controller cue** (WO-003 N1 closed).
  - **`:store`** implements `IProgressStore` as ADR-003: one JSON document `noBackupFilesDir/progress.json`, rewritten atomically, any unreadable state = a fresh install. **ADR-003 is now in `architecture.md` v1.1** (the G2 route, row 4). `PlayController` saves through `Persistence` (the cache + one FIFO writer).
  - **`:app`** loads the bundle and the store off the main thread (WO-003 F3 closed). Back closes the grid.
  - **`tools/process_death_check.sh`** proves the board survives a real process death. It refuses stale APKs, so run it after the forced Gradle line in the briefs' channel table: `:app:packageDebug --rerun :app:packageDebugAndroidTest --rerun`.
  - **Results:** 16/16 IDs green, every set on both images from the final tree:
    - JVM 157;
    - play device set 79/0 on the dev and floor images;
    - app set 5 + 2 guarded skips on each image;
    - the death pair with its control and negative run on each image;
    - V-01.
  - trace-check `--wo` GREEN; verifiers GREEN within budget. No line is `implemented (unverified)`.
  - **Code review:** 0 B / 2 S / 5 N; two deliberate breaks of the code were both caught by the kit. Corrected checks 2; product defects found 0.
- **Carry into WO-005 #DevTools:**
  - REQ-046's aid touches WO-004's screen. Its overlay "stays on while the player browses" (‹ ›, the long press, the grid), the DEV button is hidden on a Solved puzzle, and "solve now" must go through the controller and `Persistence`: state Solved, no best time.
  - The hit order (S-22) gains the DEV button.
  - Pending draft: `chg-draft-req046-android-build-terms.md` (what "prototype / test build" means on Android).
  - Release safety: V-01's canary check already proves `play/src/debug` is absent from release (REQ-046.A4's reading is the WO's to state).
- **Carry into later WOs:**
  - **WO-008 must-do** (`decisions.md` 2026-10-17, code review S1): its design states which side guarantees that a time the store's reader rejects (a negative time after a clock change) is never written. Otherwise the next launch reads the whole file as a fresh install and the first save erases every puzzle's progress.
  - **WO-006:** the grid cells and the scrim have no screen-reader labels; the ‹ › 52 dp touch sweep (REQ-037).
  - Retire `PlayController`'s memory-only constructor when the WO-003 kits move to a store double (code review N3).
  - `ILockEngine.stillValid` sufficed for REQ-025 (WO-001 N3 closed, `decisions.md` 2026-10-15 "REQ-025's load check").
- **Checkpoint items 1–5** (Restart one press · the solved row after the picture · best time "–" · the middle of the bar opens the grid · an emptied board stays In progress): **answered by the owner 2026-10-17, every default confirmed** (`decisions.md` row 17; `capture-outbox/src-draft-wo004-checkpoint-answers.md`).
- **Lock behind build:** 26 outbox drafts await a capture session. This WO wrote one (`src-draft-wo004-checkpoint-answers.md`, after G4) and amended none. REQ-038.A1 and REQ-019.A1–A4 stay unverified as before, and REQ-002.A1 and REQ-014.A1 too.
- **Owner steps:**
  - Commit the WO-004 snapshot (prompted on the G4 surface).
  - **Play the build** (`review-notes.md` 2026-10-17: browsing, the grid, Restart, the solved row, close and reopen).
  - Run a capture session on `capture-outbox/` when convenient.

## Paste this to start the WO-005 session (one orchestrator context per WO)

```text
WE ARE IN PHASE P3 — WORK ORDER WO-005 #DevTools (TangramPlaytime, SWDev
v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
files), then governance.md, architecture.md (v1.1), build-map.md (v1.4),
design-inputs.md §2, decisions.md (incl. the WO-004 rows of 2026-10-15 and
2026-10-17), the last progress.md entry, KICKOFF.md "State after WO-004",
capture-outbox/chg-draft-req046-android-build-terms.md and
chg-draft-type006-missing-transitions.md (its "Solve this puzzle now" bullet),
platform-notes/android.md (incl. the WO-004 lesson lines).
WO-005 scope: #DevTools — #SolutionReveal (REQ-046 A1..A4): the passcode-
protected DEV aid in play/src/debug (a debug-only source set; absent from the
release build, G-08), drawn on the WO-004 Play screen: the DEV button, the
passcode prompt, "show the solution" (an overlay that stays on while browsing)
and "solve this puzzle now" (Solved through the controller and Persistence,
no best time); the DEV button hidden on a Solved puzzle; its place in the S-22
hit order. Depends on REQ-022 (built, WO-001).
Mode: decide from the four reasons (no new module — play/src/debug exists; no
governed interface changed; no persistence format change) — Light unless one
reason is written in the WO.
Verification channels at G3: JVM where the logic is pure; the dev image
Medium_Phone_API_37.0 AND the floor image Phone_API_26 (one emulator at a
time); the release verifier V-01 + positive control (A4: the release has no
DEV button). Device runs are judged by their XML, never by the exit code.
Checkpoint candidates (≤5, product-visible): the DEV button's place and look
(REQ-046 ASSUMPTION: bottom-left, small, dashed). Strings to review-notes.md.
Stations as subagents; the four scripts are duties. End every turn with the
one signal.
```

## State after WO-003 (2026-10-12, closed 20:43)

- **WO-003 #Solving is done** (`workorders/WO-003.md`, Light).
  - **The Play screen in `:play` (Compose):**
    - the flat silhouette board, drawn in px (G-17);
    - the fixed tray with S/M/L marks and ghost outlines;
    - the flip badge in the tray and next to a placed parallelogram;
    - the drag (grow, 30 dp float) with its landing preview;
    - tap and two-finger twist turns, the mirror, and the 180 ms lock and go-home glides;
    - the mini's corner pulse;
    - the solve: a size-neutral pop, confetti, and the picture fading in, clipped to the silhouette.
  - **The state holder:** `PlayController` keeps every puzzle's state in memory behind S-1…S-3, so WO-004 swaps the memory for `IProgressStore`.
  - **The `:app` wiring:** `AppGraph` builds `LockEngine()` and `PuzzleLibraries.bundled(engine)` once per process. The bundle is read on the device on both images, which closes WO-002 N4.
  - **Release safety:** the verifier `.swdev/verifiers/v01_release_apk.py` (no permission, no debug canary, no `java.net`/okhttp from product classes), with a positive control. The canary is `play/src/debug/.../DebugCanary.kt`; `tools/device_reset.sh` resets the device.
  - **Results:** 32/32 IDs green on JVM + dev image + floor image. Trace-check `--wo` GREEN; verifiers GREEN within budget.
  - **The code review found five product defects after the kit was green, all fixed with red-then-green kit cases:**
    - the badge's touch area;
    - the pulse and shake replaying on recreation;
    - a moving release locking off the drawn preview;
    - the parallelogram made un-turnable by the first badge fix;
    - a cut-short shake leaving a piece off its place.
  - **Corrected checks:** 6, all kit-side (`proposals.md` 2026-10-12).
- **Carry into WO-004 #Browsing (+ `store/`):**
  - **Load off the main thread.** `AppGraph` parses the bundle in the first composition: cold launch 1.7–2.3 s on the dev image, 0.7–0.8 s on the floor image. WO-004 adds the `IProgressStore` read at the same point, so load both off the main thread and compose the screen once the controller exists (`decisions.md` 2026-10-12, F3).
  - **The celebration's trigger.** `SolveDriver` holds one "unsolved seen" id, so solve A, solve B, open A, open B replays B's celebration, and a solved puzzle shows one frame of coloured pieces. This becomes live once ‹ › and the grid call `open()`. Use a controller-held "just solved" cue, consumed once like the pulse and shake (`ShownCues`) (N1, same row).
  - **The REQ-045.A1 screen half** ("a fresh install opens on …") and REQ-040.A1, REQ-041.A1 are re-covered with navigation.
  - **The `ILockEngine.stillValid` scope question** (WO-001) for REQ-025 stands.
  - **Pending drafts on REQ-025:** `chg-draft-req025-drop-profile-storage-rule.md`, `chg-draft-req025-unreadable-state-and-save-timing.md`, `chg-draft-type006-missing-transitions.md`.
  - **Behaviours to keep:**
    - `onStop` sends a running drag home (also on a rotation);
    - twist steps are netted per frame, which matters for WO-007's `Turned` sound;
    - the touch order "the visible thing wins" (`decisions.md` 2026-10-12).
- **Lock behind build:**
  - 25 outbox drafts still await a capture session. This WO amended one: `chg-draft-req018-badge-touch-precedence.md`, now covering the board, with its tray sentence aligned.
  - REQ-002.A1 and REQ-014.A1 stay `implemented (unverified)` until `chg-draft-req002-a1-mirror.md` and `chg-draft-req014-a1-without-delay.md` are signed. REQ-038.A1 and REQ-019.A1–A4 stay unverified as before.
- **Owner steps:**
  - Commit the WO-003 snapshot (prompted on the G4 surface).
  - **Play the build on a phone.** `review-notes.md` lists what to look at: the board colours (checkpoint item 1, whose default stands 2026-10-14), flat pieces without the white edge, the pulse on every go-home in a mini, the badge touch, the S/M/L marks.
  - Run a capture session on `capture-outbox/` when convenient.

## Paste this to start the WO-004 session (one orchestrator context per WO)

```text
WE ARE IN PHASE P3 — WORK ORDER WO-004 #Browsing + store/ (TangramPlaytime,
SWDev v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on"
framework files), then governance.md, architecture.md, build-map.md,
design-inputs.md §2, decisions.md (incl. the WO-003 rows), the last
progress.md entry, KICKOFF.md "State after WO-003", capture-outbox/ (the
REQ-025 and TYPE-006 drafts), platform-notes/android.md (incl. the WO-003
lesson lines in § Compose test kit).
WO-004 scope: #Browsing — #PuzzleNav (REQ-024), #ResumePuzzle (REQ-025),
#SolvedView (REQ-026), #PuzzleOverview (REQ-050), the parent REQ-003 — on
the Play screen of :play (the top bar's ‹ › and counter, Restart / Retry,
the grid), plus the store/ module implementing IProgressStore (notify tier)
so PlayController's in-memory state (S-1…S-3) is persisted, and the
re-covered screen halves of REQ-040.A1, REQ-041.A1, REQ-045.A1. Carried from
WO-003: load the bundle and the store off the main thread (F3); the
celebration's trigger as a consumed controller cue (N1). Timers are WO-008's
(REQ-030 stays out).
Mode: Full, reason (c) a new persistence format (build-map.md: store/ is
built in WO-004) — design-author → design-reviewer first. The design names
the touch geometry and what survives an activity recreation (proposals.md
2026-10-12).
Verification channels at G3: JVM (store/, the controller), the dev image
Medium_Phone_API_37.0 AND the floor image Phone_API_26 (one emulator at a
time; both proven in WO-003), the release verifier V-01 with its positive
control; a migration/persistence test per governance row 11.
Checkpoint candidates (≤5, product-visible). Stations as subagents; the four
scripts are duties. End every turn with the one signal.
```

## WO-004 — PAUSED (2026-10-15 21:32; the owner: "plz dont start new tuff anymore after this implementer.. time to keep break") — DONE 2026-10-17 (resumed 06:46 in a fresh Opus context, closed 09:25)

**Where WO-004 #Browsing + `:store` stands (P4, dispatch step 4 of 7 done):**
- **G3 approved** 2026-10-15 19:52, Full (c). Design `designs/WO-004-design.md` (Reviewed; design review 0 B / 5 S), plan `tasks.md` § WO-004 (TASK-016…022), briefs `workorders/WO-004-briefs.md` (the dispatch-order and channel tables at the top; one brief per task).
- **Tasks:** TASK-016 (JVM kit) done · TASK-017 (`:store` file store) in review · TASK-018 (`Persistence` + controller) in review · TASK-019 (device kit) done · TASK-020 (top bar, Restart, solved row, long press, loading paper) in review · **TASK-021 (the grid overview) and TASK-022 (`:app` wiring + `tools/process_death_check.sh`) not started.**
- **Evidence** (`.swdev/run-log/WO-004.md`): JVM green (r1 20:48; i20 21:29: store 12, play 79, content 64, contracts 2); dev image: ResumeScreen 5/0, SolvedView 5/0, the six WO-003 classes 51/0, TopBar 12/1 (i20). Floor image not yet run on WO-004 code (TASK-021, TASK-022, Test & Verify).
- **One open kit finding, not yet routed:** `TopBarKitTest.aPieceDrawnOverTheRestartPillWinsTheTouchAndThePillBeatsAPiecesMargin` (untokened, S-22 hit order 3) skips by `assumeTrue` on the dev screen — no bundled puzzle draws a placed piece over the pill there, so the pin cannot fail where it runs (the connected XML records the skip as a `<failure>`). Route it to the TASK-019 kit author (fresh context: the earlier one lives in this ended session) for a constructed case or an honest placement of the pin — a corrected check if the kit changes — **before** TASK-021.
- **Corrected checks so far: 1** (design review F5(b) reversed on trace-check's code). Disputes 0. Recirculations: design-review → design author ×1 (+ two write-backs), plan-review → planner ×1. Hard-stops 0.
- **Checkpoint items 1–5** asked 2026-10-15, defaults stand 2026-10-18 (`workorders/WO-004.md` § Checkpoint items).
- **Kit hashes at landing** (an implementer never edits these; recompute at every hand-back): WO-004 log 20:23 (JVM kit) and 20:49 (device kit).
- **Emulator:** `Medium_Phone_API_37.0` was running as `emulator-5554` at the pause (it may be gone — `adb devices`, reboot headless). One at a time.
- **Owner steps:** none required. A snapshot commit of the mid-WO tree is optional (nothing is closed; the kit's red window is closed for `:play`, still open for `:app` androidTest until TASK-022).

**Paste this to resume WO-004 (fresh orchestrator context):**

```text
WE ARE IN PHASE P4 — RESUMING WORK ORDER WO-004 #Browsing + store/ (TangramPlaytime,
SWDev v0.20, intake path C) in a fresh orchestrator context. Read AGENTS.md first
(incl. the "from P3 on" framework files), then KICKOFF.md "WO-004 — PAUSED",
workorders/WO-004.md (§ Log is the narrative), tasks.md § WO-004,
workorders/WO-004-briefs.md (the two tables at the top), .swdev/run-log/WO-004.md,
decisions.md (the 2026-10-15 rows), the last progress.md entry, review-notes.md,
platform-notes/android.md.
State: TASK-016/019 done; TASK-017/018/020 in review; TASK-021 (overview) and
TASK-022 (:app wiring + the process-death check) not started. First: route the
open kit finding (TopBarKitTest hit-order-3 pin skips on the dev screen) to a
fresh acceptance-test-author with its TASK-019 brief + the finding; then TASK-021,
then TASK-022, each from its brief (slice-implementer, Opus), recomputing the kit
hashes at every hand-back and logging every hop with `date` stamps. Then
plan-check again (kit compiled), code-reviewer (Sonnet, fresh), test-verify
(Sonnet: every set on BOTH images from the final tree, the death pair + its
positive control on both, V-01 + positive control), traceability-auditor
(PYTHONUTF8=1 trace_check.py --wo WO-004, 16 IDs), and the close (run_verifiers
--wo, wo_metrics --wo --write, ledger, Contract deltas, ADR-003 via the G2 route,
G4 under row 9, KICKOFF "State after WO-004" + the WO-005 brief, AGENTS.md phase
line, proposals.md, progress.md, the snapshot prompt). Stations as subagents. End
every turn with the one signal.
```

## WO-003 — MID-WO HAND-OVER (2026-10-12 18:28; the owner moved the orchestrator off Fable for credits) — DONE 2026-10-12 (resumed 18:30 in an Opus context, closed 20:43)

**Where WO-003 #Solving stands (P5 — code review running):**
- **G3 approved** 2026-10-11 17:17 (Light; `workorders/WO-003.md`, `tasks.md` § WO-003 — the planner's Seams S-1…S-16, tag table and touch routing are the design).
- **Every task landed:** TASK-009 (JVM kit) done · TASK-011 (device kit) done · TASK-010, 012, 013, 014, 015 **in review**. Product: `play/src/main` 23 files (~2,800 LOC) + `play/src/debug/DebugCanary.kt` + `app/src/main` (`AppGraph`, `PlayLayouts`, `MainActivity`, `size_mark_*` strings en/fi); `.swdev/verifiers/v01_release_apk.py` + `manifest.json`; `tools/device_reset.sh`.
- **Evidence so far** (`.swdev/run-log/WO-003.md`): orchestrator run **r2 (18:25)** — JVM `:play` 45 + `:contracts` 2 green, both device-kit APKs + the release APK built, plan-check GREEN with the kit compiled, `run_verifiers --wo WO-003` GREEN (V-01 3/3, positive control 2/2, within budget), tokens 7 (JVM) + 29 (device, REQ-045.A1 twice), G-12/G-14/G-08 greps clean. Device: the whole play set ran **44/44 by composition** — 43 on **both** images at i14 (07:00 dev, 07:09 floor) + `SolvedPictureKitTest` 6/0 after the kit fix (k14, dev only); `AppLaunchKitTest` + `FloorChannelTest` 2/0 on the dev image (i15). **Test & Verify must still run every set on both images from the final tree** (hashes in r2: test `6f2ef86c6d15`, product `1969f967fddb`).
- **Corrected checks: 3** (all kit-side, zero product change: tray-row "shared top" pin; the sampler reading outside its capture; the "≥ 1 % base colour visible" assertion). Recirculations: kit author ×3 (resumed context). Hard-stops 0. Disputes 0. Two agents died of the API session limit (TASK-013 first attempt: nothing written; TASK-014: resumed with context intact).
- **Unverified by plan:** REQ-002.A1 (TASK-010) and REQ-014.A1 (TASK-013) → `implemented (unverified)` for `chg-draft-req002-a1-mirror.md` / `chg-draft-req014-a1-without-delay.md` (tests green).
- **Contract deltas:** `build-map.md` v1.3 (#GoHome / #PuzzleArt / #PuzzleMini → `play/`, #Solving In progress) — baseline re-freeze pending at the next `trace_check.py --wo WO-003` run. Contract files unchanged.
- **Checkpoint item 1** (board colours, default DI-2 palette) asked 2026-10-11, default stands 2026-10-14. Owner-grade rows: flat pieces without the white edge; the pulse on every go-home release in a mini (`review-notes.md`).
- **Emulator:** `Medium_Phone_API_37.0` running as `emulator-5554` at hand-over (may be gone by the next session — `adb devices`, reboot headless per `platform-notes/android.md`). One at a time.
- **Known tool facts:** `-Pandroid.testInstrumentationRunnerArguments.class=` takes one class per `cmd.exe` invocation (a comma list runs only the first); omit it for a whole set. `PYTHONUTF8=1` for trace-check. The connected-test XML folder is overwritten per run — copy XMLs you need to keep.

**Paste this to resume WO-003 (fresh orchestrator context, any model):**

```text
WE ARE IN PHASE P5→P6 — RESUMING WORK ORDER WO-003 #Solving (TangramPlaytime,
SWDev v0.20, intake path C) in a fresh orchestrator context. Read AGENTS.md
first (incl. the "from P3 on" framework files), then KICKOFF.md "WO-003 — MID-WO
HAND-OVER", workorders/WO-003.md (§ Log is the narrative), tasks.md § WO-003,
.swdev/run-log/WO-003.md, decisions.md (the WO-003 rows), the last progress.md
entries, review-notes.md, platform-notes/android.md.
State: every task landed; TASK-010/012/013/014/015 in review; a code-reviewer
(Sonnet) was dispatched 18:26 on 2026-10-12 — its report is
reviews/WO-003-code-review.md (read it; if absent, dispatch code-reviewer with
the brief shape of the WO-002 one: fresh eyes on the whole diff, substance audit
per token, re-derive by running). Then: triage findings (Blockers → the owning
implementer or kit author; kit fixes count as corrected checks, now 3), log
every hop in the WO log with `date` stamps, keep tasks.md statuses one word.
Then P6: run plan-check once more; dispatch test-verify (Sonnet) with the run
log as baseline — it must run every set on BOTH images from the final tree
(JVM; :play:connectedDebugAndroidTest whole set; :app:connectedDebugAndroidTest;
reset script before/--check after; the release verifier + positive control on a
fresh debug APK) — then traceability-auditor (Sonnet): PYTHONUTF8=1
trace_check.py --wo WO-003 must be GREEN (32 IDs; it re-freezes the build-map
v1.3 delta). Close: run_verifiers.py --wo, wo_metrics.py --wo --write (fill
judgment rows by hand; windows absorb the two calendar days), DoD ticks,
Station ledger, Metrics, Contract deltas, G4 under governance row 9 (decisions
row), build-map #Solving/#GoHome/#PuzzleArt/#PuzzleMini → Built, proposals.md
(3 corrected checks, the comma-list channel fact, the API-limit kills, the
Light-mode harvest note), review-notes.md, KICKOFF "State after WO-003" + the
WO-004 brief, AGENTS.md phase line, progress.md, the snapshot prompt. Stations
as subagents (owner staffing: Opus/Sonnet; orchestrator model is the owner's
choice today). End every turn with the one signal.
```

## State after WO-002 (2026-10-11, closed 15:45)

- **WO-002 #Content done** (`workorders/WO-002.md`, Full (b)): `:content` has
  `BundledPuzzles` (the 13 `Tangrams/*.json` bundled as **Java resources**
  by a Gradle task — `content/build/generated/puzzles/`, read through one
  `ClassLoader` call on JVM and device), `PuzzleParser`/`PuzzleDtos`,
  `PuzzleLoader` (15 `Rule`s, sealed `LoadResult`), `ValidityCheck` + `Edges`
  (congruence and overlap through `ILockEngine`), `PuzzleLibrary` and the
  entry point **`PuzzleLibraries.bundled(engine: ILockEngine): IPuzzleLibrary`**
  (fails fast on a bad bundled file). Kit 16 + 37 tests, 10/10 IDs green;
  trace-check `--wo` GREEN; code review 0 B. `IPuzzleLibrary.kt`, the schema
  and `Tangrams/` unchanged; one notify delta (`build-map.md` v1.2: #Content
  depends on the kernel via `ILockEngine`). `tools/tangram_geom.outline_corners`
  now uses the contiguity rule (warmup-4 gains (2,2); `tools/tests/test_tangram_geom.py`).
- **Carry into WO-003 #Solving (first WO with device tests):** wire
  `PuzzleLibraries.bundled(LockEngine())` in `:app` (design review F4) and
  call it once on the **floor image** `Phone_API_26` — the bundle read is
  proven on the JVM and by APK/AAR listings only (code review N4; check R8
  keeps the resources in the release build). Carried acceptance lines:
  **REQ-020.A2** (no error message for a missed drop), **REQ-039.A1** (no
  picture paints outside its outline — the solved-picture drawing clips),
  **REQ-045.A2** (placing the three mini pieces shows the solved picture).
  Add a `build-map.md` row for the rendering half of #PuzzleArt (code home
  `play/`) as WO-001 did for #SolvedCheck, or trace-check reads the play
  test as directory theater. Doc drift in the locked `architecture.md`
  (code review N5): §3 says "assets" (they are Java resources) and G-15
  names `LoadResult.Unreadable` (the real type is `LoadResult.Rejected`) —
  the owner may fix the wording; not a Contract change.
- **Carry into WO-004 #Browsing:** REQ-040.A1, REQ-041.A1, REQ-045.A1 are
  proven at the seam; the screen half ("a fresh install opens on …") is
  re-covered there. `ILockEngine.stillValid` scope question from WO-001 stands.
- **Content batch WO (Light, after WO-003, before WO-009):** author new
  puzzles to the owner's first-release count (checkpoint item 1 — default
  **25**, stands 2026-10-14); the validator's V3 (vertex-set equality) is
  looser than the Kotlin congruence (boundary order, 45° turns) and the
  `rot/flip/at` form is rejected by the app (`PLACEMENT`) — author puzzles
  in the `polygon` form and run both checks. REQ-042.A1 / REQ-039.A2 are
  WO-009's with the owner's review marks.
- **Lock behind build:** 25 outbox drafts still await a capture session;
  REQ-038.A1 (and A2) stay `implemented (unverified)` until
  `chg-draft-req038-validity-rules-in-contract.md` is signed; REQ-019.A1–A4
  until the TYPE-004 dictionary draft is.
- **Owner steps:** commit the WO-002 snapshot (prompted on the G4 surface);
  answer checkpoint item 1 if 25 is not the number; run a capture session on
  `capture-outbox/` when convenient; look at `review-notes.md` (the 13
  puzzles and their Finnish reference titles).

## Paste this to start the WO-003 session (one orchestrator context per WO)

```text
WE ARE IN PHASE P3 — WORK ORDER WO-003 #Solving (TangramPlaytime, SWDev
v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
files), then governance.md, architecture.md, build-map.md, design-inputs.md
§2, decisions.md, the last progress.md entry, KICKOFF.md "State after
WO-002", capture-outbox/ (pending deltas), platform-notes/android.md.
WO-003 scope: the #Solving subtree not yet built — #Board, #Tray, #SizeMarks,
#Drag, #TapTurn, #TwistTurn, #Flip, #LandingPreview, #CornerTeach,
#SolvedPicture (the REQ ids those tags carry in Requirements/features.md;
#Locking and #SolvedCheck are built, #TurnButtons is withdrawn) — the Play
screen in :play (Compose) consuming ILockEngine (:kernel) and IPuzzleLibrary
(:content, PuzzleLibraries.bundled wired in :app, design review F4) — plus
the carried lines REQ-020.A2, REQ-039.A1, REQ-045.A2 (KICKOFF "State after
WO-002"). Persistence (IProgressStore) is WO-004's: the screen holds its
state in memory this WO, behind the seam. Design inputs: DI-1 (prototype,
behaviour reference only), DI-2 (Spec/02 layout), DI-3 (Spec/01 gameplay),
DI-4 (Study/06, frozen), DI-9 not yet. Mode: decide from the four reasons
(:play is a P2 skeleton like :content was; a governed interface is
consumed, not implemented — say which reason, or Light).
Verification channels at G3: Compose/device tests on the dev image
Medium_Phone_API_37.0 AND the floor image Phone_API_26 (the floor is a G3
precondition for this first device WO — boot it, run the trivial test,
record it), one emulator at a time; JVM tests where the logic is pure;
the release verifier (no debug tooling, canary) arrives with this WO per
platform-notes/android.md. Draw paths in px (G-17). Checkpoint candidates
(≤5, product-visible): colours and the size-mark glyphs only if a REQ leaves
them open; strings to review-notes.md. Stations as subagents; the four
scripts are duties. End every turn with the one signal.
```

## State after WO-001 (2026-10-11, closed 13:04)

- **WO-001 #Locking done** (`workorders/WO-001.md`, Full (b)): `:kernel` has
  `PieceShapes`, `Polygons`, `Silhouette`, `LockRules` and `LockEngine :
  ILockEngine` (all 7 members, 523 LOC); kit `LockEngineKitTest` (14) +
  `GeometryKitTest` (20); 6/6 in-scope IDs green on the JVM channel;
  trace-check `--wo` GREEN; code review 0 B. `ILockEngine.kt` / `Types.kt`
  unchanged; one notify delta on `build-map.md` (`#SolvedCheck` → `kernel/`).
- **Carry into later WOs:** REQ-020.A2 ("no error message for a missed
  drop") is **WO-003's** (seam oracle dishonest; the Play screen proves it).
  WO-002 #Content re-judges `tools/tangram_geom.outline_corners` and the V11
  build-order line: the angle-sum corner rule misses two-wedge corners
  (`shapes-warmup-4` (2,2)); the kernel uses the contiguity rule
  (`design-inputs.md` §2 item 13). WO-004: `ILockEngine.stillValid` checks
  inside + no-overlap only (per KDoc) — decide if REQ-025 needs more.
- **Lock behind build:** 25 outbox drafts still await a capture session;
  REQ-019.A1–A4 stay `implemented (unverified)` until
  `chg-draft-lock-type-dictionary.md` (TYPE-004 values) is signed over there.
- **Owner steps:** commit the WO-001 snapshot (prompted on the G4 surface);
  optionally review the diff (`decisions.md` G4 row recommends it for this
  first WO); run a capture session on `capture-outbox/` when convenient.

## Paste this to start the WO-002 session (one orchestrator context per WO)

```text
WE ARE IN PHASE P3 — WORK ORDER WO-002 #Content (TangramPlaytime, SWDev
v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
files), then governance.md, architecture.md, build-map.md, design-inputs.md
§2 (incl. item 13), decisions.md, the last progress.md entry, KICKOFF.md
"State after WO-001", capture-outbox/ (pending deltas), platform-notes/android.md.
WO-002 scope: #Content subtree (REQ-007, 038, 039, 040, 041, 042, 045) in
:content behind IPuzzleLibrary (locked tier — implement, never change):
Tangrams/*.json as assets, the tangram-puzzle/1 parser (kotlinx.serialization,
ADR-004), the load-time validity check (REQ-038 rules; pending
chg-draft-req038-validity-rules-in-contract.md), ordering kind → rating → id.
Design inputs: DI-5 (puzzle format, schema, the 13 puzzles, tools/). Also
re-judge tools/tangram_geom.outline_corners + V11 against the contiguity
corner rule (design-inputs.md §2 item 13) — a project-tool fix, not Contract.
Mode: decide from the four reasons (a new Android library module with assets
is reason (a) if :content counts as new; its skeleton exists from P2 — say so).
Verification channels at G3: JVM unit tests (:content testDebugUnitTest loads
every asset); device channels only if a task needs one — say so in the WO.
Checkpoint candidates (≤5, product-visible): the puzzle count for the first
release (DI-8 default 25, REQ-042 says ≥ 20). Stations as subagents; the four
scripts are duties. End every turn with the one signal.
```

## Paste this to start the WO-001 session (one orchestrator context per WO) — DONE 2026-10-11

```text
WE ARE IN PHASE P3 — WORK ORDER WO-001 #Locking (TangramPlaytime, SWDev
v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
files), then governance.md, architecture.md, build-map.md, design-inputs.md
§2, decisions.md, the last progress.md entry, capture-outbox/ (the pending
deltas and the owner's G1 answers), platform-notes/android.md.
WO-001 scope: #Locking subtree (#AnchorLock REQ-019, #GoHome REQ-020,
#SolvedCheck REQ-022) plus the geometry rules of TYPE-001/003/004 in
:kernel behind ILockEngine (:contracts) — the prototype's findLock/validAt/
anchors ported per ADR-001, following the TYPE-004 text. Pure JVM; no UI.
Mode: Full, reason (b) a governed interface touched (ILockEngine is
implemented; its shape may change — notify tier, log the contract-delta).
Design inputs: DI-1 (prototype template, behaviour reference only), DI-4
(Study/06, frozen), DI-5 (tools/tangram_geom.py reference math).
Verification channels at G3: JVM unit tests (:kernel, :contracts); the
device channels are not needed for this WO (no UI) — say so in the WO.
Stations as subagents: design-author → design-reviewer → wo-planner →
plan-reviewer → [acceptance-test-author ∥ slice-implementer] → code-reviewer
→ test-verify → traceability-auditor. The four scripts are duties. ≤5
checkpoint items, product-visible only. End every turn with the one signal.
```

```text
WE ARE IN PHASE P1 — ACCEPTANCE OF A LOCKED COLLECTION (intake path C,
C:\GitHub\AI\SWDev\initnewproject.md §2b, SWDev v0.20). Read AGENTS.md
first. The requirement collection is Requirements/ in SwReqCollector
format v2, v1.2 (locked 2026-10-01; REQ-048 re-signed through CHG-001/002,
2026-10-11). It IS the spec: read its CLAUDE.md,
requirements.md (index), features.md (#Tag tree), reqs/REQ-NNN.md,
req_types.md, views/digest.md, views/trace.md and views/lock-receipt.md.
Every locked level (BIZ/FUN/UI) is Contract. Never write into that
folder — a change is a chg-draft in capture-outbox/.
  1. Run trace-check and confirm it parses the collection (51 REQs, 47
     locked, RED only for uncovered acceptance — no product tests yet).
  2. Dispatch the fresh-eyes requirements review (requirements-reviewer
     subagent, read-only on the collection). Write it as
     Requirements/reviews/review-01.md — the findings table with the
     Verdict column I fill per finding; every finding whose verdict will
     be `chg` also becomes a chg-draft in capture-outbox/. Never edit a REQ.
  3. Present me the digest (views/digest.md), the attention points
     (views/trace.md), the lock receipt, and the review's Blockers; then
     ask the artifact-named question:
     "Accept collection v1.2 for TangramPlaytime (G1)?"
  4. Record my yes in progress.md with approver + date. Then P2 (the stack
     is DECIDED: Kotlin + Jetpack Compose native, the prototype's locking
     logic ported — DI-8/SRC-016; the WO order is the one in KICKOFF.md):
     architecture.md (directives ack, guardrails from AGENTS.md rules 1–13,
     the governed interface registry with tiers), build-map.md (code homes
     per #Tag), design-inputs.md §2 (the Contract-delta — derive it from
     the inputs, do not import the first run's), the floor channel (API 26)
     proven, governance in force — then the G2 question.
Light mode is the default for every WO; Full needs one of the four reasons.
Owner questions are checkpoint items (≤5, product-visible, defaults after
3 days); lists to proof-read go to review-notes.md. The four scripts of a
work order are duties (AGENTS.md). End every turn with the one signal.
```
