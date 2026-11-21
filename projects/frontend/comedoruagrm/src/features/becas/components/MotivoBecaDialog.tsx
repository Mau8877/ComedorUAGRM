import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { BanIcon, PauseCircleIcon } from 'lucide-react'

import { Textarea } from '@/components/ui/textarea'
import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess, toastWarning } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'

import { mapearErrorBecas, useDarDeBaja, useSuspender } from '../api'
import { motivoSchema } from '../schemas'
import type { Beca } from '../types'
import { errorVisible } from '@/utils/errorCampo'
import { validacionFormulario } from '@/utils/validacionFormulario'

const ID_FORMULARIO = 'motivo-beca'

export type AccionMotivo = 'baja' | 'suspension'

const TEXTOS: Record<AccionMotivo, { titulo: string; confirmar: string; exito: string; aviso?: string }> = {
  baja: {
    titulo: 'Dar de baja',
    confirmar: 'Dar de baja',
    exito: 'Beca dada de baja',
    aviso: 'La baja es definitiva: la beca no se puede reactivar. Después podrás reemplazar su cupo.',
  },
  suspension: { titulo: 'Suspender beca', confirmar: 'Suspender', exito: 'Beca suspendida' },
}

interface MotivoBecaDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** La beca vigente a la que se le cambia el estado. */
  beca: Beca
  accion: AccionMotivo
}

/**
 * Ventana de baja o suspensión de una beca (CU23, flujo B): pide el motivo (obligatorio, A1) y confirma. Cancelar no
 * cambia nada (A5). Si el backend dice que la beca ya cambió (ERR_BEC_05 o 01), avisa y cierra: el padrón se vuelve
 * a pedir solo.
 */
export function MotivoBecaDialog({ open, onOpenChange, beca, accion }: MotivoBecaDialogProps) {
  const darDeBaja = useDarDeBaja()
  const suspender = useSuspender()
  const mutacion = accion === 'baja' ? darDeBaja : suspender
  const textos = TEXTOS[accion]
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)
  const nombre = `${beca.becado.nombres} ${beca.becado.apellidos}`

  const form = useForm({
    defaultValues: { motivo: '' },
    ...validacionFormulario(motivoSchema),
    onSubmit: ({ value }) => {
      if (enviando.current) return
      enviando.current = true
      setError(null)
      const { motivo } = motivoSchema.parse(value)
      mutacion.mutate(
        { beca: beca.codigo, motivo },
        {
          onSuccess: () => {
            toastSuccess(textos.exito, nombre)
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
    },
  })

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      icon={accion === 'baja' ? BanIcon : PauseCircleIcon}
      title={textos.titulo}
      subtitle={nombre}
      formId={ID_FORMULARIO}
      isSubmitting={mutacion.isPending}
      confirmLabel={mutacion.isPending ? 'Guardando…' : textos.confirmar}
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
        {textos.aviso && (
          <p role="note" className="rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-warning">
            {textos.aviso}
          </p>
        )}
        <form.Field name="motivo">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor={field.name} className="text-sm font-medium text-foreground">
                Motivo
              </label>
              <Textarea
                id={field.name}
                name={field.name}
                rows={3}
                value={field.state.value}
                aria-invalid={errorVisible(field) && field.state.meta.errors.length > 0 ? true : undefined}
                onBlur={field.handleBlur}
                onChange={(e) => {
                  if (error) setError(null)
                  field.handleChange(e.target.value)
                }}
              />
              {errorVisible(field) && field.state.meta.errors[0] && (
                <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
              )}
            </div>
          )}
        </form.Field>
        {error && <AlertaError>{error}</AlertaError>}
      </form>
    </ModalLayout>
  )
}
