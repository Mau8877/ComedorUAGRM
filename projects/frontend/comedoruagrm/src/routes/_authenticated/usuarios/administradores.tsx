import { createFileRoute } from '@tanstack/react-router'

import { UsuariosPersonalScreen } from '@/features/usuarios'

export const Route = createFileRoute('/_authenticated/usuarios/administradores')({
  component: AdministradoresPage,
})

function AdministradoresPage() {
  const { usuario } = Route.useRouteContext()
  return (
    <UsuariosPersonalScreen
      rol="ADMINISTRADOR_DUBSS"
      titulo="Administradores DUBSS"
      activeHref="/usuarios/administradores"
      usuarioActual={usuario}
    />
  )
}
