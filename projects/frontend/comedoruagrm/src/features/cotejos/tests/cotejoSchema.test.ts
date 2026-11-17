import { describe, expect, it } from 'vitest'

import { calcularCotejo, cotejoSchema, crearSolicitudCotejo } from '../schemas'

function validar(total: string, coincidencias: string, observaciones = '') {
  return cotejoSchema.safeParse({ total, coincidencias, observaciones })
}

function mensajes(total: string, coincidencias: string, observaciones = '') {
  const r = validar(total, coincidencias, observaciones)
  return r.success ? [] : r.error.issues.map((i) => i.message)
}

describe('cotejoSchema', () => {
  it('acepta un cotejo conforme sin observaciones', () => {
    expect(validar('880', '880').success).toBe(true)
  })

  it('acepta un cotejo con discrepancias y observaciones', () => {
    expect(validar('880', '861', 'Faltan firmas').success).toBe(true)
  })

  it('exige el total y las coincidencias', () => {
    expect(mensajes('', '')).toEqual(
      expect.arrayContaining(['El total de registros es obligatorio', 'Las coincidencias son obligatorias']),
    )
  })

  it('rechaza lo que no es un entero de 0 o más', () => {
    expect(mensajes('-1', '0')).toContain('Ingresa un número entero, 0 o mayor')
    expect(mensajes('10', '2.5')).toContain('Ingresa un número entero, 0 o mayor')
    expect(mensajes('abc', '1')).toContain('Ingresa un número entero, 0 o mayor')
  })

  it('rechaza más coincidencias que registros', () => {
    expect(mensajes('10', '11')).toContain('Las coincidencias no pueden superar el total de registros')
  })

  it('con discrepancias exige observaciones (A2), también si solo tienen espacios', () => {
    expect(mensajes('10', '8')).toContain('Con discrepancias, las observaciones son obligatorias')
    expect(mensajes('10', '8', '   ')).toContain('Con discrepancias, las observaciones son obligatorias')
  })

  it('sin discrepancias las observaciones son opcionales', () => {
    expect(validar('10', '10', 'Todo en orden').success).toBe(true)
  })

  it('con total 0 y 0 coincidencias es conforme', () => {
    expect(validar('0', '0').success).toBe(true)
  })
})

describe('calcularCotejo', () => {
  it('deduce las discrepancias y el resultado', () => {
    expect(calcularCotejo({ total: '10', coincidencias: '10' })).toEqual({ discrepancias: 0, resultado: 'CONFORME' })
    expect(calcularCotejo({ total: '10', coincidencias: '7' })).toEqual({
      discrepancias: 3,
      resultado: 'CON_DISCREPANCIAS',
    })
  })

  it('devuelve null mientras los números no son válidos', () => {
    expect(calcularCotejo({ total: '', coincidencias: '1' })).toBeNull()
    expect(calcularCotejo({ total: '5', coincidencias: 'x' })).toBeNull()
    expect(calcularCotejo({ total: '5', coincidencias: '6' })).toBeNull()
  })
})

describe('crearSolicitudCotejo', () => {
  it('arma el cuerpo con números y las observaciones sin espacios sobrantes', () => {
    expect(crearSolicitudCotejo({ total: '880', coincidencias: '861', observaciones: '  Faltan firmas  ' })).toEqual({
      totalRegistros: 880,
      coincidencias: 861,
      discrepancias: 19,
      resultado: 'CON_DISCREPANCIAS',
      observaciones: 'Faltan firmas',
    })
  })

  it('omite las observaciones vacías', () => {
    expect(crearSolicitudCotejo({ total: '5', coincidencias: '5', observaciones: '  ' })).toEqual({
      totalRegistros: 5,
      coincidencias: 5,
      discrepancias: 0,
      resultado: 'CONFORME',
    })
  })

  it('falla si el formulario no es válido', () => {
    expect(() => crearSolicitudCotejo({ total: '', coincidencias: '', observaciones: '' })).toThrow()
  })
})
