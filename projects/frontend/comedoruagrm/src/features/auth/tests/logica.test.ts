import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearError } from '../api/mapearError'
import { etiquetaRol } from '../roles'
import { rutaInicioPorRol } from '../rutaInicioPorRol'

describe('rutaInicioPorRol', () => {
  it('manda a /panel a los 4 roles del backend', () => {
    for (const rol of ['POSTULANTE', 'ADMINISTRADOR_DUBSS', 'DIRECTIVO', 'PERSONAL_COMEDOR']) {
      expect(rutaInicioPorRol(rol)).toBe('/panel')
    }
  })

  it('manda a /login a un rol desconocido', () => {
    expect(rutaInicioPorRol('HACKER')).toBe('/login')
  })
})

describe('etiquetaRol', () => {
  it('traduce el rol y deja tal cual uno desconocido', () => {
    expect(etiquetaRol('ADMINISTRADOR_DUBSS')).toBe('Administrador DUBSS')
    expect(etiquetaRol('OTRO')).toBe('OTRO')
  })
})

describe('mapearError', () => {
  it('ERR_AUTH_04 da siempre el mismo mensaje de credenciales', () => {
    expect(mapearError(new ApiError('ERR_AUTH_04', 'lo que diga el backend')).mensaje).toBe(
      'Correo o contraseña incorrectos.',
    )
  })

  it('ERR_AUTH_05 muestra el mensaje del backend (trae los minutos que faltan)', () => {
    const mensaje = 'Tu cuenta está bloqueada. Vuelve a intentarlo en 5 minutos.'
    expect(mapearError(new ApiError('ERR_AUTH_05', mensaje)).mensaje).toBe(mensaje)
  })

  it('ERR_AUTH_03 indica que la cuenta está desactivada y a quién acudir', () => {
    expect(mapearError(new ApiError('ERR_AUTH_03', 'x')).mensaje).toContain('DUBSS')
  })

  it('marca el campo en conflicto en ERR_US_01, 02 y 03', () => {
    expect(mapearError(new ApiError('ERR_US_01', 'Ya existe una cuenta con ese CI'))).toEqual({
      mensaje: 'Ya existe una cuenta con ese CI',
      campo: 'ci',
    })
    expect(mapearError(new ApiError('ERR_US_02', 'm')).campo).toBe('correo')
    expect(mapearError(new ApiError('ERR_US_03', 'm')).campo).toBe('registroUniversitario')
  })

  it('ERR_AUTH_08 marca el enlace como inválido para ofrecer pedir uno nuevo (CU15, A2)', () => {
    expect(mapearError(new ApiError('ERR_AUTH_08', 'lo que diga el backend'))).toEqual({
      mensaje: 'El enlace ya no sirve. Solicita uno nuevo.',
      enlaceInvalido: true,
    })
  })

  it('un error de red ofrece reintentar', () => {
    const sinRespuesta = new AxiosError('Network Error', 'ERR_NETWORK')
    expect(mapearError(sinRespuesta)).toMatchObject({ red: true })
  })

  it('un código desconocido da el mensaje genérico, sin filtrar detalles', () => {
    const mapeado = mapearError(new ApiError('ERR_SYS_00', 'stacktrace interno'))
    expect(mapeado.mensaje).not.toContain('stacktrace')
    expect(mapeado.campo).toBeUndefined()
  })
})
