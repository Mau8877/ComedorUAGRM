import type { ColumnDef } from '@tanstack/react-table'

import { Checkbox } from '@/components/ui/checkbox'

import type { EstadoNotificacion, Finalista, ResultadoAuditoriaFinalista } from '../types'
import { AuditoriaBadge, DevueltaBadge, EntregaBadge, EstadoNotificacionBadge } from './EstadoBadges'
import { formatearPuntaje } from './formatos'

export interface OpcionesColumnas {
  /** Códigos de los finalistas marcados (de cualquier página). */
  seleccionados: ReadonlySet<string>
  /** Códigos de los finalistas de la página que se ve. */
  codigosPagina: string[]
  onToggle: (codigo: string, marcado: boolean) => void
  onTogglePagina: (marcado: boolean) => void
}

// * Una sola definición de columnas alimenta DataTable y DataCards. `enableSorting` queda apagado en todas:
// * el orden es fijo (por posición) y el backend rechaza cualquier `sort`. La posición, el puntaje y los
// * estados son los que manda el backend: no se calculan en el cliente.
export function crearColumnasFinalistas({
  seleccionados,
  codigosPagina,
  onToggle,
  onTogglePagina,
}: OpcionesColumnas): ColumnDef<Finalista, unknown>[] {
  const marcadosEnPagina = codigosPagina.filter((codigo) => seleccionados.has(codigo)).length
  const todosMarcados = codigosPagina.length > 0 && marcadosEnPagina === codigosPagina.length

  return [
    {
      id: 'seleccion',
      enableSorting: false,
      header: () => (
        <Checkbox
          aria-label="Seleccionar todos los de esta página"
          checked={todosMarcados}
          indeterminate={marcadosEnPagina > 0 && !todosMarcados}
          onCheckedChange={(marcado) => onTogglePagina(Boolean(marcado))}
        />
      ),
      cell: ({ row }) => (
        <Checkbox
          aria-label={`Seleccionar a ${row.original.nombres} ${row.original.apellidos}`}
          checked={seleccionados.has(row.original.codigo)}
          onCheckedChange={(marcado) => onToggle(row.original.codigo, Boolean(marcado))}
        />
      ),
      meta: { width: 'w-12', cardOrder: -1, cardLabel: 'Seleccionar' },
    },
    {
      accessorKey: 'posicion',
      header: 'Pos.',
      enableSorting: false,
      meta: { cardOrder: 0, width: 'w-16' },
    },
    {
      id: 'finalista',
      header: 'Finalista',
      accessorFn: (fila) => `${fila.nombres} ${fila.apellidos}`,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex flex-col">
          <span>{`${row.original.nombres} ${row.original.apellidos}`}</span>
          <span className="text-xs text-muted-foreground">CI {row.original.ci}</span>
        </div>
      ),
      meta: { cardTitle: true, grow: true },
    },
    {
      accessorKey: 'puntaje',
      header: 'Puntaje',
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex flex-col">
          <span>{formatearPuntaje(row.original.puntaje)}</span>
          {row.original.puntajeTotal !== null && (
            <span className="text-xs text-muted-foreground">Total {formatearPuntaje(row.original.puntajeTotal)}</span>
          )}
        </div>
      ),
      meta: { cardOrder: 1, width: 'w-28' },
    },
    {
      accessorKey: 'estadoNotificacion',
      header: 'Notificación',
      enableSorting: false,
      cell: (ctx) => <EstadoNotificacionBadge estado={ctx.getValue<EstadoNotificacion>()} />,
      meta: { cardOrder: 2, width: 'w-32' },
    },
    {
      id: 'documentacion',
      header: 'Doc. física',
      accessorFn: (fila) => (fila.documentacionRecibidaEn ? 'Recibida' : 'Pendiente'),
      enableSorting: false,
      cell: ({ row }) => <EntregaBadge recibida={Boolean(row.original.documentacionRecibidaEn)} />,
      meta: { cardOrder: 3, width: 'w-32' },
    },
    {
      accessorKey: 'resultadoAuditoria',
      header: 'Auditoría',
      enableSorting: false,
      cell: (ctx) =>
        ctx.row.original.estadoPostulacion === 'devuelta' ? (
          <DevueltaBadge />
        ) : (
          <AuditoriaBadge resultado={ctx.getValue<ResultadoAuditoriaFinalista>()} />
        ),
      meta: { cardOrder: 4, width: 'w-32' },
    },
  ]
}
