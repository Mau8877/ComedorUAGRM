import { createFileRoute } from '@tanstack/react-router'

import { RecuperarContrasenaScreen, redirigirSiHaySesion } from '@/features/auth'

export const Route = createFileRoute('/recuperar-contrasena')({
  beforeLoad: () => redirigirSiHaySesion(),
  component: RecuperarContrasenaScreen,
})
