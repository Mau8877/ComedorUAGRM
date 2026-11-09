# UT-CU20-BE-07 — Datos de prueba de cierres en el cargador de desarrollo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-07` |
| **Nombre** | Datos de prueba de cierres en el cargador de desarrollo |
| **Caso de uso** | CU20-BE |
| **Depende de** | `UT-CU20-BE-01` |

## Antes de implementar

- `B/config/DatosPruebaExpedientes.java` y su test (CU-07, ampliado por el CU-19) y la regla de que los datos de prueba corren solo con el perfil `dev`.

## Solicitud de negocio

Hace falta tener a la mano finalistas listos para cerrar y ya cerrados para probar la pantalla y los conflictos.

## Objetivo

Con el perfil `dev` hay finalistas «calificada» para archivar o rechazar, uno «archivada» y uno «no adjudicada» con su historial.

## Alcance

- Se amplía el cargador de desarrollo (solo `dev`, nunca producción, idempotente) sobre la convocatoria `[PRUEBA]`:
  - al menos un finalista «calificada» **sin** auditorías (se puede archivar o rechazar igual);
  - un finalista cuya postulación está «archivada» con su fila en `postulacion_historial` (anterior «calificada», nuevo «archivada», un administrador de prueba y un motivo de ejemplo);
  - un finalista «no adjudicada», igual.
- Los finalistas «calificada» con auditoría ya existentes (CU-07/CU-19) se reutilizan; no se duplican.
- Test del cargador: carga una sola vez y deja esos casos.

Archivos esperados (tocar): `DatosPruebaExpedientes` y su test (o un cargador hermano si el original queda muy largo).

## Fuera de alcance (no tocar)

- Datos en producción.
- Marcar resultados como publicados (CU-22).
- Cambiar los finalistas ya cargados por otros CU más allá de lo indicado.

## Resultado esperado

Se puede probar todo el CU a mano contra datos de ejemplo.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
