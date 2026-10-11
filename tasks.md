# Tasks — TangramPlaytime

**Status:** Active  ·  **Last updated:** 2026-10-11

> Each task links back to the requirement(s) it implements via REQ IDs and
> the acceptance IDs it covers, so the traceability chain
> `REQ → task → code → test` stays intact. Do not invent tasks for work no
> requirement asked for (no silent scope). Columns and rules:
> `C:/GitHub/AI/SWDev/framework/templates/tasks.md` — **Inputs** (files
> outside the module its tests read) and **Done-check** (the test that must
> be green at that step) are mandatory; a **Status** cell is one word plus
> a link to the WO log, edited with the Edit tool only.

---

## WO-001 — #Locking: the one lock rule in :kernel behind ILockEngine  (Mode: Full (b))

Two lanes. The test lane is TASK-003 [P] (Acceptance Test Author). The implementer lane is TASK-001 → TASK-002, hardest first. Rows are in landing order. The kit lands in two stages: stage 1 beside TASK-001, and stage 2 once TASK-001 is done, because Gradle compiles the whole `kernel/src/test` set at once. Briefs: `workorders/WO-001-briefs.md`. Verification channels (G3): JVM `:kernel:test` and `:contracts:test` only; no device channel.

| Task | [P] | Description | Implements | Covers | Depends on | Inputs | Done-check | Status |
|---|---|---|---|---|---|---|---|---|
| TASK-003 | [P] | Acceptance kit (acceptance-test-author; judgment-heavy, Opus). Stage 1, beside TASK-001: `kernel/src/test/kotlin/app/tangramplaytime/kernel/KitFixtures.kt` and `kernel/src/test/kotlin/app/tangramplaytime/kernel/GeometryKitTest.kt` (hand-computed G-04 pins of the TASK-001 seams, no REQ token). Stage 2, once TASK-001 is done, beside TASK-002: `kernel/src/test/kotlin/app/tangramplaytime/kernel/LockEngineKitTest.kt`. Fixtures are transcribed; the kit reads no file | REQ-019, REQ-020, REQ-022 | REQ-019.A1, REQ-019.A2, REQ-019.A3, REQ-019.A4, REQ-020.A1, REQ-022.A1 (REQ-020.A2 → WO-003, log 12:48) | — | — | `kernel/src/test/kotlin/app/tangramplaytime/kernel/GeometryKitTest.kt` compiles once TASK-001 lands; `kernel/src/test/kotlin/app/tangramplaytime/kernel/LockEngineKitTest.kt` compiles once TASK-002 lands, with the tokens REQ-019.A1–A4, REQ-020.A1 and REQ-022.A1, each on its asserting test | done |
| TASK-001 |  | Geometry and validity helpers (slice-implementer; judgment-heavy geometry, Opus): `internal` objects `PieceShapes`, `Polygons`, `Silhouette` (contiguity corner rule, review F1) and `LockRules`, in new `PieceShapes.kt`, `Polygons.kt`, `Silhouette.kt`, `LockRules.kt` under `kernel/src/main/kotlin/app/tangramplaytime/kernel/` | REQ-019 (+ TYPE-001, TYPE-003, TYPE-004) | REQ-019.A2, REQ-019.A3 (groundwork: the validity predicate and the corner rule; proven at TASK-002) | — | — | `kernel/src/test/kotlin/app/tangramplaytime/kernel/GeometryKitTest.kt` green (G-04 pins; no REQ ID is green at this step) and `kernel/src/test/kotlin/app/tangramplaytime/kernel/KernelScaffoldTest.kt` green | done |
| TASK-002 |  | `LockEngine` (slice-implementer; judgment-heavy search and exact-anchor arithmetic, Opus): new `kernel/src/main/kotlin/app/tangramplaytime/kernel/LockEngine.kt`, which is `class LockEngine : ILockEngine` with all 7 members, `anchors` and the design §1 search. Deletes the `kernel/src/main/kotlin/app/tangramplaytime/kernel/Kernel.kt` placeholder and keeps `KernelScaffoldTest` | REQ-019, REQ-020, REQ-022 (+ TYPE-001, TYPE-003, TYPE-004) | REQ-019.A1, REQ-019.A2, REQ-019.A3, REQ-019.A4, REQ-020.A1, REQ-022.A1 | TASK-001 | — | `kernel/src/test/kotlin/app/tangramplaytime/kernel/LockEngineKitTest.kt` REQ-019.A1–A4, REQ-020.A1, REQ-022.A1 green, plus `kernel/src/test/kotlin/app/tangramplaytime/kernel/GeometryKitTest.kt` and `kernel/src/test/kotlin/app/tangramplaytime/kernel/KernelScaffoldTest.kt` green; `:contracts:test` green | implemented (unverified) → WO-001 log 2026-10-11 (tests 14/14 green; REQ-019.A1–A4 await the TYPE-004 signature, `capture-outbox/chg-draft-lock-type-dictionary.md`) |

**Statuses:** todo → in progress → in review → done · blocked (→ log) · implemented (unverified). A cell other than `todo`/`done` is the word plus `→ WO-001 log YYYY-MM-DD`.

## Change log

| Version | Date | Change | Reason |
|---|---|---|---|
| 0.1 | YYYY-MM-DD | placeholder | — |
| 0.2 | 2026-10-11 | WO-001 decomposed into TASK-001..003, replacing the placeholder section; Status → Active | P3 (wo-planner) |
| 0.3 | 2026-10-11 | WO-001 closed: TASK-001, TASK-003 done; TASK-002 implemented (unverified) for the pending TYPE-004 dictionary CHG; REQ-020.A2 moved to WO-003 | WO-001 close (orchestrator) |
