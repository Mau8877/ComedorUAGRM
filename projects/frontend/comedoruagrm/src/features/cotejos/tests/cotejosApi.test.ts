import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import { cotejosKeys, listarCotejosApi, listarRegistrosApi, obtenerDatosCotejoApi, registrarCotejoApi } from '../api'

const META = { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 }

describe('cotejosApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('obtenerDatosCotejoApi hace GET /planillas/{codigo}/cotejos/datos', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { registrosDigitales: 3 } })

    const datos = await obtenerDatosCotejoApi('p1')

    expect(get).toHaveBeenCalledWith('/planillas/p1/cotejos/datos')
    expect(datos.registrosDigitales).toBe(3)
  })

  it('listarRegistrosApi manda page, pageSize y sort, y omite el sort vacío', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarRegistrosApi('p1', { page: 2, pageSize: 10, sort: '-apellidos' })
    await listarRegistrosApi('p1', { page: 1, pageSize: 20 })

    expect(get).toHaveBeenNthCalledWith(1, '/planillas/p1/cotejos/registros', {
      params: { page: 2, pageSize: 10, sort: '-apellidos' },
    })
    expect(get).toHaveBeenNthCalledWith(2, '/planillas/p1/cotejos/registros', { params: { page: 1, pageSize: 20 } })
  })

  it('listarCotejosApi hace GET /planillas/{codigo}/cotejos con la paginación', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarCotejosApi('p1', { page: 1, pageSize: 20, sort: 'creadoEn' })

    expect(get).toHaveBeenCalledWith('/planillas/p1/cotejos', { params: { page: 1, pageSize: 20, sort: 'creadoEn' } })
  })

  it('registrarCotejoApi hace POST con el cuerpo y devuelve el cotejo', async () => {
    const cuerpo = { totalRegistros: 10, coincidencias: 10, discrepancias: 0, resultado: 'CONFORME' as const }
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { codigo: 'c1', ...cuerpo } })

    const cotejo = await registrarCotejoApi('p1', cuerpo)

    expect(post).toHaveBeenCalledWith('/planillas/p1/cotejos', cuerpo)
    expect(cotejo.codigo).toBe('c1')
  })

  it('las claves de caché empiezan por el módulo y distinguen datos, registros y listado', () => {
    const params = { page: 1, pageSize: 20 }

    expect(cotejosKeys.datos('p1')).toEqual(['cotejos', 'datos', 'p1'])
    expect(cotejosKeys.registrosDe('p1', params)).toEqual(['cotejos', 'registros', 'p1', params])
    expect(cotejosKeys.list('p1', params)).toEqual(['cotejos', 'list', 'p1', params])
    expect(cotejosKeys.list('p1', params).slice(0, 2)).toEqual(cotejosKeys.lists())
  })
})
