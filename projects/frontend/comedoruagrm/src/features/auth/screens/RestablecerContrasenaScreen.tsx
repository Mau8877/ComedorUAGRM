import { useEffect, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { Link, useNavigate } from '@tanstack/react-router'
import { Loader2Icon } from 'lucide-react'

import { PublicHeader } from '@/components/layout'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { toastSuccess } from '@/components/ui/toast'

import { mapearError, type ErrorMapeado } from '../api/mapearError'
import { useRestablecerContrasena } from '../api/useRestablecerContrasena'
import { AlertaError } from '../components/AlertaError'
import { CampoFormulario } from '../components/CampoFormulario'
import { aRestablecerRequest, restablecerContrasenaSchema } from '../schemas'

interface RestablecerContrasenaScreenProps {
  /** Token del enlace del correo (`?token=`); sin él, el enlace no sirve. */
  tokenInicial?: string
}

// * El token llega en la URL, pero se guarda solo en el estado de esta pantalla y se QUITA de la barra de
// * direcciones (replace): así no queda en el historial ni sale por el `Referer`. No se guarda en
// * localStorage, en un store ni en la caché de TanStack Query, y nada lo escribe en la consola.
export function RestablecerContrasenaScreen({ tokenInicial }: RestablecerContrasenaScreenProps) {
  const navigate = useNavigate()
  const restablecer = useRestablecerContrasena()
  const [token] = useState(tokenInicial)
  const [enlaceInvalido, setEnlaceInvalido] = useState(!tokenInicial)
  const [error, setError] = useState<ErrorMapeado | null>(null)

  useEffect(() => {
    if (tokenInicial) {
      void navigate({ to: '/restablecer-contrasena', search: {}, replace: true })
    }
  }, [tokenInicial, navigate])

  const form = useForm({
    defaultValues: { contrasena: '', confirmarContrasena: '' },
    validators: { onChange: restablecerContrasenaSchema, onBlur: restablecerContrasenaSchema },
    onSubmit: ({ value }) => {
      if (!token) return
      setError(null)
      restablecer.mutate(aRestablecerRequest(value, token), {
        onSuccess: () => {
          toastSuccess('Contraseña actualizada', 'Inicia sesión con tu nueva contraseña.')
          void navigate({ to: '/login' })
        },
        onError: (e) => {
          const mapeado = mapearError(e)
          if (mapeado.enlaceInvalido) {
            setEnlaceInvalido(true)
          } else {
            setError(mapeado)
          }
        },
      })
    },
  })

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <PublicHeader appName="ComedorU" />

      <main className="flex flex-1 items-center justify-center px-4 py-10">
        <Card className="w-full max-w-md">
          {enlaceInvalido ? (
            <>
              <CardHeader>
                <CardTitle className="font-heading text-2xl">El enlace ya no sirve</CardTitle>
                <CardDescription>
                  Venció, ya se usó o pediste uno más reciente. Solicita un enlace nuevo para continuar.
                </CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-3">
                <Link to="/recuperar-contrasena" className={buttonVariants()}>
                  Solicitar un enlace nuevo
                </Link>
                <p className="text-center text-sm">
                  <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                    Volver a iniciar sesión
                  </Link>
                </p>
              </CardContent>
            </>
          ) : (
            <>
              <CardHeader>
                <CardTitle className="font-heading text-2xl">Nueva contraseña</CardTitle>
                <CardDescription>Define tu contraseña nueva. Debe tener de 6 a 72 caracteres.</CardDescription>
              </CardHeader>
              <CardContent>
                <form
                  className="flex flex-col gap-4"
                  noValidate
                  onSubmit={(e) => {
                    e.preventDefault()
                    e.stopPropagation()
                    void form.handleSubmit()
                  }}
                >
                  {error && (
                    <AlertaError>
                      <span>{error.mensaje}</span>
                      {error.red && (
                        <button type="button" className="w-fit underline" onClick={() => void form.handleSubmit()}>
                          Reintentar
                        </button>
                      )}
                    </AlertaError>
                  )}

                  <form.Field name="contrasena">
                    {(field) => (
                      <CampoFormulario field={field} label="Nueva contraseña" type="password" autoComplete="new-password" />
                    )}
                  </form.Field>
                  <form.Field name="confirmarContrasena">
                    {(field) => (
                      <CampoFormulario
                        field={field}
                        label="Confirmar contraseña"
                        type="password"
                        autoComplete="new-password"
                      />
                    )}
                  </form.Field>

                  <Button type="submit" disabled={restablecer.isPending}>
                    {restablecer.isPending && <Loader2Icon className="animate-spin" />}
                    Guardar contraseña
                  </Button>
                </form>
              </CardContent>
            </>
          )}
        </Card>
      </main>
    </div>
  )
}
