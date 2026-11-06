import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorPriorizacion } from '../api/mapearErrorPriorizacion'

describe('mapearErrorPriorizacion', () => {
  it('ERR_PRI_01: la convocatoria ya no existe', () => {
    expect(mapearErrorPriorizacion(new ApiError('ERR_PRI_01', 'x'))).toEqual({
      mensaje: 'La convocatoria ya no existe. Elige otra.',
      convocatoriaInexistente: true,
    })
  })

  it('ERR_PRI_03: no hay nada que exportar', () => {
    expect(mapearErrorPriorizacion(new ApiError('ERR_PRI_03', 'x'))).toEqual({
      mensaje: 'No hay postulaciones para exportar.',
      listaVacia: true,
    })
  })

  it('ERR_AUTH_07: sin permiso', () => {
    expect(mapearErrorPriorizacion(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe(
      'No tienes permiso para ver la lista priorizada.',
    )
  })

  it('los errores de validación muestran el mensaje del backend', () => {
    expect(mapearErrorPriorizacion(new ApiError('ERR_PRI_02', 'filtro no soportado')).mensaje).toBe('filtro no soportado')
  })

  it('un error de red (sin respuesta) ofrece reintentar', () => {
    expect(mapearErrorPriorizacion(new AxiosError('Network Error'))).toMatchObject({ red: true })
  })

  it('cualquier otro error es genérico y no expone detalles', () => {
    expect(mapearErrorPriorizacion(new ApiError('ERR_SYS_00', 'detalle interno')).mensaje).toBe(
      'Ocurrió un error inesperado. Inténtalo de nuevo.',
    )
    expect(mapearErrorPriorizacion(new Error('boom')).mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })
})
