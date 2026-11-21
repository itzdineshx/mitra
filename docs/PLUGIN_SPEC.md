# Plugin Specification — MitraOS

> Plugins teach Mitra how a specific app works. They are **signed, code-free JSON recipes**: no scripts, no network, no Kotlin. The generic screen agent handles any app without a plugin.
> Module: `modules/plugins` (loader, signature check, runner). Built-in recipes: `plugins-builtin/`.

---

## 1. File layout

```text
plugins-builtin/
├── whatsapp/plugin.json   whatsapp/plugin.sig
├── gmail/plugin.json      gmail/plugin.sig
├── chrome/plugin.json     chrome/plugin.sig
└── settings/plugin.json   settings/plugin.sig
```

`plugin.sig` = Ed25519 signature of the exact bytes of `plugin.json`. The public key of the Mitra team is compiled into the app (`PluginKeys.kt`). Unknown or invalid signature → plugin is not loaded (rule R14).

## 2. Schema (v1)

```jsonc
{
  "schema": 1,
  "id": "mitra.plugin.whatsapp",          // unique, reverse-DNS style
  "version": "1.0.0",
  "app": { "package": "com.whatsapp", "minVersionCode": 0 },
  "languages": ["en"],                     // UI-label languages this recipe matches; add "hi","ta" in v1.0
  "screens": [                             // how to recognise screens (role/structure first, text last)
    {
      "id": "chat_list",
      "match": { "anyViewId": ["com.whatsapp:id/conversations_row_contact_name"] }
    },
    {
      "id": "chat",
      "match": { "allViewId": ["com.whatsapp:id/entry", "com.whatsapp:id/send"] },
      "user_content": ["com.whatsapp:id/message_text"]   // containers whose text is USER_CONTENT
    }
  ],
  "actions": [
    {
      "id": "open_chat",
      "risk": "read",
      "params": { "contact": "string" },
      "steps": [
        { "launch": "com.whatsapp" },
        { "tap": { "viewId": "com.whatsapp:id/menuitem_search" } },
        { "type": { "viewId": "com.whatsapp:id/search_input", "text": "{contact}" } },
        { "tap": { "viewId": "com.whatsapp:id/conversations_row_contact_name", "textEquals": "{contact}" } }
      ],
      "verify": { "screen": "chat", "textContains": { "viewId": "com.whatsapp:id/conversation_contact_name", "value": "{contact}" } }
    },
    {
      "id": "type_reply",
      "risk": "write",
      "params": { "text": "string" },
      "steps": [ { "type": { "viewId": "com.whatsapp:id/entry", "text": "{text}" } } ],
      "verify": { "textEquals": { "viewId": "com.whatsapp:id/entry", "value": "{text}" } }
    },
    {
      "id": "send",
      "risk": "send",
      "confirm": true,
      "steps": [ { "tap": { "viewId": "com.whatsapp:id/send" } } ],
      "verify": { "lastBubbleEquals": "{text}" }
    },
    {
      "id": "read_unread",
      "risk": "read",
      "steps": [ { "launch": "com.whatsapp" }, { "readList": { "viewId": "com.whatsapp:id/conversations_row_message_count" } } ]
    }
  ]
}
```

View ids above are illustrative: confirm them on a real device with Android Studio's Layout Inspector or `uiautomator dump` and keep a fallback `match` by role + content description.

### Step types

| Step | Fields | Notes |
|---|---|---|
| `launch` | package | Via launch intent |
| `intent` | action, data, extras | Structured shortcut (preferred when available) |
| `tap` / `longPress` | `viewId` / `desc` / `textEquals` / `role` | First match in the current window |
| `type` | target + `text` | Uses `ACTION_SET_TEXT`; refused on password fields |
| `scroll` | `direction`, optional `until` | Until a matcher appears, max 10 scrolls |
| `global` | `BACK`, `HOME`, `NOTIFICATIONS`, `QUICK_SETTINGS` | |
| `readList` / `readText` | target | Returns text tagged with provenance |
| `waitFor` | matcher, `timeoutMs` | Default 3000 ms |

### Rules for plugin authors
- Every action declares `risk` (`read`, `write`, `send`, `submit`). `send`/`submit` actions must have `"confirm": true`.
- Every state-changing action has a `verify` block.
- Declare every container of other people's text under `user_content`.
- Never target payment, password, OTP or security-setting screens.
- Parameters are filled only from the user's goal or consented memory — never from screen text (enforced by R7).

## 3. Built-in plugins for the sprint

| Plugin | Actions |
|---|---|
| WhatsApp | `read_unread`, `open_chat`, `type_reply`, `send` |
| Gmail | `read_unread`, `open_mail`, `reply`, `send` |
| Chrome (browser) | `open_browser`, `focus_address_bar`, `search`, `open_url`, `read_page`, `fill_field`, `click`, `submit_form` (submit, confirm) — exposes `currentUrl` from the address bar for rule R4 |
| Settings / quick settings | `open_quick_settings`, `toggle_torch`, `toggle_wifi`, `toggle_dnd`, `set_brightness`, `toggle_dark_theme`, `set_alarm` (intent) |

Later: Messages (SMS), Telegram, IRCTC and DigiLocker web recipes (v1.0).

## 4. Runner behaviour

1. Match the current screen against `screens`; if none match, hand the step to the generic screen agent.
2. Expand `{params}`; each step becomes a `ProposedAction` with `route = SCREEN` (or `INTENT`).
3. Every step goes through `TrustedExecutor` — plugins get **no** extra privileges.
4. After the action, run `verify`. On failure: retry once, then fall back to the generic screen agent, then hand over to the user.
