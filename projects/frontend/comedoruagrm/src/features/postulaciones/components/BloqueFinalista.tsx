import type { FinalistaPostulante } from '../types'
import { formatearDia } from './formatos'

/**
 * El bloque «Eres finalista» (CU21): el plazo para entregar la documentación física, los documentos a presentar
 * (del último aviso) y si la documentación ya fue recibida.
 */
export function BloqueFinalista({ finalista }: { finalista: FinalistaPostulante }) {
  return (
    <section aria-label="Eres finalista" className="rounded-2xl border border-info/30 bg-info/10 p-4">
      <h3 className="font-heading text-base text-info">Eres finalista</h3>
      <dl className="mt-2 flex flex-col gap-2 text-sm">
        <div>
          <dt className="text-xs font-medium text-muted-foreground">Plazo de entrega</dt>
          <dd>{finalista.plazoEntrega ? formatearDia(finalista.plazoEntrega) : 'Todavía sin plazo'}</dd>
        </div>
        <div>
          <dt className="text-xs font-medium text-muted-foreground">Documentos a presentar</dt>
          <dd>{finalista.documentosFisicos ?? 'La DUBSS aún no los indicó'}</dd>
        </div>
        <div>
          <dt className="text-xs font-medium text-muted-foreground">Documentación</dt>
          <dd className={finalista.documentacionRecibida ? 'font-medium text-success' : undefined}>
            {finalista.documentacionRecibida ? 'Recibida' : 'Pendiente de entrega'}
          </dd>
        </div>
      </dl>
    </section>
  )
}
