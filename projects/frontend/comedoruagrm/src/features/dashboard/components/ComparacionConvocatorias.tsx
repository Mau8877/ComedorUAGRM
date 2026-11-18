import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

import type { ConvocatoriaComparada } from '../types'
import { formatearNumero, formatearPorcentajeONulo, formatearVariacion } from './formato'

interface Props {
  convocatorias: ConvocatoriaComparada[]
  /** Elegir una fila filtra todo el dashboard por esa convocatoria. */
  onElegir: (codigo: string) => void
}

/** Comparación entre convocatorias de la gestión, con su variación frente a la anterior (CU12). */
export function ComparacionConvocatorias({ convocatorias, onElegir }: Props) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Comparación entre convocatorias</CardTitle>
      </CardHeader>
      <CardContent className="overflow-x-auto">
        {convocatorias.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            Sin convocatorias que comparar.
          </p>
        ) : (
          <table className="w-full text-sm">
            <thead className="text-left text-xs text-muted-foreground">
              <tr>
                <th className="py-2 pr-3 font-medium">Convocatoria</th>
                <th className="px-3 font-medium">Postulaciones</th>
                <th className="px-3 font-medium">Variación</th>
                <th className="px-3 font-medium">Finalistas</th>
                <th className="px-3 font-medium">Adjudicadas</th>
                <th className="px-3 font-medium">Tasa</th>
                <th className="px-3 font-medium">Puntaje IA</th>
              </tr>
            </thead>
            <tbody>
              {convocatorias.map((c) => (
                <tr key={c.codigo} className={c.seleccionada ? 'bg-muted/60' : undefined}>
                  <td className="py-2 pr-3">
                    <button
                      type="button"
                      onClick={() => onElegir(c.codigo)}
                      className="text-left font-medium underline-offset-2 hover:underline"
                    >
                      {c.nombre}
                    </button>
                  </td>
                  <td className="px-3">{formatearNumero(c.postulaciones)}</td>
                  <td
                    className={`px-3 ${
                      c.variacionPostulaciones === null
                        ? 'text-muted-foreground'
                        : c.variacionPostulaciones >= 0
                          ? 'text-success'
                          : 'text-destructive'
                    }`}
                  >
                    {formatearVariacion(c.variacionPostulaciones)}
                  </td>
                  <td className="px-3">{formatearNumero(c.finalistas)}</td>
                  <td className="px-3">
                    {formatearNumero(c.adjudicadas)}
                    {c.cupos !== null && (
                      <span className="text-muted-foreground">
                        {' '}
                        / {formatearNumero(c.cupos)} cupos
                      </span>
                    )}
                  </td>
                  <td className="px-3">{formatearPorcentajeONulo(c.tasaAdjudicacion)}</td>
                  <td className="px-3">
                    {c.puntajePromedio === null ? '—' : String(c.puntajePromedio).replace('.', ',')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </CardContent>
    </Card>
  )
}
