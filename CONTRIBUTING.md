# Contributing

1. Read `AGENTS.md` (rules for humans and AI agents alike) and pick a task with `python scripts/roadmap_status.py --next` (details in `roadmap/`).
2. Branch: `feat/T08-trust-core`, `fix/r4-lookalike` …
3. Before pushing: `./gradlew spotlessApply test assembleDirectDebug`, tick finished steps in `roadmap/`, run `python scripts/roadmap_status.py`.
4. Commits: `feat(trust): add R10 send rate limit` (Conventional Commits).
5. Any change to the trust engine needs a test for the new behaviour.
