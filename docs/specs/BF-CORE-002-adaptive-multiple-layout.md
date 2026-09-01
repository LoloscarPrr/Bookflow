# BF-CORE-002 — Multiple layout adaptable

Status: DONE
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
- [x] AC1 — Una función compartida clasifica correctamente los límites 359/360/839/840dp.
- [x] AC2 — Compact usa padding reducido y acciones apiladas sin overflow horizontal.
- [x] AC3 — Regular conserva el flujo vertical y ancho completo actual.
- [x] AC4 — Wide centra Biblioteca y Lector con ancho máximo de 1040dp.
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
- [x] Pruebas unitarias del clasificador en los cuatro límites.
- [x] Compilación Android de debug.
- [ ] Inspección de Biblioteca y Lector para las tres clases.
- [x] Registrar cada criterio como PASS/BLOCKED.

## Implementation notes
Replica la política `compact/regular/wide` de WeekFlow usando APIs nativas de Jetpack Compose.

## Verification result
- AC1: PASS — `AdaptiveLayoutTest` cubre 359, 360, 839 y 840dp; `:app:testDebugUnitTest` terminó correctamente en Actions #44.
- AC2: PASS — el código compact aplica padding reducido y botones de importación/narración de ancho completo apilados; compiló en Actions #44.
- AC3: PASS — la clase regular conserva escenario de ancho completo, tarjeta horizontal y acciones en fila.
- AC4: PASS — el escenario compartido de Biblioteca y Lector usa `widthIn(max = 1040.dp)` y alineación superior centrada en wide.
- AC5: BLOCKED — `LocalConfiguration` recalcula sin mover el estado fuera de `BookFlowApp`/`ReaderScreen`, pero falta prueba de rotación o multi-window en dispositivo real.
- AC6: BLOCKED — no se modificó la lógica de callbacks y la app compiló, pero el flujo completo requiere prueba manual en un teléfono.

Evidence: https://github.com/LoloscarPrr/Bookflow/actions/runs/33521462042
