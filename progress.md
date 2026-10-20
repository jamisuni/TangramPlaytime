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

## 2026-10-18 — The WO-006 resume prompt pasted after the close (fresh context)

**Human input (verbatim, 19:2x):**

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

**Summary (orchestrator, Opus 5.5; 19:23):**
- Framework v0.20 = project, no drift.
- Nothing to resume: WO-006 closed at G4 19:10 (WO log Metrics, KICKOFF
  "State after WO-006", AGENTS.md current phase) and the owner committed
  the snapshot (`2ccea13`). The tree is clean apart from the hook logs.
- Every step the paste asks for is already done and logged in the entry
  above. Nothing dispatched, no emulator booted, no file touched except
  this entry.
- Next is WO-010 (the content batch), and it has its own P3 prompt
  (KICKOFF "Next after WO-006 — WO-010"). Asked the owner whether to
  start it in this context.

## 2026-10-18 — WO-010 content batch opened (P3, same context — it had done no WO work)

**Human input (verbatim, 20:0x):**

> WE ARE IN PHASE P3 — WORK ORDER WO-010 content batch: new puzzles from
> TangramCollections (TangramPlaytime, SWDev v0.20, intake path C). Read
> AGENTS.md first (incl. the "from P3 on" framework files), then governance.md,
> architecture.md (v1.1; G-05, G-16, O-06), build-map.md, design-inputs.md
> (DI-5, DI-10, §2 items 14–16), decisions.md (the WO-010 rows of 2026-10-17 and
> 2026-10-18, the content rows of 2026-10-11), the last progress.md entry,
> KICKOFF.md "Next after WO-006 — WO-010", workorders/WO-010.md (the stub, its
> answered checkpoint items and § Constraints' risks),
> capture-outbox/src-draft-tangramcollections-puzzles.md,
> src-draft-wo010-checkpoint-answers.md and chg-draft-licence-notice-and-assets.md,
> Spec/03-puzzle-format.md, Tangrams/README.md, platform-notes/android.md.
> WO-010 scope: first amend architecture.md G-16 (GPL-3.0 puzzle figures,
> credited) and add the O-06 exception for the DEV aid's strings through the
> G2 route (row 4). Then convert every figure of
> C:/GitHub/AI/TangramCollections/collections/convex-13 and
> android-tangram-131 (read-only) that passes and is not a duplicate into
> Tangrams/*.json (tangram-puzzle/1, polygon form) — kind, rating, en title
> (fi reference), a picture drawn for this project, provenance + licence,
> ASSET-LICENCES.md credits, reviewedByHuman false — with the Python
> validator + renderer + preview sheet and the Kotlin load check green, and
> every kit that iterates the bundled puzzles green. Measure the start-up
> load and the grid with the full bundle. Owner rules 6–7 bind.
> Mode: decide from the four reasons — Light unless one is written in the WO.
> Verification channels at G3: the Python tool channel; JVM (the two-run line);
> the device sets on the dev image Medium_Phone_API_37.0 AND the floor image
> Phone_API_26 AND the tablet image Medium_Tablet_API_37.0 (a standing channel
> since WO-006: the app set; the floor first, the whole sets once);
> V-01 + positive control. Puzzle titles to review-notes.md. Stations as
> subagents; the four scripts are duties. End every turn with the one signal.

**Summary (orchestrator, Opus 5.5; 20:00 → 20:27):**
- Read the WO-010 set. Light (no new module, nothing governed changed,
  no format change, no hard-stop domain). Scope = WO-002's ten lines +
  "every picture inside its outline"; written into `workorders/WO-010.md`
  with ten questions for the planner.
- Found: the locked puzzle format allows only `author` and
  `reviewedByHuman` under `provenance`, so each figure's source and
  licence are credited in `ASSET-LICENCES.md` instead — the owner's own
  words; no format change (`decisions.md`).
- G2 route done: `architecture.md` v1.2 (G-16: GPL-3.0 puzzle figures,
  credited, pictures our own; O-06: the DEV aid's strings line); hatch
  opened and closed, baseline re-frozen 20:05.
- G3 channels proven: Python tools (13/13), JVM 243/0, V-01 3/3 + control
  2/2, dev / floor / tablet images 2/0 each (one void floor boot: a
  serial clash hung adb; restarted). Run log `.swdev/run-log/WO-010.md`.
- 20:27 `wo-planner` dispatched; next: plan-check, plan review, G3.

**Human input (verbatim, 20:5x):**

> plz pause when getting change

**Summary (orchestrator, 20:54):**
- Pausing at the next safe point, the planner's hand-back; no further
  station will be dispatched. At 20:54 the planner had written nothing yet.
- Recorded now so a closed session loses nothing: WO log PAUSED row,
  KICKOFF "WO-010 — PAUSED" (where it stands, both resume cases, the
  paste-in prompt), AGENTS.md current phase. Tablet emulator stopped.
- 21:06 the planner came back; received 21:07: 10 tasks (4 picture
  batches in parallel), plan check GREEN (re-run), traceability GREEN
  11/11. About 93 figures import, not ~144: 42 break the game's locked
  rules (corner-only joins, off-45° pieces, inexact), 9 are duplicates.
  The play test set will take about 2–2.5 h per emulator.
- Two owner questions put on the WO (turned copies; how ratings are
  measured), defaults stand 2026-10-21. The planner's ten readings are
  ruled at the resume. Paused; nothing running.

## 2026-10-19 — WO-010 resumed (P3 → P4, fresh orchestrator context)

**Human input (verbatim, 06:09):**

> WE ARE IN PHASE P3 — WORK ORDER WO-010 content batch, RESUMED after a pause
> (TangramPlaytime, SWDev v0.20, intake path C). Read AGENTS.md first (incl. the
> "from P3 on" framework files), then governance.md, workorders/WO-010.md (whole,
> Log to the last row), architecture.md v1.2 (G-05, G-16, O-06), build-map.md,
> design-inputs.md (DI-5, DI-10, §2 items 9, 14–16), the WO-010 rows of
> decisions.md (2026-10-17, 2026-10-18), .swdev/run-log/WO-010.md, the last
> progress.md entry, KICKOFF.md "WO-010 — PAUSED", platform-notes/android.md.
> The planner's hand-back is received (WO log 21:07): first rule its readings
> R1–R10 against tasks.md § WO-010 › Seams and write the decisions.md rows.
> Then plan review, G3 (row 7), and P4: the kit author and the implementers, code
> review, Test & Verify on the floor, dev and tablet images (the floor first,
> the whole sets once), trace-check, verifiers, wo-metrics, close. Stations as
> subagents; the four scripts are duties. End every turn with the one signal.

**Summary (orchestrator, Opus 5.5; from 06:13):**
- Framework v0.20 = project, no drift. HEAD `c953230` "Backup" (the
  owner's snapshot of the paused state); no emulator running.
- The planner's ten readings ruled: all accepted as written, ten
  `decisions.md` rows under its titles (two are guardrail readings for
  the G3 surface: the sheets/prototype one and the asset-record one).
- Run log: tree definitions extended, helper re-made (r-base); the
  eyes channel (headless Chrome PNG of a sheet) proven (g3-eyes).
- Dispatch ruling: code review after the last kit stage and before the
  long device runs, so a review fix does not re-open 7–8 h of devices.
- 06:16 `plan-reviewer` dispatched; back 06:17: no blockers, three
  small fixes (two applied to the plan, one carried in dispatch);
  plan check green again.
- 06:18 G3 passed (row 7): plan approved, P4 opened. The test-kit author
  dispatched for its first stage (the corrected order test, the
  counter-digits pin).
- 06:33 kit stage 1 accepted (one corrected check: the order test had
  pinned a fact of the 13-puzzle bundle); the counter pin red as
  planned. 06:34 the counter fix dispatched.
- 06:36 the counter fix accepted (one line; all 245 JVM tests green).
  06:37 the start-up/grid measurement and its 13-puzzle baseline on the
  three emulators dispatched.

**Human input (verbatim, ~07:00):**

> why you start spamming?

**Summary (orchestrator, 07:01):**
- Cause: the measurement station runs its emulator steps as many short
  background commands; each one that ends sends me an interim "still
  waiting" notice, and I answered every one (eleven since 06:47) with a
  status line and the turn signal. None carried news.
- Fix from now on: interim notices with no hand-back get no message
  from me; I report only when a station hands back or something needs
  the owner. Device stations from TASK-036 on are briefed to run each
  emulator's steps as one background script, so they wake me rarely.
- Filed in `proposals.md` (the signal rule has no "working" state).
- 07:14 the start-up/grid baseline accepted (13 puzzles; slowest
  start 2.5 s on the tablet, grid opens in about 0.2 s everywhere — far
  under the 5 s / 1 s thresholds; the grid does not scroll yet).
- 07:15 the import task (converter, ~93 new puzzle files, the licence
  record, a floor run) dispatched, its emulator steps as one script.

**Human input (verbatim, 07:47):**

> next change lets start shutdown process.. we wait still moment but no new ones anymore

**Summary (orchestrator, 07:47):**
- Pausing at the next safe point: the import task's hand-back. It is
  received and checked when it comes; no new station is dispatched (the
  four picture batches wait). Then the pause is written down for a
  fresh context: WO log, KICKOFF resume section, AGENTS.md phase line.
- 08:01 the import task accepted after my own checks: 93 puzzles
  imported, 51 of 144 skipped with reasons (25 joined only at a corner,
  17 not on exact positions, 9 duplicates); 6 turned copies kept and
  flagged; every file valid; all JVM tests and the two floor classes
  green over 106 puzzles; on the oldest phone the start is 1.35 s and the
  grid opens in 0.35 s, far under the limits. The ratings came out
  18 / 20 / 23 / 10 / 22 (checkpoint item 4's numbers corrected; the
  rule is the same). Pictures are still placeholders.
- **PAUSED 08:01.** Nothing running, no emulator up. Resume: KICKOFF
  "WO-010 — PAUSED again" (next: the four picture batches in parallel).

## 2026-10-19 — WO-010 resumed again (P4, fresh orchestrator context)

**Human input (verbatim, 18:26):**

> WE ARE IN PHASE P4 — WORK ORDER WO-010 content batch, RESUMED after a second
> pause (TangramPlaytime, SWDev v0.20, intake path C). Read AGENTS.md first (incl.
> the "from P3 on" framework files), then governance.md, workorders/WO-010.md
> (whole, Log to the last row), tasks.md § WO-010 (Seams, the shared-file table,
> the rows and their status cells), workorders/WO-010-briefs.md (header, dispatch
> order, channel table), .swdev/run-log/WO-010.md, the WO-010 rows of decisions.md
> (2026-10-18, 2026-10-19), the last progress.md entry, KICKOFF.md "WO-010 —
> PAUSED again", platform-notes/android.md. TASK-033…036 are done (TASK-036
> accepted 08:01: 93 imports with stub pictures). Next: the four picture batches
> TASK-037…040 in parallel (record the pre-fan-out git status first; name the
> art-sensitive device oracles; open every sheet before accepting), TASK-041 (kit stage 2),
> the code review, TASK-042 (every channel on the final tree, floor first, the
> whole sets once), Test & Verify, trace-check, verifiers, wo-metrics, close.
> Stations as subagents; device steps as one background script per image; no
> replies to interim notices. The four scripts are duties. End every turn with the
> one signal.

**Summary (orchestrator, Opus 5.5; from 18:26):**
- Framework v0.20 = project, no drift. HEAD `a0d3938` "Backup" (the
  owner's snapshot of the paused state); no emulator running.
- Tree-hash helper re-made in this session's scratch; every tree hash
  equals the last run-log row (nothing changed during the pause).
- 18:30 pre-fan-out git status recorded (only the hook logs), then the
  four picture batches dispatched at once (26 / 22 / 25 / 20 puzzles):
  shapes + numbers, letters, things + vehicles, animals + nature +
  people. Each was told which device tests will later judge its
  pictures, to keep its own Chrome profile, and to touch only its own
  files' pictures.
- 18:33 a fresh test-kit author dispatched to draft stage 2 (the licence
  record and colour checks) in its scratch; it lands after the batches.
- 18:46 the stage-2 draft back (scratch only; the licence record already
  passes it).
- 18:57–19:11 the four batches back and accepted one by one (animals /
  nature / people, things / vehicles, shapes / numbers, letters): every
  file valid, 3–8 colours, only the picture changed; I looked at all 93
  sheets on contact pages. All 93 new puzzles now have pictures; the 13
  old sheets are untouched. Title notes for the owner (mixed casing,
  "Standing", "Double Convex Hexagon" is an hourglass, the faint Letter
  Z) go to `review-notes.md` at close. One picture to watch on the
  device: `shapes-convex-12`.
- 19:12 the test author resumed to land stage 2 and run the JVM line.
- Two small frictions filed in `proposals.md` (the approver blocks a
  read-only git command; the fan-out record should list my own ledgers).
- 19:14 kit stage 2 accepted: the licence record and colour checks are
  in the tree and green; all 261 JVM tests pass over 106 puzzles (my own
  re-count). Plan check still green.
- 19:16 the independent code review dispatched (before the long device
  runs, so a fix does not re-open them).
- 19:24 code review back: no blockers. It re-checked the import from the
  source (every rating, every geometry, the licence record). Two small
  fixes ordered now: a README line on how to re-run the converter, and a
  build-file line so a change to the licence record alone always re-runs
  its test. Four notes go to the close (one licence detail for WO-009,
  two Finnish titles for the review notes).
- 19:31 both fixes accepted after my own check of the two diffs; the
  recipe was run before it was written (it also needs the old batch
  lists out of the way); all 261 JVM tests green.
- 19:33 the final run dispatched: every check once on the final code —
  the oldest phone image first, then the dev phone, then the tablet;
  expected to take several hours of emulator time.
- 20:38 no change: the final run is on the oldest phone image (one
  emulator up, as planned); no hand-back yet, nothing for the owner.

**Human input (verbatim, ~21:09):**

> we done here? I cant see nothing spinning? do we need to remove some older phone supports? Maybe we raise olderst supported to Android 12 or 13?

**Summary (orchestrator, 21:10):**
- Not done: TASK-042 runs headless (no emulator window), on the Android 8
  floor since 19:38. At 21:09 its test process was busy (≈96 % CPU) in
  the grid-colour test, which checks all 106 puzzle cells one by one —
  slow on the old emulator, not hung. Then the dev phone and the tablet;
  several more hours. Then Test & Verify, trace-check, verifiers,
  metrics, close.
- The floor: nothing needs removing. Android 8 (API 26) is the build's
  choice under the owner's G1 answer ("android 8 is more than enough,
  can go way higher if it simplifies development"); it is in
  `architecture.md` §2 and the pending `chg-draft-req006-platform-floor.md`,
  not yet in a locked REQ. Installed images: API 26 and 37 only.
- Recommended: finish WO-010 on Android 8 (changing it mid-run voids the
  final run), then raise the floor before the next WO. Asked the owner
  which version (8 / 12 / 13).

**Human input (verbatim, 21:47):**

> Lets move to Android 12 as oldest supported.

**Summary (orchestrator, 21:48):**
- Recorded the owner's words as `capture-outbox/src-draft-android-12-floor.md`
  and amended the pending REQ-006 floor draft to "Android 12 (API 31) and
  later"; a `decisions.md` row says how and when it is applied.
- WO-010 closes on Android 8 (its final run is under way on that image).
  Before the next WO's device tests: `architecture.md` v1.3 through the G2
  route, a new Android 12 emulator image installed and proven as the floor,
  and `minSdk 31` as the next WO's first task (all channels re-run there).
- TASK-042 continues untouched.

**Human input (verbatim, 22:00):**

> time to start shutting down for today.. and continue later.. anyway looks like this 2 hour session is stick

**Summary (orchestrator, 22:01):**
- Checked first: the oldest-phone run had finished 58 tests by 20:01 (one
  failed), then sat in one test — the grid colour check over all 106
  puzzles — for 2 hours with the CPU busy. So the owner was right that it
  was stuck in practice.
- Stopped it cleanly: the station stopped, emulator killed, Gradle stopped,
  no device left running.
- What stands on the final code: puzzle tools, all 261 JVM tests, the
  release check and the verifiers, the device-test compile — all green.
- Two findings for the next session: a badge-tap test fails on Letter M
  (the badge reaches past the screen's edge), and the grid colour test is
  far too slow at 106 puzzles. Each needs a reading (test or product).
- Written down for the resume: WO log, run log (the test lines quoted),
  `tasks.md` (TASK-042 blocked), KICKOFF "WO-010 — PAUSED a third time"
  with the paste prompt, AGENTS.md phase line. Nothing running.

## 2026-10-20 — WO-010 resumed a third time (P4, fresh orchestrator context)

**Human input (verbatim, 05:51):**

> WE ARE IN PHASE P4 — WORK ORDER WO-010 content batch, RESUMED after a third
> pause (TangramPlaytime, SWDev v0.20, intake path C). Read AGENTS.md first (incl.
> the "from P3 on" framework files), then governance.md, workorders/WO-010.md
> (whole, Log to the last row), tasks.md § WO-010 (Seams, the rows and their
> status cells), workorders/WO-010-briefs.md (header, dispatch order, channel
> table, the TASK-042 brief), .swdev/run-log/WO-010.md, the 2026-10-19 rows of
> decisions.md, reviews/WO-010-code-review.md, the last progress.md entry,
> KICKOFF.md "WO-010 — PAUSED a third time", platform-notes/android.md.
> TASK-033…041 are done, the code review is done (S1, S2 fixed), TASK-042 is
> blocked: steps 1–4 green (t042-a), the floor play set stopped with two
> findings F1 (DecorationKitTest on letters-m) and F2 (OverviewKitTest REQ-050.A2
> ran 2 h). Next: route F1 and F2 (class alone on the floor, under timeout),
> then TASK-042 from the floor step (floor, dev, tablet; one background script
> per image), Test & Verify, trace-check, verifiers, wo-metrics, close — WO-010
> closes on Android 8; the Android 12 floor move comes right after. Stations as
> subagents; no replies to interim notices. The four scripts are duties. End
> every turn with the one signal.

**Summary (orchestrator, Opus 5.5; from 05:51):**
- Framework v0.20 = project, no drift. HEAD `a0d3938`; no emulator
  running. Every tree hash equals the last run (nothing changed during
  the pause), so the green steps 1–4 of the final run still count.
- F1 (the badge test on Letter M): the badge's touch area on the board
  runs about 3 dp off the right edge of the screen. The rules say the
  area is at least 60 dp and sits on the board, so this is checked as a
  product defect first (a fresh implementer), then the test's own
  helper if it still fails. Same phone size on Android 8 and the dev
  image, so not an Android 8 issue.
- F2 (the grid colour test that ran 2 h): the test checks the 106 grid
  cells one at a time, slowly. The grid's own speed with the drawn
  pictures is measured first; if the grid is fine, the test author
  makes the test fast while still checking every cell.
- 06:18 F1 back: a real product defect, fixed. On 25–28 of the 104
  puzzles with a parallelogram (depending on the screen), the badge's
  60 dp touch area ran off the board's edge (the visible disc never
  did). It is now kept on the board; JVM tests 263/0 (my own re-count);
  a `decisions.md` row records where the area sits at an edge. The
  badge test still fails, now inside the test's own disc-finding helper
  (a piece of the same blue as the badge's ring touches it): a test
  defect.
- 06:18 the test author got both test defects: the disc helper (F1),
  and the slow grid test (F2) — grid speed measured first, then the
  test made fast while still checking every cell; any similar slow
  test looked for too.
- 07:21 no change: the test author is still working on the Android 8
  image (its own background runs); no hand-back yet, nothing for the
  owner.
- 08:27 still running: its fourth floor session started 08:27 (one
  emulator up, as planned); no hand-back yet.
- 08:56 the test author back, accepted after my own re-count of every
  result. The grid itself is fast with the pictures (opens in 0.3 s on
  Android 8). The 2-hour stall was a test-tool trap: a scroll that never
  finished under the test's frozen clock — at 13 puzzles no cell needed
  a scroll. Fixed, and the same scan found two more tests that would
  have run for hours (the solved-picture and tray checks); all fixed,
  every check still covering every puzzle. The badge test passes. One
  test (every puzzle reachable from every other) takes 24 min on its
  own — that is the check itself, accepted.
- 08:57 a fresh code reviewer checks all of these fixes before the
  final run on the three images.
- 09:11 review back, no blockers. The badge fix is right. One test
  detail to tighten first: the badge test's helper would no longer
  notice a badge drawn twice — sent back to the same test author. The
  test-tool trap is now written into the Android notes so it is not
  hit again. Two small notes need no change.
- 09:34 the tightened badge test back and accepted (14/0 on Android 8,
  my own re-count): it again fails on a badge drawn twice, and still
  reads the real badge.
- 09:35 the final run dispatched on the final code: JVM tests and the
  release check again, then Android 8, the dev phone and the tablet,
  one script per image. New this time: if no test finishes for 40
  minutes, the run stops itself and reports — no more silent 2-hour
  waits. Expected several hours of emulator time.
- 10:38 the final run is still going (about an hour in, on its own
  emulator scripts); nothing to judge yet, no change to the ledgers.
- 12:21 the final run came back. Android 8 is fully green; the speed
  limits are not crossed; the play set took 48 minutes, not the feared
  2 hours. On the dev phone (Android 17) one check failed: in the
  solved vulture picture, one pixel of the dark silhouette shows
  through inside the picture. The picture's own colours cannot make
  that pixel, so it is the app's drawing, the puzzle's shape data or
  the test's mask. Sent to a fresh implementer to find out which and
  fix it (the same way as this morning's badge). The tablet waits; after
  the fix the whole run repeats on all three emulators.
- 12:49 the vulture pixel is the test's fault, not the app's. The
  solved picture has a thin outline in the silhouette colour by design.
  At sharp inward notches the test thought pixels right on that outline
  were well inside the picture. Only one other puzzle (Letter Z) is
  affected. I checked this against the code and logged it as a
  decision. I also found and repaired a decisions-table row my own
  earlier edit had broken. The test fix is now with the test author,
  who also checks seven other places that use the same mask. Then the
  full run on all three emulators.
- 13:51 the test author is still working (about an hour in, on the
  emulators); nothing to judge yet, no change to the ledgers.
- 13:56 the test fix is back and checks out: the tests now measure
  "inside the picture" from the true outline, and a new self-check
  shows the old way was wrong for the vulture and Letter Z. Eight
  other tests had the same flaw and are fixed; one was fine as it was.
  Everything is green on Android 8 and the dev phone, all 106 puzzles
  judged. No app or puzzle file changed. A code reviewer now reads the
  test change before the full run on all three emulators.
- 14:09 the review is back: the test change is sound (the reviewer
  re-checked the outline maths exactly on all 106 puzzles and tried
  four ways to break the self-check; all were caught). One small ask:
  explain at one test why it keeps the old mask, and write the lesson
  down. I wrote the lesson into the Android notes; the test author
  adds two comment lines. Then the full run on all three emulators.
- 14:11 the two comment lines are in, and I checked they are only
  comments. The full run has started on the final code: JVM tests and
  the release check, then Android 8, the dev phone and the tablet, one
  emulator at a time. Expect several hours.
- 15:24 the full run is still going (about 1 h 15 min in); nothing to
  judge yet, no change to the ledgers.
- 16:43 still going (about 2 h 30 min in); nothing to judge yet, no
  change to the ledgers.
- 16:45 Jami asked: "is there really happening anything anymore or just
  open sub sessions those dont do anything?" Checked the machine: yes,
  real work. The tablet emulator is up and running its last big step
  (the app set, started 16:44). Android 8 finished at 15:23 and the dev
  phone at 16:42; by the run script's own summary both are green (play
  set 97/0, the app set with only the two guarded phases). Not yet my
  verdict: I re-count every result when the run hands back.
- 17:07 the full run is done and green on every channel: JVM tests
  263/0, the release check passes, Android 8 and the dev phone 97/0 on
  the play tests, the app tests green on all three emulators (only the
  two guarded phases). I re-counted every result file myself. The speed
  limits are not crossed anywhere (start-up 1.5–2.1 s, grid open about
  0.2–0.3 s). All ten WO-010 tasks are done. Test & Verify has started:
  a fresh station takes this run as its baseline, checks the tests
  really judge what each requirement says, and re-runs the quick
  channels itself.
- 17:22 **WO-010 closed.** Test & Verify green (all 11 requirement
  lines on every channel, no defects); the traceability check green
  (its station first claimed a report it had not written; sent back,
  now written); the plan check, the release check and the metrics run.
  Written at the close:
  - the metrics (hand-corrected where the script counted the wrong
    days);
  - 17 corrected test checks and this WO's frictions → `proposals.md`;
  - the title notes and a play-test list for you → `review-notes.md`;
  - the G4 decision;
  - KICKOFF "State after WO-010" with the next session's prompt. Next
    is the move to Android 12 as the oldest supported version, then
    the next work order.
  No emulator is running. Please commit the snapshot.
