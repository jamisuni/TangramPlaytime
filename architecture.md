# Architecture — TangramPlaytime

**Status:** Locked (G2)  ·  **Version:** 1.0  ·  **Last updated:** 2026-10-11
**Approved by:** the orchestrator under governance row 4 (`ai+inform`), after the toolchain proof (6 JVM tests green, `:app:assembleDebug`) and the floor-channel proof (instrumentation test green on `Phone_API_26`, Android 8.0.0) — `.swdev/run-log/P2-skeleton.md`  ·  **Approved on:** 2026-10-11  ·  **Owner informed:** 2026-10-11 (G2 surface; default stands 2026-10-14; the owner may reopen any line by flipping row 4 or filing a change)
**Baseline:** frozen with `trace_check.py --freeze` on 2026-10-11 (`.swdev/contract-baseline.json`).

> Together with the locked REQs (`Requirements/` collection v1.2, G1 accepted
> 2026-10-11) this document is **the Contract**. Guardrails and governed
> interfaces change only by common agreement, recorded below. Authoring
> guide: `C:/GitHub/AI/SWDev/framework/guardrails/guardrail-authoring.md`;
> standing directives: `C:/GitHub/AI/SWDev/framework/guardrails/directives.md`.

---

## 1. Standing directives — acknowledgment

D1–D6 apply in full. No deviations.

| Directive | Deviation | Reason | Approved by / on |
|---|---|---|---|
| none | — | — | — |

D1 note: the slice names are the collection's `#Tag` subtrees as mapped in `build-map.md`; one Gradle module per code home. TYPE-001/003/004/005/006/007 rules are implemented **once**, in `:kernel` (D1 duplication exception for value rules).

## 2. Project guardrails

Owner rules 1–13 of `AGENTS.md` (DI-6) made checkable. A reviewer must be able to point at a diff and say "violates G-NN".

- **G-01 — Free and offline, mechanically.** The merged manifest declares **no** `<uses-permission>` (not even `INTERNET`); no dependency on an ad, billing, analytics, crash-reporting or network library; no `java.net` / `okhttp` / `HttpURLConnection` use in product code. *(owner rule 1; REQ-001, 008, 010, 048.A3)* — verified at every WO close by the release verifier.
- **G-02 — The collection is read-only.** No build-side write under `Requirements/` except `Requirements/reviews/` and the generated `Requirements/views/` (via `views.py`). A change to a locked item is a `chg-draft` in `capture-outbox/`. *(owner rules 4, 5, 5b)*
- **G-03 — `Study/` is frozen.** Read only. *(owner rule 3)*
- **G-04 — One lock rule, in the kernel.** Anchor points are only the silhouette's outline corners and the corners of pieces currently on the board; lock distance, scoring and tolerance are exactly TYPE-004; the locked position is the anchor's exact position minus the piece corner's exact offset. Implemented once in `:kernel` behind `ILockEngine`; no UI code computes geometry. *(owner rule 6; REQ-019, 021, TYPE-004)*
- **G-05 — Puzzles live in `Tangrams/*.json` only**, format `tangram-puzzle/1` (DI-5). Every file has `kind`, an `en` title (and a `fi` title while the format requires it — pending `chg-draft-req047-english-contract.md`). After touching a puzzle run `python tools/validate_puzzles.py Tangrams` and `python tools/render_puzzle.py Tangrams Tangrams/previews` and look at the sheet. The AI never sets `provenance.reviewedByHuman: true`. *(owner rule 7; REQ-038–042, 045)*
- **G-06 — Generated files are edited through their generators** (`Spec/ui/*`, `Tangrams/previews/*`, `Spec/prototype/*`, `Requirements/views/*`). The prototype is a design input (DI-1): when it disagrees with a REQ, the REQ wins (`design-inputs.md` §2). *(owner rules 8, 9)*
- **G-07 — Tests before done.** `validate.py` on `Requirements/` (0 errors) and the puzzle validator stay green; the product's own gate is the acceptance kit on the channels named at G3. *(owner rule 10)*
- **G-08 — The DEV solution reveal (REQ-046) exists only in the `debug` build type** (its code lives in `play/src/debug/`); the release APK contains no DEV button, no passcode and no solution overlay. Verified by the release verifier (no debug tooling, canary present) at every WO close. *(owner rule 11)*
- **G-09 — No difficulty levels, no tray turn buttons.** Neither appears in code, settings or UI. *(owner rule 12; REQ-004, 027, 028, 044 withdrawn; REQ-032.A2)*
- **G-10 — Every user-visible string has an `en` and a `fi` resource.** English is the Contract wording; Finnish is a reference translation listed in `review-notes.md`, never asserted by a test. No hard-coded user-visible string in Kotlin. *(owner rule 13 as amended 2026-10-11; REQ-047; pending `chg-draft-req047-english-contract.md`)*
- **G-11 — Haptics only through the permission-free API** (`View.performHapticFeedback` / Compose `LocalHapticFeedback`); never `Vibrator` / `VibratorManager` (needs `VIBRATE`). *(REQ-010, REQ-033)*
- **G-12 — Seams.** Cross-module calls go only through the `I*` interfaces and contract types of `:contracts`. `:contracts` and `:kernel` are pure Kotlin/JVM modules (no `android.*` import). No module reaches into another module's internals. *(D1)*
- **G-13 — One store.** All persisted state (puzzle states, placed pieces, times, settings) goes through `IProgressStore`, implemented in `:store`; no other module touches files, DataStore or preferences. *(REQ-010, 025, 029, 030, 031, 033, 034)*
- **G-14 — Clock through the seam.** Time comes from the injected `Clock` of `:contracts`; product code never calls `System.currentTimeMillis()` / `SystemClock` outside `:app`'s one `Clock` implementation. Tests set the clock. *(TYPE-005; platform-notes/android.md)*
- **G-15 — Error model.** Expected outcomes cross governed seams as sealed results (`LockResult.None`, `LoadResult.Unreadable`), never as exceptions; exceptions mean a programming error. One decision, applied everywhere.
- **G-16 — Assets are self-made or licence-free**, bundled or system (no network fonts, no attribution-required packs); every shipped sound, font and image is listed with its licence in `ASSET-LICENCES.md`. *(REQ-010; pending `chg-draft-licence-notice-and-assets.md`)*
- **G-17 — Draw paths in pixels.** Board and tray paths are built in px after scaling, never drawn under a canvas scale (the API 26 floor blurs scaled paths). *(platform-notes/android.md; REQ-006 floor)*

## 3. Component map

| Component / module | Owns | Serves (features → REQs) |
|---|---|---|
| `:contracts` (Kotlin/JVM) | the governed `I*` interfaces and contract types: piece ids/types, turn, mirror, puzzle definition, puzzle state, placement, `Clock` | every module; the Contract's code half |
| `:kernel` (Kotlin/JVM) | TYPE-001 piece geometry, TYPE-003 turn/mirror, TYPE-004 lock search (`ILockEngine` impl), TYPE-005 active-second accounting, TYPE-006 state machine, TYPE-007 layout class, solved check | #Locking (REQ-019, 020, 022), #PlayTime (REQ-029, 030), #Turning rules (REQ-016, 017, 018 geometry), #SolvedCheck |
| `:content` (Android library) | the puzzle assets (`Tangrams/*.json` copied at build), the `tangram-puzzle/1` parser, load-time validity check, ordering by kind → rating → id (`IPuzzleLibrary` impl) | #Content (REQ-007, 038–042, 045), titles for REQ-047 |
| `:store` (Android library) | persistence of puzzle states, placed pieces, best/puzzle/play times, settings; reset; unreadable-state recovery (`IProgressStore` impl) | #ResumePuzzle, #PlayTime, #Settings, #ResetProgress (REQ-003, 025, 029, 030, 031, 033, 034) |
| `:play` (Android library, Compose) | the Play screen: board, silhouette, tray, drag/tap/twist/flip gestures, landing preview, go-home, corner pulse, solved picture, timer display, top bar ‹ › counter, solved view, grid overview; DEV aid in `src/debug` | #Solving, #Browsing, #TimerDisplay, #DevTools (REQ-002, 011–026, 031, 043, 046, 050, 051) |
| `:app` (Android application) | the shell: window size class → TYPE-007 layout, edge-to-edge insets, back handling, settings screen, sounds + haptic, language resources, `Clock` impl, wiring of the modules, launcher icon, manifest | #Layout, #Settings, #Language, #Promise note, #Release (REQ-006, 009, 032–037, 047–049) |
| *(system level)* | manifest, listing, release verifier, dependency scan | #Promise, #DeviceOnly, #Release (REQ-001, 008, 010, 048) |

### 3b. Component ownership rules

- **O-01** — Geometry (vertices, turn, mirror, inside/overlap tests, anchors, candidate search, solved check) lives in `:kernel` only. `:play` asks `ILockEngine`; it never re-derives a position.
- **O-02** — Puzzle parsing, validity checking and ordering live in `:content` only. Nothing else reads `Tangrams/*.json`.
- **O-03** — Everything that survives a restart is written and read by `:store` only, through `IProgressStore`.
- **O-04** — Active-second accounting (TYPE-005) and the TYPE-006 transitions live in `:kernel`; `:play` reports touches, visibility and overlay state, `:store` persists the totals.
- **O-05** — Layout class (TYPE-007), insets, orientation policy and the two tray-row splits are decided in `:app`; `:play` receives the layout, never the window size.
- **O-06** — All user-visible strings are `:app` resources (`values/`, `values-fi/`); `:play` and `:content` expose string *resource ids* or puzzle titles, never text literals.
- **O-07** — Sound and haptic playback live in `:app`; `:play` and `:kernel` emit events (`PickedUp`, `Turned`, `Locked`, `WentHome`, `Solved`).
- **O-08** — The DEV aid (REQ-046) lives in `play/src/debug/`; `src/main` has no reference to it.

## 4. Governed Interface Registry  *(normative)*

Marker convention (Kotlin): a governed contract is an `I`-prefixed interface or a contract type in module `:contracts`, package `app.tangramplaytime.contracts`, with the governed banner. AI-owned internals use ordinary Kotlin names in their own modules and never appear here. Tiers per governance row 6 (`ai`, default `notify`); the puzzle format is `locked` because a format change is a capture-side CHG (DI-5).

| Interface | Tier | Purpose (one line) | Between | Serves REQs | Defined in | Sign-off |
|---|---|---|---|---|---|---|
| `IPuzzleLibrary` + the `tangram-puzzle/1` format | **locked** | The ordered puzzle library: every puzzle's id, kind, rating, titles, solution placements and art, in ‹ › order (kind → rating → id); `puzzles`, `byId` | `:content` ↔ `:play`, `:kernel` | REQ-007, 011, 019 (anchors from the solution), 023, 026, 038, 039, 040, 041, 042, 045, 046, 047 (titles), 050 | `contracts/src/main/kotlin/app/tangramplaytime/contracts/IPuzzleLibrary.kt`; format: `Tangrams/puzzle.schema.json` + `Spec/03-puzzle-format.md` (DI-5) | orchestrator (row 4) / 2026-10-11 |
| `ILockEngine` | notify | The one lock rule (TYPE-004): `outlineCorners`, `vertices`, `findLock` (drop + landing preview), `tryTurnInPlace`, `tryMirrorInPlace`, `stillValid` (REQ-025 load check), `isSolved` | `:kernel` ↔ `:play`, `:store` | REQ-016, 017, 018, 019, 020, 021, 022, 025 (A3), 051 (the "went home" signal) | `contracts/src/main/kotlin/app/tangramplaytime/contracts/ILockEngine.kt` | orchestrator (row 4) / 2026-10-11 |
| `IProgressStore` | notify | Everything that survives a restart: per-puzzle state (TYPE-006) and board placements, puzzle/best/play times, settings (timer, sound), the last opened puzzle, `resetAll`; unreadable state = fresh install (`PuzzleProgress.FRESH`, `PlayTimes.NONE`) | `:store` ↔ `:play`, `:app`, `:kernel` | REQ-003, 025, 026, 029, 030, 031, 033, 034, 050 | `contracts/src/main/kotlin/app/tangramplaytime/contracts/IProgressStore.kt` | orchestrator (row 4) / 2026-10-11 |
| contract types (`PieceType`, `PieceId`, `Turn`, `Point`, `Placement`, `PuzzleKind`, `SolvedPiece`, `ArtStyle`, `ArtShape`, `ArtLayer`, `PuzzleDefinition`, `PuzzleState`, `PuzzleProgress`, `PlayTimes`, `Settings`, `Clock`, `LockResult`) | notify | the immutable values that cross the seams; TYPE-001/003/006 as code | all | TYPE-001, 003, 006; REQ-012, 025, 029, 031, 033 | `contracts/src/main/kotlin/app/tangramplaytime/contracts/Types.kt` | orchestrator (row 4) / 2026-10-11 |

Mirrored into `.swdev/guard.json`: `contract_paths.locked` ← `IPuzzleLibrary.kt`, `Tangrams/puzzle.schema.json`; `contract_paths.notify` ← the other `contracts/**` files and `build-map.md`.

## 5. Technology & data constraints

- **Stack (owner decision, SRC-016 / DI-8, governance row 5):** Kotlin 2.4 + Jetpack Compose (BOM 2026.09), AGP 9.4, Gradle 9.8 wrapper, JDK 25 (Android Studio `jbr`), `compileSdk`/`targetSdk` = the installed platform (37), **`minSdk 26`** (Android 8.0, decisions.md 2026-10-11). The prototype's locking logic is **ported**, not re-invented (ADR-001).
- **Allowed dependencies:** AndroidX + Compose (BOM), `kotlinx.serialization` (puzzle JSON), `kotlinx.coroutines`; JUnit 4 / Compose UI test / AndroidX test for the kit. Anything else is a D5 record in the WO.
- **Data:** device-local only; no PII, no accounts, no network, no permissions (G-01). The persistence format is chosen in WO-004 (Full reason **c**, a new persistence format) and documented as ADR-003.
- **Application id:** `app.tangramplaytime` (decisions.md 2026-10-11; owner may rename before the first Play upload).
- **Build from Git Bash:** `cmd.exe //c ".\gradlew.bat <tasks>"` with `JAVA_HOME` set to the Android Studio `jbr` (platform-notes/android.md).
- **Verification channels:** JVM unit tests (`:kernel`, `:contracts`, `:content`, `:store`); Compose/device tests on the dev image (`Medium_Phone_API_37.0`) **and** the floor image (`Phone_API_26`), one emulator at a time; release verifier + positive control at every WO close.

## 6. Architecture Decision Records

| ADR | Title | Status | Links |
|---|---|---|---|
| ADR-001 | **Port the prototype's lock search** (`tools/prototype_template.html` `findLock`/`validAt`/`anchors`, Sutherland–Hodgman clipping, 1e-6 tolerance) to Kotlin in `:kernel`, following the **TYPE-004 text** where the prototype differs (candidate window on \|t\|, not on score). Floats with 1e-6 tolerance at runtime; exact a + b√2 arithmetic stays in the Python content tools. | Accepted (owner kickoff) | REQ-019, 021, TYPE-004; DI-1, DI-4; G-04 |
| ADR-002 | **One Gradle module per code home**: `:contracts`, `:kernel`, `:content`, `:store`, `:play`, `:app`; `:contracts` and `:kernel` pure JVM so the lock rule and time rules are tested in milliseconds without a device. | Accepted | D1; build-map.md |
| ADR-003 | Persistence format for `IProgressStore` | Proposed — decided in WO-004 (Full, reason c) | REQ-025, 029, 030, 034 |
| ADR-004 | **Puzzles ship as JSON assets** in `:content`, parsed with `kotlinx.serialization`; a JVM test loads every asset and re-checks validity (piece set exactly once / 1–6 for mini, no overlap, edge-connected, picture well-formed) — the build-side oracle for REQ-038.A1 while the Python validator remains the authoring tool. | Accepted | REQ-038, 041, 045; DI-5; pending `chg-draft-req038-validity-rules-in-contract.md` |
| ADR-005 | **System font only** for v1 (no network font; a bundled licence-free display font would be a D5 record in the WO that adds it). | Accepted | REQ-010, G-16; supersedes DI-2 "Baloo 2" |
| ADR-006 | **Injected `Clock`** in `:contracts`; `:app` provides the real one; tests set it (platform-notes: time-driven tests advance the clock on the main thread, never sleep). | Accepted | TYPE-005; G-14 |

## 7. Change log

| Version | Date | Change | Reason | Approved by |
|---|---|---|---|---|
| 0.9 | 2026-10-11 | P2 draft: directives ack, G-01..G-17 from the owner's rules 1–13 + the device lessons, component map, O-01..O-08, registry (3 interfaces + contract types, tiers), tech constraints, ADR-001..006 | G1 accepted the same day; stack decided by the owner (SRC-016) | — |
| 1.0 | 2026-10-11 | **G2 lock.** Registry rows Defined in + signed; the implementer's shape refinements accepted (decisions.md); the androidx.core signature permission removed from the merged manifest (decisions.md row 13); tray turns not persisted (F36); baseline frozen | toolchain + floor proofs green (`.swdev/run-log/P2-skeleton.md`) | orchestrator under governance row 4 `ai+inform`; owner informed 2026-10-11 |
