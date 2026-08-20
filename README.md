# BookFlow

BookFlow is an AI-native Android reading experience built around three equal product pillars:

1. **Reading** — import and read books cleanly on Android.
2. **Narration** — turn reading into an expressive audio experience with offline and neural voice paths.
3. **AI Comprehension** — a spoiler-aware book brain that understands the current reading context and powers questions, study, characters and contextual audio decisions.

## Status

Fresh rebuild. Architecture-first bootstrap in progress.

## Tech direction

- Kotlin
- Jetpack Compose
- Coroutines / Flow
- Room
- Media3
- Modular clean architecture
- Local-first book parsing and indexing
- Pluggable AI providers; no provider secrets committed to the repository

See `docs/BOOKFLOW_BLUEPRINT.md` once the architecture branch lands.
