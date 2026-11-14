import { z } from 'zod'

// * Mismas reglas que el backend (RegistroRequest): el servidor vuelve a validar,
// * esto es para dar feedback inmediato campo por campo.
// * - La contraseña: de 6 a 72 caracteres, sin exigir mayúsculas ni números
// *   (72 es el máximo que procesa BCrypt).
// * - Registro universitario: exactamente 9 dígitos y sin cero inicial.
// * - Formato del CI: por definir con la DUBSS, solo no vacío y largo máximo.
export const registroSchema = z
  .object({
    nombres: z.string().trim().min(1, 'Los nombres son obligatorios').max(100, 'Los nombres no pueden superar los 100 caracteres'),
    apellidos: z.string().trim().min(1, 'Los apellidos son obligatorios').max(100, 'Los apellidos no pueden superar los 100 caracteres'),
    ci: z.string().trim().min(1, 'El CI es obligatorio').max(20, 'El CI no puede superar los 20 caracteres'),
    registroUniversitario: z
      .string()
      .min(1, 'El registro universitario es obligatorio')
      .regex(/^[1-9]\d{8}$/, 'El registro universitario debe tener 9 dígitos y no empezar por 0'),
    carreraCodigo: z.string().min(1, 'La carrera es obligatoria'),
    telefono: z.string().trim().min(1, 'El teléfono es obligatorio').max(20, 'El teléfono no puede superar los 20 caracteres'),
    correo: z
      .string()
      .trim()
      .min(1, 'El correo electrónico es obligatorio')
      .max(150, 'El correo no puede superar los 150 caracteres')
      .email('El correo electrónico no es válido'),
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

export type RegistroFormValues = z.infer<typeof registroSchema>

/** Lo que espera `POST /auth/registro`: el campo del formulario `confirmarContrasena` viaja como `confirmacionContrasena`. */
export interface RegistroRequest {
  nombres: string
  apellidos: string
  ci: string
  registroUniversitario: string
  carreraCodigo: string
  telefono: string
  correo: string
  contrasena: string
  confirmacionContrasena: string
}

export function aRegistroRequest(valores: RegistroFormValues): RegistroRequest {
  return {
    nombres: valores.nombres.trim(),
    apellidos: valores.apellidos.trim(),
    ci: valores.ci.trim(),
    registroUniversitario: valores.registroUniversitario,
    carreraCodigo: valores.carreraCodigo,
    telefono: valores.telefono.trim(),
    correo: valores.correo.trim(),
    contrasena: valores.contrasena,
    confirmacionContrasena: valores.confirmarContrasena,
  }
}
