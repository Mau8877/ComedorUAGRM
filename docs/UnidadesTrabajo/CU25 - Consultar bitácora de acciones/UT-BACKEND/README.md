# Unidades de trabajo — CU25-BACKEND

**Caso de uso:** consulta paginada y de solo lectura de la bitácora, con filtros por usuario, acción y fechas.
**Fuente CU:** [`CU25 - Consultar bitácora de acciones.md`](../../../CasosDeUso/CU25%20-%20Consultar%20bit%C3%A1cora%20de%20acciones.md)
**Diagramas de grado:** contexto img 076 · análisis img 089 · secuencia img 104 · mockup img 117
**Recurso HTTP / contrato:** `GET /api/v1/bitacora`, `GET /api/v1/bitacora/acciones`

## Antes de implementar cualquier UT

Leer [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md),
[`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md),
[`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y
[`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md). Estas fichas no
sustituyen esas reglas.

## Decisiones de diseño

- **Servicio aparte (`BitacoraConsultaService`)**: `BitacoraService` es la única vía de escritura de la bitácora;
  la consulta va en otra clase para no mezclar escritura y lectura.
- **El filtro por usuario es `search`** (nombres, apellidos o correo), no `filter[usuario]`: el contrato dice que
  `filter[...]` es un filtro exacto y el mockup pide un campo de texto libre.
- **No se expone `entidadId`**: es el id interno de la fila afectada ([PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)).
  El mockup lo muestra como `adjudicacion #512`; la respuesta trae `entidad` y `detalle` (códigos públicos).
- **Fechas en hora de Bolivia**, ambos extremos incluidos (`desde` 00:00 y `hasta` hasta las 24:00 del día).
- **Endpoint extra `/acciones`**: alimenta el filtro de acciones sin que el cliente conozca el catálogo.

## Contrato de datos / recurso HTTP

| Método y ruta | Query | `data` | Errores |
| --- | --- | --- | --- |
| `GET /api/v1/bitacora` | `page`, `pageSize`, `search`, `filter[accion]`, `filter[desde]`, `filter[hasta]`, `sort` (`fecha`, `accion`, `entidad`; por defecto `-fecha`) | `[{codigo, fecha, usuario{codigo,nombres,apellidos,correo}\|null, accion, entidad, detalle, ip}]` + `meta` | `400` `ERR_BIT_01`, `ERR_BIT_02`, `ERR_SYS_05`; `403` `ERR_AUTH_07` |
| `GET /api/v1/bitacora/acciones` | — | `["ACCION", ...]` | `403` `ERR_AUTH_07` |

## Orden de ejecución

| Código | Nombre | Depende de |
| --- | --- | --- |
| [UT-CU25-BE-01](./UT-CU25-BE-01.md) | Consulta de la bitácora (repositorio, filtros y servicio) | — |
| [UT-CU25-BE-02](./UT-CU25-BE-02.md) | Endpoints, DTO y catálogo de errores | BE-01 |
| [UT-CU25-BE-03](./UT-CU25-BE-03.md) | Pruebas de la consulta | BE-01, BE-02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
| --- | --- | --- |
| Flujo principal | Listar y filtrar | 01, 02 |
| A2 / CA03 | Rango de fechas invertido | 01, 02, 03 |
| A3 / CA04 | Sin el rol Administrador | 01 |
| CA05 | Sin operaciones de escritura | 02 |

## Follow-ups (no se implementan en estas UT)

- Índices en `bitacora` (`creado_en`, `accion`) si el volumen lo pide.

## Qué NO incluyen estas UT

- Nuevas acciones en `BitacoraAcciones` ni nuevos `@Auditar`: los escriben los CU que producen cada evento.
- Exportar la bitácora (PDF/CSV): no está en el CU.
