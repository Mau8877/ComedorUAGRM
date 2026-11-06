import { AxiosError, type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import { apiClient, ApiError } from '@/store'

import { loginApi, logoutApi, refreshApi, registrarApi, usuarioActualApi } from '../api/authApi'
import type { RegistroRequest } from '../schemas'

const usuario = { codigo: 'u1', nombres: 'Ana', apellidos: 'Perez', correo: 'ana@uagrm.edu.bo', rol: 'POSTULANTE' }
const sesion = { accessToken: 'a', refreshToken: 'r', tokenType: 'Bearer', expiresIn: 900, usuario }

describe('authApi', () => {
  let vistas: InternalAxiosRequestConfig[]
  const original = apiClient.defaults.adapter

  beforeEach(() => {
    vistas = []
  })
  afterEach(() => {
    apiClient.defaults.adapter = original
  })

  function responder(status: number, body: unknown) {
    const adapter: AxiosAdapter = async (config) => {
      vistas.push(config)
      const res = { data: body, status, statusText: '', headers: {}, config }
      if (status >= 400) throw new AxiosError('fallo', String(status), config, null, res)
      return res
    }
    apiClient.defaults.adapter = adapter
  }
  const exito = (data: unknown, extra: object = {}) => ({ status: 'success', data, message: '', timestamp: '', ...extra })
  const fallo = (error: string) => ({ status: 'failed', data: null, message: 'm', timestamp: '', error })

  it('registrarApi hace POST /auth/registro y distingue 201 (creada) de 200 (ya existía)', async () => {
    const request = { correo: 'ana@uagrm.edu.bo' } as RegistroRequest
    responder(201, exito(sesion))
    expect(await registrarApi(request)).toEqual({ sesion, creado: true })
    expect(vistas[0].url).toBe('/auth/registro')

    responder(200, exito(sesion))
    expect((await registrarApi(request)).creado).toBe(false)
  })

  it('loginApi hace POST /auth/login con correo y contraseña', async () => {
    responder(200, exito(sesion))

    expect(await loginApi('ana@uagrm.edu.bo', 'secreta')).toEqual(sesion)
    expect(JSON.parse(vistas[0].data)).toEqual({ correo: 'ana@uagrm.edu.bo', contrasena: 'secreta' })
  })

  it('un ApiError de login se propaga con su code', async () => {
    responder(401, fallo('ERR_AUTH_04'))

    await expect(loginApi('a@b.c', 'x')).rejects.toMatchObject({ code: 'ERR_AUTH_04' })
  })

  it('refreshApi y logoutApi van sin cuerpo (el navegador manda la cookie)', async () => {
    responder(200, exito(sesion))
    await refreshApi()
    responder(200, exito(null))
    await logoutApi()

    expect(vistas.map((v) => [v.method, v.url, v.data])).toEqual([
      ['post', '/auth/refresh', undefined],
      ['post', '/auth/logout', undefined],
    ])
  })

  it('usuarioActualApi devuelve el usuario, o null si no hay sesión', async () => {
    responder(200, exito(usuario))
    expect(await usuarioActualApi()).toEqual(usuario)

    for (const codigo of ['ERR_AUTH_01', 'ERR_AUTH_02', 'ERR_AUTH_03', 'ERR_AUTH_06']) {
      responder(401, fallo(codigo))
      expect(await usuarioActualApi()).toBeNull()
    }
  })

  it('usuarioActualApi propaga los errores que no son de sesión', async () => {
    responder(500, fallo('ERR_SYS_00'))

    await expect(usuarioActualApi()).rejects.toBeInstanceOf(ApiError)
  })
})
