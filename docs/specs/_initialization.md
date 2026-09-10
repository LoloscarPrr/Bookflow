# TLC Initialization Snapshot

## Repository
- Repository: `LoloscarPrr/Bookflow`
- Base ref: `main`
- Working ref: `feature/ai-native-foundation`
- Latest relevant remote commit: `346f2a07f1987c12ecb1edbaff19b36d2ae6d74c` — alpha13 Firebase verification evidence.

## App / build state
- Target app version: `0.2.0-alpha14`.
- Target Android versionCode: `18`.
- Application ID: `app.bookflow.reader`.
- Minimum Android: API 26.

## Product context
- Source: `docs/BOOKFLOW_BLUEPRINT.md`.
- Focus: stabilize the reading/narration foundation, keep neural narration local, and add diagnostics/adaptive presentation without changing the narration pipeline.

## Relevant specs
- `BF-CORE-001 — Adoptar desarrollo guiado por specs — LOCKED`.
- `BF-CORE-002 — Multiple layout adaptable — LOCKED`.
- `BF-RELEASE-001 — Diagnóstico con Firebase Crashlytics — LOCKED`.
- `BF-NARRATION-001 — Reproducción en segundo plano y progreso persistente — IMPLEMENTING`.

## Relevant implementation surface
- `MainActivity.kt` Compose shell, Biblioteca and Lector.
- Nuevo `MediaSessionService`, control Media3 y estado compartido de reproducción.
- Persistencia privada del último libro y offset; carga PDF/DOCX/TXT desde URI.
- Manifest, Gradle app y renderer Sherpa-ONNX/Piper existente.

## Baseline
- Source inspection: PASS.
- Local static/unit checks: UNAVAILABLE; this environment has Java but no Gradle/Android SDK.
- GitHub Actions unit tests and build: PASS — run #45 with Firebase configuration.
- Last branch CI: PASS — GitHub Actions run #44 without Firebase configuration.
- Physical compact/regular/wide rendering: UNAVAILABLE.

## Constraints / uncertainties
- A valid Firebase configuration for project `bookflow-ae680` and package `app.bookflow.reader` was supplied for local verification. It remains ignored and outside Git history.
- GitHub secret `FIREBASE_GOOGLE_SERVICES_JSON_BASE64` is configured; its value remains encrypted and outside Git history.
- CI must remain buildable without Firebase configuration.
- Dashboard delivery and physical layout behavior require later real-device evidence.

## Next TLC action
- Implementar y verificar `BF-NARRATION-001`, manteniendo la prueba física de pantalla apagada/notificación como evidencia pendiente.
