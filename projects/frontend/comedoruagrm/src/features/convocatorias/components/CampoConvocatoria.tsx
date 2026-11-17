import { Input } from '@/components/ui/input'

interface CampoConvocatoriaProps {
  id: string
  label: string
  value: string
  onChange: (valor: string) => void
  onBlur?: () => void
  /** Mensaje a mostrar debajo del campo (ya decidido: validación tocada o error del backend). */
  error?: string
  type?: string
  disabled?: boolean
  inputMode?: 'numeric' | 'decimal' | 'text'
  placeholder?: string
  autoComplete?: string
  /** Texto de ayuda debajo del campo (ej. por qué está deshabilitado o que se define con la DUBSS). */
  ayuda?: string
}

export function CampoConvocatoria({
  id,
  label,
  value,
  onChange,
  onBlur,
  error,
  type,
  disabled,
  inputMode,
  placeholder,
  autoComplete,
  ayuda,
}: CampoConvocatoriaProps) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-foreground">
        {label}
      </label>
      <Input
        id={id}
        name={id}
        type={type}
        value={value}
        disabled={disabled}
        inputMode={inputMode}
        placeholder={placeholder}
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
