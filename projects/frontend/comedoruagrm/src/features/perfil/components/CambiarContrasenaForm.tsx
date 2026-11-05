import { useEffect, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { useLocation } from '@tanstack/react-router'
import { Loader2Icon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { toastSuccess } from '@/components/ui/toast'
import { AlertaError, CampoFormulario } from '@/features/auth'

import { mapearErrorPerfil, type ErrorPerfil } from '../api/mapearError'
import { useCambiarContrasena } from '../api/useCambiarContrasena'
import { aCambiarContrasenaRequest, cambiarContrasenaSchema } from '../schemas'

const ID_SECCION = 'cambiar-contrasena'

/**
 * Cambiar la contraseña desde el perfil (CU15, flujo E). La sesión actual SIGUE abierta: el backend solo
 * cierra las de los demás dispositivos, así que acá no se navega ni se limpia la caché. Una contraseña actual
 * incorrecta es un `400` (no cierra la sesión) y se marca en su campo. Nada se guarda fuera del formulario.
 */
export function CambiarContrasenaForm() {
  const cambiar = useCambiarContrasena()
  const hash = useLocation({ select: (ubicacion) => ubicacion.hash })
  const [errorGeneral, setErrorGeneral] = useState<ErrorPerfil | null>(null)
  const [errorActual, setErrorActual] = useState<string | undefined>()

  // «Cambiar contraseña» del menú del header lleva a /perfil#cambiar-contrasena: se hace scroll y foco acá.
  useEffect(() => {
    if (hash === ID_SECCION) {
      const seccion = document.getElementById(ID_SECCION)
      seccion?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
      document.getElementById('cambiar-contrasenaActual')?.focus()
    }
  }, [hash])

  const form = useForm({
    defaultValues: { contrasenaActual: '', contrasenaNueva: '', confirmarContrasena: '' },
    validators: { onChange: cambiarContrasenaSchema, onBlur: cambiarContrasenaSchema },
    onSubmit: ({ value }) => {
      setErrorGeneral(null)
      setErrorActual(undefined)
      cambiar.mutate(aCambiarContrasenaRequest(value), {
        onSuccess: () => {
          form.reset()
          toastSuccess('Contraseña actualizada', 'Cerramos tu sesión en los demás dispositivos.')
        },
        onError: (e) => {
          const mapeado = mapearErrorPerfil(e)
          if (mapeado.campo === 'contrasenaActual') {
            setErrorActual(mapeado.mensaje)
          } else {
            setErrorGeneral(mapeado)
          }
        },
      })
    },
  })

  return (
    <section id={ID_SECCION} aria-labelledby="cambiar-contrasena-titulo" className="flex flex-col gap-4">
      <h2 id="cambiar-contrasena-titulo" className="font-heading text-lg font-semibold">
        Cambiar contraseña
      </h2>
      <p className="text-sm text-muted-foreground">
        La contraseña nueva debe tener de 6 a 72 caracteres. Cerraremos tu sesión en los demás dispositivos.
      </p>

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

        <form.Field name="contrasenaActual">
          {(field) => (
            <CampoFormulario
              field={field}
              id="cambiar-contrasenaActual"
              label="Contraseña actual"
              type="password"
              autoComplete="current-password"
              errorExterno={errorActual}
              onEdit={() => setErrorActual(undefined)}
            />
          )}
        </form.Field>
        <form.Field name="contrasenaNueva">
          {(field) => (
            <CampoFormulario
              field={field}
              id="cambiar-contrasenaNueva"
              label="Contraseña nueva"
              type="password"
              autoComplete="new-password"
            />
          )}
        </form.Field>
        <form.Field name="confirmarContrasena">
          {(field) => (
            <CampoFormulario
              field={field}
              id="cambiar-confirmarContrasena"
              label="Confirmar contraseña nueva"
              type="password"
              autoComplete="new-password"
            />
          )}
        </form.Field>

        <Button type="submit" className="w-fit" disabled={cambiar.isPending}>
          {cambiar.isPending && <Loader2Icon className="animate-spin" />}
          Cambiar contraseña
        </Button>
      </form>
    </section>
  )
}
