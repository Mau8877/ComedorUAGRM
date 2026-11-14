import { describe, expect, it } from 'vitest'

import { cambiarRolSchema, crearUsuarioPersonalSchema, editarUsuarioPersonalSchema } from '../schemas'

const valido = { nombres: 'Ana', apellidos: 'Perez', ci: '1234567', correo: 'ana@uagrm.edu.bo', rol: 'DIRECTIVO' }

function mensajes(resultado: { success: boolean; error?: { issues: { message: string }[] } }) {
  return resultado.error?.issues.map((i) => i.message) ?? []
}

describe('crearUsuarioPersonalSchema', () => {
  it('acepta datos válidos con cada rol del personal', () => {
    for (const rol of ['ADMINISTRADOR_DUBSS', 'DIRECTIVO', 'PERSONAL_COMEDOR']) {
      expect(crearUsuarioPersonalSchema.safeParse({ ...valido, rol }).success).toBe(true)
    }
  })

  it('rechaza campos vacíos', () => {
    const r = crearUsuarioPersonalSchema.safeParse({ ...valido, nombres: ' ', apellidos: '', ci: '', correo: '' })
    expect(mensajes(r)).toEqual(
      expect.arrayContaining([
        'Los nombres son obligatorios',
        'Los apellidos son obligatorios',
        'El CI es obligatorio',
        'El correo electrónico es obligatorio',
      ]),
    )
  })

  it('rechaza un correo con formato inválido', () => {
    expect(mensajes(crearUsuarioPersonalSchema.safeParse({ ...valido, correo: 'no-es-correo' }))).toContain(
      'El correo electrónico no es válido',
    )
  })

  it('rechaza largos máximos', () => {
    expect(crearUsuarioPersonalSchema.safeParse({ ...valido, nombres: 'a'.repeat(101) }).success).toBe(false)
    expect(crearUsuarioPersonalSchema.safeParse({ ...valido, apellidos: 'a'.repeat(101) }).success).toBe(false)
    expect(crearUsuarioPersonalSchema.safeParse({ ...valido, ci: '1'.repeat(21) }).success).toBe(false)
    expect(crearUsuarioPersonalSchema.safeParse({ ...valido, correo: `${'a'.repeat(150)}@uagrm.edu.bo` }).success).toBe(false)
    expect(crearUsuarioPersonalSchema.safeParse({ ...valido, nombres: 'a'.repeat(100), ci: '1'.repeat(20) }).success).toBe(true)
  })

  it('rechaza POSTULANTE y roles desconocidos', () => {
    expect(crearUsuarioPersonalSchema.safeParse({ ...valido, rol: 'POSTULANTE' }).success).toBe(false)
    expect(crearUsuarioPersonalSchema.safeParse({ ...valido, rol: 'OTRO' }).success).toBe(false)
    expect(crearUsuarioPersonalSchema.safeParse({ ...valido, rol: '' }).success).toBe(false)
  })
})

describe('editarUsuarioPersonalSchema', () => {
  const edicion = { nombres: 'Ana', apellidos: 'Perez', correo: 'ana@uagrm.edu.bo' }

  it('acepta datos válidos', () => {
    expect(editarUsuarioPersonalSchema.safeParse(edicion).success).toBe(true)
  })

  it('exige los tres campos y un correo válido', () => {
    expect(editarUsuarioPersonalSchema.safeParse({ ...edicion, nombres: '' }).success).toBe(false)
    expect(editarUsuarioPersonalSchema.safeParse({ ...edicion, apellidos: '  ' }).success).toBe(false)
    expect(editarUsuarioPersonalSchema.safeParse({ ...edicion, correo: 'x' }).success).toBe(false)
  })

  it('respeta los largos máximos', () => {
    expect(editarUsuarioPersonalSchema.safeParse({ ...edicion, nombres: 'a'.repeat(101) }).success).toBe(false)
    expect(editarUsuarioPersonalSchema.safeParse({ ...edicion, correo: `${'a'.repeat(150)}@x.bo` }).success).toBe(false)
  })

  it('no acepta ni valida el CI (no es editable)', () => {
    const r = editarUsuarioPersonalSchema.safeParse({ ...edicion, ci: '999' })
    expect(r.success && 'ci' in r.data).toBe(false)
  })
})

describe('cambiarRolSchema', () => {
  it('acepta los roles del personal y rechaza POSTULANTE', () => {
    expect(cambiarRolSchema.safeParse({ rol: 'PERSONAL_COMEDOR' }).success).toBe(true)
    expect(cambiarRolSchema.safeParse({ rol: 'POSTULANTE' }).success).toBe(false)
    expect(cambiarRolSchema.safeParse({ rol: '' }).success).toBe(false)
  })
})
