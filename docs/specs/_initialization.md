# TLC Initialization Snapshot

## Repository
- Repository: `LoloscarPrr/Bookflow`
- Base ref: `main`
- Working ref: `feature/ai-native-foundation`
- Latest relevant remote commit: `29a0174cf7ae08c7e87da88507faafec61447db5` — alpha15 continuous narration verification.

## App / build state
- Target app version: `0.2.0-alpha16`.
- Target Android versionCode: `20`.
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
- `BF-NARRATION-003 — Voces españolas de alta calidad — LOCKED`.

## Relevant implementation surface
- `MainActivity.kt` Compose shell, Biblioteca and Lector.
- `MediaSessionService`, cola anticipada de audio Media3 y estado compartido de reproducción.
- Persistencia privada del último libro y offset; carga PDF/DOCX/TXT desde URI.
- Manifest, Gradle app y renderer Sherpa-ONNX/Piper existente.

## Baseline
- Source inspection: PASS.
- Local static/unit checks: UNAVAILABLE; this environment has Java but no Gradle/Android SDK.
- GitHub Actions unit tests and build: PASS — run #50, alpha14 con Firebase configurado.
- GitHub Actions alpha16: PASS — run #53, dos voces high, tests, Firebase y APK de 262,384,860 bytes.
- Physical alpha14 playback: PASS para voz, pausa y reanudación según video del usuario; progreso intra-tramo: FAIL; continuidad entre tramos y pausas naturales: FAIL según evidencia del usuario.
- Physical compact/regular/wide rendering: UNAVAILABLE.

## Constraints / uncertainties
- A valid Firebase configuration for project `bookflow-ae680` and package `app.bookflow.reader` was supplied for local verification. It remains ignored and outside Git history.
- GitHub secret `FIREBASE_GOOGLE_SERVICES_JSON_BASE64` is configured; its value remains encrypted and outside Git history.
- CI must remain buildable without Firebase configuration.
- Dashboard delivery and physical layout behavior require later real-device evidence.

## Next TLC action
- Instalar alpha16 y verificar físicamente naturalidad, tildes y continuidad sostenida.
