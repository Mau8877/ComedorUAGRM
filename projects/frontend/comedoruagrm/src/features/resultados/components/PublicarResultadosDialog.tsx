import { useRef, useState } from 'react'
import { TrophyIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'

import { mapearErrorResultados } from '../api/mapearErrorResultados'
import { usePublicarResultados } from '../api/hooks'
import type { PublicacionResultado, ResumenPublicacion } from '../types'

const ID_FORMULARIO = 'publicar-resultados'

interface PublicarResultadosDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** El resumen de la convocatoria: de él salen las cantidades y el nombre. */
  resumen: ResumenPublicacion
  /** Se llama cuando la publicación terminó bien. */
  onPublicados?: (publicacion: PublicacionResultado) => void
  /** El backend dijo que el resumen quedó desactualizado (ERR_PUB_01, 02 o 03): la pantalla lo vuelve a pedir. */
  onDesactualizado?: () => void
  /** El backend dijo que la convocatoria ya no existe (ERR_PRI_01): la pantalla vuelve a elegir. */
  onConvocatoriaInexistente?: () => void
}

/**
 * Ventana «Publicar resultados» (CU22): avisa que la publicación no se puede deshacer, que bloquea nuevas
 * decisiones y a cuántos finalistas se les avisará. Sin adjudicados avisa pero no bloquea. El botón se deshabilita
 * mientras se envía (no hay idempotencia por clave: una segunda petición la resuelve el backend con ERR_PUB_02).
 */
export function PublicarResultadosDialog({
  open,
  onOpenChange,
  resumen,
  onPublicados,
  onDesactualizado,
  onConvocatoriaInexistente,
}: PublicarResultadosDialogProps) {
  const publicar = usePublicarResultados()
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)

  const enviar = () => {
    if (enviando.current) return
    enviando.current = true
    setError(null)
    publicar.mutate(resumen.convocatoria.codigo, {
      onSuccess: (publicacion) => {
        toastSuccess('Resultados publicados correctamente', `Se avisó a ${publicacion.avisados} finalistas.`)
        onPublicados?.(publicacion)
        onOpenChange(false)
      },
      onError: (e) => {
        const mapeado = mapearErrorResultados(e)
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
      icon={TrophyIcon}
      title="Publicar resultados"
      subtitle={resumen.convocatoria.nombre}
      formId={ID_FORMULARIO}
      isSubmitting={publicar.isPending}
      confirmLabel="Publicar"
    >
      <form
        id={ID_FORMULARIO}
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          enviar()
        }}
      >
        <p className="text-sm">
          Convocatoria <strong>{resumen.convocatoria.nombre}</strong>: hay <strong>{resumen.adjudicados}</strong>{' '}
          adjudicados y <strong>{resumen.noAdjudicados}</strong> no adjudicados.
        </p>
        {resumen.adjudicados === 0 && (
          <p role="note" className="rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-warning">
            No hay adjudicados: todos los finalistas recibirán “no adjudicada”.
          </p>
        )}
        <p className="text-sm">
          Se avisará a <strong>{resumen.aAvisar}</strong> finalistas por bandeja, push y correo.
        </p>
        <p
          role="note"
          className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          La publicación no se puede deshacer y bloquea nuevas decisiones de adjudicación en esta convocatoria.
        </p>

        {error && <AlertaError>{error}</AlertaError>}
      </form>
    </ModalLayout>
  )
}
