import { Button } from '@/components/ui/button'

import type { BorradorResumen } from '../types'

import { RUTA_POSTULAR } from './enlacesCu02'
import { formatearFecha, formatearFechaHora } from './formatos'

export interface BorradorPendienteCardProps {
  borrador: BorradorResumen
}

/** «Faltan 3 campos y 1 documento», o «Todo listo para enviar» cuando no falta nada. */
function resumenAvance(borrador: BorradorResumen): string {
  const partes: string[] = []
  if (borrador.camposPendientes > 0) {
    partes.push(`${borrador.camposPendientes} ${borrador.camposPendientes === 1 ? 'campo' : 'campos'}`)
  }
  if (borrador.documentosPendientes > 0) {
    partes.push(`${borrador.documentosPendientes} ${borrador.documentosPendientes === 1 ? 'documento' : 'documentos'}`)
  }
  return partes.length === 0 ? 'No falta nada: ya puedes enviarlo.' : `Faltan ${partes.join(' y ')}.`
}

/**
 * «Mi borrador» (CU-17): la postulación a medias de la convocatoria abierta, con la fecha del último guardado y lo que
 * falta para poder enviarla. «Continuar» abre `/postular`, que recupera el borrador con sus respuestas.
 */
export function BorradorPendienteCard({ borrador }: BorradorPendienteCardProps) {
  return (
    <section aria-label="Mi borrador" className="mb-4 flex flex-col gap-3 rounded-2xl border border-border bg-card p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h2 className="text-base font-semibold">{borrador.convocatoria}</h2>
          <p className="text-sm text-muted-foreground">
            {borrador.guardadoEn
              ? `Última modificación: ${formatearFechaHora(borrador.guardadoEn)}`
              : 'Todavía no guardaste ningún dato.'}
          </p>
        </div>
        <span className="rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">Borrador</span>
      </div>
      <p className="text-sm text-muted-foreground">{resumenAvance(borrador)}</p>
      <p className="rounded-md border border-info/30 bg-info/10 p-3 text-sm">
        Tu borrador se conserva hasta el cierre de la convocatoria ({formatearFecha(borrador.fechaCierre)}). Un borrador
        no cuenta como postulación enviada.
      </p>
      {RUTA_POSTULAR && (
        <div>
          <Button nativeButton={false} render={<a href={RUTA_POSTULAR} />}>
            Continuar
          </Button>
        </div>
      )}
    </section>
  )
}
