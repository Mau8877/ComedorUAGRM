import { z } from 'zod'

import type { NotificarFinalistasRequest, RegistrarEntregaRequest } from '../types'
import { hoyIso } from './fechas'

// * Mismas reglas que el backend (NotificarFinalistasRequest / RegistrarEntregaRequest): el plazo debe ser
// * posterior a hoy y la fecha de recepción no puede ser futura. Las fechas son `YYYY-MM-DD`, así que se
// * comparan como texto.

export const notificarFinalistasSchema = z.object({
  plazoEntrega: z
    .string()
    .min(1, { error: 'El plazo de entrega es obligatorio', abort: true })
    .refine((valor) => valor > hoyIso(), 'El plazo debe ser posterior a hoy'),
  lugarHorario: z
    .string()
    .trim()
    .min(1, 'El lugar y horario de entrega son obligatorios')
    .max(300, 'El lugar y horario no pueden pasar de 300 caracteres'),
  documentos: z
    .string()
    .trim()
    .min(1, 'Indica los documentos que deben presentar')
    .max(500, 'Los documentos no pueden pasar de 500 caracteres'),
})

export type NotificarFinalistasValues = z.infer<typeof notificarFinalistasSchema>

export function aNotificarRequest(finalistas: string[], valores: NotificarFinalistasValues): NotificarFinalistasRequest {
  return {
    finalistas,
    plazoEntrega: valores.plazoEntrega,
    lugarHorario: valores.lugarHorario.trim(),
    documentos: valores.documentos.trim(),
  }
}

export const registrarEntregaSchema = z.object({
  fechaRecepcion: z
    .string()
    .min(1, { error: 'La fecha de recepción es obligatoria', abort: true })
    .refine((valor) => valor <= hoyIso(), 'La fecha de recepción no puede ser futura'),
  observaciones: z.string().max(500, 'Las observaciones no pueden pasar de 500 caracteres'),
})

export type RegistrarEntregaValues = z.infer<typeof registrarEntregaSchema>

export function aEntregaRequest(valores: RegistrarEntregaValues): RegistrarEntregaRequest {
  const observaciones = valores.observaciones.trim()
  return { fechaRecepcion: valores.fechaRecepcion, ...(observaciones ? { observaciones } : {}) }
}
