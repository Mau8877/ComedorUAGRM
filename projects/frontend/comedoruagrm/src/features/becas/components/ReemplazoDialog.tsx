import { useRef, useState } from 'react'
import { ArrowLeftRightIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess, toastWarning } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'

import { mapearErrorBecas, useCandidatoReemplazo, useReemplazar } from '../api'
import type { Beca } from '../types'

const ID_FORMULARIO = 'reemplazo-beca'

interface ReemplazoDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** La beca dada de baja cuyo cupo se reemplaza. */
  beca: Beca
}

/**
 * Ventana de reemplazo de un cupo liberado (CU23, flujo C): al abrir pide al backend el siguiente postulante
 * disponible y lo muestra; el Administrador confirma o cancela (A5). Sin candidato informa y no ofrece confirmar
 * (A4). Si el candidato cambió o la beca ya cambió (ERR_BEC_08, 09...), avisa y cierra: hay que volver a abrirla.
 */
export function ReemplazoDialog({ open, onOpenChange, beca }: ReemplazoDialogProps) {
  const candidato = useCandidatoReemplazo(beca.codigo, open)
  const reemplazar = useReemplazar()
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)
  const liberado = `${beca.becado.nombres} ${beca.becado.apellidos}`
  const propuesto = candidato.data
  const errorCandidato = candidato.error ? mapearErrorBecas(candidato.error).mensaje : null

  const confirmar = () => {
    if (!propuesto) {
      onOpenChange(false)
      return
    }
    if (enviando.current) return
    enviando.current = true
    setError(null)
    reemplazar.mutate(
      { beca: beca.codigo, postulanteCodigo: propuesto.postulanteCodigo },
      {
        onSuccess: () => {
          toastSuccess('Cupo reemplazado', `${propuesto.nombres} ${propuesto.apellidos} ahora es becado.`)
          onOpenChange(false)
        },
        onError: (e) => {
          const mapeado = mapearErrorBecas(e)
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
      },
    )
  }

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      icon={ArrowLeftRightIcon}
      title="Reemplazar cupo"
      subtitle={`Cupo liberado por ${liberado}`}
      formId={ID_FORMULARIO}
      isSubmitting={reemplazar.isPending}
      confirmLabel={!propuesto ? 'Cerrar' : reemplazar.isPending ? 'Reemplazando…' : 'Reemplazar'}
      cancelLabel="Cancelar"
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
        {candidato.isLoading && <p className="text-sm text-muted-foreground">Buscando al siguiente postulante…</p>}
        {errorCandidato && <AlertaError>{errorCandidato}</AlertaError>}
        {propuesto && (
          <>
            <p className="text-sm">
              El siguiente postulante disponible es{' '}
              <strong>
                {propuesto.nombres} {propuesto.apellidos}
              </strong>{' '}
              (CI {propuesto.ci}, {propuesto.carrera}), puesto {propuesto.posicion} de la lista priorizada, con{' '}
              {propuesto.puntajeTotal} puntos.
            </p>
            <p role="note" className="rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-warning">
              Al confirmar se crea su beca vigente y el cupo de {liberado} queda reemplazado. No se puede deshacer.
            </p>
          </>
        )}
        {error && <AlertaError>{error}</AlertaError>}
      </form>
    </ModalLayout>
  )
}
