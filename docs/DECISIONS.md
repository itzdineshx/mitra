# DECISIONS.md — architecture decision log

> Short records of decisions that shape the code. AI agents: read before proposing a change in the same area; add a new entry (next number) for any new dependency or design choice. Never edit an accepted decision silently — supersede it with a new entry.

Format: **ID · Title** — Status · Date · Decision · Why · Consequences.

---

**D-001 · App only, no OS-level component** — Accepted · 2026-11-20
Decision: Mitra is one normal Android app. No root, system image, privileged APIs, `INJECT_EVENTS` or hidden virtual displays. (Supersedes the earlier "MitraOS Core" plan.)
Why: a solo developer must finish and ship it; users install it like any app.
Consequences: Mitra acts on the visible screen while the user watches; overlays can be detected and refused but not blocked; confirmations use voice + `BiometricPrompt`.

**D-002 · Open-source release outside Google Play** — Accepted · 2026-11-20
Decision: release `direct` (GitHub, website, Indus Appstore) and `foss` (F-Droid) builds; no Play build.
Why: Play's Accessibility API policy prohibits autonomous initiate-plan-execute agents except verified accessibility tools.
Consequences: onboarding must guide "Allow restricted settings" (Android 13+); developer verification must be completed before public release (global rollout 2027); `foss` must not include Firebase or Google Play Services.

**D-003 · No built-in browser** — Accepted · 2026-11-20
Decision: web tasks drive the phone's own browser (Chrome or default) through the accessibility engine.
Why: users' existing logins work; smaller app; one execution path for apps and web.
Consequences: cannot strip hidden HTML; safety relies on address-bar checks (R4), `WEB_CONTENT` tagging and R7.

**D-004 · English first** — Accepted · 2026-11-20
Decision: all core features in English (`en-IN`) through v0.9; Hindi and Tamil in v1.0; more in v1.x.
Why: focus the sprint on agents and safety; strongest model and speech quality; clean baseline for evaluation.
Consequences: code is language-ready from day one (string resources, `languageTag` everywhere, swappable STT/TTS, per-language risk keyword files).

**D-005 · Trust engine is deterministic code** — Accepted · 2026-11-20
Decision: rules R1–R14 in pure Kotlin; no model calls; the engine re-derives risk and never trusts the agent's claim.
Why: an AI cannot be talked out of rules it does not run; testable and auditable.
Consequences: every new action type needs explicit risk derivation and tests.

**D-006 · One model, six roles** — Accepted · 2026-11-20
Decision: orchestrator, app, web, memory, verifier and guardian are prompts on a single Gemma 4 E2B instance via LiteRT-LM 0.18.
Why: phone RAM; load time; latency.
Consequences: roles take turns; prompts must be short; JSON outputs validated per role.

**D-007 · Plugins are signed JSON, not code** — Accepted · 2026-11-20
Decision: recipes are data (schema v1), Ed25519-signed, interpreted by `RecipeRunner`; no scripting.
Why: no code-execution risk from plugins; easy community contributions later.
Consequences: complex app logic stays in the generic screen agent.

**D-008 · Cross-agent provenance** — Accepted · 2026-11-20
Decision: any text derived from untrusted sources keeps a provenance list across agents, memory and summaries.
Why: blocks relayed injection (web text → app action), the project's novel research contribution.
Consequences: `TaskBoard` and `MemoryAgent` APIs carry provenance; R7 consults it.

**D-009 · Confirmation = voice + BiometricPrompt** — Accepted · 2026-11-20
Decision: `CONFIRM` = spoken "yes"; `CONFIRM_STRONG` (submit, new recipient, above max risk, standing rules) adds `BiometricPrompt` with device-credential fallback.
Why: the system-drawn prompt cannot be faked by other apps.
Consequences: `USE_BIOMETRIC` permission; graceful fallback on phones without biometrics.

**D-010 · Stop overlay lives in `engine-access`** — Accepted · 2026-11-21
Decision: `StopOverlay` is drawn by `MitraAccessibilityService` as a `TYPE_ACCESSIBILITY_OVERLAY` window, so it is in `engine-access`, not `modules/ui`.
Why: that window type needs the service's context; it also needs no extra overlay permission and sits above other apps.
Consequences: `app` sets `StopOverlay.onStop` to cancel the running task.

**D-011 · `modules/trust` uses `modules/web` for rule R4** — Accepted · 2026-11-21
Decision: domain allow-list and look-alike checks live in `modules/web` (`DomainPolicy`); `TrustEngine` calls it. Both stay pure Kotlin.
Why: one implementation for the web agent and the trust engine.

**D-012 · detekt deferred** — Accepted · 2026-11-21
Decision: the starter uses ktlint (Spotless) and Android Lint only; detekt is added when a stable release supports Kotlin 2.4.
Why: detekt 1.23.x is built for older Kotlin and can break the build on newer compilers.

**D-013 · `modules/` folder and `roadmap/` checklists** — Accepted · 2026-11-21
Decision: all app parts live in `modules/` with short Gradle names (`:trust`, `:agents` …); `shared/` is gone. Work is tracked in `roadmap/<version>/README.md` checklists (replaces `docs/TASKS.md`); `docs/ROADMAP.md` moved to `roadmap/PLAN.md`.
Why: "shared" suggested a second app; one folder per version makes progress visible and gives agents one place to look.
Consequences: `scripts/roadmap_status.py` keeps the status board in sync; CI checks it.

**D-014 · Assistant-first UI** — Accepted · 2026-11-21
Decision: Mitra is an assistant panel drawn over any app (Listening, Working, Confirm, Your turn, Done) plus a small app with three tabs: Home, History, Setup. The old 12-screen design (Welcome, Control centre, Plugins tab …) is retired. Spec: `docs/UI_SPEC.md`.
Why: the owner wants a "Hey Google"-style experience: speak anywhere, watch the task happen in the real app, keep a Stop button. The app should hold only tasks, history and setup.
Consequences: new tasks T56–T59; first run reuses Permissions and On-device AI screens; Safety rules, Privacy, Apps and Language screens are drafted later (UI_SPEC §6.6).

**D-015 · Brand: lotus mark, aurora colours, DM Sans** — Accepted · 2026-11-21
Decision: the logo is a lotus with a glowing core over two ripple arcs; colours blue `#2F5BFF`, violet `#7C5CFF`, cyan `#22D3EE`, night `#101828`; font DM Sans (open licence). Files are in `brand/`; launcher and notification icons are installed in `app/src/main/res`.
Why: "mitra" means friend; the lotus is friendly and Indian; the core links the logo to the voice orb in the UI.
Consequences: do not redraw the logo; use the flat white or night versions where gradients are not allowed.

**D-016 · Assistant panel is drawn by the accessibility service** — Proposed · 2026-11-21
Decision (proposed): the panel is an overlay window created by `MitraAccessibilityService` (type `TYPE_ACCESSIBILITY_OVERLAY`, like the Stop overlay in D-010), so it needs no extra permission. Its content comes from `modules/ui`. T56 starts with a spike to choose between a `ComposeView` hosted in the service (needs lifecycle and saved-state owners set by hand) and plain Views; the result replaces this entry.
Why: same reasons as D-010; the panel must stay above other apps and survive the app being closed.
Consequences: `modules/ui` content must not depend on an Activity.

**D-017 · Verified toolchain versions (T01)** — Accepted · 2026-11-21
Decision: toolchain and dependency versions verified with Gradle 9.8 and Android SDK 36:
AGP `9.4.1`, Kotlin `2.4.20`, Coroutines `1.10.2`, Serialization `1.9.0`, AndroidX Core KTX `1.16.0`, Activity Compose `1.10.1`, Lifecycle `2.9.2`, Compose BOM `2025.07.00`, Biometric `1.1.0`, JUnit `5.13.4`, Spotless `7.2.1`.
Why: establishes clean baseline build across pure Kotlin and Android modules with multi-flavour support (`direct` and `foss`).
Consequences: build passes spotless, all unit tests, and both flavour assemblies out of the box.

**D-018 · LiteRT-LM runtime and RAM tier thresholds (T06)** — Accepted · 2026-11-21
Decision: enable `com.google.ai.edge.litertlm:litertlm-android:0.18.0` for on-device inference using CPU backend (`Backend.CPU()`). Device tiers are derived with tolerance for OS/hardware reserved memory from `ActivityManager.MemoryInfo.totalMem`:
- Tier A (Gemma 4 E2B): $\ge 5.2$ GiB (`5_583_457_485` bytes; covers advertised 6 GB+ phones that typically report 5.4–5.8 GB).
- Tier B (small model, T28): $\ge 2.6$ GiB (`2_791_728_742` bytes; covers advertised 3–5 GB phones reporting 2.7–4.8 GB).
- Tier C (guide mode / cloud with consent): $< 2.6$ GiB.
Why: advertised RAM is never reported in full by the kernel; realistic tolerances prevent falsely downgrading 6 GB devices.
Consequences: `FakeModel` remains test default; `LiteRtLocalModel` executes off-thread when real model file is placed in `Android/data/com.mitra.app/files/models/`.

---

## Open questions (owner to decide)

- Q-1 Declare `isAccessibilityTool="true"`? Mitra serves visually impaired users, but the declaration has policy implications. Default: not declared until decided.
- Q-2 DI: Hilt vs manual. Default: Hilt unless build times hurt the sprint.
- Q-3 Database encryption library choice (SQLCipher vs Jetpack Security-based approach). Default: SQLCipher for Room.
- Q-4 Which 8 apps are frozen for the benchmark.
- Q-5 Offline speech-to-text for the `foss` flavour and Android 10–11 (the on-device Google recogniser is not available there): Vosk, whisper.cpp, or another open engine. Decide before T57. Wake word default: openWakeWord (T45).
- Q-6 Model licence: confirm the licence terms of Gemma 4 E2B and any smaller model before publishing a release or any mirror; Mitra downloads the model, it does not bundle it.
- Q-7 May the encrypted database open while the phone is locked? Default yes (the wake word needs it). See docs/DATA_SPEC.md §5.
