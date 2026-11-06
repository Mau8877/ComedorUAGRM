// Contrato del backend de CU01 -- ver UT-BACKEND/README.md (UsuarioResponse y SesionResponse).

export type RolUsuario = 'POSTULANTE' | 'ADMINISTRADOR_DUBSS' | 'DIRECTIVO' | 'PERSONAL_COMEDOR'

export interface Usuario {
  codigo: string
  nombres: string
  apellidos: string
  correo: string
  /** Nombre del rol tal cual lo manda el backend (ver RolUsuario); un valor desconocido se trata como sin módulo. */
  rol: string
}

/**
 * Respuesta de registro, login y refresh. Los tokens son para mobile: la web
 * los ignora, la sesión viaja en cookies HttpOnly (ver SEGURIDAD_AUTH_BACKEND.md).
 */
export interface Sesion {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  usuario: Usuario
}

// Re-exportada: el catálogo vive en shared/api (lo usan el registro y la lista priorizada).
export type { Carrera } from '@/shared/api'
