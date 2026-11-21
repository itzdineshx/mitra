# Roadmap — what to build, in order

One folder per version. Each folder has a `README.md` with the goal, the exit criteria and a **checklist of every task**.
This folder is the single place where progress is tracked. The long-form plan (channels, risks, quality gates) is in [PLAN.md](PLAN.md).

## Status board

| Version | Phase | Status | Steps done | Tasks |
|---|---|---|---|---|
| [v0.1 — Hello Mitra](v0.1-hello-mitra/README.md) | Phase 0–1 · Foundation | ✅ Done | 35/35 | T01, T02, T03, T04, T05, T06, T07 |
| [v0.2 — Trust](v0.2-trust/README.md) | Phase 1 · App MVP | ✅ Done | 19/19 | T08, T60, T09 |
| [v0.3 — Team](v0.3-team/README.md) | Phase 1 · App MVP | 🟡 In progress | 4/15 | T10, T11, T12 |
| [v0.4 — Plugins](v0.4-plugins/README.md) | Phase 1 · App MVP | 🟡 In progress | 3/17 | T13, T14, T15, T16, T19 |
| [v0.5 — Web](v0.5-web/README.md) | Phase 1 · App MVP | 🟡 In progress | 2/8 | T17, T18 |
| [v0.6 — Control centre (first public beta)](v0.6-control-centre/README.md) | Phase 1–2 | 🟡 In progress | 4/46 | T20, T56, T57, T58, T59, T21, T23, T24, T25, T26, T27, T28 |
| [v0.7 — Hardened](v0.7-hardened/README.md) | Phase 2 · Hardening | 🟡 In progress | 1/7 | T22, T29, T30 |
| [v0.9 — Research preview](v0.9-research-preview/README.md) | Phase 3 · Evaluation | ⬜ Not started | 0/11 | T31, T32, T33, T34, T35 |
| [v1.0 — Bharat (Hindi + Tamil)](v1.0-bharat/README.md) | Phase 4 · Pilot and public release | ⬜ Not started | 0/25 | T36, T37, T38, T39, T40, T41, T42, T43, T44 |
| [v1.x — Everyday Mitra](v1.x-everyday-mitra/README.md) | Phase 4–5 | ⬜ Not started | 0/14 | T45, T46, T47, T48, T49, T50 |
| [v2.0 — Mitra Platform](v2.0-platform/README.md) | Phase 5 · Platform and scale | ⬜ Not started | 0/7 | T51, T52, T53, T54, T55 |

Legend: ⬜ not started · 🟡 in progress · ✅ done · **P0** must have · **P1** should have · **P2** nice to have · 🏃 part of the 5-day sprint (T01–T27, from 21 Nov 2026).

## Phases

| Phase | Name | Versions |
|---|---|---|
| 0 | Foundation | v0.1 |
| 1 | App MVP | v0.1 – v0.6 |
| 2 | Hardening | v0.7 |
| 3 | Evaluation and research release | v0.9 |
| 4 | Pilot and public release | v1.0, v1.x |
| 5 | Platform and scale | v1.x, v2.0 |

## How to pick the next task (humans and AI agents)

1. Go to the **lowest version** that is not ✅.
2. In it, take the first task that is not ticked, is **P0**, and whose **Depends on** tasks are all ticked. If none, take P1, then P2.
3. Read the files in **Read before starting** and the task's **Spec**.
4. Work through the task's steps in order. Work on the version branch (`phase1/<version-folder>`). One task = one commit (`feat(T09): …`); one pull request per version (see `docs/GIT_WORKFLOW.md`).
5. When a step is merged and tested, tick it here **in the same commit**. Tick the task heading `[x]` only when **Accept** is proven.
6. Do not start a later version early, except tasks with no dependencies that unblock others (ask the owner).

Task IDs (T01, T02 …) never change and are never reused. New work gets the next free number (currently **T61**) and goes into the version where it belongs.

## If the sprint runs late — cut in this order

Telegram/Messages plugins → family approval (T23) → guide mode (T24) → Gmail (T16) → standing rules (T19).
**Never cut:** T08, T09, T12, T26 (trust engine, executor, provenance, attack tests).

## Rules that apply to every version

- Every action goes through `TrustedExecutor`; the trust engine is code, never a model.
- English (`en-IN`) first; Hindi and Tamil in v1.0. All user-facing text in string resources.
- One app, open source, no Google Play build: `direct` (GitHub, website, Indus Appstore) and `foss` (F-Droid).
- See [AGENTS.md](../AGENTS.md) §3 for the full safety rules.
