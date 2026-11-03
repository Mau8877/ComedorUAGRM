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
    setAuthTokenProvider(async () => 'token-de-acceso')

    await apiClient.get('/x')

    expect(vistas[0].headers.get('Authorization')).toBe('Bearer token-de-acceso')
  })

  it('no manda Authorization si no hay proveedor o no hay sesión', async () => {
    usarAdapter(200, { status: 'success', data: null, message: '', timestamp: '' })

    await apiClient.get('/x')
    setAuthTokenProvider(async () => null)
    await apiClient.get('/x')

    expect(vistas[0].headers.has('Authorization')).toBe(false)
    expect(vistas[1].headers.has('Authorization')).toBe(false)
  })

  it('usa cookies de sesión: withCredentials queda activado', async () => {
    usarAdapter(200, { status: 'success', data: null, message: '', timestamp: '' })

    await apiClient.get('/x')

    expect(vistas[0].withCredentials).toBe(true)
  })

  it('ante un 401 avisa al manejador de sesión y rechaza con ApiError', async () => {
    usarAdapter(401, {
      status: 'failed',
      data: null,
      message: 'No autenticado',
      timestamp: '',
      error: 'ERR_SYS_03',
    })
    const alExpirar = vi.fn().mockResolvedValue(false)
    setUnauthorizedHandler(alExpirar)

    await expect(apiClient.get('/x')).rejects.toBeInstanceOf(ApiError)
    expect(alExpirar).toHaveBeenCalledTimes(1)
  })

  it('si el manejador logra renovar la sesión, reintenta la petición una sola vez', async () => {
    let llamadas = 0
    apiClient.defaults.adapter = async (config) => {
      llamadas += 1
      if (llamadas === 1) {
        const res = respuesta(config, 401, { status: 'failed', data: null, message: '', timestamp: '', error: 'ERR_AUTH_01' })
        throw new AxiosError('fallo', '401', config, null, res)
      }
      return respuesta(config, 200, { status: 'success', data: { ok: true }, message: '', timestamp: '' })
    }
    const alExpirar = vi.fn().mockResolvedValue(true)
    setUnauthorizedHandler(alExpirar)

    const res = await apiClient.get('/x')

    expect(res.data).toEqual({ ok: true })
    expect(llamadas).toBe(2)
    expect(alExpirar).toHaveBeenCalledTimes(1)
  })

  it('si el reintento también da 401, no vuelve a renovar ni reintenta en bucle', async () => {
    let llamadas = 0
    apiClient.defaults.adapter = async (config) => {
      llamadas += 1
      const res = respuesta(config, 401, { status: 'failed', data: null, message: '', timestamp: '', error: 'ERR_AUTH_01' })
      throw new AxiosError('fallo', '401', config, null, res)
    }
    const alExpirar = vi.fn().mockResolvedValue(true)
    setUnauthorizedHandler(alExpirar)

    await expect(apiClient.get('/x')).rejects.toBeInstanceOf(ApiError)
    expect(llamadas).toBe(2)
    expect(alExpirar).toHaveBeenCalledTimes(1)
  })

  it('un error que no es 401 no dispara el manejador de sesión', async () => {
    usarAdapter(404, { status: 'failed', data: null, message: 'No existe', timestamp: '', error: 'ERR_XX_01' })
    const alExpirar = vi.fn().mockResolvedValue(false)
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
