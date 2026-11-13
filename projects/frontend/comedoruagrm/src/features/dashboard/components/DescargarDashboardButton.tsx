import { useRef } from 'react'
import { FileDownIcon, Loader2Icon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { toastError, toastSuccess, toastWarning } from '@/components/ui/toast'
import { descargarArchivo } from '@/utils/descargarArchivo'

import { mapearErrorDashboard, useDescargarDashboard } from '../api'
import type { SeleccionDashboard } from '../types'

interface DescargarDashboardButtonProps {
  /** La selección que se está VIENDO (la aplicada), nunca la que se edita en los selectores. */
  seleccion: SeleccionDashboard
  /** Sin datos, o mientras cargan los indicadores, no hay nada que exportar. */
  deshabilitado?: boolean
  /** La gestión o la convocatoria ya no existe: la pantalla vuelve a la selección vigente. */
  onSeleccionInexistente?: () => void
}

/** Descarga el dashboard en PDF con la selección aplicada (CU12, paso 6). */
export function DescargarDashboardButton({
  seleccion,
  deshabilitado = false,
  onSeleccionInexistente,
}: DescargarDashboardButtonProps) {
  const descargar = useDescargarDashboard()
  // Evita el doble clic aunque React todavía no haya repintado el botón como bloqueado.
  const enCurso = useRef(false)

  const alClic = () => {
    if (enCurso.current) return
    enCurso.current = true
    descargar.mutate(seleccion, {
      onSuccess: ({ archivo, nombre }) => {
        descargarArchivo(archivo, nombre)
        toastSuccess('Dashboard descargado', `Se descargó ${nombre}.`)
      },
      onError: (error) => {
        const mapeado = mapearErrorDashboard(error)
        if (mapeado.sinDatos) toastWarning('Nada que exportar', mapeado.mensaje)
        else if (mapeado.seleccionInexistente) {
          toastError('No se pudo descargar el PDF', mapeado.mensaje)
          onSeleccionInexistente?.()
        } else toastError('No se pudo descargar el PDF', `${mapeado.mensaje}${mapeado.red ? '' : ' Inténtalo de nuevo.'}`)
      },
      onSettled: () => {
        enCurso.current = false
      },
    })
  }

  return (
    <Button size="sm" onClick={alClic} disabled={deshabilitado || descargar.isPending}>
      {descargar.isPending ? (
        <Loader2Icon className="animate-spin" data-icon="inline-start" />
      ) : (
        <FileDownIcon data-icon="inline-start" />
      )}
      {descargar.isPending ? 'Generando…' : 'Descargar PDF'}
    </Button>
  )
}
