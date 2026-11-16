import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { Link, useNavigate } from '@tanstack/react-router'
import { ClipboardCheckIcon, LockIcon, Loader2Icon, MailIcon, ShieldCheckIcon, UtensilsCrossedIcon } from 'lucide-react'

import { PublicHeader } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { ComedoruLogo } from '@/components/layout/ComedoruLogo'
import { validacionFormulario } from '@/utils/validacionFormulario'

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
    // * El error se muestra tras salir del campo o al enviar (ver CampoFormulario);
    // * onChange sigue validando para que desaparezca al corregir.
    ...validacionFormulario(loginSchema),
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

      <main className="flex flex-1 items-center justify-center px-4 py-8 sm:py-12">
        <div className="grid w-full max-w-4xl overflow-hidden rounded-3xl border border-border bg-card shadow-lg md:grid-cols-2">
          <aside className="hidden flex-col justify-between gap-10 bg-header p-10 text-header-foreground md:flex">
            <div className="flex flex-col gap-4">
              <ComedoruLogo className="size-14" />
              <h2 className="font-heading text-3xl leading-tight font-medium">Beca Comedor UAGRM</h2>
              <p className="text-sm text-header-foreground/80">
                Postula, da seguimiento a tu solicitud y consulta tu beca desde un solo lugar.
              </p>
            </div>
            <ul className="flex flex-col gap-4 text-sm">
              {[
                { icono: ClipboardCheckIcon, texto: 'Postulación 100 % en línea' },
                { icono: UtensilsCrossedIcon, texto: 'Consulta tu beca y tu asistencia' },
                { icono: ShieldCheckIcon, texto: 'Tu cuenta protegida con sesión segura' },
              ].map(({ icono: Icono, texto }) => (
                <li key={texto} className="flex items-center gap-3">
                  <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-header-foreground/15">
                    <Icono className="size-4" />
                  </span>
                  {texto}
                </li>
              ))}
            </ul>
          </aside>

          <section className="flex flex-col justify-center gap-6 p-6 sm:p-10">
            <div className="flex flex-col gap-1.5">
              <h1 className="font-heading text-2xl font-medium">Iniciar sesión</h1>
              <p className="text-sm text-muted-foreground">Ingresa con tu correo y tu contraseña.</p>
            </div>

            <form
              className="flex flex-col gap-5"
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
                {(field) => (
                  <CampoFormulario
                    field={field}
                    label="Correo electrónico" required
                    type="email"
                    autoComplete="email"
                    placeholder="nombre@correo.com"
                    icono={MailIcon}
                  />
                )}
              </form.Field>
              <form.Field name="contrasena">
                {(field) => (
                  <CampoFormulario
                    field={field}
                    label="Contraseña" required
                    type="password"
                    autoComplete="current-password"
                    icono={LockIcon}
                    alternarVisibilidad
                  />
                )}
              </form.Field>

              <Link
                to="/recuperar-contrasena"
                className="w-fit rounded text-sm font-medium text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/30"
              >
                ¿Olvidó su contraseña?
              </Link>

              <Button type="submit" size="lg" className="h-10 w-full" disabled={login.isPending}>
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
          </section>
        </div>
      </main>
    </div>
  )
}
