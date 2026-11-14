import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import { listarCarrerasApi } from './carrerasApi'

describe('listarCarrerasApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('pide GET /carreras?pageSize=200 y devuelve el arreglo', async () => {
    const carreras = [{ codigo: 'c1', nombre: 'Ingeniería', facultad: 'Tecnología' }]
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValue({ data: { data: carreras, meta: { page: 1, pageSize: 200, totalItems: 1, totalPages: 1 } } })

    expect(await listarCarrerasApi()).toEqual(carreras)
    expect(get).toHaveBeenCalledWith('/carreras', { params: { pageSize: 200 } })
  })
})
