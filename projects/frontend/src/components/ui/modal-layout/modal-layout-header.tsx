import { DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'

export interface ModalLayoutHeaderProps {
  title: string
  subtitle?: string
  className?: string
}

export function ModalLayoutHeader({ title, subtitle, className }: ModalLayoutHeaderProps) {
  return (
    <DialogHeader className={cn('shrink-0 border-b border-border p-6', className)}>
      <DialogTitle>{title}</DialogTitle>
      {subtitle && <DialogDescription>{subtitle}</DialogDescription>}
    </DialogHeader>
  )
}
