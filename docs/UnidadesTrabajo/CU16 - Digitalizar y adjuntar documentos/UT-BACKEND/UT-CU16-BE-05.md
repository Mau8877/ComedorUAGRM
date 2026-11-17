# UT-CU16-BE-05 — Controller de documentos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-05` |
| **Nombre** | Controller de documentos |
| **Caso de uso** | CU16-BE |
| **Depende de** | `UT-CU16-BE-04` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Las pantallas web y móvil necesitan los endpoints de los documentos de la postulación.

## Objetivo

Existen los cinco endpoints de `/api/v1/postulaciones/{codigo}/documentos` con el contrato de la ficha.

## Alcance

- `B/features/postulaciones/controller/DocumentosPostulacionController.java`: `GET`, `POST` y `PUT` (multipart: `tipoDocumento` y `archivo`), `DELETE` y `GET .../{documento}/acceso`. Delgado: solo delega en el servicio y envuelve en `ApiResponse`; el postulante sale de `@CurrentUserId`.
- Tests: `T/features/postulaciones/controller/DocumentosPostulacionControllerTest.java` (service simulado): códigos HTTP, forma del sobre, que no salga la ruta de almacenamiento, archivo ausente (`ERR_SYS_05`) y `401`.

Archivos esperados (crear/tocar): el controller y su test.

## Fuera de alcance (no tocar)

- Lógica de negocio y acceso a repositorios.
- Un test de integración con base real (queda como follow-up).

## Resultado esperado

Un cliente puede subir, listar, reemplazar, eliminar y abrir documentos con el sobre estándar.

## Validación

1. `./mvnw test -Dtest=DocumentosPostulacionControllerTest` pasa (necesita base de datos; sin ella el contexto de Spring no arranca).
2. `./mvnw checkstyle:check` sin errores.
3. Una subida real contra MinIO deja el objeto en `postulaciones/{codigo}/documentos/` y la fila en `documento_adjunto`.
