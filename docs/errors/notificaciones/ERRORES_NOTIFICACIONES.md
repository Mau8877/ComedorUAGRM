# Errores — Notificaciones (`NOT`)

Errores de la bandeja de avisos del usuario (CU21 / NOTIFICACIONES_WEB).

## `ERR_NOT_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El aviso no existe o no pertenece al usuario autenticado (no se distingue, para no filtrar datos de otros).
- **Dónde se lanza:** `BandejaNotificacionesService.marcarLeida()`

## `ERR_NOT_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Se mandó un `sort` que la bandeja no soporta (solo `fecha` o `-fecha`).
- **Dónde se lanza:** `BandejaNotificacionesService.listar()`

