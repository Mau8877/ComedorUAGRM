import { describe, expect, it } from 'vitest'

import { aNoAdjudicarRequest, noAdjudicarSchema } from '../schemas'

describe('noAdjudicarSchema', () => {
  it('acepta un motivo y uno de exactamente 500 caracteres', () => {
    expect(noAdjudicarSchema.safeParse({ motivo: 'No cumple' }).success).toBe(true)
    expect(noAdjudicarSchema.safeParse({ motivo: 'x'.repeat(500) }).success).toBe(true)
  })

  it('rechaza el motivo vacío o de solo espacios', () => {
    for (const motivo of ['', '   ']) {
      const resultado = noAdjudicarSchema.safeParse({ motivo })

      expect(resultado.success).toBe(false)
      expect(resultado.error?.issues[0].message).toBe('El motivo es obligatorio')
    }
  })

  it('rechaza más de 500 caracteres', () => {
    const resultado = noAdjudicarSchema.safeParse({ motivo: 'x'.repeat(501) })

    expect(resultado.success).toBe(false)
    expect(resultado.error?.issues[0].message).toBe('El motivo no puede pasar de 500 caracteres')
  })

  it('el pedido lleva el motivo sin espacios sobrantes', () => {
    expect(aNoAdjudicarRequest({ motivo: '  No cumple  ' })).toEqual({ motivo: 'No cumple' })
  })
})
