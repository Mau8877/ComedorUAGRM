import { describe, expect, it } from 'vitest'

import { aDevolucionRequest, devolucionSchema, MAX_MOTIVO } from '../schemas'

describe('devolucionSchema', () => {
  it('acepta un motivo de hasta 500 caracteres', () => {
    expect(devolucionSchema.safeParse({ motivo: 'x'.repeat(MAX_MOTIVO) }).success).toBe(true)
  })

  it('rechaza un motivo de 501 caracteres', () => {
    const r = devolucionSchema.safeParse({ motivo: 'x'.repeat(MAX_MOTIVO + 1) })

    expect(r.success).toBe(false)
    expect(r.error?.issues[0].message).toBe('El motivo no puede pasar de 500 caracteres')
  })

  it('rechaza un motivo vacío o de solo espacios', () => {
    for (const motivo of ['', '   ']) {
      const r = devolucionSchema.safeParse({ motivo })
      expect(r.success).toBe(false)
      expect(r.error?.issues[0].message).toBe('El motivo es obligatorio')
    }
  })

  it('el pedido lleva el motivo sin espacios sobrantes', () => {
    const analizado = devolucionSchema.parse({ motivo: '  Falta el carnet  ' })

    expect(aDevolucionRequest(analizado)).toEqual({ motivo: 'Falta el carnet' })
  })
})
