import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient, ApiError } from '@/store'

import {
  becasKeys,
  darDeAltaApi,
  darDeBajaApi,
  listarPadronApi,
  mapearErrorBecas,
  obtenerCandidatoApi,
  obtenerResumenApi,
  reemplazarApi,
  suspenderApi,
} from '../api'
import { columnasPadron, formatearFecha } from '../components'
import { motivoSchema } from '../schemas'

const META = { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 }

describe('becasApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('listarPadronApi manda los filtros con la forma filter[estado] y omite lo vacío', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarPadronApi('conv-1', { page: 2, pageSize: 10, search: 'ana', estado: 'BAJA', sort: '-ci' })
    await listarPadronApi('conv-1', { page: 1, pageSize: 20, search: '', estado: undefined })

    expect(get).toHaveBeenNthCalledWith(1, '/convocatorias/conv-1/padron', {
      params: { page: 2, pageSize: 10, search: 'ana', 'filter[estado]': 'BAJA', sort: '-ci' },
    })
    expect(get).toHaveBeenNthCalledWith(2, '/convocatorias/conv-1/padron', { params: { page: 1, pageSize: 20 } })
  })

  it('usa las rutas del contrato con el código público', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: {} })
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: {} })

    await obtenerResumenApi('c1')
    await obtenerCandidatoApi('b1')
    await darDeAltaApi('c1')
    await darDeBajaApi('b1', 'motivo')
    await suspenderApi('b1', 'otro')
    await reemplazarApi('b1', 'p9')

    expect(get).toHaveBeenNthCalledWith(1, '/convocatorias/c1/padron/resumen')
    expect(get).toHaveBeenNthCalledWith(2, '/becas/b1/reemplazo')
    expect(post).toHaveBeenNthCalledWith(1, '/convocatorias/c1/padron/alta')
    expect(post).toHaveBeenNthCalledWith(2, '/becas/b1/dar-de-baja', { motivo: 'motivo' })
    expect(post).toHaveBeenNthCalledWith(3, '/becas/b1/suspender', { motivo: 'otro' })
    expect(post).toHaveBeenNthCalledWith(4, '/becas/b1/reemplazar', { postulanteCodigo: 'p9' })
  })

  it('las claves de caché empiezan por el módulo y distinguen list, resumen y candidato', () => {
    const params = { page: 1, pageSize: 20 }
    expect(becasKeys.list('c1', params)).toEqual(['becas', 'list', 'c1', params])
    expect(becasKeys.resumen('c1')).toEqual(['becas', 'resumen', 'c1'])
    expect(becasKeys.candidato('b1')).toEqual(['becas', 'candidato', 'b1'])
  })
})

describe('motivoSchema', () => {
  it('exige un motivo y lo recorta', () => {
    expect(motivoSchema.safeParse({ motivo: '' }).success).toBe(false)
    expect(motivoSchema.safeParse({ motivo: '   ' }).success).toBe(false)
    expect(motivoSchema.parse({ motivo: '  Dejó la carrera  ' }).motivo).toBe('Dejó la carrera')
  })

  it('no admite más de 500 caracteres', () => {
    expect(motivoSchema.safeParse({ motivo: 'a'.repeat(500) }).success).toBe(true)
    expect(motivoSchema.safeParse({ motivo: 'a'.repeat(501) }).success).toBe(false)
  })
})

describe('mapearErrorBecas', () => {
  it('traduce los códigos del padrón y marca cuándo hay que refrescar', () => {
    expect(mapearErrorBecas(new ApiError('ERR_BEC_05', 'x'))).toMatchObject({ refrescar: true })
    expect(mapearErrorBecas(new ApiError('ERR_BEC_08', 'x'))).toMatchObject({ refrescar: true })
    expect(mapearErrorBecas(new ApiError('ERR_BEC_07', 'x'))).toEqual({
      mensaje: 'No hay un postulante disponible para ocupar el cupo.',
    })
    expect(mapearErrorBecas(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe('No tienes permiso para esta acción.')
  })

  it('muestra el mensaje del backend en un código desconocido y uno genérico en otro error', () => {
    expect(mapearErrorBecas(new ApiError('ERR_SYS_01', 'El motivo es obligatorio')).mensaje).toBe(
      'El motivo es obligatorio',
    )
    expect(mapearErrorBecas(new Error('boom')).mensaje).toContain('error inesperado')
  })
})

describe('columnasPadron', () => {
  it('solo ordena por becado, ci, carrera, estado y fechaInicio (lo que acepta el backend)', () => {
    const ordenables = columnasPadron
      .filter((c) => c.enableSorting)
      .map((c) => ('accessorKey' in c ? String(c.accessorKey) : c.id))
    expect(ordenables).toEqual(['becado', 'ci', 'carrera', 'estado', 'fechaInicio'])
  })

  it('formatea las fechas de calendario sin pasar por Date', () => {
    expect(formatearFecha('2026-11-10')).toBe('10/11/2026')
    expect(formatearFecha('sin fecha')).toBe('sin fecha')
  })
})
