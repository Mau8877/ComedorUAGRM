import { createFileRoute } from '@tanstack/react-router'

import { RestablecerContrasenaScreen, redirigirSiHaySesion } from '@/features/auth'

// * El enlace del correo abre esta ruta con `?token=...` (CU15). La pantalla guarda el token en su estado
// * y lo quita de la barra de direcciones.
export const Route = createFileRoute('/restablecer-contrasena')({
  validateSearch: (search: Record<string, unknown>): { token?: string } => ({
    token: typeof search.token === 'string' && search.token.length > 0 ? search.token : undefined,
  }),
  beforeLoad: () => redirigirSiHaySesion(),
  component: RestablecerContrasenaPage,
})

function RestablecerContrasenaPage() {
  const { token } = Route.useSearch()
  return <RestablecerContrasenaScreen tokenInicial={token} />
}
