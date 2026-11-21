# UI_SPEC.md — Mitra screens

> **This is the UI to build.** The pictures in `docs/design/screens/` are the approved design (owner: DINESH S, 21 Nov 2026).
> Build in Jetpack Compose + Material 3 with the tokens in §2. Match **layout, content, order and wording**. Exact pixels do not need to match.
> All 10 screens on one page: [`design/all-screens.png`](design/all-screens.png) · Brand and logo: [`../brand/`](../brand/README.md)

Mitra is an **assistant, not a dashboard**. You say "Hey Mitra" on any screen, your words appear live, the AI thinks **on the phone**, and Mitra does the task **in front of you** in the real app. The app itself is small: **Home, History, Setup**.

---

## 1. Design principles

1. **Say it, watch it, stay in control.** Speech in, the real app on screen, Stop always one tap away.
2. **One idea per screen.** At most three text levels (title, body, caption). No dense rows, no legends.
3. **The real app stays visible.** Mitra never hides the app it is working in; the panel is a small capsule, a card or a fade.
4. **Safety is visible, and calm.** Sending, submitting and deleting need a yes. Secrets (CAPTCHA, OTP, password, payment) are **handed to the user**. Blocked attacks are shown in History.
5. **On the phone.** Speech, the AI and the screen reading stay on the device. Say so in small text where it matters ("On this phone").
6. **For older and first-time users.** Touch targets ≥ 48 dp (main buttons 56 dp), body text ≥ 16 sp, works at 200% font size, full TalkBack labels, never colour-only meaning, respects "remove animations".

## 2. Design tokens (use these, not ad-hoc colours)

In code: `modules/ui/.../Theme.kt` (`MitraColors`, `MitraBrush`, `MitraShapes`). Use them through `MaterialTheme` where a slot exists.

| Token | Hex | Use |
|---|---|---|
| `Blue` (primary) | `#2F5BFF` | main buttons, selected tab, links, progress, caret |
| `BlueSoft` | `#EAF0FF` | selected tab, tonal icon circles, how-to boxes, field highlight |
| `Violet` / `Cyan` | `#7C5CFF` / `#22D3EE` | orb and gradient only |
| `Ink` | `#101828` | titles and body text |
| `Muted` | `#667085` | subtitles, helper text |
| `Faint` | `#98A2B3` | captions, inactive icons, the unsettled end of the transcript |
| `Line` | `#EAECF0` | hairlines between list rows (no card borders) |
| `Tonal` | `#F2F4F7` | quoted message, inputs, secondary buttons |
| `Background` / `Surface` | `#F6F7FB` / `#FFFFFF` | app pages / cards and sheets |
| `Success` · container · text | `#12B76A` · `#E7F8EF` · `#067647` | Done, allowed, granted |
| `Warning` · container · text | `#F79009` · `#FEF0C7` · `#B54708` | Waiting, "your turn", needs you |
| `Danger` · container · text | `#F04438` · `#FEE4E2` · `#B42318` | Stop square, Blocked |
| `Night` | `#101828` | dark hero card, dark launcher tile |

**Orb / gradient:** conic blue → violet → cyan with a soft white highlight at the top-left and a faint glow (`MitraBrush.Aurora`). One orb per screen at most.
**Shape:** cards 24 dp, sheets 36 dp, buttons 20 dp, pills and capsule fully rounded. **Depth:** soft shadow instead of borders. Cards are white on the `Background`.
**Spacing:** 22 dp page padding, 14 dp between cards, rows 12–16 dp.
**Icons:** one line style (1.9 stroke, round caps), shown inside a tinted circle or rounded square (38 dp).
**Font:** DM Sans (open licence, bundled in `res/font/`). System font is the fallback. Titles 34 sp Bold; panel transcript 30 sp Bold; body 16 sp; caption 13–14 sp, `Muted`.

## 3. Ways to start Mitra

| Way | Task | Notes |
|---|---|---|
| Tap the orb on **Home** | T20 | always available |
| Say **"Hey Mitra"** | T45 (v1.x) | on-device wake word; needs a foreground service with a visible notification; off by default; battery note in Setup |
| Side button / "hold Home" (Android default-assistant role) | T58 | optional; the user picks Mitra as the assistant in Setup |
| Floating orb and Quick Settings tile | T58 | optional; needs "Show over apps" |

Until T45 and T58 exist, the **orb** is the only way to start. Hide "Say 'Hey Mitra'" text on Home when the wake word is off (the screenshot shows it on).

## 4. Navigation map

```
Over any app (the assistant panel, drawn as an overlay)
   1 Listening → 2 Working ⇄ 3 Confirm / 4 Your turn → 5 Done
                                      └ variants: Paused · Blocked · Stopped · Couldn't finish

Inside the Mitra app (bottom tab bar: Home | History | Setup)
   6 Home          tap orb → panel 1
   7 History       tap a task → expanded audit trail
   8 Setup  →  9 Permissions
            →  10 On-device AI
            →  (not drawn yet) Safety rules · Privacy · Apps Mitra knows · Language
```

- The floating tab bar shows on Home, History and Setup only. Sub-screens have a back link instead.
- Back from a running task does **not** cancel it; only **Stop** does.
- **First run:** open **9 Permissions** as "Step 1 of 2", then **10 On-device AI** as "Step 2 of 2" (download), then Home. The two screens get a step label and a Continue button in this mode. (This header variant is not drawn; keep it minimal.)

## 5. Screens over any app (the assistant panel)

The panel is drawn by Mitra over whatever app is open. It never covers the element Mitra is about to tap (move it). See D-016 for how it is drawn.

### 5.1 Listening — `screens/01-listening.png` · T56, T57
- Frosted white fade rising from the bottom over the current app. Label "● Listening" (blue).
- **Live transcript**, 30 sp bold, updating with partial results. The last unsettled word(s) are `Faint`, settled words `Ink`, with a blue caret.
- **Orb** at the bottom centre with soft ripples that follow the microphone level. Tiny line under it: "🔒 On this phone".
- **End of speech:** 1.2 s of silence, or tap the orb. A tap on the transcript area cancels.
- **Accept:** first words appear within 300 ms of speech; nothing is sent to a server; TalkBack announces "Listening".

### 5.2 Working — `screens/02-working.png` · T56, T10
- The real app stays fully visible. The element Mitra acts on gets a soft blue ring and a tap ripple; read-only targets get the ring only.
- A white **capsule** at the bottom: small orb, current step in plain words ("Reading the result"), "Step 2 of 5", a hairline progress bar, and a round red-tint **Stop** button (red square).
- Mitra speaks short updates only when needed (end result, a question). It does not narrate every tap.
- **Accept:** Stop cancels the task in ≤ 300 ms and is reachable by TalkBack; the capsule never overlaps the target element.

### 5.3 Asks before sending — `screens/03-confirm.png` · T09, T56
Shown for verdicts **CONFIRM** and **CONFIRM_STRONG**.
- Dimmed app behind. Bottom card: tinted icon, title "Send to Priya?", app name ("WhatsApp"), the **exact text** in a grey quote box, "Say 'yes' or 'no'".
- Buttons: **Don't send** (tonal) and **Send** (primary), same height. For `CONFIRM_STRONG` the primary reads "Confirm with fingerprint" and opens `BiometricPrompt` (device-credential fallback).
- **Rules:** no default focus on Send; unclear speech = no; 30 s timeout = no. The text shown is the text that is sent (re-read from the screen before sending, R11).
- **Accept:** a voice "yes" and a tap both work; "no", Back and timeout all cancel without sending.

### 5.4 Your turn — `screens/04-your-turn.png` · T56, T22
Shown for verdict **USER_ONLY** (CAPTCHA, OTP, password, PIN, payment).
- Dimmed page; the field the user must fill is highlighted in amber. Card: amber lock, title "Your turn", one sentence ("Type the security code yourself. Mitra never types codes or passwords."), primary **I'm done**, red text **Stop**.
- Mitra never reads or types into the field. When the user taps **I'm done**, Mitra re-reads the screen and continues.
- **Variants (same layout):** **Paused** (verdict PAUSE: amber shield, the reason in one sentence, **Continue** / **Stop**; "Continue" acknowledges only the named rule R7, R12 or R13) · **Blocked** (verdict REFUSE or ABORT: red tint, "I can't do that", one sentence why, **OK**; the step appears red in History).
- **Accept:** a test with a password field shows this card and the engine receives no text for it.

### 5.5 Done — `screens/05-done.png` · T56
- Same frosted fade as Listening. Green check, "Done. I sent it to Priya.", caption "4 steps · saved in History". A small orb at the bottom. It closes by itself after 4 s or on tap.
- Mitra speaks the same sentence (short).
- **Variants:** **Stopped** ("Stopped. Nothing was sent." grey check) · **Couldn't finish** (amber icon, the reason in plain words, **Try again** / **Close**).
- **Accept:** every ending writes a task row; the sentence comes from the verifier's result, not from the model's own claim.

## 6. Screens inside the Mitra app

### 6.1 Home — `screens/06-home.png` · T20
- "Good evening" (caption) and title "How can I help?".
- **Orb**, ≈ 116 dp with a halo; tap = start listening (panel 5.1). Under it: "Say "Hey Mitra"" and "or tap the orb to speak" (first line only when the wake word is on).
- **Recent** card (last 3 tasks): status circle (green ✓ Done, amber clock Waiting, red shield Blocked), title, caption ("Done · 10:42"). "See all" opens History.
- Floating tab bar, Home selected.
- **Starter state:** `MainActivity` has a typed-task field behind a debug flag; keep it there for tests.

### 6.2 History — `screens/07-history.png` · T09, T20
- Title "History"; pills **All / Done / Blocked**.
- Grouped by day ("Today", "Yesterday"). One white card per task: status circle, title, caption. The newest task is **expanded** into a timeline: one row per step in plain words, with a green dot for allowed steps and an amber dot for "Asked you · you said 'yes'".
- Waiting tasks show what is needed ("you enter the UPI PIN"). Blocked tasks are red: "A message tried to give Mitra orders · Blocked · rule R7".
- Never show secure text or full screen text; show what the audit log stores.
- **Accept:** a blocked attack from the eval suite appears as a red row with its rule id.

### 6.3 Setup — `screens/08-setup.png` · T20, T21
- Title "Setup". **Readiness card**: progress ring "4 of 5", "Almost ready", "One permission is left".
- Group 1: **Permissions** (amber count badge) → 6.4 · **On-device AI** ("Ready") → 6.5.
- Group 2: **"Hey Mitra"** switch (hidden until T45) · **Language** ("English"; Hindi and Tamil appear in v1.0) · **Speak replies** switch.
- Group 3: **Safety rules** → *not drawn* · **Privacy** → *not drawn*.
- **Accept:** the ring and the badge update when the user returns from system Settings.

### 6.4 Permissions — `screens/09-permissions.png` · T20, T29
- Back link "Setup", title "Permissions", subtitle "Mitra asks only for what it needs."
- One white card with five rows (icon, name, one-line reason, status): **Microphone** "To hear you" · **Notifications** "Shows when Mitra listens" · **Control apps** "Tap and type for you" · **Show over apps** "For the panel and Stop" · **Battery** "Keeps 'Hey Mitra' on".
- Status = green check when granted, blue **Turn on** pill when not. The **Control apps** row expands to two numbered steps: "1 App info › ⋮ › **Allow restricted settings**" and "2 Accessibility › **Mitra** › On", plus the voice guide (T29).
- Footer: "🔒 Mitra never reads passwords, PINs or OTPs."
- **Accept:** returning from Settings updates the row without restarting the app. Hide **Battery** row copy about "Hey Mitra" until T45.

### 6.5 On-device AI — `screens/10-on-device-ai.png` · T06, T28
- Back link "Setup", title "On-device AI". **Dark hero card:** "Gemma 4 E2B", "2.58 GB · works offline", a green "Ready" pill and three big numbers: first word (s), speed (/s), memory (GB). Numbers come from the last speed test.
- List: **Speech and wake word** ("English · 122 MB", green check) · **Smaller model** ("About 1 GB, for slower phones" → T28) · **Cloud help** switch (**off by default**; "Off. Always asks first").
- Button **Run speed test**.
- **States:** checking phone → downloading (progress bar, pause/resume, Wi-Fi only) → verifying (SHA-256) → ready → error with **Try again**. The hero card shows the state in the pill.
- **Accept:** the numbers shown are measured, never typed in; cloud cannot be turned on without a per-task consent (test).

### 6.6 Not drawn yet (design in the same style, then ask the owner)

| Screen | Must contain | Task |
|---|---|---|
| **Safety rules** | "Ask me before" list (Sending *Always*, Submitting forms *Always*, locked), allowed apps and websites (banking and UPI are blocked, with a lock), standing rules (need phone PIN) | T21, T19 |
| **Privacy** | Cloud help switch, family helper (link/unlink), **Delete all Mitra data** (red, asks twice) | T21, T23, T59 |
| **Apps Mitra knows** | installed plugins with a switch and "Signed by Mitra team"; website recipes | T13–T16, T59 |
| **Language** | English now; Hindi and Tamil from v1.0 | T36 |

Use the same cards, rows, tab-less back link and tokens. Show the draft to the owner before building. Old wireframes of Control centre and Plugins are retired.

## 7. Shared components (build once in `modules/ui`, with `@Preview` and `contentDescription`)

| Component | Used on |
|---|---|
| `MitraOrb` (idle · listening · thinking · speaking; reduced-motion safe) | 5.1, 5.2, 5.5, 6.1 |
| `Capsule` (orb, step text, progress, `StopButton`) | 5.2 |
| `PanelCard` (icon, title, subtitle, body, buttons) and `PanelFade` (transcript + orb) | 5.1, 5.3–5.5 |
| `LiveTranscript` (settled / unsettled text, caret) | 5.1 |
| `MitraPrimaryButton`, `MitraTonalButton`, `TextAction` | all |
| `TaskRow` (status circle, title, caption) and `StepTimeline` (green/amber dots) | 6.1, 6.2 |
| `ReadinessCard` (ring) · `SettingsRow` (icon tile, title, value / switch / chevron / badge) | 6.3–6.5 |
| `PermissionRow` (status, **Turn on**, expandable steps) | 6.4 |
| `ModelHeroCard` (dark, stats) | 6.5 |
| `MitraTabBar` (floating pill; Home, History, Setup) | 6.1–6.3 |
| `Pill` (segmented All / Done / Blocked) | 6.2 |

The panel host lives outside the app screens (see D-016): components are in `modules/ui`, the overlay window is created by the service.

## 8. Voice and motion

- **Pipeline:** microphone → streaming on-device speech-to-text → live transcript → orchestrator. The orb state follows the task state: `IDLE` (slow breathing) → `LISTENING` (ripples with mic level) → `THINKING` (turning gradient) → `SPEAKING` (soft pulse).
- **Speech-to-text:** Android `SpeechRecognizer` with partial results and offline preference (on-device recogniser on Android 12+). The `foss` build cannot rely on Google speech services, so it needs an offline engine (Q-5). `modules/voice` hides this behind `SpeechToText`.
- **Replies:** short, spoken with `AndroidTts` (en-IN) only when "Speak replies" is on or the user spoke; always shown as text too.
- **Motion:** 150–250 ms ease; nothing flashes; with "remove animations" the orb is static and the ripples are a ring.

## 9. Rules for the AI building the UI

- **Follow the pictures and this file.** Don't invent screens, colours or flows. If something is missing (see §6.6), draw a short proposal and ask.
- All text in `strings.xml` (English now); no hard-coded strings, no concatenation: use placeholders (`%1$s`).
- Screens read state from a ViewModel. UI never calls the engine or the trust engine directly.
- Keep **Stop** reachable whenever a task runs (capsule + notification + in-app).
- Never show password/OTP text, full screen dumps or raw model output.
- Check every screen at 200% font size and with TalkBack before ticking T20 or T56.
- Placeholders such as "Priya", "10:42", "4 of 5" in the pictures are filled from real data.
- The pictures are made by `docs/design/source/build_screens.py` (HTML → PNG). To change the design, change that file and re-render; do not edit the PNGs.
