import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

import { formatearPuntos } from './formatos'

interface PuntajePanelProps {
  puntajeIa: number | null
  puntajeAcademico: number | null
  puntajeTotal: number | null
}

/** Puntaje de la IA (0–70), académico de la última auditoría (0–30) y el total que calcula el backend. */
export function PuntajePanel({ puntajeIa, puntajeAcademico, puntajeTotal }: PuntajePanelProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Puntaje</CardTitle>
      </CardHeader>
      <CardContent>
        <dl className="grid grid-cols-3 gap-3">
          <div className="flex flex-col">
            <dt className="text-xs text-muted-foreground">IA (de 70)</dt>
            <dd className="text-lg">{formatearPuntos(puntajeIa)}</dd>
          </div>
          <div className="flex flex-col">
            <dt className="text-xs text-muted-foreground">Académico (de 30)</dt>
            <dd className="text-lg">{formatearPuntos(puntajeAcademico)}</dd>
          </div>
          <div className="flex flex-col">
            <dt className="text-xs text-muted-foreground">Total (de 100)</dt>
            <dd className="text-lg font-semibold">{formatearPuntos(puntajeTotal)}</dd>
          </div>
        </dl>
        {puntajeTotal === null && (
          <p className="mt-3 text-xs text-muted-foreground">
            El total aparece cuando hay puntaje de la IA y un puntaje académico registrado.
          </p>
        )}
      </CardContent>
    </Card>
  )
}
