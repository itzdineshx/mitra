# v0.4 — Plugins

**Phase 1 · App MVP** · Release channel: GitHub pre-release (3–5 testers) · Sprint day 3

**Status:** 🟡 In progress (3/17 steps)

## Goal

Signed JSON recipes make common apps fast and reliable: Settings, WhatsApp and Gmail.

## Done when (exit criteria)

- [ ] Unsigned or tampered plugins never load.
- [ ] Settings tasks work by voice.
- [ ] WhatsApp reply flow works with one "yes"; malicious incoming messages trigger nothing.

## Read before starting

- docs/PLUGIN_SPEC.md
- plugins-builtin/README.md

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [ ] T13 · Plugin loader + signature check + runner (P0 · 🏃 sprint)

- **Depends on:** T09
- **Where:** `modules/plugins/`, `scripts/sign_plugins.py`
- **Spec:** docs/PLUGIN_SPEC.md §2, §4
- **Accept:** Unsigned or tampered plugin refused (R14 test); runner steps all go through `TrustedExecutor`.

- [x] Schema v1 data classes + author-rule validation
- [x] Ed25519 verification + tests; signing script
- [ ] Ed25519 verifier that works on Android 10–12 (Tink or BouncyCastle)
- [ ] `PluginKeys.kt` with the public key
- [ ] Screen matcher, `{param}` expansion, verify blocks
- [ ] Runner sends every step through `TrustedExecutor`; falls back to the generic agent

### [ ] T14 · Settings / quick-settings plugin (P0 · 🏃 sprint)

- **Depends on:** T13
- **Where:** `plugins-builtin/settings/`
- **Accept:** Torch, Wi-Fi, DND, brightness, dark theme and alarm all work by voice.

- [x] `plugin.json` started (Wi-Fi settings, quick settings, alarm)
- [ ] torch, Wi-Fi, DND, brightness, dark theme
- [ ] Sign it and load it in the app

### [ ] T15 · WhatsApp plugin + replies (P0 · 🏃 sprint)

- **Depends on:** T13
- **Where:** `plugins-builtin/whatsapp/`
- **Accept:** "Tell Priya I'll reach by 6" → draft read aloud → "yes" → sent and verified; a malicious incoming message triggers nothing (R7).

- [ ] Confirm view ids with Layout Inspector / `uiautomator dump`
- [ ] `read_unread`, `open_chat`, `type_reply`, `send`
- [ ] Declare `user_content` containers
- [ ] Injection test: incoming message tries to trigger an action

### [ ] T16 · Gmail plugin (P1 · 🏃 sprint)

- **Depends on:** T13
- **Where:** `plugins-builtin/gmail/`
- **Accept:** "Read my unread mails" and reply-after-yes work on a test account.

- [ ] `read_unread`, `open_mail`, `reply`, `send`

### [ ] T19 · Standing auto-reply rules (P1 · 🏃 sprint)

- **Depends on:** T15, T09
- **Where:** `modules/agents/`, `modules/data/`
- **Accept:** Creating a rule without PIN fails; with PIN works; R10 still enforced.

- [ ] Create a rule only after `BiometricPrompt`
- [ ] Log every automatic send
- [ ] R10 rate limit still applies

## Notes

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
