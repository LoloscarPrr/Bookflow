# BookFlow — Product & Architecture Blueprint

## Product thesis
BookFlow is an AI-native Android reading and narration platform. Its differentiator is not basic TTS: the system understands a passage before it narrates it, then coordinates expressive speech and adaptive ambience around that understanding.

## Three equal pillars

### 1. Reading
- Import PDF, DOCX and TXT.
- Reliable local parsing and rendering.
- Persistent library and reading position.
- Text extraction/indexing suitable for narration and AI comprehension.
- Later: EPUB, highlights, notes, search and accessibility controls.

### 2. AI Comprehension / Book Brain
The AI layer acts as a director as well as a reading assistant.

Core domain output: `NarrationPlan`.

A plan describes a bounded passage using provider-neutral data:
- detected narrator / speaker
- stable character identity
- mood
- emotional intensity
- speaking pace
- pause/breath hints
- scene/ambience class
- music intensity
- transition points
- spoiler-safe context boundary

The domain layer never depends directly on a specific AI vendor. Provider adapters implement interfaces defined by the domain.

### 3. Narration
Narration consumes a `NarrationPlan`; it must not guess scene semantics independently.

Target experience:
- expressive neural-quality speech
- persistent voice casting per narrator/character
- natural pauses and pacing
- adaptive music/ambience rather than one generic loop
- speech-first mixing and automatic music ducking
- smooth scene transitions
- background playback
- cached generated segments
- replay without regenerating audio

## Architecture
Clean Architecture remains mandatory.

- `presentation`: Compose UI and state
- `domain`: entities, use cases and provider-neutral interfaces
- `data`: document storage, parsing, cache and provider implementations
- `reader`: reading/rendering subsystem
- `ai`: scene comprehension and NarrationPlan generation
- `narration`: speech generation, voice casting and playback
- `audio`: ambience selection, mixing, ducking and transitions

Dependency direction: outer layers depend inward. Domain contains no Android, network SDK or provider-specific types.

## Narration pipeline

`Document -> Extracted passage -> Book Brain -> NarrationPlan -> Voice renderer -> Audio mixer -> Media3 playback -> Cache`

A passage is generated ahead of playback so the next segment can be prepared while the current one plays.

## Revised roadmap

### Phase A — Reading foundation (current)
- [x] Android project and CI APK
- [x] PDF import/rendering
- [x] TXT reading
- [x] DOCX basic text extraction
- [ ] persistent multi-book library
- [ ] reading position
- [ ] scalable PDF rendering (lazy pages instead of rendering the whole document at once)
- [ ] robust structured text extraction for AI/narration

### Phase B — Narration Lab (next priority)
Goal: prove that BookFlow can produce a short passage whose narration feels deliberately directed rather than like Android TTS.

- [ ] `NarrationPlan` domain model
- [ ] `SceneDirector` / AI provider interface
- [ ] `VoiceRenderer` provider interface
- [ ] `VoiceCast` model for stable narrator/character voices
- [ ] narration segment/cache model
- [ ] Narration Lab screen: select a passage, inspect the plan, generate and play
- [ ] Media3 playback foundation
- [ ] background playback via MediaSessionService

Acceptance gate: a short passage has natural voice delivery and the same character keeps the same assigned voice across segments.

### Phase C — Adaptive soundtrack
- [ ] ambience taxonomy (calm, mystery, melancholy, tension, action, etc.)
- [ ] local soundtrack/ambience asset catalog with licensing metadata
- [ ] scene-to-ambience mapping from NarrationPlan
- [ ] speech-first volume ducking
- [ ] fades and scene transitions
- [ ] separate speech and ambience controls

Acceptance gate: soundtrack supports the narration without masking speech or sounding like an unrelated looping track.

### Phase D — Book Brain reader integration
- [ ] spoiler boundary tied to reading progress
- [ ] ask about current passage / characters / concepts
- [ ] summaries and study assistance
- [ ] scene understanding shared with narration rather than duplicated
- [ ] character memory and aliases

### Phase E — Production narration
- [ ] pre-generation queue
- [ ] resilient retries
- [ ] local audio cache and eviction
- [ ] offline replay of generated segments
- [ ] chapter-level playback queue
- [ ] lock-screen/headset controls
- [ ] performance and battery profiling

### Phase F — Polish
- [ ] redesigned library
- [ ] reader themes/typography
- [ ] player UI
- [ ] narration settings
- [ ] voice casting UI
- [ ] accessibility
- [ ] instrumentation and crash diagnostics

## Technical media direction
Use AndroidX Media3 for playback/session infrastructure. Keep speech generation and semantic directing behind interfaces so they can evolve independently. Mixing/transition implementation must remain replaceable because advanced crossfades and dynamic narration needs may exceed one high-level Media3 API.

## Non-goals
- Do not make Android's basic system TTS the final quality target.
- Do not hard-code an AI or speech vendor into domain models.
- Do not regenerate already-cached narration unnecessarily.
- Do not let music selection operate independently of scene understanding.
- Do not sacrifice reading reliability for narration features.

## Immediate implementation target
BookFlow `0.2 Narration Lab`: establish the domain contracts and a test UI before choosing/finalizing a production neural voice provider. This lets providers be evaluated against the same passage and NarrationPlan without rewriting the app.
