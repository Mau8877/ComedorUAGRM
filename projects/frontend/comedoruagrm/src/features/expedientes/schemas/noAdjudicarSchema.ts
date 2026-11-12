import { z } from 'zod'

import type { NoAdjudicarRequest } from '../types'
import { MAX_MOTIVO } from './devolucionSchema'

// * Mismas reglas que el backend (NoAdjudicarRequest): el motivo es obligatorio (sin contar los espacios de
// * los extremos) y de hasta 500 caracteres. «Adjudicar» no lleva cuerpo, por eso no tiene schema.

export const noAdjudicarSchema = z.object({
  motivo: z
    .string()
    .trim()
    .min(1, 'El motivo es obligatorio')
    .max(MAX_MOTIVO, 'El motivo no puede pasar de 500 caracteres'),
})

export type NoAdjudicarValues = z.output<typeof noAdjudicarSchema>
export type NoAdjudicarFormValues = z.input<typeof noAdjudicarSchema>

/** Pedido listo para enviar: el motivo sin espacios sobrantes. */
export function aNoAdjudicarRequest(valores: NoAdjudicarValues): NoAdjudicarRequest {
  return { motivo: valores.motivo.trim() }
}
