import { describe, expect, it } from 'vitest'

import { aAuditoriaRequest, auditoriaSchema, leerPuntaje } from '../schemas'

const validado = { resultado: 'VALIDADO', puntajeAcademico: '25', observaciones: '' }

function mensajes(valores: unknown): string[] {
  const resultado = auditoriaSchema.safeParse(valores)
  return resultado.success ? [] : resultado.error.issues.map((i) => i.message)
}

describe('auditoriaSchema', () => {
  it('acepta «Validado» con puntaje académico, aunque no haya observaciones', () => {
    expect(auditoriaSchema.safeParse(validado).success).toBe(true)
  })

  it('exige elegir el resultado', () => {
    expect(mensajes({ ...validado, resultado: '' })).toEqual(['Elige el resultado de la auditoría'])
  })

  it('«Validado» exige el puntaje académico', () => {
    expect(mensajes({ ...validado, puntajeAcademico: '  ' })).toEqual([
      'El puntaje académico es obligatorio si el expediente es validado',
    ])
  })

  it('el puntaje debe estar entre 0 y 30 con hasta dos decimales', () => {
    for (const malo of ['-1', '30.01', '31', 'abc', '10.123', '1e2']) {
      expect(mensajes({ ...validado, puntajeAcademico: malo })).toEqual([
        'El puntaje académico debe estar entre 0 y 30, con hasta dos decimales',
      ])
    }
    for (const bueno of ['0', '30', '30.00', '22.5', '22,75']) {
      expect(auditoriaSchema.safeParse({ ...validado, puntajeAcademico: bueno }).success).toBe(true)
    }
  })

  it('«Observado» y «No válido» exigen observaciones y no piden puntaje', () => {
    for (const resultado of ['OBSERVADO', 'NO_VALIDO']) {
      expect(mensajes({ resultado, puntajeAcademico: '', observaciones: '   ' })).toEqual([
        'Las observaciones son obligatorias si el expediente es observado o no válido',
      ])
      expect(auditoriaSchema.safeParse({ resultado, puntajeAcademico: '', observaciones: 'Falta el carnet' }).success).toBe(
        true,
      )
    }
  })

  it('las observaciones no pasan de 500 caracteres', () => {
    expect(auditoriaSchema.safeParse({ ...validado, observaciones: 'x'.repeat(500) }).success).toBe(true)
    expect(mensajes({ ...validado, observaciones: 'x'.repeat(501) })).toEqual([
      'Las observaciones no pueden pasar de 500 caracteres',
    ])
  })

  it('leerPuntaje admite coma decimal y devuelve NaN si no es un número', () => {
    expect(leerPuntaje('22,5')).toBe(22.5)
    expect(leerPuntaje(' 10 ')).toBe(10)
    expect(leerPuntaje('x')).toBeNaN()
  })

  it('aAuditoriaRequest manda el puntaje solo con «Validado» y las observaciones solo si hay texto', () => {
    expect(aAuditoriaRequest({ resultado: 'VALIDADO', puntajeAcademico: '22,5', observaciones: '  ' })).toEqual({
      resultado: 'VALIDADO',
      puntajeAcademico: 22.5,
    })
    expect(
      aAuditoriaRequest({ resultado: 'OBSERVADO', puntajeAcademico: '10', observaciones: ' Falta el carnet ' }),
    ).toEqual({ resultado: 'OBSERVADO', observaciones: 'Falta el carnet' })
  })
})
