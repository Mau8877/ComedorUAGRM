# UT-CU22-BE-10 — Datos de prueba de publicación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-10` |
| **Nombre** | Datos de prueba de publicación |
| **Caso de uso** | CU22-BE |
| **Depende de** | `UT-CU22-BE-01`, `UT-CU22-BE-02` |

## Antes de implementar

- Las fichas `UT-CU20-BE-07` y `UT-CU05-BE-10` (cargador de datos de desarrollo) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).

## Solicitud de negocio

Para probar la publicación a mano hacen falta convocatorias con distintos estados de decisión.

## Objetivo

El cargador de desarrollo deja convocatorias de ejemplo para probar publicar, el `409` de pendientes y la consulta pública.

## Alcance

- Ampliar el cargador de desarrollo (solo perfil `dev`, idempotente):
  - La convocatoria `[PRUEBA]` queda con todos los finalistas decididos (adjudicados y no adjudicados) y **sin** publicar.
  - Una convocatoria `[PRUEBA] Con pendientes`, cerrada, con al menos un finalista en «calificada».
  - Una convocatoria `[PRUEBA] Publicada`, con fila en `publicacion_resultado` y sus avisos creados.
  - Adjudicados de ejemplo con carrera y facultad cargadas para la lista pública.
- Test del cargador (perfil `dev`): crea una vez y no duplica al ejecutarse de nuevo; no corre fuera de `dev`.

Archivos esperados (tocar): el cargador de desarrollo y su test.

## Fuera de alcance (no tocar)

- Datos fuera del perfil `dev`.
- Reglas de negocio.
- Cargar el acta o el padrón.

## Resultado esperado

Al arrancar en `dev` se puede publicar a mano y probar cada `409` sin preparar datos.

## Validación

1. `./mvnw test` pasa.
2. En `dev`, el resumen de `[PRUEBA]` muestra `puedePublicar = true` y el de «Con pendientes», `false`.
