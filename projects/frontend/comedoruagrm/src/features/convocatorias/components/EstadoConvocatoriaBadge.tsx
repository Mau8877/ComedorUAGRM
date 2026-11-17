import { cn } from '@/lib/utils'

import type { EstadoConvocatoria } from '../types'
import { ESTILOS_ESTADO } from './estados'

export function EstadoConvocatoriaBadge({ estado }: { estado: EstadoConvocatoria }) {
  const estilo = ESTILOS_ESTADO[estado]
  return (
    <span
      className={cn(
        'inline-flex w-fit items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium whitespace-nowrap',
        estilo.fondo,
      )}
    >
      <span className={cn('size-1.5 rounded-full', estilo.punto)} />
      {estilo.etiqueta}
    </span>
  )
}
