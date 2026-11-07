import { describe, expect, it } from 'vitest'

import {
  aEntregaRequest,
  aNotificarRequest,
  hoyIso,
  mananaIso,
  notificarFinalistasSchema,
  registrarEntregaSchema,
} from '../schemas'

const valido = { plazoEntrega: mananaIso(), lugarHorario: 'DUBSS, oficina 3', documentos: 'CI' }

describe('notificarFinalistasSchema', () => {
  it('acepta un plazo de mañana con lugar y documentos', () => {
    expect(notificarFinalistasSchema.safeParse(valido).success).toBe(true)
  })

  it('rechaza un plazo de hoy o pasado (debe ser posterior a hoy)', () => {
    for (const plazo of [hoyIso(), '2020-01-01']) {
      const resultado = notificarFinalistasSchema.safeParse({ ...valido, plazoEntrega: plazo })
      expect(resultado.success).toBe(false)
      expect(resultado.error?.issues[0].message).toBe('El plazo debe ser posterior a hoy')
    }
  })

  it('pide plazo, lugar y documentos y no acepta solo espacios', () => {
    const resultado = notificarFinalistasSchema.safeParse({ plazoEntrega: '', lugarHorario: '   ', documentos: '' })

    expect(resultado.success).toBe(false)
    expect(resultado.error?.issues.map((i) => i.message)).toEqual([
      'El plazo de entrega es obligatorio',
      'El lugar y horario de entrega son obligatorios',
      'Indica los documentos que deben presentar',
    ])
  })

  it('limita el largo de lugar y documentos como el backend', () => {
    expect(notificarFinalistasSchema.safeParse({ ...valido, lugarHorario: 'x'.repeat(301) }).success).toBe(false)
    expect(notificarFinalistasSchema.safeParse({ ...valido, documentos: 'x'.repeat(501) }).success).toBe(false)
  })

  it('aNotificarRequest arma el pedido con los códigos y los textos sin espacios sobrantes', () => {
    expect(aNotificarRequest(['a'], { ...valido, lugarHorario: ' DUBSS ', documentos: ' CI ' })).toEqual({
      finalistas: ['a'],
      plazoEntrega: valido.plazoEntrega,
      lugarHorario: 'DUBSS',
      documentos: 'CI',
    })
  })
})

describe('registrarEntregaSchema', () => {
  it('acepta hoy y una fecha pasada, pero no una futura', () => {
    expect(registrarEntregaSchema.safeParse({ fechaRecepcion: hoyIso(), observaciones: '' }).success).toBe(true)
    expect(registrarEntregaSchema.safeParse({ fechaRecepcion: '2020-01-01', observaciones: '' }).success).toBe(true)
    const futura = registrarEntregaSchema.safeParse({ fechaRecepcion: mananaIso(), observaciones: '' })
    expect(futura.success).toBe(false)
    expect(futura.error?.issues[0].message).toBe('La fecha de recepción no puede ser futura')
  })

  it('pide la fecha y limita las observaciones a 500 caracteres', () => {
    expect(registrarEntregaSchema.safeParse({ fechaRecepcion: '', observaciones: '' }).success).toBe(false)
    expect(registrarEntregaSchema.safeParse({ fechaRecepcion: hoyIso(), observaciones: 'x'.repeat(501) }).success).toBe(
      false,
    )
  })

  it('aEntregaRequest omite las observaciones vacías', () => {
    expect(aEntregaRequest({ fechaRecepcion: '2026-11-06', observaciones: '  ' })).toEqual({
      fechaRecepcion: '2026-11-06',
    })
    expect(aEntregaRequest({ fechaRecepcion: '2026-11-06', observaciones: ' Completo ' })).toEqual({
      fechaRecepcion: '2026-11-06',
      observaciones: 'Completo',
    })
  })
})
