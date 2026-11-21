#!/usr/bin/env python3
"""Recount the roadmap checklists and refresh the status lines.

Usage:
    python scripts/roadmap_status.py          # update roadmap/README.md and each version's Status line
    python scripts/roadmap_status.py --check  # CI: fail if the board is out of date
    python scripts/roadmap_status.py --next   # print the next task to work on

Counts the step checkboxes ("- [x]" / "- [ ]") under "## Tasks" in roadmap/<version>/README.md.
Exit-criteria boxes are not counted.
"""
import argparse
import pathlib
import re
import sys

ROADMAP = pathlib.Path(__file__).resolve().parent.parent / "roadmap"
TASK_HEAD = re.compile(r"^### \[( |x)\] (T\d+) · (.+?) \((P\d)")
DEPS = re.compile(r"^- \*\*Depends on:\*\* (.+)$")
STEP = re.compile(r"^- \[( |x)\] ")


def parse(readme: pathlib.Path):
    text = readme.read_text(encoding="utf-8")
    tasks_part = text.split("## Tasks", 1)[1].split("## Notes", 1)[0] if "## Tasks" in text else ""
    done = total = 0
    tasks = []
    for line in tasks_part.splitlines():
        if m := TASK_HEAD.match(line):
            tasks.append({"id": m[2], "title": m[3], "p": m[4], "ticked": m[1] == "x", "deps": [], "open_steps": 0})
        elif (m := DEPS.match(line)) and tasks:
            tasks[-1]["deps"] = [] if m[1].strip() == "none" else [d.strip() for d in m[1].split(",")]
        elif m := STEP.match(line):
            total += 1
            if m[1] == "x":
                done += 1
            elif tasks:
                tasks[-1]["open_steps"] += 1
    return text, done, total, tasks


def label(done: int, total: int) -> str:
    if done == 0:
        return "⬜ Not started"
    if done == total:
        return "✅ Done"
    return "🟡 In progress"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--next", action="store_true")
    args = ap.parse_args()

    versions = sorted(d for d in ROADMAP.iterdir() if d.is_dir() and (d / "README.md").exists())
    index_path = ROADMAP / "README.md"
    index = index_path.read_text(encoding="utf-8")
    changed = []
    all_tasks = []

    for d in versions:
        readme = d / "README.md"
        text, done, total, tasks = parse(readme)
        all_tasks += [(d.name, t) for t in tasks]
        status = f"**Status:** {label(done, total)} ({done}/{total} steps)"
        new_text = re.sub(r"^\*\*Status:\*\* .*$", status, text, count=1, flags=re.M)
        if new_text != text:
            changed.append(str(readme.relative_to(ROADMAP.parent)))
            if not args.check:
                readme.write_text(new_text, encoding="utf-8")
        # index row: | [name](dir/README.md) | phase | status | x/y | ids |
        row = re.compile(r"^(\| \[[^\]]+\]\(" + re.escape(d.name) + r"/README\.md\) \| [^|]+ \| )[^|]+ \| [^|]+ (\|.*)$", re.M)
        index = row.sub(lambda m: f"{m[1]}{label(done, total)} | {done}/{total} {m[2]}", index)

    if index != index_path.read_text(encoding="utf-8"):
        changed.append("roadmap/README.md")
        if not args.check:
            index_path.write_text(index, encoding="utf-8")

    if args.next:
        ticked = {t["id"] for _, t in all_tasks if t["ticked"]}
        for d in versions:  # lowest version first, then P0 → P1 → P2 inside it
            for prio in ("P0", "P1", "P2"):
                for v, t in all_tasks:
                    if v != d.name or t["ticked"] or t["p"] != prio:
                        continue
                    if all(dep in ticked for dep in t["deps"]):
                        print(f"Next: {t['id']} · {t['title']} ({t['p']}) — roadmap/{v}/README.md ({t['open_steps']} open steps)")
                        return 0
        print("No unblocked task left. Check the lowest unfinished version for blockers.")
        return 0

    if args.check and changed:
        print("Roadmap status is out of date. Run: python scripts/roadmap_status.py\n  " + "\n  ".join(changed))
        return 1
    print("Roadmap status up to date." if not changed else "Updated:\n  " + "\n  ".join(changed))
    return 0


if __name__ == "__main__":
    sys.exit(main())
