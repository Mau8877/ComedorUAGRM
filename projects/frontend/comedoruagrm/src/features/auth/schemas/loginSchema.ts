import { z } from 'zod'

// * Solo se valida lo mínimo: cualquier dato que no coincida da el mismo error
// * del backend (no se revela si falló el correo o la contraseña).
export const loginSchema = z.object({
  correo: z.string().trim().min(1, 'El correo electrónico es obligatorio').email('El correo electrónico no es válido'),
  contrasena: z.string().min(1, 'La contraseña es obligatoria'),
})

export type LoginFormValues = z.infer<typeof loginSchema>
