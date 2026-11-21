# v1.x — Everyday Mitra

**Phase 4–5** · Release channel: All open channels

**Status:** ⬜ Not started (0/14 steps)

## Goal

Hands-free, more languages, memory and faster structured actions.

## Done when (exit criteria)

- [ ] Each feature ships behind a setting with tests and docs.

## Read before starting

- roadmap/PLAN.md §3 v1.x

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [ ] T45 · "Hey Mitra" wake word (P1)

- **Depends on:** none
- **Where:** `modules/voice/`
- **Accept:** Wake word works with acceptable battery use.

- [ ] openWakeWord on device (phrase "Hey Mitra")
- [ ] Foreground service with a visible notification; off by default
- [ ] Setup switch and Home hint
- [ ] Battery test

### [ ] T46 · Encrypted memory with consent (P1)

- **Depends on:** none
- **Where:** `modules/data/`, `modules/agents/`
- **Accept:** User can view, edit and delete every memory.

- [ ] Memory agent
- [ ] Consent UI + delete
- [ ] Provenance kept on memories

### [ ] T47 · AppFunctions (Android 16+) (P2)

- **Depends on:** none
- **Where:** `modules/agents/`
- **Accept:** At least one app task runs via AppFunctions.

- [ ] Discover and call AppFunctions
- [ ] Still checked by the trust engine

### [ ] T48 · More Indian languages + IndicConformer (P1)

- **Depends on:** T36
- **Where:** `modules/voice/`, `app/src/main/res/`
- **Accept:** Each language passes the P0 task set.

- [ ] Telugu, Bengali, Marathi, Kannada, Malayalam
- [ ] On-device IndicConformer ASR

### [ ] T49 · LoRA fine-tuned model (P2)

- **Depends on:** T31
- **Where:** `ml/`
- **Accept:** Benchmark success improves over the base model.

- [ ] Training data from consented logs
- [ ] Fine-tune + export to `.litertlm`

### [ ] T50 · More plugins (P2)

- **Depends on:** T13
- **Where:** `plugins-builtin/`
- **Accept:** Each plugin signed, tested and documented.

- [ ] Community-requested apps

## Notes

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
