# v0.3 — Team

**Phase 1 · App MVP** · Release channel: Internal · Sprint day 2

**Status:** 🟡 In progress (4/15 steps)

## Goal

Six agent roles on one model plan and finish simple tasks end to end, and untrusted text keeps its provenance across agents.

## Done when (exit criteria)

- [ ] "Turn on the torch" runs end to end through the trust engine.
- [ ] Three Settings tasks succeed by voice.
- [ ] The relayed-injection test (web text → message) pauses with R7.

## Read before starting

- docs/AGENT_PROMPTS.md
- docs/ARCHITECTURE.md §5

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [ ] T10 · Orchestrator + TaskRunner (P0 · 🏃 sprint)

- **Depends on:** T06, T09
- **Where:** `modules/agents/`
- **Spec:** docs/AGENT_PROMPTS.md §2
- **Accept:** "Turn on the torch" works end to end on the phone.

- [x] Step loop with 25-step cap, every step via `TrustedExecutor`
- [ ] Orchestrator prompt builds `TaskScope` + plan
- [ ] Fast path for single-agent requests
- [ ] Task board with JSON schema checks
- [ ] `TaskRunner` with cancel (used by Stop)
- [ ] Plan + scope visible in history

### [ ] T11 · App agent + Verifier + Guardian (P0 · 🏃 sprint)

- **Depends on:** T10
- **Where:** `modules/agents/`, `modules/agents/src/main/resources/prompts/`
- **Accept:** "Switch on dark theme", "turn on do not disturb", "set brightness high" succeed; a forced failure ends in a spoken hand-over, not a loop.

- [x] App agent prompt v0 (`prompts/app.txt`)
- [ ] Copy the other prompts from `docs/AGENT_PROMPTS.md`
- [ ] Evidence ids, ≤ 2 retries, spoken hand-over
- [ ] Verifier: strict check after each step
- [ ] Guardian: advisory warnings

### [ ] T12 · Cross-agent provenance (P0 · 🏃 sprint)

- **Depends on:** T11
- **Where:** `modules/trust/Model.kt (`TaintedText`)`, `modules/agents/`
- **Accept:** The relayed-injection case in `docs/TRUST_ENGINE_SPEC.md` §6 passes end to end.

- [x] `TaintedText` carries `List<Provenance>`; R7 checks it
- [x] Unit test: web text typed into a chat → PAUSE
- [ ] Task board and memory keep provenance on every hop
- [ ] Sharpen `entailedByGoal` (new app, recipient, domain, action type)

## Notes

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
