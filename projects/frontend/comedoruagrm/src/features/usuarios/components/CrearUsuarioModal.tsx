import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { CheckIcon, CopyIcon, KeyRoundIcon, UserPlusIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { ModalDetailLayout, ModalLayout } from '@/components/ui/modal-layout'
import { validacionFormulario } from '@/utils/validacionFormulario'

import { mapearErrorUsuarios, useCrearUsuario } from '../api'
import type { ErrorUsuarios } from '../api'
import type { RolPersonal } from '../roles'
import { TITULOS_NUEVA_CUENTA } from '../roles'
import { crearUsuarioPersonalSchema, datosCuentaPersonalSchema } from '../schemas'
import type { UsuarioCreado } from '../types'
import { AlertaUsuarios } from './AlertaUsuarios'
import { CampoUsuario } from './CampoUsuario'

const FORM_ID = 'crear-usuario-form'

interface CrearUsuarioModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Rol de la cuenta: lo define la vista desde la que se abre; no se pregunta en el formulario. */
  rol: RolPersonal
  /** Se llama una vez creada la cuenta (la lista ya se está refrescando). */
  onCreada?: (usuario: UsuarioCreado) => void
}

/**
 * Alta de una cuenta del personal (CU14, flujo B). Al terminar muestra la contraseña
 * temporal UNA sola vez: vive solo en el estado de este componente (no en localStorage,
 * ni en un store, ni en la URL, ni en la caché de TanStack Query) y se descarta cuando el
 * padre lo desmonta al cerrar.
 */
export function CrearUsuarioModal({ open, onOpenChange, rol, onCreada }: CrearUsuarioModalProps) {
  const crear = useCrearUsuario()
  const [resultado, setResultado] = useState<UsuarioCreado | null>(null)
  const [error, setError] = useState<ErrorUsuarios | null>(null)
  const [copiada, setCopiada] = useState(false)
  // Evita el doble envío (CA22) aunque el segundo clic llegue antes de que React pinte el estado "enviando".
  const enviando = useRef(false)

  const form = useForm({
    defaultValues: { nombres: '', apellidos: '', ci: '', correo: '' },
    ...validacionFormulario(datosCuentaPersonalSchema),
    onSubmit: ({ value }) => {
      if (enviando.current) return
      enviando.current = true
      setError(null)
      crear.mutate(crearUsuarioPersonalSchema.parse({ ...value, rol }), {
        onSuccess: (creada) => {
          setResultado(creada)
          onCreada?.(creada)
        },
        onError: (e) => setError(mapearErrorUsuarios(e)),
        onSettled: () => {
          enviando.current = false
        },
      })
    },
  })

  const copiar = async () => {
    if (!resultado) return
    try {
      await navigator.clipboard.writeText(resultado.contrasenaTemporal)
      setCopiada(true)
    } catch {
      // Sin acceso al portapapeles: la persona puede seleccionar y copiar el texto a mano.
      setCopiada(false)
    }
  }

  if (resultado) {
    return (
      <ModalDetailLayout
        open={open}
        onOpenChange={onOpenChange}
        title="Cuenta creada"
        subtitle={`${resultado.nombres} ${resultado.apellidos}`}
        icon={KeyRoundIcon}
        closeLabel="Entendido, ya la anoté"
        dismissible={false}
      >
        <div className="flex flex-col gap-4">
          <p className="text-sm text-foreground">
            Entrega esta contraseña temporal a la persona para que pueda iniciar sesión con <strong>{resultado.correo}</strong>.
          </p>
          <div className="flex items-center justify-between gap-3 rounded-md border border-border bg-muted p-3">
            <code data-testid="contrasena-temporal" className="font-mono text-base break-all text-foreground">
              {resultado.contrasenaTemporal}
            </code>
            <Button type="button" variant="outline" size="sm" onClick={() => void copiar()}>
              {copiada ? <CheckIcon data-icon="inline-start" /> : <CopyIcon data-icon="inline-start" />}
              {copiada ? 'Copiada' : 'Copiar'}
            </Button>
          </div>
          <p role="note" className="rounded-md border border-warning/40 bg-warning/10 p-3 text-sm text-warning">
            Esta contraseña no se volverá a mostrar. Si la pierdes, no se puede recuperar desde aquí.
          </p>
        </div>
      </ModalDetailLayout>
    )
  }

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      title={TITULOS_NUEVA_CUENTA[rol]}
      subtitle="Crea la cuenta de un funcionario; el sistema genera su contraseña temporal"
      icon={UserPlusIcon}
      formId={FORM_ID}
      isSubmitting={crear.isPending}
      confirmLabel="Crear cuenta"
    >
      <form
        id={FORM_ID}
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          void form.handleSubmit()
        }}
      >
        {error && !error.campo && (
          <AlertaUsuarios error={error} onReintentar={() => void form.handleSubmit()} disabled={crear.isPending} />
        )}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <form.Field name="nombres">
            {(field) => (
              <CampoUsuario
                id={field.name}
                label="Nombres" required
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={field.handleChange}
                error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
              />
            )}
          </form.Field>
          <form.Field name="apellidos">
            {(field) => (
              <CampoUsuario
                id={field.name}
                label="Apellidos" required
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={field.handleChange}
                error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
              />
            )}
          </form.Field>
        </div>

        <form.Field name="ci">
          {(field) => (
            <CampoUsuario
              id={field.name}
              label="CI" required
              value={field.state.value}
              onBlur={field.handleBlur}
              onChange={(valor) => {
                if (error?.campo === 'ci') setError(null)
                field.handleChange(valor)
              }}
              error={error?.campo === 'ci' ? error.mensaje : field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
            />
          )}
        </form.Field>

        <form.Field name="correo">
          {(field) => (
            <CampoUsuario
              id={field.name}
              label="Correo electrónico" required
              type="email"
              autoComplete="off"
              value={field.state.value}
              onBlur={field.handleBlur}
              onChange={(valor) => {
                if (error?.campo === 'correo') setError(null)
                field.handleChange(valor)
              }}
              error={
                error?.campo === 'correo' ? error.mensaje : field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined
              }
            />
          )}
        </form.Field>
      </form>
    </ModalLayout>
  )
}
