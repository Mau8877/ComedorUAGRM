// Contrato del backend de CU15 -- ver UT-BACKEND/README.md (PerfilResponse).

import type { Carrera } from '@/features/auth'

/** Datos que solo tiene el Postulante. */
export interface PerfilPostulante {
  registroUniversitario: string
  carrera: Carrera
  telefono: string
}

export interface Perfil {
  codigo: string
  nombres: string
  apellidos: string
  ci: string
  correo: string
  /** Nombre del rol tal cual lo manda el backend. */
  rol: string
  activo: boolean
  /** `null` si la cuenta no es de un Postulante. */
  postulante: PerfilPostulante | null
}
