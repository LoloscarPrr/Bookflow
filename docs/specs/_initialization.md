# TLC Initialization Snapshot

## Repository
- Repository: `LoloscarPrr/Bookflow`
- Base ref: `main`
- Working ref: `feature/ai-native-foundation`
- Latest relevant local commit: `46e824e` — alpha12 verification; remote branch contains the equivalent alpha12 implementation and evidence.

## App / build state
- App version: `0.2.0-alpha13`.
- Android versionCode: `17`.
- Application ID: `app.bookflow.reader`.
- Minimum Android: API 26.

## Product context
- Source: `docs/BOOKFLOW_BLUEPRINT.md`.
- Focus: stabilize the reading/narration foundation, keep neural narration local, and add diagnostics/adaptive presentation without changing the narration pipeline.

## Relevant specs
- `BF-CORE-001 — Adoptar desarrollo guiado por specs — LOCKED`.
- `BF-CORE-002 — Multiple layout adaptable — LOCKED`.
- `BF-RELEASE-001 — Diagnóstico con Firebase Crashlytics — LOCKED`.

## Relevant implementation surface
- `MainActivity.kt` Compose shell, Biblioteca and Lector.
- Gradle root/app and Android CI workflow.
- Offline Sherpa-ONNX/Piper renderer and MediaPlayer error paths.
- Persistence/migration: none expected.

## Baseline
- Source inspection: PASS.
- Local static/unit checks: UNAVAILABLE; this environment has Java but no Gradle/Android SDK.
- Local Android build: UNAVAILABLE; verification will run in GitHub Actions.
- Last branch CI: PASS — GitHub Actions run #44 without Firebase configuration.
- Physical compact/regular/wide rendering: UNAVAILABLE.

## Constraints / uncertainties
- A valid Firebase configuration for project `bookflow-ae680` and package `app.bookflow.reader` was supplied for local verification. It remains ignored and outside Git history.
- GitHub secret `FIREBASE_GOOGLE_SERVICES_JSON_BASE64` is configured; its value remains encrypted and outside Git history.
- CI must remain buildable without Firebase configuration.
- Dashboard delivery and physical layout behavior require later real-device evidence.

## Next TLC action
- Verify `BF-RELEASE-001` with the supplied Firebase configuration, package the Firebase-enabled APK, and retain device/dashboard receipt as a separate physical-device check.
