import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const cliente = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() }))
vi.mock('@/store', () => ({ apiClient: cliente, ApiError: class ApiError extends Error {} }))

import {
  convocatoriasKeys,
  gestionesKeys,
  useAbrirConvocatoria,
  useCerrarConvocatoria,
  useConvocatoria,
  useConvocatorias,
  useCrearConvocatoria,
  useCrearGestion,
  useEditarConvocatoria,
  useEditarGestion,
  useEliminarConvocatoria,
  useGestiones,
  useTiposDocumento,
} from '../api'
import type { ConvocatoriaRequest } from '../schemas'

const pagina = { data: [], meta: { page: 1, pageSize: 20, totalItems: 0, totalPages: 1 } }

function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, wrapper }
}

const request: ConvocatoriaRequest = {
  nombre: 'Beca',
  gestionCodigo: 'g1',
  fechaApertura: '2026-08-01',
  fechaCierre: '2026-08-31',
  cupos: null,
  puntajeMinimo: null,
  cantidadFinalistas: null,
  documentos: [{ tipoDocumentoCodigo: 't1', obligatorio: true }],
}

beforeEach(() => {
  Object.values(cliente).forEach((fn) => fn.mockReset())
  cliente.get.mockResolvedValue({ data: pagina })
})

describe('listados', () => {
  it('useConvocatorias manda page, pageSize, search, filter[estado], filter[gestion] y sort', async () => {
    const { wrapper } = montar()
    renderHook(
      () => useConvocatorias({ page: 2, pageSize: 10, search: 'beca', estado: 'ABIERTA', gestion: 'g1', sort: '-fechaCierre' }),
      { wrapper },
    )

    await waitFor(() => expect(cliente.get).toHaveBeenCalled())
    expect(cliente.get).toHaveBeenCalledWith('/convocatorias', {
      params: {
        page: 2,
        pageSize: 10,
        search: 'beca',
        'filter[estado]': 'ABIERTA',
        'filter[gestion]': 'g1',
        sort: '-fechaCierre',
      },
    })
  })

  it('useConvocatorias omite los parámetros vacíos', async () => {
    const { wrapper } = montar()
    renderHook(() => useConvocatorias({ page: 1, pageSize: 20 }), { wrapper })

    await waitFor(() => expect(cliente.get).toHaveBeenCalled())
    expect(cliente.get).toHaveBeenCalledWith('/convocatorias', { params: { page: 1, pageSize: 20 } })
  })

  it('useGestiones y useTiposDocumento piden su ruta con page, pageSize, search y sort', async () => {
    const { wrapper } = montar()
    renderHook(() => useGestiones({ page: 1, pageSize: 100, search: '2026', sort: '-fechaInicio' }), { wrapper })
    renderHook(() => useTiposDocumento({ page: 1, pageSize: 100, sort: 'nombre' }), { wrapper })

    await waitFor(() => expect(cliente.get).toHaveBeenCalledTimes(2))
    expect(cliente.get).toHaveBeenCalledWith('/gestiones-academicas', {
      params: { page: 1, pageSize: 100, search: '2026', sort: '-fechaInicio' },
    })
    expect(cliente.get).toHaveBeenCalledWith('/tipos-documento', { params: { page: 1, pageSize: 100, sort: 'nombre' } })
  })
})

describe('useConvocatoria', () => {
  it('pide el detalle por código', async () => {
    cliente.get.mockResolvedValue({ data: { codigo: 'c1' } })
    const { wrapper } = montar()
    const { result } = renderHook(() => useConvocatoria('c1'), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(cliente.get).toHaveBeenCalledWith('/convocatorias/c1')
  })

  it('sin código no consulta', () => {
    const { wrapper } = montar()
    renderHook(() => useConvocatoria(undefined), { wrapper })

    expect(cliente.get).not.toHaveBeenCalled()
  })
})

describe('mutaciones de convocatoria', () => {
  it('crear hace POST con el cuerpo y refresca los listados', async () => {
    cliente.post.mockResolvedValue({ data: { codigo: 'c1' } })
    const { queryClient, wrapper } = montar()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useCrearConvocatoria(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync(request)
    })

    expect(cliente.post).toHaveBeenCalledWith('/convocatorias', request)
    expect(invalidar).toHaveBeenCalledWith({ queryKey: convocatoriasKeys.lists() })
  })

  it('editar hace PUT con la convocatoria completa y refresca listados y detalle', async () => {
    cliente.put.mockResolvedValue({ data: { codigo: 'c1' } })
    const { queryClient, wrapper } = montar()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useEditarConvocatoria(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync({ codigo: 'c1', request })
    })

    expect(cliente.put).toHaveBeenCalledWith('/convocatorias/c1', request)
    expect(invalidar).toHaveBeenCalledWith({ queryKey: convocatoriasKeys.lists() })
    expect(invalidar).toHaveBeenCalledWith({ queryKey: convocatoriasKeys.detail('c1') })
  })

  it('abrir y cerrar hacen POST a su acción y refrescan', async () => {
    cliente.post.mockResolvedValue({ data: {} })
    const { queryClient, wrapper } = montar()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const abrir = renderHook(() => useAbrirConvocatoria(), { wrapper })
    const cerrar = renderHook(() => useCerrarConvocatoria(), { wrapper })

    await act(async () => {
      await abrir.result.current.mutateAsync('c1')
      await cerrar.result.current.mutateAsync('c2')
    })

    expect(cliente.post).toHaveBeenCalledWith('/convocatorias/c1/abrir')
    expect(cliente.post).toHaveBeenCalledWith('/convocatorias/c2/cerrar')
    expect(invalidar).toHaveBeenCalledWith({ queryKey: convocatoriasKeys.lists() })
  })

  it('eliminar hace DELETE y refresca', async () => {
    cliente.delete.mockResolvedValue({ data: null })
    const { queryClient, wrapper } = montar()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useEliminarConvocatoria(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync('c1')
    })

    expect(cliente.delete).toHaveBeenCalledWith('/convocatorias/c1')
    expect(invalidar).toHaveBeenCalledWith({ queryKey: convocatoriasKeys.lists() })
  })
})

describe('mutaciones de gestión', () => {
  const gestion = { etiqueta: '2026-II', fechaInicio: '2026-07-01', fechaFin: '2026-12-20' }

  it('crear hace POST y refresca todas las listas de gestiones', async () => {
    cliente.post.mockResolvedValue({ data: { codigo: 'g1' } })
    const { queryClient, wrapper } = montar()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useCrearGestion(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync(gestion)
    })

    expect(cliente.post).toHaveBeenCalledWith('/gestiones-academicas', gestion)
    expect(invalidar).toHaveBeenCalledWith({ queryKey: gestionesKeys.all })
  })

  it('editar hace PUT y refresca también las convocatorias (muestran la etiqueta)', async () => {
    cliente.put.mockResolvedValue({ data: { codigo: 'g1' } })
    const { queryClient, wrapper } = montar()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useEditarGestion(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync({ codigo: 'g1', request: gestion })
    })

    expect(cliente.put).toHaveBeenCalledWith('/gestiones-academicas/g1', gestion)
    expect(invalidar).toHaveBeenCalledWith({ queryKey: gestionesKeys.all })
    expect(invalidar).toHaveBeenCalledWith({ queryKey: convocatoriasKeys.lists() })
  })
})

describe('keys', () => {
  it('el módulo va primero y list/detail segundo', () => {
    expect(convocatoriasKeys.list({ page: 1, pageSize: 20 })).toEqual(['convocatorias', 'list', { page: 1, pageSize: 20 }])
    expect(convocatoriasKeys.detail('c1')).toEqual(['convocatorias', 'detail', 'c1'])
    expect(gestionesKeys.list({ page: 1, pageSize: 20 })).toEqual(['gestiones-academicas', 'list', { page: 1, pageSize: 20 }])
  })
})
