# Code review — WO-004 · #Browsing + `:store` (TASK-016…022)

**Reviewer:** code-reviewer (fresh context, Sonnet 5.5) · **Date:** 2026-10-17 · **Tree:** HEAD 19806a8 + the WO-004 working tree (62 status lines apart from `.swdev/log/*`, unchanged by this review)

## Verdict

**forward** — 0 Blockers · 2 Shoulds · 5 Notes. No contract delta (no `contracts/` file in `git status`; `IProgressStore` implemented, the rest consumed). G-01, G-12 (`:play` main: `:contracts` + coroutines, which §5 allows), G-13 (only `:store` imports `java.io` / `java.nio`), G-14 (no clock read or sleep in `play/`, `store/`, `app/` main) and G-17 (no canvas scale / rotate) hold by grep. The save path, the hit order and the recreation paths held under two mutations (below).

## Checklist applied

Requirement fit (16 acceptance IDs read against REQ-003/024/025/026/050/040/041/045) · seam table S-17…S-24 against code · architecture G-01/12/13/14/15/17, O-04 · directives D1–D6 · contract frozen check · error paths (store read/write boundary, writer stop) · integration (`AppGraph`, `MainActivity.onStop` on the nullable controller, `BackHandler`) · library behaviour (kotlinx.serialization defaults, `Files.move`, foundation pointer passes) · verifier self-test (`tools/process_death_check.sh`) · the two corrected checks.

## Re-derivation record

| What | Result |
|---|---|
| `:store:testDebugUnitTest :play:testDebugUnitTest :content:testDebugUnitTest :contracts:test --rerun-tasks` (08:24) | exit 0; XML read: store 12, play 79, content 64, contracts 2, 0 failed / 0 skipped |
| Kit hashes (sha1 prefix) for `TopBarKitTest` 3a5965e60251, `OverviewKitTest` 512851067d63, `ResumeKitTest` a1db980cdaaa, `FileProgressStoreKitTest` ee542dc74674 | equal the WO log values |
| `bash tools/process_death_check.sh` (dev image, APKs rebuilt first) | phase1 passed, force-stop, phase2 passed in a new process; exit 0; reset last |
| **Mutation 1** (scratch edit, reverted): `FileProgressStore.write` replaces the `Files.move` with `tmp.delete()` (a store that never persists) | death check **FAIL, exit 1**: phase 2 `-2`, `ComparisonFailure … expected:<shapes-warmup-1> but was:<shapes-mini-1>` — the right reason. Source restored, sha1 243d216c… equals the start |
| **Mutation 2** (scratch edit, reverted): `PointerRouter` guard `if (controller.overviewOpen) return@awaitEachGesture` made `if (false)` | `OverviewKitTest` on the dev image: **2 of 6 fail** — the REQ-024.A2 token test and `theGridClosesToTheSamePuzzleStateAndNothingUnderItReacts`. Source restored, sha1 2a07ccfd… equals the start |
| Device hygiene | `device_reset.sh` then `--check` clean before and after each device step; no emulator booted, killed or switched; APKs rebuilt from the restored tree |
| Not re-run by me | the control and `--negative-run` modes, the floor image (the implementer's run log has them; the main run and mutation 1 exercised the same verdict function) |

## Substance audit per token

The quantifier lines were audited at their adversarial path; the tests do assert what the REQ line means.

| Token | Test | Verdict |
|---|---|---|
| REQ-003.A1 | `BrowsingKitTest` (every puzzle from every puzzle, JVM); `OverviewKitTest` (device) | substantive |
| REQ-003.A2, REQ-025.A1 | `ResumeKitTest` (four states through every door, the writer lagging, then a restart); `ResumeScreenKitTest`; `ProcessDeathKitTest#phase2` (real new process, fixture never the fresh start's puzzle, mirror included) | substantive; mutation 1 proves the death leg can fail |
| REQ-024.A1 / A3 | `BrowsingKitTest`, `TopBarKitTest` (each TYPE-006 state of the last puzzle) | substantive |
| REQ-024.A2 ("always") | `OverviewKitTest#theTopBarAlwaysShowsTheCurrentPuzzlesNameAndNumber`: the grid-open leg reads pixels and bar-outside-overview, not `isDisplayed`; wrap, long press, recreation, grid scrolled, recreated with the grid open | substantive; mutation 2 fails it |
| REQ-025.A2, A3 | `ResumeKitTest`; A3 seeds a piece across the outline, keeps the others, checks In progress and the saved correction, at the start and via the grid, then after a restart | substantive |
| REQ-026.A1 ("never") | `SolvedViewKitTest` (first frame of: › open with stale placements, cold start, `recreate()` three frames into the celebration, ‹/› back after another solve; 60 frames watched; pixel check) + `ResumeKitTest` | substantive, adversarial path taken |
| REQ-026.A2, REQ-050.A1/A2/A3 | `SolvedViewKitTest`, `TopBarKitTest` (hold 600 ms), `OverviewKitTest` (pixel colour per state), `BrowsingKitTest` (saved progress unchanged after three choices) | substantive |
| REQ-040.A1, 041.A1, 045.A1 (re-covers) | `AppLaunchKitTest`, `TopBarKitTest`, `BrowsingKitTest`; content tokens kept | substantive (see N4) |

**Corrected check 1** (design review F5(b) reversed): sound. `traceability-rules.md` §2 rule 3 requires "≥1 covering test under that code home", so the `content/` tokens of REQ-040.A1 / 041.A1 satisfy it and no `build-map.md` row is needed.
**Corrected check 2** (`TopBarKitTest` Restart-pill pin): sound. I read the new pin: a kit-owned full-set puzzle validated through the real engine, hosted at 731 dp, both legs (margin runs Restart; drawn shape does not), a missing case fails loudly, no `assumeTrue`. The measured cause (board 577 dp, a piece cannot reach the pill) is arithmetic I re-derived from the test's own comment. No product finding behind it.

## Findings

### S1 · Should · `FileProgressStore.readOrFresh` / `write`, `ProgressDocument.toProgress` — TASK-017 (ADR-003 row, ai+inform)
**Observation.** Read is all-or-nothing and strict; the first save after an unreadable read overwrites the file; the store writes whatever it is given (decisions.md 2026-10-15 "The store does not check the values it is given"). The three together mean one bad value or one failed read erases every puzzle's progress, silently: a negative `time` / `today` / `allTime` written once makes the next launch read the whole file as fresh, then the first save replaces it. That row rated the cause "an upstream programming error" and accepted it under D6. But WO-008 will compute these seconds from the injected `Clock`, and a device clock change makes a negative delta ordinary rather than a bug. The same hole opens for any transient read exception (the catch is `Exception`, not "file corrupt"). The contract's must-never ("lose one puzzle's progress when another is saved") is the same line.
**Proposed resolution.** Cheapest: before the first overwrite of an existing, non-empty file that failed to read, rename it to `progress.json.bad` (one generation). This keeps ADR-003's fresh-install behaviour and every row-11 case; add one pin ("the unreadable file is kept aside"). Alternative: validate on the write side too (drop a save the read side would reject) and tell WO-008. If neither is wanted, record it on the G4 surface as an accepted risk with WO-008 named, since the accepting row does not mention the clock case.

### S2 · Should · `tools/process_death_check.sh` — TASK-022 (a verifier whose pass can be stale)
**Observation.** The script installs `app/build/outputs/apk/.../*.apk` and never checks that they were built from the current tree. The only guard is "file exists". The APKs I found on disk were dated 08:08 / 08:10, before the TASK-022 hand-back; a run on them would print PASS for code that is not the reviewed code. It is outside the verifier budget (no manifest entry), so nothing else re-checks it. This is a "pass that can be stale" in the one channel that proves REQ-003.A2 / REQ-025.A1 across a death.
**Proposed resolution.** Exit 2 when either APK is older than the newest file under `app/ play/ store/ content/ kernel/ contracts/ src` (or run the `assemble` tasks inside the script), and print the two mtimes on the PASS line. Test & Verify should rebuild before running it either way.

### N1 · Note · `PlayController.load` / `Persistence.saveLastOpened`
`saveLastOpened` is queued unconditionally on every open, so each ‹ › tap, and each launch, rewrites the whole document with an `fsync` even when the id is unchanged (`keep()` already skips a no-change puzzle save). Off the main thread, so harmless today; skip it when `persistence.lastOpenedId == next.id`.

### N2 · Note · `process_death_check.sh --positive-control`
The control passes on any "ran and failed" phase 2 (e.g. a launch timeout would do). Mutation 1 shows the main run fails with the right message, so the control is honest for what V-01's control claims; requiring the failure text to contain the `puzzle shown` assertion would make it prove the right cause.

### N3 · Note · `PlayController` secondary constructor and `memory` map (D3)
A memory-only controller path lives in product code solely so the WO-003 kits compile unchanged; no REQ requires it. Design S-1Δ names it, so it is accepted; retire it when those kits move to a store double.

### N4 · Note · `TopBarKitTest` REQ-041.A1 (device)
The device re-cover asserts "warm-up" for the four puzzles, not rating 1 (the REQ-041 statement); `BrowsingKitTest` and the `content/` token do. No gap in the WO's coverage.

### N5 · Note · `Persistence.enqueue`
If the writer ever dies (a store `Error`), the next `save` throws `IllegalStateException` out of `keep()` on the main thread, so a tap crashes the app. This is S-19 / G-15 as designed (a programming error), listed only because the player is a child and the alternative (drop the save) is milder.

## Triage (orchestrator, 2026-10-17 08:33)

- **S1 → accepted for WO-004 as a G4 risk; carried to WO-008 as a must-do.** The `IProgressStore` KDoc makes unreadable state a fresh install, and the must-never it names is "when another is saved", which the one-writer path keeps (mutation 1 and the row-11 pins). In this WO nothing computes a time: Restart and Retry write 0, the rest passes through (O-04). So the negative-time vector cannot be reached until WO-008 brings the clock. The `.bad` copy is read by nothing, so it would not give a child their progress back (D6). The transient-read case is residual risk, the same class as design §3's "a kill within milliseconds of a lock". **Carry:** WO-008's design must say which side guarantees that no value the reader rejects is ever written (a clamp in the time math, or a write-side check that fails the write). `decisions.md` row of 2026-10-17.
- **S2 → recirculate to the TASK-022 implementer** (same context resumed, fix in place): the script refuses (exit 2) to judge APKs older than the newest source or build input, and prints their times on the verdict line. Then the three modes are re-run on the dev image. Test & Verify reruns the whole death channel on both images from the final tree anyway.
- **N1** (an unchanged last-opened id rewrites the file): no action; harmless, off main.
- **N2** (the control accepts any ran-and-failed): declined. Matching the kit's assertion text would tie the harness to a kit message string. The cause is proven by the reviewer's mutation 1.
- **N3** (the memory-only constructor): accepted by S-1Δ; carried, retire it when the WO-003 kits move to a store double.
- **N4** (device REQ-041.A1 asserts "warm-up", not rating 1): no action. A1's literal is "the first four puzzles after the mini puzzles are warm-ups", which is exactly what the test asserts; rating 1 is the statement's, covered by `BrowsingKitTest` and the `content/` token.
- **N5** (a dead writer throws on the next save): as designed (S-19, G-15, decisions.md 2026-10-15 "`Persistence`'s cache and its stop path").

## Handoff

- **Scope:** WO-004 whole change: `store/`, `play/src/main`, `app/src/main`, the kits' verdict logic, `tools/process_death_check.sh`, the three build files.
- **Inputs read:** the WO, design (+ write-back), `decisions.md` 2026-10-15 / 17 rows, REQ-003/024/025/026/050/040/041/045, `IProgressStore`, architecture G/O, directives, traceability rules §2, the diff and untracked files named in the brief.
- **Result:** `C:\GitHub\AI\TangramPlaytime\reviews\WO-004-code-review.md`
- **Status:** forward (Test & Verify). S1 for triage (orchestrator: fix in TASK-017 or accept on the G4 surface); S2 to the TASK-022 author or fold into Test & Verify's procedure.
- **Counts:** Blockers 0 · Shoulds 2 · Notes 5.
- **Traceability delta:** none (no token moved or added by this review; 16 IDs each covered by a substantive test).
- **Notes:** every probe was reverted and verified by sha1 and `git status`; the floor image and the control / negative-run modes were not re-run by me.
