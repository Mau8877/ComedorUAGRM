import { CheckCircle2Icon } from 'lucide-react'

import { Button } from '@/components/ui/button'

import type { ComprobantePostulacion } from '../types'
import { formatearFechaHora } from './formatos'

export interface ComprobanteEnvioProps {
  comprobante: ComprobantePostulacion
  onVerPostulacion: () => void
}

/** El comprobante del envío (CU-02, paso 10): el código de la postulación, la convocatoria, la fecha y el estado. */
export function ComprobanteEnvio({ comprobante, onVerPostulacion }: ComprobanteEnvioProps) {
  return (
    <section aria-label="Comprobante de postulación" className="flex flex-col items-center gap-4 py-6 text-center">
      <CheckCircle2Icon className="size-12 text-success" aria-hidden />
      <div>
        <h2 className="font-heading text-xl">{comprobante.reenvio ? 'Postulación reenviada' : 'Postulación enviada'}</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Recibimos tu postulación. Su estado es <strong>{comprobante.estado}</strong>; puedes seguirla en «Mis
          postulaciones».
        </p>
      </div>
      <dl className="grid w-full max-w-md grid-cols-[auto_1fr] gap-x-4 gap-y-2 rounded-2xl border border-border bg-card p-4 text-left text-sm">
        <dt className="text-muted-foreground">Código</dt>
        <dd className="font-mono break-all">{comprobante.codigo}</dd>
        <dt className="text-muted-foreground">Convocatoria</dt>
        <dd>{comprobante.convocatoria}</dd>
        <dt className="text-muted-foreground">Gestión</dt>
        <dd>{comprobante.gestion}</dd>
        <dt className="text-muted-foreground">Enviada el</dt>
        <dd>{formatearFechaHora(comprobante.fechaEnvio)}</dd>
      </dl>
      <Button onClick={onVerPostulacion}>Ver mi postulación</Button>
    </section>
  )
}
