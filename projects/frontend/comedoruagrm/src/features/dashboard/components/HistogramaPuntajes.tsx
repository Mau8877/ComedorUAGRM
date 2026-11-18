import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '@/components/ui/chart'
import type { ChartConfig } from '@/components/ui/chart'

import type { TramoPuntaje } from '../types'

const CONFIG = { cantidad: { label: 'Postulaciones', color: 'var(--info)' } } satisfies ChartConfig

/** Cómo se reparten los puntajes de la IA (0–70) en tramos de 10 puntos (CU12). */
export function HistogramaPuntajes({
  tramos,
  promedio,
}: {
  tramos: TramoPuntaje[]
  promedio: number | null
}) {
  const hayDatos = tramos.some((t) => t.cantidad > 0)
  const serie = tramos.map((t) => ({ tramo: `${t.desde}–${t.hasta}`, cantidad: t.cantidad }))
  return (
    <Card>
      <CardHeader>
        <CardTitle>Distribución de puntajes de la IA</CardTitle>
        {promedio !== null && (
          <p className="text-xs text-muted-foreground">
            Promedio: {String(promedio).replace('.', ',')} de 70
          </p>
        )}
      </CardHeader>
      <CardContent>
        {!hayDatos ? (
          <p className="py-10 text-center text-sm text-muted-foreground">
            Sin puntajes calculados.
          </p>
        ) : (
          <ChartContainer config={CONFIG} className="h-56 w-full">
            <BarChart data={serie} accessibilityLayer>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="tramo" tickLine={false} axisLine={false} />
              <YAxis allowDecimals={false} tickLine={false} axisLine={false} width={40} />
              <ChartTooltip content={<ChartTooltipContent />} />
              <Bar dataKey="cantidad" fill="var(--color-cantidad)" radius={4} />
            </BarChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  )
}
