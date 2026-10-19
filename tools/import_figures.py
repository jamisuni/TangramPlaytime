#!/usr/bin/env python3
"""
Import the TangramCollections figures (design input DI-10) as `tangram-puzzle/1` puzzle files.

WO-010 TASK-036: seams S-38 (the converter), S-39 (an imported file's fields), S-40 (the batch lists), S-41 (the
asset-record rows, in the report) and S-42 (the colour report), tasks.md § WO-010 › Seams. Python 3, standard
library only; it reuses tools/tangram_geom.py and tools/validate_puzzles.py. It stays in the repository as the
record of how each imported file was made: the title tables below are part of that record.

Usage, from the project root:
    python tools/import_figures.py import --convex <convex-13 figures.json> --android <android-tangram-131 figures.json>
                                          --report <markdown file> [--dry-run]
    python tools/import_figures.py colours <puzzle file> ...
    python tools/import_figures.py show <puzzle file>

import   reads every figure, convex-13 in file order, then android-tangram-131 in file order, and either writes it
         as Tangrams/<id>.json or skips it with a reason:
           1. pieces: L -> LT1 then LT2, M -> MT, S -> ST1 then ST2, Q -> SQ, P -> PG. The source's `mirrored`
              flag is not read: the polygon decides the mirror (TYPE-003);
           2. snap: each coordinate v (small-triangle legs 1, y down, the bounding box from (0, 0)) becomes the one
              pair (p, q), p and q multiples of 1/4 in [-16, 16], with |p + q*sqrt2 - v| <= 2e-4. No pair, or more
              than one -> skipped, "no clean snap";
           3. scale: sqrt2 * (p + q*sqrt2) = 2q + p*sqrt2, written [2q, p] (a plain number when p = 0); no turn, no
              mirror, no shift (Spec/03 §1: the set makes the 4 x 4 square);
           4. pose: tangram_geom.placement_from_polygon finds each piece's turn, mirror and anchor exactly. None ->
              skipped, "a piece off TYPE-003's turns". The written polygon is piece_polygon(piece, rot, flip, at),
              the local shape's corner order; the entries follow TYPE-001 order;
           5. screen: validate_puzzles.validate on the draft (S-39's fields, the stub picture). An error -> skipped,
              with its rule as the reason;
           6. duplicates: the silhouette (the union of the solution pieces), compared exactly in Q(sqrt2) with the
              13 shipped puzzles and with every figure accepted before it. Equal after a translation -> skipped,
              "duplicate of <id>". Equal only after one of the 8 turns and/or a mirror -> kept, reported
              "turned copy of <id>";
           7. write: each accepted figure as Tangrams/<id>.json (UTF-8, LF, no BOM, the shipped files' layout); an
              existing file stops the run before anything is written. Also the S-40 lists
              tools/import/batch-1.txt ... batch-4.txt, and the --report file (markdown). --dry-run writes only the
              report.
colours  per file, the number of distinct #RRGGBB values (case-insensitive) among art.base and every shape's fill and
         stroke. STUB marks the import stub, OUT a count outside 3..8; exit 1 if any listed file is STUB or OUT.
show     a puzzle file's bounding box and each piece's corners as floats (an authoring aid for the pictures).

It never writes under the folder the figures are read from (C:/GitHub/AI/TangramCollections), and the collection's
own numbers (`width`, `height`, `overlap`, `maxPieceAreaError`, `mirrored`) are never read.
"""
import argparse
import hashlib
import json
import math
import re
import sys
from collections import Counter
from fractions import Fraction as F
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parent
sys.path.insert(0, str(TOOLS))

from tangram_geom import (COS, PIECE_SET, Q2, SIN, SQRT2, area, piece_polygon,  # noqa: E402
                          placement_from_polygon, shared_edge_length, to_float)
from validate_puzzles import FORMAT_TAG, load_solution, perimeter, validate  # noqa: E402

TANGRAMS = ROOT / "Tangrams"
IMPORT_DIR = ROOT / "tools" / "import"

# ── S-38 step 1: the piece letters ─────────────────────────────────────────────────────────────────
PIECE_LETTERS = {"L": ["LT1", "LT2"], "M": ["MT"], "S": ["ST1", "ST2"], "Q": ["SQ"], "P": ["PG"]}
TYPE001_ORDER = ["LT1", "LT2", "MT", "SQ", "PG", "ST1", "ST2"]  # TYPE-001 tray order

# ── S-38 step 2: the snap ──────────────────────────────────────────────────────────────────────────
SNAP_STEP = 4          # p and q are multiples of 1/4
SNAP_LIMIT = 16        # ... between -16 and 16
SNAP_TOL = 2e-4        # the files store four decimals

# ── S-39: the fields ───────────────────────────────────────────────────────────────────────────────
STUB_ART = {"base": "#D9DCE8", "shapes": []}
PROVENANCE = {"author": "ai:claude", "reviewedByHuman": False}
GROUP_CATEGORY = {"base": "shapes", "geometric": "shapes", "alphabet": "letters", "numbers": "numbers",
                  "animals": "animals", "people": "people", "objects": "things", "Christmas": "things",
                  "icons": "things"}
SUBJECT_CATEGORY = {}
for _names, _cat in (
        (["Boat", "Cargo Ship", "Helicopter", "Plane", "Pram", "Rocket", "Sailboat", "Sled", "Sleigh"], "vehicles"),
        (["Mountain", "Tree", "Tree (short)", "Crescent Moon", "Star"], "nature"),
        (["Caroler", "Fairy", "Person with Gift", "Snowman"], "people")):
    for _n in _names:
        SUBJECT_CATEGORY[_n] = _cat
CONVEX_CATEGORY = "shapes"
# REQ-040's rating from E, the mean V12 exposure (S-39, checkpoint item 4's default): (lower bound, rating)
RATING_STEPS = [(0.56, 1), (0.52, 2), (0.47, 3), (0.40, 4)]
RATING_BELOW = 5

# ── S-40: the batch lists ──────────────────────────────────────────────────────────────────────────
BATCH_OF_CATEGORY = {"shapes": 1, "numbers": 1, "letters": 2, "things": 3, "vehicles": 3,
                     "animals": 4, "nature": 4, "people": 4}

# ── S-41: the asset record ─────────────────────────────────────────────────────────────────────────
LICENCE = {"this project": "GPL-3.0", "convex-13": "CC0-1.0", "android-tangram-131": "GPL-3.0"}
PICTURE = "made for this project"

# ── S-39 titles: the edits (source id -> (title.en, case)) and the reference Finnish titles ──────────
# Cases: "a" a title another bundled puzzle already has (a distinguishing word from the figure); "b" a word
# unsuitable for young children; "c" brackets become plain words; "convex" Convex N becomes a plain shape name.
# Chosen after looking at the dry run's silhouettes (TASK-036 run step 3). Every edit and every Finnish title is
# listed in review-notes.md (G-10: the Finnish titles are reference translations, never tested).
EN_EDITS = {
    "convex-01": ("Tall pentagon", "convex"),
    "convex-02": ("Right trapezoid", "convex"),
    "convex-04": ("Standing parallelogram", "convex"),
    "convex-05": ("Wide hexagon", "convex"),
    "convex-06": ("Tall hexagon", "convex"),
    "convex-07": ("Standing trapezoid", "convex"),
    "convex-08": ("Hexagon", "convex"),
    "convex-09": ("Diamond", "convex"),
    "convex-10": ("Cut square", "convex"),
    "convex-11": ("Wide pentagon", "convex"),
    "convex-12": ("Slanted trapezoid", "convex"),
    "convex-13": ("Triangle", "convex"),
    "objects-arrow": ("Thick arrow", "a"),          # the shipped Arrow has a thin shaft and fletching
    "objects-house": ("Little house", "a"),         # the shipped House has a shed and a slanted chimney
    "objects-mountain": ("Mountain range", "a"),    # three peaks; the shipped Mountain has one
    "objects-tobacco-pipe": ("Pipe", "b"),
    "christmas-tree-short": ("Short tree", "c"),
}
FI_TITLES = {
    "convex-01": "Korkea viisikulmio",
    "convex-02": "Suorakulmainen puolisuunnikas",
    "convex-04": "Seisova suunnikas",
    "convex-05": "Leveä kuusikulmio",
    "convex-06": "Korkea kuusikulmio",
    "convex-07": "Seisova puolisuunnikas",
    "convex-08": "Kuusikulmio",
    "convex-09": "Vinoneliö",
    "convex-10": "Leikattu neliö",
    "convex-11": "Leveä viisikulmio",
    "convex-12": "Vino puolisuunnikas",
    "convex-13": "Kolmio",
    "geometric-chevron": "Nuolenkärki",
    "geometric-cup": "Kuppi",
    "geometric-double-convex-hexagon": "Kaksoiskuusikulmio",
    "geometric-parallelogram": "Suunnikas",
    "geometric-trapezoid": "Puolisuunnikas",
    "objects-arrow": "Paksu nuoli",
    "objects-boat": "Vene",
    "objects-bowl": "Kulho",
    "objects-bridge": "Silta",
    "objects-cargo-ship": "Rahtilaiva",
    "objects-chair": "Tuoli",
    "objects-hammer": "Vasara",
    "objects-heart": "Sydän",
    "objects-house": "Pikkutalo",
    "objects-iron": "Silitysrauta",
    "objects-key": "Avain",
    "objects-mountain": "Vuorijono",
    "objects-pinwheel": "Tuulimylly",
    "objects-plane": "Lentokone",
    "objects-rocket": "Raketti",
    "objects-shirt": "Paita",
    "objects-shoe": "Kenkä",
    "objects-sword": "Miekka",
    "objects-table": "Pöytä",
    "objects-tobacco-pipe": "Piippu",
    "objects-vase": "Maljakko",
    "objects-wrench": "Jakoavain",
    "animals-bat": "Lepakko",
    "animals-bear": "Karhu",
    "aminals-bird": "Lintu",
    "animals-butterfly": "Perhonen",
    "animals-camel": "Kameli",
    "animals-chicken": "Kana",
    "animals-crab": "Rapu",
    "animals-giraffe": "Kirahvi",
    "animals-goat": "Vuohi",
    "animals-goose": "Hanhi",
    "animals-llama": "Laama",
    "animals-snail": "Etana",
    "animals-swan": "Joutsen",
    "animals-turtle": "Kilpikonna",
    "animals-vulture": "Korppikotka",
    "people-standing": "Seisomassa",
    "christmas-stocking": "Sukka",
    "christmas-tree-short": "Matala puu",
    "icons-back-arrow": "Paluunuoli",
    "icons-checkmark": "Väkänen",
    "icons-crescent-moon": "Kuunsirppi",
    "icons-folder": "Kansio",
    "icons-star": "Tähti",
}
# The Finnish titles the letters and the digits take by pattern (title.en -> title.fi).
FI_PATTERNS = [(re.compile(r"^Letter ([A-Z])$"), "Kirjain {0}"), (re.compile(r"^Number ([0-9])$"), "Numero {0}")]


def fi_title(source_id, title_en, fi_titles):
    if source_id in fi_titles:
        return fi_titles[source_id]
    for pattern, form in FI_PATTERNS:
        m = pattern.match(title_en)
        if m:
            return form.format(*m.groups())
    return None


# ═══ Exact helpers ═════════════════════════════════════════════════════════════════════════════════

def q2_sign(v):
    """Sign of an exact a + b*sqrt2: -1, 0 or 1."""
    a, b = v.a, v.b
    if a >= 0 and b >= 0:
        return 0 if a == 0 and b == 0 else 1
    if a <= 0 and b <= 0:
        return -1
    if a > 0:            # b < 0
        return 1 if a * a > 2 * b * b else -1
    return 1 if 2 * b * b > a * a else -1   # a < 0 < b


def point_key(p):
    """A total order on exact points: x, then y (each compared exactly)."""
    return (_Q2Order(p[0]), _Q2Order(p[1]))


class _Q2Order:
    __slots__ = ("v",)

    def __init__(self, v):
        self.v = v

    def __lt__(self, o):
        return q2_sign(o.v - self.v) > 0

    def __eq__(self, o):
        return self.v == o.v


def cross(o, a, b):
    """(a - o) x (b - o), exact."""
    return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])


def dot(o, a, b):
    """(a - o) . (b - o), exact."""
    return (a[0] - o[0]) * (b[0] - o[0]) + (a[1] - o[1]) * (b[1] - o[1])


# ═══ S-38 steps 2-4: snap, scale, pose ══════════════════════════════════════════════════════════════

def snap_candidates(v):
    """Every pair (p, q) of quarters in [-16, 16] with |p + q*sqrt2 - v| <= 2e-4 (S-38 step 2)."""
    found = []
    for k in range(-SNAP_LIMIT * SNAP_STEP, SNAP_LIMIT * SNAP_STEP + 1):
        q = F(k, SNAP_STEP)
        p = F(round((v - float(q) * SQRT2) * SNAP_STEP), SNAP_STEP)
        if abs(p) > SNAP_LIMIT:
            continue
        if abs(float(p) + float(q) * SQRT2 - v) <= SNAP_TOL:
            found.append((p, q))
    return found


def snap(v):
    """The one pair (p, q) for v, or None when there is no pair or more than one."""
    found = snap_candidates(v)
    return found[0] if len(found) == 1 else None


def scaled(pq):
    """S-38 step 3: sqrt2 * (p + q*sqrt2) = 2q + p*sqrt2, the Spec/03 §1 value."""
    p, q = pq
    return Q2(2 * q, p)


def degrees_off_the_turns(points):
    """How far (in degrees, float) a source piece's first edge lies from the nearest 45-degree direction. Only
    words the "no clean snap" reason; the snap itself decides."""
    (x0, y0), (x1, y1) = points[0], points[1]
    a = math.degrees(math.atan2(y1 - y0, x1 - x0)) % 45
    return min(a, 45 - a)


def _why_no_snap(figure):
    counters = Counter()
    for piece in figure["pieces"]:
        letter = piece["type"]
        ids = PIECE_LETTERS[letter]
        pid = ids[min(counters[letter], len(ids) - 1)]
        counters[letter] += 1
        off = degrees_off_the_turns(piece["points"])
        if off > 0.5:
            return f"{pid} lies {off:.0f}° off TYPE-003's 45° turns"
    return "every piece on a 45° turn, a position off the quarter grid"


def convert(figure):
    """S-38 steps 1-4. Returns ({piece id: exact polygon in piece_polygon's corner order}, None), or (None, reason)."""
    counters = Counter()
    targets = {}
    for piece in figure["pieces"]:
        letter = piece["type"]
        ids = PIECE_LETTERS.get(letter)
        if ids is None or counters[letter] >= len(ids):
            return None, f"unknown or repeated piece letter {letter!r}"
        pid = ids[counters[letter]]
        counters[letter] += 1
        corners = []
        for x, y in piece["points"]:
            exact = []
            for v in (x, y):
                pq = snap(v)
                if pq is None:
                    n = len(snap_candidates(v))
                    return None, f"no clean snap ({pid} value {v}: {n} pairs; {_why_no_snap(figure)})"
                exact.append(scaled(pq))
            corners.append((exact[0], exact[1]))
        targets[pid] = corners
    if sorted(targets) != sorted(TYPE001_ORDER):
        return None, f"not the seven pieces ({sorted(targets)})"
    polys = {}
    for pid in TYPE001_ORDER:
        pose = placement_from_polygon(pid, targets[pid])
        if pose is None:
            return None, f"a piece off TYPE-003's turns (V3: {pid})"
        rot, flip, at = pose
        polys[pid] = piece_polygon(pid, rot, flip, at)
    return polys, None


# ═══ S-38 step 6: the silhouette, exactly ═══════════════════════════════════════════════════════════

def _oriented(poly):
    """The polygon with a positive shoelace area (one orientation for every piece)."""
    fp = to_float(poly)
    s = sum(fp[i][0] * fp[(i + 1) % len(fp)][1] - fp[(i + 1) % len(fp)][0] * fp[i][1] for i in range(len(fp)))
    return list(poly) if s > 0 else list(reversed(poly))


def _inside_open_segment(v, a, b):
    if cross(a, b, v) != Q2(0):
        return False
    return q2_sign(dot(a, b, v)) > 0 and q2_sign(dot(a, b, b) - dot(a, b, v)) > 0


def silhouette_segments(polys):
    """The silhouette's outline as maximal directed segments, the region on one fixed side of each: every piece
    edge is split at every piece corner on it; a stretch two pieces share cancels (opposite directions); collinear
    stretches that continue each other are merged. Raises ValueError when a stretch is covered twice (an overlap)."""
    verts = {p for poly in polys.values() for p in poly}
    net = Counter()
    for poly in polys.values():
        poly = _oriented(poly)
        for i in range(len(poly)):
            a, b = poly[i], poly[(i + 1) % len(poly)]
            inner = [v for v in verts if v != a and v != b and _inside_open_segment(v, a, b)]
            inner.sort(key=lambda v: _Q2Order(dot(a, b, v)))
            chain = [a] + inner + [b]
            for u, w in zip(chain, chain[1:]):
                if point_key(u) < point_key(w):
                    net[(u, w)] += 1
                else:
                    net[(w, u)] -= 1
    segs = []
    for (u, w), n in net.items():
        if n == 0:
            continue
        if abs(n) != 1:
            raise ValueError("a stretch of outline is covered twice (pieces overlap)")
        segs.append((u, w) if n == 1 else (w, u))
    return _merge(segs)


def _merge(segs):
    segs = list(segs)
    merged = True
    while merged:
        merged = False
        starts = {}
        for s in segs:
            starts.setdefault(s[0], []).append(s)
        for s in segs:
            for t in starts.get(s[1], []):
                if t is s:
                    continue
                d1 = (s[1][0] - s[0][0], s[1][1] - s[0][1])
                d2 = (t[1][0] - t[0][0], t[1][1] - t[0][1])
                if d1[0] * d2[1] - d1[1] * d2[0] == Q2(0) and q2_sign(d1[0] * d2[0] + d1[1] * d2[1]) > 0:
                    segs.remove(s)
                    segs.remove(t)
                    segs.append((s[0], t[1]))
                    merged = True
                    break
            if merged:
                break
    return segs


def _normalised(segs):
    """The segments translated so that their least corner (x, then y) is the origin, as a set."""
    lowest = min((p for s in segs for p in s), key=point_key)
    return frozenset(((u[0] - lowest[0], u[1] - lowest[1]), (w[0] - lowest[0], w[1] - lowest[1])) for u, w in segs)


def _moved(segs, rot, mirror):
    out = []
    c, s = COS[rot], SIN[rot]
    for u, w in segs:
        pts = []
        for x, y in (u, w):
            if mirror:
                x = -x
            pts.append((x * c - y * s, x * s + y * c))
        out.append((pts[1], pts[0]) if mirror else (pts[0], pts[1]))   # a mirror swaps the region's side
    return out


def silhouette_keys(polys):
    """{(rot, mirror): key} over the 8 turns and the mirror; (0, False) is the silhouette as it lies."""
    segs = silhouette_segments(polys)
    return {(rot, mirror): _normalised(_moved(segs, rot, mirror)) for mirror in (False, True) for rot in range(8)}


def same_figure(candidate_keys, known_key):
    """'translation' when equal after a translation, 'turned' when equal only after a turn and/or a mirror, else None."""
    if candidate_keys[(0, False)] == known_key:
        return "translation"
    if any(k == known_key for k in candidate_keys.values()):
        return "turned"
    return None


# ═══ S-39: the fields ═══════════════════════════════════════════════════════════════════════════════

def slug(text):
    """Lower case, every run of characters other than a-z and 0-9 as one hyphen, no hyphen at either end."""
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")


def source_slug(collection, figure):
    """convex-13 keeps its `convex-NN`; android drops the group prefix ('alphabet-A' -> 'a', 'aminals-bird' -> 'bird')."""
    if collection == "convex-13":
        return slug(figure["id"])
    return slug(figure["id"].split("-", 1)[1])


def category_of(collection, figure):
    if collection == "convex-13":
        return CONVEX_CATEGORY
    if figure["name"] in SUBJECT_CATEGORY:
        return SUBJECT_CATEGORY[figure["name"]]
    return GROUP_CATEGORY[figure["category"]]


def exposures(polys):
    """V12's measure per piece, as tools/validate_puzzles.py measures it: 1 - shared outline / perimeter."""
    fp = {k: to_float(v) for k, v in polys.items()}
    out = {}
    for k, p in fp.items():
        shared = sum(shared_edge_length(p, q) for kk, q in fp.items() if kk != k)
        out[k] = 1 - shared / perimeter(p)
    return out


def mean_exposure(polys):
    e = exposures(polys)
    return sum(e.values()) / len(e)


def rating_of(e):
    """S-39: E >= 0.56 -> 1; 0.52 <= E < 0.56 -> 2; 0.47 <= E < 0.52 -> 3; 0.40 <= E < 0.47 -> 4; E < 0.40 -> 5."""
    for lower, rating in RATING_STEPS:
        if e >= lower:
            return rating
    return RATING_BELOW


def near_a_step(e, tol=1e-6):
    return any(abs(e - lower) < tol for lower, _ in RATING_STEPS)


def coord_json(v):
    return v.to_json()


def draft_of(pid_polys, ident, title_en, title_fi, category, rating):
    return {
        "format": FORMAT_TAG,
        "id": ident,
        "title": {"en": title_en, "fi": title_fi},
        "category": category,
        "difficulty": rating,
        "kind": "full",
        "solution": [{"piece": pid, "polygon": [[coord_json(x), coord_json(y)] for x, y in pid_polys[pid]]}
                     for pid in TYPE001_ORDER],
        "art": {"base": STUB_ART["base"], "shapes": list(STUB_ART["shapes"])},
        "provenance": dict(PROVENANCE),
    }


def bbox(polys):
    pts = [p for poly in polys.values() for p in to_float(poly)]
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    return min(xs), min(ys), max(xs), max(ys)


# ═══ S-38 step 7: the shipped files' layout ═══════════════════════════════════════════════════════════

def _one_line(value):
    return json.dumps(value, ensure_ascii=False, separators=(", ", ": "))


def format_puzzle(puzzle):
    """The shipped files' layout: 2-space indent, `title` and `provenance` one key per line, one solution entry and
    one art shape per line, LF line ends, a final newline."""
    lines = ["{"]
    keys = list(puzzle)
    for n, key in enumerate(keys):
        value = puzzle[key]
        comma = "," if n < len(keys) - 1 else ""
        k = json.dumps(key)
        if key in ("title", "provenance") and isinstance(value, dict):
            lines.append(f"  {k}: {{")
            items = list(value.items())
            for m, (kk, vv) in enumerate(items):
                lines.append(f"    {json.dumps(kk)}: {_one_line(vv)}{',' if m < len(items) - 1 else ''}")
            lines.append(f"  }}{comma}")
        elif key == "solution":
            lines.append(f"  {k}: [")
            for m, entry in enumerate(value):
                lines.append(f"    {_one_line(entry)}{',' if m < len(value) - 1 else ''}")
            lines.append(f"  ]{comma}")
        elif key == "art":
            lines.append(f"  {k}: {{")
            lines.append(f"    \"base\": {_one_line(value['base'])},")
            shapes = value["shapes"]
            if shapes:
                lines.append("    \"shapes\": [")
                for m, shape in enumerate(shapes):
                    lines.append(f"      {_one_line(shape)}{',' if m < len(shapes) - 1 else ''}")
                lines.append("    ]")
            else:
                lines.append("    \"shapes\": []")
            lines.append(f"  }}{comma}")
        else:
            lines.append(f"  {k}: {_one_line(value)}{comma}")
    lines.append("}")
    return "\n".join(lines) + "\n"


def write_text_new(path, text):
    """Write UTF-8 with LF line ends and no BOM; never over an existing file."""
    with open(path, "x", encoding="utf-8", newline="\n") as f:
        f.write(text)


# ═══ The import ═════════════════════════════════════════════════════════════════════════════════════

def shipped_puzzles(folder=TANGRAMS):
    out = []
    for p in sorted(x for x in folder.glob("*.json") if not x.name.endswith(".schema.json")):
        puzzle = json.loads(p.read_text(encoding="utf-8"))
        polys = load_solution(puzzle, [])
        out.append((puzzle, polys))
    return out


def source_root(path):
    """The collection folder's root (…/TangramCollections) for a …/collections/<name>/figures.json path."""
    p = Path(path).resolve()
    return p.parents[2] if p.parent.parent.name == "collections" else p.parent


def run_import(convex_path, android_path, en_edits=None, fi_titles=None, shipped=None):
    """Steps 1-6 over both collections. Returns the plan (imports, skips, problems); writes nothing."""
    en_edits = EN_EDITS if en_edits is None else en_edits
    fi_titles = FI_TITLES if fi_titles is None else fi_titles
    shipped = shipped_puzzles() if shipped is None else shipped
    known = []                                   # (id, silhouette key as it lies), in precedence order
    taken_ids = set()
    titles = {}                                  # lower-case title.en -> id
    for puzzle, polys in shipped:
        known.append((puzzle["id"], silhouette_keys(polys)[(0, False)]))
        taken_ids.add(puzzle["id"])
        titles[puzzle["title"]["en"].lower()] = puzzle["id"]
    sources = [("convex-13", convex_path), ("android-tangram-131", android_path)]
    figures = []
    for collection, path in sources:
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        for fig in data["figures"]:
            figures.append((collection, fig))
    imports, skips, problems, used_edits = [], [], [], set()
    for collection, fig in figures:
        src, name = fig["id"], fig["name"]
        polys, reason = convert(fig)
        if polys is None:
            skips.append({"collection": collection, "source": src, "name": name, "reason": reason})
            continue
        category = category_of(collection, fig)
        e = mean_exposure(polys)
        rating = rating_of(e)
        # Step 5: the screen, on the draft (titles present for V8; the title text does not change any rule).
        provisional = draft_of(polys, f"{category}-{source_slug(collection, fig)}", name, fi_titles.get(src, name),
                               category, rating)
        errors, _ = validate(provisional)
        if errors:
            rules = sorted({err.split()[0] for err in errors})
            label = "; ".join(errors)
            if rules == ["V5"]:
                label = "V5 not edge-connected"
            skips.append({"collection": collection, "source": src, "name": name, "reason": label})
            continue
        # Step 6: duplicates and turned copies, against the shipped 13, then every accepted figure, in order.
        keys = silhouette_keys(polys)
        dup = next((kid for kid, kkey in known if same_figure(keys, kkey) == "translation"), None)
        if dup is not None:
            skips.append({"collection": collection, "source": src, "name": name, "reason": f"duplicate of `{dup}`"})
            continue
        turned = next((kid for kid, kkey in known if same_figure(keys, kkey) == "turned"), None)
        # S-39 titles: the source name, or its edit by one of the three cases (convex: a plain shape name).
        en, case = name, None
        if src in en_edits:
            en, case = en_edits[src]
            used_edits.add(src)
        clash = titles.get(name.lower())
        if collection == "convex-13" and case != "convex":
            problems.append(f"{src}: 'Convex N' needs a plain shape name (case convex)")
        if ("(" in name or ")" in name) and case != "c":
            problems.append(f"{src}: brackets need plain words (case c)")
        if clash is not None and case not in ("a", "b", "c", "convex"):
            problems.append(f"{src}: title {name!r} is already `{clash}`'s (case a)")
        if case == "a" and clash is None:
            problems.append(f"{src}: case a edit, but {name!r} clashes with no bundled title")
        if case == "c" and "(" not in name:
            problems.append(f"{src}: case c edit, but {name!r} has no brackets")
        if en.lower() in titles:
            problems.append(f"{src}: final title {en!r} is already `{titles[en.lower()]}`'s")
        fi = fi_title(src, en, fi_titles)
        if fi is None:
            problems.append(f"{src}: no Finnish reference title")
            fi = en
        # S-39 id: <category>-<slug>; a taken id, or a `kit` segment, takes the slug of the final title.en.
        ident = f"{category}-{source_slug(collection, fig)}"
        if ident in taken_ids or "kit" in ident.split("-"):
            ident = f"{category}-{slug(en)}"
            if ident in taken_ids or "kit" in ident.split("-"):
                problems.append(f"{src}: no id: {ident!r} is taken or has a 'kit' segment")
        draft = draft_of(polys, ident, en, fi, category, rating)
        errors, info = validate(draft)
        if errors:
            problems.append(f"{src}: the final draft fails: {'; '.join(errors)}")
        known.append((ident, keys[(0, False)]))
        taken_ids.add(ident)
        titles[en.lower()] = ident
        x0, y0, x1, y1 = bbox(polys)
        imports.append({"collection": collection, "source": src, "name": name, "id": ident, "category": category,
                        "rating": rating, "E": e, "en": en, "fi": fi, "case": case, "turned": turned,
                        "width": x1 - x0, "height": y1 - y0, "draft": draft, "info": info,
                        "batch": BATCH_OF_CATEGORY[category]})
    for src in sorted(set(en_edits) - used_edits):
        problems.append(f"{src}: title edit listed for a figure that is not imported")
    for src in sorted(set(fi_titles) - {i["source"] for i in imports}):
        problems.append(f"{src}: Finnish title listed for a figure that is not imported")
    return {"figures": len(figures), "imports": imports, "skips": skips, "problems": problems,
            "shipped": [p for p, _ in shipped]}


def reason_class(reason):
    if reason.startswith("duplicate of"):
        return "duplicate"
    if reason.startswith("no clean snap"):
        return "no clean snap"
    if reason.startswith("a piece off TYPE-003"):
        return "a piece off TYPE-003's turns"
    if reason.startswith("V5 not edge-connected"):
        return "V5 not edge-connected"
    return reason.split(";")[0]


def batch_lists(imports):
    lists = {n: [] for n in (1, 2, 3, 4)}
    for i in imports:
        lists[i["batch"]].append(i["id"])
    return {n: sorted(ids) for n, ids in lists.items()}


def report_markdown(plan, inputs):
    imports, skips = plan["imports"], plan["skips"]
    out = ["# WO-010 import report (tools/import_figures.py)", ""]
    for label, path in inputs:
        digest = hashlib.sha1(Path(path).read_bytes()).hexdigest()[:12]
        out.append(f"- input {label}: `{Path(path).as_posix()}` (sha1 {digest})")
    out += ["", "## Counts", "",
            f"- figures read: {plan['figures']}",
            f"- imported: {len(imports)}",
            f"- skipped: {len(skips)}",
            f"- imported + skipped = {len(imports) + len(skips)}", ""]
    out += ["| Skip reason | Count |", "|---|---|"]
    for reason, n in sorted(Counter(reason_class(s["reason"]) for s in skips).items()):
        out.append(f"| {reason} | {n} |")
    out += ["", "## Problems (must be empty for a real run)", ""]
    out += [f"- {p}" for p in plan["problems"]] or ["- none"]
    out += ["", "## Skipped figures", "", "| Collection | Source id | Name | Reason |", "|---|---|---|---|"]
    for s in skips:
        out.append(f"| {s['collection']} | `{s['source']}` | {s['name']} | {s['reason']} |")
    out += ["", "## Turned copies (kept, checkpoint item 3's default)", ""]
    turned = [i for i in imports if i["turned"]]
    out += [f"- `{i['id']}` ({i['source']}, {i['name']}) is a turned copy of `{i['turned']}`" for i in turned] or ["- none"]
    out += ["", "## Imported figures", "",
            "| Puzzle id | Collection | Source id | Source name | title.en | title.fi | Rating | E | Width × height | Batch |",
            "|---|---|---|---|---|---|---|---|---|---|"]
    for i in imports:
        out.append(f"| `{i['id']}` | {i['collection']} | `{i['source']}` | {i['name']} | {i['en']} | {i['fi']} | "
                   f"{i['rating']} | {i['E']:.4f} | {i['width']:.3f} × {i['height']:.3f} | {i['batch']} |")
    out += ["", "## Title edits", "", "| Source id | From | To | Case |", "|---|---|---|---|"]
    for i in imports:
        if i["case"]:
            out.append(f"| `{i['source']}` | {i['name']} | {i['en']} | {i['case']} |")
    out += ["", "## Rating distribution", "", "| Rating | Count |", "|---|---|"]
    dist = Counter(i["rating"] for i in imports)
    for r in range(1, 6):
        out.append(f"| {r} | {dist.get(r, 0)} |")
    near = [i for i in imports if near_a_step(i["E"])]
    out += ["", f"- E within 1e-6 of a step: {', '.join(i['id'] for i in near) or 'none'}"]
    out += ["", "## Batch lists (S-40)", ""]
    for n, ids in batch_lists(imports).items():
        out.append(f"- batch-{n}.txt: {len(ids)} ids")
    if imports:
        widest = max(imports, key=lambda i: i["width"])
        tallest = max(imports, key=lambda i: i["height"])
        out += ["", "## Extremes (puzzle units)", "",
                f"- widest: `{widest['id']}` {widest['width']:.3f} wide",
                f"- tallest: `{tallest['id']}` {tallest['height']:.3f} tall"]
        top = sorted(imports, key=lambda i: -i["width"])[:5]
        out.append("- five widest: " + ", ".join(f"`{i['id']}` {i['width']:.3f}" for i in top))
        top = sorted(imports, key=lambda i: -i["height"])[:5]
        out.append("- five tallest: " + ", ".join(f"`{i['id']}` {i['height']:.3f}" for i in top))
    out += ["", "## ASSET-LICENCES.md rows (S-41, every puzzle, sorted by puzzle id)", "",
            "| Puzzle id | Figure | Source figure id | Source name | Licence | Picture |", "|---|---|---|---|---|---|"]
    rows = [(p["id"], "this project", "—", "—") for p in plan["shipped"]]
    rows += [(i["id"], i["collection"], f"`{i['source']}`", i["name"]) for i in imports]
    for pid, figure, sid, sname in sorted(rows):
        out.append(f"| `{pid}` | {figure} | {sid} | {sname} | {LICENCE[figure]} | {PICTURE} |")
    out += ["", "## Validator lines of the imports", ""]
    out += [f"- `{i['id']}`: {'; '.join(i['info'])}" for i in imports]
    return "\n".join(out) + "\n"


def cmd_import(args):
    forbidden = {source_root(args.convex), source_root(args.android)}
    targets = [Path(args.report).resolve(), TANGRAMS.resolve(), IMPORT_DIR.resolve()]
    for t in targets:
        for root in forbidden:
            if t == root or root in t.parents:
                print(f"refused: {t} is under the figures' folder {root}")
                return 2
    plan = run_import(args.convex, args.android)
    Path(args.report).parent.mkdir(parents=True, exist_ok=True)
    report = report_markdown(plan, [("convex-13", args.convex), ("android-tangram-131", args.android)])
    with open(args.report, "w", encoding="utf-8", newline="\n") as f:
        f.write(report)
    imports, skips = plan["imports"], plan["skips"]
    print(f"figures {plan['figures']}: imported {len(imports)}, skipped {len(skips)}; report {args.report}")
    for reason, n in sorted(Counter(reason_class(s["reason"]) for s in skips).items()):
        print(f"  skipped, {reason}: {n}")
    if plan["problems"]:
        print(f"{len(plan['problems'])} problem(s):")
        for p in plan["problems"]:
            print(f"  {p}")
    if args.dry_run:
        print("dry run: only the report was written")
        return 0
    if plan["problems"]:
        print("not written: the problems above must be solved first")
        return 2
    files = {TANGRAMS / f"{i['id']}.json": format_puzzle(i["draft"]) for i in imports}
    lists = {IMPORT_DIR / f"batch-{n}.txt": "".join(f"{x}\n" for x in ids) for n, ids in batch_lists(imports).items()}
    existing = [p for p in list(files) + list(lists) if p.exists()]
    if existing:
        print("stopped: these files exist and are never overwritten:")
        for p in existing:
            print(f"  {p}")
        return 2
    IMPORT_DIR.mkdir(parents=True, exist_ok=True)
    for path, text in list(files.items()) + list(lists.items()):
        write_text_new(path, text)
    print(f"wrote {len(files)} puzzle files and {len(lists)} batch lists")
    return 0


# ═══ S-42: the colour report ═══════════════════════════════════════════════════════════════════════

COLOUR_MIN, COLOUR_MAX = 3, 8


def colour_count(art):
    values = {str(art.get("base", "")).upper()}
    for shape in art.get("shapes", []):
        for key in ("fill", "stroke"):
            if key in shape:
                values.add(str(shape[key]).upper())
    return len(values)


def is_stub(art):
    return (isinstance(art, dict) and set(art) == {"base", "shapes"} and str(art["base"]).upper() == STUB_ART["base"]
            and art["shapes"] == [])


def cmd_colours(paths):
    bad = 0
    for path in paths:
        try:
            art = json.loads(Path(path).read_text(encoding="utf-8")).get("art", {})
        except (OSError, ValueError) as e:
            print(f"FAIL {path}: {e}")
            bad += 1
            continue
        n = colour_count(art)
        mark = "STUB" if is_stub(art) else ("OUT" if not COLOUR_MIN <= n <= COLOUR_MAX else "ok")
        print(f"{mark:4} {n} {Path(path).name}")
        bad += mark != "ok"
    print(f"\n{len(paths) - bad}/{len(paths)} files with 3 to 8 colours and a drawn picture")
    return 1 if bad else 0


# ═══ show ═══════════════════════════════════════════════════════════════════════════════════════════

def cmd_show(path):
    puzzle = json.loads(Path(path).read_text(encoding="utf-8"))
    errors = []
    polys = load_solution(puzzle, errors)
    if errors:
        print("\n".join(errors))
        return 1
    x0, y0, x1, y1 = bbox(polys)
    print(f"{puzzle['id']}: x {x0:.4f} .. {x1:.4f}, y {y0:.4f} .. {y1:.4f} ({x1 - x0:.4f} × {y1 - y0:.4f}); "
          f"area {sum(area(to_float(p)) for p in polys.values()):g}")
    for pid, poly in polys.items():
        print(f"  {pid:3} " + " ".join(f"({x:.4f}, {y:.4f})" for x, y in to_float(poly)))
    return 0


def main(argv):
    parser = argparse.ArgumentParser(description="Import TangramCollections figures (WO-010, S-38 ... S-42).")
    sub = parser.add_subparsers(dest="command", required=True)
    imp = sub.add_parser("import")
    imp.add_argument("--convex", required=True)
    imp.add_argument("--android", required=True)
    imp.add_argument("--report", required=True)
    imp.add_argument("--dry-run", action="store_true")
    col = sub.add_parser("colours")
    col.add_argument("files", nargs="+")
    show = sub.add_parser("show")
    show.add_argument("file")
    args = parser.parse_args(argv)
    if args.command == "import":
        return cmd_import(args)
    if args.command == "colours":
        return cmd_colours(args.files)
    return cmd_show(args.file)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
