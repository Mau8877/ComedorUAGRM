import { createFileRoute, redirect } from '@tanstack/react-router'

import { PerfilScreen, tienePerfilWeb } from '@/features/perfil'

// * «Mi perfil» (CU15): para el Administrador DUBSS y el Postulante, que son los roles con layout web.
// * Los demás roles todavía no tienen módulo en la web: vuelven a /panel (el perfil de ellos está en la app
// * móvil). Es solo la capa de interfaz: el backend atiende /perfil para cualquier rol autenticado.
export const Route = createFileRoute('/_authenticated/perfil')({
  beforeLoad: ({ context }) => {
    if (!tienePerfilWeb(context.usuario.rol)) {
      throw redirect({ to: '/panel' })
    }
  },
  component: PerfilPage,
})

function PerfilPage() {
  const { usuario } = Route.useRouteContext()
  return <PerfilScreen usuario={usuario} />
}
