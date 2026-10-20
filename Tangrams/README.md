# Tangrams: the puzzle library

The puzzles live here, separate from the requirements (`../Requirements/`) and the spec (`../Spec/`). Every file is one puzzle in the `tangram-puzzle/1` format (`../Spec/03-puzzle-format.md`); `puzzle.schema.json` is its structural schema.

| File | Puzzle | Kind | Difficulty | Notes |
|---|---|---|---|---|
| `shapes-mini-1.json` | Mini: Pyramid | mini | 1 | 3 pieces (square + 2 small triangles). For a fast first success and "solved" testing. |
| `shapes-mini-2.json` | Mini: Window | mini | 1 | 3 pieces (medium + 2 small triangles). |
| `shapes-warmup-1..4.json` | Warm-up 1–4 | warmup | 1 | Easy starters. At least half of every piece's outline lies on the silhouette edge (rule V12), so each piece's shape can be seen in the silhouette. Found by `tools/puzzle_search/easy_search.py`. |
| `things-house.json` | House | full | 2 | |
| `things-arrow.json` | Arrow | full | 2 | |
| `animals-cat.json` | Cat | full | 3 | |
| `shapes-rectangle.json` | Chocolate bar (rectangle) | full | 3 | The classic 2 : 1 rectangle, found by `tools/puzzle_search/rect_solver.py` |
| `vehicles-sailboat.json` | Sailboat | full | 3 | |
| `nature-mountain.json` | Mountain | full | 4 | |
| `shapes-square.json` | Gift box (the classic square) | full | 4 | Back in the library since round 6 (2026-09-28): it was removed in round 2 on the belief that V11 rejects it, which was wrong (see below). |

A puzzle does not have to depict anything: abstract shapes and plain rectangles are fine (owner, 2026-09-27). The ‹ › order in the game is by **kind** (mini, warmup, full), then by difficulty, then by id (REQ-040).

## Rules every puzzle must pass
```bash
python tools/validate_puzzles.py Tangrams                  # V1–V6, V8–V10, V12; V11 prints the build order
python tools/render_puzzle.py Tangrams Tangrams/previews   # silhouette | pieces | picture sheets
python tools/build_prototype.py                            # the prototype picks up every *.json here
```
- **V9** checks the piece count for the puzzle's `kind`: `full` and `warmup` use all seven, `mini` uses 1–6.
- **V12** checks the warm-up measure: every piece of a `warmup` exposes at least half of its outline on the silhouette edge.
- **V11 never fails a valid puzzle.** It prints the edge-first build order (each piece locks on an outline corner or on a corner of a piece placed before it). Such an order always exists: the uncovered region always has a convex corner, that corner is always an anchor, and the piece that covers it must have a vertex there. "No hidden anchors" (round 2) is a rule of the runtime lock, not of the content; a piece dropped in the middle with nothing to hold simply goes home.

`provenance.reviewedByHuman` stays `false` until a person has solved the puzzle and looked at its picture. The first release needs at least 20 reviewed puzzles over four themes, including the gift box (REQ-042).

## Imported figures (WO-010)

**93 more puzzles** come from the figures of `C:/GitHub/AI/TangramCollections` (design input DI-10): 12 of the 13 in `convex-13` (CC0-1.0) and 81 of the 131 in `android-tangram-131` (GPL-3.0, the app's own licence). With the 13 above, the folder holds 106 puzzles. Each figure's collection, source id, source name and licence are in [`../ASSET-LICENCES.md`](../ASSET-LICENCES.md), with each collection's credit.

- **How they were made:** `tools/import_figures.py` (WO-010 TASK-036; `tasks.md` § WO-010 › Seams S-38, S-39). Each coordinate is snapped to an exact value a + b√2 and scaled onto this format's 4 × 4 square. Every piece's turn is found exactly, and the validator and the app's load check decide what passes. The collection's own numbers are never trusted. The titles, ids and Finnish titles are in the script's tables; every title and title edit is in `../review-notes.md` (the WO-010 titles row).
- **To make them again:** the converter takes every top-level `Tangrams/*.json` except the schema as a shipped puzzle. On this folder as it is now, it takes the 93 imports for shipped puzzles and imports none. It reproduces this import only on a copy of the repository whose `Tangrams/` holds the 13 puzzle files of commit `2ccea13` and `puzzle.schema.json`, nothing more (each from `git show 2ccea13:Tangrams/<file>`), and whose `tools/import/` holds no `batch-*.txt` (the converter never overwrites a file; it stops). In that copy, run `python tools/import_figures.py import --convex C:/GitHub/AI/TangramCollections/collections/convex-13/figures.json --android C:/GitHub/AI/TangramCollections/collections/android-tangram-131/figures.json --report <a file outside the copy>`; add `--dry-run` to write only the report. It accounts for the same 144 figures (93 imported, 51 skipped) and writes the same 93 files and four batch lists as here, except `art`: it writes the stub, and each picture was drawn afterwards by its batch.
- **Every import** is `kind: full`, so the list still opens with the two minis and the four warm-ups. Its `difficulty` comes from the measured rule (the more of every piece's outline shows on the silhouette's edge, the easier). It has `provenance.reviewedByHuman: false`. Its picture is the one-colour stub `{"base": "#D9DCE8", "shapes": []}` until its batch draws it (`../tools/import/batch-1.txt` … `batch-4.txt`).
- **Ratings of the 93:** 18 of rating 1, 20 of rating 2, 23 of rating 3, 10 of rating 4, 22 of rating 5.

**All 144 figures are accounted for: 93 imported + 51 skipped = 144.** Skipped, by reason:

| Reason | Count |
|---|---|
| **V5 not edge-connected:** two pieces touch only at a corner, which REQ-038 does not count as connected | 25 |
| **No clean snap:** a coordinate has no exact value a + b√2 in quarters. In 13 of them a piece is turned 15°, off TYPE-003's 45° turns. In the other 4 (Pram, Penguin, Begging, Dancing Lady) every piece is on a 45° turn, but a piece sits off the quarter grid (thirds and twelfths) | 17 |
| **Duplicate:** the same silhouette as a puzzle already here, only shifted (the shipped 13 come first, then `convex-13`, then `android-tangram-131`, each in file order) | 9 |
| **Total skipped** | **51** |

No figure was skipped for a piece that has no 45° pose once snapped, and none for an overlap. No figure was reshaped to pass: both rules that turn figures away are locked (REQ-038, TYPE-003).

| Collection | Source id | Name | Reason |
|---|---|---|---|
| convex-13 | `convex-03` | Convex 3 | duplicate of `shapes-rectangle` |
| android-tangram-131 | `root-square` | Square | duplicate of `shapes-square` |
| android-tangram-131 | `geometric-elongated-diamond` | Elongated Diamond | duplicate of `shapes-convex-06` |
| android-tangram-131 | `geometric-right-isosceles-triangle` | Right Isosceles Triangle | duplicate of `shapes-convex-13` |
| android-tangram-131 | `geometric-right-angled-pentagon` | Right-angled Pentagon | duplicate of `shapes-convex-11` |
| android-tangram-131 | `geometric-2truncated-square` | Square Truncated on 2 Corners | duplicate of `shapes-convex-08` |
| android-tangram-131 | `geometric-truncated-diamond` | Truncated Diamond | duplicate of `shapes-convex-10` |
| android-tangram-131 | `geometric-wedge` | Wedge | duplicate of `shapes-convex-02` |
| android-tangram-131 | `objects-balance` | Balance | V5 not edge-connected |
| android-tangram-131 | `objects-candle` | Candle | no clean snap (PG value 1.134: 0 pairs; PG lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `objects-helicopter` | Helicopter | V5 not edge-connected |
| android-tangram-131 | `objects-pram` | Pram | no clean snap (ST1 value 3.357: 0 pairs; every piece on a 45° turn, a position off the quarter grid) |
| android-tangram-131 | `objects-sailboat` | Sailboat | V5 not edge-connected |
| android-tangram-131 | `objects-teapot` | Teapot | V5 not edge-connected |
| android-tangram-131 | `objects-watering-can` | Watering Can | V5 not edge-connected |
| android-tangram-131 | `animals-cat` | Cat | no clean snap (ST1 value 1.5249: 0 pairs; PG lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `animals-dog` | Dog | V5 not edge-connected |
| android-tangram-131 | `animals-fish` | Fish | V5 not edge-connected |
| android-tangram-131 | `animals-horse` | Horse | V5 not edge-connected |
| android-tangram-131 | `animals-penguin` | Penguin | no clean snap (ST2 value 2.4107: 0 pairs; every piece on a 45° turn, a position off the quarter grid) |
| android-tangram-131 | `animals-pigeon` | Pigeon | V5 not edge-connected |
| android-tangram-131 | `animals-rabbit` | Rabbit | no clean snap (LT1 value 5.8978: 0 pairs; PG lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `people-begging` | Begging | no clean snap (LT1 value 3.6534: 0 pairs; every piece on a 45° turn, a position off the quarter grid) |
| android-tangram-131 | `people-dancer` | Dancer | V5 not edge-connected |
| android-tangram-131 | `people-dancing-lady` | Dancing Lady | no clean snap (ST1 value 0.3333: 0 pairs; every piece on a 45° turn, a position off the quarter grid) |
| android-tangram-131 | `people-falling` | Falling | V5 not edge-connected |
| android-tangram-131 | `people-horse-rider` | Horse Rider | V5 not edge-connected |
| android-tangram-131 | `people-jumping` | Jumping | no clean snap (SQ value 1.7588: 0 pairs; SQ lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `people-kid-and-umbrella` | Kid and Umbrella | no clean snap (LT1 value 2.4495: 0 pairs; LT1 lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `people-man-in-a-boat` | Man in a Boat | no clean snap (LT1 value 2.3801: 0 pairs; SQ lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `people-reading` | Reading | no clean snap (LT1 value 3.6168: 0 pairs; LT2 lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `people-resting` | Resting | no clean snap (LT1 value 2.7802: 0 pairs; SQ lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `people-sitting` | Sitting | no clean snap (LT1 value 5.3801: 0 pairs; SQ lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `people-skater` | Skater | V5 not edge-connected |
| android-tangram-131 | `people-waiter` | Waiter | V5 not edge-connected |
| android-tangram-131 | `people-walking` | Walking | V5 not edge-connected |
| android-tangram-131 | `christmas-caroler` | Caroler | no clean snap (SQ value 3.3801: 0 pairs; MT lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `christmas-fairy` | Fairy | no clean snap (PG value 1.9267: 0 pairs; PG lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `christmas-person-with-gift` | Person with Gift | V5 not edge-connected |
| android-tangram-131 | `christmas-present-with-bow` | Present with Bow | no clean snap (PG value 3.366: 0 pairs; MT lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `christmas-sled` | Sled | V5 not edge-connected |
| android-tangram-131 | `christmas-sleigh` | Sleigh | V5 not edge-connected |
| android-tangram-131 | `christmas-snowman` | Snowman | V5 not edge-connected |
| android-tangram-131 | `christmas-tree` | Tree | V5 not edge-connected |
| android-tangram-131 | `christmas-votive-candle` | Votive Candle | no clean snap (PG value 1.634: 0 pairs; PG lies 15° off TYPE-003's 45° turns) |
| android-tangram-131 | `numbers-8` | Number 8 | V5 not edge-connected |
| android-tangram-131 | `alphabet-B` | Letter B | V5 not edge-connected |
| android-tangram-131 | `alphabet-O` | Letter O | duplicate of `numbers-0` |
| android-tangram-131 | `alphabet-R` | Letter R | V5 not edge-connected |
| android-tangram-131 | `alphabet-X` | Letter X | V5 not edge-connected |
| android-tangram-131 | `icons-pen` | Pen | V5 not edge-connected |

**Turned copies, kept** (WO-010 checkpoint item 3's default: a turned copy looks different on the board, so it ships as its own puzzle, marked here so it can be dropped when the launch set is chosen). Each has the same silhouette as an earlier puzzle, only turned or mirrored:

- `shapes-convex-09` (Diamond) is the Gift box (`shapes-square`) turned 45°.
- `shapes-convex-13` (Triangle) is the Mountain (`nature-mountain`) turned.
- `shapes-parallelogram` (Parallelogram) is the Standing parallelogram (`shapes-convex-04`) turned.
- `shapes-trapezoid` (Trapezoid) is the Standing trapezoid (`shapes-convex-07`) turned.
- `letters-v` (Letter V) is the Chevron (`shapes-chevron`) turned.
- `letters-w` (Letter W) is Letter M (`letters-m`) turned.

## Later
Non-traditional modes with other piece sets are planned (owner, 2026-09-27). The format will then gain a `pieceSet` field, and V9 will become "uses every piece of its set".
