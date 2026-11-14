# Unidades de trabajo — CU20-BE

**Caso de uso:** Backend para archivar o rechazar la postulación «calificada» de un finalista con un motivo, dejar el cambio en el historial y en la bitácora, avisar al postulante y mostrar el estado resultante en las listas.
**Fuente CU:** [`CU20 - Archivar o rechazar postulación`](../../../CasosDeUso/CU20%20-%20Archivar%20o%20rechazar%20postulación.md)
**Diagramas de grado:** CU-20 en la documentación de grado (contexto, análisis, secuencia y maqueta; números de figura por confirmar). Diseño lógico: `images/091` (transcrito en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid)).
**Recurso HTTP / contrato:** `POST /api/v1/finalistas/{codigo}/archivar` y `POST /api/v1/finalistas/{codigo}/rechazar` *(rutas propuestas, por revisar)*.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas no las sustituyen: si hay conflicto, ganan las rules.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`, `T/` = `.../src/test/java/com/comedoruagrm/backend/`, `R/` = `.../src/main/resources/`.

Las marcadas *(por revisar)* son propuestas del CU-20 que la DUBSS o Mau aún no confirmaron; si cambian, se ajusta la UT indicada.

- **Se reutiliza todo lo del CU-19.** `postulacion_historial` (tabla, Model y repository), `PostulacionHistorial`, el patrón de `DevolucionService`/`DevolucionController` y la columna `estadoPostulacion` de la lista de finalistas ya existen: **no hay migración ni tabla nueva**.
- **Todo en `features/postulaciones/`**, con los endpoints colgando de `/api/v1/finalistas/{codigo}` (se llega desde el expediente, igual que el CU-19). La consulta de lectura es JDBC propia; no se lee el repository de otro módulo.
- **Dos acciones, un solo servicio:** `CierrePostulacionService` con `archivar(...)` y `rechazar(...)`. Archivar deja `archivada`; rechazar deja `no_adjudicada`. Son estados finales: no hay vuelta a «calificada».
- **Sin auditoría previa ni entrega física obligatorias** *(por revisar, preguntas 1 y 2 del CU)*: a diferencia del CU-19, el CU-07 prevé archivar a quien no entregó en plazo, donde puede no haber auditoría. Por eso no existe un `ERR_ARC_02`.
- **Sin idempotencia por clave**, igual que el CU-19: el frontend deshabilita el botón mientras se envía; la fila de la postulación se bloquea (`FOR UPDATE`) y la segunda petición recibe `409` `ERR_ARC_01`.
- **Un solo código de error propio:** `ERR_ARC_01` (`409`, la postulación ya no está «calificada» **o** los resultados ya fueron publicados) *(código propuesto, por revisar)*. El mensaje incluye el estado actual. Finalista inexistente reutiliza `ERR_FIN_01`; motivo inválido cae en `ERR_SYS_01`.
- **Resultados publicados (CU-22):** el CU-22 aún no está implementado, así que **esta UT no crea la marca de publicación**. La consulta (UT-03) lee la marca cuando exista; mientras no exista devuelve «no publicados» y la regla se prueba con el `service` simulado. Al implementar el CU-22 se conecta (ver Follow-ups).
- **Motivo:** obligatorio, máximo 500 caracteres, se valida en el DTO. Va en `postulacion_historial.motivo` (`text`).
- **Aviso:** eventos nuevos `POSTULACION_ARCHIVADA` (`INFO`) y `POSTULACION_RECHAZADA` (`ERROR`), **bandeja, push y correo**, con el motivo *(textos y siempre-avisar, por revisar, pregunta 6)*. Un fallo de envío nunca deshace la decisión.
- **Bitácora:** `ARCHIVO_POSTULACION` y `RECHAZO_POSTULACION` con códigos y estados; sin motivo, nombres, CI, rutas ni tokens *(decisión propia, por revisar)*.
- **«Rechazar» y «no adjudicar» (CU-05)** dejan el mismo estado `no_adjudicada`; cómo se distinguen en reportes (CU-12) queda abierto *(por revisar, preguntas 5 y 7)*. Esta UT no agrega una columna para distinguirlos: la fila del historial (usuario, fecha, motivo) y la acción de bitácora ya los distinguen.
- **Sin reapertura ni apelación** *(por revisar, pregunta 4)*.
- **Roles:** `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` en el `service`.
- **Datos de ejemplo:** UT-07, solo con perfil `dev`, sobre la convocatoria `[PRUEBA]`.

## Contrato de datos / recurso HTTP

| Método | Ruta | Cuerpo | Respuesta | Errores |
|--------|------|--------|-----------|---------|
| `POST` | `/api/v1/finalistas/{codigo}/archivar` | `{ motivo }` | `200` `CierrePostulacionResponse` | `400` `ERR_SYS_01`; `404` `ERR_FIN_01`; `409` `ERR_ARC_01` |
| `POST` | `/api/v1/finalistas/{codigo}/rechazar` | `{ motivo }` | `200` `CierrePostulacionResponse` | los mismos |
| `GET` | `/api/v1/finalistas` (CU-21, ya ampliado por el CU-19) | — | `estadoPostulacion` ahora también `archivada` / `no_adjudicada` | los del CU-21 |

`CierrePostulacionResponse` = `postulacion` (código público), `estado` (`archivada` o `no_adjudicada`), `motivo`, `fecha`, `decididaPor`. Todos los identificadores son códigos públicos (UUID). Exige `ADMINISTRADOR_DUBSS`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU20-BE-01](./UT-CU20-BE-01.md) | La postulación puede archivarse y rechazarse | — |
| [UT-CU20-BE-02](./UT-CU20-BE-02.md) | Códigos de error, bitácora, DTOs y eventos de aviso | — |
| [UT-CU20-BE-03](./UT-CU20-BE-03.md) | Consulta del cierre | — |
| [UT-CU20-BE-04](./UT-CU20-BE-04.md) | Archivar o rechazar y avisar al postulante | 01, 02, 03 |
| [UT-CU20-BE-05](./UT-CU20-BE-05.md) | Las listas reflejan el cierre (solo pruebas) | — |
| [UT-CU20-BE-06](./UT-CU20-BE-06.md) | Endpoints para archivar y rechazar | 04 |
| [UT-CU20-BE-07](./UT-CU20-BE-07.md) | Datos de prueba de cierres | 01 |
| [UT-CU20-BE-08](./UT-CU20-BE-08.md) | Pruebas de integración con base real | 05, 06, 07 |

**Cadena:** `01, 02, 03 → 04 → 06`; `05` independiente; `01 → 07`; `05, 06, 07 → 08`.

## Criterios de aceptación cubiertos

| CA (CU20) | Descripción | UTs |
|---|---|---|
| CA01, CA02 | Archivar y rechazar con motivo válido | 01, 04, 06, 08 |
| CA03, CA04 | Motivo vacío o de más de 500 | 02, 06, 08 |
| CA05, CA06 | Ya no «calificada» y resultados publicados (`409`) | 03, 04, 08 |
| CA07 | Doble petición y concurrencia | 03, 04, 08 |
| CA08 | Aviso por bandeja, push y correo | 02, 04, 08 |
| CA09 | Bitácora sin datos sensibles | 02, 04, 08 |
| CA10 | Cancelar la ventana | — (solo frontend) |
| CA11 | Sale de la lista priorizada, no se audita, se conserva el historial | 04, 05, 08 |
| CA12 | `403`/`401`/`404` | 04, 06, 08 |
| CA13 | El postulante ve estado y motivo | — (CU-04, ver Follow-ups) |
| CA14 | Historial completo, sin editar ni borrar | 01, 04, 08 |

## Follow-ups (no se implementan en estas UT)

- Conectar la regla de «resultados publicados» con la marca real cuando se implemente el CU-22.
- Que el CU-04 muestre al postulante el estado y el motivo leyendo el historial (CA13).
- Reapertura, apelación y plazo de entrega física (preguntas abiertas 3 y 4 del CU, con la DUBSS).
- Cómo distinguir «rechazar» de «no adjudicar» en los reportes del CU-12.

## Qué NO incluyen estas UT

- La pantalla (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)).
- Devolver (CU-19), la decisión final de adjudicar o no adjudicar (CU-05) y publicar resultados (CU-22).
- Una tabla o columna nueva.
- La app móvil.
