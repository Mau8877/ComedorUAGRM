import { createFileRoute } from '@tanstack/react-router'

import { PruebaIngredientes } from '@/features/pruebas_layout'

export const Route = createFileRoute('/pruebas-layout/ingredientes')({
  component: PruebaIngredientes,
  staticData: { breadcrumb: 'Ingredientes' },
})
