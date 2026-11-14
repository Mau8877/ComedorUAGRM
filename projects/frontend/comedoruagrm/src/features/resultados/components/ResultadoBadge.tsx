import type { ResultadoDecision } from '../types'

/** Insignia del resultado de una decisión: «Adjudicada» o «No adjudicada». */
export function ResultadoBadge({ resultado }: { resultado: ResultadoDecision }) {
  return resultado === 'ADJUDICADA' ? (
    <span className="rounded-full bg-success/15 px-2 py-0.5 text-xs font-medium text-success">Adjudicada</span>
  ) : (
    <span className="rounded-full bg-destructive/15 px-2 py-0.5 text-xs font-medium text-destructive">
      No adjudicada
    </span>
  )
}
