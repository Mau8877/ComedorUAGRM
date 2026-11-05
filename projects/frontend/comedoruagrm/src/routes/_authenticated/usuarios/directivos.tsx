import { createFileRoute } from '@tanstack/react-router'

import { UsuariosPersonalScreen } from '@/features/usuarios'

export const Route = createFileRoute('/_authenticated/usuarios/directivos')({
  component: DirectivosPage,
})

function DirectivosPage() {
  const { usuario } = Route.useRouteContext()
  return (
    <UsuariosPersonalScreen
      rol="DIRECTIVO"
      titulo="Directivos"
      activeHref="/usuarios/directivos"
      usuarioActual={usuario}
    />
  )
}
