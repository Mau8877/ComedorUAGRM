import { z } from 'zod'

import type { RegistrarAuditoriaRequest, ResultadoAuditoria } from '../types'

// * Mismas reglas que el backend (RegistrarAuditoriaRequest): observaciones de hasta 500 caracteres y
// * obligatorias si el resultado no es «Validado»; puntaje académico de 0 a 30 con hasta 2 decimales y
// * obligatorio solo si es «Validado». El puntaje se escribe como texto en el formulario y se convierte
// * a número al armar el pedido.

export const RESULTADOS_AUDITORIA = ['VALIDADO', 'OBSERVADO', 'NO_VALIDO'] as const

export const ETIQUETA_RESULTADO: Record<ResultadoAuditoria, string> = {
  VALIDADO: 'Validado',
  OBSERVADO: 'Observado',
  NO_VALIDO: 'No válido',
}

export const MAX_OBSERVACIONES = 500
export const MAX_PUNTAJE_ACADEMICO = 30

const PUNTAJE = /^\d+([.,]\d{1,2})?$/

/** Convierte el texto del campo (admite coma decimal) a número; `NaN` si no es un número válido. */
export function leerPuntaje(texto: string): number {
  const limpio = texto.trim()
  return PUNTAJE.test(limpio) ? Number(limpio.replace(',', '.')) : Number.NaN
}

export const auditoriaSchema = z
  .object({
    resultado: z.enum(RESULTADOS_AUDITORIA, { error: 'Elige el resultado de la auditoría' }),
    puntajeAcademico: z.string(),
    observaciones: z.string().max(MAX_OBSERVACIONES, 'Las observaciones no pueden pasar de 500 caracteres'),
  })
  .superRefine((valores, ctx) => {
    const puntaje = valores.puntajeAcademico.trim()
    if (valores.resultado === 'VALIDADO' && puntaje === '') {
      ctx.addIssue({
        code: 'custom',
        path: ['puntajeAcademico'],
        message: 'El puntaje académico es obligatorio si el expediente es validado',
      })
    } else if (puntaje !== '') {
      const numero = leerPuntaje(puntaje)
      if (Number.isNaN(numero) || numero < 0 || numero > MAX_PUNTAJE_ACADEMICO) {
        ctx.addIssue({
          code: 'custom',
          path: ['puntajeAcademico'],
          message: 'El puntaje académico debe estar entre 0 y 30, con hasta dos decimales',
        })
      }
    }
    if (valores.resultado !== 'VALIDADO' && valores.observaciones.trim() === '') {
      ctx.addIssue({
        code: 'custom',
        path: ['observaciones'],
        message: 'Las observaciones son obligatorias si el expediente es observado o no válido',
      })
    }
  })

export type AuditoriaValues = z.infer<typeof auditoriaSchema>

/** Valores del formulario (el resultado empieza sin elegir). */
export interface AuditoriaFormValues {
  resultado: ResultadoAuditoria | ''
  puntajeAcademico: string
  observaciones: string
}

/** El pedido al backend: el puntaje solo viaja con «Validado» y las observaciones solo si tienen texto. */
export function aAuditoriaRequest(valores: AuditoriaValues): RegistrarAuditoriaRequest {
  const observaciones = valores.observaciones.trim()
  return {
    resultado: valores.resultado,
    ...(observaciones ? { observaciones } : {}),
    ...(valores.resultado === 'VALIDADO' ? { puntajeAcademico: leerPuntaje(valores.puntajeAcademico) } : {}),
  }
}
