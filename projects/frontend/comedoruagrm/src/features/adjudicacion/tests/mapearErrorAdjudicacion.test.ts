import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorAdjudicacion } from '../api'

describe('mapearErrorAdjudicacion', () => {
  it('ERR_PRI_01: la convocatoria no existe y hay que volver a elegir', () => {
    const mapeado = mapearErrorAdjudicacion(new ApiError('ERR_PRI_01', 'x'))

    expect(mapeado.mensaje).toBe('La convocatoria no existe o fue eliminada.')
    expect(mapeado.convocatoriaInexistente).toBe(true)
  })

  it('ERR_ADJ_01: resultados publicados, hay que refrescar el panel', () => {
    const mapeado = mapearErrorAdjudicacion(new ApiError('ERR_ADJ_01', 'x'))

    expect(mapeado.mensaje).toBe(
      'Los resultados de esta convocatoria ya se publicaron; no se pueden seleccionar finalistas.',
    )
    expect(mapeado.refrescar).toBe(true)
  })

  it('ERR_ADJ_05: la convocatoria no está lista, hay que refrescar el panel', () => {
    const mapeado = mapearErrorAdjudicacion(new ApiError('ERR_ADJ_05', 'x'))

    expect(mapeado.mensaje).toBe('La convocatoria debe estar cerrada y tener cupos y puntaje mínimo definidos.')
    expect(mapeado.refrescar).toBe(true)
  })

  it('ERR_SYS_01 muestra el mensaje del backend (cantidad inválida o mayor que las disponibles)', () => {
    expect(mapearErrorAdjudicacion(new ApiError('ERR_SYS_01', 'La cantidad (9) supera las disponibles')).mensaje).toBe(
      'La cantidad (9) supera las disponibles',
    )
  })

  it('traduce el permiso denegado', () => {
    expect(mapearErrorAdjudicacion(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe(
      'No tienes permiso para esta acción.',
    )
  })

  it('cualquier otro error da un mensaje genérico en español sin detalles técnicos', () => {
    const mapeado = mapearErrorAdjudicacion(new ApiError('ERR_SYS_00', 'NullPointerException en X.java'))

    expect(mapeado.mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
    expect(mapearErrorAdjudicacion(new Error('x')).mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })

  it('un error de red ofrece reintentar', () => {
    expect(mapearErrorAdjudicacion(new AxiosError('Network Error')).red).toBe(true)
  })
})
