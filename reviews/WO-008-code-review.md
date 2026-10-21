# Review — code · WO-008

**Date:** 2026-10-21 05:40  ·  **Reviewer:** fresh context (Sonnet)
**Inputs:** working tree against `5706854`: `IActiveTime.kt`, `ActiveTime.kt`, `AppClock.kt`, `BoardTimer.kt` (new); `PlayController.kt`, `Persistence.kt`, `PlayTexts.kt`, `Controls.kt`, `PlayTags.kt`, `PlayScreen.kt`, `DevAid.kt`, `AppGraph.kt`, `MainActivity.kt`, both `strings.xml`, the four `minSdk` lines, `IProgressStore.kt` (KDoc), `tools/device_reset.sh`; the kit's JVM and app legs read for token substance (`PlayTimeKitTest`, `ActiveTimeKitTest`, `ActiveTimeAppKitTest`, `ProcessDeathKitTest` diff). Oracles: `workorders/WO-008.md`, `designs/WO-008-design.md` §2–§3, `reviews/WO-008-design-review.md`, `tasks.md` § WO-008, `architecture.md` v1.4, REQ-005/029/030/031/006, TYPE-005/006, `decisions.md` rows 2026-10-20/21, `.swdev/run-log/WO-008.md`.

## Verdict

**forward** — the fold-and-save order (design §3's highest risk) matches S-2Δ3 line by line and the kit's pins kill every mutation I made to it; the kernel's accounting re-derives from TYPE-005 and its kit kills its mutations. Nothing stands as a Blocker or a Should. Five Notes, none of them blocking.
0 Blockers, 0 Shoulds, 5 Notes.

## Checklist applied

- [x] **Directives** — D2/D4: nothing built beyond S-44…S-48 and S-37Δ; `AppClock.override` is the ADR-006 door the design names.
- [x] **Guardrails** — G-01/V-01: the diff adds no permission, network, ad, analytics or purchase code (grep of the diff for INTERNET, `uses-permission`, analytics, billing, ads, HTTP and socket names: no hit; no manifest or dependency line changed; the only build-file change is four `minSdk` lines). G-12: `ActiveTime` imports `:contracts` and `java.time` only; `:play` reaches the kernel only through `IActiveTime`. G-13: both new writes (`savePlayTimes`, the cadence save) go through `Persistence`'s one FIFO. G-14: product reads time only in `AppClock` (grep of the diff: `SystemClock` / `LocalDate.now` appear in product only there; the rest is kit code). G-17: the stopwatch is drawn in px.
- [x] **Contract** — `IActiveTime.kt` carries the notify banner, is in `guard.json` `contract_paths.notify` and in the `architecture.md` registry (row 79); `IProgressStore.kt` is a KDoc-only delta. Signatures equal S-44 (`touched`, `visible`, `count`, `playTimes`).
- [x] **Scope** — every changed product line traces to S-1Δ3, S-2Δ3, S-11Δ3, S-13Δ3, S-15Δ3, S-19Δ, S-20Δ, S-27Δ, S-44…S-48, S-37Δ.
- [x] **Hard-stops** — none touched.
- [x] **Evidence re-derived** — see "Re-derived".
- [x] **The conceptual 20 %** — see the six flagged items, answered below.

### The orchestrator's six items

1. **Fold-first pins (judged: they still pin S-2Δ3's order).** The concern was that an action's own fold is the first after a clock jump. That is exactly the case the order matters in: if the action changed the predicate before folding, the jump's seconds land on the wrong side. I mutated the product in a scratch copy: removing the fold in `open`, `openOverview`, `closeOverview`, the tray `startDrag`, the solving `drop`, or `setVisible` each turns `PlayTimeKitTest` red (2, 1, 1, 1, 1, 1 failures); moving `open`'s fold after the leaving save also turns it red (2). The one mutant that survives, `setVisible` folding after `time.visible(shown)`, is equivalent: `ActiveTime.visible` accrues the interval itself before it flips, so the pending seconds are identical. A tick between the jump and the action would let a missing fold hide behind the tick, which is why the kit leaves it out.
2. **`DataExtractionRules` (judged: nothing needed).** The warning is advisory. `allowBackup="false"` is declared; the only persisted data is the store's file in `noBackupFilesDir`, which Auto Backup and device transfer both leave out (ADR-003). An explicit `dataExtractionRules` with excludes would silence the lint and state REQ-010 in the manifest; that is optional and not this WO's (N5).
3. **`timeOf` and a negative input (judged: unreachable).** Every source is non-negative: `count()` returns `pendingMs / 1000` of a non-negative `pendingMs`, and the store's reader rejects a negative time (decisions.md 2026-10-20 "No time the store's reader rejects is ever written …"). With a negative input `timeOf` would print `0:-5`; no path reaches it. No guard is needed.
4. **`timerShown` not snapshot state (judged: sound for this WO, one carry; N1).**
5. **`setVisible(visibleNow)` at construction (judged: harmless; N2).**
6. **Owner's rule:** see Guardrails above; V-01 re-ran green in the log, and I found no addition.

## Findings

### Blockers
None.

### Shoulds
None.

### Notes

**N1 — `timerShown` is a plain getter over a `val` read at load (product; carry to WO-007).** `PlayController.kt:178-179` reads `persistence?.settings?.timerShown`, and `Persistence.settings` (`Persistence.kt:70`) is a `val` set at `load` with no write method. Correct for WO-008: nothing can change the setting, and `TimerKitTest` seeds it before the load. When WO-007 adds the switch, a write through `Persistence` that does not also replace this value, and make the read a snapshot read, leaves the board's timer stale until the next process. WO-007's brief should name both (the getter must become state, and `Persistence` needs `saveSettings` updating the cached `settings`). The design's "Carried to WO-007" list has the reset and `timeOf` but not this.

**N2 — `AppGraph.start` calls `setVisible(visibleNow)` before publishing the controller (`AppGraph.kt:114`; product, benign).** If the load finishes after the game left the screen (`visibleNow` false), that is `setVisible(false)`, so `writeTimes()` queues `savePlayTimes(playTimes)` and the puzzle's `keep()`. The values are the ones just loaded (the play times, with today rolled to the clock's date when it differs, which `ActiveTime.playTimes()` reports anyway), and `keep()` writes nothing unless the load check corrected the puzzle. No loss and no overwrite; at most one redundant file write at start-up. By reading; I did not run a device case for it.

**N3 — Ledger: library fact 7 is not yet in `platform-notes/android.md`.** The design (§3 fact 7) says "to be filed in `platform-notes/android.md` § Compose test kit at the close": `performTouchInput` dispatches on the `AndroidComposeView` and skips `Activity.dispatchTouchEvent`, so a kit that needs the activity's touch hook must use `Instrumentation.sendPointerSync`. `grep` for `sendPointerSync` and `dispatchTouchEvent` in that file finds nothing today. The next kit author would repeat the mistake; a close item for the orchestrator, not a product or kit defect.

**N4 — The sub-second remainder crosses puzzle changes (kernel, accepted design).** `ActiveTime` keeps up to 999 ms of pending active time (S-45 "pending keeps the rest"), so on `open(B)` the whole seconds go to the leaving puzzle and the leftover fraction completes a second that is credited to B. REQ-030.A1 ("browsing away adds nothing") therefore holds at whole-second granularity, with a leak of under one second per switch. The kit pins whole seconds and the kept remainder (`wholeSecondsCountAndTheRemainderIsKept`). Not a defect against the REQ; stated so nobody reads it as an exact boundary. The same rule makes a solve under one counted second read `0:00` as the best time; unreachable by hand in a two-piece mini puzzle, so no floor is asked for.

**N5 — Optional: explicit `dataExtractionRules` (app manifest).** See item 2. If the owner wants the lint warning gone and REQ-010 stated in the manifest, an `xml/data_extraction_rules.xml` excluding the whole domain for `cloud-backup` and `device-transfer` does it; no behaviour changes. Not asked for by any REQ.

## Re-derived

| Claim | Result |
|---|---|
| JVM two-run line on a scratch copy of the working tree (run 1 builds both APKs; run 2 `:content :play :kernel :contracts :store :app` tests, `--rerun`, `--continue`), 05:38 | content 80/0, play 132/0, kernel 65/0, contracts 2/0, store 12/0, app 4/0 = **295/0**, equal to the run log's t049-host. (A first run 2 without run 1 had one red, `ReleaseBuildKitTest`, "APK not found": the documented ordering, not a defect.) Daemon stopped after |
| Mutations to `PlayController` in the scratch copy, `PlayTimeKitTest` only | baseline green; `open` without fold: 2 red; `open` with the leaving save before the fold: 2 red; solving `drop` without fold: 1 red; tray `startDrag` without fold: 1 red; `openOverview` without fold: 1 red; `closeOverview` without fold: 1 red; `setVisible` without fold: 1 red; `setVisible` fold after `time.visible`: still green (equivalent, see item 1) |
| Mutations to `ActiveTime`, `:kernel:test` | idle limit 61 000 ms: 3 red; backwards reading sets `lastTouch = now` instead of the shift: 2 red; `count` rounds up: 1 red; accrual without the visible gate: 1 red. The kernel kit can fail |
| S-45 against the code | `accrue()`: `max(0, min(now, lastTouch + 60 000) − last)` when visible and touched; `now < last` counts nothing and shifts `lastTouch` by the jump, no touch stays none; `count` rolls the date only when n > 0; `playTimes()` returns `PlayTimes(0, today, all)` on another date, date still null with none stored. Each equals the row and the three 2026-10-21 decisions rows |
| S-2Δ3 order in `PlayController` | `open`: fold, `goHome`, leaving `keep`, `load` (the construction's `load` neither folds nor saves a leaving puzzle); `restart`, `retry`, tray `startDrag`, `openOverview`, `closeOverview` fold first; the solving `drop` folds while still In progress, then `bestSeconds = if (withheld) … else minOf(…)`; `setVisible` folds, tells the kernel, and writes at once when false; `solveWithoutBest` restores the flag in `finally`. A tick-only cadence check (`unsavedPlaySeconds >= 10`) and a `time == null` controller writes no play times, as decisions rows say |
| `AppGraph` (S-15Δ3) | counts started instances (`visibleNow = startedActivities > 0`), tells the controller only on a crossing, ticker `countTime()` then `delay(250)` on `mainScope`, cancelled when not visible; touches before the load are dropped (`time` null). A recreation is `visible(false)` then `visible(true)`: times written, nothing counted between |
| REQ tokens | each token sits on a test whose assertion is the line's meaning (read: REQ-005.A1/A2, REQ-029.A1 kernel and app, REQ-030.A1/A2/A3, REQ-031.A1 app leg, the death pair's `phase2` read of 60 s); positive controls in the same test for each "at most" / "nothing" line; no empty test carries a token |
| Owner's rule (item 6) | diff grep: no permission, network, analytics, ad or purchase name; manifest and dependencies unchanged |
| `tools/device_reset.sh` S-37Δ | `crash-window` read last, matches `Application Error` / `Application Not Responding` from every `mCurrentFocus` line, never dismissed, fails the reset with "reboot the image"; the run log's control (t043-control) provoked and cleared it on the floor image. Read, not re-run (no device sets, per the brief) |
