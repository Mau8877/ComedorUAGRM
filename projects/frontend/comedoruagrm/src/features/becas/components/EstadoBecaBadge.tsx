import { cn } from '@/lib/utils'

import type { EstadoBeca } from '../types'
import { ESTILOS_ESTADO_BECA } from './estados'

export function EstadoBecaBadge({ estado }: { estado: EstadoBeca }) {
  const estilo = ESTILOS_ESTADO_BECA[estado]
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
