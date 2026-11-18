import { describe, expect, it } from 'vitest'

import { formatearPorcentajeONulo, formatearVariacion, nombreDia } from '../components/formato'

describe('formato del análisis BI', () => {
  it('la variación lleva signo y coma decimal; sin dato es un guion', () => {
    expect(formatearVariacion(12.5)).toBe('+12,5 %')
    expect(formatearVariacion(-3)).toBe('-3 %')
    expect(formatearVariacion(0)).toBe('0 %')
    expect(formatearVariacion(null)).toBe('—')
  })

  it('el porcentaje nulo es un guion', () => {
    expect(formatearPorcentajeONulo(null)).toBe('—')
    expect(formatearPorcentajeONulo(83.3)).toBe('83,3 %')
  })

  it('nombra los días de la semana de lunes a domingo', () => {
    expect(nombreDia(1)).toBe('Lun')
    expect(nombreDia(7)).toBe('Dom')
  })
})
