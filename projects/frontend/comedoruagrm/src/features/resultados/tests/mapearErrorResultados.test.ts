import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorResultados } from '../api'

describe('mapearErrorResultados', () => {
  it('ERR_PRI_01: la convocatoria no existe y hay que volver a elegir', () => {
    const mapeado = mapearErrorResultados(new ApiError('ERR_PRI_01', 'x'))

    expect(mapeado.mensaje).toBe('La convocatoria no existe o fue eliminada.')
    expect(mapeado.convocatoriaInexistente).toBe(true)
  })

  it('ERR_PUB_01 usa el mensaje del backend (cuántas decisiones faltan) y pide refrescar', () => {
    const mapeado = mapearErrorResultados(
      new ApiError('ERR_PUB_01', 'Faltan 3 decisiones de adjudicación para publicar los resultados'),
    )

    expect(mapeado.mensaje).toBe('Faltan 3 decisiones de adjudicación para publicar los resultados')
    expect(mapeado.refrescar).toBe(true)
  })

  it('ERR_PUB_02: ya estaba publicada, hay que refrescar', () => {
    const mapeado = mapearErrorResultados(new ApiError('ERR_PUB_02', 'x'))

    expect(mapeado.mensaje).toBe('Los resultados de esta convocatoria ya estaban publicados.')
    expect(mapeado.refrescar).toBe(true)
  })

  it('ERR_PUB_03: la convocatoria no está cerrada, hay que refrescar', () => {
    const mapeado = mapearErrorResultados(new ApiError('ERR_PUB_03', 'x'))

    expect(mapeado.mensaje).toBe('La convocatoria debe estar cerrada para publicar sus resultados.')
    expect(mapeado.refrescar).toBe(true)
  })

  it('ERR_PUB_04: los resultados aún no están publicados', () => {
    const mapeado = mapearErrorResultados(new ApiError('ERR_PUB_04', 'x'))

    expect(mapeado.mensaje).toBe('Los resultados aún no están publicados.')
    expect(mapeado.refrescar).toBeUndefined()
  })

  it('traduce el permiso denegado', () => {
    expect(mapearErrorResultados(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe('No tienes permiso para esta acción.')
  })

  it('cualquier otro código del backend muestra su mensaje', () => {
    expect(mapearErrorResultados(new ApiError('ERR_SYS_05', 'No se puede ordenar por «ci»')).mensaje).toBe(
      'No se puede ordenar por «ci»',
    )
  })

  it('un error de red sin respuesta ofrece reintentar', () => {
    const mapeado = mapearErrorResultados(new AxiosError('Network Error'))

    expect(mapeado.red).toBe(true)
    expect(mapeado.mensaje).toBe('No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.')
  })

  it('un error desconocido da el mensaje genérico', () => {
    expect(mapearErrorResultados(new Error('boom')).mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })
})
