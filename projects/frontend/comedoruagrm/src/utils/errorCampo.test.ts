import { describe, expect, it } from 'vitest'

import { errorVisible, mensajeError } from './errorCampo'

function campo(isBlurred: boolean, submissionAttempts: number, errors: unknown[] = [{ message: 'Obligatorio' }]) {
  return { state: { meta: { isBlurred, errors } }, form: { state: { submissionAttempts } } } as never
}

describe('errorCampo', () => {
  it('no muestra el error mientras se escribe por primera vez', () => {
    expect(errorVisible(campo(false, 0))).toBe(false)
    expect(mensajeError(campo(false, 0))).toBeUndefined()
  })

  it('lo muestra al salir del campo', () => {
    expect(mensajeError(campo(true, 0))).toBe('Obligatorio')
  })

  it('lo muestra al intentar enviar aunque no se haya salido del campo', () => {
    expect(mensajeError(campo(false, 1))).toBe('Obligatorio')
  })

  it('sin error devuelve undefined', () => {
    expect(mensajeError(campo(true, 1, []))).toBeUndefined()
  })
})
