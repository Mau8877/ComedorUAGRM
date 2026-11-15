import { Button } from '@/components/ui/button'

import type { ErrorConvocatorias } from '../api'

interface AlertaConvocatoriasProps {
  error: ErrorConvocatorias
  /** Solo se ofrece con un error de conexión. */
  onReintentar?: () => void
  disabled?: boolean
}

/** Aviso de error dentro de un modal; el formulario conserva lo que la persona había escrito. */
export function AlertaConvocatorias({ error, onReintentar, disabled }: AlertaConvocatoriasProps) {
  return (
    <div
      role="alert"
      className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
    >
      <span>{error.mensaje}</span>
      {error.red && onReintentar && (
        <Button type="button" variant="outline" size="sm" onClick={onReintentar} disabled={disabled}>
          Reintentar
        </Button>
      )}
    </div>
  )
}
