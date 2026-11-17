import { describe, expect, it } from 'vitest'

import {
  aPayload,
  parametrosSchema,
  siguienteVersion,
  sumaEsValida,
  sumaPonderaciones,
  valoresIniciales,
} from '../schemas'
import type { ParametrosFormValues } from '../schemas'

const MOCKUP: ParametrosFormValues = {
  descripcion: '',
  ingresos_familiares: '22',
  dependientes: '14',
  procedencia: '10',
  patrimonio: '12',
  estado_vulnerabilidad: '8',
  promedio_academico: '10',
  dependencia_economica: '8',
  ocupacion_sosten: '6',
  discapacidad: '6',
  hijos: '4',
}

describe('parametrosSchema', () => {
  it('acepta las diez ponderaciones', () => {
    expect(parametrosSchema.safeParse(MOCKUP).success).toBe(true)
  })

  it('acepta decimales con punto o coma, hasta dos', () => {
    expect(parametrosSchema.safeParse({ ...MOCKUP, dependientes: '14.5' }).success).toBe(true)
    expect(parametrosSchema.safeParse({ ...MOCKUP, dependientes: '14,25' }).success).toBe(true)
  })

  it('rechaza vacío, negativos, más de 100, letras y más de dos decimales', () => {
    for (const malo of ['', '-1', '101', 'abc', '10.123', '1e2']) {
      expect(parametrosSchema.safeParse({ ...MOCKUP, patrimonio: malo }).success, malo).toBe(false)
    }
  })

  it('limita la descripción a 255 caracteres', () => {
    expect(parametrosSchema.safeParse({ ...MOCKUP, descripcion: 'a'.repeat(256) }).success).toBe(false)
  })
})

describe('sumaPonderaciones', () => {
  it('suma las diez variables y es válida solo si da 100', () => {
    expect(sumaPonderaciones(MOCKUP)).toBe(100)
    expect(sumaEsValida(MOCKUP)).toBe(true)
    expect(sumaEsValida({ ...MOCKUP, ingresos_familiares: '30' })).toBe(false)
    expect(sumaEsValida({ ...MOCKUP, hijos: '' })).toBe(false)
  })

  it('no arrastra errores de punto flotante', () => {
    const valores = { ...MOCKUP, ingresos_familiares: '0.1', dependientes: '0.2', procedencia: '49.85', patrimonio: '49.85', estado_vulnerabilidad: '0', promedio_academico: '0', dependencia_economica: '0', ocupacion_sosten: '0', discapacidad: '0', hijos: '0' }

    expect(sumaPonderaciones(valores)).toBe(100)
  })

  it('lo que todavía no es un número cuenta como 0', () => {
    expect(sumaPonderaciones({ ...MOCKUP, ingresos_familiares: 'x', dependientes: '' })).toBe(64)
  })
})

describe('valoresIniciales y aPayload', () => {
  it('sin versión vigente deja todo vacío', () => {
    expect(valoresIniciales(undefined).ingresos_familiares).toBe('')
  })

  it('con una versión copia sus ponderaciones', () => {
    const valores = valoresIniciales([{ clave: 'dependientes', nombre: 'Dependientes', ponderacion: 20 }])

    expect(valores.dependientes).toBe('20')
    expect(valores.patrimonio).toBe('')
    // Una versión anterior (de seis variables) deja vacías las del formulario oficial.
    expect(valores.hijos).toBe('')
  })

  it('el payload lleva las diez variables como números y omite la descripción vacía', () => {
    const payload = aPayload({ ...MOCKUP, dependientes: '20,5' })

    expect(payload).not.toHaveProperty('descripcion')
    expect(payload.parametros).toHaveLength(10)
    expect(payload.parametros[1]).toEqual({ clave: 'dependientes', ponderacion: 20.5 })
    expect(payload.parametros[0].clave).toBe('ingresos_familiares')
    expect(payload.parametros[9]).toEqual({ clave: 'hijos', ponderacion: 4 })
  })

  it('el payload lleva la descripción sin espacios sobrantes', () => {
    expect(aPayload({ ...MOCKUP, descripcion: '  Ajuste  ' }).descripcion).toBe('Ajuste')
  })
})

describe('siguienteVersion', () => {
  it('sube el menor de la versión vigente', () => {
    expect(siguienteVersion('v1.3')).toBe('v1.4')
    expect(siguienteVersion('v1.9')).toBe('v1.10')
  })

  it('una versión sin punto cuenta como v1.0', () => {
    expect(siguienteVersion('v1')).toBe('v1.1')
  })

  it('sin versión vigente (o con un formato desconocido) es v1.0', () => {
    expect(siguienteVersion(null)).toBe('v1.0')
    expect(siguienteVersion(undefined)).toBe('v1.0')
    expect(siguienteVersion('test-123')).toBe('v1.0')
  })
})
