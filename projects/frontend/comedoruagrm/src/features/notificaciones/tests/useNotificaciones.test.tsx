import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { MockInstance } from 'vitest'

import { apiClient } from '@/store'

import { aNotificationItem, useNotificaciones } from '../api/useNotificaciones'

const aviso = (n: number, extra = {}) => ({
  id: `n-${n}`,
  tipo: 'INFO' as const,
  titulo: `Titulo ${n}`,
  detalle: `Detalle ${n}`,
  fecha: '2026-11-06T10:00:00Z',
  leida: false,
  ...extra,
})

function montar(opciones?: { enabled?: boolean }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return renderHook(() => useNotificaciones(opciones), { wrapper })
}

describe('useNotificaciones', () => {
  let get: MockInstance
  let post: MockInstance
  let borrar: MockInstance

  beforeEach(() => {
    get = vi.spyOn(apiClient, 'get').mockImplementation(((ruta: string, config?: { params?: { page?: number } }) => {
      if (ruta === '/notificaciones/me/resumen') return Promise.resolve({ data: { noLeidas: 8 } })
      const pagina = config?.params?.page ?? 1
      return Promise.resolve({
        data: {
          data: pagina === 1 ? [aviso(1), aviso(2)] : [aviso(3)],
          meta: { page: pagina, pageSize: 10, totalItems: 3, totalPages: 2 },
        },
      })
    }) as never)
    post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: null })
    borrar = vi.spyOn(apiClient, 'delete').mockResolvedValue({ data: null })
  })

  afterEach(() => vi.restoreAllMocks())

  it('aNotificationItem pasa el tipo a minúscula y el detalle nulo a undefined', () => {
    expect(aNotificationItem(aviso(1, { tipo: 'WARNING', detalle: null }))).toMatchObject({
      tipo: 'warning',
      detalle: undefined,
    })
  })

  it('el contador sale del resumen y el listado no se pide hasta abrir el panel', async () => {
    const { result } = montar()

    await waitFor(() => expect(result.current.unreadCount).toBe(8))
    expect(get.mock.calls.some((c: unknown[]) => c[0] === '/notificaciones/me')).toBe(false)
    expect(result.current.meta).toBeUndefined()

    act(() => result.current.onOpenChange(true))
    await waitFor(() => expect(result.current.items).toHaveLength(2))
    expect(get).toHaveBeenCalledWith('/notificaciones/me', { params: { page: 1, pageSize: 10, sort: '-fecha' } })
  })

  it('«Mostrar más» agrega la página siguiente sin descartar lo cargado', async () => {
    const { result } = montar()
    act(() => result.current.onOpenChange(true))
    await waitFor(() => expect(result.current.items).toHaveLength(2))

    act(() => result.current.onLoadMore())

    await waitFor(() => expect(result.current.items.map((i) => i.id)).toEqual(['n-1', 'n-2', 'n-3']))
    expect(result.current.meta).toEqual({ page: 2, pageSize: 10, totalItems: 3, totalPages: 2 })
  })

  it('marcar leída, leer todo y eliminar llaman a su endpoint', async () => {
    const { result } = montar()
    await waitFor(() => expect(result.current.unreadCount).toBe(8))

    act(() => result.current.onMarkAsRead('n-1'))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/notificaciones/n-1/leer'))
    act(() => result.current.onMarkAllAsRead())
    await waitFor(() => expect(post).toHaveBeenCalledWith('/notificaciones/leer-todo'))
    act(() => result.current.onDeleteAll())
    await waitFor(() => expect(borrar).toHaveBeenCalledWith('/notificaciones'))
  })

  it('con enabled en false no pide nada (pantallas con su propio controlador)', async () => {
    const { result } = montar({ enabled: false })
    act(() => result.current.onOpenChange(true))

    await new Promise((r) => setTimeout(r, 50))
    expect(get).not.toHaveBeenCalled()
    expect(result.current.unreadCount).toBe(0)
  })
})
