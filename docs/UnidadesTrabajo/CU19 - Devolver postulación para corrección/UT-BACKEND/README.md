# Unidades de trabajo — CU19-BE

**Caso de uso:** Backend para devolver al postulante la postulación de un finalista con un motivo, dejar el cambio en el historial y en la bitácora, avisarle y mostrar el estado «devuelta» en la lista de finalistas.
**Fuente CU:** [`CU19 - Devolver postulación para corrección`](../../../CasosDeUso/CU19%20-%20Devolver%20postulación%20para%20corrección.md)
**Diagramas de grado:** CU-19 en la documentación de grado: contexto (`072`), análisis (`085`), secuencia (`100`) y maqueta (`113`). Diseño lógico: `images/091` (transcrito en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid)).
**Recurso HTTP / contrato:** `POST /api/v1/finalistas/{codigo}/devolver` (ver tabla).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas no las sustituyen: si hay conflicto, ganan las rules.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`, `T/` = `.../src/test/java/com/comedoruagrm/backend/`, `R/` = `.../src/main/resources/`.

- **Todo en `features/postulaciones/`.** La postulación y su historial son de ese módulo (es el dueño del dato); el endpoint cuelga de `/api/v1/finalistas/{codigo}` porque se llega desde el expediente del finalista (CU-07), igual que `ExpedienteController`. La consulta de lectura es JDBC propia, como la del CU-07: no se lee el repository de otro módulo.
- **`postulacion_historial` no existe todavía:** este CU crea la tabla con la forma exacta del diseño lógico, sin columnas extra. El CU-02, el CU-20 y el CU-05 la amplían sin recrearla.
- **Sin idempotencia por clave** *(decisión del equipo, para simplificar)*: no hay `Idempotency-Key`. El frontend deshabilita el botón mientras se envía y, si aun así llegan dos peticiones, la segunda recibe `409` `ERR_DEV_01` porque la postulación ya no está «calificada»; la fila de la postulación se bloquea durante la devolución para que dos peticiones simultáneas no se pisen.
- **Auditoría previa obligatoria:** se exige al menos una auditoría del finalista **posterior a su última devolución** (si la hubo); si no, `409` `ERR_DEV_02`. No se exige la entrega física.
- **Motivo:** obligatorio, máximo 500 caracteres; se valida en el DTO y la columna `motivo` sigue siendo `text` porque el CU-20 y el CU-05 la usan también.
- **Aviso:** evento nuevo `POSTULACION_DEVUELTA`, **bandeja, push y correo**, con el motivo como observaciones. Un fallo de envío nunca deshace la devolución.
- **Historial append-only:** el Model no tiene setters; devolver varias veces la misma postulación (tras reenvíos) agrega filas, sin límite.
- **Roles:** `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` en el `service`.
- **Errores** (prefijo `DEV`, documentar en `docs/errors/`): `ERR_DEV_01` (409) y `ERR_DEV_02` (409). Finalista inexistente reutiliza `ERR_FIN_01`.
- **Bitácora:** `DEVOLUCION_POSTULACION` con códigos y estados; sin motivo, nombres ni CI.
- **Datos de ejemplo:** UT-08, solo con perfil `dev`, sobre la convocatoria `[PRUEBA]`.

## Contrato de datos / recurso HTTP

| Método | Ruta | Cuerpo | Respuesta | Errores |
|--------|------|--------|-----------|---------|
| `POST` | `/api/v1/finalistas/{codigo}/devolver` | `{ motivo }` | `200` `DevolucionResponse` | `400` `ERR_SYS_01`; `404` `ERR_FIN_01`; `409` `ERR_DEV_01`/`ERR_DEV_02` |
| `GET` | `/api/v1/finalistas` (CU-21, ampliado) | — | `FinalistaResponse` + `estadoPostulacion` | los del CU-21 |

`DevolucionResponse` = `postulacion` (código público), `estado` (`devuelta`), `motivo`, `fecha`, `devueltaPor`. Todos los identificadores son códigos públicos (UUID). Exige `ADMINISTRADOR_DUBSS`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU19-BE-01](./UT-CU19-BE-01.md) | Migración: historial de la postulación | — |
| [UT-CU19-BE-02](./UT-CU19-BE-02.md) | Model y repository del historial; la postulación puede devolverse | 01 |
| [UT-CU19-BE-03](./UT-CU19-BE-03.md) | Códigos de error, bitácora, DTOs y evento de aviso | — |
| [UT-CU19-BE-04](./UT-CU19-BE-04.md) | Consulta de la devolución | 01 |
| [UT-CU19-BE-05](./UT-CU19-BE-05.md) | Devolver la postulación y avisar al postulante | 02, 03, 04 |
| [UT-CU19-BE-06](./UT-CU19-BE-06.md) | Lista de finalistas con el estado de la postulación | — |
| [UT-CU19-BE-07](./UT-CU19-BE-07.md) | Endpoint para devolver la postulación | 05, 06 |
| [UT-CU19-BE-08](./UT-CU19-BE-08.md) | Datos de prueba de devoluciones | 01, 02 |
| [UT-CU19-BE-09](./UT-CU19-BE-09.md) | Pruebas de integración con base real | 07, 08 |

**Cadena:** `01 → 02, 04`; `03` y `06` independientes; `02, 03, 04 → 05`; `05, 06 → 07`; `01, 02 → 08`; `07, 08 → 09`.

## Criterios de aceptación cubiertos

| CA (CU19) | Descripción | UTs |
|---|---|---|
| CA01 | Devolver con motivo válido | 02, 05, 07, 09 |
| CA02, CA03 | Motivo vacío o de más de 500 | 03, 07, 09 |
| CA04, CA05, CA13 | Ya no «calificada», doble petición y concurrencia (`409`) | 04, 05, 09 |
| CA06 | Aviso por bandeja, push y correo | 03, 05, 09 |
| CA07 | Bitácora sin datos sensibles | 03, 05, 09 |
| CA08 | Cancelar la ventana | — (solo frontend) |
| CA09 | Devuelta: no se audita ni sale en la lista priorizada | 05, 09 |
| CA10, CA11 | `403`/`401` y `404` | 05, 07, 09 |
| CA12 | Lista de finalistas muestra «Devuelta» | 06, 09 |
| CA14 | Sin auditoría previa (`409`) | 04, 05, 09 |
| CA15, CA16 | Devolver de nuevo sin límite y conservar el historial | 02, 05, 09 |

## Follow-ups (no se implementan en estas UT)

- Que el CU-02 reenvíe la postulación devuelta (vuelve a «pendiente», `numero_envios` + 1) y el CU-05 la vuelva a elegir.
- Que el CU-04 muestre al postulante el motivo leyendo el historial.
- Archivar o rechazar (CU-20).

## Qué NO incluyen estas UT

- La pantalla (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)).
- El reenvío del postulante, el recálculo del puntaje y la nueva elección del finalista (CU-02, CU-03, CU-05).
- El plazo de reenvío: no existe.
- La app móvil.
