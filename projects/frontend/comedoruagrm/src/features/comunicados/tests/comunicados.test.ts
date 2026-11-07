import { AxiosError } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError, apiClient } from '@/store'

import { listarComunicadosApi, publicarComunicadoApi } from '../api/comunicadosApi'
import { mapearErrorComunicados } from '../api/mapearErrorComunicados'
import { aPublicarRequest, comunicadoSchema } from '../schemas'

describe('comunicadoSchema', () => {
  const valido = { titulo: 'Nuevo horario', contenido: 'El comedor abre a las 11', destinatarios: 'TODOS' as const }

  it('acepta un comunicado completo para TODOS o FINALISTAS', () => {
    expect(comunicadoSchema.safeParse(valido).success).toBe(true)
    expect(comunicadoSchema.safeParse({ ...valido, destinatarios: 'FINALISTAS' }).success).toBe(true)
  })

  it('pide título y contenido (sin aceptar solo espacios) y no acepta ADJUDICADOS todavía', () => {
    const vacio = comunicadoSchema.safeParse({ ...valido, titulo: '  ', contenido: '' })
    expect(vacio.error?.issues.map((i) => i.message)).toEqual(['El título es obligatorio', 'El contenido es obligatorio'])
    expect(comunicadoSchema.safeParse({ ...valido, destinatarios: 'ADJUDICADOS' }).success).toBe(false)
  })

  it('limita el largo del título a 150 y del contenido a 5000 como el backend', () => {
    expect(comunicadoSchema.safeParse({ ...valido, titulo: 'x'.repeat(151) }).success).toBe(false)
    expect(comunicadoSchema.safeParse({ ...valido, contenido: 'x'.repeat(5001) }).success).toBe(false)
  })

  it('aPublicarRequest recorta los textos y manda la convocatoria solo si hay', () => {
    expect(aPublicarRequest({ ...valido, titulo: ' Hola ' }, 'conv-1')).toEqual({
      titulo: 'Hola',
      contenido: valido.contenido,
      destinatarios: 'TODOS',
      convocatoriaId: 'conv-1',
    })
    expect(aPublicarRequest(valido, undefined)).not.toHaveProperty('convocatoriaId')
  })
})

describe('comunicadosApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('publicarComunicadoApi hace POST /comunicados', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { codigo: 'c', totalDestinatarios: 3 } })
    const pedido = { titulo: 'a', contenido: 'b', destinatarios: 'TODOS' as const }

    expect(await publicarComunicadoApi(pedido)).toEqual({ codigo: 'c', totalDestinatarios: 3 })
    expect(post).toHaveBeenCalledWith('/comunicados', pedido)
  })

  it('listarComunicadosApi pide la página con GET /comunicados', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: {} } })

    await listarComunicadosApi({ page: 2, pageSize: 10 })

    expect(get).toHaveBeenCalledWith('/comunicados', { params: { page: 2, pageSize: 10 } })
  })
})

describe('mapearErrorComunicados', () => {
  it('traduce los códigos de comunicados y oculta lo inesperado', () => {
    expect(mapearErrorComunicados(new ApiError('ERR_COM_01', 'x'))).toContain('No hay destinatarios')
    expect(mapearErrorComunicados(new ApiError('ERR_COM_02', 'x'))).toContain('elegir una convocatoria')
    expect(mapearErrorComunicados(new ApiError('ERR_SYS_01', 'Falta el título'))).toBe('Falta el título')
    expect(mapearErrorComunicados(new ApiError('ERR_SYS_00', 'técnico'))).toBe(
      'Ocurrió un error inesperado. Inténtalo de nuevo.',
    )
    expect(mapearErrorComunicados(new AxiosError('Network Error'))).toContain('No se pudo conectar')
  })
})
