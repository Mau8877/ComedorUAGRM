import type { ColumnDef } from '@tanstack/react-table'

import type { PostulacionPriorizada } from '../types'

const FORMATO_FECHA = new Intl.DateTimeFormat('es-BO', { day: '2-digit', month: '2-digit', year: 'numeric' })

export function formatearPuntaje(puntaje: number): string {
  return Number(puntaje).toFixed(2)
}

export function formatearFecha(iso: string): string {
  return FORMATO_FECHA.format(new Date(iso))
}

// * Una sola definición de columnas alimenta DataTable y DataCards. `enableSorting` queda apagado
// * en todas: el orden es fijo (puntaje de mayor a menor) y el backend rechaza cualquier `sort`.
// * La posición es la real de la lista completa de la convocatoria (la manda el backend), no el
// * número de fila de la página: por eso no se usa `showRowNumber`.
export const columnasLista: ColumnDef<PostulacionPriorizada, unknown>[] = [
  {
    accessorKey: 'posicion',
    header: 'Posición',
    enableSorting: false,
    meta: { cardOrder: 0, width: 'w-24' },
  },
  {
    id: 'postulante',
    header: 'Postulante',
    accessorFn: (fila) => `${fila.nombres} ${fila.apellidos}`,
    enableSorting: false,
    meta: { cardTitle: true, grow: true },
  },
  {
    accessorKey: 'ci',
    header: 'CI',
    enableSorting: false,
    meta: { cardOrder: 1, width: 'w-32' },
  },
  {
    id: 'carrera',
    header: 'Carrera',
    accessorFn: (fila) => fila.carrera.nombre,
    enableSorting: false,
    meta: { cardOrder: 2, width: 'w-56' },
  },
  {
    accessorKey: 'puntaje',
    header: 'Puntaje',
    enableSorting: false,
    cell: (ctx) => formatearPuntaje(ctx.getValue<number>()),
    meta: { cardOrder: 3, width: 'w-28' },
  },
  {
    accessorKey: 'fechaCalculo',
    header: 'Calculado',
    enableSorting: false,
    cell: (ctx) => formatearFecha(ctx.getValue<string>()),
    meta: { cardOrder: 4, width: 'w-32' },
  },
]
