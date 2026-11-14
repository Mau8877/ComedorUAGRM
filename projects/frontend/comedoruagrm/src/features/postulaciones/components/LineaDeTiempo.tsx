import { CheckCircle2Icon, CircleDashedIcon } from 'lucide-react'

import { cn } from '@/lib/utils'

import type { PasoLineaTiempo } from '../types'
import { formatearFechaHora } from './formatos'

const ETIQUETAS: Record<string, string> = {
  pendiente: 'Postulación enviada',
  calificada: 'Calificada',
  devuelta: 'Devuelta para corrección',
  archivada: 'Archivada',
  adjudicada: 'Adjudicada',
  no_adjudicada: 'No adjudicada',
  resultado: 'Resultado de la adjudicación',
}

/** El texto de un paso: el mismo estado cambia de sentido si ya ocurrió o si todavía falta (el reenvío). */
function etiquetaDePaso(paso: PasoLineaTiempo): string {
  if (paso.situacion === 'PENDIENTE' && paso.estado === 'pendiente') {
    return 'Reenvío de la postulación corregida'
  }
  return ETIQUETAS[paso.estado] ?? paso.estado
}

/**
 * La línea de tiempo de una postulación (CU04): los pasos que ya ocurrieron, con su fecha y hora, y los que faltan,
 * atenuados. Sale del historial real que arma el backend; no muestra quién hizo cada cambio.
 */
export function LineaDeTiempo({ pasos }: { pasos: PasoLineaTiempo[] }) {
  return (
    <ol aria-label="Línea de tiempo" className="flex flex-col gap-4">
      {pasos.map((paso, indice) => {
        const completado = paso.situacion === 'COMPLETADO'
        const Icono = completado ? CheckCircle2Icon : CircleDashedIcon
        return (
          <li
            key={`${paso.estado}-${indice}`}
            data-situacion={paso.situacion}
            className={cn('flex items-start gap-3', completado ? 'text-foreground' : 'text-muted-foreground opacity-60')}
          >
            <Icono className={cn('mt-0.5 size-5 shrink-0', completado && 'text-success')} aria-hidden />
            <div className="flex flex-col">
              <span className="text-sm font-medium">{etiquetaDePaso(paso)}</span>
              <span className="text-xs text-muted-foreground">
                {paso.fecha ? formatearFechaHora(paso.fecha) : 'Pendiente'}
              </span>
            </div>
          </li>
        )
      })}
    </ol>
  )
}
