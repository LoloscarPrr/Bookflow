# BF-NARRATION-003 — Voces españolas de alta calidad

Status: LOCKED
Owner: BookFlow

## Problem
La reproducción continua de alpha15 funciona, pero el modelo multihablante de calidad media suena artificial y pronuncia mal palabras españolas con tilde.

## Desired behavior
BookFlow narra totalmente sin conexión con dos voces Piper de alta calidad y un solo hablante: Miro para narrador y Daniela para narradora. El texto Unicode llega al sintetizador sin perder tildes ni signos y la cola anticipada de alpha15 permanece intacta.

## Scope
- Sustituir `sharvard-medium` por `es_ES-miro-high` y `es_AR-daniela-high`.
- Seleccionar un motor independiente según el reparto de narrador o narradora.
- Verificar cada archivo descargado por SHA-256 y conservar su `MODEL_CARD`.
- Separar la caché por modelo y publicar `0.2.0-alpha16`.

## Non-goals
- Reducir el tamaño del APK.
- Usar servicios, créditos o conexión a Internet durante la narración.
- Cambiar la segmentación, precarga o reproducción Media3 de alpha15.
- Prometer entonación humana perfecta; la validación perceptiva corresponde al dispositivo real.

## Acceptance criteria
- [ ] AC1 — Narrador usa Miro high y narradora usa Daniela high; ambos generan con `sid = 0`.
- [ ] AC2 — La preparación del texto sólo normaliza espacios y conserva caracteres Unicode, tildes y puntuación.
- [ ] AC3 — La clave de caché incluye el modelo para impedir cruces entre voces o versiones.
- [ ] AC4 — CI valida los SHA-256 oficiales antes de extraer ambos modelos.
- [ ] AC5 — Los `MODEL_CARD` se conservan dentro del APK y los avisos identifican ambos modelos.
- [ ] AC6 — Tests y `assembleDebug` pasan con Firebase configurado, sin exponer texto del libro.
- [ ] AC7 — La cola anticipada y la persistencia de progreso de alpha15 no se modifican.

## Verification plan
- Prueba unitaria de selección estable Miro/Daniela.
- Inspección estática de preservación Unicode, claves de caché y `sid = 0`.
- Tests y `assembleDebug` en GitHub Actions.
- Instalación y escucha de palabras con tilde en el teléfono del usuario.

