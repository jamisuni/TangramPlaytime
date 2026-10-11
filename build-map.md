# Build map — TangramPlaytime

**Status:** Signed off (G2)  ·  **Version:** 1.0  ·  **Last updated:** 2026-10-11
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
| #Solving | `play/` | Planned | #Content (via `IPuzzleLibrary`), (kernel) (via `ILockEngine`), #ResumePuzzle (via `IProgressStore`) | board, tray, size marks, drag, tap/twist/flip gestures, solved picture; the whole subtree inherits unless listed |
| #Locking | `kernel/` | Planned | — | REQ-019, 020, 022 are JVM-tested here (geometry); the UI half of go-home and solve is #Solving's |
| #LandingPreview | `play/` | Planned | (kernel) (via `ILockEngine`) | the dashed outline is UI; the search is the kernel's |
| #CornerTeach | `play/` | Planned | (kernel) (via `ILockEngine` "went home") | REQ-051 pulse |
| #Browsing | `play/` | Planned | #Content (via `IPuzzleLibrary`), #ResumePuzzle (via `IProgressStore`) | ‹ › top bar, resume/restart, solved view, long press, grid overview |
| #PlayTime | `kernel/` | Planned | #ResumePuzzle (via `IProgressStore`) | TYPE-005 active-second accounting, today/all-time, puzzle and best time (REQ-029, 030) |
| #TimerDisplay | `play/` | Planned | — | REQ-031 timer on the board |
| #Settings | `app/` | Planned | #PlayTime, #ResumePuzzle (via `IProgressStore`) | settings screen, sound toggle + playback + haptic, reset with confirmation |
| #Language | `app/` | Planned | #Content (via `IPuzzleLibrary` titles) | `values/` + `values-fi/`; English Contract, Finnish reference (pending CHG) |
| #Layout | `app/` | Planned | — | TYPE-007 window class, phone/tablet layouts, insets, back button, touch targets |
| #Content | `content/` | Planned | — | `Tangrams/*.json` as assets, parser, load-time validity, ordering; `IPuzzleLibrary` implementation |
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
