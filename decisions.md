# Decisions the AI took — TangramPlaytime

Append-only, AI-written. Every control point that `governance.md` assigns
to `ai` or `ai+inform` is exercised here, one row per decision — the
owner's review sheet, the build-side twin of SwReqCollector's
`views/trace.md` "decisions the agent took instead of asking". The owner
overrides by editing the artifact, filing a capture-side `DEF`/`CHG`, or
tightening the row in `governance.md`. Never edit a past row; add a new one.

**Owner-grade?** = a product or UX call a human owner would normally make
(what the user sees, hears, keeps, or pays). `yes` rows are the ones that
belong on a checkpoint (at most five per WO, each with a default); the
**Asked on / default** cell records the date it was put to the owner and
the default that stood if unanswered after 3 days — or `—` for a `no` row.
A `yes` row never hides inside a design doc or a plan: it is this row, and
the design cites it. *(v0.17: 59 product calls were made from inside
designs and ledgers; the owner saw four of them.)*

| Date | Control point (#) | WO / phase | Decision | Why (REQ / evidence) | How to reverse | Owner-grade? | Asked on / default | Surfaced at |
|---|---|---|---|---|---|---|---|---|
| YYYY-MM-DD | 5 — technology | P2 | <stack chosen> | <constraints §2, REQs> | <re-run P2 from architecture.md v0.1> | no | — | checkpoint N |
| YYYY-MM-DD | 12 — UX detail | WO-00N | <platform click sound stays off> | <REQ-0NN silent on it; DI-2 says off> | <flip one flag in iSettings> | yes | YYYY-MM-DD / default: off | WO-00N checkpoint item 2 |
