import { z } from 'zod'

// * Mismas reglas que el backend (MotivoRequest): el servidor vuelve a validar, esto es para dar feedback inmediato.
// * El motivo es obligatorio en la baja y en la suspensión (CU23, A1).
export const motivoSchema = z.object({
  motivo: z
    .string()
    .trim()
    .min(1, 'El motivo es obligatorio')
    .max(500, 'El motivo no puede superar los 500 caracteres'),
})

export type MotivoRequest = z.infer<typeof motivoSchema>
