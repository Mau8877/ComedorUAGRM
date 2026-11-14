# UT-CU19-BE-08 — Datos de prueba de devoluciones en el cargador de desarrollo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-08` |
| **Nombre** | Datos de prueba de devoluciones en el cargador de desarrollo |
| **Caso de uso** | CU19-BE |
| **Depende de** | `UT-CU19-BE-01`, `UT-CU19-BE-02` |

## Antes de implementar

- `B/config/DatosPruebaExpedientes.java` y su test (CU-07) y la regla de que los datos de prueba corren solo con el perfil `dev`.

## Solicitud de negocio

Sin el CU-02 no hay forma de tener postulaciones devueltas para probar la pantalla y el endpoint.

## Objetivo

Con el perfil `dev` hay finalistas listos para devolver y uno ya devuelto con su historial.

## Alcance

- Se amplía el cargador de desarrollo del CU-07 (solo `dev`, nunca producción, idempotente: no duplica si ya cargó) para que haya, sobre la convocatoria `[PRUEBA]`:
  - al menos un finalista «calificada» **con** una auditoría registrada (se puede devolver);
  - al menos un finalista «calificada» **sin** auditorías (devolver da `ERR_DEV_02`);
  - un finalista cuya postulación está «devuelta» y su fila en `postulacion_historial` (estado anterior «calificada», nuevo «devuelta», un administrador de prueba y un motivo de ejemplo).
- Test del cargador: carga una sola vez y deja esos tres casos.

Archivos esperados (tocar): `DatosPruebaExpedientes` y su test (o un cargador hermano si el original queda muy largo).

## Fuera de alcance (no tocar)

- Datos en producción.
- Crear el flujo de reenvío del postulante (CU-02).
- Cambiar los finalistas ya cargados por el CU-06, CU-21 o CU-07 más allá de lo indicado.

## Resultado esperado

Se puede probar todo el CU a mano contra datos de ejemplo.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
