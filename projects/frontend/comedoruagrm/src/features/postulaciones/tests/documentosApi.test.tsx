import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { AxiosError } from 'axios'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError, apiClient } from '@/store'

import {
  accesoDocumentoApi,
  adjuntarDocumentoApi,
  eliminarDocumentoApi,
  listarDocumentosApi,
  mapearErrorDocumentos,
  postulacionesKeys,
  reemplazarDocumentoApi,
  useAdjuntarDocumento,
  useEliminarDocumento,
} from '../api'

const archivo = new File([new Uint8Array([1, 2, 3])], 'carnet.pdf', { type: 'application/pdf' })

describe('API de documentos (CU16)', () => {
  afterEach(() => vi.restoreAllMocks())

  it('listarDocumentosApi hace GET /postulaciones/{codigo}/documentos', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { puedeEditar: true } })

    expect(await listarDocumentosApi('p-1')).toEqual({ puedeEditar: true })
    expect(get).toHaveBeenCalledWith('/postulaciones/p-1/documentos')
  })

  it('adjuntarDocumentoApi sube un FormData con tipoDocumento y archivo', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { codigo: 'd-1' } })

    expect(await adjuntarDocumentoApi('p-1', 't-1', archivo)).toEqual({ codigo: 'd-1' })

    const [url, cuerpo] = post.mock.calls[0]
    expect(url).toBe('/postulaciones/p-1/documentos')
    expect(cuerpo).toBeInstanceOf(FormData)
    expect((cuerpo as FormData).get('tipoDocumento')).toBe('t-1')
    expect((cuerpo as FormData).get('archivo')).toBeInstanceOf(File)
  })

  it('reemplazarDocumentoApi hace PUT con solo el archivo', async () => {
    const put = vi.spyOn(apiClient, 'put').mockResolvedValue({ data: { codigo: 'd-1' } })

    await reemplazarDocumentoApi('p-1', 'd-1', archivo)

    const [url, cuerpo] = put.mock.calls[0]
    expect(url).toBe('/postulaciones/p-1/documentos/d-1')
    expect((cuerpo as FormData).get('archivo')).toBeInstanceOf(File)
    expect((cuerpo as FormData).has('tipoDocumento')).toBe(false)
  })

  it('eliminarDocumentoApi hace DELETE y accesoDocumentoApi pide la dirección temporal', async () => {
    const borrar = vi.spyOn(apiClient, 'delete').mockResolvedValue({ data: null })
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { url: 'https://x/firmada', expiraEn: 'e' } })

    await eliminarDocumentoApi('p-1', 'd-1')
    expect(borrar).toHaveBeenCalledWith('/postulaciones/p-1/documentos/d-1')
    expect((await accesoDocumentoApi('p-1', 'd-1')).url).toBe('https://x/firmada')
    expect(get).toHaveBeenCalledWith('/postulaciones/p-1/documentos/d-1/acceso')
  })

  it('postulacionesKeys.documentos sigue el esquema [módulo, tipo, código]', () => {
    expect(postulacionesKeys.documentos('p-1')).toEqual(['postulaciones', 'documentos', 'p-1'])
  })
})

function envoltorio() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { wrapper, invalidar }
}

describe('hooks de documentos', () => {
  afterEach(() => vi.restoreAllMocks())

  it('adjuntar invalida la lista de documentos de esa postulación', async () => {
    vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { codigo: 'd-1' } })
    const { wrapper, invalidar } = envoltorio()
    const { result } = renderHook(() => useAdjuntarDocumento('p-1'), { wrapper })

    result.current.mutate({ tipoDocumento: 't-1', archivo })

    await waitFor(() => expect(invalidar).toHaveBeenCalledWith({ queryKey: ['postulaciones', 'documentos', 'p-1'] }))
  })

  it('eliminar invalida la lista también cuando falla (el documento ya pudo no existir)', async () => {
    vi.spyOn(apiClient, 'delete').mockRejectedValue(new ApiError('ERR_DOC_09', 'no existe'))
    const { wrapper, invalidar } = envoltorio()
    const { result } = renderHook(() => useEliminarDocumento('p-1'), { wrapper })

    result.current.mutate('d-1')

    await waitFor(() => expect(invalidar).toHaveBeenCalledWith({ queryKey: ['postulaciones', 'documentos', 'p-1'] }))
  })
})

describe('mapearErrorDocumentos', () => {
  it('mapea documento o postulación inexistente a «refrescar»', () => {
    expect(mapearErrorDocumentos(new ApiError('ERR_DOC_04', 'x')).refrescar).toBe(true)
    expect(mapearErrorDocumentos(new ApiError('ERR_DOC_09', 'x')).refrescar).toBe(true)
  })

  it('mapea postulación enviada y convocatoria cerrada a «noEditable»', () => {
    expect(mapearErrorDocumentos(new ApiError('ERR_DOC_06', 'x')).noEditable).toBe(true)
    expect(mapearErrorDocumentos(new ApiError('ERR_DOC_07', 'x')).noEditable).toBe(true)
  })

  it('deja pasar el mensaje en español del backend para formato, tamaño y máximo', () => {
    expect(mapearErrorDocumentos(new ApiError('ERR_DOC_03', 'El formato del archivo no está permitido')).mensaje).toBe(
      'El formato del archivo no está permitido',
    )
    expect(mapearErrorDocumentos(new ApiError('ERR_DOC_08', 'Ya adjuntaste el máximo')).mensaje).toBe(
      'Ya adjuntaste el máximo',
    )
  })

  it('un error de conexión permite reintentar y dice que los otros documentos siguen guardados', () => {
    const e = mapearErrorDocumentos(new AxiosError('Network Error'))

    expect(e.red).toBe(true)
    expect(e.mensaje).toContain('siguen guardados')
  })

  it('un 500 es un error del servidor reintentable', () => {
    expect(mapearErrorDocumentos(new ApiError('ERR_SYS_00', 'x')).red).toBe(true)
  })
})
