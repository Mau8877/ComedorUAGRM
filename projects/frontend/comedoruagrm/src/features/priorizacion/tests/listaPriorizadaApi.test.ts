import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import {
  exportarListaApi,
  listarConvocatoriasApi,
  listarListaPriorizadaApi,
  nombreDeArchivo,
  obtenerResumenApi,
} from '../api/listaPriorizadaApi'

describe('listaPriorizadaApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('listarConvocatoriasApi pide GET /lista-priorizada/convocatorias', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: [{ codigo: 'c' }] })

    expect(await listarConvocatoriasApi()).toEqual([{ codigo: 'c' }])
    expect(get).toHaveBeenCalledWith('/lista-priorizada/convocatorias')
  })

  it('obtenerResumenApi manda filter[convocatoriaId]', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { totalCalificadas: 1, totalPendientes: 2 } })

    await obtenerResumenApi('conv-1')

    expect(get).toHaveBeenCalledWith('/lista-priorizada/resumen', { params: { 'filter[convocatoriaId]': 'conv-1' } })
  })

  it('listarListaPriorizadaApi manda los filtros que hay y nunca sort', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: {} } })

    await listarListaPriorizadaApi({ convocatoriaId: 'c', page: 2, pageSize: 50, carreraId: 'car', search: 'ana' })
    await listarListaPriorizadaApi({ convocatoriaId: 'c', page: 1, pageSize: 20 })

    expect(get.mock.calls[0][1]).toEqual({
      params: { 'filter[convocatoriaId]': 'c', 'filter[carreraId]': 'car', search: 'ana', page: 2, pageSize: 50 },
    })
    expect(get.mock.calls[1][1]).toEqual({ params: { 'filter[convocatoriaId]': 'c', page: 1, pageSize: 20 } })
    expect(JSON.stringify(get.mock.calls)).not.toContain('sort')
  })

  it('exportarListaApi hace POST con los filtros y responseType blob, y lee el nombre del servidor', async () => {
    const archivo = new Blob(['%PDF'])
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({
      data: archivo,
      headers: { 'content-disposition': 'attachment; filename="lista-priorizada-beca-20261106.pdf"' },
    })

    const resultado = await exportarListaApi({ convocatoriaId: 'c', carreraId: 'car', search: 'ana' })

    expect(post).toHaveBeenCalledWith(
      '/lista-priorizada/exportaciones',
      { convocatoriaId: 'c', carreraId: 'car', search: 'ana' },
      { responseType: 'blob' },
    )
    expect(resultado).toEqual({ archivo, nombre: 'lista-priorizada-beca-20261106.pdf' })
  })

  it('exportarListaApi omite los filtros vacíos', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: new Blob([]), headers: {} })

    await exportarListaApi({ convocatoriaId: 'c' })

    expect(post.mock.calls[0][1]).toEqual({ convocatoriaId: 'c' })
  })

  it('nombreDeArchivo cae en un nombre genérico si el servidor no manda el encabezado', () => {
    expect(nombreDeArchivo(undefined)).toBe('lista-priorizada.pdf')
    expect(nombreDeArchivo("attachment; filename*=UTF-8''lista%20x.pdf")).toBe('lista x.pdf')
  })
})
