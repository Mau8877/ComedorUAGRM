import { useState } from 'react'
import { ExternalLinkIcon, FileTextIcon, PlusIcon, RefreshCwIcon, Trash2Icon } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ModalDestructive } from '@/components/ui/modal-destructive'
import { toastError, toastSuccess, toastWarning } from '@/components/ui/toast'

import { mapearErrorDocumentos, useAccesoDocumento, useDocumentos, useEliminarDocumento } from '../api'
import { formatearTamano } from '../schemas'
import type { ArchivoAdjunto, DocumentoRequerido } from '../types'
import { formatearFecha } from './formatos'
import { SubirDocumentoModal } from './SubirDocumentoModal'

export interface DocumentosPostulacionPanelProps {
  /** El código público de la postulación. */
  codigo: string
}

type Objetivo = { requerido: DocumentoRequerido; reemplaza?: ArchivoAdjunto }

/**
 * Los documentos de una postulación (CU16): los que exige la convocatoria, cuáles ya se adjuntaron y cuáles faltan,
 * con sus archivos. Si la postulación todavía se puede modificar (borrador o devuelta, convocatoria abierta) permite
 * adjuntar, reemplazar y eliminar; si no, solo mostrar y abrir. Lo reutiliza el formulario del CU-02.
 */
export function DocumentosPostulacionPanel({ codigo }: DocumentosPostulacionPanelProps) {
  const consulta = useDocumentos(codigo)
  const eliminar = useEliminarDocumento(codigo)
  const acceso = useAccesoDocumento(codigo)
  const [subiendo, setSubiendo] = useState<Objetivo | null>(null)
  const [eliminando, setEliminando] = useState<{ requerido: DocumentoRequerido; archivo: ArchivoAdjunto } | null>(null)

  if (consulta.isLoading) {
    return <p className="py-8 text-center text-sm text-muted-foreground">Cargando documentos…</p>
  }

  if (consulta.error || !consulta.data) {
    const error = mapearErrorDocumentos(consulta.error)
    return (
      <div
        role="alert"
        className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
      >
        <span>{error.refrescar ? 'La postulación no existe o no es tuya.' : error.mensaje}</span>
        <Button variant="outline" size="sm" onClick={() => void consulta.refetch()}>
          Reintentar
        </Button>
      </div>
    )
  }

  const { puedeEditar, requeridos, reglas } = consulta.data
  const adjuntados = requeridos.filter((r) => r.adjuntado).length

  const abrir = (archivo: ArchivoAdjunto) =>
    acceso.mutate(archivo.codigo, {
      onSuccess: (r) => window.open(r.url, '_blank', 'noopener,noreferrer'),
      onError: (e) => toastError('No se pudo abrir el documento', mapearErrorDocumentos(e).mensaje),
    })

  const confirmarEliminar = () => {
    if (!eliminando) return
    const { archivo, requerido } = eliminando
    eliminar.mutate(archivo.codigo, {
      onSuccess: () => {
        toastSuccess('Documento eliminado', requerido.nombre)
        setEliminando(null)
      },
      onError: (e) => {
        toastWarning('No se pudo eliminar', mapearErrorDocumentos(e).mensaje)
        setEliminando(null)
      },
    })
  }

  return (
    <section aria-label="Documentos" className="flex flex-col gap-3">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="font-heading text-base">
          Adjuntos ({adjuntados} de {requeridos.length})
        </h3>
        <p className="text-xs text-muted-foreground">
          Hasta {reglas.maximoArchivosPorTipo} archivos por documento
        </p>
      </div>

      {!puedeEditar && (
        <p role="status" className="rounded-md border border-border bg-muted/40 p-3 text-sm text-muted-foreground">
          Esta postulación ya no se puede modificar: solo puedes ver tus documentos.
        </p>
      )}

      {requeridos.length === 0 && (
        <p className="py-4 text-center text-sm text-muted-foreground">La convocatoria no exige documentos.</p>
      )}

      <ul className="flex flex-col gap-3">
        {requeridos.map((requerido) => (
          <li
            key={requerido.tipoDocumentoCodigo}
            className={`rounded-2xl border p-4 ${
              !requerido.adjuntado && requerido.obligatorio ? 'border-destructive/40' : 'border-border'
            } bg-card`}
          >
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <p className="font-medium">{requerido.nombre}</p>
                {requerido.descripcion && <p className="text-xs text-muted-foreground">{requerido.descripcion}</p>}
              </div>
              <EstadoDocumento requerido={requerido} />
            </div>

            {requerido.archivos.length > 0 && (
              <ul className="mt-3 flex flex-col gap-2">
                {requerido.archivos.map((archivo) => (
                  <li
                    key={archivo.codigo}
                    className="flex flex-wrap items-center justify-between gap-2 rounded-md bg-muted/40 px-3 py-2"
                  >
                    <div className="flex min-w-0 items-center gap-2">
                      <FileTextIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                      <div className="min-w-0">
                        <p className="truncate text-sm">{archivo.nombreArchivo}</p>
                        <p className="text-xs text-muted-foreground">
                          {formatearTamano(archivo.tamanoBytes)} · {formatearFecha(archivo.subidoEn)}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        aria-label={`Abrir ${archivo.nombreArchivo}`}
                        onClick={() => abrir(archivo)}
                        disabled={acceso.isPending}
                      >
                        <ExternalLinkIcon />
                      </Button>
                      {puedeEditar && (
                        <>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            aria-label={`Reemplazar ${archivo.nombreArchivo}`}
                            onClick={() => setSubiendo({ requerido, reemplaza: archivo })}
                          >
                            <RefreshCwIcon />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            aria-label={`Eliminar ${archivo.nombreArchivo}`}
                            onClick={() => setEliminando({ requerido, archivo })}
                          >
                            <Trash2Icon />
                          </Button>
                        </>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )}

            {puedeEditar && requerido.archivos.length < reglas.maximoArchivosPorTipo && (
              <div className="mt-3">
                <Button variant="outline" size="sm" onClick={() => setSubiendo({ requerido })}>
                  <PlusIcon data-icon="inline-start" />
                  {requerido.adjuntado ? 'Agregar otro archivo' : 'Adjuntar'}
                </Button>
              </div>
            )}
          </li>
        ))}
      </ul>

      {subiendo && (
        <SubirDocumentoModal
          key={`${subiendo.requerido.tipoDocumentoCodigo}-${subiendo.reemplaza?.codigo ?? 'nuevo'}`}
          open
          onOpenChange={(abierto) => {
            if (!abierto) setSubiendo(null)
          }}
          codigo={codigo}
          requerido={subiendo.requerido}
          reglas={reglas}
          reemplaza={subiendo.reemplaza}
        />
      )}

      <ModalDestructive
        open={eliminando !== null}
        onOpenChange={(abierto) => {
          if (!abierto) setEliminando(null)
        }}
        title="Eliminar documento"
        description={`Se eliminará «${eliminando?.archivo.nombreArchivo ?? ''}» de ${eliminando?.requerido.nombre ?? 'tu postulación'}.`}
        isSubmitting={eliminar.isPending}
        onConfirm={confirmarEliminar}
      />
    </section>
  )
}

function EstadoDocumento({ requerido }: { requerido: DocumentoRequerido }) {
  if (requerido.adjuntado) {
    return <Badge className="bg-success/15 text-success">Adjuntado</Badge>
  }
  return requerido.obligatorio ? (
    <Badge variant="destructive">Falta</Badge>
  ) : (
    <Badge variant="outline">Opcional</Badge>
  )
}
