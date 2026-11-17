import type { ColumnDef } from '@tanstack/react-table'

import type { Planilla } from '../types'
import { formatearFechaHora, formatearPeriodo } from './formatos'

// * Una sola definición de columnas alimenta DataTable y DataCards. Se puede ordenar solo por los campos que el
// * backend documenta en `sort` (fechaDesde y generadaEn): el id de cada columna es ese campo.
export const columnasPlanillas: ColumnDef<Planilla, unknown>[] = [
  {
    id: 'fechaDesde',
    header: 'Periodo',
    accessorFn: (planilla) => formatearPeriodo(planilla.fechaDesde, planilla.fechaHasta),
    enableSorting: true,
    meta: { cardTitle: true, grow: true },
  },
  {
    accessorKey: 'totalBecados',
    header: 'Becados',
    enableSorting: false,
    meta: { cardOrder: 1, width: 'w-28' },
  },
  {
    accessorKey: 'generadaEn',
    header: 'Generada',
    enableSorting: true,
    cell: (ctx) => formatearFechaHora(ctx.getValue<string>()),
    meta: { cardOrder: 2, width: 'w-44' },
  },
  {
    accessorKey: 'generadaPor',
    header: 'Generada por',
    enableSorting: false,
    meta: { cardOrder: 3, width: 'w-44' },
  },
]
