"""Make the Play feature graphic (the banner) from DI-9 and the title.

WO-009 TASK-068, seam S-60 (`tasks.md` § WO-009); `decisions.md` 2026-10-22
"The store pictures: a banner from the icon and the title; ..." (R11).

Writes two files under `release/pictures/`:

- `feature-graphic.svg`, 1024 x 500: DI-9's background colour
  (`Spec/icon/option-A-background.txt`), DI-9's seven pieces (the paths of
  `Spec/icon/option-A-foreground.svg`, copied verbatim, scaled and placed in
  the left part) and the title "Tangram Playtime" in the system sans-serif
  (no web font, ADR-005), dark, on the right. No other text: no promise or
  price words (CHG-001). The SVG is the source (G-06); its bytes are the same
  on every run.
- `feature-graphic.png`: the SVG rendered by headless Chrome, rewritten as an
  8-bit RGB PNG without alpha (Play takes JPEG or 24-bit PNG). Chrome's
  raster is not byte-reproducible across Chrome versions, so the PNG is not
  compared byte for byte; its size and format are.

The tool converts DI-9, it never redraws it: anything in the SVG other than
`path` elements with a `fill` and a `d` of absolute M, L and Z commands is
refused with exit 1.

Usage (from the repository root):

    python tools/make_feature_graphic.py [--chrome <chrome.exe>]

Chrome is taken from `--chrome`, else the CHROME environment variable, else
the usual Windows install path, else `chrome` / `google-chrome` /
`chromium` on the PATH. Each Chrome run is bounded by 60 seconds.
Stdlib only.
"""

from __future__ import annotations

import argparse
import os
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zlib
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
FOREGROUND_SVG = REPO / "Spec" / "icon" / "option-A-foreground.svg"
BACKGROUND_TXT = REPO / "Spec" / "icon" / "option-A-background.txt"
OUT_DIR = REPO / "release" / "pictures"
OUT_SVG = OUT_DIR / "feature-graphic.svg"
OUT_PNG = OUT_DIR / "feature-graphic.png"

SVG_NS = "http://www.w3.org/2000/svg"
TITLE = "Tangram Playtime"

# The banner's layout (S-60 fixes the size, the parts and their sides; the
# numbers below are this tool's: proposed as a decisions.md row by TASK-068).
WIDTH, HEIGHT = 1024, 500
PIECES_SIDE = 360          # the pieces' bounding square, in px
PIECES_LEFT = 72           # its left edge; it is centred vertically
TITLE_SIZE = 56            # px, bold
TITLE_COLOUR = "#2B2A33"   # dark
CHROME_TIMEOUT_S = 60

WINDOWS_CHROME = Path("C:/Program Files/Google/Chrome/Application/chrome.exe")
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


class Refused(Exception):
    """DI-9 holds something this tool does not convert, or a render failed."""


# --------------------------------------------------------------------- DI-9

_COMMAND = re.compile(r"[A-Za-z]")
_NUMBER = re.compile(r"-?(?:\d+\.?\d*|\.\d+)")
_COLOUR = re.compile(r"#[0-9A-Fa-f]{6}")


def _local(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _parse_d(d: str) -> list[tuple[float, float]]:
    """Return the points of a `d` made of absolute M, L and Z only."""
    tokens = re.findall(r"[A-Za-z]|-?(?:\d+\.?\d*|\.\d+)|\S", d)
    points: list[tuple[float, float]] = []
    i = 0
    command = None
    while i < len(tokens):
        token = tokens[i]
        if _COMMAND.fullmatch(token):
            if token not in ("M", "L", "Z"):
                raise Refused(f"path command {token!r} is not M, L or Z")
            command = token
            i += 1
            if token == "Z":
                command = None
            continue
        if not _NUMBER.fullmatch(token):
            raise Refused(f"unexpected {token!r} in a path's d")
        if command not in ("M", "L") or i + 1 >= len(tokens) \
                or not _NUMBER.fullmatch(tokens[i + 1]):
            raise Refused("a path's d is not pairs of numbers after M or L")
        points.append((float(token), float(tokens[i + 1])))
        i += 2
    if not points:
        raise Refused("a path has no points")
    return points


def read_di9() -> tuple[str, list[tuple[str, str, list[tuple[float, float]]]]]:
    """Return DI-9's background colour and its paths (fill, d, points)."""
    background = BACKGROUND_TXT.read_text(encoding="utf-8").strip()
    if not _COLOUR.fullmatch(background):
        raise Refused(f"{BACKGROUND_TXT.name} is not one #RRGGBB colour")

    root = ET.parse(FOREGROUND_SVG).getroot()
    if root.tag != f"{{{SVG_NS}}}svg":
        raise Refused("the foreground is not an SVG document")
    if root.get("viewBox") != "0 0 108 108":
        raise Refused("the foreground's viewBox is not 0 0 108 108")
    if "transform" in root.attrib:
        raise Refused("the svg element has a transform")

    paths = []
    for element in root:
        if _local(element.tag) != "path":
            raise Refused(f"element <{_local(element.tag)}> is not a path")
        if len(element):
            raise Refused("a path has child elements")
        if set(element.attrib) != {"fill", "d"}:
            raise Refused(f"a path has attributes {sorted(element.attrib)}, "
                          "not exactly fill and d")
        fill = element.get("fill")
        if not _COLOUR.fullmatch(fill):
            raise Refused(f"a path's fill {fill!r} is not #RRGGBB")
        d = element.get("d")
        paths.append((fill, d, _parse_d(d)))
    if len(paths) != 7:
        raise Refused(f"the foreground has {len(paths)} paths, not seven")
    return background, paths


# ---------------------------------------------------------------------- SVG

def _num(value: float) -> str:
    text = f"{value:.4f}".rstrip("0").rstrip(".")
    return "0" if text in ("", "-0") else text


def build_svg(background: str, paths) -> str:
    xs = [x for _, _, points in paths for x, _ in points]
    ys = [y for _, _, points in paths for _, y in points]
    min_x, max_x, min_y, max_y = min(xs), max(xs), min(ys), max(ys)
    scale = PIECES_SIDE / max(max_x - min_x, max_y - min_y)
    box_top = (HEIGHT - PIECES_SIDE) / 2
    left = PIECES_LEFT + (PIECES_SIDE - (max_x - min_x) * scale) / 2
    top = box_top + (PIECES_SIDE - (max_y - min_y) * scale) / 2
    tx = left - min_x * scale
    ty = top - min_y * scale
    # The title is centred in the part right of the pieces.
    title_x = (PIECES_LEFT + PIECES_SIDE + WIDTH) / 2
    title_y = HEIGHT / 2

    lines = [
        "<!-- Generated by tools/make_feature_graphic.py from DI-9 "
        "(Spec/icon/option-A-foreground.svg, Spec/icon/option-A-background.txt)"
        " and the store title (REQ-048 rule 1). Do not edit: run the tool. -->",
        f'<svg xmlns="{SVG_NS}" width="{WIDTH}" height="{HEIGHT}" '
        f'viewBox="0 0 {WIDTH} {HEIGHT}">',
        f'  <rect x="0" y="0" width="{WIDTH}" height="{HEIGHT}" '
        f'fill="{background}"/>',
        f'  <g transform="translate({_num(tx)} {_num(ty)}) '
        f'scale({_num(scale)})">',
    ]
    for fill, d, _ in paths:
        lines.append(f'    <path fill="{fill}" d="{d}"/>')
    lines += [
        "  </g>",
        f'  <text x="{_num(title_x)}" y="{_num(title_y)}" '
        f'font-family="sans-serif" font-size="{TITLE_SIZE}" '
        f'font-weight="700" fill="{TITLE_COLOUR}" text-anchor="middle" '
        f'dominant-baseline="central">{TITLE}</text>',
        "</svg>",
    ]
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------- PNG

def _chunks(data: bytes):
    if not data.startswith(PNG_SIGNATURE):
        raise Refused("Chrome's screenshot is not a PNG")
    pos = len(PNG_SIGNATURE)
    while pos + 8 <= len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        if len(body) != length:
            raise Refused("Chrome's screenshot is truncated")
        yield kind, body
        pos += 12 + length
        if kind == b"IEND":
            return
    raise Refused("Chrome's screenshot has no IEND chunk")


def _paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    return b if pb <= pc else c


def decode_png(data: bytes) -> tuple[int, int, list[bytearray], int]:
    """Decode an 8-bit, non-interlaced RGB or RGBA PNG.

    Returns (width, height, rows, channels) with the filters undone.
    """
    header = None
    idat = bytearray()
    for kind, body in _chunks(data):
        if kind == b"IHDR":
            header = struct.unpack(">IIBBBBB", body)
        elif kind == b"IDAT":
            idat += body
    if header is None:
        raise Refused("Chrome's screenshot has no IHDR chunk")
    width, height, depth, colour_type, _, _, interlace = header
    if depth != 8 or colour_type not in (2, 6) or interlace != 0:
        raise Refused(f"Chrome's screenshot is depth {depth}, colour type "
                      f"{colour_type}, interlace {interlace}: not decoded")
    channels = 3 if colour_type == 2 else 4
    stride = width * channels
    raw = zlib.decompress(bytes(idat))
    if len(raw) != height * (stride + 1):
        raise Refused("Chrome's screenshot has the wrong amount of pixel data")
    rows: list[bytearray] = []
    previous = bytearray(stride)
    for r in range(height):
        start = r * (stride + 1)
        kind = raw[start]
        row = bytearray(raw[start + 1:start + 1 + stride])
        if kind == 1:
            for i in range(channels, stride):
                row[i] = (row[i] + row[i - channels]) & 0xFF
        elif kind == 2:
            for i in range(stride):
                row[i] = (row[i] + previous[i]) & 0xFF
        elif kind == 3:
            for i in range(stride):
                left = row[i - channels] if i >= channels else 0
                row[i] = (row[i] + ((left + previous[i]) >> 1)) & 0xFF
        elif kind == 4:
            for i in range(stride):
                left = row[i - channels] if i >= channels else 0
                up_left = previous[i - channels] if i >= channels else 0
                row[i] = (row[i] + _paeth(left, previous[i], up_left)) & 0xFF
        elif kind != 0:
            raise Refused(f"Chrome's screenshot uses filter type {kind}")
        rows.append(row)
        previous = row
    return width, height, rows, channels


def to_rgb(rows: list[bytearray], channels: int, background: str) -> list[bytes]:
    """Drop the alpha channel; a pixel that is not opaque is laid on DI-9's
    background colour first (the SVG paints it everywhere, so Chrome's
    screenshot is opaque in practice)."""
    if channels == 3:
        return [bytes(row) for row in rows]
    bg = bytes.fromhex(background[1:])
    out = []
    for row in rows:
        rgb = bytearray(len(row) // 4 * 3)
        for p in range(len(row) // 4):
            r, g, b, a = row[4 * p:4 * p + 4]
            if a != 255:
                r = (r * a + bg[0] * (255 - a) + 127) // 255
                g = (g * a + bg[1] * (255 - a) + 127) // 255
                b = (b * a + bg[2] * (255 - a) + 127) // 255
            rgb[3 * p:3 * p + 3] = bytes((r, g, b))
        out.append(bytes(rgb))
    return out


def encode_rgb_png(width: int, height: int, rows: list[bytes]) -> bytes:
    """An 8-bit RGB PNG (colour type 2), no alpha, non-interlaced."""
    def chunk(kind: bytes, body: bytes) -> bytes:
        return (struct.pack(">I", len(body)) + kind + body
                + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF))

    raw = b"".join(b"\x00" + row for row in rows)
    return (PNG_SIGNATURE
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


# ------------------------------------------------------------------- Chrome

def find_chrome(explicit: str | None) -> Path:
    candidates = [explicit, os.environ.get("CHROME")]
    for candidate in candidates:
        if candidate:
            path = Path(candidate.strip('"'))
            if path.is_file():
                return path
            raise Refused(f"Chrome not found at {candidate}")
    if WINDOWS_CHROME.is_file():
        return WINDOWS_CHROME
    for name in ("chrome", "google-chrome", "chromium", "chromium-browser"):
        found = shutil.which(name)
        if found:
            return Path(found)
    raise Refused("Chrome not found: pass --chrome or set CHROME")


def render(chrome: Path, svg: Path, work: Path) -> bytes:
    shot = work / "chrome-shot.png"
    command = [
        str(chrome),
        "--headless=new",
        f"--screenshot={shot}",
        f"--window-size={WIDTH},{HEIGHT}",
        "--hide-scrollbars",
        "--force-device-scale-factor=1",
        f"--user-data-dir={work / 'profile'}",
        "--no-first-run",
        "--no-default-browser-check",
        "--disable-extensions",
        svg.resolve().as_uri(),
    ]
    try:
        result = subprocess.run(command, capture_output=True, text=True,
                                timeout=CHROME_TIMEOUT_S)
    except subprocess.TimeoutExpired:
        raise Refused(f"Chrome did not finish within {CHROME_TIMEOUT_S} s")
    if not shot.is_file():
        raise Refused(f"Chrome wrote no screenshot (exit {result.returncode}): "
                      f"{result.stderr.strip()[-400:]}")
    return shot.read_bytes()


# --------------------------------------------------------------------- main

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Make release/pictures/feature-graphic.svg and .png "
                    "from DI-9 and the title (S-60).")
    parser.add_argument("--chrome", help="path to chrome.exe")
    args = parser.parse_args(argv)
    try:
        background, paths = read_di9()
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        with open(OUT_SVG, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(build_svg(background, paths))
        print(f"wrote {OUT_SVG.relative_to(REPO).as_posix()}")

        chrome = find_chrome(args.chrome)
        work = Path(tempfile.mkdtemp(prefix="feature-graphic-"))
        try:
            shot = render(chrome, OUT_SVG, work)
        finally:
            shutil.rmtree(work, ignore_errors=True)
        width, height, rows, channels = decode_png(shot)
        if (width, height) != (WIDTH, HEIGHT):
            raise Refused(f"Chrome's screenshot is {width} x {height}, "
                          f"not {WIDTH} x {HEIGHT}")
        png = encode_rgb_png(width, height, to_rgb(rows, channels, background))
        OUT_PNG.write_bytes(png)
        print(f"wrote {OUT_PNG.relative_to(REPO).as_posix()} "
              f"({WIDTH} x {HEIGHT}, 8-bit RGB, no alpha; rendered by {chrome.name})")
        return 0
    except (Refused, OSError, ET.ParseError, zlib.error) as error:
        print(f"make_feature_graphic: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
