# v0.1 — Hello Mitra

**Phase 0–1 · Foundation** · Release channel: Developer phone only · Sprint day 1

**Status:** ✅ Done (35/35 steps)

## Goal

Mitra installs, hears English, speaks back, reads any screen and can tap, type and scroll — with a Stop button that always works.

## Done when (exit criteria)

- [x] `direct` and `foss` debug builds install on an Android 10+ phone.
- [x] Gemma 4 E2B returns valid JSON on a 6 GB phone through LiteRT-LM.
- [x] Saying "Turn on the torch" shows the transcript and Mitra replies by voice.
- [x] A debug screen lists the current screen's elements with provenance tags.
- [x] Stop cancels a running task in ≤ 300 ms.

## Read before starting

- docs/ARCHITECTURE.md §2–4
- docs/TRUST_ENGINE_SPEC.md §2 (provenance)

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [x] T01 · Project skeleton (P0 · 🏃 sprint)

- **Depends on:** none
- **Where:** `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/`, `.github/workflows/ci.yml`
- **Accept:** `./gradlew assembleDirectDebug assembleFossDebug test spotlessCheck` passes on CI; `foss` has no Firebase/GMS (CI step checks this).

- [x] Multi-module layout (`app/` + `modules/`)
- [x] Flavours `direct` and `foss`
- [x] Spotless/ktlint, CI workflow, `.gitignore` for keys and models
- [x] First sync in Android Studio; update versions it suggests
- [x] First green CI run on GitHub

### [x] T02 · engine-api types (P0 · 🏃 sprint)

- **Depends on:** T01
- **Where:** `modules/engine-api/`
- **Spec:** docs/ARCHITECTURE.md §3
- **Accept:** Pure Kotlin module, no Android imports; all tests green.

- [x] `ExecutionEngine`, `ScreenSnapshot`, `ScreenElement`, `Provenance`, `ActionResult`, `GlobalAction`, `IntentSpec`
- [x] Unit tests written
- [x] Confirm the JSON round-trip test is green in a real Gradle run

### [x] T03 · Accessibility service + screen reading (P0 · 🏃 sprint)

- **Depends on:** T02
- **Where:** `modules/engine-access/`
- **Spec:** docs/TRUST_ENGINE_SPEC.md §2
- **Accept:** On the phone the debug screen shows tagged elements for Settings, WhatsApp and Chrome; password fields show `secure=true` and no text.

- [x] `MitraAccessibilityService` + `mitra_accessibility.xml`
- [x] `NodeMapper` converts nodes to `ScreenElement` with provenance
- [x] Debounce snapshots (screen must be stable before reading)
- [x] Release nodes after each snapshot; never keep `AccessibilityNodeInfo`
- [x] Mark `user_content` containers from plugins (implemented via `userContentViewIds`)
- [x] Unit tests for provenance tagging with fake window trees
- [x] Debug screen in the app that lists elements + tags

### [x] T04 · Accessibility actions (P0 · 🏃 sprint)

- **Depends on:** T03
- **Where:** `modules/engine-access/AccessibilityEngine.kt`
- **Accept:** Open quick settings, toggle torch, open Settings › Display, type in a search box, scroll a list. Typing into a password field returns `Failed(SECURE_FIELD)`.

- [x] tap, long press, type (refuses secure fields), scroll, swipe, global actions
- [x] Tap falls back to the nearest clickable parent
- [x] Tap falls back to a gesture at the element centre
- [x] Wait for a stable screen after every action
- [x] Manual check on the phone (quick settings, torch, display settings, type search, scroll list)

### [x] T05 · Stop overlay (P0 · 🏃 sprint)

- **Depends on:** T04
- **Where:** `modules/engine-access/StopOverlay.kt`, `app/`
- **Accept:** Stop works from any app within 300 ms (measured).

- [x] Red Stop button drawn as an accessibility overlay
- [x] Connect `StopOverlay.onStop` to the task runner (cancel job + `engine.stop()`)
- [x] Persistent notification with Stop while a task runs
- [x] Move the button if it covers the element being tapped
- [x] Test: Stop cancels a fake task in ≤ 300 ms

### [x] T06 · Model runtime (LiteRT-LM + Gemma 4 E2B) (P0 · 🏃 sprint)

- **Depends on:** T01
- **Where:** `modules/model/`, `gradle/libs.versions.toml (litertlm)`
- **Accept:** On a 6 GB phone a fixed prompt returns valid JSON; tier detection is unit-tested.

- [x] `LocalModel` interface + `FakeModel` for tests
- [x] Enable `litertlm-android` in the version catalog; check the API in the official docs
- [x] ModelFiles path lookup + SHA-256 verify (in-app downloader deferred to T60)
- [x] `DeviceTier` detection (A/B/C) + unit test
- [x] One shared model instance; cache the system prompt
- [x] Log time to first token

### [x] T07 · Voice in and out (English) (P0 · 🏃 sprint)

- **Depends on:** T01
- **Where:** `modules/voice/`, `app/MainActivity.kt`
- **Accept:** Saying "Turn on the torch" shows the transcript; Mitra speaks within 0.5 s of end of speech.

- [x] `AndroidTts` (en-IN) and `YesNo` parser
- [x] `SpeechInput` with Android `SpeechRecognizer` (en-IN, partial results)
- [x] Push-to-talk button on Home
- [x] `RECORD_AUDIO` runtime permission flow

## Notes

- T01: builds assemble cleanly locally and on CI (GitHub Actions run #44931696501); direct (`com.mitra.app`) and foss (`com.mitra.app.foss`) installed and launched on device (LAVA LXX503 Android 14).
- T02: pure Kotlin module verified with no Android imports; 100% unit tests green.
- T03: screen reading and provenance tagging verified on device across Settings (250 elements), Chrome (179 elements, address bar read: `https://Search Google or type URL`), and WhatsApp (318 elements); debug screen lists elements with colored provenance badges; password fields show `secure=true` and `text=null`.
- T04: accessibility actions tested on device: quick settings opened, torch toggled on camera id 0 (verified via `dumpsys media.camera`), Settings › Display opened, search typed into task box, list scrolled; typing into password field is rejected with `Failed(reason=SECURE_FIELD, message=never types into secure fields)`.
- T05: Stop overlay latency measured on device across 10 repeated runs: min = 0.60 ms, median = 0.72 ms, max = 2.01 ms (well within the ≤ 300 ms requirement). Persistent ongoing notification with Stop action implemented.
- T06: in-app model downloader & manager implemented (`ModelDownloader`, `OnDeviceAiCard`); Gemma 4 E2B (2.58 GB) downloaded, verified via SHA-256 (`181938105e...`), and run via LiteRT-LM CPU backend on Tier A device (`LAVA LXX503`, 5.74 GB RAM).
  - Run 1 (cold): firstTokenMs=21,252 ms, 0.8 tok/s, 1,828 MB PSS, JSON valid=true (Action: TAP).
  - Run 2 (warm): firstTokenMs=13,859 ms, 1.3 tok/s, 1,187 MB PSS, JSON valid=true (Action: TAP).
  - Run 3 (warm): firstTokenMs=13,877 ms, 1.3 tok/s, 1,195 MB PSS, JSON valid=true (Action: TAP).
- T07: speech recognition (`AndroidSpeechRecognizer`) and TTS (`AndroidTts`) wired up; "Turn on the torch" recognized and displayed in transcript card, torch toggled on device, Mitra replied "Torch is on" with measured end-of-speech to TTS start latency of 21.54 ms (well within the ≤ 0.5 s requirement).

<!-- Decisions, surprises and links to PRs for this version. Design decisions also go in docs/DECISIONS.md. -->
