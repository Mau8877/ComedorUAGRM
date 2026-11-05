import { useNavigate } from '@tanstack/react-router'

/** Roles con layout web (los demás todavía no tienen módulo, así que tampoco perfil en la web). */
const ROLES_CON_PERFIL_WEB = ['ADMINISTRADOR_DUBSS', 'POSTULANTE']

export function tienePerfilWeb(rol: string): boolean {
  return ROLES_CON_PERFIL_WEB.includes(rol)
}

/**
 * Acciones de los ítems «Mi perfil» y «Cambiar contraseña» del menú de usuario del header (CU15).
 * Quien arma un layout las pasa como `onPerfil` y `onCambiarContrasena`; el header no sabe navegar.
 */
export function useAccionesDeCuenta() {
  const navigate = useNavigate()

  return {
    onPerfil: () => void navigate({ to: '/perfil' }),
    onCambiarContrasena: () => void navigate({ to: '/perfil', hash: 'cambiar-contrasena' }),
  }
}
