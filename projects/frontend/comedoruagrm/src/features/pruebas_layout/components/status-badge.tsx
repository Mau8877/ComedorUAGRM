import { cva, type VariantProps } from 'class-variance-authority'
import type { ComponentProps } from 'react'

import { cn } from '@/lib/utils'

const statusBadgeVariants = cva(
  'inline-flex w-fit items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium whitespace-nowrap',
  {
    variants: {
      variant: {
        success: 'bg-success/15 text-success',
        warning: 'bg-warning/15 text-warning',
        destructive: 'bg-destructive/15 text-destructive',
        info: 'bg-info/15 text-info',
        accent: 'bg-accent/15 text-accent',
        primary: 'bg-primary/10 text-primary',
        muted: 'bg-muted text-muted-foreground',
      },
    },
    defaultVariants: { variant: 'muted' },
  }
)

// * Punto de color sólido antes del texto -- ayuda a distinguir el estado
// * incluso sin leer el texto (útil en listas largas escaneadas rápido).
const dotVariants = cva('size-1.5 rounded-full', {
  variants: {
    variant: {
      success: 'bg-success',
      warning: 'bg-warning',
      destructive: 'bg-destructive',
      info: 'bg-info',
      accent: 'bg-accent',
      primary: 'bg-primary',
      muted: 'bg-muted-foreground',
    },
  },
  defaultVariants: { variant: 'muted' },
})

interface StatusBadgeProps
  extends ComponentProps<'span'>,
    VariantProps<typeof statusBadgeVariants> {
  dot?: boolean
}

export function StatusBadge({
  className,
  variant,
  dot = true,
  children,
  ...props
}: StatusBadgeProps) {
  return (
    <span className={cn(statusBadgeVariants({ variant, className }))} {...props}>
      {dot && <span className={cn(dotVariants({ variant }))} />}
      {children}
    </span>
  )
}
