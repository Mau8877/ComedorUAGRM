import { Input } from '@/components/ui/input'

interface CampoSoloLecturaProps {
  id: string
  label: string
  valor: string
  /** Aclaración debajo del campo (ej. por qué no se puede modificar). */
  nota?: string
}

/** Dato del perfil que se muestra pero no se edita: campo bloqueado con su etiqueta. */
export function CampoSoloLectura({ id, label, valor, nota }: CampoSoloLecturaProps) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-foreground">
        {label}
      </label>
      <Input id={id} value={valor} disabled readOnly />
      {nota && <span className="text-xs text-muted-foreground">{nota}</span>}
    </div>
  )
}
