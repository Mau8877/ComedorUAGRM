import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import {
  generarPlanillaApi,
  listarPlanillasApi,
  obtenerAccesoPlanillaApi,
  obtenerResumenPlanillaApi,
  obtenerVistaPreviaPlanillaApi,
  planillasKeys,
} from '../api'

const META = { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 }
const PERIODO = { fechaDesde: '2026-09-01', fechaHasta: '2026-09-30' }

describe('planillasApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('obtenerResumenPlanillaApi hace GET /convocatorias/{codigo}/planillas/resumen', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { becadosVigentes: 3 } })

    const resumen = await obtenerResumenPlanillaApi('c1')

    expect(get).toHaveBeenCalledWith('/convocatorias/c1/planillas/resumen')
    expect(resumen.becadosVigentes).toBe(3)
  })

  it('listarPlanillasApi manda page, pageSize y sort, y omite el sort vacío', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarPlanillasApi('c1', { page: 2, pageSize: 10, sort: '-generadaEn' })
    await listarPlanillasApi('c1', { page: 1, pageSize: 20 })

    expect(get).toHaveBeenNthCalledWith(1, '/convocatorias/c1/planillas', {
      params: { page: 2, pageSize: 10, sort: '-generadaEn' },
    })
    expect(get).toHaveBeenNthCalledWith(2, '/convocatorias/c1/planillas', { params: { page: 1, pageSize: 20 } })
  })

  it('generarPlanillaApi hace POST con el periodo y devuelve la planilla', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { codigo: 'p1', totalBecados: 3 } })

    const planilla = await generarPlanillaApi('c1', PERIODO)

    expect(post).toHaveBeenCalledWith('/convocatorias/c1/planillas', PERIODO)
    expect(planilla.codigo).toBe('p1')
  })

  it('obtenerAccesoPlanillaApi hace GET /planillas/{codigo}/descarga', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { url: 'https://minio/x', expiraEn: 'ya' } })

    const acceso = await obtenerAccesoPlanillaApi('p1')

    expect(get).toHaveBeenCalledWith('/planillas/p1/descarga')
    expect(acceso.url).toBe('https://minio/x')
  })

  it('obtenerVistaPreviaPlanillaApi pide el PDF como blob con desde y hasta', async () => {
    const pdf = new Blob(['%PDF'], { type: 'application/pdf' })
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: pdf })

    const respuesta = await obtenerVistaPreviaPlanillaApi('c1', PERIODO)

    expect(get).toHaveBeenCalledWith('/convocatorias/c1/planillas/vista-previa', {
      params: { desde: '2026-09-01', hasta: '2026-09-30' },
      responseType: 'blob',
    })
    expect(respuesta).toBe(pdf)
  })

  it('las claves de caché empiezan por el módulo, distinguen listado y resumen y no mezclan convocatorias', () => {
    expect(planillasKeys.resumen('c1')).toEqual(['planillas', 'resumen', 'c1'])
    expect(planillasKeys.list('c1', { page: 1, pageSize: 20 })).toEqual([
      'planillas',
      'list',
      'c1',
      { page: 1, pageSize: 20 },
    ])
    expect(planillasKeys.list('c1', { page: 1, pageSize: 20 })).not.toEqual(
      planillasKeys.list('c2', { page: 1, pageSize: 20 }),
    )
  })
})
