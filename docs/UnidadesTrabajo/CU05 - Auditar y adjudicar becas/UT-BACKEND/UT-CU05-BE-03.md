# UT-CU05-BE-03 — Códigos de error, bitácora y DTOs

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-03` |
| **Nombre** | Códigos de error, bitácora y DTOs |
| **Caso de uso** | CU05-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (DTOs como `record`).
- Como referencia: `CierrePostulacionErrorCodes`, `CierrePostulacionRequest` y `CierrePostulacionResponse` (CU-20).

## Solicitud de negocio

La adjudicación necesita sus errores catalogados, sus acciones en la bitácora y su contrato con la pantalla.

## Objetivo

Quedan listos los cinco códigos de error, las acciones de bitácora y los DTOs del panel, la selección y la decisión.

## Alcance

- `B/features/postulaciones/exception/AdjudicacionErrorCodes.java` (prefijo `ADJ`, *propuesto, por revisar*), todos `409` / `ConflictException`:
  - `ERR_ADJ_01`: la postulación ya no está «calificada» o los resultados de la convocatoria ya fueron publicados.
  - `ERR_ADJ_02`: el expediente no está auditado, o la última auditoría no es «Validado» (solo al adjudicar).
  - `ERR_ADJ_03`: el puntaje total no alcanza el puntaje mínimo de la convocatoria.
  - `ERR_ADJ_04`: los cupos de la convocatoria están agotados.
  - `ERR_ADJ_05`: la convocatoria no está cerrada, o no tiene cupos o puntaje mínimo definidos.
  Convocatoria inexistente reutiliza `ERR_PRI_01`; finalista inexistente, `ERR_FIN_01`; cantidad o motivo inválido, `ERR_SYS_01`.
- **Documentar los cinco códigos** en `docs/errors/postulaciones/ERRORES_POSTULACIONES.md` (HTTP, excepción, significado, dónde se lanza) y agregar «Adjudicación (`ADJ`)» con sus cinco links en `docs/errors/README.md`, como exige `GUIA_ERRORES_BACKEND.md`.
- `BitacoraAcciones.SELECCION_FINALISTAS`, `ADJUDICACION_BECA` y `NO_ADJUDICACION_BECA` (el actor es el administrador; el detalle lleva códigos y estados, nunca el motivo, nombres, CI, rutas ni tokens).
- DTOs en `B/features/postulaciones/dto/` (todos `record`; los de la selección también los usa `features/finalistas/`):
  - `SeleccionarFinalistasRequest`: `cantidad` (`Integer`, opcional) con `@Positive` («La cantidad debe ser un número entero positivo»).
  - `SeleccionFinalistasResponse`: `convocatoria`, `seleccionados` y `finalistas` (`FinalistaSeleccionado`: `codigo`, `postulacion`, `posicion`).
  - `NoAdjudicarRequest`: `motivo` con `@NotBlank` («El motivo es obligatorio») y `@Size(max = 500)` («El motivo no puede pasar de 500 caracteres»).
  - `DecisionAdjudicacionResponse`: `finalista`, `postulacion`, `resultado`, `estado`, `puntajeFinal`, `fechaAdjudicacion`, `decididaPor` (nombre del administrador) y `cuposDisponibles`.
  - `PanelAdjudicacionResponse` con los campos del contrato del README.
- **No** se agregan eventos a `EventoNotificacion` (decisión 3 del CU: el aviso es del CU-22).
- Tests: validación de `NoAdjudicarRequest` (vacío, solo espacios, 501 caracteres, 500 exactos válidos) y de `SeleccionarFinalistasRequest` (`null` válido, `0` y negativos inválidos, `1` válido).

Archivos esperados (crear): `AdjudicacionErrorCodes`, los cinco DTOs y sus tests. (Tocar): `BitacoraAcciones`, `docs/errors/README.md` y `docs/errors/postulaciones/ERRORES_POSTULACIONES.md`.

## Fuera de alcance (no tocar)

- La lógica que lanza los errores (UT-06 y UT-07).
- Idempotencia por clave: no se usa.
- Eventos de aviso al postulante o plantillas de notificación (CU-22).
- Cambiar los códigos `ERR_ARC_01`, `ERR_DEV_*`, `ERR_EXP_*` ni `ERR_FIN_*`.

## Resultado esperado

El módulo tiene sus códigos documentados, sus acciones de bitácora y su contrato.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Cada código del `AdjudicacionErrorCodes` tiene su sección en `docs/errors/` y su link en el índice.
