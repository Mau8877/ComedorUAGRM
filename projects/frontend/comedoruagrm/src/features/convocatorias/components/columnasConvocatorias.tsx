import type { ColumnDef } from '@tanstack/react-table'

import type { Convocatoria } from '../types'
import { EstadoConvocatoriaBadge } from './EstadoConvocatoriaBadge'
import { formatearFecha, formatearOpcional } from './formatos'

// * Una sola definición de columnas alimenta DataTable y DataCards. `enableSorting` solo donde el backend acepta el
// * campo en `sort`: nombre, fechaApertura, fechaCierre y estado. La gestión, los cupos, el puntaje y los
// * finalistas no son ordenables.
export const columnasConvocatorias: ColumnDef<Convocatoria, unknown>[] = [
  {
    accessorKey: 'nombre',
    header: 'Convocatoria',
    enableSorting: true,
    meta: { cardTitle: true, grow: true },
  },
  {
    id: 'gestion',
    header: 'Gestión',
    accessorFn: (fila) => fila.gestion.etiqueta,
    enableSorting: false,
    meta: { cardOrder: 0, width: 'w-32' },
  },
  {
    accessorKey: 'fechaApertura',
    header: 'Apertura',
    enableSorting: true,
    cell: (ctx) => formatearFecha(ctx.getValue<string>()),
    meta: { cardOrder: 1, width: 'w-32' },
  },
  {
    accessorKey: 'fechaCierre',
    header: 'Cierre',
    enableSorting: true,
    cell: (ctx) => formatearFecha(ctx.getValue<string>()),
    meta: { cardOrder: 2, width: 'w-32' },
  },
  {
    accessorKey: 'cupos',
    header: 'Cupos',
    enableSorting: false,
    cell: (ctx) => formatearOpcional(ctx.getValue<number | null>()),
    meta: { cardOrder: 3, width: 'w-24' },
  },
  {
    accessorKey: 'puntajeMinimo',
    header: 'Puntaje mín.',
    enableSorting: false,
    cell: (ctx) => formatearOpcional(ctx.getValue<number | null>()),
    meta: { cardOrder: 4, width: 'w-28' },
  },
  {
    accessorKey: 'cantidadFinalistas',
    header: 'Finalistas',
    enableSorting: false,
    cell: (ctx) => formatearOpcional(ctx.getValue<number | null>()),
    meta: { cardOrder: 5, width: 'w-28' },
  },
  {
    accessorKey: 'estado',
    header: 'Estado',
    enableSorting: true,
    cell: (ctx) => <EstadoConvocatoriaBadge estado={ctx.row.original.estado} />,
    meta: { cardOrder: 6, width: 'w-32' },
  },
]
