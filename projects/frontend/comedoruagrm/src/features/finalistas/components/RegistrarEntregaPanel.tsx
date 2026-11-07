import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { Loader2Icon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { toastSuccess } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'

import { mapearErrorFinalistas } from '../api/mapearErrorFinalistas'
import { useRegistrarEntrega } from '../api/useFinalistas'
import { aEntregaRequest, hoyIso, registrarEntregaSchema } from '../schemas'
import type { Finalista } from '../types'
import { EntregaBadge } from './EstadoBadges'
import { formatearFechaDia } from './formatos'

interface RegistrarEntregaPanelProps {
  /** El finalista elegido en la tabla; sin él, el panel pide elegir uno. */
  finalista?: Finalista
  /** Se llama con el finalista ya actualizado (el estado de su entrega cambió). */
  onRegistrada?: (actualizado: Finalista) => void
}

function FormularioEntrega({
  finalista,
  onRegistrada,
}: {
  finalista: Finalista
  onRegistrada?: (actualizado: Finalista) => void
}) {
  const registrar = useRegistrarEntrega()
  const [error, setError] = useState<string | null>(null)
  const yaRecibida = Boolean(finalista.documentacionRecibidaEn)

  const form = useForm({
    defaultValues: { fechaRecepcion: finalista.documentacionRecibidaEn ?? hoyIso(), observaciones: '' },
    validators: { onChange: registrarEntregaSchema, onBlur: registrarEntregaSchema },
    onSubmit: ({ value }) => {
      setError(null)
      registrar.mutate(
        { codigo: finalista.codigo, pedido: aEntregaRequest(value) },
        {
          onSuccess: (actualizado) => {
            onRegistrada?.(actualizado)
            toastSuccess('Entrega registrada', `${finalista.nombres} ${finalista.apellidos}`)
            form.setFieldValue('observaciones', '')
          },
          onError: (e) => setError(mapearErrorFinalistas(e).mensaje),
        },
      )
    },
  })

  return (
    <form
      className="flex flex-col gap-4"
      noValidate
      onSubmit={(e) => {
        e.preventDefault()
        e.stopPropagation()
        void form.handleSubmit()
      }}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-col">
          <span className="text-sm font-medium">{`${finalista.nombres} ${finalista.apellidos}`}</span>
          <span className="text-xs text-muted-foreground">CI {finalista.ci}</span>
        </div>
        <EntregaBadge recibida={yaRecibida} />
      </div>

      {finalista.plazoEntrega && (
        <p className="text-xs text-muted-foreground">Plazo de entrega: {formatearFechaDia(finalista.plazoEntrega)}</p>
      )}
      {yaRecibida && finalista.documentacionRecibidaEn && (
        <p className="text-xs text-muted-foreground" role="note">
          Recibida el {formatearFechaDia(finalista.documentacionRecibidaEn)}. Si cambias la fecha, la anterior queda
          en la bitácora.
        </p>
      )}

      {error && <AlertaError>{error}</AlertaError>}

      <form.Field name="fechaRecepcion">
        {(field) => (
          <div className="flex flex-col gap-1">
            <label htmlFor="entrega-fecha" className="text-sm font-medium">
              Fecha de recepción
            </label>
            <Input
              id="entrega-fecha"
              type="date"
              max={hoyIso()}
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

      <form.Field name="observaciones">
        {(field) => (
          <div className="flex flex-col gap-1">
            <label htmlFor="entrega-observaciones" className="text-sm font-medium">
              Observaciones (opcional)
            </label>
            <Textarea
              id="entrega-observaciones"
              rows={3}
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

      <Button type="submit" className="w-fit" disabled={registrar.isPending}>
        {registrar.isPending && <Loader2Icon className="animate-spin" />}
        {yaRecibida ? 'Corregir la fecha' : 'Marcar como recibida'}
      </Button>
    </form>
  )
}

/**
 * Registrar la entrega de la documentación física del finalista elegido (CU21, flujo B): fecha de recepción
 * (hoy por defecto, nunca futura) y observaciones opcionales. Si ya había una fecha, la muestra y avisa que
 * corregirla deja la anterior en la bitácora (A5).
 */
export function RegistrarEntregaPanel({ finalista, onRegistrada }: RegistrarEntregaPanelProps) {
  return (
    <section aria-labelledby="entrega-titulo" className="rounded-2xl border border-border bg-card p-4">
      <h2 id="entrega-titulo" className="mb-3 font-heading text-base font-semibold">
        Registrar entrega física
      </h2>
      {finalista ? (
        // La clave reinicia el formulario al cambiar de finalista o cuando se registra/corrige la fecha.
        <FormularioEntrega
          key={`${finalista.codigo}-${finalista.documentacionRecibidaEn ?? ''}`}
          finalista={finalista}
          onRegistrada={onRegistrada}
        />
      ) : (
        <p className="text-sm text-muted-foreground">
          Elige un finalista de la tabla con «Registrar entrega» para marcar su documentación como recibida.
        </p>
      )}
    </section>
  )
}
