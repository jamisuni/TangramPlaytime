# Review — code · WO-010

**Date:** 2026-10-19  ·  **Reviewer:** fresh context (Sonnet 5.5)
**Inputs:** working tree against `2ccea13`: `tools/import_figures.py`, `tools/tests/*`, the 93 `Tangrams/*.json` and `tools/import/batch-*.txt`, `Tangrams/README.md`, `ASSET-LICENCES.md`, `PlayTexts.kt`, `AssetLicencesKitTest.kt`, `PuzzleLoaderKitTest.kt` (diff), `ScaleMeasureTest.kt`; `tasks.md` § WO-010 (S-38…S-43, S-11Δ2, Q2/Q7/Q9), REQ-038/040/041, `Tangrams/puzzle.schema.json`, G-16 v1.2; the collections' `NOTICE.md` and `figures.json` (read only).

## Verdict

**forward** — the converter, the 93 files, the record and the one-line product fix re-derive from the sources; nothing stands as a Blocker. Two Shoulds (a tool that cannot reproduce its own output on the tree it produced; a build gap under a new pin).
0 Blockers, 2 Shoulds, 4 Notes.

## Checklist applied

- [x] **Directives** — D2/D4: nothing built beyond the seams; the converter's `show` and `colours` are named in S-38/S-42.
- [x] **Guardrails** — G-05 (bundled files, schema untouched), G-16 v1.2 (record complete, see below), G-12/G-07 not touched.
- [x] **Contract** — no `I*` changed; `git diff 2ccea13 --name-status -- Tangrams` is 93 `A` plus `README.md` `M`; no shipped puzzle file, id, rating or sheet changed (seam note 1).
- [x] **Scope** — everything traces to S-38…S-43, S-11Δ2.
- [x] **Hard-stops** — none touched.
- [x] **Evidence re-derived** — see "Re-derived" below.
- [x] **The conceptual 20 %** — the snap grid, the duplicate rule, the rating rule, the reproducibility of the tool.

## Findings

### Blockers
None.

### Shoulds

**S1 — The converter cannot reproduce its own output on the tree it produced (product/tool defect).** `tools/import_figures.py` `shipped_puzzles()` reads every `Tangrams/*.json`, so the "shipped 13" of S-38 step 6 is whatever the folder holds. Re-running the specified dry run now (`import --convex … --android … --dry-run --report <scratch>`) gives "imported 0, skipped 144 (102 duplicate, 25 V5, 17 no snap)" and 79 "problems", because the 93 imports are taken as shipped. S-38 says the tool "stays in the repo as the record of how each file was made"; a record that only runs on a tree that no longer exists is not one, and the README's "How they were made" does not say the 13-file tree is a precondition. A reader (or the next import of another collection) gets a false picture, and the never-overwrite stop is never reached. Fix: an explicit shipped list (for example `--shipped <ids>` or the `this project` rows of `ASSET-LICENCES.md`), or one README line giving the recipe (`git` tree of `2ccea13`'s 13 files). I re-derived on a scratch tree of those 13 files: see "Re-derived". No data defect follows.

**S2 — `ASSET-LICENCES.md` is read by a pin but is not an input of the test task (build defect, `content/build.gradle.kts`; not a kit defect).** `AssetLicencesKitTest` reads the record from the file system at run time. `:content:testDebugUnitTest` declares the bundled `Tangrams/*.json` (through `bundlePuzzles`) and nothing else, so a change to the record alone (a row removed, a licence cell edited) leaves the task UP-TO-DATE and the old green stands, until someone passes `--rerun`. This is from the build file, I did not run it. The decision R5 gives for the pin is "it runs on every later JVM line, so a puzzle added later without its row turns the build red": that holds for an added puzzle (the bundle changes) and not for an edit of the record. The two-run line of the channel carries `--rerun`, so this WO's verdict is sound, and the carry as it stands is acceptable for this close. For the next WO: one line in `content/build.gradle.kts`, `tasks.withType<Test>().configureEach { inputs.file(rootProject.file("ASSET-LICENCES.md")) }`, or a note in the run log that the record is verified only with `--rerun`.

### Notes

**N1 — Skip labels do not match S-38 step 4.** The 13 figures with a piece turned 15° are reported "no clean snap (… lies 15° off TYPE-003's 45° turns)" (their coordinates cannot snap), so the reason "a piece off TYPE-003's turns" has count 0. `Tangrams/README.md` is honest about it (13 + 4) and the totals (25 / 17 / 9 = 51; 93 + 51 = 144) re-derive. A wording match with S-38 and the planner's probe (14 / 2), no effect on any file. I also tried the snap at twelfths for the other four (Pram, Penguin, Begging, Dancing Lady): two then snap but fail V3 (a piece not congruent), two still have two pairs. The quarter grid is not what loses them.

**N2 — The licence chain is credited, with one omission.** `ASSET-LICENCES.md` states the upstream's own GPL-3.0 claim and the myhomeschoolmath.com worksheet origin of the Android figures (from its `NOTICE.md`); the convex-13 `NOTICE.md` also says one line of the drawing program was corrected (shape 10), which the record leaves out. Not a G-16 breach (the record lists collection, source figure id, licence; the credit is per collection by S-41). Owner's awareness only.

**N3 — Pictures lean on the clip.** 33 of the 93 draw beyond their silhouette's bounding box, 6 by more than 2 units, 4 by more than 5 (up to 23.5 units, `shapes-convex-07`'s ground polygon; `numbers-1`'s rays). The screen clips, and REQ-039.A1's device drawing test is the only guard; my check here is a bounding-box count, not a verdict. The batch brief's "inside the outline" is read as "as seen".

**N4 — Finnish reference titles.** `Pinwheel` → `Tuulimylly` (a windmill; the toy is usually `tuulipyörä`), `Standing` → `Seisomassa`. G-10: reference only, never tested; the owner's review row.

## Re-derived

| Claim | Result |
|---|---|
| `PYTHONUTF8=1 python tools/validate_puzzles.py Tangrams` | 106/106 valid |
| `python tools/tests/test_import_figures.py` | ok, 9 tests. Four mutations on a scratch copy (rating step 0.52 to 0.53; turned-copy branch removed; scale `2q` to `2q+1`; snap tolerance 2e-4 to 2e-2) each make it fail: it can fail |
| `python tools/tests/test_tangram_geom.py` | ok, 13 pinned files, 106 in the folder; each pinned file keeps its corner set |
| `python tools/import_figures.py colours <93 ids of batch-1..4>` | 93/93 ok; colour counts 3 (14), 4 (24), 5 (23), 6 (17), 7 (14), 8 (1) |
| Converter on a scratch tree of the 13 files of `2ccea13` (shipped 13 from `git show`) | 144 figures: 93 imported, 51 skipped. Every non-`art` field of all 93 written files equals the files in the tree; the four `tools/import/batch-*.txt` equal (26/22/25/20); all 93 pictures are drawn (none is the stub) |
| `--dry-run --report <scratch>` writes nothing in the tree | `git status --short` identical before and after |
| Geometry independent of the converter | Each imported piece's corner set equals the source's corners times √2: 651 pieces, worst error 6.7e-5 units |
| S-39 per file (own script, 93 files) | `id` equals file name, kebab, no `kit` segment, never a shipped id, prefix equals `category`; `kind` full; `provenance` exactly the two fields; top-level keys exactly the schema's; entries in TYPE-001 order; no BOM, no CR; every `title.en` unique over the 106 (and every `title.fi`) |
| `difficulty` | Recomputed per file from V12's measure (1 − shared edge / perimeter, mean over seven pieces) with the S-39 steps: 93/93 equal. Distribution 18 / 20 / 23 / 10 / 22 equals the README. Nearest to a step: `vehicles-boat` at 0.4004, no float boundary risk |
| `art` grammar (S-42) | Shape types and keys within the schema's, colours `#RRGGBB`, `opacity` 0–1, `width` > 0, polygons ≥ 3 points, strokes on `line` and `path`, `path.d` commands M/L/Q/C/Z with their number counts, starting with M: no violation |
| Duplicates and turned copies | Independent raster comparison of all 106 silhouettes over the 8 turns and the mirror: no remaining pair equal after a translation; the six turned copies the README names are equal after a turn or mirror (five at once, `letters-w`/`letters-m` at 3.1 % against my 3 % raster cut-off, and the converter's exact test says equal) |
| `ASSET-LICENCES.md` | `## Puzzle figures` equals the converter's report rows exactly (106 rows, sorted, figure and licence pairs); ids equal the folder's file list; credits equal the two `NOTICE.md` |
| Seam note 1 | `git diff 2ccea13 --name-status -- Tangrams`: 93 `A` and `README.md` `M` only; sheets: 93 new `Tangrams/previews/<id>.svg`, one per batch id |
| S-11Δ2 | `PlayTexts.kt` one line, `counterFormat.format(java.util.Locale.ROOT, number, count)`; no other `format` in `play`, `app`, `store`, `content` main code |
| `ScaleMeasureTest.kt` | Header "measurement … no token; it asserts only liveness"; no `REQ-` token in the file; three tests assert only that the screen, the controller and the grid exist and that the class leaves the shown puzzle as it found it; it prints `measure:` lines and never compares a number |
| `AssetLicencesKitTest.kt` | Reviewed for what it asserts: each crafted record breaks one check and `kitAssertOnly` demands that check and that id only; a control record passes; the colour fixtures include the stub, 2, 3, 8 (case-insensitive), 9. It can fail |
| JVM line (1) `:app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun` | BUILD SUCCESSFUL |
| JVM line (2) `:content … :play … :kernel :contracts :store :app` `--rerun` | BUILD SUCCESSFUL, 0 failures, 0 skipped: content 80, play 107, kernel 56, contracts 2, store 12, app 4; `AssetLicencesKitTest` result stamped 19:20 (this run) |

Not run: any emulator or device (TASK-042's), the human look at the sheets (WO-009).

Nothing was changed in the project but this file; the scratch work is under `C:/Users/Jami/AppData/Local/Temp/claude/C--GitHub-AI-TangramPlaytime/ad59e2e5-260d-41b7-9f8e-74a13697064c/scratchpad/wo010/review/`.

## Handoff

- **To:** orchestrator
- **Verdict:** forward
- **Counts:** 0 B / 2 S / 4 N
- **Product vs test:** S1 product (tool, `tools/import_figures.py`, S-38 intro); S2 build file (`content/build.gradle.kts`), not a kit defect; no test or fixture defect found.
