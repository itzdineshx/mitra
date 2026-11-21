# v2.0 — Mitra Platform

**Phase 5 · Platform and scale** · Release channel: All channels + partners

**Status:** ⬜ Not started (0/7 steps)

## Goal

From an app to a platform: skills, proactive help, a plugin marketplace and partner editions.

## Done when (exit criteria)

- [ ] Each item has its own design doc in `docs/` before work starts.

## Read before starting

- roadmap/PLAN.md §3 v2.0

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [ ] T51 · Learn a task from one demonstration (P1)

- **Depends on:** none
- **Where:** `modules/agents/`
- **Accept:** A recorded task replays safely with new parameters.

- [ ] Record → generalise → signed local skill

### [ ] T52 · Proactive help from notifications (opt-in) (P2)

- **Depends on:** none
- **Where:** `modules/agents/`
- **Accept:** Suggestions are useful and never act without the user.

- [ ] Opt-in notification listener
- [ ] Suggestions only; never acts alone

### [ ] T53 · Community plugin marketplace (P2)

- **Depends on:** T13
- **Where:** (new service)
- **Accept:** Third-party signed plugins install safely.

- [ ] Signed submissions + review

### [ ] T54 · Developer SDK (P2)

- **Depends on:** T47
- **Where:** (new module)
- **Accept:** One partner app integrates it.

- [ ] SDK for Indian app makers (AppFunctions-compatible)

### [ ] T55 · Assisted-service and pre-install editions (P2)

- **Depends on:** none
- **Where:** `app/ (new flavours)`
- **Accept:** One pilot partner.

- [ ] Service-centre edition
- [ ] Phone-maker pre-install edition

## Notes

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
