import { createFileRoute, Outlet } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'

// * Layout de la gestión de cuentas: solo el Administrador DUBSS. Las rutas hijas
// * heredan este guard; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/usuarios')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: Outlet,
})
