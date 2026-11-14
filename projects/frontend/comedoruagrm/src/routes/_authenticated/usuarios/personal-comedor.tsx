import { createFileRoute } from '@tanstack/react-router'

import { UsuariosPersonalScreen } from '@/features/usuarios'

export const Route = createFileRoute('/_authenticated/usuarios/personal-comedor')({
  component: PersonalComedorPage,
})

function PersonalComedorPage() {
  const { usuario } = Route.useRouteContext()
  return (
    <UsuariosPersonalScreen
      rol="PERSONAL_COMEDOR"
      titulo="Personal del Comedor"
      activeHref="/usuarios/personal-comedor"
      usuarioActual={usuario}
    />
  )
}
