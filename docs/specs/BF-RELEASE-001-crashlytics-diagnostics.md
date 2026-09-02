# BF-RELEASE-001 — Diagnóstico con Firebase Crashlytics

Status: DONE
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
- [x] AC1 — Gradle usa Google Services 4.5.0, Crashlytics plugin 3.0.8 y Firebase BoM 34.18.0.
- [x] AC2 — El APK compila con y sin `google-services.json`; sin configuración el reporter queda inactivo de forma segura.
- [x] AC3 — Con configuración válida se habilita captura automática de cierres, ANR y fallos NDK.
- [x] AC4 — Errores capturados de importación, extracción PDF, narración y reproducción se registran como no fatales.
- [x] AC5 — Los reportes no adjuntan texto/título del libro, URI, nombre de usuario ni otro contenido personal.
- [x] AC6 — El workflow acepta el secreto opcional `FIREBASE_GOOGLE_SERVICES_JSON_BASE64` sin imprimirlo.
- [x] AC7 — No se entrega ninguna acción visible que provoque un crash intencional.

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
- [x] Compilar en CI sin secreto Firebase.
- [x] Inspeccionar que no exista botón/test crash.
- [x] Verificar llamadas de errores no fatales y sanitización de metadata.
- [ ] Probar recepción en dashboard cuando exista configuración Firebase real.
- [x] Registrar cada criterio como PASS/BLOCKED.

## Active verification scope
- Build de CI con la configuración suministrada para `bookflow-ae680` / `app.bookflow.reader`.
- Confirmar que Gradle ejecuta Google Services y genera símbolos/mapping de Crashlytics sin incluir el JSON en Git.
- No objetivo: provocar un cierre real o afirmar recepción en Firebase sin evidencia de dispositivo/panel.

## Implementation notes
Referencia oficial: https://firebase.google.com/docs/crashlytics/android/get-started y https://firebase.google.com/docs/crashlytics/android/get-started-ndk.

## Verification result
- AC1: PASS — Actions #44 resolvió y compiló las versiones fijadas de ambos plugins, el BoM y Crashlytics/NDK.
- AC2: PASS — Actions #44 compiló sin configuración y Actions #45 compiló con la configuración real de `bookflow-ae680`; ambos caminos finalizaron correctamente.
- AC3: PASS — Actions #45 ejecutó `processDebugGoogleServices`, `injectCrashlyticsMappingFileIdDebug` e `injectCrashlyticsVersionControlInfoDebug`; el APK contiene Crashlytics NDK para ARM64 y ARMv7 y los recursos del proyecto Firebase correcto.
- AC4: PASS — importación TXT/DOCX, permisos URI, apertura/extracción/render PDF, narración y MediaPlayer llaman al reporter central.
- AC5: PASS — el reporter usa áreas enumeradas y reemplaza cada excepción por tipo + stack trace, descartando el mensaje original; no recibe título, URI, texto ni identidad.
- AC6: PASS — el workflow reconstruye el JSON desde el secreto opcional y Actions #44 confirmó el camino ausente sin exponer contenido.
- AC7: PASS — no existe acción, texto ni callback de test crash en la interfaz.

Evidence: https://github.com/LoloscarPrr/Bookflow/actions/runs/33521462042 (sin Firebase); https://github.com/LoloscarPrr/Bookflow/actions/runs/33657220470 (Firebase activo); APK alpha13 SHA-256 `8729066dc135e875f12a1b1047985dc7402d63213559a6b3673ba4a2613eedc6`.

La recepción visible de un evento en el dashboard permanece como comprobación de dispositivo: este cierre no afirma que se haya provocado un fallo deliberado.
