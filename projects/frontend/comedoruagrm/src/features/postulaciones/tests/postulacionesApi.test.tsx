import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { AxiosError } from 'axios'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError, apiClient } from '@/store'

import {
  listarMisPostulacionesApi,
  mapearErrorPostulaciones,
  obtenerPostulacionApi,
  postulacionesKeys,
  useMisPostulaciones,
  usePostulacion,
} from '../api'

const META = { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 }

describe('postulacionesApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('listarMisPostulacionesApi manda page, pageSize, filter[gestionAcademica] y sort, y omite lo vacío', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarMisPostulacionesApi({ page: 2, pageSize: 10, gestionAcademica: 'g-1', sort: '-fechaEnvio' })
    await listarMisPostulacionesApi({ page: 1, pageSize: 20, gestionAcademica: '' })

    expect(get).toHaveBeenNthCalledWith(1, '/postulaciones/mias', {
      params: { page: 2, pageSize: 10, 'filter[gestionAcademica]': 'g-1', sort: '-fechaEnvio' },
    })
    expect(get).toHaveBeenNthCalledWith(2, '/postulaciones/mias', { params: { page: 1, pageSize: 20 } })
  })

  it('obtenerPostulacionApi hace GET /postulaciones/{codigo}', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { codigo: 'p-1' } })

    expect(await obtenerPostulacionApi('p-1')).toEqual({ codigo: 'p-1' })
    expect(get).toHaveBeenCalledWith('/postulaciones/p-1')
  })

  it('postulacionesKeys sigue el esquema [módulo, tipo, ...params]', () => {
    expect(postulacionesKeys.all).toEqual(['postulaciones'])
    expect(postulacionesKeys.mias({ page: 1, pageSize: 20 })).toEqual([
      'postulaciones',
      'mias',
      { page: 1, pageSize: 20 },
    ])
    expect(postulacionesKeys.detalle('p-1')).toEqual(['postulaciones', 'detalle', 'p-1'])
  })
})

function envoltorio() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, wrapper }
}

describe('hooks de postulaciones', () => {
  afterEach(() => vi.restoreAllMocks())

  it('usePostulacion no consulta mientras no haya código', () => {
    const get = vi.spyOn(apiClient, 'get')
    const { wrapper } = envoltorio()

    const { result } = renderHook(() => usePostulacion(undefined), { wrapper })

    expect(result.current.fetchStatus).toBe('idle')
    expect(get).not.toHaveBeenCalled()
  })

  it('useMisPostulaciones y usePostulacion llaman a su ruta con sus parámetros', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })
    const { wrapper } = envoltorio()

    const lista = renderHook(() => useMisPostulaciones({ page: 1, pageSize: 20, sort: '-fechaEnvio' }), { wrapper })
    const detalle = renderHook(() => usePostulacion('p-1'), { wrapper })

    await waitFor(() => {
      expect(lista.result.current.isSuccess).toBe(true)
      expect(detalle.result.current.isSuccess).toBe(true)
    })
    expect(get.mock.calls.map((llamada) => llamada[0])).toEqual(
      expect.arrayContaining(['/postulaciones/mias', '/postulaciones/p-1']),
    )
  })

  it('no hay mutaciones: ninguna escritura sale de la feature', () => {
    const post = vi.spyOn(apiClient, 'post')
    const put = vi.spyOn(apiClient, 'put')
    const { wrapper } = envoltorio()

    renderHook(() => useMisPostulaciones({ page: 1, pageSize: 20 }), { wrapper })

    expect(post).not.toHaveBeenCalled()
    expect(put).not.toHaveBeenCalled()
  })
})

describe('mapearErrorPostulaciones', () => {
  it('ERR_POS_01: la postulación no existe o no es tuya', () => {
    const e = mapearErrorPostulaciones(new ApiError('ERR_POS_01', 'x'))

    expect(e.mensaje).toBe('La postulación no existe o no es tuya')
    expect(e.noEncontrada).toBe(true)
  })

  it('ERR_AUTH_07 y ERR_SYS_00 tienen su texto; el servidor ofrece reintentar', () => {
    expect(mapearErrorPostulaciones(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe('No tienes permiso para esta acción.')
    const servidor = mapearErrorPostulaciones(new ApiError('ERR_SYS_00', 'interno'))
    expect(servidor.mensaje).not.toContain('interno')
    expect(servidor.red).toBe(true)
  })

  it('cualquier otro código usa el mensaje del backend', () => {
    expect(mapearErrorPostulaciones(new ApiError('ERR_XX_99', 'Mensaje del backend')).mensaje).toBe(
      'Mensaje del backend',
    )
  })

  it('un error de conexión y un 5xx ofrecen reintentar; lo desconocido da el mensaje genérico', () => {
    const conexion = mapearErrorPostulaciones(new AxiosError('Network Error'))
    const respuesta = { status: 502, statusText: '', headers: {}, config: {} as never, data: null }
    const cinco = mapearErrorPostulaciones(new AxiosError('Bad Gateway', '502', undefined, null, respuesta))

    expect(conexion.red).toBe(true)
    expect(conexion.mensaje).toContain('No se pudo conectar')
    expect(cinco.red).toBe(true)
    expect(mapearErrorPostulaciones(new Error('raro')).mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })
})
