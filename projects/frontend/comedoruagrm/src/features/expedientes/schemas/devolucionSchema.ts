import { z } from 'zod'

import type { DevolverPostulacionRequest } from '../types'

// * Mismas reglas que el backend (DevolverPostulacionRequest): el motivo es obligatorio (sin contar los
// * espacios de los extremos) y de hasta 500 caracteres.

export const MAX_MOTIVO = 500

export const devolucionSchema = z.object({
  motivo: z
    .string()
    .trim()
    .min(1, 'El motivo es obligatorio')
    .max(MAX_MOTIVO, 'El motivo no puede pasar de 500 caracteres'),
})

export type DevolucionValues = z.output<typeof devolucionSchema>
export type DevolucionFormValues = z.input<typeof devolucionSchema>

/** Pedido listo para enviar: el motivo sin espacios sobrantes. */
export function aDevolucionRequest(valores: DevolucionValues): DevolverPostulacionRequest {
  return { motivo: valores.motivo.trim() }
}
