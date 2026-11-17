import type { ColumnDef } from '@tanstack/react-table'

import type { ModeloResumen } from '../types'
import { EstadoVersionBadge } from './EstadoVersionBadge'
import { formatearFecha } from './formatos'

// * Una sola definición de columnas alimenta DataTable y DataCards. El historial viene ordenado por el backend (la
// * versión más reciente primero) y no admite `sort`, así que ninguna columna es ordenable.
export const columnasHistorial: ColumnDef<ModeloResumen, unknown>[] = [
  {
    accessorKey: 'version',
    header: 'Versión',
    enableSorting: false,
    meta: { cardTitle: true, grow: true },
  },
  {
    accessorKey: 'creadoEn',
    header: 'Fecha',
    enableSorting: false,
    cell: (ctx) => formatearFecha(ctx.getValue<string>()),
    meta: { cardOrder: 0, width: 'w-32' },
  },
  {
    accessorKey: 'vigente',
    header: 'Estado',
    enableSorting: false,
    cell: (ctx) => <EstadoVersionBadge vigente={ctx.getValue<boolean>()} />,
    meta: { cardOrder: 1, width: 'w-32' },
  },
]
