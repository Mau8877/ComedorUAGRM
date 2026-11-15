import { useRef, useState } from 'react'
import { UserPlusIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess, toastWarning } from '@/components/ui/toast'
import { AlertaError } from '@/features/auth'

import { mapearErrorBecas, useDarDeAlta } from '../api'

const ID_FORMULARIO = 'alta-padron'

interface AltaPadronDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  convocatoria: { codigo: string; nombre: string }
  /** Cuántos adjudicados todavía no figuran en el padrón. */
  pendientes: number
}

/**
 * Ventana «Dar de alta» (CU23, flujo D): confirma que los adjudicados pendientes pasan a ser becados vigentes. Repetir
 * el alta no duplica nada: si el backend dice que ya no hay pendientes (ERR_BEC_04), avisa y cierra.
 */
export function AltaPadronDialog({ open, onOpenChange, convocatoria, pendientes }: AltaPadronDialogProps) {
  const alta = useDarDeAlta()
  const [error, setError] = useState<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)

  const confirmar = () => {
    if (enviando.current) return
    enviando.current = true
    setError(null)
    alta.mutate(convocatoria.codigo, {
      onSuccess: ({ creadas }) => {
        toastSuccess('Adjudicados dados de alta', `${creadas} ${creadas === 1 ? 'becado' : 'becados'} en el padrón.`)
        onOpenChange(false)
      },
      onError: (e) => {
        const mapeado = mapearErrorBecas(e)
        if (mapeado.refrescar) {
          toastWarning('Sin cambios', mapeado.mensaje)
          onOpenChange(false)
          return
        }
        setError(mapeado.mensaje)
      },
      onSettled: () => {
        enviando.current = false
      },
    })
  }

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      icon={UserPlusIcon}
      title="Dar de alta a los adjudicados"
      subtitle={convocatoria.nombre}
      formId={ID_FORMULARIO}
      isSubmitting={alta.isPending}
      confirmLabel={alta.isPending ? 'Dando de alta…' : 'Dar de alta'}
    >
      <form
        id={ID_FORMULARIO}
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          confirmar()
        }}
      >
        <p className="text-sm">
          <strong>{pendientes}</strong> {pendientes === 1 ? 'adjudicado todavía no figura' : 'adjudicados todavía no figuran'}{' '}
          en el padrón. Al confirmar pasan a ser becados vigentes.
        </p>
        {error && <AlertaError>{error}</AlertaError>}
      </form>
    </ModalLayout>
  )
}
