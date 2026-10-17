# WO-005 plan review (fresh, mechanical)

**Reviewer:** plan-reviewer (Sonnet) · **Date:** 2026-10-17 · **Plan:** `tasks.md` § WO-005 (TASK-023..TASK-026) + `workorders/WO-005-briefs.md`

## Script output (verbatim)

```
plan-check: WO-005 — 4 task(s) (0 [P]), mode Light, 4 in-scope acceptance ID(s)
plan-check: GREEN (0 blocker(s), 0 warning(s))
```

trace-check (`--wo WO-005`, PYTHONUTF8=1): code home of `#DevTools` parsed as `play/` (no directory-theater error); `RED: REQ-046 uncovered A1..A4` as expected before the kit lands. build-map v1.5 delta recorded 2026-10-17T10:19:25.

## Mechanical checks

| Check | Result |
|---|---|
| 1. Coverage both ways | A1 (023, 024, 025, 026), A2 (025, 026), A3 (023, 024, 025, 026), A4 (023, 024, 026): all four land; every task names REQ-046 |
| 2. Done-checks green at their step | TASK-023 and TASK-025 are "met at landing" with a stated red window (each states its only expected compile errors); TASK-024's green verdict names tests already landed by TASK-023; TASK-026's names tests landed by TASK-023/025. No Done-check names a later test |
| 3. Declared inputs land together | Bundled `Tangrams/*.json`, the two APKs, `app/src/main`, `app/build.gradle.kts`, `strings.xml` are listed where read; the task that changes the strings (024) precedes the task that reads them (025, 026); `MainActivity.kt` (026) is in the staleness inputs of `ReleaseBuildKitTest`, whose fresh-APK rule is in TASK-026's Done-check (4) |
| Red windows honest | `play/src/test` (023 to 024), `play/src/androidTest` (025 to 026), `app/src/androidTest` (compiles from 025, runtime-red until 026): each matches the seams' delivery column. Seam note 3 confirmed by grep: `PlayScreen(` call sites in tests are positional with a trailing `Modifier` (`devAid` defaulted, last); `ShownControls(` in `ControlsScaffoldTest.kt` uses two args (new field defaulted); `PointerRouter(` is constructed only in `PlayScreen.kt`. "In-tree tests this WO changes" (only `KitScreen.kt`) holds |
| Seams name their delivering task | S-25..S-30, S-10Δ2, S-22Δ2, S-15Δ2 all have a Delivered-by cell; S-25 and S-26 are split 024/026 explicitly, and the TASK-024 brief says "do not add the four composables now" |
| Mode | Light; none of the four Full reasons is stated or holds (existing `:play`, consumed interfaces only, ordinary `PuzzleProgress`, hard-stop 1 read and named in the WO) |
| `[P]` honesty | none, with reasons |
| Briefs point, do not paraphrase | The briefs cite seams, rows and `decisions.md` by date and bold title. All eight WO-005 titles (Q1..Q8) and all 12 older titles the briefs cite exist in `decisions.md` verbatim |
| Toolchain and channel proofs | g3, d37, f26 are in the WO log (run log rows); minSdk-26 floor image `Phone_API_26` named in the channel table and in TASK-026 Done-check (3) |
| `guard.json` | `test_globs` covers `*/src/test/*` and `*/src/androidTest/*` (all kit paths); `product_globs` gains `*/src/debug/**` as the decisions row says |

## Findings

| # | Category | Severity | Target | Observation | Proposed resolution |
|---|---|---|---|---|---|
| F1 | Task shape | Should | TASK-026 | One task carries the overlay, the DEV button, the sheet with a focused text field and keyboard, the router and hit-order change, `PlayScreen`, `HitTest`, the `MainActivity` wiring and two device channels. The brief itself names two separate "riskiest" items (sheet vs router; keyboard vs `safeDrawingPadding`). Still one seam family and one context, but the largest task by far | Keep as one if the orchestrator accepts the context cost; otherwise split at the S-22Δ2/S-10Δ2/S-15Δ2 line (router, screen, wiring) from the three composable members, with the kit already landed. No plan change is required to dispatch |
| F2 | Done-check | Should | TASK-024 | The Done-check relies on the JVM line rebuilding both APKs first (seam note 4); a bare `:play:testDebugUnitTest` makes `ReleaseBuildKitTest` refuse stale or missing APKs and shows RED. The Done-check cell says "from the JVM line of the briefs' channel table" but does not state the consequence | Keep the line as the only accepted run; Test & Verify and the Code Reviewer should be told the bare Gradle task is not a verdict. One sentence in the dispatch to the implementer and to the verifier is enough |
| F3 | Seam clarity | Should | S-25 / TASK-024 vs TASK-026 | S-25's members are all declared `internal abstract`, but TASK-024 adds only `sheetOpen` and `onButton` and the four composable members arrive in TASK-026, with `DevAid` overrides. That compiles, but `DevAidHook.kt` and `DevAid.kt` are edited by two tasks and TASK-024's Done-check does not say that the class must be instantiable without them | State in the TASK-024 row that the abstract class has exactly `sheetOpen`/`onButton` until TASK-026 adds the composable members as abstract plus overrides in the same task, so the build never has an abstract member without an override |

Counts: **0 Blockers · 3 Shoulds · 0 Notes**

## Verdict

**forward.** No Blocker: coverage both ways, Done-checks and declared inputs all hold, red windows match the delivery column, mode reason is honest. The three Shoulds are triage items for the orchestrator, not preconditions.
