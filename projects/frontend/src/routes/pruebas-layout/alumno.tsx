import { createFileRoute } from '@tanstack/react-router'

import { PruebaAlumnoLayout } from '@/features/pruebas_layout'

export const Route = createFileRoute('/pruebas-layout/alumno')({
  component: PruebaAlumnoLayout,
})
