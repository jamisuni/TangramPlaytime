# WO-009 code review, delta (S1, S2, S3) - code-reviewer, fresh

Tree: HEAD `f80ed88` plus the WO-009 working tree (product `6550e479342c`, tools `f1421752fb16`, the pause baseline). Verdict: **forward** (0 Blockers, 0 Shoulds, 2 Notes).

## Findings

| ID | Sev | Category | Target | Observation | Proposed resolution |
|---|---|---|---|---|---|
| N1 | Note | Release gate, residue | `app/build/intermediates/intermediary_bundle/release/packageReleaseBundle/intermediary-bundle.aab` | After a refused build the AGP intermediate AAB of the earlier green build is still on disk (seen in the scratch copy after the 19-mark refusal; it holds the earlier 22 puzzles, is unsigned, and sits in `intermediates/`). The decision row accepts this and `CHECKLIST.md` step 6 says to take the AAB only from a build that ended in BUILD SUCCESSFUL. Not a store-upload path. | None; keep the step 6 sentence. |
| N2 | Note | Documentation | `tools/release_install.sh` `--allow-real-device`; `release/CHECKLIST.md`, `release/README.md` | The emulator-only rule and the flag are documented in the script's header and `usage` only; neither release doc names the script. The guard itself is what protects, so this is cosmetic. | Optional: one line in `release/README.md` if the owner is ever to run the leg himself. |

## One line per fix

- **S1 - closed.** `:app:deleteReleaseBundle` (a `Delete` of `app/build/outputs/bundle/release`) is a dependency of `bundleRelease` and `packageReleaseBundle`, and the gate has `mustRunAfter` it. `bundleRelease --dry-run` lists the delete first and the gate before `packageReleaseBundle`. Scratch copy with 22 marks: build green, AAB with 22 puzzle files; unmark `shapes-mini-1` refused (item 4 "none") and no `outputs/bundle/release` left; re-mark gives the AAB again; unmark three (19 of 20) refused, no AAB. On the tree: I planted a dummy `app-release.aab` in the untracked output folder, `:app:bundleRelease` was refused with the five unmet items, and the folder was empty afterwards. The delete target is one fixed path under the module's `build/`; it cannot reach anything else. `assembleRelease` and `packageRelease --dry-run` list neither the delete nor the gate (0 matches). Nothing next to it weakened (N1 is the accepted residue).
- **S2 - closed.** `bash -n` OK; the script in the scratch copy is byte-identical to the tree's. Stub `adb` in a fake SDK, logging every call: phone serial, IP serial, empty serial and an unreadable device (`getprop` fails) each exit 2 with 0 zipalign, apksigner, install and uninstall calls (only the two `getprop` reads ran). `emulator-5554`, `ro.kernel.qemu=1` alone and `ro.boot.qemu=1` alone each pass the guard and continue (exit 1 only because the stub returns no permission dump). A phone with `--allow-real-device` prints the INFO line and continues. The guard sits before the scratch-dir creation, copy, signing, install and uninstall; only host and device reads come before it.
- **S3 - closed.** `## Store pictures` lists `feature-graphic.svg` and `.png`: made for this project, GPL-3.0, system sans-serif title rasterised by Chrome, no font file shipped (G-16, ADR-005). The files agree with the rows: SVG 1024 x 500 with DI-9's seven paths and background `#FBF7EE`; PNG IHDR 1024 x 500, depth 8, colour type 2. `release/pictures/` holds those two plus `icon-512.png`, which is listed under Launcher icon. `AssetLicencesKitTest` 16/0 and `SoundAssetsKitTest` 1/0 in run 2 (fresh XMLs); the new section is last, so the "up to the next `## `" rule loses nothing. `CHECKLIST.md` step 6 names the wizard's destination file and the BUILD SUCCESSFUL rule.

## What I ran

| Command | Result |
|---|---|
| JVM run 1 (`:app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun`), then run 2 (`--continue`, six modules, `--rerun`) | run 1 exit 0; run 2 exit 1 (expected). XMLs stamped 14:18: **351 tests, 0 skipped, 6 failures, 0 errors**, all `ReleaseSetKitTest` release legs (A1, load, mini, warm-ups, four categories, square) |
| `:app:bundleRelease` on the tree, with a planted stale file | refused at `:content:checkReleasePuzzles`, five unmet items named, no AAB afterwards |
| `bundleRelease --dry-run`; `assembleRelease packageRelease --dry-run` | delete first, gate before `packageReleaseBundle`; the other pair has neither |
| Repository copy in scratch, `mark_puzzles.py --folder <copy>/Tangrams` (22 marks) | green, refused, re-mark green, refused at 19 (above) |
| Stub adb, eight cases | above; logs in the scratch folder |

My first mark attempt ran after a mistyped `cd` with no ids; the tool refused, and `git status Tangrams` was empty and no puzzle file on the tree carries a mark. No emulator, no device, no signing, no edit of any reviewed file. Scratch: `C:/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/b3148999-a4ac-4936-a274-abc67af518f4/scratchpad/wo009/review-delta/`.
