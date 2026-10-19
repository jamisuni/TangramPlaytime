"""
Tool test for tools/tangram_geom.outline_corners (WO-002 TASK-007).

Plain asserts, stdlib only. Run from the project root:  python tools/tests/test_tangram_geom.py

The corner rule classifies a vertex by the contiguity of the covered directions (the kernel's rule).
PRE_FIX holds, as data, the corner set every file had under the old angle-sum rule. Each corner is
"a,b,c,d" meaning (a + b*sqrt2, c + d*sqrt2). Only shapes-warmup-4 may differ: it gains (2,2).
"""
import json
import sys
from fractions import Fraction as F
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))

from tangram_geom import Q2, outline_corners  # noqa: E402
from validate_puzzles import load_solution  # noqa: E402

PRE_FIX = {
    "animals-cat": "0,0,4,0 0,0,6,0 2,0,0,0 2,0,2,0 2,0,4,0 3,0,1,0 3,0,3,0 4,0,0,0 4,0,4,0 4,0,6,0 5,0,3,0 5,0,5,0",
    "nature-mountain": "0,0,4,0 4,0,0,0 8,0,4,0",
    "shapes-mini-1": "0,0,2,0 2,0,0,0 4,0,2,0",
    "shapes-mini-2": "0,0,0,0 0,0,2,0 2,0,0,0 2,0,2,0",
    "shapes-rectangle": "0,0,0,0 0,0,0,2 0,4,0,0 0,4,0,2",
    "shapes-square": "0,0,0,0 0,0,4,0 4,0,0,0 4,0,4,0",
    "shapes-warmup-1": "0,0,0,0 0,0,2,0 0,0,4,0 0,0,6,0 1,0,1,0 1,0,3,0 1,0,5,0 2,0,2,0 2,0,4,0 2,0,6,0 3,0,1,0 3,0,3,0 4,0,0,0 4,0,2,0 4,0,4,0 5,0,5,0",
    "shapes-warmup-2": "0,0,6,0 1,0,1,0 1,0,5,0 2,0,2,0 2,0,4,0 3,0,1,0 4,0,0,0 4,0,2,0 4,0,4,0 4,0,6,0 5,0,1,0 5,0,5,0",
    "shapes-warmup-3": "0,0,1,0 0,0,3,0 0,0,5,0 1,0,0,0 1,0,4,0 2,0,1,0 2,0,3,0 2,0,5,0 3,0,2,0 4,0,1,0 4,0,3,0 5,0,0,0 5,0,2,0 5,0,4,0 6,0,3,0",
    "shapes-warmup-4": "0,0,4,0 0,0,6,0 1,0,5,0 2,0,0,0 2,0,4,0 3,0,3,0 3,0,5,0 4,0,2,0 4,0,4,0 4,0,6,0 5,0,3,0 6,0,2,0 6,0,4,0",
    "things-arrow": "-2,0,1,0 -1,0,0,0 -1,0,2,0 -1,0,4,0 0,0,1,0 0,0,3,0 4,0,0,0 4,0,1,0 4,0,3,0 4,0,4,0 6,0,2,0",
    "things-house": "0,0,2,0 0,0,4,0 2,0,0,0 3,0,-1,0 3,0,1,0 4,0,0,0 4,0,4,-1 4,1,4,-1 4,1,4,0",
    "vehicles-sailboat": "0,-5/2,0,2 0,-2,0,2 0,-3/2,0,3 0,0,0,0 0,3/2,0,3 0,2,0,2 0,5/2,0,2",
}
# The one intended change: 180 degrees covered in two separate wedges is a corner.
ADDED = {"shapes-warmup-4": ["2,0,2,0"]}


def parse_corner(text):
    a, b, c, d = (F(t) for t in text.split(","))
    return (Q2(a, b), Q2(c, d))


def corner_set(name):
    puzzle = json.loads((ROOT / "Tangrams" / f"{name}.json").read_text(encoding="utf-8"))
    errors = []
    polys = load_solution(puzzle, errors)
    assert not errors, (name, errors)
    return set(outline_corners(polys))


def main():
    # WO-010 TASK-036 (tasks.md § WO-010, the Q7 table): the folder grows with imported puzzles, so the pin is
    # narrowed to its 13 pinned names. Each must still be a puzzle file, and each keeps its pinned corner set.
    files = sorted(p.stem for p in (ROOT / "Tangrams").glob("*.json") if "schema" not in p.name)
    pinned = sorted(PRE_FIX)
    missing = [name for name in pinned if name not in files]
    assert not missing, f"pinned puzzle files missing from Tangrams: {missing}"
    for name in pinned:
        expected = {parse_corner(t) for t in PRE_FIX[name].split()}
        expected |= {parse_corner(t) for t in ADDED.get(name, [])}
        got = corner_set(name)
        assert got == expected, f"{name}: extra={sorted(map(repr, got - expected))} missing={sorted(map(repr, expected - got))}"
    w4 = corner_set("shapes-warmup-4")
    assert (Q2(2), Q2(2)) in w4, "warmup-4 (2,2) must be a corner"
    print(f"ok: corner sets match for the {len(pinned)} pinned files ({len(files)} puzzle files in Tangrams); "
          f"warmup-4 gains exactly (2,2)")


if __name__ == "__main__":
    main()
