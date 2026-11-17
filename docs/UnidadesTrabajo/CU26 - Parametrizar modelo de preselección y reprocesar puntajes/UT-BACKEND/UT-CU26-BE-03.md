# UT-CU26-BE-03 — Recalcular un puntaje y contrato ampliado con la IA

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-03` |
| **Nombre** | Recalcular un puntaje y contrato ampliado con la IA |
| **Caso de uso** | CU26-BACKEND |
| **Depende de** | UT-CU26-BE-01 |

## Antes de implementar

[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) y la ficha de implementación del
[CU02](../../../Implementacion/CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md) (contrato con la IA).

## Solicitud de negocio

Recalcular el puntaje de una postulación ya calificada con otra versión del modelo, sin perder el puntaje anterior ni cambiar su estado.

## Objetivo

`PrecalificacionService.recalcular(postulacionId, modelo)` y la solicitud a la IA con la versión y las ponderaciones.

## Alcance

- `SolicitudPuntaje` suma `modelo` (versión) y `parametros` (clave → ponderación); `ModeloAplicable(id, version, parametros)`.
- `PrecalificacionService`: `precalificar()` manda la versión vigente (o `null` si no hay); `recalcular()` (lee una `calificada`, llama a la IA sin transacción, exige que la respuesta repita la versión, y en una transacción corta bloquea la fila, verifica que siga `calificada` y que no tenga ya puntaje de esa versión, retira el vigente e inserta el nuevo sin cambiar el estado); `aplicable()`; nuevo `Resultado.RECALCULADA`.
- Actualizar el contrato en `docs/Implementacion/CU02` y `PrecalificacionServiceTest` (constructor nuevo y casos de `recalcular`).

Archivos esperados (crear/tocar): `SolicitudPuntaje.java`, `ModeloAplicable.java`, `PrecalificacionService.java`, `PrecalificacionServiceTest.java`, `docs/Implementacion/CU02 ...md`.

## Fuera de alcance (no tocar)

- El módulo de IA en Python (B3): solo se define el contrato.
- La normalización del puntaje a 0–70 (queda pendiente con la DUBSS; ver el CU26).
- Recorrer las postulaciones de una convocatoria (BE-04).

## Resultado esperado

Una postulación `calificada` recalculada deja un puntaje vigente nuevo con la versión pedida, el anterior en historial y el mismo estado.

## Validación

1. `./mvnw -o -q test -Dtest=PrecalificacionServiceTest` en verde (incluye IA caída, datos rechazados, otra versión en la respuesta y puntaje ya existente).
2. Con la IA simulada: `recalcular()` dos veces seguidas con la misma versión no inserta un segundo puntaje (la segunda devuelve `OMITIDA`).
