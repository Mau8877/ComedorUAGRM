// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md y docs/pendientes/NOTIFICACIONES_WEB.md.
export const notificacionesKeys = {
  all: ['notificaciones'] as const,
  lists: () => [...notificacionesKeys.all, 'list'] as const,
  list: (params: { pageSize: number }) => [...notificacionesKeys.lists(), params] as const,
  resumen: () => [...notificacionesKeys.all, 'resumen'] as const,
}
