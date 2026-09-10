# BF-NARRATION-002 — Narración continua y pausas naturales

Status: DONE
Owner: BookFlow

## Problem
Alpha14 genera el siguiente tramo sólo después de terminar el actual. En un uso móvil prolongado esto deja silencios de carga entre tramos. Además, la voz local separa muy poco las oraciones y el porcentaje permanece sin cambios mientras suena un tramo largo.

## Desired behavior
BookFlow prepara audio por adelantado mientras reproduce, enlaza los tramos disponibles automáticamente, respeta mejor los finales de oración y actualiza el progreso visible y persistido durante el audio.

## Scope
- Mantener una cola de hasta tres tramos preparados por delante del tramo activo.
- Empezar a preparar el siguiente tramo apenas comienza la reproducción actual.
- Reanudar automáticamente si el reproductor alcanza temporalmente la cola mientras termina una generación.
- Aplicar una escala de silencio natural entre oraciones en el motor Piper.
- Calcular el offset aproximado dentro del tramo usando posición y duración de Media3.
- Persistir ese offset al pausar, detener o destruir el servicio.
- Publicar como `0.2.0-alpha15`.

## Affected modules
- `narration/NarrationPlaybackService.kt`.
- `narration/NarrationPlaybackState.kt` y sus pruebas.
- `core/data/OfflineNeuralVoiceRenderer.kt`.
- Metadatos de versión y Blueprint.

## Non-goals
- No cambiar el modelo de voz ni descargar voces adicionales.
- No añadir música o mezcla ambiental.
- No garantizar reproducción sin esperas en teléfonos cuya generación sea sostenidamente más lenta que la voz; la cola debe reducirlas y recuperarse sin intervención.
- No implementar todavía biblioteca multilibro ni control de velocidad.

## Acceptance criteria
- [ ] AC1 — Al comenzar un tramo, el servicio prepara en segundo plano hasta tres tramos posteriores.
- [ ] AC2 — Media3 reproduce consecutivamente los elementos ya preparados sin detenerse para iniciar una generación.
- [ ] AC3 — Si la reproducción alcanza una cola vacía, el siguiente tramo generado reanuda automáticamente.
- [ ] AC4 — Piper usa una escala de silencio mayor que alpha14 para separar oraciones con naturalidad.
- [ ] AC5 — El porcentaje puede avanzar dentro del tramo activo sin esperar a que termine completo.
- [ ] AC6 — Pausar, detener, retroceder y recrear el servicio conservan un offset seguro.
- [ ] AC7 — Voz local, caché, segundo plano, Firebase opcional y PDF/DOCX/TXT siguen compilando.
- [ ] AC8 — La cola no registra texto, título ni URI en Crashlytics.

## Data / persistence impact
Se conserva el mismo almacenamiento privado por hash del URI. Puede guardarse un offset más preciso dentro del tramo actual; no hay migración ni datos nuevos en la nube.

## UI / UX impact
El porcentaje avanza durante la locución. El estado informa cuando BookFlow está preparando audio por adelantado sin bloquear los controles del tramo que ya suena.

## Edge cases / regressions
- Generación más lenta que reproducción.
- Pausa o detención mientras hay precarga activa.
- Cambio de libro durante una generación.
- Reanudación desde un offset intermedio.
- Último tramo y documentos muy cortos.
- Error de render de un tramo futuro.

## Verification plan
- Pruebas unitarias del offset proporcional y segmentación consecutiva.
- Tests y `assembleDebug` en GitHub Actions con Firebase activo.
- Inspección del APK, versión y artefacto.
- Continuidad y pausas percibidas: BLOCKED hasta prueba física de alpha15.

## Verification results
- [x] AC1 — PASS: `ensureQueue` mantiene `PREFETCH_AHEAD_COUNT = 3` mientras el tramo activo se reproduce.
- [x] AC2 — PASS: los audios se agregan a una única playlist Media3 mediante `addMediaItem`; no se reemplaza el tramo activo.
- [x] AC3 — PASS: `addPreparedItem` detecta `STATE_ENDED`, avanza al nuevo elemento, prepara y reanuda automáticamente.
- [x] AC4 — PASS: Piper usa `silenceScale = 1.0f` y el perfil nuevo invalida la caché con pausas comprimidas.
- [x] AC5 — PASS: actualización cada 750 ms y prueba unitaria `playbackOffsetAdvancesInsideCurrentChunk`.
- [x] AC6 — PASS: pausa, detención y destrucción persisten `currentPlaybackOffset`; retroceso conserva su prueba de regresión.
- [x] AC7 — PASS: tests, Firebase, voz, `assembleDebug` y artefacto pasaron en GitHub Actions run #51.
- [x] AC8 — PASS: la cola sólo reporta área y excepción saneada; no adjunta texto, título ni URI.

Verificación física de continuidad sin espera y duración percibida de las pausas: **BLOCKED** hasta instalar y probar alpha15 en el teléfono del usuario.
