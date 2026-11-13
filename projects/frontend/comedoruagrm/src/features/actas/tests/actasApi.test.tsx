import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { AxiosError } from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { resultadosKeys } from '@/features/resultados'
import { ApiError, apiClient } from '@/store'

import {
  actasKeys,
  generarActaApi,
  listarActasApi,
  listarPendientesApi,
  obtenerAccesoActaApi,
  obtenerActaApi,
  obtenerVistaPreviaApi,
  useAccesoActa,
  useActa,
  useActas,
  useActasPendientes,
  useGenerarActa,
  useVistaPreviaActa,
} from '../api'

const META = { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 }

describe('actasApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('listarActasApi manda page, pageSize, search y sort, y omite lo vacío', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarActasApi({ page: 2, pageSize: 10, search: '001', sort: '-numero' })
    await listarActasApi({ page: 1, pageSize: 20, search: '' })

    expect(get).toHaveBeenNthCalledWith(1, '/actas', {
      params: { page: 2, pageSize: 10, search: '001', sort: '-numero' },
    })
    expect(get).toHaveBeenNthCalledWith(2, '/actas', { params: { page: 1, pageSize: 20 } })
  })

  it('listarPendientesApi hace GET /actas/pendientes con la página', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarPendientesApi({ page: 1, pageSize: 20 })

    expect(get).toHaveBeenCalledWith('/actas/pendientes', { params: { page: 1, pageSize: 20 } })
  })

  it('obtenerActaApi y obtenerAccesoActaApi usan la ruta de la convocatoria', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { numero: '001/2026' } })

    expect(await obtenerActaApi('c-1')).toEqual({ numero: '001/2026' })
    await obtenerAccesoActaApi('c-1')

    expect(get).toHaveBeenNthCalledWith(1, '/convocatorias/c-1/acta')
    expect(get).toHaveBeenNthCalledWith(2, '/convocatorias/c-1/acta/descarga')
  })

  it('generarActaApi hace POST /convocatorias/{codigo}/acta sin cuerpo ni idempotencia', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { numero: '001/2026' } })

    expect(await generarActaApi('c-1')).toEqual({ numero: '001/2026' })
    expect(post).toHaveBeenCalledWith('/convocatorias/c-1/acta')
  })

  it('obtenerVistaPreviaApi pide el PDF como blob y devuelve el Blob', async () => {
    const pdf = new Blob(['%PDF-'], { type: 'application/pdf' })
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: pdf })

    expect(await obtenerVistaPreviaApi('c-1')).toBe(pdf)
    expect(get).toHaveBeenCalledWith('/convocatorias/c-1/acta/vista-previa', { responseType: 'blob' })
  })

  it('un error en una respuesta blob se lee como ApiError con su código', async () => {
    const adaptador = (config: InternalAxiosRequestConfig) =>
      Promise.reject(
        new AxiosError('Request failed', 'ERR_BAD_REQUEST', config, null, {
          status: 409,
          statusText: 'Conflict',
          headers: {},
          config,
          data: new Blob([JSON.stringify({ status: 'failed', error: 'ERR_ACT_02', message: 'Ya tiene acta' })]),
        }),
      )
    const original = apiClient.defaults.adapter
    apiClient.defaults.adapter = adaptador

    try {
      const error = await obtenerVistaPreviaApi('c-1').catch((e: unknown) => e)

      expect(error).toBeInstanceOf(ApiError)
      expect((error as ApiError).code).toBe('ERR_ACT_02')
    } finally {
      apiClient.defaults.adapter = original
    }
  })

  it('actasKeys sigue el esquema [módulo, tipo, ...params]', () => {
    expect(actasKeys.all).toEqual(['actas'])
    expect(actasKeys.lists()).toEqual(['actas', 'list'])
    expect(actasKeys.list({ page: 1, pageSize: 20 })).toEqual(['actas', 'list', { page: 1, pageSize: 20 }])
    expect(actasKeys.pendientes({ page: 1, pageSize: 20 })).toEqual(['actas', 'pendientes', { page: 1, pageSize: 20 }])
    expect(actasKeys.detail('c-1')).toEqual(['actas', 'detail', 'c-1'])
  })
})

function envoltorio() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, wrapper }
}

describe('hooks de actas', () => {
  afterEach(() => vi.restoreAllMocks())

  it('useActa no consulta mientras no haya convocatoria elegida', () => {
    const get = vi.spyOn(apiClient, 'get')
    const { wrapper } = envoltorio()

    const { result } = renderHook(() => useActa(undefined), { wrapper })

    expect(result.current.fetchStatus).toBe('idle')
    expect(get).not.toHaveBeenCalled()
  })

  it('useActas, useActasPendientes y useActa llaman a su ruta', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })
    const { wrapper } = envoltorio()

    const actas = renderHook(() => useActas({ page: 1, pageSize: 20, sort: '-generadaEn' }), { wrapper })
    const pendientes = renderHook(() => useActasPendientes({ page: 1, pageSize: 20 }), { wrapper })
    const una = renderHook(() => useActa('c-1'), { wrapper })

    await waitFor(() => {
      for (const r of [actas, pendientes, una]) expect(r.result.current.isSuccess).toBe(true)
    })
    expect(get.mock.calls.map((llamada) => llamada[0])).toEqual(
      expect.arrayContaining(['/actas', '/actas/pendientes', '/convocatorias/c-1/acta']),
    )
  })

  it('useGenerarActa llama a la API e invalida las actas y los resultados', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { numero: '001/2026' } })
    const { queryClient, wrapper } = envoltorio()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')

    const { result } = renderHook(() => useGenerarActa(), { wrapper })
    result.current.mutate('c-1')

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(post).toHaveBeenCalledWith('/convocatorias/c-1/acta')
    const claves = invalidar.mock.calls.map((llamada) => llamada[0]?.queryKey)
    expect(claves).toEqual(expect.arrayContaining([actasKeys.all, resultadosKeys.all]))
  })

  it('useVistaPreviaActa devuelve el Blob y no deja nada en la caché', async () => {
    const pdf = new Blob(['%PDF-'], { type: 'application/pdf' })
    vi.spyOn(apiClient, 'get').mockResolvedValue({ data: pdf })
    const { queryClient, wrapper } = envoltorio()

    const { result } = renderHook(() => useVistaPreviaActa(), { wrapper })
    result.current.mutate('c-1')

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toBe(pdf)
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0)
  })

  it('useAccesoActa pide la dirección al hacer clic y no la guarda en la caché', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { url: 'http://minio/firmada', expiraEn: 'x' } })
    const { queryClient, wrapper } = envoltorio()

    const { result } = renderHook(() => useAccesoActa(), { wrapper })
    expect(get).not.toHaveBeenCalled()
    result.current.mutate('c-1')

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(get).toHaveBeenCalledWith('/convocatorias/c-1/acta/descarga')
    expect(result.current.data?.url).toBe('http://minio/firmada')
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0)
  })
})
