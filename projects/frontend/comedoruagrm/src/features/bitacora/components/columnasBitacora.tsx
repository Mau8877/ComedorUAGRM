import type { ColumnDef } from '@tanstack/react-table'

import type { RegistroBitacora } from '../types'

const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  timeZone: 'America/La_Paz',
})

/** «30/09/2026, 10:42:05» en la zona de La Paz. */
export function formatearFechaHora(iso: string): string {
  return FORMATO_FECHA_HORA.format(new Date(iso))
}

// * Una sola definición de columnas alimenta DataTable y DataCards. `enableSorting` solo en fecha, acción y
// * entidad: son los campos que el backend acepta en `sort`.
export const columnasBitacora: ColumnDef<RegistroBitacora, unknown>[] = [
  {
    accessorKey: 'fecha',
    header: 'Fecha y hora',
    enableSorting: true,
    cell: (ctx) => formatearFechaHora(ctx.getValue<string>()),
    meta: { cardTitle: true, width: 'w-48' },
  },
  {
    id: 'usuario',
    header: 'Usuario',
    accessorFn: (fila) => (fila.usuario ? `${fila.usuario.nombres} ${fila.usuario.apellidos}` : 'Sistema'),
    enableSorting: false,
    meta: { grow: true, cardOrder: 0 },
  },
  {
    accessorKey: 'accion',
    header: 'Acción',
    enableSorting: true,
    meta: { cardOrder: 1 },
  },
  {
    accessorKey: 'entidad',
    header: 'Entidad',
    enableSorting: true,
    cell: (ctx) => ctx.getValue<string | null>() ?? '—',
    meta: { cardOrder: 2, width: 'w-36' },
  },
  {
    accessorKey: 'ip',
    header: 'IP',
    enableSorting: false,
    cell: (ctx) => ctx.getValue<string | null>() ?? '—',
    meta: { cardOrder: 3, width: 'w-36' },
  },
]
