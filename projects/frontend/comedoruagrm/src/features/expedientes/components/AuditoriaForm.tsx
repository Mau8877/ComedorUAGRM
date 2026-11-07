import { useRef, useState } from 'react'
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
import { Label } from '@/components/ui/label'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Textarea } from '@/components/ui/textarea'
import { toastSuccess } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'
import { ApiError } from '@/store'

import { nuevaClaveIdempotencia } from '../api/claveIdempotencia'
import { mapearErrorExpedientes } from '../api/mapearErrorExpedientes'
import { useRegistrarAuditoria } from '../api/useExpediente'
import {
  aAuditoriaRequest,
  auditoriaSchema,
  ETIQUETA_RESULTADO,
  MAX_OBSERVACIONES,
  RESULTADOS_AUDITORIA,
} from '../schemas'
import type { AuditoriaFormValues, AuditoriaValues } from '../schemas'
import type { ResultadoAuditoria, ResultadoRegistroAuditoria } from '../types'

interface AuditoriaFormProps {
  /** Código del finalista que se audita. */
  finalista: string
  /** Si hay un motivo, el formulario queda deshabilitado y lo muestra. */
  bloqueo?: string | null
  onRegistrada?: (resultado: ResultadoRegistroAuditoria) => void
  /** El backend dijo que el finalista ya no existe (ERR_FIN_01): la pantalla vuelve a la lista. */
  onFinalistaInexistente?: () => void
}

const VALORES_INICIALES: AuditoriaFormValues = { resultado: '', puntajeAcademico: '', observaciones: '' }

/**
 * Formulario de auditoría del expediente (CU07, pasos 4–7): resultado, puntaje académico (solo con
 * «Validado») y observaciones (obligatorias si no es «Validado»). Pide confirmación antes de registrar
 * y no se puede enviar dos veces seguidas.
 */
export function AuditoriaForm({ finalista, bloqueo, onRegistrada, onFinalistaInexistente }: AuditoriaFormProps) {
  const registrar = useRegistrarAuditoria()
  const [pendiente, setPendiente] = useState<AuditoriaValues | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [bloqueoServidor, setBloqueoServidor] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)
  // Clave del intento en curso (header `Idempotency-Key`). Se reutiliza mientras se reintente exactamente lo
  // mismo, para que una respuesta perdida o un doble envío no dupliquen la auditoría; con otro contenido, o
  // tras un éxito, se genera una nueva.
  const intento = useRef<{ huella: string; clave: string } | null>(null)

  const motivo = bloqueo ?? bloqueoServidor
  const deshabilitado = motivo !== null && motivo !== undefined

  const form = useForm({
    defaultValues: VALORES_INICIALES,
    validators: { onChange: auditoriaSchema, onBlur: auditoriaSchema },
    onSubmit: ({ value }) => {
      const analizado = auditoriaSchema.safeParse(value)
      if (analizado.success) setPendiente(analizado.data)
    },
  })

  const confirmar = () => {
    if (!pendiente || enviando.current) return
    enviando.current = true
    setError(null)
    const pedido = aAuditoriaRequest(pendiente)
    const huella = JSON.stringify(pedido)
    if (intento.current?.huella !== huella) intento.current = { huella, clave: nuevaClaveIdempotencia() }
    registrar.mutate(
      { codigo: finalista, pedido, claveIdempotencia: intento.current.clave },
      {
        onSuccess: (resultado) => {
          intento.current = null
          toastSuccess(
            resultado.creada ? 'Auditoría registrada' : 'La auditoría ya estaba registrada',
            ETIQUETA_RESULTADO[resultado.auditoria.resultado],
          )
          form.reset(VALORES_INICIALES)
          onRegistrada?.(resultado)
        },
        onError: (e) => {
          const mapeado = mapearErrorExpedientes(e)
          if (mapeado.finalistaInexistente) {
            onFinalistaInexistente?.()
            return
          }
          // La clave ya se usó con otro contenido: no sirve reintentar con ella.
          if (e instanceof ApiError && e.code === 'ERR_EXP_06') intento.current = null
          setError(mapeado.mensaje)
          if (e instanceof ApiError && (e.code === 'ERR_EXP_01' || e.code === 'ERR_EXP_02')) {
            setBloqueoServidor(mapeado.mensaje)
          }
        },
        onSettled: () => {
          enviando.current = false
          setPendiente(null)
        },
      },
    )
  }

  return (
    <section aria-labelledby="auditoria-titulo" className="rounded-2xl border border-border bg-card p-4">
      <h2 id="auditoria-titulo" className="mb-3 font-heading text-base font-semibold">
        Registrar auditoría
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
        {deshabilitado && (
          <p role="note" className="rounded-md border border-warning/30 bg-warning/10 p-2 text-sm text-warning">
            {motivo}
          </p>
        )}
        {error && !deshabilitado && <AlertaError>{error}</AlertaError>}

        <form.Field name="resultado">
          {(field) => (
            <div className="flex flex-col gap-1">
              <span id="auditoria-resultado" className="text-sm font-medium">
                Resultado
              </span>
              <RadioGroup
                aria-labelledby="auditoria-resultado"
                className="flex flex-wrap gap-4"
                disabled={deshabilitado}
                value={field.state.value}
                onValueChange={(valor) => field.handleChange(valor as ResultadoAuditoria)}
              >
                {RESULTADOS_AUDITORIA.map((r) => (
                  <Label key={r} htmlFor={`auditoria-resultado-${r}`} className="gap-2 font-normal">
                    <RadioGroupItem
                      id={`auditoria-resultado-${r}`}
                      value={r}
                      aria-label={ETIQUETA_RESULTADO[r]}
                      onBlur={field.handleBlur}
                    />
                    {ETIQUETA_RESULTADO[r]}
                  </Label>
                ))}
              </RadioGroup>
              {field.state.meta.isTouched && field.state.meta.errors[0] && (
                <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
              )}
            </div>
          )}
        </form.Field>

        <form.Subscribe selector={(estado) => estado.values.resultado}>
          {(resultado) =>
            resultado === 'VALIDADO' && (
              <form.Field name="puntajeAcademico">
                {(field) => (
                  <div className="flex flex-col gap-1">
                    <label htmlFor="auditoria-puntaje" className="text-sm font-medium">
                      Puntaje académico (0 a 30)
                    </label>
                    <Input
                      id="auditoria-puntaje"
                      inputMode="decimal"
                      disabled={deshabilitado}
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
            )
          }
        </form.Subscribe>

        <form.Field name="observaciones">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor="auditoria-observaciones" className="text-sm font-medium">
                Observaciones
              </label>
              <Textarea
                id="auditoria-observaciones"
                rows={4}
                disabled={deshabilitado}
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
                  {field.state.value.length}/{MAX_OBSERVACIONES}
                </span>
              </div>
            </div>
          )}
        </form.Field>

        <Button type="submit" className="w-fit" disabled={deshabilitado || registrar.isPending}>
          {registrar.isPending && <Loader2Icon className="animate-spin" />}
          Registrar auditoría
        </Button>
      </form>

      <AlertDialog open={pendiente !== null} onOpenChange={(abierto) => !abierto && setPendiente(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Registrar la auditoría?</AlertDialogTitle>
            <AlertDialogDescription>
              El expediente quedará como «{pendiente ? ETIQUETA_RESULTADO[pendiente.resultado] : ''}» y se avisará al
              postulante. La auditoría queda en el historial y no se puede borrar.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <Button variant="outline" onClick={() => setPendiente(null)}>
              Volver
            </Button>
            <Button onClick={confirmar} disabled={registrar.isPending}>
              Confirmar
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </section>
  )
}
