"""
The mark command (WO-009 TASK-063; tasks.md § WO-009 › Seams S-54; decisions.md 2026-10-22 "The mark command:
status, mark and unmark, one field changed byte for byte.").

A person marks a puzzle for release after solving it and approving its picture (REQ-039 rule 2; owner rule 7,
G-05). Only the owner runs `mark` or `unmark` on Tangrams/; stations run `status` there and `mark` on copies only.

  python tools/mark_puzzles.py status [--list]          read-only: the marked count per category, the release
                                                        bar and release-ready yes or no; --list adds the marked ids
  python tools/mark_puzzles.py mark <id> [<id> ...]     provenance.reviewedByHuman false -> true
  python tools/mark_puzzles.py unmark <id> [<id> ...]   provenance.reviewedByHuman true -> false
  --folder <dir> (before or after the command)          another folder; the default is the repository's Tangrams/

The puzzle files are the folder's top-level *.json but *.schema.json. A puzzle is marked when its
provenance.reviewedByHuman is JSON true; a missing provenance or field reads false (decisions.md 2026-10-11).

`mark` and `unmark` check every id before any write: the file <id>.json exists, parses as JSON and holds
provenance.reviewedByHuman as a boolean. One unknown id or missing field refuses the whole call (exit 2, nothing
written). Otherwise the only change in a file is the `true` or `false` after its one "reviewedByHuman" and colon;
every other byte is kept (UTF-8, LF, no BOM, the final newline as it was). The new text is re-parsed and must equal
the old puzzle with only that boolean flipped, or the call is refused.

The release bar is decisions.md 2026-10-22 "The release-readiness bar, and the store build refuses below it." (S-56's
five items): REQ-042 A1..A3, REQ-045's statement and rule 3, REQ-041's statement.

Python 3, stdlib only.
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_FOLDER = ROOT / "Tangrams"

# Spec/03-puzzle-format.md §2, the `category` row, in its order (tangram-puzzle/1 is locked).
CATEGORIES = ("shapes", "animals", "people", "things", "vehicles", "nature", "letters", "numbers")

# The release bar (S-56). "Rating 1" in REQ-045 and REQ-041 is `difficulty` 1 (Spec/03 §2).
BAR_PUZZLES = 20              # REQ-042 A1 and rule 1: at least 20 marked puzzles
BAR_CATEGORIES = 4            # REQ-042 A2 and rule 2: at least four categories
BAR_SQUARE = "shapes-square"  # REQ-042 A3: the classic square
BAR_MINI_PIECES = 3           # REQ-045 statement: a mini of exactly three pieces; rule 3: rating 1
BAR_WARMUPS = 4               # REQ-041 statement: at least four warm-ups of rating 1
EASIEST = 1

KEY = b'"reviewedByHuman"'
FIELD = re.compile(rb'("reviewedByHuman"[ \t\r\n]*:[ \t\r\n]*)(true|false)')


class Refused(Exception):
    pass


def puzzle_files(folder):
    return sorted(p for p in Path(folder).glob("*.json") if p.is_file() and not p.name.endswith(".schema.json"))


def read_puzzle(path):
    data = path.read_bytes()
    return data, json.loads(data.decode("utf-8"))


def is_marked(puzzle):
    provenance = puzzle.get("provenance") if isinstance(puzzle, dict) else None
    return isinstance(provenance, dict) and provenance.get("reviewedByHuman") is True


def kind_of(puzzle):
    return puzzle.get("kind", "full")


def pieces_of(puzzle):
    solution = puzzle.get("solution")
    return len(solution) if isinstance(solution, list) else None


def is_easiest(puzzle):
    difficulty = puzzle.get("difficulty")
    return difficulty == EASIEST and not isinstance(difficulty, bool)


def title_of(puzzle):
    title = puzzle.get("title") if isinstance(puzzle, dict) else None
    return title.get("en", "") if isinstance(title, dict) else ""


def survey(folder):
    """[(id, puzzle or None)] for every puzzle file, and a warning per file that does not read as a JSON object."""
    puzzles, warnings = [], []
    for path in puzzle_files(folder):
        try:
            _, puzzle = read_puzzle(path)
        except (UnicodeDecodeError, ValueError) as e:
            puzzle = None
            warnings.append(f"warning: {path.name} is not UTF-8 JSON ({e}); counted as unmarked")
        if puzzle is not None and not isinstance(puzzle, dict):
            puzzle = None
            warnings.append(f"warning: {path.name} is not a JSON object; counted as unmarked")
        puzzles.append((path.stem, puzzle))
    return puzzles, warnings


def category_order(category):
    return (CATEGORIES.index(category), "") if category in CATEGORIES else (len(CATEGORIES), category)


def bar_items(marked):
    """S-56's five items over the marked puzzles: [(label, value, met)]."""
    categories = sorted({p.get("category") for _, p in marked if isinstance(p.get("category"), str)},
                        key=category_order)
    minis = sorted(i for i, p in marked if kind_of(p) == "mini" and pieces_of(p) == BAR_MINI_PIECES and is_easiest(p))
    warmups = [i for i, p in marked if kind_of(p) == "warmup" and is_easiest(p)]
    square = any(i == BAR_SQUARE for i, _ in marked)
    return [
        ("marked puzzles", f"{len(marked)} of {BAR_PUZZLES}", len(marked) >= BAR_PUZZLES),
        ("categories with a marked puzzle",
         f"{len(categories)} of {BAR_CATEGORIES} ({', '.join(categories) if categories else 'none'})",
         len(categories) >= BAR_CATEGORIES),
        (BAR_SQUARE, "marked" if square else "not marked", square),
        ("a marked mini with exactly three solution entries and difficulty 1", ", ".join(minis) if minis else "none",
         bool(minis)),
        ("marked warmup puzzles of difficulty 1", f"{len(warmups)} of {BAR_WARMUPS}", len(warmups) >= BAR_WARMUPS),
    ]


def bar_lines(puzzles):
    marked = [(i, p) for i, p in puzzles if p is not None and is_marked(p)]
    items = bar_items(marked)
    lines = ["release bar (the release needs all five):"]
    lines += [f"  {n}. {label}: {value}" for n, (label, value, _) in enumerate(items, 1)]
    unmet = [f"{label}: {value}" for label, value, met in items if not met]
    lines.append("release-ready: yes" if not unmet else "release-ready: no — " + "; ".join(unmet))
    return lines


def status_lines(puzzles, with_list):
    marked_ids = sorted(i for i, p in puzzles if p is not None and is_marked(p))
    lines = [f"all puzzles: {len(marked_ids)} of {len(puzzles)} marked"]
    seen = {p.get("category") for _, p in puzzles if p is not None and isinstance(p.get("category"), str)}
    for category in sorted(set(CATEGORIES) | seen, key=category_order):
        in_category = [p for _, p in puzzles if p is not None and p.get("category") == category]
        lines.append(f"{category}: {sum(1 for p in in_category if is_marked(p))} of {len(in_category)} marked")
    lines += bar_lines(puzzles)
    if with_list:
        lines.append(f"marked ids ({len(marked_ids)}):")
        lines += [f"  {i}" for i in marked_ids]
    return lines


def flipped(data, puzzle, want):
    """The file's bytes with the one boolean set to `want` and every other byte kept, or Refused."""
    if data.count(KEY) != 1:
        raise Refused(f'"reviewedByHuman" is written {data.count(KEY)} times in the file, not once')
    found = list(FIELD.finditer(data))
    if len(found) != 1:
        raise Refused('"reviewedByHuman" is not followed by a colon and a plain true or false')
    match = found[0]
    new = data[:match.start(2)] + (b"true" if want else b"false") + data[match.end(2):]
    expected = json.loads(json.dumps(puzzle))
    expected["provenance"]["reviewedByHuman"] = want
    try:
        again = json.loads(new.decode("utf-8"))
    except ValueError as e:
        raise Refused(f"the changed text does not parse ({e})")
    if json.dumps(again, sort_keys=True) != json.dumps(expected, sort_keys=True):
        raise Refused("the changed text is not the same puzzle with only that boolean flipped")
    return new


def plan(folder, ids, want):
    """Checks every id before any write: ([(id, title, path or None, new bytes or None)], [problem])."""
    files = {p.stem: p for p in puzzle_files(folder)}
    plans, problems = [], []
    for pid in dict.fromkeys(ids):
        path = files.get(pid)
        if path is None:
            problems.append(f"{pid}: no puzzle file {pid}.json in {folder}")
            continue
        try:
            data, puzzle = read_puzzle(path)
        except (UnicodeDecodeError, ValueError) as e:
            problems.append(f"{pid}: {path.name} is not UTF-8 JSON ({e})")
            continue
        provenance = puzzle.get("provenance") if isinstance(puzzle, dict) else None
        if not isinstance(provenance, dict) or not isinstance(provenance.get("reviewedByHuman"), bool):
            problems.append(f"{pid}: {path.name} has no provenance.reviewedByHuman that is true or false")
            continue
        if provenance["reviewedByHuman"] is want:
            plans.append((pid, title_of(puzzle), None, None))
            continue
        try:
            plans.append((pid, title_of(puzzle), path, flipped(data, puzzle, want)))
        except Refused as e:
            problems.append(f"{pid}: {path.name}: {e}")
    return plans, problems


def cmd_flip(folder, ids, want, out, err):
    plans, problems = plan(folder, ids, want)
    if problems:
        print(f"refused, nothing written ({len(problems)} problem{'s' if len(problems) != 1 else ''}):", file=err)
        for problem in problems:
            print(f"  {problem}", file=err)
        return 2
    word = "marked" if want else "unmarked"
    for pid, title, path, new in plans:
        if path is None:
            print(f"already {word} {pid} — {title}", file=out)
        else:
            path.write_bytes(new)
            print(f"{word} {pid} — {title}", file=out)
    puzzles, warnings = survey(folder)
    for warning in warnings:
        print(warning, file=err)
    for line in bar_lines(puzzles):
        print(line, file=out)
    return 0


def cmd_status(folder, with_list, out, err):
    puzzles, warnings = survey(folder)
    for warning in warnings:
        print(warning, file=err)
    for line in status_lines(puzzles, with_list):
        print(line, file=out)
    return 0


def parser():
    folder_help = "the puzzle folder (default: the repository's Tangrams/)"
    top = argparse.ArgumentParser(
        prog="mark_puzzles.py",
        description="Mark puzzles as reviewed by a person (provenance.reviewedByHuman) and show the release bar.")
    top.add_argument("--folder", default=None, help=folder_help)
    commands = top.add_subparsers(dest="command", required=True, metavar="{status,mark,unmark}")
    status = commands.add_parser("status", help="read-only: marked counts, the release bar, release-ready")
    status.add_argument("--list", action="store_true", help="also print the marked ids, sorted")
    status.add_argument("--folder", default=argparse.SUPPRESS, help=folder_help)
    for name, what in (("mark", "false -> true"), ("unmark", "true -> false")):
        command = commands.add_parser(name, help=f"provenance.reviewedByHuman {what} in each named puzzle")
        command.add_argument("ids", nargs="+", metavar="id", help="a puzzle id (its file name without .json)")
        command.add_argument("--folder", default=argparse.SUPPRESS, help=folder_help)
    return top


def main(argv=None, out=None, err=None):
    out = out if out is not None else sys.stdout
    err = err if err is not None else sys.stderr
    args = parser().parse_args(argv)
    folder = Path(args.folder) if args.folder else DEFAULT_FOLDER
    if not folder.is_dir():
        print(f"no such folder: {folder}", file=err)
        return 2
    if args.command == "status":
        return cmd_status(folder, args.list, out, err)
    return cmd_flip(folder, args.ids, args.command == "mark", out, err)


if __name__ == "__main__":
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(errors="replace")
    sys.exit(main())
