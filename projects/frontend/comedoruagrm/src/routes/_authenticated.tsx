import { createFileRoute, Outlet } from '@tanstack/react-router'

import { exigirSesion } from '@/features/auth'

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

function AuthenticatedLayout() {
  return <Outlet />
}
