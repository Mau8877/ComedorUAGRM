import { describe, expect, it } from 'vitest'

import { aRegistroRequest, loginSchema, registroSchema, type RegistroFormValues } from '../schemas'

const valido: RegistroFormValues = {
  nombres: 'Ana',
  apellidos: 'Perez',
  ci: '1234567',
  registroUniversitario: '219012345',
  carreraCodigo: '018f0c9a-0000-7000-8000-000000000001',
  telefono: '70000001',
  correo: 'ana@uagrm.edu.bo',
  contrasena: 'secreta',
  confirmarContrasena: 'secreta',
}

function camposConError(valores: Partial<RegistroFormValues>) {
  const resultado = registroSchema.safeParse({ ...valido, ...valores })
  return resultado.success ? [] : resultado.error.issues.map((i) => String(i.path[0]))
}

describe('registroSchema', () => {
  it('acepta un formulario completo', () => {
    expect(registroSchema.safeParse(valido).success).toBe(true)
  })

  it('rechaza una contraseña de 5 caracteres y acepta una de 6 sin mayúscula ni número', () => {
    expect(camposConError({ contrasena: '12345', confirmarContrasena: '12345' })).toEqual(['contrasena'])
    expect(camposConError({ contrasena: 'abcdef', confirmarContrasena: 'abcdef' })).toEqual([])
  })

  it('rechaza una contraseña de más de 72 caracteres', () => {
    const larga = 'a'.repeat(73)
    expect(camposConError({ contrasena: larga, confirmarContrasena: larga })).toEqual(['contrasena'])
  })

  it('marca la confirmación cuando no coincide', () => {
    expect(camposConError({ confirmarContrasena: 'otra' })).toEqual(['confirmarContrasena'])
  })

  it('marca como obligatorios los campos vacíos', () => {
    const vacio = {
      nombres: '', apellidos: '', ci: '', registroUniversitario: '', carreraCodigo: '', telefono: '',
      correo: '', contrasena: '', confirmarContrasena: '',
    }
    expect(camposConError(vacio)).toEqual(
      expect.arrayContaining(['nombres', 'apellidos', 'ci', 'registroUniversitario', 'carreraCodigo', 'telefono', 'correo', 'contrasena', 'confirmarContrasena']),
    )
  })

  it('valida el registro universitario: 9 dígitos y sin cero inicial', () => {
    for (const malo of ['21901234', '2190123456', '019012345', '21901234a']) {
      expect(camposConError({ registroUniversitario: malo })).toEqual(['registroUniversitario'])
    }
    expect(camposConError({ registroUniversitario: '100000000' })).toEqual([])
  })

  it('rechaza un correo sin formato', () => {
    expect(camposConError({ correo: 'no-es-un-correo' })).toEqual(['correo'])
  })

  it('muestra los mensajes en español', () => {
    const resultado = registroSchema.safeParse({ ...valido, ci: '', confirmarContrasena: 'otra' })
    const mensajes = resultado.success ? [] : resultado.error.issues.map((i) => i.message)
    expect(mensajes).toEqual(expect.arrayContaining(['El CI es obligatorio', 'Las contraseñas no coinciden']))
  })
})

describe('aRegistroRequest', () => {
  it('lleva correo, contraseña y la confirmación como confirmacionContrasena', () => {
    const request = aRegistroRequest({ ...valido, nombres: '  Ana ' })

    expect(request).toMatchObject({
      nombres: 'Ana',
      correo: 'ana@uagrm.edu.bo',
      contrasena: 'secreta',
      confirmacionContrasena: 'secreta',
    })
    expect(request).not.toHaveProperty('confirmarContrasena')
  })
})

describe('loginSchema', () => {
  it('acepta correo y contraseña', () => {
    expect(loginSchema.safeParse({ correo: 'ana@uagrm.edu.bo', contrasena: 'x' }).success).toBe(true)
  })

  it('rechaza campos vacíos y correos sin formato', () => {
    expect(loginSchema.safeParse({ correo: '', contrasena: '' }).success).toBe(false)
    expect(loginSchema.safeParse({ correo: 'xx', contrasena: 'x' }).success).toBe(false)
  })
})
