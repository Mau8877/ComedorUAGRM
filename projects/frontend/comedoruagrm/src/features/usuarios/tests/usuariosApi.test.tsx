import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const cliente = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), patch: vi.fn() }))
vi.mock('@/store', () => ({ apiClient: cliente, ApiError: class ApiError extends Error {} }))

import {
  useCambiarRol,
  useCrearUsuario,
  useDesactivarUsuario,
  useEditarUsuario,
  useReactivarUsuario,
  useUsuarios,
  usuariosKeys,
} from '../api'

const pagina = { data: [], meta: { page: 1, pageSize: 20, totalItems: 0, totalPages: 1 } }

function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, wrapper }
}

const nueva = { nombres: 'A', apellidos: 'B', ci: '1', correo: 'a@b.co', rol: 'DIRECTIVO' as const }

describe('useUsuarios', () => {
  beforeEach(() => {
    cliente.get.mockReset()
    cliente.get.mockResolvedValue({ data: pagina })
  })

  it('manda page, pageSize, filter[rol], filter[activo], search y sort', async () => {
    const { wrapper } = montar()
    const { result } = renderHook(
      () => useUsuarios({ page: 2, pageSize: 10, search: 'ana', rol: 'DIRECTIVO', activo: false, sort: '-ci' }),
      { wrapper },
    )

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(cliente.get).toHaveBeenCalledWith('/usuarios', {
      params: { page: 2, pageSize: 10, search: 'ana', 'filter[rol]': 'DIRECTIVO', 'filter[activo]': false, sort: '-ci' },
    })
  })

  it('omite los parámetros vacíos', async () => {
    const { wrapper } = montar()
    renderHook(() => useUsuarios({ page: 1, pageSize: 20, rol: 'DIRECTIVO' }), { wrapper })

    await waitFor(() => expect(cliente.get).toHaveBeenCalled())
    expect(cliente.get).toHaveBeenCalledWith('/usuarios', { params: { page: 1, pageSize: 20, 'filter[rol]': 'DIRECTIVO' } })
  })
})

describe('mutaciones', () => {
  beforeEach(() => {
    Object.values(cliente).forEach((fn) => fn.mockReset())
    cliente.post.mockResolvedValue({ data: { codigo: 'u1' } })
    cliente.patch.mockResolvedValue({ data: { codigo: 'u1' } })
  })

  const casos: [string, () => { mutateAsync: (v: never) => Promise<unknown> }, unknown, 'post' | 'patch', string][] = [
    ['crear', () => useCrearUsuario(), nueva, 'post', '/usuarios'],
    ['editar', () => useEditarUsuario(), { codigo: 'u1', cambios: { nombres: 'Z' } }, 'patch', '/usuarios/u1'],
    ['cambiar rol', () => useCambiarRol(), { codigo: 'u1', rol: 'DIRECTIVO' }, 'post', '/usuarios/u1/cambiar-rol'],
    ['desactivar', () => useDesactivarUsuario(), 'u1', 'post', '/usuarios/u1/desactivar'],
    ['reactivar', () => useReactivarUsuario(), 'u1', 'post', '/usuarios/u1/reactivar'],
  ]

  for (const [nombre, hook, variables, metodo, ruta] of casos) {
    it(`${nombre} llama a ${ruta} e invalida los listados`, async () => {
      const { wrapper, queryClient } = montar()
      const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
      const { result } = renderHook(hook, { wrapper })

      await act(async () => {
        await result.current.mutateAsync(variables as never)
      })

      expect(cliente[metodo].mock.calls[0][0]).toBe(ruta)
      expect(invalidar).toHaveBeenCalledWith({ queryKey: usuariosKeys.lists() })
    })
  }

  it('editar manda solo los campos que cambiaron', async () => {
    const { wrapper } = montar()
    const { result } = renderHook(() => useEditarUsuario(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync({ codigo: 'u1', cambios: { correo: 'nuevo@uagrm.edu.bo' } })
    })

    expect(cliente.patch).toHaveBeenCalledWith('/usuarios/u1', { correo: 'nuevo@uagrm.edu.bo' })
  })

  it('crear no deja la contraseña temporal en la caché de mutaciones', async () => {
    cliente.post.mockResolvedValue({ data: { codigo: 'u1', contrasenaTemporal: 'Secreta-123' } })
    const { wrapper, queryClient } = montar()
    const { result, unmount } = renderHook(() => useCrearUsuario(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync(nueva)
    })
    unmount()

    await waitFor(() => expect(queryClient.getMutationCache().getAll()).toHaveLength(0))
  })
})
