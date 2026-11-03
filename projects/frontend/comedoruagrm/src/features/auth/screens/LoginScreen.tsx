import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { Link, useNavigate } from '@tanstack/react-router'
import { Loader2Icon } from 'lucide-react'

import { PublicHeader } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

import { mapearError, type ErrorMapeado } from '../api/mapearError'
import { useLogin } from '../api/useLogin'
import { AlertaError } from '../components/AlertaError'
import { CampoFormulario } from '../components/CampoFormulario'
import { rutaInicioPorRol } from '../rutaInicioPorRol'
import { loginSchema } from '../schemas'

export function LoginScreen() {
  const navigate = useNavigate()
  const login = useLogin()
  const [error, setError] = useState<ErrorMapeado | null>(null)

  const form = useForm({
    defaultValues: { correo: '', contrasena: '' },
    validators: { onChange: loginSchema, onBlur: loginSchema },
    onSubmit: ({ value }) => {
      setError(null)
      login.mutate(value, {
        onSuccess: (sesion) => void navigate({ to: rutaInicioPorRol(sesion.usuario.rol) }),
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
            <CardTitle className="font-heading text-2xl">Iniciar sesión</CardTitle>
            <CardDescription>Ingresa con tu correo y tu contraseña.</CardDescription>
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
              {/* Alerta sobre el formulario (no por campo): no revela cuál dato falló. */}
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
                {(field) => <CampoFormulario field={field} label="Correo electrónico" type="email" autoComplete="email" />}
              </form.Field>
              <form.Field name="contrasena">
                {(field) => (
                  <CampoFormulario field={field} label="Contraseña" type="password" autoComplete="current-password" />
                )}
              </form.Field>

              <Button type="submit" disabled={login.isPending}>
                {login.isPending && <Loader2Icon className="animate-spin" />}
                Iniciar sesión
              </Button>

              <p className="text-center text-sm text-muted-foreground">
                ¿No tienes cuenta?{' '}
                <Link to="/registro" className="font-medium text-primary underline-offset-4 hover:underline">
                  Crear cuenta
                </Link>
              </p>
            </form>
          </CardContent>
        </Card>
      </main>
    </div>
  )
}
