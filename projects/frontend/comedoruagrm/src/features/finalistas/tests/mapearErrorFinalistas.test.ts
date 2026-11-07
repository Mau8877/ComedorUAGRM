import { AxiosError } from 'axios'
import { describe, expect, it } from 'vitest'

import { ApiError } from '@/store'

import { mapearErrorFinalistas } from '../api/mapearErrorFinalistas'

describe('mapearErrorFinalistas', () => {
  it('traduce los códigos del CU a mensajes claros', () => {
    expect(mapearErrorFinalistas(new ApiError('ERR_FIN_02', 'x')).convocatoriaInexistente).toBe(true)
    expect(mapearErrorFinalistas(new ApiError('ERR_FIN_03', 'x')).mensaje).toBe(
      'La fecha de recepción no puede ser futura.',
    )
    expect(mapearErrorFinalistas(new ApiError('ERR_FIN_04', 'x')).mensaje).toBe(
      'El plazo de entrega debe ser posterior a hoy.',
    )
    expect(mapearErrorFinalistas(new ApiError('ERR_COM_01', 'x')).mensaje).toContain('No hay destinatarios')
  })

  it('deja pasar el mensaje de validación del backend y oculta lo inesperado', () => {
    expect(mapearErrorFinalistas(new ApiError('ERR_SYS_01', 'Falta el título')).mensaje).toBe('Falta el título')
    expect(mapearErrorFinalistas(new ApiError('ERR_SYS_00', 'detalle técnico')).mensaje).toBe(
      'Ocurrió un error inesperado. Inténtalo de nuevo.',
    )
  })

  it('distingue el error de conexión para ofrecer reintentar', () => {
    expect(mapearErrorFinalistas(new AxiosError('Network Error')).red).toBe(true)
  })
})
