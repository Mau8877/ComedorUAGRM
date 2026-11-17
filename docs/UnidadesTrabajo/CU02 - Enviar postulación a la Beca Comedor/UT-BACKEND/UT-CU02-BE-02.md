# UT-CU02-BE-02 — `EnvioPostulacionService` y `EnvioPostulacionController`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-02` |
| **Nombre** | `EnvioPostulacionService` y `EnvioPostulacionController` |
| **Caso de uso** | CU02-BE |
| **Depende de** | UT-CU02-BE-01 |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)

## Solicitud de negocio

El postulante tiene que poder iniciar su postulación y enviarla con todas las validaciones del CU, sin duplicar el envío si repite la petición.

## Objetivo

`POST /api/v1/postulaciones` y `POST /api/v1/postulaciones/{codigo}/enviar` funcionan según el CU.

## Alcance

- `postulaciones/service/EnvioPostulacionService.java`: `iniciar` (valida con el CU18, crea el borrador o devuelve el existente/la devuelta) y `enviar` (bloquea la fila, idempotente si ya está `pendiente`/`calificada`, valida con el CU18, exige campos y documentos obligatorios, valida rangos y opciones, guarda las respuestas, `Postulacion.enviar()`, historial y pedido de precalificación tras confirmar la transacción).
- `Postulacion.enviar(Instant)` y `Postulacion.calificar()` en el Model.
- `postulaciones/controller/EnvioPostulacionController.java`: `201` al crear el borrador, `200` si ya había uno y `200` con el comprobante al enviar.
- `EnvioPostulacionServiceTest` (Mockito).

Archivos esperados (crear/tocar): `EnvioPostulacionService.java`, `EnvioPostulacionController.java`, `Postulacion.java`, `EnvioPostulacionServiceTest.java`.

## Fuera de alcance (no tocar)

- Guardar un borrador parcial (`PUT …/respuesta`): es el CU17.
- Anotar el envío en la bitácora (decisión 5 del README).
- La precalificación en sí (`UT-CU02-BE-03`): aquí solo se la pide.

## Resultado esperado

El envío completo funciona de punta a punta, con cada rechazo catalogado.

## Validación

1. `./mvnw test -Dtest=EnvioPostulacionServiceTest` en verde.
2. Con el stack levantado: iniciar → adjuntar documentos → enviar devuelve `pendiente`; repetir el envío devuelve el mismo comprobante; enviar con un campo vacío da `400` `ERR_ENV_04`.
