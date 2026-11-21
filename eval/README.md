# Evaluation

- `tasks/` — IndiaAppBench tasks (English first), one YAML per task.
- `attacks/` — attack suite, `attacks/<family>/<id>.yaml` (families A1–A6, docs/TESTING.md §3).
- `run.py` — harness (task T25): drives a phone over ADB, records success, latency and trust verdicts to `results/*.csv`.

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r eval/requirements.txt
python eval/run.py --list          # validates and lists every task and attack case
```
