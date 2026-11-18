import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '@/components/ui/chart'
import type { ChartConfig } from '@/components/ui/chart'

import type { AsistenciaBi } from '../types'
import { formatearNumero, formatearPorcentajeONulo, nombreDia } from './formato'

const CONFIG = { tasa: { label: 'Asistencia (%)', color: 'var(--success)' } } satisfies ChartConfig

/** Asistencia por día de la semana y becas en riesgo (CU12 con el registro de asistencia del CU10). */
export function AsistenciaPorDiaSemana({ asistencia }: { asistencia: AsistenciaBi }) {
  const hayDatos = asistencia.diasConRegistro > 0
  const serie = asistencia.porDiaSemana.map((d) => ({ dia: nombreDia(d.dia), tasa: d.tasa }))
  return (
    <Card>
      <CardHeader>
        <CardTitle>Asistencia por día de la semana</CardTitle>
        {hayDatos && (
          <p className="text-xs text-muted-foreground">
            Promedio {formatearPorcentajeONulo(asistencia.tasaPromedio)} ·{' '}
            {formatearNumero(asistencia.diasConRegistro)} días con registro
          </p>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {!hayDatos ? (
          <p className="py-10 text-center text-sm text-muted-foreground">
            Sin registros de asistencia.
          </p>
        ) : (
          <ChartContainer config={CONFIG} className="h-56 w-full">
            <BarChart data={serie} accessibilityLayer>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="dia" tickLine={false} axisLine={false} />
              <YAxis domain={[0, 100]} tickLine={false} axisLine={false} width={40} />
              <ChartTooltip content={<ChartTooltipContent />} />
              <Bar dataKey="tasa" fill="var(--color-tasa)" radius={4} />
            </BarChart>
          </ChartContainer>
        )}
        <p
          className={`rounded-md border p-2 text-sm ${
            asistencia.becasEnRiesgo > 0
              ? 'border-warning/40 bg-warning/10 text-warning'
              : 'text-muted-foreground'
          }`}
        >
          {asistencia.becasEnRiesgo === 0
            ? 'Ninguna beca en riesgo por inasistencia.'
            : `${formatearNumero(asistencia.becasEnRiesgo)} becas en riesgo (menos de 60 % de asistencia).`}
        </p>
      </CardContent>
    </Card>
  )
}
