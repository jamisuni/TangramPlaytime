# Design — WO-007 · #Settings

**Author:** Design Author (Opus 5.5) · **Date:** 2026-10-21 · **Status:** Reviewed
**Write-back:** 2026-10-21, for `reviews/WO-007-design-review.md` F1 (the click goes through `Feedback` and follows the switch), F2 (the kit doors know the layer), F3 (the moved-› pins land with S-53), F4 (the restart leg runs on the JVM), F5 (the bar's second line shrinks to fit). Plan write-back 2026-10-21: `decisions.md` "WO-007 / P3 plan" rows; §4 now points at `tasks.md` § WO-007.
**Full because:** (b) `IActiveTime` gains a member (notify). ADR-003 format 1 already holds `Settings(timerShown, soundOn)`: no new file field, so no (c) and no hard-stop 2.

## 1. Scope

- **Implements:** the 16 IDs of WO-007 § Scope; the licence line untokened.
- **Governed:** `IActiveTime` changes (S-44Δ). `IProgressStore` and `Types.kt` are consumed unchanged; so are `ILockEngine` and `IPuzzleLibrary`.
- **How it works:**
  - The gear (`:play`) calls `openSettings()`.
  - `:app`'s `SettingsScreen` composes over `PlayScreen`, in the same window.
  - The switches and Erase are controller calls through `Persistence`'s FIFO.
  - Every `PlayEvent`, and every Compose click (`LocalSoundEffect`), reaches `:app`'s `Feedback` (O-07).
- **Contract delta** (orchestrator: delta row, re-freeze):
  - `fun resetPlayTimes()`, with this KDoc: "Erases today's and the all-time play time (REQ-034 statement). Afterwards `playTimes` reads `PlayTimes.NONE` until a second counts; an uncounted part of a second is dropped. Visibility and the last touch (TYPE-005) are kept. The confirmation is the caller's (REQ-034 rule 1, A2)."
  - The banner's `Serves:` gains REQ-034.
  - By the G2 route: the registry row's Purpose names `resetPlayTimes`, and its Serves gains REQ-034.
- **build-map v1.9** (notify):
  - `#ActiveTime` code home `kernel/` → `app/` (Q10; the review's Re-derived row 10 agrees).
  - `#Settings` → In progress.

## 2. Seam table

| Seam | Kind | Signature / shape | REQ | Task |
|---|---|---|---|---|
| S-44Δ | I* | `fun resetPlayTimes()` (§1) | REQ-034; carry 2 | 053 |
| S-45Δ | `ActiveTime` | `accrue()`; `pendingMs = 0`; `times = PlayTimes.NONE`. `shown`, `lastTouch`, `last` kept | REQ-034 | 053 |
| S-19Δ2 | `Persistence` | **`var settings`:** read under the lock; private set.<br>**`saveSettings(s)`:** updates the cache, then queues `Write.Settings`.<br>**`resetAll()`:** queues `Write.Reset` (`store.resetAll()`), empties the cache, sets `times = NONE`; the settings and last-opened are kept.<br>All in one FIFO | N1; REQ-031, 033, 034; G-13 | 054 |
| S-1Δ4 | controller reads (snapshot) | **`settingsOpen`:** false at start.<br>**`settings`:** the controller's own `mutableStateOf`, seeded from `persistence?.settings ?: Settings()`.<br>**`timerShown`:** `settings.timerShown`.<br>**`bestTimes: List<BestTime(puzzle, seconds)>`:** listed puzzles with a stored best, in ‹ › order; the open puzzle's comes from `bestSeconds`. Refreshed by `openSettings` and `resetAll` | REQ-032 r2; 031; 033; 030 r3 | 054 |
| S-2Δ4 | controller actions (Main) | **`openSettings` / `closeSettings`:** fold first, then set the flag. The fold predicate gains `&& !settingsOpen`.<br>**`setTimerShown` / `setSoundOn`:** `settings = copy(…)`, then `persistence?.saveSettings`.<br>**`resetAll()`:**<br>(1) fold;<br>(2) `time?.resetPlayTimes()`;<br>(3) `persistence?.resetAll()`, else `memory.clear()`;<br>(4) `load(puzzle)`: New, and nothing saved but last-opened;<br>(5) re-read `playTimes`; `unsavedPlaySeconds = 0`; `bestTimes` empty.<br>It never calls `open` and emits no event | REQ-032 r2, A1; 030 r1; 034; TYPE-006 draft | 054 |
| S-11Δ4 | `PlayTexts` | `timeOf` and `titleOf` become public | REQ-029 r2; 047 | 056 |
| S-53 | gear, `:play` | **Tag:** `PlayTags.GEAR = "gear"`.<br>**Slot:** `ScreenGeometry.gear` is a 52 dp column at the safe right edge, the bar's full height. `navNext` now ends at `gear.left` (S-32Δ).<br>**Hit:** `Control.GEAR` acts at lift within the slop, with no long press, and calls `openSettings()`. It is inert under hit orders 0 and 1.<br>**Drawing:** DI-1's gear, 24 dp, muted, drawn in px | REQ-032; 037 | 056 |
| S-22Δ3 | `TopBar`'s second line (F5) | Dots, separators, "n / N" and the state word scale by one factor **f = min(1, available ÷ natural width at 13 sp)**, floored at 0.75.<br>The natural width is measured with `rememberTextMeasurer`; text sizes and the dot and separator dp are computed from f and drawn in px (G-17; no layer scale).<br>If the line still does not fit at 0.75, the state word ellipsizes; the number never does. Tags and texts are unchanged | REQ-024 rule (consumed); REQ-006.A1's 360 dp phone | 056 |
| S-49 | `SettingsScreen(controller, texts)`, `:app` | **Placement:** a sibling in `setContent`, **never a `Dialog` or `Popup`**.<br>**Scrim:** covers the whole window; its `clickable` calls `closeSettings()`.<br>**Sheet:** the scrim's *sibling* (as `Overview.kt:126–142`), inside `safeDrawing` + 16 dp, at most 560 dp wide, with a non-consuming hit target.<br>**Header (fixed):** the title and Done.<br>**Body (scrolls):** timer, sound, today / all time, best times, reset, how to play, free note (green box), privacy, licence.<br>**Reset:** inline `remember` state; only Erase calls `resetAll()`.<br>**Back:** `BackHandler(settingsOpen) { closeSettings() }` | REQ-032, 009, 049, 029.A2, 030 r3, 034 | 059 |
| S-49 tags | `SettingsTags` | **Controls:** `settings` (the scrim), `settings-done`, `settings-timer` and `settings-sound` (whole-row `toggleable`, Role.Switch, ≥ 56 dp), `settings-reset`, `reset-erase`, `reset-cancel`.<br>**Inert merged nodes:** `settings-title` (the header title: the kit's window-touch point, F2), `settings-sheet`, `play-time-today` and `play-time-all` ([label, value]), `best-times` ([heading] + "–" if none), `best-time-<id>` ([title, time]), `reset-question`, `how-to-play`, `free-note`, `privacy-text`, `licence-line`.<br>**Text entries:** each one is a resource, a title or a time; never concatenated | REQ-037; closed world | 059 |
| S-49 strings | `values{,-fi}` | Each new string has a `fi` reference, listed in `review-notes.md` (G-10):<br>• `settings_title` Settings<br>• `setting_timer` Timer on screen<br>• `setting_sound` Sound<br>• `setting_on` On<br>• `setting_off` Off<br>• `play_time_title` Play time<br>• `play_time_today` Today<br>• `play_time_all` All time<br>• `best_times_title` Best times<br>• `reset_all` Reset all progress<br>• `reset_question` Erase every puzzle and all times? Your settings stay.<br>• `reset_erase` Erase<br>• `reset_cancel` Cancel<br>• `how_to_title` How to play<br>• `how_to_text` (below)<br>• `free_note` and `privacy_text`: REQ-009 and REQ-049 verbatim<br>• `licence_line` Open source, GPL-3.0 — source code: github.com/jamisuni/TangramPlaytime<br>Reused: `done`, `no_time` | 009, 049, 047 | 059 |
| S-50 | `Feedback`, `:app` (F1) | **Types:**<br>• `enum Sound { PICK_UP, TURN, LOCK, HOME, SOLVE }`;<br>• `interface FeedbackOut { play(Sound); tick(); click() }`;<br>• `class Feedback(soundOn: () -> Boolean, device: FeedbackOut) : SoundEffect`, with `@Volatile internal var override: FeedbackOut?` (the kits' door).<br>**`on(event)`:** returns while `!soundOn()`. Otherwise it maps PickedUp, Turned, Locked (+ `tick()`), WentHome and Solved to the matching `Sound`.<br>**`playClickSound()`:** returns while `!soundOn()`, else calls `click()`.<br>Both paths go to `override ?: device`. This door is the only playback, haptic or click caller | REQ-033, A1; O-07 | 061 |
| S-51 | `DeviceFeedback(context)` | **Sounds:** one `SoundPool`, maxStreams 4, USAGE_GAME / SONIFICATION; priority SOLVE 2, LOCK 1, the rest 0.<br>**`tick()`:** `view?.performHapticFeedback(CLOCK_TICK)`.<br>**`click()`:** `view?.playSoundEffect(SoundEffectConstants.CLICK)`, the call today's `AndroidSoundEffect` makes, under the device's touch-sound setting.<br>**`view`:** the decor view; set in `onStart`, cleared in `onStop` by the activity that set it | REQ-033 r1, r3; G-01, G-11 | 061 |
| S-52 | sounds | `tools/make_sounds.py` (stdlib only, deterministic) writes `res/raw/sfx_{pick_up,turn,lock,home,solve}.wav`, 16-bit mono at 22 050 Hz. The recipes are in row "The five sounds and the tick". `ASSET-LICENCES.md` gets a § Sounds (G-06, G-16) | REQ-033 r1–2 | 057 |
| S-15Δ4 | `AppGraph`, `MainActivity` | **`AppGraph.start`:** builds `feedback = Feedback({ built.value?.settings?.soundOn == true }, DeviceFeedback(app))` synchronously, before the load; the controller's `onEvent = feedback::on`. `AppGraph.feedback` is internal.<br>**`MainActivity`:** `setContent`'s whole content sits inside `CompositionLocalProvider(LocalSoundEffect provides AppGraph.feedback)`; the settings layer and Back come with 059; the haptic view with 061 | O-07; REQ-033.A1 | 059, 061 |

**How-to-play, en (59 words; a `review-notes.md` proof-reading item):** "Drag a piece from the tray onto the shape. Tap a piece to turn it; while dragging, twist it with a second finger. The badge beside the parallelogram flips it. A dropped piece snaps into place or goes back to the tray. Fill the shape to see its picture. Press the number at the top to see all puzzles."

**Tokens (Q10)**

- **New, in `app/`:**
  - `SettingsKitTest`: REQ-032.A1–A2, REQ-009.A1, REQ-049.A1, REQ-029.A2, and REQ-031.A2 as an extra token.
  - `ResetKitTest`: REQ-034.A1–A2.
  - `SoundKitTest`: REQ-033.A1.
- **Re-covered:**
  - `PlayThroughKitTest` (REQ-001.A1, REQ-008, REQ-010.A1): the gear, the whole sheet scrolled under the closed world, both switches flipped and back, Reset then Cancel, then Done.
  - `TouchTargetKitTest`: the gear and S-49's controls.
  - `LanguageKitTest`: the `fi` resources (still unverified).
  - REQ-010.A2 stays V-01's.
- **REQ-029.A2:** the build-map move in §1 makes `app/` its code home.
  - **Its oracle can fail (F2):** the kit clock counts M s on day D, then N s on day D+1. Both rows must show today = N and all-time ≥ M + N, both above 0 and different.
  - **On the open screen:** a window touch on `settings-title` plus K s moves both rows by K s, while the In-progress puzzle's time stays put.
- **The closed world** admits the screen's times exactly as S-48 forms them at each read.

**Kit doors (Q11)**

- **JVM:** `KitStore`, the real `ActiveTime` and a kit clock.
  - **The restart leg (F4)** runs in `app/src/test`, over the real `FileProgressStore` on a temp file, `Persistence`, `ActiveTime` and the controller.
  - **Trap state:** a Solved puzzle with a best; the open puzzle In progress with pieces and time; play time > 0; timer on.
  - **Steps:** `resetAll`, `flush`, then a new store, `Persistence.load` and a controller over the same file.
  - **Assertion:** every puzzle FRESH, `PlayTimes.NONE` (plus any second counted since the reset), and the settings and last-opened kept.
- **`app/` device:** the doors are `AppGraph.controller`, `AppClock.override` and `AppGraph.feedback.override` (a recorder, set back to null by `onRestore`). F2 adds:
  - **Where window touches land:** on the open screen, `kitTouchThroughWindow` touches `settings-title`, read on the screen in use; elsewhere it uses the board's inert point.
  - **The touch helper's check:** its before/after comparison adds `settingsOpen`.
  - **`KitApp.start`:** fails loudly while `settingsOpen`. It reads `settings`, and registers `onRestore("the settings")`, which puts both values back through the setters.
  - **`restoreController`:** calls `closeSettings()`.
  - **A resetting class:** needs every puzzle New at its start and leaves them New. Before both Cancel and Erase it builds the JVM leg's trap state, by touch and through the real switch.
- **Sheet scrolls** run with auto-advance on.

## 3. Risks & edge notes

- **Must-nevers:**
  - with sound off, no sound, tick or click (`Feedback`'s first lines, per event and per click);
  - no failure sound;
  - Cancel changes nothing;
  - a reset keeps the settings;
  - no difficulty control (G-09);
  - the free note never shows over a puzzle;
  - opening or closing the screen changes no placement, state, tray turn, open puzzle or puzzle time.
- **Reset order:** a pre-reset save queued after `Write.Reset` would restore that puzzle in the store while the cache says New.
  - `open()`'s leaving `keep` would make such a save, so `resetAll` never calls `open`.
  - A process death mid-reset leaves either the old file or the reset one: ADR-003 does one rewrite, and the FIFO lands every earlier write first. The death pair is unchanged; F4's JVM restart leg carries the restart.
- **N1:** any reader of the load-time settings keeps the timer stale and the sound on. Every reader reads `controller.settings`.
- **The layer must take every touch:**
  - the scrim's `clickable` and the sheet's hit target, as *siblings*;
  - a sheet nested inside the scrim would close at any tap;
  - a `Dialog` would stop play time counting there.
- **The moved › (F3):** these files land in one step with S-53 as declared outputs, each a corrected check in `proposals.md`:
  - kit files, with the test author's task (055): `SafeAreaKitTest` (:202 fixture, :288–293), `TopBarKitTest` (:192–234). F5's pin is not one of these play-kit files: it is the app-kit class `TopBarFitKitTest` (stage 2, in one step with S-22Δ3; plan row "F5's second-line pin is an app-kit class …");
  - scaffolds, with the S-53 task (056): `ControlsScaffoldTest` (:55–59), `SafeAreaScaffoldTest` (:40, :101), `ScreenSeamScaffoldTest`.
- **The second line (F5):** measured, 52 dp narrower than today.
  - **The space:** the counter's content is 180 dp on the 360 dp phone at font scale 1. The worst case, "100 / 106 · in progress", needs about 198 dp.
  - **The fit:** f ≈ 0.91 (11.8 sp); the Finnish words are shorter or equal.
  - **The pin** (`TopBarFitKitTest`, app kit; its `fi` leg reads `:app`'s resources): at 360 × 780 dp, on a three-digit In-progress puzzle, in `en` and `fi`, the number's and the word's `TextLayoutResult` have no visual overflow and their nodes lie inside `counter`.
- **Carry 4:** the aid's burst needs no burst code; 4 streams and the priorities keep the chime. The aid is debug only.
- **`onStop`'s `goHome`** plays the 160 ms home tone as the game leaves the screen; accepted.
- **Outside the tap path:**
  - focus moves from a hardware keyboard or D-pad play the platform's navigation sound through `AndroidComposeView`'s own path, which `LocalSoundEffect` does not reach. Not a touch action; a G4 note;
  - the DEV prompt's IME is another app's window (debug only).
- **Library facts relied on** (1.12.1 bytecode; android-37):
  1. The topmost hit sibling takes the pointer, and lower siblings are not hit. A parent in the path still sees it.
  2. *(Corrected, F1.)* Foundation's `performClick()` calls `playClickSound()` before `onClick`. It reads `currentValueOf(LocalSoundEffect)` (flag `isInteractionSoundEffectOnClickEnabled`, true), so every `clickable` / `toggleable` plays whatever `LocalSoundEffect` provides. By default that is `AndroidSoundEffect(view)` → `View.playSoundEffect(CLICK)`. The router's raw-pointer controls play nothing.
  3. `SoundPool` needs no permission and loads asynchronously; past maxStreams it stops the lowest priority, then the oldest.
  4. `performHapticFeedback(CLOCK_TICK)` needs no permission and an attached view.
  5. `stringResource` uses the S-36-wrapped resources.
  6. `LocalSoundEffect` (`getLocalSoundEffect`), the `SoundEffect` interface (`playClickSound()`, not sealed), `DelegatingSoundEffect` and `SoundEffectOnInteraction` are public with no opt-in marker. Only `ProvideCommonCompositionLocals` and `AndroidComposeUiFlags` carry `@ExperimentalComposeUiApi`, and neither is used.
- **Highest-risk part:** `resetAll`'s order (S-2Δ4, S-19Δ2). The review's Re-derived row 7 found it holds on the code.
  - **Oracles:** the recording `KitStore` (no pre-reset write after the reset) and F4's JVM restart leg.
  - **Next:** REQ-033.A1's oracle, held to one device class.

## 4. Suggested cut

**Superseded by the plan's cut in `tasks.md` § WO-007 (TASK-052 … TASK-062).** It differs from this design's suggestion in five ways (`decisions.md` 2026-10-21 "WO-007 / P3 plan" rows):
- **`Feedback`'s JVM pins** (map and click gate) move to kit stage 4, TASK-060. In stage 1 they would hold `app/src/test` in a compile window until TASK-061.
- **The WAV pin** ("each WAV ≤ 1 s, and each one listed") moves to kit stage 3, TASK-058, which lands in one step with TASK-057.
- **No `[P]`.** TASK-057 and TASK-056 both change release-APK sources that the JVM line reads.
- **F5's pin** is the app-kit class `TopBarFitKitTest`, in stage 2 (TASK-055), in one step with S-22Δ3.
- **The death pair** carries no new line. It re-runs on the floor and dev images at TASK-054, TASK-059, TASK-061 and TASK-062.

The seams keep their names and their delivering tasks; no seam changed at planning.

*Alternatives considered:*
- **The screen in `:play`:** `:app` owns it.
- **`SoundEffectOnInteraction(enabled = soundOn)` (F1 a):** the click stays on `AndroidSoundEffect`, which no recorder sees, so the oracle would need a second door.
- **(F1 c):** the orchestrator's ruling rules it out.
- **A constant change for F5:** it fits the measured case by 2–10 dp, and moves ‹ or shrinks the dots fixed on 2026-10-15.
- **The death pair for F4:** it needs new phases and a script change.

## Handoff — Design Author · WO-007 (write-back)
- **Scope:** WO-007's 16 IDs · governed: `IActiveTime` (changes, notify)
- **Inputs read:** `reviews/WO-007-design-review.md` · ui-android and foundation-android 1.12.1 (bytecode) · `FileProgressStore.kt`, `app/build.gradle.kts` · the earlier inputs
- **Result:** this design (Reviewed); six amending `decisions.md` rows
- **Status:** forward → Design Reviewer (F1 only)
- **Traceability delta:** S-22Δ3 added; S-50, S-51 and S-15Δ4 changed; tasks renumbered TASK-052…062
- **Notes for next station:** F1 rests on fact 2 (corrected) and fact 6 (new).
