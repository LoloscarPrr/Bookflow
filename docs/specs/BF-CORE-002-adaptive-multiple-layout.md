# BF-CORE-002 — Multiple layout adaptable

Status: LOCKED
Owner: BookFlow

## Problem
La interfaz actual fue construida para un único ancho de teléfono. En equipos compactos los controles de narración pueden apretarse y, en tablets o pantallas grandes, el contenido se estira sin un límite de lectura cómodo.

## Desired behavior
BookFlow comparte una política de layout con tres clases observables —compact, regular y wide— y adapta el contenedor, espaciados y controles sin duplicar lógica de lectura o narración.

## Scope
- In scope:
  - Clasificar el ancho actual como compact (`<360dp`), regular (`360–839dp`) o wide (`>=840dp`).
  - Centrar y limitar el escenario principal a `1040dp` en pantallas wide.
  - Reducir padding y apilar acciones de narración en compact para evitar recortes.
  - Mantener ancho completo y comportamiento actual en teléfonos regular.
  - Aplicar la política a Biblioteca y Lector.
- Affected modules:
  - `MainActivity.kt` y componentes de presentación adaptativa.

## Non-goals
- No rediseña la identidad visual ni cambia textos funcionales.
- No cambia importación, extracción, caché, voz ni reproducción.
- No añade navegación lateral específica de tablet en este incremento.

## Acceptance criteria
- [ ] AC1 — Una función compartida clasifica correctamente los límites 359/360/839/840dp.
- [ ] AC2 — Compact usa padding reducido y acciones apiladas sin overflow horizontal.
- [ ] AC3 — Regular conserva el flujo vertical y ancho completo actual.
- [ ] AC4 — Wide centra Biblioteca y Lector con ancho máximo de 1040dp.
- [ ] AC5 — Cambiar tamaño/orientación recalcula el layout sin reiniciar libro, progreso ni reproducción.
- [ ] AC6 — Importar, abrir, narrar, detener, retroceder y volver mantienen su comportamiento.

## Data / persistence impact
None.

## UI / UX impact
Solo cambia distribución, ancho máximo y espaciado exterior según pantalla. Se conservan jerarquía, colores, acciones y accesibilidad existente.

## Edge cases / regressions
- Teléfonos de 320–359dp.
- Rotación entre regular y wide.
- Títulos largos en la barra superior.
- Botones Narrar/Detener/Atrás visibles simultáneamente.
- PDF, TXT y DOCX deben conservar el mismo flujo.

## Verification plan
- [ ] Pruebas unitarias del clasificador en los cuatro límites.
- [ ] Compilación Android de debug.
- [ ] Inspección de Biblioteca y Lector para las tres clases.
- [ ] Registrar cada criterio como PASS/BLOCKED.

## Implementation notes
Replica la política `compact/regular/wide` de WeekFlow usando APIs nativas de Jetpack Compose.

## Verification result
- AC1: PENDING
- AC2: PENDING
- AC3: PENDING
- AC4: PENDING
- AC5: PENDING
- AC6: PENDING
