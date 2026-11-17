import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

import type { EstadoBeca } from '../types'

// Un tono del tema por estado: nunca colores sueltos.
const ESTADOS: Record<EstadoBeca, { etiqueta: string; clase: string }> = {
  VIGENTE: { etiqueta: 'Vigente', clase: 'bg-success/15 text-success' },
  SUSPENDIDA: { etiqueta: 'Suspendida', clase: 'bg-warning/15 text-warning' },
  BAJA: { etiqueta: 'Dada de baja', clase: 'bg-destructive/15 text-destructive' },
}

/** El estado de la beca tal como lo manda el backend. */
export function EstadoBecaBadge({ estado }: { estado: EstadoBeca }) {
  const { etiqueta, clase } = ESTADOS[estado] ?? { etiqueta: estado, clase: 'bg-muted text-muted-foreground' }
  return <Badge className={cn(clase)}>{etiqueta}</Badge>
}
