import { cn } from '@/lib/utils'

interface ProgressBarProps {
  value: number
  max: number
  variant?: 'success' | 'warning' | 'destructive'
  className?: string
}

const fillClasses = {
  success: 'bg-success',
  warning: 'bg-warning',
  destructive: 'bg-destructive',
}

export function ProgressBar({
  value,
  max,
  variant = 'success',
  className,
}: ProgressBarProps) {
  const percent = Math.min(100, Math.round((value / max) * 100))

  return (
    <div
      className={cn('h-2 w-full overflow-hidden rounded-full bg-muted', className)}
    >
      <div
        className={cn('h-full rounded-full transition-all', fillClasses[variant])}
        style={{ width: `${percent}%` }}
      />
    </div>
  )
}
