"""
Tangram geometry core (reference implementation for the spec).

Pure Python, no dependencies. Used by validate_puzzles.py and render_puzzle.py.
The Android app must reproduce exactly the same math (see Spec/03-puzzle-format.md).

Units: the classic 7-piece set assembles into a 4 x 4 square.
Numbers are exact values of the form a + b*sqrt(2) (a, b rational), because
rotating by 45 degrees introduces sqrt(2). Screen convention: x right, y DOWN,
positive rotation = clockwise on screen.
"""
from fractions import Fraction as F
import math

SQRT2 = math.sqrt(2)


class Q2:
    """Exact number a + b*sqrt(2)."""
    __slots__ = ("a", "b")

    def __init__(self, a=0, b=0):
        self.a = F(a)
        self.b = F(b)

    @staticmethod
    def parse(v):
        """JSON coordinate: number (e.g. 2, 1.5) or [a, b] meaning a + b*sqrt(2)."""
        if isinstance(v, (list, tuple)):
            if len(v) != 2:
                raise ValueError(f"coordinate pair must be [a, b], got {v!r}")
            return Q2(F(str(v[0])), F(str(v[1])))
        return Q2(F(str(v)), 0)

    def __add__(self, o):
        o = o if isinstance(o, Q2) else Q2(o)
        return Q2(self.a + o.a, self.b + o.b)

    def __sub__(self, o):
        o = o if isinstance(o, Q2) else Q2(o)
        return Q2(self.a - o.a, self.b - o.b)

    def __neg__(self):
        return Q2(-self.a, -self.b)

    def __mul__(self, o):
        o = o if isinstance(o, Q2) else Q2(o)
        return Q2(self.a * o.a + 2 * self.b * o.b, self.a * o.b + self.b * o.a)

    __rmul__ = __mul__

    def __eq__(self, o):
        o = o if isinstance(o, Q2) else Q2(o)
        return self.a == o.a and self.b == o.b

    def __hash__(self):
        return hash((self.a, self.b))

    def __float__(self):
        return float(self.a) + float(self.b) * SQRT2

    def to_json(self):
        if self.b == 0:
            return _num(self.a)
        return [_num(self.a), _num(self.b)]

    def __repr__(self):
        return f"Q2({self.to_json()})"


def _num(fr):
    fr = F(fr)
    return int(fr) if fr.denominator == 1 else float(fr)


HALF_SQRT2 = Q2(0, F(1, 2))
# cos/sin of k*45deg, k = 0..7
COS = [Q2(1), HALF_SQRT2, Q2(0), -HALF_SQRT2, Q2(-1), -HALF_SQRT2, Q2(0), HALF_SQRT2]
SIN = [Q2(0), HALF_SQRT2, Q2(1), HALF_SQRT2, Q2(0), -HALF_SQRT2, Q2(-1), -HALF_SQRT2]

# ---------------------------------------------------------------------------
# The seven pieces. Local shape at rot=0, flip=false. Vertex 0 is the anchor.
# ---------------------------------------------------------------------------
PIECE_TYPES = {
    #  type : (local vertices, area, rotational symmetry order, is chiral)
    "LT": ([(0, 0), (4, 0), (2, 2)], 4, 1, False),          # large triangle
    "MT": ([(0, 0), (2, 0), (0, 2)], 2, 1, False),          # medium triangle (right angle at anchor)
    "ST": ([(0, 0), (2, 0), (1, 1)], 1, 1, False),          # small triangle
    "SQ": ([(0, 0), (1, -1), (2, 0), (1, 1)], 2, 4, False),  # square (diamond at rot 0)
    "PG": ([(0, 0), (2, 0), (3, -1), (1, -1)], 2, 2, True),  # parallelogram (chiral: flip matters)
}

# Piece ids in a full set -> type. Same-type pieces are interchangeable.
PIECE_SET = {
    "LT1": "LT", "LT2": "LT", "MT": "MT", "ST1": "ST", "ST2": "ST", "SQ": "SQ", "PG": "PG",
}

# Default child-friendly colors (see Spec/02-ui-layout.md for the palette table)
PIECE_COLORS = {
    "LT1": "#E8505B", "LT2": "#3D8BFD", "MT": "#F9A826", "ST1": "#2DBE7E",
    "ST2": "#9B5DE5", "SQ": "#FFD23F", "PG": "#FF7AB8",
}


def transform(points, rot, flip, at):
    """points: list of (Q2,Q2) local; returns world points (Q2,Q2)."""
    c, s = COS[rot % 8], SIN[rot % 8]
    ax, ay = at
    out = []
    for x, y in points:
        if flip:
            x = -x
        out.append((x * c - y * s + ax, x * s + y * c + ay))
    return out


def piece_polygon(piece_id, rot=0, flip=False, at=(0, 0)):
    ptype = PIECE_SET[piece_id]
    local = [(Q2(x), Q2(y)) for x, y in PIECE_TYPES[ptype][0]]
    at = (at[0] if isinstance(at[0], Q2) else Q2(at[0]), at[1] if isinstance(at[1], Q2) else Q2(at[1]))
    return transform(local, rot, flip, at)


def canonical(poly):
    """Order-independent key of a polygon's vertex set (exact)."""
    return frozenset(poly)


def to_float(poly):
    return [(float(x), float(y)) for x, y in poly]


def area(fpoly):
    s = 0.0
    n = len(fpoly)
    for i in range(n):
        x1, y1 = fpoly[i]
        x2, y2 = fpoly[(i + 1) % n]
        s += x1 * y2 - x2 * y1
    return abs(s) / 2


def centroid(fpoly):
    n = len(fpoly)
    return (sum(p[0] for p in fpoly) / n, sum(p[1] for p in fpoly) / n)


EPS = 1e-9


def _axes(fpoly):
    n = len(fpoly)
    for i in range(n):
        x1, y1 = fpoly[i]
        x2, y2 = fpoly[(i + 1) % n]
        nx, ny = -(y2 - y1), (x2 - x1)
        ln = math.hypot(nx, ny)
        yield nx / ln, ny / ln


def overlap_depth(p1, p2):
    """SAT for convex polygons. Returns penetration depth (<=EPS means no interior overlap)."""
    best = float("inf")
    for ax, ay in list(_axes(p1)) + list(_axes(p2)):
        a = [x * ax + y * ay for x, y in p1]
        b = [x * ax + y * ay for x, y in p2]
        d = min(max(a), max(b)) - max(min(a), min(b))
        if d <= EPS:
            return 0.0
        best = min(best, d)
    return best


def shared_edge_length(p1, p2):
    """Total length of collinear overlapping boundary between two polygons (touching test)."""
    total = 0.0
    for i in range(len(p1)):
        a1, a2 = p1[i], p1[(i + 1) % len(p1)]
        for j in range(len(p2)):
            b1, b2 = p2[j], p2[(j + 1) % len(p2)]
            dx, dy = a2[0] - a1[0], a2[1] - a1[1]
            ln = math.hypot(dx, dy)
            ux, uy = dx / ln, dy / ln
            # both endpoints of b on line a?
            if abs((b1[0] - a1[0]) * uy - (b1[1] - a1[1]) * ux) > 1e-7:
                continue
            if abs((b2[0] - a1[0]) * uy - (b2[1] - a1[1]) * ux) > 1e-7:
                continue
            t1 = (b1[0] - a1[0]) * ux + (b1[1] - a1[1]) * uy
            t2 = (b2[0] - a1[0]) * ux + (b2[1] - a1[1]) * uy
            lo, hi = max(0.0, min(t1, t2)), min(ln, max(t1, t2))
            if hi - lo > 1e-7:
                total += hi - lo
    return total


def touches_at_point(p1, p2):
    return any(abs(x1 - x2) < 1e-7 and abs(y1 - y2) < 1e-7 for x1, y1 in p1 for x2, y2 in p2)


def solution_polygons(puzzle):
    """Returns {piece_id: exact polygon} from a puzzle dict."""
    out = {}
    for pl in puzzle["solution"]:
        at = (Q2.parse(pl["at"][0]), Q2.parse(pl["at"][1]))
        out[pl["piece"]] = piece_polygon(pl["piece"], pl.get("rot", 0), pl.get("flip", False), at)
    return out


def placement_from_polygon(piece_id, target):
    """Authoring helper: find (rot, flip, at) that maps piece onto the exact target polygon."""
    key = canonical(target)
    ptype = PIECE_SET[piece_id]
    local = [(Q2(x), Q2(y)) for x, y in PIECE_TYPES[ptype][0]]
    for flip in (False, True):
        for rot in range(8):
            moved = transform(local, rot, flip, (Q2(0), Q2(0)))
            ox, oy = moved[0]
            for tx, ty in target:
                at = (tx - ox, ty - oy)
                if canonical(transform(local, rot, flip, at)) == key:
                    return rot, flip, at
    return None


# ---------------------------------------------------------------------------
# Outline corners and build order (Study/06-snapping-by-anchors.md)
# ---------------------------------------------------------------------------
TWO_PI = 2 * math.pi
ARC_EPS = 1e-6


def _direction(frm, to):
    """Direction from frm to to, in [0, 2*pi)."""
    return math.atan2(to[1] - frm[1], to[0] - frm[0]) % TWO_PI


def _covered_arc(p, q):
    """Arc (start, length) of directions around float point p covered by convex float polygon q,
    or None when q does not touch p. At a vertex the arc spans the two edges; strictly inside an
    edge it is the half-plane on the polygon's side."""
    n = len(q)
    idx = [i for i, v in enumerate(q) if abs(v[0] - p[0]) < 1e-9 and abs(v[1] - p[1]) < 1e-9]
    if idx:
        u, w = q[idx[0] - 1], q[(idx[0] + 1) % n]
    else:
        k = next((i for i in range(n) if _on_segment(p, q[i], q[(i + 1) % n])), None)
        if k is None:
            return None
        u, w = q[k], q[(k + 1) % n]
    centre = (sum(v[0] for v in q) / n, sum(v[1] for v in q) / n)
    d1, d2, dc = _direction(p, u), _direction(p, w), _direction(p, centre)
    span = (d2 - d1) % TWO_PI
    if (dc - d1) % TWO_PI <= span:
        return d1, span
    return d2, (d1 - d2) % TWO_PI


def _on_segment(p, a, b, eps=1e-9):
    cross = (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])
    if abs(cross) > eps:
        return False
    dot = (p[0] - a[0]) * (b[0] - a[0]) + (p[1] - a[1]) * (b[1] - a[1])
    return eps < dot < (b[0] - a[0]) ** 2 + (b[1] - a[1]) ** 2 - eps


def outline_corners(polys):
    """Corners of the silhouette, classified by the contiguity of the covered directions (the kernel's
    rule, design-inputs 2 item 13): a vertex is interior when the full 360° is covered, a straight side
    when exactly one gap remains and the covered total is 180°; every other vertex is a corner
    (so 180° covered in two separate wedges, shapes-warmup-4 (2,2), is a corner).
    polys: {piece_id: exact polygon}. Returns a list of exact points (Q2, Q2)."""
    fl = {k: to_float(v) for k, v in polys.items()}
    seen, corners = set(), []
    for poly in polys.values():
        for p in poly:
            if p in seen:
                continue
            seen.add(p)
            fp = (float(p[0]), float(p[1]))
            arcs = [a for a in (_covered_arc(fp, q) for q in fl.values()) if a is not None]
            total = sum(length for _, length in arcs)
            if total >= TWO_PI - ARC_EPS:
                continue
            arcs.sort()
            gaps = 0
            for k, (start, length) in enumerate(arcs):
                nxt = arcs[(k + 1) % len(arcs)][0]
                gap = (nxt - (start + length)) % TWO_PI
                if ARC_EPS < gap < TWO_PI - ARC_EPS:
                    gaps += 1
            if not (gaps == 1 and abs(total - math.pi) <= ARC_EPS):
                corners.append(p)
    return corners


def build_order(polys):
    """Greedy order in which every piece can be locked: each piece must have a corner on an outline
    corner or on a corner of an already placed piece. Returns (order, stuck_pieces)."""
    anchors = set(outline_corners(polys))
    order, left = [], dict(polys)
    progress = True
    while left and progress:
        progress = False
        for pid in sorted(left, key=lambda k: -PIECE_TYPES[PIECE_SET[k]][1]):
            if any(v in anchors for v in left[pid]):
                order.append(pid)
                anchors.update(left.pop(pid))
                progress = True
                break
    return order, sorted(left)
