# Design — WO-008 · #PlayTime

**Author:** Design Author (Opus 5.5) · **Date:** 2026-10-20 · **Status:** Reviewed · **Write-back:** 2026-10-20 for `reviews/WO-008-design-review.md` F1, F2 (option a), F4, F5, plus F3's KDoc item; plan write-back 2026-10-20 (`decisions.md` "WO-008 / P3 plan" rows: "Kit doors", the cut, the closed world) · **Full because:** (b) a new governed interface (`IActiveTime`; O-04, G-12). No new progress-file field, so neither reason (c) nor hard-stop 2 applies.

## 1. Scope

- **Implements:** REQ-005 A1–A2, REQ-029.A1, REQ-030 A1–A3, REQ-031 A1–A2, and REQ-006.A1 re-covered on `minSdk 31`. REQ-029.A2 is WO-007's.
- **Governed:**
  - **New:** `IActiveTime`, implemented by `:kernel`'s `ActiveTime`.
  - **Consumed:** `IProgressStore`, `Types.kt`, `ILockEngine`, `IPuzzleLibrary`.
  - **One notify delta:** the `IProgressStore` KDoc line (F3).
  - ADR-003 format 1 already holds every time and the setting.
- **How it works:**
  - **`:app`** reports the window's touches and visibility, and ticks `controller.countTime()` every 250 ms while visible.
  - **`ActiveTime`** completes whole active seconds on the injected `Clock` and keeps the play times.
  - **`PlayController`** folds each count into the open puzzle while its own predicate holds, keeps the best time, and writes through `Persistence`.
  - **`PlayScreen`** draws the timer.
- **Must-do 3 (Q8): the transitions stay in `PlayController`, and so do the puzzle and best time (REQ-030). TYPE-005 and the play times go to `:kernel`.**
  - The transitions fire on board events only the controller sees; `isSolved` is already the kernel's.
  - **F2(a):** the controller's predicate, read at each fold, decides which puzzle gets the seconds. Every change folds first, so there is no switch to re-send.
  - Rows: "O-04 read for WO-008" and its amendment.
- **G2 route** (the orchestrator applies it, F3):
  - **§4 registry row:**

    | Interface | Tier | Purpose | Between | Serves REQs | Defined in | Sign-off |
    |---|---|---|---|---|---|---|
    | `IActiveTime` | notify | TYPE-005 accounting: touch and visibility reports, whole active seconds, today's and all-time play time | `:kernel` ↔ `:play`, `:app` | REQ-005 (A2), REQ-029; supplies the seconds REQ-030 credits; TYPE-005 | `contracts/src/main/kotlin/app/tangramplaytime/contracts/IActiveTime.kt` | orchestrator (row 4) |

  - **`guard.json`:** `contract_paths.notify` gains the file, and the baseline is re-frozen after TASK-044.
  - **O-04 becomes:** "TYPE-005 accounting and the play times live in `:kernel` behind `IActiveTime`. `:app` reports touches and visibility. `PlayController` takes the TYPE-006 transitions on `ILockEngine.isSolved` and keeps each puzzle's time and best time. `:store` persists."
  - **§3:** `:kernel` serves #ActiveTime, and `:play` gains #PlayTime and #PuzzleTime.
- **`build-map.md` (notify-tier delta, applied by the orchestrator; F2(a) and Q10):**
  - `#PlayTime` → code home `play/`; deps: (kernel) via `IActiveTime`, #ResumePuzzle via `IProgressStore`; note: REQ-005 and REQ-030 (#PuzzleTime inherits).
  - A new row `#ActiveTime` → code home `kernel/`; note: REQ-029, TYPE-005.

## 2. Seam table

Δ marks a changed earlier seam (`tasks.md`).

| Seam | Kind | Signature / shape | REQ / source | Task |
|---|---|---|---|---|
| S-44 `IActiveTime` | I* `:contracts` | `fun touched()` · `fun visible(shown: Boolean)` · `fun count(): Int` (whole active seconds since the last count) · `fun playTimes(): PlayTimes`. One caller thread. **Must never:** return a negative time; count while not visible or idle more than 60 s | touched, visible: TYPE-005 rule · count: REQ-029 statement (and REQ-030's seconds) · playTimes: REQ-029 rule 1, REQ-005.A2 | TASK-044 |
| S-45 `ActiveTime` | `:kernel` class | `ActiveTime(clock: Clock, saved: PlayTimes)`, `IDLE_LIMIT_MS = 60_000`. It starts not visible, with no touch.<br>**Accrual:** each member first accrues (last, now], `max(0, min(now, lastTouch + 60 000) − last)` when visible and touched.<br>**Backwards reading (F5):** a reading `now < last` counts nothing and **shifts** `lastTouch -= last − now` (no touch stays none), then sets `last = now`. Idle stays idle.<br>**`count`:** n = pending / 1000, and pending keeps the rest. If the stored date ≠ `clock.today()`, it rolls `PlayTimes(0, today, all)`, then adds n to today and to all-time.<br>**`playTimes`:** today reads 0 on another date | TYPE-005; REQ-029; must-do 2 | TASK-044 |
| `IProgressStore` KDoc Δ | notify delta | Lines 19–20 become: "The store keeps what it is given; the rules that compute the values are its callers' (O-04): TYPE-005 accounting and the play times in `:kernel` (`IActiveTime`); the TYPE-006 transitions, the puzzle time and best-time keeping in `:play`'s `PlayController`." Logged in WO-008's Contract deltas | F3; O-04 | TASK-044 |
| S-46 `AppClock` | `:app` object : `Clock` | The one implementation (G-14): `override?.x() ?: SystemClock.elapsedRealtime()` / `LocalDate.now()`. `@Volatile internal var override: Clock?` is the kit's door (ADR-006) | G-14 | TASK-049 |
| S-1Δ3 | class | **Constructor:** `PlayController(library, engine, persistence: Persistence?, time: IActiveTime?, onEvent = {})`. The secondary `(library, engine, persistence, onEvent = {})` passes null `time`, so every existing construction compiles.<br>**Snapshot reads:** `timeSeconds` (now public), `timerShown` (from `Persistence.settings`, else false), `playTimes` (refreshed per fold) | REQ-030, 031, 005 | TASK-046 |
| S-2Δ3 | main thread | **`countTime()`:** the fold.<br>• `n = time.count()`;<br>• `if (state == IN_PROGRESS && !overviewOpen) timeSeconds += n` (WO-007 adds `&& !settingsOpen`);<br>• `playTimes = time.playTimes()`;<br>• then the S-20Δ cadence.<br>**`setVisible(shown)`:** fold, then `time.visible(shown)`; false writes the times at once.<br>**`internal fun solveWithoutBest(drive: () -> Unit)`:** a withheld flag in `try/finally`.<br>**Fold first, before:**<br>• `open`, then the **leaving** puzzle's S-20 save, then `load`;<br>• `restart`, `retry`, a tray `startDrag`;<br>• the solving `drop`, after which `bestSeconds = if (withheld) bestSeconds else minOf(bestSeconds ?: timeSeconds, timeSeconds)`;<br>• `openOverview`, `closeOverview`.<br>The construction's first `load` neither folds nor saves, and a resumed In-progress puzzle counts from the first fold | REQ-030 statement, rules 1, 2, 4, A1–A3; TYPE-006 rule 4; REQ-046 rule 4 | TASK-046 |
| S-20Δ | internal | `timeSeconds` now rides every S-20 save. Every **10 counted play seconds** and at `setVisible(false)`: `Persistence.savePlayTimes(...)`, then the open puzzle's S-20 save | REQ-005, 029, 030 | TASK-046 |
| S-19Δ `Persistence` | `:play` class | New constructor parameters `playTimes = PlayTimes.NONE, settings = Settings()`. `load` also reads both. `fun savePlayTimes(times)` updates the cache and queues the write, in one FIFO | G-13, REQ-029 | TASK-046 |
| S-27Δ `DevAid.solveNow` | debug | `drive` runs inside `c.solveWithoutBest { }` | REQ-030 rule 4; REQ-046.A3 | TASK-046 |
| S-11Δ3, S-48 | `PlayTexts`, fn | **`hoursMinutes`:** from `time_hours_minutes` = "%1$d h %2$d min" (en and fi).<br>**`internal fun PlayTexts.timeOf(seconds: Int): String`** (F4: internal until WO-007): below 3600 s `m:ss` in hand-built ASCII; from 3600 s `hoursMinutes.format(Locale.ROOT, h, m)`.<br>**`bestTimeText(seconds: Int?, texts: PlayTexts)`:** a signature change; it returns `noTime` or `timeOf` | REQ-029 rule 2; G-10 | TASK-047 |
| S-13Δ3, S-47 timer | tag, composable | `PlayTags.TIMER = "timer"`, shown while `timerShown && state != SOLVED`.<br>**Layering:** after the Restart pill, before the pieces.<br>**Slot:** right edge at the board's right − 14 dp, top at the board's top + 14 dp, width measured (as `rememberRestartSlot`).<br>**Content:** a 12 dp stopwatch outline and the time, 14 sp, muted, `tnum`, in px. An inert merged node whose text is the time, never in `ShownControls`.<br>**Recomposition:** it reads `timeSeconds` in its own composable | REQ-031; REQ-026 statement | TASK-047 |
| S-15Δ3 `:app` | wiring | **`AppGraph.start`** builds `ActiveTime(AppClock, persistence.playTimes)` and the controller with it, then calls `setVisible(visibleNow)`.<br>**`AppGraph.touched()`** forwards to the kernel.<br>**`AppGraph.visible(shown)`** remembers the value, forwards it to the controller, and starts or stops the ticker (`countTime()`, then `delay(250)`, on `mainScope`).<br>**`MainActivity`:** `dispatchTouchEvent` calls `AppGraph.touched()` before `super`. `onStart` and `onStop` call `visible`; `onStop` does so after `goHome` | TYPE-005; REQ-029.A1 | TASK-049 |

**Per screen (Q3; row "What counts on each screen").**
- **Play time** counts on every visible screen except the loading paper.
- **Puzzle time** counts only on the board while In progress. It does not count for a New puzzle or the solved view, and it pauses on the grid overview.
- **The DEV aid's sheet** does not pause it.
- **WO-007's settings screen** adds its term to the predicate.

**Kit doors (Q9), never a sleep.**
- **JVM:** a fake `Clock`, the real `ActiveTime`, and `countTime()` after each jump.
- **`play/` device:**
  - The same doors; the kit calls `time.touched()` in place of `:app`'s hook.
  - `Settings(timerShown = true)` is seeded in the kit store.
- **`app/` device (F1):**
  - `AppClock.override` is a kit clock that starts at `elapsedRealtime()`.
  - **No `app/` kit class writes the timer setting** (plan row "REQ-031.A2 is proven in `play/` only …").
    - `AppGraph`'s one controller reads the store once per process. A later write never reaches it, and an earlier one would leak the timer into every later class.
    - REQ-031.A2 is proven in `play/` only. The `app/` leg proves REQ-031.A1: a cleared app shows no timer.
  - The time lines read the counted seconds through `AppGraph.controller`, and `countTime()` runs on the main thread.
  - **Touches go through the window:** `Instrumentation.sendPointerSync` (or `UiAutomation.injectInputEvent`) at an inert point, the board's empty margin. **Never `performTouchInput` / `performClick`**: they never reach `dispatchTouchEvent` (fact 7).
  - **Each "at most" / "nothing" line has a positive control in the same test:**
    - **Untouched:** touch, +30 s → +30; touch, +5 min → **exactly** +60.
    - **Stopped:** touch, +20 s → +20; touch, Home at once, +5 min, return → +0.
    - **Death pair:** phase 1 counts N s and flushes; phase 2 reads N, not 0.

**Tokens (Q10)**, under the build-map delta of §1:
- **`kernel/`:** REQ-029.A1, on the JVM.
- **`play/`:** REQ-005 A1–A2 and REQ-030 A1–A3 (JVM, the controller over the real `ActiveTime`), and REQ-031 (device).
- **`app/`:** REQ-006.A1 (re-run).
- The `app/` time lines are extra tokens.

## 3. Risks & edge notes

- **Must-nevers.**
  - **An aid solve never sets a best time.** The REQ-046.A3 kits are the tripwire against "0:00".
  - **No negative time is written.**
  - **Browsing, the grid and tray turns add no puzzle time.**
  - **Restart and Retry keep the best time.**
  - **Idle adds at most 60 s:** ⌊(r + 60 000)/1000⌋ = 60.
  - **No timer on a fresh install or on the solved view.**
- **Fold before every change.**
  - Without a fold, "2 min on B, back to A" credits A, and a solve loses its last seconds.
  - The leaving save belongs in `open`, never in the first `load` (it would save defaults over the puzzle).
- **Backwards readings (F5).** The `app/` kit sends the clock back when it resets `AppClock.override` inside one process. Hence the shift, never `lastTouch = now`.
- **One thread.** `ActiveTime` has no locks; every caller and the kit's clock moves run on Main.
- **Recreation and death.** A recreation writes the times and counts nothing between `onStop` and `onStart`. A death on screen loses at most 10 counted seconds of each time. A new process counts from its first touch.
- **Carried to WO-007.**
  - Reset (REQ-034) must also reset `ActiveTime`'s in-memory play times (a notify delta).
  - The settings screen adds its predicate term and folds on open and close.
  - The settings screen stays in the activity's window: a Compose `Dialog` or `Popup` is another window, whose touches `dispatchTouchEvent` never sees.
  - `timeOf` becomes public then.
- **The play-through's closed world (S-48; plan row "The play-through's closed world of texts admits the best time …").** From TASK-046 on, a player's solve shows "m:ss" in the solved row where it showed "–". So `KitApp`'s closed-world checks admit the best time exactly as S-48 forms it: ASCII m:ss below one hour, or "–". A best time of one hour or more fails the leg loudly. This kit correction lands before TASK-046.
- **Floor (TASK-043).** No product `SDK_INT` check exists. The kit's `sdk >= 29/30` branches are now always true and stay. `adjustResize` stays.
- **Library facts relied on:**
  1. An activity is visible from `onStart` to `onStop`; split-screen and PiP stay STARTED. Screen-off stopping it on API 31+ is checked on the floor image, and the 60 s cap bounds any miss.
  2. `Activity.dispatchTouchEvent` sees every real touch of the window (`DecorView` → `Window.Callback`). The IME is another window.
  3. `elapsedRealtime()` is monotonic. `LocalDate.now()` reads the default zone per call, and `ActivityThread` resets that zone when it changes.
  4. `delay` on Main only paces the ticker.
  5. A snapshot read invalidates only its own scope.
  6. `tnum` gives tabular digits.
  7. **ui-test-android 1.12.1 dispatches `performTouchInput` on the `AndroidComposeView`, skipping `Activity.dispatchTouchEvent`.** `Instrumentation.sendPointerSync` goes through the window. To be filed in `platform-notes/android.md` § Compose test kit at the close.
- **Highest-risk part: the controller's fold-and-save order** (S-2Δ3, S-20Δ). Misattributed seconds, a lost solve time, or a save over the saved puzzle are all silent on screen.
  - **Oracles:** the JVM controller kit with 30 s, 2 min and 5 min jumps across each change, plus the F2 pin (an In-progress puzzle in the store → construct → visible, touch, +30 s → `countTime()` → +30, with no other action); the death pair.
  - **Next:** the aid-solve best.

## 4. Suggested cut

**Superseded by the plan's cut in `tasks.md` § WO-008 (TASK-043 … TASK-051).** The plan's cut differs from this design's suggestion in five ways:
- **TASK-043** also carries S-37Δ, the reset's crash-dialog check.
- **Every test edit is the acceptance-test author's**, scaffolding tests included.
- **Each kit stage lands with or before** the product change it pins, so no task is marked `[P]`.
- **REQ-031's device kit is in TASK-048.**
- **TASK-051 runs every channel once**, on the final tree.

The seams above keep their names; their delivering tasks are the plan's.

*Alternatives:*
- **F2(b), a `puzzleRuns` switch:** a state copy to re-send.
- **The transitions in `:kernel`:** D3, D5.
- **Touches from `:play`'s root:** they miss a sibling settings layer.
- **A write-side store check:** the failure repeats.
- **DI-1's sampling:** jumps mis-credit.
