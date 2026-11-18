import { Link } from '@tanstack/react-router'
import {
  BellIcon,
  CalendarRangeIcon,
  ClipboardCheckIcon,
  FilePenLineIcon,
  SendIcon,
  TrophyIcon,
  UserIcon,
  WalletCardsIcon,
} from 'lucide-react'

import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { TarjetaIndicador, TituloPanel, estiloTarjeta, formatearFechaHora, formatearNumero } from '@/features/dashboard'
import { EstadoBecaBadge, useMiBeca } from '@/features/mibeca'
import { notificacionesKeys, obtenerResumenNotificacionesApi } from '@/features/notificaciones'
import { EstadoPostulacionBadge, useMisPostulaciones } from '@/features/postulaciones'
import { ApiError } from '@/store'
import { useQuery } from '@tanstack/react-query'

import { usePanelPostulante } from '../api'
import { AtajoPanel, diasHasta } from '../components'
import type { AtajoPanelProps } from '../components'
import type { PanelPostulante } from '../types'

const ERR_SIN_BECA = 'ERR_MBE_01'

const ATAJOS: AtajoPanelProps[] = [
  { titulo: 'Mis postulaciones', descripcion: 'Estado y documentos de tus postulaciones', href: '/mis-postulaciones', icono: ClipboardCheckIcon, tono: 'primary' },
  { titulo: 'Resultados', descripcion: 'Resultados publicados de la convocatoria', href: '/resultados', icono: TrophyIcon, tono: 'accent' },
  { titulo: 'Mi beca', descripcion: 'Tu beca y tus asistencias al comedor', href: '/mi-beca', icono: WalletCardsIcon, tono: 'success' },
  { titulo: 'Perfil', descripcion: 'Tus datos y tu contraseña', href: '/perfil', icono: UserIcon, tono: 'info' },
]

/** La convocatoria abierta y el siguiente paso de la persona: postular, seguir su borrador o esperar. */
function ConvocatoriaAbiertaCard({ panel }: { panel: PanelPostulante }) {
  const abierta = panel.convocatoriaAbierta
  if (!abierta) {
    return (
      <Card className={estiloTarjeta('primary')}>
        <CardHeader>
          <TituloPanel icono={CalendarRangeIcon} tono="primary">Convocatoria abierta</TituloPanel>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">Por ahora no hay ninguna convocatoria abierta. Te avisaremos cuando se abra una.</p>
        </CardContent>
      </Card>
    )
  }
  const dias = diasHasta(abierta.fechaCierre)
  const accion = panel.yaPostulo
    ? null
    : panel.tieneBorrador
      ? { texto: 'Continuar mi borrador', icono: FilePenLineIcon }
      : { texto: 'Postular ahora', icono: SendIcon }
  return (
    <Card className={estiloTarjeta('primary')}>
      <CardHeader>
        <TituloPanel icono={CalendarRangeIcon} tono="primary">Convocatoria abierta</TituloPanel>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <div>
          <p className="font-heading text-2xl">{abierta.nombre}</p>
          <p className="text-sm text-muted-foreground">
            Cierra el {formatearFechaHora(abierta.fechaCierre)} ·{' '}
            {dias === 0 ? 'vence hoy' : `faltan ${formatearNumero(dias)} ${dias === 1 ? 'día' : 'días'}`}
          </p>
        </div>
        {accion ? (
          <Link to="/postular" className={`${buttonVariants()} w-fit`}>
            <accion.icono />
            {accion.texto}
          </Link>
        ) : (
          <p className="text-sm text-success">Ya enviaste tu postulación a esta convocatoria. Sigue su estado en «Mis postulaciones».</p>
        )}
      </CardContent>
    </Card>
  )
}

/** La postulación más reciente de la persona, con el estado que ella puede ver (el que ya respeta lo no publicado). */
function MiPostulacionCard() {
  const postulaciones = useMisPostulaciones({ page: 1, pageSize: 1 })
  const ultima = postulaciones.data?.data[0]
  return (
    <Card className={estiloTarjeta('info')}>
      <CardHeader>
        <TituloPanel icono={ClipboardCheckIcon} tono="info">Mi postulación</TituloPanel>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        {postulaciones.isPending ? (
          <Skeleton className="h-12" />
        ) : postulaciones.isError ? (
          <p className="text-sm text-muted-foreground">No se pudo cargar tu postulación.</p>
        ) : !ultima ? (
          <p className="text-sm text-muted-foreground">Aún no enviaste ninguna postulación.</p>
        ) : (
          <>
            <p className="font-heading text-xl">{ultima.convocatoria.nombre}</p>
            <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
              <EstadoPostulacionBadge estado={ultima.estado} />
              <span>Enviada el {formatearFechaHora(ultima.fechaEnvio)}</span>
            </div>
            <Link to="/mis-postulaciones/$codigo" params={{ codigo: ultima.codigo }} className="w-fit text-sm font-medium text-primary hover:underline">
              Ver el detalle
            </Link>
          </>
        )}
      </CardContent>
    </Card>
  )
}

/** La beca de la persona, si ya tiene una. */
function MiBecaCard() {
  const beca = useMiBeca()
  const sinBeca = beca.error instanceof ApiError && beca.error.code === ERR_SIN_BECA
  return (
    <Card className={estiloTarjeta('success')}>
      <CardHeader>
        <TituloPanel icono={WalletCardsIcon} tono="success">Mi beca</TituloPanel>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        {beca.isPending ? (
          <Skeleton className="h-12" />
        ) : sinBeca ? (
          <p className="text-sm text-muted-foreground">Todavía no tienes una beca. Cuando te adjudiquen una, la verás aquí.</p>
        ) : beca.isError || !beca.data ? (
          <p className="text-sm text-muted-foreground">No se pudo cargar tu beca.</p>
        ) : (
          <>
            <p className="font-heading text-xl">{beca.data.convocatoria}</p>
            <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
              <EstadoBecaBadge estado={beca.data.estado} />
              <span>{formatearNumero(beca.data.totalAsistencias)} asistencias al comedor</span>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}

/**
 * Pantalla de inicio del Postulante: la convocatoria abierta con su cierre y el siguiente paso, el estado de su
 * última postulación, su beca, los avisos sin leer y accesos directos a sus trámites. Cada bloque carga por su
 * cuenta: si uno falla, los demás siguen.
 */
export function PanelPostulanteScreen({ nombres }: { nombres: string }) {
  const panel = usePanelPostulante()
  const avisos = useQuery({ queryKey: notificacionesKeys.resumen(), queryFn: obtenerResumenNotificacionesApi })

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="font-heading text-xl">Hola, {nombres}</h2>
        <p className="text-sm text-muted-foreground">Este es el resumen de tu Beca Comedor.</p>
      </div>

      {panel.isPending ? (
        <Skeleton aria-label="Cargando resumen" className="h-40" />
      ) : panel.isError ? (
        <div role="alert" className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
          <span>No se pudo cargar la convocatoria. Tus accesos directos siguen disponibles.</span>
          <Button size="sm" variant="outline" onClick={() => void panel.refetch()}>Reintentar</Button>
        </div>
      ) : (
        <ConvocatoriaAbiertaCard panel={panel.data} />
      )}

      <div className="grid gap-4 lg:grid-cols-3">
        <MiPostulacionCard />
        <MiBecaCard />
        <TarjetaIndicador
          titulo="Avisos sin leer"
          valor={avisos.data ? formatearNumero(avisos.data.noLeidas) : '—'}
          ayuda="Los ves en la campana de arriba"
          tono="warning"
          icono={BellIcon}
        />
      </div>

      <section aria-labelledby="atajos" className="flex flex-col gap-3">
        <h3 id="atajos" className="font-heading text-lg">Accesos directos</h3>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {ATAJOS.map((atajo) => <AtajoPanel key={atajo.href} {...atajo} />)}
        </div>
      </section>
    </div>
  )
}
