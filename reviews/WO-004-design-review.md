# Review — design · WO-004

**Date:** 2026-10-15  ·  **Reviewer:** fresh context (design-reviewer, Opus 5.5)
**Inputs:** `designs/WO-004-design.md` (Draft, 2026-10-15, untracked over HEAD 19806a8) · `workorders/WO-004.md` (P3, log to 18:42) · REQ-003, 024, 025, 026, 050, 040, 041, 045 (locked, collection v1.2) · `req_types.md` v0.2 (TYPE-005, TYPE-006) · the three `capture-outbox/` drafts (proposed, 2026-10-11) · `architecture.md` v1.0 · `governance.md` v0.1 row 11 · `build-map.md` v1.3 · `contracts/…/IProgressStore.kt`, `Types.kt`, `ILockEngine.kt`, `IPuzzleLibrary.kt` (baseline 2026-10-11) · `play/src/main/…/*.kt`, `app/src/main/…/*.kt`, `app/src/androidTest/…/*.kt`, the `:play` / `:app` / `:store` Gradle files, `gradle/libs.versions.toml`, `LockEngine.stillValid` (HEAD 19806a8) · `decisions.md` rows 2026-10-12 and 2026-10-15 · `platform-notes/android.md`.
**Library code re-derived:** Android platform 37 sources (`sun/nio/fs/UnixCopyFile.java`) and `platforms/android-37.0/data/api-versions.xml` · JBR 25.0.3 `java.base` (javap) · Compose foundation / ui / ui-test **1.12.1** (BOM 2026.09.00; `classes.jar` from the cached AARs, javap) · kotlinx-serialization-core / -json **1.11.0** · kotlinx-coroutines-core-jvm **1.10.2** · androidx.test runner **1.7.0**.

## Verdict

**forward** — All six library facts the design relies on hold against the real code, including the two it flagged as riskiest: the atomic replace (libcore `rename()`, Windows `MoveFileEx(REPLACE_EXISTING)`) and the overview's pass order (`ClickableNode`, Main then Final). Every REQ line and edge has a seam, and no governed interface changes. The Shoulds sharpen the save path's failure boundary, the death-pair oracle, the one-writer invariant, a quantifier edge, and the cut. The planner carries them; none sends the design back.
0 Blockers, 5 Shoulds, 0 Notes.

## Checklist applied

- [x] **Directives** — D2: the simplest correct save path is one FIFO writer plus a whole-document file (§3 is right that a `launch` per save can reorder across IO threads). D3: every seam names a REQ, or a carried WO-003 item (S-23, S-3Δ). D4: no profile key, so the drop-profile draft is honoured. All nine `IProgressStore` members are implemented because a Kotlin class must implement them all; that is not speculative. D5: kotlinx.serialization is on architecture §5's list.
- [x] **Guardrails** — G-01: the file is in `noBackupFilesDir` and there is still no permission. G-12: `:play` holds `IProgressStore` only. G-13: only `:store` touches the file, and `onDevice` is its only `android.*` use. G-14: the 500 ms hold uses the pointer node's own timeout on the frame/test clock, the same family as WO-003's frame-clock animations. G-15: the boundary needs F1. G-17: the overview paths are in px. No G-NN is violated.
- [x] **Contract** — No `I*` or `Types.kt` change. `stillValid` is consumed as built. The O-04 reading is logged (decisions.md 2026-10-15, control point 13, ai+inform). The design leaves out one notify-tier consequence (F5, build-map).
- [x] **Scope** — Each of the 16 lines lands on a seam. The save-timing draft is met. The TYPE-006 empty-board case stays In progress, as locked, and is a decisions.md checkpoint candidate. The `nextUnsolved` reading "only the open one is unsolved → nothing changes" is surfaced as owner-grade (decisions.md 2026-10-15, browsing readings).
- [x] **Hard-stops** — Row 11 is declared, with ADR-003 recorded as decisions.md 2026-10-15 (ai+inform). Nothing touches security, PII or money.
- [x] **Evidence re-derived** — each library fact against the code:
  1. **`Files.move(ATOMIC_MOVE)`.** Holds.
     - API levels (`api-versions.xml`): `java/nio/file/Files` since 26, with `move`; `StandardCopyOption` since 26; `File.toPath()` since 26; `FileDescriptor.sync()` since 1; `Context.getNoBackupFilesDir()` since 21.
     - libcore `UnixCopyFile.move`: ATOMIC_MOVE → `rename(source, target)`, and EXDEV → `AtomicMoveNotSupportedException`. So `.tmp` must sit beside the file in `noBackupFilesDir`, which the design implies.
     - JBR 25 `sun.nio.fs.WindowsFileCopy.move`: ATOMIC_MOVE → `MoveFileEx(src, dst, 1)`, where 1 is `MOVEFILE_REPLACE_EXISTING`.
     - Caution: `Files.readString` / `writeString` are `since="36.1"`. They exist on the JVM test's JDK 25 but not on the floor image, and only a floor run would catch their use (F2).
  2. **kotlinx-serialization 1.11.0.** Holds; the failure boundary is F1.
     - `SerializationException extends IllegalArgumentException`.
     - `StreamingJsonEncoder.encodeDouble` → `Composer.print(D)`. It also throws `JsonEncodingException` when *encoding* a non-finite value.
  3. **coroutines 1.10.2.** `ChannelKt.Channel(int, …)` maps UNLIMITED (2147483647) to `BufferedChannel(Int.MAX_VALUE)`. Holds.
  4. **foundation 1.12.1.** Holds; the router must not consume while the overview is open (hit order 0).
     - `clickable` is no longer `detectTapGestures`. It is the non-suspending `ClickableNode.onPointerEvent`.
     - Main pass: it takes an unconsumed down, and `changedToUp` → `performClick`. It cancels on a consumed or out-of-bounds change (`handleNonUpEventIfNeeded`).
     - Final pass: `checkForCancellation` cancels on any consumed change other than its own down.
     - So a root that consumes moves in Main, after the child has seen them, cancels every overview press.
  5. **ui 1.12.1 and ui-test 1.12.1.** Holds.
     - `SuspendingPointerInputModifierNodeImpl$PointerEventHandlerCoroutine.withTimeout` launches `delay(t − 8); delay(8)` in the node's `getCoroutineScope()`.
     - `AndroidInputDispatcher.flush` → `advanceClockTime(eventTime − lastDispatchedEventTime)` → `MainTestClock.advanceTimeBy(ms, true)` before each event.
  6. **A fresh process per `am instrument`.** Holds by construction, because S-24 force-stops between the phases. I did not probe a device (device hygiene). A neighbouring assumption, that the exit status reports pass or fail, does not hold (F2).
  - Also checked: `BasicText(…, TextAutoSize)` and `TextAutoSize.StepBased` exist in foundation 1.12.1. `LockEngine.stillValid` tests inside the silhouette and no overlap with the pieces kept so far, nothing else, so the design's N3 reading (no anchor test) is accurate.
- [ ] **Plan review** — not this station (F5 is the cut as the design suggests it).
- [x] **The conceptual 20 %** — I walked the rules, acceptance lines and edges of REQ-003, 024, 025, 026 and 050, interrogating the save path first, then the router against the clickables. The findings follow.

## Findings

### F1 — QUALITY · Should · S-17; §1 "Unreadable state"; §3 fact 2
- **Observation:**
  - The Contract's must-never is "never fails the launch" (`IProgressStore` KDoc). The design's reason a bad file is survivable is fact 2: "Strict mode throws `SerializationException`, an `IllegalArgumentException`". Its validation list (unknown key or enum, turn, non-finite number, negative time) leaves out the date.
  - ADR-003 stores `playTimes.date`. Mapping a bad date to `LocalDate` throws `java.time.format.DateTimeParseException`, which extends `DateTimeException extends RuntimeException` and is not an IAE.
  - A store that catches IAE, as the fact invites, crashes on every launch when the file is valid JSON with a bad date. One bit flip is enough: '1' (0x31) → '9' (0x39) turns month 1x into 9x.
  - The crash comes before the first save, so the file is never overwritten. The result is a permanent crash loop that only "Clear data" ends.
  - The write side has the same gap: encoding a non-finite double throws `JsonEncodingException`.
- **Library evidence:** java.base 25 `java.time.format.DateTimeParseException` → `java.time.DateTimeException` → `RuntimeException` (javap); kotlinx-serialization-json 1.11.0 `StreamingJsonEncoder.encodeDouble`, the non-finite branch → `InvalidFloatingPointEncoded`.
- **Proposed resolution:**
  - State the boundary in S-17. Any `Exception` while reading, decoding, validating or mapping to contract types makes the whole store read as a fresh install. Any `Exception` while encoding or writing is a failed write: the old file is kept and the call does not throw.
  - Add "an invalid date" to the validation list.
  - Give the row-11 JVM test valid-JSON cases beside the garbage-bytes case: a bad date, an unknown enum, and an out-of-range int.
- **Triage:** (orchestrator, 2026-10-15 19:19) **accepted — fix in the design** (S-17, §1, the row-11 test). Against the `IProgressStore` KDoc: "never fails the launch and never throws for it".

### F2 — TEST · Should · S-24; TASK-019; §3 "The death pair"
- **Observation:** The death pair is the only oracle that runs fact 1 on Android's libcore; the JVM test runs `WindowsFileCopy` on JDK 25. The highest-risk section therefore rests on its honesty. It has three gaps.
  - **(a) The exit status cannot judge a phase.** S-24 says "Exits 0 only if both pass", but `AndroidJUnitRunner.onStart()` calls `finish(-1, results)` after `TestExecutor.execute(...)` whatever the outcome. The runner reports the same result code for a pass and a failure, so neither `INSTRUMENTATION_CODE` nor any exit status derived from it can tell them apart. A script built on `$?` is a check that never fails.
  - **(b) The ordinary device run executes the phases.** They live in `app/src/androidTest` with `AppLaunchKitTest` and `FloorChannelTest`, so `connectedDebugAndroidTest`, the WO's device channel, runs them in one process over one store. Phase 2 is order-dependent there. Phase 1 leaves `lastOpened` and three pieces behind, so `AppLaunchKitTest`'s tokened REQ-045.A1 "fresh start" depends on test order. The device reset runs per step, not per test.
  - **(c) `FloorChannelTest` also needs the wait.** `launchesAndShowsThePlayScreen` asserts `play-screen` right after launch. It needs the same `waitUntil` the design gives `AppLaunchKitTest`, because Compose idling cannot see the IO load.
- **Library evidence:** androidx.test runner 1.7.0, `androidx.test.runner.AndroidJUnitRunner.onStart()`: `TestExecutor.execute` then `iconst_m1` → `finish(I, Bundle)` (javap).
- **Proposed resolution:**
  - S-24 judges each phase from the instrumentation output (`am instrument -r -w`: pass means a run with zero failures, e.g. `OK (1 test)` and no `FAILURES!!!`).
  - S-24 gets a positive control: phase 2 run after `pm clear` without phase 1 must make the script exit non-zero (platform-notes: a verifier must be able to fail).
  - The phases run only when an instrumentation argument names them (`assumeTrue` on `InstrumentationRegistry.getArguments()`), so the ordinary connected run skips them.
  - The app kit's fresh-start test states its precondition (a cleared store) in its brief.
  - `FloorChannelTest` gets the `waitUntil`.
- **Triage:** (orchestrator, 19:19) **accepted — fix in the design** (S-24, TASK-019, the app kit's fresh-start precondition, `FloorChannelTest`'s wait). A death-pair script that judges by an exit status the runner never varies is a check that never fails (platform-notes § Release safety: a scanner must be able to fail).

### F3 — CONTRACT · Should · S-18; S-19/S-20 "the only writer"
- **Observation:** §3's "no save can lose another puzzle's progress" rests on two invariants: one document in memory per file, and one writer of puzzle progress. The seams enforce neither. The architecture says `:app` builds the store once, but nothing in the seams enforces it.
  - **(a) A second store instance loses saves silently.** `ProgressStores.onDevice(context)` builds a `FileProgressStore` per call. Two instances over `progress.json` each hold their own document and `Mutex`, so whichever saves next rewrites the file from a stale document and drops the other's saves. The design rejects DataStore because a second instance throws. That throw is the safe behaviour, and this store replaces it with silent loss. WO-007 (settings, reset) and WO-008 (times) are the next callers.
  - **(b) A direct `resetAll()` can be undone.** `resetAll()` is implemented and pinned now for WO-007. A direct `store.resetAll()` leaves `Persistence`'s cache stale. The overview then shows old states, and the next action on a cached In-progress puzzle saves its old board back: the reset is undone.
- **Library evidence:** the design's own S-17 and S-19 (one in-memory document per instance, the controller's cache); `IProgressStore` KDoc "Must never: lose one puzzle's progress when another is saved".
- **Proposed resolution:**
  - `onDevice` returns one process-wide instance per file (memoised), pinned by a test that two calls return the same object.
  - S-19 states that every progress write goes through `Persistence`, WO-007's reset included, and that `Persistence` updates its cache.
  - This is one line each in the seam table, with no contract change.
- **Triage:** (orchestrator, 19:19) **accepted — fix in the design** (S-18 memoised `onDevice` + its pin; S-19 every progress write through `Persistence`, WO-007's reset included). Against the `IProgressStore` KDoc's must-never and G-13.

### F4 — SCOPE · Should · REQ-024.A2; S-22 Overview; S-23; §3 "Always show the name and number"
- **Observation:**
  - §3 claims the bar shows the name and number "after a wrap, the grid, a long press, a recreation and at launch". Two windows contradict that claim.
    - While the grid is open, the overview sheet (inset 16 dp from the screen edges) covers the 66 dp top bar.
    - At launch, `play-loading` is plain paper with no bar for the whole IO load. Launch measured 1.9 s cold on the dev emulator in WO-003, before the store read was added (decisions.md 2026-10-12, "Carried to WO-004").
  - REQ-024.A2 says "always". That makes it a quantifier line, and platform-notes requires its adversarial path in the same test.
  - The kit cannot see the first window. ui-test's `checkIsDisplayed` checks placement, the host view and the window/screen bounds, but not occlusion by a sibling. An A2 assertion made with the grid open passes while the bar is hidden.
- **Library evidence:** ui-test 1.12.1 `androidx.compose.ui.test.AndroidAssertions_androidKt.checkIsDisplayed` (`isNotPlaced`, Espresso `isDisplayed` on the host view, `getBoundsInWindow`, `isInScreenBounds`, with no z-order check; javap).
- **Proposed resolution:** The design takes one of two routes and names it.
  - Start the sheet below the top bar. The bar stays visible and inert while the router stands aside, which makes "always" literally true with the grid open.
  - Or record the reading in decisions.md: "A2 holds while a puzzle is current and no overlay the player opened is in front". The A2 test then stays off the grid-open state.

  Either way, the launch window needs a recorded reading, e.g. "the paper precedes any current puzzle".
- **Triage:** (orchestrator, 19:19) **accepted, route 1** — the Contract wins over a narrowing reading: the overview sheet starts below the top bar, and the bar stays visible and inert while the overview is open (the design states the bar's touch behaviour in that state), so A2's "always" holds literally with the grid open and its test takes that adversarial path (platform-notes: quantifier lines). The launch window gets a `decisions.md` row (governance row 12): before the controller exists there is no current puzzle, so the bar is not drawn on the loading paper.

### F5 — TRACE · Should · §4 suggested cut; §1 "No contract delta"
- **Observation:**
  - **(a) TASK-017 and TASK-018 cannot run in parallel.** Both are `[P]` after TASK-016, but TASK-016's controller kit runs "over a temp-file store", which is `FileProgressStore` (S-17, TASK-017). Gradle compiles `play/src/test` as one source set. Until TASK-017 lands, the controller kit does not compile and TASK-018's done-check cannot be green. decisions.md 2026-10-11 (the WO-001 kit-staging row) records this constraint.
  - **(b) The `play/` tokens need a build-map change.** "The tokens go in the `play/` tests" includes REQ-040.A1 and REQ-041.A1. Their tags, #PuzzleOrder and #PuzzleWarmups, inherit `content/` (build-map §1). A `play/` token for them needs build-map rows, a notify-tier change (`contract_paths.notify`), while §1 says "No contract delta". The same applies if REQ-003.A2's "never" is to carry the process-death leg, whose test lives in `app/`.
- **Library evidence:** none (a planning constraint). Sources: build-map.md v1.3 §1; decisions.md 2026-10-11 WO-001 plan row ("Gradle compiles the whole `kernel/src/test` source set at once").
- **Proposed resolution:**
  - For the cut, either order TASK-017 → TASK-018, or run the controller kit over an in-memory fake `IProgressStore`, keeping the file store for `:store`'s row-11 test and the death pair.
  - For §1, either change it to "one notify-tier delta: build-map rows for #PuzzleOrder and #PuzzleWarmups (planner)", or keep those screen re-covers untokened.
  - The planner settles both, as the WO's scope note allows.
- **Triage:** (orchestrator, 19:19) **(a) accepted — fix in the design's cut**: either TASK-017 → TASK-018 in series, or the controller kit over an in-memory test double of `IProgressStore` (the file store stays the oracle of `:store`'s row-11 test and the death pair); the design names which. **(b) reversed on the evidence:** trace-check's rule 3 (`trace_check.py` `check_features_v2`) fails a tag only when *no* covering test lives under its code home; extra tokens outside the home are allowed. REQ-040.A1 and REQ-041.A1 keep their `content/` tokens (WO-002), so tokened `play/` re-covers need no build-map row; likewise a REQ-003.A2 token in `app/` is extra coverage as long as a `play/` test (the `#Browsing` home) also covers A2. §1 "No contract delta" stands. A review finding reversed on arbitration → corrected check, `proposals.md` at close.

## Handoff — design-reviewer · WO-004
- **Scope:** REQ-003 A1–A2, REQ-024 A1–A3, REQ-025 A1–A3, REQ-026 A1–A2, REQ-050 A1–A3, REQ-040.A1, REQ-041.A1, REQ-045.A1 (screen half) · governed touched: `IProgressStore` (implements), `ILockEngine` / `IPuzzleLibrary` / `Types.kt` (consumes); no change
- **Inputs read:** as listed under **Inputs** and **Library code re-derived** above
- **Result:** `reviews/WO-004-design-review.md`
- **Status:** forward (counts in the Verdict line)
- **Traceability delta:** none (review only; F5 names a build-map delta for the planner to decide)
- **Notes for next station:**
  - The six library facts were re-derived from bytecode and source, and none is wrong.
  - F2 and F5 shape the task rows and briefs: the death-pair guard, judging phases from the output, its positive control, and either TASK-017 before TASK-018 or a fake store.
  - F1 and F3 are one line each in S-17, S-18 and S-19.
