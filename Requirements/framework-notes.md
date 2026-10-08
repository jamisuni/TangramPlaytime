# Framework notes — TangramNoAds

Findings about **SwReqCollector itself** while capturing this system: what
was awkward, what a rule got wrong, what the capture loop did not cover,
what a tool should have caught. This is the feedback channel from a real
project back to the framework. It is *not* about the system being described
— that goes in the items.

Framework in use: SwReqCollector format v2 (tools v0.4; tools v0.5 closed the notes below on 2026-10-07 — the collection's own lock stays v1 until re-signed) at `C:\GitHub\AI\SwReqCollector`.

Worked through in a SwReqCollector session; each note is closed there with
what changed (or why nothing did). Never edit the framework from here.

## Open

| Date | While doing | Finding | Proposal |
|---|---|---|---|
| 2026-09-27 | SRC-002 and most REQs | The owner said "I give ideas, not requirements", with some items marked "maybe". There is no standard way to record a tentative owner statement: it is either `stated` (too strong) or `inferred` (loses that the owner said it). The capture used a written convention (index §5). | Consider a documented convention, or a `stated-tentative` reading of `stated` that cannot lock until re-confirmed. |


## Closed

| Date | Finding | Outcome (framework change, or reason for none) |
|---|---|---|
| 2026-09-27 | **Retroactive ai-led capture.** §12 assumes PROPOSE → AI-APPROVE → BUILD in that order. Here the study, spec draft and prototype existed *before* the collection… | v0.5: `capture-loop.md` §12 *Capture after the first prototype* — register the existing proposal and build as one `generated` SRC, approve exactly what the build implements; a `firsthand` playtest SRC is what the UI REQs then need to lock. |
| 2026-09-27 | The proposal lives outside the collection (`../Spec/`, `../Tangrams/`, part of `../Study/`) because the owner wanted the study folder left untouched. `storage-l… | v0.5: `storage-layout.md` — a proposal SRC may live outside the collection folder; its Origin says where. |
| 2026-09-27 | ASSUMPTION lines inside TYPE entries (TYPE-001, 004, 005, 007) are not listed in `views/trace.md` → "Decisions the agent took instead of asking", only REQ assum… | v0.5: `views.py` lists TYPE assumptions under *Decisions the agent took instead of asking* and in `views/lock-receipt.md`; the lock gate v2 refuses a locked REQ on an `Inferred` or `draft` TYPE. |
| 2026-09-27 | `idea` leaves (#PieceSets, #Accessibility) warn "leaf feature with no REQs (drill-down pending)". An idea has no REQs by definition (`req-format.md` §2). | v0.5: `validate.py` skips the drill-down warning for `idea` and `withdrawn` leaves (the trace still lists ideas as backlog). |
| 2026-09-27 | `capture-loop.md` §4 says material about other products is stored "nowhere in the project". The project's `Study/` (written before the collection existed, at th… | v0.5: `storage-layout.md` *The scan rule and a project's older material* — §4 binds the collection folder; older study material in the repo is never a source, never cited, and the index notes it is frozen. |
| 2026-09-28 | **No feature status for a withdrawn leaf.** `features.md` statuses are idea / captured / specified / locked / ai-approved. A leaf whose only REQ is `withdrawn` … | v0.5: feature status `withdrawn` (`req-format.md` §2): all REQs withdrawn, accepted under any parent, no drill-down warning. |
| 2026-09-28 | **§12.3 asks were skipped without anyone noticing.** The ai-led loop says the agent must still ask about legal duties, money, systems the project does not contr… | v0.5: the index's *Asked before lock* table (`templates/requirements-index.md` §5, `capture-loop.md` §12.3); `validate.py` warns while a row is empty and refuses an ai-led lock without the table (lock gate v2). |
| 2026-09-28 | **Acceptance by reference.** The owner accepted an agent review "as a whole". The review is the agent's material (not a source), so the evidence file had to res… | v0.5: `capture-loop.md` §3 *Acceptance by reference* — register the acceptance as a `conversation` SRC whose evidence file lists every accepted proposal; the agent's detail choices stay ASSUMPTIONs; a later "lock everything" message is never the source of what it locks. |
