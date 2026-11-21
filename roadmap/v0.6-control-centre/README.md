# v0.6 — Control centre (first public beta)

**Phase 1–2** · Release channel: Public APK on GitHub; `foss` in a self-hosted F-Droid repo · Sprint days 4–5

**Status:** 🟡 In progress (4/46 steps)

## Goal

A usable app for real people: onboarding, history, permissions, privacy, family approval, a first evaluation and the first public release.

## Done when (exit criteria)

- [ ] All UI strings in resources; TalkBack labels; 200% font works.
- [ ] Delete-all and banking-block tests pass.
- [ ] 20 English benchmark tasks and 30 attack cases run with M3 (unsafe actions without confirmation) = 0.
- [ ] Signed APKs + checksums published on GitHub.
- [ ] The assistant panel (listening, working, confirm, your turn, done) works over Chrome and WhatsApp.

## Read before starting

- docs/PROJECT_CONTEXT.md (users)
- docs/TESTING.md
- docs/UI_SPEC.md + docs/design/screens/ (the approved design)
- brand/README.md (logo and colours)

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [ ] T20 · App screens (Home, History, Setup) (P0 · 🏃 sprint)

- **Depends on:** T10
- **Where:** `app/`, `modules/ui/`
- **Spec:** docs/UI_SPEC.md §6 + docs/design/screens/06–10
- **Accept:** All strings in resources; TalkBack labels; touch targets ≥ 48 dp; works at 200% font size.

- [x] Home screen v0 with accessibility setup card and restricted-settings hint (debug version)
- [ ] Theme tokens, DM Sans font and shared components (UI_SPEC §7) with previews
- [ ] 6.1 Home with orb + Recent, floating tab bar
- [ ] 6.2 History with filter pills and expandable step timeline
- [ ] 6.3 Setup with readiness ring · 6.4 Permissions (live status, expandable steps) · 6.5 On-device AI (download, speed test)
- [ ] First-run flow: Permissions → On-device AI → Home
- [ ] TalkBack labels, 48 dp targets, 200% font check on every screen

### [ ] T56 · Assistant panel over any app (P0)

- **Depends on:** T05, T09, T10
- **Where:** `modules/ui/`, `modules/engine-access/`, `app/`
- **Spec:** docs/UI_SPEC.md §5 + docs/design/screens/01–05
- **Accept:** Over Chrome and WhatsApp: a CONFIRM step shows the exact text and cancels on 'no', Back and timeout; a password field shows Your turn and the engine receives no text for it; Stop cancels in ≤ 300 ms.

- [ ] Spike: Compose in an accessibility overlay vs plain Views; record the choice as D-016
- [ ] 5.1 Listening fade and 5.5 Done
- [ ] 5.2 Working capsule with Stop (never covers the target element)
- [ ] 5.3 Confirm card for CONFIRM and CONFIRM_STRONG (biometric)
- [ ] 5.4 Your turn for USER_ONLY; variants Paused (PAUSE), Blocked (REFUSE/ABORT), Stopped, Couldn't finish
- [ ] Connect to `TrustedExecutor` verdicts and the TaskRunner state; TalkBack and reduced-motion check

### [ ] T57 · Live speech: streaming transcript (P0)

- **Depends on:** T07
- **Where:** `modules/voice/`
- **Spec:** docs/UI_SPEC.md §5.1 and §8
- **Accept:** First words appear within 300 ms of speech on the test phone; no audio leaves the device (checked with the network off).

- [ ] `SpeechToText` emits partial and final results as a Flow (settled vs unsettled words)
- [ ] End-of-speech by 1.2 s silence or tap; microphone level for the orb
- [ ] On-device recogniser where available (Android 12+); offline engine for `foss` and Android 10–11 (decide in Q-5)
- [ ] Time to first partial word measured and logged

### [ ] T58 · Entry points: assistant role, floating orb, tile (P1)

- **Depends on:** T56
- **Where:** `app/`, `modules/engine-access/`
- **Spec:** docs/UI_SPEC.md §3
- **Accept:** Each entry point opens panel 5.1 from the lock-screen-free home screen and from inside Chrome.

- [ ] Optional default-assistant role (side button / hold Home)
- [ ] Optional floating orb (needs Show over apps)
- [ ] Quick Settings tile
- [ ] Setup shows which ways are on

### [ ] T59 · Design and build the screens not yet drawn (P1)

- **Depends on:** T20, T21
- **Where:** `docs/design/source/`, `modules/ui/`, `app/`
- **Spec:** docs/UI_SPEC.md §6.6
- **Accept:** Owner approved the drafts; every Setup row opens a working screen.

- [ ] Draft Safety rules, Privacy, Apps Mitra knows and Language in the same style; owner approves the pictures
- [ ] Add them to docs/UI_SPEC.md and docs/design/screens/
- [ ] Build them with the shared components
- [ ] Delete-all-data flow asks twice

### [ ] T21 · Permissions and privacy (P0 · 🏃 sprint)

- **Depends on:** T20
- **Where:** `app/`, `modules/data/`, `modules/trust/`
- **Accept:** Delete-all removes every table (test); a banking package cannot be allowed (test).

- [ ] Allowed apps/domains editor (banking cannot be added)
- [ ] Cloud toggle, off by default
- [ ] Delete all data

### [ ] T23 · Family approval (P1 · 🏃 sprint)

- **Depends on:** T09
- **Where:** `app/src/direct/`, `app/src/foss/`
- **Accept:** A CONFIRM_STRONG step can be approved from the linked device in `direct`.

- [ ] `direct`: Firebase Cloud Messaging
- [ ] `foss`: same-phone approval (UnifiedPush optional)

### [ ] T24 · Guide mode (P2 · 🏃 sprint)

- **Depends on:** T10
- **Where:** `modules/engine-guide/`
- **Accept:** With accessibility off, Mitra speaks Settings › Display › Dark theme steps and waits for the user's taps.

- [x] `GuideEngine` speaks each step
- [ ] Highlight / describe the target; wait for the user's tap
- [ ] Switch to guide mode when accessibility is off

### [ ] T25 · Eval harness + 20 English tasks (P0 · 🏃 sprint)

- **Depends on:** T18
- **Where:** `eval/`
- **Accept:** `python eval/run.py --config A --lang en` writes `eval/results/*.csv`.

- [x] YAML task format + validator (`run.py --list`) + 2 tasks
- [ ] ADB runner: start task, wait, check result
- [ ] 20 launch tasks (English)
- [ ] CSV results: success, steps, time to first action; configs A and C

### [ ] T26 · Attack suite v0 (P0 · 🏃 sprint)

- **Depends on:** T25
- **Where:** `eval/attacks/`
- **Spec:** docs/TESTING.md §3
- **Accept:** Zero unsafe actions without confirmation (M3 = 0).

- [x] First case (A1-001)
- [ ] ≥ 30 cases across the 6 families
- [ ] Report blocked vs succeeded attacks

### [ ] T27 · Release v0.6 (P0 · 🏃 sprint)

- **Depends on:** T25
- **Where:** `README.md`, `CHANGELOG.md`, `GitHub Releases`
- **Accept:** GitHub release published; a fresh install on a second phone completes onboarding.

- [ ] Create the release key (kept offline, never in git)
- [ ] Signed `direct` and `foss` APKs + SHA-256 checksums
- [ ] Release notes + install steps (restricted settings) + safety model
- [ ] Fresh install on a second phone

### [ ] T28 · Tier B model + cloud with consent (P2)

- **Depends on:** T06
- **Where:** `modules/model/`
- **Accept:** A 4 GB phone runs the small model; cloud is never used without consent (test).

- [ ] ~1 GB model for 3–5 GB phones
- [ ] Cloud fallback only after per-task consent; off by default

## Notes

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
