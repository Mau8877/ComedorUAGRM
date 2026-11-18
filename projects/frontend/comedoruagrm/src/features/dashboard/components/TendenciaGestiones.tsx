import { TrendingUpIcon } from 'lucide-react'

import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts'

import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '@/components/ui/chart'
import type { ChartConfig } from '@/components/ui/chart'

import type { GestionTendencia } from '../types'
import { formatearVariacion } from './formato'
import { TituloPanel } from './ChipIcono'
import { estiloTarjeta } from './tonos'

const CONFIG = {
  postulaciones: { label: 'Postulaciones', color: 'var(--primary)' },
  adjudicadas: { label: 'Adjudicadas', color: 'var(--accent)' },
} satisfies ChartConfig

/** Tendencia de postulaciones y adjudicadas entre gestiones (CU12). */
export function TendenciaGestiones({ tendencia }: { tendencia: GestionTendencia[] }) {
  const actual = tendencia.find((g) => g.seleccionada)
  return (
    <Card className={estiloTarjeta('primary')}>
      <CardHeader>
        <TituloPanel icono={TrendingUpIcon} tono="primary">Tendencia entre gestiones</TituloPanel>
        {actual && (
          <p className="text-xs text-muted-foreground">
            Gestión {actual.etiqueta}: {formatearVariacion(actual.variacionPostulaciones)} de
            postulaciones frente a la anterior
          </p>
        )}
      </CardHeader>
      <CardContent>
        {tendencia.length === 0 ? (
          <p className="py-10 text-center text-sm text-muted-foreground">
            Sin gestiones que comparar.
          </p>
        ) : (
          <ChartContainer config={CONFIG} className="h-56 w-full">
            <BarChart data={tendencia} accessibilityLayer>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="etiqueta" tickLine={false} axisLine={false} />
              <YAxis allowDecimals={false} tickLine={false} axisLine={false} width={40} />
              <ChartTooltip content={<ChartTooltipContent />} />
              <Bar dataKey="postulaciones" fill="var(--color-postulaciones)" radius={4} />
              <Bar dataKey="adjudicadas" fill="var(--color-adjudicadas)" radius={4} />
            </BarChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  )
}
