import { GraduationCapIcon } from 'lucide-react'

import { useState } from 'react'

import { Card, CardContent, CardHeader } from '@/components/ui/card'

import type { FacultadBi } from '../types'
import { formatearNumero, formatearPorcentajeONulo } from './formato'
import { TituloPanel } from './ChipIcono'
import { estiloTarjeta } from './tonos'

/**
 * Qué facultad se postula más, comparada con sus adjudicadas (CU12). Al elegir una facultad se ven sus carreras.
 *
 * * La elección es solo de la pantalla (filtro cruzado): no cambia la consulta al servidor.
 */
export function RankingFacultades({ facultades }: { facultades: FacultadBi[] }) {
  const [elegida, setElegida] = useState<string | null>(null)
  const maximo = Math.max(1, ...facultades.map((f) => f.postulaciones))
  const detalle = facultades.find((f) => f.nombre === elegida)

  return (
    <Card className={estiloTarjeta('accent')}>
      <CardHeader>
        <TituloPanel icono={GraduationCapIcon} tono="accent">Postulaciones por facultad</TituloPanel>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {facultades.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">Sin datos para graficar.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {facultades.map((f) => (
              <li key={f.nombre}>
                <button
                  type="button"
                  aria-pressed={elegida === f.nombre}
                  onClick={() => setElegida(elegida === f.nombre ? null : f.nombre)}
                  className="flex w-full flex-col gap-1 rounded-md p-2 text-left hover:bg-muted/60 aria-pressed:bg-muted"
                >
                  <span className="flex items-baseline justify-between gap-2 text-sm">
                    <span className="font-medium">{f.nombre}</span>
                    <span className="text-xs text-muted-foreground">
                      {formatearNumero(f.postulaciones)} postulaciones ·{' '}
                      {formatearPorcentajeONulo(f.porcentajePostulaciones)}
                    </span>
                  </span>
                  <span className="relative h-3 rounded-full bg-muted">
                    <span
                      className="absolute inset-y-0 left-0 rounded-full bg-primary"
                      style={{ width: `${(f.postulaciones / maximo) * 100}%` }}
                    />
                    <span
                      className="absolute inset-y-0 left-0 rounded-full bg-accent"
                      style={{ width: `${(f.adjudicadas / maximo) * 100}%` }}
                    />
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {formatearNumero(f.adjudicadas)} adjudicadas · tasa{' '}
                    {formatearPorcentajeONulo(f.tasaAdjudicacion)}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
        {detalle && (
          <div className="rounded-md border p-3" aria-label={`Carreras de ${detalle.nombre}`}>
            <p className="mb-2 text-sm font-medium">Carreras de {detalle.nombre}</p>
            <ul className="flex flex-col gap-1 text-sm">
              {detalle.carreras.map((c) => (
                <li key={c.nombre} className="flex justify-between gap-2">
                  <span>{c.nombre}</span>
                  <span className="text-muted-foreground">
                    {formatearNumero(c.postulaciones)} postulaciones ·{' '}
                    {formatearNumero(c.adjudicadas)} adjudicadas
                  </span>
                </li>
              ))}
            </ul>
          </div>
        )}
        <p className="flex gap-4 text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <span className="size-2 rounded-full bg-primary" /> Postulaciones
          </span>
          <span className="flex items-center gap-1">
            <span className="size-2 rounded-full bg-accent" /> Adjudicadas
          </span>
        </p>
      </CardContent>
    </Card>
  )
}
