import { afterEach, describe, expect, it, vi } from 'vitest'
import { AxiosError } from 'axios'

import { ApiError, apiClient } from '@/store'

import {
  enviarPostulacionApi,
  guardarBorradorApi,
  iniciarPostulacionApi,
  mapearErrorEnvio,
  miBorradorApi,
} from '../api'

import { complementariosMinimos } from './datos'

describe('API del envío de la postulación (CU-02)', () => {
  afterEach(() => vi.restoreAllMocks())

  it('iniciarPostulacionApi hace POST /postulaciones sin cuerpo y devuelve el formulario', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { postulacion: 'p-1', estado: 'borrador' } })

    expect(await iniciarPostulacionApi()).toEqual({ postulacion: 'p-1', estado: 'borrador' })
    expect(post).toHaveBeenCalledWith('/postulaciones')
  })

  it('enviarPostulacionApi hace POST /postulaciones/{codigo}/enviar con las respuestas', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { codigo: 'p-1', estado: 'pendiente' } })
    const respuestas = {
      ingresosFamiliares: 1800,
      dependientes: 4,
      procedencia: 'Provincia',
      patrimonio: 'Sin vivienda propia',
      estadoVulnerabilidad: 'Hogar monoparental',
      promedioAcademico: 72.5,
      complementarios: complementariosMinimos,
    }

    expect(await enviarPostulacionApi('p-1', respuestas)).toEqual({ codigo: 'p-1', estado: 'pendiente' })
    expect(post).toHaveBeenCalledWith('/postulaciones/p-1/enviar', respuestas)
  })
})

describe('API del borrador de la postulación (CU-17)', () => {
  afterEach(() => vi.restoreAllMocks())

  it('guardarBorradorApi hace PUT /postulaciones/{codigo}/borrador con el formulario tal cual', async () => {
    const put = vi.spyOn(apiClient, 'put').mockResolvedValue({ data: { postulacion: 'p-1', camposPendientes: 5 } })
    const borrador = {
      ingresosFamiliares: 1800,
      dependientes: null,
      procedencia: null,
      patrimonio: null,
      estadoVulnerabilidad: null,
      promedioAcademico: null,
      complementarios: null,
    }

    expect(await guardarBorradorApi('p-1', borrador)).toEqual({ postulacion: 'p-1', camposPendientes: 5 })
    expect(put).toHaveBeenCalledWith('/postulaciones/p-1/borrador', borrador)
  })

  it('miBorradorApi hace GET /postulaciones/mi-borrador y devuelve null si no hay borrador', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: null })

    expect(await miBorradorApi()).toBeNull()
    expect(get).toHaveBeenCalledWith('/postulaciones/mi-borrador')
  })

  it('miBorradorApi devuelve null (no undefined) cuando el backend omite `data` por no haber borrador', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({ data: undefined })

    expect(await miBorradorApi()).toBeNull()
  })
})

describe('mapearErrorEnvio', () => {
  it('ERR_ENV_01 es un aviso de que no hay convocatoria abierta, con el mensaje del backend', () => {
    const r = mapearErrorEnvio(new ApiError('ERR_ENV_01', 'No hay una convocatoria abierta para postular.'))

    expect(r.sinConvocatoria).toBe(true)
    expect(r.mensaje).toContain('No hay una convocatoria abierta')
  })

  it.each(['ERR_VAL_01', 'ERR_VAL_02'])('%s indica que no puede postular', (codigo) => {
    expect(mapearErrorEnvio(new ApiError(codigo, 'Ya tienes una postulación vigente')).noPuedePostular).toBe(true)
  })

  it('ERR_VAL_03 y ERR_ENV_03 indican que el plazo finalizó o ya se envió', () => {
    expect(mapearErrorEnvio(new ApiError('ERR_VAL_03', 'x')).cerrada).toBe(true)
    expect(mapearErrorEnvio(new ApiError('ERR_ENV_03', 'x')).cerrada).toBe(true)
  })

  it('ERR_ENV_04 y ERR_ENV_05 traen lo que falta tal cual lo explica el backend', () => {
    expect(mapearErrorEnvio(new ApiError('ERR_ENV_04', 'Falta completar documentos: Carnet')).mensaje).toBe(
      'Falta completar documentos: Carnet',
    )
    expect(mapearErrorEnvio(new ApiError('ERR_ENV_05', 'Revisa el formulario: x')).mensaje).toContain('Revisa')
  })

  it('un error de red permite reintentar', () => {
    const r = mapearErrorEnvio(new AxiosError('Network Error'))

    expect(r.red).toBe(true)
  })

  it('un error desconocido da el mensaje genérico', () => {
    expect(mapearErrorEnvio(new Error('x')).mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })
})
