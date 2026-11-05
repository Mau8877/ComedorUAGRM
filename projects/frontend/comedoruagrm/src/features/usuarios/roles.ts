import { etiquetaRol } from '@/features/auth'

/** Los tres roles que gestiona el CU14 (el Postulante no se gestiona desde acá). */
export const ROLES_PERSONAL = ['ADMINISTRADOR_DUBSS', 'DIRECTIVO', 'PERSONAL_COMEDOR'] as const

export type RolPersonal = (typeof ROLES_PERSONAL)[number]

/** Opciones de un select de rol, con la etiqueta de `etiquetaRol` (no se duplican los textos). */
export const OPCIONES_ROL_PERSONAL = ROLES_PERSONAL.map((rol) => ({ value: rol, label: etiquetaRol(rol) }))
