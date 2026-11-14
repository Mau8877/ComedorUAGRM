import type { AnyFieldApi } from '@tanstack/react-form'

import { Input } from '@/components/ui/input'

interface CampoFormularioProps {
  field: AnyFieldApi
  label: string
  /** `id` del input; por defecto el nombre del campo. Se pasa cuando dos formularios de una misma página comparten nombres. */
  id?: string
  type?: 'text' | 'email' | 'password' | 'tel'
  autoComplete?: string
  inputMode?: 'numeric' | 'tel' | 'text'
  /** Error que viene del servidor (ej. CI ya registrado); se muestra hasta que la persona edite el campo. */
  errorExterno?: string
  onEdit?: () => void
  disabled?: boolean
}

/**
 * Campo de texto con etiqueta y error inline. El error de validación solo se
 * muestra si el campo ya fue tocado (FORMULARIOS_ZOD_TANSTACK_FRONTEND.md).
 */
export function CampoFormulario({
  field,
  label,
  id,
  type = 'text',
  autoComplete,
  inputMode,
  errorExterno,
  onEdit,
  disabled,
}: CampoFormularioProps) {
  const errorValidacion = field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined
  const error = errorExterno ?? errorValidacion
  const idInput = id ?? field.name

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={idInput} className="text-sm font-medium text-foreground">
        {label}
      </label>
      <Input
        id={idInput}
        name={field.name}
        type={type}
        autoComplete={autoComplete}
        inputMode={inputMode}
        disabled={disabled}
        value={field.state.value}
        aria-invalid={error ? true : undefined}
        onBlur={field.handleBlur}
        onChange={(e) => {
          onEdit?.()
          field.handleChange(e.target.value)
        }}
      />
      {error && <span className="text-xs text-destructive">{error}</span>}
    </div>
  )
}
