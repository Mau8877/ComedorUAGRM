import { Input } from '@/components/ui/input'
import { MARCA_OBLIGATORIO } from '@/components/ui/label'
import { cn } from '@/lib/utils'

interface CampoUsuarioProps {
  id: string
  label: string
  /** Campo obligatorio: agrega el asterisco rojo después de la etiqueta. */
  required?: boolean
  value: string
  onChange: (valor: string) => void
  onBlur?: () => void
  /** Mensaje a mostrar debajo del campo (ya decidido: validación tocada o conflicto del backend). */
  error?: string
  type?: string
  readOnly?: boolean
  autoComplete?: string
  /** Texto de ayuda debajo del campo (ej. por qué es de solo lectura). */
  ayuda?: string
}

export function CampoUsuario({ id, label, required, value, onChange, onBlur, error, type, readOnly, autoComplete, ayuda }: CampoUsuarioProps) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className={cn('text-sm font-medium text-foreground', required && MARCA_OBLIGATORIO)}>
        {label}
      </label>
      <Input
        id={id}
        name={id}
        type={type}
        value={value}
        readOnly={readOnly}
        autoComplete={autoComplete}
        aria-invalid={error ? true : undefined}
        onBlur={onBlur}
        onChange={(e) => onChange(e.target.value)}
      />
      {error ? (
        <span className="text-xs text-destructive">{error}</span>
      ) : (
        ayuda && <span className="text-xs text-muted-foreground">{ayuda}</span>
      )}
    </div>
  )
}
