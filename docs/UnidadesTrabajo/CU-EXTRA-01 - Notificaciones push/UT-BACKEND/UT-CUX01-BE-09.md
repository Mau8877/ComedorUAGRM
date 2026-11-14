# UT-CUX01-BE-09 — Eventos y plantillas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-09` |
| **Nombre** | Eventos y plantillas |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | UT-CUX01-BE-02 |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#estructura-de-paquetes) (`enums/` para enumeraciones de negocio)

## Solicitud de negocio

Cada evento del sistema (por ejemplo, «resultados publicados») debe tener su propio texto, y agregar uno nuevo debe ser agregar una línea, sin tocar el mecanismo de envío.

## Objetivo

Un catálogo `EventoNotificacion` con plantilla por evento y su renderizado con datos, con un único evento por ahora: `PRUEBA`.

## Alcance

- `B/features/notificaciones/enums/EventoNotificacion.java`: cada constante define `tipo` (`TipoNotificacion`), `titulo` y `mensaje` con marcadores `{clave}`. Constante inicial: `PRUEBA` (`INFO`, «Notificación de prueba», «Hola {nombres}, esta es una prueba del servicio de notificaciones de ComedorU.»).
- `B/features/notificaciones/service/PlantillaNotificacion.java`: `renderizar(evento, datos)` → record `NotificacionRenderizada(tipo, titulo, mensaje)`. Si falta un marcador, lanza `PlantillaIncompletaException` (excepción interna de este módulo, no llega al cliente) que nombra la clave faltante pero no los valores.
- Títulos de hasta 150 caracteres (límite de la columna).
- Cómo agregar un evento (2 líneas en el README del módulo o en el Javadoc del enum): constante nueva + los datos que su plantilla exige. Los eventos reales los agrega el CU que los necesite.
- Tests: `PlantillaNotificacionTest` (sustitución, clave faltante, valores con caracteres especiales sin interpretarse como marcadores).

Archivos esperados (crear): `EventoNotificacion.java`, `PlantillaNotificacion.java`, `PlantillaIncompletaException.java` y su test.

## Fuera de alcance (no tocar)

- Eventos de negocio reales (resultados, comunicados, etc.).
- Plantillas HTML de correo.
- Textos en otros idiomas.

## Resultado esperado

`renderizar(PRUEBA, {nombres: "Ana"})` devuelve el aviso completo; sin `nombres` falla de forma controlada.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
