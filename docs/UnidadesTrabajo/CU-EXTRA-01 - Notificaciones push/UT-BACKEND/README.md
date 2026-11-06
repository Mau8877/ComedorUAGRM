# Unidades de trabajo — CU-EXTRA-01-BE

**Caso de uso:** Backend del servicio de notificaciones: registro de los teléfonos (token de Firebase), bandeja de avisos, envío de push por FCM y de correo por SMTP con reintentos, plantillas por evento y un aviso de prueba. Sin eventos de negocio todavía.
**Fuente CU:** [`CU-EXTRA-01 - Notificaciones push`](../../../CasosDeUso/CU-EXTRA-01%20-%20Notificaciones%20push.md)
**Diagramas de grado:** ninguno propio (no figura entre los 26 CU). Se apoya en las tablas `NOTIFICACION` y `DISPOSITIVO_FCM` del diseño lógico (`images/091`, transcrito en [`DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid)). La carpeta de la documentación de grado no estaba disponible al redactar estas fichas: antes de la `UT-CUX01-BE-02` hay que abrir `diseno_base_de_datos.md` §4 y confirmar los valores del enum `tipo_notificacion`.
**Recurso HTTP / contrato:** `POST /api/v1/dispositivos-fcm`, `POST /api/v1/dispositivos-fcm/baja` (autenticados, cualquier rol) y `POST /api/v1/notificaciones/prueba` (solo `ADMINISTRADOR_DUBSS`).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

En estas fichas, `B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/` y `T/` = `projects/backend/comedor-uagrm-core/src/test/java/com/comedoruagrm/backend/`. El módulo nuevo es `B/features/notificaciones/`.

- **Códigos de las UT: `UT-CUX01-…`.** Este CU no es uno de los 26 de la documentación de grado, así que su número es `CU-EXTRA-01` y el código de las UT lo abrevia `CUX01` (las reglas de UT piden `CU{NN}` con dos dígitos).
- **Una fila por aviso en `notificacion`, tres vías de entrega.** La fila es la bandeja del usuario; el push y el correo son formas de entrega de esa misma fila. No hay tabla de estado de envío por canal (`notificacion_envio`): se descartó para no ampliar el diagrama; `enviada_en` se llena cuando al menos una vía se entregó.
- **`notificacion` sin `comunicado_id` ni `postulacion_id` por ahora.** El diagrama las trae, pero apuntan a tablas (`comunicado`, `postulacion`) que todavía no existen en la base. Esas dos columnas las agrega el CU que cree cada tabla (regla de las UT: no construir trabajo de otro CU). Es una diferencia consciente y temporal respecto del diagrama.
- **Reintentos en memoria, sin outbox.** 3 intentos por envío con esperas crecientes (2 s y 8 s, configurables) en un hilo del ejecutor propio. Si el backend se reinicia a mitad de un reintento, ese envío se pierde; el aviso sigue en la bandeja. Un reintento persistente queda como follow-up.
- **Envío después del commit, nunca dentro de la transacción de negocio.** `NotificacionService.notificar(...)` guarda la fila en la transacción de quien llama y publica un evento que se atiende `AFTER_COMMIT` en el ejecutor asíncrono: un rollback no deja avisos ni pushes fantasma, y un push lento no demora la operación.
- **Un solo `CanalPush`, dos implementaciones.** `CanalPushFcm` (Firebase) cuando hay credenciales; `CanalPushLog` (solo escribe en el log, sin tokens) cuando no. Los tests y el CI nunca llaman a Firebase.
- **Sin credenciales, el backend arranca igual.** Si `FIREBASE_CREDENTIALS_PATH` está vacía o el archivo no existe, no se crea el bean de Firebase y se usa `CanalPushLog` (WARN en el arranque). El archivo se llama `secrets/firebase-sa.json` y no se versiona.
- **Baja de tokens: `POST /api/v1/dispositivos-fcm/baja` con el token en el cuerpo**, y no `DELETE` por código: la app no guarda el código del registro, y un `DELETE` con cuerpo no es portable. Es idempotente: un token desconocido, ya dado de baja o de otra cuenta responde `200`.
- **Un token pertenece a un solo usuario.** Registrar un token que ya está a nombre de otra cuenta lo **reasigna** a la actual (teléfono compartido).
- **Correo de notificaciones aparte del correo de restablecimiento.** `CorreoRestablecimientoService` (CU15) no se toca ni se mueve: sus enlaces de un solo uso no pasan por la bandeja ni se guardan.
- **Aviso de prueba (`POST /notificaciones/prueba`)**: existe para comprobar de punta a punta que Firebase y el correo funcionan. Solo `ADMINISTRADOR_DUBSS`, y siempre a sí mismo. Se puede retirar cuando haya eventos reales.
- **Sin códigos de error nuevos previstos.** Los rechazos son de validación (`ERR_SYS_01`), autenticación (`ERR_AUTH_01`) y rol (`ERR_AUTH_07`), ya catalogados. Si al implementar aparece uno propio, se documenta en `docs/errors/notificaciones/` (prefijo `NOT`) en ese mismo cambio.

## Contrato de datos / recurso HTTP

Todos devuelven el sobre de [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md).

| Método | Ruta | Acceso | Cuerpo | Respuesta | Errores |
|--------|------|--------|--------|-----------|---------|
| `POST` | `/api/v1/dispositivos-fcm` | Sesión | `token` (obligatorio, máx. 512), `plataforma` (`ANDROID`) | `201` con `DispositivoResponse` (`codigo`, `plataforma`); `200` si el token ya era de este usuario | `400` `ERR_SYS_01`; `401` `ERR_AUTH_01` |
| `POST` | `/api/v1/dispositivos-fcm/baja` | Sesión | `token` (obligatorio) | `200` `data: null`, idempotente | `400` `ERR_SYS_01`; `401` `ERR_AUTH_01` |
| `POST` | `/api/v1/notificaciones/prueba` | `ADMINISTRADOR_DUBSS` | — | `200` `data: null`, el envío sigue en segundo plano | `401` `ERR_AUTH_01`; `403` `ERR_AUTH_07` |

Push que recibe el teléfono: `notification` (`title`, `body`) más `data` con `codigoNotificacion` y `evento`; canal Android `notificaciones`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CUX01-BE-01](./UT-CUX01-BE-01.md) | Docker, secretos y variables de Firebase | — |
| [UT-CUX01-BE-02](./UT-CUX01-BE-02.md) | Tablas y modelos de notificaciones y dispositivos | — |
| [UT-CUX01-BE-03](./UT-CUX01-BE-03.md) | Configuración de Firebase Admin | 01 |
| [UT-CUX01-BE-04](./UT-CUX01-BE-04.md) | Canal de push: FCM y modo log | 03 |
| [UT-CUX01-BE-05](./UT-CUX01-BE-05.md) | Servicio de dispositivos (registro, baja y limpieza de tokens) | 02 |
| [UT-CUX01-BE-06](./UT-CUX01-BE-06.md) | Endpoints de dispositivos | 05 |
| [UT-CUX01-BE-07](./UT-CUX01-BE-07.md) | Reintentos y ejecutor de notificaciones | — |
| [UT-CUX01-BE-08](./UT-CUX01-BE-08.md) | Correo de notificaciones | 07 |
| [UT-CUX01-BE-09](./UT-CUX01-BE-09.md) | Eventos y plantillas | 02 |
| [UT-CUX01-BE-10](./UT-CUX01-BE-10.md) | Servicio de notificaciones: guardar y despachar | 02, 04, 05, 07, 08, 09 |
| [UT-CUX01-BE-11](./UT-CUX01-BE-11.md) | Aviso de prueba | 10 |
| [UT-CUX01-BE-12](./UT-CUX01-BE-12.md) | Pruebas de integración con base real | 06, 11 |

**Cadena:** `01 → 03 → 04`; `02 → 05 → 06`; `02 → 09`; `07 → 08`; `02, 04, 05, 07, 08, 09 → 10 → 11`; `06, 11 → 12`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (pasos 1 a 5) | Registrar el teléfono, sin duplicar | 02, 05, 06 |
| Flujo B (pasos 6 a 8) | Dar de baja el teléfono | 05, 06 |
| Flujo C (pasos 9 a 12) | Guardar el aviso y despachar push y correo | 02, 04, 07, 08, 09, 10 |
| Flujo E (pasos 15 y 16) | Aviso de prueba | 11 |
| A2 | Token de otra cuenta se reasigna | 05 |
| A3 | Sin teléfonos: solo correo | 10 |
| A4 | Token inválido se elimina | 04, 05, 10 |
| A5, A6 | Reintentos de push y de correo | 07, 08, 10 |
| A7 | Firebase sin configurar | 01, 03, 04 |
| A8 | Baja idempotente | 05, 06 |
| A9 | `401` / `403` | 06, 11 |
| A10 | Datos faltantes en la plantilla | 09, 10 |
| CA01–CA04 | Registro, sin duplicar, renovación y baja | 05, 06, 12 |
| CA06–CA09 | Despacho a varios teléfonos, solo correo, token inválido, reintentos | 04, 07, 08, 10, 12 |
| CA10, CA16 | Arranque sin credenciales | 01, 03, 04 |
| CA13, CA14 | Aviso de prueba y `403` | 11, 12 |
| CA15 | Sin tokens en los logs | 04, 05, 10 |
| CA05, CA11, CA12 | Permiso, recepción en el teléfono | ver [UT-MOBILE](../UT-MOBILE/README.md) |

## Follow-ups (no se implementan en estas UT)

- Reintento persistente (outbox) si perder un envío en un reinicio resulta inaceptable.
- Agregar `comunicado_id` y `postulacion_id` a `notificacion` en el CU que cree esas tablas.
- Endpoints de la bandeja (`GET /notificaciones/me`, leer, leer todo, eliminar): ver [`NOTIFICACIONES_WEB.md`](../../../pendientes/NOTIFICACIONES_WEB.md); hay que alinear sus nombres de columna con el diagrama.
- Cada evento real se agrega como constante de `EventoNotificacion` en el CU que lo necesite.
- Preferencias por canal, envío masivo a todos los postulantes (lotes y límite del SMTP) y push en la web / iOS.
- Credenciales de Firebase y SMTP de producción como secretos del servidor, cuando se decida el destino de despliegue.

## Qué NO incluyen estas UT

- Las pantallas y la app móvil (ver [`UT-MOBILE`](../UT-MOBILE/README.md)).
- Ningún evento de negocio concreto ni su plantilla, aparte de `PRUEBA`.
- El correo de restablecimiento de contraseña del CU15, que sigue como está.
- La bandeja web y sus endpoints de lectura.
