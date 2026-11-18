import {
  AwardIcon,
  CalendarCheckIcon,
  CalendarRangeIcon,
  ClipboardCheckIcon,
  FileTextIcon,
  InboxIcon,
  ListOrderedIcon,
  PauseCircleIcon,
  ScaleIcon,
  TrophyIcon,
  UsersRoundIcon,
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { TarjetaIndicador, TituloPanel, estiloTarjeta, formatearFechaHora, formatearNumero } from '@/features/dashboard'

import { usePanelAdministrador } from '../api'
import { AtajoPanel, diasHasta } from '../components'
import type { AtajoPanelProps } from '../components'
import type { ConvocatoriaAbierta } from '../types'

const ATAJOS: AtajoPanelProps[] = [
  { titulo: 'Convocatorias', descripcion: 'Crear, abrir y cerrar convocatorias', href: '/convocatorias', icono: CalendarRangeIcon, tono: 'primary' },
  { titulo: 'Lista priorizada', descripcion: 'Postulaciones ordenadas por puntaje', href: '/lista-priorizada', icono: ListOrderedIcon, tono: 'info' },
  { titulo: 'Auditoría y adjudicación', descripcion: 'Validar expedientes y adjudicar becas', href: '/adjudicacion', icono: AwardIcon, tono: 'warning' },
  { titulo: 'Publicar resultados', descripcion: 'Dar a conocer a los adjudicados', href: '/publicar-resultados', icono: TrophyIcon, tono: 'accent' },
  { titulo: 'Actas', descripcion: 'Actas de adjudicación', href: '/actas', icono: FileTextIcon, tono: 'primary' },
  { titulo: 'Padrón de becados', descripcion: 'Becas vigentes, suspendidas y bajas', href: '/padron-becados', icono: UsersRoundIcon, tono: 'success' },
  { titulo: 'Planillas', descripcion: 'Planillas de asistencia por convocatoria', href: '/planillas', icono: ClipboardCheckIcon, tono: 'info' },
  { titulo: 'Cotejo de asistencia', descripcion: 'Comparar planillas con el registro digital', href: '/cotejo-asistencia', icono: ScaleIcon, tono: 'success' },
]

/** Cuánto avanzó la convocatoria abierta: fecha de cierre y postulaciones frente a los cupos (si los tiene). */
function ConvocatoriaAbiertaCard({ convocatoria }: { convocatoria: ConvocatoriaAbierta | null }) {
  if (!convocatoria) {
    return (
      <Card className={estiloTarjeta('primary')}>
        <CardHeader>
          <TituloPanel icono={CalendarRangeIcon} tono="primary">Convocatoria abierta</TituloPanel>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">No hay ninguna convocatoria abierta en este momento.</p>
        </CardContent>
      </Card>
    )
  }
  const dias = diasHasta(convocatoria.fechaCierre)
  const porcentaje = convocatoria.cupos ? Math.min(100, Math.round((convocatoria.postulaciones / convocatoria.cupos) * 100)) : null
  return (
    <Card className={estiloTarjeta('primary')}>
      <CardHeader>
        <TituloPanel icono={CalendarRangeIcon} tono="primary">Convocatoria abierta</TituloPanel>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <div>
          <p className="font-heading text-2xl">{convocatoria.nombre}</p>
          <p className="text-sm text-muted-foreground">
            Cierra el {formatearFechaHora(convocatoria.fechaCierre)} ·{' '}
            {dias === 0 ? 'vence hoy' : `faltan ${formatearNumero(dias)} ${dias === 1 ? 'día' : 'días'}`}
          </p>
        </div>
        <p className="text-sm">
          <span className="font-heading text-xl">{formatearNumero(convocatoria.postulaciones)}</span> postulaciones recibidas
          {convocatoria.cupos !== null && <> · {formatearNumero(convocatoria.cupos)} cupos</>}
        </p>
        {porcentaje !== null && (
          <div
            role="progressbar"
            aria-label="Postulaciones frente a los cupos"
            aria-valuenow={porcentaje}
            aria-valuemin={0}
            aria-valuemax={100}
            className="h-2 overflow-hidden rounded-full bg-primary/15"
          >
            <div className="h-full rounded-full bg-primary" style={{ width: `${porcentaje}%` }} />
          </div>
        )}
      </CardContent>
    </Card>
  )
}

/**
 * Pantalla de inicio del Administrador DUBSS: lo pendiente de atender, el estado de las becas, la convocatoria
 * abierta y accesos directos a los módulos. Los números son conteos del backend, sin datos personales.
 */
export function PanelAdministradorScreen({ nombres }: { nombres: string }) {
  const panel = usePanelAdministrador()
  const datos = panel.data

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="font-heading text-xl">Hola, {nombres}</h2>
        <p className="text-sm text-muted-foreground">Esto es lo que necesita tu atención hoy en la Beca Comedor.</p>
      </div>

      {panel.isError && !datos && (
        <div role="alert" className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
          <span>No se pudo cargar el resumen. Los accesos directos siguen disponibles.</span>
          <Button size="sm" variant="outline" onClick={() => void panel.refetch()}>Reintentar</Button>
        </div>
      )}

      {panel.isPending && (
        <div aria-label="Cargando resumen" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2, 3, 4, 5].map((n) => <Skeleton key={n} className="h-28" />)}
        </div>
      )}

      {datos && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            <TarjetaIndicador titulo="Postulaciones pendientes" valor={formatearNumero(datos.postulacionesPendientes)} ayuda="Enviadas, esperando revisión" tono="warning" icono={InboxIcon} />
            <TarjetaIndicador titulo="Postulaciones calificadas" valor={formatearNumero(datos.postulacionesCalificadas)} ayuda="Con puntaje, listas para la lista priorizada" tono="info" icono={ListOrderedIcon} />
            <TarjetaIndicador titulo="Convocatorias abiertas" valor={formatearNumero(datos.convocatoriasAbiertas)} ayuda="Recibiendo postulaciones" tono="primary" icono={CalendarRangeIcon} />
            <TarjetaIndicador titulo="Becas vigentes" valor={formatearNumero(datos.becasVigentes)} ayuda="Becados activos hoy" tono="success" icono={UsersRoundIcon} />
            <TarjetaIndicador titulo="Becas suspendidas" valor={formatearNumero(datos.becasSuspendidas)} ayuda="Pueden reactivarse desde el padrón" tono="accent" icono={PauseCircleIcon} />
            <TarjetaIndicador titulo="Asistencias de hoy" valor={formatearNumero(datos.asistenciasHoy)} ayuda="Registradas en el comedor" tono="success" icono={CalendarCheckIcon} />
          </div>
          <ConvocatoriaAbiertaCard convocatoria={datos.convocatoriaAbierta} />
        </>
      )}

      <section aria-labelledby="atajos" className="flex flex-col gap-3">
        <h3 id="atajos" className="font-heading text-lg">Accesos directos</h3>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {ATAJOS.map((atajo) => <AtajoPanel key={atajo.href} {...atajo} />)}
        </div>
      </section>
    </div>
  )
}
