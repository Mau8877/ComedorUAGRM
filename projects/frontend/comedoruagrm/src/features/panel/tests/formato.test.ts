import { describe, expect, it } from 'vitest'

import { diasHasta, formatearDiaCorto, formatearFecha, formatearHora } from '../components/formato'

describe('formato del panel', () => {
  it('pasa un instante a la hora de La Paz', () => {
    expect(formatearHora('2026-11-18T14:30:00Z')).toBe('10:30')
  })

  it('formatea fechas yyyy-MM-dd', () => {
    expect(formatearDiaCorto('2026-11-18')).toBe('18/11')
    expect(formatearFecha('2026-11-18')).toBe('18/11/2026')
  })

  it('cuenta los días que faltan, redondeando hacia arriba y sin bajar de 0', () => {
    const ahora = new Date('2026-11-18T00:00:00Z')
    expect(diasHasta('2026-11-19T12:00:00Z', ahora)).toBe(2)
    expect(diasHasta('2026-11-18T05:00:00Z', ahora)).toBe(1)
    expect(diasHasta('2026-11-17T00:00:00Z', ahora)).toBe(0)
  })
})
