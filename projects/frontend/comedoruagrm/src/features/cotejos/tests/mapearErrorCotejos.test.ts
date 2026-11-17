import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorCotejos } from '../api'

describe('mapearErrorCotejos', () => {
  it('ERR_COT_01: la planilla no existe y hay que volver a elegir', () => {
    const e = mapearErrorCotejos(new ApiError('ERR_COT_01', 'x'))

    expect(e.mensaje).toBe('La planilla no existe o fue eliminada.')
    expect(e.planillaInexistente).toBe(true)
  })

  it('ERR_COT_02, 03 y 04 explican la regla incumplida', () => {
    expect(mapearErrorCotejos(new ApiError('ERR_COT_02', 'x')).mensaje).toContain('deben sumar el total')
    expect(mapearErrorCotejos(new ApiError('ERR_COT_03', 'x')).mensaje).toContain('resultado')
    expect(mapearErrorCotejos(new ApiError('ERR_COT_04', 'x')).mensaje).toContain('observaciones')
  })

  it('ERR_AUTH_07 avisa que no hay permiso', () => {
    expect(mapearErrorCotejos(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe('No tienes permiso para esta acción.')
  })

  it('ERR_SYS_00 y un 5xx se pueden reintentar', () => {
    expect(mapearErrorCotejos(new ApiError('ERR_SYS_00', 'x')).red).toBe(true)
    const e5xx = new AxiosError('x', 'ERR_BAD_RESPONSE', undefined, undefined, {
      status: 503,
    } as AxiosError['response'])
    expect(mapearErrorCotejos(e5xx).red).toBe(true)
  })

  it('sin respuesta del servidor avisa de la conexión y se puede reintentar', () => {
    const e = mapearErrorCotejos(new AxiosError('Network Error'))

    expect(e.mensaje).toContain('No se pudo conectar')
    expect(e.red).toBe(true)
  })

  it('un error desconocido da el mensaje genérico', () => {
    expect(mapearErrorCotejos(new Error('x')).mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })
})
