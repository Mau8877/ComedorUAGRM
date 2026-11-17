import { useRef, useState } from 'react'
import { RefreshCwIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { toastInfo, toastWarning } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'

import { mapearErrorPreseleccion, useReprocesar } from '../api'
import type { LoteReprocesamiento } from '../types'

const ID_FORMULARIO = 'reprocesar-puntajes'

interface ReprocesarDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  convocatoria: { codigo: string; nombre: string }
  /** La versión vigente con la que se recalcula. */
  modelo: string
  /** Cuántas postulaciones calificadas se van a recalcular. */
  porReprocesar: number
  /** El lote ya quedó creado y corre en segundo plano. */
  onIniciado: (lote: LoteReprocesamiento) => void
}

/**
 * «¿Reprocesar los puntajes?» (CU26, paso 7): resume lo que se va a recalcular y pide confirmar. Cancelar no cambia
 * ningún puntaje (A6). Si el backend dice que ya no se puede (publicados, lote en curso, nada por reprocesar), avisa y
 * cierra: el resumen se vuelve a pedir solo.
 */
export function ReprocesarDialog({
  open,
  onOpenChange,
  convocatoria,
  modelo,
  porReprocesar,
  onIniciado,
}: ReprocesarDialogProps) {
  const reprocesar = useReprocesar()
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)

  const confirmar = () => {
    if (enviando.current) return
    enviando.current = true
    setError(null)
    reprocesar.mutate(convocatoria.codigo, {
      onSuccess: (lote) => {
        toastInfo('Reprocesamiento iniciado', 'Puedes seguir el avance en esta pantalla.')
        onIniciado(lote)
        onOpenChange(false)
      },
      onError: (e) => {
        const mapeado = mapearErrorPreseleccion(e)
        if (mapeado.refrescar) {
          toastWarning('Sin cambios', mapeado.mensaje)
          onOpenChange(false)
          return
        }
        setError(mapeado.mensaje)
      },
      onSettled: () => {
        enviando.current = false
      },
    })
  }

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      icon={RefreshCwIcon}
      title="¿Reprocesar los puntajes?"
      subtitle={convocatoria.nombre}
      formId={ID_FORMULARIO}
      isSubmitting={reprocesar.isPending}
      confirmLabel={reprocesar.isPending ? 'Iniciando…' : 'Confirmar reprocesamiento'}
    >
      <form
        id={ID_FORMULARIO}
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          confirmar()
        }}
      >
        <p className="text-sm">
          Se recalcularán las postulaciones de {convocatoria.nombre} con la versión <strong>{modelo}</strong>. El proceso
          puede tardar varios minutos y quedará registrado en la bitácora.
        </p>
        <p role="note" className="rounded-md border border-info/30 bg-info/10 p-3 text-sm text-info">
          {porReprocesar === 1 ? 'Se recalculará 1 postulación' : `Se recalcularán ${porReprocesar} postulaciones`}. El
          puntaje anterior de cada una se conserva en la traza.
        </p>
        {error && <AlertaError>{error}</AlertaError>}
      </form>
    </ModalLayout>
  )
}
