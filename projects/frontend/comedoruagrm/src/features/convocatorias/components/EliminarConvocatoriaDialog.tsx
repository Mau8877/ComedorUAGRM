import { useRef, useState } from 'react'

import { ModalDestructive } from '@/components/ui/modal-destructive'
import { toastSuccess, toastWarning } from '@/components/ui/toast'

import { mapearErrorConvocatorias, useEliminarConvocatoria, useRefrescarConvocatorias } from '../api'
import type { ErrorConvocatorias } from '../api'
import type { Convocatoria } from '../types'

interface EliminarConvocatoriaDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  convocatoria: Convocatoria
}

/**
 * Confirmación para eliminar una convocatoria (A7, CA17). Solo se elimina una en borrador y sin postulaciones: con
 * postulaciones solo se cierra. Las reglas las aplica el backend; acá se avisa y se refresca la lista.
 */
export function EliminarConvocatoriaDialog({ open, onOpenChange, convocatoria }: EliminarConvocatoriaDialogProps) {
  const eliminar = useEliminarConvocatoria()
  const refrescar = useRefrescarConvocatorias()
  const [error, setError] = useState<ErrorConvocatorias | null>(null)
  const enviando = useRef(false)

  const confirmar = () => {
    if (enviando.current) return
    enviando.current = true
    setError(null)
    eliminar.mutate(convocatoria.codigo, {
      onSuccess: () => {
        toastSuccess('Convocatoria eliminada', convocatoria.nombre)
        onOpenChange(false)
      },
      onError: (e) => {
        const mapeado = mapearErrorConvocatorias(e)
        if (mapeado.refrescar) {
          // Ya tiene postulaciones (ERR_CV_10: solo se cierra), cambió de estado o ya no existe.
          toastWarning('No se pudo eliminar', mapeado.mensaje)
          refrescar(convocatoria.codigo)
          onOpenChange(false)
          return
        }
        setError(mapeado)
      },
      onSettled: () => {
        enviando.current = false
      },
    })
  }

  const base = `${convocatoria.nombre} se quitará de la lista. Solo se pueden eliminar las convocatorias en borrador y sin postulaciones.`
  return (
    <ModalDestructive
      open={open}
      onOpenChange={onOpenChange}
      title="¿Eliminar convocatoria?"
      description={error ? `${base} ${error.mensaje}` : base}
      confirmLabel={error?.red ? 'Reintentar' : 'Eliminar'}
      isSubmitting={eliminar.isPending}
      onConfirm={confirmar}
    />
  )
}
