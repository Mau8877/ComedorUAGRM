import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { BellRingIcon } from 'lucide-react'

import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ModalDetailLayout, ModalLayout } from '@/components/ui/modal-layout'
import { Textarea } from '@/components/ui/textarea'
import { AlertaError } from '@/features/auth'

import { mapearErrorFinalistas } from '../api/mapearErrorFinalistas'
import { useNotificarFinalistas } from '../api/useFinalistas'
import { aNotificarRequest, mananaIso, notificarFinalistasSchema } from '../schemas'
import type { NotificarFinalistasValues } from '../schemas'
import type { ResultadoNotificacion } from '../types'

const ID_FORMULARIO = 'notificar-finalistas'

interface NotificarFinalistasDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Códigos de los finalistas elegidos. */
  finalistas: string[]
  /** Se llama cuando el envío terminó bien (la pantalla limpia la selección). */
  onNotificados: () => void
}

function ErrorCampo({ error }: { error: string | undefined }) {
  return error ? <span className="text-xs text-destructive">{error}</span> : null
}

/**
 * Notificar a los finalistas elegidos (CU21, flujo A, pasos 4 a 7): plazo de entrega, lugar y horario y
 * documentos físicos. Al enviar pide confirmación; después muestra cuántos avisos salieron y cuántos fallaron
 * (los fallidos se vuelven a elegir con «Seleccionar fallidos» de la tabla, A2). No se envía nada si falta un
 * dato (A6): el backend lo valida también.
 */
export function NotificarFinalistasDialog({
  open,
  onOpenChange,
  finalistas,
  onNotificados,
}: NotificarFinalistasDialogProps) {
  const notificar = useNotificarFinalistas()
  const [pendiente, setPendiente] = useState<NotificarFinalistasValues | null>(null)
  const [resultado, setResultado] = useState<ResultadoNotificacion | null>(null)
  const [error, setError] = useState<string | null>(null)

  const form = useForm({
    defaultValues: { plazoEntrega: '', lugarHorario: '', documentos: '' } as NotificarFinalistasValues,
    validators: { onChange: notificarFinalistasSchema, onBlur: notificarFinalistasSchema },
    onSubmit: ({ value }) => {
      setError(null)
      setPendiente(value)
    },
  })

  const cerrar = (abierto: boolean) => {
    if (!abierto) {
      form.reset()
      setResultado(null)
      setError(null)
      setPendiente(null)
    }
    onOpenChange(abierto)
  }

  const confirmar = () => {
    if (!pendiente) return
    const valores = pendiente
    setPendiente(null)
    notificar.mutate(aNotificarRequest(finalistas, valores), {
      onSuccess: (r) => {
        setResultado(r)
        onNotificados()
      },
      onError: (e) => setError(mapearErrorFinalistas(e).mensaje),
    })
  }

  if (resultado) {
    return (
      <ModalDetailLayout
        open={open}
        onOpenChange={cerrar}
        icon={BellRingIcon}
        title="Resultado del envío"
        subtitle="Aviso a los finalistas"
        size="sm"
      >
        <p className="text-sm" role="status">
          Se enviaron <strong>{resultado.enviados}</strong> avisos y fallaron <strong>{resultado.fallidos}</strong>.
        </p>
        <p className="mt-2 text-sm text-muted-foreground">
          El push y el correo salen en segundo plano: si un finalista queda como «Fallida» en la tabla, usa
          «Seleccionar fallidos» y vuelve a notificar solo a esos.
        </p>
      </ModalDetailLayout>
    )
  }

  return (
    <>
      <ModalLayout
        open={open}
        onOpenChange={cerrar}
        icon={BellRingIcon}
        title="Notificar seleccionados"
        subtitle={`Se enviará a ${finalistas.length} ${finalistas.length === 1 ? 'finalista' : 'finalistas'}`}
        formId={ID_FORMULARIO}
        isSubmitting={notificar.isPending}
        confirmLabel="Enviar aviso"
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

          <form.Field name="plazoEntrega">
            {(field) => (
              <div className="flex flex-col gap-1">
                <label htmlFor="notificar-plazo" className="text-sm font-medium">
                  Plazo de entrega
                </label>
                <Input
                  id="notificar-plazo"
                  type="date"
                  min={mananaIso()}
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={(e) => field.handleChange(e.target.value)}
                />
                <ErrorCampo error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined} />
              </div>
            )}
          </form.Field>

          <form.Field name="lugarHorario">
            {(field) => (
              <div className="flex flex-col gap-1">
                <label htmlFor="notificar-lugar" className="text-sm font-medium">
                  Lugar y horario de entrega
                </label>
                <Input
                  id="notificar-lugar"
                  placeholder="Ej.: DUBSS, oficina 3, lunes a viernes de 8:00 a 12:00"
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={(e) => field.handleChange(e.target.value)}
                />
                <ErrorCampo error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined} />
              </div>
            )}
          </form.Field>

          <form.Field name="documentos">
            {(field) => (
              <div className="flex flex-col gap-1">
                <label htmlFor="notificar-documentos" className="text-sm font-medium">
                  Documentos físicos a presentar
                </label>
                <Textarea
                  id="notificar-documentos"
                  rows={3}
                  placeholder="Ej.: carnet de identidad, certificado de ingresos"
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={(e) => field.handleChange(e.target.value)}
                />
                <ErrorCampo error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined} />
              </div>
            )}
          </form.Field>
        </form>
      </ModalLayout>

      <AlertDialog open={pendiente !== null} onOpenChange={(abierto) => !abierto && setPendiente(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Enviar el aviso?</AlertDialogTitle>
            <AlertDialogDescription>
              Se avisará a {finalistas.length} {finalistas.length === 1 ? 'finalista' : 'finalistas'} por la
              bandeja, push y correo. Podrás reintentar solo a los que fallen.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <Button variant="outline" onClick={() => setPendiente(null)}>
              Volver
            </Button>
            <Button onClick={confirmar}>Confirmar envío</Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
