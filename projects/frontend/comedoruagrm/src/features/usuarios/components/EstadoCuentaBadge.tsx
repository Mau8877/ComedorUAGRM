import { cn } from '@/lib/utils'

// * Colores solo con tokens de tema (success / muted-foreground), igual que el
// * StatusBadge de la demo de layout -- que no se importa de ahí porque esa
// * feature es de prueba.
export function EstadoCuentaBadge({ activo }: { activo: boolean }) {
  return (
    <span
      className={cn(
        'inline-flex w-fit items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium whitespace-nowrap',
        activo ? 'bg-success/15 text-success' : 'bg-muted text-muted-foreground',
      )}
    >
      <span className={cn('size-1.5 rounded-full', activo ? 'bg-success' : 'bg-muted-foreground')} />
      {activo ? 'Activa' : 'Desactivada'}
    </span>
  )
}
