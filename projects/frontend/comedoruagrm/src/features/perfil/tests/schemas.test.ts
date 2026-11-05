import { describe, expect, it } from 'vitest'

import {
  aActualizarPerfilRequest,
  aCambiarContrasenaRequest,
  cambiarContrasenaSchema,
  crearActualizarPerfilSchema,
} from '../schemas'

function mensajes(resultado: { error?: { issues: { message: string; path: PropertyKey[] }[] } }) {
  return resultado.error?.issues.map((i) => `${String(i.path[0])}: ${i.message}`) ?? []
}

describe('crearActualizarPerfilSchema', () => {
  const postulante = crearActualizarPerfilSchema({ correoActual: 'ana@uagrm.edu.bo', esPostulante: true })
  const admin = crearActualizarPerfilSchema({ correoActual: 'luis@uagrm.edu.bo', esPostulante: false })
  const base = { correo: 'ana@uagrm.edu.bo', telefono: '70000001', contrasenaActual: '' }

  it('acepta los datos sin cambiar el correo y sin contraseña actual', () => {
    expect(postulante.safeParse(base).success).toBe(true)
  })

  it('exige la contraseña actual solo cuando el correo cambió', () => {
    const cambiado = { ...base, correo: 'nuevo@uagrm.edu.bo' }
    expect(mensajes(postulante.safeParse(cambiado))).toContain(
      'contrasenaActual: Ingresa tu contraseña actual para cambiar el correo',
    )
    expect(postulante.safeParse({ ...cambiado, contrasenaActual: 'secreta1' }).success).toBe(true)
  })

  it('no exige la contraseña si el correo solo cambió de mayúsculas o espacios', () => {
    expect(postulante.safeParse({ ...base, correo: ' ANA@uagrm.edu.bo ' }).success).toBe(true)
  })

  it('rechaza el correo vacío, sin formato o largo', () => {
    expect(mensajes(postulante.safeParse({ ...base, correo: '' }))).toContain('correo: El correo electrónico es obligatorio')
    expect(mensajes(postulante.safeParse({ ...base, correo: 'x' }))).toContain('correo: El correo electrónico no es válido')
    expect(postulante.safeParse({ ...base, correo: `${'a'.repeat(150)}@uagrm.edu.bo` }).success).toBe(false)
  })

  it('exige el teléfono del Postulante (hasta 20 caracteres) y no el de otros roles', () => {
    expect(mensajes(postulante.safeParse({ ...base, telefono: '  ' }))).toContain('telefono: El teléfono es obligatorio')
    expect(postulante.safeParse({ ...base, telefono: '7'.repeat(21) }).success).toBe(false)
    expect(admin.safeParse({ correo: 'luis@uagrm.edu.bo', telefono: '', contrasenaActual: '' }).success).toBe(true)
  })
})

describe('aActualizarPerfilRequest', () => {
  const original = { correo: 'ana@uagrm.edu.bo', telefono: '70000001' }

  it('manda solo el teléfono cuando solo cambió el teléfono, sin contraseña', () => {
    const request = aActualizarPerfilRequest(
      { correo: 'ana@uagrm.edu.bo', telefono: ' 71111111 ', contrasenaActual: 'ignorada' },
      original,
    )
    expect(request).toEqual({ telefono: '71111111' })
  })

  it('manda el correo con la contraseña actual cuando cambió el correo', () => {
    const request = aActualizarPerfilRequest(
      { correo: ' nuevo@uagrm.edu.bo ', telefono: '70000001', contrasenaActual: 'secreta1' },
      original,
    )
    expect(request).toEqual({ correo: 'nuevo@uagrm.edu.bo', contrasenaActual: 'secreta1' })
  })

  it('no manda nada si no cambió nada y nunca incluye el CI', () => {
    const request = aActualizarPerfilRequest({ correo: 'ana@uagrm.edu.bo', telefono: '70000001', contrasenaActual: '' }, original)
    expect(request).toEqual({})
    expect(Object.keys(request)).not.toContain('ci')
  })

  it('no manda teléfono si la cuenta no tiene (no es Postulante)', () => {
    const request = aActualizarPerfilRequest(
      { correo: 'luis@uagrm.edu.bo', telefono: 'x', contrasenaActual: '' },
      { correo: 'luis@uagrm.edu.bo' },
    )
    expect(request).toEqual({})
  })
})

describe('cambiarContrasenaSchema', () => {
  const valido = { contrasenaActual: 'actual-1', contrasenaNueva: 'nueva-123', confirmarContrasena: 'nueva-123' }

  it('acepta datos válidos', () => {
    expect(cambiarContrasenaSchema.safeParse(valido).success).toBe(true)
  })

  it('rechaza la actual vacía, la nueva corta o larga y la confirmación distinta', () => {
    expect(mensajes(cambiarContrasenaSchema.safeParse({ ...valido, contrasenaActual: '' }))).toContain(
      'contrasenaActual: La contraseña actual es obligatoria',
    )
    expect(mensajes(cambiarContrasenaSchema.safeParse({ ...valido, contrasenaNueva: '12345', confirmarContrasena: '12345' }))).toContain(
      'contrasenaNueva: La contraseña debe tener al menos 6 caracteres',
    )
    const larga = 'a'.repeat(73)
    expect(cambiarContrasenaSchema.safeParse({ ...valido, contrasenaNueva: larga, confirmarContrasena: larga }).success).toBe(false)
    expect(mensajes(cambiarContrasenaSchema.safeParse({ ...valido, confirmarContrasena: 'otra-1234' }))).toContain(
      'confirmarContrasena: Las contraseñas no coinciden',
    )
  })

  it('aCambiarContrasenaRequest renombra la confirmación y no lleva ningún token', () => {
    const request = aCambiarContrasenaRequest(valido)
    expect(request).toEqual({
      contrasenaActual: 'actual-1',
      contrasenaNueva: 'nueva-123',
      confirmacionContrasena: 'nueva-123',
    })
    expect(Object.keys(request)).not.toContain('refreshToken')
  })
})
