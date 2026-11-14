import { useState } from 'react'

import type { NotificationItem, NotificationsController, NotificationsPageMeta } from '@/components/layout'

const PAGE_SIZE = 10
// * Delay artificial para que la carga inicial (al abrir el panel) y
// * "Mostrar más" se sientan como un GET real -- sin esto los estados de
// * loading/isLoadingMore nunca llegarían a verse.
const FETCH_DELAY_MS = 450

// Simula, client-side, lo que en una feature real sería un hook de api/
// con TanStack Query (useNotificacionesQuery con `page`/`pageSize`, más
// mutations de marcar leída/leer todo/eliminar con su invalidación de
// queryKey correspondiente -- ver TANSTACK_QUERY_FRONTEND.md). Acá alcanza
// con useState + setTimeout porque es solo para verificar visualmente
// NotificationsMenu, mismo criterio que el resto de features/pruebas_layout/.
export function useMockNotifications(initial: NotificationItem[]): NotificationsController {
  const [allItems, setAllItems] = useState(initial)
  const [loadedCount, setLoadedCount] = useState(0)
  const [unreadCount, setUnreadCount] = useState(
    () => initial.filter((item) => !item.leida).length,
  )
  const [hasFetchedOnce, setHasFetchedOnce] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isLoadingMore, setIsLoadingMore] = useState(false)

  const items = allItems.slice(0, loadedCount)
  const totalItems = allItems.length
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const meta: NotificationsPageMeta | undefined = hasFetchedOnce
    ? { page: Math.max(1, Math.ceil(loadedCount / PAGE_SIZE)), pageSize: PAGE_SIZE, totalItems, totalPages }
    : undefined

  function handleOpenChange(open: boolean) {
    // * "Al darle clic al botón" -- se pide la primera página recién acá,
    // * no antes (no hay fetch al montar el layout), y solo una vez: si ya
    // * se cargó, reabrir el panel no vuelve a pedir el GET.
    if (!open || hasFetchedOnce) return

    setIsLoading(true)
    window.setTimeout(() => {
      setLoadedCount(Math.min(PAGE_SIZE, allItems.length))
      setHasFetchedOnce(true)
      setIsLoading(false)
    }, FETCH_DELAY_MS)
  }

  function handleLoadMore() {
    if (loadedCount >= totalItems) return

    setIsLoadingMore(true)
    window.setTimeout(() => {
      setLoadedCount((current) => Math.min(current + PAGE_SIZE, totalItems))
      setIsLoadingMore(false)
    }, FETCH_DELAY_MS)
  }

  function handleMarkAsRead(id: string) {
    const target = allItems.find((item) => item.id === id)
    if (target && !target.leida) {
      setUnreadCount((count) => Math.max(0, count - 1))
    }
    setAllItems((current) => current.map((item) => (item.id === id ? { ...item, leida: true } : item)))
  }

  function handleMarkAllAsRead() {
    setAllItems((current) => current.map((item) => ({ ...item, leida: true })))
    setUnreadCount(0)
  }

  function handleDeleteAll() {
    setAllItems([])
    setLoadedCount(0)
    setUnreadCount(0)
  }

  return {
    items,
    meta,
    isLoading,
    isLoadingMore,
    unreadCount,
    onOpenChange: handleOpenChange,
    onLoadMore: handleLoadMore,
    onMarkAsRead: handleMarkAsRead,
    onMarkAllAsRead: handleMarkAllAsRead,
    onDeleteAll: handleDeleteAll,
  }
}
