import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const navigate = vi.hoisted(() => vi.fn())
vi.mock('@tanstack/react-router', () => ({ useNavigate: () => navigate }))
vi.mock('../api/authApi', () => ({ logoutApi: vi.fn() }))

import { logoutApi } from '../api/authApi'
import { authKeys } from '../api/keys'
import { useCerrarSesion } from '../api/useCerrarSesion'

function montar() {
  const queryClient = new QueryClient()
  queryClient.setQueryData(authKeys.usuarioActual, { codigo: 'u1' })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, ...renderHook(() => useCerrarSesion(), { wrapper }) }
}

describe('useCerrarSesion', () => {
  beforeEach(() => {
    navigate.mockReset()
    vi.mocked(logoutApi).mockReset()
  })

  it('revoca la sesión, limpia la caché y lleva a /login', async () => {
    vi.mocked(logoutApi).mockResolvedValue()
    const { result, queryClient } = montar()

    await act(async () => result.current.mutate())

    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: '/login' }))
    expect(logoutApi).toHaveBeenCalledTimes(1)
    expect(queryClient.getQueryData(authKeys.usuarioActual)).toBeUndefined()
  })

  it('si la sesión ya había expirado o falla la red, igual limpia y navega sin error (A10)', async () => {
    vi.mocked(logoutApi).mockRejectedValue(new Error('401'))
    const { result, queryClient } = montar()

    await act(async () => result.current.mutate())

    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: '/login' }))
    expect(result.current.isError).toBe(false)
    expect(queryClient.getQueryData(authKeys.usuarioActual)).toBeUndefined()
  })
})
