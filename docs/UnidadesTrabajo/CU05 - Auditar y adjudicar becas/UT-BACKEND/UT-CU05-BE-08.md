# UT-CU05-BE-08 — Endpoints del panel, la selección y la decisión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-08` |
| **Nombre** | Endpoints del panel, la selección y la decisión |
| **Caso de uso** | CU05-BE |
| **Depende de** | `UT-CU05-BE-05`, `UT-CU05-BE-06`, `UT-CU05-BE-07` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `CierrePostulacionController` (CU-20) y `FinalistaController` (CU-21).

## Solicitud de negocio

Las pantallas «Auditoría y adjudicación» y el expediente consumen estos endpoints.

## Objetivo

Existen los cuatro endpoints con controllers delgados.

## Alcance

- `B/features/postulaciones/controller/AdjudicacionController.java` con tres métodos:
  - `GET /api/v1/convocatorias/{codigo}/adjudicacion` → `200` `ApiResponse<PanelAdjudicacionResponse>` («Panel de adjudicación obtenido correctamente»);
  - `POST /api/v1/finalistas/{codigo}/adjudicar` → `200` `ApiResponse<DecisionAdjudicacionResponse>` («Beca adjudicada correctamente»), sin cuerpo;
  - `POST /api/v1/finalistas/{codigo}/no-adjudicar` → `200` con `{ motivo }` validado con `@Valid` («Postulación marcada como no adjudicada correctamente»).
- `B/features/finalistas/controller/SeleccionFinalistasController.java` (o un método nuevo de `FinalistaController`): `POST /api/v1/convocatorias/{codigo}/finalistas` con `{ cantidad? }` opcional validado con `@Valid` → `201` `ApiResponse<SeleccionFinalistasResponse>` («Finalistas seleccionados correctamente»); el cuerpo puede venir vacío.
- Exigen `ADMINISTRADOR_DUBSS` (el `403` lo escribe `ApiAccessDeniedHandler`), usan `@CurrentUserId` como actor, no tienen lógica ni logging y **no** reciben `Idempotency-Key`. Los `409` salen con los códigos `ERR_ADJ_*` por el `GlobalExceptionHandler`.
- Tests de contrato con `MockMvc`, para cada endpoint: forma del sobre y código feliz; el panel con cupos y mínimo definidos y sin ellos; `400` `ERR_SYS_01` por cantidad cero, negativa o no numérica; `400` `ERR_SYS_01` por motivo vacío, solo espacios o de más de 500; `404` `ERR_PRI_01` / `ERR_FIN_01`; `409` con cada `ERR_ADJ_*` que le corresponde; `401` sin sesión y `403` con otro rol (Directivo, Postulante, Personal del Comedor).

Archivos esperados (crear): `AdjudicacionController`, el controller de selección y sus tests.

## Fuera de alcance (no tocar)

- Lógica de negocio.
- Un endpoint para listar el historial (lo leerá el CU-04) ni para editar la cantidad de finalistas de la convocatoria (CU-13).
- Endpoints de acta o de publicación (CU-08 y CU-22).
- Reabrir o cambiar una decisión.

## Resultado esperado

Los cuatro endpoints responden según el contrato del README.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. La documentación de la API (Swagger) lista los cuatro endpoints con sus errores.
