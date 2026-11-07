import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorExpedientes } from '../api/mapearErrorExpedientes'

describe('mapearErrorExpedientes', () => {
  it('traduce los códigos del CU07', () => {
    expect(mapearErrorExpedientes(new ApiError('ERR_EXP_01', 'x')).mensaje).toBe(
      'Primero registra la entrega de la documentación física.',
    )
    expect(mapearErrorExpedientes(new ApiError('ERR_EXP_02', 'x')).mensaje).toBe(
      'La postulación ya no está calificada.',
    )
    expect(mapearErrorExpedientes(new ApiError('ERR_EXP_03', 'x')).mensaje).toContain('observaciones')
    expect(mapearErrorExpedientes(new ApiError('ERR_EXP_04', 'x')).mensaje).toContain('entre 0 y 30')
    expect(mapearErrorExpedientes(new ApiError('ERR_EXP_05', 'x')).mensaje).toContain('documento')
    expect(mapearErrorExpedientes(new ApiError('ERR_EXP_06', 'x')).mensaje).toContain('otros datos')
    expect(mapearErrorExpedientes(new ApiError('ERR_EXP_07', 'x')).mensaje).toContain('Inténtalo de nuevo')
  })

  it('marca el finalista inexistente para volver a la lista', () => {
    expect(mapearErrorExpedientes(new ApiError('ERR_FIN_01', 'x')).finalistaInexistente).toBe(true)
  })

  it('delega los demás errores en los de finalistas', () => {
    expect(mapearErrorExpedientes(new ApiError('ERR_AUTH_07', 'x')).mensaje).toBe('No tienes permiso para esta acción.')
    expect(mapearErrorExpedientes(new ApiError('ERR_SYS_00', 'detalle')).mensaje).toBe(
      'Ocurrió un error inesperado. Inténtalo de nuevo.',
    )
    expect(mapearErrorExpedientes(new AxiosError('Network Error')).red).toBe(true)
  })
})
