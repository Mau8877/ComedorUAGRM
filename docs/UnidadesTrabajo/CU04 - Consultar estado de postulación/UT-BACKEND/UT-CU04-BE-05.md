# UT-CU04-BE-05 — Código de postulación en los avisos de la bandeja

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-05` |
| **Nombre** | Código de postulación en los avisos de la bandeja |
| **Caso de uso** | CU04-BE |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (regla de dependencia), [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `BandejaNotificacionesService`, `NotificacionController` y `NotificacionResponse`.

## Solicitud de negocio

Si el postulante recibe un aviso de un cambio de estado, al tocarlo debe llegar directo al detalle de esa postulación (A6, CA10).

## Objetivo

Cada aviso de la bandeja informa, cuando corresponde, el código público de la postulación a la que apunta.

## Alcance

- `NotificacionResponse`: agregar el campo nulable `postulacionCodigo` (`UUID`), al final del `record`. Los avisos sin postulación (comunicados generales) lo devuelven `null`.
- `NotificacionRepository`/`BandejaNotificacionesService` (módulo `notificaciones`): resolver el código con SQL propio sobre `notificacion.postulacion_id` → `postulacion.codigo_postulacion` en la misma consulta del listado (sin N+1). **No** se importa nada de `features/postulaciones/`.
- Push (para la app móvil, `UT-CU04-MOB-04`): incluir también `postulacionCodigo` en el `data` del mensaje FCM cuando el aviso tiene postulación, junto a `codigoNotificacion` y `evento`; sin postulación, no se agrega. Mismo SQL propio, sin importar `features/postulaciones/`.
- Actualizar los tests existentes de la bandeja y agregar uno: un aviso con `postulacion_id` devuelve su código; uno sin postulación, `null`. El cambio es compatible hacia atrás (campo extra).
- Revisar si el contrato HTTP de la bandeja está documentado en alguna ficha o archivo de `docs/` y actualizarlo.

Archivos esperados (tocar): `NotificacionResponse`, repository y service de la bandeja, y sus tests.

## Fuera de alcance (no tocar)

- Crear avisos nuevos, marcar como leídos o cambiar los eventos (CU-EXTRA-01).
- Que el CU-02 cree avisos al enviar (es de esa persona; deben guardar `postulacion_id`).
- Cualquier navegación en el frontend (UT-CU04-FE-04).

## Resultado esperado

La bandeja entrega el dato necesario para abrir el detalle desde un aviso.

## Validación

1. `./mvnw test` pasa.
2. Un aviso de devolución (CU-19) de una postulación devuelve el código de esa postulación.
3. `./mvnw checkstyle:check` sin errores.
