import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))

import { listarAsistenciasApi, obtenerMiBecaApi } from '../api'

import { cliente } from './clienteSimulado'

describe('mibecaApi', () => {
  beforeEach(() => cliente.get.mockReset())

  it('obtiene la beca de la sesión sin mandar ningún identificador', async () => {
    cliente.get.mockResolvedValue({ data: { codigo: 'b1' } })

    await expect(obtenerMiBecaApi()).resolves.toEqual({ codigo: 'b1' })
    expect(cliente.get).toHaveBeenCalledWith('/mi-beca')
  })

  it('arma page, pageSize, filter[desde], filter[hasta] y sort con el contrato de listados', async () => {
    cliente.get.mockResolvedValue({ data: { data: [], meta: {} } })

    await listarAsistenciasApi({ page: 2, pageSize: 10, desde: '2026-09-01', hasta: '2026-09-30', sort: 'fecha' })

    expect(cliente.get).toHaveBeenCalledWith('/mi-beca/asistencias', {
      params: { page: 2, pageSize: 10, 'filter[desde]': '2026-09-01', 'filter[hasta]': '2026-09-30', sort: 'fecha' },
    })
  })

  it('omite los parámetros opcionales que no se usan', async () => {
    cliente.get.mockResolvedValue({ data: { data: [], meta: {} } })

    await listarAsistenciasApi({ page: 1, pageSize: 20 })

    expect(cliente.get).toHaveBeenCalledWith('/mi-beca/asistencias', { params: { page: 1, pageSize: 20 } })
  })
})
