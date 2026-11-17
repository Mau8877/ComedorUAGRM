import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorPlanillas } from '../api'

describe('mapearErrorPlanillas', () => {
  it('ERR_PRI_01: la convocatoria no existe y hay que volver a elegir', () => {
    const e = mapearErrorPlanillas(new ApiError('ERR_PRI_01', 'x'))

    expect(e.mensaje).toBe('La convocatoria no existe o fue eliminada.')
    expect(e.convocatoriaInexistente).toBe(true)
  })

  it('ERR_PLA_01, 02 y 03 explican el periodo inválido', () => {
    expect(mapearErrorPlanillas(new ApiError('ERR_PLA_01', 'x')).mensaje).toBe(
      'La fecha final no puede ser anterior a la inicial.',
    )
    expect(mapearErrorPlanillas(new ApiError('ERR_PLA_02', 'x')).mensaje).toContain('gestión académica')
    expect(mapearErrorPlanillas(new ApiError('ERR_PLA_03', 'x')).mensaje).toContain('31 días')
  })

  it('ERR_PLA_04: sin becas vigentes, refresca el resumen', () => {
    const e = mapearErrorPlanillas(new ApiError('ERR_PLA_04', 'x'))

    expect(e.mensaje).toContain('no tiene becas vigentes')
    expect(e.refrescar).toBe(true)
  })

  it('ERR_PLA_05 y 06: la planilla no existe o su archivo falta', () => {
    expect(mapearErrorPlanillas(new ApiError('ERR_PLA_05', 'x')).refrescar).toBe(true)
    expect(mapearErrorPlanillas(new ApiError('ERR_PLA_06', 'x')).mensaje).toBe(
      'El archivo de la planilla no está disponible. Avisa a soporte.',
    )
  })

  it('ERR_AUTH_07 y ERR_SYS_00 tienen su texto; el servidor ofrece reintentar', () => {
    expect(mapearErrorPlanillas(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe('No tienes permiso para esta acción.')
    const servidor = mapearErrorPlanillas(new ApiError('ERR_SYS_00', 'interno'))
    expect(servidor.mensaje).not.toContain('interno')
    expect(servidor.red).toBe(true)
  })

  it('cualquier otro código usa el mensaje del backend', () => {
    expect(mapearErrorPlanillas(new ApiError('ERR_XX_99', 'Mensaje del backend')).mensaje).toBe('Mensaje del backend')
  })

  it('un error de conexión ofrece reintentar', () => {
    const e = mapearErrorPlanillas(new AxiosError('Network Error'))

    expect(e.red).toBe(true)
    expect(e.mensaje).toContain('No se pudo conectar')
  })

  it('un 5xx sin sobre da un mensaje del servidor y ofrece reintentar', () => {
    const respuesta = { status: 502, statusText: '', headers: {}, config: {} as never, data: null }
    const e = mapearErrorPlanillas(new AxiosError('Bad Gateway', '502', undefined, null, respuesta))

    expect(e.red).toBe(true)
    expect(e.mensaje).toContain('error en el servidor')
  })

  it('cualquier otra cosa da el mensaje genérico', () => {
    expect(mapearErrorPlanillas(new Error('raro')).mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })
})
