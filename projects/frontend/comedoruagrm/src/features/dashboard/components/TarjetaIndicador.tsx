import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

interface TarjetaIndicadorProps {
  titulo: string
  /** Ya formateado; «Sin datos» cuando el indicador no existe todavía. */
  valor: string
  ayuda?: string
}

/** Una tarjeta con un indicador (CU12). */
export function TarjetaIndicador({ titulo, valor, ayuda }: TarjetaIndicadorProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium text-muted-foreground">{titulo}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="font-heading text-3xl">{valor}</p>
        {ayuda && <p className="mt-1 text-xs text-muted-foreground">{ayuda}</p>}
      </CardContent>
    </Card>
  )
}
