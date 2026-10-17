# Design — WO-004 · #Browsing + `:store`

**Author:** Design Author (Opus 5.5) · **Date:** 2026-10-15 · **Status:** Reviewed (F1–F4, F5(a) folded in) · write-back 2026-10-17 (S-1Δ `puzzles`, S-24 modes, S-24 stale refusal (code review S2); decisions.md 2026-10-17) · **Full because:** (c) new persistence format; (b) also true

## 1. Scope

- **Implements:**
  - REQ-003 A1–A2, REQ-024 A1–A3, REQ-025 A1–A3, REQ-026 A1–A2, REQ-050 A1–A3.
  - The screen half of REQ-040.A1, 041.A1, 045.A1.
- **Governed:** `IProgressStore` is implemented; `ILockEngine`, `IPuzzleLibrary` and `Types.kt` are consumed. **No contract delta.** Row 11 (schema): ADR-003.
- **How it works:**
  - `:app` loads the bundle and the store on IO, then builds `PlayController` on Main.
  - The controller saves through `Persistence`, which holds the cache and one FIFO writer.
  - Every Play-screen touch goes through the root `PointerRouter`, except on the grid overlay. The overlay uses foundation `clickable`s, and the router stands aside while it is open.

**ADR-003.** One UTF-8 JSON file, `noBackupFilesDir/progress.json` (`allowBackup` is already false; REQ-010 rule). It is written with kotlinx.serialization: allowed by §5, no D5 record. Decoding is strict, into private DTOs.
```
{"format":1,"lastOpened":"<id>"?,"puzzles":{"<id>":{"state":"NEW|IN_PROGRESS|SOLVED",
 "placed":[{"piece":"LT1","turn":0..7,"mirrored":false,"x":1.5,"y":2.0}],"time":0,"best":null}},
 "playTimes":{"today":0,"date":"2026-10-15"|null,"allTime":0},"settings":{"timerShown":false,"soundOn":true}}
```
- **Reading.** Once, into memory, behind a `Mutex`.
- **Saving.** Every save rewrites the whole file: `.tmp`, `fd.sync()`, `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`. It returns after the rename, and the file is never patched.
- **Unreadable state.** The whole store reads as a fresh install, and the next save overwrites the file, when:
  - the file is missing;
  - `format` ≠ 1 (absent or 0 is older, above 1 is unknown);
  - **any `Exception`** occurs while reading, decoding, validating or mapping. Validation covers an unknown key or enum, `turn` outside 0..7, a non-finite number, a negative time and an invalid date.
- **A failed write** is any `Exception` while encoding or writing. It keeps the old file and the in-memory document; the next save retries, and the call never throws (G-15).
- **No profile key.** A profile map would be a format-2 migration (REQ-025 rule; D2).
- **All members are implemented**, because the format is whole. `playTimes`, `settings` and `resetAll` carry untokened row-11 pins; REQ-029/031/033/034 stay with WO-007 and WO-008.

**Pending drafts.** The build meets the locked text and the save-timing draft. The TYPE-006 empty-board case stays In progress, as locked. **No line is `implemented (unverified)`.**

## 2. Seam table

S-n are WO-003's seams (tasks.md); Δ marks a changed one.

| Seam | Kind | Signature & value shapes | Required by | Delivered by task |
|---|---|---|---|---|
| S-17 `FileProgressStore` | `:store` class : `IProgressStore` | `FileProgressStore(file: File, io: CoroutineDispatcher = Dispatchers.IO)`, per ADR-003. Never throws: an `Exception` on the read side means a fresh install; on the write side, a failed write | REQ-025 rule, REQ-003, row 11, the KDoc's "never fails the launch" | TASK-017 |
| S-18 `ProgressStores` | `:store` object | `onDevice(context): IProgressStore` returns **one memoised instance per file**. Two calls give the same object, pinned on the device in `AppWiringKitTest` because a JVM test cannot build a `Context`. The file is in `noBackupFilesDir`. The only `android.*` use in `:store` | G-13, REQ-010 rule, the KDoc's must-never | TASK-017 |
| S-19 `Persistence` | `:play` class | `Persistence(store, scope: CoroutineScope, saved: Map<String, PuzzleProgress>, lastOpenedId: String?)`; `companion suspend fun load(store, scope)`. It holds the cache and the one FIFO writer: `save(id, progress)`, `saveLastOpened(id)`, `suspend fun flush()`. **Every store write goes through it** and updates the cache in the same queue; WO-007's reset and WO-008's times add methods here | REQ-025, REQ-003, G-13 | TASK-018 |
| S-1Δ construction | class | `PlayController(library, engine, persistence: Persistence?, onEvent = {})`. The secondary `(library, engine, onEvent = {})` passes `null` (memory only), so the WO-003 kits compile unchanged. Opens `lastOpenedId` if listed, else `puzzles.first()` | REQ-040.A1, 041.A1, 045.A1, REQ-025 | TASK-018 |
| S-1Δ reads | snapshot state | `puzzleNumber` (1-based) · `puzzleCount` · `stateOf(id)` · `bestSeconds: Int?` · `overviewOpen` · `celebration: Celebration?` · `internal val puzzles: List<PuzzleDefinition>`, the library's listed puzzles in ‹ › order, read by the overview (decisions.md 2026-10-17 "one internal controller read") | REQ-024 rule, REQ-026, REQ-050 | TASK-018; `puzzles` TASK-021 |
| S-2Δ actions | main thread | `next()` and `previous()` wrap. `nextUnsolved()` takes the first non-Solved puzzle after the current one, wrapping: if that is the current one, nothing changes; if every puzzle is solved, it acts as `next()`. `restart()` (In progress) and `retry()` (Solved) give New, an empty board, time 0, best kept, the tray at rest. `openOverview()`, `closeOverview()`. `choose(id)` closes, then opens `id` unless it is already open. `flush()` delegates to `Persistence` | REQ-003, 024, 025, 026, 050 | TASK-018 |
| S-20 save rule | internal | After each action: if `PuzzleProgress(state, placed if IN_PROGRESS else [], time, best)` differs from the cached one, `Persistence.save` it. Each open calls `saveLastOpened`. Times pass through untouched; only Restart and Retry write 0 (O-04) | REQ-025 rule, REQ-024 rule "saves first", REQ-003.A2 | TASK-018 |
| S-21 load check | internal | On every open, the start included. IN_PROGRESS: `stillValid(puzzle, placed.filter { it.piece in puzzle.pieces }.distinctBy { it.piece })`; the state stays IN_PROGRESS even if the board comes back empty. NEW or SOLVED: an empty board. A correction is saved | REQ-025 rule, A3; REQ-026.A1 | TASK-018 |
| S-3Δ `Celebration` | data class | `Celebration(puzzleId, serial)`, set by `drop()` at the solve and cleared by open or `retry`. Adds `ShownCues.celebrationSerial`. `SolveDriver` plays a new serial once; otherwise it starts at the end, set synchronously in the first frame | REQ-023 (WO-003 N1), REQ-026.A1 | TASK-018, TASK-020 |
| S-11Δ `PlayTexts` | data class | Adds `restart, retry, next, bestTime, noTime, inProgress, solved, counterFormat, allPuzzles, done`. Kit literal: "Restart", "Retry", "Next", "Best time", "–", "in progress", "solved", "%1$d / %2$d", "All puzzles", "Done" | G-10, O-06 | TASK-020 |
| S-13Δ `PlayTags` | consts | `nav-prev`, `nav-next`, `counter`, `rating`, `state-word`, `restart`, `solved-row`, `retry`, `best-time`, `solved-next`, `overview`, `overview-done`, `overview-cell-<id>`, `play-loading`. A control's bounds are its touch area | both kits | TASK-020 |
| S-14Δ `PlaySemantics` | keys | `RatingKey: Int`. `PuzzleIdKey` and `PuzzleStateKey` on each overview cell. Texts are merged, inert nodes | REQ-024 rule, REQ-050.A2 | TASK-020, TASK-021 |
| S-22 touch geometry | `PointerRouter`, `HitTest` | Specified below | REQ-024, 025, 026, 050 | TASK-020, TASK-021 |
| S-23 `PlayLoading` | composable | `PlayLoading(modifier)`: plain paper, no top bar (no puzzle is current yet), tag `play-loading` | WO-003 F3 | TASK-020 |
| S-15Δ `:app` | wiring | `AppGraph.start(context)` (idempotent) and `AppGraph.controller: StateFlow<PlayController?>`. The library and `Persistence.load(ProgressStores.onDevice(ctx), mainScope)` run on IO; the controller is built on Main. `MainActivity` shows `PlayLoading`, then `PlayScreen`. `BackHandler(enabled = overviewOpen)`. `onStop` still sends a drag home. Strings in en and fi | WO-003 F3, REQ-050 rule (back) | TASK-020 (strings, the `PlayTexts` construction in `MainActivity`), TASK-022 |
| S-24 death check | script | `tools/process_death_check.sh`: install both APKs, `pm clear`, `am instrument -r -w -e deathPhase 1 -e class app.tangramplaytime.app.ProcessDeathKitTest#phase1`, `am force-stop`, then `phase2`; it resets the device as its last step. Each phase is judged from its per-test `-r` status blocks only, as passed, failed or not run (decisions.md 2026-10-17 "The process-death check's verdicts"); the plain run exits 0 only when both pass. Neither `OK (1 test)` nor the exit status suffices: a phase skipped by its `assumeTrue` on `deathPhase` prints `OK` too. `--positive-control` exits 0 only when phase 2 over a cleared store **ran and failed** (as V-01's control does): a skip or a crash cannot pass the control. `--negative-run`: phase 2 without `-e deathPhase`, after a passing phase 1, must exit non-zero. Stale APKs: before installing, it exits 2 (cannot judge) when either APK is older than any source or build input, and every verdict line prints both APK times. The channel's Gradle line forces `:app:packageDebug --rerun :app:packageDebugAndroidTest --rerun`, because Gradle leaves an up-to-date APK unwritten (decisions.md 2026-10-17 "Amends … verdicts" row) | REQ-025 rule, REQ-003.A2, row 11 | TASK-022 |

**Touch geometry (S-22, phone; W is the screen width).**
- **Top bar (66 dp).** Three full-height columns, with no gaps:
  - ‹ covers x from 0 to 60 dp (a 52 dp square drawn at 8 dp);
  - › covers W − 60 to W;
  - the counter is everything between: title, then `rating` dots, "n / N" and the state word.
  
  The 4 dp gap to the board hits nothing.
- **Restart.** A pill shown only In progress: 48 dp high (design-inputs §2 item 6), at the board's top-left + 10 dp, as wide as its measured label. It is drawn above the silhouette and below the pieces.
- **Solved row.** Appears from the frame the picture is fully shown, or at once for a puzzle opened already solved. It splits the tray strip into thirds: Retry, best time (inert), Next. Pills are at least 48 dp high.
- **Hit order:**
  0. The overview is open: the router neither consumes nor acts, on the top bar too.
  1. A drag is running: the touch is a twist finger, so ‹ and › never act mid-drag.
  2. A top-bar column, or a third of the solved row.
  3. The badge disc → a piece's drawn shape → **the Restart pill** → the rest of the badge area → a cell or a margin.
- **Taps and the long press.** A tap is a lift within the 12 dp slop, acted on at the lift whatever its duration; moving the slop distance or more cancels it. On › only, a finger held within the slop at 500 ms runs `nextUnsolved()` then, and the lift does nothing.
- **Overview.**
  - The scrim and the sheet start **below the top bar**. The sheet is inset 16 dp and holds a title, a `verticalScroll` grid (every cell composed) and Done (at least 48 dp).
  - The bar stays drawn and **inert**: a touch on ‹, › or the counter does nothing while the overview is open.
  - Square cells of at least 84 dp (DI-1; REQ-050 asks for 64).
  - A cell shows the flat silhouette (New), the silhouette with a dot (In progress) or the clipped picture (Solved). The open puzzle's cell is outlined. Paths are in px (G-17).
  - It closes by Done, a tap on the scrim, or back.

**What survives.**
- **An activity recreation** keeps all the controller holds: the puzzle, the board, the overview's open state and the cue serials. A cue already set before the first composition counts as shown, the celebration included. A › hold is cancelled.
- **A process death** keeps only what the store holds, after the load check. The overview is closed, the tray turns are at rest (F36), and no cue plays.

## 3. Risks & edge notes

- **Never lose another puzzle's progress** (REQ-003). This rests on one store instance per file (S-18) and one writer (S-19), which rewrites the whole document from memory in FIFO order. Never `launch` once per save: the launches can reorder, and a stale board would win.
- **Never show a solved puzzle's piece positions** (REQ-026.A1). A Solved puzzle opens with an empty board. Final placements still in memory are drawn only while their celebration's serial plays. Adversarial path: `recreate()` right after a solve, first frame, clock paused.
- **Always show the name and number** (REQ-024.A2), while a puzzle is current: after a wrap, after a long press, after a recreation, and with the grid open.
  - The grid-open case is the adversarial path. The bar sits above the sheet, and the test must check exactly that, because `isDisplayed` cannot see occlusion.
  - Before the controller exists no puzzle is current, so the loading paper has no bar (decisions.md 2026-10-15).
  - The title autosizes from 22 sp down to 14 sp before it ellipsizes.
- **Times are never invented.** The best time shows "–" until WO-008 stores one.
- **WO-003 seam text superseded.**
  - S-2's "a Solved puzzle keeps its final placements" and S-1's `first()` are replaced; pins on either go red legitimately.
  - `KitScreen.kt`'s `PlayTexts` literal grows.
  - `AppLaunchKitTest` and `FloorChannelTest` must `waitUntil` the `play-screen` node exists, because idling cannot see the IO load.
  - `AppLaunchKitTest`'s fresh start needs a cleared store (`tools/device_reset.sh`).
- **`stillValid` suffices** (WO-001 N3). An anchor test would evict a piece left legitimately after its anchor piece moved, which breaks A1. Filtering foreign and duplicate pieces is set membership, not geometry (O-01).
- **O-04.** The TYPE-006 transitions stay in `PlayController`, as in WO-003 (decisions.md, control point 13).
- **Residual risk.** A kill within milliseconds of a lock can lose that lock, because the write is off-main. Accepted.
- **Library facts relied on:**
  1. `Files.move(ATOMIC_MOVE)` replaces an existing target atomically: `rename(2)` on Android libcore (`java.nio.file` since API 26), `MoveFileEx(REPLACE_EXISTING)` on the Windows JDK of the JVM tests.
  2. kotlinx.serialization writes a `Double` with `toString()`, which round-trips exactly. Its failures are not all `IllegalArgumentException`s: `DateTimeParseException` for a date, `JsonEncodingException` for a non-finite number. Hence S-17 catches `Exception`.
  3. `Channel(UNLIMITED).trySend` never fails while open, and one consumer receives in order.
  4. Compose runs the Main pass on descendants first, and `clickable` cancels when its Final pass sees the change consumed. So the router must not consume while the overview is open.
  5. `AwaitPointerEventScope.withTimeoutOrNull` runs on the pointer node's clock: uptime on a device, `mainClock` under the test rule. Injected `advanceEventTime` moves it, so nothing sleeps (G-14).
  6. Each `am instrument` is a fresh process. Its exit status never reports a failure (`finish(-1, …)`).
- **Highest-risk part: the save path** (S-20 ordering, the ADR-003 atomic replace). A defect loses a child's progress silently, and no screen test sees it. It rests on facts 1 and 3. The oracles:
  - **Row 11's `:store` JVM test.**
    - An unknown or older version reads as a fresh install. So does corrupt content: garbage bytes, a bad date, an unknown enum, an out-of-range int.
    - A new store instance over the same file reads every member back. Saving B keeps A.
  - **On the device:**
    - `AppWiringKitTest` pins that `onDevice` gives one instance.
    - The death pair (S-24, `ProcessDeathKitTest`) runs on the dev and floor images. Phase 1 places three pieces through the real app and calls `flush()` via `AppGraph`, the kit's one door into it. Phase 2 runs in a new process and finds them in place. A same-process test would be fooled by the controller, which outlives the activity.
  - **Next-highest risk:** fact 4.

## 4. Suggested cut

| Task | [P] | Delivers seams | Covers | Depends on |
|---|---|---|---|---|
| TASK-016 | | JVM kit (test author): the `:store` row-11 test, and a controller kit over a kit-owned **in-memory `IProgressStore` double**. The file store is the oracle of row 11 and the death pair only, so TASK-017 and TASK-018 stay [P] | JVM halves | — |
| TASK-017 | P | S-17, S-18 (serialization plugin and json for `:store`) | row 11 | TASK-016 |
| TASK-018 | P | S-19, S-1Δ, S-2Δ, S-20, S-21, the controller side of S-3Δ | REQ-003, 024, 025, 026, 050, 040.A1, 041.A1, 045.A1 | TASK-016 |
| TASK-019 | | Device kit (test author): `play/` and `app/`; `AppWiringKitTest` (the S-18 pin); the argument-guarded death phases; the `AppLaunchKitTest` wait and its cleared-store precondition | screen halves | TASK-018 |
| TASK-020 | | S-11Δ with S-15Δ's strings and `PlayTexts` construction, S-13Δ, S-14Δ, S-22 (bar, Restart, solved row, long press, hit order), the screen side of S-3Δ, S-23 | REQ-024, 025, 026, 050.A1 | TASK-018 |
| TASK-021 | | The S-22 overview, the S-14Δ cell keys | REQ-050.A2, A3, REQ-003.A1 | TASK-020 |
| TASK-022 | | the rest of S-15Δ, S-24 with its positive control, `FloorChannelTest`'s wait | WO-003 F3; REQ-003.A2; the launch A1 lines | TASK-017, TASK-021 |

**Tokens.** REQ-040.A1 and 041.A1 keep their `content/` tokens (WO-002); the `play/` and `app/` re-covers are extra tokens, which trace-check allows. REQ-003.A2 is tokened in `play/` (#Browsing), and the `app/` death pair is extra coverage. So no build-map row is needed.

*Alternatives:* DataStore (a second instance over one file throws, which breaks row 11) · a TYPE-006 machine in `:kernel` (a new governed interface, D2).
