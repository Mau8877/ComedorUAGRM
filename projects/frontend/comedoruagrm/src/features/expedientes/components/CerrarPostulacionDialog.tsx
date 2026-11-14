import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { ArchiveIcon, BanIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { Textarea } from '@/components/ui/textarea'
import { toastSuccess } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'

import { mapearErrorExpedientes } from '../api/mapearErrorExpedientes'
import { useArchivarPostulacion } from '../api/useArchivarPostulacion'
import { useRechazarPostulacion } from '../api/useRechazarPostulacion'
import { aCierreRequest, cierreSchema, MAX_MOTIVO } from '../schemas'
import type { CierreFormValues } from '../schemas'
import type { AccionCierre, CierrePostulacionResultado } from '../types'

const ID_FORMULARIO = 'cerrar-postulacion'
const VALORES_INICIALES: CierreFormValues = { motivo: '' }

// * Todo lo que cambia entre las dos acciones: el resto del diálogo (motivo, contador, validación) es igual.
const TEXTOS: Record<
  AccionCierre,
  { titulo: string; boton: string; toastTitulo: string; toastDetalle: string; icono: typeof ArchiveIcon }
> = {
  archivar: {
    titulo: 'Archivar postulación',
    boton: 'Archivar',
    toastTitulo: 'Postulación archivada correctamente',
    toastDetalle: 'Se avisó al postulante.',
    icono: ArchiveIcon,
  },
  rechazar: {
    titulo: 'Rechazar postulación',
    boton: 'Rechazar',
    toastTitulo: 'Postulación rechazada correctamente',
    toastDetalle: 'Quedó como no adjudicada y se avisó al postulante.',
    icono: BanIcon,
  },
}

interface CerrarPostulacionDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Qué se hace con la postulación: archivarla o rechazarla. */
  accion: AccionCierre
  /** Código del finalista cuya postulación se cierra. */
  finalista: string
  /** Nombre del postulante, para que el administrador confirme a quién afecta. */
  postulante: string
  /** Nombre de la convocatoria. */
  convocatoria: string
  /** Se llama cuando el cierre terminó bien. */
  onCerrada?: (resultado: CierrePostulacionResultado) => void
  /** El backend dijo que la postulación ya no está calificada (ERR_ARC_01): la pantalla refresca el expediente. */
  onDesactualizado?: () => void
  /** El backend dijo que el finalista ya no existe (ERR_FIN_01): la pantalla vuelve a la lista. */
  onFinalistaInexistente?: () => void
}

/**
 * Diálogo «Archivar» / «Rechazar» postulación (CU20): el motivo es obligatorio y de hasta 500 caracteres.
 * Siempre se avisa al postulante (bandeja, push y correo), por eso no hay opción de no notificar. No se puede
 * enviar dos veces seguidas: el botón se deshabilita mientras se envía.
 */
export function CerrarPostulacionDialog({
  open,
  onOpenChange,
  accion,
  finalista,
  postulante,
  convocatoria,
  onCerrada,
  onDesactualizado,
  onFinalistaInexistente,
}: CerrarPostulacionDialogProps) {
  const archivar = useArchivarPostulacion()
  const rechazar = useRechazarPostulacion()
  const mutacion = accion === 'archivar' ? archivar : rechazar
  const textos = TEXTOS[accion]
  const Icono = textos.icono
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)

  const form = useForm({
    defaultValues: VALORES_INICIALES,
    validators: { onChange: cierreSchema, onBlur: cierreSchema },
    onSubmit: ({ value }) => {
      const analizado = cierreSchema.safeParse(value)
      if (!analizado.success || enviando.current) return
      enviando.current = true
      setError(null)
      mutacion.mutate(
        { codigo: finalista, pedido: aCierreRequest(analizado.data) },
        {
          onSuccess: (resultado) => {
            toastSuccess(textos.toastTitulo, textos.toastDetalle)
            onCerrada?.(resultado)
            onOpenChange(false)
          },
          onError: (e) => {
            const mapeado = mapearErrorExpedientes(e)
            if (mapeado.finalistaInexistente) {
              onFinalistaInexistente?.()
              onOpenChange(false)
              return
            }
            if (mapeado.refrescar) onDesactualizado?.()
            setError(mapeado.mensaje)
          },
          onSettled: () => {
            enviando.current = false
          },
        },
      )
    },
  })

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      icon={Icono}
      title={textos.titulo}
      subtitle="Se avisará al postulante por la bandeja, el push y el correo"
      formId={ID_FORMULARIO}
      isSubmitting={mutacion.isPending}
      confirmLabel={textos.boton}
    >
      <form
        id={ID_FORMULARIO}
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          void form.handleSubmit()
        }}
      >
        <p className="text-sm">
          Postulación de <strong>{postulante}</strong> en <strong>{convocatoria}</strong>.
        </p>
        <p role="note" className="text-xs text-muted-foreground">
          La decisión es definitiva para esta gestión: la postulación no vuelve a estar calificada ni se reenvía.
        </p>

        {error && <AlertaError>{error}</AlertaError>}

        <form.Field name="motivo">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor="cerrar-motivo" className="text-sm font-medium">
                Motivo
              </label>
              <Textarea
                id="cerrar-motivo"
                rows={5}
                placeholder="Explica por qué se cierra la postulación"
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              <div className="flex justify-between gap-2">
                <span className="text-xs text-destructive">
                  {field.state.meta.isTouched && field.state.meta.errors[0]
                    ? field.state.meta.errors[0].message
                    : ''}
                </span>
                <span className="text-xs text-muted-foreground">
                  {field.state.value.length}/{MAX_MOTIVO}
                </span>
              </div>
            </div>
          )}
        </form.Field>
      </form>
    </ModalLayout>
  )
}
