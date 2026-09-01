# BookFlow Specs

This directory connects the BookFlow Blueprint to implementation.

Every relevant change follows `.agents/skills/tlc-spec-driven/SKILL.md`:

`Initialize → Think → Lock → Code → Verify`

Specs move through `DRAFT → LOCKED → IMPLEMENTING → VERIFYING → DONE`.

## Naming

Use `BF-<AREA>-NNN-short-title.md`.

- `CORE` — shared architecture, state and presentation.
- `READ` — import, documents and reader.
- `NARRATION` — directing, voices, playback and cache.
- `BRAIN` — comprehension and book assistant.
- `AUDIO` — ambience, mixing and transitions.
- `RELEASE` — diagnostics, packaging, signing and delivery.

A changelog records what shipped; a spec defines what must be built and how completion is verified.
