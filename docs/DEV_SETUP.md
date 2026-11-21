# DEV_SETUP.md — get Mitra building on your machine and phone

## 1. Install
- **Android Studio** (current stable release that supports Android Gradle Plugin 9).
- **JDK 17** (the one bundled with Android Studio is fine).
- Android SDK Platform **36** and the latest Platform-Tools (installed from Android Studio › SDK Manager).
- **Python 3.11+** for `eval/` and `scripts/`.
- Git.

## 2. Get the code and open it
```bash
git clone https://github.com/<you>/mitra.git
cd mitra
```
Open the folder in Android Studio and let Gradle sync. The wrapper (`gradlew`) downloads Gradle 9.8.0.

**If sync fails:** the versions in `gradle/libs.versions.toml` were chosen without a real build. Accept the versions Android Studio suggests, keep the file as the single place for versions, and write the final ones in `docs/DECISIONS.md` (task T01).

## 3. First checks (no phone needed)
```bash
./gradlew :engine-api:test :web:test :trust:test :plugins:test   # pure Kotlin, must always pass
./gradlew spotlessApply                                           # format
./gradlew assembleDirectDebug assembleFossDebug                   # both flavours
python scripts/roadmap_status.py --next                           # what to build next
```

## 4. Phone setup (real phone recommended)
1. Settings › About phone › tap **Build number** 7 times, then Developer options › turn on **USB debugging**.
2. Connect by USB, accept the prompt, check `adb devices`.
3. `./gradlew installDirectDebug`.
4. Turn on Mitra's accessibility service: Settings › Accessibility › Mitra. On **Android 13+** the switch may be greyed out for sideloaded apps: App info › ⋮ › **Allow restricted settings**, then try again.
5. Grant microphone and notifications when asked.

Use a real phone with at least 6 GB RAM for the on-device model. An emulator is fine for UI work and the pure Kotlin tests, but not for judging model speed or the accessibility service on real apps.

## 5. Useful commands
```bash
adb logcat | grep -i mitra          # logs (never log screen text; see AGENTS.md)
adb shell am force-stop com.mitra.app
adb uninstall com.mitra.app
```
The `foss` build has the id `com.mitra.app.foss`.

## 6. Things that are intentionally not in git
Signing keys (`*.jks`, `keystore.properties`), `google-services.json`, model files (`*.litertlm`, `*.tflite`), `local.properties`. See `.gitignore`.

## 7. Where to go next
`AGENTS.md` (rules) → `roadmap/README.md` (next task) → the task's spec in `docs/`.
