import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { CalendarRangeIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { ModalDetailLayout, ModalLayout } from '@/components/ui/modal-layout'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { toastSuccess, toastWarning } from '@/components/ui/toast'

import {
  mapearErrorConvocatorias,
  useConvocatoria,
  useCrearConvocatoria,
  useEditarConvocatoria,
  useGestiones,
  useRefrescarConvocatorias,
} from '../api'
import type { ErrorConvocatorias } from '../api'
import { convocatoriaSchema } from '../schemas'
import type { ConvocatoriaFormValues } from '../schemas'
import type { ConvocatoriaDetalle } from '../types'
import { AlertaConvocatorias } from './AlertaConvocatorias'
import { CampoConvocatoria } from './CampoConvocatoria'
import { DocumentosRequeridosField } from './DocumentosRequeridosField'
import { EstadoConvocatoriaBadge } from './EstadoConvocatoriaBadge'

const FORM_ID = 'convocatoria-form'
const PAGE_SIZE_GESTIONES = 100

/** `crear` (sin convocatoria), `editar` (borrador o abierta) o `ver` (solo lectura: cerrada, publicada o «Ver detalle»). */
export type ModoConvocatoria = 'crear' | 'editar' | 'ver'

interface ConvocatoriaFormModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  modo: ModoConvocatoria
  /** El código de la convocatoria a editar o ver (se omite al crear). */
  codigo?: string
}

/**
 * Crear, editar o ver una convocatoria (CU13, flujo principal pasos 4 a 8). Al editar o ver trae primero el detalle
 * (documentos y si ya tiene postulaciones) y recién entonces arma el formulario con esos valores.
 */
export function ConvocatoriaFormModal({ open, onOpenChange, modo, codigo }: ConvocatoriaFormModalProps) {
  const detalle = useConvocatoria(modo === 'crear' ? undefined : codigo)

  if (modo === 'crear') {
    return <FormularioConvocatoria open={open} onOpenChange={onOpenChange} modo={modo} />
  }
  if (detalle.data) {
    return <FormularioConvocatoria open={open} onOpenChange={onOpenChange} modo={modo} detalle={detalle.data} />
  }
  return (
    <ModalDetailLayout
      open={open}
      onOpenChange={onOpenChange}
      title={modo === 'editar' ? 'Editar convocatoria' : 'Detalle de la convocatoria'}
      icon={CalendarRangeIcon}
    >
      {detalle.isError ? (
        <div role="alert" className="flex flex-wrap items-center justify-between gap-2 text-sm text-destructive">
          <span>{mapearErrorConvocatorias(detalle.error).mensaje}</span>
          <Button type="button" variant="outline" size="sm" onClick={() => void detalle.refetch()}>
            Reintentar
          </Button>
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">Cargando…</p>
      )}
    </ModalDetailLayout>
  )
}

interface FormularioConvocatoriaProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  modo: ModoConvocatoria
  detalle?: ConvocatoriaDetalle
}

function valoresIniciales(detalle?: ConvocatoriaDetalle): ConvocatoriaFormValues {
  if (!detalle) {
    return {
      nombre: '',
      gestionCodigo: '',
      fechaApertura: '',
      fechaCierre: '',
      cupos: '',
      puntajeMinimo: '',
      cantidadFinalistas: '',
      documentos: [{ tipoDocumentoCodigo: '', obligatorio: true }],
    }
  }
  return {
    nombre: detalle.nombre,
    gestionCodigo: detalle.gestion.codigo,
    fechaApertura: detalle.fechaApertura,
    fechaCierre: detalle.fechaCierre,
    cupos: detalle.cupos === null ? '' : String(detalle.cupos),
    puntajeMinimo: detalle.puntajeMinimo === null ? '' : String(detalle.puntajeMinimo),
    cantidadFinalistas: detalle.cantidadFinalistas === null ? '' : String(detalle.cantidadFinalistas),
    documentos: detalle.documentos.map((d) => ({
      tipoDocumentoCodigo: d.tipoDocumento.codigo,
      obligatorio: d.obligatorio,
    })),
  }
}

function FormularioConvocatoria({ open, onOpenChange, modo, detalle }: FormularioConvocatoriaProps) {
  const crear = useCrearConvocatoria()
  const editar = useEditarConvocatoria()
  const refrescar = useRefrescarConvocatorias()
  const gestiones = useGestiones({ page: 1, pageSize: PAGE_SIZE_GESTIONES, sort: '-fechaInicio' })
  const [error, setError] = useState<ErrorConvocatorias | null>(null)
  // Evita el doble envío aunque el segundo clic llegue antes de que React pinte el estado "enviando".
  const enviando = useRef(false)

  const soloLectura = modo === 'ver'
  const tienePostulaciones = detalle?.tienePostulaciones === true
  // La gestión solo se cambia en borrador (el backend responde 409 en otro estado); con postulaciones no se
  // cambian la apertura ni los documentos (A6).
  const gestionBloqueada = soloLectura || (detalle !== undefined && detalle.estado !== 'BORRADOR')
  const aperturaBloqueada = soloLectura || tienePostulaciones
  const documentosBloqueados = soloLectura || tienePostulaciones
  const mutacion = detalle ? editar : crear

  const form = useForm({
    defaultValues: valoresIniciales(detalle),
    validators: { onChange: convocatoriaSchema, onBlur: convocatoriaSchema },
    onSubmit: ({ value }) => {
      if (soloLectura || enviando.current) return
      enviando.current = true
      setError(null)
      const request = convocatoriaSchema.parse(value)
      const alTerminar = {
        onError: (e: unknown) => {
          const mapeado = mapearErrorConvocatorias(e)
          if (mapeado.refrescar) {
            // La convocatoria ya no existe o cambió de estado/postulaciones desde otra sesión (A6 a A9).
            toastWarning('Sin cambios', mapeado.mensaje)
            refrescar(detalle?.codigo)
            onOpenChange(false)
            return
          }
          setError(mapeado)
        },
        onSettled: () => {
          enviando.current = false
        },
      }
      if (detalle) {
        editar.mutate(
          { codigo: detalle.codigo, request },
          {
            ...alTerminar,
            onSuccess: () => {
              toastSuccess('Convocatoria actualizada', request.nombre)
              onOpenChange(false)
            },
          },
        )
      } else {
        crear.mutate(request, {
          ...alTerminar,
          onSuccess: () => {
            toastSuccess('Convocatoria creada', `${request.nombre} quedó en borrador.`)
            onOpenChange(false)
          },
        })
      }
    },
  })

  // Las gestiones del selector; si la de la convocatoria no entra en la primera página, se agrega igual.
  const opcionesGestion = new Map<string, string>()
  if (detalle) opcionesGestion.set(detalle.gestion.codigo, detalle.gestion.etiqueta)
  for (const gestion of gestiones.data?.data ?? []) opcionesGestion.set(gestion.codigo, gestion.etiqueta)

  const titulo = modo === 'crear' ? 'Nueva convocatoria' : modo === 'editar' ? 'Editar convocatoria' : 'Detalle de la convocatoria'
  const cuerpo = (
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
        <AlertaConvocatorias error={error} onReintentar={() => void form.handleSubmit()} disabled={mutacion.isPending} />
      )}

      {detalle && (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          Estado: <EstadoConvocatoriaBadge estado={detalle.estado} />
        </div>
      )}

      {tienePostulaciones && !soloLectura && (
        <p role="note" className="rounded-md border border-warning/40 bg-warning/10 p-3 text-sm text-warning">
          Esta convocatoria ya tiene postulaciones: no se pueden cambiar la fecha de apertura ni los documentos.
        </p>
      )}

      <form.Field name="nombre">
        {(field) => (
          <CampoConvocatoria
            id={field.name}
            label="Nombre"
            value={field.state.value}
            disabled={soloLectura}
            onBlur={field.handleBlur}
            onChange={field.handleChange}
            error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
          />
        )}
      </form.Field>

      <form.Field name="gestionCodigo">
        {(field) => (
          <div className="flex flex-col gap-1">
            <span id="convocatoria-gestion-label" className="text-sm font-medium text-foreground">
              Gestión académica
            </span>
            <Select
              value={field.state.value}
              disabled={gestionBloqueada}
              onValueChange={(valor) => field.handleChange(valor ?? '')}
            >
              <SelectTrigger className="w-full" aria-labelledby="convocatoria-gestion-label">
                <SelectValue>
                  {(valor: string | null) =>
                    valor ? (opcionesGestion.get(valor) ?? 'Cargando…') : 'Selecciona una gestión'
                  }
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                {[...opcionesGestion.entries()].map(([codigo, etiqueta]) => (
                  <SelectItem key={codigo} value={codigo}>
                    {etiqueta}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {detalle && detalle.estado !== 'BORRADOR' && !soloLectura && (
              <span className="text-xs text-muted-foreground">La gestión solo se cambia en una convocatoria en borrador.</span>
            )}
            {field.state.meta.isTouched && field.state.meta.errors[0] && (
              <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
            )}
          </div>
        )}
      </form.Field>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <form.Field name="fechaApertura">
          {(field) => (
            <CampoConvocatoria
              id={field.name}
              label="Fecha de apertura"
              type="date"
              value={field.state.value}
              disabled={aperturaBloqueada}
              onBlur={field.handleBlur}
              onChange={field.handleChange}
              error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
              ayuda="Rige desde las 00:00 de ese día (hora de Bolivia)."
            />
          )}
        </form.Field>
        <form.Field name="fechaCierre">
          {(field) => (
            <CampoConvocatoria
              id={field.name}
              label="Fecha de cierre"
              type="date"
              value={field.state.value}
              disabled={soloLectura}
              onBlur={field.handleBlur}
              onChange={(valor) => {
                if (error?.campo === 'fechaCierre') setError(null)
                field.handleChange(valor)
              }}
              error={
                error?.campo === 'fechaCierre'
                  ? error.mensaje
                  : field.state.meta.isTouched
                    ? field.state.meta.errors[0]?.message
                    : undefined
              }
              ayuda="Rige hasta las 23:59 de ese día (hora de Bolivia)."
            />
          )}
        </form.Field>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <form.Field name="cupos">
          {(field) => (
            <CampoConvocatoria
              id={field.name}
              label="Cupos"
              inputMode="numeric"
              value={field.state.value}
              disabled={soloLectura}
              onBlur={field.handleBlur}
              onChange={field.handleChange}
              error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
              ayuda="Opcional: se define con la DUBSS."
            />
          )}
        </form.Field>
        <form.Field name="puntajeMinimo">
          {(field) => (
            <CampoConvocatoria
              id={field.name}
              label="Puntaje mínimo"
              inputMode="decimal"
              value={field.state.value}
              disabled={soloLectura}
              onBlur={field.handleBlur}
              onChange={field.handleChange}
              error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
              ayuda="Opcional: de 0 a 100."
            />
          )}
        </form.Field>
        <form.Field name="cantidadFinalistas">
          {(field) => (
            <CampoConvocatoria
              id={field.name}
              label="Cantidad de finalistas"
              inputMode="numeric"
              value={field.state.value}
              disabled={soloLectura}
              onBlur={field.handleBlur}
              onChange={field.handleChange}
              error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
              ayuda="Opcional: solo la ve el Administrador."
            />
          )}
        </form.Field>
      </div>

      <form.Field name="documentos">
        {(field) => (
          <DocumentosRequeridosField
            value={field.state.value}
            onChange={field.handleChange}
            onBlur={field.handleBlur}
            disabled={documentosBloqueados}
            actuales={detalle?.documentos}
            error={field.state.meta.isTouched ? field.state.meta.errors[0]?.message : undefined}
          />
        )}
      </form.Field>
    </form>
  )

  if (soloLectura) {
    return (
      <ModalDetailLayout open={open} onOpenChange={onOpenChange} title={titulo} subtitle={detalle?.nombre} icon={CalendarRangeIcon} size="lg">
        {cuerpo}
      </ModalDetailLayout>
    )
  }

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      title={titulo}
      subtitle={
        modo === 'crear'
          ? 'La convocatoria nace en borrador; se abre desde la lista'
          : 'Cambia los datos; el estado se cambia con «Abrir» y «Cerrar»'
      }
      icon={CalendarRangeIcon}
      size="lg"
      formId={FORM_ID}
      isSubmitting={mutacion.isPending}
      confirmLabel={modo === 'crear' ? 'Crear convocatoria' : 'Guardar cambios'}
    >
      {cuerpo}
    </ModalLayout>
  )
}
