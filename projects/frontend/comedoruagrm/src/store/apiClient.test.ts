import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios'
import { AxiosError } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient, ApiError, setAuthTokenProvider, setUnauthorizedHandler } from './apiClient'

function respuesta(config: InternalAxiosRequestConfig, status: number, data: unknown) {
  return { data, status, statusText: '', headers: {}, config }
}

describe('apiClient', () => {
  let vistas: InternalAxiosRequestConfig[]
  const original = apiClient.defaults.adapter

  beforeEach(() => {
    vistas = []
  })

  afterEach(() => {
    setAuthTokenProvider(null)
    setUnauthorizedHandler(null)
    apiClient.defaults.adapter = original
  })

  function usarAdapter(status: number, body: unknown) {
    const adapter: AxiosAdapter = async (config) => {
      vistas.push(config)
      const res = respuesta(config, status, body)
      if (status >= 400) {
        throw new AxiosError('fallo', String(status), config, null, res)
      }
      return res
    }
    apiClient.defaults.adapter = adapter
  }

  it('agrega Authorization: Bearer con el token del proveedor registrado', async () => {
    usarAdapter(200, { status: 'success', data: { ok: true }, message: '', timestamp: '' })
    setAuthTokenProvider(async () => 'token-firebase')

    await apiClient.get('/x')

    expect(vistas[0].headers.get('Authorization')).toBe('Bearer token-firebase')
  })

  it('no manda Authorization si no hay proveedor o no hay sesión', async () => {
    usarAdapter(200, { status: 'success', data: null, message: '', timestamp: '' })

    await apiClient.get('/x')
    setAuthTokenProvider(async () => null)
    await apiClient.get('/x')

    expect(vistas[0].headers.has('Authorization')).toBe(false)
    expect(vistas[1].headers.has('Authorization')).toBe(false)
  })

  it('no usa cookies: withCredentials queda sin activar', async () => {
    usarAdapter(200, { status: 'success', data: null, message: '', timestamp: '' })

    await apiClient.get('/x')

    expect(vistas[0].withCredentials).toBeFalsy()
  })

  it('ante un 401 avisa al manejador de sesión y rechaza con ApiError', async () => {
    usarAdapter(401, {
      status: 'failed',
      data: null,
      message: 'No autenticado',
      timestamp: '',
      error: 'ERR_SYS_03',
    })
    const alExpirar = vi.fn()
    setUnauthorizedHandler(alExpirar)

    await expect(apiClient.get('/x')).rejects.toBeInstanceOf(ApiError)
    expect(alExpirar).toHaveBeenCalledTimes(1)
  })

  it('un error que no es 401 no dispara el manejador de sesión', async () => {
    usarAdapter(404, { status: 'failed', data: null, message: 'No existe', timestamp: '', error: 'ERR_XX_01' })
    const alExpirar = vi.fn()
    setUnauthorizedHandler(alExpirar)

    await expect(apiClient.get('/x')).rejects.toMatchObject({ code: 'ERR_XX_01' })
    expect(alExpirar).not.toHaveBeenCalled()
  })

  it('desempaqueta el sobre de éxito', async () => {
    usarAdapter(200, { status: 'success', data: { a: 1 }, message: '', timestamp: '' })

    const res = await apiClient.get('/x')

    expect(res.data).toEqual({ a: 1 })
  })
})
