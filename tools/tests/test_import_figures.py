"""
Tool test for tools/import_figures.py (WO-010 TASK-036; seams S-38 ... S-42). The implementer's self-check, not a
verdict: the verdicts are the validator and the Kotlin load check (decisions.md 2026-10-19 R1).

Plain asserts, stdlib only. Run from the project root:  python tools/tests/test_import_figures.py
It reads the shipped Tangrams/*.json files and writes only into a temporary folder.
"""
import json
import math
import sys
import tempfile
from fractions import Fraction as F
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))

import import_figures as m  # noqa: E402
from tangram_geom import COS, Q2, SIN, piece_polygon, placement_from_polygon  # noqa: E402
from validate_puzzles import load_solution, validate  # noqa: E402

SQRT2 = math.sqrt(2)

# Convex 10 of TangramCollections' README (collection units: small-triangle legs 1, y down), hand-copied.
CONVEX_10 = {"id": "convex-10", "name": "Convex 10", "pieces": [
    {"type": "L", "points": [[1, 2], [1, 0], [3, 2]]},
    {"type": "L", "points": [[1, 0], [3, 0], [3, 2]]},
    {"type": "M", "points": [[0, 2], [2, 2], [1, 3]]},
    {"type": "S", "points": [[0, 3], [0, 2], [1, 3]]},
    {"type": "S", "points": [[0, 1], [1, 0], [1, 1]]},
    {"type": "Q", "points": [[0, 2], [0, 1], [1, 1], [1, 2]]},
    {"type": "P", "points": [[1, 3], [2, 2], [3, 2], [2, 3]], "mirrored": False}]}


def shipped(name):
    puzzle = json.loads((ROOT / "Tangrams" / f"{name}.json").read_text(encoding="utf-8"))
    return puzzle, load_solution(puzzle, [])


def moved(polys, rot=0, mirror=False, dx=Q2(0), dy=Q2(0)):
    out = {}
    for pid, poly in polys.items():
        pts = []
        for x, y in poly:
            if mirror:
                x = -x
            pts.append((x * COS[rot] - y * SIN[rot] + dx, x * SIN[rot] + y * COS[rot] + dy))
        out[pid] = pts
    return out


def test_snap():
    # S-38 step 2: the one pair of quarters within 2e-4 (four decimals in the files).
    assert m.snap(1.4142) == (F(0), F(1))
    assert m.snap(0.7071) == (F(0), F(1, 2))
    assert m.snap(2.1213) == (F(0), F(3, 2))
    assert m.snap(3.0) == (F(3), F(0))
    assert m.snap(2.4142) == (F(1), F(1))
    assert m.snap(0.0) == (F(0), F(0))
    assert m.snap(1.0607) == (F(0), F(3, 4))
    # No pair: a third (the Dancing Lady's 0.3333) is not on the quarter lattice.
    assert m.snap(0.3333) is None and m.snap_candidates(0.3333) == []
    # Off by more than 2e-4 from 2.4142135...: no pair.
    assert m.snap(2.4146) is None
    # Every candidate really is within the bound, and quarters only.
    for v in (0.1, 1.7071, 4.8284, 9.5355):
        for p, q in m.snap_candidates(v):
            assert abs(float(p) + float(q) * SQRT2 - v) <= 2e-4
            assert (p * 4).denominator == 1 and (q * 4).denominator == 1


def test_scale():
    # S-38 step 3: sqrt2 * (p + q*sqrt2) = 2q + p*sqrt2, written [2q, p], or a plain number when p = 0.
    assert m.scaled((F(0), F(1))).to_json() == 2
    assert m.scaled((F(0), F(1, 2))).to_json() == 1
    assert m.scaled((F(1), F(1))).to_json() == [2, 1]
    assert m.scaled((F(3), F(0))).to_json() == [0, 3]
    assert m.scaled((F(1, 4), F(0))).to_json() == [0, 0.25]


def test_convert_maps_letters_and_keeps_the_local_corner_order():
    polys, reason = m.convert(CONVEX_10)
    assert reason is None, reason
    assert list(polys) == m.TYPE001_ORDER                       # TYPE-001 order
    # The first L is LT1, the second LT2; each is written as piece_polygon of its exact pose.
    for pid, poly in polys.items():
        rot, flip, at = placement_from_polygon(pid, poly)
        assert poly == piece_polygon(pid, rot, flip, at), pid
    # Integer collection points become pure sqrt2 multiples: (1, 2) -> (sqrt2, 2*sqrt2).
    lt1 = sorted((json.dumps(x.to_json()), json.dumps(y.to_json())) for x, y in polys["LT1"])
    assert lt1 == sorted([("[0, 1]", "[0, 2]"), ("[0, 1]", "0"), ("[0, 3]", "[0, 2]")]), lt1
    # The draft passes the validator (S-38 step 5) with S-39's fields.
    draft = m.draft_of(polys, "shapes-test-convex-10", "Cut square", "Leikattu neliö", "shapes", 5)
    errors, info = validate(draft)
    assert not errors, errors
    assert draft["kind"] == "full" and draft["art"] == m.STUB_ART
    assert draft["provenance"] == {"author": "ai:claude", "reviewedByHuman": False}


def test_convert_reasons():
    turned = json.loads(json.dumps(CONVEX_10))
    turned["pieces"][4]["points"] = [[0.3333, 1], [1.3333, 0], [1.3333, 1]]   # shifted by a third: off the grid
    polys, reason = m.convert(turned)
    assert polys is None and reason.startswith("no clean snap (ST2 value 0.3333")
    assert "a position off the quarter grid" in reason
    # A small triangle turned 15 degrees: its corners are not in Q(sqrt2), so the snap turns it away.
    c, s = math.cos(math.radians(15)), math.sin(math.radians(15))
    tri = [(0.0, 0.0), (1.0, 0.0), (0.5, 0.5)]
    rotated = json.loads(json.dumps(CONVEX_10))
    rotated["pieces"][4]["points"] = [[round(3 + x * c - y * s, 4), round(3 + x * s + y * c, 4)] for x, y in tri]
    polys, reason = m.convert(rotated)
    assert polys is None and reason.startswith("no clean snap") and "15° off TYPE-003's 45° turns" in reason
    # A piece on the lattice but not congruent to its piece: no pose (V3).
    wrong = json.loads(json.dumps(CONVEX_10))
    wrong["pieces"][3]["points"] = [[0, 3], [0, 2], [2, 3]]
    polys, reason = m.convert(wrong)
    assert polys is None and reason == "a piece off TYPE-003's turns (V3: ST1)", reason


def test_silhouette_same_figure():
    _, arrow = shipped("things-arrow")
    keys = m.silhouette_keys(arrow)
    assert m.same_figure(keys, keys[(0, False)]) == "translation"
    shifted = moved(arrow, dx=Q2(3, 1), dy=Q2(-2, F(1, 2)))
    assert m.same_figure(m.silhouette_keys(shifted), keys[(0, False)]) == "translation"
    for rot in range(1, 8):
        assert m.same_figure(m.silhouette_keys(moved(arrow, rot=rot)), keys[(0, False)]) == "turned", rot
    assert m.same_figure(m.silhouette_keys(moved(arrow, mirror=True)), keys[(0, False)]) == "turned"
    _, cat = shipped("animals-cat")
    assert m.same_figure(m.silhouette_keys(cat), keys[(0, False)]) is None
    # Another dissection of the same outline is the same figure (the silhouette, never the solution): the Gift
    # box's pieces mirrored, or turned by 90 degrees, lie elsewhere, and the square is its own mirror and turn.
    _, square = shipped("shapes-square")
    for other in (moved(square, mirror=True), moved(square, rot=2)):
        assert {k: set(v) for k, v in other.items()} != {k: set(v) for k, v in square.items()}
        assert m.same_figure(m.silhouette_keys(other), m.silhouette_keys(square)[(0, False)]) == "translation"
    # Merged outline: the square's outline is its four sides, however many piece edges lie on them.
    assert len(m.silhouette_segments(square)) == 4
    # Turned 45 degrees, it is a turned copy (Convex 9 against the Gift box).
    assert m.same_figure(m.silhouette_keys(moved(square, rot=1)), m.silhouette_keys(square)[(0, False)]) == "turned"


def test_fields():
    assert m.slug("Tree (short)") == "tree-short"
    assert m.slug("Little house") == "little-house"
    assert m.source_slug("android-tangram-131", {"id": "alphabet-A"}) == "a"
    assert m.source_slug("android-tangram-131", {"id": "aminals-bird"}) == "bird"
    assert m.source_slug("android-tangram-131", {"id": "geometric-2truncated-square"}) == "2truncated-square"
    assert m.source_slug("convex-13", {"id": "convex-01"}) == "convex-01"
    cat = lambda name, group: m.category_of("android-tangram-131", {"name": name, "category": group})  # noqa: E731
    assert cat("Boat", "objects") == "vehicles" and cat("Sled", "Christmas") == "vehicles"
    assert cat("Star", "icons") == "nature" and cat("Tree (short)", "Christmas") == "nature"
    assert cat("Caroler", "Christmas") == "people" and cat("Snowman", "Christmas") == "people"
    assert cat("Key", "objects") == "things" and cat("Folder", "icons") == "things"
    assert cat("Letter A", "alphabet") == "letters" and cat("Number 0", "numbers") == "numbers"
    assert cat("Square", "base") == "shapes" and cat("Cup", "geometric") == "shapes"
    assert m.category_of("convex-13", {"id": "convex-01", "name": "Convex 1"}) == "shapes"
    # REQ-040's rating from E (S-39).
    assert [m.rating_of(e) for e in (0.9, 0.56, 0.5599, 0.52, 0.5199, 0.47, 0.4699, 0.40, 0.3999)] == \
        [1, 1, 2, 2, 3, 3, 4, 4, 5]
    # E is the mean V12 exposure: every shipped warm-up has a piece at exactly 0.5 (decisions.md 2026-10-11).
    _, w1 = shipped("shapes-warmup-1")
    assert abs(min(m.exposures(w1).values()) - 0.5) < 1e-9
    assert m.fi_title("alphabet-Q", "Letter Q", {}) == "Kirjain Q"
    assert m.fi_title("numbers-7", "Number 7", {}) == "Numero 7"
    assert m.fi_title("x", "Cup", {}) is None
    assert m.BATCH_OF_CATEGORY == {"shapes": 1, "numbers": 1, "letters": 2, "things": 3, "vehicles": 3,
                                   "animals": 4, "nature": 4, "people": 4}


def test_layout_is_the_shipped_layout():
    for name in ("animals-cat", "nature-mountain", "shapes-square", "vehicles-sailboat"):
        text = (ROOT / "Tangrams" / f"{name}.json").read_text(encoding="utf-8")
        assert m.format_puzzle(json.loads(text)) == text, name
    polys, _ = m.convert(CONVEX_10)
    text = m.format_puzzle(m.draft_of(polys, "shapes-x", "Cut square", "Leikattu neliö", "shapes", 5))
    assert '    "shapes": []\n' in text and "\r" not in text and text.endswith("}\n")
    assert '"fi": "Leikattu neliö"' in text                      # UTF-8, not \u escapes


def test_never_overwrites():
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "a.json"
        m.write_text_new(path, "one\n")
        assert path.read_bytes() == b"one\n"
        try:
            m.write_text_new(path, "two\n")
            raise AssertionError("an existing file was overwritten")
        except FileExistsError:
            pass
        assert path.read_bytes() == b"one\n"


def test_colours():
    assert m.colour_count({"base": "#d9dce8", "shapes": []}) == 1 and m.is_stub({"base": "#d9dce8", "shapes": []})
    art = {"base": "#112233", "shapes": [{"type": "circle", "c": [0, 0], "r": 1, "fill": "#AABBCC"},
                                         {"type": "line", "from": [0, 0], "to": [1, 1], "stroke": "#aabbcc"},
                                         {"type": "rect", "x": 0, "y": 0, "w": 1, "h": 1, "fill": "#445566",
                                          "stroke": "#112233"}]}
    assert m.colour_count(art) == 3 and not m.is_stub(art)
    with tempfile.TemporaryDirectory() as tmp:
        files = []
        for name, a in (("stub", m.STUB_ART), ("three", art),
                        ("nine", {"base": "#000000", "shapes": [{"type": "circle", "c": [0, 0], "r": 1,
                                                                 "fill": f"#00000{k}"} for k in range(1, 9)]})):
            p = Path(tmp) / f"{name}.json"
            p.write_text(json.dumps({"art": a}), encoding="utf-8")
            files.append(str(p))
        assert m.cmd_colours([files[1]]) == 0
        assert m.cmd_colours([files[0]]) == 1
        assert m.cmd_colours([files[2]]) == 1


def main():
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_") and callable(v)]
    for t in tests:
        t()
    print(f"ok: {len(tests)} import tool tests")


if __name__ == "__main__":
    main()
