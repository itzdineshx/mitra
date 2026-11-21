# Built-in plugins

One folder per app: `<name>/plugin.json` + `<name>/plugin.sig`. Schema: `docs/PLUGIN_SPEC.md`.

Sprint plugins: `settings` (started), `whatsapp`, `gmail`, `chrome` — tasks T14–T17.

## Signing (development)

```bash
pip install cryptography
python scripts/sign_plugins.py --new-key      # once: writes keys/dev_ed25519.pem (git-ignored) and prints the public key
python scripts/sign_plugins.py                # signs every plugins-builtin/*/plugin.json
```

Paste the printed public key into `PluginKeys.kt` in `modules/plugins`.
**Never commit private keys.** The release key lives offline, not in this repo.
