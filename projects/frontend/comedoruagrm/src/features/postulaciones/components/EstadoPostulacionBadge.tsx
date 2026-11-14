import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

import type { EstadoPostulacion } from '../types'

// Un tono del tema por estado (success, warning, info, destructive, muted): nunca colores sueltos.
const ESTADOS: Record<EstadoPostulacion, { etiqueta: string; clase: string }> = {
  pendiente: { etiqueta: 'Pendiente', clase: 'bg-info/15 text-info' },
  devuelta: { etiqueta: 'Devuelta', clase: 'bg-warning/15 text-warning' },
  archivada: { etiqueta: 'Archivada', clase: 'bg-muted text-muted-foreground' },
  calificada: { etiqueta: 'Calificada', clase: 'bg-info/15 text-info' },
  adjudicada: { etiqueta: 'Adjudicada', clase: 'bg-success/15 text-success' },
  no_adjudicada: { etiqueta: 'No adjudicada', clase: 'bg-destructive/15 text-destructive' },
}

/** El estado visible de una postulación tal como lo manda el backend: el cliente nunca lo recalcula. */
export function EstadoPostulacionBadge({ estado }: { estado: EstadoPostulacion }) {
  const { etiqueta, clase } = ESTADOS[estado] ?? { etiqueta: estado, clase: 'bg-muted text-muted-foreground' }
  return <Badge className={cn(clase)}>{etiqueta}</Badge>
}
