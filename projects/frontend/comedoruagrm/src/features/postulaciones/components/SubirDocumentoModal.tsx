import { useEffect, useMemo, useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { FileTextIcon, UploadIcon } from 'lucide-react'

import { Input } from '@/components/ui/input'
import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess, toastWarning } from '@/components/ui/toast'

import {
  mapearErrorDocumentos,
  useAdjuntarDocumento,
  useInvalidarDocumentos,
  useReemplazarDocumento,
} from '../api'
import type { ErrorDocumentos } from '../api'
import { crearArchivoDocumentoSchema, formatearTamano, nombresDeFormatos } from '../schemas'
import type { ArchivoAdjunto, DocumentoRequerido, ReglasArchivo } from '../types'
import { validacionFormulario } from '@/utils/validacionFormulario'

const FORM_ID = 'subir-documento-form'

export interface SubirDocumentoModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** El código público de la postulación. */
  codigo: string
  /** El documento al que se agrega o cuyo archivo se cambia. */
  requerido: DocumentoRequerido
  reglas: ReglasArchivo
  /** El archivo a reemplazar; sin él, el modal agrega uno nuevo. */
  reemplaza?: ArchivoAdjunto
}

/**
 * Adjuntar o reemplazar el archivo de un documento (CU16): elige el archivo, se valida su formato y tamaño con las
 * reglas del backend, se muestra una vista previa y, al confirmar, se sube. Un error del servidor se muestra dentro del
 * modal y el archivo elegido se conserva para reintentar.
 */
export function SubirDocumentoModal({ open, onOpenChange, codigo, requerido, reglas, reemplaza }: SubirDocumentoModalProps) {
  const adjuntar = useAdjuntarDocumento(codigo)
  const reemplazar = useReemplazarDocumento(codigo)
  const refrescar = useInvalidarDocumentos(codigo)
  const [error, setError] = useState<ErrorDocumentos | null>(null)
  const enviando = useRef(false)

  const schema = useMemo(() => crearArchivoDocumentoSchema(reglas), [reglas])
  const mutacion = reemplaza ? reemplazar : adjuntar

  const form = useForm({
    defaultValues: { archivo: null as File | null },
    ...validacionFormulario(schema),
    onSubmit: ({ value }) => {
      if (enviando.current || !value.archivo) return
      enviando.current = true
      setError(null)
      const archivo = value.archivo
      const alTerminar = {
        onError: (e: unknown) => {
          const mapeado = mapearErrorDocumentos(e)
          if (mapeado.refrescar || mapeado.noEditable) {
            // La postulación cambió (ya se envió, la convocatoria cerró o el documento ya no existe): se avisa y se actualiza.
            toastWarning('Sin cambios', mapeado.mensaje)
            void refrescar()
            onOpenChange(false)
            return
          }
          setError(mapeado)
        },
        onSettled: () => {
          enviando.current = false
        },
      }
      if (reemplaza) {
        reemplazar.mutate(
          { documento: reemplaza.codigo, archivo },
          {
            ...alTerminar,
            onSuccess: () => {
              toastSuccess('Documento reemplazado', requerido.nombre)
              onOpenChange(false)
            },
          },
        )
      } else {
        adjuntar.mutate(
          { tipoDocumento: requerido.tipoDocumentoCodigo, archivo },
          {
            ...alTerminar,
            onSuccess: () => {
              toastSuccess('Documento adjuntado', requerido.nombre)
              onOpenChange(false)
            },
          },
        )
      }
    },
  })

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      title={reemplaza ? `Reemplazar ${requerido.nombre}` : `Adjuntar ${requerido.nombre}`}
      subtitle={`Se aceptan ${nombresDeFormatos(reglas.formatosPermitidos)}, hasta ${formatearTamano(reglas.tamanoMaximoBytes)}`}
      icon={UploadIcon}
      formId={FORM_ID}
      isSubmitting={mutacion.isPending}
      confirmLabel={reemplaza ? 'Reemplazar' : 'Adjuntar documento'}
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
        {error && (
          <div
            role="alert"
            className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{error.mensaje}</span>
          </div>
        )}

        <form.Field name="archivo">
          {(field) => <CampoArchivo field={field} reglas={reglas} onElegir={() => setError(null)} />}
        </form.Field>
      </form>
    </ModalLayout>
  )
}

interface CampoArchivoProps {
  field: {
    name: string
    state: { value: File | null; meta: { isTouched: boolean; errors: Array<{ message?: string } | undefined> } }
    handleChange: (archivo: File | null) => void
    handleBlur: () => void
  }
  reglas: ReglasArchivo
  onElegir: () => void
}

function CampoArchivo({ field, reglas, onElegir }: CampoArchivoProps) {
  const archivo = field.state.value
  // La dirección de la vista previa (solo imágenes) se crea al elegir el archivo y se libera al cambiarlo o al cerrar.
  const [vistaPrevia, setVistaPrevia] = useState<string | null>(null)
  useEffect(
    () => () => {
      if (vistaPrevia) URL.revokeObjectURL(vistaPrevia)
    },
    [vistaPrevia],
  )
  const mensaje = field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-1">
        <label htmlFor={field.name} className="text-sm font-medium text-foreground">
          Archivo
        </label>
        <Input
          id={field.name}
          name={field.name}
          type="file"
          accept={reglas.formatosPermitidos.join(',')}
          aria-invalid={mensaje ? true : undefined}
          onBlur={field.handleBlur}
          onChange={(e) => {
            const elegido = e.target.files?.[0] ?? null
            onElegir()
            setVistaPrevia(elegido && elegido.type.startsWith('image/') ? URL.createObjectURL(elegido) : null)
            field.handleChange(elegido)
          }}
        />
        {mensaje && <span className="text-xs text-destructive">{mensaje}</span>}
      </div>

      {archivo && !mensaje && (
        <div className="flex flex-col gap-2 rounded-md border border-border bg-muted/40 p-3" aria-label="Vista previa">
          {vistaPrevia ? (
            <img
              src={vistaPrevia}
              alt={`Vista previa de ${archivo.name}`}
              className="max-h-64 w-full rounded-md object-contain"
            />
          ) : (
            <div className="flex items-center gap-2 text-sm">
              <FileTextIcon className="size-8 text-muted-foreground" aria-hidden />
              <span>Vista previa no disponible para este formato.</span>
            </div>
          )}
          <p className="text-xs text-muted-foreground">
            {archivo.name} · {formatearTamano(archivo.size)}
          </p>
        </div>
      )}
    </div>
  )
}
