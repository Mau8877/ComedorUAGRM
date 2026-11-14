import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import { dashboardKeys } from '../api/keys'
import { useDescargarDashboard, useFiltrosDashboard, useIndicadores } from '../api/useDashboard'

function envoltorio() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const Envoltorio = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, Envoltorio }
}

describe('hooks del dashboard', () => {
  afterEach(() => vi.restoreAllMocks())

  it('las keys siguen el patrón [dashboard, tipo, params]', () => {
    expect(dashboardKeys.filtros()).toEqual(['dashboard', 'filtros'])
    expect(dashboardKeys.indicadores({ gestion: 'g' })).toEqual(['dashboard', 'indicadores', { gestion: 'g' }])
  })

  it('useFiltrosDashboard guarda su resultado bajo su key', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { gestiones: [] } })
    const { queryClient, Envoltorio } = envoltorio()

    const { result } = renderHook(() => useFiltrosDashboard(), { wrapper: Envoltorio })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(queryClient.getQueryData(dashboardKeys.filtros())).toEqual({ gestiones: [] })
  })

  it('useIndicadores usa la key de su selección y no consulta si está deshabilitado', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { sinDatos: false } })
    const { queryClient, Envoltorio } = envoltorio()

    const { result } = renderHook(() => useIndicadores({ gestion: 'g1' }), { wrapper: Envoltorio })
    renderHook(() => useIndicadores({ gestion: 'g2' }, false), { wrapper: Envoltorio })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(queryClient.getQueryData(dashboardKeys.indicadores({ gestion: 'g1' }))).toEqual({ sinDatos: false })
    expect(get).toHaveBeenCalledTimes(1)
  })

  it('useDescargarDashboard no guarda el PDF en la caché', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({ data: new Blob(['%PDF']), headers: {} })
    const { queryClient, Envoltorio } = envoltorio()

    const { result } = renderHook(() => useDescargarDashboard(), { wrapper: Envoltorio })
    result.current.mutate({ gestion: 'g1' })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0)
    expect(result.current.data?.nombre).toBe('indicadores.pdf')
  })
})
