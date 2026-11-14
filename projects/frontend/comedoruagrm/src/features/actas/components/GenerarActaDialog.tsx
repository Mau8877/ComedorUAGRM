import { useRef, useState } from 'react'
import { FileTextIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'

import { mapearErrorActas, useGenerarActa } from '../api'
import type { Acta, ConvocatoriaPendienteActa } from '../types'
import { useDescargarActa } from './useDescargarActa'

const ID_FORMULARIO = 'generar-acta'

interface GenerarActaDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** La convocatoria elegida: de ella salen el nombre y la cantidad de adjudicados. */
  convocatoria: ConvocatoriaPendienteActa
  /** Se llama cuando el acta se generó bien. */
  onGenerada?: (acta: Acta) => void
  /** El backend dijo que la lista de pendientes quedó desactualizada (ERR_ACT_01 o 02): el panel la vuelve a pedir. */
  onDesactualizado?: () => void
  /** El backend dijo que la convocatoria ya no existe (ERR_PRI_01): el panel vuelve a elegir. */
  onConvocatoriaInexistente?: () => void
}

/**
 * Ventana «Generar acta de adjudicación» (CU08): avisa que el acta es definitiva (no se regenera ni se elimina) y
 * pide confirmación. Sin adjudicados avisa pero no bloquea. El botón se deshabilita mientras se envía (no hay
 * idempotencia por clave: una segunda petición la resuelve el backend con ERR_ACT_02). Al generar, la misma
 * ventana muestra el número del acta y ofrece descargarla.
 */
export function GenerarActaDialog({
  open,
  onOpenChange,
  convocatoria,
  onGenerada,
  onDesactualizado,
  onConvocatoriaInexistente,
}: GenerarActaDialogProps) {
  const generar = useGenerarActa()
  const descarga = useDescargarActa()
  const [generada, setGenerada] = useState<Acta | null>(null)
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)

  const enviar = () => {
    if (enviando.current) return
    enviando.current = true
    setError(null)
    generar.mutate(convocatoria.codigo, {
      onSuccess: (acta) => {
        setGenerada(acta)
        toastSuccess('Acta generada correctamente')
        onGenerada?.(acta)
      },
      onError: (e) => {
        const mapeado = mapearErrorActas(e)
        if (mapeado.convocatoriaInexistente) {
          onConvocatoriaInexistente?.()
          onOpenChange(false)
          return
        }
        if (mapeado.refrescar) onDesactualizado?.()
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
      icon={FileTextIcon}
      title="Generar acta de adjudicación"
      subtitle={convocatoria.nombre}
      formId={ID_FORMULARIO}
      isSubmitting={generar.isPending || Boolean(descarga.descargando)}
      confirmLabel={generada ? 'Descargar' : generar.isPending ? 'Generando…' : 'Generar'}
      cancelLabel={generada ? 'Cerrar' : 'Cancelar'}
    >
      <form
        id={ID_FORMULARIO}
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          if (generada) {
            descarga.descargar(generada.convocatoria.codigo)
          } else {
            enviar()
          }
        }}
      >
        {generada ? (
          <>
            <p role="status" className="text-sm">
              Acta N.º <strong>{generada.numero}</strong> generada para <strong>{generada.convocatoria.nombre}</strong>{' '}
              con <strong>{generada.adjudicados}</strong> adjudicados.
            </p>
            {descarga.error && <AlertaError>{descarga.error}</AlertaError>}
          </>
        ) : (
          <>
            <p className="text-sm">
              Convocatoria <strong>{convocatoria.nombre}</strong>: el acta incluirá a{' '}
              <strong>{convocatoria.adjudicados}</strong> adjudicados.
            </p>
            {convocatoria.adjudicados === 0 && (
              <p role="note" className="rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-warning">
                No hay adjudicados: el acta dirá “Sin adjudicados”.
              </p>
            )}
            <p
              role="note"
              className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
            >
              El acta es definitiva: no se puede regenerar ni eliminar.
            </p>
            {error && <AlertaError>{error}</AlertaError>}
          </>
        )}
      </form>
    </ModalLayout>
  )
}
