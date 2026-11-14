# UT-CU05-BE-06 — Seleccionar finalistas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-06` |
| **Nombre** | Seleccionar finalistas |
| **Caso de uso** | CU05-BE |
| **Depende de** | `UT-CU05-BE-02`, `UT-CU05-BE-03`, `UT-CU05-BE-04` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/backend/ARQUITECTURA_BACKEND.md) (regla de dependencia entre módulos).
- Como referencia: `FinalistaService` (CU-21), `Finalista` (constructor `Finalista(postulacionId, posicion)`) y `CierrePostulacionService` (CU-20).

## Solicitud de negocio

La DUBSS elige como finalistas a los postulantes mejor puntuados para auditar solo esos expedientes.

## Objetivo

`SeleccionFinalistasService` crea las filas de `finalista` de los mejores postulantes calificados que todavía no lo son, con su posición en la lista priorizada.

## Alcance

- `B/features/finalistas/service/SeleccionFinalistasService.java` (el módulo de `finalistas` es el dueño de la tabla `finalista`) con `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`, `@Transactional` y `@Auditar` (`SELECCION_FINALISTAS`). Método `seleccionar(UUID actorCodigo, UUID convocatoriaCodigo, SeleccionarFinalistasRequest request)` que devuelve un `ResultadoAuditado<SeleccionFinalistasResponse>`. Obtiene los datos con `AdjudicacionConsultaRepository` (UT-04), invocado como servicio o consulta expuesta por el módulo de `postulaciones`, **sin** leer su repository directo; si hace falta, se publica un método de `AdjudicacionService` para eso, sin duplicar SQL.
- Orden de validaciones:
  1. convocatoria inexistente → `404` `ERR_PRI_01`;
  2. se bloquea la convocatoria (`bloquearConvocatoria`), así dos selecciones simultáneas no repiten finalistas;
  3. convocatoria que no está `CERRADA`, o sin cupos o sin puntaje mínimo → `409` `ERR_ADJ_05`;
  4. resultados publicados → `409` `ERR_ADJ_01`;
  5. cantidad: la del request, o si falta, `cantidad_finalistas` de la convocatoria; si no hay ninguna de las dos, o es cero, negativa o **mayor** que las calificadas que aún no son finalistas → `400` `ERR_SYS_01` con mensaje en español que dice cuántas hay disponibles.
- **Puede superar los cupos** (decisión 1: los extra son suplentes); no se valida contra los cupos disponibles.
- Si todo es válido: toma las primeras `cantidad` postulaciones de `siguientesSinFinalista` y guarda un `Finalista(postulacionId, posicion)` por cada una, con la posición de la lista **completa** (no de la página). Puede repetirse para sumar finalistas sin repetir a los ya seleccionados (índice único `uq_finalista_postulacion`). **No** se tocan plazo, notificación ni entrega (son del CU-21) ni la postulación.
- La respuesta lista los finalistas creados en el orden de la lista priorizada.
- Una sola transacción: finalistas y bitácora se guardan juntos o ninguno (A9). **No** hay idempotencia por clave. **No** se avisa a nadie.
- Bitácora: código de la convocatoria y cantidad seleccionada; **sin** nombres, CI ni puntajes. `LOG.info` con módulo, actor y cantidad.
- Tests unitarios: convocatoria inexistente; no cerrada; sin cupos; sin puntaje mínimo; resultados publicados simulados; cantidad del request prevalece sobre la configurada; se usa la configurada si no viene cantidad; ninguna de las dos (`400`); cero, negativa y mayor que las disponibles (`400` y no se guarda nada); cantidad mayor que los cupos (permitida); segunda selección que no repite a los anteriores y continúa las posiciones de la lista completa; sin candidatas.

Archivos esperados (crear): `SeleccionFinalistasService` y su test. (Tocar, si hace falta): `AdjudicacionService` para exponer la consulta.

## Fuera de alcance (no tocar)

- Notificar a los finalistas y registrar la entrega física (CU-21).
- Auditar el expediente (CU-07) y decidir (UT-07).
- Escribir `cantidad_finalistas` en la convocatoria (CU-13).
- Recalcular la posición de finalistas ya existentes cuando otros salen de la lista *(por revisar, ver README)*.
- Endpoint (UT-08).

## Resultado esperado

El Administrador puede sumar finalistas en orden de puntaje, sin repetir, y la convocatoria no cerrada o sin cupos queda protegida con `ERR_ADJ_05`.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
