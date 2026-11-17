# UT-CU03-BE-05 — Core: precalificar y registrar el puntaje

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-05` |
| **Nombre** | Core: precalificar y registrar el puntaje |
| **Caso de uso** | CU03-BACKEND |
| **Depende de** | UT-CU03-BE-03, UT-CU03-BE-04 |

## Antes de implementar

[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) (por qué no se audita) y [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (regla de dependencia entre módulos).

## Solicitud de negocio

Cuando la IA responde, el puntaje debe quedar guardado de forma atómica con el cambio de estado, sin duplicarse y sin perder el anterior.

## Objetivo

`PrecalificacionService.precalificar()` y `registrar()`: calcula, guarda el puntaje vigente y pasa la postulación a `calificada` en una sola transacción.

## Alcance

- `precalificar()`: lee los datos y el modelo vigente, llama a la IA **sin transacción abierta** y registra el resultado. `IaNoDisponible` → `IA_NO_DISPONIBLE`; `IaDatosInvalidos` → `NO_CALCULADA`; sin datos o ya no `pendiente` → `OMITIDA`.
- `registrar()` (transacción corta): bloquea la postulación, vuelve a comprobar que siga `pendiente`, busca el modelo por la versión que informó la IA (si no está registrada → `NO_CALCULADA`), retira el puntaje vigente, inserta el nuevo con su fecha y detalle y llama a `CalificacionPostulacionService.marcarCalificada()`.
- `CalificacionPostulacionService` (módulo de postulaciones): `calificar()` y una fila de historial «Precalificación automática» sin usuario.
- `PuntajePostulacion` y su repositorio.
- `PrecalificacionServiceTest` con la IA simulada: éxito, IA caída, datos rechazados, versión no registrada, repetido, y los datos del formulario oficial.

Archivos esperados (crear/tocar): `PrecalificacionService.java`, `CalificacionPostulacionService.java`, `PuntajePostulacion.java`, `PuntajePostulacionRepository.java`, `PrecalificacionServiceTest.java`.

## Fuera de alcance (no tocar)

- `recalcular()` del reprocesamiento: es de la UT-CU26-BE-03.
- La ejecución en segundo plano y el reintento (UT-CU03-BE-06).
- Mostrar el puntaje en pantalla: lo muestran el CU06 (lista priorizada) y el CU07 (expediente).

## Resultado esperado

Una postulación `pendiente` con la IA disponible termina `calificada` con un solo puntaje vigente; si algo falla, no queda un puntaje a medias ni cambia el estado.

## Validación

1. `./mvnw -o test -Dtest=PrecalificacionServiceTest` en verde (17 casos).
2. Con la IA y Postgres reales, `precalificar(id)` deja la fila vigente en `puntaje_postulacion` con la versión `v1.0` y la postulación en `calificada`.
