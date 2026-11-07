# UT-CU21-BE-07 — Registrar y corregir la entrega de documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-07` |
| **Nombre** | Registrar y corregir la entrega de documentación |
| **Caso de uso** | CU21-BE |
| **Depende de** | `UT-CU21-BE-02`, `UT-CU21-BE-03` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) y [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).

## Solicitud de negocio

Cuando el finalista lleva sus documentos, la DUBSS deja constancia de la fecha.

## Objetivo

`FinalistaService.registrarEntrega` guarda la fecha de recepción, la anota en la bitácora (con la anterior si es una corrección) y devuelve el finalista actualizado.

## Alcance

- `FinalistaService.registrarEntrega(actorCodigo, finalistaCodigo, RegistrarEntregaRequest)` con `@PreAuthorize`, `@Transactional` y `@Auditar(accion = ENTREGA_DOCUMENTACION_REGISTRADA, entidad = "finalista")`.
- `fechaRecepcion` opcional (hoy en `America/La_Paz` por defecto); una fecha futura → `400` `ERR_FIN_03` y no se guarda; finalista inexistente → `404` `ERR_FIN_01`. `observaciones` opcional (máx. 500).
- Guarda `documentacion_recibida_en` (inicio del día en `America/La_Paz`). Si ya había una fecha, la nueva la reemplaza y la anterior queda en el `detalle` de la bitácora (`fechaAnterior`); la corrección no cambia el estado de la entrega (A5).
- Bitácora: código del finalista, fecha, `fechaAnterior` si corresponde y observaciones (no hay columna: viven ahí). Nada de CI ni nombres. No cambia el estado de ninguna postulación.
- Devuelve el `FinalistaResponse` con los estados al día.
- Tests unitarios: fecha por defecto hoy, fecha pasada, fecha futura, corrección con valor anterior, finalista inexistente.

Archivos esperados (tocar): `FinalistaService` y su test.

## Fuera de alcance (no tocar)

- Devolver o archivar (CU-19, CU-20) y continuar la auditoría (CU-07).
- El checklist de documentos de la maqueta.
- Endpoint (UT-10).

## Resultado esperado

La entrega queda registrada con su fecha y la corrección conserva la anterior en la bitácora.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
