import type { ColumnDef } from '@tanstack/react-table'

import type { Cotejo, RegistroDigital } from '../types'
import { etiquetaResultado, formatearFecha, formatearFechaHora, formatearHora } from './formatos'

// * Una sola definición de columnas alimenta DataTable y DataCards. El id de una columna ordenable es el campo que
// * el backend documenta en `sort` (registros: `fecha` y `apellidos`; cotejos: `fechaCotejo` y `creadoEn`).

export const columnasRegistros: ColumnDef<RegistroDigital, unknown>[] = [
  {
    id: 'apellidos',
    header: 'Becado',
    accessorFn: (r) => `${r.apellidos} ${r.nombres}`,
    enableSorting: true,
    meta: { cardTitle: true, grow: true },
  },
  {
    accessorKey: 'carrera',
    header: 'Carrera',
    enableSorting: false,
    meta: { cardOrder: 1, width: 'w-48' },
  },
  {
    id: 'fecha',
    header: 'Fecha',
    accessorFn: (r) => formatearFecha(r.fecha),
    enableSorting: true,
    meta: { cardOrder: 2, width: 'w-32' },
  },
  {
    accessorKey: 'horaRegistro',
    header: 'Hora (digital)',
    enableSorting: false,
    cell: (ctx) => formatearHora(ctx.getValue<string>()),
    meta: { cardOrder: 3, width: 'w-32' },
  },
]

export const columnasCotejos: ColumnDef<Cotejo, unknown>[] = [
  {
    id: 'creadoEn',
    header: 'Registrado',
    accessorFn: (c) => formatearFechaHora(c.creadoEn),
    enableSorting: true,
    meta: { cardTitle: true, grow: true },
  },
  {
    accessorKey: 'resultado',
    header: 'Resultado',
    enableSorting: false,
    cell: (ctx) => etiquetaResultado(ctx.getValue<Cotejo['resultado']>()),
    meta: { cardOrder: 1, width: 'w-40' },
  },
  {
    accessorKey: 'totalRegistros',
    header: 'Registros',
    enableSorting: false,
    meta: { cardOrder: 2, width: 'w-28' },
  },
  {
    accessorKey: 'coincidencias',
    header: 'Coincidencias',
    enableSorting: false,
    meta: { cardOrder: 3, width: 'w-36' },
  },
  {
    accessorKey: 'discrepancias',
    header: 'Discrepancias',
    enableSorting: false,
    meta: { cardOrder: 4, width: 'w-36' },
  },
  {
    accessorKey: 'realizadoPor',
    header: 'Realizado por',
    enableSorting: false,
    meta: { cardOrder: 5, width: 'w-44' },
  },
  {
    accessorKey: 'observaciones',
    header: 'Observaciones',
    enableSorting: false,
    cell: (ctx) => ctx.getValue<string | null>() ?? '—',
    meta: { cardOrder: 6, width: 'w-56' },
  },
]
