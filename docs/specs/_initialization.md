# TLC Initialization Snapshot

## Repository
- Repository: `LoloscarPrr/Bookflow`
- Base ref: `main`
- Working ref: `feature/ai-native-foundation`
- Latest relevant commit: `84bd9a39d6d9c50c94355f5d20d5b7564d2ef228` — offline neural narration.

## App / build state
- App version: `0.2.0-alpha11`.
- Android versionCode: `15`.
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
- Local static/unit checks: NOT RUN; no Gradle/Kotlin toolchain is installed in this workspace.
- Local Android build: UNAVAILABLE; no Android SDK.
- Last branch CI: PASS — GitHub Actions run #43 at commit `84bd9a3`.
- Physical compact/regular/wide rendering: UNAVAILABLE.

## Constraints / uncertainties
- No Firebase project or `google-services.json` exists for BookFlow.
- CI must remain buildable without Firebase configuration.
- Dashboard delivery and physical layout behavior require later real-device evidence.

## Next TLC action
- Implement `BF-CORE-001`, then `BF-CORE-002` and `BF-RELEASE-001` in the same user-requested delivery while preserving their independent acceptance criteria.
