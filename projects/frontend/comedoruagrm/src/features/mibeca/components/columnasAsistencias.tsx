import type { ColumnDef } from '@tanstack/react-table'

import type { AsistenciaBecado } from '../types'
import { formatearDia, formatearHora } from './formatos'

// * Una sola definición de columnas alimenta DataTable y DataCards. `enableSorting` solo en la fecha: es el único
// * campo que el backend acepta en `sort` (ERR_MBE_03 con otro).
export const columnasAsistencias: ColumnDef<AsistenciaBecado, unknown>[] = [
  {
    accessorKey: 'fecha',
    header: 'Fecha',
    enableSorting: true,
    cell: (ctx) => formatearDia(ctx.getValue<string>()),
    meta: { cardTitle: true, grow: true },
  },
  {
    accessorKey: 'horaRegistro',
    header: 'Hora de registro',
    enableSorting: false,
    cell: (ctx) => formatearHora(ctx.getValue<string>()),
    meta: { cardOrder: 0, width: 'w-40' },
  },
]
