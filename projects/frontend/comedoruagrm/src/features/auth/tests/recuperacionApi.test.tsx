import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import { AxiosError, type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import { apiClient, ApiError } from '@/store'

import { restablecerContrasenaApi, solicitarRecuperacionApi } from '../api/authApi'
import { authKeys } from '../api/keys'
import { useRestablecerContrasena } from '../api/useRestablecerContrasena'
import { useSolicitarRecuperacion } from '../api/useSolicitarRecuperacion'

describe('API de recuperación de contraseña (CU15)', () => {
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
  const exito = { status: 'success', data: null, message: 'ok', timestamp: '' }
  const fallo = (error: string) => ({ status: 'failed', data: null, message: 'm', timestamp: '', error })

  it('solicitarRecuperacionApi hace POST /auth/recuperar-contrasena solo con el correo', async () => {
    responder(200, exito)

    await solicitarRecuperacionApi('ana@uagrm.edu.bo')

    expect(vistas[0].method).toBe('post')
    expect(vistas[0].url).toBe('/auth/recuperar-contrasena')
    expect(JSON.parse(String(vistas[0].data))).toEqual({ correo: 'ana@uagrm.edu.bo' })
  })

  it('restablecerContrasenaApi hace POST /auth/restablecer-contrasena con el token y las contraseñas', async () => {
    responder(200, exito)

    await restablecerContrasenaApi({ token: 'tok', contrasena: 'secreta1', confirmacionContrasena: 'secreta1' })

    expect(vistas[0].url).toBe('/auth/restablecer-contrasena')
    expect(JSON.parse(String(vistas[0].data))).toEqual({
      token: 'tok',
      contrasena: 'secreta1',
      confirmacionContrasena: 'secreta1',
    })
  })

  it('ninguna de las dos manda ni guarda tokens de sesión (viajan en cookies HttpOnly)', async () => {
    responder(200, exito)

    await solicitarRecuperacionApi('ana@uagrm.edu.bo')
    await restablecerContrasenaApi({ token: 'tok', contrasena: 'secreta1', confirmacionContrasena: 'secreta1' })

    for (const vista of vistas) {
      expect(JSON.stringify(vista.headers)).not.toContain('Bearer')
      expect(String(vista.data)).not.toContain('refreshToken')
    }
  })

  it('un enlace inválido se rechaza con ApiError ERR_AUTH_08', async () => {
    responder(400, fallo('ERR_AUTH_08'))

    await expect(
      restablecerContrasenaApi({ token: 'tok', contrasena: 'secreta1', confirmacionContrasena: 'secreta1' }),
    ).rejects.toMatchObject({ code: 'ERR_AUTH_08' })
    await expect(
      restablecerContrasenaApi({ token: 'tok', contrasena: 'secreta1', confirmacionContrasena: 'secreta1' }),
    ).rejects.toBeInstanceOf(ApiError)
  })

  describe('hooks', () => {
    function montar<T>(hook: () => T) {
      const queryClient = new QueryClient()
      queryClient.setQueryData(authKeys.usuarioActual, { codigo: 'u1' })
      const wrapper = ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      )
      return { queryClient, ...renderHook(hook, { wrapper }) }
    }

    it('useSolicitarRecuperacion recorta el correo y no limpia la caché', async () => {
      responder(200, exito)
      const { result, queryClient } = montar(() => useSolicitarRecuperacion())

      await act(async () => result.current.mutate('  ana@uagrm.edu.bo '))

      await waitFor(() => expect(result.current.isSuccess).toBe(true))
      expect(JSON.parse(String(vistas[0].data))).toEqual({ correo: 'ana@uagrm.edu.bo' })
      expect(queryClient.getQueryData(authKeys.usuarioActual)).toBeDefined()
    })

    it('useRestablecerContrasena limpia la caché al terminar bien (el backend cerró todas las sesiones)', async () => {
      responder(200, exito)
      const { result, queryClient } = montar(() => useRestablecerContrasena())

      await act(async () =>
        result.current.mutate({ token: 'tok', contrasena: 'secreta1', confirmacionContrasena: 'secreta1' }),
      )

      await waitFor(() => expect(result.current.isSuccess).toBe(true))
      expect(queryClient.getQueryData(authKeys.usuarioActual)).toBeUndefined()
    })

    it('useRestablecerContrasena con un enlace inválido no limpia la caché y deja el error', async () => {
      responder(400, fallo('ERR_AUTH_08'))
      const { result, queryClient } = montar(() => useRestablecerContrasena())

      await act(async () =>
        result.current.mutate({ token: 'tok', contrasena: 'secreta1', confirmacionContrasena: 'secreta1' }),
      )

      await waitFor(() => expect(result.current.isError).toBe(true))
      expect(queryClient.getQueryData(authKeys.usuarioActual)).toBeDefined()
    })
  })
})
