import type { ColumnDef } from '@tanstack/react-table'

import type { AdjudicadoPublico } from '../types'
import { formatearPuntaje } from './formatos'

// * Una sola definición de columnas alimenta DataTable y DataCards. Solo se ordena por los campos que el backend
// * documenta en `sort` de la lista pública (nota, apellidos y nombres): el id de cada columna es ese campo.
// * Nada de CI, correo ni motivo: la lista pública solo trae estos cinco datos.
export const columnasAdjudicados: ColumnDef<AdjudicadoPublico, unknown>[] = [
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
    meta: { cardOrder: 1, width: 'w-44' },
  },
  {
    accessorKey: 'nota',
    header: 'Nota',
    enableSorting: true,
    cell: (ctx) => formatearPuntaje(ctx.getValue<number | null>()),
    meta: { cardOrder: 2, width: 'w-28' },
  },
  {
    accessorKey: 'carrera',
    header: 'Carrera',
    enableSorting: false,
    meta: { cardOrder: 3, width: 'w-56' },
  },
  {
    accessorKey: 'facultad',
    header: 'Facultad',
    enableSorting: false,
    meta: { cardOrder: 4, width: 'w-56' },
  },
]
