import { z } from 'zod'

// * Mismas reglas que el backend (CambiarContrasenaRequest): la actual no vacía, la nueva de
// * 6 a 72 caracteres (política del CU01) y coincidir con su confirmación.
export const cambiarContrasenaSchema = z
  .object({
    contrasenaActual: z.string().min(1, 'La contraseña actual es obligatoria'),
    contrasenaNueva: z
      .string()
      .min(1, 'La contraseña nueva es obligatoria')
      .min(6, 'La contraseña debe tener al menos 6 caracteres')
      .max(72, 'La contraseña no puede superar los 72 caracteres'),
    confirmarContrasena: z.string().min(1, 'Confirma tu contraseña'),
  })
  .refine((valores) => valores.contrasenaNueva === valores.confirmarContrasena, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmarContrasena'],
  })

export type CambiarContrasenaFormValues = z.infer<typeof cambiarContrasenaSchema>

/** Lo que espera `POST /auth/cambiar-contrasena`. El refresh token NO viaja: la cookie HttpOnly lo manda sola. */
export interface CambiarContrasenaRequest {
  contrasenaActual: string
  contrasenaNueva: string
  confirmacionContrasena: string
}

export function aCambiarContrasenaRequest(valores: CambiarContrasenaFormValues): CambiarContrasenaRequest {
  return {
    contrasenaActual: valores.contrasenaActual,
    contrasenaNueva: valores.contrasenaNueva,
    confirmacionContrasena: valores.confirmarContrasena,
  }
}
