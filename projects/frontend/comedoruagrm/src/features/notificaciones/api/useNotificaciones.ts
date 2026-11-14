import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import type { NotificationItem, NotificationsController, NotificationsPageMeta } from '@/components/layout'

import { notificacionesKeys } from './keys'
import {
  eliminarTodasApi,
  listarNotificacionesApi,
  marcarLeidaApi,
  marcarTodasLeidasApi,
  obtenerResumenNotificacionesApi,
} from './notificacionesApi'
import type { NotificacionApi } from './notificacionesApi'

const PAGE_SIZE = 10
/** El número de la campanita se vuelve a pedir cada minuto (polling: no hay WebSocket, ver NOTIFICACIONES_WEB.md). */
const REFRESCO_RESUMEN_MS = 60_000

export function aNotificationItem(aviso: NotificacionApi): NotificationItem {
  return {
    id: aviso.id,
    tipo: aviso.tipo.toLowerCase() as NotificationItem['tipo'],
    titulo: aviso.titulo,
    detalle: aviso.detalle ?? undefined,
    fecha: aviso.fecha,
    leida: aviso.leida,
    postulacionCodigo: aviso.postulacionCodigo ?? undefined,
  }
}

/**
 * Arma el `NotificationsController` que espera la campanita del header con la API real (CU21, flujo D):
 * el primer GET del listado se hace recién cuando se abre el panel, se pagina de a 10 con «Mostrar más»
 * (nunca reemplaza lo ya cargado) y el contador de no leídas viene de un endpoint aparte con polling.
 * Marcar como leída no quita el aviso de la lista; «Eliminar» borra todos los avisos del usuario.
 *
 * * `enabled: false` apaga todo pedido (pantallas de prueba con su propio controlador simulado).
 */
export function useNotificaciones({ enabled = true }: { enabled?: boolean } = {}): NotificationsController {
  const queryClient = useQueryClient()
  const [abierto, setAbierto] = useState(false)

  const resumen = useQuery({
    queryKey: notificacionesKeys.resumen(),
    queryFn: obtenerResumenNotificacionesApi,
    enabled,
    refetchInterval: REFRESCO_RESUMEN_MS,
  })

  const lista = useInfiniteQuery({
    queryKey: notificacionesKeys.list({ pageSize: PAGE_SIZE }),
    queryFn: ({ pageParam }) => listarNotificacionesApi(pageParam, PAGE_SIZE),
    initialPageParam: 1,
    getNextPageParam: (ultima) => (ultima.meta.page < ultima.meta.totalPages ? ultima.meta.page + 1 : undefined),
    enabled: enabled && abierto,
  })

  const refrescar = () => queryClient.invalidateQueries({ queryKey: notificacionesKeys.all })

  const marcarLeida = useMutation({ mutationFn: marcarLeidaApi, onSuccess: refrescar })
  const marcarTodas = useMutation({ mutationFn: marcarTodasLeidasApi, onSuccess: refrescar })
  const eliminarTodas = useMutation({ mutationFn: eliminarTodasApi, onSuccess: refrescar })

  const paginas = lista.data?.pages ?? []
  const ultima = paginas[paginas.length - 1]
  const meta: NotificationsPageMeta | undefined = ultima
    ? { ...ultima.meta, page: paginas.length }
    : undefined

  return {
    items: paginas.flatMap((pagina) => pagina.data).map(aNotificationItem),
    meta,
    isLoading: lista.isLoading,
    isLoadingMore: lista.isFetchingNextPage,
    unreadCount: resumen.data?.noLeidas ?? 0,
    onOpenChange: (open) => {
      if (open) setAbierto(true)
    },
    onLoadMore: () => {
      if (lista.hasNextPage && !lista.isFetchingNextPage) void lista.fetchNextPage()
    },
    onMarkAsRead: (id) => marcarLeida.mutate(id),
    onMarkAllAsRead: () => marcarTodas.mutate(),
    onDeleteAll: () => eliminarTodas.mutate(),
  }
}
