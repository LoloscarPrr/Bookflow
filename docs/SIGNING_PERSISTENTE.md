# BookFlow — Firma Android persistente (alpha18)

## Objetivo

Una sola identidad criptográfica para APK de Android futuros. La clave se generó de manera independiente de los certificados Android Debug efímeros y **no corresponde a la firma de instalaciones anteriores**. No se puede actualizar una instalación firmada con otro certificado sin conocer su clave privada.

- applicationId: `app.bookflow.reader`
- Primera versión firmada con esta clave: `0.2.0-alpha18` (versionCode 22)
- Tipo de almacén: PKCS12, clave RSA 3072, alias `bookflow`
- Huella pública SHA-256: `19541d2616517c7c7cc7e0e68f9175154eb33e0f61be55935ed4ff60a88ec1c4`
- SHA-256 del almacén cifrado después de descifrar: `7004077b0028e551509c8813c9e06b17c5b634d42a2566b4197a9f01e724ceec`

## Dónde está la clave

El repositorio público contiene **solo** `signing/bookflow-signing.jks.enc.b64`, un almacén cifrado con AES-256-CBC, PBKDF2 (300000 iteraciones, salt aleatorio), y codificado base64.

**Nunca** subir `*.jks` en claro ni la contraseña a GitHub.

El propietario debe guardar fuera del repositorio:
- Respaldo privado `BookFlow_firma_permanente.jks`.
- Contraseña privada (única para desencriptar el archivo versionado y abrir el almacén).

Si la contraseña se pierde, la clave archivada no puede recuperarse de GitHub. Si la clave o la contraseña se filtran, se compromete la identidad de las versiones futuras.

## Activación desde teléfono en GitHub

1. Abrir `https://github.com/LoloscarPrr/Bookflow/settings/secrets/actions` en navegador con sesión iniciada (si hace falta, activar vista de escritorio).
2. Agregar **Repository secret** llamado exactamente `BOOKFLOW_SIGNING_PASSWORD`, valor: la línea completa del archivo secreto privado recibido por el propietario; sin espacios adicionales.
3. Guardar el secreto. Volver a **Actions** y volver a ejecutar el workflow `Build BookFlow APK` de `audit/phase-1-1-safe-foundation`.

El workflow falla intencionalmente si el secreto falta o la contraseña es incorrecta. Descifra el almacén solo durante CI, aplica firma persistente y verifica huella antes de publicar el artefacto.

## Pruebas de aceptación

- `Restore persistent BookFlow signing key` termina verde.
- `Build persistently signed debug APK` termina verde.
- `Validate APK package and signature` confirma la huella canónica, el package e imprime SHA256 de APK.
- Se obtiene una APK alpha18, versionCode 22, instalada desde cero (o actualizada solo desde APK firmada con esta misma clave).

**Importante:** puede requerirse una migración manual de datos para pasar desde alpha16 o alpha17 con otra clave. No desinstalar ni borrar los datos sin respaldar los libros y posiciones de lectura.

## Alcance de seguridad

La firma persistente garantiza continuidad para builds futuras de esta identidad. No garantiza que Xiaomi instale el paquete: si una instalación limpia falla, obtener el código de error `INSTALL_FAILED_...` del gestor de paquetes y comprobar el entorno.

Las builds locales sin el flag `BOOKFLOW_REQUIRE_PERSISTENT_SIGNING` aún pueden usar la clave debug del desarrollador para pruebas; **no** distribuirlas como actualizaciones. GitHub CI exige la firma canónica.
