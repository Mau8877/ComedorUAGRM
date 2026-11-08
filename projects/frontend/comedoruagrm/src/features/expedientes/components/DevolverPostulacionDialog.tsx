import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { UndoIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { Textarea } from '@/components/ui/textarea'
import { toastSuccess } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'

import { mapearErrorExpedientes } from '../api/mapearErrorExpedientes'
import { useDevolverPostulacion } from '../api/useExpediente'
import { aDevolucionRequest, devolucionSchema, MAX_MOTIVO } from '../schemas'
import type { DevolucionFormValues } from '../schemas'
import type { DevolucionResultado } from '../types'

const ID_FORMULARIO = 'devolver-postulacion'
const VALORES_INICIALES: DevolucionFormValues = { motivo: '' }

interface DevolverPostulacionDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Código del finalista cuya postulación se devuelve. */
  finalista: string
  /** Se llama cuando la devolución terminó bien. */
  onDevuelta?: (resultado: DevolucionResultado) => void
  /** El backend dijo que el finalista ya no existe (ERR_FIN_01): la pantalla vuelve a la lista. */
  onFinalistaInexistente?: () => void
}

/**
 * Diálogo «Devolver para corrección» (CU19): el motivo es obligatorio y de hasta 500 caracteres. Siempre se
 * avisa al postulante (bandeja, push y correo), por eso no hay opción de no notificar. No se puede enviar dos
 * veces seguidas: el botón se deshabilita mientras se envía.
 */
export function DevolverPostulacionDialog({
  open,
  onOpenChange,
  finalista,
  onDevuelta,
  onFinalistaInexistente,
}: DevolverPostulacionDialogProps) {
  const devolver = useDevolverPostulacion()
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)

  const form = useForm({
    defaultValues: VALORES_INICIALES,
    validators: { onChange: devolucionSchema, onBlur: devolucionSchema },
    onSubmit: ({ value }) => {
      const analizado = devolucionSchema.safeParse(value)
      if (!analizado.success || enviando.current) return
      enviando.current = true
      setError(null)
      devolver.mutate(
        { codigo: finalista, pedido: aDevolucionRequest(analizado.data) },
        {
          onSuccess: (resultado) => {
            toastSuccess('Postulación devuelta', 'Se avisó al postulante para que la corrija.')
            onDevuelta?.(resultado)
            onOpenChange(false)
          },
          onError: (e) => {
            const mapeado = mapearErrorExpedientes(e)
            if (mapeado.finalistaInexistente) {
              onFinalistaInexistente?.()
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
    },
  })

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      icon={UndoIcon}
      title="Devolver para corrección"
      subtitle="Se avisará al postulante por la bandeja, el push y el correo"
      formId={ID_FORMULARIO}
      isSubmitting={devolver.isPending}
      confirmLabel="Devolver"
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
        {error && <AlertaError>{error}</AlertaError>}

        <form.Field name="motivo">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor="devolver-motivo" className="text-sm font-medium">
                Motivo
              </label>
              <Textarea
                id="devolver-motivo"
                rows={5}
                placeholder="Explica qué debe corregir el postulante"
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
