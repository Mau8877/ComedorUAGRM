import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorDashboard } from '../api/mapearErrorDashboard'

describe('mapearErrorDashboard', () => {
  it('ERR_DAS_01: la selección ya no existe', () => {
    expect(mapearErrorDashboard(new ApiError('ERR_DAS_01', 'x'))).toEqual({
      mensaje: 'La gestión o la convocatoria elegida ya no existe.',
      seleccionInexistente: true,
    })
  })

  it('ERR_DAS_02: no hay datos para exportar', () => {
    expect(mapearErrorDashboard(new ApiError('ERR_DAS_02', 'x'))).toEqual({
      mensaje: 'No hay datos para exportar.',
      sinDatos: true,
    })
  })

  it('ERR_AUTH_07: sin permiso', () => {
    expect(mapearErrorDashboard(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe(
      'No tienes permiso para ver el dashboard.',
    )
  })

  it('otro error del backend deja su mensaje de negocio', () => {
    expect(mapearErrorDashboard(new ApiError('ERR_SYS_05', 'Parámetro inválido')).mensaje).toBe('Parámetro inválido')
  })

  it('un error sin respuesta del servidor es de red y permite reintentar', () => {
    const error = new AxiosError('Network Error')

    expect(mapearErrorDashboard(error)).toMatchObject({ red: true })
  })

  it('un error desconocido da un mensaje genérico', () => {
    expect(mapearErrorDashboard(new Error('boom')).mensaje).not.toContain('boom')
  })
})
