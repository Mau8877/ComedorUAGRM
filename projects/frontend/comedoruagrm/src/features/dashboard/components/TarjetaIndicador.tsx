import type { LucideIcon } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

import { ChipIcono } from './ChipIcono'
import { estiloTarjeta } from './tonos'
import type { Tono } from './tonos'

interface TarjetaIndicadorProps {
  titulo: string
  /** Ya formateado; «Sin datos» cuando el indicador no existe todavía. */
  valor: string
  ayuda?: string
  /** Color de acento de la tarjeta; sin él, una tarjeta neutra. */
  tono?: Tono
  icono?: LucideIcon
}

/** Una tarjeta con un indicador (CU12): borde, degradado e ícono del color de su tono. */
export function TarjetaIndicador({ titulo, valor, ayuda, tono, icono }: TarjetaIndicadorProps) {
  return (
    <Card className={tono ? estiloTarjeta(tono) : undefined}>
      <CardHeader>
        <CardTitle className="flex items-center justify-between gap-2 text-sm font-medium text-muted-foreground">
          {titulo}
          {tono && icono && <ChipIcono icono={icono} tono={tono} />}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="font-heading text-4xl">{valor}</p>
        {ayuda && <p className="mt-1 text-xs text-muted-foreground">{ayuda}</p>}
      </CardContent>
    </Card>
  )
}
