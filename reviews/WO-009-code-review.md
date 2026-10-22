# WO-009 code review (P5) — code-reviewer, fresh

Tree: HEAD `9d65059` plus the WO-009 working tree. Verdict: **forward** (0 Blockers, 3 Shoulds, 3 Notes).

## Findings

| ID | Sev | Category | Target | Observation | Proposed resolution |
|---|---|---|---|---|---|
| S1 | Should | Release gate | `app/build.gradle.kts:41-45`; `release/CHECKLIST.md` step 6 | The gate runs before `packageReleaseBundle` (dry-run order: `content:checkReleasePuzzles`, then `app:packageReleaseBundle`, then `app:bundleRelease`). But a refused build does not remove an AAB from an earlier green build. Re-derived in a scratch copy: with 20 marks `bundleRelease` wrote `app-release.aab` (20 puzzles). I then unmarked one and the build correctly refused, but the old AAB stayed in `app/build/outputs/bundle/release/`. This is the exact stale-artifact case `CHECKLIST.md` step 6 only warns about in prose. Android Studio's signed-bundle wizard reports the failure, but the file is still there to be picked up. | Make the gate remove the stale AAB. Examples: a `doFirst { delete(bundle output dir) }` on `:app:bundleRelease` / `packageReleaseBundle` registered before the gate, or a `Delete` task ordered after the gate fails. Keep the prose warning. |
| S2 | Should | Hard-limit adjacent / data loss | `tools/release_install.sh:~138` (`adb uninstall`), `:~106` (device check) | The script accepts any single attached device (`get-state == device`) and runs `adb uninstall app.tangramplaytime` before installing. The owner's children play the debug build on his phone (checkpoint item 1: "the test build on your phone keeps all 106 for the kids"). If that phone is the only device attached, one run wipes the children's progress and times, and nothing in the script stops it. The scratch-dir and signing guards are sound. The device guard is not. | Refuse a non-emulator device unless an explicit flag is given (e.g. serial must start `emulator-`, or `ro.kernel.qemu=1`). Say so in the header comment and `usage`. |
| S3 | Should | Guardrail G-16 | `ASSET-LICENCES.md` § Launcher icon | The new section lists the three icon resources and `icon-512.png`. It does not list `release/pictures/feature-graphic.png` or `.svg`, which are store images made by `tools/make_feature_graphic.py` from DI-9 plus a rendered title. The title is system sans-serif rasterised by Chrome. G-16: "every shipped … image is listed with its licence". The README's "Fonts: system font only" line also never mentions that a rasterised system-font title ships in an image. | Add two rows (made for this project, GPL-3.0; DI-9 plus the title in the system sans-serif). Optionally add a one-line font remark. `AssetLicencesKitTest` (R5's pin) would not catch this omission, since it only checks `Tangrams/` rows, so it is a doc fix. |
| N1 | Note | Maintainability | `content/build.gradle.kts` `CheckReleasePuzzles`; `tools/mark_puzzles.py`; `ReleaseSetKitTest` | The five-item bar now exists three times (Kotlin gate, Python `status`, kit constants). This is by design (S-54/S-56), and I diffed the labels and constants: they agree. A drift guard would be one test or a script comparing `status` output with the gate's message. Today the only link is the shared wording. | Optional. Name the three homes in a comment in each so a future change touches all three. |
| N2 | Note | Edge case | `content/build.gradle.kts` `readPuzzle` | Gradle's `JsonSlurper` is lenient (comments, duplicate keys, last wins), while `ReleaseMarksKitTest`'s reader is strict RFC 8259 and refuses duplicates. A malformed file could be counted marked by the gate and the AAB, then fail the kit leg. The mark tool refuses files whose `reviewedByHuman` appears other than once, and the validator rejects malformed files, so this is not reachable through the shipped paths. | None needed. Mention in the README only if the owner hand-edits puzzle files. |
| N3 | Note | Test substance | `ReleaseMarksKitTest` (`REQ-039.A2`) | The test is honest and can fail: the crafted-text test covers false, missing provenance, missing field, decoy, truncated and the unnamed file. On this tree it is green over an empty release (`index.txt` names zero files), which is vacuous. The plan accepts this and puts the real verdict at TASK-071. | The Test & Verify station should rerun it after the marks and state the marked count it saw. |

## Checked and clean

- **Debug and release bundles.** `bundlePuzzles` copies every file for every non-release variant. `bundleReleasePuzzles` keeps only JSON `true`; a string `"true"` and a numeric `1` are not marks. Task inputs are `@InputFiles` with `NAME_ONLY` path sensitivity, which still hashes content, plus `@Input markedOnly`, so a mark rebuilds. The release APK carries an empty `index.txt` on this tree.
- **`assembleRelease`, `packageRelease` and the debug tasks** do not depend on the gate (run 1 built both APKs).
- **No config-cache or variant-API misuse seen.** `onVariants` plus `addGeneratedSourceDirectory` is the supported route.
- **Honest green.** In a scratch copy with 20 marks over four categories (the square, `shapes-mini-1`, and four warm-ups), `bundleRelease` succeeded and the AAB held 20 puzzles. All content tests passed, including the six `ReleaseSetKitTest` release legs. The only `play` failure was `ReleaseBuildKitTest`, because I had not built the debug APK in the copy. It is not a product finding.
  - Unmarking one puzzle gave "19 of 20" and refused.
  - Unmarking the mini gave "none" for item 4 and refused.
  - Each message names the unmet item and its count.
- **Owner's limits.** `Tangrams/` has no diff beyond `README.md`; no `reviewedByHuman: true` anywhere in the tree. No signing config, keystore or secret in any `.kts`, `.xml` or `.properties`. `release_install.sh` signs a scratch copy only, with the Android SDK's public default debug key (`android`/`androiddebugkey`), and refuses a scratch path inside the project (`realpath`, case folded).
- **G-01.** The manifest diff is the icon attribute plus `uses-feature … required="false"`. No permission, network, ad or purchase addition.
- **Store texts.** Title has no "free" and the short description is 78 characters with the promise words. `PRIVACY.md` carries REQ-049 rule 1 verbatim. No e-mail address anywhere. Full-description claims (tap to turn, two-finger twist, flip badge, snap or return, settings contents) match `how_to_text` and the REQs. The Finnish texts are marked reference.
- **Tokens.** No dotted token for REQ-001.A2, REQ-048.A1 or REQ-048.A2 anywhere in code. The added tokens (REQ-048.A3 on two assertions that already existed, REQ-002.A2 on the settings control set) change no assertion. `NoToddlerModeKitTest` has planted-control fixtures that must fail. `StoreIconKitTest` and `LauncherIconKitTest` sample DI-9 points and carry a positive control.

## What I ran

| Command | Result |
|---|---|
| `PYTHONUTF8=1 python tools/tests/test_mark_puzzles.py` | `ok: 14` |
| `PYTHONUTF8=1 python tools/tests/test_make_icon.py` | 6 of 6 passed |
| `python tools/mark_puzzles.py status` | 0 of 106 marked, `release-ready: no`; `git status Tangrams` shows only `README.md` modified |
| JVM run 1 (`assembleRelease`, `assembleDebug`, `packageRelease --rerun`, `packageDebug --rerun`) | Built, no output |
| JVM run 2 (`--continue`, all six modules, `--rerun`) | XMLs: **351 tests, 0 skipped, 6 failures**, exactly `ReleaseSetKitTest`'s release legs: `theReleaseContainsAtLeast20…`, `theReleaseSetLoadsWithNoViolation`, `theReleaseListOpensWithAThreePieceMini…`, `theReleaseListThenHasAtLeastFourWarmUps…`, `atLeastFourCategoriesArePresentInTheRelease`, `shapesSquareIsAmongTheReleasedPuzzles` |
| Scratch copy (`.../scratchpad/wo009/review/`), 20 marks | `bundleRelease --dry-run` order confirmed; `bundleRelease` green with an AAB of 20 puzzles; content tests green; 19-mark and no-mini variants refused with named counts; stale AAB survived the refusal (S1) |

No emulator was used (per the brief).

## Handoff

- **Verdict: forward** (to Test & Verify). B 0 / S 3 / N 3.
- JVM: 351 tests, 6 red, exactly the expected `ReleaseSetKitTest` release legs, which go green honestly at 20 marks.
- S1: a stale AAB survives a refused gate; delete it in the build.
- S2: `release_install.sh` can uninstall the debug app from the owner's real phone; limit it to emulators.
- S3: `feature-graphic.png` and `.svg` are missing from `ASSET-LICENCES.md` (G-16).
- N1: the bar is triplicated by design; no drift guard.
- N2: `JsonSlurper` is more lenient than the kit's strict reader; not reachable today.
- N3: `REQ-039.A2` is vacuously green over the empty release; the real verdict is TASK-071's.
