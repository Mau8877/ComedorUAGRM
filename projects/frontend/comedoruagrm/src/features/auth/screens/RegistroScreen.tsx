import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { Link, useNavigate } from '@tanstack/react-router'
import { Loader2Icon } from 'lucide-react'

import { PublicHeader } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

import { mapearError, type ErrorMapeado } from '../api/mapearError'
import { useCarreras } from '../api/useCarreras'
import { useRegistro } from '../api/useRegistro'
import { AlertaError } from '../components/AlertaError'
import { CampoFormulario } from '../components/CampoFormulario'
import { rutaInicioPorRol } from '../rutaInicioPorRol'
import { registroSchema } from '../schemas'

type CampoServidor = NonNullable<ErrorMapeado['campo']>

// * No hay selector de rol ni forma de crear cuentas del personal (CA16): el
// * backend crea siempre un Postulante, y las cuentas del personal las crea el
// * Administrador DUBSS en el CU14.
export function RegistroScreen() {
  const navigate = useNavigate()
  const registro = useRegistro()
  const carreras = useCarreras()
  const [errorGeneral, setErrorGeneral] = useState<ErrorMapeado | null>(null)
  const [erroresServidor, setErroresServidor] = useState<Partial<Record<CampoServidor, string>>>({})

  const limpiarErrorServidor = (campo: CampoServidor) => () =>
    setErroresServidor((actuales) => (actuales[campo] ? { ...actuales, [campo]: undefined } : actuales))

  const form = useForm({
    defaultValues: {
      nombres: '',
      apellidos: '',
      ci: '',
      registroUniversitario: '',
      carreraCodigo: '',
      telefono: '',
      correo: '',
      contrasena: '',
      confirmarContrasena: '',
    },
    validators: { onChange: registroSchema, onBlur: registroSchema },
    onSubmit: ({ value }) => {
      setErrorGeneral(null)
      setErroresServidor({})
      registro.mutate(value, {
        onSuccess: ({ sesion }) => void navigate({ to: rutaInicioPorRol(sesion.usuario.rol) }),
        onError: (e) => {
          const mapeado = mapearError(e)
          if (mapeado.campo) {
            // El conflicto se marca en su campo (A1) y el formulario queda editable para corregirlo.
            setErroresServidor({ [mapeado.campo]: mapeado.mensaje })
          } else {
            setErrorGeneral(mapeado)
          }
        },
      })
    },
  })

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <PublicHeader appName="ComedorU" />

      <main className="flex flex-1 items-center justify-center px-4 py-10">
        <Card className="w-full max-w-2xl">
          <CardHeader>
            <CardTitle className="font-heading text-2xl">Crear cuenta</CardTitle>
            <CardDescription>Regístrate como postulante a la Beca Comedor.</CardDescription>
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

              <div className="grid gap-4 sm:grid-cols-2">
                <form.Field name="nombres">
                  {(field) => <CampoFormulario field={field} label="Nombres" autoComplete="given-name" />}
                </form.Field>
                <form.Field name="apellidos">
                  {(field) => <CampoFormulario field={field} label="Apellidos" autoComplete="family-name" />}
                </form.Field>
                <form.Field name="ci">
                  {(field) => (
                    <CampoFormulario
                      field={field}
                      label="CI"
                      errorExterno={erroresServidor.ci}
                      onEdit={limpiarErrorServidor('ci')}
                    />
                  )}
                </form.Field>
                <form.Field name="registroUniversitario">
                  {(field) => (
                    <CampoFormulario
                      field={field}
                      label="Registro universitario"
                      inputMode="numeric"
                      errorExterno={erroresServidor.registroUniversitario}
                      onEdit={limpiarErrorServidor('registroUniversitario')}
                    />
                  )}
                </form.Field>

                <form.Field name="carreraCodigo">
                  {(field) => {
                    const error =
                      erroresServidor.carreraCodigo ??
                      (field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined)
                    return (
                      <div className="flex flex-col gap-1">
                        <span id="carrera-label" className="text-sm font-medium text-foreground">
                          Carrera
                        </span>
                        <Select
                          value={field.state.value || null}
                          items={(carreras.data ?? []).map((c) => ({ value: c.codigo, label: c.nombre }))}
                          onValueChange={(valor) => {
                            limpiarErrorServidor('carreraCodigo')()
                            field.handleChange(valor ?? '')
                            field.handleBlur()
                          }}
                          disabled={carreras.isPending || carreras.isError}
                        >
                          <SelectTrigger className="w-full" aria-labelledby="carrera-label" aria-invalid={error ? true : undefined}>
                            <SelectValue
                              placeholder={carreras.isPending ? 'Cargando carreras…' : 'Selecciona tu carrera'}
                            />
                          </SelectTrigger>
                          <SelectContent>
                            {(carreras.data ?? []).map((carrera) => (
                              <SelectItem key={carrera.codigo} value={carrera.codigo}>
                                {carrera.nombre}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        {carreras.isError && (
                          <span className="text-xs text-destructive">
                            No se pudieron cargar las carreras.{' '}
                            <button type="button" className="underline" onClick={() => void carreras.refetch()}>
                              Reintentar
                            </button>
                          </span>
                        )}
                        {error && <span className="text-xs text-destructive">{error}</span>}
                      </div>
                    )
                  }}
                </form.Field>
                <form.Field name="telefono">
                  {(field) => <CampoFormulario field={field} label="Teléfono" type="tel" autoComplete="tel" inputMode="tel" />}
                </form.Field>

                <div className="sm:col-span-2">
                  <form.Field name="correo">
                    {(field) => (
                      <CampoFormulario
                        field={field}
                        label="Correo electrónico"
                        type="email"
                        autoComplete="email"
                        errorExterno={erroresServidor.correo}
                        onEdit={limpiarErrorServidor('correo')}
                      />
                    )}
                  </form.Field>
                </div>
                <form.Field name="contrasena">
                  {(field) => (
                    <CampoFormulario field={field} label="Contraseña" type="password" autoComplete="new-password" />
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
              </div>

              <Button type="submit" disabled={registro.isPending}>
                {registro.isPending && <Loader2Icon className="animate-spin" />}
                Crear cuenta
              </Button>

              <p className="text-center text-sm text-muted-foreground">
                ¿Ya tienes cuenta?{' '}
                <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                  Inicia sesión
                </Link>
              </p>
            </form>
          </CardContent>
        </Card>
      </main>
    </div>
  )
}
