# BookFlow

BookFlow is an AI-native Android reading experience built around three equal product pillars:

1. **Reading** — import and read books cleanly on Android.
2. **Narration** — turn reading into an expressive audio experience with an on-device neural voice that needs no credits.
3. **AI Comprehension** — a spoiler-aware book brain that understands the current reading context and powers questions, study, characters and contextual audio decisions.

## Status

Alpha 12 adds compact, regular and wide layouts plus privacy-safe Firebase Crashlytics diagnostics. Spanish neural narration continues to run locally with Sherpa-ONNX + Piper, supports male/female speakers and caches generated WAV segments without narration credits.

## Tech direction

- Kotlin
- Jetpack Compose
- Coroutines / Flow
- Room
- Media3
- Modular clean architecture
- Local-first book parsing and indexing
- Pluggable AI providers; no provider secrets committed to the repository

## Build

The neural runtime and pinned Spanish model are reproducibly downloaded from official Sherpa-ONNX releases:

```bash
bash scripts/fetch-offline-tts.sh
gradle :app:assembleDebug
```

See `docs/BOOKFLOW_BLUEPRINT.md` once the architecture branch lands.

Crashlytics is activated only when a valid Firebase configuration is supplied. See `docs/CRASHLYTICS_SETUP.md`.
