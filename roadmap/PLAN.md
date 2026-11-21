# MitraOS — Project Phases and Version Roadmap

**Product:** MitraOS, a secure, on-device multi-agent voice assistant (English first, then Indian languages) that operates any Android phone (shipped as one app)
**Owner:** DINESH S
**Document version:** 2.1 (21 November 2026) · app-only, open-source release

> Task checklists for each version are in the version folders next to this file; progress is on the [status board](README.md).

---

## 1. Overview

**Language plan:** every core feature (v0.1–v0.9) is built and tested in **English** first. **Hindi and Tamil arrive in v1.0**, after the core is complete; more Indian languages follow in v1.x. Strings and prompts are language-ready from day one.

MitraOS ships as **one Android app, Mitra**, for Android 10 and later. It controls other apps, the phone's own browser and phone functions through Android's Accessibility Service, with every action checked by a rule-based trust engine. There is no OS-level component and no built-in browser. Because Google Play does not allow accessibility-based autonomous agents, the app is released **open source outside Google Play**.

| Build | Contents | Distribution |
|---|---|---|
| `direct` | Full app, Firebase for family approvals | GitHub Releases, project website, Indus Appstore |
| `foss` | Full app without Firebase | F-Droid or a self-hosted F-Droid repository |

---

## 2. Project phases

| Phase | Name | Goal | Exit criteria |
|---|---|---|---|
| **0** | Foundation | Project setup, tooling, model on the phone | App skeleton with `direct` and `foss` flavours installs; Gemma 4 E2B runs through LiteRT-LM |
| **1** | App MVP | A complete, working Mitra app on mid-range phones | In English: voice → agents → trust engine → action works for apps, phone functions and web tasks in the phone's browser |
| **2** | Hardening | Make it safe and reliable | Attack suite passes; overlay detection; BiometricPrompt confirmation; works on Android 10 and 13+ phones |
| **3** | Evaluation and research release | Evidence that it works and is secure | Benchmark, attack-suite and ablation results; paper draft; open-source v0.9 |
| **4** | Pilot and public release | Real users and feedback | Ethics approval; user study; v1.0 on GitHub, Indus and F-Droid; NGO pilot |
| **5** | Platform and scale | Startup path | Phone-maker or telecom pre-install deals; developer SDK; assisted-service centres |

---

## 3. Version roadmap

Semantic versioning (`MAJOR.MINOR.PATCH`); below 1.0 is pre-release.

### v0.1 — "Hello Mitra" (Phase 0–1, internal)
- Kotlin + Jetpack Compose app, minSdk 29 (Android 10)
- Gemma 4 E2B on device via LiteRT-LM
- English voice in (Android speech recognition, en-IN) and out (Android TTS)
- Voice home screen
- **Accessibility engine:** read screen as labelled elements; tap, type, scroll, swipe; Back, Home, notifications, quick settings
- Floating Stop button and persistent notification
- Generic screen agent: simple tasks in Settings, quick settings (torch, Wi‑Fi, brightness) and YouTube

**Channel:** developer phone only

### v0.2 — "Trust" (Phase 1, internal)
- **Trust engine:** 4 checks (scope, provenance, risk, context), 14 rules
- Provenance tags from Accessibility APIs (`system_ui`, `app_layout`, `web_content`, `user_content`, `overlay`, `unknown`)
- Voice confirmation for send, post, delete and submit
- Password, PIN and OTP fields never read or typed; banking and UPI deny-list; send-rate limit
- Encrypted audit log

**Channel:** internal

### v0.3 — "Team" (Phase 1, internal)
- Orchestrator, app agent, web agent, memory agent, verifier, guardian on one shared model
- Shared task board with JSON schema checking
- **Cross-agent provenance**
- Fast path for single-agent requests; task-progress screen

**Channel:** internal

### v0.4 — "Plugins" (Phase 1, closed alpha)
- Plugin format (signed JSON recipes), loader and signature check
- Plugins: WhatsApp, Gmail, Settings / quick settings
- Read unread messages and mails, draft replies, send after "yes"
- Standing auto-reply rules (PIN required, every send logged)

**Channel:** APK to 3–5 testers (GitHub pre-release)

### v0.5 — "Web" (Phase 1, closed alpha)
- Web agent that opens and controls the **phone's own browser** (Chrome plugin recipe)
- Address-bar checks: domain allow-list per task, HTTPS only, look-alike domain warning; page text treated as data
- CAPTCHAs, passwords and OTPs handed to the user
- Mixed tasks ("check PNR status and send it to Priya")

**Channel:** APK to testers

### v0.6 — "Control centre" (Phase 1–2, public beta) — app screens + the assistant panel (T56–T59)
- History, permissions, privacy controls (delete all data, cloud on/off)
- Family approval and live view (`direct`: Firebase; `foss`: UnifiedPush or same phone)
- Automatic tier detection; Tier B 1 GB model; cloud with consent
- Guide mode fallback when accessibility is off

**Channel:** public APK on GitHub; `foss` build in a self-hosted F-Droid repo

### v0.7 — "Hardened" (Phase 2)
- **BiometricPrompt** confirmation for high-risk actions (system-drawn, cannot be faked)
- Overlay detection: refuse to tap through other apps' windows, warn the user
- Scam-call and screen-sharing pause (R12–R13)
- Restricted-settings setup guide by voice
- Tested on an Android 10 phone and an Android 13+ phone

**Channel:** public APK

### v0.9 — "Research preview" (Phase 3)
- IndiaAppBench (130 tasks) with automatic checkers
- Attack suite (6 families, 200+ cases)
- Ablations: each defence on/off, cross-agent provenance on/off, single vs multi-agent
- Performance tuning toward the 3-second first-action target

**Channel:** GitHub release with the paper

### v1.0 — "Bharat" (Phase 4, April 2027)
- **Indian languages, after all core features are done:** Hindi and Tamil voice, UI and spoken replies (code-mixed speech supported); "haan / nahin" confirmations
- Benchmark re-run in Hindi and Tamil
- User study (10–15 elderly, low-literate or visually impaired participants) in their own language, with fixes
- Messages (SMS) and Telegram plugins; IRCTC and DigiLocker web recipes
- Accessibility review (TalkBack, large text, contrast)
- Documentation: user guide, plugin authoring guide, security model

**Channels:** GitHub stable, project website, Indus Appstore, F-Droid

### v1.x — "Everyday Mitra" (Phase 4–5, mid-2027)
- "Mitra" wake word (openWakeWord)
- Encrypted memory with consent
- AppFunctions support on Android 16+
- More Indian languages (Telugu, Bengali, Marathi, Kannada, Malayalam); IndicConformer speech recognition on device
- LoRA fine-tuned Gemma model for Indian apps
- More plugins and languages

### v2.0 — "Mitra Platform" (Phase 5, 2028)
- Learn a task from one demonstration (skills)
- Proactive help from notifications (opt-in)
- Community plugin marketplace (signed plugins)
- Developer SDK for Indian app makers (AppFunctions-compatible)
- Assisted-service edition for service centres; pre-install edition for phone makers

---

## 4. Feature-to-version map

| Feature | First version |
|---|---|
| English voice in and out | v0.1 |
| On-device Gemma 4 | v0.1 |
| Accessibility engine (any app, phone functions) | v0.1 |
| Stop button and notification | v0.1 |
| Trust engine (4 checks, 14 rules) | v0.2 |
| Provenance tags | v0.2 |
| Voice confirmation; PIN/OTP/password protection | v0.2 |
| Audit log | v0.2 |
| Multi-agent team, cross-agent provenance | v0.3 |
| Plugin system; WhatsApp, Gmail, Settings | v0.4 |
| Auto-reply rules | v0.4 |
| Web agent using the phone's own browser | v0.5 |
| Control centre, family approval, tier detection, guide mode | v0.6 |
| BiometricPrompt confirmation, overlay detection, scam pause | v0.7 |
| Benchmark and attack evaluation | v0.9 |
| Hindi and Tamil (voice, UI, replies); Messages and Telegram plugins | v1.0 |
| Wake word, memory, AppFunctions, fine-tuned model | v1.x |
| Skills, proactive help, SDK, marketplace | v2.0 |

---

## 5. Release channels

| Channel | Build | Audience | First used |
|---|---|---|---|
| Internal | both | Developer only | v0.1 |
| GitHub pre-release | `direct` | Testers | v0.4 |
| GitHub release + website | `direct` | Public | v0.6 |
| Self-hosted F-Droid repo | `foss` | Privacy-focused users | v0.6 |
| Indus Appstore | `direct` | Indian users | v1.0 |
| F-Droid main repository | `foss` | Open-source users | v1.0 |
| Hand installation (ADB) | `direct` | NGO and pilot phones | v1.0 |

Before the first public release: verify the developer identity and register the package name in the Android Developer Console (developer verification reaches India in 2027).

---

## 6. Quality gates (every release)

1. All P0 test cases for the included features pass.
2. No open S1 bugs (any unsafe action or credential leak blocks the release).
3. Trust-engine unit-test coverage ≥ 60%.
4. Attack suite re-run; no regression in blocked attacks.
5. Every action in a test run appears in the audit log.
6. Release APK signed with the project's release key; checksums published.

---

## 7. Branch and build strategy

- `main` — always releasable; tagged per version (`v0.1.0` …)
- `phase<N>/<version>` — one working branch per roadmap version (e.g. `phase1/v0.1-hello-mitra`); one commit per task; merged to `main` with one pull request when the version's exit criteria are met (see `docs/GIT_WORKFLOW.md`)
- Flavours: `direct`, `foss`
- CI (GitHub Actions): build both flavours, run unit tests, trust-engine tests and attack regression tests on every merge; attach APKs to GitHub releases

---

## 8. Dependencies and risks by version

| Version | Key dependency | Main risk | Fallback |
|---|---|---|---|
| v0.1 | LiteRT-LM, Gemma 4 E2B | Model too slow on the test phone | 1 GB mobile model; cloud with consent |
| v0.4 | Messaging app layouts | Plugins break when apps update | Role/text matching; generic screen agent |
| v0.5 | Browser layouts, websites | Layout changes, CAPTCHAs | Browser recipe by role; CAPTCHAs to the user |
| v0.6 | Firebase free tier | Notification delays | UnifiedPush or same-phone approval |
| v0.7 | Restricted settings, Advanced Protection | Users cannot enable accessibility | Voice setup guide; guide mode fallback |
| v1.0 | Ethics approval, NGO partner | Study delayed | Family pilot results; full study later |
| v1.0 | Developer verification (2027) | Install friction for unverified apps | Verify early; F-Droid and Indus channels |
| v2.0 | Trademark | "Mitra" conflicts with an existing US app | Register "MitraOS"; legal check before launch |

---

## 9. Non-negotiables across all versions

- The trust engine is rule-based code, not an AI agent, and every action passes through it.
- Text from screens, messages, mails and web pages is data, never an instruction.
- PINs, OTPs, passwords and CAPTCHAs are always entered by the user.
- Banking and UPI apps are never automated; security settings are never changed.
- Voice, screens and memory stay on the device unless the user turns on the cloud for a task.
