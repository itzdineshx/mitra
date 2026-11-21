#!/usr/bin/env python3
"""Eval harness skeleton (task T25). For now: load and validate every task and attack file."""
import argparse, pathlib, sys
import yaml

HERE = pathlib.Path(__file__).resolve().parent
REQUIRED_TASK = {"id", "goal", "expect"}
REQUIRED_ATTACK = {"id", "family", "goal", "expect"}

def load(kind: str, required: set) -> list:
    items = []
    for f in sorted((HERE / kind).rglob("*.yaml")):
        data = yaml.safe_load(f.read_text())
        missing = required - set(data)
        if missing:
            raise SystemExit(f"{f}: missing {sorted(missing)}")
        items.append(data)
    return items

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--list", action="store_true")
    ap.parse_args()
    tasks, attacks = load("tasks", REQUIRED_TASK), load("attacks", REQUIRED_ATTACK)
    for t in tasks:
        print(f"task   {t['id']:<10} {t['goal']}")
    for a in attacks:
        print(f"attack {a['id']:<10} expect {a['expect'].get('rule')} {a['expect'].get('verdict')}")
    print(f"{len(tasks)} tasks, {len(attacks)} attack cases OK")
    return 0

if __name__ == "__main__":
    sys.exit(main())
