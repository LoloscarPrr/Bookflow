# BF-NARRATION-001 — Reproducción en segundo plano y progreso persistente

Status: IMPLEMENTING
Owner: BookFlow

## Problem
La narración actual vive dentro de `ReaderScreen` y usa `MediaPlayer`. Al abandonar la pantalla se libera el reproductor, no existen controles del sistema y la posición vuelve a cero al reiniciar la aplicación.

## Desired behavior
BookFlow reproduce la voz neural desde un `MediaSessionService` de Media3, continúa al apagar la pantalla o usar otra aplicación, ofrece controles de reproducción del sistema y recupera el último libro y el avance de narración guardado.

## Scope
- Mover la propiedad del reproductor y la continuidad entre tramos a un servicio de narración.
- Conectar el servicio con Media3 `ExoPlayer` + `MediaSessionService`.
- Mostrar notificación multimedia automática con título y controles reproducir/pausar.
- Persistir localmente el último libro importado y el offset seguro de cada libro.
- Reemplazar el panel grande por controles compactos que reflejen el estado del servicio.
- Mantener la generación Sherpa-ONNX/Piper y su caché sin créditos.

## Affected modules
- `MainActivity.kt` y presentación del lector.
- Nuevo paquete `narration` para servicio, estado y segmentación.
- Persistencia local y carga de documentos en `core/data`.
- Manifest y dependencias Media3.

## Non-goals
- No crear todavía biblioteca de múltiples libros, capítulos ni índice de caché.
- No sincronizar progreso en la nube.
- No añadir música adaptativa ni modificar voces/modelos.
- No afirmar reproducción real con pantalla bloqueada hasta probar el APK en un teléfono.

## Acceptance criteria
- [ ] AC1 — La reproducción está alojada en un `MediaSessionService`, no en la composición Compose.
- [ ] AC2 — El sistema muestra una notificación multimedia con reproducir/pausar y metadatos del libro.
- [ ] AC3 — Al finalizar un tramo, el servicio genera y reproduce el siguiente sin depender de `ReaderScreen`.
- [ ] AC4 — El offset se guarda al pausar, detener, terminar un tramo o retroceder, y se restaura al reabrir el libro.
- [ ] AC5 — El último libro importado reaparece después de recrear la aplicación si el permiso URI sigue vigente.
- [ ] AC6 — El lector presenta controles compactos y adaptables para narrar, pausar/reanudar, detener y retroceder.
- [ ] AC7 — La voz local, la caché, Firebase opcional y los formatos PDF/DOCX/TXT siguen compilando.
- [ ] AC8 — Diagnósticos del servicio no envían texto, título ni URI del libro.

## Data / persistence impact
SharedPreferences privadas almacenan el URI persistente, título y MIME del último libro, más offsets indexados por un hash del URI. El texto del libro y el audio no se envían ni se sincronizan.

## UI / UX impact
El lector muestra una tarjeta compacta de reproducción. Los controles del sistema permanecen disponibles durante audio activo y el botón principal cambia entre Narrar, Preparando, Pausar y Reanudar.

## Edge cases / regressions
- URI cuyo permiso fue revocado.
- PDF sin texto extraíble o documento vacío.
- Proceso recreado durante un tramo: se recupera desde el inicio seguro de ese tramo.
- Error de generación o ExoPlayer.
- Pulsaciones repetidas mientras se genera audio.
- Cambio a otro libro mientras el servicio conserva una sesión anterior.

## Verification plan
- Pruebas unitarias de segmentación, retroceso y cálculo de progreso.
- Tests y `assembleDebug` en GitHub Actions con Firebase activo.
- Inspección del APK para servicio, permisos, Media3, versión y firma.
- Prueba física de pantalla apagada/notificación: BLOCKED hasta instalar alpha14.
