import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { ArrowRightLeftIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { toastSuccess, toastWarning } from '@/components/ui/toast'

import { mapearErrorUsuarios, useCambiarRol, useRefrescarListas } from '../api'
import type { ErrorUsuarios } from '../api'
import { OPCIONES_ROL_PERSONAL } from '../roles'
import { cambiarRolSchema } from '../schemas'
import type { UsuarioPersonal } from '../types'
import { AlertaUsuarios } from './AlertaUsuarios'

const FORM_ID = 'cambiar-rol-form'

interface CambiarRolModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  usuario: UsuarioPersonal
}

/** Cambio de rol de una cuenta del personal (CU14, flujo D). Solo ofrece los otros roles del personal. */
export function CambiarRolModal({ open, onOpenChange, usuario }: CambiarRolModalProps) {
  const cambiar = useCambiarRol()
  const refrescar = useRefrescarListas()
  const [error, setError] = useState<ErrorUsuarios | null>(null)
  const enviando = useRef(false)

  // Ni el rol actual ni Postulante (que no está en la lista del personal).
  const opciones = OPCIONES_ROL_PERSONAL.filter((o) => o.value !== usuario.rol)

  const form = useForm({
    defaultValues: { rol: (opciones[0]?.value ?? '') as string },
    validators: { onChange: cambiarRolSchema, onBlur: cambiarRolSchema },
    onSubmit: ({ value }) => {
      if (enviando.current) return
      enviando.current = true
      setError(null)
      const { rol } = cambiarRolSchema.parse(value)
      cambiar.mutate(
        { codigo: usuario.codigo, rol },
        {
          onSuccess: (actualizada) => {
            const etiqueta = OPCIONES_ROL_PERSONAL.find((o) => o.value === actualizada.rol)?.label ?? actualizada.rol
            toastSuccess('Rol actualizado', `${usuario.nombres} ${usuario.apellidos} ahora es ${etiqueta}.`)
            onOpenChange(false)
          },
          onError: (e) => {
            const mapeado = mapearErrorUsuarios(e)
            if (mapeado.refrescar) {
              // ERR_US_12 (A9) y ERR_US_06 (A8): la lista estaba desactualizada.
              toastWarning('No se cambió el rol', mapeado.mensaje)
              void refrescar()
              onOpenChange(false)
              return
            }
            // ERR_US_08 / ERR_US_09 (A5): aviso sin cambiar nada; el modal sigue abierto.
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
      title="Cambiar rol"
      subtitle={`${usuario.nombres} ${usuario.apellidos}`}
      icon={ArrowRightLeftIcon}
      formId={FORM_ID}
      isSubmitting={cambiar.isPending}
      confirmLabel="Cambiar rol"
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
        {error && <AlertaUsuarios error={error} onReintentar={() => void form.handleSubmit()} disabled={cambiar.isPending} />}

        <form.Field name="rol">
          {(field) => (
            <div className="flex flex-col gap-1">
              <span id="cambiar-rol-label" className="text-sm font-medium text-foreground">
                Nuevo rol
              </span>
              <Select value={field.state.value} onValueChange={(valor) => field.handleChange(valor ?? '')}>
                <SelectTrigger className="w-full" aria-labelledby="cambiar-rol-label">
                  <SelectValue>
                    {(valor: string | null) => opciones.find((o) => o.value === valor)?.label ?? 'Selecciona un rol'}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {opciones.map((opcion) => (
                    <SelectItem key={opcion.value} value={opcion.value}>
                      {opcion.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {field.state.meta.isTouched && field.state.meta.errors[0] && (
                <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
              )}
            </div>
          )}
        </form.Field>

        <p className="rounded-md border border-info/40 bg-info/10 p-3 text-sm text-foreground">
          Al confirmar, la persona perderá el acceso del rol actual y <strong>tendrá que iniciar sesión de nuevo</strong> para usar el nuevo.
        </p>
      </form>
    </ModalLayout>
  )
}
