import type { InternalAxiosRequestConfig } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const manejadores = vi.hoisted(() => ({ actual: null as null | ((c: InternalAxiosRequestConfig) => Promise<boolean>) }))

vi.mock('@/store', () => ({
  setUnauthorizedHandler: (h: typeof manejadores.actual) => {
    manejadores.actual = h
  },
}))
vi.mock('../api/authApi', () => ({ refreshApi: vi.fn() }))

import { refreshApi } from '../api/authApi'
import { esRutaDeAcceso, instalarManejadorDeSesion, renovarSesion } from '../api/sesion'

const config = (url: string) => ({ url }) as InternalAxiosRequestConfig

describe('renovarSesion', () => {
  beforeEach(() => {
    vi.mocked(refreshApi).mockReset()
  })

  it('devuelve true si el backend entrega una sesión nueva', async () => {
    vi.mocked(refreshApi).mockResolvedValue({} as never)
    expect(await renovarSesion()).toBe(true)
  })

  it('devuelve false si el refresh token no sirve', async () => {
    vi.mocked(refreshApi).mockRejectedValue(new Error('ERR_AUTH_06'))
    expect(await renovarSesion()).toBe(false)
  })

  it('renueva una sola vez aunque lo pidan tres a la vez', async () => {
    vi.mocked(refreshApi).mockResolvedValue({} as never)

    const resultados = await Promise.all([renovarSesion(), renovarSesion(), renovarSesion()])

    expect(resultados).toEqual([true, true, true])
    expect(refreshApi).toHaveBeenCalledTimes(1)
  })

  it('permite una renovación nueva cuando la anterior ya terminó', async () => {
    vi.mocked(refreshApi).mockResolvedValue({} as never)
    await renovarSesion()
    await renovarSesion()
    expect(refreshApi).toHaveBeenCalledTimes(2)
  })
})

describe('esRutaDeAcceso', () => {
  it('reconoce login, registro, refresh y logout', () => {
    for (const ruta of ['/auth/login', '/auth/registro', '/auth/refresh', '/auth/logout']) {
      expect(esRutaDeAcceso(ruta)).toBe(true)
    }
    expect(esRutaDeAcceso('/auth/usuario-actual')).toBe(false)
    expect(esRutaDeAcceso(undefined)).toBe(false)
  })

  it('reconoce las rutas públicas de recuperación de contraseña (CU15): un 401 ahí no renueva la sesión', () => {
    expect(esRutaDeAcceso('/auth/recuperar-contrasena')).toBe(true)
    expect(esRutaDeAcceso('/auth/restablecer-contrasena')).toBe(true)
  })

  it('cambiar-contrasena exige sesión: un 401 ahí sí intenta renovar', () => {
    expect(esRutaDeAcceso('/auth/cambiar-contrasena')).toBe(false)
  })
})

describe('instalarManejadorDeSesion', () => {
  const alPerderSesion = vi.fn()

  beforeEach(() => {
    vi.mocked(refreshApi).mockReset()
    alPerderSesion.mockReset()
    instalarManejadorDeSesion({ alPerderSesion })
  })

  it('un 401 de login no renueva ni pierde la sesión (es un error del formulario)', async () => {
    expect(await manejadores.actual!(config('/auth/login'))).toBe(false)
    expect(refreshApi).not.toHaveBeenCalled()
    expect(alPerderSesion).not.toHaveBeenCalled()
  })

  it('un 401 de una ruta protegida renueva y pide reintentar', async () => {
    vi.mocked(refreshApi).mockResolvedValue({} as never)

    expect(await manejadores.actual!(config('/carreras-privadas'))).toBe(true)
    expect(alPerderSesion).not.toHaveBeenCalled()
  })

  it('si no se puede renovar, avisa que se perdió la sesión', async () => {
    vi.mocked(refreshApi).mockRejectedValue(new Error('x'))

    expect(await manejadores.actual!(config('/algo'))).toBe(false)
    expect(alPerderSesion).toHaveBeenCalledTimes(1)
  })
})
