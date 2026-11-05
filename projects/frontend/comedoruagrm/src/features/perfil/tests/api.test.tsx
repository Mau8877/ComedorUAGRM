import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import { AxiosError, type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient, ApiError } from '@/store'

import { actualizarPerfilApi, cambiarContrasenaApi, obtenerPerfilApi } from '../api/perfilApi'
import { perfilKeys } from '../api/keys'
import { mapearErrorPerfil } from '../api/mapearError'
import { useActualizarPerfil } from '../api/useActualizarPerfil'
import { useCambiarContrasena } from '../api/useCambiarContrasena'
import type { Perfil } from '../types'

const perfil: Perfil = {
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Perez',
  ci: '1234567',
  correo: 'ana@uagrm.edu.bo',
  rol: 'POSTULANTE',
  activo: true,
  postulante: {
    registroUniversitario: '219012345',
    carrera: { codigo: 'c1', nombre: 'Ingeniería', facultad: 'Tecnología' },
    telefono: '70000001',
  },
}

describe('API del perfil (CU15)', () => {
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
  const exito = (data: unknown) => ({ status: 'success', data, message: '', timestamp: '' })

  it('obtenerPerfilApi pide GET /perfil y devuelve el perfil', async () => {
    responder(200, exito(perfil))

    expect(await obtenerPerfilApi()).toEqual(perfil)
    expect(vistas[0].method).toBe('get')
    expect(vistas[0].url).toBe('/perfil')
  })

  it('actualizarPerfilApi hace PATCH /perfil solo con lo que se pasa', async () => {
    responder(200, exito(perfil))

    await actualizarPerfilApi({ telefono: '71111111' })

    expect(vistas[0].method).toBe('patch')
    expect(vistas[0].url).toBe('/perfil')
    expect(JSON.parse(String(vistas[0].data))).toEqual({ telefono: '71111111' })
  })

  it('cambiarContrasenaApi hace POST /auth/cambiar-contrasena sin ningún token en el cuerpo', async () => {
    responder(200, exito(null))

    await cambiarContrasenaApi({ contrasenaActual: 'a', contrasenaNueva: 'nueva-123', confirmacionContrasena: 'nueva-123' })

    expect(vistas[0].url).toBe('/auth/cambiar-contrasena')
    const cuerpo = JSON.parse(String(vistas[0].data))
    expect(cuerpo).toEqual({ contrasenaActual: 'a', contrasenaNueva: 'nueva-123', confirmacionContrasena: 'nueva-123' })
    expect(JSON.stringify(cuerpo)).not.toContain('refreshToken')
  })

  describe('hooks', () => {
    function montar<T>(hook: () => T) {
      const queryClient = new QueryClient()
      const wrapper = ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      )
      return { queryClient, ...renderHook(hook, { wrapper }) }
    }

    it('useActualizarPerfil deja el perfil nuevo en la caché e invalida el perfil y el usuario actual', async () => {
      responder(200, exito({ ...perfil, correo: 'nuevo@uagrm.edu.bo' }))
      const { result, queryClient } = montar(() => useActualizarPerfil())
      const invalidar = vi.spyOn(queryClient, 'invalidateQueries')

      await act(async () => result.current.mutate({ correo: 'nuevo@uagrm.edu.bo', contrasenaActual: 'secreta1' }))

      await waitFor(() => expect(result.current.isSuccess).toBe(true))
      expect(queryClient.getQueryData<Perfil>(perfilKeys.detail())?.correo).toBe('nuevo@uagrm.edu.bo')
      const claves = invalidar.mock.calls.map((c) => c[0]?.queryKey)
      expect(claves).toContainEqual(perfilKeys.detail())
      expect(claves).toContainEqual(['auth', 'usuario-actual'])
    })

    it('useCambiarContrasena no toca la sesión local (la caché queda igual)', async () => {
      responder(200, exito(null))
      const { result, queryClient } = montar(() => useCambiarContrasena())
      queryClient.setQueryData(['auth', 'usuario-actual'], { codigo: 'u1' })

      await act(async () =>
        result.current.mutate({ contrasenaActual: 'a', contrasenaNueva: 'nueva-123', confirmacionContrasena: 'nueva-123' }),
      )

      await waitFor(() => expect(result.current.isSuccess).toBe(true))
      expect(queryClient.getQueryData(['auth', 'usuario-actual'])).toEqual({ codigo: 'u1' })
    })
  })

  describe('mapearErrorPerfil', () => {
    it('ERR_AUTH_09 marca la contraseña actual con su propio mensaje', () => {
      expect(mapearErrorPerfil(new ApiError('ERR_AUTH_09', 'x'))).toEqual({
        mensaje: 'La contraseña actual es incorrecta.',
        campo: 'contrasenaActual',
      })
    })

    it('ERR_US_02 marca el correo con el mensaje del backend', () => {
      expect(mapearErrorPerfil(new ApiError('ERR_US_02', 'Ya existe una cuenta con ese correo electrónico'))).toEqual({
        mensaje: 'Ya existe una cuenta con ese correo electrónico',
        campo: 'correo',
      })
    })

    it('ERR_SYS_01 y ERR_US_13 muestran el mensaje del backend sin marcar campo', () => {
      expect(mapearErrorPerfil(new ApiError('ERR_SYS_01', 'dato inválido'))).toEqual({ mensaje: 'dato inválido' })
      expect(mapearErrorPerfil(new ApiError('ERR_US_13', 'sin teléfono'))).toEqual({ mensaje: 'sin teléfono' })
    })

    it('un error de red ofrece reintentar y uno desconocido da el mensaje genérico', () => {
      expect(mapearErrorPerfil(new AxiosError('Network Error', 'ERR_NETWORK'))).toMatchObject({ red: true })
      const generico = mapearErrorPerfil(new ApiError('ERR_SYS_00', 'stacktrace interno'))
      expect(generico.mensaje).not.toContain('stacktrace')
      expect(generico.campo).toBeUndefined()
    })
  })
})
