import { createFileRoute } from '@tanstack/react-router'

import { redirigirSiHaySesion } from '@/features/auth'
import { LandingScreen } from '@/features/landing'

export const Route = createFileRoute('/')({
  beforeLoad: () => redirigirSiHaySesion(),
  component: LandingScreen,
})
