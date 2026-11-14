import { TriangleAlertIcon } from 'lucide-react'
import type { ReactNode } from 'react'

/** Alerta visible sobre el formulario (errores que no son de un campo concreto). */
export function AlertaError({ children }: { children: ReactNode }) {
  return (
    <div
      role="alert"
      className="flex items-start gap-2 rounded-2xl border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
    >
      <TriangleAlertIcon className="mt-0.5 size-4 shrink-0" />
      <div className="flex flex-col gap-1">{children}</div>
    </div>
  )
}
