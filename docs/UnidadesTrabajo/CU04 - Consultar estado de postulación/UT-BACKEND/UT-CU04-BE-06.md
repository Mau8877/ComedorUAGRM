# UT-CU04-BE-06 — Datos de prueba

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-06` |
| **Nombre** | Datos de prueba |
| **Caso de uso** | CU04-BE |
| **Depende de** | — |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).
- Ver cómo cargan sus datos de ejemplo los CU anteriores (por ejemplo, los de publicación de resultados del CU-22) y repetir el mismo mecanismo, sin duplicar lo que ya existe.

## Solicitud de negocio

Para ver la pantalla funcionando hacen falta postulaciones del postulante de prueba en cada estado.

## Objetivo

Con el perfil `dev`, el postulante de prueba tiene postulaciones en los estados que el CU-04 sabe mostrar.

## Alcance

- Extender la carga de datos de ejemplo de la convocatoria `[PRUEBA]` (solo perfil `dev`, idempotente: correrla dos veces no duplica) para que el postulante de prueba tenga, además de lo ya existente, postulaciones en estado `pendiente`, `devuelta` (con observaciones en su historial), `archivada` (con motivo), `calificada`/`adjudicada` sin publicar, finalista con plazo y una `adjudicada` con resultados publicados. Cada una con sus filas de `postulacion_historial` coherentes (estado anterior y nuevo, fechas distintas).
- Usar solo las tablas y columnas existentes; no se crean tablas ni se cambia el diseño lógico.
- Un test de la carga (Testcontainers, se salta sin Docker) confirma que corre dos veces sin error y deja los estados esperados.

Archivos esperados (tocar/crear): el componente de datos de ejemplo `dev` ya usado por otros CU y su test.

## Fuera de alcance (no tocar)

- Migraciones Flyway y datos de otros perfiles.
- Código de producción del CU-04.
- Datos de otros postulantes o convocatorias reales.

## Resultado esperado

Al levantar el stack en `dev` se puede recorrer la consulta con todos los estados.

## Validación

1. `./mvnw test` pasa.
2. Iniciar sesión como el postulante de prueba y llamar a `GET /api/v1/postulaciones/mias` devuelve varias postulaciones con distintos estados.
