import { useEffect, useRef, useState } from 'react'
import { Link } from '@tanstack/react-router'
import { useQueryClient } from '@tanstack/react-query'
import { ArrowLeftIcon, BellRingIcon, UndoIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { toastError } from '@/components/ui/toast'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { finalistasKeys, NotificarFinalistasDialog, RegistrarEntregaPanel } from '@/features/finalistas'
import type { Finalista } from '@/features/finalistas'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout } from '@/layouts'

import { expedientesKeys, mapearErrorExpedientes, useExpediente } from '../api'
import {
  AuditoriaForm,
  DevolverPostulacionDialog,
  DocumentosDigitales,
  ExpedienteResumen,
  HistorialAuditorias,
  motivoBloqueoAuditoria,
  motivoBloqueoDevolucion,
  PuntajePanel,
} from '../components'
import type { Expediente } from '../types'

export interface ExpedienteScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
  /** Código público del finalista cuyo expediente se audita. */
  codigo: string
  /** El finalista ya no existe (A11): la pantalla vuelve a la lista de finalistas. */
  onFinalistaInexistente?: () => void
}

/** El `Finalista` de la lista que necesitan los paneles de CU21, armado con lo que trae el expediente. */
function comoFinalista(expediente: Expediente): Finalista {
  const { finalista, postulante } = expediente
  return {
    codigo: finalista.codigo,
    posicion: finalista.posicion,
    nombres: postulante.nombres,
    apellidos: postulante.apellidos,
    ci: postulante.ci,
    puntaje: expediente.puntajeIa,
    estadoNotificacion: 'SIN_ENVIAR',
    plazoEntrega: finalista.plazoEntrega,
    notificadoEn: null,
    documentacionRecibidaEn: finalista.entrega.fecha,
    estadoPostulacion: expediente.estadoPostulacion,
    resultadoAuditoria: 'SIN_AUDITAR',
    puntajeTotal: expediente.puntajeTotal,
  }
}

/**
 * «Auditar expediente finalista» (CU07): a la izquierda el resumen y los documentos del expediente; a la
 * derecha el puntaje, la entrega física (CU21), el aviso al finalista, el formulario de auditoría y el
 * historial. Si el finalista ya no existe (A11) lo dice y ofrece volver a la lista.
 */
export function ExpedienteScreen({
  titulo,
  activeHref,
  usuarioActual,
  codigo,
  onFinalistaInexistente,
}: ExpedienteScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const queryClient = useQueryClient()
  const [notificando, setNotificando] = useState(false)
  const [devolviendo, setDevolviendo] = useState(false)

  const consulta = useExpediente(codigo)
  const expediente = consulta.data
  const error = consulta.error ? mapearErrorExpedientes(consulta.error) : null

  // A11: el finalista desapareció (al abrir el expediente o al registrar la auditoría). Se avisa una sola vez,
  // se refresca la lista (ya no debe mostrarlo) y se vuelve a ella.
  const avisado = useRef(false)
  const finalistaInexistente = () => {
    if (avisado.current) return
    avisado.current = true
    toastError('El finalista ya no existe', 'Volviste a la lista de finalistas.')
    void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
    onFinalistaInexistente?.()
  }
  const inexistente = Boolean(error?.finalistaInexistente)
  useEffect(() => {
    if (inexistente) finalistaInexistente()
    // `finalistaInexistente` está protegido por `avisado`: basta reaccionar al cambio del error.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inexistente])

  const bloqueoDevolucion = expediente ? motivoBloqueoDevolucion(expediente) : null

  const refrescar = () => {
    void queryClient.invalidateQueries({ queryKey: expedientesKeys.detail(codigo) })
  }

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <Link to="/finalistas" className="inline-flex items-center gap-1 text-sm text-primary hover:underline">
          <ArrowLeftIcon className="size-4" />
          Volver a finalistas
        </Link>
        {expediente && (
          <div className="flex flex-wrap items-center gap-2">
            <Button variant="outline" size="sm" onClick={() => setNotificando(true)}>
              <BellRingIcon data-icon="inline-start" />
              Notificar
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={bloqueoDevolucion !== null}
              onClick={() => setDevolviendo(true)}
            >
              <UndoIcon data-icon="inline-start" />
              Devolver para corrección
            </Button>
          </div>
        )}
      </div>

      {bloqueoDevolucion && (
        <p role="note" className="mb-4 text-right text-xs text-muted-foreground">
          {bloqueoDevolucion}
        </p>
      )}

      {consulta.isLoading && <p className="py-8 text-center text-sm text-muted-foreground">Cargando expediente…</p>}

      {error && (
        <div
          role="alert"
          className="mb-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          <span>{error.mensaje}</span>
          {!error.finalistaInexistente && (
            <Button variant="outline" size="sm" onClick={() => void consulta.refetch()}>
              Reintentar
            </Button>
          )}
        </div>
      )}

      {expediente && (
        <div className="grid gap-4 xl:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
          <div className="flex flex-col gap-4">
            <ExpedienteResumen expediente={expediente} />
            <DocumentosDigitales finalista={codigo} documentos={expediente.documentos} />
          </div>

          <div className="flex flex-col gap-4">
            <PuntajePanel
              puntajeIa={expediente.puntajeIa}
              puntajeAcademico={expediente.puntajeAcademico}
              puntajeTotal={expediente.puntajeTotal}
            />
            <RegistrarEntregaPanel finalista={comoFinalista(expediente)} onRegistrada={refrescar} />
            <AuditoriaForm
              finalista={codigo}
              bloqueo={motivoBloqueoAuditoria(expediente)}
              onFinalistaInexistente={finalistaInexistente}
            />
            <HistorialAuditorias auditorias={expediente.auditorias} />
          </div>
        </div>
      )}

      {/* Se monta solo mientras está abierto: al cerrar se descarta lo escrito. */}
      {notificando && expediente && (
        <NotificarFinalistasDialog
          open
          onOpenChange={(abierto) => {
            if (!abierto) setNotificando(false)
          }}
          finalistas={[codigo]}
          onNotificados={refrescar}
        />
      )}

      {/* Se monta solo mientras está abierto: al cerrar se descarta lo escrito. */}
      {devolviendo && expediente && (
        <DevolverPostulacionDialog
          open
          onOpenChange={setDevolviendo}
          finalista={codigo}
          onFinalistaInexistente={finalistaInexistente}
        />
      )}
    </AdminLayout>
  )
}
