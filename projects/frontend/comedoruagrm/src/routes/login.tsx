import { createFileRoute } from '@tanstack/react-router'

import { LoginScreen, redirigirSiHaySesion } from '@/features/auth'

export const Route = createFileRoute('/login')({
  beforeLoad: () => redirigirSiHaySesion(),
  component: LoginScreen,
})
