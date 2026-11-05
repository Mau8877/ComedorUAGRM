import { useMemo, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { Loader2Icon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { toastSuccess } from '@/components/ui/toast'
import { AlertaError, CampoFormulario } from '@/features/auth'

import { mapearErrorPerfil, type ErrorPerfil } from '../api/mapearError'
import { useActualizarPerfil } from '../api/useActualizarPerfil'
import { aActualizarPerfilRequest, crearActualizarPerfilSchema } from '../schemas'
import type { Perfil } from '../types'

type CampoServidor = NonNullable<ErrorPerfil['campo']>

function valoresDe(perfil: Perfil) {
  return { correo: perfil.correo, telefono: perfil.postulante?.telefono ?? '', contrasenaActual: '' }
}

/**
 * Datos de contacto (CU15, flujo D): el correo y, si es Postulante, el teléfono. Cambiar el correo exige
 * la contraseña actual (A7); manda solo lo que cambió. El CI y los demás datos no son campos de este formulario.
 */
export function DatosContactoForm({ perfil }: { perfil: Perfil }) {
  const actualizar = useActualizarPerfil()
  const esPostulante = perfil.postulante !== null
  const [errorGeneral, setErrorGeneral] = useState<ErrorPerfil | null>(null)
  const [erroresServidor, setErroresServidor] = useState<Partial<Record<CampoServidor, string>>>({})

  const schema = useMemo(
    () => crearActualizarPerfilSchema({ correoActual: perfil.correo, esPostulante }),
    [perfil.correo, esPostulante],
  )
  const original = { correo: perfil.correo, telefono: perfil.postulante?.telefono }

  const limpiarErrorServidor = (campo: CampoServidor) => () =>
    setErroresServidor((actuales) => (actuales[campo] ? { ...actuales, [campo]: undefined } : actuales))

  const form = useForm({
    defaultValues: valoresDe(perfil),
    validators: { onChange: schema, onBlur: schema },
    onSubmit: ({ value }) => {
      const request = aActualizarPerfilRequest(value, original)
      if (Object.keys(request).length === 0) return
      setErrorGeneral(null)
      setErroresServidor({})
      actualizar.mutate(request, {
        onSuccess: (nuevo) => {
          // Los valores toman lo guardado y la contraseña actual se limpia.
          form.reset(valoresDe(nuevo))
          toastSuccess('Datos actualizados')
        },
        onError: (e) => {
          const mapeado = mapearErrorPerfil(e)
          if (mapeado.campo) {
            // El dato en conflicto se marca en su campo (A5, A6) y lo escrito se conserva.
            setErroresServidor({ [mapeado.campo]: mapeado.mensaje })
          } else {
            setErrorGeneral(mapeado)
          }
        },
      })
    },
  })

  return (
    <section aria-labelledby="datos-contacto" className="flex flex-col gap-4">
      <h2 id="datos-contacto" className="font-heading text-lg font-semibold">
        Datos de contacto
      </h2>
      <p className="text-sm text-muted-foreground">Las notificaciones se envían a este correo y teléfono.</p>

      <form
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          void form.handleSubmit()
        }}
      >
        {errorGeneral && (
          <AlertaError>
            <span>{errorGeneral.mensaje}</span>
            {errorGeneral.red && (
              <button type="button" className="w-fit underline" onClick={() => void form.handleSubmit()}>
                Reintentar
              </button>
            )}
          </AlertaError>
        )}

        <form.Field name="correo">
          {(field) => (
            <CampoFormulario
              field={field}
              id="contacto-correo"
              label="Correo electrónico"
              type="email"
              autoComplete="email"
              errorExterno={erroresServidor.correo}
              onEdit={limpiarErrorServidor('correo')}
            />
          )}
        </form.Field>

        {esPostulante && (
          <form.Field name="telefono">
            {(field) => (
              <CampoFormulario
                field={field}
                id="contacto-telefono"
                label="Teléfono"
                type="tel"
                inputMode="tel"
                autoComplete="tel"
              />
            )}
          </form.Field>
        )}

        {/* La contraseña actual aparece solo si el correo cambió (A7); si vuelve al original, desaparece. */}
        <form.Subscribe selector={(estado) => estado.values.correo}>
          {(correo) =>
            correo.trim().toLowerCase() !== perfil.correo.toLowerCase() && (
              <div className="flex flex-col gap-1">
                <form.Field name="contrasenaActual">
                  {(field) => (
                    <CampoFormulario
                      field={field}
                      id="contacto-contrasena-actual"
                      label="Contraseña actual"
                      type="password"
                      autoComplete="current-password"
                      errorExterno={erroresServidor.contrasenaActual}
                      onEdit={limpiarErrorServidor('contrasenaActual')}
                    />
                  )}
                </form.Field>
                <span className="text-xs text-muted-foreground">
                  Por seguridad, confirma tu contraseña para cambiar el correo.
                </span>
              </div>
            )
          }
        </form.Subscribe>

        <form.Subscribe selector={(estado) => estado.values}>
          {(valores) => {
            const sinCambios = Object.keys(aActualizarPerfilRequest(valores, original)).length === 0
            return (
              <Button type="submit" className="w-fit" disabled={actualizar.isPending || sinCambios}>
                {actualizar.isPending && <Loader2Icon className="animate-spin" />}
                Guardar cambios
              </Button>
            )
          }}
        </form.Subscribe>
      </form>
    </section>
  )
}
