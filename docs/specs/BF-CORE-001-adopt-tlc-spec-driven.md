# BF-CORE-001 — Adoptar desarrollo guiado por specs

Status: DONE
Owner: BookFlow

## Problem
BookFlow ha evolucionado mediante cambios conversacionales y builds sucesivos. Sin una especificación local, una sesión nueva puede perder decisiones vigentes, ampliar el alcance sin querer o declarar completo un cambio solo porque compila.

## Desired behavior
El repositorio incluye una skill TLC propia de BookFlow y exige inicialización, alcance bloqueado y verificación por criterios de aceptación antes de modificar producto, arquitectura, integraciones o release.

## Scope
- In scope:
  - Añadir `.agents/skills/tlc-spec-driven/SKILL.md` adaptada a BookFlow.
  - Añadir instrucciones raíz en `AGENTS.md`.
  - Añadir registro, plantilla y snapshot de inicialización en `docs/specs/`.
  - Usar identificadores `BF-<AREA>-NNN`.
- Affected modules:
  - Flujo de desarrollo y documentación del repositorio.

## Non-goals
- No cambia el comportamiento de la aplicación.
- No añade dependencias ni servicios en tiempo de ejecución.
- No reescribe el Blueprint Maestro.

## Acceptance criteria
- [x] AC1 — La skill posee frontmatter válido, define Initialize → Think → Lock → Code → Verify y su Definition of Done.
- [x] AC2 — `AGENTS.md` exige usar la skill antes de cambios relevantes.
- [x] AC3 — `docs/specs/README.md`, `_template.md` y `_initialization.md` existen y usan convenciones BookFlow.
- [x] AC4 — La skill distingue PASS, FAIL, NOT RUN, UNAVAILABLE y BLOCKED sin convertir ausencia de evidencia en éxito.
- [x] AC5 — La adopción no modifica por sí sola código ni dependencias de la app.

## Data / persistence impact
None.

## UI / UX impact
None.

## Edge cases / regressions
- Los changelogs y el Blueprint siguen siendo evidencia/intención; no sustituyen una spec activa.
- Una spec no autoriza cambios externos adicionales fuera del pedido del usuario.

## Verification plan
- [x] Validar la skill con `quick_validate.py`.
- [x] Revisar que todos los archivos requeridos existan.
- [x] Registrar cada criterio como PASS/BLOCKED.

## Implementation notes
La skill de WeekFlow se usa como referencia, pero nombres, fuentes de verdad y áreas se adaptan a BookFlow.

## Verification result
- AC1: PASS — `quick_validate.py` confirmó `Skill is valid!` y la skill contiene las cinco fases y Definition of Done.
- AC2: PASS — `AGENTS.md` referencia la skill y exige snapshot, spec y verificación.
- AC3: PASS — registro, plantilla y snapshot existen con prefijo `BF` y áreas de BookFlow.
- AC4: PASS — los estados de evidencia y la prohibición de asumir éxito están explícitos.
- AC5: PASS — esta adopción solo añadió `.agents`, `AGENTS.md` y `docs/specs`.
