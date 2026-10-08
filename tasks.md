# Tasks — TangramPlaytime

**Status:** Placeholder — populated in **Phase P3 (Planning)** by the
`wo-planner` subagent, after a requirement is locked.

> Each task links back to the requirement(s) it implements via REQ IDs and
> the acceptance IDs it covers, so the traceability chain
> `REQ → task → code → test` stays intact. Do not invent tasks for work no
> requirement asked for (no silent scope). Columns and rules:
> `C:/GitHub/AI/SWDev/framework/templates/tasks.md` — **Inputs** (files
> outside the module its tests read) and **Done-check** (the test that must
> be green at that step) are mandatory; a **Status** cell is one word plus
> a link to the WO log, edited with the Edit tool only.

---

## WO-NNN — <work order name>  (Mode: Light | Full (a–d))

| Task | [P] | Description | Implements | Covers | Depends on | Inputs | Done-check | Status |
|---|---|---|---|---|---|---|---|---|
| TASK-001 |  | <what to do> | REQ-NNN | A1 | — | — | `tests/...` A1 | todo |

## Change log

| Version | Date | Change | Reason |
|---|---|---|---|
| 0.1 | YYYY-MM-DD | placeholder | — |
