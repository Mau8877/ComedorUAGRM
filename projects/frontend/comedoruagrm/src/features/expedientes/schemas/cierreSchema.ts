import { z } from 'zod'

import type { CerrarPostulacionRequest } from '../types'
import { MAX_MOTIVO } from './devolucionSchema'

// * Mismas reglas que el backend (CierrePostulacionRequest): el motivo es obligatorio (sin contar los
// * espacios de los extremos) y de hasta 500 caracteres. Sirve para archivar y para rechazar.

export const cierreSchema = z.object({
  motivo: z
    .string()
    .trim()
    .min(1, 'El motivo es obligatorio')
    .max(MAX_MOTIVO, 'El motivo no puede pasar de 500 caracteres'),
})

export type CierreValues = z.output<typeof cierreSchema>
export type CierreFormValues = z.input<typeof cierreSchema>

/** Pedido listo para enviar: el motivo sin espacios sobrantes. */
export function aCierreRequest(valores: CierreValues): CerrarPostulacionRequest {
  return { motivo: valores.motivo.trim() }
}
