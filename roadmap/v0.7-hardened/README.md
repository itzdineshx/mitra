# v0.7 — Hardened

**Phase 2 · Hardening** · Release channel: Public APK

**Status:** 🟡 In progress (1/7 steps)

## Goal

Close the remaining attack paths and make setup work on real phones.

## Done when (exit criteria)

- [ ] Overlay over the Send button is refused with a spoken warning.
- [ ] Unknown caller and screen sharing pause tasks.
- [ ] Tested on one Android 10 phone and one Android 13+ phone.

## Read before starting

- docs/TRUST_ENGINE_SPEC.md R6, R12, R13

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [ ] T22 · Overlay detection + context pauses (P1 · 🏃 sprint)

- **Depends on:** T09
- **Where:** `modules/engine-access/`, `app/`
- **Accept:** Overlay test → refused + spoken warning; unknown-caller test pauses the task.

- [x] R6 / R12 / R13 rules + unit tests
- [ ] Real signals: other apps' windows, call state (`READ_PHONE_STATE`), screen share, new remote-access apps
- [ ] Test app that draws over the Send button

### [ ] T29 · Restricted-settings voice guide (P1)

- **Depends on:** T20
- **Where:** `app/`
- **Accept:** A first-time user on Android 13+ enables Mitra using only the voice guide.

- [ ] Detect Android 13+ restricted state
- [ ] Spoken step-by-step guide to allow restricted settings and enable the service

### [ ] T30 · Device test matrix (P0)

- **Depends on:** T27
- **Where:** `docs/TESTING.md`
- **Accept:** All P0 checks pass on both phones.

- [ ] Run all P0 manual checks on Android 10 and Android 13+
- [ ] Record results and fix blockers

## Notes

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
