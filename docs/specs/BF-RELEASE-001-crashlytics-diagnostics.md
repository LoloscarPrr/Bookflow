# BF-RELEASE-001 — Diagnóstico con Firebase Crashlytics

Status: LOCKED
Owner: BookFlow

## Problem
Los fallos observados en teléfonos reales hoy dependen de capturas o descripciones manuales. BookFlow además ejecuta una librería neural nativa, por lo que necesita evidencia tanto de errores Kotlin como de cierres NDK y ANR.

## Desired behavior
BookFlow queda integrado con Firebase Crashlytics y Crashlytics NDK cuando existe una configuración Firebase válida. Si aún no existe, el mismo código y CI compilan en modo diagnóstico inactivo, sin cerrar la app ni simular que los reportes fueron enviados.

## Scope
- In scope:
  - Integrar Firebase BoM, Crashlytics y Crashlytics NDK con versiones oficiales compatibles.
  - Aplicar plugins Firebase solo cuando existe `app/google-services.json`.
  - Añadir un reporter central que inicializa de forma segura y registra errores no fatales importantes.
  - Preparar CI para reconstruir `google-services.json` desde un secreto opcional en base64.
  - Registrar versión y área técnica, nunca contenido del libro ni identidad del lector.
- Affected modules:
  - Gradle raíz/app, workflow Android, arranque, importación, PDF y narración.

## Non-goals
- No crear ni aceptar términos de un proyecto Firebase en nombre del usuario.
- No añadir Analytics, anuncios ni seguimiento de lectura.
- No incluir un botón de crash deliberado en el APK entregado.
- No afirmar recepción en el panel hasta probarla con configuración y dispositivo reales.

## Acceptance criteria
- [ ] AC1 — Gradle usa Google Services 4.5.0, Crashlytics plugin 3.0.8 y Firebase BoM 34.18.0.
- [ ] AC2 — El APK compila con y sin `google-services.json`; sin configuración el reporter queda inactivo de forma segura.
- [ ] AC3 — Con configuración válida se habilita captura automática de cierres, ANR y fallos NDK.
- [ ] AC4 — Errores capturados de importación, extracción PDF, narración y reproducción se registran como no fatales.
- [ ] AC5 — Los reportes no adjuntan texto/título del libro, URI, nombre de usuario ni otro contenido personal.
- [ ] AC6 — El workflow acepta el secreto opcional `FIREBASE_GOOGLE_SERVICES_JSON_BASE64` sin imprimirlo.
- [ ] AC7 — No se entrega ninguna acción visible que provoque un crash intencional.

## Data / persistence impact
No cambia datos locales. Cuando Firebase esté conectado, Crashlytics enviará diagnósticos técnicos por Internet según su configuración.

## UI / UX impact
No hay controles nuevos. Los estados de error actuales permanecen visibles para el usuario.

## Edge cases / regressions
- Configuración Firebase ausente, vacía o inválida.
- Excepción durante inicialización del SDK.
- Fallo capturado antes de inicializar Firebase.
- Errores de la voz nativa y de MediaPlayer.
- Build de CI sin secretos.

## Verification plan
- [ ] Compilar en CI sin secreto Firebase.
- [ ] Inspeccionar que no exista botón/test crash.
- [ ] Verificar llamadas de errores no fatales y sanitización de metadata.
- [ ] Probar recepción en dashboard cuando exista configuración Firebase real.
- [ ] Registrar cada criterio como PASS/BLOCKED.

## Implementation notes
Referencia oficial: https://firebase.google.com/docs/crashlytics/android/get-started y https://firebase.google.com/docs/crashlytics/android/get-started-ndk.

## Verification result
- AC1: PENDING
- AC2: PENDING
- AC3: PENDING
- AC4: PENDING
- AC5: PENDING
- AC6: PENDING
- AC7: PENDING
