import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import { adjudicacionKeys, obtenerPanelApi, seleccionarFinalistasApi, usePanelAdjudicacion } from '../api'

describe('adjudicacionApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('obtenerPanelApi hace GET /convocatorias/{codigo}/adjudicacion', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { cupos: 10 } })

    expect(await obtenerPanelApi('c-1')).toEqual({ cupos: 10 })
    expect(get).toHaveBeenCalledWith('/convocatorias/c-1/adjudicacion')
  })

  it('seleccionarFinalistasApi hace POST /convocatorias/{codigo}/finalistas con la cantidad y sin cabecera de idempotencia', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { seleccionados: 3 } })

    expect(await seleccionarFinalistasApi('c-1', { cantidad: 3 })).toEqual({ seleccionados: 3 })
    expect(post).toHaveBeenCalledWith('/convocatorias/c-1/finalistas', { cantidad: 3 })
  })

  it('seleccionarFinalistasApi sin cantidad manda un cuerpo vacío (el backend usa la configurada)', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { seleccionados: 5 } })

    await seleccionarFinalistasApi('c-1', {})

    expect(post).toHaveBeenCalledWith('/convocatorias/c-1/finalistas', {})
  })

  it('adjudicacionKeys sigue el esquema [módulo, tipo, código]', () => {
    expect(adjudicacionKeys.all).toEqual(['adjudicacion'])
    expect(adjudicacionKeys.panels()).toEqual(['adjudicacion', 'panel'])
    expect(adjudicacionKeys.panel('c-1')).toEqual(['adjudicacion', 'panel', 'c-1'])
  })
})

describe('usePanelAdjudicacion', () => {
  afterEach(() => vi.restoreAllMocks())

  function envoltorio() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
  }

  it('no consulta mientras no haya convocatoria elegida', () => {
    const get = vi.spyOn(apiClient, 'get')

    const { result } = renderHook(() => usePanelAdjudicacion(undefined), { wrapper: envoltorio() })

    expect(result.current.fetchStatus).toBe('idle')
    expect(get).not.toHaveBeenCalled()
  })

  it('con una convocatoria consulta su panel', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { cupos: 4 } })

    const { result } = renderHook(() => usePanelAdjudicacion('c-1'), { wrapper: envoltorio() })

    await waitFor(() => expect(result.current.data).toEqual({ cupos: 4 }))
  })
})
