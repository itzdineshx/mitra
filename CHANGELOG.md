# Changelog

All notable changes are listed here. Format: [Keep a Changelog](https://keepachangelog.com), versions: SemVer.

## [Unreleased]

### Changed
- UI redesigned as an **assistant over any app** (Listening, Working, Confirm, Your turn, Done) plus a small app (Home, History, Setup, Permissions, On-device AI): new `docs/UI_SPEC.md`, 10 screen pictures, new design tokens in `Theme.kt` (D-014). The old 12-screen wireframes are retired.
- Roadmap: T20 rewritten; new tasks T56–T59 (assistant panel, live speech, entry points, undrawn screens); T45 is now the "Hey Mitra" wake word.
- App parts moved from `shared/` to `modules/` with short Gradle names (`:trust`, `:agents` …).
- Task list moved to `roadmap/`: one folder per version (v0.1 → v2.0) with checklists for T01–T55; `scripts/roadmap_status.py` keeps the board up to date.

### Added
- `docs/DATA_SPEC.md` (database, storage map, redaction, keys, retention, Delete all) and roadmap task T60; `PRIVACY.md` draft; `docs/DEV_SETUP.md`; backup exclusions (`dataExtractionRules`).
- DM Sans font bundled in `modules/ui` (OFL licence in `third_party/`), F-Droid metadata in `fastlane/`, issue templates, Dependabot, Code of Conduct.
- `brand/`: lotus logo, launcher and notification icons (installed in `app/src/main/res`), lockups, favicons and colours (D-015).
- `docs/UI_SPEC.md` and wireframe images in `docs/design/screens/`: the UI to build; design tokens in `modules/ui/Theme.kt`.
- Project skeleton: app (flavours `direct`, `foss`) and the modules in `modules/`.
- `engine-api` types and `ExecutionEngine` interface.
- Trust engine with rules R1–R14, risk re-derivation, `TrustedExecutor`, and unit tests for every spec case.
- Domain allow-list, HTTPS and look-alike checks (`modules/web`).
- Plugin model, Ed25519 loader and a starter Settings plugin.
- Accessibility service, screen reading with provenance tags, Stop overlay.
- Eval skeleton (tasks, attack cases), CI workflow, agent docs (AGENTS.md, docs/).
