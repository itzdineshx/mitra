<p align="center"><img src="brand/png/mark/transparent/lotus-color.png" alt="Mitra logo" width="120"></p>

<h1 align="center">Mitra</h1>

**Mitra** is an open-source Android voice agent that operates your phone for you: apps, settings and websites in your own browser. It runs an on-device model (Gemma 4 E2B on LiteRT-LM), and a deterministic **trust engine** checks every step before it happens.

> Final-year B.Tech (AI & DS) project by **DINESH S**. Full plan: [`docs/MitraOS_PRD_v3.0.pdf`](docs/MitraOS_PRD_v3.0.pdf) · Architecture: [`docs/architecture.png`](docs/architecture.png)

English first (`en-IN`); Hindi and Tamil follow in v1.0.

---

## Quick start

**You need:** Android Studio (latest stable), JDK 17, an Android 10+ phone with USB debugging.

```bash
unzip MitraOS_StarterPack.zip && cd mitra
```

1. Open the `mitra` folder in Android Studio and let it sync. If it offers newer AGP or library versions, accept them; all versions are in `gradle/libs.versions.toml`.
2. Install SDK Platform 36 when Studio asks.
3. Format once and run the trust tests. They need no phone:
   ```bash
   ./gradlew spotlessApply :trust:test
   ```
4. Install on your phone: `./gradlew installDirectDebug`
5. On the phone: **Settings → Accessibility → Mitra → On**. On Android 13+, if the switch is grey, open **App info → ⋮ → Allow restricted settings** first.

Then put it in git and push to GitHub:

```bash
git init -b main
git add .
git commit -m "chore: initial project skeleton"
git remote add origin https://github.com/<you>/mitra.git
git push -u origin main
```

## How the project is organised

It is **one app**. Everything below builds into a single APK.

```
mitra/
├── roadmap/          WHAT to build — one folder per version, each with a task checklist
├── docs/             HOW it must work — specs, decisions, PRD, UI spec + approved screen designs
├── brand/            logo (lotus), app icons, colours — PNG and SVG
├── AGENTS.md         rules for AI agents and humans (start here)
├── app/              the installed app (screens, wiring, flavours direct | foss)
├── modules/          parts of the same app, kept separate so they are easy to test
│   ├── trust/        safety checks R1–R14 + TrustedExecutor   (tested)
│   ├── web/          safe-website checks                     (tested)
│   ├── plugins/      signed app recipes                      (tested)
│   ├── engine-api/   screen types + engine interface         (tested)
│   ├── engine-access/  taps, types, reads the screen (Accessibility Service) + Stop button
│   ├── engine-guide/   fallback: speaks steps, the user taps
│   ├── agents/  model/  voice/  data/  ui/
├── plugins-builtin/  built-in recipes (settings started)
├── eval/             benchmark tasks + attack suite (Python)
├── ml/               fine-tuning and model export (Python)
└── scripts/          roadmap_status.py, sign_plugins.py
```

## Roadmap and progress

Open **[roadmap/README.md](roadmap/README.md)**: a status board for v0.1 → v2.0, with one folder per version and a checklist for every task (T01–T60).

```bash
python scripts/roadmap_status.py --next   # which task to do now
python scripts/roadmap_status.py          # refresh the board after ticking boxes
```

## Common commands

| What | Command |
|---|---|
| All unit tests | `./gradlew test` |
| Trust-engine tests | `./gradlew :trust:test` |
| Build both flavours | `./gradlew assembleDirectDebug assembleFossDebug` |
| Install on phone | `./gradlew installDirectDebug` |
| Format code | `./gradlew spotlessApply` |
| Check eval files | `cd eval && python run.py --list` |

## Two builds, no Play Store

- `direct`: GitHub releases, website, Indus Appstore.
- `foss`: F-Droid. No Firebase and no Google Play Services; CI fails if any slip in.

Release signing keys **never** go in this repo (`.gitignore` blocks `*.jks`, `keys/`, `google-services.json`).

## Building with AI agents

Point your agent at `AGENTS.md` first; its §0 says exactly how to start a session. The agent picks a task from `roadmap/`, follows the specs in `docs/`, and must never break the safety rules (for example, every action goes through `TrustedExecutor`).

## License

Apache-2.0. See [LICENSE](LICENSE).
