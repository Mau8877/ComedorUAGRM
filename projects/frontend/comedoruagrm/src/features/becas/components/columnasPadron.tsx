import type { ColumnDef } from '@tanstack/react-table'

import type { Beca } from '../types'
import { EstadoBecaBadge } from './EstadoBecaBadge'
import { formatearFecha } from './formatos'

// * Una sola definición de columnas alimenta DataTable y DataCards. Los `id` de las columnas ordenables son los
// * mismos nombres que el backend acepta en `sort`: becado, ci, carrera, estado y fechaInicio.
export const columnasPadron: ColumnDef<Beca, unknown>[] = [
  {
    id: 'becado',
    header: 'Becado',
    accessorFn: (fila) => `${fila.becado.nombres} ${fila.becado.apellidos}`,
    enableSorting: true,
    meta: { cardTitle: true, grow: true },
  },
  {
    id: 'ci',
    header: 'CI',
    accessorFn: (fila) => fila.becado.ci,
    enableSorting: true,
    meta: { cardOrder: 0, width: 'w-32' },
  },
  {
    id: 'carrera',
    header: 'Carrera',
    accessorFn: (fila) => fila.becado.carrera,
    enableSorting: true,
    meta: { cardOrder: 1, width: 'w-52' },
  },
  {
    accessorKey: 'estado',
    header: 'Estado',
    enableSorting: true,
    cell: (ctx) => <EstadoBecaBadge estado={ctx.row.original.estado} />,
    meta: { cardOrder: 2, width: 'w-36' },
  },
  {
    accessorKey: 'fechaInicio',
    header: 'Inicio',
    enableSorting: true,
    cell: (ctx) => formatearFecha(ctx.getValue<string>()),
    meta: { cardOrder: 3, width: 'w-32' },
  },
]
