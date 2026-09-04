import { createFileRoute } from '@tanstack/react-router'

import { PruebaEstudianteLayout } from '@/features/pruebas_layout'

export const Route = createFileRoute('/pruebas-layout/estudiante')({
  component: PruebaEstudianteLayout,
  staticData: { breadcrumb: 'Mi Menú' },
})
