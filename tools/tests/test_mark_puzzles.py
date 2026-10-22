"""
Tool test for tools/mark_puzzles.py (WO-009 TASK-063; tasks.md § WO-009 › Seams S-54). The implementer's self-check
(scaffolding), not an acceptance verdict: it carries no REQ token.

Plain asserts, stdlib only. Run from the project root:  python tools/tests/test_mark_puzzles.py
It reads shipped Tangrams/*.json files and copies them; it runs `mark` and `unmark` only in temporary folders and
never writes under Tangrams/ (owner rule 7, G-05).
"""
import io
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TANGRAMS = ROOT / "Tangrams"
TOOL = ROOT / "tools" / "mark_puzzles.py"
sys.path.insert(0, str(ROOT / "tools"))

import import_figures  # noqa: E402
import mark_puzzles as m  # noqa: E402

FALSE = b'"reviewedByHuman": false'
TRUE = b'"reviewedByHuman": true'
SPEC_ORDER = ["shapes", "animals", "people", "things", "vehicles", "nature", "letters", "numbers"]


def run(*argv):
    out, err = io.StringIO(), io.StringIO()
    code = m.main([str(a) for a in argv], out=out, err=err)
    return code, out.getvalue(), err.getvalue()


def snapshot(folder):
    return {p.name: p.read_bytes() for p in sorted(Path(folder).rglob("*")) if p.is_file()}


def copy_shipped(tmp, *names):
    for name in names:
        shutil.copyfile(TANGRAMS / f"{name}.json", Path(tmp) / f"{name}.json")


def crafted(pid, category, kind="full", difficulty=3, pieces=7, marked=False, provenance="yes"):
    puzzle = {"format": "tangram-puzzle/1", "id": pid, "title": {"en": f"Title {pid}", "fi": f"Otsikko {pid}"},
              "category": category, "difficulty": difficulty, "kind": kind,
              "solution": [{"piece": "SQ", "polygon": [[0, 0], [1, 0], [1, 1]]}] * pieces,
              "art": {"base": "#000000", "shapes": []}}
    if provenance == "yes":
        puzzle["provenance"] = {"author": "ai:test", "reviewedByHuman": marked}
    elif provenance == "no field":
        puzzle["provenance"] = {"author": "ai:test"}
    elif provenance == "string":
        puzzle["provenance"] = {"author": "ai:test", "reviewedByHuman": "true" if marked else "false"}
    return puzzle


def write_folder(tmp, puzzles):
    for puzzle in puzzles:
        (Path(tmp) / f"{puzzle['id']}.json").write_bytes((json.dumps(puzzle, indent=2) + "\n").encode("utf-8"))


def full_bar():
    """20 marked over four categories with the square, a three-piece mini and four warm-ups; unmarked extras."""
    return ([crafted("shapes-square", "shapes", difficulty=4, marked=True),
             crafted("shapes-mini-1", "shapes", kind="mini", difficulty=1, pieces=3, marked=True)]
            + [crafted(f"shapes-warmup-{k}", "shapes", kind="warmup", difficulty=1, marked=True) for k in range(1, 5)]
            + [crafted(f"animals-a{k}", "animals", marked=True) for k in range(1, 6)]
            + [crafted(f"things-t{k}", "things", marked=True) for k in range(1, 6)]
            + [crafted(f"letters-l{k}", "letters", marked=True) for k in range(1, 5)]
            + [crafted("nature-n1", "nature"), crafted("people-p1", "people", marked=True, provenance="none"),
               crafted("vehicles-v1", "vehicles", marked=True, provenance="no field"),
               crafted("numbers-x1", "numbers", marked=True, provenance="string")])


def replaced(puzzles, pid, **changes):
    out = []
    for p in puzzles:
        if p["id"] == pid:
            p = json.loads(json.dumps(p))
            for key, value in changes.items():
                if key == "marked":
                    p["provenance"]["reviewedByHuman"] = value
                elif key == "pieces":
                    p["solution"] = p["solution"][:1] * value
                else:
                    p[key] = value
        out.append(p)
    return out


def status_of(puzzles, *extra):
    with tempfile.TemporaryDirectory() as tmp:
        write_folder(tmp, puzzles)
        code, out, err = run("--folder", tmp, "status", *extra)
    assert code == 0 and err == "", (code, err)
    return out.splitlines()


def unmet(lines):
    """The labels named by the release-ready line ([] when ready)."""
    ready = [line for line in lines if line.startswith("release-ready: ")]
    assert len(ready) == 1, lines
    if ready[0] == "release-ready: yes":
        return []
    assert ready[0].startswith("release-ready: no — "), ready[0]
    return [item.split(": ")[0] for item in ready[0][len("release-ready: no — "):].split("; ")]


MINI = "a marked mini with exactly three solution entries and difficulty 1"
WARMUPS = "marked warmup puzzles of difficulty 1"


def test_default_folder_is_the_repositorys_tangrams():
    assert m.DEFAULT_FOLDER == TANGRAMS
    assert m.CATEGORIES == tuple(SPEC_ORDER)


def test_status_ready_for_the_full_bar():
    lines = status_of(full_bar())
    assert lines[0] == "all puzzles: 20 of 24 marked", lines[0]
    assert [line.split(":")[0] for line in lines[1:9]] == SPEC_ORDER
    assert lines[1:9] == ["shapes: 6 of 6 marked", "animals: 5 of 5 marked", "people: 0 of 1 marked",
                          "things: 5 of 5 marked", "vehicles: 0 of 1 marked", "nature: 0 of 1 marked",
                          "letters: 4 of 4 marked", "numbers: 0 of 1 marked"], lines[1:9]
    assert lines[9:] == ["release bar (the release needs all five):",
                         "  1. marked puzzles: 20 of 20",
                         "  2. categories with a marked puzzle: 4 of 4 (shapes, animals, things, letters)",
                         "  3. shapes-square: marked",
                         f"  4. {MINI}: shapes-mini-1",
                         f"  5. {WARMUPS}: 4 of 4",
                         "release-ready: yes"], lines[9:]
    assert unmet(lines) == []


def test_status_not_ready_for_19_marked():
    lines = status_of(replaced(full_bar(), "letters-l4", marked=False))
    assert unmet(lines) == ["marked puzzles"], lines
    assert "release-ready: no — marked puzzles: 19 of 20" in lines


def test_status_not_ready_for_20_over_three_categories():
    puzzles = [p for p in full_bar() if not (p["id"].startswith("letters-"))]
    puzzles += [crafted(f"things-t{k}", "things", marked=True) for k in range(6, 10)]
    lines = status_of(puzzles)
    assert lines[0].startswith("all puzzles: 20 of "), lines[0]
    assert unmet(lines) == ["categories with a marked puzzle"], lines
    assert "  2. categories with a marked puzzle: 3 of 4 (shapes, animals, things)" in lines


def test_status_not_ready_for_20_without_the_square():
    puzzles = replaced(full_bar(), "shapes-square", marked=False) + [crafted("animals-a6", "animals", marked=True)]
    lines = status_of(puzzles)
    assert lines[0].startswith("all puzzles: 20 of "), lines[0]
    assert unmet(lines) == ["shapes-square"], lines
    assert "  3. shapes-square: not marked" in lines


def test_status_not_ready_without_a_three_piece_mini():
    extra = [crafted("animals-a6", "animals", marked=True)]
    for puzzles in (replaced(full_bar(), "shapes-mini-1", pieces=4),        # four pieces
                    replaced(full_bar(), "shapes-mini-1", pieces=2),        # two pieces
                    replaced(full_bar(), "shapes-mini-1", difficulty=2),    # not rating 1
                    replaced(full_bar(), "shapes-mini-1", kind="full"),     # three pieces, not a mini
                    replaced(full_bar(), "shapes-mini-1", marked=False) + extra):  # not marked
        lines = status_of(puzzles)
        assert lines[0].startswith("all puzzles: 20 of "), lines[0]
        assert unmet(lines) == [MINI], lines
        assert f"  4. {MINI}: none" in lines


def test_status_not_ready_for_three_warmups():
    extra = [crafted("animals-a6", "animals", marked=True)]
    for puzzles in (replaced(full_bar(), "shapes-warmup-4", difficulty=2),
                    replaced(full_bar(), "shapes-warmup-4", kind="full"),
                    replaced(full_bar(), "shapes-warmup-4", marked=False) + extra):
        lines = status_of(puzzles)
        assert lines[0].startswith("all puzzles: 20 of "), lines[0]
        assert unmet(lines) == [WARMUPS], lines
        assert f"  5. {WARMUPS}: 3 of 4" in lines


def test_status_names_every_unmet_item_when_nothing_is_marked():
    puzzles = [replaced([p], p["id"], marked=False)[0] if isinstance(p.get("provenance", {}).get("reviewedByHuman"),
                                                                     bool) else p for p in full_bar()]
    lines = status_of(puzzles)
    assert lines[0] == "all puzzles: 0 of 24 marked", lines[0]
    assert unmet(lines) == ["marked puzzles", "categories with a marked puzzle", "shapes-square", MINI, WARMUPS]
    assert lines[-1] == ("release-ready: no — marked puzzles: 0 of 20; categories with a marked puzzle: 0 of 4 (none); "
                         f"shapes-square: not marked; {MINI}: none; {WARMUPS}: 0 of 4"), lines[-1]


def test_status_list_and_what_it_ignores():
    with tempfile.TemporaryDirectory() as tmp:
        write_folder(tmp, full_bar())
        shutil.copyfile(TANGRAMS / "puzzle.schema.json", Path(tmp) / "puzzle.schema.json")   # not a puzzle
        sub = Path(tmp) / "sub"
        sub.mkdir()
        write_folder(sub, [crafted("animals-deep", "animals", marked=True)])                # not top-level
        before = snapshot(tmp)
        code, out, err = run("status", "--list", "--folder", tmp)
        code2, out2, _ = run("--folder", tmp, "status", "--list")
        assert snapshot(tmp) == before                                                     # read-only
    assert code == 0 and code2 == 0 and err == "" and out == out2
    lines = out.splitlines()
    assert lines[0] == "all puzzles: 20 of 24 marked"
    listed = lines[lines.index("marked ids (20):") + 1:]
    assert listed == ["  " + i for i in sorted(p["id"] for p in full_bar()[:20])], listed
    # a missing provenance, a provenance without the field and a string "true" read as unmarked (2026-10-11 row)
    assert all(f"  {i}" not in listed for i in ("people-p1", "vehicles-v1", "numbers-x1"))


def test_status_warns_on_a_file_that_is_not_json():
    with tempfile.TemporaryDirectory() as tmp:
        write_folder(tmp, full_bar())
        (Path(tmp) / "animals-broken.json").write_bytes(b'{"id": ')
        code, out, err = run("--folder", tmp, "status")
    assert code == 0 and "animals-broken.json" in err and "counted as unmarked" in err, err
    assert out.splitlines()[0] == "all puzzles: 20 of 25 marked"


def assert_only_the_flag(old, new, want):
    a, b = (FALSE, TRUE) if want else (TRUE, FALSE)
    assert old.count(a) == 1 and old.count(b"reviewedByHuman") == 1
    assert new == old.replace(a, b), "a byte other than the one boolean changed"
    i = old.index(a)
    assert new[:i] == old[:i] and new[i + len(b):] == old[i + len(a):]
    before, after = json.loads(old.decode("utf-8")), json.loads(new.decode("utf-8"))
    before["provenance"]["reviewedByHuman"] = want
    assert json.dumps(before, sort_keys=True) == json.dumps(after, sort_keys=True)


def test_mark_and_unmark_both_shipped_layouts():
    names = ("shapes-square", "shapes-mini-1", "animals-cat", "shapes-warmup-1")
    with tempfile.TemporaryDirectory() as tmp:
        copy_shipped(tmp, *names)
        original = snapshot(tmp)
        assert b'"provenance": {"author"' in original["shapes-mini-1.json"]           # the one-line layout
        assert b'"provenance": {\n' in original["shapes-square.json"]                 # the multi-line layout

        code, out, err = run("--folder", tmp, "mark", "shapes-square", "shapes-mini-1")
        assert code == 0 and err == "", (code, err)
        lines = out.splitlines()
        assert lines[:2] == ["marked shapes-square — Gift box", "marked shapes-mini-1 — Mini: Pyramid"], lines
        assert lines[2] == "release bar (the release needs all five):" and lines[-1].startswith("release-ready: no — ")
        assert f"  4. {MINI}: shapes-mini-1" in lines and "  3. shapes-square: marked" in lines
        marked = snapshot(tmp)
        for name in ("shapes-square.json", "shapes-mini-1.json"):
            assert_only_the_flag(original[name], marked[name], True)
            assert marked[name].endswith(b"}\n") and b"\r" not in marked[name]
            assert not marked[name].startswith(b"\xef\xbb\xbf")
        for name in ("animals-cat.json", "shapes-warmup-1.json"):
            assert marked[name] == original[name], name                              # files not named: untouched
        # The marked square keeps the shipped layout (cf. test_import_figures' test_layout_is_the_shipped_layout).
        text = marked["shapes-square.json"].decode("utf-8")
        assert import_figures.format_puzzle(json.loads(text)) == text

        code, out, _ = run("--folder", tmp, "mark", "shapes-square", "animals-cat")
        assert code == 0
        assert out.splitlines()[:2] == ["already marked shapes-square — Gift box", "marked animals-cat — Cat"]
        assert snapshot(tmp)["shapes-square.json"] == marked["shapes-square.json"]

        code, out, _ = run("unmark", "--folder", tmp, "shapes-square", "shapes-mini-1", "animals-cat")
        assert code == 0
        assert out.splitlines()[:3] == ["unmarked shapes-square — Gift box", "unmarked shapes-mini-1 — Mini: Pyramid",
                                        "unmarked animals-cat — Cat"]
        assert snapshot(tmp) == original                                                # the original bytes back

        code, out, _ = run("--folder", tmp, "unmark", "shapes-warmup-1", "shapes-warmup-1")
        assert code == 0 and out.splitlines()[0] == "already unmarked shapes-warmup-1 — Warm-up 1"
        assert out.splitlines()[1] == "release bar (the release needs all five):"   # a repeated id prints once
        assert snapshot(tmp) == original


def test_mark_keeps_odd_spacing_and_a_missing_final_newline():
    raw = (b'{"format": "tangram-puzzle/1", "id": "shapes-odd", "title": {"en": "Odd", "fi": "Outo"}, '
           b'"category": "shapes", "difficulty": 2, "solution": [], "art": {"base": "#000000", "shapes": []}, '
           b'"provenance": {"reviewedByHuman"  :\n\tfalse ,"author": "human:x"}}')
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "shapes-odd.json"
        path.write_bytes(raw)
        assert run("--folder", tmp, "mark", "shapes-odd")[0] == 0
        assert path.read_bytes() == raw.replace(b":\n\tfalse ,", b":\n\ttrue ,")
        assert run("--folder", tmp, "unmark", "shapes-odd")[0] == 0
        assert path.read_bytes() == raw


def test_mark_refuses_the_whole_call_and_writes_nothing():
    both = ("mark", "unmark")
    bad = {  # id: (file content or None for no file, the commands it refuses)
        "no-such-puzzle": (None, both),                                                   # unknown id
        "animals-noflag": (crafted("animals-noflag", "animals", provenance="no field"), both),
        "animals-noprov": (crafted("animals-noprov", "animals", provenance="none"), both),
        "animals-string": (crafted("animals-string", "animals", provenance="string"), both),
        "animals-broken": (b'{"id": "animals-broken", ', both),
        "animals-array": (b'[1, 2]\n', both),
        # the key written twice: the one-occurrence rule refuses a flip (a file already in the wanted state needs none)
        "animals-twice": (b'{"note": {"reviewedByHuman": true}, "provenance": {"reviewedByHuman": false}}\n',
                          ("mark",)),
        "animals-twice-on": (b'{"provenance": {"reviewedByHuman": true}, "note": {"reviewedByHuman": false}}\n',
                             ("unmark",)),
        "puzzle.schema": (None, both),                                                    # the schema is no puzzle
        "../shapes-square": (None, both),
        "shapes-square.json": (None, both),
    }
    with tempfile.TemporaryDirectory() as tmp:
        copy_shipped(tmp, "shapes-square", "shapes-mini-1")
        shutil.copyfile(TANGRAMS / "puzzle.schema.json", Path(tmp) / "puzzle.schema.json")
        for pid, (content, _) in bad.items():
            if isinstance(content, dict):
                write_folder(tmp, [content])
            elif isinstance(content, bytes):
                (Path(tmp) / f"{pid}.json").write_bytes(content)
        before = snapshot(tmp)
        for pid, (_, commands) in bad.items():
            for command in commands:
                code, out, err = run("--folder", tmp, command, "shapes-square", pid, "shapes-mini-1")
                assert code == 2 and out == "", (pid, command, code, out)
                assert err.startswith("refused, nothing written (1 problem):") and pid in err, (pid, err)
                assert snapshot(tmp) == before, (pid, command)
        for command in both:
            code, out, err = run("--folder", tmp, command, *bad)
            refusing = sum(1 for _, commands in bad.values() if command in commands)
            assert code == 2 and out == "" and f"({refusing} problems)" in err, (command, err)
            assert snapshot(tmp) == before


def test_the_command_line():
    with tempfile.TemporaryDirectory() as tmp:
        write_folder(tmp, replaced(full_bar(), "letters-l4", marked=False))
        env = dict(os.environ, PYTHONUTF8="1")
        env.pop("PYTHONIOENCODING", None)

        def cli(*argv, **extra):
            done = subprocess.run([sys.executable, str(TOOL), *argv], capture_output=True, env=dict(env, **extra))
            return done.returncode, done.stdout.decode("utf-8", "replace"), done.stderr.decode("utf-8", "replace")

        code, out, _ = cli("--folder", tmp, "status")
        assert code == 0 and out.splitlines()[-1] == "release-ready: no — marked puzzles: 19 of 20", out
        code, out, _ = cli("mark", "letters-l4", "--folder", tmp)
        assert code == 0 and out.splitlines()[0] == "marked letters-l4 — Title letters-l4"
        assert out.splitlines()[-1] == "release-ready: yes", out
        code, out, err = cli("--folder", tmp, "unmark", "letters-l4", "letters-l9")
        assert code == 2 and out == "" and "letters-l9" in err
        code, out, _ = cli("--folder", tmp, "unmark", "letters-l4")
        assert code == 0 and out.splitlines()[-1] == "release-ready: no — marked puzzles: 19 of 20"
        # A console that cannot print the dash still gets the lines (errors="replace"), never a crash.
        env.pop("PYTHONUTF8")
        code, out, _ = cli("--folder", tmp, "status", PYTHONIOENCODING="ascii")
        assert code == 0 and out.splitlines()[-1] == "release-ready: no ? marked puzzles: 19 of 20", out
        assert cli("--folder", str(Path(tmp) / "missing"), "status")[0] == 2
        assert cli("--folder", tmp)[0] == 2                                              # no command
        assert cli("--folder", tmp, "mark")[0] == 2                                      # no id


def main():
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_") and callable(v)]
    for t in tests:
        t()
    print(f"ok: {len(tests)} mark tool tests")


if __name__ == "__main__":
    main()
