# TLC Initialization Snapshot

## Repository
- Repository: `LoloscarPrr/Bookflow`
- Base ref: `main`
- Working ref: `feature/ai-native-foundation`
- Latest relevant remote commit: `712f2cc04e60ff075d99c7bc50f16b8732e87512` — alpha14 background playback build.

## App / build state
- Target app version: `0.2.0-alpha15`.
- Target Android versionCode: `19`.
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
- `BF-NARRATION-002 — Narración continua y pausas naturales — LOCKED`.

## Relevant implementation surface
- `MainActivity.kt` Compose shell, Biblioteca and Lector.
- `MediaSessionService`, cola anticipada de audio Media3 y estado compartido de reproducción.
- Persistencia privada del último libro y offset; carga PDF/DOCX/TXT desde URI.
- Manifest, Gradle app y renderer Sherpa-ONNX/Piper existente.

## Baseline
- Source inspection: PASS.
- Local static/unit checks: UNAVAILABLE; this environment has Java but no Gradle/Android SDK.
- GitHub Actions unit tests and build: PASS — run #50, alpha14 con Firebase configurado.
- Physical alpha14 playback: PASS para voz, pausa y reanudación según video del usuario; progreso intra-tramo: FAIL; continuidad entre tramos y pausas naturales: FAIL según evidencia del usuario.
- Physical compact/regular/wide rendering: UNAVAILABLE.

## Constraints / uncertainties
- A valid Firebase configuration for project `bookflow-ae680` and package `app.bookflow.reader` was supplied for local verification. It remains ignored and outside Git history.
- GitHub secret `FIREBASE_GOOGLE_SERVICES_JSON_BASE64` is configured; its value remains encrypted and outside Git history.
- CI must remain buildable without Firebase configuration.
- Dashboard delivery and physical layout behavior require later real-device evidence.

## Next TLC action
- Implementar y verificar `BF-NARRATION-002`; continuidad y pausas percibidas quedan pendientes de prueba física de alpha15.
