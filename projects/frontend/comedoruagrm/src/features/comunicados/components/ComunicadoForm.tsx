import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { Loader2Icon } from 'lucide-react'

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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { toastSuccess } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'
import { mapearErrorComunicados } from '../api/mapearErrorComunicados'
import { usePublicarComunicado } from '../api/useComunicados'
import { aPublicarRequest, comunicadoSchema, ETIQUETAS_DESTINATARIOS, requiereConvocatoria } from '../schemas'
import type { ComunicadoValues } from '../schemas'
import type { DestinatariosComunicado } from '../types'

interface ComunicadoFormProps {
  /** Convocatoria elegida en la pantalla: obligatoria para dirigirlo a los finalistas o a los adjudicados y acota a «Todos». */
  convocatoriaId?: string
}

const VALORES_INICIALES: ComunicadoValues = { titulo: '', contenido: '', destinatarios: 'TODOS' }

/**
 * Nuevo aviso o comunicado (CU21, flujo C): título, contenido y a quién va dirigido (todos los postulantes
 * o los finalistas), con vista previa y confirmación antes de publicar. Al terminar dice a cuántos llegó.
 * Sin borrador ni publicación programada (fuera del CU). Un comunicado publicado no se edita ni se borra.
 */
export function ComunicadoForm({ convocatoriaId }: ComunicadoFormProps) {
  const publicar = usePublicarComunicado()
  const [pendiente, setPendiente] = useState<ComunicadoValues | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [enviadoA, setEnviadoA] = useState<number | null>(null)

  const form = useForm({
    defaultValues: VALORES_INICIALES,
    validators: { onChange: comunicadoSchema, onBlur: comunicadoSchema },
    onSubmit: ({ value }) => {
      setError(null)
      setEnviadoA(null)
      // Sin convocatoria no se sabe a quién avisar: se corta acá, igual que lo haría el backend (ERR_COM_02).
      if (requiereConvocatoria(value.destinatarios) && !convocatoriaId) return
      setPendiente(value)
    },
  })

  const confirmar = () => {
    if (!pendiente) return
    const valores = pendiente
    setPendiente(null)
    publicar.mutate(aPublicarRequest(valores, convocatoriaId), {
      onSuccess: (comunicado) => {
        setEnviadoA(comunicado.totalDestinatarios)
        toastSuccess('Comunicado publicado', `Llegó a ${comunicado.totalDestinatarios} destinatarios`)
        form.reset()
      },
      onError: (e) => setError(mapearErrorComunicados(e)),
    })
  }

  return (
    <section aria-labelledby="comunicado-titulo" className="rounded-2xl border border-border bg-card p-4">
      <h2 id="comunicado-titulo" className="mb-3 font-heading text-base font-semibold">
        Nuevo aviso o comunicado
      </h2>

      <form
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          void form.handleSubmit()
        }}
      >
        {error && <AlertaError>{error}</AlertaError>}
        {enviadoA !== null && (
          <p role="status" className="rounded-md border border-success/30 bg-success/10 p-3 text-sm text-success">
            Se envió a {enviadoA} {enviadoA === 1 ? 'destinatario' : 'destinatarios'}.
          </p>
        )}

        <form.Field name="titulo">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor="comunicado-titulo-campo" className="text-sm font-medium">
                Título
              </label>
              <Input
                id="comunicado-titulo-campo"
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              {field.state.meta.isTouched && field.state.meta.errors[0] && (
                <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
              )}
            </div>
          )}
        </form.Field>

        <form.Field name="contenido">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor="comunicado-contenido" className="text-sm font-medium">
                Contenido
              </label>
              <Textarea
                id="comunicado-contenido"
                rows={4}
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              {field.state.meta.isTouched && field.state.meta.errors[0] && (
                <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
              )}
            </div>
          )}
        </form.Field>

        <form.Field name="destinatarios">
          {(field) => (
            <div className="flex flex-col gap-1">
              <span className="text-sm font-medium">Dirigido a</span>
              <Select
                value={field.state.value}
                onValueChange={(valor) => valor && field.handleChange(valor as DestinatariosComunicado)}
              >
                <SelectTrigger aria-label="Dirigido a" onBlur={field.handleBlur}>
                  <SelectValue>
                    {(valor: DestinatariosComunicado | null) => (valor ? ETIQUETAS_DESTINATARIOS[valor] : '')}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(ETIQUETAS_DESTINATARIOS) as DestinatariosComunicado[]).map((d) => (
                    <SelectItem key={d} value={d}>
                      {ETIQUETAS_DESTINATARIOS[d]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {requiereConvocatoria(field.state.value) && !convocatoriaId && (
                <span className="text-xs text-destructive">
                  Elige una convocatoria para avisar a los {field.state.value === 'ADJUDICADOS' ? 'adjudicados' : 'finalistas'}.
                </span>
              )}
            </div>
          )}
        </form.Field>

        <form.Subscribe selector={(estado) => estado.values}>
          {(valores) =>
            (valores.titulo.trim() || valores.contenido.trim()) && (
              <div aria-label="Vista previa" className="rounded-md border border-dashed border-border bg-muted/40 p-3">
                <p className="text-xs text-muted-foreground">Vista previa: así lo verán los destinatarios</p>
                <p className="mt-1 text-sm font-semibold">{valores.titulo.trim() || '(sin título)'}</p>
                <p className="mt-1 whitespace-pre-line text-sm">{valores.contenido.trim() || '(sin contenido)'}</p>
              </div>
            )
          }
        </form.Subscribe>

        <Button type="submit" className="w-fit" disabled={publicar.isPending}>
          {publicar.isPending && <Loader2Icon className="animate-spin" />}
          Revisar y publicar
        </Button>
      </form>

      <AlertDialog open={pendiente !== null} onOpenChange={(abierto) => !abierto && setPendiente(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Publicar el comunicado?</AlertDialogTitle>
            <AlertDialogDescription>
              Se enviará a {pendiente ? ETIQUETAS_DESTINATARIOS[pendiente.destinatarios].toLowerCase() : ''} y no podrás
              editarlo ni borrarlo después.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <Button variant="outline" onClick={() => setPendiente(null)}>
              Volver
            </Button>
            <Button onClick={confirmar}>Confirmar publicación</Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </section>
  )
}
