# AGENTS.md: rules for AI agents in TangramPlaytime

Start every session by reading this file, then `KICKOFF.md` (until WO-001 closes) and `progress.md`'s last entry. Rules 1–13 are the owner's (Jami, carried over verbatim from the first run's intake, 2026-10-01); the build section after them is SWDev's.

1. **Enjoy, it's absolutely free.** Never add ads, analytics, purchases, donation asks, network access or permissions. If a task seems to need one of these, stop and ask.
2. **Ideas are not requirements.** Jami's messages give ideas, often marked "maybe". Record each round of his messages in `Requirements/` (a verbatim `evidence/src-NNN-*.md`, a `sources.md` entry, then the affected REQs) and log decisions in `Spec/05-open-questions.md`. You may propose better ideas; say so and record them as ASSUMPTIONs.
3. **`Study/` is frozen.** Jami's rule: leave it untouched. It holds the round 0–2 research and design notes; read it for background, never edit it. New notes go to `Requirements/` (evidence and REQs) or `Spec/`.
4. **`C:\GitHub\AI\SwReqCollector` is read-only.** It is the requirements framework this project follows. Never write to it; friction with it goes into `Requirements/framework-notes.md`.
5. **Requirements are locked (2026-10-01, SRC-014).** Never edit a locked REQ silently: record new owner input as a source, then a defect (DEF-NNN) or a change delta `Requirements/changes/CHG-NNN.md` (SwReqCollector Phase D) for Jami to sign.
5b. **Requirements first.** Behaviour comes from `Requirements/` (SwReqCollector format v2, ai-led capture; work there under `Requirements/CLAUDE.md`: validate, regenerate views, never set `priority` or `signoff`). `Spec/01–03` explain the design in prose; `Spec/04-requirements.md` is superseded. Technology is parked until Jami opens that topic.
6. **Geometry is exact** in validation (`tools/tangram_geom.py`, a + b√2). Runtime locking follows `Study/06-snapping-by-anchors.md` and TYPE-004: the anchors are the outline corners and placed-piece corners only. V11 (the edge-first build order) is information for authors, not a gate: every valid tangram has one (see `Tangrams/README.md`), so never remove a puzzle "because of V11" again. Warm-ups must pass V12.
7. **Puzzles live in `Tangrams/`**, separate from the requirements. Every file has `kind` (mini / warmup / full) and both titles (`en`, `fi`). After touching them, always run the validator and the renderer, and look at the preview sheet. Never set `provenance.reviewedByHuman` to `true`.
8. **Generated files:** `Spec/ui/*`, `Tangrams/previews/*`, `Spec/prototype/tangram-prototype.html` and `Requirements/views/*` are outputs. Edit the generators in `tools/` (the prototype source is `tools/prototype_template.html`).
9. **Prototype ≠ spec.** When they disagree, the requirements win; fix the template and rebuild.
10. **Test before calling it done:** `python tools/validate_puzzles.py Tangrams`, `python tools/build_prototype.py`, `python tools/tests/test_prototype.py` (all must pass), and the SwReqCollector `validate.py` on `Requirements/` (0 errors).
11. **The DEV solution reveal (REQ-046) is a testing aid.** Keep it in prototypes and test builds; a release build must not contain it.
12. **No difficulty levels, no tray turn buttons.** Both were removed by Jami in round 6 (REQ-004, REQ-044 withdrawn). Do not bring them back as "helpful" additions; the puzzle rating is the difficulty and a tap turns a piece.
13. **Every text in two languages.** The prototype and the game follow the device language (REQ-047, `fi` and `en`). A new string, a new puzzle title, a new note: both languages, always.
    *Amended by the owner 2026-10-11 (G1 surface, verbatim in `capture-outbox/src-draft-g1-owner-answers.md`): "lets go english only, and finnish is there just as reference to make sure language can be change but translations for fin are not important." English is the Contract wording; a Finnish string is still added for every text so the mechanism works, but its wording is a reference translation listed in `review-notes.md`, never tested. Pending `chg-draft-req047-english-contract.md`.*

---

## Build under SWDev (intake path C — SWDev v0.20, scaffolded 2026-10-08)

This project is **built** under the **SWDev framework**, located READ-ONLY at
`C:\GitHub\AI\SWDev` (rules 1–13 above stay in force and are
project-guardrail material for `architecture.md` §2). Read the framework
before acting; never edit it; never edit `C:\GitHub\AI\SwReqCollector`
either (rule 4). **This is the re-run of the first build** (TangramNoAds,
SWDev v0.15): same locked collection, same design inputs; do not read the
first run's code, designs, decisions or reviews — comparison comes after.

**Drift check (session start):** compare the version above against the
framework `README.md`'s current version. If they differ, tell the human once
and offer to re-sync the **adapter files only** (`.claude/settings.json`,
`.claude/agents/` from `project-template/`) — hook and permission fixes
should reach in-flight projects; methodology changes never retro-apply
mid-WO without the human choosing to re-baseline. *(Z02 harvest Z2-04: a
v0.10 project ran under a v0.12 framework and missed the bash auto-approver
and permission logging entirely — endless prompts, zero captured data.)*

## On every session start, read (in order)

1. `C:\GitHub\AI\SWDev\framework\00-principles.md`
2. `C:\GitHub\AI\SWDev\framework\methodology\lifecycle.md`
3. `C:\GitHub\AI\SWDev\framework\guardrails\directives.md`
4. `C:\GitHub\AI\SWDev\framework\methodology\gates-and-autonomy.md`
5. `C:\GitHub\AI\SWDev\initnewproject.md` §2b *(intake path C — how this
   project entered; items 8–11: the floor channel, the outbox, the G1 review
   format, generated inputs)*
6. `C:\GitHub\AI\SWDev\adapters\claude\CLAUDE.md` *(the Windows page)* and
   `platform-notes/android.md` *(this project's device duties)*
7. *(requirements-process.md / requirements-interview.md are the capture
   side's — not used: the collection is already locked)*
8. *(from P3 on:)* `C:\GitHub\AI\SWDev\framework\methodology\definitions.md`,
   `C:\GitHub\AI\SWDev\framework\methodology\orchestration.md`,
   `C:\GitHub\AI\SWDev\framework\methodology\task-decomposition.md`,
   `C:\GitHub\AI\SWDev\framework\agents\orchestrator.md`, and
   `C:\GitHub\AI\SWDev\framework\agents\handoff-contract.md`

Then read this project's: **`governance.md`** (who owns each control point
— human or AI; `proposed` = not in force), **`KICKOFF.md`** (until WO-001
closes), `Requirements/requirements.md` (index), `Requirements/features.md`
(#Tag tree), the `Requirements/reqs/REQ-NNN.md` files in scope,
`Requirements/views/digest.md`, `views/trace.md`, `views/lock-receipt.md`,
`build-map.md`, `architecture.md` (project root), `design-inputs.md`,
`decisions.md`, `review-notes.md`, `owner-evidence.md`, `progress.md`, and
`platform-notes/*.md` if present.
**From P3 on, one orchestrator context per work order** *(v0.17)*: a WO
kick-off opens a fresh context with a phase-scoped brief — the WO, its
`tasks.md` section, `decisions.md`, the last `progress.md` entry, the open
checkpoint items — and the WO close ends it. Do not resume a closed WO's
context for the next WO.
**Project layout (intake path C):** the spec is the locked
**SwReqCollector collection** `Requirements/` (`spec_dir` +
`spec_format: swreq-v2` in `.swdev/guard.json`), which stays under
SwReqCollector's own rules and is **never written to by the build side** —
a change is a `chg-draft` in `capture-outbox/`; the only build-launched
write inside it is the G1 review as `Requirements/reviews/review-NN.md`.
**Every locked REQ of every level (BIZ / FUN / UI) is Contract.** Withdrawn
items are retired; `idea` features are not build units; `priority: none`
is accepted — WO order is Jami's kickoff order. There is no FEAT-NNN: the
feature identity is the `#Tag`; code homes and cross-feature deps live in
**`build-map.md`**; the UI plans, the reference prototype and the puzzle
pipeline are registered in **`design-inputs.md`** (read its Contract-delta
first — Contract wins; a `generated` input supplies no Contract text).
Code lives one Gradle module per code home (`<module>/src/main`,
`<module>/src/test`, `<module>/src/androidTest`). `architecture.md`,
`build-map.md`, `design-inputs.md`, `tasks.md`, `progress.md`,
`proposals.md`, `decisions.md`, `review-notes.md`, `owner-evidence.md`,
`capture-outbox/`, `platform-notes/android.md` stay in the project root.
Rule 5 above is honoured through the outbox: the DEF/CHG is drafted here,
signed on the capture side.

## Non-negotiables (full text in framework/00-principles.md)

- Spec is the source of truth. Code derives from approved requirements.
  (Recovered-from-code REQs are provisional until human-signed — code is
  evidence, not authority.)
- **Requirements say what, never how** — at any level (BIZ / FUN / UI; BIZ
  stays UI-free). **Every locked level is Contract.** Design material is a
  design input (`design-inputs.md`), never a requirement.
- **The Contract** — locked REQs + the Governed Interface Registry in
  `architecture.md` — changes ONLY by common agreement, recorded.
- **Ownership is visible:** `ISomething` = governed contract (registry-listed,
  never changed silently); `iSomething` = AI-owned, freely refactorable.
- Guardrails and standing directives are constraints, never bent silently.
- Traceability is mandatory: REQ → design → task → code → test.
- No silent scope: build exactly the approved REQs — no more, no less. **Every
  non-trivial abstraction must name the REQ that requires it.**
- Quality bar is **senior-plus, not perfect**: clear, tested, maintainable —
  no micro-optimization or speculative hardening no REQ asked for.
- Requirements lock requires a **fresh-eyes review** by a separate session
  (`framework/agents/requirements-reviewer.md`); every Blocker finding
  resolved or explicitly accepted by the human first.
- The pipeline is autonomous between gates; recirculate freely inside the
  fountain, but anything breaching the Contract leaves the pipeline to a human.

## Hard-stops (pause and ask the human — unless `governance.md` assigns the row to the AI; hard-stop 1 is a floor)

- Security / auth / secrets / PII
- DB schema change / data migration / breaking API change
- Ambiguous requirement or guardrail conflict — **or any change to a locked
  REQ or a locked-tier governed `I*` interface**. *(Changing a **notify-tier**
  governed interface is allowed feature work: the guard logs a
  `contract-delta` event; summarize every delta in the WO's Contract-deltas
  section and present it at close/G4 — gates-and-autonomy.md, hard-stop 5.)*
- **A test or verifier that would need a product change** *(hard-stop 6,
  v0.18)*: never change the product to make a line provable — file a CHG on
  the capture side the same day, mark the line `implemented (unverified)`,
  carry on. The same when the cheapest honest oracle costs more than the
  feature (principle 15, the oracle-cost rule).

## Duties every session

- **Turn-ending signal (principle 16 — MANDATORY, every single reply):** end
  every turn with exactly one of these as the final line, verbatim:
  - `All ready, whats next Jami?` — work complete, nothing blocked, idle.
  - `Need help, answer above question JAMI` — blocked on a human decision;
    the one exact question sits immediately above this line.
  Never neither, never both — the human must see at a glance whether the
  session is waiting or done.
- **`progress.md`**: append the human's input VERBATIM, then a ~10-line summary
  of what was done. One entry per interaction.
- **The collection's views** (gen-digest steps aside here): after any
  capture-side change run the collection's own
  `python C:/GitHub/AI/SwReqCollector/tools/validate.py Requirements` (0
  errors) and `python C:/GitHub/AI/SwReqCollector/tools/views.py Requirements`;
  `views/digest.md`, `views/trace.md` and `views/lock-receipt.md` are the
  surface. Locked REQs change only through capture-side `CHG` deltas — drafted
  here in `capture-outbox/`, never by a build-side edit.
- **Traceability**: tests reference acceptance as `REQ-NNN.A<n>` tokens
  (`framework/traceability/traceability-rules.md`). Verify with
  `python C:/GitHub/AI/SWDev/framework/skills/trace-check/trace_check.py --project . --wo WO-NNN`
  — required green **for the WO** at every work-order close (the
  project-wide run is a note). An ID only a human can prove gets an
  `owner-evidence.md` row; the owner signs it.
- **The four scripts of a work order** *(v0.19 — run them, never
  hand-simulate them)*: at P3 `plan_check.py --wo` (GREEN before G3, again
  when the kit compiles); at close `trace_check.py --wo`,
  `verifiers/run_verifiers.py --wo` (every verifier + positive control, the
  budget line), and `wo-metrics/wo_metrics.py --wo --write` (the mechanical
  Metrics rows; you fill only disputes, hard-stop verdicts, lessons). All
  under `C:/GitHub/AI/SWDev/framework/skills/`.
- **`proposals.md`**: if the framework feels wrong or missing, append a note
  here. Never edit the framework folder.
- **`capture-outbox/` — the only road to the Contract** *(v0.20)*: the
  build never writes into the collection (`changes/` is locked too). A
  change a locked item needs — an acceptance line unprovable at an honest
  cost, two locked items in conflict, a locked rule the store or platform
  forbids — is a `chg-draft-<slug>.md` in `capture-outbox/`; a verbatim
  owner statement that decided something is a `src-draft-<slug>.md`. A
  capture session ingests them. At every WO close the G4 surface shows
  trace-check's **lock behind build** line (pending deltas on this WO's
  REQs, drafts not yet ingested); the dependent lines stay `implemented
  (unverified)` until the owner signs over there. A `generated` design
  input never supplies Contract text.
- **Governance:** a control point `governance.md` assigns to `ai` is
  exercised, not asked — and every such decision is appended to
  `decisions.md` (what, why, how to reverse); `ai+inform` rows also go on
  the next owner-checkpoint surface. Floors (lock, security/PII/money, VC,
  the profile) are never the AI's. Never flip a row yourself.
- **Stations are subagents (P3 on):** dispatch every fountain station as its
  named subagent from `.claude/agents/` — `wo-planner` plans (never the
  main thread), every station runs on **Sonnet or better**, design-reviewer
  and requirements-reviewer inherit the strongest; raise a station for UI,
  rendering, concurrency or device-only work, never lower one *(v0.17)*.
  Inline station work in the main thread is a justified, WO-logged
  exception. **This project's staffing (owner, 2026-10-11):** the main
  session (orchestrator) runs on **Claude Fable** and only manages — it
  never implements, designs, reviews or writes tests itself; the heavy
  stations `slice-implementer`, `design-author`, `acceptance-test-author`
  and `wo-planner` run on **Opus** (`model: opus` in their agent files —
  keep that override when re-syncing the pack); `design-reviewer` and
  `requirements-reviewer` inherit Fable; the mechanical stations
  (`plan-reviewer`, `code-reviewer`, `test-verify`, `traceability-auditor`)
  stay on Sonnet. Telemetry logs the model that actually ran.
  **Light mode is the default**; Full needs one of the four
  reasons written in the WO. One acceptance kit, compiled against the tree;
  Test & Verify starts from your run log (command · tree hashes · result
  XML — keep it). Owner questions are **checkpoint items**: at most five per
  WO, product-visible only, asked *before* the WO that depends on them, each
  written as what it is · what happened · proposal · what you give up ·
  recommended answer, each with a default that stands after 3 days and is
  logged as defaulted. Strings, translations and wording to proof-read go
  to **`review-notes.md`**, never to a checkpoint or the chat. Two locked
  items in conflict → a CHG the same day, never an interim rule
  (`framework/methodology/gates-and-autonomy.md` §8, `templates/work-order.md`).
- **Logging** (`framework/feedback/logging-spec.md`): at every WO close the
  Orchestrator fills the WO **Metrics block**; auditions and escalation
  rulings get one-line entries in `progress.md`. Hooks log mechanically to
  `.swdev/log/` — leave those files alone.
- **Version control is read-only for agents** *(v0.16, floor F3)*: `git
  status`, `diff`, `log`, `show` are yours to know what changed; never
  `git init`, `add`, `commit`, `tag`, `push`, `reset`, `checkout` or any
  other write to history — the owner commits by hand at WO closes (the
  close prompt names the state). The settings and the bash approver deny
  the writes; do not keep hand-rolled hash tables instead.
- **Ledgers through the Edit tool, paths with forward slashes** *(v0.16)*:
  `tasks.md`, `progress.md`, `decisions.md`, `workorders/*`, `designs/*`,
  `reviews/*` and every other markdown file are edited with the Edit/Write
  tools, never by `>>`, `tee`, `sed -i` or an inline script — the bash
  approver denies those (D1). Windows paths are written `C:/x` or `/c/x`;
  a backslash path outside quotes or inside an inline script is denied
  (D3). `adapters/claude/CLAUDE.md` has the Windows page.

## Current phase

`P3–P6 — work orders (G1 accepted 2026-10-11 by Jami; G2 locked 2026-10-11 under row 4 ai+inform, architecture.md v1.0). WO-001 #Locking closed 2026-10-11 (G4 row 9; :kernel lock engine behind ILockEngine, 6/6 IDs; REQ-019 lines unverified only for the unsigned TYPE-004 dictionary). WO-002 #Content closed 2026-10-11 15:45 (G4 row 9; :content puzzle library behind IPuzzleLibrary, 10/10 IDs, 13 puzzles bundled as Java resources; REQ-038.A1 unverified only for the pending validity-rules CHG; checkpoint item 1 — first-release puzzle count, default 25, stands 2026-10-14). WO-003 #Solving closed 2026-10-12 20:43 (G4 row 9; Light; the Play screen in :play wired by :app, 32/32 IDs on JVM + dev image + floor image, release verifier V-01 GREEN; REQ-002.A1 / REQ-014.A1 unverified only for their pending drafts; orchestrator moved Fable → Opus mid-P5 for credits; checkpoint item 1 — the board's colours, default DI-2 — stands 2026-10-14). WO-004 #Browsing + store/ closed 2026-10-17 09:25 (G4 row 9; Full (c); opened 2026-10-15, paused by the owner mid-P4, resumed 2026-10-17 in a fresh context; the top bar, Restart, the solved row, the grid and `:store`'s `FileProgressStore` behind `IProgressStore`, 16/16 IDs on JVM + dev image + floor image + a real process death; ADR-003 → `architecture.md` v1.1 via the G2 route; build-map v1.4; no line unverified; checkpoint items 1–5 answered by the owner 2026-10-17, every default confirmed; WO-008 carries the code review's S1). WO-005 #DevTools closed 2026-10-17 13:13 (G4 row 9; Light; the passcode-protected DEV aid of REQ-046 in `play/src/debug` behind one `src/main` door, `DevAidHook` via `ServiceLoader`; 4/4 IDs on JVM + dev image + floor image + the release APK test; no line unverified; checkpoint items 1–4 asked 2026-10-17, defaults stand 2026-10-20; the 1-in-5 spot-check WO — owner play-test recommended). WO-006 #Layout + #Language + #Promise closed 2026-10-18 19:10 (G4 row 9; Light; opened 2026-10-17, paused twice by the owner; TYPE-007 `LayoutClass` in :kernel, the safe-area geometry in :play, the window shell (phone portrait, tablet one-row tray, edge to edge, `adjustResize`) and `AppLanguage` in :app; 13/13 IDs on JVM + dev image + the new tablet image `Medium_Tablet_API_37.0` (now a standing channel) + floor image (root-route locale and airplane steps) + the release APK test; REQ-047.A1 unverified only for its English-Contract draft; build-map v1.6; 13 corrected checks; checkpoint items 1–3 stand 2026-10-20 / 2026-10-21). WO-010 content batch closed 2026-10-20 17:22 (G4 row 9; Light; opened 2026-10-18 20:00, paused three times by the owner; `architecture.md` v1.2 via the G2 route (G-16 GPL-3.0 puzzle figures credited, the O-06 DEV-aid line); 93 puzzles imported from `C:/GitHub/AI/TangramCollections` (convex-13 CC0 + android-tangram-131 GPL-3.0; 51 of 144 skipped with reasons), each with a drawn picture, 106 in all, credited in `ASSET-LICENCES.md`; the counter's `Locale.ROOT`; the board badge's area clamped onto the board (F1); 11/11 IDs on Python + JVM 263/0 + floor + dev + tablet + V-01; the scale record well inside R6; REQ-038.A1 unverified only for its validity-rules draft; 17 corrected checks; checkpoint items 3–4 stand 2026-10-21). **The floor move to Android 12 (API 31) done 2026-10-20** (the owner's decision 2026-10-19 21:47): `architecture.md` v1.3 via the G2 route (`minSdk 31`, floor channel `Phone_API_31`), the image installed and proven (`.swdev/run-log/floor-API31.md`), `platform-notes/android.md` updated. WO-008 #PlayTime closed 2026-10-21 08:53 (G4 row 9; Full (b); opened 2026-10-20 20:16, ahead of WO-007 by row 8; `minSdk 31` in every module; `ActiveTime` in :kernel behind the new governed `IActiveTime` (`architecture.md` v1.4, notify), the puzzle and best times in `PlayController`, the hidden-by-default board timer, `AppClock` + the ticker in :app; 9/9 IDs on Python + JVM 295/0 + floor `Phone_API_31` + dev + tablet + the death pair + V-01; build-map v1.8; no line unverified; 4 corrected checks; checkpoint items 1–2 stand 2026-10-23; REQ-029.A2 and the code review's N1 carried to WO-007). WO-007 #Settings closed 2026-10-22 02:49 (G4 row 9; Full (b); opened 2026-10-21 15:15; the gear and the settings screen in :app (one card in the activity's window: the timer and sound switches, live play times, best times, Reset with an in-place question, how to play, the free note, the privacy text, the licence line), `IActiveTime.resetPlayTimes` (`architecture.md` v1.5, notify), five generated tones + the lock's tick behind the one `Feedback` door; 16/16 IDs on Python + JVM 320/0 + floor `Phone_API_31` + dev + tablet + the death pair + V-01; build-map v1.10; REQ-047.A1 unverified only for its English-Contract draft; 9 corrected checks; 1 product defect found by review (N4) and fixed; checkpoint items 1–4 stand 2026-10-24). **Next: WO-009 #Release** (KICKOFF "State after WO-007")` — scaffolded 2026-10-08 under SWDev v0.20; the TangramNoAds re-run (T-28). Update this line as phases advance.
