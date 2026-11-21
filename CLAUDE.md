# CLAUDE.md

All instructions are in **[AGENTS.md](AGENTS.md)**. Start with its §0 "Start here".

Quick reminders:
- Next task: `python scripts/roadmap_status.py --next` → open `roadmap/<version>/README.md`.
- Every on-screen action goes through `TrustedExecutor`; the trust engine is deterministic code, never a model call.
- One app, open source, no Play build; no built-in browser; English (`en-IN`) first, all strings in resources.
- Never touch password/PIN/OTP fields or banking/UPI apps.
- Before committing: `./gradlew spotlessApply test`, tick finished steps, `python scripts/roadmap_status.py`.
