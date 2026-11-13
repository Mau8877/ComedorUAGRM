# UT-CU04-BE-01 — Código de error, DTOs y estado visible

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-01` |
| **Nombre** | Código de error, DTOs y estado visible |
| **Caso de uso** | CU04-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (documentar el código en el mismo cambio), [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (DTOs como `record`).
- Como referencia: `DevolucionErrorCodes`, `DevolucionResponse` y `docs/errors/postulaciones/ERRORES_POSTULACIONES.md`.

## Solicitud de negocio

El postulante nunca debe ver un estado que la DUBSS todavía no publicó, y los errores de la consulta deben estar catalogados.

## Objetivo

Existen el código `ERR_POS_01` (documentado), los DTOs del contrato y la función pura que calcula el estado visible.

## Alcance

- `B/features/postulaciones/exception/MisPostulacionesErrorCodes.java`: constante `ERR_POS_01` (postulación inexistente o ajena).
- `docs/errors/postulaciones/ERRORES_POSTULACIONES.md`: sección `ERR_POS_01` con el formato de la guía (HTTP `404`, `NotFoundException`, significado, dónde se lanza `MisPostulacionesService.detalle()`) y su link en `docs/errors/README.md`, dentro de la sección de postulaciones.
- DTOs `record` en `B/features/postulaciones/dto/`: `MiPostulacionResumenResponse`, `MiPostulacionDetalleResponse`, `PasoLineaTiempoResponse` (`estado`, `fecha`, `situacion`), `ObservacionesResponse`, `FinalistaPostulanteResponse` y `ConvocatoriaResumenResponse` (si no existe ya uno equivalente), según el contrato del README. Ningún DTO lleva ids internos, CI, correo ni el autor de un cambio.
- Enum `SituacionPaso { COMPLETADO, PENDIENTE }` en `B/features/postulaciones/enums/`.
- `B/features/postulaciones/service/EstadoVisible.java`: clase utilitaria sin estado con `calcular(EstadoPostulacion real, boolean resultadosPublicados)` (devuelve `calificada` si el real es `adjudicada` o `no_adjudicada` y no están publicados; si no, el real) y `ocultaFila(EstadoPostulacion estadoNuevo, boolean resultadosPublicados)`.
- Test unitario `T/features/postulaciones/service/EstadoVisibleTest`: todas las combinaciones de los siete estados con publicado y sin publicar.

Archivos esperados (crear): `MisPostulacionesErrorCodes`, los DTOs, `SituacionPaso`, `EstadoVisible` y su test. (Tocar): `ERRORES_POSTULACIONES.md` y `docs/errors/README.md`.

## Fuera de alcance (no tocar)

- Consultas JDBC, services y controllers (UT-02 a UT-04).
- Códigos de error de otros módulos.
- Cualquier tabla, migración o cambio de `DiagramaConceptual.mermaid` (este CU no cambia el diseño lógico).

## Resultado esperado

El módulo compila con los tipos del contrato y el cálculo del estado visible está fijado por tests.

## Validación

1. `./mvnw test` pasa (incluido `EstadoVisibleTest`).
2. `./mvnw checkstyle:check` sin errores.
3. `ERR_POS_01` aparece en `docs/errors/` y en su índice.
