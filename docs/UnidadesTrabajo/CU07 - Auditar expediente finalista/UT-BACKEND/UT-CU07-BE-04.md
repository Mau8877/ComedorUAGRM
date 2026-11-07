# UT-CU07-BE-04 — Códigos de error, acción de bitácora, DTOs y eventos de aviso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-04` |
| **Nombre** | Códigos de error, acción de bitácora, DTOs y eventos de aviso |
| **Caso de uso** | CU07-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md).
- `B/features/notificaciones/enums/EventoNotificacion.java` como referencia de eventos.

## Solicitud de negocio

Los errores del CU deben estar catalogados antes de lanzarse, y el aviso al postulante necesita sus plantillas.

## Objetivo

Existen los códigos de error `EXP`, la acción de bitácora, los DTOs y los tres eventos de aviso.

## Alcance

- `B/features/expedientes/exception/ExpedienteErrorCodes.java` (prefijo `EXP`): `ERR_EXP_01` la entrega física todavía no está registrada (409); `ERR_EXP_02` la postulación ya no está «calificada» (409); `ERR_EXP_03` las observaciones son obligatorias si el resultado es «Observado» o «No válido» (400); `ERR_EXP_04` el puntaje académico es obligatorio si el resultado es «Validado» o no está entre 0 y 30 (400); `ERR_EXP_05` el documento no existe o no pertenece al finalista (404). El finalista inexistente reutiliza `ERR_FIN_01`; los campos vacíos o de más de 500 caracteres caen en `ERR_SYS_01`.
- `docs/errors/expedientes/ERRORES_EXPEDIENTES.md` con las cinco secciones y sus links en `docs/errors/README.md` (sección «Expedientes (EXP)»).
- `BitacoraAcciones.AUDITORIA_EXPEDIENTE`.
- DTOs (`record`) en `B/features/expedientes/dto/`: `RegistrarAuditoriaRequest` (`resultado` obligatorio, `observaciones` con `@Size(max = 500)`, `puntajeAcademico` `BigDecimal`), `AuditoriaResponse`, `ResultadoAuditoriaResponse` (la auditoría y si se creó o ya existía), `ExpedienteResponse` y sus partes (postulante, convocatoria, respuesta socioeconómica, documentos, puntajes, entrega, auditorías) y `AccesoDocumentoResponse` (`url`, `expiraEn`). Nunca ids internos ni rutas de MinIO.
- Tres eventos en `EventoNotificacion`: `AUDITORIA_VALIDADA` (`SUCCESS`), `AUDITORIA_OBSERVADA` (`WARNING`, lleva `{observaciones}`) y `AUDITORIA_NO_VALIDA` (`ERROR`, lleva `{observaciones}`), los tres con `{nombres}`, solo bandeja y push (sin correo).

Archivos esperados (crear): el catálogo, la doc de errores y los DTOs. (Tocar): `BitacoraAcciones`, `EventoNotificacion`, `docs/errors/README.md`.

## Fuera de alcance (no tocar)

- La lógica que lanza los errores (UT-06, 07, 09).
- Cambiar el significado de códigos ya existentes de `FIN` o `NOT`.

## Resultado esperado

Los códigos, DTOs y plantillas existen y compilan.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Los cinco códigos aparecen en `docs/errors/README.md` con su ancla.
