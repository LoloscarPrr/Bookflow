# BookFlow

BookFlow is an AI-native Android reading experience built around three equal product pillars:

1. **Reading** — import and read books cleanly on Android.
2. **Narration** — turn reading into an expressive audio experience with an on-device neural voice that needs no credits.
3. **AI Comprehension** — a spoiler-aware book brain that understands the current reading context and powers questions, study, characters and contextual audio decisions.

## Status

Alpha 11 adds Spanish offline neural narration with Sherpa-ONNX + Piper. Narration runs locally, supports male/female speakers and caches generated WAV segments.

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
