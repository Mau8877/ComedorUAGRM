import { z } from 'zod'

// * Misma política que el registro (CU01) y que el backend (RestablecerContrasenaRequest):
// * de 6 a 72 caracteres (72 es el máximo que procesa BCrypt), sin exigir mayúsculas ni
// * números, y coincidir con su confirmación. El token del enlace NO es un campo del
// * formulario: viene de la URL y se agrega al armar el request.
export const restablecerContrasenaSchema = z
  .object({
    contrasena: z
      .string()
      .min(1, 'La contraseña es obligatoria')
      .min(6, 'La contraseña debe tener al menos 6 caracteres')
      .max(72, 'La contraseña no puede superar los 72 caracteres'),
    confirmarContrasena: z.string().min(1, 'Confirma tu contraseña'),
  })
  .refine((valores) => valores.contrasena === valores.confirmarContrasena, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmarContrasena'],
  })

export type RestablecerContrasenaFormValues = z.infer<typeof restablecerContrasenaSchema>

/** Lo que espera `POST /auth/restablecer-contrasena`: la confirmación viaja como `confirmacionContrasena`. */
export interface RestablecerContrasenaRequest {
  token: string
  contrasena: string
  confirmacionContrasena: string
}

export function aRestablecerRequest(
  valores: RestablecerContrasenaFormValues,
  token: string,
): RestablecerContrasenaRequest {
  return { token, contrasena: valores.contrasena, confirmacionContrasena: valores.confirmarContrasena }
}
