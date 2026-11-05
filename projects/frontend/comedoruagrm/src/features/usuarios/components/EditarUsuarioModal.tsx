import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { PencilIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess, toastWarning } from '@/components/ui/toast'

import { mapearErrorUsuarios, useEditarUsuario, useRefrescarListas } from '../api'
import type { ErrorUsuarios } from '../api'
import { editarUsuarioPersonalSchema } from '../schemas'
import type { EditarUsuarioPersonalRequest } from '../schemas'
import type { UsuarioPersonal } from '../types'
import { AlertaUsuarios } from './AlertaUsuarios'
import { CampoUsuario } from './CampoUsuario'

const FORM_ID = 'editar-usuario-form'

interface EditarUsuarioModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  usuario: UsuarioPersonal
}

/** Solo viajan los campos que cambiaron (PATCH). */
function soloCambios(original: UsuarioPersonal, nuevo: EditarUsuarioPersonalRequest): Partial<EditarUsuarioPersonalRequest> {
  const cambios: Partial<EditarUsuarioPersonalRequest> = {}
  if (nuevo.nombres !== original.nombres) cambios.nombres = nuevo.nombres
  if (nuevo.apellidos !== original.apellidos) cambios.apellidos = nuevo.apellidos
  if (nuevo.correo !== original.correo) cambios.correo = nuevo.correo
  return cambios
}

/** Edición de los datos de una cuenta del personal (CU14, flujo C). El CI se muestra pero no se puede cambiar (RF-10). */
export function EditarUsuarioModal({ open, onOpenChange, usuario }: EditarUsuarioModalProps) {
  const editar = useEditarUsuario()
  const refrescar = useRefrescarListas()
  const [error, setError] = useState<ErrorUsuarios | null>(null)
  const enviando = useRef(false)

  const form = useForm({
    defaultValues: { nombres: usuario.nombres, apellidos: usuario.apellidos, correo: usuario.correo },
    validators: { onChange: editarUsuarioPersonalSchema, onBlur: editarUsuarioPersonalSchema },
    onSubmit: ({ value }) => {
      if (enviando.current) return
      const cambios = soloCambios(usuario, editarUsuarioPersonalSchema.parse(value))
      if (Object.keys(cambios).length === 0) {
        onOpenChange(false)
        return
      }
      enviando.current = true
      setError(null)
      editar.mutate(
        { codigo: usuario.codigo, cambios },
        {
          onSuccess: () => {
            toastSuccess('Datos actualizados', `Se guardaron los cambios de ${usuario.nombres} ${usuario.apellidos}.`)
            onOpenChange(false)
          },
          onError: (e) => {
            const mapeado = mapearErrorUsuarios(e)
            if (mapeado.refrescar) {
              // A8: la cuenta ya no existe; se avisa, se cierra y se actualiza la lista.
              toastWarning('No se pudo editar', mapeado.mensaje)
              void refrescar()
              onOpenChange(false)
              return
            }
            setError(mapeado)
          },
          onSettled: () => {
            enviando.current = false
          },
        },
      )
    },
  })

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      title="Editar cuenta"
      subtitle={`${usuario.nombres} ${usuario.apellidos}`}
      icon={PencilIcon}
      formId={FORM_ID}
      isSubmitting={editar.isPending}
      confirmLabel="Guardar cambios"
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
          <AlertaUsuarios error={error} onReintentar={() => void form.handleSubmit()} disabled={editar.isPending} />
        )}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <form.Field name="nombres">
            {(field) => (
              <CampoUsuario
                id={field.name}
                label="Nombres"
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
                label="Apellidos"
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={field.handleChange}
                error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
              />
            )}
          </form.Field>
        </div>

        <CampoUsuario id="ci" label="CI" value={usuario.ci} onChange={() => {}} readOnly ayuda="El CI no se puede modificar." />

        <form.Field name="correo">
          {(field) => (
            <CampoUsuario
              id={field.name}
              label="Correo electrónico"
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
