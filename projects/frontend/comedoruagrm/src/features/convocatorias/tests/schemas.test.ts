import { describe, expect, it } from 'vitest'

import { convocatoriaSchema, gestionSchema } from '../schemas'
import type { ConvocatoriaFormValues } from '../schemas'

function valida(extra: Partial<ConvocatoriaFormValues> = {}): ConvocatoriaFormValues {
  return {
    nombre: 'Beca Comedor 2026-II',
    gestionCodigo: 'g1',
    fechaApertura: '2026-08-01',
    fechaCierre: '2026-08-31',
    cupos: '100',
    puntajeMinimo: '40.5',
    cantidadFinalistas: '120',
    documentos: [{ tipoDocumentoCodigo: 't1', obligatorio: true }],
    ...extra,
  }
}

function errores(valores: ConvocatoriaFormValues): Record<string, string> {
  const resultado = convocatoriaSchema.safeParse(valores)
  if (resultado.success) return {}
  return Object.fromEntries(resultado.error.issues.map((i) => [i.path.join('.'), i.message]))
}

describe('convocatoriaSchema', () => {
  it('acepta una convocatoria válida y pasa los números a número', () => {
    expect(convocatoriaSchema.parse(valida())).toEqual({
      nombre: 'Beca Comedor 2026-II',
      gestionCodigo: 'g1',
      fechaApertura: '2026-08-01',
      fechaCierre: '2026-08-31',
      cupos: 100,
      puntajeMinimo: 40.5,
      cantidadFinalistas: 120,
      documentos: [{ tipoDocumentoCodigo: 't1', obligatorio: true }],
    })
  })

  it('un opcional vacío pasa a null (cupos, puntaje mínimo y finalistas)', () => {
    const request = convocatoriaSchema.parse(valida({ cupos: '', puntajeMinimo: '  ', cantidadFinalistas: '' }))

    expect(request.cupos).toBeNull()
    expect(request.puntajeMinimo).toBeNull()
    expect(request.cantidadFinalistas).toBeNull()
  })

  it('el nombre es obligatorio y no pasa de 150 caracteres', () => {
    expect(errores(valida({ nombre: '   ' })).nombre).toBe('El nombre es obligatorio')
    expect(errores(valida({ nombre: 'x'.repeat(151) })).nombre).toBe('El nombre no puede superar los 150 caracteres')
  })

  it('la gestión y las dos fechas son obligatorias', () => {
    const e = errores(valida({ gestionCodigo: '', fechaApertura: '', fechaCierre: '' }))

    expect(e.gestionCodigo).toBe('La gestión académica es obligatoria')
    expect(e.fechaApertura).toBe('La fecha de apertura es obligatoria')
    expect(e.fechaCierre).toBe('La fecha de cierre es obligatoria')
  })

  it('el cierre igual o anterior a la apertura se rechaza en el campo de cierre', () => {
    expect(errores(valida({ fechaCierre: '2026-08-01' })).fechaCierre).toBe(
      'La fecha de cierre debe ser posterior a la de apertura',
    )
    expect(errores(valida({ fechaCierre: '2026-07-31' })).fechaCierre).toBe(
      'La fecha de cierre debe ser posterior a la de apertura',
    )
    expect(errores(valida({ fechaCierre: '2026-08-02' })).fechaCierre).toBeUndefined()
  })

  it('cupos y cantidad de finalistas: entero positivo', () => {
    for (const malo of ['0', '-3', '1.5', 'abc']) {
      expect(errores(valida({ cupos: malo })).cupos).toBe('Los cupos deben ser un número entero positivo')
      expect(errores(valida({ cantidadFinalistas: malo })).cantidadFinalistas).toBe(
        'La cantidad de finalistas debe ser un número entero positivo',
      )
    }
  })

  it('puntaje mínimo: de 0 a 100 con hasta dos decimales', () => {
    for (const bueno of ['0', '100', '40', '40.5', '99.99', '100.00']) {
      expect(errores(valida({ puntajeMinimo: bueno })).puntajeMinimo, bueno).toBeUndefined()
    }
    for (const malo of ['-1', '100.01', '101', '40.123', 'x']) {
      expect(errores(valida({ puntajeMinimo: malo })).puntajeMinimo, malo).toBe(
        'El puntaje mínimo debe estar entre 0 y 100, con hasta dos decimales',
      )
    }
  })

  it('exige al menos un documento', () => {
    expect(errores(valida({ documentos: [] })).documentos).toBe('Indica al menos un documento requerido')
  })

  it('un documento sin tipo elegido se marca en esa fila', () => {
    expect(errores(valida({ documentos: [{ tipoDocumentoCodigo: '', obligatorio: true }] }))['documentos.0.tipoDocumentoCodigo']).toBe(
      'Selecciona el tipo de documento',
    )
  })

  it('no admite un tipo de documento repetido', () => {
    const documentos = [
      { tipoDocumentoCodigo: 't1', obligatorio: true },
      { tipoDocumentoCodigo: 't1', obligatorio: false },
    ]

    expect(errores(valida({ documentos })).documentos).toBe('Un tipo de documento no puede repetirse')
  })

  it('exige al menos un documento obligatorio', () => {
    const documentos = [
      { tipoDocumentoCodigo: 't1', obligatorio: false },
      { tipoDocumentoCodigo: 't2', obligatorio: false },
    ]

    expect(errores(valida({ documentos })).documentos).toBe('Indica al menos un documento obligatorio')
  })
})

describe('gestionSchema', () => {
  const valida = { etiqueta: '2026-II', fechaInicio: '2026-07-01', fechaFin: '2026-12-20' }

  it('acepta una gestión válida y recorta la etiqueta', () => {
    expect(gestionSchema.parse({ ...valida, etiqueta: '  2026-II ' })).toEqual(valida)
  })

  it('la etiqueta es obligatoria y no pasa de 50 caracteres', () => {
    expect(gestionSchema.safeParse({ ...valida, etiqueta: ' ' }).success).toBe(false)
    expect(gestionSchema.safeParse({ ...valida, etiqueta: 'x'.repeat(51) }).success).toBe(false)
  })

  it('las dos fechas son obligatorias', () => {
    const r = gestionSchema.safeParse({ ...valida, fechaInicio: '', fechaFin: '' })

    expect(r.success).toBe(false)
    expect(r.error?.issues.map((i) => i.message)).toEqual(
      expect.arrayContaining(['La fecha de inicio es obligatoria', 'La fecha de fin es obligatoria']),
    )
  })

  it('el fin igual o anterior al inicio se rechaza en el campo de fin', () => {
    for (const fin of ['2026-07-01', '2026-06-30']) {
      const r = gestionSchema.safeParse({ ...valida, fechaFin: fin })

      expect(r.success).toBe(false)
      expect(r.error?.issues[0]).toMatchObject({
        path: ['fechaFin'],
        message: 'La fecha de fin debe ser posterior a la de inicio',
      })
    }
  })
})
