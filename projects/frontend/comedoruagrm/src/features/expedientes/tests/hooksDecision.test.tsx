import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { adjudicacionKeys } from '@/features/adjudicacion'
import { finalistasKeys } from '@/features/finalistas'
import { apiClient } from '@/store'

import { expedientesKeys, useAdjudicarFinalista, useNoAdjudicarFinalista } from '../api'

describe('hooks de la decisión (CU05)', () => {
  afterEach(() => vi.restoreAllMocks())

  function preparar() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
    return { invalidar, wrapper }
  }

  function clavesInvalidadas(invalidar: { mock: { calls: unknown[][] } }) {
    return invalidar.mock.calls.map((llamada) => (llamada[0] as { queryKey: unknown }).queryKey)
  }

  it('useAdjudicarFinalista adjudica e invalida el expediente, la lista de finalistas y el panel de cupos', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { resultado: 'ADJUDICADA' } })
    const { invalidar, wrapper } = preparar()
    const { result } = renderHook(() => useAdjudicarFinalista(), { wrapper })

    result.current.mutate({ codigo: 'f-1' })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(post).toHaveBeenCalledWith('/finalistas/f-1/adjudicar')
    expect(clavesInvalidadas(invalidar)).toEqual(
      expect.arrayContaining([expedientesKeys.detail('f-1'), finalistasKeys.lists(), adjudicacionKeys.panels()]),
    )
  })

  it('useNoAdjudicarFinalista manda el motivo e invalida las mismas tres claves', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { resultado: 'NO_ADJUDICADA' } })
    const { invalidar, wrapper } = preparar()
    const { result } = renderHook(() => useNoAdjudicarFinalista(), { wrapper })

    result.current.mutate({ codigo: 'f-1', pedido: { motivo: 'No cumple' } })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(post).toHaveBeenCalledWith('/finalistas/f-1/no-adjudicar', { motivo: 'No cumple' })
    expect(clavesInvalidadas(invalidar)).toEqual(
      expect.arrayContaining([expedientesKeys.detail('f-1'), finalistasKeys.lists(), adjudicacionKeys.panels()]),
    )
  })

  it('si la petición falla no invalida nada', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new Error('x'))
    const { invalidar, wrapper } = preparar()
    const { result } = renderHook(() => useAdjudicarFinalista(), { wrapper })

    result.current.mutate({ codigo: 'f-1' })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(invalidar).not.toHaveBeenCalled()
  })
})
