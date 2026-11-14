# UT-CU20-BE-01 — La postulación puede archivarse y rechazarse

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-01` |
| **Nombre** | La postulación puede archivarse y rechazarse |
| **Caso de uso** | CU20-BE |
| **Depende de** | — (usa `Postulacion` y `PostulacionHistorial` del CU-19, que ya existen) |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).
- Como referencia: `Postulacion.devolver()` (CU-19) y su test.

## Solicitud de negocio

La DUBSS necesita cerrar una postulación que no procede sin perder su historia.

## Objetivo

`Postulacion` sabe pasar de «calificada» a «archivada» y de «calificada» a «no adjudicada».

## Alcance

- `Postulacion` gana dos métodos: `archivar()` (`calificada` → `archivada`) y `rechazar()` (`calificada` → `no_adjudicada`). Cada uno lanza `IllegalStateException` si el estado no es «calificada» (red de seguridad; la validación de negocio con `ERR_ARC_01` es del `service`, UT-04).
- Verificar que el enum `EstadoPostulacion` y el tipo `estado_postulacion` de la base ya traen `archivada` y `no_adjudicada`; **si faltara alguno, parar y avisar** en vez de crear una migración aquí.
- Tests: cada método cambia el estado desde «calificada» y falla desde cualquier otro estado (pendiente, devuelta, archivada, no adjudicada, adjudicada); una fila de `PostulacionHistorial` con estado nuevo `archivada` o `no_adjudicada` se guarda y se lee con su enum.

Archivos esperados (tocar): `Postulacion` y su test.

## Fuera de alcance (no tocar)

- Reglas de negocio, errores y avisos (UT-04).
- Migraciones, columnas o tablas nuevas.
- Salir de «archivada» o «no adjudicada» (no hay reapertura).
- Setters nuevos en `PostulacionHistorial` (sigue append-only).

## Resultado esperado

Desde el código se puede archivar o rechazar una `Postulacion` calificada y nada más.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
