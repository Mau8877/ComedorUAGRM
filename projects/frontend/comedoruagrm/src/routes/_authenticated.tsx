import { createFileRoute, Outlet } from '@tanstack/react-router'

import { etiquetaRol, exigirSesion, useCerrarSesion } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { ShellDeRol } from '@/layouts'

// Layout "pathless" (prefijo `_`) que envuelve todas las rutas privadas.
// Ninguna ruta nueva que necesite sesión se valida a mano por su cuenta:
// se agrega como hija de este archivo (carpeta `_authenticated/`) y hereda
// este guard. Ver .claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md.
//
// * La sesión viaja en cookies HttpOnly que el JavaScript no puede leer, así
// * que `exigirSesion` le pregunta al backend (GET /auth/usuario-actual,
// * renovando el token si hace falta). Sin sesión redirige a /login; con
// * sesión deja el `usuario` en el contexto de las rutas hijas.
export const Route = createFileRoute('/_authenticated')({
  beforeLoad: () => exigirSesion(),
  component: AuthenticatedLayout,
})

// * El marco del rol (header + sidebar) vive acá, en el layout padre: al navegar entre rutas hijas solo se
// * remonta el <Outlet/> (el contenido), no el header ni el sidebar. Ver layouts/shell/.
function AuthenticatedLayout() {
  const { usuario } = Route.useRouteContext()
  const cerrarSesion = useCerrarSesion()
  const acciones = useAccionesDeCuenta()

  return (
    <ShellDeRol
      rol={usuario.rol}
      user={{ nombre: `${usuario.nombres} ${usuario.apellidos}`, rol: etiquetaRol(usuario.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      onPerfil={acciones.onPerfil}
      onCambiarContrasena={acciones.onCambiarContrasena}
    >
      <Outlet />
    </ShellDeRol>
  )
}
