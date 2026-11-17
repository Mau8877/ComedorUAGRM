import type { ColumnDef } from '@tanstack/react-table'

import type { GestionAcademica } from '../types'
import { formatearFecha } from './formatos'

// * Una sola definición de columnas alimenta DataTable y DataCards. Las tres son ordenables en el backend
// * (`etiqueta`, `fechaInicio`, `fechaFin`).
export const columnasGestiones: ColumnDef<GestionAcademica, unknown>[] = [
  {
    accessorKey: 'etiqueta',
    header: 'Gestión',
    enableSorting: true,
    meta: { cardTitle: true, grow: true },
  },
  {
    accessorKey: 'fechaInicio',
    header: 'Inicio',
    enableSorting: true,
    cell: (ctx) => formatearFecha(ctx.getValue<string>()),
    meta: { cardOrder: 0, width: 'w-40' },
  },
  {
    accessorKey: 'fechaFin',
    header: 'Fin',
    enableSorting: true,
    cell: (ctx) => formatearFecha(ctx.getValue<string>()),
    meta: { cardOrder: 1, width: 'w-40' },
  },
]
