# Build map — TangramPlaytime

**Status:** Signed off (G2)  ·  **Version:** 1.3  ·  **Last updated:** 2026-10-12
**Approved by:** the orchestrator under governance row 4 (`ai+inform`), with `architecture.md` v1.0  ·  **Approved on:** 2026-10-11 (owner informed the same day)
**Feature tree:** `Requirements/features.md` (SwReqCollector collection v1.2, locked 2026-10-01; G1 accepted for this project 2026-10-11)

> Code homes and cross-feature dependencies for the locked feature tree.
> Features not listed inherit their nearest listed ancestor's code home.
> `—` = system-level (no slice). Mapping rules: `C:/GitHub/AI/SWDev/framework/traceability/traceability-rules.md` §5.
> Code homes are Gradle modules (`<module>/src/main`, `src/test`, `src/androidTest`; the DEV aid in `play/src/debug`). Build status is pipeline-written (this file is `contract_paths.notify`).

---

## 1. Code homes

| #Tag | Code home | Build status | Cross-feature deps (via I*) | Notes |
|---|---|---|---|---|
| (kernel) | `kernel/` | Planned | — | TYPE-001/003/004/005/006/007 rules live here once (D1); `ILockEngine` implementation; pure JVM |
| #Promise | — | Planned | — | system-level: manifest has no permission, no ad/billing/network dependency (release verifier); REQ-001.A2 / REQ-048 = owner evidence |
| #NoMoney | `app/` | Planned | — | REQ-009 free note on the settings screen; REQ-008 negatives verified system-level by the same verifier |
| #DeviceOnly | — | Planned | — | REQ-010: airplane-mode run + manifest scan |
| #Solving | `play/` | Built (WO-003; REQ-002 A2 → WO-009) | #Content (via `IPuzzleLibrary`), (kernel) (via `ILockEngine`), #ResumePuzzle (via `IProgressStore`) | board, tray, size marks, drag, tap/twist/flip gestures, solved picture; the whole subtree inherits unless listed |
| #Locking | `kernel/` | Built (WO-001) | — | REQ-019 is JVM-tested here (geometry); REQ-020's screen half is the `#GoHome` row below |
| #GoHome | `play/` | Built (WO-003) | (kernel) (via `ILockEngine` `LockResult.None`) | REQ-020: A1 proven at the seam in `kernel/` (WO-001) **and** on the screen (WO-003); A2 (no error message) only on the screen. Row added 2026-10-11 so the screen tests satisfy trace-check's one-code-home rule (decisions.md WO-003) |
| #PuzzleArt | `play/` | Built (WO-003: A1; A2 → WO-009) | #Content (via `IPuzzleLibrary` `art`) | REQ-039: A1 (nothing paints outside the outline) is the solved-picture drawing's (WO-003); A2 (release marks) is WO-009's and its test must live under `play/` (test-only dependency on `:content`). Row added 2026-10-11 (decisions.md WO-003) |
| #PuzzleMini | `play/` | Built (WO-003) | #Content (via `IPuzzleLibrary`) | REQ-045: A1 proven at the seam in `content/` (WO-002) **and** on the screen (WO-003: a fresh start opens on a three-piece mini); A2 (the picture after three pieces) only on the screen. Row added 2026-10-11 (decisions.md WO-003) |
| #SolvedCheck | `kernel/` | Built (WO-001) | — | REQ-022: `ILockEngine.isSolved`, JVM-tested here (architecture.md §3: the solved check is `:kernel`'s); the solved *picture* and view are #SolvedPicture / #SolvedView (`play/`). Row added 2026-10-11 — without it the tag inherited `play/` from #Solving and trace-check read the kernel test as directory theater |
| #LandingPreview | `play/` | Built (WO-003) | (kernel) (via `ILockEngine`) | the dashed outline is UI; the search is the kernel's |
| #CornerTeach | `play/` | Built (WO-003) | (kernel) (via `ILockEngine` "went home") | REQ-051 pulse |
| #Browsing | `play/` | Planned | #Content (via `IPuzzleLibrary`), #ResumePuzzle (via `IProgressStore`) | ‹ › top bar, resume/restart, solved view, long press, grid overview |
| #PlayTime | `kernel/` | Planned | #ResumePuzzle (via `IProgressStore`) | TYPE-005 active-second accounting, today/all-time, puzzle and best time (REQ-029, 030) |
| #TimerDisplay | `play/` | Planned | — | REQ-031 timer on the board |
| #Settings | `app/` | Planned | #PlayTime, #ResumePuzzle (via `IProgressStore`) | settings screen, sound toggle + playback + haptic, reset with confirmation |
| #Language | `app/` | Planned | #Content (via `IPuzzleLibrary` titles) | `values/` + `values-fi/`; English Contract, Finnish reference (pending CHG) |
| #Layout | `app/` | Planned | — | TYPE-007 window class, phone/tablet layouts, insets, back button, touch targets |
| #Content | `content/` | Built (WO-002) | (kernel) (via `ILockEngine`: piece congruence through `vertices`, overlap through `stillValid` — the engine is injected, `:content` main code depends on `:contracts` only) | `Tangrams/*.json` bundled as Java resources at build (WO-002 design), parser, load-time validity, ordering; `IPuzzleLibrary` implementation |
| #Release | — | Planned | — | store listing, data safety, rating, icon (DI-9 in `app/src/main/res`), release verifier; owner-evidence rows |
| #DevTools | `play/` (`src/debug`) | Planned | (kernel) (via `ILockEngine` solve) | REQ-046 debug-only; absent from release (G-08) |

Not build units (no row): #Difficulty, #DifficultyChoice, #DifficultyRules, #TurnButtons (withdrawn REQs only); #PieceSets, #SafetyRope, #Accessibility (`idea`).

The persistence component `store/` (`IProgressStore` implementation) has no `#Tag` of its own: it is the code home for the *storage half* of #ResumePuzzle, #PlayTime and #Settings and is built in WO-004 (Full, reason c — a new persistence format).

## 2. Work-order order (owner's kickoff order, KICKOFF.md; `priority: none`)

WO-001 #Locking (+ `:contracts`, `:kernel`) → WO-002 #Content → WO-003 #Solving → WO-004 #Browsing + `store/` → WO-005 #DevTools → WO-006 #Layout + #Language + #Promise → WO-007 #Settings → WO-008 #PlayTime → WO-009 #Release.

## 3. Change log

| Version | Date | Change | Reason | Approved by |
|---|---|---|---|---|
| 0.9 | 2026-10-11 | initial map from the locked tree: six modules, kernel row, system-level rows, WO order | P2 | — |
| 1.0 | 2026-10-11 | signed with architecture.md v1.0; the six modules exist and compile (`.swdev/run-log/P2-skeleton.md`) | G2 | orchestrator (row 4 `ai+inform`) |
| 1.1 | 2026-10-11 | `#SolvedCheck` row (code home `kernel/`); `#Locking` and `#SolvedCheck` build status Built (WO-001); REQ-020.A2 noted as WO-003's | WO-001: the tag had inherited `play/`; the component map already placed the solved check in `:kernel` | orchestrator (notify-tier delta, WO-001 Contract deltas) |
| 1.2 | 2026-10-11 | `#Content` cross-feature deps `—` → `(kernel) (via ILockEngine)`; build status In progress (WO-002); notes updated (Java resources, not Android assets) | WO-002 design (reviewed): piece geometry comes through the seam instead of a second TYPE-001 shape table in `:content` (D1 value rules once) | orchestrator (notify-tier delta, WO-002 Contract deltas) |
| 1.3 | 2026-10-11 | Rows `#GoHome`, `#PuzzleArt`, `#PuzzleMini` with code home `play/` (they inherited `kernel/` / `content/`); `#Solving` → In progress (WO-003); `#Locking` note narrowed | WO-003 scope: the carried screen-side lines (REQ-020.A2, REQ-039.A1, REQ-045.A2) are proven only in `play/`, and trace-check allows one code home per tag; the seam-side A1 tests stay as extra coverage (decisions.md 2026-10-11, WO-003). At the WO-003 close (2026-10-12): build status Built (WO-003) for `#Solving` (REQ-002 A2 → WO-009), `#GoHome`, `#PuzzleArt` (A1; A2 → WO-009), `#PuzzleMini`, `#LandingPreview`, `#CornerTeach` — same row | orchestrator (notify-tier delta, WO-003 Contract deltas) |
