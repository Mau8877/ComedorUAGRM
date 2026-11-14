# UT-CU12-BE-08 — Datos de prueba del dashboard

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-08` |
| **Nombre** | Datos de prueba del dashboard |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-01`, `UT-CU12-BE-02` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) y la ficha `UT-CU22-BE-10` (datos de prueba del CU-22, mismo perfil `dev`).
- Revisar los datos de prueba de `dev` que ya existen (convocatoria `[PRUEBA]`, cuentas de prueba) para extenderlos y no duplicarlos.

## Solicitud de negocio

Para ver el dashboard con números reales hace falta sembrar postulaciones, finalistas y adjudicaciones en varios meses y una cuenta de Directivo.

## Objetivo

Con el perfil `dev`, el sistema arranca con datos suficientes para ver el dashboard y la vista queda refrescada.

## Alcance

- Extender el sembrador de datos de prueba del perfil `dev` (solo `dev`, nunca en otros perfiles) para que, sobre la gestión y convocatorias de prueba:
  - haya postulaciones enviadas repartidas en **varios meses** y algunos borradores que no deben contar;
  - haya finalistas y adjudicaciones en más de una convocatoria, una de ellas **con** publicación de resultados y otra **sin** publicar (para ver que no cuenta);
  - exista una **cuenta de Directivo de prueba** (si no existe ya), con su contraseña de prueba documentada solo en los archivos de ejemplo del proyecto.
- Al terminar de sembrar, llamar a `refrescar()` (UT-02) para que la vista refleje los datos.
- Es idempotente: arrancar dos veces no duplica filas.
- Tests (base real, perfil de prueba): tras sembrar, la vista tiene filas en al menos dos meses; el borrador no cuenta; la convocatoria sin publicar no suma adjudicadas.

Archivos esperados (tocar/crear): el sembrador de `dev` y su test.

## Fuera de alcance (no tocar)

- Datos de asistencia o becas (CU-10, CU-23).
- Sembrar datos en producción u otros perfiles.
- Cambiar los datos de prueba que usan otros CU, salvo agregar los necesarios.

## Resultado esperado

Con `dev`, el Directivo de prueba entra y ve cifras en el dashboard.

## Validación

1. `./mvnw test` pasa.
2. Con el backend en `dev`, `GET /api/v1/dashboard/indicadores` con la sesión del Directivo de prueba devuelve totales mayores que cero.
