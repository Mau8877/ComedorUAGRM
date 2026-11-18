import { InboxIcon } from 'lucide-react'

import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts'

import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '@/components/ui/chart'
import type { ChartConfig } from '@/components/ui/chart'

import type { IndicadorMensual } from '../types'
import { formatearMes } from './formato'
import { TituloPanel } from './ChipIcono'
import { estiloTarjeta } from './tonos'

const CONFIG = { postulaciones: { label: 'Postulaciones', color: 'var(--primary)' } } satisfies ChartConfig

/** Postulaciones recibidas por mes, en barras (CU12). */
export function GraficoPostulacionesPorMes({ datos }: { datos: IndicadorMensual[] }) {
  const serie = datos.map((d) => ({ mes: formatearMes(d.mes), postulaciones: d.postulaciones }))
  return (
    <Card className={estiloTarjeta('primary')}>
      <CardHeader>
        <TituloPanel icono={InboxIcon} tono="primary">Postulaciones por mes</TituloPanel>
      </CardHeader>
      <CardContent>
        {serie.length === 0 ? (
          <p className="py-10 text-center text-sm text-muted-foreground">Sin datos para graficar.</p>
        ) : (
          <ChartContainer config={CONFIG} className="h-64 w-full">
            <BarChart data={serie} accessibilityLayer>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="mes" tickLine={false} axisLine={false} />
              <YAxis allowDecimals={false} tickLine={false} axisLine={false} width={40} />
              <ChartTooltip content={<ChartTooltipContent />} />
              <Bar dataKey="postulaciones" fill="var(--color-postulaciones)" radius={4} />
            </BarChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  )
}
