# UT-CU16-BE-04 — `DocumentosPostulacionService`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-04` |
| **Nombre** | `DocumentosPostulacionService` |
| **Caso de uso** | CU16-BE |
| **Depende de** | `UT-CU16-BE-01`, `UT-CU16-BE-02`, `UT-CU16-BE-03` |

## Antes de implementar

- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- El CU16 completo (flujos A1–A10 y CA01–CA15)

## Solicitud de negocio

El postulante necesita adjuntar sus documentos de respaldo y que el sistema no deje archivos huérfanos ni filas sin archivo.

## Objetivo

`DocumentosPostulacionService` lista, adjunta, reemplaza, elimina y da acceso temporal a los documentos de una postulación.

## Alcance

- `B/features/postulaciones/service/DocumentosPostulacionService.java`: `@PreAuthorize` con el rol `POSTULANTE`; `listar`, `adjuntar`, `reemplazar`, `eliminar` y `acceso`. Orden de comprobaciones de `adjuntar`: postulación del usuario (`ERR_DOC_04`) → estado borrador o devuelta (`ERR_DOC_06`) → convocatoria abierta y en fechas (`ERR_DOC_07`) → tipo en la lista (`ERR_DOC_05`) → archivo (`ERR_DOC_01`/`02`/`03`) → bloqueo de la fila y máximo por tipo (`ERR_DOC_08`) → subir y registrar. Reemplazar y eliminar retiran el archivo anterior tras confirmar la transacción.
- Tests: `T/features/postulaciones/service/DocumentosPostulacionServiceTest.java` (Mockito, reloj fijo): un caso por CA aplicable, incluidos los fallos del almacenamiento y del registro.

Archivos esperados (crear/tocar): el servicio y su test.

## Fuera de alcance (no tocar)

- Endpoints (`UT-CU16-BE-05`).
- Crear borradores o enviar postulaciones (CU17, CU02).
- Bitácora y notificaciones.

## Resultado esperado

Todo lo del CU16 que no es HTTP ni pantalla está resuelto y probado con el almacenamiento simulado.

## Validación

1. `./mvnw test -Dtest=DocumentosPostulacionServiceTest` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Ningún log ni mensaje de error lleva nombres de archivo ni rutas.
