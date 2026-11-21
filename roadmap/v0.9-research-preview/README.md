# v0.9 — Research preview

**Phase 3 · Evaluation** · Release channel: GitHub release with the paper

**Status:** ⬜ Not started (0/11 steps)

## Goal

Evidence that Mitra works and is safer than a baseline agent.

## Done when (exit criteria)

- [ ] Benchmark, attack and ablation results in CSV + charts.
- [ ] Paper draft.
- [ ] Median time to first action ≤ 3 s.

## Read before starting

- docs/TESTING.md §4 (metrics)
- docs/MitraOS_PRD_v3.0.pdf (evaluation chapter)

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [ ] T31 · Full IndiaAppBench (130 tasks) (P0)

- **Depends on:** T25
- **Where:** `eval/tasks/`
- **Accept:** M1 task success measured for configs A and C.

- [ ] 130 tasks with automatic checkers
- [ ] Freeze the 8 benchmark apps (DECISIONS Q-4)

### [ ] T32 · Attack suite 200+ cases (P0)

- **Depends on:** T26
- **Where:** `eval/attacks/`
- **Accept:** M2 ≥ 50% relative reduction vs baseline; M3 = 0.

- [ ] 200+ cases across 6 families
- [ ] Baseline C comparison

### [ ] T33 · Ablations (P0)

- **Depends on:** T31, T32
- **Where:** `eval/`
- **Accept:** Results table for the paper.

- [ ] Each defence on/off
- [ ] Cross-agent provenance on/off
- [ ] Single vs multi-agent

### [ ] T34 · Performance tuning (P1)

- **Depends on:** T31
- **Where:** `modules/model/`, `modules/agents/`
- **Accept:** M4 median time to first action ≤ 3 s.

- [ ] Profile time to first action
- [ ] Prompt caching, shorter screen text

### [ ] T35 · Paper + v0.9 release (P0)

- **Depends on:** T33
- **Where:** `docs/`
- **Accept:** Release published with paper and results.

- [ ] Paper draft
- [ ] GitHub release v0.9

## Notes

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
