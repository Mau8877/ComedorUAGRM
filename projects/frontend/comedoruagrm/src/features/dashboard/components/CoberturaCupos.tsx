import { UsersIcon } from 'lucide-react'

import { Card, CardContent, CardHeader } from '@/components/ui/card'

import type { Cobertura } from '../types'
import { formatearNumero, formatearPorcentajeONulo } from './formato'
import { TituloPanel } from './ChipIcono'
import { estiloTarjeta } from './tonos'

/** Cuántos cupos están cubiertos por becas vigentes, y qué pasó con el resto (CU12). */
export function CoberturaCupos({ cobertura }: { cobertura: Cobertura }) {
  const ancho = Math.min(100, cobertura.porcentaje ?? 0)
  return (
    <Card className={estiloTarjeta('success')}>
      <CardHeader>
        <TituloPanel icono={UsersIcon} tono="success">Cobertura de cupos</TituloPanel>
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
