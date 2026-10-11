# Design inputs — TangramPlaytime

**Status:** Draft (register pre-filled at scaffold 2026-10-08; P2 completes §2)  ·  **Last updated:** 2026-10-08
**Contract:** `Requirements/` collection v1.1 — REQ-001..051 (47 locked, 4 withdrawn), TYPE-001..007, locked 2026-10-01 (SRC-014, SRC-015)

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

### Superseded (do NOT follow these)

- *(none recorded yet — P2)*

### Open forks (owner decision needed)

- none known

### Also fine (input and Contract agree — worth noting when non-obvious)

- none recorded yet

## 3. Change log

| Version | Date | Change | Reason |
|---|---|---|---|
| 0.1 | 2026-10-08 | register pre-filled from the v1 intake material (commit 10ccac4); §2 left for P2 | the TangramNoAds re-run under SWDev v0.20 (T-28) |
| 0.2 | 2026-10-08 | DI-8 the owner's first-build answers (SRC-016) as checkpoint defaults; DI-9 the picked icon | same inputs **and the same owner decisions** — the AI's own first-run readings are not carried |
