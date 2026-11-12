import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import {
  accesoDocumentoApi,
  adjudicarFinalistaApi,
  archivarPostulacionApi,
  devolverPostulacionApi,
  noAdjudicarFinalistaApi,
  obtenerExpedienteApi,
  rechazarPostulacionApi,
  registrarAuditoriaApi,
} from '../api/expedientesApi'

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

  it('devolverPostulacionApi hace POST /finalistas/{codigo}/devolver con el motivo y sin cabecera de idempotencia', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { estado: 'devuelta' } })

    expect(await devolverPostulacionApi('f-1', { motivo: 'Falta el carnet' })).toEqual({ estado: 'devuelta' })
    expect(post).toHaveBeenCalledWith('/finalistas/f-1/devolver', { motivo: 'Falta el carnet' })
  })

  it('archivarPostulacionApi hace POST /finalistas/{codigo}/archivar con el motivo y sin cabecera de idempotencia', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { estado: 'archivada' } })

    expect(await archivarPostulacionApi('f-1', { motivo: 'No entregó' })).toEqual({ estado: 'archivada' })
    expect(post).toHaveBeenCalledWith('/finalistas/f-1/archivar', { motivo: 'No entregó' })
  })

  it('rechazarPostulacionApi hace POST /finalistas/{codigo}/rechazar con el motivo y sin cabecera de idempotencia', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { estado: 'no_adjudicada' } })

    expect(await rechazarPostulacionApi('f-1', { motivo: 'Documentos falsos' })).toEqual({ estado: 'no_adjudicada' })
    expect(post).toHaveBeenCalledWith('/finalistas/f-1/rechazar', { motivo: 'Documentos falsos' })
  })

  it('adjudicarFinalistaApi hace POST /finalistas/{codigo}/adjudicar sin cuerpo ni cabecera de idempotencia', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { resultado: 'ADJUDICADA' } })

    expect(await adjudicarFinalistaApi('f-1')).toEqual({ resultado: 'ADJUDICADA' })
    expect(post).toHaveBeenCalledWith('/finalistas/f-1/adjudicar')
  })

  it('noAdjudicarFinalistaApi hace POST /finalistas/{codigo}/no-adjudicar con el motivo y sin cabecera de idempotencia', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { resultado: 'NO_ADJUDICADA' } })

    expect(await noAdjudicarFinalistaApi('f-1', { motivo: 'No cumple' })).toEqual({ resultado: 'NO_ADJUDICADA' })
    expect(post).toHaveBeenCalledWith('/finalistas/f-1/no-adjudicar', { motivo: 'No cumple' })
  })
})
