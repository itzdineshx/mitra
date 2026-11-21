# v0.5 — Web

**Phase 1 · App MVP** · Release channel: GitHub pre-release · Sprint day 3

**Status:** 🟡 In progress (2/8 steps)

## Goal

The web agent uses the phone's own browser safely: allowed domains only, HTTPS only, look-alikes refused, secrets handed to the user.

## Done when (exit criteria)

- [ ] "Check my PNR status" works on an allowed site.
- [ ] `http://` and look-alike domains are refused (R4).
- [ ] "Check my PNR and send it to Priya" works with one confirmation.

## Read before starting

- docs/PLUGIN_SPEC.md §3 (Chrome)
- docs/TRUST_ENGINE_SPEC.md R4

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [ ] T17 · Web agent + Chrome recipe + domain safety (P0 · 🏃 sprint)

- **Depends on:** T13
- **Where:** `modules/web/`, `modules/engine-access/AccessibilityEngine.kt (`AddressBar`)`, `plugins-builtin/chrome/`
- **Accept:** PNR check works on an allowed `*.gov.in` / `irctc.co.in` site; `http://` and look-alikes refused; CAPTCHA handed to the user.

- [x] `DomainPolicy` + `LookAlikeDetector` + tests
- [x] Address-bar reader v0 (Chrome, Firefox, Samsung Internet ids)
- [ ] Verify address-bar ids on a phone; detect http vs https properly (don't assume https)
- [ ] Chrome plugin: open, search, open URL, read page, fill, click, submit
- [ ] Web agent prompt
- [ ] CAPTCHA / OTP hand-over to the user

### [ ] T18 · Mixed task (web → app) (P0 · 🏃 sprint)

- **Depends on:** T15, T17
- **Where:** `modules/agents/`
- **Accept:** "Check my PNR and send it to Priya" works end to end with one confirmation.

- [ ] Web result passed to the app agent with provenance
- [ ] End-to-end test on the phone

## Notes

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
