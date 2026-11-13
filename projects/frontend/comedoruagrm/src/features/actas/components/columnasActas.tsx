import type { ColumnDef } from '@tanstack/react-table'

import type { Acta } from '../types'
import { formatearFechaHora } from './formatos'

// * Una sola definición de columnas alimenta DataTable y DataCards. Se puede ordenar solo por los campos que el
// * backend documenta en `sort` (numero y generadaEn): el id de cada columna es ese campo.
export const columnasActas: ColumnDef<Acta, unknown>[] = [
  {
    accessorKey: 'numero',
    header: 'N.º',
    enableSorting: true,
    meta: { cardTitle: true, width: 'w-32' },
  },
  {
    id: 'convocatoria',
    header: 'Convocatoria',
    accessorFn: (acta) => acta.convocatoria.nombre,
    enableSorting: false,
    meta: { cardOrder: 1, grow: true },
  },
  {
    accessorKey: 'gestion',
    header: 'Gestión',
    enableSorting: false,
    meta: { cardOrder: 2, width: 'w-28' },
  },
  {
    accessorKey: 'adjudicados',
    header: 'Adjudicados',
    enableSorting: false,
    meta: { cardOrder: 3, width: 'w-32' },
  },
  {
    accessorKey: 'generadaEn',
    header: 'Fecha',
    enableSorting: true,
    cell: (ctx) => formatearFechaHora(ctx.getValue<string>()),
    meta: { cardOrder: 4, width: 'w-44' },
  },
  {
    accessorKey: 'generadaPor',
    header: 'Generada por',
    enableSorting: false,
    meta: { cardOrder: 5, width: 'w-44' },
  },
]
