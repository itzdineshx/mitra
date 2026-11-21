# Architecture — MitraOS (Mitra app)

> Binding design for code. Picture: `docs/architecture.png`. Product context: `docs/PROJECT_CONTEXT.md`.
> One Android app · Android 10+ (minSdk 29) · no root · no built-in browser · English (`en-IN`) first.

---

## 1. Request flow (one spoken request)

```text
 user speaks ──► [voice] SpeechToText (en-IN) ──► UserGoal(text, languageTag)
                                                     │
                                                     ▼
                                    [agents] Orchestrator  ──► TaskScope + Plan (sub-goals per agent)
                                                     │
              ┌──────────────────────────────────────┴───────────────────────────┐
              ▼                                                                   ▼
   for each sub-goal:  App/Web agent ──► ProposedAction (one step, with evidence ids)
                                                     │
                                                     ▼
                                    [trust] TrustedExecutor ──► TrustEngine.check(action, scope, ctx)
                                                     │                         │
                                   ALLOW / CONFIRM ok│                         │ REFUSE / PAUSE / USER_ONLY / ABORT
                                                     ▼                         ▼
                                    [engine] ExecutionEngine.perform     speak reason / hand over / stop
                                                     │
                                                     ▼
                                    new ScreenSnapshot ──► Verifier (ok | retry | failed)
                                                     │
                                       loop until sub-goal done (max 25 steps per task)
                                                     │
                                                     ▼
                         [voice] TextToSpeech result  +  [data] audit log (every step + decision)
```

## 2. Modules and responsibilities

| Module | Responsibility | Key types | May depend on |
|---|---|---|---|
| `modules/engine-api` | Contract between brains and hands | `ExecutionEngine`, `ScreenSnapshot`, `ScreenElement`, `ActionResult`, `GlobalAction`, `Provenance` | — (pure Kotlin) |
| `modules/trust` | Deterministic safety | `TrustEngine`, `Rule` (R1–R14), `TrustedExecutor`, `Confirmer`, `AuditLog`, `DomainPolicy` | `engine-api` |
| `modules/model` | On-device LLM | `LlmRuntime` (LiteRT-LM wrapper), `PromptBuilder`, `JsonActionParser`, `DeviceTier` | — |
| `modules/agents` | Planning and step decisions | `Orchestrator`, `AppAgent`, `WebAgent`, `MemoryAgent`, `Verifier`, `Guardian`, `TaskBoard`, `TaskRunner` | `engine-api`, `trust`, `model`, `plugins`, `web`, `data` |
| `modules/plugins` | App recipes | `PluginLoader`, `SignatureVerifier`, `RecipeRunner`, `ScreenMatcher` | `engine-api`, `trust` |
| `modules/web` | Browser rules | `BrowserRecipe`, `AddressBarReader`, `DomainMatcher`, `LookAlikeDetector` | `engine-api` |
| `modules/voice` | Speech in/out | `SpeechToText`, `TextToSpeech`, `AndroidSpeechToText`, `AndroidTts` | — |
| `modules/data` | Encrypted storage | `MitraDatabase` (Room), DAOs for tasks, steps, rules, permissions, memory, plugins, family_links | — |
| `modules/ui` | Screens, assistant panel content, theme | Compose screens (Home, History, Setup …), panel components (`PanelCard`, `Capsule`, `LiveTranscript`, `MitraOrb`), `Theme.kt` tokens, strings (`StopOverlay` lives in `engine-access`, see D-010; panel window see D-016) | `agents` (via view models) |
| `modules/engine-access` | The hands | `MitraAccessibilityService`, `AccessibilityEngine`, `NodeMapper`, `ProvenanceTagger`, `GestureDispatcher` | `engine-api`, `plugins` (for user_content containers) |
| `modules/engine-guide` | Fallback hands | `GuideEngine` (speaks each step, waits for the user's tap) | `engine-api`, `voice` |
| `app` | Wiring | `MitraApp`, DI graph, flavours `direct`/`foss`, onboarding | everything |

## 3. Core interfaces (shape; exact code may evolve)

```kotlin
// modules/engine-api
enum class EngineKind { ACCESSIBILITY, GUIDE }
enum class Provenance { SYSTEM_UI, APP_LAYOUT, WEB_CONTENT, USER_CONTENT, OVERLAY, UNKNOWN }
enum class GlobalAction { BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS }

data class ScreenElement(
    val id: String,               // stable within one snapshot, e.g. "e42"
    val role: String,             // button, text, edit, image, list, switch…
    val text: String?,            // null when secure
    val contentDescription: String?,
    val viewId: String?,
    val bounds: Rect,
    val packageName: String,
    val windowType: String,       // application, system, overlay, ime, accessibility_overlay
    val provenance: Provenance,
    val clickable: Boolean,
    val editable: Boolean,
    val secure: Boolean           // isPassword() or plugin-declared secret
)

data class ScreenSnapshot(
    val foregroundPackage: String,
    val currentUrl: String?,      // filled by the browser recipe from the address bar
    val elements: List<ScreenElement>,
    val takenAtMillis: Long,
    val screenshotHash: String?   // Android 11+ only, hash never pixels
)

interface ExecutionEngine {
    val kind: EngineKind
    suspend fun isAvailable(): Boolean
    suspend fun launchApp(packageName: String): ActionResult
    suspend fun sendIntent(intent: IntentSpec): ActionResult
    suspend fun readScreen(): ScreenSnapshot
    suspend fun tap(elementId: String): ActionResult
    suspend fun longPress(elementId: String): ActionResult
    suspend fun type(elementId: String, text: String): ActionResult
    suspend fun scroll(direction: Direction, elementId: String? = null): ActionResult
    suspend fun swipe(from: Point, to: Point, durationMs: Long = 300): ActionResult
    suspend fun globalAction(action: GlobalAction): ActionResult
    fun stop()
}
```

`TrustedExecutor` (in `modules/trust`) is the **only** caller of `ExecutionEngine` mutating methods. Agents receive a `TrustedExecutor`, never a raw engine. `readScreen()` is allowed directly (read-only).

## 4. Accessibility engine details (`engine-access`)

**Service config** (`res/xml/mitra_accessibility.xml`):
- `accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeViewFocused"`
- `accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows|flagIncludeNotImportantViews"`
- `canRetrieveWindowContent="true"`, `canPerformGestures="true"`, `canTakeScreenshot="true"` (screenshots only used on API 30+)
- `notificationTimeout="100"`, spoken description in a string resource

**Reading a screen:** iterate `getWindows()`; for each window, walk `root` with a bounded depth (≤ 40) and node count (≤ 800); map nodes to `ScreenElement` via `NodeMapper`; tag with `ProvenanceTagger` (rules in `TRUST_ENGINE_SPEC.md` §2); release nodes. Debounce events: take a snapshot only after 300 ms of no `typeWindowContentChanged`.

**Acting:**
- Tap: prefer `ACTION_CLICK` on the node; if not clickable, find the nearest clickable ancestor; else `dispatchGesture` at the bounds centre.
- Type: `ACTION_SET_TEXT` with `ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE`; refuse if `isPassword()`.
- Scroll: `ACTION_SCROLL_FORWARD/BACKWARD` on the scrollable container; else a swipe gesture.
- Global: `performGlobalAction(GLOBAL_ACTION_BACK | HOME | RECENTS | NOTIFICATIONS | QUICK_SETTINGS)`.
- After every action wait for the next stable snapshot (timeout 3 s) before returning.

**Stop overlay:** a `TYPE_ACCESSIBILITY_OVERLAY` window with a single large Stop button; it calls `TaskRunner.stop()` which cancels the coroutine job and calls `engine.stop()`; target ≤ 300 ms.

**Overlay detection (R6):** any window whose package ≠ target package and ≠ system/IME and whose bounds intersect the target element → `OVERLAY`.

## 5. Web tasks (phone's own browser)

- `WebAgent` opens the default browser (or Chrome) with `launchApp`/`ACTION_VIEW`, then works only through `TrustedExecutor`.
- `AddressBarReader` reads the URL from the browser recipe's address-bar element on every snapshot → `ScreenSnapshot.currentUrl`.
- `DomainMatcher` checks `TaskScope.allowedDomains` (wildcards like `*.gov.in`) and `https`; `LookAlikeDetector` flags punycode/homoglyphs, edit distance ≤ 2 from an allowed domain, and allowed names used as sub-domains of other domains.
- Page area elements are `WEB_CONTENT`; browser chrome is `APP_LAYOUT`.
- CAPTCHA, password and OTP fields → `USER_ONLY`.

## 6. Model layer (`modules/model`)

- `LlmRuntime` wraps LiteRT-LM: `load(modelPath, backend = GPU|CPU)`, `generate(prompt, maxTokens, stop)`; keep **one** model instance for all roles; prefill and cache the shared system prompt.
- Model file: `gemma-4-E2B-it.litertlm` (2.58 GB) downloaded on first launch (Wi‑Fi only by default) to app-private storage; verify SHA-256 before loading.
- `DeviceTier`: A (≥ 6 GB RAM → E2B), B (3–5 GB → ~1 GB mobile E2B), C (< 3 GB → cloud with consent or guide mode).
- Output is parsed by `JsonActionParser` (kotlinx.serialization) against the role schema; invalid → one retry with the error, then `ask_user`.
- Cloud fallback (Gemini API) sits behind the same `LlmRuntime` interface and is used **only** when the user consents for that task. Not available in `foss` unless the user enters their own key.

## 7. Voice (`modules/voice`)

- `SpeechToText`: `SpeechRecognizer` with `EXTRA_LANGUAGE = "en-IN"`, partial results on, `EXTRA_PREFER_OFFLINE = true` where supported.
- `TextToSpeech`: `Locale("en", "IN")`; an instant acknowledgement ("Okay, checking") within 500 ms of end of speech.
- **Live transcript:** `SpeechToText` emits a Flow of partial and final results (settled and unsettled words) so the panel can show words as they are spoken (T57). The on-device recogniser is used where available; `foss` and Android 10–11 need an offline engine (Q-5).
- **Wake word:** "Hey Mitra" is detected on the device by a small keyword model in a foreground service with a visible notification; off by default (T45). Until it exists the orb is the entry point.
- Interfaces take `languageTag` so Hindi (`hi-IN`) and Tamil (`ta-IN`) plug in for v1.0.

## 8. Data (`modules/data`)

> Full spec: [`DATA_SPEC.md`](DATA_SPEC.md) (storage map, schema, redaction, keys, retention, Delete all, tests). The summary below is the short version.

Tables: `tasks`, `steps` (audit log), `rules`, `permissions`, `memory`, `plugins`, `family_links` (fields in `TRUST_ENGINE_SPEC.md` §7 and `PROJECT_CONTEXT.md`). Encryption key generated in Android Keystore. Steps kept 90 days. "Delete all data" drops every table and the model cache except the model file (user can delete that too).

## 9. Flavours and permissions

| | `direct` | `foss` |
|---|---|---|
| Firebase (family relay) | yes | no (UnifiedPush or same-phone) |
| Google Play Services | no dependency required | must not be included |
| Cloud LLM | optional, with consent | optional, user-supplied key |

Manifest permissions: `RECORD_AUDIO`, `INTERNET` (model download, optional cloud), `POST_NOTIFICATIONS` (API 33+), `FOREGROUND_SERVICE` (+ `FOREGROUND_SERVICE_MICROPHONE` / special-use type as required by the target SDK), `USE_BIOMETRIC`, `READ_CONTACTS` (resolve "Priya"), `READ_PHONE_STATE` (call-state check for R12, ask at runtime), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (optional). The accessibility service is declared with `BIND_ACCESSIBILITY_SERVICE`. Do not request permissions the PRD does not need.

## 10. Onboarding sequence

1. Language is English (India); Hindi and Tamil appear in Setup in v1.0. There is no separate welcome screen (D-014).
2. Permissions screen (UI_SPEC 6.4) as "Step 1 of 2": microphone and notifications first.
3. Accessibility: explain by voice; on Android 13+ guide the user to App info › ⋮ › **Allow restricted settings**, then Settings › Accessibility › Mitra.
4. On-device AI screen (UI_SPEC 6.5) as "Step 2 of 2": device check → tier → model download (Wi‑Fi only toggle) → cloud help (off by default).
5. Optional: link a family helper.
