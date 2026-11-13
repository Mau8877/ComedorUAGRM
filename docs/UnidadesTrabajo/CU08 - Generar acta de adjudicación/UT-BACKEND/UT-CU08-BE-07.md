# UT-CU08-BE-07 — Generar el acta y vista previa

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-07` |
| **Nombre** | Generar el acta y vista previa |
| **Caso de uso** | CU08-BE |
| **Depende de** | `UT-CU08-BE-02`, `UT-CU08-BE-03`, `UT-CU08-BE-04`, `UT-CU08-BE-05`, `UT-CU08-BE-06` |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (INFO al crear; nunca datos sensibles), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).
- Como referencia: `PublicacionResultadosService` (CU-22: bloqueo de la convocatoria, `@Auditar` con `ResultadoAuditado`) y `AlmacenamientoService` (`subir`).

## Solicitud de negocio

Generar el acta es el momento en que el resultado queda formalizado en un documento oficial, una sola vez.

## Objetivo

El Administrador genera el acta de una convocatoria con resultados publicados, queda guardada y registrada; y puede ver antes una vista previa.

## Alcance

- `B/features/actas/service/ActaAdjudicacionService.java` (`@RequiredArgsConstructor`, `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`), método `generar(UUID actorCodigo, UUID convocatoriaCodigo)` con `@Transactional` y `@Auditar(accion = BitacoraAcciones.GENERACION_ACTA, entidad = "acta_adjudicacion")`, devolviendo `ResultadoAuditado<ActaResponse>`:
  1. Busca la convocatoria (`ERR_PRI_01`, `404`) y la **bloquea** (`FOR UPDATE`); vuelve a leerla ya bajo el bloqueo.
  2. Valida en este orden: ya tiene acta → `ERR_ACT_02` (`409`); resultados sin publicar → `ERR_ACT_01` (`409`).
  3. Carga los adjudicados (una consulta) y pide el número a `NumeroActaService.siguiente(ahora)`, con `ahora` tomado de un `Clock` inyectado (para poder simular el cambio de año en las pruebas).
  4. Genera el PDF con `ActaPdfService` y lo **sube a MinIO** (`AlmacenamientoService.subir`, `application/pdf`) en `actas/{codigo_convocatoria}/acta-{NNN}-{AAAA}.pdf`. Un fallo de MinIO o del PDF sube como `500` genérico y **no** deja acta ni bitácora.
  5. Guarda la fila con `saveAndFlush`; si la unicidad de `convocatoria_id` salta pese al bloqueo, se convierte en `ERR_ACT_02`. La bitácora se anota en la misma transacción.
  6. Devuelve `ActaResponse` (sin ruta).
- Método `vistaPrevia(UUID convocatoriaCodigo)` (`@Transactional(readOnly = true)`, mismo rol): mismas validaciones (`ERR_PRI_01`, `ERR_ACT_02` y `ERR_ACT_01`), mismo PDF con `NumeroActaService.borrador()`; **no** sube a MinIO, **no** consume número y **no** anota en la bitácora. Devuelve el `byte[]` y el nombre de archivo sugerido (`acta-borrador-{convocatoria-sin-tildes}.pdf`).
- Bitácora: `detalle` con `acta`, `convocatoria`, `numero` y `adjudicados`; **sin** nombres, CI, rutas ni direcciones.
- Log `INFO` único con código de convocatoria, número y cantidad de adjudicados.
- **No** cambia el estado de ninguna postulación ni de la convocatoria, **no** avisa a nadie y **no** modifica `adjudicacion` ni `publicacion_resultado`.
- Tests con los colaboradores simulados: genera con resultados publicados; `ERR_ACT_01` sin publicar; `ERR_ACT_02` si ya tiene acta y en doble llamada; `ERR_PRI_01`; con cero adjudicados igual genera; si falla MinIO o el PDF no se guarda nada y no hay bitácora; si falla guardar la fila la excepción sube y no se anota nada; el número y la ruta siguen el formato; la bitácora no contiene datos sensibles; la vista previa no sube, no guarda y no anota, y responde los mismos `409`; ningún estado cambia; rol distinto de Administrador recibe `403`.

Archivos esperados (crear): `ActaAdjudicacionService` y su test.

## Fuera de alcance (no tocar)

- Los endpoints (UT-09).
- Listar, consultar y descargar (UT-08).
- Regenerar, reemplazar o eliminar un acta; avisar a los postulantes; el padrón (CU-23).
- Cambiar `PublicacionResultadosService` o la lógica de CU-05/CU-22.

## Resultado esperado

El acta queda guardada y registrada una sola vez por convocatoria y la vista previa no deja huella.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
