import { useState, type ComponentType } from 'react'
import { useStore, type AnyFieldApi } from '@tanstack/react-form'
import { EyeIcon, EyeOffIcon } from 'lucide-react'

import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

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
  placeholder?: string
  /** Ícono decorativo a la izquierda del input. */
  icono?: ComponentType<{ className?: string }>
  /** Solo para `type="password"`: botón para mostrar/ocultar lo escrito. */
  alternarVisibilidad?: boolean
}

/**
 * Campo de texto con etiqueta y error inline. El error de validación se
 * muestra recién cuando la persona sale del campo (blur) o intenta enviar el
 * formulario; mientras escribe por primera vez no aparece. Una vez visible,
 * se revalida al escribir y desaparece apenas el dato es válido.
 *
 * * `isTouched` no sirve para esto: TanStack Form lo pone en `true` al
 * * escribir, así que el error saltaba desde la primera letra.
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
  placeholder,
  icono: Icono,
  alternarVisibilidad,
}: CampoFormularioProps) {
  const intentosDeEnvio = useStore(field.form.store, (s) => s.submissionAttempts)
  const [visible, setVisible] = useState(false)

  const mostrarValidacion = field.state.meta.isBlurred || intentosDeEnvio > 0
  const errorValidacion = mostrarValidacion ? field.state.meta.errors[0]?.message : undefined
  const error = errorExterno ?? errorValidacion
  const idInput = id ?? field.name
  const esPassword = type === 'password'
  const conToggle = esPassword && alternarVisibilidad

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={idInput} className="text-sm font-medium text-foreground">
        {label}
      </label>
      <div className="relative">
        {Icono && (
          <Icono className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
        )}
        <Input
          id={idInput}
          name={field.name}
          type={conToggle && visible ? 'text' : type}
          autoComplete={autoComplete}
          inputMode={inputMode}
          disabled={disabled}
          placeholder={placeholder}
          value={field.state.value}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${idInput}-error` : undefined}
          className={cn(Icono && 'h-10 pl-9', !Icono && conToggle && 'h-10', conToggle && 'pr-10')}
          onBlur={field.handleBlur}
          onChange={(e) => {
            onEdit?.()
            field.handleChange(e.target.value)
          }}
        />
        {conToggle && (
          <button
            type="button"
            aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            aria-pressed={visible}
            onClick={() => setVisible((v) => !v)}
            className="absolute top-1/2 right-2 flex size-7 -translate-y-1/2 items-center justify-center rounded-full text-muted-foreground outline-none hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/30"
          >
            {visible ? <EyeOffIcon className="size-4" /> : <EyeIcon className="size-4" />}
          </button>
        )}
      </div>
      {error && (
        <span id={`${idInput}-error`} className="text-xs text-destructive">
          {error}
        </span>
      )}
    </div>
  )
}
