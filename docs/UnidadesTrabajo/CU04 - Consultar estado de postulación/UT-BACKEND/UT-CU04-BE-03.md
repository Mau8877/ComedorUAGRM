# UT-CU04-BE-03 — Detalle: línea de tiempo e información asociada

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-03` |
| **Nombre** | Detalle: línea de tiempo e información asociada |
| **Caso de uso** | CU04-BE |
| **Depende de** | `UT-CU04-BE-01` |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (un módulo no lee el repository de otro).
- Releer las «Decisiones de diseño» del [README](./README.md): estado visible, qué se oculta y qué motivo se muestra.

## Solicitud de negocio

Al abrir una postulación, el postulante debe entender en qué paso está, qué le falta y qué tiene que hacer, sin ver nada que la DUBSS todavía no publicó.

## Objetivo

El backend devuelve el detalle de una postulación propia con estado visible, línea de tiempo (pasada y pendiente) e información asociada al estado.

## Alcance

- Ampliar `MisPostulacionesConsultaRepository` (UT-02) con consultas por postulación propia: cabecera, historial (sin `usuario_id`), última fila de historial con estado nuevo `devuelta`, `archivada` o el rechazo que produjo `no_adjudicada`, fila de `finalista`, `adjudicacion`, existencia de `publicacion_resultado` y el último mensaje de la `notificacion` de aviso de finalista de esa postulación. Todas con `eliminado_en IS NULL`. Número fijo de consultas.
- `MisPostulacionesService.detalle(UUID usuarioCodigo, UUID codigoPostulacion)`: `@PreAuthorize("hasRole('POSTULANTE')")`, `@Transactional(readOnly = true)`. Si la postulación no existe, es ajena o está en `borrador`, lanza `NotFoundException` `ERR_POS_01` (el mismo mensaje en los tres casos).
- Reglas del README: oculta del historial las filas `adjudicada`/`no_adjudicada` mientras no esté publicado (`EstadoVisible.ocultaFila`); calcula los pasos pendientes según el estado visible (sin inventar pasos pasados); `observaciones` solo si `devuelta`; `motivo` solo para archivo o rechazo del CU-20 (nunca el de la decisión final del CU-05); bloque `finalista` solo si existe la fila; `resultado` solo con resultados publicados; `puedeCorregir` solo si el estado visible es `devuelta`.
- Tests unitarios (mock del repository) de cada rama: una por estado visible, finalista con y sin documentación recibida, publicada y sin publicar, ajena e inexistente (`404` igual), rol equivocado. Un test confirma que el DTO no contiene el autor de ningún cambio ni el motivo de la decisión final.

Archivos esperados (tocar): `MisPostulacionesConsultaRepository`, `MisPostulacionesService`. (Crear): sus tests.

## Fuera de alcance (no tocar)

- El listado (UT-02) y el controller (UT-04).
- Crear la fila de historial del envío o la corrección: eso es del CU-02.
- Marcar avisos como leídos, bitácora y cualquier escritura.

## Resultado esperado

El service entrega el detalle completo, sin datos reservados de la DUBSS.

## Validación

1. `./mvnw test` pasa.
2. Un detalle de una postulación ajena responde igual que uno inexistente (`ERR_POS_01`).
3. Una `adjudicada` sin publicar muestra «calificada», sin esa fila en la línea de tiempo y sin resultado.
4. `./mvnw checkstyle:check` sin errores.
