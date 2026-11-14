import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { UsersRoundIcon } from 'lucide-react'

import { Input } from '@/components/ui/input'
import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'

import { mapearErrorAdjudicacion } from '../api/mapearErrorAdjudicacion'
import { useSeleccionarFinalistas } from '../api/useSeleccionarFinalistas'
import { aSeleccionRequest, seleccionSchema, seleccionSchemaRequerido } from '../schemas'
import type { SeleccionValues } from '../schemas'
import type { PanelAdjudicacion, SeleccionFinalistasResultado } from '../types'

const ID_FORMULARIO = 'seleccionar-finalistas'

interface SeleccionarFinalistasDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** El panel de la convocatoria: de él salen la cantidad propuesta, los cupos y las postulaciones disponibles. */
  panel: PanelAdjudicacion
  /** Se llama cuando la selección terminó bien. */
  onSeleccionados?: (resultado: SeleccionFinalistasResultado) => void
  /** El backend dijo que la convocatoria ya no sirve (ERR_ADJ_01 o ERR_ADJ_05): la pantalla refresca el panel. */
  onDesactualizado?: () => void
  /** El backend dijo que la convocatoria ya no existe (ERR_PRI_01): la pantalla vuelve a elegir. */
  onConvocatoriaInexistente?: () => void
}

/** La cantidad que se propone: la configurada, sin pasar de las postulaciones que aún no son finalistas. */
function cantidadPropuesta(panel: PanelAdjudicacion): string {
  if (panel.cantidadFinalistas === null || panel.calificadasSinSeleccionar === 0) return ''
  return String(Math.min(panel.cantidadFinalistas, panel.calificadasSinSeleccionar))
}

/**
 * Diálogo «Seleccionar finalistas» (CU05): propone la cantidad configurada en la convocatoria y deja ajustarla.
 * Elegir más finalistas que cupos es válido (los de más son suplentes) y ajustar la cantidad no cambia la
 * configurada. No se puede enviar dos veces seguidas: el botón se deshabilita mientras se envía.
 */
export function SeleccionarFinalistasDialog({
  open,
  onOpenChange,
  panel,
  onSeleccionados,
  onDesactualizado,
  onConvocatoriaInexistente,
}: SeleccionarFinalistasDialogProps) {
  const seleccionar = useSeleccionarFinalistas()
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)
  // Sin cantidad configurada hay que escribirla; con una configurada, vaciar el campo la usa.
  const schema = panel.cantidadFinalistas === null ? seleccionSchemaRequerido : seleccionSchema

  const form = useForm({
    defaultValues: { cantidad: cantidadPropuesta(panel) } as SeleccionValues,
    validators: { onChange: schema, onBlur: schema },
    onSubmit: ({ value }) => {
      const analizado = schema.safeParse(value)
      if (!analizado.success || enviando.current) return
      enviando.current = true
      setError(null)
      seleccionar.mutate(
        { convocatoria: panel.convocatoria.codigo, pedido: aSeleccionRequest(analizado.data) },
        {
          onSuccess: (resultado) => {
            toastSuccess(
              resultado.seleccionados === 1
                ? '1 finalista seleccionado correctamente'
                : `${resultado.seleccionados} finalistas seleccionados correctamente`,
              'Ya puedes auditar sus expedientes.',
            )
            onSeleccionados?.(resultado)
            onOpenChange(false)
          },
          onError: (e) => {
            const mapeado = mapearErrorAdjudicacion(e)
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
        },
      )
    },
  })

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      icon={UsersRoundIcon}
      title="Seleccionar finalistas"
      subtitle="Se eligen por orden de puntaje, sin repetir a los ya seleccionados"
      formId={ID_FORMULARIO}
      isSubmitting={seleccionar.isPending}
      confirmLabel="Seleccionar"
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
          Convocatoria <strong>{panel.convocatoria.nombre}</strong>: hay{' '}
          <strong>{panel.calificadasSinSeleccionar}</strong> postulaciones calificadas sin seleccionar y{' '}
          <strong>{panel.cuposDisponibles === null ? 'sin definir' : panel.cuposDisponibles}</strong> cupos
          disponibles.
        </p>
        <p role="note" className="text-xs text-muted-foreground">
          Puedes elegir más finalistas que cupos: los de más quedan como suplentes. Ajustar la cantidad aquí no
          cambia la configurada en la convocatoria
          {panel.cantidadFinalistas === null ? ' (esta convocatoria no tiene una configurada)' : ''}.
        </p>

        {error && <AlertaError>{error}</AlertaError>}

        <form.Field name="cantidad">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor="seleccion-cantidad" className="text-sm font-medium">
                Cantidad
              </label>
              <Input
                id="seleccion-cantidad"
                inputMode="numeric"
                placeholder="Cuántos finalistas seleccionar"
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              <span className="text-xs text-destructive">
                {field.state.meta.isTouched && field.state.meta.errors[0]
                  ? field.state.meta.errors[0].message
                  : ''}
              </span>
            </div>
          )}
        </form.Field>
      </form>
    </ModalLayout>
  )
}
