# UT-CU08-BE-03 — Códigos de error, bitácora y DTOs

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-03` |
| **Nombre** | Códigos de error, bitácora y DTOs |
| **Caso de uso** | CU08-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (documentar cada código en `docs/errors/` y su índice **en el mismo cambio**), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) y [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md).
- Como referencia: `ResultadosErrorCodes`, los DTOs de `features/resultados/dto/` y `BitacoraAcciones`.

## Solicitud de negocio

Cada error del acta tiene que tener un código catalogado, y la generación y la descarga tienen que poder anotarse en la bitácora.

## Objetivo

Existen los códigos de error `ACT`, las acciones de bitácora y los DTOs del módulo.

## Alcance

- `B/features/actas/exception/ActasErrorCodes.java` (interfaz de constantes, como `ResultadosErrorCodes`): `ERR_ACT_01` (`409`, los resultados aún no están publicados), `ERR_ACT_02` (`409`, la convocatoria ya tiene acta), `ERR_ACT_03` (`404`, la convocatoria todavía no tiene acta) y `ERR_ACT_04` (`404`, el archivo del acta no está en el almacenamiento).
- `B/features/bitacora/constant/BitacoraAcciones.java`: `GENERACION_ACTA` y `DESCARGA_ACTA`, con el comentario de que el detalle lleva códigos, número y cantidades, nunca nombres, CI, rutas ni direcciones firmadas.
- `docs/errors/actas/ERRORES_ACTAS.md` con una sección por código (formato de `GUIA_ERRORES_BACKEND.md`: HTTP, excepción, significado, dónde se lanza) y los cuatro links en `docs/errors/README.md` bajo «Actas de adjudicación (`ACT`)». «Dónde se lanza» se completa al implementar las UT-07 y UT-08.
- `B/features/actas/dto/`: `ActaResponse`, `ConvocatoriaPendienteActaResponse`, `AccesoActaResponse` y `ConvocatoriaActaResponse` (`codigo`, `nombre`), todos `record`, con los campos del README. `ActaResponse` no lleva la ruta del archivo.
- Tests: los códigos tienen el formato `ERR_ACT_NN`; `ActaResponse` no expone la ruta (comprobado por reflexión sobre sus componentes); cada código está en `docs/errors/` y en su índice.

Archivos esperados (crear/tocar): `ActasErrorCodes`, los cuatro DTOs, `BitacoraAcciones`, `docs/errors/actas/ERRORES_ACTAS.md` y `docs/errors/README.md`.

## Fuera de alcance (no tocar)

- Lanzar las excepciones (UT-07 y UT-08).
- Códigos de otros módulos.
- Anotar en la bitácora (UT-07 y UT-08).

## Resultado esperado

Los códigos, las acciones de bitácora y los DTOs están listos y documentados para las UT siguientes.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Cada código aparece en `docs/errors/actas/ERRORES_ACTAS.md` y enlazado desde el índice.
