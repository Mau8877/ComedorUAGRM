import { Button } from '@/components/ui/button'

import type { ResultadoPostulacion } from '../types'

/**
 * El resultado de la postulación una vez publicados los resultados (CU22): adjudicada o no adjudicada, sin motivo
 * (el de la decisión final lo ve solo la DUBSS), y el botón hacia los resultados de la convocatoria.
 */
export function BloqueResultado({
  resultado,
  onVerResultados,
}: {
  resultado: ResultadoPostulacion
  onVerResultados: () => void
}) {
  const adjudicada = resultado === 'ADJUDICADA'
  return (
    <section
      aria-label="Resultado"
      className={
        adjudicada
          ? 'rounded-2xl border border-success/30 bg-success/10 p-4'
          : 'rounded-2xl border border-border bg-card p-4'
      }
    >
      <h3 className={adjudicada ? 'font-heading text-base text-success' : 'font-heading text-base'}>
        {adjudicada ? 'Adjudicada' : 'No adjudicada'}
      </h3>
      <p className="mt-2 text-sm">
        {adjudicada
          ? '¡Felicidades! Obtuviste la Beca Comedor con esta postulación.'
          : 'Tu postulación no fue adjudicada. Gracias por participar.'}
      </p>
      <Button variant="outline" size="sm" className="mt-3" onClick={onVerResultados}>
        Ver resultados de la convocatoria
      </Button>
    </section>
  )
}
