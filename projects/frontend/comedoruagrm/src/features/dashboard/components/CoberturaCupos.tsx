import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

import type { Cobertura } from '../types'
import { formatearNumero, formatearPorcentajeONulo } from './formato'

/** Cuántos cupos están cubiertos por becas vigentes, y qué pasó con el resto (CU12). */
export function CoberturaCupos({ cobertura }: { cobertura: Cobertura }) {
  const ancho = Math.min(100, cobertura.porcentaje ?? 0)
  return (
    <Card>
      <CardHeader>
        <CardTitle>Cobertura de cupos</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <p className="font-heading text-3xl">{formatearPorcentajeONulo(cobertura.porcentaje)}</p>
        <div className="h-3 rounded-full bg-muted">
          <div
            className="h-3 rounded-full bg-success"
            style={{ width: `${ancho}%` }}
            role="presentation"
          />
        </div>
        <p className="text-sm text-muted-foreground">
          {formatearNumero(cobertura.becasVigentes)} becas vigentes de{' '}
          {formatearNumero(cobertura.cupos)} cupos
        </p>
        <dl className="grid grid-cols-3 gap-2 text-sm">
          <div>
            <dt className="text-xs text-muted-foreground">Suspendidas</dt>
            <dd>{formatearNumero(cobertura.becasSuspendidas)}</dd>
          </div>
          <div>
            <dt className="text-xs text-muted-foreground">Dadas de baja</dt>
            <dd>{formatearNumero(cobertura.becasBaja)}</dd>
          </div>
          <div>
            <dt className="text-xs text-muted-foreground">Reemplazos</dt>
            <dd>{formatearNumero(cobertura.reemplazos)}</dd>
          </div>
        </dl>
      </CardContent>
    </Card>
  )
}
