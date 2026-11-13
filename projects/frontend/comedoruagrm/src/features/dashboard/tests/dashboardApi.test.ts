import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import {
  descargarDashboardApi,
  nombreDeArchivo,
  obtenerFiltrosApi,
  obtenerIndicadoresApi,
} from '../api/dashboardApi'

describe('dashboardApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('obtenerFiltrosApi pide GET /dashboard/filtros', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { gestiones: [] } })

    expect(await obtenerFiltrosApi()).toEqual({ gestiones: [] })
    expect(get).toHaveBeenCalledWith('/dashboard/filtros')
  })

  it('obtenerIndicadoresApi manda solo los filtros que tienen valor', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: {} })

    await obtenerIndicadoresApi({})
    await obtenerIndicadoresApi({ gestion: 'g1' })
    await obtenerIndicadoresApi({ gestion: 'g1', convocatoria: 'c1' })
    await obtenerIndicadoresApi({ gestion: '', convocatoria: 'c1' })

    expect(get.mock.calls.map((c) => c[1])).toEqual([
      { params: {} },
      { params: { 'filter[gestion]': 'g1' } },
      { params: { 'filter[gestion]': 'g1', 'filter[convocatoria]': 'c1' } },
      { params: { 'filter[convocatoria]': 'c1' } },
    ])
    expect(get.mock.calls[0][0]).toBe('/dashboard/indicadores')
  })

  it('descargarDashboardApi pide el blob con la misma selección y lee el nombre del servidor', async () => {
    const archivo = new Blob(['%PDF'])
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({
      data: archivo,
      headers: { 'content-disposition': 'attachment; filename="indicadores-2026-ii-20261113.pdf"' },
    })

    const pdf = await descargarDashboardApi({ gestion: 'g1', convocatoria: 'c1' })

    expect(get).toHaveBeenCalledWith('/dashboard/indicadores/pdf', {
      params: { 'filter[gestion]': 'g1', 'filter[convocatoria]': 'c1' },
      responseType: 'blob',
    })
    expect(pdf).toEqual({ archivo, nombre: 'indicadores-2026-ii-20261113.pdf' })
  })

  it('nombreDeArchivo usa uno genérico si el servidor no manda el encabezado', () => {
    expect(nombreDeArchivo(undefined)).toBe('indicadores.pdf')
  })
})
