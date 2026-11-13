# UT-CU22-BE-03 — Códigos de error, bitácora, DTOs y evento de aviso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-03` |
| **Nombre** | Códigos de error, bitácora, DTOs y evento de aviso |
| **Caso de uso** | CU22-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md).
- Como referencia: las piezas equivalentes del CU-20 (`UT-CU20-BE-02`) y `EventoNotificacion` de CU-EXTRA-01.

## Solicitud de negocio

Los errores y los avisos del CU deben tener su propio código y texto, y la publicación debe quedar en la bitácora.

## Objetivo

Quedan definidos los códigos de error, la acción de bitácora, los DTOs y los eventos de aviso del CU-22.

## Alcance

- `B/features/resultados/ResultadosErrorCodes.java`: `ERR_PUB_01` a `ERR_PUB_04` (ver README). Excepciones de la jerarquía: `ConflictException` para 01, 02 y 03; `NotFoundException` para 04.
- Documentar los cuatro códigos en `docs/errors/resultados/ERRORES_RESULTADOS.md` (formato obligatorio de `GUIA_ERRORES_BACKEND.md`) y enlazarlos desde `docs/errors/README.md`.
- `AccionBitacora.PUBLICACION_RESULTADOS` (valor nuevo del catálogo de acciones).
- DTOs `record` en `B/features/resultados/dto/`: `ResumenPublicacionResponse`, `DecisionResultadoResponse`, `PublicacionResultadoResponse`, `ConvocatoriaPublicadaResponse`, `AdjudicadoPublicoResponse` y `MiResultadoResponse` (campos exactos del README).
- Dos eventos en `EventoNotificacion`, ambos por **bandeja, push y correo**, con marcadores `{nombres}` y `{convocatoria}` y título «Beca Comedor: resultado de tu postulación»:
  - `BECA_ADJUDICADA` (tipo `SUCCESS`): «{nombres}, ¡felicidades! Tu postulación en {convocatoria} fue adjudicada: obtuviste la Beca Comedor.»
  - `BECA_NO_ADJUDICADA` (tipo `INFO`): «{nombres}, tu postulación en {convocatoria} no fue adjudicada. Gracias por participar.»
- Tests: cada evento reemplaza sus marcadores, envía por los tres canales y falla si falta un marcador; los códigos son únicos.

Archivos esperados (crear/tocar): `ResultadosErrorCodes`, los DTOs, `AccionBitacora`, `EventoNotificacion`, los documentos de errores y sus tests.

## Fuera de alcance (no tocar)

- Lanzar los errores o enviar avisos (UT-06).
- Cambiar los demás eventos de notificación.
- Reglas de CU-05, 19 y 20.

## Resultado esperado

Los demás componentes pueden usar códigos, DTOs, acción de bitácora y evento sin inventarlos.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Los códigos aparecen en `docs/errors/` y en su índice.
