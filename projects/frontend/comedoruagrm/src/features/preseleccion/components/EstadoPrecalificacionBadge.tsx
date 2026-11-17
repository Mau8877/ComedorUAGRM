import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

// Un tono del tema por estado: nunca colores sueltos. «Marcada para revisión» es una pendiente que la IA no pudo calcular.
const ESTADOS: Record<string, { etiqueta: string; clase: string }> = {
  pendiente: { etiqueta: 'Pendiente', clase: 'bg-info/15 text-info' },
  calificada: { etiqueta: 'Calificada', clase: 'bg-info/15 text-info' },
  devuelta: { etiqueta: 'Devuelta', clase: 'bg-warning/15 text-warning' },
  archivada: { etiqueta: 'Archivada', clase: 'bg-muted text-muted-foreground' },
  adjudicada: { etiqueta: 'Adjudicada', clase: 'bg-success/15 text-success' },
  no_adjudicada: { etiqueta: 'No adjudicada', clase: 'bg-destructive/15 text-destructive' },
}

export function EstadoPrecalificacionBadge({ estado, enRevision }: { estado: string; enRevision: boolean }) {
  if (enRevision) {
    return <Badge className="bg-warning/15 text-warning">Marcada para revisión</Badge>
  }
  const { etiqueta, clase } = ESTADOS[estado] ?? { etiqueta: estado, clase: 'bg-muted text-muted-foreground' }
  return <Badge className={cn(clase)}>{etiqueta}</Badge>
}
