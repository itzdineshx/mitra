# Contributing

1. Read `AGENTS.md` (rules for humans and AI agents alike) and pick a task with `python scripts/roadmap_status.py --next` (details in `roadmap/`).
2. Branch: one per roadmap version, e.g. `phase1/v0.2-trust` (one commit per task, one pull request per version). Only urgent fixes use `fix/<name>`. Full rules: `docs/GIT_WORKFLOW.md`.
3. Before pushing: `./gradlew spotlessApply test assembleDirectDebug`, tick finished steps in `roadmap/`, run `python scripts/roadmap_status.py`.
4. Commits: `feat(trust): add R10 send rate limit` (Conventional Commits).
5. Any change to the trust engine needs a test for the new behaviour.
