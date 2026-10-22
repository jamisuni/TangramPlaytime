#!/usr/bin/env python3
"""
Tests for tools/make_icon.py (WO-009, seam S-57). The tool test of TASK-067; plain asserts, run directly:

    python tools/tests/test_make_icon.py

Every run of the tool here writes into a temporary folder (--out-root), never into the repository. What is checked:

- Two runs write the same bytes, and those bytes equal the four files in the tree (G-06: the tree's icon files are
  the tool's output from DI-9, unedited).
- The vector foreground holds DI-9's paths in order, each fill and d copied verbatim (read here from the SVG with this
  test's own parser); the background colour resource holds DI-9's colour; the adaptive icon has exactly that
  background and that foreground and no monochrome layer.
- Each XML opens with a comment naming the tool and DI-9, and every file is LF only, without a BOM.
- The store PNG is 512 x 512, a 32-bit PNG (8-bit RGBA, colour type 6) with every alpha 255, only IHDR / IDAT / IEND
  (decisions.md 2026-10-22 "Amends ... (R7): the 512 x 512 store icon is a 32-bit PNG (RGBA, colour type 6) with every
  alpha 255 ..."). The format check must fail on two crafted files made from the tool's own pixels by this test's own
  encoder: the same RGBA pixels with one alpha at 254, and the same pixels as 24-bit RGB (colour type 2); the same
  RGBA pixels re-encoded unchanged pass it (the control). Decoded here with this test's own reader: the pixel under a
  point well inside each DI-9 piece is that piece's colour exactly, the pixel under each of the four background points
  and the window's corners is the background exactly, and the edges are anti-aliased (colours other than the eight
  occur).
- The tool converts and never redraws: a copy of DI-9 changed in one place (a transform, a text, an image, a group, a
  rect, a stroke, a relative or a curve command, another viewBox, a colour name, six paths) is refused with exit 1 and
  no file written; the unchanged copy, run the same way, passes (the control).
"""

import math
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
TOOL = os.path.join(ROOT, "tools", "make_icon.py")
SVG = os.path.join(ROOT, "Spec", "icon", "option-A-foreground.svg")
BACKGROUND = os.path.join(ROOT, "Spec", "icon", "option-A-background.txt")
OUTPUTS = (
    "app/src/main/res/drawable/ic_launcher_foreground.xml",
    "app/src/main/res/values/ic_launcher_background.xml",
    "app/src/main/res/mipmap-anydpi/ic_launcher.xml",
    "release/pictures/icon-512.png",
)
SVG_NS = "{http://www.w3.org/2000/svg}"
ANDROID = "{http://schemas.android.com/apk/res/android}"


def run_tool(out_root, svg=SVG, background=BACKGROUND):
    env = dict(os.environ, PYTHONUTF8="1")
    return subprocess.run(
        [sys.executable, TOOL, "--svg", svg, "--background", background, "--out-root", out_root],
        capture_output=True, text=True, env=env)


def read_bytes(path):
    with open(path, "rb") as f:
        return f.read()


def files_under(folder):
    found = []
    for dirpath, _, names in os.walk(folder):
        found.extend(os.path.join(dirpath, n) for n in names)
    return found


def di9_paths():
    """DI-9's paths as (fill, d), read with this test's own parse of the SVG."""
    root = ET.fromstring(read_bytes(SVG).decode("utf-8"))
    return [(p.attrib["fill"], p.attrib["d"]) for p in root.iter(SVG_NS + "path")]


def di9_background():
    return read_bytes(BACKGROUND).decode("utf-8").strip()


def polygon(d):
    """The vertices of a one-subpath d of M / L / Z, as floats (this test's own reading)."""
    numbers = [float(t) for t in d.replace("M", " ").replace("L", " ").replace("Z", " ").split()]
    return list(zip(numbers[0::2], numbers[1::2]))


def centroid(points):
    """The area centroid of a simple polygon (inside it when the polygon is convex, as each DI-9 piece is)."""
    a = cx = cy = 0.0
    for (x0, y0), (x1, y1) in zip(points, points[1:] + points[:1]):
        cross = x0 * y1 - x1 * y0
        a += cross
        cx += (x0 + x1) * cross
        cy += (y0 + y1) * cross
    return cx / (3 * a), cy / (3 * a)


def hex_rgb(colour):
    return tuple(int(colour[i:i + 2], 16) for i in (1, 3, 5))


def decode_png(data):
    """
    (width, height, bit depth, colour type, chunk types, rows of pixel tuples) of a non-interlaced 8-bit RGB (colour
    type 2: 3-tuples) or RGBA (colour type 6: 4-tuples) PNG.
    """
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "PNG signature"
    pos = 8
    kinds = []
    idat = b""
    ihdr = None
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        crc = struct.unpack(">I", data[pos + 8 + length:pos + 12 + length])[0]
        assert crc == zlib.crc32(kind + body) & 0xFFFFFFFF, "CRC of %s" % kind
        kinds.append(kind.decode("ascii"))
        if kind == b"IHDR":
            ihdr = struct.unpack(">IIBBBBB", body)
        elif kind == b"IDAT":
            idat += body
        pos += 12 + length
    width, height, depth, colour_type, _, _, interlace = ihdr
    assert depth == 8 and colour_type in (2, 6) and interlace == 0, "this reader takes 8-bit RGB or RGBA, not interlaced"
    bpp = 3 if colour_type == 2 else 4
    raw = zlib.decompress(idat)
    stride = width * bpp
    rows = []
    previous = bytearray(stride)
    at = 0
    for _ in range(height):
        kind = raw[at]
        line = bytearray(raw[at + 1:at + 1 + stride])
        at += 1 + stride
        for i in range(stride):
            left = line[i - bpp] if i >= bpp else 0
            up = previous[i]
            upper_left = previous[i - bpp] if i >= bpp else 0
            if kind == 1:
                line[i] = (line[i] + left) & 0xFF
            elif kind == 2:
                line[i] = (line[i] + up) & 0xFF
            elif kind == 3:
                line[i] = (line[i] + (left + up) // 2) & 0xFF
            elif kind == 4:
                p = left + up - upper_left
                pa, pb, pc = abs(p - left), abs(p - up), abs(p - upper_left)
                pred = left if pa <= pb and pa <= pc else (up if pb <= pc else upper_left)
                line[i] = (line[i] + pred) & 0xFF
            else:
                assert kind == 0, "filter type %d" % kind
        rows.append([tuple(line[i:i + bpp]) for i in range(0, stride, bpp)])
        previous = line
    assert at == len(raw), "trailing image data"
    return width, height, depth, colour_type, kinds, rows


def encode_png(rows, colour_type):
    """This test's own encoder (filter 0, one IDAT): rows of RGB (colour type 2) or RGBA (colour type 6) tuples."""
    def chunk(kind, body):
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)
    raw = b"".join(b"\x00" + bytes(v for p in row for v in p) for row in rows)
    ihdr = struct.pack(">IIBBBBB", len(rows[0]), len(rows), 8, colour_type, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")


def store_png_problems(data):
    """
    What breaks S-57's store-icon format in the PNG [data] (decisions.md 2026-10-22, the R7 amendment): 512 x 512, a
    32-bit PNG (8-bit RGBA, colour type 6), every alpha 255, only IHDR / IDAT / IEND. An empty list when it holds.
    """
    width, height, depth, colour_type, kinds, rows = decode_png(data)
    problems = []
    if (width, height) != (512, 512):
        problems.append("size %d x %d, not 512 x 512" % (width, height))
    if depth != 8 or colour_type != 6:
        problems.append("bit depth %d, colour type %d: not a 32-bit PNG (8-bit RGBA, colour type 6)" % (depth, colour_type))
    if not (kinds[0] == "IHDR" and kinds[-1] == "IEND" and set(kinds[1:-1]) == {"IDAT"}):
        problems.append("chunks %r, not only IHDR, IDAT, IEND" % kinds)
    if colour_type == 6:
        below = sum(1 for row in rows for p in row if p[3] != 255)
        if below:
            problems.append("%d pixel(s) with an alpha other than 255" % below)
    return problems


def pixel_at_unit(rows, x, y):
    """The store PNG's colour (RGB) under the DI-9 point (x, y): the window is units 18 to 90 over 512 px (S-57)."""
    return rows[int(math.floor((y - 18) * 512 / 72))][int(math.floor((x - 18) * 512 / 72))][:3]


# ── Tests ───────────────────────────────────────────────────────────────────────────────────────

def test_two_runs_write_the_same_bytes_equal_to_the_tree():
    with tempfile.TemporaryDirectory() as a, tempfile.TemporaryDirectory() as b:
        ra, rb = run_tool(a), run_tool(b)
        assert ra.returncode == 0, ra.stderr
        assert rb.returncode == 0, rb.stderr
        assert sorted(os.path.relpath(p, a).replace(os.sep, "/") for p in files_under(a)) == sorted(OUTPUTS), \
            "the tool writes exactly the four S-57 files"
        for rel in OUTPUTS:
            first = read_bytes(os.path.join(a, *rel.split("/")))
            second = read_bytes(os.path.join(b, *rel.split("/")))
            assert first == second, "%s: two runs differ (G-06: deterministic)" % rel
            in_tree = os.path.join(ROOT, *rel.split("/"))
            assert os.path.isfile(in_tree), "%s is not in the tree: run python tools/make_icon.py" % rel
            assert read_bytes(in_tree) == first, "%s in the tree is not the tool's output from DI-9 (G-06)" % rel


def test_the_resources_copy_di9_verbatim():
    with tempfile.TemporaryDirectory() as out:
        assert run_tool(out).returncode == 0
        vector = ET.fromstring(read_bytes(os.path.join(out, *OUTPUTS[0].split("/"))))
        assert vector.tag == "vector"
        assert vector.attrib[ANDROID + "width"] == "108dp" and vector.attrib[ANDROID + "height"] == "108dp"
        assert vector.attrib[ANDROID + "viewportWidth"] == "108" and vector.attrib[ANDROID + "viewportHeight"] == "108"
        got = [(p.attrib[ANDROID + "fillColor"], p.attrib[ANDROID + "pathData"]) for p in vector]
        assert all(p.tag == "path" for p in vector), "the vector holds only paths (no text, no image)"
        assert got == di9_paths(), "the vector's paths are DI-9's, in order, fill and d verbatim: %r" % got
        assert len(got) == 7

        colours = ET.fromstring(read_bytes(os.path.join(out, *OUTPUTS[1].split("/"))))
        assert colours.tag == "resources" and len(colours) == 1
        assert colours[0].tag == "color" and colours[0].attrib == {"name": "ic_launcher_background"}
        assert colours[0].text == di9_background() == "#FBF7EE"

        icon = ET.fromstring(read_bytes(os.path.join(out, *OUTPUTS[2].split("/"))))
        assert icon.tag == "adaptive-icon"
        layers = [(layer.tag, layer.attrib) for layer in icon]
        assert layers == [
            ("background", {ANDROID + "drawable": "@color/ic_launcher_background"}),
            ("foreground", {ANDROID + "drawable": "@drawable/ic_launcher_foreground"}),
        ], "exactly the background and the foreground, no monochrome layer: %r" % layers


def test_each_file_is_lf_without_bom_and_each_xml_names_the_tool_and_di9():
    with tempfile.TemporaryDirectory() as out:
        assert run_tool(out).returncode == 0
        for rel in OUTPUTS[:3]:
            data = read_bytes(os.path.join(out, *rel.split("/")))
            assert not data.startswith(b"\xef\xbb\xbf"), "%s: no BOM" % rel
            assert b"\r" not in data and data.endswith(b"\n"), "%s: LF line ends" % rel
            data.decode("ascii")
            assert data.startswith(b"<!--"), "%s opens with a comment" % rel
            comment = data[:data.index(b"-->")]
            assert b"tools/make_icon.py" in comment and b"DI-9" in comment, "%s: the comment names the tool and DI-9" % rel


def test_the_store_png_is_a_32_bit_opaque_png_and_the_check_can_fail():
    with tempfile.TemporaryDirectory() as out:
        assert run_tool(out).returncode == 0
        data = read_bytes(os.path.join(out, *OUTPUTS[3].split("/")))
    assert store_png_problems(data) == [], "the store icon: %r" % store_png_problems(data)
    _, _, _, _, _, rows = decode_png(data)
    # The control: the tool's own pixels, re-encoded unchanged by this test's encoder, pass the same check.
    assert store_png_problems(encode_png(rows, 6)) == [], "the re-encoded control fails: the encoder, not the file"
    # Must fail: one alpha at 254 (the pixel at row 256, column 100), and the same pixels as 24-bit RGB.
    one_below = [list(row) for row in rows]
    r, g, b, _ = one_below[256][100]
    one_below[256][100] = (r, g, b, 254)
    problems = store_png_problems(encode_png(one_below, 6))
    assert problems == ["1 pixel(s) with an alpha other than 255"], "one alpha at 254 must fail the check: %r" % problems
    problems = store_png_problems(encode_png([[p[:3] for p in row] for row in rows], 2))
    assert len(problems) == 1 and "colour type 2" in problems[0], "a 24-bit RGB file must fail the check: %r" % problems


def test_the_store_png_shows_di9_in_the_visible_window():
    with tempfile.TemporaryDirectory() as out:
        assert run_tool(out).returncode == 0
        width, height, depth, colour_type, kinds, rows = decode_png(read_bytes(os.path.join(out, *OUTPUTS[3].split("/"))))
    assert (width, height, depth, colour_type) == (512, 512, 8, 6), "512 x 512, 8-bit RGBA"
    background = hex_rgb(di9_background())
    pieces = [(hex_rgb(fill), polygon(d)) for fill, d in di9_paths()]
    for colour, points in pieces:
        x, y = centroid(points)
        assert pixel_at_unit(rows, x, y) == colour, "inside the piece %r at (%.2f, %.2f)" % (colour, x, y)
    for x, y in ((54, 22), (54, 86), (22, 54), (86, 54), (18.05, 18.05), (89.95, 18.05), (18.05, 89.95), (89.95, 89.95)):
        assert pixel_at_unit(rows, x, y) == background, "the background at (%s, %s)" % (x, y)
    eight = {background} | {colour for colour, _ in pieces}
    blended = {p[:3] for row in rows for p in row} - eight
    assert len(blended) > 0, "the edges are anti-aliased"


def test_anything_but_di9s_paths_is_refused_and_nothing_is_written():
    svg = read_bytes(SVG).decode("utf-8")
    first = '<path fill="#E8505B" d="M33.32 32.44 L74.68 32.44 L54 53.12 Z"/>'
    assert svg.count(first) == 1, "the crafted variants start from DI-9's first path"
    variants = {
        "a transform on a path": (first, first.replace("<path ", '<path transform="rotate(45 54 54)" ')),
        "a text element": (first, first + '<text x="10" y="10">Tangram</text>'),
        "an image element": (first, first + '<image href="x.png" width="10" height="10"/>'),
        "a group": (first, "<g>" + first + "</g>"),
        "a rect element": (first, first + '<rect x="1" y="1" width="5" height="5" fill="#000000"/>'),
        "a stroke": (first, first.replace("<path ", '<path stroke="#000000" ')),
        "a relative command": (first, first.replace("L74.68 32.44", "l41.36 0")),
        "a curve command": (first, first.replace("L74.68 32.44", "C40 30 60 30 74.68 32.44")),
        "a horizontal command": (first, first.replace("L74.68 32.44", "H74.68")),
        "another viewBox": ('viewBox="0 0 108 108"', 'viewBox="0 0 100 100"'),
        "a transform on the root": ('<svg ', '<svg transform="scale(2)" '),
        "a colour name": ('fill="#E8505B"', 'fill="red"'),
        "six paths": (first, ""),
    }
    with tempfile.TemporaryDirectory() as tmp:
        bg = os.path.join(tmp, "background.txt")
        shutil.copyfile(BACKGROUND, bg)
        control = os.path.join(tmp, "control.svg")
        with open(control, "w", encoding="utf-8", newline="\n") as f:
            f.write(svg)
        out = os.path.join(tmp, "out-control")
        result = run_tool(out, svg=control, background=bg)
        assert result.returncode == 0 and len(files_under(out)) == 4, "the unchanged copy passes: %s" % result.stderr

        for name, (old, new) in variants.items():
            assert svg.count(old) == 1, "%s: the change applies once" % name
            path = os.path.join(tmp, "variant.svg")
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(svg.replace(old, new))
            out = os.path.join(tmp, "out-variant")
            result = run_tool(out, svg=path, background=bg)
            assert result.returncode == 1, "%s: exit %d, expected 1 (refused)" % (name, result.returncode)
            assert "refused" in result.stderr, "%s: %s" % (name, result.stderr)
            assert not os.path.exists(out) or files_under(out) == [], "%s: a refused run writes no file" % name

        for name, text in (("no hash", "FBF7EE\n"), ("two colours", "#FBF7EE\n#000000\n"), ("empty", "")):
            path = os.path.join(tmp, "bad-background.txt")
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)
            out = os.path.join(tmp, "out-background")
            result = run_tool(out, svg=control, background=path)
            assert result.returncode == 1, "background %s: exit %d, expected 1" % (name, result.returncode)
            assert not os.path.exists(out) or files_under(out) == [], "background %s: no file written" % name


TESTS = [
    test_two_runs_write_the_same_bytes_equal_to_the_tree,
    test_the_resources_copy_di9_verbatim,
    test_each_file_is_lf_without_bom_and_each_xml_names_the_tool_and_di9,
    test_the_store_png_is_a_32_bit_opaque_png_and_the_check_can_fail,
    test_the_store_png_shows_di9_in_the_visible_window,
    test_anything_but_di9s_paths_is_refused_and_nothing_is_written,
]


def main():
    failed = 0
    for test in TESTS:
        try:
            test()
            print("PASS " + test.__name__)
        except AssertionError as e:
            failed += 1
            print("FAIL %s: %s" % (test.__name__, e))
    print("%d of %d tests passed" % (len(TESTS) - failed, len(TESTS)))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
