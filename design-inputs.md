# Design inputs — TangramPlaytime

**Status:** Current (§2 derived at P2, 2026-10-11)  ·  **Last updated:** 2026-10-11
**Contract:** `Requirements/` collection v1.2 — REQ-001..051 (47 locked, 4 withdrawn), TYPE-001..007, locked 2026-10-01 (SRC-014, SRC-015), REQ-048 re-signed 2026-10-05 (CHG-001/002); G1 accepted for this project 2026-10-11. Pending deltas: 24 chg-drafts in `capture-outbox/`.

> Inputs the pipeline must honor beside the requirements. Each is named in
> the work orders that use it (`work-order.md` → Design inputs) and in the
> briefs' "Read exactly". Contract wins over any input; §2 lists the known
> disagreements. **An input is read, never rebuilt, ported or re-tested
> inside a work order** — it is never Definition of Done. **A `generated`
> input supplies no Contract text** (SWDev v0.20): a reading taken from the
> prototype or an AI-written spec is an ASSUMPTION on the capture side (a
> `chg-draft` in `capture-outbox/` if a REQ must say it) or a `decisions.md`
> row here — never "binds like the Contract".

The material below is the v1 intake as it stood on 2026-10-01 (TangramNoAds
commit `10ccac4`), copied unchanged: the same inputs the first run had, so
the re-run is comparable. Nothing the first run produced after that date
(its code, designs, decisions, verifiers, the twelve later puzzles) is here.

---

## 1. Register

| # | Path | Role | Source / provenance | Status | Used by |
|---|---|---|---|---|---|
| DI-1 | `Spec/prototype/tangram-prototype.html` (source `tools/prototype_template.html`, built by `tools/build_prototype.py`; browser tests `tools/tests/test_prototype.py`) | reference prototype | agent, prototype 0.6 (2026-09-28), SRC-004 `generated` | current | behaviour reference only — **never** a source of acceptance tests, never DoD |
| DI-2 | `Spec/02-ui-layout.md` + `Spec/ui/01–06*.png` | explanatory spec (layout algorithm, zones, sizes, visual language; screenshots generated from DI-1) | agent, draft 0.4, SRC-004 `generated` | current | #Layout, #Solving (tray), #Settings |
| DI-3 | `Spec/01-gameplay.md` | explanatory spec (gameplay in prose) | agent, SRC-004 `generated` | current | #Solving, #Browsing |
| DI-4 | `Study/06-snapping-by-anchors.md` (+ `Study/08-round2-decisions.md`) | explanatory spec (the lock algorithm; **frozen folder**, owner rule 3) | agent, rounds 1–2, SRC-004 `generated` | current, frozen | #Locking |
| DI-5 | `Spec/03-puzzle-format.md`, `Tangrams/puzzle.schema.json`, `Tangrams/*.json` (13 puzzles + previews), `tools/validate_puzzles.py`, `tools/tangram_geom.py`, `tools/render_puzzle.py` | content pipeline | agent + owner-reviewed decisions (rounds 3, 6); puzzles not human-reviewed (`provenance.reviewedByHuman` false) | current | #Content; **governed-interface candidate** (puzzle format / library) at P2 |
| DI-6 | `AGENTS.md` owner rules 1–13 | reference material (the owner's standing rules) | owner (Jami) | current | project-guardrail material for `architecture.md` §2 |
| DI-7 | `Spec/00-vision.md`, `Spec/05-open-questions.md` (decision log), `Spec/04-requirements.md` (**superseded** by `Requirements/`) | reference material | agent + owner decisions | current (04 superseded) | orientation only |
| DI-8 | `Requirements/evidence/src-016-first-build-owner-decisions.md` (SRC-016) | reference material — **the owner's standing answers from the first build**: Kotlin + Jetpack Compose native with the prototype's logic ported; Google Play; 25 puzzles for the first release; a `mark` command for human puzzle review; play time counts all in-app time with an idle cut-off of about a minute; title "Tangram Playtime" (CHG-001); icon option A (CHG-002, DI-9) | owner (Jami), verbatim, 2026-10-01…05 | current | every WO: when the same fork appears, these are the **defaults** of the checkpoint item (still asked when product-visible, never silently re-decided the other way) |
| DI-9 | `Spec/icon/option-A-foreground.svg` + `option-A-background.txt` (+ `README.txt`) | hifi handoff — the launcher/store icon the owner picked | AI-proposed, **owner-picked 2026-10-05** (SRC-016) | current | #Release WO (CHG-002) — used as is, not redrawn |

## 2. Contract-delta — where inputs disagree with the Contract

> Contract wins. **To be derived at P2 by this run** — read every input
> against the locked REQs and list the superseded statements and open forks
> here before G2. (For comparison *after* the run, not before: the first
> run's delta is `C:/GitHub/AI/TangramNoAds/design-inputs.md` §2.)

Derived 2026-10-11 by this run from DI-1…DI-9 read against the locked REQs and TYPEs (the first run's delta was not read).

### Superseded (do NOT follow these)

1. **DI-4 `Study/06` §Candidate search — per-level lock distances (Easy 0.9 / Medium 0.65 / Hard 0.45), Easy tries k ± 1, Easy and Medium try the mirrored parallelogram, score terms 0.25 × turn change + 0.1 × flip change, preview off on Hard** → TYPE-004 (one set: R = 0.65, exact turn and mirror, score = |t| − 0.04 × snug corners, window 0.16), REQ-021 (preview always on), REQ-004/027/028 withdrawn.
2. **DI-4 `Study/06` §"Puzzles must be buildable edge-first (V11)" and "the classic square was removed"; DI-4 `Study/08` row 1 (V11 as a content rule, Gift box archived)** → REQ-038 (V11 is information for authors; the square is a puzzle again), owner rule 6.
3. **DI-1 prototype `findLock`: the candidate window compares a candidate's |t| against the best *score*** → TYPE-004 text: only candidates with |t| ≤ (best |t|) + 0.16 are considered. The port follows the TYPE (ADR-001).
4. **DI-1 prototype `haptic()` = `navigator.vibrate(12)`** → on Android the equivalent `Vibrator` API needs the `VIBRATE` permission, which REQ-010 forbids; use the permission-free haptic-feedback API (G-11).
5. **DI-2 `Spec/02` §5 "Type: a rounded display face (e.g. Baloo 2)" and DI-1's Google Fonts `<link>`** → REQ-010 (no network) and G-16: system font only (ADR-005).
6. **DI-2 `Spec/02` §4 "⚙ 44 dp", "Restart pill ≥ 44 dp high"** → REQ-037: every player control at least 48 dp (‹ › 52 dp, flip badge 60 dp). Gear and Restart are 48 dp or more.
7. **DI-7 `Spec/00` "store name Tangram, absolutely free"; DI-3 `Spec/01` and DI-7 `Spec/05` "the Finnish store title is still open"** → REQ-048 as changed by CHG-001/002: "Tangram Playtime" in both store languages, the promise in the short description, launcher icon = DI-9.
8. **DI-3 `Spec/01` §3 "Mouse wheel or R / Shift+R during a drag turns the piece"** → desktop testing aid of the prototype only; no REQ asks for it (principle 8). Not built.
9. **DI-3 `Spec/01` §9b, DI-5 `Spec/03` V8 "title.fi mandatory", owner rule 13 as originally written** → the owner's 2026-10-11 decision (`capture-outbox/src-draft-g1-owner-answers.md`): English is the Contract wording, Finnish a reference translation; pending `chg-draft-req047-english-contract.md`. Until the capture side applies it, the puzzle files keep both titles (the locked V8 text still stands) and the build adds `fi` strings as reference only.
10. **DI-5 `Tangrams/README.md` "The first release needs at least 20 reviewed puzzles"** → REQ-042 says at least 20; the owner's standing answer (DI-8, SRC-016) is **25** for the first release — the default of the matching checkpoint item in WO-002/WO-009.
11. **DI-1 prototype saves every 5 active seconds and on each piece change (`saveAll` / `persistPieces`)** → REQ-025 says only "the state survives closing the app"; the build saves at every state change (pending `chg-draft-req025-unreadable-state-and-save-timing.md`); the prototype's cadence is not a rule.
12. **DI-1 prototype DEV button "hidden in the design sheets", "until the page is restarted"** → web-only clauses; the Android reading is "until the process restarts" and "debug build type only" (G-08; pending `chg-draft-req046-android-build-terms.md`).

### Open forks (owner decision needed)

- none. The forks the first run met (channel, puzzle count, review marking, time counting, icon) are answered in DI-8 and are the defaults of the matching checkpoint items; the G1-surface answers of 2026-10-11 (channel, floor, English-only, licences) are in `capture-outbox/src-draft-g1-owner-answers.md`.

### Also fine (input and Contract agree — worth noting when non-obvious)

- **Time counting (DI-1, DI-3 §8, TYPE-005, REQ-029/030):** today's and all-time play time count every active second whatever is on screen (settings included); a puzzle's own time counts only while that puzzle is In progress and no overlay is open. The owner's 2026-10-05 words ("can be total time … if havent done anything for minute then stop counting", DI-8) describe exactly this.
- **Board fit (DI-1 `layout()`: 0.82 / 0.80 of the board) = REQ-035 "about 10 % padding".**
- **Tray scale computed for the full set, cells = turn diameter × scale + 12 dp, row ≤ max(84 dp, 16 % of height)** (DI-1, DI-2 §3) = REQ-013 rules, verbatim.
- **Anchors = outline corners found by the 360° / 180° angle-sum rule** (DI-4 §Anchors, `tangram_geom.outline_corners`) — the kernel port uses the same rule on the puzzle's solution polygons.
- **Reduced motion (DI-2 §6, REQ-023, REQ-051):** no pop, confetti or pulse animation; the picture and the corner markers still appear.
- **DI-9 icon:** option A used as is (foreground SVG + background colour → adaptive icon); REQ-048.A4.

## 3. Change log

| Version | Date | Change | Reason |
|---|---|---|---|
| 0.1 | 2026-10-08 | register pre-filled from the v1 intake material (commit 10ccac4); §2 left for P2 | the TangramNoAds re-run under SWDev v0.20 (T-28) |
| 0.2 | 2026-10-08 | DI-8 the owner's first-build answers (SRC-016) as checkpoint defaults; DI-9 the picked icon | same inputs **and the same owner decisions** — the AI's own first-run readings are not carried |
| 1.0 | 2026-10-11 | §2 Contract-delta derived from DI-1…DI-9 against collection v1.2: 12 superseded statements, no open forks, 6 agreements; Status → Current | P2 (G1 accepted 2026-10-11) |
