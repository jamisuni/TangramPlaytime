# KICKOFF — TangramPlaytime (the TangramNoAds re-run under SWDev v0.20)

**Why this project exists.** The first run (`C:/GitHub/AI/TangramNoAds`,
SWDev v0.15, 2026-10-02…05) built the game from this exact collection and
was harvested into SWDev v0.16–v0.20 and SwReqCollector v0.5
(`SWDev/project-review-tangramnoads-2026-10.md`). This project is the
verification: the **same locked requirements and the same design inputs**,
built again under the improved frameworks. Do not read the first run's
code, designs, decisions or reviews — comparing comes after, not during.

**What is already in place (scaffold 2026-10-08):**
- `Requirements/` — the locked collection, now v1.2 (47 locked REQs, 4
  withdrawn, 7 TYPEs): the first run's v1.1 unchanged except REQ-048, which
  carries the owner's 2026-10-05 decisions (CHG-001/002, SRC-016); views
  regenerated with SwReqCollector v0.5 (`views/lock-receipt.md` is new).
- `Spec/`, `Study/` (frozen), `Tangrams/` (13 puzzles + schema), `tools/`
  (prototype + puzzle pipeline) — the v1 intake material as of 2026-10-01.
- `AGENTS.md` (the owner's rules 1–13 + the v0.20 build section),
  `.claude/settings.json`, `.claude/agents/` (10 stations incl.
  `wo-planner`), `.swdev/guard.json` (path C, P1 values),
  `governance.md` (ai-mastered, **proposed** — the owner sets it in force),
  `design-inputs.md` (register pre-filled; Contract-delta is P2 work),
  `platform-notes/android.md`, `capture-outbox/`, `owner-evidence.md`,
  `review-notes.md`, and empty `architecture.md`, `build-map.md`,
  `decisions.md`, `tasks.md`, `progress.md`, `proposals.md`.

**Decided already (the owner's, from the first run — SRC-016 / DI-8):**
- **Technology:** Kotlin + Jetpack Compose, native; the prototype's locking
  logic is ported, not re-invented (first run's ADR-001). Row 5 is answered.
- **Work-order order** (your kickoff order; `priority: none` is accepted):
  WO-001 #Locking → WO-002 #Content → WO-003 #Solving → WO-004 #Browsing
  (+ the progress store) → WO-005 #DevTools → WO-006 #Layout + #Language +
  #Promise → WO-007 #Settings → WO-008 #PlayTime → WO-009 #Release. The
  same order as the first run, so the two runs compare WO by WO.
- **Standing answers** to the forks the first run met (channel, puzzle
  count, review marking, time counting, icon) are in DI-8 — the defaults of
  the matching checkpoint items.
- **Two change deltas are applied in the collection** (accepted by the
  owner 2026-10-11): `CHG-001` store title "Tangram Playtime" (the promise
  moves to the short description, REQ-048 A2 rewritten) and `CHG-002` the
  launcher icon in scope (REQ-048 A4, asset DI-9). REQ-048 is re-signed.

**Owner steps before the first session** (each is yours, not the AI's):
1. `governance.md` is **in force** (done 2026-10-08).
2. Commit the scaffold (`git add -A && git commit -m "Scaffold under SWDev v0.20 (TangramNoAds re-run)"`).
3. Make sure the API 26 (minSdk floor) emulator image still exists — it is
   a **G3 precondition** for the first WO with device tests (v0.18).

---

## State after the first session (2026-10-11)

- **G1 accepted** (Jami, 2026-10-11): collection v1.2 binds this project.
  `Requirements/reviews/review-01.md` carries all 38 verdicts; 24 chg-drafts
  + 1 src-draft wait in `capture-outbox/` for a capture session (lock behind
  build). Owner decisions taken on the G1 surface are in
  `capture-outbox/src-draft-g1-owner-answers.md` — read them: **English is
  the Contract language (Finnish = reference locale)**, Google Play, Android
  8.0 floor, licence line in Settings, the shared-type numbers confirmed.
- **P2 artefacts:** `architecture.md` (G-01..G-17, O-01..O-08, registry:
  `IPuzzleLibrary` locked; `ILockEngine`, `IProgressStore`, contract types
  notify), `build-map.md` (six Gradle modules), `design-inputs.md` §2 (12
  superseded statements, no open forks), `decisions.md` rows of 2026-10-11.
  G2 status: see `architecture.md` §7 and the last `progress.md` entry.

## Paste this to start the WO-001 session (one orchestrator context per WO)

```text
WE ARE IN PHASE P3 — WORK ORDER WO-001 #Locking (TangramPlaytime, SWDev
v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
files), then governance.md, architecture.md, build-map.md, design-inputs.md
§2, decisions.md, the last progress.md entry, capture-outbox/ (the pending
deltas and the owner's G1 answers), platform-notes/android.md.
WO-001 scope: #Locking subtree (#AnchorLock REQ-019, #GoHome REQ-020,
#SolvedCheck REQ-022) plus the geometry rules of TYPE-001/003/004 in
:kernel behind ILockEngine (:contracts) — the prototype's findLock/validAt/
anchors ported per ADR-001, following the TYPE-004 text. Pure JVM; no UI.
Mode: Full, reason (b) a governed interface touched (ILockEngine is
implemented; its shape may change — notify tier, log the contract-delta).
Design inputs: DI-1 (prototype template, behaviour reference only), DI-4
(Study/06, frozen), DI-5 (tools/tangram_geom.py reference math).
Verification channels at G3: JVM unit tests (:kernel, :contracts); the
device channels are not needed for this WO (no UI) — say so in the WO.
Stations as subagents: design-author → design-reviewer → wo-planner →
plan-reviewer → [acceptance-test-author ∥ slice-implementer] → code-reviewer
→ test-verify → traceability-auditor. The four scripts are duties. ≤5
checkpoint items, product-visible only. End every turn with the one signal.
```

```text
WE ARE IN PHASE P1 — ACCEPTANCE OF A LOCKED COLLECTION (intake path C,
C:\GitHub\AI\SWDev\initnewproject.md §2b, SWDev v0.20). Read AGENTS.md
first. The requirement collection is Requirements/ in SwReqCollector
format v2, v1.2 (locked 2026-10-01; REQ-048 re-signed through CHG-001/002,
2026-10-11). It IS the spec: read its CLAUDE.md,
requirements.md (index), features.md (#Tag tree), reqs/REQ-NNN.md,
req_types.md, views/digest.md, views/trace.md and views/lock-receipt.md.
Every locked level (BIZ/FUN/UI) is Contract. Never write into that
folder — a change is a chg-draft in capture-outbox/.
  1. Run trace-check and confirm it parses the collection (51 REQs, 47
     locked, RED only for uncovered acceptance — no product tests yet).
  2. Dispatch the fresh-eyes requirements review (requirements-reviewer
     subagent, read-only on the collection). Write it as
     Requirements/reviews/review-01.md — the findings table with the
     Verdict column I fill per finding; every finding whose verdict will
     be `chg` also becomes a chg-draft in capture-outbox/. Never edit a REQ.
  3. Present me the digest (views/digest.md), the attention points
     (views/trace.md), the lock receipt, and the review's Blockers; then
     ask the artifact-named question:
     "Accept collection v1.2 for TangramPlaytime (G1)?"
  4. Record my yes in progress.md with approver + date. Then P2 (the stack
     is DECIDED: Kotlin + Jetpack Compose native, the prototype's locking
     logic ported — DI-8/SRC-016; the WO order is the one in KICKOFF.md):
     architecture.md (directives ack, guardrails from AGENTS.md rules 1–13,
     the governed interface registry with tiers), build-map.md (code homes
     per #Tag), design-inputs.md §2 (the Contract-delta — derive it from
     the inputs, do not import the first run's), the floor channel (API 26)
     proven, governance in force — then the G2 question.
Light mode is the default for every WO; Full needs one of the four reasons.
Owner questions are checkpoint items (≤5, product-visible, defaults after
3 days); lists to proof-read go to review-notes.md. The four scripts of a
work order are duties (AGENTS.md). End every turn with the one signal.
```
