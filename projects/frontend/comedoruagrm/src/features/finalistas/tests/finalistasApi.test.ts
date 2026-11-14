import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import { listarFinalistasApi, notificarFinalistasApi, registrarEntregaApi } from '../api/finalistasApi'

describe('finalistasApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('listarFinalistasApi manda solo filter[convocatoriaId], la búsqueda y la paginación, y nunca sort', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: {} } })

    await listarFinalistasApi({ convocatoriaId: 'c', page: 2, pageSize: 50, search: 'ana' })
    await listarFinalistasApi({ convocatoriaId: 'c', page: 1, pageSize: 20 })

    expect(get.mock.calls[0]).toEqual([
      '/finalistas',
      { params: { 'filter[convocatoriaId]': 'c', search: 'ana', page: 2, pageSize: 50 } },
    ])
    expect(get.mock.calls[1][1]).toEqual({ params: { 'filter[convocatoriaId]': 'c', page: 1, pageSize: 20 } })
  })

  it('notificarFinalistasApi hace POST /finalistas/notificaciones con el pedido', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { solicitados: 2, enviados: 2, fallidos: 0 } })
    const pedido = { finalistas: ['a', 'b'], plazoEntrega: '2026-12-01', lugarHorario: 'DUBSS', documentos: 'CI' }

    expect(await notificarFinalistasApi(pedido)).toEqual({ solicitados: 2, enviados: 2, fallidos: 0 })
    expect(post).toHaveBeenCalledWith('/finalistas/notificaciones', pedido)
  })

  it('registrarEntregaApi hace PATCH /finalistas/{codigo}/entrega', async () => {
    const patch = vi.spyOn(apiClient, 'patch').mockResolvedValue({ data: { codigo: 'f-1' } })

    expect(await registrarEntregaApi('f-1', { fechaRecepcion: '2026-11-06' })).toEqual({ codigo: 'f-1' })
    expect(patch).toHaveBeenCalledWith('/finalistas/f-1/entrega', { fechaRecepcion: '2026-11-06' })
  })
})
