import type { ColumnDef } from '@tanstack/react-table'

import type { DecisionResultado } from '../types'
import { formatearPuntaje } from './formatos'
import { ResultadoBadge } from './ResultadoBadge'

// * Una sola definición de columnas alimenta DataTable y DataCards. Se puede ordenar solo por los campos que el
// * backend documenta en `sort` (puntajeFinal, apellidos y nombres): el id de cada columna es ese campo.
export const columnasDecisiones: ColumnDef<DecisionResultado, unknown>[] = [
  {
    accessorKey: 'nombres',
    header: 'Nombre',
    enableSorting: true,
    meta: { cardTitle: true, grow: true },
  },
  {
    accessorKey: 'apellidos',
    header: 'Apellido',
    enableSorting: true,
    meta: { cardOrder: 1, width: 'w-48' },
  },
  {
    accessorKey: 'resultado',
    header: 'Resultado',
    enableSorting: false,
    cell: (ctx) => <ResultadoBadge resultado={ctx.row.original.resultado} />,
    meta: { cardOrder: 2, width: 'w-36' },
  },
  {
    accessorKey: 'puntajeFinal',
    header: 'Puntaje final',
    enableSorting: true,
    cell: (ctx) => formatearPuntaje(ctx.getValue<number | null>()),
    meta: { cardOrder: 3, width: 'w-36' },
  },
]
