# v1.0 — Bharat (Hindi + Tamil)

**Phase 4 · Pilot and public release** · Release channel: GitHub stable, website, Indus Appstore, F-Droid

**Status:** ⬜ Not started (0/25 steps)

## Goal

Indian languages after the core is done, a real user study, and the first stable release on all open channels.

## Done when (exit criteria)

- [ ] Hindi and Tamil voice, UI and replies; "haan / nahin" confirmations.
- [ ] User study done with fixes.
- [ ] v1.0 on GitHub, Indus Appstore and F-Droid.

## Read before starting

- roadmap/PLAN.md §3 v1.0
- docs/DECISIONS.md (language decisions)

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [ ] T36 · Hindi (hi-IN) (P0)

- **Depends on:** T35
- **Where:** `app/src/main/res/values-hi/`, `modules/trust/src/main/resources/`
- **Accept:** All P0 tasks work in Hindi.

- [ ] String translations
- [ ] Speech + TTS
- [ ] `risk_keywords_hi.json`
- [ ] Yes/no words
- [ ] Code-mixed speech

### [ ] T37 · Tamil (ta-IN) (P0)

- **Depends on:** T35
- **Where:** `app/src/main/res/values-ta/`, `modules/trust/src/main/resources/`
- **Accept:** All P0 tasks work in Tamil.

- [ ] String translations
- [ ] Speech + TTS
- [ ] `risk_keywords_ta.json`
- [ ] Yes/no words

### [ ] T38 · Benchmark in Hindi and Tamil (P0)

- **Depends on:** T36, T37
- **Where:** `eval/`
- **Accept:** Results per language published.

- [ ] Re-run benchmark per language

### [ ] T39 · Ethics approval + user study (P0)

- **Depends on:** T36, T37
- **Where:** `docs/`
- **Accept:** Study report + fixes merged.

- [ ] Ethics approval
- [ ] 10–15 participants in their own language
- [ ] Fixes from findings

### [ ] T40 · Messages (SMS) and Telegram plugins (P1)

- **Depends on:** T13
- **Where:** `plugins-builtin/`
- **Accept:** Read and reply-after-yes work.

- [ ] Messages plugin
- [ ] Telegram plugin

### [ ] T41 · IRCTC and DigiLocker web recipes (P1)

- **Depends on:** T17
- **Where:** `plugins-builtin/`
- **Accept:** Read-only tasks work; submits need strong confirmation.

- [ ] IRCTC recipe
- [ ] DigiLocker recipe

### [ ] T42 · Accessibility review (P0)

- **Depends on:** T20
- **Where:** `app/`
- **Accept:** No blocker found in review.

- [ ] TalkBack pass
- [ ] Large text and contrast

### [ ] T43 · Documentation (P0)

- **Depends on:** none
- **Where:** `docs/`
- **Accept:** Docs published with the release.

- [ ] User guide
- [ ] Plugin authoring guide
- [ ] Security model

### [ ] T44 · Store releases + developer verification (P0)

- **Depends on:** T38, T39
- **Where:** `GitHub, Indus Appstore, F-Droid`
- **Accept:** v1.0 live on all channels.

- [ ] Developer verification + package registration
- [ ] Indus Appstore listing
- [ ] F-Droid metadata for `foss`

## Notes

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
