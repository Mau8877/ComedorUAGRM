import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

import type { Auditoria } from '../types'
import { CLASE_RESULTADO_AUDITORIA, ETIQUETA_RESULTADO_AUDITORIA, formatearFechaHora, formatearPuntos } from './formatos'

/** Auditorías anteriores del expediente, de la más reciente a la más antigua (CU07, CA de historial). */
export function HistorialAuditorias({ auditorias }: { auditorias: Auditoria[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Historial de auditorías</CardTitle>
      </CardHeader>
      <CardContent>
        {auditorias.length === 0 ? (
          <p className="text-sm text-muted-foreground">Sin auditorías todavía.</p>
        ) : (
          <ol className="flex flex-col divide-y divide-border">
            {auditorias.map((a) => (
              <li key={a.codigo} className="flex flex-col gap-1 py-2">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${CLASE_RESULTADO_AUDITORIA[a.resultado]}`}
                  >
                    {ETIQUETA_RESULTADO_AUDITORIA[a.resultado]}
                  </span>
                  <span className="text-xs text-muted-foreground">{formatearFechaHora(a.fecha)}</span>
                </div>
                <span className="text-xs text-muted-foreground">
                  {a.auditor}
                  {a.puntajeAcademico !== null ? ` · Académico ${formatearPuntos(a.puntajeAcademico)}` : ''}
                </span>
                {a.observaciones && <p className="text-sm">{a.observaciones}</p>}
              </li>
            ))}
          </ol>
        )}
      </CardContent>
    </Card>
  )
}
