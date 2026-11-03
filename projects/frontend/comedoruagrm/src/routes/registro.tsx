import { createFileRoute } from '@tanstack/react-router'

import { RegistroScreen, redirigirSiHaySesion } from '@/features/auth'

export const Route = createFileRoute('/registro')({
  beforeLoad: () => redirigirSiHaySesion(),
  component: RegistroScreen,
})
