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
