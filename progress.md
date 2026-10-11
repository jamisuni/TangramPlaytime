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
