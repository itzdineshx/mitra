# v0.2 — Trust

**Phase 1 · App MVP** · Release channel: Internal · Sprint day 2

**Status:** ✅ Done (19/19 steps)

## Goal

Every action passes the deterministic trust engine. Risky steps ask the user; secrets and payments are never touched; everything is logged.

## Done when (exit criteria)

- [x] All rule tests in `docs/TRUST_ENGINE_SPEC.md` §6 pass.
- [x] No engine mutating call can happen without a `Decision` (proved by a test).
- [x] Voice "yes" confirmation and `BiometricPrompt` work on the phone.
- [x] Every step appears in the encrypted audit log.

## Read before starting

- docs/TRUST_ENGINE_SPEC.md (all)
- AGENTS.md §3

## Tasks

> Tick a step `[x]` when it is merged and tested. Tick the task only when its **Accept** line is proven (test or recorded manual check).

### [x] T08 · Trust engine core (P0 · 🏃 sprint)

- **Depends on:** T02
- **Where:** `modules/trust/`
- **Spec:** docs/TRUST_ENGINE_SPEC.md §1–4
- **Accept:** Every rule has a firing and a non-firing test; coverage ≥ 60%.

- [x] `TaskScope`, `ProposedAction`, `DeviceContext`, `Decision` data classes
- [x] Risk re-derivation + `risk_keywords_en.json`, `denylist_packages.json`
- [x] Rules R1–R14 + scope expiry
- [x] Tests for every §6 case
- [x] A non-firing test for every rule
- [x] Coverage report (add Kover; record it in DECISIONS.md) ≥ 60%, aim 90%

### [x] T60 · Database, settings and model storage (P0)

- **Depends on:** T01
- **Where:** `modules/data/`
- **Spec:** docs/DATA_SPEC.md
- **Accept:** All DATA_SPEC §10 tests pass; after Delete all no database, plugin or settings file remains; no secure-field text reaches `steps` (test).

- [x] Decide Q-3 (SQLCipher vs alternative); record it in DECISIONS.md and check its licence for F-Droid
- [x] Room entities and DAOs for the tables in DATA_SPEC §3; schema export
- [x] Encrypted open with a Keystore-wrapped key; key-loss recovery (`.bad` file and notice)
- [x] `SettingsStore` (DataStore) with the keys in DATA_SPEC §2
- [x] Redaction rules: 40-character truncation + SHA-256, never secure text (DATA_SPEC §4)
- [x] 90-day and 100 MB prune; delete one task; `DataEraser` (Delete all)
- [x] `ModelFiles`: free-space check, resumable `.part` download, SHA-256 verify, atomic move
- [x] `allowBackup=false` + `dataExtractionRules`; migration test helper; the tests in DATA_SPEC §10

### [x] T09 · TrustedExecutor + Confirmer + AuditLog (P0 · 🏃 sprint)

- **Depends on:** T08, T04, T60
- **Where:** `modules/trust/TrustedExecutor.kt`, `modules/data/`, `app/`
- **Spec:** docs/TRUST_ENGINE_SPEC.md §5, §7
- **Accept:** Audit log shows verdict + rule for every step; no mutating engine call without a `Decision`.

- [x] `TrustedExecutor` with re-read of the target before acting
- [x] Executor tests (yes/no, biometric fail, screen changed, stop)
- [x] `Confirmer` in the app: voice yes/no + `BiometricPrompt` with device-credential fallback
- [x] `AuditLog` in Room (encrypted, see DECISIONS Q-3)
- [x] Test: secure text is never written to the log

## Notes

- **T08**: All 41 trust rules in TRUST_ENGINE_SPEC verified with firing and non-firing tests. Line coverage reached 90.46% (JaCoCo). Recorded D-022.
- **T60**: SQLCipher (BSD 3-clause) integrated with Room 2.8.5. Android Keystore AES-256-GCM wrapping passphrase in noBackupFilesDir. Full redaction, 90-day pruning, DataEraser, and DATA_SPEC §10 tests passing. Recorded D-023.
- **T09**: AndroidConfirmer with voice and BiometricPrompt + fallback tested and verified on real device (LAVA LXX503). RoomAuditLog encrypts and logs all actions, and no mutating engine calls execute without Decision. Evidence recorded in `docs/evidence/v0.2/T09_adb_evidence.txt`.
