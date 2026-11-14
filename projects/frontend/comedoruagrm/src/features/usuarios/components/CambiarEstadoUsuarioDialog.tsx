import { useRef, useState } from 'react'
import { UserCheckIcon } from 'lucide-react'

import { ModalDestructive } from '@/components/ui/modal-destructive'
import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess, toastWarning } from '@/components/ui/toast'
import { ApiError } from '@/store'

import { mapearErrorUsuarios, useDesactivarUsuario, useReactivarUsuario, useRefrescarListas } from '../api'
import type { ErrorUsuarios } from '../api'
import type { UsuarioPersonal } from '../types'
import { AlertaUsuarios } from './AlertaUsuarios'

const FORM_ID = 'cambiar-estado-form'

export type AccionEstado = 'desactivar' | 'reactivar'

interface CambiarEstadoUsuarioDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  usuario: UsuarioPersonal
  accion: AccionEstado
}

// ERR_US_08 y ERR_US_09 (A5): el backend rechaza y la cuenta queda como estaba.
const SIN_CAMBIOS = ['ERR_US_08', 'ERR_US_09']

/** Confirmación para desactivar (CU14, flujo E) o reactivar (flujo F) una cuenta del personal. */
export function CambiarEstadoUsuarioDialog({ open, onOpenChange, usuario, accion }: CambiarEstadoUsuarioDialogProps) {
  const desactivar = useDesactivarUsuario()
  const reactivar = useReactivarUsuario()
  const refrescar = useRefrescarListas()
  const [error, setError] = useState<ErrorUsuarios | null>(null)
  const enviando = useRef(false)

  const mutacion = accion === 'desactivar' ? desactivar : reactivar
  const nombre = `${usuario.nombres} ${usuario.apellidos}`

  const confirmar = () => {
    if (enviando.current) return
    enviando.current = true
    setError(null)
    mutacion.mutate(usuario.codigo, {
      onSuccess: () => {
        if (accion === 'desactivar') toastSuccess('Cuenta desactivada', `${nombre} ya no tiene acceso al sistema.`)
        else toastSuccess('Cuenta reactivada', `${nombre} vuelve a tener acceso con su rol anterior.`)
        onOpenChange(false)
      },
      onError: (e) => {
        const mapeado = mapearErrorUsuarios(e)
        if (mapeado.refrescar) {
          // A8 (la cuenta ya no existe) y A9 (ya estaba en ese estado): se avisa, se cierra y se actualiza la lista.
          toastWarning('Sin cambios', mapeado.mensaje)
          void refrescar()
          onOpenChange(false)
          return
        }
        if (e instanceof ApiError && SIN_CAMBIOS.includes(e.code)) {
          toastWarning('No se pudo completar la acción', mapeado.mensaje)
          onOpenChange(false)
          return
        }
        setError(mapeado)
      },
      onSettled: () => {
        enviando.current = false
      },
    })
  }

  if (accion === 'desactivar') {
    const base = `${nombre} perderá el acceso al sistema y se cerrarán sus sesiones abiertas.`
    return (
      <ModalDestructive
        open={open}
        onOpenChange={onOpenChange}
        title="¿Desactivar cuenta?"
        description={error ? `${base} ${error.mensaje}` : base}
        confirmLabel={error?.red ? 'Reintentar' : 'Desactivar'}
        isSubmitting={mutacion.isPending}
        onConfirm={confirmar}
      />
    )
  }

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      title="Reactivar cuenta"
      subtitle={nombre}
      icon={UserCheckIcon}
      size="sm"
      formId={FORM_ID}
      isSubmitting={mutacion.isPending}
      confirmLabel={error?.red ? 'Reintentar' : 'Reactivar'}
    >
      <form
        id={FORM_ID}
        className="flex flex-col gap-4"
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          confirmar()
        }}
      >
        {error && <AlertaUsuarios error={error} />}
        <p className="text-sm text-foreground">
          <strong>{nombre}</strong> recuperará el acceso al sistema con su rol anterior.
        </p>
      </form>
    </ModalLayout>
  )
}
