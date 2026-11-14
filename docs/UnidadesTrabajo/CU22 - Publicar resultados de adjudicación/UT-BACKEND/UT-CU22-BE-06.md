# UT-CU22-BE-06 — Publicar y avisar a los finalistas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-06` |
| **Nombre** | Publicar y avisar a los finalistas |
| **Caso de uso** | CU22-BE |
| **Depende de** | `UT-CU22-BE-02`, `UT-CU22-BE-03`, `UT-CU22-BE-04` |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (INFO al crear/aprobar; nunca datos sensibles), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).
- Como referencia: `CierrePostulacionService` (CU-20) y el envío por lotes de `UT-CU21-BE-08`.

## Solicitud de negocio

Publicar es el momento en que el resultado se vuelve oficial y cada finalista se entera.

## Objetivo

El Administrador publica los resultados de una convocatoria, queda registrado y cada finalista con decisión recibe su aviso.

## Alcance

- `B/features/resultados/service/PublicacionResultadosService.java` (`@RequiredArgsConstructor`, `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`, `@Transactional`), método `publicar(UUID convocatoriaCodigo)`:
  1. Busca la convocatoria (`ERR_PRI_01`, `404`) y la **bloquea** (`FOR UPDATE`).
  2. **Se publica aunque no haya adjudicados** (no se exige un mínimo). Valida que esté cerrada (`ERR_PUB_03`), que **no** esté publicada (`ERR_PUB_02`, `409`, y si hay carrera entre dos peticiones, la unicidad también lo convierte en este error) y que no queden finalistas en «calificada» (`ERR_PUB_01`, `409`, con el mensaje «Faltan N decisiones»).
  3. **No exige el acta (CU-08).**
  4. Guarda `PublicacionResultado` (usuario del token, `Instant.now()`), pasa la convocatoria a «publicada» y registra `PUBLICACION_RESULTADOS` en la bitácora (código de convocatoria y cantidades; **sin** nombres, CI, correos ni tokens), todo en la misma transacción.
  5. Crea las filas de `notificacion` para cada destinatario de `destinatariosDelAviso` (que ya trae todo en **una sola consulta**), con el evento `BECA_ADJUDICADA` o `BECA_NO_ADJUDICADA` según su resultado, **por lotes de 500 en la misma transacción** (inserción por lotes, nunca una por una dentro de un bucle).
  6. Después de confirmar la transacción, el servicio de notificaciones despacha push y correo de forma asíncrona, resolviendo los tokens de dispositivo **en bloque** (una consulta para todos los destinatarios de cada lote). Un fallo de envío deja el aviso en la bandeja con `enviada_en` nulo y **nunca** deshace la publicación.
  7. Devuelve `PublicacionResultadoResponse`.
- Log `INFO` único con código de convocatoria y cantidades (sin datos personales).
- **No** cambia el estado de ninguna postulación, **no** genera acta y **no** crea el padrón.
- Tests con los colaboradores simulados: publica con todo decidido, `ERR_PUB_01` con pendientes y su cantidad, `ERR_PUB_02` si ya estaba publicada y en doble llamada, `ERR_PUB_03` si no está cerrada, `ERR_PRI_01`, avisa solo a finalistas con adjudicación (no a archivados, rechazados ni devueltos), cada uno con su resultado, que un fallo de envío no deshace la publicación, que si falla guardar los avisos no queda publicación, que publicar con cero adjudicados funciona y avisa «no adjudicada» a cada finalista, que el número de consultas es **constante** con 5 o con 500 finalistas (sin N+1), que ninguna postulación cambia de estado y que la bitácora no contiene datos sensibles; rol distinto de Administrador recibe `403`.

Archivos esperados (crear): `PublicacionResultadosService` y su test.

## Fuera de alcance (no tocar)

- Los endpoints (UT-08).
- Conectar la marca en CU-05, 19 y 20 (UT-09).
- El acta (CU-08), el padrón (CU-23) y la reapertura.
- Cambiar el motor de envío de CU-EXTRA-01.

## Resultado esperado

La publicación queda registrada, la convocatoria queda «publicada» y los finalistas con decisión reciben su resultado.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
