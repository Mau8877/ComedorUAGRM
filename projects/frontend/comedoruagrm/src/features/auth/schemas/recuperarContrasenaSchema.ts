import { z } from 'zod'

// * Misma regla que el backend (RecuperarContrasenaRequest): solo el correo. El servidor
// * responde siempre igual exista o no la cuenta, así que acá no se valida nada más.
export const recuperarContrasenaSchema = z.object({
  correo: z
    .string()
    .trim()
    .min(1, 'El correo electrónico es obligatorio')
    .max(150, 'El correo no puede superar los 150 caracteres')
    .email('El correo electrónico no es válido'),
})

export type RecuperarContrasenaFormValues = z.infer<typeof recuperarContrasenaSchema>
