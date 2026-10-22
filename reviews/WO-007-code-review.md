# Review — code · WO-007

**Date:** 2026-10-21 22:30  ·  **Reviewer:** fresh context (Sonnet)
**Inputs:** the working tree against `aeba492` (hashed by the orchestrator 22:13: `aeba492 · 56 · test 26a4ea6c1fc6 · product 006d54755e9f · bundle 02feaef5d941 (106) · tools 281af78fa5d7`): `IActiveTime.kt`, `ActiveTime.kt`, `Persistence.kt`, `PlayController.kt`, `HitTest.kt`, `PointerRouter.kt`, `PlayScreen.kt`, `TopBar.kt`, `PlayTags.kt`, `PlayTexts.kt`, `AppGraph.kt`, `MainActivity.kt`, both `strings.xml`, `ASSET-LICENCES.md`, `tools/make_sounds.py`, the untracked `SettingsScreen.kt`, `Feedback.kt`, `DeviceFeedback.kt` and the five `sfx_*.wav`; `FileProgressStore.resetAll`. The kit read as evidence: `SoundKitTest` in full, the rest by its run-log rows. Oracles: `workorders/WO-007.md`, `designs/WO-007-design.md` (§2, §3), `reviews/WO-007-design-review.md` with the F1 re-check, `decisions.md` rows 219–254, `architecture.md` v1.5, REQ-009/029/031/032/033/034/037/049, `platform-notes/android.md`, `.swdev/run-log/WO-007.md`. Libraries: ui-android 1.12.1 `ui.aar` from the Gradle cache, `javap` on `SemanticsNode`.

## Verdict

**forward** — the reset order (design §3's highest risk) holds line by line: no path writes the pre-reset board or times after the store's reset, and the settings survive. F1's door is whole today: no `Popup` or `Dialog` exists in any source set. The tree builds and the JVM line re-derives at 320/0. Nothing stands as a Blocker or a Should. Five Notes, none blocking; the contrast and the TalkBack notes are owner-grade look items for the G4 surface.
0 Blockers, 0 Shoulds, 5 Notes.

## Checklist applied

- [x] **Directives** — D3: `FeedbackOut`, `BestTime` and `SettingsTags` each name their REQ; D4: `timeOf` / `titleOf` public only (S-11Δ4); D5: no dependency added.
- [x] **Guardrails** — G-01 / V-01: no permission, manifest or build-file change (`git diff` shows neither); `SoundPool` and `performHapticFeedback` are the permission-free APIs, no `Vibrator`, no `MediaPlayer` (grep). G-12: `Feedback` and `DeviceFeedback` live in `:app`, `:play` only emits `PlayEvent`. G-13: `saveSettings` and `resetAll` go through `Persistence`'s one FIFO. G-14: no clock read in the new code. G-17: the gear and the second line are drawn in px. G-09 / REQ-032.A2: no difficulty control. G-10: 18 new strings in `values` and 18 in `values-fi`.
- [x] **Contract** — `IActiveTime.resetPlayTimes()` carries the KDoc of the design, the banner's Serves gains REQ-034, and the `architecture.md` v1.5 registry row names both. `IProgressStore` and `Types.kt` are consumed unchanged.
- [x] **Scope** — every changed product line traces to S-44Δ … S-53 and S-22Δ3. The licence line is plain text, untokened, as the scope row says.
- [x] **Hard-stops** — none touched; no progress-file field is added (format 1 already holds `settings`).
- [x] **Evidence re-derived** — see "Re-derived".
- [x] **The conceptual 20 %** — the orchestrator's seven items are answered below.

### The orchestrator's watch items

1. **F1 re-check (judged: the door is whole today; nothing guards the future; N3).** Grep of `play/src/main`, `play/src/debug`, `app/src/main`, `kernel/src/main` for `Popup`, `Dialog`, `combinedClickable`, `SoundEffectOnInteraction`: the only hits are the comments in `MainActivity.kt` and `SettingsScreen.kt` that forbid them. Every `clickable` / `toggleable` is composed inside `CompositionLocalProvider(LocalSoundEffect provides feedback)` (`MainActivity.kt:130`), including the `SettingsScreen` sibling. `SoundKitTest`'s positive control presses two controls (the settings Done and a grid cell), one in each subtree, so a provider that fell out of either would fail it. A future `Popup` would not be caught by anything.
2. **TalkBack (judged: the controls stay separately reachable; N2).** Verified in the bytecode, not assumed:
   - `SemanticsNode.mergeConfig` skips every child for which `isMergingSemanticsOfDescendants()` is true (`ifne` over `mergeChild`).
   - `getChildren$ui` for a merging node returns `findOneLayerOfMergingSemanticsNodes`.
   - So the switches (`toggleable`), the Pills (`clickable`), Done, Erase and Cancel are each their own node and child of `settings-sheet`.
   - What the sheet node itself carries is only the loose texts and the body's scroll actions, which is the hop-1 note.
3. **The reset order (judged: holds).** See "Re-derived", rows 3–5.
4. **Feedback threading and lifecycle (judged: sound; N5 for the one gap).**
   - **`@Volatile override`:** correct. `on()` reads it once into `out`, so one event cannot split across two doors.
   - **`DeviceFeedback.view`:** every writer and reader is on the main thread (`onStart`, `onStop`, the controller's actions, `performClick`). `feedbackViewGone` clears only when `device.view === view`, so an older instance stopping after a newer one started leaves the new view. `onStop` runs `goHome` (home tone through the pool, no view needed), then `visible(false)`, then clears the view. A tick after that is dropped: right.
   - **`SoundPool` async load:** a sound asked for before its file has loaded returns stream 0 and plays nothing. The controller does not exist until the IO load of the library finishes, and `Feedback`'s gate reads `built.value?.settings?.soundOn == true`, so there is no sound before the controller exists. The window in which a lock could outrun the load is negligible. The decision (no listener, no queue) stands.
   - **`SoundPool` never released:** `AppGraph` is a process-lifetime object, and the pool is one native object of five short files (about 65 KB). The OS reclaims it with the process. It does not matter.
   - **Construction on the main thread** inside `onCreate` (`AppGraph.start`): five `load(context, resId, 1)` calls on small raw files. Accepted by the design ("synchronously, before the load").
5. **Play time while the sheet is open (judged: consistent with REQ-029, REQ-031, REQ-032 and the design).** `fold()` gates only `timeSeconds` (`PlayController.kt:541`). REQ-032 rule 2 pauses "puzzle time"; REQ-029's statement adds every active second "WHILE the game counts an active second (TYPE-005)", and TYPE-005 is visible plus a touch within 60 s. The sheet is in the activity's window, so `dispatchTouchEvent` sees its touches. An untouched open sheet adds at most 60 s (REQ-029.A1). `openSettings` and `closeSettings` fold before flipping the flag, so the seconds before opening belong to the puzzle and those after belong to play time only. REQ-031.A2 ("counts active seconds only") is not touched by this.
6. **Contrast (judged: note; N1).** No locked REQ, `architecture.md` guardrail or `design-inputs.md` item sets a contrast bar (grep for "contrast": no hit).
7. **Settled items** are not raised.

## Findings

### Blockers
None.

### Shoulds
None.

### Notes

**N1 — Contrast: the how-to body and the licence line fall short of 4.5:1, and Done is the weakest control (`SettingsScreen.kt:429-450`; owner-grade look, checkpoint item 1).** The ratios, WCAG relative luminance, computed from the constants:

| Text on its ground | Ratio |
|---|---|
| `MUTED` #7E8299 on the sheet `PAPER` #FBF7EE: how-to body 14 sp, section headings 13 sp, licence line 12 sp | **3.55** |
| `MUTED` on white (`CARD`) | 3.79 |
| White on `GO` #2DBE7E: the Done label, 15 sp bold | **2.39** |
| `WARN` #E8505B on white: the Reset label; white on `WARN`: Erase | 3.67 |
| `FREE_INK` on `FREE_BACKGROUND`: 5.86; `INK` on `PAPER`: 12.62; `INK` on `PRIVACY_BACKGROUND`: 11.54 | pass |
| The off track `EDGE` against the white row | 1.44, but the row also says "Off" in text |

The how-to text is the only help text in the game and sits in the weakest grey. Done, the only way off the sheet other than Back or the scrim, is the lowest ratio of the screen. These follow DI-1's tokens, which the decisions row names as a behaviour reference only. Smallest fix, if the owner asks: darken `MUTED` on this sheet to about #5C6078 (5.8:1 on `PAPER`), and give Done a dark label (`INK` on `GO` is 5.6:1) or a darker green. No logic changes.

**N2 — TalkBack: the sheet's controls are reachable, but three things read oddly, and the gear is not reachable at all (`SettingsScreen.kt:165-170, 149-155, 302-327`; `TopBar.kt` `GearButton`).**
- **The sheet node reads "Play time".** The sheet is `mergeDescendants` over a body that is not. The one loose text, the "Play time" heading, merges into the sheet, and so do the body's scroll actions. The result is a node that announces "Play time" and scrolls, ahead of "Settings". Smallest fix: give the "Play time" heading its own merged node (as "Best times" already has) and keep the sheet a plain container; the tag `settings-sheet` stays. The kit's `settings-sheet` reads would need a look, since hop 1 found the same node.
- **The scrim is an unlabelled click target** for TalkBack and a D-pad (`clickable` with no label), and the Play screen beneath it stays in the accessibility tree. Smallest fix: `onClickLabel` on the scrim's `clickable` (the "Done" text), and nothing else.
- **A switch row may be read twice.** The row merges the visible "On" / "Off" text with the toggle state, so the state can be announced as text and as state. By reading, not run on a device.
- **The gear has no semantics at all** (`PlayTags.GEAR` KDoc: "no `clickable` and no semantics click action"), so TalkBack cannot focus or activate it and cannot reach Settings, the privacy text included. This is the same as ‹, › and the counter, and the pieces are drag-only. No REQ asks for it, so it is a design fact, not a defect; stated so the G4 surface carries it.

**N3 — Nothing guards a future `Popup` or `Dialog` (F1 re-check, watch point).** Today none exists. S-49's prose and the comments forbid it, and the positive control covers two subtrees. A later `DropdownMenu`, `Popup` or `Dialog` would silently click with sound off, and nothing would go red. Smallest fix, in the kit and not the product: a JVM pin in `app/src/test` (beside `ReleasePromiseKitTest`) that scans `app/src/main`, `play/src/main` and `play/src/debug` for `androidx.compose.ui.window.Popup`, `Dialog` and `androidx.compose.material` imports, with a positive control on a string it builds itself. A record-keeping alternative is a `platform-notes/android.md` line.

**N4 — The reset question puts Erase where Reset was, and Erase is the filled, default-looking answer (`SettingsScreen.kt:269-295`).** Reset is replaced in place by the question and a row of Erase then Cancel, both starting at the same left edge as Reset. The body then scrolls the row into view with `bringIntoView`. On a short window with Reset near the bottom edge, the scrolled Erase can land under the finger that pressed Reset. A second tap, or a double tap, then erases everything, against REQ-034 rule 1 ("a second, explicit confirmation"). It is rare (the scroll must put Erase under the old press, and the second tap must follow within about 100 ms), and unrun. Smallest fix: swap the row's order (Cancel first, Erase second) and make Cancel the filled one, so a repeat tap on the old place lands on the harmless answer. Kit impact: `ResetKitTest` presses by tag, so it is unaffected.

**N5 — The positive path of REQ-033's statement is not proven by any run; it rests on the owner's ear (`DeviceFeedback.kt:40-50`; checkpoint item 4).** The kit's recorder sits at the door (`Feedback`), and `SoundAssetsKitTest` checks the files. Nothing observes that `SoundPool.load` succeeded for a file or that `play` returned a live stream: `load` returns 0 on a failed load and `ids.getValue` then hands 0 to `play`, which returns 0 silently. A WAV the device cannot decode would play nothing with no error and a green kit. The decision to have no load listener is recorded and stays. Smallest fix, if wanted at G4: a debug-only log of each `onLoadComplete` status (no queue, no wait), or simply tell the owner that the sounds are an ear-test item on a real device, which checkpoint item 4 already does.

## Re-derived

| # | Claim | How checked | Result |
|---|---|---|---|
| 1 | The tree builds and the unit tests pass | Run 1 `cmd.exe //c ".\gradlew.bat -q :app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun"` (exit 0), run 2 `cmd.exe //c ".\gradlew.bat -q :content:testDebugUnitTest --rerun :play:testDebugUnitTest --rerun :kernel:test --rerun :contracts:test --rerun :store:testDebugUnitTest --rerun :app:testDebugUnitTest --rerun --continue"` (exit 0), 22:22; counts read from `*/build/test-results` | app 10/0, content 80/0, contracts 2/0, kernel 70/0, play 146/0, store 12/0 = **320/0**, equal to the run log's t061-host |
| 2 | The five WAVs are the tool's output and within REQ-033 rule 1 | `tools/make_sounds.py` copied to the scratchpad and run there; `cmp` against the five product files; `wave` header and length read | All five **byte-identical** (deterministic); 16-bit mono 22 050 Hz; 40, 60, 70, 160 and 570 ms, all at most 1 s; the loudest peak is the lock at 16 384 (−6 dBFS); the return is the quietest (peak 2 550) and lowest (300 → 180 Hz), as REQ-033 rule 2 asks; every file starts and ends on 0 (no click). The table in `ASSET-LICENCES.md` matches the lengths |
| 3 | Reset order, controller side | `PlayController.resetAll` read against `fold`, `load`, `keep`, `savedOf`, `writeTimes`, `setVisible`, `open` | `resetAll` never calls `open`, so the leaving `keep()` of the pre-reset board is never queued. Every step runs on the main thread, so neither the 250 ms ticker nor `setVisible(false)` can run inside it. `load(puzzle)` reads the emptied cache (`FRESH`), so its `keep()` finds equality and writes nothing; only `saveLastOpened` follows. `playTimes` is re-read after the kernel's reset, so the next `writeTimes` writes `NONE`-based times. `unsavedPlaySeconds = 0` stops a stale cadence write. The board in memory is New, empty, time 0, no best, tray at rest, no drag, cue or celebration |
| 4 | Reset order, store side | `Persistence.resetAll`, `saveSettings`, the writer loop; `FileProgressStore.resetAll` and `save` | Enqueue and cache change sit in one `synchronized(lock)` step, so no reader sees between them and the FIFO has one order: every write queued earlier lands before `Write.Reset`, every later one after it. `store.resetAll()` is `copy(puzzles = emptyMap(), playTimes = NONE)`, which keeps `settings` and `lastOpened`; `Persistence` keeps `stored` and `lastOpened` the same way. A switch pressed before Erase lands before the reset and survives it. One atomic rewrite (ADR-003) means a death mid-reset leaves the old file or the reset one |
| 5 | The kernel's reset | `ActiveTime.resetPlayTimes` read against TYPE-005 and the KDoc | `accrue()` first (a backwards reading shifts the last touch as before), then `pendingMs = 0` and `times = PlayTimes.NONE`; `shown` and `lastTouch` are kept. This equals the KDoc and S-45Δ. The decision row "All-time never decreases between resets" names REQ-034 as the one exception |
| 6 | `SemanticsNode` merge rules | `javap -p -c` on `androidx.compose.ui.semantics.SemanticsNode` from `ui.aar` (ui-android 1.12.1): `mergeConfig`, `getChildren$ui` | `mergeConfig` calls `mergeChild` only for children that do not merge their own descendants; a merging node's children are `findOneLayerOfMergingSemanticsNodes`. So Done, the switch rows, the Pills and the value rows stay separate nodes under the sheet; the loose "Play time" heading and the body's scroll actions merge into the sheet (N2) |
| 7 | Layer takes every touch | `SettingsScreen` structure against design §3 and library fact 1 (the design review's re-derived row 1) | The scrim and the frame holding the sheet are siblings; the sheet's non-consuming `pointerInput` is the topmost hit under it, so a touch there reaches neither the scrim nor the Play screen, and its children's `clickable`s still see the event. The gear is a router control: inert while the grid or the DEV sheet is open or a drag runs |
| 8 | `Feedback` against REQ-033 | `Feedback.kt` read against the statement, rules 2–3, A1 | Both entries return first while `soundOn()` is false: events and clicks. The five events map to five sounds; a lock also ticks; no event or sound exists for a failed lock or a shake. `SoundKitTest`'s negative leg and its positive control in the same test can fail: a broken gate records calls with sound off; a missing provider leaves the recorder silent and the positive control fails |
| 9 | Texts | `strings.xml` against REQ-009 and REQ-049 | `free_note` and `privacy_text` equal the REQs character for character; the order follows REQ-032 rule 1 |
| 10 | Owner's rule | grep of the diff and product sources | No permission, network, analytics, ad or purchase name; manifest and dependency lines unchanged |
