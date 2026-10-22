#!/usr/bin/env python3
"""
Make the launcher icon and the store icon from DI-9 (WO-009, seam S-57; decisions.md 2026-10-22 "The launcher icon and
the store icon are made from DI-9 by one generator.").

Reads DI-9, used as is (design-inputs.md: "used as is, not redrawn"):

    Spec/icon/option-A-foreground.svg   viewBox 0 0 108 108; seven <path> elements, each a fill and a d
    Spec/icon/option-A-background.txt   the background colour, #RRGGBB

and writes these four files, and no other:

    app/src/main/res/drawable/ic_launcher_foreground.xml   the adaptive icon's foreground: a <vector> of 108 x 108 dp,
                                                           viewport 108 x 108, one <path> per SVG path, in order, the
                                                           d and the fill copied verbatim
    app/src/main/res/values/ic_launcher_background.xml     <color name="ic_launcher_background">, the background colour
    app/src/main/res/mipmap-anydpi/ic_launcher.xml         the adaptive icon: that background and that foreground; no
                                                           monochrome layer and no PNG mipmaps (minSdk 31)
    release/pictures/icon-512.png                          the store icon: 512 x 512, a 32-bit PNG (8-bit RGBA, colour
                                                           type 6) with every alpha 255, opaque; the adaptive icon's
                                                           visible window, units 18 to 90 of the 108-unit viewBox,
                                                           scaled by 512 / 72 (decisions.md 2026-10-22 "Amends ...
                                                           (R7): the 512 x 512 store icon is a 32-bit PNG ...":
                                                           Google Play's icon specification names a 32-bit PNG)

The tool converts; it never redraws. The SVG may hold only the root <svg> (viewBox "0 0 108 108"; width and height,
when given, 108) and seven <path> elements, each with exactly a fill (#RRGGBB) and a d made of absolute M, L and Z
commands (each subpath "M x y", one or more "L x y", then "Z"). Anything else (another element, a transform, a text,
an image, another path command, another attribute) is refused with exit 1, and no file is written.

The PNG: the background colour everywhere, then the paths in document order (SVG's painter's order), each in its
colour, under SVG's default nonzero fill rule. The edges are anti-aliased by a fixed supersampling: every pixel is the
mean of a 4 x 4 grid of sample points inside it (sample k of 2048 per axis at unit 18 + (k + 1/2) * 72 / 2048), and a
sample point takes the colour of the last path that contains it, or the background. The geometry is computed in exact
rational arithmetic (fractions.Fraction) from the SVG's decimal numbers, and the mean is rounded half up in integers,
so no pixel depends on floating point. Every pixel's alpha is 255: the icon is a full, opaque square, and Play applies
its own mask. The file holds IHDR, one IDAT (zlib level 9, filter type 0 on every row) and IEND: no time stamp, no
text, no gamma chunk.

Deterministic: Python standard library only; a second run writes the same bytes (architecture.md G-06: to change the
icon, change DI-9 and run the tool again). The XMLs are ASCII with LF line ends and no BOM, and each opens with a
comment naming this tool and DI-9.

Run from anywhere:   python tools/make_icon.py
Options (the tool test uses them):  --svg PATH  --background PATH  --out-root DIR
(defaults: DI-9's two files and the repository root, found from this script's own location)
"""

import argparse
import math
import os
import re
import struct
import sys
import xml.etree.ElementTree as ET
import zlib
from fractions import Fraction

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SVG_DEFAULT = os.path.join(ROOT, "Spec", "icon", "option-A-foreground.svg")
BACKGROUND_DEFAULT = os.path.join(ROOT, "Spec", "icon", "option-A-background.txt")

# The four outputs, relative to the output root (S-57).
FOREGROUND_XML = "app/src/main/res/drawable/ic_launcher_foreground.xml"
BACKGROUND_XML = "app/src/main/res/values/ic_launcher_background.xml"
ADAPTIVE_XML = "app/src/main/res/mipmap-anydpi/ic_launcher.xml"
STORE_PNG = "release/pictures/icon-512.png"
OUTPUTS = (FOREGROUND_XML, BACKGROUND_XML, ADAPTIVE_XML, STORE_PNG)

SVG_NS = "{http://www.w3.org/2000/svg}"
VIEWBOX = (0, 0, 108, 108)      # DI-9's viewBox: the adaptive icon's layer square, in units
PIECES = 7                      # DI-9 is the seven tangram pieces
WINDOW_LOW = Fraction(18)       # the adaptive icon's visible window: units 18 to 90 (72 of the 108; S-57)
WINDOW_SIZE = Fraction(72)
PNG_SIZE = 512                  # Play's store icon, px
SUPERSAMPLE = 4                 # sample points per pixel and axis (4 x 4 per pixel)
OPAQUE = 255                    # every pixel's alpha: a 32-bit PNG, opaque (decisions.md 2026-10-22, the R7 amendment)

COLOUR_RE = re.compile(r"#[0-9A-Fa-f]{6}")
NUMBER = r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?"
TOKEN_RE = re.compile(r"\s*(?:([MLZ])|(" + NUMBER + r"))\s*,?")

HEADER = ("<!-- Generated by tools/make_icon.py from DI-9 (Spec/icon/option-A-foreground.svg and "
          "Spec/icon/option-A-background.txt), WO-009 S-57. Do not edit: change DI-9 or the tool and run it "
          "again (G-06). -->\n")


class Refused(Exception):
    """DI-9 holds something the tool does not convert."""


# ── Reading DI-9 ────────────────────────────────────────────────────────────────────────────────

def read_background(text):
    """The background file's one colour, as written (#RRGGBB); its line end may be LF or CRLF."""
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    if len(lines) != 1 or not COLOUR_RE.fullmatch(lines[0]):
        raise Refused("the background file must hold exactly one #RRGGBB colour, read %r" % text)
    return lines[0]


def parse_d(d):
    """
    The subpaths of a path's d, each a list of (x, y) Fractions. Only "M x y", then one or more "L x y", then "Z" is
    accepted per subpath: every point has its own command letter, as DI-9 writes them.
    """
    tokens = []
    pos = 0
    while pos < len(d):
        m = TOKEN_RE.match(d, pos)
        if not m or m.end() == pos:
            raise Refused("path data %r: %r at %d is not an absolute M, L or Z command or a number" % (d, d[pos:pos + 8], pos))
        tokens.append(m.group(1) or Fraction(m.group(2)))
        pos = m.end()

    def point(i):
        if i + 1 >= len(tokens) or isinstance(tokens[i], str) or isinstance(tokens[i + 1], str):
            raise Refused("path data %r: each M and L takes one x y pair" % d)
        return (tokens[i], tokens[i + 1])

    subpaths = []
    i = 0
    while i < len(tokens):
        if tokens[i] != "M":
            raise Refused("path data %r: a subpath must start with M" % d)
        points = [point(i + 1)]
        i += 3
        while i < len(tokens) and tokens[i] == "L":
            points.append(point(i + 1))
            i += 3
        if i >= len(tokens) or tokens[i] != "Z":
            raise Refused("path data %r: each M x y (L x y)+ subpath must end with Z" % d)
        i += 1
        if len(points) < 3:
            raise Refused("path data %r: a subpath needs at least three points" % d)
        subpaths.append(points)
    if not subpaths:
        raise Refused("path data %r: empty" % d)
    return subpaths


def read_svg(text):
    """The SVG's viewBox text and its paths, in order: [(fill, d, subpaths)]. Anything else is refused."""
    if "<!DOCTYPE" in text or "<!ENTITY" in text:
        raise Refused("the SVG has a DOCTYPE or an ENTITY")
    try:
        root = ET.fromstring(text)
    except ET.ParseError as e:
        raise Refused("the SVG does not parse: %s" % e)
    if root.tag != SVG_NS + "svg":
        raise Refused("the root element is %s, not an SVG <svg>" % root.tag)
    for name, value in root.attrib.items():
        if name == "viewBox":
            numbers = value.replace(",", " ").split()
            if len(numbers) != 4 or tuple(Fraction(n) for n in numbers) != VIEWBOX:
                raise Refused("the viewBox is %r, not \"0 0 108 108\"" % value)
        elif name in ("width", "height"):
            if value.strip() != "108":
                raise Refused("the root's %s is %r, not 108" % (name, value))
        else:
            raise Refused("the root <svg> has the attribute %s=%r" % (name, value))
    if "viewBox" not in root.attrib:
        raise Refused("the root <svg> has no viewBox")
    if (root.text or "").strip():
        raise Refused("the root <svg> holds text %r" % root.text.strip())
    paths = []
    for child in root:
        if not isinstance(child.tag, str) or child.tag != SVG_NS + "path":
            raise Refused("the SVG holds a <%s> element; only <path> is converted" % str(child.tag).replace(SVG_NS, ""))
        if len(child) or (child.text or "").strip() or (child.tail or "").strip():
            raise Refused("a <path> holds content or text")
        if set(child.attrib) != {"fill", "d"}:
            raise Refused("a <path> has the attributes %s; only fill and d are converted" % sorted(child.attrib))
        fill = child.attrib["fill"]
        if not COLOUR_RE.fullmatch(fill):
            raise Refused("a <path> fill is %r, not #RRGGBB" % fill)
        d = child.attrib["d"]
        paths.append((fill, d, parse_d(d)))
    if len(paths) != PIECES:
        raise Refused("the SVG holds %d paths; DI-9 has the %d tangram pieces" % (len(paths), PIECES))
    return root.attrib["viewBox"], paths


# ── The three XML resources ─────────────────────────────────────────────────────────────────────

def foreground_xml(viewbox, paths):
    w = viewbox.replace(",", " ").split()[2]
    h = viewbox.replace(",", " ").split()[3]
    out = [HEADER,
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n',
           '    android:width="108dp"\n',
           '    android:height="108dp"\n',
           '    android:viewportWidth="%s"\n' % w,
           '    android:viewportHeight="%s">\n' % h]
    for fill, d, _ in paths:
        out.append('    <path\n')
        out.append('        android:fillColor="%s"\n' % fill)
        out.append('        android:pathData="%s" />\n' % d)
    out.append('</vector>\n')
    return "".join(out)


def background_xml(colour):
    return (HEADER
            + "<resources>\n"
            + '    <color name="ic_launcher_background">%s</color>\n' % colour
            + "</resources>\n")


def adaptive_xml():
    return (HEADER
            + '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            + '    <background android:drawable="@color/ic_launcher_background" />\n'
            + '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
            + "</adaptive-icon>\n")


# ── The store PNG ───────────────────────────────────────────────────────────────────────────────

def rgb(colour):
    return tuple(int(colour[i:i + 2], 16) for i in (1, 3, 5))


def edges_of(subpaths):
    """Every edge (x0, y0, x1, y1) of the closed subpaths, the closing edge included; horizontal edges dropped."""
    out = []
    for points in subpaths:
        for i, (x0, y0) in enumerate(points):
            x1, y1 = points[(i + 1) % len(points)]
            if y0 != y1:
                out.append((x0, y0, x1, y1))
    return out


def spans(edges, y):
    """The x intervals [a, b) where the horizontal line at y is inside the edges' outline (nonzero rule)."""
    crossings = []
    for x0, y0, x1, y1 in edges:
        if y0 <= y < y1:
            crossings.append((x0 + (y - y0) * (x1 - x0) / (y1 - y0), 1))
        elif y1 <= y < y0:
            crossings.append((x0 + (y - y0) * (x1 - x0) / (y1 - y0), -1))
    crossings.sort()
    out = []
    winding = 0
    previous = None
    for x, direction in crossings:
        if winding != 0 and previous is not None and x > previous:
            out.append((previous, x))
        winding += direction
        previous = x
    return out


def store_png(background, paths):
    n = PNG_SIZE * SUPERSAMPLE                       # sample points per axis
    step = WINDOW_SIZE / n                           # units between sample points
    bg = rgb(background)
    deltas = [tuple(c - b for c, b in zip(rgb(fill), bg)) for fill, _, _ in paths]
    all_edges = [edges_of(subpaths) for _, _, subpaths in paths]
    per_pixel = SUPERSAMPLE * SUPERSAMPLE

    def first_sample_at_or_after(x):
        # sample k sits at WINDOW_LOW + (k + 1/2) * step; the smallest k with that >= x, clamped to [0, n]
        k = math.ceil((x - WINDOW_LOW) / step - Fraction(1, 2))
        return min(max(k, 0), n)

    raw = bytearray()
    for py in range(PNG_SIZE):
        acc = [[0, 0, 0] for _ in range(PNG_SIZE)]
        for sy in range(SUPERSAMPLE):
            y = WINDOW_LOW + (py * SUPERSAMPLE + sy + Fraction(1, 2)) * step
            top = [-1] * n
            for index, edges in enumerate(all_edges):
                for a, b in spans(edges, y):
                    c0 = first_sample_at_or_after(a)
                    c1 = first_sample_at_or_after(b)
                    if c1 > c0:
                        top[c0:c1] = [index] * (c1 - c0)
            for c, index in enumerate(top):
                if index >= 0:
                    d = deltas[index]
                    cell = acc[c // SUPERSAMPLE]
                    cell[0] += d[0]
                    cell[1] += d[1]
                    cell[2] += d[2]
        raw.append(0)                                # filter type 0 (None)
        for cell in acc:
            for channel in range(3):
                total = bg[channel] * per_pixel + cell[channel]
                raw.append((2 * total + per_pixel) // (2 * per_pixel))   # the mean, rounded half up
            raw.append(OPAQUE)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", PNG_SIZE, PNG_SIZE, 8, 6, 0, 0, 0)   # 8 bits per channel, colour type 6 (RGBA), no interlace
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + chunk(b"IEND", b""))


# ── Main ────────────────────────────────────────────────────────────────────────────────────────

def build(svg_text, background_text):
    """The four outputs as {relative path: bytes}; raises Refused before anything is made."""
    viewbox, paths = read_svg(svg_text)
    colour = read_background(background_text)
    return {
        FOREGROUND_XML: foreground_xml(viewbox, paths).encode("ascii"),
        BACKGROUND_XML: background_xml(colour).encode("ascii"),
        ADAPTIVE_XML: adaptive_xml().encode("ascii"),
        STORE_PNG: store_png(colour, paths),
    }


def read_text(path):
    with open(path, "rb") as f:
        data = f.read()
    if data.startswith(b"\xef\xbb\xbf"):
        data = data[3:]
    return data.decode("utf-8")


def main(argv=None):
    parser = argparse.ArgumentParser(description="Make the launcher and store icons from DI-9 (S-57).")
    parser.add_argument("--svg", default=SVG_DEFAULT)
    parser.add_argument("--background", default=BACKGROUND_DEFAULT)
    parser.add_argument("--out-root", default=ROOT)
    args = parser.parse_args(argv)
    try:
        made = build(read_text(args.svg), read_text(args.background))
    except Refused as e:
        sys.stderr.write("make_icon: refused - %s; no file written\n" % e)
        return 1
    except (OSError, UnicodeDecodeError) as e:
        sys.stderr.write("make_icon: cannot read DI-9 - %s; no file written\n" % e)
        return 1
    for rel in OUTPUTS:
        path = os.path.join(args.out_root, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(made[rel])
        print("%-55s %7d bytes" % (rel, len(made[rel])))
    print("4 files written under %s" % args.out_root.replace("\\", "/"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
