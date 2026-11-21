# v0.1 — Hello Mitra

**Phase 0–1 · Foundation** · Release channel: Developer phone only · Sprint day 1

**Status:** 🟡 In progress (14/35 steps)

## Goal

Mitra installs, hears English, speaks back, reads any screen and can tap, type and scroll — with a Stop button that always works.

## Done when (exit criteria)

- [ ] `direct` and `foss` debug builds install on an Android 10+ phone.
- [ ] Gemma 4 E2B returns valid JSON on a 6 GB phone through LiteRT-LM.
- [ ] Saying "Turn on the torch" shows the transcript and Mitra replies by voice.
- [ ] A debug screen lists the current screen's elements with provenance tags.
- [ ] Stop cancels a running task in ≤ 300 ms.

## Read before starting

- docs/ARCHITECTURE.md §2–4
- docs/TRUST_ENGINE_SPEC.md §2 (provenance)

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [ ] T01 · Project skeleton (P0 · 🏃 sprint)

- **Depends on:** none
- **Where:** `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/`, `.github/workflows/ci.yml`
- **Accept:** `./gradlew assembleDirectDebug assembleFossDebug test spotlessCheck` passes on CI; `foss` has no Firebase/GMS (CI step checks this).

- [x] Multi-module layout (`app/` + `modules/`)
- [x] Flavours `direct` and `foss`
- [x] Spotless/ktlint, CI workflow, `.gitignore` for keys and models
- [x] First sync in Android Studio; update versions it suggests
- [ ] First green CI run on GitHub

### [ ] T02 · engine-api types (P0 · 🏃 sprint)

- **Depends on:** T01
- **Where:** `modules/engine-api/`
- **Spec:** docs/ARCHITECTURE.md §3
- **Accept:** Pure Kotlin module, no Android imports; all tests green.

- [x] `ExecutionEngine`, `ScreenSnapshot`, `ScreenElement`, `Provenance`, `ActionResult`, `GlobalAction`, `IntentSpec`
- [x] Unit tests written
- [x] Confirm the JSON round-trip test is green in a real Gradle run

### [ ] T03 · Accessibility service + screen reading (P0 · 🏃 sprint)

- **Depends on:** T02
- **Where:** `modules/engine-access/`
- **Spec:** docs/TRUST_ENGINE_SPEC.md §2
- **Accept:** On the phone the debug screen shows tagged elements for Settings, WhatsApp and Chrome; password fields show `secure=true` and no text.

- [x] `MitraAccessibilityService` + `mitra_accessibility.xml`
- [x] `NodeMapper` converts nodes to `ScreenElement` with provenance
- [ ] Debounce snapshots (screen must be stable before reading)
- [ ] Release nodes after each snapshot; never keep `AccessibilityNodeInfo`
- [ ] Mark `user_content` containers from plugins
- [ ] Unit tests for provenance tagging with fake window trees (Robolectric)
- [ ] Debug screen in the app that lists elements + tags

### [ ] T04 · Accessibility actions (P0 · 🏃 sprint)

- **Depends on:** T03
- **Where:** `modules/engine-access/AccessibilityEngine.kt`
- **Accept:** Open quick settings, toggle torch, open Settings › Display, type in a search box, scroll a list. Typing into a password field returns `Failed(SECURE_FIELD)`.

- [x] tap, long press, type (refuses secure fields), scroll, swipe, global actions
- [x] Tap falls back to the nearest clickable parent
- [ ] Tap falls back to a gesture at the element centre
- [ ] Wait for a stable screen after every action
- [ ] Manual check on the phone (list below)

### [ ] T05 · Stop overlay (P0 · 🏃 sprint)

- **Depends on:** T04
- **Where:** `modules/engine-access/StopOverlay.kt`, `app/`
- **Accept:** Stop works from any app within 300 ms (measured).

- [x] Red Stop button drawn as an accessibility overlay
- [ ] Connect `StopOverlay.onStop` to the task runner (cancel job + `engine.stop()`)
- [ ] Persistent notification with Stop while a task runs
- [ ] Move the button if it covers the element being tapped
- [ ] Test: Stop cancels a fake task in ≤ 300 ms

### [ ] T06 · Model runtime (LiteRT-LM + Gemma 4 E2B) (P0 · 🏃 sprint)

- **Depends on:** T01
- **Where:** `modules/model/`, `gradle/libs.versions.toml (litertlm)`
- **Accept:** On a 6 GB phone a fixed prompt returns valid JSON; tier detection is unit-tested.

- [x] `LocalModel` interface + `FakeModel` for tests
- [ ] Enable `litertlm-android` in the version catalog; check the API in the official docs
- [ ] Model download on first launch with SHA-256 check
- [ ] `DeviceTier` detection (A/B/C) + unit test
- [ ] One shared model instance; cache the system prompt
- [ ] Log time to first token

### [ ] T07 · Voice in and out (English) (P0 · 🏃 sprint)

- **Depends on:** T01
- **Where:** `modules/voice/`, `app/MainActivity.kt`
- **Accept:** Saying "Turn on the torch" shows the transcript; Mitra speaks within 0.5 s of end of speech.

- [x] `AndroidTts` (en-IN) and `YesNo` parser
- [ ] `SpeechInput` with Android `SpeechRecognizer` (en-IN, partial results)
- [ ] Push-to-talk button on Home
- [ ] `RECORD_AUDIO` runtime permission flow

## Notes

- T01 local build green; waiting for CI + phone install.

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
