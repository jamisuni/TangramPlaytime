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

## State after WO-002 (2026-10-11, closed 15:45)

- **WO-002 #Content done** (`workorders/WO-002.md`, Full (b)): `:content` has
  `BundledPuzzles` (the 13 `Tangrams/*.json` bundled as **Java resources**
  by a Gradle task — `content/build/generated/puzzles/`, read through one
  `ClassLoader` call on JVM and device), `PuzzleParser`/`PuzzleDtos`,
  `PuzzleLoader` (15 `Rule`s, sealed `LoadResult`), `ValidityCheck` + `Edges`
  (congruence and overlap through `ILockEngine`), `PuzzleLibrary` and the
  entry point **`PuzzleLibraries.bundled(engine: ILockEngine): IPuzzleLibrary`**
  (fails fast on a bad bundled file). Kit 16 + 37 tests, 10/10 IDs green;
  trace-check `--wo` GREEN; code review 0 B. `IPuzzleLibrary.kt`, the schema
  and `Tangrams/` unchanged; one notify delta (`build-map.md` v1.2: #Content
  depends on the kernel via `ILockEngine`). `tools/tangram_geom.outline_corners`
  now uses the contiguity rule (warmup-4 gains (2,2); `tools/tests/test_tangram_geom.py`).
- **Carry into WO-003 #Solving (first WO with device tests):** wire
  `PuzzleLibraries.bundled(LockEngine())` in `:app` (design review F4) and
  call it once on the **floor image** `Phone_API_26` — the bundle read is
  proven on the JVM and by APK/AAR listings only (code review N4; check R8
  keeps the resources in the release build). Carried acceptance lines:
  **REQ-020.A2** (no error message for a missed drop), **REQ-039.A1** (no
  picture paints outside its outline — the solved-picture drawing clips),
  **REQ-045.A2** (placing the three mini pieces shows the solved picture).
  Add a `build-map.md` row for the rendering half of #PuzzleArt (code home
  `play/`) as WO-001 did for #SolvedCheck, or trace-check reads the play
  test as directory theater. Doc drift in the locked `architecture.md`
  (code review N5): §3 says "assets" (they are Java resources) and G-15
  names `LoadResult.Unreadable` (the real type is `LoadResult.Rejected`) —
  the owner may fix the wording; not a Contract change.
- **Carry into WO-004 #Browsing:** REQ-040.A1, REQ-041.A1, REQ-045.A1 are
  proven at the seam; the screen half ("a fresh install opens on …") is
  re-covered there. `ILockEngine.stillValid` scope question from WO-001 stands.
- **Content batch WO (Light, after WO-003, before WO-009):** author new
  puzzles to the owner's first-release count (checkpoint item 1 — default
  **25**, stands 2026-10-14); the validator's V3 (vertex-set equality) is
  looser than the Kotlin congruence (boundary order, 45° turns) and the
  `rot/flip/at` form is rejected by the app (`PLACEMENT`) — author puzzles
  in the `polygon` form and run both checks. REQ-042.A1 / REQ-039.A2 are
  WO-009's with the owner's review marks.
- **Lock behind build:** 25 outbox drafts still await a capture session;
  REQ-038.A1 (and A2) stay `implemented (unverified)` until
  `chg-draft-req038-validity-rules-in-contract.md` is signed; REQ-019.A1–A4
  until the TYPE-004 dictionary draft is.
- **Owner steps:** commit the WO-002 snapshot (prompted on the G4 surface);
  answer checkpoint item 1 if 25 is not the number; run a capture session on
  `capture-outbox/` when convenient; look at `review-notes.md` (the 13
  puzzles and their Finnish reference titles).

## Paste this to start the WO-003 session (one orchestrator context per WO)

```text
WE ARE IN PHASE P3 — WORK ORDER WO-003 #Solving (TangramPlaytime, SWDev
v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
files), then governance.md, architecture.md, build-map.md, design-inputs.md
§2, decisions.md, the last progress.md entry, KICKOFF.md "State after
WO-002", capture-outbox/ (pending deltas), platform-notes/android.md.
WO-003 scope: the #Solving subtree not yet built — #Board, #Tray, #SizeMarks,
#Drag, #TapTurn, #TwistTurn, #Flip, #LandingPreview, #CornerTeach,
#SolvedPicture (the REQ ids those tags carry in Requirements/features.md;
#Locking and #SolvedCheck are built, #TurnButtons is withdrawn) — the Play
screen in :play (Compose) consuming ILockEngine (:kernel) and IPuzzleLibrary
(:content, PuzzleLibraries.bundled wired in :app, design review F4) — plus
the carried lines REQ-020.A2, REQ-039.A1, REQ-045.A2 (KICKOFF "State after
WO-002"). Persistence (IProgressStore) is WO-004's: the screen holds its
state in memory this WO, behind the seam. Design inputs: DI-1 (prototype,
behaviour reference only), DI-2 (Spec/02 layout), DI-3 (Spec/01 gameplay),
DI-4 (Study/06, frozen), DI-9 not yet. Mode: decide from the four reasons
(:play is a P2 skeleton like :content was; a governed interface is
consumed, not implemented — say which reason, or Light).
Verification channels at G3: Compose/device tests on the dev image
Medium_Phone_API_37.0 AND the floor image Phone_API_26 (the floor is a G3
precondition for this first device WO — boot it, run the trivial test,
record it), one emulator at a time; JVM tests where the logic is pure;
the release verifier (no debug tooling, canary) arrives with this WO per
platform-notes/android.md. Draw paths in px (G-17). Checkpoint candidates
(≤5, product-visible): colours and the size-mark glyphs only if a REQ leaves
them open; strings to review-notes.md. Stations as subagents; the four
scripts are duties. End every turn with the one signal.
```

## State after WO-001 (2026-10-11, closed 13:04)

- **WO-001 #Locking done** (`workorders/WO-001.md`, Full (b)): `:kernel` has
  `PieceShapes`, `Polygons`, `Silhouette`, `LockRules` and `LockEngine :
  ILockEngine` (all 7 members, 523 LOC); kit `LockEngineKitTest` (14) +
  `GeometryKitTest` (20); 6/6 in-scope IDs green on the JVM channel;
  trace-check `--wo` GREEN; code review 0 B. `ILockEngine.kt` / `Types.kt`
  unchanged; one notify delta on `build-map.md` (`#SolvedCheck` → `kernel/`).
- **Carry into later WOs:** REQ-020.A2 ("no error message for a missed
  drop") is **WO-003's** (seam oracle dishonest; the Play screen proves it).
  WO-002 #Content re-judges `tools/tangram_geom.outline_corners` and the V11
  build-order line: the angle-sum corner rule misses two-wedge corners
  (`shapes-warmup-4` (2,2)); the kernel uses the contiguity rule
  (`design-inputs.md` §2 item 13). WO-004: `ILockEngine.stillValid` checks
  inside + no-overlap only (per KDoc) — decide if REQ-025 needs more.
- **Lock behind build:** 25 outbox drafts still await a capture session;
  REQ-019.A1–A4 stay `implemented (unverified)` until
  `chg-draft-lock-type-dictionary.md` (TYPE-004 values) is signed over there.
- **Owner steps:** commit the WO-001 snapshot (prompted on the G4 surface);
  optionally review the diff (`decisions.md` G4 row recommends it for this
  first WO); run a capture session on `capture-outbox/` when convenient.

## Paste this to start the WO-002 session (one orchestrator context per WO)

```text
WE ARE IN PHASE P3 — WORK ORDER WO-002 #Content (TangramPlaytime, SWDev
v0.20, intake path C). Read AGENTS.md first (incl. the "from P3 on" framework
files), then governance.md, architecture.md, build-map.md, design-inputs.md
§2 (incl. item 13), decisions.md, the last progress.md entry, KICKOFF.md
"State after WO-001", capture-outbox/ (pending deltas), platform-notes/android.md.
WO-002 scope: #Content subtree (REQ-007, 038, 039, 040, 041, 042, 045) in
:content behind IPuzzleLibrary (locked tier — implement, never change):
Tangrams/*.json as assets, the tangram-puzzle/1 parser (kotlinx.serialization,
ADR-004), the load-time validity check (REQ-038 rules; pending
chg-draft-req038-validity-rules-in-contract.md), ordering kind → rating → id.
Design inputs: DI-5 (puzzle format, schema, the 13 puzzles, tools/). Also
re-judge tools/tangram_geom.outline_corners + V11 against the contiguity
corner rule (design-inputs.md §2 item 13) — a project-tool fix, not Contract.
Mode: decide from the four reasons (a new Android library module with assets
is reason (a) if :content counts as new; its skeleton exists from P2 — say so).
Verification channels at G3: JVM unit tests (:content testDebugUnitTest loads
every asset); device channels only if a task needs one — say so in the WO.
Checkpoint candidates (≤5, product-visible): the puzzle count for the first
release (DI-8 default 25, REQ-042 says ≥ 20). Stations as subagents; the four
scripts are duties. End every turn with the one signal.
```

## Paste this to start the WO-001 session (one orchestrator context per WO) — DONE 2026-10-11

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
