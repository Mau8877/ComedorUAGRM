import type { ColumnDef } from '@tanstack/react-table'

import type { EstadoPostulacion, MiPostulacionResumen } from '../types'
import { EstadoPostulacionBadge } from './EstadoPostulacionBadge'
import { formatearFecha } from './formatos'

// * Una sola definición de columnas alimenta DataTable y DataCards. Se puede ordenar solo por `fechaEnvio`, el
// * único campo que el backend documenta en `sort`: el id de esa columna es ese campo.
export const columnasPostulaciones: ColumnDef<MiPostulacionResumen, unknown>[] = [
  {
    id: 'convocatoria',
    header: 'Convocatoria',
    accessorFn: (postulacion) => postulacion.convocatoria.nombre,
    enableSorting: false,
    meta: { cardTitle: true, grow: true },
  },
  {
    id: 'gestionAcademica',
    header: 'Gestión',
    accessorFn: (postulacion) => postulacion.gestionAcademica.nombre,
    enableSorting: false,
    meta: { cardOrder: 1, width: 'w-28' },
  },
  {
    accessorKey: 'fechaEnvio',
    header: 'Fecha de envío',
    enableSorting: true,
    cell: (ctx) => formatearFecha(ctx.getValue<string>()),
    meta: { cardOrder: 2, width: 'w-40' },
  },
  {
    accessorKey: 'estado',
    header: 'Estado',
    enableSorting: false,
    cell: (ctx) => <EstadoPostulacionBadge estado={ctx.getValue<EstadoPostulacion>()} />,
    meta: { cardOrder: 3, width: 'w-36' },
  },
]
