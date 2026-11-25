import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { CalendarDaysIcon } from 'lucide-react'

import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess, toastWarning } from '@/components/ui/toast'

import { mapearErrorConvocatorias, useCrearGestion, useEditarGestion, useRefrescarGestiones } from '../api'
import type { ErrorConvocatorias } from '../api'
import { gestionSchema } from '../schemas'
import type { GestionAcademica } from '../types'
import { AlertaConvocatorias } from './AlertaConvocatorias'
import { CampoConvocatoria } from './CampoConvocatoria'
import { mensajeError, errorVisible } from '@/utils/errorCampo'
import { validacionFormulario } from '@/utils/validacionFormulario'

const FORM_ID = 'gestion-form'

interface GestionFormModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** La gestión a editar; sin ella, el modal crea una nueva. */
  gestion?: GestionAcademica
}

/** Crear o editar una gestión académica (CU13): etiqueta, fecha de inicio y fecha de fin. */
export function GestionFormModal({ open, onOpenChange, gestion }: GestionFormModalProps) {
  const crear = useCrearGestion()
  const editar = useEditarGestion()
  const refrescar = useRefrescarGestiones()
  const [error, setError] = useState<ErrorConvocatorias | null>(null)
  const enviando = useRef(false)

  const mutacion = gestion ? editar : crear

  const form = useForm({
    defaultValues: {
      etiqueta: gestion?.etiqueta ?? '',
      fechaInicio: gestion?.fechaInicio ?? '',
      fechaFin: gestion?.fechaFin ?? '',
    },
    ...validacionFormulario(gestionSchema),
    onSubmit: ({ value }) => {
      if (enviando.current) return
      enviando.current = true
      setError(null)
      const request = gestionSchema.parse(value)
      const alTerminar = {
        onError: (e: unknown) => {
          const mapeado = mapearErrorConvocatorias(e)
          if (mapeado.refrescar) {
            // La gestión ya no existe (A8): se avisa y se actualiza la lista.
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
      if (gestion) {
        editar.mutate(
          { codigo: gestion.codigo, request },
          {
            ...alTerminar,
            onSuccess: () => {
              toastSuccess('Gestión actualizada', request.etiqueta)
              onOpenChange(false)
            },
          },
        )
      } else {
        crear.mutate(request, {
          ...alTerminar,
          onSuccess: () => {
            toastSuccess('Gestión creada', `${request.etiqueta} ya se puede elegir al crear convocatorias.`)
            onOpenChange(false)
          },
        })
      }
    },
  })

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      title={gestion ? 'Editar gestión académica' : 'Nueva gestión académica'}
      subtitle="Ej. «2026-II»: el periodo en que se ofrecen las convocatorias"
      icon={CalendarDaysIcon}
      formId={FORM_ID}
      isSubmitting={mutacion.isPending}
      confirmLabel={gestion ? 'Guardar cambios' : 'Crear gestión'}
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
          <AlertaConvocatorias error={error} onReintentar={() => void form.handleSubmit()} disabled={mutacion.isPending} />
        )}

        <form.Field name="etiqueta">
          {(field) => (
            <CampoConvocatoria
              id={field.name}
              label="Etiqueta"
              autoComplete="off"
              value={field.state.value}
              onBlur={field.handleBlur}
              onChange={(valor) => {
                if (error?.campo === 'etiqueta') setError(null)
                field.handleChange(valor)
              }}
              error={
                error?.campo === 'etiqueta'
                  ? error.mensaje
                  : errorVisible(field)
                    ? field.state.meta.errors[0]?.message
                    : undefined
              }
            />
          )}
        </form.Field>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <form.Field name="fechaInicio">
            {(field) => (
              <CampoConvocatoria
                id={field.name}
                label="Fecha de inicio"
                type="date"
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={field.handleChange}
                error={mensajeError(field)}
              />
            )}
          </form.Field>
          <form.Field name="fechaFin">
            {(field) => (
              <CampoConvocatoria
                id={field.name}
                label="Fecha de fin"
                type="date"
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(valor) => {
                  if (error?.campo === 'fechaFin') setError(null)
                  field.handleChange(valor)
                }}
                error={
                  error?.campo === 'fechaFin'
                    ? error.mensaje
                    : errorVisible(field)
                      ? field.state.meta.errors[0]?.message
                      : undefined
                }
              />
            )}
          </form.Field>
        </div>
      </form>
    </ModalLayout>
  )
}
