# UT-CU21-BE-04 — Finalistas de prueba en el cargador de desarrollo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-04` |
| **Nombre** | Finalistas de prueba en el cargador de desarrollo |
| **Caso de uso** | CU21-BE |
| **Depende de** | `UT-CU21-BE-02` |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).
- Como referencia: `B/config/DatosPruebaListaPriorizada.java` (CU06, `UT-CU06-BE-03`).

## Solicitud de negocio

Los finalistas los crea el CU-05, que no existe todavía; sin ellos no se puede ver ni probar este CU.

## Objetivo

En desarrollo, la convocatoria `[PRUEBA]` grande tiene finalistas de ejemplo, unos ya notificados y con entrega recibida.

## Alcance

- `DatosPruebaListaPriorizada` (mismo perfil `dev`, misma propiedad de apagado, misma marca `[PRUEBA]`) crea al final de `cargar` los finalistas de la convocatoria grande: los **10 primeros puestos** de su lista priorizada (`posicion` 1 a 10, sobre postulaciones `calificada`).
- Variedad a propósito: 3 con `notificado_en` y `plazo_entrega`, de los cuales 1 con `documentacion_recibida_en`; los otros 7 sin notificar.
- Idempotente (el cargador ya sale si existe la convocatoria `[PRUEBA]`). No cambia el estado de ninguna postulación.
- Un test del cargador con base real: crea los 10 finalistas con las posiciones esperadas y las tres situaciones.

Archivos esperados (tocar): `DatosPruebaListaPriorizada.java` y su test.

## Fuera de alcance (no tocar)

- Avisos y comunicados de ejemplo (UT-12).
- Cambiar el estado de las postulaciones o crear convocatorias nuevas (CU-13, CU-05).
- Cualquier cosa en producción.

## Resultado esperado

Con perfil `dev`, `GET /api/v1/finalistas?filter[convocatoriaId]=...` (UT-10) devuelve los 10 finalistas.

## Validación

1. `./mvnw test` pasa.
2. Arrancar en `dev` sobre base limpia: `select count(*) from finalista` = 10.
