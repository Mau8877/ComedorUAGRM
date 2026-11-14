import { CartesianGrid, Line, LineChart, XAxis, YAxis } from 'recharts'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '@/components/ui/chart'
import type { ChartConfig } from '@/components/ui/chart'

import type { IndicadorMensual } from '../types'
import { formatearMes } from './formato'

const CONFIG = { asistencia: { label: 'Asistencia (%)', color: 'var(--accent)' } } satisfies ChartConfig

/** Asistencia por mes, en línea (CU12). Mientras llegue `null` en todos los meses muestra «Sin datos». */
export function GraficoAsistenciaPorMes({ datos }: { datos: IndicadorMensual[] }) {
  // TODO CU-10: mostrar la asistencia real cuando el backend la entregue
  const hayDatos = datos.some((d) => d.asistencia !== null)
  const serie = datos.map((d) => ({ mes: formatearMes(d.mes), asistencia: d.asistencia }))
  return (
    <Card>
      <CardHeader>
        <CardTitle>Asistencia por mes</CardTitle>
      </CardHeader>
      <CardContent>
        {!hayDatos ? (
          <p className="py-10 text-center text-sm text-muted-foreground">Sin datos</p>
        ) : (
          <ChartContainer config={CONFIG} className="h-64 w-full">
            <LineChart data={serie} accessibilityLayer>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="mes" tickLine={false} axisLine={false} />
              <YAxis domain={[0, 100]} tickLine={false} axisLine={false} width={40} />
              <ChartTooltip content={<ChartTooltipContent />} />
              <Line dataKey="asistencia" type="monotone" stroke="var(--color-asistencia)" strokeWidth={2} />
            </LineChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  )
}
