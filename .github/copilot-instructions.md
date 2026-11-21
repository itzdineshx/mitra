# Copilot instructions — Mitra

Follow **AGENTS.md** at the repository root; it is the single source of truth. Key points:

- Work comes from `roadmap/<version>/README.md` (task IDs T01…); tick steps and run `python scripts/roadmap_status.py`.
- Every on-screen action goes through `TrustedExecutor` (`modules/trust`). Never call `ExecutionEngine` mutating methods elsewhere.
- The trust engine is deterministic Kotlin — never add model calls to it, never weaken a rule to pass a test.
- One app only: no root, system APIs, `INJECT_EVENTS`, hidden displays, or a built-in browser.
- English (`en-IN`) first; all user-facing text in string resources; pass `languageTag`.
- Never read or type password/PIN/OTP/CAPTCHA fields; never automate banking/UPI apps or security settings.
- Only `modules/engine-access` may import `android.accessibilityservice.*`.
- Specs: `docs/ARCHITECTURE.md`, `docs/TRUST_ENGINE_SPEC.md`, `docs/PLUGIN_SPEC.md`, `docs/AGENT_PROMPTS.md`, `docs/TESTING.md`, `docs/DECISIONS.md`.
