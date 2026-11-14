import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { AwardIcon, BanIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { Textarea } from '@/components/ui/textarea'
import { toastSuccess } from '@/components/ui/toast'
import type { PanelAdjudicacion } from '@/features/adjudicacion'
import { AlertaError } from '@/features/auth'

import { mapearErrorExpedientes } from '../api/mapearErrorExpedientes'
import { useAdjudicarFinalista } from '../api/useAdjudicarFinalista'
import { useNoAdjudicarFinalista } from '../api/useNoAdjudicarFinalista'
import { aNoAdjudicarRequest, MAX_MOTIVO, noAdjudicarSchema } from '../schemas'
import type { NoAdjudicarFormValues } from '../schemas'
import type { AccionDecision, DecisionAdjudicacionResultado, Expediente } from '../types'
import { formatearPuntos } from './formatos'

const ID_FORMULARIO = 'decidir-adjudicacion'
const VALORES_INICIALES: NoAdjudicarFormValues = { motivo: '' }

// * Todo lo que cambia entre las dos acciones: el resto del diálogo es igual.
const TEXTOS: Record<
  AccionDecision,
  { titulo: string; boton: string; toast: string; icono: typeof AwardIcon }
> = {
  adjudicar: {
    titulo: 'Adjudicar beca',
    boton: 'Adjudicar',
    toast: 'Beca adjudicada correctamente',
    icono: AwardIcon,
  },
  'no-adjudicar': {
    titulo: 'No adjudicar',
    boton: 'No adjudicar',
    toast: 'Postulación marcada como no adjudicada correctamente',
    icono: BanIcon,
  },
}

interface DecidirAdjudicacionDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Qué se decide: adjudicar la beca o no adjudicarla. */
  accion: AccionDecision
  /** El expediente del finalista: de él salen el nombre y el puntaje total. */
  expediente: Expediente
  /** El panel de la convocatoria (cupos disponibles y puntaje mínimo); `undefined` mientras no cargó. */
  panel?: PanelAdjudicacion
  /** Se llama cuando la decisión se registró. */
  onDecidida?: (resultado: DecisionAdjudicacionResultado) => void
  /** El backend dijo que ya no se puede (ERR_ADJ_01 o ERR_ADJ_04): la pantalla refresca el expediente y los cupos. */
  onDesactualizado?: () => void
  /** El backend dijo que el finalista ya no existe (ERR_FIN_01): la pantalla vuelve a la lista. */
  onFinalistaInexistente?: () => void
}

function ErrorCampo({ error }: { error: string | undefined }) {
  return <span className="text-xs text-destructive">{error ?? ''}</span>
}

/**
 * Diálogo «Adjudicar» / «No adjudicar» (CU05): muestra a quién afecta, su puntaje total y los cupos. Adjudicar no
 * lleva campos; «No adjudicar» exige un motivo de hasta 500 caracteres. La decisión es definitiva y no se le avisa
 * al postulante ahora (lo conocerá al publicarse los resultados). No se puede enviar dos veces seguidas: el botón
 * se deshabilita mientras se envía.
 */
export function DecidirAdjudicacionDialog({
  open,
  onOpenChange,
  accion,
  expediente,
  panel,
  onDecidida,
  onDesactualizado,
  onFinalistaInexistente,
}: DecidirAdjudicacionDialogProps) {
  const adjudicar = useAdjudicarFinalista()
  const noAdjudicar = useNoAdjudicarFinalista()
  const mutacion = accion === 'adjudicar' ? adjudicar : noAdjudicar
  const textos = TEXTOS[accion]
  const Icono = textos.icono
  const pideMotivo = accion === 'no-adjudicar'
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)
  const codigo = expediente.finalista.codigo

  const alTerminar = {
    onSuccess: (resultado: DecisionAdjudicacionResultado) => {
      toastSuccess(textos.toast)
      onDecidida?.(resultado)
      onOpenChange(false)
    },
    onError: (e: unknown) => {
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
  }

  const form = useForm({
    defaultValues: VALORES_INICIALES,
    validators: pideMotivo ? { onChange: noAdjudicarSchema, onBlur: noAdjudicarSchema } : undefined,
    onSubmit: ({ value }) => {
      if (enviando.current) return
      if (!pideMotivo) {
        enviando.current = true
        setError(null)
        adjudicar.mutate({ codigo }, alTerminar)
        return
      }
      const analizado = noAdjudicarSchema.safeParse(value)
      if (!analizado.success) return
      enviando.current = true
      setError(null)
      noAdjudicar.mutate({ codigo, pedido: aNoAdjudicarRequest(analizado.data) }, alTerminar)
    },
  })

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      icon={Icono}
      title={textos.titulo}
      subtitle="Decisión final sobre el finalista auditado"
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
          Postulación de{' '}
          <strong>
            {expediente.postulante.nombres} {expediente.postulante.apellidos}
          </strong>{' '}
          en <strong>{expediente.convocatoria.nombre}</strong>.
        </p>
        <dl className="grid grid-cols-3 gap-3">
          <div className="flex flex-col">
            <dt className="text-xs text-muted-foreground">Puntaje total</dt>
            <dd className="text-sm font-medium">
              {expediente.puntajeTotal === null ? 'No disponible' : formatearPuntos(expediente.puntajeTotal)}
            </dd>
          </div>
          <div className="flex flex-col">
            <dt className="text-xs text-muted-foreground">Cupos disponibles</dt>
            <dd className="text-sm font-medium">
              {panel?.cuposDisponibles === undefined || panel.cuposDisponibles === null
                ? 'Sin definir'
                : panel.cuposDisponibles}
            </dd>
          </div>
          <div className="flex flex-col">
            <dt className="text-xs text-muted-foreground">Puntaje mínimo</dt>
            <dd className="text-sm font-medium">
              {panel?.puntajeMinimo === undefined || panel.puntajeMinimo === null
                ? 'Sin definir'
                : formatearPuntos(panel.puntajeMinimo)}
            </dd>
          </div>
        </dl>
        <p role="note" className="text-xs text-muted-foreground">
          La decisión es definitiva: no se puede reabrir ni cambiar. El postulante la conocerá cuando se publiquen
          los resultados; no se le avisa ahora.
        </p>

        {error && <AlertaError>{error}</AlertaError>}

        {pideMotivo && (
          <form.Field name="motivo">
            {(field) => (
              <div className="flex flex-col gap-1">
                <label htmlFor="decidir-motivo" className="text-sm font-medium">
                  Motivo
                </label>
                <Textarea
                  id="decidir-motivo"
                  rows={5}
                  placeholder="Explica por qué no se adjudica la beca"
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={(e) => field.handleChange(e.target.value)}
                />
                <div className="flex justify-between gap-2">
                  <ErrorCampo
                    error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
                  />
                  <span className="text-xs text-muted-foreground">
                    {field.state.value.length}/{MAX_MOTIVO}
                  </span>
                </div>
              </div>
            )}
          </form.Field>
        )}
      </form>
    </ModalLayout>
  )
}
