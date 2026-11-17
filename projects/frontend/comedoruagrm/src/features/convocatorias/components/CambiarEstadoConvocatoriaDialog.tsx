import { useRef, useState } from 'react'
import { PlayIcon } from 'lucide-react'

import { ModalDestructive } from '@/components/ui/modal-destructive'
import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess, toastWarning } from '@/components/ui/toast'

import {
  mapearErrorConvocatorias,
  useAbrirConvocatoria,
  useCerrarConvocatoria,
  useRefrescarConvocatorias,
} from '../api'
import type { ErrorConvocatorias } from '../api'
import type { Convocatoria } from '../types'
import { AlertaConvocatorias } from './AlertaConvocatorias'

const FORM_ID = 'cambiar-estado-convocatoria-form'

export type AccionEstadoConvocatoria = 'abrir' | 'cerrar'

interface CambiarEstadoConvocatoriaDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  convocatoria: Convocatoria
  accion: AccionEstadoConvocatoria
}

/** Confirmación para abrir (A3) o cerrar (A4) una convocatoria. */
export function CambiarEstadoConvocatoriaDialog({
  open,
  onOpenChange,
  convocatoria,
  accion,
}: CambiarEstadoConvocatoriaDialogProps) {
  const abrir = useAbrirConvocatoria()
  const cerrar = useCerrarConvocatoria()
  const refrescar = useRefrescarConvocatorias()
  const [error, setError] = useState<ErrorConvocatorias | null>(null)
  const enviando = useRef(false)

  const mutacion = accion === 'abrir' ? abrir : cerrar

  const confirmar = () => {
    if (enviando.current) return
    enviando.current = true
    setError(null)
    mutacion.mutate(convocatoria.codigo, {
      onSuccess: () => {
        if (accion === 'abrir') toastSuccess('Convocatoria abierta', `${convocatoria.nombre} ya recibe postulaciones.`)
        else toastSuccess('Convocatoria cerrada', `${convocatoria.nombre} dejó de recibir postulaciones.`)
        onOpenChange(false)
      },
      onError: (e) => {
        const mapeado = mapearErrorConvocatorias(e)
        if (mapeado.refrescar) {
          // Cambió de estado desde otra sesión o por el cierre automático, o ya no existe (A8, A9): se actualiza la lista.
          toastWarning('Sin cambios', mapeado.mensaje)
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

  if (accion === 'cerrar') {
    const base = `${convocatoria.nombre} dejará de recibir postulaciones y los borradores que no se enviaron quedarán bloqueados.`
    return (
      <ModalDestructive
        open={open}
        onOpenChange={onOpenChange}
        title="¿Cerrar convocatoria?"
        description={error ? `${base} ${error.mensaje}` : base}
        confirmLabel={error?.red ? 'Reintentar' : 'Cerrar convocatoria'}
        isSubmitting={mutacion.isPending}
        onConfirm={confirmar}
      />
    )
  }

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      title="Abrir convocatoria"
      subtitle={`${convocatoria.nombre} · ${convocatoria.gestion.etiqueta}`}
      icon={PlayIcon}
      size="sm"
      formId={FORM_ID}
      isSubmitting={mutacion.isPending}
      confirmLabel={error?.red ? 'Reintentar' : 'Abrir'}
    >
      <form
        id={FORM_ID}
        className="flex flex-col gap-4"
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          confirmar()
        }}
      >
        {error && <AlertaConvocatorias error={error} />}
        <p className="text-sm text-foreground">
          Al abrirla, <strong>{convocatoria.nombre}</strong> empezará a recibir postulaciones. Solo puede haber una
          convocatoria abierta por gestión académica.
        </p>
      </form>
    </ModalLayout>
  )
}
