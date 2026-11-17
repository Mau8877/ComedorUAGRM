# UT-CU03-BE-07 — Modelo inicial v1.0 y consistencia entre la IA y el core

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-07` |
| **Nombre** | Modelo inicial v1.0 y consistencia entre la IA y el core |
| **Caso de uso** | CU03-BACKEND |
| **Depende de** | UT-CU03-BE-02 |

## Antes de implementar

[`PUNTAJE_IA.md`](../../../../.claude/rules/backend-ia/PUNTAJE_IA.md) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (nombres de migraciones).

## Solicitud de negocio

Sin una versión del modelo guardada, la IA responde `v1.0` y el core rechaza un puntaje cuya versión no está registrada: ninguna postulación llegaría a `calificada` hasta que la DUBSS guardara su primera versión.

## Objetivo

Una migración que registra `v1.0` con las ponderaciones por defecto de la IA, y la garantía de que ambos lados no se desajusten.

## Alcance

- Migración `V20261121100000__sembrar_modelo_preseleccion_v1_0.sql`: crea `v1.0` **inactiva** (no ocupa el único modelo activo) con diez ponderaciones que suman 100; idempotente.
- `tests/test_consistencia_con_el_core.py` (IA): compara las ponderaciones y la versión de `app/config.py` con las de la migración.
- `ModeloPreseleccionConsultaRepositoryTest`: ejecuta el SQL de la migración (dos veces) y comprueba una sola `v1.0` inactiva con las diez claves y suma 100.

Archivos esperados (crear/tocar): `V20261121100000__sembrar_modelo_preseleccion_v1_0.sql`, `tests/test_consistencia_con_el_core.py`, `ModeloPreseleccionConsultaRepositoryTest.java`.

## Fuera de alcance (no tocar)

- Guardar o cambiar versiones desde la pantalla (CU26).
- Elegir los pesos definitivos: los fija la DUBSS.

## Resultado esperado

En una base nueva existe `v1.0` y una postulación enviada sin ninguna versión guardada se califica con ella.

## Validación

1. `pytest tests/test_consistencia_con_el_core.py` en verde (3 casos).
2. `ModeloPreseleccionConsultaRepositoryTest` en verde con Postgres real.
3. En una base vacía, `select version, activo, count(*) from modelo_preseleccion m join parametro_modelo p on p.modelo_id = m.id group by 1, 2` da `v1.0`, `false`, `10`.
