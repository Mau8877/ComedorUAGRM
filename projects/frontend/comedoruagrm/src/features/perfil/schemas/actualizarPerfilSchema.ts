import { z } from 'zod'

interface OpcionesSchema {
  /** Correo con el que está registrada la cuenta: si cambia, se exige la contraseña actual (A7). */
  correoActual: string
  /** Solo el Postulante tiene teléfono; para los demás roles el campo ni se muestra. */
  esPostulante: boolean
}

function mismoCorreo(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase()
}

// * Mismas reglas que el backend (ActualizarPerfilRequest): el servidor vuelve a validar.
// * La contraseña actual es obligatoria SOLO si el correo cambió. El CI, nombres, apellidos,
// * registro universitario y carrera no se editan: ni siquiera son campos de este formulario.
export function crearActualizarPerfilSchema({ correoActual, esPostulante }: OpcionesSchema) {
  return z
    .object({
      correo: z
        .string()
        .trim()
        .min(1, 'El correo electrónico es obligatorio')
        .max(150, 'El correo no puede superar los 150 caracteres')
        .email('El correo electrónico no es válido'),
      telefono: z.string().trim().max(20, 'El teléfono no puede superar los 20 caracteres'),
      contrasenaActual: z.string(),
    })
    .superRefine((valores, ctx) => {
      if (esPostulante && valores.telefono.length === 0) {
        ctx.addIssue({ code: 'custom', path: ['telefono'], message: 'El teléfono es obligatorio' })
      }
      if (!mismoCorreo(valores.correo, correoActual) && valores.contrasenaActual.length === 0) {
        ctx.addIssue({
          code: 'custom',
          path: ['contrasenaActual'],
          message: 'Ingresa tu contraseña actual para cambiar el correo',
        })
      }
    })
}

export type ActualizarPerfilFormValues = z.infer<ReturnType<typeof crearActualizarPerfilSchema>>

/** Lo que espera `PATCH /perfil`: solo los campos que cambiaron (el CI nunca viaja). */
export interface ActualizarPerfilRequest {
  correo?: string
  telefono?: string
  contrasenaActual?: string
}

export function aActualizarPerfilRequest(
  valores: ActualizarPerfilFormValues,
  original: { correo: string; telefono?: string },
): ActualizarPerfilRequest {
  const request: ActualizarPerfilRequest = {}
  if (!mismoCorreo(valores.correo, original.correo)) {
    request.correo = valores.correo.trim()
    request.contrasenaActual = valores.contrasenaActual
  }
  if (original.telefono !== undefined && valores.telefono.trim() !== original.telefono) {
    request.telefono = valores.telefono.trim()
  }
  return request
}
