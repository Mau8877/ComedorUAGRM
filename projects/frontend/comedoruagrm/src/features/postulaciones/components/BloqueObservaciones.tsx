import type { Observaciones } from '../types'
import { RUTA_CORREGIR } from './enlacesCu02'
import { formatearFechaHora } from './formatos'

/**
 * Las observaciones de la devolución (CU19) y su fecha. «Corregir y reenviar» solo aparece si el backend dice que
 * se puede corregir **y** existe la pantalla del CU-02; mientras no exista se oculta.
 */
export function BloqueObservaciones({
  observaciones,
  puedeCorregir,
}: {
  observaciones: Observaciones
  puedeCorregir: boolean
}) {
  return (
    <section aria-label="Observaciones" className="rounded-2xl border border-warning/30 bg-warning/10 p-4">
      <h3 className="font-heading text-base text-warning">Observaciones de la DUBSS</h3>
      <p className="mt-2 text-sm">{observaciones.texto}</p>
      <p className="mt-1 text-xs text-muted-foreground">{formatearFechaHora(observaciones.fecha)}</p>
      {puedeCorregir && RUTA_CORREGIR && (
        <a
          href={RUTA_CORREGIR}
          className="mt-3 inline-block text-sm font-medium text-primary underline-offset-4 hover:underline"
        >
          Corregir y reenviar
        </a>
      )}
    </section>
  )
}
