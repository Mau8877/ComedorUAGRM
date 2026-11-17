import { describe, expect, it } from 'vitest'

import { complementariosIniciales, miembroVacio, valoresIniciales } from '../components/valoresIniciales'
import { aBorrador, aRequest, respuestasSocioeconomicasSchema } from '../schemas'
import type { RespuestasSocioeconomicasValues } from '../schemas'

import { complementariosMinimos } from './datos'

const complementariosValidos = {
  ...complementariosIniciales(null),
  dependenciaEconomica: 'Solo de padre o madre',
  ocupacionSosten: 'Comerciante minorista',
  rangoIngresos: 'De Bs. 2501 a Bs. 4000',
  tieneHijos: 'no',
  tenenciaVivienda: 'Alquiler',
  otroBeneficio: 'no',
  discapacidad: 'no',
}

const valido: RespuestasSocioeconomicasValues = {
  ingresosFamiliares: '1800',
  dependientes: '4',
  procedencia: 'Provincia',
  patrimonio: 'Sin vivienda propia',
  estadoVulnerabilidad: 'Hogar monoparental',
  promedioAcademico: '72.5',
  complementarios: complementariosValidos,
}

const conComplementarios = (extra: Partial<RespuestasSocioeconomicasValues['complementarios']>) => ({
  ...valido,
  complementarios: { ...complementariosValidos, ...extra },
})

describe('respuestasSocioeconomicasSchema', () => {
  it('convierte el texto del formulario a los números que se envían', () => {
    const r = aRequest({ ...valido, ingresosFamiliares: '1800,50', promedioAcademico: '72,5' })

    expect(r).toEqual({
      ingresosFamiliares: 1800.5,
      dependientes: 4,
      procedencia: 'Provincia',
      patrimonio: 'Sin vivienda propia',
      estadoVulnerabilidad: 'Hogar monoparental',
      promedioAcademico: 72.5,
      complementarios: complementariosMinimos,
    })
  })

  it('marca como obligatorio cada campo vacío', () => {
    const r = respuestasSocioeconomicasSchema.safeParse({
      ingresosFamiliares: '',
      dependientes: ' ',
      procedencia: '',
      patrimonio: '',
      estadoVulnerabilidad: '',
      promedioAcademico: '',
      complementarios: complementariosIniciales(null),
    })

    expect(r.success).toBe(false)
    const campos = r.error?.issues.map((i) => i.path[0])
    expect(campos).toEqual(
      expect.arrayContaining([
        'ingresosFamiliares',
        'dependientes',
        'procedencia',
        'patrimonio',
        'estadoVulnerabilidad',
        'promedioAcademico',
        'complementarios',
      ]),
    )
  })

  it.each([
    ['ingresosFamiliares', '-5'],
    ['ingresosFamiliares', '12.345'],
    ['ingresosFamiliares', 'mil'],
    ['dependientes', '2.5'],
    ['dependientes', '51'],
    ['promedioAcademico', '101'],
    ['promedioAcademico', '72.555'],
    ['promedioAcademico', 'abc'],
  ])('rechaza %s = %s', (campo, valor) => {
    expect(respuestasSocioeconomicasSchema.safeParse({ ...valido, [campo]: valor }).success).toBe(false)
  })

  it.each([
    ['promedioAcademico', '0'],
    ['promedioAcademico', '100'],
    ['promedioAcademico', '100.00'],
    ['dependientes', '0'],
    ['dependientes', '50'],
    ['ingresosFamiliares', '0'],
  ])('acepta %s = %s', (campo, valor) => {
    expect(respuestasSocioeconomicasSchema.safeParse({ ...valido, [campo]: valor }).success).toBe(true)
  })
})

describe('datos del formulario oficial de la DUBSS', () => {
  it.each(['dependenciaEconomica', 'ocupacionSosten', 'rangoIngresos', 'tenenciaVivienda'] as const)(
    'exige elegir %s al enviar',
    (campo) => {
      const r = respuestasSocioeconomicasSchema.safeParse(conComplementarios({ [campo]: '' }))

      expect(r.success).toBe(false)
      expect(r.error?.issues.map((i) => i.path.join('.'))).toContain(`complementarios.${campo}`)
    },
  )

  it.each(['tieneHijos', 'otroBeneficio', 'discapacidad'] as const)('exige marcar sí o no en %s', (campo) => {
    expect(respuestasSocioeconomicasSchema.safeParse(conComplementarios({ [campo]: '' })).success).toBe(false)
    expect(respuestasSocioeconomicasSchema.safeParse(conComplementarios({ [campo]: 'si' })).success).toBe(
      campo !== 'tieneHijos',
    )
  })

  it('pide cuántos hijos solo cuando dice que tiene', () => {
    const sinCantidad = respuestasSocioeconomicasSchema.safeParse(conComplementarios({ tieneHijos: 'si' }))
    expect(sinCantidad.success).toBe(false)
    expect(sinCantidad.error?.issues.map((i) => i.path.join('.'))).toContain('complementarios.cantidadHijos')

    expect(aRequest(conComplementarios({ tieneHijos: 'si', cantidadHijos: 'Más de 1' }))?.complementarios).toMatchObject({
      tieneHijos: true,
      cantidadHijos: 'Más de 1',
    })
    expect(aRequest(conComplementarios({ tieneHijos: 'no' }))?.complementarios.cantidadHijos).toBeNull()
  })

  it('convierte el grupo familiar, la edad y los ambientes de la vivienda a lo que espera el backend', () => {
    const r = aRequest(
      conComplementarios({
        grupoFamiliar: [{ nombre: ' María ', parentesco: 'Madre', edad: '45', ocupacion: '', observacion: '' }],
        dormitorios: '2',
        patio: '',
        provincia: 'Andrés Ibáñez',
        comentario: 'Dependo de mi madre',
      }),
    )

    expect(r?.complementarios.grupoFamiliar).toEqual([
      { nombre: 'María', parentesco: 'Madre', edad: 45, ocupacion: null, observacion: null },
    ])
    expect(r?.complementarios.infraestructura).toMatchObject({ dormitorios: 2, patio: null })
    expect(r?.complementarios.residencia?.provincia).toBe('Andrés Ibáñez')
    expect(r?.complementarios.comentario).toBe('Dependo de mi madre')
  })

  it('admite hasta 10 familiares y rechaza el undécimo', () => {
    const familiares = (n: number) => Array.from({ length: n }, () => ({ ...miembroVacio(), nombre: 'Familiar' }))

    expect(aRequest(conComplementarios({ grupoFamiliar: familiares(10) }))).not.toBeNull()
    expect(aRequest(conComplementarios({ grupoFamiliar: familiares(11) }))).toBeNull()
  })

  it.each([
    ['edad de un familiar', { grupoFamiliar: [{ ...miembroVacio(), edad: '121' }] }],
    ['edad que no es número', { grupoFamiliar: [{ ...miembroVacio(), edad: 'abc' }] }],
    ['ambiente con más de 20', { dormitorios: '21' }],
    ['comentario de más de 500 caracteres', { comentario: 'x'.repeat(501) }],
  ])('rechaza %s', (_nombre, extra) => {
    expect(aRequest(conComplementarios(extra))).toBeNull()
  })

  it('un familiar vacío (fila agregada y sin llenar) no bloquea el envío', () => {
    expect(aRequest(conComplementarios({ grupoFamiliar: [miembroVacio()] }))).not.toBeNull()
  })
})

describe('valoresIniciales', () => {
  it('sin respuestas guardadas, todo vacío y sin filas de grupo familiar', () => {
    const v = valoresIniciales(null)

    expect(v.ingresosFamiliares).toBe('')
    expect(v.complementarios.tieneHijos).toBe('')
    expect(v.complementarios.grupoFamiliar).toEqual([])
  })

  it('lo guardado vuelve al texto del formulario y, al enviarlo, da el mismo cuerpo', () => {
    const guardado = {
      ...complementariosMinimos,
      tieneHijos: true,
      cantidadHijos: '1',
      grupoFamiliar: [{ nombre: 'María', parentesco: 'Madre', edad: 45, ocupacion: null, observacion: null }],
      otroBeneficio: true,
      otroBeneficioDetalle: 'Beca de trabajo',
    }
    const v = valoresIniciales({
      ingresosFamiliares: 1800,
      dependientes: 4,
      procedencia: 'Provincia',
      patrimonio: 'Sin vivienda propia',
      estadoVulnerabilidad: 'Hogar monoparental',
      promedioAcademico: 72.5,
      guardadoEn: null,
      complementarios: guardado,
    })

    expect(v.complementarios).toMatchObject({ tieneHijos: 'si', otroBeneficio: 'si', discapacidad: 'no' })
    expect(v.complementarios.grupoFamiliar[0]).toMatchObject({ nombre: 'María', edad: '45' })
    expect(aRequest(v)?.complementarios).toEqual(guardado)
  })
})

describe('aBorrador (CU-17)', () => {
  const vacio: RespuestasSocioeconomicasValues = {
    ingresosFamiliares: '',
    dependientes: '',
    procedencia: '',
    patrimonio: '',
    estadoVulnerabilidad: '',
    promedioAcademico: '',
    complementarios: complementariosIniciales(null),
  }

  it('el formulario vacío es un borrador válido: todo va en null', () => {
    expect(aBorrador(vacio)).toEqual({
      ok: true,
      borrador: {
        ingresosFamiliares: null,
        dependientes: null,
        procedencia: null,
        patrimonio: null,
        estadoVulnerabilidad: null,
        promedioAcademico: null,
        complementarios: expect.objectContaining({
          dependenciaEconomica: null,
          tieneHijos: null,
          otroBeneficio: null,
          discapacidad: null,
          grupoFamiliar: null,
        }),
      },
    })
  })

  it('convierte a número lo que está escrito y deja en null lo vacío', () => {
    const r = aBorrador({ ...vacio, ingresosFamiliares: '1800,50', procedencia: 'Provincia', dependientes: ' ' })

    expect(r).toMatchObject({
      ok: true,
      borrador: { ingresosFamiliares: 1800.5, procedencia: 'Provincia', dependientes: null, promedioAcademico: null },
    })
  })

  it('guarda lo que ya contestó del formulario oficial sin exigir el resto', () => {
    const r = aBorrador({
      ...vacio,
      complementarios: { ...vacio.complementarios, tieneHijos: 'si', rangoIngresos: 'Menor de Bs. 2500' },
    })

    expect(r).toMatchObject({
      ok: true,
      borrador: { complementarios: { tieneHijos: true, cantidadHijos: null, rangoIngresos: 'Menor de Bs. 2500' } },
    })
  })

  it('un valor escrito que no cumple la regla se señala por campo y no genera borrador', () => {
    const r = aBorrador({ ...vacio, promedioAcademico: '120', dependientes: '4,5' })

    expect(r.ok).toBe(false)
    expect(r.ok === false && Object.keys(r.errores)).toEqual(['dependientes', 'promedioAcademico'])
  })

  it('un valor mal escrito en el formulario oficial también se señala', () => {
    const r = aBorrador({ ...vacio, complementarios: { ...vacio.complementarios, dormitorios: 'mucho' } })

    expect(r.ok).toBe(false)
    expect(r.ok === false && Object.keys(r.errores)).toEqual(['complementarios'])
  })
})
