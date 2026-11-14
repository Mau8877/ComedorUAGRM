import { describe, expect, it } from 'vitest'

import {
  aRestablecerRequest,
  recuperarContrasenaSchema,
  restablecerContrasenaSchema,
} from '../schemas'

function mensajes(resultado: { success: boolean; error?: { issues: { message: string; path: PropertyKey[] }[] } }) {
  return resultado.error?.issues.map((i) => `${String(i.path[0])}: ${i.message}`) ?? []
}

describe('recuperarContrasenaSchema', () => {
  it('acepta un correo válido', () => {
    expect(recuperarContrasenaSchema.safeParse({ correo: ' ana@uagrm.edu.bo ' }).success).toBe(true)
  })

  it('rechaza el correo vacío, sin formato o demasiado largo', () => {
    expect(mensajes(recuperarContrasenaSchema.safeParse({ correo: '' }))).toContain('correo: El correo electrónico es obligatorio')
    expect(mensajes(recuperarContrasenaSchema.safeParse({ correo: 'no-es-correo' }))).toContain('correo: El correo electrónico no es válido')
    expect(recuperarContrasenaSchema.safeParse({ correo: `${'a'.repeat(150)}@uagrm.edu.bo` }).success).toBe(false)
  })
})

describe('restablecerContrasenaSchema', () => {
  const valido = { contrasena: 'secreta1', confirmarContrasena: 'secreta1' }

  it('acepta una contraseña de 6 a 72 caracteres que coincide con su confirmación', () => {
    expect(restablecerContrasenaSchema.safeParse(valido).success).toBe(true)
    expect(restablecerContrasenaSchema.safeParse({ contrasena: '123456', confirmarContrasena: '123456' }).success).toBe(true)
    const larga = 'a'.repeat(72)
    expect(restablecerContrasenaSchema.safeParse({ contrasena: larga, confirmarContrasena: larga }).success).toBe(true)
  })

  it('rechaza la contraseña de 5 y de 73 caracteres', () => {
    expect(mensajes(restablecerContrasenaSchema.safeParse({ contrasena: '12345', confirmarContrasena: '12345' }))).toContain(
      'contrasena: La contraseña debe tener al menos 6 caracteres',
    )
    const larga = 'a'.repeat(73)
    expect(mensajes(restablecerContrasenaSchema.safeParse({ contrasena: larga, confirmarContrasena: larga }))).toContain(
      'contrasena: La contraseña no puede superar los 72 caracteres',
    )
  })

  it('marca la confirmación cuando no coincide o está vacía', () => {
    expect(mensajes(restablecerContrasenaSchema.safeParse({ contrasena: 'secreta1', confirmarContrasena: 'otra-1234' }))).toContain(
      'confirmarContrasena: Las contraseñas no coinciden',
    )
    expect(mensajes(restablecerContrasenaSchema.safeParse({ contrasena: 'secreta1', confirmarContrasena: '' }))).toContain(
      'confirmarContrasena: Confirma tu contraseña',
    )
  })

  it('aRestablecerRequest agrega el token y renombra la confirmación', () => {
    expect(aRestablecerRequest(valido, 'tok-123')).toEqual({
      token: 'tok-123',
      contrasena: 'secreta1',
      confirmacionContrasena: 'secreta1',
    })
  })
})
