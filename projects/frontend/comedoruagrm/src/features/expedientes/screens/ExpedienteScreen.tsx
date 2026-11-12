import { useEffect, useRef, useState } from 'react'
import { Link } from '@tanstack/react-router'
import { useQueryClient } from '@tanstack/react-query'
import { ArchiveIcon, ArrowLeftIcon, AwardIcon, BanIcon, BellRingIcon, UndoIcon, XCircleIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { toastError } from '@/components/ui/toast'
import { adjudicacionKeys, usePanelAdjudicacion } from '@/features/adjudicacion'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { finalistasKeys, NotificarFinalistasDialog, RegistrarEntregaPanel } from '@/features/finalistas'
import type { Finalista } from '@/features/finalistas'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout } from '@/layouts'

import { expedientesKeys, mapearErrorExpedientes, useExpediente } from '../api'
import {
  AuditoriaForm,
  CerrarPostulacionDialog,
  DecidirAdjudicacionDialog,
  DevolverPostulacionDialog,
  DocumentosDigitales,
  ExpedienteResumen,
  HistorialAuditorias,
  motivoBloqueoAdjudicacion,
  motivoBloqueoAuditoria,
  motivoBloqueoCierre,
  motivoBloqueoDevolucion,
  PuntajePanel,
} from '../components'
import type { AccionCierre, AccionDecision, Expediente } from '../types'

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
  const [cerrando, setCerrando] = useState<AccionCierre | null>(null)
  const [decidiendo, setDecidiendo] = useState<AccionDecision | null>(null)

  const consulta = useExpediente(codigo)
  const expediente = consulta.data
  const error = consulta.error ? mapearErrorExpedientes(consulta.error) : null
  // Los cupos y el puntaje mínimo salen del panel de la convocatoria del expediente (CU05).
  const panel = usePanelAdjudicacion(expediente?.convocatoria.codigo).data

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
  const bloqueoCierre = expediente ? motivoBloqueoCierre(expediente) : null
  const bloqueoAdjudicar = expediente ? motivoBloqueoAdjudicacion(expediente, panel, 'adjudicar') : null
  const bloqueoNoAdjudicar = expediente ? motivoBloqueoAdjudicacion(expediente, panel, 'no-adjudicar') : null
  // Los avisos de por qué están deshabilitadas las acciones, sin repetir: el genérico se omite si hay uno más
  // específico, y «cargando» no es un aviso (los botones solo esperan).
  const GENERICO = 'La postulación ya no está calificada.'
  const CARGANDO = 'Cargando los cupos de la convocatoria…'
  const avisos = [
    ...new Set(
      [bloqueoCierre, bloqueoDevolucion, bloqueoAdjudicar, bloqueoNoAdjudicar].filter(
        (m): m is string => m !== null && m !== CARGANDO,
      ),
    ),
  ]
  const notasBloqueo = avisos.length > 1 ? avisos.filter((m) => m !== GENERICO) : avisos

  const refrescar = () => {
    void queryClient.invalidateQueries({ queryKey: expedientesKeys.detail(codigo) })
    void queryClient.invalidateQueries({ queryKey: adjudicacionKeys.panels() })
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
            <Button
              variant="outline"
              size="sm"
              disabled={bloqueoCierre !== null}
              onClick={() => setCerrando('archivar')}
            >
              <ArchiveIcon data-icon="inline-start" />
              Archivar
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={bloqueoCierre !== null}
              onClick={() => setCerrando('rechazar')}
            >
              <BanIcon data-icon="inline-start" />
              Rechazar
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={bloqueoAdjudicar !== null}
              onClick={() => setDecidiendo('adjudicar')}
            >
              <AwardIcon data-icon="inline-start" />
              Adjudicar
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={bloqueoNoAdjudicar !== null}
              onClick={() => setDecidiendo('no-adjudicar')}
            >
              <XCircleIcon data-icon="inline-start" />
              No adjudicar
            </Button>
          </div>
        )}
      </div>

      {notasBloqueo.map((nota) => (
        <p key={nota} role="note" className="mb-4 text-right text-xs text-muted-foreground">
          {nota}
        </p>
      ))}

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

      {/* Se monta solo mientras está abierto: al cerrar se descarta lo escrito. */}
      {cerrando && expediente && (
        <CerrarPostulacionDialog
          open
          onOpenChange={(abierto) => {
            if (!abierto) setCerrando(null)
          }}
          accion={cerrando}
          finalista={codigo}
          postulante={`${expediente.postulante.nombres} ${expediente.postulante.apellidos}`}
          convocatoria={expediente.convocatoria.nombre}
          onDesactualizado={refrescar}
          onFinalistaInexistente={finalistaInexistente}
        />
      )}

      {/* Se monta solo mientras está abierto: al cerrar se descarta lo escrito. */}
      {decidiendo && expediente && (
        <DecidirAdjudicacionDialog
          open
          onOpenChange={(abierto) => {
            if (!abierto) setDecidiendo(null)
          }}
          accion={decidiendo}
          expediente={expediente}
          panel={panel}
          onDesactualizado={refrescar}
          onFinalistaInexistente={finalistaInexistente}
        />
      )}
    </AdminLayout>
  )
}
