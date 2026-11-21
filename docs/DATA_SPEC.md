# DATA_SPEC.md — database, storage, retention and deletion

> Owner of the code: `modules/data`. Task: **T60** (roadmap v0.2). Open library choice: **Q-3** in `DECISIONS.md` (default SQLCipher for Room).
> Rule of rules: **everything stays on the phone, encrypted where it can hold personal data, and the user can see it and delete it.**

## 1. Principles

1. **On-device only.** No analytics, no crash upload, no sync. The only network uses are the model download, optional cloud help (per task, with consent) and family approval in the `direct` build.
2. **Store the least.** The audit log keeps what is needed to explain a step, not what was on the screen. Never store passwords, OTPs, secure-field text, card or UPI numbers, raw audio, screenshots or full message bodies.
3. **Encrypted at rest.** The database is encrypted; the key lives in the Android Keystore.
4. **Visible and deletable.** History shows what is stored. A task can be deleted; "Delete all" removes everything.
5. **One door.** Only `modules/data` touches Room, DataStore and the model files. Other modules use the interfaces in §7.

## 2. Storage map

| What | Where | Encrypted | Kept | Removed by "Delete all" |
|---|---|---|---|---|
| Tasks, steps (audit log), rules, permissions, memory, plugins, family links, model runs | Room database `mitra.db` in `databases/` | Yes (SQLCipher) | tasks and steps 90 days; the rest until the user removes it | Yes |
| Settings (language, "Hey Mitra", speak replies, Wi-Fi only, cloud off/on, first-run step) | Jetpack DataStore (Preferences) `settings` | No (no secrets in it) | until changed | Yes (reset to defaults) |
| On-device AI model (Gemma 4 E2B, 2.58 GB) and the optional ~1 GB model | `filesDir/models/` | No (public model) | until removed | Only if the user also ticks "Remove the model" |
| Speech and wake-word models (about 122 MB) | `filesDir/models/speech/` | No | until removed | Same as the model |
| Partial downloads | `cacheDir/downloads/*.part` | No | until finished or cleared | Yes |
| User-installed plugins (signed JSON) | `filesDir/plugins/` | No (signed, public) | until removed | Yes |
| Built-in plugins, public keys, prompts, risk lists | inside the APK (`assets`, `res/raw`, `resources/`) | n/a | n/a | n/a |
| Database key | Android Keystore (AES key wraps a random 32-byte passphrase stored in `noBackupFilesDir/`) | Yes | until deleted | Yes (key and wrapped passphrase) |
| Logs | none stored; Logcat is redacted (AGENTS.md §3) | n/a | n/a | n/a |

**Backup:** `allowBackup=false` and `dataExtractionRules` exclude everything (cloud backup and device-to-device transfer). Reason: the database key cannot move to another phone, and backups would copy personal history off the device. A restored phone starts clean.

## 3. Database schema (Room entities)

Times are epoch milliseconds (`INTEGER`). Ids are UUID strings unless noted. Add an index on every foreign key.

### `tasks`
| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | |
| `created_at`, `ended_at` | INTEGER, INTEGER NULL | |
| `user_text` | TEXT | what the user said (transcript); never audio |
| `language` | TEXT | `en-IN` now |
| `source` | TEXT | `ORB`, `WAKE_WORD`, `ASSISTANT`, `TILE`, `STANDING_RULE` |
| `status` | TEXT | `RUNNING`, `WAITING`, `DONE`, `STOPPED`, `BLOCKED`, `FAILED` |
| `summary` | TEXT | the result sentence shown in History and Done ("Sent to Priya") |
| `scope_json` | TEXT | `TaskScope` (allowed apps and domains for this task) |
| `engine` | TEXT | `ACCESSIBILITY` or `GUIDE` |
| `step_count` | INTEGER | |
Indexes: `created_at`, `status`.

### `steps` (the audit log; spec in `TRUST_ENGINE_SPEC.md` §7)
| Column | Type | Notes |
|---|---|---|
| `step_id` | TEXT PK | the `auditId` from `Decision` |
| `task_id` | TEXT FK → tasks (cascade delete) | |
| `idx` | INTEGER | order inside the task |
| `agent`, `engine`, `route` | TEXT | |
| `action_json` | TEXT | action type, target role/id, **redacted or truncated text** (§4) |
| `verdict`, `rule_id`, `reason` | TEXT | ALLOW / CONFIRM / … ; rule such as `R7`; short reason |
| `user_response` | TEXT NULL | `YES`, `NO`, `CONTINUE`, `TIMEOUT`, or null; feeds "Asked you · you said yes" in History |
| `result` | TEXT NULL | `OK` or a `FailureReason` |
| `screenshot_hash` | TEXT NULL | SHA-256 only |
| `duration_ms` | INTEGER | |
| `created_at` | INTEGER | |
Indexes: `task_id`, `created_at`.

### `permissions` (the user's allow-list; the banking deny-list is code, not data)
`id` INTEGER PK · `kind` (`APP`, `DOMAIN`, `ASK_BEFORE`) · `value` (package, domain pattern or action name) · `mode` (`ALLOW`, `BLOCK`, `ALWAYS_ASK`) · `locked` (1 = user cannot change it, e.g. Sending = Always ask) · `created_at`. Unique on (`kind`, `value`). The app refuses to insert a package that is in `denylist_packages.json` (test).

### `rules` (standing rules, T19)
`id` · `kind` (`AUTO_REPLY`) · `description` · `target_app` · `config_json` · `enabled` · `created_at` · `pin_confirmed_at` · `last_fired_at`. Turning a rule on needs the phone PIN or biometric (CONFIRM_STRONG).

### `memory` (only with consent, T46)
`key` TEXT PK · `value` TEXT · `source` (`USER_SAID` or `SCREEN`; screen-sourced values stay marked untrusted) · `source_task_id` NULL · `consent_at` · `created_at` · `updated_at`. The memory agent asks before saving anything new.

### `plugins`
`id` TEXT PK · `version` · `signer_fingerprint` · `sha256` · `source` (`BUILTIN`, `FILE`) · `enabled` · `installed_at`.

### `family_links` (T23)
`id` · `display_name` · `channel` (`FCM` in `direct`, `UNIFIEDPUSH` or `SAME_PHONE` in `foss`) · `endpoint_hash` · `status` (`LINKED`, `UNLINKED`) · `linked_at` · `last_seen_at`. A push token, if one is needed, is stored encrypted in this table and never logged.

### `model_runs` (speed tests)
`id` INTEGER PK · `ts` · `model_id` · `first_token_ms` · `tokens_per_s` · `peak_ram_mb` · `device_tier` (`A`, `B`, `C`). The On-device AI card (UI_SPEC 6.5) shows the **latest row**, never typed-in numbers.

### `send_events` (rate limit for rule R10)
`ts` · `task_id` · `kind`. Rows older than 24 h are deleted. Used for `MAX_SENDS_PER_HOUR`.

## 4. What is stored for sensitive steps

| Situation | Stored | Not stored |
|---|---|---|
| Typing text into a field | field role, target id, text length, first 40 characters **only if the field is not secure**, SHA-256 of the text | the full text; anything from a secure field |
| Sending a message (CONFIRM) | recipient label, app, first 40 characters of the text, SHA-256, the user's response | the full message |
| Password / OTP / CAPTCHA / payment (USER_ONLY) | the verdict, rule and "handed to user" | any text; the engine receives none |
| Screen read | element count, package, URL host (web), provenance counts | element text, screenshots |
| Blocked injection (R7) | rule id, a 60-character excerpt of the offending text with links and numbers masked | the full page or message |
| Voice | the transcript the user spoke (`tasks.user_text`) | audio |

A later setting "Don't save what I say" may blank `user_text`; until then the transcript is stored because History needs it.

## 5. Encryption and keys

- **Key:** on first run generate a random 32-byte passphrase, wrap it with an AES-256-GCM key in the Android Keystore (alias `mitra_db_key`, non-exportable), store the wrapped blob in `noBackupFilesDir/`. SQLCipher opens the DB with the unwrapped passphrase.
- **Locked phone:** the key does not require the screen to be unlocked, because a background task or the wake word may need the database. Decide with Q-7 if this is acceptable.
- **Key lost or unreadable** (factory reset restore, Keystore reset, corrupted file): detect the open failure, rename the file to `mitra.db.bad`, create a new database, show one notice in History ("Your old history could not be opened and was reset"). Never try to recover remotely. Delete `.bad` after 7 days.
- **Settings and model files** are not secrets and are not encrypted.

## 6. Retention, pruning and deletion

- **Prune:** on app start and after every task, delete `tasks` (and their `steps`) older than 90 days, then delete oldest tasks while the database is larger than 100 MB.
- **Delete one task:** swipe in History; cascades to its steps.
- **Delete all** (Privacy screen, asks twice), in this order:
  1. Stop any running task and the wake-word service; turn cloud help off.
  2. Close the database; delete `mitra.db`, `-wal`, `-shm`, `.bad`.
  3. Delete the key blob and the Keystore alias.
  4. Delete `filesDir/plugins/`, `cacheDir/downloads/`, and the DataStore file.
  5. If the user ticked **Remove the model**, delete `filesDir/models/`.
  6. Show "All Mitra data was deleted" and restart the app process to the first-run flow.
- **Test (T21):** after Delete all, listing the app's private directories finds no `.db`, no plugin and no settings files, and a new database has zero rows.

## 7. Interfaces (in `modules/data`; implementations use Room and DataStore)

`TaskStore`, `AuditLog` (implements the interface `TrustedExecutor` already uses), `PermissionStore`, `RuleStore`, `MemoryStore`, `PluginStore`, `FamilyStore`, `ModelRunStore`, `SettingsStore`, `ModelFiles` (download, verify, delete) and `DataEraser` (the Delete-all sequence above). Methods that the UI watches return `Flow`; writes are `suspend`. Each interface has an in-memory fake for tests (the existing `InMemoryStore` is the first one).

## 8. Model files (download and storage)

- Check free space first: need the file size + 10%. If not enough: "Mitra needs 3 GB free. Free some space and try again."
- Download over Wi-Fi only by default, to `cacheDir/downloads/<name>.part`, with HTTP range resume. Verify SHA-256 (value is pinned in `modules/model`, not read from the server). On success move atomically to `filesDir/models/`. On a mismatch delete the file and retry once, then show **Try again**.
- Never bundle the model in the APK. Record the licence terms of the model version used (Q-6).
- Budget: model 2.58 GB + speech 122 MB + optional small model 1 GB. Database expected under 50 MB; hard cap 100 MB (§6).

## 9. Which screen reads what

| Screen | Reads | Writes |
|---|---|---|
| Home (Recent) | last 3 `tasks` | task on start (via TaskRunner) |
| History | `tasks` + `steps` | delete task |
| Setup readiness ring | live system permission state, model file state, `settings` | none |
| Permissions | live system state | none |
| On-device AI | latest `model_runs`, model file state, `settings` | `model_runs` on speed test, `settings` |
| Safety rules (not drawn) | `permissions`, `rules` | same |
| Privacy (not drawn) | `family_links`, `settings` | Delete all, unlink |
| Assistant panel | current task (in memory) | `steps` through `TrustedExecutor` |

## 10. Migrations and tests

- Room `exportSchema = true`; commit the JSON to `modules/data/schemas/`. Every schema change bumps the version and ships a `Migration` with a `MigrationTestHelper` test. No destructive migration of `tasks`/`steps` except through the key-loss path.
- Required tests: no secure-field text reaches `steps` (password-field fixture); text is truncated to 40 characters and hashed; 90-day prune and the 100 MB cap; delete-one cascades; Delete all (§6); key-loss recovery; banking packages cannot enter `permissions`; `model_runs` latest-row query.
- Run on the JVM with Robolectric where possible; the Keystore parts need an instrumented test on a device or emulator.

## 11. Open questions

- **Q-3** SQLCipher vs a Jetpack Security-based approach (default SQLCipher). SQLCipher for Android is a third-party licensed library: check its licence is compatible with F-Droid before choosing.
- **Q-7** May the database open while the phone is locked? Default yes (needed for the wake word); revisit when T45 is built.
