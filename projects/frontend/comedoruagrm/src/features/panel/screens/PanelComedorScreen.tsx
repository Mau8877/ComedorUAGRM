import { CalendarCheckIcon, ClipboardCheckIcon, ClockIcon, SmartphoneIcon, UsersRoundIcon } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '@/components/ui/chart'
import type { ChartConfig } from '@/components/ui/chart'
import { Skeleton } from '@/components/ui/skeleton'
import { TarjetaIndicador, TituloPanel, estiloTarjeta, formatearNumero, formatearPorcentaje } from '@/features/dashboard'

import { usePanelComedor } from '../api'
import { formatearDiaCorto, formatearFecha, formatearHora } from '../components'

const CONFIG = { asistencias: { label: 'Asistencias', color: 'var(--success)' } } satisfies ChartConfig

/**
 * Pantalla de inicio del Personal del Comedor: cuántos becados asistieron hoy, cuántos registró la persona y cómo
 * fueron los últimos 7 días. El registro en sí se hace desde la app móvil (CU10).
 */
export function PanelComedorScreen({ nombres }: { nombres: string }) {
  const panel = usePanelComedor()
  const datos = panel.data
  const serie = (datos?.ultimosDias ?? []).map((d) => ({ dia: formatearDiaCorto(d.fecha), asistencias: d.asistencias }))
  const hayRegistros = serie.some((d) => d.asistencias > 0)

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="font-heading text-xl">Hola, {nombres}</h2>
        <p role="status" className="text-sm text-muted-foreground">
          {datos ? `Resumen de hoy, ${formatearFecha(datos.hoy)}.` : 'Resumen de hoy.'}
        </p>
      </div>

      {panel.isError && !datos && (
        <div role="alert" className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
          <span>No se pudo cargar el resumen.</span>
          <Button size="sm" variant="outline" onClick={() => void panel.refetch()}>Reintentar</Button>
        </div>
      )}

      {panel.isPending && (
        <div aria-label="Cargando resumen" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {[0, 1, 2, 3].map((n) => <Skeleton key={n} className="h-28" />)}
        </div>
      )}

      {datos && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <TarjetaIndicador titulo="Becas vigentes" valor={formatearNumero(datos.becasVigentes)} ayuda="Becados que pueden comer hoy" tono="primary" icono={UsersRoundIcon} />
            <TarjetaIndicador
              titulo="Asistencias de hoy"
              valor={formatearNumero(datos.asistenciasHoy)}
              ayuda={datos.becasVigentes > 0 ? `${formatearPorcentaje((datos.asistenciasHoy / datos.becasVigentes) * 100)} de los becados` : undefined}
              tono="success"
              icono={CalendarCheckIcon}
            />
            <TarjetaIndicador titulo="Registradas por ti" valor={formatearNumero(datos.registradasPorMi)} ayuda="Asistencias que anotaste hoy" tono="info" icono={ClipboardCheckIcon} />
            <TarjetaIndicador
              titulo="Último registro"
              valor={datos.ultimoRegistro ? formatearHora(datos.ultimoRegistro) : '—'}
              ayuda={datos.ultimoRegistro ? 'Hora del último becado registrado' : 'Hoy aún no se registra ninguna asistencia'}
              tono="accent"
              icono={ClockIcon}
            />
          </div>

          <Card className={estiloTarjeta('success')}>
            <CardHeader>
              <TituloPanel icono={CalendarCheckIcon} tono="success">Asistencias de los últimos 7 días</TituloPanel>
            </CardHeader>
            <CardContent>
              {!hayRegistros ? (
                <p className="py-10 text-center text-sm text-muted-foreground">Sin registros en los últimos 7 días.</p>
              ) : (
                <ChartContainer config={CONFIG} className="h-56 w-full">
                  <BarChart data={serie} accessibilityLayer>
                    <CartesianGrid vertical={false} />
                    <XAxis dataKey="dia" tickLine={false} axisLine={false} />
                    <YAxis allowDecimals={false} tickLine={false} axisLine={false} width={40} />
                    <ChartTooltip content={<ChartTooltipContent />} />
                    <Bar dataKey="asistencias" fill="var(--color-asistencias)" radius={4} />
                  </BarChart>
                </ChartContainer>
              )}
            </CardContent>
          </Card>
        </>
      )}

      <Card className={estiloTarjeta('info')}>
        <CardContent className="flex items-center gap-3">
          <SmartphoneIcon className="size-8 shrink-0 text-info" aria-hidden />
          <p className="text-sm">
            <span className="font-heading text-base">Registrar asistencia</span>
            <br />
            <span className="text-muted-foreground">
              El registro de cada becado (por CI) se hace desde la app móvil del comedor. Aquí ves el resumen del día.
            </span>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
