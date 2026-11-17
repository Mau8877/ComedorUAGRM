import { cn } from '@/lib/utils'

// * Colores solo con tokens de tema, igual que los demás badges de estado.
export function EstadoVersionBadge({ vigente }: { vigente: boolean }) {
  return (
    <span
      className={cn(
        'inline-flex w-fit items-center rounded-full px-2.5 py-1 text-xs font-medium whitespace-nowrap',
        vigente ? 'bg-success/15 text-success' : 'bg-muted text-muted-foreground',
      )}
    >
      {vigente ? 'Vigente' : 'Archivada'}
    </span>
  )
}
