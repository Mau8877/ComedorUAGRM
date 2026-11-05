import { z } from 'zod'

import { ROLES_PERSONAL } from '../roles'

// * Mismas reglas que el backend (CrearUsuarioPersonalRequest y compañía): el
// * servidor vuelve a validar, esto es para dar feedback inmediato campo por campo.
// * Formato del CI: por definir con la DUBSS, solo no vacío y largo máximo (igual que el registro).

const nombres = z.string().trim().min(1, 'Los nombres son obligatorios').max(100, 'Los nombres no pueden superar los 100 caracteres')
const apellidos = z.string().trim().min(1, 'Los apellidos son obligatorios').max(100, 'Los apellidos no pueden superar los 100 caracteres')
const correo = z
  .string()
  .trim()
  .min(1, 'El correo electrónico es obligatorio')
  .max(150, 'El correo no puede superar los 150 caracteres')
  .email('El correo electrónico no es válido')
const rol = z.enum(ROLES_PERSONAL, { message: 'Selecciona un rol del personal' })

export const crearUsuarioPersonalSchema = z.object({
  nombres,
  apellidos,
  ci: z.string().trim().min(1, 'El CI es obligatorio').max(20, 'El CI no puede superar los 20 caracteres'),
  correo,
  rol,
})

export const editarUsuarioPersonalSchema = z.object({ nombres, apellidos, correo })

export const cambiarRolSchema = z.object({ rol })

export type CrearUsuarioPersonalRequest = z.infer<typeof crearUsuarioPersonalSchema>
export type EditarUsuarioPersonalRequest = z.infer<typeof editarUsuarioPersonalSchema>
export type CambiarRolRequest = z.infer<typeof cambiarRolSchema>
