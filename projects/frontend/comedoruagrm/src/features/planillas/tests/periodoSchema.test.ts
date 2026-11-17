import { describe, expect, it } from 'vitest'

import { crearPeriodoSchema, MAX_DIAS_PLANILLA } from '../schemas'

const GESTION = { gestion: '2026-II', gestionDesde: '2026-07-01', gestionHasta: '2026-12-31' }

function mensajes(desde: string, hasta: string, gestion: typeof GESTION | null = GESTION) {
  const r = crearPeriodoSchema(gestion ?? undefined).safeParse({ desde, hasta })
  return r.success ? [] : r.error.issues.map((i) => `${String(i.path[0])}: ${i.message}`)
}

describe('crearPeriodoSchema', () => {
  it('acepta un periodo dentro de la gestión', () => {
    expect(mensajes('2026-09-01', '2026-09-30')).toEqual([])
  })

  it('acepta un solo día y los límites exactos de la gestión', () => {
    expect(mensajes('2026-09-01', '2026-09-01')).toEqual([])
    expect(mensajes('2026-07-01', '2026-07-31')).toEqual([])
    expect(mensajes('2026-12-01', '2026-12-31')).toEqual([])
  })

  it('pide las dos fechas', () => {
    expect(mensajes('', '')).toEqual(['desde: La fecha inicial es obligatoria', 'hasta: La fecha final es obligatoria'])
    expect(mensajes('2026-09-01', '')).toEqual(['hasta: La fecha final es obligatoria'])
  })

  it('rechaza una fecha final anterior a la inicial (ERR_PLA_01)', () => {
    expect(mensajes('2026-09-30', '2026-09-01')).toEqual(['hasta: La fecha final no puede ser anterior a la inicial'])
  })

  it('rechaza un periodo que sale de la gestión por cualquiera de los dos lados (ERR_PLA_02)', () => {
    expect(mensajes('2026-06-30', '2026-07-15')).toEqual(['desde: El periodo debe estar dentro de la gestión 2026-II'])
    expect(mensajes('2026-12-01', '2027-01-05')).toEqual(['hasta: El periodo debe estar dentro de la gestión 2026-II'])
  })

  it(`rechaza más de ${MAX_DIAS_PLANILLA} días y acepta justo ${MAX_DIAS_PLANILLA} (ERR_PLA_03)`, () => {
    expect(mensajes('2026-09-01', '2026-10-02')).toEqual(['hasta: El periodo no puede superar los 31 días'])
    expect(mensajes('2026-10-01', '2026-10-31')).toEqual([])
  })

  it('sin los límites de la gestión todavía solo valida las fechas y su orden', () => {
    expect(mensajes('2020-01-01', '2020-01-31', null)).toEqual([])
    expect(mensajes('2026-09-30', '2026-09-01', null)).toEqual([
      'hasta: La fecha final no puede ser anterior a la inicial',
    ])
  })
})
