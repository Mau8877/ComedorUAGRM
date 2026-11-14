# UT-CU06-BE-04 — Códigos de error, acción de bitácora y DTOs

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-04` |
| **Nombre** | Códigos de error, acción de bitácora y DTOs |
| **Caso de uso** | CU06-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (formato `ERR_{MODULO}_{NUMERO}` y documentación obligatoria en `docs/errors/`).
- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md).
- Como referencia: `B/features/usuarios/exception/UsuarioErrorCodes.java` y `B/features/bitacora/constant/BitacoraAcciones.java`.

## Solicitud de negocio

Cada rechazo de la lista tiene un código que el frontend y soporte pueden reconocer, y la exportación tiene su acción en la bitácora.

## Objetivo

Quedan definidos y documentados los códigos de error, la acción de bitácora y los DTOs del CU.

## Alcance

- `B/features/postulaciones/exception/PriorizacionErrorCodes.java` con `ERR_PRI_01` (404, convocatoria inexistente), `ERR_PRI_02` (400, `sort` o `filter` no soportado) y `ERR_PRI_03` (400, no hay postulaciones para exportar).
- `docs/errors/postulaciones/ERRORES_POSTULACIONES.md` (carpeta y archivo nuevos) con la sección de cada código en el formato de `GUIA_ERRORES_BACKEND.md` (HTTP, excepción, significado, dónde se lanza), y su link en `docs/errors/README.md` bajo la sección `### Priorización (PRI)`.
- `BitacoraAcciones.EXPORTACION_LISTA_PRIORIZADA`.
- DTOs (`record`) en `B/features/postulaciones/dto/`: `ConvocatoriaPriorizableResponse`, `ResumenListaPriorizadaResponse`, `PostulacionPriorizadaResponse`, `CarreraPriorizadaResponse` (`codigo`, `nombre`) y `ExportarListaRequest` (`convocatoriaId` `@NotNull`, `carreraId`, `search`), con los campos del README. Nunca llevan `id` internos.
- Test del `ExportarListaRequest` con Bean Validation (falta `convocatoriaId`).

Archivos esperados (crear): `PriorizacionErrorCodes.java`, los DTOs, `docs/errors/postulaciones/ERRORES_POSTULACIONES.md` y el test. (Tocar): `BitacoraAcciones.java` y `docs/errors/README.md`.

## Fuera de alcance (no tocar)

- Lanzar las excepciones (UT-05 a 07).
- Códigos de otros módulos.

## Resultado esperado

Los códigos, la acción de bitácora y los DTOs existen y están documentados, sin usarse todavía.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Cada código está en `docs/errors/` y en el índice del `README.md`.
