# KICKOFF — TangramPlaytime (the TangramNoAds re-run under SWDev v0.20)

**Why this project exists.** The first run (`C:/GitHub/AI/TangramNoAds`,
SWDev v0.15, 2026-10-02…05) built the game from this exact collection and
was harvested into SWDev v0.16–v0.20 and SwReqCollector v0.5
(`SWDev/project-review-tangramnoads-2026-10.md`). This project is the
verification: the **same locked requirements and the same design inputs**,
built again under the improved frameworks. Do not read the first run's
code, designs, decisions or reviews — comparing comes after, not during.

**What is already in place (scaffold 2026-10-08):**
- `Requirements/` — the locked collection v1.1 (47 locked REQs, 4
  withdrawn, 7 TYPEs), copied unchanged; views regenerated with
  SwReqCollector v0.5 (`views/lock-receipt.md` is new).
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

**Owner steps before the first session** (each is yours, not the AI's):
1. `governance.md` → set `**Status:** in force` (F4 — only you).
2. Commit the scaffold (`git add -A && git commit -m "Scaffold under SWDev v0.20 (TangramNoAds re-run)"`).
3. Make sure the API 26 (minSdk floor) emulator image still exists — it is
   a **G3 precondition** for the first WO with device tests (v0.18).

---

## Paste this to start the first session

```text
WE ARE IN PHASE P1 — ACCEPTANCE OF A LOCKED COLLECTION (intake path C,
C:\GitHub\AI\SWDev\initnewproject.md §2b, SWDev v0.20). Read AGENTS.md
first. The requirement collection is Requirements/ in SwReqCollector
format v2, locked 2026-10-01 (v1.1). It IS the spec: read its CLAUDE.md,
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
     "Accept collection v1.1 for TangramPlaytime (G1)?"
  4. Record my yes in progress.md with approver + date. Then P2:
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
