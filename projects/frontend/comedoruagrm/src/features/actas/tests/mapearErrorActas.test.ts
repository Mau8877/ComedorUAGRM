import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorActas } from '../api'

describe('mapearErrorActas', () => {
  it('ERR_PRI_01: la convocatoria no existe y hay que volver a elegir', () => {
    const e = mapearErrorActas(new ApiError('ERR_PRI_01', 'x'))

    expect(e.mensaje).toBe('La convocatoria no existe o fue eliminada.')
    expect(e.convocatoriaInexistente).toBe(true)
  })

  it('ERR_ACT_01: resultados sin publicar, refresca los pendientes', () => {
    const e = mapearErrorActas(new ApiError('ERR_ACT_01', 'x'))

    expect(e.mensaje).toContain('aún no están publicados')
    expect(e.refrescar).toBe(true)
  })

  it('ERR_ACT_02: ya tiene acta, refresca los pendientes', () => {
    const e = mapearErrorActas(new ApiError('ERR_ACT_02', 'x'))

    expect(e.mensaje).toBe('Esta convocatoria ya tiene acta de adjudicación.')
    expect(e.refrescar).toBe(true)
  })

  it('ERR_ACT_03: todavía no tiene acta', () => {
    expect(mapearErrorActas(new ApiError('ERR_ACT_03', 'x')).mensaje).toBe('Esta convocatoria todavía no tiene acta.')
  })

  it('ERR_ACT_04: el archivo no está disponible y se avisa a soporte', () => {
    expect(mapearErrorActas(new ApiError('ERR_ACT_04', 'x')).mensaje).toBe(
      'El archivo del acta no está disponible. Avisa a soporte.',
    )
  })

  it('ERR_AUTH_07 y ERR_SYS_00 tienen su texto; el servidor ofrece reintentar', () => {
    expect(mapearErrorActas(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe('No tienes permiso para esta acción.')
    const servidor = mapearErrorActas(new ApiError('ERR_SYS_00', 'interno'))
    expect(servidor.mensaje).not.toContain('interno')
    expect(servidor.red).toBe(true)
  })

  it('cualquier otro código usa el mensaje del backend', () => {
    expect(mapearErrorActas(new ApiError('ERR_XX_99', 'Mensaje del backend')).mensaje).toBe('Mensaje del backend')
  })

  it('un error de conexión ofrece reintentar', () => {
    const e = mapearErrorActas(new AxiosError('Network Error'))

    expect(e.red).toBe(true)
    expect(e.mensaje).toContain('No se pudo conectar')
  })

  it('un 5xx sin sobre da un mensaje del servidor y ofrece reintentar', () => {
    const respuesta = { status: 502, statusText: '', headers: {}, config: {} as never, data: null }
    const e = mapearErrorActas(new AxiosError('Bad Gateway', '502', undefined, null, respuesta))

    expect(e.red).toBe(true)
    expect(e.mensaje).toContain('error en el servidor')
  })

  it('un error desconocido da el mensaje genérico', () => {
    expect(mapearErrorActas(new Error('raro')).mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })
})
