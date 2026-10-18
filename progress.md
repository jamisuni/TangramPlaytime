# Progress Log — TangramPlaytime

Append-only. One entry per interaction:

- the human's input, **VERBATIM**
- a **~10-line** summary of what the AI did

This log is the ground truth the framework reads to improve itself
(see `C:\GitHub\AI\SWDev\framework\feedback\`).

---

## 2026-10-11 — P1 kickoff: trace-check, fresh-eyes review dispatched (part 1)

**Human (verbatim):**

> WE ARE IN PHASE P1 — ACCEPTANCE OF A LOCKED COLLECTION (intake path C,
> C:\GitHub\AI\SWDev\initnewproject.md §2b, SWDev v0.20). Read AGENTS.md
> first. The requirement collection is Requirements/ in SwReqCollector
> format v2, v1.2 (locked 2026-10-01; REQ-048 re-signed through CHG-001/002,
> 2026-10-11). It IS the spec: read its CLAUDE.md,
> requirements.md (index), features.md (#Tag tree), reqs/REQ-NNN.md,
> req_types.md, views/digest.md, views/trace.md and views/lock-receipt.md.
> Every locked level (BIZ/FUN/UI) is Contract. Never write into that
> folder — a change is a chg-draft in capture-outbox/.
>   1. Run trace-check and confirm it parses the collection (51 REQs, 47
>      locked, RED only for uncovered acceptance — no product tests yet).
>   2. Dispatch the fresh-eyes requirements review (requirements-reviewer
>      subagent, read-only on the collection). Write it as
>      Requirements/reviews/review-01.md — the findings table with the
>      Verdict column I fill per finding; every finding whose verdict will
>      be `chg` also becomes a chg-draft in capture-outbox/. Never edit a REQ.
>   3. Present me the digest (views/digest.md), the attention points
>      (views/trace.md), the lock receipt, and the review's Blockers; then
>      ask the artifact-named question:
>      "Accept collection v1.2 for TangramPlaytime (G1)?"
>   4. Record my yes in progress.md with approver + date. Then P2 (the stack
>      is DECIDED: Kotlin + Jetpack Compose native, the prototype's locking
>      logic ported — DI-8/SRC-016; the WO order is the one in KICKOFF.md):
>      architecture.md (directives ack, guardrails from AGENTS.md rules 1–13,
>      the governed interface registry with tiers), build-map.md (code homes
>      per #Tag), design-inputs.md §2 (the Contract-delta — derive it from
>      the inputs, do not import the first run's), the floor channel (API 26)
>      proven, governance in force — then the G2 question.
> Light mode is the default for every WO; Full needs one of the four reasons.
> Owner questions are checkpoint items (≤5, product-visible, defaults after
> 3 days); lists to proof-read go to review-notes.md. The four scripts of a
> work order are duties (AGENTS.md). End every turn with the one signal.

**What was done (summary):**

- Session start: AGENTS.md, KICKOFF.md, the framework read list (principles,
  lifecycle, directives, gates-and-autonomy, initnewproject §2b, the Windows
  page, platform-notes/android.md), governance.md (in force, ai-mastered),
  all project ledgers. Drift check: framework README v0.20 = AGENTS.md v0.20.
- Read the whole collection: index, features.md, req_types.md, all 51 REQs,
  digest, trace, lock receipt, sources, CHG-001/002, SRC-016.
- Step 1: `trace_check.py --project .` → 51 REQs (47 locked · 4 withdrawn ·
  BIZ 7 / FUN 29 / UI 11), views fresh, governance in force; RED only for
  uncovered acceptance + the missing contract baseline (frozen at G2).
  `validate.py` 0 errors, 66 lock-gate-v2 warnings. A false req-lint FAIL
  (validate.py UnicodeEncodeError on cp1252 stdout) disappears with
  `PYTHONUTF8=1` → proposals.md entry.
- Step 2: `requirements-reviewer` subagent dispatched (fresh context, read-only
  on the collection; writes `Requirements/reviews/review-01.md` with the
  Verdict column empty and chg-drafts into `capture-outbox/`). Running at the
  time of this entry; part 2 of this entry follows with its result.
- Pre-read for P2: Spec/00–03, 05, Study/06, 08, puzzle schema, a puzzle
  file, interface-planning, work-order template, trace-check SKILL. Toolchain
  inventory: AVDs Phone_API_26 + Medium_Phone_API_37.0, images android-26 /
  android-37.0, build-tools 36.0.0, Android Studio JDK 25, Gradle 9.8 wrapper
  cached; no emulator booted.
- proposals.md: two entries (validator encoding; principle 16 lacks an
  "in-flight" signal).

## 2026-10-11 — P1 part 2: review-01 delivered, G1 surface presented

**Human (verbatim):** *(no new input; continuation of the kickoff above)*

**What was done (summary):**

- `requirements-reviewer` station (Fable, fresh context, ~13 min) wrote
  `Requirements/reviews/review-01.md`: 38 findings — **4 Blockers** (F1
  REQ-038 validity rules live in an unsigned script; F2 six TYPEs Draft /
  Inferred under all 47 locked REQs; F3 distribution channel open while
  REQ-048 / REQ-001 A2 presuppose Google Play; F4 REQ-002 A1 "drag, turn and
  drop only" contradicts REQ-018 mirror), 21 Shoulds, 13 Notes; Verdict column
  empty for the owner; no misleading abstract.
- 21 `chg-draft-*.md` written to `capture-outbox/` (`origin: review`,
  `raised_in: G1 review-01 F<n>`), one per Blocker/Should that changes locked
  text; four Shoulds are owner questions without a draft (F22–F25).
- `validate.py`: 0 errors, 104 warnings (66 + 38 "no verdict" rows, expected).
  `views.py` re-run (trace.md and lock-receipt.md now list the 38 unverdicted
  findings). trace-check: views fresh, 21 outbox drafts noted, RED only for
  uncovered acceptance + missing baseline.
- Observation for the owner: F3's answer already exists in SRC-016 / DI-8
  ("Google Play"); the fresh-eyes reviewer could not see evidence/ by rule.
- G1 surface presented (digest, attention points, lock receipt, Blockers) and
  the artifact-named question asked: "Accept collection v1.2 for
  TangramPlaytime (G1)?" — waiting for the owner.

## 2026-10-11 — Owner asks for a human-readable top 7

**Human (verbatim):**

> Now thum of rule.. I do not remember requirement numbers.. plz I need these to be shown me more clearly as human readable here.. and lets start from top 7 you think are most important to me look

**What was done (summary):**

- Presented seven findings in plain words (what · found · recommend · give
  up): channel (F3), unsigned numbers (F2), validity rules in a script (F1),
  the missing "mirror" (F4), Android floor (F7), Finnish texts (F17),
  licences (F24); offered the remaining 31 in the same shape or by delegation.

## 2026-10-11 — Owner's verdicts on the top 7 and "rest ok"

**Human (verbatim):**

> (1) aye we plan for google play (2) play has been ok for 6 years old tester (3) yes (4) yes mirror is need (5) android 8 is more than enough, can go way higher if it simplifies development (like example Android 10) (6) lets go english only, and finnish is there just as reference to make sure language can be change but translations for fin are not important. (7) ok... (x) rest ok also. Plz progress ahead as automatic as possible

**What was done (summary):**

- `capture-outbox/src-draft-g1-owner-answers.md`: the words above verbatim,
  mapped item → finding.
- Verdicts filled in `Requirements/reviews/review-01.md` (all 38): 4 Blockers
  `chg`; 20 Shoulds `chg`, F23 `reject` (locked TYPE-006 Retry → New stands);
  9 Notes `reject` (as written), F27/F31/F34/F38 `defer`. A verdict note under
  the table names where the owner's answer changed a draft's direction.
- Drafts amended: store channel = Google Play (F3); platform floor = Android
  8.0 confirmed (F7); type dictionary confirmed as is, with the six-year-old
  tester as firsthand evidence (F2); F17 rewritten to "English is Contract,
  Finnish a reference translation". New drafts: `chg-draft-req047-english-
  contract.md` (owner decision, origin build), `chg-draft-licence-notice-and-
  assets.md` (F24), `chg-draft-req032-how-to-play-text.md` (F22). Outbox: 24
  chg-drafts + 1 src-draft.
- `decisions.md`: minSdk stays API 26 (row 5, ai+inform: nothing this game
  needs is gated higher; raising it only shrinks the audience); how-to text
  by the build ≤ 60 words (row 12, owner-grade); F23 kept as locked; the
  F5–F38 verdict delegation.
- AGENTS.md rule 13: dated amendment quoting the owner (English Contract,
  Finnish reference strings still added for every text).
- views.py re-run; validate 0 errors / 66 warnings (no verdict gaps);
  trace-check RED only for uncovered acceptance + missing baseline.
- G1 not yet signed: "progress ahead" is not the artifact-named yes the floor
  needs — re-asked once, precisely.

## 2026-10-11 — G1 ACCEPTED

**Human (verbatim):**

> G1 Accepted! Plz progress with development

**Gate record:** **G1 — Accept collection v1.2 for TangramPlaytime: YES.**
Approver: Jami (owner) · Date: 2026-10-11 · Artifact: `Requirements/`
collection v1.2 (47 locked REQs, 4 withdrawn, TYPE-001..007), review-01 with
all 38 verdicts, 24 chg-drafts + 1 src-draft pending in `capture-outbox/`.
This binds TangramPlaytime to the already-locked collection; the pending
deltas are "lock behind build" until the capture side applies them.

**What was done (summary):**

- Gate recorded (above); AGENTS.md "Current phase" → P2.
- P2 started: architecture.md, build-map.md, design-inputs.md §2, the
  Gradle skeleton with the contracts module, the floor channel (API 26)
  proof, guard.json tiers + `--freeze` (details in the next entry).

## 2026-10-11 — P2 done: architecture locked at G2 (ai+inform)

**Human (verbatim):** *(no new input; continuation of "G1 Accepted! Plz progress with development")*

**What was done (summary):**

- `architecture.md` v1.0: directives ack (no deviations); G-01..G-17 from the
  owner's rules 1–13 + the device lessons; component map `:contracts`,
  `:kernel`, `:content`, `:store`, `:play`, `:app`; O-01..O-08; registry
  `IPuzzleLibrary` (+ puzzle format) **locked**, `ILockEngine`,
  `IProgressStore`, contract types **notify**; ADR-001..006 (port the
  prototype lock search per TYPE-004; one module per code home; persistence
  format in WO-004; puzzles as JSON assets with a JVM validity test; system
  font; injected Clock). `build-map.md` v1.0: code homes per #Tag, WO order.
- `design-inputs.md` §2: 12 superseded input statements (per-level lock
  parameters, V11 as content rule, `navigator.vibrate` → permission-free
  haptics, Google font, 44 dp gear, old store name, mouse/keyboard aids,
  Finnish-as-Contract, 20 vs 25 puzzles, save cadence, web-only DEV
  clauses), no open forks, 6 agreements.
- Guard: the scaffold had `architecture.md` locked before P2 could author it;
  opened `unlocked` under row 4 for the window (decisions.md), closed at the
  freeze; proposals.md entry.
- `slice-implementer` (Opus, ~10 min + 1 min follow-up): Gradle skeleton
  (AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00, Gradle 9.8, JDK 25,
  compileSdk 37, minSdk 26), the three governed interfaces + Types.kt with
  banners and REQ-traced KDoc, scaffold tests, MainActivity, FloorChannelTest.
  Runs: `test` 6/6 green; `:app:assembleDebug` green;
  `:app:connectedDebugAndroidTest` on `Phone_API_26` (Android 8.0.0) 1/1
  green — the floor channel is proven. Run log: `.swdev/run-log/P2-skeleton.md`.
- Decisions (ai / ai+inform rows): tiers; module layout + versions; app id
  `app.tangramplaytime`; minSdk 26 kept; androidx.core's injected signature
  permission removed from the merged manifest (G-01 / REQ-010.A2 stay
  literally true); tray turns not persisted; the implementer's shape
  refinements accepted; **G2 signed** (row 4).
- `.swdev/guard.json`: contract tiers mirrored; `trace_check.py --freeze`
  run (baseline written); trace-check now RED only for uncovered acceptance
  (no product tests yet); 6 test files scanned; 25 outbox drafts noted.
- Emulator shut down (`adb emu kill`); `adb devices` empty.
- KICKOFF.md: state after this session + the paste-in brief for the WO-001
  session (one orchestrator context per WO). Memory notes saved (briefing
  style; the owner's G1 decisions).
- Owner informed on the G2 surface (ai+inform, default stands 2026-10-14) and
  prompted to commit the snapshot.

## 2026-10-11 — WO-001 #Locking opened (P3, fresh orchestrator context)

**Human (verbatim):**
> WE ARE IN PHASE P3 — WORK ORDER WO-001 #Locking (TangramPlaytime, SWDev
> v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
> files), then governance.md, architecture.md, build-map.md, design-inputs.md
> §2, decisions.md, the last progress.md entry, capture-outbox/ (the pending
> deltas and the owner's G1 answers), platform-notes/android.md.
> WO-001 scope: #Locking subtree (#AnchorLock REQ-019, #GoHome REQ-020,
> #SolvedCheck REQ-022) plus the geometry rules of TYPE-001/003/004 in
> :kernel behind ILockEngine (:contracts) — the prototype's findLock/validAt/
> anchors ported per ADR-001, following the TYPE-004 text. Pure JVM; no UI.
> Mode: Full, reason (b) a governed interface touched (ILockEngine is
> implemented; its shape may change — notify tier, log the contract-delta).
> Design inputs: DI-1 (prototype template, behaviour reference only), DI-4
> (Study/06, frozen), DI-5 (tools/tangram_geom.py reference math).
> Verification channels at G3: JVM unit tests (:kernel, :contracts); the
> device channels are not needed for this WO (no UI) — say so in the WO.
> Stations as subagents: design-author → design-reviewer → wo-planner →
> plan-reviewer → [acceptance-test-author ∥ slice-implementer] → code-reviewer
> → test-verify → traceability-auditor. The four scripts are duties. ≤5
> checkpoint items, product-visible only. End every turn with the one signal.

**What was done (summary, 11:11–):**

- Read AGENTS.md, the framework's P3 files (principles, lifecycle, directives,
  gates, definitions, orchestration, task-decomposition, orchestrator,
  handoff contract, initnewproject §2b, the Windows page), governance,
  architecture v1.0, build-map v1.0, design-inputs §2, decisions, the last
  progress entry, the outbox (25 drafts; the owner's G1 answers), android.md,
  REQ-019/020/022, TYPE-001/003/004, the frozen `ILockEngine.kt` / `Types.kt`,
  the station pack. Drift check: framework v0.20 = project v0.20.
- `workorders/WO-001.md` written: scope REQ-019 A1..A4, REQ-020 A1..A2,
  REQ-022 A1 + TYPE-001/003/004 in `:kernel`; Full (b); channels = JVM unit
  tests only, device channels explicitly not needed; DI-1/4/5; no checkpoint
  items; lock behind build noted (`chg-draft-lock-type-dictionary.md` on
  TYPE-004, `chg-draft-req020-interrupted-drag.md` on REQ-020).
- Toolchain proof (G3): `:contracts:test :kernel:test --rerun-tasks` green,
  HEAD bddffef; run log `.swdev/run-log/WO-001.md`.
- Design Author dispatched (`design-author`, Opus) — the fountain continues
  in this entry's follow-ups below.

**Follow-ups, same interaction (11:24–12:50; no new human input):**

- Design (`design-author`, Opus) → forward; design review (`design-reviewer`,
  Fable) found **1 Blocker**: the 360°/180° angle-sum corner rule misses a
  real outline corner at (2,2) of the shipped `shapes-warmup-4` (180° in two
  separate wedges). Same author resumed, rule replaced by the contiguity of
  covered directions; second look → forward, 0 B / 3 S. `design-inputs.md`
  v1.1 item 13 (angle-sum rule superseded; the Python tool's blind spot →
  WO-002). Three AI decisions rows (PG-only mirror, tie rule, G-15 requires).
- Plan (`wo-planner`, Opus): TASK-003 kit in two stages ∥ TASK-001 geometry
  helpers → TASK-002 `LockEngine`; `workorders/WO-001-briefs.md`. Seam-table
  header renamed so plan-check's seam check runs; `proposals.md` entry on
  three plan-check misreads. plan-check GREEN (orchestrator + reviewer runs);
  plan review (`plan-reviewer`, Sonnet) 0 B / 2 S (ordering, handled in
  dispatch). **G3 approved** under governance row 7 (`decisions.md`).
- P4: TASK-001 (`slice-implementer`, Opus) → 4 helper files, 331 lines;
  kit stage 1 (`acceptance-test-author`, Opus) → `KitFixtures.kt`,
  `GeometryKitTest.kt` 20 G-04 pins, compiled green. Orchestrator run r1
  (`.swdev/run-log/WO-001.md`): 20 + 1 + 10 + 2 green → **TASK-001 done**.
- Scope note: **REQ-020.A2 moves to WO-003** — the seam-level oracle is G-15
  conformance, not "no message shown" (`decisions.md`, WO Scope, tasks.md).
- Kit stage 2 (`LockEngineKitTest.kt`, 14 tests, 6 tokens) landed 12:50;
  TASK-002 `LockEngine.kt` (192 lines, all 7 members) → forward 12:55;
  orchestrator run r2 12:57: 54 tests green (14 kit + 20 geometry + 18
  scaffolding + 2 contracts), tokens exactly the six in-scope IDs, plan-check
  GREEN with the kit compiled.
- trace-check preview RED on bookkeeping only: Scope notes leaked IDs into
  the parser; `#SolvedCheck` inherited `play/`. Fixed (notes moved;
  `build-map.md` v1.1 `#SolvedCheck` → `kernel/`, **one notify-tier
  contract-delta**, baseline re-frozen) → GREEN 6/6.
- Code review (Sonnet) 0 B / 0 S / 3 N; Traceability Auditor GREEN for the
  WO (project-wide RED on later WOs' REQs); Test & Verify 6/6 PASS from run
  log r2 (hashes matched, no re-run); verifiers: none exist yet.
- **Close 13:04:** `run_verifiers.py` (none), `wo_metrics.py --write` (+
  hand-filled judgment rows; same-day window annotated), DoD all ticked,
  **G4 under row 9 (`decisions.md`)**; TASK-001/003 done, TASK-002
  `implemented (unverified)` for the unsigned TYPE-004 dictionary; KICKOFF.md
  WO-002 brief; AGENTS.md phase line; proposals (corrected checks none;
  wo-metrics window). Three log stamps corrected from telemetry.

### G4 surface — WO-001 #Locking (outcomes first)

**Satisfied (JVM channel, run r2, 54 tests / 0 failures):** REQ-019 A1 (lock
exactly on an outline corner, a neighbour corner, the warmup-4 two-wedge
corner, with the 30 dp floor), A2 (never overlaps / never sticks out, incl. a
sweep), A3 (no hidden anchor — two discriminating poses), A4 (the seventh
piece locks into its hole, 35 holes); REQ-020 A1 (every drop is Locked or
None); REQ-022 A1 (a different exact cover, built lock by lock, is solved).
**Moved:** REQ-020 A2 → WO-003 (the Play screen is the honest oracle).
**Deliberately not touched:** `ILockEngine.kt`, `Types.kt` (implemented as
frozen); every screen, gesture and animation; puzzle loading; persistence;
TYPE-005/006/007; the Python tools. **Risk notes:** the TYPE-004 values are
owner-confirmed but unsigned → REQ-019 lines `implemented (unverified)` until
the dictionary CHG is signed; `stillValid` does only inside + no-overlap
(REQ-025's WO decides); the content tool's corner rule has the blind spot the
kernel fixed (WO-002). **Contract deltas:** 1 — `build-map.md` v1.1
(`#SolvedCheck` → `kernel/`). **Lock behind build:** 25 drafts awaiting a
capture session, none written by this WO. **Checkpoint items:** none.
**Diff:** `kernel/src/main/kotlin/app/tangramplaytime/kernel/{PieceShapes,
Polygons,Silhouette,LockRules,LockEngine}.kt` (+523), `Kernel.kt` (−3);
`kernel/src/test/.../{KitFixtures,GeometryKitTest,LockEngineKitTest}.kt` +
`scaffolding/` (+1,497); ledgers `workorders/WO-001*.md`, `designs/`,
`reviews/` (4), `tasks.md`, `decisions.md` (+7 rows), `design-inputs.md`
v1.1, `build-map.md` v1.1, `proposals.md`, `.swdev/run-log/WO-001.md`,
`KICKOFF.md`, `AGENTS.md`. **Snapshot prompt:** owner, please commit this
state as the WO-001 close (e.g. "WO-001 #Locking closed: :kernel lock engine
behind ILockEngine, kit 6/6, build-map v1.1").

## 2026-10-11 — WO-002 #Content opened (P3, fresh orchestrator context)

**Human (verbatim):**
> WE ARE IN PHASE P3 — WORK ORDER WO-002 #Content (TangramPlaytime, SWDev
> v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
> files), then governance.md, architecture.md, build-map.md, design-inputs.md
> §2 (incl. item 13), decisions.md, the last progress.md entry, KICKOFF.md
> "State after WO-001", capture-outbox/ (pending deltas), platform-notes/android.md.
> WO-002 scope: #Content subtree (REQ-007, 038, 039, 040, 041, 042, 045) in
> :content behind IPuzzleLibrary (locked tier — implement, never change):
> Tangrams/*.json as assets, the tangram-puzzle/1 parser (kotlinx.serialization,
> ADR-004), the load-time validity check (REQ-038 rules; pending
> chg-draft-req038-validity-rules-in-contract.md), ordering kind → rating → id.
> Design inputs: DI-5 (puzzle format, schema, the 13 puzzles, tools/). Also
> re-judge tools/tangram_geom.outline_corners + V11 against the contiguity
> corner rule (design-inputs.md §2 item 13) — a project-tool fix, not Contract.
> Mode: decide from the four reasons (a new Android library module with assets
> is reason (a) if :content counts as new; its skeleton exists from P2 — say so).
> Verification channels at G3: JVM unit tests (:content testDebugUnitTest loads
> every asset); device channels only if a task needs one — say so in the WO.
> Checkpoint candidates (≤5, product-visible): the puzzle count for the first
> release (DI-8 default 25, REQ-042 says ≥ 20). Stations as subagents; the four
> scripts are duties. End every turn with the one signal.

**What was done (summary, 13:55–):**

- Read AGENTS.md, the framework's P3 files, governance (in force), architecture
  v1.0, build-map v1.1, design-inputs v1.1 §2, decisions, the WO-001 close
  entry, KICKOFF "State after WO-001", the outbox (25 drafts, five on this
  WO's REQs), android.md, the seven REQs, the frozen `IPuzzleLibrary.kt` /
  `Types.kt`, the locked schema, `Spec/03`, the content skeleton, the Python
  corner rule and V11/V12. Drift check: v0.20 = v0.20. HEAD 9e983d2 (the
  owner committed WO-001).
- `workorders/WO-002.md` written: Implements REQ-007 A1..A2, REQ-038 A1,
  REQ-040 A1..A2, REQ-041 A1..A2, REQ-042 A2..A3, REQ-045 A1 in `content/`;
  `IPuzzleLibrary` implements (locked — never edited); **Full (b)**, reason
  (a) considered and not claimed (the `:content` skeleton exists since P2);
  channels JVM `:content:testDebugUnitTest` + `:contracts:test` + the Python
  tool channel, device channels not needed as kicked off; DI-5, DI-8.
- Scope moves and unverified lines recorded in `decisions.md` (rows 8, 20):
  REQ-039.A1 / REQ-045.A2 → WO-003, REQ-039.A2 / REQ-042.A1 → WO-009 (+ the
  content batch WO), REQ-038.A2 `implemented (unverified)` for the pending
  deletion CHG. Checkpoint item 1: first-release puzzle count, default 25.
- Toolchain proof (G3): `:content:testDebugUnitTest --rerun-tasks` green
  13:56; validator 13/13 — `.swdev/run-log/WO-002.md`.
- Design Author dispatched (`design-author`, Opus) — the fountain continues
  in this entry's follow-ups below.

**Follow-ups, same interaction (14:15–15:16; no new human input):**

- Design (`design-author`, Opus) → forward: Java resources bundled by a
  Gradle task and read through one `ClassLoader` path (no `AssetManager`,
  no device task); piece geometry through `ILockEngine` (`vertices`,
  `stillValid`); fail fast on a bad file; nine decisions rows. Design
  review (`design-reviewer`, Fable) → forward 0 B / 4 S, the reading path
  proven in a scratchpad build on AGP 9.4.1. `build-map.md` v1.2
  (`#Content` deps via `ILockEngine` — one notify-tier contract-delta).
- Plan (`wo-planner`, Opus): TASK-004 reading path → TASK-008 kit →
  TASK-005 parser/validity/library → TASK-007 tools fix; F1–F4 carried.
  Artifact channel proved in this tree (14:46). Plan review (Sonnet)
  2 B / 2 S on the kit row → fixed by the same planner → second look 0 B.
  plan-check GREEN. **G3 approved** (row 7, `decisions.md`).
- P4: TASK-004 (`slice-implementer`, Opus) → done (run r1 14:56: 2 + 2
  green; 14 bundle entries in the debug APK, release APK and AAR). Kit
  draft (`acceptance-test-author`, Opus): 52 tests, 15 mutants caught;
  landing in progress.

**Follow-ups, same interaction (15:21–15:45; no new human input):**

- Kit landed 15:21 (exactly ten tokens; red window as planned). TASK-005
  (`slice-implementer`, Opus) → forward 15:32: parser, loader (15 rules,
  sealed `LoadResult`), validity check through `ILockEngine`, library,
  790 product LOC; orchestrator run r2: 65 green, plan-check GREEN with
  the kit compiled. TASK-007 (`slice-implementer`, Sonnet) → forward 15:34:
  the contiguity corner rule in `tools/tangram_geom.py`, tool test pins all
  13 corner sets, validator 13/13 unchanged build orders.
- trace-check preview GREEN under `PYTHONUTF8=1` (req-lint dies on a cp1252
  console otherwise → `proposals.md`). Code review (Sonnet) 0 B / 2 S / 5 N
  → kit author fixed S2 (picture leg on the REQ-007.A1 test), N1 (a token
  demoted to a pin), added the S1 pin (unknown shape type = `JSON`;
  decisions row); run r3: 66 green. Corrected checks 2.
- Test & Verify (Sonnet) 10/10 PASS from r3 (hashes matched, no re-run);
  Traceability Auditor (Sonnet) `--wo WO-002` GREEN, no draft written.
- **Close 15:45:** `run_verifiers.py` (none), `wo_metrics.py --write` (+
  hand-corrected same-day rows), DoD all ticked, **G4 under row 9
  (`decisions.md`)**; TASK-004/007/008 done, TASK-005 `implemented
  (unverified)` for the pending REQ-038 validity-rules CHG; `build-map.md`
  #Content Built; `review-notes.md` row (the 13 puzzles); `KICKOFF.md`
  "State after WO-002" + the WO-003 brief; `AGENTS.md` phase line;
  `proposals.md` (plan-check cross-product, `\|` in seam cells, req-lint
  encoding, the `[P]` rule on read-only inputs).

### G4 surface — WO-002 #Content (outcomes first)

**Satisfied (JVM channel, run r3, 66 tests / 0 failures; artifact channel
14 bundle entries in the debug APK, release APK and AAR; Python channel
13/13):** REQ-007 A1 (every shipped puzzle is listed and meets the shapes,
picture and order rules, re-measured by the kit's own geometry), A2 (minis,
then warm-ups); REQ-038 A1 (the written rules — piece set, congruence,
overlap, edge-connectivity, titles, rating, kind, picture — each proven to
discriminate on a crafted file; **`implemented (unverified)`** until the
validity-rules CHG is signed); REQ-040 A1 (first entry rating 1), A2 (a
rating-1 full puzzle whose id sorts first still follows every warm-up);
REQ-041 A1 (four warm-ups follow the minis), A2 (each warm-up meets the
half-outline measure; a mean-based check is caught); REQ-042 A2 (five
categories), A3 (`shapes-square` listed); REQ-045 A1 (the first entry is a
mini whose tray holds three pieces).
**Carried, not covered here:** REQ-038.A2 (the tool's printed build order —
unverified on the pending deletion; validator evidence in the log);
REQ-039.A1, REQ-045.A2 → WO-003; REQ-039.A2, REQ-042.A1 → WO-009 and the
content batch WO.
**Checkpoint item 1 — answer by 2026-10-14 or the default stands:** how
many puzzles in the first release? Thirteen exist (none reviewed yet);
your 2026-10-01 answer was 25; the REQ says at least 20. **Default 25**;
the batch is authored in a Light content WO after WO-003 so you can solve
each one on the real Play screen.
**Deliberately not touched:** `IPuzzleLibrary.kt`, `Tangrams/puzzle.schema.json`,
`ILockEngine.kt`, `Types.kt`, every puzzle file; every screen; `:app` wiring
(WO-003); persistence; the prototype and the UI sheets (not regenerated,
G-06). **Risk notes:** the bundle read is proven on the JVM and by
APK/AAR listings, not yet on a device (WO-003 calls it once on the floor
image; R8 must keep the resources); the Kotlin congruence is stricter than
the Python V3 (boundary order, 45° turns) — new puzzles must pass both;
the locked `architecture.md` wording drifts ("assets", `LoadResult.Unreadable`)
— yours to fix, not a Contract change. **Contract deltas:** 1 —
`build-map.md` v1.2 (`#Content` depends on the kernel via `ILockEngine`;
status Built). **Lock behind build:** 25 drafts awaiting a capture session,
five on this WO's REQs, none written by this WO. **Corrected checks:** 2
(code review S2, N1). **Station ledger:** complete, 11 launches, every
station on its staffed tier.
**Diff:** `content/build.gradle.kts` (+47), `content/src/main/kotlin/app/tangramplaytime/content/{BundledPuzzles,PuzzleDtos,PuzzleParser,PuzzleLoader,ValidityCheck,Edges,PuzzleLibrary,PuzzleLibraries}.kt`
(+790), `Content.kt` (−3); `content/src/test/.../{KitTexts,PuzzleLibraryKitTest,PuzzleLoaderKitTest,ContentScaffoldTest}.kt`
+ `scaffolding/ParserScaffoldTest.kt` (+1,550); `tools/tangram_geom.py`
(+44/−17), `tools/tests/test_tangram_geom.py` (+67); ledgers
`workorders/WO-002*.md`, `designs/`, `reviews/` (5), `tasks.md`,
`decisions.md` (+13 rows), `build-map.md` v1.2, `review-notes.md`,
`proposals.md`, `.swdev/run-log/WO-002.md`, `KICKOFF.md`, `AGENTS.md`.
**Snapshot prompt:** owner, please commit this state as the WO-002 close
(e.g. "WO-002 #Content closed: :content puzzle library behind
IPuzzleLibrary, kit 10/10, tools corner rule, build-map v1.2").

## 2026-10-11 — WO-003 #Solving opened (P3, fresh orchestrator context)

**Human input (verbatim):**

> WE ARE IN PHASE P3 — WORK ORDER WO-003 #Solving (TangramPlaytime, SWDev
> v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
> files), then governance.md, architecture.md, build-map.md, design-inputs.md
> §2, decisions.md, the last progress.md entry, KICKOFF.md "State after
> WO-002", capture-outbox/ (pending deltas), platform-notes/android.md.
> WO-003 scope: the #Solving subtree not yet built — #Board, #Tray, #SizeMarks,
> #Drag, #TapTurn, #TwistTurn, #Flip, #LandingPreview, #CornerTeach,
> #SolvedPicture (the REQ ids those tags carry in Requirements/features.md;
> #Locking and #SolvedCheck are built, #TurnButtons is withdrawn) — the Play
> screen in :play (Compose) consuming ILockEngine (:kernel) and IPuzzleLibrary
> (:content, PuzzleLibraries.bundled wired in :app, design review F4) — plus
> the carried lines REQ-020.A2, REQ-039.A1, REQ-045.A2 (KICKOFF "State after
> WO-002"). Persistence (IProgressStore) is WO-004's: the screen holds its
> state in memory this WO, behind the seam. Design inputs: DI-1 (prototype,
> behaviour reference only), DI-2 (Spec/02 layout), DI-3 (Spec/01 gameplay),
> DI-4 (Study/06, frozen), DI-9 not yet. Mode: decide from the four reasons
> (:play is a P2 skeleton like :content was; a governed interface is
> consumed, not implemented — say which reason, or Light).
> Verification channels at G3: Compose/device tests on the dev image
> Medium_Phone_API_37.0 AND the floor image Phone_API_26 (the floor is a G3
> precondition for this first device WO — boot it, run the trivial test,
> record it), one emulator at a time; JVM tests where the logic is pure;
> the release verifier (no debug tooling, canary) arrives with this WO per
> platform-notes/android.md. Draw paths in px (G-17). Checkpoint candidates
> (≤5, product-visible): colours and the size-mark glyphs only if a REQ leaves
> them open; strings to review-notes.md. Stations as subagents; the four
> scripts are duties. End every turn with the one signal.

**Summary (orchestrator, Fable; 16:22 → in progress):**
- Read the framework's P3 set, governance (in force), architecture v1.0,
  build-map v1.2, design-inputs §2, decisions, KICKOFF, the 25 outbox drafts
  (nine touch this WO's REQs), the frozen contracts, the 16 REQ files, DI-2,
  DI-3, DI-4, the skeleton. Framework v0.20 = project, no drift. HEAD 3fa75ac.
- **G3 proofs:** toolchain `:play:testDebugUnitTest :contracts:test` green
  16:30; **floor channel** `Phone_API_26` booted (13 s), FloorChannelTest
  1/0/0 at 16:34; **dev channel** `Medium_Phone_API_37.0` booted, same test
  1/0/0 at 16:36 (run log `.swdev/run-log/WO-003.md` g3/f26/d37). Dev image
  left running for P4.
- `workorders/WO-003.md` written: **Light** (neither (a) nor (b) by its
  letter — § Mode); 32 acceptance IDs incl. REQ-002.A1 and the carried
  REQ-020.A2 / REQ-039.A1 / REQ-045.A2, with REQ-020.A1 / REQ-045.A1 re-covered
  on the screen so trace-check's code-home rule holds (build-map rows #GoHome,
  #PuzzleArt, #PuzzleMini → `play/` at G3); REQ-002.A2 → WO-009; REQ-002.A1
  and REQ-014.A1 `implemented (unverified)` on their G1 drafts; the release
  verifier + reset script ride with the `:app` wiring task; UI/gesture tasks
  and the Compose kit staffed on Fable (raise-for-UI rule).
- `decisions.md`: six WO-003 rows (mode, scope, build-map homes, unverified
  lines, touch-routing readings of the G1 drafts, checkpoint item 1).
- **Checkpoint item 1 asked:** the board's colours — default the DI-2
  palette (silhouette `#4A4E69`, board white, tray `#EFE7D6`, paper
  `#FBF7EE`, accent `#3D8BFD`); stands 2026-10-14.
- Planner dispatched (`wo-planner`, Opus) 16:39 with the seams duty (test
  tags, state holder API, `:app` door) that Light leaves to the plan.

**Continuation (same kickoff, no new human input; 16:40 → 17:50):**
- `build-map.md` v1.3: rows `#GoHome`, `#PuzzleArt`, `#PuzzleMini` → `play/`,
  `#Solving` In progress (notify-tier contract-delta, three guard events).
- **Planner → forward 17:12** (Opus): seven tasks TASK-009…TASK-015, none
  `[P]`; a Seams table S-1…S-16, a tag table and the touch routing in
  `tasks.md` § WO-003; `workorders/WO-003-briefs.md` (58 KB). plan-check
  GREEN (the known cross-product warning). Its seven questions answered —
  five as `decisions.md` rows (px hit test is UI; size-neutral pop; pulse on
  every go-home release in a mini; badge in the tray and on the board;
  `TrayRest` the one copy), two in dispatch.
- **Plan Reviewer → forward 17:15** (Sonnet): 0 B / 3 S (F1 TASK-015 depends
  on TASK-014 — fixed, `tasks.md` 0.8; F2, F3 carried). **G3 approved 17:17**
  under governance row 7 (`decisions.md` row).
- **P4 opened:** TASK-009 JVM kit (Opus) ∥ TASK-011 device-kit draft
  (Fable, scratchpad only). **TASK-009 landed 17:49**: 28 tests (7 tokened,
  21 pins), token grep exactly the seven IDs, red window open as planned,
  one draft-time corrected check. **TASK-010 dispatched** (Fable) with the
  author's fifteen seam assumptions.
- `review-notes.md`: the pulse-on-tray-drop reading and the colours to look
  at when playing. Dev image `Medium_Phone_API_37.0` running as emulator-5554.

**Continuation (17:50 → 18:40, no new human input):**
- **TASK-010 → forward 18:00** (Fable): S-1…S-9 in `play/src/main` (606 LOC),
  `:kernel`/`:content` test-only deps, bundle loads on `:play`'s test
  classpath; orchestrator run r1 33 + 2 green → in review (REQ-002.A1
  `implemented (unverified)`). Two decisions rows (tray numbers, gesture readings).
- **TASK-011 device kit drafted 17:54 and landed 18:02** (Fable): 45 tests,
  30 tokened; both device sets red only on TASK-012's seams.
- **TASK-012 → 18:25** (Fable): the static screen core (S-10…S-14,
  `SilhouettePath` as one merged path), BoardTrayKitTest's five IDs green on
  the dev **and** the floor image (0 seam pixels on API 26); one untokened
  kit pin red (shared row top) → arbitrated a test defect, kit author fixed
  it, 7/0/0 rerun 18:27 → **TASK-012 in review**; corrected checks 1. Three
  decisions rows (drawing numbers + merged path; flat pieces without the
  white edge — owner-grade, review-notes; espresso pin).
- **TASK-013 dispatched 18:26** (Fable) — terminated by the API session limit
  (429) while reading; nothing in the tree.

## 2026-10-12 — WO-003 resumed after the session limit

**Human input (verbatim):**

> plz continue

**Summary (orchestrator, Fable; 05:38 →):**
- Tree checked: TASK-013 left nothing; the emulator was gone — the dev image
  rebooted headless. TASK-013 re-dispatched in a fresh context (Fable), same
  brief. The chain after it: TASK-014 → TASK-015 → code review → Test &
  Verify (both images) → trace audit → close.

**Continuation (05:40 → 06:52, no new human input):**
- **TASK-013 → forward 06:12** (Fable): `PointerRouter`, `HitTest`,
  `DragLayer`, `PreviewLayer` (+ `PlayScreen.kt` to 364 lines); on the dev
  image PreviewKitTest 2/0/0, BoardTrayKitTest 7/0/0, DragKitTest 18 with
  one failure — REQ-016.A1 crashing inside the kit's pixel sampler (a read
  two rows below its own tray capture, verified in the XML). Ruled a fixture
  defect (corrected check 2); kit author fixed the sampler (clip to the
  capture, alternative pose fitted by centre) → DragKitTest **18/0/0** at
  06:14 → **TASK-013 in review** (REQ-014.A1 `implemented (unverified)`).
  Two decisions rows (touch readings; motion numbers). Channel note: one test
  class per `cmd.exe` invocation (`proposals.md` at close).
- **TASK-014 dispatched 06:13** (Fable): decorations, badge, pulse, solve
  picture, then the whole play device set on both images — running (mid-run
  at 06:52, on its own device steps). Terminated ~07:10 by the API session
  limit (reset 10:30) after its floor run, before its hand-back.

## 2026-10-12 — WO-003 resumed a second time (TASK-014 cut off by the limit)

**Human input (verbatim):**

> plz continue

**Summary (orchestrator, Fable; 17:57 →):**
- State found: TASK-014's five files + three edits in the tree (`play/src/main`
  2,784 LOC); its XML copies show the whole play device set at **44 tests /
  1 failure on both images** (dev 07:00, floor 07:09). The failure is the
  REQ-023.A1 test's "≥ 1 % base colour visible" assertion on the sailboat,
  whose art covers every inside pixel → ruled a fixture defect (corrected
  check 3), kit author resumed to drop it. Emulators were gone → dev image
  rebooted (run log d37c). TASK-014 agent resumed for its hand-back.
- **TASK-014 → forward 18:05** (context intact): decorations, badge in both
  places, pulse, the solve picture (clipPath + 1.2 dp bevel outline); two
  decisions rows. **Kit fix 3 → SolvedPictureKitTest 6/0/0 (18:04).**
- **TASK-015 → forward 18:24** (Opus): `AppGraph`, `PlayLayouts`,
  `MainActivity` on the Play screen, en/fi strings, `FloorChannelTest`
  adapted; verifier V-01 (150 lines) + manifest + canary + reset script;
  AppLaunchKitTest 1/0/0 + FloorChannelTest 1/0/0 on the dev image; V-01 PASS
  + positive control PASS, runner GREEN within budget. Two decisions rows;
  strings → review-notes.
- **Orchestrator run r2 18:25:** JVM 45 + 2 green, device kits + release APK
  built, plan-check GREEN with the kit compiled, verifiers GREEN, tokens 7 +
  29, guardrail greps clean. **Every task in review; code reviewer (Sonnet)
  dispatched 18:26.**

**Human input (verbatim, mid-turn):**

> plz stop on some good moment as we need to continue another model.. my fable credits are running off

> make sure to have little status from where to continue for next model

**Summary (18:28):** stopped after the code-review dispatch. Hand-over written:
`KICKOFF.md` "WO-003 — MID-WO HAND-OVER" (state, evidence, what is left, a
paste-able resume prompt); WO log 18:28 row. The code reviewer runs on; its
report lands in `reviews/WO-003-code-review.md` for the next context.

## 2026-10-12 — WO-003 resumed in a fresh orchestrator context (Opus 5.5)

**Human input (verbatim):**

> For the next model
>
> - KICKOFF.md has a new section, "WO-003 — MID-WO HAND-OVER", with the state, the evidence rows, the open items and a paste-able resume prompt. Start the next session by pasting that prompt.
> - What is left: triage the code review, Test & Verify on both images, the traceability audit, then the close scripts, ledgers, G4 and the snapshot prompt.
> - The dev emulator may not survive the gap. Check with adb devices and reboot it headless if needed.

**Summary (orchestrator, Opus 5.5; 18:30 →):**
- Read the hand-over brief's list (framework P3 files, WO-003, tasks, run
  log, decisions, review-notes, android notes). No framework drift (v0.20).
- `adb devices`: the dev image `emulator-5554` is still up.
- The code reviewer dispatched 18:26 is still running in the previous
  session; its report is not on disk yet. Not re-dispatched; this context
  waits for `reviews/WO-003-code-review.md`, then triages, runs Test &
  Verify on both images, the trace audit and the close.

**Continuation (18:35 → 19:45, no new human input):**
- **Code review → forward 18:35** (Sonnet): 0 B / 4 S / 5 N. Triage 18:38:
  F1 (badge area into LT2's cell; board overlap), F2 (pulse/shake replay
  on recreation), F4 (`.gitattributes`), N3 (a moving release could lock
  away from the drawn preview — REQ-021 A1 "always"), N4 → fix now; F3
  (main-thread bundle parse) and N1 (celebration replay once `open()` has a
  caller) → WO-004; N2 no change; N5 → proposals at close. Draft
  `chg-draft-req018-badge-touch-precedence.md` amended (board case).
- **Kit author 19:11** (Opus): six new device cases, all red on the old
  tree for the named defect; corrected checks now 5.
- **Fix batch 19:44** (Opus): all six green; whole play set 48/0 and app
  set 2/0 on the dev image; JVM 45 + 2; release builds, V-01 3/3.
- Ruled the implementer's note: the tray badge area covered most of the PG
  miniature → the visible thing wins in the tray too (decisions row, draft
  amended again, `review-notes.md` line). A leftover-shake offset found by
  the implementer → fix now. Kit author resumed for two more red pins.

**Continuation (19:45 → 20:43, no new human input) — WO-003 closed:**
- Kit pins red (19:53), then the implementer's second round (20:13). The
  implementer contested one kit leg. The kit assumed the badge disc sat at
  its node's centre, which came from my own brief's wrong statement. Ruled
  a fixture defect: corrected check 6, disputes 1 / 0. Fixture fixed (disc
  read from the drawing), DecorationKitTest 12/0 (20:23).
- Final tree: test `29306d193615`, product `84a18f2f4e81`. plan-check GREEN.
- **Traceability audit GREEN** (32/32, baseline re-frozen). The auditor
  first reported a report file it had not written; it was resumed and
  wrote it. Noted for the harvest.
- **Test & Verify GREEN:** floor image play 51/0 + app 2/0, the dev image
  reused by hash, JVM 45 + 2, V-01 3/3 + positive control. Cold launch
  floor ~0.75 s, dev ~2 s (F3 → WO-004).
- Close: `tasks.md` statuses (5 done, TASK-010/013 unverified for their
  drafts); `build-map.md` → Built; trace-check + verifiers re-run GREEN;
  wo-metrics written and hand-corrected; DoD, ledger, deltas; four lesson
  lines in `platform-notes/android.md`; `proposals.md` entry; KICKOFF
  "State after WO-003" + the WO-004 brief; AGENTS.md phase line.
  **G4 under row 9** (`decisions.md`: owner play-test recommended).

**G4 surface — WO-003 #Solving (outcomes first):**
- **Satisfied:** 32 of 32 acceptance IDs green on JVM, the dev image and
  the API 26 floor image. REQ-002.A1 and REQ-014.A1 are green but stay
  `implemented (unverified)` for their pending G1 drafts. Release verifier
  V-01 GREEN (no permission, no debug canary, no network classes).
- **Deliberately not touched:** the top bar's ‹ › / counter / Restart /
  grid and persistence (WO-004); DEV aid (WO-005); tablet layout, insets,
  language screen (WO-006); sounds/haptics (WO-007, events only); timer
  (WO-008); REQ-002.A2 and REQ-039.A2 (WO-009). No Contract file changed;
  one notify delta (`build-map.md` v1.3), re-frozen.
- **Risk notes:** the cold launch parses the bundle on the main thread
  (~2 s on the dev emulator) → WO-004. The badge touch order was decided
  under row 12 with a draft amendment, so play it.
- **Lock behind build:** no pending capture-side CHG; 25 drafts await a
  capture session; amended this WO:
  `chg-draft-req018-badge-touch-precedence.md`; written: none.
- **Snapshot to commit (owner):** the whole working tree. Suggested
  message: `WO-003 #Solving closed: Play screen in :play behind
  ILockEngine/IPuzzleLibrary, :app wiring, kit 32/32 on JVM + API 37 +
  API 26, release verifier V-01, build-map v1.3`.

## 2026-10-15 — WO-004 #Browsing + store/ opened (P3, fresh orchestrator context)

**Human input (verbatim):**

> WE ARE IN PHASE P3 — WORK ORDER WO-004 #Browsing + store/ (TangramPlaytime,
> SWDev v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on"
> framework files), then governance.md, architecture.md, build-map.md,
> design-inputs.md §2, decisions.md (incl. the WO-003 rows), the last
> progress.md entry, KICKOFF.md "State after WO-003", capture-outbox/ (the
> REQ-025 and TYPE-006 drafts), platform-notes/android.md (incl. the WO-003
> lesson lines in § Compose test kit).
> WO-004 scope: #Browsing — #PuzzleNav (REQ-024), #ResumePuzzle (REQ-025),
> #SolvedView (REQ-026), #PuzzleOverview (REQ-050), the parent REQ-003 — on
> the Play screen of :play (the top bar's ‹ › and counter, Restart / Retry,
> the grid), plus the store/ module implementing IProgressStore (notify tier)
> so PlayController's in-memory state (S-1…S-3) is persisted, and the
> re-covered screen halves of REQ-040.A1, REQ-041.A1, REQ-045.A1. Carried from
> WO-003: load the bundle and the store off the main thread (F3); the
> celebration's trigger as a consumed controller cue (N1). Timers are WO-008's
> (REQ-030 stays out).
> Mode: Full, reason (c) a new persistence format (build-map.md: store/ is
> built in WO-004) — design-author → design-reviewer first. The design names
> the touch geometry and what survives an activity recreation (proposals.md
> 2026-10-12).
> Verification channels at G3: JVM (store/, the controller), the dev image
> Medium_Phone_API_37.0 AND the floor image Phone_API_26 (one emulator at a
> time; both proven in WO-003), the release verifier V-01 with its positive
> control; a migration/persistence test per governance row 11.
> Checkpoint candidates (≤5, product-visible). Stations as subagents; the four
> scripts are duties. End every turn with the one signal.

**Summary (orchestrator, Opus 5.5; 18:38 → in progress):**
- Read AGENTS.md, the framework's session-start and P3 files, governance (in
  force), architecture v1.0, build-map v1.3, design-inputs §2, decisions.md,
  the WO-003 close entry, KICKOFF "State after WO-003", the three REQ-025 /
  TYPE-006 drafts, android.md, the in-scope REQs and TYPE-006, the frozen
  `IProgressStore`. Framework v0.20 = project, no drift. HEAD 19806a8.
- `workorders/WO-004.md` opened: 16 acceptance IDs (REQ-003, 024, 025, 026,
  050 + the re-covered REQ-040.A1, 041.A1, 045.A1); **Full (c)**.
- **G3 proofs:** toolchain green 18:39 (store 1, contracts 2, play 45);
  floor `Phone_API_26` app set 2/0 (18:41); dev `Medium_Phone_API_37.0` 2/0
  (18:42), left running. Run log `.swdev/run-log/WO-004.md`.
- **Design Author dispatched** 18:40 (Opus).

**Continuation (18:43 → 19:47, no new human input):**
- **Design → forward 19:01** (Opus): `designs/WO-004-design.md`; ADR-003 =
  one JSON document rewritten atomically, unreadable/unknown version = fresh
  install; eight `decisions.md` rows; no contract delta.
- **Design review 19:18** (Opus 5.5): 0 B / 5 S, every library fact holds
  against the real code. Triage: F1–F4, F5(a) accepted (the save boundary,
  the death pair judged from output, one store instance, the grid below the
  top bar); **F5(b) reversed** on trace-check's code (corrected check 1).
  Same author fixed it in place 19:22 (three more rows).
- **Checkpoint items 1–5 asked** 19:23 (Restart one press · solved row
  after the picture · best time "–" · the middle of the bar opens the grid
  · emptied board stays in progress); defaults stand 2026-10-18.
- **Planner → forward 19:44** (Opus): TASK-016…022 (017 ∥ 018), briefs
  55 KB; plan-check GREEN (my re-run 19:45, the known cross-product warning).
  Five questions ruled: three design write-backs (design author resumed),
  the device-hygiene after-step (reset then `--check`), two proposals.
- Next: plan review → G3 → the JVM kit, then the two store tasks.

**Continuation (19:47 → 20:50, no new human input):**
- Plan review 19:49 (Sonnet): 0 B / 3 S, all fixed by the planner 19:51;
  plan-check GREEN. **G3 approved 19:52** under row 7 (`decisions.md`).
- **TASK-016 JVM kit landed 20:23** (Opus): 35 tests, 14 tokened, the
  store's row-11 pins (19 corrupt variants, failed write keeps the file).
- **TASK-017 `:store` 20:31** (Opus): `FileProgressStore` (atomic
  whole-file replace), `ProgressStores.onDevice`; store kit 10/0.
- **TASK-019 device-kit draft 20:37** (Opus, scratchpad): 16 IDs; flagged
  the pulse markers vs the Restart pill → ruled: markers above the pill.
- **TASK-018 `Persistence` + controller 20:48** (Opus): JVM 73 + 2 green;
  orchestrator run r1 green (store 12, play 73, content 64, contracts 2).
- Six implementer decisions recorded as `decisions.md` rows.
- Device kit landing now; then TASK-020 (top bar, Restart, solved row).

**Human input (verbatim, 21:14):**

> plz dont start new tuff anymore after this implementer.. time to keep break

**Summary (orchestrator):**
- Device kit landed 20:49 (12 files, the declared red window only); TASK-020
  (top bar, Restart, solved row, long press, loading paper) dispatched 20:50
  and still running.
- Owner pause: no new station after TASK-020. Its hand-back gets the receive
  check and the log row; then a resume note in KICKOFF.md and stop.

**Continuation (21:14 → 21:35) — paused:**
- **TASK-020 → forward 21:32** (Opus): the top bar, Restart, the solved row,
  the long press and the loading paper. Dev image: ResumeScreen 5/0,
  SolvedView 5/0, the six WO-003 classes 51/0, TopBar 12/1. TopBar's four
  tokens are green; its one pin skips because no puzzle on this screen puts
  a piece over the Restart pill. That pin is open for the kit author.
- JVM green (store 12, play 79, content 64, contracts 2); every kit hash is
  unchanged; three decision rows; ten strings in `review-notes.md`.
- **Not dispatched (owner pause):** the kit finding, TASK-021 (the grid),
  TASK-022 (`:app` wiring + the process-death check), review, verification
  and close. Resume prompt: KICKOFF.md "WO-004 — PAUSED"; AGENTS.md phase
  line updated. Emulator `Medium_Phone_API_37.0` left running.

## 2026-10-17 — WO-004 resumed (P4, fresh orchestrator context)

**Human input (verbatim):**

> WE ARE IN PHASE P4 — RESUMING WORK ORDER WO-004 #Browsing + store/ (TangramPlaytime,
> SWDev v0.20, intake path C) in a fresh orchestrator context. Read AGENTS.md first
> (incl. the "from P3 on" framework files), then KICKOFF.md "WO-004 — PAUSED",
> workorders/WO-004.md (§ Log is the narrative), tasks.md § WO-004,
> workorders/WO-004-briefs.md (the two tables at the top), .swdev/run-log/WO-004.md,
> decisions.md (the 2026-10-15 rows), the last progress.md entry, review-notes.md,
> platform-notes/android.md.
> State: TASK-016/019 done; TASK-017/018/020 in review; TASK-021 (overview) and
> TASK-022 (:app wiring + the process-death check) not started. First: route the
> open kit finding (TopBarKitTest hit-order-3 pin skips on the dev screen) to a
> fresh acceptance-test-author with its TASK-019 brief + the finding; then TASK-021,
> then TASK-022, each from its brief (slice-implementer, Opus), recomputing the kit
> hashes at every hand-back and logging every hop with `date` stamps. Then
> plan-check again (kit compiled), code-reviewer (Sonnet, fresh), test-verify
> (Sonnet: every set on BOTH images from the final tree, the death pair + its
> positive control on both, V-01 + positive control), traceability-auditor
> (PYTHONUTF8=1 trace_check.py --wo WO-004, 16 IDs), and the close (run_verifiers
> --wo, wo_metrics --wo --write, ledger, Contract deltas, ADR-003 via the G2 route,
> G4 under row 9, KICKOFF "State after WO-004" + the WO-005 brief, AGENTS.md phase
> line, proposals.md, progress.md, the snapshot prompt). Stations as subagents. End
> every turn with the one signal.

**Summary (orchestrator, Opus 5.5; 06:46 → in progress):**
- Read the resume set; framework v0.20 = project, no drift; HEAD 19806a8.
- Kit hashes 17 of 17 unchanged since landing. The emulator was gone;
  `Medium_Phone_API_37.0` rebooted headless (06:49).
- The open kit finding (the Restart-pill touch pin that skips on the dev
  screen) sent to a fresh kit author 06:50.
- Kit author back 07:13: the case cannot exist on the full-size dev phone
  (the board is too tall for a piece to reach the pill), so the pin now
  builds its own puzzle at a smaller screen height; TopBar 12/0 on the dev
  image, nothing skipped. Corrected check 2.
- TASK-021 (the puzzle grid) dispatched 07:14; back 08:01: the grid of
  every puzzle below the top bar, the bar inert while it is open. Its test
  6/0; the whole Play-screen set 79/0 on the dev phone and 79/0 on the
  oldest supported Android (API 26). Kit untouched. One gap in the seam
  table closed with an internal read (decision row; design write-back
  queued); one drawing-detail row.
- TASK-022 (the app wiring + the app-killed check) dispatched 08:02; back
  08:18: the app loads off the main thread and shows the screen once ready;
  back closes the grid; `tools/process_death_check.sh` proves a placed
  board survives the app being killed, on both phones (plus a control that
  must fail and a run that must not count a skipped phase). Release check
  V-01 green. Two decision rows.
- Design write-back (fresh design author) 08:21; my own run r2 green (JVM,
  release check, the app-killed check on the dev phone); plan-check GREEN
  with the kit compiled. Code review dispatched 08:22 (Sonnet, fresh).
- Code review 08:32: 0 Blockers, 2 Shoulds, 5 Notes; it broke the code on
  purpose twice and the tests caught both. Ruled: the "unreadable file is
  overwritten" risk accepted for now and made a must-do for WO-008 (play
  time); the app-killed check could judge an old build, so it now refuses
  stale builds (fixed by the same implementer 08:39; channel line amended;
  two decision rows; design written back twice).
- Test & Verify dispatched 08:39 (Sonnet): every set on both phones from
  the final tree. Back 09:15: GREEN. Play set 79/0 on both phones, app set
  green on both (plus the two by-design skips), the app-killed check right
  on both, release check V-01 green.
- Close 09:17–09:25: build-map v1.4 (#Browsing Built); **ADR-003 written
  into `architecture.md` v1.1 through the G2 route** (row 4, the unlocked
  hatch opened and closed, the baseline re-frozen); traceability audit GREEN
  16/16; verifiers GREEN; metrics counted; three lessons into
  platform-notes; proposals, review-notes, KICKOFF (State after WO-004 +
  the WO-005 brief), AGENTS.md phase line. **G4 under row 9** (decision
  row): ready to merge; owner play-test recommended; the snapshot is the
  owner's.

**Human input (verbatim, 2026-10-17 09:28):**

> (Q1) yes just one press (Q2) k (Q3) k (Q4) k (Q5) k. (x) plz let me know what we have left... and what exactly is these developer tools

**Summary (orchestrator):**
- All five WO-004 checkpoint items answered, every default confirmed; no
  build change. Recorded: `decisions.md` row 17 (2026-10-17), the WO's
  checkpoint table and Metrics, KICKOFF and the AGENTS.md phase line; the
  verbatim answer to the capture side as
  `capture-outbox/src-draft-wo004-checkpoint-answers.md` (item 5 supports
  the state-rules draft's proposed text). 26 outbox drafts now.
- The WO-005 brief in KICKOFF now also points at the state-rules draft (its
  "Solve this puzzle now" line).
- Answered in chat: what is left (WO-005…WO-009, the puzzle batch, the
  capture session) and what the developer tools are (the passcode-protected
  solution reveal, test builds only).

## 2026-10-17 — WO-005 #DevTools opened (P3, fresh orchestrator context)

**Human input (verbatim):**

> WE ARE IN PHASE P3 — WORK ORDER WO-005 #DevTools (TangramPlaytime, SWDev
> v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
> files), then governance.md, architecture.md (v1.1), build-map.md (v1.4),
> design-inputs.md §2, decisions.md (incl. the WO-004 rows of 2026-10-15 and
> 2026-10-17), the last progress.md entry, KICKOFF.md "State after WO-004",
> capture-outbox/chg-draft-req046-android-build-terms.md and
> chg-draft-type006-missing-transitions.md (its "Solve this puzzle now" bullet),
> platform-notes/android.md (incl. the WO-004 lesson lines).
> WO-005 scope: #DevTools — #SolutionReveal (REQ-046 A1..A4): the passcode-
> protected DEV aid in play/src/debug (a debug-only source set; absent from the
> release build, G-08), drawn on the WO-004 Play screen: the DEV button, the
> passcode prompt, "show the solution" (an overlay that stays on while browsing)
> and "solve this puzzle now" (Solved through the controller and Persistence,
> no best time); the DEV button hidden on a Solved puzzle; its place in the S-22
> hit order. Depends on REQ-022 (built, WO-001).
> Mode: decide from the four reasons (no new module — play/src/debug exists; no
> governed interface changed; no persistence format change) — Light unless one
> reason is written in the WO.
> Verification channels at G3: JVM where the logic is pure; the dev image
> Medium_Phone_API_37.0 AND the floor image Phone_API_26 (one emulator at a
> time); the release verifier V-01 + positive control (A4: the release has no
> DEV button). Device runs are judged by their XML, never by the exit code.
> Checkpoint candidates (≤5, product-visible): the DEV button's place and look
> (REQ-046 ASSUMPTION: bottom-left, small, dashed). Strings to review-notes.md.
> Stations as subagents; the four scripts are duties. End every turn with the
> one signal.

**Summary (orchestrator, Opus 5.5; 09:37 → in progress):**
- Read the kick-off set; framework v0.20 = project, no drift; HEAD 057492c
  (the WO-004 snapshot; the post-commit checkpoint-answer bookkeeping is
  still uncommitted — the owner's).
- `workorders/WO-005.md` opened: **Light** (none of the four reasons);
  scope REQ-046 A1..A4; five questions for the planner (the debug-only seam,
  "solve now" and the state rules, the overlay, the DEV button and the
  prompt, A4's oracle); hard-stop 1 named as not reached (the passcode is
  printed in the REQ, guards nothing, debug builds only).
- G3 proofs: JVM toolchain 81/0 (09:38); the dev image and the API 26
  floor image each ran the app set, 5 passed + the 2 guarded skips
  (09:39, 09:40). Run log `.swdev/run-log/WO-005.md`.
- Planner dispatched 09:42 (Opus); back 10:15: four tasks (the JVM tests,
  the aid's logic behind a debug-only door, the device tests, the screen
  work), no parallel tasks, plan-check GREEN. Nine planner questions ruled
  10:19, all accepted: eight `decisions.md` rows; the `#DevTools` code home
  in `build-map.md` v1.5 fixed so trace-check can read it; the debug source
  set counted as product code (`guard.json`).
- Checkpoint items 1–4 asked 10:24 (the DEV pill, "solve now" plays the
  normal win, the overlay under the pieces, the passcode box); defaults
  stand 2026-10-20. The aid's 12 strings in `review-notes.md`.
- Plan review 10:22: 0 Blockers, 3 Shoulds, carried in dispatch. **G3
  approved 10:23** under row 7. P4: the JVM kit author and the device kit
  author (drafting) dispatched 10:24.
- JVM kit landed 10:51 (the passcode, "solve now" over all 13 puzzles, the
  release-package scan with its control); its author found a build race,
  so the JVM verdict line is now two Gradle runs. Device kit drafted 10:53,
  revised on my rulings 11:03 (two cases built by the kit, not skipped).
- The aid's logic landed 11:05; my own run green (play 94/0, contracts
  2/0, V-01 + control); no aid text in the regular code. Device kit landed
  11:07 (only the planned three compile errors). The screen work (DEV
  button, overlay, passcode box, wiring) dispatched 11:07.
- The screen implementer was cut off by the API session limit (~11:25);
  resumed with its context 11:42. Back 12:06: both device sets on the dev
  and the API 26 images, JVM 96/0, V-01 green — one device test failed,
  the same on both images. Arbitrated 12:07 from the test source: the test
  tapped a spot that, on a solved puzzle, lies on Retry, which correctly
  comes first in the touch order. Worker wins; the product passes; the
  device kit author is correcting the test (corrected check 1).
- Test fixed 12:21 (its class 15/0 on the dev image); plan-check GREEN
  with the whole kit compiled. Code review dispatched 12:21 (Sonnet, fresh).
- Code review 12:27: 0 Blockers, 1 Should, 3 Notes; three deliberate breaks
  caught. The Should (the release scan looked for names only) fixed by the
  JVM kit author 12:33: it now also looks for "Wrong passcode." and the
  passcode (corrected check 2).
- Test & Verify 13:10: GREEN — every set on both images from the final
  tree (play 94/0 on each, app 7 + 2 guarded skips on each, JVM 174/0,
  release scan + V-01). I read the four device XMLs myself.
- Traceability GREEN 4/4 (13:11); verifiers GREEN; metrics counted (window
  rows corrected). Close 13:13: build-map `#DevTools` Built; proposals (2
  corrected checks, the build-map parser, A4's token route, a missing
  telemetry row), three platform-notes lessons, review-notes play-through,
  KICKOFF "State after WO-005" + the WO-006 brief, AGENTS.md phase line.
  **G4 under row 9:** ready to merge; this is the 1-in-5 spot-check WO, so
  an owner play-test is recommended; checkpoint items 1–4 stand on their
  defaults 2026-10-20; the snapshot commit is the owner's.

## 2026-10-17 — New puzzle source: TangramCollections → WO-010 planned

**Human input (verbatim, 16:16):**

> plz look this folder [C:\GitHub\AI\TangramCollections] it has plenty of new puzzles. Plz add task to workorders to add those out game when possible

**Summary (orchestrator, 16:16–16:22):**
- Surveyed the folder read-only: five collections in one JSON format —
  13 convex shapes (CC0), 131 Android-Tangram figures (GPL-3.0), 203 Nevit
  Dilmen figures (Commons, attribution, licence unconfirmed), 1,002
  KILOGRAM figures (research-only), 179 Richter silhouettes (no solutions).
  None has a picture, rating or Finnish title.
- Planned **WO-010 content batch** (`workorders/WO-010.md`, Proposed):
  convert accepted figures into `Tangrams/*.json` with titles, rating, a
  drawn picture and provenance, both validity checks green. Slot: after
  WO-008, before WO-009; it can be pulled forward (KICKOFF "Planned —
  WO-010" has its paste-in brief).
- Recorded: `capture-outbox/src-draft-tangramcollections-puzzles.md` (27
  drafts now); DI-10 in `design-inputs.md` v1.2 with §2 items 14–16 (no
  difficulty modes or auto-flip; no pixel-count solve check; the figures'
  units and accuracy); two `decisions.md` rows; the AGENTS.md phase line.
- Checkpoint items asked (defaults stand 2026-10-20): (1) which collections
  may ship — default only the convex shapes (your "only licence-free
  assets" rule), recommended also the GPL-3.0 figures; (2) the
  first-release count — default 25.

## 2026-10-17 — WO-006 #Layout + #Language + #Promise opened (P3, fresh orchestrator context)

**Human input (verbatim):**

> WE ARE IN PHASE P3 — WORK ORDER WO-006 #Layout + #Language + #Promise
> (TangramPlaytime, SWDev v0.20, intake path C). Read AGENTS.md first (incl. the
> "from P3 on" framework files), then governance.md, architecture.md (v1.1),
> build-map.md (v1.5), design-inputs.md §2, decisions.md (incl. the WO-005 rows
> of 2026-10-17), the last progress.md entry, KICKOFF.md "State after WO-005",
> capture-outbox/ (the REQ-006, REQ-047, REQ-048 and licence drafts),
> platform-notes/android.md (incl. the WO-005 lesson lines).
> WO-006 scope: #Layout (REQ-006) — #PhoneLayout (REQ-035), #TabletLayout
> (REQ-036), #TouchTargets (REQ-037); #Language (REQ-047); #Promise (REQ-001)
> with #NoMoney (REQ-008, REQ-009) and #DeviceOnly (REQ-010) — the TYPE-007
> window class and the phone/tablet layouts in :app (O-05), insets, back
> handling, the touch-target sweep (the dev-* nodes exempt by REQ-037's rule),
> the en/fi resources (English Contract, Finnish reference), the promise lines.
> REQ-009's free note sits on the settings screen (WO-007's) — say where it
> lands. Carried: the grid's screen-reader labels; the ‹ › 52 dp sweep; the
> keyboard under the DEV passcode box; the locale-list mix (KICKOFF).
> Mode: decide from the four reasons — Light unless one is written in the WO.
> Verification channels at G3: JVM where the logic is pure; the dev image
> Medium_Phone_API_37.0 AND the floor image Phone_API_26 (one emulator at a
> time) — and a tablet image if a REQ needs one (prove it reachable at G3 or
> record a waiver); the release verifier V-01 + positive control; the JVM line
> is two Gradle runs (APKs, then tests). Device runs are judged by their XML.
> Checkpoint candidates (≤5, product-visible). Strings to review-notes.md.
> Stations as subagents; the four scripts are duties. End every turn with the
> one signal.

**Summary (orchestrator, Opus 5.5; 16:41 → in progress):**
- Read the kick-off set; framework v0.20 = project, no drift; HEAD a797553
  (the WO-005 snapshot; the WO-010 planning rows are uncommitted, the
  owner's). The tree equals WO-005's final tree.
- `workorders/WO-006.md` opened: **Light** (none of the four reasons);
  13 acceptance IDs; eight questions for the planner (the window class and
  phone portrait lock, insets / keyboard / Back, the touch-size oracle, the
  screen-reader carry, the locale-list mix, the promise lines' oracles, the
  play-through and exact window sizes, the system-level code homes).
- **Where the two lines land** (`decisions.md`, row 8): the free note goes
  to WO-007 (its only line needs the settings screen); the store-labels
  line goes to WO-009 (it needs the Play listing). REQ-047.A1 carries
  `implemented (unverified)` on the pending English-Contract draft (row 20).
- **G3 channels proven:** JVM toolchain 96 + 2 green (16:45); a **tablet
  image** made from the installed Android 37 image (1280 × 800 dp — no
  download) and proven (16:47); floor image (16:49); dev image with gesture
  navigation (16:50), left running. Run log `.swdev/run-log/WO-006.md`.
- Planner dispatched 16:51 (Opus); back 17:30: six tasks (two parallel),
  plan-check GREEN, nine questions. Ruled 17:32 (nine `decisions.md`
  rows): seven as proposed (window class from the upright width so a phone
  stays portrait; Back closes only the grid; touch sizes measured on the
  router's real areas; the screen-reader carry dropped — no REQ; one
  language rule; the play-through proves "fully playable" as locked);
  **two amended** — the API 26 floor runs the Finnish and Swedish legs
  (locale boots), and a root route for airplane mode is probed before any
  skip. Planner resumed, amendments written 17:37.
- Checkpoint items 1–2 asked 17:32 (the tablet screen; edge to edge with
  always-dark bar icons); defaults stand 2026-10-20. No new strings.
- Plan review 17:39: 0 Blockers, 2 Shoulds (one applied, one carried).
  **G3 approved 17:39** under row 7. P4 step 1 dispatched: the JVM kit
  (lands), the device kit (drafting), the device-reset script + the floor
  route probes.
- JVM kit landed 17:55 (the release APK holds no permission, a closed
  package allow-list for "no ad / billing" code, the layout-class boundary,
  the safe-area geometry).
- Device-reset script done 18:10. **Finding:** on Android 8.0 a
  `-change-locale` boot does not change the language; a root route does
  (even a two-language list), and airplane mode works by root too — so the
  floor runs every language and airplane test; the only allowed skips are
  the two process-death phases (row "The floor routes, as measured").
  Planner rewrote the floor steps 18:16.
- Device kit drafted (seven files); seven questions ruled — the keyboard
  test runs in its own pass; rounded buttons are probed on their edges, not
  corners (my earlier corner rule narrowed before it became a test defect).
- Screen geometry done 18:41 (`:kernel` layout class, `:play` safe area,
  the DEV card above the keyboard); my re-run green 18:43 (JVM 165 tests,
  V-01 + control). Device kit landing (step 3).

**Human input (verbatim, 19:1x):**

> running out of credits soon.. plz pause when possible

**Summary (orchestrator, 19:12 — paused):**
- Device kit landed 19:10 (seven classes; hashes checked). On the dev image
  the touch-size, play-through and language tests already pass; the phone,
  tablet and keyboard tests fail only where TASK-031's window shell is still
  missing. Six kit defects fixed by its author (corrected checks); two of
  its readings confirmed (`decisions.md`).
- **Paused at a clean seam:** TASK-027…030 done, nothing dispatched or
  running. Next is TASK-031. Resume prompt and state in KICKOFF.md
  "WO-006 — PAUSED"; the WO log's PAUSED row. Dev emulator left running.

## 2026-10-18 — WO-010 checkpoint answers: 13+131 now, no launch cap; WO-010 next after WO-006

**Human input (verbatim, 07:04; the session renamed just before to "ask this to do still 13+131"):**

> lets do that 13+131 now to get bit content for my kiddos who going to test this. 6Y solves those easy buzzles in 1-3 minutes per buzzle... so more material is welcome. We worry limiting launch ones later

**Summary (orchestrator, 07:04–07:12):**
- Read as the answers to WO-010's two checkpoint items: the 13 convex
  (public domain) and 131 Android-Tangram (GPL-3.0, the app's own licence,
  credited) figures may ship; no launch cap now — every figure that passes
  goes in; the launch selection is decided before the release WO.
- Found WO-006 paused mid-build (four of six tasks done) with its changes
  uncommitted, so WO-010 cannot start cleanly "now": two work orders in one
  tree would share one snapshot and each other's half-finished tests.
  Decided (row 8): **WO-010 runs right after WO-006 closes**, ahead of
  WO-007 and WO-008.
- Recorded: `capture-outbox/src-draft-wo010-checkpoint-answers.md` (28
  drafts); `chg-draft-licence-notice-and-assets.md` amended (GPL-3.0 puzzle
  figures, credited); two `decisions.md` rows; `workorders/WO-010.md`
  (answers, the full 144 in scope, G-16 + the O-06 line as its first P3
  step, risks to measure: start-up load, the grid, kit runtime, ~144
  pictures); KICKOFF (the WO-010 section with its paste prompt; a pointer in
  the WO-006 pause note so its close hands over to WO-010); AGENTS.md.

## 2026-10-18 — WO-006 resumed (P4, fresh orchestrator context)

**Human input (verbatim, 07:0x):**

> WE ARE IN PHASE P4 — WORK ORDER WO-006 #Layout + #Language + #Promise, RESUMED
> after a pause (TangramPlaytime, SWDev v0.20, intake path C). Read AGENTS.md
> first (incl. the "from P3 on" framework files), then governance.md,
> workorders/WO-006.md (Log to the PAUSED row), tasks.md § WO-006,
> workorders/WO-006-briefs.md (header, dispatch order, channel table),
> .swdev/run-log/WO-006.md, the WO-006 rows of decisions.md (2026-10-17), the last
> progress.md entry, KICKOFF.md "WO-006 — PAUSED", platform-notes/android.md.
> Resume at dispatch step 4: TASK-031 (slice-implementer, strongest model), then
> TASK-032, then plan-check, code review, Test & Verify on the dev, tablet and
> floor images (the floor's root-route steps), trace-check, verifiers,
> wo-metrics, close. Boot the dev image first and check `tools/device_reset.sh
> --check` is clean. Stations as subagents; the four scripts are duties. End
> every turn with the one signal.

**Summary (orchestrator, Opus 5.5; 07:09 → in progress):**
- Read the resume set; framework v0.20 = project, no drift. HEAD a797553;
  the product tree is unchanged since TASK-029 (e84023455fc6), the test
  tree holds TASK-030's landed kit (789ef4a5944e).
- The dev image was gone after the pause: rebooted headless 07:09–07:10
  (API 37, 411 × 914 dp, gesture navigation); `device_reset.sh --check`
  clean 07:10:24. Kit hashes recorded in the WO log (07:10).
- Step 4 dispatched 07:11: TASK-031, the window shell (slice-implementer,
  Opus) — layout class, phone portrait, the tablet layout, edge to edge,
  insets, the keyboard; on the dev, tablet and floor images.
- 08:13: no change — only the orchestrator's dev-emulator launch exited
  (TASK-031 switches images, one at a time); TASK-031 still running.
- 09:32 TASK-031 back: the window shell is built (phone upright from the
  manifest, tablet unlocked at run time, the one-row tablet tray, edge to
  edge with dark icons, the insets and the keyboard). Its tests are green
  except three kit legs it contested with evidence.
- 09:37 ruled: all three are test-kit defects, the product stays (the
  floor can't make a 2560-px-wide tablet display, the kit read the bars'
  size before they settled, a restore waited forever). Checkpoint item 3
  asked: an older tablet turns upright for a moment at start (default:
  keep it, stands 2026-10-21). 09:39 a fresh kit author fixes the three
  legs and proves them on the three images.

**Human input (verbatim, 10:2x):**

> plz look change to close sessions, as leaving for while

**Summary (orchestrator, 10:25 — pause requested):**
- The kit author was 46 min in (six of seven kit files edited, device
  proofs running). Not stopped: a stop mid-edit leaves a half-fixed kit
  with no hand-back. Nothing new is dispatched; the pause lands at its
  hand-back.
- Pause notes written now so a close at any moment is recoverable:
  KICKOFF.md "WO-006 — PAUSED again" (state + resume prompt for both
  cases — hand-back logged, or the session closed first), the WO log
  10:25 row (kit hashes at that moment), the WO status line, AGENTS.md
  phase line.

## 2026-10-18 — WO-006 resumed again (P4, fresh orchestrator context)

**Human input (verbatim, 13:35):**

> WE ARE IN PHASE P4 — WORK ORDER WO-006 #Layout + #Language + #Promise, RESUMED
> after a second pause (TangramPlaytime, SWDev v0.20, intake path C). Read
> AGENTS.md first (incl. the "from P3 on" framework files), then governance.md,
> workorders/WO-006.md (Log to the last row), tasks.md § WO-006,
> workorders/WO-006-briefs.md (header, dispatch order, channel table),
> .swdev/run-log/WO-006.md, the WO-006 rows of decisions.md (2026-10-17 and
> 2026-10-18), the last progress.md entry, KICKOFF.md "WO-006 — PAUSED again",
> platform-notes/android.md. First settle the kit recirculation as KICKOFF says
> (accept its hand-back if logged, else re-dispatch a fresh acceptance-test-author
> on the partial fix); TASK-031 is accepted from the fixed kit's green runs on the
> dev, tablet and floor images. Then TASK-032 (slice-implementer, strongest model),
> plan-check, code review, Test & Verify on the three images (the floor's
> root-route steps), trace-check, verifiers, wo-metrics, close (next brief:
> WO-010). Boot the dev image first and check `tools/device_reset.sh --check` is
> clean. Stations as subagents; the four scripts are duties. End every turn with
> the one signal.

**Summary (orchestrator, Opus 5.5; 13:35 → in progress):**
- Read the resume set; framework v0.20 = project, no drift. No hand-back
  was logged: the session closed while the kit author ran. The seven kit
  files are exactly as at 10:25; the product is unchanged since TASK-031.
- The earlier author's evidence survived: on the phone image, after its
  last edit, the keyboard, phone and tablet tests were green; the touch
  test never finished; tablet and floor images never ran.
- Dev image rebooted 13:35; the killed run had left a tablet-sized display
  and a turned screen on it — the reset script put both back, check clean.
- 13:37 a fresh kit author finishes the fix and proves it on the phone,
  tablet and floor images (WO log 13:37).

**Human input (verbatim, 16:05):**

> what in strange world that one is doing for 2.5 hours... do we have too big tasks?

**Summary (orchestrator, 16:05 → 16:08):**
- Answered from the kit author's evidence timestamps: ~1 h was the
  needed proof (seven classes × three emulators, one at a time, ~10 min a
  full set), ~55 min a hang on the oldest phone found last, ~40 min
  re-proving the other two after that fix. The task itself was small.
- Proposed for later WOs (to `proposals.md` at close): oldest phone
  first; only the broken test while fixing, the full set once at the end;
  a per-test time limit; fewer repeat runs.
- 16:05 the kit author came back done; receive checks and the XMLs read
  directly all match. Ruled: two more kit defects → corrected checks 11;
  the reset's crash-dialog blind spot carried with an interim floor rule.
  The window-shell task accepted as done (WO log 16:08). Next: the
  language task.
- 16:09–17:43 the language task (one rule from the device's first
  locale) built and green on all three emulators; accepted 17:46. Found:
  a comma list never reaches a test argument (corrected check 12; the
  floor's list step proven by a direct run instead). Plan check green.
- 17:47 independent code review of the whole work order dispatched;
  back 17:56: no blockers, two shoulds, three notes. Triaged 18:33: the
  "no ad library" check on the release app is tightened now (corrected
  check 13, test side, no emulator needed); the language rule's corner
  (a first language no library has texts for) recorded and carried to
  the release WO; the counter's digits on Arabic/Persian phones carried
  to WO-010; the side back-swipe zone goes on the owner's play list.
- 18:39 the check fix landed (21 named Google libraries instead of the
  whole family; all green). 18:42 final test pass dispatched, starting
  from the language task's runs (same trees) — only the oldest phone's
  airplane-mode step, two small unit-test sets and the verifiers re-run.
- 18:47 a finished station, re-woken when its emulator was stopped, ran a
  reset on the final pass's floor; that step was re-run clean (lesson
  logged). 18:53 final pass GREEN, 13/13; 18:54 traceability GREEN.
- **19:10 WO-006 closed at G4** (`decisions.md` "WO-006 closed at G4").
  Build map v1.6; 13 corrected checks and the device-time lessons in
  `proposals.md`; seven lines in `platform-notes/android.md`; play-test
  items in `review-notes.md`; KICKOFF "State after WO-006" → next WO-010.
  Snapshot commit prompted to the owner.
