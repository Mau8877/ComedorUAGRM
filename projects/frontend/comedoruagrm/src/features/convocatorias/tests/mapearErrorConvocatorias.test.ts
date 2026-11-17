import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorConvocatorias } from '../api'

const error = (code: string) => new ApiError(code, `mensaje de ${code}`)

describe('mapearErrorConvocatorias', () => {
  it('ERR_CV_04 va al campo fecha de cierre', () => {
    expect(mapearErrorConvocatorias(error('ERR_CV_04'))).toEqual({
      mensaje: 'mensaje de ERR_CV_04',
      campo: 'fechaCierre',
    })
  })

  it('ERR_CV_12 va a la etiqueta y ERR_CV_13 a la fecha de fin', () => {
    expect(mapearErrorConvocatorias(error('ERR_CV_12')).campo).toBe('etiqueta')
    expect(mapearErrorConvocatorias(error('ERR_CV_13')).campo).toBe('fechaFin')
  })

  it('los errores de formulario y el de otra abierta muestran el mensaje del backend sin campo', () => {
    for (const code of ['ERR_SYS_01', 'ERR_CV_05', 'ERR_CV_06', 'ERR_CV_07']) {
      expect(mapearErrorConvocatorias(error(code))).toEqual({ mensaje: `mensaje de ${code}` })
    }
  })

  it('inexistente, estado inválido y postulaciones piden refrescar la lista', () => {
    for (const code of ['ERR_CV_01', 'ERR_CV_02', 'ERR_CV_03', 'ERR_CV_08', 'ERR_CV_09', 'ERR_CV_10']) {
      expect(mapearErrorConvocatorias(error(code))).toEqual({ mensaje: `mensaje de ${code}`, refrescar: true })
    }
  })

  it('sin el rol: aviso de permiso', () => {
    expect(mapearErrorConvocatorias(error('ERR_AUTH_07')).mensaje).toBe('No tienes permiso para esta acción.')
  })

  it('un código desconocido da el mensaje genérico', () => {
    expect(mapearErrorConvocatorias(error('ERR_XX_99')).mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })

  it('un error de red ofrece reintentar', () => {
    const red = new AxiosError('Network Error')

    expect(mapearErrorConvocatorias(red)).toMatchObject({ red: true })
  })

  it('cualquier otra cosa da el mensaje genérico', () => {
    expect(mapearErrorConvocatorias(new Error('x')).mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })
})
