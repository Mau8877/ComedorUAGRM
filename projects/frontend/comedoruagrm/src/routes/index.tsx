import { createFileRoute } from '@tanstack/react-router'

import { LandingScreen } from '@/features/landing'

export const Route = createFileRoute('/')({
  component: LandingScreen,
})
