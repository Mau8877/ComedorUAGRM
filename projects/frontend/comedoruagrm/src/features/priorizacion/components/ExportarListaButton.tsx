import { useRef } from 'react'
import { FileDownIcon, Loader2Icon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { toastError, toastSuccess, toastWarning } from '@/components/ui/toast'
import { descargarArchivo } from '@/utils/descargarArchivo'

import { mapearErrorPriorizacion, useExportarLista } from '../api'
import type { FiltrosLista } from '../types'

interface ExportarListaButtonProps {
  /** Los mismos filtros de la lista en pantalla (convocatoria, carrera y búsqueda), nunca la página. */
  filtros: FiltrosLista
  /** Sin filas en la lista no hay nada que exportar. */
  deshabilitado?: boolean
}

/** Exporta la lista priorizada completa a PDF y la descarga (CU06, paso 7). */
export function ExportarListaButton({ filtros, deshabilitado = false }: ExportarListaButtonProps) {
  const exportar = useExportarLista()
  // Evita el doble clic aunque React todavía no haya repintado el botón como bloqueado.
  const enCurso = useRef(false)

  const alClic = () => {
    if (enCurso.current) return
    enCurso.current = true
    exportar.mutate(filtros, {
      onSuccess: ({ archivo, nombre }) => {
        descargarArchivo(archivo, nombre)
        toastSuccess('Lista exportada', `Se descargó ${nombre}.`)
      },
      onError: (error) => {
        const mapeado = mapearErrorPriorizacion(error)
        if (mapeado.listaVacia) toastWarning('Nada que exportar', mapeado.mensaje)
        else toastError('No se pudo exportar la lista', mapeado.mensaje)
      },
      onSettled: () => {
        enCurso.current = false
      },
    })
  }

  return (
    <Button size="sm" onClick={alClic} disabled={deshabilitado || exportar.isPending}>
      {exportar.isPending ? <Loader2Icon className="animate-spin" data-icon="inline-start" /> : <FileDownIcon data-icon="inline-start" />}
      {exportar.isPending ? 'Generando…' : 'Exportar a PDF'}
    </Button>
  )
}
