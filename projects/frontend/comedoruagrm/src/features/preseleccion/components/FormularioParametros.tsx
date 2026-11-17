import { useEffect, useRef, useState } from 'react'
import { useForm, useStore } from '@tanstack/react-form'
import { SaveIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { toastSuccess, toastWarning } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'
import { cn } from '@/lib/utils'

import { mapearErrorPreseleccion, useGuardarModelo } from '../api'
import {
  aPayload,
  parametrosSchema,
  siguienteVersion,
  SUMA_PONDERACIONES,
  sumaEsValida,
  sumaPonderaciones,
  valoresIniciales,
  VARIABLES_MODELO,
} from '../schemas'
import type { ModeloPreseleccion } from '../types'

interface FormularioParametrosProps {
  /** La versión vigente; `null` si todavía no se guardó ninguna (la columna «Actual» queda vacía). */
  vigente: ModeloPreseleccion | null
  /** Avisa si hay cambios sin guardar: reprocesar espera a que se guarden (el «Borrador» del mockup). */
  onCambios: (hayCambios: boolean) => void
  /** Se llama cuando lo que muestra el formulario ya no es cierto (otra versión se guardó al mismo tiempo). */
  onDesactualizado: () => void
}

/**
 * «Pesos por variable (puntos máximos)» (CU26): la tabla con la ponderación vigente y la nueva de cada variable, la suma
 * con su indicador y «Guardar versión» / «Restablecer». Guardar crea la versión siguiente y pasa a ser la vigente de
 * inmediato; los valores sin guardar son solo un borrador en pantalla. El formulario se vuelve a montar (`key`) cada
 * vez que cambia la versión vigente.
 */
export function FormularioParametros({ vigente, onCambios, onDesactualizado }: FormularioParametrosProps) {
  const guardar = useGuardarModelo()
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)

  const iniciales = valoresIniciales(vigente?.parametros)
  const proxima = siguienteVersion(vigente?.version)
  const actualPorClave = new Map((vigente?.parametros ?? []).map((p) => [p.clave, p.ponderacion]))
  const sumaActual = (vigente?.parametros ?? []).reduce((total, p) => total + p.ponderacion, 0)

  const form = useForm({
    defaultValues: iniciales,
    validators: { onChange: parametrosSchema, onBlur: parametrosSchema },
    onSubmit: ({ value }) => {
      if (enviando.current) return
      if (!sumaEsValida(value)) return
      enviando.current = true
      setError(null)
      guardar.mutate(aPayload(value), {
        onSuccess: (modelo) => toastSuccess(`Versión ${modelo.version} guardada`, 'Ya es la versión vigente.'),
        onError: (e) => {
          const mapeado = mapearErrorPreseleccion(e)
          if (mapeado.refrescar) {
            toastWarning('Sin cambios', mapeado.mensaje)
            onDesactualizado()
            return
          }
          setError(mapeado.mensaje)
        },
        onSettled: () => {
          enviando.current = false
        },
      })
    },
  })

  const valores = useStore(form.store, (estado) => estado.values)
  const suma = sumaPonderaciones(valores)
  const valida = sumaEsValida(valores)
  const hayCambios = JSON.stringify(valores) !== JSON.stringify(iniciales)

  useEffect(() => {
    onCambios(hayCambios)
  }, [hayCambios, onCambios])

  return (
    <section aria-label="Pesos por variable" className="rounded-2xl border border-border bg-card p-4">
      <h2 className="font-heading text-lg">Pesos por variable (puntos máximos)</h2>
      <form
        noValidate
        className="mt-3 flex flex-col gap-4"
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          void form.handleSubmit()
        }}
      >
        <div className="overflow-x-auto rounded-xl border border-border">
          <table className="w-full text-sm">
            <thead className="bg-muted/60">
              <tr>
                <th scope="col" className="px-3 py-2 text-left font-medium">
                  Variable
                </th>
                <th scope="col" className="px-3 py-2 text-center font-medium">
                  {vigente ? `Actual (${vigente.version})` : 'Actual'}
                </th>
                <th scope="col" className="px-3 py-2 text-center font-medium">
                  {`Nuevo (${proxima})`}
                </th>
              </tr>
            </thead>
            <tbody>
              {VARIABLES_MODELO.map(({ clave, nombre }) => (
                <tr key={clave} className="border-t border-border">
                  <th scope="row" className="px-3 py-2 text-left font-normal">
                    {nombre}
                  </th>
                  <td className="px-3 py-2 text-center text-muted-foreground">{actualPorClave.get(clave) ?? '—'}</td>
                  <td className="px-3 py-2 text-center">
                    <form.Field name={clave}>
                      {(field) => (
                        <div className="mx-auto flex w-24 flex-col gap-1">
                          <Input
                            id={field.name}
                            name={field.name}
                            inputMode="decimal"
                            autoComplete="off"
                            aria-label={`Nuevo peso de ${nombre}`}
                            className="text-center"
                            value={field.state.value}
                            aria-invalid={field.state.meta.isTouched && field.state.meta.errors.length > 0 ? true : undefined}
                            onBlur={field.handleBlur}
                            onChange={(e) => {
                              if (error) setError(null)
                              field.handleChange(e.target.value)
                            }}
                          />
                          {field.state.meta.isTouched && field.state.meta.errors[0] && (
                            <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
                          )}
                        </div>
                      )}
                    </form.Field>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="border-t border-border bg-muted/40 font-semibold">
                <th scope="row" className="px-3 py-2 text-left">
                  Suma
                </th>
                <td className="px-3 py-2 text-center">{vigente ? sumaActual : '—'}</td>
                <td className="px-3 py-2 text-center">
                  <span aria-label="Suma de los nuevos pesos">{suma}</span>{' '}
                  <span
                    role="status"
                    className={cn(
                      'ml-1 inline-flex rounded-full px-2 py-0.5 text-xs font-medium',
                      valida ? 'bg-success/15 text-success' : 'bg-destructive/15 text-destructive',
                    )}
                  >
                    {valida ? '✓ válido' : `Deben sumar ${SUMA_PONDERACIONES}`}
                  </span>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>

        <form.Field name="descripcion">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor={field.name} className="text-sm font-medium text-foreground">
                Descripción de la versión (opcional)
              </label>
              <Input
                id={field.name}
                name={field.name}
                value={field.state.value}
                aria-invalid={field.state.meta.isTouched && field.state.meta.errors.length > 0 ? true : undefined}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              {field.state.meta.isTouched && field.state.meta.errors[0] && (
                <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
              )}
            </div>
          )}
        </form.Field>

        {/* * El puntaje mínimo para ser finalista es de cada convocatoria (CU13), no del modelo: aquí solo se informa. */}
        <div className="flex flex-col gap-1">
          <label htmlFor="puntaje-minimo" className="text-sm font-medium text-muted-foreground">
            Puntaje mínimo para ser finalista
          </label>
          <Input id="puntaje-minimo" disabled placeholder="Por definir con la DUBSS" />
          <span className="text-xs text-muted-foreground">Se configura en cada convocatoria.</span>
        </div>

        {error && <AlertaError>{error}</AlertaError>}

        <div className="flex flex-wrap gap-2">
          <Button type="submit" disabled={!valida || guardar.isPending}>
            <SaveIcon data-icon="inline-start" />
            {guardar.isPending ? 'Guardando…' : `Guardar versión ${proxima}`}
          </Button>
          <Button
            type="button"
            variant="outline"
            disabled={!hayCambios || guardar.isPending}
            onClick={() => {
              setError(null)
              form.reset()
            }}
          >
            Restablecer
          </Button>
        </div>
      </form>
    </section>
  )
}
