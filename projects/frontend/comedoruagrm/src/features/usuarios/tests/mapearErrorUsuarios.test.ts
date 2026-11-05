import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorUsuarios } from '../api/mapearErrorUsuarios'

describe('mapearErrorUsuarios', () => {
  it('ERR_US_01 marca el campo CI', () => {
    expect(mapearErrorUsuarios(new ApiError('ERR_US_01', 'CI repetido'))).toEqual({ mensaje: 'CI repetido', campo: 'ci' })
  })

  it('ERR_US_02 marca el campo correo', () => {
    expect(mapearErrorUsuarios(new ApiError('ERR_US_02', 'Correo repetido'))).toEqual({
      mensaje: 'Correo repetido',
      campo: 'correo',
    })
  })

  it.each(['ERR_SYS_01', 'ERR_US_10', 'ERR_US_08', 'ERR_US_09'])('%s muestra el mensaje del backend', (code) => {
    expect(mapearErrorUsuarios(new ApiError(code, 'Mensaje del backend'))).toEqual({ mensaje: 'Mensaje del backend' })
  })

  it.each(['ERR_US_06', 'ERR_US_07', 'ERR_US_12'])('%s muestra el mensaje y pide refrescar', (code) => {
    expect(mapearErrorUsuarios(new ApiError(code, 'Ya no existe'))).toEqual({ mensaje: 'Ya no existe', refrescar: true })
  })

  it('ERR_AUTH_07 dice que no hay permiso', () => {
    expect(mapearErrorUsuarios(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe('No tienes permiso para esta acción.')
  })

  it('un error sin respuesta es de conexión', () => {
    const r = mapearErrorUsuarios(new AxiosError('Network Error'))
    expect(r.red).toBe(true)
    expect(r.mensaje).toContain('No se pudo conectar')
  })

  it('cualquier otro código da el mensaje genérico, sin detalles', () => {
    const r = mapearErrorUsuarios(new ApiError('ERR_XX_99', 'detalle interno'))
    expect(r.mensaje).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
    expect(mapearErrorUsuarios(new Error('boom')).mensaje).toBe(r.mensaje)
  })
})
