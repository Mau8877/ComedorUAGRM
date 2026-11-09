import { describe, expect, it } from 'vitest'

import { aCierreRequest, cierreSchema, MAX_MOTIVO } from '../schemas'

describe('cierreSchema', () => {
  it('acepta un motivo de hasta 500 caracteres', () => {
    expect(cierreSchema.safeParse({ motivo: 'x'.repeat(MAX_MOTIVO) }).success).toBe(true)
  })

  it('rechaza un motivo de 501 caracteres', () => {
    const r = cierreSchema.safeParse({ motivo: 'x'.repeat(MAX_MOTIVO + 1) })

    expect(r.success).toBe(false)
    expect(r.error?.issues[0].message).toBe('El motivo no puede pasar de 500 caracteres')
  })

  it('rechaza un motivo vacío o de solo espacios', () => {
    for (const motivo of ['', '   ']) {
      const r = cierreSchema.safeParse({ motivo })
      expect(r.success).toBe(false)
      expect(r.error?.issues[0].message).toBe('El motivo es obligatorio')
    }
  })

  it('el pedido lleva el motivo sin espacios sobrantes', () => {
    const analizado = cierreSchema.parse({ motivo: '  No entregó sus documentos  ' })

    expect(aCierreRequest(analizado)).toEqual({ motivo: 'No entregó sus documentos' })
  })
})
