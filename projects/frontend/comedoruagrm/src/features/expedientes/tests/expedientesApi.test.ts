import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import { accesoDocumentoApi, obtenerExpedienteApi, registrarAuditoriaApi } from '../api/expedientesApi'

describe('expedientesApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('obtenerExpedienteApi hace GET /finalistas/{codigo}/expediente', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { estadoPostulacion: 'calificada' } })

    expect(await obtenerExpedienteApi('f-1')).toEqual({ estadoPostulacion: 'calificada' })
    expect(get).toHaveBeenCalledWith('/finalistas/f-1/expediente')
  })

  it('registrarAuditoriaApi hace POST /finalistas/{codigo}/auditorias con el pedido', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { creada: true } })
    const pedido = { resultado: 'VALIDADO' as const, puntajeAcademico: 25 }

    expect(await registrarAuditoriaApi('f-1', pedido)).toEqual({ creada: true })
    expect(post).toHaveBeenCalledWith('/finalistas/f-1/auditorias', pedido, undefined)
  })

  it('registrarAuditoriaApi manda la clave de idempotencia en el header Idempotency-Key', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { creada: true } })
    const pedido = { resultado: 'VALIDADO' as const, puntajeAcademico: 25 }

    await registrarAuditoriaApi('f-1', pedido, 'clave-1')

    expect(post).toHaveBeenCalledWith('/finalistas/f-1/auditorias', pedido, {
      headers: { 'Idempotency-Key': 'clave-1' },
    })
  })

  it('accesoDocumentoApi hace GET del acceso del documento y devuelve la URL temporal', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { url: 'http://x/firmada', expiraEn: 't' } })

    expect(await accesoDocumentoApi('f-1', 'd-1')).toEqual({ url: 'http://x/firmada', expiraEn: 't' })
    expect(get).toHaveBeenCalledWith('/finalistas/f-1/documentos/d-1/acceso')
  })
})
