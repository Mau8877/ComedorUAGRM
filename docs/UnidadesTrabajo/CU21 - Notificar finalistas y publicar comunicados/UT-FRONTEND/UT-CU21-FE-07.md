# UT-CU21-FE-07 — Bandeja real de avisos en los layouts (sin mocks)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-FE-07` |
| **Nombre** | Bandeja real de avisos en los layouts (sin mocks) |
| **Caso de uso** | CU21-FE |
| **Depende de** | `UT-CU21-BE-09` |

## Antes de implementar

- [`docs/pendientes/NOTIFICACIONES_WEB.md`](../../../pendientes/NOTIFICACIONES_WEB.md) (comportamiento esperado, `queryKey` y polling) y [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md).
- Como referencia: `F/components/layout/NotificationsMenu.tsx`, `types.ts` (`NotificationsController`) y `F/features/pruebas_layout/api/useMockNotifications.ts`.

## Solicitud de negocio

El Postulante y el administrador ven sus avisos reales, no datos simulados.

## Objetivo

La campanita muestra los avisos del usuario con la API real, en `AdminLayout` y `EstudianteLayout`.

## Alcance

- `F/features/notificaciones/api/`: `notificacionesApi.ts`, `keys.ts` (`['notificaciones','list',{page,pageSize}]` y `['notificaciones','resumen']`) y `useNotificaciones`, que arma el `NotificationsController` que ya espera `NotificationsMenu`: primer pedido solo al abrir el panel, páginas de 10 con «Mostrar más», contador aparte con `refetchInterval` (polling), marcar leída, leer todo y eliminar todas, invalidando listado y resumen. `tipo` en minúscula para el ícono.
- `AdminLayout` y `EstudianteLayout` usan `useNotificaciones` cuando no reciben su propio controlador (`notifications`); las pantallas de `pruebas_layout` siguen pasando su mock, porque son un banco de pruebas visual del sistema de diseño (decisión al implementar: no se retira ese mock).
- Tests: el hook mapea la respuesta, pagina, marca leída, lee todo, elimina y no pide nada con `enabled: false`.

Archivos esperados (crear): `notificacionesApi.ts`, `keys.ts`, `useNotificaciones.ts` y sus tests. (Tocar): `AdminLayout.tsx`, `EstudianteLayout.tsx`.

## Fuera de alcance (no tocar)

- El componente visual `NotificationsMenu`.
- La bandeja y el push en la app móvil.

## Resultado esperado

Un aviso creado por el backend aparece en la campanita del destinatario y se puede marcar como leído.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
4. Con el backend y los datos de prueba, la campanita del postulante de prueba muestra su aviso y el contador baja al leerlo.
