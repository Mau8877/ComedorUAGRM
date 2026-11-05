import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { Link } from '@tanstack/react-router'
import { Loader2Icon, MailCheckIcon } from 'lucide-react'

import { PublicHeader } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

import { mapearError, type ErrorMapeado } from '../api/mapearError'
import { useSolicitarRecuperacion } from '../api/useSolicitarRecuperacion'
import { AlertaError } from '../components/AlertaError'
import { CampoFormulario } from '../components/CampoFormulario'
import { recuperarContrasenaSchema } from '../schemas'

/**
 * Mensaje fijo tras pedir el enlace (CU15, A1/CA01/CA02/CA18): el mismo para cualquier respuesta,
 * sin repetir el correo ni distinguir si la cuenta existe, está desactivada o falló el correo.
 */
export const MENSAJE_RECUPERACION_ENVIADA =
  'Si la cuenta existe, enviamos un enlace al correo indicado. Vence en 60 minutos. Revisa también la carpeta de spam.'

export function RecuperarContrasenaScreen() {
  const solicitar = useSolicitarRecuperacion()
  const [enviado, setEnviado] = useState(false)
  const [error, setError] = useState<ErrorMapeado | null>(null)

  const form = useForm({
    defaultValues: { correo: '' },
    validators: { onChange: recuperarContrasenaSchema, onBlur: recuperarContrasenaSchema },
    onSubmit: ({ value }) => {
      setError(null)
      solicitar.mutate(value.correo, {
        onSuccess: () => setEnviado(true),
        onError: (e) => setError(mapearError(e)),
      })
    },
  })

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <PublicHeader appName="ComedorU" />

      <main className="flex flex-1 items-center justify-center px-4 py-10">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle className="font-heading text-2xl">Recuperar contraseña</CardTitle>
            <CardDescription>Te enviaremos un enlace para definir una contraseña nueva.</CardDescription>
          </CardHeader>
          <CardContent>
            {enviado ? (
              <div className="flex flex-col gap-4">
                <div role="status" className="flex items-start gap-2 rounded-2xl border border-border bg-muted px-3 py-3 text-sm">
                  <MailCheckIcon className="mt-0.5 size-4 shrink-0 text-primary" />
                  <p>{MENSAJE_RECUPERACION_ENVIADA}</p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    form.reset()
                    setEnviado(false)
                  }}
                >
                  Enviar de nuevo
                </Button>
                <p className="text-center text-sm">
                  <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                    Volver a iniciar sesión
                  </Link>
                </p>
              </div>
            ) : (
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

                <form.Field name="correo">
                  {(field) => (
                    <CampoFormulario field={field} label="Correo electrónico" type="email" autoComplete="email" />
                  )}
                </form.Field>

                <Button type="submit" disabled={solicitar.isPending}>
                  {solicitar.isPending && <Loader2Icon className="animate-spin" />}
                  Enviar enlace
                </Button>

                <p className="text-center text-sm">
                  <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                    Volver a iniciar sesión
                  </Link>
                </p>
              </form>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  )
}
