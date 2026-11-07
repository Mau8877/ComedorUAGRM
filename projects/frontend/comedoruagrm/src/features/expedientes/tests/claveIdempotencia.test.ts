import { describe, expect, it, vi } from 'vitest'

import { nuevaClaveIdempotencia } from '../api/claveIdempotencia'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

describe('nuevaClaveIdempotencia', () => {
  it('genera un UUID v4 distinto cada vez', () => {
    const a = nuevaClaveIdempotencia()
    const b = nuevaClaveIdempotencia()

    expect(a).toMatch(UUID)
    expect(a).not.toBe(b)
  })

  it('sin crypto.randomUUID usa el respaldo y sigue siendo un UUID v4', () => {
    vi.stubGlobal('crypto', {})
    try {
      expect(nuevaClaveIdempotencia()).toMatch(UUID)
    } finally {
      vi.unstubAllGlobals()
    }
  })
})
