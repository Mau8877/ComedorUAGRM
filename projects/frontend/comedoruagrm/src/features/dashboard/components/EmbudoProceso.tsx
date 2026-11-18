import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

import type { Embudo } from '../types'
import { formatearNumero, formatearPorcentajeONulo } from './formato'

/** Etapas del proceso, de la postulación a la beca vigente (CU12). */
export function EmbudoProceso({ embudo }: { embudo: Embudo }) {
  const etapas = [
    { nombre: 'Postulaciones', valor: embudo.postulaciones },
    { nombre: 'Precalificadas por la IA', valor: embudo.precalificadas },
    { nombre: 'Finalistas', valor: embudo.finalistas },
    { nombre: 'Adjudicadas', valor: embudo.adjudicadas },
    { nombre: 'Becas vigentes', valor: embudo.becasVigentes },
  ]
  const maximo = Math.max(1, ...etapas.map((e) => e.valor))
  return (
    <Card>
      <CardHeader>
        <CardTitle>Embudo del proceso</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {etapas.map((etapa, i) => {
          const anterior = i === 0 ? null : etapas[i - 1].valor
          const conversion =
            anterior === null || anterior === 0 ? null : (etapa.valor / anterior) * 100
          return (
            <div key={etapa.nombre} className="flex flex-col gap-1">
              <div className="flex items-baseline justify-between text-sm">
                <span>{etapa.nombre}</span>
                <span className="font-medium">
                  {formatearNumero(etapa.valor)}
                  {conversion !== null && (
                    <span className="ml-2 text-xs font-normal text-muted-foreground">
                      {formatearPorcentajeONulo(conversion)} de la etapa anterior
                    </span>
                  )}
                </span>
              </div>
              <div className="h-3 rounded-full bg-muted">
                <div
                  className="h-3 rounded-full bg-primary"
                  style={{ width: `${(etapa.valor / maximo) * 100}%` }}
                  role="presentation"
                />
              </div>
            </div>
          )
        })}
      </CardContent>
    </Card>
  )
}
